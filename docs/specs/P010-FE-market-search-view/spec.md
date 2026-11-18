# Especificação Técnica: P010-FE — Interface de Pesquisa Autorizada, Políticas de Fontes, Comparação e Badges de Proveniência

> **Identificador da Feature:** `P010-FE` &middot; **Versão:** 1.0.0  
> **Status da Spec:** Aprovada para Implementação  
> **Requisitos do PRD:** RF-005, RF-006, RF-009, RF-010, RF-034, RNF-001, RNF-002, RNF-004, RNF-006, RNF-014, ADR-002, ADR-004  
> **Componente:** `MarketSearchView.tsx`, `frontend/src/app/api/market-search/*`

---

## 1. Contexto e Objetivos

Esta entrega desenvolve a interface de pesquisa de mercado e inteligência de produtos para vendedores multi-canal, conectando-se aos endpoints autorizados do monólito backend (`/api/v1/market-search`) através do Next.js 15 Backend-for-Frontend (BFF).

A interface atende com rigor aos seguintes princípios e critérios de aceite:
1. **Busca por Termo e Categoria com Filtros Combináveis:**
   - Campo de busca principal por texto/SKU/GTIN.
   - Filtro por Marketplace (`TODOS`, `MERCADO_LIVRE`, `SHOPEE`, `AMAZON`, `INTERNAL`).
   - Filtro por Categoria e Faixa de Preço (`minPrice`, `maxPrice`).
   - Toggle para busca exclusiva no catálogo próprio (`onlyOwnCatalog`).
2. **Resultados com Ordenação Flexível:**
   - Ordenação por Preço (Menor Preço, Maior Preço), Título (A-Z, Z-A) e Data de Coleta (Mais Recentes).
3. **Diferenciação Estrita entre Anúncio Próprio e Concorrente:**
   - Badge visual claro de distinção de titularidade:
     - **Anúncio Próprio:** `Próprio` (badge azul/verde com indicação de catálogo do seller).
     - **Concorrente / Mercado:** `Concorrente` (badge âmbar/cinza indicando item de terceiro observado publicamente).
4. **Badges de Proveniência e Metadados Obrigatórios Sempre Visíveis:**
   - Badges de proveniência padronizados (`OFFICIAL`, `OBSERVED`, `LICENSED`).
   - Link oficial externo sanitizado com `rel="noopener noreferrer" target="_blank"` para auditoria no domínio oficial.
   - Data de coleta/sincronização formatada no padrão brasileiro (`pt-BR`).
   - Identificador da política governante (`sourcePolicyId`).
   - Nota anti-alucinação explícita (`salesMetricNotice`) esclarecendo que métricas de vendas concorrenciais não são forjadas.
5. **Painel / Modal de Comparação Lado a Lado:**
   - Seleção de múltiplos itens (checkboxes de comparação) para contraste direto de preço, vendedor, marketplace, condição, código de barras (GTIN) e proveniência.
6. **Avisos de Capacidade e Fail-Closed:**
   - Sinalização transparente quando uma busca retorna `RESTRICTED` (ex: Shopee anti-scraping fallback) ou `UNVERIFIED` (Amazon fail-closed), apresentando o motivo legal e diretrizes oficiais.
   - Tratamento de erro HTTP 429 Too Many Requests com tempo de espera (`Retry-After`).
7. **Responsividade Mobile & Acessibilidade WCAG 2.2 AA:**
   - No desktop: tabela de dados estruturada com ordenação e paginação.
   - No mobile: alternância automática para cartões informativos estruturados (Cards view) ou container horizontal com scroll acessível.
   - Touch targets $\ge 44\text{px}$, suporte integral a teclado e leitor de tela (`aria-label`, `role="region"`, `role="table"`, `role="dialog"`).

---

## 2. Cenários BDD / Gherkin

### Cenário 1: Pesquisa com Filtros e Exibição de Proveniência e Titularidade
```gherkin
Dado que o usuário está na tela de Pesquisa de Mercado
Quando preenche o termo "cadeira gamer" e filtra por marketplace "MERCADO_LIVRE"
E clica no botão "Pesquisar"
Então a listagem deve exibir os resultados normalizados
E cada resultado deve exibir a fonte, data de coleta, política governante e badge de proveniência
E deve distinguir visualmente se o item é "Próprio" ou "Concorrente"
```

### Cenário 2: Exibição de Aviso Fail-Closed / Anti-Scraping para Shopee e Amazon
```gherkin
Dado que o usuário pesquisa no marketplace "SHOPEE"
Quando a resposta do backend retorna capabilityStatus "RESTRICTED"
Então a interface deve exibir um banner de aviso esclarecendo a recusa de web scraping
E informar que os dados exibidos pertencem ao catálogo próprio do tenant
E para "AMAZON" deve exibir banner de capacidade "UNVERIFIED" com zero dados falsos
```

### Cenário 3: Comparação Lado a Lado de Anúncios Selecionados
```gherkin
Dado que existem múltiplos resultados na pesquisa
Quando o usuário seleciona 2 itens para comparação e clica em "Comparar Selecionados"
Então uma gaveta ou modal de comparação deve abrir com os dois produtos lado a lado
E exibir as diferenças de preço, vendedor, marketplace, condição e proveniência
```

### Cenário 4: Responsividade Mobile (Alternância para Cards Acessíveis)
```gherkin
Dado que o usuário acessa a plataforma em viewport móvel (< 768px)
Quando os resultados da busca forem carregados
Então os dados devem ser apresentados em cards verticais legíveis
E todos os metadados (preço, proveniência, data, link oficial) devem permanecer visíveis
E touch targets para botões e paginação devem possuir altura mínima de 44px
```

### Cenário 5: Tratamento de Limite de Taxa (HTTP 429 Too Many Requests)
```gherkin
Dado que o limite de taxa do conector ou canal foi atingido
Quando o backend retornar HTTP 429 com cabeçalho Retry-After
Então a interface deve exibir um feedback informativo amigável
E instruir o usuário sobre o tempo restante para nova requisição
```

---

## 3. Arquitetura de Componentes Frontend

- `frontend/src/app/api/market-search/route.ts` &middot; BFF Proxy para `GET /api/v1/market-search`
- `frontend/src/app/api/market-search/sources/route.ts` &middot; BFF Proxy para `GET /api/v1/market-search/sources`
- `frontend/src/app/api/market-search/history/route.ts` &middot; BFF Proxy para `GET /api/v1/market-search/history`
- `frontend/src/components/MarketSearchView.tsx` &middot; View principal com formulário de busca, banner de capacidades, tabela responsiva, cards mobile, filtros, ordenação e modal de comparação.
- `frontend/src/components/ProductCompareModal.tsx` &middot; Modal/drawer de comparação lado a lado de produtos.
