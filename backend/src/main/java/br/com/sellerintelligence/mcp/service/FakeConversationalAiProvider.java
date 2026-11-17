package br.com.sellerintelligence.mcp.service;

import br.com.sellerintelligence.mcp.domain.ConversationalMessage;
import br.com.sellerintelligence.mcp.domain.McpToolDefinition;
import br.com.sellerintelligence.mcp.web.dto.McpToolCallRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Profile({"test", "default", "dev"})
public class FakeConversationalAiProvider implements ConversationalAiProvider {

    private static final Pattern SKU_PATTERN = Pattern.compile("(?i)sku\\s+([A-Z0-9_-]+)");
    private static final Pattern PRICE_PATTERN = Pattern.compile("(?i)r?\\$\\s*([0-9]+(?:[.,][0-9]{2})?)");

    @Override
    public AiIntentDecision analyzeAndDecide(
            String systemPrompt,
            List<ConversationalMessage> conversationHistory,
            String currentMessage,
            List<McpToolDefinition> availableTools,
            String modelOverride
    ) {
        String msg = currentMessage != null ? currentMessage.toLowerCase() : "";

        // 1. Detecção de Tentativa de Mutação
        if (msg.contains("alterar preço") || msg.contains("mudar preço") ||
            msg.contains("publicar anúncio") || msg.contains("criar anúncio") ||
            msg.contains("deletar pedido") || msg.contains("excluir anúncio") ||
            msg.contains("executar sql")) {
            return new AiIntentDecision(
                    "Operação mutável bloqueada",
                    Collections.emptyList(),
                    "Operações de escrita, alteração de preços ou publicação de anúncios não são permitidas via assistente conversacional/MCP. Essas ações exigem navegação e confirmação explícita na interface web com tela de prévia de impacto (ADR DEC-007 / DEC-023)."
            );
        }

        // 2. Vendas / Faturamento / Pedidos -> query_sales_indicators
        if (msg.contains("venda") || msg.contains("faturamento") || msg.contains("pedido") || msg.contains("ticket médio")) {
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("marketplace", "TODOS");
            return new AiIntentDecision(
                    "Consulta a indicadores agregados de vendas do tenant",
                    List.of(new McpToolCallRequest("query_sales_indicators", args)),
                    null
            );
        }

        // 3. Pesquisa de Mercado / Concorrentes -> search_market
        if (msg.contains("pesquis") || msg.contains("mercado") || msg.contains("concorrente") || msg.contains("buscar")) {
            String queryTerm = extractSearchTerm(currentMessage);
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("query", queryTerm.isBlank() ? "fone bluetooth" : queryTerm);
            args.put("marketplace", "MERCADO_LIVRE");
            args.put("limit", 5);
            return new AiIntentDecision(
                    "Pesquisa pública de mercado e tendências de concorrentes",
                    List.of(new McpToolCallRequest("search_market", args)),
                    null
            );
        }

        // 4. Margem / Preço Mínimo / Simulação -> calculate_sku_margin
        if (msg.contains("margem") || msg.contains("preço mínimo") || msg.contains("lucro") || msg.contains("simul")) {
            Matcher skuMatcher = SKU_PATTERN.matcher(currentMessage);
            String sku = skuMatcher.find() ? skuMatcher.group(1).toUpperCase() : "SKU-DEMO-01";

            Matcher priceMatcher = PRICE_PATTERN.matcher(currentMessage);
            double price = 120.00;
            if (priceMatcher.find()) {
                try {
                    price = Double.parseDouble(priceMatcher.group(1).replace(",", "."));
                } catch (Exception ignored) {}
            }

            Map<String, Object> args = new LinkedHashMap<>();
            args.put("sku", sku);
            args.put("salePrice", price);
            args.put("marketplace", "MERCADO_LIVRE");

            return new AiIntentDecision(
                    "Cálculo financeiro determinístico de margem de SKU",
                    List.of(new McpToolCallRequest("calculate_sku_margin", args)),
                    null
            );
        }

        // 5. Produtos Próprios / Estoque / Anúncios -> query_tenant_products
        if (msg.contains("produto") || msg.contains("meus anúncio") || msg.contains("estoque") || msg.contains("catálogo")) {
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("limit", 10);
            return new AiIntentDecision(
                    "Consulta ao catálogo de produtos e anúncios do tenant",
                    List.of(new McpToolCallRequest("query_tenant_products", args)),
                    null
            );
        }

        // 6. Conciliação / Repasse / Extrato -> query_reconciliation_summary
        if (msg.contains("conciliação") || msg.contains("repasse") || msg.contains("extrato") || msg.contains("divergência")) {
            Map<String, Object> args = new LinkedHashMap<>();
            return new AiIntentDecision(
                    "Consulta à conciliação financeira de repasses",
                    List.of(new McpToolCallRequest("query_reconciliation_summary", args)),
                    null
            );
        }

        // 7. Pergunta Geral / Saudação
        return new AiIntentDecision(
                "Resposta direta conversacional",
                Collections.emptyList(),
                "Olá! Sou o assistente de inteligência de vendas da Seller Intelligence Platform. Posso consultar indicadores de vendas, pesquisar tendências de mercado, simular margens financeiras determinísticas e consultar seus produtos ou repasses via ferramentas autorizadas."
        );
    }

    @Override
    public String synthesizeFinalReply(
            String systemPrompt,
            String userMessage,
            List<Object> toolResults,
            String modelOverride
    ) {
        if (toolResults == null || toolResults.isEmpty()) {
            return "Não foi possível obter dados suficientes no sistema para responder à sua solicitação. Os dados solicitados encontram-se atualmente INDISPONÍVEIS (UNAVAILABLE).";
        }

        StringBuilder reply = new StringBuilder();
        reply.append("Com base nos dados autorizados consultados em tempo real via ferramentas MCP:\n\n");

        for (Object res : toolResults) {
            if (res instanceof Map<?, ?> map) {
                if (map.containsKey("grossRevenue")) {
                    reply.append("📊 **Indicadores de Vendas (Últimos 30 Dias):**\n");
                    reply.append("- **Pedidos Confirmados:** ").append(map.get("totalOrders")).append(" pedidos\n");
                    reply.append("- **Faturamento Bruto:** R$ ").append(map.get("grossRevenue")).append("\n");
                    reply.append("- **Ticket Médio:** R$ ").append(map.get("averageTicket")).append("\n");
                    reply.append("- **Comissões e Tarifas Estimadas:** R$ ").append(map.get("estimatedMarketplaceFees")).append("\n\n");
                } else if (map.containsKey("contributionMargin")) {
                    reply.append("💰 **Análise de Margem para SKU ").append(map.get("sku")).append(":**\n");
                    reply.append("- **Preço de Venda Simulado:** R$ ").append(map.get("salePrice")).append("\n");
                    reply.append("- **Custo Unitário Registrado:** R$ ").append(map.get("unitCost")).append("\n");
                    reply.append("- **Comissão de Canal (16%):** -R$ ").append(map.get("commissionAmount")).append("\n");
                    reply.append("- **Estimativa Tributária (6%):** -R$ ").append(map.get("taxAmount")).append("\n");
                    reply.append("- **Frete Seller Estimado:** -R$ ").append(map.get("shippingCost")).append("\n");
                    reply.append("- **Margem de Contribuição:** R$ ").append(map.get("contributionMargin"))
                            .append(" (").append(map.get("marginPercentage")).append(")\n");
                    reply.append("- **Preço Mínimo de Equilíbrio (Margem Zero):** R$ ").append(map.get("minimumPriceTarget")).append("\n\n");
                } else if (map.containsKey("totalResults")) {
                    reply.append("🔍 **Pesquisa de Mercado (").append(map.get("marketplace")).append("):**\n");
                    reply.append("Encontrados ").append(map.get("totalResults")).append(" anúncios observados para o termo '").append(map.get("query")).append("'.\n\n");
                } else if (map.containsKey("totalSettlementBatches")) {
                    reply.append("🏦 **Resumo de Conciliação Financeira de Repasses:**\n");
                    reply.append("- **Lotes de Extratos:** ").append(map.get("totalSettlementBatches")).append("\n");
                    reply.append("- **Bruto Liquidado:** R$ ").append(map.get("totalGrossLiquidated")).append("\n");
                    reply.append("- **Líquido Efetivamente Creditado:** R$ ").append(map.get("totalNetReceived")).append("\n");
                    reply.append("- **Lançamentos Conciliados:** ").append(map.get("conciliatedItemsCount")).append("\n");
                    reply.append("- **Lançamentos com Divergência:** ").append(map.get("discrepantItemsCount")).append("\n\n");
                } else if (map.containsKey("totalFound")) {
                    reply.append("📦 **Catálogo de Produtos do Tenant:**\n");
                    reply.append("Total de ").append(map.get("totalFound")).append(" anúncios próprios localizados na base sincronizada.\n\n");
                }
            }
        }

        reply.append("> *Aviso de Rastreabilidade:* Todas as informações acima foram extraídas deterministicamente da base de dados e conectores oficiais do tenant. Não foram efetuadas deduções fictícias nem gerados comandos de alteração externa.");
        return reply.toString();
    }

    @Override
    public String getProviderName() {
        return "FakeConversationalAiProvider-v1";
    }

    private String extractSearchTerm(String message) {
        String cleaned = message.replaceAll("(?i)(pesquise|pesquisar|busque|buscar|no mercado livre|na shopee|por|sobre|itens|anúncios)", "").trim();
        return cleaned.replaceAll("[\"']", "");
    }
}
