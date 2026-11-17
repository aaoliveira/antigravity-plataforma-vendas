'use client';

import React, { useState, useMemo } from 'react';
import { CompanyManager, CompanyItem } from './CompanyManager';
import { StoreManager, StoreItem } from './StoreManager';
import { HealthChecker } from './HealthChecker';
import { MarketplaceCapabilities } from './MarketplaceCapabilities';
import { SyncManager } from './SyncManager';
import { MarketIntelligenceView } from './MarketIntelligenceView';
import { FinancialEngineView } from './FinancialEngineView';
import { FinancialDashboardView } from './FinancialDashboardView';
import { ListingStudioView } from './ListingStudioView';
import { CompetitorMonitoringView } from './CompetitorMonitoringView';
import { ActionCenterView } from './ActionCenterView';
import { ProductDiscoveryView } from './ProductDiscoveryView';
import { ReconciliationAndDreView } from './ReconciliationAndDreView';
import { ConversationalAssistantView } from './ConversationalAssistantView';
import { ComplianceDashboardView } from './ComplianceDashboardView';
import { SystemGuideView } from './SystemGuideView';

export type TabKey =
  | 'COMPANIES'
  | 'STORES'
  | 'CAPABILITIES'
  | 'SYNC'
  | 'MARKET'
  | 'MONITORING'
  | 'ACTION_CENTER'
  | 'DISCOVERY'
  | 'FINANCIAL'
  | 'RECONCILIATION_DRE'
  | 'AI_STUDIO'
  | 'ASSISTANT'
  | 'COMPLIANCE'
  | 'HEALTH'
  | 'GUIDE';

interface NavItem {
  id: TabKey;
  label: string;
  shortLabel?: string;
  badge?: string;
  description: string;
  pillar: 'BI' | 'MARKET' | 'AI' | 'FINANCE' | 'MANAGEMENT' | 'GOVERNANCE';
}

const initialDemoCompanies: CompanyItem[] = [
  {
    id: 'comp-1',
    cnpj: '06.990.590/0001-23',
    tradeName: 'Empresa Matriz Principal',
    legalName: 'Empresa Matriz Principal E-commerce Ltda',
    taxRegime: 'SIMPLES_NACIONAL',
    status: 'ACTIVE',
    createdAt: '2026-11-17T12:00:00.000Z',
  },
];

const initialDemoStores: StoreItem[] = [
  {
    id: 'store-1',
    companyId: 'comp-1',
    companyTradeName: 'Empresa Matriz Principal',
    marketplace: 'MERCADO_LIVRE',
    internalName: 'Mercado Livre - Loja Gold',
    externalAccountId: 'MLB10293847',
    connectionStatus: 'DISCONNECTED',
    status: 'ACTIVE',
    createdAt: '2026-11-17T12:00:00.000Z',
  },
];

export const DashboardView: React.FC = () => {
  const [activeTab, setActiveTab] = useState<TabKey>('COMPANIES');
  const [financialSubTab, setFinancialSubTab] = useState<'DASHBOARD' | 'ENGINE'>('DASHBOARD');
  const [companies, setCompanies] = useState<CompanyItem[]>(initialDemoCompanies);
  const [stores, setStores] = useState<StoreItem[]>(initialDemoStores);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [searchFilter, setSearchFilter] = useState('');

  const handleAddCompany = (newComp: Omit<CompanyItem, 'id' | 'createdAt'>) => {
    const comp: CompanyItem = {
      ...newComp,
      id: crypto.randomUUID ? crypto.randomUUID() : `comp-${Date.now()}`,
      createdAt: new Date().toISOString(),
    };
    setCompanies((prev) => [comp, ...prev]);
  };

  const handleAddStore = (newStore: Omit<StoreItem, 'id' | 'createdAt'>) => {
    const store: StoreItem = {
      ...newStore,
      id: crypto.randomUUID ? crypto.randomUUID() : `store-${Date.now()}`,
      createdAt: new Date().toISOString(),
    };
    setStores((prev) => [store, ...prev]);
  };

  const navItems: NavItem[] = useMemo(
    () => [
      // 1. Business Intelligence & Ações
      {
        id: 'ACTION_CENTER',
        label: 'Smart Alerts & Action Center',
        shortLabel: 'Alertas',
        badge: 'Fase 2',
        description: 'Inbox operacional de alertas, deduplicação e tarefas de seller',
        pillar: 'BI',
      },
      {
        id: 'MONITORING',
        label: 'Monitoramento & Concorrentes',
        shortLabel: 'Concorrentes',
        badge: 'Fase 2',
        description: 'Watchlists de produtos concorrentes e histórico temporal de preços',
        pillar: 'BI',
      },

      // 2. Market Intelligence
      {
        id: 'MARKET',
        label: 'Pesquisa de Mercado',
        shortLabel: 'Mercado',
        description: 'Descoberta de produtos, categorias, preços observados e tendências',
        pillar: 'MARKET',
      },
      {
        id: 'DISCOVERY',
        label: 'Product Discovery',
        shortLabel: 'Catálogos PDF',
        badge: 'Fase 2',
        description: 'Extração segura de catálogos PDF de fornecedores e busca por imagem',
        pillar: 'MARKET',
      },

      // 3. AI Commerce
      {
        id: 'AI_STUDIO',
        label: 'AI Listing Studio',
        shortLabel: 'AI Studio',
        description: 'Auditoria de score, geração assistida e otimização de anúncios',
        pillar: 'AI',
      },
      {
        id: 'ASSISTANT',
        label: 'Assistente & MCP',
        shortLabel: 'Assistente IA',
        badge: 'Fase 3',
        description: 'Assistente conversacional fundamentado e servidor MCP read-only',
        pillar: 'AI',
      },

      // 4. Seller Control & Finanças
      {
        id: 'FINANCIAL',
        label: 'Motor Financeiro',
        shortLabel: 'Finanças',
        description: 'Rentabilidade real, margens determinísticas em BigDecimal e simulador',
        pillar: 'FINANCE',
      },
      {
        id: 'RECONCILIATION_DRE',
        label: 'DRE & Conciliação',
        shortLabel: 'DRE & Repasses',
        badge: 'Fase 2',
        description: 'DRE gerencial (caixa vs competência) e conciliação de repasses',
        pillar: 'FINANCE',
      },

      // 5. Gestão & Integrações
      {
        id: 'COMPANIES',
        label: `Empresas & CNPJs (${companies.length})`,
        shortLabel: 'Empresas',
        badge: `${companies.length}`,
        description: 'Gestão de empresas, regimes tributários e isolamento multi-tenant',
        pillar: 'MANAGEMENT',
      },
      {
        id: 'STORES',
        label: `Lojas & Marketplaces (${stores.length})`,
        shortLabel: 'Lojas',
        badge: `${stores.length}`,
        description: 'Contas em Mercado Livre, Shopee, Amazon e canais homologados',
        pillar: 'MANAGEMENT',
      },
      {
        id: 'SYNC',
        label: 'Sincronização & Catálogo',
        shortLabel: 'Sincronização',
        description: 'Central assíncrona com RabbitMQ, webhooks e contingência CSV',
        pillar: 'MANAGEMENT',
      },
      {
        id: 'CAPABILITIES',
        label: 'Capacidades & Conectores',
        shortLabel: 'Conectores',
        description: 'Matriz oficial de recursos suportados por conector de marketplace',
        pillar: 'MANAGEMENT',
      },

      // 6. Governança & Sistema
      {
        id: 'COMPLIANCE',
        label: 'Compliance & Políticas',
        shortLabel: 'Compliance',
        badge: 'Fase 3',
        description: 'Monitoramento contínuo de políticas, Fail-Closed e reativação segura',
        pillar: 'GOVERNANCE',
      },
      {
        id: 'HEALTH',
        label: 'Diagnóstico de Infraestrutura',
        shortLabel: 'Diagnóstico',
        description: 'Métricas de saúde e conectividade com Postgres, RabbitMQ e Redis',
        pillar: 'GOVERNANCE',
      },
      {
        id: 'GUIDE',
        label: 'Guia do Sistema & Operação',
        shortLabel: 'Guia do Sistema',
        badge: 'Manual',
        description: 'Manual de funcionamento dos módulos, setup local, extensão e deploy',
        pillar: 'GOVERNANCE',
      },
    ],
    [companies.length, stores.length]
  );

  const pillars = [
    { key: 'BI', title: 'Business Intelligence & Ações', color: 'text-emerald-400', badgeColor: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30' },
    { key: 'MARKET', title: 'Market Intelligence', color: 'text-indigo-400', badgeColor: 'bg-indigo-500/10 text-indigo-400 border-indigo-500/30' },
    { key: 'AI', title: 'AI Commerce', color: 'text-purple-400', badgeColor: 'bg-purple-500/10 text-purple-400 border-purple-500/30' },
    { key: 'FINANCE', title: 'Seller Control & Finanças', color: 'text-teal-400', badgeColor: 'bg-teal-500/10 text-teal-400 border-teal-500/30' },
    { key: 'MANAGEMENT', title: 'Gestão & Integrações', color: 'text-blue-400', badgeColor: 'bg-blue-500/10 text-blue-400 border-blue-500/30' },
    { key: 'GOVERNANCE', title: 'Segurança & Governança', color: 'text-rose-400', badgeColor: 'bg-rose-500/10 text-rose-400 border-rose-500/30' },
  ];

  const currentItem = navItems.find((item) => item.id === activeTab) || navItems[0];
  const currentPillar = pillars.find((p) => p.key === currentItem.pillar) || pillars[0];

  const filteredItems = useMemo(() => {
    if (!searchFilter.trim()) return navItems;
    const term = searchFilter.toLowerCase();
    return navItems.filter(
      (item) =>
        item.label.toLowerCase().includes(term) ||
        item.description.toLowerCase().includes(term) ||
        item.pillar.toLowerCase().includes(term)
    );
  }, [navItems, searchFilter]);

  const selectTab = (tab: TabKey) => {
    setActiveTab(tab);
    setMobileMenuOpen(false);
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col w-full max-w-full overflow-x-hidden">
      {/* =========================================================================
          1. UNIFIED RESPONSIVE MASTER TOP BAR (Header)
      ========================================================================= */}
      <header className="sticky top-0 z-40 w-full h-14 sm:h-16 bg-slate-900/95 border-b border-slate-800/80 backdrop-blur-md px-3 sm:px-6 flex items-center justify-between select-none">
        {/* Left Side: Mobile Hamburger + Brand Logo + Title */}
        <div className="flex items-center gap-2.5 sm:gap-3 min-w-0">
          <button
            onClick={() => setMobileMenuOpen(true)}
            aria-label="Abrir menu de navegação"
            className="lg:hidden p-2 rounded-lg bg-slate-800/90 border border-slate-700/80 text-slate-300 hover:text-white min-w-[40px] min-h-[40px] flex items-center justify-center shrink-0 focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>

          <div className="flex h-9 w-9 sm:h-10 sm:w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-blue-600 via-indigo-600 to-purple-600 text-white font-black text-sm sm:text-base shadow-md shadow-blue-500/20 shrink-0">
            SI
          </div>

          <div className="min-w-0">
            <h1 className="text-sm sm:text-base font-bold text-white tracking-tight truncate flex items-center gap-1.5">
              <span>Seller Intelligence Platform</span>
            </h1>
            <p className="text-[10px] sm:text-[11px] text-slate-400 hidden sm:block truncate">
              SaaS Multi-tenant &middot; E-commerce & Marketplaces
            </p>
          </div>

          {/* Desktop Breadcrumb / Active Module Tag */}
          <div className="hidden xl:flex items-center gap-1.5 text-xs text-slate-400 border-l border-slate-800 pl-3 ml-2">
            <span>&gt;</span>
            <span className={`font-semibold ${currentPillar.color}`}>{currentPillar.title.split(' ')[0]}</span>
            <span>&gt;</span>
            <span className="text-slate-200 font-medium">{currentItem.shortLabel || currentItem.label}</span>
          </div>
        </div>

        {/* Right Side: Tenant info + Status pills */}
        <div className="flex items-center gap-2 sm:gap-3 shrink-0">
          <div className="hidden md:flex items-center gap-1.5 text-xs bg-slate-800/80 border border-slate-700/80 rounded-full px-3 py-1 text-slate-300">
            <span className="h-1.5 w-1.5 rounded-full bg-blue-400" />
            <span className="font-medium truncate max-w-[150px]">Matriz Principal</span>
          </div>

          <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-2.5 py-1 text-[11px] font-medium text-emerald-400">
            <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
            <span className="hidden sm:inline">Fail-Closed Protegido</span>
            <span className="sm:hidden">Protegido</span>
          </span>

          <span className="hidden sm:inline-flex items-center rounded-full border border-slate-700 bg-slate-800/80 px-2.5 py-1 text-[11px] font-mono text-slate-300">
            v1.2.0
          </span>
        </div>
      </header>

      {/* =========================================================================
          2. WORKSPACE CONTAINER (Sidebar + Main Area)
      ========================================================================= */}
      <div className="flex flex-1 w-full max-w-full overflow-x-hidden">
        {/* Desktop Sidebar (lg: screens) */}
        <aside className="hidden lg:flex lg:flex-col lg:w-64 xl:w-72 shrink-0 bg-slate-900/95 border-r border-slate-800/80 sticky top-14 sm:top-16 h-[calc(100vh-3.5rem)] sm:h-[calc(100vh-4rem)] overflow-y-auto p-3.5 flex-col justify-between select-none">
          <div className="space-y-4">
            {/* Quick Filter Input */}
            <div className="relative">
              <input
                type="text"
                placeholder="Buscar módulo..."
                value={searchFilter}
                onChange={(e) => setSearchFilter(e.target.value)}
                className="w-full rounded-lg bg-slate-950/90 border border-slate-800 px-3 py-2 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
              />
              {searchFilter && (
                <button
                  onClick={() => setSearchFilter('')}
                  className="absolute right-2.5 top-2 text-xs text-slate-500 hover:text-slate-300"
                >
                  ✕
                </button>
              )}
            </div>

            {/* Categorized Pillar Menus */}
            <nav className="space-y-4">
              {pillars.map((pillar) => {
                const itemsInPillar = filteredItems.filter((item) => item.pillar === pillar.key);
                if (itemsInPillar.length === 0) return null;

                return (
                  <div key={pillar.key} className="space-y-1">
                    <div className="px-2 py-1 text-[10px] font-bold uppercase tracking-wider text-slate-400 flex items-center justify-between">
                      <span>{pillar.title}</span>
                    </div>

                    <div className="space-y-0.5">
                      {itemsInPillar.map((item) => {
                        const isActive = activeTab === item.id;
                        return (
                          <button
                            key={item.id}
                            onClick={() => selectTab(item.id)}
                            className={`w-full text-left px-3 py-2 rounded-lg text-xs font-medium transition-all flex items-center justify-between group ${
                              isActive
                                ? 'bg-blue-600/20 text-blue-400 font-semibold border-l-2 border-blue-500 shadow-sm'
                                : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
                            }`}
                          >
                            <span className="truncate">{item.label}</span>
                            {item.badge && (
                              <span
                                className={`text-[10px] px-1.5 py-0.5 rounded font-medium border ${
                                  isActive
                                    ? 'bg-blue-500/20 text-blue-300 border-blue-500/30'
                                    : 'bg-slate-800 text-slate-400 border-slate-700'
                                }`}
                              >
                                {item.badge}
                              </span>
                            )}
                          </button>
                        );
                      })}
                    </div>
                  </div>
                );
              })}
            </nav>
          </div>

          {/* Sidebar Footer */}
          <div className="pt-3 border-t border-slate-800/80 px-2 space-y-1.5 text-xs text-slate-400">
            <div className="flex items-center justify-between text-[11px]">
              <span className="flex items-center gap-1.5 text-emerald-400 font-medium">
                <span className="h-1.5 w-1.5 rounded-full bg-emerald-400" />
                <span>Fail-Closed Ativo</span>
              </span>
              <span className="text-[10px] text-slate-400 font-mono">v1.2.0</span>
            </div>
            <p className="text-[10px] text-slate-400 leading-tight">
              APIs oficiais e isolamento multi-tenant determinístico.
            </p>
          </div>
        </aside>

        {/* Mobile Slide-Over Navigation Drawer */}
        {mobileMenuOpen && (
          <div className="lg:hidden fixed inset-0 z-50 flex">
            {/* Backdrop */}
            <div
              className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm transition-opacity"
              onClick={() => setMobileMenuOpen(false)}
            />

            {/* Drawer Panel */}
            <div className="relative w-80 max-w-[85vw] bg-slate-900 border-r border-slate-800 flex flex-col p-4 shadow-2xl z-50 overflow-y-auto">
              <div className="flex items-center justify-between pb-3 border-b border-slate-800">
                <div className="flex items-center gap-2.5">
                  <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-600 font-bold text-white shadow-md">
                    SI
                  </div>
                  <div>
                    <h2 className="text-sm font-bold text-white">Seller Intelligence</h2>
                    <p className="text-[11px] text-slate-400">Menu Principal</p>
                  </div>
                </div>
                <button
                  onClick={() => setMobileMenuOpen(false)}
                  className="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 min-w-[36px] min-h-[36px] flex items-center justify-center"
                  aria-label="Fechar menu"
                >
                  ✕
                </button>
              </div>

              {/* Mobile Search Filter */}
              <div className="mt-3">
                <input
                  type="text"
                  placeholder="Filtrar opções..."
                  value={searchFilter}
                  onChange={(e) => setSearchFilter(e.target.value)}
                  className="w-full rounded-lg bg-slate-950 border border-slate-800 px-3 py-2 text-xs text-slate-200 focus:outline-none focus:border-blue-500"
                />
              </div>

              {/* Mobile Navigation List */}
              <div className="mt-4 space-y-4 flex-1">
                {pillars.map((pillar) => {
                  const itemsInPillar = filteredItems.filter((item) => item.pillar === pillar.key);
                  if (itemsInPillar.length === 0) return null;

                  return (
                    <div key={pillar.key} className="space-y-1">
                      <div className="text-[10px] font-bold uppercase tracking-wider text-slate-400 px-1">
                        {pillar.title}
                      </div>
                      <div className="space-y-1">
                        {itemsInPillar.map((item) => {
                          const isActive = activeTab === item.id;
                          return (
                            <button
                              key={item.id}
                              onClick={() => selectTab(item.id)}
                              className={`w-full text-left px-3 py-2.5 rounded-lg text-xs font-medium flex items-center justify-between transition-colors min-h-[44px] ${
                                isActive
                                  ? 'bg-blue-600 text-white font-semibold'
                                  : 'text-slate-300 hover:bg-slate-800'
                              }`}
                            >
                              <span className="truncate">{item.label}</span>
                              {item.badge && (
                                <span
                                  className={`text-[10px] px-1.5 py-0.5 rounded font-mono ${
                                    isActive ? 'bg-blue-700 text-white' : 'bg-slate-800 text-slate-400'
                                  }`}
                                >
                                  {item.badge}
                                </span>
                              )}
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        )}

        {/* =========================================================================
            3. MAIN CONTENT CONTAINER (Zero negative margins, fluid responsiveness)
        ========================================================================= */}
        <main className="flex-1 min-w-0 w-full max-w-full px-3 sm:px-6 lg:px-8 py-4 sm:py-6 space-y-6 pb-28 lg:pb-12 overflow-x-hidden">
          {/* Dynamic Contextual Header / Banner */}
          {activeTab === 'COMPANIES' ? (
            <div className="rounded-2xl border border-slate-800 bg-gradient-to-br from-slate-900 via-slate-900/80 to-slate-950 p-5 sm:p-8 shadow-xl">
              <div className="max-w-3xl">
                <span className="inline-block rounded-md bg-blue-500/10 px-2.5 py-1 text-xs font-semibold text-blue-400 border border-blue-500/20">
                  Fase 1 — MVP &middot; Gestão de Organizações, CNPJs e Lojas
                </span>
                <h1 className="mt-3 text-xl sm:text-3xl font-extrabold tracking-tight text-white leading-tight">
                  Estrutura Organizacional e Gestão de Acessos
                </h1>
                <p className="mt-2 text-xs sm:text-base text-slate-300 leading-relaxed">
                  Controle multi-loja e multi-CNPJ com isolamento determinístico por tenant, parametrização informativa de regime tributário e governança granular de escopos.
                </p>
              </div>
            </div>
          ) : (
            <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-4 sm:p-6 shadow-lg backdrop-blur-sm">
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                <div className="min-w-0">
                  <div className="flex items-center gap-1.5 text-xs text-slate-400 mb-1 flex-wrap">
                    <span>Plataforma</span>
                    <span>&gt;</span>
                    <span className={`font-semibold ${currentPillar.color}`}>{currentPillar.title}</span>
                    <span>&gt;</span>
                    <span className="text-slate-200 font-medium">{currentItem.label}</span>
                  </div>
                  <h1 className="text-lg sm:text-2xl font-bold tracking-tight text-white truncate">
                    {currentItem.label}
                  </h1>
                  <p className="mt-1 text-xs sm:text-sm text-slate-300">
                    {currentItem.description}
                  </p>
                </div>

                <div className="shrink-0 self-start sm:self-center">
                  <span className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium border ${currentPillar.badgeColor}`}>
                    <span className="h-1.5 w-1.5 rounded-full bg-current animate-pulse" />
                    <span>{currentPillar.title.split(' ')[0]}</span>
                  </span>
                </div>
              </div>
            </div>
          )}

          {/* Quick Horizontal Carousel on Mobile (Swipe-friendly) */}
          <div className="flex sm:hidden overflow-x-auto gap-2 pb-1 scrollbar-none text-xs touch-scroll">
            {navItems.slice(0, 7).map((item) => (
              <button
                key={item.id}
                onClick={() => selectTab(item.id)}
                className={`whitespace-nowrap px-3 py-1.5 rounded-full border transition-colors shrink-0 min-h-[32px] ${
                  activeTab === item.id
                    ? 'bg-blue-600 text-white border-blue-500 font-semibold'
                    : 'bg-slate-900 text-slate-400 border-slate-800 hover:text-slate-200'
                }`}
              >
                {item.shortLabel || item.label}
              </button>
            ))}
          </div>

          {/* Financial Sub-navigation (When in Financial Tab) */}
          {activeTab === 'FINANCIAL' && (
            <div className="flex flex-wrap items-center gap-2 p-1.5 bg-slate-900/90 rounded-xl border border-slate-800 w-full sm:w-fit">
              <button
                onClick={() => setFinancialSubTab('DASHBOARD')}
                className={`flex-1 sm:flex-initial px-3 sm:px-4 py-2 text-xs sm:text-sm font-semibold rounded-lg transition-all min-h-[38px] ${
                  financialSubTab === 'DASHBOARD'
                    ? 'bg-blue-600 text-white shadow-md'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                Painel Financeiro &amp; Rentabilidade
              </button>
              <button
                onClick={() => setFinancialSubTab('ENGINE')}
                className={`flex-1 sm:flex-initial px-3 sm:px-4 py-2 text-xs sm:text-sm font-semibold rounded-lg transition-all min-h-[38px] ${
                  financialSubTab === 'ENGINE'
                    ? 'bg-blue-600 text-white shadow-md'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                Simulador &amp; Regras de Preço
              </button>
            </div>
          )}

          {/* =========================================================================
              4. ACTIVE TAB COMPONENT RENDERING
          ========================================================================= */}
          <div className="w-full max-w-full overflow-x-hidden">
            {activeTab === 'COMPANIES' && (
              <CompanyManager
                initialCompanies={companies}
                onAddCompany={handleAddCompany}
              />
            )}

            {activeTab === 'STORES' && (
              <StoreManager
                companies={companies}
                initialStores={stores}
                onAddStore={handleAddStore}
              />
            )}

            {activeTab === 'CAPABILITIES' && <MarketplaceCapabilities />}

            {activeTab === 'SYNC' && <SyncManager />}

            {activeTab === 'MARKET' && <MarketIntelligenceView />}

            {activeTab === 'MONITORING' && <CompetitorMonitoringView />}

            {activeTab === 'ACTION_CENTER' && <ActionCenterView />}

            {activeTab === 'DISCOVERY' && <ProductDiscoveryView />}

            {activeTab === 'FINANCIAL' && (
              <div>
                {financialSubTab === 'DASHBOARD' ? <FinancialDashboardView /> : <FinancialEngineView />}
              </div>
            )}

            {activeTab === 'RECONCILIATION_DRE' && <ReconciliationAndDreView />}

            {activeTab === 'AI_STUDIO' && <ListingStudioView />}

            {activeTab === 'ASSISTANT' && <ConversationalAssistantView />}

            {activeTab === 'COMPLIANCE' && <ComplianceDashboardView />}

            {activeTab === 'HEALTH' && (
              <div className="space-y-6">
                <HealthChecker />

                {/* Infrastructure Matrix Overview */}
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
                  <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-4 shadow-sm">
                    <div className="text-[11px] uppercase font-bold tracking-wider text-blue-400">Backend Core</div>
                    <div className="mt-1.5 text-base font-bold text-white">Java 25 LTS / Spring Boot 3.5</div>
                    <p className="mt-1 text-xs text-slate-400">Spring Modulith, ArchUnit, Flyway e Bean Validation.</p>
                  </div>

                  <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-4 shadow-sm">
                    <div className="text-[11px] uppercase font-bold tracking-wider text-emerald-400">Frontend Web</div>
                    <div className="mt-1.5 text-base font-bold text-white">Next.js 15 / React 19</div>
                    <p className="mt-1 text-xs text-slate-400">App Router, TypeScript Strict, Tailwind CSS e Vitest.</p>
                  </div>

                  <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-4 shadow-sm">
                    <div className="text-[11px] uppercase font-bold tracking-wider text-purple-400">Persistência ACID</div>
                    <div className="mt-1.5 text-base font-bold text-white">PostgreSQL 18.x</div>
                    <p className="mt-1 text-xs text-slate-400">NUMERIC determinístico para finanças e isolamento multi-tenant.</p>
                  </div>

                  <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-4 shadow-sm">
                    <div className="text-[11px] uppercase font-bold tracking-wider text-amber-400">Mensageria & Filas</div>
                    <div className="mt-1.5 text-base font-bold text-white">RabbitMQ 4.x / Redis 7</div>
                    <p className="mt-1 text-xs text-slate-400">Processamento assíncrono idempotente com Dead-Letter Queues.</p>
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'GUIDE' && <SystemGuideView />}
          </div>

          {/* Footer inside content area */}
          <footer className="pt-8 border-t border-slate-800/60 text-center text-xs text-slate-400 space-y-1">
            <p>Seller Intelligence Platform &copy; 2026 &middot; Todos os direitos reservados.</p>
            <p className="text-[11px] text-slate-400">Monólito Modular &middot; Java 25 LTS &middot; Next.js 15 &middot; PostgreSQL 18 &middot; Fail-Closed Protegido</p>
          </footer>
        </main>
      </div>

      {/* =========================================================================
          5. MOBILE BOTTOM NAVIGATION DOCK (1-thumb touch navigation)
      ========================================================================= */}
      <nav
        aria-label="Navegação rápida móvel"
        className="lg:hidden fixed bottom-0 inset-x-0 z-30 bg-slate-900/95 border-t border-slate-800 backdrop-blur-md px-1 flex items-center justify-around select-none h-16"
      >
        <button
          onClick={() => selectTab('ACTION_CENTER')}
          className={`flex flex-col items-center justify-center min-w-[56px] min-h-[48px] py-1 transition-colors ${
            activeTab === 'ACTION_CENTER' ? 'text-emerald-400 font-semibold' : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
          </svg>
          <span className="text-[10px] mt-0.5">Alertas</span>
        </button>

        <button
          onClick={() => selectTab('MARKET')}
          className={`flex flex-col items-center justify-center min-w-[56px] min-h-[48px] py-1 transition-colors ${
            activeTab === 'MARKET' ? 'text-indigo-400 font-semibold' : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
          </svg>
          <span className="text-[10px] mt-0.5">Mercado</span>
        </button>

        <button
          onClick={() => selectTab('FINANCIAL')}
          className={`flex flex-col items-center justify-center min-w-[56px] min-h-[48px] py-1 transition-colors ${
            activeTab === 'FINANCIAL' ? 'text-teal-400 font-semibold' : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <span className="text-[10px] mt-0.5">Finanças</span>
        </button>

        <button
          onClick={() => selectTab('AI_STUDIO')}
          className={`flex flex-col items-center justify-center min-w-[56px] min-h-[48px] py-1 transition-colors ${
            activeTab === 'AI_STUDIO' ? 'text-purple-400 font-semibold' : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 3v4M3 5h4M6 17v4m-2-2h4m5-16l2.286 6.857L21 12l-5.714 2.143L13 21l-2.286-6.857L5 12l5.714-2.143L13 3z" />
          </svg>
          <span className="text-[10px] mt-0.5">AI Studio</span>
        </button>

        <button
          onClick={() => setMobileMenuOpen(true)}
          className="flex flex-col items-center justify-center min-w-[56px] min-h-[48px] py-1 text-slate-400 hover:text-slate-200 transition-colors"
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
