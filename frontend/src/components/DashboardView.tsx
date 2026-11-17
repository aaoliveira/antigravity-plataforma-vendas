'use client';

import React, { useState } from 'react';
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
  const [activeTab, setActiveTab] = useState<'COMPANIES' | 'STORES' | 'CAPABILITIES' | 'SYNC' | 'MARKET' | 'MONITORING' | 'ACTION_CENTER' | 'DISCOVERY' | 'FINANCIAL' | 'RECONCILIATION_DRE' | 'AI_STUDIO' | 'ASSISTANT' | 'HEALTH'>('COMPANIES');
  const [financialSubTab, setFinancialSubTab] = useState<'DASHBOARD' | 'ENGINE'>('DASHBOARD');
  const [companies, setCompanies] = useState<CompanyItem[]>(initialDemoCompanies);
  const [stores, setStores] = useState<StoreItem[]>(initialDemoStores);

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

  return (
    <div className="space-y-6">
      {/* Tab Navigation */}
      <div className="flex border-b border-slate-700 space-x-6 text-sm font-medium">
        <button
          onClick={() => setActiveTab('COMPANIES')}
          className={`pb-3 px-1 border-b-2 transition-colors ${
            activeTab === 'COMPANIES'
              ? 'border-blue-500 text-blue-400 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          Empresas & CNPJs ({companies.length})
        </button>
        <button
          onClick={() => setActiveTab('STORES')}
          className={`pb-3 px-1 border-b-2 transition-colors ${
            activeTab === 'STORES'
              ? 'border-blue-500 text-blue-400 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          Lojas & Marketplaces ({stores.length})
        </button>
        <button
          onClick={() => setActiveTab('CAPABILITIES')}
          className={`pb-3 px-1 border-b-2 transition-colors ${
            activeTab === 'CAPABILITIES'
              ? 'border-blue-500 text-blue-400 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          Capacidades &amp; Conectores
        </button>
        <button
          onClick={() => setActiveTab('SYNC')}
          className={`pb-3 px-1 border-b-2 transition-colors ${
            activeTab === 'SYNC'
              ? 'border-blue-500 text-blue-400 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          Sincronização &amp; Catálogo
        </button>
        <button
          onClick={() => setActiveTab('MARKET')}
          className={`pb-3 px-1 border-b-2 transition-colors ${
            activeTab === 'MARKET'
              ? 'border-blue-500 text-blue-400 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          Pesquisa de Mercado
        </button>
        <button
          onClick={() => setActiveTab('MONITORING')}
          className={`pb-3 px-1 border-b-2 transition-colors flex items-center gap-1.5 ${
            activeTab === 'MONITORING'
              ? 'border-indigo-500 text-indigo-400 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <span>Monitoramento &amp; Concorrentes</span>
          <span className="text-[10px] px-1.5 py-0.2 rounded bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">Fase 2</span>
        </button>
        <button
          onClick={() => setActiveTab('ACTION_CENTER')}
          className={`pb-3 px-1 border-b-2 transition-colors flex items-center gap-1.5 ${
            activeTab === 'ACTION_CENTER'
              ? 'border-emerald-500 text-emerald-400 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <span>Smart Alerts &amp; Action Center</span>
          <span className="text-[10px] px-1.5 py-0.2 rounded bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">Fase 2</span>
        </button>
        <button
          onClick={() => setActiveTab('DISCOVERY')}
          className={`pb-3 px-1 border-b-2 transition-colors flex items-center gap-1.5 ${
            activeTab === 'DISCOVERY'
              ? 'border-amber-500 text-amber-400 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <span>Product Discovery</span>
          <span className="text-[10px] px-1.5 py-0.2 rounded bg-amber-500/20 text-amber-300 border border-amber-500/30">Fase 2</span>
        </button>
        <button
          onClick={() => setActiveTab('FINANCIAL')}
          className={`pb-3 px-1 border-b-2 transition-colors ${
            activeTab === 'FINANCIAL'
              ? 'border-blue-500 text-blue-400 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          Motor Financeiro
        </button>
        <button
          onClick={() => setActiveTab('RECONCILIATION_DRE')}
          className={`pb-3 px-1 border-b-2 transition-colors flex items-center gap-1.5 ${
            activeTab === 'RECONCILIATION_DRE'
              ? 'border-teal-500 text-teal-400 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <span>DRE &amp; Conciliação</span>
          <span className="text-[10px] px-1.5 py-0.2 rounded bg-teal-500/20 text-teal-300 border border-teal-500/30">Fase 2</span>
        </button>
        <button
          onClick={() => setActiveTab('AI_STUDIO')}
          className={`pb-3 px-1 border-b-2 transition-colors ${
            activeTab === 'AI_STUDIO'
              ? 'border-purple-500 text-purple-400 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          AI Listing Studio
        </button>
        <button
          onClick={() => setActiveTab('ASSISTANT')}
          className={`pb-3 px-1 border-b-2 transition-colors flex items-center gap-1.5 ${
            activeTab === 'ASSISTANT'
              ? 'border-violet-500 text-violet-400 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          <span>Assistente &amp; MCP</span>
          <span className="text-[10px] px-1.5 py-0.2 rounded bg-violet-500/20 text-violet-300 border border-violet-500/30">Fase 3</span>
        </button>
        <button
          onClick={() => setActiveTab('HEALTH')}
          className={`pb-3 px-1 border-b-2 transition-colors ${
            activeTab === 'HEALTH'
              ? 'border-blue-500 text-blue-400 font-semibold'
              : 'border-transparent text-slate-400 hover:text-slate-200'
          }`}
        >
          Diagnóstico de Infraestrutura
        </button>
      </div>

      {/* Tab Content */}
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
        <div className="space-y-6">
          <div className="flex bg-slate-800 p-1 rounded-lg w-fit text-xs font-semibold text-slate-300">
            <button
              onClick={() => setFinancialSubTab('DASHBOARD')}
              className={`px-3 py-1.5 rounded-md transition-colors ${
                financialSubTab === 'DASHBOARD' ? 'bg-blue-600 text-white shadow' : 'hover:text-white'
              }`}
            >
              Painel &amp; Relatórios
            </button>
            <button
              onClick={() => setFinancialSubTab('ENGINE')}
              className={`px-3 py-1.5 rounded-md transition-colors ${
                financialSubTab === 'ENGINE' ? 'bg-blue-600 text-white shadow' : 'hover:text-white'
              }`}
            >
              Simulador &amp; Regras
            </button>
          </div>
          {financialSubTab === 'DASHBOARD' ? <FinancialDashboardView /> : <FinancialEngineView />}
        </div>
      )}

      {activeTab === 'RECONCILIATION_DRE' && <ReconciliationAndDreView />}

      {activeTab === 'AI_STUDIO' && <ListingStudioView />}

      {activeTab === 'ASSISTANT' && <ConversationalAssistantView />}

      {activeTab === 'HEALTH' && <HealthChecker />}
    </div>
  );
};
