package com.yin.cita.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yin.cita.model.Document;
import com.yin.cita.model.FileParsingResult;
import com.yin.cita.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.MalformedURLException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final FileParserService fileParserService;
    private final VectorStoreService vectorStoreService;
    private final ChunkingService chunkingService;
    private final ObjectMapper objectMapper;
    private final String uploadDir;

    private final Map<Long, Document> processingCache = new java.util.concurrent.ConcurrentHashMap<>();

    public DocumentService(DocumentRepository documentRepository,
            FileParserService fileParserService, VectorStoreService vectorStoreService,
            ChunkingService chunkingService,
            @org.springframework.beans.factory.annotation.Value("${citarag.files.upload-dir:data/uploads}") String uploadDir) {
        this.documentRepository = documentRepository;
        this.fileParserService = fileParserService;
        this.vectorStoreService = vectorStoreService;
        this.chunkingService = chunkingService;
        this.objectMapper = new ObjectMapper();
        this.uploadDir = uploadDir;
    }

    @Transactional
    public Document initiateUpload(MultipartFile file) throws IOException {
        System.out.println("Initiating upload for: " + file.getOriginalFilename());

        // 0. Calculate Hash
        String fileHash = calculateFileHash(file);
        Optional<Document> existingDoc = documentRepository.findByFileHash(fileHash);
        if (existingDoc.isPresent()) {
            System.out.println("Document with hash " + fileHash + " already exists.");
            return existingDoc.get();
        }

        // 1. Save Original File to Disk (Synchronous)
        Path savedFilePath = saveOriginalFileInternal(file);
        System.out.println("File saved to: " + savedFilePath.toString());

        // 2. Create Initial Document Record
        Document document = new Document();
        document.setFilename(truncate(file.getOriginalFilename(), 255));
        document.setFileHash(fileHash);
        document.setUploadDate(LocalDateTime.now());
        document.setProcessingStatus("PENDING"); // Only in memory/DTO initially
        document.setProcessingProgress(0);

        Document savedDoc = documentRepository.save(document); // Doesn't save status to DB anymore

        // Add to cache immediately
        savedDoc.setProcessingStatus("PENDING");
        savedDoc.setProcessingProgress(0);
        processingCache.put(savedDoc.getId(), savedDoc);

        return savedDoc;
    }

    @org.springframework.scheduling.annotation.Async("taskExecutor")
    public void processDocumentAsync(Long documentId) {
        System.out.println("Async processing started for document ID: " + documentId + " on thread "
                + Thread.currentThread().getName());

        // Retrieve from cache first to update transient state
        Document document = processingCache.get(documentId);
        if (document == null) {
            Optional<Document> docOpt = documentRepository.findById(documentId);
            if (docOpt.isEmpty())
                return;
            document = docOpt.get();
            // Put in cache if missing (e.g. on restart recovery, though transient state is
            // lost)
            processingCache.put(documentId, document);
        }

        updateProgress(document, "PROCESSING", 10); // Started

        try {
            // Reconstruct path
            Path uploadDirPath = Paths.get(this.uploadDir);
            Path filePath = uploadDirPath.resolve(document.getFilename());

            // 2. Parse File
            FileParsingResult result = fileParserService.parseFileToResult(filePath);

            updateProgress(document, "PROCESSING", 30); // Parsed

            // 3. Chunking
            List<Map<String, Object>> chunks = chunkingService.chunkElements(result.getElements());

            updateProgress(document, "PROCESSING", 50); // Chunked

            // 4. Store in Vector Store
            vectorStoreService.storeChunks(chunks);

            updateProgress(document, "PROCESSING", 80); // Embedded

            // 5. Extract Metadata
            Map<String, String> metadata = result.getMetadata();
            String title = metadata.get("title");
            if (title == null || title.isEmpty())
                title = document.getFilename();

            String author = metadata.get("Author");
            if (author == null)
                author = metadata.get("creator");
            if (author == null)
                author = "Unknown";

            String pubDate = metadata.get("Creation-Date");

            // 6. Update Document (Core fields only)
            document.setTitle(truncate(title, 255));
            document.setAuthor(truncate(author, 255));
            document.setPublicationDate(truncate(pubDate, 255));
            document.setContent(result.getContent());

            updateProgress(document, "COMPLETED", 100);

            // Save persistent fields to DB
            documentRepository.save(document);
            System.out.println("Async processing completed for doc ID: " + documentId);

            // Allow cache to hold completed state for a bit? Or remove?
            // User wants polling to see 'completed'. Keep in cache.
            // On restart, cache is cleared, but content != null implies completed.

        } catch (Exception e) {
            System.err.println("Error processing document " + documentId + ": " + e.getMessage());
            e.printStackTrace();
            updateProgress(document, "FAILED", 0);
            document.setErrorMessage(e.getMessage());
            // Don't save error to DB (per user request)
        }
    }

    private void updateProgress(Document doc, String status, int progress) {
        doc.setProcessingStatus(status);
        doc.setProcessingProgress(progress);
        // Map updates by reference or put again
        processingCache.put(doc.getId(), doc);
    }

    // Support method used by initiateUpload
    private Path saveOriginalFileInternal(MultipartFile file) throws IOException {
        Path uploadDirPath = Paths.get(this.uploadDir);
        if (!Files.exists(uploadDirPath)) {
            Files.createDirectories(uploadDirPath);
        }
        String filename = file.getOriginalFilename();
        if (filename == null)
            filename = "unknown_" + System.currentTimeMillis();
        Path targetPath = uploadDirPath.resolve(filename);
        Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return targetPath;
    }

    public List<Document> getAllDocuments() {
        // Merge DB results with Cache Status
        List<Document> all = documentRepository.findAll();
        for (Document doc : all) {
            overlayStatus(doc);
        }
        return all;
    }

    public Optional<Document> getDocumentById(Long id) {
        Optional<Document> docOpt = documentRepository.findById(id);
        docOpt.ifPresent(this::overlayStatus);
        return docOpt;
    }

    private void overlayStatus(Document doc) {
        if (processingCache.containsKey(doc.getId())) {
            Document cached = processingCache.get(doc.getId());
            doc.setProcessingStatus(cached.getProcessingStatus());
            doc.setProcessingProgress(cached.getProcessingProgress());
            doc.setErrorMessage(cached.getErrorMessage());
        } else {
            // Infer status if not in cache (e.g. after restart)
            if (doc.getContent() != null && !doc.getContent().isEmpty()) {
                doc.setProcessingStatus("COMPLETED");
                doc.setProcessingProgress(100);
            } else {
                // No content and not in cache -> Failed or abandoned?
                // Or just "Pending" if we assume it might restart?
                // Let's call it "FAILED" or "PENDING".
                // Since we don't store "Processing", if it's not in cache/content, it's
                // inactive.
                doc.setProcessingStatus("PENDING");
            }
        }
    }

    private String calculateFileHash(MultipartFile file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    private void saveOriginalFile(MultipartFile file) throws IOException {
        Path uploadDirPath = Paths.get(this.uploadDir);
        if (!Files.exists(uploadDirPath)) {
            Files.createDirectories(uploadDirPath);
        }

        String filename = file.getOriginalFilename();
        if (filename == null)
            filename = "unknown_" + System.currentTimeMillis();

        Path targetPath = uploadDirPath.resolve(filename);
        Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Saved original file to: " + targetPath.toAbsolutePath());
    }

    public Resource loadAsResource(String filename) {
        try {
            Path file = Paths.get(this.uploadDir).resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not read file: " + filename, e);
        }
    }

    private String truncate(String value, int length) {
        if (value == null || value.length() <= length) {
            return value;
        }
        return value.substring(0, length);
    }
}
