# 11 — Extensão Chrome Manifest V3 complementar

**Fase:** Fase 1 — MVP / P0  
**Pré-requisito:** Prompt 00 concluído e aprovado.  
**Objetivo:** criar a extensão mínima para conectar a experiência do navegador à plataforma com permissões mínimas e sem coleta não autorizada.

## Contexto
Requisito de extensão e FR-LST/MKT pertinentes. A extensão é uma interface auxiliar; não substitui os conectores oficiais nem contorna políticas do marketplace.

## Escopo obrigatório
- Criar extensão Manifest V3 em TypeScript/React/Vite com popup, service worker e content script somente onde necessário.
- Implementar autenticação segura por fluxo suportado, sem persistir access token em storage inseguro. Avaliar uso de sessão via backend e armazenamento mínimo.
- Criar ação explícita do usuário para enviar à plataforma o URL/ID atualmente selecionado, quando a página e política permitirem.
- Restringir host permissions aos domínios estritamente necessários e pedir permissão opcional quando possível.
- Não coletar cookies, conteúdo oculto, dados de sessão ou informações de outros usuários.
- Não automatizar cliques, login, compra, alteração de preço ou publicação.
- Exibir status da conexão e abrir o detalhe do item na aplicação web.
- Documentar instalação local/desenvolvimento e processo de build.

## Tarefas de implementação
- Extensão compila e carrega em Chrome/Chromium com Manifest V3.
- Permissões declaradas são mínimas e justificadas no README.
- Usuário precisa iniciar explicitamente o compartilhamento de URL/ID.
- Nenhum cookie ou token de sessão do marketplace é lido/transmitido.
- Usuário não autenticado recebe orientação de autenticação segura.
- Erros de comunicação com backend são apresentados sem expor dados sensíveis.
- Testes validam parsing de URL/IDs suportados, mensagens entre componentes e permissões.
- Revisão manual confirma que não existe scraping proibido ou escrita automática em marketplace.

## Critérios de aceite objetivos
Não implementar extensão de análise invasiva, captura automática contínua ou automação de ações no marketplace.

## Fora do escopo
Prompt 10 aprovado e revisão de política da extensão concluída.

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

