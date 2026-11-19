'use client';

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { FinancialFactSummary, OrderFinancialFact, FinancialFilterState } from '@/types/financial';
import { FinancialKpiCards } from './FinancialKpiCards';
import { FinancialFiltersBar } from './FinancialFiltersBar';
import { FinancialFactsTable } from './FinancialFactsTable';
import { FinancialFactDetailDrawer } from './FinancialFactDetailDrawer';

export const FinancialFactsDashboardView: React.FC = () => {
  const [summary, setSummary] = useState<FinancialFactSummary | null>(null);
  const [facts, setFacts] = useState<OrderFinancialFact[]>([]);
  const [companies, setCompanies] = useState<Array<{ id: string; corporateName: string }>>([]);
  const [accounts, setAccounts] = useState<Array<{ id: string; accountName: string; marketplace: string }>>([]);
  const [isLoadingSummary, setIsLoadingSummary] = useState(true);
  const [isLoadingFacts, setIsLoadingFacts] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Estado dos Filtros Combináveis
  const [filters, setFilters] = useState<FinancialFilterState>(() => {
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - 30);
    return {
      periodPreset: 'LAST_30',
      startDate: start.toISOString().split('T')[0],
      endDate: end.toISOString().split('T')[0],
      companyId: '',
      marketplace: '',
      sellerAccountId: '',
      sku: '',
      reconciliationStatus: '',
    };
  });

  // Gaveta de Detalhe
  const [selectedFact, setSelectedFact] = useState<OrderFinancialFact | null>(null);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);

  // Carrega lista de empresas e contas para os selects dos filtros
  useEffect(() => {
    async function loadAuxiliaryData() {
      try {
        const [compRes, accRes] = await Promise.all([
          fetch('/api/companies?activeOnly=true&size=100').catch(() => null),
          fetch('/api/catalog/seller-accounts').catch(() => null),
        ]);

        if (compRes && compRes.ok) {
          const compData = await compRes.json();
          setCompanies(compData.content || compData || []);
        }

        if (accRes && accRes.ok) {
          const accData = await accRes.json();
          setAccounts(accData.accounts || accData || []);
        }
      } catch {
        // Fallback gracioso
      }
    }
    loadAuxiliaryData();
  }, []);

  // Busca do Resumo (Summary)
  const fetchSummary = useCallback(async () => {
    setIsLoadingSummary(true);
    try {
      const params = new URLSearchParams();
      if (filters.companyId) params.set('companyId', filters.companyId);
      if (filters.marketplace) params.set('marketplace', filters.marketplace);
      if (filters.startDate) params.set('startDate', new Date(filters.startDate).toISOString());
      if (filters.endDate) {
        const end = new Date(filters.endDate);
        end.setHours(23, 59, 59, 999);
        params.set('endDate', end.toISOString());
      }

      const res = await fetch(`/api/financial-facts/summary?${params.toString()}`);
      if (!res.ok) throw new Error('Falha ao carregar métricas consolidadas.');
      const data = await res.json();
      setSummary(data);
    } catch (err: unknown) {
      console.warn('Erro ao carregar resumo:', err);
    } finally {
      setIsLoadingSummary(false);
    }
  }, [filters.companyId, filters.marketplace, filters.startDate, filters.endDate]);

  // Busca dos Fatos (Orders List)
  const fetchFacts = useCallback(async () => {
    setIsLoadingFacts(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      if (filters.companyId) params.set('companyId', filters.companyId);
      if (filters.marketplace) params.set('marketplace', filters.marketplace);
      if (filters.reconciliationStatus && filters.reconciliationStatus !== 'MISSING_DATA') {
        params.set('status', filters.reconciliationStatus);
      }
      if (filters.startDate) params.set('startDate', new Date(filters.startDate).toISOString());
      if (filters.endDate) {
        const end = new Date(filters.endDate);
        end.setHours(23, 59, 59, 999);
        params.set('endDate', end.toISOString());
      }
      params.set('size', '100');

      const res = await fetch(`/api/financial-facts?${params.toString()}`);
      if (!res.ok) throw new Error('Falha ao carregar fatos financeiros dos pedidos.');
      const data = await res.json();
      setFacts(data.content || data || []);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Erro ao buscar dados.');
    } finally {
      setIsLoadingFacts(false);
    }
  }, [filters.companyId, filters.marketplace, filters.reconciliationStatus, filters.startDate, filters.endDate]);

  useEffect(() => {
    fetchSummary();
    fetchFacts();
  }, [fetchSummary, fetchFacts]);

  // Filtros combinados no cliente (SKU, conta, dados incompletos)
  const filteredFacts = useMemo(() => {
    return facts.filter((fact) => {
      // Filtro de Conta
      if (filters.sellerAccountId && fact.sellerAccountId !== filters.sellerAccountId) {
        return false;
      }

      // Filtro de Dados Incompletos
      if (filters.reconciliationStatus === 'MISSING_DATA' && !fact.hasMissingData) {
        return false;
      }

      // Filtro de SKU
      if (filters.sku.trim()) {
        const searchSku = filters.sku.toLowerCase().trim();
        const matchesOrder = fact.externalOrderId.toLowerCase().includes(searchSku);
        const matchesItemSku = fact.items?.some((i) =>
          (i.sku || '').toLowerCase().includes(searchSku) || i.title.toLowerCase().includes(searchSku)
        );
        if (!matchesOrder && !matchesItemSku) {
          return false;
        }
      }

      return true;
    });
  }, [facts, filters.sellerAccountId, filters.reconciliationStatus, filters.sku]);

  // Contagem de filtros ativos
  const activeFiltersCount = useMemo(() => {
    let count = 0;
    if (filters.companyId) count++;
    if (filters.marketplace) count++;
    if (filters.sellerAccountId) count++;
    if (filters.sku.trim()) count++;
    if (filters.reconciliationStatus) count++;
    if (filters.periodPreset === 'CUSTOM') count++;
    return count;
  }, [filters]);

  const handleResetFilters = () => {
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - 30);
    setFilters({
      periodPreset: 'LAST_30',
      startDate: start.toISOString().split('T')[0],
      endDate: end.toISOString().split('T')[0],
      companyId: '',
      marketplace: '',
      sellerAccountId: '',
      sku: '',
      reconciliationStatus: '',
    });
  };

  const handleOpenDetail = (fact: OrderFinancialFact) => {
    setSelectedFact(fact);
    setIsDrawerOpen(true);
  };

  const handleFactUpdated = (updated: OrderFinancialFact) => {
    setSelectedFact(updated);
    setFacts((prev) => prev.map((f) => (f.id === updated.id ? updated : f)));
    fetchSummary();
  };

  return (
    <section aria-label="Painel Financeiro e Conciliação" className="space-y-6">
      {/* Cabeçalho da Tela */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 border-b border-slate-200 dark:border-slate-800 pb-4">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
            Painel Financeiro & Conciliação
          </h1>
          <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
            Fatos financeiros por pedido e item, margem de contribuição, conferência de extratos e auditoria.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => {
              fetchSummary();
              fetchFacts();
            }}
            aria-label="Atualizar dados financeiros"
            className="min-h-[44px] px-3.5 py-2 text-xs font-semibold rounded-md border border-slate-300 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300 transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            ↻ Atualizar
          </button>
        </div>
      </div>

      {/* Erro Geral */}
      {error && (
        <div role="alert" className="p-4 rounded-lg bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900 text-xs text-rose-800 dark:text-rose-200 flex justify-between items-center">
          <span>{error}</span>
          <button onClick={() => fetchFacts()} className="underline font-bold text-rose-900 dark:text-rose-100 min-h-[44px] px-2">
            Tentar novamente
          </button>
        </div>
      )}

      {/* Cards de KPIs Consolidados */}
      <FinancialKpiCards
        summary={summary}
        isLoading={isLoadingSummary}
        onFilterMissingData={() => setFilters({ ...filters, reconciliationStatus: 'MISSING_DATA' })}
      />

      {/* Barra de Filtros Combináveis */}
      <FinancialFiltersBar
        filters={filters}
        onChange={setFilters}
        onReset={handleResetFilters}
        companies={companies}
        accounts={accounts}
        activeCount={activeFiltersCount}
      />

      {/* Tabela de Fatos Financeiros */}
      <div>
        <div className="flex items-center justify-between mb-2">
          <h2 className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
            Fatos Financeiros por Pedido ({filteredFacts.length} {filteredFacts.length === 1 ? 'pedido' : 'pedidos'})
          </h2>
        </div>

        <FinancialFactsTable
          facts={filteredFacts}
          isLoading={isLoadingFacts}
          onSelectFact={handleOpenDetail}
        />
      </div>

      {/* Gaveta de Detalhamento do Pedido */}
      <FinancialFactDetailDrawer
        fact={selectedFact}
        isOpen={isDrawerOpen}
        onClose={() => setIsDrawerOpen(false)}
        onFactUpdated={handleFactUpdated}
      />
    </section>
  );
};
