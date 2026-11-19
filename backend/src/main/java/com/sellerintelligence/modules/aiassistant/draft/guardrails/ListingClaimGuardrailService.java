package com.sellerintelligence.modules.aiassistant.draft.guardrails;

import com.sellerintelligence.modules.aiassistant.draft.model.ClaimValidationResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motor de validação de claims e rastreabilidade de atributos confirmados (P020-BE).
 * Aplica os guardrails anti-alucinação da Seller Intelligence Platform:
 * - Proíbe expressamente inventar certificações, compatibilidades, materiais nobres,
 *   dimensões numéricas, garantias e superlativos enganosos.
 */
@Service
public class ListingClaimGuardrailService {

    // 1. Padrões de Certificações Proibidas quando não confirmadas
    private static final Pattern CERTIFICATION_PATTERN = Pattern.compile(
            "(?i)\\b(inmetro|anvisa|anatel|iso\\s*\\d{3,5}|ce\\b|fda\\b|selo\\s+procel|procel|abnt)\\b"
    );

    // 2. Padrões de Compatibilidade com Terceiros
    private static final Pattern COMPATIBILITY_PATTERN = Pattern.compile(
            "(?i)\\b(compat[ií]vel\\s+com|serve\\s+(?:para|no|na)|funciona\\s+(?:em|com))\\s+([A-Za-z0-9_\\-\\s]{3,30})"
    );

    // 3. Padrões de Materiais Nobres/Específicos
    private static final Pattern MATERIAL_PATTERN = Pattern.compile(
            "(?i)\\b(ouro(?:\\s+18k|\\s+24k)?|prata\\s+925|tit[aâ]nio|couro\\s+leg[ií]timo|a[çc]o\\s+cir[uú]rgico|blindad[oa]|fibra\\s+de\\s+carbono|100%\\s+algod[aã]o)\\b"
    );

    // 4. Padrões de Garantias
    private static final Pattern WARRANTY_PATTERN = Pattern.compile(
            "(?i)\\b(garantia\\s+vital[ií]cia|garantia\\s+de\\s+\\d+\\s*(?:anos?|meses?)|garantia\\s+eterna)\\b"
    );

    // 5. Padrões de Superlativos Enganosos / Alegações Médicas ou Absolutas
    private static final Pattern DECEPTIVE_SUPERLATIVES_PATTERN = Pattern.compile(
            "(?i)\\b(o\\s+melhor\\s+do\\s+(?:mundo|brasil|mercado)|cura\\b|milagros[oa]|infal[ií]vel|100%\\s+(?:eficaz|garantido|seguro)|[uú]nico\\s+aprovado|sem\\s+defeitos?|milagre)\\b"
    );

    // 6. Dimensões e Pesos Numéricos Específicos
    private static final Pattern NUMERIC_MEASUREMENT_PATTERN = Pattern.compile(
            "(?i)\\b(\\d+(?:[\\.,]\\d+)?\\s*(?:cm|mm|metros?|kg|gramas?|litros?|ml))\\b"
    );

    public ClaimValidationResult validateAndTraceClaims(
            String title,
            String description,
            Map<String, Object> confirmedAttributes,
            List<String> keywords
    ) {
        String fullText = ((title != null ? title : "") + " " + (description != null ? description : "")).trim();
        String fullTextLower = fullText.toLowerCase(Locale.ROOT);

        List<String> unprovenClaims = new ArrayList<>();
        Map<String, String> traceableAttributes = new LinkedHashMap<>();

        // Coletar todo texto comprovado em minúsculas para verificação de contenção
        String confirmedContext = extractConfirmedContext(confirmedAttributes, keywords).toLowerCase(Locale.ROOT);

        // 1. Auditoria de Certificações
        Matcher certMatcher = CERTIFICATION_PATTERN.matcher(fullText);
        while (certMatcher.find()) {
            String cert = certMatcher.group(1);
            if (!confirmedContext.contains(cert.toLowerCase(Locale.ROOT))) {
                unprovenClaims.add("Certificação não comprovada detectada: '" + cert + "'");
            }
        }

        // 2. Auditoria de Compatibilidade com Terceiros
        Matcher compMatcher = COMPATIBILITY_PATTERN.matcher(fullText);
        while (compMatcher.find()) {
            String target = compMatcher.group(2).trim();
            if (!confirmedContext.contains(target.toLowerCase(Locale.ROOT))) {
                unprovenClaims.add("Compatibilidade não confirmada detectada: '" + compMatcher.group(0).trim() + "'");
            }
        }

        // 3. Auditoria de Materiais Nobres/Específicos
        Matcher matMatcher = MATERIAL_PATTERN.matcher(fullText);
        while (matMatcher.find()) {
            String mat = matMatcher.group(1);
            if (!confirmedContext.contains(mat.toLowerCase(Locale.ROOT))) {
                unprovenClaims.add("Material não comprovado nos atributos do produto: '" + mat + "'");
            }
        }

        // 4. Auditoria de Garantias
        Matcher warMatcher = WARRANTY_PATTERN.matcher(fullText);
        while (warMatcher.find()) {
            String war = warMatcher.group(1);
            if (!confirmedContext.contains(war.toLowerCase(Locale.ROOT))) {
                unprovenClaims.add("Garantia não confirmada detectada: '" + war + "'");
            }
        }

        // 5. Auditoria de Superlativos Enganosos
        Matcher supMatcher = DECEPTIVE_SUPERLATIVES_PATTERN.matcher(fullText);
        while (supMatcher.find()) {
            String sup = supMatcher.group(1);
            unprovenClaims.add("Superlativo ou alegação enganosa proibida: '" + sup + "'");
        }

        // 6. Auditoria de Dimensões / Medidas Numéricas
        Matcher numMatcher = NUMERIC_MEASUREMENT_PATTERN.matcher(fullText);
        while (numMatcher.find()) {
            String measurement = numMatcher.group(1);
            if (!confirmedContext.contains(measurement.toLowerCase(Locale.ROOT))) {
                unprovenClaims.add("Dimensão ou peso numérico não comprovado: '" + measurement + "'");
            }
        }

        // Mapear Rastreabilidade de Atributos Confirmados
        traceConfirmedAttributes(fullTextLower, confirmedAttributes, keywords, traceableAttributes);

        boolean isValid = unprovenClaims.isEmpty();
        return new ClaimValidationResult(
                isValid,
                Collections.unmodifiableList(unprovenClaims),
                Collections.unmodifiableMap(traceableAttributes),
                title != null ? title.trim() : "",
                description != null ? description.trim() : ""
        );
    }

    private void traceConfirmedAttributes(
            String textLower,
            Map<String, Object> confirmedAttributes,
            List<String> keywords,
            Map<String, String> traceableAttributes
    ) {
        if (confirmedAttributes != null) {
            for (Map.Entry<String, Object> entry : confirmedAttributes.entrySet()) {
                if (entry.getValue() == null) continue;
                Object val = entry.getValue();
                if (val instanceof List<?> list) {
                    for (Object item : list) {
                        if (item != null) {
                            String itemStr = item.toString().trim();
                            if (!itemStr.isEmpty() && textLower.contains(itemStr.toLowerCase(Locale.ROOT))) {
                                traceableAttributes.put(entry.getKey() + ":" + itemStr, itemStr);
                            }
                        }
                    }
                } else if (val instanceof Map<?, ?> map) {
                    for (Map.Entry<?, ?> me : map.entrySet()) {
                        String k = me.getKey().toString();
                        String v = me.getValue() != null ? me.getValue().toString() : "";
                        if (!v.isEmpty() && textLower.contains(v.toLowerCase(Locale.ROOT))) {
                            traceableAttributes.put(entry.getKey() + "." + k, v);
                        }
                    }
                } else {
                    String valStr = val.toString().trim();
                    if (!valStr.isEmpty()) {
                        boolean matched = textLower.contains(valStr.toLowerCase(Locale.ROOT));
                        if (!matched && valStr.contains(" ")) {
                            String[] words = valStr.toLowerCase(Locale.ROOT).split("\\s+");
                            int found = 0;
                            int totalSig = 0;
                            for (String w : words) {
                                if (w.length() >= 3 && !List.of("de", "do", "da", "em", "para", "com", "conforme").contains(w)) {
                                    totalSig++;
                                    if (textLower.contains(w)) found++;
                                }
                            }
                            if (totalSig > 0 && found >= totalSig) {
                                matched = true;
                            }
                        }
                        if (matched) {
                            traceableAttributes.put(entry.getKey(), valStr);
                        }
                    }
                }
            }
        }

        if (keywords != null) {
            for (String kw : keywords) {
                if (kw != null && !kw.trim().isEmpty() && textLower.contains(kw.trim().toLowerCase(Locale.ROOT))) {
                    traceableAttributes.put("keyword:" + kw.trim(), kw.trim());
                }
            }
        }
    }

    private String extractConfirmedContext(Map<String, Object> confirmedAttributes, List<String> keywords) {
        StringBuilder sb = new StringBuilder();
        if (confirmedAttributes != null) {
            for (Object val : confirmedAttributes.values()) {
                if (val != null) {
                    sb.append(" ").append(val);
                }
            }
        }
        if (keywords != null) {
            for (String kw : keywords) {
                if (kw != null) {
                    sb.append(" ").append(kw);
                }
            }
        }
        return sb.toString();
    }
}
