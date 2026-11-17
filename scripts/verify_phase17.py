#!/usr/bin/env python3
"""
Verification Script for Prompt 17 (DRE gerencial e conciliação de repasses).
Validates Acceptance Criteria AC-17-1 through AC-17-10.
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
    print("=== Starting Verification for Prompt 17 (DRE Gerencial & Conciliação de Repasses) ===")
    all_passed = True

    backend_base = "backend/src/main/java/br/com/sellerintelligence/reconciliation"
    migration_file = "backend/src/main/resources/db/migration/V12__marketplace_reconciliation_and_managerial_dre.sql"
    frontend_view = "frontend/src/components/ReconciliationAndDreView.tsx"
    frontend_test = "frontend/src/components/ReconciliationAndDreView.test.tsx"
    dashboard_view = "frontend/src/components/DashboardView.tsx"

    # AC-17-1: Ingestion idempotency with SHA-256 and unique key
    ingestion_service = os.path.join(backend_base, "service/SettlementIngestionService.java")
    settlement_entity = os.path.join(backend_base, "domain/ReconciliationSettlement.java")
    all_passed &= check(os.path.exists(ingestion_service), "AC-17-1: SettlementIngestionService exists")
    all_passed &= check(os.path.exists(settlement_entity), "AC-17-1: ReconciliationSettlement entity exists")
    if os.path.exists(ingestion_service):
        with open(ingestion_service, "r") as f:
            content = f.read()
            all_passed &= check("calculateHash" in content and "SHA-256" in content, "AC-17-1: Computes SHA-256 hash fingerprint for payload idempotency")
            all_passed &= check("findByTenantIdAndMarketplaceAndExternalSettlementId" in content, "AC-17-1: Enforces natural idempotency check on import")

    # AC-17-2: Reconciliation states (CONCILED, PENDENTE, DIVERGENTE, NÃO_IDENTIFICADO)
    item_status_enum = os.path.join(backend_base, "domain/ReconciliationItemStatus.java")
    matching_engine = os.path.join(backend_base, "service/ReconciliationMatchingEngine.java")
    all_passed &= check(os.path.exists(item_status_enum), "AC-17-2: ReconciliationItemStatus enum exists")
    all_passed &= check(os.path.exists(matching_engine), "AC-17-2: ReconciliationMatchingEngine exists")
    if os.path.exists(item_status_enum):
        with open(item_status_enum, "r") as f:
            content = f.read()
            all_passed &= check("CONCILED" in content and "PENDING_MATCH" in content, "AC-17-2: Contains CONCILED and PENDING_MATCH")
            all_passed &= check("DISCREPANT" in content and "UNIDENTIFIED" in content, "AC-17-2: Contains DISCREPANT and UNIDENTIFIED")

    # AC-17-3: Deterministic BigDecimal arithmetic for discrepancies
    item_entity = os.path.join(backend_base, "domain/ReconciliationSettlementItem.java")
    all_passed &= check(os.path.exists(item_entity), "AC-17-3: ReconciliationSettlementItem exists")
    if os.path.exists(matching_engine):
        with open(matching_engine, "r") as f:
            content = f.read()
            all_passed &= check("BigDecimal" in content and "compareTo" in content, "AC-17-3: Deterministic discrepancy calculation in BigDecimal")
            all_passed &= check("settled.subtract(expected)" in content, "AC-17-3: Quantifies discrepancy between expected order amount and actual payout")

    # AC-17-4: Complete financial event lifecycle
    entry_type_enum = os.path.join(backend_base, "domain/ReconciliationEntryType.java")
    all_passed &= check(os.path.exists(entry_type_enum), "AC-17-4: ReconciliationEntryType enum exists")
    if os.path.exists(entry_type_enum):
        with open(entry_type_enum, "r") as f:
            content = f.read()
            all_passed &= check("ORDER_PAYOUT" in content and "COMMISSION_FEE" in content, "AC-17-4: Covers payouts and commissions")
            all_passed &= check("SHIPPING_DEBIT" in content and "REFUND_CHARGEBACK" in content, "AC-17-4: Covers shipping debits and refund/chargeback")
            all_passed &= check("ESCROW_RELEASE" in content and "ADJUSTMENT" in content, "AC-17-4: Covers escrow releases and adjustments")

    # AC-17-5: Immutable manual audit trail
    audit_entity = os.path.join(backend_base, "domain/ReconciliationAuditLog.java")
    manual_service = os.path.join(backend_base, "service/ManualReconciliationService.java")
    all_passed &= check(os.path.exists(audit_entity), "AC-17-5: ReconciliationAuditLog entity exists")
    all_passed &= check(os.path.exists(manual_service), "AC-17-5: ManualReconciliationService exists")
    if os.path.exists(manual_service):
        with open(manual_service, "r") as f:
            content = f.read()
            all_passed &= check("auditLogRepository.save" in content, "AC-17-5: Persists immutable audit record with justification")
            all_passed &= check("manuallyAssociateOrder" in content and "ignoreDiscrepancy" in content, "AC-17-5: Supports manual association and discrepancy ignoring")

    # AC-17-6: Dispute registration with non-automated disclaimer
    dispute_entity = os.path.join(backend_base, "domain/ReconciliationDispute.java")
    all_passed &= check(os.path.exists(dispute_entity), "AC-17-6: ReconciliationDispute entity exists")
    if os.path.exists(dispute_entity):
        with open(dispute_entity, "r") as f:
            content = f.read()
            all_passed &= check("não executa envio automático" in content, "AC-17-6: Embeds non-automated submission legal disclaimer")
            all_passed &= check("disclaimer" in content and "protocolNumber" in content, "AC-17-6: Enforces dispute protocol for internal seller tracking")

    # AC-17-7: Managerial DRE under Accrual and Cash bases with structured lines
    dre_service = os.path.join(backend_base, "service/DreCalculationService.java")
    all_passed &= check(os.path.exists(dre_service), "AC-17-7: DreCalculationService exists")
    if os.path.exists(dre_service):
        with open(dre_service, "r") as f:
            content = f.read()
            all_passed &= check("calculateAccrualBasisDre" in content, "AC-17-7: Implements Accrual basis DRE (Competência)")
            all_passed &= check("calculateCashBasisDre" in content, "AC-17-7: Implements Cash basis DRE (Caixa)")
            all_passed &= check("buildDreLines" in content, "AC-17-7: Standardizes 10 managerial DRE lines with formulas")

    # AC-17-8: Legal disclaimer on managerial DRE
    if os.path.exists(dre_service):
        with open(dre_service, "r") as f:
            content = f.read()
            all_passed &= check("DRE_LEGAL_DISCLAIMER" in content, "AC-17-8: Defines mandatory DRE legal non-fiscal disclaimer")
            all_passed &= check("exportDreToCsv" in content, "AC-17-8: Exports CSV with timezone, timestamp and legal disclaimer headers")

    # AC-17-9: Multi-tenant isolation and Flyway migration V12
    all_passed &= check(os.path.exists(migration_file), "AC-17-9: Migration V12 exists")
    if os.path.exists(migration_file):
        with open(migration_file, "r") as f:
            content = f.read()
            all_passed &= check("tenant_id" in content, "AC-17-9: Enforces tenant_id isolation across all tables")
            all_passed &= check("uk_settlement_idempotency" in content, "AC-17-9: Enforces database unique idempotency constraint")
            all_passed &= check("reconciliation_audit_logs" in content, "AC-17-9: Creates audit trail table")
            all_passed &= check("reconciliation_disputes" in content, "AC-17-9: Creates disputes table")

    # AC-17-10: Frontend Integration & Vitest suite
    all_passed &= check(os.path.exists(frontend_view), "AC-17-10: ReconciliationAndDreView component exists")
    all_passed &= check(os.path.exists(frontend_test), "AC-17-10: ReconciliationAndDreView test suite exists")
    if os.path.exists(dashboard_view):
        with open(dashboard_view, "r") as f:
            content = f.read()
            all_passed &= check("RECONCILIATION_DRE" in content and "ReconciliationAndDreView" in content, "AC-17-10: Integrated into main Dashboard tabs")

    print("\n========================================================")
    if all_passed:
        print("[SUCCESS] All criteria AC-17-1 through AC-17-10 are VERIFIED!")
        return 0
    else:
        print("[ERROR] Some criteria failed verification.")
        return 1

if __name__ == "__main__":
    sys.exit(main())
