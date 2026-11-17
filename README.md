# Seller Intelligence Platform — Monorepo

Plataforma SaaS multi-tenant para vendedores de e-commerce e marketplaces (Mercado Livre, Shopee e expansões).
Construída com arquitetura de monólito modular orientada a evidências, isolamento multi-tenant estrito e motor financeiro determinístico em `BigDecimal`.

---

## 1. Stack Tecnológica Baseline

| Componente | Tecnologia | Papel / Versão |
|---|---|---|
| **Backend** | Java 25 LTS + Spring Boot 3.5.x | Monólito modular com Spring Modulith, ArchUnit, Flyway e Spring Security. |
| **Frontend Web** | Next.js 15 (App Router) + React 19 + TypeScript | UI com Tailwind CSS, shadcn/ui e Vitest. |
| **Persistência** | PostgreSQL 18.x | Fonte única da verdade transacional. `NUMERIC` para dinheiro e `uuidv7()` nativo. |
| **Cache & Lock** | Redis 7.x / 8.x | Cache volátil e controle de concorrência. |
| **Mensageria** | RabbitMQ 4.x / 3.13-management | Ingestão assíncrona, retries e Dead-Letter Queues (DLQ). |
| **Identidade** | Keycloak 26.x (OIDC) | Provedor de identidade e autenticação OIDC/OAuth 2.0. |
| **Infra Local** | Docker Compose | Orquestração dos serviços de apoio para desenvolvimento. |

---

## 2. Estrutura do Monorepo

```
antigravity-avantepro-clone/
├── .agents/
│   ├── rules/                    # Regras persistentes (anti-hallucination, stack, testing, phase-gates)
│   └── skills/                   # Skills operacionais de automação e phase gate
├── .github/workflows/ci.yml      # Pipeline de Integração Contínua (CI)
├── backend/                      # Monólito modular Spring Boot (Java 25)
│   ├── pom.xml
│   ├── mvnw                      # Maven Wrapper
│   └── src/
├── frontend/                     # Aplicação Next.js 15 (TypeScript / App Router)
│   ├── package.json
│   ├── pnpm-lock.yaml
│   └── src/
├── docs/                         # Documentação viva de produto, arquitetura e entrega
│   ├── product/PRD.md            # Fonte única da verdade de produto
│   ├── architecture/             # DECISION_LOG.md e marketplace-capability-matrix.md
│   └── delivery/                 # PHASE_STATUS.md e phase-gate.json
├── docker-compose.yml            # Serviços locais (Postgres, Redis, RabbitMQ, Keycloak)
├── .env.example                  # Template seguro de variáveis de ambiente
└── prompts/                      # Especificações de cada prompt do roadmap
```

---

## 3. Instruções Rápidas de Execução Local

### 3.1 Pré-requisitos
- **Java 25 LTS** e **Maven 3.9+** (ou use `./mvnw`)
- **Node.js 22+** e **pnpm 10+**
- **Docker** e **Docker Compose**

### 3.2 Subindo os Serviços de Apoio
```bash
# 1. Copie o template de variáveis de ambiente
cp .env.example .env

# 2. Inicie Postgres, Redis, RabbitMQ e Keycloak em background
docker compose up -d

# 3. Verifique a saúde dos serviços
docker compose ps
```

### 3.3 Executando o Backend (Spring Boot)
```bash
cd backend

# Executar testes unitários e de arquitetura (ArchUnit)
./mvnw clean verify

# Iniciar o servidor local (porta 8082)
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```
O backend disponibiliza:
- Health check: [http://localhost:8082/actuator/health](http://localhost:8082/actuator/health)
- Info do sistema: [http://localhost:8082/actuator/info](http://localhost:8082/actuator/info)

### 3.4 Executando o Frontend (Next.js)
```bash
cd frontend

# Instalar dependências
pnpm install

# Executar testes de componentes (Vitest)
pnpm run test

# Executar linter
pnpm run lint

# Iniciar em modo de desenvolvimento (porta 3000)
pnpm run dev
```
Acesse a aplicação técnica em: [http://localhost:3000](http://localhost:3000)

### 3.5 Parando o Ambiente
```bash
docker compose down
```

---

## 4. Segurança, Compliance e Proteção de Contas

A Seller Intelligence Platform implementa uma camada arquitetural avançada de segurança e conformidade operacional orientada à proteção absoluta das contas comerciais dos vendedores:

- **Política FAIL CLOSED:** Toda interação externa com marketplaces passa pelo `MarketplaceComplianceGuard`. Qualquer operação com legitimidade incerta, loja desconectada ou token expirado é sumariamente bloqueada no backend antes de consumir chamadas remotas.
- **Proteção contra Automação Abusiva (Circuit Breaker & 429 Cooldown):** Em caso de HTTP 429 (`Too Many Requests`), a loja é colocada em quarentena temporária baseada no cabeçalho `Retry-After`. Cinco erros consecutivos (HTTP 401, 403 ou 5xx) acionam a abertura do Circuit Breaker (`CIRCUIT_BREAKER_OPEN`), interrompendo requisições em cascata.
- **Governança Human-in-the-Loop para Mutações Críticas:** Operações comerciais que alteram preços, publicam anúncios ou modificam estoque (`OperationRiskLevel.MUTABLE_CRITICAL`) exigem consentimento explícito e aprovação humana do seller (`humanApproved == true`). Mutações autônomas sem supervisão são rejeitadas com `HUMAN_APPROVAL_REQUIRED`.
- **Bloqueio Estrito de Canais Não Homologados (`UNVERIFIED_COMPLIANCE`):** Marketplaces em fase de homologação documental (TikTok Shop, Magalu, SHEIN) permanecem categoricamente bloqueados para produção até a conclusão de parcerias oficiais e concessão de credenciais.
- **Isolamento Multi-Tenant e Sanitização de Credenciais:** Tokens OAuth 2.0 são criptografados com AES-256-GCM em repouso e sistematicamente mascarados com `[REDACTED]` em logs e mensagens de erro.
- **Documentação de Segurança:**
  - Relatório de Auditoria: [docs/security/SECURITY_AUDIT_REPORT.md](docs/security/SECURITY_AUDIT_REPORT.md)
  - Matriz de Conformidade por Canal: [docs/security/MARKETPLACE_COMPLIANCE_MATRIX.md](docs/security/MARKETPLACE_COMPLIANCE_MATRIX.md)
  - Plano de Remediação: [docs/security/SECURITY_REMEDIATION_PLAN.md](docs/security/SECURITY_REMEDIATION_PLAN.md)
  - Relatório de Testes de Segurança: [docs/security/SECURITY_TEST_REPORT.md](docs/security/SECURITY_TEST_REPORT.md)
  - Diretrizes Técnicas de Integração: [docs/security/MARKETPLACE_INTEGRATION_GUIDELINES.md](docs/security/MARKETPLACE_INTEGRATION_GUIDELINES.md)
  - Registro de Decisões de Compliance: [docs/security/COMPLIANCE_DECISIONS.md](docs/security/COMPLIANCE_DECISIONS.md)

---

## 5. Governança e Regras Invariantes
- **Preflight Obrigatório:** Consulte [AGENTS.md](AGENTS.md) antes de cada etapa de desenvolvimento.
- **Hierarquia de Evidências:** `PRD.md` > `DECISION_LOG.md` > Critérios de Aceite > Código/Testes > Docs Oficiais.
- **Anti-Alucinação:** Dados de mercado recebem rótulos estritos (`OBSERVED`, `IMPORTED`, `ESTIMATED`, `UNAVAILABLE`). Proibido inventar dados de concorrentes ou converter indisponíveis em zero. Cálculos monetários exclusivamente com `BigDecimal`.
