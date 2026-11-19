import React, { useEffect, useRef } from 'react';
import { StructuredAiResponseDto } from '@/types/aiassistant';

interface AiSourcesDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  response: StructuredAiResponseDto | null;
}

export const AiSourcesDrawer: React.FC<AiSourcesDrawerProps> = ({
  isOpen,
  onClose,
  response,
}) => {
  const closeButtonRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    if (isOpen) {
      setTimeout(() => closeButtonRef.current?.focus(), 50);
    }
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen || !response) return null;

  return (
    <div
      className="fixed inset-0 z-50 overflow-hidden bg-slate-900/60 backdrop-blur-sm transition-opacity"
      role="dialog"
      aria-modal="true"
      aria-label="Inspeção de Fontes e Dados Oficiais"
      data-testid="ai-sources-drawer"
    >
      <div className="fixed inset-y-0 right-0 flex max-w-full pl-10">
        <div className="w-screen max-w-lg bg-white dark:bg-slate-900 shadow-2xl flex flex-col border-l border-slate-200 dark:border-slate-800">
          {/* Header */}
          <div className="px-6 py-5 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between bg-slate-50 dark:bg-slate-800/50">
            <div>
              <div className="flex items-center gap-2">
                <span className="p-1.5 rounded-md bg-indigo-100 dark:bg-indigo-900/40 text-indigo-600 dark:text-indigo-400">
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                </span>
                <h3 className="text-base font-semibold text-slate-900 dark:text-white">
                  Fontes e Dados Oficiais
                </h3>
              </div>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                Execução {response.executionId.substring(0, 8)} • Provedor {response.provider} ({response.model})
              </p>
            </div>
            <button
              ref={closeButtonRef}
              type="button"
              onClick={onClose}
              className="p-2 rounded-lg text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 min-h-[44px] min-w-[44px] flex items-center justify-center"
              aria-label="Fechar gaveta de fontes"
            >
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          {/* Body */}
          <div className="flex-1 overflow-y-auto p-6 space-y-6 text-sm">
            {/* Aviso de MVP Consultivo */}
            <div className="p-3.5 rounded-xl bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800/60 text-amber-800 dark:text-amber-300">
              <div className="flex items-start gap-2.5">
                <svg className="w-5 h-5 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <div>
                  <h4 className="font-semibold text-xs tracking-wide uppercase">Operação em Modo Somente Leitura (MVP)</h4>
                  <p className="text-xs mt-1 leading-relaxed">
                    Este assistente é estritamente consultivo e determinístico. Nenhuma alteração foi realizada nos marketplaces ou anúncios conectados. Botões de mutação em APIs externas não são permitidos nesta versão.
                  </p>
                </div>
              </div>
            </div>

            {/* Fontes consultadas */}
            <div>
              <h4 className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">
                Fontes Internas Consultadas
              </h4>
              {response.sources && response.sources.length > 0 ? (
                <ul className="space-y-1.5" data-testid="sources-list">
                  {response.sources.map((src, idx) => (
                    <li
                      key={idx}
                      className="flex items-center gap-2 p-2 rounded-lg bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700/60 text-xs font-mono text-slate-800 dark:text-slate-200"
                    >
                      <svg className="w-4 h-4 text-emerald-500 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                      </svg>
                      <span>{src}</span>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="text-xs text-slate-500 italic">Nenhuma fonte externa consultada para esta inferência.</p>
              )}
            </div>

            {/* Tools Executadas */}
            <div>
              <h4 className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">
                Ferramentas Somente Leitura Invocadas
              </h4>
              <div className="flex flex-wrap gap-2">
                {response.toolsExecuted && response.toolsExecuted.length > 0 ? (
                  response.toolsExecuted.map((tool, idx) => (
                    <span
                      key={idx}
                      className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-mono font-medium bg-blue-50 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800"
                    >
                      <svg className="w-3.5 h-3.5 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                      </svg>
                      {tool}
                    </span>
                  ))
                ) : (
                  <span className="text-xs text-slate-500 italic">Nenhuma tool requerida pelo template.</span>
                )}
              </div>
            </div>

            {/* Timestamps */}
            <div>
              <h4 className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">
                Data dos Dados & Registro Temporal
              </h4>
              <div className="p-3 rounded-lg bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700/60 space-y-1.5 text-xs">
                {Object.entries(response.timestamps || {}).map(([key, val]) => (
                  <div key={key} className="flex justify-between items-center text-slate-600 dark:text-slate-400 font-mono">
                    <span className="font-sans font-medium text-slate-700 dark:text-slate-300 capitalize">{key}:</span>
                    <span>{new Date(val).toLocaleString('pt-BR')}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Avisos de Indisponibilidade */}
            {response.unavailabilityNotes && response.unavailabilityNotes.length > 0 && (
              <div>
                <h4 className="text-xs font-bold text-rose-600 dark:text-rose-400 uppercase tracking-wider mb-2">
                  Dados Não Disponíveis na Consulta
                </h4>
                <ul className="space-y-1 text-xs text-rose-700 dark:text-rose-300 bg-rose-50 dark:bg-rose-950/30 p-3 rounded-lg border border-rose-200 dark:border-rose-900/60">
                  {response.unavailabilityNotes.map((note, idx) => (
                    <li key={idx} className="flex items-start gap-1.5">
                      <span>•</span>
                      <span>{note}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {/* Dados estruturados brutos */}
            {response.structuredData && Object.keys(response.structuredData).length > 0 && (
              <div>
                <h4 className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider mb-2">
                  Payload Estruturado das Tools (JSON)
                </h4>
                <pre className="p-3 bg-slate-900 text-slate-200 rounded-lg text-xs font-mono overflow-x-auto max-h-56">
                  {JSON.stringify(response.structuredData, null, 2)}
                </pre>
              </div>
            )}
          </div>

          {/* Footer */}
          <div className="p-4 border-t border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-800/50 flex justify-end">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 rounded-lg bg-slate-200 dark:bg-slate-700 hover:bg-slate-300 dark:hover:bg-slate-600 text-slate-800 dark:text-slate-100 font-medium text-xs transition-colors min-h-[44px]"
            >
              Fechar Inspeção
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
