# Status de Entregas e Phase Gates (Phase Status)

> **Governança Sequencial de Release** &middot; **Versão:** 1.0.0  
> **Status Vigente:** PROMPT P019-BE = `APPROVED` (P019-FE Liberado)  
> **Data:** 19/11/2026

---

## 1. Tabela Oficial de Phase Gates

| Prompt ID   | Título da Etapa                                                                          | Status do Gate | Data da Avaliação | Evidência Formal                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        | Próximo Prompt Liberado?   |
| ----------- | ---------------------------------------------------------------------------------------- | -------------- | ----------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------- |
| **P000**    | Master Project Governance & Engineering Rules                                            | **`APPROVED`** | 17/11/2026        | [docs/PROMPT_EXECUTION_LOG.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/PROMPT_EXECUTION_LOG.md)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | **SIM (P001 liberado)**    |
| **P001-BE** | Estrutura Base do Repositório e Backend Modular                                          | **`APPROVED`** | 17/11/2026        | [docs/PROMPT_EXECUTION_LOG.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/PROMPT_EXECUTION_LOG.md)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | **SIM (P001-FE liberado)** |
| **P001-FE** | Shell da Aplicação Web Responsiva                                                        | **`APPROVED`** | 17/11/2026        | [docs/PROMPT_EXECUTION_LOG.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/PROMPT_EXECUTION_LOG.md)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | **SIM (P002-BE liberado)** |
| **P002-BE** | Identidade Mínima e Contexto do Tenant                                                   | **`APPROVED`** | 18/11/2026        | [docs/PROMPT_EXECUTION_LOG.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/PROMPT_EXECUTION_LOG.md)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | **SIM (P002-FE liberado)** |
| **P002-FE** | Interface de Autenticação e Troca de Contexto                                            | **`APPROVED`** | 18/11/2026        | [docs/PROMPT_EXECUTION_LOG.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/PROMPT_EXECUTION_LOG.md)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | **SIM (P003-BE liberado)** |
| **P003-BE** | Isolamento Multi-Tenant em Profundidade (RLS, Transacional e Testcontainers)             | **`APPROVED`** | 18/11/2026        | [PostgresMultiTenantRlsIntegrationTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/tenancy/PostgresMultiTenantRlsIntegrationTest.java)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       | **SIM (P003-FE liberado)** |
| **P003-FE** | Tratamento Padronizado de Erros (401/403/404) e Segurança de Tenant na UI                | **`APPROVED`** | 18/11/2026        | [ErrorHandlingUniformity.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/ErrorHandlingUniformity.test.tsx)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | **SIM (P004-BE liberado)** |
| **P004-BE** | Contrato MarketplaceAdapter, Registro de Capacidades, Evidências e Camada de Resiliência | **`APPROVED`** | 18/11/2026        | [MarketplaceAdapterResilienceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/marketplaceadapters/MarketplaceAdapterResilienceTest.java)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     | **SIM (P004-FE liberado)** |
| **P004-FE** | Interface de Gestão de Conexões Oficiais, Capacidades e Evidências                       | **`APPROVED`** | 18/11/2026        | [MarketplaceIntegrationsView.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/MarketplaceIntegrationsView.test.tsx)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        | **SIM (P005-BE liberado)** |
| **P005-BE** | Integração Oficial OAuth 2.0 Mercado Livre, PKCE, Cifragem KMS e Rotação Segura          | **`APPROVED`** | 18/11/2026        | [MercadoLivreOAuthIntegrationTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/marketplaceconnection/mercadolivre/MercadoLivreOAuthIntegrationTest.java)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | **SIM (P005-FE liberado)** |
| **P005-FE** | Fluxo Oficial de Conexão com Mercado Livre (OAuth 2.0 PKCE UI)                           | **`APPROVED`** | 18/11/2026        | [MeliConnectFlow.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/MeliConnectFlow.test.tsx)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | **SIM (P006-BE liberado)** |
| **P006-BE** | Integração Shopee Open Platform v2, Assinatura HMAC, Webhooks e Políticas                | **`APPROVED`** | 18/11/2026        | [ShopeeOpenPlatformIntegrationTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/marketplaceconnection/shopee/ShopeeOpenPlatformIntegrationTest.java)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | **SIM (P006-FE liberado)** |
| **P006-FE** | Fluxo Oficial de Conexão com Shopee (Open Platform v2 UI)                                | **`APPROVED`** | 18/11/2026        | [ShopeeConnectFlow.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/ShopeeConnectFlow.test.tsx)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            | **SIM (P007-BE liberado)** |
| **P007-BE** | Catálogo Canônico, Anúncios, Mapeamentos Externos e Sincronização Resiliente             | **`APPROVED`** | 18/11/2026        | [CatalogListingSyncServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/catalog/CatalogListingSyncServiceTest.java), [PostgresCatalogRlsIntegrationTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/catalog/PostgresCatalogRlsIntegrationTest.java)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            | **SIM (P007-FE liberado)** |
| **P007-FE** | Catálogo Responsivo, Busca, Filtros Combináveis e Detalhe de Anúncio                     | **`APPROVED`** | 18/11/2026        | [CatalogAndListingsView.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/CatalogAndListingsView.test.tsx)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | **SIM (P008-BE liberado)** |
| **P008-BE** | Ingestão de Pedidos e Itens Próprios, Reconciliação, Outbox e RLS                        | **`APPROVED`** | 18/11/2026        | [OrderSyncServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/orderingestion/OrderSyncServiceTest.java), [PostgresOrdersRlsIntegrationTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/orderingestion/PostgresOrdersRlsIntegrationTest.java)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | **SIM (P008-FE liberado)** |
| **P008-FE** | Gestão e Detalhes de Pedidos com Filtros por Canal, Status e Incompletos                 | **`APPROVED`** | 18/11/2026        | [OrdersView.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/OrdersView.test.tsx), [AppShell.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/AppShell.test.tsx)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | **SIM (P009-BE liberado)** |
| **P009-BE** | Modelo Comum para Observações de Mercado, Proveniência, Deduplicação e Matching          | **`APPROVED`** | 18/11/2026        | [MarketObservationServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/marketintelligence/MarketObservationServiceTest.java), [ProductMatchingServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/marketintelligence/ProductMatchingServiceTest.java), [PostgresMarketIntelligenceRlsIntegrationTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/marketintelligence/PostgresMarketIntelligenceRlsIntegrationTest.java)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | **SIM (P009-FE liberado)** |
| **P009-FE** | Exibição de Origem, Evidências, Confiança e Explicação de Dados na UI                    | **`APPROVED`** | 18/11/2026        | [MarketIntelligenceView.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/MarketIntelligenceView.test.tsx), [AppShell.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/AppShell.test.tsx)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | **SIM (P010-BE liberado)** |
| **P010-BE** | Pesquisa de Mercado e Concorrência Autorizada, Registro de Fontes e Cache                | **`APPROVED`** | 18/11/2026        | [docs/specs/P010-BE-market-search/spec.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/specs/P010-BE-market-search/spec.md), [MarketProductSearchServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/competitorresearch/MarketProductSearchServiceTest.java), [MarketSearchControllerTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/competitorresearch/MarketSearchControllerTest.java), [PostgresMarketSearchRlsIntegrationTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/competitorresearch/PostgresMarketSearchRlsIntegrationTest.java)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | **SIM (P010-FE liberado)** |
| **P010-FE** | Interface de Pesquisa Autorizada, Políticas de Fontes e Fallback Seguro                  | **`APPROVED`** | 18/11/2026        | [docs/specs/P010-FE-market-search-view/spec.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/specs/P010-FE-market-search-view/spec.md), [MarketSearchView.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/MarketSearchView.test.tsx)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | **SIM (P011-BE liberado)** |
| **P011-BE** | Comparação de Listings Equivalentes, Variação Temporal e Histórico Não-Inventado         | **`APPROVED`** | 18/11/2026        | [docs/specs/P011-BE-listing-comparison/spec.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/specs/P011-BE-listing-comparison/spec.md), [ListingEquivalenceServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/competitorresearch/comparison/ListingEquivalenceServiceTest.java), [ListingPriceComparisonServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/competitorresearch/comparison/ListingPriceComparisonServiceTest.java), [ListingPriceHistoryServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/competitorresearch/comparison/ListingPriceHistoryServiceTest.java), [ListingComparisonControllerTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/competitorresearch/comparison/ListingComparisonControllerTest.java), [PostgresListingComparisonRlsIntegrationTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/competitorresearch/comparison/PostgresListingComparisonRlsIntegrationTest.java)                                                                                                                                                                                                                                                                                                                                                                                                       | **SIM (P011-FE liberado)** |
| **P011-FE** | Interface de Comparação de Listings, Histórico Real e Alertas de Incompatibilidade       | **`APPROVED`** | 18/11/2026        | [docs/specs/P011-FE-listing-comparison-view/spec.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/specs/P011-FE-listing-comparison-view/spec.md), [ListingComparisonView.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/ListingComparisonView.test.tsx), [ListingPriceHistoryDrawer.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/ListingPriceHistoryDrawer.test.tsx)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | **SIM (P012-BE liberado)** |
| **P012-BE** | Grupos por Tenant, Itens Monitorados e Snapshots Históricos Seguros                      | **`APPROVED`** | 18/11/2026        | [docs/specs/P012-BE-product-monitoring/spec.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/specs/P012-BE-product-monitoring/spec.md), [MonitoringGroupServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/productmonitoring/service/MonitoringGroupServiceTest.java), [MonitoredItemServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/productmonitoring/service/MonitoredItemServiceTest.java), [MonitoredItemSnapshotServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/productmonitoring/service/MonitoredItemSnapshotServiceTest.java), [MonitoringSchedulerPolicyValidatorTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/productmonitoring/service/MonitoringSchedulerPolicyValidatorTest.java), [MonitoringRetentionServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/productmonitoring/service/MonitoringRetentionServiceTest.java), [ProductMonitoringControllerTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/productmonitoring/ProductMonitoringControllerTest.java), [PostgresProductMonitoringRlsIntegrationTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/productmonitoring/PostgresProductMonitoringRlsIntegrationTest.java) | **SIM (P012-FE liberado)** |
| **P012-FE** | Interface de Grupos de Monitoramento, Snapshots e Alertas                                | **`APPROVED`** | 18/11/2026        | [docs/specs/P012-FE-product-monitoring-view/spec.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/specs/P012-FE-product-monitoring-view/spec.md), [ProductMonitoringView.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/ProductMonitoringView.test.tsx), [MonitoringTimelineDrawer.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/MonitoringTimelineDrawer.test.tsx), [AppShell.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/AppShell.test.tsx)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | **SIM (P013 liberado)**    |
| **P013-BE** | Snapshots Idempotentes, Detecção de Alterações e Resiliência de Coleta                   | **`APPROVED`** | 18/11/2026        | [docs/specs/P013-BE-idempotent-snapshots/spec.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/specs/P013-BE-idempotent-snapshots/spec.md), [MonitoredItemSnapshotServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/productmonitoring/service/MonitoredItemSnapshotServiceTest.java), [ProductMonitoringControllerTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/productmonitoring/ProductMonitoringControllerTest.java), [PostgresProductMonitoringRlsIntegrationTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/productmonitoring/PostgresProductMonitoringRlsIntegrationTest.java)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     | **SIM (P013-FE liberado)** |
| **P013-FE** | Gráfico Histórico, Tooltips de Fonte e Lacunas Visíveis                                  | **`APPROVED`** | 19/11/2026        | [docs/specs/P013-FE-price-history-chart/spec.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/specs/P013-FE-price-history-chart/spec.md), [PriceHistoryChart.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/PriceHistoryChart.test.tsx), [MonitoringTimelineDrawer.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/MonitoringTimelineDrawer.test.tsx)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | **SIM (P014-BE liberado)** |
| **P014-BE** | Cadastro de Custos por Produto/Variante/Empresa e Regras de Taxas                        | **`APPROVED`** | 19/11/2026        | [docs/specs/P014-BE-product-costs-and-fee-rules/spec.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/specs/P014-BE-product-costs-and-fee-rules/spec.md), [ProductCostServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/pricing/ProductCostServiceTest.java), [FeeRuleServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/pricing/FeeRuleServiceTest.java), [FeeResolutionServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/pricing/FeeResolutionServiceTest.java), [PricingAndCostsControllerTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/pricing/PricingAndCostsControllerTest.java), [PostgresPricingRlsIntegrationTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/pricing/PostgresPricingRlsIntegrationTest.java)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | **SIM (P014-FE liberado)** |
| **P014-FE** | Interface de Gestão de Custos, Tabela de Taxas e Simulador                               | **`APPROVED`** | 19/11/2026        | [docs/specs/P014-FE-pricing-and-costs-view/spec.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/specs/P014-FE-pricing-and-costs-view/spec.md), [ProductCostForm.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/pricing/ProductCostForm.test.tsx), [FeeRuleForm.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/pricing/FeeRuleForm.test.tsx), [FeeSimulator.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/pricing/FeeSimulator.test.tsx), [PricingAndCostsView.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/pricing/PricingAndCostsView.test.tsx), [AppShell.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/AppShell.test.tsx)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        | **SIM (P015-BE liberado)** |
| **P015-BE** | Serviço Determinístico de Preço, Margem de Contribuição e Preço de Equilíbrio            | **`APPROVED`** | 19/11/2026        | [docs/specs/P015-BE-deterministic-pricing-engine/spec.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/specs/P015-BE-deterministic-pricing-engine/spec.md), [PricingEngineServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/pricing/PricingEngineServiceTest.java), [PricingAndCostsControllerTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/pricing/PricingAndCostsControllerTest.java)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              | **SIM (P015-FE liberado)** |
| **P015-FE** | Calculadora Interativa de Precificação, Detalhamento de Margens e Cenários               | **`APPROVED`** | 19/11/2026        | [docs/specs/P015-FE-pricing-calculator-view/spec.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/specs/P015-FE-pricing-calculator-view/spec.md), [PricingCalculator.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/pricing/PricingCalculator.test.tsx), [PricingAndCostsView.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/pricing/PricingAndCostsView.test.tsx)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | **SIM (P016-BE liberado)** |
| **P016-BE** | Entidades Company/BusinessUnit com CNPJ Sintático e Vínculo de Contas de Marketplace     | **`APPROVED`** | 19/11/2026        | [docs/specs/P016-BE-company-and-account-link/spec.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/specs/P016-BE-company-and-account-link/spec.md), [CompanyServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/company/CompanyServiceTest.java), [CompanyAccountLinkServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/company/CompanyAccountLinkServiceTest.java), [CompanyControllerTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/company/CompanyControllerTest.java), [CnpjValidatorTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/company/CnpjValidatorTest.java), [PostgresCompanyRlsIntegrationTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/company/PostgresCompanyRlsIntegrationTest.java)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | **SIM (P016-FE liberado)** |
| **P016-FE** | Cadastro de Empresa e Vínculo de Contas de Marketplace com Proteção de CNPJ              | **`APPROVED`** | 19/11/2026        | [docs/specs/P016-FE-company-and-account-link-view/spec.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/specs/P016-FE-company-and-account-link-view/spec.md), [CompaniesAndAccountsView.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/company/CompaniesAndAccountsView.test.tsx), [cnpj.test.ts](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/utils/cnpj.test.ts), [AppShell.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/AppShell.test.tsx)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           | **SIM (P017-BE liberado)** |
| **P017-BE** | Fatos Financeiros por Pedido/Item, Reconciliação e Trilha de Cálculo                     | **`APPROVED`** | 19/11/2026        | [docs/specs/P017-BE-financial-facts-reconciliation/spec.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/specs/P017-BE-financial-facts-reconciliation/spec.md), [OrderFinancialFactServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/financialanalytics/OrderFinancialFactServiceTest.java), [OrderFinancialFactControllerTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/financialanalytics/OrderFinancialFactControllerTest.java), [PostgresFinancialFactsRlsIntegrationTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/financialanalytics/PostgresFinancialFactsRlsIntegrationTest.java)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | **SIM (P017-FE liberado)** |
| **P017-FE** | Painel Financeiro, Filtros Combináveis, Detalhamento de Componentes e Alertas            | **`APPROVED`** | 19/11/2026        | [docs/specs/P017-FE-financial-dashboard-view/spec.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/specs/P017-FE-financial-dashboard-view/spec.md), [FinancialFactsDashboardView.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/financial/FinancialFactsDashboardView.test.tsx), [AppShell.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/AppShell.test.tsx)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          | **SIM (P018-BE liberado)** |
| **P018-BE** | Inventário de Métricas Oficiais por Canal, Catálogo Documentado e Ingestão               | **`APPROVED`** | 19/11/2026        | [docs/specs/P018-BE-official-metrics-inventory/spec.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/specs/P018-BE-official-metrics-inventory/spec.md), [AccountMetricIngestionServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/listinganalytics/AccountMetricIngestionServiceTest.java), [MetricFormulaEngineTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/listinganalytics/MetricFormulaEngineTest.java), [PostgresConnectedAccountMetricsRlsIntegrationTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/listinganalytics/PostgresConnectedAccountMetricsRlsIntegrationTest.java)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | **SIM (P018-FE liberado)** |
| **P018-FE** | Dashboard de Analytics por Anúncio, Período, Comparação Temporal e Aviso de Indisponível | **`APPROVED`** | 19/11/2026        | [docs/specs/P018-FE-listing-analytics-dashboard/spec.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/specs/P018-FE-listing-analytics-dashboard/spec.md), [ListingAnalyticsDashboardView.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/listinganalytics/ListingAnalyticsDashboardView.test.tsx), [AppShell.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/AppShell.test.tsx)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | **SIM (P019-BE liberado)** |
| **P019-BE** | Gateway Multi-Provider com Secrets, Defesa Prompt Injection e Tool Calling Read-Only     | **`APPROVED`** | 19/11/2026        | [docs/specs/P019-BE-ai-gateway-and-readonly-tools/spec.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/specs/P019-BE-ai-gateway-and-readonly-tools/spec.md), [DefaultAiSecretManagerTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/aiassistant/gateway/DefaultAiSecretManagerTest.java), [PromptInjectionDefenseServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/aiassistant/security/PromptInjectionDefenseServiceTest.java), [AiToolRegistryAndInternalToolsTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/aiassistant/tools/AiToolRegistryAndInternalToolsTest.java), [AiGatewayServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/aiassistant/service/AiGatewayServiceTest.java), [AiAssistantControllerTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/aiassistant/AiAssistantControllerTest.java), [PostgresAiAuditRlsIntegrationTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/aiassistant/PostgresAiAuditRlsIntegrationTest.java) | **SIM (P019-FE liberado)** |

---

---

## 1.1 Gate de Manutenção

| ID          | Etapa                               | Status         | Data da Avaliação | Evidência                                   | Resultado                                               |
| ----------- | ----------------------------------- | -------------- | ----------------- | ------------------------------------------- | ------------------------------------------------------- |
| **JAVA-25** | Upgrade do runtime Java para LTS 25 | **`APPROVED`** | 18/11/2026        | `backend/pom.xml`; `./mvnw clean verify -q` | Java 25.0.5; Spring Boot 3.5.16; 11/11 testes aprovados |

### Transição: Manutenção JAVA-25 → `APPROVED`

- **Data/Hora:** 18/11/2026 08:49 BRT
- **Critérios Validados:**
  1. Maven configura Java 25 (`java.version=25`) e o build executa no runtime Java 25.0.5.
  2. Spring Boot 3.5.16 é compatível com Java 25 conforme [requisitos oficiais](https://docs.spring.io/spring-boot/3.5/system-requirements.html).
  3. ArchUnit 1.5.1 está alinhado para ler bytecode Java 25, inclusive a dependência transitiva usada pelo Spring Modulith.
  4. `cd backend && ./mvnw clean verify -q` concluído com sucesso: 11 testes, zero falhas, erros ou skips.
  5. CI já usa JDK 25; as referências atuais da documentação técnica e do shell foram sincronizadas.
- **Resultado:** manutenção aprovada; o fluxo sequencial de prompts P001-FE → P002-BE permanece inalterado.

---

## 2. Histórico de Transições e Auditoria dos Gates

### Transição: PROMPT P007-FE $\rightarrow$ `APPROVED`

- **Data/Hora:** 18/11/2026 12:46 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. **Tabela Responsiva e Adaptativa (Desktop & Mobile):** Implementação de `CatalogAndListingsView.tsx` com layout adaptativo: tabela estruturada e rolável no desktop/tablet e cards interativos compactos no mobile, com touch targets $\ge 44\text{px}$.
  2. **Paginação Server-Side Completa:** Navegação paginada via `GET /api/v1/catalog/listings` e BFF `/api/catalog/listings` com parâmetros `page`, `size` (10, 25, 50), `totalPages`, `totalElements` e `last`.
  3. **Filtros Combináveis e Busca em Tempo Real:** Filtragem dinâmica simultânea por Marketplace (`MERCADO_LIVRE`, `SHOPEE`), Conta Conectada (`sellerAccountId`), Status (`ACTIVE`, `PAUSED`, `CLOSED`) e busca textual (título, SKU, external ID) com botão "Limpar Filtros".
  4. **Resiliência e Estados de Interface (Loading, Empty, Error):** Skeleton de carregamento com `role="status"` acessível, `EmptyState` com `role="region"` com orientação clara de ação, e tratamento de erro sanitizado para 401, 403 e 500 sem expor stacktraces ou segredos.
  5. **Painel de Detalhes (`ListingDetailDrawer.tsx`):** Drawer lateral responsivo com tecla Escape, link oficial seguro (`rel="noopener noreferrer"`), produto canônico mestre vinculado e metadados de proveniência (`origin`, `schema_version = 'v1'`, `synced_at`).
  6. **Tratamento Explícito de Dados Ausentes:** Garantia de que campos nulos ou parciais (sem SKU mestre, sem SKU no canal, sem ID de variação, sem categoria/marca) renderizam badges neutras claras ("Não informado", "Sem SKU", "Sem variação", "Não vinculado"), nunca quebrando ou deixando texto vazio.
  7. **Isolamento Multi-Tenant Estrito:** Dados estritamente limitados à organização ativa. Testes comprovam que alternância de tenant via `TenantSwitcher` ou requisições de outros tenants não revelam registros cruzados (HTTP 404 em cross-tenant por ID).
  8. **Build & Testes:** 73/73 testes frontend aprovados (incluindo 10 testes dedicados de catálogo em `CatalogAndListingsView.test.tsx`), 122/122 testes de backend aprovados (incluindo `CatalogControllerTest`), `tsc --noEmit` limpo e build de produção Next.js 15.1.7 gerado com 18 rotas.
- **Resultado:** **`PROMPT P007-FE = APPROVED`**. O prompt subsequente (`P008-BE`) está formalmente liberado e transita para `READY`.

### Transição: PROMPT P007-BE $\rightarrow$ `APPROVED`

- **Data/Hora:** 18/11/2026 12:10 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. **Separação Canônica vs Anúncios de Canal (1:N):** Modelagem estrita de `Product`, `ProductVariant`, `MarketplaceListing` e `SellerAccount`. Um produto canônico centraliza N anúncios de múltiplos canais (Mercado Livre, Shopee).
  2. **Regra de Ouro — Não Deduplicar Apenas por Título:** O motor de conciliação NUNCA assume que anúncios com mesmo título ou título similar pertencem ao mesmo produto. A vinculação requer correspondência estrita por `SKU`, `GTIN` ou mapeamento explícito (`ExternalIdentifierMapping` de tipo `ITEM_ID`). Na ausência de identificadores compatíveis, novos produtos canônicos isolados são criados preventivamente.
  3. **Preservação de Payload Minimizado e Versionado:** O payload original bruto autorizado retornado pela API oficial do canal é preservado de forma higienizada no campo `raw_payload_json`, acompanhado de schema versionado (`schema_version = 'v1'`), canal de origem (`origin = 'MERCADO_LIVRE_ITEMS_API' | 'SHOPEE_OPENAPI_V2'`) e timestamps auditáveis (`synced_at`, `created_at`, `updated_at`).
  4. **Sincronização Paginada com Checkpoint e Retomada:** A entidade `ListingSyncCheckpoint` registra `last_offset`, `total_items_synced`, `sync_status` (`IN_PROGRESS`, `COMPLETED`, `FAILED`) e `error_message`. Em caso de falha de rede/timeout no canal, o processo é retomável a partir do último checkpoint com integridade referencial.
  5. **Idempotência e Reconciliação:** O reprocessamento da mesma página ou anúncio não gera duplicatas (`CONSTRAINT uq_marketplace_listings_external UNIQUE (tenant_id, marketplace, external_listing_id)`). Registros existentes são reconciliados (preço, estoque, status e timestamps atualizados).
  6. **Isolamento Multi-Tenant em Profundidade com PostgreSQL 17 RLS:** Políticas de Row-Level Security (`ENABLE` e `FORCE ROW LEVEL SECURITY`) aplicadas nas 6 tabelas (`seller_accounts`, `products`, `product_variants`, `marketplace_listings`, `external_identifier_mappings`, `listing_sync_checkpoints`) com `SET LOCAL app.current_tenant`. Testes de integração via Testcontainers com usuário `seller_app` (NOBYPASSRLS) comprovaram que leituras, inserções (WITH CHECK violando RLS), updates (0 rows) e deletes (0 rows) entre tenants são categoricamente negados.
  7. **Risco Zero e Ausência de Consultas a Concorrentes:** Operação restrita a anúncios próprios do lojista autorizado via `READ_OWN_CATALOG`. Nenhuma chamada a concorrentes implementada.
  8. **Testes & Cobertura:** 12/12 testes específicos de catálogo (6 testes de serviço/reconciliação/idempotência em `CatalogListingSyncServiceTest` + 6 testes de isolamento RLS real em `PostgresCatalogRlsIntegrationTest`), 117/117 testes backend passando, 3/3 regras modulares ArchUnit verificadas e 62/62 testes frontend em conformidade.
- **Resultado:** **`PROMPT P007-BE = APPROVED`**. O prompt subsequente (`P007-FE`) está formalmente liberado e transita para `READY`.

### Transição: PROMPT P006-FE $\rightarrow$ `APPROVED`

- **Data/Hora:** 18/11/2026 11:34 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. **Consentimento Oficial e Orientações Claras:** Modal `ShopeeConnectModal.tsx` implementado com orientações detalhadas sobre autorização oficial da Shopee Open Platform OpenAPI v2, Shop Level (`v2.product.get_item_list`), métricas oficiais (`v2.analytics`) e sincronização por Webhooks Push.
  2. **Garantia de Não-Escrita (Risco Zero):** Alerta ostensivo assegurando que permissões de escrita/alteração de catálogo e preços (`write`) **NÃO** são solicitadas.
  3. **Segurança Inegociável — Zero Credenciais Solicitadas:** A aplicação nunca solicita, armazena ou transita login, senha, chave secreta ou token da conta da Shopee. Ausência total de campos de senha (`input[type="password"]`).
  4. **Redirecionamento ao Portal Oficial com Assinatura HMAC:** Acionamento de `POST /api/integrations/shopee/authorize` no BFF gera a URL assinada via HMAC-SHA256 (`partner_id`, `timestamp`, `sign`, `redirect`) e redireciona o vendedor com exclusividade para o domínio oficial da Shopee.
  5. **Processamento de Callback sem Exposição de Segredos:** Ao retornar com `code` e `shop_id`, a interface executa imediatamente `window.history.replaceState` para limpar a URL, despacha POST para `/api/integrations/shopee/callback` e atualiza o canal para `ACTIVE` (Conectado). Zero tokens ou segredos permanecem no DOM ou storage do cliente.
  6. **Tratamento de Cancelamento e Erros Sanitizados:** Cancelamento pelo lojista (`error=cancelled`) ou falhas no callback tratadas com mensagens claras e sem expor exceções, stacktraces ou UUIDs técnicos.
  7. **Monitoramento de Webhooks Push em Tempo Real:** Card de informações técnicas destaca a sincronização via Push Mechanism v2 com desautorização atômica automática (Push Code 2).
  8. **Acessibilidade & Responsividade:** Conformidade WCAG 2.2 AA, touch targets $\ge 44\text{px}$, suporte a navegação por teclado (ESC).
  9. **Build & Testes:** 62/62 testes frontend aprovados (incluindo 8 testes dedicados de ShopeeConnectFlow e 9 de MarketplaceIntegrationsView), `tsc --noEmit` limpo e build de produção Next.js 15.1.7 gerado com 16 rotas estáticas/SSR.
- **Resultado:** **`PROMPT P006-FE = APPROVED`**. O prompt subsequente (`P007-BE`) está formalmente liberado e transita para `READY`.

### Transição: PROMPT P006-BE $\rightarrow$ `APPROVED`

- **Data/Hora:** 18/11/2026 11:21 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. **Documentação Oficial Shopee Open Platform OpenAPI v2:** Especificação oficial validada para Partner Authorization, HMAC-SHA256 request signing, atomic single-use refresh token rotation e Webhook Push Mechanism.
  2. **Assinatura HMAC-SHA256 por Requisição:** `ShopeeSignatureUtil` calcula e valida a assinatura criptográfica para endpoints públicos/auth (`partner_id + path + timestamp`) e de shop (`partner_id + path + timestamp + access_token + shop_id`) com chave simétrica `partner_key`.
  3. **Validação Estrita de Webhook Push:** `ShopeeWebhookVerifier` realiza comparação em tempo constante (`MessageDigest.isEqual`) da assinatura no header `Authorization` usando `callback_url + "|" + raw_body`. Requisições inválidas são imediatamente rejeitadas com HTTP 401 (`ShopeeWebhookSignatureException`).
  4. **Tratamento de Push Code 2 (Shop Authorization Canceled):** Notificação oficial de cancelamento da autorização invalida imediatamente a conexão local, expurga os tokens cifrados e transita o status para `REVOKED`.
  5. **Cifragem de Tokens via KMS (AES-256-GCM):** Tokens de acesso e refresh da Shopee são cifrados antes da persistência no PostgreSQL com IV aleatório de 12 bytes e tag de 128 bits. Zero tokens em texto puro em logs ou banco.
  6. **Rotação Segura com Lock Distribuído:** Atualização de access/refresh token sob `DistributedConnectionLock` impedindo concorrência e quebra da cadeia de refresh de uso único (single-use token rotation).
  7. **Política Fail-Closed & Bloqueio de Não Autorizados:** Quando o parceiro não possui credenciais ativas homologadas (`partner_id=0` ou desabilitado), o `ShopeeMarketplaceAdapter` opera em `UNVERIFIED` e rejeita qualquer chamada real com `UnverifiedCapabilityException`.
  8. **Tratamento de Quotas e Rate Limiting:** Tratamento padronizado de HTTP 429 (`ShopeeRateLimitException`) com respeito a retry e backoff.
  9. **Zero Scraping & Zero Competitor Revenue:** `COMPETITOR_SALES_VOLUME` mantido como `UNSUPPORTED`.
  10. **Testes & Arquitetura:** 105/105 testes de backend aprovados (25 testes dedicados da Shopee com cobertura de HMAC, webhook, lock concorrente, controller e adapter), testes modulares ArchUnit com 100% de sucesso, e regressão frontend de 54/54 testes passando.
- **Resultado:** **`PROMPT P006-BE = APPROVED`**. O prompt subsequente (`P006-FE`) está formalmente liberado e transita para `READY`.

### Transição: PROMPT P005-FE $\rightarrow$ `APPROVED`

- **Data/Hora:** 18/11/2026 10:58 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. **Redirecionamento ao Domínio Oficial:** O fluxo aciona `POST /api/integrations/mercadolivre/authorize` e redireciona o lojista com exclusividade para o domínio oficial `auth.mercadolivre.com.br` com parâmetros criptográficos PKCE (`code_challenge`) e anti-CSRF (`state`).
  2. **Transparência e Explicação de Permissões:** Modal de consentimento (`MeliConnectModal.tsx`) detalha os escopos solicitados (`read` e `offline_access`) em linguagem clara para o vendedor.
  3. **Garantia de Não-Escrita (Risco Zero):** Alerta destacado de que a permissão `write` **NÃO** é solicitada, garantindo categoricamente que a plataforma nunca alterará preços, anúncios ou inventário.
  4. **Segurança Inegociável — Zero Senhas Solicitadas:** A aplicação nunca solicita, armazena ou manipula senhas do Mercado Livre; ausência total de inputs do tipo password.
  5. **Callback sem Exposição de Tokens:** Processamento automático de callback limpa imediatamente a URL via `window.history.replaceState`, despacha `code` e `state` via POST ao BFF/backend, e garante ausência total de tokens ou segredos no DOM ou storage do navegador.
  6. **Status Reflete Backend:** Conexão atualizada para `ACTIVE` (Conectado) e persistida de forma consistente.
  7. **Falhas Compreensíveis:** Tratamento amigável e desprovido de stacktraces para cenários de recusa do usuário (`access_denied`), expiração de state anti-CSRF e falhas de rede.
  8. **Responsividade & Acessibilidade:** Conformidade WCAG 2.2 AA, touch targets $\ge 44\text{px}$, suporte a navegação por teclado (ESC).
  9. **Build & Testes:** 54/54 testes frontend aprovados (incluindo 7 testes de fluxo OAuth e 9 testes de integrações), `tsc --noEmit` sem erros e `next build` 15.1.7 gerando build estático/SSR limpo.
- **Resultado:** **`PROMPT P005-FE = APPROVED`**. O prompt subsequente (`P006-BE`) está formalmente liberado e transita para `READY`.

### Transição: PROMPT P005-BE $\rightarrow$ `APPROVED`

- **Data/Hora:** 18/11/2026 10:42 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. Integração com o programa oficial de desenvolvedores do Mercado Livre (`developers.mercadolibre.com.br`) implementada conforme documentação oficial vigente.
  2. Protocolo OAuth 2.0 Authorization Code Flow com PKCE (RFC 7636) implementado com `code_verifier` de alta entropia e `code_challenge` S256 codificado em Base64URL.
  3. Proteção anti-CSRF e anti-Session Fixation estrita: parâmetro `state` criptograficamente seguro com TTL de 10 minutos e consumo atômico de uso único (one-time use). Estados inválidos, adulterados ou expirados são sumariamente rejeitados com HTTP 400 (`InvalidStateCsrfException`).
  4. Cifragem simétrica de tokens via KMS/Secret Manager Pattern: tokens de acesso (`access_token`) e renovação (`refresh_token`) cifrados com AES-256-GCM (IV aleatório de 12 bytes e authentication tag de 128 bits). Zero tokens em texto puro persistidos no banco de dados.
  5. Rotação segura de refresh token com lock distribuído/sincronizado (`DistributedConnectionLock`): 10 threads concorrentes executam sob exclusão mútua e verificação de frescor (Double-Check), impedindo categoricamente duplicação de chamadas de refresh e a quebra da cadeia (`invalid_grant`).
  6. Revogação de conexão: invalidação atômica das credenciais locais, remoção de tokens e transição de status para `REVOKED`, impedindo qualquer nova obtenção de access token.
  7. Princípio do Menor Privilégio: escopos solicitados estritamente limitados a `read` e `offline_access`. Escopo `write` categoricamente bloqueado por governança do MVP (`UnauthorizedScopeException`).
  8. Auditoria limpa e sem vazamento de segredos: todos os eventos de ciclo de vida (`OAUTH_AUTHORIZE_INITIATED`, `OAUTH_CALLBACK_SUCCESS`, `OAUTH_CALLBACK_INVALID_STATE`, `TOKEN_REFRESH_SUCCESS`, `TOKEN_REVOCATION_SUCCESS`) auditados sem inclusão de tokens ou credenciais.
  9. Suíte de testes do backend com 80/80 testes aprovados (100% de sucesso) e 47/47 testes de frontend mantidos com 100% de sucesso.
- **Resultado:** **`PROMPT P005-BE = APPROVED`**. O prompt subsequente (`P005-FE`) está formalmente liberado e transita para `READY`.

### Transição: PROMPT P004-FE $\rightarrow$ `APPROVED`

- **Data/Hora:** 18/11/2026 10:25 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. Tela de integrações e capacidades oficiais (`MarketplaceIntegrationsView.tsx`) implementada e integrada ao shell da aplicação no módulo `STORES` ("Lojas & Marketplaces").
  2. Estados visuais padronizados e inconfundíveis para as capacidades:
     - `SUPPORTED`: Verde/Checkmark (Oficialmente Homologado pela API do marketplace).
     - `RESTRICTED`: Âmbar/Alerta (Restrições de cota ou deliberadamente desabilitada no MVP para garantia de risco zero, ex: operações de escrita).
     - `UNSUPPORTED`: Carmim/Bloqueio (Não disponibilizado pelo canal, ex: faturamento e vendas de terceiros; scraping expressamente proibido).
     - `UNVERIFIED`: Cinza/Cadeado (Em validação técnica documental; sob a regra Fail-Closed, jamais é apresentado como disponível para uso).
  3. Transparência operacional completa: exibição de data da última verificação documental, escopos OAuth homologados (`read`, `offline_access`), quotas de requisição (Token Bucket com jitter) e URLs oficiais de documentação.
  4. Princípio de Menor Privilégio e Ações Permitidas Exclusivas:
     - As únicas ações operacionais autorizadas ao usuário são estritamente `Conectar`, `Atualizar Status` e `Revogar`.
     - Zero botões de escrita destrutiva, manipulação arbitrária de preços ou raspagem de dados de concorrentes.
     - Canais `UNVERIFIED` (Amazon SP-API, TikTok Shop) não expõem botões ativos de conexão ou execução.
  5. Higiene absoluta de segredos: nenhum token OAuth (`access_token`, `refresh_token`), hash HMAC ou chave de API é renderizado no DOM ou em atributos HTML.
  6. Sanitização de erros ativa: alertas e feedbacks processados por `sanitizeErrorMessage()`, expurgando UUIDs técnicos de tenants e stacktraces internos.
  7. Usabilidade Mobile e Acessibilidade: touch targets com altura mínima de 44px (`min-h-[44px]`), semântica ARIA (`role="region"`, `role="list"`, `role="alert"`, `aria-selected`), sem overflow horizontal.
  8. Suíte de testes do frontend com 47/47 testes aprovados (100% de sucesso), typecheck `tsc --noEmit` limpo e build de produção Next.js 15 compilado com sucesso.
- **Resultado:** **`PROMPT P004-FE = APPROVED`**. O prompt subsequente (`P005-BE`) está formalmente liberado e transita para `READY`.

### Transição: PROMPT P004-BE $\rightarrow$ `APPROVED`

- **Data/Hora:** 18/11/2026 10:04 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. Contrato `MarketplaceAdapter` e classe base `AbstractMarketplaceAdapter` implementados com tipagem forte e desacoplamento total de canais específicos.
  2. Matriz de capacidades com estados `SUPPORTED`, `UNSUPPORTED`, `RESTRICTED` e `UNVERIFIED` governada pelo `CapabilityRegistry` sob política Fail-Closed: capacidades sem evidência documental comprovada são rebaixadas para `UNVERIFIED` e impedidas de executar.
  3. Cadastro rigoroso de evidências (`CapabilityEvidence`) contendo URL oficial, data de verificação, escopos OAuth necessários, quotas de rate limit, restrições contratuais, versão da documentação e engenheiro responsável.
  4. Esteira de resiliência integrada (`ResiliencePipeline`):
     - **Kill Switch** global e por canal com interrupção instantânea (`KillSwitchActivatedException`);
     - **Rate Limiting** determinístico via Token Bucket com suporte a quotas estritas;
     - **Retry Policy com Full Jitter** e respeito mandatória ao cabeçalho `Retry-After`, sem exceder a política máxima configurada;
     - **Circuit Breaker** com máquina de estados atômica (`CLOSED`, `OPEN`, `HALF_OPEN`) abrindo após 3 falhas consecutivas e recuperando com sucesso em `HALF_OPEN`;
     - **Bulkhead** baseado em semáforos para isolamento de concorrência com Fail-Fast (`BulkheadFullException`);
     - **Timeout** delimitado por duração configurada (`MarketplaceTimeoutException`);
     - **Idempotência** em memória com deduplicação de chamadas idênticas via chave única.
  5. Conector de testes `FakeMarketplaceAdapter` implementado para validação unitária e de integração sem tocar na rede externa ou presumi endpoints inexistentes.
  6. Mascaramento automático de dados sensíveis e credenciais nos logs (`AdapterLogSanitizer`) substituindo tokens e senhas por `[REDACTED]`.
  7. Suíte de testes do backend com 64/64 testes aprovados (100% de sucesso) e 37/37 testes de frontend mantidos com 100% de sucesso.
- **Resultado:** **`PROMPT P004-BE = APPROVED`**. O prompt subsequente (`P004-FE`) está formalmente liberado e transita para `READY`.

### Transição: PROMPT P003-FE $\rightarrow$ `APPROVED`

- **Data/Hora:** 18/11/2026 09:45 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. Tratamento padronizado de erros 403 e 404 implementado via `lib/errorHandler.ts`, `components/AccessDeniedFeedback.tsx` e `app/not-found.tsx` com mensagem neutra estrita ("Recurso não encontrado ou acesso restrito"), eliminando completamente vetores de enumeração de recursos alheios e timing attacks.
  2. Proteção contra manipulação de tenant via URL implementada no `middleware.ts`: qualquer tentativa de injetar query parameters (`?tenantId=...`, `?orgId=...`, `?tenant=...`) é imediatamente interceptada, expurgada e redirecionada para a URL canônica limpa, mantendo o contexto governado exclusivamente pela sessão segura HttpOnly e memberships validadas.
  3. Ocultação absoluta de UUIDs técnicos de tenant na UI: auditoria e testes unitários comprovam que nenhum identificador técnico no padrão UUID v4 é renderizado para o usuário; a interface exibe estritamente nomes amigáveis das empresas e papéis associados.
  4. Estados de sessão e feedback acessível (WCAG 2.2 AA): componente `SessionBanner` alerta com `role="alert"` e `aria-live="assertive"` quando a sessão expira (HTTP 401), oferecendo ação rápida para login sem exposição de detalhes internos.
  5. Modo Fail-Closed e sanitização de exceções em `app/error.tsx`: mensagens de erro globais são filtradas por `sanitizeErrorMessage`, impedindo que traces, nomes de classes internas ou UUIDs sejam expostos ao usuário.
  6. Suíte de testes do frontend com 37/37 testes aprovados (100% de sucesso) e build Next.js 15 gerado sem erros de compilação, lint ou tipo.
- **Resultado:** **`PROMPT P003-FE = APPROVED`**. O prompt subsequente (`P004-BE`) está formalmente liberado e transita para `READY`.

### Transição: PROMPT P003-BE $\rightarrow$ `APPROVED`

- **Data/Hora:** 18/11/2026 09:35 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. `tenant_id` obrigatório nas tabelas tenant-scoped (`marketplace_connections`, `tenant_documents`) com integridade referencial estrita (`REFERENCES organizations(id) ON DELETE CASCADE`) e índices compostos de alto desempenho (`idx_conn_tenant_status`, `idx_conn_tenant_marketplace`, `idx_docs_tenant_created`).
  2. Defesa em Profundidade ativada com PostgreSQL Row-Level Security (`ENABLE ROW LEVEL SECURITY` e `FORCE ROW LEVEL SECURITY`) via migration `V3_1__postgresql_rls_defense.sql`.
  3. Políticas RLS estritas com verificação dupla (`USING` e `WITH CHECK`) ancoradas em `NULLIF(current_setting('app.current_tenant', true), '')::uuid`. Sem tenant ativo, zero registros são expostos ou aceitos.
  4. Sessão de banco transacional atômica via `TenantDatabaseSession` (`SELECT set_config('app.current_tenant', ?, true)`), mantendo o isolamento estritamente no escopo da transação e limpando conexões devolvidas ao pool HikariCP.
  5. Princípio do Menor Privilégio: usuário da aplicação `seller_app` configurado como `NOSUPERUSER NOBYPASSRLS`, garantindo aplicação infalível de RLS em ambiente PostgreSQL 17 real.
  6. Isolamento estrito em infraestrutura auxiliar:
     - `TenantJobExecutor`: execução segura de jobs em background com limpeza no `finally`.
     - `TenantCacheKeyGenerator`: chaves de cache namespaced `tenant:{tenantId}:{entity}:{identifier}` prevenindo colisões no Redis.
     - `TenantQueueValidator`: validação e extração mandatória de cabeçalho `x-tenant-id` para filas RabbitMQ com bloqueio de cross-tenant.
     - `TenantStoragePathResolver`: caminhos seguros `/tenants/{tenantId}/...` com bloqueio terminante contra path traversal (`..`, `\`) e acesso a arquivos alheios.
  7. Sanitização de segredos na auditoria via `AuditSanitizer`: mascaramento determinístico com `[REDACTED]` de senhas, access tokens, refresh tokens e JWTs soltos antes da persistência no banco e emissão em logs.
  8. Suíte de testes de integração com PostgreSQL 17 real via Testcontainers (`PostgresMultiTenantRlsIntegrationTest`):
     - Leitura cross-tenant negada (retorna vazio).
     - Criação cross-tenant negada (RLS `WITH CHECK` lança violação de integridade).
     - Alteração cross-tenant negada (0 linhas afetadas, dados originais preservados).
     - Exclusão cross-tenant negada (0 linhas afetadas, dados originais preservados).
     - Pesquisa e filtros cross-tenant negados (somente dados do próprio tenant retornados).
     - Acesso a storage cross-tenant negado (`TenantAccessDeniedException`).
     - Reutilização limpa de conexão no pool HikariCP (zero vazamento de contexto).
     - Sanitização comprovada em registro real de auditoria no banco.
  9. Total de 49/49 testes de backend aprovados (100% de sucesso) e 28/28 testes de frontend mantidos com 100% de sucesso.
- **Resultado:** **`PROMPT P003-BE = APPROVED`**. O prompt subsequente (`P003-FE`) está formalmente liberado e transita para `READY`.

### Transição: PROMPT P002-FE $\rightarrow$ `APPROVED`

- **Data/Hora:** 18/11/2026 09:19 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. Tela de login (`/login`) acessível (WCAG 2.2 AA) implementada com formulários semânticos, atributos ARIA e alertas dinâmicos (401 credenciais inválidas, 429 brute force lockout).
  2. Política de Cookies HttpOnly estrita: tokens sensíveis gravados exclusivamente em cookies `HttpOnly`, `SameSite=Lax`, `Path=/` via Route Handlers BFF Next.js (`/api/auth/*`), com zero tokens em `localStorage`.
  3. Proteção de rotas em middleware (`middleware.ts`): requisições anônimas ao dashboard `/` são redirecionadas com status 307 para `/login`. Usuários autenticados acessando `/login` são redirecionados para `/`.
  4. Detecção e tratamento de sessão expirada: expiração de token direciona suavemente para reautenticação com aviso informativo acessível.
  5. Isolamento visual absoluto de tenants: `TenantSwitcher` exibe e permite alternar exclusivamente entre as memberships legítimas do usuário autenticado, sem qualquer exposição de terceiros.
  6. Suíte de testes do frontend com 28/28 testes aprovados (100% de sucesso) e build de produção Next.js 15 gerado sem erros.
- **Resultado:** **`PROMPT P002-FE = APPROVED`**. O prompt subsequente (`P003-BE`) está formalmente liberado e transita para `READY`.

### Transição: PROMPT P002-BE $\rightarrow$ `APPROVED`

- **Data/Hora:** 18/11/2026 09:07 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. Identidade mínima, autenticação segura e tokens JWT (RFC 7519 HMAC-SHA256) implementados com expiração configurada (900s).
  2. Derivação mandatória de tenant exclusivamente a partir de credenciais e memberships validadas, nunca de parâmetros ou headers arbitrários.
  3. Contenção rigorosa de spoofing: header `X-Tenant-Id` para tenant não associado resulta em HTTP 403 Forbidden e auditoria imediata (`TENANT_SPOOFING_ATTEMPT`).
  4. Query parameters arbitrários (`?tenantId=...`) são categoricamente ignorados para resolução de contexto tenant-scoped.
  5. Proteção contra força bruta (Brute Force Protection) com bloqueio temporário em HTTP 429 Too Many Requests após 5 falhas consecutivas e auditoria (`BRUTE_FORCE_BLOCKED`).
  6. Trilha de auditoria persistente e estruturada via `AuditService` em transação autônoma (`REQUIRES_NEW`), garantindo que auditorias de falha/spoofing nunca sejam descartadas por rollback.
  7. Endpoints tenant-scoped e rota de contexto (`/api/v1/tenancy/current`) operando sob política Fail-Closed (acesso negado por padrão a requisições anônimas ou tokens sem tenant).
  8. Suíte completa de testes automatizados com 31/31 testes aprovados (100% de sucesso), incluindo testes de tokens, brute force, anti-spoofing e integridade de arquitetura ArchUnit.
  9. Zero credenciais expostas no repositório.
- **Resultado:** **`PROMPT P002-BE = APPROVED`**. O prompt subsequente (`P002-FE`) está formalmente liberado e transita para `READY`.

### Transição: PROMPT P001-FE $\rightarrow$ `APPROVED`

- **Data/Hora:** 17/11/2026 21:32 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. Build limpo de produção Next.js 15 (`pnpm run build`) concluído com exit code 0 e geração estática de rotas.
  2. Typecheck estrito (`tsc --noEmit`) e linting validados sem nenhum erro ou warning.
  3. Suíte de testes unitários e de componentes com Vitest (16/16 testes aprovados com 100% de sucesso).
  4. Validação de layout responsivo em 360px, 768px, 1024px e 1440px sem overflow horizontal crítico.
  5. Touch targets no dock mobile em conformidade WCAG (mínimo de 48px).
  6. Alternância de tema Light Mode (padrão) e Dark Mode (opcional) com persistência em `localStorage` (`seller_theme`) e acessibilidade via teclado/ARIA.
  7. Empty states, skeletons de carregamento com `role="status"` e error boundary global (`error.tsx`) com fail-closed ativo.
  8. Ausência de telas funcionais ou chamadas simuladas de backend sem contrato definido; navegação estruturada sobre os 17 módulos de domínio do PRD com placeholders informativos e rastreamento de proveniência.
  9. Zero segredos ou credenciais expostas no código do frontend.
- **Resultado:** **`PROMPT P001-FE = APPROVED`**. O prompt subsequente (`P002-BE`) está formalmente liberado e transita para `READY`.

### Transição: PROMPT P001-BE $\rightarrow$ `APPROVED`

- **Data/Hora:** 17/11/2026 21:22 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. Build Maven reproduzível com wrapper (`mvnw`) em Java 21 LTS e Spring Boot 3.4.3 / 3.5.x.
  2. Definição estrutural dos 17 módulos de domínio com boundaries delimitados.
  3. Suíte de testes de arquitetura com ArchUnit aprovada (zero ciclos de dependência entre módulos).
  4. Flyway com migração inicial (`V1__init_schema.sql`) validada e aplicada com sucesso.
  5. Actuator Health (`/actuator/health`) respondendo HTTP 200 com status `UP` e liveness/readiness ativos.
  6. OpenAPI 3.0 (`/v3/api-docs`) gerado com sucesso.
  7. Correlation ID (`X-Correlation-Id`) propagado via MDC e retornado no cabeçalho.
  8. Tratamento de erros RFC 7807 (`ProblemDetail`) implementado.
  9. Docker Compose com PostgreSQL 17, Redis 7, RabbitMQ 3.13 e Keycloak 26 validado.
  10. Zero segredos ou credenciais expostas no repositório.
- **Resultado:** **`PROMPT P001-BE = APPROVED`**. O prompt subsequente (`P001-FE`) está formalmente liberado e transita para `READY`.

### Transição: PROMPT P009-BE $\rightarrow$ `APPROVED`

- **Data/Hora:** 18/11/2026 14:03 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. Modelo comum de observações de mercado (`MarketObservation`) implementado com proveniência completa: `source_type` (`OFFICIAL`, `OBSERVED`, `ESTIMATED`, `USER_INPUT`), `source_url`, `collected_at`, `observed_at`, `valid_until`, `method`, `confidence` e `minimized_payload`.
  2. Regra Anti-Alucinação estritamente implementada e testada: criação de valores de vendas, visitas, CTR ou faturamento sem metodologia e fonte documentadas é sumariamente rejeitada com `BusinessRuleException` (HTTP 422).
  3. Observações `ESTIMATED` exigem obrigatoriamente `methodology_description`, `model_version`, `time_window` e `confidence` definido.
  4. Defesa SSRF ativa: bloqueio de URLs apontando para endereços de metadados em nuvem ou faixas de IP privadas.
  5. Minimização de payload sensível: remoção de tokens, credenciais, segredos e dados pessoais (LGPD).
  6. Deduplicação inteligente de observações para mesmo anúncio/marketplace dentro da janela de validade, evitando registros vazios duplicados.
  7. Algoritmo de matching multi-fator explicável com pontuação ponderada entre 0.00 e 1.00 (similaridade de títulos com normalização e stopwords, GTIN/EAN idêntico ou penalidade em divergência, marca e plausibilidade).
  8. Regra de ouro do critério de aceite comprovada: itens abaixo do limiar (threshold, padrão 0.75) **NUNCA são unidos automaticamente** (permanecem categorizados como `POTENTIAL_MATCH` ou `NO_MATCH`).
  9. Isolamento multi-tenant PostgreSQL RLS implementado e validado via Testcontainers PostgreSQL 17 (`V7_1__postgresql_rls_market_intelligence.sql`).
  10. 158/158 testes do backend aprovados com zero falhas, zero skips e zero regressões.
- **Resultado:** **`PROMPT P009-BE = APPROVED`**. O prompt subsequente (`P009-FE`) está formalmente liberado e transita para `READY`.

### Transição: PROMPT P009-FE $\rightarrow$ `APPROVED`

- **Data/Hora:** 18/11/2026 14:13 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. Componente de visualização de inteligência de mercado (`MarketIntelligenceView`) e gaveta de detalhes (`ObservationDetailDrawer`) implementados e integrados com sucesso na aplicação web.
  2. Cumprimento estrito do critério de aceite: **dado estimado nunca aparece como oficial** (badges estritos e distintos para `OFFICIAL`, `OBSERVED`, `ESTIMATED` e `USER_INPUT`, com alertas de modelo e aviso anti-alucinação explícitos para estimativas).
  3. Fonte e data visíveis: canais de marketplace, identificadores externos, links externos seguros (`rel="noopener noreferrer" target="_blank"`), datas formatadas em padrão pt-BR de observação (`observedAt`) e coleta (`collectedAt`), validade e método de coleta.
  4. Gaveta de explicação e matching com catálogo canônico acessível com um clique, apresentando pontuação explicável, breakdown de GTIN, similaridade de título, marca e justificativa textual transparente.
  5. Rotas BFF Next.js 15 (`/api/market-intelligence/observations`, `[id]`, `[id]/match`) com repasse de tenant e proteção de sessão implementadas.
  6. Acessibilidade WCAG 2.2 AA atendida: `role="dialog"`, `aria-modal="true"`, fechamento via tecla `ESC`, touch targets $\ge 44\text{px}$, skeletons com `role="status"` e empty state com `role="region"`.
  7. 93/93 testes do frontend aprovados (100% de sucesso).
  8. Build de produção do Next.js aprovado gerando 21 rotas estáticas e dinâmicas com zero erros.
- **Resultado:** **`PROMPT P009-FE = APPROVED`**. O prompt subsequente (`P010-BE`) está formalmente liberado e transita para `READY`.

### Transição: PROMPT P010-BE $\rightarrow$ `APPROVED`

- **Data/Hora:** 18/11/2026 14:40 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. Registro de políticas oficiais de fontes (`market_search_source_policies` e `SearchSourcePolicyRegistry`) implementado com validação prévia de canais, endpoints, documentação e limites de taxa.
  2. Política estrita **Fail-Closed** implementada: canais com status `UNVERIFIED` (Amazon SP-API sem homologação) bloqueiam imediatamente requisições externas, retornando aviso formal e zero dados fabricados.
  3. Proteção **Anti-Scraping** mandatória: para canais `RESTRICTED` que não disponibilizam busca concorrencial aberta (Shopee Open Platform), o sistema recusa expressamente scraping hostil e executa fallback transparente para dados do catálogo próprio do tenant (`OWN_CATALOG`).
  4. Garantia **Anti-Alucinação**: métricas não fornecidas pelas APIs oficiais (vendas unitárias de concorrentes, faturamento, CTR) **nunca são forjadas**; retornam nulas com a sinalização `salesMetricNotice = "Métricas de vendas unitárias e faturamento de terceiros não são fornecidas pela API oficial do canal (UNSUPPORTED) e não foram fabricadas."`.
  5. Rastreabilidade total: cada item retornado (`NormalizedMarketSearchResult`) possui proveniência explícita (`OFFICIAL`, `OBSERVED`, `LICENSED`), identificador da política (`sourcePolicyId`), link oficial higienizado, classificação de vendedor e timestamp de coleta.
  6. Cache inteligente por tenant com validade (TTL de 15 minutos) e hash determinístico (`market_search_cache`), preservando quotas de rate limit upstream.
  7. Limitador de taxa em janela deslizante (`MarketSearchRateLimiter`) com retorno de HTTP 429 Too Many Requests e cabeçalho `Retry-After`.
  8. Trilha de auditoria tenant-isolated em `market_search_queries` com registro de consultas, tempo de execução e status de capacidade.
  9. Isolamento multi-tenant PostgreSQL Row-Level Security (`ENABLE` e `FORCE ROW LEVEL SECURITY`) nas tabelas `market_search_queries` e `market_search_cache`, com validação real via Testcontainers PostgreSQL 17 (`PostgresMarketSearchRlsIntegrationTest`).
  10. Verificação modular ArchUnit (`ModularArchitectureTest`) aprovada com zero ciclos.
  11. 179/179 testes do backend e 93/93 testes do frontend aprovados (272/272 no total) com zero regressões.
- **Resultado:** **`PROMPT P010-BE = APPROVED`**. O prompt subsequente (`P010-FE`) está formalmente liberado e transita para `READY`.

### Transição: PROMPT P010-FE $\rightarrow$ `APPROVED`

- **Data/Hora:** 18/11/2026 14:57 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. **Interface de Pesquisa Autorizada e Responsiva (`MarketSearchView.tsx`):** Busca por termo/categoria com filtros de marketplace, faixa de preço, tipo de vendedor/propriedade (`ALL`, `OWN`, `COMPETITOR`) e ordenação por relevância, menor preço, maior preço e data de coleta.
  2. **Diferenciação Estrita entre Anúncio Próprio e Concorrente:** Badges visuais destacados e acessíveis (`🟢 Próprio` e `🟠 Concorrente`) respeitando os limites da fonte oficial; ausência de adivinhações quando o identificador de vendedor não estiver disponível.
  3. **Proveniência e Fontes Sempre Visíveis:** Cada resultado exibe ostensivamente badge de proveniência (`OFFICIAL`, `OBSERVED`, `LICENSED`), código da política registrada (`sourcePolicyId`), canal de marketplace, seller name, condição e timestamp de coleta/sincronização no padrão brasileiro `pt-BR`.
  4. **Links Externos Seguros:** Links para os anúncios oficiais externos sanitizados com proteção contra tabnabbing (`target="_blank" rel="noopener noreferrer"`).
  5. **Modal de Comparação Lado a Lado (`ProductCompareModal.tsx`):** Comparação acessível (`role="dialog"`, `aria-modal="true"`, tecla `ESC`, touch targets $\ge 44\text{px}$) entre produtos/anúncios selecionados, contrastando atributos, preços, GTIN, condição e proveniência sem expor dados fabricados.
  6. **Resiliência e Políticas de Canal (Fail-Closed, Fallback e Rate-Limit):**
     - Amazon (UNVERIFIED): banner explicativo Fail-Closed instruindo que o canal requer homologação oficial e não realiza scraping.
     - Shopee (RESTRICTED): aviso amigável de anti-scraping informando fallback seguro para catálogo próprio (`OWN_CATALOG`).
     - HTTP 429 Too Many Requests: captura amigável com tempo de espera (`Retry-After`) e aviso transparente de proteção de quotas dos canais.
  7. **Responsividade e Acessibilidade (WCAG 2.2 AA):**
     - Alternância de visualização entre Tabela (com container horizontal rolável acessível `role="region"`, `tabIndex={0}`, `aria-label`) e Cards empilhados ideais para dispositivos móveis.
     - Suporte pleno a navegação por teclado, estados de foco visíveis e touch targets $\ge 44\text{px}$.
  8. **Rotas BFF Next.js 15 Implementadas:**
     - `GET /api/market-search`: repassa filtros, termos e cabeçalhos de autorização/tenant, tratando 429 e 422.
     - `GET /api/market-search/sources`: expõe registro auditado de políticas de fontes.
     - `GET /api/market-search/history`: consulta histórico de buscas tenant-isolated.
  9. **Build & Testes:**
     - 101/101 testes frontend aprovados (16 arquivos de teste, incluindo 8 testes dedicados de pesquisa, comparação, badges de proveniência e fallback em `MarketSearchView.test.tsx`).
     - Next.js 15.1.7 Production Build aprovado gerando 24 rotas estáticas e dinâmicas com zero erros.
     - Suíte combinada do projeto com 280/280 testes automatizados aprovados (179 backend + 101 frontend).
- **Resultado:** **`PROMPT P010-FE = APPROVED`**. O prompt subsequente (`P011-BE`) está formalmente liberado e transita para `READY`.

### Transição: PROMPT P011-BE $\rightarrow$ `APPROVED`

- **Data/Hora:** 18/11/2026 15:51 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. **Score e Critérios Explicáveis (`ListingEquivalenceService`):** Motor de cálculo determinístico decompõe pontuações de GTIN (35%), Marca (25%), Modelo (20%), Atributos Técnicos (10%) e Título (10%), com representação estrita em `BigDecimal` e justificativa legível por humanos em linguagem natural.
  2. **Sinalização Mandatória de Itens Incompatíveis:**
     - Detecção estrita de incompatibilidade: GTINs válidos conflitantes, marcas comerciais divergentes, condição novo vs usado/recondicionado, especificações críticas divergentes (voltagem 110V vs 220V, capacidade 64GB vs 256GB) e incompatibilidade estrutural de tipo (acessório vs aparelho principal).
     - Itens incompatíveis recebem `isCompatible = false`, status `INCOMPATIBLE`, lista de motivos em `incompatibilityReasons` e aviso ostensivo em `warningMessage`.
     - Regra de Ouro: itens incompatíveis **NUNCA são classificados como equivalentes** (`isEquivalent = false`).
  3. **Variação de Preço em Janela Temporal Comparável (`ListingPriceComparisonService`):**
     - O cálculo de diferença monetária (`priceDifference`) e percentual (`priceVariationPercentage`) é executado estritamente se o intervalo temporal entre as observações for comparável ($|T_2 - T_1| \le \text{maxComparableDays}$, padrão 30 dias).
     - Quando a janela é excedida, o cálculo é categoricamente omitido (`priceVariationComparable = false`, valores nulos) com aviso formal de incomparabilidade temporal, impedindo falsas inferências de mercado.
  4. **Proteção Rigorosa contra Inferência de Frete Desconhecido:**
     - Quando o custo de frete for desconhecido (`shippingCost == null`), o sistema reporta `shippingStatus = UNKNOWN` e **NUNCA** infere ou presume frete final (`finalPriceWithShipping = null`), mantendo estritamente o `listingPrice` separado e emitindo aviso legal formal.
     - O cálculo de frete total só é efetuado quando ambos os anúncios possuírem custos oficiais conhecidos.
  5. **Histórico Legítimo sem Retroatividade Inventada (`ListingPriceHistoryService`):**
     - Registro em tabela dedicada `listing_price_history` de pontos reais coletados com timestamps auditáveis (`observedAt`, `collectedAt`).
     - Anúncios com observação única retornam exatamente 1 ponto, com sinalização expressa de que a plataforma não fabrica histórico retroativo passado ("Zero retroatividade inventada").
     - Tendências e variações históricas só são computadas na presença de $\ge 2$ pontos legítimos observados.
  6. **Isolamento Multi-Tenant em Profundidade com PostgreSQL 17 RLS:**
     - Tabelas `listing_price_history` e `listing_comparisons` com RLS ativada e forçada (`FORCE ROW LEVEL SECURITY`) via migrações Flyway `V9__listing_equivalence_and_comparison.sql` e `V9_1__postgresql_rls_listing_comparison.sql`.
     - Teste de integração via Testcontainers com usuário `seller_app` (NOBYPASSRLS) comprovou que tentativas de leitura cross-tenant, mutações e tentativas de spoofing (WITH CHECK) são bloqueadas pelo banco de dados (SQLState 42501).
  7. **Arquitetura Modular Livre de Ciclos:**
     - Testes modulares ArchUnit (`ModularArchitectureTest`) aprovados com 100% de sucesso e zero ciclos.
  8. **Testes & Cobertura:**
     - 203/203 testes de backend aprovados (24 novos testes dedicados a equivalência, compatibilidade, cálculo temporal, frete desconhecido, histórico não-inventado, controller e RLS real).
     - 101/101 testes de frontend mantidos com zero regressões.
     - Total do projeto: 304 testes automatizados aprovados (0 falhas, 0 skips).
- **Resultado:** **`PROMPT P011-BE = APPROVED`**. O prompt subsequente (`P011-FE`) está formalmente liberado e transita para `READY`.

### Transição: PROMPT P011-FE $\rightarrow$ `APPROVED`

- **Data/Hora:** 18/11/2026 16:40 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. **Comparação Lado a Lado Responsiva (`ListingComparisonView.tsx`):**
     - Layout comparativo lado a lado estruturado em matriz e cards de análise, contrastando o produto próprio de referência contra anúncios concorrentes e de mercado.
     - Responsividade completa para desktop (tabela comparativa com cabeçalho sticky e colunas fixas) e mobile (cards de análise visual otimizados para toque).
  2. **Diferenças Claras de Atributos:**
     - Matriz de atributos linha a linha comparando: Preço, Frete, Preço Total, Condição, Marca, Modelo, GTIN, Voltagem, Armazenamento/RAM e Data da Observação.
     - Destaque visual inconfundível para atributos idênticos (`✓ Idêntico` com badges sutis) e divergentes (`⚠️ Divergente` com background destacado e ícones de alerta).
  3. **Baixa Confiança Destacada:**
     - Scores abaixo do limiar oficial ($< 75\%$) ou baixos ($< 50\%$) recebem badges ostensivos em tons de alerta (âmbar e vermelho).
     - Itens limítrofes / borderline (50-74%) são identificados com "⚠️ NÃO EQUIVALENTE (REVISÃO)" para inspeção manual pelo lojista.
  4. **Sem Falsa Equivalência Visual:**
     - Anúncios com incompatibilidade estrutural (`isCompatible === false` ou `isEquivalent === false`) são **estritamente proibidos de exibir distintivos verdes de "Equivalente"**.
     - Renderização obrigatória de tarja ostensiva "⛔ ALERTA ANTI-FALSA EQUIVALÊNCIA" destacando os motivos impeditivos (ex.: NOVO vs USADO, marcas distintas Apple vs Samsung, voltagens conflitantes) e alertando que precificar com base neles causará distorções econômicas graves.
  5. **Integridade Temporal e Frete Desconhecido:**
     - Observações com intervalo temporal superior a 30 dias têm percentuais de variação de preço ocultados com aviso de defasagem de janela.
     - Anúncios com frete não informado pelo canal (`UNKNOWN`) exibem explicitamente "⚠️ Não informado pelo canal" e nunca inventam preço total fictício com frete.
  6. **Histórico Legítimo de Preços (`ListingPriceHistoryDrawer.tsx`):**
     - Painel auditável exibindo exclusivamente pontos reais persistidos coletados pelo backend, sem retroatividade ou interpolações artificiais do passado.
     - Disclaimer ostensivo de integridade de dados e KPIs auditáveis (preço atual, inicial, mínimo, máximo e variação no período).
  7. **Modal de Explicabilidade do Score:**
     - Modal detalhando a decomposição ponderada do algoritmo: GTIN (35%), Marca (20%), Modelo (20%), Título (15%) e Especificações Técnicas (10%).
  8. **Rotas BFF Seguras (Next.js 15):**
     - Proxies criados em `/api/market-comparison/compare`, `/api/market-comparison/find-equivalents`, `/api/market-comparison/price-history` e `/api/market-comparison/saved` com propagação de JWT e `tenant_id`.
  9. **Acessibilidade e Usabilidade (WCAG 2.2 AA):**
     - Modais e drawers com `role="dialog"`, `aria-modal="true"`, foco acessível e fechamento por tecla ESC.
     - Touch targets $\ge 44\text{px}$ em todos os controles interativos.
  10. **Testes & Builds:**
      - 119/119 testes de frontend aprovados (18 novos testes dedicados a comparação, diferenças claras, anti-falsa equivalência, filtros, histórico e drawer).
      - 203/203 testes de backend aprovados (zero regressões).
      - Total do projeto: 322 testes automatizados aprovados (100% de sucesso).
      - `pnpm exec tsc --noEmit` limpo e `next build` 15.1.7 com 28 rotas compiladas e otimizadas com sucesso.
- **Resultado:** **`PROMPT P011-FE = APPROVED`**. O prompt subsequente (`P012-BE`) está formalmente liberado e transita para `READY`.

### Transição: PROMPT P012-BE $\rightarrow$ `APPROVED`

- **Data/Hora:** 18/11/2026 17:10 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. **CRUD Isolado Multi-Tenant em Profundidade (`MonitoringGroupService` & `MonitoredItemService`):**
     - Grupos de monitoramento e itens monitorados completamente isolados por tenant via PostgreSQL 17 RLS (`FORCE ROW LEVEL SECURITY`).
     - Consultas, atualizações e exclusões filtram estritamente pelo tenant contextual.
  2. **Controle Rigoroso de Duplicidade (Anti-Duplication):**
     - Nomes de grupos únicos por tenant (`uq_monitoring_groups_tenant_name`); tentativas duplicadas retornam HTTP 409 (`DuplicateResourceException`).
     - Itens monitorados únicos por grupo e anúncio externo (`uq_monitored_item_group_listing`); duplicatas no mesmo grupo rejeitadas com HTTP 409.
  3. **Histórico Estritamente Não-Retroativo (`MonitoredItemSnapshotService`):**
     - O histórico de monitoramento começa no primeiro snapshot efetivo pós-inclusão (`first_snapshot_at >= created_at`).
     - Rejeição categórica de snapshots forjados com timestamp anterior à data de inclusão do item (`BusinessRuleException` "Violação de integridade histórica: snapshot anterior à inclusão do item"), eliminando qualquer fabricação retroativa de dados passados.
  4. **Agendamento Respeita Políticas, Janela Mínima, Quotas e Kill Switch (`MonitoringSchedulerPolicyValidator` & `MonitoringJobExecutionService`):**
     - Janela mínima: respeita `min_interval_minutes` configurado no grupo (`SKIPPED_WINDOW_MINIMA`).
     - Quota/Rate limit: respeita limite por minuto por tenant (`SKIPPED_RATE_LIMIT`).
     - Kill switch: bloqueio imediato se kill switch ativado para o marketplace/fonte (`SKIPPED_KILL_SWITCH`).
     - Fontes não homologadas / proibidas (ex.: `UNLICENSED_SCRAPER`): bloqueadas categoricamente com `BusinessRuleException` / HTTP 422 ("Fonte não homologada para monitoramento"), cumprindo a diretriz de não buscar dados em fontes proibidas.
  5. **Exclusão Segue Retenção Definida (`MonitoringRetentionService`):**
     - Exclusão lógica (soft delete) de grupos e itens (`DELETED`, `deleted_at = NOW()`), desativando coletas ativas sem apagar snapshots históricos auditáveis.
     - Política de retenção configurável no grupo (`retention_days`, mínimo 7 dias, padrão 90 dias) para purga automática de snapshots expirados via job de retenção auditado.
  6. **Isolamento Multi-Tenant Testado em Banco Real (PostgreSQL 17 RLS):**
     - 5/5 cenários de teste de integração com Testcontainers (`PostgresProductMonitoringRlsIntegrationTest`) aprovados usando usuário `seller_app` (NOBYPASSRLS): isolamento de leitura de grupos e itens/snapshots, prevenção de spoofing no INSERT via `WITH CHECK` (SQLState 42501), proteção contra mutações indevidas e retorno vazio quando tenant_id não está configurado.
  7. **Testes e Arquitetura:**
     - 233/233 testes de backend aprovados (30 testes dedicados de monitoramento: 25 unitários/controller e 5 integração RLS).
     - 119/119 testes de frontend preservados sem regressões.
     - 3/3 testes modulares de arquitetura ArchUnit aprovados sem ciclos ou violações.
- **Resultado:** **`PROMPT P012-BE = APPROVED`**. O prompt subsequente (`P012-FE`) está formalmente liberado e transita para `READY`.

### Transição: PROMPT P012-FE $\rightarrow$ `APPROVED`

- **Data/Hora:** 18/11/2026 17:28 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. **Tela de Grupos de Monitoramento Completa (`ProductMonitoringView.tsx`):**
     - Listagem de grupos temáticos isolados pelo tenant ativo com contadores, cores distintivas, intervalo configurável (`minIntervalMinutes`), período de retenção (`retentionDays`) e status (`ACTIVE`, `PAUSED`).
     - Modais para criação e edição de grupos (`CreateGroupModal.tsx`) com seleção de paleta visual e tratamento de unicidade (HTTP 409).
  2. **Visão Detalhada de Itens Monitorados:**
     - Exibição de metadados do grupo, contagem de itens, estatísticas de execução e listagem completa dos anúncios monitorados.
     - Suporte a múltiplos canais (`MERCADO_LIVRE`, `SHOPEE`), link oficial da fonte protegido por `rel="noopener noreferrer"`, preço atualizado, vendedor e status.
     - Transparência de coletas: indicação expressa de "Aguardando 1ª coleta" quando nenhum snapshot foi capturado pós-inclusão.
  3. **Adição de Item via Pesquisa Autorizada e Inserção Direta (`AddItemToGroupModal.tsx`):**
     - Aba de Pesquisa Integrada: consulta endpoints de pesquisa autorizada de mercado (`/api/market-search`), permitindo localizar anúncios concorrentes ou próprios e preencher automaticamente os dados do anúncio no grupo.
     - Aba de Inserção Direta: formulário completo para cadastro manual com seleção de canal, ID externo, título, URL e anotações.
     - Prevenção de duplicidade: bloqueio imediato e feedback claro quando o anúncio já consta no grupo (HTTP 409).
  4. **Remoção com Confirmação e Aviso de Retenção Legal (`ConfirmDeleteModal.tsx`):**
     - Modais acessíveis de confirmação (`role="dialog"`, `aria-modal="true"`, foco e cancelamento via tecla ESC).
     - Informação ostensiva ao lojista sobre os efeitos da exclusão: cancelamento imediato de novas coletas programadas e retenção obrigatória dos snapshots históricos coletados para fins de auditoria regulatória.
  5. **Timeline Cronológica com Destaque da Última Coleta e Detecção de Gaps (`MonitoringTimelineDrawer.tsx`):**
     - Painel lateral exibindo o cartão de destaque da Última Coleta Efetiva (preço mais recente, data/hora, tempo relativo decorrido e status).
     - Botão "Coletar Agora" permitindo acionamento sob demanda via API BFF com feedback imediato de regras de agendamento (`SKIPPED_WINDOW_MINIMA`, `SKIPPED_RATE_LIMIT`, `SKIPPED_KILL_SWITCH`).
     - Linha do tempo cronológica com cálculo determinístico de variação de preço ($\Delta R\$$ e $\Delta\%$).
     - **Detecção Explícita de Gaps Temporais:** janelas onde coletas foram puladas ou atrasadas ($> 2 \times$ `minIntervalMinutes`) são identificadas com blocos de aviso de gap, deixando explícito que a plataforma **não inventa valores intermediários**.
  6. **Estados Vazios Acessíveis (Empty State):**
     - Estados vazios amigáveis com orientação clara para grupos não cadastrados, grupos sem itens e anúncios sem snapshots históricos.
  7. **Filtros e Responsividade (WCAG 2.2 AA):**
     - Filtros combináveis por canal (`MERCADO_LIVRE`, `SHOPEE`), status (`ACTIVE`, `PAUSED`) e busca por texto.
     - Alternância entre visualização em Tabela (desktop) e Cards (mobile).
     - Touch targets $\ge 44\text{px}$ em todos os controles interativos.
  8. **Testes & Builds:**
     - 136/136 testes de frontend aprovados (17 novos testes dedicados ao monitoramento em `ProductMonitoringView.test.tsx`, `MonitoringTimelineDrawer.test.tsx` e `AppShell.test.tsx`).
     - 233/233 testes de backend mantidos com 100% de sucesso.
     - Total do projeto: 369 testes automatizados aprovados (0 falhas, 0 erros, 0 skips).
     - `pnpm exec tsc --noEmit` limpo e `next build` 15.1.7 gerando 34 rotas com sucesso.
- **Resultado:** **`PROMPT P012-FE = APPROVED`**. O prompt subsequente (`P013-BE`) está formalmente liberado e transita para `READY`.

---

### Transição: PROMPT P013-BE $\rightarrow$ `APPROVED`

- **Data/Hora:** 18/11/2026 18:05 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. **Snapshots Idempotentes com Chave Determinística (`MonitoredItemSnapshotService.java`):**
     - Deduplicação física no banco via constraint `uq_snapshots_tenant_idempotency UNIQUE (tenant_id, idempotency_key)`.
     - Geração automática de hash SHA-256 determinístico (`tenant_id:item_id:epoch_minute:price:quantity`) caso o cliente não forneça chave customizada.
     - Re-execuções com a mesma chave retornam o registro existente imediatamente sem gerar novas linhas nem erros.
  2. **Proveniência, Validade e Timestamps Precisos:**
     - Registro explícito de `collected_at` (audit timestamp UTC), `observed_at` (timestamp no marketplace), `valid_until` (TTL de validade da observação), `source_url`, `source_type` (`OFFICIAL_API`, `LICENSED_CATALOG`, `PUBLIC_ENDPOINT`) e `collection_status` (`SUCCESS`, `PARTIAL`, `FAILED_SOURCE_UNAVAILABLE`).
  3. **Detecção Automática de Alterações (Change Detection):**
     - Comparação em tempo de persistência com o snapshot imediatamente anterior (`previous_snapshot_id`).
     - Cálculo exato em `BigDecimal` com 4 casas decimais e arredondamento `HALF_UP` para `price_change_amount` e `price_change_percent`.
     - Detecção de alteração de estoque (`has_availability_change`) e atributos/status (`has_attribute_change`).
     - Classificação automática de `change_type`: `INITIAL`, `NO_CHANGE`, `PRICE_INCREASE`, `PRICE_DECREASE`, `AVAILABILITY_CHANGE`, `STATUS_CHANGE`.
  4. **Resiliência Fail-Closed quando Fonte Indisponível:**
     - Em caso de indisponibilidade da fonte (5xx, timeout, circuit breaker, kill switch), **nenhum snapshot é criado** na tabela `monitored_item_snapshots`.
     - A falha é registrada em `monitoring_job_executions` com `execution_status = 'FAILED_SOURCE_UNAVAILABLE'`, motivo detalhado e duração da tentativa.
     - Exceção `SourceUnavailableException` mapeada no `GlobalExceptionHandler` para HTTP 503 (Service Unavailable) em chamadas diretas.
  5. **Governança de Retenção e Particionamento:**
     - Decisão formal documentada: volume do MVP (< 1M linhas) não justifica particionamento físico no PostgreSQL 17 devido a restrições de FK e RLS.
     - Retenção aplicada via `MonitoringRetentionService` com expurgo auditado após `retention_days`.
  6. **Isolamento PostgreSQL 17 RLS e Integridade:**
     - Migration Flyway `V11__idempotent_snapshots_and_change_detection.sql` executada com sucesso em PostgreSQL 17 e H2.
     - Testes com Testcontainers (`PostgresProductMonitoringRlsIntegrationTest`) comprovaram unicidade de idempotência por tenant e bloqueio de violações cross-tenant.
  7. **Testes Automatizados:**
     - 243/243 testes de backend aprovados (10 unitários em `MonitoredItemSnapshotServiceTest`, 5 de controller em `ProductMonitoringControllerTest`, 6 de integração RLS em `PostgresProductMonitoringRlsIntegrationTest`).
     - 136/136 testes de frontend aprovados (regressão mantida).
     - Total geral do projeto: 379 testes automatizados aprovados (0 falhas, 0 erros, 0 skips).
- **Resultado:** **`PROMPT P013-BE = APPROVED`**. O prompt subsequente (`P013-FE`) está formalmente liberado e transita para `READY`.

---

### Transição: PROMPT P013-FE $\rightarrow$ `APPROVED`

- **Data/Hora:** 19/11/2026 08:12 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. **Gráfico Histórico com Intervalos Temporais (`PriceHistoryChart.tsx`):**
     - Plotagem precisa em SVG responsivo (`viewBox`) da curva cronológica de preços observados.
     - Filtros de período temporal operacionais: `24h`, `7d`, `30d`, `90d`, `all` ("Tudo").
     - Cards de estatísticas e KPIs do período selecionado: Preço Atual, Mínimo no Período, Máximo no Período e Variação ($\Delta R\$$ e $\Delta\%$).
  2. **Regra Anti-Alucinação Estrita (Visualização de Lacunas de Coleta):**
     - O gráfico **não interpola** visualmente observações ausentes como dados contínuos reais.
     - Quando o intervalo entre duas observações consecutivas ultrapassa o limiar de tolerância ($> 2 \times$ `minIntervalMinutes`), a linha é quebrada em segmentos isolados (`path`).
     - A lacuna é demarcada por área sombreada com padrão hachurado (`gap-pattern`), linhas tracejadas e marcador central (`⚠️ Lacuna (Xh)`).
     - Clique na lacuna exibe banner com aviso explícito de que dados intermediários não são inventados nem interpolados.
  3. **Tooltip e Painel de Auditoria de Fonte:**
     - Interação via clique, foco ou toque revela metadados completos de proveniência: `sourceType` (`OFFICIAL_API`, etc.), `collectionStatus` (`SUCCESS`), `availableQuantity`, `validUntil` e link oficial seguro com `rel="noopener noreferrer"`.
     - Badges de alteração cromática por `changeType` (`PRICE_DECREASE` em verde, `PRICE_INCREASE` em vermelho, `AVAILABILITY_CHANGE` em âmbar, `INITIAL` em azul).
  4. **Acessibilidade e WCAG 2.2 AA:**
     - Navegação total por teclado nos pontos com setas (`ArrowRight`, `ArrowLeft`), `Enter` e `Space`.
     - Atributos semânticos `role="region"`, `role="img"`, `role="button"` e `aria-label` descritivos em todos os pontos.
     - Alternância para **"Visão em Tabela"** (`<table>`), apresentando observações cronológicas em formato tabular estruturado com linhas de alerta para lacunas.
  5. **Mobile Funcional:**
     - Touch targets com altura mínima garantida $\ge 44\text{px}$ (`min-h-[44px]`).
     - Layout adaptativo touch-friendly sem overflow horizontal (`touch-pan-x`).
  6. **Integração na Linha do Tempo (`MonitoringTimelineDrawer.tsx`):**
     - Componente integrado ao Drawer de Monitoramento, renderizado automaticamente quando snapshots estão presentes.
  7. **Testes Automatizados & Builds:**
     - 144/144 testes de frontend aprovados (8 novos testes em `PriceHistoryChart.test.tsx` cobrindo filtros, gaps anti-alucinação, tooltips, tabela e teclado).
     - 243/243 testes de backend mantidos com 100% de sucesso.
     - Total geral do projeto: 387 testes automatizados aprovados (0 falhas, 0 erros, 0 skips).
     - TypeScript limpo (`tsc --noEmit`) e build Next.js 15.1.7 gerando todas as 34 rotas com sucesso.
- **Resultado:** **`PROMPT P013-FE = APPROVED`**. O prompt subsequente (`P014-BE`) está formalmente liberado e transita para `READY`.

---

### Transição: PROMPT P014-BE $\rightarrow$ `APPROVED`

- **Data/Hora:** 19/11/2026 08:54 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. **Cadastro de Custos Versionado e Imutável (`ProductCostRecord.java`, `ProductCostService.java`):**
     - Estrutura de custos diretos (`acquisitionCost`, `packagingCost`, `additionalCost`, `subsidizedShippingCost`) e percentuais (`taxRatePercent`, `otherExpensesPercent`) escopados por `(tenantId, productId, variantId, companyId)`.
     - Versionamento estrito: novas atualizações encerram a vigência do registro anterior (`effectiveTo = newEffectiveFrom`) e incrementam a versão, preservando 100% do histórico pretérito sem sobrescrever dados.
  2. **Cascata de Resolução Determinística por Especificidade:**
     - Resolução hierárquica por 4 níveis de especificidade decrescente: Variante + Empresa $\rightarrow$ Variante $\rightarrow$ Produto + Empresa $\rightarrow$ Produto Base.
     - Ausência de custo retorna `Optional.empty()` sem inventar valores ou assumir custo zero.
  3. **Regras de Taxas Parametrizáveis com Vigência e Faixas (`FeeRuleRecord.java`, `FeeRuleService.java`):**
     - Suporte a tipos de cobrança: `COMMISSION`, `FIXED_FEE`, `LOGISTICS_FEE`, `PAYMENT_PROCESSING`, `OTHER`.
     - Modalidades: `PERCENTAGE`, `FIXED_AMOUNT`, `PERCENTAGE_PLUS_FIXED`.
     - Filtros por canal/marketplace (`MERCADO_LIVRE`, `SHOPEE`), modalidade de anúncio (`listingType`), categoria, faixas de preço (`minPriceThreshold`, `maxPriceThreshold`), vigência (`effectiveFrom`, `effectiveTo`) e versão.
  4. **Regra Anti-Alucinação Estrita (Ausência Explícita de Taxas - `FeeResolutionService.java`):**
     - "Não inventar tabela de taxas; iniciar sem regra quando não houver fonte validada e solicitar input do usuário."
     - Se o canal/categoria não possuir regra contratual cadastrada, a plataforma **não assume 0,00% de comissão** nem calcula valores falsos.
     - Retorno explícito de `hasMissingFees = true`, enumeração em `missingFeeTypes` (ex: `COMMISSION`) e mensagem ostensiva orientando o usuário a cadastrar a regra com fonte oficial.
     - Campo `sourceReference` obrigatório e não vazio em todo cadastro de regra (`CONTRACT`, `OFFICIAL_PORTAL`, `MANUAL_OVERRIDE`).
  5. **Precisão Decimal e Arredondamento Documentado:**
     - Uso universal de `BigDecimal` com escala interna 4 e arredondamento padrão `RoundingMode.HALF_UP` documentado na especificação técnica.
  6. **Isolamento PostgreSQL 17 RLS e Auditoria:**
     - Migrations Flyway `V12__product_costs_and_fee_rules.sql` e `V12_1__postgresql_rls_pricing_and_fees.sql` aplicam RLS forçado em `product_costs` e `marketplace_fee_rules`.
     - Testes com Testcontainers (`PostgresPricingRlsIntegrationTest`) comprovam isolamento estrito contra injeção ou leitura cruzada entre tenants.
     - Eventos de auditoria emitidos via `AuditPublisher` para `PRODUCT_COST_CREATED`, `PRODUCT_COST_UPDATED`, `FEE_RULE_CREATED`, `FEE_RULE_UPDATED`.
  7. **Testes Automatizados & Builds:**
     - 267/267 testes de backend aprovados (+24 novos testes unitários, de controller e integração RLS).
     - 144/144 testes de frontend mantidos com 100% de sucesso.
     - Total geral do projeto: 411 testes automatizados aprovados (0 falhas, 0 erros, 0 skips).
     - TypeScript limpo (`tsc --noEmit`).
- **Resultado:** **`PROMPT P014-BE = APPROVED`**. O prompt subsequente (`P014-FE`) está formalmente liberado e transita para `READY`.

---

### Transição: PROMPT P014-FE $\rightarrow$ `APPROVED`

- **Data/Hora:** 19/11/2026 09:27 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. **Validação Estrita de Campos de Custos e Taxas (`ProductCostForm.tsx`, `FeeRuleForm.tsx`):**
     - Custos: Bloqueio de submissão sem `productId` válido (formato UUID) e `acquisitionCost` $> 0$; validação de alíquotas percentuais (0-100%) e custos indiretos ($\ge 0$).
     - Taxas: Validação de obrigatoriedade de `marketplace`, `feeType`, valores percentuais/fixos conforme modalidade e coerência de faixas de preço (`minPriceThreshold <= maxPriceThreshold`).
     - Todas as mensagens de erro associadas com `role="alert"` e `aria-describedby` para leitores de tela.
  2. **Histórico Visível de Versões de Custos:**
     - Carregamento automático via `/api/pricing/costs/history?productId=...`.
     - Exibição tabular estruturada com identificador de versão (`v1`, `v2`...), badge de status ("Vigente" vs "Encerrado"), intervalo de vigência (`effectiveFrom` a `effectiveTo`), decomposição de custos (aquisição, embalagem, frete subsidiado, adicionais) e autoria.
  3. **Regra Anti-Alucinação Estrita (Nenhuma Regra Não Verificada como Oficial):**
     - Ao selecionar `sourceType = 'MANUAL_OVERRIDE'` no formulário, a UI renderiza imediatamente alerta ostensivo informando que a regra não possui validação contratual.
     - Na tabela de taxas, regras manuais são marcadas com badge de destaque `"⚠️ Manual (Não Oficial)"`, enquanto regras oficiais exibem `"🛡️ Portal Oficial"` ou `"🛡️ Contrato"` com a URL/referência contratual visível.
  4. **Alertas no Cálculo / Simulador (`FeeSimulator.tsx`):**
     - **Alerta de Valor Manual:** Quando o motor de cálculo utiliza regra com `sourceType = 'MANUAL_OVERRIDE'`, o simulador exibe aviso ostensivo informando que o resultado é uma estimativa preliminar baseada em entrada manual.
     - **Alerta de Taxa Ausente:** Quando `hasMissingFees = true`, a UI exibe banner destacado listando `missingFeeTypes` (ex: `COMMISSION`) e informando expressamente que o sistema **NUNCA assume 0%** nem inventa valores, solicitando o cadastro da regra oficial.
  5. **Acessibilidade e Usabilidade (WCAG 2.2 AA):**
     - Estrutura semântica com `<label>`, `role="tablist"`, `role="tab"` e `role="tabpanel"`.
     - Touch targets com altura mínima garantida $\ge 44\text{px}$ em todos os botões e inputs.
     - Modo tabela com rolagem horizontal acessível e compatível com mobile.
  6. **Integração no Shell da Aplicação (`AppShell.tsx`):**
     - Renderização de `PricingAndCostsView` no módulo `FINANCIAL` ("Motor Financeiro Determinístico").
  7. **Testes Automatizados & Builds:**
     - 163/163 testes de frontend aprovados (19 novos testes cobrindo formulários, histórico, anti-alucinação, simulador e tabs).
     - 267/267 testes de backend mantidos com 100% de sucesso.
     - Total geral do projeto: 430 testes automatizados aprovados (0 falhas, 0 erros, 0 skips).
     - TypeScript limpo (`tsc --noEmit`) e build de produção Next.js 15.1.7 gerando todas as 38 rotas com sucesso.
- **Resultado:** **`PROMPT P014-FE = APPROVED`**. O prompt subsequente (`P015-BE`) está formalmente liberado e transita para `READY`.

---

### Transição: PROMPT P015-BE $\rightarrow$ `APPROVED`

- **Data/Hora:** 19/11/2026 09:51 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. **Motor Determinístico de Preço e Margem sem LLM (`PricingEngineService.java`):**
     - Cálculos 100% aritméticos em código Java sem chamadas a LLM ou inferências probabilísticas.
     - Utilização universal de `BigDecimal` com escala de cálculo 4 (`scale = 4`) e de apresentação 2 (`scale = 2`), arredondamento estrito `RoundingMode.HALF_UP`.
  2. **Grandezas Financeiras Formais Exatas:**
     - Receita Bruta ($RB$) e Receita Líquida ($RL = RB - \text{descontos}$).
     - Custos Diretos ($CD = \text{aquisição} + \text{embalagem} + \text{adicionais}$).
     - Deduções Fixas ($DF = \text{tarifa fixa} + \text{frete subsidiado} + \text{logística} + \text{outras fixas}$).
     - Deduções Variáveis ($DV_{\%} = \text{comissão} + \text{impostos} + \text{outras despesas}$ e $DV_{\text{valor}} = RL \times DV_{\%} / 100$).
     - Margem de Contribuição em Valor: $MC_{\text{valor}} = RL - CD - DF - DV_{\text{valor}}$.
     - Margem de Contribuição Percentual: $MC_{\%} = MC_{\text{valor}} / RL \times 100$.
     - Preço de Equilíbrio ($P_{\text{eq}} = (CD + DF) / (1 - DV_{\%} / 100) + \text{desconto}$).
     - Preço Mínimo e Preço-Alvo com base em margem percentual desejada ou margem monetária fixa.
  3. **Tratamento Fail-Safe de Divisão por Zero e Impossibilidades Matemáticas:**
     - Quando $RL \le 0$, a margem percentual retorna determinística em $0.00\%$ sem disparar `ArithmeticException`.
     - Quando $DV_{\%} \ge 100\%$ ($1 - DV_{\%} \le 0$), o Preço de Equilíbrio é formalmente inatingível: o motor retorna `isBreakEvenAttainable = false`, `breakEvenPrice = null` e aviso explícito, sem exceção aritmética.
  4. **Regras Anti-Alucinação Estritas:**
     - **Margem de Contribuição NUNCA rotulada como Lucro Líquido:** O DTO `PricingCalculationResult` documenta e exporta expressamente as premissas em `antiHallucinationDisclaimer` ("Margem de Contribuição não representa Lucro Líquido, pois não contempla custos e despesas fixas estruturais do negócio"), `formulaVersion = "v1.0.0"` e detalhamento item a item em `deductions`.
     - **Ausência de Taxas Tratada Ostensivamente:** Quando `FeeResolutionService` identifica taxas obrigatórias não cadastradas (`hasMissingFees = true`), o motor não assume 0%, bloqueia ou avisa expressamente (`hasMissingFees = true`, `userActionRequired`).
     - **Identificação de Regras Manuais:** O motor sinaliza `isManualFeeApplied = true` quando qualquer dedução originar-se de `USER_CONFIGURED` / `MANUAL_OVERRIDE`.
  5. **Endpoints REST no Controlador (`PricingAndCostsController.java`):**
     - `POST /api/v1/pricing/margins/calculate`: Cálculo direto determinístico a partir de parâmetros numéricos explícitos.
     - `POST /api/v1/pricing/margins/simulate-product`: Resolução integrada de custos vigentes de produto e taxas de marketplace para cálculo contextualizado de margens.
  6. **Testes Automatizados & Builds:**
     - 14/14 testes de pricing aprovados: 8 testes unitários em `PricingEngineServiceTest` cobrindo cenários padrão, margem negativa, divisão por zero em receita nula, impossibilidade matemática de break-even, preço mínimo/alvo percentual e fixo, ausência de taxas e taxas manuais; 6 testes de integração e controller em `PricingAndCostsControllerTest`.
     - 277/277 testes de backend mantidos com 100% de sucesso.
     - 163/163 testes de frontend mantidos com 100% de sucesso.
     - Total geral do projeto: 440 testes automatizados aprovados (0 falhas, 0 erros, 0 skips).
- **Resultado:** **`PROMPT P015-BE = APPROVED`**. O prompt subsequente (`P015-FE`) está formalmente liberado e transita para `READY`.

---

### Transição: PROMPT P015-FE $\rightarrow$ `APPROVED`

- **Data/Hora:** 19/11/2026 10:07 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. **Valores Batem Fielmente com a API (`PricingCalculator.tsx`):**
     - Integração direta e determinística via rotas BFF `/api/pricing/margins/calculate` e `/api/pricing/margins/simulate-product`.
     - Exibição de métricas calculadas (Receita Bruta, Desconto, Receita Líquida, Deduções Totais, Margem de Contribuição em R$ e %, Preço de Equilíbrio, Preço-Alvo e Preço Mínimo).
  2. **Acessibilidade por Teclado e Padrões WCAG 2.2 AA:**
     - Todos os campos com rótulos semânticos `<label>` e identificadores únicos `useId()`.
     - Mensagens de validação e erro integradas via `role="alert"` e `aria-describedby`.
     - Todos os botões e seletores com altura mínima $\ge 44\text{px}$ garantindo touch targets adequados.
     - Navegação fluida por Tab e tecla Enter para submissão e alternância entre modos.
  3. **Validação Clara de Formulário:**
     - Bloqueio e feedback imediato quando preço de venda $\le 0$, desconto $\ge$ preço de venda, alíquotas fora de $[0\%, 100\%]$ ou UUID em formato inválido no modo produto.
  4. **Layout Mobile Utilizável e Responsivo:**
     - Layout fluido em grid adaptativo (1 coluna em 360px móvel, 2 colunas em tablet, 4 colunas em desktop).
     - Tabelas de cenários e de deduções encapsuladas em containers com rolagem horizontal acessível (`overflow-x-auto`).
  5. **Simulação Comparativa de Cenários:**
     - Comparação lado a lado entre Cenário Atual, Cenário Ponto de Equilíbrio (Break-Even) e Meta Desejada (Target Margin).
  6. **Regras Anti-Alucinação Estritas:**
     - **Margem de Contribuição NÃO é Lucro Líquido:** Banner permanente destacado esclarecendo a ausência de despesas fixas estruturais na conta unitária.
     - **Avisos de Fontes e Dados Ausentes/Manuais:** Exibição ostensiva de avisos do motor quando regras aplicadas forem manuais ou quando houver taxas faltantes no canal.
     - **Tratamento de Inviabilidade Matemática:** Mensagem explicativa e bloqueio visual elegante quando taxas variáveis $\ge 100\%$ inviabilizam o break-even.
  7. **Testes Automatizados & Builds:**
     - 25/25 testes do módulo de pricing aprovados (7 novos testes em `PricingCalculator.test.tsx`).
     - 170/170 testes de frontend mantidos com 100% de sucesso em 26 arquivos.
     - 277/277 testes de backend mantidos com 100% de sucesso.
     - Total geral do projeto: 447 testes automatizados aprovados (0 falhas, 0 erros, 0 skips).
     - TypeScript limpo (`tsc --noEmit`) e build de produção Next.js 15.1.7 gerando 40 rotas com sucesso.
- **Resultado:** **`PROMPT P015-FE = APPROVED`**. O prompt subsequente (`P016-BE`) está formalmente liberado e transita para `READY`.

---

### Transição: PROMPT P016-BE $\rightarrow$ `APPROVED`

- **Data/Hora:** 19/11/2026 10:41 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. **Entidades Company e BusinessUnit (`CompanyRecord`):**
     - Mapeamento em JPA e DTOs tenant-scoped para pessoa jurídica brasileira (Razão Social, Nome Fantasia, CNPJ limpo de 14 dígitos, Inscrição Estadual e Regime Tributário opcional).
     - Rastreamento de metadados: vigência, criação, atualização, desativação lógica (`isActive = false`) e observações.
  2. **Validação Sintática Estrita de CNPJ (`CnpjValidator.java`):**
     - Algoritmo determinístico módulo 11 calculando os 2 dígitos verificadores (pesos 5..2 e 6..2).
     - Rejeição rigorosa de CNPJs com tamanhos incorretos, caracteres não numéricos e sequências com todos os dígitos repetidos (`00000000000000` até `99999999999999`).
     - 26 testes exaustivos em `CnpjValidatorTest` cobrindo matriz completa de validações sintáticas.
  3. **Regra Anti-Alucinação Estrita (Sem Suposição Fiscal nem Consulta Indevida):**
     - O sistema **NUNCA** consulta a Receita Federal ou serviços de terceiros sem autorização formal e comprovante.
     - O sistema **NUNCA** presume nem infere situação cadastral (Ativa, Baixada, Suspensa), enquadramento tributário (Simples, Presumido, Real) ou alíquota fiscal padrão a partir do CNPJ.
     - Valores de regime tributário e inscrição estadual são puramente declaratórios fornecidos pelo usuário no tenant.
  4. **Vínculo Explícito entre Marketplace Account e Empresa (`CompanyAccountLinkService.java`):**
     - Uma conta de vendedor (`SellerAccount`) só pode possuir **um único vínculo ativo** simultaneamente (`isActive = true`, `effectiveTo = null`).
     - Proibição de vínculo duplicado: tentativa de vincular à mesma empresa dispara conflito HTTP 409 (`DuplicateResourceException`).
     - Proibição de vínculo ambíguo: se uma conta já estiver vinculada a outra empresa, qualquer tentativa de vínculo adicional sem consentimento explícito (`replaceActiveLink = false`) é sumariamente bloqueada com HTTP 409 (`DuplicateResourceException`).
  5. **Manutenção Integral e Imutável de Histórico:**
     - Quando `replaceActiveLink = true`, o vínculo ativo anterior tem sua vigência encerrada (`effectiveTo = now()`, `isActive = false`), preservando os registros pretéritos para auditoria e apuração contábil retroativa. O novo vínculo é criado com versionamento incremental (`version = previousVersion + 1`).
     - Histórico de contas consultável via `GET /api/v1/companies/accounts/{sellerAccountId}/history`.
  6. **Isolamento Multi-Tenant e RLS no PostgreSQL 17 (`PostgresCompanyRlsIntegrationTest.java`):**
     - Migrações Flyway `V13__companies_and_account_links.sql` e `V13_1__postgresql_rls_companies.sql` com políticas RLS forçadas (`FORCE ROW LEVEL SECURITY`).
     - Testes comprovam que Tenant Beta não enxerga empresas nem vínculos do Tenant Alpha, e tentativas de injeção direta cross-tenant são bloqueadas pelo PostgreSQL com violação de política RLS.
  7. **Não-Escopo Respeitado:**
     - Nenhuma gestão de usuários, times, cargos ou permissões de equipe foi implementada neste módulo.
  8. **Testes Automatizados & Builds:**
     - 45/45 novos testes do módulo Company aprovados (26 unitários de CNPJ, 5 unitários de empresa, 7 unitários de vínculo, 4 de controller, 3 de integração RLS PostgreSQL).
     - 322/322 testes de backend mantidos com 100% de sucesso (0 falhas, 0 erros, 0 skips).
     - 170/170 testes de frontend mantidos com 100% de sucesso.
     - Total geral do projeto: 492 testes automatizados aprovados sem regressões.
- **Resultado:** **`PROMPT P016-BE = APPROVED`**. O prompt subsequente (`P016-FE`) está formalmente liberado e transita para `READY`.

---

### Transição: PROMPT P016-FE $\rightarrow$ `APPROVED`

- **Data/Hora:** 19/11/2026 11:30 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. **Validação e Máscara Sintática de CNPJ em Tempo Real (`CompanyFormModal.tsx`, `cnpj.ts`):**
     - Formatação dinâmica `00.000.000/0000-00` enquanto o usuário digita.
     - Validação estrita por algoritmo módulo 11 (2 dígitos verificadores e rejeição de 14 dígitos idênticos).
     - Feedback visual imediato e bloqueio de envio com mensagem acessível (`role="alert"`) caso o CNPJ esteja sintaticamente inválido.
  2. **Proteção de Privacidade & Não-Exposição de CNPJ sem Necessidade:**
     - Na listagem de empresas, o CNPJ é ofuscado por padrão (`33.***.***/0001-01`), preservando prefixo, filial e dígitos de controle sem expor a raiz corporativa.
     - Botão acessível de alternância com `aria-label` permite revelar o CNPJ completo sob demanda pelo usuário.
     - Função `sanitizeCnpjForLog` garante que logs de depuração e telemetria não registrem CNPJs completos.
  3. **Vínculo Explícito e Prevenção de Ambiguidade (`LinkAccountModal.tsx`):**
     - Seleção inequívoca de empresa e conta de marketplace conectada.
     - Detecção proativa de vínculo ativo existente: caso a conta já pertença a outra empresa, a interface exibe aviso de transferência de titularidade e exige consentimento formal via checkbox (`replaceActiveLink: true`), encerrando o vínculo anterior e mantendo a vigência histórica.
     - Vínculo à mesma empresa é devidamente bloqueado com aviso explicativo.
  4. **Aplicação de Permissões por Papel (RBAC):**
     - Usuários com perfil somente leitura (`VIEWER` / `ANALYST`) visualizam dados cadastrais e vínculos com botões de criação, edição, desativação e transferência desabilitados (`aria-disabled="true"`).
     - Banner informativo orienta sobre a restrição de privilégios.
  5. **Responsividade Mobile & Acessibilidade WCAG 2.2 AA:**
     - Layout 100% responsivo para viewports móveis (360px+).
     - Todos os botões e áreas de toque com altura mínima $\ge 44\text{px}$.
     - Suporte a teclado completo: tecla `Escape` fecha modais e gavetas com foco gerenciado.
  6. **Consulta de Histórico e Linha do Tempo (`AccountLinkHistoryDrawer.tsx`):**
     - Gaveta acessível exibindo histórico completo de vínculos de cada conta com versão, empresa associada, vigência (de/até) e status ativo/encerrado.
  7. **Testes Automatizados & Builds:**
     - 15 novos testes de frontend aprovados (6 em `cnpj.test.ts`, 8 em `CompaniesAndAccountsView.test.tsx`, 1 em `AppShell.test.tsx`).
     - 185/185 testes de frontend mantidos com 100% de sucesso em 28 arquivos.
     - 322/322 testes de backend mantidos com 100% de sucesso.
     - Total geral do projeto: 507 testes automatizados aprovados (0 falhas, 0 erros, 0 skips).
     - TypeScript verificado com zero erros (`tsc --noEmit`).
     - Next.js 15.1.7 build de produção compilado com sucesso com 47 rotas.
- **Resultado:** **`PROMPT P016-FE = APPROVED`**. O prompt subsequente (`P017-BE`) está formalmente liberado e transita para `READY`.

---

### Transição: PROMPT P017-BE $\rightarrow$ `APPROVED`

- **Data/Hora:** 19/11/2026 12:05 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. **Modelo de Fatos Financeiros por Pedido e por Item (`OrderFinancialFactRecord.java`, `OrderItemFinancialFactRecord.java`):**
     - Estrutura completa de parcelas: receita bruta (`gross_revenue`), desconto (`discount_amount`), receita líquida (`net_revenue`), cancelamento (`cancellation_amount`), devolução (`return_amount`), comissão (`marketplace_fee`), tarifa fixa (`fixed_fee`), frete (`shipping_cost`), custo de aquisição/CMV (`product_cost`), embalagem (`packaging_cost`), adicionais (`additional_cost`), imposto informado (`tax_amount`), outras despesas informadas (`other_expenses_amount`), deduções totais (`total_deductions`), margem de contribuição em valor (`contribution_margin_amount`) e margem percentual (`contribution_margin_ratio`).
  2. **Diferenciação Estrita de Proveniência (`FinancialValueProvenance`):**
     - `CONFIRMED`: valores conferidos e confirmados por extrato/liquidação externa do canal.
     - `ESTIMATED`: valores apurados deterministamente via regras vigentes (`FeeResolutionService`).
     - `USER_INFORMED`: custos unitários de aquisição, insumos e alíquotas fiscais declaradas pelo usuário no sistema (`ProductCostRecord`).
     - `MISSING`: valores ausentes ou não parametrizados, explicitados formalmente sem inventar valores zerados (Anti-Alucinação).
  3. **Reconciliabilidade Matemática Aditiva Rigorosa:**
     - A soma de todas as parcelas dos itens (`gross_revenue`, `shipping_cost`, `total_deductions`, `contribution_margin_amount`) é exatamente idêntica ao total consolidado do cabeçalho do pedido.
     - Rateio proporcional determinístico do frete do pedido entre os itens com compensação de centavos no último item, garantindo zero resíduos de arredondamento.
     - Aritmética financeira com `BigDecimal`, escala 4 (`NUMERIC(15, 4)`), modo bancário `RoundingMode.HALF_EVEN`.
  4. **Reprocessamento Idempotente:**
     - Restrições de unicidade `(tenant_id, order_id)` e `(tenant_id, order_item_id)`.
     - Reprocessar o mesmo pedido N vezes executa atualização atômica sem gerar duplicatas.
  5. **Regra Anti-Alucinação Estrita (Proibição de Lucro Líquido):**
     - Enquanto despesas fixas estruturais do negócio não estiverem modeladas, qualquer campo ou menção a lucro líquido é sumariamente proibido.
     - Campo obrigatório `antiHallucinationDisclaimer` presente em entidades, DTOs e trilhas de cálculo: _"Este demonstrativo apura estritamente a Margem de Contribuição (Receita Líquida deduzida de custos diretos variáveis, comissões, fretes e impostos informados). Despesas fixas estruturais e operacionais da empresa não estão contempladas, sendo proibido rotular este valor como Lucro Líquido."_
  6. **Reconciliação com Origem Externa (`ReconcileSettlementCommand`):**
     - Comando `POST /api/v1/financial-facts/orders/{orderId}/reconcile` para vincular repasse externo (`settlementExternalId`, `settlementDate`).
     - Comparação automática entre tarifas estimadas e confirmadas: diferença $\le 0.01$ transita para `RECONCILED`; diferença $> 0.01$ transita para `DIVERGENT` com auditoria das discrepâncias.
     - Recálculo das margens reais com base nos valores confirmados e rateio proporcional entre os itens.
  7. **Trilha de Cálculo Auditável em JSON (`calculation_trace_json`):**
     - Registro estruturado com versão da engine (`v1.0.0`), timestamp, ID da empresa vinculada, regras de comissão aplicadas, custos unitários adotados e motivos de dados ausentes.
  8. **Isolamento Multi-Tenant e RLS no PostgreSQL 17 (`PostgresFinancialFactsRlsIntegrationTest.java`):**
     - Migrações Flyway `V14__financial_facts_and_reconciliation.sql` e `V14_1__postgresql_rls_financial_facts.sql` com políticas RLS forçadas (`FORCE ROW LEVEL SECURITY`).
     - Testes comprovam isolamento hermético entre tenants e bloqueio de violações cross-tenant pelo PostgreSQL.
  9. **Testes Automatizados & Builds:**
     - 12 novos testes de backend aprovados (7 unitários/serviço, 2 de controller REST, 3 de integração RLS PostgreSQL).
     - 334/334 testes de backend mantidos com 100% de sucesso (0 falhas, 0 erros, 0 skips).
     - 185/185 testes de frontend mantidos com 100% de sucesso em 28 arquivos.
     - Total geral do projeto: 519 testes automatizados aprovados sem regressões.
     - ModularArchitectureTest (ArchUnit): 3/3 regras de arquitetura limpa e sem ciclos aprovadas.
- **Resultado:** **`PROMPT P017-BE = APPROVED`**. O prompt subsequente (`P017-FE`) está formalmente liberado e transita para `READY`.

---

### Transição: PROMPT P017-FE $\rightarrow$ `APPROVED`

- **Data/Hora:** 19/11/2026 13:00 BRT
- **Avaliador:** Tech Lead / Staff Frontend Engineer
- **Critérios Validados:**
  1. **Totais Reconciliam (Aditividade Estrita e DRE Gerencial em Cascata):**
     - O painel financeiro exibe cards de KPI agregados: Receita Bruta, Descontos de Venda, Receita Líquida, Deduções Totais e Margem de Contribuição apurada.
     - A soma das parcelas individuais da tabela e dos itens da gaveta lateral (`FinancialFactDetailDrawer`) bate rigorosamente com os agregados do pedido e do resumo (`/api/financial-facts/summary`).
     - Cascata DRE gerencial simplificada calculada com exatidão: $\text{Receita Bruta} - \text{Descontos} = \text{Receita Líquida} - (\text{Comissões} + \text{Tarifas Fixas} + \text{Fretes} + \text{Custos de Produto} + \text{Impostos}) = \text{Margem de Contribuição}$.
  2. **Filtros Combináveis:**
     - Barra de filtros (`FinancialFiltersBar`) com suporte completo a presets de período (Hoje, Últimos 7 dias, Últimos 30 dias, Mês Atual, Personalizado).
     - Filtros simultâneos e combináveis por Empresa/CNPJ, Canal/Marketplace (Todos, Mercado Livre, Shopee), Conta Conectada, SKU e Status de Reconciliação (`ESTIMATED`, `RECONCILED`, `DIVERGENT`, `MISSING_DATA`).
     - Indicador de filtros ativos e botão "Limpar filtros" para restauração rápida.
  3. **Definições Acessíveis dos Indicadores (Regra Anti-Alucinação Estrita):**
     - **PROIBIDO ROTULAR OU EXIBIR MARGEM DE CONTRIBUIÇÃO COMO LUCRO LÍQUIDO:** Despesas fixas estruturais do negócio (aluguel, pró-labore, folha de pagamento, contabilidade, infraestrutura) não estão modeladas.
     - Banner de disclaimer anti-alucinação persistente e em destaque: _"Aviso de Conformidade Financeira: Os indicadores apuram estritamente a Margem de Contribuição após custos e deduções variáveis diretas da venda. Despesas fixas estruturais do negócio não estão contempladas; é terminantemente proibido rotular este valor como Lucro Líquido."_
     - Todos os cards de KPIs possuem botão de ajuda acessível com semântica ARIA (`aria-label`, `role="tooltip"`), definição formal, fórmula matemática e o disclaimer anti-alucinação.
  4. **Alertas e Gestão de Incompletude de Dados:**
     - Identificação visual clara via badge pulsante e banner interativo quando há pedidos com custos ausentes (`PRODUCT_COST_MISSING`) ou regras de taxas não cadastradas (`MARKETPLACE_FEE_RULE_MISSING`).
     - Filtro com 1 clique para isolar pedidos incompletos e orientações acionáveis para cadastrar custos no módulo financeiro.
  5. **Detalhamento de Componentes (Drawer & Modal de Reconciliação Externa):**
     - Gaveta lateral (`FinancialFactDetailDrawer`) com DRE em cascata do pedido, tabela de itens com rateio proporcional de frete e tarifas, e exibição expandível da trilha de cálculo JSON auditável (`calculation_trace_json`) com versão da fórmula (`v1.0.0`).
     - Modal de conciliação de liquidação externa (`ReconcileSettlementModal`) permitindo registrar ID do repasse, data e tarifas confirmadas, com cálculo de divergência em tempo real e reprocessamento idempotente via BFF Next.js.
  6. **Acessibilidade, Responsividade & Usabilidade:**
     - Layout móvel fluido com cards em grid responsivo, tabelas com scroll horizontal seguro, touch targets $\ge 44\text{px}$, suporte integral a navegação por teclado e tecla `Escape` para fechar modais e gavetas.
  7. **Testes Automatizados, TypeScript & Build de Produção:**
     - 7 novos testes de frontend aprovados (6 em `FinancialFactsDashboardView.test.tsx`, 1 em `AppShell.test.tsx`).
     - 192/192 testes de frontend mantidos com 100% de sucesso em 29 arquivos.
     - 334/334 testes de backend mantidos com 100% de sucesso.
     - Total geral do projeto: 526 testes automatizados aprovados (0 falhas, 0 erros, 0 skips).
     - TypeScript verificado com zero erros (`tsc --noEmit`).
     - Next.js 15.1.7 build de produção compilado com 52 rotas limpas.
- **Resultado:** **`PROMPT P017-FE = APPROVED`**. O prompt subsequente (`P018-BE`) está formalmente liberado e transita para `READY`.

---

### Transição: PROMPT P018-BE $\rightarrow$ `APPROVED`

- **Data/Hora:** 19/11/2026 13:35 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. **Catálogo de Métricas Documentado (`OfficialMetricCatalog.java`):**
     - Inventário exaustivo por marketplace com escopo (`ACCOUNT`, `LISTING`), granularidade (`DAILY`, `PERIOD_TOTAL`), unidade (`COUNT`, `CURRENCY`, `PERCENTAGE`), endpoint oficial, limites operacionais/quotas e janela temporal máxima suportada.
     - Mercado Livre: `VISITS` (100 req/min, até 365 dias), `ORDERS_COUNT`, `UNITS_SOLD`, `REVENUE`, `CONVERSION_RATE_ORDERS`, `CONVERSION_RATE_UNITS`.
     - Shopee (OpenAPI v2): `PAGE_VIEWS`, `UNIQUE_VISITORS`, `ORDERS_COUNT`, `UNITS_SOLD`, `REVENUE`, `IMPRESSIONS` (Ads), `CLICKS` (Ads), `CTR` (Ads), `CONVERSION_RATE_ORDERS`, `CONVERSION_RATE_UNITS`.
  2. **Ausência Representada como Indisponível (Regra Anti-Alucinação Estrita):**
     - **Proibição absoluta de estimativas silenciosas ou substituição por zero:** No tráfego orgânico do Mercado Livre, `IMPRESSIONS` e `CLICKS` não são disponibilizados para sellers (exclusivos de Mercado Ads contratado); a métrica `CTR` é categoricamente persistida como `UNSUPPORTED_BY_CHANNEL` com valor `null` e motivo explícito.
     - Métricas concorrenciais de terceiros (`COMPETITOR_SALES_VOLUME`, `COMPETITOR_CTR`) são catalogadas como `UNSUPPORTED_BY_CHANNEL` e proibidas de inferência sintética.
  3. **Fórmulas Determinísticas Testadas (`MetricFormulaEngine.java`):**
     - $\text{CTR} = \frac{\text{clicks}}{\text{impressions}} \times 100$ computado com `BigDecimal`, escala 6 e arredondamento `HALF_EVEN`.
     - $\text{Conversion Rate (Orders)} = \frac{\text{orders\_count}}{\text{visits}} \times 100$ e $\text{Conversion Rate (Units)} = \frac{\text{units\_sold}}{\text{visits}} \times 100$.
     - Tratamento formal de divisão por zero: quando o denominador for 0, o status é classificado como `INSUFFICIENT_DATA` com valor `0.0000` e denominador `0.0000` registrado, sem disparar exceções não tratadas.
     - Rejeição de anomalias estatísticas (`clicks > impressions`) e valores negativos.
  4. **Origem e Janela Registradas (`connected_account_metrics`):**
     - Metadados completos gravados por registro: `source_endpoint`, `window_start`, `window_end`, `granularity`, `ingested_at`, `provenance` (`OFFICIAL_API`, `DERIVED_FORMULA`, `UNAVAILABLE`), `formula_definition`, `denominator_metric` e `denominator_value`.
     - Idempotência externa rigorosa: restrição única composta `(tenant_id, seller_account_id, marketplace, target_identifier, metric_key, granularity, window_start, window_end)` garantindo atualização atômica sem duplicatas ao reprocessar a mesma janela.
  5. **Isolamento Multi-Tenant e RLS no PostgreSQL 17 (`PostgresConnectedAccountMetricsRlsIntegrationTest.java`):**
     - Migrações Flyway `V15__connected_account_metrics.sql` e `V15_1__postgresql_rls_connected_account_metrics.sql` com `FORCE ROW LEVEL SECURITY`.
     - Testes comprovam que Tenant Beta não enxerga métricas do Tenant Alpha e tentativas de inserção cross-tenant são bloqueadas pelo PostgreSQL com violação de política.
  6. **Testes Automatizados & Arquitetura:**
     - 29 novos testes de backend aprovados (11 em `MetricFormulaEngineTest`, 5 em `OfficialMetricCatalogTest`, 6 em `AccountMetricIngestionServiceTest`, 5 em `AccountMetricControllerTest`, 2 em `PostgresConnectedAccountMetricsRlsIntegrationTest`).
     - 363/363 testes de backend mantidos com 100% de sucesso (0 falhas, 0 erros, 0 skips).
     - 192/192 testes de frontend mantidos com 100% de sucesso em 29 arquivos.
     - Total geral do projeto: 555 testes automatizados aprovados sem regressões.
     - ModularArchitectureTest (ArchUnit): conformidade de pacotes e zero ciclos mantidos.
- **Resultado:** **`PROMPT P018-BE = APPROVED`**. O prompt subsequente (`P018-FE`) está formalmente liberado e transita para `READY`.

---

### Transição: PROMPT P018-FE $\rightarrow$ `APPROVED`

- **Data/Hora:** 19/11/2026 14:12 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. **Período e Timezone Explícitos (`ListingAnalyticsDashboardView.tsx`):**
     - Exibição em destaque do fuso horário de referência (`America/Sao_Paulo (UTC-03:00)`).
     - Presets de período configuráveis (Hoje, 7 Dias, 14 Dias, 30 Dias, Mês Atual e Personalizado) com normalização UTC na camada BFF e datas locais amigáveis.
  2. **Cálculos Conferem e Comparação Temporal Determinística (`MetricKpiGrid.tsx`):**
     - Variação percentual ($\Delta\%$) e absoluta calculadas frente ao período anterior de igual duração.
     - Tratamento determinístico de métricas sem histórico anterior ("Sem base anterior"), evitando alucinações como $+100\%$ ou divisão por zero.
     - Indicadores de Visitas, Pedidos, Unidades, Receita, Conversão de Pedidos e CTR com botões acessíveis de ajuda ARIA e tooltips de fórmulas oficiais.
  3. **Gráficos e Detecção Explícita de Lacunas (`MetricTimeSeriesChart.tsx`):**
     - **Gráficos não mascaram dados ausentes:** As séries temporais sinalizam explicitamente datas sem dados coletados com zonas de lacuna e alerta visual (`Aviso de lacunas de coleta`), quebrando as linhas contínuas (`discontinuous line segments`).
     - Proibição estrita de interpolar dados ausentes ou plotar dias não coletados como 0.
     - Alternância de visualização entre gráfico SVG responsivo e tabela diária de dados acessível por leitores de tela.
  4. **Aviso de Indisponibilidade & Regra Anti-Alucinação Estrita:**
     - CTR de tráfego orgânico no Mercado Livre e métricas de concorrentes classificadas com badge `UNSUPPORTED_BY_CHANNEL` e justificativa explícita.
     - Banner institucional permanente afirmando a conformidade com APIs oficiais homologadas.
     - Gaveta deslizante (`MetricCatalogDrawer.tsx`) para consulta das quotas de API, rate limits e limitações de cada canal.
  5. **Testes Automatizados & Builds:**
     - 7 novos testes criados em `ListingAnalyticsDashboardView.test.tsx` (100% aprovados).
     - 1 novo teste de integração de navegação em `AppShell.test.tsx` (14/14 testes aprovados).
     - Suíte frontend: 200/200 testes aprovados em 30 arquivos (0 falhas).
     - TypeScript verificado com zero erros (`npx tsc --noEmit`).
     - Build de produção Next.js 15.1.7 (`next build`) aprovado com 42 rotas compiladas e geradas.
     - Suíte backend: 363/363 testes aprovados (0 falhas, 0 erros, 0 skips).
     - Total geral do projeto: 563 testes automatizados aprovados (363 backend + 200 frontend).
- **Resultado:** **`PROMPT P018-FE = APPROVED`**. O prompt subsequente (`P019-BE`) está formalmente liberado e transita para `READY`.

---

### Transição: PROMPT P019-BE $\rightarrow$ `APPROVED`

- **Data/Hora:** 19/11/2026 15:37 BRT
- **Avaliador:** Tech Lead / Staff Software Engineer
- **Critérios Validados:**
  1. **Isolamento Multi-Tenant Hermético nas Tools Internas (`AiToolRegistry.java`, `InternalAiTools.java`):**
     - O modelo não recebe SQL livre, tokens de marketplace ou acesso irrestrito.
     - As tools executam sob o `authenticatedTenantId` do `TenantContextHolder` e validam autorização de forma autônoma.
     - Tentativas de injeção de parâmetros cross-tenant pelo LLM (argumentos `tenantId` ou `tenant_id` divergentes) são sumariamente interceptadas com `SecurityException`.
     - Tools registradas: `search_listings`, `calculate_margin` e `summarize_metrics`.
  2. **Defesa em Profundidade contra Prompt Injection (`PromptInjectionDefenseService.java`):**
     - Detecção e bloqueio de tentativas de substituição de instruções (`INSTRUCTION_OVERRIDE`), jailbreaks de persona (`JAILBREAK_PERSONA`), exfiltração de prompts/regras (`PROMPT_LEAKAGE_ATTEMPT`), delimitadores estruturais (`DELIMITER_INJECTION`) e SQL injection (`DATA_EXFILTRATION_SQL`).
     - Contenção e sanitização estrita de fontes externas em tags `<untrusted_external_content context="..." sanitized="true">` com neutralização de tags maliciosas.
     - Tentativas de ataque são bloqueadas imediatamente (`PromptInjectionException`) e registradas em auditoria com `status = "BLOCKED_SECURITY"`.
  3. **Prevenção Rigorosa de Alucinações (No Silent Hallucinations):**
     - Ausência de dados no catálogo próprio ou analytics retorna formalmente status `UNAVAILABLE` ou `INSUFFICIENT_DATA` acompanhada de justificativa detalhada, lista de fontes consultadas e timestamps UTC.
     - A ferramenta `summarize_metrics` explicita a ausência de CTR orgânico no Mercado Livre e a ferramenta `calculate_margin` emite disclaimer formal de que Margem de Contribuição não deduz despesas fixas da empresa.
  4. **Segurança e Gerenciamento de Segredos (`DefaultAiSecretManager.java`):**
     - Chaves de API gerenciadas via `AiSecretManager` com suporte a Gemini, OpenAI, Claude e Mock determinístico.
     - Mascaramento mandatório de credenciais (`sk-***[last4]`), impedindo qualquer exposição de chaves em logs, payloads ou respostas REST.
     - Falha controlada fail-closed (`IllegalStateException`) quando uma credencial de provedor externo não estiver configurada.
  5. **Monitoramento de Custo, Latência e Auditoria com RLS (`ai_usage_audits`):**
     - Registro obrigatório de cada interação: tokens de entrada, tokens de conclusão, latência em milissegundos, ferramentas acionadas e custo financeiro determinístico estimado em BRL.
     - Migrações Flyway `V16__ai_gateway_and_audit.sql` e `V16_1__postgresql_rls_ai_gateway.sql` com ativação e imposição mandatória de `FORCE ROW LEVEL SECURITY`.
     - `PostgresAiAuditRlsIntegrationTest.java` em PostgreSQL 17 real comprova que um tenant nunca visualiza registros de auditoria de outro tenant.
  6. **Testes Automatizados & Arquitetura:**
     - 24 novos testes de backend criados e aprovados:
       - 11 testes em `PromptInjectionDefenseServiceTest`
       - 4 testes em `DefaultAiSecretManagerTest`
       - 7 testes em `AiToolRegistryAndInternalToolsTest`
       - 5 testes em `AiGatewayServiceTest`
       - 7 testes em `AiAssistantControllerTest`
       - 1 teste de integração Testcontainers PostgreSQL 17 em `PostgresAiAuditRlsIntegrationTest`
     - Suíte backend: 398/398 testes aprovados (0 falhas, 0 erros, 0 skips).
     - Suíte frontend: 200/200 testes aprovados em 30 arquivos (0 falhas).
     - Total geral do projeto: 598 testes automatizados aprovados sem regressões.
     - ModularArchitectureTest (ArchUnit): conformidade estrita mantida com zero dependências cíclicas.
- **Resultado:** **`PROMPT P019-BE = APPROVED`**. O prompt subsequente (`P019-FE`) está formalmente liberado e transita para `READY`.
