# Stack Tecnológica Oficial e Baseline de Versões

> **Versão:** 1.0.0 &middot; **Status:** Baseline Aprovado  
> **Data de Homologação:** 17/11/2026  
> **Política de Versões:** Versões fixadas, compatibilidade verificada oficialmente, licenças permissivas e proibição de tags `:latest`.  

---

## 1. Matriz de Componentes e Versões Homologadas

### 1.1 Backend Core (Java & Spring Boot)

| Componente | Tecnologia | Versão Fixada | Licença | Finalidade / Justificativa |
|---|---|---|---|---|
| **Linguagem / SDK** | Java SDK | **21 LTS** (Compilação) / **25 LTS** (Runtime) | GPLv2+CE / BSL | Suporte de longo prazo (LTS), Virtual Threads (`Project Loom`), Records e Pattern Matching. |
| **Framework Base** | Spring Boot | **4.1.x** (ou **3.5.x LTS**) | Apache 2.0 | Framework enterprise oficial para monólito modular, microsserviços e integração cloud. |
| **Segurança & OIDC** | Spring Security | Compatível com Spring Boot | Apache 2.0 | Resource Server OAuth 2.0 / JWT, controle RBAC estrito e headers de segurança HTTP. |
| **Persistência ORM** | Spring Data JPA / Hibernate | 6.x / 7.x (Managed) | LGPL 2.1 / Apache 2.0 | Mapeamento relacional com suporte a tipos determinísticos e dialect PostgreSQL. |
| **Modularidade** | Spring Modulith | 1.3+ / 2.0+ | Apache 2.0 | Verificação em tempo de compilação de limites de pacotes e eventos de domínio internos. |
| **Arquitetura de Teste** | ArchUnit | **1.3.0+** | Apache 2.0 | Testes automatizados para impedir violações de camadas e imports cruzados de módulos. |
| **Migrações de Banco** | Flyway Community | **10.x / 11.x** | Apache 2.0 | Versionamento de schema SQL determinístico, idempotente e reproduzível. |
| **Validação de Entrada** | Bean Validation (Hibernate Validator) | 8.x / 9.x | Apache 2.0 | Validação declarativa de DTOs e entidades (`@NotNull`, `@Size`, `@Pattern`, RFC 7807). |
| **Mapeamento DTO** | MapStruct | **1.6.x** | Apache 2.0 | Mapeador DTO $\leftrightarrow$ Entidade de alta performance sem reflexão em runtime. |
| **Contratos de API** | Springdoc-OpenAPI | **2.8.x** | Apache 2.0 | Geração automática de especificações OpenAPI 3.0 e documentação Swagger. |
| **Mensageria** | Spring AMQP / RabbitMQ Client | 3.x / 4.x (Managed) | Apache 2.0 | Comunicação assíncrona, retries com backoff, dead-letter exchanges e serialização JSON. |

---

### 1.2 Frontend Web (Next.js & TypeScript)

| Componente | Tecnologia | Versão Fixada | Licença | Finalidade / Justificativa |
|---|---|---|---|---|
| **Framework Web** | Next.js | **15.x** (App Router) | MIT | Framework React com Server-Side Rendering (SSR), compilação veloz (Turbopack) e rotas tipadas. |
| **Biblioteca de UI** | React | **19.x** | MIT | Camada de renderização declarativa e hooks modernos de concorrência. |
| **Linguagem Frontend** | TypeScript | **5.x** | Apache 2.0 | Tipagem estrita (`strict: true`), contratos compartilhados com DTOs e prevenção de bugs. |
| **Estilização** | Tailwind CSS | **3.4.x / 4.x** | MIT | Utility-first CSS com suporte a design responsivo (mobile-first) e modo escuro/claro. |
| **Componentes Base** | shadcn/ui + Radix UI | Componentes Primitivos | MIT | Acessibilidade garantida (WCAG 2.2 AA), navegação por teclado e design limpo. |
| **Gerenciamento de Estado de API** | TanStack Query (React Query) | **5.x** | MIT | Cache cliente, revalidação em background, deduplicação de requisições e paginação. |
| **Formulários & Schemas** | React Hook Form + Zod | RHF 7.x + Zod 3.x | MIT | Validação de formulários no cliente com schemas TypeScript reutilizáveis. |
| **Visualização de Dados** | Recharts | **2.x** | MIT | Gráficos SVG responsivos de margens, concorrência, sazonalidade e linha do tempo de preços. |
| **Testes Unitários UI** | Vitest + Testing Library | Vitest 3.x | MIT | Execução ultrarrápida de testes de componentes isolados com DOM virtual. |
| **Testes End-to-End** | Playwright | **1.49+** | Apache 2.0 | Automação de testes em navegadores reais (Chromium, Firefox, WebKit) em modo headless. |

---

### 1.3 Infraestrutura, Dados e Observabilidade

| Componente | Tecnologia | Versão Fixada | Imagem Docker Oficial | Justificativa Técnica |
|---|---|---|---|---|
| **Banco Transacional** | PostgreSQL | **18.x / 17.x** | `postgres:17-alpine` | Fonte única da verdade ACID, suporte a `NUMERIC` exato para finanças e Row-Level Security (RLS). |
| **Cache & Locks** | Redis | **7.4 / 8.x** | `redis:7-alpine` | Cache em memória de curta duração, controle de concorrência distribuída e rate limiting. |
| **Broker de Mensagens** | RabbitMQ | **4.x / 3.13** | `rabbitmq:3.13-management-alpine` | Filas assíncronas com garantia de entrega, roteamento flexível e painel de gestão. |
| **Identidade / OIDC** | Keycloak | **26.x** | `quay.io/keycloak/keycloak:26.1` | Provedor de identidade local homologado, suporte a OIDC, tokens JWT e RBAC. |
| **Object Storage** | MinIO (Local) / AWS S3 | Release Estável | `minio/minio:RELEASE.2024-11-07` | Armazenamento privado de catálogos PDF, imagens e exportações via URLs pré-assinadas. |
| **Métricas** | Prometheus | **2.55+** | `prom/prometheus:v2.55.1` | Coleta de telemetria e métricas de taxa de requisições, latência e erros (RED). |
| **Visualização** | Grafana | **11.x** | `grafana/grafana:11.3.0` | Dashboards operacionais de infraestrutura, saúde dos adaptadores e monitoramento de filas. |
| **Tracing Distribuído**| OpenTelemetry | Collector v0.114+ | `otel/opentelemetry-collector:0.114.0` | Rastreamento distribuído com `trace_id` propagado entre requisições HTTP e filas AMQP. |

---

## 2. Política de Dependências e Licenciamento

1. **Licenças Permitidas:**
   - Estritamente autorizadas: **MIT**, **Apache 2.0**, **BSD-2-Clause**, **BSD-3-Clause**, **ISC**.
   - Licenças com restrições condicionais: **LGPL 2.1** (permitida apenas como biblioteca dinâmica/JAR em dependências padrão do framework como Hibernate).
   - Licenças categoricamente **proibidas**: **GPLv3**, **AGPLv3** ou licenças com cláusulas comerciais restritivas (*Commons Clause*, *SSPL*) em bibliotecas incluídas na distribuição.
2. **Proibição de Tags `:latest`:** Todas as imagens Docker em `docker-compose.yml`, Dockerfiles e manifestos de Kubernetes devem apontar para tags de versão semântica exatas ou hashes SHA-256.
3. **Bloqueio de Dependências Não Verificadas:** Qualquer nova biblioteca requer análise prévia de CVEs conhecidos via OWASP Dependency-Check / Snyk antes da adoção.
