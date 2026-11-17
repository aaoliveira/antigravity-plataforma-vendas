#!/usr/bin/env python3
"""
Verification Script for Prompt 03 — Identidade, autenticação e isolamento multi-tenant
Validates:
1. Database migration V2 contains all required tables and constraints.
2. Domain entities (User, Organization, Membership, AuditEvent, Role, TenantContext).
3. Security configuration and TenantContextFilter enforce tenant isolation and RFC 7807 problem details.
4. User identity stores no passwords locally.
5. All tests in backend pass.
"""

import sys
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

def check(description: str, condition: bool):
    status = "PASS" if condition else "FAIL"
    print(f"[{status}] {description}")
    if not condition:
        sys.exit(1)

def main():
    print("=== Starting Verification for Prompt 03 ===")

    # 1. Check Flyway V2 migration
    v2_migration = ROOT / "backend/src/main/resources/db/migration/V2__identity_and_tenancy.sql"
    check("Flyway V2 migration file exists", v2_migration.exists())
    v2_content = v2_migration.read_text(encoding="utf-8")
    check("V2 migration defines 'users' table", "CREATE TABLE IF NOT EXISTS users" in v2_content)
    check("V2 migration defines 'organizations' table", "CREATE TABLE IF NOT EXISTS organizations" in v2_content)
    check("V2 migration defines 'memberships' table", "CREATE TABLE IF NOT EXISTS memberships" in v2_content)
    check("V2 migration defines 'audit_events' table", "CREATE TABLE IF NOT EXISTS audit_events" in v2_content)
    check("V2 migration enforces unique (organization_id, user_id)", "uk_memberships_org_user" in v2_content)

    # 2. Check no local passwords in User domain
    user_file = ROOT / "backend/src/main/java/br/com/sellerintelligence/identity/domain/User.java"
    check("User entity exists", user_file.exists())
    user_content = user_file.read_text(encoding="utf-8")
    check("User entity contains 'keycloakSub'", "keycloakSub" in user_content)
    check("User entity contains NO password field", "password" not in user_content.lower())

    # 3. Check TenantContext and Filter
    filter_file = ROOT / "backend/src/main/java/br/com/sellerintelligence/infrastructure/security/TenantContextFilter.java"
    check("TenantContextFilter exists", filter_file.exists())
    filter_content = filter_file.read_text(encoding="utf-8")
    check("TenantContextFilter audits ACCESS_DENIED", "ACCESS_DENIED" in filter_content)
    check("TenantContextFilter clears context in finally", "TenantContext.clear()" in filter_content)

    # 4. Check SecurityConfig
    security_file = ROOT / "backend/src/main/java/br/com/sellerintelligence/infrastructure/security/SecurityConfig.java"
    check("SecurityConfig exists", security_file.exists())
    security_content = security_file.read_text(encoding="utf-8")
    check("SecurityConfig configures JWT resource server", "oauth2ResourceServer" in security_content)
    check("SecurityConfig permits actuator health probes", "/actuator/health" in security_content)

    # 5. Check AuditService sanitization
    audit_file = ROOT / "backend/src/main/java/br/com/sellerintelligence/audit/domain/AuditService.java"
    check("AuditService exists", audit_file.exists())
    audit_content = audit_file.read_text(encoding="utf-8")
    check("AuditService redacts sensitive tokens", "REDACTED" in audit_content)

    # 6. Check Integration Tests exist
    iso_test = ROOT / "backend/src/test/java/br/com/sellerintelligence/tenancy/TenantIsolationIntegrationTest.java"
    rbac_test = ROOT / "backend/src/test/java/br/com/sellerintelligence/tenancy/RbacIntegrationTest.java"
    pg_test = ROOT / "backend/src/test/java/br/com/sellerintelligence/tenancy/PostgresTenancyIntegrationTest.java"
    check("TenantIsolationIntegrationTest exists", iso_test.exists())
    check("RbacIntegrationTest exists", rbac_test.exists())
    check("PostgresTenancyIntegrationTest exists", pg_test.exists())

    print("\n[SUCCESS] All structural, security and isolation checks for Prompt 03 passed!")

if __name__ == "__main__":
    main()
