# Guia de Desenvolvimento — Seller Intelligence Platform

## 1. Visão Geral e Filosofia de Engenharia
A **Seller Intelligence Platform** segue um processo de engenharia orientado a evidências, rigor arquitetural e gating sequencial. O desenvolvimento é dividido em 20 prompts numerados (`00` a `19`), organizados em 4 fases:
- **Fase 0 — Fundação & Descoberta Técnica** (Prompts 00–01): `CONCLUÍDA E APROVADA`. Constituição, regras persistentes, viabilidade técnica, compliance e ADRs.
- **Fase 1 — MVP** (Prompts 02–12): `EM ANDAMENTO`. Bootstrap do monorepo, identidade/tenancy, conectores Mercado Livre/Shopee, catálogo, pedidos, pesquisa de mercado, motor financeiro, AI Listing Studio, extensão Chrome MV3 e homologação E2E.
- **Fase 2 — Inteligência e Automação Avançadas** (Prompts 13–17): Monitoramento de produtos/concorrentes, alertas, action center, leitura de catálogos PDF/imagens, Ads Intelligence e conciliação/DRE.
- **Fase 3 — Plataforma Inteligente Ampliada** (Prompts 18–19): Assistente conversacional, servidor MCP read-only e expansão de conectores (Amazon e outros).

---

## 2. Protocolo de Trabalho por Prompt
Toda sessão de desenvolvimento deve seguir o ciclo definido na skill [.agents/skills/mvp-phase-gate/SKILL.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/.agents/skills/mvp-phase-gate/SKILL.md):
1. **Preflight:** Validar que o prompt anterior está marcado como `APPROVED` em [docs/delivery/phase-gate.json](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/delivery/phase-gate.json). Ler PRD e regras em `.agents/rules/`.
2. **Planejamento:** Registrar escopo, arquivos afetados, testes previstos e catalogar dúvidas em [docs/product/OPEN_QUESTIONS.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/product/OPEN_QUESTIONS.md).
3. **Execução:** Desenvolver código e testes em paralelo. Atualizar status para `IN_PROGRESS`.
4. **Verificação:** Executar testes reais, registrar comandos e saídas de terminal.
5. **Gate:** Se todos os critérios forem comprovados, atualizar para `APPROVED`. Se houver bloqueio não resolvido, marcar `BLOCKED` e parar.

---

## 3. Catálogo de Comandos do Projeto

### 3.1 Verificação de Governança e Integridade

| Comando | Finalidade | Estado Atual |
|---|---|---|
| `git status` | Inspeciona estado do repositório local. | `[DISPONÍVEL AGORA]` |
| `jq . docs/delivery/phase-gate.json` | Valida sintaxe e lê o status do gate atual da entrega. | `[DISPONÍVEL AGORA]` |
| `diff -u prompts/PRD.md docs/product/PRD.md` | Verifica integridade e paridade absoluta do PRD. | `[DISPONÍVEL AGORA]` |
| `python3 scripts/verify_phase01.py` | Executa testes automatizados de governança da Fase 0. | `[DISPONÍVEL AGORA]` |
| `python3 scripts/verify_phase02.py` | Executa testes automatizados do bootstrap da Fase 1. | `[DISPONÍVEL AGORA]` |

### 3.2 Ambiente Local e Infraestrutura (Docker Compose)

| Comando | Finalidade | Estado Atual |
|---|---|---|
| `docker compose up -d` | Sobe PostgreSQL 17/18, Redis 7, RabbitMQ 3.13/4 e Keycloak 26. | `[DISPONÍVEL AGORA]` |
| `docker compose ps` | Inspeciona a saúde dos contêineres locais. | `[DISPONÍVEL AGORA]` |
| `docker compose config` | Valida sintaxe e interpolação de variáveis do Compose. | `[DISPONÍVEL AGORA]` |
| `docker compose down` | Derruba serviços preservando volumes. | `[DISPONÍVEL AGORA]` |
| `docker compose down -v` | Derruba serviços e remove volumes efêmeros. | `[DISPONÍVEL AGORA]` |

### 3.3 Backend (Java 21 / Spring Boot 3.5.x / Maven)

| Comando | Finalidade | Estado Atual |
|---|---|---|
| `cd backend && ./mvnw clean compile` | Compila o monólito backend e valida bytecode Java 21. | `[DISPONÍVEL AGORA]` |
| `cd backend && ./mvnw test` | Executa testes unitários (JUnit 5, AssertJ, Mockito). | `[DISPONÍVEL AGORA]` |
| `cd backend && ./mvnw clean verify` | Executa testes de integração e arquitetura (ArchUnit). | `[DISPONÍVEL AGORA]` |
| `cd backend && ./mvnw spring-boot:run` | Inicia o servidor backend localmente (porta 8082). | `[DISPONÍVEL AGORA]` |
| `curl -s http://localhost:8082/actuator/health` | Verifica a saúde do backend em execução. | `[DISPONÍVEL AGORA]` |

### 3.4 Frontend Web (Next.js 15 / React 19 / TypeScript / pnpm)

| Comando | Finalidade | Estado Atual |
|---|---|---|
| `cd frontend && pnpm install` | Instala dependências do Next.js e design system. | `[DISPONÍVEL AGORA]` |
| `cd frontend && pnpm run dev` | Inicia o servidor de desenvolvimento do Next.js (porta 3000). | `[DISPONÍVEL AGORA]` |
| `cd frontend && pnpm run lint` | Executa ESLint com regras estritas de TypeScript. | `[DISPONÍVEL AGORA]` |
| `cd frontend && pnpm run test` | Executa testes de componentes via Vitest e Testing Library. | `[DISPONÍVEL AGORA]` |
| `cd frontend && pnpm run build` | Compila a aplicação Next.js para produção. | `[DISPONÍVEL AGORA]` |

### 3.5 Extensão Chrome (Manifest V3 / Vite / TypeScript)

| Comando | Finalidade | Estado Atual |
|---|---|---|
| `npm run build:extension` | Compila os bundles da extensão MV3 na pasta `dist/`. | `[PLANEJADO - DISPONÍVEL NO PROMPT 11+]` |
| `npm run test:extension` | Executa testes unitários dos scripts da extensão. | `[PLANEJADO - DISPONÍVEL NO PROMPT 11+]` |

### 3.6 Testes End-to-End e Segurança (Playwright)

| Comando | Finalidade | Estado Atual |
|---|---|---|
| `npx playwright test` | Executa os fluxos E2E headless de todas as jornadas P0. | `[PLANEJADO - DISPONÍVEL NO PROMPT 12+]` |

---

## 4. Estrutura de Diretórios Atualizada do Monorepo

```
antigravity-avantepro-clone/
├── .agents/
│   ├── rules/
│   │   ├── anti-hallucination.md
│   │   ├── architecture-and-stack.md
│   │   ├── phase-gates.md
│   │   └── testing-and-quality.md
│   └── skills/
│       └── mvp-phase-gate/
│           └── SKILL.md
├── .github/
│   └── workflows/
│       └── ci.yml
├── AGENTS.md
├── README.md
├── docker-compose.yml
├── .env.example
├── .gitignore
├── backend/
│   ├── pom.xml
│   ├── mvnw
│   ├── .mvn/wrapper/
│   └── src/
│       ├── main/java/br/com/sellerintelligence/
│       │   ├── SellerIntelligenceApplication.java
│       │   └── infrastructure/
│       │       ├── config/ (CorsConfig.java)
│       │       ├── error/ (GlobalExceptionHandler.java, BusinessException.java)
│       │       └── logging/ (CorrelationIdFilter.java)
│       ├── main/resources/
│       │   ├── application.yml
│       │   ├── application-dev.yml
│       │   ├── application-test.yml
│       │   └── db/migration/V1__initial_infrastructure.sql
│       └── test/java/br/com/sellerintelligence/
│           ├── SellerIntelligenceApplicationTests.java
│           └── architecture/ArchitectureTests.java
├── frontend/
│   ├── package.json
│   ├── pnpm-lock.yaml
│   ├── tsconfig.json
│   ├── next.config.mjs
│   ├── tailwind.config.mjs
│   ├── postcss.config.mjs
│   ├── vitest.config.ts
│   ├── .eslintrc.json
│   └── src/
│       ├── app/ (layout.tsx, page.tsx, globals.css)
│       ├── components/ (HealthChecker.tsx, HealthChecker.test.tsx)
│       └── lib/ (api.ts)
├── docs/
│   ├── README_DEVELOPMENT.md
│   ├── architecture/
│   │   ├── DECISION_LOG.md
│   │   └── marketplace-capability-matrix.md
│   ├── delivery/
│   │   ├── PHASE_STATUS.md
│   │   └── phase-gate.json
│   └── product/
│       ├── OPEN_QUESTIONS.md
│       ├── PRD.md
│       └── REQUIREMENTS_TRACEABILITY.md
└── scripts/
    ├── verify_phase01.py
    └── verify_phase02.py
```
