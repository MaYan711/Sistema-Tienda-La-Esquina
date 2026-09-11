INSERT INTO roles (name, description)
VALUES
    ('ADMIN', 'Administrador del sistema'),
    ('EMPLOYEE', 'Empleado')
ON CONFLICT (name) DO NOTHING;
