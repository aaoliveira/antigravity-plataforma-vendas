package com.sellerintelligence.modules.financialanalytics.aggregation;

import com.sellerintelligence.modules.company.repository.CompanyRepository;
import com.sellerintelligence.modules.financialanalytics.entity.OrderFinancialFactRecord;
import com.sellerintelligence.modules.financialanalytics.model.FinancialValueProvenance;
import com.sellerintelligence.modules.financialanalytics.model.ReconciliationStatus;
import com.sellerintelligence.modules.financialanalytics.repository.OrderFinancialFactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AggregatedKpiPerformanceBenchmarkTest - Benchmark de Latência e Medição de p95")
class AggregatedKpiPerformanceBenchmarkTest {

    @Mock
    private OrderFinancialFactRepository factRepository;

    @Mock
    private CompanyRepository companyRepository;

    private TenantAwareKpiCacheService cacheService;
    private AggregatedKpiService kpiService;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID companyId = UUID.randomUUID();
    private List<OrderFinancialFactRecord> syntheticFacts;

    @BeforeEach
    void setUp() {
        cacheService = new TenantAwareKpiCacheService();
        kpiService = new AggregatedKpiService(factRepository, companyRepository, cacheService);

        // Gera 1.000 fatos financeiros sintéticos para teste de agregação em lote
        syntheticFacts = new ArrayList<>(1000);
        Instant baseTime = Instant.now().minus(30, ChronoUnit.DAYS);

        for (int i = 0; i < 1000; i++) {
            OrderFinancialFactRecord fact = new OrderFinancialFactRecord(
                    UUID.randomUUID(),
                    tenantId,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    companyId,
                    i % 2 == 0 ? "MERCADO_LIVRE" : "SHOPEE",
                    "EXT-ORDER-" + i,
                    "PAID",
                    baseTime.plus(i * 30L, ChronoUnit.MINUTES),
                    BigDecimal.valueOf(100.00 + (i % 50)),
                    BigDecimal.valueOf(5.00),
                    BigDecimal.valueOf(95.00 + (i % 50)),
                    i % 20 == 0 ? BigDecimal.valueOf(95.00) : BigDecimal.ZERO,
                    i % 30 == 0 ? BigDecimal.valueOf(50.00) : BigDecimal.ZERO,
                    BigDecimal.valueOf(15.00),
                    BigDecimal.valueOf(3.00),
                    BigDecimal.valueOf(12.00),
                    BigDecimal.valueOf(40.00),
                    BigDecimal.valueOf(2.00),
                    BigDecimal.valueOf(1.00),
                    BigDecimal.valueOf(8.00),
                    BigDecimal.valueOf(0.50),
                    BigDecimal.valueOf(81.50),
                    BigDecimal.valueOf(13.50 + (i % 50)),
                    BigDecimal.valueOf(14.2100),
                    "BRL",
                    ReconciliationStatus.RECONCILED,
                    FinancialValueProvenance.CONFIRMED,
                    FinancialValueProvenance.CONFIRMED,
                    FinancialValueProvenance.CONFIRMED,
                    FinancialValueProvenance.USER_INFORMED,
                    false,
                    "[]",
                    "v1.0.0",
                    "{}"
            );
            syntheticFacts.add(fact);
        }

        when(factRepository.findForAggregation(eq(tenantId), any(), any(), any(), any(), any()))
                .thenReturn(syntheticFacts);
    }

    @Test
    @DisplayName("Deve medir latência p95 < 500ms em execuções sequenciais (com e sem cache)")
    void shouldMeasureSequentialP95LatencyBelowThreshold() {
        AggregatedKpiFilter filter = new AggregatedKpiFilter(
                companyId,
                null,
                null,
                Instant.now().minus(30, ChronoUnit.DAYS),
                Instant.now()
        );

        int warmUpIterations = 20;
        int benchmarkIterations = 150;
        List<Long> latenciesNanos = new ArrayList<>(benchmarkIterations);

        // Aquecimento JVM
        for (int i = 0; i < warmUpIterations; i++) {
            kpiService.getAggregatedSummary(tenantId, filter);
            cacheService.invalidateTenant(tenantId); // Força uncached no warmup
        }

        // Benchmark: 20% uncached (invalidação simulada), 80% cached
        for (int i = 0; i < benchmarkIterations; i++) {
            if (i % 5 == 0) {
                cacheService.invalidateTenant(tenantId);
            }
            long start = System.nanoTime();
            AggregatedKpiSummaryDto summary = kpiService.getAggregatedSummary(tenantId, filter);
            long elapsed = System.nanoTime() - start;
            latenciesNanos.add(elapsed);

            assertThat(summary).isNotNull();
            assertThat(summary.totalOrders()).isEqualTo(1000);
        }

        // Cálculo de métricas estatísticas
        Collections.sort(latenciesNanos);
        int p50Index = (int) Math.ceil(0.50 * latenciesNanos.size()) - 1;
        int p90Index = (int) Math.ceil(0.90 * latenciesNanos.size()) - 1;
        int p95Index = (int) Math.ceil(0.95 * latenciesNanos.size()) - 1;
        int p99Index = (int) Math.ceil(0.99 * latenciesNanos.size()) - 1;

        double p50Ms = latenciesNanos.get(p50Index) / 1_000_000.0;
        double p90Ms = latenciesNanos.get(p90Index) / 1_000_000.0;
        double p95Ms = latenciesNanos.get(p95Index) / 1_000_000.0;
        double p99Ms = latenciesNanos.get(p99Index) / 1_000_000.0;
        double avgMs = latenciesNanos.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0;

        System.out.printf("--- BENCHMARK RESULTS (SEQUENTIAL 1.000 Fatos) ---%n");
        System.out.printf("Iterações: %d | Amostras: %d%n", benchmarkIterations, latenciesNanos.size());
        System.out.printf("Média:   %.4f ms%n", avgMs);
        System.out.printf("p50:     %.4f ms%n", p50Ms);
        System.out.printf("p90:     %.4f ms%n", p90Ms);
        System.out.printf("p95:     %.4f ms%n", p95Ms);
        System.out.printf("p99:     %.4f ms%n", p99Ms);
        System.out.printf("Cache Hits: %d, Misses: %d, Size: %d%n",
                cacheService.getHitCount(), cacheService.getMissCount(), cacheService.size());

        // Verificação do critério de aceite: p95 medido e estritamente abaixo do SLA de 500ms (meta < 100ms)
        assertThat(p95Ms).isLessThan(500.0);
        assertThat(p95Ms).isLessThan(100.0); // Esperado em memória < 100ms
    }

    @Test
    @DisplayName("Deve medir latência concorrente e garantir p95 < 500ms com 10 threads simultâneas")
    void shouldMeasureConcurrentP95LatencyUnderMultiThreading() throws Exception {
        AggregatedKpiFilter filter = new AggregatedKpiFilter(
                companyId,
                null,
                null,
                Instant.now().minus(30, ChronoUnit.DAYS),
                Instant.now()
        );

        int numThreads = 10;
        int requestsPerThread = 25;
        int totalRequests = numThreads * requestsPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numThreads);

        List<Long> allLatenciesNanos = Collections.synchronizedList(new ArrayList<>(totalRequests));

        for (int t = 0; t < numThreads; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < requestsPerThread; i++) {
                        long start = System.nanoTime();
                        AggregatedKpiSummaryDto summary = kpiService.getAggregatedSummary(tenantId, filter);
                        long elapsed = System.nanoTime() - start;
                        allLatenciesNanos.add(elapsed);
                        assertThat(summary).isNotNull();
                    }
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Dispara todas as threads concorrentemente
        startLatch.countDown();
        boolean completed = finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        assertThat(allLatenciesNanos).hasSize(totalRequests);

        List<Long> sortedLatencies = new ArrayList<>(allLatenciesNanos);
        Collections.sort(sortedLatencies);

        int p95Index = (int) Math.ceil(0.95 * sortedLatencies.size()) - 1;
        double p95Ms = sortedLatencies.get(p95Index) / 1_000_000.0;
        double avgMs = sortedLatencies.stream().mapToLong(Long::longValue).average().orElse(0) / 1_000_000.0;

        System.out.printf("--- CONCURRENT BENCHMARK RESULTS (10 Threads, 250 Requisições) ---%n");
        System.out.printf("Média:   %.4f ms%n", avgMs);
        System.out.printf("p95:     %.4f ms%n", p95Ms);
        System.out.printf("Cache Hits: %d, Misses: %d, Size: %d%n",
                cacheService.getHitCount(), cacheService.getMissCount(), cacheService.size());

        assertThat(p95Ms).isLessThan(500.0);
    }
}
