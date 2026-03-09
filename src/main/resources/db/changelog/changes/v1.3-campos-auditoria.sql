-- liquibase formatted sql
-- changeset lucasmedeiros:v1.3-campos-auditoria

ALTER TABLE efa_metas ADD COLUMN nivel_dificuldade VARCHAR(50) DEFAULT 'SEM_DIFICULDADES' NOT NULL;
ALTER TABLE efa_metas ADD COLUMN evidencias_auditoria TEXT;
ALTER TABLE efa_metas ADD COLUMN observacoes TEXT;
