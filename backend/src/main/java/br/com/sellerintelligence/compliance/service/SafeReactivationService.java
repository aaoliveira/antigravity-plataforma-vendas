package br.com.sellerintelligence.compliance.service;

import br.com.sellerintelligence.audit.domain.AuditResult;
import br.com.sellerintelligence.audit.domain.AuditService;
import br.com.sellerintelligence.compliance.domain.*;
import br.com.sellerintelligence.compliance.exception.ComplianceViolationException;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing governed, safe reactivation of suspended features.
 * Strictly prevents automatic reactivation.
 */
@Service
public class SafeReactivationService {

    private static final Logger log = LoggerFactory.getLogger(SafeReactivationService.class);

    private final FeatureEnforcementRepository enforcementRepository;
    private final SafeReactivationLogRepository reactivationLogRepository;
    private final ComplianceIncidentRepository incidentRepository;
    private final MarketplacePolicyRepository policyRepository;
    private final AuditService auditService;

    public SafeReactivationService(
            FeatureEnforcementRepository enforcementRepository,
            SafeReactivationLogRepository reactivationLogRepository,
            ComplianceIncidentRepository incidentRepository,
            MarketplacePolicyRepository policyRepository,
            AuditService auditService) {
        this.enforcementRepository = enforcementRepository;
        this.reactivationLogRepository = reactivationLogRepository;
        this.incidentRepository = incidentRepository;
        this.policyRepository = policyRepository;
        this.auditService = auditService;
    }

    @Transactional
    public SafeReactivationLog reactivateFeature(
            Marketplace marketplace,
            String featureName,
            String approvedBy,
            String evidence,
            String policyVersion,
            String testsExecuted) {

        if (marketplace == null || featureName == null || featureName.isBlank()) {
            throw new IllegalArgumentException("Marketplace and featureName must not be empty");
        }

        if (approvedBy == null || approvedBy.isBlank()) {
            throw new ComplianceViolationException("UNAUTHORIZED_REACTIVATION",
                    "Feature reactivation strictly requires an identified authorized administrator.");
        }

        if (evidence == null || evidence.isBlank()) {
            throw new ComplianceViolationException("MISSING_REACTIVATION_EVIDENCE",
                    "Feature reactivation requires verified documentary evidence.");
        }

        if (testsExecuted == null || testsExecuted.isBlank()) {
            throw new ComplianceViolationException("MISSING_TEST_PROOF",
                    "Feature reactivation requires execution of automated validation tests.");
        }

        // 1. Verify that the marketplace policy source is NOT currently unavailable
        List<MarketplacePolicy> policies = policyRepository.findByMarketplace(marketplace);
        boolean sourceUnavailable = policies.stream().anyMatch(p -> p.getStatus() == PolicyStatus.SOURCE_UNAVAILABLE);
        if (sourceUnavailable) {
            throw new ComplianceViolationException("SOURCE_STILL_UNAVAILABLE",
                    "Cannot reactivate feature while official policy source is marked as SOURCE_UNAVAILABLE.");
        }

        // 2. Update feature enforcement record
        FeatureEnforcementRecord record = enforcementRepository.findByMarketplaceAndFeatureName(marketplace, featureName)
                .orElseGet(() -> new FeatureEnforcementRecord(UUID.randomUUID(), marketplace, featureName, PolicyStatus.ACTIVE, "Reactivated", null));

        record.setStatus(PolicyStatus.ACTIVE);
        record.setReason("Reactivated with authorized human sign-off by " + approvedBy + ". Evidence: " + evidence);
        enforcementRepository.save(record);

        // 3. Resolve associated open incidents
        List<ComplianceIncident> openIncidents = incidentRepository.findByMarketplace(marketplace);
        for (ComplianceIncident inc : openIncidents) {
            if (inc.getAffectedFeatures().contains(featureName) && inc.getStatus() != IncidentStatus.RESOLVED && inc.getStatus() != IncidentStatus.CLOSED) {
                inc.resolve(approvedBy, "Resolved via safe reactivation. Evidence: " + evidence);
                incidentRepository.save(inc);
            }
        }

        // 4. Record Safe Reactivation Log
        SafeReactivationLog logRecord = new SafeReactivationLog(
                UUID.randomUUID(),
                marketplace,
                featureName,
                approvedBy,
                evidence,
                policyVersion,
                testsExecuted
        );
        logRecord = reactivationLogRepository.save(logRecord);

        // 5. Audit Event
        auditService.recordEvent(
                null,
                approvedBy,
                "SAFE_REACTIVATION_APPROVED",
                "FeatureEnforcementRecord",
                record.getId().toString(),
                "Safe reactivation of " + featureName + " on " + marketplace + " approved by " + approvedBy,
                AuditResult.SUCCESS
        );

        log.info("SafeReactivationService: Feature '{}' on {} successfully reactivated by {} with test proof: {}",
                featureName, marketplace, approvedBy, testsExecuted);

        return logRecord;
    }

    @Transactional(readOnly = true)
    public List<SafeReactivationLog> listHistory(Marketplace marketplace) {
        if (marketplace != null) {
            return reactivationLogRepository.findByMarketplaceOrderByApprovedAtDesc(marketplace);
        }
        return reactivationLogRepository.findTop20ByOrderByApprovedAtDesc();
    }
}
