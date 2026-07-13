CREATE TABLE audit_outbox (
 id UUID PRIMARY KEY,
 user_id BIGINT,
 user_email VARCHAR(254),
 action VARCHAR(100) NOT NULL,
 resource_type VARCHAR(100) NOT NULL,
 resource_id VARCHAR(150),
 result VARCHAR(30) NOT NULL,
 ip_address VARCHAR(64),
 details VARCHAR(2000),
 correlation_id VARCHAR(100),
 status VARCHAR(20) NOT NULL,
 attempts INTEGER NOT NULL DEFAULT 0,
 next_attempt_at TIMESTAMPTZ NOT NULL,
 created_at TIMESTAMPTZ NOT NULL,
 sent_at TIMESTAMPTZ,
 CONSTRAINT chk_audit_outbox_status CHECK (status IN ('PENDING','SENT'))
);
CREATE INDEX idx_audit_outbox_dispatch ON audit_outbox(status, next_attempt_at, created_at);
