# 14 — Smart Alerts e Action Center

**Fase:** Fase 2 — após MVP  
**Pré-requisito:** Prompt 00 concluído e aprovado.  
**Objetivo:** transformar eventos e análises confiáveis em alertas operacionais e tarefas rastreáveis.

## Contexto
Requisitos FR-ACT-001 a FR-ACT-006. Alertas devem apontar evidência, origem, severidade e contexto.

## Escopo obrigatório
- Implementar catálogo de regras para alerta de margem, preço, sincronização, queda de indicador e estoque quando fonte suportar.
- Permitir configurar limiar, janela, escopo e canal de notificação.
- Criar inbox de alertas com estados novo, lido, resolvido e ignorado.
- Converter alerta/recomendação em tarefa com responsável, prazo, prioridade e evidência.
- Auditar mudanças e resultados.
- Implementar controle de frequência, deduplicação e opt-in de notificações.

## Tarefas de implementação
- Regra testada com evento simulado gera alerta no escopo correto.
- Alertas repetidos são deduplicados na janela configurada.
- Tarefa herda link para alerta e evidência.
- Usuário não pode atribuir tarefa fora de seu escopo.
- Canais sem consentimento não recebem notificação.
- Histórico e auditoria permitem rastrear criação e conclusão.

## Critérios de aceite objetivos
Não executar ações externas em marketplaces automaticamente.

## Fora do escopo
Prompt 13 aprovado.

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

