package br.com.sellerintelligence.compliance.service;

import br.com.sellerintelligence.compliance.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service that analyzes functional impact of detected policy changes and coordinates
 * automated enforcement, incident creation, and notifications.
 */
@Service
public class ComplianceImpactAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(ComplianceImpactAnalyzer.class);

    private final RiskAssessmentEngine riskAssessmentEngine;
    private final AutomaticComplianceEnforcer complianceEnforcer;
    private final ComplianceIncidentService incidentService;
    private final ComplianceNotificationService notificationService;

    public ComplianceImpactAnalyzer(
            RiskAssessmentEngine riskAssessmentEngine,
            AutomaticComplianceEnforcer complianceEnforcer,
            @Lazy ComplianceIncidentService incidentService,
            ComplianceNotificationService notificationService) {
        this.riskAssessmentEngine = riskAssessmentEngine;
        this.complianceEnforcer = complianceEnforcer;
        this.incidentService = incidentService;
        this.notificationService = notificationService;
    }

    @Transactional
    public void analyzeAndEnforce(MarketplacePolicyChange change, String actorId) {
        if (change == null) {
            return;
        }

        String affectedFeature = deduceAffectedFeature(change);
        ComplianceRiskLevel riskLevel = riskAssessmentEngine.assessRisk(change, affectedFeature);
        change.setRiskLevel(riskLevel);

        String impactDescription = "Detected policy change impacting feature '" + affectedFeature +
                "' on " + change.getMarketplace() + ". Evaluated Risk: " + riskLevel +
                ". Reason: " + change.getChangeType();

        change.setPreliminaryImpact(impactDescription);

        log.info("ComplianceImpactAnalyzer: Evaluated impact for change {}: feature={}, risk={}",
                change.getId(), affectedFeature, riskLevel);

        if (riskLevel == ComplianceRiskLevel.CRITICAL) {
            // 1. Open Incident
            ComplianceIncident incident = incidentService.openIncident(
                    change.getMarketplace(),
                    change.getPolicy().getId(),
                    change.getId(),
                    ComplianceRiskLevel.CRITICAL,
                    affectedFeature,
                    "SECURITY_RESPONSE_TEAM",
                    "Critical policy modification detected: " + change.getChangeType() + ". Immediate feature suspension applied.",
                    change.getEvidence(),
                    "Perform formal technical review, verify updated API documentation, and execute regression test suite prior to safe reactivation.",
                    actorId
            );

            // 2. Automated Least-Scope Suspension
            complianceEnforcer.enforceRestriction(
                    change.getMarketplace(),
                    affectedFeature,
                    PolicyStatus.SUSPENDED,
                    "Automated suspension triggered by CRITICAL policy change (" + change.getChangeType() + ")",
                    incident.getId(),
                    actorId
            );

            // 3. Administrative Alert
            notificationService.sendAlert(
                    change.getMarketplace(),
                    ComplianceRiskLevel.CRITICAL,
                    "Critical Policy Change - Feature Suspended",
                    "Feature " + affectedFeature + " on " + change.getMarketplace() +
                            " has been automatically SUSPENDED under Fail-Closed policy. Incident " + incident.getIncidentCode() + " opened."
            );

            change.setStatus(ChangeAnalysisStatus.INCIDENT_OPENED);

        } else if (riskLevel == ComplianceRiskLevel.HIGH) {
            // High risk: Restrict feature and require compliance analysis
            ComplianceIncident incident = incidentService.openIncident(
                    change.getMarketplace(),
                    change.getPolicy().getId(),
                    change.getId(),
                    ComplianceRiskLevel.HIGH,
                    affectedFeature,
                    "COMPLIANCE_LEAD",
                    "High-risk policy modification detected: " + change.getChangeType(),
                    change.getEvidence(),
                    "Review operational limits, adapt connector parameters and update compliance matrix.",
                    actorId
            );

            complianceEnforcer.enforceRestriction(
                    change.getMarketplace(),
                    affectedFeature,
                    PolicyStatus.RESTRICTED,
                    "Automated restriction applied due to HIGH risk policy change",
                    incident.getId(),
                    actorId
            );

            notificationService.sendAlert(
                    change.getMarketplace(),
                    ComplianceRiskLevel.HIGH,
                    "High-Risk Policy Change - Feature Restricted",
                    "Feature " + affectedFeature + " on " + change.getMarketplace() + " is restricted. Incident " + incident.getIncidentCode() + " opened."
            );

            change.setStatus(ChangeAnalysisStatus.INCIDENT_OPENED);

        } else if (riskLevel == ComplianceRiskLevel.MEDIUM) {
            notificationService.sendAlert(
                    change.getMarketplace(),
                    ComplianceRiskLevel.MEDIUM,
                    "Medium-Risk Policy Change Detected",
                    "Policy change detected on " + change.getMarketplace() + " affecting " + affectedFeature + ". Scheduled for review."
            );
            change.setStatus(ChangeAnalysisStatus.ANALYZED);

        } else {
            // LOW or INFORMATIONAL
            change.setStatus(ChangeAnalysisStatus.ANALYZED);
        }
    }

    private String deduceAffectedFeature(MarketplacePolicyChange change) {
        if (change.getPolicy() != null && change.getPolicy().getAffectedFeature() != null) {
            return change.getPolicy().getAffectedFeature();
        }

        String content = change.getCurrentContent() != null ? change.getCurrentContent().toLowerCase() : "";
        if (content.contains("order") || content.contains("pedido") || content.contains("venda")) {
            return "SYNC_ORDERS";
        }
        if (content.contains("item") || content.contains("product") || content.contains("catalog") || content.contains("anúncio")) {
            return "SYNC_CATALOG";
        }
        if (content.contains("price") || content.contains("preço")) {
            return "PRICE_UPDATE";
        }
        if (content.contains("extension") || content.contains("extens")) {
            return "BROWSER_EXTENSION";
        }
        if (content.contains("search") || content.contains("busca")) {
            return "PUBLIC_SEARCH";
        }
        return "GENERAL_INTEGRATION";
    }
}
