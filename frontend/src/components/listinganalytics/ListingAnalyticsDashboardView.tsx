'use client';

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useAuth } from '@/context/AuthContext';
import {
  AccountMetricDto,
  MetricsSummaryDto,
} from '@/types/metrics';
import { MetricKpiGrid } from './MetricKpiGrid';
import { MetricTimeSeriesChart } from './MetricTimeSeriesChart';
import { MetricCatalogDrawer } from './MetricCatalogDrawer';
import { ListingAnalyticsDetailTable } from './ListingAnalyticsDetailTable';

export type PeriodPreset = 'TODAY' | '7D' | '14D' | '30D' | 'MONTH' | 'CUSTOM';

export interface SellerAccountOption {
  id: string;
  marketplace: string;
  accountName: string;
  externalAccountId: string;
  status?: string;
}

export const ListingAnalyticsDashboardView: React.FC = () => {
  const { currentTenant } = useAuth();
  const timezoneName = 'America/Sao_Paulo (UTC-03:00)';

  // Filtros de seleção
  const [marketplace, setMarketplace] = useState<string>('MERCADO_LIVRE');
  const [sellerAccountId, setSellerAccountId] = useState<string>('ALL');
  const [externalListingId, setExternalListingId] = useState<string>('');
  const [periodPreset, setPeriodPreset] = useState<PeriodPreset>('30D');
  const [isComparing, setIsComparing] = useState<boolean>(true);

  // Contas disponíveis
  const [sellerAccounts, setSellerAccounts] = useState<SellerAccountOption[]>([]);

  // Datas locais (YYYY-MM-DD)
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');

  // Estados de dados
  const [summary, setSummary] = useState<MetricsSummaryDto | null>(null);
  const [previousSummary, setPreviousSummary] = useState<MetricsSummaryDto | null>(null);
  const [metrics, setMetrics] = useState<AccountMetricDto[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  // Drawer de catálogo
  const [catalogDrawerOpen, setCatalogDrawerOpen] = useState<boolean>(false);

  // Helper para formatar data local no formato YYYY-MM-DD
  const formatYmd = (d: Date): string => {
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd}`;
  };

  // Define as datas com base no preset
  const applyPreset = useCallback((preset: PeriodPreset) => {
    setPeriodPreset(preset);
    const today = new Date();
    const endStr = formatYmd(today);

    if (preset === 'TODAY') {
      setStartDate(endStr);
      setEndDate(endStr);
    } else if (preset === '7D') {
      const past = new Date();
      past.setDate(today.getDate() - 6);
      setStartDate(formatYmd(past));
      setEndDate(endStr);
    } else if (preset === '14D') {
      const past = new Date();
      past.setDate(today.getDate() - 13);
      setStartDate(formatYmd(past));
      setEndDate(endStr);
    } else if (preset === '30D') {
      const past = new Date();
      past.setDate(today.getDate() - 29);
      setStartDate(formatYmd(past));
      setEndDate(endStr);
    } else if (preset === 'MONTH') {
      const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);
      setStartDate(formatYmd(firstDay));
      setEndDate(endStr);
    }
  }, []);

  // Inicializa o período com 30D
  useEffect(() => {
    applyPreset('30D');
  }, [applyPreset]);

  // Carrega contas de vendedores conectadas
  useEffect(() => {
    async function loadAccounts() {
      try {
        const res = await fetch('/api/catalog/seller-accounts');
        if (res.ok) {
          const data = await res.json();
          setSellerAccounts(data);
        }
      } catch {
        // Fallback silencioso
      }
    }
    loadAccounts();
  }, []);

  // Calcula o período anterior com exata mesma duração em dias
  const previousPeriod = useMemo(() => {
    if (!startDate || !endDate) return null;
    const start = new Date(`${startDate}T12:00:00Z`);
    const end = new Date(`${endDate}T12:00:00Z`);
    if (isNaN(start.getTime()) || isNaN(end.getTime()) || start > end) return null;

    const diffMs = end.getTime() - start.getTime();
    const diffDays = Math.round(diffMs / (1000 * 60 * 60 * 24)) + 1;

    const prevEnd = new Date(start);
    prevEnd.setUTCDate(prevEnd.getUTCDate() - 1);

    const prevStart = new Date(prevEnd);
    prevStart.setUTCDate(prevStart.getUTCDate() - (diffDays - 1));

    return {
      startDate: formatYmd(prevStart),
      endDate: formatYmd(prevEnd),
      days: diffDays,
    };
  }, [startDate, endDate]);

  // Busca dados de resumo e métricas detalhadas
  const fetchData = useCallback(async () => {
    if (!startDate || !endDate) return;
    setLoading(true);
    setError(null);

    const startIso = `${startDate}T00:00:00Z`;
    const endIso = `${endDate}T23:59:59Z`;

    try {
      // 1. Resumo do Período Atual
      const summaryUrl = new URL('/api/metrics/summary', window.location.origin);
      if (marketplace && marketplace !== 'ALL') summaryUrl.searchParams.set('marketplace', marketplace);
      if (sellerAccountId && sellerAccountId !== 'ALL') summaryUrl.searchParams.set('sellerAccountId', sellerAccountId);
      if (externalListingId.trim()) summaryUrl.searchParams.set('externalListingId', externalListingId.trim());
      summaryUrl.searchParams.set('startDate', startIso);
      summaryUrl.searchParams.set('endDate', endIso);

      const summaryRes = await fetch(summaryUrl.toString());
      if (summaryRes.ok) {
        const data: MetricsSummaryDto = await summaryRes.json();
        setSummary(data);
      } else {
        const err = await summaryRes.json().catch(() => ({}));
        throw new Error(err.message || 'Falha ao buscar resumo de métricas.');
      }

      // 2. Resumo do Período Anterior (se comparação ativa)
      if (isComparing && previousPeriod) {
        const prevStartIso = `${previousPeriod.startDate}T00:00:00Z`;
        const prevEndIso = `${previousPeriod.endDate}T23:59:59Z`;

        const prevSummaryUrl = new URL('/api/metrics/summary', window.location.origin);
        if (marketplace && marketplace !== 'ALL') prevSummaryUrl.searchParams.set('marketplace', marketplace);
        if (sellerAccountId && sellerAccountId !== 'ALL') prevSummaryUrl.searchParams.set('sellerAccountId', sellerAccountId);
        if (externalListingId.trim()) prevSummaryUrl.searchParams.set('externalListingId', externalListingId.trim());
        prevSummaryUrl.searchParams.set('startDate', prevStartIso);
        prevSummaryUrl.searchParams.set('endDate', prevEndIso);

        const prevRes = await fetch(prevSummaryUrl.toString());
        if (prevRes.ok) {
          const prevData: MetricsSummaryDto = await prevRes.json();
          setPreviousSummary(prevData);
        } else {
          setPreviousSummary(null);
        }
      } else {
        setPreviousSummary(null);
      }

      // 3. Lista de Métricas Individuais / Pontos da Série Temporal
      const metricsUrl = new URL('/api/metrics', window.location.origin);
      if (marketplace && marketplace !== 'ALL') metricsUrl.searchParams.set('marketplace', marketplace);
      if (sellerAccountId && sellerAccountId !== 'ALL') metricsUrl.searchParams.set('sellerAccountId', sellerAccountId);
      if (externalListingId.trim()) metricsUrl.searchParams.set('externalListingId', externalListingId.trim());
      metricsUrl.searchParams.set('startDate', startIso);
      metricsUrl.searchParams.set('endDate', endIso);
      metricsUrl.searchParams.set('size', '200');

      const metricsRes = await fetch(metricsUrl.toString());
      if (metricsRes.ok) {
        const pageData = await metricsRes.json();
        const content: AccountMetricDto[] = Array.isArray(pageData) ? pageData : pageData.content || [];
        setMetrics(content);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao processar consulta de métricas.');
    } finally {
      setLoading(false);
    }
  }, [marketplace, sellerAccountId, externalListingId, startDate, endDate, isComparing, previousPeriod]);

  // Recarrega dados ao alterar filtros essenciais
  useEffect(() => {
    fetchData();
  }, [fetchData]);

  return (
    <div className="space-y-6 animate-fadeIn pb-12">
      {/* =========================================================================
          1. HEADER DO DASHBOARD COM TIMEZONE & PERIOD EXPLICITOS
      ========================================================================= */}
      <div className="bg-white dark:bg-slate-900 rounded-2xl p-5 border border-slate-200 dark:border-slate-800 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2.5 flex-wrap">
            <h2 className="text-lg font-bold text-slate-900 dark:text-white tracking-tight">
              Analytics de Anúncios & Desempenho Oficial
            </h2>
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-50 dark:bg-blue-950/70 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
              <span className="w-2 h-2 rounded-full bg-blue-500 animate-pulse" />
              Fontes Homologadas
            </span>
          </div>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Fuso Horário Oficial:{' '}
            <strong className="text-slate-800 dark:text-slate-200 font-semibold">{timezoneName}</strong>{' '}
            &middot; Tenant: <span className="font-mono text-slate-600 dark:text-slate-400">{currentTenant?.name || 'Padrão'}</span>
          </p>
        </div>

        {/* Botões de Ação Rápida */}
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => fetchData()}
            disabled={loading}
            className="px-3 py-2 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-xs font-semibold text-slate-800 dark:text-slate-200 transition-colors flex items-center gap-1.5 min-h-[44px]"
          >
            <svg
              className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`}
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
              />
            </svg>
            Atualizar
          </button>

          <button
            type="button"
            onClick={() => setCatalogDrawerOpen(true)}
            className="px-3.5 py-2 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-semibold shadow-sm transition-colors flex items-center gap-1.5 min-h-[44px]"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
              />
            </svg>
            Catálogo Oficial & Limites
          </button>
        </div>
      </div>

      {/* =========================================================================
          2. BARRA DE FILTROS COMBINÁVEIS & SELEÇÃO DE PERÍODO / TIMEZONE
      ========================================================================= */}
      <div className="bg-white dark:bg-slate-900 rounded-2xl p-5 border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3.5">
          {/* Marketplace / Canal */}
          <div>
            <label htmlFor="marketplace-select" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Marketplace / Canal
            </label>
            <select
              id="marketplace-select"
              value={marketplace}
              onChange={(e) => setMarketplace(e.target.value)}
              className="w-full text-xs bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg px-3 py-2 text-slate-800 dark:text-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[40px]"
            >
              <option value="MERCADO_LIVRE">Mercado Livre</option>
              <option value="SHOPEE">Shopee OpenAPI v2</option>
              <option value="ALL">Todos os Canais</option>
            </select>
          </div>

          {/* Conta Conectada */}
          <div>
            <label htmlFor="account-select" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Conta Conectada
            </label>
            <select
              id="account-select"
              value={sellerAccountId}
              onChange={(e) => setSellerAccountId(e.target.value)}
              className="w-full text-xs bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg px-3 py-2 text-slate-800 dark:text-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[40px]"
            >
              <option value="ALL">Todas as Contas ({sellerAccounts.length})</option>
              {sellerAccounts.map((acc) => (
                <option key={acc.id} value={acc.id}>
                  {acc.accountName} ({acc.marketplace})
                </option>
              ))}
            </select>
          </div>

          {/* Anúncio (External Listing ID) */}
          <div>
            <label htmlFor="listing-input" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Código do Anúncio (Opcional)
            </label>
            <div className="relative">
              <input
                id="listing-input"
                type="text"
                placeholder="Ex: MLB1234567890 ou deixe vazio"
                value={externalListingId}
                onChange={(e) => setExternalListingId(e.target.value)}
                className="w-full text-xs bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg px-3 py-2 text-slate-800 dark:text-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[40px]"
              />
              {externalListingId && (
                <button
                  type="button"
                  onClick={() => setExternalListingId('')}
                  aria-label="Limpar filtro de anúncio"
                  className="absolute right-2.5 top-2.5 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 text-xs"
                >
                  ✕
                </button>
              )}
            </div>
          </div>

          {/* Toggle de Comparação Temporal */}
          <div className="flex flex-col justify-end">
            <div className="flex items-center justify-between p-2 rounded-lg bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 min-h-[40px]">
              <span className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                Comparar Período Anterior
              </span>
              <button
                type="button"
                role="switch"
                aria-checked={isComparing}
                aria-label="Alternar comparação temporal contra período anterior"
                onClick={() => setIsComparing(!isComparing)}
                className={`relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  isComparing ? 'bg-blue-600' : 'bg-slate-300 dark:bg-slate-700'
                }`}
              >
                <span
                  className={`pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                    isComparing ? 'translate-x-4' : 'translate-x-0'
                  }`}
                />
              </button>
            </div>
          </div>
        </div>

        {/* Linha de Seleção de Período e Presets Rápidos */}
        <div className="pt-3 border-t border-slate-100 dark:border-slate-800 flex flex-col md:flex-row md:items-center justify-between gap-3 text-xs">
          {/* Presets Rápidos */}
          <div className="flex items-center gap-1.5 flex-wrap">
            <span className="text-slate-500 dark:text-slate-400 font-semibold mr-1">Presets:</span>
            {(['TODAY', '7D', '14D', '30D', 'MONTH'] as PeriodPreset[]).map((p) => {
              const label =
                p === 'TODAY'
                  ? 'Hoje'
                  : p === '7D'
                  ? '7 Dias'
                  : p === '14D'
                  ? '14 Dias'
                  : p === '30D'
                  ? '30 Dias'
                  : 'Mês Atual';
              return (
                <button
                  key={p}
                  type="button"
                  onClick={() => applyPreset(p)}
                  className={`px-2.5 py-1.5 rounded-lg font-medium transition-colors min-h-[36px] ${
                    periodPreset === p
                      ? 'bg-blue-600 text-white font-semibold shadow-xs'
                      : 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 hover:bg-slate-200'
                  }`}
                >
                  {label}
                </button>
              );
            })}
          </div>

          {/* Inputs de Data Explicitos com Timezone */}
          <div className="flex items-center gap-2 flex-wrap">
            <div className="flex items-center gap-1">
              <label htmlFor="start-date" className="text-slate-500 dark:text-slate-400 font-medium">De:</label>
              <input
                id="start-date"
                type="date"
                value={startDate}
                onChange={(e) => {
                  setStartDate(e.target.value);
                  setPeriodPreset('CUSTOM');
                }}
                className="text-xs bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg px-2 py-1 text-slate-800 dark:text-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[36px]"
              />
            </div>

            <div className="flex items-center gap-1">
              <label htmlFor="end-date" className="text-slate-500 dark:text-slate-400 font-medium">Até:</label>
              <input
                id="end-date"
                type="date"
                value={endDate}
                onChange={(e) => {
                  setEndDate(e.target.value);
                  setPeriodPreset('CUSTOM');
                }}
                className="text-xs bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg px-2 py-1 text-slate-800 dark:text-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[36px]"
              />
            </div>

            <span className="text-[11px] text-slate-400 font-mono hidden lg:inline">
              [{timezoneName.split(' ')[0]}]
            </span>
          </div>
        </div>

        {/* Exibição da Janela Comparativa */}
        {isComparing && previousPeriod && (
          <div className="p-2.5 rounded-lg bg-blue-50/60 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-900/60 text-[11px] text-blue-900 dark:text-blue-200 flex items-center justify-between flex-wrap gap-2">
            <div>
              <strong>Janela Atual:</strong> {startDate} a {endDate} ({previousPeriod.days} dias)
              <span className="mx-2">&middot;</span>
              <strong>Período Anterior Equivalente:</strong> {previousPeriod.startDate} a {previousPeriod.endDate} ({previousPeriod.days} dias)
            </div>
            <span className="text-[10px] text-blue-600 dark:text-blue-400 font-semibold uppercase">
              Comparação 1:1 Sem Interpolação
            </span>
          </div>
        )}
      </div>

      {/* =========================================================================
          3. BANNER ANTI-ALUCINAÇÃO & CONFORMIDADE OFICIAL
      ========================================================================= */}
      <div
        role="region"
        aria-label="Aviso de Conformidade e Anti-Alucinação"
        className="p-3.5 rounded-xl bg-slate-100 dark:bg-slate-800/80 border border-slate-300 dark:border-slate-700 text-xs text-slate-700 dark:text-slate-300 flex items-start gap-3 shadow-xs"
      >
        <div className="p-1 rounded-md bg-blue-100 dark:bg-blue-950 text-blue-600 dark:text-blue-400 shrink-0 mt-0.5">
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
          </svg>
        </div>
        <div className="space-y-1">
          <div className="font-bold text-slate-900 dark:text-white">
            Diretriz de Transparência e Anti-Alucinação Estrita
          </div>
          <p className="leading-relaxed">
            Todas as métricas são consumidas exclusivamente de endpoints autenticados e oficiais de cada marketplace.
            Métricas não fornecidas pelo canal (como <strong>CTR no tráfego orgânico do Mercado Livre</strong> ou números privados de concorrentes)
            são categorizadas como <code>UNSUPPORTED_BY_CHANNEL</code> com valor nulo, sem preenchimento silencioso por estimativas.
          </p>
        </div>
      </div>

      {/* Alerta de Erro na Busca */}
      {error && (
        <div className="p-4 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 text-xs text-rose-700 dark:text-rose-300">
          <strong>Erro ao sincronizar dados:</strong> {error}
        </div>
      )}

      {/* =========================================================================
          4. GRADE DE CARDS DE KPIS COM COMPARAÇÃO TEMPORAL
      ========================================================================= */}
      <MetricKpiGrid
        summary={summary}
        previousSummary={previousSummary}
        isComparing={isComparing}
        marketplace={marketplace}
        onOpenCatalog={() => setCatalogDrawerOpen(true)}
      />

      {/* =========================================================================
          5. SÉRIE TEMPORAL COM INDICAÇÃO EXPLÍCITA DE LACUNAS
      ========================================================================= */}
      <MetricTimeSeriesChart
        metrics={metrics}
        startDate={startDate}
        endDate={endDate}
        timezoneName={timezoneName}
      />

      {/* =========================================================================
          6. TABELA AUDITÁVEL DE REGISTROS DE MÉTRICAS
      ========================================================================= */}
      <ListingAnalyticsDetailTable
        metrics={metrics}
        loading={loading}
        timezoneName={timezoneName}
      />

      {/* =========================================================================
          7. DRAWER LATERAL DE CAPACIDADES DO CATÁLOGO OFICIAL
      ========================================================================= */}
      <MetricCatalogDrawer
        isOpen={catalogDrawerOpen}
        onClose={() => setCatalogDrawerOpen(false)}
        marketplace={marketplace}
      />
    </div>
  );
};
