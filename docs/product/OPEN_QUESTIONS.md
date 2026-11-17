# Questões em Aberto (Open Questions) — Seller Intelligence Platform

## Diretriz de Governança
Este documento reúne as questões em aberto identificadas no PRD oficial ([docs/product/PRD.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/product/PRD.md), Seção 20) que bloqueiam decisões arquiteturais, de escopo, de integrações ou fiscais.
Em estrita conformidade com as regras anti-alucinação ([.agents/rules/anti-hallucination.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/.agents/rules/anti-hallucination.md)), **nenhuma questão pode ser respondida por suposição ou dedução**. Resoluções só podem ser registradas após decisão humana formal, evidência documental oficial ou deliberação técnica/jurídica aprovada.

---

## Tabela Consolidada de Questões em Aberto (PRD Seção 20)

| ID | Questão | Por que importa | Responsável Sugerido | Estado | Impacto no Projeto |
|---|---|---|---|---|---|
| **OQ-001** | Quais endpoints, escopos e permissões estarão disponíveis para cada marketplace e região? | Define o que pode ser sincronizado e exibido sem violação de termos de uso ou falhas de autorização. | Tech Lead / Integrações | `OPEN` | Bloqueia escopo final dos conectores ML/Shopee (Prompts 01 e 05). |
| **OQ-002** | Quais dados competitivos podem ser obtidos legitimamente e com que frequência? | Define a viabilidade, cobertura e custo da inteligência de mercado sem scraping hostil. | Produto / Jurídico / Integrações | `OPEN` | Condiciona arquitetura de Market Intelligence (Prompt 07). |
| **OQ-003** | A extensão será usada apenas como interface contextual ou também como fonte de captura permitida? | Impacta políticas da Chrome Web Store, privacidade do usuário e arquitetura do backend. | Produto / Jurídico | `OPEN` | Define arquitetura da extensão Manifest V3 (Prompt 11). |
| **OQ-004** | Qual é o conjunto exato de componentes da margem por marketplace/modalidade? | Evita resultados financeiros incorretos e processos decisórios errôneos do seller. | Produto / Financeiro | `OPEN` | Define motor financeiro e fórmulas determinísticas (Prompt 08). |
| **OQ-005** | Como serão tratados regimes tributários e particularidades por UF/categoria? | Afeta parametrização fiscal e complexidade de cálculo de impostos variáveis. | Financeiro / Especialista Fiscal | `OPEN` | Define modelo de impostos configuráveis (Prompt 08). |
| **OQ-006** | Quais provedores/fontes de Ads oferecem granularidade por campanha, SKU e período? | Define métricas disponíveis (ROAS, ACOS, TACOS) e modelo de atribuição. | Integrações / Produto | `OPEN` | Bloqueia módulo de Ads Intelligence (Prompt 16 - Fase 2). |
| **OQ-007** | Qual política de retenção para snapshots, pedidos, dados pessoais e arquivos? | Impacta conformidade com LGPD, dimensionamento de storage e custos operacionais. | Segurança / Privacidade | `OPEN` | Define ciclo de vida dos dados e expurgos automáticos (Prompt 01). |
| **OQ-008** | Qual volume inicial esperado de organizações, SKUs, pedidos e snapshots por dia? | Base indispensável para dimensionamento de banco de dados, filas e infraestrutura. | Produto / Engenharia | `OPEN` | Impacta estratégia de particionamento e índices (Prompt 02). |
| **OQ-009** | Quais idiomas/moedas/regiões serão suportados no lançamento? | Impacta normalização de dados, tipos monetários e experiência do usuário (UX). | Produto | `OPEN` | Valida se BRL/Português é exclusivo no MVP (Prompt 02). |
| **OQ-010** | A publicação de anúncios entra no MVP ou somente na Fase 2? | Define o escopo de operações externas mutáveis e o risco operacional correspondente. | Produto | `OPEN` | Determina se Prompt 10 inclui envio à API ou apenas rascunho local. |
| **OQ-011** | Quais canais de notificação serão suportados primeiro? | Impacta arquitetura de mensageria, preferências de usuários e custos de provedores. | Produto / Engenharia | `OPEN` | Define escopo de notificações nos Prompts 12 e 14. |
| **OQ-012** | MCP será lançado como servidor remoto multi-tenant, integração interna ou ambos? | Define arquitetura de autenticação, distribuição e segurança das ferramentas de IA. | Arquitetura / Produto | `OPEN` | Condiciona desenho do módulo MCP na Fase 3 (Prompt 18). |
| **OQ-013** | Quais provedores de IA serão aprovados e quais dados podem ser enviados a cada provedor? | Impacta privacidade (LGPD), confidencialidade comercial, custo e governança de dados. | Segurança / IA / Produto | `OPEN` | Define configuração do Spring AI e provedores aceitos (Prompt 10). |
| **OQ-014** | Qual será a metodologia inicial do score de anúncio e score de oportunidade? | Garante explicabilidade e evita acusações de causalidade ou falsas promessas de vendas. | Produto / Data | `OPEN` | Define algoritmo de scoring explicável (Prompt 10). |
| **OQ-015** | O dashboard financeiro priorizará regime de caixa, competência ou oferecerá ambos? | Evita ambiguidade contábil nos relatórios gerenciais de DRE e conciliação. | Financeiro / Produto | `OPEN` | Define consultas analíticas do dashboard financeiro (Prompt 09). |

---

## Procedimento para Resolução de Questões
Quando uma questão for deliberada:
1. Deve ser emitido ou atualizado um ADR correspondente em `docs/architecture/DECISION_LOG.md`.
2. O estado no quadro acima mudará de `OPEN` para `RESOLVED` (ou `DEFERRED` se postergado para fases futuras), acompanhado da data e link para a decisão.
3. Nenhuma alteração de código ou schema pode assumir uma resolução que permaneça em estado `OPEN`.
