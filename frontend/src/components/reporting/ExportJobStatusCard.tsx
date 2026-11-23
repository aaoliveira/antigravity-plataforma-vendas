'use client';

import React, { useState } from 'react';
import { ExportJobResponseDto, DATASET_CATALOG } from '@/types/reports';

interface ExportJobStatusCardProps {
  job: ExportJobResponseDto;
  onDownload: (job: ExportJobResponseDto) => Promise<void>;
  onRefreshJob?: (jobId: string) => Promise<void>;
  isDownloading?: boolean;
}

export const ExportJobStatusCard: React.FC<ExportJobStatusCardProps> = ({
  job,
  onDownload,
  onRefreshJob,
  isDownloading = false,
}) => {
  const [copiedChecksum, setCopiedChecksum] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const datasetInfo = DATASET_CATALOG[job.dataset] || {
    name: job.dataset,
    columns: [],
  };

  const formatDate = (isoString?: string | null) => {
    if (!isoString) return '—';
    try {
      return new Intl.DateTimeFormat('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      }).format(new Date(isoString));
    } catch {
      return isoString;
    }
  };

  const formatFileSize = (bytes?: number | null) => {
    if (!bytes || bytes <= 0) return '—';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  };

  const isExpired = () => {
    if (job.status === 'EXPIRED') return true;
    if (job.expiresAt && new Date(job.expiresAt).getTime() < Date.now()) return true;
    return false;
  };

  const getExpirationRemaining = () => {
    if (!job.expiresAt) return null;
    const diffMs = new Date(job.expiresAt).getTime() - Date.now();
    if (diffMs <= 0) return 'Expirado';
    const hours = Math.floor(diffMs / (1000 * 60 * 60));
    const minutes = Math.floor((diffMs % (1000 * 60 * 60)) / (1000 * 60));
    if (hours > 0) return `${hours}h ${minutes}m restantes`;
    return `${minutes}m restantes`;
  };

  const handleCopyChecksum = () => {
    if (job.checksumSha256) {
      navigator.clipboard.writeText(job.checksumSha256).catch(() => {});
      setCopiedChecksum(true);
      setTimeout(() => setCopiedChecksum(false), 2000);
    }
  };

  const handleRefreshClick = async () => {
    if (!onRefreshJob) return;
    setIsRefreshing(true);
    try {
      await onRefreshJob(job.id);
    } finally {
      setIsRefreshing(false);
    }
  };

  const expired = isExpired();
  const effectiveStatus = expired && job.status === 'COMPLETED' ? 'EXPIRED' : job.status;

  return (
    <article
      data-testid={`export-job-card-${job.id}`}
      className="p-4 sm:p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm transition-all hover:shadow-md space-y-4"
    >
      {/* Top Header do Card: Dataset, Formato e Status Badge */}
      <div className="flex flex-wrap items-center justify-between gap-2.5">
        <div className="flex items-center gap-2">
          <span className="font-bold text-sm text-slate-900 dark:text-white">
            {datasetInfo.name}
          </span>
          <span
            className={`px-2 py-0.5 rounded text-[10px] font-mono font-bold tracking-wider ${
              job.fileFormat === 'XLSX'
                ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300'
                : 'bg-indigo-100 text-indigo-800 dark:bg-indigo-950/60 dark:text-indigo-300'
            }`}
          >
            {job.fileFormat}
          </span>
        </div>

        {/* Badge de Estado do Job */}
        <div>
          {effectiveStatus === 'PENDING' && (
            <span
              role="status"
              className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 dark:bg-amber-950/50 dark:text-amber-300"
            >
              <span className="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse" />
              Na Fila
            </span>
          )}

          {effectiveStatus === 'PROCESSING' && (
            <span
              role="status"
              className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-100 text-blue-800 dark:bg-blue-950/50 dark:text-blue-300"
            >
              <svg className="w-3.5 h-3.5 animate-spin text-blue-600 dark:text-blue-400" fill="none" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
              </svg>
              Processando...
            </span>
          )}

          {effectiveStatus === 'COMPLETED' && (
            <span
              role="status"
              className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 dark:bg-emerald-950/50 dark:text-emerald-300"
            >
              <svg className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
              </svg>
              Concluído
            </span>
          )}

          {effectiveStatus === 'FAILED' && (
            <span
              role="status"
              className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-rose-100 text-rose-800 dark:bg-rose-950/50 dark:text-rose-300"
            >
              <svg className="w-3.5 h-3.5 text-rose-600 dark:text-rose-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
              Falhou
            </span>
          )}

          {effectiveStatus === 'EXPIRED' && (
            <span
              role="status"
              className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300"
            >
              <svg className="w-3.5 h-3.5 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              Expirado
            </span>
          )}
        </div>
      </div>

      {/* Grid de Informações Detalhadas */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-1 text-xs">
        <div>
          <span className="block text-[11px] text-slate-600 dark:text-slate-400">Registros</span>
          <div className="flex items-center gap-1.5 mt-0.5">
            <span className="font-semibold text-slate-800 dark:text-slate-200">
              {job.status === 'COMPLETED' ? job.totalRows.toLocaleString('pt-BR') : '—'}
            </span>
            {job.isTruncated && (
              <span
                title="A quantidade de registros excedeu o teto solicitado e o arquivo foi limitado com segurança."
                className="px-1.5 py-0.2 rounded bg-amber-100 text-amber-800 dark:bg-amber-950/50 dark:text-amber-300 text-[10px] font-medium"
              >
                Truncado
              </span>
            )}
          </div>
        </div>

        <div>
          <span className="block text-[11px] text-slate-600 dark:text-slate-400">Tamanho</span>
          <span className="font-semibold text-slate-800 dark:text-slate-200 mt-0.5 block font-mono">
            {formatFileSize(job.fileSizeBytes)}
          </span>
        </div>

        <div>
          <span className="block text-[11px] text-slate-600 dark:text-slate-400">Criado em</span>
          <span className="text-slate-700 dark:text-slate-300 mt-0.5 block">
            {formatDate(job.createdAt)}
          </span>
        </div>

        <div>
          <span className="block text-[11px] text-slate-600 dark:text-slate-400">Validade</span>
          <span className="text-slate-700 dark:text-slate-300 mt-0.5 block">
            {job.status === 'COMPLETED' ? (
              expired ? (
                <span className="text-rose-600 dark:text-rose-400 font-medium">Expirado</span>
              ) : (
                <span className="text-emerald-700 dark:text-emerald-400 font-medium">
                  {getExpirationRemaining()}
                </span>
              )
            ) : (
              '—'
            )}
          </span>
        </div>
      </div>

      {/* Nome do Arquivo & Checksum SHA-256 (Se disponível) */}
      {job.fileName && (
        <div className="p-2.5 rounded-xl bg-slate-50 dark:bg-slate-800/40 border border-slate-100 dark:border-slate-800 text-[11px] flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div className="flex items-center gap-2 truncate">
            <svg className="w-4 h-4 text-slate-400 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <span className="font-mono text-slate-700 dark:text-slate-300 truncate" title={job.fileName}>
              {job.fileName}
            </span>
          </div>

          {job.checksumSha256 && (
            <div className="flex items-center gap-1.5 flex-shrink-0">
              <span className="text-slate-600 dark:text-slate-400">SHA-256:</span>
              <span className="font-mono text-slate-600 dark:text-slate-400 text-[10px]">
                {job.checksumSha256.substring(0, 10)}...
              </span>
              <button
                type="button"
                onClick={handleCopyChecksum}
                title="Copiar Hash SHA-256 completo para conferência de integridade"
                className="px-1.5 py-0.5 rounded text-[10px] bg-slate-200 dark:bg-slate-700 hover:bg-slate-300 dark:hover:bg-slate-600 text-slate-700 dark:text-slate-200 transition-colors"
              >
                {copiedChecksum ? 'Copiado!' : 'Copiar'}
              </button>
            </div>
          )}
        </div>
      )}

      {/* Detalhes de Erro (Se FAILED) */}
      {effectiveStatus === 'FAILED' && job.errorMessage && (
        <div className="p-3 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 text-xs text-rose-700 dark:text-rose-300">
          <span className="font-bold">Motivo da falha: </span>
          {job.errorMessage}
        </div>
      )}

      {/* Ações Autorizadas */}
      <div className="pt-2 border-t border-slate-100 dark:border-slate-800 flex flex-wrap items-center justify-between gap-3">
        <span className="text-[11px] text-slate-600 dark:text-slate-400">
          Solicitado por <span className="font-medium text-slate-600 dark:text-slate-300">{job.requestedBy}</span>
        </span>

        <div className="flex items-center gap-2">
          {/* Botão de Refresh manual para jobs em andamento */}
          {(effectiveStatus === 'PENDING' || effectiveStatus === 'PROCESSING') && (
            <button
              type="button"
              onClick={handleRefreshClick}
              disabled={isRefreshing}
              className="px-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700 text-xs text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800 transition-colors flex items-center gap-1.5 min-h-[38px]"
            >
              <svg className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              <span>{isRefreshing ? 'Consultando...' : 'Atualizar'}</span>
            </button>
          )}

          {/* Botão de Download Autorizado: NUNCA renderizar tag <a> com link público! */}
          {effectiveStatus === 'COMPLETED' && !expired && (
            <button
              type="button"
              onClick={() => onDownload(job)}
              disabled={isDownloading}
              aria-label={`Baixar arquivo seguro ${job.fileName || job.dataset}`}
              className="px-4 py-2 rounded-xl text-xs font-semibold bg-emerald-700 hover:bg-emerald-800 text-white transition-all shadow-sm flex items-center gap-2 min-h-[40px] disabled:opacity-50"
            >
              {isDownloading ? (
                <>
                  <svg className="w-4 h-4 animate-spin text-white" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                  </svg>
                  <span>Baixando...</span>
                </>
              ) : (
                <>
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4" />
                  </svg>
                  <span>Download Seguro</span>
                </>
              )}
            </button>
          )}

          {effectiveStatus === 'EXPIRED' && (
            <span className="text-[11px] text-slate-600 dark:text-slate-400 italic">
              Arquivo removido por expiração de segurança.
            </span>
          )}
        </div>
      </div>
    </article>
  );
};
