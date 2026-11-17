package br.com.sellerintelligence.sync.service;

import br.com.sellerintelligence.audit.domain.AuditResult;
import br.com.sellerintelligence.audit.domain.AuditService;
import br.com.sellerintelligence.connector.crypto.TokenCryptoService;
import br.com.sellerintelligence.connector.model.*;
import br.com.sellerintelligence.connector.ports.MarketplaceConnector;
import br.com.sellerintelligence.connector.registry.MarketplaceConnectorRegistry;
import br.com.sellerintelligence.infrastructure.error.BusinessException;
import br.com.sellerintelligence.tenancy.domain.TenantContext;
import br.com.sellerintelligence.sync.domain.*;
import br.com.sellerintelligence.sync.infrastructure.rabbitmq.SyncJobMessage;
import br.com.sellerintelligence.sync.infrastructure.rabbitmq.SyncJobMessagePublisher;
import br.com.sellerintelligence.tenancy.store.Store;
import br.com.sellerintelligence.tenancy.store.StoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class SyncJobService {

    private static final Logger log = LoggerFactory.getLogger(SyncJobService.class);

    private final SyncJobRepository syncJobRepository;
    private final StoreRepository storeRepository;
    private final MarketplaceCredentialRepository credentialRepository;
    private final TokenCryptoService tokenCryptoService;
    private final MarketplaceConnectorRegistry connectorRegistry;
    private final ProductRepository productRepository;
    private final ListingRepository listingRepository;
    private final OrderRepository orderRepository;
    private final AuditService auditService;
    private final ObjectProvider<SyncJobMessagePublisher> publisherProvider;

    public SyncJobService(
            SyncJobRepository syncJobRepository,
            StoreRepository storeRepository,
            MarketplaceCredentialRepository credentialRepository,
            TokenCryptoService tokenCryptoService,
            MarketplaceConnectorRegistry connectorRegistry,
            ProductRepository productRepository,
            ListingRepository listingRepository,
            OrderRepository orderRepository,
            AuditService auditService,
            ObjectProvider<SyncJobMessagePublisher> publisherProvider) {
        this.syncJobRepository = syncJobRepository;
        this.storeRepository = storeRepository;
        this.credentialRepository = credentialRepository;
        this.tokenCryptoService = tokenCryptoService;
        this.connectorRegistry = connectorRegistry;
        this.productRepository = productRepository;
        this.listingRepository = listingRepository;
        this.orderRepository = orderRepository;
        this.auditService = auditService;
        this.publisherProvider = publisherProvider;
    }

    @Transactional
    public SyncJob triggerSync(UUID storeId, SyncResourceType resourceType, SyncTriggerType triggerType) {
        UUID tenantId = TenantContext.getTenantId();
        Store store = storeRepository.findByIdAndTenantId(storeId, tenantId)
                .orElseThrow(() -> {
                    auditService.recordEvent(
                            tenantId,
                            TenantContext.getCurrentUserId().map(UUID::toString).orElse("SYSTEM"),
                            "ACCESS_DENIED",
                            "Store",
                            storeId.toString(),
                            "Attempt to trigger sync for non-existent store or another tenant",
                            AuditResult.DENIED
                    );
                    return new BusinessException("STORE_NOT_FOUND", "Store not found or access denied", HttpStatus.NOT_FOUND);
                });

        MarketplaceCredential credential = credentialRepository.findByTenantIdAndStoreId(tenantId, storeId)
                .orElseThrow(() -> new BusinessException("STORE_NOT_CONNECTED", "Store is not connected to marketplace. Complete OAuth authorization first.", HttpStatus.BAD_REQUEST));

        return triggerSyncForStore(store, resourceType, triggerType);
    }

    @Transactional
    public SyncJob triggerSyncForStore(Store store, SyncResourceType resourceType, SyncTriggerType triggerType) {
        UUID tenantId = store.getTenantId();
        MarketplaceCredential credential = credentialRepository.findByTenantIdAndStoreId(tenantId, store.getId())
                .orElse(null);

        if (credential == null) {
            log.warn("Store {} is not connected (no credentials), skipping sync trigger.", store.getId());
            return null;
        }

        SyncJob job = new SyncJob(tenantId, store.getId(), store.getMarketplace(), resourceType, triggerType);
        job = syncJobRepository.save(job);

        log.info("Created sync job: id={}, tenant={}, store={}, resource={}",
                job.getId(), tenantId, store.getId(), resourceType);

        SyncJobMessagePublisher publisher = publisherProvider.getIfAvailable();
        if (publisher != null) {
            publisher.publishSyncJob(new SyncJobMessage(job.getId(), tenantId, 1));
        } else {
            // Synchronous fallback if no publisher bean is configured
            processJob(job.getId());
        }

        return job;
    }

    @Transactional
    public void processJob(UUID jobId) {
        SyncJob job = syncJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException("SYNC_JOB_NOT_FOUND", "Sync job not found: " + jobId, HttpStatus.NOT_FOUND));

        if (job.getStatus() == SyncStatus.COMPLETED) {
            log.info("Sync job {} already completed, skipping.", jobId);
            return;
        }

        job.start();
        job = syncJobRepository.save(job);
        final UUID storeId = job.getStoreId();

        try {
            MarketplaceCredential credential = credentialRepository.findByTenantIdAndStoreId(job.getTenantId(), storeId)
                    .orElseThrow(() -> new IllegalStateException("Credentials missing for store: " + storeId));

            String accessToken = tokenCryptoService.decrypt(credential.getEncryptedAccessToken());
            MarketplaceConnector connector = connectorRegistry.getConnector(job.getMarketplace());

            if (job.getResourceType() == SyncResourceType.CATALOG || job.getResourceType() == SyncResourceType.LISTINGS) {
                processCatalogSync(job, connector, accessToken, credential.getExternalAccountId());
            } else if (job.getResourceType() == SyncResourceType.ORDERS) {
                processOrdersSync(job, connector, accessToken, credential.getExternalAccountId());
            }

            job.complete(job.getCursor());
            syncJobRepository.save(job);
            log.info("Sync job {} completed successfully with {} items processed", jobId, job.getItemsProcessed());
        } catch (Exception e) {
            log.error("Sync job {} failed with error", jobId, e);
            job.fail(e.getMessage());
            syncJobRepository.save(job);
        }
    }

    private void processCatalogSync(SyncJob job, MarketplaceConnector connector, String accessToken, String externalAccountId) {
        ConnectorSyncBatch<ConnectorListing> batch = connector.fetchListings(accessToken, externalAccountId, job.getCursor(), 50);

        for (ConnectorListing item : batch.items()) {
            UUID productId = null;
            if (item.sku() != null && !item.sku().isBlank()) {
                Product product = productRepository.findByTenantIdAndStoreIdAndSku(job.getTenantId(), job.getStoreId(), item.sku())
                        .orElse(null);

                if (product == null) {
                    product = new Product(
                            job.getTenantId(), job.getStoreId(), item.sku(), item.title(),
                            null, null, null, null, DataProvenance.IMPORTED
                    );
                } else {
                    product.setTitle(item.title());
                }
                product = productRepository.save(product);
                productId = product.getId();
            }

            Listing listing = listingRepository.findByTenantIdAndMarketplaceAndExternalListingId(
                    job.getTenantId(), job.getMarketplace(), item.externalId()
            ).orElse(null);

            if (listing == null) {
                listing = new Listing(
                        job.getTenantId(), job.getStoreId(), productId, job.getMarketplace(),
                        item.externalId(), item.sku(), item.title(), item.status(),
                        item.price(), item.currency() != null ? item.currency() : "BRL",
                        item.availableQuantity(), item.soldQuantity(), item.listingType(),
                        item.permalink(), item.thumbnailUrl(), DataProvenance.IMPORTED,
                        item.rawPayload(), item.originUpdatedAt()
                );
                listingRepository.save(listing);
                job.recordSuccess(1);
            } else {
                // Out-of-order protection: update only if new timestamp is not older than existing
                if (item.originUpdatedAt() == null || listing.getOriginUpdatedAt() == null
                        || !item.originUpdatedAt().isBefore(listing.getOriginUpdatedAt())) {
                    listing.update(item.title(), item.status(), item.price(), item.availableQuantity(),
                            item.soldQuantity(), item.listingType(), item.permalink(), item.thumbnailUrl(),
                            item.rawPayload(), item.originUpdatedAt());
                    if (productId != null) {
                        listing.setProductId(productId);
                    }
                    listingRepository.save(listing);
                }
                job.recordSuccess(1);
            }
        }
        job.setCursor(batch.nextCursor());
    }

    private void processOrdersSync(SyncJob job, MarketplaceConnector connector, String accessToken, String externalAccountId) {
        ConnectorSyncBatch<ConnectorOrder> batch = connector.fetchOrders(accessToken, externalAccountId, null, job.getCursor(), 50);

        for (ConnectorOrder item : batch.items()) {
            Order order = orderRepository.findByTenantIdAndMarketplaceAndExternalOrderId(
                    job.getTenantId(), job.getMarketplace(), item.externalOrderId()
            ).orElse(null);

            if (order == null) {
                order = new Order(
                        job.getTenantId(), job.getStoreId(), job.getMarketplace(),
                        item.externalOrderId(), item.status(), item.totalAmount(),
                        item.currency() != null ? item.currency() : "BRL",
                        item.orderDate(), item.buyerNickname(), item.shippingId(),
                        item.shippingCostSeller(), item.marketplaceFee(),
                        DataProvenance.IMPORTED, item.rawPayload(), item.originUpdatedAt()
                );
                if (item.items() != null) {
                    for (ConnectorOrderItem ci : item.items()) {
                        OrderItem orderItem = new OrderItem(
                                job.getTenantId(), order, ci.externalItemId(), ci.sku(),
                                ci.title(), ci.unitPrice(), ci.quantity(), ci.saleFee()
                        );
                        order.addItem(orderItem);
                    }
                }
                orderRepository.save(order);
                job.recordSuccess(1);
            } else {
                // Out of order protection
                if (item.originUpdatedAt() == null || order.getOriginUpdatedAt() == null
                        || !item.originUpdatedAt().isBefore(order.getOriginUpdatedAt())) {
                    order.update(item.status(), item.totalAmount(), item.shippingCostSeller(), item.marketplaceFee(), item.rawPayload(), item.originUpdatedAt());
                    orderRepository.save(order);
                }
                job.recordSuccess(1);
            }
        }
        job.setCursor(batch.nextCursor());
    }

    @Transactional(readOnly = true)
    public List<SyncJob> listJobs(UUID tenantId) {
        return syncJobRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public SyncJob getJob(UUID jobId, UUID tenantId) {
        return syncJobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new BusinessException("SYNC_JOB_NOT_FOUND", "Sync job not found or access denied", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public SyncJob retryJob(UUID jobId, UUID tenantId) {
        SyncJob job = syncJobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new BusinessException("SYNC_JOB_NOT_FOUND", "Sync job not found or access denied", HttpStatus.NOT_FOUND));

        if (job.getStatus() != SyncStatus.FAILED) {
            throw new BusinessException("INVALID_JOB_STATE", "Only failed jobs can be retried", HttpStatus.BAD_REQUEST);
        }

        SyncJob newJob = new SyncJob(job.getTenantId(), job.getStoreId(), job.getMarketplace(), job.getResourceType(), job.getTriggerType());
        newJob = syncJobRepository.save(newJob);

        SyncJobMessagePublisher publisher = publisherProvider.getIfAvailable();
        if (publisher != null) {
            publisher.publishSyncJob(new SyncJobMessage(newJob.getId(), tenantId, 1));
        } else {
            processJob(newJob.getId());
        }

        return newJob;
    }
}
