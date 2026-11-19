# Especificação Técnica: P019-FE - Interface do Assistente de IA, Chat Consultivo e Indicador de Custo/Origem

## 1. Contexto & Objetivos

Esta especificação técnica define o frontend da etapa **P019-FE**, integrando os recursos do Gateway Multi-Provider de IA homologados no **P019-BE**.
O objetivo é fornecer ao vendedor uma interface conversacional e assistiva avançada, estritamente orientada por dados comprovados e guardrails de governança:
- **Histórico Tenant-Scoped:** Mensagens e histórico de auditoria vinculados exclusivamente à organização ativa.
- **Indicação Transparente de Fontes:** Cada resposta gerada pela IA cita formalmente as ferramentas internas e origens de dados consultadas (`sources`).
- **Data e Fórmulas dos Dados:** Identificação explícita do momento de execução e versão dos cálculos determinísticos (`timestamps`).
- **Aviso de Estimativa & Anti-Alucinação:** Aviso de que valores representam projeções e margem de contribuição (e não lucro líquido ou ações consumadas no marketplace).
- **Sem Botões de Execução Externa (MVP):** Nenhuma ação de mutação em marketplaces externos (como "alterar preço no ML" ou "pausar anúncio no Shopee") é oferecida; o assistente atua exclusivamente como consultor analítico em modo somente leitura.
- **Feedback de Resposta:** Mecanismo integrado de avaliação (útil / não útil e comentários de melhoria).
- **Limites e Erros Transparentes:** Exibição clara de tokens consumidos, custo financeiro em BRL, tempo de resposta (ms), rate limits e alertas de segurança (ex.: detecção de Prompt Injection).

---

## 2. Critérios de Aceite (Definition of Acceptance)

1. **Fontes Acessíveis:**
   - Cada resposta do assistente exibe badges ou tags interativas das fontes consultadas (`catalog:marketplace_listings`, `pricing:PricingEngineService`, `analytics:connected_account_metrics`).
   - Painel/gaveta acessível para inspecionar os dados retornados pelas ferramentas somente leitura.
2. **Respostas Não Alegam Ação Executada:**
   - O layout e os avisos do assistente reforçam que as recomendações são simulações analíticas.
   - Proibição absoluta de botões que apliquem ações em canais de venda externos nesta fase do MVP.
3. **Erros e Limites Claros:**
   - Tentativas de injeção de prompt ou parâmetros não autorizados retornam alerta visual imediato de bloqueio de segurança.
   - Indisponibilidade de dados (ex: CTR orgânico no Mercado Livre) é apresentada com aviso explícito e badge `UNAVAILABLE` ou `INSUFFICIENT_DATA`.
   - Exibição de métricas operacionais por resposta: Tokens de Entrada, Tokens de Saída, Total, Custo Estimado (R$) e Latência (ms).
4. **Feedback de Resposta:**
   - Botões acessíveis de feedback positivo (Polegar para cima) e negativo (Polegar para baixo) com confirmação visual e opção de observação textual.
5. **Acessibilidade e Responsividade:**
   - Conformidade com WCAG 2.1 AA: navegação completa por teclado, atributos ARIA adequados (`role="log"`, `aria-live="polite"`, `aria-label`).
   - Layout responsivo mobile-first sem transbordo horizontal (zero overflow em telas $\ge 360$px).

---

## 3. Arquitetura da Solução

### 3.1 Tipos TypeScript (`frontend/src/types/aiassistant.ts`)
- `AiProvider`: `'GEMINI' | 'OPENAI' | 'CLAUDE' | 'MOCK_PROVIDER'`
- `PromptTemplateDto`: chave, versão, descrição, instruções e lista de tools autorizadas.
- `AiToolDefinitionDto`: nome da tool, descrição e schema JSON dos parâmetros.
- `AiPromptRequestDto`: solicitação assistiva enviada ao Gateway.
- `AiUsageMetricsDto`: tokens (prompt, completion, total), latência e custo em BRL.
- `StructuredAiResponseDto`: resposta auditada contendo texto de análise, fontes, timestamps, notas de indisponibilidade e disclaimers.
- `ChatMessage`: estrutura de estado da conversa (remetente `user` ou `assistant`, conteúdo, metadados de execução, status e feedback).

### 3.2 Rotas BFF Next.js (`frontend/src/app/api/ai/`)
- `POST /api/ai/execute`: encaminha requisição autenticada para o backend Spring `/api/v1/ai/execute`.
- `GET /api/ai/prompts`: consulta o catálogo de prompts em `/api/v1/ai/prompts`.
- `GET /api/ai/tools`: lista tools registradas em `/api/v1/ai/tools`.
- `GET /api/ai/audits`: obtém o histórico paginado de auditoria de consumo em `/api/v1/ai/audits`.

### 3.3 Componentes React (`frontend/src/components/aiassistant/`)
1. `AiAssistantView.tsx`: tela principal com histórico de conversas, seleção de template versionado, envio de mensagens e painel lateral de métricas/fontes.
2. `AiChatMessage.tsx`: renderizador de mensagens do usuário e da IA, com blocos de fontes, timestamps, disclaimer anti-alucinação e botões de feedback.
3. `AiSourcesDrawer.tsx`: gaveta deslizante para inspeção aprofundada de dados brutos e fontes oficiais consultadas pelas tools.
4. `AiUsageMetricsPill.tsx`: indicador compacto de governança e custo de tokens em BRL.

---

## 4. Plano de Testes & Verificação

- **Testes Unitários e de Componente (`AiAssistantView.test.tsx`):**
  - Renderização inicial com seleção de prompt template catalogado.
  - Envio de mensagem com simulação de resposta assistiva e fontes exibidas.
  - Bloqueio de Prompt Injection na UI com exibição de aviso amigável de segurança RFC 7807.
  - Interação com feedback de resposta (útil / não útil).
  - Verificação de que não existem botões de execução externa nos anúncios.
  - Acessibilidade por teclado (Enter, Escape na gaveta de fontes, touch targets $\ge 44$px).
- **Testes de Integração de Navegação (`AppShell.test.tsx`):**
  - Adição do item `AI_ASSISTANT` no menu lateral e gaveta mobile.
- **Suíte Completa:** Execução de `npm test -- --run`, `npx tsc --noEmit` e `npm run build`.
