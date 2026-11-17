# 02 — Bootstrap do monorepo e ambiente local

**Fase:** Fase 1 — MVP / Fundação técnica  
**Pré-requisito:** Prompt 00 concluído e aprovado.  
**Objetivo:** criar a estrutura executável do monorepo, com serviços mínimos, dependências locais e automação de qualidade.

## Contexto
Stack aprovada: Java 21, Spring Boot 3.5.x, Maven, Next.js/React/TypeScript, PostgreSQL 18, Redis, RabbitMQ 4.x, Keycloak OIDC e Docker Compose. O desenho inicial é monólito modular com frontends separados e workers no backend.

## Escopo obrigatório
- Criar a estrutura de diretórios do repositório conforme a stack do PRD.
- Criar serviço Spring Boot com endpoints `/actuator/health` e `/actuator/info`, configuração por profiles e tratamento de erro inicial.
- Criar aplicação Next.js com layout base, página inicial técnica e chamada ao health endpoint sem dados fictícios.
- Criar Docker Compose para PostgreSQL, Redis, RabbitMQ e Keycloak; MinIO apenas se necessário para o MVP.
- Configurar `.env.example` sem segredos; validar variáveis obrigatórias na inicialização.
- Configurar Maven Wrapper, pnpm lockfile, ESLint, formatter, TypeScript strict e testes básicos.
- Criar pipeline CI para build/test/lint dos módulos existentes.
- Criar convenções de pacote, nomes, logs estruturados e configuração de CORS.
- Criar README com comandos reais para subir, testar e parar o ambiente.

## Tarefas de implementação
- O projeto compila e inicia localmente via instruções documentadas.
- `docker compose config` valida sem erro.
- PostgreSQL, Redis, RabbitMQ e Keycloak ficam saudáveis no ambiente local.
- Backend responde health UP e frontend carrega sem erro de console.
- Build/test backend e lint/typecheck/build frontend passam em CI.
- Nenhum segredo está versionado; `.env` está ignorado e `.env.example` contém apenas placeholders.
- Migração Flyway inicial cria somente infraestrutura mínima, sem entidades de negócio prematuras.
- Instruções foram executadas do zero em ambiente limpo ou containerizado.

## Critérios de aceite objetivos
Não criar domínios de seller, endpoints de marketplace, tabelas funcionais ou dashboards. Não criar microsserviços nem Kubernetes.

## Fora do escopo
Prompt 01 aprovado e ADRs da stack aceitos.

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

