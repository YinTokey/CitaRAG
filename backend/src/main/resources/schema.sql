CREATE TABLE IF NOT EXISTS documents (
    id SERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    title VARCHAR(255),
    author VARCHAR(255),
    publication_date VARCHAR(255),
    upload_date TIMESTAMP,
    content TEXT
);

CREATE TABLE IF NOT EXISTS collections (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS collection_documents (
    collection_id INTEGER NOT NULL,
    document_id INTEGER NOT NULL,
    PRIMARY KEY (collection_id, document_id),
    FOREIGN KEY (collection_id) REFERENCES collections(id) ON DELETE CASCADE,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
);

