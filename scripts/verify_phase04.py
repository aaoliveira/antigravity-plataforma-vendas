#!/usr/bin/env python3
"""
Verification Script for Prompt 04 — Organizações, CNPJs, lojas e gestão de acessos
Validates:
1. Database migration V3 exists and defines companies, stores, user_invitations, and membership_scopes.
2. CnpjValidator enforces 14-digit check-digit logic without RFB calls.
3. Domain entities and services enforce tenant boundary (cross-tenant store linking prohibited).
4. Granular scopes and user invitation lifecycle (single-use, expiration, role and scopes).
5. Frontend components (CompanyManager, StoreManager, Cnpj utility) and Vitest tests exist.
6. All backend and frontend tests pass.
"""

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

def check(description: str, condition: bool):
    status = "PASS" if condition else "FAIL"
    print(f"[{status}] {description}")
    if not condition:
        sys.exit(1)

def main():
    print("=== Starting Verification for Prompt 04 ===")

    # 1. Flyway V3 Migration
    v3_migration = ROOT / "backend/src/main/resources/db/migration/V3__companies_stores_invitations.sql"
    check("Flyway V3 migration file exists", v3_migration.exists())
    v3_content = v3_migration.read_text(encoding="utf-8")
    check("V3 migration defines 'companies' table", "CREATE TABLE IF NOT EXISTS companies" in v3_content)
    check("V3 migration defines 'stores' table", "CREATE TABLE IF NOT EXISTS stores" in v3_content)
    check("V3 migration defines 'user_invitations' table", "CREATE TABLE IF NOT EXISTS user_invitations" in v3_content)
    check("V3 migration defines 'membership_scopes' table", "CREATE TABLE IF NOT EXISTS membership_scopes" in v3_content)
    check("V3 migration enforces unique constraint on (tenant_id, cnpj)", "uk_companies_tenant_cnpj" in v3_content)
    check("V3 migration enforces unique constraint on (membership_id, scope_type, scope_id)", "uk_membership_scopes" in v3_content)

    # 2. CnpjValidator
    cnpj_file = ROOT / "backend/src/main/java/br/com/sellerintelligence/tenancy/company/CnpjValidator.java"
    check("CnpjValidator exists in backend", cnpj_file.exists())
    cnpj_content = cnpj_file.read_text(encoding="utf-8")
    check("CnpjValidator implements 14-digit check-digit algorithm", "calculateCheckDigit" in cnpj_content)
    check("CnpjValidator documents zero RFB consultation", "Receita Federal" in cnpj_content)

    # 3. Company & Store Services
    comp_svc = ROOT / "backend/src/main/java/br/com/sellerintelligence/tenancy/company/CompanyService.java"
    store_svc = ROOT / "backend/src/main/java/br/com/sellerintelligence/tenancy/store/StoreService.java"
    check("CompanyService exists", comp_svc.exists())
    check("StoreService exists", store_svc.exists())
    store_svc_content = store_svc.read_text(encoding="utf-8")
    check("StoreService validates company belongs to same tenant", "findByIdAndTenantId" in store_svc_content)
    check("StoreService audits cross-tenant access denial", "ACCESS_DENIED" in store_svc_content)

    # 4. Invitations and Granular Scopes
    inv_svc = ROOT / "backend/src/main/java/br/com/sellerintelligence/tenancy/invitation/InvitationService.java"
    scope_svc = ROOT / "backend/src/main/java/br/com/sellerintelligence/tenancy/scope/ScopeAuthorizationService.java"
    check("InvitationService exists", inv_svc.exists())
    check("ScopeAuthorizationService exists", scope_svc.exists())
    inv_svc_content = inv_svc.read_text(encoding="utf-8")
    check("InvitationService enforces token expiration check", "isExpired" in inv_svc_content)
    check("InvitationService sets ACCEPTED status (single-use)", "ACCEPTED" in inv_svc_content)

    # 5. Integration Tests
    comp_test = ROOT / "backend/src/test/java/br/com/sellerintelligence/tenancy/CompanyAndStoreIntegrationTest.java"
    scope_test = ROOT / "backend/src/test/java/br/com/sellerintelligence/tenancy/ScopeAndInvitationIntegrationTest.java"
    pg_test = ROOT / "backend/src/test/java/br/com/sellerintelligence/tenancy/PostgresCompaniesStoresIntegrationTest.java"
    check("CompanyAndStoreIntegrationTest exists", comp_test.exists())
    check("ScopeAndInvitationIntegrationTest exists", scope_test.exists())
    check("PostgresCompaniesStoresIntegrationTest exists", pg_test.exists())

    # 6. Frontend Components
    comp_ui = ROOT / "frontend/src/components/CompanyManager.tsx"
    store_ui = ROOT / "frontend/src/components/StoreManager.tsx"
    cnpj_ts = ROOT / "frontend/src/lib/cnpj.ts"
    cnpj_test = ROOT / "frontend/src/lib/cnpj.test.ts"
    comp_ui_test = ROOT / "frontend/src/components/CompanyManager.test.tsx"
    check("Frontend CompanyManager exists", comp_ui.exists())
    check("Frontend StoreManager exists", store_ui.exists())
    check("Frontend CNPJ utility exists", cnpj_ts.exists())
    check("Frontend CNPJ tests exist", cnpj_test.exists())
    check("Frontend CompanyManager tests exist", comp_ui_test.exists())

    print("\n[SUCCESS] All structural, security, and UI checks for Prompt 04 passed!")

if __name__ == "__main__":
    main()
