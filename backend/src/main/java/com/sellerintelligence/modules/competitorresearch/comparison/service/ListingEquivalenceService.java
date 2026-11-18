package com.sellerintelligence.modules.competitorresearch.comparison.service;

import com.sellerintelligence.modules.competitorresearch.comparison.model.ComparableListingItem;
import com.sellerintelligence.modules.competitorresearch.comparison.model.CompatibilityStatus;
import com.sellerintelligence.modules.competitorresearch.comparison.model.EquivalenceScoreBreakdown;
import com.sellerintelligence.modules.competitorresearch.comparison.model.ItemCondition;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Motor determinístico de análise de equivalência e compatibilidade entre listings.
 * Cumpre os critérios de aceite:
 * - Score e critérios explicáveis (GTIN, marca, modelo, título e atributos técnicos);
 * - Não comparar itens incompatíveis sem aviso (sinalização estrita de incompatibilidade);
 * - Cálculo reproduzível sem números aleatórios ou heurísticas opacas.
 */
@Service
public class ListingEquivalenceService {

    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("0.7500");
    private static final BigDecimal BORDERLINE_THRESHOLD = new BigDecimal("0.5000");

    private static final Set<String> STOPWORDS = Set.of(
            "de", "do", "da", "dos", "das", "em", "no", "na", "nos", "nas",
            "com", "para", "por", "e", "ou", "a", "o", "os", "as", "um",
            "uma", "uns", "umas", "sem", "sob", "sobre", "kit", "combo", "promocao"
    );

    private static final Pattern VOLTAGE_PATTERN = Pattern.compile("\\b(110v|127v|220v|bivolt)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CAPACITY_PATTERN = Pattern.compile("\\b(\\d+\\s*(?:gb|tb|mb))\\b", Pattern.CASE_INSENSITIVE);

    public EquivalenceScoreBreakdown calculateEquivalence(
            ComparableListingItem base,
            ComparableListingItem target,
            BigDecimal requestedThreshold
    ) {
        Objects.requireNonNull(base, "Item base é obrigatório");
        Objects.requireNonNull(target, "Item alvo é obrigatório");

        BigDecimal threshold = (requestedThreshold != null) ? requestedThreshold : DEFAULT_THRESHOLD;
        List<String> incompatibilityReasons = new ArrayList<>();

        // 1. Verificação de Incompatibilidade Crítica
        checkIncompatibilities(base, target, incompatibilityReasons);
        boolean isCompatible = incompatibilityReasons.isEmpty();

        // 2. Score de GTIN / EAN
        BigDecimal gtinScore = computeGtinScore(base.gtin(), target.gtin(), incompatibilityReasons);
        boolean hasGtinMatch = gtinScore.compareTo(BigDecimal.ONE) == 0;

        // 3. Score de Marca
        BigDecimal brandScore = computeBrandScore(base.brand(), target.brand());

        // 4. Score de Modelo
        BigDecimal modelScore = computeModelScore(base, target);

        // 5. Score de Título (Jaccard normalizado)
        BigDecimal titleScore = computeTitleScore(base.title(), target.title());

        // 6. Score de Atributos Técnicos
        BigDecimal attributesScore = computeAttributesScore(base.attributes(), target.attributes());

        // 7. Cálculo do Score Geral Ponderado (BigDecimal)
        BigDecimal overallScore;
        if (hasGtinMatch) {
            // Com GTIN comprovado: GTIN 35%, Marca 25%, Modelo 20%, Atributos 10%, Título 10%
            overallScore = gtinScore.multiply(new BigDecimal("0.3500"))
                    .add(brandScore.multiply(new BigDecimal("0.2500")))
                    .add(modelScore.multiply(new BigDecimal("0.2000")))
                    .add(attributesScore.multiply(new BigDecimal("0.1000")))
                    .add(titleScore.multiply(new BigDecimal("0.1000")))
                    .setScale(4, RoundingMode.HALF_UP);
        } else {
            // Sem GTIN: Marca 35%, Modelo 30%, Atributos 20%, Título 15%
            overallScore = brandScore.multiply(new BigDecimal("0.3500"))
                    .add(modelScore.multiply(new BigDecimal("0.3000")))
                    .add(attributesScore.multiply(new BigDecimal("0.2000")))
                    .add(titleScore.multiply(new BigDecimal("0.1500")))
                    .setScale(4, RoundingMode.HALF_UP);
        }

        // 8. Determinação de Status de Compatibilidade e Equivalência
        CompatibilityStatus compatibilityStatus;
        if (!isCompatible) {
            compatibilityStatus = CompatibilityStatus.INCOMPATIBLE;
        } else if (overallScore.compareTo(threshold) >= 0) {
            compatibilityStatus = CompatibilityStatus.COMPATIBLE;
        } else if (overallScore.compareTo(BORDERLINE_THRESHOLD) >= 0) {
            compatibilityStatus = CompatibilityStatus.BORDERLINE;
        } else {
            compatibilityStatus = CompatibilityStatus.INCOMPATIBLE;
        }

        boolean isEquivalent = isCompatible && overallScore.compareTo(threshold) >= 0;

        // 9. Construção do Aviso e Explicação Textual
        String warningMessage = buildWarningMessage(isCompatible, compatibilityStatus, incompatibilityReasons);
        String explanation = buildExplanation(base, target, gtinScore, brandScore, modelScore,
                titleScore, attributesScore, overallScore, threshold, isEquivalent, isCompatible, incompatibilityReasons);

        return new EquivalenceScoreBreakdown(
                gtinScore,
                brandScore,
                modelScore,
                titleScore,
                attributesScore,
                overallScore,
                threshold,
                isEquivalent,
                isCompatible,
                compatibilityStatus,
                incompatibilityReasons,
                warningMessage,
                explanation
        );
    }

    private void checkIncompatibilities(
            ComparableListingItem base,
            ComparableListingItem target,
            List<String> reasons
    ) {
        // A. Condição incompatível (ex: Novo vs Usado/Recondicionado)
        if (base.condition() != ItemCondition.NOT_SPECIFIED &&
            target.condition() != ItemCondition.NOT_SPECIFIED &&
            base.condition() != target.condition()) {
            reasons.add("Condição divergente: o anúncio base é '" + base.condition() +
                        "' e o anúncio comparado é '" + target.condition() + "'.");
        }

        // B. Marcas Conflitantes
        String normBrand1 = normalizeText(base.brand());
        String normBrand2 = normalizeText(target.brand());
        if (!normBrand1.isEmpty() && !normBrand2.isEmpty() &&
            !normBrand1.equals("generico") && !normBrand2.equals("generico") &&
            !normBrand1.equals(normBrand2) &&
            !normBrand1.contains(normBrand2) && !normBrand2.contains(normBrand1)) {
            reasons.add("Marcas conflitantes: '" + base.brand() + "' vs '" + target.brand() + "'.");
        }

        // C. Voltagem Conflitante
        String v1 = extractVoltage(base);
        String v2 = extractVoltage(target);
        if (!v1.isEmpty() && !v2.isEmpty() && !v1.equals(v2) && !v1.equals("bivolt") && !v2.equals("bivolt")) {
            reasons.add("Especificação de voltagem incompatível: " + v1.toUpperCase() + " vs " + v2.toUpperCase() + ".");
        }

        // D. Capacidade / Armazenamento Conflitante
        String cap1 = extractCapacity(base);
        String cap2 = extractCapacity(target);
        if (!cap1.isEmpty() && !cap2.isEmpty() && !cap1.equalsIgnoreCase(cap2)) {
            reasons.add("Capacidade de armazenamento/memória divergente: " + cap1.toUpperCase() + " vs " + cap2.toUpperCase() + ".");
        }

        // E. Categorias Estruturalmente Conflitantes (Acessório vs Aparelho)
        if (isAccessoryVsDeviceConflict(base.title(), target.title())) {
            reasons.add("Incompatibilidade estrutural de tipo: um anúncio refere-se a um acessório (capa/película/cabo) e o outro ao aparelho principal.");
        }
    }

    private BigDecimal computeGtinScore(String gtin1, String gtin2, List<String> reasons) {
        String clean1 = cleanGtin(gtin1);
        String clean2 = cleanGtin(gtin2);

        if (!clean1.isEmpty() && !clean2.isEmpty()) {
            if (clean1.equals(clean2)) {
                return BigDecimal.valueOf(1.0000).setScale(4, RoundingMode.HALF_UP);
            } else {
                reasons.add("Códigos GTIN/EAN conflitantes e incompatíveis: '" + gtin1 + "' vs '" + gtin2 + "'.");
                return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
            }
        }
        return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal computeBrandScore(String brand1, String brand2) {
        String b1 = normalizeText(brand1);
        String b2 = normalizeText(brand2);

        if (b1.isEmpty() || b2.isEmpty() || b1.equals("generico") || b2.equals("generico")) {
            return new BigDecimal("0.5000"); // neutro/parcial quando não informada
        }
        if (b1.equals(b2) || b1.contains(b2) || b2.contains(b1)) {
            return new BigDecimal("1.0000");
        }
        return new BigDecimal("0.0000");
    }

    private BigDecimal computeModelScore(ComparableListingItem base, ComparableListingItem target) {
        String m1 = normalizeText(base.model());
        String m2 = normalizeText(target.model());

        if (!m1.isEmpty() && !m2.isEmpty()) {
            if (m1.equals(m2)) {
                return new BigDecimal("1.0000");
            }
            if (m1.contains(m2) || m2.contains(m1)) {
                return new BigDecimal("0.8000");
            }
            return new BigDecimal("0.1000");
        }

        // Extrai tokens significativos do título caso model não esteja informado
        Set<String> t1 = extractTokens(base.title());
        Set<String> t2 = extractTokens(target.title());
        double jaccard = calculateJaccard(t1, t2);
        return BigDecimal.valueOf(jaccard).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal computeTitleScore(String title1, String title2) {
        Set<String> tokens1 = extractTokens(title1);
        Set<String> tokens2 = extractTokens(title2);
        double jaccard = calculateJaccard(tokens1, tokens2);
        return BigDecimal.valueOf(jaccard).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal computeAttributesScore(Map<String, String> attr1, Map<String, String> attr2) {
        if (attr1 == null || attr2 == null || attr1.isEmpty() || attr2.isEmpty()) {
            return new BigDecimal("0.5000"); // score neutro se atributos técnicos não estiverem cadastrados
        }

        int commonKeys = 0;
        int matchingValues = 0;

        for (Map.Entry<String, String> entry : attr1.entrySet()) {
            String normKey = normalizeText(entry.getKey());
            for (Map.Entry<String, String> targetEntry : attr2.entrySet()) {
                if (normKey.equals(normalizeText(targetEntry.getKey()))) {
                    commonKeys++;
                    if (normalizeText(entry.getValue()).equals(normalizeText(targetEntry.getValue()))) {
                        matchingValues++;
                    }
                    break;
                }
            }
        }

        if (commonKeys == 0) {
            return new BigDecimal("0.5000");
        }

        return BigDecimal.valueOf((double) matchingValues / commonKeys).setScale(4, RoundingMode.HALF_UP);
    }

    private String buildWarningMessage(boolean isCompatible, CompatibilityStatus status, List<String> reasons) {
        if (!isCompatible) {
            return "AVISO DE INCOMPATIBILIDADE: Os anúncios comparados apresentam divergências técnicas ou estruturais e NÃO devem ser tratados como substitutos diretos: " +
                    String.join("; ", reasons);
        }
        if (status == CompatibilityStatus.BORDERLINE) {
            return "AVISO: Similaridade parcial (borderline). Verifique atentamente se os itens compartilham as mesmas especificações.";
        }
        return null;
    }

    private String buildExplanation(
            ComparableListingItem base,
            ComparableListingItem target,
            BigDecimal gtinScore,
            BigDecimal brandScore,
            BigDecimal modelScore,
            BigDecimal titleScore,
            BigDecimal attributesScore,
            BigDecimal overallScore,
            BigDecimal threshold,
            boolean isEquivalent,
            boolean isCompatible,
            List<String> reasons
    ) {
        StringBuilder sb = new StringBuilder();
        if (!isCompatible) {
            sb.append("Comparação reprovada por incompatibilidade crítica (").append(String.join(", ", reasons)).append("). ");
        } else if (isEquivalent) {
            sb.append("Itens considerados equivalentes (score ").append(overallScore).append(" >= limiar ").append(threshold).append("). ");
        } else {
            sb.append("Itens não atingiram o limiar mínimo de equivalência (score ").append(overallScore).append(" < limiar ").append(threshold).append("). ");
        }

        sb.append(String.format(java.util.Locale.ROOT, "Detalhamento: GTIN=%.4f, Marca=%.4f, Modelo=%.4f, Título=%.4f, Atributos=%.4f.",
                gtinScore, brandScore, modelScore, titleScore, attributesScore));

        return sb.toString();
    }

    private boolean isAccessoryVsDeviceConflict(String title1, String title2) {
        boolean t1Accessory = isAccessory(title1);
        boolean t2Accessory = isAccessory(title2);
        return t1Accessory != t2Accessory;
    }

    private boolean isAccessory(String title) {
        if (title == null) return false;
        String t = normalizeText(title);
        return t.contains("capa") || t.contains("capinha") || t.contains("pelicula") ||
               t.contains("case") || t.contains("suporte") || t.contains("cabo") || t.contains("adaptador");
    }

    private String extractVoltage(ComparableListingItem item) {
        if (item.attributes() != null) {
            for (Map.Entry<String, String> e : item.attributes().entrySet()) {
                if (normalizeText(e.getKey()).contains("voltagem") || normalizeText(e.getKey()).contains("tensao")) {
                    Matcher m = VOLTAGE_PATTERN.matcher(e.getValue());
                    if (m.find()) return m.group(1).toLowerCase();
                }
            }
        }
        if (item.title() != null) {
            Matcher m = VOLTAGE_PATTERN.matcher(item.title());
            if (m.find()) return m.group(1).toLowerCase();
        }
        return "";
    }

    private String extractCapacity(ComparableListingItem item) {
        if (item.attributes() != null) {
            for (Map.Entry<String, String> e : item.attributes().entrySet()) {
                String k = normalizeText(e.getKey());
                if (k.contains("capacidade") || k.contains("armazenamento") || k.contains("memoria")) {
                    Matcher m = CAPACITY_PATTERN.matcher(e.getValue());
                    if (m.find()) return m.group(1).replaceAll("\\s+", "").toLowerCase();
                }
            }
        }
        if (item.title() != null) {
            Matcher m = CAPACITY_PATTERN.matcher(item.title());
            if (m.find()) return m.group(1).replaceAll("\\s+", "").toLowerCase();
        }
        return "";
    }

    private String cleanGtin(String gtin) {
        if (gtin == null) return "";
        String clean = gtin.replaceAll("[^0-9]", "");
        return (clean.length() >= 8 && clean.length() <= 14) ? clean : "";
    }

    private String normalizeText(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private Set<String> extractTokens(String text) {
        if (text == null || text.isBlank()) return Collections.emptySet();
        String norm = normalizeText(text);
        return Arrays.stream(norm.split("\\s+"))
                .filter(token -> token.length() > 1 && !STOPWORDS.contains(token))
                .collect(Collectors.toSet());
    }

    private double calculateJaccard(Set<String> s1, Set<String> s2) {
        if (s1.isEmpty() && s2.isEmpty()) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(s1);
        intersection.retainAll(s2);

        Set<String> union = new HashSet<>(s1);
        union.addAll(s2);

        return (double) intersection.size() / union.size();
    }
}
