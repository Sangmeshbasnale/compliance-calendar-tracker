ALTER TABLE compliance ADD COLUMN is_deleted BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_compliance_is_deleted ON compliance (is_deleted);
