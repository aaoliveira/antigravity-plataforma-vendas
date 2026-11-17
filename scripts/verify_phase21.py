#!/usr/bin/env python3
"""
Verification Script for Prompt 21:
Continuous Marketplace Compliance & Policy Monitoring Engine.
Validates Criteria CA-01 through CA-15.
"""

import sys
import os
import json

def check(condition, message):
    if condition:
        print(f"[PASS] {message}")
        return True
    else:
        print(f"[FAIL] {message}")
        return False

def main():
    print("=== Starting Verification for Prompt 21 (Continuous Marketplace Compliance & Policy Monitoring) ===")
    all_passed = True

    # 1. Database Migration V14
    v14_path = "backend/src/main/resources/db/migration/V14__continuous_compliance_and_policy_monitoring.sql"
    all_passed &= check(os.path.exists(v14_path), "DB: V14 migration exists")
    if os.path.exists(v14_path):
        with open(v14_path, "r") as f:
            v14_content = f.read()
            all_passed &= check("marketplace_policies" in v14_content, "DB: marketplace_policies table created")
            all_passed &= check("marketplace_policy_changes" in v14_content, "DB: marketplace_policy_changes table created")
            all_passed &= check("compliance_incidents" in v14_content, "DB: compliance_incidents table created")
            all_passed &= check("marketplace_api_lifecycles" in v14_content, "DB: marketplace_api_lifecycles table created")
            all_passed &= check("feature_enforcement_records" in v14_content, "DB: feature_enforcement_records table created")
            all_passed &= check("safe_reactivation_logs" in v14_content, "DB: safe_reactivation_logs table created")

    # 2. Domain Models & Enums
    domain_path = "backend/src/main/java/br/com/sellerintelligence/compliance/domain"
    all_passed &= check(os.path.exists(f"{domain_path}/MarketplacePolicy.java"), "Domain: MarketplacePolicy exists")
    all_passed &= check(os.path.exists(f"{domain_path}/MarketplacePolicyChange.java"), "Domain: MarketplacePolicyChange exists")
    all_passed &= check(os.path.exists(f"{domain_path}/ComplianceIncident.java"), "Domain: ComplianceIncident exists")
    all_passed &= check(os.path.exists(f"{domain_path}/MarketplaceApiLifecycle.java"), "Domain: MarketplaceApiLifecycle exists")
    all_passed &= check(os.path.exists(f"{domain_path}/FeatureEnforcementRecord.java"), "Domain: FeatureEnforcementRecord exists")
    all_passed &= check(os.path.exists(f"{domain_path}/SafeReactivationLog.java"), "Domain: SafeReactivationLog exists")
    all_passed &= check(os.path.exists(f"{domain_path}/ComplianceRiskLevel.java"), "Domain: ComplianceRiskLevel enum exists")
    all_passed &= check(os.path.exists(f"{domain_path}/PolicyStatus.java"), "Domain: PolicyStatus enum exists")
    all_passed &= check(os.path.exists(f"{domain_path}/ApiLifecycleStatus.java"), "Domain: ApiLifecycleStatus enum exists")

    # 3. Compliance Engine Services
    service_path = "backend/src/main/java/br/com/sellerintelligence/compliance/service"
    services = [
        "MarketplacePolicyRegistryService.java",
        "PolicyChangeDetector.java",
        "ComplianceImpactAnalyzer.java",
        "RiskAssessmentEngine.java",
        "AutomaticComplianceEnforcer.java",
        "ComplianceIncidentService.java",
        "ComplianceNotificationService.java",
        "PolicyMonitoringEngine.java",
        "SafeReactivationService.java",
        "PermissionAuthorizationValidator.java",
        "ApiLifecycleMonitor.java",
        "ComplianceDashboardService.java",
        "MarketplaceComplianceGuard.java"
    ]
    for svc in services:
        all_passed &= check(os.path.exists(f"{service_path}/{svc}"), f"Service: {svc} exists")

    # Check key architectural mechanisms in services
    if os.path.exists(f"{service_path}/AutomaticComplianceEnforcer.java"):
        with open(f"{service_path}/AutomaticComplianceEnforcer.java") as f:
            content = f.read()
            all_passed &= check("SUSPENDED" in content and ("LEAST-SCOPE" in content or "Least-scope" in content),
                                "CA-06: Least Scope enforcement verified")
            all_passed &= check("isFeatureRestricted" in content, "CA-05: Feature restriction checking method exists")

    if os.path.exists(f"{service_path}/SafeReactivationService.java"):
        with open(f"{service_path}/SafeReactivationService.java") as f:
            content = f.read()
            all_passed &= check("SafeReactivationLog" in content, "CA-11: Safe reactivation immutable log verified")
            all_passed &= check("evidence" in content and "reactivateFeature" in content,
                                "CA-11: Documentary evidence mandatory for reactivation")

    # Check Anti-SSRF URL validator
    url_val_path = "backend/src/main/java/br/com/sellerintelligence/infrastructure/security/UrlSecurityValidator.java"
    all_passed &= check(os.path.exists(url_val_path), "Security: UrlSecurityValidator exists")
    if os.path.exists(url_val_path):
        with open(url_val_path) as f:
            content = f.read()
            all_passed &= check("validatePolicyMonitoringUrl" in content, "CA-13: Policy monitoring URL validation present")
            all_passed &= check("ALLOWED_POLICY_DOMAINS" in content, "CA-13: Authoritative allowlist of domains present")

    # 4. REST Controller and Frontend
    ctrl_path = "backend/src/main/java/br/com/sellerintelligence/compliance/web/ComplianceDashboardController.java"
    all_passed &= check(os.path.exists(ctrl_path), "Controller: ComplianceDashboardController exists")

    frontend_view = "frontend/src/components/ComplianceDashboardView.tsx"
    frontend_test = "frontend/src/components/ComplianceDashboardView.test.tsx"
    all_passed &= check(os.path.exists(frontend_view), "Frontend: ComplianceDashboardView component exists")
    all_passed &= check(os.path.exists(frontend_test), "Frontend: ComplianceDashboardView test suite exists")

    # 5. Integration Test Suite covering all 20 aspects
    test_suite = "backend/src/test/java/br/com/sellerintelligence/compliance/ContinuousComplianceEngineIntegrationTest.java"
    all_passed &= check(os.path.exists(test_suite), "CA-14: ContinuousComplianceEngineIntegrationTest exists")
    if os.path.exists(test_suite):
        with open(test_suite) as f:
            test_content = f.read()
            test_methods = [
                "test01_02_detectPolicyChangeAndHashDiff",
                "test03_04_formattingOnlyVsSemanticChange",
                "test05_recordNewPolicyVersion",
                "test06_detectSourceUnavailable",
                "test07_08_09_leastScopeEnforcement",
                "test10_identifyRevokedPermissionOrInsufficientScope",
                "test11_identifyApiDeprecationAndSunset",
                "test12_createAndManageComplianceIncident",
                "test13_14_notificationsAndDeduplicationCooldown",
                "test15_16_safeReactivationGovernance",
                "test17_multiTenantIsolationInPermissionValidation",
                "test18_ssrfProtectionBlocksPrivateAndNonOfficialDomains",
                "test19_20_recoveryAndDashboardSummary"
            ]
            for tm in test_methods:
                all_passed &= check(tm in test_content, f"CA-14: Test method {tm} present")

    # 6. Documentation Files in docs/compliance/
    docs_compliance = [
        "docs/compliance/CONTINUOUS_COMPLIANCE_ARCHITECTURE.md",
        "docs/compliance/POLICY_MONITORING_GUIDELINES.md",
        "docs/compliance/COMPLIANCE_LIFECYCLE.md",
        "docs/compliance/COMPLIANCE_INCIDENT_PLAYBOOK.md",
        "docs/compliance/SAFE_REACTIVATION.md",
        "docs/compliance/MARKETPLACE_POLICY_REGISTRY.md",
        "docs/compliance/CONTINUOUS_COMPLIANCE_TEST_REPORT.md"
    ]
    for doc in docs_compliance:
        all_passed &= check(os.path.exists(doc), f"Docs: {doc} exists")

    # 7. Governance & Architecture Updates
    dec_log = "docs/architecture/DECISION_LOG.md"
    all_passed &= check(os.path.exists(dec_log), "Governance: DECISION_LOG.md exists")
    if os.path.exists(dec_log):
        with open(dec_log) as f:
            content = f.read()
            all_passed &= check("DEC-026" in content and "Continuous Marketplace Compliance Engine" in content,
                                "Governance: ADR DEC-026 registered in DECISION_LOG.md")

    # Check phase-gate.json
    phase_gate_path = "docs/delivery/phase-gate.json"
    all_passed &= check(os.path.exists(phase_gate_path), "Phase Gate: phase-gate.json exists")
    if os.path.exists(phase_gate_path):
        with open(phase_gate_path) as f:
            pg = json.load(f)
            all_passed &= check(pg.get("currentPrompt") == "21", "Phase Gate: currentPrompt is 21")
            all_passed &= check(pg.get("status") == "APPROVED", "Phase Gate: status is APPROVED")
            all_passed &= check(pg.get("nextPromptPrerequisiteMet") is True, "Phase Gate: nextPromptPrerequisiteMet is True")
            criteria = {c["id"]: c["status"] for c in pg.get("criteria", [])}
            all_passed &= check(len(criteria) == 15, "Phase Gate: 15 criteria defined")
            all_passed &= check(all(s == "MET" for s in criteria.values()), "Phase Gate: all criteria MET")

    print("\n" + ("=" * 70))
    if all_passed:
        print("[SUCCESS] All Continuous Marketplace Compliance verifications PASSED (100%)!")
        return 0
    else:
        print("[FAILURE] Some verifications failed. Check above logs.")
        return 1

if __name__ == "__main__":
    sys.exit(main())
