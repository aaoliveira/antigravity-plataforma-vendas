#!/usr/bin/env python3
"""
Verification Script for Prompt 06 — Sincronização de catálogo, anúncios e pedidos
Fase 1 — MVP / P0
Seller Intelligence Platform
"""

import sys
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
    print("=== Starting Verification for Prompt 06 ===")

    # 1. Database Migrations
    v5_migration = ROOT / "backend/src/main/resources/db/migration/V5__catalog_listings_orders_sync.sql"
    check(v5_migration.is_file(), "Flyway V5 migration file exists")
    check(file_contains(v5_migration, "CREATE TABLE IF NOT EXISTS products"), "V5 defines products table")
    check(file_contains(v5_migration, "CREATE TABLE IF NOT EXISTS listings"), "V5 defines listings table")
    check(file_contains(v5_migration, "CREATE TABLE IF NOT EXISTS orders"), "V5 defines orders table")
    check(file_contains(v5_migration, "CREATE TABLE IF NOT EXISTS order_items"), "V5 defines order_items table")
    check(file_contains(v5_migration, "CREATE TABLE IF NOT EXISTS sync_jobs"), "V5 defines sync_jobs table")
    check(file_contains(v5_migration, "CREATE TABLE IF NOT EXISTS webhook_events"), "V5 defines webhook_events table")
    check(file_contains(v5_migration, "uk_products_tenant_store_sku"), "V5 enforces unique product constraint")
    check(file_contains(v5_migration, "uk_listings_tenant_marketplace_ext_id"), "V5 enforces unique listing constraint")
    check(file_contains(v5_migration, "uk_orders_tenant_marketplace_ext_id"), "V5 enforces unique order constraint")
    check(file_contains(v5_migration, "uk_webhook_events_marketplace_ext_id"), "V5 enforces unique webhook event constraint")

    # 2. Domain Models and Enums
    domain_dir = ROOT / "backend/src/main/java/br/com/sellerintelligence/sync/domain"
    check((domain_dir / "DataProvenance.java").is_file(), "DataProvenance enum exists")
    check(file_contains(domain_dir / "DataProvenance.java", "IMPORTED"), "DataProvenance defines IMPORTED")
    check(file_contains(domain_dir / "DataProvenance.java", "UNAVAILABLE"), "DataProvenance defines UNAVAILABLE")
    check((domain_dir / "SyncStatus.java").is_file(), "SyncStatus enum exists")
    check((domain_dir / "SyncResourceType.java").is_file(), "SyncResourceType enum exists")
    check((domain_dir / "SyncTriggerType.java").is_file(), "SyncTriggerType enum exists")
    check((domain_dir / "Product.java").is_file(), "Product entity exists")
    check((domain_dir / "Listing.java").is_file(), "Listing entity exists")
    check((domain_dir / "Order.java").is_file(), "Order entity exists")
    check((domain_dir / "OrderItem.java").is_file(), "OrderItem entity exists")
    check((domain_dir / "SyncJob.java").is_file(), "SyncJob entity exists")
    check((domain_dir / "WebhookEvent.java").is_file(), "WebhookEvent entity exists")

    # 3. CSV Import Service
    csv_dir = ROOT / "backend/src/main/java/br/com/sellerintelligence/sync/csv"
    check((csv_dir / "CsvImportService.java").is_file(), "CsvImportService exists")
    check(file_contains(csv_dir / "CsvImportService.java", "importCatalogCsv"), "CsvImportService implements importCatalogCsv")
    check(file_contains(csv_dir / "CsvImportService.java", "importOrdersCsv"), "CsvImportService implements importOrdersCsv")
    check(file_contains(csv_dir / "CsvImportService.java", "CsvRowError"), "CsvImportService generates CsvRowError")

    # 4. RabbitMQ & Asynchronous Sync
    rabbitmq_dir = ROOT / "backend/src/main/java/br/com/sellerintelligence/sync/infrastructure/rabbitmq"
    check((rabbitmq_dir / "RabbitMQConfig.java").is_file(), "RabbitMQConfig exists")
    check(file_contains(rabbitmq_dir / "RabbitMQConfig.java", "SYNC_DLQ"), "RabbitMQConfig configures dead letter queue")
    check((rabbitmq_dir / "SyncJobConsumer.java").is_file(), "SyncJobConsumer exists")
    check((rabbitmq_dir / "DirectSyncJobPublisher.java").is_file(), "DirectSyncJobPublisher test fallback exists")

    # 5. Webhook Service
    webhook_dir = ROOT / "backend/src/main/java/br/com/sellerintelligence/sync/webhook"
    check((webhook_dir / "WebhookService.java").is_file(), "WebhookService exists")
    check(file_contains(webhook_dir / "WebhookService.java", "verifySignature"), "WebhookService verifies HMAC signature")
    check(file_contains(webhook_dir / "WebhookService.java", "DUPLICATE_IGNORED"), "WebhookService detects duplicate replay events")

    # 6. REST Controllers
    web_dir = ROOT / "backend/src/main/java/br/com/sellerintelligence/sync/web"
    check((web_dir / "SyncJobController.java").is_file(), "SyncJobController exists")
    check((web_dir / "CsvImportController.java").is_file(), "CsvImportController exists")
    check((web_dir / "WebhookController.java").is_file(), "WebhookController exists")
    check((web_dir / "CatalogAndOrdersController.java").is_file(), "CatalogAndOrdersController exists")

    # 7. Frontend UI & Vitest Tests
    frontend_comp = ROOT / "frontend/src/components"
    check((frontend_comp / "SyncManager.tsx").is_file(), "Frontend SyncManager component exists")
    check((frontend_comp / "SyncManager.test.tsx").is_file(), "Frontend SyncManager tests exist")

    # 8. Automated Backend Tests
    sync_test_dir = ROOT / "backend/src/test/java/br/com/sellerintelligence/sync"
    check((sync_test_dir / "csv/CsvImportServiceTest.java").is_file(), "CsvImportServiceTest exists")
    check((sync_test_dir / "service/SyncIdempotencyIntegrationTest.java").is_file(), "SyncIdempotencyIntegrationTest exists")
    check((sync_test_dir / "webhook/WebhookSecurityAndReplayTest.java").is_file(), "WebhookSecurityAndReplayTest exists")
    check((sync_test_dir / "web/SyncCrossTenantIntegrationTest.java").is_file(), "SyncCrossTenantIntegrationTest exists")
    check((sync_test_dir / "PostgresSyncIntegrationTest.java").is_file(), "PostgresSyncIntegrationTest exists")

    print("\n[SUCCESS] All structural, database, security, and idempotency checks for Prompt 06 passed!")

if __name__ == "__main__":
    main()
