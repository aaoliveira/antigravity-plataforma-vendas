'use client';

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { useAuth } from '@/context/AuthContext';
import { CompanyDto, CompanyAccountLinkDto, SellerAccountDto } from '@/types/company';
import { formatCnpj, maskCnpjForList, sanitizeCnpjForLog } from '@/utils/cnpj';
import { CompanyFormModal } from './CompanyFormModal';
import { LinkAccountModal } from './LinkAccountModal';
import { AccountLinkHistoryDrawer } from './AccountLinkHistoryDrawer';

export function CompaniesAndAccountsView() {
  const { currentTenant } = useAuth();

  // Permissões de escrita (RBAC): VIEWER e ANALYST possuem apenas permissão de leitura
  const canManage = useMemo(() => {
    const role = (currentTenant?.role || '').toUpperCase();
    return !['VIEWER', 'ANALYST'].includes(role);
  }, [currentTenant?.role]);

  // Abas de navegação
  const [activeTab, setActiveTab] = useState<'COMPANIES' | 'LINKS'>('COMPANIES');

  // Estados de dados
  const [companies, setCompanies] = useState<CompanyDto[]>([]);
  const [activeLinks, setActiveLinks] = useState<CompanyAccountLinkDto[]>([]);
  const [sellerAccounts, setSellerAccounts] = useState<SellerAccountDto[]>([]);

  // Estados de carregamento e feedback
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successFeedback, setSuccessFeedback] = useState<string | null>(null);

  // Filtros
  const [searchTerm, setSearchTerm] = useState('');
  const [showActiveOnly, setShowActiveOnly] = useState(false);

  // Controle de privacidade de CNPJ: mapa de visibilidade por empresa
  const [revealedCnpjs, setRevealedCnpjs] = useState<Record<string, boolean>>({});

  // Modais e Gavetas
  const [isCompanyModalOpen, setIsCompanyModalOpen] = useState(false);
  const [companyToEdit, setCompanyToEdit] = useState<CompanyDto | null>(null);

  const [isLinkModalOpen, setIsLinkModalOpen] = useState(false);
  const [targetCompanyIdForLink, setTargetCompanyIdForLink] = useState<string | undefined>(undefined);

  const [isHistoryDrawerOpen, setIsHistoryDrawerOpen] = useState(false);
  const [accountForHistory, setAccountForHistory] = useState<SellerAccountDto | null>(null);

  const [linkToDelete, setLinkToDelete] = useState<CompanyAccountLinkDto | null>(null);
  const [companyToDeactivate, setCompanyToDeactivate] = useState<CompanyDto | null>(null);

  // Carrega todas as informações necessárias
  const loadData = useCallback(async () => {
    setIsLoading(true);
    setErrorMessage(null);

    try {
      // 1. Carregar Empresas
      const companiesRes = await fetch('/api/companies?activeOnly=false&size=100');
      let companiesList: CompanyDto[] = [];
      if (companiesRes.ok) {
        const data = await companiesRes.json();
        companiesList = Array.isArray(data) ? data : data.content || [];
        setCompanies(companiesList);
      } else {
        const err = await companiesRes.json().catch(() => ({}));
        throw new Error(err.message || 'Falha ao buscar empresas cadastradas.');
      }

      // 2. Carregar Contas de Marketplace conectadas
      const accountsRes = await fetch('/api/catalog/seller-accounts');
      let accountsList: SellerAccountDto[] = [];
      if (accountsRes.ok) {
        accountsList = await accountsRes.json();
        setSellerAccounts(accountsList);
      }

      // 3. Carregar links ativos de todas as empresas
      const linksPromises = companiesList.map((c) =>
        fetch(`/api/companies/${c.id}/links`)
          .then((res) => (res.ok ? res.json() : []))
          .catch(() => [])
      );
      const linksNested: CompanyAccountLinkDto[][] = await Promise.all(linksPromises);
      const allActiveLinks = linksNested.flat();
      setActiveLinks(allActiveLinks);
    } catch (err: unknown) {
      setErrorMessage(err instanceof Error ? err.message : 'Erro ao carregar dados de empresas e vínculos.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Alterna visibilidade do CNPJ individual
  const toggleCnpjVisibility = (companyId: string) => {
    setRevealedCnpjs((prev) => ({
      ...prev,
      [companyId]: !prev[companyId],
    }));
  };

  // Filtragem de empresas
  const filteredCompanies = useMemo(() => {
    return companies.filter((c) => {
      if (showActiveOnly && !c.isActive) return false;
      if (!searchTerm.trim()) return true;
      const term = searchTerm.toLowerCase();
      const corp = c.corporateName.toLowerCase();
      const trade = (c.tradeName || '').toLowerCase();
      const cnpjClean = c.cnpj.toLowerCase();
      return corp.includes(term) || trade.includes(term) || cnpjClean.includes(term);
    });
  }, [companies, searchTerm, showActiveOnly]);

  // Desativação de Empresa
  const handleConfirmDeactivateCompany = async () => {
    if (!companyToDeactivate) return;
    try {
      const res = await fetch(`/api/companies/${companyToDeactivate.id}`, {
        method: 'DELETE',
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data.message || 'Falha ao desativar empresa.');
      }
      setSuccessFeedback(`Empresa "${companyToDeactivate.corporateName}" desativada com sucesso.`);
      setCompanyToDeactivate(null);
      loadData();
    } catch (err: unknown) {
      setErrorMessage(err instanceof Error ? err.message : 'Erro ao desativar empresa.');
    }
  };

  // Desvinculação de Conta
  const handleConfirmUnlink = async () => {
    if (!linkToDelete) return;
    try {
      const res = await fetch(`/api/companies/links/${linkToDelete.id}`, {
        method: 'DELETE',
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data.message || 'Falha ao desvincular conta.');
      }
      setSuccessFeedback(`Conta "${linkToDelete.accountName}" desvinculada com sucesso.`);
      setLinkToDelete(null);
      loadData();
    } catch (err: unknown) {
      setErrorMessage(err instanceof Error ? err.message : 'Erro ao desvincular conta.');
    }
  };

  return (
    <div className="space-y-6" role="region" aria-label="Módulo de Gestão de Empresas e Contas">
      {/* Header com Metadados & Selos de Governança */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-4 border-b border-slate-200 dark:border-slate-800">
        <div>
          <div className="flex items-center gap-2 flex-wrap">
            <h1 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-white">
              Empresas & Unidades de Negócio
            </h1>
            <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-blue-100 dark:bg-blue-950/60 text-blue-700 dark:text-blue-300 border border-blue-200 dark:border-blue-800">
              Isolamento Multi-Tenant
            </span>
            <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800">
              CNPJ Módulo 11
            </span>
            <span className="text-[11px] font-semibold px-2 py-0.5 rounded-full bg-indigo-100 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800">
              LGPD & Proteção de CNPJ
            </span>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1 max-w-3xl">
            Cadastro de Pessoas Jurídicas e vinculação explícita de contas de marketplace para centros de custo e apuração fiscal.
          </p>
        </div>

        {/* Botão de Criação de Empresa */}
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => {
              setCompanyToEdit(null);
              setIsCompanyModalOpen(true);
            }}
            disabled={!canManage}
            aria-disabled={!canManage}
            title={!canManage ? 'Apenas administradores ou operadores podem cadastrar empresas' : undefined}
            className={`px-4 py-2.5 rounded-xl text-xs font-semibold flex items-center justify-center gap-2 min-h-[44px] transition-colors ${
              canManage
                ? 'bg-blue-600 hover:bg-blue-700 text-white shadow-sm'
                : 'bg-slate-200 dark:bg-slate-800 text-slate-400 cursor-not-allowed opacity-60'
            }`}
          >
            <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            Nova Empresa
          </button>
        </div>
      </div>

      {/* Banner de Permissão Somente Leitura quando canManage = false */}
      {!canManage && (
        <div
          role="status"
          className="p-3.5 rounded-xl bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-900 text-xs text-amber-800 dark:text-amber-300 flex items-center gap-2.5"
        >
          <svg className="w-5 h-5 text-amber-500 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span>
            <strong>Modo Somente Leitura:</strong> Seu perfil atual possui privilégios de visualização. Ações de cadastro, alteração e transferência de contas estão restritas a administradores.
          </span>
        </div>
      )}

      {/* Feedback de Erro */}
      {errorMessage && (
        <div
          role="alert"
          className="p-3.5 rounded-xl bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900 text-xs text-rose-700 dark:text-rose-300 flex items-start justify-between gap-2.5"
        >
          <div className="flex items-center gap-2">
            <svg className="w-5 h-5 text-rose-500 shrink-0" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
            </svg>
            <span>{errorMessage}</span>
          </div>
          <button
            onClick={() => setErrorMessage(null)}
            className="text-rose-500 hover:text-rose-700 text-sm font-bold min-h-[32px] px-2"
          >
            ✕
          </button>
        </div>
      )}

      {/* Feedback de Sucesso */}
      {successFeedback && (
        <div
          role="status"
          className="p-3.5 rounded-xl bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-900 text-xs text-emerald-700 dark:text-emerald-300 flex items-start justify-between gap-2.5"
        >
          <div className="flex items-center gap-2">
            <svg className="w-5 h-5 text-emerald-500 shrink-0" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
            </svg>
            <span>{successFeedback}</span>
          </div>
          <button
            onClick={() => setSuccessFeedback(null)}
            className="text-emerald-500 hover:text-emerald-700 text-sm font-bold min-h-[32px] px-2"
          >
            ✕
          </button>
        </div>
      )}

      {/* Abas de Navegação */}
      <div className="flex items-center gap-2 border-b border-slate-200 dark:border-slate-800" role="tablist">
        <button
          role="tab"
          aria-selected={activeTab === 'COMPANIES'}
          onClick={() => setActiveTab('COMPANIES')}
          className={`pb-3 px-3 text-xs sm:text-sm font-semibold transition-colors border-b-2 min-h-[44px] flex items-center gap-2 ${
            activeTab === 'COMPANIES'
              ? 'border-blue-600 text-blue-600 dark:text-blue-400'
              : 'border-transparent text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
          }`}
        >
          <span>Empresas Cadastradas</span>
          <span className="text-[11px] px-2 py-0.5 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400">
            {companies.length}
          </span>
        </button>

        <button
          role="tab"
          aria-selected={activeTab === 'LINKS'}
          onClick={() => setActiveTab('LINKS')}
          className={`pb-3 px-3 text-xs sm:text-sm font-semibold transition-colors border-b-2 min-h-[44px] flex items-center gap-2 ${
            activeTab === 'LINKS'
              ? 'border-blue-600 text-blue-600 dark:text-blue-400'
              : 'border-transparent text-slate-500 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
          }`}
        >
          <span>Vínculos de Contas de Marketplace</span>
          <span className="text-[11px] px-2 py-0.5 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400">
            {activeLinks.length}
          </span>
        </button>
      </div>

      {/* Loading Skeleton */}
      {isLoading && (
        <div className="py-16 text-center space-y-3" role="status" aria-busy="true">
          <div className="inline-block animate-spin h-8 w-8 text-blue-600 border-4 border-current border-t-transparent rounded-full" />
          <p className="text-xs text-slate-500 dark:text-slate-400">Carregando dados organizacionais e vínculos...</p>
        </div>
      )}

      {/* =========================================================================
          ABA 1: EMPRESAS CADASTRADAS
      ========================================================================= */}
      {!isLoading && activeTab === 'COMPANIES' && (
        <div className="space-y-4">
          {/* Barra de Filtros */}
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 bg-slate-50 dark:bg-slate-900/60 p-3 rounded-2xl border border-slate-200 dark:border-slate-800">
            <div className="relative flex-1">
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Buscar por Razão Social, Nome Fantasia ou CNPJ..."
                className="w-full pl-9 pr-3.5 py-2.5 rounded-xl text-xs bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 text-slate-900 dark:text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-blue-500 min-h-[44px]"
              />
              <svg className="w-4 h-4 text-slate-400 absolute left-3 top-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </div>

            <div className="flex items-center gap-2">
              <label className="flex items-center gap-2 text-xs text-slate-600 dark:text-slate-400 cursor-pointer select-none px-2 min-h-[44px]">
                <input
                  type="checkbox"
                  checked={showActiveOnly}
                  onChange={(e) => setShowActiveOnly(e.target.checked)}
                  className="w-4 h-4 text-blue-600 rounded border-slate-300 dark:border-slate-700 focus:ring-blue-500"
                />
                Apenas Ativas
              </label>
            </div>
          </div>

          {/* Tabela de Empresas */}
          {filteredCompanies.length === 0 ? (
            <div className="p-12 text-center border-2 border-dashed border-slate-200 dark:border-slate-800 rounded-2xl space-y-3" role="region">
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Nenhuma empresa encontrada com os filtros aplicados.
              </p>
              {canManage && (
                <button
                  type="button"
                  onClick={() => setIsCompanyModalOpen(true)}
                  className="px-4 py-2 rounded-xl text-xs font-semibold bg-blue-600 text-white hover:bg-blue-700 transition-colors min-h-[44px]"
                >
                  Cadastrar Primeira Empresa
                </button>
              )}
            </div>
          ) : (
            <div className="overflow-x-auto rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
              <table className="w-full text-left text-xs" role="table">
                <thead className="bg-slate-50 dark:bg-slate-800/60 border-b border-slate-200 dark:border-slate-800 text-slate-500 dark:text-slate-400 font-semibold">
                  <tr>
                    <th scope="col" className="p-4">Empresa / Razão Social</th>
                    <th scope="col" className="p-4">CNPJ (Protegido)</th>
                    <th scope="col" className="p-4">Regime Tributário</th>
                    <th scope="col" className="p-4">Contas Vinculadas</th>
                    <th scope="col" className="p-4">Status</th>
                    <th scope="col" className="p-4 text-right">Ações</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60 text-slate-700 dark:text-slate-300">
                  {filteredCompanies.map((c) => {
                    const isRevealed = revealedCnpjs[c.id];
                    const displayedCnpj = isRevealed
                      ? c.formattedCnpj || formatCnpj(c.cnpj)
                      : maskCnpjForList(c.cnpj);

                    const companyLinks = activeLinks.filter((l) => l.companyId === c.id);

                    return (
                      <tr key={c.id} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/40 transition-colors">
                        <td className="p-4">
                          <div className="font-semibold text-slate-900 dark:text-white">
                            {c.corporateName}
                          </div>
                          {c.tradeName && (
                            <div className="text-[11px] text-slate-500 dark:text-slate-400">
                              {c.tradeName}
                            </div>
                          )}
                        </td>

                        {/* CNPJ com Proteção de Dados & Botão Mostrar/Ocultar */}
                        <td className="p-4">
                          <div className="flex items-center gap-2">
                            <span className="font-mono text-[11px] font-medium text-slate-800 dark:text-slate-200">
                              {displayedCnpj}
                            </span>
                            <button
                              type="button"
                              onClick={() => toggleCnpjVisibility(c.id)}
                              aria-label={isRevealed ? `Ocultar CNPJ de ${c.corporateName}` : `Revelar CNPJ de ${c.corporateName}`}
                              title={isRevealed ? 'Ocultar CNPJ' : 'Mostrar CNPJ completo'}
                              className="p-1.5 text-slate-400 hover:text-slate-700 dark:hover:text-slate-200 rounded hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors min-h-[36px] min-w-[36px] flex items-center justify-center"
                            >
                              {isRevealed ? (
                                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l18 18" />
                                </svg>
                              ) : (
                                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                                </svg>
                              )}
                            </button>
                          </div>
                        </td>

                        <td className="p-4">
                          <span className="text-[11px] px-2.5 py-1 rounded-lg bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 font-medium">
                            {c.taxRegime || 'Não Informado'}
                          </span>
                        </td>

                        <td className="p-4">
                          <div className="flex items-center gap-1.5 flex-wrap">
                            <span className="font-semibold text-slate-900 dark:text-white">
                              {companyLinks.length}
                            </span>
                            <span className="text-[11px] text-slate-500">
                              {companyLinks.length === 1 ? 'conta' : 'contas'}
                            </span>
                            {canManage && c.isActive && (
                              <button
                                type="button"
                                onClick={() => {
                                  setTargetCompanyIdForLink(c.id);
                                  setIsLinkModalOpen(true);
                                }}
                                className="ml-1 text-[11px] text-blue-600 dark:text-blue-400 hover:underline font-semibold min-h-[36px] flex items-center px-1"
                              >
                                + Vincular
                              </button>
                            )}
                          </div>
                        </td>

                        <td className="p-4">
                          <span
                            className={`text-[11px] font-semibold px-2 py-0.5 rounded-full ${
                              c.isActive
                                ? 'bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300'
                                : 'bg-slate-200 dark:bg-slate-700 text-slate-600 dark:text-slate-400'
                            }`}
                          >
                            {c.isActive ? 'Ativa' : 'Inativa'}
                          </span>
                        </td>

                        {/* Ações */}
                        <td className="p-4 text-right">
                          <div className="flex items-center justify-end gap-2">
                            <button
                              type="button"
                              onClick={() => {
                                setCompanyToEdit(c);
                                setIsCompanyModalOpen(true);
                              }}
                              disabled={!canManage}
                              aria-disabled={!canManage}
                              className={`p-2 rounded-lg text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors min-h-[44px] min-w-[44px] flex items-center justify-center ${
                                !canManage ? 'opacity-40 cursor-not-allowed' : ''
                              }`}
                              aria-label={`Editar ${c.corporateName}`}
                              title={canManage ? 'Editar Empresa' : 'Acesso Restrito'}
                            >
                              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                              </svg>
                            </button>

                            {c.isActive && (
                              <button
                                type="button"
                                onClick={() => setCompanyToDeactivate(c)}
                                disabled={!canManage}
                                aria-disabled={!canManage}
                                className={`p-2 rounded-lg text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/40 transition-colors min-h-[44px] min-w-[44px] flex items-center justify-center ${
                                  !canManage ? 'opacity-40 cursor-not-allowed' : ''
                                }`}
                                aria-label={`Desativar ${c.corporateName}`}
                                title={canManage ? 'Desativar Empresa' : 'Acesso Restrito'}
                              >
                                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
                                </svg>
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* =========================================================================
          ABA 2: VÍNCULOS DE CONTAS DE MARKETPLACE
      ========================================================================= */}
      {!isLoading && activeTab === 'LINKS' && (
        <div className="space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-slate-50 dark:bg-slate-900/60 p-3 rounded-2xl border border-slate-200 dark:border-slate-800">
            <div>
              <span className="font-semibold text-xs text-slate-800 dark:text-slate-200">
                Contas Conectadas & Titularidade
              </span>
              <p className="text-[11px] text-slate-500 dark:text-slate-400">
                Cada conta de marketplace conectada deve possuir vínculo inequívoco com no máximo 1 empresa ativa.
              </p>
            </div>
            {canManage && (
              <button
                type="button"
                onClick={() => {
                  setTargetCompanyIdForLink(undefined);
                  setIsLinkModalOpen(true);
                }}
                className="px-4 py-2.5 rounded-xl text-xs font-semibold bg-blue-600 hover:bg-blue-700 text-white transition-colors min-h-[44px] flex items-center justify-center gap-2"
              >
                <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                </svg>
                Vincular Nova Conta
              </button>
            )}
          </div>

          {sellerAccounts.length === 0 ? (
            <div className="p-12 text-center border-2 border-dashed border-slate-200 dark:border-slate-800 rounded-2xl space-y-3" role="region">
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Nenhuma conta de marketplace conectada encontrada neste tenant. Conecte contas via Mercado Livre ou Shopee na aba de Lojas.
              </p>
            </div>
          ) : (
            <div className="overflow-x-auto rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
              <table className="w-full text-left text-xs" role="table">
                <thead className="bg-slate-50 dark:bg-slate-800/60 border-b border-slate-200 dark:border-slate-800 text-slate-500 dark:text-slate-400 font-semibold">
                  <tr>
                    <th scope="col" className="p-4">Conta / Marketplace</th>
                    <th scope="col" className="p-4">Empresa Vinculada</th>
                    <th scope="col" className="p-4">Versão do Vínculo</th>
                    <th scope="col" className="p-4">Vigência Inicial</th>
                    <th scope="col" className="p-4 text-right">Ações</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800/60 text-slate-700 dark:text-slate-300">
                  {sellerAccounts.map((account) => {
                    const activeLink = activeLinks.find((l) => l.sellerAccountId === account.id && l.isActive);
                    const isMeli = account.marketplace === 'MERCADO_LIVRE';

                    return (
                      <tr key={account.id} className="hover:bg-slate-50/60 dark:hover:bg-slate-800/40 transition-colors">
                        <td className="p-4">
                          <div className="flex items-center gap-2">
                            <span
                              className={`text-[10px] font-bold px-2 py-0.5 rounded-md ${
                                isMeli
                                  ? 'bg-amber-100 dark:bg-amber-950/60 text-amber-800 dark:text-amber-300 border border-amber-200 dark:border-amber-800'
                                  : 'bg-orange-100 dark:bg-orange-950/60 text-orange-800 dark:text-orange-300 border border-orange-200 dark:border-orange-800'
                              }`}
                            >
                              {isMeli ? 'Mercado Livre' : account.marketplace}
                            </span>
                            <span className="font-semibold text-slate-900 dark:text-white">
                              {account.accountName}
                            </span>
                          </div>
                          <div className="text-[11px] text-slate-500 font-mono mt-0.5">
                            ID: {account.externalAccountId}
                          </div>
                        </td>

                        <td className="p-4">
                          {activeLink ? (
                            <div>
                              <div className="font-semibold text-slate-900 dark:text-white">
                                {activeLink.companyName}
                              </div>
                              <span className="text-[10px] font-medium text-emerald-600 dark:text-emerald-400">
                                ● Vínculo Ativo
                              </span>
                            </div>
                          ) : (
                            <span className="inline-flex items-center gap-1.5 text-amber-700 dark:text-amber-400 font-medium text-[11px]">
                              ⚠ Sem empresa vinculada
                            </span>
                          )}
                        </td>

                        <td className="p-4">
                          {activeLink ? (
                            <span className="text-[11px] font-semibold px-2 py-0.5 rounded-md bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300">
                              v{activeLink.version}
                            </span>
                          ) : (
                            '—'
                          )}
                        </td>

                        <td className="p-4 text-slate-600 dark:text-slate-400">
                          {activeLink ? (
                            new Date(activeLink.effectiveFrom).toLocaleDateString('pt-BR')
                          ) : (
                            '—'
                          )}
                        </td>

                        <td className="p-4 text-right">
                          <div className="flex items-center justify-end gap-2">
                            {/* Ver Histórico */}
                            <button
                              type="button"
                              onClick={() => {
                                setAccountForHistory(account);
                                setIsHistoryDrawerOpen(true);
                              }}
                              className="px-3 py-1.5 rounded-lg text-xs font-semibold text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors min-h-[44px] flex items-center gap-1.5"
                              title="Ver Linha do Tempo e Histórico de Vigência"
                            >
                              <svg className="w-4 h-4 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                              </svg>
                              Histórico
                            </button>

                            {/* Desvincular se ativo */}
                            {activeLink && canManage && (
                              <button
                                type="button"
                                onClick={() => setLinkToDelete(activeLink)}
                                className="px-3 py-1.5 rounded-lg text-xs font-semibold text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/40 transition-colors min-h-[44px] flex items-center gap-1.5"
                              >
                                Desvincular
                              </button>
                            )}

                            {/* Vincular se sem empresa */}
                            {!activeLink && canManage && (
                              <button
                                type="button"
                                onClick={() => {
                                  setTargetCompanyIdForLink(undefined);
                                  setIsLinkModalOpen(true);
                                }}
                                className="px-3 py-1.5 rounded-lg text-xs font-semibold bg-blue-50 dark:bg-blue-950/50 text-blue-600 dark:text-blue-400 hover:bg-blue-100 dark:hover:bg-blue-900/50 transition-colors min-h-[44px] flex items-center gap-1.5"
                              >
                                Vincular
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>
      )}

      {/* Modal de Cadastro / Edição de Empresa */}
      <CompanyFormModal
        isOpen={isCompanyModalOpen}
        onClose={() => {
          setIsCompanyModalOpen(false);
          setCompanyToEdit(null);
        }}
        onSuccess={() => {
          setSuccessFeedback(companyToEdit ? 'Empresa atualizada com sucesso.' : 'Empresa cadastrada com sucesso.');
          loadData();
        }}
        companyToEdit={companyToEdit}
      />

      {/* Modal de Vinculação de Conta */}
      <LinkAccountModal
        isOpen={isLinkModalOpen}
        onClose={() => {
          setIsLinkModalOpen(false);
          setTargetCompanyIdForLink(undefined);
        }}
        onSuccess={() => {
          setSuccessFeedback('Conta vinculada à empresa com sucesso.');
          loadData();
        }}
        companies={companies.filter((c) => c.isActive)}
        targetCompanyId={targetCompanyIdForLink}
        sellerAccounts={sellerAccounts}
        existingActiveLinks={activeLinks}
      />

      {/* Gaveta de Histórico de Vínculos */}
      <AccountLinkHistoryDrawer
        isOpen={isHistoryDrawerOpen}
        onClose={() => {
          setIsHistoryDrawerOpen(false);
          setAccountForHistory(null);
        }}
        sellerAccount={accountForHistory}
      />

      {/* Modal de Confirmação de Desvinculação */}
      {linkToDelete && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="confirm-unlink-title"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm"
        >
          <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-6 max-w-md w-full space-y-4 shadow-2xl">
            <h3 id="confirm-unlink-title" className="text-base font-bold text-slate-900 dark:text-white">
              Confirmar Desvinculação de Conta
            </h3>
            <p className="text-xs text-slate-600 dark:text-slate-400">
              Tem certeza que deseja desvincular a conta <strong>{linkToDelete.accountName}</strong> da empresa <strong>{linkToDelete.companyName}</strong>?
            </p>
            <p className="text-[11px] text-slate-500 dark:text-slate-400">
              O vínculo ativo será encerrado e arquivado no histórico de vigência para integridade contábil.
            </p>
            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setLinkToDelete(null)}
                className="px-4 py-2.5 rounded-xl text-xs font-semibold text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors min-h-[44px]"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={handleConfirmUnlink}
                className="px-4 py-2.5 rounded-xl text-xs font-semibold bg-rose-600 hover:bg-rose-700 text-white transition-colors min-h-[44px]"
              >
                Confirmar Desvinculação
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal de Confirmação de Desativação de Empresa */}
      {companyToDeactivate && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="confirm-deactivate-title"
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm"
        >
          <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-6 max-w-md w-full space-y-4 shadow-2xl">
            <h3 id="confirm-deactivate-title" className="text-base font-bold text-slate-900 dark:text-white">
              Desativar Empresa
            </h3>
            <p className="text-xs text-slate-600 dark:text-slate-400">
              Deseja desativar a empresa <strong>{companyToDeactivate.corporateName}</strong>?
            </p>
            <p className="text-[11px] text-slate-500 dark:text-slate-400">
              Empresas desativadas não poderão receber novos vínculos de contas e seus registros históricos serão preservados para auditoria.
            </p>
            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setCompanyToDeactivate(null)}
                className="px-4 py-2.5 rounded-xl text-xs font-semibold text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors min-h-[44px]"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={handleConfirmDeactivateCompany}
                className="px-4 py-2.5 rounded-xl text-xs font-semibold bg-rose-600 hover:bg-rose-700 text-white transition-colors min-h-[44px]"
              >
                Confirmar Desativação
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
