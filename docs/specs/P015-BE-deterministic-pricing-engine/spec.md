# Especificação Técnica: PROMPT P015-BE - Motor Determinístico de Preço, Margem de Contribuição e Ponto de Equilíbrio

> **Status:** APPROVED  
> **Camada:** Backend (Java 25 LTS / Spring Boot 3.5.x / Spring Modulith)  
> **Módulo:** `com.sellerintelligence.modules.pricing`  
> **Data:** 19/11/2026  
> **Versão:** 1.0.0  

---

## 1. Visão Geral e Princípios Fundamentais

O prompt `P015-BE` implementa o motor central de precificação e rentabilidade determinística da plataforma.

### Diretrizes Invioláveis:
1. **Proibição Irrestrita de LLM:** Cálculos monetários, deduções, margens e preços de equilíbrio são 100% determinísticos via código Java. Nenhuma LLM ou modelo probabilístico é acionado para calcular valores monetários.
2. **Precisão Universal em `BigDecimal`:** Todos os cálculos operam exclusivamente em `BigDecimal` com escala interna 4 (`scale = 4`) e arredondamento explícito `RoundingMode.HALF_UP`. Resultados monetários finais são formatados com escala 2.
3. **Divisão por Zero Tratada:** Qualquer operação com risco de denominador zero ou negativo (ex.: alíquotas percentuais totais $\ge 100\%$, receita líquida $\le 0$) é estritamente interceptada e tratada de forma fail-safe, retornando flags e mensagens explicativas em vez de lançar `ArithmeticException`.
4. **Nomenclatura Financeira Rigorosa (Anti-Alucinação):**
   - O resultado gerado é expressa e formalmente denominado **Margem de Contribuição** (unitária em R$ e %).
   - A Margem de Contribuição **NUNCA é rotulada como Lucro Líquido**, uma vez que despesas fixas estruturais do lojista (aluguel, folha, energia, pro-labore) não pertencem ao nível de anúncio/unidade vendida.
5. **Rastreabilidade e Detalhamento de Parcelas:** Cada cálculo retorna a decomposição exata de cada parcela deduzida (CMV, frete, embalagem, comissão, imposto, tarifa fixa), a versão da fórmula (`v1.0.0`) e a lista de premissas financeiras.

---

## 2. Definições Formais e Fórmulas Matemáticas

### 2.1 Receita
- **Receita Bruta ($RB$):**
  $$RB = \text{Preço de Venda Bruto do Anúncio}$$
- **Receita Líquida de Desconto ($RL$):**
  $$RL = RB - \text{Desconto Promocional Concedido pelo Vendedor}$$

### 2.2 Custos Diretos do Produto ($CD$ / CMV Unitário)
$$CD = \text{acquisitionCost} + \text{packagingCost} + \text{additionalCost}$$

### 2.3 Deduções Variáveis Percentuais ($DV_{\%}$ e $DV_{\text{valor}}$)
$$DV_{\%} = \text{channelCommissionPercent} + \text{taxRatePercent} + \text{otherVariableExpensesPercent}$$
$$DV_{\text{valor}} = RL \times \frac{DV_{\%}}{100}$$

### 2.4 Deduções Fixas da Venda ($DF$)
$$DF = \text{channelFixedFee} + \text{subsidizedShippingCost} + \text{logisticsFee} + \text{otherFixedDeductions}$$

### 2.5 Total de Deduções sobre a Venda ($D_{\text{venda}}$)
$$D_{\text{venda}} = DV_{\text{valor}} + DF$$

### 2.6 Margem de Contribuição em Valor ($MC_{\text{valor}}$)
$$MC_{\text{valor}} = RL - D_{\text{venda}} - CD = RL - DV_{\text{valor}} - DF - CD$$

### 2.7 Margem de Contribuição Percentual ($MC_{\%}$)
$$MC_{\%} = \begin{cases} \frac{MC_{\text{valor}}}{RL} \times 100, & \text{se } RL > 0 \\ 0.00\%, & \text{se } RL \le 0 \end{cases}$$

### 2.8 Preço de Equilíbrio (Break-Even Price, $P_{\text{eq}}$)
Preço no qual $MC_{\text{valor}} = 0$:
$$P_{\text{eq}} = \frac{CD + DF}{1 - \frac{DV_{\%}}{100}}$$
- Se $DV_{\%} \ge 100\%$, $1 - \frac{DV_{\%}}{100} \le 0$. O ponto de equilíbrio é matematicamente inatingível. O motor define `breakEvenAttainable = false` e `breakEvenPrice = null`.

### 2.9 Preço Mínimo ($P_{\text{min}}$)
Preço para atingir uma margem mínima $M_{\text{min}\%}$:
$$P_{\text{min}} = \frac{CD + DF}{1 - \frac{DV_{\%} + M_{\text{min}\%}}{100}}$$
- Se $DV_{\%} + M_{\text{min}\%} \ge 100\%$, `minPriceAttainable = false` e `minPrice = null`.

### 2.10 Preço-Alvo ($P_{\text{target}}$)
- Por Margem Percentual Alvo ($M_{\text{target}\%}$):
  $$P_{\text{target}} = \frac{CD + DF}{1 - \frac{DV_{\%} + M_{\text{target}\%}}{100}}$$
- Por Valor Monetário Alvo ($M_{\text{target R\$}}$):
  $$P_{\text{target}} = \frac{CD + DF + M_{\text{target R\$}}}{1 - \frac{DV_{\%}}{100}}$$

---

## 3. Contratos de Dados (DTOs e Modelos)

### 3.1 `PricingCalculationRequest`
- `salePrice` (`BigDecimal`, obrigatório, $> 0$)
- `promotionalDiscount` (`BigDecimal`, opcional, $\ge 0$)
- `acquisitionCost` (`BigDecimal`, obrigatório, $\ge 0$)
- `packagingCost` (`BigDecimal`, opcional, $\ge 0$)
- `additionalCost` (`BigDecimal`, opcional, $\ge 0$)
- `subsidizedShippingCost` (`BigDecimal`, opcional, $\ge 0$)
- `channelCommissionPercent` (`BigDecimal`, opcional, $\ge 0$)
- `channelFixedFee` (`BigDecimal`, opcional, $\ge 0$)
- `logisticsFee` (`BigDecimal`, opcional, $\ge 0$)
- `taxRatePercent` (`BigDecimal`, opcional, $\ge 0$)
- `otherVariableExpensesPercent` (`BigDecimal`, opcional, $\ge 0$)
- `otherFixedDeductions` (`BigDecimal`, opcional, $\ge 0$)
- `targetMarginPercent` (`BigDecimal`, opcional)
- `targetMarginAmount` (`BigDecimal`, opcional)
- `minMarginPercent` (`BigDecimal`, opcional)
- `currency` (`String`, padrão "BRL")

### 3.2 `PricingCalculationResult`
- `grossSellingPrice`, `netSellingPrice`, `promotionalDiscount`
- `totalDirectCosts` ($CD$)
- `totalVariableDeductions` ($DV_{\text{valor}}$)
- `totalFixedDeductions` ($DF$)
- `totalDeductions` ($D_{\text{venda}} + CD$)
- `contributionMarginAmount` ($MC_{\text{valor}}$)
- `contributionMarginPercent` ($MC_{\%}$)
- `breakEvenPrice`, `breakEvenAttainable`
- `minPrice`, `minPriceAttainable`
- `targetPrice`, `targetPriceAttainable`
- `currency`, `formulaVersion`, `calculatedAt`
- `deductionItems` (`List<PricingDeductionItem>`)
- `assumptions` (`List<String>`)
- `warnings` (`List<String>`)

---

## 4. Estratégia de Testes

1. **Testes de Referência Aprovados:** Cenários reais de venda no Mercado Livre e Shopee (ex: Produto R$ 120, Custo R$ 50, Comissão 16%, Taxa Fixa R$ 6, Imposto 8%, etc.).
2. **Divisão por Zero e Fronteiras:**
   - Preço zero ou negativo.
   - Soma de alíquotas percentuais $\ge 100\%$ ($DV_{\%} \ge 100\%$).
   - Margem mínima/alvo que torne o denominador $\le 0$.
3. **Não Rotulado como Lucro Líquido:** Verificação de que nenhum campo, DTO ou mensagem classifica a margem de contribuição como lucro líquido.
4. **Precisão e Arredondamento:** Validação com dízimas periódicas (ex: $1/3$, $16.6667\%$) comprovando estabilidade e reprodutibilidade com `HALF_UP`.
