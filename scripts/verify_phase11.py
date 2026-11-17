#!/usr/bin/env python3
"""
Verification Script for Prompt 11 — Extensão Chrome Manifest V3 complementar
Fase 1 — MVP / P0
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
    print("=== Starting Verification for Prompt 11 ===")

    # 1. Manifest V3 Configuration
    manifest_file = ROOT / "extension/public/manifest.json"
    check(manifest_file.is_file(), "Manifest V3 file exists")

    manifest = json.loads(manifest_file.read_text(encoding="utf-8"))
    check(manifest.get("manifest_version") == 3, "Manifest version is strictly 3")
    check("activeTab" in manifest.get("permissions", []), "Manifest declares minimum permission 'activeTab'")
    check("storage" in manifest.get("permissions", []), "Manifest declares minimum permission 'storage'")
    check("<all_urls>" not in manifest.get("permissions", []), "Manifest does NOT request dangerous '<all_urls>' permission")
    check("cookies" not in manifest.get("permissions", []), "Manifest does NOT request marketplace cookies permission")
    check("action" in manifest and "default_popup" in manifest["action"], "Manifest defines action default_popup")
    check("background" in manifest and manifest["background"].get("type") == "module", "Manifest defines ES module service_worker")
    check("content_scripts" in manifest, "Manifest defines content_scripts for supported marketplaces")

    # 2. Parsers and Domain Logic
    parsers_dir = ROOT / "extension/src/parsers"
    parser_file = parsers_dir / "marketplaceUrlParser.ts"
    check(parser_file.is_file(), "marketplaceUrlParser exists")
    check(file_contains(parser_file, "MLB"), "Parser handles Mercado Livre canonical IDs")
    check(file_contains(parser_file, "shopee.com.br"), "Parser handles Shopee canonical URLs")
    check(file_contains(parser_file, "/checkout/"), "Parser blocks private checkout pages")
    check(file_contains(parser_file, "/carrinho"), "Parser blocks cart pages")

    # 3. Anti-Scraping & Security Guard
    sec_dir = ROOT / "extension/src/security"
    guard_file = sec_dir / "antiScrapingGuard.ts"
    check(guard_file.is_file(), "antiScrapingGuard exists")
    check(file_contains(guard_file, "READ_COOKIES"), "Guard explicitly prohibits reading cookies")
    check(file_contains(guard_file, "AUTOMATE_PURCHASE"), "Guard explicitly prohibits purchase automation")
    check(file_contains(guard_file, "CHANGE_PRICE"), "Guard explicitly prohibits price alteration")
    check(file_contains(guard_file, "PUBLISH_EXTERNAL_LISTING"), "Guard explicitly prohibits external listing publication")
    check(file_contains(guard_file, "javascript:"), "Guard blocks javascript: URL schemes")

    # 4. Background and Content Script
    check((ROOT / "extension/src/background/serviceWorker.ts").is_file(), "serviceWorker exists")
    check((ROOT / "extension/src/content/contentScript.ts").is_file(), "contentScript exists")

    # 5. Popup React UI
    popup_dir = ROOT / "extension/src/popup"
    check((popup_dir / "Popup.tsx").is_file(), "Popup component exists")
    check((popup_dir / "main.tsx").is_file(), "Popup main entry exists")
    check(file_contains(popup_dir / "Popup.tsx", "Seller Intelligence"), "Popup renders platform title")
    check(file_contains(popup_dir / "Popup.tsx", "Garantia de Privacidade"), "Popup presents privacy guarantee notice")
    check(file_contains(popup_dir / "Popup.tsx", "Otimizar no AI Listing Studio"), "Popup includes action button for AI Listing Studio")
    check(file_contains(popup_dir / "Popup.tsx", "Pesquisar Concorrência"), "Popup includes action button for Market Intelligence")

    # 6. Documentation & Build Setup
    readme_file = ROOT / "extension/README.md"
    check(readme_file.is_file(), "README.md exists in extension directory")
    check(file_contains(readme_file, "chrome://extensions"), "README documents installation via chrome://extensions")
    check(file_contains(readme_file, "Modo do desenvolvedor"), "README documents Developer Mode")
    check(file_contains(readme_file, "pnpm build"), "README documents build process")

    # 7. Distribution Build
    dist_dir = ROOT / "extension/dist"
    check((dist_dir / "manifest.json").is_file(), "Built dist/manifest.json exists")
    check((dist_dir / "popup.html").is_file(), "Built dist/popup.html exists")
    check((dist_dir / "serviceWorker.js").is_file(), "Built dist/serviceWorker.js exists")
    check((dist_dir / "contentScript.js").is_file(), "Built dist/contentScript.js exists")

    # 8. Automated Tests
    check((parsers_dir / "marketplaceUrlParser.test.ts").is_file(), "marketplaceUrlParser tests exist")
    check((sec_dir / "antiScrapingGuard.test.ts").is_file(), "antiScrapingGuard tests exist")
    check((popup_dir / "Popup.test.tsx").is_file(), "Popup tests exist")

    print("\n[SUCCESS] All Manifest V3, parser, anti-scraping, security, UI, build and test checks for Prompt 11 passed!")

if __name__ == "__main__":
    main()
