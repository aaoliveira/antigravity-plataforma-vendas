package br.com.sellerintelligence.financial.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderFinancialCalculationRepository extends JpaRepository<OrderFinancialCalculation, UUID> {

    Optional<OrderFinancialCalculation> findByTenantIdAndOrderId(UUID tenantId, UUID orderId);

    List<OrderFinancialCalculation> findByTenantIdOrderByCalculatedAtDesc(UUID tenantId);
}
