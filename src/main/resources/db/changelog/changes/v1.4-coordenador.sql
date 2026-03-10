--liquibase formatted sql

--changeset lucasmedeiros:v1.4-coordenador
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM pg_tables WHERE tablename = 'efa_coordenadores'
CREATE TABLE efa_coordenadores (
    id BIGINT PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    login_keycloak VARCHAR(255) UNIQUE,
    data_criacao TIMESTAMP NOT NULL,
    usuario_criacao VARCHAR(255) NOT NULL,
    data_atualizacao TIMESTAMP,
    usuario_atualizacao VARCHAR(255)
);

--changeset lucasmedeiros:v1.4-coordenador-fk
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.columns WHERE table_name='efa_metas' AND column_name='coordenador_id'
ALTER TABLE efa_metas ADD COLUMN coordenador_id BIGINT;
ALTER TABLE efa_metas ADD CONSTRAINT fk_efa_metas_coordenador FOREIGN KEY (coordenador_id) REFERENCES efa_coordenadores(id);
