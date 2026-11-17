package br.com.sellerintelligence.compliance.service;

import br.com.sellerintelligence.audit.domain.AuditResult;
import br.com.sellerintelligence.audit.domain.AuditService;
import br.com.sellerintelligence.compliance.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Service responsible for detecting and categorizing changes in marketplace policies and APIs.
 */
@Service
public class PolicyChangeDetector {

    private static final Logger log = LoggerFactory.getLogger(PolicyChangeDetector.class);

    private final MarketplacePolicyChangeRepository changeRepository;
    private final ComplianceImpactAnalyzer impactAnalyzer;
    private final AuditService auditService;

    public PolicyChangeDetector(
            MarketplacePolicyChangeRepository changeRepository,
            ComplianceImpactAnalyzer impactAnalyzer,
            AuditService auditService) {
        this.changeRepository = changeRepository;
        this.impactAnalyzer = impactAnalyzer;
        this.auditService = auditService;
    }

    @Transactional
    public MarketplacePolicyChange evaluateAndRecordChange(
            MarketplacePolicy policy,
            String newRawContent,
            String newVersion,
            String evidence,
            String actorId) {

        Objects.requireNonNull(policy, "policy must not be null");

        String previousContent = policy.getRawContent() != null ? policy.getRawContent() : "";
        String currentContent = newRawContent != null ? newRawContent : "";

        String previousHash = policy.getContentHash();
        String currentHash = MarketplacePolicyRegistryService.computeHash(currentContent);

        // 1. Semantic diff vs formatting diff
        boolean formattingOnly = isFormattingOnlyChange(previousContent, currentContent);

        PolicyChangeType changeType;
        ComplianceRiskLevel riskLevel;

        if (formattingOnly) {
            changeType = PolicyChangeType.FORMATTING_ONLY;
            riskLevel = ComplianceRiskLevel.INFORMATIONAL;
            log.info("PolicyChangeDetector: Detected formatting-only change in policy '{}' ({})",
                    policy.getPolicyName(), policy.getMarketplace());
        } else {
            changeType = deduceChangeType(policy, currentContent);
            riskLevel = deduceRiskLevel(policy, changeType, currentContent);
            log.warn("PolicyChangeDetector: Detected SUBSTANTIVE policy change in '{}' ({}): type={}, risk={}",
                    policy.getPolicyName(), policy.getMarketplace(), changeType, riskLevel);
        }

        String preliminaryImpact = "Affected feature: " + (policy.getAffectedFeature() != null ? policy.getAffectedFeature() : "GENERAL") +
                ". Risk classification: " + riskLevel + ". Change type: " + changeType;

        MarketplacePolicyChange change = new MarketplacePolicyChange(
                UUID.randomUUID(),
                policy,
                policy.getMarketplace(),
                policy.getOfficialUrl(),
                changeType,
                previousContent,
                currentContent,
                previousHash,
                currentHash,
                OffsetDateTime.now(),
                policy.getEffectiveDate(),
                evidence,
                preliminaryImpact,
                riskLevel,
                ChangeAnalysisStatus.NEW
        );

        change = changeRepository.save(change);

        // Update policy record
        policy.setContentHash(currentHash);
        policy.setRawContent(currentContent);
        policy.setLastChangedAt(OffsetDateTime.now());
        policy.setLastCheckedAt(OffsetDateTime.now());
        if (newVersion != null && !newVersion.isBlank()) {
            policy.setPolicyVersion(newVersion);
        }

        auditService.recordEvent(
                null,
                actorId != null ? actorId : "SYSTEM",
                "POLICY_CHANGE_DETECTED",
                "MarketplacePolicyChange",
                change.getId().toString(),
                "Change detected for " + policy.getMarketplace() + " [" + changeType + " - " + riskLevel + "]",
                AuditResult.SUCCESS
        );

        // Delegate to impact analyzer
        impactAnalyzer.analyzeAndEnforce(change, actorId);

        return change;
    }

    /**
     * Normalizes text by removing non-semantic whitespace and HTML formatting to check if change is purely formatting.
     */
    public boolean isFormattingOnlyChange(String prev, String curr) {
        if (prev.equals(curr)) {
            return true;
        }
        String normPrev = prev.replaceAll("\\s+", " ").trim();
        String normCurr = curr.replaceAll("\\s+", " ").trim();
        return normPrev.equalsIgnoreCase(normCurr);
    }

    private PolicyChangeType deduceChangeType(MarketplacePolicy policy, String currentContent) {
        String lower = currentContent.toLowerCase();
        if (lower.contains("deprecated") || lower.contains("sunset") || lower.contains("descontinuad")) {
            return PolicyChangeType.API_DEPRECATED;
        }
        if (lower.contains("rate limit") || lower.contains("limite de requisi") || lower.contains("quota")) {
            return PolicyChangeType.RATE_LIMIT_CHANGED;
        }
        if (lower.contains("oauth") || lower.contains("scope") || lower.contains("escopo") || lower.contains("permiss")) {
            return PolicyChangeType.PERMISSION_CHANGED;
        }
        if (lower.contains("automation") || lower.contains("automação") || lower.contains("bot") || lower.contains("scraping")) {
            return PolicyChangeType.AUTOMATION_RULE_CHANGED;
        }
        if (lower.contains("endpoint") || lower.contains("url") || lower.contains("path")) {
            return PolicyChangeType.ENDPOINT_CHANGED;
        }
        return PolicyChangeType.RULE_ADDITION;
    }

    private ComplianceRiskLevel deduceRiskLevel(MarketplacePolicy policy, PolicyChangeType changeType, String currentContent) {
        if (changeType == PolicyChangeType.API_DEPRECATED || changeType == PolicyChangeType.PERMISSION_CHANGED) {
            return ComplianceRiskLevel.CRITICAL;
        }
        if (changeType == PolicyChangeType.AUTOMATION_RULE_CHANGED || changeType == PolicyChangeType.RATE_LIMIT_CHANGED) {
            return ComplianceRiskLevel.HIGH;
        }
        if (changeType == PolicyChangeType.FORMATTING_ONLY) {
            return ComplianceRiskLevel.INFORMATIONAL;
        }
        return ComplianceRiskLevel.MEDIUM;
    }
}
