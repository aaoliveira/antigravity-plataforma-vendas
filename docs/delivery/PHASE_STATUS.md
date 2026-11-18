# Status de Entregas e Phase Gates (Phase Status)

> **Governança Sequencial de Release** &middot; **Versão:** 1.0.0  
> **Status Vigente:** PROMPT P007-BE = `APPROVED` | P007-FE = `READY`  
> **Data:** 18/11/2026

---

## 1. Tabela Oficial de Phase Gates

| Prompt ID   | Título da Etapa                                                                          | Status do Gate | Data da Avaliação | Evidência Formal                                                                                                                                                                                                                   | Próximo Prompt Liberado?   |
| ----------- | ---------------------------------------------------------------------------------------- | -------------- | ----------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------- |
| **P000**    | Master Project Governance & Engineering Rules                                            | **`APPROVED`** | 17/11/2026        | [docs/PROMPT_EXECUTION_LOG.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/PROMPT_EXECUTION_LOG.md)                                                                                                           | **SIM (P001 liberado)**    |
| **P001-BE** | Estrutura Base do Repositório e Backend Modular                                          | **`APPROVED`** | 17/11/2026        | [docs/PROMPT_EXECUTION_LOG.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/PROMPT_EXECUTION_LOG.md)                                                                                                           | **SIM (P001-FE liberado)** |
| **P001-FE** | Shell da Aplicação Web Responsiva                                                        | **`APPROVED`** | 17/11/2026        | [docs/PROMPT_EXECUTION_LOG.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/PROMPT_EXECUTION_LOG.md)                                                                                                           | **SIM (P002-BE liberado)** |
| **P002-BE** | Identidade Mínima e Contexto do Tenant                                                   | **`APPROVED`** | 18/11/2026        | [docs/PROMPT_EXECUTION_LOG.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/PROMPT_EXECUTION_LOG.md)                                                                                                           | **SIM (P002-FE liberado)** |
| **P002-FE** | Interface de Autenticação e Troca de Contexto                                            | **`APPROVED`** | 18/11/2026        | [docs/PROMPT_EXECUTION_LOG.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/PROMPT_EXECUTION_LOG.md)                                                                                                           | **SIM (P003-BE liberado)** |
| **P003-BE** | Isolamento Multi-Tenant em Profundidade (RLS, Transacional e Testcontainers)             | **`APPROVED`** | 18/11/2026        | [PostgresMultiTenantRlsIntegrationTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/tenancy/PostgresMultiTenantRlsIntegrationTest.java)                  | **SIM (P003-FE liberado)** |
| **P003-FE** | Tratamento Padronizado de Erros (401/403/404) e Segurança de Tenant na UI                | **`APPROVED`** | 18/11/2026        | [ErrorHandlingUniformity.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/ErrorHandlingUniformity.test.tsx)                                                                           | **SIM (P004-BE liberado)** |
| **P004-BE** | Contrato MarketplaceAdapter, Registro de Capacidades, Evidências e Camada de Resiliência | **`APPROVED`** | 18/11/2026        | [MarketplaceAdapterResilienceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/marketplaceadapters/MarketplaceAdapterResilienceTest.java)                | **SIM (P004-FE liberado)** |
| **P004-FE** | Interface de Gestão de Conexões Oficiais, Capacidades e Evidências                       | **`APPROVED`** | 18/11/2026        | [MarketplaceIntegrationsView.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/MarketplaceIntegrationsView.test.tsx)                                                                   | **SIM (P005-BE liberado)** |
| **P005-BE** | Integração Oficial OAuth 2.0 Mercado Livre, PKCE, Cifragem KMS e Rotação Segura          | **`APPROVED`** | 18/11/2026        | [MercadoLivreOAuthIntegrationTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/marketplaceconnection/mercadolivre/MercadoLivreOAuthIntegrationTest.java) | **SIM (P005-FE liberado)** |
| **P005-FE** | Fluxo Oficial de Conexão com Mercado Livre (OAuth 2.0 PKCE UI)                           | **`APPROVED`** | 18/11/2026        | [MeliConnectFlow.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/MeliConnectFlow.test.tsx)                                                                                           | **SIM (P006-BE liberado)** |
| **P006-BE** | Integração Shopee Open Platform v2, Assinatura HMAC, Webhooks e Políticas | **`APPROVED`** | 18/11/2026 | [ShopeeOpenPlatformIntegrationTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/marketplaceconnection/shopee/ShopeeOpenPlatformIntegrationTest.java) | **SIM (P006-FE liberado)** |
| **P006-FE** | Fluxo Oficial de Conexão com Shopee (Open Platform v2 UI)                                | **`APPROVED`** | 18/11/2026        | [ShopeeConnectFlow.test.tsx](file:///home/alexandre/Projetos/antigravity-avantepro-clone/frontend/src/components/ShopeeConnectFlow.test.tsx)                                                                                         | **SIM (P007-BE liberado)** |
| **P007-BE** | Catálogo Canônico, Anúncios, Mapeamentos Externos e Sincronização Resiliente              | **`APPROVED`** | 18/11/2026        | [CatalogListingSyncServiceTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/catalog/CatalogListingSyncServiceTest.java), [PostgresCatalogRlsIntegrationTest.java](file:///home/alexandre/Projetos/antigravity-avantepro-clone/backend/src/test/java/com/sellerintelligence/modules/catalog/PostgresCatalogRlsIntegrationTest.java) | **SIM (P007-FE liberado)** |
| **P008-BE** | Analytics de Anúncios Próprios e Métricas Oficiais                                       | `NOT_STARTED`  | ---               | Pré-requisito: P004                                                                                                                                                                                                                | Não                        |
| **P009-BE** | Consolidação Financeira, DRE e Reconciliação                                             | `NOT_STARTED`  | ---               | Pré-requisito: P007                                                                                                                                                                                                                | Não                        |
| **P010-BE** | IA Assistiva com Tool Calling Somente Leitura                                            | `NOT_STARTED`  | ---               | Pré-requisitos: P007, P008                                                                                                                                                                                                         | Não                        |
| **P011-BE** | Exportação Segura de Relatórios e Auditoria                                              | `NOT_STARTED`  | ---               | Pré-requisito: P009                                                                                                                                                                                                                | Não                        |
| **P012**    | Hardening de Segurança, Testes E2E e Release MVP                                         | `NOT_STARTED`  | ---               | Pré-requisitos: P001 a P011                                                                                                                                                                                                        | Não                        |

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
