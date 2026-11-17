# Manual do Sistema & Guia de Operações da Seller Intelligence Platform

> **Documento Operacional e Técnico Oficial**  
> **Versão:** 1.0.0 &middot; **Arquitetura:** Monólito Modular (Java 25 LTS + Next.js 15)  
> **Segurança:** Isolamento Multi-tenant Estrito &middot; Política Fail-Closed Ativa &middot; Chrome Manifest V3  

---

## 1. Visão Geral e Princípios Arquiteturais

A **Seller Intelligence Platform** é uma plataforma SaaS multi-tenant concebida para vendedores profissionais de e-commerce e marketplaces. Sua engenharia é fundamentada em quatro pilares estratégicos e dois pilares estruturais de gestão e governança:

1. **Market Intelligence:** Descoberta de produtos, categorias, volumes e preços observados em fontes oficiais.
2. **AI Commerce:** Análise auditável de qualidade de anúncios e geração assistida com guardrails anti-alucinação.
3. **Seller Control & Finanças:** Cálculo financeiro determinístico em `BigDecimal`, margens de contribuição e conciliação de repasses.
4. **Business Intelligence & Ações:** Inbox de alertas acionáveis com deduplicação temporal e monitoramento contínuo de concorrência.
5. **Gestão & Integrações:** Governança multi-empresa, multi-CNPJ, multi-loja e matriz de conectores oficiais.
6. **Segurança & Governança:** Continuous Marketplace Compliance com política *Fail-Closed*, auditoria em trilha imutável e isolamento de dados por tenant.

### Invariantes Técnicas Mandatórias
* **Cálculo Financeiro Determinístico:** Proibição irrestrita de tipos de ponto flutuante binário (`float`/`double`) ou modelos LLM para cálculos monetários. Todas as operações utilizam `BigDecimal` com escala e modo de arredondamento explícitos (`RoundingMode.HALF_UP` ou `HALF_EVEN`).
* **Isolamento Multi-Tenant:** Toda entidade, consulta SQL, chave de cache e mensagem de mensageria possui obrigatoriamente a identificação `tenant_id`. Nenhuma transação pode acessar dados fora do tenant autenticado.
* **Rotulagem e Proveniência de Dados:** Todo dado externo de mercado deve possuir timestamp de coleta e rótulo explícito de proveniência:
  * `OBSERVED`: Coletado via API oficial autorizada.
  * `IMPORTED`: Inserido formalmente pelo usuário via planilha ou conciliação.
  * `ESTIMATED`: Calculado por modelo estatístico ou regras documentadas.
  * `AI_GENERATED`: Gerado assistidamente por modelo de inteligência artificial.
  * `UNAVAILABLE`: Dado indisponível na fonte. **Nunca é transformado em zero.**
* **Proteção de Contas e Zero Scraping Hostil:** É terminantemente proibido o contorno de CAPTCHAs, emulação de navegação para quebra de proteções, reutilização indevida de cookies de sessão de vendedores ou qualquer operação que viole as políticas de uso dos marketplaces.

---

## 2. Catálogo Funcional dos Módulos

```
                                    ┌────────────────────────────────────────────────────────┐
                                    │             SELLER INTELLIGENCE PLATFORM               │
                                    └────────────────────────────────────────────────────────┘
                                                                 │
     ┌──────────────────────┬──────────────────────┬─────────────┴────────┬──────────────────────┬──────────────────────┐
     ▼                      ▼                      ▼                      ▼                      ▼                      ▼
┌──────────────┐      ┌──────────────┐      ┌──────────────┐      ┌──────────────┐      ┌──────────────┐      ┌──────────────┐
│   Business   │      │    Market    │      │  AI Commerce │      │Seller Control│      │   Gestão &   │      │ Segurança &  │
│ Intelligence │      │ Intelligence │      │              │      │  & Finanças  │      │ Integrações  │      │  Governança  │
├──────────────┤      ├──────────────┤      ├──────────────┤      ├──────────────┤      ├──────────────┤      ├──────────────┤
│• Smart Alerts│      │• Pesquisa de │      │• AI Listing  │      │• Motor Finan-│      │• Empresas &  │      │• Compliance  │
│  & Action    │      │  Mercado     │      │  Studio      │      │  ceiro & Mar-│      │  CNPJs       │      │  Contínuo    │
│  Center      │      │• Product     │      │• Assistente  │      │  gem Real    │      │• Lojas &     │• Diagnóstico │
│• Monitora-   │      │  Discovery   │      │  Conversaci- │      │• DRE Gerenci-│      │  Marketplaces│  de Infra-   │
│  mento de    │      │  (PDFs &     │      │  onal & MCP  │      │  al & Conci- │      │• Sincroniza- │  estrutura   │
│  Concorrentes│      │  Imagens)    │      │  Read-Only   │      │  liação      │      │  ção Assínc. │              │
└──────────────┘      └──────────────┘      └──────────────┘      └──────────────┘      └──────────────┘      └──────────────┘
```

---

### 2.1. Pilar: Business Intelligence & Ações

#### Módulo 1: Smart Alerts & Action Center (`ACTION_CENTER`)
* **Objetivo:** Transformar anomalias e oportunidades detectadas no negócio em um fluxo unificado de tarefas operacionais com responsáveis, prazos e rastreabilidade.
* **Mecânica Interna:**
  * Avalia eventos de redução de margem, queda repentina de vendas, alteração de preço por concorrentes e falhas de sincronização de catálogo.
  * Mecanismo de deduplicação temporal (*cooldown* padrão de 5 minutos) indexado por `(tenant_id, rule_type, target_entity_id)` para impedir saturação de alertas similares.
  * Transições formais de estado: `NEW` $\rightarrow$ `ACKNOWLEDGED` $\rightarrow$ `CONVERTED_TO_TASK` $\rightarrow$ `RESOLVED` (ou `DISMISSED` com justificativa auditada).
* **Passo a Passo de Uso:**
  1. No menu lateral, acesse **Smart Alerts & Action Center**.
  2. Na aba **Alertas Operacionais**, filtre os eventos por severidade (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`).
  3. Clique em **Converter em Tarefa** em um alerta (ex.: alerta de margem abaixo do mínimo) para definir prioridade, responsável e prazo de resolução.
  4. Alterne para a aba **Regras & Automação** para ajustar thresholds e testar gatilhos sintéticos de simulação.

#### Módulo 2: Monitoramento & Concorrentes (`MONITORING`)
* **Objetivo:** Rastrear a evolução de preços, estoque e estratégia de anúncios rivais sem empregar web scraping predatório.
* **Mecânica Interna:**
  * Utiliza endpoints públicos autorizados das APIs dos marketplaces para obter snapshots periódicos de anúncios concorrentes.
  * Armazena histórico temporal imutável com timestamp e identificação de canal.
  * Calcula variações relativas (%) e alerta caso um concorrente ganhe a Buy Box ou reduza o preço abaixo da margem de segurança do vendedor.
* **Passo a Passo de Uso:**
  1. Acesse **Monitoramento & Concorrentes**.
  2. Clique em **Adicionar Anúncio à Watchlist** e informe o identificador canônico (ex.: `MLB123456789`).
  3. Visualize o gráfico de evolução de preço ao longo dos últimos 30 dias.
  4. Ative a notificação de divergência de preço para receber alertas automáticos no Action Center.

---

### 2.2. Pilar: Market Intelligence

#### Módulo 3: Pesquisa de Mercado (`MARKET`)
* **Objetivo:** Fornecer inteligência acionável sobre demanda, preço médio praticado, volume de vendas e concorrência por categoria.
* **Mecânica Interna:**
  * Integração com as APIs oficiais (ex.: Mercado Livre `/sites/MLB/search`).
  * Toda métrica apresentada na interface recebe rotulagem estrita de proveniência (`OBSERVED`, `ESTIMATED` ou `UNAVAILABLE`).
  * Filtros determinísticos por faixa de preço, condição do produto (novo/usado), tipo de envio (Full, normal) e reputação do vendedor.
* **Passo a Passo de Uso:**
  1. Acesse **Pesquisa de Mercado**.
  2. Digite termos de busca ou navegue pelas árvores de categorias homologadas.
  3. Avalie métricas de faturamento estimado da categoria, ticket médio e concentração de mercado.
  4. Clique no ícone de estrela para salvar termos promissores no painel de oportunidades do seller.

#### Módulo 4: Product Discovery — Catálogos PDF e Busca Visual (`DISCOVERY`)
* **Objetivo:** Processar catálogos brutos de fornecedores em formato PDF e imagens de produtos para localizar equivalentes no mercado e enriquecer a base local.
* **Mecânica Interna:**
  * Pipeline de ingestão seguro com sanitização contra ataques de injeção de prompt via metadados de arquivos (OWASP LLM01).
  * Extração de texto e atributos (SKU, EAN/GTIN, dimensões, peso, preço de tabela) com cálculo de confiança estatística por campo (0% a 100%).
  * Exigência de revisão humana obrigatória: os dados extraídos permanecem no estado `PENDING_REVIEW` até aprovação expressa do usuário.
* **Passo a Passo de Uso:**
  1. Acesse **Product Discovery**.
  2. Na aba **Upload de Arquivo**, envie o PDF de tabela de fornecedor ou a foto de um produto (máximo 15MB).
  3. Na aba **Itens Extraídos**, revise cada linha: valide os campos com baixa confiança e clique em **Aprovar Item**.
  4. Na aba **Busca Visual**, envie a imagem de uma peça para localizar produtos similares cadastrados no catálogo de mercado.

---

### 2.3. Pilar: AI Commerce

#### Módulo 5: AI Listing Studio (`AI_STUDIO`)
* **Objetivo:** Auditar anúncios existentes contra as melhores práticas de SEO dos marketplaces e gerar sugestões assistidas de títulos e fichas técnicas.
* **Mecânica Interna:**
  * Cálculo do **Score de Qualidade do Anúncio** (0 a 100) distribuído em 4 dimensões auditáveis:
    * *Título (25%):* Comprimento ideal (40-60 caracteres), inclusão de marca, modelo e atributo primário sem superlativos proibidos ("o melhor", "grátis").
    * *Descrição (30%):* Clareza, benefícios, dados técnicos e compatibilidade.
    * *Ficha Técnica (25%):* Preenchimento de atributos obrigatórios da categoria e validação de código EAN.
    * *Fotos (20%):* Resolução mínima (1200x1200px), fundo branco puro e ausência de marcas d'água proibidas.
  * Guardrails anti-alucinação: a IA é instruída a recusar categoricamente inventar especificações técnicas não presentes no cadastro.
* **Passo a Passo de Uso:**
  1. Acesse **AI Listing Studio**.
  2. Selecione um produto ou anúncio importado da sua loja.
  3. Observe o diagnóstico de pontuação e as recomendações de melhoria.
  4. Utilize o botão **Otimizar Título com IA** para gerar variações baseadas nas palavras-chave mais buscadas.
  5. Salve uma nova versão com histórico auditável antes de submeter ao marketplace.

#### Módulo 6: Assistente Conversacional & Servidor MCP Read-Only (`ASSISTANT`)
* **Objetivo:** Disponibilizar interface em linguagem natural para responder a dúvidas operacionais e analíticas fundamentadas em dados reais do vendedor, além de servir ferramentas seguras via Model Context Protocol (MCP).
* **Mecânica Interna:**
  * Catálogo de ferramentas MCP estritamente *somente leitura*: consultas de catálogo, vendas históricas, cálculo de margens e checagem de conformidade.
  * Bloqueio sumário (`BLOCKED_MUTABLE_DENIED`) de qualquer comando que tente alterar preços, pausar anúncios ou transferir valores.
  * Injeção mandatória do contexto de segurança: consultas não executam SQL gerado livremente; utilizam métodos tipados com o `tenant_id` fixado pelo token de autenticação.
* **Passo a Passo de Uso:**
  1. Acesse **Assistente & MCP**.
  2. Faça perguntas operacionais (ex.: *"Qual SKU apresentou a maior margem de contribuição no último mês?"* ou *"Simule a margem para o item TECLADO-RGB se o preço for R$ 120"*).
  3. Visualize as ferramentas invocadas e a memória de cálculo nas respostas geradas.
  4. Na aba **Configuração MCP**, consulte as credenciais de integração com clientes externos (ex.: Claude Desktop, Cursor, IDEs).

---

### 2.4. Pilar: Seller Control & Finanças

#### Módulo 7: Motor Financeiro & Rentabilidade Real (`FINANCIAL`)
* **Objetivo:** Fornecer o cálculo exato e determinístico da margem de contribuição, lucro líquido e ponto de equilíbrio (*break-even*) para cada SKU e canal de venda.
* **Mecânica Interna:**
  * Modelagem determinística em `BigDecimal` considerando:
    $$\text{Margem de Contribuição} = \text{Preço Bruto} - \text{Taxa Marketplace} - \text{Frete} - \text{Impostos} - \text{CMV} - \text{Custos Operacionais}$$
  * Parametrização tributária por regime da empresa: Simples Nacional (com faixas do anexo configuráveis), Lucro Presumido ou Lucro Real.
  * Tratamento de taxas de comissão percentuais e custos fixos por faixa de preço (ex.: taxa fixa de R$ 6,00 para itens abaixo de R$ 79,00).
* **Passo a Passo de Uso:**
  1. Acesse **Motor Financeiro**.
  2. Alterne entre o **Painel Financeiro & Rentabilidade** (visão analítica dos últimos períodos) e o **Simulador & Regras de Preço**.
  3. No simulador, selecione a empresa/CNPJ de origem e o canal de venda (ex.: Mercado Livre Clássico vs Premium).
  4. Informe o CMV e os custos de embalagem; o motor informará instantaneamente o preço mínimo de venda para atingir a margem desejada.

#### Módulo 8: DRE Gerencial & Conciliação de Repasses (`RECONCILIATION_DRE`)
* **Objetivo:** Conciliar os repasses financeiros creditados pelos marketplaces contra os pedidos e eventos de venda faturados, identificando retenções indevidas ou divergências de taxas.
* **Mecânica Interna:**
  * Importação idempotente de extratos de liquidação financeira (evita duplicidade de lançamentos via hash do evento).
  * Classificação dos repasses em quatro estados: `CONCILIATED` (valores batem integralmente), `PENDING` (aguardando liberação do marketplace), `DIVERGENT` (diferença entre valor contratado e valor liberado) e `UNIDENTIFIED` (créditos sem pedido correspondente).
  * Exibição de DRE Gerencial nos regimes de **Competência** (pela data da venda) ou **Caixa** (pela data da efetiva liberação financeira).
* **Passo a Passo de Uso:**
  1. Acesse **DRE & Conciliação**.
  2. Na aba **Conciliação de Repasses**, importe o extrato financeiro do marketplace em arquivo CSV ou sincronize via conector.
  3. Visualize os pedidos com status `Divergente`: clique no registro para verificar o valor esperado versus o valor liberado e a taxa cobrada a maior.
  4. Na aba **DRE Gerencial**, selecione o período contábil e visualize a cascata de receita bruta, deduções, margem e resultado operacional.

---

### 2.5. Pilar: Gestão & Integrações

#### Módulo 9: Gestão de Empresas & CNPJs (`COMPANIES`)
* **Objetivo:** Cadastrar e organizar as razões sociais, nomes fantasia e parâmetros fiscais de cada entidade jurídica do grupo vendedor.
* **Mecânica Interna:**
  * Validação determinística de dígitos verificadores de CNPJ e formatação canônica (`XX.XXX.XXX/XXXX-XX`).
  * Associação de regime tributário padrão (`SIMPLES_NACIONAL`, `LUCRO_PRESUMIDO`, `LUCRO_REAL`).
  * Isolamento lógico em nível de linha (*Row-Level Security*) ancorado no identificador da empresa.
* **Passo a Passo de Uso:**
  1. Acesse **Empresas & CNPJs**.
  2. Clique em **Cadastrar Empresa**.
  3. Preencha CNPJ, Razão Social, Nome Fantasia e Regime Tributário.
  4. Salve a empresa para habilitá-la como proprietária de contas nos marketplaces.

#### Módulo 10: Lojas & Marketplaces (`STORES`)
* **Objetivo:** Gerenciar as contas e lojas conectadas aos canais homologados (Mercado Livre, Shopee, Amazon), vinculando-as ao CNPJ correspondente.
* **Mecânica Interna:**
  * Fluxo de autorização OAuth 2.0 oficial com armazenamento de tokens criptografados em repouso com algoritmo AES-256-GCM.
  * Chaves e credenciais segregadas por tenant; proteção contra exposição de segredos em logs ou respostas de API.
* **Passo a Passo de Uso:**
  1. Acesse **Lojas & Marketplaces**.
  2. Clique em **Conectar Nova Loja**.
  3. Selecione a empresa mantenedora e o canal desejado (ex.: Mercado Livre).
  4. Complete o fluxo oficial de autorização no portal do parceiro.

#### Módulo 11: Sincronização & Catálogo (`SYNC`)
* **Objetivo:** Orquestrar a ingestão assíncrona e periódica de pedidos, inventário e alterações cadastrais entre as lojas e o monólito.
* **Mecânica Interna:**
  * Fila de mensageria assíncrona gerenciada com RabbitMQ e Dead-Letter Queues (DLQ) para retry idempotente.
  * Contingência para upload manual de planilhas CSV com tratamento seguro contra injeção de fórmulas CSV (bloqueio de caracteres `=`, `+`, `-`, `@`).
* **Passo a Passo de Uso:**
  1. Acesse **Sincronização & Catálogo**.
  2. Monitore a saúde das filas e o status da última execução de sincronismo.
  3. Caso haja instabilidade na API externa, utilize a aba **Importação por Planilha** para atualizar estoque em regime de contingência.

#### Módulo 12: Capacidades & Conectores (`CAPABILITIES`)
* **Objetivo:** Exibir de forma transparente a matriz oficial de recursos suportados (`SUPPORTED`) e não suportados (`UNSUPPORTED`) por marketplace parceiro.
* **Mecânica Interna:**
  * Documentação viva baseada nas capacidades homologadas nas APIs oficiais.
  * Veda a suposição de recursos inexistentes (ex.: emissão fiscal direta em canais que não expõem endpoint oficial).
* **Passo a Passo de Uso:**
  1. Acesse **Capacidades & Conectores**.
  2. Compare recursos entre Mercado Livre, Shopee e Amazon (ex.: webhooks de pedidos, suporte a variações, precificação em lote).

---

### 2.6. Pilar: Segurança & Governança

#### Módulo 13: Continuous Marketplace Compliance & Políticas (`COMPLIANCE`)
* **Objetivo:** Monitorar permanentemente a conformidade legal e técnica das integrações com as políticas dos marketplaces, aplicando proteção Fail-Closed caso haja risco para a conta do vendedor.
* **Mecânica Interna:**
  * Monitoramento de hashes criptográficos (SHA-256) das documentações e termos oficiais de uso das APIs parceiras.
  * Detecção de alterações de escopos OAuth, deprecation de endpoints ou redução de limites de chamadas (*rate limits*).
  * Princípio do Menor Escopo (*Least Scope*): em caso de violação ou incidente, suspende exclusivamente o endpoint ou feature impactada, mantendo o restante da plataforma operacional.
  * Reativação formal: proibição de reativação automática pós-incidente; exige aprovação humana, justificativa registrada e evidência de testes em trilha imutável.
* **Passo a Passo de Uso:**
  1. Acesse **Compliance & Políticas**.
  2. Na aba **Overview**, visualize os indicadores de conformidade de cada canal conectado.
  3. Na aba **Incidentes & Políticas**, acompanhe o histórico de revisões de termos dos marketplaces.
  4. Na aba **Reativação Segura**, submeta a solicitação formal de liberação de uma feature suspensa, informando o código da aprovação técnica e os testes executados.

#### Módulo 14: Diagnóstico de Infraestrutura & Saúde (`HEALTH`)
* **Objetivo:** Monitorar em tempo real a conectividade, latência e integridade das dependências operacionais do sistema.
* **Mecânica Interna:**
  * Integração com Spring Boot Actuator (`/actuator/health`) com verificadores de saúde customizados para PostgreSQL, Redis e RabbitMQ.
  * Sanitização de respostas: métricas não expõem senhas, strings de conexão ou detalhes sensíveis da infraestrutura interna.
* **Passo a Passo de Uso:**
  1. Acesse **Diagnóstico de Infraestrutura**.
  2. Verifique o status operacional (`UP` ou `DOWN`) dos serviços centrais.
  3. Inspecione tempos de resposta e uso de memória da JVM.

---

## 3. Passo a Passo de Instalação e Configuração Local (Setup)

### 3.1. Pré-requisitos de Máquina

| Software | Versão Mínima | Finalidade |
|---|---|---|
| **Java JDK** | 25 LTS (OpenJDK / Temurin) | Compilação e execução do monólito backend com Virtual Threads |
| **Node.js** | 22.x LTS ou superior | Ambiente de execução do frontend |
| **pnpm** | 9.x ou superior | Gerenciador de pacotes do frontend e da extensão |
| **Docker** | 24.x+ com Docker Compose v2 | Execução dos bancos de dados e mensageria |
| **Apache Maven** | 3.9+ | Gerenciador de ciclo de vida do backend |

### 3.2. Mapeamento de Portas e Infraestrutura Local

| Serviço | Porta Padrão | Variável / Configuração | Observações |
|---|---|---|---|
| **Frontend Web** | `3000` | `PORT=3000` | Next.js 15 App Router |
| **Backend Spring Boot** | `8082` | `SERVER_PORT=8082` | Monólito Modular Java 25 |
| **PostgreSQL 17** | `5432` | `SPRING_DATASOURCE_URL` | Base ACID multi-tenant |
| **Redis 7** | `6379` | `SPRING_DATA_REDIS_PORT` | Cache e limites de rate limit |
| **RabbitMQ AMQP** | `5672` | `SPRING_RABBITMQ_PORT` | Filas assíncronas e mensageria |
| **RabbitMQ Management** | `15672` | Interface Web (`guest/guest`) | Dashboard de acompanhamento |
| **Keycloak Identity** | `8085` | `KC_HTTP_PORT=8085` | Provedor de identidade e OAuth 2.0 |

### 3.3. Roteiro Passo a Passo de Execução

#### Passo 1: Clonar o Repositório e Navegar até a Pasta Raiz
```bash
git clone https://github.com/empresa/antigravity-avantepro-clone.git
cd antigravity-avantepro-clone
```

#### Passo 2: Inicializar os Contêineres de Infraestrutura
Execute o comando abaixo na raiz do projeto para subir PostgreSQL, Redis, RabbitMQ e Keycloak:
```bash
docker compose up -d
```
Verifique se todos os contêineres estão operacionais com status `healthy`:
```bash
docker compose ps
```

#### Passo 3: Compilar e Executar o Backend Spring Boot
O Spring Boot executará as migrações Flyway automaticamente da versão `V1` até `V14`:
```bash
mvn -f backend/pom.xml clean spring-boot:run
```
Aguarde a mensagem no log confirmando que a aplicação subiu na porta **8082**:
```
Started SellerIntelligenceApplication in X.XXX seconds (process running for X.XXX)
```
Teste a saúde do backend em outro terminal:
```bash
curl http://localhost:8082/actuator/health
# Resposta esperada: {"status":"UP"}
```

#### Passo 4: Instalar Dependências e Executar o Frontend
Em um novo terminal, entre no diretório do frontend:
```bash
cd frontend
pnpm install
pnpm run dev
```
O console exibirá:
```
▲ Next.js 15.5.29
- Local: http://localhost:3000
```
Abra o navegador no endereço `http://localhost:3000` para acessar o sistema.

---

## 4. Guia da Extensão Contextual de Navegador (Chrome Manifest V3)

A extensão da Seller Intelligence Platform localiza-se na pasta `extension/`. Ela foi construída com TypeScript e segue rigorosamente as diretrizes da **Chrome Extension Manifest V3**.

```
extension/
├── manifest.json         # Manifesto estrito V3 (activeTab, storage)
├── package.json          # Scripts e dependências de build (vite/esbuild)
├── tsconfig.json         # Tipagem estrita TypeScript
└── src/
    ├── popup.ts          # Lógica do painel ao clicar no ícone
    ├── popup.html        # Interface do popup contextual
    ├── content.ts        # Leitura passiva da URL pública do produto
    └── background.ts     # Service worker do Manifest V3
```

### 4.1. Princípio do Menor Privilégio e Proteção ao Usuário
* **Permissões Mínimas Solicitadas:** Apenas `activeTab` e `storage`.
* **Zero Coleta Oculta:** A extensão não injeta scripts em background indiscriminadamente. Ela só é ativada quando o usuário clica expressamente no ícone da extensão enquanto navega em um produto suportado.
* **Zero Acesso a Credenciais:** É expressamente proibido ler cookies de sessão, credenciais ou dados bancários do vendedor.
* **Domínios Suportados:** Declarados estritamente no manifesto para correspondência de URLs de anúncio:
  * `*://*.mercadolivre.com.br/*`
  * `*://*.shopee.com.br/*`

### 4.2. Como Compilar a Extensão Localmente
1. Acesse o diretório da extensão no terminal:
   ```bash
   cd extension
   pnpm install
   ```
2. Execute o build para gerar a pasta `dist/`:
   ```bash
   pnpm run build
   ```
3. O diretório `extension/dist` conterá os arquivos finais `manifest.json`, `popup.html`, `popup.js`, `content.js` e ícones.

### 4.3. Como Instalar no Navegador (Modo Desenvolvedor)
1. Abra o navegador Google Chrome (ou Brave / Microsoft Edge).
2. Acesse a URL: `chrome://extensions`.
3. No canto superior direito, ative a chave **Modo do desenvolvedor** (*Developer mode*).
4. Clique no botão **Carregar sem compactação** (*Load unpacked*).
5. Na janela de seleção de pasta, selecione o caminho absoluto do diretório compilado:
   `/caminho/para/antigravity-avantepro-clone/extension/dist`
6. A extensão **Seller Intelligence Platform - Extensão Contextual** aparecerá listada e pronta para uso.

### 4.4. Como Testar e Usar na Prática
1. Abra uma aba no navegador e navegue até um anúncio público de produto no Mercado Livre ou Shopee.
2. Clique no ícone do pin da extensão na barra de ferramentas do Chrome para fixar o ícone da **SI**.
3. Clique no ícone da extensão:
   * O script lê o ID público do anúncio (ex.: `MLB123456789`).
   * O popup consulta o backend local em `http://localhost:8082` e exibe:
     * Estimativa de comissão do marketplace.
     * Histórico de variações de preço do produto.
     * Botão direto para importar o produto para o simulador do **AI Listing Studio**.

---

## 5. Procedimento de Testes Automatizados Locais

A Seller Intelligence Platform conta com uma suíte de testes em camadas, garantindo ausência de regressões, conformidade arquitetural e segurança multi-tenant.

### 5.1. Testes do Backend (Spring Boot & Modulith)
Executam testes unitários, testes de arquitetura (ArchUnit), testes de contrato e validações de concorrência:
```bash
mvn -f backend/pom.xml test
```
* **Testes de Arquitetura:** Verificam que nenhum módulo acessa classes internas de outro módulo sem passar pela interface pública oficial.
* **Testes de Isolamento Multi-Tenant:** Simulam tentativas adversariais de injeção de parâmetros para ler registros de outros tenants.
* **Testes de Compliance:** Validam que incidentes de termos de uso suspendem adequadamente os endpoints dependentes em regime *Fail-Closed*.

### 5.2. Testes do Frontend (Vitest & Testing Library)
Executam 75 testes automatizados cobrindo os 14 módulos, o guia interativo e o layout mobile:
```bash
pnpm --dir frontend run test
```
Para rodar a verificação de tipos e compilação do Next.js:
```bash
pnpm --dir frontend run build
```

### 5.3. Testes da Extensão Chrome
Executam os testes unitários do extrator de identificadores e comunicação de mensagens:
```bash
pnpm --dir extension run test
```

### 5.4. Scripts de Verificação de Phase Gates
Para atestar formalmente a integridade da entrega dos gates e regras de conformidade:
```bash
python3 scripts/verify_phase21.py
```
O script executa a matriz de integridade estrutural, checagem de hashes e ausência de violações de segurança.

---

## 6. Guia de Publicação e Operação em Ambiente Produtivo

### 6.1. Checklist Obrigatório de Segurança Pré-Deploy

* [ ] **Criptografia de Segredos:** A variável de ambiente `ENCRYPTION_SECRET_KEY` deve conter uma chave de 256 bits gerada aleatoriamente em HSM ou KMS para cifra dos tokens OAuth em repouso.
* [ ] **Zero Credenciais em Repositório:** Nenhuma senha de banco, chave de API de LLM ou segredo de marketplace pode residir em código ou arquivos de configuração versionados no Git.
* [ ] **Política Fail-Closed Ativa:** A propriedade `seller.compliance.allow-fake-marketplace=false` deve estar estritamente configurada no perfil de produção (`application-prod.yml`).
* [ ] **Terminação TLS 1.3:** O tráfego público deve passar obrigatoriamente por proxy reverso (Nginx / Cloudflare / Ingress Controller) com TLS 1.3 e cabeçalhos de segurança:
  * `Strict-Transport-Security: max-age=63072000; includeSubDomains; preload`
  * `X-Content-Type-Options: nosniff`
  * `X-Frame-Options: DENY`
  * `Content-Security-Policy: default-src 'self'; script-src 'self'; ...`
* [ ] **Rotinas de Backup ACID:** Configurar rotina automatizada diária de backup com verificação de restore do PostgreSQL (`pg_dump` com compressão e criptografia para bucket privado).

### 6.2. Build dos Contêineres de Produção (Docker Multi-stage)

#### Container Backend (Spring Boot)
O Dockerfile multi-stage compila a aplicação com Maven e copia apenas o JAR executável para uma imagem mínima baseada em Eclipse Temurin JRE 25:
```bash
docker build -t seller-intelligence-backend:1.0.0 -f backend/Dockerfile .
```

#### Container Frontend (Next.js Standalone)
Compila os assets estáticos e empacota o runtime Node.js standalone otimizado:
```bash
docker build -t seller-intelligence-frontend:1.0.0 -f frontend/Dockerfile .
```

### 6.3. Publicação da Extensão na Chrome Web Store

1. **Empacotar a Extensão:**
   Gere o arquivo ZIP da versão de distribuição:
   ```bash
   cd extension/dist
   zip -r ../seller-intelligence-extension-v1.0.0.zip .
   ```
2. **Acessar o Chrome Web Store Developer Dashboard:**
   Acesse [https://chrome.google.com/webstore/devconsole](https://chrome.google.com/webstore/devconsole) com uma conta autorizada de desenvolvedor Google.
3. **Criar Novo Item:**
   Clique em **Adicionar novo item** e faça o upload do arquivo `seller-intelligence-extension-v1.0.0.zip`.
4. **Declaração de Práticas de Privacidade (Justificativas de Permissões):**
   * *Permissão `activeTab`:* "Utilizada sob demanda estrita quando o vendedor aciona o popup da extensão na aba ativa de um marketplace para consultar estimativas fiscais e de margem do produto em exibição. Nenhum histórico de navegação ou dado de abas não ativas é coletado."
   * *Permissão `storage`:* "Utilizada unicamente para armazenar localmente as preferências de tema e a URL do endpoint de backend autorizado pelo usuário."
5. **Submissão:**
   Envie para revisão da equipe de moderação de extensões da Google.

---

## 7. Suporte e Resolução de Problemas Comuns (Troubleshooting)

### Erro: Conexão recusada no banco PostgreSQL na porta 5432
* **Causa:** O contêiner `seller_postgres` não subiu ou a porta está em conflito com uma instância local existente do PostgreSQL.
* **Solução:**
  1. Verifique contêineres ativos: `docker compose ps`.
  2. Caso a porta 5432 esteja ocupada por serviço local, altere o mapeamento de porta externa no `docker-compose.yml` para `5433:5432` e atualize a variável `SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/seller_db`.

### Erro: Incidentes de compliance bloqueiam requisições de marketplace (`FEATURE_SUSPENDED_FAIL_CLOSED`)
* **Causa:** O monitoramento contínuo identificou discrepância de políticas ou falha de homologação de termos.
* **Solução:**
  1. Acesse o módulo **Compliance & Políticas** no sistema.
  2. Verifique o incidente na aba **Incidentes & Políticas**.
  3. Após revisar a causa e rodar a suíte de testes de regressão, submeta o protocolo formal de desbloqueio na aba **Reativação Segura**.
