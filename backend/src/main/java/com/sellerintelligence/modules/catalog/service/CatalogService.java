package com.sellerintelligence.modules.catalog.service;

import com.sellerintelligence.modules.catalog.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço de gestão de catálogo canônico e consulta de anúncios próprios escopados por tenant.
 */
@Service
public class CatalogService {

    private final SellerAccountRepository sellerAccountRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final MarketplaceListingRepository listingRepository;
    private final ExternalIdentifierMappingRepository externalMappingRepository;
    private final ListingSyncCheckpointRepository checkpointRepository;

    public CatalogService(
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

    @Transactional
    public SellerAccount registerSellerAccount(
            UUID tenantId,
            String marketplace,
            String externalAccountId,
            String accountName
    ) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(marketplace, "marketplace não pode ser nulo");
        Objects.requireNonNull(externalAccountId, "externalAccountId não pode ser nulo");
        Objects.requireNonNull(accountName, "accountName não pode ser nulo");

        return sellerAccountRepository
                .findByTenantIdAndMarketplaceAndExternalAccountId(tenantId, marketplace, externalAccountId)
                .orElseGet(() -> sellerAccountRepository.save(new SellerAccount(
                        UUID.randomUUID(),
                        tenantId,
                        marketplace,
                        externalAccountId,
                        accountName,
                        "ACTIVE"
                )));
    }

    @Transactional
    public Product createProduct(
            UUID tenantId,
            String title,
            String canonicalSku,
            String brand,
            String category
    ) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(title, "title não pode ser nulo");

        Product product = new Product(
                UUID.randomUUID(),
                tenantId,
                title,
                canonicalSku != null && !canonicalSku.isBlank() ? canonicalSku.trim() : null,
                brand,
                category,
                "ACTIVE"
        );
        return productRepository.save(product);
    }

    @Transactional
    public ProductVariant createVariant(
            UUID tenantId,
            UUID productId,
            String variantSku,
            String gtinBarcode,
            String attributesJson
    ) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(productId, "productId não pode ser nulo");

        // Valida que o produto pertence ao tenant informado
        productRepository.findByIdAndTenantId(productId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Produto pai não encontrado para o tenant informado"));

        ProductVariant variant = new ProductVariant(
                UUID.randomUUID(),
                tenantId,
                productId,
                variantSku != null && !variantSku.isBlank() ? variantSku.trim() : null,
                gtinBarcode != null && !gtinBarcode.isBlank() ? gtinBarcode.trim() : null,
                attributesJson,
                "ACTIVE"
        );
        return productVariantRepository.save(variant);
    }

    @Transactional(readOnly = true)
    public List<Product> listProducts(UUID tenantId) {
        return productRepository.findAllByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public Optional<Product> getProduct(UUID tenantId, UUID productId) {
        return productRepository.findByIdAndTenantId(productId, tenantId);
    }

    @Transactional(readOnly = true)
    public List<MarketplaceListing> listListings(UUID tenantId) {
        return listingRepository.findAllByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public List<MarketplaceListing> listListingsByProduct(UUID tenantId, UUID productId) {
        return listingRepository.findAllByTenantIdAndProductId(tenantId, productId);
    }

    @Transactional(readOnly = true)
    public List<MarketplaceListing> listListingsByAccount(UUID tenantId, UUID sellerAccountId) {
        return listingRepository.findAllByTenantIdAndSellerAccountId(tenantId, sellerAccountId);
    }

    @Transactional(readOnly = true)
    public Optional<ListingSyncCheckpoint> getCheckpoint(UUID tenantId, UUID sellerAccountId, String marketplace) {
        return checkpointRepository.findByTenantIdAndSellerAccountIdAndMarketplace(tenantId, sellerAccountId, marketplace);
    }

    @Transactional(readOnly = true)
    public Page<MarketplaceListing> searchListings(
            UUID tenantId,
            String marketplace,
            UUID sellerAccountId,
            String status,
            String search,
            Pageable pageable
    ) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        return listingRepository.searchListings(
                tenantId,
                (marketplace != null && !marketplace.isBlank() && !marketplace.equalsIgnoreCase("ALL")) ? marketplace.trim() : null,
                sellerAccountId,
                (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) ? status.trim() : null,
                (search != null && !search.isBlank()) ? search.trim() : null,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Optional<MarketplaceListing> getListing(UUID tenantId, UUID listingId) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(listingId, "listingId não pode ser nulo");
        return listingRepository.findByIdAndTenantId(listingId, tenantId);
    }

    @Transactional(readOnly = true)
    public List<SellerAccount> listSellerAccounts(UUID tenantId) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        return sellerAccountRepository.findAllByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public Optional<SellerAccount> getSellerAccount(UUID tenantId, UUID sellerAccountId) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(sellerAccountId, "sellerAccountId não pode ser nulo");
        return sellerAccountRepository.findByIdAndTenantId(sellerAccountId, tenantId);
    }

    @Transactional(readOnly = true)
    public Optional<ProductVariant> getVariant(UUID tenantId, UUID variantId) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(variantId, "variantId não pode ser nulo");
        return productVariantRepository.findByIdAndTenantId(variantId, tenantId);
    }
}
