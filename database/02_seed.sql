-- Dados iniciais de desenvolvimento. Senhas guardadas só como hash BCrypt.

INSERT INTO usuarios (nome, email, senha_hash) VALUES
    ('Usuário Padrão', 'usuario@vgrfinance.test',
     '$2a$10$Jj/Ljkmw/hQLTqCD1L0ZnOnfq5o8Gj4EvpL/IBOXT2DBc1olw0PKe');

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
WHERE u.email = 'usuario@vgrfinance.test';
