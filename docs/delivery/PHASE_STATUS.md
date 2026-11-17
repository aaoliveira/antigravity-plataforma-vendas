# Status de Entregas e Phase Gates — Seller Intelligence Platform

## 1. Visão Geral do Gate Atual

- **Fase Atual:** Fase 2 — Expansão de Inteligência & Monitoramento
- **Prompt Concluído:** `Prompt 14 — Smart Alerts e Action Center`
- **Status do Gate:** `APPROVED`
- **Data de Conclusão:** 2026-11-17T16:35:00-03:00
- **Responsável:** Antigravity AI Assistant & Engenharia de Dados
- **Próximo Prompt Liberado:** `Prompt 15 — Product Discovery avançado: PDFs e busca por imagem`

---

## 2. Verificação Objetiva dos Critérios de Aceite (Prompt 14 — Smart Alerts e Action Center)

| Critério de Aceite | Requisito Verificado | Evidência Comprovada | Avaliação |
|---|---|---|---|
| **AC-14-1: Catálogo de Regras Parametrizáveis** | Catálogo com 5 tipos de regras (margem, preço, sync, vendas, estoque), escopos e limiares com precisão decimal. | Entidades `ActionAlertRule` e `AlertRuleType` com operadores `LESS_THAN`, `GREATER_THAN`, `EQUALS` e escopos multi-tenant. Testado em `ActionAlertRuleServiceTest`. | `APROVADO` |
| **AC-14-2: Motor de Simulação com Eventos Sintéticos** | Avaliação e teste de regras com valores simulados sem poluição de dados privados. | Método `simulateRule` em `ActionAlertRuleService` retornando `RuleSimulationResult` com justificativa explicativa e opção de persistir alerta gerado. | `APROVADO` |
| **AC-14-3: Deduplicação com Janela de Cooldown** | Deduplicação determinística de alertas repetidos evitando fadiga de alertas. | `ActionAlertService` com cálculo determinístico de `dedupKey` SHA-256 e constraint única `uk_action_alert_dedup` no PostgreSQL. Testado em `ActionAlertServiceTest`. | `APROVADO` |
| **AC-14-4: Inbox Unificado de Alertas** | Centralização com estados (`NEW`, `READ`, `RESOLVED`, `IGNORED`), severidades e proveniência rastreável. | `ActionAlert` com proveniência (`CALCULATED`, `OBSERVED`, `SYSTEM`), payload JSON de contexto e endpoints de transição de status em `ActionCenterController`. | `APROVADO` |
| **AC-14-5: Conversão em Tarefa com Link Rastreável** | Conversão de alerta em tarefa operacional com herança de evidência e vínculo bidirecional. | `ActionTaskService.createTaskFromAlert` preservando link `alert_id`, transicionando alerta para `READ` e herdando contexto. Testado em `ActionTaskServiceTest`. | `APROVADO` |
| **AC-14-6: Proteção Anti-IDOR de Atribuição** | Proibição estrita de atribuição de tarefas a usuários externos à organização tenant. | Validação `validateAssignee` contra `MembershipRepository` rejeitando usuários fora do tenant com `IllegalArgumentException`. Testado em `ActionTaskServiceTest`. | `APROVADO` |
| **AC-14-7: Governança de Opt-In & Controle de Taxa** | Notificações externas exigem consentimento prévio e respeitam frequência máxima por hora. | `ActionNotificationService` gravando `SUPPRESSED_NO_OPT_IN` para canais sem consentimento e `SUPPRESSED_COOLDOWN` ao atingir limite. Testado em `ActionNotificationServiceTest`. | `APROVADO` |
| **AC-14-8: Trilha de Auditoria Imutável** | Rastreabilidade do ciclo de vida das tarefas e histórico de disparos de notificação. | Tabelas `action_task_audits` e `action_notification_deliveries` gravando cronologia imutável com ator, transições e status de entrega. | `APROVADO` |
| **AC-14-9: Invariante de Segurança (Zero Auto-Mutação)** | Tarefas são exclusivamente de controle interno, sem disparos automáticos de mutação externa em marketplaces. | `ActionCenterController` sem referências ou invocações de escrita para conectores externos de marketplaces. | `APROVADO` |
| **AC-14-10: Banco V10, ADR DEC-020 & Testes Integrados** | Migration V10, ADR DEC-020 e suítes completas de testes automatizados. | Migration V10 aplicada no PostgreSQL, ADR DEC-020 aceito, 15 novos testes backend (161 totais), 6 novos testes frontend (44 totais), 13 extensão e 5 E2E. | `APROVADO` |

---

## 2. Verificação Objetiva dos Critérios de Aceite (Prompt 13 — Monitoramento Contínuo)

| Critério de Aceite | Requisito Verificado | Evidência Comprovada | Avaliação |
|---|---|---|---|
| **AC-13-1: Watchlists Multi-Tenant** | Watchlists de produtos, anúncios e vendedores por tenant com adição/remoção lógica e preservação estrita de histórico temporal. | Entidades `Watchlist` e `WatchlistItem` isoladas por `tenantId`. Remoção lógica via `status = UNTRACKED` com integridade referencial `ON DELETE RESTRICT` garantindo preservação integral do histórico de snapshots. 4 testes em `MonitoringWatchlistServiceTest`. | `APROVADO` |
| **AC-13-2: Snapshots & Proveniência** | Snapshots periódicos coletados via conectores/fontes autorizadas com timestamp, status e proveniência `OBSERVED` explícita. | `MonitoringSnapshot` com `BigDecimal NUMERIC(15, 2)`, proveniência fixa `OBSERVED` e suporte a `CollectionStatus` (`SUCCESS`, `COLLECTION_FAILED`, `GAP`). Falhas de coleta são registradas com status e mensagem sem quebrar o pipeline. | `APROVADO` |
| **AC-13-3: Diff Determinístico & Deduplicação** | Detecção determinística de mudanças (preço em R$ e %, status de anúncio) gerando eventos imutáveis com hash de deduplicação. | `MonitoringDiffEngine` calculando variações com precisão e arredondamento `HALF_EVEN`, detectando transições (`OUT_OF_STOCK`, `REACTIVATED`) e gerando hash SHA-256 com constraint única no PostgreSQL (`uk_monitoring_change_event_hash`). 3 testes em `MonitoringDiffEngineTest`. | `APROVADO` |
| **AC-13-4: Séries Temporais & Zero Interpolação** | Série temporal histórica com representação explícita de lacunas de coleta (`GAP` / `COLLECTION_FAILED`) sem interpolação silenciosa. | `MonitoringTimelineService` detecta intervalos $> 1.5 \times \text{frequência}$ e injeta pontos explícitos com `CollectionStatus.GAP` e `price = null` (estritamente sem alucinação de dados). Cálculo de taxa de cobertura de coleta implementado. 2 testes em `MonitoringTimelineServiceTest`. | `APROVADO` |
| **AC-13-5: Regras de Alerta & Cooldown** | Configuração de limiares de alerta e geração de notificações deduplicadas por janela temporal (cooldown). | `MonitoringAlertService` com regras de variação percentual de preço e ruptura de estoque. Deduplicação determinística no PostgreSQL via `uk_monitoring_alerts_dedup` e chave por janela de cooldown. 2 testes em `MonitoringAlertServiceTest`. | `APROVADO` |
| **AC-13-6: Comparador Próprio vs Concorrente** | Comparador lado a lado de anúncio próprio vs concorrente monitorado com identificação de campos ausentes. | `CompetitorComparisonService` confrontando listagem própria contra concorrente com cálculo de delta financeiro em R$ e %, e disclaimers obrigatórios de `UNAVAILABLE` para estoque real e volume de vendas de terceiros. 1 teste em `CompetitorComparisonServiceTest`. | `APROVADO` |
| **AC-13-7: ADR DEC-019 & Retenção de 12 Meses** | ADR DEC-019 formalizando retenção de snapshots por 12 meses, particionamento e índices compostos no PostgreSQL. | Registrado formalmente **ADR DEC-019** em `docs/architecture/DECISION_LOG.md`. Migration Flyway V9 (`V9__competitor_and_product_monitoring.sql`) cria 6 tabelas, índices de série temporal e constraints de deduplicação. | `APROVADO` |
| **AC-13-8: Suíte de Testes Automatizados** | Suítes unitárias, integração com PostgreSQL real (Testcontainers) e Vitest frontend. | 14 novos testes de backend (100% aprovados, totalizando 146 no backend), 5 novos testes Vitest em `CompetitorMonitoringView.test.tsx` (totalizando 38 no frontend), 13 na extensão e 5 jornadas E2E Playwright. | `APROVADO` |
| **AC-13-9: Rastreabilidade de Requisitos** | Atualização de rastreabilidade (`FR-CMP-002` a `008`, `FR-MON-001` a `005`) e conformidade de preflight. | Matriz atualizada em `REQUIREMENTS_TRACEABILITY.md` com status `VERIFIED (Prompt 13)`. Script `scripts/verify_phase13.py` validado com 100% PASS em todos os 9 critérios. | `APROVADO` |

---

## 2. Verificação Objetiva dos Critérios de Aceite (Prompt 12 — Homologação e Release)

| Critério de Aceite | Requisito Verificado | Evidência Comprovada | Avaliação |
|---|---|---|---|
| **AC-12-1: Suíte E2E Playwright** | Cobertura das jornadas críticas de ponta a ponta do MVP (Auth, Organizações, Lojas, Conectores, Sincronização, Mercado, Financeiro, AI Studio). | Suíte em `e2e/tests/mvp-critical-journeys.spec.ts` executada com Chromium headless: 5/5 jornadas aprovadas (100%) em 6.0 s. | `APROVADO` |
| **AC-12-2: Isolamento Multi-Tenant & RBAC** | Testes adversariais de intrusão cross-tenant (anti-IDOR) e enforcement de matriz de papéis (VIEWER e OPERATOR). | 10 testes em `AdversarialSecurityIntegrationTest.java` bloqueando violações de tenant em empresas, lojas, jobs de sync, catálogo e finanças, além de 403 Forbidden em mutações por VIEWER. | `APROVADO` |
| **AC-12-3: OWASP Top 10 & Rate Limiting** | Proteção contra SSRF, rate limiting por IP/Tenant, headers de segurança HTTP (CSP, nosniff, frame-ancestors) e sanitização. | Implementado `UrlSecurityValidator` (anti-SSRF para metadados de nuvem e RFC-1918), `RateLimitFilter` (RFC-7807 429) e 7 testes em `OwaspSecurityIntegrationTest.java`. | `APROVADO` |
| **AC-12-4: Performance Benchmarks (p95 < 2,5 s)** | Testes de latência p95 sob carga contínua para endpoints críticos em ambiente controlado. | `BenchmarkPerformanceTest.java` (100 iterações): pesquisa de mercado com p95 = 184 ms (< 2.500 ms SLA), simulação financeira < 10 ms, health check < 5 ms. | `APROVADO` |
| **AC-12-5: Observabilidade & Tracing** | Propagação de correlation ID, trace ID contextual, RFC-7807 ProblemDetail e redação de segredos. | `ObservabilityIntegrationTest.java` (3 testes) validando `X-Correlation-Id`, trace_id em erros e resposta sanitizada em `/actuator/health`. | `APROVADO` |
| **AC-12-6: Resiliência Assíncrona & DLQ** | Degradação graciosa em conectores, retries exponenciais, DLQ do RabbitMQ e idempotência por composite key. | `SyncResilienceIntegrationTest.java` (3 testes) confirmando desvio para `sync.dlq`, falha elegante sem quebrar aplicação e idempotência estrita de execução. | `APROVADO` |
| **AC-12-7: Simulação de DR (PostgreSQL)** | Exercício automatizado de Backup & Restore com medição determinística de RPO e RTO. | `scripts/backup_restore_drill.sh` executado contra `seller_postgres`: 30/30 tabelas restauradas com paridade perfeita, RPO = 0 (zero perda) e RTO = 0.542 segundos. | `APROVADO` |
| **AC-12-8: Auditoria de Vulnerabilidades & SBOM** | Auditoria de dependências, SBOM consolidado e varredura de secrets em todo o código-fonte. | Gerado `docs/security/SBOM.md`, `scripts/scan_secrets.py` (379 arquivos inspecionados, 0 segredos detectados) e `pnpm audit` com 0 vulnerabilidades. | `APROVADO` |
| **AC-12-9: Relatório Formal & GO Release** | Relatório consolidado com limites conhecidos, escopo futuro e decisão formal de GO/NO-GO para o MVP. | Elaborado `docs/delivery/MVP_HOMOLOGATION_REPORT.md` declarando **GO PARA LANÇAMENTO DO MVP (Fase 1 / RC 1.0.0)** e consolidando 183 testes automatizados (100% sucesso). | `APROVADO` |

---

## 3. Registro Histórico de Gates por Prompt

| Prompt | Título | Fase | Status | Data de Aprovação | Evidências / Notas |
|---|---|---|---|---|---|
| **00** | Constituição do projeto, contexto e proteção contra alucinações | Fase 0 | `APPROVED` | 2026-11-17 | Constituição das regras, PRD integral, rastreabilidade, OQs e ADRs baseline. |
| **01** | Descoberta técnica, viabilidade e ADRs | Fase 0 | `APPROVED` | 2026-11-17 | Matriz de capacidades ML/Shopee, ADRs DEC-010 a DEC-013, auditoria OQ P0/P1/P2 e script de validação. |
| **02** | Bootstrap do monorepo e ambiente local | Fase 1 | `APPROVED` | 2026-11-17 | Monorepo criado, Spring Boot 3.5, Next.js 15, Docker Compose saudável, Flyway V1, CI workflow e testes passando. |
| **03** | Identidade, autenticação e isolamento multi-tenant | Fase 1 | `APPROVED` | 2026-11-17 | Spring Security JWT, User sem senha, Organization, Membership RBAC, TenantContextFilter, auditoria e 20 testes passando. |
| **04** | Organizações, CNPJs, lojas e gestão de acessos | Fase 1 | `APPROVED` | 2026-11-17 | CRUD empresas/CNPJs sem consulta RFB, lojas por marketplace, convites com escopo granular, UI Next.js, Flyway V3 e 45 testes backend + 10 frontend. |
| **05** | Framework de conectores e autorização OAuth | Fase 1 | `APPROVED` | 2026-11-17 | Port MarketplaceConnector, registry, OAuth oficial ML/Shopee, AES-256-GCM para tokens, Flyway V4, UI Next.js e 57 testes backend + 13 frontend. |
| **06** | Sincronização de catálogo, anúncios e pedidos | Fase 1 | `APPROVED` | 2026-11-17 | Modelos normalizados, idempotência estrita, jobs assíncronos RabbitMQ, webhooks HMAC, contingência CSV, Flyway V5, UI SyncManager e 71 testes backend + 16 frontend. |
| **07** | Market Intelligence: pesquisa e análise | Fase 1 | `APPROVED` | 2026-11-17 | Pesquisa pública de mercado via API autorizada, snapshots, proveniência explicável, buscas e produtos salvos, isolamento multi-tenant e 77 testes backend + 20 frontend. |
| **08** | Motor financeiro determinístico e versionado | Fase 1 | `APPROVED` | 2026-11-17 | Endereçamento OQ-004 e OQ-005 no ADR DEC-015, custos por SKU, regras versionadas, cálculo em BigDecimal, memória de cálculo, UI de simulação e 88 testes backend + 24 frontend. |
| **09** | Painel financeiro, rentabilidade e relatórios | Fase 1 | `APPROVED` | 2026-11-17 | Endereçamento OQ-015 no ADR DEC-016, agregação de faturamento e margem, comparação de períodos, exportação CSV RFC-4180 com metadados de proveniência, RBAC estrito e 93 testes backend + 28 frontend. |
| **10** | AI Listing Studio: análise e geração de conteúdo | Fase 1 | `APPROVED` | 2026-11-17 | Endereçamento OQ-010, OQ-013 e OQ-014 no ADR DEC-017, AiProvider agnóstico, score auditável (4 pilares), guardrail anti-alucinação, defesa contra prompt injection, rascunhos imutáveis e 106 testes backend + 33 frontend. |
| **11** | Extensão Chrome Manifest V3 complementar | Fase 1 | `APPROVED` | 2026-11-17 | Endereçamento OQ-003 no ADR DEC-018, Manifest V3 em TypeScript/React/Vite, permissões mínimas (activeTab e storage), zero scraping hostil, parser canônico ML/Shopee e 13 testes automatizados. |
| **12** | Homologação, segurança E2E e release do MVP | Fase 1 | `APPROVED` | 2026-11-17 | Fechamento da Fase 1 (MVP). Playwright E2E (5/5), testes adversariais cross-tenant, OWASP Top 10, anti-SSRF, rate limiting, DR drill (RTO 0.54s, RPO 0), p95 184ms, SBOM e GO para release (183 testes). |
| **13** | Monitoramento contínuo de produtos e concorrentes | Fase 2 | `APPROVED` | 2026-11-17 | Watchlists de concorrentes, histórico temporal sem interpolação de lacunas, detecção determinística de mudanças com hash SHA-256, alertas com cooldown deduplicado e ADR DEC-019. |
| **14** | Smart Alerts e Action Center | Fase 2 | `APPROVED` | 2026-11-17 | Catálogo de regras de alerta, simulação com eventos sintéticos, deduplicação com cooldown, inbox unificado, conversão em tarefas operacionais, proteção anti-IDOR e opt-in mandatório. |
| **15** | Product Discovery avançado: PDFs e busca por imagem | Fase 2 | `NOT_STARTED` | — | Leitura de catálogos de fornecedores. |
| **16** | Ads Intelligence e rentabilidade de campanhas | Fase 2 | `NOT_STARTED` | — | Gestão de anúncios patrocinados. |
| **17** | DRE gerencial e conciliação de repasses | Fase 2 | `NOT_STARTED` | — | Fechamento da Fase 2. |
| **18** | Assistente conversacional e servidor MCP read-only | Fase 3 | `NOT_STARTED` | — | IA conversacional e MCP. |
| **19** | Expansão de conectores para Amazon e outros | Fase 3 | `NOT_STARTED` | — | Fechamento da Fase 3. |

---

## 4. Conclusão do Prompt 14 e Liberação para o Prompt 15
O **Prompt 14 — Smart Alerts e Action Center** foi concluído e aprovado com 100% de conformidade.
Todos os critérios de aceite (AC-14-1 a AC-14-10) foram verificados objetivamente com testes de backend (161 testes), frontend (44 testes), extensão (13 testes) e E2E Playwright (5 testes), totalizando 223 testes automatizados aprovados no monorepo.
A plataforma está oficialmente liberada para o **Prompt 15 — Product Discovery avançado: PDFs e busca por imagem (Fase 2)**.
