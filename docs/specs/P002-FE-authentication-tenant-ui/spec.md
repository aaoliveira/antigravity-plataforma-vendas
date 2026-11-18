# Especificação Técnica: P002-FE — Interface de Autenticação e Troca de Contexto

> **Identificador da Feature:** `P002-FE` &middot; **Versão:** 1.0.0  
> **Status da Spec:** Aprovada para Implementação  
> **Requisitos do PRD:** RF-001, RF-002, RF-050, RF-051, RNF-001, RNF-009, RNF-010  
> **Padrão Arquitetural:** Next.js 15 App Router, BFF Route Handlers com Cookies HttpOnly, Route Protection Middleware e WCAG 2.2 AA  

---

## 1. Contexto e Objetivo

Implementar a interface de usuário mínima e segura para **Autenticação**, **Proteção de Rotas** e **Seleção/Troca de Contexto do Tenant** no frontend da **Seller Intelligence Platform**.

Em estrita consonância com os requisitos de segurança:
1. **Zero Tokens Sensíveis no `localStorage`:** O token JWT emitido pelo backend é interceptado pelos Route Handlers do Next.js (BFF) e persistido exclusivamente em cookies `HttpOnly`, `SameSite=Lax`, `Path=/`, e `Secure` (em produção), mitigando completamente ataques XSS para roubo de sessão.
2. **Proteção de Rotas:** Acesso a rotas da aplicação (`/`) exige sessão autenticada válida; requisições anônimas são redirecionadas para `/login`.
3. **Sessão Expirada:** Ao detectar expiração de sessão (token expirado ou HTTP 401), a interface invalida o estado local e redireciona o usuário para reautenticação.
4. **Isolamento de Informação de Tenant:** Apenas as organizações/memberships comprovadamente pertencentes ao usuário autenticado são apresentadas na interface de seleção; nenhuma informação de terceiros é exposta.
5. **Acessibilidade e Responsividade:** Totalmente aderente ao padrão WCAG 2.2 AA e verificado nos breakpoints de 360px a 1440px.

---

## 2. Escopo Obrigatório

1. **Camada BFF de Autenticação via Cookies HttpOnly (Next.js Route Handlers):**
   - `POST /api/auth/login`: Recebe email e senha, encaminha ao backend Java (`/api/v1/auth/login`), recebe o token JWT e injeta cookie `HttpOnly` `seller_access_token` (maxAge: 900s). Retorna apenas dados de perfil e tenant ativo.
   - `POST /api/auth/logout`: Invalida o cookie `seller_access_token` (`maxAge: 0`) e encerra a sessão.
   - `GET /api/auth/session`: Lê o cookie `HttpOnly`, valida com o backend (`/api/v1/tenancy/current` e `/api/v1/auth/me`), e retorna os dados do usuário e tenant ativo. Se o backend retornar 401 (token expirado), limpa o cookie e retorna status 401.
   - `POST /api/auth/switch-tenant`: Encaminha troca de tenant com o token do cookie para `/api/v1/auth/switch-tenant`, atualiza o cookie `seller_access_token` com o novo token emitido pelo backend e retorna o novo contexto.

2. **Tela de Autenticação (`/login`):**
   - Formulário acessível com campos de e-mail e senha.
   - Atributos ARIA adequados (`aria-required`, `aria-invalid`, `role="alert"` para mensagens de erro).
   - Suporte a mensagens de bloqueio por força bruta (HTTP 429) e credenciais inválidas (HTTP 401).
   - Botão de alternância de tema (Light Mode padrão / Dark Mode opcional).
   - Responsividade completa de 360px a desktop.

3. **Contexto de Autenticação do Cliente (`AuthContext` / `useAuth`):**
   - Provedor React Context (`AuthProvider`) gerenciando o estado da sessão: `user`, `currentTenant`, `isLoading`, `isAuthenticated`.
   - Métodos: `login(email, password)`, `logout()`, `switchTenant(targetTenantId)`.
   - Interceptor/detector de sessão expirada redirecionando para `/login`.

4. **Componente de Troca de Contexto de Organização/Tenant (`TenantSwitcher`):**
   - Integrado ao Header e Drawer do `AppShell`.
   - Exibe nome e role da organização ativa.
   - Dropdown/modal acessível permitindo alternar apenas entre as memberships válidas do próprio usuário.

5. **Proteção de Rotas:**
   - Middleware e verificação de layout redirecionando usuários não autenticados para `/login`.
   - Redirecionamento de usuários já autenticados que acessam `/login` de volta para o dashboard principal.

---

## 3. Não-Escopo (O Que NÃO Será Implementado Neste Prompt)

- Convite de novos membros ou gestão de assentos/equipes.
- Faturamento, planos, cobranças ou upgrades (billing).
- CRUD completo ou edição de dados cadastrais de usuários.
- Conexão e telas de autorização OAuth de marketplaces (Mercado Livre, Shopee).

---

## 4. Cenários Gherkin de Validação

```gherkin
Funcionalidade: Autenticação Segura, Proteção de Rotas e Troca de Contexto do Tenant

  Cenário: Login bem-sucedido com cookie HttpOnly
    Dado que o usuário insere credenciais válidas "seller@example.com" e "Password@123"
    Quando o formulário de login é submetido
    Então a rota BFF deve gravar o cookie "seller_access_token" com HttpOnly e SameSite=Lax
    E o usuário deve ser redirecionado para a aplicação principal
    E nenhum token sensível deve ser salvo no localStorage

  Cenário: Login com credenciais inválidas
    Dado que o usuário insere senha incorreta
    Quando o formulário é submetido
    Então deve ser exibida uma mensagem de alerta acessível com role="alert"
    E o usuário permanece na tela de login

  Cenário: Redirecionamento de rota protegida para não autenticados
    Dado que o usuário não possui sessão ou cookie ativo
    Quando tenta acessar a rota principal "/"
    Então deve ser redirecionado automaticamente para "/login"

  Cenário: Sessão expirada direciona para autenticação
    Dado que a sessão do usuário expirou (token JWT expirado no backend)
    Quando o cliente tenta validar a sessão ou executar operação
    Então a sessão local deve ser limpa
    E o usuário é redirecionado para "/login"

  Cenário: Troca de contexto entre organizações do próprio usuário
    Dado que o usuário possui membership nas organizações A e B
    Quando o usuário aciona o seletor de tenant e escolhe a Organização B
    Então o seletor exibe apenas as organizações A e B (nenhuma de terceiros)
    E o contexto ativo é atualizado com sucesso
```

---

## 5. Matriz de Rastreabilidade

| Requisito PRD | Descrição | Componente Técnico | Teste Automatizado |
|---|---|---|---|
| **RF-001** | Tenant derivado de contexto confiável | `TenantSwitcher`, BFF `/api/auth/switch-tenant` | `TenantSwitcher.test.tsx` |
| **RF-002** | Autorização e isolamento de tenant | `AuthProvider`, rotas protegidas | `AuthContext.test.tsx` |
| **RF-050** | Layout e design system acessível | `/login`, `TenantSwitcher` | `LoginPage.test.tsx` |
| **RF-051** | Light Mode padrão e Dark Mode | `ThemeToggle`, Tailwind classes | `ThemeToggle.test.tsx` |
| **RNF-001** | OWASP ASVS (Cookies HttpOnly, sem localStorage) | Route Handlers Next.js (`/api/auth/*`) | `BffAuthRoutes.test.ts` |
| **RNF-009** | Acessibilidade WCAG 2.2 AA | Atributos ARIA, foco e contraste | `LoginPage.test.tsx` |
| **RNF-010** | Responsividade (360px a 1440px) | Layout Tailwind flex/grid responsivo | `LoginPage.test.tsx` |
