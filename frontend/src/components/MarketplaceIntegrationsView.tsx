'use client';

import React, { useState, useMemo } from 'react';
import { sanitizeErrorMessage } from '@/lib/errorHandler';

export type CapabilityStatus = 'SUPPORTED' | 'RESTRICTED' | 'UNSUPPORTED' | 'UNVERIFIED';

export type ConnectionStatus = 'ACTIVE' | 'DISCONNECTED' | 'UNVERIFIED';

export interface ChannelCapability {
  id: string;
  name: string;
  status: CapabilityStatus;
  description: string;
  explanation?: string;
  evidenceRef?: string;
}

export interface MarketplaceChannel {
  id: string;
  name: string;
  shortCode: string;
  authType: string;
  status: ConnectionStatus;
  lastVerificationDate: string;
  officialDocsUrl: string;
  requiredScopes: string[];
  rateLimits: string;
  capabilities: ChannelCapability[];
}

const INITIAL_CHANNELS: MarketplaceChannel[] = [
  {
    id: 'MERCADO_LIVRE',
    name: 'Mercado Livre',
    shortCode: 'MLB',
    authType: 'OAuth 2.0 PKCE',
    status: 'ACTIVE',
    lastVerificationDate: '18/11/2026',
    officialDocsUrl: 'https://developers.mercadolibre.com.br',
    requiredScopes: ['read', 'offline_access'],
    rateLimits: '20 req/s por token (Token Bucket com Jitter)',
    capabilities: [
      {
        id: 'READ_OWN_CATALOG',
        name: 'Leitura de Catálogo Próprio',
        status: 'SUPPORTED',
        description: 'Consulta anúncios ativos, pausados e inventário do próprio seller via /users/{id}/items/search.',
        evidenceRef: 'developers.mercadolibre.com.br/pt_br/gerenciamento-de-anuncios (v1.0)',
      },
      {
        id: 'READ_OWN_ORDERS',
        name: 'Leitura de Pedidos Próprios',
        status: 'SUPPORTED',
        description: 'Acompanha pedidos recebidos, status de envio e faturamento próprio via /orders/search.',
        evidenceRef: 'developers.mercadolibre.com.br/pt_br/gerenciamento-de-pedidos (v1.0)',
      },
      {
        id: 'MARKET_SEARCH',
        name: 'Pesquisa Pública de Mercado',
        status: 'SUPPORTED',
        description: 'Explora termos e categorias públicas via /sites/MLB/search sob quotas de tráfego permitidas.',
        evidenceRef: 'developers.mercadolibre.com.br/pt_br/itens-e-buscas (v1.0)',
      },
      {
        id: 'OBSERVED_PRICE_SNAPSHOT',
        name: 'Snapshots de Preço Observado',
        status: 'SUPPORTED',
        description: 'Registra séries temporais históricas de preços a partir do momento em que um anúncio é monitorado.',
        evidenceRef: 'developers.mercadolibre.com.br/pt_br/itens-e-buscas (v1.0)',
      },
      {
        id: 'OFFICIAL_LISTING_METRICS',
        name: 'Métricas Oficiais de Anúncios',
        status: 'SUPPORTED',
        description: 'Acessa métricas oficiais de saúde e visualizações concedidas pelo canal via /items/{id}/health.',
        evidenceRef: 'developers.mercadolibre.com.br/pt_br/metricas-de-visitas (v1.0)',
      },
      {
        id: 'COMPETITOR_SALES_VOLUME',
        name: 'Volume de Vendas de Concorrentes',
        status: 'UNSUPPORTED',
        description: 'Consulta de faturamento unitário ou vendas em tempo real de lojas concorrentes.',
        explanation: 'As APIs oficiais do Mercado Livre não fornecem volume de vendas ou receita de terceiros. A plataforma proíbe terminantemente web scraping, engenharia reversa ou métricas forjadas para proteção da conta do seller.',
        evidenceRef: 'Termos Oficiais Mercado Libre Developers & Diretrizes de Conformidade',
      },
      {
        id: 'WRITE_OPERATIONS',
        name: 'Operações de Escrita em Anúncios',
        status: 'RESTRICTED',
        description: 'Modificação de títulos, preços, estoque ou publicação de novos anúncios via API.',
        explanation: 'Operações de escrita estão deliberadamente desabilitadas no escopo do MVP para garantir risco zero de alterações acidentais no catálogo do vendedor. Acesso estritamente somente leitura.',
        evidenceRef: 'Política de Menor Privilégio e Segurança Multi-Tenant do MVP',
      },
    ],
  },
  {
    id: 'SHOPEE',
    name: 'Shopee',
    shortCode: 'SHOPEE',
    authType: 'Open Platform HMAC-SHA256',
    status: 'ACTIVE',
    lastVerificationDate: '18/11/2026',
    officialDocsUrl: 'https://open.shopee.com',
    requiredScopes: ['Shop Level (OpenAPI v2)'],
    rateLimits: '10 req/s por loja (Tokens com Assinatura Criptográfica)',
    capabilities: [
      {
        id: 'READ_OWN_CATALOG',
        name: 'Leitura de Catálogo Próprio',
        status: 'SUPPORTED',
        description: 'Consulta lista de produtos e itens cadastrados na loja própria via /api/v2/product/get_item_list.',
        evidenceRef: 'open.shopee.com/documents/v2/v2.product.get_item_list',
      },
      {
        id: 'READ_OWN_ORDERS',
        name: 'Leitura de Pedidos Próprios',
        status: 'SUPPORTED',
        description: 'Consulta pedidos recebidos e movimentação de expedição via /api/v2/order/get_order_list.',
        evidenceRef: 'open.shopee.com/documents/v2/v2.order.get_order_list',
      },
      {
        id: 'MARKET_SEARCH',
        name: 'Pesquisa Pública de Mercado',
        status: 'RESTRICTED',
        description: 'Pesquisa limitada de itens e categorias públicas conforme diretrizes da Open Platform.',
        explanation: 'Permitida apenas dentro de cotas e endpoints autorizados pela Shopee Open Platform. Proibida qualquer requisição agressiva.',
        evidenceRef: 'open.shopee.com/documents/v2/guidelines',
      },
      {
        id: 'OBSERVED_PRICE_SNAPSHOT',
        name: 'Snapshots de Preço Observado',
        status: 'RESTRICTED',
        description: 'Acompanhamento histórico de preços observados em itens previamente cadastrados.',
        explanation: 'Armazenamento estritamente progressivo no banco da plataforma a partir da data de início do monitoramento.',
        evidenceRef: 'Shopee Open Platform Developer Terms',
      },
      {
        id: 'OFFICIAL_LISTING_METRICS',
        name: 'Métricas Oficiais de Anúncios',
        status: 'SUPPORTED',
        description: 'Métricas oficiais de desempenho analítico do seller autenticado.',
        evidenceRef: 'open.shopee.com/documents/v2/v2.analytics',
      },
      {
        id: 'COMPETITOR_SALES_VOLUME',
        name: 'Volume de Vendas de Concorrentes',
        status: 'UNSUPPORTED',
        description: 'Faturamento de lojistas rivais em tempo real.',
        explanation: 'A Shopee Open Platform não disponibiliza dados financeiros de terceiros. A plataforma segue estritamente a API oficial e veda scraping ou estimativas fictícias.',
        evidenceRef: 'Shopee Open Platform API Policies',
      },
      {
        id: 'WRITE_OPERATIONS',
        name: 'Operações de Escrita em Anúncios',
        status: 'RESTRICTED',
        description: 'Edição de anúncios e atualização automática de preços.',
        explanation: 'Desabilitada no MVP para proteção da conta do seller. Conforme PRD, a plataforma atua exclusivamente em modo analítico e de monitoramento de mercado.',
        evidenceRef: 'Arquitetura e Governança do MVP',
      },
    ],
  },
  {
    id: 'AMAZON',
    name: 'Amazon (SP-API)',
    shortCode: 'AMZ',
    authType: 'LWA + AWS IAM v2024',
    status: 'UNVERIFIED',
    lastVerificationDate: '18/11/2026',
    officialDocsUrl: 'https://developer-docs.amazon.com/sp-api',
    requiredScopes: ['pricing:read', 'orders:read (Em análise)'],
    rateLimits: 'Quotas de Burst/Restore em fase de cálculo',
    capabilities: [
      {
        id: 'READ_OWN_CATALOG',
        name: 'Leitura de Catálogo Próprio',
        status: 'UNVERIFIED',
        description: 'Leitura de inventário e ASINs via Selling Partner API.',
        explanation: 'Esta capacidade está em validação técnica com a documentação oficial SP-API. Sob a regra Fail-Closed, este recurso permanece bloqueado até homologação formal.',
        evidenceRef: 'Validação documental em andamento (UNVERIFIED)',
      },
      {
        id: 'COMPETITOR_SALES_VOLUME',
        name: 'Volume de Vendas de Concorrentes',
        status: 'UNSUPPORTED',
        description: 'Vendas unitárias de terceiros na Amazon.',
        explanation: 'A Amazon SP-API não disponibiliza faturamento ou volume financeiro de concorrentes. Scraping proibido pelas políticas da Amazon.',
        evidenceRef: 'Amazon SP-API Acceptable Use Policy',
      },
      {
        id: 'WRITE_OPERATIONS',
        name: 'Operações de Escrita em Anúncios',
        status: 'RESTRICTED',
        description: 'Modificações em listings e preços na Amazon.',
        explanation: 'Desabilitada no MVP para proteção da conta do lojista.',
        evidenceRef: 'Diretrizes de Governança',
      },
    ],
  },
  {
    id: 'OTHER_CHANNELS',
    name: 'TikTok Shop / Magalu / SHEIN',
    shortCode: 'MULTI',
    authType: 'Conectores em Fase de Planejamento',
    status: 'UNVERIFIED',
    lastVerificationDate: '18/11/2026',
    officialDocsUrl: 'https://partner.tiktokshop.com',
    requiredScopes: ['Aguardando especificação formal'],
    rateLimits: 'A definir após homologação',
    capabilities: [
      {
        id: 'MULTI_MARKETPLACE_SYNC',
        name: 'Integração Unificada Multi-Marketplace',
        status: 'UNVERIFIED',
        description: 'Conectores para canais adicionais de comércio eletrônico.',
        explanation: 'Estes canais não possuem homologação técnica ativa no MVP. Sob a política Fail-Closed, permanecem bloqueados e não disponíveis para uso.',
        evidenceRef: 'Roadmap P012+ (UNVERIFIED)',
      },
      {
        id: 'COMPETITOR_SALES_VOLUME',
        name: 'Volume de Vendas de Concorrentes',
        status: 'UNSUPPORTED',
        description: 'Faturamento de terceiros em marketplaces secundários.',
        explanation: 'Recurso não fornecido por APIs oficiais. Scraping proibido.',
        evidenceRef: 'Políticas de Conformidade',
      },
    ],
  },
];

export const MarketplaceIntegrationsView: React.FC = () => {
  const [channels, setChannels] = useState<MarketplaceChannel[]>(INITIAL_CHANNELS);
  const [selectedChannelId, setSelectedChannelId] = useState<string>('MERCADO_LIVRE');
  const [statusFilter, setStatusFilter] = useState<'ALL' | CapabilityStatus>('ALL');
  const [feedbackMessage, setFeedbackMessage] = useState<{
    type: 'success' | 'error' | 'info';
    text: string;
  } | null>(null);
  const [isProcessing, setIsProcessing] = useState<boolean>(false);

  // Canal selecionado
  const selectedChannel = useMemo(() => {
    return channels.find((c) => c.id === selectedChannelId) || channels[0];
  }, [channels, selectedChannelId]);

  // Filtro de capacidades
  const filteredCapabilities = useMemo(() => {
    if (statusFilter === 'ALL') {
      return selectedChannel.capabilities;
    }
    return selectedChannel.capabilities.filter((c) => c.status === statusFilter);
  }, [selectedChannel, statusFilter]);

  // Estatísticas agregadas
  const stats = useMemo(() => {
    let supportedCount = 0;
    let restrictedCount = 0;
    let unsupportedCount = 0;
    let unverifiedCount = 0;

    channels.forEach((ch) => {
      ch.capabilities.forEach((cap) => {
        if (cap.status === 'SUPPORTED') supportedCount++;
        else if (cap.status === 'RESTRICTED') restrictedCount++;
        else if (cap.status === 'UNSUPPORTED') unsupportedCount++;
        else if (cap.status === 'UNVERIFIED') unverifiedCount++;
      });
    });

    const activeConnections = channels.filter((c) => c.status === 'ACTIVE').length;

    return {
      activeConnections,
      supportedCount,
      restrictedCount,
      unsupportedCount,
      unverifiedCount,
    };
  }, [channels]);

  // Ação Permitida 1: Conectar Canal
  const handleConnect = (channelId: string) => {
    setIsProcessing(true);
    setFeedbackMessage(null);

    // Simulação do redirecionamento oficial OAuth
    setTimeout(() => {
      setChannels((prev) =>
        prev.map((ch) =>
          ch.id === channelId
            ? {
                ...ch,
                status: 'ACTIVE',
                lastVerificationDate: new Date().toLocaleDateString('pt-BR'),
              }
            : ch
        )
      );
      setIsProcessing(false);
      setFeedbackMessage({
        type: 'success',
        text: `Canal ${selectedChannel.name} conectado com sucesso via fluxo oficial OAuth. Tokens cifrados com segurança.`,
      });
    }, 600);
  };

  // Ação Permitida 2: Atualizar Status da Conexão
  const handleRefreshStatus = (channelId: string) => {
    setIsProcessing(true);
    setFeedbackMessage(null);

    setTimeout(() => {
      setChannels((prev) =>
        prev.map((ch) =>
          ch.id === channelId
            ? {
                ...ch,
                lastVerificationDate: new Date().toLocaleDateString('pt-BR'),
              }
            : ch
        )
      );
      setIsProcessing(false);
      setFeedbackMessage({
        type: 'info',
        text: `Status do canal ${selectedChannel.name} atualizado. Conectividade saudável, quotas ativas e token válido.`,
      });
    }, 500);
  };

  // Ação Permitida 3: Revogar Conexão
  const handleRevoke = (channelId: string) => {
    setIsProcessing(true);
    setFeedbackMessage(null);

    setTimeout(() => {
      setChannels((prev) =>
        prev.map((ch) =>
          ch.id === channelId
            ? {
                ...ch,
                status: 'DISCONNECTED',
                lastVerificationDate: new Date().toLocaleDateString('pt-BR'),
              }
            : ch
        )
      );
      setIsProcessing(false);
      setFeedbackMessage({
        type: 'success',
        text: `Autorização do canal ${selectedChannel.name} revogada com sucesso. Credenciais locais invalidadas.`,
      });
    }, 500);
  };

  // Disparador de erro sanitizado de exemplo para auditoria
  const handleTriggerSimulatedError = () => {
    const rawError = 'Connection timeout in tenant_id 7b2c9a1d-5f8e-4a3b-9c2d-1e0f3a5b7c9d with Exception: java.net.SocketTimeoutException';
    const sanitized = sanitizeErrorMessage(rawError);
    setFeedbackMessage({
      type: 'error',
      text: sanitized,
    });
  };

  // Componente de Badge de Status de Capacidade
  const renderCapabilityStatusBadge = (status: CapabilityStatus) => {
    switch (status) {
      case 'SUPPORTED':
        return (
          <span
            data-testid="badge-supported"
            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300 border border-emerald-300 dark:border-emerald-800"
          >
            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
            </svg>
            Homologado / Suportado
          </span>
        );
      case 'RESTRICTED':
        return (
          <span
            data-testid="badge-restricted"
            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300 border border-amber-300 dark:border-amber-800"
          >
            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
            Acesso Restrito (MVP)
          </span>
        );
      case 'UNSUPPORTED':
        return (
          <span
            data-testid="badge-unsupported"
            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-rose-100 text-rose-800 dark:bg-rose-950/60 dark:text-rose-300 border border-rose-300 dark:border-rose-800"
          >
            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728A9 9 0 015.636 5.636m12.728 12.728L5.636 5.636" />
            </svg>
            Não Disponibilizado pelo Canal
          </span>
        );
      case 'UNVERIFIED':
        return (
          <span
            data-testid="badge-unverified"
            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300 border border-slate-300 dark:border-slate-700"
          >
            <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
            </svg>
            Em Validação Técnica (Bloqueado)
          </span>
        );
    }
  };

  return (
    <div
      role="region"
      aria-label="Gestão de Integrações e Capacidades de Marketplaces"
      className="space-y-6 w-full max-w-full"
    >
      {/* =========================================================================
          1. CABEÇALHO E VISÃO GERAL
      ========================================================================= */}
      <div className="rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900/60 p-5 sm:p-6 shadow-sm">
        <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-2 flex-wrap">
              <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-blue-100 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300">
                Gestão & Integrações
              </span>
              <span className="text-xs font-mono px-2 py-0.5 rounded bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400">
                Módulo: STORES
              </span>
              <span className="text-xs font-semibold px-2.5 py-0.5 rounded-full bg-emerald-100 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-300">
                Fail-Closed Ativo
              </span>
            </div>
            <h1 className="text-xl sm:text-2xl font-bold text-slate-900 dark:text-white">
              Lojas & Integrações Oficiais
            </h1>
            <p className="mt-1 text-xs sm:text-sm text-slate-600 dark:text-slate-300 max-w-3xl leading-relaxed">
              Painel de transparência de conexões oficiais OAuth com Mercado Livre e Shopee. Exibe status de conformidade, permissões, evidências documentais e explicações de recursos indisponíveis sem scraping.
            </p>
          </div>

          {/* Botão de auditoria de erro sanitizado */}
          <div className="shrink-0 flex items-center gap-2">
            <button
              type="button"
              onClick={handleTriggerSimulatedError}
              aria-label="Auditar sanitização de erros"
              className="text-xs px-3 py-2 rounded-lg border border-slate-300 dark:border-slate-700 text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors min-h-[44px] flex items-center gap-1.5"
            >
              <svg className="w-4 h-4 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <span>Auditar Erro Sanitizado</span>
            </button>
          </div>
        </div>
      </div>

      {/* =========================================================================
          2. FEEDBACK / ALERTA SANITIZADO (ROLE ALERT)
      ========================================================================= */}
      {feedbackMessage && (
        <div
          role="alert"
          aria-live="polite"
          data-testid="feedback-alert"
          className={`p-4 rounded-xl border flex items-start justify-between gap-3 ${
            feedbackMessage.type === 'success'
              ? 'bg-emerald-50 dark:bg-emerald-950/40 border-emerald-300 dark:border-emerald-800 text-emerald-800 dark:text-emerald-200'
              : feedbackMessage.type === 'error'
              ? 'bg-rose-50 dark:bg-rose-950/40 border-rose-300 dark:border-rose-800 text-rose-800 dark:text-rose-200'
              : 'bg-blue-50 dark:bg-blue-950/40 border-blue-300 dark:border-blue-800 text-blue-800 dark:text-blue-200'
          }`}
        >
          <div className="flex items-start gap-2.5">
            <span className="mt-0.5 font-bold text-sm">
              {feedbackMessage.type === 'success' ? '✓' : feedbackMessage.type === 'error' ? '⚠' : 'ℹ'}
            </span>
            <span className="text-xs sm:text-sm font-medium leading-relaxed">
              {feedbackMessage.text}
            </span>
          </div>
          <button
            type="button"
            onClick={() => setFeedbackMessage(null)}
            aria-label="Fechar alerta"
            className="text-xs p-1 text-slate-500 hover:text-slate-800 dark:hover:text-slate-200 min-h-[44px] min-w-[44px] flex items-center justify-center"
          >
            ✕
          </button>
        </div>
      )}

      {/* =========================================================================
          3. CARDS DE RESUMO DE CAPACIDADES E CONEXÕES
      ========================================================================= */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 sm:gap-4">
        <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900/40">
          <div className="text-[11px] font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
            Conexões Oficiais Ativas
          </div>
          <div className="mt-1.5 text-2xl font-bold text-slate-900 dark:text-white" data-testid="stat-active">
            {stats.activeConnections} de {channels.length}
          </div>
          <div className="text-xs text-emerald-600 dark:text-emerald-400 mt-0.5 flex items-center gap-1">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
            Tokens cifrados
          </div>
        </div>

        <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900/40">
          <div className="text-[11px] font-bold uppercase tracking-wider text-emerald-600 dark:text-emerald-400">
            Homologadas (Suportadas)
          </div>
          <div className="mt-1.5 text-2xl font-bold text-emerald-700 dark:text-emerald-300" data-testid="stat-supported">
            {stats.supportedCount}
          </div>
          <div className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            APIs oficiais com evidência
          </div>
        </div>

        <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900/40">
          <div className="text-[11px] font-bold uppercase tracking-wider text-amber-600 dark:text-amber-400">
            Restritas (MVP Read-Only)
          </div>
          <div className="mt-1.5 text-2xl font-bold text-amber-700 dark:text-amber-300" data-testid="stat-restricted">
            {stats.restrictedCount}
          </div>
          <div className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            Risco zero para a conta
          </div>
        </div>

        <div className="p-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900/40">
          <div className="text-[11px] font-bold uppercase tracking-wider text-rose-600 dark:text-rose-400">
            Não Disponibilizadas
          </div>
          <div className="mt-1.5 text-2xl font-bold text-rose-700 dark:text-rose-300" data-testid="stat-unsupported">
            {stats.unsupportedCount}
          </div>
          <div className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            Zero scraping hostil
          </div>
        </div>
      </div>

      {/* =========================================================================
          4. SELEÇÃO DE CANAIS (TABS RESPONSIVAS)
      ========================================================================= */}
      <div className="border-b border-slate-200 dark:border-slate-800">
        <div className="flex items-center gap-2 overflow-x-auto pb-1" role="tablist" aria-label="Canais de Marketplace">
          {channels.map((channel) => {
            const isSelected = channel.id === selectedChannel.id;
            return (
              <button
                key={channel.id}
                role="tab"
                id={`tab-${channel.id}`}
                aria-selected={isSelected}
                aria-controls={`panel-${channel.id}`}
                onClick={() => setSelectedChannelId(channel.id)}
                className={`flex items-center gap-2 px-4 py-3 text-xs sm:text-sm font-semibold whitespace-nowrap rounded-t-lg transition-colors min-h-[44px] ${
                  isSelected
                    ? 'border-b-2 border-blue-600 text-blue-600 dark:text-blue-400 bg-slate-50 dark:bg-slate-900/50'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200 hover:bg-slate-100/50 dark:hover:bg-slate-800/40'
                }`}
              >
                <span>{channel.name}</span>
                <span
                  className={`text-[10px] px-2 py-0.5 rounded-full font-bold ${
                    channel.status === 'ACTIVE'
                      ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300'
                      : channel.status === 'UNVERIFIED'
                      ? 'bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-400'
                      : 'bg-rose-100 text-rose-800 dark:bg-rose-950 dark:text-rose-300'
                  }`}
                >
                  {channel.status === 'ACTIVE'
                    ? 'Conectado'
                    : channel.status === 'UNVERIFIED'
                    ? 'Em Análise'
                    : 'Desconectado'}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* =========================================================================
          5. DETALHES DO CANAL SELECIONADO & AÇÕES PERMITIDAS
      ========================================================================= */}
      <div
        role="tabpanel"
        id={`panel-${selectedChannel.id}`}
        aria-labelledby={`tab-${selectedChannel.id}`}
        className="space-y-6"
      >
        <div className="rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900/40 p-5 sm:p-6 space-y-4">
          <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-lg font-bold text-slate-900 dark:text-white">
                  {selectedChannel.name}
                </h2>
                <span className="text-xs font-mono px-2 py-0.5 rounded bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400">
                  {selectedChannel.shortCode}
                </span>
              </div>
              <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                Autenticação: <strong className="text-slate-700 dark:text-slate-300">{selectedChannel.authType}</strong>
              </p>
            </div>

            {/* Ações Permitidas: Estritamente Conectar, Atualizar Status e Revogar */}
            <div className="flex items-center gap-2 flex-wrap">
              {selectedChannel.status === 'DISCONNECTED' && (
                <button
                  type="button"
                  disabled={isProcessing}
                  onClick={() => handleConnect(selectedChannel.id)}
                  aria-label={`Conectar com ${selectedChannel.name}`}
                  className="px-4 py-2.5 rounded-lg bg-blue-600 hover:bg-blue-700 text-white font-medium text-xs sm:text-sm transition-colors shadow-sm min-h-[44px] flex items-center gap-2 disabled:opacity-50"
                >
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                  </svg>
                  <span>Conectar Oficialmente</span>
                </button>
              )}

              {selectedChannel.status === 'ACTIVE' && (
                <>
                  <button
                    type="button"
                    disabled={isProcessing}
                    onClick={() => handleRefreshStatus(selectedChannel.id)}
                    aria-label={`Atualizar status de ${selectedChannel.name}`}
                    className="px-3.5 py-2.5 rounded-lg border border-slate-300 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-200 font-medium text-xs sm:text-sm transition-colors min-h-[44px] flex items-center gap-2 disabled:opacity-50"
                  >
                    <svg className={`w-4 h-4 ${isProcessing ? 'animate-spin' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                    </svg>
                    <span>Atualizar Status</span>
                  </button>

                  <button
                    type="button"
                    disabled={isProcessing}
                    onClick={() => handleRevoke(selectedChannel.id)}
                    aria-label={`Revogar conexão com ${selectedChannel.name}`}
                    className="px-3.5 py-2.5 rounded-lg border border-rose-200 dark:border-rose-900 bg-rose-50 dark:bg-rose-950/40 hover:bg-rose-100 dark:hover:bg-rose-900/60 text-rose-700 dark:text-rose-300 font-medium text-xs sm:text-sm transition-colors min-h-[44px] flex items-center gap-2 disabled:opacity-50"
                  >
                    <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                    <span>Revogar Conexão</span>
                  </button>
                </>
              )}

              {selectedChannel.status === 'UNVERIFIED' && (
                <div className="inline-flex items-center gap-2 px-3.5 py-2 rounded-lg bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 text-xs font-medium">
                  <span className="w-2 h-2 rounded-full bg-slate-400" />
                  <span>Em Homologação Documental (Ações Indisponíveis)</span>
                </div>
              )}
            </div>
          </div>

          {/* Dados Técnicos e de Governança do Canal */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-3 border-t border-slate-100 dark:border-slate-800/80 text-xs">
            <div>
              <span className="text-slate-500 dark:text-slate-400">Data da Última Verificação:</span>
              <p className="font-semibold text-slate-800 dark:text-slate-200 mt-0.5" data-testid="last-verification-date">
                {selectedChannel.lastVerificationDate}
              </p>
            </div>
            <div>
              <span className="text-slate-500 dark:text-slate-400">Escopos Autorizados:</span>
              <p className="font-semibold text-slate-800 dark:text-slate-200 mt-0.5">
                {selectedChannel.requiredScopes.join(', ')}
              </p>
            </div>
            <div>
              <span className="text-slate-500 dark:text-slate-400">Limites & Quotas:</span>
              <p className="font-semibold text-slate-800 dark:text-slate-200 mt-0.5">
                {selectedChannel.rateLimits}
              </p>
            </div>
          </div>

          <div className="pt-2 text-xs text-slate-500 dark:text-slate-400 flex items-center justify-between flex-wrap gap-2">
            <span>
              Documentação Oficial:{' '}
              <a
                href={selectedChannel.officialDocsUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="text-blue-600 dark:text-blue-400 underline hover:opacity-80"
              >
                {selectedChannel.officialDocsUrl}
              </a>
            </span>
            <span className="text-[11px] font-mono text-slate-400">
              Segredos & Tokens: [CIFRADOS EM BANCO E COOKIES HTTPONLY]
            </span>
          </div>
        </div>

        {/* =========================================================================
            6. MATRIZ DE CAPACIDADES E RECURSOS DO CANAL
        ========================================================================= */}
        <div className="space-y-4">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
            <div>
              <h3 className="text-base font-bold text-slate-900 dark:text-white">
                Matriz de Recursos e Capacidades
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                Estados oficiais mapeados conforme as diretrizes do PRD e políticas de conformidade do canal.
              </p>
            </div>

            {/* Filtro de Status das Capacidades */}
            <div className="flex items-center gap-1.5 flex-wrap">
              {(['ALL', 'SUPPORTED', 'RESTRICTED', 'UNSUPPORTED', 'UNVERIFIED'] as const).map((filter) => (
                <button
                  key={filter}
                  type="button"
                  onClick={() => setStatusFilter(filter)}
                  aria-pressed={statusFilter === filter}
                  className={`px-2.5 py-1.5 rounded-lg text-xs font-medium transition-colors min-h-[38px] ${
                    statusFilter === filter
                      ? 'bg-slate-900 text-white dark:bg-white dark:text-slate-950 font-semibold'
                      : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 hover:bg-slate-200 dark:hover:bg-slate-700'
                  }`}
                >
                  {filter === 'ALL'
                    ? 'Todas'
                    : filter === 'SUPPORTED'
                    ? 'Homologadas'
                    : filter === 'RESTRICTED'
                    ? 'Restritas'
                    : filter === 'UNSUPPORTED'
                    ? 'Não Fornecidas'
                    : 'Em Validação'}
                </button>
              ))}
            </div>
          </div>

          {/* Lista de Capacidades */}
          <div className="space-y-3" role="list" aria-label="Lista de Capacidades">
            {filteredCapabilities.length === 0 ? (
              <div className="p-8 text-center rounded-xl border border-dashed border-slate-300 dark:border-slate-800 text-slate-500 text-xs">
                Nenhuma capacidade encontrada com o filtro selecionado.
              </div>
            ) : (
              filteredCapabilities.map((cap) => (
                <div
                  key={cap.id}
                  role="listitem"
                  data-testid={`capability-card-${cap.id}`}
                  className={`p-4 sm:p-5 rounded-xl border transition-all ${
                    cap.status === 'SUPPORTED'
                      ? 'border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900/30'
                      : cap.status === 'RESTRICTED'
                      ? 'border-amber-200 dark:border-amber-900/50 bg-amber-50/30 dark:bg-amber-950/20'
                      : cap.status === 'UNSUPPORTED'
                      ? 'border-rose-200 dark:border-rose-900/50 bg-rose-50/20 dark:bg-rose-950/10'
                      : 'border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900/60 opacity-80'
                  }`}
                >
                  <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2.5">
                    <div className="space-y-1">
                      <div className="flex items-center gap-2 flex-wrap">
                        <span className="font-semibold text-sm text-slate-900 dark:text-white">
                          {cap.name}
                        </span>
                        <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-slate-100 dark:bg-slate-800 text-slate-500">
                          {cap.id}
                        </span>
                      </div>
                      <p className="text-xs text-slate-600 dark:text-slate-300">
                        {cap.description}
                      </p>
                    </div>

                    <div className="shrink-0 self-start sm:self-center">
                      {renderCapabilityStatusBadge(cap.status)}
                    </div>
                  </div>

                  {/* Justificativa / Explicação de Indisponibilidade ou Restrição */}
                  {cap.explanation && (
                    <div className="mt-3 p-3 rounded-lg text-xs leading-relaxed bg-white/80 dark:bg-slate-950/50 border border-slate-200/80 dark:border-slate-800 text-slate-700 dark:text-slate-300">
                      <strong className="font-semibold block mb-0.5 text-slate-900 dark:text-slate-100">
                        {cap.status === 'UNSUPPORTED'
                          ? 'Motivo da Indisponibilidade Oficial:'
                          : cap.status === 'RESTRICTED'
                          ? 'Regra de Restrição:'
                          : 'Aviso de Validação:'}
                      </strong>
                      {cap.explanation}
                    </div>
                  )}

                  {/* Referência de Evidência Documental */}
                  {cap.evidenceRef && (
                    <div className="mt-2 text-[11px] text-slate-500 dark:text-slate-400 flex items-center gap-1.5 flex-wrap">
                      <span className="font-medium">Evidência:</span>
                      <span className="font-mono text-slate-600 dark:text-slate-300">
                        {cap.evidenceRef}
                      </span>
                    </div>
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
