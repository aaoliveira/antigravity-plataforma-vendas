# PROMPT — AUDITORIA COMPLETA DE SEGURANÇA, COMPLIANCE E PROTEÇÃO DE CONTAS DOS MARKETPLACES

## 1. CONTEXTO E PAPEL

Atue como um Staff Software Engineer, Software Architect, Application Security Engineer e especialista em integrações com marketplaces, com experiência em:

- Segurança de aplicações SaaS.
- OWASP ASVS e OWASP Top 10.
- Segurança de APIs REST.
- OAuth 2.0 e OpenID Connect.
- Integrações oficiais com marketplaces.
- Políticas de utilização de APIs.
- Automação comercial.
- Browser Extensions e Chrome Manifest V3.
- Proteção de contas comerciais.
- Engenharia de conformidade (Compliance Engineering).
- LGPD e proteção de dados.
- Prevenção de abuso e comportamento automatizado indevido.

Você foi designado para realizar uma auditoria profunda na Seller Intelligence Platform, considerando o PRD, a arquitetura, o código-fonte, as integrações existentes e todas as funcionalidades implementadas ou planejadas.

**Sua principal responsabilidade é garantir que a plataforma não execute operações que violem as políticas, os termos de uso, as regras de integração ou os mecanismos de proteção dos marketplaces conectados.**

A proteção das contas dos usuários deve ser considerada um requisito crítico de negócio e de arquitetura.

Não basta garantir que o sistema funcione tecnicamente. Ele precisa funcionar de maneira segura, legítima e compatível com as regras dos marketplaces.

---

# 2. OBJETIVO PRINCIPAL

Realizar uma auditoria completa para identificar, corrigir e prevenir comportamentos que possam provocar:

- Bloqueio temporário de contas.
- Suspensão de contas.
- Banimento permanente.
- Restrições de acesso às APIs.
- Revogação de tokens OAuth.
- Bloqueio de aplicações integradoras.
- Limitação de requisições.
- Identificação da plataforma como ferramenta de automação abusiva.
- Bloqueio de IPs.
- Restrições de publicação de anúncios.
- Restrições de consulta de informações.
- Problemas relacionados à utilização de extensões de navegador.
- Violações de privacidade e proteção de dados.
- Descumprimento dos termos de utilização dos marketplaces.

A auditoria deverá considerar todos os marketplaces previstos no projeto:

1. Mercado Livre.
2. Shopee.
3. Amazon.
4. TikTok Shop.
5. Magalu.
6. SHEIN.

Não presuma que as regras de um marketplace podem ser aplicadas aos demais.

Cada integração deverá possuir uma avaliação de conformidade independente.

---

# 3. REGRA FUNDAMENTAL: NÃO INVENTAR POLÍTICAS OU PERMISSÕES

Esta regra é obrigatória.

Você NÃO DEVE:

- Inventar permissões de APIs.
- Presumir que determinada operação é permitida.
- Presumir que uma API pública autoriza qualquer utilização comercial.
- Presumir que uma informação acessível pelo navegador pode ser coletada automaticamente.
- Considerar que uma operação permitida em um marketplace também é permitida em outro.
- Implementar mecanismos de coleta sem verificar sua legitimidade.
- Criar funcionalidades que dependam de violação dos termos de utilização.
- Tratar a ausência de documentação como autorização.

Para cada funcionalidade relacionada a um marketplace, verifique a documentação oficial e os termos aplicáveis.

Utilize prioritariamente:

- Documentação oficial das APIs.
- Termos de uso.
- Políticas de desenvolvedores.
- Políticas de privacidade.
- Políticas de automação.
- Regras de autenticação.
- Políticas de rate limit.
- Regras de utilização de dados.
- Políticas de extensões e integrações.

Registre a URL oficial consultada, a data da consulta e a regra identificada.

Se não conseguir confirmar se determinada operação é permitida, classifique-a como:

`UNVERIFIED_COMPLIANCE`

Não implemente ou habilite essa operação em produção até que sua conformidade seja confirmada.

---

# 4. AUDITORIA DAS INTEGRAÇÕES COM MARKETPLACES

Examine profundamente todos os conectores e mecanismos de integração existentes.

Para cada marketplace, verifique:

## 4.1. Autenticação e autorização

- Utilização de OAuth 2.0 quando exigido.
- Utilização dos fluxos oficiais de autorização.
- Validação de state para proteção contra CSRF.
- Validação de redirect URI.
- Utilização de PKCE quando aplicável.
- Armazenamento seguro dos access tokens.
- Armazenamento seguro dos refresh tokens.
- Renovação automática de tokens conforme documentação.
- Tratamento de tokens expirados.
- Revogação de tokens.
- Controle dos escopos autorizados.
- Princípio do menor privilégio.
- Isolamento das credenciais entre organizações.

Não solicite ou armazene senhas pessoais dos usuários dos marketplaces.

Não utilize mecanismos de login que contornem os processos oficiais de autenticação.

## 4.2. Consumo das APIs

Verifique:

- Se os endpoints utilizados são oficiais.
- Se os endpoints estão documentados.
- Se a operação possui autorização compatível.
- Se os parâmetros enviados estão corretos.
- Se os limites de requisição são respeitados.
- Se existe controle de concorrência.
- Se existe tratamento de HTTP 429.
- Se existe backoff exponencial.
- Se existe controle de retries.
- Se existe idempotência nas operações aplicáveis.
- Se existe controle de timeout.
- Se existe circuit breaker.
- Se existe registro de falhas.

Não implemente estratégias para contornar limites de requisição.

Não utilize múltiplas contas ou IPs com a finalidade de burlar limites impostos pelo marketplace.

Quando o limite for atingido, a aplicação deverá reduzir ou interromper as chamadas conforme as regras oficiais.

## 4.3. Webhooks

Verifique:

- Autenticidade dos eventos.
- Validação de assinatura, quando suportada.
- Proteção contra replay attacks.
- Idempotência.
- Validação do conteúdo recebido.
- Controle de eventos duplicados.
- Controle de eventos fora de ordem.
- Proteção contra payloads maliciosos.
- Isolamento por organização.
- Registro de auditoria.

Nunca confie exclusivamente na origem declarada em um payload.

---

# 5. AUDITORIA DE WEB SCRAPING E COLETA DE DADOS

Esta é uma das etapas mais críticas.

A plataforma possui funcionalidades de:

- Análise de concorrência.
- Monitoramento de anúncios.
- Análise de preços.
- Histórico de produtos.
- Pesquisa de produtos.
- Análise de vendedores.
- Análise de categorias.
- Monitoramento de avaliações.
- Análise de posicionamento.
- Extensões para navegadores.

Essas funcionalidades podem envolver coleta e processamento de informações disponíveis nos marketplaces.

Você deverá investigar como cada informação está sendo obtida.

Classifique cada mecanismo de coleta em uma das categorias:

**A. API OFICIAL AUTORIZADA**

Coleta realizada por endpoint oficial, dentro das permissões concedidas.

**B. DADO DISPONIBILIZADO OFICIALMENTE**

Informação disponibilizada publicamente por mecanismo oficialmente autorizado.

**C. COLETA POR NAVEGADOR**

Informação obtida durante a navegação do usuário.

**D. WEB SCRAPING**

Extração automatizada de informações das páginas dos marketplaces.

**E. MÉTODO NÃO VALIDADO**

Mecanismo cuja legitimidade não pode ser confirmada.

Para cada mecanismo, determine:

1. Qual informação é coletada?
2. Qual é a origem?
3. Qual técnica é utilizada?
4. Existe autorização para coleta?
5. Existe restrição contratual?
6. Existe restrição de frequência?
7. Existe restrição de armazenamento?
8. Existe restrição de redistribuição?
9. Existe risco de bloqueio?
10. Qual alternativa oficial pode ser utilizada?

**REGRA DE SEGURANÇA:**

Não implemente CAPTCHA bypass, evasão de mecanismos anti-bot, rotação de IP para contornar bloqueios, falsificação de identidade do navegador, manipulação de fingerprints ou qualquer mecanismo destinado a evitar controles de segurança dos marketplaces.

Se uma funcionalidade depender de uma técnica não autorizada ou cuja permissão não possa ser confirmada, ela deverá ser bloqueada para utilização em produção.

Não considere que o fato de uma informação ser publicamente acessível significa que sua coleta automatizada é permitida.

---

# 6. AUDITORIA DA EXTENSÃO CHROME

A extensão do navegador precisa ser examinada separadamente.

Verifique:

- Utilização do Manifest V3.
- Permissões declaradas.
- Host permissions.
- Content scripts.
- Background service worker.
- Comunicação com o backend.
- Armazenamento local.
- Gerenciamento de tokens.
- Comunicação entre páginas.
- Isolamento do contexto de execução.
- Segurança contra XSS.
- Segurança contra CSRF.
- Validação de mensagens.
- Controle de origens.
- Proteção contra vazamento de credenciais.

Avalie se a extensão realiza alguma operação que possa ser interpretada como comportamento automatizado não permitido pelo marketplace.

A extensão NÃO deverá:

- Executar ações de compra automaticamente.
- Publicar anúncios sem autorização explícita.
- Alterar preços automaticamente sem aprovação.
- Realizar cliques automatizados para contornar restrições.
- Enviar mensagens em massa.
- Realizar consultas abusivas.
- Simular comportamento humano para evitar mecanismos de detecção.
- Modificar informações do marketplace sem autorização.
- Interferir no funcionamento normal das páginas.

A extensão deverá priorizar recursos de leitura e análise, respeitando as permissões e restrições aplicáveis.

Sempre que houver dúvida sobre a legitimidade de uma interação com o marketplace, interrompa sua implementação e registre a pendência.

---

# 7. AUDITORIA DE AUTOMAÇÕES E OPERAÇÕES COMERCIAIS

Analise todas as funcionalidades que executam operações em nome do usuário.

Exemplos:

- Criação de anúncios.
- Edição de anúncios.
- Publicação de anúncios.
- Alteração de preços.
- Atualização de estoque.
- Aceite de promoções.
- Atualização de imagens.
- Respostas a perguntas.
- Gestão de campanhas.
- Atualização de informações comerciais.

Para cada operação, identifique:

1. Se existe API oficial.
2. Se existe autorização para execução automatizada.
3. Se a operação exige consentimento explícito.
4. Se exige aprovação humana.
5. Se existe limite operacional.
6. Se existe restrição por conta.
7. Se existe histórico de execução.
8. Se existe mecanismo de reversão, quando aplicável.

Implemente uma política de autorização por operação.

As ações críticas deverão exigir aprovação humana antes da execução.

Não implemente publicação, alteração de preço ou qualquer outra operação de escrita utilizando métodos não autorizados.

---

# 8. PROTEÇÃO CONTRA BLOQUEIOS E COMPORTAMENTOS SUSPEITOS

Implemente uma camada de proteção operacional.

Ela deverá ser responsável por impedir que o sistema continue executando operações quando houver indícios de risco.

Crie mecanismos para:

- Monitorar respostas HTTP 401.
- Monitorar respostas HTTP 403.
- Monitorar respostas HTTP 429.
- Identificar expiração de tokens.
- Identificar revogação de autorização.
- Identificar erros de permissão.
- Identificar bloqueios de acesso.
- Identificar mudanças inesperadas nas respostas das APIs.
- Interromper operações em caso de falha de autorização.
- Impedir retries excessivos.
- Respeitar limites documentados.
- Suspender integrações com comportamento anormal.

Implemente um mecanismo chamado:

`Marketplace Compliance Guard`

Responsabilidades:

1. Verificar se uma operação está autorizada.
2. Verificar se a integração está habilitada.
3. Verificar os limites operacionais.
4. Verificar o estado da autenticação.
5. Verificar se existem bloqueios conhecidos.
6. Impedir operações não autorizadas.
7. Registrar o motivo do bloqueio.
8. Notificar o usuário.
9. Registrar eventos de auditoria.

O mecanismo deverá utilizar a política:

`FAIL CLOSED`

Isso significa que, quando não for possível confirmar a legitimidade de uma operação, ela não deverá ser executada.

Não tente contornar um bloqueio retornado pelo marketplace.

---

# 9. SEGURANÇA MULTI-TENANT

Como a plataforma permite múltiplas empresas, CNPJs e contas de marketplaces, examine:

- Isolamento de dados entre organizações.
- Isolamento das credenciais.
- Isolamento das contas conectadas.
- Controle de permissões.
- Autorização por organização.
- Controle de acesso por usuário.
- Prevenção contra IDOR.
- Proteção contra acesso cruzado.
- Controle de operações administrativas.
- Auditoria de alterações.

Nenhuma organização poderá consultar dados privados de outra organização.

Uma conta de marketplace conectada a determinada organização não poderá ser utilizada por outra organização sem autorização explícita.

---

# 10. SEGURANÇA DOS DADOS E LGPD

Verifique a conformidade com a Lei Geral de Proteção de Dados.

Avalie:

- Base legal para tratamento.
- Finalidade do processamento.
- Minimização dos dados.
- Retenção.
- Exclusão.
- Anonimização.
- Controle de acesso.
- Criptografia.
- Auditoria.
- Exportação de dados.
- Tratamento de dados pessoais.
- Compartilhamento com provedores de IA.

As informações privadas dos vendedores não poderão ser utilizadas para treinar modelos de terceiros sem autorização e base legal adequadas.

Tokens e credenciais nunca deverão aparecer em:

- Logs.
- Mensagens de erro.
- Relatórios.
- Respostas de APIs.
- Prompts enviados aos modelos de IA.
- Arquivos versionados no Git.

---

# 11. AUDITORIA DAS FUNCIONALIDADES DE INTELIGÊNCIA ARTIFICIAL

Examine todas as funcionalidades de IA.

Verifique se:

- A IA acessa apenas informações autorizadas.
- Existe isolamento entre organizações.
- Existe proteção contra prompt injection.
- Existe validação das respostas.
- Existe controle das ferramentas disponíveis.
- Existe auditoria das operações.
- Existe controle das informações enviadas aos provedores.
- Existe proteção contra exposição de dados sensíveis.

A IA não poderá executar operações diretamente nos marketplaces sem passar pelas regras de autorização do backend.

Toda ação deverá passar pelo Marketplace Compliance Guard.

A IA deverá atuar como mecanismo de análise e recomendação, não como mecanismo para contornar políticas dos marketplaces.

---

# 12. MATRIZ DE CONFORMIDADE POR MARKETPLACE

Crie um documento:

`docs/security/MARKETPLACE_COMPLIANCE_MATRIX.md`

Esse documento deverá conter uma matriz individual para:

- Mercado Livre.
- Shopee.
- Amazon.
- TikTok Shop.
- Magalu.
- SHEIN.

Para cada marketplace, registre:

| Campo                  | Descrição                          |
| ---------------------- | ---------------------------------- |
| Marketplace            | Nome                               |
| Funcionalidade         | Recurso analisado                  |
| Método de integração   | API, webhook, extensão etc.        |
| Endpoint               | Endpoint oficial, quando aplicável |
| Documentação oficial   | URL                                |
| Política aplicável     | Termo ou regra                     |
| Permissão necessária   | Escopo ou autorização              |
| Limite operacional     | Limite documentado                 |
| Status de conformidade | Resultado                          |
| Risco                  | Classificação                      |
| Ação necessária        | Correção ou validação              |
| Evidência              | Referência documental              |
| Data da verificação    | Data da consulta                   |

Utilize os seguintes status:

- `COMPLIANT`
- `NON_COMPLIANT`
- `PARTIALLY_COMPLIANT`
- `UNVERIFIED_COMPLIANCE`
- `NOT_APPLICABLE`

Não marque nenhuma funcionalidade como COMPLIANT sem evidência suficiente.

---

# 13. CLASSIFICAÇÃO DE RISCOS

Classifique cada vulnerabilidade ou não conformidade:

**CRITICAL**

Existe risco imediato de comprometimento de contas, credenciais, dados ou violação grave de políticas.

**HIGH**

Existe risco elevado de bloqueio, suspensão, acesso indevido ou operação não autorizada.

**MEDIUM**

Existe risco moderado que exige correção antes da disponibilização em produção.

**LOW**

Melhoria de segurança sem risco imediato identificado.

**INFORMATIONAL**

Recomendação ou observação sem vulnerabilidade confirmada.

Crie o arquivo:

`docs/security/SECURITY_AUDIT_REPORT.md`

Inclua:

- Resumo executivo.
- Escopo auditado.
- Metodologia.
- Vulnerabilidades identificadas.
- Não conformidades.
- Riscos por marketplace.
- Evidências.
- Correções necessárias.
- Recomendações.
- Pendências.
- Resultado final.

---

# 14. IMPLEMENTAÇÃO DAS CORREÇÕES

Após concluir a auditoria, não se limite a produzir um relatório.

Corrija os problemas identificados que possam ser resolvidos com segurança e sem alterar indevidamente os requisitos do produto.

Implemente:

- Marketplace Compliance Guard.
- Controle centralizado de permissões.
- Rate limiting.
- Backoff exponencial.
- Circuit breaker.
- Tratamento de HTTP 429.
- Proteção de tokens.
- Auditoria de operações.
- Controle de aprovação humana.
- Bloqueio de funcionalidades não validadas.
- Alertas de segurança.
- Monitoramento de integrações.
- Proteção multi-tenant.

Não altere políticas comerciais, regras financeiras ou funcionalidades sem registrar a decisão.

Se uma funcionalidade do PRD não puder ser implementada de maneira comprovadamente compatível com determinado marketplace, mantenha-a bloqueada e documente a alternativa técnica.

---

# 15. TESTES OBRIGATÓRIOS

Implemente testes automatizados para validar:

1. Operação não autorizada é bloqueada.
2. Token expirado impede operações.
3. Token revogado impede operações.
4. HTTP 429 aciona tratamento de limite.
5. Falhas consecutivas acionam circuit breaker.
6. Operações bloqueadas não são repetidas indefinidamente.
7. Credenciais não aparecem nos logs.
8. Uma organização não acessa dados de outra.
9. Usuário sem permissão não executa operações críticas.
10. IA não consegue ignorar o mecanismo de autorização.
11. Extensão não executa ações não autorizadas.
12. Operações críticas exigem aprovação humana.
13. Funcionalidades com compliance não validado permanecem bloqueadas.
14. Eventos de auditoria são registrados corretamente.
15. A revogação de uma integração interrompe novas operações.

Utilize JUnit, Mockito, Testcontainers e Playwright conforme a camada testada.

Não utilize contas reais de produção para testes destrutivos.

---

# 16. REGRAS INEGOCIÁVEIS DE DESENVOLVIMENTO

Durante toda a execução:

1. Não invente políticas.
2. Não invente permissões.
3. Não invente endpoints.
4. Não invente respostas de APIs.
5. Não considere scraping automaticamente permitido.
6. Não implemente bypass de CAPTCHA.
7. Não implemente evasão de mecanismos anti-bot.
8. Não implemente rotação de IP para contornar limites.
9. Não implemente falsificação de fingerprint.
10. Não implemente automações abusivas.
11. Não exponha credenciais.
12. Não ignore erros de autorização.
13. Não transforme estimativas em dados reais.
14. Não remova controles de segurança para facilitar testes.
15. Não habilite funcionalidades com conformidade desconhecida em produção.

Toda decisão técnica deverá ser documentada.

---

# 17. ENTREGÁVEIS OBRIGATÓRIOS

Ao concluir, entregue:

1. `docs/security/SECURITY_AUDIT_REPORT.md`
2. `docs/security/MARKETPLACE_COMPLIANCE_MATRIX.md`
3. `docs/security/SECURITY_REMEDIATION_PLAN.md`
4. `docs/security/SECURITY_TEST_REPORT.md`
5. `docs/security/MARKETPLACE_INTEGRATION_GUIDELINES.md`
6. `docs/security/COMPLIANCE_DECISIONS.md`
7. Implementação do Marketplace Compliance Guard.
8. Testes automatizados.
9. Atualização do README.
10. Atualização do DECISION_LOG.md.

---

# 18. CRITÉRIOS DE ACEITE

A execução somente será considerada aprovada quando:

- Todos os marketplaces previstos tiverem sido avaliados individualmente.
- Todos os mecanismos de coleta estiverem identificados.
- As integrações estiverem classificadas.
- Não existirem vulnerabilidades CRITICAL abertas.
- Não existirem operações não autorizadas habilitadas em produção.
- O Marketplace Compliance Guard estiver implementado.
- As operações críticas estiverem protegidas.
- O isolamento multi-tenant estiver validado.
- Os testes automatizados estiverem passando.
- Os documentos de auditoria estiverem completos.
- Todas as pendências estiverem registradas.

**Atenção:** a aprovação técnica desta auditoria não representa garantia absoluta de que um marketplace jamais restringirá uma conta. A decisão final sobre permissões e conformidade depende das regras vigentes e da avaliação de cada plataforma.

---

# 19. REGRA DE CONTINUIDADE E APROVAÇÃO

Este prompt faz parte da sequência oficial de desenvolvimento da Seller Intelligence Platform.

Antes de executar qualquer etapa:

1. Releia as regras persistentes do projeto.
2. Releia o PRD.
3. Releia o AGENTS.md.
4. Releia as regras de segurança.
5. Releia as decisões arquiteturais.
6. Verifique os critérios de aceite.
7. Identifique os impactos da implementação.

Ao concluir:

- Execute os testes.
- Apresente as evidências.
- Atualize os documentos.
- Informe vulnerabilidades pendentes.
- Apresente o resultado da auditoria.

Utilize os seguintes estados:

`APPROVED`: todos os critérios obrigatórios foram atendidos.

`BLOCKED`: existe não conformidade, falha ou pendência impeditiva.

`PARTIALLY_APPROVED`: existem entregas aprovadas, mas ainda existem pendências.

**REGRA ABSOLUTA:**

O próximo prompt da sequência de desenvolvimento não poderá ser aceito ou executado enquanto este prompt não estiver com status `APPROVED`.

Caso existam pendências, apresente as correções necessárias e aguarde a resolução e validação.

Não declare a auditoria aprovada apenas porque o código compila ou os testes unitários passam.

A aprovação exige evidências técnicas, documentais e de conformidade.

Inicie agora a auditoria.
