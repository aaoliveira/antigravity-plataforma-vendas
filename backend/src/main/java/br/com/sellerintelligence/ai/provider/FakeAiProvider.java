package br.com.sellerintelligence.ai.provider;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Profile({"test", "default", "dev"})
public class FakeAiProvider implements AiProvider {

    private static final String MODEL_NAME = "fake-model-v1";

    @Override
    public ListingAiOutput generateListingContent(
            String promptType,
            String systemPrompt,
            String userContextXml,
            String modelOverride
    ) {
        long startTime = System.currentTimeMillis();

        // 1. Simulação explícita de falha/indisponibilidade
        if (userContextXml != null && userContextXml.contains("SIMULATE_AI_UNAVAILABLE")) {
            throw new AiServiceUnavailableException("Provedor de inteligência artificial indisponível ou fora do ar (simulado).");
        }

        // 2. Extrai dados do XML sanitizado
        String title = extractTag(userContextXml, "title");
        String brand = extractTag(userContextXml, "brand");
        String category = extractTag(userContextXml, "category");

        Map<String, String> attributes = extractAttributes(userContextXml);
        if (brand.isBlank() && attributes.containsKey("marca")) {
            brand = attributes.get("marca");
        }

        Map<String, String> confirmedUsed = new LinkedHashMap<>();
        if (!brand.isBlank()) confirmedUsed.put("marca", brand);
        if (attributes.containsKey("modelo")) confirmedUsed.put("modelo", attributes.get("modelo"));
        if (attributes.containsKey("cor")) confirmedUsed.put("cor", attributes.get("cor"));
        if (attributes.containsKey("voltagem")) confirmedUsed.put("voltagem", attributes.get("voltagem"));

        List<String> missingAttributes = new ArrayList<>();
        if (!confirmedUsed.containsKey("voltagem")) missingAttributes.add("Voltagem (Bivolt/110V/220V)");
        if (!confirmedUsed.containsKey("cor")) missingAttributes.add("Cor Predominante");
        if (!confirmedUsed.containsKey("garantia")) missingAttributes.add("Prazo de Garantia");

        String resultText;
        String rationale;

        if ("TITLE".equalsIgnoreCase(promptType)) {
            // Gera título otimizado sem inventar dados
            StringBuilder sb = new StringBuilder();
            if (!category.isBlank()) sb.append(capitalize(category)).append(" ");
            else sb.append("Produto ");

            if (!brand.isBlank()) sb.append(brand).append(" ");
            if (confirmedUsed.containsKey("modelo")) sb.append(confirmedUsed.get("modelo")).append(" ");
            if (confirmedUsed.containsKey("cor")) sb.append(confirmedUsed.get("cor")).append(" ");

            String candidate = sb.toString().trim();
            if (candidate.length() < 40) {
                candidate = candidate + " Original com Nota Fiscal";
            }
            if (candidate.length() > 60) {
                candidate = candidate.substring(0, 60).trim();
            }

            resultText = candidate;
            rationale = "Título estruturado com tipo de produto, marca e modelo confirmados, ajustado para extensão ideal entre 40 e 60 caracteres sem termos promocionais proibidos.";
        } else {
            // Gera descrição estruturada com ficha técnica confirmada
            StringBuilder sb = new StringBuilder();
            sb.append("## Apresentação do Produto\n");
            sb.append("Apresentamos o ").append(title.isBlank() ? "produto" : title).append(".\n\n");

            sb.append("## Ficha Técnica Confirmada\n");
            for (Map.Entry<String, String> entry : confirmedUsed.entrySet()) {
                sb.append("- ").append(capitalize(entry.getKey())).append(": ").append(entry.getValue()).append("\n");
            }
            sb.append("\n## Informações Importantes\n");
            sb.append("- Produto novo e original com procedência garantida.\n");
            sb.append("- Emitimos Nota Fiscal para todas as vendas.\n");

            resultText = sb.toString();
            rationale = "Descrição técnica dividida em tópicos, contendo exclusivamente os atributos confirmados e seções padronizadas de garantia e procedência.";
        }

        long latency = System.currentTimeMillis() - startTime;
        return new ListingAiOutput(
                resultText,
                confirmedUsed,
                missingAttributes,
                rationale,
                modelOverride != null ? modelOverride : MODEL_NAME,
                latency
        );
    }

    @Override
    public String getProviderName() {
        return "FakeAiProvider";
    }

    private String extractTag(String xml, String tag) {
        if (xml == null) return "";
        Pattern p = Pattern.compile("<" + tag + ">(.*?)</" + tag + ">", Pattern.DOTALL);
        Matcher m = p.matcher(xml);
        return m.find() ? m.group(1).trim() : "";
    }

    private Map<String, String> extractAttributes(String xml) {
        Map<String, String> map = new HashMap<>();
        if (xml == null) return map;
        Pattern p = Pattern.compile("<attribute name=\"([^\"]+)\">(.*?)</attribute>");
        Matcher m = p.matcher(xml);
        while (m.find()) {
            map.put(m.group(1).toLowerCase().trim(), m.group(2).trim());
        }
        return map;
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) return "";
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
}
