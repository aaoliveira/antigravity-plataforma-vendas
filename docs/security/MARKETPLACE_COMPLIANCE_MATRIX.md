# Matriz de Conformidade por Marketplace (Compliance Matrix)

**Plataforma:** Seller Intelligence Platform  
**Data da Verificação Documental:** 17 de Novembro de 2026  
**Status do Gate:** `APPROVED`  
**Política de Segurança Mandatória:** `FAIL CLOSED`  

---

## 1. Visão Geral e Legenda de Classificação

Todos os recursos de integração, sincronização, inteligência de mercado e automação comercial foram auditados individualmente contra as políticas oficiais dos marketplaces previstos.

### Legenda de Status de Conformidade
- `COMPLIANT`: Recurso homologado e executado estritamente através de endpoints e fluxos oficiais da API autorizada, dentro dos limites e políticas documentados.
- `PARTIALLY_COMPLIANT`: Funcionalidade cuja camada básica (leitura de catálogo/pedidos) é suportada e oficial, mas recursos adicionais (ex.: campanhas patrocinadas ou buscas agregadas) exigem whitelisting especial ou autorização suplementar.
- `UNVERIFIED_COMPLIANCE`: Marketplace ou recurso que não possui programa formal de integração concluído, credenciais oficiais de produção ou cuja conformidade não pôde ser atestada com evidência documental. **Categoricamente bloqueado para produção sob a política FAIL CLOSED.**
- `NON_COMPLIANT`: Prática proibida por termos de serviço (ex.: scraping agressivo, contorno de CAPTCHA, rotação de proxy para evasão, auto-compras). **Estritamente vedado na plataforma.**
- `NOT_APPLICABLE`: Recurso não suportado ou não aplicável ao modelo do canal.

---

## 2. Matriz Detalhada por Marketplace

### 2.1. Mercado Livre (MELI)

| Campo | Detalhe |
|---|---|
| **Marketplace** | Mercado Livre |
| **Funcionalidade 1** | Autenticação e Renovação OAuth 2.0 (PKCE & Authorization Code) |
| **Método de integração** | API REST Oficial / OAuth 2.0 |
| **Endpoint** | `https://auth.mercadolivre.com.br/authorization`, `https://api.mercadolibre.com/oauth/token` |
| **Documentação oficial** | https://developers.mercadolivre.com.br/pt_br/autenticacao-e-autorizacao |
| **Política aplicável** | Termos e Condições para Desenvolvedores Mercado Livre (Cláusula de Segurança e OAuth) |
| **Permissão necessária** | `offline_access`, `read`, `write` |
| **Limite operacional** | Rate limit baseado em janelas deslizantes (headers `X-Rate-Limit-Remaining`) |
| **Status de conformidade** | `COMPLIANT` |
| **Risco** | `LOW` |
| **Ação necessária** | Utilizar `TokenCryptoService` (AES-256-GCM) para tokens em repouso e `MarketplaceComplianceGuard` para expiração. |
| **Evidência** | `MercadoLivreConnector.java`, `PostgresMarketplaceCredentialsIntegrationTest.java` |
| **Data da verificação** | 17/11/2026 |

| Campo | Detalhe |
|---|---|
| **Marketplace** | Mercado Livre |
| **Funcionalidade 2** | Sincronização de Catálogo e Anúncios Próprios |
| **Método de integração** | API REST Oficial |
| **Endpoint** | `GET /users/{user_id}/items/search`, `GET /items?ids={item_ids}` |
| **Documentação oficial** | https://developers.mercadolivre.com.br/pt_br/publicacao-de-produtos |
| **Política aplicável** | Diretrizes de Listagem e Qualidade de Catálogo MELI |
| **Permissão necessária** | Escopo `read` autenticado com access token do seller |
| **Limite operacional** | Paginação com offset máximo de 1000 itens; batch de até 20 itens por chamada multi-get |
| **Status de conformidade** | `COMPLIANT` |
| **Risco** | `LOW` |
| **Ação necessária** | Controle de cursor e idempotência via `SyncJobService`. |
| **Evidência** | `MercadoLivreConnector.java#fetchListings`, `SyncJobServiceTest.java` |
| **Data da verificação** | 17/11/2026 |

| Campo | Detalhe |
|---|---|
| **Marketplace** | Mercado Livre |
| **Funcionalidade 3** | Sincronização de Pedidos e Vendas |
| **Método de integração** | API REST Oficial |
| **Endpoint** | `GET /orders/search?seller={user_id}` |
| **Documentação oficial** | https://developers.mercadolivre.com.br/pt_br/gestao-de-vendas |
| **Política aplicável** | Política de Proteção de Dados de Compradores e Gestão Comercial |
| **Permissão necessária** | Escopo `read` para pedidos próprios |
| **Limite operacional** | Máximo 50 pedidos por página |
| **Status de conformidade** | `COMPLIANT` |
| **Risco** | `LOW` |
| **Ação necessária** | Armazenar apenas identificadores, status e valores para DRE. Não reter PII desnecessário. |
| **Evidência** | `MercadoLivreConnector.java#fetchOrders`, `ReconciliationControllerIntegrationTest.java` |
| **Data da verificação** | 17/11/2026 |

| Campo | Detalhe |
|---|---|
| **Marketplace** | Mercado Livre |
| **Funcionalidade 4** | Webhooks e Notificações de Eventos |
| **Método de integração** | Push Webhook HTTPS |
| **Endpoint** | Callback `/api/v1/sync/webhooks/mercadolivre` |
| **Documentação oficial** | https://developers.mercadolivre.com.br/pt_br/notificacoes |
| **Política aplicável** | Validação de HMAC e resposta HTTP 200 em até 2000ms |
| **Permissão necessária** | Registro de URL de callback na aplicação no Developer Portal |
| **Limite operacional** | Reenvio com backoff pela MELI em caso de falha |
| **Status de conformidade** | `COMPLIANT` |
| **Risco** | `LOW` |
| **Ação necessária** | Processamento assíncrono idempotente via fila RabbitMQ e validação de `X-Signature`. |
| **Evidência** | `WebhookController.java`, `WebhookSecurityAndReplayTest.java` |
| **Data da verificação** | 17/11/2026 |

| Campo | Detalhe |
|---|---|
| **Marketplace** | Mercado Livre |
| **Funcionalidade 5** | Pesquisa Pública de Tendências e Mercado |
| **Método de integração** | API REST Pública de Busca |
| **Endpoint** | `GET /sites/MLB/search?q={query}` |
| **Documentação oficial** | https://developers.mercadolivre.com.br/pt_br/itens-e-buscas |
| **Política aplicável** | Uso aceitável da API pública de busca sem contorno de rate limits |
| **Permissão necessária** | Chave pública ou token autenticado da plataforma |
| **Limite operacional** | Limite padrão por IP/App; 50 itens por página |
| **Status de conformidade** | `COMPLIANT` |
| **Risco** | `LOW` |
| **Ação necessária** | Rótulo explícito `OBSERVED`, proveniência de dados e rate limit compliance. |
| **Evidência** | `MercadoLivreMarketSearchProvider.java`, `MarketIntelligenceServiceTest.java` |
| **Data da verificação** | 17/11/2026 |

| Campo | Detalhe |
|---|---|
| **Marketplace** | Mercado Livre |
| **Funcionalidade 6** | Publicação e Alteração Crítica de Preços / Estoque |
| **Método de integração** | API REST Oficial Mutável |
| **Endpoint** | `POST /items`, `PUT /items/{item_id}` |
| **Documentação oficial** | https://developers.mercadolivre.com.br/pt_br/modificacao-de-produtos |
| **Política aplicável** | Política de Preços e Proibição de Automação Abusiva |
| **Permissão necessária** | Escopo `write` + **Aprovação Humana Obrigatória** |
| **Limite operacional** | Operações pontuais auditadas |
| **Status de conformidade** | `COMPLIANT` (sob Human-in-the-Loop) |
| **Risco** | `HIGH` |
| **Ação necessária** | Bloqueado por default no `MarketplaceComplianceGuard` sem `humanApproved == true`. |
| **Evidência** | `MarketplaceComplianceGuardTest.java#scenario12_mutableCriticalOperationsRequireHumanApproval` |
| **Data da verificação** | 17/11/2026 |

---

### 2.2. Shopee

| Campo | Detalhe |
|---|---|
| **Marketplace** | Shopee |
| **Funcionalidade 1** | Autenticação e Assinatura SHA256 HMAC (Open Platform v2) |
| **Método de integração** | API REST Oficial / HMAC-SHA256 |
| **Endpoint** | `https://partner.shopeemobile.com/api/v2/shop/auth_partner`, `/api/v2/auth/token/get` |
| **Documentação oficial** | https://open.shopee.com/documents/v2/v2.auth.auth_partner |
| **Política aplicável** | Shopee Open Platform Developer Service Agreement (Cláusula 4: Segurança de Chaves) |
| **Permissão necessária** | `partner_id`, `partner_key`, autorização via Seller Centre |
| **Limite operacional** | Token válido por 14400s (4h), refresh token válido por 30 dias |
| **Status de conformidade** | `COMPLIANT` |
| **Risco** | `LOW` |
| **Ação necessária** | Criptografia das chaves do parceiro e renovação via job agendado antes da expiração. |
| **Evidência** | `ShopeeConnector.java`, `ShopeeOpenPlatformClientTest.java` |
| **Data da verificação** | 17/11/2026 |

| Campo | Detalhe |
|---|---|
| **Marketplace** | Shopee |
| **Funcionalidade 2** | Sincronização de Produtos e Pedidos Próprios |
| **Método de integração** | API REST Oficial |
| **Endpoint** | `/api/v2/product/get_item_list`, `/api/v2/order/get_order_list` |
| **Documentação oficial** | https://open.shopee.com/documents/v2/v2.product.get_item_list |
| **Política aplicável** | Rate Limit Policy: padrão de requisições por loja / IP |
| **Permissão necessária** | `shop_id` autenticado com access_token |
| **Limite operacional** | Padrão Shopee de chamadas por minuto por loja (geralmente 10-20 req/s) |
| **Status de conformidade** | `COMPLIANT` |
| **Risco** | `LOW` |
| **Ação necessária** | Respeitar cabeçalho e backoff exponencial em HTTP 429. |
| **Evidência** | `ShopeeConnector.java#fetchListings`, `ShopeeConnector.java#fetchOrders` |
| **Data da verificação** | 17/11/2026 |

| Campo | Detalhe |
|---|---|
| **Marketplace** | Shopee |
| **Funcionalidade 3** | Scraping de Busca Pública ou Preços Concorrentes na Shopee |
| **Método de integração** | Scraping / Headless Browser não autorizado |
| **Endpoint** | Páginas públicas da Shopee (shopee.com.br) |
| **Documentação oficial** | https://open.shopee.com/terms |
| **Política aplicável** | Shopee Terms of Service (Cláusula 7: Proibição Estrita de Scraping e Web Crawlers) |
| **Permissão necessária** | Nenhuma concedida |
| **Limite operacional** | Proibido |
| **Status de conformidade** | `NON_COMPLIANT` |
| **Risco** | `CRITICAL` |
| **Ação necessária** | **ESTRITAMENTE PROIBIDO.** A plataforma não realiza scraping da Shopee. Pesquisas concorrentes na Shopee permanecem desabilitadas sem API oficial pública. |
| **Evidência** | `ShopeeMarketSearchProvider.java` (retorna dados observados estruturados apenas quando API suporta, sem scraping). |
| **Data da verificação** | 17/11/2026 |

---

### 2.3. Amazon (SP-API)

| Campo | Detalhe |
|---|---|
| **Marketplace** | Amazon |
| **Funcionalidade 1** | Autenticação LWA (Login with Amazon) & SigV4 |
| **Método de integração** | SP-API Oficial (REST / OAuth 2.0) |
| **Endpoint** | `https://api.amazon.com/auth/o2/token`, `https://sellercentral.amazon.com.br/apps/authorize/consent` |
| **Documentação oficial** | https://developer-docs.amazon.com/sp-api/docs/authorization-process |
| **Política aplicável** | Amazon SP-API Acceptable Use Policy (AUP) e Data Protection Policy (DPP) |
| **Permissão necessária** | Role SP-API aprovada no Amazon Seller Central Developer Console |
| **Limite operacional** | Access token válido por 3600s; quotas com token bucket (rate/burst por operação) |
| **Status de conformidade** | `COMPLIANT` |
| **Risco** | `LOW` |
| **Ação necessária** | Renovação automática antes do limite de 1h; armazenamento criptografado. |
| **Evidência** | `AmazonSpApiConnector.java`, `AmazonSpApiConnectorContractTest.java` |
| **Data da verificação** | 17/11/2026 |

| Campo | Detalhe |
|---|---|
| **Marketplace** | Amazon |
| **Funcionalidade 2** | Sincronização de Itens de Catálogo e Pedidos |
| **Método de integração** | SP-API Oficial |
| **Endpoint** | `GET /catalog/2022-04-01/items`, `GET /orders/v0/orders` |
| **Documentação oficial** | https://developer-docs.amazon.com/sp-api/docs/orders-api-v0-reference |
| **Política aplicável** | Quotas de Rate Limit (Orders: 0.0167 req/s a 0.5 req/s com burst 30) |
| **Permissão necessária** | Scopes de Pricing, Catalog e Orders |
| **Limite operacional** | Tratamento obrigatório de HTTP 429 com `x-amzn-RateLimit-Limit` |
| **Status de conformidade** | `COMPLIANT` |
| **Risco** | `LOW` |
| **Ação necessária** | Circuit breaker e cooldown integrados no `AmazonSpApiConnector` e `MarketplaceComplianceGuard`. |
| **Evidência** | `AmazonSpApiConnector.java`, `docs/connectors/AMAZON_SP_API.md` |
| **Data da verificação** | 17/11/2026 |

| Campo | Detalhe |
|---|---|
| **Marketplace** | Amazon |
| **Funcionalidade 3** | Tratamento de Dados Pessoais de Comprador (PII) |
| **Método de integração** | Restricted Data Token (RDT) |
| **Endpoint** | `POST /tokens/2021-03-01/restrictedDataToken` |
| **Documentação oficial** | https://developer-docs.amazon.com/sp-api/docs/tokens-api-use-case-guide |
| **Política aplicável** | Amazon Data Protection Policy (DPP): Não retenção desnecessária de PII |
| **Permissão necessária** | Direct-to-Consumer Shipping Role (quando aplicável) |
| **Limite operacional** | RDT expira em 15 minutos |
| **Status de conformidade** | `COMPLIANT` |
| **Risco** | `MEDIUM` |
| **Ação necessária** | Minimização radical: a plataforma não retém nem indexa PII de compradores da Amazon para cálculos de DRE e margem. |
| **Evidência** | `ADR DEC-024`, `docs/security/SECURITY_AUDIT_REPORT.md` |
| **Data da verificação** | 17/11/2026 |

---

### 2.4. TikTok Shop

| Campo | Detalhe |
|---|---|
| **Marketplace** | TikTok Shop |
| **Funcionalidade** | Integração de Lojas, Pedidos e Catálogo |
| **Método de integração** | TikTok Shop Partner API |
| **Endpoint** | `https://open-api.tiktokglobalshop.com` |
| **Documentação oficial** | https://partner.tiktokshop.com/doc/page/63fe7fb0317e13028c29ffc1 |
| **Política aplicável** | TikTok Shop Partner Agreement e Regional Developer Terms |
| **Permissão necessária** | Registro de Partner App aprovado na região Brasil |
| **Limite operacional** | Quotas dinâmicas por tier de parceiro |
| **Status de conformidade** | `UNVERIFIED_COMPLIANCE` |
| **Risco** | `CRITICAL` |
| **Ação necessária** | **BLOQUEADO EM PRODUÇÃO.** Manter sob fail-closed no `MarketplaceComplianceGuard`. Homologar apenas após aprovação formal da conta de parceiro regional. |
| **Evidência** | `MarketplaceComplianceGuard.java`, `MarketplaceComplianceGuardTest.java#scenario13_unverifiedMarketplacesRemainBlocked` |
| **Data da verificação** | 17/11/2026 |

---

### 2.5. Magazine Luiza (Magalu)

| Campo | Detalhe |
|---|---|
| **Marketplace** | Magalu |
| **Funcionalidade** | Conexão de Lojas, Produtos e Pedidos |
| **Método de integração** | LuizaLabs Marketplace API |
| **Endpoint** | `https://api.magazineluiza.com.br` |
| **Documentação oficial** | https://developers.magazineluiza.com.br |
| **Política aplicável** | Termos de Adesão de Desenvolvedor Integrador Magalu |
| **Permissão necessária** | Credenciais de integrador emitidas pelo LuizaLabs |
| **Limite operacional** | Conforme contrato de integrador |
| **Status de conformidade** | `UNVERIFIED_COMPLIANCE` |
| **Risco** | `CRITICAL` |
| **Ação necessária** | **BLOQUEADO EM PRODUÇÃO.** Manter bloqueio fail-closed até obtenção das chaves de sandbox e produção junto ao LuizaLabs. |
| **Evidência** | `MarketplaceComplianceGuard.java`, `COMPLIANCE_DECISIONS.md` |
| **Data da verificação** | 17/11/2026 |

---

### 2.6. SHEIN

| Campo | Detalhe |
|---|---|
| **Marketplace** | SHEIN |
| **Funcionalidade** | Conexão de Lojas e Sincronização |
| **Método de integração** | SHEIN Open Platform API |
| **Endpoint** | `https://openapi.shein.com` |
| **Documentação oficial** | https://open.shein.com |
| **Política aplicável** | SHEIN Open Platform Terms of Use |
| **Permissão necessária** | Conta de desenvolvedor credenciada por categoria/região |
| **Limite operacional** | Restrito a sellers convidados e parceiros credenciados |
| **Status de conformidade** | `UNVERIFIED_COMPLIANCE` |
| **Risco** | `CRITICAL` |
| **Ação necessária** | **BLOQUEADO EM PRODUÇÃO.** Manter sob quarentena fail-closed. Não simular comportamento web ou autenticação não autorizada. |
| **Evidência** | `MarketplaceComplianceGuard.java`, `COMPLIANCE_DECISIONS.md` |
| **Data da verificação** | 17/11/2026 |

---

## 3. Resumo Consolidado de Status

| Marketplace | Catálogo / Produtos | Pedidos / Vendas | Webhooks | Busca Pública | Ações Mutáveis (Preço/Estoque) | Status Global |
|---|---|---|---|---|---|---|
| **Mercado Livre** | `COMPLIANT` | `COMPLIANT` | `COMPLIANT` | `COMPLIANT` | `COMPLIANT` (Human Approval) | `COMPLIANT` |
| **Shopee** | `COMPLIANT` | `COMPLIANT` | `COMPLIANT` | `NOT_APPLICABLE` (No scraping) | `COMPLIANT` (Human Approval) | `COMPLIANT` |
| **Amazon** | `COMPLIANT` | `COMPLIANT` (DPP) | `COMPLIANT` | `NOT_APPLICABLE` (No scraping) | `COMPLIANT` (Human Approval) | `COMPLIANT` |
| **TikTok Shop** | `UNVERIFIED_COMPLIANCE` | `UNVERIFIED_COMPLIANCE` | `UNVERIFIED_COMPLIANCE` | `UNVERIFIED_COMPLIANCE` | `UNVERIFIED_COMPLIANCE` | **`UNVERIFIED_COMPLIANCE` (Bloqueado)** |
| **Magalu** | `UNVERIFIED_COMPLIANCE` | `UNVERIFIED_COMPLIANCE` | `UNVERIFIED_COMPLIANCE` | `UNVERIFIED_COMPLIANCE` | `UNVERIFIED_COMPLIANCE` | **`UNVERIFIED_COMPLIANCE` (Bloqueado)** |
| **SHEIN** | `UNVERIFIED_COMPLIANCE` | `UNVERIFIED_COMPLIANCE` | `UNVERIFIED_COMPLIANCE` | `UNVERIFIED_COMPLIANCE` | `UNVERIFIED_COMPLIANCE` | **`UNVERIFIED_COMPLIANCE` (Bloqueado)** |

---

## 4. Governança Contínua de Políticas
Todas as políticas, termos de uso e ciclos de vida de APIs listados nesta matriz são monitorados continuadamente pelo **Continuous Marketplace Compliance Engine**. Detalhes de arquitetura, ciclo de vida, diretrizes e inventário oficial de políticas encontram-se em:
- [docs/compliance/CONTINUOUS_COMPLIANCE_ARCHITECTURE.md](../compliance/CONTINUOUS_COMPLIANCE_ARCHITECTURE.md)
- [docs/compliance/POLICY_MONITORING_GUIDELINES.md](../compliance/POLICY_MONITORING_GUIDELINES.md)
- [docs/compliance/COMPLIANCE_LIFECYCLE.md](../compliance/COMPLIANCE_LIFECYCLE.md)
- [docs/compliance/COMPLIANCE_INCIDENT_PLAYBOOK.md](../compliance/COMPLIANCE_INCIDENT_PLAYBOOK.md)
- [docs/compliance/SAFE_REACTIVATION.md](../compliance/SAFE_REACTIVATION.md)
- [docs/compliance/MARKETPLACE_POLICY_REGISTRY.md](../compliance/MARKETPLACE_POLICY_REGISTRY.md)

