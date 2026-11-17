package br.com.sellerintelligence.compliance.service;

import br.com.sellerintelligence.audit.domain.AuditResult;
import br.com.sellerintelligence.audit.domain.AuditService;
import br.com.sellerintelligence.compliance.domain.*;
import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service managing compliance incident lifecycle from OPEN to CLOSED.
 */
@Service
public class ComplianceIncidentService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceIncidentService.class);
    private static final AtomicLong INCIDENT_COUNTER = new AtomicLong(100);

    private final ComplianceIncidentRepository incidentRepository;
    private final AuditService auditService;

    public ComplianceIncidentService(
            ComplianceIncidentRepository incidentRepository,
            AuditService auditService) {
        this.incidentRepository = incidentRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ComplianceIncident openIncident(
            Marketplace marketplace,
            UUID policyId,
            UUID policyChangeId,
            ComplianceRiskLevel severity,
            String affectedFeatures,
            String responsible,
            String description,
            String evidence,
            String remediationPlan,
            String actorId) {

        String incidentCode = "INC-" + OffsetDateTime.now().getYear() + "-" + INCIDENT_COUNTER.incrementAndGet();

        ComplianceIncident incident = new ComplianceIncident(
                UUID.randomUUID(),
                incidentCode,
                marketplace,
                policyId,
                policyChangeId,
                severity,
                affectedFeatures,
                IncidentStatus.OPEN,
                responsible != null ? responsible : "COMPLIANCE_OFFICER",
                description,
                evidence,
                remediationPlan
        );

        incident = incidentRepository.save(incident);

        auditService.recordEvent(
                null,
                actorId != null ? actorId : "SYSTEM",
                "COMPLIANCE_INCIDENT_OPENED",
                "ComplianceIncident",
                incident.getId().toString(),
                "Incident " + incidentCode + " opened for " + marketplace + " (" + severity + ")",
                AuditResult.SUCCESS
        );

        log.warn("ComplianceIncident: Opened incident {} for {} [Severity: {}] - Affected: {}",
                incidentCode, marketplace, severity, affectedFeatures);

        return incident;
    }

    @Transactional
    public ComplianceIncident updateStatus(UUID incidentId, IncidentStatus newStatus, String actorId, String notes) {
        ComplianceIncident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));

        IncidentStatus prevStatus = incident.getStatus();
        incident.setStatus(newStatus);
        if (notes != null && !notes.isBlank()) {
            incident.setRemediationPlan((incident.getRemediationPlan() != null ? incident.getRemediationPlan() + "\n" : "") + notes);
        }

        if (newStatus == IncidentStatus.RESOLVED || newStatus == IncidentStatus.CLOSED) {
            incident.setResolvedAt(OffsetDateTime.now());
            incident.setApprovedBy(actorId);
        }

        incident = incidentRepository.save(incident);

        auditService.recordEvent(
                null,
                actorId != null ? actorId : "SYSTEM",
                "COMPLIANCE_INCIDENT_STATUS_CHANGED",
                "ComplianceIncident",
                incident.getId().toString(),
                "Transitioned incident " + incident.getIncidentCode() + " from " + prevStatus + " to " + newStatus,
                AuditResult.SUCCESS
        );

        return incident;
    }

    @Transactional(readOnly = true)
    public List<ComplianceIncident> listOpenIncidents() {
        return incidentRepository.findByStatusIn(List.of(IncidentStatus.OPEN, IncidentStatus.INVESTIGATING, IncidentStatus.MITIGATING));
    }

    @Transactional(readOnly = true)
    public List<ComplianceIncident> listAll() {
        return incidentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<ComplianceIncident> getById(UUID id) {
        return incidentRepository.findById(id);
    }
}
