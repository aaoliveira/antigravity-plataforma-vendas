package br.com.sellerintelligence.compliance.domain;

import br.com.sellerintelligence.tenancy.store.Marketplace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ComplianceIncidentRepository extends JpaRepository<ComplianceIncident, UUID> {

    List<ComplianceIncident> findByStatus(IncidentStatus status);

    List<ComplianceIncident> findByMarketplace(Marketplace marketplace);

    List<ComplianceIncident> findByStatusIn(List<IncidentStatus> statuses);

    Optional<ComplianceIncident> findByIncidentCode(String incidentCode);

    long countByStatus(IncidentStatus status);

    long countBySeverity(ComplianceRiskLevel severity);
}
