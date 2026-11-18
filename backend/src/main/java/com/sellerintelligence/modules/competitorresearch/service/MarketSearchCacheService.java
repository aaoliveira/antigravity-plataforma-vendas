package com.sellerintelligence.modules.competitorresearch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchCacheRecord;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchCacheRepository;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchRequest;
import com.sellerintelligence.modules.competitorresearch.model.MarketSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço de cache de resultados de busca por tenant com validade temporal (TTL).
 * Evita sobrecarga das APIs externas, garante conformidade com quotas de rate limiting
 * e preserva o isolamento multi-tenant.
 */
@Service
public class MarketSearchCacheService {

    private static final Logger log = LoggerFactory.getLogger(MarketSearchCacheService.class);

    private final MarketSearchCacheRepository cacheRepository;
    private final ObjectMapper objectMapper;

    public MarketSearchCacheService(MarketSearchCacheRepository cacheRepository, ObjectMapper objectMapper) {
        this.cacheRepository = cacheRepository;
        this.objectMapper = objectMapper;
    }

    public String computeCacheKey(UUID tenantId, MarketSearchRequest request) {
        String normalizedQuery = request.q() != null ? request.q().trim().toLowerCase() : "";
        String marketplace = request.marketplace() != null ? request.marketplace().trim().toUpperCase() : "ALL";
        String category = request.category() != null ? request.category().trim().toLowerCase() : "";
        String minPrice = request.minPrice() != null ? request.minPrice().toPlainString() : "";
        String maxPrice = request.maxPrice() != null ? request.maxPrice().toPlainString() : "";
        String ownCatalog = Boolean.TRUE.equals(request.onlyOwnCatalog()) ? "true" : "false";
        int page = request.page() != null ? request.page() : 0;
        int size = request.size() != null ? request.size() : 20;

        String rawKey = String.join("|",
                tenantId.toString(),
                normalizedQuery,
                marketplace,
                category,
                minPrice,
                maxPrice,
                ownCatalog,
                String.valueOf(page),
                String.valueOf(size)
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(rawKey.hashCode());
        }
    }

    @Transactional(readOnly = true)
    public Optional<MarketSearchResponse> getValidCache(UUID tenantId, String cacheKey) {
        Optional<MarketSearchCacheRecord> recordOpt = cacheRepository.findByTenantIdAndCacheKey(tenantId, cacheKey);
        if (recordOpt.isEmpty()) {
            return Optional.empty();
        }

        MarketSearchCacheRecord record = recordOpt.get();
        if (record.getExpiresAt().isBefore(Instant.now())) {
            log.debug("Cache expirado para tenant={} cacheKey={}", tenantId, cacheKey);
            return Optional.empty();
        }

        try {
            MarketSearchResponse response = objectMapper.readValue(record.getResultsPayloadJson(), MarketSearchResponse.class);
            // Retorna marcado como cached=true
            return Optional.of(new MarketSearchResponse(
                    response.items(),
                    response.totalElements(),
                    response.totalPages(),
                    response.currentPage(),
                    response.pageSize(),
                    response.sourceUsed(),
                    response.sourcePolicyId(),
                    response.capabilityStatus(),
                    true,
                    response.capabilityNotice(),
                    response.rateLimitRemaining()
            ));
        } catch (JsonProcessingException e) {
            log.error("Erro ao desserializar payload de cache para tenant={}: {}", tenantId, e.getMessage());
            return Optional.empty();
        }
    }

    @Transactional
    public void putCache(UUID tenantId, String cacheKey, String sourcePolicyId, MarketSearchResponse response, Duration ttl) {
        try {
            String payloadJson = objectMapper.writeValueAsString(response);
            Instant expiresAt = Instant.now().plus(ttl != null ? ttl : Duration.ofMinutes(15));

            Optional<MarketSearchCacheRecord> existingOpt = cacheRepository.findByTenantIdAndCacheKey(tenantId, cacheKey);
            MarketSearchCacheRecord record;
            if (existingOpt.isPresent()) {
                record = existingOpt.get();
                record.setResultsPayloadJson(payloadJson);
                record.setResultCount(response.items().size());
                record.setSourcePolicyId(sourcePolicyId);
                record.setExpiresAt(expiresAt);
            } else {
                record = new MarketSearchCacheRecord(
                        UUID.randomUUID(),
                        tenantId,
                        cacheKey,
                        sourcePolicyId,
                        payloadJson,
                        response.items().size(),
                        expiresAt,
                        Instant.now()
                );
            }
            cacheRepository.save(record);
            log.debug("Cache gravado com sucesso para tenant={} key={} expiresAt={}", tenantId, cacheKey, expiresAt);
        } catch (JsonProcessingException e) {
            log.error("Erro ao serializar payload de pesquisa para cache: {}", e.getMessage());
        }
    }

    @Transactional
    public void evictExpired() {
        cacheRepository.deleteExpired(Instant.now());
    }
}
