package br.com.sellerintelligence.ai.engine;

import br.com.sellerintelligence.ai.dto.CriterionEvaluationDto;
import br.com.sellerintelligence.ai.dto.QualityAuditResultDto;
import br.com.sellerintelligence.sync.domain.Listing;
import br.com.sellerintelligence.sync.domain.Product;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Component
public class ListingAuditEngine {

    private static final List<String> PROHIBITED_SPAM_WORDS = List.of(
            "frete grátis", "frete gratis", "promoção", "promocao", "oferta", "desconto",
            "imperdível", "imperdivel", "o melhor", "melhor do mercado", "barato", "queima de estoque"
    );

    public QualityAuditResultDto auditListing(
            Listing listing,
            Product product,
            String fullDescription,
            Map<String, String> attributes,
            int imageCount
    ) {
        List<CriterionEvaluationDto> criteria = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        List<String> missingAttributes = new ArrayList<>();

        String title = listing.getTitle() != null ? listing.getTitle().trim() : "";
        String brand = product != null && product.getBrand() != null ? product.getBrand() : extractAttribute(attributes, "brand", "marca");
        String model = extractAttribute(attributes, "model", "modelo");
        String description = fullDescription != null ? fullDescription.trim() : "";

        // ==========================================
        // 1. Pilar Título (Máx 25 Pontos)
        // ==========================================
        int titlePoints = 0;

        // 1.1 Comprimento (10 pts)
        int titleLen = title.length();
        if (titleLen >= 40 && titleLen <= 60) {
            titlePoints += 10;
            criteria.add(new CriterionEvaluationDto(
                    "TITLE_LENGTH", "Comprimento do Título", 10, 10, 10, "GOOD",
                    "Título possui " + titleLen + " caracteres (faixa ideal: 40 a 60).",
                    "Mantenha essa faixa de caracteres para melhor visualização em telas mobile e desktop."
            ));
        } else if (titleLen >= 25 && titleLen <= 75) {
            titlePoints += 5;
            criteria.add(new CriterionEvaluationDto(
                    "TITLE_LENGTH", "Comprimento do Título", 10, 5, 10, "NEEDS_IMPROVEMENT",
                    "Título possui " + titleLen + " caracteres (fora da faixa ideal de 40 a 60).",
                    "Ajuste o tamanho do título para ficar entre 40 e 60 caracteres sem cortar termos importantes."
            ));
            recommendations.add("Ajuste o comprimento do título para 40 a 60 caracteres (atualmente com " + titleLen + ").");
        } else {
            titlePoints += 2;
            criteria.add(new CriterionEvaluationDto(
                    "TITLE_LENGTH", "Comprimento do Título", 10, 2, 10, "CRITICAL",
                    "Título muito curto ou excessivo (" + titleLen + " caracteres).",
                    "Reescreva o título para conter entre 40 e 60 caracteres descrevendo produto + marca + modelo."
            ));
            recommendations.add("Título com tamanho inadequado (" + titleLen + " caracteres). Reescreva com foco nas palavras-chave principais.");
        }

        // 1.2 Marca no título (5 pts)
        if (brand != null && !brand.isBlank() && containsIgnoreCase(title, brand)) {
            titlePoints += 5;
            criteria.add(new CriterionEvaluationDto(
                    "TITLE_BRAND", "Marca no Título", 5, 5, 5, "GOOD",
                    "A marca '" + brand + "' está presente no título.",
                    "Excelente inclusão da marca oficial."
            ));
        } else {
            criteria.add(new CriterionEvaluationDto(
                    "TITLE_BRAND", "Marca no Título", 5, 0, 5, "NEEDS_IMPROVEMENT",
                    brand != null ? "A marca '" + brand + "' não consta no título." : "Marca não cadastrada no produto.",
                    "Inclua a marca do produto no título logo após o tipo de produto."
            ));
            recommendations.add("Inclua a marca oficial no título para aumentar o índice de cliques (CTR).");
        }

        // 1.3 Modelo no título (5 pts)
        if (model != null && !model.isBlank() && containsIgnoreCase(title, model)) {
            titlePoints += 5;
            criteria.add(new CriterionEvaluationDto(
                    "TITLE_MODEL", "Modelo no Título", 5, 5, 5, "GOOD",
                    "O modelo '" + model + "' consta no título.",
                    "O modelo facilita a indexação por compradores com alta intenção de compra."
            ));
        } else {
            criteria.add(new CriterionEvaluationDto(
                    "TITLE_MODEL", "Modelo no Título", 5, 0, 5, "NEEDS_IMPROVEMENT",
                    "Modelo específico do produto ausente no título.",
                    "Adicione o código ou nome do modelo exato do produto no título."
            ));
            recommendations.add("Adicione o modelo exato do produto no título.");
        }

        // 1.4 Termos proibidos / spam comercial (5 pts)
        boolean hasSpam = false;
        String spamFound = "";
        for (String spamWord : PROHIBITED_SPAM_WORDS) {
            if (containsIgnoreCase(title, spamWord)) {
                hasSpam = true;
                spamFound = spamWord;
                break;
            }
        }
        if (title.contains("!!!") || title.contains("***")) {
            hasSpam = true;
            spamFound = "pontuação excessiva";
        }

        if (!hasSpam) {
            titlePoints += 5;
            criteria.add(new CriterionEvaluationDto(
                    "TITLE_NO_SPAM", "Ausência de Termos Proibidos", 5, 5, 5, "GOOD",
                    "Nenhum termo promocional ou proibido detectado no título.",
                    "O título está em conformidade com as diretrizes dos marketplaces."
            ));
        } else {
            criteria.add(new CriterionEvaluationDto(
                    "TITLE_NO_SPAM", "Ausência de Termos Proibidos", 5, 0, 5, "CRITICAL",
                    "Termo impróprio detectado no título: '" + spamFound + "'.",
                    "Remova palavras como 'promoção', 'frete grátis' ou pontuações apelativas do título."
            ));
            recommendations.add("Remova termos promocionais do título ('" + spamFound + "') para evitar penalização pelo algoritmo.");
        }

        // ==========================================
        // 2. Pilar Descrição (Máx 25 Pontos)
        // ==========================================
        int descPoints = 0;
        int descLen = description.length();

        // 2.1 Presença de Descrição (5 pts)
        if (descLen > 0) {
            descPoints += 5;
            criteria.add(new CriterionEvaluationDto(
                    "DESC_PRESENCE", "Presença de Descrição", 5, 5, 5, "GOOD",
                    "Anúncio possui descrição cadastrada.", "Boa prática atendida."
            ));
        } else {
            criteria.add(new CriterionEvaluationDto(
                    "DESC_PRESENCE", "Presença de Descrição", 5, 0, 5, "CRITICAL",
                    "Anúncio não possui descrição.", "Cadastre uma descrição detalhada imediatamente."
            ));
            recommendations.add("Cadastre uma descrição completa para o anúncio.");
        }

        // 2.2 Extensão da Descrição (10 pts)
        if (descLen >= 150) {
            descPoints += 10;
            criteria.add(new CriterionEvaluationDto(
                    "DESC_LENGTH", "Extensão da Descrição", 10, 10, 10, "GOOD",
                    "Descrição com boa extensão (" + descLen + " caracteres).",
                    "Texto explicativo adequado para sanar dúvidas do comprador."
            ));
        } else if (descLen >= 50) {
            descPoints += 5;
            criteria.add(new CriterionEvaluationDto(
                    "DESC_LENGTH", "Extensão da Descrição", 10, 5, 10, "NEEDS_IMPROVEMENT",
                    "Descrição curta (" + descLen + " caracteres).",
                    "Expanda a descrição detalhando os diferenciais e funcionalidades do produto."
            ));
            recommendations.add("Enriqueça a descrição com mais detalhes (atualmente com apenas " + descLen + " caracteres).");
        } else if (descLen > 0) {
            criteria.add(new CriterionEvaluationDto(
                    "DESC_LENGTH", "Extensão da Descrição", 10, 0, 10, "CRITICAL",
                    "Descrição muito superficial (" + descLen + " caracteres).",
                    "Crie uma descrição completa com seções e ficha técnica."
            ));
        }

        // 2.3 Estrutura em tópicos / parágrafos (10 pts)
        boolean hasStructure = description.contains("\n") || description.contains("- ") || description.contains("• ");
        if (hasStructure && descLen >= 100) {
            descPoints += 10;
            criteria.add(new CriterionEvaluationDto(
                    "DESC_STRUCTURE", "Estrutura e Legibilidade", 10, 10, 10, "GOOD",
                    "Descrição estruturada com parágrafos ou tópicos organizados.",
                    "Excelente formatação visual para leitura rápida."
            ));
        } else if (descLen > 0) {
            descPoints += 3;
            criteria.add(new CriterionEvaluationDto(
                    "DESC_STRUCTURE", "Estrutura e Legibilidade", 10, 3, 10, "NEEDS_IMPROVEMENT",
                    "Descrição em bloco único de texto.",
                    "Divida o texto em tópicos (Apresentação, Ficha Técnica, Itens Inclusos)."
            ));
            recommendations.add("Divida a descrição em seções organizadas em tópicos para facilitar a leitura no smartphone.");
        }

        // ==========================================
        // 3. Pilar Atributos / Ficha Técnica (Máx 30 Pontos)
        // ==========================================
        int attrPoints = 0;
        String[] requiredAttributes = {"marca", "modelo", "condicao", "garantia", "cor"};
        Map<String, String> lowerCaseAttrs = new HashMap<>();
        if (attributes != null) {
            attributes.forEach((k, v) -> lowerCaseAttrs.put(k.toLowerCase().trim(), v));
        }
        if (product != null && product.getBrand() != null) {
            lowerCaseAttrs.put("marca", product.getBrand());
        }

        for (String reqAttr : requiredAttributes) {
            String val = lowerCaseAttrs.get(reqAttr);
            if (val != null && !val.isBlank()) {
                attrPoints += 6;
            } else {
                missingAttributes.add(capitalize(reqAttr));
            }
        }

        if (attrPoints >= 24) {
            criteria.add(new CriterionEvaluationDto(
                    "TECH_ATTRIBUTES", "Ficha Técnica e Atributos", 30, attrPoints, 30, "GOOD",
                    (attrPoints / 6) + " de 5 atributos essenciais preenchidos na ficha técnica.",
                    "Ficha técnica rica melhora os filtros de busca da plataforma."
            ));
        } else if (attrPoints >= 12) {
            criteria.add(new CriterionEvaluationDto(
                    "TECH_ATTRIBUTES", "Ficha Técnica e Atributos", 30, attrPoints, 30, "NEEDS_IMPROVEMENT",
                    (attrPoints / 6) + " de 5 atributos preenchidos. Faltam: " + String.join(", ", missingAttributes) + ".",
                    "Preencha os atributos obrigatórios restantes para não perder posições no ranqueamento."
            ));
            recommendations.add("Preencha os atributos faltantes da ficha técnica: " + String.join(", ", missingAttributes) + ".");
        } else {
            criteria.add(new CriterionEvaluationDto(
                    "TECH_ATTRIBUTES", "Ficha Técnica e Atributos", 30, attrPoints, 30, "CRITICAL",
                    "Ficha técnica praticamente vazia. Atributos faltantes: " + String.join(", ", missingAttributes) + ".",
                    "Complete a ficha técnica para que o anúncio apareça nos filtros laterais do marketplace."
            ));
            recommendations.add("Ficha técnica incompleta. Preencha urgentemente: " + String.join(", ", missingAttributes) + ".");
        }

        // ==========================================
        // 4. Pilar Fotos e Imagens (Máx 20 Pontos)
        // ==========================================
        int imgPoints = 0;
        int effectiveImageCount = imageCount > 0 ? imageCount : (listing.getThumbnailUrl() != null && !listing.getThumbnailUrl().isBlank() ? 1 : 0);

        if (effectiveImageCount >= 4) {
            imgPoints = 20;
            criteria.add(new CriterionEvaluationDto(
                    "LISTING_IMAGES", "Galeria de Fotos", 20, 20, 20, "GOOD",
                    "Anúncio possui " + effectiveImageCount + " fotos na galeria.",
                    "Excelente quantidade de imagens para demonstração do produto."
            ));
        } else if (effectiveImageCount >= 2) {
            imgPoints = 12;
            criteria.add(new CriterionEvaluationDto(
                    "LISTING_IMAGES", "Galeria de Fotos", 20, 12, 20, "NEEDS_IMPROVEMENT",
                    "Anúncio possui apenas " + effectiveImageCount + " foto(s).",
                    "Adicione pelo menos 4 a 6 fotos mostrando diferentes ângulos e embalagem."
            ));
            recommendations.add("Adicione mais fotos ao anúncio (recomendado: 4 ou mais ângulos detalhados).");
        } else if (effectiveImageCount == 1) {
            imgPoints = 6;
            criteria.add(new CriterionEvaluationDto(
                    "LISTING_IMAGES", "Galeria de Fotos", 20, 6, 20, "CRITICAL",
                    "Apenas 1 foto cadastrada.",
                    "Anúncios com foto única sofrem rejeição alta de compradores. Adicione mais fotos reais."
            ));
            recommendations.add("Adicione pelo menos mais 3 fotos ao anúncio para aumentar a taxa de conversão.");
        } else {
            criteria.add(new CriterionEvaluationDto(
                    "LISTING_IMAGES", "Galeria de Fotos", 20, 0, 20, "CRITICAL",
                    "Nenhuma foto cadastrada.",
                    "Anúncios sem fotos têm visibilidade nula nos marketplaces."
            ));
            recommendations.add("Cadastre fotos do produto em alta resolução com fundo branco.");
        }

        int overallScore = titlePoints + descPoints + attrPoints + imgPoints;
        if (overallScore > 100) overallScore = 100;

        return new QualityAuditResultDto(
                listing.getId(),
                overallScore,
                titlePoints,
                descPoints,
                attrPoints,
                imgPoints,
                criteria,
                recommendations,
                missingAttributes,
                Instant.now()
        );
    }

    private boolean containsIgnoreCase(String source, String target) {
        if (source == null || target == null) return false;
        return source.toLowerCase().contains(target.toLowerCase());
    }

    private String extractAttribute(Map<String, String> attributes, String... keys) {
        if (attributes == null) return null;
        for (String k : keys) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(k)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
}
