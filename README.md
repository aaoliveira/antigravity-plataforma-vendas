# Seller Intelligence Platform — Monólito Modular & SaaS Multi-Tenant

Plataforma SaaS multi-tenant de inteligência de mercado, analytics financeiro determinístico e gestão para e-commerce (Mercado Livre, Shopee e expansões).  
Construída com arquitetura de **Monólito Modular**, isolamento multi-tenant em profundidade (Application Context + PostgreSQL Row-Level Security), motor financeiro determinístico em `BigDecimal` e governança de release 100% automatizada.

---

## 1. Visão Geral da Arquitetura & Stack Tecnológica

| Camada | Tecnologia Homologada | Versão | Finalidade |
| :--- | :--- | :--- | :--- |
| **Backend Core** | Java SDK / Spring Boot | Java 25 LTS / Spring Boot 3.5.x | Monólito modular com Spring Modulith, Spring Security e Flyway. |
| **Banco de Dados** | PostgreSQL | 17+ Alpine | Banco relacional ACID com Row-Level Security (RLS) forçado por tenant. |
| **Cache & Locks** | Redis | 7.x Alpine | Cache distribuído tenant-scoped e rate limiting. |
| **Mensageria** | RabbitMQ | 3.13 / 4.x Alpine | Ingestão assíncrona com DLQ, retries e cabeçalhos `x-tenant-id`. |
| **Identidade / OIDC** | Keycloak | 26.x | Provedor de identidade, autenticação JWT e gestão de memberships. |
| **Frontend Web** | Next.js / TypeScript | Next.js 15 (App Router) / React 19 / TS 5 | SPA/SSR responsiva com Tailwind CSS e shadcn/ui. |
| **Qualidade & Testes** | JUnit 5 / Vitest / Playwright / Axe | Testcontainers / Vitest 3 / Playwright 1.65 | 783 testes automatizados (unitários, integração, E2E e WCAG 2.1 AA). |

---

## 2. Pré-requisitos de Máquina

Antes de iniciar a configuração local, certifique-se de possuir instalado:

- **Java Development Kit (JDK):** Versão 21 LTS (mínimo) ou **25 LTS** (recomendado para runtime com ZGC).
- **Node.js & Gerenciador de Pacotes:** Node.js **20+** ou **22+ LTS** e **pnpm** versão 9+.
- **Docker & Docker Compose:** Docker Engine 24+ com plugin Docker Compose v2.
- **Git:** Git 2.40+.

Verifique seu ambiente no terminal:
```bash
java -version
node -v
pnpm -v
docker compose version
```

---

## 3. Configuração e Execução no Ambiente Local (Desenvolvimento)

Siga os passos abaixo para configurar e subir toda a aplicação e suas dependências localmente.

### Passo 1: Clonar o Repositório
```bash
git clone https://github.com/seu-usuario/antigravity-avantepro-clone.git
cd antigravity-avantepro-clone
```

### Passo 2: Configurar o Arquivo de Variáveis de Ambiente Local
Copie o template de desenvolvimento:
```bash
cp .env.example .env
```
O arquivo `.env` já vem pré-configurado com as credenciais padrão dos serviços de apoio para desenvolvimento local.

### Passo 3: Inicializar Serviços de Apoio via Docker Compose
Suba os contêineres do PostgreSQL, Redis, RabbitMQ e Keycloak em segundo plano:
```bash
docker compose up -d
```

Verifique se todos os serviços estão saudáveis (`healthy`):
```bash
docker compose ps
```

#### Mapeamento de Portas e Painéis Locais
| Serviço | Porta Local | Credenciais Padrão (Dev) | Acesso / Painel |
| :--- | :--- | :--- | :--- |
| **PostgreSQL** | `5432` | `postgres` / `postgres_dev_password` | `localhost:5432/seller_intelligence` |
| **Redis** | `6379` | `redis_dev_password` | `localhost:6379` |
| **RabbitMQ** | `5672` (AMQP) / `15672` (UI) | `guest` / `guest` | [http://localhost:15672](http://localhost:15672) |
| **Keycloak** | `8085` | `admin` / `admin_dev_password` | [http://localhost:8085](http://localhost:8085) |

### Passo 4: Inicializar o Backend (Spring Boot)
O backend utiliza o Maven Wrapper (`mvnw`) e executa migrações de schema com Flyway automaticamente na inicialização:
```bash
cd backend
./mvnw clean spring-boot:run
```
* O backend iniciará na porta `8082` sob o perfil `local`.
* **Healthcheck do Actuator:** [http://localhost:8082/actuator/health](http://localhost:8082/actuator/health)
* **Documentação Swagger / OpenAPI:** [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html)

### Passo 5: Inicializar o Frontend (Next.js)
Em outro terminal, instale as dependências e inicie o servidor de desenvolvimento:
```bash
cd frontend
pnpm install
pnpm dev
```
* Acesse a aplicação web no navegador: [http://localhost:3000](http://localhost:3000).

---

## 4. Execução de Testes Automatizados e Garantia da Qualidade

O projeto possui **783 testes automatizados** distribuídos entre backend e frontend:

### 4.1 Testes do Backend (509 Testes Unitários, Integração e RLS)
Os testes utilizam **Testcontainers** para subir instâncias reais do PostgreSQL com Row-Level Security:
```bash
cd backend
./mvnw test
```

### 4.2 Testes Unitários e de Integração do Frontend (260 Testes Vitest)
```bash
cd frontend
pnpm test
```

### 4.3 Testes End-to-End e Acessibilidade WCAG (14 Testes Playwright)
Executa cenários completos em navegadores reais em dois viewports (Desktop 1280x800 e Mobile 375x667):
```bash
cd frontend
# Caso necessário instalar o binário do Chromium na primeira execução:
npx playwright install chromium

# Executar a suíte Playwright completa:
pnpm run test:e2e
```

---

## 5. Configuração e Disponibilização para Ambiente Produtivo

Para disponibilizar a **Seller Intelligence Platform** em ambiente de produção (VPS, servidores dedicados, Kubernetes ou Cloud Providers como AWS/GCP/Azure), siga as diretrizes e procedimentos abaixo.

### 5.1 Princípios Obrigatórios de Segurança (OWASP ASVS & LGPD)
1. **Segredos Fora do Repositório:** Jamais commite arquivos `.env`, chaves privadas, senhas ou tokens no Git. Utilize segredos injetados por variáveis de ambiente ou Secrets Manager (ex: AWS Secrets Manager, HashiCorp Vault).
2. **Defesa em Profundidade:**
   - Todas as tabelas sensíveis no PostgreSQL possuem políticas de **Row-Level Security (RLS)** ativas.
   - O backend opera com `SET LOCAL app.current_tenant = '<uuid>'` por transação.
3. **Criptografia em Repouso:** Tokens OAuth dos marketplaces são cifrados em banco com algoritmo **AES-256-GCM**.
4. **Sanitização de Logs:** O Logback está configurado para mascarar automaticamente credenciais, tokens JWT, PII (CPF, e-mails, endereços) e dados de compradores.

---

### 5.2 Configuração de Variáveis de Ambiente de Produção

Gere as chaves criptográficas de 256 bits via OpenSSL:
```bash
# Gerar chave JWT Secret (64 caracteres hexadecimais)
openssl rand -hex 32

# Gerar chave mestre de encriptação AES-256-GCM para tokens OAuth (64 caracteres hexadecimais)
openssl rand -hex 32
```

Crie o arquivo `.env.prod` baseado no template fornecido [`.env.prod.example`](.env.prod.example):
```bash
cp .env.prod.example .env.prod
chmod 600 .env.prod
```

Preencha os valores críticos em `.env.prod`:
```ini
# Banco de Dados de Produção
POSTGRES_DB=seller_intelligence
POSTGRES_USER=seller_prod_user
POSTGRES_PASSWORD=<SENHA_FORTE_GERADA_COM_MINIMO_32_CHARS>

# Cache Redis
REDIS_PASSWORD=<SENHA_FORTE_REDIS>

# Message Broker RabbitMQ
RABBITMQ_USER=seller_rabbit
RABBITMQ_PASSWORD=<SENHA_FORTE_RABBITMQ>

# Chaves Criptográficas
JWT_SECRET=<CHAVE_HEX_32_BYTES_GERADA_PELO_OPENSSL>
TOKEN_ENCRYPTION_KEY=<CHAVE_HEX_32_BYTES_GERADA_PELO_OPENSSL>

# Integrações Oficiais Homologadas
MELI_APP_ID=<APP_ID_MERCADO_LIVRE>
MELI_CLIENT_SECRET=<CLIENT_SECRET_MERCADO_LIVRE>
MELI_REDIRECT_URI=https://app.seudominio.com.br/api/v1/integrations/mercadolivre/callback

SHOPEE_PARTNER_ID=<PARTNER_ID_SHOPEE>
SHOPEE_PARTNER_KEY=<PARTNER_KEY_SHOPEE>
SHOPEE_REDIRECT_URL=https://app.seudominio.com.br/api/v1/integrations/shopee/callback
SHOPEE_SANDBOX=false
SHOPEE_ENABLED=true

# URLs e CORS Públicos
NEXT_PUBLIC_API_URL=https://app.seudominio.com.br
CORS_ALLOWED_ORIGINS=https://app.seudominio.com.br
```

---

### 5.3 Deploy via Docker Compose de Produção (Turnkey)

O projeto inclui imagens multi-stage otimizadas e um arquivo [`docker-compose.prod.yml`](docker-compose.prod.yml) pronto para orquestrar todos os serviços em contêineres independentes e usuários não-root:

1. **Subir toda a stack em background:**
   ```bash
   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
   ```

2. **Verificar status dos serviços:**
   ```bash
   docker compose -f docker-compose.prod.yml ps
   ```

3. **Acompanhar logs:**
   ```bash
   # Logs do backend
   docker compose -f docker-compose.prod.yml logs -f backend

   # Logs do frontend
   docker compose -f docker-compose.prod.yml logs -f frontend
   ```

4. **Executar migrações de banco:**
   O backend com perfil `prod` aciona automaticamente o Flyway contra as pastas `classpath:db/migration` e `classpath:db/migration-postgresql`, aplicando as tabelas e políticas de Row-Level Security de forma idempotente.

---

### 5.4 Build das Imagens Docker para Orquestração (Kubernetes / ECS / Cloud Run)

Caso você utilize um registro de contêineres privado (ex.: Amazon ECR, Google Artifact Registry, Docker Hub):

#### 1. Build e Publicação da Imagem do Backend
```bash
docker build -t seu-registry/seller-backend:1.0.0 -f backend/Dockerfile backend/
docker push seu-registry/seller-backend:1.0.0
```
* **Base Runtime:** `eclipse-temurin:25-jre-alpine`
* **Usuário:** Não-root (`appuser`)
* **Parâmetros de JVM recomendados:** `-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0`

#### 2. Build e Publicação da Imagem do Frontend
```bash
docker build -t seu-registry/seller-frontend:1.0.0 -f frontend/Dockerfile frontend/
docker push seu-registry/seller-frontend:1.0.0
```
* **Base Runtime:** `node:22-alpine` (Next.js Standalone Server)
* **Usuário:** Não-root (`nextjs`)
* **Porta:** `3000`

---

### 5.5 Configuração de Proxy Reverso, TLS/HTTPS e Domínio (Nginx / Cloudflare)

Em produção, os serviços devem ficar vinculados internamente a `127.0.0.1` e expostos através de um proxy reverso com certificado SSL/TLS (HTTPS).

Exemplo de configuração para o **Nginx** (`/etc/nginx/sites-available/seller-intelligence.conf`):

```nginx
# Redirecionamento HTTP -> HTTPS
server {
    listen 80;
    listen [::]:80;
    server_name app.seudominio.com.br;
    return 301 https://$host$request_uri;
}

# Servidor Seguro HTTPS
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name app.seudominio.com.br;

    # Certificados SSL (Let's Encrypt / Certbot)
    ssl_certificate /etc/letsencrypt/live/app.seudominio.com.br/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/app.seudominio.com.br/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # Headers de Segurança Mandatórios (OWASP)
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;

    # Roteamento da API Backend
    location /api/ {
        proxy_pass http://127.0.0.1:8082/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_connect_timeout 60s;
        proxy_read_timeout 60s;
    }

    # Roteamento dos Healthchecks do Actuator
    location /actuator/health {
        proxy_pass http://127.0.0.1:8082/actuator/health;
        proxy_set_header Host $host;
    }

    # Roteamento da Aplicação Web Frontend (Next.js)
    location / {
        proxy_pass http://127.0.0.1:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Ativação do certificado via Certbot:
```bash
sudo certbot --nginx -d app.seudominio.com.br
sudo systemctl reload nginx
```

---

### 5.6 Monitoramento, Observabilidade e Healthchecks em Produção

- **Probes de Liveness / Readiness:**
  - `GET /actuator/health/liveness` $\rightarrow$ Valida se o processo Java está ativo.
  - `GET /actuator/health/readiness` $\rightarrow$ Valida conectividade com PostgreSQL e componentes de persistência.
- **Métricas Prometheus:**
  - `GET /actuator/prometheus` $\rightarrow$ Coleta de métricas RED (Rate, Errors, Duration), contagem de threads virtuais e consumo de heap.
- **Superfície de Ataque Reduzida:**
  - No perfil `prod`, o Swagger UI e especificações OpenAPI são desabilitados automaticamente (`springdoc.api-docs.enabled: false`).
  - O Actuator oculta detalhes de exceções e configurações internas (`show-details: never`).

---

## 6. Documentação de Engenharia e Governança

Para detalhes aprofundados sobre arquitetura, políticas de marketplaces, runbooks e decisões técnicas, consulte a pasta [`docs/`](docs/):

| Documento | Descrição / Finalidade |
| :--- | :--- |
| [docs/PROJECT_CONTEXT.md](docs/PROJECT_CONTEXT.md) | Visão de produto, contexto de mercado, personas e modelo de negócio. |
| [docs/PRD.md](docs/PRD.md) | Product Requirements Document — Fonte da verdade de requisitos funcionais e não-funcionais. |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Diagramas de componentes, monólito modular e limites de domínio. |
| [docs/SECURITY.md](docs/SECURITY.md) | Diretrizes OWASP ASVS Nível 2, isolamento multi-tenant, RLS e criptografia. |
| [docs/TECH_STACK.md](docs/TECH_STACK.md) | Matriz oficial de versões, bibliotecas homologadas e justificativas. |
| [docs/MARKETPLACE_POLICIES.md](docs/MARKETPLACE_POLICIES.md) | Políticas oficiais de APIs do Mercado Livre e Shopee, limites e rate limiting. |
| [docs/ENGINEERING_RULES.md](docs/ENGINEERING_RULES.md) | As 15 regras invioláveis, salvaguardas financeiras e Definition of Done. |
| [docs/DECISION_LOG.md](docs/DECISION_LOG.md) | Architecture Decision Records (ADRs) homologados. |
| [docs/delivery/PHASE_STATUS.md](docs/delivery/PHASE_STATUS.md) | Histórico de avaliação formal e aprovação de todos os gates de release. |
| [docs/runbooks/E2E_PIPELINE_EXECUTION.md](docs/runbooks/E2E_PIPELINE_EXECUTION.md) | Runbook operacional de reprodução do pipeline de ponta a ponta. |

---

## 7. Status do Projeto

- **Fase Atual:** MVP Completo Homologado (Prompts P000 a P023-FE) $\rightarrow$ **`APPROVED / 100% GREEN`**
- **Cobertura de Testes:** 783 testes automatizados (509 Backend + 260 Frontend Vitest + 14 Frontend Playwright).
- **Licença:** Proprietária / Comercial &middot; Todos os direitos reservados.
