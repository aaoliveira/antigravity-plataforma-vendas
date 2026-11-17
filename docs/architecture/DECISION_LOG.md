# Registro de Decisões de Arquitetura (DECISION_LOG / ADRs)

## Visão Geral
Este documento registra formalmente as Decisões de Arquitetura de Software (ADRs) da **Seller Intelligence Platform**.
Conforme estipulado no PRD ([docs/product/PRD.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/product/PRD.md), Seção 19 e 21) e refinado pela Descoberta Técnica do **Prompt 01**, este registro estrutura-se em:
- **Parte 1: Decisões de Arquitetura Aprovadas (ACCEPTED)**
- **Parte 2: Decisões Propostas e Pendências Classificadas (PROPOSED)**

---

# Parte 1: Decisões Aprovadas (ACCEPTED)

## DEC-001 / ADR-001 — Adoção de Monólito Modular com Processamento Assíncrono
- **Data:** 2026-11-17
- **Status:** `ACCEPTED`
- **Contexto:** Necessidade de construir uma plataforma SaaS com múltiplos domínios interligados (Identidade, Tenancy, Conectores, Catálogo, Financeiro, IA, BI) sem a sobrecarga operacional prematura e latência de rede de microsserviços.
- **Opções consideradas:**
  1. Microsserviços distribuídos desde o início (Kubernetes, gRPC, bancos separados por serviço).
  2. Monólito tradicional acoplado sem limites de domínio.
  3. Monólito modular com fronteiras de módulos reforçadas e workers assíncronos.
- **Decisão:** Opção 3. Adotar monólito modular em Spring Boot com Spring Modulith e ArchUnit para garantia de fronteiras, com mensageria assíncrona via RabbitMQ 4.x.
- **Justificativa:** Menor complexidade operacional, transações ACID garantidas no PostgreSQL, deploy unificado e facilidade de refatoração, mantendo limites limpos para futura extração caso métricas reais de escala justifiquem.
- **Consequências:** Exige disciplina estrita de arquitetura para não violar limites de pacotes; dependências cíclicas entre módulos são bloqueadas pelo ArchUnit no build do Maven.
- **Requisitos relacionados:** NFR-SCL-001, NFR-MNT-001, PRD Seção 8.1.
- **Responsável:** Tech Lead / Arquiteto de Software.
- **Revisão:** Fase 3 ou quando um domínio atingir gargalo comprovado de concorrência ou escala.

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
- **Justificativa:** Maturidade excepcional, suporte a JSONB para payloads variáveis de marketplaces, garantias ACID, índices avançados, suporte nativo a `uuidv7()` (PostgreSQL 18) e Row-Level Security (RLS) para defesa em profundidade multi-tenant.
- **Consequências:** Requer modelagem cuidadosa de índices compostos (`tenant_id, ...`) e particionamento temporal para tabelas de histórico e snapshots de mercado em volumes elevados.
- **Requisitos relacionados:** FR-FIN-001 a 006, NFR-SEC-002, PRD Seção 6.2 e 8.4.
- **Responsável:** Engenheiro de Dados / Arquiteto de Software.
- **Revisão:** Avaliar storage analítico dedicado (ClickHouse ou particionamento nativo) apenas se o volume de snapshots ultrapassar a capacidade do PostgreSQL na Fase 2.

---

## DEC-003 / ADR-003 — Contratos de Conectores Marketplace-Agnostic e Isolamento por Adapters
- **Data:** 2026-11-17
- **Status:** `ACCEPTED`
- **Contexto:** A plataforma integra múltiplos canais (Mercado Livre e Shopee no MVP; Amazon, TikTok Shop e Magalu nas fases seguintes). Cada canal possui particularidades de autenticação, taxonomia, termos e estruturas de dados.
- **Opções consideradas:**
  1. Condicionais espalhados nas entidades e serviços centrais (`if (channel == "MERCADO_LIVRE") ...`).
  2. Camada desacoplada de adaptadores implementando interfaces agnósticas de domínio (`MarketplaceConnector`).
- **Decisão:** Opção 2. Interface abstrata comum de conectores com adaptadores dedicados e isolados por marketplace (`MercadoLivreConnectorAdapter`, `ShopeeConnectorAdapter`).
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
- **Decisão:** Opção 2. É expressamente proibido qualquer mecanismo de scraping hostil ou que burle autenticações, limites de taxa ou termos de serviço.
- **Justificativa:** Segurança jurídica para os usuários e para o produto, sustentabilidade operacional, conformidade com LGPD e prevenção contra bloqueios permanentes de IP/contas.
- **Consequências:** Indicadores que os canais não disponibilizarem legitimamente serão declarados como `UNAVAILABLE` ou estimados com rotulagem explícita e explicável, nunca forjados.
- **Requisitos relacionados:** FR-MKT-001 a 004, NFR-SEC-005, PRD Seção 1.2 e 4.2.
- **Responsável:** Tech Lead / Jurídico.
- **Revisão:** Análise de viabilidade realizada no Prompt 01.

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
- **Decisão:** Opção 2. Todo dado ou indicador carrega proveniência obrigatória e explícita.
- **Justificativa:** Transparência radical, proteção contra acusações de dados falsos e clareza analítica.
- **Consequências:** Entidades de snapshot e DTOs de resposta devem incluir o bloco de proveniência estruturado.
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

## DEC-009 / ADR-009 — Baseline de Tecnologias de Engenharia e Operação (Validado na Fase 0)
- **Data:** 2026-11-17
- **Status:** `ACCEPTED`
- **Contexto:** Padronização da infraestrutura de desenvolvimento e produção para garantir previsibilidade, suporte LTS e ecossistema maduro. A Descoberta Técnica do Prompt 01 auditou o suporte oficial dos componentes.
- **Opções consideradas:**
  1. Pilha poliglota heterogênea.
  2. Stack padronizada: Java 21 LTS + Spring Boot 3.5.x, Next.js App Router + TypeScript, PostgreSQL 18.x, Redis 7/8, RabbitMQ 4.x, Keycloak OIDC e Docker Compose.
- **Decisão:** Opção 2. Fixar stack baseline indicada no PRD Seção 8.4 e auditada no Prompt 01:
  - Backend: Java 21 LTS + Spring Boot 3.5.x (Spring Framework 6.2, Spring Modulith, ArchUnit).
  - Persistência: PostgreSQL 18.x com `NUMERIC` para finanças e `uuidv7()` nativo.
  - Mensageria: RabbitMQ 4.x com exchanges tópicos e Dead-Letter Queues.
  - Frontend: Next.js (App Router) + React 19 + TypeScript + Tailwind CSS / shadcn/ui.
  - Identidade: Keycloak 26.x (OIDC) local e IdP gerenciado em produção.
- **Justificativa:** Alta produtividade, estabilidade comprovada para ecossistemas corporativos e robustez tipada tanto no backend quanto no frontend.
- **Consequências:** Nenhuma dependência maior pode ser alterada sem criação de novo ADR e justificativa técnica.
- **Requisitos relacionados:** NFR-MNT-001, PRD Seção 8.4.
- **Responsável:** Time de Engenharia.
- **Revisão:** Anual ou mediante término do ciclo de suporte dos componentes LTS.

---

## DEC-010 / ADR-010 — Arquitetura de Autenticação e Matriz de Capacidades de Marketplaces (ML e Shopee)
- **Data:** 2026-11-17
- **Status:** `ACCEPTED`
- **Contexto:** O PRD demanda integração oficial com Mercado Livre e Shopee para sincronização de catálogo próprio, anúncios e pedidos, além de extração de taxas para cálculo de margem. A Descoberta Técnica auditou as documentações oficiais e constatou modelos de autenticação distintos.
- **Opções consideradas:**
  1. Utilizar bibliotecas terceiras não oficiais ou engenharia reversa de APIs privadas.
  2. Implementar autenticação oficial OAuth 2.0 por canal:
     - Mercado Livre: Authorization Code Grant global sem escopos de URL, tokens de 6h (refresh de 6 meses).
     - Shopee: Authorization Code Grant com assinatura criptográfica HMAC-SHA256 (`sign`) com `partner_key` em toda requisição, tokens de 4h (refresh de 30 dias).
- **Decisão:** Opção 2. Adotar estritamente os fluxos e endpoints oficiais documentados em [docs/architecture/marketplace-capability-matrix.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/architecture/marketplace-capability-matrix.md).
- **Justificativa:** Conformidade integral com as políticas das plataformas parceiras, evitando cancelamento de credenciais de desenvolvedor e bloqueios legais.
- **Consequências:** Cada adaptador de conector deve implementar seu próprio gerenciador de ciclo de vida de tokens e gerador de assinaturas (`ShopeeRequestSigner`).
- **Requisitos relacionados:** FR-INT-001, FR-INT-002, FR-INT-003, FR-INT-004, FR-INT-006.
- **Responsável:** Tech Lead de Integrações.
- **Revisão:** A cada atualização semestral das APIs de Mercado Livre e Shopee.

---

## DEC-011 / ADR-011 — Inteligência Competitiva Limitada a Dados Públicos Oficiais e Separação de Estimativas
- **Data:** 2026-11-17
- **Status:** `ACCEPTED`
- **Contexto:** Análise de concorrência e descoberta de produtos exigem dados de anúncios de terceiros. Métricas como vendas reais de concorrentes, faturamento de concorrentes ou taxa de conversão não são disponibilizadas pelas APIs oficiais para lojas de terceiros.
- **Opções consideradas:**
  1. Simular ou estimar dados de concorrentes e apresentá-los como "fatos reais".
  2. Implementar scrapers para capturar pedidos de concorrentes via contas falsas de comprador.
  3. Utilizar apenas dados públicos observáveis (ML `/sites/MLB/search`, `/trends`), tratar métricas de vendas de concorrentes exclusivamente como estimativas explicáveis com selo `ESTIMATED`, e marcar dados privados ausentes como `UNAVAILABLE`.
- **Decisão:** Opção 3. Dados de terceiros limitam-se a dados públicos oficiais ou estimativas com metodologia explícita; dados de vendas reais privadas de concorrentes são marcados como `UNAVAILABLE`.
- **Justificativa:** Cumprimento rigoroso das regras anti-alucinação, proteção jurídica contra litígios de concorrência desleal e conformidade com as diretrizes do PRD.
- **Consequências:** A interface do usuário deve educar o seller sobre a diferença entre dados próprios reais e sinais públicos de mercado.
- **Requisitos relacionados:** FR-MKT-001 a 004, FR-CMP-001 a 008, PRD Seção 6.1.
- **Responsável:** Product Manager / Jurídico.
- **Revisão:** Validação de metodologia de scoring no Prompt 10.

---

## DEC-012 / ADR-012 — Extensão Manifest V3 com Permissão activeTab e Sem Segredos
- **Data:** 2026-11-17
- **Status:** `ACCEPTED`
- **Contexto:** A extensão de navegador oferece assistência contextual ao vendedor enquanto navega nos marketplaces. As políticas do Chrome Web Store Developer Program (Limited Use Policy) proíbem coleta desnecessária de dados.
- **Opções consideradas:**
  1. Extensão com permissões amplas (`<all_urls>`), armazenando tokens OAuth no LocalStorage e raspando páginas continuamente em background.
  2. Extensão Manifest V3 com permissão restrita `activeTab` disparada apenas por ação explícita do usuário, comunicando-se com o backend da plataforma via sessão efêmera e sem armazenar segredos.
- **Decisão:** Opção 2. Adotar arquitetura de extensão com permissões mínimas, `activeTab` e zero credenciais persistidas localmente.
- **Justificativa:** Aprovação facilitada na Chrome Web Store, conformidade com a LGPD, segurança contra roubo de tokens e respeito às políticas dos marketplaces.
- **Consequências:** A extensão não captura dados em segundo plano; a análise é disparada sob demanda pelo usuário.
- **Requisitos relacionados:** FR-LST-011, PRD Seção 4.1 e 12.4.
- **Responsável:** Engenheiro Frontend / Segurança.
- **Revisão:** Prompt 11.

---

## DEC-013 / ADR-013 — Estratégia de Isolamento de Ambientes para Integrações Externas
- **Data:** 2026-11-17
- **Status:** `ACCEPTED`
- **Contexto:** Regra anti-alucinação proíbe terminantemente simular integrações em produção e utilizar credenciais reais em testes automatizados.
- **Opções consideradas:**
  1. Conectar ambiente de teste às APIs de produção de contas reais de sellers.
  2. Simular respostas em produção quando faltar credencial.
  3. Segregação rigorosa: WireMock/Testcontainers efêmeros em `dev`/`test`; sandbox oficial (`test_user` do ML e Shopee Sandbox) em homologação; APIs reais com credenciais OAuth autorizadas pelo seller em produção. Se faltar credencial em produção, conector exibe `NOT_CONFIGURED`.
- **Decisão:** Opção 3. Hermetismo total nos testes e exigência de credencial válida em produção sem dados fictícios.
- **Justificativa:** Testes reproduzíveis sem flakiness de rede e garantia de que nenhum dado falso seja exibido ao usuário em produção.
- **Consequências:** Obrigatoriedade de manter fixtures WireMock sincronizadas com a documentação oficial.
- **Requisitos relacionados:** NFR-SEC-003, NFR-REL-001, PRD Seção 13.1.
- **Responsável:** Time de QA / DevOps.
- **Revisão:** Prompt 02 e Prompt 05.

---

## DEC-014 / ADR-014 — Atualização do Runtime Backend para Java 25 LTS
- **Data:** 2026-11-17
- **Status:** `ACCEPTED`
- **Contexto:** A baseline Java 21 do DEC-009 foi escolhida na fundação e o PRD Seção 8.4 classifica Java 21 como referência não bloqueante. A plataforma solicita agora a atualização do runtime para Java 25, LTS atual em 2026.
- **Opções consideradas:**
  1. Manter Java 21 LTS, já fixado pelo DEC-009.
  2. Atualizar o runtime e o bytecode do backend para Java 25 LTS, mantendo Spring Boot 3.5.x e as dependências atuais.
- **Decisão:** Opção 2. Java 25 LTS passa a ser a baseline do backend; Maven compiler usa a propriedade `${java.version}` como fonte única do nível de release, e CI compila/testa com JDK 25.
- **Justificativa:** Adotar a LTS atual prolonga a janela de suporte do runtime e alinha desenvolvimento, bytecode e CI. A mudança é isolada ao runtime/build; não exige migração de framework ou dependências. A próxima LTS planejada (Java 29, setembro de 2027) será avaliada separadamente.
- **Consequências:** Desenvolvedores e CI precisam de JDK 25; artefatos backend passam a direcionar Java 25. DEC-009 fica substituído somente quanto à versão Java; os demais itens da stack permanecem válidos. O PRD Seção 8.4 permanece como referência não bloqueante e o registro histórico da aprovação do Prompt 02 não é reescrito.
- **Requisitos relacionados:** DEC-009, PRD Seção 8.4, compatibilidade do build backend.
- **Responsável:** Time de Engenharia.
- **Revisão:** Quando Java 25 deixar de ser LTS ou diante da próxima decisão formal de runtime.

---

## DEC-015 / ADR-015 — Motor Financeiro Determinístico em BigDecimal, Regras Parametrizadas e Memória de Cálculo Explicável
- **Data:** 2026-11-17
- **Status:** `ACCEPTED`
- **Contexto:** Endereçamento definitivo de **OQ-004** (Catálogo de componentes de margem) e **OQ-005** (Parametrização fiscal sem presunção tributária oficial). O sistema calcula margem de contribuição, preço mínimo e simulações por SKU/pedido, exigindo rastreabilidade centavo a centavo e proibição absoluta de pontos flutuantes (`float`/`double`) ou IA na fórmula.
- **Opções consideradas:**
  1. Integrar motor tributário externo ou embutir regras fiscais presumidas de ICMS/ST/DIFAL. (Inviável no MVP e viola regra anti-alucinação de apuração fiscal oficial).
  2. Implementar motor determinístico agnóstico em `BigDecimal` (`HALF_EVEN`) com regras versionadas compostas por:
     - Componentes de Dedução: Custo de Aquisição (COGS com vigência), Comissão Percentual, Tarifa Fixa (com threshold de valor), Frete/Logística Seller, Embalagem, Alíquota Tributária Parametrizada informada pelo seller, e Outros Custos Variáveis.
     - Indicador de Qualidade do Cálculo: `COMPLETE`, `PARTIAL` (com lista de campos ausentes e disclaimer de que não é lucro real) e `SIMULATED`.
     - Tratamento Contábil de Eventos: cancelamento reverte receita e taxas proporcionais; devoluções registram custos irreversíveis como prejuízo.
     - Memória de cálculo completa e reproduzível exportável linha a linha.
- **Decisão:** Opção 2. Motor determinístico puro em Java com `BigDecimal`, componentes parametrizáveis com vigência temporal e persistência imutável de cálculos fechados vinculados à versão da regra utilizada.
- **Justificativa:** Precisão matemática exata, conformidade contábil, isolamento de responsabilidade fiscal (o sistema é gerencial e não consultor tributário oficial) e total transparência para o vendedor.
- **Consequências:** Mudanças em regras tarifárias ou fiscais criam nova versão (`version + 1`) com vigência, sem recalcular silenciosamente pedidos passados já fechados.
- **Requisitos relacionados:** FR-FIN-001 a 006, FR-FIN-010, FR-FIN-011, OQ-004, OQ-005, ADR DEC-005.
- **Responsável:** Especialista Financeiro / Engenheiro Backend.
- **Revisão:** A cada novo tipo de componente financeiro adicionado.

---

## DEC-016 / ADR-016 — Painel Financeiro e Relatórios por Regime de Competência no MVP (com Metadados de Origem e Consistência de Exportação)
- **Data:** 2026-11-17
- **Status:** `ACCEPTED`
- **Contexto:** Endereçamento definitivo de **OQ-015** (Regime de Caixa vs. Regime de Competência). No MVP, os dados disponíveis dos marketplaces consistem em pedidos importados e calculados pelo motor financeiro na data do evento (`order_date`). Repasses e liquidações bancárias (escrow) dependem de conciliação aprofundada prevista para a Fase 2 (Prompt 17).
- **Opções consideradas:**
  1. Forjar regime de caixa presumindo prazos médios de D+14/D+30 sem dados reais de repasse.
  2. Implementar visão primária executiva estritamente por **regime de competência** (baseado na data do pedido e cálculos fechados do motor financeiro), exibindo cobertura, status de cálculo e metadados de geração em todas as telas e exportações CSV.
- **Decisão:** Opção 2. Painel financeiro do MVP opera 100% por regime de competência, unificando o serviço de domínio de agregação entre tela e CSV. A visão por regime de caixa fica reservada para o módulo de conciliação de repasses na Fase 2.
- **Justificativa:** Conformidade com as normas contábeis, eliminação de estimativas arbitrárias de fluxo de caixa e garantia de que os totalizadores da tela e do arquivo exportado sejam idênticos ao centavo.
- **Consequências:** Usuários visualizam faturamento e margem relativos ao período de realização das vendas, com clara sinalização da data da última sincronização.
- **Requisitos relacionados:** FR-FIN-005, FR-DAS-001, FR-DAS-002, FR-DAS-005, OQ-015, PRD Seção 5.12.
- **Responsável:** Especialista Financeiro / Engenheiro Backend.
- **Revisão:** Fase 2 (Prompt 17 - Conciliação e Regime de Caixa).

---

## DEC-017 / ADR-017 — AI Listing Studio: Provedor Agnóstico, Prompts Versionados, Proteção Anti-Hallucination e Score Explicável
- **Data:** 2026-11-17
- **Status:** `ACCEPTED`
- **Contexto:** Endereçamento definitivo de **OQ-010** (Publicação e escopo de escrita no MVP), **OQ-013** (Provedor de IA e governança de dados) e **OQ-014** (Metodologia explicável de score de qualidade de anúncio). O AI Listing Studio deve fornecer análise auditável de anúncios próprios e geração assistida de títulos/descrições sem inventar características do produto e sem publicar diretamente no marketplace.
- **Opções consideradas:**
  1. Dependência acoplada a SDK proprietário de LLM, prompts dinâmicos em código e score de qualidade em formato caixa-preta (apenas um número de 0 a 100).
  2. Arquitetura em camadas com:
     - Porta `AiProvider` agnóstica com suporte a chamadas REST estruturadas e fallback explícito (`AiServiceUnavailableException`) que nunca fabrica respostas se a API falhar.
     - Implementação de `FakeAiProvider` para testes automatizados sem rede externa e sem risco de vazamento de segredos.
     - Prompts versionados em arquivos de recursos (`prompts/*.st`), schemas JSON estritos e validação de saída.
     - Proteção contra prompt injection: sanitização e isolamento de dados de anúncios/avaliações externas em tags `<untrusted_product_context>`.
     - Factual Guardrail anti-alucinação: proibição expressa de criar medidas, certificações, garantias ou especificações ausentes, marcando lacunas em `missing_attributes`.
     - Score explicável de qualidade (0 a 100) ponderado em 4 pilares: Título (25%), Descrição (25%), Atributos/Ficha Técnica (30%) e Fotos/Imagens (20%), com detalhamento de evidências e recomendações acionáveis.
     - Histórico de versões (`listing_versions`) com rastreabilidade de ator, prompt, modelo, timestamp e status de rascunho sem alteração no marketplace externo.
- **Decisão:** Opção 2. Provedor agnóstico, prompts versionados em arquivo, score 100% explicável, proteção contra injection e persistência imutável de versões de rascunho.
- **Justificativa:** Conformidade integral com as regras anti-alucinação do PRD, segurança contra vazamentos multi-tenant e garantia de que o seller mantenha controle total sobre o conteúdo publicado.
- **Consequências:** Publicação direta no marketplace permanece bloqueada no MVP (reservada para a Fase 3 / Prompt 19 sob confirmação humana obrigatória).
- **Requisitos relacionados:** FR-LST-001, FR-LST-002, FR-LST-003, FR-LST-008, FR-LST-011, OQ-010, OQ-013, OQ-014, PRD Seção 5.3.
- **Responsável:** Time de IA e Engenharia de Backend.
- **Revisão:** Fase 2 (Prompt 15 - Processamento de Imagens e PDFs de fornecedores).

---

## DEC-018 / ADR-018 — Extensão Chrome Manifest V3: Interface Contextual Segura, Permissão activeTab sob Demanda e Sem Coleta Hostil
- **Data:** 2026-11-17
- **Status:** `ACCEPTED`
- **Contexto:** Endereçamento definitivo de **OQ-003** (Uso da extensão como interface contextual vs. fonte de captura permitida). A extensão deve conectar a experiência de navegação do vendedor no Mercado Livre ou Shopee à plataforma web da Seller Intelligence Platform, sem violar as políticas do Chrome Web Store Developer Program (Limited Use Policy) e sem contornar termos de serviço dos marketplaces.
- **Opções consideradas:**
  1. Extensão invasiva com scripts de injeção global (`<all_urls>`), monitoramento contínuo em segundo plano e raspagem oculta de páginas/preços de concorrentes ou dados de sessão.
  2. Extensão Manifest V3 com arquitetura em TypeScript/React/Vite com:
     - Permissão estritamente limitada a `activeTab` e storage local efêmero (`chrome.storage.session` / memória), sem ler cookies do marketplace e sem armazenar segredos ou senhas.
     - Ação disparada exclusivamente por iniciativa explícita do usuário ao abrir o popup ou clicar no botão de envio para a plataforma.
     - Content script mínimo focado apenas em identificar o identificador canônico público da página atual (ex.: `MLB12345678` ou `item_id` da Shopee) a partir do URL/DOM canônico aberto na aba ativa.
     - Autenticação federada com a aplicação web da plataforma (verificação de sessão ativa via handshake com o backend e abertura de link seguro).
     - Proibição absoluta de automação de cliques, compras, logins, alteração de preços ou publicação de anúncios no marketplace.
     - Tratamento seguro de erros de conexão com o backend sem vazamento de dados sensíveis ou tokens.
- **Decisão:** Opção 2. Extensão Manifest V3 leve, transparente, focada em produtividade contextual sob demanda e 100% aderente às diretrizes de permissões mínimas.
- **Justificativa:** Conformidade integral com a LGPD, aprovação sem restrições na Chrome Web Store, prevenção contra vazamento de credenciais de sellers e garantia de segurança operacional.
- **Consequências:** Nenhuma coleta oculta em background é realizada. O usuário precisa interagir com a extensão para inspecionar um produto ou enviar o item para o AI Listing Studio / Pesquisa de Mercado na plataforma web.
- **Requisitos relacionados:** FR-LST-011, NFR-SEC-004, NFR-PRV-001, OQ-003, ADR DEC-012, PRD Seção 5.14, 8.2 e 12.4.
- **Responsável:** Engenheiro Frontend / Segurança.
- **Revisão:** Fase 2 (Prompt 13 - Monitoramento sob Demanda).

---

## DEC-019 / ADR-019 — Monitoramento Temporal de Concorrentes: Watchlists Imutáveis, Deduplicação de Mudanças, Lacunas Explícitas sem Interpolação e Retenção de 12 Meses
- **Data:** 2026-11-17
- **Status:** `ACCEPTED`
- **Contexto:** Necessidade de permitir que o vendedor monitore anúncios, produtos e concorrentes ao longo do tempo (FR-CMP-002 a FR-CMP-008 e FR-MON-001 a FR-MON-005). A integridade do histórico depende de regras estritas: fontes autorizadas, ausência de interpolação forjada em falhas de coleta, deduplicação de alertas e preservação do histórico mesmo após desativação de itens da watchlist.
- **Opções consideradas:**
  1. Raspagem não autorizada com interpolação linear contínua (fabricando pontos de preço ausentes para desenhar gráficos contínuos) e deleção em cascata de histórico.
  2. Arquitetura determinística, transparente e em conformidade estrita:
     - **Watchlists com Preservação de Histórico:** Itens podem ser adicionados, pausados ou desativados (`status: UNTRACKED`). Snapshots anteriores são imutáveis e protegidos contra deleção acidental (`ON DELETE RESTRICT`).
     - **Snapshots com Proveniência Auditável:** Cada coleta armazena timestamp original, preço em `NUMERIC(15, 2)`, status do anúncio (`ACTIVE`, `PAUSED`, `OUT_OF_STOCK`, etc.), reputação do seller e rótulo explícito `OBSERVED`. Vendas privadas ou estoque não fornecido são gravados como `UNAVAILABLE` e nunca transformados em zero ou afirmados sem comprovação oficial.
     - **Diff Engine com Deduplicação Determinística:** Compara snapshots adjacentes calculando variação absoluta e percentual de preço em `BigDecimal`. Cada evento gera um hash SHA-256 (`tenant_id + item_id + event_type + previous + current + date_bucket`) com constraint `uk_event_hash` impedindo duplicações.
     - **Série Temporal com Sinalização Explícita de Lacunas (Gaps):** Quando o intervalo entre coletas sucessivas ultrapassar $1,5 \times \text{frequência}$ ou houver falha de coleta (`COLLECTION_FAILED`), o serviço emite um ponto de dados explícito com status `GAP`. É **terminantemente proibido interpolar preços silenciosamente**. A interface exibe a taxa de cobertura real e os períodos sem dados.
     - **Alertas com Cooldown Configurável:** Limiares de alerta (ex: variação de preço $> 5\%$) avaliados com deduplicação por chave única de cooldown (`uk_monitoring_alert_dedup`) para evitar disparos repetitivos em sequência.
     - **Retenção e Particionamento:** Snapshots mantidos por 12 meses conforme proposta DEC-P05, indexados por `(tenant_id, item_id, collected_at)` para viabilizar consultas temporais rápidas e expurgo automatizado de dados obsoletos.
- **Decisão:** Opção 2.
- **Justificativa:** Atendimento integral às regras anti-alucinação do PRD, rigor financeiro determinístico, prevenção contra poluição de dados e conformidade técnica.
- **Consequências:** Gráficos de monitoramento terão descontinuidades visíveis quando houver falhas de coleta ou interrupções de serviço, o que confere credibilidade absoluta aos dados apresentados ao seller.
- **Requisitos relacionados:** FR-CMP-002 a FR-CMP-008, FR-MON-001 a FR-MON-005, NFR-SEC-002, NFR-PER-001, OQ-002, OQ-007, PRD Seção 5.5 e 5.10.
- **Responsável:** Engenheiro Backend / Arquiteto de Software.
- **Revisão:** Fase 2 (Prompt 14 - Smart Alerts e Action Center).

---

# Parte 2: Decisões Propostas e Pendências Classificadas (PROPOSED)

## DEC-P05 — Política de Retenção de Dados e Expurgos Automáticos LGPD (Vinculada a OQ-007)
- **Status:** `PROPOSED` / `PENDING_REVIEW` (Classificação: **P0**)
- **Contexto:** Estabelecer tempos de guarda para logs de auditoria, snapshots de mercado e dados anonimizados de compradores.
- **Proposta Inicial da Engenharia:**
  - Snapshots de mercado: Retenção de 12 meses (dados históricos de preço e tendências).
  - Logs de auditoria: Retenção de 5 anos (atendimento a exigências fiscais/legais).
  - Dados pessoais de compradores (nome, endereço de entrega): Anonimização / expurgo após 90 dias da entrega concluída do pedido, preservando apenas os valores financeiros.
- **Pendência:** Validação com o Encarregado de Proteção de Dados (DPO) / Jurídico.

## DEC-P06 — Metodologia Explicável do Score de Anúncio e Oportunidade (Vinculada a OQ-014)
- **Status:** `PROPOSED` / `PENDING_REVIEW` (Classificação: **P0**)
- **Contexto:** Definir os pesos e critérios que compõem o score de qualidade de anúncio (FR-LST-001) para que seja 100% auditável e explicável.
- **Proposta Inicial da Engenharia:** Score de 0 a 100 ponderado por: Comprimento do título e palavras-chave (25%), Completude da ficha técnica e atributos (30%), Qualidade e quantidade de fotos (25%), Preço vs média de mercado (20%).
- **Pendência:** Validação de produto no Prompt 10.
