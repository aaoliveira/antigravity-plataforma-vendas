# Especificação Técnica: P009-BE — Modelo Comum para Observações de Mercado, Proveniência, Deduplicação e Matching Explicável

> **Identificador da Feature:** `P009-BE` &middot; **Versão:** 1.0.0  
> **Status da Spec:** Aprovada para Implementação  
> **Requisitos do PRD:** RF-009, RF-010, RF-011, RF-034, RNF-001, RNF-002, RNF-006, RNF-007, RNF-014, ADR-002, ADR-004  
> **Módulo:** `com.sellerintelligence.modules.marketintelligence`

---

## 1. Contexto e Objetivos

O módulo de Inteligência de Mercado (`marketintelligence`) é o componente responsável pela ingestão, normalização, proveniência e correspondência de dados de produtos e anúncios públicos e oficiais observados nos marketplaces (Mercado Livre, Shopee, Amazon).

Esta entrega implementa:
1. **Modelo Comum para Observações de Mercado (`MarketObservation`):**
   - Registro unificado de observações de preços, condições, vendedor legitimamente disponível, canal/marketplace, código de barras (GTIN/EAN), marca e dados descritivos.
   - Metadados de proveniência mandatórios em todo registro: `source_type` (`OFFICIAL`, `OBSERVED`, `ESTIMATED`, `USER_INPUT`), `source_url` (validada contra domínios autorizados), `collected_at`, `observed_at`, `valid_until`, `method` e `confidence` (0.0000 a 1.0000).
2. **Defesas Anti-Alucinação Rígidas:**
   - **Proibição de Métricas Fantasmas:** Vendas estimadas, contagem de visitas, CTR ou faturamento jamais podem ser criados sem fonte comprovada e metodologia documentada e versionada.
   - **Requisitos de Estimativas:** Quando `source_type == ESTIMATED`, os campos `methodology_description`, `model_version` e `time_window` tornam-se de preenchimento estritamente obrigatório.
3. **Higienização e Minimização de Payload Sensível:**
   - O payload original da fonte é minimizado e sanitizado: remoção de dados pessoais (LGPD), tokens, credenciais, segredos e query parameters de rastreamento antes da gravação no banco (`minimized_payload`).
4. **Deduplicação Inteligente de Observações:**
   - Unicidade por anúncio e canal dentro do período de vigência: observações repetidas para o mesmo anúncio mantêm histórico temporal sem duplicatas vazias de banco de dados.
5. **Algoritmo de Matching com Score Explicável:**
   - Correspondência entre uma observação de mercado e o catálogo de produtos canônicos (`Product`) do tenant.
   - Cálculo ponderado multi-fator:
     - **GTIN/Barcode Match:** Se coincidente, eleva o score com alta confiabilidade determinística.
     - **Title Similarity:** Normalização de títulos (remoção de acentos, pontuação, stopwords) e cálculo de similaridade por tokens (Jaccard).
     - **Brand Match:** Correspondência exata/normalizada de marca.
     - **Price Plausibility:** Verificação de consistência na ordem de grandeza de preços.
   - **Explicabilidade Estruturada:** Cada match fornece um texto claro e detalhado dos pesos e fatores que compuseram a nota (ex: "Match 88%: GTIN idêntico (+45%), 80% similaridade de título (+30%), marca 'Samsung' coincidente (+13%)").
   - **Limiar de Aceitação Rígido (Threshold):** Itens com score abaixo do limiar (padrão 0.75) **NUNCA são vinculados automaticamente** (`MATCHED`). Ficam categorizados como `POTENTIAL_MATCH` (para revisão humana) ou `NO_MATCH`.
6. **Isolamento Multi-Tenant em Profundidade:**
   - Tabelas `market_observations` e `product_matches` com `tenant_id NOT NULL` e políticas ativas e forçadas de PostgreSQL Row-Level Security (`ENABLE` e `FORCE ROW LEVEL SECURITY`).
7. **Exposição de API REST:**
   - Endpoints para criação, consulta paginada com filtros e execução de matching explicável.

---

## 2. Cenários BDD / Gherkin

### Cenário 1: Ingestão de Observação com Proveniência Completa
```gherkin
Dado que um conector obtém dados públicos de um anúncio do Mercado Livre
Quando a observação for persistida com source_type "OBSERVED", URL oficial, preço, timestamps e método
Então a observação deve ser salva com confidence = 1.0000
E todos os timestamps (collected_at, observed_at, valid_until) devem ser registrados
E o payload original deve ter dados sensíveis removidos
```

### Cenário 2: Rejeição Anti-Alucinação de Vendas/Faturamento sem Metodologia
```gherkin
Dado que uma requisição tenta cadastrar uma observação com vendas estimadas de 1500 unidades e receita de R$ 50.000,00
Mas não informa metodologia_description nem model_version
Quando o serviço validar a observação
Então a operação deve ser rejeitada com BusinessValidationException
E nenhuma observação deve ser persistida
```

### Cenário 3: Exigência de Metodologia em Registros ESTIMATED
```gherkin
Dado que uma observação é enviada com source_type "ESTIMATED"
Quando o campo methodology_description ou model_version estiver ausente
Então a validação deve falhar informando a obrigatoriedade da metodologia
E quando todos os metadados metodológicos e janela temporal forem fornecidos
Então o registro ESTIMATED deve ser aceito com a confiança especificada
```

### Cenário 4: Matching com Score Explicável acima do Limiar (Auto-Match)
```gherkin
Dado que o tenant possui um produto canônico "Smartphone Samsung Galaxy A54 128GB Preto" com GTIN "7891234567890" e marca "Samsung"
E uma observação de mercado possui GTIN "7891234567890", título "Samsung Galaxy A54 128GB 5G Preto" e marca "Samsung"
Quando o serviço de matching avaliar o candidato
Então o score calculado deve superar o limiar de 0.75 (ex: >= 0.85)
E o status do match deve ser "MATCHED"
E a justificativa explicável deve detalhar o match de GTIN, marca e termos do título
```

### Cenário 5: Matching abaixo do Limiar (Sem Vínculo Automático)
```gherkin
Dado que uma observação de mercado possui título "Capa Protetora Silicone para Galaxy A54" sem GTIN
E o produto canônico é o aparelho "Smartphone Samsung Galaxy A54 128GB"
Quando o serviço de matching calcular o score
Então o score global deve ficar abaixo de 0.75 (ex: ~0.55)
E o status deve ser "POTENTIAL_MATCH" ou "NO_MATCH"
E o produto NUNCA deve ser vinculado automaticamente
```

### Cenário 6: Isolamento Multi-Tenant Estrito
```gherkin
Dado que o Tenant Alpha registrou observações e matches de mercado
Quando o Tenant Beta consultar observações ou matches via API ou banco com RLS ativado
Então nenhuma observação ou correspondência do Tenant Alpha deve ser retornada
```

---

## 3. Modelo de Dados

### 3.1 Tabela `market_observations`
- `id` UUID PRIMARY KEY
- `tenant_id` UUID NOT NULL REFERENCES organizations(id)
- `observed_listing_id` VARCHAR(100) NOT NULL
- `marketplace` VARCHAR(50) NOT NULL
- `title` VARCHAR(255) NOT NULL
- `price` NUMERIC(15, 4) NOT NULL
- `currency` VARCHAR(10) NOT NULL DEFAULT 'BRL'
- `item_condition` VARCHAR(50) NOT NULL
- `seller_identifier` VARCHAR(255)
- `source_type` VARCHAR(50) NOT NULL (`OFFICIAL`, `OBSERVED`, `ESTIMATED`, `USER_INPUT`)
- `source_url` VARCHAR(1000) NOT NULL
- `collected_at` TIMESTAMP WITH TIME ZONE NOT NULL
- `observed_at` TIMESTAMP WITH TIME ZONE NOT NULL
- `valid_until` TIMESTAMP WITH TIME ZONE NOT NULL
- `method` VARCHAR(100) NOT NULL
- `confidence` NUMERIC(5, 4) NOT NULL DEFAULT 1.0000
- `methodology_description` TEXT
- `model_version` VARCHAR(50)
- `time_window` VARCHAR(50)
- `gtin` VARCHAR(50)
- `brand` VARCHAR(100)
- `sales_volume` NUMERIC(15, 4)
- `visits_count` BIGINT
- `ctr` NUMERIC(8, 4)
- `revenue` NUMERIC(15, 4)
- `minimized_payload` TEXT
- `created_at` TIMESTAMP WITH TIME ZONE NOT NULL
- `updated_at` TIMESTAMP WITH TIME ZONE NOT NULL

### 3.2 Tabela `product_matches`
- `id` UUID PRIMARY KEY
- `tenant_id` UUID NOT NULL REFERENCES organizations(id)
- `canonical_product_id` UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE
- `observation_id` UUID NOT NULL REFERENCES market_observations(id) ON DELETE CASCADE
- `overall_score` NUMERIC(5, 4) NOT NULL
- `gtin_score` NUMERIC(5, 4)
- `title_score` NUMERIC(5, 4) NOT NULL
- `brand_score` NUMERIC(5, 4)
- `price_score` NUMERIC(5, 4)
- `match_status` VARCHAR(50) NOT NULL (`MATCHED`, `POTENTIAL_MATCH`, `NO_MATCH`)
- `explanation` TEXT NOT NULL
- `threshold_applied` NUMERIC(5, 4) NOT NULL
- `matched_at` TIMESTAMP WITH TIME ZONE NOT NULL
- `created_at` TIMESTAMP WITH TIME ZONE NOT NULL
