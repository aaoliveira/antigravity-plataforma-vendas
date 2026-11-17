# Backlog do Projeto --- MVP e Evolução

**Convenções:** P0 = bloqueador/fundação; P1 = essencial para MVP; P2 =
pós-MVP prioritário; P3 = evolução.\
Cada item deve ser transformado em issue rastreável, com critérios de
aceite, dependências e evidências.

## Épico E00 --- Governança e SDD

  -------------------------------------------------------------------------------
  ID            História/entrega          Prioridade Dependências   Aceite
                                                                    resumido
  ------------- ------------------ ----------------- -------------- -------------
  E00-01        Criar baseline de                 P0 ---            Documentos
                contexto, PRD e                                     versionados e
                decisões                                            coerentes

  E00-02        Definir prompt                    P0 E00-01         Antigravity
                mestre e protocolo                                  verifica
                de execução                                         premissas
                                                                    antes de cada
                                                                    prompt

  E00-03        Criar status                      P0 E00-02         Bloqueia
                machine e quality                                   próximo
                gates                                               prompt se
                                                                    teste falhar

  E00-04        Criar ADR e log de                P0 E00-01         Decisões e
                execução                                            evidências
                                                                    rastreáveis
  -------------------------------------------------------------------------------

## Épico E01 --- Fundação técnica

  ---------------------------------------------------------------------------------
  ID            História/entrega          Prioridade Dependências   Aceite resumido
  ------------- ------------------ ----------------- -------------- ---------------
  E01-01        Monorepo ou                       P0 E00            Build
                repositório                                         reproduzível
                organizado                                          
                backend/frontend                                    

  E01-02        Backend Spring                    P0 E01-01         Módulos e
                Boot modular                                        boundaries
                                                                    testados

  E01-03        Frontend Next.js                  P0 E01-01         Shell de
                responsivo                                          aplicação
                                                                    responsivo

  E01-04        PostgreSQL e                      P0 E01-02         Migrações
                Flyway                                              reproduzíveis

  E01-05        Redis e RabbitMQ                  P1 E01-02         Health checks e
                locais                                              conexão

  E01-06        Observabilidade                   P0 E01-02         Logs, métricas,
                base                                                traces e
                                                                    correlation ID

  E01-07        CI com build,                     P0 E01-01         Pipeline
                testes, lint e                                      bloqueia falhas
                scanners                                            
  ---------------------------------------------------------------------------------

## Épico E02 --- Identidade mínima, tenant e segurança

  ---------------------------------------------------------------------------------
  ID            História/entrega          Prioridade Dependências    Aceite
                                                                     resumido
  ------------- ------------------ ----------------- --------------- --------------
  E02-01        Principal                         P0 E01             Tenant vem de
                autenticado e                                        identidade
                tenant context                                       confiável

  E02-02        RBAC mínimo e                     P0 E02-01          Acesso negado
                autorização por                                      por padrão
                recurso                                              

  E02-03        Isolamento tenant                 P0 E02-01,E01-04   Testes
                em aplicação e RLS                                   cross-tenant
                                                                     passam

  E02-04        Auditoria de                      P0 E02-01          Eventos
                operações                                            auditáveis sem
                sensíveis                                            segredos

  E02-05        Gestão de segredos                P0 E01             Nenhum segredo
                e criptografia de                                    em texto puro
                tokens                                               

  E02-06        Hardening OWASP e                 P0 E01-03,E02-01   ASVS baseline
                headers                                              verificada

  E02-07        Segurança de jobs,                P0 E02-03,E01-05   Testes de
                cache, filas e                                       isolamento
                arquivos por                                         passam
                tenant                                               
  ---------------------------------------------------------------------------------

## Épico E03 --- Hub de integração e políticas

  --------------------------------------------------------------------------------------
  ID            História/entrega                Prioridade Dependências    Aceite
                                                                           resumido
  ------------- ------------------------ ----------------- --------------- -------------
  E03-01        Contrato                                P0 E01-02          Capacidades
                MarketplaceAdapter e                                       explícitas
                Capability Registry                                        por canal

  E03-02        Registro de evidências                  P0 E03-01          Cada
                de política/escopos                                        capacidade
                                                                           possui fonte
                                                                           e data

  E03-03        OAuth/conexão Mercado                   P1 E02,E03-01      Conectar,
                Livre                                                      renovar,
                                                                           revogar

  E03-04        OAuth/conexão Shopee                    P1 E02,E03-01      Conectar,
                                                                           renovar,
                                                                           revogar

  E03-05        Rate limiter, backoff,                  P0 E03-01          Respeita
                circuit breaker                                            quotas e
                                                                           Retry-After

  E03-06        Observabilidade e health                P1 E03-03,E03-04   Status e
                das conexões                                               erros
                                                                           visíveis

  E03-07        Kill switch por                         P0 E03-01          Operação
                marketplace/capacidade                                     bloqueável
                                                                           sem deploy
  --------------------------------------------------------------------------------------

## Épico E04 --- Catálogo e pedidos próprios

  ----------------------------------------------------------------------------------
  ID            História/entrega          Prioridade Dependências    Aceite resumido
  ------------- ------------------ ----------------- --------------- ---------------
  E04-01        Modelo Product,                   P1 E02,E03         Produto
                Listing, Variant e                                   distinto de
                SellerAccount                                        anúncio

  E04-02        Sincronizar                       P1 E03-03,E04-01   Paginação e
                anúncios próprios                                    idempotência
                ML                                                   

  E04-03        Sincronizar                       P1 E03-04,E04-01   Paginação e
                anúncios próprios                                    idempotência
                Shopee                                               

  E04-04        Sincronizar                       P1 E03-03          Sem duplicidade
                pedidos e itens ML                                   

  E04-05        Sincronizar                       P1 E03-04          Sem duplicidade
                pedidos e itens                                      
                Shopee                                               

  E04-06        Reconciliação e                   P1 E04-04,E04-05   Divergências
                reprocessamento                                      identificadas

  E04-07        Frontend de                       P1 E04-02..06      Fluxos
                conexões, catálogo                                   responsivos e
                e pedidos                                            estados claros
  ----------------------------------------------------------------------------------

## Épico E05 --- Inteligência de mercado e concorrência

  -----------------------------------------------------------------------------------
  ID            História/entrega          Prioridade Dependências   Aceite resumido
  ------------- ------------------ ----------------- -------------- -----------------
  E05-01        Pesquisa por                      P1 E03            Não usa scraping
                fonte/capacidade                                    proibido
                autorizada                                          

  E05-02        Normalização e                    P1 E05-01         Fonte, data e
                proveniência de                                     tipo de evidência
                resultados                                          

  E05-03        Comparação de                     P1 E05-02         Comparabilidade e
                preços e atributos                                  confiança
                                                                    visíveis

  E05-04        Perfil de                         P2 E05-02         Não inferir dados
                concorrente quando                                  privados
                permitido                                           

  E05-05        Categorias e                      P2 E05-02         Metodologia e
                tendências com                                      período
                fonte                                               documentados

  E05-06        Frontend pesquisa,                P1 E05-01..03     Paginação e
                filtros e                                           responsividade
                comparação                                          

  E05-07        Relatório de                      P2 E05-03,E09     Score explicado,
                oportunidade                                        sem falsa
                explicável                                          precisão
  -----------------------------------------------------------------------------------

## Épico E06 --- Monitoramento de produtos

  ------------------------------------------------------------------------------------
  ID            História/entrega            Prioridade Dependências    Aceite resumido
  ------------- -------------------- ----------------- --------------- ---------------
  E06-01        Grupos e itens                      P1 E05-02          CRUD isolado
                monitorados                                            por tenant

  E06-02        Agendamento de                      P1 E03-05,E06-01   Respeita
                snapshots                                              limites e
                                                                       política

  E06-03        Histórico de preço e                P1 E06-02          Sem histórico
                atributos                                              retroativo
                                                                       inventado

  E06-04        Alertas de variação                 P2 E06-03,E14      Regras
                                                                       configuráveis

  E06-05        Frontend                            P1 E06-01..03      Gráficos e
                Hype/monitoramento                                     estados vazios
  ------------------------------------------------------------------------------------

## Épico E07 --- Pricing e lucratividade

  ---------------------------------------------------------------------------------
  ID            História/entrega          Prioridade Dependências    Aceite
                                                                     resumido
  ------------- ------------------ ----------------- --------------- --------------
  E07-01        Cadastro de custos                P1 E02             Histórico e
                por SKU/empresa                                      vigência

  E07-02        Regras de                         P1 E03             Fonte e
                taxas/comissões                                      vigência
                versionadas                                          obrigatórias

  E07-03        Motor                             P1 E07-01,E07-02   Testes de
                determinístico de                                    referência
                margem                                               

  E07-04        Simulador de preço                P1 E07-03          Premissas
                mínimo/alvo                                          explícitas

  E07-05        Configuração                      P1 E07-03          Não substitui
                tributária                                           orientação
                informativa                                          fiscal

  E07-06        Frontend                          P1 E07-03,E07-04   Cálculo
                calculadora e                                        reproduzível
                tabela de margem                                     
  ---------------------------------------------------------------------------------

## Épico E08 --- Controle financeiro

  -------------------------------------------------------------------------------------------
  ID            História/entrega                   Prioridade Dependências   Aceite resumido
  ------------- --------------------------- ----------------- -------------- ----------------
  E08-01        Cadastro de empresa/CNPJ e                 P1 E02,E03        Conta vinculada
                vínculo de conta                                             explicitamente

  E08-02        Modelo financeiro de pedido                P1 E04,E07        Componentes
                e liquidação                                                 separados

  E08-03        Receita, taxas, custos e                   P1 E08-02         Reconciliação
                margem por pedido                                            auditável

  E08-04        Dashboards por                             P1 E08-03         Filtros corretos
                período/canal/empresa/SKU                                    

  E08-05        Métricas Ads ROAS/ROI                      P2 Dados de Ads   Não exibe sem
                condicionais                                                 dados
                                                                             suficientes

  E08-06        Frontend Control                           P1 E08-01..04     Visões
                                                                             consolidadas e
                                                                             detalhadas
  -------------------------------------------------------------------------------------------

## Épico E09 --- Analytics de anúncios próprios

  -----------------------------------------------------------------------------------
  ID            História/entrega          Prioridade Dependências   Aceite resumido
  ------------- ------------------ ----------------- -------------- -----------------
  E09-01        Inventário de                     P1 E03            Evidência de
                métricas por                                        API/escopo
                marketplace                                         

  E09-02        Ingestão de                       P1 E09-01         Sem métricas
                métricas                                            inventadas
                disponíveis                                         

  E09-03        CTR/conversão                     P2 E09-02         Fórmulas e
                derivados quando                                    denominadores
                aplicável                                           explícitos

  E09-04        Comparação                        P1 E09-02         Janelas
                temporal                                            equivalentes

  E09-05        Frontend Analytics                P1 E09-02..04     Disponibilidade
                                                                    claramente
                                                                    indicada
  -----------------------------------------------------------------------------------

## Épico E10 --- IA assistiva e conteúdo

  --------------------------------------------------------------------------------
  ID            História/entrega          Prioridade Dependências    Aceite
                                                                     resumido
  ------------- ------------------ ----------------- --------------- -------------
  E10-01        AI Gateway e                      P1 E01,E02         Chaves
                provider                                             protegidas e
                abstraction                                          limites

  E10-02        Tool calling para                 P1 E05,E07,E08     Ferramentas
                dados internos                                       autorizadas
                                                                     por tenant

  E10-03        Assistente somente                P1 E10-01,E10-02   Fontes e
                leitura                                              limitações na
                                                                     resposta

  E10-04        Gerar                             P2 E10-01,E04      Sem atributos
                título/descrição                                     inventados
                como rascunho                                        

  E10-05        Análise de reviews                P2 Fonte           PII
                permitidos                           autorizada      minimizada

  E10-06        Frontend                          P2 E10-03,E10-04   Revisão
                assistente e                                         humana
                editor de                                            obrigatória
                rascunhos                                            
  --------------------------------------------------------------------------------

## Épico E11 --- Relatórios e interface transversal

  --------------------------------------------------------------------------------
  ID            História/entrega          Prioridade Dependências   Aceite
                                                                    resumido
  ------------- ------------------ ----------------- -------------- --------------
  E11-01        Design system e                   P0 E01-03         Light/dark
                tokens de tema                                      consistente

  E11-02        Layout responsivo                 P0 E11-01         360px+ sem
                e navegação                                         overflow
                                                                    crítico

  E11-03        Dashboard inicial                 P1 E08,E09        Indicadores
                                                                    com origem

  E11-04        Exportação                        P1 E05,E08,E09    Escopo tenant
                CSV/XLSX                                            e auditoria

  E11-05        Notificações                      P2 E06,E14        Sem dados
                in-app                                              cross-tenant
  --------------------------------------------------------------------------------

## Épico E12 --- Segurança, testes e release

  -----------------------------------------------------------------------------------
  ID            História/entrega             Prioridade Dependências   Aceite
                                                                       resumido
  ------------- --------------------- ----------------- -------------- --------------
  E12-01        Testes unitários e                   P0 Todos          Regras
                arquitetura                                            críticas
                                                                       cobertas

  E12-02        Testes                               P0 E03,E04        Contratos e
                integração/contrato                                    adapters
                                                                       validados

  E12-03        Testes cross-tenant                  P0 E02            Zero acesso
                                                                       indevido

  E12-04        Testes E2E                           P0 Fluxos MVP     Jornadas
                desktop/mobile                                         completas
                                                                       passam

  E12-05        SAST, SCA, secret                    P0 E01            Pipeline
                scan, container scan                                   bloqueia
                                                                       severidade
                                                                       crítica/alta

  E12-06        Backup/restore e                     P1 Infra          Restore
                runbooks                                               comprovado

  E12-07        Homologação e release                P0 Todos          Sem
                checklist                                              bloqueadores
                                                                       abertos
  -----------------------------------------------------------------------------------

------------------------------------------------------------------------

## Ordem sugerida de execução

1.  E00 governança.
2.  E01 fundação.
3.  E02 identidade/tenant/segurança.
4.  E03 hub de integração.
5.  E04 catálogo e pedidos.
6.  E07 pricing (pode avançar em paralelo apenas se o orquestrador
    permitir trilhas independentes; no modo estritamente sequencial,
    após E04).
7.  E05 inteligência de mercado.
8.  E06 monitoramento.
9.  E08 controle financeiro.
10. E09 analytics.
11. E10 IA.
12. E11 relatórios/interface.
13. E12 hardening e release.

**Regra:** dependências e quality gates prevalecem sobre a ordem
nominal. Nenhum item dependente inicia com predecessor em `BLOCKED`.

## Definition of Ready (DoR)

Uma história só pode iniciar se: - objetivo e escopo estão claros; -
dependências estão `PASSED`; - critérios de aceite são verificáveis; -
políticas/escopos da API foram validados quando aplicável; - dados de
entrada e saída estão definidos; - riscos e dúvidas bloqueantes foram
resolvidos; - testes esperados estão descritos.

## Definition of Done (DoD)

-   implementação backend/frontend conforme escopo;
-   testes unitários, integração, contrato e E2E pertinentes aprovados;
-   segurança e isolamento validados;
-   lint, build e scanners aprovados;
-   observabilidade adicionada;
-   documentação e OpenAPI atualizadas;
-   ADR/Decision Log atualizado se houve decisão;
-   evidências anexadas ao log;
-   status `PASSED` atribuído pelo gate, não por autodeclaração do
    agente.
