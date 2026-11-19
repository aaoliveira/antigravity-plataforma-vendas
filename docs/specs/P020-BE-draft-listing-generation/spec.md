# Especificação Técnica: P020-BE - Geração Assistida de Título e Descrição como Rascunho com Restrição de Claims

## 1. Contexto & Objetivos

Esta especificação define o backend do **P020-BE**, implementando o motor de **Geração Assistida de Anúncios (AI Listing Studio - Modo Rascunho)** da **Seller Intelligence Platform**.
O objetivo é capacitar os vendedores a criar títulos e descrições de alta conversão para marketplaces a partir de atributos confirmados do produto, categoria e palavras-chave fornecidas, aplicando guardrails severos contra alucinações e claims infundadas.

### Princípios Inegociáveis & Guardrails:
1. **Atributos Confirmados Exclusivos:**
   - O conteúdo deve ser gerado estritamente com base nos atributos confirmados do produto (`brand`, `category`, `model`, `materials`, `dimensions`, `warranty`, `technicalSpecifications`, `confirmedBenefits`) e palavras-chave expressamente fornecidas pelo usuário.
   - Rastreabilidade total: todo fato presente no título e na descrição é mapeado para um atributo confirmado (`traceableAttributes`).
2. **Restrição Severa de Claims (Anti-Hallucination Guardrail):**
   - **É terminantemente proibido inventar:**
     * Certificações (ex.: Inmetro, Anvisa, Anatel, ISO, CE, FDA) que não constem nos atributos confirmados.
     * Compatibilidades técnicas com marcas/modelos terceiros que não tenham sido informadas.
     * Materiais ou composições nobres (ouro, titânio, couro legítimo, aço cirúrgico, 100% algodão) não confirmados.
     * Dimensões, capacidades ou pesos não comprovados.
     * Prazos ou condições de garantia abusivos ou não confirmados (ex.: "garantia vitalícia", prazos maiores que o legal/confirmado).
     * Benefícios milagrosos, alegações médicas ou superlativos enganosos ("o melhor do mundo", "cura", "infalível", "100% à prova de falhas").
   - O serviço `ListingClaimGuardrailService` analisa o texto gerado e rejeita/sinaliza claims não comprovadas.
3. **Seleção de Tom (Tone of Voice):**
   - Suporte aos tons de redação autorizados:
     * `INFORMATIVE_OBJECTIVE`: Tom padrão, claro, conciso, focado em benefícios reais e especificações diretas.
     * `TECHNICAL`: Focado em terminologia técnica, especificações de engenharia, medidas e normas confirmadas.
     * `COMMERCIAL_POLITE`: Persuasivo moderado, cortês, elegante e comercial, sem exageros ou superlativos proibidos.
4. **Limites Oficiais de Caracteres por Marketplace (Validados):**
   - Os limites de caracteres são aplicados com base nas regras oficiais vigentes e homologadas dos canais:
     * **Mercado Livre (`MERCADO_LIVRE`):**
       - Título: máx. **60 caracteres** (regra estrita da API de Itens do Mercado Livre).
       - Descrição: máx. **25.000 caracteres**, formato estritamente **texto plano** (sem tags HTML).
     * **Shopee (`SHOPEE`):**
       - Título: mín. **20 caracteres**, máx. **120 caracteres** (regra da Shopee Open Platform OpenAPI v2 `item_name`).
       - Descrição: mín. **20 caracteres**, máx. **3.000 caracteres** (regra da Shopee Open Platform OpenAPI v2 `description`), formato texto plano.
   - Se for solicitado marketplace sem regra oficial homologada na plataforma, o motor falha closed com `UNVALIDATED_MARKETPLACE_RULE`.
5. **Persistência de Versões, Prompt e Modelo:**
   - Todo rascunho possui histórico de versões (`DraftListingVersion`), gravando:
     * `version_number` incremental.
     * `prompt_template_id` e `prompt_template_version`.
     * `ai_model_name` e `ai_provider`.
     * `input_attributes_json` (atributos originais confirmados).
     * `generated_title` e `generated_description`.
     * `reviewed_title` e `reviewed_description` (quando editados pelo usuário).
     * `traceable_attributes_json` e `unproven_claims_json`.
     * Contagem de caracteres do título e descrição.
     * Flags de validação de limite de caracteres e intervenção humana (`user_edited`).
6. **Conteúdo é Rascunho & Zero Publicação Externa (Strict MVP Constraint):**
   - O conteúdo é criado com status `DRAFT` e pode transitar para `REVIEWED`, `APPROVED` ou `REJECTED`.
   - **NENHUMA publicação externa:** A plataforma não implementa chamadas a endpoints de escrita de anúncios no Mercado Livre ou Shopee.
   - Contrato de API expõe `publicationStatus = "NOT_PUBLISHED_DRAFT_ONLY"` e `externalPublishBlocked = true`.
7. **Isolamento Multi-Tenant em Profundidade com PostgreSQL 17 RLS:**
   - Tabelas `draft_listings` e `draft_listing_versions` protegidas por Row-Level Security mandatório (`FORCE ROW LEVEL SECURITY`).

---

## 2. Critérios de Aceite (Definition of Acceptance)

1. **Saída Estruturada:**
   - Endpoint retorna DTO estruturado contendo ID do rascunho, versão, título gerado/revisado, descrição gerada/revisada, contagem de caracteres, conformidade com os limites do canal, tom selecionado, modelo e template utilizados.
2. **Atributos Rastreáveis:**
   - Cada claim ou dado técnico presente no título e na descrição é mapeado para um atributo confirmado do produto ou palavra-chave fornecida.
3. **Limites Validados:**
   - Validação estrita de limites oficiais (ex.: Mercado Livre título $\le 60$ caracteres; Shopee título entre 20 e 120 caracteres). Qualquer valor fora dos limites oficiais gera erro descritivo.
4. **Conteúdo é Rascunho com Edição e Aprovação:**
   - O vendedor pode editar título e descrição gerando nova versão ou atualizando rascunho (`REVIEWED`), e aprovar o rascunho (`APPROVED`) para uso local.
5. **Nenhuma Publicação Externa no MVP:**
   - Nenhuma chamada para mutação externa de catálogo/anúncios é permitida. A API bloqueia e declara explicitamente a restrição de publicação externa.

---

## 3. Modelo de Dados & Migrações Flyway

### 3.1 `V17__draft_listing_content.sql`
- Criação de `draft_listings`:
  - `id` (UUID PRIMARY KEY)
  - `tenant_id` (UUID NOT NULL)
  - `product_id` (UUID NULL)
  - `product_variant_id` (UUID NULL)
  - `target_marketplace` (VARCHAR(50) NOT NULL)
  - `current_version_number` (INT NOT NULL DEFAULT 1)
  - `status` (VARCHAR(50) NOT NULL DEFAULT 'DRAFT')
  - `created_at` (TIMESTAMP WITH TIME ZONE NOT NULL)
  - `updated_at` (TIMESTAMP WITH TIME ZONE NOT NULL)
- Criação de `draft_listing_versions`:
  - `id` (UUID PRIMARY KEY)
  - `tenant_id` (UUID NOT NULL)
  - `draft_listing_id` (UUID NOT NULL REFERENCES draft_listings(id) ON DELETE CASCADE)
  - `version_number` (INT NOT NULL)
  - `prompt_template_id` (VARCHAR(100) NOT NULL)
  - `prompt_template_version` (VARCHAR(50) NOT NULL)
  - `ai_model_name` (VARCHAR(100) NOT NULL)
  - `ai_provider` (VARCHAR(50) NOT NULL)
  - `tone` (VARCHAR(50) NOT NULL)
  - `target_marketplace` (VARCHAR(50) NOT NULL)
  - `input_attributes_json` (TEXT NOT NULL)
  - `generated_title` (VARCHAR(255) NOT NULL)
  - `generated_description` (TEXT NOT NULL)
  - `reviewed_title` (VARCHAR(255) NULL)
  - `reviewed_description` (TEXT NULL)
  - `traceable_attributes_json` (TEXT NOT NULL)
  - `unproven_claims_json` (TEXT NULL)
  - `title_char_count` (INT NOT NULL)
  - `description_char_count` (INT NOT NULL)
  - `character_limit_valid` (BOOLEAN NOT NULL)
  - `user_edited` (BOOLEAN NOT NULL DEFAULT FALSE)
  - `review_notes` (TEXT NULL)
  - `created_at` (TIMESTAMP WITH TIME ZONE NOT NULL)
- Índices e restrições:
  - `CONSTRAINT uq_draft_versions_ver UNIQUE (tenant_id, draft_listing_id, version_number)`
  - Índices por `tenant_id`, `product_id` e `draft_listing_id`.

### 3.2 `V17_1__postgresql_rls_draft_listing_content.sql`
- `ALTER TABLE draft_listings ENABLE ROW LEVEL SECURITY;`
- `ALTER TABLE draft_listings FORCE ROW LEVEL SECURITY;`
- `ALTER TABLE draft_listing_versions ENABLE ROW LEVEL SECURITY;`
- `ALTER TABLE draft_listing_versions FORCE ROW LEVEL SECURITY;`
- Criação de policies `draft_listings_tenant_isolation_policy` e `draft_listing_versions_tenant_isolation_policy`.

---

## 4. Endpoints REST da API

- `POST /api/v1/ai/drafts/generate`: Gera ou refaz um rascunho de anúncio.
- `GET /api/v1/ai/drafts`: Lista rascunhos do tenant com paginação e filtros.
- `GET /api/v1/ai/drafts/{id}`: Detalhe do rascunho com versão ativa e histórico.
- `GET /api/v1/ai/drafts/{id}/versions/{versionNumber}`: Detalhe de uma versão específica.
- `PUT /api/v1/ai/drafts/{id}/edit`: Edição de título/descrição pelo usuário (marca `user_edited = true`, status `REVIEWED`).
- `POST /api/v1/ai/drafts/{id}/approve`: Aprovação formal do rascunho pelo lojista (status `APPROVED`).
- `POST /api/v1/ai/drafts/{id}/reject`: Rejeição do rascunho com notas (status `REJECTED`).
- `GET /api/v1/ai/drafts/rules`: Consulta limites oficiais homologados por marketplace.
