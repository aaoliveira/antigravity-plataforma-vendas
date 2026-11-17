package br.com.sellerintelligence.connector.model;

public record ConnectorCapability(
        ConnectorCapabilityType type,
        CapabilityStatus status,
        String description,
        boolean requiresSpecialApproval
) {
    public static ConnectorCapability supported(ConnectorCapabilityType type, String description) {
        return new ConnectorCapability(type, CapabilityStatus.SUPPORTED, description, false);
    }

    public static ConnectorCapability unsupported(ConnectorCapabilityType type, String description) {
        return new ConnectorCapability(type, CapabilityStatus.UNSUPPORTED, description, false);
    }

    public static ConnectorCapability pendingValidation(ConnectorCapabilityType type, String description, boolean requiresSpecialApproval) {
        return new ConnectorCapability(type, CapabilityStatus.PENDING_VALIDATION, description, requiresSpecialApproval);
    }
}
