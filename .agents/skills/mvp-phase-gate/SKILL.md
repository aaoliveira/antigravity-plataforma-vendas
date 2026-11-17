---
name: mvp-phase-gate
description: Procedimento operacional padronizado de preflight, planejamento, execução, testes, verificação e transição de gate para cada prompt da Seller Intelligence Platform.
---

# Skill: MVP Phase Gate Execution

## 1. Objetivo da Skill
Garantir que o desenvolvimento da **Seller Intelligence Platform** ocorra de forma incremental, estritamente rastreável, testada e governada pelos phase gates, sem avanços precipitados, sem débitos técnicos ocultos e em conformidade absoluta com as regras anti-alucinação.

---

## 2. Fluxo Operacional Passo a Passo

### Passo 1: Preflight Obrigatório
Antes de iniciar qualquer trabalho no prompt corrente:
1. **Verificação do Gate Anterior:**
   - Leia `docs/delivery/phase-gate.json` e `docs/delivery/PHASE_STATUS.md`.
   - Se o prompt anterior (N-1) não estiver marcado como `APPROVED`, **interrompa imediatamente**.
   - Avise o usuário que o Prompt N-1 precisa ser concluído e aprovado antes de prosseguir.
2. **Leitura da Fonte da Verdade:**
   - Releia as seções aplicáveis de `docs/product/PRD.md`.
   - Releia as regras persistentes: `.agents/rules/anti-hallucination.md`, `.agents/rules/architecture-and-stack.md`, `.agents/rules/testing-and-quality.md` e `.agents/rules/phase-gates.md`.
   - Consulte `docs/architecture/DECISION_LOG.md` e `docs/product/OPEN_QUESTIONS.md`.
3. **Mapeamento de Requisitos e Critérios:**
   - Extraia os IDs de requisitos funcionais (`FR-...`) e não funcionais (`NFR-...`) cobertos pelo prompt.
   - Liste os critérios de aceite objetivos e as condições de reprovação.

### Passo 2: Planejamento Detalhado
1. **Definição de Escopo e Não-Escopo:**
   - Especifique exatamente os módulos, classes, componentes e migrações que serão alterados.
   - Reafirme o que **não** será feito neste prompt para evitar vazamento de escopo.
2. **Análise de Incertezas:**
   - Se houver dúvida ou endpoint/regra não comprovada, registre imediatamente a questão em `docs/product/OPEN_QUESTIONS.md`.
   - Se a dúvida for impeditiva (segurança, integridade monetária, dados), marque o gate como `BLOCKED` e pare.
3. **Estratégia de Testes:**
   - Desenhe os testes unitários, testes de integração com Testcontainers ou testes E2E antes de codificar a solução.

### Passo 3: Execução e Implementação
1. **Atualização do Status Inicial:**
   - Atualize `docs/delivery/phase-gate.json` e `docs/delivery/PHASE_STATUS.md` com status `IN_PROGRESS`.
2. **Implementação Guiada por Testes:**
   - Escreva testes e código em conjunto.
   - Aplique padrões de arquitetura (modularidade, isolamento multi-tenant com `tenant_id`, cálculos determinísticos com `BigDecimal`).
   - Não insira mocks de produção, não crie atalhos sem auditoria e não use tags `:latest`.

### Passo 4: Verificação e Validação
1. **Execução de Testes e Builds:**
   - Execute a suíte de testes relevante (backend Maven / JUnit / Testcontainers, frontend Vitest / Playwright).
   - Registre o comando exato executado e o resultado obtido.
2. **Inspeção de Logs e Segurança:**
   - Verifique se os logs estruturados JSON contêm `trace_id` e `tenant_id`.
   - Garanta a ausência total de tokens, credenciais ou dados pessoais (LGPD) nos logs.
3. **Verificação de Definition of Done (DoD):**
   - Valide se todos os 10 itens do DoD (.agents/rules/testing-and-quality.md) foram atendidos.

### Passo 5: Transição e Atualização do Gate
1. **Decisão do Gate:**
   - Se todos os critérios forem comprovados com evidências de teste reais: classifique como **`APPROVED`**.
   - Se algum critério falhar ou houver dependência bloqueante não resolvida: classifique como **`BLOCKED`**.
2. **Sincronização dos Artefatos:**
   - Atualize a tabela em `docs/delivery/PHASE_STATUS.md` com a data, evidências e justificativas.
   - Atualize o arquivo JSON `docs/delivery/phase-gate.json`.
3. **Comunicação:**
   - Apresente ao usuário o relatório final da etapa com as evidências de verificação antes de receber o próximo prompt.
