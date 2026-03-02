package com.yin.cita;

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
public class DocumentIntegrationTest {

    @Autowired
    private DocumentService documentService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setup() {
        jdbcTemplate.execute("DELETE FROM documents");
    }

    @Test
    public void testDocumentUploadFlow() throws Exception {
        // 1. Upload Document
        String content = "Test content for the document. Author: Test User.";
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", content.getBytes());
        Document savedDoc = documentService.initiateUpload(file);

        assertNotNull(savedDoc.getId());
        assertEquals("test.txt", savedDoc.getFilename());
    }
}
