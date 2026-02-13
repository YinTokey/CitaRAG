package com.yin.cita.service;

import com.yin.cita.model.DocumentElement;
import com.yin.cita.model.FileParsingResult;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.ToXMLContentHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Arrays;

@Service
public class FileParserService {

    // We keep this method for backward compatibility if needed, or update it to use
    // new logic
    // But for the new requirement, we should probably switch to returning elements.
    // However, to keep it simple, let's add a NEW method: parseToElements
    // And update the original parseFile to return a JSON representation of
    // elements?
    // OR update the signature. Updating signature breaks Controller. Let's update
    // Controller too.

    public FileParsingResult parseFileToResult(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null)
            filename = "unknown";

        try {
            return parseToResult(file.getInputStream(), filename);
        } catch (TikaException | SAXException e) {
            throw new IOException("Failed to parse file: " + e.getMessage(), e);
        }
    }

    // Deprecated or delegate to new method if needed, but for now we simply wrap
    // the new result
    public List<DocumentElement> parseFileToElements(MultipartFile file) throws IOException {
        return parseFileToResult(file).getElements();
    }

    // Keep original method but make it use the new logic
    public String parseFile(MultipartFile file) throws IOException {
        List<DocumentElement> elements = parseFileToElements(file);
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(elements);
    }

    private FileParsingResult parseToResult(InputStream stream, String filename)
            throws IOException, TikaException, SAXException {
        // 1. Tika -> XHTML
        Parser parser = new AutoDetectParser();
        ContentHandler handler = new ToXMLContentHandler();
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

        parser.parse(stream, handler, metadata, context);
        String xhtml = handler.toString();

        // Extract relevant metadata
        java.util.Map<String, String> extractedMetadata = new java.util.HashMap<>();
        for (String name : metadata.names()) {
            extractedMetadata.put(name, metadata.get(name));
        }

        // 2. Jsoup -> Elements
        Document doc = Jsoup.parse(xhtml);
        System.out.println("Parsed XHTML: " + (xhtml.length() > 500 ? xhtml.substring(0, 500) + "..." : xhtml));

        List<DocumentElement> docElements = new ArrayList<>();
        String fullText = doc.text();

        // Traverse body children
        Element body = doc.body();
        if (body != null) {
            Elements children = body.children();
            System.out.println("Found " + children.size() + " top-level elements.");
            int currentPage = 1;

            AtomicReference<String> currentSectionHeader = new AtomicReference<>("");

            for (Element child : children) {
                // Check for Tika page break (div class="page")
                if (child.hasClass("page")) {
                    if (child.children().isEmpty()) {
                        // Separator style <div class="page" />
                        currentPage++;
                    } else {
                        // Wrapper style <div class="page">...</div>
                        for (Element pageChild : child.children()) {
                            processElement(pageChild, docElements, filename, currentPage, currentSectionHeader);
                        }
                        currentPage++; // Increment after processing the page
                    }
                } else {
                    processElement(child, docElements, filename, currentPage, currentSectionHeader);
                }
            }
        }

        // 3. Enhanced Metadata Extraction
        String title = extractTitle(metadata, docElements, filename);
        String author = extractAuthor(metadata, docElements);

        extractedMetadata.put("title", title);
        extractedMetadata.put("author", author);

        return new FileParsingResult(docElements, extractedMetadata, fullText);
    }

    String extractTitle(Metadata metadata, List<DocumentElement> elements, String filename) {
        // Layer 1: Metadata
        String title = metadata.get("title");
        if (title == null || title.isEmpty()) {
            title = metadata.get("dc:title");
        }

        if (title != null && !title.trim().isEmpty()) {
            // Validate metadata title isn't just a filename or empty
            String cleanT = cleanTitle(title);
            if (isValidTitle(cleanT)) {
                return cleanT;
            }
        }

        // Layer 2: Heuristics (First TITLE element which usually corresponds to H1/H2)
        for (DocumentElement el : elements) {
            String content = el.getText();
            if (el.getType() == DocumentElement.Type.TITLE) {
                String cleanContent = cleanTitle(content);
                if (isValidTitle(cleanContent)) {
                    return cleanContent;
                }
            }
        }

        // Fallback: First substantial text block (likely P if no H tags)
        for (DocumentElement el : elements) {
            if (el.getType() == DocumentElement.Type.NARRATIVE_TEXT) {
                String content = el.getText();
                if (content.length() > 5 && content.length() < 150) { // Reasonable title length
                    String cleanContent = cleanTitle(content);
                    if (isValidTitle(cleanContent)) {
                        return cleanContent;
                    }
                }
            }
        }

        return filename;
    }

    String extractAuthor(Metadata metadata, List<DocumentElement> elements) {
        // Layer 1: Metadata
        String author = metadata.get("Author");
        if (author == null || author.isEmpty()) {
            author = metadata.get("creator");
        }
        if (author == null || author.isEmpty()) {
            author = metadata.get("dc:creator");
        }
        if (author == null || author.isEmpty()) {
            author = metadata.get("meta:author");
        }

        if (author != null && !author.trim().isEmpty()) {
            if (isValidAuthor(author)) {
                return author.trim();
            }
            // Try split, but for simplicity return if at least one looks valid
            String[] candidates = author.split(";| |,");
            boolean hasValid = false;
            for (String candidate : candidates) {
                if (isValidAuthor(candidate.trim())) {
                    hasValid = true;
                    break;
                }
            }
            if (hasValid)
                return author.trim();
        }

        // Layer 2: Heuristics - Look for text near start
        int linesChecked = 0;

        for (DocumentElement el : elements) {
            String content = el.getText().trim();
            if (content.isEmpty())
                continue;

            if (el.getType() == DocumentElement.Type.TITLE) {
                linesChecked++;
                continue;
            }

            // Check first 10 text blocks
            if (linesChecked < 10) {
                // 1. "By [Name]"
                if (content.toLowerCase().startsWith("by ")) {
                    String potentialAuthor = content.substring(3).trim();
                    if (isValidAuthor(potentialAuthor))
                        return potentialAuthor;
                }

                // 2. Just a name
                if (isValidAuthor(content)) {
                    return content;
                }

                linesChecked++;
            }
        }

        return "Unknown Author";
    }

    String cleanTitle(String rawTitle) {
        if (rawTitle == null)
            return "";
        String title = rawTitle.trim();
        // Remove common prefixes
        title = title.replaceAll("^(?i)(Title:|Subject:)\\s*", "");
        // Remove file extensions
        title = title.replaceAll("(?i)\\.(pdf|docx|doc|txt)$", "");
        return title.trim();
    }

    boolean isValidTitle(String title) {
        if (title == null || title.isEmpty())
            return false;
        if (title.length() < 3)
            return false;
        if (title.equalsIgnoreCase("untitled"))
            return false;
        if (title.toLowerCase().startsWith("microsoft word"))
            return false;
        return true;
    }

    boolean isValidAuthor(String name) {
        if (name == null || name.isEmpty())
            return false;
        name = name.trim();

        if (name.length() < 3 || name.length() > 50)
            return false;
        if (name.matches(".*\\d.*"))
            return false;
        if (name.equalsIgnoreCase("unknown"))
            return false;

        // Check for common non-name words
        String lower = name.toLowerCase();
        List<String> invalidWords = Arrays.asList("introduction", "abstract", "chapter", "page", "university",
                "department", "school", "faculty", "submitted", "thesis");
        for (String word : invalidWords) {
            if (lower.contains(word))
                return false;
        }

        // Regex: Start with Capital, reasonable chars
        return name.matches("^[A-Z][a-zA-Z.\\-']+(?:\\s+[A-Z][a-zA-Z.\\-']+){1,3}$");

    }

    private void processElement(Element element, List<DocumentElement> docElements, String filename, int pageNumber,
            AtomicReference<String> currentSectionHeader) {
        String tagName = element.tagName().toLowerCase();
        String pageStr = String.valueOf(pageNumber);

        if (tagName.matches("h[1-6]")) {
            String text = element.text().trim();
            if (!text.isEmpty()) {
                currentSectionHeader.set(text); // Update current section
                DocumentElement el = new DocumentElement(DocumentElement.Type.TITLE, text);
                el.addMetadata("filename", filename);
                el.addMetadata("tag", tagName);
                el.addMetadata("page_number", pageStr);
                el.addMetadata("section_header", text);
                docElements.add(el);
            }
        } else if (tagName.equals("div")) {
            // Check if div contains block elements (preserving structure)
            if (!element.select("p, h1, h2, h3, h4, h5, h6, div, table, ul, ol").isEmpty()) {
                for (Element child : element.children()) {
                    processElement(child, docElements, filename, pageNumber, currentSectionHeader);
                }
            } else {
                // Treat as text container
                String text = element.text().trim();
                if (!text.isEmpty()) {
                    DocumentElement el = new DocumentElement(DocumentElement.Type.NARRATIVE_TEXT, text);
                    el.addMetadata("filename", filename);
                    el.addMetadata("page_number", pageStr);
                    el.addMetadata("section_header", currentSectionHeader.get());
                    docElements.add(el);
                }
            }
        } else if (tagName.equals("p") || tagName.equals("span")) {
            String text = element.text().trim();
            if (!text.isEmpty()) {
                DocumentElement el = new DocumentElement(DocumentElement.Type.NARRATIVE_TEXT, text);
                el.addMetadata("filename", filename);
                el.addMetadata("page_number", pageStr);
                el.addMetadata("section_header", currentSectionHeader.get());
                docElements.add(el);
            }
        } else if (tagName.equals("table")) {
            DocumentElement el = new DocumentElement(DocumentElement.Type.TABLE, element.text());
            el.addMetadata("filename", filename);
            el.addMetadata("text_as_html", element.outerHtml());
            el.addMetadata("page_number", pageStr);
            el.addMetadata("section_header", currentSectionHeader.get());
            docElements.add(el);
        } else if (tagName.equals("ul") || tagName.equals("ol")) {
            // Handle lists
            for (Element li : element.children()) {
                if (li.tagName().equals("li")) {
                    DocumentElement el = new DocumentElement(DocumentElement.Type.LIST_ITEM, li.text());
                    el.addMetadata("filename", filename);
                    el.addMetadata("page_number", pageStr);
                    el.addMetadata("section_header", currentSectionHeader.get());
                    docElements.add(el);
                }
            }
        } else {
            // Fallback
            String text = element.text().trim();
            if (!text.isEmpty()) {
                DocumentElement el = new DocumentElement(DocumentElement.Type.UNCATEGORIZED, text);
                el.addMetadata("filename", filename);
                el.addMetadata("page_number", pageStr);
                el.addMetadata("section_header", currentSectionHeader.get());
                docElements.add(el);
            }
        }
    }

    public void saveToLocal(String filename, String content) throws IOException {
        java.nio.file.Path directory = java.nio.file.Paths.get("data/parsed");
        if (!java.nio.file.Files.exists(directory)) {
            java.nio.file.Files.createDirectories(directory);
        }

        String savedFilename = filename + ".json";

        java.nio.file.Path file = directory.resolve(savedFilename);
        java.nio.file.Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        System.out.println("Saved parsed file to: " + file.toAbsolutePath());
    }
}
