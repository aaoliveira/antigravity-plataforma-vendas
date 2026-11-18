'use client';

import React, { useState, useEffect, useCallback, useId, useMemo } from 'react';
import { useAuth } from '@/context/AuthContext';
import { EmptyState } from './EmptyState';
import { OrderDetail, OrderDetailDrawer } from './OrderDetailDrawer';
import { SellerAccountOption } from './CatalogAndListingsView';

export interface OrderSyncCheckpointInfo {
  id: string;
  sellerAccountId: string;
  marketplace: string;
  syncStatus: string;
  cursorToken?: string | null;
  lastOffset: number;
  pageSize: number;
  totalOrdersSynced: number;
  startedAt: string;
  lastCheckpointAt: string;
  completedAt?: string | null;
  errorMessage?: string | null;
}

export const OrdersView: React.FC = () => {
  const { currentTenant } = useAuth();

  const [orders, setOrders] = useState<OrderDetail[]>([]);
  const [sellerAccounts, setSellerAccounts] = useState<SellerAccountOption[]>([]);
  const [checkpoints, setCheckpoints] = useState<OrderSyncCheckpointInfo[]>([]);
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
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [appliedStartDate, setAppliedStartDate] = useState<string>('');
  const [appliedEndDate, setAppliedEndDate] = useState<string>('');

  // Detalhes do Pedido Selecionado
  const [selectedOrder, setSelectedOrder] = useState<OrderDetail | null>(null);

  const searchInputId = useId();
  const marketplaceSelectId = useId();
  const accountSelectId = useId();
  const statusSelectId = useId();
  const startDateInputId = useId();
  const endDateInputId = useId();
  const pageSizeSelectId = useId();

  // Carrega contas de vendedores conectadas
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

  // Carrega checkpoints de sincronização
  const fetchCheckpoints = useCallback(async () => {
    try {
      const res = await fetch('/api/orders/checkpoints');
      if (res.ok) {
        const data = await res.json();
        setCheckpoints(data);
      }
    } catch {
      // Falha não bloqueante
    }
  }, []);

  // Busca de pedidos com filtros e paginação server-side
  const fetchOrders = useCallback(async () => {
    setLoading(true);
    setError(null);

    const params = new URLSearchParams();
    params.set('page', page.toString());
    params.set('size', pageSize.toString());

    if (marketplace !== 'ALL') params.set('marketplace', marketplace);
    if (sellerAccountId !== 'ALL') params.set('sellerAccountId', sellerAccountId);
    if (status !== 'ALL') params.set('status', status);
    if (appliedStartDate.trim()) params.set('startDate', new Date(appliedStartDate).toISOString());
    if (appliedEndDate.trim()) {
      const end = new Date(appliedEndDate);
      end.setHours(23, 59, 59, 999);
      params.set('endDate', end.toISOString());
    }
    if (appliedSearch.trim()) params.set('search', appliedSearch.trim());

    try {
      const res = await fetch(`/api/orders?${params.toString()}`);

      if (!res.ok) {
        if (res.status === 401) {
          setError('Sessão expirada ou não autenticada. Por favor, realize login novamente.');
        } else if (res.status === 403) {
          setError('Acesso negado: você não possui permissão para visualizar pedidos desta organização.');
        } else {
          setError('Não foi possível carregar os pedidos de venda. Verifique sua conexão e tente novamente.');
        }
        setOrders([]);
        setTotalElements(0);
        setTotalPages(0);
        return;
      }

      const data = await res.json();
      setOrders(data.content || []);
      setTotalElements(data.totalElements || 0);
      setTotalPages(data.totalPages || 0);
    } catch {
      setError('Erro de comunicação com o servidor ao consultar pedidos de venda.');
      setOrders([]);
      setTotalElements(0);
      setTotalPages(0);
    } finally {
      setLoading(false);
    }
  }, [page, pageSize, marketplace, sellerAccountId, status, appliedStartDate, appliedEndDate, appliedSearch]);

  useEffect(() => {
    fetchAccounts();
    fetchCheckpoints();
  }, [fetchAccounts, fetchCheckpoints]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  const handleFilterSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    setAppliedSearch(search);
    setAppliedStartDate(startDate);
    setAppliedEndDate(endDate);
  };

  const handleClearFilters = () => {
    setSearch('');
    setAppliedSearch('');
    setMarketplace('ALL');
    setSellerAccountId('ALL');
    setStatus('ALL');
    setStartDate('');
    setEndDate('');
    setAppliedStartDate('');
    setAppliedEndDate('');
    setPage(0);
  };

  const formatCurrency = (val: number, currency = 'BRL') => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: currency || 'BRL',
    }).format(val || 0);
  };

  const formatDate = (isoString?: string | null) => {
    if (!isoString) return null;
    try {
      return new Intl.DateTimeFormat('pt-BR', {
        dateStyle: 'short',
        timeStyle: 'short',
      }).format(new Date(isoString));
    } catch {
      return isoString;
    }
  };

  // Cálculos de métricas da página atual
  const metrics = useMemo(() => {
    const totalAmountSum = orders.reduce((acc, o) => acc + (Number(o.totalAmount) || 0), 0);
    const shippingSum = orders.reduce((acc, o) => acc + (Number(o.shippingCost) || 0), 0);
    const incompleteCount = orders.filter((o) => {
      const missingTracking = (o.status === 'SHIPPED' || o.status === 'DELIVERED') && !o.trackingNumber;
      const missingBuyer = !o.buyerName;
      const itemsMissingSku = (o.items || []).some((i) => !i.sku);
      return missingTracking || missingBuyer || itemsMissingSku;
    }).length;

    return {
      totalAmountSum,
      shippingSum,
      incompleteCount,
    };
  }, [orders]);

  // Informação agregada do último checkpoint
  const latestCheckpoint = useMemo(() => {
    if (!checkpoints || checkpoints.length === 0) return null;
    return checkpoints.reduce((prev, curr) => {
      return new Date(curr.lastCheckpointAt) > new Date(prev.lastCheckpointAt) ? curr : prev;
    }, checkpoints[0]);
  }, [checkpoints]);

  const getStatusBadge = (orderStatus: string) => {
    switch (orderStatus) {
      case 'PAID':
        return <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300">Pago</span>;
      case 'SHIPPED':
        return <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300">Enviado</span>;
      case 'DELIVERED':
        return <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-teal-100 text-teal-800 dark:bg-teal-950 dark:text-teal-300">Entregue</span>;
      case 'CANCELLED':
        return <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-300">Cancelado</span>;
      case 'RETURNED':
        return <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-purple-100 text-purple-800 dark:bg-purple-950 dark:text-purple-300">Devolvido</span>;
      default:
        return <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300">{orderStatus}</span>;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header com Contexto */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-slate-200 dark:border-slate-800">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <span className="px-2 py-0.5 rounded text-xs font-semibold bg-indigo-100 text-indigo-800 dark:bg-indigo-950 dark:text-indigo-300">
              Módulo Oficial &middot; P008
            </span>
            <span className="px-2 py-0.5 rounded text-xs font-semibold bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300">
              Tenant: {currentTenant?.name || 'Organização Ativa'}
            </span>
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
            Pedidos de Venda & Reconciliação
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Ingestão oficial de pedidos próprios, status de conciliação, data de sincronização e sinalização de incompletos.
          </p>
        </div>

        {/* Resumo do Checkpoint de Sincronização */}
        {latestCheckpoint && (
          <div className="flex items-center gap-3 bg-slate-50 dark:bg-slate-800/60 px-4 py-2.5 rounded-xl border border-slate-200 dark:border-slate-800 text-xs text-slate-600 dark:text-slate-300">
            <span className="relative flex h-2.5 w-2.5">
              <span className={`animate-ping absolute inline-flex h-full w-full rounded-full opacity-75 ${latestCheckpoint.syncStatus === 'COMPLETED' ? 'bg-emerald-400' : 'bg-amber-400'}`}></span>
              <span className={`relative inline-flex rounded-full h-2.5 w-2.5 ${latestCheckpoint.syncStatus === 'COMPLETED' ? 'bg-emerald-500' : 'bg-amber-500'}`}></span>
            </span>
            <div>
              <p className="font-semibold text-slate-900 dark:text-white">
                Sincronização: {latestCheckpoint.syncStatus === 'COMPLETED' ? 'Concluída' : 'Em andamento'}
              </p>
              <p className="text-[11px] text-slate-500 dark:text-slate-400">
                Última checagem: {formatDate(latestCheckpoint.lastCheckpointAt)} &middot; {latestCheckpoint.totalOrdersSynced} pedidos sincronizados
              </p>
            </div>
          </div>
        )}
      </div>

      {/* KPI Cards de Resumo */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="p-4 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs">
          <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider block">
            Total de Pedidos
          </span>
          <p className="text-2xl font-extrabold text-slate-900 dark:text-white mt-1">
            {totalElements}
          </p>
          <span className="text-xs text-slate-500 dark:text-slate-400">
            {orders.length} exibidos nesta página
          </span>
        </div>

        <div className="p-4 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs">
          <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider block">
            Faturamento na Página
          </span>
          <p className="text-2xl font-extrabold text-indigo-600 dark:text-indigo-400 mt-1">
            {formatCurrency(metrics.totalAmountSum)}
          </p>
          <span className="text-xs text-slate-500 dark:text-slate-400">
            Valores oficiais do canal
          </span>
        </div>

        <div className="p-4 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs">
          <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider block">
            Frete Consolidado
          </span>
          <p className="text-2xl font-extrabold text-slate-900 dark:text-white mt-1">
            {formatCurrency(metrics.shippingSum)}
          </p>
          <span className="text-xs text-slate-500 dark:text-slate-400">
            Custo total de envio
          </span>
        </div>

        <div className="p-4 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs">
          <span className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider block">
            Pedidos Incompletos
          </span>
          <p className={`text-2xl font-extrabold mt-1 ${metrics.incompleteCount > 0 ? 'text-amber-600 dark:text-amber-400' : 'text-emerald-600 dark:text-emerald-400'}`}>
            {metrics.incompleteCount}
          </p>
          <span className="text-xs text-slate-500 dark:text-slate-400">
            {metrics.incompleteCount > 0 ? 'Sem rastreio, SKU ou comprador' : 'Todos os dados íntegros'}
          </span>
        </div>
      </div>

      {/* Formulário de Busca e Filtros Combináveis */}
      <form
        onSubmit={handleFilterSubmit}
        className="bg-white dark:bg-slate-900 p-4 rounded-xl border border-slate-200 dark:border-slate-800 shadow-xs space-y-4"
        role="search"
      >
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-3">
          {/* Busca Textual */}
          <div className="lg:col-span-2">
            <label htmlFor={searchInputId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Buscar Pedido
            </label>
            <div className="relative">
              <input
                id={searchInputId}
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="ID do pedido, comprador ou rastreio..."
                className="w-full pl-9 pr-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-hidden focus:ring-2 focus:ring-indigo-500 min-h-[44px]"
              />
              <svg className="w-4 h-4 text-slate-400 absolute left-3 top-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>
          </div>

          {/* Filtro por Marketplace */}
          <div>
            <label htmlFor={marketplaceSelectId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Canal
            </label>
            <select
              id={marketplaceSelectId}
              value={marketplace}
              onChange={(e) => {
                setMarketplace(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-hidden focus:ring-2 focus:ring-indigo-500 min-h-[44px]"
            >
              <option value="ALL">Todos os Canais</option>
              <option value="MERCADO_LIVRE">Mercado Livre</option>
              <option value="SHOPEE">Shopee</option>
            </select>
          </div>

          {/* Filtro por Conta */}
          <div>
            <label htmlFor={accountSelectId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Conta
            </label>
            <select
              id={accountSelectId}
              value={sellerAccountId}
              onChange={(e) => {
                setSellerAccountId(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-hidden focus:ring-2 focus:ring-indigo-500 min-h-[44px]"
            >
              <option value="ALL">Todas as Contas</option>
              {sellerAccounts.map((acc) => (
                <option key={acc.id} value={acc.id}>
                  {acc.accountName} ({acc.marketplace === 'MERCADO_LIVRE' ? 'ML' : 'Shopee'})
                </option>
              ))}
            </select>
          </div>

          {/* Filtro por Status */}
          <div>
            <label htmlFor={statusSelectId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Status
            </label>
            <select
              id={statusSelectId}
              value={status}
              onChange={(e) => {
                setStatus(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-hidden focus:ring-2 focus:ring-indigo-500 min-h-[44px]"
            >
              <option value="ALL">Todos os Status</option>
              <option value="PAID">Pago</option>
              <option value="SHIPPED">Enviado</option>
              <option value="DELIVERED">Entregue</option>
              <option value="CANCELLED">Cancelado</option>
              <option value="RETURNED">Devolvido</option>
              <option value="PENDING">Pendente</option>
            </select>
          </div>

          {/* Intervalo de Datas */}
          <div>
            <label htmlFor={startDateInputId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Data Início
            </label>
            <input
              id={startDateInputId}
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              className="w-full px-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-hidden focus:ring-2 focus:ring-indigo-500 min-h-[44px]"
            />
          </div>
        </div>

        {/* Linha inferior de ações e Data Fim */}
        <div className="flex flex-col sm:flex-row items-end sm:items-center justify-between gap-3 pt-2 border-t border-slate-100 dark:border-slate-800">
          <div className="flex items-center gap-2">
            <div>
              <label htmlFor={endDateInputId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Data Fim
              </label>
              <input
                id={endDateInputId}
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
                className="px-3 py-2 text-sm rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:outline-hidden focus:ring-2 focus:ring-indigo-500 min-h-[44px]"
              />
            </div>
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto">
            <button
              type="submit"
              className="flex-1 sm:flex-none px-4 py-2 min-h-[44px] bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-semibold rounded-lg shadow-xs transition-colors focus:outline-hidden focus:ring-2 focus:ring-indigo-500"
            >
              Filtrar
            </button>
            <button
              type="button"
              onClick={handleClearFilters}
              className="px-4 py-2 min-h-[44px] bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 text-sm font-medium rounded-lg transition-colors focus:outline-hidden focus:ring-2 focus:ring-indigo-500"
            >
              Limpar
            </button>
          </div>
        </div>
      </form>

      {/* Estados de Interface: Loading, Error e Tabela */}
      {loading ? (
        <div
          role="status"
          aria-live="polite"
          className="bg-white dark:bg-slate-900 p-8 rounded-xl border border-slate-200 dark:border-slate-800 space-y-4"
        >
          <div className="flex items-center gap-3">
            <div className="w-5 h-5 border-2 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
            <span className="text-sm font-medium text-slate-600 dark:text-slate-400">
              Carregando pedidos de venda...
            </span>
          </div>
          <div className="space-y-3 pt-2">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="h-12 bg-slate-100 dark:bg-slate-800 rounded-lg animate-pulse" />
            ))}
          </div>
        </div>
      ) : error ? (
        <div
          role="alert"
          className="p-6 rounded-xl bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800 text-red-900 dark:text-red-200 flex items-start gap-4"
        >
          <svg className="w-6 h-6 text-red-600 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <div>
            <h3 className="font-semibold text-base">Falha ao consultar pedidos</h3>
            <p className="text-sm mt-1">{error}</p>
            <button
              onClick={() => fetchOrders()}
              className="mt-3 px-3 py-1.5 min-h-[44px] text-xs font-semibold bg-red-600 hover:bg-red-700 text-white rounded-lg transition-colors"
            >
              Tentar Novamente
            </button>
          </div>
        </div>
      ) : orders.length === 0 ? (
        <EmptyState
          title="Nenhum pedido encontrado"
          description={
            appliedSearch || marketplace !== 'ALL' || status !== 'ALL' || appliedStartDate
              ? 'Nenhum pedido corresponde aos filtros aplicados. Tente ajustar os termos de busca ou redefinir os filtros.'
              : 'Nenhum pedido próprio sincronizado para a organização ativa até o momento.'
          }
          actionLabel={appliedSearch || marketplace !== 'ALL' || status !== 'ALL' ? 'Limpar Filtros' : undefined}
          onAction={appliedSearch || marketplace !== 'ALL' || status !== 'ALL' ? handleClearFilters : undefined}
        />
      ) : (
        <div className="space-y-4">
          {/* Tabela Desktop */}
          <div className="hidden md:block bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 overflow-hidden shadow-xs">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-sm">
                <thead>
                  <tr className="border-b border-slate-200 dark:border-slate-800 bg-slate-50/75 dark:bg-slate-800/50 text-slate-600 dark:text-slate-400 font-semibold text-xs uppercase tracking-wider">
                    <th scope="col" className="py-3.5 px-4">Pedido / Canal</th>
                    <th scope="col" className="py-3.5 px-4">Data Venda</th>
                    <th scope="col" className="py-3.5 px-4">Comprador</th>
                    <th scope="col" className="py-3.5 px-4">Status</th>
                    <th scope="col" className="py-3.5 px-4">Rastreio</th>
                    <th scope="col" className="py-3.5 px-4 text-right">Total</th>
                    <th scope="col" className="py-3.5 px-4">Sincronizado</th>
                    <th scope="col" className="py-3.5 px-4 text-center">Ações</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60">
                  {orders.map((order) => {
                    const isMeli = order.marketplace === 'MERCADO_LIVRE';
                    const isShopee = order.marketplace === 'SHOPEE';
                    const isMissingTracking = (order.status === 'SHIPPED' || order.status === 'DELIVERED') && !order.trackingNumber;
                    const isMissingBuyer = !order.buyerName;
                    const isIncomplete = isMissingTracking || isMissingBuyer;

                    return (
                      <tr
                        key={order.id}
                        className="hover:bg-slate-50/80 dark:hover:bg-slate-800/40 transition-colors"
                      >
                        <td className="py-3.5 px-4">
                          <div className="flex items-center gap-2">
                            <span
                              className={`inline-flex items-center px-1.5 py-0.5 rounded text-[11px] font-bold ${
                                isMeli
                                  ? 'bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300'
                                  : isShopee
                                  ? 'bg-orange-100 text-orange-800 dark:bg-orange-950 dark:text-orange-300'
                                  : 'bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-300'
                              }`}
                            >
                              {isMeli ? 'ML' : isShopee ? 'Shopee' : order.marketplace}
                            </span>
                            <span className="font-medium font-mono text-slate-900 dark:text-white">
                              {order.externalOrderId}
                            </span>
                            {isIncomplete && (
                              <span
                                title="Pedido com dados incompletos (rastreamento ou comprador)"
                                className="inline-flex items-center px-1.5 py-0.2 rounded text-[10px] font-bold bg-amber-100 text-amber-900 dark:bg-amber-950 dark:text-amber-200 border border-amber-300 dark:border-amber-700"
                              >
                                Incompleto
                              </span>
                            )}
                          </div>
                        </td>

                        <td className="py-3.5 px-4 text-slate-600 dark:text-slate-300 text-xs">
                          {formatDate(order.orderCreatedAt)}
                        </td>

                        <td className="py-3.5 px-4 text-slate-700 dark:text-slate-300">
                          {order.buyerName ? (
                            <span className="truncate max-w-[140px] block">{order.buyerName}</span>
                          ) : (
                            <span className="text-xs px-1.5 py-0.5 rounded bg-slate-100 dark:bg-slate-800 text-slate-500">
                              Não informado
                            </span>
                          )}
                        </td>

                        <td className="py-3.5 px-4">
                          {getStatusBadge(order.status)}
                        </td>

                        <td className="py-3.5 px-4">
                          {order.trackingNumber ? (
                            <span className="font-mono text-xs text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-950/40 px-1.5 py-0.5 rounded">
                              {order.trackingNumber}
                            </span>
                          ) : (
                            <span className="text-xs text-slate-400 dark:text-slate-500 italic">
                              Sem rastreio
                            </span>
                          )}
                        </td>

                        <td className="py-3.5 px-4 text-right font-semibold text-slate-900 dark:text-white">
                          {formatCurrency(order.totalAmount, order.currency)}
                        </td>

                        <td className="py-3.5 px-4 text-slate-500 dark:text-slate-400 text-xs">
                          {formatDate(order.syncedAt)}
                        </td>

                        <td className="py-3.5 px-4 text-center">
                          <button
                            onClick={() => setSelectedOrder(order)}
                            className="inline-flex items-center justify-center px-3 py-1.5 min-h-[44px] rounded-lg text-xs font-semibold bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-300 hover:bg-indigo-100 dark:hover:bg-indigo-900 transition-colors focus:outline-hidden focus:ring-2 focus:ring-indigo-500"
                          >
                            Ver Detalhes
                          </button>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* Cards Mobile */}
          <div className="md:hidden space-y-3">
            {orders.map((order) => {
              const isMeli = order.marketplace === 'MERCADO_LIVRE';
              const isShopee = order.marketplace === 'SHOPEE';
              const isMissingTracking = (order.status === 'SHIPPED' || order.status === 'DELIVERED') && !order.trackingNumber;
              const isMissingBuyer = !order.buyerName;
              const isIncomplete = isMissingTracking || isMissingBuyer;

              return (
                <div
                  key={order.id}
                  className="bg-white dark:bg-slate-900 p-4 rounded-xl border border-slate-200 dark:border-slate-800 shadow-xs space-y-3"
                >
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-1.5">
                      <span
                        className={`inline-flex items-center px-1.5 py-0.5 rounded text-[11px] font-bold ${
                          isMeli
                            ? 'bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300'
                            : isShopee
                            ? 'bg-orange-100 text-orange-800 dark:bg-orange-950 dark:text-orange-300'
                            : 'bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-300'
                        }`}
                      >
                        {isMeli ? 'ML' : isShopee ? 'Shopee' : order.marketplace}
                      </span>
                      <span className="font-bold text-sm font-mono text-slate-900 dark:text-white">
                        {order.externalOrderId}
                      </span>
                    </div>
                    {getStatusBadge(order.status)}
                  </div>

                  {isIncomplete && (
                    <div className="text-[11px] font-semibold text-amber-800 dark:text-amber-300 bg-amber-50 dark:bg-amber-950/40 p-1.5 rounded border border-amber-200 dark:border-amber-800">
                      ⚠ Pedido com dados incompletos
                    </div>
                  )}

                  <div className="grid grid-cols-2 gap-2 text-xs text-slate-600 dark:text-slate-400">
                    <div>
                      <span className="block text-slate-400 dark:text-slate-500">Comprador:</span>
                      <span className="font-medium text-slate-800 dark:text-slate-200 truncate block">
                        {order.buyerName || 'Não informado'}
                      </span>
                    </div>
                    <div>
                      <span className="block text-slate-400 dark:text-slate-500">Rastreio:</span>
                      <span className="font-mono text-slate-800 dark:text-slate-200 truncate block">
                        {order.trackingNumber || 'Sem rastreio'}
                      </span>
                    </div>
                    <div>
                      <span className="block text-slate-400 dark:text-slate-500">Data do Pedido:</span>
                      <span>{formatDate(order.orderCreatedAt)}</span>
                    </div>
                    <div>
                      <span className="block text-slate-400 dark:text-slate-500">Total:</span>
                      <span className="font-bold text-slate-900 dark:text-white text-sm">
                        {formatCurrency(order.totalAmount, order.currency)}
                      </span>
                    </div>
                  </div>

                  <div className="pt-2 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
                    <span className="text-[11px] text-slate-400">
                      Sync: {formatDate(order.syncedAt)}
                    </span>
                    <button
                      onClick={() => setSelectedOrder(order)}
                      className="px-3 py-1.5 min-h-[44px] rounded-lg text-xs font-semibold bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-300 hover:bg-indigo-100 dark:hover:bg-indigo-900 transition-colors"
                    >
                      Ver Detalhes
                    </button>
                  </div>
                </div>
              );
            })}
          </div>

          {/* Paginação Server-Side */}
          <div className="flex flex-col sm:flex-row items-center justify-between gap-4 p-4 bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800">
            <div className="flex items-center gap-2 text-sm text-slate-600 dark:text-slate-400">
              <span>Exibindo</span>
              <select
                id={pageSizeSelectId}
                value={pageSize}
                onChange={(e) => {
                  setPageSize(Number(e.target.value));
                  setPage(0);
                }}
                className="px-2 py-1 rounded border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white text-xs min-h-[36px]"
              >
                <option value={10}>10</option>
                <option value={25}>25</option>
                <option value={50}>50</option>
              </select>
              <span>pedidos por página &middot; Total: <strong className="text-slate-900 dark:text-white">{totalElements}</strong></span>
            </div>

            <div className="flex items-center gap-2">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-3 py-1.5 min-h-[44px] text-xs font-medium rounded-lg border border-slate-300 dark:border-slate-700 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors"
              >
                Anterior
              </button>
              <span className="text-xs text-slate-600 dark:text-slate-400 font-medium">
                Página {page + 1} de {Math.max(1, totalPages)}
              </span>
              <button
                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="px-3 py-1.5 min-h-[44px] text-xs font-medium rounded-lg border border-slate-300 dark:border-slate-700 disabled:opacity-50 disabled:cursor-not-allowed hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors"
              >
                Próxima
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Drawer de Detalhes do Pedido */}
      {selectedOrder && (
        <OrderDetailDrawer
          order={selectedOrder}
          onClose={() => setSelectedOrder(null)}
        />
      )}
    </div>
  );
};
