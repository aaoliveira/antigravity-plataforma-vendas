package com.sellerintelligence.modules.reporting.provider;

import com.sellerintelligence.modules.reporting.model.ExportDataPage;
import com.sellerintelligence.modules.reporting.model.ExportDataset;

import java.util.Map;
import java.util.UUID;

/**
 * Contrato para provedores de dados autorizados de exportação tenant-scoped.
 */
public interface ExportDataProvider {

    ExportDataset supportedDataset();

    ExportDataPage fetchTenantData(UUID tenantId, Map<String, Object> filterParams, int maxRows);
}
