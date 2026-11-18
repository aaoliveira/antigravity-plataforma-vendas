'use client';

import React from 'react';
import Link from 'next/link';
import { UNIFORM_ACCESS_OR_NOT_FOUND_MESSAGE } from '@/lib/errorHandler';

interface AccessDeniedFeedbackProps {
  onBackClick?: () => void;
  customMessage?: string;
}

export function AccessDeniedFeedback({ onBackClick, customMessage }: AccessDeniedFeedbackProps) {
  return (
    <div
      role="region"
      aria-labelledby="access-denied-heading"
      className="w-full flex items-center justify-center p-4 sm:p-6"
    >
      <div className="max-w-md w-full bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-6 sm:p-8 text-center shadow-lg space-y-4">
        <div className="w-14 h-14 rounded-2xl bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 mx-auto flex items-center justify-center">
          <svg className="w-7 h-7" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.75}
              d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z"
            />
          </svg>
        </div>

        <h2 id="access-denied-heading" className="text-base sm:text-lg font-bold text-slate-900 dark:text-slate-100">
          Recurso Não Encontrado ou Restrito
        </h2>

        <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 leading-relaxed">
          {customMessage || UNIFORM_ACCESS_OR_NOT_FOUND_MESSAGE}
        </p>

        <p className="text-[11px] text-slate-400 dark:text-slate-500 italic">
          Por motivos de segurança e isolamento de dados, o sistema não diferencia páginas inexistentes de permissões não concedidas.
        </p>

        <div className="pt-2 flex flex-col sm:flex-row items-center justify-center gap-3">
          {onBackClick ? (
            <button
              type="button"
              onClick={onBackClick}
              className="w-full sm:w-auto px-5 py-2.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white text-xs sm:text-sm font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-indigo-500 min-h-[44px]"
            >
              Voltar ao Início
            </button>
          ) : (
            <Link
              href="/"
              className="w-full sm:w-auto inline-flex items-center justify-center px-5 py-2.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white text-xs sm:text-sm font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-indigo-500 min-h-[44px]"
            >
              Voltar ao Dashboard
            </Link>
          )}
        </div>
      </div>
    </div>
  );
}
