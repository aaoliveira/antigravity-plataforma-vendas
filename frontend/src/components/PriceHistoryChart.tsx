'use client';

import React, { useState, useMemo } from 'react';

export interface MonitoredItemSnapshotDto {
  id: string;
  tenantId?: string;
  itemId?: string;
  groupId?: string;
  marketplace?: string;
  externalListingId?: string;
  price: number;
  currency: string;
  shippingCost?: number | null;
  shippingStatus?: string | null;
  availableQuantity?: number | null;
  status?: string | null;
  provenanceType?: string;
  collectedAt: string;
  observedAt: string;
  validUntil?: string | null;
  isEffectiveSnapshot?: boolean;
  idempotencyKey?: string | null;
  sourceUrl?: string | null;
  sourceType?: string | null;
  collectionStatus?: string | null;
  previousSnapshotId?: string | null;
  hasPriceChange?: boolean;
  priceChangeAmount?: number | null;
  priceChangePercent?: number | null;
  hasAvailabilityChange?: boolean;
  hasAttributeChange?: boolean;
  changeType?: string | null;
  isAvailable?: boolean; // retrocompatibilidade
  createdAt?: string;
}

export type TimeInterval = '24h' | '7d' | '30d' | '90d' | 'all';

export interface PriceHistoryChartProps {
  snapshots: MonitoredItemSnapshotDto[];
  minIntervalMinutes?: number;
  itemTitle?: string;
  marketplace?: string;
  onSelectSnapshot?: (snapshot: MonitoredItemSnapshotDto) => void;
}

interface GapInfo {
  id: string;
  startX: number;
  endX: number;
  startObservedAt: string;
  endObservedAt: string;
  durationHours: string;
}

export function PriceHistoryChart({
  snapshots = [],
  minIntervalMinutes = 60,
  itemTitle,
  marketplace,
  onSelectSnapshot,
}: PriceHistoryChartProps) {
  const [selectedInterval, setSelectedInterval] = useState<TimeInterval>('all');
  const [viewMode, setViewMode] = useState<'chart' | 'table'>('chart');
  const [activeSnapshot, setActiveSnapshot] = useState<MonitoredItemSnapshotDto | null>(null);
  const [activeGap, setActiveGap] = useState<GapInfo | null>(null);

  // 1. Filtragem por Período
  const filteredSnapshots = useMemo(() => {
    if (!snapshots || snapshots.length === 0) return [];

    const now = Date.now();
    let cutoffMs = 0;

    switch (selectedInterval) {
      case '24h':
        cutoffMs = now - 24 * 60 * 60 * 1000;
        break;
      case '7d':
        cutoffMs = now - 7 * 24 * 60 * 60 * 1000;
        break;
      case '30d':
        cutoffMs = now - 30 * 24 * 60 * 60 * 1000;
        break;
      case '90d':
        cutoffMs = now - 90 * 24 * 60 * 60 * 1000;
        break;
      case 'all':
      default:
        cutoffMs = 0;
        break;
    }

    const filtered = cutoffMs > 0
      ? snapshots.filter((s) => new Date(s.observedAt).getTime() >= cutoffMs)
      : snapshots;

    // Ordenar cronologicamente crescente para renderização da curva temporal
    return [...filtered].sort(
      (a, b) => new Date(a.observedAt).getTime() - new Date(b.observedAt).getTime()
    );
  }, [snapshots, selectedInterval]);

  // 2. Cálculo de Estatísticas do Período
  const stats = useMemo(() => {
    if (filteredSnapshots.length === 0) {
      return { min: 0, max: 0, current: 0, delta: 0, deltaPct: 0, count: 0 };
    }

    const prices = filteredSnapshots.map((s) => Number(s.price));
    const min = Math.min(...prices);
    const max = Math.max(...prices);
    const first = prices[0];
    const current = prices[prices.length - 1];
    const delta = current - first;
    const deltaPct = first > 0 ? (delta / first) * 100 : 0;

    return { min, max, current, delta, deltaPct, count: filteredSnapshots.length };
  }, [filteredSnapshots]);

  // Formatadores
  const formatCurrency = (val: number, cur = 'BRL') => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: cur }).format(val);
  };

  const formatDate = (dateStr: string) => {
    try {
      return new Date(dateStr).toLocaleString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return dateStr;
    }
  };

  const formatShortDate = (dateStr: string) => {
    try {
      return new Date(dateStr).toLocaleDateString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
      });
    } catch {
      return dateStr;
    }
  };

  // 3. Dimensões do Gráfico SVG
  const svgWidth = 600;
  const svgHeight = 220;
  const padding = { top: 25, right: 30, bottom: 40, left: 60 };
  const plotWidth = svgWidth - padding.left - padding.right;
  const plotHeight = svgHeight - padding.top - padding.bottom;

  // 4. Mapeamento de Coordenadas e Detecção Estrita de Lacunas (Gaps)
  const { points, segments, gaps } = useMemo(() => {
    if (filteredSnapshots.length === 0) {
      return { points: [], segments: [], gaps: [] };
    }

    const prices = filteredSnapshots.map((s) => Number(s.price));
    let yMin = Math.min(...prices);
    let yMax = Math.max(...prices);

    if (yMin === yMax) {
      yMin = Math.max(0, yMin - yMin * 0.1 || 10);
      yMax = yMax + (yMax * 0.1 || 20);
    } else {
      const margin = (yMax - yMin) * 0.15;
      yMin = Math.max(0, yMin - margin);
      yMax = yMax + margin;
    }

    const tMin = new Date(filteredSnapshots[0].observedAt).getTime();
    const tMax = new Date(filteredSnapshots[filteredSnapshots.length - 1].observedAt).getTime();
    const tRange = tMax - tMin || 3600000; // Evita divisão por zero se apenas 1 observação

    // Coordenadas dos pontos
    const mappedPoints = filteredSnapshots.map((snap) => {
      const t = new Date(snap.observedAt).getTime();
      const x =
        filteredSnapshots.length === 1
          ? padding.left + plotWidth / 2
          : padding.left + ((t - tMin) / tRange) * plotWidth;
      const y = padding.top + plotHeight - ((Number(snap.price) - yMin) / (yMax - yMin)) * plotHeight;
      return { x, y, snapshot: snap };
    });

    // Detecção de Lacunas (Gap se intervalo entre observações consecutivas > 2x intervalo configurado)
    const gapThresholdMs = (minIntervalMinutes || 60) * 2 * 60 * 1000;
    const detectedSegments: Array<typeof mappedPoints> = [];
    const detectedGaps: GapInfo[] = [];

    if (mappedPoints.length > 0) {
      let currentSegment: typeof mappedPoints = [mappedPoints[0]];

      for (let i = 1; i < mappedPoints.length; i++) {
        const prevPoint = mappedPoints[i - 1];
        const currPoint = mappedPoints[i];
        const prevT = new Date(prevPoint.snapshot.observedAt).getTime();
        const currT = new Date(currPoint.snapshot.observedAt).getTime();
        const diffMs = currT - prevT;

        if (diffMs > gapThresholdMs) {
          // REGRA ANTI-ALUCINAÇÃO: Lacuna identificada! Quebra a linha contínua!
          detectedSegments.push(currentSegment);
          detectedGaps.push({
            id: `gap-${prevPoint.snapshot.id}-${currPoint.snapshot.id}`,
            startX: prevPoint.x,
            endX: currPoint.x,
            startObservedAt: prevPoint.snapshot.observedAt,
            endObservedAt: currPoint.snapshot.observedAt,
            durationHours: (diffMs / (1000 * 60 * 60)).toFixed(1),
          });
          currentSegment = [currPoint];
        } else {
          currentSegment.push(currPoint);
        }
      }
      detectedSegments.push(currentSegment);
    }

    return { points: mappedPoints, segments: detectedSegments, gaps: detectedGaps };
  }, [filteredSnapshots, minIntervalMinutes, plotWidth, plotHeight, padding.left, padding.top]);

  // Seletor de pontos para navegação por teclado (Acessibilidade)
  const handleKeyDownPoint = (
    e: React.KeyboardEvent,
    index: number,
    snapshot: MonitoredItemSnapshotDto
  ) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      setActiveSnapshot(snapshot);
      setActiveGap(null);
      if (onSelectSnapshot) onSelectSnapshot(snapshot);
    } else if (e.key === 'ArrowRight' && index < points.length - 1) {
      e.preventDefault();
      const nextPoint = points[index + 1];
      setActiveSnapshot(nextPoint.snapshot);
      setActiveGap(null);
      if (onSelectSnapshot) onSelectSnapshot(nextPoint.snapshot);
    } else if (e.key === 'ArrowLeft' && index > 0) {
      e.preventDefault();
      const prevPoint = points[index - 1];
      setActiveSnapshot(prevPoint.snapshot);
      setActiveGap(null);
      if (onSelectSnapshot) onSelectSnapshot(prevPoint.snapshot);
    }
  };

  // Cores dinâmicas por tipo de alteração (ChangeType)
  const getPointColor = (snap: MonitoredItemSnapshotDto) => {
    switch (snap.changeType) {
      case 'PRICE_DECREASE':
        return '#10b981'; // Verde (Redução de preço competitiva)
      case 'PRICE_INCREASE':
        return '#f43f5e'; // Vermelho / Rosa (Aumento de preço)
      case 'AVAILABILITY_CHANGE':
        return '#f59e0b'; // Âmbar (Mudança de estoque)
      case 'STATUS_CHANGE':
        return '#8b5cf6'; // Roxo (Mudança de status)
      case 'INITIAL':
      case 'NO_CHANGE':
      default:
        return '#3b82f6'; // Azul padrão
    }
  };

  return (
    <div
      role="region"
      aria-label="Gráfico de histórico de preços e observações"
      className="p-4 sm:p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm space-y-4"
    >
      {/* 1. Header do Gráfico: Título, Filtros de Período e Alternância de Visualização */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-100 dark:border-slate-800/80 pb-3">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-base" aria-hidden="true">📈</span>
            <h3 className="text-sm font-bold text-slate-900 dark:text-white">
              Histórico de Preços & Observações
            </h3>
            {marketplace && (
              <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300">
                {marketplace}
              </span>
            )}
          </div>
          {itemTitle && (
            <p className="text-xs text-slate-500 dark:text-slate-400 truncate max-w-md mt-0.5">
              {itemTitle}
            </p>
          )}
        </div>

        {/* Filtros de Período e Toggle Gráfico / Tabela */}
        <div className="flex items-center gap-2 flex-wrap">
          {/* Seletor de Intervalo */}
          <div
            role="group"
            aria-label="Filtro de período temporal"
            className="inline-flex rounded-xl bg-slate-100 dark:bg-slate-800 p-0.5 border border-slate-200 dark:border-slate-700"
          >
            {(['24h', '7d', '30d', '90d', 'all'] as TimeInterval[]).map((interval) => (
              <button
                key={interval}
                type="button"
                onClick={() => setSelectedInterval(interval)}
                aria-pressed={selectedInterval === interval}
                className={`min-h-[44px] px-3 py-1.5 text-xs font-semibold rounded-lg transition-all ${
                  selectedInterval === interval
                    ? 'bg-white dark:bg-slate-900 text-blue-600 dark:text-blue-400 shadow-sm'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                }`}
              >
                {interval === 'all' ? 'Tudo' : interval}
              </button>
            ))}
          </div>

          {/* Toggle Gráfico vs Tabela Acessível */}
          <div className="inline-flex rounded-xl bg-slate-100 dark:bg-slate-800 p-0.5 border border-slate-200 dark:border-slate-700">
            <button
              type="button"
              onClick={() => setViewMode('chart')}
              aria-label="Visualização em Gráfico"
              aria-pressed={viewMode === 'chart'}
              className={`min-h-[44px] px-3 py-1.5 text-xs font-semibold rounded-lg transition-all flex items-center gap-1 ${
                viewMode === 'chart'
                  ? 'bg-white dark:bg-slate-900 text-blue-600 dark:text-blue-400 shadow-sm'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
              }`}
            >
              <span>📈</span>
              <span className="hidden sm:inline">Gráfico</span>
            </button>
            <button
              type="button"
              onClick={() => setViewMode('table')}
              aria-label="Visualização em Tabela Acessível"
              aria-pressed={viewMode === 'table'}
              className={`min-h-[44px] px-3 py-1.5 text-xs font-semibold rounded-lg transition-all flex items-center gap-1 ${
                viewMode === 'table'
                  ? 'bg-white dark:bg-slate-900 text-blue-600 dark:text-blue-400 shadow-sm'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
              }`}
            >
              <span>📋</span>
              <span className="hidden sm:inline">Tabela</span>
            </button>
          </div>
        </div>
      </div>

      {/* 2. Cards de KPIs Resumo do Período */}
      {filteredSnapshots.length > 0 && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-800">
            <span className="text-[11px] font-medium text-slate-500 dark:text-slate-400 block">
              Preço Atual
            </span>
            <span className="text-sm sm:text-base font-bold text-slate-900 dark:text-white mt-0.5 block">
              {formatCurrency(stats.current)}
            </span>
          </div>

          <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-800">
            <span className="text-[11px] font-medium text-slate-500 dark:text-slate-400 block">
              Mínimo no Período
            </span>
            <span className="text-sm sm:text-base font-bold text-emerald-600 dark:text-emerald-400 mt-0.5 block">
              {formatCurrency(stats.min)}
            </span>
          </div>

          <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-800">
            <span className="text-[11px] font-medium text-slate-500 dark:text-slate-400 block">
              Máximo no Período
            </span>
            <span className="text-sm sm:text-base font-bold text-slate-700 dark:text-slate-300 mt-0.5 block">
              {formatCurrency(stats.max)}
            </span>
          </div>

          <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-800">
            <span className="text-[11px] font-medium text-slate-500 dark:text-slate-400 block">
              Variação no Período
            </span>
            <span
              className={`text-sm sm:text-base font-bold mt-0.5 block ${
                stats.delta < 0
                  ? 'text-emerald-600 dark:text-emerald-400'
                  : stats.delta > 0
                  ? 'text-rose-600 dark:text-rose-400'
                  : 'text-slate-600 dark:text-slate-400'
              }`}
            >
              {stats.delta > 0 ? '+' : ''}
              {formatCurrency(stats.delta)} ({stats.deltaPct > 0 ? '+' : ''}
              {stats.deltaPct.toFixed(1)}%)
            </span>
          </div>
        </div>
      )}

      {/* 3. Renderização Condicional: Gráfico SVG vs Tabela Acessível vs Estado Vazio */}
      {filteredSnapshots.length === 0 ? (
        <div
          role="status"
          className="py-12 px-4 text-center border-2 border-dashed border-slate-200 dark:border-slate-800 rounded-2xl space-y-2"
        >
          <span className="text-3xl block" aria-hidden="true">⏱️</span>
          <h4 className="text-xs font-bold text-slate-800 dark:text-slate-200">
            Nenhum snapshot encontrado para o período selecionado ({selectedInterval})
          </h4>
          <p className="text-[11px] text-slate-500 dark:text-slate-400 max-w-sm mx-auto">
            Selecione outro período no filtro acima ou aguarde a execução das próximas coletas programadas.
          </p>
        </div>
      ) : viewMode === 'chart' ? (
        <div className="space-y-4">
          {/* Container do Gráfico SVG Responsivo */}
          <div className="w-full overflow-x-auto relative touch-pan-x">
            <svg
              viewBox={`0 0 ${svgWidth} ${svgHeight}`}
              className="w-full h-auto min-w-[340px] max-h-[260px] overflow-visible"
              role="img"
              aria-label={`Gráfico de preços com ${filteredSnapshots.length} observações coletadas. Mínimo: ${formatCurrency(stats.min)}, Máximo: ${formatCurrency(stats.max)}.`}
            >
              <defs>
                {/* Padrão hachurado para áreas de Lacuna de Coleta */}
                <pattern id="gap-pattern" width="8" height="8" patternUnits="userSpaceOnUse" patternTransform="rotate(45)">
                  <line x1="0" y1="0" x2="0" y2="8" stroke="currentColor" strokeWidth="2" className="text-amber-500/20 dark:text-amber-400/20" />
                </pattern>
                {/* Gradiente sutil para área sob a linha */}
                <linearGradient id="area-gradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#3b82f6" stopOpacity="0.25" />
                  <stop offset="100%" stopColor="#3b82f6" stopOpacity="0.0" />
                </linearGradient>
              </defs>

              {/* Linhas de Grade Horizontais */}
              {[0, 0.25, 0.5, 0.75, 1].map((ratio) => {
                const y = padding.top + plotHeight * ratio;
                return (
                  <line
                    key={`grid-${ratio}`}
                    x1={padding.left}
                    y1={y}
                    x2={svgWidth - padding.right}
                    y2={y}
                    stroke="currentColor"
                    className="text-slate-200 dark:text-slate-800/80"
                    strokeDasharray="3 3"
                    strokeWidth="1"
                  />
                );
              })}

              {/* Rótulos do Eixo Y (Preço) */}
              <text
                x={padding.left - 8}
                y={padding.top + 4}
                textAnchor="end"
                className="text-[10px] font-mono fill-slate-400 select-none"
              >
                {formatCurrency(stats.max)}
              </text>
              <text
                x={padding.left - 8}
                y={padding.top + plotHeight}
                textAnchor="end"
                className="text-[10px] font-mono fill-slate-400 select-none"
              >
                {formatCurrency(stats.min)}
              </text>

              {/* 4. Áreas de Lacunas de Coleta (GAPS) — Demarcação Visual Anti-Alucinação */}
              {gaps.map((gap) => (
                <g key={gap.id} className="cursor-pointer" onClick={() => setActiveGap(gap)}>
                  {/* Faixa vertical sombreada representando a lacuna sem dados */}
                  <rect
                    x={gap.startX}
                    y={padding.top}
                    width={Math.max(4, gap.endX - gap.startX)}
                    height={plotHeight}
                    fill="url(#gap-pattern)"
                    className="hover:opacity-80 transition-opacity"
                  />
                  {/* Linha de borda tracejada da lacuna */}
                  <line
                    x1={gap.startX}
                    y1={padding.top}
                    x2={gap.startX}
                    y2={padding.top + plotHeight}
                    stroke="#f59e0b"
                    strokeWidth="1.5"
                    strokeDasharray="4 3"
                  />
                  <line
                    x1={gap.endX}
                    y1={padding.top}
                    x2={gap.endX}
                    y2={padding.top + plotHeight}
                    stroke="#f59e0b"
                    strokeWidth="1.5"
                    strokeDasharray="4 3"
                  />
                  {/* Marcador no centro da lacuna */}
                  <rect
                    x={(gap.startX + gap.endX) / 2 - 32}
                    y={padding.top + 6}
                    width="64"
                    height="18"
                    rx="4"
                    className="fill-amber-100 dark:fill-amber-950 stroke-amber-300 dark:stroke-amber-800"
                    strokeWidth="1"
                  />
                  <text
                    x={(gap.startX + gap.endX) / 2}
                    y={padding.top + 18}
                    textAnchor="middle"
                    className="text-[9px] font-bold fill-amber-800 dark:fill-amber-300 pointer-events-none select-none"
                  >
                    ⚠️ Lacuna ({gap.durationHours}h)
                  </text>
                </g>
              ))}

              {/* 5. Linhas Segmentadas (NÃO INTERPOLA ENTRE GAPS) */}
              {segments.map((seg, sIdx) => {
                if (seg.length <= 1) return null;
                const pathD = seg.reduce(
                  (acc, pt, idx) => `${acc} ${idx === 0 ? 'M' : 'L'} ${pt.x} ${pt.y}`,
                  ''
                );

                return (
                  <path
                    key={`seg-${sIdx}`}
                    d={pathD}
                    fill="none"
                    stroke="#2563eb"
                    strokeWidth="2.5"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    className="dark:stroke-blue-500"
                  />
                );
              })}

              {/* 6. Pontos de Dados Interativos (Hover / Tap / Teclado) */}
              {points.map((pt, idx) => {
                const isSelected = activeSnapshot?.id === pt.snapshot.id;
                const color = getPointColor(pt.snapshot);

                return (
                  <g key={pt.snapshot.id}>
                    <circle
                      cx={pt.x}
                      cy={pt.y}
                      r={isSelected ? 6 : 4}
                      fill={color}
                      stroke="#ffffff"
                      strokeWidth={isSelected ? 2.5 : 1.5}
                      tabIndex={0}
                      role="button"
                      aria-label={`Observação em ${formatDate(pt.snapshot.observedAt)}: ${formatCurrency(pt.snapshot.price, pt.snapshot.currency)}`}
                      className="cursor-pointer focus:outline-none focus:ring-2 focus:ring-blue-500 transition-transform hover:scale-125"
                      onClick={() => {
                        setActiveSnapshot(pt.snapshot);
                        setActiveGap(null);
                        if (onSelectSnapshot) onSelectSnapshot(pt.snapshot);
                      }}
                      onKeyDown={(e) => handleKeyDownPoint(e, idx, pt.snapshot)}
                    />
                  </g>
                );
              })}

              {/* Rótulos do Eixo X (Data Inicial e Final) */}
              {points.length > 0 && (
                <>
                  <text
                    x={padding.left}
                    y={svgHeight - 12}
                    textAnchor="start"
                    className="text-[10px] font-medium fill-slate-400 select-none"
                  >
                    {formatShortDate(filteredSnapshots[0].observedAt)}
                  </text>
                  <text
                    x={svgWidth - padding.right}
                    y={svgHeight - 12}
                    textAnchor="end"
                    className="text-[10px] font-medium fill-slate-400 select-none"
                  >
                    {formatShortDate(filteredSnapshots[filteredSnapshots.length - 1].observedAt)}
                  </text>
                </>
              )}
            </svg>
          </div>

          {/* 7. Banner Informativo de Lacuna de Coleta (quando clicada no gráfico) */}
          {activeGap && (
            <div
              role="status"
              className="p-3 rounded-xl border border-amber-300 dark:border-amber-900/60 bg-amber-50 dark:bg-amber-950/40 text-xs text-amber-900 dark:text-amber-300 flex items-start justify-between gap-3 animate-fade-in"
            >
              <div className="space-y-1">
                <span className="font-bold flex items-center gap-1.5">
                  <span>⚠️</span> Lacuna de Coleta Identificada (~{activeGap.durationHours} horas sem dados)
                </span>
                <p className="text-[11px] text-amber-800 dark:text-amber-400 leading-relaxed">
                  Período entre <span className="font-semibold">{formatDate(activeGap.startObservedAt)}</span> e{' '}
                  <span className="font-semibold">{formatDate(activeGap.endObservedAt)}</span>.
                  Nenhuma observação foi coletada devido a pausa no agendamento ou indisponibilidade temporária da fonte.
                  <strong className="block mt-0.5">
                    A plataforma não interpola valores intermediários nem assume estabilidade artificial de preços.
                  </strong>
                </p>
              </div>
              <button
                type="button"
                onClick={() => setActiveGap(null)}
                className="text-amber-600 dark:text-amber-400 hover:text-amber-900 p-1 min-h-[44px] min-w-[44px] flex items-center justify-center font-bold"
                aria-label="Fechar aviso de lacuna"
              >
                ✕
              </button>
            </div>
          )}

          {/* 8. Tooltip de Detalhes da Observação Selecionada (Metadados de Fonte, Validade e Auditoria) */}
          {activeSnapshot && (
            <div
              role="region"
              aria-label="Detalhes da observação selecionada"
              className="p-4 rounded-xl border border-blue-200 dark:border-blue-900/60 bg-blue-50/50 dark:bg-slate-800/80 shadow-sm space-y-3 animate-fade-in"
            >
              <div className="flex items-start justify-between gap-3">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="text-base font-bold text-slate-900 dark:text-white">
                      {formatCurrency(activeSnapshot.price, activeSnapshot.currency)}
                    </span>

                    {/* Badge de Variação de Preço */}
                    {activeSnapshot.changeType === 'PRICE_INCREASE' && (
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-rose-100 text-rose-800 dark:bg-rose-950 dark:text-rose-300 flex items-center gap-0.5">
                        ▲ Aumento (+{activeSnapshot.priceChangePercent?.toFixed(1) || '0.0'}%)
                      </span>
                    )}
                    {activeSnapshot.changeType === 'PRICE_DECREASE' && (
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300 flex items-center gap-0.5">
                        ▼ Queda ({activeSnapshot.priceChangePercent?.toFixed(1) || '0.0'}%)
                      </span>
                    )}
                    {activeSnapshot.changeType === 'AVAILABILITY_CHANGE' && (
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300">
                        📦 Estoque Alterado
                      </span>
                    )}
                    {activeSnapshot.changeType === 'INITIAL' && (
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300">
                        🚀 1ª Leitura
                      </span>
                    )}
                  </div>

                  <span className="text-[11px] text-slate-500 dark:text-slate-400 block">
                    Observado em: <strong className="text-slate-700 dark:text-slate-300">{formatDate(activeSnapshot.observedAt)}</strong>
                  </span>
                </div>

                <button
                  type="button"
                  onClick={() => setActiveSnapshot(null)}
                  className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1 min-h-[44px] min-w-[44px] flex items-center justify-center font-bold"
                  aria-label="Fechar detalhes da observação"
                >
                  ✕
                </button>
              </div>

              {/* Grid de Metadados de Proveniência e Auditoria */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-2 border-t border-slate-200/60 dark:border-slate-700/60 text-[11px]">
                <div>
                  <span className="text-slate-400 block">Tipo da Fonte</span>
                  <span className="font-semibold text-slate-800 dark:text-slate-200">
                    {activeSnapshot.sourceType || 'OFFICIAL_API'}
                  </span>
                </div>

                <div>
                  <span className="text-slate-400 block">Status de Coleta</span>
                  <span className="font-semibold text-emerald-600 dark:text-emerald-400">
                    {activeSnapshot.collectionStatus || 'SUCCESS'}
                  </span>
                </div>

                <div>
                  <span className="text-slate-400 block">Estoque Disponível</span>
                  <span className="font-semibold text-slate-800 dark:text-slate-200">
                    {activeSnapshot.availableQuantity !== null && activeSnapshot.availableQuantity !== undefined
                      ? `${activeSnapshot.availableQuantity} un.`
                      : 'Não informado'}
                  </span>
                </div>

                <div>
                  <span className="text-slate-400 block">Validade do Dado</span>
                  <span className="font-semibold text-slate-800 dark:text-slate-200">
                    {activeSnapshot.validUntil ? formatDate(activeSnapshot.validUntil) : 'Até nova coleta'}
                  </span>
                </div>
              </div>

              {/* Link Seguro para a Fonte Oficial */}
              {activeSnapshot.sourceUrl && (
                <div className="pt-1">
                  <a
                    href={activeSnapshot.sourceUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="inline-flex items-center gap-1 text-[11px] font-semibold text-blue-600 dark:text-blue-400 hover:underline min-h-[44px]"
                  >
                    <span>Ver Anúncio na Fonte Oficial ↗</span>
                  </a>
                </div>
              )}
            </div>
          )}
        </div>
      ) : (
        /* 9. Modo Alternativo: Tabela Acessível de Observações (WCAG 2.2 AA) */
        <div className="overflow-x-auto">
          <table
            className="w-full text-left text-xs text-slate-700 dark:text-slate-300 border-collapse"
            aria-label="Tabela detalhada de observações de preço e coletas"
          >
            <thead>
              <tr className="border-b border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800/60 font-semibold text-slate-900 dark:text-white">
                <th scope="col" className="p-2.5">Data/Hora (Observação)</th>
                <th scope="col" className="p-2.5">Preço</th>
                <th scope="col" className="p-2.5">Variação</th>
                <th scope="col" className="p-2.5">Estoque</th>
                <th scope="col" className="p-2.5">Fonte</th>
                <th scope="col" className="p-2.5">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
              {filteredSnapshots.map((snap, idx) => {
                const prev = idx > 0 ? filteredSnapshots[idx - 1] : null;
                let hasPrecedingGap = false;
                let gapHoursStr = '';

                if (prev) {
                  const diffMs =
                    new Date(snap.observedAt).getTime() - new Date(prev.observedAt).getTime();
                  if (diffMs > (minIntervalMinutes || 60) * 2 * 60 * 1000) {
                    hasPrecedingGap = true;
                    gapHoursStr = (diffMs / (1000 * 60 * 60)).toFixed(1);
                  }
                }

                return (
                  <React.Fragment key={snap.id}>
                    {/* Linha de aviso de Lacuna de Coleta */}
                    {hasPrecedingGap && (
                      <tr className="bg-amber-50/80 dark:bg-amber-950/30 text-amber-900 dark:text-amber-300 font-medium">
                        <td colSpan={6} className="p-2 text-[11px]" role="alert">
                          ⚠️ <strong>Lacuna de Coleta (~{gapHoursStr}h):</strong> Nenhuma observação registrada nesta janela. A plataforma não interpola valores intermediários.
                        </td>
                      </tr>
                    )}
                    <tr
                      className="hover:bg-slate-50/80 dark:hover:bg-slate-800/40 cursor-pointer transition-colors"
                      onClick={() => setActiveSnapshot(snap)}
                    >
                      <td className="p-2.5 font-mono text-[11px] whitespace-nowrap">
                        {formatDate(snap.observedAt)}
                      </td>
                      <td className="p-2.5 font-bold text-slate-900 dark:text-white whitespace-nowrap">
                        {formatCurrency(snap.price, snap.currency)}
                      </td>
                      <td className="p-2.5 whitespace-nowrap">
                        {snap.changeType === 'PRICE_INCREASE' ? (
                          <span className="text-rose-600 dark:text-rose-400 font-bold">
                            ▲ +{formatCurrency(snap.priceChangeAmount || 0, snap.currency)}
                          </span>
                        ) : snap.changeType === 'PRICE_DECREASE' ? (
                          <span className="text-emerald-600 dark:text-emerald-400 font-bold">
                            ▼ {formatCurrency(Math.abs(snap.priceChangeAmount || 0), snap.currency)}
                          </span>
                        ) : snap.changeType === 'INITIAL' ? (
                          <span className="text-blue-600 dark:text-blue-400">1ª Leitura</span>
                        ) : (
                          <span className="text-slate-400">Estável</span>
                        )}
                      </td>
                      <td className="p-2.5">
                        {snap.availableQuantity !== null && snap.availableQuantity !== undefined
                          ? `${snap.availableQuantity} un.`
                          : '-'}
                      </td>
                      <td className="p-2.5 text-[11px]">
                        <span className="font-medium text-slate-800 dark:text-slate-200">
                          {snap.sourceType || 'OFFICIAL_API'}
                        </span>
                      </td>
                      <td className="p-2.5">
                        <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300">
                          {snap.collectionStatus || 'SUCCESS'}
                        </span>
                      </td>
                    </tr>
                  </React.Fragment>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
