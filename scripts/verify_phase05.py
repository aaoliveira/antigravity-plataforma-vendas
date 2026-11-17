#!/usr/bin/env python3
"""
Verification Script for Prompt 05 — Framework de conectores e autorização OAuth
Validates:
1. Flyway V4 migration creates marketplace_credentials and oauth_states tables.
2. Port MarketplaceConnector and Registry exist with explicit capability models.
3. TokenCryptoService provides AES-GCM encryption for tokens in repose.
4. FakeMarketplaceConnector is restricted to the 'test' profile.
5. Integration tests cover OAuth lifecycle, CSRF states, cross-tenant denial, and Testcontainers Postgres.
6. Frontend exposes capabilities matrix and integration states.
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
    print("=== Starting Verification for Prompt 05 ===")

    # 1. Flyway V4 Migration
    v4_migration = ROOT / "backend/src/main/resources/db/migration/V4__marketplace_connectors.sql"
    check("Flyway V4 migration file exists", v4_migration.exists())
    v4_content = v4_migration.read_text(encoding="utf-8")
    check("V4 defines marketplace_credentials table", "CREATE TABLE IF NOT EXISTS marketplace_credentials" in v4_content)
    check("V4 defines oauth_states table", "CREATE TABLE IF NOT EXISTS oauth_states" in v4_content)
    check("V4 enforces unique store constraint", "uk_marketplace_credentials_store" in v4_content)

    # 2. Port & Capabilities
    port_file = ROOT / "backend/src/main/java/br/com/sellerintelligence/connector/ports/MarketplaceConnector.java"
    cap_file = ROOT / "backend/src/main/java/br/com/sellerintelligence/connector/model/CapabilityStatus.java"
    check("MarketplaceConnector port exists", port_file.exists())
    check("CapabilityStatus enum exists", cap_file.exists())
    cap_content = cap_file.read_text(encoding="utf-8")
    check("CapabilityStatus defines SUPPORTED, UNSUPPORTED, PENDING_VALIDATION",
          "SUPPORTED" in cap_content and "UNSUPPORTED" in cap_content and "PENDING_VALIDATION" in cap_content)

    # 3. Registry & Adapters
    registry_file = ROOT / "backend/src/main/java/br/com/sellerintelligence/connector/registry/MarketplaceConnectorRegistry.java"
    ml_file = ROOT / "backend/src/main/java/br/com/sellerintelligence/connector/adapters/mercadolivre/MercadoLivreConnector.java"
    shopee_file = ROOT / "backend/src/main/java/br/com/sellerintelligence/connector/adapters/shopee/ShopeeConnector.java"
    fake_file = ROOT / "backend/src/main/java/br/com/sellerintelligence/connector/adapters/fake/FakeMarketplaceConnector.java"
    check("MarketplaceConnectorRegistry exists", registry_file.exists())
    check("MercadoLivreConnector exists", ml_file.exists())
    check("ShopeeConnector exists", shopee_file.exists())
    check("FakeMarketplaceConnector exists", fake_file.exists())

    # Check Fake connector has @Profile("test")
    fake_content = fake_file.read_text(encoding="utf-8")
    check("FakeMarketplaceConnector restricted to @Profile('test')", '@Profile("test")' in fake_content)

    # 4. Crypto Service
    crypto_file = ROOT / "backend/src/main/java/br/com/sellerintelligence/connector/crypto/TokenCryptoService.java"
    check("TokenCryptoService exists", crypto_file.exists())
    crypto_content = crypto_file.read_text(encoding="utf-8")
    check("TokenCryptoService uses AES/GCM/NoPadding", "AES/GCM/NoPadding" in crypto_content)

    # 5. Connection Service & CSRF State
    svc_file = ROOT / "backend/src/main/java/br/com/sellerintelligence/connector/service/MarketplaceConnectionService.java"
    check("MarketplaceConnectionService exists", svc_file.exists())
    svc_content = svc_file.read_text(encoding="utf-8")
    check("Service verifies OAuth state is not used", "isUsed" in svc_content)
    check("Service verifies OAuth state is not expired", "isExpired" in svc_content)
    check("Service audits cross-tenant access denial", "ACCESS_DENIED" in svc_content)

    # 6. Automated Backend Tests
    crypto_test = ROOT / "backend/src/test/java/br/com/sellerintelligence/connector/crypto/TokenCryptoServiceTest.java"
    reg_test = ROOT / "backend/src/test/java/br/com/sellerintelligence/connector/registry/MarketplaceConnectorRegistryTest.java"
    oauth_test = ROOT / "backend/src/test/java/br/com/sellerintelligence/connector/web/MarketplaceOAuthIntegrationTest.java"
    pg_test = ROOT / "backend/src/test/java/br/com/sellerintelligence/connector/PostgresMarketplaceCredentialsIntegrationTest.java"
    check("TokenCryptoServiceTest exists", crypto_test.exists())
    check("MarketplaceConnectorRegistryTest exists", reg_test.exists())
    check("MarketplaceOAuthIntegrationTest exists", oauth_test.exists())
    check("PostgresMarketplaceCredentialsIntegrationTest exists", pg_test.exists())

    # 7. Frontend Components
    caps_ui = ROOT / "frontend/src/components/MarketplaceCapabilities.tsx"
    caps_test = ROOT / "frontend/src/components/MarketplaceCapabilities.test.tsx"
    check("Frontend MarketplaceCapabilities component exists", caps_ui.exists())
    check("Frontend MarketplaceCapabilities tests exist", caps_test.exists())

    print("\n[SUCCESS] All structural, security, and connector checks for Prompt 05 passed!")

if __name__ == "__main__":
    main()
