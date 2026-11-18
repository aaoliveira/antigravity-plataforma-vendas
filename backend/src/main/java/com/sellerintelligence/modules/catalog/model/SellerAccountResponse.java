package com.sellerintelligence.modules.catalog.model;

import java.util.UUID;

public record SellerAccountResponse(
        UUID id,
        String marketplace,
        String externalAccountId,
        String accountName,
        String status
) {}
