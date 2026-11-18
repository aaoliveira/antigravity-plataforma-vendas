# Runbook: Preparação e Inicialização do Ambiente Local

> **Código:** `RB-ENV-001` &middot; **Versão:** 1.0.0  
> **Finalidade:** Guia determinístico para setup e execução do ambiente de desenvolvimento.  

---

## 1. Pré-requisitos de Máquina

Certifique-se de que sua estação de trabalho possui as ferramentas abaixo instaladas e configuradas no `PATH`:

```bash
# 1. Verificar Java (Java 25 LTS)
java -version

# 2. Verificar Node.js (Node 22 LTS ou superior)
node -v

# 3. Verificar pnpm (pnpm 9 ou superior)
pnpm -v

# 4. Verificar Docker e Docker Compose v2
docker compose version
```

---

## 2. Configuração de Variáveis de Ambiente

Copie o template de desenvolvimento e revise os valores locais:
```bash
cp .env.example .env
```
* **Aviso Mandatório:** Nunca commite o arquivo `.env` preenchido nem adicione segredos de produção ao repositório.

---

## 3. Inicialização dos Serviços de Apoio (Docker Compose)

Suba os serviços locais (PostgreSQL, Redis, RabbitMQ e Keycloak) em background:
```bash
docker compose up -d
```

Verifique se todos os contêineres atingiram status `healthy`:
```bash
docker compose ps
```

### Mapeamento de Portas Locais
| Serviço | Porta Externa | Credencial Padrão (Dev) | Painel Web / Dashboard |
|---|---|---|---|
| **PostgreSQL** | `5432` | `postgres / postgres_dev_password` | `localhost:5432/seller_intelligence` |
| **Redis** | `6379` | `redis_dev_password` | --- |
| **RabbitMQ** | `5672` (AMQP) | `guest / guest` | [http://localhost:15672](http://localhost:15672) |
| **Keycloak** | `8085` | `admin / admin_dev_password` | [http://localhost:8085](http://localhost:8085) |

---

## 4. Inicialização do Backend (Spring Boot)

Após a implementação dos módulos no Prompt P001:
```bash
cd backend
./mvnw clean spring-boot:run
```
O backend estará disponível em `http://localhost:8082` com telemetria em `/actuator/health`.

---

## 5. Inicialização do Frontend (Next.js)

```bash
cd frontend
pnpm install
pnpm run dev
```
Acesse a aplicação em `http://localhost:3000`.
