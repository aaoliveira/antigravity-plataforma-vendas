package com.sellerintelligence.modules.orderingestion;

import com.sellerintelligence.modules.orderingestion.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Order> findByTenantIdAndMarketplaceAndExternalOrderId(
            UUID tenantId,
            String marketplace,
            String externalOrderId
    );

    List<Order> findAllByTenantId(UUID tenantId);

    List<Order> findAllByTenantIdAndSellerAccountId(UUID tenantId, UUID sellerAccountId);

    long countByTenantId(UUID tenantId);

    long countByTenantIdAndStatus(UUID tenantId, OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.tenantId = :tenantId " +
           "AND (:marketplace IS NULL OR :marketplace = '' OR o.marketplace = :marketplace) " +
           "AND (:sellerAccountId IS NULL OR o.sellerAccountId = :sellerAccountId) " +
           "AND (:status IS NULL OR o.status = :status) " +
           "AND (CAST(:startDate AS timestamp) IS NULL OR o.orderCreatedAt >= :startDate) " +
           "AND (CAST(:endDate AS timestamp) IS NULL OR o.orderCreatedAt <= :endDate) " +
           "AND (:search IS NULL OR :search = '' OR " +
           "     LOWER(o.externalOrderId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(o.buyerName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "     LOWER(o.trackingNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Order> searchOrders(
            @Param("tenantId") UUID tenantId,
            @Param("marketplace") String marketplace,
            @Param("sellerAccountId") UUID sellerAccountId,
            @Param("status") OrderStatus status,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            @Param("search") String search,
            Pageable pageable
    );
}
