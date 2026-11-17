# 07 — Market Intelligence: pesquisa e análise de produtos

**Fase:** Fase 1 — MVP / P0  
**Pré-requisito:** Prompt 00 concluído e aprovado.  
**Objetivo:** entregar pesquisa de mercado baseada somente em dados disponíveis por fontes autorizadas ou dados importados, exibindo proveniência e limites.

## Contexto
Requisitos FR-MKT-001 a FR-MKT-004 e FR-MKT-008. O usuário pesquisa produto/termo/URL/identificador conforme a matriz de capacidades. A aplicação deve separar mercado de dados privados do seller.

## Escopo obrigatório
- Criar domínio `MarketIntelligence` isolado de `SellerOperations`.
- Implementar pesquisa por palavra-chave e, se suportado, URL/ID de anúncio, utilizando somente conectores/fontes aprovados.
- Implementar filtros que estejam realmente disponíveis na fonte: categoria, preço, marca, avaliação e sinais de demanda. Campos não fornecidos devem aparecer como indisponíveis.
- Criar modelo de snapshot de resultado com source, collectedAt, marketplace, classification, coverage e confidence/methodology quando estimado.
- Exibir detalhes do anúncio/produto e rótulos OBSERVED/IMPORTED/ESTIMATED/AI_GENERATED/UNAVAILABLE.
- Permitir salvar pesquisas, produtos e notas por tenant.
- Criar paginação, ordenação, filtros combináveis e estados de carregamento/vazio/erro.
- Se nenhuma fonte oficial/permitida fornecer pesquisa de mercado pública, implementar apenas adaptador/importação que foi aprovada e sinalizar limitação; NÃO coletar via scraping proibido.

## Tarefas de implementação
- Pesquisa válida retorna resultados da fonte aprovada com origem e data de captura.
- Usuário consegue ver filtros aplicados e limpar/alterar a pesquisa.
- Dado indisponível não é representado como zero.
- Estimativa sempre mostra método/limitação e nunca é rotulada como venda real.
- Produtos salvos são isolados por tenant e podem ser reabertos.
- Testes cobrem resultados parciais, nenhuma correspondência, fonte indisponível e filtros.
- Não há endpoint que exponha dados privados de outro tenant no domínio de mercado.

## Critérios de aceite objetivos
Não implementar ranking preditivo, sazonalidade, busca visual, monitoramento contínuo ou scanner de fornecedor.

## Fora do escopo
Prompt 06 aprovado e fontes de pesquisa aprovadas no Prompt 01.

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

