# Especificação Técnica: P007-FE - Catálogo Responsivo, Busca, Filtros Combináveis e Detalhe de Anúncio

> **Status:** APPROVED &middot; **Data:** 18/11/2026 &middot; **Autor:** Equipe de Engenharia e Arquitetura  
> **Referência:** PROMPTS_MVP.md (P007-FE) &middot; DoD (.agents/rules/testing-and-quality.md)

---

## 1. Visão Geral e Objetivos de Negócio

Disponibilizar ao lojista uma interface web de alto desempenho, acessível e responsiva para visualização e gerenciamento do catálogo de produtos e anúncios integrados via canais oficiais (Mercado Livre e Shopee).

A interface garante:
1. **Visualização Integrada de Produtos e Anúncios:** Apresentação clara da relação $1:N$ entre o produto canônico de estoque e os múltiplos anúncios publicados nos marketplaces.
2. **Paginação Server-Side e Performance:** Navegação ágil por grandes volumes de anúncios sem carregamento integral em memória no cliente.
3. **Filtros Combináveis e Busca em Tempo Real:** Filtragem dinâmica e simultânea por Marketplace (`MERCADO_LIVRE`, `SHOPEE`), Conta de Vendedor, Status do Anúncio (`ACTIVE`, `PAUSED`, `CLOSED`) e busca textual por título, SKU ou ID externo.
4. **Resiliência de Estados de UI:** Estados padronizados de *Loading* (Skeletons acessíveis), *Empty State* (quando não houver dados ou filtros sem resultados) e *Error Feedback* (erros sanitizados sem vazamento de stacktrace ou UUIDs).
5. **Painel de Detalhes com Tratamento de Dados Ausentes:** Visualização aprofundada do anúncio selecionado, destacando claramente dados ausentes (ex: "Não informado", "Sem SKU", "Sem GTIN") com badges informativas.
6. **Segurança Multi-Tenant Absoluta:** O catálogo é estritamente isolado pelo tenant ativo do usuário logado; ao trocar de organização, os dados do tenant anterior são imediatamente expurgados da tela e o novo catálogo é carregado.

---

## 2. Cenários BDD Gherkin de Aceite

### Cenário 1: Tabela Responsiva com Paginação Server-Side
```gherkin
Dado que o vendedor autenticado acessa o módulo de Catálogo de Produtos & Anúncios
Quando a página é carregada
Então deve requisitar a página inicial de anúncios via paginação server-side (page=0, size=10)
E deve renderizar a tabela com colunas: Anúncio, Canal, Preço, Estoque, Status e Produto Vinculado
E deve exibir os controles de paginação (página atual, total de páginas, total de itens, botões anterior/próximo)
E os elementos interativos devem possuir touch target mínimo de 44px
```

### Cenário 2: Busca Textual e Filtros Combináveis
```gherkin
Dado que a listagem de anúncios está visível
Quando o vendedor digita "Mouse Gamer" no campo de busca
E seleciona o filtro de marketplace "MERCADO_LIVRE"
E seleciona o status "ACTIVE"
Então os parâmetros combinados devem ser disparados para a API de catálogo
E a tabela deve exibir exclusivamente os anúncios ativos do Mercado Livre correspondentes à busca
E deve ser exibido um botão para limpar todos os filtros aplicados
```

### Cenário 3: Estados de Loading, Empty e Tratamento de Erro
```gherkin
Dado que o vendedor realiza uma filtragem ou navegação
Quando a requisição está em trânsito
Então deve exibir o skeleton de carregamento com role="status"
Quando a resposta retornar uma lista vazia
Então deve exibir o componente de Empty State com role="region" e orientações
Quando a chamada à API falhar com status 401, 403 ou 500
Então deve apresentar mensagem de erro amigável e sanitizada, sem exibir UUIDs técnicos ou stacktraces
```

### Cenário 4: Detalhe do Anúncio e Tratamento Explícito de Dados Ausentes
```gherkin
Dado que a listagem de anúncios exibe itens com dados parciais
Quando o vendedor clica para ver os detalhes de um anúncio
Então deve abrir o painel lateral ou modal de detalhes do anúncio
E deve exibir o título, ID externo, permalink oficial, preço e estoque
E deve exibir os dados do produto canônico vinculado (título mestre, categoria, marca)
E caso o anúncio ou produto não possua SKU, GTIN ou variação informada
Então deve exibir ostensivamente o texto "Não informado" ou "Sem SKU" com badge informativa
E nenhum campo ausente deve renderizar vazio, "null" ou "undefined"
```

### Cenário 5: Isolamento Multi-Tenant Estrito
```gherkin
Dado que o usuário está no Tenant Alpha com seus anúncios listados
Quando o usuário alterna para o Tenant Beta via TenantSwitcher
Então a listagem anterior do Tenant Alpha deve ser imediatamente descartada
E uma nova requisição com o cabeçalho/contexto do Tenant Beta deve ser executada
E nenhuma informação de anúncios, contas ou produtos do Tenant Alpha deve permanecer visível
```
