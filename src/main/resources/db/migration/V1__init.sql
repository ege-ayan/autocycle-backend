CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE brands (
    id BIGSERIAL PRIMARY KEY,
    brand_id VARCHAR(64) NOT NULL UNIQUE,
    name TEXT NOT NULL,
    industry TEXT,
    product_categories JSONB,
    plastic_materials JSONB,
    sustainability_philosophy TEXT,
    key_partners_or_collaborators JSONB,
    manufacturing_regions JSONB,
    brand_positioning TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE plastic_types (
    id BIGSERIAL PRIMARY KEY,
    plastic_id VARCHAR(64) NOT NULL UNIQUE,
    type TEXT NOT NULL,
    properties JSONB,
    applications JSONB,
    environmental_impact TEXT,
    recycling_potential TEXT,
    regional_relevance TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE brand_embedding (
    brand_id VARCHAR(64) PRIMARY KEY REFERENCES brands (brand_id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL,
    model VARCHAR(160) NOT NULL,
    embedding vector(1024) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE plastic_embedding (
    plastic_id VARCHAR(64) PRIMARY KEY REFERENCES plastic_types (plastic_id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL,
    model VARCHAR(160) NOT NULL,
    embedding vector(1024) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX brand_embedding_hnsw ON brand_embedding USING hnsw (embedding vector_cosine_ops);
CREATE INDEX plastic_embedding_hnsw ON plastic_embedding USING hnsw (embedding vector_cosine_ops);

CREATE TABLE collaborations (
    id BIGSERIAL PRIMARY KEY,
    source_brand TEXT NOT NULL,
    target_brand TEXT NOT NULL,
    plastic_type TEXT,
    location TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX ux_collaboration_key
    ON collaborations (lower(source_brand), lower(target_brand), lower(plastic_type), lower(location));

CREATE TABLE collaboration_products (
    id BIGSERIAL PRIMARY KEY,
    collaboration_id BIGINT NOT NULL REFERENCES collaborations (id) ON DELETE CASCADE,
    product_type TEXT,
    product_name TEXT,
    product_description TEXT,
    pitch TEXT,
    image_url TEXT,
    full_result JSONB
);

CREATE INDEX idx_collaboration_products_collaboration
    ON collaboration_products (collaboration_id);
