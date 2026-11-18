# Especificação Técnica: P003-FE — Tratamento Padronizado de Erros (401/403/404) e Segurança de Tenant na UI

> **Identificador da Feature:** `P003-FE` &middot; **Versão:** 1.0.0  
> **Status da Spec:** Aprovada para Implementação  
> **Requisitos do PRD:** RF-001, RF-002, RF-003, RNF-001, RNF-009, RNF-010  
> **Padrão de Segurança:** OWASP ASVS Nível 2 (Anti-Enumeração, Proteção contra Manipulação de URL e Ocultação de Metadados Internos)

---

## 1. Contexto e Objetivo

No backend (`P003-BE`), consolidamos a Defesa em Profundidade com isolamento transacional estrito, PostgreSQL Row-Level Security (RLS) e sanitização de logs.  
Nesta etapa de frontend (`P003-FE`), implementamos o **tratamento padronizado de erros e as salvaguardas de interface** para garantir que a aplicação web:
1. **Nunca revele a existência de recursos de outro tenant** ao receber respostas 401, 403 ou 404 (tratamento uniforme anti-enumeração).
2. **Nunca permita a troca ou seleção de organização/tenant através da manipulação da URL** (query parameters como `?tenantId=...`, paths forjados ou fragmentos).
3. **Nunca exiba o identificador técnico interno (UUID)** do tenant na interface de usuário, menus, breadcrumbs ou mensagens de erro.
4. **Forneça feedback claro e acessível sobre estados de sessão** (sessão ativa, expirando, expirada e acessos não autorizados) com navegação segura de retorno.

---

## 2. Escopo Obrigatório

1. **Proteção contra Manipulação de URL (`middleware.ts` & `AuthContext.tsx`):**
   - O tenant ativo é governado exclusivamente pelo cookie de sessão autenticada `seller_access_token` e pela membership validada no backend.
   - O middleware intercepta e descarta qualquer tentativa de forçar tenant via URL (`?tenantId=...`, `?tenant=...`, `?orgId=...`).
   - Se parâmetros de tenant forem detectados na URL, o usuário é redirecionado para a rota canônica limpa, sem que qualquer alternância de tenant ocorra.

2. **Ocultação Mandatória de UUID Técnico na UI:**
   - Todos os componentes de interface (`TenantSwitcher`, `AppShell`, páginas de erro e cabeçalhos) devem exibir apenas nomes de exibição amigáveis (ex: "Tenant Alpha Corp", "Loja Matriz") e papéis de acesso (ex: "OWNER", "ADMIN").
   - Mensagens de erro de API são sanitizadas antes de serem exibidas para mascarar qualquer UUID ou metadado técnico interno.

3. **Página e Componentes de Erro Uniformes (401, 403, 404):**
   - `frontend/src/app/not-found.tsx`: Página padronizada para recursos não encontrados ou inacessíveis, com mensagem neutra ("Página ou recurso não encontrado ou sem permissão de acesso"), evitando que o usuário deduza se o ID pertence a outra organização.
   - `frontend/src/components/AccessDeniedFeedback.tsx`: Componente de feedback para ações bloqueadas por perfil de acesso ou escopo na organização.
   - `frontend/src/components/SessionBanner.tsx`: Alerta acessível para sessão expirada ou restrita, orientando o usuário para login sem perda de dados.
   - `frontend/src/app/error.tsx`: Componente global atualizado para sanitização estrita de stack trace e digest em produção.

4. **Testes Automatizados de Componente (Vitest):**
   - Teste de descarte de manipulação de URL de tenant.
   - Teste de ausência de tenant ID técnico (UUID) em todos os elementos de UI.
   - Teste de mensagens uniformes para 403/404 contra enumeração cross-tenant.
   - Teste de renderização de estados de sessão expirada (401) e acesso negado (403).

---

## 3. Não-Escopo

- Gestão de convites, novos membros ou cobrança (fora do escopo deste prompt).
- Conexão OAuth real com marketplaces (escopo de prompts futuros).

---

## 4. Cenários Gherkin de Validação

```gherkin
Funcionalidade: Tratamento Padronizado de Erros e Proteção de Tenant na UI

  Cenário: Usuário tenta trocar de organização manipulando a URL
    Dado que o usuário está autenticado na organização "Tenant Alpha Corp"
    Quando o usuário acessa "/?tenantId=22222222-2222-2222-2222-222222222222"
    Então o sistema deve ignorar o parâmetro de URL e manter o contexto em "Tenant Alpha Corp"
    E a URL deve ser sanitizada removendo os parâmetros injetados

  Cenário: Tentativa de acesso a recurso não existente ou pertencente a outro tenant
    Dado que o usuário solicita uma página ou recurso inacessível
    Quando a resposta for 403 ou 404
    Então a interface deve exibir mensagem uniforme "Recurso não encontrado ou acesso restrito"
    E nenhuma informação sobre a existência prévia do recurso ou de outro tenant deve ser revelada

  Cenário: Sessão expirada no backend (HTTP 401)
    Dado que a sessão do usuário expirou
    Quando uma requisição retornar HTTP 401 com erro "SESSION_EXPIRED"
    Então a UI deve exibir o estado de sessão expirada
    E oferecer redirecionamento seguro para a tela de autenticação

  Cenário: Ocultação de ID técnico interno do tenant
    Dado que o usuário navega pelo shell da aplicação e pelo seletor de organizações
    Quando qualquer elemento da interface for renderizado
    Então nenhum UUID técnico v4 deve estar visível no texto renderizado
    E apenas o nome amigável da organização e papel devem ser exibidos
```

---

## 5. Matriz de Rastreabilidade

| Requisito PRD | Descrição | Componente Frontend | Teste Automatizado |
|---|---|---|---|
| **RF-001** | Resolução segura de tenant | `middleware.ts`, `AuthContext.tsx` | `UrlTenantProtection.test.ts` |
| **RF-002** | Bloqueio de manipulação de tenant via URL | `middleware.ts` | `UrlTenantProtection.test.ts` |
| **RF-003** | Respostas uniformes 403/404 anti-enumeração | `not-found.tsx`, `AccessDeniedFeedback.tsx` | `ErrorHandlingUniformity.test.tsx` |
| **RNF-001** | Ocultação de metadados e UUIDs técnicos | `TenantSwitcher.tsx`, `AppShell.tsx` | `TenantSwitcher.test.tsx`, `ErrorHandlingUniformity.test.tsx` |
| **RNF-009** | Acessibilidade WCAG 2.2 AA nos alertas | `AccessDeniedFeedback.tsx`, `SessionBanner.tsx` | `ErrorHandlingUniformity.test.tsx` |
| **RNF-010** | Responsividade Mobile First (360px a 1440px) | `not-found.tsx`, `error.tsx` | `ErrorHandlingUniformity.test.tsx` |
