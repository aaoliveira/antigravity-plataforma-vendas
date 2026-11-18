# Estratégia Abrangente de Testes e Garantia da Qualidade

> **Documento Normativo de Qualidade** &middot; **Versão:** 1.0.0  
> **Status:** Ativo &middot; **Data:** 17/11/2026  
> **Princípio Central:** Nenhum código vai para produção sem evidência real de teste automatizado executado com sucesso.  

---

## 1. Pirâmide de Testes Automatizados

A garantia de qualidade da **Seller Intelligence Platform** organiza-se em uma pirâmide de testes estruturada para cobrir desde a lógica de domínio pura até jornadas completas de usuário:

```
                      / \
                     /   \
                    / E2E \       Playwright (Jornadas Críticas de Ponta a Ponta)
                   /-------\
                  / Security\     Testes Negativos Cross-Tenant & RBAC
                 /-----------\
                / Integration \   Testcontainers (Postgres, RabbitMQ, Redis, Keycloak)
               /---------------\
              /  Architecture   \ ArchUnit & Spring Modulith (Limites de Módulos)
             /-------------------\
            /     Unit Tests      \ JUnit 5, AssertJ, Mockito (BE) & Vitest (FE)
           /-----------------------\
```

### 1.1 Detalhamento por Camada

| Camada de Teste | Ferramentas Obrigatórias | Escopo & Responsabilidade | Critério de Aceite |
|---|---|---|---|
| **Testes Unitários** | JUnit 5, AssertJ, Mockito (Backend); Vitest (Frontend) | Regras fiscais, fórmulas financeiras, scores de anúncio, normalizadores de dados e hooks de UI. | 100% determinísticos, execução em milissegundos, sem I/O de rede ou disco. |
| **Testes de Arquitetura** | ArchUnit, Spring Modulith | Verificação estática de boundaries modulares, proibições de imports cruzados e dependências direcionais. | Zero violações de isolamento de módulos no monólito. |
| **Testes de Integração** | Testcontainers, Spring Boot Test | Repositórios JPA, migrações Flyway em banco limpo, filas RabbitMQ reais, cache Redis e endpoints Spring MVC. | Contêineres Docker efêmeros executados e destruídos a cada execução. |
| **Testes de Contrato** | Springdoc / MockServer / WireMock | Compatibilidade de payloads REST (OpenAPI 3.0) e eventos de domínio serializados em JSON. | Schemas estritos validados sem campos inesperados. |
| **Testes de Segurança** | Spring Security Test, custom cross-tenant tests | Tentativas adversariais de acesso a dados de outros tenants, manipulação de IDs e bypass de RLS. | 100% de tentativas de violação bloqueadas com HTTP 403/404 e auditadas. |
| **Testes de Resiliência** | WireMock com fault injection | Simulação de HTTP 429 (`Too Many Requests`), instabilidade de rede externa, retries e Dead-Letter Queues. | Backoff com jitter comprovado e ausência de duplicação de pedidos. |
| **Testes End-to-End (E2E)** | Playwright (Headless Chromium) | Fluxo de onboarding, conexão de conta, simulação de margem no simulador e exportação de relatórios. | Execução estável sem *flakiness* nos breakpoints desktop e mobile. |

---

## 2. Suíte Mandatória de Regressão Financeira (10 Cenários)

O motor financeiro (`pricing` e `profitability`) deve obrigatoriamente executar e aprovar os 10 cenários determinísticos estabelecidos na Seção 13.2 do PRD:

1. **Cenário 1 — Pedido Completo Padrão:** Cálculo de margem com todos os componentes conhecidos (preço de venda, comissão percentual, taxa fixa por item, frete pago pelo comprador, CMV e imposto do Simples Nacional).
2. **Cenário 2 — Frete Subsidiado Parcialmente:** Rateio correto de frete com coparticipação do seller em faixas promocionais de envio gratuito.
3. **Cenário 3 — Desconto Financiado:** Distinção exata entre cupom financiado pelo seller (deduz da receita bruta) e subsídio comercial concedido pelo marketplace (não reduz o crédito do vendedor).
4. **Cenário 4 — Cancelamento Antes do Envio:** Estorno integral de comissões e taxas de marketplace, reclassificando o pedido sem perda de histórico.
5. **Cenário 5 — Devolução Parcial de Pedido Multi-Itens:** Estorno proporcional da comissão e frete correspondentes apenas às unidades devolvidas, recalculando o resultado final.
6. **Cenário 6 — Estorno Posterior ao Fechamento do Período:** Tratamento contábil que não altera retrospectivamente competências fiscais já encerradas, lançando o estorno no período corrente.
7. **Cenário 7 — Alteração de Custo com Vigência Futura:** Aplicação do novo custo de reposição de SKU apenas para pedidos criados após o timestamp da vigência da regra.
8. **Cenário 8 — Alteração de Regra de Comissão do Marketplace:** Simulação histórica mantendo a alíquota antiga para transações passadas e nova alíquota para eventos futuros.
9. **Cenário 9 — Transação em Moeda Estrangeira:** Conversão cambial com taxa travada no momento da liquidação financeira (se aplicável ao canal).
10. **Cenário 10 — Divisão de Pedido Multi-Itens com Taxa Fixa:** Rateio centavo a centavo de taxas fixas (ex.: R$ 6,00) entre itens de valores distintos, com algoritmo de compensação de resíduo para evitar divergência de arredondamento.

---

## 3. Validações Automáticas de Integridade de Dados (*Data Quality Gates*)

Antes de qualquer persistência em lote ou sincronização de pedidos e produtos, o pipeline executa verificações contra:
- **Duplicidade:** Chave única composta imutável (`marketplace + account_id + external_id`).
- **Detecção de Lag de Sincronismo:** Alerta se o timestamp da última coleta for superior a 6 horas.
- **Campos Mandatórios Ausentes:** Bloqueio de gravação e notificação se atributos vitais estiverem nulos (nunca preencher silenciosamente com zeros).
- **Valores Fora de Faixa:** Rejeição de preços negativos, comissões maiores que 100% ou quantidades fracionárias em produtos indivisíveis.
