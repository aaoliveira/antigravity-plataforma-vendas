'use client';

import React, { useState } from 'react';
import { CompanyManager, CompanyItem } from './CompanyManager';
import { StoreManager, StoreItem } from './StoreManager';
import { HealthChecker } from './HealthChecker';

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
  const [activeTab, setActiveTab] = useState<'COMPANIES' | 'STORES' | 'HEALTH'>('COMPANIES');
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

      {activeTab === 'HEALTH' && <HealthChecker />}
    </div>
  );
};
