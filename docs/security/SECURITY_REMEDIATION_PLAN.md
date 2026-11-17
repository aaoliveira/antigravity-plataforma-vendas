# Plano de Remediação de Segurança e Compliance

**Plataforma:** Seller Intelligence Platform  
**Data de Emissão:** 17 de Novembro de 2026  
**Responsável:** Application Security Lead & Compliance Engineering  
**Status:** `RESOLVED / IN_CONTINUOUS_MONITORING`  

---

## 1. Diretrizes de SLA por Severidade

| Severidade | Descrição | SLA Máximo para Mitigação | Ação Mandatória |
|---|---|---|---|
| **CRITICAL** | Risco imediato de banimento de contas de sellers, vazamento cross-tenant ou comprometimento de tokens. | **Imediato (0 dias)** | Bloqueio `FAIL CLOSED` ou Hotfix imediato antes de qualquer release. |
| **HIGH** | Violação potencial de limites de API, falha de autorização RBAC ou ausência de consentimento humano em mutações. | **Até 7 dias** | Implementação de guardrails, circuit breakers e testes automatizados. |
| **MEDIUM** | Tratamento incompleto de falhas, sanitização preventiva de campos não confidenciais ou logging redundante. | **Até 14 dias** | Ajustes arquiteturais e sanitização. |
| **LOW / INFO** | Otimizações de configuração, limpeza de código e atualização documental de novos escopos. | **Até 30 dias** | Revisão em ciclo regular de sprint. |

---

## 2. Inventário de Remediações Implementadas

| ID | Vulnerabilidade / Risco | Severidade | Ação de Remediação Implementada | Responsável | Status | Verificação |
|---|---|---|---|---|---|---|
| **REM-001** | Ativação não autorizada de canais sem parceria (TikTok, Magalu, SHEIN) | **CRITICAL** | Implementação do bloqueio `UNVERIFIED_MARKETPLACE_BLOCKED` no `MarketplaceComplianceGuard`. Conexão OAuth e chamadas de API barradas em runtime. | AppSec & Backend Lead | **RESOLVIDO** | `MarketplaceComplianceGuardTest#scenario13` |
| **REM-002** | Alteração automatizada de preços/anúncios sem supervisão | **HIGH** | Requisito mandatória de flag `humanApproved == true` para qualquer operação com nível `MUTABLE_CRITICAL`. Bloqueio por default com `HUMAN_APPROVAL_REQUIRED`. | Connector Engineer | **RESOLVIDO** | `MarketplaceComplianceGuardTest#scenario12` |
| **REM-003** | Sobrecarga de chamadas em HTTP 429 gerando suspensão de IP | **HIGH** | Criação de quarentena automática de 60s (ou tempo do cabeçalho `Retry-After`) por loja no `MarketplaceComplianceGuard`. Novas chamadas rejeitadas localmente. | Reliability Engineer | **RESOLVIDO** | `MarketplaceComplianceGuardTest#scenario04` |
| **REM-004** | Degradação por erros repetidos de rede/API externa | **MEDIUM** | Implementação de Circuit Breaker com limiar de 5 falhas consecutivas abrindo quarentena temporária (`CIRCUIT_BREAKER_OPEN`). | Reliability Engineer | **RESOLVIDO** | `MarketplaceComplianceGuardTest#scenario05` |
| **REM-005** | Vazamento acidental de tokens em mensagens de log ou exceções | **HIGH** | Sanitização regex `SENSITIVE_PATTERN` em `ComplianceViolationException` e `AuditService` que substitui credenciais e Bearer tokens por `[REDACTED]`. | AppSec Engineer | **RESOLVIDO** | `MarketplaceComplianceGuardTest#scenario07` |
| **REM-006** | Acesso cross-tenant a lojas e credenciais (IDOR) | **CRITICAL** | Validação mandatória de escopo `tenantId` em todas as queries e verificação explícita no `MarketplaceComplianceGuard` (`STORE_NOT_FOUND_OR_CROSS_TENANT`). | Security Architect | **RESOLVIDO** | `MarketplaceComplianceGuardTest#scenario08` |
| **REM-007** | Scraping agressivo ou uso de headless browser não autorizado | **HIGH** | Remoção de qualquer fluxo de scraping não documentado. Plataforma opera estritamente via APIs oficiais autorizadas. Buscas públicas rotuladas `OBSERVED`. | Data Engineer | **RESOLVIDO** | `MARKETPLACE_COMPLIANCE_MATRIX.md` |
| **REM-008** | Invasão ou abuso via extensão de navegador | **MEDIUM** | Extensão em Manifest V3 com host permissions mínimas, sem armazenamento de tokens em local storage e sem execução de cliques robóticos não autorizados. | Frontend Engineer | **RESOLVIDO** | `MarketplaceComplianceGuardTest#scenario11` |
| **REM-009** | Execução de comandos ou mutações diretas por agentes de IA/MCP | **MEDIUM** | Servidor MCP mantido 100% read-only. Ferramentas de IA atuam exclusivamente como assessoras analíticas, exigindo aprovação no frontend. | AI Engineer | **RESOLVIDO** | `MarketplaceComplianceGuardTest#scenario10` |
| **REM-010** | Retenção indiscriminada de dados pessoais de compradores (PII) | **HIGH** | Ajuste das entidades de sincronização de pedidos para não reter CPF, telefone ou endereço residencial de compradores da Amazon/Shopee para fins de inteligência. | DPO / Legal Tech | **RESOLVIDO** | `SECURITY_AUDIT_REPORT.md` |

---

## 3. Procedimento Operacional Pós-Incidente e Quarentena

Caso um marketplace retorne código de erro crítico (ex.: revogação de chaves, HTTP 403 Forbidden por suspensão de app ou abuso):
1. **Desativação Imediata:** O `MarketplaceComplianceGuard` marca a integração da loja afetada como `NEEDS_REAUTH` ou `REVOKED`.
2. **Notificação Segura:** Emissão de alerta operacional de severidade máxima ao seller via Action Center, sem expor credenciais.
3. **Bloqueio de Retentativas:** O circuit breaker impede qualquer worker ou fila de continuar disparando requisições contra o marketplace.
4. **Trilha de Auditoria:** Gravação detalhada de cabeçalhos de resposta HTTP (sanitizados) para diagnóstico conjunto com o suporte do canal.
5. **Reativação Controlada:** Reautorização manual pelo seller via OAuth 2.0 padrão após saneamento da causa raiz.

---

## 4. Cronograma de Reavaliação

- **Auditoria de Conformidade Trimestral:** Revisão de alterações nas políticas de desenvolvedores de Mercado Livre, Shopee e Amazon.
- **Homologação de Novos Canais:** TikTok Shop, Magalu e SHEIN serão submetidos a reavaliação técnica somente mediante assinatura de contrato de parceria e concessão de chaves de sandbox oficiais.
