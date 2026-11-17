package br.com.sellerintelligence.financial.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ReturnOrderRequest(
        @NotNull(message = "Prejuízo de frete deve ser informado (zero se coberto pelo marketplace)")
        @PositiveOrZero(message = "Prejuízo de frete deve ser positivo ou zero")
        BigDecimal shippingLoss,

        @NotNull(message = "Prejuízo de embalagem deve ser informado")
        @PositiveOrZero(message = "Prejuízo de embalagem deve ser positivo ou zero")
        BigDecimal packagingLoss,

        @NotNull(message = "Prejuízo de mercadoria danificada (zero se produto voltou íntegro ao estoque)")
        @PositiveOrZero(message = "Prejuízo de mercadoria deve ser positivo ou zero")
        BigDecimal cogsLoss,

        String reason
) {}
