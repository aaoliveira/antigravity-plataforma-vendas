# Backlog do Projeto — MVP e Evolução

> **Status:** Baseline Homologado &middot; **Versão:** 1.0.0  
> **Convenção de Prioridade:**  
> - **P0:** Bloqueador de arquitetura / Fundação inegociável.  
> - **P1:** Requisito essencial para o primeiro release do MVP.  
> - **P2:** Pós-MVP prioritário.  
> - **P3:** Evolução futura de longo prazo.  

---

## 1. Épico E00 — Governança e Spec-Driven Development (SDD)

| ID | História / Entrega | Prioridade | Dependência | Status | Critério de Aceite Resumido |
|---|---|---|---|---|---|
| **E00-01** | Criar baseline de contexto, PRD e decisões | **P0** | --- | `PASSED` | Documentos versionados e alinhados à spec do PRD em `docs/`. |
| **E00-02** | Definir prompt mestre e protocolo de execução | **P0** | E00-01 | `PASSED` | Protocolo Preflight $\rightarrow$ Spec $\rightarrow$ Impl $\rightarrow$ Validação estabelecido. |
| **E00-03** | Criar status machine e quality gates | **P0** | E00-02 | `PASSED` | Máquina de estados configurada e bloqueio estrito de avanço de prompts. |
| **E00-04** | Criar ADR e log de execução rastreável | **P0** | E00-01 | `PASSED` | Registro de decisões arquiteturais e log de preflight formalizados. |

---

## 2. Épico E01 — Fundação Técnica (P001)

| ID | História / Entrega | Prioridade | Dependência | Status | Critério de Aceite Resumido |
|---|---|---|---|---|---|
| **E01-01** | Estrutura de Monólito Modular Backend | **P0** | E00 | `READY` | Spring Boot modularizado com Spring Modulith e ArchUnit; compilação limpa. |
| **E01-02** | Shell de Aplicação Web Frontend | **P0** | E01-01 | `NOT_STARTED` | Next.js 15 responsivo (360px a 1440px+), light/dark mode e acessibilidade WCAG. |
| **E01-03** | Persistência PostgreSQL & Migrações Flyway | **P0** | E01-01 | `NOT_STARTED` | Scripts de migração reproduzíveis e idempotentes em banco de dados limpo. |
| **E01-04** | Mensageria RabbitMQ & Cache Redis Local | **P1** | E01-01 | `NOT_STARTED` | Health checks e infraestrutura validada via Docker Compose. |
| **E01-05** | Observabilidade Base & Structured Logging | **P0** | E01-01 | `NOT_STARTED` | Logs em JSON com `trace_id`, `tenant_id` e sem vazamento de segredos. |

---

## 3. Épico E02 — Identidade Mínima, Tenant e Segurança (P002)

| ID | História / Entrega | Prioridade | Dependência | Status | Critério de Aceite Resumido |
|---|---|---|---|---|---|
| **E02-01** | Contexto de Tenant e Principal Autenticado | **P0** | E01 | `NOT_STARTED` | Tenant context derivado unicamente de token JWT assinado; acesso negado por padrão. |
| **E02-02** | Isolamento Multi-Tenant em Aplicação e RLS | **P0** | E02-01 | `NOT_STARTED` | Testes negativos cross-tenant cobrindo 100% dos repositórios e endpoints. |
| **E02-03** | Criptografia de Tokens e Gestão de Segredos | **P0** | E01 | `NOT_STARTED` | Envelope encryption AES-256-GCM para credenciais; segredos fora do Git. |
| **E02-04** | Trilha de Auditoria Imutável | **P0** | E02-01 | `NOT_STARTED` | Eventos de auditoria gravados para ações sensíveis e alterações de conexões. |

---

## 4. Épico E03 — Conexões Oficiais com Marketplaces (P003)

| ID | História / Entrega | Prioridade | Dependência | Status | Critério de Aceite Resumido |
|---|---|---|---|---|---|
| **E03-01** | Conector OAuth 2.0 Mercado Livre | **P0** | E02 | `NOT_STARTED` | Fluxo Authorization Code com PKCE, tokens cifrados e renovação segura. |
| **E03-02** | Conector Shopee Open Platform | **P0** | E02 | `NOT_STARTED` | Autenticação via assinatura HMAC-SHA256 e respeito estrito a quotas. |
| **E03-03** | Resiliência, Backoff e Circuit Breaker | **P0** | E03-01 | `NOT_STARTED` | Tratamento de HTTP 429 Retry-After e isolamento de contas instáveis. |

---

## 5. Épico E04 — Ingestão de Catálogo e Pedidos Próprios (P004)

| ID | História / Entrega | Prioridade | Dependência | Status | Critério de Aceite Resumido |
|---|---|---|---|---|---|
| **E04-01** | Sincronização Incremental de Anúncios Próprios | **P0** | E03 | `NOT_STARTED` | Ingestão idempotente de SKUs, títulos, atributos e estoque. |
| **E04-02** | Ingestão Idempotente de Pedidos e Itens | **P0** | E03 | `NOT_STARTED` | Processamento assíncrono via RabbitMQ; repetição de webhook não duplica pedidos. |

---

## 6. Épico E05 a E12 — Inteligência, Finanças e Release MVP

- **E05 (P005):** Inteligência de Mercado Inicial (Pesquisa com rotulagem `OBSERVED`, sem forjar faturamento de concorrentes).
- **E06 (P006):** Monitoramento de Produtos e Concorrentes (Snapshots históricos a partir da data de inclusão).
- **E07 (P007):** Motor Financeiro Determinístico em `BigDecimal` (Cálculo de margem de contribuição, break-even e taxas versionadas).
- **E08 (P008):** Analytics de Anúncios Próprios (Visitas, conversão e cliques conforme dados oficiais da API).
- **E09 (P009):** Painel Financeiro e DRE Inicial (Consolidação multi-loja e multi-CNPJ com reconciliação de dados ausentes).
- **E10 (P010):** IA Assistiva e RAG Read-Only (Tool calling somente leitura, proibição de mutação e cálculo determinístico delegado ao motor).
- **E11 (P011):** Relatórios e Exportação Auditada (CSV/XLSX com sanitização de fórmulas e auditoria).
- **E12 (P012):** Hardening de Release MVP (Checklist de segurança OWASP ASVS, testes E2E e validação de compliance).
