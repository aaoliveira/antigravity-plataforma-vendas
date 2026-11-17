# Matriz de Capacidades e Viabilidade de Integrações com Marketplaces

**Versão:** 1.0  
**Data da Consulta Oficial:** 17 de novembro de 2026  
**Status:** Baseline Técnico Vinculante para a Fase 0 e MVP  
**Autor:** Antigravity AI Assistant & Engenharia

---

## 1. Visão Geral e Fontes Consultadas

Este documento estabelece as capacidades técnicas, limites, escopos, modelos de autenticação e viabilidade de dados para os marketplaces integrados à **Seller Intelligence Platform**. 
Em conformidade com a hierarquia de evidências ([AGENTS.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/AGENTS.md)) e com as regras anti-alucinação ([.agents/rules/anti-hallucination.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/.agents/rules/anti-hallucination.md)), **nenhum endpoint, métrica ou permissão foi inferido ou presumido**. Todas as informações derivam da documentação técnica oficial vigente.

### Fontes Oficiais Auditadas
1. **Mercado Livre Developers:**
   - Portal: [https://developers.mercadolivre.com.br](https://developers.mercadolivre.com.br) / [https://developers.mercadolibre.com](https://developers.mercadolibre.com)
   - Documentação de Autenticação (OAuth 2.0): `https://developers.mercadolivre.com.br/pt_br/autenticacao-e-autorizacao`
   - Documentação de Pedidos: `https://developers.mercadolivre.com.br/pt_br/gestao-de-pedidos`
   - Documentação de Anúncios e Catálogo: `https://developers.mercadolivre.com.br/pt_br/publicacao-de-produtos`
   - Documentação de Mercado Envios (Logística): `https://developers.mercadolivre.com.br/pt_br/envios`
   - Documentação de Mercado Ads (Product Ads): `https://developers.mercadolibre.com/en_us/product-ads`
   - Data de Consulta: 17 de novembro de 2026.
2. **Shopee Open Platform (API v2):**
   - Portal: [https://open.shopee.com](https://open.shopee.com) / [https://openplatform.shopee.com.br](https://openplatform.shopee.com.br)
   - Documentação de Autenticação e Assinatura: `https://open.shopee.com/documents/v2/v2.auth.token.get`
   - Documentação de Pedidos e Escrow Financeiro: `https://open.shopee.com/documents/v2/v2.payment.get_escrow_detail`
   - Documentação de Catálogo e Produtos: `https://open.shopee.com/documents/v2/v2.product.get_item_base_info`
   - Documentação de Shopee Ads: `https://open.shopee.com/documents/v2/v2.marketing.ads`
   - Data de Consulta: 17 de novembro de 2026.
3. **Google Chrome Web Store:**
   - Developer Program Policies (Limited Use Policy): `https://developer.chrome.com/docs/webstore/program-policies/`
   - Data de Consulta: 17 de novembro de 2026.

---

## 2. Mercado Livre Developers (Brasil - MLB)

### 2.1 Modelo de Autenticação e Autorização (OAuth 2.0)
- **Fluxo:** OAuth 2.0 Authorization Code Grant.
- **URL de Autorização (Browser):**
  ```http
  https://auth.mercadolivre.com.br/authorization?response_type=code&client_id={APP_ID}&redirect_uri={REDIRECT_URI}
  ```
- **Peculiaridade Crítica de Escopo:** O Mercado Livre **não utiliza escopos granulares** (como `read:orders` ou `read:items`) na URL de autorização. O consentimento do seller é concedido globalmente para a aplicação registrada, e o nível de acesso decorre dos tópicos e capacidades aprovados no painel *My Apps* do Mercado Livre Developers.
- **Troca de Código por Tokens:**
  - **Endpoint:** `POST https://api.mercadolibre.com/oauth/token`
  - **Payload:** `grant_type=authorization_code&client_id={ID}&client_secret={SECRET}&code={CODE}&redirect_uri={URI}`
  - **Retorno:** `access_token` (validade de **6 horas**), `refresh_token` (validade de **6 meses**), `user_id` (seller ID).
- **Renovação de Token (Refresh):**
  - **Endpoint:** `POST https://api.mercadolibre.com/oauth/token`
  - **Payload:** `grant_type=refresh_token&client_id={ID}&client_secret={SECRET}&refresh_token={REFRESH_TOKEN}`

### 2.2 Endpoints Oficiais e Capacidades Confirmadas

| Domínio | Endpoint | Método | Descrição e Campos Confirmados | Status no MVP |
|---|---|---|---|---|
| **Usuário** | `/users/me` | `GET` | Identificador do seller (`id`), apelido (`nickname`), pontuação e reputação. | `CONFIRMADO` (P0) |
| **Catálogo Próprio** | `/users/{user_id}/items/search` | `GET` | Lista IDs de anúncios do seller autenticado com paginação (`scroll_id` ou `offset/limit`). | `CONFIRMADO` (P0) |
| **Item Próprio** | `/items/{item_id}` ou `/items?ids={id1,id2}` | `GET` | Título, preço, moeda (`BRL`), quantidade disponível, fotos, atributos técnicos, categoria, status (`active`, `paused`). Suporta multiget de até 20 itens. | `CONFIRMADO` (P0) |
| **Pesquisa Pública (Mercado)** | `/sites/MLB/search?q={query}` | `GET` | Busca pública por palavra-chave: itens retornados trazem título, preço, seller nickname, condições, reputação e filtros de categoria. Não requer token privado. | `CONFIRMADO` (P0) |
| **Tendências Públicas** | `/trends/MLB/{category_id}` | `GET` | Retorna termos mais buscados na categoria. Dado puramente observacional/agregado. | `CONFIRMADO` (P1) |
| **Pedidos Próprios** | `/orders/search?seller={user_id}` | `GET` | Busca pedidos por período (`order.date_created.from/to`) e status (`paid`, `cancelled`). | `CONFIRMADO` (P0) |
| **Detalhes do Pedido** | `/orders/{order_id}` | `GET` | Itens (`order_items`), quantidades, preços unitários, comissão base do canal (`sale_fee`), total bruto e vínculo com envio (`shipping.id`). | `CONFIRMADO` (P0) |
| **Custos de Envio** | `/shipments/{shipment_id}/costs` | `GET` | Custos reais de frete suportados pelo seller vs frete pago pelo comprador (essencial para o motor financeiro). | `CONFIRMADO` (P0) |
| **Webhooks** | `/applications/{app_id}/notifications` | `POST` | Configurado no console do app. Tópicos suportados: `orders_v2`, `items`, `shipments`. | `CONFIRMADO` (P0) |
| **Publicação / Escrita** | `/items` | `POST` | Criação de anúncio. Exige validação estrita de categorias e atributos obrigatórios. | `CONFIRMADO` (Fase 3 / OQ-010) |
| **Mercado Ads** | `/advertising/advertisers/{advertiser_id}/product_ads/campaigns/search` | `GET` | Campanhas de Product Ads, orçamento, status. **Requer escopo especial aprovado** (`advertising/product_ads`). | `REQUER_APROVACAO` (Fase 2) |
| **Métricas de Ads** | `/advertising/advertisers/{advertiser_id}/product_ads/items` | `GET` | Impressões, cliques, investimento (CPC), vendas diretas/indiretas, receita e ACOS por anúncio. | `REQUER_APROVACAO` (Fase 2) |

### 2.3 Limites de Taxa e Restrições Operacionais (Mercado Livre)
- **Rate Limit Padrão:** O Mercado Livre impõe limitação dinâmica por aplicação e token. Respostas com código HTTP `429 Too Many Requests` incluem os cabeçalhos:
  - `X-Rate-Limit-Limit`: Limite máximo de requisições na janela.
  - `X-Rate-Limit-Remaining`: Requisições restantes.
  - `X-Rate-Limit-Reset`: Segundos até a renovação do teto.
- **Política de Retentativa:** Utilizar backoff exponencial com jitter e fila de espera no RabbitMQ.
- **Ambiente de Teste:** O Mercado Livre disponibiliza a criação de usuários de teste (`test_user: true`) via `/users/test_user`. Contas de produção não operam em ambiente sandbox e vice-versa.

---

## 3. Shopee Open Platform (Brasil - BR)

### 3.1 Modelo de Autenticação e Assinatura Criptográfica (v2 API)
- **Base URL Produção:** `https://partner.shopeemobile.com` ou `https://openplatform.shopee.com.br`.
- **Base URL Sandbox:** `https://partner.test-stable.shopeemobile.com`.
- **Fluxo OAuth:** Authorization Code Grant com vínculo obrigatório de `partner_id` e `shop_id`.
- **URL de Consentimento do Seller:**
  ```http
  https://partner.shopeemobile.com/api/v2/shop/auth_partner?partner_id={PARTNER_ID}&timestamp={TIMESTAMP}&sign={SIGN}&redirect={REDIRECT_URI}
  ```
- **Regra de Assinatura Criptográfica Obrigatória (`sign`):**
  Toda requisição deve conter assinatura gerada via **HMAC-SHA256** com a `partner_key` da aplicação.
  - *Para APIs Públicas/Autenticação:*
    $$\text{Base String} = \text{partner\_id} + \text{path} + \text{timestamp}$$
  - *Para APIs de Loja (com Shop ID):*
    $$\text{Base String} = \text{partner\_id} + \text{path} + \text{timestamp} + \text{access\_token} + \text{shop\_id}$$
  - **Validade do Timestamp:** A assinatura expira rigorosamente em **5 minutos**.
- **Troca de Código por Tokens:**
  - **Endpoint:** `POST /api/v2/auth/token/get`
  - **Retorno:** `access_token` (validade de **4 horas**), `refresh_token` (validade de **30 dias**), `shop_id`.
- **Renovação de Token:** `POST /api/v2/auth/access_token/get` com `refresh_token`.

### 3.2 Endpoints Oficiais e Capacidades Confirmadas

| Domínio | Endpoint | Método | Descrição e Campos Confirmados | Status no MVP |
|---|---|---|---|---|
| **Perfil da Loja** | `/api/v2/shop/get_shop_info` | `GET` | Nome da loja, país (`BR`), status, data de autorização. | `CONFIRMADO` (P0) |
| **Lista de Anúncios** | `/api/v2/product/get_item_list` | `GET` | IDs dos anúncios da loja com status (`NORMAL`, `UNLIST`, `BANNED`). | `CONFIRMADO` (P0) |
| **Detalhes do Produto** | `/api/v2/product/get_item_base_info` | `GET` | Busca até 50 itens por chamada: nome, descrição, categoria, marca, imagens, peso, dimensões. | `CONFIRMADO` (P0) |
| **Variações / SKU** | `/api/v2/product/get_model_list` | `GET` | Modelos/variações do produto, SKU, preço e estoque por variação. | `CONFIRMADO` (P0) |
| **Lista de Pedidos** | `/api/v2/order/get_order_list` | `GET` | Lista `order_sn` por intervalo de data (`create_time`, `update_time`) e status (`UNPAID`, `READY_TO_SHIP`, `COMPLETED`, `CANCELLED`). | `CONFIRMADO` (P0) |
| **Detalhes do Pedido** | `/api/v2/order/get_order_detail` | `GET` | Detalhes de até 50 pedidos: itens, preços unitários, descontos aplicados pelo seller vs Shopee, dados de entrega. | `CONFIRMADO` (P0) |
| **Extrato e Repasses (Escrow)** | `/api/v2/payment/get_escrow_detail` | `GET` | **Endpoint crítico para o motor financeiro e conciliação:** discrimina comissão da Shopee, taxa de serviço, taxa de transação, frete pago pelo comprador, desconto do seller e valor líquido repassado (`order_income`). | `CONFIRMADO` (P0/P1) |
| **Notificações Push** | Shopee Push Mechanism | `POST` | Configurado no Open Platform Console. Eventos: atualização de pedido, atualização de item. | `CONFIRMADO` (P0) |
| **Publicação de Item** | `/api/v2/product/add_item` | `POST` | Cadastro de novos anúncios na Shopee. | `CONFIRMADO` (Fase 3 / OQ-010) |
| **Shopee Ads API** | `/api/v2/marketing/...` | `GET` | Métricas de anúncios patrocinados da Shopee. **Requer aprovação avançada e parceria direta de BD com a Shopee.** | `REQUER_APROVACAO` (Fase 2) |

### 3.3 Limites de Taxa e Restrições Operacionais (Shopee)
- **Rate Limit Padrão:** 10 requisições por segundo (QPS) por loja autorizada. Parceiros certificados de alto volume podem solicitar até 50 QPS.
- **Tratamento de Erros:** Erros de taxa retornam `error: "error_limit"`. Exige backoff com controle de concorrência por loja.
- **Ambiente de Teste:** Testes utilizam o ambiente `test-stable` com app de testes e lojas de teste criadas no console da Shopee.

---

## 4. Classificação Rigorosa da Proveniência dos Dados

Para garantir fidelidade com o PRD e com as regras anti-alucinação, os dados utilizados pela plataforma são classificados estruturalmente em 5 categorias:

| Categoria | Descrição | Fontes Válidas no Produto | Tratamento no Sistema |
|---|---|---|---|
| **1. Dados Próprios do Seller** | Informações transacionais e operacionais privadas das lojas do próprio usuário. | APIs oficiais após OAuth (ML `/orders`, `/items`; Shopee `/order`, `/payment/get_escrow_detail`) ou arquivos enviados pelo próprio seller (CSV/XLSX). | Rótulo `IMPORTED`. Isolamento total por `tenant_id`. Fonte da verdade financeira. |
| **2. Dados Públicos Observáveis** | Informações disponíveis a qualquer visitante na web sem quebra de controle técnico. | Endpoints oficiais de busca pública (ML `/sites/MLB/search`, `/trends`), extensões em contexto com autorização do usuário ou catálogos abertos. | Rótulo `OBSERVED`. Segregados estruturalmente dos dados de seller. |
| **3. Dados Licenciados** | Bases de inteligência de mercado contratadas comercialmente de terceiros oficiais. | Provedores licenciados de dados de mercado (a integrar na Fase 2). | Rótulo `IMPORTED` (com indicação da fonte terceira licenciada). |
| **4. Estimativas Heurísticas** | Cálculos e proxies inferidos a partir de dados observáveis parciais. | Algoritmos estatísticos internos (ex.: concentração de categoria, score de qualidade do anúncio, projeção de demanda). | Rótulo `ESTIMATED`. **Obrigatório exibir metodologia resumida, intervalo de confiança e ressalvas**. |
| **5. Dados Indisponíveis** | Dados que a API oficial não fornece e que não possuem fonte legítima. | Ex.: Vendas reais exatas de concorrente, estoque real de concorrente, faturamento privado de loja concorrente, taxa de conversão privada de terceiro. | Rótulo `UNAVAILABLE`. **Estritamente proibido converter em zero ou gerar valores sintéticos**. Exibir como "Dado Indisponível pelo Marketplace". |

---

## 5. Política para a Extensão de Navegador (Chrome Manifest V3)

A extensão Chrome auxiliar está sujeita aos requisitos de conformidade com a **Chrome Web Store Limited Use Policy** e aos termos de serviço dos marketplaces:

1. **Princípio do Propósito Único (*Single Purpose*):**
   - A extensão funciona exclusivamente como assistente de navegação para o vendedor logado na plataforma, permitindo consultar métricas e scores do anúncio que o próprio usuário está visualizando na aba ativa.
2. **Permissões Mínimas Obrigatórias:**
   - Usar `activeTab` disparada unicamente por ação explícita do usuário (clique no ícone da extensão ou comando contextual).
   - **Proibido** o uso de `<all_urls>` ou injeção permanente de content scripts em segundo plano em todas as páginas da internet.
3. **Isolamento de Segurança e Credenciais:**
   - A extensão **não armazena chaves de API, credenciais OAuth ou senhas**.
   - A comunicação é realizada exclusivamente contra o backend da própria Seller Intelligence Platform via token de sessão de curta duração.
   - **Proibido** ler ou capturar cookies de sessão, credenciais bancárias ou dados pessoais de navegação dos marketplaces visitados.
4. **Transparência e Consentimento:**
   - A interface da extensão deve exibir aviso explícito informando quais dados da página atual estão sendo processados e indicar claramente se as informações apresentadas são observadas ou estimadas.

---

## 6. Estratégia de Ambientes e Simulações (Dev/Test vs Produção)

Para respeitar a regra anti-alucinação de **nunca simular integração externa em produção**:

| Ambiente | Provedor de Dados Externos | Configuração e Evidências |
|---|---|---|
| **Desenvolvimento Local (`dev`)** | WireMock / Testcontainers efêmeros. | Payloads reais anonimizados armazenados como fixtures de teste; nenhum tráfego para APIs de produção. |
| **Testes Automatizados (`test`)** | Testcontainers (WireMock, PostgreSQL, RabbitMQ, Redis, Keycloak). | Execução hermética sem dependência de internet ou de contas de terceiros. |
| **Homologação (`staging`)** | Contas de Teste Oficiais (Mercado Livre `test_user` e Shopee Sandbox). | Validação do fluxo OAuth real, renovação de tokens e webhooks em ambiente controlado. |
| **Produção (`production`)** | APIs Oficiais de Produção mediante autorização OAuth concedida pelo seller. | Se o seller não tiver conectado a conta, o conector exibe estado `NOT_CONFIGURED` com passo a passo de conexão. **Proibido rodar mocks em produção**. |

---

## 7. Plano de Contingência contra Indisponibilidade de APIs e Rate Limits

1. **Importação Manual por Arquivo (CSV/XLSX) — Requisito FR-INT-008:**
   - Caso a API do marketplace esteja indisponível, revogada ou passe por instabilidade prolongada, o usuário poderá fazer o upload de extratos de pedidos, catálogos e relatórios oficiais emitidos pelo próprio marketplace.
   - O parser validará o schema do arquivo, rejeitará linhas corrompidas com relatório de erro detalhado e fará a ingestão idempotente no banco com rótulo `IMPORTED`.
2. **Resiliência e Recuperação de Falhas:**
   - Fila de dead-letter (DLQ) no RabbitMQ para capturar payloads de webhooks que falhem no processamento após 3 retentativas com backoff exponencial.
   - Circuit Breaker configurado via Resilience4j no backend Spring Boot para proteger o sistema contra exaustão de threads em momentos de degradação das APIs externas.
