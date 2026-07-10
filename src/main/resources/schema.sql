-- Run this once on your PostgreSQL database before starting the app.
-- Requires the pgvector extension to be installed on the Postgres server.

CREATE EXTENSION IF NOT EXISTS vector;

-- Hibernate will create the rest of the tables automatically (ddl-auto=update),
-- but the embedding column needs to be vector type, which Hibernate does not know
-- how to create by itself. So we create document_chunks manually here and Hibernate
-- will just manage the rest of the columns going forward.

CREATE TABLE IF NOT EXISTS document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL,
    chunk_index INTEGER NOT NULL,
    chunk_text TEXT NOT NULL,
    embedding vector(768),
    created_at TIMESTAMP DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_document_chunks_document_id ON document_chunks(document_id);

-- ivfflat index for faster similarity search once you have a few thousand rows
-- (skip this until you have real data, needs ANALYZE after data load)
-- CREATE INDEX ON document_chunks USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
