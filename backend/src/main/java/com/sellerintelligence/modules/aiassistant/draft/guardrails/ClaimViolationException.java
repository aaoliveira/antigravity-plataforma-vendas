package com.sellerintelligence.modules.aiassistant.draft.guardrails;

import java.util.List;

/**
 * Exceção lançada quando claims proibidas ou não comprovadas são detectadas na geração de anúncios (P020-BE).
 */
public class ClaimViolationException extends RuntimeException {

    private final List<String> violations;

    public ClaimViolationException(String message, List<String> violations) {
        super(message);
        this.violations = violations != null ? List.copyOf(violations) : List.of();
    }

    public List<String> getViolations() {
        return violations;
    }
}
