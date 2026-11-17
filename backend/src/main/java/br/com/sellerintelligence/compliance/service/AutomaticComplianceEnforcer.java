package br.com.sellerintelligence.compliance.service;

import br.com.sellerintelligence.audit.domain.AuditResult;
import br.com.sellerintelligence.audit.domain.AuditService;
import br.com.sellerintelligence.compliance.domain.*;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service applying automated compliance enforcement following the Principle of Least Scope:
 * Suspends or restricts only the specific affected feature, preserving unrelated legitimate operations.
 */
@Service
public class AutomaticComplianceEnforcer {

    private static final Logger log = LoggerFactory.getLogger(AutomaticComplianceEnforcer.class);

    private final FeatureEnforcementRepository enforcementRepository;
    private final AuditService auditService;

    public AutomaticComplianceEnforcer(
            FeatureEnforcementRepository enforcementRepository,
            AuditService auditService) {
        this.enforcementRepository = enforcementRepository;
        this.auditService = auditService;
    }

    @Transactional
    public FeatureEnforcementRecord enforceRestriction(
            Marketplace marketplace,
            String featureName,
            PolicyStatus status,
            String reason,
            UUID incidentId,
            String actorId) {

        String targetFeature = (featureName != null && !featureName.isBlank()) ? featureName : "ALL_OPERATIONS";

        FeatureEnforcementRecord record = enforcementRepository.findByMarketplaceAndFeatureName(marketplace, targetFeature)
                .orElseGet(() -> new FeatureEnforcementRecord(UUID.randomUUID(), marketplace, targetFeature, status, reason, incidentId));

        record.setStatus(status);
        record.setReason(reason);
        record.setIncidentId(incidentId);
        record = enforcementRepository.save(record);

        auditService.recordEvent(
                null,
                actorId != null ? actorId : "SYSTEM_ENFORCER",
                "COMPLIANCE_ENFORCEMENT_APPLIED",
                "FeatureEnforcementRecord",
                record.getId().toString(),
                "Enforced status " + status + " on " + marketplace + " feature " + targetFeature + " (" + reason + ")",
                AuditResult.SUCCESS
        );

        log.warn("AutomaticComplianceEnforcer: Enforced LEAST-SCOPE status {} on {} [Feature: {}] - Reason: {}",
                status, marketplace, targetFeature, reason);

        return record;
    }

    @Transactional(readOnly = true)
    public boolean isFeatureRestricted(Marketplace marketplace, String featureName) {
        if (marketplace == null) {
            return true;
        }

        // 1. Check feature-specific restriction
        if (featureName != null) {
            Optional<FeatureEnforcementRecord> specific = enforcementRepository.findByMarketplaceAndFeatureName(marketplace, featureName);
            if (specific.isPresent() && isBlockingStatus(specific.get().getStatus())) {
                return true;
            }
        }

        // 2. Check marketplace-wide restriction
        Optional<FeatureEnforcementRecord> global = enforcementRepository.findByMarketplaceAndFeatureName(marketplace, "ALL_OPERATIONS");
        return global.isPresent() && isBlockingStatus(global.get().getStatus());
    }

    @Transactional(readOnly = true)
    public Optional<FeatureEnforcementRecord> getRecord(Marketplace marketplace, String featureName) {
        return enforcementRepository.findByMarketplaceAndFeatureName(marketplace, featureName);
    }

    @Transactional(readOnly = true)
    public List<FeatureEnforcementRecord> listAll() {
        return enforcementRepository.findAll();
    }

    private boolean isBlockingStatus(PolicyStatus status) {
        return status == PolicyStatus.SUSPENDED
                || status == PolicyStatus.BLOCKED
                || status == PolicyStatus.RESTRICTED;
    }
}
