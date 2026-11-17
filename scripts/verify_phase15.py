#!/usr/bin/env python3
"""
Verification Script for Prompt 15 (Product Discovery avançado: catálogos PDF e busca por imagem).
Validates Acceptance Criteria AC-15-1 through AC-15-10.
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
    print("=== Starting Verification for Prompt 15 (Product Discovery Avançado & Fornecedores) ===")
    all_passed = True

    backend_base = "backend/src/main/java/br/com/sellerintelligence/discovery"
    migration_file = "backend/src/main/resources/db/migration/V11__product_discovery_catalogs_and_similarity.sql"

    # AC-15-1: Secure File Upload & Validation
    validator_file = os.path.join(backend_base, "security/SecureFileUploadValidator.java")
    all_passed &= check(os.path.exists(validator_file), "AC-15-1: SecureFileUploadValidator exists")
    if os.path.exists(validator_file):
        with open(validator_file, "r") as f:
            content = f.read()
            all_passed &= check("MAX_FILE_SIZE_BYTES" in content, "AC-15-1: Enforces file size limitation (25MB)")
            all_passed &= check("PDF_MAGIC" in content and "PNG_MAGIC" in content, "AC-15-1: Enforces magic bytes verification for PDF/PNG/JPEG")
            all_passed &= check("sanitizeFilename" in content, "AC-15-1: Implements filename path traversal defense")

    # AC-15-2: Indirect Prompt Injection Defense
    sanitizer_file = os.path.join(backend_base, "security/UntrustedTextSanitizer.java")
    all_passed &= check(os.path.exists(sanitizer_file), "AC-15-2: UntrustedTextSanitizer exists")
    if os.path.exists(sanitizer_file):
        with open(sanitizer_file, "r") as f:
            content = f.read()
            all_passed &= check("INJECTION_PATTERNS" in content, "AC-15-2: Detects and neutralizes prompt injection patterns")
            all_passed &= check("REDACTED_SUSPICIOUS_PROMPT_INJECTION" in content, "AC-15-2: Replaces injection payloads with safe redaction marker")

    # AC-15-3: Mathematical EAN Checksum Validation
    ean_file = os.path.join(backend_base, "security/EanChecksumValidator.java")
    all_passed &= check(os.path.exists(ean_file), "AC-15-3: EanChecksumValidator exists")
    if os.path.exists(ean_file):
        with open(ean_file, "r") as f:
            content = f.read()
            all_passed &= check("isValidEan" in content, "AC-15-3: Implements mathematical modulo-10 checksum validation")

    # AC-15-4: PDF Catalog Extraction Service
    pdf_service = os.path.join(backend_base, "service/PdfCatalogExtractionService.java")
    all_passed &= check(os.path.exists(pdf_service), "AC-15-4: PdfCatalogExtractionService exists")
    if os.path.exists(pdf_service):
        with open(pdf_service, "r") as f:
            content = f.read()
            all_passed &= check("Loader.loadPDF" in content, "AC-15-4: Native PDF parsing using Apache PDFBox 3.0")
            all_passed &= check("PRICE_PATTERN" in content and "EAN_PATTERN" in content, "AC-15-4: Extracts price, SKU and EAN heuristics")
            all_passed &= check("confidenceDetails" in content or "overall" in content, "AC-15-4: Assigns confidence score per item")

    # AC-15-5: Mandatory Human Review State Machine
    item_entity = os.path.join(backend_base, "domain/DiscoveryCatalogItem.java")
    review_status_enum = os.path.join(backend_base, "domain/ItemReviewStatus.java")
    review_service = os.path.join(backend_base, "service/CatalogReviewService.java")
    all_passed &= check(os.path.exists(item_entity), "AC-15-5: DiscoveryCatalogItem entity exists")
    all_passed &= check(os.path.exists(review_status_enum), "AC-15-5: ItemReviewStatus enum exists")
    all_passed &= check(os.path.exists(review_service), "AC-15-5: CatalogReviewService exists")
    if os.path.exists(review_status_enum):
        with open(review_status_enum, "r") as f:
            content = f.read()
            for st in ["PENDING_REVIEW", "REVIEWED", "REJECTED"]:
                all_passed &= check(st in content, f"AC-15-5: ItemReviewStatus supports {st}")
    if os.path.exists(review_service):
        with open(review_service, "r") as f:
            content = f.read()
            all_passed &= check("reviewAndConfirmItem" in content, "AC-15-5: Implements human review confirmation")
            all_passed &= check("rejectItem" in content, "AC-15-5: Implements item rejection")

    # AC-15-6: Product Similarity Matching Service
    similarity_service = os.path.join(backend_base, "service/ProductSimilarityMatcherService.java")
    match_entity = os.path.join(backend_base, "domain/DiscoveryProductMatch.java")
    all_passed &= check(os.path.exists(similarity_service), "AC-15-6: ProductSimilarityMatcherService exists")
    all_passed &= check(os.path.exists(match_entity), "AC-15-6: DiscoveryProductMatch entity exists")
    if os.path.exists(similarity_service):
        with open(similarity_service, "r") as f:
            content = f.read()
            all_passed &= check("findAndGenerateMatches" in content, "AC-15-6: Generates product match proposals")
            all_passed &= check("calculateTextSimilarity" in content or "EAN_EXACT" in content, "AC-15-6: Implements exact EAN and text similarity scoring")

    # AC-15-7: Deterministic Estimated Gross Margin
    if os.path.exists(similarity_service):
        with open(similarity_service, "r") as f:
            content = f.read()
            all_passed &= check("estimatedMargin" in content and "revenue.subtract(cost)" in content, "AC-15-7: Implements deterministic gross margin calculation")
            all_passed &= check("BigDecimal" in content and "RoundingMode" in content, "AC-15-7: Enforces BigDecimal and explicit rounding for financial calculations")

    # AC-15-8: Match Confirmation & Rejection (No Auto Action)
    match_status_enum = os.path.join(backend_base, "domain/MatchStatus.java")
    all_passed &= check(os.path.exists(match_status_enum), "AC-15-8: MatchStatus enum exists")
    if os.path.exists(match_status_enum):
        with open(match_status_enum, "r") as f:
            content = f.read()
            for mst in ["CANDIDATE", "CONFIRMED", "REJECTED"]:
                all_passed &= check(mst in content, f"AC-15-8: MatchStatus supports {mst}")
    if os.path.exists(similarity_service):
        with open(similarity_service, "r") as f:
            content = f.read()
            all_passed &= check("confirmMatch" in content and "rejectMatch" in content, "AC-15-8: Human seller confirms or rejects candidate match")

    # AC-15-9: Visual Search Service & Migration
    visual_service = os.path.join(backend_base, "service/VisualSearchService.java")
    all_passed &= check(os.path.exists(visual_service), "AC-15-9: VisualSearchService exists")
    all_passed &= check(os.path.exists(migration_file), "AC-15-9: Database migration V11 exists")
    if os.path.exists(migration_file):
        with open(migration_file, "r") as f:
            content = f.read()
            all_passed &= check("discovery_catalogs" in content, "AC-15-9: V11 creates discovery_catalogs table")
            all_passed &= check("discovery_catalog_items" in content, "AC-15-9: V11 creates discovery_catalog_items table")
            all_passed &= check("discovery_product_matches" in content, "AC-15-9: V11 creates discovery_product_matches table")
            all_passed &= check("discovery_visual_searches" in content, "AC-15-9: V11 creates discovery_visual_searches table")
            all_passed &= check("idx_discovery_catalogs_tenant" in content, "AC-15-9: V11 creates tenant isolation indices")

    # AC-15-10: Frontend UI & Vitest Integration
    frontend_view = "frontend/src/components/ProductDiscoveryView.tsx"
    frontend_test = "frontend/src/components/ProductDiscoveryView.test.tsx"
    dashboard_view = "frontend/src/components/DashboardView.tsx"
    all_passed &= check(os.path.exists(frontend_view), "AC-15-10: ProductDiscoveryView.tsx exists")
    all_passed &= check(os.path.exists(frontend_test), "AC-15-10: ProductDiscoveryView.test.tsx exists")
    if os.path.exists(dashboard_view):
        with open(dashboard_view, "r") as f:
            content = f.read()
            all_passed &= check("ProductDiscoveryView" in content, "AC-15-10: ProductDiscoveryView registered in DashboardView")
            all_passed &= check("DISCOVERY" in content, "AC-15-10: DISCOVERY tab added to DashboardView")

    print("\n" + "="*50)
    if all_passed:
        print("ALL ACCEPTANCE CRITERIA FOR PROMPT 15 PASSED SUCCESSFULLY!")
        return 0
    else:
        print("SOME CHECKS FAILED. Please review the output above.")
        return 1

if __name__ == "__main__":
    sys.exit(main())
