# 05 — Framework de conectores e autorização OAuth de marketplaces

**Fase:** Fase 1 — MVP / P0  
**Pré-requisito:** Prompt 00 concluído e aprovado.  
**Objetivo:** implementar uma arquitetura de conectores segura e os fluxos de conexão somente para as capacidades confirmadas na matriz técnica.

## Contexto
Requisitos FR-INT-001, FR-INT-002, FR-INT-006 e FR-INT-009. Mercado Livre e Shopee são os conectores prioritários do MVP; a documentação e permissões reais foram verificadas no Prompt 01. Não deduzir endpoints nem scopes.

## Escopo obrigatório
- Criar interface interna `MarketplaceConnector`/ports com capacidades explícitas para catálogo, anúncios, pedidos, campanhas e eventos; cada capacidade pode ser `SUPPORTED`, `UNSUPPORTED` ou `PENDING_VALIDATION`.
- Implementar registry/factory para Mercado Livre e Shopee, sem espalhar condicionais de marketplace pelos serviços de domínio.
- Implementar OAuth conforme documentação oficial validada: state/CSRF, redirect URI, callback, troca de token, refresh, revogação quando suportada e tratamento de erro.
- Criptografar tokens em repouso com mecanismo configurável; nunca retorná-los ao frontend.
- Registrar scopes consentidos, expiração, conta externa, última validação e estado.
- Implementar rate limit/backoff e timeouts no cliente HTTP.
- Exibir capacidade e estado da integração na UI.
- Criar fake connector somente para perfil `test`; proibir uso no profile de produção.
- Se não houver credenciais reais, concluir testes com sandbox/mock controlado e documentar que o fluxo real aguarda credenciais, sem marcar conexão real como validada.

## Tarefas de implementação
- Testes validam state inválido, callback inválido, token expirado, refresh, revogação e erros externos.
- Segredos são criptografados e mascarados em logs.
- Usuário só conecta/desconecta contas do tenant e escopos autorizados.
- Matriz de capacidades é consultável pela API e interface.
- Conector retorna `UNSUPPORTED` para dados não suportados; não fabrica resposta.
- Integração de produção só recebe estado `CONNECTED` após autorização real confirmada; mocks aparecem explicitamente como TEST.
- Retry/backoff não excede limites documentados e chamadas são rastreáveis.

## Critérios de aceite objetivos
Não sincronizar catálogo/pedidos ainda. Não incluir scraping, CAPTCHA bypass, automação de browser para contornar API nem credenciais de verdade em fixtures.

## Fora do escopo
Prompt 04 aprovado e matriz oficial de API aceita.

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

