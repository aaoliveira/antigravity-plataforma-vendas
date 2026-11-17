'use client';

import React from 'react';

export interface CapabilityItem {
  type: string;
  label: string;
  status: 'SUPPORTED' | 'UNSUPPORTED' | 'PENDING_VALIDATION';
  description: string;
  notes?: string;
}

export interface MarketplaceCapabilityMatrix {
  marketplace: 'MERCADO_LIVRE' | 'SHOPEE';
  name: string;
  authMethod: string;
  capabilities: CapabilityItem[];
}

export const OFFICIAL_MARKETPLACE_CAPABILITIES: MarketplaceCapabilityMatrix[] = [
  {
    marketplace: 'MERCADO_LIVRE',
    name: 'Mercado Livre (Brasil - MLB)',
    authMethod: 'OAuth 2.0 Authorization Code (Token 6h / Refresh 6m)',
    capabilities: [
      {
        type: 'CATALOG',
        label: 'Catálogo & Itens Próprios',
        status: 'SUPPORTED',
        description: 'Leitura de anúncios do seller via /users/{id}/items/search e /items/{id}',
      },
      {
        type: 'LISTINGS',
        label: 'Publicação de Anúncios',
        status: 'SUPPORTED',
        description: 'Criação e edição de anúncios próprios via POST /items',
      },
      {
        type: 'ORDERS',
        label: 'Pedidos & Custos de Envio',
        status: 'SUPPORTED',
        description: 'Leitura de pedidos próprios via /orders/search e custos via /shipments/{id}/costs',
      },
      {
        type: 'ADS',
        label: 'Mercado Ads (Product Ads)',
        status: 'PENDING_VALIDATION',
        description: 'Campanhas e métricas (CPC, ACOS) exigem aprovação especial de escopo advertising/product_ads',
        notes: 'Fase 2 (Requer Aprovação)',
      },
      {
        type: 'WEBHOOKS',
        label: 'Webhooks em Tempo Real',
        status: 'SUPPORTED',
        description: 'Notificações de pedidos (orders_v2) e itens em tempo real',
      },
      {
        type: 'PUBLIC_SEARCH',
        label: 'Pesquisa Pública de Mercado',
        status: 'SUPPORTED',
        description: 'Consulta pública por palavras-chave via /sites/MLB/search sem token privado',
      },
      {
        type: 'PUBLIC_TRENDS',
        label: 'Tendências de Busca',
        status: 'SUPPORTED',
        description: 'Termos mais buscados por categoria via /trends/MLB/{category_id}',
      },
    ],
  },
  {
    marketplace: 'SHOPEE',
    name: 'Shopee Open Platform (Brasil - BR)',
    authMethod: 'OAuth 2.0 com Assinatura HMAC-SHA256 (Token 4h / Refresh 30d)',
    capabilities: [
      {
        type: 'CATALOG',
        label: 'Catálogo & Itens Próprios',
        status: 'SUPPORTED',
        description: 'Leitura de itens próprios via /api/v2/product/get_item_base_info',
      },
      {
        type: 'LISTINGS',
        label: 'Publicação de Anúncios',
        status: 'SUPPORTED',
        description: 'Sincronização de catálogo e anúncios próprios',
      },
      {
        type: 'ORDERS',
        label: 'Pedidos & Escrow Financeiro',
        status: 'SUPPORTED',
        description: 'Consulta de pedidos e detalhamento financeiro via /api/v2/payment/get_escrow_detail',
      },
      {
        type: 'ADS',
        label: 'Shopee Ads',
        status: 'UNSUPPORTED',
        description: 'Não suportado no MVP (sem endpoint de criação/gestão de anúncios patrocinados no escopo)',
      },
      {
        type: 'WEBHOOKS',
        label: 'Push Notifications',
        status: 'SUPPORTED',
        description: 'Notificações de alteração de pedidos e pagamentos',
      },
      {
        type: 'PUBLIC_SEARCH',
        label: 'Pesquisa Pública de Mercado',
        status: 'UNSUPPORTED',
        description: 'A Shopee Open Platform não disponibiliza busca aberta de concorrentes via API oficial',
        notes: 'Sem endpoint público',
      },
      {
        type: 'PUBLIC_TRENDS',
        label: 'Tendências de Busca',
        status: 'UNSUPPORTED',
        description: 'Sem endpoint oficial aberto para tendências',
      },
    ],
  },
];

export const MarketplaceCapabilities: React.FC = () => {
  return (
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 p-6 space-y-6">
      <div>
        <h2 className="text-xl font-bold text-slate-900">Matriz Oficial de Capacidades dos Conectores</h2>
        <p className="text-sm text-slate-500">
          Mapeamento determinístico das capacidades e permissões oficiais auditadas na Descoberta Técnica.
        </p>
      </div>

      <div className="p-4 bg-slate-50 border border-slate-200 rounded-lg text-xs text-slate-700 leading-relaxed">
        <strong>Invariante Arquitetural:</strong> Conectores retornam explicitamente <code className="bg-slate-200 px-1 py-0.5 rounded">UNSUPPORTED</code> para dados não disponibilizados pelas APIs oficiais. Nenhum dado sintético, mock ou scraping não autorizado é apresentado como dado real.
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {OFFICIAL_MARKETPLACE_CAPABILITIES.map((mkt) => (
          <div key={mkt.marketplace} className="border border-slate-200 rounded-xl p-5 bg-white shadow-xs">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div>
                <h3 className="font-bold text-slate-900 text-base">{mkt.name}</h3>
                <span className="text-[11px] text-slate-500">{mkt.authMethod}</span>
              </div>
              <span className="px-2.5 py-1 bg-blue-50 text-blue-700 text-xs font-semibold rounded-full border border-blue-200">
                {mkt.marketplace.replace('_', ' ')}
              </span>
            </div>

            <div className="mt-4 space-y-3">
              {mkt.capabilities.map((cap) => (
                <div key={cap.type} className="flex items-start justify-between text-xs py-1.5 border-b border-slate-50">
                  <div className="pr-4">
                    <span className="font-semibold text-slate-800 block">{cap.label}</span>
                    <span className="text-slate-500 text-[11px]">{cap.description}</span>
                    {cap.notes && <span className="block text-amber-600 font-medium text-[10px] mt-0.5">{cap.notes}</span>}
                  </div>
                  <span
                    className={`shrink-0 px-2 py-0.5 rounded-full text-[10px] font-bold ${
                      cap.status === 'SUPPORTED'
                        ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                        : cap.status === 'PENDING_VALIDATION'
                        ? 'bg-amber-50 text-amber-700 border border-amber-200'
                        : 'bg-slate-100 text-slate-500 border border-slate-200'
                    }`}
                  >
                    {cap.status === 'SUPPORTED' ? 'Suportado' : cap.status === 'PENDING_VALIDATION' ? 'Pendente' : 'Não Suportado'}
                  </span>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
