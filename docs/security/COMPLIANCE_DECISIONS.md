# Registro Formal de Decisões de Compliance (Compliance Decisions Log)

**Plataforma:** Seller Intelligence Platform  
**Data da Emissão:** 17 de Novembro de 2026  
**Órgão Decisório:** Comitê Técnico de Arquitetura, Segurança e Conformidade (AppSec + Legal Tech)  
**Status Geral:** `ACTIVE & ENFORCED`  

---

## 1. Contexto e Finalidade

Este documento consolida o registro formal de decisões de conformidade, políticas de bloqueio preventivo e salvaguardas operacionais instituídas para garantir que a Seller Intelligence Platform opere com segurança máxima e dentro das regras de negócio de cada ecossistema parceiro.

---

## 2. Decisões de Conformidade Registradas

### CD-001: Princípio Arquitetural Fail-Closed Mandatório
- **Data:** 17/11/2026  
- **Contexto:** Em sistemas distribuídos que interagem com APIs comerciais sensíveis, falhas na validação de permissões, ausência de credenciais ou comportamentos anômalos podem expor contas de sellers a penalizações.
- **Decisão:** Toda e qualquer operação de marketplace deve passar obrigatoriamente pelo componente central `MarketplaceComplianceGuard`. Se a legitimidade de uma chamada, a validade do token ou o status da loja não puderem ser confirmados com certeza determinística, a operação é sumariamente rejeitada (`FAIL CLOSED`).
- **Consequências:** Nenhuma operação é executada com suposições de permissão. Tentativas com lojas inválidas, desconectadas ou tokens expirados são bloqueadas localmente antes de gerar tráfego externo.
- **Status:** `ADOTADA & IMPLEMENTADA`

---

### CD-002: Bloqueio Preventivo de Canais em Expansão (`UNVERIFIED_COMPLIANCE`)
- **Data:** 17/11/2026  
- **Contexto:** Os marketplaces TikTok Shop, Magalu e SHEIN estão previstos no plano de expansão do PRD, porém ainda não possuem homologação contratual formalizada, contas de desenvolvedor aprovadas ou ambientes de produção concedidos à plataforma.
- **Decisão:** Classificar `TIKTOK_SHOP`, `MAGALU` e `SHEIN` formalmente como `UNVERIFIED_COMPLIANCE`. O `MarketplaceComplianceGuard` e o `MarketplaceConnectionService` devem barrar categoricamente qualquer tentativa de início de fluxo OAuth ou chamada de sincronização para esses canais em ambiente de produção com o código `UNVERIFIED_MARKETPLACE_BLOCKED`.
- **Consequências:** Impede que desenvolvedores ou usuários tentem automações não oficiais ou arriscadas nesses canais até a conclusão dos acordos formais de parceria comercial.
- **Status:** `ADOTADA & IMPLEMENTADA`

---

### CD-003: Governança Mandatória Human-in-the-Loop para Ações Mutáveis Comerciais
- **Data:** 17/11/2026  
- **Contexto:** Operações de escrita em marketplaces (publicação de anúncios, alteração de preços, alteração de quantidade em estoque e envio de mensagens para clientes) possuem alto impacto financeiro e reputacional direto.
- **Decisão:** Definir a categoria de risco `OperationRiskLevel.MUTABLE_CRITICAL`. Nenhuma mutação comercial pode ser executada por agentes de IA, rotinas agendadas ou extensões de navegador de forma autônoma sem a confirmação e aprovação explícita de um operador humano autenticado (`humanApproved == true`).
- **Consequências:** Se `humanApproved == false`, o `MarketplaceComplianceGuard` rejeita a execução com `HUMAN_APPROVAL_REQUIRED` e grava evento de auditoria com status `DENIED`. A plataforma protege os sellers contra precificações incorretas ou publicações automáticas indesejadas.
- **Status:** `ADOTADA & IMPLEMENTADA`

---

### CD-004: Quarentena Automática e Circuit Breaker em HTTP 429 e Falhas Repetitivas
- **Data:** 17/11/2026  
- **Contexto:** Requisições contínuas disparadas contra marketplaces quando a quota de requisições se esgota (HTTP 429) ou quando a API externa apresenta erros sistemáticos podem acarretar bloqueio temporário de IP ou suspensão de aplicativos parceiros.
- **Decisão:** Instituir quarentena automática de no mínimo 60 segundos por loja em caso de HTTP 429 e abrir o Circuit Breaker (`CIRCUIT_BREAKER_OPEN`) após 5 falhas consecutivas (HTTP 401, 403 ou 5xx). Durante o período de cooldown, novas requisições para a loja são barradas localmente em memória.
- **Consequências:** Protege a reputação da plataforma perante os servidores dos marketplaces, evita o consumo inútil de recursos de rede e impede o agravamento de incidentes externos.
- **Status:** `ADOTADA & IMPLEMENTADA`

---

### CD-005: Proibição Irrestrita de Scraping Ilegítimo, CAPTCHA Bypass e Evasão Anti-Bot
- **Data:** 17/11/2026  
- **Contexto:** O benchmark funcional do setor (Avantpro, JoomPulse) utiliza dados de mercado. No entanto, técnicas agressivas de scraping contra páginas protegidas de marketplaces violam expressamente termos de uso e leis de propriedade intelectual.
- **Decisão:** Proibir em absoluto qualquer código de scraping não autorizado, solvers de CAPTCHA, proxies rotativos para evasão, falsificação de fingerprints e simulação robótica de usuários. Pesquisas de mercado devem utilizar exclusivamente APIs oficiais autorizadas ou informações legítimas disponibilizadas via fluxo regular do usuário com rótulo explícito de proveniência (`OBSERVED`).
- **Consequências:** Garante segurança jurídica total para a plataforma e para os vendedores, eliminando riscos de processos judiciais por violação de termos de uso de marketplaces.
- **Status:** `ADOTADA & IMPLEMENTADA`

---

### CD-006: Minimização Radical de Dados Pessoais de Compradores (LGPD e Amazon DPP)
- **Data:** 17/11/2026  
- **Contexto:** A Lei Geral de Proteção de Dados (LGPD) e a política Amazon Data Protection Policy (DPP) estabelecem que dados de compradores (nome, CPF, endereço, telefone) devem ser estritamente minimizados e protegidos.
- **Decisão:** A plataforma não persiste nem indexa dados pessoais de compradores finais em bases compartilhadas para fins de inteligência analítica. Pedidos são persistidos com identificador externo, data, status e valores monetários determinísticos (`BigDecimal`) estritamente necessários para cálculo financeiro, DRE e conciliação.
- **Consequências:** Elimina a superfície de ataque para vazamento de PII e assegura conformidade com auditorias de segurança da Amazon SP-API e órgãos reguladores de proteção de dados.
- **Status:** `ADOTADA & IMPLEMENTADA`

---

### CD-007: Sanitização Sistemática de Logs e Exceções contra Vazamento de Tokens
- **Data:** 17/11/2026  
- **Contexto:** Erros de integração frequentemente contêm payloads com cabeçalhos `Authorization: Bearer ...` ou URLs de redirecionamento com tokens e chaves, gerando risco de exposição em logs centralizados.
- **Decisão:** Implementar filtro regex `SENSITIVE_PATTERN` universal em `ComplianceViolationException` e no `AuditService` para mascaramento automático de qualquer padrão de credencial com `[REDACTED]`.
- **Consequências:** Garante que mesmo em situações de falha crítica ou logs de depuração ativados, segredos criptográficos não vazem para ferramentas de observabilidade ou relatórios.
- **Status:** `ADOTADA & IMPLEMENTADA`

---

### CD-008: Servidor MCP Read-Only e IA Estritamente Consultiva
- **Data:** 17/11/2026  
- **Contexto:** Com a expansão do ecossistema com assistentes conversacionais e servidores MCP (Model Context Protocol), comandos gerados por modelos de linguagem poderiam tentar induzir operações nos canais sem as validações de backend.
- **Decisão:** O servidor MCP é restrito estritamente a ferramentas de leitura (`read-only`). A IA e o Listing Studio atuam exclusivamente como mecanismos analíticos e de recomendação. Nenhuma sugestão é aplicada nos marketplaces sem que passe pelo fluxo regular de validação do backend com o `MarketplaceComplianceGuard`.
- **Consequências:** Previne ataques de Prompt Injection e impede que agentes executem SQL arbitrário ou operações mutáveis sem supervisão.
- **Status:** `ADOTADA & IMPLEMENTADA`
