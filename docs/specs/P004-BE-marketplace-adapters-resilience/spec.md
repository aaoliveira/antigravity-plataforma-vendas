# Especificação Técnica: P004-BE — Contrato MarketplaceAdapter, Registro de Capacidades, Evidências e Camada de Resiliência

> **Identificador da Feature:** `P004-BE` &middot; **Versão:** 1.0.0  
> **Status da Spec:** Aprovada para Implementação  
> **Requisitos do PRD:** RF-005, RF-006, RF-007, RF-008, RF-009, RNF-001, RNF-003, RNF-006, ADR-003, ADR-004  
> **Padrão Arquitetural:** Resilient Gateway / Adapter Pattern com Defesa em Profundidade, Fail-Closed e Respeito Estrito a Políticas de Marketplaces

---

## 1. Contexto e Objetivo

O módulo de **Marketplace Adapters** é o ponto único de contato entre o monólito modular da **Seller Intelligence Platform** e os canais externos de comércio eletrônico (Mercado Livre, Shopee, Amazon, etc.).  
Para garantir o **risco zero para as contas dos vendedores**, a plataforma adota o princípio de **Fail-Closed**:
1. Nenhuma chamada externa é permitida sem validação prévia de capacidade homologada (`SUPPORTED`).
2. Se uma capacidade não possuir evidência documental oficial ou estiver marcada como `UNVERIFIED` ou `UNSUPPORTED`, a chamada é **bloqueada imediatamente**.
3. Toda comunicação externa deve ser protegida por uma **esteira de resiliência multicamadas**:
   - **Rate Limiting** (controle de vazão por token/canal);
   - **Timeout** (limite rígido de tempo por requisição);
   - **Retry com Full Jitter** e respeito obrigatório ao cabeçalho `Retry-After`;
   - **Circuit Breaker** (estados `CLOSED`, `OPEN`, `HALF_OPEN`);
   - **Bulkhead** (limitação estrita de chamadas concorrentes);
   - **Idempotência** (prevenção contra duplicidade de operações);
   - **Kill Switch** (desligamento instantâneo por canal ou geral);
4. Logs nunca devem registrar segredos, tokens ou dados sensíveis em texto puro.
5. É expressamente proibido qualquer mecanismo de scraping ou tentativa de adivinhar endpoints não documentados.

---

## 2. Escopo Obrigatório

1. **Estados de Capacidade (`CapabilityStatus`):**
   - `SUPPORTED`: Oficialmente suportada e homologada pela API do marketplace.
   - `UNSUPPORTED`: Não disponibilizada oficialmente pelo marketplace (ex: volume de vendas de concorrentes).
   - `RESTRICTED`: Sujeita a termos específicos ou cotas severas (ex: operações de escrita desabilitadas no MVP).
   - `UNVERIFIED`: Não homologada formalmente com documentação oficial ou sem evidência registrada. Não executa.

2. **Cadastro de Evidência por Capacidade (`CapabilityEvidence`):**
   - URL oficial da documentação.
   - Data de verificação.
   - Escopos OAuth necessários.
   - Limites de taxa e quotas oficiais.
   - Restrições técnicas e contratuais.
   - Versão do documento/API consultado.
   - Engenheiro responsável pela homologação.

3. **Contrato do Adaptador (`MarketplaceAdapter`):**
   - Identificação do canal (`getMarketplaceId()`).
   - Registro de capacidades (`getCapabilityRegistry()`).
   - Execução de operações resilientes (`execute(operation, context)`).
   - Verificação de conectividade / saúde (`isHealthy()`).

4. **Esteira de Resiliência (`ResiliencePipeline`):**
   - **Kill Switch:** Chave geral e por canal para interrupção imediata.
   - **Validador de Capacidade:** Bloqueia operações `UNVERIFIED`, `UNSUPPORTED` ou `RESTRICTED` desabilitadas.
   - **Rate Limiter:** Controle determinístico de chamadas por segundo.
   - **Bulkhead:** Semáforo de concorrência máxima por canal.
   - **Circuit Breaker:** Transições atômicas entre `CLOSED`, `OPEN` e `HALF_OPEN`.
   - **Retry Policy com Full Jitter:** Fórmula $\text{Sleep} = \text{random}(0, \min(\text{maxDelay}, \text{baseDelay} \times 2^{\text{attempt}}))$, integrando respeito ao tempo do cabeçalho `Retry-After`.
   - **Timeout:** Interrupção forçada com liberação de recursos após tempo limite.
   - **Idempotência:** Deduplicação por chave única da operação.

5. **Adaptador Fake para Testes (`FakeMarketplaceAdapter`):**
   - Implementação utilizada para testes unitários e de integração sem tocar na rede externa.
   - Capacidade de simular sucessos, erros HTTP 429 com `Retry-After`, falhas consecutivas, timeouts e validações de kill switch.

6. **Sanitização de Logs:**
   - Logs de operações mascaram tokens de acesso, refresh tokens e credenciais com `[REDACTED]`.

---

## 3. Não-Escopo

- Scraping hostil de páginas HTML de concorrentes.
- Operações de escrita em marketplaces (anunciar produtos, alterar preços, aceitar ofertas).
- Conexão em tempo real aos servidores de produção do Mercado Livre / Shopee neste prompt (escopo dos conectores concretos subsequentes).

---

## 4. Cenários Gherkin de Validação

```gherkin
Funcionalidade: Contrato MarketplaceAdapter e Camada de Resiliência

  Cenário: Tentativa de executar capacidade sem evidência ou UNVERIFIED
    Dado que a capacidade "COMPETITOR_SALES_VOLUME" está marcada como "UNVERIFIED"
    Quando uma operação requisita a execução desta capacidade no adaptador
    Então o sistema deve lançar UnverifiedCapabilityException
    E a chamada externa nunca deve ser realizada

  Cenário: Ativação de Kill Switch
    Dado que o Kill Switch do canal "MERCADO_LIVRE" foi acionado
    Quando qualquer operação for solicitada para "MERCADO_LIVRE"
    Então o sistema deve lançar KillSwitchActivatedException imediatamente
    E o circuito de chamada deve falhar fechado

  Cenário: Respeito a Rate Limiting e Retry-After com Full Jitter
    Dado que uma chamada externa retorna status HTTP 429 com cabeçalho "Retry-After: 2"
    Quando a política de retry for executada
    Então o adaptador deve respeitar o tempo mínimo indicado
    E o número de retentativas não deve ultrapassar o limite configurado

  Cenário: Abertura de Circuit Breaker após falhas consecutivas
    Dado que o limiar de falhas do Circuit Breaker é de 5 erros consecutivos
    Quando o canal falha 5 vezes seguidas
    Então o estado do Circuit Breaker deve mudar para "OPEN"
    E a 6ª chamada deve falhar imediatamente com CircuitBreakerOpenException sem tocar no canal

  Cenário: Isolamento de concorrência via Bulkhead
    Dado que a capacidade máxima do Bulkhead é de 2 chamadas simultâneas
    Quando 3 chamadas concorrentes disputam execução
    Então a 3ª chamada deve ser rejeitada com BulkheadFullException

  Cenário: Mascaramento de dados sensíveis nos logs do adaptador
    Dado que a requisição contém um access_token "APP_USR_secret_12345"
    Quando o adaptador emitir logs da execução
    Então o token deve ser mascarado para "[REDACTED]"
```

---

## 5. Matriz de Rastreabilidade

| Requisito PRD | Descrição | Componente Técnico | Teste Automatizado |
|---|---|---|---|
| **RF-005** | Integração oficial com APIs | `MarketplaceAdapter`, `FakeMarketplaceAdapter` | `MarketplaceAdapterResilienceTest` |
| **RF-006** | Matriz de capacidades e evidências | `CapabilityRegistry`, `CapabilityEvidence` | `CapabilityRegistryTest` |
| **RF-007** | Resiliência, Rate Limiting e Circuit Breaker | `ResiliencePipeline`, `CircuitBreaker`, `RateLimiter` | `MarketplaceAdapterResilienceTest` |
| **RF-008** | Retry com Full Jitter e Retry-After | `RetryPolicy` | `MarketplaceAdapterResilienceTest` |
| **RF-009** | Kill switch e isolamento Fail-Closed | `KillSwitch`, `Bulkhead` | `MarketplaceAdapterResilienceTest` |
| **RNF-001** | OWASP ASVS (Sanitização de logs) | Logs do adaptador, `AuditSanitizer` | `MarketplaceAdapterResilienceTest` |
| **RNF-003** | Idempotência em operações | `IdempotencyManager` | `MarketplaceAdapterResilienceTest` |
