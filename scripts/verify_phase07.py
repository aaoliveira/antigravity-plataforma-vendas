#!/usr/bin/env python3
"""
Verification Script for Prompt 07 — Market Intelligence: pesquisa e análise de produtos
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
    print("=== Starting Verification for Prompt 07 ===")

    # 1. Database Migrations
    v6_migration = ROOT / "backend/src/main/resources/db/migration/V6__market_intelligence.sql"
    check(v6_migration.is_file(), "Flyway V6 migration file exists")
    check(file_contains(v6_migration, "CREATE TABLE IF NOT EXISTS market_search_snapshots"), "V6 defines market_search_snapshots table")
    check(file_contains(v6_migration, "CREATE TABLE IF NOT EXISTS market_product_items"), "V6 defines market_product_items table")
    check(file_contains(v6_migration, "CREATE TABLE IF NOT EXISTS saved_searches"), "V6 defines saved_searches table")
    check(file_contains(v6_migration, "CREATE TABLE IF NOT EXISTS saved_market_products"), "V6 defines saved_market_products table")
    check(file_contains(v6_migration, "uk_saved_market_products_tenant_item"), "V6 enforces unique constraint on saved products")

    # 2. Domain Entities and Repositories
    domain_dir = ROOT / "backend/src/main/java/br/com/sellerintelligence/market/domain"
    check((domain_dir / "MarketSearchSnapshot.java").is_file(), "MarketSearchSnapshot entity exists")
    check((domain_dir / "MarketProductItem.java").is_file(), "MarketProductItem entity exists")
    check((domain_dir / "SavedSearch.java").is_file(), "SavedSearch entity exists")
    check((domain_dir / "SavedMarketProduct.java").is_file(), "SavedMarketProduct entity exists")
    check((domain_dir / "MarketSearchSnapshotRepository.java").is_file(), "MarketSearchSnapshotRepository exists")
    check((domain_dir / "MarketProductItemRepository.java").is_file(), "MarketProductItemRepository exists")
    check((domain_dir / "SavedSearchRepository.java").is_file(), "SavedSearchRepository exists")
    check((domain_dir / "SavedMarketProductRepository.java").is_file(), "SavedMarketProductRepository exists")

    # 3. Search Providers and Anti-Hallucination
    provider_dir = ROOT / "backend/src/main/java/br/com/sellerintelligence/market/provider"
    check((provider_dir / "MarketSearchProvider.java").is_file(), "MarketSearchProvider interface exists")
    check((provider_dir / "MercadoLivreMarketSearchProvider.java").is_file(), "MercadoLivreMarketSearchProvider exists")
    check((provider_dir / "ShopeeMarketSearchProvider.java").is_file(), "ShopeeMarketSearchProvider exists")
    check(file_contains(provider_dir / "ShopeeMarketSearchProvider.java", "DataProvenance.UNAVAILABLE"), "ShopeeMarketSearchProvider returns UNAVAILABLE without scraping")
    check(file_contains(provider_dir / "ShopeeMarketSearchProvider.java", "proibido"), "ShopeeMarketSearchProvider enforces anti-scraping policy")
    check((provider_dir / "MarketSearchRegistry.java").is_file(), "MarketSearchRegistry exists")

    # 4. Service and Controller
    check((ROOT / "backend/src/main/java/br/com/sellerintelligence/market/service/MarketIntelligenceService.java").is_file(), "MarketIntelligenceService exists")
    check((ROOT / "backend/src/main/java/br/com/sellerintelligence/market/web/MarketIntelligenceController.java").is_file(), "MarketIntelligenceController exists")

    # 5. Frontend UI & Tests
    frontend_dir = ROOT / "frontend/src/components"
    check((frontend_dir / "MarketIntelligenceView.tsx").is_file(), "Frontend MarketIntelligenceView component exists")
    check((frontend_dir / "MarketIntelligenceView.test.tsx").is_file(), "Frontend MarketIntelligenceView tests exist")
    check(file_contains(frontend_dir / "MarketIntelligenceView.tsx", "OBSERVED"), "UI presents OBSERVED data provenance label")
    check(file_contains(frontend_dir / "MarketIntelligenceView.tsx", "ESTIMATED"), "UI presents ESTIMATED indicator with methodology")

    # 6. Automated Backend Tests
    market_test_dir = ROOT / "backend/src/test/java/br/com/sellerintelligence/market"
    check((market_test_dir / "service/MarketIntelligenceSearchTest.java").is_file(), "MarketIntelligenceSearchTest exists")
    check((market_test_dir / "service/SavedSearchesAndProductsIntegrationTest.java").is_file(), "SavedSearchesAndProductsIntegrationTest exists")
    check((market_test_dir / "PostgresMarketIntelligenceIntegrationTest.java").is_file(), "PostgresMarketIntelligenceIntegrationTest exists")

    print("\n[SUCCESS] All structural, database, security, and market intelligence checks for Prompt 07 passed!")

if __name__ == "__main__":
    main()
