'use client';

import React, { useState, useEffect } from 'react';
import {
  ExportDataset,
  ExportFileFormat,
  ExportRequestDto,
  DATASET_CATALOG,
} from '@/types/reports';

interface ExportRequestModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (request: ExportRequestDto) => Promise<boolean>;
  isSubmitting: boolean;
}

export const ExportRequestModal: React.FC<ExportRequestModalProps> = ({
  isOpen,
  onClose,
  onSubmit,
  isSubmitting,
}) => {
  const [dataset, setDataset] = useState<ExportDataset>('FINANCIAL_FACTS');
  const [fileFormat, setFileFormat] = useState<ExportFileFormat>('CSV');
  const [maxRows, setMaxRows] = useState<number>(10000);
  const [datePreset, setDatePreset] = useState<'LAST_7' | 'LAST_30' | 'LAST_90' | 'CUSTOM'>('LAST_30');
  const [startDate, setStartDate] = useState<string>(() => {
    const d = new Date();
    d.setDate(d.getDate() - 30);
    return d.toISOString().split('T')[0];
  });
  const [endDate, setEndDate] = useState<string>(() => {
    return new Date().toISOString().split('T')[0];
  });
  const [marketplace, setMarketplace] = useState<string>('');
  const [status, setStatus] = useState<string>('');
  const [sellerAccountId, setSellerAccountId] = useState<string>('');
  const [showColumnsPreview, setShowColumnsPreview] = useState<boolean>(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Esc key fecha modal
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  // Atualiza datas quando o preset muda
  const handlePresetChange = (preset: 'LAST_7' | 'LAST_30' | 'LAST_90' | 'CUSTOM') => {
    setDatePreset(preset);
    if (preset === 'CUSTOM') return;

    const days = preset === 'LAST_7' ? 7 : preset === 'LAST_30' ? 30 : 90;
    const end = new Date();
    const start = new Date();
    start.setDate(end.getDate() - days);

    setStartDate(start.toISOString().split('T')[0]);
    setEndDate(end.toISOString().split('T')[0]);
  };

  if (!isOpen) return null;

  const currentDatasetConfig = DATASET_CATALOG[dataset];

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);

    // Validação de datas
    if (startDate && endDate && new Date(startDate) > new Date(endDate)) {
      setErrorMsg('A data inicial não pode ser superior à data final.');
      return;
    }

    if (maxRows < 1 || maxRows > 50000) {
      setErrorMsg('O limite de linhas deve estar entre 1 e 50.000.');
      return;
    }

    const filterParams: Record<string, unknown> = {};
    if (startDate) filterParams.startDate = `${startDate}T00:00:00Z`;
    if (endDate) filterParams.endDate = `${endDate}T23:59:59Z`;
    if (marketplace) filterParams.marketplace = marketplace;
    if (status) filterParams.status = status;
    if (sellerAccountId) filterParams.sellerAccountId = sellerAccountId;

    const idempotencyKey = `exp-${dataset.toLowerCase()}-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`;

    const payload: ExportRequestDto = {
      dataset,
      fileFormat,
      maxRows,
      idempotencyKey,
      filterParams,
    };

    const success = await onSubmit(payload);
    if (success) {
      onClose();
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="modal-export-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 overflow-y-auto"
    >
      {/* Backdrop */}
      <div
        className="fixed inset-0 bg-slate-900/60 backdrop-blur-sm transition-opacity"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Janela Modal */}
      <div className="relative w-full max-w-2xl bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 p-5 sm:p-6 max-h-[90vh] overflow-y-auto z-10">
        <div className="flex items-center justify-between pb-4 border-b border-slate-100 dark:border-slate-800">
          <div>
            <h2
              id="modal-export-title"
              className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2"
            >
              <svg className="w-5 h-5 text-blue-600 dark:text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
              Solicitar Nova Exportação
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Processamento assíncrono com sanitização de fórmulas e proteção de tenant.
            </p>
          </div>

          <button
            type="button"
            onClick={onClose}
            aria-label="Fechar janela de exportação"
            className="p-2 text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {errorMsg && (
          <div
            role="alert"
            className="mt-4 p-3 bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 rounded-lg text-xs text-rose-700 dark:text-rose-300"
          >
            {errorMsg}
          </div>
        )}

        <form onSubmit={handleSubmit} className="mt-4 space-y-5">
          {/* 1. Seleção de Conjunto de Dados (Dataset) */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-2">
              1. Conjunto de Dados (Dataset Autorizado)
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
              {(Object.keys(DATASET_CATALOG) as ExportDataset[]).map((dsKey) => {
                const item = DATASET_CATALOG[dsKey];
                const isSelected = dataset === dsKey;
                return (
                  <button
                    key={dsKey}
                    type="button"
                    onClick={() => setDataset(dsKey)}
                    className={`p-3 rounded-xl border text-left transition-all ${
                      isSelected
                        ? 'border-blue-600 dark:border-blue-500 bg-blue-50/70 dark:bg-blue-950/40 shadow-sm ring-1 ring-blue-600'
                        : 'border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700 bg-slate-50/50 dark:bg-slate-800/40'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className="text-xs font-bold text-slate-900 dark:text-white">
                        {item.name}
                      </span>
                      {isSelected && (
                        <span className="w-2 h-2 rounded-full bg-blue-600 dark:bg-blue-400" />
                      )}
                    </div>
                    <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-1 line-clamp-2">
                      {item.description}
                    </p>
                  </button>
                );
              })}
            </div>
          </div>

          {/* 2. Formato de Arquivo & Limite de Linhas */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                2. Formato do Arquivo
              </label>
              <div className="flex gap-2">
                <button
                  type="button"
                  onClick={() => setFileFormat('CSV')}
                  className={`flex-1 py-2 px-3 rounded-lg border text-xs font-semibold transition-all flex items-center justify-center gap-1.5 ${
                    fileFormat === 'CSV'
                      ? 'border-blue-600 bg-blue-50 dark:bg-blue-950/40 text-blue-700 dark:text-blue-300 ring-1 ring-blue-600'
                      : 'border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
                  }`}
                >
                  <span className="font-mono font-bold">CSV</span>
                  <span className="text-[10px] opacity-75">(RFC 4180 BOM)</span>
                </button>
                <button
                  type="button"
                  onClick={() => setFileFormat('XLSX')}
                  className={`flex-1 py-2 px-3 rounded-lg border text-xs font-semibold transition-all flex items-center justify-center gap-1.5 ${
                    fileFormat === 'XLSX'
                      ? 'border-blue-600 bg-blue-50 dark:bg-blue-950/40 text-blue-700 dark:text-blue-300 ring-1 ring-blue-600'
                      : 'border-slate-200 dark:border-slate-800 text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800'
                  }`}
                >
                  <span className="font-mono font-bold">XLSX</span>
                  <span className="text-[10px] opacity-75">(Excel Nativo)</span>
                </button>
              </div>
            </div>

            <div>
              <label htmlFor="max-rows-select" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
                Limite de Linhas (Máx. 50.000)
              </label>
              <select
                id="max-rows-select"
                value={maxRows}
                onChange={(e) => setMaxRows(Number(e.target.value))}
                className="w-full h-9 px-3 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              >
                <option value={1000}>1.000 registros (Rápido)</option>
                <option value={5000}>5.000 registros</option>
                <option value={10000}>10.000 registros (Padrão)</option>
                <option value={25000}>25.000 registros</option>
                <option value={50000}>50.000 registros (Teto de Segurança)</option>
              </select>
            </div>
          </div>

          {/* 3. Filtros Persistidos (Período & Marketplace) */}
          <div className="p-3.5 bg-slate-50 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-800 rounded-xl space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-semibold text-slate-800 dark:text-slate-200">
                3. Filtros de Extração
              </span>
              <div className="flex gap-1 text-[11px]">
                {(['LAST_7', 'LAST_30', 'LAST_90', 'CUSTOM'] as const).map((preset) => (
                  <button
                    key={preset}
                    type="button"
                    onClick={() => handlePresetChange(preset)}
                    className={`px-2 py-0.5 rounded transition-colors ${
                      datePreset === preset
                        ? 'bg-blue-600 text-white font-medium'
                        : 'text-slate-500 dark:text-slate-400 hover:bg-slate-200 dark:hover:bg-slate-700'
                    }`}
                  >
                    {preset === 'LAST_7' ? '7d' : preset === 'LAST_30' ? '30d' : preset === 'LAST_90' ? '90d' : 'Custom'}
                  </button>
                ))}
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
              <div>
                <label htmlFor="start-date-input" className="block text-[11px] text-slate-500 dark:text-slate-400 mb-1">
                  Data Inicial
                </label>
                <input
                  id="start-date-input"
                  type="date"
                  value={startDate}
                  onChange={(e) => {
                    setStartDate(e.target.value);
                    setDatePreset('CUSTOM');
                  }}
                  className="w-full h-8 px-2.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs text-slate-900 dark:text-white"
                />
              </div>

              <div>
                <label htmlFor="end-date-input" className="block text-[11px] text-slate-500 dark:text-slate-400 mb-1">
                  Data Final
                </label>
                <input
                  id="end-date-input"
                  type="date"
                  value={endDate}
                  onChange={(e) => {
                    setEndDate(e.target.value);
                    setDatePreset('CUSTOM');
                  }}
                  className="w-full h-8 px-2.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs text-slate-900 dark:text-white"
                />
              </div>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
              <div>
                <label htmlFor="marketplace-select" className="block text-[11px] text-slate-500 dark:text-slate-400 mb-1">
                  Marketplace / Canal
                </label>
                <select
                  id="marketplace-select"
                  value={marketplace}
                  onChange={(e) => setMarketplace(e.target.value)}
                  className="w-full h-8 px-2.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs text-slate-900 dark:text-white"
                >
                  <option value="">Todos os Marketplaces</option>
                  <option value="MERCADO_LIVRE">Mercado Livre</option>
                  <option value="SHOPEE">Shopee</option>
                </select>
              </div>

              <div>
                <label htmlFor="status-filter-input" className="block text-[11px] text-slate-500 dark:text-slate-400 mb-1">
                  Status Operacional (Opcional)
                </label>
                <input
                  id="status-filter-input"
                  type="text"
                  placeholder="Ex: PAID, DELIVERED, ACTIVE"
                  value={status}
                  onChange={(e) => setStatus(e.target.value)}
                  className="w-full h-8 px-2.5 rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs text-slate-900 dark:text-white"
                />
              </div>
            </div>
          </div>

          {/* 4. Preview de Colunas Incluídas */}
          <div className="border border-slate-200 dark:border-slate-800 rounded-xl overflow-hidden">
            <button
              type="button"
              onClick={() => setShowColumnsPreview(!showColumnsPreview)}
              className="w-full px-3.5 py-2.5 bg-slate-50/70 dark:bg-slate-800/40 text-left flex items-center justify-between text-xs font-medium text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
            >
              <span className="flex items-center gap-1.5">
                <span>Colunas incluídas no relatório</span>
                <span className="px-1.5 py-0.2 bg-blue-100 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300 rounded text-[10px] font-semibold">
                  {currentDatasetConfig.columns.length} colunas
                </span>
              </span>
              <svg
                className={`w-4 h-4 text-slate-400 transition-transform ${showColumnsPreview ? 'rotate-180' : ''}`}
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
              </svg>
            </button>

            {showColumnsPreview && (
              <div className="p-3 bg-white dark:bg-slate-900 flex flex-wrap gap-1.5 border-t border-slate-100 dark:border-slate-800 max-h-36 overflow-y-auto">
                {currentDatasetConfig.columns.map((col, idx) => (
                  <span
                    key={idx}
                    className="inline-flex items-center px-2 py-0.5 rounded-md text-[11px] bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 font-mono"
                  >
                    {col}
                  </span>
                ))}
              </div>
            )}
          </div>

          {/* Avisos de Segurança e Guardrails */}
          <div className="p-3 bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800 rounded-xl text-[11px] text-amber-800 dark:text-amber-300 space-y-1">
            <p className="font-semibold flex items-center gap-1.5">
              <svg className="w-4 h-4 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
              Guardrails de Segurança Ativos:
            </p>
            <ul className="list-disc pl-5 space-y-0.5 text-[10px] text-amber-700 dark:text-amber-400">
              <li>Isolamento estrito: apenas dados autorizados deste tenant serão exportados.</li>
              <li>Prevenção contra CSV Formula Injection ativa (OWASP ASVS / CWE-1236).</li>
              <li>O arquivo final é armazenado em storage privado com expiração automática em 24h.</li>
              <li>Nenhum link público permanente é gerado; o download exige autorização ativa da sessão.</li>
            </ul>
          </div>

          {/* Rodapé com Ações */}
          <div className="flex items-center justify-end gap-2.5 pt-3 border-t border-slate-100 dark:border-slate-800">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2 rounded-xl text-xs font-semibold text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors min-h-[40px]"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-5 py-2 rounded-xl text-xs font-semibold bg-blue-600 hover:bg-blue-700 text-white transition-all shadow-sm flex items-center gap-2 min-h-[40px] disabled:opacity-50"
            >
              {isSubmitting ? (
                <>
                  <svg className="w-4 h-4 animate-spin text-white" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                  </svg>
                  <span>Solicitando...</span>
                </>
              ) : (
                <>
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                  </svg>
                  <span>Solicitar Exportação Assíncrona</span>
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
