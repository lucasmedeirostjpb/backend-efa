--liquibase formatted sql

--changeset polvo:3
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM pg_tables WHERE tablename = 'eixos_tematicos'
CREATE TABLE eixos_tematicos (
    id BIGINT PRIMARY KEY,
    nome VARCHAR(255) UNIQUE NOT NULL,
    data_criacao TIMESTAMP NOT NULL,
    usuario_criacao VARCHAR(255) NOT NULL,
    data_atualizacao TIMESTAMP,
    usuario_atualizacao VARCHAR(255)
);

--changeset polvo:4
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM pg_tables WHERE tablename = 'setores'
CREATE TABLE setores (
    id BIGINT PRIMARY KEY,
    sigla VARCHAR(50) UNIQUE NOT NULL,
    nome VARCHAR(255) NOT NULL,
    data_criacao TIMESTAMP NOT NULL,
    usuario_criacao VARCHAR(255) NOT NULL,
    data_atualizacao TIMESTAMP,
    usuario_atualizacao VARCHAR(255)
);

--changeset polvo:5
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.columns WHERE table_name='metas' AND column_name='artigo'
ALTER TABLE metas ALTER COLUMN id DROP DEFAULT;
DROP SEQUENCE IF EXISTS metas_id_seq CASCADE;

ALTER TABLE metas DROP COLUMN concluida;

ALTER TABLE metas ADD COLUMN eixo_id BIGINT;
ALTER TABLE metas ADD COLUMN setor_id BIGINT;
ALTER TABLE metas ADD COLUMN artigo VARCHAR(255);
ALTER TABLE metas ADD COLUMN ano_ciclo INT;
ALTER TABLE metas ADD COLUMN deadline DATE;

ALTER TABLE metas ADD COLUMN status VARCHAR(50) DEFAULT 'PENDENTE' NOT NULL;
ALTER TABLE metas ADD COLUMN p_maximo DECIMAL(10,2);
ALTER TABLE metas ADD COLUMN estimativa_real DECIMAL(10,2);
ALTER TABLE metas ADD COLUMN teto_estimado DECIMAL(10,2);
ALTER TABLE metas ADD COLUMN pontos_atingidos DECIMAL(10,2);

ALTER TABLE metas ADD CONSTRAINT fk_metas_eixo FOREIGN KEY (eixo_id) REFERENCES eixos_tematicos(id);
ALTER TABLE metas ADD CONSTRAINT fk_metas_setor FOREIGN KEY (setor_id) REFERENCES setores(id);
