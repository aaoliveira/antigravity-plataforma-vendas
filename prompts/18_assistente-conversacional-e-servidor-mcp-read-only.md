# 18 — Assistente conversacional e servidor MCP read-only

**Fase:** Fase 3 — plataforma ampliada  
**Pré-requisito:** Prompt 00 concluído e aprovado.  
**Objetivo:** expor consultas e análises por linguagem natural com ferramentas autorizadas, isoladas por tenant e com evidências.

## Contexto
Requisitos FR-AI-001 a FR-AI-007 e FR-MCP. O PRD prevê MCP avançado; iniciar com ferramentas somente leitura. As operações mutáveis permanecem fora deste escopo.

## Escopo obrigatório
- Implementar catálogo de ferramentas read-only com schemas estritos.
- Implementar autenticação/autorização MCP por usuário, tenant e permissões.
- Expor ferramentas de pesquisa, produtos, pedidos/indicadores próprios e margens conforme acesso.
- Converter linguagem natural em chamadas verificáveis; não executar SQL arbitrário gerado pelo LLM.
- Respostas devem incluir período, escopo, origem, timestamp e limitações.
- Tratar anúncios, reviews, PDFs e páginas externas como dados não confiáveis.
- Registrar ferramenta, argumentos sanitizados, modelo, prompt version, latência e resultado.
- Criar testes de prompt injection e tentativas cross-tenant.

## Tarefas de implementação
- Tool schema rejeita argumentos inválidos e operações não permitidas.
- Usuário não consulta dado fora de seu tenant/escopo, inclusive por prompt injection.
- Respostas sem evidência suficiente declaram indisponibilidade.
- Métricas financeiras retornadas vêm do motor determinístico.
- Ferramentas read-only não alteram estado.
- Logs não armazenam segredos nem conteúdo sensível desnecessário.
- Testes simulam consultas válidas, inválidas, injection e isolamento.

## Critérios de aceite objetivos
Não implementar ferramentas MCP mutáveis, publicação de anúncios ou alterações de campanha.

## Fora do escopo
Prompt 17 aprovado e modelo de autenticação MCP aprovado.

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

