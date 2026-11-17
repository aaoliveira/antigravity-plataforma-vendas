# 16 — Ads Intelligence e rentabilidade de campanhas

**Fase:** Fase 2 — após MVP  
**Pré-requisito:** Prompt 00 concluído e aprovado.  
**Objetivo:** importar métricas autorizadas de publicidade e relacioná-las à margem, sem confundir atribuição com causalidade.

## Contexto
Requisitos FR-ADS-001 a FR-ADS-007 e FR-FIN-007. Somente métricas e granularidades suportadas pela API/fonte poderão ser apresentadas.

## Escopo obrigatório
- Integrar fontes de Ads autorizadas identificadas na matriz.
- Importar campanhas, grupos, produtos e métricas com período, moeda, timezone e fonte.
- Implementar fórmulas versionadas para ROAS, ACOS e TACOS e documentar denominadores.
- Exibir impressões, cliques, CTR, CPC e conversões apenas quando fornecidos.
- Associar Ads a SKU conforme chave/atribuição disponível; exibir janela e limitações.
- Cruzar Ads com margem financeira e apresentar resultado após Ads quando houver dados suficientes.
- Criar alertas de metas e recomendações explicáveis, com aprovação humana para qualquer alteração.

## Tarefas de implementação
- Fórmulas passam por testes de referência e são idênticas na API, dashboard e exportação.
- Métrica ausente aparece como indisponível, nunca zero.
- Receita atribuída tem fonte, janela e regra explícitas.
- Campanhas sem associação confiável não são distribuídas arbitrariamente entre SKUs.
- Recomendação contém período, métrica e justificativa.
- Nenhuma campanha é alterada sem confirmação e autorização explícita.

## Critérios de aceite objetivos
Não automatizar lances/orçamentos ou prometer causalidade em testes observacionais.

## Fora do escopo
Prompt 15 aprovado.

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

