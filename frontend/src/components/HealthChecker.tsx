'use client';

import React, { useEffect, useState, useCallback } from 'react';
import { fetchHealth, HealthResponse } from '@/lib/api';

export function HealthChecker() {
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);
  const [latency, setLatency] = useState<number | null>(null);
  const [lastChecked, setLastChecked] = useState<string | null>(null);

  const checkStatus = useCallback(async () => {
    setLoading(true);
    setError(null);
    const start = performance.now();
    try {
      const data = await fetchHealth();
      const end = performance.now();
      setLatency(Math.round(end - start));
      setHealth(data);
      setLastChecked(new Date().toLocaleTimeString());
    } catch (err: unknown) {
      const end = performance.now();
      setLatency(Math.round(end - start));
      setHealth({ status: 'DOWN' });
      setError(err instanceof Error ? err.message : 'Backend unreachable');
      setLastChecked(new Date().toLocaleTimeString());
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    checkStatus();
  }, [checkStatus]);

  const isUp = health?.status === 'UP';

  return (
    <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-6 shadow-xl backdrop-blur">
      <div className="flex items-center justify-between pb-4 border-b border-slate-800">
        <div>
          <h2 className="text-lg font-semibold text-white">Backend Health Status</h2>
          <p className="text-sm text-slate-400">Verificação em tempo real via /actuator/health</p>
        </div>
        <button
          onClick={checkStatus}
          disabled={loading}
          className="rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white transition hover:bg-blue-500 disabled:opacity-50"
        >
          {loading ? 'Verificando...' : 'Revalidar'}
        </button>
      </div>

      <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <div className="rounded-lg border border-slate-800/80 bg-slate-950/40 p-4">
          <span className="text-xs uppercase tracking-wider text-slate-500">Status</span>
          <div className="mt-2 flex items-center gap-2">
            <span
              className={`inline-block h-3 w-3 rounded-full ${
                loading
                  ? 'bg-amber-400 animate-pulse'
                  : isUp
                  ? 'bg-emerald-400'
                  : 'bg-rose-500'
              }`}
            />
            <span
              className={`text-base font-bold ${
                loading
                  ? 'text-amber-400'
                  : isUp
                  ? 'text-emerald-400'
                  : 'text-rose-500'
              }`}
            >
              {loading ? 'VERIFICANDO' : health?.status || 'DOWN'}
            </span>
          </div>
        </div>

        <div className="rounded-lg border border-slate-800/80 bg-slate-950/40 p-4">
          <span className="text-xs uppercase tracking-wider text-slate-500">Latência</span>
          <div className="mt-2 text-base font-bold text-white">
            {latency !== null ? `${latency} ms` : '—'}
          </div>
        </div>

        <div className="rounded-lg border border-slate-800/80 bg-slate-950/40 p-4">
          <span className="text-xs uppercase tracking-wider text-slate-500">Última Checagem</span>
          <div className="mt-2 text-base font-bold text-white">
            {lastChecked || '—'}
          </div>
        </div>
      </div>

      {error && (
        <div className="mt-4 rounded-lg border border-rose-900/50 bg-rose-950/30 p-3 text-xs text-rose-300">
          <strong>Aviso de Conectividade:</strong> {error}
        </div>
      )}

      {health?.components && (
        <div className="mt-4 border-t border-slate-800/80 pt-4">
          <span className="text-xs uppercase tracking-wider text-slate-400 font-medium">Componentes Detectados</span>
          <div className="mt-2 flex flex-wrap gap-2">
            {Object.entries(health.components).map(([name, comp]) => (
              <span
                key={name}
                className="inline-flex items-center gap-1.5 rounded bg-slate-800 px-2 py-1 text-xs text-slate-300"
              >
                <span
                  className={`h-1.5 w-1.5 rounded-full ${
                    comp.status === 'UP' ? 'bg-emerald-400' : 'bg-rose-400'
                  }`}
                />
                {name}: {comp.status}
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
