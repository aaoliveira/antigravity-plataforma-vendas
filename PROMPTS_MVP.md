# Biblioteca de Prompts --- MVP

## Instrução de uso

Execute **um prompt por vez** no Google Antigravity. O prompt 000 deve
estar `PASSED`. Antes de cada prompt, o agente deve executar o preflight
descrito em `PROMPT_000_GOVERNANCE.md`.

Cada funcionalidade está separada em backend e frontend. Não iniciar o
frontend antes do backend correspondente estar `PASSED`. Quando a
feature não exigir uma das camadas, o prompt explica a razão.

------------------------------------------------------------------------

# P001 --- Estrutura base do repositório e backend modular

## P001-BE --- Backend

Implemente a fundação do backend Java 21/Spring Boot 4.x como modular
monolith. Antes de codificar, verifique as versões estáveis compatíveis
nas fontes oficiais e registre-as. Configure build reproduzível,
profiles local/test/prod, configuração externa, health checks, OpenAPI,
Flyway, validação, tratamento padronizado de erros, correlation ID e
testes de arquitetura. Crie módulos vazios com boundaries definidos no
PRD, sem lógica de negócio antecipada. Não exponha endpoints
administrativos inseguros.

**Aceite:** build limpo; aplicação inicia via Docker Compose; migração
inicial executa; health/readiness funcionam; OpenAPI é gerado; testes de
arquitetura impedem dependências proibidas; nenhum segredo está no
repositório.

## P001-FE --- Frontend

Após P001-BE `PASSED`, crie o shell Next.js/React/TypeScript com layout
responsivo, navegação placeholder, tratamento global de erro, loading,
empty states, tokens de design, light mode padrão e dark mode opcional.
Use componentes acessíveis e evite telas funcionais sem contrato
backend.

**Aceite:** build/lint/typecheck passam; layout validado em 360, 768,
1024 e 1440 px; tema persiste; navegação por teclado funciona; sem
overflow horizontal crítico.

------------------------------------------------------------------------

# P002 --- Identidade mínima e contexto do tenant

## P002-BE

Implemente somente o mínimo seguro de principal autenticado, tenant,
membership e autorização. Tenant deve ser derivado de
identidade/membership validada, nunca de parâmetro ou header arbitrário.
Defina contratos de autenticação adequados à arquitetura escolhida; não
crie gestão completa de usuários. Inclua expiração de sessão/token,
proteção contra brute force e auditoria. Adicione testes de acesso sem
autenticação, tenant ausente, tenant inválido e tentativa de spoofing.

**Aceite:** acesso negado por padrão; nenhum endpoint tenant-scoped
funciona sem contexto válido; tenant não pode ser alterado pelo cliente;
testes negativos passam.

## P002-FE

Após backend aprovado, implemente telas mínimas de login/seleção de
contexto quando necessárias e proteção de rotas. Não implementar
convite, equipes, billing ou administração de usuários. Não guardar
tokens sensíveis em localStorage se houver alternativa segura por cookie
HttpOnly.

**Aceite:** rotas protegidas; sessão expirada direciona para
autenticação; nenhuma informação de outro tenant aparece; acessibilidade
e responsividade aprovadas.

------------------------------------------------------------------------

# P003 --- Isolamento multi-tenant e auditoria

## P003-BE

Implemente `tenant_id` obrigatório, filtros server-side, PostgreSQL RLS
como defesa adicional, `SET LOCAL` transacional, FKs/índices compostos,
auditoria e isolamento em jobs, cache, filas e storage. Crie testes de
integração com pelo menos dois tenants que tentem ler, criar, alterar,
excluir, pesquisar, exportar e recuperar arquivos cruzados. Inclua
testes para conexão reutilizada no pool e contexto transacional limpo.

**Aceite:** todos os cenários cross-tenant são negados; RLS habilitada
nas tabelas aplicáveis; conexão não vaza contexto entre transações;
auditoria não armazena segredos; testes passam em PostgreSQL real via
Testcontainers.

## P003-FE

Após backend aprovado, inclua tratamento padronizado para 401/403/404
sem revelar existência de recursos de outro tenant, estados de sessão e
feedback de acesso negado. Não exibir tenant ID técnico.

**Aceite:** erros não revelam recursos cross-tenant; UI não permite
trocar tenant manipulando URL; testes de componente passam.

------------------------------------------------------------------------

# P004 --- Hub de integração, capabilities e políticas

## P004-BE

Crie contrato `MarketplaceAdapter`, registro de capacidades por
marketplace e estados `SUPPORTED`, `UNSUPPORTED`, `RESTRICTED`,
`UNVERIFIED`. Crie cadastro de evidência por capacidade: URL oficial,
data de verificação, escopos, quotas, restrições, versão/documento e
responsável. Implemente interface de rate limiting, timeout, retry com
jitter, respeito a Retry-After, circuit breaker, bulkhead, idempotência
e kill switch. Não implemente scraping nem endpoints presumidos.

**Aceite:** adapter fake usado em testes; capability sem evidência fica
`UNVERIFIED` e não executa; kill switch bloqueia chamadas; retry não
excede política; logs mascaram dados sensíveis.

## P004-FE

Após backend aprovado, implemente tela de integrações/capacidades com
status, data da última verificação, permissões, erros sanitizados e
explicação de recursos indisponíveis. Não apresentar capacidade não
validada como disponível.

**Aceite:** estados distinguíveis; nenhum token aparece; ações
permitidas são somente conectar, atualizar status e revogar quando
suportado; mobile utilizável.

------------------------------------------------------------------------

# P005 --- Conexão oficial Mercado Livre

## P005-BE

Consulte documentação oficial vigente do Mercado Livre antes de
implementar. Valide programa de desenvolvedor, OAuth, redirect URI,
escopos, políticas, rate limits, renovação e revogação. Registre
evidências no inventário. Implemente authorization code flow conforme
documentação, state anti-CSRF, PKCE se suportado/recomendado, validação
de callback, cifragem de tokens por KMS/secret manager, refresh com lock
distribuído e rotação segura. Não use senha do seller, scraping
autenticado ou escopos além do necessário. Se credenciais/documentação
não estiverem disponíveis, pare e registre `OPEN_QUESTION`.

**Aceite:** fluxo completo em sandbox/test account autorizada; state
inválido é rejeitado; tokens cifrados; refresh concorrente não duplica;
revogação invalida conexão; auditoria sem token; testes de segurança
passam.

## P005-FE

Após backend aprovado, implemente fluxo de conectar Mercado Livre com
explicação de permissões, redirecionamento oficial, retorno de
sucesso/erro e status da conexão. Não solicitar senha na aplicação.

**Aceite:** usuário é redirecionado ao domínio oficial; callback não
expõe tokens; status reflete backend; falhas são compreensíveis;
responsividade aprovada.

------------------------------------------------------------------------

# P006 --- Conexão oficial Shopee

## P006-BE

Consulte documentação oficial vigente da Shopee Open Platform e confirme
requisitos de parceiro, autorização, assinatura, escopos, tokens,
quotas, webhooks e políticas. Não presuma que o fluxo seja igual ao
Mercado Livre. Registre evidências. Implemente somente operações
autorizadas e suportadas. Proteja app secret e tokens com secret
manager/KMS. Se não houver acesso aprovado à plataforma ou documentação
suficiente, mantenha adapter `UNVERIFIED` e bloqueie a implementação de
chamadas reais.

**Aceite:** conexão de teste autorizada; assinatura/callback validado
conforme documentação; tokens protegidos; revogação/expiração tratadas;
testes de quota e erro passam.

## P006-FE

Após backend aprovado, implemente conexão Shopee com orientações de
autorização e estado da integração. Não pedir credenciais da conta ao
usuário.

**Aceite:** redirecionamento/consentimento oficial; status consistente;
erro não revela segredo; UI responsiva.

------------------------------------------------------------------------

# P007 --- Catálogo próprio e normalização

## P007-BE

Implemente Product, ProductVariant, MarketplaceListing, SellerAccount e
mapeamentos de identificadores externos. Um produto canônico pode ter
vários anúncios; não deduplicar apenas por título. Preserve payload
original minimizado e permitido, com schema versionado, origem e
timestamps. Sincronize anúncios próprios pelos adapters aprovados, com
paginação, checkpoint, idempotência, reconciliação e isolamento tenant.
Não consultar concorrentes nesta feature.

**Aceite:** reprocessar a mesma página não duplica; produto e listing
permanecem distintos; dados de dois tenants isolados; falha parcial é
retomável; origem e data registradas.

## P007-FE

Após backend aprovado, crie catálogo com tabela responsiva, busca,
filtros por marketplace/conta/status e detalhe do anúncio. Exiba dados
ausentes claramente.

**Aceite:** paginação server-side; filtros combináveis;
loading/empty/error; detalhe correto; nenhum dado cross-tenant.

------------------------------------------------------------------------

# P008 --- Sincronização de pedidos próprios

## P008-BE

Implemente ingestão de pedidos e itens para cada adapter cuja capacidade
e escopo estejam comprovados. Use cursor/checkpoint, janela de
reconciliação, idempotency key baseada em identificador externo + conta,
upsert seguro, tratamento de cancelamentos/devoluções e outbox para
eventos internos. Não inferir dados ausentes. Respeite rate limits. Jobs
devem transportar tenant validado e ser reexecutáveis.

**Aceite:** reprocessamento não duplica; pedidos atualizam status
conforme fonte; falhas são recuperáveis; DLQ e reprocessamento
auditados; métricas de atraso/erro disponíveis.

## P008-FE

Após backend aprovado, implemente lista/detalhe de pedidos com filtros
por data, status, conta e canal. Exiba data de sincronização e sinalize
pedidos incompletos.

**Aceite:** filtros corretos; totais conciliam com backend; estados de
sincronização visíveis; responsividade aprovada.

------------------------------------------------------------------------

# P009 --- Motor de proveniência e normalização de mercado

## P009-BE

Implemente modelo comum para observações de mercado: produto/listing
observado, preço, condição, vendedor quando legalmente disponível,
marketplace, `source_type`, source URL/ID permitido, `collected_at`,
`observed_at`, validade, método, confiança e payload mínimo. Diferencie
`OFFICIAL`, `OBSERVED`, `ESTIMATED`, `USER_INPUT`. Não criar valores de
vendas, visitas, CTR ou faturamento sem fonte e metodologia. Implemente
deduplicação e matching com score explicável.

**Aceite:** todo dado tem origem e timestamp; estimativas exigem
metodologia/versionamento; matching não une automaticamente itens abaixo
do limiar; payload sensível é minimizado.

## P009-FE

Após backend aprovado, exiba origem, tipo de evidência, data e confiança
em cards/tabelas. Permita abrir explicação do dado.

**Aceite:** dado estimado nunca aparece como oficial; fonte e
atualização são visíveis; UI acessível.

------------------------------------------------------------------------

# P010 --- Pesquisa de mercado e concorrência autorizada

## P010-BE

Implemente pesquisa de produtos/anúncios somente usando capacidades
oficialmente suportadas, fontes licenciadas ou dados públicos cujo uso
tenha sido validado. Antes de cada endpoint/fonte, registrar política,
limites e permissão. Se a pesquisa concorrencial não estiver
autorizada/disponível via API, não contornar com scraping; oferecer
apenas dados próprios ou fonte alternativa licenciada. Implementar
filtros, paginação, normalização e cache com validade.

**Aceite:** cada resultado rastreável; nenhuma fonte não aprovada é
chamada; limites respeitados; resultados não disponíveis retornam
capability status, não dados fabricados.

## P010-FE

Após backend aprovado, crie busca por termo/categoria e resultados com
filtros, ordenação, comparação e badges de proveniência. Diferencie
anúncio próprio e concorrente quando a fonte permitir.

**Aceite:** filtros e paginação funcionam; fonte/data sempre visíveis;
mobile permite navegar tabela em cards ou scroll acessível.

------------------------------------------------------------------------

# P011 --- Comparação de preços e atributos

## P011-BE

Implemente comparação de listings equivalentes usando atributos, marca,
modelo, GTIN quando disponível e score de similaridade. Não comparar
itens incompatíveis sem aviso. Calcule variação de preço apenas entre
observações com janela temporal comparável. Não inferir preço final com
frete não conhecido.

**Aceite:** score e critérios explicáveis; itens incompatíveis são
sinalizados; cálculo reproduzível; histórico sem retroatividade
inventada.

## P011-FE

Após backend aprovado, crie comparação lado a lado, filtros e detalhe de
correspondência. Mostrar diferenças de atributos e nível de confiança.

**Aceite:** diferenças claras; baixa confiança destacada; sem falsa
equivalência visual.

------------------------------------------------------------------------

# P012 --- Monitoramento Hype: grupos e itens

## P012-BE

Implemente grupos por tenant e itens monitorados vinculados a
fonte/marketplace. Criar, editar, remover e listar grupos;
adicionar/remover itens. Histórico começa no primeiro snapshot efetivo
após inclusão. Agendamento respeita quotas, janela mínima, kill switch e
capacidade validada. Não buscar dado em fonte proibida.

**Aceite:** CRUD isolado; duplicidade controlada; histórico não
retroativo; jobs respeitam política e rate limit; exclusão segue
retenção definida.

## P012-FE

Após backend aprovado, implemente tela de grupos, detalhe, adicionar
item a partir da pesquisa, remoção com confirmação e timeline.

**Aceite:** estados vazios; filtros; confirmação; timeline mostra gaps e
última coleta; responsivo.

------------------------------------------------------------------------

# P013 --- Snapshots e histórico de preços

## P013-BE

Implemente snapshots idempotentes de preço e atributos disponíveis, com
timestamp, origem, validade e status de coleta. Criar política de
retenção e compressão/particionamento somente se volume justificar.
Detectar alteração por comparação com snapshot anterior. Não criar
snapshots se a fonte estiver indisponível.

**Aceite:** duplicatas evitadas; falhas registradas; timestamp correto;
histórico demonstra apenas dados coletados.

## P013-FE

Após backend aprovado, exiba gráfico histórico com intervalos, tooltip
de fonte e lacunas de coleta. Não interpolar visualmente dados ausentes
como observações reais.

**Aceite:** gráfico acessível; períodos corretos; lacunas
identificáveis; mobile funcional.

------------------------------------------------------------------------

# P014 --- Custos e regras de taxas

## P014-BE

Implemente cadastro de custos por produto/variante/empresa, moeda,
unidade, vigência e histórico. Implemente regras de comissão, tarifa
fixa, logística e outras cobranças com marketplace, modalidade,
categoria, faixa, fonte oficial/contratual, vigência e versão. Não
inventar tabela de taxas; iniciar sem regra quando não houver fonte
validada e solicitar input do usuário. Manter precisão decimal e
arredondamento documentado.

**Aceite:** regras vigentes selecionadas corretamente; histórico
preservado; ausência de taxa é explícita; alteração auditada; testes de
fronteira passam.

## P014-FE

Após backend aprovado, crie formulários de custos e regras com
validação, vigência e fonte. Alertar quando cálculo usar valor manual ou
taxa desatualizada.

**Aceite:** validação de campos; histórico visível; nenhuma regra não
verificada apresentada como oficial.

------------------------------------------------------------------------

# P015 --- Pricing Engine e calculadora de margem

## P015-BE

Implemente serviço determinístico de preço/margem. Entradas: preço,
custo, comissão, tarifa fixa, frete subsidiado, embalagem, impostos
configurados, descontos e outros custos aplicáveis. Defina formalmente
receita bruta, deduções, margem de contribuição em valor e percentual,
preço de equilíbrio, preço mínimo e preço-alvo. Usar BigDecimal, moeda,
escala e arredondamento explícitos. Não chamar LLM para calcular.
Retornar detalhamento de cada parcela, versão da fórmula e premissas.

**Aceite:** testes de referência aprovados; resultados reproduzíveis;
divisões por zero tratadas; margem de contribuição não rotulada como
lucro líquido; taxa ausente gera aviso/bloqueio conforme regra.

## P015-FE

Após backend aprovado, implemente calculadora interativa com inputs,
resultado detalhado, simulação de cenários e avisos de dados
ausentes/desatualizados.

**Aceite:** valores batem com API; acessível por teclado; validação
clara; layout mobile utilizável.

------------------------------------------------------------------------

# P016 --- Empresas/CNPJs e vínculo financeiro

## P016-BE

Implemente entidades Company/BusinessUnit com CNPJ validado
sintaticamente, sem assumir situação fiscal, regime ou alíquota.
Relacione explicitamente marketplace account a uma empresa. Proíba
vínculo ambíguo e mantenha histórico. Dados sempre tenant-scoped. Não
implementar gestão de usuários/equipes.

**Aceite:** conta vinculada a empresa válida; vínculo duplicado/ambíguo
bloqueado; auditoria; testes cross-tenant.

## P016-FE

Após backend aprovado, crie cadastro de empresa e tela de vínculo de
contas conectadas. Não expor CNPJ completo em logs ou listagens sem
necessidade.

**Aceite:** validação e máscara; vínculo explícito; permissões
aplicadas; mobile funcional.

------------------------------------------------------------------------

# P017 --- Consolidação financeira por pedidos

## P017-BE

Implemente modelo de fatos financeiros por pedido/item: receita,
desconto, cancelamento, devolução, comissão, tarifa, frete, custo do
produto, imposto informado e margem de contribuição. Diferenciar valores
confirmados, estimados e informados pelo usuário. Criar reconciliação
com origem externa e versão do cálculo. Não declarar lucro líquido se
despesas completas não estiverem modeladas.

**Aceite:** soma por item/pedido reconciliável; reprocessamento
idempotente; dados ausentes sinalizados; trilha de cálculo persistida.

## P017-FE

Após backend aprovado, crie painel financeiro com filtros por período,
empresa, canal, conta e SKU; detalhamento de componentes e alertas de
incompletude.

**Aceite:** totais reconciliam; filtros combináveis; definições dos
indicadores acessíveis; responsivo.

------------------------------------------------------------------------

# P018 --- Analytics de anúncios próprios

## P018-BE

Inventarie por marketplace quais métricas são disponibilizadas
oficialmente para a conta conectada, com escopo, janela, granularidade e
limites. Implemente ingestão somente para métricas comprovadas. CTR =
cliques/impressões e conversão = pedidos ou unidades/visitas somente se
denominadores e definição forem consistentes e documentados. Não
preencher métricas ausentes com estimativas silenciosas.

**Aceite:** catálogo de métricas documentado; ausência representada como
indisponível; fórmulas testadas; origem e janela registradas.

## P018-FE

Após backend aprovado, crie dashboard de analytics por anúncio e
período, com comparação temporal e aviso de métricas indisponíveis.

**Aceite:** período/timezone explícitos; cálculos conferem; gráficos não
mascaram dados ausentes.

------------------------------------------------------------------------

# P019 --- AI Gateway e assistente somente leitura

## P019-BE

Implemente gateway multi-provider com secrets em secret manager, limites
de custo/tokens, timeout, retry seguro, logging sem PII e versionamento
de prompts. Exponha tools internas tipadas para pesquisar dados,
consultar margem e resumir analytics. Toda tool valida tenant e
autorização independentemente do contexto do LLM. O modelo não recebe
SQL, tokens de marketplace ou acesso irrestrito. Defenda contra prompt
injection em títulos, descrições, reviews e fontes externas. Validar
saída por schema e retornar fontes/timestamps.

**Aceite:** tool não acessa outro tenant; prompt injection não altera
políticas; resposta sem dado informa indisponibilidade; custo/latência
monitorados; segredo nunca logado.

## P019-FE

Após backend aprovado, implemente assistente com histórico
tenant-scoped, indicação de fontes, data dos dados, aviso de estimativa
e feedback de resposta. Não oferecer botões de execução externa no MVP.

**Aceite:** fontes acessíveis; respostas não alegam ação executada;
erros e limites claros; acessível e responsivo.

------------------------------------------------------------------------

# P020 --- Geração de título e descrição como rascunho

## P020-BE

Implemente geração assistida a partir de atributos confirmados do
produto, categoria e palavras fornecidas. Restringir claims; não
inventar certificações, compatibilidade, material, dimensões, garantia
ou benefícios não comprovados. Permitir seleção de tom e limite de
caracteres por marketplace somente quando regra oficial validada. Salvar
versões e prompt/modelo. Nenhuma publicação externa.

**Aceite:** saída estruturada; atributos rastreáveis; limites validados;
conteúdo é rascunho; usuário pode editar/aprovar.

## P020-FE

Após backend aprovado, crie editor de rascunho com original/gerado,
comparação, contagem de caracteres quando aplicável, histórico de
versões e copiar/exportar.

**Aceite:** edição preservada; limite configurável; revisão humana
evidente; sem botão publicar no MVP.

------------------------------------------------------------------------

# P021 --- Relatórios e exportação

## P021-BE

Implemente exportação assíncrona CSV/XLSX dos dados autorizados do
tenant, com filtros persistidos, limites de linhas, expiração do
arquivo, URL temporária e auditoria. Sanitizar células contra CSV
formula injection. Storage privado, namespace tenant e autorização na
geração e download.

**Aceite:** exportação não cruza tenant; fórmulas perigosas
neutralizadas; link expira; job idempotente; auditoria registrada.

## P021-FE

Após backend aprovado, implemente seleção de colunas/filtros, progresso,
histórico e download autorizado. Não mostrar link permanente público.

**Aceite:** estados de job; download exige autorização; mobile permite
consultar status.

------------------------------------------------------------------------

# P022 --- Dashboard inicial e experiência responsiva

## P022-BE

Se necessário, criar endpoints agregados read-only com filtros e cache
tenant-aware. Não duplicar lógica financeira em controllers. Documentar
definições de cada KPI e invalidar cache em eventos relevantes.

**Aceite:** agregações conferem com fontes; cache não mistura tenant;
p95 medido; contrato documentado.

## P022-FE

Implemente dashboard com KPIs de pedidos, receita, margem de
contribuição, desempenho por canal, sincronização e alertas de qualidade
dos dados. Light mode padrão, dark mode opcional. Responsivo de 360 px a
desktop. Componentes com skeleton, empty, error e stale data.

**Aceite:** números batem com API; tema persiste; breakpoints testados;
WCAG nos fluxos principais; sem overflow.

------------------------------------------------------------------------

# P023 --- Testes E2E e homologação do MVP

## P023-BE/QA

Crie cenários E2E e de integração para: autenticar → tenant → conectar
marketplace sandbox/teste → sincronizar dados → consultar
catálogo/pedidos → simular margem → consultar dashboard → exportar.
Inclua falhas de OAuth, API indisponível, rate limit, token expirado,
retry, DLQ, duplicidade e acesso cross-tenant.

**Aceite:** pipeline reproduzível; evidências anexadas; nenhum cenário
crítico ignorado.

## P023-FE

Execute Playwright desktop/mobile, acessibilidade e regressão visual nos
fluxos principais. Não aprovar por screenshot isolado; validar
comportamento e dados.

**Aceite:** fluxos E2E passam em breakpoints; sem erro console crítico;
acessibilidade essencial aprovada.

------------------------------------------------------------------------

## Prompt de correção --- usar somente quando um prompt ficar BLOCKED

Analise o relatório de falha da feature atual. Não inicie outra feature.
Releia todos os documentos de governança e a spec. Reproduza o erro,
identifique causa raiz, proponha a correção mínima, implemente dentro do
escopo autorizado e execute novamente o teste falho e a regressão
pertinente. Não remova testes, reduza assertions, relaxe segurança ou
altere critérios para forçar aprovação. Se depender de decisão externa,
mantenha `BLOCKED`, registre opções e aguarde decisão humana. Só marque
`PASSED` com evidências de execução.
