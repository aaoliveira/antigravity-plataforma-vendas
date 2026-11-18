'use client';

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { LoadingSkeleton } from './LoadingSkeleton';
import { EmptyState } from './EmptyState';
import { CreateGroupModal, MonitoringGroupItem } from './CreateGroupModal';
import { AddItemToGroupModal, MonitoredItemDto } from './AddItemToGroupModal';
import { ConfirmDeleteModal } from './ConfirmDeleteModal';
import { MonitoringTimelineDrawer } from './MonitoringTimelineDrawer';

export interface ProductMonitoringViewProps {
  onOpenMarketSearch?: () => void;
}

export function ProductMonitoringView({ onOpenMarketSearch }: ProductMonitoringViewProps) {
  // Dados de Grupos
  const [groups, setGroups] = useState<MonitoringGroupItem[]>([]);
  const [isLoadingGroups, setIsLoadingGroups] = useState(true);
  const [groupSearch, setGroupSearch] = useState('');
  const [groupStatusFilter, setGroupStatusFilter] = useState<'ALL' | 'ACTIVE' | 'PAUSED'>('ALL');

  // Grupo Selecionado (Visão Detalhada)
  const [selectedGroup, setSelectedGroup] = useState<MonitoringGroupItem | null>(null);

  // Itens do Grupo Selecionado
  const [items, setItems] = useState<MonitoredItemDto[]>([]);
  const [isLoadingItems, setIsLoadingItems] = useState(false);
  const [itemSearch, setItemSearch] = useState('');
  const [itemMarketplaceFilter, setItemMarketplaceFilter] = useState<'ALL' | 'MERCADO_LIVRE' | 'SHOPEE'>('ALL');
  const [itemStatusFilter, setItemStatusFilter] = useState<'ALL' | 'ACTIVE' | 'PAUSED'>('ALL');
  const [itemViewMode, setItemViewMode] = useState<'table' | 'cards'>('table');

  // Modais de Controle
  const [isCreateGroupModalOpen, setIsCreateGroupModalOpen] = useState(false);
  const [groupToEdit, setGroupToEdit] = useState<MonitoringGroupItem | null>(null);

  const [isAddItemModalOpen, setIsAddItemModalOpen] = useState(false);

  // Confirmação de Exclusão
  const [deleteTarget, setDeleteTarget] = useState<{
    type: 'GROUP' | 'ITEM';
    id: string;
    name: string;
    groupId?: string;
  } | null>(null);
  const [isDeleting, setIsDeleting] = useState(false);

  // Drawer de Timeline
  const [selectedItemForTimeline, setSelectedItemForTimeline] = useState<MonitoredItemDto | null>(null);

  // Erro Geral
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // =========================================================================
  // CARREGAMENTO DE GRUPOS
  // =========================================================================
  const fetchGroups = useCallback(async () => {
    setIsLoadingGroups(true);
    setErrorMessage(null);

    const params = new URLSearchParams();
    if (groupSearch.trim()) params.set('search', groupSearch.trim());
    params.set('page', '0');
    params.set('size', '50');

    try {
      const res = await fetch(`/api/monitoring/groups?${params.toString()}`);
      if (!res.ok) {
        throw new Error('Falha ao listar grupos de monitoramento.');
      }
      const data = await res.json();
      setGroups(data.items || []);
    } catch (err: unknown) {
      setErrorMessage(err instanceof Error ? err.message : 'Erro ao buscar grupos.');
    } finally {
      setIsLoadingGroups(false);
    }
  }, [groupSearch]);

  useEffect(() => {
    fetchGroups();
  }, [fetchGroups]);

  // =========================================================================
  // CARREGAMENTO DE ITENS DO GRUPO SELECIONADO
  // =========================================================================
  const fetchItemsInGroup = useCallback(async (groupId: string) => {
    setIsLoadingItems(true);
    setErrorMessage(null);

    try {
      const res = await fetch(`/api/monitoring/groups/${groupId}/items?size=100`);
      if (!res.ok) {
        throw new Error('Falha ao listar itens do grupo.');
      }
      const data = await res.json();
      setItems(data.items || []);
    } catch (err: unknown) {
      setErrorMessage(err instanceof Error ? err.message : 'Erro ao buscar itens do grupo.');
    } finally {
      setIsLoadingItems(false);
    }
  }, []);

  useEffect(() => {
    if (selectedGroup) {
      fetchItemsInGroup(selectedGroup.id);
    } else {
      setItems([]);
    }
  }, [selectedGroup, fetchItemsInGroup]);

  // =========================================================================
  // EXCLUSÃO COM CONFIRMAÇÃO
  // =========================================================================
  const handleConfirmDelete = async () => {
    if (!deleteTarget) return;
    setIsDeleting(true);

    try {
      if (deleteTarget.type === 'GROUP') {
        const res = await fetch(`/api/monitoring/groups/${deleteTarget.id}`, { method: 'DELETE' });
        if (!res.ok) throw new Error('Falha ao remover grupo.');
        setDeleteTarget(null);
        if (selectedGroup?.id === deleteTarget.id) {
          setSelectedGroup(null);
        }
        await fetchGroups();
      } else if (deleteTarget.type === 'ITEM' && deleteTarget.groupId) {
        const res = await fetch(
          `/api/monitoring/groups/${deleteTarget.groupId}/items/${deleteTarget.id}`,
          { method: 'DELETE' }
        );
        if (!res.ok) throw new Error('Falha ao remover item.');
        setDeleteTarget(null);
        await fetchItemsInGroup(deleteTarget.groupId);
        await fetchGroups(); // Atualiza contagem de itens
      }
    } catch (err: unknown) {
      setErrorMessage(err instanceof Error ? err.message : 'Erro ao excluir.');
    } finally {
      setIsDeleting(false);
    }
  };

  // Alternar Status do Grupo (Pausar / Ativar)
  const handleToggleGroupStatus = async (group: MonitoringGroupItem) => {
    const newStatus = group.status === 'ACTIVE' ? 'PAUSED' : 'ACTIVE';
    try {
      const res = await fetch(`/api/monitoring/groups/${group.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: group.name,
          description: group.description,
          color: group.color,
          minIntervalMinutes: group.minIntervalMinutes,
          retentionDays: group.retentionDays,
          status: newStatus,
        }),
      });
      if (!res.ok) throw new Error('Falha ao atualizar status do grupo.');
      const updated = await res.json();
      setGroups((prev) => prev.map((g) => (g.id === updated.id ? updated : g)));
      if (selectedGroup?.id === updated.id) {
        setSelectedGroup(updated);
      }
    } catch (err: unknown) {
      setErrorMessage(err instanceof Error ? err.message : 'Erro ao alternar status do grupo.');
    }
  };

  // =========================================================================
  // FILTRAGEM DE GRUPOS E ITENS
  // =========================================================================
  const filteredGroups = useMemo(() => {
    return groups.filter((g) => {
      if (groupStatusFilter !== 'ALL' && g.status !== groupStatusFilter) return false;
      return true;
    });
  }, [groups, groupStatusFilter]);

  const filteredItems = useMemo(() => {
    return items.filter((item) => {
      if (itemMarketplaceFilter !== 'ALL' && item.marketplace !== itemMarketplaceFilter) return false;
      if (itemStatusFilter !== 'ALL' && item.status !== itemStatusFilter) return false;
      if (itemSearch.trim()) {
        const term = itemSearch.toLowerCase();
        const matchesTitle = item.title.toLowerCase().includes(term);
        const matchesId = item.externalListingId.toLowerCase().includes(term);
        const matchesSeller = item.sellerName?.toLowerCase().includes(term);
        if (!matchesTitle && !matchesId && !matchesSeller) return false;
      }
      return true;
    });
  }, [items, itemMarketplaceFilter, itemStatusFilter, itemSearch]);

  const formatCurrency = (val: number | null | undefined, cur = 'BRL') => {
    if (val == null) return '---';
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: cur }).format(val);
  };

  const formatDate = (dateStr: string | null | undefined) => {
    if (!dateStr) return '---';
    try {
      return new Date(dateStr).toLocaleString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      });
    } catch {
      return dateStr;
    }
  };

  // KPIs
  const totalMonitoredItems = groups.reduce((acc, g) => acc + (g.itemCount || 0), 0);
  const activeGroupsCount = groups.filter((g) => g.status === 'ACTIVE').length;

  return (
    <div className="space-y-6 animate-fade-in" data-testid="product-monitoring-view">
      {/* =========================================================================
          CABEÇALHO PRINCIPAL DO MÓDULO
      ========================================================================= */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-200 dark:border-slate-800">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-xl">🎯</span>
            <h1 className="text-lg font-bold text-slate-900 dark:text-white">
              Monitoramento de Produtos & Concorrentes
            </h1>
          </div>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Gestão de grupos e rastreamento contínuo de preços e disponibilidade com conformidade oficial.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          {onOpenMarketSearch && (
            <button
              type="button"
              onClick={onOpenMarketSearch}
              className="px-3.5 py-2 min-h-[44px] text-xs font-semibold rounded-xl border border-slate-300 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300 transition-colors flex items-center gap-1.5"
            >
              🔍 Pesquisar no Mercado
            </button>
          )}

          <button
            type="button"
            onClick={() => {
              setGroupToEdit(null);
              setIsCreateGroupModalOpen(true);
            }}
            className="px-4 py-2 min-h-[44px] text-xs font-semibold rounded-xl bg-blue-600 hover:bg-blue-700 text-white transition-colors shadow-sm flex items-center gap-1.5"
          >
            ➕ Novo Grupo
          </button>
        </div>
      </div>

      {/* Alerta Geral de Erro */}
      {errorMessage && (
        <div
          role="alert"
          className="p-3 bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-800 text-xs text-rose-800 dark:text-rose-300 rounded-xl flex items-center justify-between"
        >
          <span>{errorMessage}</span>
          <button
            type="button"
            onClick={() => setErrorMessage(null)}
            className="p-1 hover:text-rose-900 font-bold min-h-[44px] min-w-[44px] flex items-center justify-center"
          >
            ✕
          </button>
        </div>
      )}

      {/* =========================================================================
          SEÇÃO 1: VISÃO GERAL DE GRUPOS (Quando nenhum grupo está selecionado)
      ========================================================================= */}
      {!selectedGroup ? (
        <div className="space-y-6">
          {/* KPI Cards de Monitoramento */}
          <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
              <span className="text-xs font-medium text-slate-500 dark:text-slate-400">Total de Grupos</span>
              <div className="mt-1 flex items-baseline justify-between">
                <span className="text-2xl font-bold text-slate-900 dark:text-white">{groups.length}</span>
                <span className="text-xs text-slate-400">Pastas</span>
              </div>
            </div>

            <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
              <span className="text-xs font-medium text-emerald-600 dark:text-emerald-400">Grupos Ativos</span>
              <div className="mt-1 flex items-baseline justify-between">
                <span className="text-2xl font-bold text-emerald-700 dark:text-emerald-400">{activeGroupsCount}</span>
                <span className="text-xs text-emerald-600/70">Coletas em execução</span>
              </div>
            </div>

            <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
              <span className="text-xs font-medium text-blue-600 dark:text-blue-400">Itens Monitorados</span>
              <div className="mt-1 flex items-baseline justify-between">
                <span className="text-2xl font-bold text-blue-700 dark:text-blue-400">{totalMonitoredItems}</span>
                <span className="text-xs text-blue-600/70">Anúncios</span>
              </div>
            </div>

            <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
              <span className="text-xs font-medium text-purple-600 dark:text-purple-400">Anti-Retroatividade</span>
              <div className="mt-1 flex items-baseline justify-between">
                <span className="text-sm font-bold text-purple-700 dark:text-purple-400">100% Auditável</span>
                <span className="text-[10px] text-purple-600/70">Zero fabricação</span>
              </div>
            </div>
          </div>

          {/* Barra de Filtros e Busca de Grupos */}
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 p-3 bg-slate-50 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-800 rounded-xl text-xs">
            <div className="flex-1 flex items-center gap-2">
              <input
                type="text"
                value={groupSearch}
                onChange={(e) => setGroupSearch(e.target.value)}
                placeholder="Filtrar grupos por nome ou descrição..."
                className="w-full max-w-sm px-3 py-2 text-xs rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 min-h-[44px]"
              />
              {groupSearch && (
                <button
                  type="button"
                  onClick={() => setGroupSearch('')}
                  className="px-2 py-1 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 min-h-[44px]"
                >
                  Limpar
                </button>
              )}
            </div>

            <div className="flex items-center gap-2">
              <span className="font-semibold text-slate-600 dark:text-slate-400">Status:</span>
              <div className="flex rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 p-0.5">
                {(['ALL', 'ACTIVE', 'PAUSED'] as const).map((st) => (
                  <button
                    key={st}
                    type="button"
                    onClick={() => setGroupStatusFilter(st)}
                    className={`px-3 py-1.5 rounded-md text-xs font-medium transition-colors min-h-[38px] ${
                      groupStatusFilter === st
                        ? 'bg-blue-600 text-white font-semibold'
                        : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                    }`}
                  >
                    {st === 'ALL' ? 'Todos' : st === 'ACTIVE' ? 'Ativos' : 'Pausados'}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Listagem de Grupos */}
          {isLoadingGroups ? (
            <LoadingSkeleton rows={4} />
          ) : filteredGroups.length === 0 ? (
            <EmptyState
              title="Nenhum grupo de monitoramento encontrado"
              description="Crie pastas ou grupos temáticos para monitorar anúncios próprios e da concorrência com coletas agendadas."
              actionLabel="Criar Primeiro Grupo"
              onAction={() => {
                setGroupToEdit(null);
                setIsCreateGroupModalOpen(true);
              }}
            />
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {filteredGroups.map((group) => (
                <div
                  key={group.id}
                  className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm hover:shadow-md transition-all flex flex-col justify-between space-y-4"
                >
                  <div className="space-y-2">
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-center gap-2">
                        <span
                          className="w-3.5 h-3.5 rounded-full inline-block"
                          style={{ backgroundColor: group.color || '#3B82F6' }}
                        />
                        <h3 className="text-sm font-bold text-slate-900 dark:text-white line-clamp-1">
                          {group.name}
                        </h3>
                      </div>

                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          group.status === 'ACTIVE'
                            ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/60 dark:text-emerald-300'
                            : 'bg-amber-100 text-amber-800 dark:bg-amber-900/60 dark:text-amber-300'
                        }`}
                      >
                        {group.status === 'ACTIVE' ? '🟢 Ativo' : '⏸️ Pausado'}
                      </span>
                    </div>

                    {group.description && (
                      <p className="text-xs text-slate-500 dark:text-slate-400 line-clamp-2 leading-relaxed">
                        {group.description}
                      </p>
                    )}

                    <div className="grid grid-cols-2 gap-2 pt-2 text-[11px] text-slate-500 dark:text-slate-400 border-t border-slate-100 dark:border-slate-800">
                      <div>
                        <span>Intervalo: </span>
                        <span className="font-semibold text-slate-700 dark:text-slate-300">
                          {group.minIntervalMinutes} min
                        </span>
                      </div>
                      <div>
                        <span>Retenção: </span>
                        <span className="font-semibold text-slate-700 dark:text-slate-300">
                          {group.retentionDays} dias
                        </span>
                      </div>
                    </div>
                  </div>

                  <div className="pt-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
                    <span className="text-xs font-semibold text-blue-600 dark:text-blue-400">
                      {group.itemCount || 0} {(group.itemCount || 0) === 1 ? 'item' : 'itens'}
                    </span>

                    <div className="flex items-center gap-1">
                      <button
                        type="button"
                        onClick={() => handleToggleGroupStatus(group)}
                        title={group.status === 'ACTIVE' ? 'Pausar monitoramento' : 'Retomar monitoramento'}
                        className="p-2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 min-h-[44px] min-w-[44px] flex items-center justify-center rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
                      >
                        {group.status === 'ACTIVE' ? '⏸️' : '▶️'}
                      </button>

                      <button
                        type="button"
                        onClick={() => {
                          setGroupToEdit(group);
                          setIsCreateGroupModalOpen(true);
                        }}
                        title="Editar grupo"
                        className="p-2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 min-h-[44px] min-w-[44px] flex items-center justify-center rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
                      >
                        ✏️
                      </button>

                      <button
                        type="button"
                        onClick={() =>
                          setDeleteTarget({
                            type: 'GROUP',
                            id: group.id,
                            name: group.name,
                          })
                        }
                        title="Remover grupo"
                        className="p-2 text-rose-400 hover:text-rose-600 min-h-[44px] min-w-[44px] flex items-center justify-center rounded-lg hover:bg-rose-50 dark:hover:bg-rose-950/40 transition-colors"
                      >
                        🗑️
                      </button>

                      <button
                        type="button"
                        onClick={() => setSelectedGroup(group)}
                        className="ml-1 px-3 py-1.5 min-h-[44px] text-xs font-semibold rounded-xl bg-blue-50 dark:bg-blue-950/50 text-blue-600 dark:text-blue-300 hover:bg-blue-100 dark:hover:bg-blue-900/60 transition-colors flex items-center gap-1"
                      >
                        <span>Abrir</span>
                        <span>→</span>
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      ) : (
        /* =========================================================================
            SEÇÃO 2: VISÃO DETALHADA DO GRUPO SELECIONADO
        ========================================================================= */
        <div className="space-y-6">
          {/* Header do Grupo com Botão Voltar */}
          <div className="p-5 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm space-y-4">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div className="flex items-center gap-3">
                <button
                  type="button"
                  onClick={() => setSelectedGroup(null)}
                  className="px-3 py-1.5 min-h-[44px] text-xs font-semibold text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-xl transition-colors flex items-center gap-1.5"
                >
                  <span>←</span>
                  <span>Todos os Grupos</span>
                </button>

                <div className="h-5 w-[1px] bg-slate-200 dark:bg-slate-700" />

                <div className="flex items-center gap-2">
                  <span
                    className="w-4 h-4 rounded-full"
                    style={{ backgroundColor: selectedGroup.color || '#3B82F6' }}
                  />
                  <h2 className="text-base font-bold text-slate-900 dark:text-white">
                    {selectedGroup.name}
                  </h2>
                </div>

                <span
                  className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                    selectedGroup.status === 'ACTIVE'
                      ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/60 dark:text-emerald-300'
                      : 'bg-amber-100 text-amber-800 dark:bg-amber-900/60 dark:text-amber-300'
                  }`}
                >
                  {selectedGroup.status === 'ACTIVE' ? '🟢 Ativo' : '⏸️ Pausado'}
                </span>
              </div>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setIsAddItemModalOpen(true)}
                  className="px-4 py-2 min-h-[44px] text-xs font-semibold rounded-xl bg-blue-600 hover:bg-blue-700 text-white transition-colors shadow-sm flex items-center gap-1.5"
                >
                  ➕ Adicionar Anúncio
                </button>

                <button
                  type="button"
                  onClick={() => {
                    setGroupToEdit(selectedGroup);
                    setIsCreateGroupModalOpen(true);
                  }}
                  className="px-3 py-2 min-h-[44px] text-xs font-semibold rounded-xl border border-slate-300 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300 transition-colors"
                >
                  Configurações
                </button>
              </div>
            </div>

            {selectedGroup.description && (
              <p className="text-xs text-slate-600 dark:text-slate-400 leading-relaxed">
                {selectedGroup.description}
              </p>
            )}

            <div className="flex flex-wrap items-center gap-6 text-xs text-slate-500 dark:text-slate-400 pt-2 border-t border-slate-100 dark:border-slate-800">
              <div>
                <span>Itens monitorados: </span>
                <span className="font-semibold text-slate-800 dark:text-slate-200">
                  {items.length} anúncios
                </span>
              </div>
              <div>
                <span>Janela mínima de coleta: </span>
                <span className="font-semibold text-slate-800 dark:text-slate-200">
                  {selectedGroup.minIntervalMinutes} minutos
                </span>
              </div>
              <div>
                <span>Retenção histórica: </span>
                <span className="font-semibold text-slate-800 dark:text-slate-200">
                  {selectedGroup.retentionDays} dias
                </span>
              </div>
              <div>
                <span>Criado em: </span>
                <span className="font-semibold text-slate-800 dark:text-slate-200">
                  {formatDate(selectedGroup.createdAt)}
                </span>
              </div>
            </div>
          </div>

          {/* Barra de Filtros de Itens */}
          <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-3 p-3 bg-slate-50 dark:bg-slate-800/40 border border-slate-200 dark:border-slate-800 rounded-xl text-xs">
            <div className="flex-1 flex items-center gap-2">
              <input
                type="text"
                value={itemSearch}
                onChange={(e) => setItemSearch(e.target.value)}
                placeholder="Buscar por título, ID ou vendedor..."
                className="w-full max-w-sm px-3 py-2 text-xs rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white focus:ring-2 focus:ring-blue-500 min-h-[44px]"
              />
              {itemSearch && (
                <button
                  type="button"
                  onClick={() => setItemSearch('')}
                  className="px-2 py-1 text-slate-400 hover:text-slate-600 min-h-[44px]"
                >
                  Limpar
                </button>
              )}
            </div>

            <div className="flex flex-wrap items-center gap-3">
              {/* Filtro Canal */}
              <div className="flex items-center gap-1.5">
                <span className="text-slate-500">Canal:</span>
                <select
                  value={itemMarketplaceFilter}
                  onChange={(e) =>
                    setItemMarketplaceFilter(e.target.value as 'ALL' | 'MERCADO_LIVRE' | 'SHOPEE')
                  }
                  className="px-2.5 py-1.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs min-h-[40px]"
                >
                  <option value="ALL">Todos os Canais</option>
                  <option value="MERCADO_LIVRE">Mercado Livre</option>
                  <option value="SHOPEE">Shopee</option>
                </select>
              </div>

              {/* Filtro Status */}
              <div className="flex items-center gap-1.5">
                <span className="text-slate-500">Status:</span>
                <select
                  value={itemStatusFilter}
                  onChange={(e) =>
                    setItemStatusFilter(e.target.value as 'ALL' | 'ACTIVE' | 'PAUSED')
                  }
                  className="px-2.5 py-1.5 rounded-lg border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-xs min-h-[40px]"
                >
                  <option value="ALL">Todos os Status</option>
                  <option value="ACTIVE">Ativo</option>
                  <option value="PAUSED">Pausado</option>
                </select>
              </div>

              {/* Alternador Tabela / Cards */}
              <div className="flex rounded-lg border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 p-0.5">
                <button
                  type="button"
                  onClick={() => setItemViewMode('table')}
                  aria-label="Visualização em tabela"
                  className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors min-h-[38px] ${
                    itemViewMode === 'table'
                      ? 'bg-blue-600 text-white'
                      : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                  }`}
                >
                  Tabela
                </button>
                <button
                  type="button"
                  onClick={() => setItemViewMode('cards')}
                  aria-label="Visualização em cards"
                  className={`px-3 py-1.5 rounded-md text-xs font-semibold transition-colors min-h-[38px] ${
                    itemViewMode === 'cards'
                      ? 'bg-blue-600 text-white'
                      : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                  }`}
                >
                  Cards
                </button>
              </div>
            </div>
          </div>

          {/* Listagem de Itens */}
          {isLoadingItems ? (
            <LoadingSkeleton rows={5} />
          ) : filteredItems.length === 0 ? (
            <EmptyState
              title="Nenhum item monitorado neste grupo"
              description="Adicione anúncios próprios ou concorrentes para acompanhar a evolução de preços e disponibilidade."
              actionLabel="Adicionar Anúncio"
              onAction={() => setIsAddItemModalOpen(true)}
            />
          ) : itemViewMode === 'table' ? (
            /* VISUALIZAÇÃO EM TABELA RESPONSIVA */
            <div
              role="region"
              aria-label="Tabela de anúncios monitorados com rolagem acessível"
              tabIndex={0}
              className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl overflow-hidden shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs" role="table">
                  <thead className="bg-slate-50 dark:bg-slate-800/80 text-slate-500 dark:text-slate-400 font-semibold border-b border-slate-200 dark:border-slate-800">
                    <tr>
                      <th className="py-3 px-4">Canal</th>
                      <th className="py-3 px-4">Anúncio / Título</th>
                      <th className="py-3 px-4">Vendedor</th>
                      <th className="py-3 px-4">Preço Atual</th>
                      <th className="py-3 px-4">1ª Coleta</th>
                      <th className="py-3 px-4">Última Coleta</th>
                      <th className="py-3 px-4">Status</th>
                      <th className="py-3 px-4 text-right">Ações</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 dark:divide-slate-800 text-slate-700 dark:text-slate-300">
                    {filteredItems.map((item) => (
                      <tr
                        key={item.id}
                        className="hover:bg-slate-50/80 dark:hover:bg-slate-800/40 transition-colors"
                      >
                        {/* Canal */}
                        <td className="py-3 px-4 whitespace-nowrap">
                          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200">
                            {item.marketplace}
                          </span>
                        </td>

                        {/* Título & ID */}
                        <td className="py-3 px-4 max-w-xs">
                          <div className="font-semibold text-slate-900 dark:text-white line-clamp-1">
                            {item.title}
                          </div>
                          <div className="text-[10px] font-mono text-slate-400 mt-0.5 flex items-center gap-2">
                            <span>ID: {item.externalListingId}</span>
                            {item.sourceUrl && (
                              <a
                                href={item.sourceUrl}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="text-blue-600 dark:text-blue-400 hover:underline min-h-[44px] flex items-center"
                              >
                                Canal ↗
                              </a>
                            )}
                          </div>
                        </td>

                        {/* Vendedor */}
                        <td className="py-3 px-4 whitespace-nowrap">
                          {item.sellerName || <span className="text-slate-400">---</span>}
                        </td>

                        {/* Preço */}
                        <td className="py-3 px-4 whitespace-nowrap font-bold text-slate-900 dark:text-white">
                          {formatCurrency(item.lastPrice, item.currency || 'BRL')}
                        </td>

                        {/* Primeira Coleta */}
                        <td className="py-3 px-4 whitespace-nowrap text-slate-500 dark:text-slate-400">
                          {item.firstSnapshotAt ? (
                            formatDate(item.firstSnapshotAt)
                          ) : (
                            <span className="text-[11px] text-amber-600 dark:text-amber-400 font-medium">
                              Aguardando 1ª coleta
                            </span>
                          )}
                        </td>

                        {/* Última Coleta */}
                        <td className="py-3 px-4 whitespace-nowrap text-slate-500 dark:text-slate-400">
                          {formatDate(item.lastSnapshotAt)}
                        </td>

                        {/* Status */}
                        <td className="py-3 px-4 whitespace-nowrap">
                          <span
                            className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                              item.status === 'ACTIVE'
                                ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/60 dark:text-emerald-300'
                                : 'bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-300'
                            }`}
                          >
                            {item.status}
                          </span>
                        </td>

                        {/* Ações */}
                        <td className="py-3 px-4 text-right whitespace-nowrap">
                          <div className="flex items-center justify-end gap-1">
                            <button
                              type="button"
                              onClick={() => setSelectedItemForTimeline(item)}
                              className="px-2.5 py-1.5 min-h-[44px] text-xs font-semibold rounded-xl bg-blue-50 dark:bg-blue-950/40 text-blue-600 dark:text-blue-300 hover:bg-blue-100 dark:hover:bg-blue-900/50 transition-colors flex items-center gap-1"
                            >
                              <span>📈</span>
                              <span>Timeline</span>
                            </button>

                            <button
                              type="button"
                              onClick={() =>
                                setDeleteTarget({
                                  type: 'ITEM',
                                  id: item.id,
                                  name: item.title,
                                  groupId: selectedGroup.id,
                                })
                              }
                              title="Remover do monitoramento"
                              className="p-2 text-rose-500 hover:text-rose-700 min-h-[44px] min-w-[44px] flex items-center justify-center rounded-lg hover:bg-rose-50 dark:hover:bg-rose-950/40 transition-colors"
                            >
                              🗑️
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          ) : (
            /* VISUALIZAÇÃO EM CARDS (Mobile / Compacto) */
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {filteredItems.map((item) => (
                <div
                  key={item.id}
                  className="p-4 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm space-y-3"
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-slate-100 dark:bg-slate-800 text-slate-800 dark:text-slate-200">
                      {item.marketplace}
                    </span>

                    <span
                      className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                        item.status === 'ACTIVE'
                          ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/60 dark:text-emerald-300'
                          : 'bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-300'
                      }`}
                    >
                      {item.status}
                    </span>
                  </div>

                  <div>
                    <h4 className="text-xs font-bold text-slate-900 dark:text-white line-clamp-2">
                      {item.title}
                    </h4>
                    <div className="font-mono text-[10px] text-slate-400 mt-0.5">
                      ID: {item.externalListingId}
                    </div>
                  </div>

                  <div className="flex items-baseline justify-between pt-1">
                    <span className="text-sm font-bold text-slate-900 dark:text-white">
                      {formatCurrency(item.lastPrice, item.currency || 'BRL')}
                    </span>
                    {item.sellerName && (
                      <span className="text-[11px] text-slate-500 dark:text-slate-400 truncate max-w-[140px]">
                        {item.sellerName}
                      </span>
                    )}
                  </div>

                  <div className="text-[10px] text-slate-400 border-t border-slate-100 dark:border-slate-800 pt-2 space-y-1">
                    <div>
                      Última coleta:{' '}
                      <span className="text-slate-600 dark:text-slate-300 font-medium">
                        {formatDate(item.lastSnapshotAt)}
                      </span>
                    </div>
                    <div>
                      1ª coleta efetiva:{' '}
                      <span className="text-slate-600 dark:text-slate-300 font-medium">
                        {item.firstSnapshotAt ? (
                          formatDate(item.firstSnapshotAt)
                        ) : (
                          <span className="text-amber-600">Aguardando</span>
                        )}
                      </span>
                    </div>
                  </div>

                  <div className="pt-2 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
                    <button
                      type="button"
                      onClick={() => setSelectedItemForTimeline(item)}
                      className="px-3 py-1.5 min-h-[44px] text-xs font-semibold rounded-xl bg-blue-600 text-white hover:bg-blue-700 transition-colors flex items-center gap-1"
                    >
                      <span>📈</span>
                      <span>Ver Timeline</span>
                    </button>

                    <button
                      type="button"
                      onClick={() =>
                        setDeleteTarget({
                          type: 'ITEM',
                          id: item.id,
                          name: item.title,
                          groupId: selectedGroup.id,
                        })
                      }
                      className="px-3 py-1.5 min-h-[44px] text-xs font-semibold text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/40 rounded-xl transition-colors"
                    >
                      Remover
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* =========================================================================
          MODAIS E DRAWERS DE APOIO
      ========================================================================= */}

      {/* Modal Criar/Editar Grupo */}
      <CreateGroupModal
        isOpen={isCreateGroupModalOpen}
        groupToEdit={groupToEdit}
        onClose={() => setIsCreateGroupModalOpen(false)}
        onSuccess={async () => {
          await fetchGroups();
        }}
      />

      {/* Modal Adicionar Item ao Grupo */}
      {selectedGroup && (
        <AddItemToGroupModal
          isOpen={isAddItemModalOpen}
          groupId={selectedGroup.id}
          groupName={selectedGroup.name}
          onClose={() => setIsAddItemModalOpen(false)}
          onSuccess={async () => {
            await fetchItemsInGroup(selectedGroup.id);
            await fetchGroups();
          }}
        />
      )}

      {/* Modal de Confirmação de Exclusão com Aviso de Retenção */}
      <ConfirmDeleteModal
        isOpen={deleteTarget !== null}
        title={
          deleteTarget?.type === 'GROUP'
            ? `Excluir Grupo "${deleteTarget?.name}"?`
            : `Remover Anúncio "${deleteTarget?.name}" do Monitoramento?`
        }
        description={
          deleteTarget?.type === 'GROUP'
            ? 'Esta ação desativará o grupo de monitoramento. Todos os agendamentos ativos vinculados serão cancelados.'
            : 'O anúncio será removido deste grupo e novas coletas programadas serão interrompidas imediatamente.'
        }
        impactNotice={
          deleteTarget?.type === 'GROUP'
            ? 'Em conformidade com a política de retenção legal, os snapshots históricos já coletados serão preservados para auditoria durante o prazo de retenção definido (90 dias padrão) antes da purga definitiva.'
            : 'Os snapshots históricos coletados deste anúncio não serão apagados imediatamente; eles permanecem auditáveis durante a janela de retenção regulatória.'
        }
        confirmLabel="Confirmar Remoção"
        isDeleting={isDeleting}
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeleteTarget(null)}
      />

      {/* Drawer da Linha do Tempo e Snapshots */}
      <MonitoringTimelineDrawer
        isOpen={selectedItemForTimeline !== null}
        item={
          selectedItemForTimeline
            ? {
                id: selectedItemForTimeline.id,
                groupId: selectedItemForTimeline.groupId,
                marketplace: selectedItemForTimeline.marketplace,
                externalListingId: selectedItemForTimeline.externalListingId,
                title: selectedItemForTimeline.title,
                sellerName: selectedItemForTimeline.sellerName,
                sourceUrl: selectedItemForTimeline.sourceUrl,
                createdAt: selectedItemForTimeline.createdAt,
                minIntervalMinutes: selectedGroup?.minIntervalMinutes || 60,
              }
            : null
        }
        onClose={() => setSelectedItemForTimeline(null)}
        onSnapshotCreated={async () => {
          if (selectedGroup) {
            await fetchItemsInGroup(selectedGroup.id);
          }
        }}
      />
    </div>
  );
}
