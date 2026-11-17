# 12 — Homologação, segurança, E2E e release do MVP

**Fase:** Fase 1 — MVP / Gate de lançamento  
**Pré-requisito:** Prompt 00 concluído e aprovado.  
**Objetivo:** provar que as jornadas críticas do MVP funcionam de ponta a ponta e que segurança, observabilidade, recuperação e documentação estão adequadas para homologação.

## Contexto
Critérios globais do PRD, Definition of Done, NFR, SLOs e riscos. Esta etapa não adiciona módulos novos; corrige lacunas encontradas nos testes.

## Escopo obrigatório
- Criar suíte E2E Playwright para: cadastro/login, criação de tenant/empresa, convite e RBAC, conexão em ambiente de teste, sincronização, pesquisa com fonte, cálculo financeiro, dashboard/exportação e AI Listing Studio.
- Executar testes de isolamento tenant adversariais, RBAC, validação, rate limit e segurança de secrets.
- Revisar OWASP Top 10 relevante, CORS, CSRF, SSRF, injection, IDOR, upload, logs e headers.
- Adicionar testes de carga básicos e medir endpoints p95 dentro do ambiente definido.
- Validar backup e restauração do PostgreSQL; documentar RPO/RTO observados.
- Verificar retry, dead-letter, reprocessamento e idempotência de jobs.
- Validar logs, métricas, traces, health checks e alertas de falha de integração.
- Gerar SBOM/relatório de dependências vulneráveis conforme tooling disponível.
- Revisar documentação de deploy, configuração de secrets e runbooks.
- Corrigir todos os defeitos bloqueantes e de severidade alta antes de marcar o MVP como pronto.

## Tarefas de implementação
- Todas as jornadas P0 E2E passam em ambiente limpo com dados de teste identificados.
- Testes cross-tenant/RBAC passam sem vazamento.
- Não há secrets em Git nem dados pessoais/financeiros em logs.
- Backup e restauração foram executados e registrados.
- Jobs e conectores demonstram retry, dead-letter e recuperação.
- Build, testes unitários, integração, lint, typecheck, build frontend e E2E passam no CI.
- Vulnerabilidades críticas/altas conhecidas estão corrigidas ou formalmente aceitas pelo responsável com mitigação e prazo; nenhuma é escondida.
- Limitações de APIs e cobertura de dados são mostradas na UI/documentação.
- Relatório de homologação contém evidências, defeitos restantes, riscos aceitos e decisão de GO/NO-GO.

## Critérios de aceite objetivos
Não iniciar fase 2 ou adicionar recursos novos durante a homologação.

## Fora do escopo
Prompts 02 a 11 aprovados.

## Entregáveis obrigatórios
- Código e testes relacionados ao escopo.
- Documentação técnica/operacional atualizada.
- Evidências dos comandos e testes executados.
- Atualização do mapa PRD → requisito → implementação → teste.
- Atualização do gate em `docs/delivery/PHASE_STATUS.md`.


### PROTOCOLO OBRIGATÓRIO DE EXECUÇÃO E GATE (repetir antes de cada prompt)

Antes de alterar qualquer arquivo:
1. Releia `AGENTS.md`/`GEMINI.md`, todos os arquivos imediatamente dentro de `.agents/rules/`, a skill `.agents/skills/mvp-phase-gate/SKILL.md`, o PRD em `docs/product/PRD.md`, os ADRs, `OPEN_QUESTIONS.md`, `DECISION_LOG.md` e `docs/delivery/PHASE_STATUS.md`.
2. Resuma no plano de execução as regras herdadas, o requisito/IDs do PRD atendidos, o que está fora do escopo e os critérios de aceite deste prompt.
3. Verifique o gate do prompt anterior. Só prossiga se o status for `APPROVED` e os testes obrigatórios estiverem registrados com evidência. Se estiver `BLOCKED`, `IN_PROGRESS`, ausente ou inconsistente, NÃO implemente este prompt: apresente os bloqueios e solicite a correção/validação do prompt anterior.
4. Produza primeiro um plano curto com arquivos/módulos que serão alterados, riscos, migrações, testes e comandos. Não comece mudanças fora desse plano.
5. Implemente somente o escopo deste prompt. Não antecipe funcionalidades de prompts futuros e não faça refatorações amplas sem necessidade comprovada.
6. Execute os testes relevantes e, quando aplicável, lint, typecheck, build, migrações em banco limpo e teste E2E. Não declare sucesso se algum comando falhar ou não tiver sido executado.
7. Atualize documentação, rastreabilidade de requisitos e `docs/delivery/PHASE_STATUS.md`. Registre evidências, commit/hash se disponível, comandos executados, resultados e limitações.
8. Marque `APPROVED` somente quando TODOS os critérios de aceite deste prompt forem atendidos. Caso contrário, marque `BLOCKED`, liste falhas e não avance.

**Regra de encadeamento:** o próximo prompt da sequência só pode ser aceito/executado quando o atual estiver `APPROVED`. Se o usuário enviar o prompt seguinte prematuramente, recuse a execução de código e apresente o relatório de gate pendente. Não interprete a simples existência de arquivos como prova de sucesso; valide o comportamento por testes.

