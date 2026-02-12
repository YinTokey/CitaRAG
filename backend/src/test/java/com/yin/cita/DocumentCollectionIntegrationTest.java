package com.yin.cita;

import com.yin.cita.model.Collection;
import com.yin.cita.model.Document;
import com.yin.cita.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class DocumentCollectionIntegrationTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setup() {
        // H2 might need schema init if not auto-loaded, but Spring Boot usually loads
        // schema.sql.
        // We can manually clear tables just in case, though @Transactional usually
        // handles rollback.
        jdbcTemplate.execute("DELETE FROM collection_documents");
        jdbcTemplate.execute("DELETE FROM documents");
        jdbcTemplate.execute("DELETE FROM collections");
    }

    @Test
    public void testDocumentAndCollectionFlow() throws Exception {
        // 1. Upload Document
        String content = "Test content for the document. Author: Test User.";
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", content.getBytes());
        Document savedDoc = documentService.uploadAndParse(file);

        assertNotNull(savedDoc.getId());
        assertEquals("test.txt", savedDoc.getFilename());
        assertEquals("test.txt", savedDoc.getTitle());
        // Simple mock file doesn't have metadata, so publicationDate might be null.
        // assertNull(savedDoc.getPublicationDate());

        // 2. Create Collection
        Collection collection = documentService.createCollection("Research 2024", "Important papers");
        assertNotNull(collection.getId());
        assertEquals("Research 2024", collection.getName());

        // 3. Add Document to Collection
        documentService.addDocumentToCollection(collection.getId(), savedDoc.getId());

        // 4. Verify Relationship
        Collection fetchedCollection = documentService.getCollectionById(collection.getId()).get();
        assertEquals(1, fetchedCollection.getDocuments().size());

        // Explicitly check if one of the docs has the same ID (equality might fail on
        // object ref if not overridden equals)
        boolean docFound = fetchedCollection.getDocuments().stream().anyMatch(d -> d.getId().equals(savedDoc.getId()));
        assertTrue(docFound);

        // 5. Remove Document
        documentService.removeDocumentFromCollection(collection.getId(), savedDoc.getId());

        fetchedCollection = documentService.getCollectionById(collection.getId()).get();
        assertTrue(fetchedCollection.getDocuments().isEmpty());
    }
}
