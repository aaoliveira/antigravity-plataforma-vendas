# Gating sequencial dos prompts

- Cada prompt é uma unidade independente com gate próprio.
- Antes de iniciar, reler regras, skill, PRD, ADRs, OPEN_QUESTIONS, DECISION_LOG e status.
- O prompt seguinte só pode ser executado quando o anterior estiver APPROVED e todos os critérios tiverem evidência.
- BLOCKED/IN_PROGRESS/ausente/inconsistente => não implementar; relatar bloqueios.
- Marcar APPROVED somente após testes, build/lint/typecheck relevantes, inspeção de resultados e atualização documental.
- Não aceitar que a existência de código substitua teste de comportamento.
- Não avançar funcionalidades de fases futuras para dentro do MVP sem decisão humana e ADR.