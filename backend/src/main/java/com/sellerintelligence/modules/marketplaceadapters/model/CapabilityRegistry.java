package com.sellerintelligence.modules.marketplaceadapters.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registro de capacidades e evidências documentais por marketplace.
 * Governa com rigor a política Fail-Closed: capacidades sem evidência oficial
 * permanecem como UNVERIFIED e são categoricamente impedidas de executar.
 */
public class CapabilityRegistry {

    private final String marketplaceId;
    private final Map<MarketplaceCapability, CapabilityEntry> entries = new EnumMap<>(MarketplaceCapability.class);

    public record CapabilityEntry(CapabilityStatus status, CapabilityEvidence evidence) {
    }

    public CapabilityRegistry(String marketplaceId) {
        if (marketplaceId == null || marketplaceId.isBlank()) {
            throw new IllegalArgumentException("marketplaceId é obrigatório no CapabilityRegistry");
        }
        this.marketplaceId = marketplaceId;
    }

    public String getMarketplaceId() {
        return marketplaceId;
    }

    /**
     * Registra uma capacidade com seu respectivo status e evidência documental.
     * Se uma capacidade for declarada como SUPPORTED mas não possuir evidência,
     * seu status é forçado para UNVERIFIED (Fail-Closed).
     */
    public synchronized void register(MarketplaceCapability capability, CapabilityStatus status, CapabilityEvidence evidence) {
        if (capability == null) {
            throw new IllegalArgumentException("capability não pode ser nula");
        }

        CapabilityStatus finalStatus = status != null ? status : CapabilityStatus.UNVERIFIED;

        // Regra anti-alucinação: SUPPORTED sem evidência é rebaixado para UNVERIFIED
        if (finalStatus == CapabilityStatus.SUPPORTED && evidence == null) {
            finalStatus = CapabilityStatus.UNVERIFIED;
        }

        entries.put(capability, new CapabilityEntry(finalStatus, evidence));
    }

    /**
     * Consulta o status atual de uma capacidade no canal.
     * Se a capacidade não estiver catalogada, retorna UNVERIFIED por padrão.
     */
    public CapabilityStatus getStatus(MarketplaceCapability capability) {
        CapabilityEntry entry = entries.get(capability);
        return entry != null ? entry.status() : CapabilityStatus.UNVERIFIED;
    }

    /**
     * Consulta a evidência documental associada a uma capacidade.
     */
    public Optional<CapabilityEvidence> getEvidence(MarketplaceCapability capability) {
        CapabilityEntry entry = entries.get(capability);
        return entry != null ? Optional.ofNullable(entry.evidence()) : Optional.empty();
    }

    /**
     * Verifica se a capacidade está aprovada para execução operacional.
     * Somente retorna true se o status for SUPPORTED e houver evidência válida.
     */
    public boolean isExecutable(MarketplaceCapability capability) {
        CapabilityEntry entry = entries.get(capability);
        if (entry == null) {
            return false;
        }
        return entry.status() == CapabilityStatus.SUPPORTED && entry.evidence() != null;
    }

    /**
     * Retorna visualização imutável de todas as capacidades registradas.
     */
    public Map<MarketplaceCapability, CapabilityEntry> getAllEntries() {
        return Collections.unmodifiableMap(entries);
    }
}
