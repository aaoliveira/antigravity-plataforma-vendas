# PRD — Plataforma de Inteligência de Mercado e Gestão para Sellers

**Nome do produto:** Seller Intelligence Platform (nome provisório)  
**Versão do documento:** 1.0  
**Data:** 17 de novembro de 2026  
**Status:** Proposta inicial para validação de produto, negócio, dados e arquitetura  
**Tipo:** Product Requirements Document (PRD)  
**Referências de benchmark:** Avantpro (referência funcional primária), JoomPulse e GoSmarter  
**Escopo comercial excluído:** planos, assinaturas, precificação comercial e cobrança SaaS.

> **Nota de enquadramento:** este PRD transforma o inventário público de funcionalidades apresentado na análise comparativa em uma especificação de produto. Ele não afirma acesso a funcionalidades privadas, APIs não documentadas ou métricas internas dos concorrentes. A disponibilidade real de dados e permissões de cada marketplace deverá ser validada antes de assumir compromissos de produto. O documento é uma baseline de produto sujeita às decisões registradas em `OPEN_QUESTIONS` e `DECISION_LOG`.

---

# 1. Resumo executivo

A Seller Intelligence Platform será uma plataforma SaaS multiempresa para vendedores de e-commerce e marketplaces. O produto reunirá inteligência de mercado, análise competitiva, descoberta de produtos, criação e otimização de anúncios com IA, gestão financeira, análise de publicidade, monitoramento, alertas e assistência conversacional por MCP.

O produto terá como referência funcional primária o conjunto divulgado pelo Avantpro: análise de anúncios e concorrência, recursos de criação e otimização com IA, controle financeiro multi-loja/multi-CNPJ e integração conversacional via MCP. A JoomPulse será referência complementar para busca avançada, tendências, sazonalidade, monitoramento de produtos e vendedores, pesquisa visual e leitura de catálogos de fornecedores. A GoSmarter será referência complementar para gestão operacional, eficiência de Ads, DRE, conciliação financeira, alertas, tarefas e diagnóstico de operação.

A proposta de valor é conectar o ciclo completo do seller: **descobrir uma oportunidade → validar mercado e margem → criar o anúncio → publicar ou preparar a publicação → monitorar o desempenho → otimizar → conciliar os recebimentos → decidir a próxima ação**.

A primeira entrega priorizará Mercado Livre e Shopee, inteligência de mercado compatível com as fontes legalmente disponíveis, controle financeiro básico e assistente de análise de anúncios. Amazon, scanner de catálogos, pesquisa por imagem, publicidade avançada, conciliação aprofundada e MCP ampliado entrarão em ondas posteriores, conforme validação técnica e comercial.

## 1.1 Resultados de negócio pretendidos

- Reduzir o tempo gasto para pesquisar e validar produtos.
- Aumentar a qualidade e a competitividade dos anúncios.
- Dar visibilidade à margem de contribuição e ao lucro por SKU, loja, marketplace e empresa.
- Detectar mudanças relevantes em concorrentes, preços, demanda e campanhas.
- Transformar insights em tarefas rastreáveis.
- Diminuir divergências entre vendas, repasses e resultado financeiro.
- Permitir consultas em linguagem natural com respostas rastreáveis às fontes de dados.

## 1.2 Princípios de produto

1. **Precisão e transparência:** dado observado, dado importado e estimativa devem ser claramente diferenciados.
2. **Fonte autorizada primeiro:** priorizar APIs oficiais, integrações autorizadas, dados fornecidos pelo próprio usuário e fontes licenciadas. Não contornar autenticação, controles técnicos ou termos de uso.
3. **Ação com controle humano:** ações que alterem anúncios, preços, campanhas, pedidos ou dados financeiros exigem autorização explícita e trilha de auditoria.
4. **Financeiro determinístico:** cálculos monetários são feitos por regras versionadas e testáveis, não pela interpretação livre de um LLM.
5. **Multi-tenant desde o início:** isolamento de dados entre organizações é requisito estrutural.
6. **Arquitetura evolutiva:** começar com monólito modular e processamento assíncrono; separar serviços apenas quando houver necessidade comprovada.
7. **Marketplace-agnostic:** regras específicas de cada canal ficam encapsuladas em conectores e políticas próprias.
8. **IA fundamentada:** recomendações devem indicar evidências, data, limitações e grau de confiança.

---

# 2. Contexto, problema e oportunidade

## 2.1 Problemas do usuário

Vendedores que operam em um ou mais marketplaces enfrentam dados dispersos, baixa visibilidade sobre concorrência, dificuldade em estimar a atratividade de novos produtos e esforço manual para construir anúncios competitivos. Em paralelo, taxas, frete, impostos, descontos, devoluções e publicidade tornam difícil conhecer a rentabilidade real por produto e canal.

As ferramentas disponíveis tendem a resolver partes distintas do processo. Oportunidade identificada: oferecer uma experiência integrada que una pesquisa de mercado, execução comercial e controle financeiro, com dados e recomendações contextualizados pela operação de cada organização.

## 2.2 Hipóteses de produto a validar

- H1: sellers valorizam uma visão unificada de mercado, anúncios e lucratividade.
- H2: filtros avançados e monitoramento histórico reduzem o tempo de descoberta de oportunidades.
- H3: o cruzamento entre margem e Ads é mais útil que relatórios isolados de ROAS/ACOS.
- H4: recomendações de IA com evidências e próximos passos aumentam a taxa de execução.
- H5: o scanner de catálogos e a pesquisa por imagem reduzem o tempo entre receber uma oferta de fornecedor e decidir se vale anunciar.

## 2.3 Métricas de sucesso do produto

Metas numéricas definitivas deverão ser estabelecidas após baseline e piloto. Os indicadores iniciais são:

| Dimensão | Indicador | Definição operacional |
|---|---|---|
| Ativação | Organizações conectadas | Organizações que conectaram pelo menos uma conta de marketplace e importaram dados válidos. |
| Adoção | Usuários ativos semanais | Usuários que executaram uma ação de valor em uma semana. |
| Inteligência | Pesquisas concluídas | Pesquisas de produto/categoria finalizadas com fonte e data disponíveis. |
| Descoberta | Oportunidades salvas | Produtos classificados e salvos pelo usuário para avaliação ou sourcing. |
| Anúncios | Anúncios analisados | Análises concluídas com score, critérios e recomendações. |
| Financeiro | SKUs com margem calculada | SKUs com custo e componentes financeiros suficientes para cálculo. |
| Qualidade financeira | Divergências de conciliação identificadas | Divergências classificadas entre valor esperado e recebido. |
| Execução | Recomendações convertidas em tarefas | Recomendações aceitas e transformadas em tarefas. |
| Confiabilidade | Sucesso de sincronização | Percentual de execuções de sincronização concluídas sem erro não recuperado. |
| IA | Respostas fundamentadas | Respostas com referências a dados/fontes, dentro do conjunto de consultas avaliadas. |

---

# 3. Usuários, personas e permissões

## 3.1 Personas principais

| Persona | Objetivos | Necessidades | Dores atuais |
|---|---|---|---|
| Seller individual | Encontrar produtos e vender com margem | Pesquisa simples, análise de anúncios, preço e lucro | Dados fragmentados e falta de tempo |
| Gestor de operação | Gerenciar várias lojas e marketplaces | Visão consolidada, alertas, tarefas e desempenho por canal | Operação distribuída e retrabalho |
| Analista de mercado | Pesquisar categorias e concorrentes | Filtros, histórico, exportação e comparações | Limitações de pesquisa e dados sem histórico |
| Financeiro/contábil | Apurar resultado e conciliar repasses | Pedidos, custos, taxas, impostos, devoluções e DRE | Divergência entre venda e recebimento |
| Gestor de tráfego/Ads | Melhorar eficiência de campanhas | ROAS, ACOS, TACOS, CTR, CPC e margem atribuída | Métricas de Ads desconectadas da lucratividade |
| Administrador da organização | Controlar acesso e estrutura empresarial | Usuários, CNPJs, contas, permissões e auditoria | Falta de governança multiempresa |

## 3.2 Modelo organizacional

A hierarquia lógica será:

`Organização (tenant) → Empresas/CNPJs → Lojas/contas de marketplace → Produtos/SKUs, anúncios, pedidos e campanhas`.

Uma organização poderá possuir uma ou mais empresas, cada empresa poderá possuir várias contas de marketplace e cada conta poderá representar uma loja/canal. O usuário poderá ter acesso a uma ou mais empresas e contas, conforme suas permissões.

## 3.3 Papéis e permissões iniciais

| Papel | Permissões principais |
|---|---|
| Owner | Controle total da organização, usuários, empresas, integrações e configurações. |
| Admin | Administração operacional, usuários e integrações, sem transferência de propriedade. |
| Manager | Acesso às áreas operacionais e relatórios das empresas/lojas autorizadas. |
| Analyst | Leitura, pesquisas, análises, relatórios e exportações permitidas. |
| Finance | Custos, pedidos, margens, conciliação e relatórios financeiros autorizados. |
| Operator | Produtos, anúncios, atendimento e tarefas operacionais autorizadas. |
| Viewer | Somente leitura, sem exportações sensíveis por padrão. |

Permissões deverão ser avaliadas no backend em toda operação. Ocultar controles na interface não constitui autorização.

---

# 4. Escopo do produto

## 4.1 Dentro do escopo

- Aplicação web responsiva.
- Extensão Chrome/Chromium como interface complementar de contexto.
- Multi-tenant, multiusuário, multiempresa, multi-CNPJ e multi-conta.
- Integrações com Mercado Livre e Shopee no MVP, condicionadas à disponibilidade e autorização dos dados requeridos.
- Camada de conectores preparada para Amazon e demais marketplaces.
- Pesquisa e análise de produtos, categorias e concorrência.
- Monitoramento de produtos e vendedores.
- Motor financeiro determinístico.
- Análise e criação assistida de anúncios com IA.
- Gestão de Ads conforme dados disponibilizados pelos conectores.
- Dashboard, alertas, tarefas e relatórios.
- Scanner de catálogos e pesquisa visual em fase posterior.
- MCP/assistente conversacional em fase posterior, com ferramentas e permissões explícitas.

## 4.2 Fora do escopo inicial

- Planos, assinaturas, billing e cobrança SaaS.
- ERP contábil/fiscal completo.
- Emissão de notas fiscais.
- Escrituração contábil e apuração tributária oficial.
- Garantia de acesso a dados privados de concorrentes, como pedidos reais, CTR real ou conversão real, quando não disponibilizados legitimamente.
- Automação que burle CAPTCHA, autenticação, limites de API ou controles dos marketplaces.
- Publicação/alteração automática sem autorização do usuário.
- Gestão de logística própria ou execução física de fulfillment.
- Aplicativo mobile nativo no MVP.
- Serviços humanos de assessoria contábil, jurídica ou de marketplace.

## 4.3 Marketplaces e estratégia de disponibilidade

| Marketplace | Fase alvo | Estratégia de integração |
|---|---|---|
| Mercado Livre | MVP | API oficial e autorização do seller; dados públicos permitidos e extensão complementar dentro das políticas. |
| Shopee | MVP | APIs/parceiros autorizados e autorização do seller; validar escopo de dados, limites e elegibilidade. |
| Amazon | Fase 2 | APIs oficiais aplicáveis e permissões por conta/região. |
| TikTok Shop | Fase 3 | Avaliar API, disponibilidade regional e permissões comerciais. |
| Magalu | Fase 3 | Avaliar API e modelo de integração disponível. |
| SHEIN | Fase 3 | Avaliar elegibilidade, API/parceiro e disponibilidade de dados. |


---

# 5. Requisitos funcionais

## 5.1 Convenções

- Identificadores: `FR-<DOMÍNIO>-NNN`.
- Prioridades: **P0** indispensável ao MVP; **P1** expansão da primeira versão; **P2** evolução.
- Cada requisito deve ser implementado com logs, autorização, testes e tratamento de falhas apropriados.
- Critérios de aceite em BDD são condições mínimas verificáveis; não substituem testes de integração, segurança e usabilidade.

## 5.2 Identidade, tenant e governança

| ID | Pri. | Requisito | Critério de aceite |
|---|---|---|---|
| FR-ID-001 | P0 | Cadastro, login, logout e recuperação de acesso. | Dado um usuário cadastrado, quando autenticar com credenciais válidas, então recebe sessão/token válido; credenciais inválidas não revelam se o usuário existe. |
| FR-ID-002 | P0 | Criar organização (tenant) e associar o usuário Owner. | Quando a organização for criada, então todos os registros de domínio serão associados ao tenant e o Owner terá acesso administrativo. |
| FR-ID-003 | P0 | Convidar usuários e atribuir papéis. | Dado um convite válido, quando o convidado aceitar, então recebe apenas as permissões do papel e escopo atribuídos. |
| FR-ID-004 | P0 | Isolar dados por tenant. | Dado um usuário do tenant A, quando tentar consultar/alterar registro do tenant B, então a operação será negada e auditada. |
| FR-ID-005 | P0 | Gerenciar empresas/CNPJs, lojas e contas. | Um Owner/Admin poderá cadastrar, editar, desativar e associar empresas e contas sem misturar dados entre empresas. |
| FR-ID-006 | P1 | Permissões granulares por empresa, loja e módulo. | Um usuário restrito a uma loja não visualizará dados de outra loja por API, exportação, dashboard ou MCP. |
| FR-ID-007 | P0 | Trilha de auditoria para ações sensíveis. | Alterações de integração, permissões, custos, anúncios, configurações fiscais e ações externas registrarão ator, data, entidade e resultado. |

## 5.3 Integrações com marketplaces

| ID | Pri. | Requisito | Critério de aceite |
|---|---|---|---|
| FR-INT-001 | P0 | Conectar conta por fluxo OAuth/autorizado aplicável. | Ao concluir autorização, credenciais serão armazenadas criptografadas e a conta aparecerá como conectada. |
| FR-INT-002 | P0 | Renovar tokens e tratar expiração/revogação. | Ao expirar token, o conector tentará renovação permitida; em falha, marcará a conexão como ação necessária e notificará o usuário. |
| FR-INT-003 | P0 | Sincronizar catálogo e anúncios próprios. | A sincronização importará os campos disponíveis, registrará origem e data e poderá ser reexecutada sem duplicar entidades. |
| FR-INT-004 | P0 | Sincronizar pedidos próprios. | Pedidos serão importados com identificador externo único por marketplace/conta, status e timestamps de origem. |
| FR-INT-005 | P1 | Sincronizar campanhas e métricas de Ads autorizadas. | Somente métricas entregues pelo conector serão exibidas; ausência de dado será indicada como indisponível, nunca como zero. |
| FR-INT-006 | P0 | Exibir saúde, histórico e última sincronização por conector. | O usuário verá estado, última execução, itens processados, falhas e opção de reprocessar quando suportado. |
| FR-INT-007 | P0 | Processar webhooks/eventos quando disponíveis. | Eventos repetidos serão idempotentes e eventos fora de ordem não corromperão o estado consolidado. |
| FR-INT-008 | P0 | Disponibilizar importação CSV como contingência. | Arquivo válido será validado e importado com relatório de linhas aceitas/rejeitadas e motivo de rejeição. |
| FR-INT-009 | P1 | Catálogo de capacidades por marketplace. | A aplicação informará quais métricas e ações são suportadas, por conector, região e autorização. |

## 5.4 Market Intelligence — pesquisa de mercado

| ID | Pri. | Requisito | Critério de aceite |
|---|---|---|---|
| FR-MKT-001 | P0 | Pesquisar produtos por palavra-chave. | A consulta retornará resultados compatíveis com a fonte, com marketplace, data de coleta e filtros disponíveis. |
| FR-MKT-002 | P0 | Pesquisar por URL ou identificador de anúncio quando suportado. | Uma URL/ID válido será normalizado e associado ao item correto; entrada inválida apresentará erro acionável. |
| FR-MKT-003 | P0 | Filtrar por categoria, subcategoria, preço, marca, avaliações e sinais de demanda, quando disponíveis. | Os filtros aplicados serão visíveis, combináveis e refletidos no resultado e na exportação. |
| FR-MKT-004 | P0 | Visualizar detalhes e indicadores do produto/anúncio. | Cada indicador terá definição, unidade, fonte, data/hora e rótulo observado/importado/estimado. |
| FR-MKT-005 | P1 | Analisar categoria e subcategoria. | O usuário poderá visualizar produtos destacados, distribuição de preços e sinais agregados disponíveis. |
| FR-MKT-006 | P1 | Identificar tendências e sazonalidade. | Séries históricas serão exibidas somente quando houver cobertura mínima; lacunas e período de cobertura serão informados. |
| FR-MKT-007 | P1 | Calcular concentração competitiva. | O sistema apresentará a metodologia do índice, universo analisado e limitações; não chamará estimativa de participação real sem evidência. |
| FR-MKT-008 | P0 | Salvar pesquisas e listas de oportunidades. | O usuário poderá salvar consulta, produtos selecionados e notas, mantendo tenant e autor. |
| FR-MKT-009 | P1 | Exportar resultados. | CSV/XLSX conterá filtros, data, fonte e marcação de estimativa; exportação respeitará permissões. |

## 5.5 Competitor Intelligence — concorrentes

| ID | Pri. | Requisito | Critério de aceite |
|---|---|---|---|
| FR-CMP-001 | P0 | Cadastrar concorrente por perfil/loja ou anúncio. | O concorrente será associado ao tenant e terá marketplace, identificador e URL quando disponíveis. |
| FR-CMP-002 | P1 | Agrupar concorrentes em listas. | O usuário poderá criar, renomear e remover grupos sem apagar dados históricos por padrão. |
| FR-CMP-003 | P1 | Monitorar preço e atributos observáveis de anúncios. | Cada captura será armazenada com timestamp e origem; mudança gerará evento comparável. |
| FR-CMP-004 | P1 | Detectar novos anúncios, remoções ou pausas quando observáveis. | O monitoramento gerará evento com evidência e horário da detecção, sem afirmar causa não observada. |
| FR-CMP-005 | P1 | Comparar anúncio próprio com concorrentes. | A comparação exibirá atributos, preço, logística e conteúdo disponíveis, indicando campos ausentes. |
| FR-CMP-006 | P1 | Exibir histórico de concorrente. | O histórico terá intervalo de cobertura, frequência de coleta e lacunas. |
| FR-CMP-007 | P1 | Gerar alerta de mudança relevante. | O usuário poderá configurar limiar e canal; alertas duplicados serão deduplicados por janela. |
| FR-CMP-008 | P1 | Identificar reputação e indicadores de seller quando públicos/permitidos. | Campos serão exibidos conforme disponibilidade da fonte e nunca inferidos como dado oficial. |

## 5.6 Product Discovery e fornecedores

| ID | Pri. | Requisito | Critério de aceite |
|---|---|---|---|
| FR-DIS-001 | P1 | Criar ranking de oportunidades com critérios configuráveis. | O score exibirá fatores, pesos, cobertura e explicação; não será apresentado como garantia de vendas. |
| FR-DIS-002 | P1 | Buscar produto semelhante por imagem. | Imagem será processada com consentimento; resultados mostrarão similaridade estimada e links/fontes. |
| FR-DIS-003 | P1 | Enviar PDF de catálogo de fornecedor. | Arquivo será validado, armazenado com controle de acesso e submetido a processamento assíncrono. |
| FR-DIS-004 | P1 | Extrair itens, códigos, EAN, descrição, preço, peso e dimensões. | Extrações terão nível de confiança por campo e permitirão correção manual antes de usar os dados. |
| FR-DIS-005 | P1 | Comparar itens do catálogo com resultados do mercado. | Correspondências serão classificadas por similaridade e poderão ser confirmadas/rejeitadas pelo usuário. |
| FR-DIS-006 | P1 | Comparar fornecedores e custos. | O usuário poderá salvar fornecedor, preço, validade da cotação, MOQ e observações, quando informados. |
| FR-DIS-007 | P1 | Simular margem dos itens identificados. | A simulação utilizará o motor financeiro e identificará campos presumidos ou ausentes. |
| FR-DIS-008 | P1 | Exportar ranking e catálogo processado. | Exportação incluirá valores originais, normalizados, confiança e origem. |

## 5.7 AI Listing Studio e Analytics

| ID | Pri. | Requisito | Critério de aceite |
|---|---|---|---|
| FR-LST-001 | P0 | Analisar qualidade de anúncio próprio. | A análise apresentará score e critérios individuais, evidências e data da avaliação. |
| FR-LST-002 | P0 | Gerar e otimizar títulos. | A IA proporá versões com limites e políticas do canal selecionado; usuário poderá editar e comparar. |
| FR-LST-003 | P0 | Gerar e otimizar descrições. | O texto deverá utilizar apenas características confirmadas ou marcadas como pendentes; não inventará certificações ou especificações. |
| FR-LST-004 | P1 | Sugerir categoria e atributos. | Sugestões mostrarão confiança e permitirão confirmação manual. |
| FR-LST-005 | P1 | Sugerir palavras-chave. | Termos serão acompanhados de origem, relevância estimada e relação com o produto. |
| FR-LST-006 | P1 | Preparar imagens de produto. | Ferramentas de edição preservarão informações reais do produto; mudanças geradas serão revisáveis. |
| FR-LST-007 | P1 | Criar rascunho a partir de anúncio de referência. | O rascunho usará estrutura/informação permitida, exigirá conteúdo original e revisão antes da publicação. |
| FR-LST-008 | P1 | Gerenciar versões e histórico. | Cada geração/edição terá autor, timestamp e possibilidade de comparar versões. |
| FR-LST-009 | P1 | Publicar ou atualizar anúncio por API autorizada. | Ação exigirá confirmação explícita, validação de campos e registro do payload/resultado sem armazenar segredos. |
| FR-LST-010 | P1 | Executar teste A/B quando o marketplace ou processo suportar. | O sistema definirá hipótese, variante, janela e métrica; se não houver experimento nativo/isolável, exibirá comparação observacional, não A/B causal. |
| FR-LST-011 | P0 | Distinguir recomendação de IA de métrica observada. | UI e exportações identificarão claramente conteúdo gerado, recomendação, estimativa e dado original. |

## 5.8 Seller Financial Control — financeiro

| ID | Pri. | Requisito | Critério de aceite |
|---|---|---|---|
| FR-FIN-001 | P0 | Cadastrar custo de aquisição e custos adicionais por SKU. | Custo terá moeda, vigência, unidade, origem e histórico de alterações. |
| FR-FIN-002 | P0 | Configurar componentes de tarifa, comissão, frete e impostos. | Regras serão versionadas por marketplace, categoria, modalidade, região e período de vigência, conforme dados disponíveis. |
| FR-FIN-003 | P0 | Calcular margem de contribuição por pedido/SKU. | O resultado será reproduzível a partir dos componentes e da versão de regra usada. |
| FR-FIN-004 | P0 | Calcular preço mínimo e simular preço-alvo. | O sistema informará premissas, custos ausentes e margem resultante; não recomendará preço se dados críticos faltarem sem aviso. |
| FR-FIN-005 | P0 | Consolidar resultados por loja, marketplace e empresa. | Filtros e consolidações não duplicarão pedidos e respeitarão alocações. |
| FR-FIN-006 | P0 | Considerar descontos, cancelamentos, devoluções e estornos. | Eventos financeiros serão refletidos conforme política de reconhecimento configurada e manterão histórico. |
| FR-FIN-007 | P1 | Calcular margem após custo de Ads atribuído. | A atribuição de Ads será identificada como regra/fonte, com janela e limitações documentadas. |
| FR-FIN-008 | P1 | Apresentar DRE gerencial. | Categorias e fórmulas serão configuráveis; relatório indicará período, competência/caixa e origem dos valores. |
| FR-FIN-009 | P1 | Exibir Curva ABC e rentabilidade. | Classificação terá base e período selecionáveis, com regra visível. |
| FR-FIN-010 | P0 | Versionar regras de cálculo. | Alterar uma regra não modificará resultados históricos já fechados sem reprocessamento explícito e auditado. |
| FR-FIN-011 | P0 | Informar dados incompletos e qualidade do cálculo. | O sistema sinalizará cálculo completo, parcial ou simulado e listará premissas ausentes. |

## 5.9 Ads Intelligence

| ID | Pri. | Requisito | Critério de aceite |
|---|---|---|---|
| FR-ADS-001 | P1 | Importar campanhas e métricas autorizadas. | Campanha será associada à conta, período e fonte, com atualização registrada. |
| FR-ADS-002 | P1 | Calcular ROAS, ACOS e TACOS. | Fórmulas e denominadores estarão documentados e serão consistentes entre telas e exportações. |
| FR-ADS-003 | P1 | Exibir CTR, CPC, cliques, impressões e conversão quando disponíveis. | Métricas indisponíveis serão marcadas como não fornecidas, não convertidas em zero. |
| FR-ADS-004 | P1 | Analisar desempenho por campanha, grupo e SKU. | A granularidade respeitará o que o conector disponibiliza e a atribuição aplicável. |
| FR-ADS-005 | P1 | Cruzar publicidade com margem. | O usuário verá resultado após Ads quando houver dados suficientes e método de atribuição visível. |
| FR-ADS-006 | P1 | Recomendar otimizações com IA. | Recomendações incluirão métrica, período, justificativa e ação sugerida; alteração externa exigirá aprovação. |
| FR-ADS-007 | P1 | Alertar campanha fora de meta. | Limiares serão configuráveis e alertas terão deduplicação e histórico. |

## 5.10 Product Monitoring

| ID | Pri. | Requisito | Critério de aceite |
|---|---|---|---|
| FR-MON-001 | P1 | Criar lista de observação de produtos/anúncios. | O usuário poderá adicionar, remover e organizar itens por organização. |
| FR-MON-002 | P1 | Registrar histórico temporal de preço e atributos disponíveis. | Capturas manterão timestamp e fonte, sem sobrescrever o passado. |
| FR-MON-003 | P1 | Acompanhar vendas estimadas apenas quando houver método/fonte válida. | O indicador será explicitamente estimado e apresentará metodologia resumida e cobertura. |
| FR-MON-004 | P1 | Configurar alertas por variação. | O usuário definirá limiar e janela; sistema testará regras com eventos simulados. |
| FR-MON-005 | P1 | Exibir gráficos e comparar períodos. | Gráficos identificarão lacunas e não interpolarão dados sem sinalização. |

## 5.11 Smart Alerts e Action Center

| ID | Pri. | Requisito | Critério de aceite |
|---|---|---|---|
| FR-ACT-001 | P1 | Centralizar alertas por severidade e domínio. | Alertas apresentarão origem, data, contexto, severidade e estado. |
| FR-ACT-002 | P1 | Configurar regras de alerta. | Usuário autorizado poderá criar, pausar e testar regras no escopo permitido. |
| FR-ACT-003 | P1 | Converter recomendação/alerta em tarefa. | Tarefa herdará contexto, origem e vínculo com o alerta. |
| FR-ACT-004 | P1 | Atribuir responsável, prazo, prioridade e status. | Alterações serão auditadas e notificações respeitarão preferências. |
| FR-ACT-005 | P1 | Acompanhar resultado da ação. | Tarefa poderá ser concluída com comentário/evidência e vínculo com indicador posterior. |
| FR-ACT-006 | P1 | Enviar notificações. | Canais habilitados terão opt-in, controle de frequência e registro de entrega/falha. |

## 5.12 Executive Dashboard e relatórios

| ID | Pri. | Requisito | Critério de aceite |
|---|---|---|---|
| FR-DAS-001 | P0 | Exibir painel consolidado de vendas e rentabilidade. | Valores serão filtráveis por empresa, loja, marketplace e período, com definição de cada KPI. |
| FR-DAS-002 | P0 | Comparar períodos. | Comparação utilizará períodos equivalentes e indicará quando houver dados incompletos. |
| FR-DAS-003 | P1 | Exibir ranking de produtos/canais. | Ranking permitirá selecionar métrica e período e explicará ordenação. |
| FR-DAS-004 | P1 | Exibir alertas e tarefas prioritárias. | O dashboard exibirá itens abertos do escopo autorizado e permitirá navegar ao detalhe. |
| FR-DAS-005 | P0 | Exportar relatórios. | Exportação refletirá filtros e permissões e incluirá data/hora de geração. |
| FR-DAS-006 | P1 | Agendar envio de relatórios. | Agendamento terá destinatários autorizados, frequência, cancelamento e trilha de execução. |

## 5.13 Marketplace Reconciliation

| ID | Pri. | Requisito | Critério de aceite |
|---|---|---|---|
| FR-REC-001 | P1 | Importar extratos/repasses disponíveis. | Dados serão associados à conta e período, com validação de duplicidade. |
| FR-REC-002 | P1 | Relacionar pedidos, eventos financeiros e repasses. | Correspondências terão status e método de associação. |
| FR-REC-003 | P1 | Identificar divergências. | Divergência apresentará valor esperado, recebido, diferença e evidências disponíveis. |
| FR-REC-004 | P1 | Registrar contestação/ação externa. | O sistema registrará protocolo, data, estado e documentos, sem afirmar envio automático quando não suportado. |
| FR-REC-005 | P1 | Exibir relatório de conciliação. | Relatório discriminará conciliado, pendente, divergente e não identificado. |

## 5.14 AI Assistant e MCP

| ID | Pri. | Requisito | Critério de aceite |
|---|---|---|---|
| FR-AI-001 | P1 | Disponibilizar assistente conversacional contextual. | O assistente responderá apenas com dados acessíveis ao usuário e indicará período e escopo consultados. |
| FR-AI-002 | P1 | Consultar produtos, concorrentes, categorias e indicadores por linguagem natural. | Consulta será convertida em filtros/ferramentas verificáveis e os resultados poderão ser abertos na aplicação. |
| FR-AI-003 | P1 | Gerar análises e relatórios explicáveis. | Resposta mostrará fontes, métricas e limitações; sem evidência suficiente, declarará indisponibilidade. |
| FR-AI-004 | P2 | Expor ferramentas via MCP. | Cada ferramenta terá schema, autenticação, autorização por tenant e logs de execução. |
| FR-AI-005 | P2 | Separar ferramentas read-only de ferramentas mutáveis. | Operações mutáveis exigirão confirmação explícita fora da chamada de consulta e validação adicional. |
| FR-AI-006 | P1 | Proteger contra prompt injection em conteúdo externo. | Texto de anúncios, PDFs e avaliações será tratado como dado não confiável e não poderá alterar instruções do sistema. |
| FR-AI-007 | P1 | Registrar uso de IA e avaliação de qualidade. | Execuções registrarão modelo/provedor, versão de prompt, ferramenta, latência, custo técnico e resultado, respeitando privacidade. |

---

# 6. Regras de negócio

## 6.1 Dados de mercado e estimativas

- **Dado observado:** capturado diretamente de uma fonte identificável no momento indicado.
- **Dado autorizado/importado:** recebido de conta do próprio seller ou arquivo fornecido pelo usuário.
- **Dado estimado:** inferido a partir de uma metodologia; deve conter rótulo, intervalo/confiança quando possível, período e metodologia resumida.
- **Dado indisponível:** a fonte não fornece ou a autorização não permite; nunca substituir por zero.
- **Dado gerado por IA:** conteúdo ou recomendação produzida pelo modelo, separado do dado de origem.

É proibido apresentar CTR, conversão, pedidos, estoque ou receita real de concorrente sem fonte autorizada e evidência. Quando o produto usar proxies ou estimativas, deverá informar claramente essa condição.

## 6.2 Motor financeiro

O motor deverá manter componentes de cálculo separados, configuráveis e versionados.

**Modelo conceitual de margem de contribuição por item:**

`Receita líquida do item - custo do produto - comissão/tarifas - frete/logística suportados pelo seller - embalagem - impostos variáveis - descontos suportados - outros custos variáveis - Ads atribuível (quando aplicável)`.

A fórmula exata depende do marketplace, modalidade logística, regime fiscal, política de reconhecimento e origem de cada valor. O sistema deverá apresentar a memória de cálculo, não somente o resultado final.

- Valores monetários devem usar decimal de precisão definida, nunca ponto flutuante binário para cálculo financeiro.
- Moeda, arredondamento e política de casas decimais serão explícitos.
- Custos devem ter vigência temporal.
- Regras de comissão e frete devem possuir versão e data de validade.
- Resultados fechados não serão recalculados silenciosamente quando regras forem alteradas.
- Cálculo incompleto será identificado como parcial/simulado.
- O sistema é gerencial e não substitui orientação fiscal/contábil profissional.

## 6.3 Ads e atribuição

- ROAS = receita atribuída a Ads / investimento em Ads.
- ACOS = investimento em Ads / receita atribuída a Ads.
- TACOS = investimento em Ads / receita total considerada no período.
- CTR = cliques / impressões.
- CPC = investimento / cliques.
- Taxa de conversão = conversões / cliques ou outra base definida pela fonte; a definição usada deverá acompanhar a métrica.

O sistema deverá respeitar a janela e a metodologia de atribuição fornecidas pela plataforma de Ads. Comparações entre marketplaces somente serão permitidas com aviso de diferenças metodológicas.

## 6.4 Score de anúncio e oportunidade

- Scores serão indicadores internos explicáveis, não métricas oficiais do marketplace.
- Cada score deverá informar fatores, pesos, versão da metodologia e cobertura dos dados.
- Score não deverá afirmar causalidade nem garantir aumento de vendas.
- Alterações na metodologia gerarão nova versão e preservarão histórico.

## 6.5 Publicação e automação

- Ações externas exigem consentimento/autorização e escopo OAuth compatível.
- Antes de publicar/alterar, validar dados obrigatórios, regras do canal e mudanças propostas.
- Para ação em lote, exibir prévia, quantidade de itens e resumo de impacto.
- Toda ação terá chave de idempotência quando aplicável e registro de resultado.
- Falhas parciais devem ser apresentadas por item, com opção segura de reprocessamento.

---

# 7. Requisitos não funcionais

| ID | Categoria | Requisito |
|---|---|---|
| NFR-SEC-001 | Segurança | TLS em trânsito e criptografia em repouso para dados e segredos. |
| NFR-SEC-002 | Segurança | Isolamento multi-tenant aplicado no backend e, quando viável, reforçado no banco por Row-Level Security. |
| NFR-SEC-003 | Segurança | Segredos OAuth e chaves de integração armazenados em cofre/serviço de segredos, nunca em texto puro. |
| NFR-SEC-004 | Segurança | RBAC e autorização por recurso, organização, empresa e conta. |
| NFR-SEC-005 | Segurança | Proteção contra OWASP Top 10, CSRF, XSS, SSRF, SQL injection, abuso de API e upload malicioso. |
| NFR-SEC-006 | Segurança | MFA para papéis privilegiados como requisito recomendado para produção. |
| NFR-SEC-007 | Segurança | Rate limiting, detecção de abuso e proteção contra enumeração de usuários. |
| NFR-PRV-001 | Privacidade | Minimização de dados pessoais e tratamento compatível com LGPD, com finalidade, base legal, retenção e direitos do titular. |
| NFR-PRV-002 | Privacidade | Dados de um tenant não poderão ser usados para treinar modelos compartilhados sem base legal e autorização explícita aplicável. |
| NFR-PRV-003 | Privacidade | Logs não deverão conter tokens, credenciais, dados pessoais desnecessários ou conteúdo financeiro sensível em claro. |
| NFR-REL-001 | Confiabilidade | Jobs de sincronização devem ser idempotentes e recuperáveis. |
| NFR-REL-002 | Confiabilidade | Falhas transitórias devem usar retry com backoff e jitter, respeitando rate limits. |
| NFR-REL-003 | Confiabilidade | Dead-letter queue ou mecanismo equivalente para mensagens não processadas. |
| NFR-REL-004 | Confiabilidade | Backups automatizados e testes periódicos de restauração. |
| NFR-PER-001 | Performance | Meta inicial: p95 abaixo de 2,5 s para consultas comuns de aplicação, excluindo processamento externo/relatórios pesados. Validar em carga piloto. |
| NFR-PER-002 | Performance | Pesquisas e relatórios longos devem executar assincronamente e expor estado/progresso. |
| NFR-PER-003 | Performance | Paginação, filtros e limites de exportação devem evitar consultas sem limite. |
| NFR-SCL-001 | Escalabilidade | Workers de ingestão, IA e geração de relatórios devem escalar independentemente da API web. |
| NFR-OBS-001 | Observabilidade | Logs estruturados, métricas e tracing distribuído via OpenTelemetry ou padrão equivalente. |
| NFR-OBS-002 | Observabilidade | Correlation ID deverá acompanhar requisições, eventos e jobs. |
| NFR-OBS-003 | Observabilidade | Dashboards e alertas para falhas de conectores, filas, latência e qualidade de dados. |
| NFR-UX-001 | Usabilidade | Interface responsiva, acessível e consistente; estados vazios, carregando, erro e indisponibilidade devem ser explícitos. |
| NFR-UX-002 | Usabilidade | Indicadores devem possuir definição acessível e glossário contextual. |
| NFR-AI-001 | IA | Respostas devem ser fundamentadas, informar limites e evitar inventar dados. |
| NFR-AI-002 | IA | Dados externos serão tratados como conteúdo não confiável; execução de ferramentas seguirá política de autorização. |
| NFR-MNT-001 | Manutenibilidade | Conectores devem implementar contrato comum, testes de contrato e versionamento. |
| NFR-MNT-002 | Manutenibilidade | Regras de cálculo e scoring devem ser versionadas e cobertas por testes determinísticos. |

## 7.1 Metas iniciais de disponibilidade e recuperação

Metas finais serão acordadas após sizing e análise de custo. Como baseline para produção:

- Disponibilidade mensal alvo da aplicação: 99,5%, excluindo indisponibilidade comprovada de terceiros e janelas comunicadas.
- RPO alvo: até 15 minutos para dados transacionais críticos, sujeito à estratégia de banco e custo.
- RTO alvo: até 4 horas para recuperação do serviço principal.
- Jobs de sincronização poderão ter SLA próprio, conforme limites e disponibilidade do marketplace.

---

# 8. Arquitetura lógica e domínios

## 8.1 Abordagem recomendada

Começar com **monólito modular** e workers assíncronos, mantendo limites de domínio e contratos explícitos. A extração de microsserviços deverá ocorrer somente quando houver necessidade operacional, de escala, autonomia de times ou isolamento de falhas.

## 8.2 Contextos de domínio

1. Identity & Tenancy.
2. Marketplace Connections.
3. Catalog & Listings.
4. Market Intelligence.
5. Competitor Intelligence.
6. Product Discovery & Supplier Intelligence.
7. Listing Studio & AI Analytics.
8. Financial Control.
9. Ads Intelligence.
10. Monitoring & Alerts.
11. Tasks & Action Center.
12. Reconciliation & Reporting.
13. AI Assistant & MCP.

## 8.3 Componentes lógicos

- Web Frontend: aplicação responsiva.
- Browser Extension: captura contextual autorizada e abertura de análises, sem credenciais próprias de marketplace.
- API Backend: autenticação, autorização, casos de uso e regras de domínio.
- Connector Adapters: um adaptador por marketplace/provedor.
- Ingestion Workers: sincronização, normalização, deduplicação e reprocessamento.
- Event/Message Broker: eventos de domínio e processamento assíncrono.
- AI Orchestrator: seleção de modelo, prompts versionados, ferramentas, avaliação e guardrails.
- Financial Engine: cálculos determinísticos e versionados.
- Notification Service: e-mail/in-app e outros canais habilitados.
- Reporting/Analytics: consultas agregadas e exportações.
- MCP Server: expõe ferramentas autorizadas e auditadas.

## 8.4 Stack de referência (não bloqueante)

| Camada | Tecnologia sugerida | Observações |
|---|---|---|
| Backend | Java 21 + Spring Boot 3 | Domínios, APIs, segurança e regras de negócio. |
| Frontend | React + Next.js + TypeScript | Aplicação web e design system. |
| Extensão | TypeScript + Chrome Extension APIs | Permissões mínimas e compatibilidade Chromium. |
| Banco transacional | PostgreSQL | Dados de domínio, transações e histórico inicial. |
| Cache | Redis | Cache, locks e controle de jobs quando necessário. |
| Mensageria | RabbitMQ inicialmente; Kafka se volume/streaming justificar | Eventos e processamento assíncrono. |
| Arquivos | Object Storage compatível com S3 | PDFs, imagens e exportações. |
| Busca | PostgreSQL no MVP; mecanismo dedicado depois | Evitar introduzir complexidade prematuramente. |
| Observabilidade | OpenTelemetry + backend de logs/métricas/traces | Correlação de ponta a ponta. |
| IA | Provedores LLM intercambiáveis + modelos especializados | Abstração de provider e avaliação. |
| MCP | Servidor MCP com ferramentas tipadas e escopo de autorização | Apenas ferramentas explicitamente liberadas. |

## 8.5 Eventos de domínio sugeridos

- `MarketplaceAccountConnected`
- `MarketplaceTokenExpired`
- `CatalogSyncRequested`
- `CatalogSyncCompleted`
- `OrderImported`
- `OrderStatusChanged`
- `ListingImported`
- `MarketSnapshotCaptured`
- `CompetitorPriceChanged`
- `ProductWatchAlertTriggered`
- `ListingAnalysisCompleted`
- `FinancialCalculationCompleted`
- `AdCampaignSynced`
- `ReconciliationDiscrepancyDetected`
- `TaskCreated`
- `TaskCompleted`

Eventos devem carregar tenant ID, correlation ID, versão do schema e identificador de origem. Não incluir segredos ou dados pessoais desnecessários.

---

# 9. Modelo de dados conceitual

Entidades iniciais (nomes conceituais, não esquema físico definitivo):

| Entidade | Responsabilidade | Relações relevantes |
|---|---|---|
| Organization | Tenant raiz | Users, Companies, Connections, Watchlists |
| User | Identidade e preferências | Memberships, AuditEvents |
| Membership | Papel e escopo do usuário | Organization, User, Role |
| Company | Empresa/CNPJ | Stores, Costs, FinancialSettings |
| Marketplace | Catálogo de canais | Connections, Listings, Orders |
| MarketplaceConnection | Credenciais e estado da conexão | Organization, Company, Store |
| Store | Loja/conta do seller | Listings, Orders, Campaigns |
| Product | Produto/SKU interno ou normalizado | Listings, Costs, Inventory |
| Listing | Anúncio em marketplace | Product, Store, Snapshots |
| MarketProduct | Produto observado no mercado | MarketSnapshots, Competitor |
| MarketSnapshot | Captura temporal de dados de mercado | MarketProduct, Source |
| Competitor | Seller/loja/anúncio monitorado | Watchlists, CompetitorEvents |
| CostRule | Regra/versionamento de custo | Company, Product, Marketplace |
| FinancialTransaction | Evento financeiro | Order, Payout, Reconciliation |
| Order | Pedido importado | Items, Transactions |
| OrderItem | Item do pedido | Product, Listing, CostSnapshot |
| AdCampaign | Campanha de Ads | Store, AdMetrics |
| AdMetric | Métrica por período/granularidade | Campaign, Listing/Product |
| Supplier | Fornecedor | SupplierCatalog, SupplierProduct |
| SupplierCatalog | Arquivo e processamento | SupplierProducts |
| AIAnalysis | Análise de IA | Listing/Product, PromptVersion |
| AlertRule | Regra de monitoramento | Organization/User |
| Alert | Ocorrência de alerta | AlertRule, Task |
| Task | Ação operacional | User, Alert, Organization |
| Reconciliation | Processo de conciliação | Payout, Transactions |
| AuditEvent | Trilha de auditoria | Actor, Tenant, Entity |
| DataSource | Proveniência da informação | Snapshots, Metrics, AIAnalysis |

## 9.1 Regras de persistência

- Toda entidade pertencente a cliente deve possuir tenant ID, direta ou indiretamente com garantia verificável.
- Identificadores externos devem ter unicidade composta por marketplace, conta e ID externo.
- Dados históricos de mercado devem ser append-only ou versionados; não sobrescrever capturas antigas.
- Valores monetários devem armazenar moeda e precisão adequadas.
- Datas de origem e datas de ingestão devem ser separadas.
- Dados derivados devem manter referência aos dados e versões de regra que os produziram.

---

# 10. Experiências e jornadas principais

## 10.1 Onboarding e conexão

1. Usuário cria conta e organização.
2. Cadastra empresa/CNPJ e loja.
3. Conecta a conta de marketplace por fluxo autorizado.
4. Sistema valida escopo e mostra quais dados poderão ser sincronizados.
5. Usuário escolhe escopo de sincronização.
6. Primeira sincronização é executada em background.
7. Dashboard apresenta estado, dados carregados e pendências.

**Aceite E2E:** o usuário conclui onboarding, conecta uma conta válida, acompanha a sincronização e visualiza os dados importados apenas dentro de seu tenant.

## 10.2 Pesquisa e validação de oportunidade

1. Usuário pesquisa produto/categoria.
2. Aplica filtros.
3. Abre detalhe do produto e concorrentes.
4. Consulta preços e sinais de demanda disponíveis.
5. Simula custo e preço no motor financeiro.
6. Salva a oportunidade e adiciona notas.

**Aceite E2E:** a oportunidade salva mantém filtros, origem, timestamps e memória da simulação, incluindo premissas e dados ausentes.

## 10.3 Criação e otimização de anúncio

1. Usuário seleciona produto/SKU e marketplace.
2. Importa dados próprios ou preenche ficha técnica.
3. Solicita análise e sugestões de IA.
4. Revisa título, descrição, atributos e imagens.
5. Executa validações específicas do marketplace.
6. Salva rascunho ou publica com confirmação.
7. Sistema registra versão e retorno do marketplace.

**Aceite E2E:** nenhuma alteração externa acontece antes de confirmação explícita; payload, resultado e autor ficam auditados.

## 10.4 Monitoramento e ação

1. Usuário adiciona produto ou concorrente à lista de observação.
2. Define regra de alerta.
3. Coletor registra snapshots conforme frequência permitida.
4. Motor detecta alteração.
5. Central de alertas apresenta evidência.
6. Usuário converte o alerta em tarefa.
7. Responsável executa e conclui tarefa.
8. Dashboard permite verificar resultado posterior; alertas não duplicam indefinidamente, possuem evidência e podem ser rastreados até a conclusão.

## 10.5 Apuração de lucratividade

1. Pedidos são importados.
2. Itens são associados a SKUs.
3. Motor encontra custo e regras vigentes na data relevante.
4. Sistema calcula receita, deduções, custos e margem.
5. Usuário abre memória de cálculo.
6. Dado incompleto é sinalizado e pode ser corrigido.

**Aceite E2E:** o mesmo conjunto de entradas e versões de regra produz o mesmo resultado; nenhuma tarifa ausente é tratada silenciosamente como zero.

## 10.6 Assistente/MCP

1. Usuário autorizado inicia consulta.
2. Assistente identifica intenção e escopo permitido.
3. Executa ferramenta read-only compatível.
4. Retorna síntese, fontes, período, filtros e ressalvas.
5. Se a pergunta pedir ação mutável, solicita confirmação via fluxo apropriado.

**Aceite E2E:** o assistente não acessa outro tenant, não inventa métricas indisponíveis e não executa escrita sem aprovação explícita.

---

# 11. Instrumentação, eventos e analytics de produto

Eventos analíticos sugeridos:

- `organization_created`
- `marketplace_connection_started`
- `marketplace_connection_succeeded`
- `initial_sync_completed`
- `market_search_submitted`
- `market_product_opened`
- `opportunity_saved`
- `listing_analysis_requested`
- `listing_analysis_completed`
- `ai_suggestion_accepted`
- `financial_simulation_completed`
- `alert_rule_created`
- `alert_triggered`
- `task_created_from_alert`
- `task_completed`
- `reconciliation_completed`
- `report_exported`

Regras: não incluir dados pessoais, tokens ou textos sensíveis em ferramentas analíticas; usar IDs pseudonimizados; documentar finalidade e retenção; permitir desativação/configuração conforme política de privacidade.

---

# 12. Segurança, privacidade e conformidade

## 12.1 LGPD e governança de dados

Antes do lançamento, definir controlador/operador por fluxo, base legal, finalidade, retenção, subprocessadores, política de privacidade, canal de direitos dos titulares e processo de incidente. Minimizar dados pessoais de compradores; importar apenas campos necessários para os objetivos do produto.

## 12.2 Segurança de integrações

- OAuth com escopos mínimos necessários.
- Criptografia e rotação de segredos.
- Revogação de acesso e desconexão disponíveis ao usuário.
- Segredos nunca enviados ao browser extension, logs ou LLM.
- Limites e retries compatíveis com políticas de terceiros.
- Webhooks autenticados e com proteção contra replay.

## 12.3 Segurança da IA

- Conteúdo de anúncios, avaliações, imagens e PDFs é entrada não confiável.
- Ferramentas devem usar allowlist, schema estrito e autorização contextual.
- Segregar prompts de sistema, dados externos e instruções do usuário.
- Não expor informações de outros clientes em contexto ou memória.
- Não permitir que o modelo defina autonomamente preço final, imposto ou lucro.
- Para ações externas, exigir confirmação e apresentar prévia.
- Manter avaliação automatizada para alucinação, vazamento e prompt injection.

## 12.4 Extensão de navegador

- Solicitar permissões mínimas por domínio e funcionalidade.
- Não interceptar credenciais nem armazenar cookies/sessões de marketplace.
- Usar autenticação da própria plataforma e tokens de curta duração.
- Evitar scripts de coleta em páginas fora do escopo.
- Exibir claramente quando um dado foi lido da página atual.
- Respeitar políticas da Chrome Web Store e termos dos marketplaces.

---

# 13. Estratégia de qualidade e testes

## 13.1 Pirâmide de testes

- Unitários: regras financeiras, normalização, filtros, scores e políticas de autorização.
- Integração: banco, filas, object storage, OAuth, webhooks e adaptadores.
- Contrato: schemas dos conectores e fornecedores externos.
- E2E: jornadas de onboarding, pesquisa, anúncio, financeiro, alertas e MCP.
- Segurança: tenant isolation, RBAC, OWASP, upload, secrets, rate limits e prompt injection.
- Performance: API, pesquisas, dashboards, exportações e ingestão concorrente.
- Resiliência: timeout, rate limit de terceiro, token revogado, mensagem duplicada e indisponibilidade de fila.

## 13.2 Casos mínimos de regressão financeira

1. Pedido com todos os componentes conhecidos.
2. Pedido com frete subsidiado parcialmente.
3. Desconto financiado pelo seller versus marketplace, quando identificável.
4. Cancelamento antes do envio.
5. Devolução parcial.
6. Estorno posterior ao fechamento do período.
7. Custo do SKU alterado com vigência futura.
8. Comissão alterada com nova versão de regra.
9. Pedido em moeda diferente, se suportado.
10. Divisão de pedido em múltiplos itens e tarifas alocadas.

## 13.3 Qualidade de dados

Criar verificações para duplicidade, atraso de sincronização, campos obrigatórios ausentes, valores fora de faixa, datas inconsistentes, divergência entre total do pedido e itens e origem desconhecida. Problemas devem gerar status de qualidade, não correções silenciosas.

---

# 14. Roadmap e entregas

## Fase 0 — Descoberta e validação técnica

- Validar APIs, permissões, políticas e disponibilidade regional de cada marketplace.
- Definir matriz de capacidades por conector.
- Validar fontes possíveis para dados públicos/concorrência e estimativas.
- Definir modelo fiscal/financeiro gerencial e casos de cálculo.
- Prototipar experiência e testar com sellers.
- Fechar decisões de stack, arquitetura, retenção e privacidade.

**Gate de saída:** fontes e permissões documentadas, jornadas validadas, riscos legais e técnicos classificados, backlog P0 refinado.

## Fase 1 — MVP

- Identidade, organização, usuários, empresas e RBAC.
- Conectores iniciais de Mercado Livre e Shopee, conforme matriz validada.
- Sincronização de catálogo, anúncios e pedidos próprios.
- Pesquisa de mercado e análise de concorrência com fontes aprovadas.
- Histórico básico de preço onde a captura for permitida.
- Motor financeiro e simulação por SKU/pedido.
- Dashboard base.
- AI Listing Analytics e geração assistida de título/descrição.
- Extensão Chrome mínima, com permissões restritas.
- Exportação e auditoria.

**Gate de saída:** jornadas P0 aprovadas, isolamento multi-tenant testado, cálculos financeiros validados por casos de referência e sincronização monitorada.

## Fase 2 — Inteligência e automação

- Monitoramento de produtos e vendedores.
- Alertas e Action Center.
- Palavras-chave e sazonalidade, conforme cobertura de fonte.
- Scanner de catálogos e pesquisa por imagem.
- Ads Intelligence.
- DRE e conciliação financeira.
- Conector Amazon, mediante viabilidade.
- Relatórios agendados.

**Gate de saída:** qualidade e cobertura de dados documentadas; alertas e relatórios com evidência e rastreabilidade.

## Fase 3 — Plataforma inteligente ampliada

- Assistente conversacional avançado.
- Servidor MCP e ferramentas externas autorizadas.
- Publicação/alteração assistida de anúncios.
- Testes experimentais quando viáveis.
- Outros marketplaces conforme acesso e demanda.
- Diagnóstico de maturidade e planejamento de crescimento.
- Aplicativo mobile ou PWA avançada, conforme validação.

**Gate de saída:** governança de IA, segurança de ferramentas mutáveis e métricas de adoção aprovadas.

---

# 15. Riscos, dependências e mitigação

| Risco/dependência | Impacto | Mitigação |
|---|---|---|
| APIs não fornecem todos os dados desejados | Alto | Definir matriz de capacidades; reduzir escopo de métricas; usar fonte licenciada ou estimativa claramente rotulada. |
| Mudanças em APIs e políticas de marketplace | Alto | Adaptadores isolados, monitoramento de contrato, versionamento e plano de contingência. |
| Restrições a coleta via extensão | Alto | Permissões mínimas, revisão de política e alternativa por APIs/fontes autorizadas. |
| Dados estimados interpretados como exatos | Alto | Proveniência, rótulos, intervalos, confiança e explicação da metodologia. |
| Cálculos financeiros incorretos | Alto | Motor determinístico, regras versionadas, memória de cálculo e validação com especialistas. |
| Complexidade tributária brasileira | Alto | Configuração por empresa/regime e validação com contador; não posicionar como apuração fiscal oficial. |
| Vazamento entre tenants | Crítico | RLS/escopo, testes automatizados, revisão de segurança e auditoria. |
| Prompt injection ou vazamento via IA | Alto | Tratar fontes externas como não confiáveis, ferramentas restritas, filtros de saída e testes adversariais. |
| Custo/latência de LLM | Médio | Cache apropriado, modelos por tarefa, cotas internas, avaliação e fallback. |
| Volume elevado de histórico | Médio/alto | Retenção configurável, particionamento, agregações e storage analítico conforme crescimento. |
| Dependência de múltiplos provedores | Médio | Camada de abstração e contratos internos. |

---

# 16. Critérios globais de aceite do MVP

O MVP será considerado aceitável quando todos os itens abaixo forem demonstrados em ambiente de homologação:

1. Uma organização consegue criar usuários, empresas e lojas com papéis diferentes.
2. Usuário de uma organização não acessa dados de outra por nenhum endpoint ou exportação testados.
3. Uma conta de marketplace autorizada pode ser conectada, sincronizada, desconectada e reautorizada.
4. Catálogo, anúncios e pedidos próprios são importados com idempotência e histórico de sincronização.
5. O sistema permite pesquisar produtos e visualizar dados com fonte, data e classificação observado/importado/estimado.
6. O sistema permite monitorar concorrentes apenas dentro das fontes e políticas aprovadas.
7. O usuário consegue analisar anúncio, receber score explicável e gerar sugestões de título/descrição sem fabricação de especificações.
8. O motor financeiro calcula margem com memória de cálculo, regra versionada e sinalização de dados ausentes.
9. Dashboard e exportações usam os mesmos critérios de cálculo e filtros.
10. Logs, métricas, tracing, alertas operacionais, backups e restauração foram testados.
11. Testes de segurança essenciais e testes E2E P0 foram aprovados.
12. Termos, privacidade, retenção e consentimentos necessários estão publicados e revisados.

---

# 17. Definição de pronto (Definition of Done)

Uma funcionalidade será considerada concluída somente quando:

- Requisito e critérios de aceite estiverem vinculados à implementação.
- Fluxo feliz, erros e cenários de borda estiverem testados.
- Autorização por tenant, papel e escopo estiver validada.
- Logs e métricas relevantes estiverem presentes, sem vazamento de segredo ou dado sensível.
- Contratos de API e eventos estiverem documentados e versionados.
- Tratamento de timeout, retry, idempotência e indisponibilidade estiver definido quando aplicável.
- Documentação do usuário e suporte estiver atualizada.
- Acessibilidade e responsividade tiverem sido verificadas.
- Testes automatizados estiverem no pipeline.
- Evidência de homologação estiver anexada à tarefa/PR.

---

# 18. Métricas operacionais e SLOs

| Área | Indicador | Alvo inicial / política |
|---|---|---|
| API | Latência p95 de operações comuns | < 2,5 s em carga definida, excluindo terceiros e jobs longos. |
| Sincronização | Taxa de sucesso | Monitorar por conector e definir baseline no piloto. |
| Dados | Freshness | Exibir idade do dado; SLA varia por conector e limite externo. |
| Filas | Idade da mensagem mais antiga | Alertar quando exceder o SLA do tipo de job. |
| Segurança | Eventos de acesso negado cross-tenant | 100% registrados e investigados conforme severidade. |
| Financeiro | Reprodutibilidade do cálculo | Mesma entrada + mesma versão de regra = mesmo resultado. |
| IA | Taxa de resposta sem evidência | Monitorar e reduzir; respostas sem fonte devem declarar limitação. |
| Jobs | Taxa de retry e dead-letter | Dashboard e alerta por tipo de job. |

---

# 19. Decisões arquiteturais iniciais propostas

Estas decisões devem ser ratificadas em ADRs antes de implementação de produção:

- **ADR-001:** começar com monólito modular, API e workers assíncronos.
- **ADR-002:** PostgreSQL como banco transacional principal.
- **ADR-003:** contratos de conectores independentes por marketplace.
- **ADR-004:** APIs oficiais/autorizadas como fonte primária; nenhuma dependência de scraping que viole políticas.
- **ADR-005:** motor financeiro determinístico e versionado.
- **ADR-006:** todo dado de mercado terá proveniência, timestamp e classificação de confiança.
- **ADR-007:** MCP inicialmente read-only; ferramentas mutáveis exigirão confirmação explícita.
- **ADR-008:** aplicação multi-tenant com autorização no backend e defesa em profundidade no banco.
- **ADR-009:** stack de referência Java 21/Spring Boot, React/Next.js, TypeScript, PostgreSQL, Redis e RabbitMQ, sujeita a validação da equipe.

---

# 20. Questões em aberto — OPEN_QUESTIONS.md

As questões abaixo bloqueiam decisões de escopo ou arquitetura e devem ser resolvidas na Fase 0. Não devem ser respondidas por suposição.

| ID | Questão | Por que importa | Responsável sugerido | Estado |
|---|---|---|---|---|
| OQ-001 | Quais endpoints, escopos e permissões estarão disponíveis para cada marketplace e região? | Define o que pode ser sincronizado e exibido. | Tech Lead/Integrações | OPEN |
| OQ-002 | Quais dados competitivos podem ser obtidos legitimamente e com que frequência? | Define cobertura e custo da inteligência de mercado. | Produto/Jurídico/Integrações | OPEN |
| OQ-003 | A extensão será usada apenas como interface contextual ou também como fonte de captura permitida? | Impacta políticas, privacidade e arquitetura. | Produto/Jurídico | OPEN |
| OQ-004 | Qual é o conjunto exato de componentes da margem por marketplace/modalidade? | Evita resultados financeiros incorretos. | Produto/Financeiro | OPEN |
| OQ-005 | Como serão tratados regimes tributários e particularidades por UF/categoria? | Afeta modelo de cálculo. | Financeiro/Especialista fiscal | OPEN |
| OQ-006 | Quais provedores/fontes de Ads oferecem granularidade por campanha, SKU e período? | Define métricas e atribuição. | Integrações/Produto | OPEN |
| OQ-007 | Qual política de retenção para snapshots, pedidos, dados pessoais e arquivos? | Impacta LGPD, storage e custo. | Segurança/Privacidade | OPEN |
| OQ-008 | Qual volume inicial esperado de organizações, SKUs, pedidos e snapshots por dia? | Base para sizing e arquitetura analítica. | Produto/Engenharia | OPEN |
| OQ-009 | Quais idiomas/moedas/regiões serão suportados no lançamento? | Impacta normalização, moeda e UX. | Produto | OPEN |
| OQ-010 | A publicação de anúncios entra no MVP ou somente na Fase 2? | Define escopo de escrita externa e risco operacional. | Produto | OPEN |
| OQ-011 | Quais canais de notificação serão suportados primeiro? | Impacta integração e preferências. | Produto/Engenharia | OPEN |
| OQ-012 | MCP será lançado como servidor remoto multi-tenant, integração interna ou ambos? | Define autenticação, distribuição e segurança. | Arquitetura/Produto | OPEN |
| OQ-013 | Quais provedores de IA serão aprovados e quais dados podem ser enviados a cada provedor? | Impacta privacidade, custo e governança. | Segurança/IA/Produto | OPEN |
| OQ-014 | Qual será a metodologia inicial do score de anúncio e score de oportunidade? | Garante explicabilidade e validação. | Produto/Data | OPEN |
| OQ-015 | O dashboard financeiro priorizará regime de caixa, competência ou oferecerá ambos? | Evita ambiguidade em DRE e conciliação. | Financeiro/Produto | OPEN |

---

# 21. Modelo de `DECISION_LOG.md`

Toda decisão que altere escopo, arquitetura, regra financeira, integração ou política de dados deverá ser registrada com este formato:

```markdown
## DEC-001 — [Título da decisão]
- Data: AAAA-MM-DD
- Status: PROPOSED | ACCEPTED | REJECTED | SUPERSEDED
- Contexto: qual problema precisa ser resolvido?
- Opções consideradas:
  1. Opção A
  2. Opção B
- Decisão: opção selecionada.
- Justificativa: critérios técnicos, de produto, segurança e custo.
- Consequências: impactos positivos, negativos e trabalho decorrente.
- Requisitos relacionados: FR-XXX-001, NFR-XXX-001.
- Responsável: papel/nome.
- Revisão: condição ou data para reavaliar.
```

---

# 22. Glossário

| Termo | Definição |
|---|---|
| Seller | Pessoa ou empresa que vende em marketplace. |
| Marketplace | Canal/plataforma de comércio eletrônico. |
| Listing/Anúncio | Oferta publicada em marketplace. |
| SKU | Identificador interno de produto/variação. |
| EAN | Código de identificação comercial do produto. |
| CTR | Cliques divididos por impressões, conforme definição da fonte. |
| CPC | Custo por clique. |
| ROAS | Receita atribuída a Ads dividida pelo investimento em Ads. |
| ACOS | Investimento em Ads dividido pela receita atribuída a Ads. |
| TACOS | Investimento em Ads dividido pela receita total considerada. |
| Margem de contribuição | Receita líquida menos custos e despesas variáveis definidos na política de cálculo. |
| DRE | Demonstração do Resultado do Exercício; neste produto, visão gerencial e não escrituração oficial. |
| Buy Box | Área/oferta destacada em determinados marketplaces e contextos de catálogo. |
| MCP | Model Context Protocol, protocolo para disponibilizar contexto e ferramentas a aplicações de IA. |
| Tenant | Organização isolada logicamente na plataforma. |
| RPO | Recovery Point Objective: perda máxima de dados tolerada em recuperação. |
| RTO | Recovery Time Objective: tempo-alvo para recuperação. |
| Proveniência | Registro da fonte, momento e método usados para obter um dado. |

---

# 23. Referências de benchmark e limitações

Plataformas usadas como referência funcional no levantamento que originou este PRD:

- Avantpro: https://avantpro.com.br/
- Avantpro Mercado Livre: https://avantpro.com.br/ml/
- Avantpro Shopee: https://avantpro.com.br/shp/
- Avantpro Amazon: https://avantpro.com.br/amz/
- Avantpro Control: https://avantpro.com.br/control/
- Avantpro MCP: https://avantpro.com.br/mcp/
- JoomPulse: https://joompulse.com/
- GoSmarter: https://gosmarter.com.br/

**Limitação:** o benchmark deriva de informações públicas descritas no levantamento anterior. Recursos podem mudar, exigir login, variar por plano/região ou não estar disponíveis para todos os usuários. Antes da implementação, validar novamente páginas, demonstrações, documentação, permissões, APIs e políticas. O PRD não especifica cópia de código, interface proprietária, conteúdo protegido ou métodos de coleta proibidos; descreve capacidades de produto a serem implementadas de forma independente e legítima.

---

# 24. Aprovação e controle de versão

| Versão | Data | Alteração | Responsável | Status |
|---|---|---|---|---|
| 1.0 | 17/11/2026 | Primeira versão consolidada do PRD a partir da análise de benchmark. | Produto/Arquitetura | Proposta para revisão |

**Aprovações necessárias antes de iniciar o desenvolvimento:**

- Product Owner: valida problema, personas, escopo e prioridades.
- Tech Lead/Arquiteto: valida arquitetura, integrações e requisitos não funcionais.
- Segurança/Privacidade: valida LGPD, controle de acesso, extensão e IA.
- Responsável Financeiro/Fiscal: valida regras de margem e relatórios gerenciais.
- Responsável por Integrações: confirma viabilidade técnica e contratual dos marketplaces.
