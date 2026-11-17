# Relatório de Auditoria de Segurança, Compliance e Proteção de Contas

**Plataforma:** Seller Intelligence Platform  
**Data da Auditoria:** 17 de Novembro de 2026  
**Auditor Responsável:** Staff Software & Security Engineering / Marketplace Compliance Officer  
**Versão:** 1.0.0  
**Status do Gate de Segurança:** `APPROVED`  

---

## 1. Resumo Executivo

A **Seller Intelligence Platform** é uma plataforma SaaS multi-tenant voltada a vendedores profissionais de e-commerce e marketplaces (Mercado Livre, Shopee, Amazon, TikTok Shop, Magalu e SHEIN). No contexto de comércio eletrônico automatizado, **a proteção da conta comercial do vendedor e a integridade de suas operações constituem requisitos existenciais de negócio**.

Esta auditoria profunda foi conduzida para identificar, corrigir, bloquear e prevenir comportamentos técnicos ou arquiteturais que pudessem expor vendedores a:
- Suspensão ou banimento temporário ou permanente de contas em marketplaces.
- Revogação de credenciais OAuth 2.0 e bloqueio de aplicações parceiras.
- Bloqueio por automação abusiva, rate limits (HTTP 429) e penalizações em algoritmos de busca.
- Acesso indevido cross-tenant ou vazamento de dados confidenciais (pedidos, clientes, margens, faturamento).
- Violações da Lei Geral de Proteção de Dados (LGPD) e diretrizes de minimização de PII.
- Riscos operacionais decorrentes de automações sem supervisão humana (Human-in-the-Loop).

### Conclusão Executiva
A plataforma adota formalmente o princípio arquitetural **FAIL CLOSED**. Nenhuma operação externa em marketplace é permitida a menos que sua legitimidade, autenticação ativa, conformidade contratual e ausência de quarentena estejam verificadas. Qualquer canal sem homologação formal comprovada (caso atual de TikTok Shop, Magalu e SHEIN) permanece **estritamente bloqueado para produção** sob o rótulo `UNVERIFIED_COMPLIANCE`. Mutações comerciais críticas (publicação de anúncios, alteração de preços, alteração de estoque) exigem aprovação explícita do usuário seller.

---

## 2. Escopo Auditado

A auditoria cobriu 100% da superfície arquitetural da Seller Intelligence Platform:
1. **Conectores Oficiais de Marketplace:**
   - Mercado Livre (OAuth 2.0, Catalog, Orders v2, Webhooks com HMAC, Buscas públicas).
   - Shopee (Open Platform v2, SHA256 HMAC signature, Order sync, Webhooks).
   - Amazon (Selling Partner API - SP-API, LWA OAuth 2.0, Catalog 2022-04-01, Orders v0, RDT PII protection).
   - TikTok Shop, Magalu e SHEIN (Avaliação documental e bloqueio preventivo).
2. **Camada de Automação e IA:**
   - Spring AI / Listing Studio (Geração de títulos, descrições e scoring com prompts versionados e proteção contra prompt injection).
   - Servidor MCP (Model Context Protocol) Read-Only (Catálogo de ferramentas estritas, sem SQL arbitrário gerado por IA).
3. **Extensão de Navegador:**
   - Chrome Extension Manifest V3 (Content scripts restritos, service worker sem tokens persistidos em local storage inseguro, sem auto-clicker ou scraping agressivo).
4. **Isolamento Multi-Tenant e Segurança de Dados:**
   - Tenant isolation em PostgreSQL (coluna obrigatória `tenant_id`, índices compostos, TenantContext com ThreadLocal sanitizado).
   - Criptografia simétrica AES-256-GCM para tokens OAuth e credenciais de marketplace em repouso.
   - Sanitização de logs para mascaramento de tokens (`[REDACTED]`).
5. **Observabilidade e Resiliência Operacional:**
   - Circuit Breaker centralizado e quarentena de HTTP 429 (Too Many Requests).
   - Trilha de auditoria append-only (`audit_events`) persistida com transação autônoma (`REQUIRES_NEW`).

---

## 3. Metodologia de Avaliação

A auditoria combinou abordagens estáticas, dinâmicas e de compliance documental:
- **OWASP ASVS v4.0.3 (Application Security Verification Standard):** Níveis 2 e 3 aplicáveis a SaaS financeiro e comercial (V2 Autenticação, V3 Gerenciamento de Sessão, V4 Controle de Acesso, V5 Validação de Entrada, V8 Criptografia, V10 Comunicação Segura).
- **OWASP Top 10 2021:** A01 Broken Access Control (IDOR), A02 Cryptographic Failures, A03 Injection, A04 Insecure Design, A07 Identification & Auth Failures, A08 Software & Data Integrity.
- **Marketplace API Policies & Developer Agreements:**
  - Mercado Livre Developers Policy & Terms of Use.
  - Shopee Open Platform Developer Service Agreement & API Rate Limit Guidelines.
  - Amazon SP-API Acceptable Use Policy (AUP), Data Protection Policy (DPP) e Marketplace Developer Agreement.
  - TikTok Shop Academy & Partner Terms.
  - Magalu LuizaLabs Marketplace API Terms.
  - SHEIN Open Platform Terms.
- **LGPD (Lei nº 13.709/2018):** Minimização de dados, tratamento restrito de PII de compradores, ciclo de retenção e base legal.

---

## 4. Classificação de Riscos e Não Conformidades

| ID | Categoria | Marketplace / Componente | Severidade | Descrição do Risco | Resolução / Mitigação Implementada |
|---|---|---|---|---|---|
| **VULN-001** | Abuse Prevention | Canais em Expansão (TikTok, Magalu, SHEIN) | **CRITICAL** | Tentativa prematura de conexão ou automação em canais sem homologação/parceria formal pode violar termos e gerar banimento imediato. | **FAIL CLOSED Implementado:** Canais marcados como `UNVERIFIED_COMPLIANCE`. `MarketplaceComplianceGuard` bloqueia categoricamente qualquer tentativa de conexão ou chamada. |
| **VULN-002** | Account Protection | Automação Comercial (Preços / Anúncios) | **HIGH** | Alteração automatizada não supervisionada de preços ou títulos pode causar prejuízos severos ou violações de precificação abusiva. | **Mandatory Human-in-the-Loop:** Operações `MUTABLE_CRITICAL` bloqueadas a menos que `humanApproved == true`. Eventos auditados. |
| **VULN-003** | Rate Limiting | Conectores Mercado Livre & Shopee | **HIGH** | Chamadas repetitivas após HTTP 429 podem gerar banimento de IP ou suspensão de app key. | **Rate Limit Cooldown:** Detecção de HTTP 429 aciona quarentena imediata com duração baseada no cabeçalho `Retry-After` (mínimo 60s). |
| **VULN-004** | Fault Tolerance | Chamadas Consecutivas com Erro | **MEDIUM** | Requisições repetidas com token revogado ou falha de infraestrutura externa degradam performance e geram alertas nos marketplaces. | **Circuit Breaker Operacional:** Limiar de 5 falhas consecutivas abre o circuito por 60 segundos (`CIRCUIT_BREAKER_OPEN`). |
| **VULN-005** | Credential Leak | Logs de Aplicação e Exceções | **HIGH** | Risco de vazamento de access tokens ou refresh tokens em stack traces, logs ou relatórios de erro. | **Sanitização Universal:** Regex `SENSITIVE_PATTERN` em `ComplianceViolationException` e `AuditService` substitui tokens por `[REDACTED]`. |
| **VULN-006** | Multi-Tenancy | Acesso a Lojas e Credenciais (IDOR) | **CRITICAL** | Risco de um tenant acessar ou disparar operações em lojas conectadas de outro tenant. | **Isolamento de Escopo:** Queries com `tenantId` estrito e validação em `MarketplaceComplianceGuard` (`STORE_NOT_FOUND_OR_CROSS_TENANT`). |
| **VULN-007** | Scraping / Anti-Bot | Coleta Pública e Monitoramento | **HIGH** | Utilização de bots, proxies rotativos ou contorno de CAPTCHA viola termos contratuais e gera risco legal e reputacional. | **Proibição Absoluta de Scraping Ilegítimo:** Removida qualquer técnica de scraping não autorizada. Coletas públicas usam estritamente APIs oficiais autorizadas. |
| **VULN-008** | Browser Extension | Manifest V3 / Injeção em Páginas | **MEDIUM** | Extensão executando cliques fantasmas ou lendo dados fora do contexto autorizado. | **Permissões Mínimas:** Content script restrito a URLs permitidas; sem persistência insegura de tokens; ações de envio exigem clique explícito do usuário. |
| **VULN-009** | AI & Prompts | Listing Studio & MCP Server | **MEDIUM** | IA gerando comandos maliciosos ou tentando executar ações mutáveis nos canais diretamente. | **Read-Only MCP & Guard Enforcement:** MCP Server 100% read-only; sugestões de IA são meramente consultivas e exigem aprovação do seller. |
| **VULN-010** | Data Protection | PII de Compradores (Amazon / Shopee) | **HIGH** | Armazenamento indiscriminado de CPF, nome e endereço de compradores violando LGPD e Amazon DPP. | **Minimização de Dados:** Pedidos armazenam apenas identificadores e valores financeiros necessários para conciliação e DRE. PII de comprador não é indexado. |

---

## 5. Avaliação Independente por Marketplace

### 5.1. Mercado Livre
- **Status Geral:** `COMPLIANT` para Catálogo, Pedidos e Webhooks. `PARTIALLY_COMPLIANT` para Ads e Campanhas (exige homologação complementar).
- **Pontos Fortes:** OAuth 2.0 padrão, validação de redirect URI, webhooks assinados, rate limits claros via headers (`X-Rate-Limit-Remaining`).
- **Riscos Mitigados:** Chamadas sem token válido são rejeitadas preventivamente no backend antes do envio aos servidores da MELI.

### 5.2. Shopee
- **Status Geral:** `COMPLIANT` para Catálogo e Pedidos v2.
- **Pontos Fortes:** Assinatura criptográfica HMAC-SHA256 baseada em `partner_key`, timestamp e rota oficial.
- **Riscos Mitigados:** Proibido scraping de busca pública da Shopee; dados de mercado são restritos ao escopo das lojas autenticadas do próprio seller.

### 5.3. Amazon (SP-API)
- **Status Geral:** `COMPLIANT` para Selling Partner API (LWA OAuth 2.0).
- **Pontos Fortes:** Autenticação robusta Login with Amazon, tokens com expiração de 3600s, endpoints seguros com assinatura SigV4/SP-API.
- **Riscos Mitigados:** Conformidade estrita com Amazon DPP (Data Protection Policy). Dados de comprador (PII) exigem RDT (Restricted Data Token) e não são armazenados permanentemente quando desnecessários.

### 5.4. TikTok Shop
- **Status Geral:** `UNVERIFIED_COMPLIANCE`.
- **Decisão:** **ESTRITAMENTE BLOQUEADO EM PRODUÇÃO.**
- **Motivo:** Plataforma de integração comercial no Brasil em fase de homologação restrita. Conexões diretas sem programa de parceiro credenciado geram banimento imediato da loja do vendedor.

### 5.5. Magazine Luiza (Magalu)
- **Status Geral:** `UNVERIFIED_COMPLIANCE`.
- **Decisão:** **ESTRITAMENTE BLOQUEADO EM PRODUÇÃO.**
- **Motivo:** Exige credenciamento formal e assinatura de termo de integrador no LuizaLabs Open API. Bloqueado até assinatura contratual e chaves de produção emitidas.

### 5.6. SHEIN
- **Status Geral:** `UNVERIFIED_COMPLIANCE`.
- **Decisão:** **ESTRITAMENTE BLOQUEADO EM PRODUÇÃO.**
- **Motivo:** O SHEIN Open Platform opera com restrições rígidas por geografia e categoria. Conexão bloqueada preventivamente até homologação oficial.

---

## 6. Resultado da Auditoria e Parecer Técnico

A arquitetura e os controles implementados satisfazem integralmente os 15 critérios de segurança e proteção de contas exigidos pelo projeto:
1. Operações não autorizadas são bloqueadas sob a política **FAIL CLOSED**.
2. Tokens expirados ou revogados impedem novas chamadas.
3. Tratamento determinístico de HTTP 429 com quarentena e circuit breaker após falhas repetitivas.
4. Isolamento multi-tenant garantido em todas as camadas sem risco de IDOR.
5. Aprovação humana mandatória para ações mutáveis.
6. Canais não homologados permanecem categoricamente bloqueados.

**Parecer Final:** `APPROVED`. A plataforma está protegida e em conformidade estrita para as operações com Mercado Livre, Shopee e Amazon, mantendo sob quarentena e bloqueio os canais e funcionalidades não autorizados.
