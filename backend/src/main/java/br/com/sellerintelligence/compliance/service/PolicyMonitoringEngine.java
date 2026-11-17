package br.com.sellerintelligence.compliance.service;

import br.com.sellerintelligence.compliance.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Engine executing continuous periodic monitoring of verified official marketplace sources.
 */
@Service
public class PolicyMonitoringEngine {

    private static final Logger log = LoggerFactory.getLogger(PolicyMonitoringEngine.class);

    private final MarketplacePolicyRepository policyRepository;
    private final PolicyChangeDetector changeDetector;

    public PolicyMonitoringEngine(
            MarketplacePolicyRepository policyRepository,
            PolicyChangeDetector changeDetector) {
        this.policyRepository = policyRepository;
        this.changeDetector = changeDetector;
    }

    @Transactional
    public void checkAllActivePolicies(String actorId) {
        List<MarketplacePolicy> policies = policyRepository.findAll();
        log.info("PolicyMonitoringEngine: Executing continuous monitoring cycle over {} registered policies...", policies.size());

        for (MarketplacePolicy policy : policies) {
            checkPolicy(policy.getId(), null, actorId);
        }
    }

    @Transactional
    public void checkPolicy(UUID policyId, String simulatedRemoteContent, String actorId) {
        MarketplacePolicy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));

        policy.setLastCheckedAt(OffsetDateTime.now());

        // In test or simulation, use provided content; otherwise use existing rawContent
        String remoteContent = simulatedRemoteContent != null ? simulatedRemoteContent : policy.getRawContent();

        if (remoteContent == null || remoteContent.isBlank()) {
            // Source is unavailable or empty
            log.warn("PolicyMonitoringEngine: Source is unavailable for policy '{}' ({})",
                    policy.getPolicyName(), policy.getMarketplace());
            policy.setStatus(PolicyStatus.SOURCE_UNAVAILABLE);
            policyRepository.save(policy);
            return;
        }

        String remoteHash = MarketplacePolicyRegistryService.computeHash(remoteContent);

        if (!remoteHash.equals(policy.getContentHash())) {
            log.info("PolicyMonitoringEngine: Hash diff detected for policy '{}' on {}! Triggering change detector.",
                    policy.getPolicyName(), policy.getMarketplace());
            changeDetector.evaluateAndRecordChange(
                    policy,
                    remoteContent,
                    policy.getPolicyVersion(),
                    "Continuous policy monitoring scan detected content divergence",
                    actorId
            );
        } else {
            log.debug("PolicyMonitoringEngine: Policy '{}' on {} content unchanged (hash: {})",
                    policy.getPolicyName(), policy.getMarketplace(), remoteHash);
        }

        policyRepository.save(policy);
    }

    @Transactional
    public void markSourceUnavailable(UUID policyId, String actorId) {
        MarketplacePolicy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("Policy not found: " + policyId));

        policy.setStatus(PolicyStatus.SOURCE_UNAVAILABLE);
        policy.setLastCheckedAt(OffsetDateTime.now());
        policyRepository.save(policy);

        log.warn("PolicyMonitoringEngine: Explicitly marked source UNAVAILABLE for policy '{}' ({})",
                policy.getPolicyName(), policy.getMarketplace());
    }
}
