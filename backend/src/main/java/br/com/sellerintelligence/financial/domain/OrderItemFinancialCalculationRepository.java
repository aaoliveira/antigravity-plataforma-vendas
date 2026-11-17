package br.com.sellerintelligence.financial.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemFinancialCalculationRepository extends JpaRepository<OrderItemFinancialCalculation, UUID> {

    List<OrderItemFinancialCalculation> findByTenantIdAndOrderCalculationId(UUID tenantId, UUID orderCalculationId);

    @Query("SELECT ic FROM OrderItemFinancialCalculation ic, OrderFinancialCalculation c, br.com.sellerintelligence.sync.domain.Order o " +
           "WHERE ic.orderCalculation.id = c.id AND c.orderId = o.id AND ic.tenantId = :tenantId " +
           "AND o.orderDate >= :from AND o.orderDate <= :to " +
           "AND (:storeId IS NULL OR o.storeId = :storeId) " +
           "AND (:marketplace IS NULL OR o.marketplace = :marketplace)")
    List<OrderItemFinancialCalculation> findItemCalculationsByPeriod(
            @Param("tenantId") UUID tenantId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("storeId") UUID storeId,
            @Param("marketplace") Marketplace marketplace
    );
}
