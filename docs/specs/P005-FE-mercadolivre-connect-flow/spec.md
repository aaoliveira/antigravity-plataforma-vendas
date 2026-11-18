# Especificação Técnica: P005-FE — Fluxo Oficial de Conexão com Mercado Livre (OAuth 2.0 PKCE UI)

> **Identificador da Feature:** `P005-FE` &middot; **Versão:** 1.0.0  
> **Status da Spec:** Aprovada para Implementação  
> **Requisitos do PRD:** RF-005, RF-006, RF-007, RNF-001, RNF-002, RNF-003, RNF-006, ADR-002, ADR-004  
> **Fonte Oficial:** [Mercado Libre Developers — Autenticação Oficial](https://developers.mercadolibre.com.br)

---

## 1. Contexto e Objetivos

O conector com o **Mercado Livre** é o canal prioritário de integração da **Seller Intelligence Platform**.  
Para garantir a máxima confiança do lojista e conformidade inegociável com as diretrizes de segurança:
1. **Nunca Solicitar Senhas:** A aplicação nunca solicita, armazena ou transita a senha do vendedor do Mercado Livre. O login ocorre com exclusividade no domínio oficial `auth.mercadolivre.com.br`.
2. **Explicação Clara de Permissões:** Antes de qualquer redirecionamento, a interface apresenta um modal de consentimento esclarecendo:
   - Escopo `read`: Leitura segura de perfil, anúncios próprios e inventário.
   - Escopo `offline_access`: Renovação automática de sessão em background sem exigir login contínuo.
   - **Garantia de Não-Escrita:** Aviso explícito de que a plataforma **não possui permissão de escrita** (`write`), impedindo qualquer alteração acidental de preços ou títulos de anúncios.
3. **Redirecionamento Oficial:** O backend gera a URL oficial contendo `code_challenge` (PKCE S256) e `state` anti-CSRF criptográfico.
4. **Tratamento de Callback sem Vazamento de Tokens:**
   - O callback recebe `code` e `state` na URL, despacha para o backend via POST seguro e limpa imediatamente a URL via `window.history.replaceState`.
   - O backend cifra os tokens com AES-256-GCM e retorna apenas o DTO de resumo da conexão (`ConnectionSummaryResponse`).
   - Nenhum token sensível é exposto ao cliente, ao DOM ou ao `localStorage`.
5. **Feedback Compreensível:** Mensagens claras em caso de cancelamento pelo lojista, state anti-CSRF inválido ou falhas de comunicação.
6. **Responsividade & Acessibilidade:** Conformidade WCAG 2.2 AA, touch targets $\ge 44\text{px}$, suporte a leitores de tela e usabilidade mobile fluida (360px+).

---

## 2. Cenários BDD / Gherkin

### Cenário 1: Apresentação de Permissões e Garantia de Não-Escrita
```gherkin
Dado que o lojista está no módulo "Lojas & Marketplaces" e clica em "Conectar Oficialmente" no Mercado Livre
Quando o modal de conexão for aberto
Então a tela deve listar os escopos solicitados ("read" e "offline_access") com explicações em linguagem clara
E deve exibir um selo de segurança garantindo que a senha nunca é solicitada
E deve destacar que a permissão de alteração de preços e anúncios ("write") NÃO é solicitada
```

### Cenário 2: Redirecionamento para o Domínio Oficial do Mercado Livre
```gherkin
Dado que o lojista confirma a conexão no modal de consentimento
Quando o sistema acionar o endpoint de iniciação OAuth
Então o backend deve retornar a URL oficial contendo https://auth.mercadolivre.com.br/authorization
E a interface deve redirecionar o navegador para essa URL com os parâmetros de state e code_challenge
```

### Cenário 3: Processamento do Callback sem Exposição de Tokens no DOM
```gherkin
Dado que o Mercado Livre redireciona o vendedor de volta com ?code=TG-...&state=...
Quando a página de callback processar a resposta
Então o código e o state devem ser enviados ao backend para validação e cifragem
E a URL deve ser limpa imediatamente para não deixar o authorization code no histórico do navegador
E o resumo retornado pelo backend NÃO DEVE conter access_token ou refresh_token em nenhum nó HTML ou atributo
```

### Cenário 4: Atualização de Status da Conexão
```gherkin
Dado que o callback foi concluído com sucesso
Quando o lojista for redirecionado de volta ao painel de integrações
Então o status do Mercado Livre deve exibir "Conectado" (ACTIVE) com indicador verde
E a data da última verificação e os escopos ativos devem ser refletidos com exatidão
```

### Cenário 5: Tratamento de Falhas e Rejeição de State CSRF
```gherkin
Dado que ocorre uma falha no callback (ex.: o usuário negou permissão no Mercado Livre ou o state expirou)
Quando a falha for interceptada
Então a interface deve exibir um alerta compreensível explicando a razão do erro
E deve orientar o usuário a tentar novamente sem expor stacktraces técnicos
```
