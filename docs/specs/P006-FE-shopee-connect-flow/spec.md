# Especificação Técnica: P006-FE — Fluxo Oficial de Conexão com Shopee Open Platform v2

> **Identificador da Feature:** `P006-FE` &middot; **Versão:** 1.0.0  
> **Status da Spec:** Aprovada para Implementação  
> **Requisitos do PRD:** RF-005, RF-006, RF-007, RNF-001, RNF-002, RNF-003, RNF-006, ADR-002, ADR-004  
> **Fonte Oficial:** [Shopee Open Platform — OpenAPI v2](https://open.shopee.com)

---

## 1. Contexto e Objetivos

O conector com a **Shopee** é o canal homologado para monitoramento e análise de lojas de e-commerce na plataforma Shopee via OpenAPI v2.  
Para garantir conformidade inegociável com a segurança, políticas da plataforma e proteção do lojista:

1. **Nunca Solicitar Credenciais da Conta ao Usuário:**  
   A aplicação **jamais** solicita, manipula ou armazena email, senha, chaves de API (`partner_id`, `partner_key`) ou credenciais da conta do lojista. O login e a concessão de consentimento ocorrem exclusivamente na página oficial da Shopee Open Platform.
2. **Orientações de Autorização Claras e Transparentes:**  
   Antes de redirecionar o usuário, a interface exibe o modal de consentimento (`ShopeeConnectModal.tsx`) informando:
   - Necessidade de possuir uma loja ativa na Shopee (Brasil ou internacional).
   - Explicação sobre a assinatura criptográfica HMAC-SHA256 gerada pelo backend da aplicação para autenticar a requisição perante a Shopee.
   - Escopos concedidos em nível de loja (*Shop Level*): leitura de catálogo próprio (`v2.product.get_item_list`), leitura de pedidos (`v2.order.get_order_list`) e métricas oficiais (`v2.analytics`).
   - **Garantia de Risco Zero:** Alerta ostensivo de que a aplicação atua exclusivamente em modo de leitura e análise, **sem permissões de escrita/alteração** de preços, estoque ou títulos de anúncios.
   - **Mecanismo de Webhook Push em Tempo Real:** Informação de que a conexão é sincronizada automaticamente com a Shopee; cancelamentos de autorização feitos no portal da Shopee são processados imediatamente via webhook push (*Push Code 2*), revogando a conexão local e expurgando tokens.
3. **Redirecionamento Oficial com Assinatura Criptográfica:**  
   O frontend aciona a rota BFF `POST /api/integrations/shopee/authorize`, que solicita ao backend a URL assinada via HMAC-SHA256 (`partner_id`, `timestamp`, `sign`, `redirect`), redirecionando o navegador para o domínio oficial da Shopee.
4. **Tratamento de Callback sem Exposição de Segredos:**  
   - Ao receber o retorno da Shopee contendo `code` e `shop_id` (ou cancelamento via `error`), a interface limpa imediatamente a URL via `window.history.replaceState`.
   - O código de autorização e o ID da loja são despachados via `POST /api/integrations/shopee/callback` para troca por tokens cifrados com AES-256-GCM no backend.
   - Nenhum token sensível (`access_token`, `refresh_token`, chave simétrica) é exposto ao DOM ou gravado em storage do cliente.
5. **Estado da Conexão Consistente e Orientações:**  
   - Exibição consistente do status (`ACTIVE`, `DISCONNECTED`, `REVOKED`) e da data de última sincronização.
   - Badge informativo sobre o status dos Webhooks Push em tempo real.
6. **Erros Sanitizados:**  
   Falhas de autorização, expiração de código ou cancelamentos pelo usuário são apresentados de forma compreensível e amigável, sem vazar stacktraces Java, exceções ou UUIDs técnicos.
7. **Responsividade & Acessibilidade:**  
   Conformidade WCAG 2.2 AA, touch targets $\ge 44\text{px}$, suporte a navegação por teclado (ESC) e usabilidade mobile fluida (360px+).

---

## 2. Cenários BDD / Gherkin

### Cenário 1: Apresentação de Orientações de Autorização e Garantia de Zero Credenciais
```gherkin
Dado que o lojista está na aba da Shopee no módulo "Lojas & Marketplaces" e clica em "Conectar Oficialmente"
Quando o modal de conexão com a Shopee for aberto
Então a tela deve exibir orientações claras sobre os requisitos de autorização oficial Shopee OpenAPI v2
E deve assegurar ostensivamente que NENHUMA senha, login ou chave da Shopee é solicitada nesta aplicação
E deve listar os acessos somente-leitura solicitados (catálogo próprio, pedidos e métricas)
E deve destacar a Garantia de Risco Zero informando que a plataforma não altera produtos nem preços
```

### Cenário 2: Redirecionamento para a URL Oficial Assinada da Shopee
```gherkin
Dado que o lojista confirma a autorização no modal da Shopee
Quando a interface acionar a iniciação de autorização
Então o BFF deve retornar a URL oficial assinada com partner_id, timestamp e sign HMAC-SHA256
E a interface deve redirecionar o navegador para a página oficial da Shopee
```

### Cenário 3: Processamento do Callback Oficial Shopee e Sanitização da URL
```gherkin
Dado que a Shopee redireciona o vendedor de volta contendo ?code=shopee_auth_code_123&shop_id=987654321
Quando a página de integrações for carregada
Então a URL deve ser limpa imediatamente no histórico via window.history.replaceState
E os parâmetros code e shop_id devem ser enviados via POST para /api/integrations/shopee/callback
E o status do canal Shopee deve ser atualizado para "Conectado" (ACTIVE) com feedback de sucesso
E nenhum token, código ou segredo deve ser exposto no DOM ou nos atributos HTML
```

### Cenário 4: Tratamento de Cancelamento e Erros Sanitizados
```gherkin
Dado que o lojista cancela a autorização na Shopee ou ocorre um erro com ?error=cancelled
Quando o retorno for interceptado
Então a URL deve ser limpa imediatamente
E um alerta amigável deve informar que a autorização foi cancelada pelo usuário sem alterações na conta
E nenhuma mensagem com stacktrace, exceção de runtime ou segredo técnico deve ser exibida
```

### Cenário 5: Responsividade Mobile e Acessibilidade (WCAG 2.2 AA)
```gherkin
Dado que o lojista abre o modal de conexão da Shopee em qualquer resolução (360px a 1440px)
Quando inspecionar os elementos interativos
Então todos os botões de ação e fechar devem possuir altura mínima de 44px
E o fechamento do modal deve funcionar ao pressionar a tecla ESC ou clicar no botão fechar
```
