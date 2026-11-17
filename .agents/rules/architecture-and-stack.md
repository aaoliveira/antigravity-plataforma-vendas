# Arquitetura e Stack Invariantes

## 1. Stack Técnica Baseline Obrigatória
Toda implementação no repositório deve adotar estritamente o conjunto de tecnologias abaixo, com versões fixadas e verificação oficial de compatibilidade. É terminantemente proibido atualizar versões maiores (*major bumps*) ou utilizar tags `:latest` em imagens e dependências sem validação formal e registro de ADR.

| Camada | Tecnologia Baseline | Papel e Restrições |
|---|---|---|
| **Linguagem Backend** | Java 25 LTS | Runtime LTS atual, tipagem forte, Records e Virtual Threads quando aplicável. |
| **Framework Backend** | Spring Boot 3.5.x | Spring Security, Spring Data JPA, Bean Validation, OpenAPI/Swagger. |
| **Build Backend** | Apache Maven | Gerenciamento de dependências com lock e plugins de análise estática. |
| **Modularidade Backend** | Spring Modulith & ArchUnit | Garantia em tempo de compilação dos limites modulares de domínio. |
| **Frontend Web** | Next.js (App Router) + React + TypeScript | SSR/SPA, design system com Tailwind CSS e shadcn/ui. |
| **Gerenciamento de Estado UI** | TanStack Query (React Query) | Cache assíncrono e sincronização de dados de API. |
| **Formulários e Validação** | React Hook Form + Zod | Schemas estritos compartilhados conceitualmente com backend. |
| **Visualização de Dados** | Recharts | Gráficos determinísticos de vendas, margens, concorrentes e sazonalidade. |
| **Banco Transacional** | PostgreSQL 18.x | Fonte única da verdade transacional. `BigDecimal` / `NUMERIC` para dinheiro. |
| **Cache & Distributed Lock** | Redis 7.x / 8.x | Cache volátil e locks distribuídos. **Nunca** fonte de verdade de domínio. |
| **Mensageria & Filas** | RabbitMQ 4.x | Processamento de jobs assíncronos, DLQ e eventos de domínio. |
| **Gestão de Identidade** | OIDC com Keycloak (local/homologação) | Padrão OAuth 2.0 / OIDC; Spring Security Resource Server no backend. |
| **Armazenamento de Arquivos** | MinIO (local) / S3 compatível | Armazenamento de catálogos PDF, imagens e exportações com URLs pré-assinadas. |
| **Integração com IA** | Spring AI | Interface abstrata `AiProvider`, saída tipada/estruturada, prompts versionados. |
| **Extensão de Navegador** | Manifest V3 + TypeScript + React/Vite | Permissões mínimas (`activeTab`), sem armazenamento inseguro de credenciais. |
| **Qualidade & Testes** | JUnit 5, AssertJ, Mockito, Testcontainers, ArchUnit, Vitest, Playwright | Pirâmide completa de testes com automação rigorosa. |
| **Observabilidade** | OpenTelemetry, Prometheus, Grafana | Logs estruturados JSON com `trace_id`, `span_id` e `tenant_id`. |
| **Ambiente Local** | Docker Compose | Orquestração reproduzível de todos os serviços de apoio. |

---

## 2. Princípios Arquiteturais Obrigatórios

### 2.1 Monólito Modular
- O backend inicia e permanece como um **monólito modular**.
- Não separar microsserviços antecipadamente. A divisão em microsserviços só será considerada mediante gargalos comprovados de escala, isolamento de falhas ou independência de deploy.
- Os domínios comunicam-se via contratos públicos de módulo (Java Interfaces/DTOs ou Eventos de Domínio), verificados por testes de arquitetura com **ArchUnit** e **Spring Modulith**.

### 2.2 Isolamento Rígido de Contextos de Domínio
A arquitetura deve manter os seguintes contextos segregados:
1. `identity`: Usuários, credenciais OIDC, autenticação e recuperação de acesso.
2. `tenancy`: Organizações (tenants), empresas (CNPJs), lojas, filiais, membros e papéis.
3. `marketplace-integration`: Adaptadores de conectores (Mercado Livre, Shopee, Amazon), autenticação OAuth, tokens cifrados e webhooks.
4. `catalog-orders`: Produtos/SKUs internos e pedidos do próprio seller.
5. `market-intelligence`: Pesquisa de palavras-chave, categorias, rankings e tendências públicas.
6. `competitor-intelligence`: Monitoramento de anúncios de concorrentes, alterações de preço e reputação.
7. `product-discovery`: Leitura de catálogos de fornecedores (PDF) e busca por imagem.
8. `listing-studio`: Score de anúncio, análise de títulos e descrições, IA generativa assistida.
9. `financial-control`: Motor financeiro determinístico, custos, comissões, frete, impostos e margem.
10. `ads-intelligence`: Campanhas de publicidade patrocinada (Ads), métricas de ROAS/ACOS/TACOS.
11. `monitoring-alerts`: Regras de alerta, detecção de eventos e envio de notificações.
12. `action-center`: Gestão operacional de tarefas decorrentes de alertas e oportunidades.
13. `reconciliation-reporting`: Conciliação de repasses de marketplaces e DRE gerencial.
14. `ai-assistant-mcp`: Servidor MCP e endpoints conversacionais com ferramentas de leitura/ação explícitas.
15. `audit`: Trilha imutável de eventos de auditoria para ações sensíveis.

### 2.3 Camada de Conectores Marketplace-Agnostic
- As regras de negócio centrais da plataforma **não** conhecem particularidades de marketplaces.
- Cada canal (Mercado Livre, Shopee, etc.) implementa um adaptador específico orientado a uma interface comum de conector (`MarketplaceConnector`).
- Nenhum condicional do tipo `if (marketplace == "MERCADO_LIVRE")` é permitido na lógica central de domínio.

### 2.4 Segregação de Dados Privados vs. Dados de Mercado
- **Dados Privados do Seller:** Pedidos, faturamento, custos de aquisição, clientes e credenciais são confidenciais e protegidos por chave de tenant.
- **Dados de Mercado:** Snapshots públicos de preços, categorias e concorrência são estruturalmente segregados e nunca misturados às métricas privadas do seller.

### 2.5 Multi-Tenancy Estrutural desde o Primeiro Schema
- Toda tabela relacionada a dados de sellers deve conter a coluna `tenant_id NOT NULL`.
- O isolamento deve ser garantido em nível de aplicação (filtros de repositório, validação no service) e reforçado no banco com Row-Level Security (RLS) sempre que aplicável.
- Falhas de isolamento são tratadas como vulnerabilidades críticas de segurança.

### 2.6 Operações Externas Assíncronas, Idempotentes e Resilientes
- Sincronizações de catálogo, ingestão de pedidos, consultas massivas e webhooks rodam assincronamente através de filas RabbitMQ.
- Toda mensagem ou webhook deve possuir chave de idempotência (`idempotency_key` ou `external_id + marketplace`).
- Retries devem utilizar backoff exponencial com jitter e envio para Dead-Letter Queue (DLQ) após o limite de tentativas.

### 2.7 Persistência e Migrações de Banco
- Todo schema e alteração estrutural no PostgreSQL é gerenciado exclusivamente por migrações versionadas do **Flyway** (`V1__...sql`).
- Toda migração deve ser testada e validada em banco limpo via Testcontainers antes da aprovação.
- Índices devem ser planejados para consultas filtradas por `tenant_id`.

### 2.8 Contratos de API, Validação e Erros Padronizados
- Todas as APIs REST expostas pelo backend devem seguir OpenAPI 3.0 / Swagger.
- Entradas devem ser rigorosamente validadas via Bean Validation (`@Valid`, `@NotNull`, `@Size`, etc.).
- Paginação obrigatória em consultas de listas (`page`, `size`, `sort`) com limites máximos de página para evitar DoS.
- Tratamento de erros uniforme seguindo RFC 7807 (Problem Details for HTTP APIs).

### 2.9 Ações Externas Mutáveis com Confirmação e Auditoria
- Qualquer alteração externa em marketplaces (pausa de anúncio, troca de preço, edição de estoque, publicação) exige confirmação explícita humana e registro no log de auditoria com ator, data, dados prévios e retorno da API externa.
