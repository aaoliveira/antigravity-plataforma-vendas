package br.com.sellerintelligence;

import br.com.sellerintelligence.infrastructure.logging.CorrelationIdFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SellerIntelligenceApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Context loads successfully")
    void contextLoads() {
        assertThat(restTemplate).isNotNull();
    }

    @Test
    @DisplayName("Actuator health endpoint returns UP status")
    void actuatorHealthReturnsUp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("Actuator info endpoint responds OK")
    void actuatorInfoRespondsOk() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/info", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("CorrelationIdFilter generates and propagates correlation ID")
    void correlationIdPropagated() {
        HttpHeaders headers = new HttpHeaders();
        String testId = "test-corr-12345";
        headers.set(CorrelationIdFilter.CORRELATION_ID_HEADER, testId);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange("/actuator/health", HttpMethod.GET, request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER)).isEqualTo(testId);
    }

    @Test
    @DisplayName("CorrelationIdFilter generates new UUID if header not provided")
    void correlationIdGeneratedWhenMissing() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String header = response.getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(header).isNotBlank();
    }
}
