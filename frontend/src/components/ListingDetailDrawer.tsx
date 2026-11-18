'use client';

import React, { useEffect } from 'react';

export interface ProductSummary {
  id: string;
  title: string;
  canonicalSku?: string | null;
  brand?: string | null;
  category?: string | null;
  status: string;
}

export interface VariantSummary {
  id: string;
  variantSku?: string | null;
  gtinBarcode?: string | null;
  attributesJson?: string | null;
}

export interface ListingDetail {
  id: string;
  sellerAccountId: string;
  sellerAccountName: string;
  marketplace: string;
  externalListingId: string;
  externalSku?: string | null;
  externalVariationId?: string | null;
  title: string;
  price: number;
  currency: string;
  availableQuantity: number;
  status: string;
  permalink?: string | null;
  origin: string;
  schemaVersion: string;
  syncedAt: string;
  product?: ProductSummary | null;
  variant?: VariantSummary | null;
}

interface ListingDetailDrawerProps {
  listing: ListingDetail | null;
  onClose: () => void;
}

export const ListingDetailDrawer: React.FC<ListingDetailDrawerProps> = ({ listing, onClose }) => {
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  if (!listing) return null;

  const isMeli = listing.marketplace === 'MERCADO_LIVRE';
  const isShopee = listing.marketplace === 'SHOPEE';

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: listing.currency || 'BRL',
    }).format(val);
  };

  const formatDate = (isoString: string) => {
    try {
      return new Intl.DateTimeFormat('pt-BR', {
        dateStyle: 'short',
        timeStyle: 'medium',
      }).format(new Date(isoString));
    } catch {
      return isoString;
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Detalhes do Anúncio"
      className="fixed inset-0 z-50 flex justify-end"
    >
      {/* Backdrop */}
      <div
        onClick={onClose}
        className="fixed inset-0 bg-slate-950/60 backdrop-blur-sm transition-opacity"
        aria-hidden="true"
      />

      {/* Slide-over Drawer Panel */}
      <div className="relative w-full max-w-lg bg-white dark:bg-slate-900 h-full p-6 flex flex-col justify-between shadow-2xl z-10 overflow-y-auto">
        <div className="space-y-6">
          {/* Header */}
          <div className="flex items-start justify-between pb-4 border-b border-slate-200 dark:border-slate-800">
            <div>
              <div className="flex items-center gap-2 mb-1.5 flex-wrap">
                <span
                  className={`px-2.5 py-0.5 rounded-full text-[11px] font-bold uppercase tracking-wider ${
                    isMeli
                      ? 'bg-amber-100 text-amber-900 dark:bg-amber-950/60 dark:text-amber-300'
                      : isShopee
                      ? 'bg-orange-100 text-orange-900 dark:bg-orange-950/60 dark:text-orange-300'
                      : 'bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-200'
                  }`}
                >
                  {isMeli ? 'Mercado Livre' : isShopee ? 'Shopee' : listing.marketplace}
                </span>

                <span
                  className={`px-2 py-0.5 rounded text-[11px] font-semibold ${
                    listing.status === 'ACTIVE'
                      ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300'
                      : listing.status === 'PAUSED'
                      ? 'bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300'
                      : 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300'
                  }`}
                >
                  {listing.status === 'ACTIVE'
                    ? 'Ativo'
                    : listing.status === 'PAUSED'
                    ? 'Pausado'
                    : listing.status}
                </span>

                <span className="text-xs text-slate-500 dark:text-slate-400">
                  ID: <span className="font-mono font-medium text-slate-800 dark:text-slate-200">{listing.externalListingId}</span>
                </span>
              </div>

              <h2 className="text-lg font-bold text-slate-900 dark:text-white leading-snug">
                {listing.title}
              </h2>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                Conta: <span className="font-semibold text-slate-700 dark:text-slate-300">{listing.sellerAccountName}</span>
              </p>
            </div>

            <button
              onClick={onClose}
              aria-label="Fechar detalhes"
              className="p-2 -mr-2 text-slate-400 hover:text-slate-700 dark:hover:text-white transition-colors rounded-lg min-w-[44px] min-h-[44px] flex items-center justify-center"
            >
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          {/* Métricas Principais (Preço e Estoque) */}
          <div className="grid grid-cols-2 gap-3 p-4 rounded-xl bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-800">
            <div>
              <span className="text-[11px] font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wider block">
                Preço no Canal
              </span>
              <span className="text-xl font-extrabold text-blue-600 dark:text-blue-400">
                {formatCurrency(listing.price)}
              </span>
            </div>
            <div>
              <span className="text-[11px] font-medium text-slate-500 dark:text-slate-400 uppercase tracking-wider block">
                Estoque Disponível
              </span>
              <span className="text-xl font-extrabold text-slate-900 dark:text-white">
                {listing.availableQuantity}{' '}
                <span className="text-xs font-normal text-slate-500 dark:text-slate-400">unidades</span>
              </span>
            </div>
          </div>

          {/* Produto Canônico Vinculado (Relação 1:N) */}
          <div className="space-y-3 p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-bold text-slate-900 dark:text-white uppercase tracking-wider flex items-center gap-1.5">
                <svg className="w-4 h-4 text-indigo-600 dark:text-indigo-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
                </svg>
                Produto Canônico Mestre
              </h3>
              <span className="text-[10px] px-2 py-0.5 rounded bg-indigo-50 text-indigo-700 dark:bg-indigo-950/60 dark:text-indigo-300 font-medium">
                Inventário Central
              </span>
            </div>

            {listing.product ? (
              <div className="space-y-2 text-xs">
                <div>
                  <span className="text-slate-500 dark:text-slate-400">Título Mestre:</span>
                  <p className="font-semibold text-slate-900 dark:text-white mt-0.5">
                    {listing.product.title}
                  </p>
                </div>

                <div className="grid grid-cols-2 gap-2 pt-1">
                  <div>
                    <span className="text-slate-500 dark:text-slate-400 block text-[11px]">SKU Mestre:</span>
                    {listing.product.canonicalSku ? (
                      <span className="font-mono font-medium text-slate-800 dark:text-slate-200">
                        {listing.product.canonicalSku}
                      </span>
                    ) : (
                      <span className="inline-block px-1.5 py-0.5 rounded text-[10px] font-medium bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400">
                        Não informado
                      </span>
                    )}
                  </div>

                  <div>
                    <span className="text-slate-500 dark:text-slate-400 block text-[11px]">Marca:</span>
                    {listing.product.brand ? (
                      <span className="font-medium text-slate-800 dark:text-slate-200">
                        {listing.product.brand}
                      </span>
                    ) : (
                      <span className="inline-block px-1.5 py-0.5 rounded text-[10px] font-medium bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400">
                        Não informada
                      </span>
                    )}
                  </div>

                  <div>
                    <span className="text-slate-500 dark:text-slate-400 block text-[11px]">Categoria:</span>
                    {listing.product.category ? (
                      <span className="font-medium text-slate-800 dark:text-slate-200">
                        {listing.product.category}
                      </span>
                    ) : (
                      <span className="inline-block px-1.5 py-0.5 rounded text-[10px] font-medium bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400">
                        Não informada
                      </span>
                    )}
                  </div>

                  <div>
                    <span className="text-slate-500 dark:text-slate-400 block text-[11px]">Status Canônico:</span>
                    <span className="inline-block px-1.5 py-0.5 rounded text-[10px] font-medium bg-emerald-50 text-emerald-700 dark:bg-emerald-950/60 dark:text-emerald-300">
                      {listing.product.status}
                    </span>
                  </div>
                </div>
              </div>
            ) : (
              <div className="p-3 rounded-lg bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-900 text-xs text-amber-800 dark:text-amber-300">
                <p className="font-medium">Nenhum produto canônico vinculado</p>
                <p className="text-[11px] mt-0.5 text-amber-700 dark:text-amber-400">
                  O anúncio foi sincronizado sem SKU mestre correspondente. A vinculação pode ser realizada por SKU ou mapeamento explícito.
                </p>
              </div>
            )}
          </div>

          {/* Identificadores Técnicos e Dados Ausentes Claramente Exibidos */}
          <div className="space-y-3 p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900">
            <h3 className="text-xs font-bold text-slate-900 dark:text-white uppercase tracking-wider">
              Identificadores do Canal & Mapeamentos
            </h3>

            <div className="grid grid-cols-2 gap-3 text-xs">
              <div>
                <span className="text-slate-500 dark:text-slate-400 block text-[11px]">SKU no Canal:</span>
                {listing.externalSku ? (
                  <span className="font-mono font-medium text-slate-800 dark:text-slate-200">
                    {listing.externalSku}
                  </span>
                ) : (
                  <span className="inline-block px-2 py-0.5 rounded text-[10px] font-medium bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400">
                    Sem SKU
                  </span>
                )}
              </div>

              <div>
                <span className="text-slate-500 dark:text-slate-400 block text-[11px]">ID de Variação:</span>
                {listing.externalVariationId ? (
                  <span className="font-mono font-medium text-slate-800 dark:text-slate-200">
                    {listing.externalVariationId}
                  </span>
                ) : (
                  <span className="inline-block px-2 py-0.5 rounded text-[10px] font-medium bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-400">
                    Sem variação
                  </span>
                )}
              </div>

              <div>
                <span className="text-slate-500 dark:text-slate-400 block text-[11px]">Origem dos Dados:</span>
                <span className="font-mono text-[11px] text-slate-800 dark:text-slate-200">
                  {listing.origin}
                </span>
              </div>

              <div>
                <span className="text-slate-500 dark:text-slate-400 block text-[11px]">Versão do Schema:</span>
                <span className="font-mono text-[11px] text-slate-800 dark:text-slate-200">
                  {listing.schemaVersion}
                </span>
              </div>

              <div className="col-span-2">
                <span className="text-slate-500 dark:text-slate-400 block text-[11px]">Última Sincronização:</span>
                <span className="font-medium text-slate-800 dark:text-slate-200">
                  {formatDate(listing.syncedAt)}
                </span>
              </div>
            </div>
          </div>

          {/* Link Oficial para o Canal */}
          {listing.permalink ? (
            <a
              href={listing.permalink}
              target="_blank"
              rel="noopener noreferrer"
              className="w-full py-3 px-4 rounded-xl bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-900 dark:text-white font-semibold text-xs transition-colors flex items-center justify-center gap-2 min-h-[44px]"
            >
              <span>Ver anúncio oficial no marketplace</span>
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
              </svg>
            </a>
          ) : (
            <div className="p-3 rounded-lg bg-slate-50 dark:bg-slate-800/40 text-center text-xs text-slate-500 dark:text-slate-400">
              Link direto do anúncio não fornecido pela API oficial do canal.
            </div>
          )}
        </div>

        {/* Rodapé do Drawer */}
        <div className="pt-4 mt-6 border-t border-slate-200 dark:border-slate-800">
          <button
            type="button"
            onClick={onClose}
            className="w-full py-2.5 px-4 rounded-lg bg-slate-200 hover:bg-slate-300 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-800 dark:text-slate-200 font-semibold text-xs transition-colors min-h-[44px]"
          >
            Fechar Painel
          </button>
        </div>
      </div>
    </div>
  );
};
