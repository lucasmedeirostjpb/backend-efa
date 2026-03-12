--liquibase formatted sql

--changeset copilot:v1.6-delegacoes-email
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:1 SELECT count(*) FROM information_schema.columns WHERE table_name = 'efa_delegacoes' AND column_name = 'delegado_login_keycloak'
ALTER TABLE efa_delegacoes
    RENAME COLUMN delegado_login_keycloak TO delegado_email;

ALTER TABLE efa_delegacoes
    DROP CONSTRAINT uk_efa_delegacoes_coordenador_login;

ALTER TABLE efa_delegacoes
    ADD CONSTRAINT uk_efa_delegacoes_coordenador_email
    UNIQUE (coordenador_id, delegado_email);