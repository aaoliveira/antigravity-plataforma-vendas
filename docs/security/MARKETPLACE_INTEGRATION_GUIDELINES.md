# Diretrizes de Integração Segura com Marketplaces (Integration Guidelines)

**Plataforma:** Seller Intelligence Platform  
**Público-alvo:** Engenheiros de Software, Arquitetos, Desenvolvedores de Conectores e Agentes de IA  
**Versão:** 1.0.0  
**Status:** `VINCULANTE / MANDATÓRIO`  

---

## 1. Princípio Fundamental: Fail Closed e Proteção da Conta

No ecossistema da **Seller Intelligence Platform**, a preservação da legitimidade e da integridade da conta comercial do vendedor é um requisito inegociável de negócio e arquitetura. Qualquer falha técnica ou comportamento abusivo pode resultar em suspensão de conta, retenção de recebíveis ou banimento definitivo do seller no marketplace.

Por isso, **toda integração opera sob a política FAIL CLOSED**:
> *"Se não for possível confirmar a legitimidade, a autorização, a validade do token ou a conformidade contratual de uma operação, ela não deverá ser executada."*

---

## 2. As 15 Regras Inegociáveis de Desenvolvimento

Durante o desenvolvimento, manutenção e evolução de conectores, extensões e rotinas de backend, as seguintes regras devem ser cumpridas sem exceção:

1. **Não inventar políticas:** Não presuma que regras vigentes em um marketplace se aplicam a outro. Consulte sempre os termos oficiais da plataforma em questão.
2. **Não inventar permissões:** Não utilize escopos ou chaves além daqueles concedidos explicitamente pelo seller no fluxo de consentimento.
3. **Não inventar endpoints:** É estritamente proibido utilizar endpoints privados, não documentados ou obtidos por engenharia reversa de frontends sem documentação oficial de suporte.
4. **Não inventar respostas de APIs:** Mantenha testes baseados em mocks aderentes aos contratos e schemas JSON oficiais divulgados nas documentações dos marketplaces.
5. **Não considerar scraping automaticamente permitido:** O fato de uma informação estar visível no navegador não autoriza sua coleta automatizada. Scraping de páginas protegidas é terminantemente proibido.
6. **Não implementar bypass de CAPTCHA:** Qualquer desafio de verificação humana deve ser apresentado ao usuário; jamais automatize a quebra de mecanismos anti-bot (reCAPTCHA, hCaptcha, Turnstile, etc.).
7. **Não implementar evasão de mecanismos anti-bot:** É vedado o uso de técnicas de spoofing de headers, falsificação de fingerprints de navegador ou mascaramento de user-agent.
8. **Não implementar rotação de IP para contornar limites:** Rate limits devem ser respeitados via fila, backoff e contenção local, nunca contornados via proxies rotativos para burlar quotas.
9. **Não implementar falsificação de fingerprint:** O tráfego emitido pela plataforma ou extensão deve se identificar com honestidade técnica.
10. **Não implementar automações abusivas:** Auto-compras, auto-cliques ou publicação massiva sem supervisão são estritamente proibidos.
11. **Não expor credenciais:** Chaves de API, client secrets, access tokens e refresh tokens jamais devem ser impressos em logs, exceções, respostas HTTP ou commitados no repositório Git.
12. **Não ignorar erros de autorização:** Erros HTTP 401 Unauthorized ou 403 Forbidden devem interromper o ciclo de chamadas e disparar revisão de token ou status de conexão.
13. **Não transformar estimativas em dados reais:** Todo dado analítico deve ser categorizado com proveniência explícita (`OBSERVED`, `IMPORTED`, `ESTIMATED`, `AI_GENERATED` ou `UNAVAILABLE`).
14. **Não remover controles de segurança para facilitar testes:** Controles multi-tenant e validações de conformidade devem permanecer ativos em testes de integração e homologação.
15. **Não habilitar funcionalidades com conformidade desconhecida em produção:** Recursos sem certificação documental formal devem permanecer sob status `UNVERIFIED_COMPLIANCE` e bloqueados para produção.

---

## 3. Padrões de Implementação de Conectores

### 3.1. Autenticação e Gestão de Tokens
- Use exclusivamente **OAuth 2.0 com validação de State CSRF** e PKCE quando aplicável.
- Tokens em repouso devem ser criptografados via `TokenCryptoService` com cifra simétrica AES-256-GCM.
- Armazene o timestamp de expiração (`expires_at`) e implemente renovação preventiva via refresh token com folga mínima de 5 minutos antes do vencimento.

### 3.2. Resiliência e Rate Limiting
- **Tratamento de HTTP 429 Too Many Requests:**
  - Leia o cabeçalho `Retry-After` ou equivalente (`x-amzn-RateLimit-Limit`, `X-Rate-Limit-Remaining`).
  - Notifique o `MarketplaceComplianceGuard.recordRateLimit429(storeId, seconds)`.
  - O conector deve suspender requisições para a respectiva loja até o término da janela de cooldown.
- **Circuit Breaker:**
  - Após 5 falhas consecutivas de rede ou erros 5xx/401/403, o circuito abre por 60 segundos (`CIRCUIT_BREAKER_OPEN`).
  - Chamadas de teste (half-open) só acontecem após o encerramento do período de quarentena.

### 3.3. Ações Mutáveis Críticas (Human-in-the-Loop)
- Operações de escrita em marketplaces (publicação de anúncios, alteração de preço, alteração de estoque, envio de mensagens) pertencem à categoria `OperationRiskLevel.MUTABLE_CRITICAL`.
- Todo endpoint ou worker que execute tais operações deve invocar:
  ```java
  complianceGuard.enforceOperation(
      tenantId, storeId, marketplace,
      OperationRiskLevel.MUTABLE_CRITICAL,
      isHumanApproved, // Obrigatoriamente true mediante consentimento explícito
      actorId
  );
  ```
- Se `humanApproved == false`, o `MarketplaceComplianceGuard` lançará `ComplianceViolationException("HUMAN_APPROVAL_REQUIRED")` e registrará evento de auditoria com status `DENIED`.

### 3.4. Webhooks e Notificações Push
- Valide a assinatura criptográfica de cada payload (ex.: HMAC-SHA256 da MELI e Shopee) antes de consumir os dados.
- Mantenha tabela de controle de eventos processados (`webhook_events`) com identificador externo para garantir idempotência estrita e proteção contra replay attacks.
- Responda HTTP 200/204 rapidamente e processe a carga de forma assíncrona via filas RabbitMQ.

---

## 4. Checklist para Homologação de Novos Marketplaces

Para promover um marketplace de `UNVERIFIED_COMPLIANCE` para `COMPLIANT`:
1. [ ] Termo de adesão ao programa oficial de parceiros ou desenvolvedores assinado pela organização.
2. [ ] Credenciais oficiais de Sandbox e Produção emitidas no portal do marketplace.
3. [ ] Atualização da `MARKETPLACE_COMPLIANCE_MATRIX.md` com URLs de documentação oficial e data de verificação.
4. [ ] Implementação de classe adaptadora aderente à interface `MarketplaceConnector`.
5. [ ] Suíte de testes de contrato cobrindo OAuth, busca de catálogo, pedidos e tratamento de rate limits.
6. [ ] Validação no `MarketplaceComplianceGuardTest` garantindo operação segura e isolamento multi-tenant.
7. [ ] Registro de ADR no `DECISION_LOG.md` formalizando a aceitação do novo canal.
