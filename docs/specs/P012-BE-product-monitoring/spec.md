# Especificação Técnica — P012-BE: Grupos por Tenant, Itens Monitorados e Snapshots Históricos Seguros

**Status:** IN_PROGRESS  
**Prompt de Origem:** P012-BE  
**Data:** 18/11/2026  
**Responsável:** Antigravity Agent  
**Contexto:** P011-FE aprovado (Interface de Comparação de Listings Equivalentes).

---

## 1. Visão Geral e Objetivos do Módulo

Implementar o módulo de backend de **Monitoramento Contínuo de Produtos** (`com.sellerintelligence.modules.productmonitoring`), permitindo a lojistas organizar produtos de mercado e concorrentes em **Grupos de Monitoramento por Tenant** e coletar **Snapshots Históricos Auditáveis** ao longo do tempo.

O módulo atende de forma irrestrita às regras de ouro da plataforma:
1. **CRUD Isolado por Tenant:** Grupos, itens e snapshots são vinculados obrigatoriamente a `tenant_id` e isolados em profundidade via PostgreSQL 17 Row-Level Security (RLS). Nenhuma operação cross-tenant é permitida.
2. **Duplicidade Controlada:**
   - Nomes de grupos são únicos por tenant (`uq_monitoring_groups_tenant_name`).
   - Um mesmo item (`marketplace + external_listing_id`) não pode ser adicionado repetidamente ao mesmo grupo (`uq_monitored_item_group_listing`), prevenindo duplicação de jobs e dispersão de métricas.
3. **Histórico Não-Retroativo:**
   - **"O histórico começa no primeiro snapshot efetivo após a inclusão."**
   - Sob nenhuma hipótese o sistema sintetiza, interpola ou retroage dados anteriores à data/hora em que o item foi adicionado ao monitoramento (`first_snapshot_at >= created_at`).
4. **Agendamento com Respeito a Quotas, Janela Mínima, Kill Switch e Capacidade Validada:**
   - **Janela Mínima:** Cada grupo/item define um intervalo mínimo (`min_interval_minutes`, padrão 60 min, mínimo 15 min). Se um job for acionado antes da janela decorrida, a execução é ignorada com status `SKIPPED_WINDOW_MINIMA`.
   - **Quotas / Rate Limits:** O agendador respeita o teto de requisições por janela temporal do tenant e do canal; se a quota estiver saturada, o job é ignorado com `SKIPPED_RATE_LIMIT`.
   - **Kill Switch:** Se um switch de emergência for acionado para um canal ou fonte, as coletas são abortadas imediatamente com `SKIPPED_KILL_SWITCH`, gerando log e auditoria sem onerar a infraestrutura.
   - **Capacidade Validada & Fontes Proibidas:** Não buscar dado em fonte proibida. Checagem prévia de política de canal e capability (`CapabilityStatus.SUPPORTED`). Tentativa em fonte proibida resulta em rejeição com `FAILED_UNAUTHORIZED_SOURCE`.
5. **Exclusão com Retenção Definida:**
   - Grupos e itens removidos transitam para `ARCHIVED` ou `DELETED` (`deleted_at = NOW()`), interrompendo novos jobs.
   - Snapshots históricos permanecem armazenados para fins de auditoria e conformidade fiscal/legal até o término do período de retenção configurado (`retention_days`, padrão 90 dias), momento em que o serviço de expurgo (`MonitoringRetentionService`) realiza a limpeza definitiva.

---

## 2. Requisitos Funcionais e Critérios de Aceite

| ID | Requisito | Critério de Aceite |
| :--- | :--- | :--- |
| **FR-MON-01** | CRUD de Grupos | Criar, editar, listar e remover grupos de monitoramento com isolamento estrito por `tenant_id`. |
| **FR-MON-02** | Gestão de Itens no Grupo | Adicionar, listar e remover itens monitorados vinculados a marketplace e política de fonte autorizada. |
| **FR-MON-03** | Controle de Duplicidade | Rejeição (HTTP 409) ao tentar criar grupo com nome duplicado no mesmo tenant ou adicionar o mesmo anúncio mais de uma vez ao mesmo grupo. |
| **FR-MON-04** | Histórico Não-Retroativo | Snapshots coletados iniciam a partir da data de inclusão do item. Consultas históricas nunca retornam snapshots forjados pré-inclusão. |
| **FR-MON-05** | Janela Mínima de Agendamento | Jobs disparados antes de decorrido o intervalo mínimo (`min_interval_minutes`) são registrados como `SKIPPED_WINDOW_MINIMA`. |
| **FR-MON-06** | Kill Switch & Rate Limit | Abortar coletas e registrar auditoria quando o Kill Switch estiver ativo (`SKIPPED_KILL_SWITCH`) ou a quota esgotada (`SKIPPED_RATE_LIMIT`). |
| **FR-MON-07** | Bloqueio de Fontes Proibidas | Rejeitar agendamento ou snapshot para fontes não autorizadas ou capacidades não homologadas (`FAILED_UNAUTHORIZED_SOURCE`). |
| **FR-MON-08** | Política de Retenção e Exclusão | Itens removidos param de coletar dados; histórico é mantido durante o período de retenção (`retention_days`) antes do expurgo permanente. |
| **FR-MON-09** | Isolamento PostgreSQL RLS | Tabelas protegidas por RLS forçada (`FORCE ROW LEVEL SECURITY`) impedindo vazamento cross-tenant comprovado via Testcontainers. |

---

## 3. Arquitetura de Dados (Flyway Migrations)

### 3.1 Tabelas
1. `monitoring_groups`:
   - `id UUID PRIMARY KEY`, `tenant_id UUID NOT NULL`
   - `name VARCHAR(120) NOT NULL`, `description VARCHAR(500)`, `color VARCHAR(30)`
   - `min_interval_minutes INT NOT NULL DEFAULT 60`, `retention_days INT NOT NULL DEFAULT 90`
   - `status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'`, `deleted_at TIMESTAMPTZ`
   - `created_at TIMESTAMPTZ NOT NULL`, `updated_at TIMESTAMPTZ NOT NULL`
   - `UNIQUE (tenant_id, name)`
2. `monitored_items`:
   - `id UUID PRIMARY KEY`, `tenant_id UUID NOT NULL`, `group_id UUID NOT NULL REFERENCES monitoring_groups(id)`
   - `marketplace VARCHAR(50) NOT NULL`, `source_policy_id VARCHAR(100) NOT NULL`, `external_listing_id VARCHAR(100) NOT NULL`
   - `title VARCHAR(255) NOT NULL`, `initial_price NUMERIC(15,2)`, `current_price NUMERIC(15,2)`, `currency VARCHAR(10)`
   - `source_url VARCHAR(500)`, `seller_name VARCHAR(150)`, `item_condition VARCHAR(30)`
   - `status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE'`, `monitoring_interval_minutes INT DEFAULT 60`
   - `last_snapshot_at TIMESTAMPTZ`, `next_run_at TIMESTAMPTZ NOT NULL`
   - `first_snapshot_at TIMESTAMPTZ`, `deleted_at TIMESTAMPTZ`
   - `created_at TIMESTAMPTZ NOT NULL`, `updated_at TIMESTAMPTZ NOT NULL`
   - `UNIQUE (tenant_id, group_id, marketplace, external_listing_id)`
3. `monitored_item_snapshots`:
   - `id UUID PRIMARY KEY`, `tenant_id UUID NOT NULL`, `item_id UUID NOT NULL REFERENCES monitored_items(id)`
   - `group_id UUID NOT NULL REFERENCES monitoring_groups(id)`
   - `marketplace VARCHAR(50) NOT NULL`, `external_listing_id VARCHAR(100) NOT NULL`
   - `price NUMERIC(15,2) NOT NULL`, `currency VARCHAR(10)`, `shipping_cost NUMERIC(15,2)`, `shipping_status VARCHAR(30)`
   - `available_quantity INT`, `status VARCHAR(50)`
   - `provenance_type VARCHAR(50) NOT NULL DEFAULT 'OBSERVED'`
   - `collected_at TIMESTAMPTZ NOT NULL`, `observed_at TIMESTAMPTZ NOT NULL`
   - `is_effective_snapshot BOOLEAN NOT NULL DEFAULT TRUE`
   - `created_at TIMESTAMPTZ NOT NULL`
4. `monitoring_job_executions`:
   - `id UUID PRIMARY KEY`, `tenant_id UUID NOT NULL`
   - `item_id UUID`, `group_id UUID`
   - `job_type VARCHAR(50) NOT NULL`, `marketplace VARCHAR(50) NOT NULL`, `source_policy_id VARCHAR(100) NOT NULL`
   - `execution_status VARCHAR(50) NOT NULL`, `skip_reason VARCHAR(255)`
   - `executed_at TIMESTAMPTZ NOT NULL`, `duration_ms BIGINT`

### 3.2 Row-Level Security
Políticas RLS em PostgreSQL 17:
- `SET LOCAL app.current_tenant = '<tenant_id>'`
- `USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)`
- `WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid)`

---

## 4. Endpoints REST (`/api/v1/monitoring/*`)

- `POST /api/v1/monitoring/groups` — Cria grupo de monitoramento.
- `GET /api/v1/monitoring/groups` — Lista grupos com paginação e busca.
- `GET /api/v1/monitoring/groups/{id}` — Detalhe do grupo.
- `PUT /api/v1/monitoring/groups/{id}` — Atualiza dados do grupo.
- `DELETE /api/v1/monitoring/groups/{id}` — Exclusão lógica com política de retenção.
- `POST /api/v1/monitoring/groups/{groupId}/items` — Adiciona item ao grupo.
- `GET /api/v1/monitoring/groups/{groupId}/items` — Lista itens de um grupo.
- `DELETE /api/v1/monitoring/groups/{groupId}/items/{itemId}` — Remove item do grupo (marca DELETED).
- `GET /api/v1/monitoring/items/{itemId}/snapshots` — Consulta histórico de snapshots do item (não-retroativo).
- `POST /api/v1/monitoring/items/{itemId}/trigger-snapshot` — Dispara coleta manual sob validação de janelas e quotas.
- `GET /api/v1/monitoring/executions` — Consulta histórico de auditoria de jobs.

---

## 5. Estratégia de Testes

1. **Testes Unitários de Serviço:**
   - `MonitoringGroupServiceTest`: CRUD isolado, bloqueio de nome duplicado, atualização e soft-delete.
   - `MonitoredItemServiceTest`: Inclusão de item, bloqueio de item duplicado no mesmo grupo, remoção e listagem.
   - `MonitoredItemSnapshotServiceTest`: Registro de snapshot, verificação de `first_snapshot_at >= created_at`, garantia de histórico sem retroatividade forjada.
   - `MonitoringSchedulerPolicyValidatorTest`: Janela mínima respeitada, kill switch aborta com `SKIPPED_KILL_SWITCH`, rate limit esgotado gera `SKIPPED_RATE_LIMIT`, fonte proibida rejeitada com `FAILED_UNAUTHORIZED_SOURCE`.
   - `MonitoringRetentionServiceTest`: Purga de snapshots além da janela de retenção e preservação dos snapshots dentro do prazo.
2. **Testes de Controller:**
   - `ProductMonitoringControllerTest`: Validação de requisições com MockMvc, autenticação JWT, status HTTP 200/201/400/404/409.
3. **Testes de Integração com PostgreSQL 17 RLS (Testcontainers):**
   - `PostgresProductMonitoringRlsIntegrationTest`: Teste de RLS real com `seller_app` (NOBYPASSRLS) comprovando isolamento absoluto de grupos, itens, snapshots e execuções entre múltiplos tenants.
