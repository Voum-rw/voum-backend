-- V14: Add motorcycle model and color columns to motaris table
ALTER TABLE motaris ADD COLUMN IF NOT EXISTS moto_model VARCHAR(255);
ALTER TABLE motaris ADD COLUMN IF NOT EXISTS moto_color VARCHAR(255);
