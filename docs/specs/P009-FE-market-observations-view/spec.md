# Especificação Técnica: P009-FE — Exibição de Origem, Evidências, Confiança e Explicação de Dados na UI

> **Identificador da Feature:** `P009-FE` &middot; **Versão:** 1.0.0  
> **Status da Spec:** Aprovada para Implementação  
> **Requisitos do PRD:** RF-009, RF-010, RF-011, RNF-007, RNF-009, RNF-010, RNF-014  
> **Camada:** Frontend (Next.js 15, React 19, Tailwind CSS, TypeScript)  
> **Módulo:** `MARKET` (Market Intelligence & Observações de Mercado)

---

## 1. Contexto e Objetivos

O módulo de Inteligência de Mercado no Frontend é responsável por apresentar com transparência total a proveniência dos dados coletados, diferenciando categoricamente o que é dado oficial, dado público observado e estimativa inferida.

Esta entrega implementa:
1. **Cards e Indicadores de Proveniência:**
   - Visualização de métricas agregadas de proveniência: Total Observado, Cobertura Oficial, Dados Públicos e Estimativas Ativas.
2. **Badges Estritos de Classificação:**
   - `OFFICIAL`: Badge verde com ícone de escudo/verificado ("Oficial").
   - `OBSERVED`: Badge azul com ícone de olho ("Público Observado").
   - `ESTIMATED`: Badge âmbar/laranja com ícone de balança/estatística ("Estimado - Metodologia Documentada").
     - **Regra Rígida de Aceite:** *"dado estimado nunca aparece como oficial"*.
   - `USER_INPUT`: Badge roxo ("Entrada Manual").
3. **Visibilidade da Fonte e Atualização:**
   - Exibição visível do canal de marketplace (Mercado Livre, Shopee, Amazon), link externo validado para o anúncio (`target="_blank" rel="noopener noreferrer"`), método de coleta, data de observação na fonte (`observedAt`) e data de coleta na plataforma (`collectedAt`).
4. **Gaveta Lateral de Detalhes e Explicação do Dado (`ObservationDetailDrawer`):**
   - Abertura com um clique para qualquer observação.
   - Se `ESTIMATED`: exibe alerta de transparência estatística, metodologia detalhada, versão do modelo, janela temporal e percentual de confiança.
   - Se houver correspondência com o catálogo canônico: aciona matching explicável e exibe score global, decomposição (GTIN, Título, Marca) e texto legível justificando a classificação (`MATCHED`, `POTENTIAL_MATCH`, `NO_MATCH`).
   - Visualização do payload mínimo higienizado (sem dados pessoais ou credenciais).
5. **Filtros Combináveis e Paginação Server-Side:**
   - Filtros por classificação de proveniência (`sourceType`), canal (`marketplace`), termo de busca / GTIN.
6. **Acessibilidade e Responsividade (WCAG 2.2 AA & Mobile):**
   - Suporte a telas a partir de 360px de largura com cards mobile e touch targets $\ge 44\text{px}$.
   - Suporte a navegação por teclado: tecla `ESC` fecha a gaveta, `aria-modal="true"`, `role="dialog"`, `role="status"` e `role="region"`.

---

## 2. Cenários BDD / Gherkin

### Cenário 1: Diferenciação Estrita de Dado Estimado vs Oficial
```gherkin
Dado que o usuário acessa a tela de Inteligência de Mercado
Quando a lista renderizar uma observação com sourceType "ESTIMATED"
Então ela deve exibir um badge âmbar com o texto "Dado Estimado"
E em nenhuma circunstância deve exibir o badge verde ou rótulo de "Oficial"
E ao lado deve ser exibida a versão do modelo e o percentual de confiança
```

### Cenário 2: Visibilidade de Origem, Link Externo Seguro e Datas
```gherkin
Dado que uma observação de mercado do Mercado Livre é exibida na tabela ou card
Então o marketplace "Mercado Livre" deve ser exibido com destaque
E a URL de origem deve ser um link clicável seguro com rel="noopener noreferrer"
E as datas de observação e coleta devem estar formatadas e visíveis
```

### Cenário 3: Abertura e Explicação Detalhada do Dado (Drawer)
```gherkin
Dado que o usuário clica em "Ver Explicação" em uma observação
Quando o drawer lateral for aberto
Então a gaveta deve ter role="dialog" e aria-modal="true"
E se o dado for ESTIMATED, a metodologia completa e janela temporal devem ser explicadas
E o botão de fechar ou a tecla ESC devem fechar o drawer restaurando o foco
```

### Cenário 4: Explicação de Matching Canônico com Score
```gherkin
Dado que o usuário abre a explicação de uma observação com GTIN coincidente a um produto
Quando o matching explicável for exibido
Então o score geral (ex: 88%) deve ser acompanhado do status ("MATCHED" ou "POTENTIAL_MATCH")
E a justificativa textual detalhada deve explicar o impacto do GTIN, termos do título e marca
E se o score for inferior a 75%, deve alertar que o item NÃO foi vinculado automaticamente
```

### Cenário 5: Responsividade Mobile e Acessibilidade
```gherkin
Dado que a tela é acessada em um dispositivo móvel com viewport de 360px
Quando a página for renderizada
Então a visualização em cards táticos deve substituir a tabela horizontal
E todos os elementos interativos devem ter dimensões mínimas de 44x44px
```
