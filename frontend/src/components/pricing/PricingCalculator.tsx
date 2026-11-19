'use client';

import React, { useState, useId } from 'react';
import {
  PricingCalculationRequest,
  PricingCalculationResult,
  ProductMarginSimulationRequest,
} from '@/types/pricing';

interface ScenarioComparisonItem {
  title: string;
  price: number;
  netRevenue: number;
  totalDeductions: number;
  marginAmount: number;
  marginPercent: number;
  badge: string;
  badgeColor: string;
}

export function PricingCalculator() {
  const [calculationMode, setCalculationMode] = useState<'MANUAL' | 'PRODUCT'>('MANUAL');

  // Campos do Formulário Manual
  const [grossPrice, setGrossPrice] = useState<string>('100.00');
  const [discount, setDiscount] = useState<string>('0.00');
  const [acquisitionCost, setAcquisitionCost] = useState<string>('30.00');
  const [packagingCost, setPackagingCost] = useState<string>('2.50');
  const [additionalCost, setAdditionalCost] = useState<string>('1.50');
  const [subsidizedShipping, setSubsidizedShipping] = useState<string>('5.00');
  const [commissionPercent, setCommissionPercent] = useState<string>('16.00');
  const [fixedFee, setFixedFee] = useState<string>('6.00');
  const [logisticsFee, setLogisticsFee] = useState<string>('0.00');
  const [taxRatePercent, setTaxRatePercent] = useState<string>('8.00');
  const [otherVariablePercent, setOtherVariablePercent] = useState<string>('2.00');
  const [otherFixedDeductions, setOtherFixedDeductions] = useState<string>('0.00');
  const [targetMarginPercent, setTargetMarginPercent] = useState<string>('20.00');
  const [minMarginPercent, setMinMarginPercent] = useState<string>('10.00');

  // Campos do Modo Produto Cadastrado
  const [productId, setProductId] = useState<string>('');
  const [marketplace, setMarketplace] = useState<string>('MERCADO_LIVRE');
  const [listingType, setListingType] = useState<string>('gold_pro');
  const [category, setCategory] = useState<string>('Eletrônicos');

  // Estados de Execução e Resultados
  const [loading, setLoading] = useState(false);
  const [calculationResult, setCalculationResult] = useState<PricingCalculationResult | null>(null);
  const [formErrors, setFormErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState<string | null>(null);

  // IDs para acessibilidade
  const grossPriceId = useId();
  const discountId = useId();
  const acquisitionCostId = useId();
  const packagingCostId = useId();
  const additionalCostId = useId();
  const subsidizedShippingId = useId();
  const commissionPercentId = useId();
  const fixedFeeId = useId();
  const logisticsFeeId = useId();
  const taxRatePercentId = useId();
  const otherVariablePercentId = useId();
  const targetMarginPercentId = useId();
  const minMarginPercentId = useId();
  const productIdId = useId();
  const marketplaceId = useId();
  const listingTypeId = useId();
  const categoryId = useId();

  // Validação dos Campos
  const validate = (): boolean => {
    const errors: Record<string, string> = {};

    const numGross = parseFloat(grossPrice);
    if (isNaN(numGross) || numGross <= 0) {
      errors.grossPrice = 'Preço de venda deve ser um número maior que zero.';
    }

    const numDiscount = parseFloat(discount || '0');
    if (isNaN(numDiscount) || numDiscount < 0) {
      errors.discount = 'Desconto não pode ser negativo.';
    } else if (numDiscount >= numGross) {
      errors.discount = 'Desconto não pode ser maior ou igual ao preço de venda.';
    }

    if (calculationMode === 'MANUAL') {
      const numAcquisition = parseFloat(acquisitionCost);
      if (isNaN(numAcquisition) || numAcquisition < 0) {
        errors.acquisitionCost = 'Custo de aquisição não pode ser negativo.';
      }

      const numCommission = parseFloat(commissionPercent || '0');
      if (isNaN(numCommission) || numCommission < 0 || numCommission > 100) {
        errors.commissionPercent = 'Comissão deve estar entre 0% e 100%.';
      }

      const numTax = parseFloat(taxRatePercent || '0');
      if (isNaN(numTax) || numTax < 0 || numTax > 100) {
        errors.taxRatePercent = 'Alíquota de imposto deve estar entre 0% e 100%.';
      }

      const numOtherVar = parseFloat(otherVariablePercent || '0');
      if (isNaN(numOtherVar) || numOtherVar < 0 || numOtherVar > 100) {
        errors.otherVariablePercent = 'Outras despesas variáveis devem estar entre 0% e 100%.';
      }

      const numTargetMargin = parseFloat(targetMarginPercent || '0');
      if (isNaN(numTargetMargin) || numTargetMargin < 0 || numTargetMargin >= 100) {
        errors.targetMarginPercent = 'Margem alvo deve estar entre 0% e 99.99%.';
      }
    } else {
      if (!productId.trim()) {
        errors.productId = 'Identificador do produto (UUID) é obrigatório.';
      } else {
        const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
        if (!uuidRegex.test(productId.trim())) {
          errors.productId = 'Formato de UUID inválido (ex: 123e4567-e89b-12d3-a456-426614174000).';
        }
      }
      if (!marketplace.trim()) {
        errors.marketplace = 'Marketplace é obrigatório.';
      }
    }

    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  // Submissão do Cálculo
  const handleCalculate = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!validate()) return;

    setLoading(true);
    setServerError(null);

    try {
      if (calculationMode === 'MANUAL') {
        const payload: PricingCalculationRequest = {
          grossSellingPrice: parseFloat(grossPrice),
          promotionalDiscount: parseFloat(discount || '0'),
          acquisitionCost: parseFloat(acquisitionCost || '0'),
          packagingCost: parseFloat(packagingCost || '0'),
          additionalCost: parseFloat(additionalCost || '0'),
          subsidizedShippingCost: parseFloat(subsidizedShipping || '0'),
          channelCommissionPercent: parseFloat(commissionPercent || '0'),
          channelFixedFee: parseFloat(fixedFee || '0'),
          logisticsFee: parseFloat(logisticsFee || '0'),
          taxRatePercent: parseFloat(taxRatePercent || '0'),
          otherVariableExpensesPercent: parseFloat(otherVariablePercent || '0'),
          otherFixedDeductions: parseFloat(otherFixedDeductions || '0'),
          targetMarginPercent: parseFloat(targetMarginPercent || '0'),
          minMarginPercent: parseFloat(minMarginPercent || '0'),
          currency: 'BRL',
        };

        const res = await fetch('/api/pricing/margins/calculate', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload),
        });

        if (!res.ok) {
          const err = await res.json().catch(() => ({}));
          throw new Error(err.message || 'Erro ao calcular margem determinística.');
        }

        const data: PricingCalculationResult = await res.json();
        setCalculationResult(data);
      } else {
        const payload: ProductMarginSimulationRequest = {
          productId: productId.trim(),
          marketplace,
          listingType: listingType.trim() || undefined,
          category: category.trim() || undefined,
          grossSellingPrice: parseFloat(grossPrice),
          promotionalDiscount: parseFloat(discount || '0'),
          targetMarginPercent: targetMarginPercent ? parseFloat(targetMarginPercent) : undefined,
          minMarginPercent: minMarginPercent ? parseFloat(minMarginPercent) : undefined,
        };

        const res = await fetch('/api/pricing/margins/simulate-product', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload),
        });

        if (!res.ok) {
          const err = await res.json().catch(() => ({}));
          throw new Error(err.message || 'Erro ao simular margem do produto.');
        }

        const data: PricingCalculationResult = await res.json();
        setCalculationResult(data);
      }
    } catch (err: unknown) {
      setServerError(err instanceof Error ? err.message : 'Falha na comunicação com o servidor.');
    } finally {
      setLoading(false);
    }
  };

  // Formatação Monetária e Percentual
  const formatMoney = (val: number | null | undefined): string => {
    if (val === null || val === undefined) return 'N/A';
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val);
  };

  const formatPercent = (val: number | null | undefined): string => {
    if (val === null || val === undefined) return 'N/A';
    return `${val.toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}%`;
  };

  // Geração de Cenários Comparativos Baseados no Resultado
  const generateScenarios = (result: PricingCalculationResult): ScenarioComparisonItem[] => {
    const list: ScenarioComparisonItem[] = [];

    // Cenário 1: Atual
    list.push({
      title: 'Cenário Atual',
      price: result.grossSellingPrice,
      netRevenue: result.netSellingPrice,
      totalDeductions: result.totalDeductions,
      marginAmount: result.contributionMarginAmount,
      marginPercent: result.contributionMarginPercent,
      badge: result.contributionMarginAmount >= 0 ? 'Lucrativo' : 'Prejuízo Operacional',
      badgeColor:
        result.contributionMarginAmount >= 0
          ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300'
          : 'bg-rose-100 text-rose-800 dark:bg-rose-950/60 dark:text-rose-300',
    });

    // Cenário 2: Ponto de Equilíbrio (Break-Even)
    if (result.isBreakEvenAttainable && result.breakEvenPrice !== null) {
      const beNet = result.breakEvenPrice - result.promotionalDiscount;
      list.push({
        title: 'Ponto de Equilíbrio (Zero a Zero)',
        price: result.breakEvenPrice,
        netRevenue: beNet,
        totalDeductions: beNet, // No break-even, deduções = receita líquida
        marginAmount: 0.0,
        marginPercent: 0.0,
        badge: 'Margem Zero (Break-Even)',
        badgeColor: 'bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300',
      });
    }

    // Cenário 3: Preço-Alvo / Meta Desejada
    if (result.isTargetPriceAttainable && result.targetPrice !== null) {
      const targetNet = result.targetPrice - result.promotionalDiscount;
      const targetMarginAmt = targetNet * ((parseFloat(targetMarginPercent) || 0) / 100);
      list.push({
        title: `Meta de Rentabilidade (${targetMarginPercent}%)`,
        price: result.targetPrice,
        netRevenue: targetNet,
        totalDeductions: targetNet - targetMarginAmt,
        marginAmount: targetMarginAmt,
        marginPercent: parseFloat(targetMarginPercent) || 0,
        badge: 'Meta Alvo',
        badgeColor: 'bg-blue-100 text-blue-800 dark:bg-blue-950/60 dark:text-blue-300',
      });
    }

    return list;
  };

  return (
    <div className="space-y-6">
      {/* Disclaimer Anti-Alucinação Perene */}
      <div
        role="region"
        aria-label="Aviso Regulatório de Margem de Contribuição"
        className="p-4 rounded-xl border border-amber-200 dark:border-amber-900/60 bg-amber-50/70 dark:bg-amber-950/30 flex items-start gap-3"
      >
        <span className="text-xl shrink-0" aria-hidden="true">
          ⚖️
        </span>
        <div className="text-xs text-amber-900 dark:text-amber-200 space-y-1">
          <p className="font-bold">
            Atenção Anti-Alucinação: Margem de Contribuição NÃO é Lucro Líquido
          </p>
          <p className="leading-relaxed">
            A Margem de Contribuição demonstra quanto cada unidade vendida contribui para cobrir as despesas
            fixas da empresa e gerar lucro. Ela considera apenas custos diretos e deduções variáveis unitárias.
            Custos fixos operacionais da estrutura (aluguel, pró-labore, folha e administrativos) não pertencem ao nível
            unitário do produto.
          </p>
        </div>
      </div>

      {/* Seletor de Modo de Cálculo */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm">
        <div>
          <h2 className="text-sm font-bold text-slate-900 dark:text-white">
            Calculadora Interativa de Precificação & Rentabilidade
          </h2>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            Selecione entre parâmetros manuais dinâmicos ou cálculo automático via produto cadastrado no catálogo.
          </p>
        </div>

        <div className="flex items-center gap-2" role="group" aria-label="Modo de operação da calculadora">
          <button
            type="button"
            onClick={() => {
              setCalculationMode('MANUAL');
              setCalculationResult(null);
              setServerError(null);
            }}
            className={`px-3 py-2 text-xs font-semibold rounded-lg transition-colors min-h-[44px] flex items-center gap-1.5 ${
              calculationMode === 'MANUAL'
                ? 'bg-blue-600 text-white shadow-sm'
                : 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700'
            }`}
          >
            <span>🎛️</span>
            <span>Entrada Manual</span>
          </button>

          <button
            type="button"
            onClick={() => {
              setCalculationMode('PRODUCT');
              setCalculationResult(null);
              setServerError(null);
            }}
            className={`px-3 py-2 text-xs font-semibold rounded-lg transition-colors min-h-[44px] flex items-center gap-1.5 ${
              calculationMode === 'PRODUCT'
                ? 'bg-blue-600 text-white shadow-sm'
                : 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700'
            }`}
          >
            <span>📦</span>
            <span>Por Produto Cadastrado</span>
          </button>
        </div>
      </div>

      {/* Formulário de Inputs */}
      <form
        onSubmit={handleCalculate}
        className="p-6 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm space-y-6"
      >
        <div className="border-b border-slate-200 dark:border-slate-800 pb-3">
          <h3 className="text-sm font-bold text-slate-900 dark:text-white">
            {calculationMode === 'MANUAL'
              ? '1. Parâmetros de Venda, Custos e Deduções Unitárias'
              : '1. Parâmetros do Produto e Canal de Venda'}
          </h3>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            Insira os valores vigentes. Todas as operações são processadas com precisão BigDecimal determinística.
          </p>
        </div>

        {calculationMode === 'PRODUCT' ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div>
              <label htmlFor={productIdId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                UUID do Produto Mestre *
              </label>
              <input
                id={productIdId}
                type="text"
                placeholder="Ex: b86765b0-c6c3-4655-815a-f8c12ede88ce"
                value={productId}
                onChange={(e) => setProductId(e.target.value)}
                aria-describedby={formErrors.productId ? `${productIdId}-err` : undefined}
                className={`w-full text-xs px-3 py-2.5 rounded-lg border bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 min-h-[44px] ${
                  formErrors.productId
                    ? 'border-rose-500 focus:ring-rose-500'
                    : 'border-slate-300 dark:border-slate-700 focus:ring-blue-500'
                }`}
              />
              {formErrors.productId && (
                <p id={`${productIdId}-err`} role="alert" className="text-xs text-rose-600 dark:text-rose-400 mt-1">
                  {formErrors.productId}
                </p>
              )}
            </div>

            <div>
              <label htmlFor={marketplaceId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Marketplace *
              </label>
              <select
                id={marketplaceId}
                value={marketplace}
                onChange={(e) => setMarketplace(e.target.value)}
                className="w-full text-xs px-3 py-2.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
              >
                <option value="MERCADO_LIVRE">Mercado Livre</option>
                <option value="SHOPEE">Shopee</option>
                <option value="AMAZON">Amazon</option>
              </select>
            </div>

            <div>
              <label htmlFor={listingTypeId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Modalidade de Anúncio
              </label>
              <input
                id={listingTypeId}
                type="text"
                value={listingType}
                onChange={(e) => setListingType(e.target.value)}
                placeholder="Ex: gold_pro ou CLASSIC"
                className="w-full text-xs px-3 py-2.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
              />
            </div>

            <div>
              <label htmlFor={categoryId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Categoria
              </label>
              <input
                id={categoryId}
                type="text"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                placeholder="Ex: Eletrônicos"
                className="w-full text-xs px-3 py-2.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
              />
            </div>
          </div>
        ) : null}

        {/* Linha 1: Preço e Desconto */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div>
            <label htmlFor={grossPriceId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Preço de Venda Bruto (R$) *
            </label>
            <input
              id={grossPriceId}
              type="number"
              step="0.01"
              value={grossPrice}
              onChange={(e) => setGrossPrice(e.target.value)}
              aria-describedby={formErrors.grossPrice ? `${grossPriceId}-err` : undefined}
              className={`w-full text-xs px-3 py-2.5 rounded-lg border bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 min-h-[44px] ${
                formErrors.grossPrice
                  ? 'border-rose-500 focus:ring-rose-500'
                  : 'border-slate-300 dark:border-slate-700 focus:ring-blue-500'
              }`}
            />
            {formErrors.grossPrice && (
              <p id={`${grossPriceId}-err`} role="alert" className="text-xs text-rose-600 dark:text-rose-400 mt-1">
                {formErrors.grossPrice}
              </p>
            )}
          </div>

          <div>
            <label htmlFor={discountId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Desconto Promocional (R$)
            </label>
            <input
              id={discountId}
              type="number"
              step="0.01"
              value={discount}
              onChange={(e) => setDiscount(e.target.value)}
              aria-describedby={formErrors.discount ? `${discountId}-err` : undefined}
              className={`w-full text-xs px-3 py-2.5 rounded-lg border bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 min-h-[44px] ${
                formErrors.discount
                  ? 'border-rose-500 focus:ring-rose-500'
                  : 'border-slate-300 dark:border-slate-700 focus:ring-blue-500'
              }`}
            />
            {formErrors.discount && (
              <p id={`${discountId}-err`} role="alert" className="text-xs text-rose-600 dark:text-rose-400 mt-1">
                {formErrors.discount}
              </p>
            )}
          </div>

          <div>
            <label htmlFor={targetMarginPercentId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Margem Alvo Desejada (%)
            </label>
            <input
              id={targetMarginPercentId}
              type="number"
              step="0.1"
              value={targetMarginPercent}
              onChange={(e) => setTargetMarginPercent(e.target.value)}
              aria-describedby={formErrors.targetMarginPercent ? `${targetMarginPercentId}-err` : undefined}
              className="w-full text-xs px-3 py-2.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
            />
            {formErrors.targetMarginPercent && (
              <p id={`${targetMarginPercentId}-err`} role="alert" className="text-xs text-rose-600 dark:text-rose-400 mt-1">
                {formErrors.targetMarginPercent}
              </p>
            )}
          </div>

          <div>
            <label htmlFor={minMarginPercentId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Margem Mínima Tolerada (%)
            </label>
            <input
              id={minMarginPercentId}
              type="number"
              step="0.1"
              value={minMarginPercent}
              onChange={(e) => setMinMarginPercent(e.target.value)}
              className="w-full text-xs px-3 py-2.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
            />
          </div>
        </div>

        {/* Linha 2 e 3: Custos Diretos e Deduções no Modo Manual */}
        {calculationMode === 'MANUAL' && (
          <div className="space-y-4 pt-2 border-t border-slate-100 dark:border-slate-800">
            <h4 className="text-xs font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider">
              Custos Diretos & Taxas do Canal
            </h4>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <div>
                <label htmlFor={acquisitionCostId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Custo de Aquisição (R$) *
                </label>
                <input
                  id={acquisitionCostId}
                  type="number"
                  step="0.01"
                  value={acquisitionCost}
                  onChange={(e) => setAcquisitionCost(e.target.value)}
                  className="w-full text-xs px-3 py-2.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
                />
              </div>

              <div>
                <label htmlFor={packagingCostId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Custo de Embalagem (R$)
                </label>
                <input
                  id={packagingCostId}
                  type="number"
                  step="0.01"
                  value={packagingCost}
                  onChange={(e) => setPackagingCost(e.target.value)}
                  className="w-full text-xs px-3 py-2.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
                />
              </div>

              <div>
                <label htmlFor={additionalCostId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Custos Adicionais (R$)
                </label>
                <input
                  id={additionalCostId}
                  type="number"
                  step="0.01"
                  value={additionalCost}
                  onChange={(e) => setAdditionalCost(e.target.value)}
                  className="w-full text-xs px-3 py-2.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
                />
              </div>

              <div>
                <label htmlFor={subsidizedShippingId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Frete Subsidiado (R$)
                </label>
                <input
                  id={subsidizedShippingId}
                  type="number"
                  step="0.01"
                  value={subsidizedShipping}
                  onChange={(e) => setSubsidizedShipping(e.target.value)}
                  className="w-full text-xs px-3 py-2.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
                />
              </div>

              <div>
                <label htmlFor={commissionPercentId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Comissão do Marketplace (%)
                </label>
                <input
                  id={commissionPercentId}
                  type="number"
                  step="0.1"
                  value={commissionPercent}
                  onChange={(e) => setCommissionPercent(e.target.value)}
                  className="w-full text-xs px-3 py-2.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
                />
              </div>

              <div>
                <label htmlFor={fixedFeeId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Tarifa Fixa do Canal (R$)
                </label>
                <input
                  id={fixedFeeId}
                  type="number"
                  step="0.01"
                  value={fixedFee}
                  onChange={(e) => setFixedFee(e.target.value)}
                  className="w-full text-xs px-3 py-2.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
                />
              </div>

              <div>
                <label htmlFor={taxRatePercentId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Alíquota de Imposto (%)
                </label>
                <input
                  id={taxRatePercentId}
                  type="number"
                  step="0.1"
                  value={taxRatePercent}
                  onChange={(e) => setTaxRatePercent(e.target.value)}
                  className="w-full text-xs px-3 py-2.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
                />
              </div>

              <div>
                <label htmlFor={otherVariablePercentId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Outras Desp. Variáveis (%)
                </label>
                <input
                  id={otherVariablePercentId}
                  type="number"
                  step="0.1"
                  value={otherVariablePercent}
                  onChange={(e) => setOtherVariablePercent(e.target.value)}
                  className="w-full text-xs px-3 py-2.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
                />
              </div>
            </div>
          </div>
        )}

        {/* Mensagem de Erro de Servidor */}
        {serverError && (
          <div role="alert" className="p-3 rounded-lg bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900 text-xs text-rose-700 dark:text-rose-300 flex items-center gap-2">
            <span>❌</span>
            <span>{serverError}</span>
          </div>
        )}

        {/* Botão de Ação */}
        <div className="flex justify-end pt-2">
          <button
            type="submit"
            disabled={loading}
            className="px-6 py-2.5 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white text-xs font-bold rounded-lg shadow-sm transition-all min-h-[44px] flex items-center gap-2"
          >
            {loading ? (
              <>
                <span className="animate-spin inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full" />
                <span>Processando Cálculo Determinístico...</span>
              </>
            ) : (
              <>
                <span>⚡</span>
                <span>Calcular Preço & Margem de Contribuição</span>
              </>
            )}
          </button>
        </div>
      </form>

      {/* Resultados Detalhados da Precificação */}
      {calculationResult && (
        <div className="space-y-6">
          {/* Alertas Retornados pelo Motor (Taxas Manuais / Ausência / Break-Even Inviável) */}
          {calculationResult.warnings && calculationResult.warnings.length > 0 && (
            <div
              role="alert"
              className="p-4 rounded-xl border border-amber-300 dark:border-amber-800 bg-amber-50 dark:bg-amber-950/40 text-xs text-amber-900 dark:text-amber-200 space-y-2"
            >
              <div className="font-bold flex items-center gap-2">
                <span>⚠️</span>
                <span>Alertas de Conformidade e Fontes de Dados:</span>
              </div>
              <ul className="list-disc list-inside space-y-1">
                {calculationResult.warnings.map((warn, idx) => (
                  <li key={idx}>{warn}</li>
                ))}
              </ul>
            </div>
          )}

          {/* Cards de Métricas Principais */}
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* Card 1: Receita Líquida */}
            <div className="p-4 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm">
              <span className="text-xs font-semibold text-slate-500 dark:text-slate-400">
                Receita Líquida Unitária
              </span>
              <p className="text-xl font-bold text-slate-900 dark:text-white mt-1">
                {formatMoney(calculationResult.netSellingPrice)}
              </p>
              <div className="text-[11px] text-slate-500 dark:text-slate-400 mt-1 flex justify-between">
                <span>Bruto: {formatMoney(calculationResult.grossSellingPrice)}</span>
                {calculationResult.promotionalDiscount > 0 && (
                  <span className="text-rose-600 dark:text-rose-400">
                    Desc: -{formatMoney(calculationResult.promotionalDiscount)}
                  </span>
                )}
              </div>
            </div>

            {/* Card 2: Deduções Totais */}
            <div className="p-4 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm">
              <span className="text-xs font-semibold text-slate-500 dark:text-slate-400">
                Deduções Totais (Custos + Taxas)
              </span>
              <p className="text-xl font-bold text-slate-900 dark:text-white mt-1">
                {formatMoney(calculationResult.totalDeductions)}
              </p>
              <div className="text-[11px] text-slate-500 dark:text-slate-400 mt-1 flex justify-between">
                <span>Diretos: {formatMoney(calculationResult.totalDirectCosts)}</span>
                <span>Variáveis: {formatMoney(calculationResult.totalVariableDeductions)}</span>
              </div>
            </div>

            {/* Card 3: Margem de Contribuição */}
            <div
              className={`p-4 rounded-xl border shadow-sm ${
                calculationResult.contributionMarginAmount >= 0
                  ? 'bg-emerald-50/50 dark:bg-emerald-950/20 border-emerald-200 dark:border-emerald-900/50'
                  : 'bg-rose-50/50 dark:bg-rose-950/20 border-rose-200 dark:border-rose-900/50'
              }`}
            >
              <span className="text-xs font-semibold text-slate-600 dark:text-slate-300">
                Margem de Contribuição
              </span>
              <p
                className={`text-xl font-bold mt-1 ${
                  calculationResult.contributionMarginAmount >= 0
                    ? 'text-emerald-700 dark:text-emerald-300'
                    : 'text-rose-700 dark:text-rose-300'
                }`}
              >
                {formatMoney(calculationResult.contributionMarginAmount)}
              </p>
              <p
                className={`text-xs font-semibold mt-1 ${
                  calculationResult.contributionMarginAmount >= 0
                    ? 'text-emerald-600 dark:text-emerald-400'
                    : 'text-rose-600 dark:text-rose-400'
                }`}
              >
                {formatPercent(calculationResult.contributionMarginPercent)} da Receita Líquida
              </p>
            </div>

            {/* Card 4: Preço de Equilíbrio (Break-Even) */}
            <div className="p-4 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm">
              <span className="text-xs font-semibold text-slate-500 dark:text-slate-400">
                Preço de Equilíbrio (Break-Even)
              </span>
              <p className="text-xl font-bold text-slate-900 dark:text-white mt-1">
                {calculationResult.isBreakEvenAttainable
                  ? formatMoney(calculationResult.breakEvenPrice)
                  : 'Inatingível (Taxas >= 100%)'}
              </p>
              <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-1">
                {calculationResult.isBreakEvenAttainable
                  ? 'Preço mínimo para zerar a margem (MC = 0)'
                  : 'Deduções variáveis inviabilizam o equilíbrio'}
              </p>
            </div>
          </div>

          {/* Comparador de Cenários de Precificação */}
          <div className="p-6 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-2">
                  <span>📊</span>
                  <span>Simulação Comparativa de Cenários</span>
                </h3>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                  Compare o impacto no fluxo de margens entre o preço praticado, o equilíbrio operacional e a meta alvo.
                </p>
              </div>
              <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400">
                Fórmula: {calculationResult.formulaVersion}
              </span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-xs text-left">
                <thead className="bg-slate-50 dark:bg-slate-800/60 text-slate-600 dark:text-slate-400 font-semibold border-b border-slate-200 dark:border-slate-700">
                  <tr>
                    <th className="p-3">Cenário</th>
                    <th className="p-3">Preço Bruto</th>
                    <th className="p-3">Receita Líquida</th>
                    <th className="p-3">Deduções Totais</th>
                    <th className="p-3">Margem Contrib. (R$)</th>
                    <th className="p-3">Margem (%)</th>
                    <th className="p-3">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {generateScenarios(calculationResult).map((sc, idx) => (
                    <tr key={idx} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40">
                      <td className="p-3 font-semibold text-slate-900 dark:text-white">
                        {sc.title}
                      </td>
                      <td className="p-3 font-mono">{formatMoney(sc.price)}</td>
                      <td className="p-3 font-mono">{formatMoney(sc.netRevenue)}</td>
                      <td className="p-3 font-mono text-slate-600 dark:text-slate-400">
                        {formatMoney(sc.totalDeductions)}
                      </td>
                      <td
                        className={`p-3 font-mono font-bold ${
                          sc.marginAmount >= 0
                            ? 'text-emerald-700 dark:text-emerald-400'
                            : 'text-rose-700 dark:text-rose-400'
                        }`}
                      >
                        {formatMoney(sc.marginAmount)}
                      </td>
                      <td
                        className={`p-3 font-mono font-bold ${
                          sc.marginPercent >= 0
                            ? 'text-emerald-700 dark:text-emerald-400'
                            : 'text-rose-700 dark:text-rose-400'
                        }`}
                      >
                        {formatPercent(sc.marginPercent)}
                      </td>
                      <td className="p-3">
                        <span className={`px-2 py-0.5 rounded text-[10px] font-semibold ${sc.badgeColor}`}>
                          {sc.badge}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Detalhamento Granular de Cada Dedução */}
          <div className="p-6 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
            <h3 className="text-sm font-bold text-slate-900 dark:text-white flex items-center gap-2">
              <span>🧾</span>
              <span>Composição Detalhada das Deduções Unitárias</span>
            </h3>

            <div className="overflow-x-auto">
              <table className="w-full text-xs text-left">
                <thead className="bg-slate-50 dark:bg-slate-800/60 text-slate-600 dark:text-slate-400 font-semibold border-b border-slate-200 dark:border-slate-700">
                  <tr>
                    <th className="p-3">Parcela</th>
                    <th className="p-3">Categoria</th>
                    <th className="p-3">Alíquota / Base</th>
                    <th className="p-3">Valor Deduzido</th>
                    <th className="p-3">% da Receita Líquida</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {calculationResult.deductionItems.map((item, idx) => (
                    <tr key={idx} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40">
                      <td className="p-3 font-semibold text-slate-900 dark:text-white">{item.name}</td>
                      <td className="p-3">
                        <span className="px-2 py-0.5 rounded text-[10px] font-mono bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300">
                          {item.category}
                        </span>
                      </td>
                      <td className="p-3 font-mono">
                        {item.isPercentage ? `${item.appliedRate.toFixed(2)}%` : 'Valor Fixo'}
                      </td>
                      <td className="p-3 font-mono text-rose-600 dark:text-rose-400 font-semibold">
                        -{formatMoney(item.amount)}
                      </td>
                      <td className="p-3 font-mono text-slate-600 dark:text-slate-400">
                        {formatPercent(item.percentageOfNetRevenue)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
