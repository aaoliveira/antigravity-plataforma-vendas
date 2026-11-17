package br.com.sellerintelligence.reconciliation.service;

import br.com.sellerintelligence.reconciliation.domain.*;
import br.com.sellerintelligence.sync.domain.Order;
import br.com.sellerintelligence.sync.domain.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ReconciliationMatchingEngine {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationMatchingEngine.class);

    private final OrderRepository orderRepository;
    private final ReconciliationSettlementItemRepository itemRepository;
    private final ReconciliationSettlementRepository settlementRepository;

    public ReconciliationMatchingEngine(OrderRepository orderRepository,
                                        ReconciliationSettlementItemRepository itemRepository,
                                        ReconciliationSettlementRepository settlementRepository) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.settlementRepository = settlementRepository;
    }

    @Transactional
    public void reconcileBatch(ReconciliationSettlement settlement, List<ReconciliationSettlementItem> items) {
        log.info("Starting automated matching for settlement batch id={}, items={}", settlement.getId(), items.size());

        int reconciledCount = 0;
        int discrepantCount = 0;

        for (ReconciliationSettlementItem item : items) {
            reconcileItem(settlement, item);
            if (item.getReconciliationStatus() == ReconciliationItemStatus.CONCILED) {
                reconciledCount++;
            } else if (item.getReconciliationStatus() == ReconciliationItemStatus.DISCREPANT) {
                discrepantCount++;
            }
        }

        itemRepository.saveAll(items);

        settlement.updateMetrics(items.size(), reconciledCount, discrepantCount);
        settlementRepository.save(settlement);

        log.info("Finished automated matching for settlement id={}: reconciled={}, discrepant={}",
                settlement.getId(), reconciledCount, discrepantCount);
    }

    private void reconcileItem(ReconciliationSettlement settlement, ReconciliationSettlementItem item) {
        if (item.getExternalOrderId() != null && !item.getExternalOrderId().isBlank()) {
            Optional<Order> orderOpt = orderRepository.findByTenantIdAndExternalOrderId(
                    settlement.getTenantId(), item.getExternalOrderId()
            );

            if (orderOpt.isPresent()) {
                Order order = orderOpt.get();
                BigDecimal expected = calculateExpectedAmount(order, item.getEntryType());
                BigDecimal settled = item.getSettledAmount();
                BigDecimal discrepancy = settled.subtract(expected);

                boolean isWithinThreshold = discrepancy.abs().compareTo(new BigDecimal("0.01")) <= 0;

                if (isWithinThreshold) {
                    item.reconcileMatched(
                            order.getId(), expected, BigDecimal.ZERO, MatchingMethod.ORDER_ID_EXACT,
                            new BigDecimal("1.0000"), "Conciliado perfeitamente com pedido " + item.getExternalOrderId()
                    );
                } else {
                    item.reconcileMatched(
                            order.getId(), expected, discrepancy, MatchingMethod.ORDER_ID_EXACT,
                            new BigDecimal("1.0000"),
                            String.format("Divergência detectada: esperado R$ %s, liquidado R$ %s (diferença: R$ %s)",
                                    expected, settled, discrepancy)
                    );
                }
            } else {
                if (item.getEntryType() == ReconciliationEntryType.ORDER_PAYOUT) {
                    item.markUnidentified("Pedido " + item.getExternalOrderId() + " não localizado nas vendas sincronizadas do tenant.");
                } else {
                    item.reconcileMatched(
                            null, item.getSettledAmount(), BigDecimal.ZERO, MatchingMethod.TRANSACTION_ID_EXACT,
                            new BigDecimal("0.8500"), "Taxa ou lançamento operacional avulso do canal sem pedido direto."
                    );
                }
            }
        } else {
            if (item.getEntryType() == ReconciliationEntryType.ESCROW_RELEASE ||
                item.getEntryType() == ReconciliationEntryType.ADJUSTMENT ||
                item.getEntryType() == ReconciliationEntryType.TRANSACTION_FEE) {
                item.reconcileMatched(
                        null, item.getSettledAmount(), BigDecimal.ZERO, MatchingMethod.TRANSACTION_ID_EXACT,
                        new BigDecimal("0.8500"), "Evento financeiro avulso do marketplace."
                );
            } else {
                item.markUnidentified("Lançamento sem identificador de pedido ou transação externa.");
            }
        }
    }

    private BigDecimal calculateExpectedAmount(Order order, ReconciliationEntryType entryType) {
        BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal fee = order.getMarketplaceFee() != null ? order.getMarketplaceFee() : BigDecimal.ZERO;
        BigDecimal shipping = order.getShippingCostSeller() != null ? order.getShippingCostSeller() : BigDecimal.ZERO;

        return switch (entryType) {
            case ORDER_PAYOUT -> total.subtract(fee).subtract(shipping);
            case COMMISSION_FEE -> fee.negate();
            case SHIPPING_DEBIT -> shipping.negate();
            case REFUND_CHARGEBACK -> total.negate();
            default -> total;
        };
    }
}
