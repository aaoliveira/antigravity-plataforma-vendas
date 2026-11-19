# Especificação Técnica: P021-BE - Exportação Assíncrona CSV/XLSX com Filtros Persistidos, Sanitização de Fórmulas e Auditoria

## 1. Contexto & Objetivos

Esta especificação define o backend do **P021-BE**, implementando o motor de **Exportação Assíncrona de Dados Autorizados do Tenant (CSV/XLSX)** com storage privado namespaced, URLs temporárias assinadas, expiração de arquivo, prevenção estrita contra CSV Formula Injection e trilha de auditoria completa na **Seller Intelligence Platform**.

### Princípios Inegociáveis & Guardrails:
1. **Isolamento de Dados do Próprio Tenant (Zero Cross-Tenant Leakage):**
   - A exportação processa e extrai **exclusivamente** dados autorizados pertencentes ao `tenant_id` autenticado (`FINANCIAL_FACTS`, `ORDERS`, `CATALOG`, `METRICS`).
   - É terminantemente proibido cruzar dados de múltiplos tenants ou permitir que o operador do Tenant Alpha exporte ou baixe dados do Tenant Beta.
   - O download do arquivo valida simultaneamente: (a) o `tenant_id` da sessão ativa, (b) o ID do job de exportação, (c) o token de download temporário criptograficamente seguro e (d) o carimbo de expiração (`expires_at`).
2. **Sanitização Severa contra CSV Formula Injection (OWASP ASVS Nível 2):**
   - Todo campo tabular em exportações CSV (e células string em XLSX) é submetido a sanitização ativa antes da serialização.
   - Qualquer célula cujo valor (após trim ou com caracteres de controle iniciais) comece com:
     * `=` (fórmulas do Excel/Google Sheets)
     * `+` (operador aritmético de coerção)
     * `-` (operador aritmético ou execução de comandos)
     * `@` (funções ou chamadas DDE)
     * `\t` (tabulação para evasão)
     * `\r` (carriage return para evasão)
     * `|` (pipe para execução de comandos Shell/PowerShell)
   - É neutralizada prefixando-se um apóstrofo (`'`) ou neutralizando os caracteres de controle, impedindo que o Excel, LibreOffice Calc ou Google Sheets interpretem o conteúdo como código executável.
3. **Storage Privado com Namespace por Tenant:**
   - Nenhum arquivo exportado é salvo em pasta pública ou acessível diretamente pelo servidor web HTTP.
   - Os arquivos são persistidos em diretório privado namespaced: `tenants/{tenant_id}/exports/{job_id}.{csv|xlsx}`.
   - Validação anti-path traversal em qualquer parâmetro de caminho de arquivo (rejeitando `..`, `/`, `\` ou caracteres nulos).
4. **URL Temporária e Expiração Automática:**
   - Cada job de exportação concluído gera um `downloadToken` aleatório seguro (64 caracteres hex/URL-safe).
   - O arquivo possui validade finita (`expires_at`), por padrão 24 horas (máximo 72 horas).
   - Tentativas de download após o prazo de expiração são rejeitadas com erro RFC 7807 (`EXPORT_EXPIRED` - HTTP 410 Gone ou HTTP 403 Forbidden).
5. **Job Idempotente:**
   - Submissões com a mesma `idempotencyKey` pelo mesmo tenant retornam o job existente se ele estiver em processamento (`PENDING`, `PROCESSING`) ou já finalizado (`COMPLETED` e não expirado), prevenindo duplicidade de processamento e sobrecarga de I/O.
6. **Limites Rígidos de Linhas:**
   - O job suporta limitação configurável de linhas com teto de segurança (`defaultMaxRows = 10.000`, `hardMaxRows = 50.000`).
   - Se o conjunto de dados exceder o limite, o arquivo é gerado até o limite e sinalizado com `is_truncated = true` e contagem real de registros.
7. **Trilha de Auditoria Completa:**
   - Toda etapa do ciclo de vida é registrada em `export_audit_logs`:
     * `EXPORT_REQUESTED`: solicitada exportação com dataset e filtros.
     * `EXPORT_COMPLETED`: geração finalizada com sucesso, contagem de linhas e checksum SHA-256.
     * `EXPORT_FAILED`: erro interno durante o processamento assíncrono.
     * `EXPORT_DOWNLOADED`: arquivo baixado pelo operador com IP e User-Agent.
     * `EXPORT_EXPIRED_ACCESS_ATTEMPT`: tentativa de download após expiração.
     * `EXPORT_CROSS_TENANT_ACCESS_BLOCKED`: tentativa não autorizada de acessar job de outro tenant.
8. **Isolamento em Profundidade com PostgreSQL 17 RLS:**
   - Tabelas `export_jobs` e `export_audit_logs` possuem chave estrangeira mandatória para `organizations(id)` e `FORCE ROW LEVEL SECURITY`.

---

## 2. Critérios de Aceite (Definition of Acceptance)

1. **Exportação Não Cruza Tenant:**
   - Nenhuma linha ou coluna pertencente a outro tenant é incluída no arquivo.
   - Teste de integração cross-tenant comprova que o Tenant Beta não consegue listar, consultar status nem baixar exportações geradas pelo Tenant Alpha, mesmo forjando o ID ou possuindo o token.
2. **Fórmulas Perigosas Neutralizadas:**
   - Células contendo `=SUM(A1:B10)`, `-2+3*cmd|' /C calc'!A0`, `@HYPERLINK("http://evil.com")`, `+12345` com payload malicioso ou tabulações são sanitizadas adicionando `'` no início.
   - Testes unitários com suíte exaustiva de vetores OWASP comprovam a neutralização.
3. **Link Expira:**
   - Tentativa de download após o carimbo `expiresAt` é bloqueada com status apropriado (`EXPORT_EXPIRED`) e evento de auditoria registrado.
4. **Job Idempotente:**
   - Requisições repetidas com a mesma chave de idempotência e tenant retornam o mesmo `export_id` sem reprocessar do zero.
5. **Auditoria Registrada:**
   - Ações de solicitação, geração, download e bloqueios de segurança são persistidas com `tenant_id`, `export_job_id`, `user_id`, `ip_address` e `timestamp`.

---

## 3. Modelo de Dados & Migrações Flyway

### Tabela `export_jobs`
- `id` (UUID PRIMARY KEY)
- `tenant_id` (UUID NOT NULL REFERENCES organizations(id))
- `requested_by` (VARCHAR(100) NOT NULL)
- `idempotency_key` (VARCHAR(120))
- `dataset` (VARCHAR(50) NOT NULL) -- FINANCIAL_FACTS, ORDERS, CATALOG, METRICS
- `file_format` (VARCHAR(10) NOT NULL) -- CSV, XLSX
- `filter_params_json` (TEXT NOT NULL DEFAULT '{}')
- `status` (VARCHAR(30) NOT NULL DEFAULT 'PENDING') -- PENDING, PROCESSING, COMPLETED, FAILED, EXPIRED
- `max_rows` (INTEGER NOT NULL DEFAULT 10000)
- `total_rows` (INTEGER DEFAULT 0)
- `is_truncated` (BOOLEAN NOT NULL DEFAULT FALSE)
- `file_name` (VARCHAR(255))
- `storage_path` (VARCHAR(500))
- `file_size_bytes` (BIGINT DEFAULT 0)
- `checksum_sha256` (VARCHAR(64))
- `download_token` (VARCHAR(120))
- `expires_at` (TIMESTAMP WITH TIME ZONE)
- `completed_at` (TIMESTAMP WITH TIME ZONE)
- `error_message` (TEXT)
- `created_at`, `updated_at` (TIMESTAMP WITH TIME ZONE)

### Tabela `export_audit_logs`
- `id` (UUID PRIMARY KEY)
- `tenant_id` (UUID NOT NULL REFERENCES organizations(id))
- `export_job_id` (UUID REFERENCES export_jobs(id) ON DELETE SET NULL)
- `action` (VARCHAR(60) NOT NULL)
- `user_id` (VARCHAR(100) NOT NULL)
- `ip_address` (VARCHAR(45))
- `user_agent` (VARCHAR(300))
- `details_json` (TEXT DEFAULT '{}')
- `created_at` (TIMESTAMP WITH TIME ZONE)

---

## 4. Endpoints da API REST (Módulo Reporting)

- `POST /api/reports/exports`: Solicita job de exportação (idempotente, filtros persistidos).
- `GET /api/reports/exports`: Lista histórico de exportações do tenant.
- `GET /api/reports/exports/{id}`: Consulta status e metadados de um job.
- `GET /api/reports/exports/{id}/download?token={downloadToken}`: Download seguro do arquivo gerado (valida tenant, token e expiração).
- `GET /api/reports/exports/audit`: Lista registros de auditoria de exportação do tenant.
