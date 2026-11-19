package com.sellerintelligence.modules.pricing.service;

import com.sellerintelligence.core.audit.AuditPublisher;
import com.sellerintelligence.modules.pricing.entity.FeeRuleRecord;
import com.sellerintelligence.modules.pricing.model.FeeRuleDto;
import com.sellerintelligence.modules.pricing.model.FeeValueType;
import com.sellerintelligence.modules.pricing.model.RegisterFeeRuleCommand;
import com.sellerintelligence.modules.pricing.repository.FeeRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço responsável pelo cadastro, versionamento e governança de regras de tarifas e comissões.
 * Garante conformidade com o princípio anti-alucinação exigindo referência documental da fonte.
 */
@Service
public class FeeRuleService {

    private static final Logger log = LoggerFactory.getLogger(FeeRuleService.class);

    private final FeeRuleRepository feeRuleRepository;
    private final AuditPublisher auditPublisher;

    public FeeRuleService(
            FeeRuleRepository feeRuleRepository,
            AuditPublisher auditPublisher
    ) {
        this.feeRuleRepository = feeRuleRepository;
        this.auditPublisher = auditPublisher;
    }

    /**
     * Registra ou atualiza uma regra de cobrança.
     * Se já existir uma regra em aberto para a mesma combinação (marketplace, listingType, categoryId, feeType),
     * encerra a vigência da anterior e gera nova versão.
     */
    @Transactional
    public FeeRuleDto registerRule(UUID tenantId, String userEmail, RegisterFeeRuleCommand cmd) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(userEmail, "userEmail não pode ser nulo");
        Objects.requireNonNull(cmd, "cmd não pode ser nulo");

        if (cmd.sourceReference() == null || cmd.sourceReference().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "sourceReference é obrigatório. Não é permitido criar regras de taxa sem fonte contratual ou oficial comprovada."
            );
        }

        validateValues(cmd);

        Instant effectiveFrom = cmd.effectiveFrom() != null ? cmd.effectiveFrom() : Instant.now();

        String normMarketplace = cmd.marketplace().toUpperCase().trim();
        String normListingType = cmd.listingType().toUpperCase().trim();
        String normCategory = (cmd.categoryId() != null && !cmd.categoryId().isBlank()) ? cmd.categoryId().trim() : null;

        Optional<FeeRuleRecord> currentOpenOpt = feeRuleRepository.findCurrentOpenRule(
                tenantId,
                normMarketplace,
                normListingType,
                normCategory,
                cmd.feeType()
        );

        int version = 1;
        String eventType = "FEE_RULE_CREATED";

        if (currentOpenOpt.isPresent()) {
            FeeRuleRecord previous = currentOpenOpt.get();
            version = previous.getVersion() + 1;
            eventType = "FEE_RULE_UPDATED";

            previous.closeValidity(effectiveFrom);
            feeRuleRepository.save(previous);

            log.info("Encerrada vigência da regra de taxa id={} marketplace={} tipo={} versão={}",
                    previous.getId(), previous.getMarketplace(), previous.getFeeType(), previous.getVersion());
        }

        FeeRuleRecord newRecord = new FeeRuleRecord(
                UUID.randomUUID(),
                tenantId,
                normMarketplace,
                normListingType,
                normCategory,
                cmd.feeType(),
                cmd.feeValueType(),
                cmd.percentageRate(),
                cmd.fixedAmount(),
                cmd.currency(),
                cmd.minPriceThreshold(),
                cmd.maxPriceThreshold(),
                cmd.sourceType(),
                cmd.sourceReference().trim(),
                cmd.legalDisclaimer(),
                effectiveFrom,
                null,
                version,
                true,
                userEmail,
                Instant.now(),
                Instant.now()
        );

        FeeRuleRecord saved = feeRuleRepository.save(newRecord);

        auditPublisher.publish(
                eventType,
                null,
                userEmail,
                tenantId,
                "127.0.0.1",
                "SellerIntelligence-Pricing",
                UUID.randomUUID().toString(),
                String.format("Regra de taxa registrada: mkt=%s modalidade=%s tipo=%s versao=%d fonte=%s",
                        normMarketplace, normListingType, cmd.feeType(), version, cmd.sourceReference())
        );

        log.info("Regra de taxa cadastrada: id={} tenant={} mkt={} tipo={} versao={}",
                saved.getId(), tenantId, normMarketplace, cmd.feeType(), version);

        return FeeRuleDto.fromRecord(saved);
    }

    @Transactional(readOnly = true)
    public List<FeeRuleDto> listRules(UUID tenantId, String marketplace) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");

        List<FeeRuleRecord> records;
        if (marketplace != null && !marketplace.isBlank()) {
            records = feeRuleRepository.findByTenantIdAndMarketplaceOrderByEffectiveFromDesc(
                    tenantId, marketplace.toUpperCase().trim()
            );
        } else {
            records = feeRuleRepository.findByTenantIdOrderByMarketplaceAscListingTypeAscEffectiveFromDesc(tenantId);
        }

        return records.stream().map(FeeRuleDto::fromRecord).toList();
    }

    private void validateValues(RegisterFeeRuleCommand cmd) {
        if (cmd.feeValueType() == FeeValueType.PERCENTAGE || cmd.feeValueType() == FeeValueType.PERCENTAGE_PLUS_FIXED) {
            if (cmd.percentageRate() == null || cmd.percentageRate().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("percentageRate é obrigatório e não pode ser negativo para a modalidade " + cmd.feeValueType());
            }
        }
        if (cmd.feeValueType() == FeeValueType.FIXED_AMOUNT || cmd.feeValueType() == FeeValueType.PERCENTAGE_PLUS_FIXED) {
            if (cmd.fixedAmount() == null || cmd.fixedAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("fixedAmount é obrigatório e não pode ser negativo para a modalidade " + cmd.feeValueType());
            }
        }
        if (cmd.minPriceThreshold() != null && cmd.maxPriceThreshold() != null) {
            if (cmd.minPriceThreshold().compareTo(cmd.maxPriceThreshold()) > 0) {
                throw new IllegalArgumentException("minPriceThreshold não pode ser maior que maxPriceThreshold");
            }
        }
    }
}
