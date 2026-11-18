# Especificação Técnica: P003-BE — Isolamento Multi-Tenant em Profundidade (RLS, Transacional e Testcontainers)

> **Identificador da Feature:** `P003-BE` &middot; **Versão:** 1.0.0  
> **Status da Spec:** Aprovada para Implementação  
> **Requisitos do PRD:** RF-001, RF-002, RF-003, RF-004, RNF-001, RNF-002, RNF-006, ADR-002  
> **Padrão Arquitetural:** Multi-Tenant Defense-in-Depth (Server-Side Filter + PostgreSQL RLS + SET LOCAL transacional + Testcontainers)  

---

## 1. Contexto e Objetivo

Implementar o mecanismo completo de **Defesa em Profundidade para Isolamento Multi-Tenant** no backend da **Seller Intelligence Platform**.  
Garantir que sob nenhuma hipótese ocorra vazamento, acesso cruzado, leitura, criação, alteração ou exclusão de registros entre diferentes organizações/tenants.

A solução cobre:
1. **Camada de Dados com PostgreSQL Row-Level Security (RLS):**
   - Ativação de `ENABLE ROW LEVEL SECURITY` e `FORCE ROW LEVEL SECURITY` em tabelas tenant-scoped (`marketplace_connections`, `tenant_documents`).
   - Política `tenant_isolation_policy` vinculando registros a `NULLIF(current_setting('app.current_tenant', true), '')::uuid`.
   - Índices compostos `(tenant_id, ...)` e chaves estrangeiras com integridade referencial estrita para `organizations(id)`.
2. **Contexto Transacional e SET LOCAL:**
   - Interceptor/serviço transacional que executa `SET LOCAL app.current_tenant = '<tenant_id>'` a cada transação aberta.
   - Escopo atômico de transação no PostgreSQL e garantia de limpeza (`RESET app.current_tenant`) ao retornar a conexão ao pool HikariCP.
3. **Filtros Server-Side na Aplicação:**
   - Repositórios Spring Data JPA com cláusulas explícitas filtrando `tenantId` em todas as operações CRUD e pesquisas.
4. **Sanitização de Segredos na Auditoria:**
   - Mecanismo de mascaramento `[REDACTED]` impedindo persistência de tokens, senhas ou segredos na trilha de auditoria.
5. **Isolamento em Jobs, Cache, Filas e Storage:**
   - Utilitários padronizados para execução de jobs (`TenantJobExecutor`), chaves de cache (`tenant:{id}:...`), cabeçalhos de mensageria (`x-tenant-id`) e caminhos de storage S3/MinIO (`/tenants/{id}/...` com bloqueio a path traversal).
6. **Suíte Completa de Testes em PostgreSQL Real via Testcontainers:**
   - Cenários exaustivos com pelo menos dois tenants (Tenant Alpha e Tenant Beta).
   - Tentativas cruzadas de ler, criar, alterar, excluir, pesquisar, exportar e recuperar arquivos de outro tenant — todas categoricamente negadas.
   - Verificação de que conexões reutilizadas no pool não retêm contexto residual da transação anterior.

---

## 2. Escopo Obrigatório

1. **Migrações Flyway:**
   - `db/migration/V3__tenant_scoped_entities.sql`:
     * Tabela `marketplace_connections` (UUID, tenant_id FK NOT NULL, marketplace, seller_account_id, account_name, encrypted_access_token, encrypted_refresh_token, status, timestamps, unique(tenant_id, marketplace, seller_account_id)).
     * Tabela `tenant_documents` (UUID, tenant_id FK NOT NULL, file_name, storage_path, file_size_bytes, content_type, checksum, timestamps).
     * Índices compostos otimizados: `idx_conn_tenant_status` e `idx_docs_tenant_created`.
   - `db/migration-postgresql/V3_1__postgresql_rls_defense.sql` (aplicado em PostgreSQL / Testcontainers):
     * `ALTER TABLE marketplace_connections ENABLE ROW LEVEL SECURITY;`
     * `ALTER TABLE marketplace_connections FORCE ROW LEVEL SECURITY;`
     * `CREATE POLICY tenant_isolation_connections ON marketplace_connections FOR ALL USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid) WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);`
     * `ALTER TABLE tenant_documents ENABLE ROW LEVEL SECURITY;`
     * `ALTER TABLE tenant_documents FORCE ROW LEVEL SECURITY;`
     * `CREATE POLICY tenant_isolation_documents ON tenant_documents FOR ALL USING (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid) WITH CHECK (tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid);`

2. **Entidades e Repositórios:**
   - Em `modules/marketplaceconnection`: `MarketplaceConnection`, `MarketplaceConnectionRepository`.
   - Em `modules/reporting`: `TenantDocument`, `TenantDocumentRepository`.

3. **Gerenciamento de Sessão de Banco Transacional (`TenantDatabaseSession`):**
   - Execução de `SET LOCAL app.current_tenant` vinculado à transação atual.
   - Limpeza explícita para conexões retornadas ao pool.

4. **Sanitização de Auditoria (`AuditSanitizer`):**
   - Substituição de tokens (`access_token`, `refresh_token`), senhas e segredos por `[REDACTED]`.

5. **Isolamento de Infraestrutura Auxiliar:**
   - `TenantJobExecutor`: execução isolada de tarefas assíncronas com propagação e limpeza de contexto.
   - `TenantCacheKeyGenerator`: geração de chaves Redis namespaced `tenant:{tenant_id}:{entity}:{key}`.
   - `TenantQueueValidator`: validação de cabeçalho `x-tenant-id` para mensagens RabbitMQ.
   - `TenantStoragePathResolver`: resolução de caminhos S3 `/tenants/{tenant_id}/...` com proteção contra path traversal.

6. **Testes de Integração com Testcontainers (PostgreSQL 17/18 Real):**
   - Teste de leitura cross-tenant -> Retorna vazio / 404.
   - Teste de criação com tenantId divergente do contexto -> Bloqueado por RLS / validação.
   - Teste de alteração cruzada -> 0 linhas afetadas / negado.
   - Teste de exclusão cruzada -> 0 linhas afetadas / negado.
   - Teste de busca/pesquisa cruzada -> Nenhum dado do outro tenant retornado.
   - Teste de exportação e download cruzado de storage -> `TenantAccessDeniedException`.
   - Teste de reuso de conexão no pool HikariCP -> `app.current_tenant` limpo entre transações.

---

## 3. Não-Escopo

- Chamadas HTTP a APIs externas reais do Mercado Livre ou Shopee (escopo dos adaptadores subsequentes).
- Telas de visualização de conexão (pertence a P003-FE).

---

## 4. Cenários Gherkin de Validação

```gherkin
Funcionalidade: Defesa em Profundidade e Isolamento Multi-Tenant

  Cenário: Tentativa de leitura cross-tenant no banco
    Dado que existe uma conexão cadastrada no Tenant Alpha
    Quando uma transação com contexto do Tenant Beta tenta ler essa conexão
    Então a consulta deve retornar vazio
    E nenhuma informação do Tenant Alpha deve ser exposta

  Cenário: Tentativa de alteração cross-tenant no banco
    Dado que existe uma conexão cadastrada no Tenant Alpha
    Quando uma transação com contexto do Tenant Beta tenta atualizar o nome da conexão
    Então a operação deve falhar ou afetar zero linhas
    E os dados no Tenant Alpha devem permanecer inalterados

  Cenário: Tentativa de exclusão cross-tenant no banco
    Dado que existe um documento cadastrado no Tenant Alpha
    Quando uma transação com contexto do Tenant Beta tenta excluir o documento
    Então a operação deve afetar zero linhas
    E o documento do Tenant Alpha deve continuar existindo

  Cenário: Tentativa de download de arquivo de outro tenant no storage
    Dado que existe um arquivo "/tenants/11111111-1111-1111-1111-111111111111/export.csv"
    Quando o Tenant Beta tenta resolver ou acessar esse caminho
    Então o sistema deve lançar TenantAccessDeniedException
    E o acesso deve ser categoricamente bloqueado

  Cenário: Reutilização de conexão no pool sem vazamento de contexto
    Dado que a Conexão 1 executou uma transação para o Tenant Alpha com SET LOCAL
    Quando a transação é finalizada e a Conexão 1 é reutilizada para uma nova transação sem tenant
    Então o valor de "app.current_tenant" na conexão deve estar vazio/nulo
    E nenhuma política de RLS do Tenant Alpha deve estar ativa
```

---

## 5. Matriz de Rastreabilidade

| Requisito PRD | Descrição | Componente Técnico | Teste Automatizado |
|---|---|---|---|
| **RF-001** | Resolução segura de tenant | `TenantDatabaseSession`, `TenantContextHolder` | `PostgresMultiTenantRlsIntegrationTest` |
| **RF-003** | Segregação estrita de dados | PostgreSQL RLS, Server-Side Filters | `PostgresMultiTenantRlsIntegrationTest` |
| **RF-004** | Auditoria imutável sem segredos | `AuditSanitizer`, `AuditService` | `AuditSanitizerTest` |
| **RNF-001** | OWASP ASVS (Prevenção de vazamento) | Sanitização de logs, RLS `FORCE` | `PostgresMultiTenantRlsIntegrationTest`, `AuditSanitizerTest` |
| **RNF-002** | Testes automatizados negativos cross-tenant | Testcontainers PostgreSQL 17/18 | `PostgresMultiTenantRlsIntegrationTest` |
