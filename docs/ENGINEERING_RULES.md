# Regras de Engenharia, Governança e Quality Gates

> **Documento Normativo Obrigatório** &middot; **Versão:** 1.0.0  
> **Status:** Ativo e Inviolável &middot; **Data:** 17/11/2026  
> **Aplicação:** Todos os agentes, desenvolvedores e pipelines da Seller Intelligence Platform.  

---

## 1. As 15 Regras Invioláveis de Engenharia

O desenvolvimento desta plataforma é pautado pela disciplina de software engineering e evidências comprovadas. As 15 regras abaixo são **invioláveis**:

### Regra 1: Não Alucinar
É terminantemente proibido inventar APIs, endpoints, escopos OAuth, limites operacionais, eventos de webhook, métricas de mercado, schemas JSON ou capacidades não documentadas de qualquer marketplace. Se não houver confirmação documental, a capacidade é considerada inexistente.

### Regra 2: Fonte Oficial Obrigatória
Para cada integração com marketplace ou serviço externo, consulte a documentação oficial vigente do canal. Toda spec deve registrar a URL da documentação, a data da consulta, os escopos mínimos exigidos, quotas, limites de taxa (*rate limits*) e a evidência formal do requisito.

### Regra 3: Dúvida Bloqueante
Se não houver evidência técnica ou documental suficiente para implementar um recurso com segurança e conformidade, registre imediatamente a questão em [docs/OPEN_QUESTIONS.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/OPEN_QUESTIONS.md), marque a capacidade correspondente como `UNVERIFIED` e **não implemente a operação**.

### Regra 4: Conformidade com Políticas dos Marketplaces
Não implementar qualquer tipo de web scraping proibido, acesso não autorizado a dados privados de terceiros, bypass de CAPTCHA, contorno de Web Application Firewall (WAF), evasão de rate limit por rotação de IP ou proxies não declarados, automação de comportamento humano ou ações que coloquem a conta do seller em risco de suspensão ou banimento.

### Regra 5: Menor Privilégio (*Least Privilege*)
Solicitar estritamente os escopos OAuth mínimos necessários para a leitura autorizada de catálogo e pedidos próprios do seller. Rejeitar escopos com privilégios excessivos (ex.: escopos de mutação financeira quando apenas leitura é exigida).

### Regra 6: Sem Escrita Externa no MVP
O MVP opera sob regime estrito de **somente leitura** em relação aos marketplaces externos. É categoricamente proibido publicar anúncios, editar títulos ou preços, aceitar campanhas promocionais, impulsionar produtos, enviar mensagens automáticas, seguir usuários ou executar qualquer chamada HTTP de mutação (`POST`, `PUT`, `PATCH`, `DELETE`) em endpoints de marketplace.

### Regra 7: Isolamento Multi-Tenant Absoluto
Toda operação de negócio, consulta SQL, job assíncrono, evento de mensageria, chave de cache Redis, objeto em storage S3 e arquivo exportado deve carregar o contexto de tenant explicitamente validado (`tenant_id`). É proibido confiar em identificadores de tenant enviados pelo navegador ou client sem validação contra a sessão/token do principal autenticado.

### Regra 8: Proteção Rígida de Segredos e Credenciais
Tokens OAuth, chaves de API e segredos nunca devem ser armazenados em texto puro no banco de dados (exigida criptografia com envelope encryption AES-256-GCM). Segredos, senhas e tokens jamais podem ser impressos em logs, expostos em mensagens de erro ou commitados no repositório Git.

### Regra 9: Cálculos Financeiros Determinísticos em `BigDecimal`
Valores monetários, margens de contribuição, alíquotas tributárias, taxas de marketplace, custos operacionais e ROI devem ser calculados exclusivamente por código determinístico em Java utilizando `BigDecimal`, com escala e regras de arredondamento (`RoundingMode.HALF_UP` ou `HALF_EVEN`) explicitamente versionadas. **Modelos de Linguagem (LLMs) ou tipos de ponto flutuante binário (`float`/`double`) nunca calculam valores financeiros.**

### Regra 10: Evidência Comprovada de Testes
Nenhum critério de aceite ou funcionalidade pode ser declarado aprovado sem a execução efetiva da suíte de testes correspondente e o registro do comando, ambiente e log do resultado. Testes comentados, ignorados (`@Disabled`) ou com asserções frouxas (*tautologias*) são infrações graves de qualidade.

### Regra 11: Sem Atalhos Técnicos
É proibido desabilitar testes, controles de segurança, linters, checagens de tipos, validações de entrada (Bean Validation/Zod), Row-Level Security (RLS) ou scanners de segurança apenas para fazer um pipeline passar ou contornar um erro.

### Regra 12: Sem Expansão Silenciosa de Escopo
Não adicionar dependências externas, módulos de software, endpoints REST, colunas de banco ou comportamentos que não constem explicitamente na especificação da feature corrente. Qualquer mudança estrutural exige aprovação prévia e registro de ADR em [docs/DECISION_LOG.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/DECISION_LOG.md).

### Regra 13: Bloqueio Sequencial Rígido de Prompts
Nenhum prompt ou fase posterior (`Prompt N+1`) pode ser iniciado ou planejado enquanto o prompt corrente (`Prompt N`) não atingir o estado formal de `APPROVED` no arquivo [docs/delivery/phase-gate.json](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/delivery/phase-gate.json). Se houver qualquer teste falho ou pendência crítica, o próximo prompt permanece bloqueado.

### Regra 14: Revisão Obrigatória de Premissas em Cada Preflight
Antes de iniciar qualquer prompt, o agente deve reler obrigatoriamente:
- `docs/PROJECT_CONTEXT.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/TECH_STACK.md`
- `docs/ENGINEERING_RULES.md`
- `docs/SECURITY.md`
- `docs/MARKETPLACE_POLICIES.md`
- `docs/BACKLOG.md`
- `docs/DECISION_LOG.md`
- `docs/OPEN_QUESTIONS.md`
- `docs/IMPLEMENTATION_STATUS.md`
Se for detectada qualquer contradição ou divergência com o estado do repositório, o agente deve **parar e reportar**.

### Regra 15: Separação Estrita Backend $\rightarrow$ Frontend
Para toda funcionalidade que envolva backend e frontend, a camada backend (domínio, migrações, serviços, endpoints REST e testes automatizados) deve ser executada e validada primeiro. A implementação frontend só pode ser iniciada após o backend estar formalmente `PASSED`. Caso uma entrega seja puramente backend ou frontend, o log de execução deve justificar formalmente o motivo.

---

## 2. Estratégia de Branches e Convenção de Commits

### 2.1 Branch Strategy
Adotamos uma estratégia baseada em trunk-based development com feature branches curtas:
- `main`: Branch protegida contendo apenas código estável que passou por todos os quality gates e pipelines de CI.
- `feature/<prompt-id>-<slug>`: Branch dedicada para a execução de um prompt específico (ex.: `feature/P001-backend-modular-structure`).
- `fix/<issue-id>-<slug>`: Branch dedicada à resolução de bugs ou vulnerabilidades identificadas.

### 2.2 Convenção de Commits (Conventional Commits v1.0.0)
As mensagens de commit devem seguir o padrão:
```
<tipo>(<escopo>): <descrição no imperativo e em minúsculas>

[corpo opcional explicando o contexto e a motivação da alteração]

[rodapé opcional com referências a issues ou breaking changes]
```

**Tipos Permitidos:**
- `feat`: Nova funcionalidade para o usuário ou API.
- `fix`: Correção de bug no código.
- `docs`: Alterações exclusivas na documentação.
- `test`: Adição ou refatoração de testes automatizados sem alterar código de produção.
- `refactor`: Refatoração de código que não altera comportamento funcional nem adiciona feature.
- `perf`: Alteração de código orientada à melhoria comprovada de performance.
- `security`: Melhoria ou implementação de mecanismos de segurança e isolamento.
- `chore`: Atualizações de build, configuração de CI ou ferramentas de desenvolvimento.

---

## 3. Definition of Ready (DoR) e Definition of Done (DoD)

### 3.1 Definition of Ready (DoR) — Condições para Iniciar uma Feature
Uma funcionalidade só transita de `NOT_STARTED` para `READY` e `IN_PROGRESS` se:
1. O prompt anterior estiver formalmente `APPROVED`.
2. Todos os requisitos funcionais e critérios de aceite estiverem explicitados.
3. Não houver dúvidas impeditivas abertas em `docs/OPEN_QUESTIONS.md`.
4. Os contratos de dados, endpoints e premissas fiscais/matemáticas estiverem documentados na spec da feature (`docs/specs/<feature-id>/`).
5. A documentação oficial da API externa envolvida tiver sido consultada e catalogada.

### 3.2 Definition of Done (DoD) — Condições para Concluir uma Feature
Uma funcionalidade só transita para `PASSED` se:
1. O código de produção implementar estritamente o escopo especificado, sem expansões silenciosas.
2. Toda a suíte de testes (unitários, integração com Testcontainers, arquitetura com ArchUnit) passar com 100% de sucesso.
3. Isolamento multi-tenant comprovado por testes negativos cross-tenant.
4. Logs estruturados JSON emitidos com correlação (`trace_id`, `tenant_id`) e sem vazamento de tokens, senhas ou PII.
5. Contratos de API em conformidade com OpenAPI 3.0 e RFC 7807 para erros.
6. Acessibilidade (WCAG 2.2 AA) e responsividade (360px a ultrawide) validadas em componentes frontend.
7. Documentação do sistema, status dos gates e log de execução devidamente atualizados.
