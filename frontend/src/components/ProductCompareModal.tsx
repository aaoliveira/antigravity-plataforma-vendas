'use client';

import React, { useEffect } from 'react';

export interface NormalizedSearchResultItem {
  itemId: string;
  title: string;
  marketplace: string;
  price: number | string;
  currency: string;
  sellerName?: string;
  sellerClassification?: string;
  listingUrl?: string;
  gtin?: string;
  itemCondition?: string;
  availableQuantity?: number | null;
  sourcePolicyId: string;
  provenanceType: 'OFFICIAL' | 'OBSERVED' | 'LICENSED' | string;
  collectedAt: string;
  salesMetricNotice?: string;
}

interface ProductCompareModalProps {
  isOpen: boolean;
  onClose: () => void;
  products: NormalizedSearchResultItem[];
  onRemoveProduct: (itemId: string) => void;
}

export function ProductCompareModal({
  isOpen,
  onClose,
  products,
  onRemoveProduct,
}: ProductCompareModalProps) {
  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

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

  const isOwnListing = (item: NormalizedSearchResultItem) => {
    const classification = item.sellerClassification?.toLowerCase() || '';
    const seller = item.sellerName?.toLowerCase() || '';
    return (
      item.provenanceType === 'OFFICIAL' &&
      (classification.includes('próprio') || seller.includes('próprio'))
    );
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="compare-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/70 backdrop-blur-sm overflow-y-auto"
    >
      <div className="relative w-full max-w-5xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl shadow-2xl p-6 overflow-hidden max-h-[90vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-slate-200 dark:border-slate-800">
          <div>
            <h2
              id="compare-modal-title"
              className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2"
            >
              <span>⚖️ Comparação de Anúncios Lado a Lado</span>
              <span className="text-xs px-2 py-0.5 rounded-full bg-blue-100 text-blue-800 dark:bg-blue-900/60 dark:text-blue-300">
                {products.length} {products.length === 1 ? 'item' : 'itens'}
              </span>
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
              Contraste direto de preços, titularidade, vendedor e proveniência auditável de mercado.
            </p>
          </div>
          <button
            onClick={onClose}
            aria-label="Fechar comparação"
            className="p-2 min-h-[44px] min-w-[44px] rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            ✕
          </button>
        </div>

        {/* Comparison Content */}
        <div className="flex-1 overflow-x-auto overflow-y-auto py-4">
          {products.length === 0 ? (
            <div className="text-center py-12 text-slate-500 dark:text-slate-400">
              Nenhum produto selecionado para comparação.
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 min-w-[600px]">
              {products.map((item) => {
                const own = isOwnListing(item);
                return (
                  <div
                    key={item.itemId}
                    className={`rounded-lg border p-4 flex flex-col justify-between ${
                      own
                        ? 'border-emerald-300 dark:border-emerald-800 bg-emerald-50/30 dark:bg-emerald-950/20'
                        : 'border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/40'
                    }`}
                  >
                    <div>
                      {/* Titularidade e Remoção */}
                      <div className="flex items-center justify-between gap-2 mb-2">
                        <span
                          className={`text-xs px-2 py-0.5 rounded-md font-semibold ${
                            own
                              ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/60 dark:text-emerald-300'
                              : 'bg-amber-100 text-amber-800 dark:bg-amber-900/60 dark:text-amber-300'
                          }`}
                        >
                          {own ? '🟢 Anúncio Próprio' : '🟠 Concorrente'}
                        </span>
                        <button
                          onClick={() => onRemoveProduct(item.itemId)}
                          className="text-xs text-red-600 dark:text-red-400 hover:underline p-1 min-h-[36px]"
                          aria-label={`Remover ${item.title} da comparação`}
                        >
                          Remover
                        </button>
                      </div>

                      {/* Título */}
                      <h3 className="font-semibold text-sm text-slate-900 dark:text-white line-clamp-2 mb-2">
                        {item.title}
                      </h3>

                      {/* Preço */}
                      <div className="mb-4">
                        <span className="text-2xl font-bold text-slate-900 dark:text-white">
                          {formatPrice(item.price, item.currency)}
                        </span>
                      </div>

                      {/* Atributos comparados */}
                      <dl className="text-xs space-y-2 border-t border-slate-200 dark:border-slate-700/60 pt-3">
                        <div className="flex justify-between">
                          <dt className="text-slate-500 dark:text-slate-400">Marketplace:</dt>
                          <dd className="font-medium text-slate-800 dark:text-slate-200">
                            {item.marketplace}
                          </dd>
                        </div>
                        <div className="flex justify-between">
                          <dt className="text-slate-500 dark:text-slate-400">Vendedor:</dt>
                          <dd className="font-medium text-slate-800 dark:text-slate-200 truncate max-w-[150px]">
                            {item.sellerName || 'Não informado'}
                          </dd>
                        </div>
                        <div className="flex justify-between">
                          <dt className="text-slate-500 dark:text-slate-400">Condição:</dt>
                          <dd className="font-medium text-slate-800 dark:text-slate-200">
                            {item.itemCondition === 'NEW' ? 'Novo' : item.itemCondition || 'N/A'}
                          </dd>
                        </div>
                        <div className="flex justify-between">
                          <dt className="text-slate-500 dark:text-slate-400">GTIN/EAN:</dt>
                          <dd className="font-mono text-slate-800 dark:text-slate-200">
                            {item.gtin || 'Não cadastrado'}
                          </dd>
                        </div>
                        <div className="flex justify-between">
                          <dt className="text-slate-500 dark:text-slate-400">Disponível:</dt>
                          <dd className="font-medium text-slate-800 dark:text-slate-200">
                            {item.availableQuantity !== null && item.availableQuantity !== undefined
                              ? `${item.availableQuantity} un`
                              : 'Indisponível via API'}
                          </dd>
                        </div>
                        <div className="flex justify-between">
                          <dt className="text-slate-500 dark:text-slate-400">Proveniência:</dt>
                          <dd className="font-semibold text-blue-600 dark:text-blue-400">
                            {item.provenanceType}
                          </dd>
                        </div>
                        <div className="flex justify-between">
                          <dt className="text-slate-500 dark:text-slate-400">Política ID:</dt>
                          <dd className="font-mono text-[10px] text-slate-600 dark:text-slate-400">
                            {item.sourcePolicyId}
                          </dd>
                        </div>
                        <div className="flex justify-between">
                          <dt className="text-slate-500 dark:text-slate-400">Coletado em:</dt>
                          <dd className="text-slate-600 dark:text-slate-400">
                            {formatDate(item.collectedAt)}
                          </dd>
                        </div>
                      </dl>
                    </div>

                    {/* Rodapé do card de comparação */}
                    <div className="mt-4 pt-3 border-t border-slate-200 dark:border-slate-700/60">
                      {item.listingUrl && (
                        <a
                          href={item.listingUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="inline-flex items-center justify-center w-full px-3 py-2 min-h-[44px] rounded-lg text-xs font-medium text-blue-600 dark:text-blue-400 bg-blue-50 dark:bg-blue-950/40 hover:bg-blue-100 dark:hover:bg-blue-900/60 transition-colors"
                        >
                          Ver no Canal Oficial ↗
                        </a>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="pt-3 border-t border-slate-200 dark:border-slate-800 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 min-h-[44px] rounded-lg bg-slate-200 dark:bg-slate-800 text-slate-800 dark:text-slate-200 hover:bg-slate-300 dark:hover:bg-slate-700 text-sm font-medium transition-colors"
          >
            Fechar Comparação
          </button>
        </div>
      </div>
    </div>
  );
}
