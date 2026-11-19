# Especificação Técnica P018-BE: Inventário de Métricas Oficiais por Marketplace, Catálogo Documentado e Ingestão Comprovada

> **Versão:** 1.0.0  
> **Status:** IN_PROGRESS  
> **Data:** 19/11/2026  
> **Autor:** Tech Lead / Staff Software Engineer  
> **Módulo:** `com.sellerintelligence.modules.listinganalytics`  
> **Prompt:** P018-BE

---

## 1. Visão Geral e Objetivos de Negócio

O objetivo do **P018-BE** é inventariar, catalogar e estruturar a ingestão oficial de métricas de desempenho para contas conectadas e anúncios próprios na Seller Intelligence Platform.

Em conformidade rigorosa com o **PRD (MVP-08, RF-040, RF-041, RNF-014)** e as **Regras Anti-Alucinação**, a plataforma rejeita expressamente preencher lacunas de métricas com médias sintéticas, estimativas silenciosas ou fórmulas com denominadores arbitrários. Cada métrica possui escopo, janela temporal, granularidade, limites e definição formal documentados.

### 1.1 Inventário Oficial por Marketplace

| Marketplace | Métrica | Escopo | Granularidade | Endpoint Oficial | Limites / Janela | Suportado Oficialmente? |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **MERCADO LIVRE** | `VISITS` (Visitas) | `LISTING`, `ACCOUNT` | `DAILY`, `PERIOD_TOTAL` | `/items/{id}/visits/time_window` | 100 req/min, até 365 dias | **SIM (Oficial)** |
| **MERCADO LIVRE** | `ORDERS_COUNT` (Pedidos) | `LISTING`, `ACCOUNT` | `DAILY`, `PERIOD_TOTAL` | `/orders/search` | 20 req/s, histórico completo | **SIM (Oficial)** |
| **MERCADO LIVRE** | `UNITS_SOLD` (Unidades) | `LISTING`, `ACCOUNT` | `DAILY`, `PERIOD_TOTAL` | `/orders/search` | 20 req/s, histórico completo | **SIM (Oficial)** |
| **MERCADO LIVRE** | `REVENUE` (Receita) | `LISTING`, `ACCOUNT` | `DAILY`, `PERIOD_TOTAL` | `/orders/search` | 20 req/s, histórico completo | **SIM (Oficial)** |
| **MERCADO LIVRE** | `IMPRESSIONS` (Impressões) | `LISTING` | `DAILY`, `PERIOD_TOTAL` | `/advertising/product_ads` | Requer escopo Mercado Ads | **NÃO no Orgânico** (`UNSUPPORTED_BY_CHANNEL`) / **SIM no Ads** |
| **MERCADO LIVRE** | `CLICKS` (Cliques) | `LISTING` | `DAILY`, `PERIOD_TOTAL` | `/advertising/product_ads` | Requer escopo Mercado Ads | **NÃO no Orgânico** (`UNSUPPORTED_BY_CHANNEL`) / **SIM no Ads** |
| **MERCADO LIVRE** | `CTR` (Taxa de Clique) | `LISTING` | `DAILY`, `PERIOD_TOTAL` | Derivada: $\frac{\text{clicks}}{\text{impressions}}$ | Somente se impressões $> 0$ | **NÃO no Orgânico** / **SIM no Ads** |
| **MERCADO LIVRE** | `CONVERSION_RATE_ORDERS` | `LISTING`, `ACCOUNT` | `DAILY`, `PERIOD_TOTAL` | Derivada: $\frac{\text{orders}}{\text{visits}}$ | Somente se visitas $> 0$ | **SIM (Derivada Comprovada)** |
| **MERCADO LIVRE** | `CONVERSION_RATE_UNITS` | `LISTING`, `ACCOUNT` | `DAILY`, `PERIOD_TOTAL` | Derivada: $\frac{\text{units}}{\text{visits}}$ | Somente se visitas $> 0$ | **SIM (Derivada Comprovada)** |
| **SHOPEE** | `PAGE_VIEWS` (Visualizações) | `LISTING`, `ACCOUNT` | `DAILY`, `PERIOD_TOTAL` | `v2.analytics.get_item_performance` | 10 req/s, janela 15-90 dias | **SIM (Oficial)** |
| **SHOPEE** | `UNIQUE_VISITORS` (Visitantes) | `LISTING`, `ACCOUNT` | `DAILY`, `PERIOD_TOTAL` | `v2.analytics.get_item_performance` | 10 req/s, janela 15-90 dias | **SIM (Oficial)** |
| **SHOPEE** | `ORDERS_COUNT` (Pedidos) | `LISTING`, `ACCOUNT` | `DAILY`, `PERIOD_TOTAL` | `v2.order.get_order_list` | 10 req/s, histórico completo | **SIM (Oficial)** |
| **SHOPEE** | `UNITS_SOLD` (Unidades) | `LISTING`, `ACCOUNT` | `DAILY`, `PERIOD_TOTAL` | `v2.order.get_order_list` | 10 req/s, histórico completo | **SIM (Oficial)** |
| **SHOPEE** | `REVENUE` (GMV) | `LISTING`, `ACCOUNT` | `DAILY`, `PERIOD_TOTAL` | `v2.order.get_order_list` | 10 req/s, histórico completo | **SIM (Oficial)** |
| **SHOPEE** | `IMPRESSIONS` (Impressões) | `LISTING` | `DAILY`, `PERIOD_TOTAL` | `v2.ads.get_item_performance` | Requer escopo Shopee Ads | **NÃO no Orgânico** / **SIM no Ads** |
| **SHOPEE** | `CLICKS` (Cliques) | `LISTING` | `DAILY`, `PERIOD_TOTAL` | `v2.ads.get_item_performance` | Requer escopo Shopee Ads | **NÃO no Orgânico** / **SIM no Ads** |
| **SHOPEE** | `CTR` (Taxa de Clique) | `LISTING` | `DAILY`, `PERIOD_TOTAL` | Derivada: $\frac{\text{clicks}}{\text{impressions}}$ | Somente se impressões $> 0$ | **NÃO no Orgânico** / **SIM no Ads** |
| **SHOPEE** | `CONVERSION_RATE_ORDERS` | `LISTING`, `ACCOUNT` | `DAILY`, `PERIOD_TOTAL` | Derivada: $\frac{\text{orders}}{\text{unique\_visitors}}$ | Somente se visitantes $> 0$ | **SIM (Derivada Comprovada)** |
| **SHOPEE** | `CONVERSION_RATE_UNITS` | `LISTING`, `ACCOUNT` | `DAILY`, `PERIOD_TOTAL` | Derivada: $\frac{\text{units}}{\text{unique\_visitors}}$ | Somente se visitantes $> 0$ | **SIM (Derivada Comprovada)** |
| **TODOS OS CANAIS** | Métricas de Terceiros / Concorrentes | Concorrentes | Qualquer | Proibido scraping | Não suportado por APIs oficiais | **NÃO (`UNSUPPORTED_BY_CHANNEL`)** |

---

## 2. Regras Anti-Alucinação e Fórmulas de Cálculo

### 2.1 Regra Anti-Estimativas Silenciosas
> [!IMPORTANT]
> **NÃO ESTIMAR OU ADIVINHAR DADOS AUSENTES:**
> 1. Quando uma métrica não for fornecida pela API oficial (ex: impressões em busca orgânica do Mercado Livre), a métrica deve ser categoricamente registrada como `availability_status = UNSUPPORTED_BY_CHANNEL`, com valor monetário/numérico `NULL` e motivo explícito.
> 2. É expressamente proibido calcular CTR sem dados reais de impressões e cliques comprovados.
> 3. É expressamente proibido calcular taxa de conversão sem dados de visitas ou visitantes únicos do anúncio.
> 4. Quando o denominador for igual a 0: a taxa resultante não causa divisão por zero e não é assumida como 100% ou nula sem explicação; ela é classificada como `availability_status = INSUFFICIENT_DATA`, valor `0.0000` e denominador `0.0000` formalmente registrado.

### 2.2 Fórmulas Determinísticas de Cálculo
Todas as operações de divisão matemática usam `BigDecimal` com escala mínima 6 e arredondamento financeiro bancário `RoundingMode.HALF_EVEN`:

1. **Taxa de Cliques (CTR):**
   $$\text{CTR} = \frac{\text{clicks}}{\text{impressions}}$$
   - Condição: `impressions > 0`.
   - Se `impressions` for nula: status `UNAVAILABLE`, valor `null`.
   - Se `impressions == 0`: status `INSUFFICIENT_DATA`, valor `0.0000`.
   - Se `clicks > impressions`: status `UNAVAILABLE` com motivo de inconsistência nos dados de origem.

2. **Taxa de Conversão por Pedidos:**
   $$\text{Conversion Rate (Orders)} = \frac{\text{orders\_count}}{\text{visits}}$$
   - Condição: `visits > 0`.
   - Se `visits` for nula: status `UNAVAILABLE`, valor `null`.
   - Se `visits == 0`: status `INSUFFICIENT_DATA`, valor `0.0000`.

3. **Taxa de Conversão por Unidades:**
   $$\text{Conversion Rate (Units)} = \frac{\text{units\_sold}}{\text{visits}}$$
   - Condição: `visits > 0`.

---

## 3. Modelo de Dados e Esquema de Tabelas

### 3.1 Tabela `connected_account_metrics`
Armazena a série temporal de métricas de contas e anúncios sincronizadas ou derivadas.

| Campo | Tipo | Nulo? | Descrição |
| :--- | :--- | :--- | :--- |
| `id` | UUID | Não | Identificador primário da métrica |
| `tenant_id` | UUID | Não | Identificador do tenant (RLS mandatória) |
| `seller_account_id` | UUID | Não | ID da conta conectada (`seller_accounts.id`) |
| `marketplace` | VARCHAR(50) | Não | Identificador do canal (`MERCADO_LIVRE`, `SHOPEE`) |
| `listing_id` | UUID | Sim | ID do anúncio no catálogo interno se escopo for LISTING |
| `external_listing_id` | VARCHAR(100) | Sim | ID do anúncio na plataforma externa |
| `metric_scope` | VARCHAR(30) | Não | Escopo: `ACCOUNT` ou `LISTING` |
| `granularity` | VARCHAR(30) | Não | Granularidade: `DAILY` ou `PERIOD_TOTAL` |
| `window_start` | TIMESTAMP WITH TIME ZONE | Não | Início da janela temporal da métrica |
| `window_end` | TIMESTAMP WITH TIME ZONE | Não | Fim da janela temporal da métrica |
| `metric_key` | VARCHAR(100) | Não | Código da métrica (ex: `VISITS`, `CTR`, `ORDERS_COUNT`) |
| `metric_value` | NUMERIC(15, 4) | Sim | Valor apurado (NULL se indisponível) |
| `metric_unit` | VARCHAR(30) | Não | Unidade (`COUNT`, `CURRENCY`, `PERCENTAGE`, `RATIO`) |
| `provenance` | VARCHAR(30) | Não | Origem: `OFFICIAL_API`, `DERIVED_FORMULA`, `UNAVAILABLE` |
| `formula_definition` | VARCHAR(500) | Sim | Fórmula descritiva documentada |
| `denominator_metric` | VARCHAR(100) | Sim | Métrica utilizada no denominador (ex: `IMPRESSIONS`) |
| `denominator_value` | NUMERIC(15, 4) | Sim | Valor numérico do denominador |
| `numerator_metric` | VARCHAR(100) | Sim | Métrica utilizada no numerador (ex: `CLICKS`) |
| `numerator_value` | NUMERIC(15, 4) | Sim | Valor numérico do numerador |
| `availability_status` | VARCHAR(30) | Não | `AVAILABLE`, `UNAVAILABLE`, `INSUFFICIENT_DATA`, `UNSUPPORTED_BY_CHANNEL` |
| `unavailability_reason` | VARCHAR(500) | Sim | Justificativa documental da indisponibilidade |
| `source_endpoint` | VARCHAR(255) | Não | Endpoint ou processo gerador da informação |
| `ingested_at` | TIMESTAMP WITH TIME ZONE | Não | Timestamp de persistência no sistema |
| `metadata_json` | TEXT | Sim | Metadados complementares auditáveis em JSON |

**Restrição de Unicidade / Idempotência:**
`(tenant_id, seller_account_id, marketplace, COALESCE(external_listing_id, '__ACCOUNT__'), metric_key, granularity, window_start, window_end)`

---

## 4. Endpoints da API REST

Base path: `/api/v1/metrics`

1. **`GET /api/v1/metrics/catalog`**
   - Retorna o catálogo oficial de todas as métricas inventariadas por marketplace com escopo, janela, granularidade, rate limits e fórmulas de cálculo.
   - Filtros opcionais: `marketplace`, `scope`.

2. **`POST /api/v1/metrics/ingest`**
   - Executa a ingestão idempotente de métricas para uma conta conectada em um intervalo temporal (`sellerAccountId`, `marketplace`, `startDate`, `endDate`, `granularity`).
   - Ingesta métricas comprovadas e deriva fórmulas com validação estrita.

3. **`GET /api/v1/metrics`**
   - Lista métricas persistidas com filtros por `sellerAccountId`, `marketplace`, `externalListingId`, `scope`, `metricKey`, `status`, `startDate`, `endDate`.

4. **`GET /api/v1/metrics/summary`**
   - Resumo consolidado de métricas (totais de visitas, pedidos, conversão média, disponibilidade e pendências).

---

## 5. Critérios de Aceite (Gherkin)

```gherkin
Funcionalidade: Inventário e Ingestão de Métricas Oficiais por Marketplace

  Cenário: Consulta do catálogo formal de métricas por marketplace
    Dado que a aplicação possui o catálogo de métricas oficial configurado
    Quando for requisitado o catálogo de métricas do Mercado Livre
    Então deve listar a métrica VISITS com endpoint oficial e rate limit documentados
    E a métrica IMPRESSIONS deve ser identificada como não suportada em tráfego orgânico
    E a fórmula de conversão de pedidos deve documentar orders_count / visits como denominador

  Cenário: Ingestão de métricas com CTR sem impressões fornecidas
    Dado uma conta conectada do Mercado Livre em busca orgânica
    Quando a ingestão for executada para o período de 30 dias
    Então a métrica VISITS deve ser persistida com status AVAILABLE
    E a métrica CTR deve ser gravada com status UNSUPPORTED_BY_CHANNEL e valor nulo
    E nenhuma estimativa silenciosa de CTR deve ser gerada

  Cenário: Cálculo de taxa de conversão com visitas zeradas
    Dado um anúncio que registrou 0 visitas e 0 pedidos no período
    Quando o cálculo de conversão for acionado
    Então o valor da taxa de conversão deve ser gravado como 0.0000
    E o status de disponibilidade deve ser INSUFFICIENT_DATA
    E nenhuma divisão por zero deve ocorrer

  Cenário: Reingestão idempotente do mesmo período
    Dado que uma janela diária de métricas já foi persistida
    Quando a mesma janela for reingerida com dados atualizados da API oficial
    Então os registros existentes devem ser atualizados atomicamente
    E a contagem total de registros para a chave única não deve sofrer duplicação

  Cenário: Isolamento multi-tenant hermético no PostgreSQL 17 (RLS)
    Dado que o Tenant A possui métricas persistidas para sua conta
    Quando o Tenant B executar consultas na tabela connected_account_metrics
    Então nenhum registro do Tenant A deve ser retornado pelo banco de dados
```
