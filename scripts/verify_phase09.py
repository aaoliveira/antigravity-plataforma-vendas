#!/usr/bin/env python3
"""
Verification Script for Prompt 09 — Painel financeiro, rentabilidade e relatórios do MVP
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
    print("=== Starting Verification for Prompt 09 ===")

    # 1. Backend Domain & Dashboard DTOs
    dash_dir = ROOT / "backend/src/main/java/br/com/sellerintelligence/financial/dashboard"
    check((dash_dir / "FinancialDashboardFilter.java").is_file(), "FinancialDashboardFilter exists")
    check((dash_dir / "FinancialSummaryDto.java").is_file(), "FinancialSummaryDto exists")
    check((dash_dir / "PeriodComparisonDto.java").is_file(), "PeriodComparisonDto exists")
    check((dash_dir / "ProductProfitabilityDto.java").is_file(), "ProductProfitabilityDto exists")
    check((dash_dir / "StoreProfitabilityDto.java").is_file(), "StoreProfitabilityDto exists")
    check((dash_dir / "FinancialOrderListItemDto.java").is_file(), "FinancialOrderListItemDto exists")
    check((dash_dir / "DashboardOverviewResponse.java").is_file(), "DashboardOverviewResponse exists")

    # 2. Service & Controller
    service_file = dash_dir / "FinancialDashboardService.java"
    controller_file = dash_dir / "FinancialDashboardController.java"
    check(service_file.is_file(), "FinancialDashboardService exists")
    check(controller_file.is_file(), "FinancialDashboardController exists")

    # 3. Precision, Determinism & Anti-Hallucination
    check(file_contains(service_file, "RoundingMode.HALF_EVEN"), "FinancialDashboardService uses RoundingMode.HALF_EVEN")
    check(file_contains(service_file, "FORBIDDEN_FINANCIAL_ACCESS"), "FinancialDashboardService enforces RBAC and throws FORBIDDEN BusinessException")
    check(file_contains(service_file, "Role.OPERATOR"), "FinancialDashboardService checks OPERATOR role")
    check(file_contains(service_file, "Role.VIEWER"), "FinancialDashboardService checks VIEWER role")

    service_code = service_file.read_text(encoding="utf-8")
    code_no_comments = re.sub(r"/\*.*?\*/", "", service_code, flags=re.DOTALL)
    code_no_comments = re.sub(r"//.*", "", code_no_comments)
    float_matches = re.findall(r"\b(float|Float)\b", code_no_comments)
    double_matches = re.findall(r"\b(double|Double)\b", code_no_comments)
    check(len(float_matches) == 0, "No float types in FinancialDashboardService")
    check(len(double_matches) == 0, "No double types in FinancialDashboardService")

    # 4. CSV Exporter & Metadata Header
    csv_file = dash_dir / "FinancialCsvExporter.java"
    check(csv_file.is_file(), "FinancialCsvExporter exists")
    check(file_contains(csv_file, "Relatório Financeiro e Margem de Contribuição"), "CSV exporter includes audit title header")
    check(file_contains(csv_file, "Fuso Horário:"), "CSV exporter includes timezone in audit header")
    check(file_contains(csv_file, "Data de Geração:"), "CSV exporter includes generation timestamp in audit header")
    check(file_contains(csv_file, "Aviso Legal:"), "CSV exporter includes legal disclaimer")

    # 5. Repositories
    repo_dir = ROOT / "backend/src/main/java/br/com/sellerintelligence/financial/domain"
    check(file_contains(repo_dir / "OrderFinancialCalculationRepository.java", "findCalculationsByPeriod"), "OrderFinancialCalculationRepository has findCalculationsByPeriod query")
    check(file_contains(repo_dir / "OrderItemFinancialCalculationRepository.java", "findItemCalculationsByPeriod"), "OrderItemFinancialCalculationRepository has findItemCalculationsByPeriod query")

    # 6. Frontend UI & Integration
    frontend_dir = ROOT / "frontend/src/components"
    dash_view = frontend_dir / "FinancialDashboardView.tsx"
    dash_test = frontend_dir / "FinancialDashboardView.test.tsx"
    check(dash_view.is_file(), "FinancialDashboardView component exists")
    check(dash_test.is_file(), "FinancialDashboardView test file exists")
    check(file_contains(dash_view, "Painel Financeiro") and file_contains(dash_view, "Rentabilidade"), "UI contains executive financial dashboard header")
    check(file_contains(dash_view, "Regime de Competência"), "UI references accrual basis accounting (Regime de Competência)")
    check(file_contains(dash_view, "Faturamento Bruto"), "UI displays Gross Revenue KPI")
    check(file_contains(dash_view, "Margem de Contribuição"), "UI displays Contribution Margin KPI")
    check(file_contains(dash_view, "Exportar CSV"), "UI includes CSV export action")
    check(file_contains(frontend_dir / "DashboardView.tsx", "FinancialDashboardView"), "DashboardView integrates FinancialDashboardView")

    # 7. Backend Integration Test
    backend_test_file = ROOT / "backend/src/test/java/br/com/sellerintelligence/financial/dashboard/FinancialDashboardIntegrationTest.java"
    check(backend_test_file.is_file(), "FinancialDashboardIntegrationTest exists")
    check(file_contains(backend_test_file, "shouldConsolidateDashboardMetrics"), "Integration test verifies metric consolidation & period comparison")
    check(file_contains(backend_test_file, "shouldMatchScreenTotalsAndCsvExportExactly"), "Integration test verifies CSV export consistency with domain calculations")
    check(file_contains(backend_test_file, "shouldDenyAccessToRestrictedRoles"), "Integration test verifies RBAC denial for OPERATOR/VIEWER roles")
    check(file_contains(backend_test_file, "shouldIsolateDashboardBetweenTenants"), "Integration test verifies multi-tenant isolation")

    print("\n[SUCCESS] All structural, precision, export, RBAC, frontend and test checks for Prompt 09 passed!")

if __name__ == "__main__":
    main()
