package com.yin.cita.repository;

import com.yin.cita.model.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class DocumentRepository {

    private final JdbcTemplate jdbcTemplate;

    public DocumentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Document> fullDocumentRowMapper = (rs, rowNum) -> {
        Document doc = new Document();
        doc.setId(rs.getLong("id"));
        doc.setFilename(rs.getString("filename"));
        doc.setTitle(rs.getString("title"));
        doc.setAuthor(rs.getString("author"));
        doc.setPublicationDate(rs.getString("publication_date"));
        doc.setContent(rs.getString("content"));
        doc.setFileHash(rs.getString("file_hash"));
        Timestamp ts = rs.getTimestamp("upload_date");
        if (ts != null) {
            doc.setUploadDate(ts.toLocalDateTime());
        }
        return doc;
    };

    private final RowMapper<Document> summaryDocumentRowMapper = (rs, rowNum) -> {
        Document doc = new Document();
        doc.setId(rs.getLong("id"));
        doc.setFilename(rs.getString("filename"));
        doc.setTitle(rs.getString("title"));
        doc.setAuthor(rs.getString("author"));
        doc.setPublicationDate(rs.getString("publication_date"));
        // No content
        doc.setFileHash(rs.getString("file_hash"));
        Timestamp ts = rs.getTimestamp("upload_date");
        if (ts != null) {
            doc.setUploadDate(ts.toLocalDateTime());
        }
        return doc;
    };

    public Document save(Document document) {
        if (document.getId() == null) {
            String sql = "INSERT INTO documents (filename, title, author, publication_date, upload_date, content, file_hash) VALUES (?, ?, ?, ?, ?, ?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, document.getFilename());
                ps.setString(2, document.getTitle());
                ps.setString(3, document.getAuthor());
                ps.setString(4, document.getPublicationDate());
                ps.setTimestamp(5, Timestamp.valueOf(document.getUploadDate()));
                ps.setString(6, document.getContent());
                ps.setString(7, document.getFileHash());
                return ps;
            }, keyHolder);

            Map<String, Object> keys = keyHolder.getKeys();
            if (keys != null && keys.containsKey("id")) {
                document.setId(((Number) keys.get("id")).longValue());
            } else if (keyHolder.getKey() != null) {
                document.setId(keyHolder.getKey().longValue());
            }
        } else {
            String sql = "UPDATE documents SET filename = ?, title = ?, author = ?, publication_date = ?, upload_date = ?, content = ?, file_hash = ? WHERE id = ?";
            jdbcTemplate.update(sql, document.getFilename(), document.getTitle(), document.getAuthor(),
                    document.getPublicationDate(),
                    Timestamp.valueOf(document.getUploadDate()),
                    document.getContent(),
                    document.getFileHash(),
                    document.getId());
        }
        return document;
    }

    public List<Document> findAll() {
        return jdbcTemplate.query(
                "SELECT id, filename, title, author, publication_date, upload_date, file_hash FROM documents",
                summaryDocumentRowMapper);
    }

    public Optional<Document> findById(Long id) {
        List<Document> result = jdbcTemplate.query("SELECT * FROM documents WHERE id = ?", fullDocumentRowMapper, id);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    public Optional<Document> findByFileHash(String fileHash) {
        List<Document> result = jdbcTemplate.query("SELECT * FROM documents WHERE file_hash = ?", fullDocumentRowMapper,
                fileHash);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }
}
