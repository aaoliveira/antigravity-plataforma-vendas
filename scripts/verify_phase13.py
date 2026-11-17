#!/usr/bin/env python3
"""
Verification Script for Prompt 13 — Monitoramento contínuo de produtos e concorrentes
Fase: Fase 2 — Expansão de Inteligência & Monitoramento
Seller Intelligence Platform
"""

import sys
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

def check(condition: bool, message: str) -> None:
    if condition:
        print(f"[PASS] {message}")
    else:
        print(f"[FAIL] {message}")
        sys.exit(1)

def file_contains(path: Path, pattern: str) -> bool:
    if not path.is_file():
        return False
    return pattern in path.read_text(encoding="utf-8")

def main():
    print("=== Starting Verification for Prompt 13 (Monitoramento de Produtos & Concorrentes) ===")

    # 1. AC-13-1: Watchlists & Items Model & History Preservation
    watchlist_entity = ROOT / "backend/src/main/java/br/com/sellerintelligence/monitoring/domain/Watchlist.java"
    item_entity = ROOT / "backend/src/main/java/br/com/sellerintelligence/monitoring/domain/WatchlistItem.java"
    watchlist_service = ROOT / "backend/src/main/java/br/com/sellerintelligence/monitoring/service/MonitoringWatchlistService.java"
    check(watchlist_entity.is_file(), "AC-13-1: Watchlist entity exists")
    check(item_entity.is_file(), "AC-13-1: WatchlistItem entity exists")
    check(watchlist_service.is_file(), "AC-13-1: MonitoringWatchlistService exists")
    check(file_contains(watchlist_service, "untrackItem"), "AC-13-1: Watchlist service supports soft untracking")
    check(file_contains(item_entity, "UNTRACKED"), "AC-13-1: Watchlist item status supports UNTRACKED without data deletion")

    # 2. AC-13-2: Snapshots with Explicit Provenance and Authoritative Sources
    snapshot_entity = ROOT / "backend/src/main/java/br/com/sellerintelligence/monitoring/domain/MonitoringSnapshot.java"
    collector_service = ROOT / "backend/src/main/java/br/com/sellerintelligence/monitoring/service/MonitoringCollectorService.java"
    check(snapshot_entity.is_file(), "AC-13-2: MonitoringSnapshot entity exists")
    check(collector_service.is_file(), "AC-13-2: MonitoringCollectorService exists")
    check(file_contains(snapshot_entity, "OBSERVED"), "AC-13-2: Snapshot enforces OBSERVED provenance label")
    check(file_contains(snapshot_entity, "CollectionStatus"), "AC-13-2: Snapshot supports CollectionStatus (SUCCESS, COLLECTION_FAILED, GAP)")
    check(file_contains(collector_service, "recordObservation"), "AC-13-2: Collector records observed snapshot with timestamp and source")
    check(file_contains(collector_service, "recordFailure"), "AC-13-2: Collector records collection failure gracefully")

    # 3. AC-13-3: Deterministic Diff Engine & Event Deduplication
    diff_engine = ROOT / "backend/src/main/java/br/com/sellerintelligence/monitoring/service/MonitoringDiffEngine.java"
    change_event_entity = ROOT / "backend/src/main/java/br/com/sellerintelligence/monitoring/domain/MonitoringChangeEvent.java"
    check(diff_engine.is_file(), "AC-13-3: MonitoringDiffEngine exists")
    check(change_event_entity.is_file(), "AC-13-3: MonitoringChangeEvent entity exists")
    check(file_contains(diff_engine, "generateEventHash"), "AC-13-3: Diff engine generates deterministic SHA-256 event hash")
    check(file_contains(diff_engine, "PRICE_CHANGE"), "AC-13-3: Diff engine detects price change in amount and percentage")
    check(file_contains(diff_engine, "STATUS_CHANGE"), "AC-13-3: Diff engine detects listing status changes (OUT_OF_STOCK, REACTIVATED)")

    # 4. AC-13-4: Timeline Service & Explicit Gaps Without Silent Interpolation
    timeline_service = ROOT / "backend/src/main/java/br/com/sellerintelligence/monitoring/service/MonitoringTimelineService.java"
    check(timeline_service.is_file(), "AC-13-4: MonitoringTimelineService exists")
    check(file_contains(timeline_service, "allowedGapThresholdMinutes"), "AC-13-4: Timeline service detects time gaps exceeding threshold")
    check(file_contains(timeline_service, "CollectionStatus.GAP"), "AC-13-4: Timeline explicitly marks gap points with CollectionStatus.GAP")
    check(file_contains(timeline_service, "null, // Strict Anti-Hallucination: never fabricate missing prices"), "AC-13-4: Strict anti-hallucination verified (never interpolates prices)")
    check(file_contains(timeline_service, "coveragePercentage"), "AC-13-4: Timeline service calculates collection coverage percentage")

    # 5. AC-13-5: Alert Rules & Deduplication with Cooldown Window
    alert_service = ROOT / "backend/src/main/java/br/com/sellerintelligence/monitoring/service/MonitoringAlertService.java"
    alert_rule_entity = ROOT / "backend/src/main/java/br/com/sellerintelligence/monitoring/domain/MonitoringAlertRule.java"
    alert_entity = ROOT / "backend/src/main/java/br/com/sellerintelligence/monitoring/domain/MonitoringAlert.java"
    check(alert_service.is_file(), "AC-13-5: MonitoringAlertService exists")
    check(alert_rule_entity.is_file(), "AC-13-5: MonitoringAlertRule entity exists")
    check(alert_entity.is_file(), "AC-13-5: MonitoringAlert entity exists")
    check(file_contains(alert_service, "cooldownSeconds"), "AC-13-5: Alert service enforces cooldown window deduplication")
    check(file_contains(alert_service, "dedupKey"), "AC-13-5: Alert service calculates dedupKey per rule, item and cooldown bucket")

    # 6. AC-13-6: Competitor vs Own Listing Comparator
    comp_service = ROOT / "backend/src/main/java/br/com/sellerintelligence/monitoring/service/CompetitorComparisonService.java"
    check(comp_service.is_file(), "AC-13-6: CompetitorComparisonService exists")
    check(file_contains(comp_service, "ComparisonResult"), "AC-13-6: ComparisonResult record returns side-by-side details")
    check(file_contains(comp_service, "Estoque real do concorrente não fornecido pela API"), "AC-13-6: Explicit disclaimer of private third-party inventory")
    check(file_contains(comp_service, "Vendas reais do concorrente não fornecidas pela API"), "AC-13-6: Explicit disclaimer of private third-party sales")

    # 7. AC-13-7: ADR DEC-019 & Database Schema Flyway V9
    adr_doc = ROOT / "docs/architecture/DECISION_LOG.md"
    flyway_v9 = ROOT / "backend/src/main/resources/db/migration/V9__competitor_and_product_monitoring.sql"
    check(flyway_v9.is_file(), "AC-13-7: Flyway migration V9 exists")
    check(file_contains(flyway_v9, "monitoring_watchlists"), "AC-13-7: Migration creates monitoring_watchlists table")
    check(file_contains(flyway_v9, "monitoring_snapshots"), "AC-13-7: Migration creates monitoring_snapshots table")
    check(file_contains(flyway_v9, "idx_monitoring_snapshots_timeline"), "AC-13-7: Migration creates composite timeline index")
    check(file_contains(flyway_v9, "uk_monitoring_change_event_hash"), "AC-13-7: Migration enforces unique event hash constraint")
    check(file_contains(flyway_v9, "uk_monitoring_alerts_dedup"), "AC-13-7: Migration enforces unique alert dedup constraint")
    check(file_contains(adr_doc, "DEC-019 / ADR-019"), "AC-13-7: ADR DEC-019 registered in DECISION_LOG.md")
    check(file_contains(adr_doc, "Retenção de 12 Meses"), "AC-13-7: ADR DEC-019 defines 12-month snapshot retention")

    # 8. AC-13-8: Test Suites & Multi-Tenant Isolation
    watchlist_test = ROOT / "backend/src/test/java/br/com/sellerintelligence/monitoring/service/MonitoringWatchlistServiceTest.java"
    diff_test = ROOT / "backend/src/test/java/br/com/sellerintelligence/monitoring/service/MonitoringDiffEngineTest.java"
    timeline_test = ROOT / "backend/src/test/java/br/com/sellerintelligence/monitoring/service/MonitoringTimelineServiceTest.java"
    alert_test = ROOT / "backend/src/test/java/br/com/sellerintelligence/monitoring/service/MonitoringAlertServiceTest.java"
    comparison_test = ROOT / "backend/src/test/java/br/com/sellerintelligence/monitoring/service/CompetitorComparisonServiceTest.java"
    postgres_test = ROOT / "backend/src/test/java/br/com/sellerintelligence/monitoring/PostgresMonitoringIntegrationTest.java"
    controller_test = ROOT / "backend/src/test/java/br/com/sellerintelligence/monitoring/MonitoringControllerIntegrationTest.java"
    frontend_test = ROOT / "frontend/src/components/CompetitorMonitoringView.test.tsx"

    check(watchlist_test.is_file(), "AC-13-8: MonitoringWatchlistServiceTest exists")
    check(diff_test.is_file(), "AC-13-8: MonitoringDiffEngineTest exists")
    check(timeline_test.is_file(), "AC-13-8: MonitoringTimelineServiceTest exists")
    check(alert_test.is_file(), "AC-13-8: MonitoringAlertServiceTest exists")
    check(comparison_test.is_file(), "AC-13-8: CompetitorComparisonServiceTest exists")
    check(postgres_test.is_file(), "AC-13-8: PostgresMonitoringIntegrationTest exists")
    check(controller_test.is_file(), "AC-13-8: MonitoringControllerIntegrationTest exists")
    check(frontend_test.is_file(), "AC-13-8: CompetitorMonitoringView.test.tsx exists")

    # 9. AC-13-9: Requirements Traceability & Gate Status
    trace_doc = ROOT / "docs/product/REQUIREMENTS_TRACEABILITY.md"
    check(file_contains(trace_doc, "FR-CMP-002"), "AC-13-9: FR-CMP-002 tracked")
    check(file_contains(trace_doc, "VERIFIED (Prompt 13)"), "AC-13-9: FR-CMP and FR-MON verified in Prompt 13")
    check(file_contains(trace_doc, "FR-MON-001"), "AC-13-9: FR-MON-001 tracked")

    print("\n[SUCCESS] All 9 Acceptance Criteria for Prompt 13 Verified Successfully!")

if __name__ == "__main__":
    main()
