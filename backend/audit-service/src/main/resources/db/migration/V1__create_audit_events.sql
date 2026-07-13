CREATE TABLE audit_events (
    id UUID PRIMARY KEY,
    user_id VARCHAR(100),
    user_email VARCHAR(254),
    source_service VARCHAR(80) NOT NULL,
    action VARCHAR(120) NOT NULL,
    resource VARCHAR(120),
    resource_id VARCHAR(120),
    result VARCHAR(30) NOT NULL,
    ip_address VARCHAR(64),
    correlation_id VARCHAR(100) NOT NULL,
    detail VARCHAR(1000),
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_audit_occurred_at
    ON audit_events (occurred_at);
CREATE INDEX idx_audit_user_email
    ON audit_events (user_email);
CREATE INDEX idx_audit_service_action
    ON audit_events (source_service, action);
CREATE INDEX idx_audit_correlation_id
    ON audit_events (correlation_id);

-- Evita UPDATE y DELETE para el usuario normal de la aplicación.
-- En producción, usa un segundo usuario administrativo para mantenimiento.
