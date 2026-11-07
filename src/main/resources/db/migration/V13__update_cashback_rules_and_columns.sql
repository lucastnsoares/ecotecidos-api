-- Renomear a coluna no banco de dados
ALTER TABLE cashback_transaction RENAME COLUMN cashback_percentage TO points_per_kg;

-- Tabela para armazenar a regra global configurável pelo Admin
CREATE TABLE cashback_rules (
    id BIGSERIAL PRIMARY KEY,
    points_per_kg DECIMAL(5, 2) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Inserir a regra padrão inicial (10 pontos por kg)
INSERT INTO cashback_rules (points_per_kg, updated_at) VALUES (10.00, CURRENT_TIMESTAMP);