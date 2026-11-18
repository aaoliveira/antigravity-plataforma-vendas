# Especificação Técnica: P006-BE — Integração Shopee Open Platform v2, Assinatura HMAC, Webhooks e Políticas

> **Identificador da Feature:** `P006-BE` &middot; **Versão:** 1.0.0  
> **Status da Spec:** Aprovada para Implementação  
> **Requisitos do PRD:** RF-005, RF-006, RF-007, RNF-001, RNF-002, RNF-003, RNF-006, ADR-002, ADR-004  
> **Fonte Oficial:** [Shopee Open Platform (v2.0)](https://open.shopee.com) &middot; Documentação Oficial OpenAPI v2

---

## 1. Contexto e Objetivos

O canal **Shopee** é o segundo marketplace prioritário da **Seller Intelligence Platform**.  
Diferente do Mercado Livre (que adota OAuth 2.0 padrão com PKCE), a **Shopee Open Platform** adota um protocolo baseado em:
1. **Identidade de Parceiro:** Toda requisição e autorização é amarrada a um `partner_id` (long) e validada por uma assinatura criptográfica **HMAC-SHA256** calculada com o `partner_key` (segredo do app).
2. **Autorização no Nível de Loja (Shop Level Permission):** O lojista autoriza a aplicação em `/api/v2/shop/auth_partner` e a Shopee retorna um `code` e um `shop_id` numérico.
3. **Assinatura por Requisição:** A assinatura depende do tipo de endpoint:
   - **Public / Auth API:** $\text{sign} = \text{HMAC-SHA256}(\text{partner\_id} + \text{path} + \text{timestamp}, \text{partner\_key})$
   - **Shop-Level API:** $\text{sign} = \text{HMAC-SHA256}(\text{partner\_id} + \text{path} + \text{timestamp} + \text{access\_token} + \text{shop\_id}, \text{partner\_key})$
4. **Ciclo de Vida de Tokens:**
   - `access_token`: expira em 4 horas (14.400 segundos).
   - `refresh_token`: expira em 30 dias e possui **rotação estrita de uso único** (single-use rotation).
5. **Push Mechanism (Webhooks):**
   - Shopee envia eventos HTTP POST com a assinatura no header `Authorization`.
   - Fórmula do Webhook: $\text{sign} = \text{HMAC-SHA256}(\text{callback\_url} + \text{"|"} + \text{raw\_body}, \text{partner\_key})$.
   - Evento `code: 2` (**Shop Authorization Canceled Push**): a loja revogou o acesso; o sistema deve imediatamente anular credenciais locais e marcar a conexão como `REVOKED`.
6. **Política Fail-Closed & Proteção de Segredos:**
   - O `partner_key` e todos os tokens (`access_token`, `refresh_token`) devem ser cifrados via KMS/AES-256-GCM.
   - Na ausência de credenciais reais homologadas em produção (`shopee.partner-id` ausente ou `shopee.enabled=false`), o conector de produção permanece categoricamente como **`UNVERIFIED`** e chamadas reais para `open.shopee.com` são **bloqueadas**.
   - Em ambiente de testes/sandbox, a conexão de teste é autorizada e toda a máquina de estados e validações de assinatura são testadas com precisão.

---

## 2. Matriz de Evidências Oficiais da Shopee Open Platform

| Item | Especificação Oficial Shopee v2 | Evidência Documental / URL Oficial |
|---|---|---|
| **Portal do Desenvolvedor** | Shopee Open Platform | `https://open.shopee.com` |
| **Sandbox de Testes** | Open Platform Test Sandbox | `https://openplatform.sandbox.test-stable.shopee.sg` |
| **Iniciação de Autorização** | `GET /api/v2/shop/auth_partner?partner_id=...&timestamp=...&sign=...&redirect=...` | Shopee Open Platform: *Shop Authorization* |
| **Troca de Token** | `POST /api/v2/auth/token/get` com body `{"code": "...", "partner_id": ..., "shop_id": ...}` | Shopee Open Platform: `v2.public.get_token_by_resend_code` |
| **Renovação de Token** | `POST /api/v2/auth/access_token/get` com body `{"refresh_token": "...", "partner_id": ..., "shop_id": ...}` | Shopee Open Platform: `v2.public.refresh_access_token` |
| **Assinatura de Webhooks** | Header `Authorization: <hex_sign>` com base string `callback_url + "\|" + raw_body` | Shopee Open Platform: *Push Mechanism Documentation* |
| **Webhook de Revogação** | Push `code: 2` (*Shop Authorization Canceled Push*) | Shopee Open Platform: *Push Notification Codes* |
| **Quotas & Rate Limits** | Frequência por loja e quotas diárias por App ID (`error_rate_limit`, `error_limit`) | Shopee Open Platform: *API Rate Limit & Calling Rules* |

---

## 3. Comparativo de Diferenças: Mercado Livre vs. Shopee

| Dimensão | Mercado Livre (`MLB`) | Shopee (`BR`) |
|---|---|---|
| **Padrão de Autenticação** | OAuth 2.0 Authorization Code Flow com PKCE (RFC 7636) | Open Platform Custom HMAC-SHA256 Signature por chamada |
| **Credenciais de App** | `client_id`, `client_secret` (KMS) | `partner_id` (long), `partner_key` (KMS secret) |
| **Parâmetros de Callback** | `code`, `state` (anti-CSRF) | `code`, `shop_id` (identificador numérico da loja) |
| **Assinatura nas APIs** | Header `Authorization: Bearer <token>` | Query params: `partner_id`, `timestamp`, `sign`, `access_token`, `shop_id` |
| **Validade do Access Token**| 6 horas | 4 horas (14.400 segundos) |
| **Validade do Refresh Token**| Renovável indefinidamente por rotação | 30 dias (de uso único; expira se inativo por 30 dias) |
| **Assinatura de Webhook** | Assinatura HMAC no header `x-signature` ou verificação por endpoint | Header `Authorization` com $\text{HMAC-SHA256}(\text{url} + \text{"\|"} + \text{raw\_body})$ |
| **Revogação pelo Seller** | Notificação via webhook ou endpoint `/users/{id}/applications/{app_id}` | Push Webhook `code: 2` (*Shop Authorization Canceled Push*) |

---

## 4. Cenários BDD / Gherkin

### Cenário 1: Iniciação de Autorização Shopee com Assinatura HMAC-SHA256
```gherkin
Dado que o tenant deseja conectar uma loja Shopee
Quando o sistema solicitar a iniciação de autorização
Então a URL gerada deve conter o domínio da Shopee (ou sandbox)
E deve incluir os parâmetros "partner_id", "timestamp", "sign" e "redirect"
E o parâmetro "sign" deve corresponder ao HMAC-SHA256(partner_id + path + timestamp, partner_key)
```

### Cenário 2: Validação de Callback e Troca Segura de Tokens
```gherkin
Dado que a Shopee redireciona o vendedor com ?code=TEST_AUTH_CODE&shop_id=987654321
Quando o backend processar o callback de autorização
Então deve validar a presença do "code" e "shop_id"
E deve disparar a troca de tokens em /api/v2/auth/token/get
E os tokens retornados (access_token e refresh_token) devem ser cifrados via KMS/AES-256-GCM antes de persistir
E o status da conexão deve ser definido como "ACTIVE" associado ao tenant do lojista
```

### Cenário 3: Renovação de Token com Lock Distribuído
```gherkin
Dado uma conexão Shopee existente com token próximo da expiração
Quando for solicitada a renovação de access_token
Então o endpoint /api/v2/auth/access_token/get deve ser invocado sob lock atômico
E o novo refresh_token retornado deve substituir imediatamente o anterior
E os tokens sensíveis não devem ser expostos nos logs
```

### Cenário 4: Processamento de Webhook de Revogação (Push Code 2)
```gherkin
Dado que a Shopee envia um webhook de cancelamento de autorização (code: 2)
Quando o endpoint de push receber a requisição
Então a assinatura do header "Authorization" deve ser validada contra callback_url + "|" + raw_body
E se a assinatura for válida, a conexão correspondente ao shop_id deve ter status alterado para "REVOKED"
E os tokens cifrados locais devem ser anulados
E se a assinatura for inválida, a requisição deve ser rejeitada com HTTP 401
```

### Cenário 5: Tratamento de Quotas e Erros de Rate Limit da Shopee
```gherkin
Dado que a Shopee retorna erro de limite de taxa ("error_rate_limit" ou "error_limit")
Quando o conector interceptar a resposta
Então deve lançar RateLimitExceededException
E não deve persistir dados inconsistentes nem registrar segredos nos logs
```

### Cenário 6: Bloqueio Fail-Closed na Ausência de Acesso Aprovado
```gherkin
Dado que o conector Shopee está configurado em modo produção sem credenciais aprovadas
Quando uma operação real de integração for solicitada
Então o adaptador deve acusar status "UNVERIFIED"
E a chamada real deve ser sumariamente bloqueada com UnverifiedCapabilityException
```
