# 19 — Expansão de conectores para Amazon e outros marketplaces

**Fase:** Fase 3 — expansão  
**Pré-requisito:** Prompt 00 concluído e aprovado.  
**Objetivo:** adicionar novos canais sem alterar os contratos de domínio e sem assumir capacidades não verificadas.

## Contexto
O PRD lista Amazon, TikTok Shop, Magalu e SHEIN como expansão. Cada canal depende de API, acesso regional, credenciais e permissões.

## Escopo obrigatório
- Revisar documentação oficial atual e preencher matriz de capacidades antes de qualquer código.
- Implementar adaptador do marketplace aprovado mantendo o contrato interno.
- Implementar OAuth/credenciais, sincronização suportada, health, rate limit, retries e idempotência.
- Criar testes de contrato por adaptador e documentação de campos suportados/não suportados.
- Ativar recursos apenas quando credencial/autorização e homologação estiverem disponíveis.

## Tarefas de implementação
- Novo conector passa nos testes de contrato.
- Campos não suportados não são inventados nem zerados.
- Conector não altera comportamento dos canais existentes; suíte de regressão passa.
- Tokens são criptografados e segregados por tenant/conta.
- Matriz e docs do conector são aprovadas antes da ativação em produção.

## Critérios de aceite objetivos
Não implementar vários marketplaces simultaneamente sem validação e autorização específicas.

## Fora do escopo
Prompt 18 aprovado ou decisão de roadmap autoriza expansão de conectores.

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

