CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255),
    title VARCHAR(255),
    author VARCHAR(255),
    publication_date VARCHAR(255),
    upload_date TIMESTAMP,
    content TEXT,
    file_hash VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS collections (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255),
    description VARCHAR(255),
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS collection_documents (
    collection_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    PRIMARY KEY (collection_id, document_id),
    CONSTRAINT fk_collection
      FOREIGN KEY(collection_id) 
      REFERENCES collections(id)
      ON DELETE CASCADE,
    CONSTRAINT fk_document
      FOREIGN KEY(document_id) 
      REFERENCES documents(id)
      ON DELETE CASCADE
);
