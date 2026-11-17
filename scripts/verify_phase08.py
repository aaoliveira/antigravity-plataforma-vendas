#!/usr/bin/env python3
"""
Verification Script for Prompt 08 — Motor financeiro determinístico e versionado
Fase 1 — MVP / P0
Seller Intelligence Platform
"""

import sys
import re
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
    print("=== Starting Verification for Prompt 08 ===")

    # 1. Database Migrations
    v7_migration = ROOT / "backend/src/main/resources/db/migration/V7__financial_engine.sql"
    check(v7_migration.is_file(), "Flyway V7 migration file exists")
    check(file_contains(v7_migration, "CREATE TABLE IF NOT EXISTS sku_costs"), "V7 defines sku_costs table")
    check(file_contains(v7_migration, "CREATE TABLE IF NOT EXISTS financial_rule_sets"), "V7 defines financial_rule_sets table")
    check(file_contains(v7_migration, "CREATE TABLE IF NOT EXISTS financial_rule_components"), "V7 defines financial_rule_components table")
    check(file_contains(v7_migration, "CREATE TABLE IF NOT EXISTS order_financial_calculations"), "V7 defines order_financial_calculations table")
    check(file_contains(v7_migration, "CREATE TABLE IF NOT EXISTS order_item_financial_calculations"), "V7 defines order_item_financial_calculations table")
    check(file_contains(v7_migration, "NUMERIC(15, 4)"), "V7 enforces 4-decimal precision for unit COGS and component values")

    # 2. Domain Entities and Repositories
    domain_dir = ROOT / "backend/src/main/java/br/com/sellerintelligence/financial/domain"
    check((domain_dir / "SkuCost.java").is_file(), "SkuCost entity exists")
    check((domain_dir / "SkuCostRepository.java").is_file(), "SkuCostRepository exists")
    check((domain_dir / "FinancialRuleSet.java").is_file(), "FinancialRuleSet entity exists")
    check((domain_dir / "FinancialRuleComponent.java").is_file(), "FinancialRuleComponent entity exists")
    check((domain_dir / "FinancialRuleSetRepository.java").is_file(), "FinancialRuleSetRepository exists")
    check((domain_dir / "OrderFinancialCalculation.java").is_file(), "OrderFinancialCalculation entity exists")
    check((domain_dir / "OrderItemFinancialCalculation.java").is_file(), "OrderItemFinancialCalculation entity exists")
    check((domain_dir / "OrderFinancialCalculationRepository.java").is_file(), "OrderFinancialCalculationRepository exists")
    check((domain_dir / "OrderItemFinancialCalculationRepository.java").is_file(), "OrderItemFinancialCalculationRepository exists")

    # 3. Deterministic Engine & Anti-Hallucination
    engine_dir = ROOT / "backend/src/main/java/br/com/sellerintelligence/financial/engine"
    engine_file = engine_dir / "FinancialCalculationEngine.java"
    check(engine_file.is_file(), "FinancialCalculationEngine exists")
    check(file_contains(engine_file, "RoundingMode.HALF_EVEN"), "Engine uses explicit RoundingMode.HALF_EVEN")
    check(file_contains(engine_file, "CalculationMemory"), "Engine produces detailed CalculationMemory")

    # Check that no float or double fields/methods are used in calculation formulas
    engine_code = engine_file.read_text(encoding="utf-8")
    code_no_comments = re.sub(r"/\*.*?\*/", "", engine_code, flags=re.DOTALL)
    code_no_comments = re.sub(r"//.*", "", code_no_comments)
    float_matches = re.findall(r"\b(float|Float)\b", code_no_comments)
    double_matches = re.findall(r"\b(double|Double)\b", code_no_comments)
    check(len(float_matches) == 0, "No float types in FinancialCalculationEngine")
    check(len(double_matches) == 0, "No double types in FinancialCalculationEngine")

    # 4. Services and Controller
    service_dir = ROOT / "backend/src/main/java/br/com/sellerintelligence/financial/service"
    check((service_dir / "SkuCostService.java").is_file(), "SkuCostService exists")
    check((service_dir / "FinancialRuleService.java").is_file(), "FinancialRuleService exists")
    check((service_dir / "FinancialCalculationService.java").is_file(), "FinancialCalculationService exists")
    check((ROOT / "backend/src/main/java/br/com/sellerintelligence/financial/web/FinancialEngineController.java").is_file(), "FinancialEngineController exists")

    # 5. Frontend UI & Tests
    frontend_dir = ROOT / "frontend/src/components"
    check((frontend_dir / "FinancialEngineView.tsx").is_file(), "FinancialEngineView component exists")
    check((frontend_dir / "FinancialEngineView.test.tsx").is_file(), "FinancialEngineView tests exist")
    check(file_contains(frontend_dir / "FinancialEngineView.tsx", "Preço Mínimo (Break-even)"), "UI presents break-even price")
    check(file_contains(frontend_dir / "FinancialEngineView.tsx", "Memória de Cálculo Explicável"), "UI presents explainable memory table")
    check(file_contains(frontend_dir / "FinancialEngineView.tsx", "CÁLCULO PARCIAL"), "UI presents partial warning when data is missing")

    # 6. Automated Backend Tests
    financial_test_dir = ROOT / "backend/src/test/java/br/com/sellerintelligence/financial"
    check((financial_test_dir / "engine/FinancialCalculationEngineTest.java").is_file(), "FinancialCalculationEngineTest exists")
    check((financial_test_dir / "service/FinancialOrderCalculationIntegrationTest.java").is_file(), "FinancialOrderCalculationIntegrationTest exists")
    check((financial_test_dir / "PostgresFinancialEngineIntegrationTest.java").is_file(), "PostgresFinancialEngineIntegrationTest exists")

    print("\n[SUCCESS] All structural, database, precision, rules and financial engine checks for Prompt 08 passed!")

if __name__ == "__main__":
    main()
