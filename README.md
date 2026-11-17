# Pacote de prompts — Antigravity | Seller Intelligence Platform

**Versão:** 1.0  
**Data:** 17/11/2026  
**Objetivo:** transformar o PRD da Seller Intelligence Platform em uma sequência incremental de prompts para Google Antigravity, com stack definida, regras persistentes, critérios de aceite e bloqueio de avanço até validação.

## Arquivos

- `prompts/`: prompts numerados individualmente. Comece obrigatoriamente pelo `00`.
- `rules/`: conteúdo inicial de regras persistentes para o workspace.
- `skills/mvp-phase-gate/SKILL.md`: procedimento reutilizável de gate.

## Como preparar o workspace

1. Crie/abra a pasta do projeto no Antigravity.
2. Copie o PRD original para `docs/product/PRD.md`, preservando o conteúdo integral.
3. Copie `rules/AGENTS.md` para a raiz do repositório como `AGENTS.md`.
4. Copie os demais arquivos de `rules/` para `.agents/rules/`.
5. Copie `skills/mvp-phase-gate/` para `.agents/skills/mvp-phase-gate/`.
6. Envie o conteúdo completo de `prompts/00_...md` ao agente. Não comece pelo Prompt 01.
7. Só envie o prompt seguinte depois de o Antigravity marcar o anterior como `APPROVED` e apresentar as evidências.

## Como operar no Antigravity

Use o modo de planejamento antes de cada etapa. Revise o plano e os artefatos gerados antes de permitir alterações importantes. Mantenha o modo de execução de comandos em **Request Review**, não em execução automática irrestrita. O Antigravity gera artefatos como plano, lista de tarefas, diffs e walkthrough; use-os para avaliar o resultado.

As regras persistentes do Antigravity são adequadas para invariantes de arquitetura e comportamento; Agent Skills são adequadas para processos repetíveis. Este pacote usa `.agents/rules/` e `.agents/skills/`. Evite criar novas Workflows tradicionais: a documentação oficial informou migração de Workflows para Agent Skills em 2026.

## Regra crítica de sequência

`Prompt N+1` não pode ser executado enquanto `Prompt N` não estiver `APPROVED`. Se houver falha, o agente deve marcar `BLOCKED`, exibir as evidências e corrigir a etapa atual, sem avançar.

## Fases

| Fase | Prompts | Resultado |
|---|---|---|
| Fundação / Fase 0 | 00–01 | Contexto, regras, evidências de viabilidade e ADRs |
| MVP | 02–12 | Plataforma mínima operacional, segura e validada |
| Fase 2 | 13–17 | Monitoramento, automação, discovery, Ads e finanças avançadas |
| Fase 3 | 18–19 | MCP, IA conversacional e expansão de marketplaces |

Consulte o arquivo completo `../Prompts_Antigravity_Seller_Intelligence.md` para stack, explicações e sequência completa. Comece pelo Prompt 00.
