'use client';

import React, { useState, useEffect, useRef } from 'react';
import {
  ChatMessage,
  PromptTemplateDto,
  StructuredAiResponseDto,
  AiUsageAuditRecord,
} from '@/types/aiassistant';
import { AiChatMessage } from './AiChatMessage';
import { AiSourcesDrawer } from './AiSourcesDrawer';

type ViewTab = 'CHAT' | 'AUDIT';

const DEFAULT_TEMPLATES: PromptTemplateDto[] = [
  {
    key: 'LISTING_QUALITY_AUDIT',
    version: 'v1.0.0',
    description: 'Auditoria técnica de qualidade, score de catálogo e oportunidades de melhoria de anúncios.',
    allowedTools: ['search_listings', 'calculate_margin', 'summarize_metrics'],
    systemInstructions: '',
    registeredAt: '2026-11-19T10:00:00Z',
  },
  {
    key: 'ANALYTICS_METRICS_SUMMARY',
    version: 'v1.0.0',
    description: 'Resumo e explicação analítica de métricas oficiais de tráfego, conversão e receita.',
    allowedTools: ['summarize_metrics', 'search_listings'],
    systemInstructions: '',
    registeredAt: '2026-11-19T10:00:00Z',
  },
  {
    key: 'FINANCIAL_PRICE_EXPLAINER',
    version: 'v1.0.0',
    description: 'Explicação da formação de preço, ponto de equilíbrio e margem de contribuição.',
    allowedTools: ['calculate_margin', 'search_listings'],
    systemInstructions: '',
    registeredAt: '2026-11-19T10:00:00Z',
  },
];

export const AiAssistantView: React.FC = () => {
  const [activeTab, setActiveTab] = useState<ViewTab>('CHAT');
  const [templates, setTemplates] = useState<PromptTemplateDto[]>(DEFAULT_TEMPLATES);
  const [selectedTemplateKey, setSelectedTemplateKey] = useState<string>('LISTING_QUALITY_AUDIT');
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputText, setInputText] = useState<string>('');
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [securityError, setSecurityError] = useState<{ threatType: string; snippet?: string; message: string } | null>(null);

  // Auditoria
  const [audits, setAudits] = useState<AiUsageAuditRecord[]>([]);
  const [isLoadingAudits, setIsLoadingAudits] = useState<boolean>(false);

  // Gaveta de fontes
  const [selectedResponseForDrawer, setSelectedResponseForDrawer] = useState<StructuredAiResponseDto | null>(null);
  const [isDrawerOpen, setIsDrawerOpen] = useState<boolean>(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Carrega templates de prompt
  useEffect(() => {
    async function loadPrompts() {
      try {
        const res = await fetch('/api/ai/prompts');
        if (res.ok) {
          const data = await res.json();
          if (Array.isArray(data) && data.length > 0) {
            setTemplates(data);
          }
        }
      } catch {
        // Fallback silencioso para DEFAULT_TEMPLATES
      }
    }
    loadPrompts();
  }, []);

  // Carrega auditorias ao alternar para a aba de auditoria
  useEffect(() => {
    if (activeTab === 'AUDIT') {
      loadAudits();
    }
  }, [activeTab]);

  const loadAudits = async () => {
    setIsLoadingAudits(true);
    try {
      const res = await fetch('/api/ai/audits?size=50');
      if (res.ok) {
        const data = await res.json();
        setAudits(data.content || []);
      }
    } catch {
      // Falha de rede tratada
    } finally {
      setIsLoadingAudits(false);
    }
  };

  // Scroll automático para a mensagem mais recente
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView?.({ behavior: 'smooth' });
  }, [messages, isLoading]);

  const handleSendMessage = async (customPrompt?: string) => {
    const textToSend = customPrompt || inputText;
    if (!textToSend.trim() || isLoading) return;

    setSecurityError(null);
    const userMsg: ChatMessage = {
      id: `usr-${Date.now()}`,
      sender: 'user',
      content: textToSend.trim(),
      timestamp: new Date().toISOString(),
    };

    setMessages((prev) => [...prev, userMsg]);
    if (!customPrompt) {
      setInputText('');
    }
    setIsLoading(true);

    try {
      const selectedTpl = templates.find((t) => t.key === selectedTemplateKey) || templates[0];

      const res = await fetch('/api/ai/execute', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          promptKey: selectedTpl.key,
          promptVersion: selectedTpl.version,
          userInput: textToSend.trim(),
        }),
      });

      const data = await res.json();

      if (!res.ok) {
        // Detecção de ameaça de Prompt Injection bloqueada (RFC 7807)
        if (data.threat_type || data.title?.includes('Prompt Injection')) {
          const threat = data.threat_type || 'INSTRUCTION_OVERRIDE';
          setSecurityError({
            threatType: threat,
            snippet: data.offending_snippet,
            message: data.detail || 'Entrada bloqueada por violação das políticas de segurança.',
          });

          const errorMsg: ChatMessage = {
            id: `err-${Date.now()}`,
            sender: 'assistant',
            content: `A solicitação foi bloqueada pelo filtro de Prompt Injection (${threat}). Por diretrizes de integridade, comandos que tentam sobrescrever instruções ou regras de sistema não são processados.`,
            timestamp: new Date().toISOString(),
            isError: true,
            threatType: threat,
          };
          setMessages((prev) => [...prev, errorMsg]);
        } else {
          const errDetail = data.detail || data.message || 'Falha ao processar a consulta no AI Gateway.';
          const errorMsg: ChatMessage = {
            id: `err-${Date.now()}`,
            sender: 'assistant',
            content: `Erro operacional: ${errDetail}`,
            timestamp: new Date().toISOString(),
            isError: true,
          };
          setMessages((prev) => [...prev, errorMsg]);
        }
        return;
      }

      // Resposta estruturada com fontes e guardrails
      const structuredRes = data as StructuredAiResponseDto;
      const assistantMsg: ChatMessage = {
        id: structuredRes.executionId || `ast-${Date.now()}`,
        sender: 'assistant',
        content: structuredRes.analysisText,
        timestamp: new Date().toISOString(),
        responseMeta: structuredRes,
      };

      setMessages((prev) => [...prev, assistantMsg]);
    } catch {
      const netErrorMsg: ChatMessage = {
        id: `err-${Date.now()}`,
        sender: 'assistant',
        content: 'Não foi possível conectar ao AI Gateway. Verifique a conectividade de rede ou credenciais.',
        timestamp: new Date().toISOString(),
        isError: true,
      };
      setMessages((prev) => [...prev, netErrorMsg]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  const handleOpenSources = (meta: StructuredAiResponseDto) => {
    setSelectedResponseForDrawer(meta);
    setIsDrawerOpen(true);
  };

  const handleFeedback = (messageId: string, type: 'helpful' | 'not_helpful', comment?: string) => {
    setMessages((prev) =>
      prev.map((msg) =>
        msg.id === messageId
          ? { ...msg, feedback: { type, comment } }
          : msg
      )
    );
  };

  const currentTemplate = templates.find((t) => t.key === selectedTemplateKey) || templates[0];

  return (
    <div className="space-y-6" data-testid="ai-assistant-view">
      {/* Cabeçalho do Módulo */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-200 dark:border-slate-800">
        <div>
          <div className="flex items-center gap-2">
            <span className="p-2 rounded-xl bg-indigo-500/10 text-indigo-600 dark:text-indigo-400">
              <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </span>
            <h1 className="text-xl font-bold text-slate-900 dark:text-white">
              Assistente de IA & Governança Analítica
            </h1>
          </div>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Consultoria analítica determinística com invocação de ferramentas oficiais, citação de fontes e isolamento por organização.
          </p>
        </div>

        {/* Badges de Governança */}
        <div className="flex flex-wrap items-center gap-2">
          <span className="inline-flex items-center gap-1.5 text-xs font-semibold px-2.5 py-1 rounded-full bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-300 border border-emerald-200 dark:border-emerald-800/60">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
            Isolamento Tenant Ativo
          </span>
          <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-amber-50 dark:bg-amber-950/40 text-amber-800 dark:text-amber-300 border border-amber-200 dark:border-amber-800/60">
            Modo Somente Leitura (MVP)
          </span>
        </div>
      </div>

      {/* Navegação por Abas (Tablist) */}
      <div
        role="tablist"
        aria-label="Abas do Assistente de IA"
        className="flex items-center gap-2 border-b border-slate-200 dark:border-slate-800 overflow-x-auto pb-1"
      >
        <button
          role="tab"
          id="tab-chat"
          aria-selected={activeTab === 'CHAT'}
          aria-controls="panel-chat"
          onClick={() => setActiveTab('CHAT')}
          className={`flex items-center gap-2 px-4 py-2 text-xs font-semibold rounded-lg transition-colors min-h-[44px] ${
            activeTab === 'CHAT'
              ? 'bg-indigo-50 dark:bg-indigo-950/40 text-indigo-600 dark:text-indigo-400 border border-indigo-200 dark:border-indigo-800'
              : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
          }`}
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>
          Chat Assistivo
        </button>

        <button
          role="tab"
          id="tab-audit"
          aria-selected={activeTab === 'AUDIT'}
          aria-controls="panel-audit"
          onClick={() => setActiveTab('AUDIT')}
          className={`flex items-center gap-2 px-4 py-2 text-xs font-semibold rounded-lg transition-colors min-h-[44px] ${
            activeTab === 'AUDIT'
              ? 'bg-indigo-50 dark:bg-indigo-950/40 text-indigo-600 dark:text-indigo-400 border border-indigo-200 dark:border-indigo-800'
              : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
          }`}
        >
          <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
          </svg>
          Auditoria de Consumo (Tenant)
        </button>
      </div>

      {/* ABA 1: CHAT ASSISTIVO */}
      {activeTab === 'CHAT' && (
        <div id="panel-chat" role="tabpanel" aria-labelledby="tab-chat" className="space-y-4">
          {/* Seletor de Prompt Template Versionado & Indicador de Tools */}
          <div className="p-4 rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-800 flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div className="space-y-1">
              <label htmlFor="prompt-template-select" className="text-xs font-bold text-slate-700 dark:text-slate-300">
                Template de Prompt Homologado:
              </label>
              <div className="flex items-center gap-2">
                <select
                  id="prompt-template-select"
                  value={selectedTemplateKey}
                  onChange={(e) => setSelectedTemplateKey(e.target.value)}
                  className="text-xs font-medium px-3 py-2 rounded-lg bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-700 text-slate-900 dark:text-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-500 min-h-[40px]"
                >
                  {templates.map((tpl) => (
                    <option key={tpl.key} value={tpl.key}>
                      {tpl.key} ({tpl.version}) - {tpl.description}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Tools Autorizadas para este Template */}
            <div className="flex flex-wrap items-center gap-1.5 text-xs">
              <span className="text-slate-500 dark:text-slate-400 font-medium">Tools Autorizadas:</span>
              {currentTemplate?.allowedTools.map((t, i) => (
                <span
                  key={i}
                  className="px-2 py-0.5 rounded bg-indigo-50 dark:bg-indigo-950/40 text-indigo-700 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800 text-[11px] font-mono"
                >
                  {t}
                </span>
              ))}
            </div>
          </div>

          {/* Banner de Erro de Segurança / Injeção se houver */}
          {securityError && (
            <div
              className="p-4 rounded-xl bg-rose-50 dark:bg-rose-950/50 border border-rose-200 dark:border-rose-900/60 text-rose-800 dark:text-rose-200"
              role="alert"
              data-testid="security-alert-banner"
            >
              <div className="flex items-start gap-3">
                <svg className="w-5 h-5 text-rose-600 dark:text-rose-400 shrink-0 mt-0.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
                <div className="flex-1 text-xs">
                  <h4 className="font-bold text-sm">Entrada Bloqueada por Defesa de Segurança</h4>
                  <p className="mt-1">{securityError.message}</p>
                  <p className="mt-1 font-mono text-[11px] text-rose-600 dark:text-rose-400">
                    Ameaça: {securityError.threatType} {securityError.snippet ? `(Trecho: "${securityError.snippet}")` : ''}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => setSecurityError(null)}
                  className="text-rose-500 hover:text-rose-700 p-1"
                  aria-label="Fechar alerta de segurança"
                >
                  <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            </div>
          )}

          {/* Área de Mensagens (Thread) */}
          <div
            className="p-4 sm:p-6 rounded-2xl bg-slate-50/50 dark:bg-slate-900/50 border border-slate-200 dark:border-slate-800 min-h-[420px] max-h-[560px] overflow-y-auto"
            role="log"
            aria-live="polite"
            aria-label="Histórico de Mensagens do Chat"
          >
            {messages.length === 0 ? (
              <div className="h-full flex flex-col items-center justify-center text-center py-12 px-4 space-y-4">
                <div className="w-12 h-12 rounded-2xl bg-indigo-100 dark:bg-indigo-900/40 text-indigo-600 dark:text-indigo-400 flex items-center justify-center">
                  <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z" />
                  </svg>
                </div>
                <div>
                  <h3 className="text-base font-semibold text-slate-800 dark:text-slate-200">
                    Como posso auxiliar suas decisões de venda hoje?
                  </h3>
                  <p className="text-xs text-slate-500 dark:text-slate-400 max-w-md mt-1">
                    Suas consultas acionam ferramentas determinísticas internas para busca de catálogo próprio, cálculo de margem e resumo de métricas.
                  </p>
                </div>

                {/* Sugestões Rápidas de Prompt */}
                <div className="flex flex-wrap justify-center gap-2 max-w-lg pt-2">
                  <button
                    type="button"
                    onClick={() => {
                      setSelectedTemplateKey('LISTING_QUALITY_AUDIT');
                      handleSendMessage('Auditar anúncios próprios e avaliar oportunidades de melhoria');
                    }}
                    className="px-3 py-1.5 rounded-lg text-xs font-medium bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700 hover:border-indigo-400 transition-colors shadow-sm"
                  >
                    🔍 Auditar anúncios próprios
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setSelectedTemplateKey('FINANCIAL_PRICE_EXPLAINER');
                      handleSendMessage('Simular precificação e margem de contribuição com preço de R$ 150');
                    }}
                    className="px-3 py-1.5 rounded-lg text-xs font-medium bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700 hover:border-indigo-400 transition-colors shadow-sm"
                  >
                    💵 Simular preço e margem
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setSelectedTemplateKey('ANALYTICS_METRICS_SUMMARY');
                      handleSendMessage('Resumo oficial de tráfego, pedidos e conversão dos últimos 30 dias');
                    }}
                    className="px-3 py-1.5 rounded-lg text-xs font-medium bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-300 border border-slate-200 dark:border-slate-700 hover:border-indigo-400 transition-colors shadow-sm"
                  >
                    📊 Resumo de métricas (30 dias)
                  </button>
                </div>
              </div>
            ) : (
              <div className="space-y-4">
                {messages.map((msg) => (
                  <AiChatMessage
                    key={msg.id}
                    message={msg}
                    onOpenSources={handleOpenSources}
                    onFeedback={handleFeedback}
                  />
                ))}

                {isLoading && (
                  <div className="flex items-center gap-2 text-xs text-slate-500 dark:text-slate-400 p-3" data-testid="ai-loading-indicator">
                    <div className="w-2 h-2 rounded-full bg-indigo-500 animate-ping" />
                    <span>Invocando provedor de IA e executando ferramentas de dados...</span>
                  </div>
                )}
                <div ref={messagesEndRef} />
              </div>
            )}
          </div>

          {/* Barra de Entrada de Mensagem */}
          <div className="p-3 bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 shadow-sm">
            <div className="flex items-end gap-2">
              <textarea
                ref={textareaRef}
                value={inputText}
                onChange={(e) => setInputText(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Faça uma pergunta sobre seus anúncios, margens ou métricas... (Pressione Enter para enviar)"
                rows={2}
                disabled={isLoading}
                aria-label="Mensagem para o assistente de IA"
                className="flex-1 resize-none text-xs sm:text-sm p-2.5 rounded-xl bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 text-slate-900 dark:text-slate-100 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 disabled:opacity-50"
              />

              <div className="flex flex-col gap-1.5 shrink-0">
                <button
                  type="button"
                  onClick={() => handleSendMessage()}
                  disabled={isLoading || !inputText.trim()}
                  className="px-4 py-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-300 dark:disabled:bg-slate-800 text-white font-medium text-xs rounded-xl shadow-sm transition-colors flex items-center justify-center gap-1.5 min-h-[44px] min-w-[44px]"
                  aria-label="Enviar mensagem para o assistente"
                >
                  {isLoading ? (
                    <svg className="animate-spin w-4 h-4" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z" />
                    </svg>
                  ) : (
                    <>
                      <span>Enviar</span>
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 5l7 7m0 0l-7 7m7-7H3" />
                      </svg>
                    </>
                  )}
                </button>

                {messages.length > 0 && (
                  <button
                    type="button"
                    onClick={() => {
                      setMessages([]);
                      setSecurityError(null);
                    }}
                    className="text-[11px] text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 text-center py-1"
                  >
                    Limpar chat
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ABA 2: AUDITORIA DE CONSUMO */}
      {activeTab === 'AUDIT' && (
        <div id="panel-audit" role="tabpanel" aria-labelledby="tab-audit" className="space-y-4">
          <div className="flex justify-between items-center">
            <div>
              <h3 className="text-sm font-bold text-slate-900 dark:text-white">
                Histórico de Auditoria & Governança de Tokens
              </h3>
              <p className="text-xs text-slate-500">
                Registros com isolamento multi-tenant garantido por Row-Level Security no PostgreSQL.
              </p>
            </div>
            <button
              type="button"
              onClick={loadAudits}
              className="px-3 py-1.5 text-xs font-medium rounded-lg bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700 transition-colors min-h-[36px]"
            >
              Atualizar Registros
            </button>
          </div>

          <div className="overflow-x-auto rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 shadow-sm">
            <table className="w-full text-left text-xs text-slate-600 dark:text-slate-300" data-testid="audits-table">
              <thead className="bg-slate-50 dark:bg-slate-800/60 text-slate-500 uppercase text-[10px] tracking-wider border-b border-slate-200 dark:border-slate-800">
                <tr>
                  <th className="px-4 py-3 font-semibold">Data / Hora</th>
                  <th className="px-4 py-3 font-semibold">Prompt Key</th>
                  <th className="px-4 py-3 font-semibold">Provedor / Modelo</th>
                  <th className="px-4 py-3 font-semibold">Tokens (E/S/Total)</th>
                  <th className="px-4 py-3 font-semibold">Latência</th>
                  <th className="px-4 py-3 font-semibold">Custo Est.</th>
                  <th className="px-4 py-3 font-semibold">Tools</th>
                  <th className="px-4 py-3 font-semibold">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-800 font-mono text-xs">
                {isLoadingAudits ? (
                  <tr>
                    <td colSpan={8} className="px-4 py-8 text-center text-slate-400">
                      Carregando auditoria de consumo...
                    </td>
                  </tr>
                ) : audits.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="px-4 py-8 text-center text-slate-400">
                      Nenhum registro de auditoria encontrado para o tenant ativo.
                    </td>
                  </tr>
                ) : (
                  audits.map((a) => (
                    <tr key={a.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40">
                      <td className="px-4 py-3 font-sans">
                        {new Date(a.createdAt).toLocaleString('pt-BR')}
                      </td>
                      <td className="px-4 py-3">
                        <span className="font-semibold text-slate-900 dark:text-slate-100 font-sans">
                          {a.promptKey}
                        </span>
                        <span className="text-[10px] text-slate-400 block">{a.promptVersion}</span>
                      </td>
                      <td className="px-4 py-3">
                        {a.provider} <span className="text-[10px] text-slate-400 block">{a.modelName}</span>
                      </td>
                      <td className="px-4 py-3">
                        {a.promptTokens} / {a.completionTokens} <span className="font-bold text-indigo-600 dark:text-indigo-400">({a.totalTokens})</span>
                      </td>
                      <td className="px-4 py-3 font-sans">{a.latencyMs}ms</td>
                      <td className="px-4 py-3 font-semibold text-emerald-600 dark:text-emerald-400">
                        R$ {Number(a.estimatedCostBrl).toFixed(4)}
                      </td>
                      <td className="px-4 py-3 text-[11px] font-sans">
                        {a.toolsInvoked ? (
                          <span className="truncate max-w-[120px] block" title={a.toolsInvoked}>
                            {a.toolsInvoked}
                          </span>
                        ) : (
                          <span className="text-slate-400">-</span>
                        )}
                      </td>
                      <td className="px-4 py-3 font-sans">
                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                            a.status === 'SUCCESS'
                              ? 'bg-emerald-100 text-emerald-800 dark:bg-emerald-950/60 dark:text-emerald-300'
                              : a.status === 'BLOCKED_SECURITY'
                              ? 'bg-rose-100 text-rose-800 dark:bg-rose-950/60 dark:text-rose-300'
                              : 'bg-amber-100 text-amber-800 dark:bg-amber-950/60 dark:text-amber-300'
                          }`}
                        >
                          {a.status}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Gaveta de Inspeção de Fontes */}
      <AiSourcesDrawer
        isOpen={isDrawerOpen}
        onClose={() => setIsDrawerOpen(false)}
        response={selectedResponseForDrawer}
      />
    </div>
  );
};
