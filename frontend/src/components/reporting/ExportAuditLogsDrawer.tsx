'use client';

import React, { useEffect, useState } from 'react';
import { ExportAuditLogDto, ExportAuditAction } from '@/types/reports';

interface ExportAuditLogsDrawerProps {
  isOpen: boolean;
  onClose: () => void;
}

export const ExportAuditLogsDrawer: React.FC<ExportAuditLogsDrawerProps> = ({
  isOpen,
  onClose,
}) => {
  const [logs, setLogs] = useState<ExportAuditLogDto[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Esc key fecha a gaveta
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  // Carrega logs de auditoria
  useEffect(() => {
    if (!isOpen) return;

    let isMounted = true;
    async function fetchAuditLogs() {
      setIsLoading(true);
      setError(null);
      try {
        const res = await fetch('/api/reports/exports/audit?size=50');
        if (!res.ok) {
          const err = await res.json().catch(() => ({}));
          throw new Error(err.message || 'Falha ao carregar registros de auditoria.');
        }
        const data = await res.json();
        if (isMounted) {
          setLogs(data.content || data || []);
        }
      } catch (err: unknown) {
        if (isMounted) {
          setError(err instanceof Error ? err.message : 'Erro ao consultar auditoria.');
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    fetchAuditLogs();
    return () => {
      isMounted = false;
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const formatDate = (isoString?: string | null) => {
    if (!isoString) return '—';
    try {
      return new Intl.DateTimeFormat('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      }).format(new Date(isoString));
    } catch {
      return isoString;
    }
  };

  const getActionBadge = (action: ExportAuditAction) => {
    switch (action) {
      case 'EXPORT_REQUESTED':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-blue-100 text-blue-800 dark:bg-blue-950/60 dark:text-blue-300">
            Solicitação
          </span>
        );
      case 'EXPORT_COMPLETED':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300">
            Concluído
          </span>
        );
      case 'EXPORT_DOWNLOADED':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-teal-100 text-teal-800 dark:bg-teal-950/60 dark:text-teal-300">
            Download Efetuado
          </span>
        );
      case 'EXPORT_FAILED':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-rose-100 text-rose-800 dark:bg-rose-950/60 dark:text-rose-300">
            Falha
          </span>
        );
      case 'EXPORT_EXPIRED_ACCESS_ATTEMPT':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300">
            Acesso Expirado
          </span>
        );
      case 'EXPORT_CROSS_TENANT_ACCESS_BLOCKED':
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-rose-200 text-rose-900 dark:bg-rose-900/60 dark:text-rose-200">
            Bloqueio Cross-Tenant
          </span>
        );
      default:
        return (
          <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-slate-100 text-slate-800 dark:bg-slate-800 dark:text-slate-300">
            {action}
          </span>
        );
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-label="Trilha de Auditoria de Exportações"
      className="fixed inset-0 z-50 flex justify-end"
    >
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm transition-opacity"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Painel Lateral */}
      <div className="relative w-full max-w-lg bg-white dark:bg-slate-900 h-full shadow-2xl border-l border-slate-200 dark:border-slate-800 p-5 flex flex-col justify-between z-10 select-none overflow-hidden">
        <div className="flex flex-col h-full">
          {/* Header da Gaveta */}
          <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800 flex-shrink-0">
            <div>
              <h2 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <svg className="w-5 h-5 text-indigo-600 dark:text-indigo-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
                </svg>
                Trilha de Auditoria
              </h2>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                Registro de segurança e downloads auditados do tenant.
              </p>
            </div>

            <button
              type="button"
              onClick={onClose}
              aria-label="Fechar gaveta de auditoria"
              className="p-2 text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            >
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>

          {/* Conteúdo da Lista de Auditoria */}
          <div className="flex-1 overflow-y-auto py-4 space-y-3">
            {isLoading ? (
              <div className="py-12 flex flex-col items-center justify-center gap-2 text-slate-400">
                <svg className="w-6 h-6 animate-spin text-blue-600" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                </svg>
                <span className="text-xs">Carregando eventos de auditoria...</span>
              </div>
            ) : error ? (
              <div
                role="alert"
                className="p-3 bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 rounded-xl text-xs text-rose-700 dark:text-rose-300"
              >
                {error}
              </div>
            ) : logs.length === 0 ? (
              <div className="py-12 text-center text-xs text-slate-500 dark:text-slate-400">
                Nenhum evento de exportação auditado até o momento.
              </div>
            ) : (
              logs.map((log) => (
                <div
                  key={log.id}
                  className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/40 space-y-2 text-xs"
                >
                  <div className="flex items-center justify-between">
                    {getActionBadge(log.action)}
                    <span className="text-[11px] text-slate-400 font-mono">
                      {formatDate(log.createdAt)}
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-2 text-[11px] text-slate-600 dark:text-slate-300">
                    <div>
                      <span className="text-slate-400 block text-[10px]">Operador:</span>
                      <span className="font-medium truncate block">{log.userId}</span>
                    </div>

                    <div>
                      <span className="text-slate-400 block text-[10px]">Origem IP:</span>
                      <span className="font-mono truncate block">{log.ipAddress || '—'}</span>
                    </div>
                  </div>

                  {log.exportJobId && (
                    <div className="text-[10px] text-slate-400 font-mono truncate">
                      Job ID: {log.exportJobId}
                    </div>
                  )}
                </div>
              ))
            )}
          </div>

          {/* Rodapé da Gaveta */}
          <div className="pt-3 border-t border-slate-100 dark:border-slate-800 text-[11px] text-slate-400 flex items-center justify-between flex-shrink-0">
            <span>Retenção de logs: 90 dias</span>
            <button
              type="button"
              onClick={onClose}
              className="px-3 py-1.5 rounded-lg border border-slate-200 dark:border-slate-700 text-xs text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800"
            >
              Fechar
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
