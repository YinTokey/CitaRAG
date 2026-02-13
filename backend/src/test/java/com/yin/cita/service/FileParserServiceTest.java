package com.yin.cita.service;

import com.yin.cita.model.DocumentElement;
import org.apache.tika.metadata.Metadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class FileParserServiceTest {

    private FileParserService fileParserService;

    @BeforeEach
    void setUp() {
        fileParserService = new FileParserService();
    }

    @Test
    void testCleanTitle() {
        assertEquals("My Document", fileParserService.cleanTitle("Title: My Document"));
        assertEquals("My Document", fileParserService.cleanTitle("Subject: My Document"));
        assertEquals("My Document", fileParserService.cleanTitle("My Document.pdf"));
        assertEquals("My Document", fileParserService.cleanTitle("My Document.docx"));
        assertEquals("My Document", fileParserService.cleanTitle("  My Document  "));
    }

    @Test
    void testIsValidAuthor() {
        assertTrue(fileParserService.isValidAuthor("John Doe"));
        assertTrue(fileParserService.isValidAuthor("J. Doe"));
        assertTrue(fileParserService.isValidAuthor("Jean-Pierre Polnareff"));
        assertTrue(fileParserService.isValidAuthor("Sinead O'Connor"));

        assertFalse(fileParserService.isValidAuthor("Unknown"));
        assertFalse(fileParserService.isValidAuthor("Introduction"));
        assertFalse(fileParserService.isValidAuthor("John Doe 123"));
        assertFalse(fileParserService.isValidAuthor("Jo")); // Too short
    }

    @Test
    void testExtractTitleFromMetadata() {
        Metadata metadata = new Metadata();
        metadata.add("title", "Metadata Title");
        List<DocumentElement> elements = new ArrayList<>();

        String title = fileParserService.extractTitle(metadata, elements, "filename.pdf");
        assertEquals("Metadata Title", title);
    }

    @Test
    void testExtractTitleFromContent() {
        Metadata metadata = new Metadata(); // Empty metadata
        List<DocumentElement> elements = new ArrayList<>();
        elements.add(new DocumentElement(DocumentElement.Type.TITLE, "Content Title"));
        elements.add(new DocumentElement(DocumentElement.Type.NARRATIVE_TEXT, "Some text"));

        String title = fileParserService.extractTitle(metadata, elements, "filename.pdf");
        assertEquals("Content Title", title);
    }

    @Test
    void testExtractTitleFallback() {
        Metadata metadata = new Metadata();
        List<DocumentElement> elements = new ArrayList<>();
        elements.add(new DocumentElement(DocumentElement.Type.NARRATIVE_TEXT, "Fallback Title"));

        String title = fileParserService.extractTitle(metadata, elements, "filename.pdf");
        assertEquals("Fallback Title", title);
    }

    @Test
    void testExtractAuthorFromMetadata() {
        Metadata metadata = new Metadata();
        metadata.add("Author", "Metadata Author");
        List<DocumentElement> elements = new ArrayList<>();

        String author = fileParserService.extractAuthor(metadata, elements);
        assertEquals("Metadata Author", author);
    }

    @Test
    void testExtractAuthorFromContent_ByKeyword() {
        Metadata metadata = new Metadata();
        List<DocumentElement> elements = new ArrayList<>();
        elements.add(new DocumentElement(DocumentElement.Type.TITLE, "The Title"));
        elements.add(new DocumentElement(DocumentElement.Type.NARRATIVE_TEXT, "By John Author"));

        String author = fileParserService.extractAuthor(metadata, elements);
        assertEquals("John Author", author);
    }

    @Test
    void testExtractAuthorFromContent_NameOnly() {
        Metadata metadata = new Metadata();
        List<DocumentElement> elements = new ArrayList<>();
        elements.add(new DocumentElement(DocumentElement.Type.TITLE, "The Title"));
        elements.add(new DocumentElement(DocumentElement.Type.NARRATIVE_TEXT, "Jane Doe"));

        String author = fileParserService.extractAuthor(metadata, elements);
        assertEquals("Jane Doe", author);
    }
}
