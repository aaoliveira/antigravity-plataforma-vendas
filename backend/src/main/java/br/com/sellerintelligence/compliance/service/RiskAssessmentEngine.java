package br.com.sellerintelligence.compliance.service;

import br.com.sellerintelligence.compliance.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Deterministic risk evaluation engine for policy changes and operational impacts.
 */
@Service
public class RiskAssessmentEngine {

    private static final Logger log = LoggerFactory.getLogger(RiskAssessmentEngine.class);

    public ComplianceRiskLevel assessRisk(MarketplacePolicyChange change, String affectedFeature) {
        if (change == null) {
            return ComplianceRiskLevel.INFORMATIONAL;
        }

        PolicyChangeType type = change.getChangeType();
        String content = change.getCurrentContent() != null ? change.getCurrentContent().toLowerCase() : "";

        // 1. Formatting only
        if (type == PolicyChangeType.FORMATTING_ONLY) {
            return ComplianceRiskLevel.INFORMATIONAL;
        }

        // 2. Critical risks: deprecation, permission revocation, scraping bans
        if (type == PolicyChangeType.API_DEPRECATED || type == PolicyChangeType.PERMISSION_CHANGED) {
            log.warn("RiskAssessmentEngine: Assigned CRITICAL risk to {} change in {}", type, change.getMarketplace());
            return ComplianceRiskLevel.CRITICAL;
        }

        if (content.contains("suspens") || content.contains("ban") || content.contains("proib")
                || content.contains("revoked") || content.contains("violation")) {
            return ComplianceRiskLevel.CRITICAL;
        }

        // 3. High risks: Rate limits, authentication requirements, automation changes
        if (type == PolicyChangeType.RATE_LIMIT_CHANGED
                || type == PolicyChangeType.AUTH_REQUIREMENT_CHANGED
                || type == PolicyChangeType.AUTOMATION_RULE_CHANGED) {
            log.warn("RiskAssessmentEngine: Assigned HIGH risk to {} change in {}", type, change.getMarketplace());
            return ComplianceRiskLevel.HIGH;
        }

        // 4. Medium risks: Payload changes, endpoint modifications
        if (type == PolicyChangeType.PAYLOAD_CHANGED || type == PolicyChangeType.ENDPOINT_CHANGED) {
            return ComplianceRiskLevel.MEDIUM;
        }

        if (type == PolicyChangeType.RULE_ADDITION || type == PolicyChangeType.RULE_REMOVAL) {
            return ComplianceRiskLevel.MEDIUM;
        }

        return ComplianceRiskLevel.LOW;
    }
}
