CREATE TABLE file_metadata (
    id          BIGSERIAL PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,
    stored_name   VARCHAR(255) NOT NULL UNIQUE,
    file_path     VARCHAR(500) NOT NULL,
    file_type     VARCHAR(100) NOT NULL,
    size          BIGINT       NOT NULL,
    uploaded_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_file_metadata_stored_name ON file_metadata (stored_name);
