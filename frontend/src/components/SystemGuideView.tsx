'use client';

import React, { useState } from 'react';

export const SystemGuideView: React.FC = () => {
  const [activeGuideTab, setActiveGuideTab] = useState<'MODULES' | 'SETUP' | 'EXTENSION' | 'TESTING' | 'DEPLOY'>('MODULES');
  const [selectedPillar, setSelectedPillar] = useState<'ALL' | 'BI' | 'MARKET' | 'AI' | 'FINANCE' | 'MANAGEMENT' | 'GOVERNANCE'>('ALL');
  const [copiedSnippet, setCopiedSnippet] = useState<string | null>(null);

  const copyToClipboard = (text: string, id: string) => {
    if (navigator?.clipboard) {
      navigator.clipboard.writeText(text);
      setCopiedSnippet(id);
      setTimeout(() => setCopiedSnippet(null), 2000);
    }
  };

  const modulesCatalog = [
    // 1. Business Intelligence & Ações
    {
      id: 'ACTION_CENTER',
      name: 'Smart Alerts & Action Center',
      pillar: 'BI',
      pillarName: 'Business Intelligence & Ações',
      phase: 'Fase 2',
      badgeColor: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30',
      description: 'Central de inteligência operacional que transforma eventos do negócio em alertas acionáveis e tarefas rastreáveis.',
      howItWorks: 'Monitora margens, quedas de vendas, concorrência e sincronização. Possui mecanismo de deduplicação temporal (cooldown de 5 min) para evitar saturação. Permite transitar alertas entre os estados NOVO, LIDO, RESOLVIDO e IGNORADO, com conversão em tarefas operacionais com responsável, prazo e evidência auditável.',
      howToUse: '1. Acesse o menu "Alertas & Tarefas".\n2. Visualize os alertas organizados por severidade (CRITICAL, HIGH, MEDIUM, LOW).\n3. Clique em "Converter em Tarefa" para delegar uma ação corretiva com prazo e prioridade.\n4. Alterne para a aba "Regras & Simulador" para testar gatilhos com eventos sintéticos.',
    },
    {
      id: 'MONITORING',
      name: 'Monitoramento & Concorrentes',
      pillar: 'BI',
      pillarName: 'Business Intelligence & Ações',
      phase: 'Fase 2',
      badgeColor: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30',
      description: 'Acompanhamento histórico contínuo de anúncios de concorrentes e tendências de preços.',
      howItWorks: 'Gerencia watchlists isoladas por tenant. Coleta snapshots periódicos utilizando unicamente APIs oficiais autorizadas. Calcula variações reais de preços e estoque sem interpolar dados ausentes silenciosamente.',
      howToUse: '1. Acesse "Monitoramento & Concorrentes".\n2. Adicione anúncios à sua Watchlist informando a URL ou ID canônico (ex.: MLB...).\n3. Acompanhe a linha do tempo de preços e status de estoque com histórico visual auditável.',
    },

    // 2. Market Intelligence
    {
      id: 'MARKET',
      name: 'Pesquisa de Mercado',
      pillar: 'MARKET',
      pillarName: 'Market Intelligence',
      phase: 'Fase 1 (MVP)',
      badgeColor: 'bg-indigo-500/10 text-indigo-400 border-indigo-500/30',
      description: 'Descoberta de produtos, categorias, volumes e preços observados em marketplaces parceiros.',
      howItWorks: 'Consulta endpoints públicos autorizados via adaptadores oficiais (ex.: Mercado Livre API). Toda métrica de mercado é estritamente rotulada com sua proveniência (OBSERVED, IMPORTED, ESTIMATED ou UNAVAILABLE). Valores inexistentes nunca são convertidos em zero.',
      howToUse: '1. Acesse "Pesquisa de Mercado".\n2. Digite palavras-chave ou categorias na barra de pesquisa (ex.: "teclado mecanico").\n3. Analise métricas observadas de preço médio, reputação e concorrência.\n4. Salve pesquisas de interesse na lista de oportunidades com um clique.',
    },
    {
      id: 'DISCOVERY',
      name: 'Product Discovery (PDFs & Busca Visual)',
      pillar: 'MARKET',
      pillarName: 'Market Intelligence',
      phase: 'Fase 2',
      badgeColor: 'bg-indigo-500/10 text-indigo-400 border-indigo-500/30',
      description: 'Extração segura de dados de catálogos PDF de fornecedores e busca de similares por imagem.',
      howItWorks: 'Implementa upload seguro com sanitização contra ataques de injeção de prompt em arquivos PDF (OWASP LLM01). Extrai nome, SKU, EAN, dimensões e preço com score de confiança por campo. Requer revisão humana explícita antes de confirmar dados no catálogo.',
      howToUse: '1. Acesse "Product Discovery".\n2. Faça upload do arquivo PDF ou imagem do produto do fornecedor.\n3. Revise os campos extraídos na aba "Itens Extraídos" e aprove ou ajuste valores.\n4. Utilize a aba "Busca Visual" para encontrar produtos similares na base homologada.',
    },

    // 3. AI Commerce
    {
      id: 'AI_STUDIO',
      name: 'AI Listing Studio',
      pillar: 'AI',
      pillarName: 'AI Commerce',
      phase: 'Fase 1 (MVP)',
      badgeColor: 'bg-purple-500/10 text-purple-400 border-purple-500/30',
      description: 'Análise de qualidade de anúncios em 4 pilares auditáveis e redação assistida por inteligência artificial.',
      howItWorks: 'Calcula o Score de Qualidade (0 a 100) baseado em: Título (25%), Descrição (30%), Ficha Técnica (25%) e Fotos (20%). O motor de IA utiliza guardrails anti-alucinação rígidos para nunca inventar atributos técnicos não declarados pelo vendedor.',
      howToUse: '1. Acesse "AI Listing Studio".\n2. Selecione um anúncio para avaliar o Score e identificar lacunas de conversão.\n3. Clique em "Otimizar Título com IA" ou "Gerar Descrição Técnica".\n4. Revise as sugestões e clique em "Salvar Rascunho da Versão" para versionamento auditável.',
    },
    {
      id: 'ASSISTANT',
      name: 'Assistente Conversacional & Servidor MCP Read-Only',
      pillar: 'AI',
      pillarName: 'AI Commerce',
      phase: 'Fase 3',
      badgeColor: 'bg-purple-500/10 text-purple-400 border-purple-500/30',
      description: 'Assistente conversacional fundamentado em dados reais do seller e servidor Model Context Protocol (MCP) somente leitura.',
      howItWorks: 'Expõe catálogo de ferramentas MCP fortemente tipadas com schemas JSON rígidos. Bloqueia sumariamente qualquer tentativa de mutação externa (BLOCKED_MUTABLE_DENIED) e impede execução de comandos SQL arbitrários, injetando forçadamente o tenant_id em todas as consultas.',
      howToUse: '1. Acesse "Assistente & MCP".\n2. Converse em linguagem natural sobre indicadores, margens de produtos ou vendas.\n3. Visualize as ferramentas invocadas e a memória de cálculo explicável nas respostas.\n4. Na aba "Configuração MCP", gere e audite chaves seguras de API para IDEs e clientes MCP.',
    },

    // 4. Seller Control & Finanças
    {
      id: 'FINANCIAL',
      name: 'Motor Financeiro & Rentabilidade Real',
      pillar: 'FINANCE',
      pillarName: 'Seller Control & Finanças',
      phase: 'Fase 1 (MVP)',
      badgeColor: 'bg-teal-500/10 text-teal-400 border-teal-500/30',
      description: 'Cálculo financeiro determinístico em centavos (BigDecimal) de margem de contribuição, break-even e simulação de custos.',
      howItWorks: 'Proíbe estritamente o uso de ponto flutuante binário (float/double) e LLMs para cálculos monetários. Modela taxas de marketplace por canal, fretes, impostos por regime tributário e custo de mercadoria vendida (CMV), fornecendo memória de cálculo auditável.',
      howToUse: '1. Acesse "Motor Financeiro".\n2. No "Painel Financeiro & Rentabilidade", filtre períodos para ver Faturamento Bruto, Margem e Ticket Médio.\n3. Alterne para o "Simulador & Regras de Preço" para calcular preço mínimo e margem líquida simulada.\n4. Exporte relatórios auditáveis em formato CSV padronizado (RFC 4180).',
    },
    {
      id: 'RECONCILIATION_DRE',
      name: 'DRE Gerencial & Conciliação de Repasses',
      pillar: 'FINANCE',
      pillarName: 'Seller Control & Finanças',
      phase: 'Fase 2',
      badgeColor: 'bg-teal-500/10 text-teal-400 border-teal-500/30',
      description: 'Visão gerencial de resultado econômico (Regime de Competência) vs financeiro (Regime de Caixa / Repasses).',
      howItWorks: 'Importa arquivos de liquidação e extratos bancários de marketplaces de forma 100% idempotente (hash SHA-256). Cruza valores repassados com pedidos esperados, identificando divergências ao centavo e mantendo trilha de auditoria imutável.',
      howToUse: '1. Acesse "DRE & Conciliação".\n2. Na aba "Demonstrativo (DRE)", analise a composição de Receita, Deduções, CMV e Margem Operacional.\n3. Na aba "Conciliação de Repasses", identifique repasses CONCILIADOS, PENDENTES e DIVERGENTES.\n4. Abra protocolos internos de contestação com o disclaimer explícito de apuração gerencial.',
    },

    // 5. Gestão & Integrações
    {
      id: 'COMPANIES',
      name: 'Empresas & CNPJs (Estrutura Organizacional)',
      pillar: 'MANAGEMENT',
      pillarName: 'Gestão & Integrações',
      phase: 'Fase 1 (MVP)',
      badgeColor: 'bg-blue-500/10 text-blue-400 border-blue-500/30',
      description: 'Gerenciamento multi-CNPJ e parametrização informativa de enquadramento tributário por tenant.',
      howItWorks: 'Garante o isolamento lógico estrito de dados por tenant_id em nível de banco de dados e filtros de segurança. Permite associar múltiplos CNPJs à mesma organização sem consultas não autorizadas a bases fiscais externas.',
      howToUse: '1. Acesse "Empresas & CNPJs".\n2. Clique em "+ Nova Empresa" para cadastrar Razão Social, Nome Fantasia e CNPJ.\n3. Selecione o regime tributário da empresa (Simples Nacional, Lucro Presumido, Lucro Real, MEI).',
    },
    {
      id: 'STORES',
      name: 'Lojas & Marketplaces',
      pillar: 'MANAGEMENT',
      pillarName: 'Gestão & Integrações',
      phase: 'Fase 1 (MVP)',
      badgeColor: 'bg-blue-500/10 text-blue-400 border-blue-500/30',
      description: 'Vinculação de contas de canais (Mercado Livre, Shopee, Amazon) a empresas e gestão de credenciais.',
      howItWorks: 'Armazena tokens de autenticação criptografados em repouso com algoritmo AES-256-GCM. Aplica política Fail-Closed: impede requisições para lojas com tokens expirados ou sem homologação documental concluída.',
      howToUse: '1. Acesse "Lojas & Marketplaces".\n2. Clique em "+ Nova Loja" e selecione o marketplace parceiro e a empresa vinculada.\n3. Complete o fluxo oficial de autorização OAuth para habilitar sincronizações.',
    },
    {
      id: 'SYNC',
      name: 'Sincronização & Catálogo',
      pillar: 'MANAGEMENT',
      pillarName: 'Gestão & Integrações',
      phase: 'Fase 1 (MVP)',
      badgeColor: 'bg-blue-500/10 text-blue-400 border-blue-500/30',
      description: 'Central de processamento assíncrono de dados de catálogo, anúncios e pedidos.',
      howItWorks: 'Processa eventos de catálogo via RabbitMQ com filas Dead-Letter (DLQ) para resiliência a falhas de rede. Suporta recepção de Webhooks oficiais com validação de assinatura HMAC e contingência manual via upload de arquivos CSV.',
      howToUse: '1. Acesse "Sincronização & Catálogo".\n2. Acompanhe o status das tarefas em segundo plano e das filas de processamento.\n3. Utilize o botão "Sincronizar Agora" para disparar cargas manuais sob demanda.\n4. Em caso de instabilidade na API do marketplace, utilize a "Importação de Contingência por Arquivo".',
    },
    {
      id: 'CAPABILITIES',
      name: 'Capacidades & Conectores',
      pillar: 'MANAGEMENT',
      pillarName: 'Gestão & Integrações',
      phase: 'Fase 1 (MVP)',
      badgeColor: 'bg-blue-500/10 text-blue-400 border-blue-500/30',
      description: 'Matriz oficial de recursos, endpoints e permissões suportadas por marketplace parceiro.',
      howItWorks: 'Define com transparência técnica quais recursos são SUPPORTED (apoiados em APIs oficiais) e quais são UNSUPPORTED. Veda expressamente a criação de dados sintéticos ou scraping para simular capacidades não suportadas.',
      howToUse: '1. Acesse "Capacidades & Conectores".\n2. Consulte a matriz comparativa entre Mercado Livre, Shopee e Amazon.\n3. Verifique os métodos de autenticação vigentes e os limites operacionais de cada conector.',
    },

    // 6. Governança & Sistema
    {
      id: 'COMPLIANCE',
      name: 'Continuous Marketplace Compliance & Governança',
      pillar: 'GOVERNANCE',
      pillarName: 'Segurança & Governança',
      phase: 'Fase 3',
      badgeColor: 'bg-rose-500/10 text-rose-400 border-rose-500/30',
      description: 'Motor de governança permanente de termos de uso, monitoramento de APIs e reativação segura.',
      howItWorks: 'Monitora fontes oficiais de documentação via hashes SHA-256 com proteção anti-SSRF. Em caso de risco crítico, aplica FAIL CLOSED preventivo sob o Princípio do Menor Escopo (Least Scope), suspendendo somente a feature afetada. Proíbe estritamente a reativação automática: restauração exige assinatura humana, justificativa e evidência de testes.',
      howToUse: '1. Acesse "Compliance & Políticas".\n2. Monitore a integridade das políticas dos marketplaces integrados na aba "Overview".\n3. Consulte incidentes regulatórios abertos e planos de mitigação.\n4. Para reativar uma feature suspensa, preencha o formulário formal na aba "Reativação Segura".',
    },
    {
      id: 'HEALTH',
      name: 'Diagnóstico de Infraestrutura & Saúde',
      pillar: 'GOVERNANCE',
      pillarName: 'Segurança & Governança',
      phase: 'Fase 1 (MVP)',
      badgeColor: 'bg-rose-500/10 text-rose-400 border-rose-500/30',
      description: 'Monitoramento em tempo real da conectividade com banco de dados, mensageria e cache.',
      howItWorks: 'Consome os endpoints de `/actuator/health` do Spring Boot, atestando a integridade do PostgreSQL, RabbitMQ, Redis e microsserviços do backend sem vazar credenciais ou segredos em relatórios.',
      howToUse: '1. Acesse "Diagnóstico de Infraestrutura".\n2. Verifique o status de UP / DOWN de cada dependência do ecossistema.\n3. Monitore os tempos de resposta e a matriz técnica do monólito modular.',
    },
  ];

  const filteredCatalog = selectedPillar === 'ALL'
    ? modulesCatalog
    : modulesCatalog.filter((m) => m.pillar === selectedPillar);

  return (
    <div className="space-y-6">
      {/* Module Navigation Tabs */}
      <div className="flex flex-wrap gap-2 p-1.5 bg-slate-900/90 rounded-xl border border-slate-800 w-full sm:w-fit select-none">
        <button
          onClick={() => setActiveGuideTab('MODULES')}
          className={`px-3.5 py-2 text-xs sm:text-sm font-semibold rounded-lg transition-all min-h-[40px] flex items-center gap-1.5 ${
            activeGuideTab === 'MODULES' ? 'bg-blue-600 text-white shadow-md' : 'text-slate-400 hover:text-white'
          }`}
        >
          <span>📘 Módulos do Sistema</span>
        </button>

        <button
          onClick={() => setActiveGuideTab('SETUP')}
          className={`px-3.5 py-2 text-xs sm:text-sm font-semibold rounded-lg transition-all min-h-[40px] flex items-center gap-1.5 ${
            activeGuideTab === 'SETUP' ? 'bg-blue-600 text-white shadow-md' : 'text-slate-400 hover:text-white'
          }`}
        >
          <span>⚙️ Passo a Passo de Configuração</span>
        </button>

        <button
          onClick={() => setActiveGuideTab('EXTENSION')}
          className={`px-3.5 py-2 text-xs sm:text-sm font-semibold rounded-lg transition-all min-h-[40px] flex items-center gap-1.5 ${
            activeGuideTab === 'EXTENSION' ? 'bg-blue-600 text-white shadow-md' : 'text-slate-400 hover:text-white'
          }`}
        >
          <span>🧩 Extensão Chrome (Manifest V3)</span>
        </button>

        <button
          onClick={() => setActiveGuideTab('TESTING')}
          className={`px-3.5 py-2 text-xs sm:text-sm font-semibold rounded-lg transition-all min-h-[40px] flex items-center gap-1.5 ${
            activeGuideTab === 'TESTING' ? 'bg-blue-600 text-white shadow-md' : 'text-slate-400 hover:text-white'
          }`}
        >
          <span>🧪 Como Testar Localmente</span>
        </button>

        <button
          onClick={() => setActiveGuideTab('DEPLOY')}
          className={`px-3.5 py-2 text-xs sm:text-sm font-semibold rounded-lg transition-all min-h-[40px] flex items-center gap-1.5 ${
            activeGuideTab === 'DEPLOY' ? 'bg-blue-600 text-white shadow-md' : 'text-slate-400 hover:text-white'
          }`}
        >
          <span>🚀 Publicação em Produção</span>
        </button>
      </div>

      {/* =========================================================================
          TAB 1: MÓDULOS DO SISTEMA (VISÃO FUNCIONAL)
      ========================================================================= */}
      {activeGuideTab === 'MODULES' && (
        <div className="space-y-6">
          <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 shadow-sm">
            <h2 className="text-lg font-bold text-white flex items-center gap-2">
              <span>Catálogo Funcional da Seller Intelligence Platform</span>
            </h2>
            <p className="mt-1 text-xs sm:text-sm text-slate-300">
              Conheça em detalhes a arquitetura, objetivo e fluxo de uso dos 14 módulos integrados estruturados sobre os pilares estratégicos do produto.
            </p>

            {/* Pillar Filter Pills */}
            <div className="flex flex-wrap gap-1.5 mt-4 pt-3 border-t border-slate-800/80 text-xs">
              <button
                onClick={() => setSelectedPillar('ALL')}
                className={`px-3 py-1.5 rounded-lg border font-medium transition-colors ${
                  selectedPillar === 'ALL'
                    ? 'bg-blue-600 text-white border-blue-500 font-semibold'
                    : 'bg-slate-950 text-slate-400 border-slate-800 hover:text-white'
                }`}
              >
                Todos os Pilares ({modulesCatalog.length})
              </button>
              <button
                onClick={() => setSelectedPillar('BI')}
                className={`px-3 py-1.5 rounded-lg border font-medium transition-colors ${
                  selectedPillar === 'BI'
                    ? 'bg-emerald-600 text-white border-emerald-500 font-semibold'
                    : 'bg-slate-950 text-emerald-400/80 border-slate-800 hover:text-emerald-300'
                }`}
              >
                📊 BI &amp; Ações
              </button>
              <button
                onClick={() => setSelectedPillar('MARKET')}
                className={`px-3 py-1.5 rounded-lg border font-medium transition-colors ${
                  selectedPillar === 'MARKET'
                    ? 'bg-indigo-600 text-white border-indigo-500 font-semibold'
                    : 'bg-slate-950 text-indigo-400/80 border-slate-800 hover:text-indigo-300'
                }`}
              >
                🔍 Market Intelligence
              </button>
              <button
                onClick={() => setSelectedPillar('AI')}
                className={`px-3 py-1.5 rounded-lg border font-medium transition-colors ${
                  selectedPillar === 'AI'
                    ? 'bg-purple-600 text-white border-purple-500 font-semibold'
                    : 'bg-slate-950 text-purple-400/80 border-slate-800 hover:text-purple-300'
                }`}
              >
                🤖 AI Commerce
              </button>
              <button
                onClick={() => setSelectedPillar('FINANCE')}
                className={`px-3 py-1.5 rounded-lg border font-medium transition-colors ${
                  selectedPillar === 'FINANCE'
                    ? 'bg-teal-600 text-white border-teal-500 font-semibold'
                    : 'bg-slate-950 text-teal-400/80 border-slate-800 hover:text-teal-300'
                }`}
              >
                💰 Finanças &amp; Seller Control
              </button>
              <button
                onClick={() => setSelectedPillar('MANAGEMENT')}
                className={`px-3 py-1.5 rounded-lg border font-medium transition-colors ${
                  selectedPillar === 'MANAGEMENT'
                    ? 'bg-blue-600 text-white border-blue-500 font-semibold'
                    : 'bg-slate-950 text-blue-400/80 border-slate-800 hover:text-blue-300'
                }`}
              >
                🏬 Gestão &amp; Lojas
              </button>
              <button
                onClick={() => setSelectedPillar('GOVERNANCE')}
                className={`px-3 py-1.5 rounded-lg border font-medium transition-colors ${
                  selectedPillar === 'GOVERNANCE'
                    ? 'bg-rose-600 text-white border-rose-500 font-semibold'
                    : 'bg-slate-950 text-rose-400/80 border-slate-800 hover:text-rose-300'
                }`}
              >
                🛡️ Segurança &amp; Governança
              </button>
            </div>
          </div>

          {/* Cards Grid */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {filteredCatalog.map((mod) => (
              <div
                key={mod.id}
                className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 shadow-sm space-y-4 hover:border-slate-700 transition-all flex flex-col justify-between"
              >
                <div className="space-y-3">
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <span className="text-[10px] font-bold uppercase tracking-wider text-slate-400">
                        {mod.pillarName}
                      </span>
                      <h3 className="text-base font-bold text-white mt-0.5">{mod.name}</h3>
                    </div>
                    <span className={`text-[10px] px-2 py-0.5 rounded-full border font-medium shrink-0 ${mod.badgeColor}`}>
                      {mod.phase}
                    </span>
                  </div>

                  <p className="text-xs text-slate-300 leading-relaxed">
                    {mod.description}
                  </p>

                  <div className="p-3 bg-slate-950/70 border border-slate-800/80 rounded-lg space-y-1.5">
                    <div className="text-[11px] font-semibold text-blue-400 uppercase tracking-wide">
                      Mecânica &amp; Invariantes
                    </div>
                    <p className="text-[11px] text-slate-400 leading-relaxed whitespace-pre-line">
                      {mod.howItWorks}
                    </p>
                  </div>
                </div>

                <div className="pt-3 border-t border-slate-800/80 space-y-1.5">
                  <div className="text-[11px] font-semibold text-emerald-400 uppercase tracking-wide">
                    Como Usar na Prática
                  </div>
                  <p className="text-[11px] text-slate-300 leading-relaxed whitespace-pre-line font-mono bg-slate-950/40 p-2 rounded border border-slate-800/40">
                    {mod.howToUse}
                  </p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* =========================================================================
          TAB 2: PASSO A PASSO DE CONFIGURAÇÃO (SETUP)
      ========================================================================= */}
      {activeGuideTab === 'SETUP' && (
        <div className="space-y-6">
          <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 shadow-sm space-y-2">
            <h2 className="text-lg font-bold text-white">Guia Completo de Instalação e Inicialização Local</h2>
            <p className="text-xs sm:text-sm text-slate-300">
              Siga os passos determinísticos abaixo para preparar seu ambiente e inicializar todo o ecossistema com serviços de persistência e mensageria.
            </p>
          </div>

          {/* Step 1: Pre-requisites */}
          <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-5 space-y-3">
            <div className="flex items-center gap-2">
              <span className="flex h-6 w-6 items-center justify-center rounded-full bg-blue-600 text-xs font-bold text-white">1</span>
              <h3 className="text-sm font-bold text-white">Pré-requisitos Mandatórios da Stack</h3>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3 text-xs">
              <div className="p-3 bg-slate-950 rounded-lg border border-slate-800">
                <div className="font-bold text-white">Java 25 LTS</div>
                <p className="text-slate-400 mt-1">OpenJDK 25 com suporte a Virtual Threads e Spring Boot 3.5.</p>
              </div>
              <div className="p-3 bg-slate-950 rounded-lg border border-slate-800">
                <div className="font-bold text-white">Node.js 22+ &amp; pnpm 9+</div>
                <p className="text-slate-400 mt-1">Ambiente de execução JavaScript e gerenciador veloz do frontend.</p>
              </div>
              <div className="p-3 bg-slate-950 rounded-lg border border-slate-800">
                <div className="font-bold text-white">Docker &amp; Compose v2</div>
                <p className="text-slate-400 mt-1">Para os contêineres de PostgreSQL 17, Redis 7 e RabbitMQ 3.13.</p>
              </div>
              <div className="p-3 bg-slate-950 rounded-lg border border-slate-800">
                <div className="font-bold text-white">Apache Maven 3.9+</div>
                <p className="text-slate-400 mt-1">Para compilação, testes e execução dos módulos backend.</p>
              </div>
            </div>
          </div>

          {/* Step 2: Infrastructure containers */}
          <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-5 space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="flex h-6 w-6 items-center justify-center rounded-full bg-blue-600 text-xs font-bold text-white">2</span>
                <h3 className="text-sm font-bold text-white">Subir a Infraestrutura Base (Docker Compose)</h3>
              </div>
              <button
                onClick={() => copyToClipboard('docker compose up -d', 'cmd-docker')}
                className="text-xs px-2.5 py-1 rounded bg-slate-800 text-slate-300 hover:text-white border border-slate-700"
              >
                {copiedSnippet === 'cmd-docker' ? 'Copiado!' : 'Copiar'}
              </button>
            </div>
            <pre className="p-3 bg-slate-950 rounded-lg border border-slate-800 text-xs text-emerald-400 font-mono overflow-x-auto">
              docker compose up -d
            </pre>
            <p className="text-xs text-slate-400 leading-relaxed">
              Isso iniciará o contêiner <code>seller_postgres</code> na porta <strong>5432</strong>, o <code>seller_redis</code> na porta <strong>6379</strong>, o <code>seller_rabbitmq</code> na porta <strong>5672 (AMQP)</strong> / <strong>15672 (Dashboard)</strong> e o <code>seller_keycloak</code> na porta <strong>8085</strong>.
            </p>
          </div>

          {/* Step 3: Backend Boot */}
          <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-5 space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="flex h-6 w-6 items-center justify-center rounded-full bg-blue-600 text-xs font-bold text-white">3</span>
                <h3 className="text-sm font-bold text-white">Inicializar o Backend Spring Boot</h3>
              </div>
              <button
                onClick={() => copyToClipboard('mvn -f backend/pom.xml spring-boot:run', 'cmd-backend')}
                className="text-xs px-2.5 py-1 rounded bg-slate-800 text-slate-300 hover:text-white border border-slate-700"
              >
                {copiedSnippet === 'cmd-backend' ? 'Copiado!' : 'Copiar'}
              </button>
            </div>
            <pre className="p-3 bg-slate-950 rounded-lg border border-slate-800 text-xs text-emerald-400 font-mono overflow-x-auto">
              mvn -f backend/pom.xml spring-boot:run
            </pre>
            <p className="text-xs text-slate-400 leading-relaxed">
              O Flyway aplicará automaticamente as migrações <strong>V1 até V14</strong>, populando as tabelas e políticas oficiais. A API estará pronta em <code>http://localhost:8082</code> com telemetria em <code>/actuator/health</code>.
            </p>
          </div>

          {/* Step 4: Frontend Boot */}
          <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-5 space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="flex h-6 w-6 items-center justify-center rounded-full bg-blue-600 text-xs font-bold text-white">4</span>
                <h3 className="text-sm font-bold text-white">Instalar Dependências e Inicializar o Frontend Next.js</h3>
              </div>
              <button
                onClick={() => copyToClipboard('pnpm --dir frontend install && pnpm --dir frontend run dev', 'cmd-frontend')}
                className="text-xs px-2.5 py-1 rounded bg-slate-800 text-slate-300 hover:text-white border border-slate-700"
              >
                {copiedSnippet === 'cmd-frontend' ? 'Copiado!' : 'Copiar'}
              </button>
            </div>
            <pre className="p-3 bg-slate-950 rounded-lg border border-slate-800 text-xs text-emerald-400 font-mono overflow-x-auto">
              pnpm --dir frontend install && pnpm --dir frontend run dev
            </pre>
            <p className="text-xs text-slate-400 leading-relaxed">
              O frontend estará disponível em <code>http://localhost:3000</code> com suporte a Hot Module Replacement e renderização híbrida.
            </p>
          </div>
        </div>
      )}

      {/* =========================================================================
          TAB 3: EXTENSÃO CHROME MANIFEST V3
      ========================================================================= */}
      {activeGuideTab === 'EXTENSION' && (
        <div className="space-y-6">
          <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 shadow-sm space-y-2">
            <h2 className="text-lg font-bold text-white">Extensão Contextual Chrome (Manifest V3)</h2>
            <p className="text-xs sm:text-sm text-slate-300">
              Interface auxiliar e passiva para navegação no Mercado Livre e Shopee, concebida sob o Princípio do Menor Privilégio e sem coleta não autorizada.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-5 space-y-3">
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <span className="h-2 w-2 rounded-full bg-blue-400" />
                <span>O que a extensão faz</span>
              </h3>
              <ul className="text-xs text-slate-300 space-y-2 list-disc list-inside leading-relaxed">
                <li>Extrai de forma canônica o identificador público da página de anúncio (ex.: <code>MLB12345678</code> ou <code>item_id</code> na Shopee).</li>
                <li>Permite com um clique explícito do usuário enviar o anúncio observado para a **Pesquisa de Mercado** ou para o **AI Listing Studio**.</li>
                <li>Permite consultar rapidamente margem simulada ou score do anúncio diretamente pelo popup no navegador.</li>
              </ul>
            </div>

            <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-5 space-y-3">
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <span className="h-2 w-2 rounded-full bg-emerald-400" />
                <span>Invariantes de Segurança e Privacidade</span>
              </h3>
              <ul className="text-xs text-slate-300 space-y-2 list-disc list-inside leading-relaxed">
                <li><strong>Zero Coleta de Senhas ou Cookies:</strong> Não lê nem transmite cookies de login dos marketplaces.</li>
                <li><strong>Permissões Mínimas Estritas:</strong> Utiliza apenas <code>activeTab</code> (acionada ao clicar) e <code>storage</code> de sessão.</li>
                <li><strong>Sem Automação Invasiva:</strong> É terminantemente proibido checkout automático, bots de compra ou scraping massivo em segundo plano.</li>
                <li><strong>Páginas Sensíveis Bloqueadas:</strong> Checkout, carrinhos e dados de pagamento são sumariamente ignorados.</li>
              </ul>
            </div>
          </div>

          {/* Compilation and installation instructions */}
          <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-5 space-y-3">
            <h3 className="text-sm font-bold text-white">Como Compilar e Carregar a Extensão no Chrome</h3>
            <ol className="text-xs text-slate-300 space-y-3 list-decimal list-inside leading-relaxed">
              <li>
                <strong>Compilar os assets da extensão:</strong>
                <pre className="mt-1 p-2.5 bg-slate-950 rounded border border-slate-800 text-emerald-400 font-mono">
                  pnpm --dir extension install &amp;&amp; pnpm --dir extension run build
                </pre>
                Isso gerará o bundle compilado em <code>extension/dist</code> com o <code>manifest.json</code> V3.
              </li>
              <li>
                <strong>Abrir o gerenciador de extensões:</strong> No Google Chrome, Brave ou Edge, navegue até:
                <code className="ml-1 bg-slate-950 px-2 py-0.5 rounded border border-slate-800 text-blue-400 font-mono">chrome://extensions</code>
              </li>
              <li>
                <strong>Ativar o Modo do Desenvolvedor:</strong> Ligue a chave <em>&quot;Developer mode&quot;</em> no canto superior direito.
              </li>
              <li>
                <strong>Carregar extensão descompactada:</strong> Clique no botão <em>&quot;Load unpacked&quot; (Carregar sem compactação)</em> e selecione a pasta:
                <code className="ml-1 bg-slate-950 px-2 py-0.5 rounded border border-slate-800 text-amber-400 font-mono">.../antigravity-avantepro-clone/extension/dist</code>
              </li>
              <li>
                <strong>Usar no Marketplace:</strong> Abra qualquer anúncio público no Mercado Livre ou Shopee e clique no ícone da <strong>SI</strong> na barra de ferramentas do navegador.
              </li>
            </ol>
          </div>
        </div>
      )}

      {/* =========================================================================
          TAB 4: COMO TESTAR LOCALMENTE
      ========================================================================= */}
      {activeGuideTab === 'TESTING' && (
        <div className="space-y-6">
          <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 shadow-sm space-y-2">
            <h2 className="text-lg font-bold text-white">Guia de Testes Automatizados e Homologação Local</h2>
            <p className="text-xs sm:text-sm text-slate-300">
              A suíte de testes da plataforma conta com mais de 350 testes automatizados cobrindo integração, contratos, segurança adversarial e fluxos ponta a ponta.
            </p>
          </div>

          <div className="space-y-4">
            {/* Backend Tests */}
            <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-4 space-y-2">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-bold text-white flex items-center gap-2">
                  <span className="px-2 py-0.5 rounded bg-blue-500/10 text-blue-400 text-xs font-mono">Backend</span>
                  <span>Executar Suíte Completa de Testes Spring Boot</span>
                </h3>
                <button
                  onClick={() => copyToClipboard('mvn -f backend/pom.xml test', 'cmd-test-backend')}
                  className="text-xs px-2.5 py-1 rounded bg-slate-800 text-slate-300 hover:text-white border border-slate-700"
                >
                  {copiedSnippet === 'cmd-test-backend' ? 'Copiado!' : 'Copiar'}
                </button>
              </div>
              <pre className="p-3 bg-slate-950 rounded-lg border border-slate-800 text-xs text-emerald-400 font-mono">
                mvn -f backend/pom.xml test
              </pre>
              <p className="text-xs text-slate-400">
                Executa mais de 250 testes com H2 in-memory, Testcontainers, isolamento multi-tenant adversarial e verificações do MarketplaceComplianceGuard.
              </p>
            </div>

            {/* Frontend Tests */}
            <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-4 space-y-2">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-bold text-white flex items-center gap-2">
                  <span className="px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 text-xs font-mono">Frontend</span>
                  <span>Executar Suíte de Testes Vitest (Next.js &amp; Componentes)</span>
                </h3>
                <button
                  onClick={() => copyToClipboard('pnpm --dir frontend run test', 'cmd-test-frontend')}
                  className="text-xs px-2.5 py-1 rounded bg-slate-800 text-slate-300 hover:text-white border border-slate-700"
                >
                  {copiedSnippet === 'cmd-test-frontend' ? 'Copiado!' : 'Copiar'}
                </button>
              </div>
              <pre className="p-3 bg-slate-950 rounded-lg border border-slate-800 text-xs text-emerald-400 font-mono">
                pnpm --dir frontend run test
              </pre>
              <p className="text-xs text-slate-400">
                Executa 74 testes em 16 arquivos cobrindo todos os módulos interativos, validação de CNPJs, simulação financeira e navegação mobile-first.
              </p>
            </div>

            {/* Extension Tests */}
            <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-4 space-y-2">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-bold text-white flex items-center gap-2">
                  <span className="px-2 py-0.5 rounded bg-purple-500/10 text-purple-400 text-xs font-mono">Extensão</span>
                  <span>Testes Unitários de Parsers e Anti-Scraping Guard</span>
                </h3>
                <button
                  onClick={() => copyToClipboard('pnpm --dir extension run test', 'cmd-test-ext')}
                  className="text-xs px-2.5 py-1 rounded bg-slate-800 text-slate-300 hover:text-white border border-slate-700"
                >
                  {copiedSnippet === 'cmd-test-ext' ? 'Copiado!' : 'Copiar'}
                </button>
              </div>
              <pre className="p-3 bg-slate-950 rounded-lg border border-slate-800 text-xs text-emerald-400 font-mono">
                pnpm --dir extension run test
              </pre>
              <p className="text-xs text-slate-400">
                Valida a extração de identificadores canônicos e a rejeição preventiva de páginas de checkout e pagamento.
              </p>
            </div>

            {/* Gate Verification Scripts */}
            <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-4 space-y-2">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-bold text-white flex items-center gap-2">
                  <span className="px-2 py-0.5 rounded bg-amber-500/10 text-amber-400 text-xs font-mono">Gate Scripts</span>
                  <span>Verificação Automatizada de Critérios de Aceite</span>
                </h3>
                <button
                  onClick={() => copyToClipboard('python3 scripts/verify_phase21.py', 'cmd-test-gate')}
                  className="text-xs px-2.5 py-1 rounded bg-slate-800 text-slate-300 hover:text-white border border-slate-700"
                >
                  {copiedSnippet === 'cmd-test-gate' ? 'Copiado!' : 'Copiar'}
                </button>
              </div>
              <pre className="p-3 bg-slate-950 rounded-lg border border-slate-800 text-xs text-emerald-400 font-mono">
                python3 scripts/verify_phase21.py
              </pre>
              <p className="text-xs text-slate-400">
                Audita automaticamente a integridade das tabelas, regras anti-SSRF, menor escopo, modelos e documentações em <code>docs/compliance/</code>.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* =========================================================================
          TAB 5: COMO PUBLICAR EM AMBIENTE PRODUTIVO
      ========================================================================= */}
      {activeGuideTab === 'DEPLOY' && (
        <div className="space-y-6">
          <div className="rounded-xl border border-slate-800 bg-slate-900/60 p-5 shadow-sm space-y-2">
            <h2 className="text-lg font-bold text-white">Guia de Publicação e Operação em Produção</h2>
            <p className="text-xs sm:text-sm text-slate-300">
              Procedimentos padronizados para compilação, containerização, deploy em nuvem e submissão da extensão na Chrome Web Store.
            </p>
          </div>

          <div className="space-y-4">
            {/* Checklist de Segurança */}
            <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-5 space-y-3">
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <span className="h-2 w-2 rounded-full bg-emerald-400" />
                <span>Checklist de Segurança Antes do Deploy</span>
              </h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                <div className="p-3 bg-slate-950 rounded border border-slate-800 space-y-1">
                  <div className="font-semibold text-white">1. Criptografia de Chaves e Tokens</div>
                  <p className="text-slate-400">Configurar <code>ENCRYPTION_SECRET_KEY</code> de 256 bits para criptografia AES-GCM dos tokens de marketplace.</p>
                </div>
                <div className="p-3 bg-slate-950 rounded border border-slate-800 space-y-1">
                  <div className="font-semibold text-white">2. Provedor de Segredos Externo</div>
                  <p className="text-slate-400">Injetar segredos via AWS Secrets Manager, HashiCorp Vault ou Kubernetes Secrets. Nunca commitar credenciais.</p>
                </div>
                <div className="p-3 bg-slate-950 rounded border border-slate-800 space-y-1">
                  <div className="font-semibold text-white">3. Política Fail Closed Ativa</div>
                  <p className="text-slate-400">Garantir que <code>seller.compliance.allow-fake-marketplace=false</code> esteja configurado em produção.</p>
                </div>
                <div className="p-3 bg-slate-950 rounded border border-slate-800 space-y-1">
                  <div className="font-semibold text-white">4. TLS 1.3 &amp; Headers de Segurança</div>
                  <p className="text-slate-400">Configurar terminação TLS no proxy reverso com HSTS, CSP estrita e proteção contra framing.</p>
                </div>
              </div>
            </div>

            {/* Containerization */}
            <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-5 space-y-3">
              <h3 className="text-sm font-bold text-white">Build dos Contêineres de Produção (Docker Multi-stage)</h3>
              <p className="text-xs text-slate-300">
                Gere imagens otimizadas e sem ferramentas de build nos contêineres finais:
              </p>
              <pre className="p-3 bg-slate-950 rounded-lg border border-slate-800 text-xs text-emerald-400 font-mono overflow-x-auto">
{`# 1. Build da Imagem Backend (Spring Boot em container distroless/alpine com Java 25)
docker build -t seller-intelligence-backend:latest -f backend/Dockerfile .

# 2. Build da Imagem Frontend (Next.js standalone otimizado)
docker build -t seller-intelligence-frontend:latest -f frontend/Dockerfile .`}
              </pre>
            </div>

            {/* Publishing Extension */}
            <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-5 space-y-3">
              <h3 className="text-sm font-bold text-white">Publicação da Extensão na Chrome Web Store</h3>
              <ol className="text-xs text-slate-300 space-y-2 list-decimal list-inside leading-relaxed">
                <li>
                  Gere o arquivo ZIP da extensão compilada:
                  <code className="ml-1 bg-slate-950 px-2 py-0.5 rounded text-blue-400 font-mono">cd extension/dist &amp;&amp; zip -r ../extension-v1.0.0.zip .</code>
                </li>
                <li>Acesse o <strong>Chrome Web Store Developer Dashboard</strong> (<a href="https://chrome.google.com/webstore/devconsole" target="_blank" rel="noreferrer" className="text-blue-400 underline">link</a>).</li>
                <li>Clique em <em>&quot;Novo item&quot;</em> e faça o upload do arquivo <code>extension-v1.0.0.zip</code>.</li>
                <li>
                  Na seção de <strong>Práticas de Privacidade</strong>, declare a justificativa de permissões mínimas:
                  <div className="mt-1 p-2 bg-slate-950 rounded border border-slate-800 text-[11px] text-slate-400 italic">
                    &quot;A permissão &apos;activeTab&apos; é utilizada exclusivamente sob demanda ao clicar no popup para identificar o ID público do produto em páginas de marketplace parceiros (Mercado Livre e Shopee). Nenhum cookie de sessão, senha ou dado de navegação é coletado ou vendido.&quot;
                  </div>
                </li>
                <li>Submeta para revisão da Google com homologação Manifest V3.</li>
              </ol>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
