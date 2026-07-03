-- Audit Logs Table
CREATE TABLE IF NOT EXISTS audit_logs (
                                          id BIGSERIAL PRIMARY KEY,
                                          user_id UUID REFERENCES users(id) ON DELETE SET NULL,
                                          action VARCHAR(100) NOT NULL,
                                          ip_address VARCHAR(45),
                                          user_agent TEXT,
                                          timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                          details JSONB
);

CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_timestamp ON audit_logs(timestamp);