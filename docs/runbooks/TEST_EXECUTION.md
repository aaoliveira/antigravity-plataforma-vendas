# Runbook: Execução de Testes Automatizados e Quality Gates

> **Código:** `RB-TEST-001` &middot; **Versão:** 1.0.0  
> **Finalidade:** Roteiro padrão de execução das suítes de teste e validação de gates.  

---

## 1. Testes do Backend (Java / Spring Boot)

### 1.1 Testes Unitários e de Arquitetura (ArchUnit)
Executa testes rápidos isolados de I/O e valida limites modulares:
```bash
cd backend
./mvnw clean test
```

### 1.2 Testes de Integração com Testcontainers
Inicializa contêineres Docker efêmeros de PostgreSQL, Redis e RabbitMQ para validar migrações Flyway e repositórios:
```bash
cd backend
./mvnw verify -Dtest=*IntegrationTest
```

### 1.3 Testes Específicos de Isolamento Cross-Tenant
Valida que nenhuma consulta vaza dados entre tenants diferentes:
```bash
cd backend
./mvnw test -Dtest=MultiTenantSecurityIntegrationTest
```

---

## 2. Testes do Frontend (Next.js / Vitest / Playwright)

### 2.1 Testes Unitários e de Componentes (Vitest)
Executa a suíte de testes de componentes React, hooks e formulários:
```bash
cd frontend
pnpm run test
```

### 2.2 Verificação de Tipos e Linter
```bash
cd frontend
pnpm run lint
pnpm run build
```

### 2.3 Testes End-to-End (Playwright)
Executa jornadas completas de usuário em navegadores headless:
```bash
cd frontend
pnpm run test:e2e
```

---

## 3. Varredura de Segredos e Dependências

Antes de cada commit, execute a verificação estática para garantir que nenhum segredo foi exposto acidentalmente:
```bash
git diff --cached | grep -iE 'api[_-]?key|secret|password|token' || echo "Nenhum padrão suspeito encontrado."
```
