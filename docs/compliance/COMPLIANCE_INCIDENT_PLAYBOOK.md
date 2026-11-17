# Playbook de Resposta a Incidentes de Conformidade (Compliance Incident Playbook)

## 1. Objetivo e Escopo
Este playbook estabelece os procedimentos padronizados para detecção, triagem, contenção, resolução e auditoria de incidentes de conformidade e segurança que possam impactar as integrações da **Seller Intelligence Platform** com marketplaces parceiros.

O objetivo primordial é **proteger a segurança jurídica e operacional dos sellers, suas contas comerciais e a integridade da plataforma**.

---

## 2. Classificação de Severidade e Matriz de Resposta

| Nível de Risco | Definição e Cenários Típicos | Ação Imediata do Sistema | SLA de Resposta | Responsável Primário |
|---|---|---|---|---|
| **CRITICAL** | Risco iminente de suspensão de conta de seller, violação direta de termos de uso de marketplace, vazamento de credenciais/tokens, endpoint desligado sem migração, ou quebra de LGPD. | **Bloqueio Automático Imediato (FAIL CLOSED)** na funcionalidade afetada; Notificação urgente em canais de plantão. | **15 minutos** | AppSec / Tech Lead |
| **HIGH** | Anúncio de depreciação com data de corte (sunset) em menos de 30 dias; alteração contratual que exige novos consentimentos; redução substancial de rate limit. | Marcação da funcionalidade como `RESTRICTED` ou agendamento de suspensão; Abertura de card prioritário no backlog. | **2 horas** | Tech Lead / Compliance Officer |
| **MEDIUM** | Depreciação de endpoint com mais de 60 dias de prazo; introdução de novos campos recomendados de catálogo; alteração de diretrizes de categorização. | Registro de incidente; Avaliação de impacto na próxima sprint técnica. | **24 horas** | Engenheiro de Integração |
| **LOW** | Alteração em documentação técnica sem impacto em contratos de API vigentes; reorganização de FAQs ou manuais de integração. | Registro de histórico e arquivamento do diff. | **5 dias úteis** | Analista de QA / Suporte |
| **INFORMATIONAL** | Mudança puramente cosmética, ajuste de formatação ou correção tipográfica na documentação do marketplace. | Atualização automática do hash de conteúdo sem abertura de chamado operacional. | N/A | Automatizado |

---

## 3. Protocolo de Contenção de Menor Escopo (Least Scope Protocol)

Ao detectar uma não-conformidade ou risco crítico, o operador e o sistema devem seguir rigorosamente o princípio do menor escopo:

```
[INCIDENTE DETECTADO]
         │
         ▼
[Identificar Feature Específica Afetada]
   ├── Exemplo 1: Nova política proíbe edição em lote de preços de categoria X
   │     └─► Ação: Suspender APENAS `PRICE_BULK_UPDATE` nessa categoria/canal.
   │     └─► Preservar: Sincronização de pedidos, conciliação e BI continuam 100% ATIVOS.
   │
   └── Exemplo 2: API de Perguntas & Respostas do canal Y sofreu alteração de escopo OAuth
         └─► Ação: Suspender APENAS `MESSAGING_REPLY_ACTION`.
         └─► Preservar: Listagem e monitoramento continuam operando normalmente.
```

---

## 4. Matriz de Papéis e Responsabilidades (RACI)

| Ação / Etapa | Security Engineer | Compliance Officer | Tech Lead | Integration Dev |
|---|---|---|---|---|
| **Detecção e Alerta Inicial** | I | I | A | R |
| **Avaliação de Risco e Escopo** | C | A | R | C |
| **Acionamento de Bloqueio Fail-Closed** | R | A | R | I |
| **Desenvolvimento de Correção / Migração** | C | I | A | R |
| **Execução de Testes Automatizados de Regressão** | C | I | A | R |
| **Homologação em Ambiente Sandbox do Canal** | I | C | A | R |
| **Assinatura de Desbloqueio (SafeReactivation)** | A | R | A | I |
| **Comunicação aos Sellers Impactados** | I | R | C | I |

*Legenda: R = Responsável pela execução; A = Aprovador final; C = Consultado; I = Informado.*

---

## 5. Fluxo de Resolução de Incidentes Passo a Passo

### Passo 1: Triagem e Notificação
1. O `PolicyMonitoringEngine` ou o monitoramento de APIs detecta uma anomalia e instancia um `ComplianceIncident` com status `OPEN`.
2. O `ComplianceNotificationService` despacha webhook/alerta para os canais de engenharia e compliance.
3. O operador técnico confirma a notificação no painel de compliance e assume o incidente (`status: IN_ANALYSIS`).

### Passo 2: Contenção
1. O `AutomaticComplianceEnforcer` verifica a regra associada e registra um `FeatureEnforcementRecord` com status `SUSPENDED` ou `RESTRICTED`.
2. O `MarketplaceComplianceGuard` intercepta requisições de sellers e retorna mensagem descritiva de transparência operacional:
   *"Esta funcionalidade está temporariamente suspensa para atualização preventiva de conformidade com os termos oficiais do marketplace. Seus demais serviços continuam operando normalmente."*

### Passo 3: Engenharia e Correção
1. É criada uma branch de remediação (`fix/compliance-<incident-id>`).
2. Implementam-se os novos requisitos de API, validação ou escopo.
3. Criam-se testes automatizados dedicados cobrindo os novos critérios.
4. Executa-se a suíte completa de testes de integração e regressão.

### Passo 4: Aprovação e Reativação Segura
1. O responsável técnico submete os artefatos de teste e o relatório de homologação no `SafeReactivationService`.
2. O Compliance Officer e o Tech Lead realizam a aprovação formal digital.
3. O status do `FeatureEnforcementRecord` é atualizado para `ACTIVE`.
4. É gerado registro imutável em `safe_reactivation_logs`.

---

## 6. Modelo de Post-Mortem de Conformidade

Todo incidente classificado como `CRITICAL` ou `HIGH` exige a elaboração de um relatório de Post-Mortem em até 48 horas após a resolução:

```markdown
# Relatório de Post-Mortem de Incidente de Conformidade

- **ID do Incidente:** INC-YYYYMMDD-XXX
- **Marketplace Afetado:** [Mercado Livre / Shopee / Amazon / TikTok Shop / Magalu / SHEIN]
- **Severidade:** [CRITICAL / HIGH]
- **Data/Hora de Início:** YYYY-MM-DD HH:mm UTC
- **Data/Hora de Resolução:** YYYY-MM-DD HH:mm UTC
- **Tempo Total de Contenção (TTR):** XX minutos

### 1. Resumo Executivo
Descrição breve do ocorrido, causa raiz e impacto em clientes/tenants.

### 2. Causa Raiz Técnica / Regulatória
Detalhamento da alteração no marketplace (cláusula, endpoint ou escopo) e por que o sistema identificou o risco.

### 3. Ações de Menor Escopo Aplicadas
Quais funcionalidades foram suspensas e quais permaneceram ativas para garantir a continuidade dos sellers.

### 4. Linha do Tempo dos Fatos
- HH:mm - Detecção pelo PolicyMonitoringEngine
- HH:mm - Contenção aplicada via FeatureEnforcementRecord
- HH:mm - Equipe de desenvolvimento mobilizada
- HH:mm - Correção commitada e testes de integração executados
- HH:mm - Reativação formal executada via SafeReactivationService

### 5. Medidas Preventivas e Melhorias
- [ ] Ação 1: Adição de novo teste de contrato para API X
- [ ] Ação 2: Refinamento de regra semântica de diff
- [ ] Ação 3: Atualização de documentação de integração
```
