-- =============================================================================
-- Migration: V13__companies_and_account_links.sql
-- Description: Tabelas de empresas (Company/BusinessUnit) e vínculos de contas conectadas
-- Engine: PostgreSQL 17 / 18 / H2 in PostgreSQL Mode
-- =============================================================================

-- 1. Tabela de Empresas (companies)
CREATE TABLE IF NOT EXISTS companies (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    corporate_name VARCHAR(255) NOT NULL,
    trade_name VARCHAR(255),
    cnpj VARCHAR(14) NOT NULL,
    state_registration VARCHAR(50),
    tax_regime VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_companies_tenant_cnpj UNIQUE (tenant_id, cnpj)
);

CREATE INDEX IF NOT EXISTS idx_companies_tenant_active 
    ON companies (tenant_id, is_active);

-- 2. Tabela de Vínculos entre Empresas e Contas de Marketplaces (company_seller_account_links)
CREATE TABLE IF NOT EXISTS company_seller_account_links (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    company_id UUID NOT NULL REFERENCES companies(id),
    seller_account_id UUID NOT NULL REFERENCES seller_accounts(id),
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version INT NOT NULL DEFAULT 1,
    notes TEXT,
    linked_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_company_account_links_lookup 
    ON company_seller_account_links (tenant_id, seller_account_id, is_active);

CREATE INDEX IF NOT EXISTS idx_company_account_links_company 
    ON company_seller_account_links (tenant_id, company_id, is_active);
