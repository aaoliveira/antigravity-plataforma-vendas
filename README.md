# Pacote de Engenharia — Plataforma de Inteligência para E-commerce

Plataforma SaaS multi-tenant de inteligência de mercado, analytics e gestão para e-commerce (Mercado Livre, Shopee e expansões).  
Construída com arquitetura de **Monólito Modular**, isolamento multi-tenant estrito (Application Context + PostgreSQL RLS) e motor financeiro determinístico em `BigDecimal`.

---

## 1. Documentos de Governança e Engenharia (`docs/`)

| Documento | Descrição / Finalidade |
|---|---|
| [docs/PROJECT_CONTEXT.md](docs/PROJECT_CONTEXT.md) | Visão de produto, contexto de mercado, personas e valor de negócio. |
| [docs/PRD.md](docs/PRD.md) | Product Requirements Document — Fonte única da verdade de produto. |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Arquitetura em Monólito Modular, boundaries de domínio e diagramas. |
| [docs/TECH_STACK.md](docs/TECH_STACK.md) | Matriz oficial de tecnologias, bibliotecas e versões homologadas. |
| [docs/ENGINEERING_RULES.md](docs/ENGINEERING_RULES.md) | As 15 regras invioláveis, branch strategy, commits e DoD/DoR. |
| [docs/SECURITY.md](docs/SECURITY.md) | Diretrizes OWASP ASVS Nível 2, isolamento multi-tenant e RLS. |
| [docs/MARKETPLACE_POLICIES.md](docs/MARKETPLACE_POLICIES.md) | Matriz de capacidades por marketplace e proteção de contas. |
| [docs/BACKLOG.md](docs/BACKLOG.md) | Backlog priorizado por épicos (P0 a P3) e critérios de aceite. |
| [docs/DECISION_LOG.md](docs/DECISION_LOG.md) | Architecture Decision Records (ADRs) homologados. |
| [docs/OPEN_QUESTIONS.md](docs/OPEN_QUESTIONS.md) | Rastreamento de dúvidas que exigem confirmação humana/credenciais. |
| [docs/IMPLEMENTATION_STATUS.md](docs/IMPLEMENTATION_STATUS.md) | Rastreamento do estado de cada prompt e quality gates. |
| [docs/TEST_STRATEGY.md](docs/TEST_STRATEGY.md) | Pirâmide de testes, regressão financeira e integridade de dados. |
| [docs/PROMPT_EXECUTION_LOG.md](docs/PROMPT_EXECUTION_LOG.md) | Trilha formal de auditoria e relatórios de execução de prompts. |
| [docs/runbooks/](docs/runbooks/) | Runbooks operacionais de setup, testes e disaster recovery. |

---

## 2. Ordem de Leitura e Execução dos Prompts

1. `PRD.md` &middot; Fonte da verdade de produto.
2. `BACKLOG_MVP.md` &middot; Épicos e histórias.
3. `PROMPT_000_GOVERNANCE.md` &middot; **Status: `PASSED`**. Governança configurada.
4. `PROMPTS_MVP.md` &middot; Executar **um prompt por vez**, respeitando a aprovação prévia do gate anterior.
5. `PROMPTS_EVOLUCAO.md` &middot; Executar somente após homologação formal do MVP.

---

## 3. Status Atual do Projeto
* **Prompt Vigente:** PROMPT 000 — Master Project Governance & Engineering Rules $\rightarrow$ **`APPROVED / PASSED`**
* **Próximo Prompt Liberado:** Prompt P001 — Estrutura Base do Repositório e Backend Modular $\rightarrow$ **`READY`**
