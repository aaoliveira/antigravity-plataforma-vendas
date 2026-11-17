# Instruções permanentes do workspace — Seller Intelligence Platform

Este repositório implementa a Seller Intelligence Platform conforme `docs/product/PRD.md`. Antes de cada tarefa, releia o PRD, todos os arquivos `.agents/rules/*.md`, a skill `.agents/skills/mvp-phase-gate/SKILL.md`, os ADRs, `OPEN_QUESTIONS.md`, `DECISION_LOG.md` e `docs/delivery/PHASE_STATUS.md`.

A fonte da verdade é o PRD e decisões aprovadas. Não invente APIs, campos, dados, regras fiscais, métricas, permissões ou resultados de testes. Se faltar evidência, registre questão aberta e pare o trabalho dependente. Trabalhe somente no prompt/escopo atual, planeje antes de editar, teste antes de aprovar e bloqueie o próximo prompt enquanto o gate atual não estiver `APPROVED`.

Stack baseline: Java 21/Spring Boot 3.5.x, Maven, Next.js App Router/React/TypeScript, PostgreSQL 18, Redis, RabbitMQ 4.x, OIDC/Keycloak, Docker Compose. Não atualize major nem altere arquitetura sem ADR aprovado. Consulte os arquivos de regras para invariantes completas.