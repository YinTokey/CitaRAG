package com.yin.cita.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yin.cita.model.Collection;
import com.yin.cita.model.Document;
import com.yin.cita.model.FileParsingResult;
import com.yin.cita.repository.CollectionRepository;
import com.yin.cita.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final CollectionRepository collectionRepository;
    private final FileParserService fileParserService;
    private final VectorStoreService vectorStoreService;
    private final ChunkingService chunkingService;
    private final ObjectMapper objectMapper;

    public DocumentService(DocumentRepository documentRepository, CollectionRepository collectionRepository,
            FileParserService fileParserService, VectorStoreService vectorStoreService,
            ChunkingService chunkingService) {
        this.documentRepository = documentRepository;
        this.collectionRepository = collectionRepository;
        this.fileParserService = fileParserService;
        this.vectorStoreService = vectorStoreService;
        this.chunkingService = chunkingService;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public Document uploadAndParse(MultipartFile file) throws IOException {
        System.out.println("Processing file: " + file.getOriginalFilename());

        // 1. Parse File to Elements
        FileParsingResult result = fileParserService.parseFileToResult(file);
        System.out.println("Parsed " + result.getElements().size() + " elements.");

        // 2. Save Parsed Elements (JSON) for debugging
        String elementsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result.getElements());
        fileParserService.saveToLocal(file.getOriginalFilename(), elementsJson);

        // 3. Chunk Elements (using ChunkingService with context-aware chunking)
        List<Map<String, Object>> chunks = chunkingService.chunkElements(result.getElements());
        System.out.println("Generated " + chunks.size() + " chunks.");

        // 4. Save Chunks (JSON) for debugging
        chunkingService.saveChunks(file.getOriginalFilename(), chunks);

        // 5. Store in Vector Store (Milvus)
        vectorStoreService.storeChunks(chunks);

        // 6. Extract document-level metadata
        String title = null;
        String author = null;
        String publicationDate = null;

        // Fallback to Tika / Heuristics
        Map<String, String> metadata = result.getMetadata();
        author = metadata.get("Author");
        if (author == null)
            author = metadata.get("creator");
        if (author == null)
            author = "Unknown";

        title = metadata.get("title");
        if (title == null || title.isEmpty()) {
            title = file.getOriginalFilename();
        }

        publicationDate = metadata.get("Creation-Date");
        if (publicationDate == null)
            publicationDate = metadata.get("date");
        if (publicationDate == null)
            publicationDate = metadata.get("created");

        // 7. Persist Document entity to PostgreSQL
        Document document = new Document();
        document.setFilename(truncate(file.getOriginalFilename(), 255));
        document.setTitle(truncate(title, 255));
        document.setAuthor(truncate(author, 255));
        document.setPublicationDate(truncate(publicationDate, 255));
        document.setContent(result.getContent()); // Save full content
        document.setUploadDate(LocalDateTime.now());

        Document savedDoc = documentRepository.save(document);

        System.out.println(
                "Persisted document in PostgreSQL: " + savedDoc.getFilename() + " (ID: " + savedDoc.getId() + ")");

        return savedDoc;
    }

    public List<Document> getAllDocuments() {
        System.out.println("Service: Requesting all documents from repository...");
        List<Document> all = documentRepository.findAll();
        System.out.println("Service: Repository returned " + all.size() + " documents.");
        return all;
    }

    public Optional<Document> getDocumentById(Long id) {
        return documentRepository.findById(id);
    }

    @Transactional
    public Collection createCollection(String name, String description) {
        Collection collection = new Collection(name);
        collection.setDescription(description);
        return collectionRepository.save(collection);
    }

    public List<Collection> getAllCollections() {
        return collectionRepository.findAll();
    }

    public Optional<Collection> getCollectionById(Long id) {
        return collectionRepository.findById(id);
    }

    @Transactional
    public void addDocumentToCollection(Long collectionId, Long documentId) {
        // Verify existence
        collectionRepository.findById(collectionId)
                .orElseThrow(() -> new RuntimeException("Collection not found"));
        documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        collectionRepository.addDocument(collectionId, documentId);
    }

    @Transactional
    public void removeDocumentFromCollection(Long collectionId, Long documentId) {
        // Verify existence
        collectionRepository.findById(collectionId)
                .orElseThrow(() -> new RuntimeException("Collection not found"));
        documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        collectionRepository.removeDocument(collectionId, documentId);
    }

    private String truncate(String value, int length) {
        if (value == null || value.length() <= length) {
            return value;
        }
        return value.substring(0, length);
    }
}
