# 09 — Painel financeiro, rentabilidade e relatórios do MVP

**Fase:** Fase 1 — MVP / P0  
**Pré-requisito:** Prompt 00 concluído e aprovado.  
**Objetivo:** apresentar ao seller os resultados do motor financeiro e indicadores de vendas de forma filtrável, consistente e exportável.

## Contexto
Requisitos FR-DAS-001, FR-DAS-002, FR-DAS-005 e requisitos financeiros P0. A interface deve usar os mesmos contratos/consultas do backend para tela e exportação.

## Escopo obrigatório
- Criar dashboard com faturamento importado, quantidade de pedidos, ticket médio e margem de contribuição, respeitando disponibilidade dos dados.
- Criar visão por produto/SKU, empresa, loja e marketplace.
- Implementar filtros de período e comparação de períodos equivalentes.
- Mostrar aviso de cobertura/incompletude e data da última sincronização.
- Criar tabela de pedidos/produtos com drill-down para memória de cálculo.
- Implementar exportação CSV de relatório filtrado com timezone, data de geração, origem e filtros.
- Criar estados vazios, dados incompletos, erro e loading.
- Garantir que totalizadores da tela e exportação usam o mesmo serviço de domínio.

## Tarefas de implementação
- Filtros por empresa, loja, marketplace e período afetam todos os cards e tabelas de forma consistente.
- Usuário sem permissão não vê dado financeiro por UI, API ou exportação.
- Soma dos itens/pedidos corresponde ao total do relatório segundo a regra documentada.
- Comparação utiliza períodos equivalentes e sinaliza períodos incompletos.
- CSV abre corretamente, respeita filtros/permissões e contém metadados de origem.
- Não há confusão entre faturamento, receita líquida, margem de contribuição e lucro líquido.
- Playwright valida a jornada principal e os testes de API cobrem filtros e autorização.

## Critérios de aceite objetivos
Não incluir DRE completo, Curva ABC, Ads atribuído nem conciliação aprofundada.

## Fora do escopo
Prompt 08 aprovado.

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

