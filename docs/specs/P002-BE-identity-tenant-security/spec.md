# Especificação Técnica: P002-BE — Identidade Mínima, Autenticação e Contexto do Tenant

> **Identificador da Feature:** `P002-BE` &middot; **Versão:** 1.0.0  
> **Status da Spec:** Aprovada para Implementação  
> **Requisitos do PRD:** RF-001, RF-002, RF-003, RF-004, RNF-001, RNF-002  
> **Padrão Arquitetural:** Multi-Tenant Defense-in-Depth, JWT RFC 7519 HMAC-SHA256, Anti-Spoofing e Brute Force Protection  

---

## 1. Contexto e Objetivo

Implementar o núcleo seguro de **Identidade Mínima**, **Autenticação**, **Membership** e **Contexto do Tenant** no backend da **Seller Intelligence Platform**.  
O identificador do tenant (`tenant_id`) deve ser derivado exclusivamente do principal autenticado e de sua respectiva membership ativa comprovada, impedindo categoricamente qualquer tentativa de manipulação, injeção ou spoofing por headers arbitrários (`X-Tenant-Id`) ou parâmetros de consulta (`?tenantId=...`).

A solução inclui:
- Contratos REST mínimos de autenticação (`/api/v1/auth/login`, `/api/v1/auth/me`, `/api/v1/auth/switch-tenant`).
- Provedor e validador de tokens JWT (RFC 7519 HMAC-SHA256) com tempo de expiração explícito.
- Mecanismo de proteção contra força bruta (Brute Force Protection) bloqueando credenciais/IPs abusivos.
- Trilha de auditoria persistente e estruturada para autenticações, bloqueios e tentativas de spoofing.
- Suíte exaustiva de testes negativos (acesso anônimo, tenant ausente, token expirado, credenciais inválidas, bloqueio brute-force e spoofing cross-tenant).

---

## 2. Escopo Obrigatório

1. **Migração Flyway `V2__identity_and_tenancy.sql`:**
   - Criação da tabela `app_users` (UUID, email único, password_hash BCrypt, full_name, status, timestamps).
   - Criação da tabela `memberships` (UUID, user_id FK, tenant_id FK para organizations, role, status, unique(user_id, tenant_id), timestamps).
   - Criação da tabela `audit_events` (UUID, event_type, principal_id, principal_email, tenant_id, ip_address, user_agent, correlation_id, details, created_at).
   - Carga inicial (seed de teste/local) com organizações e usuários pré-configurados com senhas seguras em BCrypt.

2. **Entidades de Domínio e Repositórios:**
   - Em `modules/identityaccess`: `AppUser`, `UserRepository`, `PasswordService` (BCryptPasswordEncoder).
   - Em `modules/tenancy`: `Organization`, `Membership`, `MembershipRepository`, `TenancyService`.
   - Em `modules/audit`: `AuditEvent`, `AuditEventRepository`, `AuditService`.

3. **Mecanismo de Token JWT Seguro (RFC 7519):**
   - Provedor HMAC-SHA256 assinado com chave secreta da aplicação (`security.jwt.secret`), com expiração de 15 minutos (900s).
   - Validação criptográfica da assinatura e do timestamp `exp`. Rejeição com erro explícito se expirado ou adulterado.

4. **Resolução de Contexto Multi-Tenant e Anti-Spoofing:**
   - `TenantContext` & `TenantContextHolder`: encapsulamento em `ThreadLocal` do `tenant_id` e roles do tenant ativo.
   - Limpeza mandatória do `TenantContextHolder` via `finally` em filtro web (`TenantContextFilter`).
   - Regra de Derivação Estrita:
     * O tenant padrão é extraído das memberships ativas do usuário autenticado.
     * Se o cliente enviar o header `X-Tenant-Id`, o sistema valida se o usuário possui membership ATIVA naquele tenant específico.
     * Se o usuário **NÃO** pertencer ao tenant solicitado: a requisição é IMEDIATAMENTE rejeitada com **HTTP 403 Forbidden**, e um evento `TENANT_SPOOFING_ATTEMPT` é registrado na auditoria com o IP, usuário, correlation ID e tenant fraudulento.
     * Parâmetros de consulta (`?tenantId=...`) são categoricamente ignorados para resolução de contexto de execução.

5. **Proteção Contra Força Bruta (Brute Force Protection):**
   - `BruteForceProtectionService`: rastreia tentativas consecutivas de autenticação com falha por chave (`ip:email`).
   - Limite configurável (padrão: 5 tentativas com falha).
   - Ao atingir o limite, novas tentativas são rejeitadas com **HTTP 429 Too Many Requests** durante janela de 15 minutos (900s), registrando `BRUTE_FORCE_BLOCKED` na auditoria.
   - Sucesso no login zera o contador de tentativas da respectiva chave.

6. **Endpoints REST de Autenticação e Contexto:**
   - `POST /api/v1/auth/login`: recebe email e senha, valida bloqueio brute force, valida hash BCrypt, identifica memberships ativas e gera token JWT.
   - `GET /api/v1/auth/me`: retorna os dados da identidade autenticada (`userId`, `email`, `fullName`, memberships disponíveis).
   - `POST /api/v1/auth/switch-tenant`: permite alternar o tenant ativo, validando se o usuário possui membership válida no tenant destino e emitindo novo token escopado.
   - `GET /api/v1/tenancy/current`: endpoint de teste/verificação tenant-scoped que exige contexto de tenant válido.

7. **Configuração de Segurança Spring Security:**
   - `SecurityConfig`: Atualizada para integrar `JwtAuthenticationFilter` e `TenantContextFilter`.
   - Política Fail-Closed: Qualquer endpoint que não seja público (`/actuator/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/api/v1/auth/login`) exige autenticação obrigatória.

---

## 3. Não-Escopo (O Que NÃO Será Implementado Neste Prompt)

- Telas e componentes visuais de login ou formulários (pertence ao P002-FE).
- Gestão completa de usuários (cadastro público aberto, convite de membros por email, recuperação de senha por email, upload de avatar).
- Integração real com marketplaces externos (pertence aos prompts subsequentes P003+).
- Ingestão de pedidos ou cálculos financeiros (pertence a P004+ e P007+).

---

## 4. Cenários Gherkin de Validação

```gherkin
Funcionalidade: Identidade Mínima, Autenticação e Contexto Seguro do Tenant

  Cenário: Autenticação com credenciais válidas e derivação do tenant
    Dado que existe um usuário "seller@exemplo.com" com membership no Tenant A
    Quando o usuário envia POST para "/api/v1/auth/login" com a senha correta
    Então o status HTTP deve ser 200
    E a resposta deve conter "accessToken" válido e o "tenantId" do Tenant A
    E deve haver um registro de "LOGIN_SUCCESS" na auditoria

  Cenário: Autenticação com senha incorreta
    Dado que existe um usuário "seller@exemplo.com"
    Quando o usuário envia POST para "/api/v1/auth/login" com senha incorreta
    Então o status HTTP deve ser 401 Unauthorized
    E deve haver um registro de "LOGIN_FAILED" na auditoria

  Cenário: Bloqueio por brute force após tentativas consecutivas
    Dado que um cliente errou a senha 5 vezes consecutivas para "seller@exemplo.com"
    Quando o cliente tenta autenticar pela 6ª vez
    Então o status HTTP deve ser 429 Too Many Requests
    E deve haver um registro de "BRUTE_FORCE_BLOCKED" na auditoria

  Cenário: Acesso anônimo a endpoint tenant-scoped
    Dado que uma requisição anônima (sem header Authorization) é enviada para "/api/v1/tenancy/current"
    Quando a requisição é processada
    Então o status HTTP deve ser 401 Unauthorized ou 403 Forbidden (Fail-Closed)
    E o acesso aos dados do tenant deve ser negado

  Cenário: Acesso com token JWT expirado
    Dado que o cliente possui um token JWT com data de expiração no passado
    Quando o cliente envia GET para "/api/v1/tenancy/current" com o token expirado
    Então o status HTTP deve ser 401 Unauthorized
    E a mensagem deve indicar que o token está expirado

  Cenário: Tentativa de spoofing cross-tenant via header arbitrário
    Dado que o usuário "seller@exemplo.com" autenticado possui membership apenas no Tenant A
    Quando o usuário envia uma requisição para "/api/v1/tenancy/current" com header "X-Tenant-Id: <UUID do Tenant B>"
    Então o status HTTP deve ser 403 Forbidden
    E a operação deve ser categoricamente bloqueada
    E deve haver um registro de "TENANT_SPOOFING_ATTEMPT" na auditoria

  Cenário: Usuário sem nenhuma membership ativa
    Dado que um usuário autenticado não possui nenhuma membership em qualquer organização
    Quando o usuário tenta acessar "/api/v1/tenancy/current"
    Então o status HTTP deve ser 403 Forbidden
    E a mensagem deve indicar ausência de tenant ativo
```

---

## 5. Matriz de Rastreabilidade

| Requisito PRD | Descrição | Componente Técnico | Teste Automatizado |
|---|---|---|---|
| **RF-001** | Identidade e tenant resolvidos por contexto confiável | `TenantContextFilter`, `TenantContextHolder`, `JwtTokenProvider` | `AuthenticationAndTenancyIntegrationTest` |
| **RF-002** | Autorização por tenant, recurso e permissão | `SecurityConfig`, `TenancyService` | `AuthenticationAndTenancyIntegrationTest` |
| **RF-003** | Segregação de dados por tenant | `memberships`, `organizations` com FK estrita | `AuthenticationAndTenancyIntegrationTest` |
| **RF-004** | Auditoria de acessos e ações críticas | `AuditService`, `audit_events` | `AuditServiceTest`, `AuthenticationAndTenancyIntegrationTest` |
| **RNF-001** | OWASP ASVS Nível 2 (Proteção de sessão e tokens) | BCrypt, JWT HMAC-SHA256, Brute Force Throttling | `BruteForceProtectionServiceTest`, `JwtTokenProviderTest` |
| **RNF-002** | Testes automatizados negativos cross-tenant | Validação anti-spoofing em `TenantContextFilter` | `AuthenticationAndTenancyIntegrationTest` (spoofing scenarios) |
