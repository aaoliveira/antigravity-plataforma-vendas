# Biblioteca de Prompts --- Funcionalidades de Evolução

**Importante:** estes prompts não pertencem ao MVP inicial. Executar
somente depois de o MVP estar homologado e após revalidar documentação,
políticas e disponibilidade das fontes. Cada item possui Backend e
Frontend separados. Se a funcionalidade não for suportada por fonte
autorizada, o resultado correto é `UNSUPPORTED`/`BLOCKED`, não scraping
ou inferência.

## E001 --- Amazon SP-API

**BE:** Validar programa SP-API, roles, autorização, tokens, endpoints,
quotas, relatórios e políticas vigentes em documentação oficial.
Implementar adapter isolado e capabilities específicas; sincronizar
somente dados próprios autorizados. Proteger refresh tokens, tratar
relatórios assíncronos, polling com limites e processamento
idempotente.\
**Aceite:** autorização aprovada; escopos mínimos; relatório tratado
conforme ciclo oficial; sem scraping; auditoria e testes de quota.\
**FE:** conectar Amazon, mostrar roles/permissões, estado de relatórios
e capacidades reais.\
**Aceite:** status fiel ao backend; sem prometer métricas não
acessíveis.

## E002 --- TikTok Shop

**BE:** validar requisitos de Partner Center, autorização, escopos,
APIs, webhooks, rate limits e política regional. Implementar apenas
capabilities comprovadas. Assinatura de webhook, replay protection,
idempotência e verificação de origem conforme documentação.\
**Aceite:** eventos autenticados; duplicatas ignoradas; capacidades
documentadas.\
**FE:** conexão, status, catálogo/pedidos/analytics apenas se
suportados.\
**Aceite:** exibir limitações por região/escopo.

## E003 --- Magalu

**BE:** validar credenciamento, OAuth/chaves, contratos, APIs de
produto/pedido/financeiro/Ads e limites. Criar adapter sem acoplar
domínio aos DTOs do fornecedor.\
**Aceite:** contrato oficial validado; sincronização idempotente; rate
limit e auditoria.\
**FE:** tela de conexão e visão dos dados efetivamente disponíveis.\
**Aceite:** nenhuma métrica não suportada é exibida.

## E004 --- SHEIN

**BE:** validar programa de Open Platform, aprovação da conta, escopos,
limitações por seller/região e restrições de métricas. Bloquear
analytics não disponibilizado.\
**Aceite:** inventário de capacidades com evidência; nenhuma coleta
proibida; dados próprios sincronizados conforme permissão.\
**FE:** conexão e indicadores de disponibilidade/limitação.\
**Aceite:** transparência sobre dados ausentes.

## E005 --- Tendências de categoria

**BE:** implementar agregações sobre fontes licenciadas/autorizadas, com
janela temporal, cobertura, tamanho da amostra, metodologia e confiança.
Não chamar amostra limitada de "mercado total".\
**Aceite:** metodologia reproduzível; amostra e cobertura visíveis; sem
estimativa sem base.\
**FE:** dashboard de categorias, crescimento e filtros por
canal/período.\
**Aceite:** contexto metodológico sempre acessível.

## E006 --- Score de oportunidade de produto

**BE:** implementar score explicável com dimensões configuráveis
(demanda, concorrência, margem, tendência, logística e qualidade da
evidência). Exibir pesos, dados ausentes e sensibilidade. Não apresentar
score como previsão garantida.\
**Aceite:** score reproduzível; baixa confiança reduz score ou bloqueia
classificação; pesos versionados.\
**FE:** ranking de oportunidades com explicação por dimensão.\
**Aceite:** usuário consegue entender fatores e limitações.

## E007 --- Buy Box e catálogo

**BE:** validar se API oficial oferece elegibilidade, posição, preço e
participantes. Implementar apenas campos autorizados; não inferir
vencedor com dados incompletos.\
**Aceite:** fonte e momento da consulta explícitos; nenhum dado privado
inferido.\
**FE:** comparação de ofertas e histórico quando suportado.\
**Aceite:** atualização e limitações visíveis.

## E008 --- Análise avançada de concorrentes

**BE:** implementar perfil concorrencial somente com dados
públicos/licenciados. Não coletar dados pessoais desnecessários nem
contornar controles. Vendas/faturamento só podem ser exibidos como
estimativa se houver metodologia validada.\
**Aceite:** proveniência, base legal/termos e confiança registrados;
estimativas identificadas.\
**FE:** perfil e comparação com filtros.\
**Aceite:** distinção clara entre observado e estimado.

## E009 --- Keywords e SEO interno

**BE:** obter termos apenas de API/fonte autorizada ou entrada do
usuário. Registrar volume, janela, fonte e definição. Não afirmar volume
de busca quando só houver frequência de termos em títulos.\
**Aceite:** definição da métrica explícita; origem e janela presentes.\
**FE:** painel de termos, relevância e sugestões de título.\
**Aceite:** recomendação explicável e revisável.

## E010 --- Ranking orgânico

**BE:** validar fonte permitida e método de coleta. Ranking varia por
localização, personalização, sessão e horário; registrar contexto. Não
automatizar scraping proibido.\
**Aceite:** contexto de observação e limitações; nenhuma posição
apresentada como universal.\
**FE:** histórico de posição com contexto e lacunas.\
**Aceite:** tooltip de contexto e fonte.

## E011 --- Avaliações e quebra de objeções

**BE:** validar acesso e direitos de uso de reviews. Minimizar dados
pessoais; classificar sentimento/temas com IA e manter evidência textual
permitida. Nunca fabricar review.\
**Aceite:** fonte autorizada; PII removida/minimizada; classificação
auditável.\
**FE:** painel de temas positivos/negativos e objeções com exemplos
permitidos.\
**Aceite:** amostra, fonte e limitações visíveis.

## E012 --- Analytics avançado de anúncios

**BE:** ampliar inventário de métricas oficiais por marketplace,
armazenar granularidade e atribuição. Implementar CTR, conversão, taxa
de cancelamento e outras fórmulas somente com definições consistentes.\
**Aceite:** contrato de métrica documentado; denominadores válidos;
testes de cálculo.\
**FE:** dashboards por listing, SKU, canal e período.\
**Aceite:** filtros e definições disponíveis.

## E013 --- Testes de conteúdo/experimentos

**BE:** implementar experimento sequencial antes/depois como baseline,
registrando mudança, timestamp, janela, sazonalidade e fatores de
confusão. Só implementar A/B randomizado se plataforma fornecer
mecanismo válido.\
**Aceite:** não declarar causalidade sem desenho experimental;
resultados com limitações.\
**FE:** criar experimento, registrar variante e acompanhar indicadores.\
**Aceite:** comparação mostra janela e ressalvas.

## E014 --- Geração avançada de anúncios

**BE:** gerar rascunhos estruturados por marketplace com atributos
verificados, limites oficiais e validação de claims. Não publicar
automaticamente.\
**Aceite:** conteúdo sem atributos inventados; validação por canal;
revisão humana.\
**FE:** wizard de ficha técnica, título, descrição e revisão.\
**Aceite:** cada etapa editável e salvável.

## E015 --- Editor de imagens

**BE:** integrar provedor de processamento de imagem com validação de
arquivo, limites, antivírus quando aplicável, storage privado, remoção
segura e controle de custo. Não alterar produto de modo enganoso.\
**Aceite:** arquivos isolados por tenant; MIME real validado; metadados
removidos quando apropriado; custos limitados.\
**FE:** upload, comparação original/resultado, edição e exportação.\
**Aceite:** acessível, responsivo, sem URL pública permanente.

## E016 --- Análise de fornecedores

**BE:** importar catálogo fornecido pelo usuário, normalizar
SKU/GTIN/custo e cruzar somente com dados de mercado autorizados.
Calcular margem pelo Pricing Engine.\
**Aceite:** origem dos custos; matching com confiança; nenhum dado de
mercado inventado.\
**FE:** importação CSV/XLSX, mapeamento de colunas, relatório
comparativo.\
**Aceite:** erros por linha; preview antes de processar.

## E017 --- MCP Server somente leitura

**BE:** implementar MCP com autenticação e autorização tenant-scoped,
tools allowlisted, schemas estritos, limites, audit logs e somente
leitura. Nunca expor SQL genérico, credenciais ou tools de escrita.
Validar protocolo/SDK oficial vigente.\
**Aceite:** tool cross-tenant negada; limites aplicados; logs; testes de
prompt injection e autorização.\
**FE:** tela de configuração/credenciais e documentação de conexão, sem
revelar segredo novamente.\
**Aceite:** rotação/revogação; escopo read-only.

## E018 --- Alertas e notificações

**BE:** regras configuráveis para variação de preço, queda de margem,
sincronização falha e oportunidades; deduplicação, cooldown, preferência
e canal autorizado.\
**Aceite:** sem spam; tenant isolation; auditoria e entrega
idempotente.\
**FE:** central de notificações, preferências e histórico.\
**Aceite:** filtros, lido/não lido e acessibilidade.

## E019 --- Automação de promoções

**BE:** não implementar ação automática até haver API oficial e
autorização explícita documentadas. Primeira etapa é somente
recomendação/simulação; qualquer execução futura exige ADR, opt-in
granular, limite de margem, confirmação, kill switch e auditoria.\
**Aceite:** por padrão nenhuma ação write; simulação não executa
alteração.\
**FE:** painel de recomendações e simulação.\
**Aceite:** CTA deixa claro que é sugestão, não ação realizada.

## E020 --- Automação de preço/publicação

**BE:** fora do MVP. Só iniciar após validação documental específica por
marketplace, permissões, riscos e consentimento. Exigir dry-run,
limites, aprovação humana, idempotência, rollback quando possível e kill
switch.\
**Aceite:** nenhuma escrita sem autorização comprovada; trilha
completa.\
**FE:** workflow de aprovação/dry-run somente após backend aprovado.\
**Aceite:** preview e confirmação explícita.

## E021 --- Mobile/PWA

**BE:** revisar APIs, autenticação e notificações push com segurança;
não criar endpoint privilegiado para mobile.\
**Aceite:** mesmos controles tenant e rate limit.\
**FE:** PWA responsiva instalável, navegação mobile, cache seguro e
estados offline sem expor dados sensíveis.\
**Aceite:** testes em dispositivos e acessibilidade.

## E022 --- Previsão estatística

**BE:** implementar modelos somente após histórico suficiente, baseline,
validação temporal, backtesting, intervalo de confiança e monitoramento
de drift. Separar previsão de observação.\
**Aceite:** métrica de erro documentada; baseline comparativo; modelo
versionado; previsão sinalizada.\
**FE:** visualização de previsão com intervalo e explicação.\
**Aceite:** nunca representar previsão como venda garantida.

------------------------------------------------------------------------

## Catálogo de funcionalidades aprendidas e rastreabilidade

  -----------------------------------------------------------------------
  Capacidade                          Prompt
  ----------------------------------- -----------------------------------
  Pesquisa de produtos/anúncios       P010

  Análise primeira/mais páginas       P010, condicionada à fonte
                                      autorizada

  Perfil de concorrentes/vendedores   E008

  Preço atual e comparação            P011

  Vendas/faturamento estimados        E008, somente com metodologia
                                      validada

  Reputação do vendedor               E008, se público e permitido

  Idade do anúncio                    P009/P010, se disponível

  Catálogo/Buy Box                    E007

  Logística/frete                     P011, se dados disponíveis

  Concentração de vendas              E005/E008, com amostra declarada

  Categorias e tendências             E005

  Oportunidade/score                  E006

  Monitoramento/grupos                P012

  Histórico de preço                  P013

  Histórico de                        E010/E012/E011, conforme fonte
  visitas/vendas/ranking/reviews      

  CTR/conversão                       P018/E012, somente com métricas
                                      oficiais

  Palavras-chave                      E009

  Avaliações/download/análise         E011, respeitando direitos/termos

  Quebra de objeções                  E011

  Analytics e recomendações de        E012
  anúncio                             

  Geração de título/descrição         P020/E014

  Publicação de anúncio               E020, fora do MVP e condicionada a
                                      autorização

  Geração/edição de imagens           E015

  Teste A/B                           E013, somente se tecnicamente
                                      suportado

  Calculadora de contribuição         P015

  Simulação de preço                  P015

  Planilha de lucratividade           P017/P021

  Configuração tributária             P014/P015, com orientação fiscal do
                                      usuário

  Multi-CNPJ                          P016

  Pedidos multi-marketplace           P008

  Métricas financeiras consolidadas   P017

  ROI/ROAS Ads                        E012/E08-05, quando dados e
                                      atribuição existirem

  Aceite de promoções                 E019, recomendação apenas
                                      inicialmente

  Impulsionamento/seguir usuários     Não implementar sem API e política
                                      expressamente autorizadas; não faz
                                      parte do roadmap padrão

  Gerador EAN/GTIN                    Não gerar códigos oficiais
                                      arbitrários; somente integração com
                                      entidade emissora autorizada ou
                                      validação de código fornecido

  Fornecedores                        E016

  MCP/assistente                      P019/E017

  App mobile                          E021

  Notificações                        E018

  Exportações                         P021
  -----------------------------------------------------------------------
