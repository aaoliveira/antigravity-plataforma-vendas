# Registro e Inventário Oficial de Políticas de Marketplaces (Marketplace Policy Registry)

## 1. Visão Geral
Este documento constitui o catálogo oficial de políticas, termos de uso para desenvolvedores, diretrizes de APIs e normas de segurança monitoradas de forma contínua pelo **Continuous Marketplace Compliance Engine** da **Seller Intelligence Platform**.

Todas as fontes cadastradas foram verificadas em conformidade com as diretrizes de segurança (anti-SSRF, domínios oficiais autorizados e ausência de scraping de páginas autenticadas).

---

## 2. Catálogo Consolidado de Políticas Monitoradas

| ID | Marketplace | Nome da Política / Documento | Tipo de Política | URL Oficial Autorizada | Frequência | Hash SHA-256 Inicial | Status |
|---|---|---|---|---|---|---|---|
| **POL-MELI-01** | MERCADO_LIVRE | Termos e Condições para Desenvolvedores | `TERMS_OF_SERVICE` | `https://developers.mercadolivre.com.br/pt_br/termos-e-condicoes` | Diária | `e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855` | ACTIVE |
| **POL-MELI-02** | MERCADO_LIVRE | Diretrizes de Rate Limit e Quotas de API | `RATE_LIMIT` | `https://developers.mercadolivre.com.br/pt_br/limites-e-regras` | Diária | `a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e` | ACTIVE |
| **POL-MELI-03** | MERCADO_LIVRE | Política de Segurança e Privacidade de Dados | `PRIVACY_DATA_PROTECTION` | `https://www.mercadolivre.com.br/privacidade` | Semanal | `1c8b2739556847e32025dc4ed5bf30f905103ea81a3e51f0b0ce5243510c04a2` | ACTIVE |
| **POL-SHOP-01** | SHOPEE | Open Platform Developer Terms of Service | `TERMS_OF_SERVICE` | `https://open.shopee.com/documents?module=63&type=2` | Diária | `4355a46b19d348dc2f57c046f8ef63d4538ebb936000f3c9ee954a27460dd865` | ACTIVE |
| **POL-SHOP-02** | SHOPEE | API Call Frequency & Throttling Rules | `RATE_LIMIT` | `https://open.shopee.com/developer-guide/rate-limit` | Diária | `53c234e5e8472b6ac51c1ae1cab3fe06fad053beb8ebfd8977b010655bfdd3c3` | ACTIVE |
| **POL-SHOP-03** | SHOPEE | Seller Data Protection & Privacy Policy | `PRIVACY_DATA_PROTECTION` | `https://open.shopee.com.br/documents?module=95&type=2` | Semanal | `6ca13d52ca70c883e0f0bb101e425a89e8624de51db2d2392593af6a84118090` | ACTIVE |
| **POL-AMZN-01** | AMAZON | Selling Partner API Developer Agreement | `DEVELOPER_AGREEMENT` | `https://developer-docs.amazon.com/sp-api/docs/sp-api-developer-agreement` | Diária | `8f434346648f6b96df89dda901c5176b10a6d83961dd3c1ac88b59b2dc327aa4` | ACTIVE |
| **POL-AMZN-02** | AMAZON | SP-API Acceptable Use Policy (AUP) | `API_USAGE` | `https://developer-docs.amazon.com/sp-api/docs/acceptable-use-policy` | Diária | `eccbc87e4b5ce2fe28308fd9f2a7baf3a879f426f9ad8015c906360e954f2a47` | ACTIVE |
| **POL-AMZN-03** | AMAZON | Data Protection Policy (DPP) | `PRIVACY_DATA_PROTECTION` | `https://developer-docs.amazon.com/sp-api/docs/data-protection-policy` | Semanal | `c81e728d9d4c2f636f067f89cc14862c1f300daee3a4f304f1054d48a5825b11` | ACTIVE |
| **POL-TIKT-01** | TIKTOK_SHOP | Partner Platform Service Agreement | `DEVELOPER_AGREEMENT` | `https://partner.tiktokshop.com/doc/page/terms-of-service` | Diária | `c4ca4238a0b923820dcc509a6f75849b` | ACTIVE |
| **POL-TIKT-02** | TIKTOK_SHOP | OpenAPI Usage Guidelines & Rate Limits | `RATE_LIMIT` | `https://partner.tiktokshop.com/doc/page/rate-limits` | Diária | `e4da3b7fbbce2345d7772b0674a318d5` | ACTIVE |
| **POL-MAGA-01** | MAGALU | Termos de Integração e APIs de Parceiros | `TERMS_OF_SERVICE` | `https://developers.magalu.com/termos-de-uso` | Diária | `1679091c5a880faf6fb5e6087eb1b2dc` | ACTIVE |
| **POL-MAGA-02** | MAGALU | Regras de Chamadas e Concorrência de API | `RATE_LIMIT` | `https://developers.magalu.com/boas-praticas` | Diária | `8f14e45fceea167a5a36dedd4bea2543` | ACTIVE |
| **POL-SHEI-01** | SHEIN | Open Platform Integration Terms | `TERMS_OF_SERVICE` | `https://open.shein.com/documents/terms` | Diária | `c9f0f895fb98ab9159f51fd0297e236d` | ACTIVE |
| **POL-SHEI-02** | SHEIN | API Rate Limiting & Safety Guidelines | `RATE_LIMIT` | `https://open.shein.com/documents/rate-limits` | Diária | `45c48cce2e2d7fbdea1afc51c7c6ad26` | ACTIVE |

---

## 3. Classificação Funcional e Impacto Mapeado

Cada política cadastrada é mapeada diretamente para as funcionalidades da plataforma que dependem de sua vigência:

1. **`POL-MELI-01` & `POL-MELI-02`:** Impactam `CATALOG_UPDATE`, `PRICE_UPDATE`, `ORDER_SYNC` e `MESSAGING_REPLY`.
2. **`POL-SHOP-01` & `POL-SHOP-02`:** Impactam `CATALOG_UPDATE`, `INVENTORY_SYNC`, `ORDER_SYNC`.
3. **`POL-AMZN-01` & `POL-AMZN-02`:** Impactam `LISTING_MANAGEMENT`, `REPORT_GENERATION`, `ORDER_SYNC`.
4. **`POL-TIKT-01` & `POL-TIKT-02`:** Impactam `VIDEO_COMMERCE_SYNC`, `ORDER_SYNC`, `PROMOTION_MANAGEMENT`.
5. **`POL-MAGA-01` & `POL-MAGA-02`:** Impactam `PRODUCT_SYNC`, `ORDER_SYNC`, `PRICE_UPDATE`.
6. **`POL-SHEI-01` & `POL-SHEI-02`:** Impactam `CATALOG_SYNC`, `INVENTORY_UPDATE`, `ORDER_SYNC`.

---

## 4. Procedimento para Inclusão de Nova Política
Para registrar um novo documento de política ou atualizar uma URL monitorada:
1. O domínio deve constar na allowlist de segurança em `UrlSecurityValidator.ALLOWED_POLICY_DOMAINS`.
2. A URL deve ser acessível via HTTPS público sem exigência de credencial interativa.
3. O registro é executado via `MarketplacePolicyRegistryService.registerPolicy(...)`.
4. É calculado o hash inicial SHA-256 e gerado um log de auditoria no sistema.
