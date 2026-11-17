# Registro de Decisões de Arquitetura (DECISION_LOG / ADRs)

## Visão Geral
Este documento registra formalmente as Decisões de Arquitetura de Software (ADRs) da **Seller Intelligence Platform**.
Conforme estipulado no PRD ([docs/product/PRD.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/product/PRD.md), Seção 19 e 21), este registro divide-se estritamente entre:
- **Parte 1: Decisões Aprovadas (Baseline Vinculante pelo PRD e Prompt 00)** — `ACCEPTED`
- **Parte 2: Decisões Propostas / Pendentes de Validação Técnica** — `PROPOSED` (vinculadas ao Prompt 01 e às Open Questions)

---

# Parte 1: Decisões Aprovadas (ACCEPTED)

## DEC-001 / ADR-001 — Adoção de Monólito Modular com Processamento Assíncrono
- **Data:** 2026-11-17
- **Status:** `ACCEPTED`
- **Contexto:** Necessidade de construir uma plataforma SaaS com diversos domínios interligados (Identidade, Tenancy, Conectores, Catálogo, Financeiro, IA, BI) sem a sobrecarga operacional prematura e a complexidade de redes distribuídas de microsserviços.
- **Opções consideradas:**
  1. Microsserviços distribuídos desde o início (Kubernetes, gRPC, bancos por serviço).
  2. Monólito tradicional acoplado sem limites de domínio.
  3. Monólito modular com fronteiras de módulos reforçadas e workers assíncronos.
- **Decisão:** Opção 3. Adotar monólito modular em Spring Boot com Spring Modulith e ArchUnit para garantia de fronteiras, com mensageria assíncrona via RabbitMQ.
- **Justificativa:** Menor complexidade operacional, transações ACID simplificadas no PostgreSQL, deploy unificado e facilidade de refatoração, mantendo separação estrita para futura extração se justificada por métricas reais de escala.
- **Consequências:** Exige disciplina estrita de arquitetura para não violar limites de pacotes; dependências cíclicas são bloqueadas pelo ArchUnit no build.
- **Requisitos relacionados:** NFR-SCL-001, NFR-MNT-001, PRD Seção 8.1.
- **Responsável:** Tech Lead / Arquiteto de Software.
- **Revisão:** Fase 3 ou quando um domínio atingir gargalo específico de IO/CPU não solucionável verticalmente.

---

## DEC-002 / ADR-002 — PostgreSQL como Fonte Única da Verdade Transacional e Numéricos Determinísticos
- **Data:** 2026-11-17
- **Status:** `ACCEPTED`
- **Contexto:** O produto manipula transações financeiras críticas, conciliação de repasses, rateios tributários e metadados de catálogo que exigem garantias ACID absolutas e precisão matemática.
- **Opções consideradas:**
  1. Banco NoSQL / Document store (MongoDB, DynamoDB).
  2. Banco relacional PostgreSQL 18.x com tipos `NUMERIC` para valores monetários.
  3. Arquitetura híbrida transacional e analítica prematura.
- **Decisão:** Opção 2. PostgreSQL 18.x como fonte primária da verdade transacional, usando `NUMERIC` / `BigDecimal`.
- **Justificativa:** Maturidade excepcional, suporte a JSONB para payloads variáveis de marketplaces, garantias ACID, índices avançados e suporte nativo a Row-Level Security (RLS) para defesa em profundidade multi-tenant.
- **Consequências:** Requer modelagem cuidadosa de índices e particionamento temporal para tabelas de histórico e snapshots de mercado em volumes elevados.
- **Requisitos relacionados:** FR-FIN-001 a 006, NFR-SEC-002, PRD Seção 6.2 e 8.4.
- **Responsável:** Engenheiro de Dados / Arquiteto de Software.
- **Revisão:** Avaliar storage analítico dedicado (ClickHouse ou particionamento) apenas se o volume de snapshots ultrapassar a capacidade do PostgreSQL na Fase 2.

---

## DEC-003 / ADR-003 — Contratos de Conectores Marketplace-Agnostic e Isolamento por Adapters
- **Data:** 2026-11-17
- **Status:** `ACCEPTED`
- **Contexto:** A plataforma integra múltiplos canais (Mercado Livre e Shopee no MVP; Amazon, TikTok Shop e Magalu nas fases seguintes). Cada canal possui particularidades de autenticação, taxonomia, termos e estruturas de dados.
- **Opções consideradas:**
  1. Condicionais espalhados nas entidades e serviços centrais (`if (channel == "ML") ...`).
  2. Camada desacoplada de adaptadores implementando interfaces agnósticas de domínio (`MarketplaceConnector`).
- **Decisão:** Opção 2. Interface abstrata comum de conectores com adaptadores dedicados e isolados por marketplace.
- **Justificativa:** Protege o modelo de domínio central contra mudanças frequentes de APIs externas; permite plugar novos marketplaces sem impactar regras existentes.
- **Consequências:** Necessidade de manter DTOs de normalização e conversores bidirecionais entre os modelos externos e internos.
- **Requisitos relacionados:** FR-INT-001 a 009, NFR-MNT-001, PRD Seção 8.3.
- **Responsável:** Tech Lead de Integrações.
- **Revisão:** A cada novo conector adicionado.

---

## DEC-004 / ADR-004 — Fontes Oficiais e Licenciadas como Regra Primária de Ingestão de Dados
- **Data:** 2026-11-17
- **Status:** `ACCEPTED`
- **Contexto:** Obtenção de dados de catálogo, vendas e concorrência para inteligência de mercado sem violar termos de serviço, políticas legais ou controles de segurança dos marketplaces.
- **Opções consideradas:**
  1. Scraping não autorizado contornando proteções, CAPTCHAs e bloqueios técnicos.
  2. Exclusivamente APIs oficiais/autorizadas pelo seller, arquivos enviados pelo usuário (CSV/XLSX) e fontes de dados devidamente licenciadas.
- **Decisão:** Opção 2. É proibido qualquer mecanismo de scraping hostil ou que burle autenticações, limites de taxa ou termos de serviço.
- **Justificativa:** Segurança jurídica para os usuários e para o produto, sustentabilidade operacional, conformidade com LGPD e prevenção contra bloqueios permanentes de IP/contas.
- **Consequências:** Indicadores que os canais não disponibilizarem legitimamente serão declarados como `UNAVAILABLE` ou estimados com rotulagem explícita e explicável, nunca forjados.
- **Requisitos relacionados:** FR-MKT-001 a 004, NFR-SEC-005, PRD Seção 1.2 e 4.2.
- **Responsável:** Tech Lead / Jurídico.
- **Revisão:** Análise de viabilidade no Prompt 01.

---

## DEC-005 / ADR-005 — Motor Financeiro Determinístico e Versionado em BigDecimal
- **Data:** 2026-11-17
- **Status:** `ACCEPTED`
- **Contexto:** Vendedores tomam decisões críticas de precificação e compras baseadas no lucro e margem de contribuição. Erros de arredondamento ou alucinações de modelos de linguagem destroem a confiança no produto.
- **Opções consideradas:**
  1. Cálculo de margem por prompts de IA ou funções heurísticas livres.
  2. Ponto flutuante binário (`float` / `double`) no backend.
  3. Motor de cálculo determinístico em Java utilizando `BigDecimal`, regras com vigência temporal e geração de memória de cálculo detalhada.
- **Decisão:** Opção 3. Cálculos estritamente matemáticos em `BigDecimal` com escala e modo de arredondamento padronizados (`RoundingMode.HALF_EVEN`), com regras parametrizadas e versionadas.
- **Justificativa:** Garantia de reprodutibilidade exata (mesmas entradas + mesma versão de regra = mesmo resultado), rastreabilidade de centavos e conformidade contábil.
- **Consequências:** Mudanças em regras de comissão ou frete exigem criação de nova versão sem sobrescrever períodos fechados retroativamente.
- **Requisitos relacionados:** FR-FIN-001 a 011, NFR-MNT-002, PRD Seção 6.2.
- **Responsável:** Especialista Financeiro / Engenheiro Backend.
- **Revisão:** Validação com suíte de regressão financeira (PRD Seção 13.2).

---

## DEC-006 / ADR-006 — Proveniência Obrigatória e Metadados em Todos os Indicadores de Mercado
- **Data:** 2026-11-17
- **Status:** `ACCEPTED`
- **Contexto:** Dados de mercado, concorrentes e pesquisas de produtos variam em atualização e confiabilidade. O usuário precisa saber com clareza o que é observado diretamente e o que é inferido.
- **Opções consideradas:**
  1. Exibir números absolutos sem contextualizar a origem.
  2. Armazenar e exibir metadados padronizados: `source`, `collectedAt`, `scope` e classificação (`OBSERVED`, `IMPORTED`, `ESTIMATED`, `AI_GENERATED`, `UNAVAILABLE`).
- **Decisão:** Opção 2. Todo dado ou indicador carrega proveniência obrigatória.
- **Justificativa:** Transparência radical, proteção contra acusações de dados falsos e clareza analítica.
- **Consequências:** Entidades de snapshot e DTOs de resposta devem incluir o bloco de proveniência.
- **Requisitos relacionados:** FR-MKT-004, FR-CMP-003, PRD Seção 6.1.
- **Responsável:** Product Manager / Engenheiro de Dados.
- **Revisão:** Contínua em todos os endpoints de mercado.

---

## DEC-007 / ADR-007 — MCP Inicialmente Read-Only e Proteção Rigorosa de Ações Mutáveis
- **Data:** 2026-11-17
- **Status:** `ACCEPTED`
- **Contexto:** O Model Context Protocol (MCP) conecta modelos de linguagem ao contexto operacional do seller. Ferramentas mutáveis descontroladas podem pausar anúncios, alterar preços ou corromper inventário por alucinação.
- **Opções consideradas:**
  1. Liberar ferramentas de escrita e edição diretamente no chat/MCP sem fricção.
  2. Implementar servidor MCP estritamente read-only na primeira entrega (Fase 3), segregando ferramentas mutáveis para fluxos que exigem confirmação explícita na UI.
- **Decisão:** Opção 2. Ferramentas MCP iniciais apenas para leitura e relatórios; ações externas mutáveis exigem confirmação humana e tela de prévia de impacto.
- **Justificativa:** Segurança de dados dos clientes e prevenção contra perdas financeiras decorrentes de injeção de prompt ou comportamento imprevisível de agentes.
- **Consequências:** Ações como alteração de preço exigem tela de confirmação e auditoria, mesmo se iniciadas via assistente conversacional.
- **Requisitos relacionados:** FR-AI-001 a 007, NFR-AI-002, PRD Seção 6.5 e 12.3.
- **Responsável:** Arquiteto de IA / Segurança da Informação.
- **Revisão:** Fase 3 (Prompt 18).

---

## DEC-008 / ADR-008 — Multi-Tenancy Estrutural com Isolamento no Backend e no Banco
- **Data:** 2026-11-17
- **Status:** `ACCEPTED`
- **Contexto:** A plataforma armazena dados comerciais sigilosos de diferentes concorrentes e sellers. O vazamento de dados entre organizações representa falha catastrófica de segurança e conformidade LGPD.
- **Opções consideradas:**
  1. Banco de dados separado por tenant (custo proibitivo e complexidade de manutenção no MVP).
  2. Banco compartilhado com filtro de tenant exclusivamente na camada visual/frontend.
  3. Banco compartilhado com coluna `tenant_id` obrigatória, validação em todas as queries no backend e reforço por Row-Level Security (RLS) no PostgreSQL.
- **Decisão:** Opção 3. Isolamento multi-tenant estrutural com defesa em profundidade: `tenant_id` obrigatório em todo schema transacional, verificação em interceptors de segurança e RLS nas tabelas sensíveis.
- **Justificativa:** Eficiência de infraestrutura combinada com proteção rigorosa contra quebra de isolamento.
- **Consequências:** Todo teste unitário e de integração deve validar cenários adversariais cross-tenant.
- **Requisitos relacionados:** FR-ID-004, NFR-SEC-002, PRD Seção 3.2 e 9.1.
- **Responsável:** Arquiteto de Segurança / Tech Lead.
- **Revisão:** Prompt 03 e testes E2E do Prompt 12.

---

## DEC-009 / ADR-009 — Baseline de Tecnologias de Engenharia e Operação
- **Data:** 2026-11-17
- **Status:** `ACCEPTED`
- **Contexto:** Padronização da infraestrutura de desenvolvimento e produção para garantir previsibilidade, suporte LTS e ecossistema maduro.
- **Opções consideradas:**
  1. Pilha poliglota heterogênea.
  2. Stack padronizada: Java 21 LTS + Spring Boot 3.5.x, Next.js App Router + TypeScript, PostgreSQL 18.x, Redis 7/8, RabbitMQ 4.x, Keycloak OIDC e Docker Compose.
- **Decisão:** Opção 2. Fixar stack baseline indicada no PRD Seção 8.4 e nas regras de arquitetura.
- **Justificativa:** Alta produtividade, estabilidade comprovada para ecossistemas corporativos e robustez tipada tanto no backend quanto no frontend.
- **Consequências:** Nenhuma dependência maior pode ser alterada sem criação de novo ADR e justificativa técnica.
- **Requisitos relacionados:** NFR-MNT-001, PRD Seção 8.4.
- **Responsável:** Time de Engenharia.
- **Revisão:** Anual ou mediante término do ciclo de suporte dos componentes LTS.

---

# Parte 2: Decisões Propostas e Pendentes de Validação (PROPOSED)

## DEC-P01 — Matriz de Capacidades e Escopos OAuth por Marketplace (Vinculada a OQ-001)
- **Status:** `PROPOSED` / `PENDING_VALIDATION` (Escopo do Prompt 01)
- **Contexto:** Definir com base em documentação oficial da API do Mercado Livre e Shopee quais escopos exatos serão requisitados (ex.: `read_orders`, `write_listings`).
- **Pendência:** Aguardando conclusão da investigação técnica da Fase 0 (Prompt 01).

## DEC-P02 — Mecanismo e Viabilidade Legal de Coleta de Preços de Concorrentes (Vinculada a OQ-002)
- **Status:** `PROPOSED` / `PENDING_VALIDATION` (Escopo do Prompt 01)
- **Contexto:** Determinar quais fontes legítimas e licenciadas ou APIs públicas homologadas serão usadas para monitoramento de concorrentes sem violar termos de uso.
- **Pendência:** Análise jurídica e técnica de viabilidade no Prompt 01.

## DEC-P03 — Arquitetura de Comunicação da Extensão Chrome Manifest V3 (Vinculada a OQ-003)
- **Status:** `PROPOSED` / `PENDING_VALIDATION` (Escopo do Prompt 01 e Prompt 11)
- **Contexto:** Definir se a extensão funcionará exclusivamente enviando o identificador do anúncio atual para consulta no backend via sessão autenticada, sem ler cookies ou dados privados de navegação.
- **Pendência:** Validação de segurança e políticas da Chrome Web Store no Prompt 01.

## DEC-P04 — Parametrização dos Regimes Tributários Brasileiros no Motor Financeiro (Vinculada a OQ-004 e OQ-005)
- **Status:** `PROPOSED` / `PENDING_VALIDATION` (Escopo do Prompt 01 e Prompt 08)
- **Contexto:** Definir como Simples Nacional (com faixas de faturamento), Lucro Presumido e Lucro Real serão parametrizados nas regras de dedução de impostos sobre vendas.
- **Pendência:** Alinhamento com especialista contábil/fiscal durante o Prompt 01.

## DEC-P05 — Política de Retenção de Dados e Expurgos Automáticos LGPD (Vinculada a OQ-007)
- **Status:** `PROPOSED` / `PENDING_VALIDATION` (Escopo do Prompt 01)
- **Contexto:** Estabelecer tempos de guarda para logs de auditoria (ex.: 5 anos), snapshots de mercado (ex.: 12 meses) e dados anonimizados de compradores.
- **Pendência:** Definição com encarregado de privacidade (DPO) no Prompt 01.
