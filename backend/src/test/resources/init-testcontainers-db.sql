-- =============================================================================
-- Inicialização de Usuário da Aplicação com Menor Privilégio no PostgreSQL
-- Garante que a aplicação conecte como usuário não-superuser (NOSUPERUSER),
-- ativando a aplicação estrita de Row-Level Security (RLS) sem bypass.
-- =============================================================================

CREATE ROLE seller_app WITH LOGIN PASSWORD 'seller_app' NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS;
GRANT ALL ON SCHEMA public TO seller_app;
GRANT ALL PRIVILEGES ON DATABASE test TO seller_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO seller_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO seller_app;
