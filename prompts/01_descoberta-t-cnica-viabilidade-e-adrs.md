# 01 — Descoberta técnica, viabilidade e ADRs

**Fase:** Fase 0 — descoberta  
**Pré-requisito:** Prompt 00 aprovado.  
**Objetivo:** validar as dependências técnicas que podem bloquear o MVP antes da implementação.

## Contexto
O PRD lista questões abertas, especialmente permissões, escopos e capacidades de APIs de Mercado Livre e Shopee; origem lícita de dados competitivos; regras de cálculo financeiro; retenção LGPD; IA e extensão. A implementação depende de evidências oficiais, não de suposições.

## Escopo obrigatório
- Auditar OQ-001 a OQ-015 e classificar bloqueios P0/P1/P2.
- Validar documentação oficial atual de Mercado Livre e Shopee (OAuth, catálogo, anúncios, pedidos, webhooks, Ads e limites), registrando URL, data, escopos e restrições.
- Validar política/termos relevantes para extensão e acesso a dados públicos.
- Criar matriz de capacidades por marketplace: disponível via API, escopo necessário, indisponível, requer aprovação/credencial ou ainda não confirmado.
- Rever stack e compatibilidade das versões escolhidas.
- Definir estratégia para dev/test sem credenciais reais e para homologação com credenciais do usuário.

## Tarefas de implementação
- Criar `docs/architecture/marketplace-capability-matrix.md`.
- Criar ADRs para stack, monólito modular, multi-tenancy, integrações, proveniência de dados e motor financeiro.
- Atualizar `OPEN_QUESTIONS.md` somente quando houver evidência e responsável/decisão; não marcar resolvida sem aprovação explícita.
- Definir lista de endpoints permitidos e escopos apenas quando verificados em fonte oficial.
- Apresentar riscos de dependência externa e plano de contingência por importação CSV ou indisponibilidade.
- Não escrever integração de produção neste prompt.

## Critérios de aceite objetivos
- Todas as capacidades que entrarão no MVP têm fonte oficial registrada ou estão explicitamente marcadas como não confirmadas.
- Cada endpoint/escopo mencionado está associado à documentação oficial consultada e à data de consulta.
- Dados competitivos sem fonte autorizada estão excluídos do MVP ou classificados como pendência bloqueante.
- A matriz diferencia dados próprios, dados públicos observáveis, dados licenciados, estimativas e dados indisponíveis.
- ADRs estão escritos, numerados, com contexto, decisão, alternativas, consequências e estado `PROPOSED` ou `ACCEPTED`.
- Nenhuma OQ crítica foi encerrada por inferência do agente.

## Fora do escopo
Não implementar APIs, telas, autenticação, OAuth real ou sincronização. O objetivo é reduzir incerteza e preparar decisões.

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

