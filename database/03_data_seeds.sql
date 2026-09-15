-- =============================================
-- SEEDS: ROLES Y USUARIOS DE PRUEBA
-- =============================================

INSERT INTO Rol (nombre_rol) VALUES
    ('ROLE_ADMIN'),
    ('ROLE_OPERADOR'),
    ('ROLE_CONDUCTOR');

-- Contraseña en texto plano de cada usuario (para tu README):
-- admin      / admin123   -> ROLE_ADMIN
-- operador1  / oper123    -> ROLE_OPERADOR
-- conductor1 / cond123    -> ROLE_CONDUCTOR

INSERT INTO Usuario (username, password_hash, nombre_completo, email, activo) VALUES
    ('admin', '$2b$10$rEnNiAQd7.0OkfoMI0f1POpCiaRyYj/mkmLzX5QUivgEjdT6TWDC6', 'Javier Moreno Sibaja', 'admin@expresofast.cr', 1),
    ('operador1', '$2b$10$5q3CaFl1lOD12ViACzVZIOFp3AxRS2177qQ6iPOmNxWCagmVRha92', 'Operador Uno', 'operador1@expresofast.cr', 1),
    ('conductor1', '$2b$10$uVBbtvIPFNbsti58WM0z2uKkbkYOga6w3vFErk6AGhJOaBi.wjGMS', 'Conductor Uno', 'conductor1@expresofast.cr', 1);

INSERT INTO UsuarioRol (usuario_id, rol_id) VALUES
    (1, 1), -- admin -> ROLE_ADMIN
    (2, 2), -- operador1 -> ROLE_OPERADOR
    (3, 3); -- conductor1 -> ROLE_CONDUCTOR