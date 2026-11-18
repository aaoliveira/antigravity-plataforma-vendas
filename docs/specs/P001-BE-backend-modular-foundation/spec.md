# Especificação Técnica: P001-BE — Fundação do Backend Modular

> **Identificador da Feature:** `P001-BE` &middot; **Versão:** 1.0.0  
> **Status da Spec:** Aprovada para Implementação  
> **Requisitos do PRD:** MVP-01, RF-001, RNF-005, RNF-007, RNF-008, RNF-012  
> **Padrão Arquitetural:** Monólito Modular com Spring Modulith, ArchUnit e Flyway  

---

## 1. Contexto e Objetivo

Implementar a base arquitetural do backend da **Seller Intelligence Platform** em Java 25 LTS e Spring Boot 3.5.x como um **Monólito Modular** com limites de domínio rígidos, build reproduzível, gerenciamento de perfis (`local`, `test`, `prod`), rastreamento distribuído via correlation ID, tratamento de erros padronizado (RFC 7807), documentação OpenAPI 3.0, migração inicial Flyway e suíte de testes de arquitetura (ArchUnit).

---

## 2. Escopo Obrigatório

1. **Configuração Maven Reproduzível:**
   - Criação da pasta `backend/` com `pom.xml` fixando dependências e versões oficiais.
   - Inclusão do Maven Wrapper (`mvnw`, `.mvn/wrapper/`).
2. **Definição dos 17 Módulos de Domínio (Boundaries Vazios):**
   - Criação dos pacotes modulares em `com.sellerintelligence.modules.*`:
     - `identityaccess`
     - `tenancy`
     - `marketplaceconnection`
     - `marketplaceadapters`
     - `catalog`
     - `orderingestion`
     - `marketintelligence`
     - `competitorresearch`
     - `productmonitoring`
     - `pricing`
     - `profitability`
     - `financialanalytics`
     - `listinganalytics`
     - `aiassistant`
     - `reporting`
     - `notification`
     - `audit`
   - Cada módulo contém seu marcador de pacote (`package-info.java`) ou classe exportadora pública sem vazamento de repositórios.
3. **Gestão de Perfis de Configuração:**
   - `application.yml`: Configurações comuns, propriedades de aplicação e actuator.
   - `application-local.yml`: Conexão com containers locais do `docker-compose.yml`.
   - `application-test.yml`: Configuração com banco in-memory H2 compatível com PostgreSQL ou Testcontainers para testes isolados.
   - `application-prod.yml`: Configurações seguras com Fail-Closed ativo e secrets externos.
4. **Filtro de Correlation ID & Logging:**
   - `CorrelationIdFilter`: Intercepta toda requisição HTTP, extrai o header `X-Correlation-Id` ou gera um identificador único, injetando no MDC do SLF4J (`correlation_id`) e no header da resposta.
   - Padrão de log estruturado contendo timestamp, nível, correlation_id e mensagem.
5. **Tratamento Padronizado de Erros (RFC 7807):**
   - `GlobalExceptionHandler` anotado com `@RestControllerAdvice` estendendo `ResponseEntityExceptionHandler`.
   - Retornos de erro estruturados como `ProblemDetail` com campos: `type`, `title`, `status`, `detail`, `instance`, `timestamp` e `correlation_id`.
6. **Observabilidade & Actuator:**
   - Endpoints expostos estritamente: `health`, `info`, `metrics`, `prometheus`.
   - Proibição de exposição de endpoints administrativos perigosos (`env`, `restart`, `shutdown`, `beans` públicos).
   - Suporte a probes do Kubernetes: `/actuator/health/liveness` e `/actuator/health/readiness`.
7. **Documentação Viva de API (OpenAPI 3.0):**
   - Configuração do Springdoc OpenAPI com título, descrição, termos e versão em `/v3/api-docs` e `/swagger-ui.html`.
8. **Migração Inicial Flyway:**
   - Script `db/migration/V1__init_schema.sql` criando as tabelas iniciais de governança: `organizations` e `app_metadata`.
9. **Testes de Arquitetura (ArchUnit):**
   - `ArchitectureTests`: Valida que nenhum módulo de domínio acessa classes internas (`internal`) de outro módulo e impede dependências cíclicas.
   - `ApplicationContextTest`: Valida a inicialização limpa do contexto Spring Boot com profile `test`.
10. **Infraestrutura Local (Docker Compose):**
    - `docker-compose.yml` na raiz contendo PostgreSQL 17, Redis 7, RabbitMQ 3.13 e Keycloak 26.

---

## 3. Não-Escopo (O Que NÃO Será Implementado Neste Prompt)

- Implementação de regras de negócio de cálculo de comissão, margem ou lucros.
- Endpoints funcionais de pesquisa ou integração real com APIs do Mercado Livre ou Shopee.
- Implementação de telas frontend (pertence ao P001-FE).
- Criação de credenciais ou chaves hardcoded no código.

---

## 4. Cenários Gherkin de Validação

```gherkin
Funcionalidade: Inicialização e Verificação da Fundação do Backend

  Cenário: Inicialização do Contexto Spring Boot e Health Check
    Dado que o ambiente de teste está configurado com o profile "test"
    Quando a aplicação Spring Boot é inicializada
    Então o contexto deve carregar com sucesso sem erros
    E o endpoint "/actuator/health" deve retornar status HTTP 200 com "status": "UP"
    E os endpoints de liveness e readiness devem estar ativos

  Cenário: Geração do Schema OpenAPI 3.0
    Dado que a aplicação está em execução
    Quando uma requisição GET é enviada para "/v3/api-docs"
    Então o status HTTP deve ser 200
    E o payload retornado deve ser um JSON válido contendo a especificação OpenAPI 3.0
    E deve conter as informações de metadados da Seller Intelligence Platform

  Cenário: Propagação de Correlation ID nas Requisições
    Dado que um cliente envia uma requisição HTTP com o header "X-Correlation-Id: test-correlation-123"
    Quando a requisição é processada
    Então o header "X-Correlation-Id" da resposta deve conter "test-correlation-123"
    E o MDC de log deve conter o correlation_id correspondente

  Cenário: Tratamento Padronizado de Erros RFC 7807
    Dado que um cliente requisita um recurso inexistente ou envia payload inválido
    Quando o erro é disparado
    Então a resposta HTTP deve ter Content-Type "application/problem+json"
    E deve conter os campos "title", "status", "detail" e "correlation_id"

  Cenário: Integridade de Arquitetura com ArchUnit
    Dado o conjunto de módulos do monólito em "com.sellerintelligence.modules"
    Quando o teste de arquitetura ArchUnit é executado
    Então não deve haver ciclos de dependência entre pacotes
    E nenhum módulo pode acessar pacotes internos de outro módulo diretamente

  Cenário: Execução da Migração Inicial Flyway
    Dado um banco de dados limpo
    Quando a aplicação inicia e o Flyway é acionado
    Então a migração "V1__init_schema.sql" deve ser aplicada com sucesso
    E a tabela "organizations" deve existir com as colunas esperadas
```

---

## 5. Matriz de Rastreabilidade

| Requisito PRD | Descrição | Componente Técnico | Teste Automatizado |
|---|---|---|---|
| **MVP-01** | Fundação estrutural, migrações e observabilidade | `pom.xml`, Spring Boot, Flyway, Actuator | `ApplicationContextTest`, `ActuatorHealthTest` |
| **RF-001** | Isolamento e identidade confiável | `V1__init_schema.sql` (organizations) | `FlywayMigrationTest` |
| **RNF-005** | Resiliência e tratamento de erros | `GlobalExceptionHandler` (RFC 7807) | `GlobalExceptionHandlerTest` |
| **RNF-007** | Observabilidade e logs estruturados | `CorrelationIdFilter`, Logback JSON pattern | `CorrelationIdFilterTest` |
| **RNF-008** | Manutenibilidade e arquitetura modular | 17 módulos de domínio, Spring Modulith | `ModularArchitectureTest` (ArchUnit) |
| **RNF-012** | Portabilidade e infraestrutura local | `docker-compose.yml` (Postgres, Redis, RabbitMQ) | Validação do compose config |
