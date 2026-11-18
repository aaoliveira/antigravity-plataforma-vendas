# Especificação Técnica — P011-FE: Interface de Comparação Lado a Lado, Detalhes de Correspondência e Destaque de Confiança

**Status:** IN_PROGRESS  
**Prompt de Origem:** P011-FE  
**Data:** 18/11/2026  
**Responsável:** Antigravity Agent  
**Contexto:** P011-BE aprovado (Backend de Comparação de Listings Equivalentes, Variação Temporal e Histórico Não-Inventado).

---

## 1. Objetivo e Escopo

Construir a camada de apresentação frontend (Next.js 15, Tailwind CSS, React) para a funcionalidade de comparação de listings equivalentes e análise competitiva de preços, integrada aos endpoints desenvolvidos em `P011-BE` (`/api/v1/market-comparison/*`).

A interface deve garantir transparência absoluta aos sellers:
1. **Diferenças Claras:** Exibição lado a lado de anúncios base vs concorrentes/candidatos, com matriz de diferenças estruturadas destacando atributos idênticos (`✓ Idêntico`) e divergentes (`⚠️ Divergente`) em marca, modelo, condição, voltagem, capacidade/memória, GTIN, frete e preço.
2. **Baixa Confiança Destacada:** Pontuações abaixo do limiar oficial ($< 75\%$) ou fracas ($< 50\%$) devem possuir distintivos chamativos em tons de alerta (âmbar/vermelho), alertando o vendedor sobre o risco de imprecisão.
3. **Sem Falsa Equivalência Visual:** Anúncios formalmente incompatíveis (`isCompatible === false` ou `isEquivalent === false`) são terminantemente proibidos de utilizar badges verdes de equivalência. Uma tarja explícita de "Alerta Anti-Falsa Equivalência" é exibida destacando os motivos impeditivos (ex.: marcas distintas, itens usados vs novos, voltagens discrepantes).
4. **Variação Temporal e Frete Desconhecido:**
   - Observações com janela temporal $> 30$ dias ocultam o percentual de variação de preço para prevenir falsas conclusões econômicas.
   - Listings com frete não informado pelo canal (`UNKNOWN`) exibem explicitamente "Frete não informado pelo canal" sem forjar ou estimar total fictício.
5. **Histórico Legítimo de Preços:** Drawer ou painel de auditoria de histórico que consome `GET /api/v1/market-comparison/price-history` e estampa aviso de integridade: "Histórico sem retroatividade inventada: apenas coletas reais com timestamp auditável são exibidas".

---

## 2. Requisitos Funcionais e Critérios de Aceite

| ID | Requisito | Critério de Aceite |
| :--- | :--- | :--- |
| **FR-COMP-01** | Visualização Lado a Lado | Apresentar produto de referência à esquerda e até 4 anúncios candidatos à direita em desktop (grid responsiva ou tabela comparativa) e cards empilhados em mobile. |
| **FR-COMP-02** | Matriz de Diferenças de Atributos | Comparar linha a linha: Preço, Frete, Preço Total, Condição, Marca, Modelo, GTIN, Voltagem, Armazenamento/RAM e Atributos Técnicos. Destacar divergências visualmente com ícones e backgrounds específicos. |
| **FR-COMP-03** | Indicador de Confiança & Breakdown | Exibir score geral de similaridade ($0.0$ a $1.0$ / $0\%$ a $100\%$) e permitir expansão do detalhamento (`gtinScore`, `brandScore`, `modelScore`, `titleScore`, `attributesScore`). |
| **FR-COMP-04** | Destaque de Baixa Confiança | Scores $< 50\%$ recebem badge vermelho "Baixa Confiança" e scores $50-74\%$ recebem badge âmbar "Borderline / Revisão Manual". |
| **FR-COMP-05** | Regra Anti-Falsa Equivalência | Itens com `isCompatible === false` ou `isEquivalent === false` NUNCA exibem "Equivalente". Exibir banner vermelho/âmbar: "Anúncio Incompatível: Não utilizar como referência de precificação". |
| **FR-COMP-06** | Janela Temporal Segura | Se `isTemporalWindowComparable === false`, ocultar delta percentual e exibir advertência de defasagem de datas (> 30 dias). |
| **FR-COMP-07** | Frete Não Conhecido | Se `shippingStatus === 'UNKNOWN'`, mostrar "Não informado pelo canal" e nunca inventar preço total com frete. |
| **FR-COMP-08** | Filtros e Ordenação | Filtrar candidatos por: Nível de Confiança (Todos, Alta Confiança, Revisão/Borderline, Incompatíveis), Marketplace e Condição. Ordenar por Score, Preço ou Recência. |
| **FR-COMP-09** | Histórico Auditável de Preços | Modal/Drawer para visualização de histórico de preços legítimo consumindo a API real, com gráfico/tabela cronológica e sem dados retroativos fictícios. |
| **FR-COMP-10** | Rotas BFF Next.js 15 | Proxy seguro com propagação de token JWT e `tenant_id` para `/api/v1/market-comparison/*`. |

---

## 3. Arquitetura de Componentes e Rotas

### 3.1 Rotas BFF (Next.js 15 App Router)
- `frontend/src/app/api/market-comparison/compare/route.ts` (POST)
- `frontend/src/app/api/market-comparison/find-equivalents/route.ts` (POST)
- `frontend/src/app/api/market-comparison/price-history/route.ts` (GET)
- `frontend/src/app/api/market-comparison/saved/route.ts` (GET, POST)

### 3.2 Componentes React
- `frontend/src/components/ListingComparisonView.tsx`:
  - Visão principal de comparação e seleção de anúncios.
  - Matriz de atributos comparativos lado a lado com realce de divergências (`DiffRow`).
  - Painel de pontuação de equivalência com breakdown visual (`ScoreBreakdownDrawer` / `ScoreAccordion`).
  - Banners de alerta anti-falsa equivalência e restrições temporais de frete.
  - Barra de ferramentas com filtros (confiança, marketplace, compatibilidade).
- `frontend/src/components/ListingPriceHistoryDrawer.tsx`:
  - Drawer com pontos históricos reais, gráfico ou tabela temporal e métricas (mínimo, máximo, variação no período).
- `frontend/src/components/MarketIntelligenceView.tsx`:
  - Incorporação da aba `⚖️ Comparação & Equivalência` ao lado de `Observações` e `Pesquisa`.

---

## 4. Estratégia de Testes

- `frontend/src/components/ListingComparisonView.test.tsx`:
  - **Cenário 1:** Renderização lado a lado do item base e candidatos com matriz de atributos.
  - **Cenário 2:** Diferenças claras — destacar atributos divergentes (ex.: Marca Apple vs Samsung, Condição Novo vs Usado) e idênticos.
  - **Cenário 3:** Baixa confiança destacada — badges vermelhos/âmbar para scores abaixo do limiar.
  - **Cenário 4:** Sem falsa equivalência visual — anúncios incompatíveis nunca exibem "Equivalente" e mostram banner impeditivo.
  - **Cenário 5:** Janela temporal $> 30$ dias oculta variação percentual de preço e exibe aviso de defasagem.
  - **Cenário 6:** Frete desconhecido (`UNKNOWN`) exibe "Não informado pelo canal" sem forjar preço final com frete.
  - **Cenário 7:** Filtros de compatibilidade e marketplace reduzem a lista de candidatos conforme esperado.
  - **Cenário 8:** Histórico legítimo de preços exibe apenas pontos reais persistidos sem retroatividade inventada.
  - **Cenário 9:** Acessibilidade WCAG 2.2 AA (elementos interativos $\ge 44\text{px}$, suporte a ESC em drawers/modais, contrastes adequados).
