# Especificação Técnica: Suíte E2E Playwright Desktop/Mobile, Acessibilidade WCAG e Regressão Visual

> **Prompt ID:** P023-FE  
> **Epic:** E12 - Testes End-to-End, Resiliência e Homologação Frontend  
> **Status:** `IN_PROGRESS`  
> **Data:** 23/11/2026  
> **Autor:** Engenharia de Qualidade e Frontend  

---

## 1. Visão Geral e Objetivos

O objetivo deste gate é implementar e executar uma suíte de testes de ponta a ponta com **Playwright** cobrindo os fluxos principais da **Seller Intelligence Platform** em múltiplos breakpoints (Desktop e Mobile), com auditoria automatizada de **Acessibilidade WCAG 2.1 AA** (via `@axe-core/playwright`), monitoramento contínuo de **erros de console críticos** e validação comportamental profunda (sem aprovação baseada exclusivamente em snapshots estáticos).

### Critérios de Aceite Oficiais:
1. **Fluxos E2E passam em breakpoints:**
   - Desktop padrão ($1280 \times 800$ px).
   - Mobile responsivo ($375 \times 667$ px e $360 \times 740$ px).
   - Zero overflow horizontal (`scrollWidth <= clientWidth`).
   - Comportamento de gaveta mobile (drawer hambúrguer), bottom navigation dock e sidebar desktop testados e funcionais.
2. **Sem erro de console crítico:**
   - Monitoramento contínuo via `page.on('console')` e `page.on('pageerror')`.
   - Nenhuma exceção JavaScript não tratada (`TypeError`, `Unhandled promise rejection`, `SyntaxError`, `Invariant Violation`).
3. **Acessibilidade Essencial Aprovada:**
   - Auditoria via `@axe-core/playwright` (`AxeBuilder`) nas páginas e módulos principais com tags `wcag2a`, `wcag2aa`, `wcag21aa`.
   - Zero violações com impacto `critical` ou `serious`.
4. **Validação de Comportamento e Dados (Anti-Alucinação):**
   - Não aprovar por screenshot isolado.
   - Conferência de dados numéricos nos cartões e formulários (ex: R$ 38.450,00 de faturamento, margem de contribuição de R$ 11.150,00, R$ 200,00 de preço simulado).
   - Presença inamovível do aviso contábil anti-alucinação ("Margem de Contribuição NÃO é Lucro Líquido").

---

## 2. Arquitetura da Suíte Playwright

### 2.1 Configuração (`playwright.config.ts`)
- **Projetos:**
  - `desktop-chromium`: Viewport $1280 \times 800$, desktop user-agent.
  - `mobile-chromium`: Viewport $375 \times 667$, mobile touch emulation, mobile user-agent.
- **Web Server:**
  - Inicialização automática do servidor Next.js em `http://localhost:3000` via comando de produção (`npm run start`) ou dev (`npm run dev`).
  - Timeout generoso para inicialização e execução isolada.
- **Reporters:**
  - `list` no terminal para visibilidade imediata.
  - `html` reporter persistido para auditoria de evidências.

### 2.2 Estrutura de Arquivos de Teste
```text
frontend/
├── e2e/
│   ├── helpers/
│   │   ├── console-monitor.ts      # Monitor de console e erros de runtime
│   │   ├── mock-routes.ts          # Mocking determinístico de APIs para isolamento
│   │   └── a11y-helper.ts          # Wrapper do AxeBuilder para análise WCAG 2.1 AA
│   ├── 01-auth-lifecycle.spec.ts   # Fluxo de login, validação, credenciais e sessão expirada
│   ├── 02-executive-dashboard.spec.ts # Dashboard executivo, KPIs, filtros, catálogo de fórmulas
│   ├── 03-catalog-and-orders.spec.ts  # Catálogo canônico e pedidos com fatos contábeis
│   ├── 04-pricing-simulator.spec.ts   # Simulador determinístico de margem e precificação
│   ├── 05-reports-and-exports.spec.ts # Central de relatórios, modal e jobs assíncronos
│   ├── 06-ai-and-studio.spec.ts       # Assistente de IA e editor de rascunhos de anúncios
│   └── 07-mobile-and-responsive.spec.ts # Responsividade em 360px, drawer, dock e zero overflow
├── playwright.config.ts
└── package.json
```

---

## 3. Matriz de Cobertura dos Fluxos Principais

| Fluxo | Rota / Módulo | Dispositivos | Validações Comportamentais | Validação de A11y |
| :--- | :--- | :--- | :--- | :--- |
| **Autenticação** | `/login` | Desktop & Mobile | Validação de campos vazios, aviso de sessão expirada, submissão de credenciais válidas, redirecionamento para o shell | `wcag2a`, `wcag2aa`, `wcag21aa` (sem violações críticas) |
| **Dashboard Executivo** | `/` (`EXECUTIVE_DASHBOARD`) | Desktop & Mobile | Renderização dos 6 KPIs principais, alternância de granularidade temporal, quebra por canais, abertura da gaveta de catálogo de fórmulas | Labels semânticos, contraste de cores, ARIA landmarks |
| **Catálogo & Pedidos** | `/` (`SYNC` e `ORDERS`) | Desktop & Mobile | Filtros de busca, detalhamento de status de sincronização, conferência de itens do pedido e apuração do fato contábil | Tabela acessível, headers semânticos |
| **Simulador de Preço** | `/` (`FINANCIAL`) | Desktop & Mobile | Input de preço (`R$ 200,00`), cálculo de margem e markup determinísticos, visualização de ponto de equilíbrio | Inputs com rótulos `label`, acessibilidade de formulário |
| **Relatórios & Exportação** | `/` (`REPORTS`) | Desktop & Mobile | Abertura do modal, seleção de colunas e formato (CSV/XLSX), visualização de card de status e download auditado | Diálogo modal com foco gerenciado, `role="dialog"` |
| **Assistente & Copywriting** | `/` (`ASSISTANT`, `AI_STUDIO`) | Desktop & Mobile | Envio de prompt no chat, renderização de badges de fonte e proveniência, editor de rascunhos sem botão de publicação direta | Textareas rotulados, botões com aria-label |
| **Layout & Responsividade** | `/` (Todos os módulos) | 360px, 375px, 768px, 1280px | Zero overflow horizontal (`scrollWidth <= clientWidth`), drawer hambúrguer abre e fecha com Escape, dock inferior navegável | Touch targets $\ge 44$ px, contraste $\ge 4.5:1$ |

---

## 4. Regras Anti-Alucinação e Garantias de Qualidade

1. **Anti-Hallucination Disclaimer:** Todos os testes que interagem com margens conferem explicitamente a presença do aviso de que "Margem de Contribuição NÃO é Lucro Líquido".
2. **Sem Falsos Positivos por Screenshots:** Cada asserção visual é acompanhada de asserções estritas no DOM (`toBeVisible`, `toHaveText`, `toBeGreaterThan`, `toHaveAttribute`).
3. **Auditoria de Console:** Nenhum teste passa se disparar erro de console com severidade `error` ou exceção de unhandled rejection.
