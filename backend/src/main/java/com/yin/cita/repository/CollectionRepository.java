package com.yin.cita.repository;

import com.yin.cita.model.Collection;
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
public class CollectionRepository {

    private final JdbcTemplate jdbcTemplate;

    public CollectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Collection> collectionRowMapper = (rs, rowNum) -> {
        Collection col = new Collection();
        col.setId(rs.getLong("id"));
        col.setName(rs.getString("name"));
        col.setDescription(rs.getString("description"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            col.setCreatedAt(ts.toLocalDateTime());
        }
        return col;
    };

    public Collection save(Collection collection) {
        if (collection.getId() == null) {
            String sql = "INSERT INTO collections (name, description, created_at) VALUES (?, ?, ?)";
            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, collection.getName());
                ps.setString(2, collection.getDescription());
                ps.setTimestamp(3, Timestamp.valueOf(collection.getCreatedAt()));
                return ps;
            }, keyHolder);

            Map<String, Object> keys = keyHolder.getKeys();
            if (keys != null && keys.containsKey("id")) {
                collection.setId(((Number) keys.get("id")).longValue());
            } else if (keyHolder.getKey() != null) {
                collection.setId(keyHolder.getKey().longValue());
            }
        } else {
            String sql = "UPDATE collections SET name = ?, description = ?, created_at = ? WHERE id = ?";
            jdbcTemplate.update(sql, collection.getName(), collection.getDescription(),
                    Timestamp.valueOf(collection.getCreatedAt()), collection.getId());
        }

        // Save relations NOT implemented here automatically for simplicity unless
        // needed.
        // Usually handled by specific methods like addDocumentToCollection
        return collection;
    }

    public List<Collection> findAll() {
        return jdbcTemplate.query("SELECT * FROM collections", collectionRowMapper);
    }

    public Optional<Collection> findById(Long id) {
        List<Collection> result = jdbcTemplate.query("SELECT * FROM collections WHERE id = ?", collectionRowMapper, id);
        if (result.isEmpty())
            return Optional.empty();

        Collection collection = result.get(0);
        // Load documents
        String sql = "SELECT d.* FROM documents d JOIN collection_documents cd ON d.id = cd.document_id WHERE cd.collection_id = ?";
        // We reuse DocumentRepository's mapper logic effectively, but here we can't
        // access private mapper easily.
        // Duplicating mapper logic or making it public is an option.
        // Or just using query maps.

        List<Document> documents = jdbcTemplate.query(sql, (rs, rowNum) -> {
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
        }, id);

        collection.getDocuments().addAll(documents);
        return Optional.of(collection);
    }

    public void addDocument(Long collectionId, Long documentId) {
        String sql = "INSERT INTO collection_documents (collection_id, document_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        jdbcTemplate.update(sql, collectionId, documentId);
    }

    public void removeDocument(Long collectionId, Long documentId) {
        String sql = "DELETE FROM collection_documents WHERE collection_id = ? AND document_id = ?";
        jdbcTemplate.update(sql, collectionId, documentId);
    }
}
