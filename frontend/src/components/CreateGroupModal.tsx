'use client';

import React, { useState, useEffect } from 'react';

export interface MonitoringGroupItem {
  id: string;
  tenantId: string;
  name: string;
  description?: string | null;
  color: string;
  minIntervalMinutes: number;
  retentionDays: number;
  status: 'ACTIVE' | 'PAUSED' | 'DELETED';
  itemCount?: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateGroupModalProps {
  isOpen: boolean;
  groupToEdit?: MonitoringGroupItem | null;
  onClose: () => void;
  onSuccess: (savedGroup: MonitoringGroupItem) => void;
}

const PRESET_COLORS = [
  '#3B82F6', // Azul
  '#10B981', // Esmeralda
  '#8B5CF6', // Roxo
  '#F59E0B', // Âmbar
  '#EC4899', // Rosa
  '#6366F1', // Índigo
  '#14B8A6', // Ciano
  '#EF4444', // Vermelho
];

export function CreateGroupModal({
  isOpen,
  groupToEdit,
  onClose,
  onSuccess,
}: CreateGroupModalProps) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [color, setColor] = useState('#3B82F6');
  const [minIntervalMinutes, setMinIntervalMinutes] = useState(60);
  const [retentionDays, setRetentionDays] = useState(90);
  const [status, setStatus] = useState<'ACTIVE' | 'PAUSED'>('ACTIVE');

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (groupToEdit) {
      setName(groupToEdit.name);
      setDescription(groupToEdit.description || '');
      setColor(groupToEdit.color || '#3B82F6');
      setMinIntervalMinutes(groupToEdit.minIntervalMinutes || 60);
      setRetentionDays(groupToEdit.retentionDays || 90);
      setStatus(groupToEdit.status === 'PAUSED' ? 'PAUSED' : 'ACTIVE');
    } else {
      setName('');
      setDescription('');
      setColor('#3B82F6');
      setMinIntervalMinutes(60);
      setRetentionDays(90);
      setStatus('ACTIVE');
    }
    setErrorMessage(null);
  }, [groupToEdit, isOpen]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen && !isSubmitting) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, isSubmitting, onClose]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setErrorMessage('O nome do grupo é obrigatório.');
      return;
    }
    if (retentionDays < 7) {
      setErrorMessage('O período de retenção deve ser de no mínimo 7 dias.');
      return;
    }

    setIsSubmitting(true);
    setErrorMessage(null);

    const isEdit = !!groupToEdit?.id;
    const url = isEdit
      ? `/api/monitoring/groups/${groupToEdit.id}`
      : '/api/monitoring/groups';
    const method = isEdit ? 'PUT' : 'POST';

    try {
      const res = await fetch(url, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: name.trim(),
          description: description.trim() || null,
          color,
          minIntervalMinutes: Number(minIntervalMinutes),
          retentionDays: Number(retentionDays),
          status,
        }),
      });

      if (res.status === 409) {
        const err = await res.json().catch(() => ({}));
        setErrorMessage(err.message || 'Já existe um grupo de monitoramento com este nome.');
        setIsSubmitting(false);
        return;
      }

      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        setErrorMessage(err.message || 'Falha ao salvar grupo de monitoramento.');
        setIsSubmitting(false);
        return;
      }

      const saved = await res.json();
      onSuccess(saved);
      onClose();
    } catch {
      setErrorMessage('Erro de comunicação ao salvar grupo.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="group-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-fade-in"
    >
      <div className="relative w-full max-w-lg bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 p-6 space-y-5">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
          <div>
            <h2 id="group-modal-title" className="text-base font-bold text-slate-900 dark:text-white">
              {groupToEdit ? 'Editar Grupo de Monitoramento' : 'Novo Grupo de Monitoramento'}
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Organize anúncios e concorrentes por nicho com intervalo e retenção configuráveis.
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fechar modal"
            className="p-2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 min-h-[44px] min-w-[44px] flex items-center justify-center"
          >
            ✕
          </button>
        </div>

        {errorMessage && (
          <div
            role="alert"
            className="p-3 bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 text-xs text-rose-800 dark:text-rose-300 rounded-xl"
          >
            {errorMessage}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Nome */}
          <div>
            <label htmlFor="group-name" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Nome do Grupo *
            </label>
            <input
              id="group-name"
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Ex: Concorrentes Fones TWS, Mercado Livre Monitores"
              className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 min-h-[44px]"
            />
          </div>

          {/* Descrição */}
          <div>
            <label htmlFor="group-desc" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Descrição (opcional)
            </label>
            <textarea
              id="group-desc"
              rows={2}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Objetivo do grupo, estratégia ou observações operacionais..."
              className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* Seletor de Cor */}
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-2">
              Cor de Identificação
            </label>
            <div className="flex items-center gap-2 flex-wrap">
              {PRESET_COLORS.map((c) => (
                <button
                  key={c}
                  type="button"
                  onClick={() => setColor(c)}
                  aria-label={`Selecionar cor ${c}`}
                  style={{ backgroundColor: c }}
                  className={`w-7 h-7 rounded-full transition-transform min-h-[44px] min-w-[44px] flex items-center justify-center ${
                    color === c ? 'ring-4 ring-offset-2 ring-blue-500 scale-110' : 'hover:scale-105'
                  }`}
                >
                  {color === c && <span className="text-white text-xs font-bold">✓</span>}
                </button>
              ))}
            </div>
          </div>

          {/* Linha Dupla: Intervalo Mínimo & Retenção */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label htmlFor="group-interval" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Intervalo Mínimo de Coleta
              </label>
              <select
                id="group-interval"
                value={minIntervalMinutes}
                onChange={(e) => setMinIntervalMinutes(Number(e.target.value))}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 min-h-[44px]"
              >
                <option value={15}>15 minutos (Alta frequência)</option>
                <option value={30}>30 minutos</option>
                <option value={60}>1 hora (Padrão recomendado)</option>
                <option value={120}>2 horas</option>
                <option value={360}>6 horas</option>
                <option value={1440}>24 horas (Diário)</option>
              </select>
              <span className="text-[10px] text-slate-400 mt-1 block">
                Respeita quotas e janelas mínimas sem sobrecarregar a fonte.
              </span>
            </div>

            <div>
              <label htmlFor="group-retention" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Retenção de Snapshots (Dias)
              </label>
              <select
                id="group-retention"
                value={retentionDays}
                onChange={(e) => setRetentionDays(Number(e.target.value))}
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 min-h-[44px]"
              >
                <option value={15}>15 dias</option>
                <option value={30}>30 dias</option>
                <option value={60}>60 dias</option>
                <option value={90}>90 dias (Recomendado)</option>
                <option value={180}>180 dias (6 meses)</option>
                <option value={365}>365 dias (1 ano)</option>
              </select>
              <span className="text-[10px] text-slate-400 mt-1 block">
                Mínimo obrigatório de 7 dias para fins de auditoria e conformidade.
              </span>
            </div>
          </div>

          {/* Status */}
          <div>
            <label htmlFor="group-status" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Status do Monitoramento
            </label>
            <select
              id="group-status"
              value={status}
              onChange={(e) => setStatus(e.target.value as 'ACTIVE' | 'PAUSED')}
              className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 min-h-[44px]"
            >
              <option value="ACTIVE">🟢 Ativo (Executa coletas periódicas)</option>
              <option value="PAUSED">⏸️ Pausado (Interrompe novas coletas)</option>
            </select>
          </div>

          {/* Ações */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-100 dark:border-slate-800">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2.5 min-h-[44px] text-xs font-medium text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-xl transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-5 py-2.5 min-h-[44px] text-xs font-semibold text-white bg-blue-600 hover:bg-blue-700 active:bg-blue-800 rounded-xl transition-colors shadow-sm disabled:opacity-50 flex items-center gap-2"
            >
              {isSubmitting ? (
                <>
                  <svg className="animate-spin h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                  </svg>
                  <span>Salvando...</span>
                </>
              ) : (
                <span>{groupToEdit ? 'Atualizar Grupo' : 'Criar Grupo'}</span>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
