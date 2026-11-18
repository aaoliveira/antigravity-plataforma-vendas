# Especificação Técnica: P008-BE — Ingestão de Pedidos e Itens Próprios, Reconciliação, Outbox e RLS

> **Identificador da Feature:** `P008-BE` &middot; **Versão:** 1.0.0  
> **Status da Spec:** Aprovada para Implementação  
> **Requisitos do PRD:** RF-007, RF-008, RF-034, RNF-001, RNF-002, RNF-003, RNF-006, RNF-007, ADR-002, ADR-004  
> **Módulo:** `com.sellerintelligence.modules.orderingestion`

---

## 1. Contexto e Objetivos

O módulo de pedidos (`orderingestion`) é o componente transacional responsável pela ingestão, normalização, reconciliação e publicação de eventos de vendas de contas próprias do vendedor (Mercado Livre e Shopee).

Esta entrega implementa:
1. **Entidades Centrais do Domínio de Pedidos:**
   - **`Order`**: Registro mestre do pedido de venda escopado pelo tenant, contendo marketplace, conta, valores determinísticos em `BigDecimal`, status (`PENDING`, `PAID`, `SHIPPED`, `DELIVERED`, `CANCELLED`, `RETURNED`), timestamps e metadados de cancelamento/devolução.
   - **`OrderItem`**: Itens individuais do pedido vinculados ao pedido mestre, com referências opcionais ao catálogo canônico (`Product`, `ProductVariant`), título, quantidade, preço unitário e valor total.
   - **`OrderSyncCheckpoint`**: Estado de sincronização paginada e cursor por conta e marketplace, permitindo retentativa segura e retomada após falhas de rede ou quotas.
   - **`OrderOutboxEvent`**: Padrão Transactional Outbox para garantir que eventos de integração (`ORDER_SYNCED`, `ORDER_STATUS_CHANGED`, `ORDER_CANCELLED`, `ORDER_RETURNED`) sejam persistidos atomicamente com os pedidos na mesma transação.
   - **`OrderDlqRecord`**: Dead-Letter Queue estruturada no banco de dados para registrar e isolar payloads inválidos ou falhas irrecuperáveis sem abortar a sincronização do lote.
2. **Idempotência Externa e Reconciliação:**
   - A unicidade é garantida pela restrição `(tenant_id, marketplace, external_order_id)`.
   - O reprocessamento de um mesmo pedido nunca gera duplicações de registros; ele atualiza o status, valores, data de sincronização e reconcilia os itens associados.
3. **Cancelamentos e Devoluções:**
   - Atualização determinística de status para `CANCELLED` ou `RETURNED`, com gravação de data e justificativa quando presentes no canal oficial.
4. **Resiliência e DLQ:**
   - Itens ou pedidos corrompidos são encaminhados para a DLQ do tenant com contadores de tentativa e mensagem de erro sanitizada.
5. **Isolamento Multi-Tenant e RLS:**
   - Todas as tabelas são protegidas por `tenant_id NOT NULL` e políticas estritas de Row-Level Security (`ENABLE` e `FORCE ROW LEVEL SECURITY`) no PostgreSQL.
6. **API de Consulta e Suporte ao Frontend (`P008-FE`):**
   - Endpoints REST para listagem paginada com múltiplos filtros combináveis (data, status, conta, canal), detalhe de pedido e itens, visualização de checkpoints e registros de DLQ.

---

## 2. Cenários BDD / Gherkin

### Cenário 1: Ingestão Inicial e Idempotência (Sem Duplicação)
```gherkin
Dado que uma conta de vendedor possui 5 novos pedidos retornados pela API oficial
Quando o serviço de sincronização processar a página de pedidos
Então exatamente 5 registros de Order e seus respectivos OrderItems devem ser persistidos
E quando a mesma página for processada novamente na íntegra
Então nenhum novo registro de Order ou OrderItem deve ser inserido
E os registros existentes devem ter o campo synced_at atualizado
```

### Cenário 2: Reconciliação de Status (De PAID para SHIPPED/DELIVERED)
```gherkin
Dado que um pedido já existe no banco com status "PAID"
Quando a sincronização receber uma atualização do marketplace com status "SHIPPED"
Então o status do pedido deve ser atualizado para "SHIPPED"
E um evento "ORDER_STATUS_CHANGED" deve ser gerado no Transactional Outbox na mesma transação
```

### Cenário 3: Cancelamento e Devolução de Pedido
```gherkin
Dado que um pedido existe com status "PAID"
Quando o canal reportar o cancelamento do pedido com motivo "Comprador desistiu da compra"
Então o status do pedido deve ser atualizado para "CANCELLED"
E os campos cancellation_reason e cancelled_at devem ser preenchidos
E um evento "ORDER_CANCELLED" deve ser gravado no Outbox
```

### Cenário 4: Retomada de Sincronização via Checkpoint e Cursor
```gherkin
Dado que uma sincronização foi interrompida no offset 50 com cursor "cursor-token-abc"
Quando a sincronização for retomada
Então o serviço deve solicitar a próxima página a partir do cursor "cursor-token-abc"
E o checkpoint deve ser atualizado para o novo cursor e status "COMPLETED" ao final
```

### Cenário 5: Encaminhamento para DLQ em Payload Corrompido
```gherkin
Dado que um lote de 3 pedidos contém 2 pedidos válidos e 1 pedido com dados corrompidos ou inválidos
Quando a sincronização processar o lote
Então os 2 pedidos válidos devem ser persistidos com sucesso
E o pedido corrompido deve ser registrado na tabela OrderDlqRecord com erro detalhado
E o processo não deve ser abortado
```

### Cenário 6: Isolamento Multi-Tenant Estrito (PostgreSQL RLS)
```gherkin
Dado que o Tenant Alpha possui pedidos persistidos no banco
Quando uma sessão conectada como Tenant Beta consultar os pedidos com RLS ativado
Então nenhum pedido do Tenant Alpha deve ser retornado
E qualquer tentativa de acesso direto por ID de outro tenant deve retornar vazio ou 404
```
