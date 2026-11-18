import React from 'react';

interface ModulePlaceholderProps {
  moduleId: string;
  moduleName: string;
  pillar: string;
  description: string;
  status: 'READY' | 'PLANNED';
  provenience: string;
}

export const ModulePlaceholder: React.FC<ModulePlaceholderProps> = ({
  moduleId,
  moduleName,
  pillar,
  description,
  status,
  provenience,
}) => {
  return (
    <div className="space-y-6 w-full max-w-full">
      {/* Banner de Contexto do Módulo */}
      <div className="rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900/60 p-5 sm:p-6 shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div>
            <div className="flex items-center gap-2 mb-1.5 flex-wrap">
              <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-blue-100 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300">
                {pillar}
              </span>
              <span className="text-xs font-mono px-2 py-0.5 rounded bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400">
                ID: {moduleId}
              </span>
              <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-emerald-100 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-300">
                {status}
              </span>
            </div>
            <h2 className="text-lg sm:text-2xl font-bold text-slate-900 dark:text-white">
              {moduleName}
            </h2>
            <p className="mt-1 text-xs sm:text-sm text-slate-600 dark:text-slate-300 max-w-3xl leading-relaxed">
              {description}
            </p>
          </div>

          <div className="shrink-0 self-start sm:self-center">
            <span className="inline-flex items-center gap-1.5 text-xs px-3 py-1 rounded-full border border-slate-300 dark:border-slate-700 bg-slate-50 dark:bg-slate-800/80 text-slate-700 dark:text-slate-300">
              <span className="h-2 w-2 rounded-full bg-blue-500 animate-pulse" />
              <span>Proveniência: {provenience}</span>
            </span>
          </div>
        </div>
      </div>

      {/* Grid de Diretrizes Arquiteturais do Módulo */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900/40 space-y-1.5">
          <div className="text-[11px] font-bold uppercase tracking-wider text-blue-600 dark:text-blue-400">
            Segurança & Isolamento
          </div>
          <div className="text-sm font-semibold text-slate-800 dark:text-slate-200">
            Multi-Tenant RLS & Token Cifrado
          </div>
          <p className="text-xs text-slate-500 dark:text-slate-400 leading-relaxed">
            Todas as transações operam sob o contexto estrito do tenant autenticado com PostgreSQL Row-Level Security.
          </p>
        </div>

        <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900/40 space-y-1.5">
          <div className="text-[11px] font-bold uppercase tracking-wider text-emerald-600 dark:text-emerald-400">
            Proteção de Contas
          </div>
          <div className="text-sm font-semibold text-slate-800 dark:text-slate-200">
            Zero Scraping Hostil & Fail-Closed
          </div>
          <p className="text-xs text-slate-500 dark:text-slate-400 leading-relaxed">
            Consumo exclusivo de APIs oficiais autorizadas; sem bypass de CAPTCHA e com circuit breaker em HTTP 429.
          </p>
        </div>

        <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900/40 space-y-1.5">
          <div className="text-[11px] font-bold uppercase tracking-wider text-purple-600 dark:text-purple-400">
            Integridade de Dados
          </div>
          <div className="text-sm font-semibold text-slate-800 dark:text-slate-200">
            Cálculo Determinístico em BigDecimal
          </div>
          <p className="text-xs text-slate-500 dark:text-slate-400 leading-relaxed">
            Proibição irrestrita de floats e cálculos por LLM. Valores monetários apurados com precisão em centavos.
          </p>
        </div>
      </div>
    </div>
  );
};
