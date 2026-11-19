'use client';

import React, { useEffect, useState, useCallback } from 'react';
import { MetricCatalogEntry } from '@/types/metrics';

export interface MetricCatalogDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  marketplace: string;
}

export const MetricCatalogDrawer: React.FC<MetricCatalogDrawerProps> = ({
  isOpen,
  onClose,
  marketplace,
}) => {
  const [catalog, setCatalog] = useState<MetricCatalogEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [filterScope, setFilterScope] = useState<'ALL' | 'ACCOUNT' | 'LISTING'>('ALL');

  const fetchCatalog = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const url = new URL('/api/metrics/catalog', window.location.origin);
      if (marketplace && marketplace !== 'ALL') {
        url.searchParams.set('marketplace', marketplace);
      }
      const res = await fetch(url.toString());
      if (!res.ok) {
        throw new Error('Falha ao carregar catálogo de métricas.');
      }
      const data: MetricCatalogEntry[] = await res.json();
      setCatalog(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erro ao buscar catálogo');
    } finally {
      setLoading(false);
    }
  }, [marketplace]);

  useEffect(() => {
    if (isOpen) {
      fetchCatalog();
    }
  }, [isOpen, fetchCatalog]);

  // Teclado: Fechar com tecla Escape
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

  const filteredCatalog = catalog.filter((entry) => {
    if (filterScope === 'ALL') return true;
    return entry.scope === filterScope;
  });

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Catálogo Oficial de Métricas e Capacidades de API"
      className="fixed inset-0 z-50 flex justify-end animate-fadeIn"
    >
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-slate-950/60 backdrop-blur-xs transition-opacity"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Painel do Drawer */}
      <div className="relative w-full max-w-2xl bg-white dark:bg-slate-900 h-full p-6 flex flex-col shadow-2xl z-10 overflow-y-auto">
        {/* Cabeçalho */}
        <div className="flex items-center justify-between pb-4 border-b border-slate-200 dark:border-slate-800">
          <div>
            <h2 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
              <span>Catálogo Oficial de Métricas</span>
              <span className="text-xs px-2 py-0.5 rounded-full bg-blue-100 dark:bg-blue-950/80 text-blue-700 dark:text-blue-300 font-semibold">
                {marketplace === 'MERCADO_LIVRE'
                  ? 'Mercado Livre'
                  : marketplace === 'SHOPEE'
                  ? 'Shopee OpenAPI v2'
                  : 'Todos os Canais'}
              </span>
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Inventário exaustivo de capacidades comprovadas, cotas de taxa e guardrails anti-alucinação.
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fechar catálogo"
            className="p-2 text-slate-400 hover:text-slate-700 dark:hover:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 rounded-lg min-w-[40px] min-h-[40px] flex items-center justify-center"
          >
            ✕
          </button>
        </div>

        {/* Filtros de Escopo */}
        <div className="flex items-center gap-2 py-3 border-b border-slate-100 dark:border-slate-800 text-xs">
          <span className="text-slate-500 dark:text-slate-400 font-medium">Filtrar por escopo:</span>
          <button
            type="button"
            onClick={() => setFilterScope('ALL')}
            className={`px-2.5 py-1 rounded-md font-medium transition-colors ${
              filterScope === 'ALL'
                ? 'bg-blue-600 text-white'
                : 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300'
            }`}
          >
            Todos ({catalog.length})
          </button>
          <button
            type="button"
            onClick={() => setFilterScope('ACCOUNT')}
            className={`px-2.5 py-1 rounded-md font-medium transition-colors ${
              filterScope === 'ACCOUNT'
                ? 'bg-blue-600 text-white'
                : 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300'
            }`}
          >
            Conta ({catalog.filter((c) => c.scope === 'ACCOUNT').length})
          </button>
          <button
            type="button"
            onClick={() => setFilterScope('LISTING')}
            className={`px-2.5 py-1 rounded-md font-medium transition-colors ${
              filterScope === 'LISTING'
                ? 'bg-blue-600 text-white'
                : 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300'
            }`}
          >
            Anúncio ({catalog.filter((c) => c.scope === 'LISTING').length})
          </button>
        </div>

        {/* Conteúdo do Catálogo */}
        <div className="flex-1 py-4 space-y-4">
          {loading ? (
            <div className="space-y-3 animate-pulse">
              {[...Array(4)].map((_, i) => (
                <div key={i} className="h-24 bg-slate-100 dark:bg-slate-800 rounded-lg" />
              ))}
            </div>
          ) : error ? (
            <div className="p-4 rounded-lg bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 text-xs text-rose-700 dark:text-rose-300">
              {error}
            </div>
          ) : filteredCatalog.length === 0 ? (
            <div className="text-center py-8 text-xs text-slate-500">
              Nenhuma métrica encontrada para os filtros selecionados.
            </div>
          ) : (
            <div className="space-y-3">
              {filteredCatalog.map((entry) => (
                <div
                  key={`${entry.marketplace}-${entry.metricKey}`}
                  className={`p-4 rounded-xl border transition-all text-xs space-y-2 ${
                    entry.available
                      ? 'bg-white dark:bg-slate-900 border-slate-200 dark:border-slate-800'
                      : 'bg-amber-50/20 dark:bg-amber-950/20 border-dashed border-amber-300 dark:border-amber-800/80'
                  }`}
                >
                  {/* Topo do Card de Métrica */}
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-sm text-slate-900 dark:text-white">
                          {entry.displayName}
                        </span>
                        <code className="text-[11px] font-mono px-1.5 py-0.5 rounded bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300">
                          {entry.metricKey}
                        </code>
                      </div>
                      <div className="flex items-center gap-2 mt-1 text-[11px] text-slate-500">
                        <span>Escopo: <strong className="text-slate-700 dark:text-slate-300">{entry.scope}</strong></span>
                        <span>&middot;</span>
                        <span>Unidade: <strong className="text-slate-700 dark:text-slate-300">{entry.unit}</strong></span>
                      </div>
                    </div>

                    <div>
                      {entry.available ? (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800">
                          Oficial Homologada
                        </span>
                      ) : (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-semibold bg-purple-50 dark:bg-purple-950/60 text-purple-700 dark:text-purple-300 border border-purple-200 dark:border-purple-800">
                          Não Disponível no Canal
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Informações Técnicas */}
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-[11px] bg-slate-50 dark:bg-slate-800/50 p-2.5 rounded-lg border border-slate-100 dark:border-slate-800">
                    <div>
                      <span className="text-slate-400 block text-[10px] uppercase font-bold">Endpoint Oficial:</span>
                      <span className="font-mono text-slate-700 dark:text-slate-300 break-all">
                        {entry.officialSourceEndpoint}
                      </span>
                    </div>
                    <div>
                      <span className="text-slate-400 block text-[10px] uppercase font-bold">Janela Máxima & Quota:</span>
                      <span className="text-slate-700 dark:text-slate-300 font-medium">
                        Até {entry.maxSupportedWindowDays} dias &middot; {entry.rateLimitPerMinute} req/min
                      </span>
                    </div>
                  </div>

                  {/* Fórmula ou Motivo de Indisponibilidade */}
                  {entry.formulaDefinition && (
                    <div className="text-[11px]">
                      <span className="text-slate-400 block text-[10px] uppercase font-bold">Fórmula Determinística:</span>
                      <code className="text-blue-600 dark:text-blue-400 font-mono bg-blue-50/60 dark:bg-blue-950/40 px-2 py-1 rounded block mt-0.5 break-all">
                        {entry.formulaDefinition}
                      </code>
                    </div>
                  )}

                  {!entry.available && entry.unavailabilityReason && (
                    <div className="text-[11px] p-2.5 rounded bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800 text-amber-900 dark:text-amber-300">
                      <strong>Guardrail Anti-Alucinação:</strong> {entry.unavailabilityReason}
                    </div>
                  )}
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Rodapé do Drawer */}
        <div className="pt-4 border-t border-slate-200 dark:border-slate-800 flex items-center justify-between">
          <span className="text-[11px] text-slate-400">
            Conforme documentação oficial Mercado Livre e Shopee OpenAPI v2
          </span>
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 rounded-lg bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-xs font-semibold text-slate-800 dark:text-slate-200 transition-colors min-h-[44px]"
          >
            Fechar
          </button>
        </div>
      </div>
    </div>
  );
};
