# Especificação Técnica P017-BE: Fatos Financeiros por Pedido/Item, Reconciliação e Trilha de Cálculo

> **Versão:** 1.0.0  
> **Status:** APPROVED  
> **Data:** 19/11/2026  
> **Autor:** Tech Lead / Staff Software Engineer  
> **Módulo:** `com.sellerintelligence.modules.financialanalytics`  
> **Prompt:** P017-BE

---

## 1. Visão Geral e Objetivos de Negócio

O objetivo do **P017-BE** é estabelecer a infraestrutura canônica de **Fatos Financeiros** por pedido e por item da Seller Intelligence Platform. O sistema consolida as receitas, deduções de canal, custos diretos de produto, alíquotas de impostos declaradas e calcula deterministamente a **Margem de Contribuição** em valor monetário e percentual.

Além da geração estimada baseada em regras de comissão e custos de cadastro, o módulo provê **Reconciliação com Origem Externa** (extratos de liquidação/repasse do marketplace), detectando divergências centesimais entre o previsto e o debitado pela plataforma parceira.

### 1.1 Regra Anti-Alucinação Estrita: Margem de Contribuição vs. Lucro Líquido
> [!CAUTION]
> **PROIBIÇÃO DE ALUCINAÇÃO DE LUCRO LÍQUIDO:**
> Enquanto os custos fixos estruturais e despesas operacionais da empresa (aluguel, salários, pró-labore, infraestrutura, contabilidade, amortizações) não estiverem completamente modelados no sistema, **é terminantemente proibido declarar, rotular ou sugerir Lucro Líquido** em qualquer entidade, DTO, resposta de API, log ou interface.
> A métrica calculada é **estritamente Margem de Contribuição**. Todas as entidades e DTOs devem carregar o `antiHallucinationDisclaimer`.

---

## 2. Modelo de Dados e Esquema de Tabelas

### 2.1 Tabela `order_financial_facts`
Armazena a consolidação dos fatos financeiros no nível de cabeçalho do pedido.

| Campo | Tipo | Nulo? | Descrição |
| :--- | :--- | :--- | :--- |
| `id` | UUID | Não | Identificador primário do fato financeiro |
| `tenant_id` | UUID | Não | Identificador do tenant (RLS) |
| `order_id` | UUID | Não | ID do pedido interno (`orders.id`) |
| `seller_account_id` | UUID | Não | ID da conta do vendedor |
| `company_id` | UUID | Sim | ID da empresa vinculada à conta no momento da apuração |
| `marketplace` | VARCHAR(50) | Não | Canal (ex: MERCADO_LIVRE, SHOPEE) |
| `external_order_id` | VARCHAR(100) | Não | ID do pedido na plataforma externa |
| `order_status` | VARCHAR(50) | Não | Status do pedido (PAID, CANCELLED, RETURNED, etc.) |
| `order_date` | TIMESTAMP | Não | Data de criação original do pedido |
| `gross_revenue` | NUMERIC(15, 4) | Não | Soma dos preços brutos dos itens |
| `discount_amount` | NUMERIC(15, 4) | Não | Descontos concedidos |
| `net_revenue` | NUMERIC(15, 4) | Não | Receita Líquida (`gross_revenue - discount_amount`) |
| `cancellation_amount`| NUMERIC(15, 4) | Não | Valor cancelado/estornado |
| `return_amount` | NUMERIC(15, 4) | Não | Valor devolvido |
| `marketplace_fee` | NUMERIC(15, 4) | Não | Comissão percentual cobrada pelo marketplace |
| `fixed_fee` | NUMERIC(15, 4) | Não | Tarifas fixas do marketplace |
| `shipping_cost` | NUMERIC(15, 4) | Não | Custo de frete debitado ao seller |
| `product_cost` | NUMERIC(15, 4) | Não | Custo de aquisição total dos produtos (CMV) |
| `packaging_cost` | NUMERIC(15, 4) | Não | Custos de embalagem |
| `additional_cost` | NUMERIC(15, 4) | Não | Custos adicionais diretos |
| `tax_amount` | NUMERIC(15, 4) | Não | Impostos calculados sobre alíquota informada |
| `other_expenses_amount`| NUMERIC(15, 4)| Não | Outras despesas variáveis informadas |
| `total_deductions` | NUMERIC(15, 4) | Não | Soma de todas as deduções |
| `contribution_margin_amount` | NUMERIC(15, 4) | Não | `net_revenue - total_deductions` |
| `contribution_margin_ratio` | NUMERIC(8, 4) | Não | Margem em % (`margin / net_revenue * 100`) |
| `currency` | VARCHAR(10) | Não | Moeda ISO (padrão: BRL) |
| `reconciliation_status` | VARCHAR(30) | Não | `ESTIMATED`, `RECONCILED`, `DIVERGENT`, `PARTIAL` |
| `fee_provenance` | VARCHAR(30) | Não | `CONFIRMED`, `ESTIMATED`, `MISSING` |
| `shipping_provenance`| VARCHAR(30) | Não | `CONFIRMED`, `ESTIMATED`, `MISSING` |
| `cost_provenance` | VARCHAR(30) | Não | `USER_INFORMED`, `MISSING` |
| `tax_provenance` | VARCHAR(30) | Não | `USER_INFORMED`, `MISSING` |
| `has_missing_data` | BOOLEAN | Não | Flag indicando se algum custo/taxa essencial faltou |
| `missing_data_reasons`| TEXT | Sim | JSON array com motivos da ausência de dados |
| `calculation_version`| VARCHAR(20) | Não | Versão da fórmula (ex: "v1.0.0") |
| `calculation_trace_json` | TEXT | Não | Trilha de auditoria e regras em JSON |
| `settlement_external_id` | VARCHAR(100) | Sim | ID da liquidação/repasse no extrato |
| `settlement_date` | TIMESTAMP | Sim | Data do repasse financeiro |
| `settlement_notes` | TEXT | Sim | Observações da reconciliação |
| `reconciled_at` | TIMESTAMP | Sim | Timestamp da conciliação |
| `created_at` | TIMESTAMP | Não | Timestamp de criação |
| `updated_at` | TIMESTAMP | Não | Timestamp de última atualização |

### 2.2 Tabela `order_item_financial_facts`
Armazena os fatos financeiros de cada item do pedido, garantindo rastreabilidade unitária e rateio proporcional determinístico.

| Campo | Tipo | Nulo? | Descrição |
| :--- | :--- | :--- | :--- |
| `id` | UUID | Não | Identificador primário |
| `tenant_id` | UUID | Não | Identificador do tenant (RLS) |
| `order_fact_id` | UUID | Não | Chave estrangeira para `order_financial_facts.id` |
| `order_id` | UUID | Não | ID do pedido (`orders.id`) |
| `order_item_id` | UUID | Não | ID do item de pedido (`order_items.id`) |
| `product_id` | UUID | Sim | ID do produto de catálogo |
| `variant_id` | UUID | Sim | ID da variante do produto |
| `sku` | VARCHAR(100) | Sim | SKU do item |
| `title` | VARCHAR(255) | Não | Título do anúncio/item |
| `quantity` | INTEGER | Não | Quantidade vendida |
| `unit_price` | NUMERIC(15, 4) | Não | Preço unitário de venda |
| `gross_revenue` | NUMERIC(15, 4) | Não | `unit_price * quantity` |
| `discount_amount` | NUMERIC(15, 4) | Não | Desconto rateado/atribuído ao item |
| `net_revenue` | NUMERIC(15, 4) | Não | `gross_revenue - discount_amount` |
| `cancellation_amount`| NUMERIC(15, 4) | Não | Valor cancelado |
| `return_amount` | NUMERIC(15, 4) | Não | Valor devolvido |
| `marketplace_fee` | NUMERIC(15, 4) | Não | Comissão proporcional do marketplace |
| `fixed_fee` | NUMERIC(15, 4) | Não | Tarifa fixa do item |
| `shipping_cost` | NUMERIC(15, 4) | Não | Frete rateado proporcionalmente à receita |
| `unit_product_cost` | NUMERIC(15, 4) | Não | Custo unitário de aquisição |
| `product_cost` | NUMERIC(15, 4) | Não | `unit_product_cost * quantity` |
| `packaging_cost` | NUMERIC(15, 4) | Não | Custo de embalagem unitário $\times$ quantidade |
| `additional_cost` | NUMERIC(15, 4) | Não | Custo adicional unitário $\times$ quantidade |
| `tax_rate_percent` | NUMERIC(8, 4) | Não | Alíquota de imposto informada |
| `tax_amount` | NUMERIC(15, 4) | Não | `net_revenue * tax_rate_percent / 100` |
| `other_expenses_amount`| NUMERIC(15, 4)| Não | Outras despesas informadas |
| `total_deductions` | NUMERIC(15, 4) | Não | Soma das deduções do item |
| `contribution_margin_amount` | NUMERIC(15, 4) | Não | `net_revenue - total_deductions` |
| `contribution_margin_ratio` | NUMERIC(8, 4) | Não | Margem em % do item |
| `cost_provenance` | VARCHAR(30) | Não | `USER_INFORMED`, `MISSING` |
| `has_missing_data` | BOOLEAN | Não | Flag indicando ausência de custo/taxa |
| `missing_data_reasons`| TEXT | Sim | JSON array com motivos |
| `created_at` | TIMESTAMP | Não | Data de criação |
| `updated_at` | TIMESTAMP | Não | Data de atualização |

---

## 3. Regras Matemáticas e Determinismo

### 3.1 Precisão e Arredondamento
- Todos os cálculos monetários utilizam `BigDecimal` com escala 4 (`NUMERIC(15, 4)`) e `RoundingMode.HALF_EVEN` (arredondamento bancário IEEE 754).
- Percentuais de margem utilizam escala 4 (`NUMERIC(8, 4)`).
- **Tratamento de divisão por zero:** Se `net_revenue <= 0`, a margem percentual (`contribution_margin_ratio`) é fixada deterministamente em `0.0000%`.

### 3.2 Soma por Item e Pedido Reconciliável
A soma de todos os itens do pedido para cada parcela financeira bate exatamente com os totais gravados em `order_financial_facts`:
$$\sum \text{gross\_revenue}_{\text{item}} = \text{gross\_revenue}_{\text{order}}$$
$$\sum \text{shipping\_cost}_{\text{item}} = \text{shipping\_cost}_{\text{order}}$$
$$\sum \text{total\_deductions}_{\text{item}} = \text{total\_deductions}_{\text{order}}$$
$$\sum \text{contribution\_margin}_{\text{item}} = \text{contribution\_margin}_{\text{order}}$$

Para parcelas informadas a nível de pedido (como o frete total), o rateio entre os itens é feito proporcionalmente à receita de cada item:
$$\text{rateio}_i = \text{shipping\_order} \times \left( \frac{\text{net\_revenue}_i}{\text{net\_revenue}_{\text{order}}} \right)$$
O resíduo de centavos decorrente do arredondamento é adicionado ao último item da coleção, garantindo soma exata sem perda ou ganho de centavos.

### 3.3 Reprocessamento Idempotente
- Restrição de unicidade em banco:
  - `uk_order_financial_facts_tenant_order UNIQUE (tenant_id, order_id)`
  - `uk_order_item_financial_facts_tenant_item UNIQUE (tenant_id, order_item_id)`
- Execuções subsequentes do motor realizam `UPDATE` determinístico dos registros sem duplicar linhas.

---

## 4. Reconciliação com Origem Externa

Ao receber extrato financeiro via comando `POST /api/v1/financial-facts/orders/{orderId}/reconcile`:
1. Valida existência do fato financeiro gerado para o pedido.
2. Atualiza as tarifas e fretes com os valores reais confirmados pelo marketplace.
3. Compara os valores confirmados com os valores previamente estimados:
   - Se a diferença absoluta entre estimado e confirmado for $\le 0.01$ (1 centavo), o status transita para `RECONCILED`.
   - Se houver divergência superior a 1 centavo em comissão, tarifa ou frete, o status transita para `DIVERGENT` e a discrepância é registrada na trilha de auditoria.
4. Recalcula as deduções totais e a margem de contribuição com base nos valores confirmados.
5. Atualiza `fee_provenance = CONFIRMED`, `shipping_provenance = CONFIRMED`, `reconciled_at = now()`, e armazena `settlement_external_id`.

---

## 5. Trilha de Cálculo Persistida (`calculation_trace_json`)

Estrutura JSON armazenada em cada apuração:
```json
{
  "calculatedAt": "2026-11-19T11:45:00Z",
  "engineVersion": "v1.0.0",
  "antiHallucinationDisclaimer": "Este demonstrativo apura estritamente a Margem de Contribuição...",
  "companyId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "reconciliationStatus": "ESTIMATED",
  "items": [
    {
      "orderItemId": "...",
      "sku": "SKU-123",
      "appliedCostRecordId": "...",
      "appliedCostVersion": 1,
      "appliedFeeRules": ["FEE-RULE-001"],
      "missingDataWarnings": []
    }
  ],
  "warnings": []
}
```

---

## 6. Critérios de Aceite e Testes (Gherkin)

```gherkin
Funcionalidade: Fatos Financeiros por Pedido e por Item com Reconciliação

  Cenário: Apuração idempotente e reconciliabilidade matemática exata
    Dado um pedido próprio com 2 itens e frete total de R$ 20,00
    Quando o serviço de fatos financeiros apurar o pedido
    Então os registros de fatos de pedido e itens devem ser persistidos
    E a soma dos itens de cada parcela deve ser rigorosamente igual ao total do pedido
    E uma segunda execução com os mesmos parâmetros deve atualizar os registros sem duplicar

  Cenário: Sinalização de dados ausentes sem inventar valores
    Dado um pedido com um item cujo produto não possui cadastro em product_costs
    Quando o serviço de fatos financeiros for acionado
    Então o fato deve ser gerado com has_missing_data = true
    E missing_data_reasons deve conter "PRODUCT_COST_MISSING"
    E a margem de contribuição não deve alucinar lucro líquido

  Cenário: Reconciliação externa com detecção de divergência
    Dado um pedido com comissão estimada de R$ 15,00
    Quando for enviada uma liquidação externa com comissão de R$ 18,50
    Então o status de reconciliação deve ser atualizado para "DIVERGENT"
    E fee_provenance deve ser atualizado para "CONFIRMED"
    E a margem de contribuição deve ser recalculada com os valores reais
```
