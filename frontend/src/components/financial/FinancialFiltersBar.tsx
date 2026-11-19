'use client';

import React from 'react';
import { FinancialFilterState, PeriodPreset } from '@/types/financial';

interface CompanyOption {
  id: string;
  corporateName: string;
  cnpj?: string;
}

interface AccountOption {
  id: string;
  accountName: string;
  marketplace: string;
}

interface FinancialFiltersBarProps {
  filters: FinancialFilterState;
  onChange: (newFilters: FinancialFilterState) => void;
  onReset: () => void;
  companies: CompanyOption[];
  accounts: AccountOption[];
  activeCount: number;
}

export const FinancialFiltersBar: React.FC<FinancialFiltersBarProps> = ({
  filters,
  onChange,
  onReset,
  companies,
  accounts,
  activeCount,
}) => {
  const handlePresetClick = (preset: PeriodPreset) => {
    const now = new Date();
    let start = new Date();
    let end = new Date();

    if (preset === 'TODAY') {
      start.setHours(0, 0, 0, 0);
      end.setHours(23, 59, 59, 999);
    } else if (preset === 'LAST_7') {
      start.setDate(now.getDate() - 7);
      start.setHours(0, 0, 0, 0);
    } else if (preset === 'LAST_30') {
      start.setDate(now.getDate() - 30);
      start.setHours(0, 0, 0, 0);
    } else if (preset === 'THIS_MONTH') {
      start = new Date(now.getFullYear(), now.getMonth(), 1);
      end = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59, 999);
    }

    onChange({
      ...filters,
      periodPreset: preset,
      startDate: preset === 'CUSTOM' ? filters.startDate : start.toISOString().split('T')[0],
      endDate: preset === 'CUSTOM' ? filters.endDate : end.toISOString().split('T')[0],
    });
  };

  return (
    <div className="rounded-lg border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 p-4 shadow-sm space-y-4">
      {/* Linha 1: Presets de Período & Filtros de Data */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-1.5" role="group" aria-label="Filtro de período predefinido">
          <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 mr-1">Período:</span>
          {(['TODAY', 'LAST_7', 'LAST_30', 'THIS_MONTH', 'CUSTOM'] as PeriodPreset[]).map((preset) => {
            const labels: Record<PeriodPreset, string> = {
              TODAY: 'Hoje',
              LAST_7: 'Últimos 7 dias',
              LAST_30: 'Últimos 30 dias',
              THIS_MONTH: 'Mês Atual',
              CUSTOM: 'Personalizado',
            };
            const isSelected = filters.periodPreset === preset;
            return (
              <button
                key={preset}
                type="button"
                onClick={() => handlePresetClick(preset)}
                className={`min-h-[44px] px-3 py-1.5 text-xs font-medium rounded-md transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  isSelected
                    ? 'bg-blue-600 text-white font-semibold'
                    : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700'
                }`}
              >
                {labels[preset]}
              </button>
            );
          })}
        </div>

        {/* Datas Customizadas */}
        <div className="flex items-center gap-2 text-xs">
          <label htmlFor="filter-start-date" className="text-slate-500 dark:text-slate-400 font-medium">De:</label>
          <input
            id="filter-start-date"
            type="date"
            value={filters.startDate}
            onChange={(e) => onChange({ ...filters, periodPreset: 'CUSTOM', startDate: e.target.value })}
            className="min-h-[44px] rounded-md border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 px-2.5 py-1.5 text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <label htmlFor="filter-end-date" className="text-slate-500 dark:text-slate-400 font-medium">Até:</label>
          <input
            id="filter-end-date"
            type="date"
            value={filters.endDate}
            onChange={(e) => onChange({ ...filters, periodPreset: 'CUSTOM', endDate: e.target.value })}
            className="min-h-[44px] rounded-md border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 px-2.5 py-1.5 text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
      </div>

      {/* Linha 2: Dropdowns e Busca Combinada */}
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-3">
        {/* Empresa */}
        <div>
          <label htmlFor="filter-company" className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
            Empresa / CNPJ
          </label>
          <select
            id="filter-company"
            value={filters.companyId}
            onChange={(e) => onChange({ ...filters, companyId: e.target.value })}
            className="min-h-[44px] w-full rounded-md border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 px-3 py-2 text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">Todas as Empresas</option>
            {companies.map((c) => (
              <option key={c.id} value={c.id}>
                {c.corporateName}
              </option>
            ))}
          </select>
        </div>

        {/* Canal */}
        <div>
          <label htmlFor="filter-marketplace" className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
            Canal / Marketplace
          </label>
          <select
            id="filter-marketplace"
            value={filters.marketplace}
            onChange={(e) => onChange({ ...filters, marketplace: e.target.value })}
            className="min-h-[44px] w-full rounded-md border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 px-3 py-2 text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">Todos os Canais</option>
            <option value="MERCADO_LIVRE">Mercado Livre</option>
            <option value="SHOPEE">Shopee</option>
          </select>
        </div>

        {/* Conta */}
        <div>
          <label htmlFor="filter-account" className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
            Conta Conectada
          </label>
          <select
            id="filter-account"
            value={filters.sellerAccountId}
            onChange={(e) => onChange({ ...filters, sellerAccountId: e.target.value })}
            className="min-h-[44px] w-full rounded-md border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 px-3 py-2 text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">Todas as Contas</option>
            {accounts.map((acc) => (
              <option key={acc.id} value={acc.id}>
                {acc.accountName} ({acc.marketplace})
              </option>
            ))}
          </select>
        </div>

        {/* Status de Reconciliação */}
        <div>
          <label htmlFor="filter-status" className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
            Reconciliação / Status
          </label>
          <select
            id="filter-status"
            value={filters.reconciliationStatus}
            onChange={(e) => onChange({ ...filters, reconciliationStatus: e.target.value })}
            className="min-h-[44px] w-full rounded-md border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 px-3 py-2 text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="">Todos os Status</option>
            <option value="ESTIMATED">Estimado (Pendente Extrato)</option>
            <option value="RECONCILED">Reconciliado (Extrato Bateu)</option>
            <option value="DIVERGENT">Divergente (Tarifas a Maior)</option>
            <option value="MISSING_DATA">Apenas Dados Incompletos</option>
          </select>
        </div>

        {/* Busca por SKU */}
        <div>
          <label htmlFor="filter-sku" className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
            Filtrar por SKU
          </label>
          <input
            id="filter-sku"
            type="text"
            placeholder="Ex: SKU-123"
            value={filters.sku}
            onChange={(e) => onChange({ ...filters, sku: e.target.value })}
            className="min-h-[44px] w-full rounded-md border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 px-3 py-2 text-xs text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>
      </div>

      {/* Linha 3: Barra de Ação & Contadores */}
      <div className="flex items-center justify-between pt-1 border-t border-slate-100 dark:border-slate-800/80 text-xs text-slate-500">
        <div>
          {activeCount > 0 ? (
            <span className="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full bg-blue-50 dark:bg-blue-950/40 text-blue-700 dark:text-blue-300 font-medium">
              <span className="w-1.5 h-1.5 rounded-full bg-blue-600 dark:bg-blue-400"></span>
              {activeCount} {activeCount === 1 ? 'filtro ativo' : 'filtros ativos'}
            </span>
          ) : (
            <span>Nenhum filtro adicional ativo</span>
          )}
        </div>

        {activeCount > 0 && (
          <button
            type="button"
            onClick={onReset}
            className="min-h-[44px] text-xs text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-200 font-semibold underline px-2 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            Limpar Filtros
          </button>
        )}
      </div>
    </div>
  );
};
