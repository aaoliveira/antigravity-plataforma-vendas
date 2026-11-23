'use client';

import React, { useState, useEffect, useCallback } from 'react';
import {
  DraftListingSummaryDto,
  DraftListingDetailDto,
  DraftListingVersionDto,
  MarketplaceRuleLimitsDto,
  DraftTone,
  DraftGenerationRequest,
} from '@/types/draftlisting';
import { CharacterLimitGauge } from './CharacterLimitGauge';
import { DraftDiffViewer } from './DraftDiffViewer';
import { VersionHistoryDrawer } from './VersionHistoryDrawer';

export const DraftListingEditorView: React.FC = () => {
  const [drafts, setDrafts] = useState<DraftListingSummaryDto[]>([]);
  const [selectedDraftId, setSelectedDraftId] = useState<string | null>(null);
  const [activeDraft, setActiveDraft] = useState<DraftListingDetailDto | null>(null);
  const [rules, setRules] = useState<MarketplaceRuleLimitsDto[]>([]);

  // Filtros
  const [marketplaceFilter, setMarketplaceFilter] = useState<string>('TODOS');
  const [statusFilter, setStatusFilter] = useState<string>('TODOS');

  // Editor Inputs
  const [titleInput, setTitleInput] = useState<string>('');
  const [descriptionInput, setDescriptionInput] = useState<string>('');
  const [reviewNotesInput, setReviewNotesInput] = useState<string>('');

  // Modais e Painéis
  const [isDiffOpen, setIsDiffOpen] = useState<boolean>(false);
  const [isHistoryDrawerOpen, setIsHistoryDrawerOpen] = useState<boolean>(false);
  const [isNewModalOpen, setIsNewModalOpen] = useState<boolean>(false);

  // Status & Feedback
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);
  const [copyFeedback, setCopyFeedback] = useState<string | null>(null);

  // Form de Novo Rascunho
  const [newMkt, setNewMkt] = useState<string>('MERCADO_LIVRE');
  const [newTone, setNewTone] = useState<DraftTone>('INFORMATIVE_OBJECTIVE');
  const [newBrand, setNewBrand] = useState<string>('');
  const [newModel, setNewModel] = useState<string>('');
  const [newCategory, setNewCategory] = useState<string>('');
  const [newMaterials, setNewMaterials] = useState<string>('');
  const [newDimensions, setNewDimensions] = useState<string>('');
  const [newWarranty, setNewWarranty] = useState<string>('90 dias legal conforme CDC');
  const [newKeywords, setNewKeywords] = useState<string>('');

  // 1. Carregar Regras Homologadas
  const fetchRules = useCallback(async () => {
    try {
      const res = await fetch('/api/ai/drafts/rules');
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data)) {
          setRules(data);
        }
      }
    } catch {
      // Silencioso ou fallback
    }
  }, []);

  // 2. Carregar Lista de Rascunhos
  const fetchDrafts = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage(null);
    try {
      const params = new URLSearchParams();
      if (marketplaceFilter !== 'TODOS') params.set('targetMarketplace', marketplaceFilter);
      if (statusFilter !== 'TODOS') params.set('status', statusFilter);

      const res = await fetch(`/api/ai/drafts?${params.toString()}`);
      if (!res.ok) {
        throw new Error('Falha ao carregar rascunhos.');
      }
      const data = await res.json();
      const content: DraftListingSummaryDto[] = data.content || [];
      setDrafts(content);

      if (content.length > 0 && !selectedDraftId) {
        setSelectedDraftId(content[0].id);
      }
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Erro ao carregar dados.');
    } finally {
      setIsLoading(false);
    }
  }, [marketplaceFilter, statusFilter, selectedDraftId]);

  // 3. Carregar Detalhe do Rascunho Selecionado
  const fetchDraftDetail = useCallback(async (id: string) => {
    setErrorMessage(null);
    try {
      const res = await fetch(`/api/ai/drafts/${id}`);
      if (!res.ok) {
        throw new Error('Falha ao carregar detalhes do rascunho.');
      }
      const detail: DraftListingDetailDto = await res.json();
      setActiveDraft(detail);

      const effectiveTitle = detail.currentVersion.reviewedTitle || detail.currentVersion.generatedTitle;
      const effectiveDesc = detail.currentVersion.reviewedDescription || detail.currentVersion.generatedDescription;

      setTitleInput(effectiveTitle);
      setDescriptionInput(effectiveDesc);
      setReviewNotesInput(detail.currentVersion.reviewNotes || '');
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Erro ao carregar rascunho.');
    }
  }, []);

  useEffect(() => {
    fetchRules();
    fetchDrafts();
  }, [fetchRules, fetchDrafts]);

  useEffect(() => {
    if (selectedDraftId) {
      fetchDraftDetail(selectedDraftId);
    }
  }, [selectedDraftId, fetchDraftDetail]);

  // Validação de Limites
  const limits =
    activeDraft?.marketplaceLimits ||
    (Array.isArray(rules) ? rules.find((r) => r.marketplace === activeDraft?.targetMarketplace) : undefined);
  const minTitle = limits?.minTitleLength ?? 1;
  const maxTitle = limits?.maxTitleLength ?? 60;
  const minDesc = limits?.minDescriptionLength ?? 1;
  const maxDesc = limits?.maxDescriptionLength ?? 25000;

  const isTitleValid = titleInput.trim().length >= minTitle && titleInput.trim().length <= maxTitle;
  const isDescValid = descriptionInput.trim().length >= minDesc && descriptionInput.trim().length <= maxDesc;
  const canSave = isTitleValid && isDescValid && !isSaving;

  // Ação: Salvar Revisão (Edição Humana)
  const handleSaveEdit = async () => {
    if (!activeDraft || !selectedDraftId || !canSave) return;
    setIsSaving(true);
    setErrorMessage(null);
    setSuccessMessage(null);

    try {
      const res = await fetch(`/api/ai/drafts/${selectedDraftId}/edit`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          reviewedTitle: titleInput.trim(),
          reviewedDescription: descriptionInput.trim(),
          reviewNotes: reviewNotesInput.trim() || undefined,
        }),
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.detail || data.message || 'Falha ao salvar edição.');
      }

      setSuccessMessage('Nova versão de revisão salva com sucesso!');
      await fetchDraftDetail(selectedDraftId);
      await fetchDrafts();
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Erro ao salvar rascunho.');
    } finally {
      setIsSaving(false);
    }
  };

  // Ação: Aprovar Rascunho Local
  const handleApprove = async () => {
    if (!activeDraft || !selectedDraftId) return;
    setIsSaving(true);
    setErrorMessage(null);
    try {
      const res = await fetch(`/api/ai/drafts/${selectedDraftId}/approve`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ approvalNotes: 'Aprovado para uso interno/exportação' }),
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.detail || 'Falha ao aprovar rascunho.');
      }

      setSuccessMessage('Rascunho aprovado com sucesso para uso local!');
      await fetchDraftDetail(selectedDraftId);
      await fetchDrafts();
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Erro ao aprovar.');
    } finally {
      setIsSaving(false);
    }
  };

  // Ação: Rejeitar Rascunho
  const handleReject = async () => {
    if (!activeDraft || !selectedDraftId) return;
    const reason = window.prompt('Informe o motivo da rejeição do rascunho:');
    if (!reason || !reason.trim()) return;

    setIsSaving(true);
    setErrorMessage(null);
    try {
      const res = await fetch(`/api/ai/drafts/${selectedDraftId}/reject`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ rejectionReason: reason.trim() }),
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.detail || 'Falha ao rejeitar rascunho.');
      }

      setSuccessMessage('Rascunho marcado como rejeitado.');
      await fetchDraftDetail(selectedDraftId);
      await fetchDrafts();
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Erro ao rejeitar.');
    } finally {
      setIsSaving(false);
    }
  };

  // Ação: Gerar Novo Rascunho
  const handleCreateDraft = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    setErrorMessage(null);

    const materialsArray = newMaterials.split(',').map((s) => s.trim()).filter(Boolean);
    const keywordsArray = newKeywords.split(',').map((s) => s.trim()).filter(Boolean);
    const dimensionsMap = newDimensions ? { medidas: newDimensions.trim() } : undefined;

    const payload: DraftGenerationRequest = {
      targetMarketplace: newMkt,
      tone: newTone,
      brand: newBrand.trim() || undefined,
      model: newModel.trim() || undefined,
      category: newCategory.trim() || undefined,
      materials: materialsArray.length > 0 ? materialsArray : undefined,
      dimensions: dimensionsMap,
      warranty: newWarranty.trim() || undefined,
      keywords: keywordsArray.length > 0 ? keywordsArray : undefined,
    };

    try {
      const res = await fetch('/api/ai/drafts/generate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.detail || data.message || 'Falha ao gerar rascunho.');
      }

      setIsNewModalOpen(false);
      setSuccessMessage('Rascunho de anúncio gerado com sucesso!');
      setSelectedDraftId(data.id);
      await fetchDrafts();
      await fetchDraftDetail(data.id);
    } catch (err) {
      setErrorMessage(err instanceof Error ? err.message : 'Erro na geração de rascunho.');
    } finally {
      setIsSaving(false);
    }
  };

  // Copiar para Área de Transferência
  const copyToClipboard = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    setCopyFeedback(`${label} copiado!`);
    setTimeout(() => setCopyFeedback(null), 3000);
  };

  // Exportar como TXT
  const handleExportTxt = () => {
    if (!activeDraft) return;
    const content = `[RASCUNHO DE ANÚNCIO - SELLER INTELLIGENCE PLATFORM]
Canal: ${activeDraft.targetMarketplace}
Versão: ${activeDraft.currentVersionNumber}
Status: ${activeDraft.status} (Publicação Externa Desabilitada no MVP)

TÍTULO:
${titleInput}

DESCRIÇÃO:
${descriptionInput}
`;
    const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `anuncio-${activeDraft.targetMarketplace.toLowerCase()}-v${activeDraft.currentVersionNumber}.txt`;
    a.click();
    URL.revokeObjectURL(url);
  };

  // Exportar como JSON
  const handleExportJson = () => {
    if (!activeDraft) return;
    const data = JSON.stringify(activeDraft, null, 2);
    const blob = new Blob([data], { type: 'application/json;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `anuncio-${activeDraft.id}-v${activeDraft.currentVersionNumber}.json`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="flex flex-col gap-6 w-full max-w-7xl mx-auto p-4 md:p-6" role="region" aria-label="Editor de Rascunho de Anúncios">
      {/* Banner de Governança Estrita do MVP */}
      <div className="bg-amber-50 dark:bg-amber-950/40 border border-amber-300 dark:border-amber-800 rounded-xl p-4 flex flex-col md:flex-row items-start md:items-center justify-between gap-3 text-xs text-amber-900 dark:text-amber-200">
        <div className="flex items-center gap-3">
          <span className="text-xl">⚠️</span>
          <div>
            <p className="font-bold">
              MODO RASCUNHO ASSISTIDO &middot; REVISÃO HUMANA MANDATÓRIA
            </p>
            <p className="text-amber-800 dark:text-amber-300">
              Todo conteúdo gerado por IA requer revisão e validação pelo operador. A publicação automática ou direta em marketplaces está categoricamente desabilitada no MVP.
            </p>
          </div>
        </div>
        <button
          onClick={() => setIsNewModalOpen(true)}
          className="whitespace-nowrap px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-medium transition-colors shadow-sm min-h-[44px]"
        >
          + Novo Rascunho
        </button>
      </div>

      {/* Mensagens de Feedback */}
      {errorMessage && (
        <div className="p-3 bg-red-50 dark:bg-red-950/40 border border-red-300 dark:border-red-800 rounded-lg text-xs text-red-800 dark:text-red-300 flex items-center justify-between">
          <span>{errorMessage}</span>
          <button onClick={() => setErrorMessage(null)} className="text-red-500 font-bold ml-2">✕</button>
        </div>
      )}
      {successMessage && (
        <div className="p-3 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-300 dark:border-emerald-800 rounded-lg text-xs text-emerald-800 dark:text-emerald-300 flex items-center justify-between">
          <span>{successMessage}</span>
          <button onClick={() => setSuccessMessage(null)} className="text-emerald-500 font-bold ml-2">✕</button>
        </div>
      )}
      {copyFeedback && (
        <div className="p-2 bg-slate-800 text-white text-xs rounded-md fixed bottom-4 right-4 z-50 shadow-lg animate-fade-in">
          ✓ {copyFeedback}
        </div>
      )}

      {/* Grid Principal */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Painel Lateral: Lista de Rascunhos e Filtros */}
        <div className="lg:col-span-4 flex flex-col gap-4">
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-4 flex flex-col gap-3">
            <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100">
              Rascunhos do Catálogo ({drafts.length})
            </h3>

            {/* Filtros */}
            <div className="grid grid-cols-2 gap-2 text-xs">
              <div>
                <label htmlFor="filter-marketplace" className="block text-slate-500 dark:text-slate-400 mb-1">Marketplace</label>
                <select
                  id="filter-marketplace"
                  aria-label="Filtrar rascunhos por marketplace"
                  value={marketplaceFilter}
                  onChange={(e) => setMarketplaceFilter(e.target.value)}
                  className="w-full p-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg"
                >
                  <option value="TODOS">Todos</option>
                  <option value="MERCADO_LIVRE">Mercado Livre</option>
                  <option value="SHOPEE">Shopee</option>
                </select>
              </div>
              <div>
                <label htmlFor="filter-status" className="block text-slate-500 dark:text-slate-400 mb-1">Status</label>
                <select
                  id="filter-status"
                  aria-label="Filtrar rascunhos por status"
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                  className="w-full p-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg"
                >
                  <option value="TODOS">Todos</option>
                  <option value="DRAFT">Rascunho</option>
                  <option value="REVIEWED">Revisado</option>
                  <option value="APPROVED">Aprovado</option>
                  <option value="REJECTED">Rejeitado</option>
                </select>
              </div>
            </div>

            {/* Lista com Scroll */}
            <div className="flex flex-col gap-2 max-h-[500px] overflow-y-auto mt-2 pr-1">
              {isLoading ? (
                <div className="text-center py-8 text-xs text-slate-600 dark:text-slate-400">Carregando rascunhos...</div>
              ) : drafts.length === 0 ? (
                <div className="text-center py-8 text-xs text-slate-600 dark:text-slate-400">
                  Nenhum rascunho encontrado para os filtros selecionados.
                </div>
              ) : (
                drafts.map((d) => {
                  const isSelected = d.id === selectedDraftId;
                  return (
                    <button
                      key={d.id}
                      onClick={() => setSelectedDraftId(d.id)}
                      className={`text-left p-3 rounded-lg border transition-all ${
                        isSelected
                          ? 'border-indigo-600 bg-indigo-50/50 dark:bg-indigo-950/30'
                          : 'border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700'
                      }`}
                    >
                      <div className="flex items-center justify-between mb-1">
                        <span className="text-xs font-semibold text-slate-900 dark:text-slate-100 uppercase">
                          {d.targetMarketplace === 'MERCADO_LIVRE' ? 'Mercado Livre' : d.targetMarketplace}
                        </span>
                        <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          d.status === 'APPROVED' ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300' :
                          d.status === 'REVIEWED' ? 'bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300' :
                          d.status === 'REJECTED' ? 'bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-300' :
                          'bg-blue-100 text-blue-800 dark:bg-blue-950 dark:text-blue-300'
                        }`}>
                          {d.status}
                        </span>
                      </div>
                      <div className="flex items-center justify-between text-[11px] text-slate-500 dark:text-slate-400">
                        <span>Versão {d.currentVersionNumber}</span>
                        <span>{new Date(d.updatedAt).toLocaleDateString('pt-BR')}</span>
                      </div>
                    </button>
                  );
                })
              )}
            </div>
          </div>
        </div>

        {/* Painel Central: Editor de Rascunho */}
        <div className="lg:col-span-8 flex flex-col gap-4">
          {activeDraft ? (
            <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-5 flex flex-col gap-5">
              {/* Cabeçalho do Rascunho Ativo */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-slate-200 dark:border-slate-800 pb-4">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-xs font-bold uppercase tracking-wider text-slate-600 dark:text-slate-400">
                      {activeDraft.targetMarketplace}
                    </span>
                    <span className="text-xs font-semibold text-indigo-600 dark:text-indigo-400">
                      &bull; Versão {activeDraft.currentVersionNumber}
                    </span>
                    {activeDraft.currentVersion.userEdited && (
                      <span className="px-2 py-0.5 rounded-full text-[10px] bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300 font-semibold">
                        Editado pelo Operador
                      </span>
                    )}
                  </div>
                  <h2 className="text-lg font-bold text-slate-900 dark:text-slate-100">
                    Editor de Rascunho Assistido
                  </h2>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setIsHistoryDrawerOpen(true)}
                    className="px-3 py-1.5 border border-slate-300 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg text-xs font-semibold transition-colors min-h-[44px]"
                  >
                    Histórico ({activeDraft.versionHistory.length})
                  </button>
                  <button
                    onClick={() => setIsDiffOpen(!isDiffOpen)}
                    className={`px-3 py-1.5 border rounded-lg text-xs font-semibold transition-colors min-h-[44px] ${
                      isDiffOpen
                        ? 'border-indigo-600 bg-indigo-50 text-indigo-700 dark:bg-indigo-950/40 dark:text-indigo-300'
                        : 'border-slate-300 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800'
                    }`}
                  >
                    {isDiffOpen ? 'Ocultar Comparação' : 'Ver Comparação Lado a Lado'}
                  </button>
                </div>
              </div>

              {/* Comparador Lado a Lado Opcional */}
              {isDiffOpen && (
                <DraftDiffViewer
                  originalTitle={activeDraft.currentVersion.generatedTitle}
                  reviewedTitle={titleInput}
                  originalDescription={activeDraft.currentVersion.generatedDescription}
                  reviewedDescription={descriptionInput}
                />
              )}

              {/* Editor de Título */}
              <div className="flex flex-col gap-2">
                <CharacterLimitGauge
                  currentCount={titleInput.trim().length}
                  minLimit={minTitle}
                  maxLimit={maxTitle}
                  label="Título do Anúncio"
                  officialSource={limits?.officialRuleSource}
                />
                <input
                  type="text"
                  aria-label="Título do anúncio"
                  value={titleInput}
                  onChange={(e) => setTitleInput(e.target.value)}
                  placeholder="Título do anúncio..."
                  className={`w-full p-3 rounded-lg border text-sm font-medium bg-slate-50 dark:bg-slate-800 transition-colors ${
                    isTitleValid
                      ? 'border-slate-200 dark:border-slate-700 focus:border-indigo-500'
                      : 'border-red-500 dark:border-red-600 focus:border-red-500'
                  }`}
                />
                {!isTitleValid && (
                  <span className="text-xs text-red-600 dark:text-red-400 font-medium">
                    O título deve possuir entre {minTitle} e {maxTitle} caracteres no {activeDraft.targetMarketplace}.
                  </span>
                )}
              </div>

              {/* Editor de Descrição */}
              <div className="flex flex-col gap-2">
                <CharacterLimitGauge
                  currentCount={descriptionInput.trim().length}
                  minLimit={minDesc}
                  maxLimit={maxDesc}
                  label="Descrição Completa (Texto Plano)"
                  officialSource={limits?.officialRuleSource}
                />
                <textarea
                  rows={10}
                  aria-label="Descrição do anúncio"
                  value={descriptionInput}
                  onChange={(e) => setDescriptionInput(e.target.value)}
                  placeholder="Descrição do anúncio..."
                  className={`w-full p-3 rounded-lg border text-xs font-mono bg-slate-50 dark:bg-slate-800 transition-colors leading-relaxed ${
                    isDescValid
                      ? 'border-slate-200 dark:border-slate-700 focus:border-indigo-500'
                      : 'border-red-500 dark:border-red-600 focus:border-red-500'
                  }`}
                />
                {!isDescValid && (
                  <span className="text-xs text-red-600 dark:text-red-400 font-medium">
                    A descrição deve possuir entre {minDesc} e {maxDesc} caracteres.
                  </span>
                )}
              </div>

              {/* Apontamentos do Operador (Opcional) */}
              <div className="flex flex-col gap-1.5">
                <label htmlFor="editor-review-notes" className="text-xs font-medium text-slate-700 dark:text-slate-300">
                  Notas de Revisão (Opcional)
                </label>
                <input
                  id="editor-review-notes"
                  aria-label="Notas de Revisão"
                  type="text"
                  value={reviewNotesInput}
                  onChange={(e) => setReviewNotesInput(e.target.value)}
                  placeholder="Ex: Ajustei o título para enfatizar aço inox e conferi dimensões..."
                  className="w-full p-2.5 rounded-lg border border-slate-200 dark:border-slate-700 text-xs bg-slate-50 dark:bg-slate-800"
                />
              </div>

              {/* Barra de Ações do Operador */}
              <div className="flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-slate-200 dark:border-slate-800">
                <div className="flex items-center gap-2">
                  <button
                    onClick={handleSaveEdit}
                    disabled={!canSave}
                    className="px-4 py-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white rounded-lg text-xs font-bold transition-colors min-h-[44px]"
                  >
                    Salvar Revisão (Nova Versão)
                  </button>

                  <button
                    onClick={handleApprove}
                    disabled={activeDraft.status === 'APPROVED' || isSaving}
                    className="px-4 py-2.5 bg-emerald-700 hover:bg-emerald-800 disabled:opacity-50 text-white rounded-lg text-xs font-bold transition-colors min-h-[44px]"
                  >
                    {activeDraft.status === 'APPROVED' ? '✓ Aprovado Localmente' : 'Aprovar Rascunho Local'}
                  </button>

                  <button
                    onClick={handleReject}
                    disabled={activeDraft.status === 'REJECTED' || isSaving}
                    className="px-3 py-2.5 border border-red-300 dark:border-red-800 text-red-700 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/40 rounded-lg text-xs font-semibold transition-colors min-h-[44px]"
                  >
                    Rejeitar
                  </button>
                </div>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => copyToClipboard(titleInput, 'Título')}
                    className="px-3 py-2 border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 text-xs font-medium rounded-lg min-h-[44px]"
                  >
                    Copiar Título
                  </button>
                  <button
                    onClick={() => copyToClipboard(descriptionInput, 'Descrição')}
                    className="px-3 py-2 border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 text-xs font-medium rounded-lg min-h-[44px]"
                  >
                    Copiar Descrição
                  </button>
                  <button
                    onClick={handleExportTxt}
                    className="px-3 py-2 border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 text-xs font-medium rounded-lg min-h-[44px]"
                  >
                    Exportar TXT
                  </button>
                  <button
                    onClick={handleExportJson}
                    className="px-3 py-2 border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 text-xs font-medium rounded-lg min-h-[44px]"
                  >
                    Exportar JSON
                  </button>
                </div>
              </div>
            </div>
          ) : (
            <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl p-12 text-center text-slate-600 dark:text-slate-400 text-sm">
              Selecione um rascunho na lista ou clique em &quot;+ Novo Rascunho&quot; para iniciar.
            </div>
          )}
        </div>
      </div>

      {/* Gaveta de Histórico de Versões */}
      {activeDraft && (
        <VersionHistoryDrawer
          isOpen={isHistoryDrawerOpen}
          onClose={() => setIsHistoryDrawerOpen(false)}
          versionHistory={activeDraft.versionHistory}
          currentVersionNumber={activeDraft.currentVersionNumber}
          onSelectVersion={(v: DraftListingVersionDto) => {
            setTitleInput(v.effectiveTitle);
            setDescriptionInput(v.effectiveDescription);
            setReviewNotesInput(v.reviewNotes || '');
          }}
        />
      )}

      {/* Modal de Criação de Novo Rascunho */}
      {isNewModalOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 backdrop-blur-sm p-4 overflow-y-auto"
          role="dialog"
          aria-modal="true"
          aria-label="Gerar Novo Rascunho com IA"
        >
          <div className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl w-full max-w-lg p-6 shadow-2xl flex flex-col gap-4 my-8">
            <div className="flex items-center justify-between border-b border-slate-200 dark:border-slate-800 pb-3">
              <h3 className="text-base font-bold text-slate-900 dark:text-slate-100">
                Novo Rascunho de Anúncio
              </h3>
              <button
                onClick={() => setIsNewModalOpen(false)}
                className="text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateDraft} className="flex flex-col gap-3 text-xs">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label htmlFor="modal-new-mkt" className="block text-slate-700 dark:text-slate-300 font-semibold mb-1">
                    Marketplace Alvo *
                  </label>
                  <select
                    id="modal-new-mkt"
                    aria-label="Marketplace Alvo"
                    value={newMkt}
                    onChange={(e) => setNewMkt(e.target.value)}
                    className="w-full p-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg"
                  >
                    <option value="MERCADO_LIVRE">Mercado Livre (Máx 60c)</option>
                    <option value="SHOPEE">Shopee (20 a 120c)</option>
                  </select>
                </div>

                <div>
                  <label htmlFor="modal-new-tone" className="block text-slate-700 dark:text-slate-300 font-semibold mb-1">
                    Tom de Redação
                  </label>
                  <select
                    id="modal-new-tone"
                    aria-label="Tom de Redação"
                    value={newTone}
                    onChange={(e) => setNewTone(e.target.value as DraftTone)}
                    className="w-full p-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg"
                  >
                    <option value="INFORMATIVE_OBJECTIVE">Informativo & Objetivo</option>
                    <option value="TECHNICAL">Técnico & Engenharia</option>
                    <option value="COMMERCIAL_POLITE">Comercial & Cortês</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label htmlFor="modal-new-brand" className="block text-slate-700 dark:text-slate-300 font-semibold mb-1">
                    Marca Confirmada
                  </label>
                  <input
                    id="modal-new-brand"
                    aria-label="Marca Confirmada"
                    type="text"
                    value={newBrand}
                    onChange={(e) => setNewBrand(e.target.value)}
                    placeholder="Ex: Tramontina, Bosch, Apple..."
                    className="w-full p-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg"
                  />
                </div>
                <div>
                  <label htmlFor="modal-new-model" className="block text-slate-700 dark:text-slate-300 font-semibold mb-1">
                    Modelo Confirmado
                  </label>
                  <input
                    id="modal-new-model"
                    aria-label="Modelo Confirmado"
                    type="text"
                    value={newModel}
                    onChange={(e) => setNewModel(e.target.value)}
                    placeholder="Ex: Solar, GaN5, Ultra..."
                    className="w-full p-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg"
                  />
                </div>
              </div>

              <div>
                <label htmlFor="modal-new-category" className="block text-slate-700 dark:text-slate-300 font-semibold mb-1">
                  Categoria
                </label>
                <input
                  id="modal-new-category"
                  aria-label="Categoria"
                  type="text"
                  value={newCategory}
                  onChange={(e) => setNewCategory(e.target.value)}
                  placeholder="Ex: Panelas de Cozinha, Parafusadeiras..."
                  className="w-full p-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg"
                />
              </div>

              <div>
                <label htmlFor="modal-new-materials" className="block text-slate-700 dark:text-slate-300 font-semibold mb-1">
                  Materiais Confirmados (separados por vírgula)
                </label>
                <input
                  id="modal-new-materials"
                  aria-label="Materiais Confirmados"
                  type="text"
                  value={newMaterials}
                  onChange={(e) => setNewMaterials(e.target.value)}
                  placeholder="Ex: Aço Inox 304, Vidro Temperado..."
                  className="w-full p-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label htmlFor="modal-new-dimensions" className="block text-slate-700 dark:text-slate-300 font-semibold mb-1">
                    Dimensões / Medidas
                  </label>
                  <input
                    id="modal-new-dimensions"
                    aria-label="Dimensões / Medidas"
                    type="text"
                    value={newDimensions}
                    onChange={(e) => setNewDimensions(e.target.value)}
                    placeholder="Ex: 20cm diâmetro, 500g..."
                    className="w-full p-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg"
                  />
                </div>
                <div>
                  <label htmlFor="modal-new-warranty" className="block text-slate-700 dark:text-slate-300 font-semibold mb-1">
                    Garantia
                  </label>
                  <input
                    id="modal-new-warranty"
                    aria-label="Garantia"
                    type="text"
                    value={newWarranty}
                    onChange={(e) => setNewWarranty(e.target.value)}
                    placeholder="Ex: 90 dias legal conforme CDC..."
                    className="w-full p-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg"
                  />
                </div>
              </div>

              <div>
                <label htmlFor="modal-new-keywords" className="block text-slate-700 dark:text-slate-300 font-semibold mb-1">
                  Palavras-Chave de Busca (separadas por vírgula)
                </label>
                <input
                  id="modal-new-keywords"
                  aria-label="Palavras-Chave de Busca"
                  type="text"
                  value={newKeywords}
                  onChange={(e) => setNewKeywords(e.target.value)}
                  placeholder="Ex: cozinha, indução, resistente, durável..."
                  className="w-full p-2 bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg"
                />
              </div>

              <div className="bg-slate-100 dark:bg-slate-800/60 p-2.5 rounded-lg text-[11px] text-slate-600 dark:text-slate-400 italic mt-1">
                Guardrail de Segurança: O motor de IA só utilizará os dados acima. Certificações não comprovadas e superlativos enganosos serão bloqueados.
              </div>

              <div className="flex items-center justify-end gap-2 pt-3 border-t border-slate-200 dark:border-slate-800 mt-2">
                <button
                  type="button"
                  onClick={() => setIsNewModalOpen(false)}
                  className="px-4 py-2 border border-slate-200 dark:border-slate-700 rounded-lg font-medium hover:bg-slate-100 dark:hover:bg-slate-800 min-h-[44px]"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={isSaving}
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white rounded-lg font-bold min-h-[44px]"
                >
                  {isSaving ? 'Gerando...' : 'Gerar Rascunho com IA'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
