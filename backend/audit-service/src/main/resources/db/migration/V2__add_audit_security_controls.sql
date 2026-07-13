ALTER TABLE audit_events
    ADD COLUMN IF NOT EXISTS recorded_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS critical BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS previous_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS event_hash VARCHAR(64);

-- Compatibilidad con instalaciones que ya tenían V1 y eventos antiguos.
-- Los registros heredados quedan identificados criptográficamente, pero la
-- verificación de cadena advertirá que no pertenecen a la nueva cadena HMAC.
UPDATE audit_events
SET recorded_at = COALESCE(recorded_at, occurred_at),
    previous_hash = COALESCE(previous_hash, REPEAT('0', 64)),
    event_hash = COALESCE(event_hash, MD5(id::text) || MD5(id::text || '-legacy'))
WHERE recorded_at IS NULL
   OR previous_hash IS NULL
   OR event_hash IS NULL;

ALTER TABLE audit_events
    ALTER COLUMN recorded_at SET NOT NULL,
    ALTER COLUMN event_hash SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_audit_event_hash
    ON audit_events (event_hash);
CREATE INDEX IF NOT EXISTS idx_audit_recorded_at
    ON audit_events (recorded_at);
CREATE INDEX IF NOT EXISTS idx_audit_critical
    ON audit_events (critical);
