package com.yin.cita.service;

import com.yin.cita.model.DocumentElement;
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

    public List<DocumentElement> parseFileToElements(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null)
            filename = "unknown";

        try {
            return parseToElements(file.getInputStream(), filename);
        } catch (TikaException | SAXException e) {
            throw new IOException("Failed to parse file: " + e.getMessage(), e);
        }
    }

    // Keep original method but make it use the new logic and converting to JSON or
    // Text for simple view
    public String parseFile(MultipartFile file) throws IOException {
        List<DocumentElement> elements = parseFileToElements(file);
        // Convert to string representation (e.g. JSON or just text)
        // For backward compatibility with the controller's current expectatins of
        // printing length
        // we can return a summary.
        // BUT, the plan says "Refactor Controller". So we will fix Controller next.
        // For now, let's return a JSON string of elements.
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(elements);
    }

    private List<DocumentElement> parseToElements(InputStream stream, String filename)
            throws IOException, TikaException, SAXException {
        // 1. Tika -> XHTML
        Parser parser = new AutoDetectParser();
        ContentHandler handler = new ToXMLContentHandler();
        Metadata metadata = new Metadata();
        ParseContext context = new ParseContext();

        parser.parse(stream, handler, metadata, context);
        String xhtml = handler.toString();

        // 2. Jsoup -> Elements
        Document doc = Jsoup.parse(xhtml);
        List<DocumentElement> docElements = new ArrayList<>();

        // Traverse body children
        Element body = doc.body();
        if (body != null) {
            Elements children = body.children();
            for (Element child : children) {
                processElement(child, docElements, filename);
            }
        }

        return docElements;
    }

    private void processElement(Element element, List<DocumentElement> docElements, String filename) {
        String tagName = element.tagName().toLowerCase();

        if (tagName.matches("h[1-6]")) {
            DocumentElement el = new DocumentElement(DocumentElement.Type.TITLE, element.text());
            el.addMetadata("filename", filename);
            el.addMetadata("tag", tagName);
            docElements.add(el);
        } else if (tagName.equals("p") || tagName.equals("div") || tagName.equals("span")) {
            String text = element.text().trim();
            if (!text.isEmpty()) {
                DocumentElement el = new DocumentElement(DocumentElement.Type.NARRATIVE_TEXT, text);
                el.addMetadata("filename", filename);
                docElements.add(el);
            }
        } else if (tagName.equals("table")) {
            DocumentElement el = new DocumentElement(DocumentElement.Type.TABLE, element.text());
            el.addMetadata("filename", filename);
            el.addMetadata("text_as_html", element.outerHtml());
            docElements.add(el);
        } else if (tagName.equals("ul") || tagName.equals("ol")) {
            // Handle lists - treat as text for now, or split?
            // "NarrativeText" is fine given user example
            // Iterate li?
            for (Element li : element.children()) {
                if (li.tagName().equals("li")) {
                    DocumentElement el = new DocumentElement(DocumentElement.Type.LIST_ITEM, li.text());
                    el.addMetadata("filename", filename);
                    docElements.add(el);
                }
            }
        } else {
            // Fallback for other tags, treat as text if has text
            String text = element.text().trim();
            if (!text.isEmpty()) {
                DocumentElement el = new DocumentElement(DocumentElement.Type.UNCATEGORIZED, text);
                el.addMetadata("filename", filename);
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
