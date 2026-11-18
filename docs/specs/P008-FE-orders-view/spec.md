# Especificação Técnica: P008-FE — Gestão e Detalhes de Pedidos com Filtros por Canal, Status e Incompletos

> **Identificador da Feature:** `P008-FE` &middot; **Versão:** 1.0.0  
> **Status da Spec:** Aprovada para Implementação  
> **Requisitos do PRD:** RF-007, RF-008, RF-034, RNF-001, RNF-002, RNF-003, RNF-006, RNF-007, ADR-002, ADR-004  
> **Componente Principal:** `frontend/src/components/OrdersView.tsx`  
> **Componente Detalhe:** `frontend/src/components/OrderDetailDrawer.tsx`

---

## 1. Contexto e Objetivos

A interface de pedidos (`OrdersView`) permite aos vendedores monitorar e auditar todas as vendas realizadas em canais homologados (Mercado Livre e Shopee):
1. **Filtros Combináveis Server-Side:**
   - Intervalo de datas de criação do pedido (`startDate`, `endDate`).
   - Canal de venda (`MERCADO_LIVRE`, `SHOPEE`).
   - Conta conectada (`sellerAccountId`).
   - Status do pedido (`PAID`, `SHIPPED`, `DELIVERED`, `CANCELLED`, `RETURNED`, `PENDING`).
   - Busca textual por ID externo, nome do comprador ou código de rastreamento.
2. **Exibição Explícita de Dados e Totais Conciliados:**
   - Totais financeiros (Subtotal, Frete, Total) em `BigDecimal`/formatados em BRL conciliados com o backend.
   - Data e hora de sincronização visíveis (`syncedAt`) e status de checkpoint.
   - Sinalização clara de pedidos incompletos ou dados ausentes (sem rastreamento, sem SKU, sem dados fiscais do comprador) através de badges informativas visíveis.
3. **Painel de Detalhes (`OrderDetailDrawer`):**
   - Drawer responsivo exibindo todos os metadados do pedido, itens adquiridos com quantidade e valores, informações de cancelamento/devolução e proveniência auditável.
4. **Resiliência e Acessibilidade:**
   - Loading skeletons (`role="status"`), empty states informativos (`role="region"`), tratamento sanitizado de erros 401/403/500 e conformidade WCAG 2.2 AA (touch targets $\ge 44\text{px}$).

---

## 2. Cenários BDD / Gherkin

### Cenário 1: Renderização de Pedidos e Totais Conciliados
```gherkin
Dado que o lojista possui pedidos sincronizados retornados pelo backend
Quando a tela de pedidos for carregada
Então a tabela deve listar os pedidos com ID externo, canal, status, valor total e data de sincronização
E os cards de resumo no topo devem exibir a contagem total de pedidos e os valores conciliados
```

### Cenário 2: Filtragem Dinâmica por Canal, Status e Intervalo de Datas
```gherkin
Dado que existem pedidos de múltiplos marketplaces e status
Quando o usuário selecionar o canal "MERCADO_LIVRE", status "PAID" e preencher o intervalo de datas
E acionar o botão "Filtrar"
Então a requisição à API deve conter todos os parâmetros correspondentes
E a tabela deve exibir apenas os pedidos correspondentes aos critérios selecionados
```

### Cenário 3: Sinalização de Pedidos Incompletos
```gherkin
Dado que um pedido sincronizado com status "SHIPPED" não possui código de rastreamento ou dados do comprador
Quando o pedido for exibido na lista ou no drawer de detalhes
Então uma badge com aviso "Incompleto" ou "Sem rastreio" deve ser visível com destaque
E nenhum valor nulo ou "undefined" deve ser renderizado na tela
```

### Cenário 4: Abertura e Fechamento do Drawer de Detalhes
```gherkin
Dado que um pedido está listado na tabela
Quando o usuário clicar em "Ver Detalhes"
Então o drawer lateral deve abrir exibindo os itens do pedido, quantidade, preços e proveniência
E ao pressionar a tecla Escape ou clicar no botão fechar, o drawer deve se fechar
```
