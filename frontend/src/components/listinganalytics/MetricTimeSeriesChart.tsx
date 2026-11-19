'use client';

import React, { useState, useMemo } from 'react';
import { AccountMetricDto, MetricAvailabilityStatus } from '@/types/metrics';

export interface MetricTimeSeriesChartProps {
  metrics: AccountMetricDto[];
  startDate: string; // YYYY-MM-DD
  endDate: string; // YYYY-MM-DD
  timezoneName?: string;
}

type AvailableMetricKey =
  | 'VISITS'
  | 'ORDERS_COUNT'
  | 'UNITS_SOLD'
  | 'REVENUE'
  | 'CONVERSION_RATE_ORDERS'
  | 'CTR';

interface DayPoint {
  date: string; // YYYY-MM-DD
  formattedDate: string;
  hasRecord: boolean;
  value: number | null;
  status: MetricAvailabilityStatus;
  unavailabilityReason?: string | null;
  endpoint?: string;
}

interface ContinuousSegment {
  points: { x: number; y: number; day: DayPoint }[];
}

export const MetricTimeSeriesChart: React.FC<MetricTimeSeriesChartProps> = ({
  metrics,
  startDate,
  endDate,
  timezoneName = 'America/Sao_Paulo (UTC-03:00)',
}) => {
  const [selectedMetric, setSelectedMetric] = useState<AvailableMetricKey>('VISITS');
  const [viewMode, setViewMode] = useState<'chart' | 'table'>('chart');
  const [hoveredPoint, setHoveredPoint] = useState<DayPoint | null>(null);

  const metricLabels: Record<AvailableMetricKey, { label: string; unit: string; isCurrency?: boolean; isPercent?: boolean }> = {
    VISITS: { label: 'Visitas Diárias', unit: 'visitas' },
    ORDERS_COUNT: { label: 'Pedidos Diários', unit: 'pedidos' },
    UNITS_SOLD: { label: 'Unidades Vendidas', unit: 'unidades' },
    REVENUE: { label: 'Receita Bruta (BRL)', unit: 'R$', isCurrency: true },
    CONVERSION_RATE_ORDERS: { label: 'Taxa de Conversão', unit: '%', isPercent: true },
    CTR: { label: 'CTR (Cliques/Impressões)', unit: '%', isPercent: true },
  };

  // 1. Gera o calendário contínuo de dias entre startDate e endDate
  const timelineDays = useMemo(() => {
    const days: DayPoint[] = [];
    if (!startDate || !endDate) return days;

    const start = new Date(`${startDate}T12:00:00Z`);
    const end = new Date(`${endDate}T12:00:00Z`);

    if (isNaN(start.getTime()) || isNaN(end.getTime()) || start > end) return days;

    // Filtra métricas pertinentes à chave selecionada
    const relevantMetrics = metrics.filter(
      (m) =>
        m.metricKey === selectedMetric ||
        (selectedMetric === 'VISITS' && (m.metricKey === 'PAGE_VIEWS' || m.metricKey === 'UNIQUE_VISITORS'))
    );

    const curr = new Date(start);
    while (curr <= end) {
      const yyyy = curr.getUTCFullYear();
      const mm = String(curr.getUTCMonth() + 1).padStart(2, '0');
      const dd = String(curr.getUTCDate()).padStart(2, '0');
      const dateStr = `${yyyy}-${mm}-${dd}`;

      // Localized display
      const displayStr = `${dd}/${mm}`;

      // Encontra o registro que cobre este dia
      const match = relevantMetrics.find((m) => {
        const winStart = m.windowStart ? m.windowStart.substring(0, 10) : '';
        const winEnd = m.windowEnd ? m.windowEnd.substring(0, 10) : '';
        return winStart <= dateStr && dateStr <= winEnd;
      });

      if (match) {
        days.push({
          date: dateStr,
          formattedDate: displayStr,
          hasRecord: true,
          value: match.metricValue,
          status: match.availabilityStatus,
          unavailabilityReason: match.unavailabilityReason,
          endpoint: match.sourceEndpoint,
        });
      } else {
        // Lacuna de coleta explícita: sem registro
        days.push({
          date: dateStr,
          formattedDate: displayStr,
          hasRecord: false,
          value: null,
          status: 'UNAVAILABLE',
          unavailabilityReason: 'Nenhuma coleta oficial registrada nesta data (lacuna de dados).',
        });
      }

      curr.setUTCDate(curr.getUTCDate() + 1);
    }

    return days;
  }, [metrics, startDate, endDate, selectedMetric]);

  // Identificação de lacunas de coleta
  const detectedGaps = useMemo(() => {
    return timelineDays.filter((d) => !d.hasRecord || d.status !== 'AVAILABLE');
  }, [timelineDays]);

  // Cálculo de escala (Min e Max válidos)
  const { minVal, maxVal, segments } = useMemo(() => {
    const validValues = timelineDays
      .filter((d) => d.hasRecord && d.status === 'AVAILABLE' && d.value !== null && !isNaN(d.value))
      .map((d) => d.value as number);

    if (validValues.length === 0) {
      return { minVal: 0, maxVal: 100, segments: [] };
    }

    const min = Math.min(0, ...validValues);
    const rawMax = Math.max(...validValues);
    const max = rawMax === 0 ? 10 : rawMax * 1.15; // 15% padding no topo

    // Dimensões do SVG do gráfico
    const chartWidth = 700;
    const chartHeight = 220;
    const paddingX = 40;
    const paddingY = 25;
    const usableW = chartWidth - paddingX * 2;
    const usableH = chartHeight - paddingY * 2;

    const totalDays = timelineDays.length;
    const stepX = totalDays > 1 ? usableW / (totalDays - 1) : usableW / 2;

    // Segmentação estrita: quebra a linha em lacunas (sem interpolação falsa de dados ausentes)
    const segs: ContinuousSegment[] = [];
    let currentSeg: ContinuousSegment | null = null;

    timelineDays.forEach((day, index) => {
      const x = paddingX + index * stepX;

      if (day.hasRecord && day.status === 'AVAILABLE' && day.value !== null) {
        const normY = (day.value - min) / (max - min || 1);
        const y = chartHeight - paddingY - normY * usableH;

        if (!currentSeg) {
          currentSeg = { points: [] };
          segs.push(currentSeg);
        }
        currentSeg.points.push({ x, y, day });
      } else {
        // Quebra a linha ao encontrar lacuna de dados
        currentSeg = null;
      }
    });

    return { minVal: min, maxVal: max, segments: segs };
  }, [timelineDays]);

  const formatNumber = (val: number | null) => {
    if (val === null || val === undefined) return '—';
    const cfg = metricLabels[selectedMetric];
    if (cfg.isCurrency) {
      return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val);
    }
    if (cfg.isPercent) {
      return `${Number(val).toFixed(2)}%`;
    }
    return new Intl.NumberFormat('pt-BR').format(val);
  };

  const chartWidth = 700;
  const chartHeight = 220;
  const paddingX = 40;
  const paddingY = 25;
  const totalDays = timelineDays.length;
  const stepX = totalDays > 1 ? (chartWidth - paddingX * 2) / (totalDays - 1) : (chartWidth - paddingX * 2) / 2;

  return (
    <div className="bg-white dark:bg-slate-900 rounded-xl p-5 border border-slate-200 dark:border-slate-800 shadow-sm space-y-4">
      {/* Header com Seletor de Métrica + Alternador Visual/Tabela + Timezone */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3 border-b border-slate-100 dark:border-slate-800">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="text-sm font-bold text-slate-900 dark:text-white">
              Evolução Temporal Diária
            </h3>
            <span className="text-[11px] px-2 py-0.5 rounded bg-blue-50 dark:bg-blue-950/60 text-blue-700 dark:text-blue-300 font-medium">
              Sem Interpolação Oculta
            </span>
          </div>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            Fuso Horário de Referência: <span className="font-semibold text-slate-700 dark:text-slate-300">{timezoneName}</span>
          </p>
        </div>

        <div className="flex items-center gap-2 flex-wrap">
          {/* Seletor de Métrica */}
          <label htmlFor="metric-select" className="sr-only">Selecionar Métrica</label>
          <select
            id="metric-select"
            value={selectedMetric}
            onChange={(e) => setSelectedMetric(e.target.value as AvailableMetricKey)}
            className="text-xs bg-slate-50 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 rounded-lg px-2.5 py-1.5 font-medium text-slate-800 dark:text-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[36px]"
          >
            <option value="VISITS">Visitas Diárias</option>
            <option value="ORDERS_COUNT">Pedidos Diários</option>
            <option value="UNITS_SOLD">Unidades Vendidas</option>
            <option value="REVENUE">Receita Bruta (BRL)</option>
            <option value="CONVERSION_RATE_ORDERS">Taxa de Conversão (%)</option>
            <option value="CTR">CTR (%)</option>
          </select>

          {/* Toggle Gráfico / Tabela */}
          <div className="inline-flex rounded-lg border border-slate-200 dark:border-slate-700 p-0.5 bg-slate-100 dark:bg-slate-800 text-xs">
            <button
              type="button"
              onClick={() => setViewMode('chart')}
              className={`px-2.5 py-1 rounded font-medium transition-colors ${
                viewMode === 'chart'
                  ? 'bg-white dark:bg-slate-700 text-blue-600 dark:text-white shadow-sm'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
              }`}
            >
              Gráfico
            </button>
            <button
              type="button"
              onClick={() => setViewMode('table')}
              className={`px-2.5 py-1 rounded font-medium transition-colors ${
                viewMode === 'table'
                  ? 'bg-white dark:bg-slate-700 text-blue-600 dark:text-white shadow-sm'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900'
              }`}
            >
              Tabela Diária
            </button>
          </div>
        </div>
      </div>

      {/* Alerta de Lacunas Detectadas */}
      {detectedGaps.length > 0 && (
        <div
          role="region"
          aria-label="Aviso de lacunas de coleta"
          className="flex items-start gap-2.5 p-3 rounded-lg bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800/60 text-xs text-amber-900 dark:text-amber-300"
        >
          <svg className="w-4 h-4 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
          <div>
            <span className="font-bold">
              {detectedGaps.length} {detectedGaps.length === 1 ? 'lacuna de coleta identificada' : 'lacunas de coleta identificadas'} no intervalo:
            </span>{' '}
            Os dias sem sincronização ou não suportados pelo canal oficial não são interpolados como zero nem mascarados como linha contínua.
            <div className="mt-1 flex flex-wrap gap-1">
              {detectedGaps.slice(0, 8).map((g) => (
                <span
                  key={g.date}
                  className="px-1.5 py-0.5 rounded text-[10px] bg-amber-100 dark:bg-amber-900/60 font-mono text-amber-800 dark:text-amber-200"
                >
                  {g.date} ({g.status})
                </span>
              ))}
              {detectedGaps.length > 8 && (
                <span className="text-[10px] text-amber-700 dark:text-amber-400 self-center">
                  +{detectedGaps.length - 8} outros dias
                </span>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Renderização do Modo Gráfico */}
      {viewMode === 'chart' && (
        <div className="relative">
          {timelineDays.length === 0 ? (
            <div className="h-48 flex items-center justify-center text-xs text-slate-400">
              Selecione um intervalo válido para visualizar a série temporal.
            </div>
          ) : (
            <div className="w-full overflow-x-auto pb-2">
              <svg
                viewBox={`0 0 ${chartWidth} ${chartHeight}`}
                className="w-full min-w-[500px] h-52 select-none"
                role="img"
                aria-label={`Gráfico de ${metricLabels[selectedMetric].label}`}
              >
                {/* Linhas de Grade de Fundo */}
                <line
                  x1={paddingX}
                  y1={paddingY}
                  x2={chartWidth - paddingX}
                  y2={paddingY}
                  stroke="currentColor"
                  className="text-slate-100 dark:text-slate-800"
                  strokeDasharray="4 4"
                />
                <line
                  x1={paddingX}
                  y1={chartHeight / 2}
                  x2={chartWidth - paddingX}
                  y2={chartHeight / 2}
                  stroke="currentColor"
                  className="text-slate-100 dark:text-slate-800"
                  strokeDasharray="4 4"
                />
                <line
                  x1={paddingX}
                  y1={chartHeight - paddingY}
                  x2={chartWidth - paddingX}
                  y2={chartHeight - paddingY}
                  stroke="currentColor"
                  className="text-slate-200 dark:text-slate-700"
                />

                {/* Rótulos dos Eixos Y */}
                <text
                  x={paddingX - 6}
                  y={paddingY + 4}
                  textAnchor="end"
                  className="text-[10px] fill-slate-400 font-mono"
                >
                  {formatNumber(maxVal)}
                </text>
                <text
                  x={paddingX - 6}
                  y={chartHeight - paddingY + 3}
                  textAnchor="end"
                  className="text-[10px] fill-slate-400 font-mono"
                >
                  {formatNumber(minVal)}
                </text>

                {/* Zonas de Lacuna de Dados (Destacadas em Amber com hachuras) */}
                {timelineDays.map((day, idx) => {
                  if (day.hasRecord && day.status === 'AVAILABLE') return null;
                  const x = paddingX + idx * stepX;
                  const barW = Math.max(stepX, 10);
                  return (
                    <g key={`gap-${day.date}`}>
                      <rect
                        x={x - barW / 2}
                        y={paddingY}
                        width={barW}
                        height={chartHeight - paddingY * 2}
                        className="fill-amber-500/10 dark:fill-amber-500/20"
                      />
                      <line
                        x1={x}
                        y1={paddingY}
                        x2={x}
                        y2={chartHeight - paddingY}
                        stroke="currentColor"
                        className="text-amber-400 dark:text-amber-500/50"
                        strokeDasharray="2 2"
                      />
                    </g>
                  );
                })}

                {/* Segmentos Contínuos de Linha (Discontínuos em Lacunas) */}
                {segments.map((seg, sIdx) => {
                  if (seg.points.length === 0) return null;
                  const pathD = seg.points.reduce((acc, pt, pIdx) => {
                    return pIdx === 0 ? `M ${pt.x} ${pt.y}` : `${acc} L ${pt.x} ${pt.y}`;
                  }, '');

                  return (
                    <g key={`seg-${sIdx}`}>
                      <path
                        d={pathD}
                        fill="none"
                        stroke="#2563eb"
                        strokeWidth={2.5}
                        strokeLinecap="round"
                        strokeLinejoin="round"
                      />
                    </g>
                  );
                })}

                {/* Pontos de Observação e Marcadores de Lacuna */}
                {timelineDays.map((day, idx) => {
                  const x = paddingX + idx * stepX;
                  const isAvailable = day.hasRecord && day.status === 'AVAILABLE' && day.value !== null;
                  const normY = isAvailable
                    ? (day.value! - minVal) / (maxVal - minVal || 1)
                    : 0;
                  const y = isAvailable
                    ? chartHeight - paddingY - normY * (chartHeight - paddingY * 2)
                    : chartHeight - paddingY;

                  return (
                    <g
                      key={`pt-${day.date}`}
                      className="cursor-pointer group"
                      onMouseEnter={() => setHoveredPoint(day)}
                      onMouseLeave={() => setHoveredPoint(null)}
                      tabIndex={0}
                      role="button"
                      aria-label={`${day.date}: ${isAvailable ? formatNumber(day.value) : 'Sem dados'}`}
                      onFocus={() => setHoveredPoint(day)}
                      onBlur={() => setHoveredPoint(null)}
                    >
                      {/* Ponto Válido */}
                      {isAvailable ? (
                        <>
                          <circle
                            cx={x}
                            cy={y}
                            r={4}
                            className="fill-blue-600 dark:fill-blue-400 stroke-white dark:stroke-slate-900 stroke-2 group-hover:r-6 transition-all"
                          />
                        </>
                      ) : (
                        /* Marcador de Lacuna (X vermelho/âmbar) */
                        <>
                          <circle
                            cx={x}
                            cy={y}
                            r={3}
                            className="fill-amber-500 stroke-white dark:stroke-slate-900 stroke-1"
                          />
                        </>
                      )}

                      {/* Rótulo da data no eixo X a cada poucos dias */}
                      {(idx === 0 ||
                        idx === timelineDays.length - 1 ||
                        idx % Math.ceil(timelineDays.length / 7) === 0) && (
                        <text
                          x={x}
                          y={chartHeight - 6}
                          textAnchor="middle"
                          className="text-[9px] fill-slate-400 font-mono"
                        >
                          {day.formattedDate}
                        </text>
                      )}
                    </g>
                  );
                })}
              </svg>

              {/* Tooltip Hover Acessível */}
              {hoveredPoint && (
                <div
                  className="mt-2 p-2.5 rounded-lg bg-slate-900 dark:bg-slate-800 text-white text-xs max-w-sm border border-slate-700 shadow-lg flex items-center justify-between gap-3 animate-fadeIn"
                >
                  <div>
                    <div className="font-semibold text-slate-200">
                      Data: {hoveredPoint.date} ({timezoneName.split(' ')[0]})
                    </div>
                    {hoveredPoint.hasRecord && hoveredPoint.status === 'AVAILABLE' ? (
                      <div className="text-blue-300 font-mono text-sm font-bold mt-0.5">
                        {metricLabels[selectedMetric].label}: {formatNumber(hoveredPoint.value)}
                      </div>
                    ) : (
                      <div className="text-amber-300 text-xs mt-0.5">
                        <strong>Lacuna:</strong> {hoveredPoint.unavailabilityReason || 'Sem dados'}
                      </div>
                    )}
                  </div>
                  {hoveredPoint.endpoint && (
                    <div className="text-[10px] text-slate-400 text-right font-mono truncate max-w-[150px]">
                      {hoveredPoint.endpoint}
                    </div>
                  )}
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Renderização do Modo Tabela Diária (Acessibilidade Completa) */}
      {viewMode === 'table' && (
        <div className="border border-slate-200 dark:border-slate-800 rounded-lg overflow-x-auto max-h-72">
          <table className="w-full text-xs text-left">
            <thead className="bg-slate-50 dark:bg-slate-800/80 text-slate-700 dark:text-slate-300 font-semibold sticky top-0 border-b border-slate-200 dark:border-slate-700">
              <tr>
                <th className="py-2.5 px-3">Data ({timezoneName.split(' ')[0]})</th>
                <th className="py-2.5 px-3">Valor da Métrica</th>
                <th className="py-2.5 px-3">Status de Coleta</th>
                <th className="py-2.5 px-3">Origem / Endpoint</th>
                <th className="py-2.5 px-3">Justificativa / Motivo</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-800 text-slate-800 dark:text-slate-200">
              {timelineDays.map((day) => {
                const isAvailable = day.hasRecord && day.status === 'AVAILABLE';
                return (
                  <tr
                    key={day.date}
                    className={
                      !isAvailable
                        ? 'bg-amber-50/30 dark:bg-amber-950/20'
                        : 'hover:bg-slate-50 dark:hover:bg-slate-800/40'
                    }
                  >
                    <td className="py-2 px-3 font-mono font-medium">{day.date}</td>
                    <td className="py-2 px-3 font-semibold">
                      {isAvailable ? formatNumber(day.value) : <span className="text-slate-400 italic">—</span>}
                    </td>
                    <td className="py-2 px-3">
                      {isAvailable ? (
                        <span className="text-emerald-600 dark:text-emerald-400 font-medium">Disponível</span>
                      ) : (
                        <span className="text-amber-600 dark:text-amber-400 font-medium">
                          {day.status} (Lacuna)
                        </span>
                      )}
                    </td>
                    <td className="py-2 px-3 font-mono text-[11px] text-slate-500">
                      {day.endpoint || '—'}
                    </td>
                    <td className="py-2 px-3 text-[11px] text-slate-600 dark:text-slate-400">
                      {day.unavailabilityReason || 'Dado oficial ingerido com sucesso.'}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};
