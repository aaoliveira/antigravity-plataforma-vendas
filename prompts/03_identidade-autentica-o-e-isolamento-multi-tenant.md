# 03 — Identidade, autenticação e isolamento multi-tenant

**Fase:** Fase 1 — MVP / P0  
**Pré-requisito:** Prompt 00 concluído e aprovado.  
**Objetivo:** implementar autenticação e o limite de segurança entre organizações desde a primeira entidade de domínio.

## Contexto
Requisitos PRD: FR-ID-001 a FR-ID-004 e FR-ID-007. O tenant é a fronteira primária de isolamento. A autorização deve ser validada no backend, jamais apenas na interface.

## Escopo obrigatório
- Integrar Spring Security com OIDC/Keycloak usando Authorization Code + PKCE para fluxo browser apropriado.
- Implementar identidade local do usuário vinculada ao `sub` OIDC; não armazenar senha se o IdP for responsável pela autenticação.
- Criar entidade Organization/Tenant e vínculo membership usuário-organização com papel Owner/Admin/Manager/Analyst/Finance/Operator/Viewer.
- Criar contexto autenticado que resolva tenant ativo e escopo do usuário em cada request.
- Implementar autorização por tenant e preparar escopo empresa/loja para prompts seguintes.
- Adotar filtro global/repositórios que exijam tenant; avaliar PostgreSQL RLS como defesa adicional.
- Implementar auditoria para criação/alteração de membros, papéis e acesso negado.
- Padronizar respostas 401/403 sem revelar existência de recursos de outros tenants.
- Criar testes automatizados de isolamento para acesso direto, listagem, busca, alteração e exportação.

## Tarefas de implementação
- Usuário pode autenticar e encerrar sessão com OIDC no ambiente de teste.
- Usuário sem sessão recebe 401 em endpoints protegidos.
- Usuário de tenant A não consegue ler, listar, alterar ou inferir registros de tenant B; testes negativos passam.
- Papel sem permissão recebe 403.
- Toda query de domínio exige tenant context e testes de arquitetura impedem acesso sem escopo.
- Auditoria registra ator, tenant, ação, alvo, timestamp e resultado sem token/segredo.
- Testes de integração rodam com PostgreSQL via Testcontainers.

## Critérios de aceite objetivos
Não implementar integração de marketplace, financeiro ou funcionalidades administrativas avançadas.

## Fora do escopo
Prompt 02 aprovado.

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

