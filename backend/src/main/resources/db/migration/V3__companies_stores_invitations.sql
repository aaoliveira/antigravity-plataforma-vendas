-- V3__companies_stores_invitations.sql
-- Seller Intelligence Platform: Companies, Stores/Marketplace Accounts, Invitations and Granular Scopes

CREATE TABLE IF NOT EXISTS companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    cnpj VARCHAR(18) NOT NULL,
    trade_name VARCHAR(255) NOT NULL,
    legal_name VARCHAR(255),
    tax_regime VARCHAR(50) NOT NULL DEFAULT 'SIMPLES_NACIONAL',
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_companies_tenant_cnpj UNIQUE (tenant_id, cnpj)
);

CREATE INDEX IF NOT EXISTS idx_companies_tenant_id ON companies(tenant_id);
CREATE INDEX IF NOT EXISTS idx_companies_cnpj ON companies(cnpj);

CREATE TABLE IF NOT EXISTS stores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE RESTRICT,
    marketplace VARCHAR(50) NOT NULL,
    internal_name VARCHAR(255) NOT NULL,
    external_account_id VARCHAR(255),
    connection_status VARCHAR(50) NOT NULL DEFAULT 'DISCONNECTED',
    metadata TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_stores_tenant_id ON stores(tenant_id);
CREATE INDEX IF NOT EXISTS idx_stores_company_id ON stores(company_id);
CREATE INDEX IF NOT EXISTS idx_stores_marketplace ON stores(marketplace);

CREATE TABLE IF NOT EXISTS user_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    allowed_company_ids TEXT,
    allowed_store_ids TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    invited_by_user_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_invitations_tenant_id ON user_invitations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_invitations_token ON user_invitations(token);
CREATE INDEX IF NOT EXISTS idx_invitations_email ON user_invitations(email);

CREATE TABLE IF NOT EXISTS membership_scopes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    membership_id UUID NOT NULL REFERENCES memberships(id) ON DELETE CASCADE,
    scope_type VARCHAR(50) NOT NULL,
    scope_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_membership_scopes UNIQUE (membership_id, scope_type, scope_id)
);

CREATE INDEX IF NOT EXISTS idx_membership_scopes_membership_id ON membership_scopes(membership_id);
CREATE INDEX IF NOT EXISTS idx_membership_scopes_type_id ON membership_scopes(scope_type, scope_id);
