package com.sellerintelligence.modules.marketintelligence.service;

import com.sellerintelligence.core.exception.BusinessRuleException;
import com.sellerintelligence.modules.marketintelligence.model.CreateObservationRequest;
import com.sellerintelligence.modules.marketintelligence.model.MarketObservationSourceType;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Set;

/**
 * Validador de conformidade anti-alucinação e governança de dados para observações de mercado.
 * Impõe:
 * 1. Proibição absoluta de criação de estimativas de vendas, visitas, CTR ou receita sem fonte e metodologia.
 * 2. Exigência estrita de metodologia e versionamento para qualquer observação classificada como ESTIMATED.
 * 3. Validação de URLs e defesa contra SSRF e fontes não autorizadas.
 */
@Component
public class MarketObservationValidator {

    private static final Set<String> ALLOWED_DOMAINS = Set.of(
            "mercadolivre.com.br",
            "mercadolibre.com",
            "shopee.com.br",
            "amazon.com.br",
            "magazineluiza.com.br",
            "casasbahia.com.br",
            "americanas.com.br",
            "sellerintelligence.com.br",
            "localhost"
    );

    public void validate(CreateObservationRequest request) {
        // 1. Regra Anti-Alucinação: Proibição de métricas sem fonte e metodologia documentada
        boolean hasEstimatedMetrics = request.salesVolume() != null
                || request.visitsCount() != null
                || request.ctr() != null
                || request.revenue() != null;

        if (hasEstimatedMetrics) {
            boolean hasMethodology = request.methodologyDescription() != null && !request.methodologyDescription().isBlank();
            boolean hasModelVersion = request.modelVersion() != null && !request.modelVersion().isBlank();
            boolean hasTimeWindow = request.timeWindow() != null && !request.timeWindow().isBlank();

            if (!hasMethodology || !hasModelVersion || !hasTimeWindow) {
                throw new BusinessRuleException(
                        "Valores de vendas, visitas, CTR ou faturamento são proibidos sem fonte verificável e metodologia documentada."
                );
            }
        }

        // 2. Regra para observações do tipo ESTIMATED
        if (request.sourceType() == MarketObservationSourceType.ESTIMATED) {
            if (request.methodologyDescription() == null || request.methodologyDescription().isBlank()
                    || request.modelVersion() == null || request.modelVersion().isBlank()
                    || request.timeWindow() == null || request.timeWindow().isBlank()
                    || request.confidence() == null) {
                throw new BusinessRuleException(
                        "Observações do tipo ESTIMATED exigem metodologia documentada, versão do modelo, janela temporal e índice de confiança."
                );
            }
        }

        // 3. Validação de URL de origem e defesa contra SSRF
        validateSourceUrl(request.sourceUrl());
    }

    private void validateSourceUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("A URL de origem é obrigatória.");
        }

        try {
            URI uri = URI.create(rawUrl.trim());
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("Host inválido na URL de origem: " + rawUrl);
            }

            // Defesa SSRF: bloquear endpoints de metadados de nuvem e faixas de IP privadas
            String lowerHost = host.toLowerCase();
            if (lowerHost.equals("169.254.169.254") || lowerHost.startsWith("10.")
                    || lowerHost.startsWith("192.168.") || lowerHost.startsWith("172.16.")
                    || lowerHost.equals("metadata.google.internal")) {
                throw new BusinessRuleException("Endereços de rede interna ou metadados de nuvem são proibidos como source_url.");
            }

            // Valida se o domínio pertence aos canais autorizados ou catálogo de e-commerce
            boolean isAllowed = ALLOWED_DOMAINS.stream().anyMatch(lowerHost::endsWith);
            if (!isAllowed) {
                throw new BusinessRuleException("O domínio informado na source_url (" + host + ") não está na lista de fontes de mercado autorizadas.");
            }
        } catch (Exception e) {
            if (e instanceof BusinessRuleException || e instanceof IllegalArgumentException) {
                throw e;
            }
            throw new IllegalArgumentException("URL de origem malformada: " + e.getMessage());
        }
    }
}
