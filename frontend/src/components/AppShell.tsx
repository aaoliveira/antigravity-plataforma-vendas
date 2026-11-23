'use client';

import React, { useState, useMemo, useEffect } from 'react';
import { ThemeToggle } from './ThemeToggle';
import { ModulePlaceholder } from './ModulePlaceholder';
import { EmptyState } from './EmptyState';
import { LoadingSkeleton } from './LoadingSkeleton';
import { useAuth } from '@/context/AuthContext';
import { TenantSwitcher } from './TenantSwitcher';
import { SessionBanner } from './SessionBanner';
import { AccessDeniedFeedback } from './AccessDeniedFeedback';
import { MarketplaceIntegrationsView } from './MarketplaceIntegrationsView';
import { CatalogAndListingsView } from './CatalogAndListingsView';
import { OrdersView } from './OrdersView';
import { MarketIntelligenceView } from './MarketIntelligenceView';
import { MarketSearchView } from './MarketSearchView';
import { ProductMonitoringView } from './ProductMonitoringView';
import { PricingAndCostsView } from './pricing/PricingAndCostsView';
import { CompaniesAndAccountsView } from './company/CompaniesAndAccountsView';
import { FinancialFactsDashboardView } from './financial/FinancialFactsDashboardView';
import { ListingAnalyticsDashboardView } from './listinganalytics/ListingAnalyticsDashboardView';
import { AiAssistantView } from './aiassistant/AiAssistantView';
import { DraftListingEditorView } from './draftlisting/DraftListingEditorView';
import { ReportsExportView } from './reporting/ReportsExportView';

export interface NavModule {
  id: string;
  name: string;
  shortLabel: string;
  pillarKey: 'BI' | 'MARKET' | 'AI' | 'FINANCE' | 'MANAGEMENT' | 'GOVERNANCE';
  pillarName: string;
  description: string;
  status: 'READY' | 'PLANNED';
  provenience: string;
}

export const navModules: NavModule[] = [
  // 1. Business Intelligence & Ações
  {
    id: 'ACTION_CENTER',
    name: 'Smart Alerts & Action Center',
    shortLabel: 'Alertas',
    pillarKey: 'BI',
    pillarName: 'Business Intelligence & Ações',
    description: 'Central de tarefas operacionais derivadas de alertas de margem e anomalias de mercado.',
    status: 'PLANNED',
    provenience: 'OBSERVED / INTERNAL_EVENT',
  },
  {
    id: 'MONITORING',
    name: 'Monitoramento & Concorrentes',
    shortLabel: 'Concorrentes',
    pillarKey: 'BI',
    pillarName: 'Business Intelligence & Ações',
    description: 'Pesquisa autorizada e monitoramento de concorrência com proveniência e comparação lado a lado.',
    status: 'READY',
    provenience: 'OBSERVED / OFFICIAL',
  },
  {
    id: 'LISTING_ANALYTICS',
    name: 'Analytics de Anúncios & Métricas Oficiais',
    shortLabel: 'Analytics',
    pillarKey: 'BI',
    pillarName: 'Business Intelligence & Ações',
    description: 'Dashboard de métricas oficiais de anúncios por período, comparação temporal e aviso de indisponibilidade.',
    status: 'READY',
    provenience: 'OFFICIAL_METRICS_API',
  },

  // 2. Market Intelligence
  {
    id: 'MARKET',
    name: 'Pesquisa de Mercado',
    shortLabel: 'Mercado',
    pillarKey: 'MARKET',
    pillarName: 'Market Intelligence',
    description: 'Descoberta de produtos e categorias em fontes públicas autorizadas com proveniência explícita.',
    status: 'READY',
    provenience: 'OBSERVED / OFFICIAL',
  },
  {
    id: 'DISCOVERY',
    name: 'Product Discovery',
    shortLabel: 'Discovery',
    pillarKey: 'MARKET',
    pillarName: 'Market Intelligence',
    description: 'Extração segura de catálogos PDF de fornecedores e busca de similares na base.',
    status: 'PLANNED',
    provenience: 'IMPORTED (Revisão Humana)',
  },

  // 3. AI Commerce
  {
    id: 'AI_STUDIO',
    name: 'AI Listing Studio',
    shortLabel: 'AI Studio',
    pillarKey: 'AI',
    pillarName: 'AI Commerce',
    description: 'Auditoria de score de anúncios e redação assistida sob guardrails anti-alucinação.',
    status: 'READY',
    provenience: 'AI_GENERATED (Rascunho)',
  },
  {
    id: 'ASSISTANT',
    name: 'Assistente de IA & Governança',
    shortLabel: 'Assistente',
    pillarKey: 'AI',
    pillarName: 'AI Commerce',
    description: 'Consultoria assistiva multi-canal fundamentada em ferramentas determinísticas e isolamento multi-tenant.',
    status: 'READY',
    provenience: 'INTERNAL_TOOLS_ONLY',
  },

  // 4. Seller Control & Finanças
  {
    id: 'FINANCIAL',
    name: 'Motor Financeiro Determinístico',
    shortLabel: 'Finanças',
    pillarKey: 'FINANCE',
    pillarName: 'Seller Control & Finanças',
    description: 'Cálculo de margem de contribuição, CMV e taxas de canais com precisão em BigDecimal.',
    status: 'READY',
    provenience: 'DETERMINISTIC_CALCULATION',
  },
  {
    id: 'RECONCILIATION_DRE',
    name: 'DRE Gerencial & Conciliação',
    shortLabel: 'DRE',
    pillarKey: 'FINANCE',
    pillarName: 'Seller Control & Finanças',
    description: 'Visão gerencial de resultado (Caixa vs Competência) e conciliação de repasses.',
    status: 'READY',
    provenience: 'IMPORTED / RECONCILED',
  },

  // 5. Gestão & Integrações
  {
    id: 'COMPANIES',
    name: 'Empresas & CNPJs',
    shortLabel: 'Empresas',
    pillarKey: 'MANAGEMENT',
    pillarName: 'Gestão & Integrações',
    description: 'Estrutura organizacional, enquadramento tributário e isolamento multi-tenant.',
    status: 'READY',
    provenience: 'USER_INPUT',
  },
  {
    id: 'STORES',
    name: 'Lojas & Marketplaces',
    shortLabel: 'Lojas',
    pillarKey: 'MANAGEMENT',
    pillarName: 'Gestão & Integrações',
    description: 'Gestão de conexões oficiais OAuth, capacidades e conformidade com Mercado Livre e Shopee.',
    status: 'READY',
    provenience: 'OFFICIAL_OAUTH',
  },
  {
    id: 'SYNC',
    name: 'Catálogo & Anúncios Próprios',
    shortLabel: 'Catálogo',
    pillarKey: 'MANAGEMENT',
    pillarName: 'Gestão & Integrações',
    description: 'Catálogo canônico multi-anúncio, vinculação de canais (Mercado Livre e Shopee), paginação server-side e detalhes.',
    status: 'READY',
    provenience: 'CANONICAL_CATALOG',
  },
  {
    id: 'ORDERS',
    name: 'Pedidos de Venda & Reconciliação',
    shortLabel: 'Pedidos',
    pillarKey: 'MANAGEMENT',
    pillarName: 'Gestão & Integrações',
    description: 'Ingestão e reconciliação de pedidos próprios, status de conciliação, itens e sinalização de incompletos.',
    status: 'READY',
    provenience: 'OFFICIAL_ORDERS_API',
  },

  // 6. Segurança & Governança
  {
    id: 'COMPLIANCE',
    name: 'Continuous Compliance & Políticas',
    shortLabel: 'Compliance',
    pillarKey: 'GOVERNANCE',
    pillarName: 'Segurança & Governança',
    description: 'Monitoramento contínuo de termos dos marketplaces com proteção Fail-Closed.',
    status: 'READY',
    provenience: 'POLICY_AUDIT',
  },
  {
    id: 'REPORTS',
    name: 'Relatórios & Exportações',
    shortLabel: 'Relatórios',
    pillarKey: 'GOVERNANCE',
    pillarName: 'Segurança & Governança',
    description: 'Central de exportação assíncrona CSV/XLSX, acompanhamento de jobs, histórico e download autorizado.',
    status: 'READY',
    provenience: 'AUDITED_EXPORT_SERVICE',
  },
  {
    id: 'HEALTH',
    name: 'Diagnóstico de Infraestrutura',
    shortLabel: 'Diagnóstico',
    pillarKey: 'GOVERNANCE',
    pillarName: 'Segurança & Governança',
    description: 'Telemetria de conectividade com PostgreSQL, Redis, RabbitMQ e Keycloak.',
    status: 'READY',
    provenience: 'ACTUATOR_HEALTH',
  },
];

export const AppShell: React.FC = () => {
  const { user, currentTenant, logout, isAuthenticated, sessionExpired } = useAuth();
  const [activeModuleId, setActiveModuleId] = useState<string>('FINANCIAL');
  const [mobileMenuOpen, setMobileMenuOpen] = useState<boolean>(false);
  const [searchFilter, setSearchFilter] = useState<string>('');
  const [demoState, setDemoState] = useState<'NORMAL' | 'EMPTY' | 'LOADING' | 'DENIED'>('NORMAL');

  const activeModule = useMemo(
    () => navModules.find((m) => m.id === activeModuleId) || navModules[0],
    [activeModuleId]
  );

  const filteredModules = useMemo(() => {
    if (!searchFilter.trim()) return navModules;
    const term = searchFilter.toLowerCase();
    return navModules.filter(
      (m) =>
        m.name.toLowerCase().includes(term) ||
        m.description.toLowerCase().includes(term) ||
        m.pillarName.toLowerCase().includes(term)
    );
  }, [searchFilter]);

  // Teclado: Escape fecha gaveta mobile
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && mobileMenuOpen) {
        setMobileMenuOpen(false);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [mobileMenuOpen]);

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 flex flex-col w-full max-w-full overflow-x-hidden">
      {/* =========================================================================
          1. HEADER UNIFICADO RESPONSIVO (Master Top Bar)
      ========================================================================= */}
      <header className="sticky top-0 z-40 w-full h-14 sm:h-16 bg-white/95 dark:bg-slate-900/95 border-b border-slate-200 dark:border-slate-800 backdrop-blur-md px-3 sm:px-6 flex items-center justify-between select-none">
        {/* Esquerda: Menu mobile + Logo + Título */}
        <div className="flex items-center gap-2.5 sm:gap-3 min-w-0">
          <button
            onClick={() => setMobileMenuOpen(true)}
            aria-label="Abrir menu de navegação"
            type="button"
            className="lg:hidden p-2 rounded-lg bg-slate-100 dark:bg-slate-800 border border-slate-300 dark:border-slate-700 text-slate-700 dark:text-slate-300 hover:text-slate-900 dark:hover:text-white min-w-[40px] min-h-[40px] flex items-center justify-center shrink-0 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>

          <div className="flex h-9 w-9 sm:h-10 sm:w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-blue-600 via-indigo-600 to-purple-600 text-white font-black text-sm sm:text-base shadow-sm shrink-0">
            SI
          </div>

          <div className="min-w-0">
            <h1 className="text-sm sm:text-base font-bold text-slate-900 dark:text-white tracking-tight truncate">
              Seller Intelligence Platform
            </h1>
            <p className="text-[10px] sm:text-[11px] text-slate-500 dark:text-slate-400 hidden sm:block truncate">
              SaaS Multi-tenant &middot; E-commerce & Marketplaces
            </p>
          </div>
        </div>

        {/* Direita: Tenant + Status + ThemeToggle + Logout */}
        <div className="flex items-center gap-2 sm:gap-3 shrink-0">
          {currentTenant ? (
            <TenantSwitcher />
          ) : (
            <div className="hidden md:flex items-center gap-1.5 text-xs bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-full px-3 py-1 text-slate-700 dark:text-slate-300">
              <span className="h-1.5 w-1.5 rounded-full bg-blue-500" />
              <span className="font-medium truncate max-w-[120px]">Matriz Principal</span>
            </div>
          )}

          <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-300 dark:border-emerald-800 bg-emerald-50 dark:bg-emerald-950/40 px-2.5 py-1 text-[11px] font-medium text-emerald-700 dark:text-emerald-400">
            <span className="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-pulse" />
            <span className="hidden sm:inline">Fail-Closed Protegido</span>
            <span className="sm:hidden">Protegido</span>
          </span>

          {isAuthenticated && (
            <button
              onClick={logout}
              type="button"
              aria-label="Encerrar sessão"
              className="p-2 rounded-lg text-slate-500 hover:text-rose-600 dark:hover:text-rose-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
              title="Sair"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
              </svg>
            </button>
          )}

          <ThemeToggle />
        </div>
      </header>

      {/* Alerta de Sessão Expirada / Falha de Autenticação */}
      {sessionExpired && <SessionBanner />}

      {/* =========================================================================
          2. WORKSPACE CONTAINER (Sidebar Desktop + Conteúdo Principal)
      ========================================================================= */}
      <div className="flex flex-1 w-full max-w-full overflow-x-hidden">
        {/* Sidebar Desktop (Telas >= 1024px) */}
        <aside
          aria-label="Navegação desktop"
          className="hidden lg:flex lg:flex-col lg:w-64 xl:w-72 shrink-0 bg-white dark:bg-slate-900/95 border-r border-slate-200 dark:border-slate-800 sticky top-14 sm:top-16 h-[calc(100vh-3.5rem)] sm:h-[calc(100vh-4rem)] overflow-y-auto p-4 select-none justify-between"
        >
          <div className="space-y-4">
            {/* Campo de Busca Rápida */}
            <div className="relative">
              <input
                type="text"
                placeholder="Buscar módulo..."
                value={searchFilter}
                onChange={(e) => setSearchFilter(e.target.value)}
                className="w-full rounded-lg bg-slate-50 dark:bg-slate-950 border border-slate-300 dark:border-slate-800 px-3 py-2 text-xs text-slate-800 dark:text-slate-200 placeholder-slate-400 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
              />
              {searchFilter && (
                <button
                  onClick={() => setSearchFilter('')}
                  aria-label="Limpar busca"
                  className="absolute right-2.5 top-2 text-xs text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
                >
                  ✕
                </button>
              )}
            </div>

            {/* Lista de Módulos */}
            <nav className="space-y-1">
              {filteredModules.map((mod) => {
                const isActive = mod.id === activeModuleId;
                return (
                  <button
                    key={mod.id}
                    onClick={() => setActiveModuleId(mod.id)}
                    type="button"
                    className={`w-full text-left px-3 py-2 rounded-lg text-xs font-medium transition-colors flex items-center justify-between min-h-[40px] ${
                      isActive
                        ? 'bg-blue-600 text-white font-semibold shadow-sm'
                        : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-white'
                    }`}
                  >
                    <span className="truncate">{mod.name}</span>
                    <span
                      className={`text-[10px] px-1.5 py-0.5 rounded font-mono ${
                        isActive
                          ? 'bg-blue-700 text-white'
                          : 'bg-slate-200 dark:bg-slate-800 text-slate-600 dark:text-slate-400'
                      }`}
                    >
                      {mod.status}
                    </span>
                  </button>
                );
              })}
            </nav>
          </div>

          <div className="pt-4 border-t border-slate-200 dark:border-slate-800 text-[11px] text-slate-500 dark:text-slate-400 text-center">
            Monólito Modular &middot; Java 25 & Next.js 15
          </div>
        </aside>

        {/* Conteúdo Principal */}
        <main className="flex-1 min-w-0 w-full max-w-full px-3 sm:px-6 lg:px-8 py-4 sm:py-6 space-y-6 pb-24 lg:pb-12 overflow-x-hidden">
          {/* Barra de Alternância de Demonstração de Estados (Normal / Vazio / Carregando) */}
          <div className="flex items-center justify-between bg-slate-100 dark:bg-slate-900/60 p-2 rounded-xl border border-slate-200 dark:border-slate-800 flex-wrap gap-2 text-xs">
            <span className="text-slate-600 dark:text-slate-400 font-medium">
              Simular Estado de UI:
            </span>
            <div className="flex items-center gap-1">
              <button
                onClick={() => setDemoState('NORMAL')}
                className={`px-3 py-1 rounded-md font-medium transition-colors ${
                  demoState === 'NORMAL'
                    ? 'bg-blue-600 text-white'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                }`}
              >
                Conteúdo Normal
              </button>
              <button
                onClick={() => setDemoState('EMPTY')}
                className={`px-3 py-1 rounded-md font-medium transition-colors ${
                  demoState === 'EMPTY'
                    ? 'bg-blue-600 text-white'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                }`}
              >
                Estado Vazio
              </button>
              <button
                onClick={() => setDemoState('LOADING')}
                className={`px-3 py-1 rounded-md font-medium transition-colors ${
                  demoState === 'LOADING'
                    ? 'bg-blue-600 text-white'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                }`}
              >
                Carregando (Skeleton)
              </button>
              <button
                onClick={() => setDemoState('DENIED')}
                className={`px-3 py-1 rounded-md font-medium transition-colors ${
                  demoState === 'DENIED'
                    ? 'bg-blue-600 text-white'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                }`}
              >
                Acesso Restrito (403/404)
              </button>
            </div>
          </div>

          {/* Renderização Condicional de Estado */}
          {demoState === 'LOADING' ? (
            <LoadingSkeleton rows={5} />
          ) : demoState === 'EMPTY' ? (
            <EmptyState
              title={`Nenhum dado encontrado para ${activeModule.name}`}
              description="Ainda não existem registros sincronizados para este módulo no tenant atual. Conecte sua loja ou importe dados para começar."
              actionLabel="Conectar Nova Loja"
              onAction={() => setActiveModuleId('STORES')}
            />
          ) : demoState === 'DENIED' ? (
            <AccessDeniedFeedback onBackClick={() => setDemoState('NORMAL')} />
          ) : activeModule.id === 'STORES' ? (
            <MarketplaceIntegrationsView />
          ) : activeModule.id === 'SYNC' ? (
            <CatalogAndListingsView />
          ) : activeModule.id === 'ORDERS' ? (
            <OrdersView />
          ) : activeModule.id === 'MARKET' ? (
            <MarketIntelligenceView />
          ) : activeModule.id === 'MONITORING' ? (
            <ProductMonitoringView onOpenMarketSearch={() => setActiveModuleId('MARKET')} />
          ) : activeModule.id === 'LISTING_ANALYTICS' ? (
            <ListingAnalyticsDashboardView />
          ) : activeModule.id === 'FINANCIAL' ? (
            <PricingAndCostsView />
          ) : activeModule.id === 'RECONCILIATION_DRE' ? (
            <FinancialFactsDashboardView />
          ) : activeModule.id === 'COMPANIES' ? (
            <CompaniesAndAccountsView />
          ) : activeModule.id === 'ASSISTANT' ? (
            <AiAssistantView />
          ) : activeModule.id === 'AI_STUDIO' ? (
            <DraftListingEditorView />
          ) : activeModule.id === 'REPORTS' ? (
            <ReportsExportView />
          ) : (
            <ModulePlaceholder
              moduleId={activeModule.id}
              moduleName={activeModule.name}
              pillar={activeModule.pillarName}
              description={activeModule.description}
              status={activeModule.status}
              provenience={activeModule.provenience}
            />
          )}

          {/* Footer Informativo */}
          <footer className="pt-8 border-t border-slate-200 dark:border-slate-800 text-center text-xs text-slate-500 dark:text-slate-400 space-y-1">
            <p>Seller Intelligence Platform &copy; 2026 &middot; Todos os direitos reservados.</p>
            <p className="text-[11px]">Monólito Modular &middot; Java 25 LTS &middot; Next.js 15 &middot; Fail-Closed Ativo</p>
          </footer>
        </main>
      </div>

      {/* =========================================================================
          3. DRAWER SLIDE-OVER MOBILE (Menu Hambúrguer)
      ========================================================================= */}
      {mobileMenuOpen && (
        <div
          role="dialog"
          aria-modal="true"
          aria-label="Menu principal de navegação móvel"
          className="lg:hidden fixed inset-0 z-50 flex"
        >
          {/* Backdrop Escuro */}
          <div
            onClick={() => setMobileMenuOpen(false)}
            className="fixed inset-0 bg-slate-950/70 backdrop-blur-sm transition-opacity"
          />

          {/* Painel do Menu */}
          <div className="relative w-4/5 max-w-xs bg-white dark:bg-slate-900 h-full p-4 flex flex-col justify-between shadow-2xl z-10 select-none">
            <div className="space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-slate-200 dark:border-slate-800">
                <span className="font-bold text-sm text-slate-900 dark:text-white">
                  Menu Principal
                </span>
                <button
                  onClick={() => setMobileMenuOpen(false)}
                  aria-label="Fechar menu"
                  className="p-2 text-slate-500 hover:text-slate-900 dark:hover:text-white focus:outline-none"
                >
                  ✕
                </button>
              </div>

              <nav className="space-y-1 max-h-[calc(100vh-10rem)] overflow-y-auto">
                {navModules.map((mod) => (
                  <button
                    key={mod.id}
                    onClick={() => {
                      setActiveModuleId(mod.id);
                      setMobileMenuOpen(false);
                    }}
                    className={`w-full text-left px-3 py-2.5 rounded-lg text-xs font-medium transition-colors flex items-center justify-between min-h-[44px] ${
                      mod.id === activeModuleId
                        ? 'bg-blue-600 text-white font-semibold'
                        : 'text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
                    }`}
                  >
                    <span className="truncate">{mod.name}</span>
                    <span className="text-[10px] opacity-75">{mod.pillarKey}</span>
                  </button>
                ))}
              </nav>
            </div>

            {isAuthenticated && (
              <div className="pt-3 border-t border-slate-200 dark:border-slate-800 space-y-2">
                <div className="text-xs text-slate-600 dark:text-slate-400 truncate">
                  Conectado como <span className="font-semibold text-slate-900 dark:text-white">{user?.email}</span>
                </div>
                <button
                  type="button"
                  onClick={logout}
                  className="w-full py-2.5 px-3 rounded-lg bg-rose-50 dark:bg-rose-950/40 text-rose-700 dark:text-rose-300 font-medium text-xs hover:bg-rose-100 dark:hover:bg-rose-900/50 transition-colors flex items-center justify-center gap-2 min-h-[44px]"
                >
                  Encerrar Sessão
                </button>
              </div>
            )}

            <div className="pt-3 border-t border-slate-200 dark:border-slate-800 text-[10px] text-slate-400 text-center">
              Seller Intelligence Platform &copy; 2026
            </div>
          </div>
        </div>
      )}

      {/* =========================================================================
          4. DOCK INFERIOR MOBILE (1-Thumb Touch Navigation)
      ========================================================================= */}
      <nav
        aria-label="Navegação rápida móvel"
        className="lg:hidden fixed bottom-0 inset-x-0 z-30 bg-white/95 dark:bg-slate-900/95 border-t border-slate-200 dark:border-slate-800 backdrop-blur-md px-1 flex items-center justify-around select-none h-16"
      >
        <button
          onClick={() => setActiveModuleId('ACTION_CENTER')}
          className={`flex flex-col items-center justify-center min-w-[56px] min-h-[48px] py-1 transition-colors ${
            activeModuleId === 'ACTION_CENTER'
              ? 'text-blue-600 dark:text-blue-400 font-semibold'
              : 'text-slate-500 dark:text-slate-400'
          }`}
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
          </svg>
          <span className="text-[10px] mt-0.5">Alertas</span>
        </button>

        <button
          onClick={() => setActiveModuleId('MARKET')}
          className={`flex flex-col items-center justify-center min-w-[56px] min-h-[48px] py-1 transition-colors ${
            activeModuleId === 'MARKET'
              ? 'text-blue-600 dark:text-blue-400 font-semibold'
              : 'text-slate-500 dark:text-slate-400'
          }`}
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <span className="text-[10px] mt-0.5">Mercado</span>
        </button>

        <button
          onClick={() => setActiveModuleId('FINANCIAL')}
          className={`flex flex-col items-center justify-center min-w-[56px] min-h-[48px] py-1 transition-colors ${
            activeModuleId === 'FINANCIAL'
              ? 'text-blue-600 dark:text-blue-400 font-semibold'
              : 'text-slate-500 dark:text-slate-400'
          }`}
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span className="text-[10px] mt-0.5">Finanças</span>
        </button>

        <button
          onClick={() => setActiveModuleId('AI_STUDIO')}
          className={`flex flex-col items-center justify-center min-w-[56px] min-h-[48px] py-1 transition-colors ${
            activeModuleId === 'AI_STUDIO'
              ? 'text-blue-600 dark:text-blue-400 font-semibold'
              : 'text-slate-500 dark:text-slate-400'
          }`}
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z" />
          </svg>
          <span className="text-[10px] mt-0.5">AI Studio</span>
        </button>

        <button
          onClick={() => setMobileMenuOpen(true)}
          className="flex flex-col items-center justify-center min-w-[56px] min-h-[48px] py-1 text-slate-500 dark:text-slate-400 transition-colors"
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
          </svg>
          <span className="text-[10px] mt-0.5">Menu</span>
        </button>
      </nav>
    </div>
  );
};
