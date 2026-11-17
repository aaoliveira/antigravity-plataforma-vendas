package br.com.sellerintelligence.compliance.service;

import br.com.sellerintelligence.compliance.domain.*;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service tracking API lifecycle, version evolution, and deprecation notices across marketplaces.
 */
@Service
public class ApiLifecycleMonitor {

    private static final Logger log = LoggerFactory.getLogger(ApiLifecycleMonitor.class);

    private final MarketplaceApiLifecycleRepository lifecycleRepository;
    private final ComplianceNotificationService notificationService;

    public ApiLifecycleMonitor(
            MarketplaceApiLifecycleRepository lifecycleRepository,
            ComplianceNotificationService notificationService) {
        this.lifecycleRepository = lifecycleRepository;
        this.notificationService = notificationService;
    }

    @PostConstruct
    public void init() {
        seedDefaultApiLifecyclesIfEmpty();
    }

    @Transactional
    public MarketplaceApiLifecycle registerApi(
            Marketplace marketplace,
            String apiName,
            String version,
            ApiLifecycleStatus status,
            OffsetDateTime sunsetDate,
            String migrationGuideUrl) {

        Optional<MarketplaceApiLifecycle> existing = lifecycleRepository.findByMarketplaceAndApiNameAndVersion(marketplace, apiName, version);
        if (existing.isPresent()) {
            MarketplaceApiLifecycle rec = existing.get();
            rec.setStatus(status);
            rec.setSunsetDate(sunsetDate);
            rec.setMigrationGuideUrl(migrationGuideUrl);
            rec.setLastAuditedAt(OffsetDateTime.now());
            return lifecycleRepository.save(rec);
        }

        MarketplaceApiLifecycle lifecycle = new MarketplaceApiLifecycle(
                UUID.randomUUID(),
                marketplace,
                apiName,
                version,
                status,
                sunsetDate,
                migrationGuideUrl
        );

        lifecycle = lifecycleRepository.save(lifecycle);

        if (status == ApiLifecycleStatus.DEPRECATED || status == ApiLifecycleStatus.SUNSET_SCHEDULED) {
            notificationService.sendAlert(
                    marketplace,
                    ComplianceRiskLevel.HIGH,
                    "Marketplace API Deprecated",
                    "API " + apiName + " (" + version + ") on " + marketplace + " is marked as " + status +
                            (sunsetDate != null ? ". Sunset scheduled for " + sunsetDate : ".")
            );
        }

        return lifecycle;
    }

    @Transactional(readOnly = true)
    public List<MarketplaceApiLifecycle> listByMarketplace(Marketplace marketplace) {
        return lifecycleRepository.findByMarketplace(marketplace);
    }

    @Transactional(readOnly = true)
    public List<MarketplaceApiLifecycle> listAll() {
        return lifecycleRepository.findAll();
    }

    @Transactional
    public void seedDefaultApiLifecyclesIfEmpty() {
        if (lifecycleRepository.count() > 0) {
            return;
        }

        log.info("ApiLifecycleMonitor: Seeding default verified marketplace API lifecycles...");

        registerApi(
                Marketplace.MERCADO_LIVRE,
                "Orders API",
                "v2",
                ApiLifecycleStatus.ACTIVE,
                null,
                "https://developers.mercadolivre.com.br/pt_br/gestao-de-vendas"
        );

        registerApi(
                Marketplace.MERCADO_LIVRE,
                "Items & Listings API",
                "v1",
                ApiLifecycleStatus.ACTIVE,
                null,
                "https://developers.mercadolivre.com.br/pt_br/publicacao-de-produtos"
        );

        registerApi(
                Marketplace.SHOPEE,
                "Open Platform API",
                "v2.0",
                ApiLifecycleStatus.ACTIVE,
                null,
                "https://open.shopee.com"
        );

        registerApi(
                Marketplace.AMAZON,
                "SP-API Orders",
                "v0",
                ApiLifecycleStatus.ACTIVE,
                null,
                "https://developer-docs.amazon.com/sp-api/docs/orders-api-v0-reference"
        );

        registerApi(
                Marketplace.AMAZON,
                "SP-API Listings Items",
                "2021-08-01",
                ApiLifecycleStatus.ACTIVE,
                null,
                "https://developer-docs.amazon.com/sp-api/docs/listings-items-api-v2021-08-01-reference"
        );
    }
}
