'use client';

import React, { useState, useEffect, useCallback, useId } from 'react';
import { useAuth } from '@/context/AuthContext';
import { EmptyState } from './EmptyState';
import { ListingDetail, ListingDetailDrawer } from './ListingDetailDrawer';

export interface SellerAccountOption {
  id: string;
  marketplace: string;
  externalAccountId: string;
  accountName: string;
  status: string;
}

export const CatalogAndListingsView: React.FC = () => {
  const { currentTenant } = useAuth();

  const [listings, setListings] = useState<ListingDetail[]>([]);
  const [sellerAccounts, setSellerAccounts] = useState<SellerAccountOption[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Paginação Server-side
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);
  const [totalElements, setTotalElements] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // Filtros Combináveis
  const [search, setSearch] = useState('');
  const [appliedSearch, setAppliedSearch] = useState('');
  const [marketplace, setMarketplace] = useState<string>('ALL');
  const [sellerAccountId, setSellerAccountId] = useState<string>('ALL');
  const [status, setStatus] = useState<string>('ALL');

  // Detalhes do Anúncio Selecionado
  const [selectedListing, setSelectedListing] = useState<ListingDetail | null>(null);

  const searchInputId = useId();
  const marketplaceSelectId = useId();
  const accountSelectId = useId();
  const statusSelectId = useId();
  const pageSizeSelectId = useId();

  // Carrega lista de contas para o filtro
  const fetchAccounts = useCallback(async () => {
    try {
      const res = await fetch('/api/catalog/seller-accounts');
      if (res.ok) {
        const data = await res.json();
        setSellerAccounts(data);
      }
    } catch {
      // Falha não bloqueante do filtro de contas
    }
  }, []);

  // Busca de anúncios com filtros e paginação server-side
  const fetchListings = useCallback(async () => {
    setLoading(true);
    setError(null);

    const params = new URLSearchParams();
    params.set('page', page.toString());
    params.set('size', pageSize.toString());
    if (marketplace !== 'ALL') params.set('marketplace', marketplace);
    if (sellerAccountId !== 'ALL') params.set('sellerAccountId', sellerAccountId);
    if (status !== 'ALL') params.set('status', status);
    if (appliedSearch.trim()) params.set('search', appliedSearch.trim());

    try {
      const res = await fetch(`/api/catalog/listings?${params.toString()}`);

      if (!res.ok) {
        if (res.status === 401) {
          setError('Sessão expirada ou não autenticada. Por favor, autentique-se novamente.');
        } else if (res.status === 403) {
          setError('Acesso negado: você não possui permissão para consultar os anúncios desta organização.');
        } else {
          setError('Não foi possível carregar os anúncios do catálogo. Verifique sua conexão e tente novamente.');
        }
        setListings([]);
        return;
      }

      const data = await res.json();
      setListings(data.content || []);
      setTotalElements(data.totalElements || 0);
      setTotalPages(data.totalPages || 0);
    } catch {
      setError('Instabilidade de comunicação com o servidor de catálogo. Tente novamente mais tarde.');
      setListings([]);
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, marketplace, sellerAccountId, status, appliedSearch]);

  // Recarrega ao trocar de tenant ou filtros
  useEffect(() => {
    fetchAccounts();
  }, [fetchAccounts, currentTenant]);

  useEffect(() => {
    fetchListings();
  }, [fetchListings, currentTenant]);

  // Reseta para a primeira página ao alterar filtros
  const handleMarketplaceChange = (val: string) => {
    setMarketplace(val);
    setPage(0);
  };

  const handleAccountChange = (val: string) => {
    setSellerAccountId(val);
    setPage(0);
  };

  const handleStatusChange = (val: string) => {
    setStatus(val);
    setPage(0);
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setAppliedSearch(search);
    setPage(0);
  };

  const handleClearFilters = () => {
    setSearch('');
    setAppliedSearch('');
    setMarketplace('ALL');
    setSellerAccountId('ALL');
    setStatus('ALL');
    setPage(0);
  };

  const isFilterActive =
    search.trim() !== '' ||
    appliedSearch.trim() !== '' ||
    marketplace !== 'ALL' ||
    sellerAccountId !== 'ALL' ||
    status !== 'ALL';

  const formatPrice = (val: number, currency = 'BRL') => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency,
    }).format(val);
  };

  return (
    <div className="space-y-6">
      {/* =========================================================================
          1. CABEÇALHO DO MÓDULO DE CATÁLOGO
      ========================================================================= */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-slate-200 dark:border-slate-800">
        <div>
          <div className="flex items-center gap-2.5 flex-wrap">
            <h1 className="text-xl md:text-2xl font-extrabold text-slate-900 dark:text-white tracking-tight">
              Catálogo de Produtos & Anúncios
            </h1>
            <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-100 text-blue-800 dark:bg-blue-950/60 dark:text-blue-300">
              {totalElements} {totalElements === 1 ? 'anúncio' : 'anúncios'}
            </span>
          </div>
          <p className="text-xs md:text-sm text-slate-600 dark:text-slate-400 mt-1">
            Gestão unificada de inventário canônico e anúncios próprios integrados via Mercado Livre e Shopee.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => fetchListings()}
            disabled={loading}
            aria-label="Atualizar listagem"
            className="px-3.5 py-2 rounded-lg border border-slate-300 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300 text-xs font-semibold transition-colors flex items-center gap-1.5 min-h-[44px]"
          >
            <svg
              className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`}
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            <span>Atualizar</span>
          </button>
        </div>
      </div>

      {/* =========================================================================
          2. TOOLBAR DE BUSCA E FILTROS COMBINÁVEIS
      ========================================================================= */}
      <section
        aria-label="Filtros do Catálogo"
        className="p-4 rounded-xl bg-slate-50 dark:bg-slate-900/60 border border-slate-200 dark:border-slate-800 space-y-3"
      >
        <form onSubmit={handleSearchSubmit} className="flex flex-col lg:flex-row gap-3">
          {/* Campo de Busca Textual */}
          <div className="flex-1 relative">
            <label htmlFor={searchInputId} className="sr-only">
              Buscar anúncios
            </label>
            <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
            <input
              id={searchInputId}
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Buscar por título, SKU ou ID do anúncio..."
              className="w-full pl-10 pr-9 py-2.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white placeholder-slate-400 text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none min-h-[44px]"
            />
            {search && (
              <button
                type="button"
                onClick={() => {
                  setSearch('');
                  setPage(0);
                }}
                aria-label="Limpar busca"
                className="absolute inset-y-0 right-0 pr-3 flex items-center text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
              >
                ✕
              </button>
            )}
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            {/* Filtro: Marketplace */}
            <div>
              <label htmlFor={marketplaceSelectId} className="sr-only">
                Filtrar por canal
              </label>
              <select
                id={marketplaceSelectId}
                value={marketplace}
                onChange={(e) => handleMarketplaceChange(e.target.value)}
                aria-label="Filtrar por Marketplace"
                className="w-full px-3 py-2.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none min-h-[44px]"
              >
                <option value="ALL">Todos os Canais</option>
                <option value="MERCADO_LIVRE">Mercado Livre</option>
                <option value="SHOPEE">Shopee</option>
              </select>
            </div>

            {/* Filtro: Conta Conectada */}
            <div>
              <label htmlFor={accountSelectId} className="sr-only">
                Filtrar por conta
              </label>
              <select
                id={accountSelectId}
                value={sellerAccountId}
                onChange={(e) => handleAccountChange(e.target.value)}
                aria-label="Filtrar por Conta"
                className="w-full px-3 py-2.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none min-h-[44px]"
              >
                <option value="ALL">Todas as Contas</option>
                {sellerAccounts.map((acc) => (
                  <option key={acc.id} value={acc.id}>
                    {acc.accountName} ({acc.marketplace})
                  </option>
                ))}
              </select>
            </div>

            {/* Filtro: Status */}
            <div>
              <label htmlFor={statusSelectId} className="sr-only">
                Filtrar por status
              </label>
              <select
                id={statusSelectId}
                value={status}
                onChange={(e) => handleStatusChange(e.target.value)}
                aria-label="Filtrar por Status"
                className="w-full px-3 py-2.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-900 dark:text-white text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none min-h-[44px]"
              >
                <option value="ALL">Todos os Status</option>
                <option value="ACTIVE">Ativo (ACTIVE)</option>
                <option value="PAUSED">Pausado (PAUSED)</option>
                <option value="CLOSED">Encerrado (CLOSED)</option>
              </select>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="submit"
              className="w-full sm:w-auto px-4 py-2.5 rounded-lg bg-blue-600 hover:bg-blue-700 text-white font-semibold text-xs transition-colors flex items-center justify-center min-h-[44px]"
            >
              Filtrar
            </button>

            {isFilterActive && (
              <button
                type="button"
                onClick={handleClearFilters}
                className="w-full sm:w-auto px-3.5 py-2.5 rounded-lg border border-rose-300 dark:border-rose-900/60 bg-rose-50 dark:bg-rose-950/30 text-rose-700 dark:text-rose-300 hover:bg-rose-100 font-semibold text-xs transition-colors min-h-[44px]"
              >
                Limpar
              </button>
            )}
          </div>
        </form>
      </section>

      {/* =========================================================================
          3. TRATAMENTO DE ERROS SANITIZADOS
      ========================================================================= */}
      {error && (
        <div
          role="alert"
          aria-live="assertive"
          className="p-4 rounded-xl bg-rose-50 dark:bg-rose-950/50 border border-rose-200 dark:border-rose-900 text-rose-800 dark:text-rose-200 flex items-start justify-between gap-3"
        >
          <div className="flex items-center gap-2">
            <svg className="w-5 h-5 text-rose-600 dark:text-rose-400 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            <span className="text-xs md:text-sm font-medium">{error}</span>
          </div>

          <button
            type="button"
            onClick={() => fetchListings()}
            className="px-3 py-1.5 rounded-lg bg-rose-100 dark:bg-rose-900/60 hover:bg-rose-200 text-rose-900 dark:text-rose-200 font-semibold text-xs transition-colors shrink-0 min-h-[44px]"
          >
            Tentar novamente
          </button>
        </div>
      )}

      {/* =========================================================================
          4. ESTADO DE CARREGAMENTO (LOADING SKELETON)
      ========================================================================= */}
      {loading && (
        <div
          role="status"
          aria-label="Carregando catálogo..."
          className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 space-y-3"
        >
          <div className="h-6 bg-slate-200 dark:bg-slate-800 rounded animate-pulse w-1/4" />
          <div className="space-y-2">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="h-12 bg-slate-100 dark:bg-slate-800/60 rounded animate-pulse" />
            ))}
          </div>
          <span className="sr-only">Carregando lista de anúncios...</span>
        </div>
      )}

      {/* =========================================================================
          5. ESTADO VAZIO (EMPTY STATE)
      ========================================================================= */}
      {!loading && !error && listings.length === 0 && (
        <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900">
          <EmptyState
            title="Nenhum anúncio encontrado"
            description={
              isFilterActive
                ? "Nenhum anúncio corresponde aos filtros e termos de busca aplicados. Tente ajustar os parâmetros ou limpar os filtros."
                : "Seu catálogo ainda não possui anúncios sincronizados nesta organização. Conecte sua loja oficial no Mercado Livre ou Shopee para iniciar a sincronização."
            }
            actionLabel={isFilterActive ? "Limpar Filtros" : "Conectar Loja Oficial"}
            onAction={isFilterActive ? handleClearFilters : () => window.location.assign('/')}
          />
        </div>
      )}

      {/* =========================================================================
          6. TABELA RESPONSIVA DE ANÚNCIOS (DESKTOP & TABLET)
      ========================================================================= */}
      {!loading && !error && listings.length > 0 && (
        <div className="space-y-4">
          {/* Tabela para Desktop (oculta em telas < 1024px) */}
          <div className="hidden lg:block overflow-hidden rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs border-collapse">
                <thead>
                  <tr className="bg-slate-50 dark:bg-slate-800/80 border-b border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-400 font-bold uppercase tracking-wider">
                    <th scope="col" className="p-3.5">Anúncio</th>
                    <th scope="col" className="p-3.5">Canal / Loja</th>
                    <th scope="col" className="p-3.5">Produto Canônico</th>
                    <th scope="col" className="p-3.5 text-right">Preço</th>
                    <th scope="col" className="p-3.5 text-center">Estoque</th>
                    <th scope="col" className="p-3.5 text-center">Status</th>
                    <th scope="col" className="p-3.5 text-center">Ações</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200 dark:divide-slate-800">
                  {listings.map((item) => {
                    const isMeli = item.marketplace === 'MERCADO_LIVRE';
                    const isShopee = item.marketplace === 'SHOPEE';

                    return (
                      <tr
                        key={item.id}
                        className="hover:bg-slate-50/80 dark:hover:bg-slate-800/40 transition-colors"
                      >
                        {/* Coluna 1: Anúncio */}
                        <td className="p-3.5 max-w-xs">
                          <div className="font-semibold text-slate-900 dark:text-white line-clamp-2">
                            {item.title}
                          </div>
                          <div className="flex items-center gap-1.5 mt-1">
                            <span className="font-mono text-[10px] text-slate-500 dark:text-slate-400">
                              ID: {item.externalListingId}
                            </span>
                            {item.externalSku ? (
                              <span className="font-mono text-[10px] px-1.5 py-0.2 rounded bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300">
                                SKU: {item.externalSku}
                              </span>
                            ) : (
                              <span className="text-[10px] px-1.5 py-0.2 rounded bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-400">
                                Sem SKU
                              </span>
                            )}
                          </div>
                        </td>

                        {/* Coluna 2: Canal / Loja */}
                        <td className="p-3.5 whitespace-nowrap">
                          <span
                            className={`inline-block px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider mb-0.5 ${
                              isMeli
                                ? 'bg-amber-100 text-amber-900 dark:bg-amber-950/60 dark:text-amber-300'
                                : isShopee
                                ? 'bg-orange-100 text-orange-900 dark:bg-orange-950/60 dark:text-orange-300'
                                : 'bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-200'
                            }`}
                          >
                            {isMeli ? 'Mercado Livre' : isShopee ? 'Shopee' : item.marketplace}
                          </span>
                          <div className="text-[11px] text-slate-600 dark:text-slate-400 font-medium">
                            {item.sellerAccountName}
                          </div>
                        </td>

                        {/* Coluna 3: Produto Canônico Mestre */}
                        <td className="p-3.5 max-w-xs">
                          {item.product ? (
                            <div>
                              <div className="font-medium text-slate-800 dark:text-slate-200 line-clamp-1">
                                {item.product.title}
                              </div>
                              <div className="text-[10px] font-mono text-indigo-600 dark:text-indigo-400">
                                {item.product.canonicalSku || 'Sem SKU Mestre'}
                              </div>
                            </div>
                          ) : (
                            <span className="inline-block px-2 py-0.5 rounded text-[10px] font-medium bg-slate-100 text-slate-500 dark:bg-slate-800 dark:text-slate-400">
                              Não vinculado
                            </span>
                          )}
                        </td>

                        {/* Coluna 4: Preço */}
                        <td className="p-3.5 text-right whitespace-nowrap">
                          <span className="font-bold text-slate-900 dark:text-white">
                            {formatPrice(item.price, item.currency)}
                          </span>
                        </td>

                        {/* Coluna 5: Estoque */}
                        <td className="p-3.5 text-center whitespace-nowrap">
                          {item.availableQuantity > 0 ? (
                            <span className="font-medium text-slate-800 dark:text-slate-200">
                              {item.availableQuantity} un
                            </span>
                          ) : (
                            <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-rose-100 text-rose-800 dark:bg-rose-950/60 dark:text-rose-300">
                              Esgotado
                            </span>
                          )}
                        </td>

                        {/* Coluna 6: Status */}
                        <td className="p-3.5 text-center whitespace-nowrap">
                          <span
                            className={`inline-block px-2 py-0.5 rounded text-[10px] font-semibold ${
                              item.status === 'ACTIVE'
                                ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300'
                                : item.status === 'PAUSED'
                                ? 'bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300'
                                : 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300'
                            }`}
                          >
                            {item.status === 'ACTIVE'
                              ? 'Ativo'
                              : item.status === 'PAUSED'
                              ? 'Pausado'
                              : item.status}
                          </span>
                        </td>

                        {/* Coluna 7: Ações */}
                        <td className="p-3.5 text-center whitespace-nowrap">
                          <button
                            type="button"
                            onClick={() => setSelectedListing(item)}
                            aria-label={`Ver detalhes do anúncio ${item.title}`}
                            className="px-3 py-1.5 rounded-lg bg-blue-50 dark:bg-blue-950/40 text-blue-700 dark:text-blue-300 hover:bg-blue-100 dark:hover:bg-blue-900/60 font-semibold text-xs transition-colors min-h-[44px] min-w-[44px] inline-flex items-center justify-center"
                          >
                            Detalhes
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* Cards Responsivos para Mobile & Tablet (ocultos em telas >= 1024px) */}
          <div className="lg:hidden space-y-3">
            {listings.map((item) => {
              const isMeli = item.marketplace === 'MERCADO_LIVRE';
              const isShopee = item.marketplace === 'SHOPEE';

              return (
                <div
                  key={item.id}
                  className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm space-y-3"
                >
                  <div className="flex items-start justify-between gap-2">
                    <span
                      className={`px-2 py-0.5 rounded text-[10px] font-bold uppercase tracking-wider ${
                        isMeli
                          ? 'bg-amber-100 text-amber-900 dark:bg-amber-950/60 dark:text-amber-300'
                          : isShopee
                          ? 'bg-orange-100 text-orange-900 dark:bg-orange-950/60 dark:text-orange-300'
                          : 'bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-200'
                      }`}
                    >
                      {isMeli ? 'Mercado Livre' : isShopee ? 'Shopee' : item.marketplace}
                    </span>

                    <span
                      className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                        item.status === 'ACTIVE'
                          ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300'
                          : item.status === 'PAUSED'
                          ? 'bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300'
                          : 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300'
                      }`}
                    >
                      {item.status === 'ACTIVE' ? 'Ativo' : item.status === 'PAUSED' ? 'Pausado' : item.status}
                    </span>
                  </div>

                  <div>
                    <h3 className="font-bold text-sm text-slate-900 dark:text-white line-clamp-2">
                      {item.title}
                    </h3>
                    <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-0.5">
                      ID: <span className="font-mono">{item.externalListingId}</span> &middot; Conta: {item.sellerAccountName}
                    </p>
                  </div>

                  <div className="flex items-center justify-between pt-2 border-t border-slate-100 dark:border-slate-800">
                    <div>
                      <span className="text-[10px] text-slate-500 dark:text-slate-400 block">Preço</span>
                      <span className="text-sm font-extrabold text-blue-600 dark:text-blue-400">
                        {formatPrice(item.price, item.currency)}
                      </span>
                    </div>

                    <div>
                      <span className="text-[10px] text-slate-500 dark:text-slate-400 block">Estoque</span>
                      <span className="text-xs font-semibold text-slate-800 dark:text-slate-200">
                        {item.availableQuantity} un
                      </span>
                    </div>

                    <button
                      type="button"
                      onClick={() => setSelectedListing(item)}
                      aria-label={`Ver detalhes de ${item.title}`}
                      className="px-3.5 py-2 rounded-lg bg-blue-50 dark:bg-blue-950/40 text-blue-700 dark:text-blue-300 font-semibold text-xs transition-colors min-h-[44px] flex items-center justify-center"
                    >
                      Ver Detalhes
                    </button>
                  </div>
                </div>
              );
            })}
          </div>

          {/* =========================================================================
              7. BARRA INFERIOR DE PAGINAÇÃO SERVER-SIDE
          ========================================================================= */}
          <nav
            aria-label="Paginação do Catálogo"
            className="flex flex-col sm:flex-row items-center justify-between gap-3 p-3.5 rounded-xl bg-slate-50 dark:bg-slate-900/60 border border-slate-200 dark:border-slate-800 text-xs text-slate-600 dark:text-slate-400"
          >
            <div className="flex items-center gap-2">
              <span>
                Exibindo{' '}
                <strong className="text-slate-900 dark:text-white">
                  {totalElements === 0 ? 0 : page * pageSize + 1}
                </strong>{' '}
                a{' '}
                <strong className="text-slate-900 dark:text-white">
                  {Math.min((page + 1) * pageSize, totalElements)}
                </strong>{' '}
                de <strong className="text-slate-900 dark:text-white">{totalElements}</strong> anúncios
              </span>

              <span className="text-slate-300 dark:text-slate-700">|</span>

              <div className="flex items-center gap-1.5">
                <label htmlFor={pageSizeSelectId} className="text-[11px]">
                  Por página:
                </label>
                <select
                  id={pageSizeSelectId}
                  value={pageSize}
                  onChange={(e) => {
                    setPageSize(Number(e.target.value));
                    setPage(0);
                  }}
                  aria-label="Itens por página"
                  className="px-2 py-1 rounded border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-800 dark:text-slate-200 text-xs focus:ring-1 focus:ring-blue-500 focus:outline-none"
                >
                  <option value={10}>10</option>
                  <option value={25}>25</option>
                  <option value={50}>50</option>
                </select>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <span className="text-[11px] font-medium">
                Página {totalPages === 0 ? 0 : page + 1} de {totalPages || 1}
              </span>

              <div className="flex items-center gap-1">
                <button
                  type="button"
                  onClick={() => setPage((prev) => Math.max(0, prev - 1))}
                  disabled={page <= 0 || loading}
                  aria-label="Página anterior"
                  className="px-3 py-1.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-700 dark:text-slate-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors font-medium min-h-[44px] min-w-[44px] flex items-center justify-center"
                >
                  Anterior
                </button>

                <button
                  type="button"
                  onClick={() => setPage((prev) => prev + 1)}
                  disabled={page >= totalPages - 1 || loading}
                  aria-label="Próxima página"
                  className="px-3 py-1.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-900 text-slate-700 dark:text-slate-300 disabled:opacity-40 disabled:cursor-not-allowed hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors font-medium min-h-[44px] min-w-[44px] flex items-center justify-center"
                >
                  Próxima
                </button>
              </div>
            </div>
          </nav>
        </div>
      )}

      {/* =========================================================================
          8. DRAWER DE DETALHES DO ANÚNCIO
      ========================================================================= */}
      <ListingDetailDrawer
        listing={selectedListing}
        onClose={() => setSelectedListing(null)}
      />
    </div>
  );
};
