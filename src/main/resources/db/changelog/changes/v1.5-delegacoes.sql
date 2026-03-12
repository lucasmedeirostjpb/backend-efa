--liquibase formatted sql

--changeset copilot:v1.5-delegacoes
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM pg_tables WHERE tablename = 'efa_delegacoes'
CREATE TABLE efa_delegacoes (
    id BIGINT PRIMARY KEY,
    coordenador_id BIGINT NOT NULL,
    delegado_login_keycloak VARCHAR(255) NOT NULL,
    delegado_nome VARCHAR(255) NOT NULL
);

ALTER TABLE efa_delegacoes
    ADD CONSTRAINT fk_efa_delegacoes_coordenador
    FOREIGN KEY (coordenador_id) REFERENCES efa_coordenadores(id);

ALTER TABLE efa_delegacoes
    ADD CONSTRAINT uk_efa_delegacoes_coordenador_login
    UNIQUE (coordenador_id, delegado_login_keycloak);