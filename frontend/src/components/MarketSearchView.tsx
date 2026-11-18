'use client';

import React, { useState, useEffect, useMemo } from 'react';
import { ProductCompareModal, NormalizedSearchResultItem } from './ProductCompareModal';
import { LoadingSkeleton } from './LoadingSkeleton';
import { EmptyState } from './EmptyState';

export interface MarketSearchApiResponse {
  items: NormalizedSearchResultItem[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  sourceUsed: string;
  sourcePolicyId: string;
  capabilityStatus: 'SUPPORTED' | 'RESTRICTED' | 'UNVERIFIED' | string;
  cached: boolean;
  capabilityNotice?: string;
  rateLimitRemaining: number;
}

export interface MarketSearchViewProps {
  onAddToMonitoring?: (item: NormalizedSearchResultItem) => void;
}

export function MarketSearchView({ onAddToMonitoring }: MarketSearchViewProps = {}) {
  // Search parameters
  const [query, setQuery] = useState('');
  const [marketplace, setMarketplace] = useState('ALL');
  const [category, setCategory] = useState('');
  const [minPrice, setMinPrice] = useState('');
  const [maxPrice, setMaxPrice] = useState('');
  const [onlyOwnCatalog, setOnlyOwnCatalog] = useState(false);
  const [page, setPage] = useState(0);
  const pageSize = 20;

  // State
  const [data, setData] = useState<MarketSearchApiResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [rateLimitWaitSeconds, setRateLimitWaitSeconds] = useState<number | null>(null);

  // Sorting & Filtering in View
  const [sortBy, setSortBy] = useState<'recent' | 'price_asc' | 'price_desc' | 'title_asc'>('recent');
  const [ownershipFilter, setOwnershipFilter] = useState<'ALL' | 'OWN' | 'COMPETITOR'>('ALL');
  const [viewMode, setViewMode] = useState<'table' | 'cards'>('table');

  // Compare mode
  const [selectedItemsForCompare, setSelectedItemsForCompare] = useState<NormalizedSearchResultItem[]>([]);
  const [isCompareModalOpen, setIsCompareModalOpen] = useState(false);

  // Initial load
  useEffect(() => {
    executeSearch(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const executeSearch = async (targetPage = 0) => {
    setIsLoading(true);
    setErrorMessage(null);
    setRateLimitWaitSeconds(null);
    setPage(targetPage);

    const params = new URLSearchParams();
    const searchTerm = query.trim() || 'monitor'; // Default safe initial term
    params.set('q', searchTerm);
    params.set('page', targetPage.toString());
    params.set('size', pageSize.toString());

    if (marketplace && marketplace !== 'ALL') {
      params.set('marketplace', marketplace);
    }
    if (category.trim()) {
      params.set('category', category.trim());
    }
    if (minPrice.trim()) {
      params.set('minPrice', minPrice.trim());
    }
    if (maxPrice.trim()) {
      params.set('maxPrice', maxPrice.trim());
    }
    if (onlyOwnCatalog) {
      params.set('onlyOwnCatalog', 'true');
    }

    try {
      const res = await fetch(`/api/market-search?${params.toString()}`);

      if (res.status === 429) {
        const errorData = await res.json().catch(() => ({}));
        const retryAfter = errorData.retryAfter || 60;
        setRateLimitWaitSeconds(retryAfter);
        setErrorMessage(
          errorData.message ||
            `Limite de taxa de pesquisa excedido pela API oficial. Aguarde ${retryAfter} segundos para tentar novamente.`
        );
        setData(null);
        return;
      }

      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        setErrorMessage(err.message || 'Erro ao realizar pesquisa de mercado.');
        setData(null);
        return;
      }

      const result: MarketSearchApiResponse = await res.json();
      setData(result);
    } catch {
      setErrorMessage('Falha de conexão com a API de busca de mercado.');
      setData(null);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    executeSearch(0);
  };

  const isOwnItem = (item: NormalizedSearchResultItem) => {
    const classification = item.sellerClassification?.toLowerCase() || '';
    const seller = item.sellerName?.toLowerCase() || '';
    return (
      item.provenanceType === 'OFFICIAL' &&
      (classification.includes('próprio') || seller.includes('próprio'))
    );
  };

  // Process and sort items
  const processedItems = useMemo(() => {
    if (!data?.items) return [];

    let filtered = [...data.items];

    // Filter by ownership
    if (ownershipFilter === 'OWN') {
      filtered = filtered.filter((i) => isOwnItem(i));
    } else if (ownershipFilter === 'COMPETITOR') {
      filtered = filtered.filter((i) => !isOwnItem(i));
    }

    // Sort
    filtered.sort((a, b) => {
      const priceA = typeof a.price === 'string' ? parseFloat(a.price) : a.price || 0;
      const priceB = typeof b.price === 'string' ? parseFloat(b.price) : b.price || 0;

      if (sortBy === 'price_asc') return priceA - priceB;
      if (sortBy === 'price_desc') return priceB - priceA;
      if (sortBy === 'title_asc') return a.title.localeCompare(b.title);
      // 'recent'
      return new Date(b.collectedAt).getTime() - new Date(a.collectedAt).getTime();
    });

    return filtered;
  }, [data?.items, ownershipFilter, sortBy]);

  // Comparison selection
  const toggleSelectForCompare = (item: NormalizedSearchResultItem) => {
    setSelectedItemsForCompare((prev) => {
      const exists = prev.some((p) => p.itemId === item.itemId);
      if (exists) {
        return prev.filter((p) => p.itemId !== item.itemId);
      }
      if (prev.length >= 4) {
        alert('Você pode comparar até 4 anúncios simultaneamente.');
        return prev;
      }
      return [...prev, item];
    });
  };

  const removeCompareItem = (itemId: string) => {
    setSelectedItemsForCompare((prev) => prev.filter((p) => p.itemId !== itemId));
  };

  const formatPrice = (val: number | string, currency = 'BRL') => {
    const num = typeof val === 'string' ? parseFloat(val) : val;
    if (isNaN(num)) return 'R$ 0,00';
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: currency || 'BRL',
    }).format(num);
  };

  const formatDate = (isoStr: string) => {
    if (!isoStr) return '---';
    try {
      return new Intl.DateTimeFormat('pt-BR', {
        dateStyle: 'short',
        timeStyle: 'short',
      }).format(new Date(isoStr));
    } catch {
      return isoStr;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-xl md:text-2xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
          <span>🔍 Pesquisa de Produtos e Anúncios de Mercado</span>
        </h1>
        <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
          Busca de mercado autorizada via APIs oficiais, rastreabilidade completa de proveniência,
          política Fail-Closed e proteção anti-scraping.
        </p>
      </div>

      {/* Formulário de Busca */}
      <form
        onSubmit={handleSearchSubmit}
        className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-4 md:p-5 shadow-sm space-y-4"
      >
        <div className="grid grid-cols-1 md:grid-cols-12 gap-4">
          {/* Termo Principal */}
          <div className="md:col-span-6 space-y-1">
            <label
              htmlFor="market-search-input"
              className="block text-xs font-semibold text-slate-700 dark:text-slate-300"
            >
              Termo de Busca / SKU / GTIN:
            </label>
            <input
              id="market-search-input"
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Ex: monitor 27 polegadas, iPhone, SKU-123..."
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
            />
          </div>

          {/* Marketplace */}
          <div className="md:col-span-3 space-y-1">
            <label
              htmlFor="market-select"
              className="block text-xs font-semibold text-slate-700 dark:text-slate-300"
            >
              Canal de Mercado:
            </label>
            <select
              id="market-select"
              value={marketplace}
              onChange={(e) => setMarketplace(e.target.value)}
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
            >
              <option value="ALL">Todos os Canais</option>
              <option value="MERCADO_LIVRE">Mercado Livre (Público)</option>
              <option value="SHOPEE">Shopee (Restrito / Loja)</option>
              <option value="AMAZON">Amazon SP-API (Homologação)</option>
              <option value="INTERNAL">Catálogo Interno (Próprio)</option>
            </select>
          </div>

          {/* Categoria */}
          <div className="md:col-span-3 space-y-1">
            <label
              htmlFor="category-input"
              className="block text-xs font-semibold text-slate-700 dark:text-slate-300"
            >
              Categoria (Opcional):
            </label>
            <input
              id="category-input"
              type="text"
              value={category}
              onChange={(e) => setCategory(e.target.value)}
              placeholder="Ex: Eletrônicos, Informática"
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
            />
          </div>
        </div>

        {/* Linha secundária de filtros */}
        <div className="flex flex-wrap items-center justify-between gap-4 pt-2 border-t border-slate-100 dark:border-slate-800">
          <div className="flex flex-wrap items-center gap-3">
            {/* Faixa de Preço */}
            <div className="flex items-center gap-2">
              <span className="text-xs text-slate-500 dark:text-slate-400">Preço (R$):</span>
              <input
                type="number"
                step="0.01"
                placeholder="Mínimo"
                value={minPrice}
                onChange={(e) => setMinPrice(e.target.value)}
                className="w-24 px-2 py-1.5 text-xs rounded border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white min-h-[38px]"
                aria-label="Preço mínimo"
              />
              <span className="text-xs text-slate-400">até</span>
              <input
                type="number"
                step="0.01"
                placeholder="Máximo"
                value={maxPrice}
                onChange={(e) => setMaxPrice(e.target.value)}
                className="w-24 px-2 py-1.5 text-xs rounded border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white min-h-[38px]"
                aria-label="Preço máximo"
              />
            </div>

            {/* Toggle Catálogo Próprio */}
            <label
              htmlFor="only-own-catalog-toggle"
              className="inline-flex items-center gap-2 cursor-pointer text-xs font-medium text-slate-700 dark:text-slate-300 min-h-[44px]"
            >
              <input
                id="only-own-catalog-toggle"
                type="checkbox"
                checked={onlyOwnCatalog}
                onChange={(e) => setOnlyOwnCatalog(e.target.checked)}
                className="h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-blue-500"
              />
              <span>Apenas Meu Catálogo Próprio</span>
            </label>
          </div>

          <button
            type="submit"
            disabled={isLoading}
            aria-label="Executar pesquisa de mercado"
            className="w-full sm:w-auto px-6 py-2.5 min-h-[44px] rounded-lg bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-semibold text-sm transition-colors shadow-sm flex items-center justify-center gap-2"
          >
            {isLoading ? (
              <span>Pesquisando...</span>
            ) : (
              <>
                <span>🔎</span>
                <span>Pesquisar</span>
              </>
            )}
          </button>
        </div>
      </form>

      {/* Alerta de Rate Limit (HTTP 429) */}
      {rateLimitWaitSeconds !== null && (
        <div
          role="alert"
          className="p-4 rounded-xl border border-red-300 dark:border-red-900/60 bg-red-50 dark:bg-red-950/40 text-red-900 dark:text-red-200 text-sm space-y-1"
        >
          <div className="flex items-center gap-2 font-bold text-red-800 dark:text-red-300">
            <span>⚠️ Limite de Taxa Excedido (HTTP 429)</span>
          </div>
          <p className="text-xs">
            As cotas oficiais da API foram consumidas para proteger sua conta contra bloqueios.
            Aguarde <strong>{rateLimitWaitSeconds} segundos</strong> antes de disparar novas consultas.
          </p>
        </div>
      )}

      {/* Mensagem de Erro Geral */}
      {errorMessage && rateLimitWaitSeconds === null && (
        <div
          role="alert"
          className="p-4 rounded-xl border border-amber-300 dark:border-amber-900/60 bg-amber-50 dark:bg-amber-950/40 text-amber-900 dark:text-amber-200 text-sm"
        >
          <span className="font-semibold">Aviso: </span>
          {errorMessage}
        </div>
      )}

      {/* Banner de Metadados da Política e Capacidade */}
      {data && (
        <div className="bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700/60 rounded-xl p-4 space-y-3">
          <div className="flex flex-wrap items-center justify-between gap-2 text-xs">
            <div className="flex flex-wrap items-center gap-2">
              <span className="font-semibold text-slate-700 dark:text-slate-300">Fonte Utilizada:</span>
              <span className="px-2 py-0.5 rounded font-mono font-medium bg-slate-200 dark:bg-slate-700 text-slate-800 dark:text-slate-200">
                {data.sourceUsed}
              </span>
              <span className="text-slate-400">&middot;</span>
              <span className="font-mono text-[11px] text-slate-500 dark:text-slate-400">
                {data.sourcePolicyId}
              </span>

              {/* Status da Capacidade */}
              <span
                className={`px-2 py-0.5 rounded-full font-bold text-[11px] ${
                  data.capabilityStatus === 'SUPPORTED'
                    ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/60 dark:text-emerald-300'
                    : data.capabilityStatus === 'RESTRICTED'
                    ? 'bg-amber-100 text-amber-800 dark:bg-amber-900/60 dark:text-amber-300'
                    : 'bg-red-100 text-red-800 dark:bg-red-900/60 dark:text-red-300'
                }`}
              >
                {data.capabilityStatus}
              </span>

              {/* Badge de Cache */}
              {data.cached && (
                <span className="px-2 py-0.5 rounded-full text-[11px] font-semibold bg-cyan-100 text-cyan-800 dark:bg-cyan-900/60 dark:text-cyan-300 flex items-center gap-1">
                  <span>⚡</span>
                  <span>Cache (TTL 15m)</span>
                </span>
              )}
            </div>

            {/* Quota restante */}
            <div className="text-slate-500 dark:text-slate-400">
              Cota restante da fonte:{' '}
              <span className="font-semibold text-slate-800 dark:text-slate-200">
                {data.rateLimitRemaining} req
              </span>
            </div>
          </div>

          {/* Aviso Legal / Capability Notice */}
          {data.capabilityNotice && (
            <div
              className={`text-xs p-3 rounded-lg border ${
                data.capabilityStatus === 'RESTRICTED'
                  ? 'bg-amber-50 dark:bg-amber-950/30 border-amber-200 dark:border-amber-800 text-amber-900 dark:text-amber-200'
                  : data.capabilityStatus === 'UNVERIFIED'
                  ? 'bg-red-50 dark:bg-red-950/30 border-red-200 dark:border-red-800 text-red-900 dark:text-red-200'
                  : 'bg-blue-50/50 dark:bg-blue-950/20 border-blue-200 dark:border-blue-900 text-blue-900 dark:text-blue-200'
              }`}
            >
              <div className="flex items-start gap-2">
                <span className="text-base leading-none">
                  {data.capabilityStatus === 'RESTRICTED'
                    ? '🛡️'
                    : data.capabilityStatus === 'UNVERIFIED'
                    ? '🛑'
                    : 'ℹ️'}
                </span>
                <div>
                  <p className="font-semibold">
                    {data.capabilityStatus === 'RESTRICTED'
                      ? 'Política Anti-Scraping & Proteção de Conta:'
                      : data.capabilityStatus === 'UNVERIFIED'
                      ? 'Política Fail-Closed & Homologação Pendente:'
                      : 'Informação de Conformidade:'}
                  </p>
                  <p className="mt-0.5 text-[11px] leading-relaxed">{data.capabilityNotice}</p>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Barra de Controles, Ordenação e Comparação */}
      {data && (
        <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
          {/* Contador e Filtro de Titularidade */}
          <div className="flex flex-wrap items-center gap-2 text-xs">
            <span className="font-semibold text-slate-700 dark:text-slate-300">
              {processedItems.length}{' '}
              {processedItems.length === 1 ? 'item exibido' : 'itens exibidos'}
              {data.totalElements > processedItems.length && ` (de ${data.totalElements})`}
            </span>

            <span className="text-slate-300 dark:text-slate-700">|</span>

            {/* Filtros de Titularidade */}
            <div className="inline-flex rounded-lg border border-slate-200 dark:border-slate-700 p-0.5 bg-slate-100 dark:bg-slate-800">
              <button
                type="button"
                onClick={() => setOwnershipFilter('ALL')}
                className={`px-2.5 py-1 text-xs rounded-md font-medium transition-colors min-h-[36px] ${
                  ownershipFilter === 'ALL'
                    ? 'bg-white dark:bg-slate-700 text-slate-900 dark:text-white shadow-sm'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                }`}
              >
                Todos
              </button>
              <button
                type="button"
                onClick={() => setOwnershipFilter('OWN')}
                className={`px-2.5 py-1 text-xs rounded-md font-medium transition-colors min-h-[36px] ${
                  ownershipFilter === 'OWN'
                    ? 'bg-emerald-600 text-white shadow-sm'
                    : 'text-slate-600 dark:text-slate-400 hover:text-emerald-600'
                }`}
              >
                🟢 Anúncios Próprios
              </button>
              <button
                type="button"
                onClick={() => setOwnershipFilter('COMPETITOR')}
                className={`px-2.5 py-1 text-xs rounded-md font-medium transition-colors min-h-[36px] ${
                  ownershipFilter === 'COMPETITOR'
                    ? 'bg-amber-600 text-white shadow-sm'
                    : 'text-slate-600 dark:text-slate-400 hover:text-amber-600'
                }`}
              >
                🟠 Concorrentes
              </button>
            </div>
          </div>

          {/* Ordenação, Comparação e Toggle de Visualização */}
          <div className="flex flex-wrap items-center gap-2">
            {/* Botão de Comparar */}
            <button
              type="button"
              disabled={selectedItemsForCompare.length < 2}
              onClick={() => setIsCompareModalOpen(true)}
              className="px-3 py-1.5 min-h-[44px] rounded-lg text-xs font-semibold bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-300 dark:disabled:bg-slate-800 disabled:text-slate-500 text-white transition-colors shadow-sm flex items-center gap-1.5"
            >
              <span>⚖️ Comparar Selecionados</span>
              <span className="px-1.5 py-0.2 rounded-full bg-indigo-800 text-[10px]">
                {selectedItemsForCompare.length}
              </span>
            </button>

            {/* Ordenação */}
            <div className="flex items-center gap-1 text-xs text-slate-500 dark:text-slate-400">
              <label htmlFor="sort-by-select" className="sr-only">
                Ordenar por
              </label>
              <select
                id="sort-by-select"
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value as any)}
                className="px-2 py-1.5 text-xs rounded border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white min-h-[44px]"
              >
                <option value="recent">Mais Recentes</option>
                <option value="price_asc">Menor Preço</option>
                <option value="price_desc">Maior Preço</option>
                <option value="title_asc">Título (A-Z)</option>
              </select>
            </div>

            {/* Toggle de Layout Desktop (Tabela / Cards) */}
            <div className="hidden sm:inline-flex rounded-lg border border-slate-200 dark:border-slate-700 p-0.5 bg-slate-100 dark:bg-slate-800">
              <button
                type="button"
                onClick={() => setViewMode('table')}
                aria-label="Visualização em tabela"
                className={`p-1.5 rounded-md min-h-[36px] min-w-[36px] ${
                  viewMode === 'table'
                    ? 'bg-white dark:bg-slate-700 text-slate-900 dark:text-white shadow-sm'
                    : 'text-slate-500'
                }`}
              >
                📋
              </button>
              <button
                type="button"
                onClick={() => setViewMode('cards')}
                aria-label="Visualização em cards"
                className={`p-1.5 rounded-md min-h-[36px] min-w-[36px] ${
                  viewMode === 'cards'
                    ? 'bg-white dark:bg-slate-700 text-slate-900 dark:text-white shadow-sm'
                    : 'text-slate-500'
                }`}
              >
                🗂️
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Conteúdo Principal */}
      {isLoading ? (
        <LoadingSkeleton rows={6} />
      ) : !data || processedItems.length === 0 ? (
        <EmptyState
          title="Nenhum anúncio ou produto encontrado"
          description="Tente ajustar os termos de busca, selecionar 'Todos os Canais' ou relaxar os filtros de preço."
          actionLabel="Limpar Filtros e Buscar Novamente"
          onAction={() => {
            setQuery('');
            setMarketplace('ALL');
            setMinPrice('');
            setMaxPrice('');
            setOnlyOwnCatalog(false);
            setOwnershipFilter('ALL');
            executeSearch(0);
          }}
        />
      ) : (
        <>
          {/* Visualização Condicional: Tabela com Scroll Acessível OU Grade de Cards */}
          {viewMode === 'table' ? (
            <div
              role="region"
              aria-label="Tabela de resultados de pesquisa com rolagem horizontal acessível"
              tabIndex={0}
              className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs" role="table">
                  <thead className="bg-slate-50 dark:bg-slate-800/80 text-slate-500 dark:text-slate-400 font-semibold border-b border-slate-200 dark:border-slate-800">
                    <tr>
                      <th className="py-3 px-3 w-10 text-center">⚖️</th>
                      <th className="py-3 px-3">Titularidade</th>
                      <th className="py-3 px-3">Produto / Anúncio</th>
                      <th className="py-3 px-3">Canal</th>
                      <th className="py-3 px-3">Preço</th>
                      <th className="py-3 px-3">Vendedor</th>
                      <th className="py-3 px-3">Proveniência</th>
                      <th className="py-3 px-3">Data / Coleta</th>
                      <th className="py-3 px-3 text-right">Ação</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 dark:divide-slate-800 text-slate-700 dark:text-slate-300">
                    {processedItems.map((item) => {
                      const own = isOwnItem(item);
                      const isSelected = selectedItemsForCompare.some((p) => p.itemId === item.itemId);

                      return (
                        <tr
                          key={item.itemId}
                          className={`hover:bg-slate-50/80 dark:hover:bg-slate-800/40 transition-colors ${
                            isSelected
                              ? 'bg-indigo-50/40 dark:bg-indigo-950/20'
                              : own
                              ? 'bg-emerald-50/20 dark:bg-emerald-950/10'
                              : ''
                          }`}
                        >
                          {/* Checkbox Comparação */}
                          <td className="py-3 px-3 text-center">
                            <input
                              type="checkbox"
                              checked={isSelected}
                              onChange={() => toggleSelectForCompare(item)}
                              className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500 cursor-pointer"
                              aria-label={`Selecionar ${item.title} para comparação`}
                            />
                          </td>

                          {/* Titularidade */}
                          <td className="py-3 px-3">
                            <span
                              className={`inline-flex items-center px-2 py-0.5 rounded text-[11px] font-bold ${
                                own
                                  ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/60 dark:text-emerald-300'
                                  : 'bg-amber-100 text-amber-800 dark:bg-amber-900/60 dark:text-amber-300'
                              }`}
                            >
                              {own ? '🟢 Próprio' : '🟠 Concorrente'}
                            </span>
                          </td>

                          {/* Produto e Link */}
                          <td className="py-3 px-3 max-w-xs">
                            <div className="font-semibold text-slate-900 dark:text-white line-clamp-2">
                              {item.title}
                            </div>
                            <div className="font-mono text-[10px] text-slate-400 mt-0.5">
                              ID: {item.itemId} {item.gtin ? `| GTIN: ${item.gtin}` : ''}
                            </div>
                            {item.salesMetricNotice && (
                              <div className="text-[10px] text-slate-400 italic mt-0.5">
                                {item.salesMetricNotice}
                              </div>
                            )}
                          </td>

                          {/* Canal */}
                          <td className="py-3 px-3">
                            <span className="font-medium text-slate-800 dark:text-slate-200">
                              {item.marketplace}
                            </span>
                          </td>

                          {/* Preço */}
                          <td className="py-3 px-3">
                            <span className="font-bold text-sm text-slate-900 dark:text-white">
                              {formatPrice(item.price, item.currency)}
                            </span>
                          </td>

                          {/* Vendedor */}
                          <td className="py-3 px-3">
                            <div className="font-medium text-slate-800 dark:text-slate-200 truncate max-w-[120px]">
                              {item.sellerName || 'Não identificado'}
                            </div>
                            <div className="text-[10px] text-slate-400">
                              {item.sellerClassification || 'Geral'}
                            </div>
                          </td>

                          {/* Proveniência */}
                          <td className="py-3 px-3">
                            <span
                              className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                                item.provenanceType === 'OFFICIAL'
                                  ? 'bg-blue-100 text-blue-800 dark:bg-blue-900/60 dark:text-blue-300'
                                  : item.provenanceType === 'LICENSED'
                                  ? 'bg-purple-100 text-purple-800 dark:bg-purple-900/60 dark:text-purple-300'
                                  : 'bg-slate-200 text-slate-800 dark:bg-slate-700 dark:text-slate-300'
                              }`}
                            >
                              {item.provenanceType}
                            </span>
                            <div className="font-mono text-[9px] text-slate-400 mt-0.5">
                              {item.sourcePolicyId}
                            </div>
                          </td>

                          {/* Data */}
                          <td className="py-3 px-3 text-slate-500 dark:text-slate-400 whitespace-nowrap">
                            {formatDate(item.collectedAt)}
                          </td>

                          {/* Ações: Monitorar & Link Oficial Sanitizado */}
                          <td className="py-3 px-3 text-right">
                            <div className="flex items-center justify-end gap-1">
                              {onAddToMonitoring && (
                                <button
                                  type="button"
                                  onClick={() => onAddToMonitoring(item)}
                                  className="inline-flex items-center px-2 py-1 min-h-[44px] text-xs font-semibold text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-950/40 rounded-lg transition-colors whitespace-nowrap"
                                  title="Adicionar anúncio ao monitoramento"
                                >
                                  🎯 Monitorar
                                </button>
                              )}
                              {item.listingUrl ? (
                                <a
                                  href={item.listingUrl}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  className="inline-flex items-center px-3 py-1.5 min-h-[44px] text-xs font-medium text-blue-600 dark:text-blue-400 hover:text-blue-800 dark:hover:text-blue-200 hover:underline"
                                >
                                  Canal ↗
                                </a>
                              ) : (
                                <span className="text-slate-400 text-xs">---</span>
                              )}
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          ) : (
            /* Cards View */
            <div
              role="region"
              aria-label="Grade de cartões de produtos"
              className="grid grid-cols-1 md:grid-cols-2 gap-4"
            >
              {processedItems.map((item) => {
                const own = isOwnItem(item);
                const isSelected = selectedItemsForCompare.some((p) => p.itemId === item.itemId);

                return (
                  <div
                    key={item.itemId}
                    className={`p-4 rounded-xl border transition-all ${
                      isSelected
                        ? 'border-indigo-400 bg-indigo-50/30 dark:bg-indigo-950/20'
                        : own
                        ? 'border-emerald-300 dark:border-emerald-800/60 bg-emerald-50/20 dark:bg-emerald-950/10'
                        : 'border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm'
                    }`}
                  >
                    {/* Topo do Card */}
                    <div className="flex items-center justify-between gap-2 mb-2">
                      <div className="flex items-center gap-2">
                        <input
                          type="checkbox"
                          checked={isSelected}
                          onChange={() => toggleSelectForCompare(item)}
                          className="h-4 w-4 rounded border-slate-300 text-indigo-600 focus:ring-indigo-500 cursor-pointer"
                          aria-label={`Comparar ${item.title}`}
                        />
                        <span
                          className={`text-[11px] px-2 py-0.5 rounded font-bold ${
                            own
                              ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/60 dark:text-emerald-300'
                              : 'bg-amber-100 text-amber-800 dark:bg-amber-900/60 dark:text-amber-300'
                          }`}
                        >
                          {own ? '🟢 Anúncio Próprio' : '🟠 Concorrente'}
                        </span>
                      </div>

                      <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-blue-100 text-blue-800 dark:bg-blue-900/60 dark:text-blue-300">
                        {item.provenanceType}
                      </span>
                    </div>

                    {/* Título & Preço */}
                    <h3 className="font-semibold text-sm text-slate-900 dark:text-white line-clamp-2 mb-2">
                      {item.title}
                    </h3>

                    <div className="flex items-baseline justify-between mb-3">
                      <span className="text-xl font-bold text-slate-900 dark:text-white">
                        {formatPrice(item.price, item.currency)}
                      </span>
                      <span className="text-xs text-slate-500 dark:text-slate-400 font-medium">
                        {item.marketplace}
                      </span>
                    </div>

                    {/* Metadados do Card */}
                    <div className="text-xs space-y-1 text-slate-500 dark:text-slate-400 border-t border-slate-100 dark:border-slate-800 pt-2 mb-3">
                      <div className="flex justify-between">
                        <span>Vendedor:</span>
                        <span className="font-medium text-slate-800 dark:text-slate-200 truncate max-w-[150px]">
                          {item.sellerName || 'Não identificado'}
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span>Coleta:</span>
                        <span>{formatDate(item.collectedAt)}</span>
                      </div>
                      <div className="flex justify-between font-mono text-[10px]">
                        <span>Política:</span>
                        <span>{item.sourcePolicyId}</span>
                      </div>
                    </div>

                    {/* Ações Mobile */}
                    <div className="flex flex-col gap-2 pt-1">
                      {onAddToMonitoring && (
                        <button
                          type="button"
                          onClick={() => onAddToMonitoring(item)}
                          className="inline-flex items-center justify-center w-full px-4 py-2 min-h-[44px] rounded-lg text-xs font-semibold text-white bg-blue-600 hover:bg-blue-700 transition-colors"
                        >
                          🎯 Monitorar Anúncio
                        </button>
                      )}
                      {item.listingUrl && (
                        <a
                          href={item.listingUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="inline-flex items-center justify-center w-full px-4 py-2 min-h-[44px] rounded-lg text-xs font-semibold text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/40 hover:bg-blue-100 dark:hover:bg-blue-900/60 transition-colors"
                        >
                          Acessar no Canal Oficial ↗
                        </a>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {/* Paginação Acessível */}
          {data.totalPages > 1 && (
            <div className="flex items-center justify-between pt-4 border-t border-slate-200 dark:border-slate-800 text-xs">
              <span className="text-slate-500 dark:text-slate-400">
                Página <strong>{page + 1}</strong> de <strong>{data.totalPages}</strong>
              </span>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  disabled={page <= 0 || isLoading}
                  onClick={() => executeSearch(page - 1)}
                  aria-label="Página anterior"
                  className="px-4 py-2 min-h-[44px] rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-300 disabled:opacity-50 hover:bg-slate-50 dark:hover:bg-slate-700 font-medium transition-colors"
                >
                  &larr; Anterior
                </button>
                <button
                  type="button"
                  disabled={page >= data.totalPages - 1 || isLoading}
                  onClick={() => executeSearch(page + 1)}
                  aria-label="Próxima página"
                  className="px-4 py-2 min-h-[44px] rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-300 disabled:opacity-50 hover:bg-slate-50 dark:hover:bg-slate-700 font-medium transition-colors"
                >
                  Próxima &rarr;
                </button>
              </div>
            </div>
          )}
        </>
      )}

      {/* Modal de Comparação Lado a Lado */}
      <ProductCompareModal
        isOpen={isCompareModalOpen}
        onClose={() => setIsCompareModalOpen(false)}
        products={selectedItemsForCompare}
        onRemoveProduct={removeCompareItem}
      />
    </div>
  );
}
