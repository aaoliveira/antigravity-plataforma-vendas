# Matriz de Rastreabilidade de Requisitos (Requirements Traceability Matrix)

## 1. Visão Geral
Esta matriz mapeia todos os requisitos funcionais (**FR**) e não funcionais (**NFR**) catalogados no PRD oficial ([docs/product/PRD.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/product/PRD.md), Seções 5 e 7) para as respectivas fases de desenvolvimento, prompts de execução e suítes de testes automatizados planejadas.
A viabilidade técnica, fontes oficiais e matriz de capacidades foram auditadas no **Prompt 01** e documentadas em [docs/architecture/marketplace-capability-matrix.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/architecture/marketplace-capability-matrix.md) e [docs/architecture/DECISION_LOG.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/architecture/DECISION_LOG.md) (ADRs DEC-010 a DEC-013).

Nenhum requisito pode ser considerado concluído sem que os testes associados estejam implementados e aprovados pelo phase gate correspondente.

---

## 2. Requisitos Funcionais — Identidade, Tenant e Governança

| ID | Prioridade | Descrição do Requisito | Fase | Prompt Alvo | Tipo de Teste Planejado | Status Atual |
|---|---|---|---|---|---|---|
| **FR-ID-001** | P0 | Cadastro, login, logout e recuperação de acesso. | Fase 1 (MVP) | Prompt 03 | Integração (Keycloak/Spring Security), E2E (Playwright) | `VERIFIED (Prompt 03)` |
| **FR-ID-002** | P0 | Criar organização (tenant) e associar usuário Owner. | Fase 1 (MVP) | Prompt 03 | Unitário, Integração (JPA/Flyway), E2E | `VERIFIED (Prompt 03)` |
| **FR-ID-003** | P0 | Convidar usuários e atribuir papéis (RBAC). | Fase 1 (MVP) | Prompt 04 | Unitário (Domain), Integração (API/Mail) | `VERIFIED (Prompt 04)` |
| **FR-ID-004** | P0 | Isolar dados estruturalmente por tenant. | Fase 1 (MVP) | Prompt 03, Prompt 12 | Segurança (Adversarial cross-tenant), Testcontainers | `VERIFIED (Prompt 03)` |
| **FR-ID-005** | P0 | Gerenciar empresas/CNPJs, lojas e contas. | Fase 1 (MVP) | Prompt 04 | Unitário, Integração (JPA/PostgreSQL) | `VERIFIED (Prompt 04)` |
| **FR-ID-006** | P1 | Permissões granulares por empresa, loja e módulo. | Fase 1 (MVP) | Prompt 04 | Unitário, Integração (Security Interceptor) | `VERIFIED (Prompt 04)` |
| **FR-ID-007** | P0 | Trilha de auditoria para ações sensíveis. | Fase 1 (MVP) | Prompt 03, Prompt 04 | Integração (Audit Listener), Unitário | `VERIFIED (Prompt 03, Prompt 04)` |

---

## 3. Requisitos Funcionais — Integrações com Marketplaces

| ID | Prioridade | Descrição do Requisito | Fase | Prompt Alvo | Tipo de Teste Planejado | Status Atual |
|---|---|---|---|---|---|---|
| **FR-INT-001** | P0 | Conectar conta por fluxo OAuth/autorizado aplicável. | Fase 1 (MVP) | Prompt 05 | Integração (OAuth Handshake), MockServer | `VERIFIED (Prompt 05)` |
| **FR-INT-002** | P0 | Renovar tokens e tratar expiração/revogação. | Fase 1 (MVP) | Prompt 05 | Unitário, Integração com WireMock | `VERIFIED (Prompt 05)` |
| **FR-INT-003** | P0 | Sincronizar catálogo e anúncios próprios. | Fase 1 (MVP) | Prompt 06 | Integração (RabbitMQ Worker), Testcontainers | `VERIFIED (Prompt 06)` |
| **FR-INT-004** | P0 | Sincronizar pedidos próprios com idempotência. | Fase 1 (MVP) | Prompt 06 | Integração (Idempotency Key), Unitário | `VERIFIED (Prompt 06)` |
| **FR-INT-005** | P1 | Sincronizar campanhas e métricas de Ads autorizadas. | Fase 2 | Prompt 16 | Integração (Ads API Adapter), Unitário | `PLANNED` |
| **FR-INT-006** | P0 | Exibir saúde, histórico e última sincronização. | Fase 1 (MVP) | Prompt 05, Prompt 06 | Unitário, Integração (REST API) | `VERIFIED (Prompt 05, Prompt 06)` |
| **FR-INT-007** | P0 | Processar webhooks/eventos com proteção contra replay. | Fase 1 (MVP) | Prompt 06 | Integração (Signature Verification), Idempotência | `VERIFIED (Prompt 06)` |
| **FR-INT-008** | P0 | Disponibilizar importação CSV como contingência. | Fase 1 (MVP) | Prompt 06 | Unitário (CSV Parser), Integração | `VERIFIED (Prompt 06)` |
| **FR-INT-009** | P1 | Catálogo de capacidades por marketplace. | Fase 1 (MVP) | Prompt 05 | Unitário (Matrix Registry), Integração | `VERIFIED (Prompt 05)` |

---

## 4. Requisitos Funcionais — Market Intelligence (Pesquisa de Mercado)

| ID | Prioridade | Descrição do Requisito | Fase | Prompt Alvo | Tipo de Teste Planejado | Status Atual |
|---|---|---|---|---|---|---|
| **FR-MKT-001** | P0 | Pesquisar produtos por palavra-chave. | Fase 1 (MVP) | Prompt 07 | Integração (Search Service), Unitário | `VERIFIED (Prompt 07)` |
| **FR-MKT-002** | P0 | Pesquisar por URL ou identificador de anúncio. | Fase 1 (MVP) | Prompt 07 | Unitário (Normalizer), Integração | `VERIFIED (Prompt 07)` |
| **FR-MKT-003** | P0 | Filtrar por categoria, preço, marca, avaliações. | Fase 1 (MVP) | Prompt 07 | Unitário (Filter Engine), Integração (JPA/Query) | `VERIFIED (Prompt 07)` |
| **FR-MKT-004** | P0 | Visualizar detalhes e proveniência de indicadores. | Fase 1 (MVP) | Prompt 07 | Unitário (Provenance Metadata), E2E | `VERIFIED (Prompt 07)` |
| **FR-MKT-005** | P1 | Analisar categoria e subcategoria agregadas. | Fase 2 | Prompt 13 | Unitário (Aggregator), Integração | `PLANNED` |
| **FR-MKT-006** | P1 | Identificar tendências e sazonalidade históricas. | Fase 2 | Prompt 13 | Unitário (Time-Series Analysis) | `PLANNED` |
| **FR-MKT-007** | P1 | Calcular concentração competitiva (Herfindahl-Hirschman). | Fase 2 | Prompt 13 | Unitário determinístico com fórmula documentada | `PLANNED` |
| **FR-MKT-008** | P0 | Salvar pesquisas e listas de oportunidades. | Fase 1 (MVP) | Prompt 07 | Integração (JPA/Tenant), Unitário | `VERIFIED (Prompt 07)` |
| **FR-MKT-009** | P1 | Exportar resultados em CSV/XLSX. | Fase 1 (MVP) | Prompt 07 | Unitário (File Generator), Integração | `PLANNED` |

---

## 5. Requisitos Funcionais — Competitor Intelligence (Concorrentes)

| ID | Prioridade | Descrição do Requisito | Fase | Prompt Alvo | Tipo de Teste Planejado | Status Atual |
|---|---|---|---|---|---|---|
| **FR-CMP-001** | P0 | Cadastrar concorrente por perfil/loja ou anúncio. | Fase 1 (MVP) | Prompt 07 | Integração (JPA), Unitário | `PLANNED` |
| **FR-CMP-002** | P1 | Agrupar concorrentes em listas personalizadas. | Fase 2 | Prompt 13 | Unitário, Integração | `PLANNED` |
| **FR-CMP-003** | P1 | Monitorar preço e atributos observáveis. | Fase 2 | Prompt 13 | Integração (Worker de Snapshot), Unitário | `PLANNED` |
| **FR-CMP-004** | P1 | Detectar novos anúncios, remoções ou pausas observadas. | Fase 2 | Prompt 13 | Unitário (Diff Engine), Integração | `PLANNED` |
| **FR-CMP-005** | P1 | Comparar anúncio próprio com concorrentes. | Fase 2 | Prompt 13 | Unitário (Comparison DTO), E2E | `PLANNED` |
| **FR-CMP-006** | P1 | Exibir histórico temporal de preços e concorrência. | Fase 2 | Prompt 13 | Integração (Timeseries Query) | `PLANNED` |
| **FR-CMP-007** | P1 | Gerar alerta de mudança relevante em concorrente. | Fase 2 | Prompt 14 | Unitário (Rule Evaluator), Integração | `PLANNED` |
| **FR-CMP-008** | P1 | Identificar reputação e indicadores públicos de seller. | Fase 2 | Prompt 13 | Unitário (Metadata Validator) | `PLANNED` |

---

## 6. Requisitos Funcionais — Product Discovery e Fornecedores

| ID | Prioridade | Descrição do Requisito | Fase | Prompt Alvo | Tipo de Teste Planejado | Status Atual |
|---|---|---|---|---|---|---|
| **FR-DIS-001** | P1 | Criar ranking de oportunidades com critérios configuráveis. | Fase 2 | Prompt 15 | Unitário (Scoring Engine) | `PLANNED` |
| **FR-DIS-002** | P1 | Buscar produto semelhante por imagem. | Fase 2 | Prompt 15 | Integração (Visual Search Provider) | `PLANNED` |
| **FR-DIS-003** | P1 | Enviar PDF de catálogo de fornecedor. | Fase 2 | Prompt 15 | Integração (S3 Upload / Antivirus / Validation) | `PLANNED` |
| **FR-DIS-004** | P1 | Extrair itens, EAN, descrição, preço, peso e dimensões. | Fase 2 | Prompt 15 | Unitário (Extraction Parser), Prompt Injection Guard | `PLANNED` |
| **FR-DIS-005** | P1 | Comparar itens do catálogo com dados do mercado. | Fase 2 | Prompt 15 | Unitário (Similarity Matcher) | `PLANNED` |
| **FR-DIS-006** | P1 | Comparar fornecedores, custos e MOQ. | Fase 2 | Prompt 15 | Unitário, Integração | `PLANNED` |
| **FR-DIS-007** | P1 | Simular margem dos itens identificados no catálogo. | Fase 2 | Prompt 15 | Unitário (Financial Engine Integration) | `PLANNED` |
| **FR-DIS-008** | P1 | Exportar ranking e catálogo processado. | Fase 2 | Prompt 15 | Unitário (Exporter) | `PLANNED` |

---

## 7. Requisitos Funcionais — AI Listing Studio e Analytics

| ID | Prioridade | Descrição do Requisito | Fase | Prompt Alvo | Tipo de Teste Planejado | Status Atual |
|---|---|---|---|---|---|---|
| **FR-LST-001** | P0 | Analisar qualidade de anúncio próprio com score explicável. | Fase 1 (MVP) | Prompt 10 | Unitário (Audit Score Engine) | `PLANNED` |
| **FR-LST-002** | P0 | Gerar e otimizar títulos assistidos por IA. | Fase 1 (MVP) | Prompt 10 | Unitário (Prompt Versioning), Spring AI Mock | `PLANNED` |
| **FR-LST-003** | P0 | Gerar descrições sem inventar especificações técnicas. | Fase 1 (MVP) | Prompt 10 | Unitário (Factual Guardrail), Spring AI Mock | `PLANNED` |
| **FR-LST-004** | P1 | Sugerir categoria e atributos recomendados. | Fase 1 (MVP) | Prompt 10 | Unitário, Integração | `PLANNED` |
| **FR-LST-005** | P1 | Sugerir palavras-chave relevantes. | Fase 1 (MVP) | Prompt 10 | Unitário (Keyword Ranking) | `PLANNED` |
| **FR-LST-006** | P1 | Preparar e tratar imagens de produto. | Fase 2 | Prompt 15 | Integração (Image Processing Service) | `PLANNED` |
| **FR-LST-007** | P1 | Criar rascunho de anúncio a partir de referência. | Fase 2 | Prompt 15 | Unitário (Template Transformation) | `PLANNED` |
| **FR-LST-008** | P1 | Gerenciar versões e histórico de anúncios. | Fase 1 (MVP) | Prompt 10 | Integração (Audit Trail / JPA) | `PLANNED` |
| **FR-LST-009** | P1 | Publicar/atualizar anúncio por API com confirmação humana. | Fase 3 | Prompt 19 | Integração (Marketplace Write API), E2E | `PLANNED` |
| **FR-LST-010** | P1 | Executar testes comparativos/AB observacionais. | Fase 3 | Prompt 18 | Unitário (Comparative Analytics) | `PLANNED` |
| **FR-LST-011** | P0 | Distinguir recomendação de IA de métrica observada. | Fase 1 (MVP) | Prompt 10, Prompt 11 | Unitário (DTO Classifier), E2E | `PLANNED` |

---

## 8. Requisitos Funcionais — Seller Financial Control (Motor Financeiro)

| ID | Prioridade | Descrição do Requisito | Fase | Prompt Alvo | Tipo de Teste Planejado | Status Atual |
|---|---|---|---|---|---|---|
| **FR-FIN-001** | P0 | Cadastrar custo de aquisição e custos adicionais por SKU. | Fase 1 (MVP) | Prompt 08 | Unitário, Integração (Vigência Temporal) | `VERIFIED (Prompt 08)` |
| **FR-FIN-002** | P0 | Configurar componentes de tarifa, comissão, frete e tributos. | Fase 1 (MVP) | Prompt 08 | Unitário, Integração (Versioned Rules) | `VERIFIED (Prompt 08)` |
| **FR-FIN-003** | P0 | Calcular margem de contribuição por pedido e SKU. | Fase 1 (MVP) | Prompt 08 | Suíte Mínima de Regressão Financial (`FinancialCalculationEngineTest`) | `VERIFIED (Prompt 08)` |
| **FR-FIN-004** | P0 | Calcular preço mínimo e simular preço-alvo. | Fase 1 (MVP) | Prompt 08 | Unitário determinístico (`BigDecimal`) | `VERIFIED (Prompt 08)` |
| **FR-FIN-005** | P0 | Consolidar resultados por loja, marketplace e empresa. | Fase 1 (MVP) | Prompt 09 | Integração (Aggregation Queries) | `VERIFIED (Prompt 09)` |
| **FR-FIN-006** | P0 | Tratar descontos, cancelamentos, devoluções e estornos. | Fase 1 (MVP) | Prompt 08 | Unitário, Integração (`FinancialOrderCalculationIntegrationTest`) | `VERIFIED (Prompt 08)` |
| **FR-FIN-007** | P1 | Calcular margem após custo de Ads atribuído. | Fase 2 | Prompt 16 | Unitário (Attribution Allocation) | `PLANNED` |
| **FR-FIN-008** | P1 | Apresentar DRE gerencial configurável. | Fase 2 | Prompt 17 | Integração (Financial Reporting Engine) | `PLANNED` |
| **FR-FIN-009** | P1 | Exibir Curva ABC e rentabilidade por SKU. | Fase 2 | Prompt 17 | Unitário (Pareto Analysis Engine) | `PLANNED` |
| **FR-FIN-010** | P0 | Versionar regras de cálculo sem corromper fechados. | Fase 1 (MVP) | Prompt 08 | Unitário, Integração (`FinancialOrderCalculationIntegrationTest`) | `VERIFIED (Prompt 08)` |
| **FR-FIN-011** | P0 | Informar dados incompletos e qualidade do cálculo. | Fase 1 (MVP) | Prompt 08 | Unitário (`CalculationQuality.PARTIAL` / `CalculationMemory`) | `VERIFIED (Prompt 08)` |

---

## 9. Requisitos Funcionais — Ads Intelligence

| ID | Prioridade | Descrição do Requisito | Fase | Prompt Alvo | Tipo de Teste Planejado | Status Atual |
|---|---|---|---|---|---|---|
| **FR-ADS-001** | P1 | Importar campanhas e métricas autorizadas de Ads. | Fase 2 | Prompt 16 | Integração (Ads API Connector) | `PLANNED` |
| **FR-ADS-002** | P1 | Calcular ROAS, ACOS e TACOS com precisão. | Fase 2 | Prompt 16 | Unitário determinístico com validação de zero | `PLANNED` |
| **FR-ADS-003** | P1 | Exibir CTR, CPC, cliques e impressões sem falsear zero. | Fase 2 | Prompt 16 | Unitário (Data Provenance & Unavailable Checks) | `PLANNED` |
| **FR-ADS-004** | P1 | Analisar desempenho por campanha, grupo e SKU. | Fase 2 | Prompt 16 | Integração (Aggregation Queries) | `PLANNED` |
| **FR-ADS-005** | P1 | Cruzar publicidade com margem de contribuição. | Fase 2 | Prompt 16 | Unitário (Cross-Domain Financial Service) | `PLANNED` |
| **FR-ADS-006** | P1 | Recomendar otimizações assistidas por IA explicável. | Fase 2 | Prompt 16 | Unitário (Recommendation Engine) | `PLANNED` |
| **FR-ADS-007** | P1 | Alertar campanhas fora da meta de ROAS/ACOS. | Fase 2 | Prompt 16 | Unitário (Alert Trigger) | `PLANNED` |

---

## 10. Requisitos Funcionais — Product Monitoring (Monitoramento)

| ID | Prioridade | Descrição do Requisito | Fase | Prompt Alvo | Tipo de Teste Planejado | Status Atual |
|---|---|---|---|---|---|---|
| **FR-MON-001** | P1 | Criar listas de observação (watchlists) de produtos. | Fase 2 | Prompt 13 | Integração (JPA/Tenant) | `PLANNED` |
| **FR-MON-002** | P1 | Registrar histórico temporal de preços e atributos. | Fase 2 | Prompt 13 | Integração (Append-Only Snapshot Storage) | `PLANNED` |
| **FR-MON-003** | P1 | Acompanhar vendas estimadas apenas com metodologia explícita. | Fase 2 | Prompt 13 | Unitário (Estimative Labeling) | `PLANNED` |
| **FR-MON-004** | P1 | Configurar alertas por variação percentual ou nominal. | Fase 2 | Prompt 14 | Unitário (Rule Trigger Simulator) | `PLANNED` |
| **FR-MON-005** | P1 | Exibir gráficos temporais com marcação de lacunas. | Fase 2 | Prompt 13 | Frontend Component Test (Recharts) | `PLANNED` |

---

## 11. Requisitos Funcionais — Smart Alerts e Action Center

| ID | Prioridade | Descrição do Requisito | Fase | Prompt Alvo | Tipo de Teste Planejado | Status Atual |
|---|---|---|---|---|---|---|
| **FR-ACT-001** | P1 | Centralizar alertas por severidade e domínio. | Fase 2 | Prompt 14 | Integração (Alert Repository) | `PLANNED` |
| **FR-ACT-002** | P1 | Configurar regras personalizadas de alerta. | Fase 2 | Prompt 14 | Unitário (Rule Evaluation Engine) | `PLANNED` |
| **FR-ACT-003** | P1 | Converter recomendação ou alerta em tarefa operacional. | Fase 2 | Prompt 14 | Unitário, Integração (Task Service) | `PLANNED` |
| **FR-ACT-004** | P1 | Atribuir responsável, prazo, prioridade e status à tarefa. | Fase 2 | Prompt 14 | Integração (RBAC & Audit Trail) | `PLANNED` |
| **FR-ACT-005** | P1 | Acompanhar conclusão e resultado posterior da ação. | Fase 2 | Prompt 14 | Unitário, E2E | `PLANNED` |
| **FR-ACT-006** | P1 | Enviar notificações por e-mail e in-app com opt-in. | Fase 2 | Prompt 14 | Integração (Notification Service / Mock SMTP) | `PLANNED` |

---

## 12. Requisitos Funcionais — Executive Dashboard e Relatórios

| ID | Prioridade | Descrição do Requisito | Fase | Prompt Alvo | Tipo de Teste Planejado | Status Atual |
|---|---|---|---|---|---|---|
| **FR-DAS-001** | P0 | Exibir painel consolidado de vendas e rentabilidade. | Fase 1 (MVP) | Prompt 09 | Integração (BI Aggregation), E2E | `VERIFIED (Prompt 09)` |
| **FR-DAS-002** | P0 | Comparar períodos equivalentes de faturamento/margem. | Fase 1 (MVP) | Prompt 09 | Unitário (Period Shifter), E2E | `VERIFIED (Prompt 09)` |
| **FR-DAS-003** | P1 | Exibir ranking ordenado de produtos e canais. | Fase 1 (MVP) | Prompt 09 | Unitário (Sorting/Filtering) | `VERIFIED (Prompt 09)` |
| **FR-DAS-004** | P1 | Exibir alertas e tarefas prioritárias no painel. | Fase 2 | Prompt 14 | Integração (Action Center Query) | `PLANNED` |
| **FR-DAS-005** | P0 | Exportar relatórios gerenciais em formatos padrão. | Fase 1 (MVP) | Prompt 09 | Unitário (CSV/Spreadsheet Exporter) | `VERIFIED (Prompt 09)` |
| **FR-DAS-006** | P1 | Agendar envio automatizado de relatórios por e-mail. | Fase 2 | Prompt 17 | Integração (Scheduled Job Service) | `PLANNED` |

---

## 13. Requisitos Funcionais — Conciliação Financeira (Reconciliation)

| ID | Prioridade | Descrição do Requisito | Fase | Prompt Alvo | Tipo de Teste Planejado | Status Atual |
|---|---|---|---|---|---|---|
| **FR-REC-001** | P1 | Importar extratos e repasses oficiais do marketplace. | Fase 2 | Prompt 17 | Integração (Payout File Ingestor) | `PLANNED` |
| **FR-REC-002** | P1 | Relacionar pedidos, taxas e repasses bancários/virtuais. | Fase 2 | Prompt 17 | Unitário (Matching Engine) | `PLANNED` |
| **FR-REC-003** | P1 | Identificar e quantificar divergências financeiras. | Fase 2 | Prompt 17 | Unitário determinístico (`BigDecimal`) | `PLANNED` |
| **FR-REC-004** | P1 | Registrar protocolo de contestação e acompanhamento. | Fase 2 | Prompt 17 | Integração (Dispute Service) | `PLANNED` |
| **FR-REC-005** | P1 | Exibir relatório estruturado de conciliação financeira. | Fase 2 | Prompt 17 | Integração (Reporting Service) | `PLANNED` |

---

## 14. Requisitos Funcionais — AI Assistant e Servidor MCP

| ID | Prioridade | Descrição do Requisito | Fase | Prompt Alvo | Tipo de Teste Planejado | Status Atual |
|---|---|---|---|---|---|---|
| **FR-AI-001** | P1 | Disponibilizar assistente conversacional contextualizado. | Fase 3 | Prompt 18 | Integração (Spring AI / Chat Context) | `PLANNED` |
| **FR-AI-002** | P1 | Consultar catálogo, métricas e concorrentes via linguagem natural. | Fase 3 | Prompt 18 | Unitário (Natural Language to Query Tool) | `PLANNED` |
| **FR-AI-003** | P1 | Gerar análises e relatórios explicáveis com fontes citadas. | Fase 3 | Prompt 18 | Unitário (Response Factuality & Provenance) | `PLANNED` |
| **FR-AI-004** | P2 | Expor ferramentas autorizadas via Model Context Protocol (MCP). | Fase 3 | Prompt 18 | Integração (MCP Server Protocol Test) | `PLANNED` |
| **FR-AI-005** | P2 | Segregar ferramentas read-only de operações mutáveis. | Fase 3 | Prompt 18 | Segurança (MCP Mutability Barrier Test) | `PLANNED` |
| **FR-AI-006** | P1 | Proteger contra prompt injection em dados externos não confiáveis. | Fase 2/3 | Prompt 15, Prompt 18 | Teste de Segurança Adversarial (Injection Fuzzing) | `PLANNED` |
| **FR-AI-007** | P1 | Registrar telemetria de uso de IA (tokens, latência, avaliação). | Fase 1/3 | Prompt 10, Prompt 18 | Integração (AI Telemetry Listener) | `PLANNED` |

---

## 15. Requisitos Não Funcionais (NFRs)

| ID | Categoria | Descrição do Requisito | Prompt Alvo | Validação Planejada | Status Atual |
|---|---|---|---|---|---|
| **NFR-SEC-001** | Segurança | TLS em trânsito e criptografia AES-GCM em repouso para credenciais. | Prompt 02, Prompt 05 | Teste de Criptografia de Secrets | `PLANNED` |
| **NFR-SEC-002** | Segurança | Isolamento multi-tenant garantido no backend e banco via RLS. | Prompt 03, Prompt 12 | Testes Adversariais Cross-Tenant | `PLANNED` |
| **NFR-SEC-003** | Segurança | Segredos OAuth e tokens armazenados em cofre cifrado. | Prompt 05 | Code Review e Teste de Repositório | `PLANNED` |
| **NFR-SEC-004** | Segurança | Controle de acesso baseado em papéis (RBAC) granular. | Prompt 04 | Teste de Autorização de Endpoints | `PLANNED` |
| **NFR-SEC-005** | Segurança | Proteção OWASP Top 10 (CSRF, XSS, SSRF, SQL Injection). | Prompt 12 | Scanner de Vulnerabilidades e Testes E2E | `PLANNED` |
| **NFR-SEC-006** | Segurança | Suporte a MFA para contas administrativas. | Prompt 03 | Configuração OIDC Keycloak | `PLANNED` |
| **NFR-SEC-007** | Segurança | Rate limiting e proteção contra abuso e enumeração. | Prompt 03, Prompt 12 | Teste de Carga de Rate Limit | `PLANNED` |
| **NFR-PRV-001** | Privacidade | Minimização de dados pessoais e conformidade estrita com LGPD. | Prompt 01, Prompt 06 | Auditoria de Schemas e PII | `PLANNED` |
| **NFR-PRV-002** | Privacidade | Proibição de uso de dados do seller para treinar modelos globais. | Prompt 10, Prompt 18 | Verificação de Contrato de Provedor LLM | `PLANNED` |
| **NFR-PRV-003** | Privacidade | Sanitização estrita de logs: zero tokens, senhas ou PII. | Prompt 02, Prompt 12 | Validação de Log Formatter | `PLANNED` |
| **NFR-REL-001** | Confiabilidade | Jobs de sincronização idempotentes e recuperáveis. | Prompt 06 | Teste de Replay de Mensagens RabbitMQ | `PLANNED` |
| **NFR-REL-002** | Confiabilidade | Retries com backoff exponencial e jitter contra rate limits. | Prompt 05, Prompt 06 | Teste com WireMock Fault Injection | `PLANNED` |
| **NFR-REL-003** | Confiabilidade | Dead-letter queues (DLQ) para mensagens e eventos não processados. | Prompt 02, Prompt 06 | Teste de Roteamento de Erros no RabbitMQ | `PLANNED` |
| **NFR-REL-004** | Confiabilidade | Backups automatizados do PostgreSQL e teste de restauração. | Prompt 12 | Script de Disaster Recovery | `PLANNED` |
| **NFR-PER-001** | Performance | Latência p95 < 2,5 s para operações analíticas e de tela padrão. | Prompt 12 | Testes de Carga com k6 / Gatling | `PLANNED` |
| **NFR-PER-002** | Performance | Processamento assíncrono para relatórios e exportações massivas. | Prompt 06, Prompt 09 | Teste de Job Polling / SSE | `PLANNED` |
| **NFR-PER-003** | Performance | Paginação compulsória e filtros limitadores em consultas. | Prompt 06, Prompt 07 | Teste de API com Limites Máximos | `PLANNED` |
| **NFR-SCL-001** | Escalabilidade | Workers assíncronos desacoplados escaláveis independentemente. | Prompt 02, Prompt 06 | Arquitetura Modular em Threads/Containers | `PLANNED` |
| **NFR-OBS-001** | Observabilidade | Logs estruturados JSON, métricas Prometheus e tracing OTel. | Prompt 02, Prompt 12 | Teste de Ingestão de Telemetria | `PLANNED` |
| **NFR-OBS-002** | Observabilidade | Correlation ID (`trace_id`) propagado em HTTP, filas e jobs. | Prompt 02, Prompt 06 | Teste de Propagação de Cabeçalho | `VERIFIED (Prompt 02)` |
| **NFR-OBS-003** | Observabilidade | Dashboards e alertas para filas congestionadas e falhas de APIs. | Prompt 12 | Configuração de Regras de Alerta Prometheus | `PLANNED` |
| **NFR-UX-001** | Usabilidade | Interface acessível, responsiva e com estados explícitos de erro. | Prompt 09, Prompt 10 | Vitest, Testing Library e A11y Review | `PLANNED` |
| **NFR-UX-002** | Usabilidade | Glossário contextual e explicações acessíveis para indicadores. | Prompt 07, Prompt 09 | Inspeção de Tooltips e Documentação UI | `PLANNED` |
| **NFR-AI-001** | IA | Respostas fundamentadas com citação de fontes e limites claros. | Prompt 10, Prompt 18 | Teste de Avaliação de Assertividade | `PLANNED` |
| **NFR-AI-002** | IA | Defesa em profundidade contra dados externos não confiáveis. | Prompt 15, Prompt 18 | Teste de Injeção em Metadados | `PLANNED` |
| **NFR-MNT-001** | Manutenibilidade | Contratos padronizados de conectores e limites ArchUnit. | Prompt 02, Prompt 05 | Testes de Arquitetura no Maven Build | `VERIFIED (Prompt 02)` |
| **NFR-MNT-002** | Manutenibilidade | Regras de cálculo versionadas com cobertura determinística. | Prompt 08 | Suíte de Regressão Financeira JUnit 5 | `PLANNED` |
