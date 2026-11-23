'use client';

import React, { useEffect } from 'react';
import { sanitizeErrorMessage } from '@/lib/errorHandler';

interface ErrorProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function GlobalError({ error, reset }: ErrorProps) {
  useEffect(() => {
    // Log estruturado do erro mantido apenas no console do cliente para depuração
    console.error('Falha interceptada pelo mecanismo de contenção:', error);
  }, [error]);

  const safeMessage = sanitizeErrorMessage(error?.message);

  return (
    <div
      role="alert"
      className="min-h-screen flex items-center justify-center p-4 bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100"
    >
      <div className="max-w-md w-full bg-white dark:bg-slate-900 p-6 sm:p-8 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-lg text-center space-y-4">
        <div className="w-12 h-12 rounded-xl bg-rose-100 dark:bg-rose-900/40 text-rose-600 dark:text-rose-400 mx-auto flex items-center justify-center">
          <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
        </div>

        <h2 className="text-lg font-bold">Ocorreu um erro no processamento</h2>
        <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 leading-relaxed">
          {safeMessage}
        </p>

        <p className="text-[11px] text-slate-600 dark:text-slate-400">
          O sistema opera em modo Fail-Closed para garantir a integridade dos dados e o isolamento entre organizações.
        </p>

        <div className="pt-2 flex flex-col sm:flex-row items-center justify-center gap-3">
          <button
            onClick={() => reset()}
            type="button"
            className="w-full sm:w-auto inline-flex items-center justify-center px-4 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white text-xs sm:text-sm font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-indigo-500 min-h-[44px]"
          >
            Tentar Novamente
          </button>
          <a
            href="/"
            className="w-full sm:w-auto inline-flex items-center justify-center px-4 py-2 rounded-lg border border-slate-300 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300 text-xs sm:text-sm font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-indigo-500 min-h-[44px]"
          >
            Retornar ao Dashboard
          </a>
        </div>
      </div>
    </div>
  );
}
