-- Garante FK para processamento_ressarcimento (bases onde V6 possa ter ficado incompleta).
IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys fk
    INNER JOIN sys.tables t ON fk.parent_object_id = t.object_id
    WHERE t.name = 'auditoria_entrada_consumida'
      AND fk.name = 'fk_aud_ent_cons_proc_ress'
)
BEGIN
    ALTER TABLE auditoria_entrada_consumida
    ADD CONSTRAINT fk_aud_ent_cons_proc_ress
        FOREIGN KEY (processamento_ressarcimento_id) REFERENCES processamento_ressarcimento(id);
END
