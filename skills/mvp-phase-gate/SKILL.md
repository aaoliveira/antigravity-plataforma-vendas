---
name: mvp-phase-gate
description: Execute uma etapa de desenvolvimento da Seller Intelligence Platform de forma incremental, rastreável e testada. Use antes e durante cada prompt numerado para verificar regras, PRD, decisões, dependências e gate anterior.
---

# Skill — MVP Phase Gate

## Objetivo
Assegurar que cada prompt resulte em uma entrega pequena, verificável e rastreável, sem avançar quando a etapa anterior não estiver aprovada.

## Preflight obrigatório
1. Leia `AGENTS.md`, todos os `.agents/rules/*.md`, o PRD, ADRs, `OPEN_QUESTIONS.md`, `DECISION_LOG.md` e `docs/delivery/PHASE_STATUS.md`.
2. Verifique o `phase-gate.json`. Se o prompt anterior não estiver `APPROVED`, pare.
3. Resuma requisitos, escopo, não-escopo e critérios de aceite.
4. Liste ambiguidades; não invente resolução.

## Plano
- Liste módulos e arquivos a alterar.
- Liste riscos, migrações, testes e comandos.
- Mantenha as mudanças limitadas ao prompt.

## Execução
- Implemente código e testes juntos.
- Prefira alterações pequenas e reversíveis.
- Não use dados/mocks de teste em produção.
- Não registre segredos nem PII em logs.
- Atualize rastreabilidade e documentação.

## Verificação
- Execute unit, integration, lint, typecheck, build e E2E aplicáveis.
- Registre comandos e resultados reais.
- Investigue toda falha; não ignore teste flaky sem registrar/corrigir.
- Revise diff e migrações.

## Gate
- `APPROVED`: todos os critérios foram comprovados.
- `BLOCKED`: existe critério falho, fonte ausente, decisão pendente ou teste não executado.
- Nunca marcar aprovado por intenção, mock de produção ou implementação incompleta.
- O prompt seguinte não pode iniciar antes de `APPROVED`.
