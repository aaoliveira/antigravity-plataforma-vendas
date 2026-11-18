# Especificação Técnica: P004-FE — Interface de Gestão de Conexões Oficiais, Capacidades e Evidências

> **Identificador da Feature:** `P004-FE` &middot; **Versão:** 1.0.0  
> **Status da Spec:** Aprovada para Implementação  
> **Requisitos do PRD:** RF-005, RF-006, RF-007, RF-008, RF-009, RNF-001, RNF-003, RNF-006, ADR-003, ADR-004  
> **Padrão de UX:** Interface Transparente de Resiliência e Conformidade Oficial com Fail-Closed

---

## 1. Contexto e Objetivo

No módulo de **Gestão de Lojas & Marketplaces** (`STORES`), o seller gerencia as conexões oficiais OAuth (Mercado Livre, Shopee) e visualiza a transparência completa das capacidades homologadas, restrições contratuais, datas de verificação e políticas de resiliência.

Para manter a governança inegociável do projeto:
1. **Transparência de Capacidades:** Cada capacidade de marketplace possui status visual explícito e inconfundível:
   - `SUPPORTED`: Oficialmente homologada com documentação e cotas de API.
   - `RESTRICTED`: Sujeita a limites severos ou desabilitada no MVP (ex: operações de escrita bloqueadas para garantir risco zero de alterações indevidas).
   - `UNSUPPORTED`: Proibida ou indisponível oficialmente pelo canal (ex: estimativa ou scraping de faturamento de concorrentes), com explicação direta do motivo.
   - `UNVERIFIED`: Em fase de análise documental. **Nunca apresentada como disponível**.
2. **Segurança de Segredos:** Nenhum token (`access_token`, `refresh_token`, `client_secret`) é renderizado no DOM ou em atributos HTML.
3. **Sanitização de Erros:** Qualquer falha técnica é sanitizada via `sanitizeErrorMessage()`, sem expor identificadores internos (UUIDs) ou dados de infraestrutura.
4. **Ações Permitidas Restritas:** As únicas interações autorizadas para os canais são:
   - **Conectar**: Inicia o fluxo de autorização oficial OAuth.
   - **Atualizar Status**: Consulta o estado de saúde e quotas ativas.
   - **Revogar**: Desconecta o canal e invalida localmente as credenciais armazenadas de forma segura.
5. **Responsividade & Acessibilidade:** Conformidade WCAG 2.2 AA, touch targets $\ge 44\text{px}$, sem overflow horizontal e usabilidade em telas móveis a partir de 360px.

---

## 2. Cenários BDD / Gherkin

### Cenário 1: Distinção Clara dos Quatro Estados de Capacidade
```gherkin
Dado que o usuário acessa o módulo "Lojas & Marketplaces"
Quando a matriz de capacidades do Mercado Livre ou Shopee é renderizada
Então os 4 estados (SUPPORTED, RESTRICTED, UNSUPPORTED, UNVERIFIED) devem possuir elementos visuais e badges semanticamente distinguíveis
E as capacidades UNSUPPORTED devem exibir justificativa clara sobre ausência na API oficial e proibição de scraping
```

### Cenário 2: Bloqueio de Capacidades Não Validadas (UNVERIFIED)
```gherkin
Dado que um canal ou capacidade está marcado como UNVERIFIED (ex: Amazon SP-API ou TikTok Shop)
Quando o componente for renderizado
Então o status exibido deve ser "Em Validação Técnica" ou "Não Homologado"
E o sistema NÃO DEVE apresentar a capacidade como disponível ou executável
E não devem existir botões de acionamento de recursos para capacidades UNVERIFIED
```

### Cenário 3: Ausência Absoluta de Tokens Sensíveis no DOM
```gherkin
Dado que existem integrações ativas ou em conexão
Quando o HTML da página for inspecionado no DOM ou atributos de elementos
Então nenhum token no padrão Bearer, JWT, refresh_token ou client_secret deve estar presente em texto ou atributo
```

### Cenário 4: Ações Permitidas Exclusivas
```gherkin
Dado um canal de marketplace exibido na tela
Quando o usuário interagir com as ações disponíveis
Então as únicas ações permitidas devem ser "Conectar", "Atualizar Status" e "Revogar"
E não devem existir ações de escrita em anúncios, alteração de preços ou scraping de terceiros
```

### Cenário 5: Erros Sanitizados e Acessíveis
```gherkin
Dado que ocorre uma falha temporária de comunicação com a API de integração
Quando a mensagem de erro for exibida na tela
Então a mensagem deve ser processada por sanitizeErrorMessage
E nenhum UUID técnico de tenant ou stacktrace deve ser exposto ao usuário
```

### Cenário 6: Usabilidade Mobile e Acessibilidade
```gherkin
Dado um dispositivo móvel com largura de tela de 360px
Quando o usuário navega pelo painel de integrações
Então o layout não deve apresentar overflow horizontal
E todos os botões devem respeitar a área de toque mínima de 44px
E os componentes devem conter atributos ARIA e tags semânticas adequadas
```
