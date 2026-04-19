ALTER TABLE reading_plans ADD COLUMN pdf_sha256 VARCHAR(64);

-- Vários NULL são permitidos (planos antigos); valores não nulos devem ser únicos (H2 + PostgreSQL).
CREATE UNIQUE INDEX uq_reading_plans_pdf_sha256 ON reading_plans (pdf_sha256);
