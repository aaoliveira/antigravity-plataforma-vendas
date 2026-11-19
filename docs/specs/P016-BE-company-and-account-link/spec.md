# Especificação Técnica: P016-BE - Entidades Company/BusinessUnit com CNPJ Sintático e Vínculo de Contas de Marketplace

## 1. Visão Geral do Módulo
O módulo `com.sellerintelligence.modules.company` implementa o cadastro e a gestão estrutural de empresas / pessoas jurídicas (`Company` / `BusinessUnit`) e o vínculo financeiro explícito com contas conectadas de marketplaces (`SellerAccount`).

---

## 2. Regras Invioláveis de Negócio e Anti-Alucinação
1. **Validação Sintática Estrita de CNPJ sem Inferência Fiscal**:
   - O CNPJ informado deve possuir 14 dígitos e satisfazer a validação dos dois dígitos verificadores via algoritmo módulo 11.
   - Sequências repetitivas inválidas (`00000000000000`, `11111111111111`...) são sumariamente rejeitadas.
   - **Regra Anti-Alucinação**: A validação é exclusivamente sintática e matemática. A aplicação NÃO consulta a Receita Federal, NÃO valida situação cadastral externa e NÃO assume ou inventa regime tributário (Simples Nacional, Lucro Presumido, Real) ou alíquotas fiscais padrão.
2. **Vínculo Unívoco e Bloqueio de Ambiguidade**:
   - Em um dado momento, uma conta conectada de marketplace (`seller_account_id`) só pode estar ativamente vinculada a uma única empresa (`is_active = true` e `effective_to IS NULL`).
   - Tentativa de vincular uma conta já atrelada sem encerramento ou transferência explícita é bloqueada como conflito (HTTP 409).
3. **Preservação Histórica e Versionamento de Vínculos**:
   - Quando uma conta é desvinculada ou transferida, o registro anterior tem seu encerramento registrado com `effective_to = now()` e `is_active = false`.
   - Registros pretéritos nunca são deletados, mantendo a auditabilidade para reconciliação contábil de pedidos passados.
4. **Isolamento Multi-Tenant em Profundidade**:
   - Todas as tabelas possuem `tenant_id` obrigatório e políticas ativas e forçadas de Row-Level Security (RLS) no PostgreSQL 17.
   - Tentativas de relacionar contas ou empresas de outro tenant são terminantemente bloqueadas.
5. **Não-Escopo Estrito**:
   - Nenhuma gestão de usuários, papéis, equipes ou permissões corporativas.

---

## 3. Modelo de Dados e Banco de Dados (Flyway V13)
### Tabela `companies`
- `id`: UUID PRIMARY KEY
- `tenant_id`: UUID NOT NULL
- `corporate_name`: VARCHAR(255) NOT NULL
- `trade_name`: VARCHAR(255)
- `cnpj`: VARCHAR(14) NOT NULL
- `state_registration`: VARCHAR(50)
- `tax_regime`: VARCHAR(50)
- `is_active`: BOOLEAN NOT NULL DEFAULT TRUE
- `notes`: TEXT
- `created_at`: TIMESTAMPTZ NOT NULL
- `updated_at`: TIMESTAMPTZ NOT NULL
- Constraints: `UNIQUE (tenant_id, cnpj)`

### Tabela `company_seller_account_links`
- `id`: UUID PRIMARY KEY
- `tenant_id`: UUID NOT NULL
- `company_id`: UUID NOT NULL REFERENCES companies(id)
- `seller_account_id`: UUID NOT NULL REFERENCES seller_accounts(id)
- `effective_from`: TIMESTAMPTZ NOT NULL
- `effective_to`: TIMESTAMPTZ
- `is_active`: BOOLEAN NOT NULL DEFAULT TRUE
- `version`: INT NOT NULL DEFAULT 1
- `notes`: TEXT
- `linked_by`: VARCHAR(255) NOT NULL
- `created_at`: TIMESTAMPTZ NOT NULL
- `updated_at`: TIMESTAMPTZ NOT NULL

---

## 4. Endpoints REST
- `POST /api/v1/companies`: Criação de nova empresa com CNPJ validado.
- `GET /api/v1/companies`: Listagem paginada/filtrada de empresas do tenant.
- `GET /api/v1/companies/{id}`: Detalhes da empresa.
- `PUT /api/v1/companies/{id}`: Atualização de dados cadastrais.
- `DELETE /api/v1/companies/{id}`: Desativação lógica da empresa.
- `POST /api/v1/companies/{id}/links`: Vínculo de conta de marketplace à empresa.
- `DELETE /api/v1/companies/links/{linkId}`: Encerramento do vínculo de conta.
- `GET /api/v1/companies/{id}/accounts`: Listagem de contas ativas vinculadas à empresa.
- `GET /api/v1/companies/accounts/{sellerAccountId}/history`: Histórico completo de vínculos da conta.
