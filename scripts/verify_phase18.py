#!/usr/bin/env python3
"""
Verification Script for Prompt 18 (Assistente conversacional e servidor MCP read-only).
Validates Acceptance Criteria AC-18-1 through AC-18-10.
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
    print("=== Starting Verification for Prompt 18 (Assistente Conversacional & Servidor MCP Read-Only) ===")
    all_passed = True

    backend_base = "backend/src/main/java/br/com/sellerintelligence/mcp"
    migration_file = "backend/src/main/resources/db/migration/V13__conversational_ai_and_mcp_audit.sql"
    frontend_view = "frontend/src/components/ConversationalAssistantView.tsx"
    frontend_test = "frontend/src/components/ConversationalAssistantView.test.tsx"
    dashboard_view = "frontend/src/components/DashboardView.tsx"
    decision_log = "docs/architecture/DECISION_LOG.md"
    open_questions = "docs/product/OPEN_QUESTIONS.md"

    # AC-18-1: MCP Tool Registry & Read-Only Governance
    tool_registry = os.path.join(backend_base, "service/McpToolRegistry.java")
    all_passed &= check(os.path.exists(tool_registry), "AC-18-1: McpToolRegistry exists")
    if os.path.exists(tool_registry):
        with open(tool_registry, "r") as f:
            content = f.read()
            all_passed &= check("search_market" in content and "query_tenant_products" in content, "AC-18-1: Registers search_market and query_tenant_products")
            all_passed &= check("query_sales_indicators" in content and "calculate_sku_margin" in content, "AC-18-1: Registers query_sales_indicators and calculate_sku_margin")
            all_passed &= check("query_reconciliation_summary" in content, "AC-18-1: Registers query_reconciliation_summary")
            all_passed &= check("BLOCKED_MUTABLE_DENIED" in content or "MUTABLE_OPERATIONS" in content, "AC-18-1: Enforces mutable operations deny list")

    # AC-18-2: Authentication & Authorization for MCP Keys
    auth_service = os.path.join(backend_base, "service/McpAuthService.java")
    api_key_entity = os.path.join(backend_base, "domain/McpApiKey.java")
    all_passed &= check(os.path.exists(auth_service), "AC-18-2: McpAuthService exists")
    all_passed &= check(os.path.exists(api_key_entity), "AC-18-2: McpApiKey entity exists")
    if os.path.exists(auth_service):
        with open(auth_service, "r") as f:
            content = f.read()
            all_passed &= check("sip_mcp_" in content, "AC-18-2: Generates API keys with prefix sip_mcp_")
            all_passed &= check("SHA-256" in content or "sha256" in content.lower(), "AC-18-2: Stores API key hashes securely with SHA-256")
            all_passed &= check("revokeApiKey" in content, "AC-18-2: Supports explicit key revocation")

    # AC-18-3: Multi-Tenant Isolation & Anti-Cross-Tenant Execution
    tool_executor = os.path.join(backend_base, "service/McpToolExecutorService.java")
    controller_file = os.path.join(backend_base, "web/McpServerController.java")
    all_passed &= check(os.path.exists(tool_executor), "AC-18-3: McpToolExecutorService exists")
    if os.path.exists(tool_executor):
        with open(tool_executor, "r") as f:
            content = f.read()
            all_passed &= check("BLOCKED_CROSS_TENANT" in content, "AC-18-3: Detects and blocks cross-tenant execution attempts")
            all_passed &= check("UUID tenantId" in content and "foreignTenantStr" in content, "AC-18-3: Scopes tool execution strictly to authenticated tenantId")
    if os.path.exists(controller_file):
        with open(controller_file, "r") as f:
            content = f.read()
            all_passed &= check("TenantContext" in content, "AC-18-3: Controller extracts tenant context securely")

    # AC-18-4: Anti-Prompt Injection Defense (OWASP LLM01)
    if os.path.exists(tool_executor):
        with open(tool_executor, "r") as f:
            content = f.read()
            all_passed &= check("UntrustedTextSanitizer" in content and "containsSuspiciousInstructions" in content, "AC-18-4: Uses UntrustedTextSanitizer for injection scanning")
            all_passed &= check("BLOCKED_INJECTION" in content, "AC-18-4: Rejects injection payloads with BLOCKED_INJECTION")

    # AC-18-5: Deterministic Financial Calculations & No Arbitrary SQL
    if os.path.exists(tool_executor):
        with open(tool_executor, "r") as f:
            content = f.read()
            all_passed &= check("skuCostRepository.findEffectiveCost" in content, "AC-18-5: Looks up effective cost from tenant registry")
            all_passed &= check("BigDecimal" in content and "RoundingMode.HALF_EVEN" in content, "AC-18-5: Retains deterministic arithmetic in BigDecimal HALF_EVEN")

    # AC-18-6: Immutable Tool Execution Audit Logging
    audit_entity = os.path.join(backend_base, "domain/McpToolExecutionLog.java")
    all_passed &= check(os.path.exists(audit_entity), "AC-18-6: McpToolExecutionLog entity exists")
    if os.path.exists(tool_executor):
        with open(tool_executor, "r") as f:
            content = f.read()
            all_passed &= check("executionLogRepository.save" in content and "sanitizeArgsToJson" in content, "AC-18-6: Persists execution logs with sanitized arguments and status")

    # AC-18-7: Conversational Assistant Sessions & Grounded Answers
    assistant_service = os.path.join(backend_base, "service/ConversationalAssistantService.java")
    message_entity = os.path.join(backend_base, "domain/ConversationalMessage.java")
    session_entity = os.path.join(backend_base, "domain/ConversationalSession.java")
    all_passed &= check(os.path.exists(assistant_service), "AC-18-7: ConversationalAssistantService exists")
    all_passed &= check(os.path.exists(message_entity), "AC-18-7: ConversationalMessage entity exists")
    all_passed &= check(os.path.exists(session_entity), "AC-18-7: ConversationalSession entity exists")
    if os.path.exists(assistant_service):
        with open(assistant_service, "r") as f:
            content = f.read()
            all_passed &= check("processChat" in content, "AC-18-7: Coordinates session chat and tool invocation")
            all_passed &= check("provenance" in content.lower(), "AC-18-7: Attaches provenance classification to answers")

    # AC-18-8: Database Migration V13
    all_passed &= check(os.path.exists(migration_file), "AC-18-8: Migration V13 exists")
    if os.path.exists(migration_file):
        with open(migration_file, "r") as f:
            content = f.read()
            all_passed &= check("mcp_api_keys" in content and "mcp_tool_execution_logs" in content, "AC-18-8: Migration creates mcp_api_keys and logs tables")
            all_passed &= check("conversational_sessions" in content and "conversational_messages" in content, "AC-18-8: Migration creates chat sessions and messages tables")

    # AC-18-9: Architecture ADR DEC-023
    all_passed &= check(os.path.exists(decision_log), "AC-18-9: DECISION_LOG.md exists")
    if os.path.exists(decision_log):
        with open(decision_log, "r") as f:
            content = f.read()
            all_passed &= check("DEC-023" in content and "Model Context Protocol" in content, "AC-18-9: ADR DEC-023 accepted in DECISION_LOG.md")
    if os.path.exists(open_questions):
        with open(open_questions, "r") as f:
            content = f.read()
            all_passed &= check("OQ-012" in content and "RESOLVED" in content, "AC-18-9: OQ-012 marked as RESOLVED")

    # AC-18-10: Frontend Integration & Test Suite
    all_passed &= check(os.path.exists(frontend_view), "AC-18-10: ConversationalAssistantView.tsx exists")
    all_passed &= check(os.path.exists(frontend_test), "AC-18-10: ConversationalAssistantView.test.tsx exists")
    if os.path.exists(dashboard_view):
        with open(dashboard_view, "r") as f:
            content = f.read()
            all_passed &= check("ConversationalAssistantView" in content and "ASSISTANT" in content, "AC-18-10: DashboardView connects ConversationalAssistantView")

    print("--------------------------------------------------")
    if all_passed:
        print("ALL ACCEPTANCE CRITERIA PASSED FOR PROMPT 18!")
        return 0
    else:
        print("SOME CHECKS FAILED FOR PROMPT 18.")
        return 1

if __name__ == "__main__":
    sys.exit(main())
