# Especificação Oficial do Conector Amazon SP-API (Selling Partner API)

## 1. Visão Geral e Arquitetura

A **Amazon Selling Partner API (SP-API)** é a API oficial e mandatória da Amazon para vendedores do Seller Central. No âmbito da **Seller Intelligence Platform**, a integração atende aos requisitos de expansão de canais sem alterar os contratos internos e invariantes do sistema.

### 1.1 Identificação Regional e Endpoints
- **Região Primária:** Brasil (BR / North America Selling Region).
- **Marketplace ID Brasil:** `A2Q3Y263D00KWC`.
- **Endpoint de Produção SP-API:** `https://sellingpartnerapi-na.amazon.com`.
- **Endpoint de Sandbox SP-API:** `https://sandbox.sellingpartnerapi-na.amazon.com`.
- **Endpoint de Autenticação LWA (Login with Amazon):** `https://api.amazon.com/auth/o2/token`.
- **URL de Consentimento OAuth (Seller Central Brasil):** `https://sellercentral.amazon.com.br/apps/authorize/consent`.

---

## 2. Matriz de Capacidades Oficiais (Amazon SP-API)

Em estrita consonância com a regra anti-alucinação e o PRD, a tabela abaixo define os recursos suportados e explicitamente não suportados:

| Capacidade | Status | API Oficial da Amazon | Descrição Técnica & Limitações |
|---|---|---|---|
| **CATALOG** | `SUPPORTED` | Listings Items API (`/listings/2021-08-01/items/{sellerId}/{sku}`) & Catalog Items API | Leitura de ofertas do seller, preço atual, quantidade disponível, ASIN, SKU e status do anúncio. |
| **LISTINGS** | `SUPPORTED` | Listings Items API (`PUT /listings/2021-08-01/items/{sellerId}/{sku}`) | Atualização e sincronização de atributos de oferta via JSON Schema por Product Type. |
| **ORDERS** | `SUPPORTED` | Orders API v0 (`/orders/v0/orders`) | Consulta de pedidos emitidos, status (`Unshipped`, `Shipped`), data da compra, total monetário e itens do pedido. |
| **ADS** | `UNSUPPORTED` | Amazon Advertising API | A API de publicidade da Amazon é apartada da SP-API, exigindo convite de desenvolvedor e credenciais dedicadas. Marcado como `UNSUPPORTED` no conector base. |
| **WEBHOOKS** | `SUPPORTED` | Notifications API (`/notifications/v1/subscriptions`) | Notificações baseadas em eventos via Amazon EventBridge (ex: `ORDER_CHANGE`, `LISTINGS_ITEM_STATUS_CHANGE`). |
| **PUBLIC_SEARCH** | `UNSUPPORTED` | Inexistente na SP-API aberta | A Amazon não oferece endpoint público não autenticado de busca concorrencial. É estritamente proibido realizar scraping não autorizado. |
| **PUBLIC_TRENDS** | `UNSUPPORTED` | Inexistente na SP-API aberta | Sem endpoint público de tendências de mercado. Marcado como `UNSUPPORTED`. |

---

## 3. Mapeamento de Campos Suportados vs. Não Suportados

### 3.1 Mapeamento de Anúncios (`ConnectorListing`)

| Campo Interno | Campo Amazon SP-API | Suportado? | Tratamento / Observações |
|---|---|---|---|
| `id` | `sku` / `asin` | Sim | Identificador primário da oferta (SKU do seller). |
| `title` | `attributes.item_name[0].value` | Sim | Título oficial do catálogo. |
| `price` | `attributes.purchasable_offer[0].our_price[0].schedule[0].value_with_tax` | Sim | Preço em `BigDecimal` monetário exato. |
| `currency` | `attributes.purchasable_offer[0].currency` | Sim | Moeda oficial (`BRL`). |
| `availableQuantity` | `fulfillmentAvailability[0].quantity` | Sim | Quantidade de estoque vendável. |
| `permalink` | `https://www.amazon.com.br/dp/{asin}` | Sim | Link público montado com ASIN oficial. |
| `status` | `status` (`BUYABLE`, `DISCOVERABLE`) | Sim | Convertido para `ACTIVE` ou `INACTIVE`. |
| `category` | `productType` | Sim | Tipo de produto Amazon (ex: `ELECTRONIC_CABLE`). |
| *Métricas privadas concorrentes* | N/A | **Não** | Não suportado. Proibido inventar ou estimar sem fonte. |

### 3.2 Mapeamento de Pedidos (`ConnectorOrder`)

| Campo Interno | Campo Amazon SP-API | Suportado? | Tratamento / Observações |
|---|---|---|---|
| `orderId` | `AmazonOrderId` | Sim | Identificador do pedido (ex: `701-1234567-1234567`). |
| `date` | `PurchaseDate` | Sim | Timestamp UTC da compra (`Instant`). |
| `status` | `OrderStatus` | Sim | `Unshipped`, `PartiallyShipped`, `Shipped`, `Canceled`. |
| `totalAmount` | `OrderTotal.Amount` | Sim | Valor total pago em `BigDecimal`. |
| `items.orderItemId` | `OrderItemId` | Sim | ID do item no pedido Amazon. |
| `items.externalListingId`| `SellerSKU` / `ASIN` | Sim | Identificador da oferta vinculada. |
| `items.title` | `Title` | Sim | Nome do produto faturado. |
| `items.unitPrice` | `ItemPrice.Amount / QuantityOrdered` | Sim | Preço unitário determinístico. |
| `items.quantity` | `QuantityOrdered` | Sim | Quantidade faturada. |
| *PII de Comprador (CPF/CNPJ)* | Restricted Data Token (RDT) | **Restrito** | Exige RDT e auditoria DPP (Data Protection Policy). Sem RDT, o campo é preenchido como nulo (`UNAVAILABLE`), nunca com dados falsos. |

---

## 4. Política de Rate Limiting e Resiliência

A Amazon SP-API aplica o algoritmo *Token Bucket* para cada operação:
- **Orders API:** Rate: 0.5 req/s, Burst: 30 requisições.
- **Listings API:** Rate: 5 req/s, Burst: 10 requisições.
- **Headers Oficiais:** `x-amzn-RateLimit-Limit` retorna a taxa atribuída.
- **Tratamento de 429 (Too Many Requests):** O conector intercepta `429`, executa retries com backoff exponencial e jitter, ou lança `BusinessException` com `RATE_LIMIT_EXCEEDED` sem quebrar o tenant.

---

## 5. Segurança, Criptografia e Isolamento Multi-Tenant

1. **Credenciais Segregadas:** As credenciais (`access_token`, `refresh_token`, `client_secret`) são encriptadas em repouso com algoritmo AES-256-GCM via `TokenCryptoService`.
2. **Isolamento de Loja:** Cada credencial é vinculada estritamente à `Store` e `tenant_id` da organização, impedindo vazamento cross-tenant.
3. **Health Check Determinístico:** O método `validateConnection` invoca `/sellers/v1/marketplaceParticipations`. Se retornar `200 OK` e o marketplace `A2Q3Y263D00KWC` estiver ativo, a conexão é marcada como `CONNECTED`.
