-- Dados iniciais de desenvolvimento. Senhas guardadas só como hash BCrypt.

INSERT INTO usuarios (nome, email, senha_hash) VALUES
    ('Usuário Padrão', 'usuario@vgrfinance.local',
     '$2a$10$9LuupemWJeDNMa0eD7vOleTiwvMKHeE3J2I/kup3H3rWId9H1ZNtS');

-- Categorias iniciais do usuário padrão. Sem "Salário": receita não precisa de categoria.
INSERT INTO categorias (usuario_id, nome, essencial)
SELECT u.id, c.nome, c.essencial
FROM usuarios u
CROSS JOIN (VALUES
    ('Moradia', TRUE),
    ('Alimentação', TRUE),
    ('Transporte', TRUE),
    ('Saúde', TRUE),
    ('Lazer', FALSE),
    ('Delivery', FALSE),
    ('Assinaturas', FALSE)
) AS c (nome, essencial)
WHERE u.email = 'usuario@vgrfinance.local';
