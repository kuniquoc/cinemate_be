-- Migration to create chunk_uploads table for supporting chunked file uploads
CREATE TABLE chunk_uploads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    upload_id VARCHAR(255) NOT NULL UNIQUE,
    filename VARCHAR(255) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    total_size BIGINT NOT NULL,
    total_chunks INTEGER NOT NULL,
    chunk_size INTEGER NOT NULL,
    uploaded_chunks INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    uploaded_chunks_list TEXT DEFAULT '[]',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    movie_title VARCHAR(255) NOT NULL,
    movie_description TEXT
);

-- Create indexes for better performance
CREATE INDEX idx_chunk_uploads_upload_id ON chunk_uploads(upload_id);
CREATE INDEX idx_chunk_uploads_status ON chunk_uploads(status);
CREATE INDEX idx_chunk_uploads_expires_at ON chunk_uploads(expires_at);
CREATE INDEX idx_chunk_uploads_created_at ON chunk_uploads(created_at);

-- Add constraints
ALTER TABLE chunk_uploads ADD CONSTRAINT chk_chunk_uploads_status 
    CHECK (status IN ('INITIATED', 'IN_PROGRESS', 'COMPLETED', 'MERGING', 'FAILED', 'EXPIRED'));

ALTER TABLE chunk_uploads ADD CONSTRAINT chk_chunk_uploads_total_size 
    CHECK (total_size > 0);

ALTER TABLE chunk_uploads ADD CONSTRAINT chk_chunk_uploads_total_chunks 
    CHECK (total_chunks > 0);

ALTER TABLE chunk_uploads ADD CONSTRAINT chk_chunk_uploads_chunk_size 
    CHECK (chunk_size > 0);

ALTER TABLE chunk_uploads ADD CONSTRAINT chk_chunk_uploads_uploaded_chunks 
    CHECK (uploaded_chunks >= 0 AND uploaded_chunks <= total_chunks);