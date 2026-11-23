'use client';

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  ExportJobResponseDto,
  ExportRequestDto,
  ExportDataset,
  ExportJobStatus,
  DATASET_CATALOG,
} from '@/types/reports';
import { ExportRequestModal } from './ExportRequestModal';
import { ExportJobStatusCard } from './ExportJobStatusCard';
import { ExportAuditLogsDrawer } from './ExportAuditLogsDrawer';

export const ReportsExportView: React.FC = () => {
  const [jobs, setJobs] = useState<ExportJobResponseDto[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [downloadingJobId, setDownloadingJobId] = useState<string | null>(null);

  // Filtros de visualização local
  const [datasetFilter, setDatasetFilter] = useState<'ALL' | ExportDataset>('ALL');
  const [statusFilter, setStatusFilter] = useState<'ALL' | ExportJobStatus>('ALL');

  // Modais e Gavetas
  const [isRequestModalOpen, setIsRequestModalOpen] = useState<boolean>(false);
  const [isAuditDrawerOpen, setIsAuditDrawerOpen] = useState<boolean>(false);

  // Notificações e Feedback
  const [notification, setNotification] = useState<{
    type: 'success' | 'error' | 'info';
    message: string;
  } | null>(null);

  // Notificação com auto-dismiss em 6 segundos
  useEffect(() => {
    if (!notification) return;
    const timer = setTimeout(() => setNotification(null), 6000);
    return () => clearTimeout(timer);
  }, [notification]);

  // Carrega histórico de jobs do tenant
  const loadJobs = useCallback(async (silent = false) => {
    if (!silent) setIsLoading(true);
    try {
      const res = await fetch('/api/reports/exports?page=0&size=50');
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Falha ao buscar relatórios.');
      }
      const data = await res.json();
      setJobs(data.content || data || []);
    } catch (err: unknown) {
      if (!silent) {
        setNotification({
          type: 'error',
          message: err instanceof Error ? err.message : 'Erro ao carregar histórico de exportações.',
        });
      }
    } finally {
      if (!silent) setIsLoading(false);
    }
  }, []);

  // Carga inicial
  useEffect(() => {
    loadJobs();
  }, [loadJobs]);

  // Polling automático enquanto houver jobs em PENDING ou PROCESSING
  useEffect(() => {
    const hasActiveJobs = jobs.some(
      (j) => j.status === 'PENDING' || j.status === 'PROCESSING'
    );

    if (!hasActiveJobs) return;

    const interval = setInterval(() => {
      loadJobs(true);
    }, 4000);

    return () => clearInterval(interval);
  }, [jobs, loadJobs]);

  // Criação de nova exportação
  const handleCreateExport = async (payload: ExportRequestDto): Promise<boolean> => {
    setIsSubmitting(true);
    try {
      const res = await fetch('/api/reports/exports', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Falha ao solicitar geração da exportação.');
      }

      const newJob: ExportJobResponseDto = await res.json();
      setJobs((prev) => [newJob, ...prev.filter((j) => j.id !== newJob.id)]);
      setNotification({
        type: 'success',
        message: `Exportação assíncrona "${DATASET_CATALOG[payload.dataset]?.name}" iniciada com sucesso. Acompanhe o progresso nesta tela.`,
      });
      return true;
    } catch (err: unknown) {
      setNotification({
        type: 'error',
        message: err instanceof Error ? err.message : 'Erro ao solicitar exportação.',
      });
      return false;
    } finally {
      setIsSubmitting(false);
    }
  };

  // Atualização individual de status de um job
  const handleRefreshJob = async (jobId: string) => {
    try {
      const res = await fetch(`/api/reports/exports/${jobId}`);
      if (res.ok) {
        const updated: ExportJobResponseDto = await res.json();
        setJobs((prev) => prev.map((j) => (j.id === updated.id ? updated : j)));
      }
    } catch {
      // Silencioso
    }
  };

  // Download Autorizado Seguro (Regra: NUNCA mostrar link permanente público!)
  const handleAuthorizedDownload = async (job: ExportJobResponseDto) => {
    if (!job.downloadToken) {
      setNotification({
        type: 'error',
        message: 'Token de download não disponível para este arquivo.',
      });
      return;
    }

    setDownloadingJobId(job.id);
    try {
      const res = await fetch(
        `/api/reports/exports/${job.id}/download?token=${encodeURIComponent(job.downloadToken)}`
      );

      if (res.status === 410) {
        // Arquivo expirado
        setJobs((prev) =>
          prev.map((j) => (j.id === job.id ? { ...j, status: 'EXPIRED' } : j))
        );
        setNotification({
          type: 'error',
          message: 'Este arquivo expirou e foi removido do storage seguro. Solicite uma nova exportação.',
        });
        return;
      }

      if (res.status === 403) {
        setNotification({
          type: 'error',
          message: 'Acesso negado: token inválido ou sessão não autorizada para este tenant.',
        });
        return;
      }

      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || 'Falha ao autorizar download.');
      }

      // Converte a resposta em Blob e dispara o download efêmero em memória
      const blob = await res.blob();
      const downloadUrl = window.URL.createObjectURL(blob);
      const safeFileName = job.fileName || `export-${job.id}.${job.fileFormat.toLowerCase()}`;

      const link = document.createElement('a');
      link.href = downloadUrl;
      link.download = safeFileName;
      link.style.display = 'none';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(downloadUrl);

      setNotification({
        type: 'success',
        message: `Download do arquivo "${safeFileName}" efetuado com sucesso.`,
      });
    } catch (err: unknown) {
      setNotification({
        type: 'error',
        message: err instanceof Error ? err.message : 'Erro ao realizar download.',
      });
    } finally {
      setDownloadingJobId(null);
    }
  };

  // Filtragem de jobs para visualização
  const filteredJobs = useMemo(() => {
    return jobs.filter((job) => {
      if (datasetFilter !== 'ALL' && job.dataset !== datasetFilter) return false;
      if (statusFilter !== 'ALL') {
        const isExpired =
          job.status === 'EXPIRED' ||
          (job.expiresAt && new Date(job.expiresAt).getTime() < Date.now());
        if (statusFilter === 'EXPIRED' && !isExpired) return false;
        if (statusFilter === 'COMPLETED' && (job.status !== 'COMPLETED' || isExpired))
          return false;
        if (statusFilter !== 'EXPIRED' && statusFilter !== 'COMPLETED' && job.status !== statusFilter)
          return false;
      }
      return true;
    });
  }, [jobs, datasetFilter, statusFilter]);

  // Contadores de KPIs
  const stats = useMemo(() => {
    const total = jobs.length;
    let completed = 0;
    let processing = 0;
    let failedOrExpired = 0;

    for (const j of jobs) {
      const isExpired =
        j.status === 'EXPIRED' ||
        (j.expiresAt && new Date(j.expiresAt).getTime() < Date.now());
      if (j.status === 'PENDING' || j.status === 'PROCESSING') {
        processing++;
      } else if (j.status === 'COMPLETED' && !isExpired) {
        completed++;
      } else {
        failedOrExpired++;
      }
    }

    return { total, completed, processing, failedOrExpired };
  }, [jobs]);

  return (
    <section
      role="region"
      aria-label="Central de Relatórios e Exportações"
      className="space-y-6 w-full max-w-full"
    >
      {/* 1. Header & Ações Principais */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-200 dark:border-slate-800">
        <div>
          <h1 className="text-xl sm:text-2xl font-black text-slate-900 dark:text-white flex items-center gap-2">
            <svg className="w-7 h-7 text-blue-600 dark:text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            Central de Relatórios & Exportações
          </h1>
          <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1">
            Exportação assíncrona CSV/XLSX, sanitização de fórmulas, isolamento do tenant e downloads auditados.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          <button
            type="button"
            onClick={() => setIsAuditDrawerOpen(true)}
            aria-label="Abrir trilha de auditoria"
            className="px-3.5 py-2 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs font-semibold text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-700 transition-colors flex items-center gap-1.5 min-h-[42px]"
          >
            <svg className="w-4 h-4 text-indigo-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z" />
            </svg>
            <span>Trilha de Auditoria</span>
          </button>

          <button
            type="button"
            onClick={() => loadJobs()}
            aria-label="Recarregar lista de exportações"
            className="p-2.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-700 transition-colors min-h-[42px] min-w-[42px] flex items-center justify-center"
            title="Atualizar lista"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
          </button>

          <button
            type="button"
            onClick={() => setIsRequestModalOpen(true)}
            aria-label="Solicitar nova exportação"
            className="px-4 py-2 rounded-xl text-xs font-bold bg-blue-600 hover:bg-blue-700 text-white transition-all shadow-sm flex items-center gap-2 min-h-[42px]"
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M12 4v16m8-8H4" />
            </svg>
            <span>Nova Exportação</span>
          </button>
        </div>
      </div>

      {/* 2. Banner de Notificações / Status Feedback */}
      {notification && (
        <div
          role="status"
          className={`p-4 rounded-xl border text-xs flex items-center justify-between gap-3 shadow-sm transition-all ${
            notification.type === 'success'
              ? 'bg-emerald-50 dark:bg-emerald-950/40 border-emerald-200 dark:border-emerald-800 text-emerald-800 dark:text-emerald-300'
              : notification.type === 'error'
              ? 'bg-rose-50 dark:bg-rose-950/40 border-rose-200 dark:border-rose-800 text-rose-800 dark:text-rose-300'
              : 'bg-blue-50 dark:bg-blue-950/40 border-blue-200 dark:border-blue-800 text-blue-800 dark:text-blue-300'
          }`}
        >
          <div className="flex items-center gap-2">
            {notification.type === 'success' ? (
              <svg className="w-4 h-4 text-emerald-600 dark:text-emerald-400 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
            ) : notification.type === 'error' ? (
              <svg className="w-4 h-4 text-rose-600 dark:text-rose-400 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            ) : (
              <svg className="w-4 h-4 text-blue-600 dark:text-blue-400 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            )}
            <span>{notification.message}</span>
          </div>
          <button
            type="button"
            onClick={() => setNotification(null)}
            className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 p-1"
            aria-label="Dispensar aviso"
          >
            ✕
          </button>
        </div>
      )}

      {/* 3. Cards de Resumo / KPIs */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
          <span className="text-[11px] text-slate-400 dark:text-slate-500 font-medium">Total de Exportações</span>
          <p className="text-xl sm:text-2xl font-black text-slate-900 dark:text-white mt-1">
            {stats.total}
          </p>
        </div>

        <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
          <span className="text-[11px] text-emerald-600 dark:text-emerald-400 font-medium">Concluídos (Disponíveis)</span>
          <p className="text-xl sm:text-2xl font-black text-emerald-700 dark:text-emerald-300 mt-1">
            {stats.completed}
          </p>
        </div>

        <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
          <span className="text-[11px] text-blue-600 dark:text-blue-400 font-medium flex items-center gap-1.5">
            <span>Em Processamento</span>
            {stats.processing > 0 && <span className="w-2 h-2 rounded-full bg-blue-500 animate-ping" />}
          </span>
          <p className="text-xl sm:text-2xl font-black text-blue-700 dark:text-blue-300 mt-1">
            {stats.processing}
          </p>
        </div>

        <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
          <span className="text-[11px] text-slate-500 dark:text-slate-400 font-medium">Expirados / Falhas</span>
          <p className="text-xl sm:text-2xl font-black text-slate-700 dark:text-slate-300 mt-1">
            {stats.failedOrExpired}
          </p>
        </div>
      </div>

      {/* 4. Barra de Filtros de Histórico */}
      <div className="p-3.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 flex flex-wrap items-center justify-between gap-3 text-xs">
        {/* Filtro por Dataset */}
        <div className="flex flex-wrap items-center gap-1.5">
          <span className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 mr-1">
            Dataset:
          </span>
          <button
            type="button"
            onClick={() => setDatasetFilter('ALL')}
            className={`px-2.5 py-1 rounded-lg text-xs font-medium transition-colors ${
              datasetFilter === 'ALL'
                ? 'bg-blue-600 text-white'
                : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
            }`}
          >
            Todos
          </button>
          {(Object.keys(DATASET_CATALOG) as ExportDataset[]).map((ds) => (
            <button
              key={ds}
              type="button"
              onClick={() => setDatasetFilter(ds)}
              className={`px-2.5 py-1 rounded-lg text-xs font-medium transition-colors ${
                datasetFilter === ds
                  ? 'bg-blue-600 text-white'
                  : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
              }`}
            >
              {DATASET_CATALOG[ds].label}
            </button>
          ))}
        </div>

        {/* Filtro por Status */}
        <div className="flex items-center gap-1.5">
          <span className="text-[11px] font-semibold text-slate-500 dark:text-slate-400 mr-1">
            Status:
          </span>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as 'ALL' | ExportJobStatus)}
            aria-label="Filtrar por status"
            className="h-8 px-2.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs text-slate-900 dark:text-white"
          >
            <option value="ALL">Todos os Estados</option>
            <option value="COMPLETED">Apenas Concluídos</option>
            <option value="PROCESSING">Em Andamento</option>
            <option value="EXPIRED">Expirados</option>
            <option value="FAILED">Falhas</option>
          </select>
        </div>
      </div>

      {/* 5. Lista de Jobs de Exportação */}
      <div className="space-y-3">
        {isLoading ? (
          <div className="py-16 flex flex-col items-center justify-center gap-3 text-slate-400">
            <svg className="w-8 h-8 animate-spin text-blue-600" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
            </svg>
            <span className="text-xs">Carregando histórico de exportações...</span>
          </div>
        ) : filteredJobs.length === 0 ? (
          <div className="py-16 px-4 rounded-2xl border border-dashed border-slate-200 dark:border-slate-800 bg-white/50 dark:bg-slate-900/40 text-center space-y-3">
            <div className="w-12 h-12 rounded-full bg-slate-100 dark:bg-slate-800 mx-auto flex items-center justify-center text-slate-400">
              <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 13h6m-3-3v6m5 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
            </div>
            <div>
              <p className="font-bold text-sm text-slate-800 dark:text-slate-200">
                Nenhuma exportação encontrada
              </p>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-md mx-auto">
                Não há arquivos exportados para os filtros selecionados. Solicite uma nova exportação para gerar planilhas CSV ou XLSX dos dados do seu tenant.
              </p>
            </div>
            <button
              type="button"
              onClick={() => setIsRequestModalOpen(true)}
              className="px-4 py-2 rounded-xl text-xs font-semibold bg-blue-600 hover:bg-blue-700 text-white transition-colors"
            >
              Solicitar Primeira Exportação
            </button>
          </div>
        ) : (
          filteredJobs.map((job) => (
            <ExportJobStatusCard
              key={job.id}
              job={job}
              onDownload={handleAuthorizedDownload}
              onRefreshJob={handleRefreshJob}
              isDownloading={downloadingJobId === job.id}
            />
          ))
        )}
      </div>

      {/* Modal de Solicitação de Nova Exportação */}
      <ExportRequestModal
        isOpen={isRequestModalOpen}
        onClose={() => setIsRequestModalOpen(false)}
        onSubmit={handleCreateExport}
        isSubmitting={isSubmitting}
      />

      {/* Gaveta de Trilha de Auditoria */}
      <ExportAuditLogsDrawer
        isOpen={isAuditDrawerOpen}
        onClose={() => setIsAuditDrawerOpen(false)}
      />
    </section>
  );
};
