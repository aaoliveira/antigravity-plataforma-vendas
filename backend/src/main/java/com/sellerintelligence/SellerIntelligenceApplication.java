package com.sellerintelligence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Ponto de entrada do Monólito Modular da Seller Intelligence Platform.
 * Estruturado com Spring Modulith para validação contínua de limites de domínio.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SellerIntelligenceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SellerIntelligenceApplication.class, args);
    }
}
