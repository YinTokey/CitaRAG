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

        return new FileParsingResult(docElements, extractedMetadata, fullText);
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
