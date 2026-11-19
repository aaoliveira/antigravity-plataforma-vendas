# Status de Implementação e Quality Gates do Roadmap

> **Status Atual Geral:** PROMPT P020-FE = `PASSED` | P021-BE = `READY`  
> **Data da Última Atualização:** 19/11/2026  
> **Regra de Transição:** O próximo prompt só é liberado para `IN_PROGRESS` quando o anterior atingir formalmente `PASSED`.

---

## 1. Rastreamento Sequencial de Prompts

| Prompt ID   | Título da Entrega                                                                        | Épico | Camada     | Estado do Gate | Pré-requisito         | Data Conclusão |
| ----------- | ---------------------------------------------------------------------------------------- | ----- | ---------- | -------------- | --------------------- | -------------- |
| **P000**    | Master Project Governance & Engineering Rules                                            | E00   | Governança | **`PASSED`**   | ---                   | 17/11/2026     |
| **P001-BE** | Estrutura Base do Repositório e Backend Modular                                          | E01   | Backend    | **`PASSED`**   | P000 (`PASSED`)       | 17/11/2026     |
| **P001-FE** | Shell da Aplicação Web Responsiva                                                        | E01   | Frontend   | **`PASSED`**   | P001-BE (`PASSED`)    | 17/11/2026     |
| **P002-BE** | Identidade Mínima, Autenticação e Contexto do Tenant                                     | E02   | Backend    | **`PASSED`**   | P001 (`PASSED`)       | 18/11/2026     |
| **P002-FE** | Interface de Autenticação e Troca de Contexto                                            | E02   | Frontend   | **`PASSED`**   | P002-BE (`PASSED`)    | 18/11/2026     |
| **P003-BE** | Isolamento Multi-Tenant em Profundidade (RLS, Transacional e Testcontainers)             | E03   | Backend    | **`PASSED`**   | P002 (`PASSED`)       | 18/11/2026     |
| **P003-FE** | Tratamento Padronizado de Erros (401/403/404) e Segurança de Tenant na UI                | E03   | Frontend   | **`PASSED`**   | P003-BE (`PASSED`)    | 18/11/2026     |
| **P004-BE** | Contrato MarketplaceAdapter, Registro de Capacidades, Evidências e Camada de Resiliência | E04   | Backend    | **`PASSED`**   | P003 (`PASSED`)       | 18/11/2026     |
| **P004-FE** | Interface de Gestão de Conexões Oficiais, Capacidades e Evidências                       | E04   | Frontend   | **`PASSED`**   | P004-BE (`PASSED`)    | 18/11/2026     |
| **P005-BE** | Integração Oficial OAuth 2.0 Mercado Livre, PKCE, Cifragem KMS e Rotação Segura          | E04   | Backend    | **`PASSED`**   | P004 (`PASSED`)       | 18/11/2026     |
| **P005-FE** | Fluxo Oficial de Conexão com Mercado Livre (OAuth 2.0 PKCE UI)                           | E04   | Frontend   | **`PASSED`**   | P005-BE (`PASSED`)    | 18/11/2026     |
| **P006-BE** | Integração Shopee Open Platform v2, Assinatura HMAC, Webhooks e Políticas                | E04   | Backend    | **`PASSED`**   | P005-FE (`PASSED`)    | 18/11/2026     |
| **P006-FE** | Fluxo Oficial de Conexão com Shopee (Open Platform v2 UI)                                | E04   | Frontend   | **`PASSED`**   | P006-BE (`PASSED`)    | 18/11/2026     |
| **P007-BE** | Catálogo Canônico, Anúncios, Mapeamentos Externos e Sincronização Resiliente             | E05   | Backend    | **`PASSED`**   | P006-FE (`PASSED`)    | 18/11/2026     |
| **P007-FE** | Catálogo Responsivo, Busca, Filtros Combináveis e Detalhe de Anúncio                     | E05   | Frontend   | **`PASSED`**   | P007-BE (`PASSED`)    | 18/11/2026     |
| **P008-BE** | Ingestão de Pedidos e Itens Próprios, Reconciliação, Outbox e RLS                        | E04   | Backend    | **`PASSED`**   | P007-FE (`PASSED`)    | 18/11/2026     |
| **P008-FE** | Gestão e Detalhes de Pedidos com Filtros por Canal, Status e Incompletos                 | E04   | Frontend   | **`PASSED`**   | P008-BE (`PASSED`)    | 18/11/2026     |
| **P009-BE** | Modelo Comum para Observações de Mercado, Proveniência, Deduplicação e Matching          | E06   | Backend    | **`PASSED`**   | P008-FE (`PASSED`)    | 18/11/2026     |
| **P009-FE** | Exibição de Origem, Evidências, Confiança e Explicação de Dados na UI                    | E06   | Frontend   | **`PASSED`**   | P009-BE (`PASSED`)    | 18/11/2026     |
| **P010-BE** | Pesquisa de Mercado e Concorrência Autorizada, Registro de Fontes e Cache                | E06   | Backend    | **`PASSED`**   | P009-FE (`PASSED`)    | 18/11/2026     |
| **P010-FE** | Interface de Pesquisa Autorizada, Políticas de Fontes e Fallback Seguro                  | E06   | Frontend   | **`PASSED`**   | P010-BE (`PASSED`)    | 18/11/2026     |
| **P011-BE** | Comparação de Listings Equivalentes, Variação Temporal e Histórico Não-Inventado         | E06   | Backend    | **`PASSED`**   | P010-FE (`PASSED`)    | 18/11/2026     |
| **P011-FE** | Interface de Comparação de Listings, Histórico Real e Alertas de Incompatibilidade       | E06   | Frontend   | **`PASSED`**   | P011-BE (`PASSED`)    | 18/11/2026     |
| **P012-BE** | Grupos por Tenant, Itens Monitorados e Snapshots Históricos Seguros                      | E05   | Backend    | **`PASSED`**   | P011-FE (`PASSED`)    | 18/11/2026     |
| **P012-FE** | Interface de Grupos de Monitoramento, Snapshots e Alertas                                | E05   | Frontend   | **`PASSED`**   | P012-BE (`PASSED`)    | 18/11/2026     |
| **P007-FE** | Simulador de Preço e Painel de Rentabilidade                                             | E07   | Frontend   | `NOT_STARTED`  | P007-BE (`PASSED`)    | ---            |
| **P008-BE** | Analytics de Anúncios Próprios e Métricas Oficiais                                       | E08   | Backend    | `NOT_STARTED`  | P004 (`PASSED`)       | ---            |
| **P008-FE** | Dashboard de Desempenho e Saúde de Anúncios                                              | E08   | Frontend   | `NOT_STARTED`  | P008-BE (`PASSED`)    | ---            |
| **P009-BE** | Consolidação Financeira, DRE e Reconciliação                                             | E09   | Backend    | `NOT_STARTED`  | P007 (`PASSED`)       | ---            |
| **P009-FE** | Painel Financeiro Consolidado Multi-CNPJ                                                 | E09   | Frontend   | `NOT_STARTED`  | P009-BE (`PASSED`)    | ---            |
| **P011-BE** | IA Assistiva com Tool Calling Somente Leitura                                            | E10   | Backend    | `NOT_STARTED`  | P007, P008 (`PASSED`) | ---            |
| **P011-FE** | Chat Assistente e Geração Assistida de Anúncios                                          | E10   | Frontend   | `NOT_STARTED`  | P011-BE (`PASSED`)    | ---            |
| **P012-BE** | Exportação Segura de Relatórios e Auditoria                                              | E11   | Backend    | `NOT_STARTED`  | P009 (`PASSED`)       | ---            |
| **P012-FE** | Central de Relatórios e Downloads Auditados                                              | E11   | Frontend   | `NOT_STARTED`  | P012-BE (`PASSED`)    | ---            |
| **P013-BE** | Snapshots Idempotentes, Detecção de Alterações e Resiliência de Coleta                   | E05   | Backend    | `PASSED`       | P012-FE (`PASSED`)    | 18/11/2026     |
| **P013-FE** | Gráfico Histórico, Tooltips de Fonte e Lacunas Visíveis                                  | E05   | Frontend   | **`PASSED`**   | P013-BE (`PASSED`)    | 19/11/2026     |
| **P014-BE** | Cadastro de Custos por Produto/Variante/Empresa e Regras de Taxas                        | E07   | Backend    | **`PASSED`**   | P013-FE (`PASSED`)    | 19/11/2026     |
| **P014-FE** | Interface de Gestão de Custos, Tabela de Taxas e Simulador de Comissões                  | E07   | Frontend   | **`PASSED`**   | P014-BE (`PASSED`)    | 19/11/2026     |
| **P015-BE** | Motor de Precificação e Rentabilidade com Custos e Taxas                                 | E07   | Backend    | **`PASSED`**   | P014-FE (`PASSED`)    | 19/11/2026     |
| **P015-FE** | Simulador de Precificação, Margens e Ponto de Equilíbrio                                 | E07   | Frontend   | **`PASSED`**   | P015-BE (`PASSED`)    | 19/11/2026     |
| **P016-BE** | Empresas/CNPJs e Vínculo Financeiro de Contas Conectadas                                 | E07   | Backend    | **`PASSED`**   | P015-FE (`PASSED`)    | 19/11/2026     |
| **P016-FE** | Cadastro de Empresas e Vínculo de Contas de Marketplace                                  | E07   | Frontend   | **`PASSED`**   | P016-BE (`PASSED`)    | 19/11/2026     |
| **P017-BE** | Fatos Financeiros por Pedido/Item, Reconciliação e Trilha de Cálculo                     | E09   | Backend    | **`PASSED`**   | P016-FE (`PASSED`)    | 19/11/2026     |
| **P017-FE** | Painel Financeiro, Filtros Combináveis e Detalhamento de Componentes                     | E09   | Frontend   | **`PASSED`**   | P017-BE (`PASSED`)    | 19/11/2026     |
| **P018-BE** | Inventário de Métricas Oficiais por Canal, Catálogo Documentado e Ingestão               | E08   | Backend    | **`PASSED`**   | P017-FE (`PASSED`)    | 19/11/2026     |
| **P018-FE** | Dashboard de Analytics por Anúncio, Período, Comparação Temporal e Aviso de Indisponível | E08   | Frontend   | **`PASSED`**   | P018-BE (`PASSED`)    | 19/11/2026     |
| **P019-BE** | Gateway Multi-Provider com Secrets, Defesa Prompt Injection e Tool Calling Read-Only     | E10   | Backend    | **`PASSED`**   | P018-FE (`PASSED`)    | 19/11/2026     |
| **P019-FE** | Assistente de IA Interativo, Chat com Ferramentas e Indicador de Custo/Origem             | E10   | Frontend   | **`PASSED`**   | P019-BE (`PASSED`)    | 19/11/2026     |
| **P020-BE** | Geração Assistida de Título e Descrição como Rascunho com Restrição de Claims             | E10   | Backend    | **`PASSED`**   | P019-FE (`PASSED`)    | 19/11/2026     |
| **P020-FE** | Editor de Rascunho com Original/Gerado, Contagem de Caracteres e Histórico de Versões     | E10   | Frontend   | **`PASSED`**   | P020-BE (`PASSED`)    | 19/11/2026     |
| **P021-BE** | Exportação Segura de Relatórios e Auditoria de Download                                   | E11   | Backend    | `READY`        | P020-FE (`PASSED`)    | ---            |

---

## 2. Máquina de Estados Oficial do Projeto

Cada funcionalidade ou prompt transita estritamente pelos seguintes estados:

```
[ NOT_STARTED ] ──(DoR Aprovada)──> [ READY ] ──(Iniciar Trabalho)──> [ IN_PROGRESS ]
                                                                             │
         ┌───────────────────────────────────────────────────────────────────┤
         ▼                                                                   ▼
   [ BLOCKED ] ◄──(Falha de Teste / Dúvida Impeditiva)                  (Todos os Gates OK)
         │                                                                   │
         ▼                                                                   ▼
   [ FIXING ] ──(Reexecutar Suíte Completa com Sucesso)──────────────>  [ PASSED ]
```
