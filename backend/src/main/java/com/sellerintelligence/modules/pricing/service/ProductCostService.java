package com.sellerintelligence.modules.pricing.service;

import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.modules.pricing.entity.ProductCostRecord;
import com.sellerintelligence.modules.pricing.model.ProductCostDto;
import com.sellerintelligence.modules.pricing.model.RegisterProductCostCommand;
import com.sellerintelligence.modules.pricing.repository.ProductCostRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço responsável pelo cadastro, versionamento temporal e recuperação de custos unitários de produtos/variantes/empresas.
 */
@Service
public class ProductCostService {

    private static final Logger log = LoggerFactory.getLogger(ProductCostService.class);

    private final ProductCostRepository productCostRepository;
    private final AuditPublisher auditPublisher;

    public ProductCostService(
            ProductCostRepository productCostRepository,
            AuditPublisher auditPublisher
    ) {
        this.productCostRepository = productCostRepository;
        this.auditPublisher = auditPublisher;
    }

    /**
     * Registra ou atualiza um custo.
     * Caso já exista um custo em aberto (effectiveTo == null) para a mesma tupla (productId, variantId, companyId),
     * encerra a vigência do anterior no instante effectiveFrom e cria uma nova versão preservando o histórico.
     */
    @Transactional
    public ProductCostDto registerCost(UUID tenantId, String userEmail, RegisterProductCostCommand cmd) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(userEmail, "userEmail não pode ser nulo");
        Objects.requireNonNull(cmd, "cmd não pode ser nulo");
        Objects.requireNonNull(cmd.productId(), "productId não pode ser nulo");

        Instant effectiveFrom = cmd.effectiveFrom() != null ? cmd.effectiveFrom() : Instant.now();

        Optional<ProductCostRecord> currentOpenOpt = productCostRepository.findCurrentOpenRecord(
                tenantId,
                cmd.productId(),
                cmd.variantId(),
                cmd.companyId()
        );

        int version = 1;
        String eventType = "PRODUCT_COST_CREATED";

        if (currentOpenOpt.isPresent()) {
            ProductCostRecord previous = currentOpenOpt.get();
            version = previous.getVersion() + 1;
            eventType = "PRODUCT_COST_UPDATED";

            // Encerra vigência anterior garantindo integridade temporal
            previous.closeValidity(effectiveFrom);
            productCostRepository.save(previous);

            log.info("Encerrada vigência do custo anterior id={} para produto={} versão={}",
                    previous.getId(), previous.getProductId(), previous.getVersion());
        }

        ProductCostRecord newRecord = new ProductCostRecord(
                UUID.randomUUID(),
                tenantId,
                cmd.productId(),
                cmd.variantId(),
                cmd.companyId(),
                cmd.currency(),
                cmd.unit(),
                cmd.acquisitionCost(),
                cmd.packagingCost(),
                cmd.additionalCost(),
                cmd.subsidizedShippingCost(),
                cmd.taxRatePercent(),
                cmd.otherExpensesPercent(),
                effectiveFrom,
                null, // Aberto / vigente atual
                version,
                true,
                userEmail,
                cmd.notes(),
                Instant.now(),
                Instant.now()
        );

        ProductCostRecord saved = productCostRepository.save(newRecord);

        auditPublisher.publish(
                eventType,
                null,
                userEmail,
                tenantId,
                "127.0.0.1",
                "SellerIntelligence-Pricing",
                UUID.randomUUID().toString(),
                String.format("Custo registrado para produto=%s variante=%s empresa=%s versao=%d totalDireto=%s",
                        cmd.productId(), cmd.variantId(), cmd.companyId(), version, saved.getTotalDirectUnitCost())
        );

        log.info("Custo registrado com sucesso: id={} tenant={} product={} version={} totalDireto={}",
                saved.getId(), tenantId, cmd.productId(), version, saved.getTotalDirectUnitCost());

        return ProductCostDto.fromRecord(saved);
    }

    /**
     * Recupera o custo vigente para a data de referência, aplicando a hierarquia de especificidade:
     * 1. (productId, variantId, companyId)
     * 2. (productId, variantId, null)
     * 3. (productId, null, companyId)
     * 4. (productId, null, null) - custo base do produto
     */
    @Transactional(readOnly = true)
    public Optional<ProductCostDto> getEffectiveCost(
            UUID tenantId,
            UUID productId,
            UUID variantId,
            UUID companyId,
            Instant referenceDate
    ) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(productId, "productId não pode ser nulo");

        Instant refDate = referenceDate != null ? referenceDate : Instant.now();

        // 1. Especificidade máxima: variante + empresa
        if (variantId != null && companyId != null) {
            List<ProductCostRecord> exact = productCostRepository.findCandidateCosts(
                    tenantId, productId, variantId, companyId, refDate
            );
            if (!exact.isEmpty()) {
                return Optional.of(ProductCostDto.fromRecord(exact.get(0)));
            }
        }

        // 2. Variante (empresa geral/padrão)
        if (variantId != null) {
            List<ProductCostRecord> byVariant = productCostRepository.findCandidateCosts(
                    tenantId, productId, variantId, null, refDate
            );
            if (!byVariant.isEmpty()) {
                return Optional.of(ProductCostDto.fromRecord(byVariant.get(0)));
            }
        }

        // 3. Produto + empresa
        if (companyId != null) {
            List<ProductCostRecord> byCompany = productCostRepository.findCandidateCosts(
                    tenantId, productId, null, companyId, refDate
            );
            if (!byCompany.isEmpty()) {
                return Optional.of(ProductCostDto.fromRecord(byCompany.get(0)));
            }
        }

        // 4. Produto base geral
        List<ProductCostRecord> baseProduct = productCostRepository.findCandidateCosts(
                tenantId, productId, null, null, refDate
        );
        if (!baseProduct.isEmpty()) {
            return Optional.of(ProductCostDto.fromRecord(baseProduct.get(0)));
        }

        return Optional.empty();
    }

    /**
     * Retorna o histórico cronológico de custos registrados para um produto, permitindo auditoria de versões.
     */
    @Transactional(readOnly = true)
    public List<ProductCostDto> listCostHistory(UUID tenantId, UUID productId) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(productId, "productId não pode ser nulo");

        return productCostRepository.findByTenantIdAndProductIdOrderByEffectiveFromDesc(tenantId, productId)
                .stream()
                .map(ProductCostDto::fromRecord)
                .toList();
    }
}
