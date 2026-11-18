'use client';

import React, { useState, useEffect, useCallback } from 'react';

export interface MonitoredItemSnapshotDto {
  id: string;
  tenantId: string;
  itemId: string;
  groupId: string;
  marketplace: string;
  externalListingId: string;
  price: number;
  currency: string;
  shippingCost?: number | null;
  shippingType?: string | null;
  availableQuantity?: number | null;
  listingStatus?: string | null;
  provenanceType: string;
  collectedAt: string;
  observedAt: string;
  isAvailable: boolean;
  createdAt: string;
}

export interface MonitoringTimelineDrawerProps {
  isOpen: boolean;
  item: {
    id: string;
    groupId: string;
    marketplace: string;
    externalListingId: string;
    title: string;
    sellerName?: string | null;
    sourceUrl?: string | null;
    createdAt: string;
    minIntervalMinutes?: number;
  } | null;
  onClose: () => void;
  onSnapshotCreated?: () => void;
}

export function MonitoringTimelineDrawer({
  isOpen,
  item,
  onClose,
  onSnapshotCreated,
}: MonitoringTimelineDrawerProps) {
  const [snapshots, setSnapshots] = useState<MonitoredItemSnapshotDto[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Trigger manual
  const [isTriggering, setIsTriggering] = useState(false);
  const [triggerFeedback, setTriggerFeedback] = useState<{
    status: 'SUCCESS' | 'SKIPPED' | 'FAILED';
    message: string;
  } | null>(null);

  const fetchSnapshots = useCallback(async () => {
    if (!item?.id) return;
    setIsLoading(true);
    setErrorMessage(null);

    try {
      const res = await fetch(`/api/monitoring/items/${item.id}/snapshots`);
      if (!res.ok) {
        throw new Error('Falha ao obter snapshots do item.');
      }
      const data: MonitoredItemSnapshotDto[] = await res.json();
      // Ordenar decrescente (mais recente primeiro)
      data.sort((a, b) => new Date(b.observedAt).getTime() - new Date(a.observedAt).getTime());
      setSnapshots(data);
    } catch (err: unknown) {
      setErrorMessage(err instanceof Error ? err.message : 'Erro ao carregar histórico.');
    } finally {
      setIsLoading(false);
    }
  }, [item?.id]);

  useEffect(() => {
    if (isOpen && item?.id) {
      fetchSnapshots();
      setTriggerFeedback(null);
    } else {
      setSnapshots([]);
    }
  }, [isOpen, item?.id, fetchSnapshots]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen && !isTriggering) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, isTriggering, onClose]);

  if (!isOpen || !item) return null;

  const handleTriggerSnapshot = async () => {
    setIsTriggering(true);
    setTriggerFeedback(null);

    try {
      const res = await fetch(`/api/monitoring/items/${item.id}/trigger`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ triggerSource: 'MANUAL_UI' }),
      });

      if (!res.ok) {
        throw new Error('Falha ao acionar coleta.');
      }

      const result = await res.json();
      if (result.status === 'SUCCESS') {
        setTriggerFeedback({
          status: 'SUCCESS',
          message: 'Snapshot coletado e registrado com sucesso!',
        });
        await fetchSnapshots();
        if (onSnapshotCreated) onSnapshotCreated();
      } else if (result.status === 'SKIPPED') {
        let msg = 'Coleta pulada pela política de agendamento.';
        if (result.skipReason === 'SKIPPED_WINDOW_MINIMA') {
          msg = 'Coleta pulada: janela mínima de intervalo ainda não decorrida.';
        } else if (result.skipReason === 'SKIPPED_RATE_LIMIT') {
          msg = 'Coleta pulada: limite de requisições por minuto do tenant atingido.';
        } else if (result.skipReason === 'SKIPPED_KILL_SWITCH') {
          msg = 'Coleta pulada: kill switch de emergência ativado para este canal.';
        }
        setTriggerFeedback({ status: 'SKIPPED', message: msg });
      } else {
        setTriggerFeedback({
          status: 'FAILED',
          message: result.errorMessage || 'Falha na coleta de snapshot.',
        });
      }
    } catch (err: unknown) {
      setTriggerFeedback({
        status: 'FAILED',
        message: err instanceof Error ? err.message : 'Erro ao disparar coleta.',
      });
    } finally {
      setIsTriggering(false);
    }
  };

  const formatCurrency = (val: number, cur = 'BRL') => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: cur }).format(val);
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

  const formatRelativeTime = (dateStr: string) => {
    try {
      const diffMs = Date.now() - new Date(dateStr).getTime();
      const diffMins = Math.floor(diffMs / (1000 * 60));
      if (diffMins < 1) return 'agora há pouco';
      if (diffMins < 60) return `há ${diffMins} min`;
      const diffHours = Math.floor(diffMins / 60);
      if (diffHours < 24) return `há ${diffHours} h`;
      const diffDays = Math.floor(diffHours / 24);
      return `há ${diffDays} dias`;
    } catch {
      return '';
    }
  };

  const latestSnapshot = snapshots.length > 0 ? snapshots[0] : null;
  const intervalThresholdMins = (item.minIntervalMinutes || 60) * 2; // Gap se > 2x o intervalo

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="timeline-title"
      className="fixed inset-0 z-50 overflow-hidden bg-slate-900/60 backdrop-blur-sm animate-fade-in flex justify-end"
    >
      <div className="w-full max-w-xl bg-white dark:bg-slate-900 h-full shadow-2xl border-l border-slate-200 dark:border-slate-800 flex flex-col">
        {/* Header do Drawer */}
        <div className="p-5 border-b border-slate-100 dark:border-slate-800 flex items-start justify-between gap-4">
          <div className="min-w-0 flex-1">
            <div className="flex items-center gap-2">
              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200">
                {item.marketplace}
              </span>
              <span className="font-mono text-xs text-slate-400">ID: {item.externalListingId}</span>
            </div>
            <h2 id="timeline-title" className="text-sm font-bold text-slate-900 dark:text-white mt-1 line-clamp-2">
              {item.title}
            </h2>
            {item.sellerName && (
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                Vendedor: <span className="font-medium text-slate-700 dark:text-slate-300">{item.sellerName}</span>
              </p>
            )}
          </div>

          <button
            type="button"
            onClick={onClose}
            aria-label="Fechar timeline"
            className="p-2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 min-h-[44px] min-w-[44px] flex items-center justify-center rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            ✕
          </button>
        </div>

        {/* Corpo com Scroll */}
        <div className="flex-1 overflow-y-auto p-5 space-y-6">
          {/* Card Destaque: Status da Última Coleta & Ação de Coleta Imediata */}
          <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/40 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-1.5">
                <span>📡</span> Última Coleta Efetiva
              </span>
              <button
                type="button"
                onClick={handleTriggerSnapshot}
                disabled={isTriggering}
                className="px-3 py-1.5 min-h-[44px] text-xs font-semibold rounded-xl bg-blue-600 hover:bg-blue-700 text-white transition-colors shadow-sm disabled:opacity-50 flex items-center gap-1.5"
              >
                {isTriggering ? (
                  <>
                    <svg className="animate-spin h-3.5 w-3.5 text-white" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                    </svg>
                    <span>Coletando...</span>
                  </>
                ) : (
                  <span>Coletar Agora</span>
                )}
              </button>
            </div>

            {latestSnapshot ? (
              <div className="grid grid-cols-2 gap-3 pt-1">
                <div>
                  <span className="text-[11px] text-slate-400">Preço Registrado</span>
                  <div className="text-lg font-bold text-slate-900 dark:text-white">
                    {formatCurrency(latestSnapshot.price, latestSnapshot.currency)}
                  </div>
                </div>
                <div>
                  <span className="text-[11px] text-slate-400">Horário da Coleta</span>
                  <div className="text-xs font-semibold text-slate-800 dark:text-slate-200">
                    {formatDate(latestSnapshot.observedAt)}
                  </div>
                  <span className="text-[10px] text-slate-400">
                    ({formatRelativeTime(latestSnapshot.observedAt)})
                  </span>
                </div>
              </div>
            ) : (
              <div className="text-xs text-amber-700 dark:text-amber-400 pt-1">
                ⚠️ Nenhuma coleta realizada ainda. Aguardando o primeiro snapshot efetivo.
              </div>
            )}

            {/* Feedback do Disparo Manual */}
            {triggerFeedback && (
              <div
                role="status"
                className={`p-2.5 rounded-lg text-xs border ${
                  triggerFeedback.status === 'SUCCESS'
                    ? 'bg-emerald-50 dark:bg-emerald-950/40 border-emerald-200 dark:border-emerald-800 text-emerald-800 dark:text-emerald-300'
                    : triggerFeedback.status === 'SKIPPED'
                    ? 'bg-amber-50 dark:bg-amber-950/40 border-amber-200 dark:border-amber-800 text-amber-800 dark:text-amber-300'
                    : 'bg-rose-50 dark:bg-rose-950/40 border-rose-200 dark:border-rose-800 text-rose-800 dark:text-rose-300'
                }`}
              >
                {triggerFeedback.message}
              </div>
            )}
          </div>

          {/* Erro Geral */}
          {errorMessage && (
            <div
              role="alert"
              className="p-3 bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 text-xs text-rose-800 dark:text-rose-300 rounded-xl"
            >
              {errorMessage}
            </div>
          )}

          {/* Timeline */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-bold text-slate-700 dark:text-slate-300 uppercase tracking-wider">
                Linha do Tempo de Preços & Observações
              </h3>
              <span className="text-[11px] text-slate-400">
                {snapshots.length} {snapshots.length === 1 ? 'leitura' : 'leituras'}
              </span>
            </div>

            {isLoading ? (
              <div className="py-8 text-center text-xs text-slate-400" role="status">
                Carregando histórico de snapshots...
              </div>
            ) : snapshots.length === 0 ? (
              <div className="py-8 px-4 text-center border-2 border-dashed border-slate-200 dark:border-slate-800 rounded-2xl space-y-2">
                <span className="text-2xl block">⏱️</span>
                <h4 className="text-xs font-bold text-slate-800 dark:text-slate-200">
                  Aguardando Primeira Coleta Efetiva
                </h4>
                <p className="text-[11px] text-slate-500 dark:text-slate-400 max-w-sm mx-auto leading-relaxed">
                  O item foi incluído no monitoramento em{' '}
                  <span className="font-semibold text-slate-700 dark:text-slate-300">
                    {formatDate(item.createdAt)}
                  </span>
                  . O histórico inicia formalmente após o primeiro snapshot coletado.
                </p>
                <div className="pt-2">
                  <button
                    type="button"
                    onClick={handleTriggerSnapshot}
                    disabled={isTriggering}
                    className="px-4 py-2 min-h-[44px] text-xs font-semibold rounded-xl bg-blue-600 hover:bg-blue-700 text-white transition-colors"
                  >
                    Disparar Primeira Leitura
                  </button>
                </div>
              </div>
            ) : (
              <div className="relative pl-6 border-l-2 border-blue-200 dark:border-blue-900/60 space-y-6 my-4">
                {snapshots.map((snap, idx) => {
                  const prevSnap = idx < snapshots.length - 1 ? snapshots[idx + 1] : null;

                  // Cálculo de Gap Temporal
                  let hasGap = false;
                  let gapDurationStr = '';
                  if (prevSnap) {
                    const diffMs =
                      new Date(snap.observedAt).getTime() - new Date(prevSnap.observedAt).getTime();
                    const diffMins = Math.floor(diffMs / (1000 * 60));
                    if (diffMins > intervalThresholdMins) {
                      hasGap = true;
                      const hours = (diffMins / 60).toFixed(1);
                      gapDurationStr = `${hours} horas`;
                    }
                  }

                  // Cálculo de Variação de Preço em relação ao snapshot anterior
                  let priceDiffNode: React.ReactNode = null;
                  if (prevSnap && prevSnap.price > 0) {
                    const diff = snap.price - prevSnap.price;
                    const pct = ((diff / prevSnap.price) * 100).toFixed(1);
                    if (diff > 0) {
                      priceDiffNode = (
                        <span className="text-[10px] font-bold text-rose-600 dark:text-rose-400 flex items-center gap-0.5">
                          ▲ +{formatCurrency(diff, snap.currency)} (+{pct}%)
                        </span>
                      );
                    } else if (diff < 0) {
                      const absDiff = Math.abs(diff);
                      const absPct = Math.abs(Number(pct));
                      priceDiffNode = (
                        <span className="text-[10px] font-bold text-emerald-600 dark:text-emerald-400 flex items-center gap-0.5">
                          ▼ {formatCurrency(absDiff, snap.currency)} (-{absPct}%)
                        </span>
                      );
                    } else {
                      priceDiffNode = (
                        <span className="text-[10px] text-slate-400">Estável (=)</span>
                      );
                    }
                  }

                  return (
                    <div key={snap.id} className="relative group">
                      {/* Marcador de Nó na Linha */}
                      <div className="absolute -left-[31px] top-1.5 w-3.5 h-3.5 rounded-full border-2 border-blue-600 bg-white dark:bg-slate-900 group-hover:scale-125 transition-transform" />

                      {/* Conteúdo do Snapshot */}
                      <div className="p-3.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-800/60 shadow-sm space-y-1.5">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-2">
                            <span className="text-sm font-bold text-slate-900 dark:text-white">
                              {formatCurrency(snap.price, snap.currency)}
                            </span>
                            {priceDiffNode}
                          </div>

                          <span
                            className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                              snap.isAvailable
                                ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/60 dark:text-emerald-300'
                                : 'bg-rose-100 text-rose-800 dark:bg-rose-900/60 dark:text-rose-300'
                            }`}
                          >
                            {snap.isAvailable ? 'Disponível' : 'Pausado / Sem Estoque'}
                          </span>
                        </div>

                        <div className="flex flex-wrap items-center justify-between gap-2 text-[11px] text-slate-500 dark:text-slate-400 pt-1 border-t border-slate-100 dark:border-slate-700/50">
                          <span>📅 {formatDate(snap.observedAt)}</span>
                          <span className="font-mono text-[10px] text-slate-400">
                            Fonte: {snap.provenanceType}
                          </span>
                        </div>
                      </div>

                      {/* Sinalização de Gap Temporal detectado entre snapshots */}
                      {hasGap && (
                        <div className="my-3 py-2 px-3 rounded-lg border border-amber-300 dark:border-amber-900/60 bg-amber-50/80 dark:bg-amber-950/30 text-amber-900 dark:text-amber-300 text-[11px] flex items-start gap-2">
                          <span className="text-base leading-none">⚠️</span>
                          <div>
                            <span className="font-semibold block">
                              Gap de Coleta Detectado (~{gapDurationStr})
                            </span>
                            <span className="text-[10px] text-amber-800 dark:text-amber-400 leading-tight block mt-0.5">
                              Nenhum snapshot foi capturado nesta janela (agendamento pulado ou pausa temporária).
                              A plataforma não inventa pontos intermediários.
                            </span>
                          </div>
                        </div>
                      )}
                    </div>
                  );
                })}

                {/* Marcador de Fundação: Inclusão do Item */}
                <div className="relative pt-2">
                  <div className="absolute -left-[31px] top-3.5 w-3.5 h-3.5 rounded-full bg-slate-400 dark:bg-slate-600" />
                  <div className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900 text-xs text-slate-500 dark:text-slate-400 space-y-1">
                    <span className="font-semibold text-slate-700 dark:text-slate-300 block">
                      🚀 Início do Monitoramento
                    </span>
                    <p className="text-[11px]">
                      Anúncio incluído em {formatDate(item.createdAt)}.
                      Conforme a política anti-retroatividade, dados históricos iniciam a partir deste momento.
                    </p>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Footer do Drawer */}
        <div className="p-4 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
          {item.sourceUrl ? (
            <a
              href={item.sourceUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-xs font-semibold text-blue-600 dark:text-blue-400 hover:underline min-h-[44px] flex items-center"
            >
              Abrir Anúncio Oficial ↗
            </a>
          ) : (
            <span className="text-xs text-slate-400">Sem link direto da fonte</span>
          )}

          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2 min-h-[44px] text-xs font-semibold text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-xl transition-colors"
          >
            Fechar
          </button>
        </div>
      </div>
    </div>
  );
}
