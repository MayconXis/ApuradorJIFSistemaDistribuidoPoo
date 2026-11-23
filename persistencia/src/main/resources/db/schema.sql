-- Esquema do Apurador JIF no PostgreSQL.
-- É executado uma vez, na primeira subida do banco (pasta /docker-entrypoint-initdb.d do container).

CREATE SEQUENCE IF NOT EXISTS evento_seq START WITH 1 INCREMENT BY 1;

-- Chave da fase classificatória. A linha da chave é a que o worker trava para aplicar eventos.
CREATE TABLE IF NOT EXISTS chave (
    id            VARCHAR(40)  PRIMARY KEY,
    nome          VARCHAR(60)  NOT NULL,
    modalidade    VARCHAR(20)  NOT NULL,
    versao        BIGINT       NOT NULL DEFAULT 0,
    criada_em     TIMESTAMPTZ  NOT NULL,
    atualizada_em TIMESTAMPTZ  NOT NULL
);

CREATE TABLE IF NOT EXISTS chave_equipe (
    chave_id VARCHAR(40) NOT NULL REFERENCES chave (id),
    ordem    INTEGER     NOT NULL,
    equipe   VARCHAR(60) NOT NULL,
    PRIMARY KEY (chave_id, ordem)
);

-- Partida com o último resultado aplicado (histórico de partidas homologadas).
CREATE TABLE IF NOT EXISTS partida (
    id                 VARCHAR(60)  PRIMARY KEY,
    chave_id           VARCHAR(40)  NOT NULL REFERENCES chave (id),
    mandante           VARCHAR(60)  NOT NULL,
    visitante          VARCHAR(60)  NOT NULL,
    estado             VARCHAR(20)  NOT NULL,
    placar_mandante    INTEGER      NOT NULL DEFAULT 0,
    placar_visitante   INTEGER      NOT NULL DEFAULT 0,
    parciais           VARCHAR(120),
    wo                 BOOLEAN      NOT NULL DEFAULT FALSE,
    ausente            VARCHAR(10),
    amarelos_mandante  INTEGER      NOT NULL DEFAULT 0,
    amarelos_visitante INTEGER      NOT NULL DEFAULT 0,
    vermelhos_mandante INTEGER      NOT NULL DEFAULT 0,
    vermelhos_visitante INTEGER     NOT NULL DEFAULT 0,
    sancoes_mandante   INTEGER      NOT NULL DEFAULT 0,
    sancoes_visitante  INTEGER      NOT NULL DEFAULT 0,
    evento_id          VARCHAR(60),
    seq_aplicada       BIGINT       NOT NULL DEFAULT 0,
    atualizada_em      TIMESTAMPTZ  NOT NULL
);
CREATE INDEX IF NOT EXISTS partida_chave_idx ON partida (chave_id);

-- Todo resultado enviado. É o histórico que substitui o log do Kafka:
-- o evento é gravado aqui como PENDENTE antes de a tarefa ser publicada.
CREATE TABLE IF NOT EXISTS evento_resultado (
    seq                BIGINT       PRIMARY KEY,
    evento_id          VARCHAR(60)  NOT NULL UNIQUE,
    chave_id           VARCHAR(40)  NOT NULL REFERENCES chave (id),
    partida_id         VARCHAR(60)  NOT NULL,
    placar_mandante    INTEGER      NOT NULL DEFAULT 0,
    placar_visitante   INTEGER      NOT NULL DEFAULT 0,
    parciais           VARCHAR(120),
    wo                 BOOLEAN      NOT NULL DEFAULT FALSE,
    ausente            VARCHAR(10),
    amarelos_mandante  INTEGER      NOT NULL DEFAULT 0,
    amarelos_visitante INTEGER      NOT NULL DEFAULT 0,
    vermelhos_mandante INTEGER      NOT NULL DEFAULT 0,
    vermelhos_visitante INTEGER     NOT NULL DEFAULT 0,
    sancoes_mandante   INTEGER      NOT NULL DEFAULT 0,
    sancoes_visitante  INTEGER      NOT NULL DEFAULT 0,
    estado             VARCHAR(20)  NOT NULL,
    motivo             VARCHAR(300),
    worker             VARCHAR(60),
    recebido_em        TIMESTAMPTZ  NOT NULL,
    encerrado_em       TIMESTAMPTZ,
    aplicacoes         INTEGER      NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS evento_chave_estado_idx ON evento_resultado (chave_id, estado, seq);

-- Classificação gravada de cada chave.
CREATE TABLE IF NOT EXISTS posicao (
    chave_id            VARCHAR(40)  NOT NULL REFERENCES chave (id),
    colocacao           INTEGER      NOT NULL,
    equipe              VARCHAR(60)  NOT NULL,
    pontos              INTEGER      NOT NULL,
    jogos               INTEGER      NOT NULL,
    vitorias            INTEGER      NOT NULL,
    empates             INTEGER      NOT NULL,
    derrotas            INTEGER      NOT NULL,
    pro                 INTEGER      NOT NULL,
    contra              INTEGER      NOT NULL,
    criterio_decisivo   VARCHAR(80)  NOT NULL,
    pendente_de_sorteio BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (chave_id, colocacao)
);
