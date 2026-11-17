# Guia de Desenvolvimento — Seller Intelligence Platform

## 1. Visão Geral e Filosofia de Engenharia
A **Seller Intelligence Platform** segue um processo de engenharia orientado a evidências, rigor arquitetural e gating sequencial. O desenvolvimento é dividido em 20 prompts numerados (`00` a `19`), organizados em 4 fases:
- **Fase 0 — Fundação & Descoberta Técnica** (Prompts 00–01): Constituição, regras persistentes, viabilidade técnica, compliance e ADRs.
- **Fase 1 — MVP** (Prompts 02–12): Bootstrap do monorepo, identidade/tenancy, conectores Mercado Livre/Shopee, catálogo, pedidos, pesquisa de mercado, motor financeiro, AI Listing Studio, extensão Chrome MV3 e homologação E2E.
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

> [!NOTE]
> Como o projeto encontra-se atualmente na **Fase 0 (Prompt 00 — Governança e Constituição)**, a estrutura de código de negócio e os arquivos executáveis serão criados no **Prompt 02 (Bootstrap do Monorepo)**.
> Abaixo, cada comando está categorizado explicitamente entre **`[DISPONÍVEL AGORA]`** e **`[PLANEJADO - DISPONÍVEL NO PROMPT 02+]`**.

### 3.1 Verificação de Governança e Integridade (Fase 0)

| Comando | Finalidade | Estado Atual |
|---|---|---|
| `git status` | Inspeciona estado do repositório local. | `[DISPONÍVEL AGORA]` |
| `jq . docs/delivery/phase-gate.json` | Valida sintaxe e lê o status do gate atual da entrega. | `[DISPONÍVEL AGORA]` |
| `diff -u prompts/PRD.md docs/product/PRD.md` | Verifica integridade e paridade absoluta do PRD. | `[DISPONÍVEL AGORA]` |

### 3.2 Ambiente Local e Infraestrutura (Docker Compose)

| Comando | Finalidade | Estado Atual |
|---|---|---|
| `docker compose up -d` | Sobe PostgreSQL 18, Redis, RabbitMQ 4, Keycloak e MinIO. | `[PLANEJADO - DISPONÍVEL NO PROMPT 02+]` |
| `docker compose ps` | Inspeciona a saúde dos contêineres locais. | `[PLANEJADO - DISPONÍVEL NO PROMPT 02+]` |
| `docker compose down -v` | Derruba serviços e remove volumes efêmeros. | `[PLANEJADO - DISPONÍVEL NO PROMPT 02+]` |

### 3.3 Backend (Java 21 / Spring Boot 3.5.x / Maven)

| Comando | Finalidade | Estado Atual |
|---|---|---|
| `./mvnw clean compile` | Compila o monólito backend e valida bytecode Java 21. | `[PLANEJADO - DISPONÍVEL NO PROMPT 02+]` |
| `./mvnw test` | Executa testes unitários (JUnit 5, AssertJ, Mockito). | `[PLANEJADO - DISPONÍVEL NO PROMPT 02+]` |
| `./mvnw verify` | Executa testes de integração (Testcontainers) e arquitetura (ArchUnit). | `[PLANEJADO - DISPONÍVEL NO PROMPT 02+]` |
| `./mvnw flyway:migrate` | Executa migrações Flyway no banco PostgreSQL configurado. | `[PLANEJADO - DISPONÍVEL NO PROMPT 02+]` |
| `./mvnw spring-boot:run` | Inicia o servidor backend localmente (porta 8080). | `[PLANEJADO - DISPONÍVEL NO PROMPT 02+]` |

### 3.4 Frontend Web (Next.js / TypeScript / React)

| Comando | Finalidade | Estado Atual |
|---|---|---|
| `npm install` (na raiz do frontend) | Instala dependências do Next.js e design system shadcn/ui. | `[PLANEJADO - DISPONÍVEL NO PROMPT 02+]` |
| `npm run dev` | Inicia o servidor de desenvolvimento do Next.js (porta 3000). | `[PLANEJADO - DISPONÍVEL NO PROMPT 02+]` |
| `npm run lint` | Executa ESLint com regras estritas de TypeScript. | `[PLANEJADO - DISPONÍVEL NO PROMPT 02+]` |
| `npm run test` | Executa testes de componentes via Vitest e Testing Library. | `[PLANEJADO - DISPONÍVEL NO PROMPT 02+]` |
| `npm run build` | Compila a aplicação Next.js para produção. | `[PLANEJADO - DISPONÍVEL NO PROMPT 02+]` |

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

## 4. Estrutura de Diretórios Planejada do Monorepo

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
├── AGENTS.md
├── README.md
├── docs/
│   ├── README_DEVELOPMENT.md
│   ├── architecture/
│   │   └── DECISION_LOG.md
│   ├── delivery/
│   │   ├── PHASE_STATUS.md
│   │   └── phase-gate.json
│   └── product/
│       ├── OPEN_QUESTIONS.md
│       ├── PRD.md
│       └── REQUIREMENTS_TRACEABILITY.md
├── backend/                       [A criar no Prompt 02]
│   ├── pom.xml
│   └── src/
│       ├── main/java/...
│       └── test/java/...
├── frontend/                      [A criar no Prompt 02]
│   ├── package.json
│   ├── tsconfig.json
│   └── src/app/...
├── extension/                     [A criar no Prompt 11]
│   ├── manifest.json
│   └── src/...
├── docker-compose.yml             [A criar no Prompt 02]
└── prompts/                       [Prompts de especificação do produto]
```

---

## 5. Boas Práticas e Higiene de Segurança
- **Nunca comite arquivos `.env` com senhas reais.** Utilize exclusivamente `.env.example` para referências.
- **Não commite tokens ou credenciais de marketplaces.** Testes com conectores devem utilizar mocks (`WireMock` ou servidores de teste efêmeros) ou segredos injetados por variáveis de ambiente.
- **Validação de Código:** Todo commit deve respeitar o gating sequencial. O avanço de prompt requer atualização dos artefatos em `docs/delivery/`.
