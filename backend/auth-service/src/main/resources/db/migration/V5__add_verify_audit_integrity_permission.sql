-- Crea el permiso para verificar la integridad criptográfica
-- de la cadena de eventos almacenados en audit-service.
INSERT INTO permissions (name, description)
VALUES (
    'VERIFY_AUDIT_INTEGRITY',
    'Permite verificar la integridad criptográfica de los registros de auditoría'
)
ON CONFLICT (name) DO NOTHING;

-- Asigna el permiso al rol AUDITOR.
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    r.id,
    p.id
FROM roles r
JOIN permissions p
    ON p.name = 'VERIFY_AUDIT_INTEGRITY'
WHERE r.name = 'AUDITOR'
ON CONFLICT DO NOTHING;

-- Asigna el permiso al rol ADMINISTRADOR.
INSERT INTO role_permissions (role_id, permission_id)
SELECT
    r.id,
    p.id
FROM roles r
JOIN permissions p
    ON p.name = 'VERIFY_AUDIT_INTEGRITY'
WHERE r.name = 'ADMINISTRADOR'
ON CONFLICT DO NOTHING;