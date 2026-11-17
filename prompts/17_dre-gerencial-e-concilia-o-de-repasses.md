# 17 — DRE gerencial e conciliação de repasses

**Fase:** Fase 2 — após MVP  
**Pré-requisito:** Prompt 00 concluído e aprovado.  
**Objetivo:** construir visão gerencial de resultado e conciliar pedidos, eventos financeiros e valores recebidos.

## Contexto
Requisitos FR-FIN-008, FR-REC-001 a FR-REC-005. DRE gerencial não é declaração contábil/fiscal oficial; bases caixa/competência precisam ser explícitas.

## Escopo obrigatório
- Importar extratos/repasses por API ou arquivo autorizado.
- Relacionar repasse, pedido e evento financeiro com método e confiança.
- Detectar divergências, pendências, taxas e itens não identificados.
- Criar fluxo de revisão manual e registro de contestação/protocolo sem afirmar envio se não suportado.
- Implementar DRE gerencial com categorias configuráveis e opção de caixa/competência se decisões de negócio aprovadas.
- Versionar classificação e preservar trilha de ajustes.

## Tarefas de implementação
- Relatório separa conciliado, pendente, divergente e não identificado.
- Divergência mostra esperado, recebido, diferença, fonte e evidência.
- Importação duplicada não duplica repasses.
- Ajustes manuais são auditados.
- DRE informa base de reconhecimento, fórmulas, período e origem.
- Testes reconciliam dataset conhecido e cobrem estorno, taxa e repasse parcial.

## Critérios de aceite objetivos
Não substituir ERP/contabilidade nem gerar obrigações fiscais.

## Fora do escopo
Prompt 16 aprovado.

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

