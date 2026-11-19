# Especificação Técnica: PROMPT P014-FE - Interface de Gestão de Custos, Tabela de Taxas e Simulador

> **Status:** APPROVED  
> **Camada:** Frontend (Next.js 15 / React 19 / Tailwind CSS / TypeScript)  
> **Módulo:** `frontend/src/components/pricing/` & `frontend/src/app/api/pricing/`  
> **Data:** 19/11/2026  
> **Versão:** 1.0.0  

---

## 1. Visão Geral e Objetivos

O prompt `P014-FE` implementa a interface de usuário para o módulo de finanças e precificação determinística, compreendendo:
1. **Formulário de Custos de Produtos com Versionamento e Histórico:** Cadastro detalhado de custos diretos (aquisição, embalagem, adicionais, frete subsidiado) e percentuais (alíquota de imposto, outras despesas operacionais), validação estrita e visualização cronológica de versões anteriores.
2. **Formulário e Tabela de Regras de Taxas de Marketplaces:** Parametrização de comissões, tarifas fixas, logística e processamento com faixas de preço, vigência temporal e proveniência obrigatória (`sourceType`, `sourceReference`).
3. **Regra Anti-Alucinação Estrita (Proibição de Falsa Oficialidade):**
   - Nenhuma regra não verificada (`MANUAL_OVERRIDE`) pode ser apresentada como oficial. Exibição ostensiva de badges e alertas de não oficialidade.
   - Alerta visual no cálculo quando uma taxa utilizada for manual ou desatualizada/expirada.
   - Quando não houver taxa cadastrada (`hasMissingFees = true`), a UI nunca assume 0% de comissão nem inventa valores, exibindo banner explícito orientando o cadastro com fonte contratual.
4. **Simulador de Resolução de Taxas:** Ferramenta interativa de cálculo de taxas com detalhamento de cada item resolvido e totalizadores em `BigDecimal` formatados em BRL.

---

## 2. Contratos de Dados e API BFF

### 2.1 Rotas BFF (`frontend/src/app/api/pricing/`)
- `POST /api/pricing/costs`: Registra nova versão de custo de produto.
- `GET /api/pricing/costs/effective`: Consulta custo efetivo vigente para um produto/variante/empresa.
- `GET /api/pricing/costs/history`: Lista histórico completo e versionado de custos de um produto.
- `POST /api/pricing/fee-rules`: Registra nova regra de taxa de marketplace.
- `GET /api/pricing/fee-rules`: Lista regras cadastradas com filtro opcional por marketplace.
- `POST /api/pricing/fee-rules/resolve`: Resolve determinística de taxas ativas para um cenário de venda.

### 2.2 Modelos TypeScript (`frontend/src/types/pricing.ts`)
```typescript
export type FeeType = 'COMMISSION' | 'FIXED_FEE' | 'LOGISTICS_FEE' | 'PAYMENT_PROCESSING' | 'OTHER';
export type FeeValueType = 'PERCENTAGE' | 'FIXED_AMOUNT' | 'PERCENTAGE_PLUS_FIXED';
export type FeeSourceType = 'OFFICIAL_PORTAL' | 'CONTRACT' | 'MANUAL_OVERRIDE';
export type FeeResolutionStatus = 'SUCCESS' | 'PARTIAL' | 'MISSING';

export interface ProductCost {
  id: string;
  tenantId: string;
  productId: string;
  variantId?: string | null;
  companyId?: string | null;
  currency: string;
  unit: string;
  acquisitionCost: number;
  packagingCost: number;
  additionalCost: number;
  subsidizedShippingCost: number;
  taxRatePercent: number;
  otherExpensesPercent: number;
  effectiveFrom: string;
  effectiveTo?: string | null;
  version: number;
  sourceType: string;
  notes?: string | null;
  createdByName?: string | null;
  createdAt: string;
}

export interface FeeRule {
  id: string;
  tenantId: string;
  marketplace: string;
  feeType: FeeType;
  feeValueType: FeeValueType;
  percentageValue?: number | null;
  fixedAmount?: number | null;
  minPriceThreshold?: number | null;
  maxPriceThreshold?: number | null;
  listingType?: string | null;
  category?: string | null;
  effectiveFrom: string;
  effectiveTo?: string | null;
  version: number;
  isActive: boolean;
  sourceType: FeeSourceType;
  sourceReference: string;
  notes?: string | null;
  createdByName?: string | null;
  createdAt: string;
}

export interface ResolvedFeeItem {
  feeType: FeeType;
  ruleId?: string | null;
  name: string;
  sourceType: FeeSourceType;
  sourceReference: string;
  calculatedAmount: number;
  appliedPercentage?: number | null;
  appliedFixedAmount?: number | null;
  notes?: string | null;
}

export interface FeeResolutionResult {
  marketplace: string;
  referenceDate: string;
  status: FeeResolutionStatus;
  totalEstimatedFees: number;
  hasMissingFees: boolean;
  missingFeeTypes: FeeType[];
  resolvedFees: ResolvedFeeItem[];
  message?: string | null;
}
```

---

## 3. Critérios de Aceite e Regras de Negócio

1. **Validação de Campos:**
   - Custos: `productId` obrigatório e válido; `acquisitionCost` $> 0$; custos adicionais $\ge 0$; alíquotas percentuais entre $0$ e $100\%$; data `effectiveFrom` obrigatória.
   - Taxas: `marketplace`, `feeType`, `feeValueType` obrigatórios; `sourceReference` obrigatório e não vazio; `percentageValue` ou `fixedAmount` condizentes com `feeValueType`; se fornecidos, `minPriceThreshold <= maxPriceThreshold`.
2. **Histórico Visível:**
   - Visualização clara de versões anteriores com linha do tempo de vigência (`effectiveFrom` a `effectiveTo`) e autor.
   - Status visual evidente: "Vigente" (verde) vs "Histórico / Encerrado" (cinza neutro).
3. **Nenhuma Regra Não Verificada Apresentada como Oficial:**
   - Regras com `sourceType = 'MANUAL_OVERRIDE'` possuem badge ostensiva amarela: "⚠️ Manual (Não Oficial)" e aviso de que não possuem validação contratual.
   - Regras oficiais/contratuais possuem badge azul/verde com link ou identificador da fonte.
4. **Alertas no Cálculo / Simulador:**
   - Alerta destacado se qualquer taxa aplicada foi de origem manual.
   - Alerta destacado se uma taxa utilizada estiver com vigência expirada ou desatualizada.
   - Alerta enfático quando `hasMissingFees = true`, informando que taxas ausentes não foram assumidas como 0% e solicitando o cadastro da regra oficial.
5. **Acessibilidade e Usabilidade (WCAG 2.2 AA):**
   - Rótulos explícitos com `<label>` associados via `htmlFor`.
   - Mensagens de erro com `role="alert"` e `aria-describedby`.
   - Modos desktop (tabelas com scroll horizontal acessível) e mobile (cards compactos e touch targets $\ge 44\text{px}$).

---

## 4. Estratégia de Testes

- Testes de validação dos formulários (bloqueio de submissão inválida, mensagens de erro visíveis).
- Testes de renderização de histórico de custos com múltiplas versões e vigência.
- Testes de regras de taxas com distinção estrita entre `OFFICIAL_PORTAL`/`CONTRACT` e `MANUAL_OVERRIDE`.
- Testes do simulador:
  - Resolução completa sem taxas ausentes.
  - Alerta de uso de regra manual (`MANUAL_OVERRIDE`).
  - Alerta de ausência de taxas (`hasMissingFees`) com proibição de assumir 0%.
  - Alerta de taxa desatualizada/expirada.
- Testes de integração no `AppShell.tsx` ao selecionar o módulo de finanças (`FINANCIAL`).
