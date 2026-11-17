package br.com.sellerintelligence.discovery.service;

import br.com.sellerintelligence.discovery.domain.*;
import br.com.sellerintelligence.discovery.security.EanChecksumValidator;
import br.com.sellerintelligence.discovery.security.SecureFileUploadValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
public class CatalogReviewService {

    private static final Logger log = LoggerFactory.getLogger(CatalogReviewService.class);

    private final DiscoveryCatalogRepository catalogRepository;
    private final DiscoveryCatalogItemRepository itemRepository;
    private final SecureFileUploadValidator fileUploadValidator;
    private final EanChecksumValidator eanValidator;
    private final PdfCatalogExtractionService extractionService;

    public CatalogReviewService(DiscoveryCatalogRepository catalogRepository,
                                DiscoveryCatalogItemRepository itemRepository,
                                SecureFileUploadValidator fileUploadValidator,
                                EanChecksumValidator eanValidator,
                                PdfCatalogExtractionService extractionService) {
        this.catalogRepository = catalogRepository;
        this.itemRepository = itemRepository;
        this.fileUploadValidator = fileUploadValidator;
        this.eanValidator = eanValidator;
        this.extractionService = extractionService;
    }

    @Transactional
    public DiscoveryCatalog uploadAndProcessCatalog(UUID tenantId, String originalFilename,
                                                   byte[] fileBytes, String supplierName) {
        Objects.requireNonNull(tenantId, "tenantId cannot be null");
        Objects.requireNonNull(fileBytes, "fileBytes cannot be null");

        // 1. Validação de tamanho (máximo 25 MB)
        fileUploadValidator.validateSize(fileBytes.length);

        // 2. Validação de Magic Bytes
        byte[] header = Arrays.copyOf(fileBytes, Math.min(fileBytes.length, 16));
        String mimeType = fileUploadValidator.detectAndValidateMimeType(header);

        // 3. Sanitização do nome do arquivo
        String safeOriginalName = fileUploadValidator.sanitizeFilename(originalFilename);
        String storedFilename = UUID.randomUUID() + ".pdf";

        DiscoveryCatalog catalog = new DiscoveryCatalog(
                tenantId,
                safeOriginalName,
                storedFilename,
                mimeType,
                fileBytes.length,
                supplierName
        );
        DiscoveryCatalog savedCatalog = catalogRepository.save(catalog);
        log.info("Uploaded catalog: id={}, filename='{}', tenantId={}", savedCatalog.getId(), safeOriginalName, tenantId);

        // 4. Extração assíncrona/estruturada de texto e itens
        extractionService.extractItemsFromPdf(savedCatalog, fileBytes);

        return catalogRepository.save(savedCatalog);
    }

    @Transactional
    public DiscoveryCatalogItem reviewAndConfirmItem(UUID tenantId, UUID itemId, String name,
                                                    String supplierSku, String ean, String description,
                                                    BigDecimal costPrice, BigDecimal weightKg,
                                                    String dimensionsCm) {
        DiscoveryCatalogItem item = getItem(tenantId, itemId);

        // Se EAN for informado, valida checksum
        if (ean != null && !ean.isBlank() && !eanValidator.isValidEan(ean)) {
            throw new IllegalArgumentException("Dígito verificador do EAN informado (" + ean + ") é inválido.");
        }

        item.updateAndConfirm(name, supplierSku, ean, description, costPrice, weightKg, dimensionsCm);
        DiscoveryCatalogItem saved = itemRepository.save(item);

        catalogRepository.findByIdAndTenantId(item.getCatalogId(), tenantId)
                .ifPresent(DiscoveryCatalog::incrementReviewedItems);

        log.info("Item reviewed and confirmed: itemId={}, name='{}', tenantId={}", itemId, name, tenantId);
        return saved;
    }

    @Transactional
    public DiscoveryCatalogItem rejectItem(UUID tenantId, UUID itemId) {
        DiscoveryCatalogItem item = getItem(tenantId, itemId);
        item.reject();
        log.info("Item rejected: itemId={}, tenantId={}", itemId, tenantId);
        return itemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<DiscoveryCatalog> listCatalogs(UUID tenantId) {
        return catalogRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public List<DiscoveryCatalogItem> listCatalogItems(UUID tenantId, UUID catalogId) {
        return itemRepository.findByCatalogIdAndTenantId(catalogId, tenantId);
    }

    @Transactional(readOnly = true)
    public DiscoveryCatalogItem getItem(UUID tenantId, UUID itemId) {
        return itemRepository.findByIdAndTenantId(itemId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Item de catálogo não encontrado: " + itemId));
    }

    @Transactional
    public void deleteCatalog(UUID tenantId, UUID catalogId) {
        DiscoveryCatalog catalog = catalogRepository.findByIdAndTenantId(catalogId, tenantId)
                .orElseThrow(() -> new NoSuchElementException("Catálogo não encontrado para exclusão: " + catalogId));
        List<DiscoveryCatalogItem> items = itemRepository.findByCatalogIdAndTenantId(catalogId, tenantId);
        itemRepository.deleteAll(items);
        catalogRepository.delete(catalog);
        log.info("Securely deleted catalog and its items: id={}, tenantId={}", catalogId, tenantId);
    }
}
