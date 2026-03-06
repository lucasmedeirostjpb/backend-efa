--liquibase formatted sql

--changeset polvo:1
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM pg_tables WHERE tablename = 'metas'
CREATE TABLE metas (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao TEXT,
    concluida BOOLEAN DEFAULT FALSE,
    data_criacao TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--changeset polvo:2
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT count(*) FROM information_schema.columns WHERE table_name='metas' AND column_name='usuario_criacao'
ALTER TABLE metas ADD COLUMN usuario_criacao VARCHAR(255);
ALTER TABLE metas ADD COLUMN data_atualizacao TIMESTAMP;
ALTER TABLE metas ADD COLUMN usuario_atualizacao VARCHAR(255);

-- Se já existirem dados, data_criacao precisa ser preenchida. 
-- Mas como o usuário vai zerar o banco, isso não é estritamente necessário agora.
-- No entanto, para o JPA Auditing funcionar bem, as colunas de criação devem ser NOT NULL.
UPDATE metas SET usuario_criacao = 'system' WHERE usuario_criacao IS NULL;
ALTER TABLE metas ALTER COLUMN usuario_criacao SET NOT NULL;
ALTER TABLE metas ALTER COLUMN data_criacao SET NOT NULL;
