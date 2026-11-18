'use client';

import React, { useEffect } from 'react';

export interface OrderItemDetail {
  id: string;
  externalItemId?: string | null;
  externalListingId?: string | null;
  sku?: string | null;
  title: string;
  unitPrice: number;
  quantity: number;
  totalItemPrice: number;
}

export interface OrderDetail {
  id: string;
  tenantId: string;
  sellerAccountId: string;
  marketplace: string;
  externalOrderId: string;
  status: string;
  subtotal: number;
  shippingCost: number;
  totalAmount: number;
  currency: string;
  buyerName?: string | null;
  buyerDocument?: string | null;
  trackingNumber?: string | null;
  cancellationReason?: string | null;
  cancelledAt?: string | null;
  returnedAt?: string | null;
  rawPayloadJson?: string | null;
  schemaVersion: string;
  origin: string;
  orderCreatedAt: string;
  syncedAt: string;
  items: OrderItemDetail[];
}

interface OrderDetailDrawerProps {
  order: OrderDetail | null;
  onClose: () => void;
}

export const OrderDetailDrawer: React.FC<OrderDetailDrawerProps> = ({ order, onClose }) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  if (!order) return null;

  const isMeli = order.marketplace === 'MERCADO_LIVRE';
  const isShopee = order.marketplace === 'SHOPEE';

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: order.currency || 'BRL',
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

  // Verificação de incompletude
  const isMissingTracking = (order.status === 'SHIPPED' || order.status === 'DELIVERED') && !order.trackingNumber;
  const isMissingBuyer = !order.buyerName;
  const hasItemsMissingSku = order.items.some((i) => !i.sku);
  const isIncomplete = isMissingTracking || isMissingBuyer || hasItemsMissingSku;

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'PAID':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300">Pago</span>;
      case 'SHIPPED':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300">Enviado</span>;
      case 'DELIVERED':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-teal-100 text-teal-800 dark:bg-teal-950 dark:text-teal-300">Entregue</span>;
      case 'CANCELLED':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-300">Cancelado</span>;
      case 'RETURNED':
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-purple-100 text-purple-800 dark:bg-purple-950 dark:text-purple-300">Devolvido</span>;
      default:
        return <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300">{status}</span>;
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 overflow-hidden bg-slate-900/60 backdrop-blur-xs animate-in fade-in duration-200"
      role="dialog"
      aria-modal="true"
      aria-labelledby="order-drawer-title"
    >
      <div className="absolute inset-y-0 right-0 max-w-full flex pl-10">
        <div className="w-screen max-w-2xl bg-white dark:bg-slate-900 shadow-2xl flex flex-col border-l border-slate-200 dark:border-slate-800">
          {/* Header */}
          <div className="p-6 border-b border-slate-200 dark:border-slate-800 flex items-start justify-between bg-slate-50/50 dark:bg-slate-800/50">
            <div>
              <div className="flex items-center gap-2 mb-2">
                <span
                  className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-bold ${
                    isMeli
                      ? 'bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300'
                      : isShopee
                      ? 'bg-orange-100 text-orange-800 dark:bg-orange-950 dark:text-orange-300'
                      : 'bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-300'
                  }`}
                >
                  {isMeli ? 'Mercado Livre' : isShopee ? 'Shopee' : order.marketplace}
                </span>
                {getStatusBadge(order.status)}
                {isIncomplete && (
                  <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-bold bg-amber-100 text-amber-900 dark:bg-amber-950 dark:text-amber-200 border border-amber-300 dark:border-amber-700">
                    ⚠ Pedido Incompleto
                  </span>
                )}
              </div>
              <h2 id="order-drawer-title" className="text-xl font-bold text-slate-900 dark:text-white">
                Pedido {order.externalOrderId}
              </h2>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                Data do pedido: {formatDate(order.orderCreatedAt)} &middot; Sincronizado: {formatDate(order.syncedAt)}
              </p>
            </div>
            <button
              onClick={onClose}
              className="p-2 min-h-[44px] min-w-[44px] rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors focus:outline-hidden focus:ring-2 focus:ring-indigo-500"
              aria-label="Fechar detalhes do pedido"
            >
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          {/* Body */}
          <div className="flex-1 overflow-y-auto p-6 space-y-6">
            {/* Aviso de Incompletude */}
            {isIncomplete && (
              <div className="p-4 rounded-xl bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800 text-sm text-amber-900 dark:text-amber-200 space-y-1">
                <p className="font-semibold flex items-center gap-1.5">
                  <svg className="w-4 h-4 text-amber-600 dark:text-amber-400 shrink-0" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                  </svg>
                  Atenção: Informações incompletas detectadas
                </p>
                <ul className="list-disc pl-5 text-xs space-y-0.5 text-amber-800 dark:text-amber-300">
                  {isMissingTracking && <li>Código de rastreamento não informado para pedido em trânsito/entregue.</li>}
                  {isMissingBuyer && <li>Identificação do comprador ausente no payload do canal.</li>}
                  {hasItemsMissingSku && <li>Um ou mais itens não possuem SKU registrado no canal.</li>}
                </ul>
              </div>
            )}

            {/* Informações do Comprador e Envio */}
            <div className="bg-slate-50 dark:bg-slate-800/40 p-4 rounded-xl border border-slate-200 dark:border-slate-800">
              <h3 className="text-sm font-semibold text-slate-900 dark:text-white mb-3 flex items-center gap-2">
                <svg className="w-4 h-4 text-indigo-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
                Dados do Comprador & Envio
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
                <div>
                  <span className="text-xs text-slate-500 dark:text-slate-400 block">Comprador</span>
                  {order.buyerName ? (
                    <span className="font-medium text-slate-900 dark:text-slate-100">{order.buyerName}</span>
                  ) : (
                    <span className="inline-block mt-0.5 px-2 py-0.5 rounded text-xs bg-slate-200 dark:bg-slate-700 text-slate-600 dark:text-slate-300">
                      Não informado
                    </span>
                  )}
                </div>
                <div>
                  <span className="text-xs text-slate-500 dark:text-slate-400 block">Documento (CPF/CNPJ)</span>
                  {order.buyerDocument ? (
                    <span className="font-mono text-xs text-slate-800 dark:text-slate-200">{order.buyerDocument}</span>
                  ) : (
                    <span className="inline-block mt-0.5 px-2 py-0.5 rounded text-xs bg-slate-200 dark:bg-slate-700 text-slate-600 dark:text-slate-300">
                      Não informado
                    </span>
                  )}
                </div>
                <div>
                  <span className="text-xs text-slate-500 dark:text-slate-400 block">Código de Rastreamento</span>
                  {order.trackingNumber ? (
                    <span className="font-mono text-xs font-semibold text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-950/50 px-2 py-0.5 rounded border border-indigo-200 dark:border-indigo-800">
                      {order.trackingNumber}
                    </span>
                  ) : (
                    <span className="inline-block mt-0.5 px-2 py-0.5 rounded text-xs bg-amber-100 dark:bg-amber-950 text-amber-800 dark:text-amber-300 font-semibold">
                      Sem rastreio
                    </span>
                  )}
                </div>
                <div>
                  <span className="text-xs text-slate-500 dark:text-slate-400 block">Status de Entrega</span>
                  <span className="text-xs text-slate-700 dark:text-slate-300 font-medium">
                    {order.status === 'DELIVERED'
                      ? 'Entrega concluída'
                      : order.status === 'SHIPPED'
                      ? 'Em transporte'
                      : order.status === 'CANCELLED'
                      ? 'Cancelado'
                      : order.status === 'RETURNED'
                      ? 'Devolvido ao remetente'
                      : 'Aguardando envio'}
                  </span>
                </div>
              </div>
            </div>

            {/* Cancelamento / Devolução */}
            {(order.status === 'CANCELLED' || order.status === 'RETURNED') && (
              <div className="bg-red-50 dark:bg-red-950/30 p-4 rounded-xl border border-red-200 dark:border-red-900">
                <h3 className="text-sm font-semibold text-red-900 dark:text-red-200 mb-2 flex items-center gap-1.5">
                  <svg className="w-4 h-4 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                  </svg>
                  {order.status === 'CANCELLED' ? 'Detalhes do Cancelamento' : 'Detalhes da Devolução'}
                </h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
                  {order.status === 'CANCELLED' && (
                    <>
                      <div>
                        <span className="text-xs text-red-700 dark:text-red-400 block">Motivo</span>
                        <span className="font-medium text-red-900 dark:text-red-100">
                          {order.cancellationReason || 'Motivo não informado pelo canal'}
                        </span>
                      </div>
                      <div>
                        <span className="text-xs text-red-700 dark:text-red-400 block">Data do Cancelamento</span>
                        <span className="text-xs font-mono text-red-900 dark:text-red-200">
                          {formatDate(order.cancelledAt) || 'Não informada'}
                        </span>
                      </div>
                    </>
                  )}
                  {order.status === 'RETURNED' && (
                    <div>
                      <span className="text-xs text-purple-700 dark:text-purple-400 block">Data da Devolução</span>
                      <span className="text-xs font-mono text-purple-900 dark:text-purple-200">
                        {formatDate(order.returnedAt) || 'Não informada'}
                      </span>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Itens do Pedido */}
            <div>
              <h3 className="text-sm font-semibold text-slate-900 dark:text-white mb-3 flex items-center justify-between">
                <span>Itens do Pedido ({order.items.length})</span>
                <span className="text-xs font-normal text-slate-500 dark:text-slate-400">
                  Valores determinísticos em BRL
                </span>
              </h3>
              <div className="border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden divide-y divide-slate-200 dark:divide-slate-800">
                {order.items.length === 0 ? (
                  <div className="p-4 text-center text-sm text-slate-500 dark:text-slate-400">
                    Nenhum item listado para este pedido.
                  </div>
                ) : (
                  order.items.map((item, idx) => (
                    <div key={item.id || idx} className="p-4 bg-white dark:bg-slate-900 hover:bg-slate-50 dark:hover:bg-slate-800/50 transition-colors">
                      <div className="flex items-start justify-between gap-4">
                        <div className="space-y-1">
                          <p className="text-sm font-semibold text-slate-900 dark:text-white leading-snug">
                            {item.title}
                          </p>
                          <div className="flex flex-wrap items-center gap-2 text-xs text-slate-500 dark:text-slate-400">
                            <span>SKU:</span>
                            {item.sku ? (
                              <span className="font-mono font-medium text-slate-800 dark:text-slate-200 bg-slate-100 dark:bg-slate-800 px-1.5 py-0.5 rounded">
                                {item.sku}
                              </span>
                            ) : (
                              <span className="px-1.5 py-0.5 rounded text-xs bg-slate-100 dark:bg-slate-800 text-slate-500">
                                Sem SKU
                              </span>
                            )}
                            {item.externalListingId && (
                              <>
                                <span>&middot;</span>
                                <span>Anúncio: <span className="font-mono">{item.externalListingId}</span></span>
                              </>
                            )}
                          </div>
                        </div>
                        <div className="text-right shrink-0">
                          <p className="text-sm font-bold text-slate-900 dark:text-white">
                            {formatCurrency(item.totalItemPrice)}
                          </p>
                          <p className="text-xs text-slate-500 dark:text-slate-400">
                            {item.quantity}x de {formatCurrency(item.unitPrice)}
                          </p>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>

            {/* Totais Financeiros */}
            <div className="bg-slate-50 dark:bg-slate-800/40 p-4 rounded-xl border border-slate-200 dark:border-slate-800 space-y-2">
              <h3 className="text-sm font-semibold text-slate-900 dark:text-white mb-2">
                Totais Financeiros do Pedido
              </h3>
              <div className="flex justify-between text-sm text-slate-600 dark:text-slate-400">
                <span>Subtotal dos Produtos</span>
                <span className="font-medium text-slate-900 dark:text-slate-200">{formatCurrency(order.subtotal)}</span>
              </div>
              <div className="flex justify-between text-sm text-slate-600 dark:text-slate-400">
                <span>Custo de Frete</span>
                <span className="font-medium text-slate-900 dark:text-slate-200">{formatCurrency(order.shippingCost)}</span>
              </div>
              <div className="pt-2 border-t border-slate-200 dark:border-slate-700 flex justify-between text-base font-bold text-slate-900 dark:text-white">
                <span>Total Consolidado</span>
                <span className="text-indigo-600 dark:text-indigo-400">{formatCurrency(order.totalAmount)}</span>
              </div>
            </div>

            {/* Proveniência e Metadados */}
            <div className="text-xs text-slate-500 dark:text-slate-400 border-t border-slate-200 dark:border-slate-800 pt-4 space-y-1">
              <p>Origem do payload: <span className="font-mono text-slate-700 dark:text-slate-300">{order.origin}</span></p>
              <p>Schema: <span className="font-mono">{order.schemaVersion}</span> &middot; ID Interno: <span className="font-mono">{order.id}</span></p>
              <p>Última reconciliação: {formatDate(order.syncedAt)}</p>
            </div>
          </div>

          {/* Footer */}
          <div className="p-4 border-t border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/50 flex justify-end">
            <button
              onClick={onClose}
              className="px-4 py-2 min-h-[44px] rounded-lg text-sm font-semibold bg-slate-200 dark:bg-slate-700 text-slate-800 dark:text-slate-200 hover:bg-slate-300 dark:hover:bg-slate-600 transition-colors focus:outline-hidden focus:ring-2 focus:ring-indigo-500"
            >
              Fechar
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
