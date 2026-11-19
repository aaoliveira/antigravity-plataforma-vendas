'use client';

import React, { useState, useEffect, useId } from 'react';
import { CompanyDto, CreateCompanyInput, UpdateCompanyInput, TAX_REGIMES } from '@/types/company';
import { cleanCnpj, formatCnpj, isValidCnpj } from '@/utils/cnpj';

interface CompanyFormModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  companyToEdit?: CompanyDto | null;
}

export function CompanyFormModal({
  isOpen,
  onClose,
  onSuccess,
  companyToEdit,
}: CompanyFormModalProps) {
  const isEditing = Boolean(companyToEdit);

  const [corporateName, setCorporateName] = useState('');
  const [tradeName, setTradeName] = useState('');
  const [cnpj, setCnpj] = useState('');
  const [stateRegistration, setStateRegistration] = useState('');
  const [taxRegime, setTaxRegime] = useState('SIMPLES_NACIONAL');
  const [notes, setNotes] = useState('');
  const [isActive, setIsActive] = useState(true);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [cnpjTouched, setCnpjTouched] = useState(false);

  const corporateNameId = useId();
  const tradeNameId = useId();
  const cnpjId = useId();
  const stateRegId = useId();
  const taxRegimeId = useId();
  const notesId = useId();
  const isActiveId = useId();

  useEffect(() => {
    if (companyToEdit) {
      setCorporateName(companyToEdit.corporateName || '');
      setTradeName(companyToEdit.tradeName || '');
      setCnpj(companyToEdit.formattedCnpj || formatCnpj(companyToEdit.cnpj) || '');
      setStateRegistration(companyToEdit.stateRegistration || '');
      setTaxRegime(companyToEdit.taxRegime || 'SIMPLES_NACIONAL');
      setNotes(companyToEdit.notes || '');
      setIsActive(companyToEdit.isActive ?? true);
    } else {
      setCorporateName('');
      setTradeName('');
      setCnpj('');
      setStateRegistration('');
      setTaxRegime('SIMPLES_NACIONAL');
      setNotes('');
      setIsActive(true);
    }
    setErrorMessage(null);
    setCnpjTouched(false);
  }, [companyToEdit, isOpen]);

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

  if (!isOpen) return null;

  const cnpjCleanValue = cleanCnpj(cnpj);
  const isCnpjValid = isEditing ? true : isValidCnpj(cnpjCleanValue);
  const showCnpjError = !isEditing && cnpjTouched && cnpjCleanValue.length > 0 && !isCnpjValid;

  const handleCnpjChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const formatted = formatCnpj(e.target.value);
    setCnpj(formatted);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    if (!corporateName.trim()) {
      setErrorMessage('A Razão Social é obrigatória.');
      return;
    }

    if (!isEditing && !isValidCnpj(cnpjCleanValue)) {
      setErrorMessage('CNPJ sintaticamente inválido. Verifique os dígitos digitados.');
      return;
    }

    setIsSubmitting(true);

    try {
      if (isEditing && companyToEdit) {
        const updatePayload: UpdateCompanyInput = {
          corporateName: corporateName.trim(),
          tradeName: tradeName.trim() || undefined,
          stateRegistration: stateRegistration.trim() || undefined,
          taxRegime: taxRegime || undefined,
          isActive,
          notes: notes.trim() || undefined,
        };

        const res = await fetch(`/api/companies/${companyToEdit.id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(updatePayload),
        });

        if (!res.ok) {
          const data = await res.json().catch(() => ({}));
          throw new Error(data.message || 'Falha ao atualizar dados da empresa.');
        }
      } else {
        const createPayload: CreateCompanyInput = {
          corporateName: corporateName.trim(),
          tradeName: tradeName.trim() || undefined,
          cnpj: cnpjCleanValue,
          stateRegistration: stateRegistration.trim() || undefined,
          taxRegime: taxRegime || undefined,
          notes: notes.trim() || undefined,
        };

        const res = await fetch('/api/companies', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(createPayload),
        });

        if (!res.ok) {
          const data = await res.json().catch(() => ({}));
          throw new Error(data.message || 'Falha ao cadastrar empresa.');
        }
      }

      onSuccess();
      onClose();
    } catch (err: unknown) {
      setErrorMessage(err instanceof Error ? err.message : 'Erro ao processar requisição.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="company-form-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm overflow-y-auto"
    >
      <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-2xl w-full max-w-xl max-h-[90vh] flex flex-col my-auto overflow-hidden">
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-200 dark:border-slate-800 flex items-center justify-between">
          <div>
            <h2 id="company-form-title" className="text-lg font-bold text-slate-900 dark:text-white">
              {isEditing ? 'Editar Empresa' : 'Cadastrar Nova Empresa'}
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Unidade de negócio com validação sintática e isolamento por tenant.
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            disabled={isSubmitting}
            aria-label="Fechar formulário"
            className="p-2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors min-h-[44px] min-w-[44px] flex items-center justify-center"
          >
            ✕
          </button>
        </div>

        {/* Form Body */}
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

          {/* Razão Social */}
          <div>
            <label htmlFor={corporateNameId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Razão Social <span className="text-rose-500">*</span>
            </label>
            <input
              id={corporateNameId}
              type="text"
              required
              value={corporateName}
              onChange={(e) => setCorporateName(e.target.value)}
              placeholder="Ex: Comercial Distribuidora e Logística LTDA"
              className="w-full px-3.5 py-2.5 rounded-xl text-xs bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
            />
          </div>

          {/* Nome Fantasia */}
          <div>
            <label htmlFor={tradeNameId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Nome Fantasia <span className="text-slate-400 font-normal">(Opcional)</span>
            </label>
            <input
              id={tradeNameId}
              type="text"
              value={tradeName}
              onChange={(e) => setTradeName(e.target.value)}
              placeholder="Ex: Distribuidora Alfa"
              className="w-full px-3.5 py-2.5 rounded-xl text-xs bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
            />
          </div>

          {/* CNPJ com Máscara e Validação em Tempo Real */}
          <div>
            <div className="flex items-center justify-between mb-1">
              <label htmlFor={cnpjId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300">
                CNPJ <span className="text-rose-500">*</span>
              </label>
              {!isEditing && cnpjCleanValue.length === 14 && (
                <span className={`text-[11px] font-medium flex items-center gap-1 ${isCnpjValid ? 'text-emerald-600 dark:text-emerald-400' : 'text-rose-600 dark:text-rose-400'}`}>
                  {isCnpjValid ? '✓ CNPJ sintaticamente válido' : '✕ Dígitos verificadores incorretos'}
                </span>
              )}
            </div>
            <input
              id={cnpjId}
              type="text"
              required
              disabled={isEditing}
              value={cnpj}
              onChange={handleCnpjChange}
              onBlur={() => setCnpjTouched(true)}
              placeholder="00.000.000/0000-00"
              maxLength={18}
              aria-describedby={showCnpjError ? `${cnpjId}-error` : undefined}
              className={`w-full px-3.5 py-2.5 rounded-xl text-xs bg-white dark:bg-slate-800 border ${
                showCnpjError
                  ? 'border-rose-500 focus:ring-rose-500'
                  : 'border-slate-300 dark:border-slate-700 focus:ring-blue-500'
              } text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 min-h-[44px] ${
                isEditing ? 'opacity-60 cursor-not-allowed bg-slate-50 dark:bg-slate-800/50' : ''
              }`}
            />
            {isEditing && (
              <p className="text-[11px] text-slate-500 dark:text-slate-400 mt-1">
                O CNPJ é o identificador único permanente da pessoa jurídica e não pode ser alterado após o cadastro.
              </p>
            )}
            {showCnpjError && (
              <p id={`${cnpjId}-error`} role="alert" className="text-[11px] text-rose-500 font-medium mt-1">
                CNPJ sintaticamente inválido. Certifique-se de digitar os 14 dígitos numéricos válidos.
              </p>
            )}
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {/* Inscrição Estadual */}
            <div>
              <label htmlFor={stateRegId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Inscrição Estadual <span className="text-slate-400 font-normal">(Opcional)</span>
              </label>
              <input
                id={stateRegId}
                type="text"
                value={stateRegistration}
                onChange={(e) => setStateRegistration(e.target.value)}
                placeholder="Ex: 123.456.789.000"
                className="w-full px-3.5 py-2.5 rounded-xl text-xs bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
              />
            </div>

            {/* Regime Tributário (Declaratório) */}
            <div>
              <label htmlFor={taxRegimeId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Regime Tributário
              </label>
              <select
                id={taxRegimeId}
                value={taxRegime}
                onChange={(e) => setTaxRegime(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl text-xs bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 text-slate-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
              >
                {TAX_REGIMES.map((reg) => (
                  <option key={reg.value} value={reg.value}>
                    {reg.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Aviso Anti-Alucinação */}
          <div className="p-3 rounded-xl bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-800 text-[11px] text-slate-600 dark:text-slate-400 space-y-1">
            <span className="font-semibold text-slate-700 dark:text-slate-300 flex items-center gap-1.5">
              <svg className="w-4 h-4 text-blue-500 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              Regra de Integridade & Não-Alucinação Fiscal
            </span>
            <p>
              O regime tributário informado é puramente declaratório para categorização interna. O sistema não assume situações fiscais padrão nem consulta bases externas sem autorização.
            </p>
          </div>

          {/* Status Ativo (apenas ao editar) */}
          {isEditing && (
            <div className="flex items-center gap-2 pt-1">
              <input
                id={isActiveId}
                type="checkbox"
                checked={isActive}
                onChange={(e) => setIsActive(e.target.checked)}
                className="w-4 h-4 text-blue-600 rounded border-slate-300 dark:border-slate-700 focus:ring-blue-500"
              />
              <label htmlFor={isActiveId} className="text-xs font-semibold text-slate-700 dark:text-slate-300">
                Empresa Ativa no Tenant
              </label>
            </div>
          )}

          {/* Observações */}
          <div>
            <label htmlFor={notesId} className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Observações Internas <span className="text-slate-400 font-normal">(Opcional)</span>
            </label>
            <textarea
              id={notesId}
              rows={2}
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Anotações para controle contábil ou fiscal interno..."
              className="w-full px-3.5 py-2 rounded-xl text-xs bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>

          {/* Footer Actions */}
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
              disabled={isSubmitting || (!isEditing && !isCnpjValid)}
              className="px-5 py-2.5 rounded-xl text-xs font-semibold bg-blue-600 hover:bg-blue-700 text-white disabled:opacity-50 disabled:cursor-not-allowed transition-colors min-h-[44px] flex items-center gap-2"
            >
              {isSubmitting ? (
                <>
                  <svg className="animate-spin -ml-1 mr-1 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                  </svg>
                  Salvando...
                </>
              ) : isEditing ? (
                'Salvar Alterações'
              ) : (
                'Cadastrar Empresa'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
