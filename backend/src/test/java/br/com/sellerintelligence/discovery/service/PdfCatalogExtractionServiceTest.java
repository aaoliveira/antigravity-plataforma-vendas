package br.com.sellerintelligence.discovery.service;

import br.com.sellerintelligence.discovery.domain.*;
import br.com.sellerintelligence.discovery.security.EanChecksumValidator;
import br.com.sellerintelligence.discovery.security.UntrustedTextSanitizer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfCatalogExtractionServiceTest {

    @Mock
    private DiscoveryCatalogItemRepository itemRepository;

    private PdfCatalogExtractionService extractionService;
    private final UUID tenantId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        UntrustedTextSanitizer sanitizer = new UntrustedTextSanitizer();
        EanChecksumValidator eanValidator = new EanChecksumValidator();
        extractionService = new PdfCatalogExtractionService(itemRepository, sanitizer, eanValidator);
    }

    @Test
    @DisplayName("Should extract structured items, prices, SKUs and EANs from valid PDF with text layer")
    void shouldExtractStructuredItemsFromPdf() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText("Teclado Mecanico Gamer RGB SKU: KB-99 Preco: R$ 159,90 EAN: 7891234567895");
                stream.endText();
            }
            doc.save(baos);
        }
        byte[] pdfBytes = baos.toByteArray();

        DiscoveryCatalog catalog = new DiscoveryCatalog(
                tenantId, "catalogo.pdf", "stored-123.pdf", "application/pdf", pdfBytes.length, "Distribuidora Tech"
        );

        when(itemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<DiscoveryCatalogItem> items = extractionService.extractItemsFromPdf(catalog, pdfBytes);

        assertThat(items).isNotEmpty();
        DiscoveryCatalogItem item = items.get(0);
        assertThat(item.getName()).contains("Teclado Mecanico Gamer RGB");
        assertThat(item.getCostPrice()).isEqualByComparingTo("159.90");
        assertThat(item.getSupplierSku()).isEqualTo("KB-99");
        assertThat(item.getEan()).isEqualTo("7891234567895");
        assertThat(item.getReviewStatus()).isEqualTo(ItemReviewStatus.PENDING_REVIEW);
        assertThat(item.getExtractionMethod()).isEqualTo(ExtractionMethod.PDF_TEXT);
        assertThat(item.getOverallConfidence()).isGreaterThan(new BigDecimal("0.7000"));

        assertThat(catalog.getStatus()).isEqualTo(CatalogStatus.PARSED);
        assertThat(catalog.getTotalItems()).isEqualTo(items.size());
    }

    @Test
    @DisplayName("Should flag document for OCR when PDF has no text layer (Scanned document)")
    void shouldFlagForOcrWhenPdfHasNoTextLayer() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(); // empty page with zero text
            doc.addPage(page);
            doc.save(baos);
        }
        byte[] emptyPdfBytes = baos.toByteArray();

        DiscoveryCatalog catalog = new DiscoveryCatalog(
                tenantId, "scanned.pdf", "stored-scan.pdf", "application/pdf", emptyPdfBytes.length, "Fornecedor Scan"
        );

        when(itemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<DiscoveryCatalogItem> items = extractionService.extractItemsFromPdf(catalog, emptyPdfBytes);

        assertThat(items).hasSize(1);
        DiscoveryCatalogItem item = items.get(0);
        assertThat(item.getExtractionMethod()).isEqualTo(ExtractionMethod.OCR);
        assertThat(item.getName()).contains("Requer OCR");
        assertThat(item.getOverallConfidence()).isLessThan(new BigDecimal("0.5000"));
    }
}
