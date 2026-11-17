# 04 — Organizações, CNPJs, lojas e gestão de acessos

**Fase:** Fase 1 — MVP / P0  
**Pré-requisito:** Prompt 00 concluído e aprovado.  
**Objetivo:** permitir que uma organização mantenha empresas, filiais, contas de marketplace e usuários autorizados com escopo bem definido.

## Contexto
Requisitos FR-ID-003, FR-ID-005 e FR-ID-006. Modelo: tenant → empresas/CNPJs → lojas/contas de marketplace. Uma conta deve estar associada a uma empresa e possuir marketplace e identificador externo quando conectado.

## Escopo obrigatório
- Criar CRUD para empresa/CNPJ, nome fantasia, razão social opcional, regime tributário como configuração informativa e status. Não validar regime fiscal por suposição.
- Criar entidade Store/MarketplaceAccount com marketplace, nome interno, empresa associada, identificador externo, estado de conexão e metadados não secretos.
- Criar convite de usuário com validade, uso único e papel/escopo.
- Implementar permissões por empresa e loja, preservando o limite do tenant.
- Criar UI para listar, cadastrar, editar, desativar e associar empresas/lojas.
- Validar CNPJ como formato/dígitos, sem declarar situação cadastral oficial.
- Auditar alterações de estrutura empresarial, papéis e escopos.

## Tarefas de implementação
- Owner/Admin consegue cadastrar, editar e desativar empresa e loja.
- CNPJ malformado é rejeitado; CNPJ válido em formato não implica situação fiscal regular.
- Conta não pode ser associada a empresa de outro tenant.
- Usuário limitado à Loja A não consulta Loja B por UI/API/exportação.
- Convite expira, não pode ser reutilizado e aplica papel/escopo corretos.
- Mudanças críticas geram eventos de auditoria.
- Testes de integração e E2E cobrem criação, convite, alteração e isolamento.

## Critérios de aceite objetivos
Não consultar Receita Federal nem implementar contabilidade fiscal. Não criar integração real de marketplace.

## Fora do escopo
Prompt 03 aprovado.

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

