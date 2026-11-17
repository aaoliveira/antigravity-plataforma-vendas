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
  | 'HEALTH';

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
    createdAt: new Date().toISOString(),
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
    createdAt: new Date().toISOString(),
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
        shortLabel: 'Alertas & Tarefas',
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
        shortLabel: 'Pesquisa de Mercado',
        description: 'Descoberta de produtos, categorias, preços observados e tendências',
        pillar: 'MARKET',
      },
      {
        id: 'DISCOVERY',
        label: 'Product Discovery',
        shortLabel: 'Catálogos PDF & Imagem',
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
        shortLabel: 'Assistente IA & MCP',
        badge: 'Fase 3',
        description: 'Assistente conversacional fundamentado e servidor MCP read-only',
        pillar: 'AI',
      },

      // 4. Seller Control & Finanças
      {
        id: 'FINANCIAL',
        label: 'Motor Financeiro',
        shortLabel: 'Finanças & Margem',
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
        shortLabel: 'Empresas & CNPJs',
        badge: `${companies.length}`,
        description: 'Gestão de empresas, regimes tributários e isolamento multi-tenant',
        pillar: 'MANAGEMENT',
      },
      {
        id: 'STORES',
        label: `Lojas & Marketplaces (${stores.length})`,
        shortLabel: 'Lojas Conectadas',
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
        shortLabel: 'Capacidades',
        description: 'Matriz oficial de recursos suportados por conector de marketplace',
        pillar: 'MANAGEMENT',
      },

      // 6. Governança & Sistema
      {
        id: 'COMPLIANCE',
        label: 'Compliance & Políticas',
        shortLabel: 'Governança & Termos',
        badge: 'Fase 3',
        description: 'Monitoramento contínuo de políticas, Fail-Closed e reativação segura',
        pillar: 'GOVERNANCE',
      },
      {
        id: 'HEALTH',
        label: 'Diagnóstico de Infraestrutura',
        shortLabel: 'Status do Sistema',
        description: 'Métricas de saúde e conectividade com Postgres, RabbitMQ e Redis',
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
    <div className="flex flex-col lg:flex-row min-h-screen bg-slate-950 text-slate-100 -mx-3 -my-4 sm:-mx-6 sm:-my-6 lg:-mx-8 lg:-my-8">
      {/* =========================================================================
          1. DESKTOP SIDEBAR (Visible on lg: screens and up)
      ========================================================================= */}
      <aside className="hidden lg:flex lg:flex-col lg:w-72 lg:shrink-0 bg-slate-900/90 border-r border-slate-800/80 sticky top-0 h-screen overflow-y-auto p-4 justify-between select-none">
        <div className="space-y-4">
          {/* Brand & Workspace Info */}
          <div className="flex items-center gap-3 px-2 py-1">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-tr from-blue-600 via-indigo-600 to-purple-600 text-white font-black text-lg shadow-lg shadow-blue-500/20">
              SI
            </div>
            <div>
              <div className="text-sm font-bold text-white tracking-tight flex items-center gap-1.5">
                <span>Seller Intelligence</span>
                <span className="text-[10px] uppercase font-semibold px-1.5 py-0.5 rounded bg-blue-500/10 text-blue-400 border border-blue-500/20">PRO</span>
              </div>
              <div className="text-xs text-slate-400 flex items-center gap-1">
                <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
                <span>Multi-tenant Conectado</span>
              </div>
            </div>
          </div>

          {/* Quick Filter Search Input */}
          <div className="relative px-1">
            <input
              type="text"
              placeholder="Buscar módulos e ferramentas..."
              value={searchFilter}
              onChange={(e) => setSearchFilter(e.target.value)}
              className="w-full rounded-lg bg-slate-950/80 border border-slate-800 px-3 py-2 text-xs text-slate-200 placeholder-slate-500 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500"
            />
            {searchFilter && (
              <button
                onClick={() => setSearchFilter('')}
                className="absolute right-3 top-2.5 text-xs text-slate-500 hover:text-slate-300"
              >
                ✕
              </button>
            )}
          </div>

          {/* Categorized Pillar Menus */}
          <nav className="space-y-4 pt-1">
            {pillars.map((pillar) => {
              const itemsInPillar = filteredItems.filter((item) => item.pillar === pillar.key);
              if (itemsInPillar.length === 0) return null;

              return (
                <div key={pillar.key} className="space-y-1">
                  <div className="px-2 py-1 text-[11px] font-bold uppercase tracking-wider text-slate-400 flex items-center justify-between">
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
                              ? 'bg-blue-600/15 text-blue-400 font-semibold border-l-2 border-blue-500 shadow-sm'
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
        <div className="pt-4 border-t border-slate-800/80 px-2 space-y-2 text-xs text-slate-500">
          <div className="flex items-center justify-between">
            <span className="flex items-center gap-1.5">
              <span className="h-2 w-2 rounded-full bg-emerald-400" />
              <span>Proteção Fail-Closed</span>
            </span>
            <span className="text-[10px] text-slate-400 font-mono">v1.2.0</span>
          </div>
          <p className="text-[11px] text-slate-400 leading-snug">
            Plataforma homologada em conformidade com termos oficiais de marketplaces.
          </p>
        </div>
      </aside>

      {/* =========================================================================
          2. MOBILE TOP BAR & DRAWER (Visible on < lg: screens)
      ========================================================================= */}
      <div className="lg:hidden sticky top-0 z-40 bg-slate-900/95 border-b border-slate-800 backdrop-blur-md px-4 py-3 flex items-center justify-between">
        <div className="flex items-center gap-2.5">
          <button
            onClick={() => setMobileMenuOpen(true)}
            aria-label="Abrir menu de navegação"
            className="p-2 rounded-lg bg-slate-800/80 border border-slate-700/80 text-slate-300 hover:text-white focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
          <div>
            <div className="text-xs font-bold text-white flex items-center gap-1.5">
              <span className="truncate max-w-[160px] sm:max-w-none">{currentItem.shortLabel || currentItem.label}</span>
              <span className={`text-[10px] px-1.5 py-0.2 rounded border ${currentPillar.badgeColor}`}>
                {currentPillar.title.split(' ')[0]}
              </span>
            </div>
            <p className="text-[10px] text-slate-400 truncate max-w-[200px]">Tenant: Empresa Matriz Principal</p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 border border-emerald-500/20 px-2.5 py-1 text-[11px] font-medium text-emerald-400">
            <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
            <span className="hidden sm:inline">Online</span>
          </span>
        </div>
      </div>

      {/* Mobile Slide-Over Drawer */}
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
                className="p-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800"
                aria-label="Fechar menu"
              >
                ✕
              </button>
            </div>

            {/* Mobile Filter Input */}
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
                    <div className="text-[11px] font-bold uppercase tracking-wider text-slate-400 px-1">
                      {pillar.title}
                    </div>
                    <div className="space-y-1">
                      {itemsInPillar.map((item) => {
                        const isActive = activeTab === item.id;
                        return (
                          <button
                            key={item.id}
                            onClick={() => selectTab(item.id)}
                            className={`w-full text-left px-3 py-2.5 rounded-lg text-xs font-medium flex items-center justify-between transition-colors ${
                              isActive
                                ? 'bg-blue-600 text-white font-semibold'
                                : 'text-slate-300 hover:bg-slate-800'
                            }`}
                          >
                            <span>{item.label}</span>
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
          3. MAIN CONTENT CONTAINER
      ========================================================================= */}
      <main className="flex-1 min-w-0 p-4 sm:p-6 lg:p-8 space-y-6 pb-24 lg:pb-12">
        {/* Dynamic Contextual Header / Banner */}
        {activeTab === 'COMPANIES' ? (
          <div className="rounded-2xl border border-slate-800 bg-gradient-to-br from-slate-900 via-slate-900/80 to-slate-950 p-6 sm:p-8 shadow-xl">
            <div className="max-w-3xl">
              <span className="inline-block rounded-md bg-blue-500/10 px-2.5 py-1 text-xs font-semibold text-blue-400 border border-blue-500/20">
                Fase 1 — MVP &middot; Gestão de Organizações, CNPJs e Lojas
              </span>
              <h1 className="mt-3 text-2xl sm:text-3xl font-extrabold tracking-tight text-white">
                Estrutura Organizacional e Gestão de Acessos
              </h1>
              <p className="mt-2 text-sm sm:text-base text-slate-300 leading-relaxed">
                Controle multi-loja e multi-CNPJ com isolamento determinístico por tenant, parametrização informativa de regime tributário e governança granular de escopos.
              </p>
            </div>
          </div>
        ) : (
          <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-5 sm:p-6 shadow-lg backdrop-blur-sm">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
              <div>
                <div className="flex items-center gap-2 text-xs text-slate-400 mb-1">
                  <span>Plataforma</span>
                  <span>&gt;</span>
                  <span className={`font-semibold ${currentPillar.color}`}>{currentPillar.title}</span>
                  <span>&gt;</span>
                  <span className="text-slate-200 font-medium">{currentItem.label}</span>
                </div>
                <h1 className="text-xl sm:text-2xl font-bold tracking-tight text-white">
                  {currentItem.label}
                </h1>
                <p className="mt-1 text-xs sm:text-sm text-slate-300">
                  {currentItem.description}
                </p>
              </div>

              {/* Pillar Status Badge */}
              <div className="shrink-0">
                <span className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium border ${currentPillar.badgeColor}`}>
                  <span className="h-2 w-2 rounded-full bg-current animate-pulse" />
                  <span>{currentPillar.title.split(' ')[0]}</span>
                </span>
              </div>
            </div>
          </div>
        )}

        {/* Quick Horizontal Carousel for Small Viewports */}
        <div className="flex sm:hidden overflow-x-auto gap-2 pb-2 scrollbar-none text-xs">
          {navItems.slice(0, 7).map((item) => (
            <button
              key={item.id}
              onClick={() => selectTab(item.id)}
              className={`whitespace-nowrap px-3 py-1.5 rounded-full border transition-colors ${
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
          <div className="flex flex-wrap items-center gap-2 p-1.5 bg-slate-900/90 rounded-xl border border-slate-800 w-fit">
            <button
              onClick={() => setFinancialSubTab('DASHBOARD')}
              className={`px-4 py-2 text-xs sm:text-sm font-semibold rounded-lg transition-all ${
                financialSubTab === 'DASHBOARD'
                  ? 'bg-blue-600 text-white shadow-md'
                  : 'text-slate-400 hover:text-white'
              }`}
            >
              Painel Financeiro &amp; Rentabilidade
            </button>
            <button
              onClick={() => setFinancialSubTab('ENGINE')}
              className={`px-4 py-2 text-xs sm:text-sm font-semibold rounded-lg transition-all ${
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
        <div className="transition-all duration-150">
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
        </div>
      </main>

      {/* =========================================================================
          5. MOBILE BOTTOM NAVIGATION (Visible only on mobile devices)
      ========================================================================= */}
      <nav
        aria-label="Navegação rápida móvel"
        className="lg:hidden fixed bottom-0 inset-x-0 z-40 bg-slate-900/95 border-t border-slate-800 backdrop-blur-md px-2 py-2 flex items-center justify-around select-none"
      >
        <button
          onClick={() => selectTab('ACTION_CENTER')}
          className={`flex flex-col items-center justify-center min-w-[56px] py-1 transition-colors ${
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
          className={`flex flex-col items-center justify-center min-w-[56px] py-1 transition-colors ${
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
          className={`flex flex-col items-center justify-center min-w-[56px] py-1 transition-colors ${
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
          className={`flex flex-col items-center justify-center min-w-[56px] py-1 transition-colors ${
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
          className="flex flex-col items-center justify-center min-w-[56px] py-1 text-slate-400 hover:text-slate-200 transition-colors"
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
