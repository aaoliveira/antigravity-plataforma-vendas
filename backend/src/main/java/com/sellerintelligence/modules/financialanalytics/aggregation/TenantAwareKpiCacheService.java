package com.sellerintelligence.modules.financialanalytics.aggregation;

import com.sellerintelligence.core.tenant.TenantCacheKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Serviço de cache em memória thread-safe e tenant-aware para agregações analíticas de KPIs.
 * Previne categoricamente vazamentos cross-tenant de cache ao exigir validação dupla de chave e
 * verificação de tenantId no payload em cache.
 */
@Service
public class TenantAwareKpiCacheService {

    private static final Logger log = LoggerFactory.getLogger(TenantAwareKpiCacheService.class);
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(15);

    private final ConcurrentMap<String, CacheEntry> cacheStore = new ConcurrentHashMap<>();
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);

    /**
     * Recupera um resultado em cache de forma estritamente isolada pelo tenantId.
     */
    public <T> Optional<T> get(UUID tenantId, String endpoint, String filterKey, Class<T> clazz) {
        Objects.requireNonNull(tenantId, "tenantId é obrigatório para leitura em cache");
        String fullKey = buildKey(tenantId, endpoint, filterKey);

        CacheEntry entry = cacheStore.get(fullKey);
        if (entry == null) {
            missCount.incrementAndGet();
            return Optional.empty();
        }

        // Validação de expiração (TTL)
        if (entry.expiresAt().isBefore(Instant.now())) {
            cacheStore.remove(fullKey, entry);
            missCount.incrementAndGet();
            return Optional.empty();
        }

        // Validação em profundidade contra contaminação cross-tenant
        if (!tenantId.equals(entry.tenantId())) {
            log.warn("Tentativa de leitura cross-tenant detectada no cache! Chave: {}, Tenant Solicitante: {}, Tenant Proprietário: {}",
                    fullKey, tenantId, entry.tenantId());
            missCount.incrementAndGet();
            return Optional.empty();
        }

        hitCount.incrementAndGet();
        try {
            return Optional.of(clazz.cast(entry.data()));
        } catch (ClassCastException e) {
            log.error("Erro de coerção de tipo no cache para chave {}: {}", fullKey, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Armazena resultado em cache associado ao tenantId e TTL especificado.
     */
    public void put(UUID tenantId, String endpoint, String filterKey, Object data, Duration ttl) {
        Objects.requireNonNull(tenantId, "tenantId é obrigatório para escrita em cache");
        Objects.requireNonNull(data, "dados a armazenar não podem ser nulos");

        Duration effectiveTtl = (ttl != null && !ttl.isNegative() && !ttl.isZero()) ? ttl : DEFAULT_TTL;
        String fullKey = buildKey(tenantId, endpoint, filterKey);
        Instant expiresAt = Instant.now().plus(effectiveTtl);

        cacheStore.put(fullKey, new CacheEntry(tenantId, data, expiresAt));
    }

    /**
     * Armazena resultado com o TTL padrão de 15 minutos.
     */
    public void put(UUID tenantId, String endpoint, String filterKey, Object data) {
        put(tenantId, endpoint, filterKey, data, DEFAULT_TTL);
    }

    /**
     * Invalida seletivamente todas as entradas de cache pertencentes ao tenantId especificado,
     * mantendo completamente intactos os dados em cache de outros tenants.
     */
    public long invalidateTenant(UUID tenantId) {
        Objects.requireNonNull(tenantId, "tenantId é obrigatório para invalidação");
        long removed = 0;

        for (var entry : cacheStore.entrySet()) {
            if (tenantId.equals(entry.getValue().tenantId())) {
                if (cacheStore.remove(entry.getKey(), entry.getValue())) {
                    removed++;
                }
            }
        }

        evictionCount.addAndGet(removed);
        log.info("Cache de KPIs invalidado para tenant={}: {} entradas removidas.", tenantId, removed);
        return removed;
    }

    /**
     * Invalidação total de cache (útil em resets administrativos ou testes).
     */
    public void invalidateAll() {
        int size = cacheStore.size();
        cacheStore.clear();
        evictionCount.addAndGet(size);
        log.info("Cache global de KPIs resetado: {} entradas removidas.", size);
    }

    public long getHitCount() {
        return hitCount.get();
    }

    public long getMissCount() {
        return missCount.get();
    }

    public long getEvictionCount() {
        return evictionCount.get();
    }

    public int size() {
        return cacheStore.size();
    }

    private String buildKey(UUID tenantId, String endpoint, String filterKey) {
        String subKey = String.format("%s:%s", endpoint, filterKey != null ? filterKey : "default");
        return TenantCacheKeyGenerator.generateKey(tenantId, "kpis", subKey);
    }

    private record CacheEntry(UUID tenantId, Object data, Instant expiresAt) {}
}
