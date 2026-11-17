package br.com.sellerintelligence.tenancy.company;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanyRepository extends JpaRepository<Company, UUID> {

    List<Company> findByTenantId(UUID tenantId);

    List<Company> findByTenantIdAndStatus(UUID tenantId, CompanyStatus status);

    Optional<Company> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndCnpj(UUID tenantId, String cnpj);
}
