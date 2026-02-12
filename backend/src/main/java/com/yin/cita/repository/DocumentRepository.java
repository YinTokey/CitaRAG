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

    private final RowMapper<Document> documentRowMapper = (rs, rowNum) -> {
        Document doc = new Document();
        doc.setId(rs.getLong("id"));
        doc.setFilename(rs.getString("filename"));
        doc.setTitle(rs.getString("title"));
        doc.setAuthor(rs.getString("author"));
        doc.setPublicationDate(rs.getString("publication_date"));
        Timestamp ts = rs.getTimestamp("upload_date");
        if (ts != null) {
            doc.setUploadDate(ts.toLocalDateTime());
        }
        return doc;
    };

    public Document save(Document document) {
        if (document.getId() == null) {
            String sql = "INSERT INTO documents (filename, title, author, publication_date, upload_date) VALUES (?, ?, ?, ?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, document.getFilename());
                ps.setString(2, document.getTitle());
                ps.setString(3, document.getAuthor());
                ps.setString(4, document.getPublicationDate());
                ps.setTimestamp(5, Timestamp.valueOf(document.getUploadDate()));
                return ps;
            }, keyHolder);

            Map<String, Object> keys = keyHolder.getKeys();
            if (keys != null && keys.containsKey("id")) {
                document.setId(((Number) keys.get("id")).longValue());
            } else if (keyHolder.getKey() != null) {
                document.setId(keyHolder.getKey().longValue());
            }
        } else {
            String sql = "UPDATE documents SET filename = ?, title = ?, author = ?, publication_date = ?, upload_date = ? WHERE id = ?";
            jdbcTemplate.update(sql, document.getFilename(), document.getTitle(), document.getAuthor(),
                    document.getPublicationDate(),
                    Timestamp.valueOf(document.getUploadDate()),
                    document.getId());
        }
        return document;
    }

    public List<Document> findAll() {
        return jdbcTemplate.query("SELECT * FROM documents", documentRowMapper);
    }

    public Optional<Document> findById(Long id) {
        List<Document> result = jdbcTemplate.query("SELECT * FROM documents WHERE id = ?", documentRowMapper, id);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }
}
