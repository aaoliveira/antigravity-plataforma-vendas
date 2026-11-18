# PRD --- Plataforma de Inteligência de Mercado, Analytics e Gestão para E-commerce

**Versão:** 1.0\
**Data:** 17/11/2026\
**Status:** Baseline para desenvolvimento\
**Metodologia:** Vibe Coding + Spec-Driven Development (SDD)\
**Ferramenta de desenvolvimento:** Google Antigravity

------------------------------------------------------------------------

## 1. Visão do produto

Construir uma plataforma web responsiva, multi-tenant, para vendedores e
operadores de e-commerce que consolide inteligência de mercado, análise
de concorrência, monitoramento de produtos, analytics de anúncios,
precificação, lucratividade, gestão financeira e recursos de IA.

A plataforma será inspirada nas capacidades públicas observadas no
ecossistema Avantpro, sem copiar código, identidade visual, conteúdo
proprietário ou mecanismos internos. A implementação deverá usar
integrações autorizadas, dados licenciados, APIs oficiais e/ou fontes
públicas cujo uso seja permitido pelos termos aplicáveis.

### 1.1 Problema

Vendedores operam em vários marketplaces, com dados fragmentados, pouca
visibilidade histórica, dificuldade de estimar oportunidades e baixa
clareza sobre a rentabilidade real por produto, canal e empresa.

### 1.2 Proposta de valor

Oferecer uma visão integrada para responder: - O que vale a pena
vender? - Quem são os concorrentes e como o mercado se comporta? - Como
os preços e indicadores mudaram ao longo do tempo? - Como melhorar
anúncios e conteúdo? - Qual é a margem de contribuição real por SKU,
anúncio, canal e CNPJ? - Onde campanhas e promoções estão consumindo
rentabilidade? - Quais decisões podem ser recomendadas com evidências
rastreáveis?

### 1.3 Objetivos do produto

1.  Unificar dados próprios autorizados dos marketplaces conectados.
2.  Disponibilizar inteligência de mercado com procedência,
    temporalidade e confiança explícitas.
3.  Permitir análise e monitoramento de anúncios/produtos.
4.  Calcular preço, margem de contribuição e indicadores financeiros de
    forma determinística.
5.  Oferecer recomendações de IA fundamentadas em dados internos.
6.  Reduzir trabalho manual sem executar ações que violem políticas de
    marketplace.
7.  Proteger rigorosamente dados entre tenants e credenciais externas.

### 1.4 Não objetivos do MVP

-   Planos, cobrança, faturamento SaaS e assinaturas.
-   Gestão funcional completa de usuários, convites, equipes e
    administração de assentos.
-   Automação de ações que alterem anúncios, preços, pedidos, promoções,
    mensagens ou seguidores.
-   Scraping autenticado, evasão de CAPTCHA, rotação de identidade,
    bypass de rate limit ou qualquer mecanismo de ocultação.
-   Prometer vendas, CTR, conversão ou faturamento de concorrentes
    quando a fonte não os fornece.
-   Implementar todos os marketplaces com paridade funcional no primeiro
    release.
-   Substituir ERP, contabilidade ou sistema fiscal oficial.

**Nota:** autenticação, autorização mínima, identidade do tenant e
controle de acesso são requisitos fundacionais de segurança e não podem
ser adiados, mesmo que a gestão funcional de usuários esteja fora do
escopo.

------------------------------------------------------------------------

## 2. Personas e cenários

### Persona A --- Seller individual

Opera uma ou poucas contas, precisa encontrar produtos, comparar
concorrentes e entender margem.

### Persona B --- Operador de e-commerce

Gerencia catálogo, anúncios, pedidos e desempenho em vários canais.

### Persona C --- Gestor financeiro/comercial

Precisa consolidar resultados por CNPJ, marketplace, SKU, campanha e
período.

### Persona D --- Analista de mercado

Pesquisa categorias, preços, vendedores, avaliações e tendências com
evidências de origem.

### Cenários principais

1.  Conectar uma conta de marketplace com OAuth/autorização oficial.
2.  Importar pedidos e anúncios próprios permitidos pela API.
3.  Pesquisar um produto e visualizar resultados cuja fonte e data
    estejam explícitas.
4.  Monitorar um item e observar snapshots históricos coletados após a
    inclusão.
5.  Simular preço e margem com taxas e custos configuráveis.
6.  Consultar um painel financeiro consolidado por empresa e canal.
7.  Pedir à IA uma análise de mercado baseada em dados recuperados pelos
    serviços da plataforma.
8.  Exportar relatórios sem expor dados de outro tenant.

------------------------------------------------------------------------

## 3. Escopo funcional

### 3.1 MVP --- primeiro release validável

O MVP será construído em duas integrações prioritárias: Mercado Livre e
Shopee. A cobertura de cada API deverá ser confirmada por documentação
oficial, credenciais de desenvolvedor e permissões reais. O sistema deve
mostrar indisponibilidade quando um dado não for oferecido.

**MVP-01 --- Fundação** - Estrutura modular, configuração, migrações,
logs e observabilidade. - Autenticação e autorização mínimas. - Tenant
context confiável e isolamento de dados. - Auditoria de operações
sensíveis. - Interface responsiva, light mode padrão e dark mode
opcional.

**MVP-02 --- Conexões** - Cadastro de conexão por tenant. - OAuth ou
mecanismo oficial aplicável. - Armazenamento cifrado de tokens. -
Renovação, revogação, expiração e health status. - Limites, backoff,
circuit breaker e trilha de auditoria. - Nenhuma senha de marketplace
armazenada.

**MVP-03 --- Dados próprios** - Sincronização de catálogo/anúncios
próprios, pedidos e itens de pedido, conforme escopos autorizados. -
Sincronização incremental e idempotente. - Registro da origem, horário
de coleta e período de referência. - Tratamento de paginação, retries,
webhooks quando oficiais e reconciliação.

**MVP-04 --- Inteligência de mercado inicial** - Pesquisa de
anúncios/produtos apenas por fontes permitidas. - Normalização de
resultados. - Comparação de preço e atributos. - Sinalização de
estimativa versus dado observado/oficial. - Sem alegar venda estimada se
não houver fonte ou metodologia aprovada.

**MVP-05 --- Monitoramento** - Grupos de monitoramento. -
Adicionar/remover itens. - Snapshots a partir da data de inclusão. -
Histórico de preços e atributos observáveis. - Atualização sob limites e
políticas permitidos. - Alertas básicos para variações de preço quando
houver dados.

**MVP-06 --- Pricing & Profitability** - Cadastro de custo de aquisição,
embalagem, frete subsidiado, despesas e impostos configuráveis. - Regras
de comissão/taxas versionadas por marketplace, modalidade e vigência. -
Calculadora determinística de margem de contribuição. - Simulação de
preço-alvo e preço mínimo. - Exibir premissas e componentes do
cálculo. - Não rotular margem de contribuição como lucro líquido.

**MVP-07 --- Financeiro inicial** - Consolidar pedidos sincronizados. -
Visão de receita, cancelamentos/devoluções quando disponíveis, taxas e
custo informado. - Indicadores por período, empresa, conta, canal e
SKU. - Reconciliação e indicação de dados ausentes. - ROI/ROAS somente
quando dados de investimento e atribuição estiverem disponíveis e
claramente definidos.

**MVP-08 --- Analytics inicial** - Métricas de anúncios próprios que a
API disponibilizar. - Visitas, impressões, cliques, CTR, conversão e
vendas somente quando cada indicador estiver acessível. - Exibir "não
disponível" em vez de inferir silenciosamente. - Comparação entre
períodos com datas e timezone explícitos.

**MVP-09 --- IA assistiva** - Chat/assistente somente leitura no MVP. -
RAG/tool calling para consultar serviços internos. - Geração de
título/descrição a partir de dados fornecidos pelo vendedor, com revisão
humana. - Análise de avaliações apenas quando a coleta/uso for
permitido. - Citações internas de origem e data dos dados. - IA não
calcula taxas/margens; chama o Pricing Engine.

**MVP-10 --- Relatórios** - Dashboard inicial. - Filtros por período,
empresa, marketplace e conta. - Exportação CSV/XLSX de dados autorizados
do próprio tenant. - Auditoria de exportações.

### 3.2 Evolução pós-MVP

-   Amazon SP-API.
-   TikTok Shop, Magalu e SHEIN, após validação de acesso, programa e
    escopos.
-   Inteligência de categorias e tendências em fontes
    licenciadas/autorizadas.
-   Análise avançada de concorrência.
-   Buy Box/catálogo quando API/fonte permitida.
-   Análise de avaliações e quebra de objeções.
-   Analytics de SEO/ranking/keywords conforme disponibilidade oficial.
-   Geração avançada de anúncios e editor de imagens.
-   Experimentos sequenciais de anúncios; A/B verdadeiro somente quando
    tecnicamente suportado.
-   Análise de fornecedores.
-   MCP server somente leitura.
-   Alertas avançados e automações permitidas.
-   Aplicativo mobile/PWA.
-   Previsões estatísticas com validação e intervalo de confiança.

------------------------------------------------------------------------

## 4. Requisitos funcionais (RF)

### Identidade, tenant e segurança

-   **RF-001:** toda requisição autenticada deve possuir identidade e
    tenant resolvidos por contexto confiável, nunca por `tenantId`
    arbitrário enviado pelo cliente.
-   **RF-002:** autorizar cada operação por tenant, recurso e permissão.
-   **RF-003:** segregar dados por tenant em todas as entidades
    tenant-scoped.
-   **RF-004:** auditar acesso e alteração de credenciais, conexões,
    regras financeiras, exportações e ações administrativas.
-   **RF-005:** permitir revogar uma conexão e apagar/anonimizar tokens
    conforme política de retenção.

### Integrações

-   **RF-010:** conectar Mercado Livre e Shopee por mecanismos oficiais
    aplicáveis.
-   **RF-011:** persistir tokens cifrados com rotação de chave e acesso
    restrito.
-   **RF-012:** sincronizar dados de forma incremental, paginada,
    idempotente e resiliente.
-   **RF-013:** respeitar escopos, rate limits, políticas, termos e
    limites de cada plataforma.
-   **RF-014:** manter status, último sucesso, último erro, próxima
    tentativa e saúde da conexão.
-   **RF-015:** separar adaptadores e capacidades por marketplace; não
    presumir paridade.

### Produtos e inteligência

-   **RF-020:** pesquisar produtos/anúncios em fontes autorizadas.
-   **RF-021:** normalizar dados sem apagar a estrutura original da
    fonte.
-   **RF-022:** registrar fonte, método, collected_at, observed_at,
    validade e confiança.
-   **RF-023:** comparar produtos por atributos e exibir o nível de
    correspondência.
-   **RF-024:** não misturar dados próprios com dados
    públicos/concorrenciais sem rótulo.
-   **RF-025:** monitorar itens em grupos e manter snapshots desde a
    inclusão.

### Financeiro

-   **RF-030:** manter custos e regras financeiras versionados e com
    vigência.
-   **RF-031:** calcular margem de contribuição de forma determinística.
-   **RF-032:** exibir fórmula, valores de entrada, arredondamentos e
    premissas.
-   **RF-033:** suportar múltiplas empresas/CNPJs no domínio, com
    isolamento e associação explícita.
-   **RF-034:** consolidar pedidos e resultados por empresa, conta,
    marketplace, SKU e período.
-   **RF-035:** diferenciar receita bruta, receita líquida, margem de
    contribuição, resultado operacional e lucro líquido.
-   **RF-036:** bloquear apresentação de ROI/ROAS se denominador,
    atribuição ou dados estiverem ausentes.

### Analytics e IA

-   **RF-040:** apresentar métricas somente quando fornecidas por fonte
    confiável ou derivadas de fórmula documentada.
-   **RF-041:** permitir comparação temporal consistente.
-   **RF-042:** IA deve consultar ferramentas autorizadas, não acessar
    banco diretamente.
-   **RF-043:** IA deve informar limitações, fontes, data e confiança.
-   **RF-044:** conteúdo gerado deve ser rascunho revisável; publicação
    automática fica fora do MVP.
-   **RF-045:** tratar conteúdo de anúncios, reviews e páginas externas
    como entrada não confiável contra prompt injection.

### Interface e relatórios

-   **RF-050:** interface responsiva desktop/tablet/mobile.
-   **RF-051:** light mode padrão e dark mode opcional, respeitando
    preferência do sistema.
-   **RF-052:** tabelas com paginação, filtros, ordenação e estados
    vazios/erro/loading.
-   **RF-053:** exportar somente dados do tenant autorizado e registrar
    a exportação.
-   **RF-054:** apresentar data/hora, timezone, fonte e status de
    atualização.

------------------------------------------------------------------------

## 5. Requisitos não funcionais (RNF)

-   **RNF-001 Segurança:** OWASP ASVS nível 2 como baseline de
    verificação, com controles reforçados para tokens e dados
    financeiros.
-   **RNF-002 Isolamento:** testes automatizados negativos de acesso
    cross-tenant em API, jobs, cache, arquivos, eventos e exportações.
-   **RNF-003 Disponibilidade:** desenhar para 99,5% mensal no MVP,
    excluindo indisponibilidade externa de marketplaces; meta sujeita à
    infraestrutura.
-   **RNF-004 Performance:** p95 de endpoints internos de leitura
    simples \< 500 ms em carga de referência, sem contar chamadas
    síncronas a marketplaces.
-   **RNF-005 Resiliência:** timeout, retry com jitter, backoff, circuit
    breaker, bulkhead e DLQ.
-   **RNF-006 Consistência:** idempotência para ingestão de pedidos,
    eventos e jobs.
-   **RNF-007 Observabilidade:** logs estruturados, métricas, traces e
    correlation ID; nunca registrar tokens, segredos ou PII
    desnecessária.
-   **RNF-008 Manutenibilidade:** módulos com contratos explícitos,
    dependências direcionais e testes de arquitetura.
-   **RNF-009 Acessibilidade:** WCAG 2.2 AA como objetivo para fluxos
    principais.
-   **RNF-010 Responsividade:** suporte a larguras de 360 px até desktop
    ultrawide.
-   **RNF-011 Privacidade:** minimização, finalidade, retenção, direitos
    dos titulares e controles LGPD.
-   **RNF-012 Portabilidade:** Docker para desenvolvimento e
    infraestrutura declarativa para ambientes.
-   **RNF-013 Recuperação:** backups criptografados, restore testado e
    RPO/RTO definidos antes de produção.
-   **RNF-014 Dados:** cada métrica deve ter definição, unidade, origem,
    timestamp e limitações.

------------------------------------------------------------------------

## 6. Regras de negócio essenciais

1.  Um produto canônico pode possuir vários anúncios em diferentes
    marketplaces/contas; produto e anúncio não são a mesma entidade.
2.  Dados concorrenciais não podem ser tratados como dados oficiais da
    conta do seller.
3.  Toda métrica deve indicar `source_type`: `OFFICIAL`, `OBSERVED`,
    `ESTIMATED` ou `USER_INPUT`.
4.  Estimativas devem ter metodologia, janela temporal, versão do modelo
    e confiança; se não houver, não exibir estimativa.
5.  Snapshots históricos começam no primeiro registro efetivamente
    coletado; não inventar histórico retroativo.
6.  Taxas, comissões e tributos devem ter fonte, vigência, escopo e data
    de atualização.
7.  Cálculos financeiros devem ser determinísticos, versionados e
    reproduzíveis.
8.  Margem de contribuição não equivale a lucro líquido.
9.  Não aceitar promoção, alterar preço, publicar anúncio ou executar
    outra ação write no MVP.
10. A indisponibilidade de uma API não deve levar a fallback proibido;
    marcar a capacidade como indisponível e orientar o usuário.
11. Um retry nunca pode duplicar pedido, item, cobrança interna ou
    evento.
12. Nenhuma resposta da IA pode afirmar que executou uma ação externa
    sem confirmação verificável.

------------------------------------------------------------------------

## 7. Arquitetura e stack

### Backend

-   Java 21 LTS.
-   Spring Boot 4.x (selecionar patch estável compatível no início do
    projeto).
-   Spring Security.
-   Spring Data JPA/Hibernate.
-   Spring Modulith.
-   Bean Validation.
-   Flyway.
-   MapStruct.
-   Spring AMQP.
-   Spring Batch quando houver processamento em lote real.
-   OpenAPI.

### Frontend

-   Next.js (App Router), React e TypeScript.
-   Tailwind CSS e shadcn/ui.
-   TanStack Query.
-   React Hook Form + Zod.
-   Zustand apenas para estado global de UI que realmente precise.
-   Biblioteca de gráficos mantida e acessível.

### Dados e infraestrutura

-   PostgreSQL como fonte transacional.
-   Redis para cache/locks efêmeros com namespace tenant-aware; nunca
    como fonte única de autorização.
-   RabbitMQ para processamento assíncrono.
-   S3 compatível para arquivos privados.
-   Docker Compose local.
-   OpenTelemetry, Prometheus, Grafana e logs centralizados.
-   Testcontainers para testes de integração.

**Regra de versões:** não inventar versões. Antes de implementar,
verificar compatibilidade nas documentações oficiais e fixar versões
exatas em arquivos de build/lock. Não atualizar major version durante
uma feature sem ADR e aprovação.

### Arquitetura

Começar com **modular monolith** e boundaries explícitos. Não iniciar
com microserviços. Extrair serviço somente quando houver evidência de
escala, isolamento operacional ou necessidade independente.

Módulos de domínio: - identity-access (mínimo necessário) - tenancy -
marketplace-connection - marketplace-adapters - catalog -
order-ingestion - market-intelligence - competitor-research -
product-monitoring - pricing - profitability - financial-analytics -
listing-analytics - ai-assistant - reporting - notification - audit

------------------------------------------------------------------------

## 8. Segurança multi-tenant

### Modelo recomendado

-   `tenant_id` obrigatório em toda entidade tenant-scoped.
-   Tenant resolvido pelo principal autenticado e membership/claims
    assinadas.
-   Nunca confiar em `X-Tenant-Id` ou parâmetro de rota sem autorização
    server-side.
-   PostgreSQL Row-Level Security como defesa adicional.
-   `SET LOCAL app.current_tenant` dentro da transação; limpar contexto
    automaticamente ao final.
-   Repositórios e serviços sempre filtram tenant; RLS não substitui
    autorização de aplicação.
-   Índices únicos compostos incluindo tenant quando aplicável.
-   Chaves estrangeiras compostas ou validações equivalentes para
    impedir referências cruzadas.
-   Cache keys, filas, arquivos e objetos S3 namespaced por tenant.
-   Jobs carregam tenant explícito e validado; não executar job sem
    contexto.
-   Testes cross-tenant para leitura, escrita, exclusão, exportação,
    busca, cache, eventos e arquivos.
-   Auditoria de ações sensíveis.
-   Criptografia de tokens com envelope encryption/KMS ou secret
    manager; não usar chave hardcoded.
-   Segredos fora do repositório; secret scanning no CI.
-   Cookies HttpOnly, Secure, SameSite; proteção CSRF conforme
    arquitetura de sessão.
-   CSP, CORS allowlist, validação de entrada e saída.
-   Proteção SSRF em URLs externas; bloquear IPs privados, metadata
    endpoints e redirects perigosos.
-   Upload com validação MIME real, tamanho, antivírus quando aplicável
    e armazenamento privado.
-   LGPD: minimização, finalidade, retenção, exclusão e registro de
    operações de tratamento.

### Gestão de usuários fora do escopo

A implementação inicial deve ter apenas o mínimo para autenticar um
principal de teste/administrador e estabelecer tenant context seguro.
Não construir telas completas de convite, equipes, perfis avançados,
recuperação sofisticada, billing ou administração de assentos. Nunca
desativar autenticação em produção para simplificar o MVP.

------------------------------------------------------------------------

## 9. Política de integração segura com marketplaces

Cada adapter deverá implementar um contrato de capacidades, por
exemplo: - `supportsOwnCatalogRead` - `supportsOwnOrdersRead` -
`supportsListingMetricsRead` - `supportsPublicProductSearch` -
`supportsCompetitorData` - `supportsWriteOperations` (desabilitado no
MVP)

Cada capacidade deverá ter evidência documental, escopos necessários,
limites, termos e data da validação.

### Controles obrigatórios

-   Preferir APIs oficiais e programas autorizados.
-   OAuth/credenciais de aplicação conforme documentação oficial.
-   Menor privilégio de escopos.
-   Token cifrado, renovação segura, revogação e rotação.
-   Respeitar rate limit e quotas; backoff exponencial com jitter e
    `Retry-After`.
-   Timeout, circuit breaker, bulkhead e concorrência limitada por
    conta/marketplace.
-   Idempotency key para comandos e ingestões.
-   Não fazer scraping de páginas autenticadas, contornar bloqueios,
    CAPTCHA, WAF ou mecanismos anti-bot.
-   Não usar proxies/rotação de IP para contornar limites.
-   Não automatizar cliques/ações em nome do usuário sem base oficial e
    autorização explícita.
-   Não fazer follow, impulsionar, aceitar promoções, editar preço,
    publicar ou alterar anúncios no MVP.
-   Manter kill switch por marketplace/capacidade.
-   Em dúvida sobre política, bloquear a capacidade e registrar
    `OPEN_QUESTION`; não presumir permissão.
-   Revisar termos e documentação oficial antes de cada integração e
    periodicamente.

**Aviso:** nenhum desenho pode garantir risco zero de
restrição/suspensão de conta. A meta é reduzir risco por conformidade,
menor privilégio, transparência e ausência de automação não autorizada.

------------------------------------------------------------------------

## 10. IA responsável

-   IA não recebe credenciais nem acesso SQL direto.
-   Todas as ferramentas têm autorização por tenant e schema de entrada
    estrito.
-   Conteúdo externo é não confiável; defender contra prompt injection.
-   Saídas estruturadas validadas por schema.
-   Cálculos monetários sempre executados no Pricing/Profitability
    Engine.
-   Respostas distinguem fatos, inferências e recomendações.
-   Mostrar fontes, datas e limitações.
-   Não gerar avaliações falsas, atributos não comprovados ou claims
    enganosos.
-   Conteúdo gerado é rascunho e exige revisão humana.
-   Registrar modelo, versão do prompt, ferramentas chamadas e
    custo/latência sem armazenar dados sensíveis desnecessários.

------------------------------------------------------------------------

## 11. Métricas de sucesso

-   100% dos testes críticos de isolamento multi-tenant aprovados.
-   100% das integrações com escopos e capacidades documentados.
-   0 tokens/segredos em logs ou repositório.
-   100% dos cálculos financeiros com trilha de premissas e teste de
    referência.
-   100% dos dados de mercado com origem e timestamp.
-   Sincronização idempotente sem duplicidade em testes de repetição.
-   Pelo menos 90% de cobertura de testes nas regras financeiras e de
    autorização críticas.
-   Jornada E2E: conectar conta → sincronizar dados → visualizar
    indicadores → simular margem → exportar relatório.
-   Testes responsivos nos breakpoints definidos.
-   Nenhuma feature liberada com vulnerabilidade crítica/alta não
    mitigada.

------------------------------------------------------------------------

## 12. Riscos e mitigação

  -----------------------------------------------------------------------
  Risco                               Mitigação
  ----------------------------------- -----------------------------------
  API não oferece dado concorrencial  Exibir capacidade indisponível;
                                      buscar fonte licenciada/permitida;
                                      não fazer scraping proibido.

  Mudança de política do marketplace  Registro de política por adapter,
                                      revisão periódica e kill switch.

  Rate limit/banimento                Quotas conservadoras, backoff,
                                      jitter, circuit breaker e
                                      paralelismo limitado.

  Dados estimados incorretos          Rótulo, metodologia, confiança e
                                      ausência de estimativa quando não
                                      sustentada.

  Vazamento cross-tenant              Tenant context, RLS, autorização,
                                      testes negativos e auditoria.

  IA alucinar métricas                Tool calling, dados estruturados,
                                      fontes e cálculos determinísticos.

  Divergência financeira              Regras versionadas, testes de
                                      referência e reconciliação.

  Complexidade excessiva              Modular monolith, MVP focado e ADR
                                      para mudanças.
  -----------------------------------------------------------------------

------------------------------------------------------------------------

## 13. Critérios de aceite do MVP

O MVP só será considerado aprovado quando: 1. Um principal autenticado
acessa somente o tenant autorizado. 2. Testes negativos comprovam
isolamento cross-tenant. 3. É possível conectar e revogar conexões de
Mercado Livre e Shopee usando mecanismos oficiais disponíveis. 4. A
sincronização de dados próprios funciona de modo idempotente. 5. Dados
mostram origem, timestamp e tipo de evidência. 6. Pesquisa e
monitoramento funcionam apenas nas capacidades legalmente/técnicamente
suportadas. 7. Cálculos de margem são determinísticos e reproduzíveis.
8. Painel financeiro reconcilia pedidos importados e sinaliza dados
faltantes. 9. A IA responde com base em ferramentas internas e não
inventa números. 10. A interface funciona em desktop e mobile, com
light/dark mode. 11. Testes unitários, integração, contrato, segurança e
E2E passam. 12. Documentação, ADRs, inventário de capacidades e runbooks
estão atualizados. 13. Nenhuma ação write de marketplace é executada
pelo MVP. 14. Nenhuma vulnerabilidade crítica/alta conhecida permanece
sem mitigação aprovada.
