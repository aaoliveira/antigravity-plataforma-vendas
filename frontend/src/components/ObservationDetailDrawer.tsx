'use client';

import React, { useEffect, useState, useCallback } from 'react';

export type MarketObservationSourceType = 'OFFICIAL' | 'OBSERVED' | 'ESTIMATED' | 'USER_INPUT';
export type ItemCondition = 'NEW' | 'USED' | 'REFURBISHED' | 'NOT_SPECIFIED';
export type MatchStatus = 'MATCHED' | 'POTENTIAL_MATCH' | 'NO_MATCH';

export interface MarketObservationItem {
  id: string;
  tenantId: string;
  observedListingId: string;
  marketplace: string;
  title: string;
  price: number;
  currency: string;
  itemCondition: ItemCondition;
  sellerIdentifier?: string | null;
  sourceType: MarketObservationSourceType;
  sourceUrl: string;
  collectedAt: string;
  observedAt: string;
  validUntil: string;
  method: string;
  confidence: number;
  methodologyDescription?: string | null;
  modelVersion?: string | null;
  timeWindow?: string | null;
  gtin?: string | null;
  brand?: string | null;
  salesVolume?: number | null;
  visitsCount?: number | null;
  ctr?: number | null;
  revenue?: number | null;
  minimizedPayload?: string | null;
}

export interface MatchScoreBreakdown {
  overallScore: number;
  gtinScore?: number | null;
  titleScore: number;
  brandScore?: number | null;
  priceScore?: number | null;
  status: MatchStatus;
  explanation: string;
  thresholdApplied: number;
}

export interface ProductMatchCandidate {
  canonicalProductId: string;
  productTitle: string;
  canonicalSku?: string | null;
  brand?: string | null;
  scoreBreakdown: MatchScoreBreakdown;
}

interface ObservationDetailDrawerProps {
  observation: MarketObservationItem | null;
  onClose: () => void;
}

export function ObservationDetailDrawer({ observation, onClose }: ObservationDetailDrawerProps) {
  const [candidates, setCandidates] = useState<ProductMatchCandidate[]>([]);
  const [isLoadingMatch, setIsLoadingMatch] = useState(false);
  const [matchError, setMatchError] = useState<string | null>(null);

  const handleKeyDown = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    },
    [onClose]
  );

  useEffect(() => {
    if (observation) {
      window.addEventListener('keydown', handleKeyDown);
      document.body.style.overflow = 'hidden';
    }
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      document.body.style.overflow = 'unset';
    };
  }, [observation, handleKeyDown]);

  if (!observation) return null;

  const fetchMatches = async () => {
    setIsLoadingMatch(true);
    setMatchError(null);
    try {
      const res = await fetch(`/api/market-intelligence/observations/${observation.id}/match`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ threshold: 0.75 }),
      });
      if (!res.ok) {
        throw new Error('Falha ao processar matching da observação.');
      }
      const data: ProductMatchCandidate[] = await res.json();
      setCandidates(data);
    } catch (err: unknown) {
      setMatchError(err instanceof Error ? err.message : 'Erro ao calcular correspondência.');
    } finally {
      setIsLoadingMatch(false);
    }
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

  const renderSourceTypeBadge = (type: MarketObservationSourceType) => {
    switch (type) {
      case 'OFFICIAL':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300 border border-emerald-300 dark:border-emerald-700">
            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
            Dado Oficial
          </span>
        );
      case 'OBSERVED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-100 text-blue-800 dark:bg-blue-950/60 dark:text-blue-300 border border-blue-300 dark:border-blue-700">
            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
            </svg>
            Público Observado
          </span>
        );
      case 'ESTIMATED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300 border border-amber-400 dark:border-amber-600">
            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
            Dado Estimado (Metodologia Documentada)
          </span>
        );
      case 'USER_INPUT':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-purple-100 text-purple-800 dark:bg-purple-950/60 dark:text-purple-300 border border-purple-300 dark:border-purple-700">
            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
            </svg>
            Entrada Manual
          </span>
        );
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="observation-drawer-title"
      className="fixed inset-0 z-50 overflow-hidden bg-slate-900/60 backdrop-blur-sm flex justify-end animate-in fade-in duration-200"
    >
      <div className="w-full max-w-2xl bg-white dark:bg-slate-900 shadow-2xl h-full flex flex-col border-l border-slate-200 dark:border-slate-800 animate-in slide-in-from-right duration-300">
        {/* Header */}
        <div className="px-6 py-5 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between bg-slate-50/50 dark:bg-slate-800/40">
          <div className="flex flex-col gap-1">
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                {observation.marketplace.replace('_', ' ')}
              </span>
              <span className="text-slate-300 dark:text-slate-600">&bull;</span>
              <span className="text-xs font-mono text-slate-600 dark:text-slate-300">
                {observation.observedListingId}
              </span>
            </div>
            <h2 id="observation-drawer-title" className="text-lg font-bold text-slate-900 dark:text-white line-clamp-1">
              {observation.title}
            </h2>
          </div>
          <button
            onClick={onClose}
            className="p-2 min-w-[44px] min-h-[44px] flex items-center justify-center rounded-lg text-slate-500 hover:text-slate-900 hover:bg-slate-100 dark:text-slate-400 dark:hover:text-white dark:hover:bg-slate-800 transition-colors"
            aria-label="Fechar gaveta"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto px-6 py-6 space-y-6">
          {/* Provenience & Classification Banner */}
          <div className="p-4 rounded-xl border bg-slate-50 dark:bg-slate-800/60 border-slate-200 dark:border-slate-700 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-slate-700 dark:text-slate-300">Classificação de Proveniência:</span>
              {renderSourceTypeBadge(observation.sourceType)}
            </div>

            {/* Aviso Estrito de Estimativa */}
            {observation.sourceType === 'ESTIMATED' && (
              <div className="p-3 rounded-lg bg-amber-50 dark:bg-amber-950/40 border border-amber-300 dark:border-amber-700/60 text-xs text-amber-900 dark:text-amber-200 space-y-1.5">
                <div className="flex items-center gap-1.5 font-bold">
                  <svg className="w-4 h-4 text-amber-600 dark:text-amber-400 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                  </svg>
                  Aviso Anti-Alucinação / Transparência Estatística
                </div>
                <p>
                  Este registro é uma <strong>estimativa estatística</strong> e nunca deve ser considerado métrica oficial do marketplace.
                  Valores foram calculados com base no modelo registrado e auditável.
                </p>
                <div className="pt-1 grid grid-cols-2 gap-2 text-[11px]">
                  <div>
                    <span className="font-semibold">Versão do Modelo:</span> {observation.modelVersion || 'v1.0.0'}
                  </div>
                  <div>
                    <span className="font-semibold">Janela Temporal:</span> {observation.timeWindow || 'N/A'}
                  </div>
                  <div className="col-span-2">
                    <span className="font-semibold">Metodologia:</span> {observation.methodologyDescription || 'Estatística documental.'}
                  </div>
                </div>
              </div>
            )}

            <div className="grid grid-cols-2 gap-3 text-xs pt-1 border-t border-slate-200 dark:border-slate-700">
              <div>
                <span className="text-slate-500 dark:text-slate-400 block">Confiança dos Dados:</span>
                <span className="font-semibold text-slate-800 dark:text-slate-200">
                  {(observation.confidence * 100).toFixed(1)}%
                </span>
              </div>
              <div>
                <span className="text-slate-500 dark:text-slate-400 block">Método de Coleta:</span>
                <span className="font-mono text-slate-800 dark:text-slate-200">
                  {observation.method}
                </span>
              </div>
            </div>
          </div>

          {/* Dados Centrais do Anúncio */}
          <div className="space-y-4">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Detalhes Comerciais
            </h3>

            <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
              <div className="p-3 rounded-lg bg-slate-50 dark:bg-slate-800/40 border border-slate-100 dark:border-slate-800">
                <span className="text-xs text-slate-500 dark:text-slate-400 block">Preço Observado</span>
                <span className="text-lg font-bold text-slate-900 dark:text-white">
                  {formatCurrency(observation.price)}
                </span>
              </div>

              <div className="p-3 rounded-lg bg-slate-50 dark:bg-slate-800/40 border border-slate-100 dark:border-slate-800">
                <span className="text-xs text-slate-500 dark:text-slate-400 block">Condição</span>
                <span className="text-sm font-semibold text-slate-800 dark:text-slate-200">
                  {observation.itemCondition === 'NEW' ? 'Novo' : observation.itemCondition === 'USED' ? 'Usado' : 'Recondicionado'}
                </span>
              </div>

              <div className="p-3 rounded-lg bg-slate-50 dark:bg-slate-800/40 border border-slate-100 dark:border-slate-800">
                <span className="text-xs text-slate-500 dark:text-slate-400 block">Marca</span>
                <span className="text-sm font-semibold text-slate-800 dark:text-slate-200">
                  {observation.brand || 'Não informada'}
                </span>
              </div>

              <div className="p-3 rounded-lg bg-slate-50 dark:bg-slate-800/40 border border-slate-100 dark:border-slate-800 col-span-2 sm:col-span-1">
                <span className="text-xs text-slate-500 dark:text-slate-400 block">GTIN / Código de Barras</span>
                <span className="text-sm font-mono font-semibold text-slate-800 dark:text-slate-200">
                  {observation.gtin || 'Não informado'}
                </span>
              </div>

              <div className="p-3 rounded-lg bg-slate-50 dark:bg-slate-800/40 border border-slate-100 dark:border-slate-800 col-span-2">
                <span className="text-xs text-slate-500 dark:text-slate-400 block">Vendedor (Identificador Legítimo)</span>
                <span className="text-sm font-medium text-slate-800 dark:text-slate-200">
                  {observation.sellerIdentifier || 'Vendedor público / Não divulgado'}
                </span>
              </div>
            </div>

            {/* Link Oficial Externo Seguro */}
            <div className="pt-1">
              <a
                href={observation.sourceUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1.5 text-xs font-semibold text-blue-600 dark:text-blue-400 hover:underline min-h-[44px] py-2"
              >
                <span>Acessar Anúncio Oficial no {observation.marketplace.replace('_', ' ')}</span>
                <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
                </svg>
              </a>
            </div>
          </div>

          {/* Temporalidade */}
          <div className="space-y-3 pt-3 border-t border-slate-200 dark:border-slate-800">
            <h3 className="text-sm font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
              Rastreamento Temporal
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
              <div>
                <span className="text-slate-500 dark:text-slate-400 block">Observado na Fonte:</span>
                <span className="font-medium text-slate-800 dark:text-slate-200">{formatDate(observation.observedAt)}</span>
              </div>
              <div>
                <span className="text-slate-500 dark:text-slate-400 block">Coletado na Plataforma:</span>
                <span className="font-medium text-slate-800 dark:text-slate-200">{formatDate(observation.collectedAt)}</span>
              </div>
              <div>
                <span className="text-slate-500 dark:text-slate-400 block">Válido Até:</span>
                <span className="font-medium text-slate-800 dark:text-slate-200">{formatDate(observation.validUntil)}</span>
              </div>
            </div>
          </div>

          {/* Matching Explicável com Catálogo Canônico */}
          <div className="space-y-4 pt-3 border-t border-slate-200 dark:border-slate-800">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-sm font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                  Correspondência com Catálogo Canônico
                </h3>
                <p className="text-xs text-slate-500 dark:text-slate-400">
                  Avaliação determinística com decomposição de score e justificativa transparente.
                </p>
              </div>
              <button
                onClick={fetchMatches}
                disabled={isLoadingMatch}
                className="px-3 py-2 min-h-[44px] bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white rounded-lg text-xs font-semibold transition-colors flex items-center gap-1.5"
              >
                {isLoadingMatch ? (
                  <>
                    <svg className="w-3.5 h-3.5 animate-spin" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"></path>
                    </svg>
                    Analisando...
                  </>
                ) : (
                  <>
                    <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
                    </svg>
                    Calcular Matching
                  </>
                )}
              </button>
            </div>

            {matchError && (
              <div className="p-3 rounded-lg bg-red-50 dark:bg-red-950/40 text-red-800 dark:text-red-300 text-xs border border-red-200 dark:border-red-800">
                {matchError}
              </div>
            )}

            {candidates.length > 0 && (
              <div className="space-y-3">
                {candidates.map((cand) => (
                  <div
                    key={cand.canonicalProductId}
                    className="p-3.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 space-y-2.5"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div>
                        <h4 className="text-sm font-bold text-slate-900 dark:text-white">
                          {cand.productTitle}
                        </h4>
                        <div className="text-xs text-slate-500 dark:text-slate-400">
                          SKU: {cand.canonicalSku || 'N/A'} &bull; Marca: {cand.brand || 'N/A'}
                        </div>
                      </div>
                      <div>
                        {cand.scoreBreakdown.status === 'MATCHED' ? (
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-bold bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300 border border-emerald-300">
                            MATCHED ({(cand.scoreBreakdown.overallScore * 100).toFixed(0)}%)
                          </span>
                        ) : cand.scoreBreakdown.status === 'POTENTIAL_MATCH' ? (
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-bold bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300 border border-amber-300">
                            POTENCIAL ({(cand.scoreBreakdown.overallScore * 100).toFixed(0)}%)
                          </span>
                        ) : (
                          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-bold bg-slate-100 text-slate-600 dark:bg-slate-700 dark:text-slate-300">
                            NO MATCH ({(cand.scoreBreakdown.overallScore * 100).toFixed(0)}%)
                          </span>
                        )}
                      </div>
                    </div>

                    {/* Justificativa Explicável */}
                    <div className="p-2.5 rounded-lg bg-slate-50 dark:bg-slate-900/50 text-xs text-slate-700 dark:text-slate-300 border border-slate-100 dark:border-slate-800 space-y-1">
                      <div className="font-semibold text-slate-800 dark:text-slate-200">Justificativa do Algoritmo:</div>
                      <p className="leading-relaxed">{cand.scoreBreakdown.explanation}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Payload Minimizado */}
          {observation.minimizedPayload && (
            <div className="space-y-2 pt-3 border-t border-slate-200 dark:border-slate-800">
              <h3 className="text-sm font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                Payload Sanitizado (LGPD / Risco Zero)
              </h3>
              <pre className="p-3 rounded-lg bg-slate-100 dark:bg-slate-950 text-[11px] font-mono text-slate-700 dark:text-slate-300 overflow-x-auto max-h-40 border border-slate-200 dark:border-slate-800">
                {observation.minimizedPayload}
              </pre>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
