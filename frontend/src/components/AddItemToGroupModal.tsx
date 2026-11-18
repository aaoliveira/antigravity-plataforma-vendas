'use client';

import React, { useState, useEffect } from 'react';
import { NormalizedSearchResultItem } from './ProductCompareModal';

export interface MonitoredItemDto {
  id: string;
  tenantId: string;
  groupId: string;
  marketplace: string;
  externalListingId: string;
  title: string;
  sellerId?: string | null;
  sellerName?: string | null;
  sourceUrl?: string | null;
  lastPrice?: number | null;
  currency?: string | null;
  status: 'ACTIVE' | 'PAUSED' | 'DELETED';
  firstSnapshotAt?: string | null;
  lastSnapshotAt?: string | null;
  notes?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface AddItemToGroupModalProps {
  isOpen: boolean;
  groupId: string;
  groupName: string;
  onClose: () => void;
  onSuccess: (newItem: MonitoredItemDto) => void;
  preselectedSearchItem?: NormalizedSearchResultItem | null;
}

export function AddItemToGroupModal({
  isOpen,
  groupId,
  groupName,
  onClose,
  onSuccess,
  preselectedSearchItem,
}: AddItemToGroupModalProps) {
  const [activeTab, setActiveTab] = useState<'search' | 'direct'>('search');

  // Search Tab State
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<NormalizedSearchResultItem[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [searchError, setSearchError] = useState<string | null>(null);

  // Direct Tab State
  const [marketplace, setMarketplace] = useState<'MERCADO_LIVRE' | 'SHOPEE'>('MERCADO_LIVRE');
  const [externalListingId, setExternalListingId] = useState('');
  const [title, setTitle] = useState('');
  const [sellerName, setSellerName] = useState('');
  const [sourceUrl, setSourceUrl] = useState('');
  const [notes, setNotes] = useState('');

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  useEffect(() => {
    if (preselectedSearchItem) {
      setActiveTab('direct');
      setMarketplace(preselectedSearchItem.marketplace === 'SHOPEE' ? 'SHOPEE' : 'MERCADO_LIVRE');
      setExternalListingId(preselectedSearchItem.itemId);
      setTitle(preselectedSearchItem.title);
      setSellerName(preselectedSearchItem.sellerName || '');
      setSourceUrl(preselectedSearchItem.listingUrl || '');
    } else {
      setSearchQuery('');
      setSearchResults([]);
      setExternalListingId('');
      setTitle('');
      setSellerName('');
      setSourceUrl('');
      setNotes('');
    }
    setSearchError(null);
    setSubmitError(null);
  }, [preselectedSearchItem, isOpen]);

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

  const handleExecuteSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!searchQuery.trim()) return;

    setIsSearching(true);
    setSearchError(null);

    try {
      const res = await fetch(`/api/market-search?q=${encodeURIComponent(searchQuery.trim())}&size=8`);
      if (!res.ok) {
        throw new Error('Falha ao buscar anúncios de mercado.');
      }
      const data = await res.json();
      setSearchResults(data.items || []);
    } catch (err: unknown) {
      setSearchError(err instanceof Error ? err.message : 'Erro na pesquisa.');
    } finally {
      setIsSearching(false);
    }
  };

  const handleSelectFromSearch = (item: NormalizedSearchResultItem) => {
    setMarketplace(item.marketplace === 'SHOPEE' ? 'SHOPEE' : 'MERCADO_LIVRE');
    setExternalListingId(item.itemId);
    setTitle(item.title);
    setSellerName(item.sellerName || '');
    setSourceUrl(item.listingUrl || '');
    setActiveTab('direct');
  };

  const handleSubmitDirect = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!externalListingId.trim()) {
      setSubmitError('O ID do anúncio externo é obrigatório.');
      return;
    }
    if (!title.trim()) {
      setSubmitError('O título do anúncio é obrigatório.');
      return;
    }

    setIsSubmitting(true);
    setSubmitError(null);

    try {
      const res = await fetch(`/api/monitoring/groups/${groupId}/items`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          marketplace,
          externalListingId: externalListingId.trim(),
          title: title.trim(),
          sellerName: sellerName.trim() || null,
          sourceUrl: sourceUrl.trim() || null,
          notes: notes.trim() || null,
        }),
      });

      if (res.status === 409) {
        setSubmitError('Este anúncio já está cadastrado neste grupo de monitoramento.');
        setIsSubmitting(false);
        return;
      }

      if (res.status === 422) {
        setSubmitError('Fonte não homologada para monitoramento conforme as políticas oficiais.');
        setIsSubmitting(false);
        return;
      }

      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        setSubmitError(err.message || 'Falha ao adicionar item ao grupo.');
        setIsSubmitting(false);
        return;
      }

      const created = await res.json();
      onSuccess(created);
      onClose();
    } catch {
      setSubmitError('Erro de comunicação ao adicionar item ao monitoramento.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="add-item-modal-title"
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-fade-in"
    >
      <div className="relative w-full max-w-2xl bg-white dark:bg-slate-900 rounded-2xl shadow-2xl border border-slate-200 dark:border-slate-800 p-6 space-y-4 max-h-[90vh] overflow-y-auto">
        {/* Header */}
        <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800">
          <div>
            <h2 id="add-item-modal-title" className="text-base font-bold text-slate-900 dark:text-white">
              Adicionar Anúncio ao Monitoramento
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Grupo destino:{' '}
              <span className="font-semibold text-slate-900 dark:text-white">
                {groupName}
              </span>
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

        {/* Abas: Pesquisa Autorizada vs Inserção Direta */}
        <div className="flex border-b border-slate-200 dark:border-slate-800">
          <button
            type="button"
            onClick={() => setActiveTab('search')}
            className={`px-4 py-2.5 text-xs font-semibold border-b-2 transition-colors min-h-[44px] ${
              activeTab === 'search'
                ? 'border-blue-600 text-blue-600 dark:border-blue-400 dark:text-blue-400'
                : 'border-transparent text-slate-500 hover:text-slate-700 dark:hover:text-slate-300'
            }`}
          >
            🔍 Pesquisar no Mercado
          </button>
          <button
            type="button"
            onClick={() => setActiveTab('direct')}
            className={`px-4 py-2.5 text-xs font-semibold border-b-2 transition-colors min-h-[44px] ${
              activeTab === 'direct'
                ? 'border-blue-600 text-blue-600 dark:border-blue-400 dark:text-blue-400'
                : 'border-transparent text-slate-500 hover:text-slate-700 dark:hover:text-slate-300'
            }`}
          >
            ✏️ Inserção Direta de Anúncio
          </button>
        </div>

        {/* Conteúdo Aba: Pesquisar no Mercado */}
        {activeTab === 'search' && (
          <div className="space-y-4">
            <form onSubmit={handleExecuteSearch} className="flex gap-2">
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Buscar por termo ou modelo (ex: teclado mecânico, fone bluetooth)..."
                className="flex-1 px-3 py-2 text-xs rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 min-h-[44px]"
              />
              <button
                type="submit"
                disabled={isSearching || !searchQuery.trim()}
                className="px-4 py-2 text-xs font-semibold text-white bg-blue-600 hover:bg-blue-700 rounded-xl transition-colors min-h-[44px] disabled:opacity-50 flex items-center gap-1.5"
              >
                {isSearching ? 'Buscando...' : 'Pesquisar'}
              </button>
            </form>

            {searchError && (
              <div className="p-3 bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 text-xs text-rose-800 dark:text-rose-300 rounded-xl">
                {searchError}
              </div>
            )}

            {/* Resultados da Pesquisa */}
            <div className="space-y-2 max-h-72 overflow-y-auto">
              {searchResults.length === 0 && !isSearching && (
                <div className="text-center py-8 text-xs text-slate-400">
                  Digite um termo acima para localizar anúncios públicos autorizados para monitorar.
                </div>
              )}

              {searchResults.map((item) => (
                <div
                  key={item.itemId}
                  className="p-3 rounded-xl border border-slate-200 dark:border-slate-800 hover:border-blue-300 dark:hover:border-blue-700 bg-slate-50/50 dark:bg-slate-800/40 flex items-center justify-between gap-3 transition-colors"
                >
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="px-1.5 py-0.5 rounded text-[10px] font-bold bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-300">
                        {item.marketplace}
                      </span>
                      <span className="text-xs font-semibold text-slate-900 dark:text-white truncate">
                        {item.title}
                      </span>
                    </div>
                    <div className="text-[11px] text-slate-500 dark:text-slate-400 mt-1 flex items-center gap-3">
                      <span>ID: {item.itemId}</span>
                      {item.price != null && (
                        <span className="font-semibold text-slate-900 dark:text-white">
                          {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: item.currency || 'BRL' }).format(Number(item.price))}
                        </span>
                      )}
                      {item.sellerName && <span>Vendedor: {item.sellerName}</span>}
                    </div>
                  </div>

                  <button
                    type="button"
                    onClick={() => handleSelectFromSearch(item)}
                    className="px-3 py-1.5 min-h-[44px] text-xs font-semibold text-blue-600 dark:text-blue-400 hover:bg-blue-50 dark:hover:bg-blue-950/50 rounded-xl transition-colors whitespace-nowrap"
                  >
                    ➕ Selecionar
                  </button>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Conteúdo Aba: Inserção Direta */}
        {activeTab === 'direct' && (
          <form onSubmit={handleSubmitDirect} className="space-y-4">
            {submitError && (
              <div
                role="alert"
                className="p-3 bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 text-xs text-rose-800 dark:text-rose-300 rounded-xl"
              >
                {submitError}
              </div>
            )}

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label htmlFor="item-marketplace" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Marketplace / Canal *
                </label>
                <select
                  id="item-marketplace"
                  value={marketplace}
                  onChange={(e) => setMarketplace(e.target.value as 'MERCADO_LIVRE' | 'SHOPEE')}
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 min-h-[44px]"
                >
                  <option value="MERCADO_LIVRE">Mercado Livre</option>
                  <option value="SHOPEE">Shopee</option>
                </select>
              </div>

              <div>
                <label htmlFor="item-external-id" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  ID do Anúncio Externo *
                </label>
                <input
                  id="item-external-id"
                  type="text"
                  required
                  value={externalListingId}
                  onChange={(e) => setExternalListingId(e.target.value)}
                  placeholder="Ex: MLB1234567890 ou 100293847"
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 min-h-[44px]"
                />
              </div>
            </div>

            <div>
              <label htmlFor="item-title" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Título do Anúncio *
              </label>
              <input
                id="item-title"
                type="text"
                required
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Ex: Fone De Ouvido Bluetooth Sem Fio Cancelamento De Ruído"
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 min-h-[44px]"
              />
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label htmlFor="item-seller" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Nome do Vendedor / Loja (opcional)
                </label>
                <input
                  id="item-seller"
                  type="text"
                  value={sellerName}
                  onChange={(e) => setSellerName(e.target.value)}
                  placeholder="Ex: Loja Oficial Tech, Vendedor Concorrente"
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 min-h-[44px]"
                />
              </div>

              <div>
                <label htmlFor="item-url" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  URL Oficial da Fonte (opcional)
                </label>
                <input
                  id="item-url"
                  type="url"
                  value={sourceUrl}
                  onChange={(e) => setSourceUrl(e.target.value)}
                  placeholder="https://produto.mercadolivre.com.br/MLB-..."
                  className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 min-h-[44px]"
                />
              </div>
            </div>

            <div>
              <label htmlFor="item-notes" className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                Anotações Internas (opcional)
              </label>
              <textarea
                id="item-notes"
                rows={2}
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                placeholder="Ex: Principal concorrente em preço na região Sudeste..."
                className="w-full px-3 py-2 text-xs rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500"
              />
            </div>

            {/* Aviso Anti-Retroatividade */}
            <div className="p-3 bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-900/50 rounded-xl text-xs text-blue-900 dark:text-blue-200">
              <span className="font-semibold block">⏱️ Histórico Não-Retroativo:</span>
              <p className="text-[11px] mt-0.5 leading-relaxed">
                As observações históricas de preço terão início apenas no primeiro snapshot efetivo pós-inclusão.
                A plataforma nunca fabricará dados retroativos anteriores ao cadastro.
              </p>
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
                    <span>Cadastrando...</span>
                  </>
                ) : (
                  <span>Adicionar ao Monitoramento</span>
                )}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
