package br.com.sellerintelligence.discovery.service;

import br.com.sellerintelligence.discovery.domain.*;
import br.com.sellerintelligence.discovery.security.EanChecksumValidator;
import br.com.sellerintelligence.discovery.security.SecureFileUploadValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogReviewServiceTest {

    @Mock
    private DiscoveryCatalogRepository catalogRepository;

    @Mock
    private DiscoveryCatalogItemRepository itemRepository;

    @Mock
    private SecureFileUploadValidator fileUploadValidator;

    @Mock
    private EanChecksumValidator eanValidator;

    @Mock
    private PdfCatalogExtractionService extractionService;

    private CatalogReviewService reviewService;
    private final UUID tenantId = UUID.randomUUID();
    private final UUID catalogId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        reviewService = new CatalogReviewService(
                catalogRepository, itemRepository, fileUploadValidator, eanValidator, extractionService
        );
    }

    @Test
    @DisplayName("Should review, update and confirm catalog item to REVIEWED status")
    void shouldReviewAndConfirmItem() {
        DiscoveryCatalog catalog = new DiscoveryCatalog(
                tenantId, "catalogo.pdf", "stored.pdf", "application/pdf", 1024, "Fornecedor XYZ"
        );
        DiscoveryCatalogItem item = new DiscoveryCatalogItem(
                catalog.getId(), tenantId, "Mouse Gamer", "M-10", "7891234567895",
                "Desc", new BigDecimal("49.90"), null, null,
                ExtractionMethod.PDF_TEXT, new BigDecimal("0.9000"), "{}"
        );

        when(itemRepository.findByIdAndTenantId(item.getId(), tenantId)).thenReturn(Optional.of(item));
        when(eanValidator.isValidEan("7891234567895")).thenReturn(true);
        when(itemRepository.save(any(DiscoveryCatalogItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(catalogRepository.findByIdAndTenantId(catalog.getId(), tenantId)).thenReturn(Optional.of(catalog));

        DiscoveryCatalogItem reviewed = reviewService.reviewAndConfirmItem(
                tenantId, item.getId(), "Mouse Gamer RGB Pro 16000 DPI",
                "M-10-PRO", "7891234567895", "Descrição revisada",
                new BigDecimal("45.00"), new BigDecimal("0.150"), "12x6x4 cm"
        );

        assertThat(reviewed.getReviewStatus()).isEqualTo(ItemReviewStatus.REVIEWED);
        assertThat(reviewed.getName()).isEqualTo("Mouse Gamer RGB Pro 16000 DPI");
        assertThat(reviewed.getCostPrice()).isEqualByComparingTo("45.00");
        assertThat(reviewed.getWeightKg()).isEqualByComparingTo("0.150");

        verify(itemRepository, times(1)).save(item);
        assertThat(catalog.getReviewedItems()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should reject item during review and update status to REJECTED")
    void shouldRejectItem() {
        DiscoveryCatalogItem item = new DiscoveryCatalogItem(
                catalogId, tenantId, "Item Indesejado", "BAD-1", null,
                "Desc", null, null, null,
                ExtractionMethod.PDF_TEXT, new BigDecimal("0.5000"), "{}"
        );

        when(itemRepository.findByIdAndTenantId(item.getId(), tenantId)).thenReturn(Optional.of(item));
        when(itemRepository.save(any(DiscoveryCatalogItem.class))).thenAnswer(inv -> inv.getArgument(0));

        DiscoveryCatalogItem rejected = reviewService.rejectItem(tenantId, item.getId());

        assertThat(rejected.getReviewStatus()).isEqualTo(ItemReviewStatus.REJECTED);
        verify(itemRepository, times(1)).save(item);
    }

    @Test
    @DisplayName("Should block confirmation if EAN check digit is mathematically invalid")
    void shouldBlockConfirmationOnInvalidEan() {
        DiscoveryCatalogItem item = new DiscoveryCatalogItem(
                catalogId, tenantId, "Item Teste", "SKU-1", null,
                "Desc", new BigDecimal("10.00"), null, null,
                ExtractionMethod.PDF_TEXT, new BigDecimal("0.8000"), "{}"
        );

        when(itemRepository.findByIdAndTenantId(item.getId(), tenantId)).thenReturn(Optional.of(item));
        when(eanValidator.isValidEan("7891234567890")).thenReturn(false);

        assertThatThrownBy(() -> reviewService.reviewAndConfirmItem(
                tenantId, item.getId(), "Item", "SKU-1", "7891234567890",
                "Desc", new BigDecimal("10.00"), null, null
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Dígito verificador do EAN informado");

        verify(itemRepository, never()).save(any(DiscoveryCatalogItem.class));
    }
}
