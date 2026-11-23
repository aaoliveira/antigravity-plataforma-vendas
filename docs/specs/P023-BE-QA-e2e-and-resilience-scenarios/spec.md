# Especificação Técnica: P023-BE/QA — Cenários E2E, Resiliência e Integração Ponta a Ponta

> **Versão:** 1.0.0 &middot; **Status:** EM IMPLEMENTAÇÃO &middot; **Data:** 23/11/2026  
> **Camada:** Backend / QA & E2E Integration (Spring Boot 3.5 / JUnit 5 / Testcontainers / MockMvc)  
> **Objetivo:** Validar o pipeline completo do sistema (autenticar → tenant → conectar marketplace sandbox → sincronizar dados → consultar catálogo/pedidos → simular margem → consultar dashboard → exportar) e os cenários críticos de resiliência e segurança (falhas de OAuth, API indisponível, rate limit, token expirado, retry, DLQ, duplicidade e acesso cross-tenant).  
> **Critérios de Aceite:** Pipeline reproduzível; evidências anexadas; nenhum cenário crítico ignorado.

---

## 1. Arquitetura dos Cenários de Teste

Para garantir a máxima integridade, reproduzibilidade e cobertura sem mocks de fachada para regras de negócio fundamentais, dividimos a suíte em duas classes de teste principais sob `com.sellerintelligence.e2e`:

### 1.1 `EndToEndLifecycleIntegrationTest.java` (Pipeline Completo do Ciclo de Vida)
Executa a esteira sequencial ponta a ponta reproduzindo o fluxo operacional real:
1. **Autenticação & Tenant:** Obtenção de token JWT assinado para um operador do Tenant Alpha com claims e tenant_id válidos.
2. **Contexto Multi-Tenant:** Estabelecimento estrito do `TenantContextHolder` garantindo isolamento em aplicação e banco de dados.
3. **Conexão Marketplace Sandbox:** Registro e conexão de conta oficial de marketplace em modo sandbox com credenciais cifradas.
4. **Sincronização de Dados:** Ingestão de anúncios de catálogo e pedidos de venda (`OrderStatus.PAID`), com apuração automática de fatos financeiros contábeis.
5. **Consulta de Catálogo & Pedidos:** Validação dos endpoints REST `/api/v1/catalog/listings` e `/api/v1/orders` com filtros e ordenação.
6. **Simulação Determinística de Margem:** Chamada a `/api/v1/pricing/margins/simulate-product` comprovando precisão em `BigDecimal` (escala 4, `HALF_EVEN`) para receita, comissões, frete, impostos e margem de contribuição.
7. **Consulta do Dashboard & Cache Tenant-Aware:** Consulta de KPIs agregados em `/api/v1/analytics/kpis/summary`, `/by-channel` e `/by-period`, verificando cálculo de totais e cache hit seguro.
8. **Exportação Assíncrona & Download Auditado:** Solicitação de exportação em `/api/v1/reports/exports`, geração de arquivo privado com sanitização contra CSV formula injection, consulta de status e download auditado com link temporário assinado.

---

### 1.2 `SystemResilienceAndSecurityIntegrationTest.java` (Cenários de Caos, Resiliência e Segurança)
Testa os 8 cenários críticos de falha exigidos:
1. **Falha de OAuth:** Tentativa de callback com `state` inválido ou divergente e `code_verifier` PKCE corrompido, resultando em rejeição imediata com log de segurança.
2. **API Externa Indisponível (503 / Timeout / Fail-Closed):** Simulação de indisponibilidade ou lentidão extrema na API do marketplace acionando política fail-closed e circuit breaker, sem preencher métricas silenciosamente.
3. **Rate Limiting (429 Too Many Requests):** Resposta HTTP 429 da API externa sendo capturada com respeito ao cabeçalho `Retry-After` e backoff exponencial sem sobrecarregar o canal.
4. **Token Expirado:** Requisições com JWT expirado são terminantemente rejeitadas com HTTP 401 Unauthorized de forma fail-closed sem bypass de segurança.
5. **Retry & Idempotência Contábil:** Reprocessamento repetido do mesmo pedido ou evento financeiro não duplica linhas na base nem corrompe os totais acumulados.
6. **DLQ (Dead Letter Queue):** Cargas ou mensagens corrompidas que falham de forma permanente são roteadas para fila de descarte / auditoria sem travar a esteira principal nem provocar loops infinitos de retry.
7. **Duplicidade de Exportação:** Solicitações de exportação com a mesma chave `idempotencyKey` retornam o job existente se estiver em andamento ou completado dentro do TTL.
8. **Acesso Cross-Tenant (Segurança Estrita):** Tentativa do Tenant Beta de consultar dados, métricas, jobs de exportação ou efetuar downloads de arquivos de propriedade do Tenant Alpha é bloqueada categoricamente com HTTP 403 Forbidden ou 404 Not Found.

---

## 2. Critérios de Aceite Objetivos

1. **Pipeline Reproduzível:** Executável via comando único `./mvnw test -Dtest="EndToEndLifecycleIntegrationTest,SystemResilienceAndSecurityIntegrationTest"`.
2. **Nenhum Cenário Crítico Ignorado:** Todos os 8 cenários de falha e todas as 8 etapas do pipeline executivo contemplados e aprovados.
3. **Isolamento de Tenant em Profundidade:** Comprovado que o Tenant Beta não enxerga dados de catálogo, pedidos, fatos financeiros ou exportações do Tenant Alpha.
4. **Anti-Alucinação Mantido:** Todas as respostas financeiras e analíticas respeitam a regra de não inventar dados e exibem o disclaimer contábil proibindo o rótulo de Lucro Líquido.
