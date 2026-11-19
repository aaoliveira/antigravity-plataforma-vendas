'use client';

import React, { useState, useEffect } from 'react';
import { CompanyAccountLinkDto, SellerAccountDto } from '@/types/company';

interface AccountLinkHistoryDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  sellerAccount: SellerAccountDto | null;
}

export function AccountLinkHistoryDrawer({
  isOpen,
  onClose,
  sellerAccount,
}: AccountLinkHistoryDrawerProps) {
  const [history, setHistory] = useState<CompanyAccountLinkDto[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!isOpen || !sellerAccount) {
      setHistory([]);
      setError(null);
      return;
    }

    async function loadHistory() {
      if (!sellerAccount) return;
      setIsLoading(true);
      setError(null);

      try {
        const res = await fetch(`/api/companies/accounts/${sellerAccount.id}/history`);
        if (!res.ok) {
          const data = await res.json().catch(() => ({}));
          throw new Error(data.message || 'Falha ao carregar histórico de vínculos.');
        }
        const data: CompanyAccountLinkDto[] = await res.json();
        setHistory(data);
      } catch (err: unknown) {
        setError(err instanceof Error ? err.message : 'Erro ao buscar dados históricos.');
      } finally {
        setIsLoading(false);
      }
    }

    loadHistory();
  }, [isOpen, sellerAccount]);

  // Tecla Escape para fechar
  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen || !sellerAccount) return null;

  const formatDate = (isoString: string | null | undefined) => {
    if (!isoString) return '—';
    try {
      const date = new Date(isoString);
      return date.toLocaleString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return isoString;
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="history-drawer-title"
      className="fixed inset-0 z-50 flex justify-end bg-slate-900/60 backdrop-blur-sm"
    >
      <div className="bg-white dark:bg-slate-900 border-l border-slate-200 dark:border-slate-800 shadow-2xl w-full max-w-md h-full flex flex-col overflow-hidden">
        {/* Header */}
        <div className="p-5 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between">
          <div>
            <h2 id="history-drawer-title" className="text-base font-bold text-slate-900 dark:text-white">
              Histórico de Vínculos & Vigência
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Conta: <strong className="text-slate-800 dark:text-slate-200">{sellerAccount.accountName}</strong> ({sellerAccount.marketplace})
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fechar histórico"
            className="p-2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors min-h-[44px] min-w-[44px] flex items-center justify-center"
          >
            ✕
          </button>
        </div>

        {/* Content Body */}
        <div className="flex-1 overflow-y-auto p-5 space-y-4">
          {isLoading && (
            <div className="py-12 flex flex-col items-center justify-center text-xs text-slate-500 dark:text-slate-400 gap-3" role="status">
              <svg className="animate-spin h-6 w-6 text-blue-600" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
              </svg>
              <span>Carregando linha do tempo de vigências...</span>
            </div>
          )}

          {error && (
            <div role="alert" className="p-4 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900 text-xs text-rose-700 dark:text-rose-300">
              {error}
            </div>
          )}

          {!isLoading && !error && history.length === 0 && (
            <div className="py-12 text-center text-xs text-slate-500 dark:text-slate-400" role="region">
              Nenhum histórico de vínculo registrado para esta conta de marketplace.
            </div>
          )}

          {!isLoading && !error && history.length > 0 && (
            <div className="space-y-4 relative before:absolute before:inset-0 before:left-3.5 before:w-0.5 before:bg-slate-200 dark:before:bg-slate-800">
              {history.map((item) => (
                <div key={item.id} className="relative flex items-start gap-4 pl-8">
                  {/* Marcador na linha do tempo */}
                  <div
                    className={`absolute left-2 top-1.5 w-3.5 h-3.5 rounded-full border-2 ${
                      item.isActive
                        ? 'bg-emerald-500 border-white dark:border-slate-900 shadow-sm ring-2 ring-emerald-200 dark:ring-emerald-900'
                        : 'bg-slate-400 border-white dark:border-slate-900'
                    }`}
                  />

                  {/* Card de versão */}
                  <div className="flex-1 p-3.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50/70 dark:bg-slate-800/50 space-y-2">
                    <div className="flex items-center justify-between gap-2">
                      <span className="font-bold text-xs text-slate-900 dark:text-white">
                        {item.companyName}
                      </span>
                      <div className="flex items-center gap-1.5">
                        <span className="text-[10px] font-semibold px-2 py-0.5 rounded-md bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-300">
                          v{item.version}
                        </span>
                        <span
                          className={`text-[10px] font-semibold px-2 py-0.5 rounded-md ${
                            item.isActive
                              ? 'bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300'
                              : 'bg-slate-200 dark:bg-slate-700 text-slate-600 dark:text-slate-400'
                          }`}
                        >
                          {item.isActive ? 'Vigente' : 'Encerrado'}
                        </span>
                      </div>
                    </div>

                    <div className="text-[11px] text-slate-600 dark:text-slate-400 space-y-1">
                      <div>
                        <strong>Início:</strong> {formatDate(item.effectiveFrom)}
                      </div>
                      <div>
                        <strong>Término:</strong>{' '}
                        {item.effectiveTo ? formatDate(item.effectiveTo) : '— (Ativo)'}
                      </div>
                      {item.linkedBy && (
                        <div>
                          <strong>Responsável:</strong> {item.linkedBy}
                        </div>
                      )}
                      {item.notes && (
                        <div className="text-slate-500 italic pt-1">
                          &quot;{item.notes}&quot;
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-slate-200 dark:border-slate-800 flex justify-end">
          <button
            type="button"
            onClick={onClose}
            className="px-4 py-2.5 rounded-xl text-xs font-semibold bg-slate-100 hover:bg-slate-200 dark:bg-slate-800 dark:hover:bg-slate-700 text-slate-800 dark:text-slate-200 transition-colors min-h-[44px]"
          >
            Fechar
          </button>
        </div>
      </div>
    </div>
  );
}
