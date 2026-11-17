package br.com.sellerintelligence.reconciliation.service;

import br.com.sellerintelligence.reconciliation.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class SettlementIngestionService {

    private static final Logger log = LoggerFactory.getLogger(SettlementIngestionService.class);

    private final ReconciliationSettlementRepository settlementRepository;
    private final ReconciliationSettlementItemRepository itemRepository;
    private final ReconciliationMatchingEngine matchingEngine;

    public SettlementIngestionService(ReconciliationSettlementRepository settlementRepository,
                                      ReconciliationSettlementItemRepository itemRepository,
                                      ReconciliationMatchingEngine matchingEngine) {
        this.settlementRepository = settlementRepository;
        this.itemRepository = itemRepository;
        this.matchingEngine = matchingEngine;
    }

    public record SettlementItemIngestDto(
            String externalOrderId,
            String externalTransactionId,
            ReconciliationEntryType entryType,
            BigDecimal settledAmount
    ) {}

    public record SettlementIngestRequest(
            String externalSettlementId,
            String marketplace,
            UUID storeId,
            OffsetDateTime settlementDate,
            BigDecimal grossAmount,
            BigDecimal netAmount,
            BigDecimal totalFees,
            BigDecimal totalShipping,
            BigDecimal totalAdjustments,
            String sourceFilename,
            List<SettlementItemIngestDto> items
    ) {}

    @Transactional
    public ReconciliationSettlement ingestSettlement(UUID tenantId, SettlementIngestRequest request) {
        log.info("Ingesting settlement batch: externalSettlementId={}, marketplace={}, tenantId={}",
                request.externalSettlementId(), request.marketplace(), tenantId);

        // 1. Idempotência: verificar se lote já foi importado para evitar duplicidade
        Optional<ReconciliationSettlement> existing = settlementRepository
                .findByTenantIdAndMarketplaceAndExternalSettlementId(tenantId, request.marketplace(), request.externalSettlementId());
        if (existing.isPresent()) {
            log.warn("Settlement batch already exists. Returning existing batch to guarantee idempotency: id={}, externalSettlementId={}",
                    existing.get().getId(), request.externalSettlementId());
            return existing.get();
        }

        // 2. Calcula hash SHA-256 do lote
        String hashFingerprint = calculateHash(
                tenantId.toString(),
                request.marketplace(),
                request.externalSettlementId(),
                request.grossAmount().toPlainString(),
                request.netAmount().toPlainString(),
                String.valueOf(request.items() != null ? request.items().size() : 0)
        );

        // 3. Cria e salva o lote de repasse
        ReconciliationSettlement settlement = new ReconciliationSettlement(
                tenantId,
                request.storeId(),
                request.marketplace(),
                request.externalSettlementId(),
                request.sourceFilename(),
                request.settlementDate() != null ? request.settlementDate() : OffsetDateTime.now(),
                request.grossAmount(),
                request.netAmount(),
                request.totalFees(),
                request.totalShipping(),
                request.totalAdjustments(),
                hashFingerprint
        );
        ReconciliationSettlement savedSettlement = settlementRepository.save(settlement);

        // 4. Cria e salva os lançamentos individuais
        List<ReconciliationSettlementItem> itemsToSave = new ArrayList<>();
        if (request.items() != null) {
            for (SettlementItemIngestDto itemDto : request.items()) {
                ReconciliationSettlementItem item = new ReconciliationSettlementItem(
                        savedSettlement.getId(),
                        tenantId,
                        itemDto.externalOrderId(),
                        itemDto.externalTransactionId(),
                        itemDto.entryType(),
                        itemDto.settledAmount()
                );
                itemsToSave.add(item);
            }
        }
        List<ReconciliationSettlementItem> savedItems = itemRepository.saveAll(itemsToSave);

        // 5. Executa conciliação automática com os pedidos do tenant
        matchingEngine.reconcileBatch(savedSettlement, savedItems);

        log.info("Successfully processed settlement batch: id={}, totalItems={}", savedSettlement.getId(), savedItems.size());
        return savedSettlement;
    }

    private String calculateHash(String... values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String val : values) {
                if (val != null) {
                    digest.update(val.getBytes(StandardCharsets.UTF_8));
                }
            }
            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
