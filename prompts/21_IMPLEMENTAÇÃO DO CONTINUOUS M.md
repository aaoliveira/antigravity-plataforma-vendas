# PROMPT — IMPLEMENTAÇÃO DO CONTINUOUS MARKETPLACE COMPLIANCE & POLICY MONITORING

## 1. PAPEL E RESPONSABILIDADE

Atue como Staff Software Engineer, Software Architect, Application Security Engineer e especialista em Marketplace Compliance Engineering.

Você será responsável por projetar e implementar um mecanismo permanente de monitoramento, avaliação e aplicação de conformidade das integrações da Seller Intelligence Platform com marketplaces.

Sua missão é garantir que a plataforma acompanhe continuamente mudanças nas políticas, termos de utilização, documentação técnica, APIs, permissões, limites operacionais e requisitos de segurança dos marketplaces integrados.

A conformidade deverá ser tratada como uma propriedade arquitetural permanente, integrada ao ciclo de vida da aplicação.

Não implemente apenas um agendador que consulta páginas periodicamente.

Implemente um mecanismo completo de governança de conformidade, com:

- Monitoramento contínuo.
- Versionamento de políticas.
- Histórico de alterações.
- Detecção de mudanças.
- Análise de impacto.
- Avaliação de riscos.
- Bloqueio preventivo.
- Revalidação de permissões.
- Notificações.
- Auditoria.
- Aprovação controlada.
- Recuperação segura das funcionalidades.

---

# 2. CONTEXTO DO PROJETO

A Seller Intelligence Platform é uma plataforma SaaS de inteligência de mercado, gestão e otimização de operações de e-commerce e marketplaces.

A solução possui integrações atuais ou planejadas com:

1. Mercado Livre.
2. Shopee.
3. Amazon.
4. TikTok Shop.
5. Magalu.
6. SHEIN.

A plataforma oferece funcionalidades de:

- Análise de concorrência.
- Pesquisa de produtos.
- Monitoramento de anúncios.
- Monitoramento de preços.
- Análise de categorias.
- Análise de vendedores.
- Criação de anúncios.
- Otimização de anúncios com IA.
- Gestão financeira.
- Sincronização de pedidos.
- Gestão de campanhas.
- Integrações com APIs.
- Extensões de navegador.
- Assistente de inteligência artificial.
- Integrações MCP.

O sistema já possui ou deverá possuir o módulo:

`Marketplace Compliance Guard`

Este módulo é responsável por impedir operações que não atendam aos requisitos de autorização e conformidade.

**Sua implementação deverá ampliar esse mecanismo, criando uma camada permanente de governança denominada:**

`Continuous Marketplace Compliance Engine`

---

# 3. OBJETIVO FUNCIONAL

Implementar um mecanismo capaz de responder continuamente às seguintes perguntas:

1. As políticas do marketplace continuam válidas?
2. A documentação da API sofreu alterações?
3. Alguma permissão foi modificada ou revogada?
4. Algum endpoint deixou de ser suportado?
5. Algum limite de requisições foi alterado?
6. Alguma operação passou a exigir autorização adicional?
7. Alguma funcionalidade da plataforma pode ter se tornado incompatível?
8. Existe risco de bloqueio ou restrição das contas conectadas?
9. Alguma integração precisa ser temporariamente suspensa?
10. Existe alguma atualização que exige intervenção humana?

Quando uma alteração relevante for identificada, o sistema deverá avaliar seu impacto e aplicar medidas preventivas.

---

# 4. MÓDULO 01 — MARKETPLACE POLICY REGISTRY

Crie um repositório centralizado de políticas e requisitos de conformidade.

Cada marketplace deverá possuir seu próprio registro.

Marketplaces:

- Mercado Livre.
- Shopee.
- Amazon.
- TikTok Shop.
- Magalu.
- SHEIN.

Cada registro deverá armazenar:

| Campo           | Descrição                             |
| --------------- | ------------------------------------- |
| id              | Identificador da política             |
| marketplace     | Marketplace relacionado               |
| policy_type     | Tipo de política                      |
| policy_name     | Nome da política                      |
| official_url    | URL oficial                           |
| source_type     | Documentação, API, termos ou política |
| policy_version  | Versão identificada                   |
| effective_date  | Data de vigência                      |
| last_checked_at | Última verificação                    |
| last_changed_at | Última alteração identificada         |
| content_hash    | Hash do conteúdo analisado            |
| status          | Estado da política                    |
| risk_level      | Nível de risco                        |
| evidence        | Evidências coletadas                  |

Tipos de políticas:

- API_POLICY.
- TERMS_OF_SERVICE.
- DEVELOPER_POLICY.
- AUTHENTICATION_POLICY.
- RATE_LIMIT_POLICY.
- DATA_USAGE_POLICY.
- PRIVACY_POLICY.
- AUTOMATION_POLICY.
- EXTENSION_POLICY.
- WEBHOOK_POLICY.
- CONTENT_POLICY.
- SECURITY_POLICY.

Regras:

1. Utilize somente fontes oficiais sempre que disponíveis.
2. Não invente URLs.
3. Não invente versões.
4. Não presuma que uma política permanece válida sem verificação.
5. Não considere páginas inacessíveis como confirmação de conformidade.
6. Registre a data da última verificação.
7. Mantenha o histórico das versões.
8. Preserve as evidências utilizadas nas avaliações.

Caso uma fonte não possa ser verificada, marque seu estado como `SOURCE_UNAVAILABLE`.

---

# 5. MÓDULO 02 — POLICY MONITORING ENGINE

Implemente um mecanismo responsável por verificar periodicamente as fontes oficiais.

Ele deverá:

- Consultar as fontes cadastradas.
- Identificar alterações de conteúdo.
- Comparar versões anteriores.
- Identificar alterações em endpoints.
- Identificar alterações de parâmetros.
- Identificar mudanças de permissões.
- Identificar alterações de limites.
- Identificar mudanças nas regras de autenticação.
- Detectar remoção de funcionalidades.
- Identificar mudanças nas políticas de utilização de dados.

A implementação deverá utilizar mecanismos legítimos de acesso às fontes.

Priorize:

1. APIs oficiais de documentação, quando disponíveis.
2. Feeds oficiais de atualização.
3. Changelogs oficiais.
4. Documentação oficial versionada.
5. Páginas oficiais publicamente acessíveis, quando a consulta automatizada for permitida.

Não utilize técnicas de evasão de mecanismos anti-bot, CAPTCHA bypass ou métodos que violem as condições de acesso às fontes.

Quando não existir um mecanismo automatizado autorizado, disponibilize um processo de revisão manual.

## Frequência de monitoramento

Implemente frequências configuráveis por tipo de fonte.

Utilize inicialmente:

| Tipo de fonte                | Frequência sugerida |
| ---------------------------- | ------------------- |
| Changelog de APIs            | Diária              |
| Documentação de APIs         | Diária              |
| Políticas de desenvolvedores | Diária              |
| Termos de utilização         | Diária              |
| Políticas de automação       | Diária              |
| Políticas de privacidade     | Semanal             |
| Revisão manual especializada | Mensal              |

Essas frequências são parâmetros operacionais iniciais, não limites ou exigências definidos pelos marketplaces.

Permita ajustar a frequência conforme criticidade, disponibilidade da fonte e autorização para consulta.

---

# 6. MÓDULO 03 — POLICY CHANGE DETECTION

Implemente um mecanismo para detectar alterações nas políticas.

A detecção deverá considerar:

- Inclusão de novas regras.
- Remoção de regras.
- Alteração de permissões.
- Alteração de escopos OAuth.
- Alteração de limites de requisições.
- Alteração de endpoints.
- Depreciação de APIs.
- Alteração de formatos de payload.
- Alteração de requisitos de autenticação.
- Alteração de regras de armazenamento.
- Alteração de regras de utilização comercial.
- Alteração de regras de automação.

Para cada alteração identificada, gere um registro:

`MarketplacePolicyChange`

Campos mínimos:

- Identificador.
- Marketplace.
- Fonte.
- Tipo de alteração.
- Conteúdo anterior.
- Conteúdo atual.
- Data de detecção.
- Data de vigência, quando identificada.
- Evidências.
- Impacto preliminar.
- Nível de risco.
- Status de análise.

Não considere uma simples mudança de formatação de página como alteração de política.

Utilize comparação semântica quando necessário, preservando o conteúdo original como evidência.

Quando a análise automática não conseguir determinar o significado de uma alteração, encaminhe para revisão humana.

---

# 7. MÓDULO 04 — COMPLIANCE IMPACT ANALYZER

Este módulo deverá identificar quais funcionalidades da Seller Intelligence Platform podem ser afetadas por uma mudança de política.

Para isso, implemente rastreabilidade entre:

- Marketplace.
- Política.
- API.
- Endpoint.
- Permissão.
- Conector.
- Serviço de domínio.
- Funcionalidade.
- Regra de negócio.
- Testes automatizados.

Exemplo:

Uma alteração no limite de requisições do Mercado Livre deverá identificar quais componentes utilizam o endpoint afetado.

O sistema deverá determinar:

1. Qual integração foi afetada?
2. Quais operações utilizam o endpoint?
3. Quais organizações utilizam essas operações?
4. Qual é o impacto potencial?
5. Qual é o risco para as contas?
6. Qual medida preventiva deverá ser aplicada?

Crie um mapa de dependências entre políticas e funcionalidades.

Nenhuma alteração relevante deverá ficar sem uma avaliação de impacto.

---

# 8. MÓDULO 05 — RISK ASSESSMENT ENGINE

Implemente um motor de avaliação de riscos.

Classifique os riscos em:

`CRITICAL`

Existe possibilidade concreta de violação grave, comprometimento de contas ou execução de operação proibida.

`HIGH`

Existe risco elevado de restrição, suspensão ou execução incompatível com políticas.

`MEDIUM`

Existe alteração relevante que exige revisão e possível adequação.

`LOW`

Existe mudança de baixo impacto operacional.

`INFORMATIONAL`

Alteração sem impacto relevante identificado.

A classificação deverá considerar:

- Gravidade da alteração.
- Probabilidade de violação.
- Quantidade de funcionalidades afetadas.
- Quantidade de marketplaces afetados.
- Tipo de operação.
- Existência de autorização.
- Data de vigência.
- Confiabilidade da evidência.

Não utilize somente inteligência artificial para determinar se uma operação é permitida.

A IA poderá auxiliar na interpretação e classificação preliminar.

A decisão deverá utilizar regras determinísticas, evidências documentais e, quando necessário, aprovação humana.

---

# 9. MÓDULO 06 — AUTOMATIC COMPLIANCE ENFORCEMENT

Este é um dos componentes mais importantes.

Implemente um mecanismo para aplicar automaticamente medidas preventivas.

Quando uma operação deixar de possuir autorização comprovada, o sistema deverá impedir sua execução.

Estados possíveis:

- `ACTIVE`
- `UNDER_REVIEW`
- `RESTRICTED`
- `SUSPENDED`
- `BLOCKED`
- `PENDING_VALIDATION`
- `APPROVED_FOR_REACTIVATION`

Regras:

### Risco CRITICAL

- Suspender imediatamente a funcionalidade afetada.
- Interromper novas operações relacionadas.
- Bloquear a execução no backend.
- Registrar o motivo.
- Notificar os administradores.
- Abrir incidente de compliance.

### Risco HIGH

- Restringir a funcionalidade afetada.
- Exigir análise de conformidade.
- Impedir novas operações potencialmente incompatíveis.
- Registrar a decisão.
- Notificar os responsáveis.

### Risco MEDIUM

- Abrir revisão obrigatória.
- Identificar funcionalidades afetadas.
- Definir prazo de adequação.
- Monitorar a situação.

### Risco LOW ou INFORMATIONAL

- Registrar a alteração.
- Manter monitoramento.
- Notificar quando necessário.

**REGRA FUNDAMENTAL:**

Não suspenda indiscriminadamente um marketplace inteiro quando somente uma operação específica estiver afetada.

O bloqueio deverá respeitar o menor escopo necessário.

Exemplo:

Se uma alteração afetar apenas a coleta de determinada métrica, suspenda essa coleta sem interromper a sincronização autorizada de pedidos.

Quando não for possível determinar com segurança o escopo afetado, aplique uma restrição conservadora e encaminhe para revisão.

---

# 10. MÓDULO 07 — PERMISSION & AUTHORIZATION VALIDATOR

Implemente um mecanismo para verificar continuamente se as permissões utilizadas pelas integrações continuam válidas.

Verifique:

- Escopos OAuth.
- Validade dos tokens.
- Estado da autorização.
- Permissões concedidas.
- Permissões revogadas.
- Alterações de escopo.
- Permissões exigidas pelas operações.
- Compatibilidade entre permissões e funcionalidades.

O sistema deverá impedir operações quando:

- O token estiver expirado.
- A autorização tiver sido revogada.
- O escopo necessário não estiver presente.
- A operação não estiver documentada.
- A permissão necessária não estiver confirmada.
- A autorização da aplicação estiver suspensa.

Não execute operações adicionais para testar permissões sem autorização.

Utilize mecanismos oficiais de introspecção, renovação ou consulta quando disponibilizados pelo marketplace.

---

# 11. MÓDULO 08 — API DEPRECATION & VERSION MONITORING

Implemente monitoramento de ciclo de vida das APIs.

O sistema deverá identificar:

- Novas versões.
- Versões depreciadas.
- Endpoints descontinuados.
- Alterações incompatíveis.
- Mudanças em parâmetros.
- Mudanças em respostas.
- Novos requisitos de autenticação.
- Alterações de rate limit.
- Datas de descontinuação.

Crie um registro de versões por marketplace.

Exemplo:

| Marketplace   | API            | Versão             | Status       |
| ------------- | -------------- | ------------------ | ------------ |
| Mercado Livre | API de pedidos | Versão documentada | ACTIVE       |
| Shopee        | Open API       | Versão documentada | ACTIVE       |
| Amazon        | SP-API         | Versão documentada | UNDER_REVIEW |

Os valores deverão ser preenchidos com dados reais obtidos nas documentações oficiais.

Não invente versões.

Quando uma versão estiver próxima da descontinuação:

1. Criar alerta.
2. Identificar os componentes afetados.
3. Abrir tarefa de atualização.
4. Definir responsável.
5. Criar plano de migração.
6. Validar a nova versão em ambiente de testes.
7. Liberar a atualização somente após aprovação.

---

# 12. MÓDULO 09 — COMPLIANCE INCIDENT MANAGEMENT

Implemente uma central de incidentes.

Cada incidente deverá possuir:

- Identificador.
- Marketplace.
- Política relacionada.
- Alteração detectada.
- Severidade.
- Funcionalidades afetadas.
- Data de abertura.
- Responsável.
- Descrição.
- Evidências.
- Plano de correção.
- Status.
- Data de resolução.

Estados:

- `OPEN`
- `INVESTIGATING`
- `MITIGATING`
- `AWAITING_APPROVAL`
- `RESOLVED`
- `CLOSED`

Permita:

- Abrir incidentes automaticamente.
- Associar alterações de políticas.
- Associar funcionalidades afetadas.
- Criar tarefas.
- Atribuir responsáveis.
- Registrar decisões.
- Registrar aprovações.
- Consultar histórico.

---

# 13. MÓDULO 10 — NOTIFICATIONS & ALERTS

Implemente notificações para eventos importantes.

Exemplos:

**Alteração de política**

"Foi identificada uma alteração na documentação oficial do Mercado Livre. Algumas funcionalidades estão sendo avaliadas."

**Risco elevado**

"Uma alteração na política de utilização da API pode afetar o monitoramento de anúncios. A funcionalidade foi temporariamente restringida."

**API depreciada**

"A versão utilizada por determinada integração possui aviso de descontinuação. Uma atualização técnica é necessária."

**Permissão revogada**

"A autorização de uma integração foi revogada. Novas operações foram interrompidas."

Canais:

- Notificações dentro da plataforma.
- E-mail para administradores.
- Alertas no painel administrativo.

Não envie notificações excessivas.

Implemente agrupamento, deduplicação e controle de frequência.

---

# 14. MÓDULO 11 — ADMINISTRATIVE COMPLIANCE DASHBOARD

Crie um painel administrativo para acompanhamento da conformidade.

O dashboard deverá apresentar:

### Indicadores gerais

- Total de marketplaces monitorados.
- Fontes oficiais cadastradas.
- Fontes verificadas.
- Fontes indisponíveis.
- Políticas com alterações recentes.
- Incidentes abertos.
- Riscos críticos.
- Funcionalidades suspensas.
- Funcionalidades em revisão.
- APIs próximas da descontinuação.
- Última verificação realizada.

### Visão por marketplace

Apresente:

- Estado de conformidade.
- Última verificação.
- Quantidade de alterações.
- Incidentes.
- Funcionalidades afetadas.
- Permissões pendentes.
- APIs depreciadas.

### Histórico

Permita consultar:

- Alterações identificadas.
- Decisões tomadas.
- Funcionalidades bloqueadas.
- Funcionalidades reativadas.
- Aprovações.
- Evidências.
- Histórico de incidentes.

O dashboard deverá diferenciar claramente:

- Conformidade confirmada.
- Conformidade parcialmente verificada.
- Conformidade desconhecida.
- Não conformidade identificada.

Nunca apresente um marketplace como totalmente seguro apenas porque não foram detectadas alterações recentes.

---

# 15. MÓDULO 12 — SAFE REACTIVATION

Implemente um mecanismo controlado de reativação.

Uma funcionalidade suspensa não poderá ser reativada automaticamente apenas porque a fonte oficial voltou a responder ou porque a alteração deixou de aparecer.

A reativação deverá exigir:

1. Nova verificação da fonte oficial.
2. Confirmação da regra aplicável.
3. Análise de impacto atualizada.
4. Validação das permissões.
5. Correção técnica, quando necessária.
6. Testes automatizados.
7. Aprovação de um responsável autorizado.

Registre:

- Quem aprovou.
- Quando aprovou.
- Qual evidência foi utilizada.
- Qual versão da política foi considerada.
- Quais testes foram executados.

A reativação deverá ser limitada à funcionalidade afetada.

---

# 16. ARQUITETURA TÉCNICA

Implemente esse mecanismo respeitando a arquitetura existente da Seller Intelligence Platform.

Não introduza microsserviços desnecessários.

Utilize a stack já definida no projeto:

- Java 21.
- Spring Boot 3.x, respeitando a versão registrada no projeto.
- Spring Security.
- PostgreSQL.
- Redis.
- RabbitMQ.
- Flyway.
- Next.js.
- TypeScript.
- Docker.
- OpenTelemetry.

Organize os componentes em módulos de domínio:

```text
continuous-compliance
    |
    +-- policy-registry
    |
    +-- policy-monitoring
    |
    +-- change-detection
    |
    +-- impact-analysis
    |
    +-- risk-assessment
    |
    +-- compliance-enforcement
    |
    +-- permission-validator
    |
    +-- api-lifecycle-monitor
    |
    +-- incident-management
    |
    +-- notifications
    |
    +-- safe-reactivation
    |
    +-- compliance-dashboard
```

Implemente processamento assíncrono utilizando RabbitMQ quando necessário.

Utilize agendamentos controlados para verificações periódicas.

Implemente idempotência para impedir processamento duplicado de alterações.

Utilize PostgreSQL como fonte persistente de verdade.

Redis poderá ser utilizado para cache e controle operacional, mas nunca como única fonte do estado de conformidade.

---

# 17. SEGURANÇA E GOVERNANÇA

Implemente:

- RBAC.
- Permissões administrativas.
- Auditoria imutável ou protegida contra alterações indevidas.
- Histórico de decisões.
- Controle de acesso multi-tenant.
- Proteção das credenciais.
- Criptografia.
- Validação de entradas.
- Proteção contra SSRF no monitoramento de URLs.
- Allowlist de domínios oficiais.
- Limites de requisições para fontes monitoradas.
- Timeout.
- Circuit breaker.
- Tratamento de indisponibilidade.

O monitoramento de URLs deverá impedir acesso a endereços internos, localhost, serviços de metadata e redes privadas.

Apenas administradores autorizados poderão aprovar alterações críticas de conformidade.

---

# 18. TESTES AUTOMATIZADOS OBRIGATÓRIOS

Crie testes para validar:

1. Identificação de alteração em política.
2. Identificação de mudança de hash.
3. Identificação de mudança semântica.
4. Desconsideração de alterações irrelevantes de formatação.
5. Registro de nova versão.
6. Detecção de fonte indisponível.
7. Bloqueio de funcionalidade crítica.
8. Bloqueio restrito à operação afetada.
9. Preservação de operações não afetadas.
10. Identificação de permissão revogada.
11. Identificação de API depreciada.
12. Criação de incidente.
13. Envio de notificação.
14. Prevenção de eventos duplicados.
15. Proteção contra reativação não autorizada.
16. Aprovação administrativa.
17. Isolamento multi-tenant.
18. Proteção contra SSRF.
19. Recuperação após indisponibilidade do monitoramento.
20. Funcionamento do sistema quando fontes externas estão indisponíveis.

Utilize:

- JUnit 5.
- Mockito.
- Testcontainers.
- Testes de integração.
- Playwright.

Não utilize contas reais de produção para simular alterações ou bloqueios.

Utilize fixtures e ambientes controlados.

---

# 19. DOCUMENTAÇÃO OBRIGATÓRIA

Crie ou atualize os seguintes documentos:

`docs/compliance/CONTINUOUS_COMPLIANCE_ARCHITECTURE.md`

Documentação da arquitetura do mecanismo.

`docs/compliance/POLICY_MONITORING_GUIDELINES.md`

Regras de monitoramento das fontes oficiais.

`docs/compliance/COMPLIANCE_LIFECYCLE.md`

Ciclo de vida das políticas e permissões.

`docs/compliance/COMPLIANCE_INCIDENT_PLAYBOOK.md`

Procedimentos de tratamento de incidentes.

`docs/compliance/SAFE_REACTIVATION.md`

Procedimentos para reativação segura.

`docs/compliance/MARKETPLACE_POLICY_REGISTRY.md`

Registro das fontes oficiais.

`docs/compliance/CONTINUOUS_COMPLIANCE_TEST_REPORT.md`

Evidências dos testes realizados.

Atualize também:

- AGENTS.md.
- README.md.
- DECISION_LOG.md.
- SECURITY_AUDIT_REPORT.md.
- MARKETPLACE_COMPLIANCE_MATRIX.md.

---

# 20. CRITÉRIOS DE ACEITE

A implementação somente poderá ser aprovada quando os seguintes critérios forem atendidos.

### CA-01 — Monitoramento

O sistema deve conseguir verificar periodicamente fontes oficiais cadastradas.

### CA-02 — Detecção

O sistema deve identificar alterações relevantes e manter evidências das versões anterior e atual.

### CA-03 — Rastreabilidade

Toda alteração deve permitir identificar as funcionalidades potencialmente afetadas.

### CA-04 — Avaliação de riscos

Toda alteração relevante deve possuir classificação de risco.

### CA-05 — Bloqueio preventivo

Uma funcionalidade classificada como não autorizada ou de risco crítico deve ser bloqueada antes de sua próxima execução.

### CA-06 — Isolamento

O bloqueio deve afetar somente as funcionalidades relacionadas, sempre que for possível determinar seu escopo com segurança.

### CA-07 — Permissões

O sistema deve identificar permissões expiradas, revogadas ou insuficientes.

### CA-08 — APIs

O sistema deve identificar mudanças de versão e avisos de descontinuação.

### CA-09 — Incidentes

Alterações críticas devem gerar incidentes rastreáveis.

### CA-10 — Notificações

Os administradores devem receber alertas sobre mudanças relevantes.

### CA-11 — Reativação

Nenhuma funcionalidade suspensa por não conformidade poderá ser reativada sem passar pelo processo de validação e aprovação.

### CA-12 — Auditoria

Todas as alterações, bloqueios, aprovações e reativações devem possuir histórico.

### CA-13 — Segurança

O mecanismo de monitoramento não pode criar vulnerabilidades, como SSRF ou exposição de credenciais.

### CA-14 — Testes

Todos os testes obrigatórios devem passar.

### CA-15 — Documentação

Todos os documentos exigidos devem estar atualizados.

---

# 21. REGRAS DE EXECUÇÃO E CONTROLE DE QUALIDADE

Antes de iniciar:

1. Releia o PRD.
2. Releia o AGENTS.md.
3. Releia as regras de prevenção de alucinações.
4. Releia a arquitetura atual.
5. Releia o Marketplace Compliance Guard.
6. Releia o relatório de auditoria de segurança.
7. Releia a matriz de conformidade.
8. Verifique os módulos já implementados.
9. Identifique dependências e possíveis conflitos.

Não recrie funcionalidades existentes.

Estenda os componentes atuais sempre que tecnicamente adequado.

Não altere funcionalidades aprovadas sem justificativa e registro de decisão.

Não invente políticas, fontes, endpoints ou permissões.

Não marque verificações como concluídas sem evidências.

Não considere fonte indisponível como confirmação de conformidade.

Não utilize inteligência artificial como única autoridade para aprovar operações.

Não permita reativação automática de funcionalidades bloqueadas.

---

# 22. REGRA DE APROVAÇÃO E CONTINUIDADE

Este prompt representa uma etapa independente da evolução de segurança da Seller Intelligence Platform.

Ao finalizar:

1. Apresente um resumo da implementação.
2. Apresente a arquitetura criada.
3. Apresente os arquivos modificados.
4. Apresente os testes executados.
5. Apresente os resultados dos critérios de aceite.
6. Apresente pendências.
7. Apresente evidências técnicas.

Utilize os estados:

`APPROVED`: todos os critérios obrigatórios foram atendidos.

`PARTIALLY_APPROVED`: implementação parcial, com pendências documentadas.

`BLOCKED`: existem falhas ou riscos impeditivos.

O próximo prompt da sequência de desenvolvimento somente poderá ser aceito e executado quando este prompt estiver com status `APPROVED`.

Se houver pendências, não avance automaticamente.

Apresente as correções necessárias e aguarde a validação.

**Inicie a análise da arquitetura existente e, em seguida, implemente o Continuous Marketplace Compliance Engine.**
