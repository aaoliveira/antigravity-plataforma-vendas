package br.com.sellerintelligence.discovery.service;

import br.com.sellerintelligence.discovery.domain.*;
import br.com.sellerintelligence.discovery.security.EanChecksumValidator;
import br.com.sellerintelligence.discovery.security.UntrustedTextSanitizer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfCatalogExtractionService {

    private static final Logger log = LoggerFactory.getLogger(PdfCatalogExtractionService.class);

    private static final Pattern PRICE_PATTERN = Pattern.compile("(?i)(?:(?:pre[çc]o|custo|valor):?\\s*)?(?:R\\$\\s*)?(\\d{1,6}(?:[.,]\\d{2}))");
    private static final Pattern EAN_PATTERN = Pattern.compile("\\b(\\d{13}|\\d{8}|\\d{14})\\b");
    private static final Pattern SKU_PATTERN = Pattern.compile("(?i)(?:c[oó]d(?:igo)?|sku|ref):?\\s*([a-zA-Z0-9-_]{3,20})");
    private static final Pattern WEIGHT_PATTERN = Pattern.compile("(?i)(\\d+(?:[.,]\\d+)?)\\s*(?:kg|g|quilos?)\\b");

    private final DiscoveryCatalogItemRepository itemRepository;
    private final UntrustedTextSanitizer textSanitizer;
    private final EanChecksumValidator eanValidator;

    public PdfCatalogExtractionService(DiscoveryCatalogItemRepository itemRepository,
                                       UntrustedTextSanitizer textSanitizer,
                                       EanChecksumValidator eanValidator) {
        this.itemRepository = itemRepository;
        this.textSanitizer = textSanitizer;
        this.eanValidator = eanValidator;
    }

    @Transactional
    public List<DiscoveryCatalogItem> extractItemsFromPdf(DiscoveryCatalog catalog, byte[] pdfBytes) {
        log.info("Starting PDF catalog extraction for catalogId={}, size={} bytes", catalog.getId(), pdfBytes.length);

        String fullText = "";
        boolean isScannedOnly = false;

        try (PDDocument document = Loader.loadPDF(new RandomAccessReadBuffer(pdfBytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            fullText = stripper.getText(document);
        } catch (Exception ex) {
            log.error("Failed to parse PDF document with PDFBox: {}", ex.getMessage());
            catalog.markFailed("Erro ao ler estrutura do PDF: " + ex.getMessage());
            return Collections.emptyList();
        }

        if (fullText == null || fullText.trim().length() < 10) {
            log.warn("PDF contains little or no text layer for catalogId={}. Flagging for OCR requirement.", catalog.getId());
            isScannedOnly = true;
        }

        // Sanitiza texto do documento contra Prompt Injection indireto
        String sanitizedFullText = textSanitizer.sanitize(fullText);

        List<DiscoveryCatalogItem> extractedItems = parseItemsFromText(catalog, sanitizedFullText, isScannedOnly);

        if (extractedItems.isEmpty() && !isScannedOnly) {
            // Cria ao menos um item preliminar baseado no título do documento
            extractedItems.add(createFallbackItem(catalog, sanitizedFullText));
        }

        List<DiscoveryCatalogItem> savedItems = itemRepository.saveAll(extractedItems);
        catalog.markParsed(savedItems.size());
        log.info("Extracted {} items from catalogId={}", savedItems.size(), catalog.getId());
        return savedItems;
    }

    private List<DiscoveryCatalogItem> parseItemsFromText(DiscoveryCatalog catalog, String text, boolean isScannedOnly) {
        List<DiscoveryCatalogItem> items = new ArrayList<>();
        if (text == null || text.isBlank()) {
            if (isScannedOnly) {
                // Item preliminar sinalizando OCR
                DiscoveryCatalogItem ocrItem = new DiscoveryCatalogItem(
                        catalog.getId(),
                        catalog.getTenantId(),
                        "Documento Digitalizado (Requer OCR)",
                        "SCAN-001",
                        null,
                        "O arquivo enviado não possui camada textual vetorial. Requer processamento OCR óptico.",
                        null,
                        null,
                        null,
                        ExtractionMethod.OCR,
                        new BigDecimal("0.4000"),
                        "{\"ocrRequired\": true, \"textLayerFound\": false}"
                );
                items.add(ocrItem);
            }
            return items;
        }

        String[] lines = text.split("\r?\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.length() < 8) continue;

            // Busca preço na linha
            Matcher priceMatcher = PRICE_PATTERN.matcher(line);
            if (priceMatcher.find()) {
                String priceStr = priceMatcher.group(1).replace(".", "").replace(",", ".");
                BigDecimal price;
                try {
                    price = new BigDecimal(priceStr);
                } catch (Exception e) {
                    continue;
                }

                // Busca EAN
                String ean = null;
                boolean validEan = false;
                Matcher eanMatcher = EAN_PATTERN.matcher(line);
                if (eanMatcher.find()) {
                    String candidateEan = eanMatcher.group(1);
                    if (eanValidator.isValidEan(candidateEan)) {
                        ean = candidateEan;
                        validEan = true;
                    }
                }

                // Busca SKU
                String sku = null;
                Matcher skuMatcher = SKU_PATTERN.matcher(line);
                if (skuMatcher.find()) {
                    sku = skuMatcher.group(1);
                }

                // Extrai nome removendo preço e códigos
                String name = line.replaceAll(PRICE_PATTERN.pattern(), "")
                        .replaceAll(EAN_PATTERN.pattern(), "")
                        .replaceAll(SKU_PATTERN.pattern(), "")
                        .replaceAll("(?i)\\b(?:item|produto|preco|custo|ean|sku|ref):?", "")
                        .trim();
                if (name.length() < 3) {
                    name = "Produto " + (sku != null ? sku : "Catálogo");
                }

                // Calcula scores de confiança
                BigDecimal nameConf = new BigDecimal("0.9500");
                BigDecimal eanConf = validEan ? new BigDecimal("1.0000") : new BigDecimal("0.3000");
                BigDecimal priceConf = new BigDecimal("0.9000");

                BigDecimal overall = nameConf.add(eanConf).add(priceConf).divide(new BigDecimal("3"), 4, RoundingMode.HALF_EVEN);
                String confidenceDetails = String.format("{\"name\":%s,\"ean\":%s,\"price\":%s,\"eanValid\":%b}",
                        nameConf, eanConf, priceConf, validEan);

                DiscoveryCatalogItem item = new DiscoveryCatalogItem(
                        catalog.getId(),
                        catalog.getTenantId(),
                        name,
                        sku,
                        ean,
                        "Extraído do catálogo " + catalog.getFilename(),
                        price,
                        null,
                        null,
                        ExtractionMethod.PDF_TEXT,
                        overall,
                        confidenceDetails
                );
                items.add(item);
            }
        }
        return items;
    }

    private DiscoveryCatalogItem createFallbackItem(DiscoveryCatalog catalog, String text) {
        String cleanSnippet = text.length() > 200 ? text.substring(0, 200) : text;
        return new DiscoveryCatalogItem(
                catalog.getId(),
                catalog.getTenantId(),
                "Item Catálogo: " + catalog.getFilename().replaceAll("(?i)\\.pdf$", ""),
                "CAT-" + System.currentTimeMillis() % 10000,
                null,
                cleanSnippet,
                null,
                null,
                null,
                ExtractionMethod.PDF_TEXT,
                new BigDecimal("0.6000"),
                "{\"fallback\": true, \"note\": \"Item agrupado do catálogo para revisão manual\"}"
        );
    }
}
