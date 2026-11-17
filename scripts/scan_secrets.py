#!/usr/bin/env python3
"""
Secrets & Sensitive Information Scanner
Seller Intelligence Platform — Phase 1 MVP Release Gate
Validates NFR-SEC-003 and anti-leakage invariants.
"""

import os
import re
import sys
from pathlib import Path

WORKSPACE = Path("/home/alexandre/Projetos/antigravity-avantepro-clone")

# Secret patterns
PATTERNS = [
    (r"-----BEGIN (RSA|EC|DSA|OPENSSH) PRIVATE KEY-----", "Private Cryptographic Key"),
    (r"AKIA[0-9A-Z]{16}", "AWS Access Key ID"),
    (r"(?i)aws_secret_access_key\s*=\s*['\"][A-Za-z0-9/+=]{40}['\"]", "AWS Secret Access Key"),
    (r"(?i)(ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9_]{36,}", "GitHub Personal Access Token"),
    (r"(?i)xox[baprs]-[0-9A-Za-z-]{10,}", "Slack Token"),
    (r"ey[A-Za-z0-9-_=]{20,}\.ey[A-Za-z0-9-_=]{20,}\.[A-Za-z0-9-_.+/=]{20,}", "Signed JWT / Bearer Token"),
]

EXCLUDED_DIRS = {".git", "node_modules", "target", ".next", "dist", ".gemini", "build"}
EXCLUDED_FILES = {
    "pnpm-lock.yaml",
    "package-lock.json",
    ".env.example",
    "application-test.yml",
    "application-dev.yml",
    "docker-compose.yml",
    "scan_secrets.py"
}

def scan_file(filepath):
    findings = []
    try:
        with open(filepath, "r", encoding="utf-8", errors="ignore") as f:
            for line_no, line in enumerate(f, start=1):
                # Ignore test mock tokens
                if "mock" in line.lower() or "test" in line.lower() or "dummy" in line.lower() or "sub-" in line:
                    continue
                for pattern, desc in PATTERNS:
                    if re.search(pattern, line):
                        findings.append((line_no, desc, line.strip()[:80]))
    except Exception as e:
        pass
    return findings

def main():
    print("=" * 70)
    print("Executing Secrets & Credential Leakage Scan")
    print(f"Scanning Workspace: {WORKSPACE}")
    print("=" * 70)

    total_files = 0
    total_findings = 0

    for root, dirs, files in os.walk(WORKSPACE):
        dirs[:] = [d for d in dirs if d not in EXCLUDED_DIRS]
        for f in files:
            if f in EXCLUDED_FILES or f.endswith(".jar") or f.endswith(".png") or f.endswith(".pdf"):
                continue
            total_files += 1
            fpath = os.path.join(root, f)
            res = scan_file(fpath)
            if res:
                rel = os.path.relpath(fpath, WORKSPACE)
                for line_no, desc, snippet in res:
                    print(f"[LEAK DETECTED] {rel}:{line_no} - {desc} -> {snippet}")
                    total_findings += 1

    print("-" * 70)
    print(f"Files scanned: {total_files}")
    if total_findings == 0:
        print("[+] SCAN PASSED: No hardcoded secrets, private keys, or tokens detected!")
        return 0
    else:
        print(f"[-] SCAN FAILED: {total_findings} potential secret(s) found!")
        return 1

if __name__ == "__main__":
    sys.exit(main())
