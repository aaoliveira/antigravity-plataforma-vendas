# Especificação Técnica: P001-FE — Shell da Aplicação Web Responsiva

> **Identificador da Feature:** `P001-FE` &middot; **Versão:** 1.0.0  
> **Status da Spec:** Aprovada para Implementação  
> **Requisitos do PRD:** MVP-01, RF-050, RF-051, RF-052, RNF-009, RNF-010  
> **Stack:** Next.js 15 (App Router), React 19, TypeScript 5, Tailwind CSS, Vitest  

---

## 1. Contexto e Objetivo

Construir o shell da aplicação web da **Seller Intelligence Platform** em Next.js 15, React 19 e TypeScript, com arquitetura responsiva (mobile-first), tokens de design padronizados, light mode padrão com dark mode opcional persistido, navegação por teclado (WCAG 2.2 AA), componentes de estado vazio (*empty states*), carregamento (*skeletons*), tratamento global de erros e ausência estrita de overflow horizontal em qualquer resolução (360px a ultrawide).

---

## 2. Escopo Obrigatório

1. **Configuração do Projeto Frontend (`frontend/`):**
   - `package.json` com dependências estáveis e scripts de build, test e lint.
   - `tsconfig.json` com TypeScript strict mode (`"strict": true`).
   - `tailwind.config.ts` com tokens semânticos de cores, tipografia e modo escuro configurado com classe (`darkMode: 'class'`).
   - `postcss.config.mjs` e estilos globais em `app/globals.css`.
   - `vitest.config.ts` configurado para testes unitários com jsdom e `@testing-library/react`.
2. **Sistema de Temas (Light Mode Padrão / Dark Mode Opcional):**
   - O tema padrão inicial é **Light Mode**, em conformidade estrita com o PRD (Seção 3.1 - MVP-01 e RF-051).
   - Alternância para Dark Mode com persistência da preferência no `localStorage` sob a chave `seller_theme`.
   - Prevenção de FOUC (*Flash of Unstyled Content*) com inicialização consistente.
3. **Shell de Navegação Responsiva (Layout Master):**
   - **Header Unificado:** Logo institucional, nome da plataforma, tenant ativo (Matriz Principal), badge de status ("Fail-Closed Protegido"), versão e botão de alternância de tema.
   - **Sidebar Desktop (Telas $\ge$ 1024px):** Navegação lateral organizada pelos 6 pilares estratégicos do PRD, com busca rápida de módulos.
   - **Dock Inferior Mobile (Telas $<$ 1024px):** Barra inferior fixa (`fixed bottom-0`, altura `4rem / 64px`, alvos de toque $\ge 48$px) com atalhos principais para operação com 1 polegar.
   - **Drawer / Slide-Over Mobile:** Menu hambúrguer completo para acesso aos 17 módulos de domínio sem quebra de layout.
   - **Contenção Horizontal:** Aplicação rigorosa de `w-full max-w-full overflow-x-hidden` sem margens negativas que gerem rolagem horizontal indesejada.
4. **Tratamento de Estados de UI:**
   - `EmptyState`: Componente reutilizável para listagens ou buscas vazias com ícone, título, descrição amigável e ação recomendada.
   - `LoadingSkeleton`: Componente de carregamento visual para dados de cards e tabelas.
   - `ErrorBoundary` (`app/error.tsx` e `app/global-error.tsx`): Captura global de falhas de renderização com mensagem amigável e botão de recuperação.
   - `ModulePlaceholder`: Exibição visual padronizada do módulo ativo com seus objetivos, pilares e proveniência sem forjar dados não suportados.
5. **Acessibilidade e Usabilidade (WCAG 2.2 AA):**
   - Todos os elementos interativos possuem alvos de toque mínimos de 44x44px.
   - Suporte a navegação integral por teclado (`Tab`, `Shift+Tab`, `Enter`, `Escape` para fechar drawer).
   - Atributos `aria-label`, `aria-expanded` e `role="navigation"` implementados.

---

## 3. Não-Escopo

- Construção de telas funcionais complexas com chamadas a endpoints que ainda não existem (o backend atualmente implementou apenas o core foundation e actuator/openapi).
- Integração com gateways de pagamento ou telas de faturamento.
- Scraping ou manipulação de páginas externas.

---

## 4. Cenários Gherkin de Validação

```gherkin
Funcionalidade: Shell da Aplicação Web Responsiva

  Cenário: Inicialização do Shell no Tema Claro (Light Mode Padrão)
    Dado que o usuário acessa a plataforma pela primeira vez
    Quando a aplicação é renderizada
    Então o tema ativo deve ser o Light Mode
    E a classe "dark" não deve estar presente no elemento raiz HTML
    E o botão de alternância de tema deve indicar a opção de ativar o modo escuro

  Cenário: Alternância e Persistência do Modo Escuro
    Dado que a aplicação está no Light Mode
    Quando o usuário clica no botão de alternância de tema
    Então a classe "dark" deve ser aplicada ao elemento raiz
    E a preferência "dark" deve ser gravada no localStorage sob a chave "seller_theme"
    E uma nova recarga deve manter o modo escuro ativo

  Cenário: Navegação e Seleção de Módulos nos Pilares Estratégicos
    Dado que a aplicação está carregada no desktop
    Quando o usuário clica no módulo "Motor Financeiro"
    Então o cabeçalho contextual deve exibir o pilar "Seller Control & Finanças"
    E o painel principal deve exibir o placeholder com as regras e status do motor financeiro

  Cenário: Responsividade e Ausência de Overflow Horizontal
    Dado que o layout é avaliado nos breakpoints de 360px, 768px, 1024px e 1440px
    Quando a página é renderizada
    Então a largura máxima do container principal não deve exceder 100vw
    E a barra de rolagem horizontal deve estar ausente

  Cenário: Abertura e Fechamento do Menu Mobile
    Dado que a aplicação está em resolução mobile (360px)
    Quando o usuário toca no botão hambúrguer do cabeçalho
    Então o menu lateral slide-over deve ser exibido com os módulos categorizados
    E ao clicar no botão fechar ou pressionar "Escape", o menu deve ser fechado

  Cenário: Renderização de Componente Empty State
    Dado que uma lista ou pesquisa não retorna resultados
    Quando o componente EmptyState é renderizado
    Então deve exibir o título amigável, descrição explicativa e botão de ação acessível
```

---

## 5. Matriz de Rastreabilidade

| Requisito PRD | Descrição | Componente Técnico | Teste Automatizado |
|---|---|---|---|
| **MVP-01** | Shell de aplicação responsivo e modular | `AppShell.tsx`, `layout.tsx`, `page.tsx` | `AppShell.test.tsx` |
| **RF-050** | Responsividade desktop/tablet/mobile (360px a ultrawide)| Tailwind Breakpoints (`sm`, `md`, `lg`, `xl`) | `ResponsiveLayout.test.tsx` |
| **RF-051** | Light mode padrão e dark mode opcional persistido | `ThemeToggle.tsx`, hook `useTheme` | `ThemeToggle.test.tsx` |
| **RF-052** | Estados vazios, loading e erro | `EmptyState.tsx`, `LoadingSkeleton.tsx`, `error.tsx` | `UiStates.test.tsx` |
| **RNF-009** | Acessibilidade WCAG 2.2 AA e navegação por teclado | ARIA labels, focus states, trap de teclado | `Accessibility.test.tsx` |
| **RNF-010** | Sem overflow horizontal crítico | Contenção `overflow-x-hidden`, zero margem negativa | `ResponsiveLayout.test.tsx` |
