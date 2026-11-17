# PROMPT 00 — Constituição do projeto, contexto e proteção contra alucinações

**Fase:** Fundação / Fase 0 — obrigatório e primeiro prompt.  
**Objetivo:** fazer o Antigravity compreender exatamente o produto, estabelecer a fonte única da verdade, criar regras persistentes de engenharia e impedir que decisões, dados, integrações ou funcionalidades sejam inventados. Este prompt é de governança e descoberta; NÃO deve implementar funcionalidades de negócio.

## Contexto do produto

Estamos construindo a **Seller Intelligence Platform**, uma plataforma SaaS multi-tenant para vendedores de e-commerce e marketplaces. O produto une quatro pilares:
1. **Market Intelligence:** pesquisa de produtos, categorias, concorrentes, preços e tendências.
2. **AI Commerce:** análise, criação assistida e otimização de anúncios.
3. **Seller Control:** pedidos, custos, impostos configuráveis, margens e lucratividade multi-loja/multi-CNPJ.
4. **Business Intelligence:** dashboards, alertas, relatórios e apoio a decisões.

O benchmark funcional primário é Avantpro. JoomPulse e GoSmarter são referências complementares, nunca justificativa para substituir requisitos equivalentes do Avantpro. O PRD oficial do projeto contém o escopo vinculante. Não copiar marcas, código, imagens, conteúdo protegido ou identidade visual dos benchmarks.

## Fonte da verdade e hierarquia de evidências

Use esta ordem, sem inverter:
1. PRD fornecido pelo usuário em `docs/product/PRD.md`.
2. Decisões aprovadas em `docs/architecture/DECISION_LOG.md` e ADRs.
3. Requisitos e critérios de aceite deste prompt e dos prompts seguintes.
4. Código, contratos e testes existentes no repositório.
5. Documentação oficial atual dos fornecedores (marketplaces, framework, bibliotecas e Antigravity).
6. Hipóteses explicitamente registradas e aprovadas.

Se houver conflito, requisito ausente, endpoint não documentado, campo não confirmado, política de marketplace incerta ou regra fiscal ambígua: NÃO invente. Registre a questão em `docs/product/OPEN_QUESTIONS.md`, indique impacto, evidência já consultada e decisão necessária. Se bloquear segurança, dados, cálculo financeiro ou integração externa, interrompa a parte dependente e peça decisão humana. Para itens não bloqueantes, proponha alternativas, sem implementá-las silenciosamente.

## Regras anti-alucinação e integridade

- Nunca invente endpoints, escopos OAuth, payloads, limites de API, métricas, fontes, credenciais, taxas, impostos, dados de concorrentes ou resultados de testes.
- Não apresente mock, fixture, seed ou dado sintético como dado real. Dados de demonstração devem ser visivelmente marcados e restritos a `dev`/`test`.
- Não transforme `UNAVAILABLE` em zero. Não apresente vendas, CTR, conversão, receita ou estoque de concorrente como reais sem fonte e permissão válidas.
- Todo indicador deve carregar origem, instante de coleta, escopo, tipo de dado e, se estimado, metodologia/limitações.
- Nunca use scraping para contornar login, CAPTCHA, paywall, rate limit, controles técnicos ou termos do marketplace. Priorize APIs oficiais/autorizadas, dados do próprio seller, arquivos importados pelo usuário e provedores licenciados.
- Nunca calcule dinheiro por LLM ou `float/double`. Use motor determinístico e `BigDecimal`.
- Não invente atributos de produtos, certificações, materiais, medidas, garantias ou benefícios na geração de anúncios. Use apenas fatos fornecidos ou confirmados.
- Não exponha segredos, tokens, dados pessoais ou financeiros em logs, testes, exemplos ou respostas de IA.
- Não simule integração externa em produção. Se faltar credencial, deixe a integração não configurada e explique como habilitar; use mocks somente em testes.
- Não afirme que uma funcionalidade está pronta sem teste executado e evidência.
- Não altere escopo, stack, arquitetura, contratos públicos ou banco por conveniência sem registrar ADR e impacto.

## Stack técnica a adotar como baseline

- Backend: Java 21 LTS + Spring Boot 3.5.x, Maven, Spring Security, Spring Data JPA, Flyway, Bean Validation, OpenAPI e Spring Modulith/ArchUnit para limites modulares.
- Frontend: Next.js App Router + React + TypeScript, Tailwind CSS, shadcn/ui, TanStack Query, React Hook Form + Zod e Recharts.
- Persistência: PostgreSQL 18.x; `BigDecimal` para dinheiro.
- Cache: Redis; nunca fonte de verdade.
- Mensageria: RabbitMQ 4.x para jobs e eventos assíncronos.
- Identidade: OIDC com Keycloak no ambiente local/homologação ou IdP gerenciado em produção; Spring Security no backend.
- Arquivos: S3 compatível; MinIO local.
- IA: Spring AI atrás de interface abstrata de provedor, com saída estruturada, prompt versionado e ferramentas limitadas.
- Extensão: Chrome Manifest V3, TypeScript, React/Vite, permissões mínimas.
- Qualidade: JUnit 5, AssertJ, Mockito, Testcontainers, ArchUnit, Vitest, Testing Library e Playwright.
- Operação: Docker Compose local, GitHub Actions, OpenTelemetry, Prometheus e Grafana.

Não atualize majors nem escolha versões `latest` sem verificar suporte e compatibilidade oficiais. Fixe versões e lockfiles.

## Arquitetura e princípios obrigatórios

- Começar com monólito modular, não microsserviços.
- Separar contextos: Identity/Tenancy, Marketplace Integration, Catalog/Orders, Market Intelligence, Competitor Intelligence, Financial Control, Listing AI, Analytics/Reporting, Audit e Notifications.
- Criar contratos de conectores independentes por marketplace; regra específica de canal não deve vazar para o domínio central.
- Separar dados privados de seller e dados de mercado.
- Multi-tenancy e autorização por tenant/empresa/conta são estruturais desde o primeiro schema.
- Integrações externas e tarefas demoradas são assíncronas, observáveis, idempotentes e com retry/dead-letter.
- Todas as migrações são versionadas e testadas em banco limpo.
- APIs possuem validação, paginação, ordenação, filtros permitidos, tratamento consistente de erros e OpenAPI atualizado.
- Ações externas mutáveis precisam de confirmação humana e auditoria.

## Protocolo de execução por etapas

- Trabalhar exclusivamente no prompt atualmente fornecido. Não antecipar o seguinte.
- Antes de codificar, ler o PRD inteiro, gerar plano de implementação e listar ambiguidades.
- Executar o escopo do prompt como unidade pequena e verificável.
- Adicionar testes antes/de junto com o código; não deixar testes para depois.
- Atualizar `docs/delivery/PHASE_STATUS.md` com `IN_PROGRESS`, `APPROVED` ou `BLOCKED`.
- O próximo prompt só será aceito após o atual estar `APPROVED` e todos os testes/critério de aceite estiverem comprovados.

## Tarefa deste primeiro prompt

1. Inspecione o repositório e identifique arquivos já existentes. Não apague nem sobrescreva conteúdo sem avaliar.
2. Localize o PRD anexado. Se o arquivo não estiver no workspace, pare e solicite que o usuário o copie para `docs/product/PRD.md`. Não reconstrua o documento por memória, nem presuma seu conteúdo.
3. Leia o PRD completo. Extraia requisitos, prioridades, critérios de aceite, questões em aberto, fases e decisões arquiteturais.
4. Crie/atualize os seguintes artefatos sem implementar funcionalidades de negócio:
   - `AGENTS.md` na raiz, contendo contexto do produto e regra de consulta às regras persistentes.
   - `.agents/rules/anti-hallucination.md` com todas as regras de evidência e proibições acima.
   - `.agents/rules/architecture-and-stack.md` com stack e princípios arquiteturais.
   - `.agents/rules/testing-and-quality.md` com testes, Definition of Done e padrões de qualidade.
   - `.agents/rules/phase-gates.md` com a regra de bloqueio sequencial.
   - `.agents/skills/mvp-phase-gate/SKILL.md` com o procedimento de preflight, planejamento, implementação, testes e atualização de gate.
   - `docs/product/PRD.md` (somente cópia exata do documento fornecido, se ainda não estiver lá).
   - `docs/product/REQUIREMENTS_TRACEABILITY.md` mapeando IDs do PRD para prompts/fases e testes futuros.
   - `docs/product/OPEN_QUESTIONS.md` transcrevendo as questões abertas e estados do PRD, sem resolvê-las por suposição.
   - `docs/architecture/DECISION_LOG.md` com decisões aprovadas pelo PRD e pendências claramente separadas.
   - `docs/delivery/PHASE_STATUS.md` com status inicial e gate do Prompt 00.
   - `docs/README_DEVELOPMENT.md` com o protocolo de trabalho e comandos planejados, marcando comandos ainda não disponíveis.
5. Crie um arquivo de configuração de status do gate em formato legível por máquina, por exemplo `docs/delivery/phase-gate.json`, com prompt atual, status, critérios, testes e bloqueios. Não use status `APPROVED` se a verificação do PRD ou estrutura falhar.
6. Não gere tela, API de negócio, entidades de domínio ou dados de demonstração neste prompt.

## Critérios de aceite objetivos

- O PRD original está presente em `docs/product/PRD.md` e seu conteúdo não foi reescrito nem resumido.
- Existem os 4 arquivos de regras e a skill indicada, com instruções coerentes entre si.
- `AGENTS.md` instrui o agente a reler as regras e o PRD antes de cada prompt.
- `OPEN_QUESTIONS.md` contém todas as OQ do PRD sem respostas inventadas.
- `DECISION_LOG.md` separa decisões definidas de decisões pendentes.
- O mapa de rastreabilidade cobre todos os IDs FR do PRD ou lista claramente os que precisam de mapeamento.
- `PHASE_STATUS.md` e `phase-gate.json` possuem status consistente e evidência de verificação.
- Nenhum código de funcionalidade de negócio foi criado/modificado.
- Não existem endpoints, métricas ou integrações descritos como confirmados sem fonte.

## Regra de parada

Se o PRD não estiver disponível, se houver conflito de documentos ou se não for possível identificar a versão correta, pare sem inventar. Marque `BLOCKED` e informe exatamente o arquivo/decisão que falta. O Prompt 01 só poderá ser executado depois que o Prompt 00 estiver `APPROVED`.
