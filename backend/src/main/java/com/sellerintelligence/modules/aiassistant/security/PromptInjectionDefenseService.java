package com.sellerintelligence.modules.aiassistant.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Serviço de Defesa em Profundidade contra Prompt Injection (Direto e Indireto).
 * Protege o assistente de IA contra ataques via títulos de anúncios, descrições,
 * avaliações de concorrentes e fontes externas não confiáveis.
 */
@Service
public class PromptInjectionDefenseService {

    private static final Logger log = LoggerFactory.getLogger(PromptInjectionDefenseService.class);

    public record ScanResult(boolean clean, String threatType, String offendingSnippet) {
        public static ScanResult safe() {
            return new ScanResult(true, null, null);
        }

        public static ScanResult threat(String type, String snippet) {
            return new ScanResult(false, type, snippet);
        }
    }

    private static final List<ThreatPattern> THREAT_PATTERNS = List.of(
            // 1. Tentativas de descarte ou sobrescrita de instruções de sistema
            new ThreatPattern("INSTRUCTION_OVERRIDE",
                    Pattern.compile("(?i)\\b(ignore|disregard|forget|bypass)\\s+(all\\s+)?(previous|prior|system)\\s+(instructions|prompts|rules|commands)\\b")),

            // 2. Sobrescrita explícita de modo de operação ou persona
            new ThreatPattern("JAILBREAK_PERSONA",
                    Pattern.compile("(?i)\\b(system\\s+prompt\\s+override|you\\s+are\\s+now\\s+(unrestricted|DAN|root|admin|in\\s+developer\\s+mode)|act\\s+as\\s+(unrestricted|an\\s+adversary))\\b")),

            // 3. Exfiltração de prompt de sistema ou segredos
            new ThreatPattern("PROMPT_LEAKAGE_ATTEMPT",
                    Pattern.compile("(?i)\\b(reveal|print|show|output|leak|repeat)\\s+(your|the)?.*\\b(system\\s+prompt|internal\\s+prompt|hidden\\s+prompt|system\\s+instructions|secret\\s+rules|secrets)\\b")),

            // 4. Injeção de delimitadores de contexto de LLMs populares
            new ThreatPattern("DELIMITER_INJECTION",
                    Pattern.compile("(?i)(<\\|im_start\\|>|<\\|im_end\\|>|\\[INST\\]|<<SYS>>|<system>|</system>|<assistant>)")),

            // 5. Injeção de SQL ou quebra de fronteiras multi-tenant
            new ThreatPattern("DATA_EXFILTRATION_SQL",
                    Pattern.compile("(?i)\\b(export\\s+all\\s+tenants|select\\s+.*\\s+from\\s+(organizations|tenants|users)|drop\\s+table|delete\\s+from)\\b"))
    );

    private record ThreatPattern(String threatType, Pattern pattern) {}

    /**
     * Examina o texto em busca de padrões maliciosos de injeção.
     */
    public ScanResult scan(String text, String contextName) {
        if (text == null || text.isBlank()) {
            return ScanResult.safe();
        }

        for (ThreatPattern tp : THREAT_PATTERNS) {
            Matcher matcher = tp.pattern().matcher(text);
            if (matcher.find()) {
                String snippet = matcher.group();
                log.warn("Tentativa de Prompt Injection detectada! Tipo: '{}', Contexto: '{}', Trecho: '{}'",
                        tp.threatType(), contextName, snippet);
                return ScanResult.threat(tp.threatType(), snippet);
            }
        }

        return ScanResult.safe();
    }

    /**
     * Valida uma entrada de usuário. Se for detectada tentativa de injeção,
     * lança PromptInjectionException imediatamente (Fail-Closed).
     */
    public void validateSafeInput(String text, String contextName) {
        ScanResult result = scan(text, contextName);
        if (!result.clean()) {
            throw new PromptInjectionException(
                    "Entrada bloqueada por violação das políticas de segurança e integridade (Prompt Injection detectado).",
                    result.threatType(),
                    result.offendingSnippet()
            );
        }
    }

    /**
     * Sanitiza e envolve dados não confiáveis de terceiros (descrições, títulos, observações)
     * em tags XML de contenção estrita, garantindo que o modelo os trate como dados literais e não como instruções.
     */
    public String sanitizeAndWrapUntrustedContent(String content, String contextTag) {
        if (content == null) {
            return "";
        }

        // 1. Escapa e neutraliza delimitadores estruturais perigosos
        String sanitized = content
                .replace("<system>", "&lt;system&gt;")
                .replace("</system>", "&lt;/system&gt;")
                .replace("<assistant>", "&lt;assistant&gt;")
                .replace("</assistant>", "&lt;/assistant&gt;")
                .replace("<|im_start|>", "")
                .replace("<|im_end|>", "")
                .replace("<<SYS>>", "");

        // 2. Escaneia por injeções ativas remanescentes de override ou extração
        ScanResult scan = scan(sanitized, contextTag);
        if (!scan.clean() && !"DELIMITER_INJECTION".equals(scan.threatType())) {
            throw new PromptInjectionException(
                    "Conteúdo externo bloqueado por conter tentativa de Prompt Injection no contexto: " + contextTag,
                    scan.threatType(),
                    scan.offendingSnippet()
            );
        }

        // 3. Envolve em tag de contenção estrita
        return "<untrusted_external_content context=\"" + contextTag + "\" sanitized=\"true\">\n" +
                sanitized.trim() + "\n" +
                "</untrusted_external_content>";
    }
}
