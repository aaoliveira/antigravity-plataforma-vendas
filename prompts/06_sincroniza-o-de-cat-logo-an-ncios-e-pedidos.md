# 06 — Sincronização de catálogo, anúncios e pedidos

**Fase:** Fase 1 — MVP / P0  
**Pré-requisito:** Prompt 00 concluído e aprovado.  
**Objetivo:** importar dados próprios das contas autorizadas com rastreabilidade, idempotência e mecanismo alternativo de CSV.

## Contexto
Requisitos FR-INT-003, FR-INT-004, FR-INT-006, FR-INT-007 e FR-INT-008. Só sincronizar recursos e campos marcados como suportados na matriz do Prompt 01.

## Escopo obrigatório
- Criar modelo normalizado de produto/SKU, anúncio/listing e pedido/order, preservando IDs externos e payload de origem minimizado quando permitido.
- Criar jobs assíncronos de sincronização usando RabbitMQ, status, checkpoints, retry exponencial e dead-letter.
- Garantir idempotência por marketplace + conta + external ID + tipo de recurso.
- Processar webhooks somente se documentados e habilitados; verificar assinatura e deduplicar eventos.
- Criar CSV import para catálogo/pedidos com template documentado, validação por linha e relatório de erros.
- Armazenar origem, timestamp de extração, última atualização e qualidade dos campos.
- Criar tela de status de sincronização, histórico, itens importados, falhas e reprocessamento permitido.
- Implementar paginação da API externa, limites, retomada de cursor e reconciliação de contagens.

## Tarefas de implementação
- Duas execuções do mesmo lote não duplicam produtos, anúncios ou pedidos.
- Eventos repetidos e fora de ordem não corrompem estado; testes cobrem idempotência.
- Falha temporária é retentada com política definida; falha definitiva vai para dead-letter com diagnóstico.
- CSV válido importa; linhas inválidas são rejeitadas com número da linha e motivo; nenhum registro inválido entra parcialmente.
- Registros importados guardam marketplace, conta, tenant e timestamps de origem.
- Dados ausentes permanecem ausentes, não são preenchidos com zero ou strings inventadas.
- Usuário não acessa sincronizações de outro tenant.
- Dashboard de jobs mostra sucesso/falha e última sincronização.

## Critérios de aceite objetivos
Não importar concorrentes, Ads ou repasses nesta etapa.

## Fora do escopo
Prompt 05 aprovado.

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

