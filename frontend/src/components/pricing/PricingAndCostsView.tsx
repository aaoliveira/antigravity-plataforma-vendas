'use client';

import React, { useState } from 'react';
import { ProductCostForm } from './ProductCostForm';
import { FeeRuleForm } from './FeeRuleForm';
import { FeeSimulator } from './FeeSimulator';
import { PricingCalculator } from './PricingCalculator';

type TabKey = 'COSTS' | 'FEES' | 'SIMULATOR' | 'CALCULATOR';

export function PricingAndCostsView() {
  const [activeTab, setActiveTab] = useState<TabKey>('COSTS');

  return (
    <div className="space-y-6">
      {/* Cabeçalho do Módulo */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-200 dark:border-slate-800">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-xl">💰</span>
            <h1 className="text-xl font-bold text-slate-900 dark:text-white">
              Gestão de Custos, Taxas e Simulação Financeira
            </h1>
          </div>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Controle determinístico de custos unitários, alíquotas operacionais e regras contratuais de comissões por marketplace.
          </p>
        </div>

        {/* Badge de Governança */}
        <div className="flex items-center gap-2">
          <span className="text-xs font-semibold px-2.5 py-1 rounded bg-blue-50 dark:bg-blue-950/40 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-900">
            Precisão BigDecimal
          </span>
          <span className="text-xs font-semibold px-2.5 py-1 rounded bg-amber-50 dark:bg-amber-950/40 text-amber-700 dark:text-amber-300 border border-amber-200 dark:border-amber-900">
            Anti-Alucinação Ativa
          </span>
        </div>
      </div>

      {/* Navegação por Abas (Tablist Acessível) */}
      <div
        role="tablist"
        aria-label="Abas de Gestão de Custos e Taxas"
        className="flex items-center gap-2 border-b border-slate-200 dark:border-slate-800 overflow-x-auto pb-1"
      >
        <button
          role="tab"
          id="tab-costs"
          aria-selected={activeTab === 'COSTS'}
          aria-controls="tabpanel-costs"
          onClick={() => setActiveTab('COSTS')}
          className={`px-4 py-2.5 text-xs font-bold rounded-lg transition-colors flex items-center gap-2 min-h-[44px] whitespace-nowrap ${
            activeTab === 'COSTS'
              ? 'bg-blue-600 text-white shadow-sm'
              : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
          }`}
        >
          <span>📦</span>
          <span>Custos por Produto</span>
        </button>

        <button
          role="tab"
          id="tab-fees"
          aria-selected={activeTab === 'FEES'}
          aria-controls="tabpanel-fees"
          onClick={() => setActiveTab('FEES')}
          className={`px-4 py-2.5 text-xs font-bold rounded-lg transition-colors flex items-center gap-2 min-h-[44px] whitespace-nowrap ${
            activeTab === 'FEES'
              ? 'bg-blue-600 text-white shadow-sm'
              : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
          }`}
        >
          <span>🏷️</span>
          <span>Regras de Taxas & Tarifas</span>
        </button>

        <button
          role="tab"
          id="tab-simulator"
          aria-selected={activeTab === 'SIMULATOR'}
          aria-controls="tabpanel-simulator"
          onClick={() => setActiveTab('SIMULATOR')}
          className={`px-4 py-2.5 text-xs font-bold rounded-lg transition-colors flex items-center gap-2 min-h-[44px] whitespace-nowrap ${
            activeTab === 'SIMULATOR'
              ? 'bg-blue-600 text-white shadow-sm'
              : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
          }`}
        >
          <span>⚡</span>
          <span>Simulador de Resolução</span>
        </button>

        <button
          role="tab"
          id="tab-calculator"
          aria-selected={activeTab === 'CALCULATOR'}
          aria-controls="tabpanel-calculator"
          onClick={() => setActiveTab('CALCULATOR')}
          className={`px-4 py-2.5 text-xs font-bold rounded-lg transition-colors flex items-center gap-2 min-h-[44px] whitespace-nowrap ${
            activeTab === 'CALCULATOR'
              ? 'bg-blue-600 text-white shadow-sm'
              : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
          }`}
        >
          <span>🧮</span>
          <span>Calculadora de Margem & Cenários</span>
        </button>
      </div>

      {/* Conteúdo das Abas (Tabpanels Acessíveis) */}
      <div className="pt-2">
        {activeTab === 'COSTS' && (
          <div
            role="tabpanel"
            id="tabpanel-costs"
            aria-labelledby="tab-costs"
          >
            <ProductCostForm />
          </div>
        )}

        {activeTab === 'FEES' && (
          <div
            role="tabpanel"
            id="tabpanel-fees"
            aria-labelledby="tab-fees"
          >
            <FeeRuleForm />
          </div>
        )}

        {activeTab === 'SIMULATOR' && (
          <div
            role="tabpanel"
            id="tabpanel-simulator"
            aria-labelledby="tab-simulator"
          >
            <FeeSimulator />
          </div>
        )}

        {activeTab === 'CALCULATOR' && (
          <div
            role="tabpanel"
            id="tabpanel-calculator"
            aria-labelledby="tab-calculator"
          >
            <PricingCalculator />
          </div>
        )}
      </div>
    </div>
  );
}
