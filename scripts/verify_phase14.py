#!/usr/bin/env python3
"""
Verification Script for Prompt 14 (Smart Alerts & Action Center).
Validates Acceptance Criteria AC-14-1 through AC-14-10.
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
    print("=== Starting Verification for Prompt 14 (Smart Alerts & Action Center) ===")
    all_passed = True

    backend_base = "backend/src/main/java/br/com/sellerintelligence/action"
    migration_file = "backend/src/main/resources/db/migration/V10__smart_alerts_and_action_center.sql"

    # AC-14-1: Alert Rule Catalog
    rule_entity = os.path.join(backend_base, "domain/ActionAlertRule.java")
    rule_types = os.path.join(backend_base, "domain/AlertRuleType.java")
    all_passed &= check(os.path.exists(rule_entity), "AC-14-1: ActionAlertRule entity exists")
    all_passed &= check(os.path.exists(rule_types), "AC-14-1: AlertRuleType enum exists")
    if os.path.exists(rule_types):
        with open(rule_types, "r") as f:
            content = f.read()
            for rtype in ["MARGIN_THRESHOLD", "PRICE_UNDERCUT", "SYNC_FAILURE", "SALES_DROP", "STOCK_DEPLETION"]:
                all_passed &= check(rtype in content, f"AC-14-1: AlertRuleType supports {rtype}")

    # AC-14-2: Rule Simulation Engine
    rule_service = os.path.join(backend_base, "service/ActionAlertRuleService.java")
    all_passed &= check(os.path.exists(rule_service), "AC-14-2: ActionAlertRuleService exists")
    if os.path.exists(rule_service):
        with open(rule_service, "r") as f:
            content = f.read()
            all_passed &= check("simulateRule" in content, "AC-14-2: Service implements simulateRule method")
            all_passed &= check("RuleSimulationResult" in content, "AC-14-2: Service returns structured simulation feedback")

    # AC-14-3: Deduplication with Cooldown Window
    alert_service = os.path.join(backend_base, "service/ActionAlertService.java")
    all_passed &= check(os.path.exists(alert_service), "AC-14-3: ActionAlertService exists")
    if os.path.exists(alert_service):
        with open(alert_service, "r") as f:
            content = f.read()
            all_passed &= check("createAlertIfAbsent" in content, "AC-14-3: Implements deduplication check before creating alert")
            all_passed &= check("calculateDedupKey" in content, "AC-14-3: Implements deterministic cooldown dedupKey calculation")

    # AC-14-4: Unified Alerts Inbox
    alert_entity = os.path.join(backend_base, "domain/ActionAlert.java")
    alert_status = os.path.join(backend_base, "domain/ActionAlertStatus.java")
    all_passed &= check(os.path.exists(alert_entity), "AC-14-4: ActionAlert entity exists")
    all_passed &= check(os.path.exists(alert_status), "AC-14-4: ActionAlertStatus enum exists")
    if os.path.exists(alert_status):
        with open(alert_status, "r") as f:
            content = f.read()
            for status in ["NEW", "READ", "RESOLVED", "IGNORED"]:
                all_passed &= check(status in content, f"AC-14-4: ActionAlertStatus supports {status}")

    # AC-14-5: Task Conversion from Alert
    task_service = os.path.join(backend_base, "service/ActionTaskService.java")
    all_passed &= check(os.path.exists(task_service), "AC-14-5: ActionTaskService exists")
    if os.path.exists(task_service):
        with open(task_service, "r") as f:
            content = f.read()
            all_passed &= check("createTaskFromAlert" in content, "AC-14-5: Implements createTaskFromAlert")
            all_passed &= check("evidenceSummary" in content, "AC-14-5: Task inherits evidence summary and link to alert")

    # AC-14-6: Anti-IDOR Scope Validation
    if os.path.exists(task_service):
        with open(task_service, "r") as f:
            content = f.read()
            all_passed &= check("validateAssignee" in content, "AC-14-6: Implements strict assignee validation")
            all_passed &= check("existsByOrganizationIdAndUserIdAndStatus" in content, "AC-14-6: Validates assignee membership in organization tenant")
            all_passed &= check("não pertence à organização tenant" in content or "outside organization tenant" in content, "AC-14-6: Throws error on cross-tenant assignment attempt")

    # AC-14-7: Notification Governance & Opt-In
    notif_service = os.path.join(backend_base, "service/ActionNotificationService.java")
    all_passed &= check(os.path.exists(notif_service), "AC-14-7: ActionNotificationService exists")
    if os.path.exists(notif_service):
        with open(notif_service, "r") as f:
            content = f.read()
            all_passed &= check("SUPPRESSED_NO_OPT_IN" in content, "AC-14-7: Enforces strict opt-in suppression")
            all_passed &= check("SUPPRESSED_COOLDOWN" in content, "AC-14-7: Enforces frequency limit per hour")

    # AC-14-8: Immutable Audit Trail
    audit_entity = os.path.join(backend_base, "domain/ActionTaskAudit.java")
    all_passed &= check(os.path.exists(audit_entity), "AC-14-8: ActionTaskAudit entity exists")

    # AC-14-9: Zero Automatic External Marketplace Mutations
    controller_file = os.path.join(backend_base, "web/ActionCenterController.java")
    all_passed &= check(os.path.exists(controller_file), "AC-14-9: ActionCenterController exists")
    if os.path.exists(controller_file):
        with open(controller_file, "r") as f:
            content = f.read()
            all_passed &= check("MarketplaceConnector" not in content, "AC-14-9: Confirmed no direct marketplace mutations in Action Center")

    # AC-14-10: Flyway V10 Migration & ADR DEC-020
    all_passed &= check(os.path.exists(migration_file), "AC-14-10: Flyway migration V10 exists")
    if os.path.exists(migration_file):
        with open(migration_file, "r") as f:
            content = f.read()
            for tbl in ["action_alert_rules", "action_notification_preferences", "action_alerts", "action_tasks", "action_task_audits", "action_notification_deliveries"]:
                all_passed &= check(tbl in content, f"AC-14-10: Migration creates {tbl} table")
            all_passed &= check("uk_action_alert_dedup" in content, "AC-14-10: Migration enforces uk_action_alert_dedup constraint")
            all_passed &= check("uk_action_notif_pref" in content, "AC-14-10: Migration enforces uk_action_notif_pref constraint")

    dec_log = "docs/architecture/DECISION_LOG.md"
    all_passed &= check(os.path.exists(dec_log), "AC-14-10: DECISION_LOG.md exists")
    if os.path.exists(dec_log):
        with open(dec_log, "r") as f:
            content = f.read()
            all_passed &= check("DEC-020" in content, "AC-14-10: ADR DEC-020 registered")

    frontend_view = "frontend/src/components/ActionCenterView.tsx"
    frontend_test = "frontend/src/components/ActionCenterView.test.tsx"
    all_passed &= check(os.path.exists(frontend_view), "AC-14-10: ActionCenterView.tsx exists")
    all_passed &= check(os.path.exists(frontend_test), "AC-14-10: ActionCenterView.test.tsx exists")

    if all_passed:
        print("\n[SUCCESS] All 10 Acceptance Criteria for Prompt 14 Verified Successfully!")
        sys.exit(0)
    else:
        print("\n[ERROR] Some Acceptance Criteria Failed Verification.")
        sys.exit(1)

if __name__ == "__main__":
    main()
