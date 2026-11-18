# Especificação Técnica: P005-BE — Integração Oficial OAuth 2.0 Mercado Livre, PKCE, Cifragem KMS e Rotação Segura

> **Identificador da Feature:** `P005-BE` &middot; **Versão:** 1.0.0  
> **Status da Spec:** Aprovada para Implementação  
> **Requisitos do PRD:** RF-005, RF-006, RF-007, RNF-001, RNF-002, RNF-003, RNF-006, ADR-002, ADR-004  
> **Fonte Oficial:** [Mercado Libre Developers — Autenticação e Autorização](https://developers.mercadolibre.com.br) &middot; Consulta: 18/11/2026

---

## 1. Contexto e Objetivos

O conector de marketplace com o **Mercado Livre** (`MLB`) é o canal prioritário da plataforma. Para garantir conformidade com o programa oficial de desenvolvedores do Mercado Livre e proteção máxima para a conta do seller:
1. **Protocolo:** Implementação estrita do **OAuth 2.0 Authorization Code Flow** reforçado com **PKCE** (Proof Key for Code Exchange — RFC 7636) e parâmetro `state` criptograficamente seguro contra ataques de CSRF e Session Fixation (RFC 6749 Seção 10.12).
2. **Escopos Oficiais de Menor Privilégio:**
   - `read`: Permissão de leitura de dados de perfil do vendedor, anúncios e inventário.
   - `offline_access`: Emissão de `refresh_token` para renovação transparente de sessão sem exigir nova interação do lojista no navegador.
   - *Escopo `write`:* **Expressamente vetado** no MVP.
3. **Cifragem de Tokens (KMS / Secret Manager):**
   - Os tokens de acesso (`access_token`) e de renovação (`refresh_token`) nunca devem residir em texto puro no banco de dados.
   - Utilização do algoritmo criptográfico **AES-256-GCM** (Galois/Counter Mode) com vetor de inicialização (IV) aleatório de 12 bytes gerado por `SecureRandom` e tag de integridade de 128 bits.
   - Chave mestre de criptografia derivada de variável de ambiente (`TOKEN_ENCRYPTION_KEY`), simulando integração de envelope com AWS KMS / GCP Cloud KMS / HashiCorp Vault.
4. **Renovação com Lock Distribuído e Rotação Segura:**
   - O Mercado Livre adota o mecanismo de **Refresh Token Rotation**: cada chamada de refresh invalida imediatamente o `refresh_token` anterior e emite um par novo (`access_token` + novo `refresh_token`).
   - Se duas requisições tentarem renovar o token simultaneamente para o mesmo lojista, a segunda requisição falhará com `invalid_grant` e quebrará a cadeia de autenticação.
   - A renovação deve ser protegida por **lock distribuído/sincronizado** por conexão (`connection_id`), garantindo idempotência e atomicidade na persistência do novo par de tokens.
5. **Revogação Local e Auditoria Sem Segredos:**
   - A revogação limpa e anula localmente as credenciais cifradas, atualizando o status para `REVOKED` ou `DISCONNECTED`.
   - Todos os eventos de ciclo de vida (`OAUTH_AUTHORIZE_INITIATED`, `OAUTH_CALLBACK_SUCCESS`, `TOKEN_REFRESH_SUCCESS`, `TOKEN_REVOCATION_SUCCESS`) devem ser auditados via `AuditService` com dados estritamente sanitizados (zero tokens nos logs).

---

## 2. Cenários BDD / Gherkin

### Cenário 1: Iniciação do Fluxo de Autorização com State Anti-CSRF e PKCE
```gherkin
Dado que um usuário autenticado em um tenant solicita a conexão com o Mercado Livre
Quando o endpoint de iniciação de autorização for acionado
Então o sistema deve gerar um code_verifier de alta entropia (RFC 7636)
E calcular o code_challenge utilizando SHA-256 codificado em Base64URL
E gerar um token de state criptograficamente seguro com expiração delimitada (10 minutos)
E retornar a URL oficial de autorização do Mercado Livre contendo response_type=code, client_id, redirect_uri, state, code_challenge e code_challenge_method=S256
E registrar um evento de auditoria OAUTH_AUTHORIZE_INITIATED sem dados sensíveis
```

### Cenário 2: Rejeição de Callback com State Inválido, Adulterado ou Expirado (Anti-CSRF)
```gherkin
Dado que uma requisição de callback do Mercado Livre chega com um state inexistente, adulterado ou com TTL vencido
Quando o callback for processado
Então o sistema deve rejeitar imediatamente a requisição com erro de segurança (InvalidStateCsrfException / HTTP 400)
E nenhuma troca de código por token deve ser tentada no Mercado Livre
E um evento de auditoria OAUTH_CALLBACK_INVALID_STATE deve ser registrado
```

### Cenário 3: Troca de Authorization Code e Cifragem de Tokens via AES-256-GCM
```gherkin
Dado um authorization code válido e o respectivo code_verifier em sandbox/ambiente autorizado
Quando o callback for validado com sucesso
Então o sistema deve chamar o endpoint /oauth/token do Mercado Livre com grant_type=authorization_code e code_verifier
E receber access_token, refresh_token, expires_in e user_id
E cifrar ambos os tokens com o serviço de criptografia AES-256-GCM antes de gravar no banco de dados
E persistir a conexão no banco de dados com tenant_id do contexto
E nenhum token em texto puro deve ser armazenado em colunas da tabela ou logs
```

### Cenário 4: Renovação Concorrente Sob Lock Distribuído sem Duplicação
```gherkin
Dado uma conexão ativa com o Mercado Livre que necessita de renovação de token
Quando 10 requisições simultâneas solicitarem o refresh_token para a mesma conexão
Então o sistema deve adquirir o lock sincronizado/distribuído
E executar a chamada de renovação ao Mercado Livre exatamente UMA vez
E rotacionar com sucesso o novo refresh_token e novo access_token no banco
E as outras 9 requisições concorrentes não devem disparar chamadas duplicadas ao canal
E nenhum erro de invalid_grant deve ocorrer
```

### Cenário 5: Revogação de Conexão com Invalidação Local e Bloqueio de Novas Chamadas
```gherkin
Dado uma conexão ativa com o Mercado Livre
Quando a revogação da conexão for solicitada pelo seller
Então os tokens cifrados armazenados devem ser expurgados da conexão
E o status da conexão deve transitar para REVOKED
E requisições subsequentes que dependam de credenciais devem ser rejeitadas como não autorizadas
E um evento de auditoria TOKEN_REVOCATION_SUCCESS deve ser emitido
```

### Cenário 6: Auditoria Limpa sem Vazamento de Tokens ou Chaves
```gherkin
Dado qualquer operação de OAuth (iniciação, callback, refresh, revogação)
Quando os registros de auditoria persistidos no banco de dados forem inspecionados
Então nenhum token em texto puro, segredo de cliente ou chave criptográfica deve constar nos campos principal, details ou correlation_id
```

### Cenário 7: Menor Privilégio e Rejeição de Escopos Não Autorizados
```gherkin
Dado a configuração do conector de marketplace
Quando a URL de autorização ou a solicitação de token for montada
Então os escopos solicitados devem ser estritamente "read" e "offline_access"
E qualquer tentativa de incluir escopo de escrita ("write") no MVP deve ser barrada por regra de governança
```
