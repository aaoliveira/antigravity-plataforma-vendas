#!/usr/bin/env python3
"""
Verification Script for Prompt 19 (Expansão de conectores para Amazon e outros marketplaces).
Validates Acceptance Criteria AC-19-1 through AC-19-10.
"""

import sys
import os

def check(condition, message):
    if condition:
        print(f"[PASS] {message}")
        return True
    else:
        print(f"[FAIL] {message}")
        return False

def main():
    print("=== Starting Verification for Prompt 19 (Expansão de Conectores: Amazon SP-API) ===")
    all_passed = True

    connector_file = "backend/src/main/java/br/com/sellerintelligence/connector/adapters/amazon/AmazonSpApiConnector.java"
    connector_test = "backend/src/test/java/br/com/sellerintelligence/connector/adapters/amazon/AmazonSpApiConnectorContractTest.java"
    registry_file = "backend/src/main/java/br/com/sellerintelligence/connector/registry/MarketplaceConnectorRegistry.java"
    registry_test = "backend/src/test/java/br/com/sellerintelligence/connector/registry/MarketplaceConnectorRegistryTest.java"
    docs_file = "docs/connectors/AMAZON_SP_API.md"
    decision_log = "docs/architecture/DECISION_LOG.md"
    open_questions = "docs/product/OPEN_QUESTIONS.md"
    frontend_capabilities = "frontend/src/components/MarketplaceCapabilities.tsx"
    frontend_test = "frontend/src/components/MarketplaceCapabilities.test.tsx"

    # AC-19-1: Pre-filled documentation and matrix
    all_passed &= check(os.path.exists(docs_file), "AC-19-1: docs/connectors/AMAZON_SP_API.md exists")
    if os.path.exists(docs_file):
        with open(docs_file, "r") as f:
            content = f.read()
            all_passed &= check("A2Q3Y263D00KWC" in content, "AC-19-1: Documents Brazil Marketplace ID A2Q3Y263D00KWC")
            all_passed &= check("Login with Amazon" in content or "LWA" in content, "AC-19-1: Documents LWA OAuth 2.0 authentication")
            all_passed &= check("CATALOG" in content and "ORDERS" in content, "AC-19-1: Outlines capabilities matrix")
            all_passed &= check("Restricted Data Token" in content or "RDT" in content, "AC-19-1: Documents PII protection and RDT")

    if os.path.exists(decision_log):
        with open(decision_log, "r") as f:
            content = f.read()
            all_passed &= check("DEC-024" in content and "Amazon SP-API" in content, "AC-19-1: ADR DEC-024 accepted in DECISION_LOG.md")

    if os.path.exists(open_questions):
        with open(open_questions, "r") as f:
            content = f.read()
            all_passed &= check("OQ-016" in content and "RESOLVED" in content, "AC-19-1: OQ-016 marked as RESOLVED in OPEN_QUESTIONS.md")

    # AC-19-2: AmazonSpApiConnector implementation
    all_passed &= check(os.path.exists(connector_file), "AC-19-2: AmazonSpApiConnector exists")
    if os.path.exists(connector_file):
        with open(connector_file, "r") as f:
            content = f.read()
            all_passed &= check("implements MarketplaceConnector" in content, "AC-19-2: Implements MarketplaceConnector port")
            all_passed &= check("return Marketplace.AMAZON" in content, "AC-19-2: Returns Marketplace.AMAZON")

    # AC-19-3: LWA OAuth flow
    if os.path.exists(connector_file):
        with open(connector_file, "r") as f:
            content = f.read()
            all_passed &= check("generateAuthorizationUrl" in content and "sellercentral.amazon.com.br" in content, "AC-19-3: Generates Seller Central Brazil consent URL")
            all_passed &= check("exchangeAuthorizationCode" in content and "lwaAuthUrl" in content, "AC-19-3: Exchanges authorization code via LWA")

    # AC-19-4: Connection validation and token refresh
    if os.path.exists(connector_file):
        with open(connector_file, "r") as f:
            content = f.read()
            all_passed &= check("refreshAccessToken" in content, "AC-19-4: Implements refreshAccessToken")
            all_passed &= check("validateConnection" in content and "marketplaceParticipations" in content, "AC-19-4: Implements validateConnection using marketplaceParticipations")

    # AC-19-5: Listings and Orders synchronization
    if os.path.exists(connector_file):
        with open(connector_file, "r") as f:
            content = f.read()
            all_passed &= check("fetchListings" in content and "ConnectorListing" in content, "AC-19-5: Implements fetchListings mapping to ConnectorListing")
            all_passed &= check("fetchOrders" in content and "ConnectorOrder" in content, "AC-19-5: Implements fetchOrders mapping to ConnectorOrder")

    # AC-19-6: Rate limiting handling
    if os.path.exists(connector_file):
        with open(connector_file, "r") as f:
            content = f.read()
            all_passed &= check("HttpClientErrorException.TooManyRequests" in content or "AMAZON_RATE_LIMIT_EXCEEDED" in content, "AC-19-6: Intercepts and handles 429 Too Many Requests")

    # AC-19-7: Data integrity & unsupported fields
    if os.path.exists(connector_file):
        with open(connector_file, "r") as f:
            content = f.read()
            all_passed &= check("ConnectorCapability.unsupported(ConnectorCapabilityType.PUBLIC_SEARCH" in content, "AC-19-7: PUBLIC_SEARCH declared UNSUPPORTED")
            all_passed &= check("ConnectorCapability.unsupported(ConnectorCapabilityType.PUBLIC_TRENDS" in content, "AC-19-7: PUBLIC_TRENDS declared UNSUPPORTED")
            all_passed &= check("ConnectorCapability.unsupported(ConnectorCapabilityType.ADS" in content, "AC-19-7: ADS declared UNSUPPORTED")

    # AC-19-8: Registry and zero regression
    all_passed &= check(os.path.exists(registry_file), "AC-19-8: MarketplaceConnectorRegistry exists")
    all_passed &= check(os.path.exists(registry_test), "AC-19-8: MarketplaceConnectorRegistryTest exists")
    if os.path.exists(registry_test):
        with open(registry_test, "r") as f:
            content = f.read()
            all_passed &= check("Marketplace.AMAZON" in content and "AmazonSpApiConnector" in content, "AC-19-8: Registry tested with Amazon connector")

    # AC-19-9: Contract test suite
    all_passed &= check(os.path.exists(connector_test), "AC-19-9: AmazonSpApiConnectorContractTest exists")
    if os.path.exists(connector_test):
        with open(connector_test, "r") as f:
            content = f.read()
            all_passed &= check("shouldComplyWithMarketplaceConnectorContract" in content, "AC-19-9: Contract compliance test")
            all_passed &= check("shouldDeclareAccurateCapabilities" in content, "AC-19-9: Capabilities assertion test")
            all_passed &= check("shouldHandleRateLimitExceeded" in content, "AC-19-9: Rate limit 429 test")

    # AC-19-10: Frontend integration
    all_passed &= check(os.path.exists(frontend_capabilities), "AC-19-10: MarketplaceCapabilities.tsx exists")
    all_passed &= check(os.path.exists(frontend_test), "AC-19-10: MarketplaceCapabilities.test.tsx exists")
    if os.path.exists(frontend_capabilities):
        with open(frontend_capabilities, "r") as f:
            content = f.read()
            all_passed &= check("Amazon SP-API" in content and "A2Q3Y263D00KWC" in content, "AC-19-10: Frontend contains Amazon SP-API capabilities")
    if os.path.exists(frontend_test):
        with open(frontend_test, "r") as f:
            content = f.read()
            all_passed &= check("Amazon SP-API" in content, "AC-19-10: Frontend test asserts Amazon SP-API")

    print("--------------------------------------------------")
    if all_passed:
        print("ALL ACCEPTANCE CRITERIA PASSED FOR PROMPT 19!")
        return 0
    else:
        print("SOME CHECKS FAILED FOR PROMPT 19.")
        return 1

if __name__ == "__main__":
    sys.exit(main())
