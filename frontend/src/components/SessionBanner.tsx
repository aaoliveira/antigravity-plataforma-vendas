'use client';

import React from 'react';
import { UNIFORM_SESSION_EXPIRED_MESSAGE } from '@/lib/errorHandler';

interface SessionBannerProps {
  onLoginClick?: () => void;
}

export function SessionBanner({ onLoginClick }: SessionBannerProps) {
  const handleRedirect = () => {
    if (onLoginClick) {
      onLoginClick();
    } else {
      window.location.href = '/login?expired=true';
    }
  };

  return (
    <aside
      role="alert"
      aria-live="assertive"
      aria-label="Aviso de Sessão Expirada"
      className="w-full bg-amber-500/10 border-b border-amber-500/20 text-amber-900 dark:text-amber-200 px-4 py-3 sm:px-6 transition-all"
    >
      <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-lg bg-amber-500/20 text-amber-600 dark:text-amber-400 flex items-center justify-center shrink-0">
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
              />
            </svg>
          </div>
          <div className="flex flex-col">
            <span className="text-xs sm:text-sm font-semibold">
              Sessão Expirada ou Acesso Requerido
            </span>
            <p className="text-xs text-amber-800 dark:text-amber-300">
              {UNIFORM_SESSION_EXPIRED_MESSAGE}
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={handleRedirect}
          className="w-full sm:w-auto inline-flex items-center justify-center px-4 py-2 rounded-lg bg-amber-600 hover:bg-amber-700 text-white text-xs font-semibold shadow-sm transition-colors focus:outline-none focus:ring-2 focus:ring-amber-500 min-h-[44px]"
        >
          Entrar Novamente
        </button>
      </div>
    </aside>
  );
}
