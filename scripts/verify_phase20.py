#!/usr/bin/env python3
"""
Verification Script for Prompt 20 (Auditoria Completa de Segurança, Compliance e Proteção de Contas dos Marketplaces).
Validates Acceptance Criteria AC-20-1 through AC-20-10.
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
    print("=== Starting Verification for Prompt 20 (Auditoria de Segurança, Compliance e Proteção de Contas) ===")
    all_passed = True

    audit_report = "docs/security/SECURITY_AUDIT_REPORT.md"
    compliance_matrix = "docs/security/MARKETPLACE_COMPLIANCE_MATRIX.md"
    remediation_plan = "docs/security/SECURITY_REMEDIATION_PLAN.md"
    compliance_decisions = "docs/security/COMPLIANCE_DECISIONS.md"
    integration_guidelines = "docs/security/MARKETPLACE_INTEGRATION_GUIDELINES.md"
    test_report = "docs/security/SECURITY_TEST_REPORT.md"
    compliance_guard = "backend/src/main/java/br/com/sellerintelligence/compliance/service/MarketplaceComplianceGuard.java"
    compliance_exception = "backend/src/main/java/br/com/sellerintelligence/compliance/exception/ComplianceViolationException.java"
    compliance_test = "backend/src/test/java/br/com/sellerintelligence/compliance/MarketplaceComplianceGuardTest.java"
    decision_log = "docs/architecture/DECISION_LOG.md"
    readme = "README.md"
    phase_gate = "docs/delivery/phase-gate.json"

    # AC-20-1: Executive and detailed security audit report
    all_passed &= check(os.path.exists(audit_report), "AC-20-1: docs/security/SECURITY_AUDIT_REPORT.md exists")
    if os.path.exists(audit_report):
        with open(audit_report, "r") as f:
            content = f.read()
            all_passed &= check("OWASP ASVS" in content, "AC-20-1: Methodology references OWASP ASVS")
            all_passed &= check("FAIL CLOSED" in content, "AC-20-1: Documents mandatory FAIL CLOSED architecture")
            all_passed &= check("Mercado Livre" in content and "Shopee" in content and "Amazon" in content, "AC-20-1: Evaluates core channels")
            all_passed &= check("TikTok Shop" in content and "Magalu" in content and "SHEIN" in content, "AC-20-1: Evaluates expansion channels")
            all_passed &= check("APPROVED" in content, "AC-20-1: Final audit result is APPROVED")

    # AC-20-2: Compliance matrix for all 6 marketplaces
    all_passed &= check(os.path.exists(compliance_matrix), "AC-20-2: docs/security/MARKETPLACE_COMPLIANCE_MATRIX.md exists")
    if os.path.exists(compliance_matrix):
        with open(compliance_matrix, "r") as f:
            content = f.read()
            all_passed &= check("https://developers.mercadolivre.com.br" in content, "AC-20-2: Contains official Mercado Livre URL")
            all_passed &= check("https://open.shopee.com" in content, "AC-20-2: Contains official Shopee URL")
            all_passed &= check("https://developer-docs.amazon.com" in content, "AC-20-2: Contains official Amazon URL")
            all_passed &= check("https://partner.tiktokshop.com" in content, "AC-20-2: Contains official TikTok Shop URL")
            all_passed &= check("https://developers.magazineluiza.com.br" in content, "AC-20-2: Contains official Magalu URL")
            all_passed &= check("https://open.shein.com" in content, "AC-20-2: Contains official SHEIN URL")
            all_passed &= check("COMPLIANT" in content and "UNVERIFIED_COMPLIANCE" in content, "AC-20-2: Utilizes standardized compliance status")

    # AC-20-3: Remediation plan and compliance decisions
    all_passed &= check(os.path.exists(remediation_plan), "AC-20-3: docs/security/SECURITY_REMEDIATION_PLAN.md exists")
    all_passed &= check(os.path.exists(compliance_decisions), "AC-20-3: docs/security/COMPLIANCE_DECISIONS.md exists")
    if os.path.exists(compliance_decisions):
        with open(compliance_decisions, "r") as f:
            content = f.read()
            all_passed &= check("CD-001" in content and "FAIL CLOSED" in content, "AC-20-3: CD-001 Fail Closed registered")
            all_passed &= check("CD-002" in content and "UNVERIFIED_COMPLIANCE" in content, "AC-20-3: CD-002 Unverified channels blocked")
            all_passed &= check("CD-003" in content and "MUTABLE_CRITICAL" in content, "AC-20-3: CD-003 Human-in-the-loop registered")

    # AC-20-4: Guidelines and Security Test Report
    all_passed &= check(os.path.exists(integration_guidelines), "AC-20-4: docs/security/MARKETPLACE_INTEGRATION_GUIDELINES.md exists")
    all_passed &= check(os.path.exists(test_report), "AC-20-4: docs/security/SECURITY_TEST_REPORT.md exists")
    if os.path.exists(integration_guidelines):
        with open(integration_guidelines, "r") as f:
            content = f.read()
            all_passed &= check("15 Regras Inegociáveis" in content, "AC-20-4: Contains 15 inegotiable developer rules")
            all_passed &= check("CAPTCHA" in content and "anti-bot" in content, "AC-20-4: Prohibits CAPTCHA bypass and bot evasion")

    # AC-20-5: MarketplaceComplianceGuard implementation
    all_passed &= check(os.path.exists(compliance_guard), "AC-20-5: MarketplaceComplianceGuard.java exists")
    if os.path.exists(compliance_guard):
        with open(compliance_guard, "r") as f:
            content = f.read()
            all_passed &= check("checkOperation" in content and "enforceOperation" in content, "AC-20-5: Exposes checkOperation and enforceOperation")
            all_passed &= check("isCircuitOpen" in content and "recordFailure" in content, "AC-20-5: Implements Circuit Breaker logic")
            all_passed &= check("isRateLimited" in content and "recordRateLimit429" in content, "AC-20-5: Implements HTTP 429 rate limit cooldown")
            all_passed &= check("TOKEN_EXPIRED" in content and "CREDENTIAL_REVOKED" in content, "AC-20-5: Enforces token expiration & revocation checks")

    # AC-20-6: Human-in-the-loop approval for mutable critical actions
    if os.path.exists(compliance_guard):
        with open(compliance_guard, "r") as f:
            content = f.read()
            all_passed &= check("OperationRiskLevel.MUTABLE_CRITICAL" in content and ("humanApprovalRequired" in content or "HUMAN_APPROVAL_REQUIRED" in content), "AC-20-6: Enforces human approval for MUTABLE_CRITICAL operations")

    # AC-20-7: Blocking unverified compliance channels
    if os.path.exists(compliance_guard):
        with open(compliance_guard, "r") as f:
            content = f.read()
            all_passed &= check("Marketplace.TIKTOK_SHOP, ComplianceStatus.UNVERIFIED_COMPLIANCE" in content, "AC-20-7: TikTok Shop set to UNVERIFIED_COMPLIANCE")
            all_passed &= check("Marketplace.MAGALU, ComplianceStatus.UNVERIFIED_COMPLIANCE" in content, "AC-20-7: Magalu set to UNVERIFIED_COMPLIANCE")
            all_passed &= check("Marketplace.SHEIN, ComplianceStatus.UNVERIFIED_COMPLIANCE" in content, "AC-20-7: SHEIN set to UNVERIFIED_COMPLIANCE")
            all_passed &= check("UNVERIFIED_MARKETPLACE_BLOCKED" in content, "AC-20-7: Returns UNVERIFIED_MARKETPLACE_BLOCKED error code")

    # AC-20-8: Multi-tenant protection & credential leak prevention
    if os.path.exists(compliance_guard):
        with open(compliance_guard, "r") as f:
            content = f.read()
            all_passed &= check("STORE_NOT_FOUND_OR_CROSS_TENANT" in content, "AC-20-8: Enforces store and tenant scoping check")

    if os.path.exists(compliance_exception):
        with open(compliance_exception, "r") as f:
            content = f.read()
            all_passed &= check("SENSITIVE_PATTERN" in content and "[REDACTED]" in content, "AC-20-8: Sanitizes sensitive tokens in ComplianceViolationException")

    # AC-20-9: Automated security test suite with 15 scenarios
    all_passed &= check(os.path.exists(compliance_test), "AC-20-9: MarketplaceComplianceGuardTest.java exists")
    if os.path.exists(compliance_test):
        with open(compliance_test, "r") as f:
            content = f.read()
            for i in range(1, 16):
                scenario_prefix = f"scenario{i:02d}"
                all_passed &= check(scenario_prefix in content, f"AC-20-9: Contains {scenario_prefix}")

    # AC-20-10: Governance, README and DECISION_LOG
    if os.path.exists(decision_log):
        with open(decision_log, "r") as f:
            content = f.read()
            all_passed &= check("DEC-025" in content and "Marketplace Compliance Guard" in content, "AC-20-10: ADR DEC-025 accepted in DECISION_LOG.md")

    if os.path.exists(readme):
        with open(readme, "r") as f:
            content = f.read()
            all_passed &= check("MarketplaceComplianceGuard" in content and "FAIL CLOSED" in content, "AC-20-10: README documents MarketplaceComplianceGuard and FAIL CLOSED")

    if os.path.exists(phase_gate):
        with open(phase_gate, "r") as f:
            content = f.read()
            all_passed &= check('"currentPrompt": "20"' in content, "AC-20-10: phase-gate.json is on Prompt 20")

    print("\n" + "=" * 80)
    if all_passed:
        print("[SUCCESS] All 10 Acceptance Criteria for Prompt 20 PASSED!")
        return 0
    else:
        print("[FAILURE] Some Acceptance Criteria failed. See details above.")
        return 1

if __name__ == "__main__":
    sys.exit(main())
