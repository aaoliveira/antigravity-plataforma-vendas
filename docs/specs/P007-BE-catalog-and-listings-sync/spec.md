# Especificação Técnica: P007-BE — Catálogo Canônico, Anúncios Próprios e Sincronização Resiliente

> **Identificador da Feature:** `P007-BE` &middot; **Versão:** 1.0.0  
> **Status da Spec:** Aprovada para Implementação  
> **Requisitos do PRD:** RF-005, RF-006, RF-007, RF-008, RNF-001, RNF-002, RNF-003, RNF-006, ADR-002, ADR-004  
> **Módulo:** `com.sellerintelligence.modules.catalog`

---

## 1. Contexto e Objetivos

O módulo de catálogo e sincronização de anúncios é a base analítica e operacional da **Seller Intelligence Platform**.  
Esta entrega implementa as entidades centrais do domínio e o motor de ingestão e reconciliação idempotente de anúncios próprios:

1. **Separação Rigorosa entre Produto Canônico e Anúncio (Listing):**
   - **`Product`**: Entidade mestra canônica interna do tenant, representando o item de estoque/catálogo do seller (SKU mestre, marca, categoria).
   - **`ProductVariant`**: Variante canônica do produto (tamanho, voltagem, cor, GTIN/EAN).
   - **`MarketplaceListing`**: Anúncio concreto em um canal/marketplace (ex.: Mercado Livre, Shopee).
   - **`SellerAccount`**: Conta do vendedor homologada no marketplace para o tenant.
   - **`ExternalIdentifierMapping`**: Tabela de resolução de identificadores externos (Item ID, Variation ID, SKU externo, GTIN) para entidades canônicas internas.
   - **Regra Arquitetural Inegociável:** Um produto canônico pode possuir múltiplos anúncios (ex.: anúncio Clássico e anúncio Premium no Mercado Livre, além de anúncio na Shopee). A plataforma **NUNCA** realiza deduplicação ou fusão de anúncios baseando-se apenas em similaridade de títulos. A vinculação requer SKU, GTIN ou identificador mapeado explícito.
2. **Preservação de Payload Original Minimizado e Permitido:**
   - O payload original fornecido pela API oficial é armazenado de forma minimizada, sem dados pessoais (PII), senhas ou tokens.
   - Schema versionado (`schema_version = 'v1'`), rastreabilidade de origem (`origin = 'MERCADO_LIVRE_ITEMS_API'` / `'SHOPEE_OPENAPI_V2'`) e timestamps de ingestão (`synced_at`).
3. **Sincronização Resiliente de Anúncios Próprios:**
   - **Paginação:** Consumo estruturado em páginas com limites controlados (`limit`/`offset` ou `page_size`).
   - **Checkpoint (`ListingSyncCheckpoint`):** Armazena o estado e o cursor da sincronização por tenant e conta. Em caso de falha transitória (timeout, rate limit), o processo pode ser retomado exatamente a partir do último checkpoint sem reiniciar do zero.
   - **Idempotência:** A reexecução da mesma página ou conjunto de anúncios é idempotente via chave única `(tenant_id, marketplace, external_listing_id)`. Reprocessar não duplica registros nem corrompe métricas.
   - **Reconciliação:** Atualiza preço, estoque e status de listings existentes; cria novos quando inéditos.
4. **Isolamento Multi-Tenant Estrito:**
   - Todas as tabelas contêm `tenant_id NOT NULL REFERENCES organizations(id) ON DELETE CASCADE`.
   - Políticas PostgreSQL Row-Level Security (`ENABLE` e `FORCE ROW LEVEL SECURITY`) ativas em todas as novas tabelas.
   - Testes com PostgreSQL real (Testcontainers) comprovam a impossibilidade de vazamento cross-tenant.
5. **Zero Consulta a Concorrentes:**
   - O motor opera estritamente sob a capacidade `READ_OWN_CATALOG`. Consultas a lojistas concorrentes ou scraping são terminantemente vedadas nesta feature.

---

## 2. Cenários BDD / Gherkin

### Cenário 1: Produto Canônico e Listings permanecem distintos (Relação 1:N)
```gherkin
Dado que um tenant possui um produto canônico "Teclado Mecânico RGB" com SKU "KB-RGB-01"
Quando forem sincronizados dois anúncios do Mercado Livre com o mesmo título ("Teclado Mecânico RGB") mas tipos diferentes (Clássico e Premium)
Então o sistema deve criar dois registros distintos de MarketplaceListing
E ambos os listings podem estar associados ao mesmo produto canônico via SKU ou mapeamento
E os listings não devem ser fundidos nem deduplicados apenas pelo título
```

### Cenário 2: Idempotência na Ingestão (Reprocessar a mesma página não duplica)
```gherkin
Dado que uma página de 20 anúncios de uma conta da Shopee foi sincronizada com sucesso
Quando a mesma página com os mesmos 20 anúncios for reprocessada
Então nenhum novo registro de MarketplaceListing deve ser inserido no banco de dados
E o total de listings deve permanecer exatamente 20
E os campos de preço, estoque e synced_at devem ser atualizados conforme reconciliação
```

### Cenário 3: Retomada a partir de Checkpoint após Falha Parcial
```gherkin
Dado que a sincronização de um catálogo de 60 itens falha no offset 40 (página 3)
Quando o serviço de sincronização for reiniciado para aquela SellerAccount
Então ele deve consultar o ListingSyncCheckpoint com status FAILED ou IN_PROGRESS
E deve retomar o processamento a partir do offset 40
E ao finalizar todos os 60 itens, o status do checkpoint deve transitar para COMPLETED
```

### Cenário 4: Preservação de Payload Minimizado com Schema Versionado e Origem
```gherkin
Dado que um anúncio é ingerido via adapter do Mercado Livre
Quando o registro for salvo em marketplace_listings
Então a coluna raw_payload_json deve conter o JSON minimizado sem tokens ou credenciais
E a coluna schema_version deve registrar "v1"
E a coluna origin deve registrar "MERCADO_LIVRE_ITEMS_API"
E synced_at deve registrar o timestamp UTC da operação
```

### Cenário 5: Isolamento Multi-Tenant em Profundidade
```gherkin
Dado que o Tenant A sincroniza seus anúncios próprios
E o Tenant B consulta os produtos e anúncios cadastrados
Então o Tenant B não deve visualizar nenhum listing ou produto pertencente ao Tenant A
E qualquer tentativa de inserir ou atualizar anúncio do Tenant A usando o contexto do Tenant B deve ser rejeitada
```
