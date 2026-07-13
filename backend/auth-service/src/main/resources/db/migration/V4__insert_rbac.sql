-- ROLES

INSERT INTO roles (name, description)
VALUES
    ('CLIENTE', 'Usuario que consulta productos y realiza compras'),
    ('VENDEDOR', 'Usuario encargado de gestionar productos y pedidos'),
    ('SOPORTE', 'Usuario encargado de atender incidencias y solicitudes'),
    ('AUDITOR', 'Usuario autorizado para consultar registros de auditoría'),
    ('ADMINISTRADOR', 'Usuario encargado de administrar usuarios y roles')
ON CONFLICT (name) DO NOTHING;

-- PERMISOS
INSERT INTO permissions (name, description)
VALUES
    ('VIEW_PRODUCTS', 'Permite consultar productos'),
    ('CREATE_PRODUCT', 'Permite crear productos'),
    ('UPDATE_PRODUCT', 'Permite actualizar productos'),
    ('DELETE_PRODUCT', 'Permite eliminar o desactivar productos'),

    ('CREATE_ORDER', 'Permite crear pedidos'),
    ('VIEW_OWN_ORDERS', 'Permite consultar los pedidos propios'),
    ('VIEW_ALL_ORDERS', 'Permite consultar todos los pedidos'),
    ('UPDATE_ORDER_STATUS', 'Permite actualizar el estado de los pedidos'),
    ('CANCEL_OWN_ORDER', 'Permite cancelar pedidos propios'),

    ('CREATE_TICKET', 'Permite crear tickets de soporte'),
    ('VIEW_OWN_TICKETS', 'Permite consultar tickets propios'),
    ('VIEW_ALL_TICKETS', 'Permite consultar todos los tickets'),
    ('REPLY_TICKET', 'Permite responder tickets'),
    ('ASSIGN_TICKET', 'Permite asignar tickets'),
    ('UPDATE_TICKET_STATUS', 'Permite actualizar el estado de los tickets'),

    ('MANAGE_ROLES', 'Permite asignar o reemplazar roles de usuarios'),
    ('VIEW_AUDIT_LOGS', 'Permite consultar los registros de auditoría')
ON CONFLICT (name) DO NOTHING;

-- CLIENTE
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
    ON p.name IN (
        'VIEW_PRODUCTS',
        'CREATE_ORDER',
        'VIEW_OWN_ORDERS',
        'CANCEL_OWN_ORDER',
        'CREATE_TICKET',
        'VIEW_OWN_TICKETS'
    )
WHERE r.name = 'CLIENTE'
ON CONFLICT DO NOTHING;

-- VENDEDOR
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
    ON p.name IN (
        'VIEW_PRODUCTS',
        'CREATE_PRODUCT',
        'UPDATE_PRODUCT',
        'DELETE_PRODUCT',
        'VIEW_ALL_ORDERS',
        'UPDATE_ORDER_STATUS'
    )
WHERE r.name = 'VENDEDOR'
ON CONFLICT DO NOTHING;

-- SOPORTE
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
    ON p.name IN (
        'VIEW_ALL_TICKETS',
        'REPLY_TICKET',
        'ASSIGN_TICKET',
        'UPDATE_TICKET_STATUS'
    )
WHERE r.name = 'SOPORTE'
ON CONFLICT DO NOTHING;

-- AUDITOR
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p
    ON p.name IN (
        'VIEW_AUDIT_LOGS'
    )
WHERE r.name = 'AUDITOR'
ON CONFLICT DO NOTHING;

-- ADMINISTRADOR
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON TRUE
WHERE r.name = 'ADMINISTRADOR'
ON CONFLICT DO NOTHING;