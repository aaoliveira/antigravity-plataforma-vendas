# Especificação Técnica: P015-FE - Calculadora Interativa de Precificação, Detalhamento de Margens e Cenários

## 1. Visão Geral do Módulo
A Calculadora Interativa de Precificação e Margem de Contribuição fornece aos vendedores uma ferramenta determinística e auditável para cálculo em tempo real de:
- **Receita Líquida**: Receita bruta deduzida de descontos promocionais.
- **Custos Diretos**: Aquisição, embalagem e adicionais.
- **Deduções Fixas**: Tarifas fixas do marketplace, frete subsidiado e taxas logísticas.
- **Deduções Variáveis**: Comissões percentuais, impostos faturados e outras despesas proporcionais.
- **Margem de Contribuição**: Em moeda ($MC_{\text{valor}}$) e percentual ($MC_{\%}$).
- **Preço de Equilíbrio (Break-Even)**: Preço mínimo para não incorrer em prejuízo operacional unitário ($MC = 0$).
- **Preço Mínimo e Preço-Alvo**: Preços sugeridos para atingir margens desejadas.
- **Comparador de Cenários**: Análise comparativa entre Cenário Atual, Cenário Ponto de Equilíbrio e Cenário Meta de Rentabilidade.

---

## 2. Regras Invioláveis de Negócio e Anti-Alucinação
1. **Margem de Contribuição NUNCA Rotulada como Lucro Líquido**:
   - A interface deve exibir de forma perene e proeminente a advertência formal:
     > *"A Margem de Contribuição apura a cobertura de custos diretos e variáveis unitários. Ela NÃO representa Lucro Líquido, pois despesas fixas estruturais da empresa (aluguel, folha, administrativas) não integram este cálculo."*
2. **Avisos de Dados Ausentes ou Desatualizados**:
   - Ao resolver dados via produto cadastrado, se houver taxas ausentes no canal (`hasMissingFees = true`), a UI bloqueia a projeção oficial e exibe alerta ostensivo informando as taxas faltantes (ex: comissão não configurada).
   - Se taxas aplicadas provêm de `MANUAL_OVERRIDE` ou regras manuais (`isManualFeeApplied = true`), um aviso ostensivo sinaliza que o resultado é uma estimativa preliminar.
3. **Divisão por Zero e Inviabilidade Matemática**:
   - Receita líquida menor ou igual a zero retorna $0.00\%$ de margem sem quebra visual.
   - Soma de deduções variáveis $\ge 100\%$ indica que o preço de equilíbrio é matematicamente inalcançável, desativando o card de break-even e explicando a causa ao usuário.
4. **Precisão e Arredondamento**:
   - Valores monetários formatados em `BRL` com duas casas decimais e separador de milhar brasileiro (`pt-BR`).
   - Alíquotas formatadas com `%` explícito.

---

## 3. Arquitetura de Componentes
- `frontend/src/types/pricing.ts`: Expansão das interfaces TypeScript (`PricingCalculationRequest`, `PricingCalculationResult`, `PricingDeductionItem`, `ProductMarginSimulationRequest`).
- `frontend/src/app/api/pricing/margins/calculate/route.ts`: BFF proxy para `POST /api/v1/pricing/margins/calculate`.
- `frontend/src/app/api/pricing/margins/simulate-product/route.ts`: BFF proxy para `POST /api/v1/pricing/margins/simulate-product`.
- `frontend/src/components/pricing/PricingCalculator.tsx`: Componente interativo da calculadora com formulário reativo, grid de métricas, detalhamento sanfonado de parcelas, comparador de cenários e alertas de conformidade.
- `frontend/src/components/pricing/PricingAndCostsView.tsx`: Inclusão da aba "Calculadora & Cenários".

---

## 4. Critérios de Aceite
1. **Valores Batem com a API**:
   - Cálculo via formulário reflete fielmente as saídas determinísticas do motor backend `PricingEngineService`.
2. **Acessibilidade por Teclado (WCAG 2.2 AA)**:
   - Foco lógico sequencial em todos os campos.
   - Mensagens de erro associadas via `role="alert"` e `aria-describedby`.
   - Elementos interativos com altura mínima $\ge 44\text{px}$.
3. **Validação Clara**:
   - Validação inline de preço de venda $> 0$, custos $\ge 0$ e alíquotas entre 0% e 100%.
   - Indicação visual de campos com valores inconsistentes.
4. **Layout Mobile Utilizável**:
   - Layout responsivo adaptado para 360px a 1440px sem corte ou overflow horizontal prejudicial.
   - Cards e colunas empilháveis em telas compactas.
