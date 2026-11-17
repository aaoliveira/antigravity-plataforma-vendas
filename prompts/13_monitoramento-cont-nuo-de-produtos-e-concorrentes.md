# 13 — Monitoramento contínuo de produtos e concorrentes

**Fase:** Fase 2 — após MVP  
**Pré-requisito:** Prompt 00 concluído e aprovado.  
**Objetivo:** adicionar histórico temporal e alertas sobre mudanças em produtos/concorrentes usando somente fontes e frequências aprovadas.

## Contexto
Requisitos FR-CMP-002 a FR-CMP-008 e FR-MON-001 a FR-MON-005. A qualidade do histórico depende de cobertura e permissões da fonte.

## Escopo obrigatório
- Criar watchlists de produtos, anúncios e vendedores por tenant.
- Coletar snapshots periódicos apenas por conectores autorizados.
- Registrar alterações de preço, status e atributos observáveis.
- Criar comparação temporal sem interpolar lacunas silenciosamente.
- Exibir origem, cobertura, frequência e falhas de coleta.
- Criar configuração de limiares e alertas deduplicados.
- Implementar retenção/particionamento de snapshots conforme ADR.

## Tarefas de implementação
- Item pode ser adicionado/removido de watchlist e histórico não é apagado acidentalmente.
- Snapshots preservam timestamp e fonte.
- Alteração detectada gera evento reproduzível e deduplicado.
- Lacunas de coleta aparecem no gráfico/relatório.
- Alertas respeitam tenant e regras configuradas.
- Testes simulam alteração, ausência de coleta e retry.

## Critérios de aceite objetivos
Não afirmar vendas/estoque real de concorrente sem fonte.

## Fora do escopo
Prompt 12 aprovado e escopo fase 2 aprovado.

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

