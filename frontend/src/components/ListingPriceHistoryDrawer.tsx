'use client';

import React, { useEffect, useState, useCallback } from 'react';

export interface ListingPricePointDto {
  id?: string;
  price: number;
  currency?: string;
  shippingCost?: number | null;
  observedAt: string;
  sourceType?: string;
  isPromotional?: boolean;
  note?: string;
}

export interface ListingPriceHistoryResponse {
  marketplace: string;
  externalListingId: string;
  title?: string;
  dataPointsCount: number;
  hasHistoricalTrend: boolean;
  minPrice?: number | null;
  maxPrice?: number | null;
  latestPrice?: number | null;
  earliestPrice?: number | null;
  overallVariationPercentage?: number | null;
  timeWindowDays?: number | null;
  trendDescription?: string | null;
  historyNotice?: string | null;
  points: ListingPricePointDto[];
}

interface ListingPriceHistoryDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  marketplace: string;
  externalListingId: string;
  listingTitle?: string;
  initialData?: ListingPriceHistoryResponse | null;
}

export function ListingPriceHistoryDrawer({
  isOpen,
  onClose,
  marketplace,
  externalListingId,
  listingTitle,
  initialData,
}: ListingPriceHistoryDrawerProps) {
  const [history, setHistory] = useState<ListingPriceHistoryResponse | null>(initialData || null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchPriceHistory = useCallback(async () => {
    if (!marketplace || !externalListingId) return;
    setIsLoading(true);
    setError(null);

    try {
      const res = await fetch(
        `/api/market-comparison/price-history?marketplace=${encodeURIComponent(
          marketplace
        )}&externalListingId=${encodeURIComponent(externalListingId)}`
      );

      if (!res.ok) {
        const errData = await res.json().catch(() => ({}));
        throw new Error(errData.message || 'Falha ao buscar histórico legítimo de preços.');
      }

      const data: ListingPriceHistoryResponse = await res.json();
      setHistory(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Erro ao consultar histórico de preços.');
    } finally {
      setIsLoading(false);
    }
  }, [marketplace, externalListingId]);

  useEffect(() => {
    if (isOpen) {
      if (initialData) {
        setHistory(initialData);
      } else {
        fetchPriceHistory();
      }
    }
  }, [isOpen, initialData, fetchPriceHistory]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const formatPrice = (val: number | null | undefined, currency = 'BRL') => {
    if (val === null || val === undefined) return '---';
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: currency || 'BRL',
    }).format(val);
  };

  const formatDate = (isoStr: string) => {
    try {
      return new Intl.DateTimeFormat('pt-BR', {
        dateStyle: 'short',
        timeStyle: 'short',
      }).format(new Date(isoStr));
    } catch {
      return isoStr;
    }
  };

  const formatPercentage = (val: number | null | undefined) => {
    if (val === null || val === undefined) return '---';
    const prefix = val > 0 ? '+' : '';
    return `${prefix}${val.toFixed(2)}%`;
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="price-history-drawer-title"
      className="fixed inset-0 z-50 flex items-center justify-end bg-slate-900/60 backdrop-blur-sm"
    >
      <div className="relative w-full max-w-2xl h-full bg-white dark:bg-slate-900 border-l border-slate-200 dark:border-slate-800 shadow-2xl flex flex-col p-6 overflow-y-auto">
        {/* Header */}
        <div className="flex items-start justify-between pb-4 border-b border-slate-200 dark:border-slate-800">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-xl">📈</span>
              <h2
                id="price-history-drawer-title"
                className="text-lg font-bold text-slate-900 dark:text-white"
              >
                Histórico Legítimo de Preços
              </h2>
            </div>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 line-clamp-2">
              {listingTitle || history?.title || `Anúncio ${externalListingId} (${marketplace})`}
            </p>
          </div>
          <button
            onClick={onClose}
            aria-label="Fechar histórico de preços"
            className="p-2 min-h-[44px] min-w-[44px] rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            ✕
          </button>
        </div>

        {/* Disclaimer Anti-Retroatividade Fictícia */}
        <div className="my-4 p-3.5 rounded-lg bg-blue-50 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-800 text-xs text-blue-900 dark:text-blue-200">
          <div className="flex items-center gap-2 font-semibold mb-1">
            <span>🛡️ Integridade dos Dados de Preço</span>
          </div>
          <p>
            {history?.historyNotice ||
              'Histórico sem retroatividade inventada: apenas coletas reais com timestamp auditável são exibidas. Nunca geramos curvas passadas artificiais.'}
          </p>
        </div>

        {/* Content Body */}
        {isLoading ? (
          <div className="flex-1 flex flex-col items-center justify-center py-12 text-slate-500">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mb-3" />
            <span className="text-sm">Consultando pontos históricos auditáveis...</span>
          </div>
        ) : error ? (
          <div className="p-4 rounded-lg bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800 text-red-800 dark:text-red-300 text-sm">
            <p className="font-semibold">Erro ao carregar histórico</p>
            <p className="text-xs mt-1">{error}</p>
            <button
              onClick={fetchPriceHistory}
              className="mt-3 px-3 py-1.5 text-xs bg-red-100 dark:bg-red-900 text-red-900 dark:text-red-100 rounded-md hover:bg-red-200 min-h-[44px]"
            >
              Tentar Novamente
            </button>
          </div>
        ) : !history || history.points.length === 0 ? (
          <div className="flex-1 flex flex-col items-center justify-center py-12 text-slate-400 text-center">
            <span className="text-3xl mb-2">🔎</span>
            <p className="font-medium text-slate-600 dark:text-slate-300">
              Nenhum ponto histórico persistido
            </p>
            <p className="text-xs text-slate-500 dark:text-slate-400 max-w-sm mt-1">
              Este anúncio foi observado recentemente e ainda não possui coletas passadas suficientes para traçar uma tendência temporal.
            </p>
          </div>
        ) : (
          <div className="space-y-6 flex-1">
            {/* KPI Cards do Histórico */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <div className="p-3 rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/50">
                <span className="text-xs text-slate-500 dark:text-slate-400">Preço Atual</span>
                <p className="text-base font-bold text-slate-900 dark:text-white mt-0.5">
                  {formatPrice(history.latestPrice)}
                </p>
              </div>

              <div className="p-3 rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/50">
                <span className="text-xs text-slate-500 dark:text-slate-400">Preço Inicial</span>
                <p className="text-base font-bold text-slate-700 dark:text-slate-300 mt-0.5">
                  {formatPrice(history.earliestPrice)}
                </p>
              </div>

              <div className="p-3 rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/50">
                <span className="text-xs text-slate-500 dark:text-slate-400">Mínimo / Máximo</span>
                <p className="text-xs font-semibold text-slate-800 dark:text-slate-200 mt-1">
                  {formatPrice(history.minPrice)} / {formatPrice(history.maxPrice)}
                </p>
              </div>

              <div className="p-3 rounded-lg border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/50">
                <span className="text-xs text-slate-500 dark:text-slate-400">Variação no Período</span>
                <p
                  className={`text-base font-bold mt-0.5 ${
                    (history.overallVariationPercentage || 0) < 0
                      ? 'text-emerald-600 dark:text-emerald-400'
                      : (history.overallVariationPercentage || 0) > 0
                      ? 'text-red-600 dark:text-red-400'
                      : 'text-slate-600 dark:text-slate-400'
                  }`}
                >
                  {formatPercentage(history.overallVariationPercentage)}
                </p>
              </div>
            </div>

            {/* Descrição de Tendência */}
            {history.trendDescription && (
              <div className="p-3 rounded-lg border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-800 text-xs text-slate-700 dark:text-slate-300">
                <span className="font-semibold text-slate-900 dark:text-white mr-1.5">
                  📊 Tendência Auditável:
                </span>
                {history.trendDescription}
              </div>
            )}

            {/* Tabela de Pontos Históricos */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <h3 className="text-xs font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                  Pontos de Observação Coletados ({history.points.length})
                </h3>
                {history.timeWindowDays !== null && history.timeWindowDays !== undefined && (
                  <span className="text-xs text-slate-500 dark:text-slate-400">
                    Janela: {history.timeWindowDays} {history.timeWindowDays === 1 ? 'dia' : 'dias'}
                  </span>
                )}
              </div>

              <div className="border border-slate-200 dark:border-slate-800 rounded-lg overflow-hidden">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 dark:bg-slate-800/80 border-b border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-300">
                    <tr>
                      <th className="py-2.5 px-3">Data/Hora</th>
                      <th className="py-2.5 px-3">Preço</th>
                      <th className="py-2.5 px-3">Frete</th>
                      <th className="py-2.5 px-3">Origem</th>
                      <th className="py-2.5 px-3">Observação</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                    {history.points.map((pt, idx) => (
                      <tr
                        key={pt.id || idx}
                        className="hover:bg-slate-50 dark:hover:bg-slate-800/40 transition-colors"
                      >
                        <td className="py-2.5 px-3 whitespace-nowrap text-slate-700 dark:text-slate-300 font-mono">
                          {formatDate(pt.observedAt)}
                        </td>
                        <td className="py-2.5 px-3 font-semibold text-slate-900 dark:text-white">
                          {formatPrice(pt.price, pt.currency)}
                          {pt.isPromotional && (
                            <span className="ml-1.5 px-1.5 py-0.5 rounded text-[10px] bg-amber-100 text-amber-800 dark:bg-amber-900/60 dark:text-amber-300">
                              Promo
                            </span>
                          )}
                        </td>
                        <td className="py-2.5 px-3 text-slate-600 dark:text-slate-400">
                          {pt.shippingCost === 0
                            ? 'Grátis'
                            : pt.shippingCost !== null && pt.shippingCost !== undefined
                            ? formatPrice(pt.shippingCost, pt.currency)
                            : 'Não inf.'}
                        </td>
                        <td className="py-2.5 px-3 text-slate-500 dark:text-slate-400">
                          {pt.sourceType || 'OBSERVED'}
                        </td>
                        <td className="py-2.5 px-3 text-slate-500 dark:text-slate-400 max-w-xs truncate">
                          {pt.note || '---'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}

        {/* Footer */}
        <div className="pt-4 border-t border-slate-200 dark:border-slate-800 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 text-sm font-medium text-slate-700 dark:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg min-h-[44px]"
          >
            Fechar
          </button>
        </div>
      </div>
    </div>
  );
}
