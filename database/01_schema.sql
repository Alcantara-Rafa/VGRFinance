-- Estrutura do banco do VGRFinance (DER no README).
-- Roda só na criação do volume; após alterações: bash scripts/reset-banco.sh

CREATE TABLE usuarios (
    id                   SERIAL        PRIMARY KEY,
    nome                 VARCHAR(100)  NOT NULL,
    email                VARCHAR(255)  NOT NULL,
    senha_hash           VARCHAR(100)  NOT NULL,
    papel                VARCHAR(20)   NOT NULL DEFAULT 'USUARIO' CHECK (papel IN ('ADMIN', 'USUARIO')),
    renda_liquida_mensal NUMERIC(12,2) CHECK (renda_liquida_mensal >= 0)
);

CREATE UNIQUE INDEX uk_usuarios_email ON usuarios (LOWER(email));

CREATE TABLE categorias (
    id         SERIAL       PRIMARY KEY,
    usuario_id INTEGER      NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    nome       VARCHAR(60)  NOT NULL CHECK (nome = BTRIM(nome) AND nome <> ''),
    -- Base da regra proporcional: essencial (moradia, comida) ou não (delivery, lazer).
    essencial  BOOLEAN      NOT NULL DEFAULT FALSE
);

-- RN22: nome único por usuário, sem diferenciar maiúsculas e minúsculas.
CREATE UNIQUE INDEX uk_categorias_usuario_nome ON categorias (usuario_id, LOWER(nome));

CREATE TABLE lancamentos (
    id              SERIAL        PRIMARY KEY,
    usuario_id      INTEGER       NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    -- Sem cascade: apagar uma categoria não pode apagar o histórico (RN09).
    -- Nulo só em receita: despesa sempre tem categoria.
    categoria_id    INTEGER       REFERENCES categorias (id),
    descricao       VARCHAR(120),
    valor           NUMERIC(12,2) NOT NULL CHECK (valor > 0),
    data_lancamento DATE          NOT NULL,
    tipo            VARCHAR(10)   NOT NULL CHECK (tipo IN ('RECEITA', 'DESPESA')),
    CONSTRAINT ck_despesa_exige_categoria CHECK (tipo = 'RECEITA' OR categoria_id IS NOT NULL)
);

CREATE INDEX idx_lancamentos_usuario_data ON lancamentos (usuario_id, data_lancamento DESC);
CREATE INDEX idx_lancamentos_categoria ON lancamentos (categoria_id);
