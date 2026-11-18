# Especificação Técnica — P011-BE: Comparação de Listings Equivalentes, Variação Temporal e Histórico Não-Inventado

## 1. Contexto & Objetivos

A tomada de decisão estratégica de precificação e posicionamento competitivo exige que o lojista compare seus anúncios ou produtos monitorados com anúncios concorrentes equivalentes no mercado. No entanto, ferramentas convencionais frequentemente induzem o lojista ao erro ao:
1. Comparar produtos estruturalmente incompatíveis (ex: capa de celular com o aparelho telefônico, item 110V com 220V, produto usado com novo) sem avisos claros;
2. Calcular variações de preço comparando observações de momentos temporais díspares (ex: comparar o preço de Black Friday de meses atrás com o preço padrão de hoje);
3. Inferir ou "adivinhar" o preço final com frete gratuito ou presumido quando a API oficial não informa o custo de frete;
4. Criar retroatividade inventada ("fake history"), gerando gráficos de preços passados inexistentes para simular monitoramento prolongado.

O objetivo do **P011-BE** é fornecer um motor determinístico, auditável e explicável de **Comparação de Listings Equivalentes**, **Variação Temporal de Preço** e **Histórico Legítimo de Preços**, em conformidade inegociável com as regras de integridade da Seller Intelligence Platform.

---

## 2. Critérios de Aceite

| ID | Critério | Descrição & Condição de Sucesso |
|---|---|---|
| **AC-01** | **Score e Critérios Explicáveis** | O cálculo de similaridade/equivalência decompõe sub-scores ponderados para GTIN (35%), Marca (25%), Modelo (20%), Atributos Técnicos (10%) e Título (10%). O resultado expõe a pontuação geral (`overallScore`), o limiar aplicado e uma explicação textual detalhada (`explanation`). |
| **AC-02** | **Sinalização Obrigatória de Itens Incompatíveis** | Se dois anúncios apresentarem divergência crítica (GTINs distintos válidos, marcas comerciais diferentes, condições novo vs usado/recondicionado, ou atributos críticos como voltagem 110V vs 220V), o motor marca `isCompatible = false`, status `INCOMPATIBLE`, lista os motivos específicos em `incompatibilityReasons` e anexa `warningMessage`. Itens incompatíveis NUNCA são considerados equivalentes (`isEquivalent = false`). |
| **AC-03** | **Cálculo de Variação de Preço em Janela Temporal Comparável** | Variação absoluta (`priceDifference`) e percentual (`priceVariationPercentage`) só são calculadas se as observações pertencerem a uma janela temporal comparável ($|T_{\text{target}} - T_{\text{base}}| \le \text{maxComparableDays}$, padrão 30 dias). Se o intervalo exceder a janela, o cálculo é omitido (`priceVariationComparable = false`, valores nulos) com aviso formal de incomparabilidade temporal. |
| **AC-04** | **Proteção contra Inferência de Frete Desconhecido** | Se o custo de frete for desconhecido/nulo (`shippingCost == null`), o sistema NÃO calcula `finalPriceWithShipping` (permanece `null`), mantendo estritamente o `listingPrice` separado e reportando `shippingStatus = UNKNOWN` e aviso formal de que o frete não pode ser inferido. |
| **AC-05** | **Histórico sem Retroatividade Inventada** | O histórico de preços de um anúncio retorna estritamente os pontos legítimos registrados no banco (`observed_at`, `price`, `collected_at`). Se o anúncio tiver apenas 1 ponto histórico, retorna 1 ponto. É terminantemente proibido fabricar pontos históricos retroativos ("D-30", "D-7") inexistentes. |
| **AC-06** | **Cálculo Determinístico em BigDecimal** | Toda aritmética monetária e percentual utiliza `BigDecimal` com escala explícita (preços com 4 casas, percentuais com 4 casas) e `RoundingMode.HALF_UP`. Zero tipos de ponto flutuante binário (`double`/`float`) e zero delegação a LLM. |
| **AC-07** | **Isolamento Multi-Tenant com RLS** | As tabelas `listing_comparisons` e `listing_price_history` são isoladas por `tenant_id` com PostgreSQL Row-Level Security forçada (`FORCE ROW LEVEL SECURITY`), validadas via Testcontainers. |

---

## 3. Modelo de Domínio e Contratos

### 3.1 Entidades de Comparação

- `ComparableListingItem`:
  - `listingId`: String (ID do anúncio próprio ou observação externa)
  - `title`: String
  - `brand`: String
  - `model`: String
  - `gtin`: String
  - `category`: String
  - `condition`: ItemCondition (`NEW`, `USED`, `REFURBISHED`)
  - `attributes`: Map<String, String> (ex: `{"voltage": "110V", "storage": "128GB"}`)
  - `price`: BigDecimal
  - `shippingCost`: BigDecimal (nulo se desconhecido)
  - `marketplace`: String
  - `sellerName`: String
  - `observedAt`: Instant
  - `sourceUrl`: String
  - `isOwnListing`: boolean

- `EquivalenceScoreBreakdown`:
  - `gtinScore`: BigDecimal (0.0000 a 1.0000)
  - `brandScore`: BigDecimal (0.0000 a 1.0000)
  - `modelScore`: BigDecimal (0.0000 a 1.0000)
  - `titleScore`: BigDecimal (0.0000 a 1.0000)
  - `attributesScore`: BigDecimal (0.0000 a 1.0000)
  - `overallScore`: BigDecimal (0.0000 a 1.0000)
  - `thresholdApplied`: BigDecimal (padrão 0.7500)
  - `isEquivalent`: boolean
  - `isCompatible`: boolean
  - `compatibilityStatus`: CompatibilityStatus (`COMPATIBLE`, `INCOMPATIBLE`, `BORDERLINE`)
  - `incompatibilityReasons`: List<String>
  - `warningMessage`: String
  - `explanation`: String

- `PriceComparisonResult`:
  - `basePrice`: BigDecimal
  - `targetPrice`: BigDecimal
  - `priceDifference`: BigDecimal (nulo se não comparável)
  - `priceVariationPercentage`: BigDecimal (nulo se não comparável)
  - `isTemporalWindowComparable`: boolean
  - `timeDeltaDays`: Long
  - `temporalStatus`: TemporalComparabilityStatus (`COMPARABLE`, `NON_COMPARABLE`)
  - `temporalWarningMessage`: String
  - `baseShippingCost`: BigDecimal (nulo se desconhecido)
  - `baseShippingStatus`: ShippingStatus (`KNOWN`, `UNKNOWN`)
  - `targetShippingCost`: BigDecimal (nulo se desconhecido)
  - `targetShippingStatus`: ShippingStatus (`KNOWN`, `UNKNOWN`)
  - `baseFinalPriceWithShipping`: BigDecimal (nulo se frete desconhecido)
  - `targetFinalPriceWithShipping`: BigDecimal (nulo se frete desconhecido)
  - `shippingNotice`: String

---

## 4. Banco de Dados & Migrações

- `V9__listing_equivalence_and_comparison.sql`:
  - Cria tabela `listing_comparisons` para auditoria de comparações executadas/salvas.
  - Cria tabela `listing_price_history` para registro pontual e cronológico de preços legítimos.
- `V9_1__postgresql_rls_listing_comparison.sql`:
  - Habilita `ROW LEVEL SECURITY` e `FORCE ROW LEVEL SECURITY` em ambas as tabelas para o tenant `app.current_tenant` com permissões restritas a `seller_app`.

---

## 5. Endpoints REST da API

1. `POST /api/v1/market-comparison/compare`:
   - Executa comparação determinística detalhada entre item base e item alvo.
   - Retorna scores discriminados, status de compatibilidade, variação temporal de preços e status de frete.
2. `POST /api/v1/market-comparison/find-equivalents`:
   - Dado um anúncio base e lista de candidatos (ou busca no catálogo e observações do tenant), calcula equivalências ordenadas por score.
3. `GET /api/v1/market-comparison/price-history`:
   - Consulta o histórico real de preços de um anúncio (`marketplace`, `externalListingId`).
   - Retorna exclusivamente os pontos coletados, sem pontos fictícios.
4. `POST /api/v1/market-comparison/price-history`:
   - Registra um ponto de preço legítimo com timestamp `observedAt` e fonte comprovada.
5. `GET /api/v1/market-comparison/saved`:
   - Lista comparações persistidas pelo tenant ativo.
6. `POST /api/v1/market-comparison/saved`:
   - Salva uma comparação para auditoria e monitoramento contínuo.
