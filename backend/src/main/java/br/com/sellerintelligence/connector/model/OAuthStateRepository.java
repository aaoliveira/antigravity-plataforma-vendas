package br.com.sellerintelligence.connector.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OAuthStateRepository extends JpaRepository<OAuthState, UUID> {

    Optional<OAuthState> findByState(String state);

    void deleteByStoreId(UUID storeId);
}
