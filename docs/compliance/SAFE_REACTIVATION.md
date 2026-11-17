# Protocolo de Reativação Segura de Funcionalidades (Safe Reactivation Protocol)

## 1. Princípio de Não-Automatismo na Reativação
A restauração de funcionalidades bloqueadas por motivos de conformidade, termos de uso ou restrições de API **nunca pode ser automática**. 

Enquanto a **suspensão/bloqueio preventivo é imediato e automatizado (Fail Closed)** para conter riscos rapidamente, a **reativação exige intervenção humana qualificada**, validação documental e aprovação auditável.

A tentativa de contornar este protocolo ou reativar recursos via modificação manual direta de banco de dados é tratada como violação grave de integridade arquitetural.

---

## 2. Checklist Obrigatório de Reativação
Antes que qualquer funcionalidade bloqueada possa ter sua reativação aprovada, os seguintes cinco critérios devem ser rigorosamente atendidos e documentados:

| Etapa | Requisito Mandatório | Evidência Exigida |
|---|---|---|
| **1. Resolução do Incidente** | Incidente de conformidade analisado e encerrado ou classificado como mitigado. | Número do incidente (`incidentId`) com status `RESOLVED` ou `CLOSED`. |
| **2. Adequação Técnica de Código** | Ajuste implementado no código, endpoints ou regras de negócio para aderir às novas diretrizes do marketplace. | Identificador do commit ou Pull Request aprovado em revisão por pares. |
| **3. Testes Automatizados Aprovados** | Suíte de testes de unidade, contrato e integração de conformidade executada com 100% de sucesso. | ID do pipeline de CI/CD ou hash de relatório de execução de testes (`testEvidenceRunId`). |
| **4. Homologação em Sandbox** | Chamada experimental de validação bem-sucedida em ambiente de sandbox do marketplace. | Log de resposta de sandbox com código HTTP 200/201 sem advertências de compliance. |
| **5. Assinatura de Responsabilidade** | Identificação formal do operador aprovador com justificativa circunstanciada. | Nome de usuário autenticado (`reviewedBy`) e texto de justificativa técnica (`justification`). |

---

## 3. Fluxo de Execução Técnica

```
[Funcionalidade Bloqueada: SUSPENDED / BLOCKED]
                         │
                         ▼
             [Adequação Técnica & Testes]
                         │
                         ▼
        [Execução de Suíte Automatizada]
        └── Relatório gerado com Test Run ID
                         │
                         ▼
           [Submissão no Painel de Compliance]
           └── Operador preenche Formulário de Reativação
                         │
                         ▼
      [Validação pelo SafeReactivationService]
      ├── 1. Verifica existência e status do FeatureEnforcementRecord
      ├── 2. Valida não-vacuidade de Justificativa, TestEvidenceRunId e Reviewer
      ├── 3. Atualiza status do FeatureEnforcementRecord para ACTIVE
      └── 4. Persiste registro imutável em SafeReactivationLog
                         │
                         ▼
       [Feature Reativada em Tempo Real]
       └── MarketplaceComplianceGuard libera requisições imediatamente
```

---

## 4. Contrato de API para Reativação

### 4.1. Endpoint de Reativação
- **Método:** `POST`
- **Caminho:** `/api/v1/compliance/features/{id}/reactivate`
- **Permissão Obrigatória:** `ROLE_ADMIN` ou `ROLE_COMPLIANCE_OFFICER`

### 4.2. Payload de Requisição (`ReactivateRequest`)
```json
{
  "reviewedBy": "sec-officer@sellerintelligence.com.br",
  "justification": "Endpoint migrado para a versão v2026-04 da API de Catálogo da Shopee conforme documentação técnica oficial. Testes de contrato aprovados na esteira de integração contínua.",
  "testEvidenceRunId": "CI-RUN-2026-11-17-0842-PASSED"
}
```

### 4.3. Resposta de Sucesso (HTTP 200 OK)
```json
{
  "id": "e4b2d358-8b9a-4e55-92c1-d41c9b21fa91",
  "marketplace": "SHOPEE",
  "featureName": "CATALOG_UPDATE",
  "status": "ACTIVE",
  "reason": "Reativado com sucesso após adequação técnica e testes",
  "enforcedAt": "2026-11-17T18:30:00Z",
  "updatedAt": "2026-11-17T19:40:00Z"
}
```

---

## 5. Estrutura de Auditoria Imutável (`SafeReactivationLog`)

Cada evento de reativação bem-sucedido gera uma linha imutável na tabela `safe_reactivation_logs` contendo:
- `id`: Identificador único UUIDv4.
- `enforcement_id`: Chave estrangeira para o registro da funcionalidade reativada.
- `marketplace`: Identificador do canal (`MERCADO_LIVRE`, `SHOPEE`, `AMAZON`, etc.).
- `feature_name`: Nome da funcionalidade reativada (`PRICE_UPDATE`, `ORDER_SYNC`, etc.).
- `reactivated_by`: Identidade do operador responsável.
- `justification`: Justificativa detalhada de liberação.
- `test_evidence_run_id`: Identificador da execução de testes de homologação.
- `created_at`: Carimbo de data/hora UTC com precisão de microssegundos.

Estes registros são protegidos contra alteração ou exclusão e compõem as evidências fornecidas em auditorias de conformidade SOC 2, ISO 27001 e revisões anuais de desenvolvedores de marketplaces.
