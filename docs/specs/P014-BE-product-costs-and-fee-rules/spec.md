# Especificação Técnica P014-BE: Cadastro de Custos por Produto/Variante/Empresa e Regras de Taxas

## 1. Visão Geral e Objetivos

Esta especificação define a arquitetura e os contratos para o módulo de precificação e custos (**Deterministic Pricing & Cost Engine**) da Seller Intelligence Platform.

### Objetivos Principais:
1. **Cadastro de Custos Estruturado e Versionado:**
   - Registro de custos direto por produto (`productId`), variante (`variantId`) e empresa (`companyId`), com moeda (`currency`), unidade (`unit`), alíquota de impostos e despesas variáveis.
   - Preservação total de histórico: novas alterações encerram a vigência do registro anterior (`effective_to = now`) e incrementam a versão sem apagar registros pretéritos.
2. **Regras de Taxas e Comissões Parametrizáveis:**
   - Suporte a comissão percentual, tarifa fixa, tarifa logística e outras cobranças por marketplace, modalidade de anúncio, categoria, faixa de preço, vigência e versão.
   - **Regra Anti-Alucinação Estrita:** a plataforma não inventa taxas nem pressupõe comissão zero na ausência de regras; sinaliza explicitamente `hasMissingFees = true`, identifica os tipos faltantes e solicita entrada do usuário.
3. **Precisão Decimal e Arredondamento:**
   - Utilização estrita de `BigDecimal` com escala interna 4 e arredondamento `HALF_UP` documentado para cálculos monetários e percentuais.
4. **Isolamento Multi-Tenant e Auditoria:**
   - RLS mandatória no PostgreSQL 17 (`tenant_id NOT NULL`), bloqueio de vazamento cross-tenant e auditoria de cada criação e modificação via `AuditPublisher`.

---

## 2. Modelo de Dados e Banco de Dados (Flyway V12)

### 2.1 Tabela `product_costs`
```sql
CREATE TABLE product_costs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    product_id UUID NOT NULL,
    variant_id UUID,
    company_id UUID,
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    unit VARCHAR(20) NOT NULL DEFAULT 'UN',
    acquisition_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    packaging_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    additional_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    subsidized_shipping_cost NUMERIC(15, 4) NOT NULL DEFAULT 0.0000,
    tax_rate_percent NUMERIC(8, 4) NOT NULL DEFAULT 0.0000,
    other_expenses_percent NUMERIC(8, 4) NOT NULL DEFAULT 0.0000,
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE,
    version INT NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(255) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_product_costs_lookup 
    ON product_costs (tenant_id, product_id, variant_id, company_id, effective_from, effective_to);
```

### 2.2 Tabela `marketplace_fee_rules`
```sql
CREATE TABLE marketplace_fee_rules (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    marketplace VARCHAR(50) NOT NULL,
    listing_type VARCHAR(50) NOT NULL,
    category_id VARCHAR(100),
    fee_type VARCHAR(50) NOT NULL,
    fee_value_type VARCHAR(50) NOT NULL,
    percentage_rate NUMERIC(8, 4),
    fixed_amount NUMERIC(15, 4),
    currency VARCHAR(3) NOT NULL DEFAULT 'BRL',
    min_price_threshold NUMERIC(15, 4),
    max_price_threshold NUMERIC(15, 4),
    source_type VARCHAR(50) NOT NULL,
    source_reference TEXT NOT NULL,
    legal_disclaimer TEXT,
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE,
    version INT NOT NULL DEFAULT 1,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_fee_rules_lookup 
    ON marketplace_fee_rules (tenant_id, marketplace, listing_type, fee_type, effective_from, effective_to);
```

---

## 3. Regras de Negócio e Casos de Borda (Boundary Conditions)

1. **Vigência Temporal:**
   - Uma regra ou custo é vigente para uma data de referência `T` se e somente se:
     $$\text{effective\_from} \le T < \text{effective\_to} \quad \text{(ou } \text{effective\_to IS NULL)}$$
   - Fronteira `T == effective_from`: VIGENTE.
   - Fronteira `T == effective_to`: NÃO VIGENTE (fim de vigência é exclusivo).
2. **Faixas de Preço (Price Tier Thresholds):**
   - Aplica-se se:
     $$(\text{min\_price\_threshold IS NULL} \lor P \ge \text{min\_price\_threshold}) \land (\text{max\_price\_threshold IS NULL} \lor P \le \text{max\_price\_threshold})$$
   - Limites inferior e superior são inclusivos.
3. **Ausência Explícita de Taxas:**
   - Se nenhuma regra de `COMMISSION` for encontrada para a combinação `(marketplace, listingType, categoria, preço)`:
     - `hasMissingFees = true`
     - `missingFeeTypes = ['COMMISSION']`
     - `resolutionStatus = MISSING` (ou `PARTIAL` se houver outra taxa como taxa fixa)
     - Mensagem explícita solicitando input do usuário.
4. **Precisão e Arredondamento:**
   - Cálculos em Java usando `BigDecimal` com `RoundingMode.HALF_UP`.
   - Exemplo: Comissão percentual = `price.multiply(percentageRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)`.
