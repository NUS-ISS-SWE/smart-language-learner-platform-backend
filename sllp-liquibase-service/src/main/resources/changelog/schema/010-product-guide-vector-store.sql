--liquibase formatted sql

--changeset mingwei:010-product-guide-vector-store
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS product_guide_vector_store
(
    id        UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    content   TEXT,
    metadata  JSON,
    embedding VECTOR(1536)
);

CREATE INDEX IF NOT EXISTS product_guide_vector_store_index
    ON product_guide_vector_store USING HNSW (embedding vector_cosine_ops);
