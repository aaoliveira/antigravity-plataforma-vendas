'use client';

import React, { useEffect } from 'react';
import { KpiDefinition } from '@/types/kpis';

interface KpiDefinitionsDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  definitions: KpiDefinition[];
  selectedCode?: string | null;
}

export const KpiDefinitionsDrawer: React.FC<KpiDefinitionsDrawerProps> = ({
  isOpen,
  onClose,
  definitions,
  selectedCode,
}) => {
  // Fechamento via Escape (WCAG 2.1 AA)
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

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Catálogo de Fórmulas e Definições de KPIs"
      className="fixed inset-0 z-50 flex justify-end"
    >
      {/* Backdrop */}
      <div
        onClick={onClose}
        className="fixed inset-0 bg-slate-950/60 backdrop-blur-sm transition-opacity"
      />

      {/* Drawer */}
      <div className="relative w-full max-w-lg bg-white dark:bg-slate-900 h-full p-6 shadow-2xl flex flex-col justify-between z-10 overflow-y-auto">
        <div className="space-y-5">
          <div className="flex items-center justify-between pb-4 border-b border-slate-200 dark:border-slate-800">
            <div>
              <h2 className="text-base font-bold text-slate-900 dark:text-white">
                Catálogo Oficial de KPIs
              </h2>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                18 definições formais, fórmulas matemáticas e proveniência de dados
              </p>
            </div>
            <button
              type="button"
              onClick={onClose}
              aria-label="Fechar gaveta de definições"
              className="p-2 text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 min-h-[44px] min-w-[44px] flex items-center justify-center transition-colors"
            >
              ✕
            </button>
          </div>

          {/* Salvaguarda Anti-Alucinação */}
          <div className="p-3.5 rounded-xl border border-rose-200 dark:border-rose-900/60 bg-rose-50/70 dark:bg-rose-950/20 text-xs space-y-1">
            <span className="font-bold text-rose-800 dark:text-rose-300 block">
              Aviso Contábil & Anti-Alucinação:
            </span>
            <p className="text-rose-700 dark:text-rose-300/90 text-[11px] leading-relaxed">
              Este sistema apura deterministamente a <strong>Margem de Contribuição</strong>. Despesas fixas estruturais, operacionais e de folha não estão contempladas, sendo terminantemente proibido rotular este resultado como Lucro Líquido.
            </p>
          </div>

          {/* Lista de Definições */}
          <div className="space-y-3">
            {definitions.map((def) => {
              const isSelected = selectedCode && selectedCode.toUpperCase() === def.code.toUpperCase();
              return (
                <article
                  key={def.code}
                  id={`kpi-def-${def.code.toLowerCase()}`}
                  className={`p-3.5 rounded-xl border transition-all ${
                    isSelected
                      ? 'border-blue-500 bg-blue-50/50 dark:bg-blue-950/30 ring-1 ring-blue-500'
                      : 'border-slate-200 dark:border-slate-800 bg-slate-50/60 dark:bg-slate-800/40'
                  }`}
                >
                  <div className="flex items-center justify-between mb-1">
                    <h3 className="font-semibold text-xs text-slate-900 dark:text-white">
                      {def.name}
                    </h3>
                    <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-300">
                      {def.code}
                    </span>
                  </div>

                  <p className="text-xs text-slate-600 dark:text-slate-300 mb-2">
                    {def.description}
                  </p>

                  <div className="p-2 rounded bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 font-mono text-[11px] text-blue-700 dark:text-blue-300 mb-2 break-all">
                    fórmula: {def.formula}
                  </div>

                  <div className="flex items-center justify-between text-[10px] text-slate-500 dark:text-slate-400">
                    <span>Unidade: <strong>{def.unit}</strong></span>
                    <span>Fonte: <strong>{def.provenance}</strong></span>
                  </div>
                </article>
              );
            })}
          </div>
        </div>

        <div className="pt-4 border-t border-slate-200 dark:border-slate-800">
          <button
            type="button"
            onClick={onClose}
            className="w-full py-2.5 rounded-lg bg-slate-900 dark:bg-white text-white dark:text-slate-900 text-xs font-semibold hover:opacity-90 transition-opacity min-h-[44px]"
          >
            Entendido, fechar catálogo
          </button>
        </div>
      </div>
    </div>
  );
};
