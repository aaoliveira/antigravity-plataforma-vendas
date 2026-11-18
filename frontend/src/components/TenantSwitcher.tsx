'use client';

import React, { useState, useRef, useEffect } from 'react';
import { useAuth } from '@/context/AuthContext';

export function TenantSwitcher() {
  const { user, currentTenant, switchTenant } = useAuth();
  const [isOpen, setIsOpen] = useState(false);
  const [switching, setSwitching] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Fecha o dropdown ao pressionar Escape ou clicar fora
  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape' && isOpen) {
        setIsOpen(false);
      }
    }

    function handleClickOutside(e: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    }

    if (isOpen) {
      document.addEventListener('keydown', handleKeyDown);
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [isOpen]);

  if (!user || !currentTenant) {
    return (
      <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-100 dark:bg-slate-800 text-xs text-slate-500 animate-pulse">
        Carregando organização...
      </div>
    );
  }

  const memberships = user.memberships || [];

  const handleSelectTenant = async (tenantId: string) => {
    if (tenantId === currentTenant.tenantId) {
      setIsOpen(false);
      return;
    }

    try {
      setSwitching(tenantId);
      setErrorMessage(null);
      await switchTenant(tenantId);
      setIsOpen(false);
    } catch (err: unknown) {
      setErrorMessage(err instanceof Error ? err.message : 'Falha ao trocar organização');
    } finally {
      setSwitching(null);
    }
  };

  return (
    <div className="relative inline-block text-left" ref={dropdownRef}>
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        aria-haspopup="listbox"
        aria-expanded={isOpen}
        aria-label={`Organização ativa: ${currentTenant.name}. Pressione para alternar organização.`}
        className="flex items-center gap-2 px-3 py-2 text-left rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 hover:bg-slate-50 dark:hover:bg-slate-750 transition-colors focus:outline-none focus:ring-2 focus:ring-indigo-500 min-h-[44px]"
      >
        <div className="w-6 h-6 rounded-md bg-indigo-100 dark:bg-indigo-950/80 text-indigo-700 dark:text-indigo-400 font-bold flex items-center justify-center text-xs shrink-0">
          {currentTenant.name.charAt(0).toUpperCase()}
        </div>
        <div className="flex flex-col min-w-0 pr-1">
          <span className="text-xs font-semibold text-slate-900 dark:text-slate-100 truncate max-w-[140px] sm:max-w-[200px]">
            {currentTenant.name}
          </span>
          <span className="text-[10px] text-slate-500 dark:text-slate-400 font-mono">
            {currentTenant.role}
          </span>
        </div>
        <svg
          className={`w-4 h-4 text-slate-400 transition-transform ${isOpen ? 'rotate-180' : ''}`}
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {isOpen && (
        <div
          role="listbox"
          aria-label="Lista de organizações disponíveis"
          className="absolute left-0 mt-2 w-72 rounded-xl bg-white dark:bg-slate-900 shadow-xl border border-slate-200 dark:border-slate-800 z-50 p-2 divide-y divide-slate-100 dark:divide-slate-800 focus:outline-none"
        >
          <div className="px-3 py-2">
            <p className="text-[11px] font-bold tracking-wider uppercase text-slate-400 dark:text-slate-500">
              Suas Organizações ({memberships.length})
            </p>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Apenas organizações onde você possui membership ativa.
            </p>
          </div>

          {errorMessage && (
            <div role="alert" className="p-2 text-xs text-rose-600 dark:text-rose-400 bg-rose-50 dark:bg-rose-950/50 rounded-md">
              {errorMessage}
            </div>
          )}

          <div className="py-1 space-y-1 max-h-60 overflow-y-auto">
            {memberships.map((m) => {
              const isSelected = m.tenantId === currentTenant.tenantId;
              const isProcessing = switching === m.tenantId;

              return (
                <button
                  key={m.tenantId}
                  role="option"
                  aria-selected={isSelected}
                  onClick={() => handleSelectTenant(m.tenantId)}
                  disabled={isProcessing}
                  className={`w-full flex items-center justify-between gap-3 px-3 py-2.5 rounded-lg text-left text-xs transition-colors min-h-[48px] ${
                    isSelected
                      ? 'bg-indigo-50 dark:bg-indigo-950/60 text-indigo-900 dark:text-indigo-200 font-medium'
                      : 'hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300'
                  }`}
                >
                  <div className="flex items-center gap-2.5 min-w-0">
                    <div
                      className={`w-7 h-7 rounded-md flex items-center justify-center font-bold text-xs shrink-0 ${
                        isSelected
                          ? 'bg-indigo-600 text-white'
                          : 'bg-slate-200 dark:bg-slate-800 text-slate-700 dark:text-slate-300'
                      }`}
                    >
                      {m.tenantName.charAt(0).toUpperCase()}
                    </div>
                    <div className="flex flex-col min-w-0">
                      <span className="font-semibold truncate">{m.tenantName}</span>
                      <span className="text-[10px] text-slate-400 dark:text-slate-500 font-mono">
                        Papel: {m.role}
                      </span>
                    </div>
                  </div>

                  {isSelected && (
                    <span className="text-indigo-600 dark:text-indigo-400 shrink-0" aria-label="Ativa">
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
                      </svg>
                    </span>
                  )}

                  {isProcessing && (
                    <span className="text-indigo-500 text-[10px] animate-pulse">Trocando...</span>
                  )}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
