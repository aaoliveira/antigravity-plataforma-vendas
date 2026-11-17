# Instruções Permanentes do Agente — Seller Intelligence Platform

## 1. Contexto do Produto
A **Seller Intelligence Platform** é uma plataforma SaaS multi-tenant voltada a vendedores de e-commerce e marketplaces. O produto estrutura-se sobre quatro pilares estratégicos:
1. **Market Intelligence:** pesquisa de produtos, categorias, concorrentes, preços e tendências.
2. **AI Commerce:** análise, criação assistida e otimização de anúncios.
3. **Seller Control:** pedidos, custos, impostos configuráveis, margens e lucratividade multi-loja e multi-CNPJ.
4. **Business Intelligence:** dashboards, alertas operacionais, relatórios e apoio determinístico a decisões.

O benchmark funcional primário é o **Avantpro**; referências complementares são **JoomPulse** e **GoSmarter** (não justificam substituição de requisitos equivalentes do Avantpro). Não copiar marcas, código, imagens ou propriedades intelectuais de benchmarks. O escopo vinculante é definido estritamente pelo PRD em [docs/product/PRD.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/product/PRD.md).

---

## 2. Protocolo Obrigatório de Pré-Leitura (Preflight)
Antes de processar qualquer instrução, planejar ou executar qualquer modificação no repositório, o agente DEVE reler obrigatoriamente:
1. [docs/product/PRD.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/product/PRD.md) (Fonte única da verdade de produto).
2. As 4 regras persistentes em `.agents/rules/`:
   - [.agents/rules/anti-hallucination.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/.agents/rules/anti-hallucination.md) (Regras anti-alucinação, evidências e integridade).
   - [.agents/rules/architecture-and-stack.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/.agents/rules/architecture-and-stack.md) (Stack técnica baseline e invariantes arquiteturais).
   - [.agents/rules/testing-and-quality.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/.agents/rules/testing-and-quality.md) (Padrões de testes, Definition of Done e qualidade).
   - [.agents/rules/phase-gates.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/.agents/rules/phase-gates.md) (Protocolo de gating sequencial e regras de bloqueio).
3. A skill operacional de phase gate:
   - [.agents/skills/mvp-phase-gate/SKILL.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/.agents/skills/mvp-phase-gate/SKILL.md).
4. O estado atual da entrega e decisões:
   - [docs/delivery/PHASE_STATUS.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/delivery/PHASE_STATUS.md) e [docs/delivery/phase-gate.json](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/delivery/phase-gate.json).
   - [docs/product/OPEN_QUESTIONS.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/product/OPEN_QUESTIONS.md).
   - [docs/architecture/DECISION_LOG.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/architecture/DECISION_LOG.md).
   - [docs/product/REQUIREMENTS_TRACEABILITY.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/product/REQUIREMENTS_TRACEABILITY.md).

---

## 3. Hierarquia Rígida de Evidências
Se houver conflito ou dúvida, resolva estritamente pela seguinte hierarquia (sem inverter):
1. **PRD oficial** em `docs/product/PRD.md`.
2. **Decisões aprovadas** em `docs/architecture/DECISION_LOG.md` e ADRs aceitos.
3. **Requisitos e critérios de aceite** do prompt atual e prompts aprovados.
4. **Código, contratos e testes existentes** aprovados no repositório.
5. **Documentação oficial atualizada dos fornecedores** (APIs de marketplaces, Spring Boot, PostgreSQL, etc.).
6. **Hipóteses explicitamente registradas e aprovadas** por decisão humana.

Se houver requisito ausente, endpoint não documentado, campo incerto ou regra fiscal ambígua: **NÃO INVENTE**. Registre em [docs/product/OPEN_QUESTIONS.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/product/OPEN_QUESTIONS.md) e solicite decisão humana. Se o item afetar segurança, dinheiro, dados ou integração externa, marque `BLOCKED` e pare a parte dependente.

---

## 4. Invariantes Técnicas e de Engenharia
- **Monólito Modular:** Iniciar obrigatoriamente com monólito modular em Java 21 + Spring Boot 3.5.x; sem microsserviços prematuros.
- **Isolamento Multi-Tenant:** `tenant_id` é obrigatório em toda entidade, query, índice, validação de segurança e teste. Autorização é avaliada no backend; interface nunca é limite de segurança.
- **Cálculo Financeiro Determinístico:** Usar exclusivamente `BigDecimal` com escala e arredondamento explícitos. LLM ou ponto flutuante binário (`float/double`) são estritamente proibidos para cálculos monetários.
- **Proveniência de Dados:** Todo dado de mercado deve ter origem, timestamp de coleta, escopo e rótulo explícito (`OBSERVED`, `IMPORTED`, `ESTIMATED`, `AI_GENERATED` ou `UNAVAILABLE`). `UNAVAILABLE` nunca vira zero.
- **Integrações Oficiais:** Priorizar APIs oficiais e autorizadas. É terminantemente proibido contornar autenticação, CAPTCHA, login, rate limits ou termos de serviço.
- **Ações Externas Mutáveis:** Publicar ou alterar anúncios, preços ou campanhas exige autorização explícita humana e auditoria completa.
- **Gating Sequencial:** Trabalhar estritamente no prompt ativo. O prompt `N+1` não pode ser iniciado enquanto o prompt `N` não estiver `APPROVED` com evidência comprovada.
