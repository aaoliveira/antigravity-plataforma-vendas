package br.com.sellerintelligence.connector.model;

import java.util.List;

public record ConnectorSyncBatch<T>(
        List<T> items,
        String nextCursor,
        boolean hasMore
) {}
