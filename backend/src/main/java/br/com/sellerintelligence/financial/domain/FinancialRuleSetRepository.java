package br.com.sellerintelligence.financial.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FinancialRuleSetRepository extends JpaRepository<FinancialRuleSet, UUID> {

    List<FinancialRuleSet> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<FinancialRuleSet> findByTenantIdAndNameOrderByVersionDesc(UUID tenantId, String name);

    Optional<FinancialRuleSet> findByTenantIdAndNameAndVersion(UUID tenantId, String name, Integer version);

    @Query("SELECT r FROM FinancialRuleSet r WHERE r.tenantId = :tenantId AND r.active = true " +
           "AND (:marketplace IS NULL OR r.marketplace = :marketplace OR r.marketplace IS NULL) " +
           "AND r.effectiveFrom <= :targetDate " +
           "AND (r.effectiveTo IS NULL OR r.effectiveTo > :targetDate) " +
           "ORDER BY r.version DESC")
    List<FinancialRuleSet> findActiveRulesAtDate(
            @Param("tenantId") UUID tenantId,
            @Param("marketplace") String marketplace,
            @Param("targetDate") Instant targetDate
    );

    default Optional<FinancialRuleSet> findBestMatchingRule(UUID tenantId, String marketplace, Instant targetDate) {
        List<FinancialRuleSet> rules = findActiveRulesAtDate(tenantId, marketplace, targetDate);
        if (rules.isEmpty()) {
            return Optional.empty();
        }
        // Prefer exact marketplace match if available
        return rules.stream()
                .filter(r -> marketplace != null && marketplace.equalsIgnoreCase(r.getMarketplace()))
                .findFirst()
                .or(() -> Optional.of(rules.get(0)));
    }
}
