# Especificação Técnica P017-FE: Painel Financeiro, Filtros Combináveis, Detalhamento de Componentes e Alertas de Incompletude

> **Versão:** 1.0.0  
> **Status:** APPROVED  
> **Data:** 19/11/2026  
> **Autor:** Tech Lead / Frontend Staff Engineer  
> **Módulo:** `frontend/src/components/financial/FinancialFactsDashboardView.tsx`  
> **Prompt:** P017-FE

---

## 1. Visão Geral e Objetivos de Negócio

O objetivo do **P017-FE** é fornecer a interface visual do **Painel Financeiro** da Seller Intelligence Platform. O painel conecta-se aos endpoints do backend de fatos financeiros (`/api/v1/financial-facts`), exibindo:
1. **Cards de KPIs Agregados:** Receita Bruta, Receita Líquida, Deduções de Canal, Custos Diretos/CMV, Impostos Informados, Margem de Contribuição (R$ e %) e Alerta de Incompletude.
2. **Definições Acessíveis dos Indicadores (Anti-Alucinação):** Cada indicador possui botão/ícone de ajuda acessível com explicação formal, reforçando mandatariamente que Margem de Contribuição **NÃO é Lucro Líquido**.
3. **Filtros Combináveis:** Período com presets temporais, Empresa, Canal/Marketplace, Conta conectada, SKU e Status de Reconciliação.
4. **Tabela de Pedidos com Totais Reconciliáveis:** Listagem de pedidos com visualização clara de receitas, deduções, margem, badges de proveniência e reconciliação externa.
5. **Gaveta de Detalhamento de Componentes (`FinancialFactDetailDrawer`):** Detalhamento analítico do pedido e de cada item, DRE simplificada, rateio de frete e tarifas, modal de conciliação de liquidação externa e auditoria JSON.
6. **Alertas de Incompletude:** Sinalização explícita de custos ausentes (`PRODUCT_COST_MISSING`) e regras de taxas não cadastradas (`MARKETPLACE_FEE_RULE_MISSING`) com links para cadastramento imediato.

---

## 2. Arquitetura da Interface e Componentes

### 2.1 Estrutura de Arquivos
- **Tipos TypeScript:**
  - `frontend/src/types/financial.ts`: Tipos das entidades, comandos e resumos retornados pelo BFF/backend.
- **Next.js BFF Routes:**
  - `frontend/src/app/api/financial-facts/route.ts`: Proxy para `GET /api/v1/financial-facts` (listagem com filtros).
  - `frontend/src/app/api/financial-facts/summary/route.ts`: Proxy para `GET /api/v1/financial-facts/summary`.
  - `frontend/src/app/api/financial-facts/orders/[orderId]/route.ts`: Proxy para `GET /api/v1/financial-facts/orders/{orderId}`.
  - `frontend/src/app/api/financial-facts/orders/[orderId]/compute/route.ts`: Proxy para `POST /api/v1/financial-facts/orders/{orderId}/compute`.
  - `frontend/src/app/api/financial-facts/orders/[orderId]/reconcile/route.ts`: Proxy para `POST /api/v1/financial-facts/orders/{orderId}/reconcile`.
- **Componentes React:**
  - `frontend/src/components/financial/FinancialKpiCards.tsx`: Exibição de métricas consolidadas com tooltips explicativos acessíveis.
  - `frontend/src/components/financial/FinancialFiltersBar.tsx`: Barra de filtros combináveis com presets de período, selects e busca por SKU.
  - `frontend/src/components/financial/FinancialFactsTable.tsx`: Tabela responsiva de pedidos com status, margens e badges.
  - `frontend/src/components/financial/FinancialFactDetailDrawer.tsx`: Gaveta lateral com DRE por pedido, tabela granular de itens, rateio e auditoria.
  - `frontend/src/components/financial/ReconcileSettlementModal.tsx`: Modal para registrar repasse externo e conferir divergências centesimais.
  - `frontend/src/components/financial/FinancialFactsDashboardView.tsx`: Tela principal unificadora integrada ao `AppShell.tsx`.

---

## 3. Regra Anti-Alucinação Estrita

> [!CAUTION]
> **PROIBIÇÃO CATEGÓRICA DE LUCRO LÍQUIDO:**
> Enquanto os custos fixos estruturais e despesas operacionais da empresa (aluguel, folha de pagamento, pró-labore, servidores, contabilidade) não estiverem modelados no sistema, **é terminantemente proibido rotular ou exibir Margem de Contribuição como Lucro Líquido**.
> O painel exibe um banner com o `antiHallucinationDisclaimer` oficial e todas as explicações de indicadores reforçam que a Margem de Contribuição representa a sobra para cobertura de despesas fixas.

---

## 4. Critérios de Aceite e Cenários Gherkin

```gherkin
Funcionalidade: Painel Financeiro e Reconciliação por Pedido/Item

  Cenário: Totais reconciliáveis e visualização de indicadores com definições acessíveis
    Dado que o usuário acessa o Painel Financeiro
    Quando os dados agregados forem carregados
    Então a soma das receitas e deduções dos pedidos deve bater com os KPIs consolidados
    E cada card de KPI deve possuir botão de ajuda com explicação acessível (aria-label)
    E o disclaimer anti-alucinação deve estar visível informando que não se trata de Lucro Líquido

  Cenário: Filtros combináveis e busca por SKU
    Dado que o painel possui pedidos de múltiplos canais e empresas
    Quando o usuário selecionar o canal "MERCADO_LIVRE", uma empresa específica e digitar um SKU no filtro
    Então apenas os pedidos correspondentes aos filtros combinados devem ser listados
    E o contador de resultados e filtros ativos deve ser atualizado
    E o botão "Limpar Filtros" deve restaurar a visualização padrão

  Cenário: Alertas de incompletude e navegação corretiva
    Dado um pedido com item sem custo de produto cadastrado
    Quando a lista de pedidos for renderizada
    Então o pedido deve exibir um badge de alerta "Dados Incompletos"
    E ao abrir o detalhe do pedido, o motivo "PRODUCT_COST_MISSING" deve ser explicado
    E deve haver link/botão para cadastrar o custo do produto correspondente

  Cenário: Detalhamento de componentes e conciliação de extrato externo
    Dado um pedido no estado "ESTIMATED"
    Quando o usuário abrir a gaveta de detalhes e submeter um repasse com comissão divergente
    Então a reconciliação externa deve ser enviada via API
    E o status do pedido deve transitar para "DIVERGENT"
    E as divergências devem ser destacadas visualmente com as diferenças em reais
```
