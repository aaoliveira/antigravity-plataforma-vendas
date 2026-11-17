package br.com.sellerintelligence.financial.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemFinancialCalculationRepository extends JpaRepository<OrderItemFinancialCalculation, UUID> {

    List<OrderItemFinancialCalculation> findByTenantIdAndOrderCalculationId(UUID tenantId, UUID orderCalculationId);
}
