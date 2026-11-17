# Arquitetura e stack invariantes

- Monólito modular no MVP; não criar microsserviços/Kubernetes sem ADR e evidência.
- Backend Java 25 LTS + Spring Boot 3.5.x/Maven; módulos por domínio e contratos explícitos.
- Frontend Next.js App Router + React + TypeScript; extensão MV3 TypeScript.
- PostgreSQL é fonte da verdade; Redis é cache; RabbitMQ para jobs/eventos assíncronos.
- Flyway para migrações; OpenAPI para contratos; Docker Compose para local.
- Multi-tenancy é estrutural: tenant em queries, constraints, autorização e testes.
- Conectores são adapters por marketplace, não condicionais espalhadas no domínio.
- Financeiro usa BigDecimal, regras versionadas e memória de cálculo.
- Ações externas mutáveis exigem confirmação humana, autorização e auditoria.
- Fixar versões; não usar `latest` em produção.