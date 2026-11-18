# Especificação Técnica: P010-BE — Pesquisa de Mercado e Anúncios Autorizada, Registro de Fontes, Cache e Respeito a Limites

> **Identificador da Feature:** `P010-BE` &middot; **Versão:** 1.0.0  
> **Status da Spec:** Aprovada para Implementação  
> **Requisitos do PRD:** RF-005, RF-006, RF-009, RF-010, RF-034, RNF-001, RNF-002, RNF-006, RNF-007, RNF-014, ADR-002, ADR-004  
> **Módulo:** `com.sellerintelligence.modules.competitorresearch`

---

## 1. Contexto e Objetivos

O módulo de Pesquisa de Mercado e Concorrência (`competitorresearch`) governa a busca e descoberta de produtos e anúncios em canais de marketplace, assegurando conformidade irrestrita com os Termos de Serviço dos canais, proteção das contas dos sellers e conformidade com a LGPD e políticas de dados de terceiros.

Esta entrega implementa:
1. **Registro Central de Políticas de Fontes (`MarketSearchSourcePolicy` & `SearchSourcePolicyRegistry`):**
   - Registro formal e imutável de todas as fontes de pesquisa autorizadas (Mercado Livre, Shopee, Amazon, Catálogo Próprio, Provedor Licenciado).
   - Metadados mandatórios por fonte: identificador da política, canal (`marketplace`), capacidade associada (`MarketplaceCapability`), status da capacidade (`CapabilityStatus`), requisito de autorização, padrão de endpoint, URL oficial de documentação, rate limit por minuto, permissão de busca concorrencial (`allowsCompetitorSearch`) e regras contratuais.
2. **Política Rigorosa Fail-Closed e Anti-Scraping:**
   - **Proibição Absoluta de Scraping:** Jamais contornar endpoints com scraping não autorizado, headless browsers ocultos ou injeção em clientes não oficiais.
   - **Fail-Closed em Fontes `UNVERIFIED` ou `RESTRICTED`:** Se a pesquisa concorrencial não estiver homologada (`UNVERIFIED` como na Amazon) ou for restrita a dados de loja (`RESTRICTED` como na Shopee), o sistema **não chama nenhum endpoint não aprovado**. Retorna explicitamente o `capabilityStatus` e mensagem educativa com aviso legal, oferecendo como alternativa segura os dados do catálogo próprio (`OWN_CATALOG`) ou fonte licenciada (`LICENSED_DATA`).
3. **Garantia de Não-Fabricação de Dados (Anti-Alucinação):**
   - Vendas de concorrentes, faturamento estimado, CTR ou visitas **NUNCA são gerados artificialmente ou alucinados**. Se a API oficial não fornecer esses dados, o campo permanece nulo/ausente com sinalização explícita (`salesMetricNotice`).
4. **Normalização Padronizada e Rastreabilidade Completa:**
   - Cada item retornado (`NormalizedMarketSearchResult`) contém identificador original, título higienizado, marketplace, preço monetário com precisão decimal, vendedor/classificação, link oficial sanitizado (sem tokens nem parâmetros de rastreamento de afiliados), GTIN/EAN, condição, proveniência (`OFFICIAL`, `OBSERVED`, `LICENSED`), timestamp de coleta e ID da política de autorização (`sourcePolicyId`).
5. **Cache com Validade (TTL) e Proteção contra Rate Limiting:**
   - Cache com TTL configurável (padrão 15 minutos) por tenant e hash determinístico dos parâmetros de busca (`market_search_cache`).
   - Respeito integral aos limites de taxa da fonte e proteção contra exaustão de quota upstream.
6. **Histórico e Trilha de Auditoria por Tenant:**
   - Tabela `market_search_queries` para registrar cada pesquisa efetuada, tempo de execução, status de capacidade e política utilizada.
7. **Isolamento Multi-Tenant em Profundidade:**
   - Tabelas de busca (`market_search_queries` e `market_search_cache`) isoladas com `tenant_id NOT NULL` e PostgreSQL Row-Level Security forçado (`FORCE ROW LEVEL SECURITY`).

---

## 2. Cenários BDD / Gherkin

### Cenário 1: Pesquisa em Fonte com Capacidade Autorizada (Mercado Livre)
```gherkin
Dado que o tenant autenticado executa uma pesquisa de mercado com o termo "monitor 27" no Mercado Livre
E a política registrada para Mercado Livre possui status SUPPORTED e permite busca pública
Quando a busca for processada
Então os resultados retornados devem ser normalizados com rastreabilidade completa
E cada item deve conter sourcePolicyId "POL-MLB-PUBLIC-01" e proveniência identificada
E métricas de vendas concorrenciais não disponíveis oficialmente devem retornar nulas com capability notice
E a consulta deve ser registrada no histórico de auditoria do tenant
```

### Cenário 2: Bloqueio Fail-Closed para Canal sem Capacidade Homologada (Amazon UNVERIFIED)
```gherkin
Dado que o usuário solicita pesquisa concorrencial na Amazon SP-API
E a política registrada para Amazon possui status UNVERIFIED por falta de homologação
Quando o serviço de pesquisa for acionado
Então nenhuma requisição externa deve ser disparada
E a resposta deve retornar capabilityStatus = UNVERIFIED
E conter aviso formal explicando a indisponibilidade da fonte documental
E zero dados fictícios ou fabricados devem ser retornados
```

### Cenário 3: Proteção Anti-Scraping e Fallback de Shopee RESTRICTED para Catálogo Próprio
```gherkin
Dado que o usuário solicita pesquisa concorrencial no canal Shopee
E a Shopee possui status RESTRICTED para busca aberta sem aprovação comercial de parceiro
Quando a busca for executada com fallback habilitado
Então o sistema não deve executar web scraping
E deve retornar capabilityStatus = RESTRICTED informando a política do canal
E deve disponibilizar os resultados correspondentes do próprio catálogo do tenant (OWN_CATALOG)
E cada resultado deve indicar proveniência "OFFICIAL" e política "POL-OWN-CATALOG-01"
```

### Cenário 4: Cache com TTL Evita Chamadas Upstream Duplicadas
```gherkin
Dado que uma pesquisa idêntica foi executada pelo tenant há 2 minutos
E o cache possui validade de 15 minutos
Quando a mesma busca for requisitada novamente
Então a resposta deve ser servida diretamente do cache
E o campo cached deve ser true
E nenhuma chamada upstream ou consumo de quota de rate limit deve ocorrer
```

### Cenário 5: Respeito Rigoroso ao Limite de Taxa (Rate Limiting)
```gherkin
Dado que o tenant atingiu o limite de taxa de 60 requisições por minuto para a fonte de pesquisa
Quando uma 61ª requisição for submetida dentro da mesma janela temporal
Então o sistema deve rejeitar a chamada com erro de RateLimitExceededException (HTTP 429)
E informar o tempo restante para expiração da janela (Retry-After)
```

---

## 3. Matriz de Fontes e Políticas Registradas

| Política ID | Canal / Fonte | Capacidade | Status | Permite Concorrência? | Rate Limit / min | Padrão de Endpoint / Ref |
|---|---|---|---|---|---|---|
| `POL-MLB-PUBLIC-01` | `MERCADO_LIVRE` | `MARKET_SEARCH` | `SUPPORTED` | **SIM** (Público) | 60 | `/sites/MLB/search` |
| `POL-SHOPEE-RESTRICTED-01` | `SHOPEE` | `MARKET_SEARCH` | `RESTRICTED` | **NÃO** (Sem Scraping) | 10 | `/api/v2/product/search_item` |
| `POL-AMAZON-UNVERIFIED-01` | `AMAZON` | `MARKET_SEARCH` | `UNVERIFIED` | **NÃO** (Não Homologado) | 0 | `SP-API /catalog/2022-04-01/items` |
| `POL-OWN-CATALOG-01` | `INTERNAL` | `READ_OWN_CATALOG` | `SUPPORTED` | **NÃO** (Próprio) | 300 | `internal://catalog-service` |
| `POL-LICENSED-PROVIDER-01` | `LICENSED_PROVIDER` | `MARKET_SEARCH` | `SUPPORTED` | **SIM** (B2B Licenciado) | 120 | `https://api.licensed-provider.internal/v1/search` |

---

## 4. Arquitetura e Contratos

### DTOs de Entrada e Saída
- `MarketSearchRequest`:
  - `query`: string
  - `marketplace`: enum (`MERCADO_LIVRE`, `SHOPEE`, `AMAZON`, `ALL`)
  - `category`: string
  - `minPrice`: BigDecimal
  - `maxPrice`: BigDecimal
  - `onlyOwnCatalog`: boolean
  - `page`: int
  - `size`: int
- `NormalizedMarketSearchResult`:
  - `itemId`: string
  - `title`: string
  - `marketplace`: string
  - `price`: BigDecimal
  - `currency`: string
  - `sellerName`: string
  - `sellerClassification`: string
  - `listingUrl`: string
  - `gtin`: string
  - `itemCondition`: string
  - `availableQuantity`: Integer
  - `sourcePolicyId`: string
  - `provenanceType`: string (`OFFICIAL`, `OBSERVED`, `LICENSED`)
  - `collectedAt`: Instant
  - `salesMetricNotice`: string
- `MarketSearchResponse`:
  - `items`: List<NormalizedMarketSearchResult>
  - `totalElements`: long
  - `totalPages`: int
  - `currentPage`: int
  - `pageSize`: int
  - `sourceUsed`: string
  - `sourcePolicyId`: string
  - `capabilityStatus`: CapabilityStatus
  - `cached`: boolean
  - `capabilityNotice`: string
  - `rateLimitRemaining`: int

---

## 5. Estratégia de Testes

1. `SearchSourcePolicyRegistryTest`: Verifica validação de políticas, recuperação por canal, proibição de scraping e integridade dos limites.
2. `MarketProductSearchServiceTest`: Verifica fluxo completo, aplicação de cache, detecção de rate limit, resposta de capabilityStatus para canais UNVERIFIED/RESTRICTED, fallback limpo para OWN_CATALOG e ausência estrita de dados fabricados.
3. `MarketSearchControllerTest`: Testes MockMvc validando autorização JWT, tenant isolation, paginação, filtros e serialização JSON.
4. `PostgresMarketSearchRlsIntegrationTest`: Teste de integração PostgreSQL 17 com Testcontainers validando isolamento entre Tenant A e Tenant B nas tabelas `market_search_queries` e `market_search_cache`.
5. `ModularArchitectureTest`: Verificação ArchUnit garantindo ausência de ciclos e aderência às regras do modulith.
