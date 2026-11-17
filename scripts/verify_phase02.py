#!/usr/bin/env python3
"""
Automated Verification Suite for Phase 1 / Prompt 02 (Bootstrap do Monorepo)
Validates repository structure, Docker Compose, Backend compilation, Frontend lint & test,
Flyway minimal migration, and absence of premature business entities.
"""

import sys
import json
import os
import subprocess

def run_check(description, check_fn):
    print(f"[*] Checking: {description}...", end=" ", flush=True)
    try:
        check_fn()
        print("PASSED")
        return True
    except AssertionError as e:
        print(f"FAILED: {e}")
        return False
    except Exception as e:
        print(f"ERROR: {e}")
        return False

def check_directory_structure():
    required_paths = [
        "backend/pom.xml",
        "backend/mvnw",
        "backend/src/main/java/br/com/sellerintelligence/SellerIntelligenceApplication.java",
        "backend/src/main/resources/application.yml",
        "backend/src/main/resources/db/migration/V1__initial_infrastructure.sql",
        "frontend/package.json",
        "frontend/pnpm-lock.yaml",
        "frontend/tsconfig.json",
        "frontend/src/app/page.tsx",
        "frontend/src/components/HealthChecker.tsx",
        "docker-compose.yml",
        ".env.example",
        ".gitignore",
        ".github/workflows/ci.yml"
    ]
    for p in required_paths:
        assert os.path.exists(p), f"Required path missing: {p}"

def check_docker_compose_config():
    res = subprocess.run(["docker", "compose", "config"], capture_output=True, text=True)
    assert res.returncode == 0, f"docker compose config failed: {res.stderr}"
    assert "postgres" in res.stdout, "PostgreSQL service missing in docker-compose"
    assert "redis" in res.stdout, "Redis service missing in docker-compose"
    assert "rabbitmq" in res.stdout, "RabbitMQ service missing in docker-compose"
    assert "keycloak" in res.stdout, "Keycloak service missing in docker-compose"

def check_security_and_env():
    # Verify .gitignore contains .env
    with open(".gitignore", "r", encoding="utf-8") as f:
        git_content = f.read()
    assert ".env" in git_content, ".env must be ignored in .gitignore"
    
    # Verify .env.example contains only placeholders/dev defaults
    with open(".env.example", "r", encoding="utf-8") as f:
        env_content = f.read()
    assert "DO NOT commit real credentials" in env_content
    assert "postgres_dev_password" in env_content, "Expected dev placeholder password"
    assert "admin_dev_password" in env_content, "Expected dev placeholder admin password"

def check_flyway_minimal_infrastructure():
    migration_path = "backend/src/main/resources/db/migration/V1__initial_infrastructure.sql"
    with open(migration_path, "r", encoding="utf-8") as f:
        sql = f.read().lower()
    
    # Must NOT contain business tables
    forbidden_tables = ["seller", "store", "product", "listing", "order", "marketplace_connection", "campaign"]
    for table in forbidden_tables:
        assert f"create table {table}" not in sql and f"create table if not exists {table}" not in sql, \
            f"Premature business entity found in V1 migration: {table}"

def check_backend_compilation_and_tests():
    res = subprocess.run(["./mvnw", "test"], cwd="backend", capture_output=True, text=True)
    assert res.returncode == 0, f"Backend maven test failed:\n{res.stdout}\n{res.stderr}"
    assert "BUILD SUCCESS" in res.stdout, "Maven build did not finish with BUILD SUCCESS"

def check_frontend_lint_and_tests():
    # Run vitest
    res_test = subprocess.run(["pnpm", "run", "test"], cwd="frontend", capture_output=True, text=True)
    assert res_test.returncode == 0, f"Frontend vitest failed:\n{res_test.stdout}\n{res_test.stderr}"
    assert "passed" in res_test.stdout, "Vitest tests did not pass"
    
    # Run lint
    res_lint = subprocess.run(["pnpm", "run", "lint"], cwd="frontend", capture_output=True, text=True)
    assert res_lint.returncode == 0, f"Frontend lint failed:\n{res_lint.stdout}\n{res_lint.stderr}"

def check_phase_gate_json():
    path = "docs/delivery/phase-gate.json"
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    assert data.get("currentPrompt") == "02"
    assert "criteria" in data and len(data["criteria"]) >= 8

def main():
    print("=" * 60)
    print("SELLER INTELLIGENCE PLATFORM — VERIFICATION SUITE PROMPT 02")
    print("=" * 60)
    
    checks = [
        ("Monorepo Directory Structure Conforming to Stack", check_directory_structure),
        ("Docker Compose Configuration Syntax & Services", check_docker_compose_config),
        ("Security Hygiene (.gitignore & .env.example placeholders)", check_security_and_env),
        ("Flyway Minimal Infrastructure (No premature business tables)", check_flyway_minimal_infrastructure),
        ("Backend Compilation, ArchUnit & Health Tests (Maven / Java 21)", check_backend_compilation_and_tests),
        ("Frontend Lint, TypeScript Strict & Component Tests (Next.js / Vitest)", check_frontend_lint_and_tests),
        ("Phase Gate JSON Schema Integrity", check_phase_gate_json),
    ]
    
    success = True
    for desc, fn in checks:
        if not run_check(desc, fn):
            success = False
            
    print("=" * 60)
    if success:
        print("ALL BOOTSTRAP VERIFICATION CHECKS PASSED (7/7)")
        sys.exit(0)
    else:
        print("SOME BOOTSTRAP CHECKS FAILED")
        sys.exit(1)

if __name__ == "__main__":
    main()
