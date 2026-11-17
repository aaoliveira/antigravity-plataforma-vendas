package br.com.sellerintelligence.financial.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderFinancialCalculationRepository extends JpaRepository<OrderFinancialCalculation, UUID> {

    Optional<OrderFinancialCalculation> findByTenantIdAndOrderId(UUID tenantId, UUID orderId);

    List<OrderFinancialCalculation> findByTenantIdOrderByCalculatedAtDesc(UUID tenantId);

    @Query("SELECT c FROM OrderFinancialCalculation c, br.com.sellerintelligence.sync.domain.Order o " +
           "WHERE c.orderId = o.id AND c.tenantId = :tenantId " +
           "AND o.orderDate >= :from AND o.orderDate <= :to " +
           "AND (:storeId IS NULL OR o.storeId = :storeId) " +
           "AND (:marketplace IS NULL OR o.marketplace = :marketplace) " +
           "ORDER BY o.orderDate DESC")
    List<OrderFinancialCalculation> findCalculationsByPeriod(
            @Param("tenantId") UUID tenantId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("storeId") UUID storeId,
            @Param("marketplace") Marketplace marketplace
    );
}
