# Padrões de Teste, Qualidade e Definition of Done (DoD)

## 1. Princípio da Evidência Comprovada
Nenhuma funcionalidade, classe ou endpoint é considerado concluído sem que critérios de aceite objetivos sejam validados por testes automatizados executados com sucesso no ambiente. A simples existência de código sem testes executados e evidências registradas constitui violação de qualidade.

---

## 2. Pirâmide de Testes e Ferramentas

| Nível de Teste | Escopo | Ferramentas Obrigatórias | Critério de Sucesso |
|---|---|---|---|
| **Unitários** | Regras de negócio, cálculos matemáticos, scores, normalização de dados e validações de domínio. | JUnit 5, AssertJ, Mockito (backend); Vitest (frontend). | 100% determinísticos, execução em milissegundos, isolados de IO. |
| **Integração** | Repositórios JPA, migrações Flyway, filas RabbitMQ, cache Redis, endpoints Spring MVC e Keycloak OIDC. | Testcontainers (Postgres, RabbitMQ, Redis, Keycloak), Spring Boot Test. | Banco e brokers reais efêmeros rodando via Docker; migrações testadas em banco limpo. |
| **Arquitetura** | Limites entre módulos de domínio, dependências cíclicas e respeito a padrões de camadas. | ArchUnit, Spring Modulith. | Zero violações de isolamento de módulos no monólito. |
| **Contrato** | Schemas OpenAPI, DTOs de conectores externos e payloads de eventos. | Spring Cloud Contract ou MockServer / WireMock. | Validação estrita de compatibilidade com especificações oficiais. |
| **End-to-End (E2E)** | Jornadas críticas do usuário (onboarding, conexões, cálculos, AI Studio, dashboards). | Playwright, Testing Library. | Execução em browser headless contra ambiente integrado. |
| **Segurança** | Isolamento multi-tenant, autorização RBAC, injeção SQL, XSS, CSRF, prompt injection. | Testes automatizados de segurança, OWASP ZAP (quando aplicável). | 100% de tentativas de acesso cross-tenant bloqueadas e auditadas. |
| **Resiliência** | Falhas transitórias de APIs de marketplaces, desconexões de rede, webhooks duplicados. | WireMock com fault injection, Chaos Monkey (se aplicável). | Idempotência comprovada, retries com backoff e envio para DLQ. |

---

## 3. Suíte Mínima de Regressão Financeira (Obrigatória)
O motor financeiro deve conter obrigatoriamente testes determinísticos para os 10 cenários do PRD (Seção 13.2):
1. **Pedido completo padrão:** todos os componentes de tarifa, frete e imposto conhecidos.
2. **Frete subsidiado parcialmente:** cálculo proporcional do frete suportado pelo seller vs. marketplace.
3. **Desconto financiado:** distinção precisa entre desconto arcado pelo seller e subsídio comercial do marketplace.
4. **Cancelamento antes do envio:** estorno de tarifas e reclassificação financeira sem perda de histórico.
5. **Devolução parcial:** estorno proporcional de tarifas, comissões e impostos sobre os itens devolvidos.
6. **Estorno posterior ao fechamento:** registro retroativo ou em competência atual sem sobrescrever períodos fechados.
7. **Alteração de custo com vigência futura:** novo custo do SKU aplicado apenas a pedidos posteriores à vigência.
8. **Alteração de regra de comissão:** nova taxa percentual ou fixa aplicada estritamente conforme versão de vigência.
9. **Pedido em moeda estrangeira:** conversão cambial com taxa registrada, se aplicável ao marketplace.
10. **Divisão de pedido multi-itens:** rateio matemático preciso de tarifas fixas e frete entre itens sem erro de centavos.

---

## 4. Verificações de Qualidade de Dados (Data Quality)
Antes de persistir dados sincronizados ou calculados, devem ser executadas validações automáticas contra:
- **Duplicidade:** Chave única composta (`marketplace + account_id + external_id`).
- **Atraso de Sincronização (*Lag*):** Monitoramento do intervalo entre a última execução e o timestamp atual.
- **Campos Obrigatórios Ausentes:** Notificação de integridade incompleta (nunca substituição silenciosa).
- **Valores Fora de Faixa:** Preços negativos, margens impossíveis, comissões superiores a 100%.
- **Divergência de Totais:** Soma dos itens versus total do pedido no marketplace.

---

## 5. Definição de Pronto (Definition of Done — DoD)
Uma tarefa ou funcionalidade só é considerada `DONE` quando preencher cumulativamente os 10 requisitos abaixo:
1. Requisitos e critérios de aceite do PRD estão vinculados à implementação e validados.
2. Fluxo principal (*happy path*), cenários de erro e casos de borda estão cobertos por testes automatizados.
3. Isolamento multi-tenant, autorização de papéis (RBAC) e escopos de acesso validados no backend.
4. Logs estruturados JSON emitidos com correlação (`trace_id`, `span_id`), sem tokens, segredos, senhas ou PII (LGPD).
5. Contratos de API (OpenAPI 3.0) e schemas de eventos de domínio estão documentados e versionados.
6. Resiliência comprovada: timeouts configurados, retries com jitter, idempotência garantida e DLQ configurada.
7. Documentação técnica, dicionários de dados e instruções operacionais atualizados.
8. Acessibilidade (WCAG) e responsividade verificadas nos componentes frontend.
9. Toda a suíte de testes do pipeline está verde (zero falhas, zero testes pulados sem justificativa).
10. Relatório de execução e evidências de validação anexados ao status da fase/PR.
