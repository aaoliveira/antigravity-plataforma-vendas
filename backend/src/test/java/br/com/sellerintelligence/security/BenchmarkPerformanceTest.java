package br.com.sellerintelligence.security;

import br.com.sellerintelligence.config.TestSecurityConfig;
import br.com.sellerintelligence.identity.domain.User;
import br.com.sellerintelligence.identity.domain.UserRepository;
import br.com.sellerintelligence.tenancy.company.Company;
import br.com.sellerintelligence.tenancy.company.CompanyRepository;
import br.com.sellerintelligence.tenancy.company.TaxRegime;
import br.com.sellerintelligence.tenancy.store.StoreRepository;
import br.com.sellerintelligence.tenancy.domain.Membership;
import br.com.sellerintelligence.tenancy.domain.MembershipRepository;
import br.com.sellerintelligence.tenancy.domain.MembershipStatus;
import br.com.sellerintelligence.tenancy.domain.Organization;
import br.com.sellerintelligence.tenancy.domain.OrganizationRepository;
import br.com.sellerintelligence.tenancy.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
public class BenchmarkPerformanceTest {

    private static final Logger log = LoggerFactory.getLogger(BenchmarkPerformanceTest.class);
    private static final long MAX_P95_LATENCY_MS = 2500; // NFR-PER-001: p95 < 2.5s
    private static final int ITERATIONS = 100;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private StoreRepository storeRepository;

    private User user;
    private Organization org;

    @BeforeEach
    void setUp() {
        storeRepository.deleteAll();
        companyRepository.deleteAll();
        membershipRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(new User("sub-bench", "bench@seller.com", "Benchmark User"));
        org = organizationRepository.save(new Organization("Benchmark Org", "bench-org"));
        membershipRepository.save(new Membership(org, user, Role.OWNER, MembershipStatus.ACTIVE));

        companyRepository.save(new Company(
                org.getId(),
                "06.990.590/0001-23",
                "Benchmark Company",
                "Benchmark Legal",
                TaxRegime.SIMPLES_NACIONAL
        ));
    }

    @Test
    @DisplayName("Performance Benchmark: /actuator/health p95 latency < 2500 ms")
    void benchmarkHealthEndpointP95() throws Exception {
        List<Long> latencies = new ArrayList<>(ITERATIONS);

        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk());
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            latencies.add(durationMs);
        }

        LatencyReport report = computeReport("/actuator/health", latencies);
        logReport(report);
        assertThat(report.p95()).isLessThan(MAX_P95_LATENCY_MS);
    }

    @Test
    @DisplayName("Performance Benchmark: /api/v1/market/search p95 latency < 2500 ms")
    void benchmarkMarketSearchP95() throws Exception {
        String payload = """
                {
                    "query": "fone de ouvido bluetooth",
                    "marketplace": "MERCADO_LIVRE"
                }
                """;

        List<Long> latencies = new ArrayList<>(ITERATIONS);

        // Warmup
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/market/search")
                    .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                    .header("X-Tenant-Id", org.getId().toString())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload));
        }

        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            mockMvc.perform(post("/api/v1/market/search")
                            .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                            .header("X-Tenant-Id", org.getId().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk());
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            latencies.add(durationMs);
        }

        LatencyReport report = computeReport("/api/v1/market/search", latencies);
        logReport(report);
        assertThat(report.p95()).isLessThan(MAX_P95_LATENCY_MS);
    }

    @Test
    @DisplayName("Performance Benchmark: /api/v1/financial/simulate p95 latency < 2500 ms")
    void benchmarkFinancialSimulationP95() throws Exception {
        String payload = """
                {
                    "unitCogs": 45.00,
                    "packagingCost": 3.50,
                    "shippingSeller": 12.00,
                    "fixedFee": 6.00,
                    "fixedFeeThreshold": 79.00,
                    "commissionPercentage": 16.50,
                    "taxPercentage": 4.50,
                    "otherVariablePercentage": 2.00,
                    "trialPrice": 120.00
                }
                """;

        List<Long> latencies = new ArrayList<>(ITERATIONS);

        for (int i = 0; i < ITERATIONS; i++) {
            long start = System.nanoTime();
            mockMvc.perform(post("/api/v1/financial/simulate")
                            .with(jwt().jwt(b -> b.subject(user.getKeycloakSub()).claim("email", user.getEmail()).claim("name", user.getName())))
                            .header("X-Tenant-Id", org.getId().toString())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk());
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            latencies.add(durationMs);
        }

        LatencyReport report = computeReport("/api/v1/financial/simulate", latencies);
        logReport(report);
        assertThat(report.p95()).isLessThan(MAX_P95_LATENCY_MS);
    }

    private LatencyReport computeReport(String endpoint, List<Long> latencies) {
        Collections.sort(latencies);
        long min = latencies.getFirst();
        long max = latencies.getLast();
        double avg = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        long p50 = latencies.get((int) (latencies.size() * 0.50));
        long p90 = latencies.get((int) (latencies.size() * 0.90));
        long p95 = latencies.get((int) (latencies.size() * 0.95));
        long p99 = latencies.get((int) (latencies.size() * 0.99));
        return new LatencyReport(endpoint, latencies.size(), min, max, avg, p50, p90, p95, p99);
    }

    private void logReport(LatencyReport r) {
        log.info("BENCHMARK [{}] -> n={}, min={}ms, avg={:.1f}ms, p50={}ms, p90={}ms, p95={}ms, p99={}ms, max={}ms",
                r.endpoint(), r.samples(), r.min(), r.avg(), r.p50(), r.p90(), r.p95(), r.p99(), r.max());
    }

    public record LatencyReport(
            String endpoint,
            int samples,
            long min,
            long max,
            double avg,
            long p50,
            long p90,
            long p95,
            long p99
    ) {}
}
