package com.sellerintelligence.modules.pricing.service;

import com.sellerintelligence.modules.pricing.entity.FeeRuleRecord;
import com.sellerintelligence.modules.pricing.model.FeeResolutionRequest;
import com.sellerintelligence.modules.pricing.model.FeeResolutionResult;
import com.sellerintelligence.modules.pricing.model.FeeResolutionStatus;
import com.sellerintelligence.modules.pricing.model.FeeType;
import com.sellerintelligence.modules.pricing.model.ResolvedFeeItem;
import com.sellerintelligence.modules.pricing.repository.FeeRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Motor determinístico de resolução e cálculo de tarifas e comissões.
 * Aplica filtros de vigência temporal, faixa de preço e hierarquia de categoria,
 * garantindo explicitação mandatória de ausência de taxas sem inventar valores fictícios (Anti-Alucinação).
 */
@Service
public class FeeResolutionService {

    private static final Logger log = LoggerFactory.getLogger(FeeResolutionService.class);

    private final FeeRuleRepository feeRuleRepository;

    public FeeResolutionService(FeeRuleRepository feeRuleRepository) {
        this.feeRuleRepository = feeRuleRepository;
    }

    @Transactional(readOnly = true)
    public FeeResolutionResult resolveFees(UUID tenantId, FeeResolutionRequest req) {
        Objects.requireNonNull(tenantId, "tenantId não pode ser nulo");
        Objects.requireNonNull(req, "req não pode ser nulo");
        Objects.requireNonNull(req.marketplace(), "marketplace não pode ser nulo");
        Objects.requireNonNull(req.listingType(), "listingType não pode ser nulo");
        Objects.requireNonNull(req.price(), "price não pode ser nulo");

        String normMarketplace = req.marketplace().toUpperCase().trim();
        String normListingType = req.listingType().toUpperCase().trim();
        String category = (req.categoryId() != null && !req.categoryId().isBlank()) ? req.categoryId().trim() : null;
        BigDecimal price = req.price().setScale(4, RoundingMode.HALF_UP);
        Instant refDate = req.referenceDate() != null ? req.referenceDate() : Instant.now();

        // 1. Busca todas as regras candidatas vigentes
        List<FeeRuleRecord> candidateRules = feeRuleRepository.findCandidateRules(
                tenantId, normMarketplace, normListingType, refDate
        );

        // 2. Filtra regras aplicáveis à faixa de preço e categoria
        List<FeeRuleRecord> matchingRules = candidateRules.stream()
                .filter(r -> r.appliesToPrice(price))
                .filter(r -> categoryMatches(r.getCategoryId(), category))
                .toList();

        // 3. Deduplica por tipo de taxa (FeeType), selecionando a regra mais específica
        Map<FeeType, FeeRuleRecord> bestRuleByFeeType = new EnumMap<>(FeeType.class);

        for (FeeRuleRecord rule : matchingRules) {
            FeeType type = rule.getFeeType();
            if (!bestRuleByFeeType.containsKey(type)) {
                bestRuleByFeeType.put(type, rule);
            } else {
                FeeRuleRecord currentBest = bestRuleByFeeType.get(type);
                if (isMoreSpecific(rule, currentBest, category)) {
                    bestRuleByFeeType.put(type, rule);
                }
            }
        }

        // 4. Calcula os valores de cada taxa resolvida
        List<ResolvedFeeItem> resolvedFeeItems = new ArrayList<>();
        BigDecimal totalFees = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        for (FeeRuleRecord rule : bestRuleByFeeType.values()) {
            BigDecimal feeAmount = rule.calculateFeeAmount(price);
            totalFees = totalFees.add(feeAmount).setScale(4, RoundingMode.HALF_UP);

            resolvedFeeItems.add(new ResolvedFeeItem(
                    rule.getId(),
                    rule.getFeeType(),
                    rule.getFeeValueType(),
                    rule.getPercentageRate(),
                    rule.getFixedAmount(),
                    feeAmount,
                    rule.getCurrency(),
                    rule.getSourceType(),
                    rule.getSourceReference(),
                    rule.getVersion()
            ));
        }

        resolvedFeeItems.sort(Comparator.comparing(f -> f.feeType().name()));

        // 5. Validação de Completude e Ausência Explícita (Anti-Alucinação Estrita)
        boolean hasCommission = bestRuleByFeeType.containsKey(FeeType.COMMISSION);
        boolean hasMissingFees = !hasCommission;
        List<FeeType> missingFeeTypes = new ArrayList<>();
        FeeResolutionStatus status;
        String userAction = null;

        if (!hasCommission) {
            missingFeeTypes.add(FeeType.COMMISSION);
            if (resolvedFeeItems.isEmpty()) {
                status = FeeResolutionStatus.MISSING;
            } else {
                status = FeeResolutionStatus.PARTIAL;
            }
            userAction = String.format(
                    "Nenhuma regra de comissão validada encontrada para o canal %s (%s). " +
                    "A plataforma não assume taxa zero sem fonte oficial comprovada. " +
                    "Por favor, cadastre a comissão oficial ou contratual aplicável.",
                    normMarketplace, normListingType
            );
            log.warn("Resolução de taxas incompleta para tenant={} mkt={} modalidade={}: taxa de comissão ausente",
                    tenantId, normMarketplace, normListingType);
        } else {
            status = FeeResolutionStatus.COMPLETE;
        }

        return new FeeResolutionResult(
                normMarketplace,
                normListingType,
                category,
                price,
                "BRL",
                Collections.unmodifiableList(resolvedFeeItems),
                totalFees,
                hasMissingFees,
                Collections.unmodifiableList(missingFeeTypes),
                status,
                userAction
        );
    }

    private boolean categoryMatches(String ruleCategory, String requestedCategory) {
        if (ruleCategory == null || ruleCategory.isBlank() || "DEFAULT".equalsIgnoreCase(ruleCategory)) {
            return true;
        }
        return requestedCategory != null && ruleCategory.equalsIgnoreCase(requestedCategory);
    }

    private boolean isMoreSpecific(FeeRuleRecord candidate, FeeRuleRecord current, String requestedCategory) {
        // Categoria específica ganha de categoria nula/DEFAULT
        boolean candidateHasSpecificCategory = candidate.getCategoryId() != null &&
                requestedCategory != null &&
                candidate.getCategoryId().equalsIgnoreCase(requestedCategory);
        boolean currentHasSpecificCategory = current.getCategoryId() != null &&
                requestedCategory != null &&
                current.getCategoryId().equalsIgnoreCase(requestedCategory);

        if (candidateHasSpecificCategory && !currentHasSpecificCategory) {
            return true;
        }
        if (!candidateHasSpecificCategory && currentHasSpecificCategory) {
            return false;
        }

        // Versão mais recente ganha
        return candidate.getVersion() > current.getVersion();
    }
}
