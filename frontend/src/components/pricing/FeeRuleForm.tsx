'use client';

import React, { useState, useEffect } from 'react';
import { FeeRule, FeeType, FeeValueType, FeeSourceType } from '@/types/pricing';

interface FeeRuleFormProps {
  onRuleCreated?: (rule: FeeRule) => void;
}

interface RuleFormState {
  marketplace: string;
  feeType: FeeType;
  feeValueType: FeeValueType;
  percentageValue: string;
  fixedAmount: string;
  minPriceThreshold: string;
  maxPriceThreshold: string;
  listingType: string;
  category: string;
  effectiveFrom: string;
  effectiveTo: string;
  sourceType: FeeSourceType;
  sourceReference: string;
  notes: string;
}

interface RuleFormErrors {
  marketplace?: string;
  feeType?: string;
  percentageValue?: string;
  fixedAmount?: string;
  priceThresholds?: string;
  effectiveFrom?: string;
  sourceReference?: string;
}

export function FeeRuleForm({ onRuleCreated }: FeeRuleFormProps) {
  const [formState, setFormState] = useState<RuleFormState>({
    marketplace: 'MERCADO_LIVRE',
    feeType: 'COMMISSION',
    feeValueType: 'PERCENTAGE',
    percentageValue: '16.00',
    fixedAmount: '0.00',
    minPriceThreshold: '',
    maxPriceThreshold: '',
    listingType: '',
    category: '',
    effectiveFrom: new Date().toISOString().slice(0, 16),
    effectiveTo: '',
    sourceType: 'OFFICIAL_PORTAL',
    sourceReference: 'https://www.mercadolivre.com.br/ajuda/custos-vender-produtos_1338',
    notes: '',
  });

  const [errors, setErrors] = useState<RuleFormErrors>({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitFeedback, setSubmitFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  // Lista de Regras Existentes
  const [rules, setRules] = useState<FeeRule[]>([]);
  const [isLoadingRules, setIsLoadingRules] = useState(false);
  const [filterMarketplace, setFilterMarketplace] = useState<string>('ALL');

  const validate = (): boolean => {
    const newErrors: RuleFormErrors = {};

    if (!formState.marketplace.trim()) {
      newErrors.marketplace = 'Marketplace é obrigatório.';
    }

    if (!formState.sourceReference.trim()) {
      newErrors.sourceReference = 'A referência da fonte oficial/contratual é obrigatória (ex: URL ou ID do contrato).';
    }

    if (!formState.effectiveFrom.trim()) {
      newErrors.effectiveFrom = 'Data/hora inicial de vigência é obrigatória.';
    }

    // Validação de percentual
    if (formState.feeValueType === 'PERCENTAGE' || formState.feeValueType === 'PERCENTAGE_PLUS_FIXED') {
      const perc = parseFloat(formState.percentageValue);
      if (!formState.percentageValue.trim() || isNaN(perc)) {
        newErrors.percentageValue = 'Valor percentual é obrigatório e deve ser numérico.';
      } else if (perc < 0 || perc > 100) {
        newErrors.percentageValue = 'Percentual deve estar entre 0% e 100%.';
      }
    }

    // Validação de fixo
    if (formState.feeValueType === 'FIXED_AMOUNT' || formState.feeValueType === 'PERCENTAGE_PLUS_FIXED') {
      const fixed = parseFloat(formState.fixedAmount);
      if (!formState.fixedAmount.trim() || isNaN(fixed)) {
        newErrors.fixedAmount = 'Valor fixo é obrigatório e deve ser numérico.';
      } else if (fixed < 0) {
        newErrors.fixedAmount = 'Valor fixo não pode ser negativo.';
      }
    }

    // Faixas de preço
    const minP = formState.minPriceThreshold.trim() ? parseFloat(formState.minPriceThreshold) : null;
    const maxP = formState.maxPriceThreshold.trim() ? parseFloat(formState.maxPriceThreshold) : null;
    if (minP !== null && maxP !== null && minP > maxP) {
      newErrors.priceThresholds = 'O preço mínimo da faixa não pode ser maior que o preço máximo.';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const loadRules = async (mkt?: string) => {
    setIsLoadingRules(true);
    try {
      const query = mkt && mkt !== 'ALL' ? `?marketplace=${encodeURIComponent(mkt)}` : '';
      const res = await fetch(`/api/pricing/fee-rules${query}`);
      if (res.ok) {
        const data = await res.json();
        setRules(data);
      } else {
        setRules([]);
      }
    } catch {
      setRules([]);
    } finally {
      setIsLoadingRules(false);
    }
  };

  useEffect(() => {
    loadRules(filterMarketplace);
  }, [filterMarketplace]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitFeedback(null);

    if (!validate()) {
      return;
    }

    setIsSubmitting(true);
    try {
      const payload = {
        marketplace: formState.marketplace,
        feeType: formState.feeType,
        feeValueType: formState.feeValueType,
        percentageValue:
          formState.feeValueType === 'PERCENTAGE' || formState.feeValueType === 'PERCENTAGE_PLUS_FIXED'
            ? parseFloat(formState.percentageValue)
            : null,
        fixedAmount:
          formState.feeValueType === 'FIXED_AMOUNT' || formState.feeValueType === 'PERCENTAGE_PLUS_FIXED'
            ? parseFloat(formState.fixedAmount)
            : null,
        minPriceThreshold: formState.minPriceThreshold.trim() ? parseFloat(formState.minPriceThreshold) : null,
        maxPriceThreshold: formState.maxPriceThreshold.trim() ? parseFloat(formState.maxPriceThreshold) : null,
        listingType: formState.listingType.trim() || null,
        category: formState.category.trim() || null,
        effectiveFrom: new Date(formState.effectiveFrom).toISOString(),
        effectiveTo: formState.effectiveTo.trim() ? new Date(formState.effectiveTo).toISOString() : null,
        sourceType: formState.sourceType,
        sourceReference: formState.sourceReference.trim(),
        notes: formState.notes.trim() || null,
      };

      const res = await fetch('/api/pricing/fee-rules', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Erro ao cadastrar regra de taxa.');
      }

      const created: FeeRule = await res.json();
      setSubmitFeedback({
        type: 'success',
        message: `Regra de taxa para ${created.marketplace} cadastrada com sucesso (v${created.version})!`,
      });

      if (onRuleCreated) {
        onRuleCreated(created);
      }

      // Recarrega regras
      loadRules(filterMarketplace);
    } catch (err: any) {
      setSubmitFeedback({
        type: 'error',
        message: err.message || 'Falha ao salvar regra de taxa.',
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-8">
      {/* Formulário de Nova Regra */}
      <section
        aria-labelledby="rule-form-heading"
        className="bg-white dark:bg-slate-900 rounded-xl p-6 border border-slate-200 dark:border-slate-800 shadow-sm"
      >
        <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800">
          <div>
            <h2 id="rule-form-heading" className="text-base font-bold text-slate-900 dark:text-white">
              Cadastro de Regras de Tarifas e Comissões
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Configure as regras oficiais ou contratuais de cobrança por marketplace, modalidade e faixas de preço.
            </p>
          </div>
          <span className="text-xs px-2.5 py-1 bg-amber-50 dark:bg-amber-950/40 text-amber-700 dark:text-amber-300 rounded font-medium border border-amber-200 dark:border-amber-900">
            Regra Anti-Alucinação Estrita
          </span>
        </div>

        {submitFeedback && (
          <div
            role="alert"
            className={`mt-4 p-3.5 rounded-lg text-xs font-medium flex items-center gap-2 ${
              submitFeedback.type === 'success'
                ? 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-800 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-900'
                : 'bg-rose-50 dark:bg-rose-950/40 text-rose-800 dark:text-rose-300 border border-rose-200 dark:border-rose-900'
            }`}
          >
            <span>{submitFeedback.type === 'success' ? '✓' : '⚠️'}</span>
            <span>{submitFeedback.message}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate className="mt-5 space-y-5">
          {/* Linha 1: Marketplace e Tipos */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label htmlFor="rule-marketplace" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Marketplace <span className="text-rose-500">*</span>
              </label>
              <select
                id="rule-marketplace"
                name="marketplace"
                value={formState.marketplace}
                onChange={(e) => setFormState({ ...formState, marketplace: e.target.value })}
                className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="MERCADO_LIVRE">Mercado Livre</option>
                <option value="SHOPEE">Shopee</option>
                <option value="AMAZON">Amazon Brasil</option>
                <option value="MAGALU">Magazine Luiza</option>
              </select>
            </div>

            <div>
              <label htmlFor="rule-fee-type" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Tipo de Cobrança <span className="text-rose-500">*</span>
              </label>
              <select
                id="rule-fee-type"
                name="feeType"
                value={formState.feeType}
                onChange={(e) => setFormState({ ...formState, feeType: e.target.value as FeeType })}
                className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="COMMISSION">Comissão Padrão do Canal</option>
                <option value="FIXED_FEE">Tarifa Fixa por Venda</option>
                <option value="LOGISTICS_FEE">Taxa Operacional de Frete</option>
                <option value="PAYMENT_PROCESSING">Processamento de Pagamento</option>
                <option value="OTHER">Outras Taxas Regulatórias</option>
              </select>
            </div>

            <div>
              <label htmlFor="rule-fee-value-type" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Formato do Valor <span className="text-rose-500">*</span>
              </label>
              <select
                id="rule-fee-value-type"
                name="feeValueType"
                value={formState.feeValueType}
                onChange={(e) => setFormState({ ...formState, feeValueType: e.target.value as FeeValueType })}
                className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value="PERCENTAGE">Apenas Percentual (%)</option>
                <option value="FIXED_AMOUNT">Apenas Valor Fixo (R$)</option>
                <option value="PERCENTAGE_PLUS_FIXED">Percentual + Valor Fixo (% + R$)</option>
              </select>
            </div>
          </div>

          {/* Linha 2: Valores e Faixas */}
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
            {(formState.feeValueType === 'PERCENTAGE' || formState.feeValueType === 'PERCENTAGE_PLUS_FIXED') && (
              <div>
                <label htmlFor="rule-percentage-value" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Alíquota (%) <span className="text-rose-500">*</span>
                </label>
                <input
                  id="rule-percentage-value"
                  name="percentageValue"
                  type="number"
                  step="0.01"
                  min="0.00"
                  max="100.00"
                  placeholder="Ex: 16.00"
                  value={formState.percentageValue}
                  onChange={(e) => setFormState({ ...formState, percentageValue: e.target.value })}
                  aria-invalid={!!errors.percentageValue}
                  className={`w-full text-xs px-3 py-2 rounded-lg border bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 ${
                    errors.percentageValue
                      ? 'border-rose-500 focus:ring-rose-500'
                      : 'border-slate-300 dark:border-slate-700 focus:ring-blue-500'
                  }`}
                />
                {errors.percentageValue && (
                  <p role="alert" className="text-[11px] text-rose-500 mt-1 font-medium">{errors.percentageValue}</p>
                )}
              </div>
            )}

            {(formState.feeValueType === 'FIXED_AMOUNT' || formState.feeValueType === 'PERCENTAGE_PLUS_FIXED') && (
              <div>
                <label htmlFor="rule-fixed-amount" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Tarifa Fixa (R$) <span className="text-rose-500">*</span>
                </label>
                <input
                  id="rule-fixed-amount"
                  name="fixedAmount"
                  type="number"
                  step="0.01"
                  min="0.00"
                  placeholder="Ex: 6.00"
                  value={formState.fixedAmount}
                  onChange={(e) => setFormState({ ...formState, fixedAmount: e.target.value })}
                  aria-invalid={!!errors.fixedAmount}
                  className={`w-full text-xs px-3 py-2 rounded-lg border bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 ${
                    errors.fixedAmount
                      ? 'border-rose-500 focus:ring-rose-500'
                      : 'border-slate-300 dark:border-slate-700 focus:ring-blue-500'
                  }`}
                />
                {errors.fixedAmount && (
                  <p role="alert" className="text-[11px] text-rose-500 mt-1 font-medium">{errors.fixedAmount}</p>
                )}
              </div>
            )}

            <div>
              <label htmlFor="rule-min-price" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Preço Mínimo da Faixa (R$)
              </label>
              <input
                id="rule-min-price"
                name="minPriceThreshold"
                type="number"
                step="0.01"
                min="0.00"
                placeholder="Ex: 0.00"
                value={formState.minPriceThreshold}
                onChange={(e) => setFormState({ ...formState, minPriceThreshold: e.target.value })}
                className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label htmlFor="rule-max-price" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Preço Máximo da Faixa (R$)
              </label>
              <input
                id="rule-max-price"
                name="maxPriceThreshold"
                type="number"
                step="0.01"
                min="0.00"
                placeholder="Ex: 79.00"
                value={formState.maxPriceThreshold}
                onChange={(e) => setFormState({ ...formState, maxPriceThreshold: e.target.value })}
                className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>
          {errors.priceThresholds && (
            <p role="alert" className="text-[11px] text-rose-500 font-medium">{errors.priceThresholds}</p>
          )}

          {/* Linha 3: Segmentação e Vigência */}
          <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-4">
            <div>
              <label htmlFor="rule-listing-type" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Modalidade de Anúncio
              </label>
              <input
                id="rule-listing-type"
                name="listingType"
                type="text"
                placeholder="Ex: gold_pro ou clássico"
                value={formState.listingType}
                onChange={(e) => setFormState({ ...formState, listingType: e.target.value })}
                className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label htmlFor="rule-category" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Categoria (Opcional)
              </label>
              <input
                id="rule-category"
                name="category"
                type="text"
                placeholder="Ex: Eletrônicos ou Moda"
                value={formState.category}
                onChange={(e) => setFormState({ ...formState, category: e.target.value })}
                className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>

            <div>
              <label htmlFor="rule-effective-from" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Vigência Inicial <span className="text-rose-500">*</span>
              </label>
              <input
                id="rule-effective-from"
                name="effectiveFrom"
                type="datetime-local"
                value={formState.effectiveFrom}
                onChange={(e) => setFormState({ ...formState, effectiveFrom: e.target.value })}
                aria-invalid={!!errors.effectiveFrom}
                className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
              {errors.effectiveFrom && (
                <p role="alert" className="text-[11px] text-rose-500 mt-1 font-medium">{errors.effectiveFrom}</p>
              )}
            </div>

            <div>
              <label htmlFor="rule-effective-to" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Vigência Final (Opcional)
              </label>
              <input
                id="rule-effective-to"
                name="effectiveTo"
                type="datetime-local"
                value={formState.effectiveTo}
                onChange={(e) => setFormState({ ...formState, effectiveTo: e.target.value })}
                className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          {/* Linha 4: Proveniência Estrita & Regra Anti-Alucinação */}
          <div className="p-4 rounded-lg bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 space-y-3">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label htmlFor="rule-source-type" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Tipo de Fonte da Regra <span className="text-rose-500">*</span>
                </label>
                <select
                  id="rule-source-type"
                  name="sourceType"
                  value={formState.sourceType}
                  onChange={(e) => setFormState({ ...formState, sourceType: e.target.value as FeeSourceType })}
                  className="w-full text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="OFFICIAL_PORTAL">Portal Oficial do Marketplace (URL Pública)</option>
                  <option value="CONTRACT">Contrato Comercial / Tabela Negociada</option>
                  <option value="MANUAL_OVERRIDE">Regra Manual / Estimativa Temporária (Não Oficial)</option>
                </select>
              </div>

              <div>
                <label htmlFor="rule-source-reference" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Referência da Fonte (URL ou Contrato) <span className="text-rose-500">*</span>
                </label>
                <input
                  id="rule-source-reference"
                  name="sourceReference"
                  type="text"
                  placeholder="URL do portal ou identificador do aditivo contratual"
                  value={formState.sourceReference}
                  onChange={(e) => setFormState({ ...formState, sourceReference: e.target.value })}
                  aria-invalid={!!errors.sourceReference}
                  className={`w-full text-xs px-3 py-2 rounded-lg border bg-white dark:bg-slate-900 text-slate-900 dark:text-white focus:outline-none focus:ring-2 ${
                    errors.sourceReference
                      ? 'border-rose-500 focus:ring-rose-500'
                      : 'border-slate-300 dark:border-slate-700 focus:ring-blue-500'
                  }`}
                />
                {errors.sourceReference && (
                  <p role="alert" className="text-[11px] text-rose-500 mt-1 font-medium">{errors.sourceReference}</p>
                )}
              </div>
            </div>

            {/* Banner de Aviso Anti-Alucinação para Manual Override */}
            {formState.sourceType === 'MANUAL_OVERRIDE' ? (
              <div
                role="alert"
                className="p-3 bg-amber-100/70 dark:bg-amber-950/40 text-amber-900 dark:text-amber-200 border border-amber-300 dark:border-amber-800 rounded-lg text-xs flex items-start gap-2"
              >
                <span className="text-base leading-none">⚠️</span>
                <div>
                  <strong className="font-bold">Regra Manual Não Verificada:</strong>
                  <p className="mt-0.5 text-[11px] opacity-90">
                    Esta regra não possui comprovação oficial ou contratual. Ela nunca será exibida como taxa oficial do marketplace e gerará alertas ostensivos durante qualquer cálculo de rentabilidade.
                  </p>
                </div>
              </div>
            ) : (
              <div className="p-3 bg-emerald-50 dark:bg-emerald-950/30 text-emerald-800 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800/50 rounded-lg text-xs flex items-center gap-2">
                <span>🛡️</span>
                <span>
                  <strong>Fonte Verificada:</strong> Esta regra será registrada como proveniente de fonte oficial com rastreabilidade auditável.
                </span>
              </div>
            )}
          </div>

          <div className="flex justify-end pt-2">
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-5 py-2.5 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white text-xs font-bold rounded-lg shadow transition-colors flex items-center justify-center gap-2 min-h-[44px]"
            >
              {isSubmitting ? (
                <>
                  <span className="w-3.5 h-3.5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  <span>Cadastrando Regra...</span>
                </>
              ) : (
                <span>Cadastrar Regra de Taxa</span>
              )}
            </button>
          </div>
        </form>
      </section>

      {/* Tabela de Regras Cadastradas */}
      <section
        aria-labelledby="rule-list-heading"
        className="bg-white dark:bg-slate-900 rounded-xl p-6 border border-slate-200 dark:border-slate-800 shadow-sm"
      >
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-slate-100 dark:border-slate-800">
          <div>
            <h3 id="rule-list-heading" className="text-sm font-bold text-slate-900 dark:text-white">
              Tabela de Regras e Tarifas Vigentes
            </h3>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Políticas de cobrança ativas e arquivadas por marketplace.
            </p>
          </div>

          <div className="flex items-center gap-2">
            <label htmlFor="filter-marketplace-select" className="text-xs text-slate-500 font-medium">
              Filtrar Canal:
            </label>
            <select
              id="filter-marketplace-select"
              value={filterMarketplace}
              onChange={(e) => setFilterMarketplace(e.target.value)}
              className="text-xs px-2.5 py-1.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800 text-slate-800 dark:text-slate-200 min-h-[36px]"
            >
              <option value="ALL">Todos os Marketplaces</option>
              <option value="MERCADO_LIVRE">Mercado Livre</option>
              <option value="SHOPEE">Shopee</option>
            </select>
          </div>
        </div>

        {isLoadingRules ? (
          <div className="py-8 text-center text-xs text-slate-500 dark:text-slate-400" role="status">
            Carregando tabela de taxas...
          </div>
        ) : rules.length === 0 ? (
          <div className="py-8 text-center text-xs text-slate-500 dark:text-slate-400" role="region">
            Nenhuma regra cadastrada para o filtro selecionado. Use o formulário acima para registrar uma regra com sua respectiva fonte.
          </div>
        ) : (
          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-slate-200 dark:border-slate-800 text-slate-500 dark:text-slate-400">
                  <th className="py-2.5 px-3 font-semibold">Canal</th>
                  <th className="py-2.5 px-3 font-semibold">Cobrança</th>
                  <th className="py-2.5 px-3 font-semibold">Valor Aplicado</th>
                  <th className="py-2.5 px-3 font-semibold">Faixa de Preço</th>
                  <th className="py-2.5 px-3 font-semibold">Vigência</th>
                  <th className="py-2.5 px-3 font-semibold">Oficialidade / Fonte</th>
                  <th className="py-2.5 px-3 font-semibold">Versão</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800 text-slate-800 dark:text-slate-200">
                {rules.map((rule) => {
                  const isExpired = rule.effectiveTo && new Date(rule.effectiveTo) <= new Date();
                  const isManual = rule.sourceType === 'MANUAL_OVERRIDE';

                  return (
                    <tr
                      key={rule.id}
                      className={
                        isManual
                          ? 'bg-amber-50/40 dark:bg-amber-950/20'
                          : isExpired
                          ? 'opacity-60 bg-slate-50/50 dark:bg-slate-900/40'
                          : ''
                      }
                    >
                      <td className="py-3 px-3 font-semibold text-slate-900 dark:text-white">
                        {rule.marketplace}
                      </td>
                      <td className="py-3 px-3">
                        <span className="font-medium text-slate-800 dark:text-slate-200">
                          {rule.feeType}
                        </span>
                        {rule.listingType && (
                          <span className="text-[10px] block text-slate-500">
                            Mod: {rule.listingType}
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-3 font-semibold">
                        {rule.feeValueType === 'PERCENTAGE' && (
                          <span className="text-blue-600 dark:text-blue-400">
                            {rule.percentageValue?.toFixed(2)}%
                          </span>
                        )}
                        {rule.feeValueType === 'FIXED_AMOUNT' && (
                          <span className="text-emerald-600 dark:text-emerald-400">
                            R$ {rule.fixedAmount?.toFixed(2)}
                          </span>
                        )}
                        {rule.feeValueType === 'PERCENTAGE_PLUS_FIXED' && (
                          <span className="text-purple-600 dark:text-purple-400">
                            {rule.percentageValue?.toFixed(2)}% + R$ {rule.fixedAmount?.toFixed(2)}
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-3 text-[11px] text-slate-600 dark:text-slate-400">
                        {rule.minPriceThreshold != null || rule.maxPriceThreshold != null ? (
                          <>
                            {rule.minPriceThreshold != null ? `De R$ ${rule.minPriceThreshold.toFixed(2)}` : 'Até'}
                            {rule.maxPriceThreshold != null ? ` até R$ ${rule.maxPriceThreshold.toFixed(2)}` : ' em diante'}
                          </>
                        ) : (
                          'Qualquer preço'
                        )}
                      </td>
                      <td className="py-3 px-3 text-[11px] text-slate-600 dark:text-slate-400">
                        {isExpired ? (
                          <span className="inline-flex items-center gap-1 text-rose-600 dark:text-rose-400 font-semibold">
                            ⚠️ Expirada ({new Date(rule.effectiveTo!).toLocaleDateString('pt-BR')})
                          </span>
                        ) : (
                          <span>
                            Vigente desde {new Date(rule.effectiveFrom).toLocaleDateString('pt-BR')}
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-3 text-[11px]">
                        {isManual ? (
                          <span
                            title="Regra manual - Não verificada com fontes oficiais"
                            className="inline-flex items-center gap-1 px-2 py-0.5 rounded font-bold bg-amber-100 dark:bg-amber-900/60 text-amber-800 dark:text-amber-200"
                          >
                            ⚠️ Manual (Não Oficial)
                          </span>
                        ) : (
                          <span
                            title={rule.sourceReference}
                            className="inline-flex items-center gap-1 px-2 py-0.5 rounded font-medium bg-emerald-100 dark:bg-emerald-900/40 text-emerald-800 dark:text-emerald-300 truncate max-w-[150px]"
                          >
                            🛡️ {rule.sourceType === 'CONTRACT' ? 'Contrato' : 'Portal Oficial'}
                          </span>
                        )}
                        <span className="block text-[10px] text-slate-400 truncate max-w-[180px] mt-0.5" title={rule.sourceReference}>
                          {rule.sourceReference}
                        </span>
                      </td>
                      <td className="py-3 px-3 text-[11px] text-slate-500 font-mono">
                        v{rule.version}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}
