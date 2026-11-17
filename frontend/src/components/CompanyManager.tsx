'use client';

import React, { useState } from 'react';
import { isValidCnpj, formatCnpj, cleanCnpj } from '@/lib/cnpj';

export interface CompanyItem {
  id: string;
  cnpj: string;
  tradeName: string;
  legalName?: string;
  taxRegime: 'SIMPLES_NACIONAL' | 'LUCRO_PRESUMIDO' | 'LUCRO_REAL' | 'MEI' | 'OUTRO';
  status: 'ACTIVE' | 'INACTIVE';
  createdAt: string;
}

interface CompanyManagerProps {
  initialCompanies?: CompanyItem[];
  onAddCompany?: (company: Omit<CompanyItem, 'id' | 'createdAt'>) => void;
  onToggleStatus?: (id: string, newStatus: 'ACTIVE' | 'INACTIVE') => void;
}

export const CompanyManager: React.FC<CompanyManagerProps> = ({
  initialCompanies = [],
  onAddCompany,
  onToggleStatus,
}) => {
  const [companies, setCompanies] = useState<CompanyItem[]>(initialCompanies);
  const [showForm, setShowForm] = useState(false);

  // Form states
  const [cnpj, setCnpj] = useState('');
  const [tradeName, setTradeName] = useState('');
  const [legalName, setLegalName] = useState('');
  const [taxRegime, setTaxRegime] = useState<CompanyItem['taxRegime']>('SIMPLES_NACIONAL');
  const [formError, setFormError] = useState<string | null>(null);

  const isCnpjValid = cnpj.length > 0 ? isValidCnpj(cnpj) : null;

  const handleCnpjChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const raw = e.target.value;
    const cleaned = cleanCnpj(raw);
    if (cleaned.length <= 14) {
      setCnpj(cleaned.length === 14 ? formatCnpj(cleaned) : raw);
    }
  };

  const handleCreateCompany = (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (!isValidCnpj(cnpj)) {
      setFormError('CNPJ inválido: deve possuir 14 dígitos e dígitos verificadores corretos.');
      return;
    }

    if (!tradeName.trim()) {
      setFormError('Nome Fantasia é obrigatório.');
      return;
    }

    const newCompany: CompanyItem = {
      id: crypto.randomUUID ? crypto.randomUUID() : `comp-${Date.now()}`,
      cnpj: formatCnpj(cnpj),
      tradeName: tradeName.trim(),
      legalName: legalName.trim() || undefined,
      taxRegime,
      status: 'ACTIVE',
      createdAt: new Date().toISOString(),
    };

    setCompanies((prev) => [newCompany, ...prev]);
    if (onAddCompany) {
      onAddCompany(newCompany);
    }

    // Reset form
    setCnpj('');
    setTradeName('');
    setLegalName('');
    setTaxRegime('SIMPLES_NACIONAL');
    setShowForm(false);
  };

  const handleToggle = (id: string, currentStatus: 'ACTIVE' | 'INACTIVE') => {
    const nextStatus = currentStatus === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    setCompanies((prev) =>
      prev.map((c) => (c.id === id ? { ...c, status: nextStatus } : c))
    );
    if (onToggleStatus) {
      onToggleStatus(id, nextStatus);
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-xl font-bold text-slate-900">Empresas e CNPJs</h2>
          <p className="text-sm text-slate-500">
            Gerenciamento de matrizes e filiais para isolamento multi-CNPJ e parametrização fiscal.
          </p>
        </div>
        <button
          onClick={() => setShowForm(!showForm)}
          className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white font-medium rounded-lg text-sm transition-colors shadow-sm"
        >
          {showForm ? 'Fechar Formulário' : '+ Nova Empresa'}
        </button>
      </div>

      {/* Alerta de transparência sobre validação CNPJ e regime tributário */}
      <div className="mb-6 p-4 bg-amber-50 border border-amber-200 rounded-lg text-amber-900 text-xs leading-relaxed">
        <strong>Nota Informativa:</strong> A validação de CNPJ é estritamente algorítmica (formato e dígitos verificadores) e não substitui nem declara consulta oficial de situação cadastral perante a Receita Federal. O regime tributário é uma parametrização configurável pelo usuário para fins de projeção operacional.
      </div>

      {showForm && (
        <form onSubmit={handleCreateCompany} className="mb-8 p-5 bg-slate-50 border border-slate-200 rounded-lg">
          <h3 className="text-sm font-semibold text-slate-800 mb-4">Cadastrar Nova Empresa</h3>

          {formError && (
            <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded">
              {formError}
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                CNPJ <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={cnpj}
                onChange={handleCnpjChange}
                placeholder="00.000.000/0000-00"
                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                required
              />
              {isCnpjValid !== null && (
                <span className={`text-[11px] font-medium mt-1 inline-block ${isCnpjValid ? 'text-emerald-600' : 'text-red-600'}`}>
                  {isCnpjValid ? '✓ Formato e dígitos verificadores válidos' : '✗ CNPJ com formato ou dígitos inválidos'}
                </span>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Nome Fantasia <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={tradeName}
                onChange={(e) => setTradeName(e.target.value)}
                placeholder="Ex: Minha Loja Matriz"
                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Razão Social (Opcional)
              </label>
              <input
                type="text"
                value={legalName}
                onChange={(e) => setLegalName(e.target.value)}
                placeholder="Ex: Comercio Digital Ltda"
                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Regime Tributário (Configuração Informativa)
              </label>
              <select
                value={taxRegime}
                onChange={(e) => setTaxRegime(e.target.value as CompanyItem['taxRegime'])}
                className="w-full px-3 py-2 border border-slate-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none bg-white"
              >
                <option value="SIMPLES_NACIONAL">Simples Nacional</option>
                <option value="LUCRO_PRESUMIDO">Lucro Presumido</option>
                <option value="LUCRO_REAL">Lucro Real</option>
                <option value="MEI">Microempreendedor Individual (MEI)</option>
                <option value="OUTRO">Outro / Isento</option>
              </select>
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
              Salvar Empresa
            </button>
          </div>
        </form>
      )}

      {/* Lista de Empresas */}
      {companies.length === 0 ? (
        <div className="text-center py-10 text-slate-400 text-sm">
          Nenhuma empresa cadastrada nesta organização. Clique em &quot;+ Nova Empresa&quot; para começar.
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 border-b border-slate-200 text-slate-600 uppercase text-[11px] font-semibold">
              <tr>
                <th className="py-3 px-4">Nome Fantasia / Razão Social</th>
                <th className="py-3 px-4">CNPJ</th>
                <th className="py-3 px-4">Regime Tributário</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4 text-right">Ações</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {companies.map((c) => (
                <tr key={c.id} className="hover:bg-slate-50/50">
                  <td className="py-3 px-4">
                    <div className="font-semibold text-slate-900">{c.tradeName}</div>
                    {c.legalName && <div className="text-xs text-slate-500">{c.legalName}</div>}
                  </td>
                  <td className="py-3 px-4 font-mono text-slate-700 text-xs">{c.cnpj}</td>
                  <td className="py-3 px-4 text-xs text-slate-600">
                    <span className="px-2 py-0.5 bg-slate-100 text-slate-700 rounded text-[11px] font-medium">
                      {c.taxRegime.replace('_', ' ')}
                    </span>
                  </td>
                  <td className="py-3 px-4">
                    <span
                      className={`inline-flex items-center px-2 py-0.5 rounded-full text-[11px] font-semibold ${
                        c.status === 'ACTIVE'
                          ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                          : 'bg-slate-100 text-slate-600 border border-slate-300'
                      }`}
                    >
                      {c.status === 'ACTIVE' ? 'Ativa' : 'Inativa'}
                    </span>
                  </td>
                  <td className="py-3 px-4 text-right">
                    <button
                      onClick={() => handleToggle(c.id, c.status)}
                      className={`text-xs font-medium px-3 py-1 rounded transition-colors ${
                        c.status === 'ACTIVE'
                          ? 'text-red-600 hover:bg-red-50'
                          : 'text-emerald-600 hover:bg-emerald-50'
                      }`}
                    >
                      {c.status === 'ACTIVE' ? 'Desativar' : 'Ativar'}
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
