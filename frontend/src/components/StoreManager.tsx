'use client';

import React, { useState } from 'react';
import { CompanyItem } from './CompanyManager';

export interface StoreItem {
  id: string;
  companyId: string;
  companyTradeName: string;
  marketplace: 'MERCADO_LIVRE' | 'SHOPEE' | 'AMAZON' | 'OUTRO';
  internalName: string;
  externalAccountId?: string;
  connectionStatus: 'DISCONNECTED' | 'CONNECTED' | 'NEEDS_REAUTH' | 'PAUSED' | 'REVOKED';
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
}

interface StoreManagerProps {
  companies: CompanyItem[];
  initialStores?: StoreItem[];
  onAddStore?: (store: Omit<StoreItem, 'id' | 'createdAt'>) => void;
  onToggleStatus?: (id: string, newStatus: 'ACTIVE' | 'INACTIVE') => void;
}

export const StoreManager: React.FC<StoreManagerProps> = ({
  companies,
  initialStores = [],
  onAddStore,
  onToggleStatus,
}) => {
  const [stores, setStores] = useState<StoreItem[]>(initialStores);
  const [showForm, setShowForm] = useState(false);
  const [selectedCompanyFilter, setSelectedCompanyFilter] = useState<string>('ALL');

  // Form states
  const [companyId, setCompanyId] = useState(companies.length > 0 ? companies[0].id : '');
  const [marketplace, setMarketplace] = useState<StoreItem['marketplace']>('MERCADO_LIVRE');
  const [internalName, setInternalName] = useState('');
  const [externalAccountId, setExternalAccountId] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  const filteredStores = stores.filter((s) =>
    selectedCompanyFilter === 'ALL' ? true : s.companyId === selectedCompanyFilter
  );

  const handleCreateStore = (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (!companyId) {
      setFormError('Selecione uma empresa válida.');
      return;
    }

    if (!internalName.trim()) {
      setFormError('Nome Interno da loja é obrigatório.');
      return;
    }

    const company = companies.find((c) => c.id === companyId);
    if (!company) {
      setFormError('Empresa selecionada não encontrada.');
      return;
    }

    const newStore: StoreItem = {
      id: crypto.randomUUID ? crypto.randomUUID() : `store-${Date.now()}`,
      companyId,
      companyTradeName: company.tradeName,
      marketplace,
      internalName: internalName.trim(),
      externalAccountId: externalAccountId.trim() || undefined,
      connectionStatus: 'DISCONNECTED',
      status: 'ACTIVE',
      createdAt: new Date().toISOString(),
    };

    setStores((prev) => [newStore, ...prev]);
    if (onAddStore) {
      onAddStore(newStore);
    }

    setInternalName('');
    setExternalAccountId('');
    setShowForm(false);
  };

  const handleToggle = (id: string, currentStatus: 'ACTIVE' | 'INACTIVE') => {
    const nextStatus = currentStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    setStores((prev) =>
      prev.map((s) => (s.id === id ? { ...s, status: nextStatus } : s))
    );
    if (onToggleStatus) {
      onToggleStatus(id, nextStatus);
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Lojas & Contas de Marketplace</h2>
          <p className="text-sm text-slate-500">
            Associação de contas de vendas a empresas/CNPJs com controle de escopo.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <select
            value={selectedCompanyFilter}
            onChange={(e) => setSelectedCompanyFilter(e.target.value)}
            className="px-3 py-2 border border-slate-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
          >
            <option value="ALL">Todas as Empresas</option>
            {companies.map((c) => (
              <option key={c.id} value={c.id}>
                {c.tradeName} ({c.cnpj})
              </option>
            ))}
          </select>
          <button
            onClick={() => setShowForm(!showForm)}
            disabled={companies.length === 0}
            className={`px-4 py-2 font-medium rounded-lg text-sm transition-colors shadow-sm ${
              companies.length === 0
                ? 'bg-slate-300 text-slate-500 cursor-not-allowed'
                : 'bg-blue-600 hover:bg-blue-700 text-white'
            }`}
          >
            {showForm ? 'Fechar Formulário' : '+ Nova Loja'}
          </button>
        </div>
      </div>

      {companies.length === 0 && (
        <div className="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-lg text-blue-900 text-xs">
          Para cadastrar uma loja, cadastre previamente ao menos uma Empresa/CNPJ na aba &quot;Empresas &amp; CNPJs&quot;.
        </div>
      )}

      {showForm && (
        <form onSubmit={handleCreateStore} className="mb-8 p-5 bg-slate-50 border border-slate-200 rounded-lg">
          <h3 className="text-sm font-semibold text-slate-800 mb-4">Cadastrar Nova Loja</h3>

          {formError && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded">
              {formError}
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Empresa Vinculada (Mesmo Tenant) <span className="text-red-500">*</span>
              </label>
              <select
                value={companyId}
                onChange={(e) => setCompanyId(e.target.value)}
                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none bg-white"
                required
              >
                {companies.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.tradeName} — CNPJ {c.cnpj}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Marketplace <span className="text-red-500">*</span>
              </label>
              <select
                value={marketplace}
                onChange={(e) => setMarketplace(e.target.value as StoreItem['marketplace'])}
                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none bg-white"
                required
              >
                <option value="MERCADO_LIVRE">Mercado Livre</option>
                <option value="SHOPEE">Shopee</option>
                <option value="AMAZON">Amazon</option>
                <option value="OUTRO">Outro Canal</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Nome Interno da Loja <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={internalName}
                onChange={(e) => setInternalName(e.target.value)}
                placeholder="Ex: Mercado Livre - Conta Oficial"
                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Identificador Externo / Conta (Opcional)
              </label>
              <input
                type="text"
                value={externalAccountId}
                onChange={(e) => setExternalAccountId(e.target.value)}
                placeholder="Ex: MLB12345678"
                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>
          </div>

          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setShowForm(false)}
              className="px-4 py-2 border border-slate-300 text-slate-700 text-sm font-medium rounded-md hover:bg-slate-100 transition-colors"
            >
              Cancelar
            </button>
            <button
              type="submit"
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white text-sm font-medium rounded-md transition-colors"
            >
              Salvar Loja
            </button>
          </div>
        </form>
      )}

      {/* Lista de Lojas */}
      {filteredStores.length === 0 ? (
        <div className="text-center py-10 text-slate-400 text-sm">
          Nenhuma loja encontrada para o filtro selecionado.
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 uppercase text-[11px] font-semibold">
              <tr>
                <th className="py-3 px-4">Canal / Marketplace</th>
                <th className="py-3 px-4">Nome da Loja</th>
                <th className="py-3 px-4">Empresa Vinculada</th>
                <th className="py-3 px-4">Conexão</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4 text-right">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredStores.map((s) => (
                <tr key={s.id} className="hover:bg-slate-50/50">
                  <td className="py-3 px-4">
                    <span className="px-2 py-0.5 rounded text-[11px] font-semibold bg-slate-100 text-slate-800">
                      {s.marketplace.replace('_', ' ')}
                    </span>
                  </td>
                  <td className="py-3 px-4">
                    <div className="font-semibold text-slate-900">{s.internalName}</div>
                    {s.externalAccountId && (
                      <div className="text-xs text-slate-400 font-mono">ID: {s.externalAccountId}</div>
                    )}
                  </td>
                  <td className="py-3 px-4 text-xs text-slate-700">{s.companyTradeName}</td>
                  <td className="py-3 px-4">
                    <span className="text-xs text-slate-500 font-medium">
                      {s.connectionStatus === 'CONNECTED' ? '● Conectado' : '○ Desconectado'}
                    </span>
                  </td>
                  <td className="py-3 px-4">
                    <span
                      className={`inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold ${
                        s.status === 'ACTIVE'
                          ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                          : 'bg-slate-100 text-slate-600 border border-slate-300'
                      }`}
                    >
                      {s.status === 'ACTIVE' ? 'Ativa' : 'Inativa'}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-right">
                    <button
                      onClick={() => handleToggle(s.id, s.status)}
                      className={`text-xs font-medium px-3 py-1 rounded transition-colors ${
                        s.status === 'ACTIVE'
                          ? 'text-red-600 hover:bg-red-50'
                          : 'text-emerald-600 hover:bg-emerald-50'
                      }`}
                    >
                      {s.status === 'ACTIVE' ? 'Desativar' : 'Ativar'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};
