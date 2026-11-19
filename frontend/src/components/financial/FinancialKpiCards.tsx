'use client';

import React, { useState } from 'react';
import { FinancialFactSummary } from '@/types/financial';

interface FinancialKpiCardsProps {
  summary: FinancialFactSummary | null;
  isLoading: boolean;
  onFilterMissingData?: () => void;
}

interface MetricDefinition {
  title: string;
  definition: string;
  formula: string;
  antiHallucinationNote?: string;
}

const DEFINITIONS: Record<string, MetricDefinition> = {
  grossRevenue: {
    title: 'Receita Bruta',
    definition: 'Valor monetário total das vendas calculadas a preço nominal antes de quaisquer deduções ou descontos.',
    formula: 'Σ (Preço Unitário × Quantidade)',
  },
  netRevenue: {
    title: 'Receita Líquida',
    definition: 'Faturamento bruto subtraído dos descontos promocionais concedidos diretamente nas transações.',
    formula: 'Receita Bruta - Descontos',
  },
  channelFees: {
    title: 'Deduções de Canal',
    definition: 'Comissões percentuais e tarifas fixas por transação cobradas pelas plataformas de marketplace.',
    formula: 'Comissões + Tarifas Fixas do Marketplace',
  },
  directCosts: {
    title: 'Custos Diretos (CMV)',
    definition: 'Custo das Mercadorias Vendidas (aquisição dos produtos) somado aos custos de embalagens e insumos diretos informados.',
    formula: 'Custo de Aquisição + Embalagem + Custos Adicionais',
  },
  taxes: {
    title: 'Impostos Informados',
    definition: 'Estimativa de tributos fiscais sobre o faturamento declarada pelo usuário no enquadramento tributário.',
    formula: 'Receita Líquida × Alíquota Fiscal Informada (%)',
  },
  contributionMargin: {
    title: 'Margem de Contribuição',
    definition: 'Valor financeiro que sobra da receita líquida após o pagamento de todas as despesas e custos diretamente variáveis da venda.',
    formula: 'Receita Líquida - (Deduções de Canal + Frete + CMV + Embalagem + Impostos)',
    antiHallucinationNote: 'NÃO É LUCRO LÍQUIDO. As despesas fixas da estrutura da empresa (aluguel, pró-labore, folha de pagamento, contabilidade, servidores) não estão modeladas e precisam ser cobertas por esta margem.',
  },
};

export const FinancialKpiCards: React.FC<FinancialKpiCardsProps> = ({
  summary,
  isLoading,
  onFilterMissingData,
}) => {
  const [activeTooltip, setActiveTooltip] = useState<string | null>(null);

  const formatCurrency = (val?: number) => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val || 0);
  };

  const formatPercent = (val?: number) => {
    return `${(val || 0).toFixed(2)}%`;
  };

  const toggleTooltip = (key: string) => {
    setActiveTooltip(activeTooltip === key ? null : key);
  };

  const totalChannelFees = (summary?.totalMarketplaceFees || 0) + (summary?.totalFixedFees || 0);
  const totalDirectCosts = (summary?.totalProductCost || 0) + (summary?.totalPackagingCost || 0) + (summary?.totalAdditionalCost || 0);

  return (
    <div className="space-y-4">
      {/* Banner Anti-Alucinação Mandatório */}
      <div
        role="alert"
        className="rounded-lg border border-amber-200 bg-amber-50 dark:border-amber-900/50 dark:bg-amber-950/30 p-3.5 text-xs text-amber-900 dark:text-amber-200 flex items-start gap-3"
      >
        <span className="text-amber-600 dark:text-amber-400 font-bold text-base select-none" aria-hidden="true">
          ℹ
        </span>
        <div className="flex-1">
          <p className="font-semibold text-amber-950 dark:text-amber-100">
            Aviso de Governança Financeira (Margem de Contribuição vs. Lucro Líquido):
          </p>
          <p className="mt-0.5 leading-relaxed">
            {summary?.antiHallucinationDisclaimer ||
              'Este demonstrativo apura estritamente a Margem de Contribuição (Receita Líquida deduzida de custos diretos variáveis, comissões, fretes e impostos informados). Despesas fixas estruturais e operacionais da empresa não estão contempladas, sendo proibido rotular este valor como Lucro Líquido.'}
          </p>
        </div>
      </div>

      {/* Grid de Cards de Indicadores */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-3">
        {/* 1. Receita Bruta */}
        <div className="relative rounded-lg border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-4 shadow-sm flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-500 dark:text-slate-400">Receita Bruta</span>
            <button
              type="button"
              onClick={() => toggleTooltip('grossRevenue')}
              aria-label="Definição de Receita Bruta"
              className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 text-xs font-bold p-1 rounded-full focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              ?
            </button>
          </div>
          <div className="mt-2">
            {isLoading ? (
              <div className="h-6 w-24 bg-slate-200 dark:bg-slate-700 animate-pulse rounded" />
            ) : (
              <p className="text-xl font-bold text-slate-900 dark:text-white">
                {formatCurrency(summary?.totalGrossRevenue)}
              </p>
            )}
            <p className="text-[11px] text-slate-500 mt-0.5">
              {summary?.totalOrders || 0} pedidos no período
            </p>
          </div>
          {activeTooltip === 'grossRevenue' && (
            <div
              role="tooltip"
              className="absolute z-20 left-2 right-2 top-12 p-3 bg-slate-850 dark:bg-slate-800 text-white rounded-md shadow-xl border border-slate-700 text-xs"
            >
              <div className="flex justify-between items-start font-semibold">
                <span>{DEFINITIONS.grossRevenue.title}</span>
                <button onClick={() => setActiveTooltip(null)} className="text-slate-400 hover:text-white" aria-label="Fechar">✕</button>
              </div>
              <p className="mt-1 text-slate-300">{DEFINITIONS.grossRevenue.definition}</p>
              <p className="mt-1 font-mono text-[10px] text-blue-300">{DEFINITIONS.grossRevenue.formula}</p>
            </div>
          )}
        </div>

        {/* 2. Deduções de Canal */}
        <div className="relative rounded-lg border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-4 shadow-sm flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-500 dark:text-slate-400">Deduções Canal</span>
            <button
              type="button"
              onClick={() => toggleTooltip('channelFees')}
              aria-label="Definição de Deduções de Canal"
              className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 text-xs font-bold p-1 rounded-full focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              ?
            </button>
          </div>
          <div className="mt-2">
            {isLoading ? (
              <div className="h-6 w-24 bg-slate-200 dark:bg-slate-700 animate-pulse rounded" />
            ) : (
              <p className="text-xl font-bold text-rose-600 dark:text-rose-400">
                - {formatCurrency(totalChannelFees)}
              </p>
            )}
            <p className="text-[11px] text-slate-500 mt-0.5">Comissões + Tarifas</p>
          </div>
          {activeTooltip === 'channelFees' && (
            <div
              role="tooltip"
              className="absolute z-20 left-2 right-2 top-12 p-3 bg-slate-850 dark:bg-slate-800 text-white rounded-md shadow-xl border border-slate-700 text-xs"
            >
              <div className="flex justify-between items-start font-semibold">
                <span>{DEFINITIONS.channelFees.title}</span>
                <button onClick={() => setActiveTooltip(null)} className="text-slate-400 hover:text-white" aria-label="Fechar">✕</button>
              </div>
              <p className="mt-1 text-slate-300">{DEFINITIONS.channelFees.definition}</p>
              <p className="mt-1 font-mono text-[10px] text-blue-300">{DEFINITIONS.channelFees.formula}</p>
            </div>
          )}
        </div>

        {/* 3. Custos Diretos / CMV */}
        <div className="relative rounded-lg border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-4 shadow-sm flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-500 dark:text-slate-400">Custos Diretos (CMV)</span>
            <button
              type="button"
              onClick={() => toggleTooltip('directCosts')}
              aria-label="Definição de Custos Diretos"
              className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 text-xs font-bold p-1 rounded-full focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              ?
            </button>
          </div>
          <div className="mt-2">
            {isLoading ? (
              <div className="h-6 w-24 bg-slate-200 dark:bg-slate-700 animate-pulse rounded" />
            ) : (
              <p className="text-xl font-bold text-amber-600 dark:text-amber-400">
                - {formatCurrency(totalDirectCosts)}
              </p>
            )}
            <p className="text-[11px] text-slate-500 mt-0.5">Produtos + Embalagens</p>
          </div>
          {activeTooltip === 'directCosts' && (
            <div
              role="tooltip"
              className="absolute z-20 left-2 right-2 top-12 p-3 bg-slate-850 dark:bg-slate-800 text-white rounded-md shadow-xl border border-slate-700 text-xs"
            >
              <div className="flex justify-between items-start font-semibold">
                <span>{DEFINITIONS.directCosts.title}</span>
                <button onClick={() => setActiveTooltip(null)} className="text-slate-400 hover:text-white" aria-label="Fechar">✕</button>
              </div>
              <p className="mt-1 text-slate-300">{DEFINITIONS.directCosts.definition}</p>
              <p className="mt-1 font-mono text-[10px] text-blue-300">{DEFINITIONS.directCosts.formula}</p>
            </div>
          )}
        </div>

        {/* 4. Impostos Informados */}
        <div className="relative rounded-lg border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-4 shadow-sm flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-500 dark:text-slate-400">Impostos Informados</span>
            <button
              type="button"
              onClick={() => toggleTooltip('taxes')}
              aria-label="Definição de Impostos Informados"
              className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 text-xs font-bold p-1 rounded-full focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              ?
            </button>
          </div>
          <div className="mt-2">
            {isLoading ? (
              <div className="h-6 w-24 bg-slate-200 dark:bg-slate-700 animate-pulse rounded" />
            ) : (
              <p className="text-xl font-bold text-indigo-600 dark:text-indigo-400">
                - {formatCurrency(summary?.totalTaxAmount)}
              </p>
            )}
            <p className="text-[11px] text-slate-500 mt-0.5">Alíquota declarada</p>
          </div>
          {activeTooltip === 'taxes' && (
            <div
              role="tooltip"
              className="absolute z-20 left-2 right-2 top-12 p-3 bg-slate-850 dark:bg-slate-800 text-white rounded-md shadow-xl border border-slate-700 text-xs"
            >
              <div className="flex justify-between items-start font-semibold">
                <span>{DEFINITIONS.taxes.title}</span>
                <button onClick={() => setActiveTooltip(null)} className="text-slate-400 hover:text-white" aria-label="Fechar">✕</button>
              </div>
              <p className="mt-1 text-slate-300">{DEFINITIONS.taxes.definition}</p>
              <p className="mt-1 font-mono text-[10px] text-blue-300">{DEFINITIONS.taxes.formula}</p>
            </div>
          )}
        </div>

        {/* 5. Margem de Contribuição */}
        <div className="relative rounded-lg border-2 border-emerald-500/30 bg-emerald-50/40 dark:bg-emerald-950/20 p-4 shadow-sm flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold text-emerald-800 dark:text-emerald-300">Margem Contribuição</span>
            <button
              type="button"
              onClick={() => toggleTooltip('contributionMargin')}
              aria-label="Definição de Margem de Contribuição"
              className="text-emerald-600 hover:text-emerald-800 dark:hover:text-emerald-200 text-xs font-bold p-1 rounded-full focus:outline-none focus:ring-2 focus:ring-emerald-500"
            >
              ?
            </button>
          </div>
          <div className="mt-2">
            {isLoading ? (
              <div className="h-6 w-24 bg-emerald-200 dark:bg-emerald-800 animate-pulse rounded" />
            ) : (
              <p className="text-xl font-bold text-emerald-700 dark:text-emerald-400">
                {formatCurrency(summary?.totalContributionMarginAmount)}
              </p>
            )}
            <p className="text-[11px] font-semibold text-emerald-600 dark:text-emerald-400 mt-0.5">
              {formatPercent(summary?.averageContributionMarginRatio)} da receita líquida
            </p>
          </div>
          {activeTooltip === 'contributionMargin' && (
            <div
              role="tooltip"
              className="absolute z-20 left-2 right-2 top-12 p-3 bg-slate-900 text-white rounded-md shadow-xl border border-emerald-500/50 text-xs"
            >
              <div className="flex justify-between items-start font-bold text-emerald-400">
                <span>{DEFINITIONS.contributionMargin.title}</span>
                <button onClick={() => setActiveTooltip(null)} className="text-slate-400 hover:text-white" aria-label="Fechar">✕</button>
              </div>
              <p className="mt-1 text-slate-300">{DEFINITIONS.contributionMargin.definition}</p>
              <p className="mt-1 font-mono text-[10px] text-emerald-300">{DEFINITIONS.contributionMargin.formula}</p>
              <div className="mt-2 p-1.5 rounded bg-rose-950/50 border border-rose-800/50 text-[10px] text-rose-300 font-semibold">
                ⚠️ {DEFINITIONS.contributionMargin.antiHallucinationNote}
              </div>
            </div>
          )}
        </div>

        {/* 6. Alertas de Incompletude */}
        <div className={`relative rounded-lg border p-4 shadow-sm flex flex-col justify-between ${
          (summary?.ordersWithMissingDataCount || 0) > 0
            ? 'border-rose-300 dark:border-rose-900 bg-rose-50/50 dark:bg-rose-950/20'
            : 'border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900'
        }`}>
          <div className="flex items-center justify-between">
            <span className="text-xs font-medium text-slate-500 dark:text-slate-400">Dados Incompletos</span>
            <span className="text-xs" aria-hidden="true">⚠️</span>
          </div>
          <div className="mt-2">
            {isLoading ? (
              <div className="h-6 w-16 bg-slate-200 dark:bg-slate-700 animate-pulse rounded" />
            ) : (
              <p className={`text-xl font-bold ${
                (summary?.ordersWithMissingDataCount || 0) > 0
                  ? 'text-rose-600 dark:text-rose-400'
                  : 'text-slate-900 dark:text-white'
              }`}>
                {summary?.ordersWithMissingDataCount || 0}
              </p>
            )}
            <p className="text-[11px] text-slate-500 mt-0.5">
              {(summary?.ordersWithMissingDataCount || 0) > 0 ? (
                <button
                  type="button"
                  onClick={onFilterMissingData}
                  className="text-rose-600 dark:text-rose-400 underline font-medium hover:text-rose-800"
                >
                  Filtrar pendências
                </button>
              ) : (
                'Tudo parametrizado'
              )}
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
