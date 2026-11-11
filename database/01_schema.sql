-- Estrutura do banco do VGRFinance (DER no README).
-- Roda só na criação do volume do banco; após alterações, o volume precisa ser recriado.

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

CREATE TABLE orcamentos (
    id             SERIAL        PRIMARY KEY,
    usuario_id     INTEGER       NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    -- Sempre o dia 1 do mês (a aplicação usa YearMonth). RN27.
    mes_referencia DATE          NOT NULL CHECK (EXTRACT(DAY FROM mes_referencia) = 1),
    renda_base     NUMERIC(12,2) NOT NULL CHECK (renda_base > 0),
    metodo         VARCHAR(20)   NOT NULL CHECK (metodo IN ('PROPORCIONAL', 'BASE_ZERO')),
    -- RN05: no máximo um orçamento por usuário em cada mês.
    CONSTRAINT uq_orcamento_usuario_mes UNIQUE (usuario_id, mes_referencia)
);

CREATE TABLE itens_orcamento (
    id             SERIAL        PRIMARY KEY,
    -- Composição: apagar o orçamento apaga os itens dele (RN06).
    orcamento_id   INTEGER       NOT NULL REFERENCES orcamentos (id) ON DELETE CASCADE,
    -- Sem cascade: uma categoria em uso não pode ser apagada (RN09).
    categoria_id   INTEGER       NOT NULL REFERENCES categorias (id),
    valor_previsto NUMERIC(12,2) NOT NULL CHECK (valor_previsto >= 0),
    CONSTRAINT uq_item_orcamento_categoria UNIQUE (orcamento_id, categoria_id)
);

CREATE INDEX idx_itens_orcamento_categoria ON itens_orcamento (categoria_id);
