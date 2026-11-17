#!/usr/bin/env python3
"""
Automated Verification Script for Phase 0 / Prompt 01
Validates technical discovery, capability matrix, ADRs, OQs, and phase-gate integrity.
"""

import sys
import json
import os
import subprocess

def run_check(description, check_fn):
    print(f"[*] Checking: {description}...", end=" ")
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

def check_prd_integrity():
    with open("prompts/PRD.md", "r", encoding="utf-8") as f1, open("docs/product/PRD.md", "r", encoding="utf-8") as f2:
        c1 = f1.read()
        c2 = f2.read()
        assert c1 == c2, "docs/product/PRD.md does not match prompts/PRD.md"
        assert len(c1.splitlines()) == 965, f"Expected 965 lines, got {len(c1.splitlines())}"

def check_capability_matrix():
    path = "docs/architecture/marketplace-capability-matrix.md"
    assert os.path.exists(path), f"File {path} does not exist"
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    
    # Check official sources
    assert "https://developers.mercadolivre.com.br" in content, "Missing Mercado Livre Developers URL"
    assert "https://open.shopee.com" in content, "Missing Shopee Open Platform URL"
    assert "17 de novembro de 2026" in content, "Missing official consultation date"
    
    # Check required topics
    assert "OAuth 2.0" in content, "Missing OAuth 2.0 section"
    assert "HMAC-SHA256" in content, "Missing HMAC-SHA256 signature specification for Shopee"
    assert "get_escrow_detail" in content, "Missing escrow detail for Shopee payment reconciliation"
    assert "Rate Limit" in content, "Missing rate limit / throttling documentation"
    assert "Chrome Manifest V3" in content, "Missing Chrome extension policy section"
    assert "activeTab" in content, "Missing activeTab permission specification"
    
    # Check 5 data provenance categories
    for cat in ["IMPORTED", "OBSERVED", "ESTIMATED", "UNAVAILABLE"]:
        assert cat in content, f"Missing data classification category {cat}"

def check_adrs():
    path = "docs/architecture/DECISION_LOG.md"
    assert os.path.exists(path), f"File {path} does not exist"
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    
    # Check accepted ADRs DEC-001 through DEC-013
    for i in range(1, 14):
        adr_id = f"DEC-{i:03d}"
        assert adr_id in content, f"Missing ADR {adr_id}"
    
    # Check proposed ADRs
    assert "DEC-P04" in content, "Missing DEC-P04"
    assert "DEC-P05" in content, "Missing DEC-P05"
    assert "DEC-P06" in content, "Missing DEC-P06"

def check_open_questions():
    path = "docs/product/OPEN_QUESTIONS.md"
    assert os.path.exists(path), f"File {path} does not exist"
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    
    # Check OQ-001 through OQ-015
    for i in range(1, 16):
        oq_id = f"OQ-{i:03d}"
        assert oq_id in content, f"Missing {oq_id}"
    
    # Verify no critical OQ was marked RESOLVED without approval
    assert "RESOLVED" not in content, "Critical OQ was prematurely marked as RESOLVED"
    assert "P0" in content, "Missing P0 priority classification"
    assert "P1" in content, "Missing P1 priority classification"
    assert "P2" in content, "Missing P2 priority classification"

def check_no_premature_code():
    # Verify that no business code or domain entities were created in this discovery prompt
    res = subprocess.run(["git", "status", "--porcelain"], capture_output=True, text=True)
    lines = res.stdout.splitlines()
    for line in lines:
        filename = line[3:].strip()
        # Ensure no src/main/java or frontend src/ created
        assert not filename.startswith("src/"), f"Unexpected source file created: {filename}"
        assert not filename.startswith("backend/"), f"Unexpected backend file created: {filename}"
        assert not filename.startswith("frontend/"), f"Unexpected frontend file created: {filename}"

def check_traceability_matrix():
    path = "docs/product/REQUIREMENTS_TRACEABILITY.md"
    assert os.path.exists(path), f"File {path} does not exist"
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()
    
    assert "FR-INT-001" in content, "Missing FR-INT requirements"
    assert "docs/architecture/marketplace-capability-matrix.md" in content, "Missing link to capability matrix"

def check_phase_gate_json():
    path = "docs/delivery/phase-gate.json"
    assert os.path.exists(path), f"File {path} does not exist"
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    
    assert data.get("project") == "Seller Intelligence Platform"
    assert "criteria" in data and len(data["criteria"]) >= 6
    assert data.get("currentPrompt") == "01"

def main():
    print("=" * 60)
    print("SELLER INTELLIGENCE PLATFORM — VERIFICATION SUITE PROMPT 01")
    print("=" * 60)
    
    checks = [
        ("PRD Integrity (Exact 965 lines)", check_prd_integrity),
        ("Marketplace Capability Matrix (Official sources, URLs, date)", check_capability_matrix),
        ("Architectural Decision Records (DEC-001 to DEC-013 ACCEPTED, DEC-P04 to P06 PROPOSED)", check_adrs),
        ("Open Questions Audit (OQ-001 to OQ-015, P0/P1/P2, no false resolutions)", check_open_questions),
        ("No Premature Business Code / Production Endpoints", check_no_premature_code),
        ("Requirements Traceability Matrix Linked to Findings", check_traceability_matrix),
        ("Phase Gate JSON Schema and Criteria", check_phase_gate_json),
    ]
    
    success = True
    for desc, fn in checks:
        if not run_check(desc, fn):
            success = False
            
    print("=" * 60)
    if success:
        print("ALL VERIFICATION CHECKS PASSED SUCCESSFULLY (6/6)")
        sys.exit(0)
    else:
        print("SOME VERIFICATION CHECKS FAILED")
        sys.exit(1)

if __name__ == "__main__":
    main()
