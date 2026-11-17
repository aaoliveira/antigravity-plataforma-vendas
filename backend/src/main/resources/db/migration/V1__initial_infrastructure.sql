-- V1__initial_infrastructure.sql
-- Seller Intelligence Platform: Minimal Infrastructure Initialization
-- No premature business domains or functional entities created at Bootstrap phase.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Schema metadata table for infrastructure verification
CREATE TABLE IF NOT EXISTS platform_infrastructure_metadata (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    component_name VARCHAR(100) NOT NULL UNIQUE,
    version VARCHAR(50) NOT NULL,
    initialized_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL
);

INSERT INTO platform_infrastructure_metadata (component_name, version, status)
VALUES ('platform_foundation', '1.0.0', 'INITIALIZED')
ON CONFLICT (component_name) DO NOTHING;
