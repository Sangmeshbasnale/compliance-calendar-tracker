-- Add indexes to improve query performance for common searches and filters
CREATE INDEX idx_compliance_status ON compliance(status);
CREATE INDEX idx_compliance_due_date ON compliance(due_date);
CREATE INDEX idx_compliance_title ON compliance(title);
