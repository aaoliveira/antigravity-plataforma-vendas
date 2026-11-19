package com.sellerintelligence.modules.reporting.provider;

import com.sellerintelligence.modules.reporting.model.ExportDataset;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ExportDataProviderRegistry {

    private final Map<ExportDataset, ExportDataProvider> providers = new EnumMap<>(ExportDataset.class);

    public ExportDataProviderRegistry(List<ExportDataProvider> providerList) {
        for (ExportDataProvider provider : providerList) {
            providers.put(provider.supportedDataset(), provider);
        }
    }

    public ExportDataProvider getProvider(ExportDataset dataset) {
        return Optional.ofNullable(providers.get(dataset))
                .orElseThrow(() -> new IllegalArgumentException("Nenhum provedor de dados registrado para o dataset: " + dataset));
    }
}
