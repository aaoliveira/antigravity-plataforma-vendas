package br.com.sellerintelligence.compliance.web.dto;

import br.com.sellerintelligence.tenancy.store.Marketplace;

public record ReactivateRequest(
        Marketplace marketplace,
        String featureName,
        String evidence,
        String policyVersion,
        String testsExecuted
) {}
