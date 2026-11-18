'use client';

import React, { useState, useEffect, useCallback } from 'react';
import {
  ObservationDetailDrawer,
  MarketObservationItem,
  MarketObservationSourceType,
} from './ObservationDetailDrawer';
import { MarketSearchView } from './MarketSearchView';

export function MarketIntelligenceView() {
  const [observations, setObservations] = useState<MarketObservationItem[]>([]);
  const [selectedObservation, setSelectedObservation] = useState<MarketObservationItem | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filtros
  const [activeTab, setActiveTab] = useState<'observations' | 'search'>('observations');
  const [sourceTypeFilter, setSourceTypeFilter] = useState<string>('ALL');
  const [marketplaceFilter, setMarketplaceFilter] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState<string>('');

  // Paginação
  const [page, setPage] = useState(0);
  const [size] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const fetchObservations = useCallback(async () => {
    setIsLoading(true);
    setError(null);

    const params = new URLSearchParams();
    params.set('page', page.toString());
    params.set('size', size.toString());

    if (sourceTypeFilter !== 'ALL') params.set('sourceType', sourceTypeFilter);
    if (marketplaceFilter !== 'ALL') params.set('marketplace', marketplaceFilter);
    if (searchTerm.trim()) params.set('search', searchTerm.trim());

    try {
      const res = await fetch(`/api/market-intelligence/observations?${params.toString()}`);
      if (!res.ok) {
        throw new Error('Falha ao carregar observações de inteligência de mercado.');
      }
      const data = await res.json();
      setObservations(data.items || []);
      setTotalElements(data.totalElements || 0);
      setTotalPages(data.totalPages || 1);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Erro ao consultar observações.');
    } finally {
      setIsLoading(false);
    }
  }, [page, size, sourceTypeFilter, marketplaceFilter, searchTerm]);

  useEffect(() => {
    fetchObservations();
  }, [fetchObservations]);

  const handleFilterSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchObservations();
  };

  const handleResetFilters = () => {
    setSourceTypeFilter('ALL');
    setMarketplaceFilter('ALL');
    setSearchTerm('');
    setPage(0);
  };

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val);
  };

  const formatDate = (dateStr: string) => {
    try {
      return new Date(dateStr).toLocaleString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return dateStr;
    }
  };

  // KPIs de Proveniência Calculados
  const officialCount = observations.filter((o) => o.sourceType === 'OFFICIAL').length;
  const observedCount = observations.filter((o) => o.sourceType === 'OBSERVED').length;
  const estimatedCount = observations.filter((o) => o.sourceType === 'ESTIMATED').length;
  const avgConfidence =
    observations.length > 0
      ? observations.reduce((acc, curr) => acc + (curr.confidence || 1), 0) / observations.length
      : 1.0;

  const renderBadge = (type: MarketObservationSourceType) => {
    switch (type) {
      case 'OFFICIAL':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300 border border-emerald-300 dark:border-emerald-700">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
            Oficial
          </span>
        );
      case 'OBSERVED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-100 text-blue-800 dark:bg-blue-950/60 dark:text-blue-300 border border-blue-300 dark:border-blue-700">
            <span className="w-1.5 h-1.5 rounded-full bg-blue-500"></span>
            Observado
          </span>
        );
      case 'ESTIMATED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300 border border-amber-400 dark:border-amber-600">
            <span className="w-1.5 h-1.5 rounded-full bg-amber-500"></span>
            Estimado (Modelo)
          </span>
        );
      case 'USER_INPUT':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-semibold bg-purple-100 text-purple-800 dark:bg-purple-950/60 dark:text-purple-300 border border-purple-300 dark:border-purple-700">
            <span className="w-1.5 h-1.5 rounded-full bg-purple-500"></span>
            Manual
          </span>
        );
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Banner / Título */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
            Inteligência de Mercado & Proveniência
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Rastreamento de evidências públicas e oficiais com explicabilidade e diferenciação estrita de dados estimados.
          </p>
        </div>
      </div>

      {/* Abas de Navegação */}
      <div className="flex border-b border-slate-200 dark:border-slate-800 gap-2">
        <button
          type="button"
          onClick={() => setActiveTab('observations')}
          className={`px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors min-h-[44px] ${
            activeTab === 'observations'
              ? 'border-blue-600 text-blue-600 dark:border-blue-400 dark:text-blue-400'
              : 'border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
          }`}
        >
          📊 Observações & Matching
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('search')}
          className={`px-4 py-2.5 text-sm font-semibold border-b-2 transition-colors min-h-[44px] ${
            activeTab === 'search'
              ? 'border-blue-600 text-blue-600 dark:border-blue-400 dark:text-blue-400'
              : 'border-transparent text-slate-500 hover:text-slate-800 dark:hover:text-slate-200'
          }`}
        >
          🔍 Pesquisa de Mercado & Concorrência
        </button>
      </div>

      {activeTab === 'search' ? (
        <MarketSearchView />
      ) : (
        <>
          {/* KPI Cards de Proveniência */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
          <span className="text-xs font-medium text-slate-500 dark:text-slate-400">Total Observações</span>
          <div className="mt-1 flex items-baseline justify-between">
            <span className="text-2xl font-bold text-slate-900 dark:text-white">{totalElements}</span>
            <span className="text-xs text-slate-400">Registros</span>
          </div>
        </div>

        <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
          <span className="text-xs font-medium text-emerald-600 dark:text-emerald-400">Dados Oficiais</span>
          <div className="mt-1 flex items-baseline justify-between">
            <span className="text-2xl font-bold text-emerald-700 dark:text-emerald-400">{officialCount}</span>
            <span className="text-xs text-emerald-600/70">Canais Conectados</span>
          </div>
        </div>

        <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
          <span className="text-xs font-medium text-amber-600 dark:text-amber-400">Dados Estimados</span>
          <div className="mt-1 flex items-baseline justify-between">
            <span className="text-2xl font-bold text-amber-700 dark:text-amber-400">{estimatedCount}</span>
            <span className="text-xs text-amber-600/70">Modelos Auditáveis</span>
          </div>
        </div>

        <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
          <span className="text-xs font-medium text-blue-600 dark:text-blue-400">Confiabilidade Média</span>
          <div className="mt-1 flex items-baseline justify-between">
            <span className="text-2xl font-bold text-blue-700 dark:text-blue-400">
              {(avgConfidence * 100).toFixed(0)}%
            </span>
            <span className="text-xs text-blue-600/70">Score Proveniência</span>
          </div>
        </div>
      </div>

      {/* Formulário de Filtros */}
      <form
        onSubmit={handleFilterSubmit}
        className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm space-y-4"
      >
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div>
            <label htmlFor="source-type-filter" className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
              Classificação / Evidência
            </label>
            <select
              id="source-type-filter"
              value={sourceTypeFilter}
              onChange={(e) => setSourceTypeFilter(e.target.value)}
              className="w-full h-11 px-3 text-sm bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500"
            >
              <option value="ALL">Todas as Fontes</option>
              <option value="OFFICIAL">Oficial (Canais Conectados)</option>
              <option value="OBSERVED">Público Observado</option>
              <option value="ESTIMATED">Estimado (Metodologia)</option>
              <option value="USER_INPUT">Entrada Manual</option>
            </select>
          </div>

          <div>
            <label htmlFor="marketplace-filter" className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
              Canal / Marketplace
            </label>
            <select
              id="marketplace-filter"
              value={marketplaceFilter}
              onChange={(e) => setMarketplaceFilter(e.target.value)}
              className="w-full h-11 px-3 text-sm bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500"
            >
              <option value="ALL">Todos os Canais</option>
              <option value="MERCADO_LIVRE">Mercado Livre</option>
              <option value="SHOPEE">Shopee</option>
              <option value="AMAZON">Amazon</option>
            </select>
          </div>

          <div className="lg:col-span-2">
            <label htmlFor="search-input" className="block text-xs font-medium text-slate-700 dark:text-slate-300 mb-1">
              Busca (Título, ID de Anúncio ou GTIN)
            </label>
            <div className="flex gap-2">
              <input
                id="search-input"
                type="text"
                placeholder="Ex: Samsung Galaxy, MLB12345, 7891234..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full h-11 px-3 text-sm bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white placeholder-slate-400 focus:ring-2 focus:ring-blue-500"
              />
              <button
                type="submit"
                className="px-4 h-11 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-semibold transition-colors shrink-0"
              >
                Filtrar
              </button>
              {(sourceTypeFilter !== 'ALL' || marketplaceFilter !== 'ALL' || searchTerm) && (
                <button
                  type="button"
                  onClick={handleResetFilters}
                  className="px-3 h-11 border border-slate-300 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300 rounded-lg text-sm font-semibold transition-colors shrink-0"
                >
                  Limpar
                </button>
              )}
            </div>
          </div>
        </div>
      </form>

      {/* Loading State */}
      {isLoading && (
        <div role="status" aria-busy="true" className="p-8 text-center space-y-4">
          <div className="inline-block w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin"></div>
          <p className="text-sm text-slate-500 dark:text-slate-400">Carregando evidências de mercado...</p>
        </div>
      )}

      {/* Error State */}
      {error && !isLoading && (
        <div role="alert" className="p-6 rounded-xl border border-red-200 dark:border-red-900/50 bg-red-50 dark:bg-red-950/30 text-center space-y-3">
          <div className="text-sm font-bold text-red-800 dark:text-red-300">Falha na Consulta de Observações</div>
          <p className="text-xs text-red-600 dark:text-red-400">{error}</p>
          <button
            onClick={fetchObservations}
            className="px-4 py-2 min-h-[44px] bg-red-600 hover:bg-red-700 text-white text-xs font-semibold rounded-lg transition-colors"
          >
            Tentar Novamente
          </button>
        </div>
      )}

      {/* Empty State */}
      {!isLoading && !error && observations.length === 0 && (
        <div role="region" aria-label="Sem observações" className="p-12 text-center border-2 border-dashed border-slate-200 dark:border-slate-800 rounded-xl space-y-3">
          <svg className="w-12 h-12 mx-auto text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
          </svg>
          <div className="text-base font-semibold text-slate-900 dark:text-white">Nenhuma observação encontrada</div>
          <p className="text-xs text-slate-500 dark:text-slate-400 max-w-md mx-auto">
            Não há observações de mercado cadastradas para os filtros aplicados. Tente ajustar os parâmetros de busca.
          </p>
        </div>
      )}

      {/* Tabela Desktop (Hidden on mobile) */}
      {!isLoading && !error && observations.length > 0 && (
        <div className="hidden md:block rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 overflow-hidden shadow-sm">
          <table className="w-full text-left text-sm border-collapse">
            <thead className="bg-slate-50 dark:bg-slate-800/60 text-slate-500 dark:text-slate-400 text-xs font-semibold uppercase tracking-wider border-b border-slate-200 dark:border-slate-800">
              <tr>
                <th className="py-3.5 px-4">Anúncio / Identificador</th>
                <th className="py-3.5 px-4">Canal</th>
                <th className="py-3.5 px-4">Evidência</th>
                <th className="py-3.5 px-4">Preço</th>
                <th className="py-3.5 px-4">Confiança</th>
                <th className="py-3.5 px-4">Coletado Em</th>
                <th className="py-3.5 px-4 text-right">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {observations.map((obs) => (
                <tr key={obs.id} className="hover:bg-slate-50/70 dark:hover:bg-slate-800/40 transition-colors">
                  <td className="py-3 px-4 max-w-xs">
                    <div className="font-semibold text-slate-900 dark:text-white truncate" title={obs.title}>
                      {obs.title}
                    </div>
                    <div className="text-xs text-slate-500 dark:text-slate-400 flex items-center gap-2 mt-0.5">
                      <span className="font-mono">{obs.observedListingId}</span>
                      {obs.gtin && (
                        <>
                          <span>&bull;</span>
                          <span className="font-mono">GTIN: {obs.gtin}</span>
                        </>
                      )}
                    </div>
                  </td>
                  <td className="py-3 px-4 whitespace-nowrap">
                    <span className="text-xs font-medium text-slate-700 dark:text-slate-300">
                      {obs.marketplace.replace('_', ' ')}
                    </span>
                  </td>
                  <td className="py-3 px-4 whitespace-nowrap">
                    {renderBadge(obs.sourceType)}
                  </td>
                  <td className="py-3 px-4 whitespace-nowrap font-bold text-slate-900 dark:text-white">
                    {formatCurrency(obs.price)}
                  </td>
                  <td className="py-3 px-4 whitespace-nowrap">
                    <span className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                      {(obs.confidence * 100).toFixed(0)}%
                    </span>
                  </td>
                  <td className="py-3 px-4 whitespace-nowrap text-xs text-slate-500 dark:text-slate-400">
                    {formatDate(obs.collectedAt)}
                  </td>
                  <td className="py-3 px-4 whitespace-nowrap text-right">
                    <button
                      onClick={() => setSelectedObservation(obs)}
                      className="px-3 py-1.5 min-h-[44px] text-xs font-semibold text-blue-600 dark:text-blue-400 hover:text-blue-700 hover:bg-blue-50 dark:hover:bg-blue-950/40 rounded-lg transition-colors"
                    >
                      Ver Explicação
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Cards Mobile (Visible only on mobile) */}
      {!isLoading && !error && observations.length > 0 && (
        <div className="md:hidden space-y-3">
          {observations.map((obs) => (
            <div
              key={obs.id}
              className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm space-y-3"
            >
              <div className="flex items-start justify-between gap-2">
                <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                  {obs.marketplace.replace('_', ' ')}
                </span>
                {renderBadge(obs.sourceType)}
              </div>

              <div>
                <h3 className="text-sm font-bold text-slate-900 dark:text-white line-clamp-2">
                  {obs.title}
                </h3>
                <div className="text-xs font-mono text-slate-500 dark:text-slate-400 mt-1">
                  ID: {obs.observedListingId}
                </div>
              </div>

              <div className="flex items-baseline justify-between pt-2 border-t border-slate-100 dark:border-slate-800">
                <div>
                  <span className="text-[10px] text-slate-400 uppercase block">Preço</span>
                  <span className="text-base font-bold text-slate-900 dark:text-white">
                    {formatCurrency(obs.price)}
                  </span>
                </div>
                <div className="text-right">
                  <span className="text-[10px] text-slate-400 uppercase block">Confiança</span>
                  <span className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                    {(obs.confidence * 100).toFixed(0)}%
                  </span>
                </div>
              </div>

              <button
                onClick={() => setSelectedObservation(obs)}
                className="w-full py-2.5 min-h-[44px] bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-800 dark:text-slate-200 rounded-lg text-xs font-semibold transition-colors flex items-center justify-center gap-1.5"
              >
                <span>Ver Detalhes & Explicação</span>
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                </svg>
              </button>
            </div>
          ))}
        </div>
      )}

      {/* Paginação */}
      {!isLoading && !error && totalPages > 1 && (
        <div className="flex items-center justify-between pt-2">
          <span className="text-xs text-slate-500 dark:text-slate-400">
            Página {page + 1} de {totalPages} ({totalElements} registros)
          </span>
          <div className="flex gap-2">
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-3 py-1.5 min-h-[44px] border border-slate-300 dark:border-slate-700 disabled:opacity-40 text-xs font-semibold rounded-lg hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors"
            >
              Anterior
            </button>
            <button
              onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="px-3 py-1.5 min-h-[44px] border border-slate-300 dark:border-slate-700 disabled:opacity-40 text-xs font-semibold rounded-lg hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors"
            >
              Próxima
            </button>
          </div>
        </div>
      )}

      {/* Drawer de Detalhes e Explicação */}
      <ObservationDetailDrawer
        observation={selectedObservation}
        onClose={() => setSelectedObservation(null)}
      />
        </>
      )}
    </div>
  );
}
