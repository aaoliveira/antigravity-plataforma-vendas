# 10 — AI Listing Studio: análise de anúncio e geração de conteúdo

**Fase:** Fase 1 — MVP / P0  
**Pré-requisito:** Prompt 00 concluído e aprovado.  
**Objetivo:** implementar análise explicável de anúncios próprios e geração assistida de títulos/descrições sem inventar características do produto.

## Contexto
Requisitos FR-LST-001, FR-LST-002, FR-LST-003 e FR-LST-011. A IA deve usar provedor configurado, prompt versionado e dados do produto/anúncio acessíveis ao usuário. Sugestão gerada é diferente de fato observado.

## Escopo obrigatório
- Criar interface `AiProvider` e implementação Spring AI para o provedor aprovado, configurado por ambiente.
- Criar prompts versionados em arquivo, schemas de resposta e validação de saída.
- Implementar score de qualidade baseado em critérios explícitos e configuráveis: título, descrição, atributos e imagens disponíveis. Score não pode ser um número sem explicação.
- Gerar sugestões de títulos e descrições a partir apenas de atributos confirmados no cadastro/anúncio.
- Marcar lacunas de informação e solicitar confirmação do usuário quando um atributo for necessário.
- Incluir revisão humana, edição, comparação e histórico de versões.
- Registrar modelo/provedor, versão do prompt, latência e erro, sem guardar dado sensível desnecessário.
- Implementar fallback claro quando credencial/provedor estiver indisponível; não retornar texto inventado como fallback.
- Criar proteção contra prompt injection em conteúdo do anúncio e avaliações: conteúdo externo é dado não confiável.

## Tarefas de implementação
- Saídas são validadas por schema; resposta inválida é tratada como erro e não publicada.
- A IA não cria especificação, certificação, medida, material, garantia, compatibilidade ou promessa que não esteja na fonte confirmada.
- Score exibe os critérios e evidências que contribuíram.
- Usuário pode editar e salvar uma versão sem alterar o anúncio no marketplace.
- Cada versão tem ator, timestamp, prompt/modelo e vínculo ao produto.
- Quando o provedor estiver indisponível, a UI apresenta indisponibilidade e não mostra resposta fabricada.
- Testes adversariais validam prompt injection e vazamento cross-tenant.
- Testes de integração usam fake provider exclusivamente em profile de teste.

## Critérios de aceite objetivos
Não publicar automaticamente anúncios, gerar imagem, clonar integralmente conteúdo de terceiros ou executar teste A/B.

## Fora do escopo
Prompt 09 aprovado; política e provedor de IA aprovados.

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

