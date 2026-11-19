# Especificação Técnica: P020-FE - Editor de Rascunho de Anúncios com Comparação, Contagem de Caracteres e Histórico de Versões

## 1. Contexto & Objetivos

Esta especificação define o frontend do **P020-FE**, implementando o **AI Listing Studio (Editor de Rascunhos)** da **Seller Intelligence Platform**.
O objetivo é disponibilizar aos lojistas uma interface rica, responsiva e acessível para redigir, comparar, revisar e auditar títulos e descrições de anúncios gerados por IA, garantindo conformidade rigorosa com os limites de caracteres dos marketplaces e aplicando os guardrails de claims do MVP.

### Princípios Inegociáveis:
1. **Edição Preservada & Histórico Imutável:**
   - O operador pode editar tanto o título quanto a descrição.
   - Cada edição gera uma nova versão incremental (`versionNumber`), marcando `userEdited = true` e status `REVIEWED`.
   - O histórico de versões permanece acessível para auditoria e restauração.
2. **Comparação Lado a Lado (Original vs Editado):**
   - Visualização comparativa lado a lado ("Gerado Original pela IA" vs "Revisado pelo Operador").
   - Destaque das alterações textuais e dos atributos confirmados utilizados.
3. **Contagem e Limites Oficiais em Tempo Real:**
   - Gauge visual e contador de caracteres atualizados em tempo real conforme digitação.
   - Regras oficiais aplicadas por canal:
     * **Mercado Livre:** Título máximo de **60 caracteres**; Descrição texto puro.
     * **Shopee:** Título entre **20 e 120 caracteres**; Descrição entre **20 e 3.000 caracteres**.
   - Indicação visual clara: verde (dentro do limite), amarelo (próximo do limite) e vermelho (limite violado com aviso impeditivo).
4. **Revisão Humana Evidente:**
   - Alerta ostensivo: "RASCUNHO GERADO POR IA - EXIGE REVISÃO HUMANA".
   - Badges de proveniência (`AI_GENERATED`, `USER_EDITED`, `LOCAL_APPROVED`).
   - Tags de rastreabilidade de atributos confirmados (`brand`, `category`, `materials`, `dimensions`, `warranty`).
5. **Zero Publicação Externa no MVP:**
   - **NÃO oferecer botão de publicação externa.**
   - Ações disponíveis: "Aprovar Rascunho Local", "Salvar Revisão", "Rejeitar Rascunho", "Copiar Título", "Copiar Descrição", "Exportar Rascunho".
   - Alerta institucional declarando que publicação direta está fora do escopo do MVP.
6. **Acessibilidade e Responsividade:**
   - WCAG 2.1 AA (touch targets $\ge 44\text{px}$, suporte a teclado e leitores de tela).
   - Layout utilizável no mobile ($\ge 360\text{px}$) com alternância de abas/cards e no desktop em painel duplo.

---

## 2. Critérios de Aceite (Definition of Acceptance)

1. **Edição Preservada:**
   - Usuário edita título e descrição, salva alteração, e a versão original gerada pela IA permanece preservada no histórico.
2. **Limite Configurável / Validado:**
   - Contadores de caracteres exibem o tamanho atual e os limites oficiais do canal. Títulos que ultrapassam 60 caracteres no Mercado Livre ou que saem de 20-120 na Shopee são sinalizados com erro visual e bloqueio de submissão.
3. **Revisão Humana Evidente:**
   - A interface demarca ostensivamente que o conteúdo é um rascunho assistido que exige aprovação humana, destacando se a versão atual foi editada por operador.
4. **Sem Botão Publicar no MVP:**
   - Verificado via teste automatizado que a interface não contém nenhum botão ou ação que alegue publicar ou enviar o anúncio para o Mercado Livre ou Shopee.
5. **Copiar / Exportar Funcional:**
   - Botões para copiar título, descrição ou rascunho completo para a área de transferência, e exportação em formato TXT e JSON.
6. **Acessível e Responsivo:**
   - Totalmente operável por teclado, sem overflow horizontal em telas móveis ($\ge 360\text{px}$).

---

## 3. Arquitetura de Componentes Frontend

- `src/types/draftlisting.ts`: Tipagens TypeScript completas espelhando os DTOs do backend.
- `src/app/api/ai/drafts/*`: Rotas BFF Next.js 15 repassando `x-tenant-id`.
- `src/components/draftlisting/CharacterLimitGauge.tsx`: Barra de progresso e contador de caracteres com estados (Normal, Atenção, Excedido).
- `src/components/draftlisting/DraftDiffViewer.tsx`: Comparador visual do título e descrição (Gerado vs Revisado).
- `src/components/draftlisting/VersionHistoryDrawer.tsx`: Gaveta lateral exibindo linha do tempo de versões, notas de revisão e restauração.
- `src/components/draftlisting/DraftListingEditorView.tsx`: Tela principal com lista de rascunhos, modal de nova geração, editor interativo, gauges e exportadores.
- `src/components/AppShell.tsx`: Módulo `AI_STUDIO` atualizado de `PLANNED` para `READY`, renderizando `DraftListingEditorView`.
