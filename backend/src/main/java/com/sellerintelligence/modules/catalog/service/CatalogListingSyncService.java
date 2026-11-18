package com.sellerintelligence.modules.catalog.service;

import com.sellerintelligence.modules.catalog.*;
import com.sellerintelligence.modules.catalog.model.ListingPageRequest;
import com.sellerintelligence.modules.catalog.model.ListingPageResponse;
import com.sellerintelligence.modules.catalog.model.ListingSyncResult;
import com.sellerintelligence.modules.catalog.model.RawListingItem;
import com.sellerintelligence.modules.catalog.operations.ReadOwnCatalogOperation;
import com.sellerintelligence.modules.marketplaceadapters.api.MarketplaceAdapter;
import com.sellerintelligence.modules.marketplaceadapters.model.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * Motor central de sincronização de anúncios próprios com os canais homologados.
 * Garante idempotência, separação rigorosa entre produto canônico e listings,
 * paginação resiliente com checkpoint retomável e auditoria de origem.
 */
@Service
public class CatalogListingSyncService {

    private static final Logger log = LoggerFactory.getLogger(CatalogListingSyncService.class);

    private final SellerAccountRepository sellerAccountRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final MarketplaceListingRepository listingRepository;
    private final ExternalIdentifierMappingRepository externalMappingRepository;
    private final ListingSyncCheckpointRepository checkpointRepository;

    public CatalogListingSyncService(
            SellerAccountRepository sellerAccountRepository,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            MarketplaceListingRepository listingRepository,
            ExternalIdentifierMappingRepository externalMappingRepository,
            ListingSyncCheckpointRepository checkpointRepository
    ) {
        this.sellerAccountRepository = sellerAccountRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.listingRepository = listingRepository;
        this.externalMappingRepository = externalMappingRepository;
        this.checkpointRepository = checkpointRepository;
    }

    /**
     * Sincroniza anúncios próprios a partir de um adaptador oficial de marketplace.
     * Suporta paginação, checkpoint e retomada em caso de falha transitória.
     *
     * @param tenantId ID do tenant autenticado
     * @param sellerAccountId ID da conta do seller
     * @param pageSize tamanho da página a ser requisitada
     * @param adapter adaptador homologado do marketplace
     * @param pageFetcher função que realiza a consulta da página no canal
     * @return resultado consolidado da sincronização
     */
    @Transactional
    public ListingSyncResult syncOwnListings(
            UUID tenantId,
            UUID sellerAccountId,
            int pageSize,
            MarketplaceAdapter adapter,
            BiFunction<ListingPageRequest, ExecutionContext, ListingPageResponse> pageFetcher
    ) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(sellerAccountId, "sellerAccountId não pode ser nulo");
        Objects.requireNonNull(adapter, "adapter não pode ser nulo");
        Objects.requireNonNull(pageFetcher, "pageFetcher não pode ser nulo");

        SellerAccount sellerAccount = sellerAccountRepository.findByIdAndTenantId(sellerAccountId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Conta de vendedor não encontrada para o tenant informado"));

        String marketplace = sellerAccount.getMarketplace();
        ListingSyncCheckpoint checkpoint = getOrCreateCheckpoint(tenantId, sellerAccountId, marketplace, pageSize);

        int offset = determineStartOffset(checkpoint);
        checkpoint.setSyncStatus("IN_PROGRESS");
        checkpoint.setLastOffset(offset);
        checkpoint.setErrorMessage(null);
        checkpointRepository.save(checkpoint);

        int createdCount = 0;
        int updatedCount = 0;
        int syncedCount = 0;

        try {
            boolean hasMore = true;

            while (hasMore) {
                ListingPageRequest pageRequest = new ListingPageRequest(offset, pageSize);
                ExecutionContext context = new ExecutionContext(
                        tenantId,
                        sellerAccount.getExternalAccountId(),
                        null,
                        UUID.randomUUID().toString()
                );

                ReadOwnCatalogOperation operation = new ReadOwnCatalogOperation(pageFetcher);
                ListingPageResponse pageResponse = adapter.execute(operation, pageRequest, context);

                if (pageResponse == null || pageResponse.items().isEmpty()) {
                    break;
                }

                for (RawListingItem rawItem : pageResponse.items()) {
                    UpsertResult upsertResult = processListingItem(tenantId, sellerAccount, rawItem);
                    if (upsertResult.created()) {
                        createdCount++;
                    } else {
                        updatedCount++;
                    }
                    syncedCount++;
                }

                offset += pageResponse.items().size();
                checkpoint.setLastOffset(offset);
                checkpoint.setTotalItemsSynced(checkpoint.getTotalItemsSynced() + pageResponse.items().size());
                checkpoint.setLastCheckpointAt(Instant.now());
                checkpointRepository.save(checkpoint);

                hasMore = pageResponse.hasMore() && pageResponse.items().size() >= pageSize;
            }

            checkpoint.setSyncStatus("COMPLETED");
            checkpoint.setCompletedAt(Instant.now());
            checkpointRepository.save(checkpoint);

            log.info("Sincronização de catálogo concluída com sucesso: tenant={} account={} marketplace={} total={} created={} updated={}",
                    tenantId, sellerAccountId, marketplace, syncedCount, createdCount, updatedCount);

            return new ListingSyncResult(
                    sellerAccountId,
                    marketplace,
                    syncedCount,
                    createdCount,
                    updatedCount,
                    "COMPLETED",
                    offset,
                    null
            );

        } catch (Exception e) {
            checkpoint.setSyncStatus("FAILED");
            checkpoint.setErrorMessage(e.getMessage());
            checkpoint.setLastCheckpointAt(Instant.now());
            checkpointRepository.save(checkpoint);

            log.warn("Falha durante sincronização de catálogo (retomável a partir do offset {}): tenant={} error={}",
                    offset, tenantId, e.getMessage());

            ListingSyncResult partialResult = new ListingSyncResult(
                    sellerAccountId,
                    marketplace,
                    syncedCount,
                    createdCount,
                    updatedCount,
                    "FAILED",
                    offset,
                    e.getMessage()
            );

            throw new CatalogSyncException("Falha parcial durante sincronização de catálogo no offset " + offset, partialResult, e);
        }
    }

    private ListingSyncCheckpoint getOrCreateCheckpoint(
            UUID tenantId,
            UUID sellerAccountId,
            String marketplace,
            int pageSize
    ) {
        return checkpointRepository
                .findByTenantIdAndSellerAccountIdAndMarketplace(tenantId, sellerAccountId, marketplace)
                .orElseGet(() -> checkpointRepository.save(new ListingSyncCheckpoint(
                        UUID.randomUUID(),
                        tenantId,
                        sellerAccountId,
                        marketplace,
                        pageSize
                )));
    }

    private int determineStartOffset(ListingSyncCheckpoint checkpoint) {
        if ("COMPLETED".equals(checkpoint.getSyncStatus())) {
            return 0; // Reinicia sincronização limpa se a anterior foi finalizada
        }
        return checkpoint.getLastOffset() != null ? checkpoint.getLastOffset() : 0;
    }

    private UpsertResult processListingItem(UUID tenantId, SellerAccount sellerAccount, RawListingItem rawItem) {
        Optional<MarketplaceListing> existingOpt = listingRepository
                .findByTenantIdAndMarketplaceAndExternalListingId(tenantId, sellerAccount.getMarketplace(), rawItem.externalListingId());

        if (existingOpt.isPresent()) {
            MarketplaceListing listing = existingOpt.get();
            listing.setTitle(rawItem.title());
            listing.setPrice(rawItem.price());
            listing.setAvailableQuantity(rawItem.availableQuantity());
            listing.setStatus(rawItem.status());
            listing.setPermalink(rawItem.permalink());
            listing.setRawPayloadJson(rawItem.rawJson());
            listing.setSchemaVersion(rawItem.schemaVersion());
            listing.setOrigin(rawItem.origin());
            listing.setExternalSku(rawItem.externalSku());
            listing.setSyncedAt(Instant.now());
            listingRepository.save(listing);
            return new UpsertResult(false, listing);
        }

        // Resolução do vínculo com Produto Canônico e Variante:
        // A deduplicação NUNCA ocorre somente por título!
        UUID canonicalProductId = resolveCanonicalProductId(tenantId, sellerAccount.getMarketplace(), rawItem);
        UUID canonicalVariantId = resolveCanonicalVariantId(tenantId, rawItem);

        MarketplaceListing newListing = new MarketplaceListing(
                UUID.randomUUID(),
                tenantId,
                sellerAccount.getId(),
                canonicalProductId,
                canonicalVariantId,
                sellerAccount.getMarketplace(),
                rawItem.externalListingId(),
                rawItem.externalSku(),
                null,
                rawItem.title(),
                rawItem.price(),
                "BRL",
                rawItem.availableQuantity(),
                rawItem.status(),
                rawItem.permalink(),
                rawItem.rawJson(),
                rawItem.schemaVersion(),
                rawItem.origin()
        );

        listingRepository.save(newListing);
        return new UpsertResult(true, newListing);
    }

    private UUID resolveCanonicalProductId(UUID tenantId, String marketplace, RawListingItem rawItem) {
        // 1. Verifica se há mapeamento explícito de identificador externo
        Optional<ExternalIdentifierMapping> mappingOpt = externalMappingRepository
                .findByTenantIdAndMarketplaceAndExternalIdTypeAndExternalIdValue(
                        tenantId,
                        marketplace,
                        "ITEM_ID",
                        rawItem.externalListingId()
                );
        if (mappingOpt.isPresent() && "PRODUCT".equals(mappingOpt.get().getInternalEntityType())) {
            return mappingOpt.get().getInternalEntityId();
        }

        // 2. Se houver SKU externo, tenta vincular por correspondência de SKU canônico
        if (rawItem.externalSku() != null && !rawItem.externalSku().isBlank()) {
            Optional<Product> productBySku = productRepository
                    .findByTenantIdAndCanonicalSku(tenantId, rawItem.externalSku().trim());
            if (productBySku.isPresent()) {
                return productBySku.get().getId();
            }

            // Verifica se pertence a uma variante
            Optional<ProductVariant> variantBySku = productVariantRepository
                    .findByTenantIdAndVariantSku(tenantId, rawItem.externalSku().trim());
            if (variantBySku.isPresent()) {
                return variantBySku.get().getProductId();
            }
        }

        // 3. Se houver GTIN, tenta vincular por código de barras de variante
        if (rawItem.gtin() != null && !rawItem.gtin().isBlank()) {
            Optional<ProductVariant> variantByGtin = productVariantRepository
                    .findByTenantIdAndGtinBarcode(tenantId, rawItem.gtin().trim());
            if (variantByGtin.isPresent()) {
                return variantByGtin.get().getProductId();
            }
        }

        // 4. Se não encontrar correspondência explícita, cria um produto canônico dedicado
        // sem fundir cegamente com outros produtos que compartilhem o mesmo título.
        Product newProduct = new Product(
                UUID.randomUUID(),
                tenantId,
                rawItem.title(),
                rawItem.externalSku() != null && !rawItem.externalSku().isBlank() ? rawItem.externalSku().trim() : null,
                null,
                null,
                "ACTIVE"
        );
        productRepository.save(newProduct);

        // Registra o mapeamento do ITEM_ID para o produto recém-criado
        ExternalIdentifierMapping newMapping = new ExternalIdentifierMapping(
                UUID.randomUUID(),
                tenantId,
                marketplace,
                "ITEM_ID",
                rawItem.externalListingId(),
                "PRODUCT",
                newProduct.getId()
        );
        externalMappingRepository.save(newMapping);

        return newProduct.getId();
    }

    private UUID resolveCanonicalVariantId(UUID tenantId, RawListingItem rawItem) {
        if (rawItem.externalSku() != null && !rawItem.externalSku().isBlank()) {
            Optional<ProductVariant> variantOpt = productVariantRepository
                    .findByTenantIdAndVariantSku(tenantId, rawItem.externalSku().trim());
            if (variantOpt.isPresent()) {
                return variantOpt.get().getId();
            }
        }

        if (rawItem.gtin() != null && !rawItem.gtin().isBlank()) {
            Optional<ProductVariant> variantOpt = productVariantRepository
                    .findByTenantIdAndGtinBarcode(tenantId, rawItem.gtin().trim());
            if (variantOpt.isPresent()) {
                return variantOpt.get().getId();
            }
        }

        return null;
    }

    private record UpsertResult(boolean created, MarketplaceListing listing) {}
}
