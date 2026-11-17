package br.com.sellerintelligence.discovery.web;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record ReviewItemRequest(
        @NotBlank(message = "Nome do produto é obrigatório")
        String name,
        String supplierSku,
        String ean,
        String description,
        BigDecimal costPrice,
        BigDecimal weightKg,
        String dimensionsCm
) {}
