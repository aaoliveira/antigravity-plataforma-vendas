'use client';

import React, { useState } from 'react';

export type MarketplaceCode = 'MERCADO_LIVRE' | 'SHOPEE' | 'AMAZON' | 'TIKTOK_SHOP' | 'MAGALU' | 'SHEIN';
export type ComplianceRisk = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFORMATIONAL';
export type PolicyStatusType = 'ACTIVE' | 'UNDER_REVIEW' | 'RESTRICTED' | 'SUSPENDED' | 'BLOCKED' | 'PENDING_VALIDATION' | 'SOURCE_UNAVAILABLE';
export type IncidentStatusType = 'OPEN' | 'INVESTIGATING' | 'MITIGATING' | 'RESOLVED' | 'CLOSED';

export interface PolicyItem {
  id: string;
  marketplace: MarketplaceCode;
  policyType: string;
  policyName: string;
  officialUrl: string;
  policyVersion?: string;
  effectiveDate?: string;
  lastCheckedAt: string;
  contentHash: string;
  status: PolicyStatusType;
  riskLevel: ComplianceRisk;
  affectedFeature?: string;
  evidence?: string;
}

export interface IncidentItem {
  id: string;
  incidentCode: string;
  marketplace: MarketplaceCode;
  severity: ComplianceRisk;
  affectedFeatures: string;
  status: IncidentStatusType;
  openedAt: string;
  responsible: string;
  description: string;
  remediationPlan?: string;
}

export interface ApiLifecycleItem {
  id: string;
  marketplace: MarketplaceCode;
  apiName: string;
  version: string;
  status: 'ACTIVE' | 'UNDER_REVIEW' | 'DEPRECATED' | 'SUNSET_SCHEDULED' | 'RETIRED';
  sunsetDate?: string;
  migrationGuideUrl?: string;
}

export interface ReactivationHistoryItem {
  id: string;
  marketplace: MarketplaceCode;
  featureName: string;
  approvedBy: string;
  approvedAt: string;
  evidence: string;
  policyVersion?: string;
  testsExecuted: string;
}

const defaultDemoPolicies: PolicyItem[] = [
  {
    id: 'pol-1',
    marketplace: 'MERCADO_LIVRE',
    policyType: 'DEVELOPER_POLICY',
    policyName: 'Mercado Livre Developer Terms & OAuth Guidelines',
    officialUrl: 'https://developers.mercadolivre.com.br/pt_br/autenticacao-e-autorizacao',
    policyVersion: 'v2026.1',
    lastCheckedAt: new Date().toISOString(),
    contentHash: '41b94c77319e7aa754de17540143ef51e1e0d856f0aa1e5bcd44f4475083dbaf',
    status: 'ACTIVE',
    riskLevel: 'HIGH',
    affectedFeature: 'OAUTH_CONNECTION',
    evidence: 'Docs verification on 2026-11-17',
  },
  {
    id: 'pol-2',
    marketplace: 'SHOPEE',
    policyType: 'DEVELOPER_POLICY',
    policyName: 'Shopee Open Platform Developer Service Agreement',
    officialUrl: 'https://open.shopee.com/documents/v2/v2.auth.auth_partner',
    policyVersion: 'v2.0',
    lastCheckedAt: new Date().toISOString(),
    contentHash: '2b97fc5def51da62cdb4d330fa110de09a6ba80032b47a1f3b9e26f032b42be7',
    status: 'ACTIVE',
    riskLevel: 'CRITICAL',
    affectedFeature: 'SYNC_CATALOG',
    evidence: 'Docs verification on 2026-11-17',
  },
  {
    id: 'pol-3',
    marketplace: 'AMAZON',
    policyType: 'API_POLICY',
    policyName: 'Amazon SP-API Acceptable Use Policy & DPP',
    officialUrl: 'https://developer-docs.amazon.com/sp-api/docs/authorization-process',
    policyVersion: 'v2026',
    lastCheckedAt: new Date().toISOString(),
    contentHash: '26ef2b86c1a1ec53c112ba594342ce359de4a5f5a139763391704902fb2f6275',
    status: 'ACTIVE',
    riskLevel: 'CRITICAL',
    affectedFeature: 'SYNC_ORDERS',
    evidence: 'Docs verification on 2026-11-17',
  },
  {
    id: 'pol-4',
    marketplace: 'TIKTOK_SHOP',
    policyType: 'DEVELOPER_POLICY',
    policyName: 'TikTok Shop Partner Agreement Brazil',
    officialUrl: 'https://partner.tiktokshop.com/doc/page/63fe7fb0317e13028c29ffc1',
    policyVersion: 'v1.0',
    lastCheckedAt: new Date().toISOString(),
    contentHash: 'ef38ed3f39837232f99497bf390fb60415deb9e6d9a2da940ade4c8e8a956fe9',
    status: 'BLOCKED',
    riskLevel: 'CRITICAL',
    affectedFeature: 'ALL_OPERATIONS',
    evidence: 'Status UNVERIFIED_COMPLIANCE (Fail Closed)',
  },
];

const defaultDemoIncidents: IncidentItem[] = [
  {
    id: 'inc-1',
    incidentCode: 'INC-2026-101',
    marketplace: 'TIKTOK_SHOP',
    severity: 'CRITICAL',
    affectedFeatures: 'ALL_OPERATIONS',
    status: 'OPEN',
    openedAt: new Date().toISOString(),
    responsible: 'SECURITY_RESPONSE_TEAM',
    description: 'Channel marked as UNVERIFIED_COMPLIANCE. Operations strictly blocked in production under FAIL CLOSED.',
    remediationPlan: 'Formal regional partner accreditation required before unlocking.',
  },
];

const defaultDemoApis: ApiLifecycleItem[] = [
  {
    id: 'api-1',
    marketplace: 'MERCADO_LIVRE',
    apiName: 'Orders API',
    version: 'v2',
    status: 'ACTIVE',
  },
  {
    id: 'api-2',
    marketplace: 'SHOPEE',
    apiName: 'Open Platform API',
    version: 'v2.0',
    status: 'ACTIVE',
  },
  {
    id: 'api-3',
    marketplace: 'AMAZON',
    apiName: 'SP-API Orders',
    version: 'v0',
    status: 'ACTIVE',
  },
  {
    id: 'api-4',
    marketplace: 'MERCADO_LIVRE',
    apiName: 'Legacy Metrics API',
    version: 'v1',
    status: 'DEPRECATED',
    sunsetDate: '2027-01-31',
    migrationGuideUrl: 'https://developers.mercadolivre.com.br/migration',
  },
];

export const ComplianceDashboardView: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'OVERVIEW' | 'POLICIES' | 'INCIDENTS' | 'REACTIVATION' | 'APIS'>('OVERVIEW');
  const [policies, setPolicies] = useState<PolicyItem[]>(defaultDemoPolicies);
  const [incidents, setIncidents] = useState<IncidentItem[]>(defaultDemoIncidents);
  const [apis] = useState<ApiLifecycleItem[]>(defaultDemoApis);
  const [reactivations, setReactivations] = useState<ReactivationHistoryItem[]>([]);

  // Reactivation form state
  const [reactivateMarketplace, setReactivateMarketplace] = useState<MarketplaceCode>('MERCADO_LIVRE');
  const [reactivateFeature, setReactivateFeature] = useState('');
  const [approvedBy, setApprovedBy] = useState('');
  const [evidence, setEvidence] = useState('');
  const [policyVersion, setPolicyVersion] = useState('');
  const [testsExecuted, setTestsExecuted] = useState('');
  const [reactivateError, setReactivateError] = useState<string | null>(null);
  const [reactivateSuccess, setReactivateSuccess] = useState<string | null>(null);

  // Filter state
  const [selectedMarketplace, setSelectedMarketplace] = useState<string>('ALL');

  const handleResolveIncident = (incidentId: string) => {
    setIncidents((prev) =>
      prev.map((inc) => (inc.id === incidentId ? { ...inc, status: 'RESOLVED' } : inc))
    );
  };

  const handleSafeReactivateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setReactivateError(null);
    setReactivateSuccess(null);

    if (!approvedBy.trim()) {
      setReactivateError('Identificação do administrador aprovador é obrigatória.');
      return;
    }
    if (!evidence.trim()) {
      setReactivateError('Evidência documental de conformidade é obrigatória.');
      return;
    }
    if (!testsExecuted.trim()) {
      setReactivateError('Comprovação de testes automatizados é obrigatória.');
      return;
    }

    const newLog: ReactivationHistoryItem = {
      id: `react-${Date.now()}`,
      marketplace: reactivateMarketplace,
      featureName: reactivateFeature || 'ALL_OPERATIONS',
      approvedBy,
      approvedAt: new Date().toISOString(),
      evidence,
      policyVersion: policyVersion || 'v2026.1',
      testsExecuted,
    };

    setReactivations((prev) => [newLog, ...prev]);
    setReactivateSuccess(`Funcionalidade ${newLog.featureName} no ${newLog.marketplace} reativada com sucesso após aprovação formal.`);
    setApprovedBy('');
    setEvidence('');
    setTestsExecuted('');
    setReactivateFeature('');
  };

  const filteredPolicies = selectedMarketplace === 'ALL'
    ? policies
    : policies.filter((p) => p.marketplace === selectedMarketplace);

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 text-slate-100 shadow-xl space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between border-b border-slate-800 pb-4 gap-4">
        <div>
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <span className="p-2 bg-emerald-500/10 text-emerald-400 rounded-lg">🛡️</span>
            Continuous Marketplace Compliance & Policy Governance
          </h2>
          <p className="text-xs text-slate-400 mt-1">
            Governança contínua de conformidade, monitoramento de fontes oficiais e proteção determinística FAIL CLOSED.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span className="px-3 py-1 bg-emerald-950/60 border border-emerald-500/40 text-emerald-400 rounded-full text-xs font-semibold">
            ● FAIL CLOSED ACTIVE
          </span>
          <span className="px-3 py-1 bg-slate-800 border border-slate-700 text-slate-300 rounded-full text-xs">
            6 Marketplaces Mapeados
          </span>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex flex-wrap gap-2 border-b border-slate-800 pb-2">
        <button
          onClick={() => setActiveTab('OVERVIEW')}
          className={`px-4 py-2 text-xs font-semibold rounded-lg transition-all ${
            activeTab === 'OVERVIEW'
              ? 'bg-emerald-600 text-white shadow'
              : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
          }`}
        >
          📊 Visão Geral & Canais
        </button>
        <button
          onClick={() => setActiveTab('POLICIES')}
          className={`px-4 py-2 text-xs font-semibold rounded-lg transition-all ${
            activeTab === 'POLICIES'
              ? 'bg-emerald-600 text-white shadow'
              : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
          }`}
        >
          📜 Registro de Políticas ({policies.length})
        </button>
        <button
          onClick={() => setActiveTab('INCIDENTS')}
          className={`px-4 py-2 text-xs font-semibold rounded-lg transition-all ${
            activeTab === 'INCIDENTS'
              ? 'bg-emerald-600 text-white shadow'
              : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
          }`}
        >
          🚨 Incidentes ({incidents.filter((i) => i.status !== 'RESOLVED').length})
        </button>
        <button
          onClick={() => setActiveTab('REACTIVATION')}
          className={`px-4 py-2 text-xs font-semibold rounded-lg transition-all ${
            activeTab === 'REACTIVATION'
              ? 'bg-emerald-600 text-white shadow'
              : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
          }`}
        >
          🔐 Reativação Segura
        </button>
        <button
          onClick={() => setActiveTab('APIS')}
          className={`px-4 py-2 text-xs font-semibold rounded-lg transition-all ${
            activeTab === 'APIS'
              ? 'bg-emerald-600 text-white shadow'
              : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/60'
          }`}
        >
          ⚙️ Ciclo de Vida de APIs ({apis.length})
        </button>
      </div>

      {/* Tab 1: Overview */}
      {activeTab === 'OVERVIEW' && (
        <div className="space-y-6">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="bg-slate-800/60 border border-slate-700/60 rounded-xl p-4">
              <span className="text-xs text-slate-400">Marketplaces Monitorados</span>
              <p className="text-2xl font-bold text-white mt-1">6</p>
              <span className="text-[10px] text-slate-500">MELI, Shopee, Amazon + 3 Bloqueados</span>
            </div>
            <div className="bg-slate-800/60 border border-slate-700/60 rounded-xl p-4">
              <span className="text-xs text-slate-400">Fontes Oficiais Ativas</span>
              <p className="text-2xl font-bold text-emerald-400 mt-1">
                {policies.filter((p) => p.status === 'ACTIVE').length}
              </p>
              <span className="text-[10px] text-emerald-500/80">100% URLs oficiais auditadas</span>
            </div>
            <div className="bg-slate-800/60 border border-slate-700/60 rounded-xl p-4">
              <span className="text-xs text-slate-400">Incidentes Abertos</span>
              <p className="text-2xl font-bold text-amber-400 mt-1">
                {incidents.filter((i) => i.status !== 'RESOLVED').length}
              </p>
              <span className="text-[10px] text-amber-500/80">Governança sob mitigação</span>
            </div>
            <div className="bg-slate-800/60 border border-slate-700/60 rounded-xl p-4">
              <span className="text-xs text-slate-400">APIs Depreciadas</span>
              <p className="text-2xl font-bold text-rose-400 mt-1">
                {apis.filter((a) => a.status === 'DEPRECATED').length}
              </p>
              <span className="text-[10px] text-rose-500/80">Com aviso de descontinuação</span>
            </div>
          </div>

          {/* Marketplaces Health Card List */}
          <div className="bg-slate-800/40 border border-slate-800 rounded-xl p-4">
            <h3 className="text-sm font-semibold text-white mb-4">Status Consolidado de Conformidade por Canal</h3>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {[
                { name: 'Mercado Livre', status: 'COMPLIANT (VERIFIED)', color: 'text-emerald-400 bg-emerald-950/40 border-emerald-500/30', note: 'OAuth 2.0 PKCE, Pedidos v2 e Catálogo ativos' },
                { name: 'Shopee', status: 'COMPLIANT (VERIFIED)', color: 'text-emerald-400 bg-emerald-950/40 border-emerald-500/30', note: 'HMAC-SHA256, Open Platform v2 ativo' },
                { name: 'Amazon', status: 'COMPLIANT (VERIFIED)', color: 'text-emerald-400 bg-emerald-950/40 border-emerald-500/30', note: 'LWA OAuth 2.0, SP-API Orders v0 ativo' },
                { name: 'TikTok Shop', status: 'UNVERIFIED_COMPLIANCE (BLOCKED)', color: 'text-rose-400 bg-rose-950/40 border-rose-500/30', note: 'Bloqueado preventivamente em produção' },
                { name: 'Magalu', status: 'UNVERIFIED_COMPLIANCE (BLOCKED)', color: 'text-rose-400 bg-rose-950/40 border-rose-500/30', note: 'Aguardando credenciamento no LuizaLabs' },
                { name: 'SHEIN', status: 'UNVERIFIED_COMPLIANCE (BLOCKED)', color: 'text-rose-400 bg-rose-950/40 border-rose-500/30', note: 'Aguardando credenciamento oficial' },
              ].map((mkt) => (
                <div key={mkt.name} className="p-4 bg-slate-900 border border-slate-800 rounded-lg space-y-2">
                  <div className="flex items-center justify-between">
                    <span className="font-semibold text-white text-sm">{mkt.name}</span>
                    <span className={`text-[10px] px-2 py-0.5 rounded border font-semibold ${mkt.color}`}>
                      {mkt.status}
                    </span>
                  </div>
                  <p className="text-xs text-slate-400">{mkt.note}</p>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Tab 2: Policies */}
      {activeTab === 'POLICIES' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-semibold text-white">Políticas e Termos Oficiais Registrados</h3>
            <select
              aria-label="Filtrar por canal"
              value={selectedMarketplace}
              onChange={(e) => setSelectedMarketplace(e.target.value)}
              className="px-3 py-1.5 bg-slate-800 border border-slate-700 text-xs rounded-lg text-slate-200"
            >
              <option value="ALL">Todos os Marketplaces</option>
              <option value="MERCADO_LIVRE">Mercado Livre</option>
              <option value="SHOPEE">Shopee</option>
              <option value="AMAZON">Amazon</option>
              <option value="TIKTOK_SHOP">TikTok Shop</option>
              <option value="MAGALU">Magalu</option>
              <option value="SHEIN">SHEIN</option>
            </select>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border border-slate-800 rounded-lg">
              <thead className="bg-slate-800/60 text-slate-300">
                <tr>
                  <th className="p-3">Marketplace</th>
                  <th className="p-3">Política</th>
                  <th className="p-3">Tipo</th>
                  <th className="p-3">Versão</th>
                  <th className="p-3">Status</th>
                  <th className="p-3">Risco</th>
                  <th className="p-3">Hash SHA-256</th>
                  <th className="p-3">Link Oficial</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {filteredPolicies.map((pol) => (
                  <tr key={pol.id} className="hover:bg-slate-800/30">
                    <td className="p-3 font-semibold text-white">{pol.marketplace}</td>
                    <td className="p-3 text-slate-200">{pol.policyName}</td>
                    <td className="p-3 text-slate-400">{pol.policyType}</td>
                    <td className="p-3 text-slate-300">{pol.policyVersion || '—'}</td>
                    <td className="p-3">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-semibold border ${
                        pol.status === 'ACTIVE'
                          ? 'bg-emerald-950/40 text-emerald-400 border-emerald-500/30'
                          : 'bg-rose-950/40 text-rose-400 border-rose-500/30'
                      }`}>
                        {pol.status}
                      </span>
                    </td>
                    <td className="p-3">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-semibold ${
                        pol.riskLevel === 'CRITICAL' ? 'text-rose-400' : 'text-amber-400'
                      }`}>
                        {pol.riskLevel}
                      </span>
                    </td>
                    <td className="p-3 font-mono text-[10px] text-slate-500">
                      {pol.contentHash.substring(0, 12)}...
                    </td>
                    <td className="p-3">
                      <a
                        href={pol.officialUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="text-emerald-400 hover:underline flex items-center gap-1"
                      >
                        Abrir ↗
                      </a>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Tab 3: Incidents */}
      {activeTab === 'INCIDENTS' && (
        <div className="space-y-4">
          <h3 className="text-sm font-semibold text-white">Central de Incidentes de Compliance</h3>
          <div className="space-y-3">
            {incidents.map((inc) => (
              <div
                key={inc.id}
                className="p-4 bg-slate-800/40 border border-slate-800 rounded-xl flex flex-col md:flex-row md:items-center justify-between gap-4"
              >
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-xs font-bold text-white">{inc.incidentCode}</span>
                    <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-rose-950/40 border border-rose-500/30 text-rose-400">
                      {inc.severity}
                    </span>
                    <span className="text-xs text-slate-400">• {inc.marketplace}</span>
                  </div>
                  <p className="text-xs text-slate-300">{inc.description}</p>
                  <p className="text-[11px] text-slate-500">
                    Plano de Correção: {inc.remediationPlan || 'Em análise técnica.'}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  <span className={`px-2 py-1 rounded text-xs font-semibold ${
                    inc.status === 'RESOLVED' ? 'bg-emerald-950/60 text-emerald-400 border border-emerald-500/40' : 'bg-amber-950/60 text-amber-400 border border-amber-500/40'
                  }`}>
                    {inc.status}
                  </span>
                  {inc.status !== 'RESOLVED' && (
                    <button
                      onClick={() => handleResolveIncident(inc.id)}
                      className="px-3 py-1 bg-emerald-600 hover:bg-emerald-500 text-white rounded text-xs font-semibold transition"
                    >
                      Resolver
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Tab 4: Safe Reactivation */}
      {activeTab === 'REACTIVATION' && (
        <div className="space-y-6">
          <div className="bg-amber-950/20 border border-amber-500/30 rounded-xl p-4 text-xs text-amber-300 space-y-1">
            <p className="font-bold">⚠️ Protocolo Obrigatório de Reativação Segura (Safe Reactivation):</p>
            <p>
              Nenhuma funcionalidade bloqueada por compliance pode ser reativada de forma automática.
              A reativação exige aprovação formal de administrador autenticado, evidência documental de adequação e prova de testes automatizados executados com sucesso.
            </p>
          </div>

          <form onSubmit={handleSafeReactivateSubmit} className="bg-slate-800/40 border border-slate-800 rounded-xl p-5 space-y-4">
            <h4 className="text-sm font-semibold text-white">Solicitar Reativação Controlada</h4>

            {reactivateError && (
              <div className="p-3 bg-rose-950/40 border border-rose-500/40 text-rose-300 rounded text-xs">
                {reactivateError}
              </div>
            )}
            {reactivateSuccess && (
              <div className="p-3 bg-emerald-950/40 border border-emerald-500/40 text-emerald-300 rounded text-xs">
                {reactivateSuccess}
              </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs text-slate-400 mb-1">Marketplace</label>
                <select
                  value={reactivateMarketplace}
                  onChange={(e) => setReactivateMarketplace(e.target.value as MarketplaceCode)}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded text-xs text-white"
                >
                  <option value="MERCADO_LIVRE">Mercado Livre</option>
                  <option value="SHOPEE">Shopee</option>
                  <option value="AMAZON">Amazon</option>
                  <option value="TIKTOK_SHOP">TikTok Shop</option>
                  <option value="MAGALU">Magalu</option>
                  <option value="SHEIN">SHEIN</option>
                </select>
              </div>

              <div>
                <label className="block text-xs text-slate-400 mb-1">Funcionalidade / Escopo Afetado</label>
                <input
                  type="text"
                  placeholder="Ex: PRICE_UPDATE, SYNC_ORDERS"
                  value={reactivateFeature}
                  onChange={(e) => setReactivateFeature(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded text-xs text-white"
                />
              </div>

              <div>
                <label className="block text-xs text-slate-400 mb-1">Administrador Aprovador</label>
                <input
                  type="text"
                  placeholder="Nome ou Sub do Administrador"
                  value={approvedBy}
                  onChange={(e) => setApprovedBy(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded text-xs text-white"
                />
              </div>

              <div>
                <label className="block text-xs text-slate-400 mb-1">Versão da Política Analisada</label>
                <input
                  type="text"
                  placeholder="Ex: v2026.1"
                  value={policyVersion}
                  onChange={(e) => setPolicyVersion(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded text-xs text-white"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs text-slate-400 mb-1">Evidência Documental</label>
              <textarea
                rows={2}
                placeholder="Referência da documentação oficial e análise que comprova a conformidade"
                value={evidence}
                onChange={(e) => setEvidence(e.target.value)}
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded text-xs text-white"
              />
            </div>

            <div>
              <label className="block text-xs text-slate-400 mb-1">Comprovação de Testes Automatizados Executados</label>
              <textarea
                rows={2}
                placeholder="Ex: ContinuousComplianceEngineIntegrationTest 100% verde (13/13)"
                value={testsExecuted}
                onChange={(e) => setTestsExecuted(e.target.value)}
                className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded text-xs text-white"
              />
            </div>

            <button
              type="submit"
              className="px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded text-xs font-semibold transition"
            >
              Aprovar Reativação Segura
            </button>
          </form>

          {reactivations.length > 0 && (
            <div className="space-y-2">
              <h4 className="text-xs font-semibold text-slate-300">Histórico Recente de Reativações Aprovadas</h4>
              <div className="space-y-2">
                {reactivations.map((r) => (
                  <div key={r.id} className="p-3 bg-slate-900 border border-slate-800 rounded text-xs space-y-1">
                    <div className="flex items-center justify-between">
                      <span className="font-semibold text-white">{r.featureName} ({r.marketplace})</span>
                      <span className="text-[10px] text-slate-500">{new Date(r.approvedAt).toLocaleString('pt-BR')}</span>
                    </div>
                    <p className="text-slate-400">Aprovado por: <span className="text-slate-200">{r.approvedBy}</span></p>
                    <p className="text-slate-400">Testes: <span className="text-emerald-400">{r.testsExecuted}</span></p>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Tab 5: APIs */}
      {activeTab === 'APIS' && (
        <div className="space-y-4">
          <h3 className="text-sm font-semibold text-white">Monitoramento de Ciclo de Vida e Versões de APIs</h3>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border border-slate-800 rounded-lg">
              <thead className="bg-slate-800/60 text-slate-300">
                <tr>
                  <th className="p-3">Marketplace</th>
                  <th className="p-3">API</th>
                  <th className="p-3">Versão</th>
                  <th className="p-3">Status</th>
                  <th className="p-3">Data de Sunset (Descontinuação)</th>
                  <th className="p-3">Guia de Migração</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {apis.map((api) => (
                  <tr key={api.id} className="hover:bg-slate-800/30">
                    <td className="p-3 font-semibold text-white">{api.marketplace}</td>
                    <td className="p-3 text-slate-200">{api.apiName}</td>
                    <td className="p-3 font-mono text-slate-300">{api.version}</td>
                    <td className="p-3">
                      <span className={`px-2 py-0.5 rounded text-[10px] font-semibold border ${
                        api.status === 'ACTIVE'
                          ? 'bg-emerald-950/40 text-emerald-400 border-emerald-500/30'
                          : 'bg-amber-950/40 text-amber-400 border-amber-500/30'
                      }`}>
                        {api.status}
                      </span>
                    </td>
                    <td className="p-3 text-slate-400">{api.sunsetDate || 'Ativa (Sem data definida)'}</td>
                    <td className="p-3">
                      {api.migrationGuideUrl ? (
                        <a href={api.migrationGuideUrl} target="_blank" rel="noreferrer" className="text-emerald-400 hover:underline">
                          Manual ↗
                        </a>
                      ) : (
                        '—'
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};
