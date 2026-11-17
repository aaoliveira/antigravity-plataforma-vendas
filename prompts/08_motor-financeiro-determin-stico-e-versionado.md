# 08 — Motor financeiro determinístico e versionado

**Fase:** Fase 1 — MVP / P0  
**Pré-requisito:** Prompt 00 concluído e aprovado.  
**Objetivo:** implementar cálculos de margem, preço mínimo e simulações por SKU/pedido usando componentes configuráveis, precisão decimal e memória de cálculo.

## Contexto
Requisitos FR-FIN-001 a FR-FIN-006, FR-FIN-010 e FR-FIN-011. Regras tributárias e tarifárias dependem de configuração fornecida/validada; não presumir regime, tarifa, comissão ou frete de um canal.

## Escopo obrigatório
- Criar modelo de custo por SKU com moeda, unidade, origem, vigência e histórico.
- Criar modelo versionado de componentes de tarifa, comissão, logística/frete, embalagem, tributos parametrizados, descontos e outros custos variáveis.
- Implementar domínio de cálculo usando `BigDecimal`, escala e arredondamento explicitamente definidos.
- Implementar cálculo de margem de contribuição por item e pedido; preço mínimo e preço-alvo.
- Criar memória de cálculo explicando cada componente, fonte e regra aplicada.
- Marcar cálculo `COMPLETE`, `PARTIAL` ou `SIMULATED`; listar campos ausentes e premissas.
- Tratar cancelamento, devolução, estorno e desconto conforme política de reconhecimento documentada.
- Versionar regras com vigência; não recalcular históricos silenciosamente.
- Criar testes de referência com casos numéricos aprovados pelo responsável de produto/financeiro.
- IA não participa da execução da fórmula.

## Tarefas de implementação
- Casos de referência financeiros passam com valores esperados e arredondamento exato.
- Mesma entrada + mesma versão de regra produz resultado idêntico.
- Atualizar regra não altera cálculo histórico fechado sem ação explícita e auditada.
- Cálculo parcial indica componentes ausentes e não se apresenta como lucro líquido real.
- Cancelamento/devolução/estorno não duplica ou mantém receita incorretamente conforme política configurada.
- Não existe uso de `float`/`double` em cálculos monetários.
- Testes de unidade cobrem limites, valores negativos, arredondamento, moeda e vigência.
- Memória de cálculo é reproduzível e exportável.

## Critérios de aceite objetivos
Não implementar apuração fiscal oficial, emissão de nota, consultoria tributária ou cálculo de imposto com alíquota presumida.

## Fora do escopo
Prompt 07 aprovado e OQ-004/OQ-005 endereçadas com regras explícitas aprovadas.

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

