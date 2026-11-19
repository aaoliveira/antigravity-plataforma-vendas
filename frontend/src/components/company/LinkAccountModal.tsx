'use client';

import React, { useState, useEffect, useId, useMemo } from 'react';
import { CompanyDto, CompanyAccountLinkDto, SellerAccountDto, LinkAccountInput } from '@/types/company';

interface LinkAccountModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  companies: CompanyDto[];
  targetCompanyId?: string;
  sellerAccounts: SellerAccountDto[];
  existingActiveLinks: CompanyAccountLinkDto[];
}

export function LinkAccountModal({
  isOpen,
  onClose,
  onSuccess,
  companies,
  targetCompanyId,
  sellerAccounts,
  existingActiveLinks,
}: LinkAccountModalProps) {
  const [selectedCompanyId, setSelectedCompanyId] = useState('');
  const [selectedAccountId, setSelectedAccountId] = useState('');
  const [notes, setNotes] = useState('');
  const [confirmTransfer, setConfirmTransfer] = useState(false);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const companySelectId = useId();
  const accountSelectId = useId();
  const notesId = useId();
  const confirmTransferId = useId();

  // Inicializa a empresa e conta selecionadas
  useEffect(() => {
    if (targetCompanyId) {
      setSelectedCompanyId(targetCompanyId);
    } else if (companies.length > 0) {
      setSelectedCompanyId(companies[0].id);
    } else {
      setSelectedCompanyId('');
    }

    if (sellerAccounts.length > 0) {
      setSelectedAccountId(sellerAccounts[0].id);
    } else {
      setSelectedAccountId('');
    }

    setNotes('');
    setConfirmTransfer(false);
    setErrorMessage(null);
  }, [targetCompanyId, companies, sellerAccounts, isOpen]);

  // Tecla Escape para fechar
  useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === 'Escape' && isOpen && !isSubmitting) {
        onClose();
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, isSubmitting, onClose]);

  // Encontra vínculo ativo da conta selecionada, se houver
  const currentActiveLink = useMemo(() => {
    if (!selectedAccountId) return null;
    return existingActiveLinks.find((l) => l.sellerAccountId === selectedAccountId && l.isActive) || null;
  }, [selectedAccountId, existingActiveLinks]);

  const selectedCompany = useMemo(() => {
    return companies.find((c) => c.id === selectedCompanyId) || null;
  }, [selectedCompanyId, companies]);

  const selectedAccount = useMemo(() => {
    return sellerAccounts.find((a) => a.id === selectedAccountId) || null;
  }, [selectedAccountId, sellerAccounts]);

  const isSameCompany = Boolean(
    currentActiveLink && selectedCompanyId && currentActiveLink.companyId === selectedCompanyId
  );

  const isTransfer = Boolean(
    currentActiveLink && selectedCompanyId && currentActiveLink.companyId !== selectedCompanyId
  );

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!selectedCompanyId) {
      setErrorMessage('Selecione uma empresa válida.');
      return;
    }

    if (!selectedAccountId) {
      setErrorMessage('Selecione uma conta de marketplace conectada.');
      return;
    }

    if (isSameCompany) {
      setErrorMessage('Esta conta já está vinculada a esta mesma empresa.');
      return;
    }

    if (isTransfer && !confirmTransfer) {
      setErrorMessage('É necessário confirmar expressamente a transferência do vínculo.');
      return;
    }

    setIsSubmitting(true);

    try {
      const payload: LinkAccountInput = {
        sellerAccountId: selectedAccountId,
        notes: notes.trim() || undefined,
        replaceActiveLink: isTransfer,
      };

      const res = await fetch(`/api/companies/${selectedCompanyId}/links`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data.message || 'Falha ao vincular conta à empresa.');
      }

      onSuccess();
      onClose();
    } catch (err: unknown) {
      setErrorMessage(err instanceof Error ? err.message : 'Erro ao processar vínculo.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const isSubmitDisabled =
    isSubmitting ||
    !selectedCompanyId ||
    !selectedAccountId ||
    isSameCompany ||
    (isTransfer && !confirmTransfer);

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="link-account-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm overflow-y-auto"
    >
      <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-2xl w-full max-w-lg max-h-[90vh] flex flex-col my-auto overflow-hidden">
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between">
          <div>
            <h2 id="link-account-title" className="text-lg font-bold text-slate-900 dark:text-white">
              Vincular Conta de Marketplace
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Associação inequívoca e controle de titularidade por empresa.
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            aria-label="Fechar diálogo de vínculo"
            className="p-2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors min-h-[44px] min-w-[44px] flex items-center justify-center"
          >
            ✕
          </button>
        </div>

        {/* Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4 overflow-y-auto flex-1">
          {errorMessage && (
            <div
              role="alert"
              className="p-3.5 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900 text-xs text-rose-700 dark:text-rose-300 flex items-start gap-2.5"
            >
              <svg className="w-5 h-5 shrink-0 text-rose-500" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
              </svg>
              <span>{errorMessage}</span>
            </div>
          )}

          {/* Selecionar Empresa */}
          <div>
            <label htmlFor={companySelectId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Empresa Receptora <span className="text-rose-500">*</span>
            </label>
            <select
              id={companySelectId}
              required
              value={selectedCompanyId}
              onChange={(e) => setSelectedCompanyId(e.target.value)}
              className="w-full px-3.5 py-2.5 rounded-xl text-xs bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
            >
              <option value="" disabled>Selecione uma empresa...</option>
              {companies.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.corporateName} {c.tradeName ? `(${c.tradeName})` : ''} — {c.formattedCnpj || c.cnpj}
                </option>
              ))}
            </select>
          </div>

          {/* Selecionar Conta Conectada */}
          <div>
            <label htmlFor={accountSelectId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Conta de Marketplace Conectada <span className="text-rose-500">*</span>
            </label>
            <select
              id={accountSelectId}
              required
              value={selectedAccountId}
              onChange={(e) => {
                setSelectedAccountId(e.target.value);
                setConfirmTransfer(false);
              }}
              className="w-full px-3.5 py-2.5 rounded-xl text-xs bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
            >
              <option value="" disabled>Selecione uma conta...</option>
              {sellerAccounts.map((a) => {
                const link = existingActiveLinks.find((l) => l.sellerAccountId === a.id && l.isActive);
                const statusSuffix = link ? ` [Vinculada à: ${link.companyName}]` : ' [Não vinculada]';
                return (
                  <option key={a.id} value={a.id}>
                    {a.accountName} ({a.marketplace}) — ID {a.externalAccountId}{statusSuffix}
                  </option>
                );
              })}
            </select>
          </div>

          {/* Estado de Vínculo: Mesma Empresa */}
          {isSameCompany && (
            <div className="p-3.5 rounded-xl bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-900 text-xs text-amber-800 dark:text-amber-300 space-y-1">
              <span className="font-semibold flex items-center gap-1.5">
                ⚠ Vínculo Já Existente
              </span>
              <p>
                A conta &quot;{selectedAccount?.accountName}&quot; já está vinculada ativamente a esta mesma empresa. Nenhuma ação de transferência é necessária.
              </p>
            </div>
          )}

          {/* Estado de Vínculo: Transferência de Empresa */}
          {isTransfer && currentActiveLink && (
            <div className="p-4 rounded-xl bg-amber-50 dark:bg-amber-950/40 border border-amber-300 dark:border-amber-800 space-y-3">
              <div className="flex items-start gap-2.5 text-xs text-amber-900 dark:text-amber-200">
                <svg className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
                <div className="space-y-1">
                  <span className="font-bold">Aviso de Transferência de Titularidade</span>
                  <p>
                    Esta conta já está vinculada ativamente à empresa <strong className="underline">{currentActiveLink.companyName}</strong>.
                  </p>
                  <p className="text-[11px] text-amber-800 dark:text-amber-300">
                    Ao confirmar a transferência para <strong>{selectedCompany?.corporateName}</strong>, o vínculo anterior será encerrado imediatamente e preservado no histórico para integridade contábil.
                  </p>
                </div>
              </div>

              <div className="pt-2 border-t border-amber-200 dark:border-amber-900/60 flex items-start gap-2.5">
                <input
                  id={confirmTransferId}
                  type="checkbox"
                  required
                  checked={confirmTransfer}
                  onChange={(e) => setConfirmTransfer(e.target.checked)}
                  className="w-4 h-4 mt-0.5 text-amber-600 rounded border-amber-300 focus:ring-amber-500"
                />
                <label htmlFor={confirmTransferId} className="text-xs font-semibold text-amber-900 dark:text-amber-200 cursor-pointer">
                  Confirmo a transferência desta conta para a nova empresa e o encerramento do vínculo vigente anterior.
                </label>
              </div>
            </div>
          )}

          {/* Estado de Vínculo: Novo vínculo direto */}
          {!isSameCompany && !isTransfer && selectedAccountId && (
            <div className="p-3.5 rounded-xl bg-blue-50 dark:bg-blue-950/40 border border-blue-200 dark:border-blue-900 text-xs text-blue-800 dark:text-blue-300 flex items-center gap-2">
              <svg className="w-5 h-5 text-blue-500 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span>Esta conta ainda não possui vínculo ativo e será associada à empresa selecionada.</span>
            </div>
          )}

          {/* Observações */}
          <div>
            <label htmlFor={notesId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Motivo ou Observação do Vínculo <span className="text-slate-400 font-normal">(Opcional)</span>
            </label>
            <textarea
              id={notesId}
              rows={2}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Ex: Alocação operacional da loja oficial na filial..."
              className="w-full px-3.5 py-2 rounded-xl text-xs bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* Actions */}
          <div className="pt-4 border-t border-slate-200 dark:border-slate-800 flex items-center justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2.5 rounded-xl text-xs font-semibold text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors min-h-[44px]"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isSubmitDisabled}
              className="px-5 py-2.5 rounded-xl text-xs font-semibold bg-blue-600 hover:bg-blue-700 text-white disabled:opacity-50 disabled:cursor-not-allowed transition-colors min-h-[44px] flex items-center gap-2"
            >
              {isSubmitting ? (
                <>
                  <svg className="animate-spin -ml-1 mr-1 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                  </svg>
                  Processando...
                </>
              ) : isTransfer ? (
                'Confirmar Transferência'
              ) : (
                'Confirmar Vínculo'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
