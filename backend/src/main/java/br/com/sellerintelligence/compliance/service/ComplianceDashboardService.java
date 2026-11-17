package br.com.sellerintelligence.compliance.service;

import br.com.sellerintelligence.compliance.domain.*;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service providing aggregated indicators and compliance health metrics for the administrative dashboard.
 */
@Service
public class ComplianceDashboardService {

    private final MarketplacePolicyRepository policyRepository;
    private final MarketplacePolicyChangeRepository changeRepository;
    private final ComplianceIncidentRepository incidentRepository;
    private final FeatureEnforcementRepository enforcementRepository;
    private final MarketplaceApiLifecycleRepository lifecycleRepository;

    public ComplianceDashboardService(
            MarketplacePolicyRepository policyRepository,
            MarketplacePolicyChangeRepository changeRepository,
            ComplianceIncidentRepository incidentRepository,
            FeatureEnforcementRepository enforcementRepository,
            MarketplaceApiLifecycleRepository lifecycleRepository) {
        this.policyRepository = policyRepository;
        this.changeRepository = changeRepository;
        this.incidentRepository = incidentRepository;
        this.enforcementRepository = enforcementRepository;
        this.lifecycleRepository = lifecycleRepository;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();

        long totalPolicies = policyRepository.count();
        long activePolicies = policyRepository.countByStatus(PolicyStatus.ACTIVE);
        long unavailableSources = policyRepository.countByStatus(PolicyStatus.SOURCE_UNAVAILABLE);
        long suspendedPolicies = policyRepository.countByStatus(PolicyStatus.SUSPENDED) + policyRepository.countByStatus(PolicyStatus.BLOCKED);

        long openIncidents = incidentRepository.countByStatus(IncidentStatus.OPEN) +
                incidentRepository.countByStatus(IncidentStatus.INVESTIGATING) +
                incidentRepository.countByStatus(IncidentStatus.MITIGATING);
        long criticalIncidents = incidentRepository.countBySeverity(ComplianceRiskLevel.CRITICAL);

        long suspendedFeatures = enforcementRepository.countByStatus(PolicyStatus.SUSPENDED) +
                enforcementRepository.countByStatus(PolicyStatus.BLOCKED);

        long deprecatedApis = lifecycleRepository.findByStatus(ApiLifecycleStatus.DEPRECATED).size() +
                lifecycleRepository.findByStatus(ApiLifecycleStatus.SUNSET_SCHEDULED).size();

        summary.put("totalMarketplacesMonitored", 6);
        summary.put("totalOfficialSources", totalPolicies);
        summary.put("verifiedSources", activePolicies);
        summary.put("unavailableSources", unavailableSources);
        summary.put("suspendedPolicies", suspendedPolicies);
        summary.put("openIncidents", openIncidents);
        summary.put("criticalRisks", criticalIncidents);
        summary.put("suspendedFeatures", suspendedFeatures);
        summary.put("deprecatedApis", deprecatedApis);

        // Marketplace Breakdown
        List<Map<String, Object>> marketplaceStats = new ArrayList<>();
        for (Marketplace mkt : List.of(Marketplace.MERCADO_LIVRE, Marketplace.SHOPEE, Marketplace.AMAZON,
                Marketplace.TIKTOK_SHOP, Marketplace.MAGALU, Marketplace.SHEIN)) {

            Map<String, Object> mktData = new LinkedHashMap<>();
            List<MarketplacePolicy> policies = policyRepository.findByMarketplace(mkt);
            List<ComplianceIncident> incidents = incidentRepository.findByMarketplace(mkt);
            List<FeatureEnforcementRecord> enforcements = enforcementRepository.findByMarketplace(mkt);

            boolean hasSuspension = enforcements.stream().anyMatch(e -> e.getStatus() == PolicyStatus.SUSPENDED || e.getStatus() == PolicyStatus.BLOCKED);
            boolean hasUnavailable = policies.stream().anyMatch(p -> p.getStatus() == PolicyStatus.SOURCE_UNAVAILABLE);

            String statusLabel;
            if (mkt == Marketplace.TIKTOK_SHOP || mkt == Marketplace.MAGALU || mkt == Marketplace.SHEIN) {
                statusLabel = "UNVERIFIED_COMPLIANCE (BLOCKED)";
            } else if (hasSuspension) {
                statusLabel = "RESTRICTED / SUSPENDED";
            } else if (hasUnavailable) {
                statusLabel = "PARTIALLY_VERIFIED (SOURCE_UNAVAILABLE)";
            } else {
                statusLabel = "COMPLIANT (VERIFIED)";
            }

            mktData.put("marketplace", mkt.name());
            mktData.put("status", statusLabel);
            mktData.put("policyCount", policies.size());
            mktData.put("openIncidents", incidents.stream().filter(i -> i.getStatus() != IncidentStatus.RESOLVED && i.getStatus() != IncidentStatus.CLOSED).count());
            mktData.put("suspendedFeatures", enforcements.stream().filter(e -> e.getStatus() == PolicyStatus.SUSPENDED || e.getStatus() == PolicyStatus.BLOCKED).count());
            marketplaceStats.add(mktData);
        }

        summary.put("marketplaces", marketplaceStats);
        summary.put("recentChanges", changeRepository.findTop20ByOrderByDetectedAtDesc());
        summary.put("activeIncidents", incidentRepository.findByStatusIn(List.of(IncidentStatus.OPEN, IncidentStatus.INVESTIGATING, IncidentStatus.MITIGATING)));

        return summary;
    }
}
