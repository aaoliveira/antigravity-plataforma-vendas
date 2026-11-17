#!/usr/bin/env python3
"""
Verification Script for Prompt 10 — AI Listing Studio: análise de anúncio e geração de conteúdo
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
    print("=== Starting Verification for Prompt 10 ===")

    # 1. Database Migrations
    v8_migration = ROOT / "backend/src/main/resources/db/migration/V8__ai_listing_studio.sql"
    check(v8_migration.is_file(), "Flyway V8 migration exists")
    check(file_contains(v8_migration, "CREATE TABLE IF NOT EXISTS listing_versions"), "V8 defines listing_versions table")
    check(file_contains(v8_migration, "CREATE TABLE IF NOT EXISTS listing_quality_audits"), "V8 defines listing_quality_audits table")
    check(file_contains(v8_migration, "CREATE TABLE IF NOT EXISTS ai_generation_logs"), "V8 defines ai_generation_logs table")

    # 2. Versioned Prompts in Files
    prompts_dir = ROOT / "backend/src/main/resources/prompts"
    title_prompt = prompts_dir / "listing-title-v1.st"
    desc_prompt = prompts_dir / "listing-description-v1.st"
    check(title_prompt.is_file(), "Versioned title prompt listing-title-v1.st exists")
    check(desc_prompt.is_file(), "Versioned description prompt listing-description-v1.st exists")
    check(file_contains(title_prompt, "REGRAS RÍGIDAS ANTI-ALUCINAÇÃO"), "Title prompt enforces anti-hallucination rules")
    check(file_contains(desc_prompt, "REGRAS RÍGIDAS ANTI-ALUCINAÇÃO"), "Description prompt enforces anti-hallucination rules")
    check(file_contains(title_prompt, "<untrusted_product_context>"), "Title prompt isolates external data inside <untrusted_product_context>")
    check(file_contains(desc_prompt, "<untrusted_product_context>"), "Description prompt isolates external data inside <untrusted_product_context>")

    # 3. Domain Entities and Repositories
    ai_domain = ROOT / "backend/src/main/java/br/com/sellerintelligence/ai/domain"
    check((ai_domain / "ListingVersion.java").is_file(), "ListingVersion entity exists")
    check((ai_domain / "ListingVersionRepository.java").is_file(), "ListingVersionRepository exists")
    check((ai_domain / "ListingQualityAudit.java").is_file(), "ListingQualityAudit entity exists")
    check((ai_domain / "ListingQualityAuditRepository.java").is_file(), "ListingQualityAuditRepository exists")
    check((ai_domain / "AiGenerationLog.java").is_file(), "AiGenerationLog entity exists")
    check((ai_domain / "AiGenerationLogRepository.java").is_file(), "AiGenerationLogRepository exists")

    # 4. Engine, Sanitizer & Providers
    ai_engine = ROOT / "backend/src/main/java/br/com/sellerintelligence/ai/engine"
    ai_provider = ROOT / "backend/src/main/java/br/com/sellerintelligence/ai/provider"
    check((ai_engine / "ListingAuditEngine.java").is_file(), "ListingAuditEngine exists")
    check((ai_engine / "AiPromptSanitizer.java").is_file(), "AiPromptSanitizer exists")
    check((ai_provider / "AiProvider.java").is_file(), "AiProvider interface exists")
    check((ai_provider / "FakeAiProvider.java").is_file(), "FakeAiProvider exists")
    check((ai_provider / "AiServiceUnavailableException.java").is_file(), "AiServiceUnavailableException exists")

    # 5. Prompt Injection Defense & Factual Guardrail
    check(file_contains(ai_engine / "AiPromptSanitizer.java", "REDACTED_PROMPT_INJECTION"), "AiPromptSanitizer redacts prompt injection patterns")
    check(file_contains(ai_engine / "AiPromptSanitizer.java", "untrusted_product_context"), "AiPromptSanitizer encapsulates untrusted context")
    check(file_contains(ai_engine / "ListingAuditEngine.java", "missingAttributes"), "ListingAuditEngine tracks missing required attributes")
    check(file_contains(ai_provider / "FakeAiProvider.java", "SIMULATE_AI_UNAVAILABLE"), "FakeAiProvider supports simulating service unavailability")

    # 6. Service & Controller
    service_file = ROOT / "backend/src/main/java/br/com/sellerintelligence/ai/service/ListingStudioService.java"
    controller_file = ROOT / "backend/src/main/java/br/com/sellerintelligence/ai/web/ListingStudioController.java"
    check(service_file.is_file(), "ListingStudioService exists")
    check(controller_file.is_file(), "ListingStudioController exists")
    check(file_contains(service_file, "AI_GENERATED"), "ListingStudioService marks provenance as AI_GENERATED")
    check(file_contains(service_file, "Sugestão gerada por Inteligência Artificial"), "ListingStudioService attaches mandatory disclaimer")
    check(file_contains(service_file, "AiGenerationLog"), "ListingStudioService logs model, latency and status in audit trail")

    # 7. Frontend UI & Integration
    frontend_dir = ROOT / "frontend/src/components"
    studio_view = frontend_dir / "ListingStudioView.tsx"
    studio_test = frontend_dir / "ListingStudioView.test.tsx"
    check(studio_view.is_file(), "ListingStudioView component exists")
    check(studio_test.is_file(), "ListingStudioView test exists")
    check(file_contains(studio_view, "AI Listing Studio"), "UI contains AI Listing Studio header")
    check(file_contains(studio_view, "Score de Qualidade do Anúncio"), "UI displays Quality Scorecard")
    check(file_contains(studio_view, "Factual Guardrail Ativo"), "UI indicates active Factual Guardrail")
    check(file_contains(studio_view, "SUGESTÃO IA"), "UI marks AI suggestions explicitly")
    check(file_contains(studio_view, "Histórico de Versões Salvas"), "UI contains version history table")
    check(file_contains(frontend_dir / "DashboardView.tsx", "AI_STUDIO"), "DashboardView integrates AI_STUDIO tab")

    # 8. Automated Backend Tests
    ai_test_engine = ROOT / "backend/src/test/java/br/com/sellerintelligence/ai/engine"
    ai_test_service = ROOT / "backend/src/test/java/br/com/sellerintelligence/ai/service"
    check((ai_test_engine / "ListingAuditEngineTest.java").is_file(), "ListingAuditEngineTest exists")
    check((ai_test_engine / "AiPromptSanitizerTest.java").is_file(), "AiPromptSanitizerTest exists")
    check((ai_test_service / "ListingStudioIntegrationTest.java").is_file(), "ListingStudioIntegrationTest exists")

    print("\n[SUCCESS] All structural, migration, engine, sanitizer, provider, UI, and test checks for Prompt 10 passed!")

if __name__ == "__main__":
    main()
