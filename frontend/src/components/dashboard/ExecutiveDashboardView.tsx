'use client';

import React, { useState, useEffect, useCallback } from 'react';
import {
  AggregatedKpiSummary,
  ChannelKpiAggregate,
  PeriodicKpiAggregate,
  CompanyKpiAggregate,
  KpiDefinition,
  KpiGranularity,
  DashboardPeriodPreset,
  DashboardFilterState,
} from '@/types/kpis';
import { ExecutiveKpiCards } from './ExecutiveKpiCards';
import { ChannelPerformanceCard } from './ChannelPerformanceCard';
import { PeriodicTrendChart } from './PeriodicTrendChart';
import { DataQualityAlerts } from './DataQualityAlerts';
import { StaleDataBanner } from './StaleDataBanner';
import { KpiDefinitionsDrawer } from './KpiDefinitionsDrawer';

export const ExecutiveDashboardView: React.FC = () => {
  // Estado dos Dados
  const [summary, setSummary] = useState<AggregatedKpiSummary | null>(null);
  const [channels, setChannels] = useState<ChannelKpiAggregate[]>([]);
  const [periods, setPeriods] = useState<PeriodicKpiAggregate[]>([]);
  const [companies, setCompanies] = useState<CompanyKpiAggregate[]>([]);
  const [definitions, setDefinitions] = useState<KpiDefinition[]>([]);

  // Estados de Controle de Interface
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);
  const [granularity, setGranularity] = useState<KpiGranularity>('DAILY');

  // Gaveta de Definições
  const [isDrawerOpen, setIsDrawerOpen] = useState<boolean>(false);
  const [selectedKpiCode, setSelectedKpiCode] = useState<string | null>(null);

  // Filtros Combináveis
  const [filters, setFilters] = useState<DashboardFilterState>(() => {
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
    };
  });

  // Função para carregar os dados agregados
  const loadDashboardData = useCallback(async (isSilentRefresh = false) => {
    if (!isSilentRefresh) {
      setIsLoading(true);
    }
    setError(null);

    const params = new URLSearchParams();
    if (filters.startDate) params.set('startDate', `${filters.startDate}T00:00:00Z`);
    if (filters.endDate) params.set('endDate', `${filters.endDate}T23:59:59Z`);
    if (filters.companyId) params.set('companyId', filters.companyId);
    if (filters.marketplace) params.set('marketplace', filters.marketplace);
    if (filters.sellerAccountId) params.set('sellerAccountId', filters.sellerAccountId);

    const periodParams = new URLSearchParams(params);
    periodParams.set('granularity', granularity);

    try {
      const [summaryRes, channelRes, periodRes, companyRes, defsRes] = await Promise.all([
        fetch(`/api/analytics/kpis/summary?${params.toString()}`),
        fetch(`/api/analytics/kpis/by-channel?${params.toString()}`),
        fetch(`/api/analytics/kpis/by-period?${periodParams.toString()}`),
        fetch(`/api/analytics/kpis/by-company?${params.toString()}`),
        fetch('/api/analytics/kpis/definitions'),
      ]);

      if (!summaryRes.ok) {
        const errJson = await summaryRes.json().catch(() => ({}));
        throw new Error(errJson.message || 'Falha ao carregar indicadores agregados.');
      }

      const [summaryData, channelData, periodData, companyData, defsData] = await Promise.all([
        summaryRes.json(),
        channelRes.ok ? channelRes.json() : [],
        periodRes.ok ? periodRes.json() : [],
        companyRes.ok ? companyRes.json() : [],
        defsRes.ok ? defsRes.json() : [],
      ]);

      setSummary(summaryData);
      setChannels(Array.isArray(channelData) ? channelData : []);
      setPeriods(Array.isArray(periodData) ? periodData : []);
      setCompanies(Array.isArray(companyData) ? companyData : []);
      setDefinitions(Array.isArray(defsData) ? defsData : summaryData?.kpiDefinitions || []);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Erro inesperado ao carregar dados do dashboard.';
      setError(msg);
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  }, [filters, granularity]);

  useEffect(() => {
    loadDashboardData();
  }, [loadDashboardData]);

  // Forçar invalidação e revalidação sob demanda
  const handleRefresh = async () => {
    setIsRefreshing(true);
    try {
      await fetch('/api/analytics/kpis/cache/invalidate', { method: 'POST' });
    } catch {
      // Ignora falha de invalidação e prossegue com re-fetch
    }
    await loadDashboardData(true);
  };

  // Preset de período
  const handlePresetChange = (preset: DashboardPeriodPreset) => {
    const end = new Date();
    const start = new Date();

    if (preset === 'LAST_7') {
      start.setDate(end.getDate() - 7);
    } else if (preset === 'LAST_30') {
      start.setDate(end.getDate() - 30);
    } else if (preset === 'LAST_90') {
      start.setDate(end.getDate() - 90);
    }

    setFilters((prev) => ({
      ...prev,
      periodPreset: preset,
      ...(preset !== 'CUSTOM'
        ? {
            startDate: start.toISOString().split('T')[0],
            endDate: end.toISOString().split('T')[0],
          }
        : {}),
    }));
  };

  const openDefinitionsFor = (code?: string) => {
    setSelectedKpiCode(code || null);
    setIsDrawerOpen(true);
  };

  const isEmpty = !isLoading && !error && summary?.totalOrders === 0;

  return (
    <div
      role="region"
      aria-label="Painel Executivo de Indicadores e KPIs"
      className="space-y-6 max-w-7xl mx-auto overflow-x-hidden pb-12 select-text"
    >
      {/* =========================================================================
          1. HEADER & BARRA DE FILTROS COMBINÁVEIS
      ========================================================================= */}
      <header className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-200 dark:border-slate-800">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-white tracking-tight">
            Dashboard Executivo
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            Métricas de vendas, margem de contribuição determinística e integridade contábil
          </p>
        </div>

        {/* Botão de Glossário e Fórmulas */}
        <div className="flex items-center gap-2 self-start sm:self-auto">
          <button
            type="button"
            onClick={() => openDefinitionsFor()}
            aria-label="Abrir catálogo com definições e fórmulas de KPIs"
            className="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-700 text-xs font-medium transition-colors min-h-[44px]"
          >
            <svg className="w-4 h-4 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
            </svg>
            Catálogo de Fórmulas (18)
          </button>
        </div>
      </header>

      {/* Barra de Filtros */}
      <section
        aria-label="Filtros do Dashboard"
        className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm space-y-3"
      >
        <div className="flex flex-wrap items-center justify-between gap-3">
          {/* Presets Rápidos */}
          <div
            role="group"
            aria-label="Presets de período"
            className="flex flex-wrap items-center gap-1"
          >
            {[
              { id: 'LAST_7', label: '7 Dias' },
              { id: 'LAST_30', label: '30 Dias' },
              { id: 'LAST_90', label: '90 Dias' },
              { id: 'CUSTOM', label: 'Personalizado' },
            ].map((p) => (
              <button
                key={p.id}
                type="button"
                onClick={() => handlePresetChange(p.id as DashboardPeriodPreset)}
                aria-pressed={filters.periodPreset === p.id}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors min-h-[38px] ${
                  filters.periodPreset === p.id
                    ? 'bg-blue-600 text-white font-semibold'
                    : 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700'
                }`}
              >
                {p.label}
              </button>
            ))}
          </div>

          {/* Filtro de Canal */}
          <div className="flex items-center gap-2">
            <label htmlFor="marketplace-select" className="text-xs font-medium text-slate-600 dark:text-slate-400">
              Canal:
            </label>
            <select
              id="marketplace-select"
              value={filters.marketplace}
              onChange={(e) => setFilters((prev) => ({ ...prev, marketplace: e.target.value }))}
              className="px-2.5 py-1.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white text-xs font-medium min-h-[38px] focus:ring-2 focus:ring-blue-500"
            >
              <option value="">Todos os canais</option>
              <option value="MERCADO_LIVRE">Mercado Livre</option>
              <option value="SHOPEE">Shopee</option>
            </select>
          </div>
        </div>

        {/* Datas Personalizadas */}
        {filters.periodPreset === 'CUSTOM' && (
          <div className="flex flex-wrap items-center gap-3 pt-2 border-t border-slate-100 dark:border-slate-800">
            <div className="flex items-center gap-2">
              <label htmlFor="start-date" className="text-xs text-slate-600 dark:text-slate-400">
                Início:
              </label>
              <input
                id="start-date"
                type="date"
                value={filters.startDate}
                onChange={(e) => setFilters((prev) => ({ ...prev, startDate: e.target.value }))}
                className="px-2.5 py-1.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white text-xs min-h-[38px]"
              />
            </div>
            <div className="flex items-center gap-2">
              <label htmlFor="end-date" className="text-xs text-slate-600 dark:text-slate-400">
                Fim:
              </label>
              <input
                id="end-date"
                type="date"
                value={filters.endDate}
                onChange={(e) => setFilters((prev) => ({ ...prev, endDate: e.target.value }))}
                className="px-2.5 py-1.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white text-xs min-h-[38px]"
              />
            </div>
          </div>
        )}
      </section>

      {/* =========================================================================
          2. STALE DATA & STATUS DE SINCRONIZAÇÃO
      ========================================================================= */}
      <StaleDataBanner
        calculatedAt={summary?.calculatedAt}
        isCached={Boolean(summary?.cached)}
        isRefreshing={isRefreshing}
        onRefresh={handleRefresh}
      />

      {/* =========================================================================
          3. SALVAGUARDA ANTI-ALUCINAÇÃO
      ========================================================================= */}
      <section
        role="region"
        aria-label="Aviso Anti-Alucinação e Conformidade Contábil"
        className="p-3.5 rounded-xl border border-blue-200 dark:border-blue-900/50 bg-blue-50/60 dark:bg-blue-950/20 flex items-start gap-3 text-xs"
      >
        <svg className="w-5 h-5 text-blue-600 dark:text-blue-400 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
        </svg>
        <p className="text-blue-900 dark:text-blue-200 text-[11px] leading-relaxed">
          <strong>Salvaguarda Contábil:</strong> {summary?.antiHallucinationDisclaimer || 'Este demonstrativo apura estritamente a Margem de Contribuição (Receita Líquida deduzida de custos diretos variáveis, comissões, fretes e impostos informados). Despesas fixas estruturais e operacionais da empresa não estão contempladas, sendo proibido rotular este valor como Lucro Líquido.'}
        </p>
      </section>

      {/* =========================================================================
          4. ESTADO DE ERRO (RFC 7807)
      ========================================================================= */}
      {error && (
        <section
          role="alert"
          className="p-5 rounded-xl border border-rose-300 dark:border-rose-700/60 bg-rose-50 dark:bg-rose-950/30 text-xs space-y-3"
        >
          <div className="flex items-start gap-3">
            <svg className="w-5 h-5 text-rose-600 dark:text-rose-400 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 018 0z" />
            </svg>
            <div>
              <h3 className="font-bold text-rose-900 dark:text-rose-200">
                Erro ao Carregar Dashboard
              </h3>
              <p className="text-rose-800 dark:text-rose-300 text-[11px] mt-0.5">
                {error}
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => loadDashboardData()}
            className="px-4 py-2 rounded-lg bg-rose-600 hover:bg-rose-700 text-white font-semibold text-xs min-h-[44px]"
          >
            Tentar Novamente
          </button>
        </section>
      )}

      {/* =========================================================================
          5. ALERTAS DE QUALIDADE DOS DADOS
      ========================================================================= */}
      <DataQualityAlerts summary={summary} isLoading={isLoading} />

      {/* =========================================================================
          6. CARDS EXECUTIVOS DE KPIS
      ========================================================================= */}
      <ExecutiveKpiCards
        summary={summary}
        isLoading={isLoading}
        onOpenDefinitions={openDefinitionsFor}
      />

      {/* =========================================================================
          7. ESTADO VAZIO (EMPTY STATE)
      ========================================================================= */}
      {isEmpty && (
        <section
          role="region"
          aria-label="Nenhum dado encontrado"
          className="p-12 text-center rounded-xl border border-dashed border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 space-y-3"
        >
          <div className="w-12 h-12 mx-auto rounded-full bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-slate-400">
            <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
            </svg>
          </div>
          <h3 className="font-bold text-sm text-slate-900 dark:text-white">
            Nenhum pedido ou fato financeiro apurado
          </h3>
          <p className="text-xs text-slate-500 dark:text-slate-400 max-w-sm mx-auto">
            Não encontramos movimentações para os filtros e intervalo selecionados. Ajuste o período ou verifique a conexão com as contas dos canais.
          </p>
        </section>
      )}

      {/* =========================================================================
          8. GRID 2 COLUNAS: DESEMPENHO POR CANAL & EVOLUÇÃO TEMPORAL
      ========================================================================= */}
      {!isEmpty && (
        <section
          aria-label="Desempenho Comparativo e Séries Temporais"
          className="grid grid-cols-1 lg:grid-cols-2 gap-6"
        >
          <ChannelPerformanceCard
            channels={channels}
            isLoading={isLoading}
          />
          <PeriodicTrendChart
            periods={periods}
            granularity={granularity}
            onGranularityChange={setGranularity}
            isLoading={isLoading}
          />
        </section>
      )}

      {/* =========================================================================
          9. GAVETA DE DEFINIÇÕES DOS KPIS
      ========================================================================= */}
      <KpiDefinitionsDrawer
        isOpen={isDrawerOpen}
        onClose={() => setIsDrawerOpen(false)}
        definitions={definitions}
        selectedCode={selectedKpiCode}
      />
    </div>
  );
};
