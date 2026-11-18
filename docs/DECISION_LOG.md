# Registro de Decisões Arquiteturais (Architecture Decision Log)

> **Formato:** Architecture Decision Records (ADR)  
> **Status:** Baseline Homologado &middot; **Versão:** 1.0.0  
> **Data:** 17/11/2026  

---

## 1. Sumário de Decisões Homologadas

| ADR ID | Título da Decisão | Status | Data | Impacto Principal |
|---|---|---|---|---|
| **ADR-001** | Adoção de Monólito Modular com Spring Modulith e Java 25 LTS | **Aprovado** | 17/11/2026 | Arquitetura unificada sem latência de rede; limites de domínio verificados no build. |
| **ADR-002** | Defesa em Profundidade para Isolamento Multi-Tenant (Context + RLS) | **Aprovado** | 17/11/2026 | Impossibilidade de vazamento de dados entre clientes em todas as camadas. |
| **ADR-003** | Cálculos Financeiros Determinísticos Exclusivamente com BigDecimal | **Aprovado** | 17/11/2026 | Precisão absoluta em centavos; proibição de floats e de cálculos via LLM. |
| **ADR-004** | Conectores de Marketplace com Capability Matrix e Política Fail-Closed | **Aprovado** | 17/11/2026 | Proteção total das contas comerciais dos vendedores contra bloqueios ou scraping hostil. |
| **ADR-005** | IA Assistiva Restrita a Tool Calling Read-Only e Sem Acesso Direto a Bancos | **Aprovado** | 17/11/2026 | IA nunca altera preços ou dados; atua como copiloto consultivo explicável. |
| **ADR-006** | Metodologia Spec-Driven Development (SDD) com Quality Gates Sequenciais | **Aprovado** | 17/11/2026 | Bloqueio estrito de avanço de prompt se houver testes ou verificações pendentes. |

---

## 2. Detalhamento dos Architecture Decision Records (ADRs)

### ADR-001: Adoção de Monólito Modular com Spring Modulith e Java 25 LTS
* **Contexto:** A plataforma precisa de alta coesão entre módulos (produtos, pedidos, financeiro, concorrência), rapidez de iteração e baixo custo operacional inicial, mas sem gerar acoplamento caótico ("espaguete").
* **Decisão:** Adotar um **Monólito Modular** usando Java 25 LTS e Spring Boot 3.5.x, com Spring Modulith e verificação automatizada por testes de arquitetura via **ArchUnit**.
* **Atualização de compatibilidade (18/11/2026):** O backend passou a compilar e executar em Java 25 LTS; Spring Boot 3.5.16 declara suporte até Java 25. ArchUnit foi alinhado em 1.5.1 (incluindo o artefato transitivo gerenciado pelo Spring Modulith), pois a versão 1.3.0 não consegue importar bytecode Java 25 (major 69).
* **Consequências Positivas:** Deploy atômico, transações ACID locais simplificadas, sem sobrecarga de infraestrutura de rede, e fronteiras prontas para eventual extração futura de microsserviços quando houver evidência real de necessidade.
* **Consequências Negativas:** Exige disciplina estrita de engenharia para não violar contratos internos.

---

### ADR-002: Defesa em Profundidade para Isolamento Multi-Tenant
* **Contexto:** Como SaaS multi-tenant, o vazamento de dados de faturamento, pedidos ou concorrência entre vendedores seria fatal para a credibilidade do negócio.
* **Decisão:** Implementar defesa em duas camadas ativas:
  1. *Camada de Aplicação:* Resolução mandatória de `tenant_id` derivado do token JWT assinado e validação de permissões nos serviços.
  2. *Camada de Banco:* PostgreSQL Row-Level Security (RLS) forçado em todas as tabelas de sellers com `SET LOCAL app.current_tenant`.
* **Consequências:** Zero tolerância a falhas de isolamento; testes negativos automatizados em CI impedem qualquer regressão.

---

### ADR-003: Cálculos Financeiros Determinísticos com `BigDecimal`
* **Contexto:** Arredondamentos imprecisos (ponto flutuante binário `double`/`float`) ou alucinações de modelos de linguagem causam erros contábeis e prejuízos materiais para sellers.
* **Decisão:** Todo cálculo financeiro de margem, imposto, frete e comissão é implementado em classes utilitárias puras em Java usando `BigDecimal`, com escala e modo de arredondamento explicitados nas regras de negócio. Modelos de IA são categoricamente proibidos de calcular valores monetários.
* **Consequências:** Cálculos 100% reproduzíveis, auditáveis e com rastreabilidade completa de fórmulas.

---

### ADR-004: Conectores com Capability Matrix e Fail-Closed
* **Contexto:** Marketplaces possuem diferentes termos de uso, limites de taxa e graus de maturidade de APIs. Ações agressivas de scraping ou requisições descontroladas provocam o banimento de contas comerciais.
* **Decisão:** Desenvolver adaptadores desacoplados do core que implementam uma Matriz de Capacidades (`SUPPORTED`, `UNSUPPORTED`, `RESTRICTED`, `UNVERIFIED`). Toda operação não comprovada oficialmente é bloqueada. Se houver erro ou dúvida, a plataforma adota postura *Fail-Closed*.
* **Consequências:** Máxima segurança jurídica e operacional para as contas de vendedores conectadas.

---

### ADR-005: IA Assistiva Restrita a Tool Calling Read-Only
* **Contexto:** Inteligência Artificial generativa pode alucinar dados de concorrência ou tentar executar ações indesejadas no marketplace se receber privilégios de escrita.
* **Decisão:** No MVP, o assistente opera exclusivamente sob o padrão de *Tool Calling* somente leitura. O modelo recebe apenas saídas de serviços internos autorizados e todo conteúdo gerado (títulos, fichas) é tratado como rascunho sujeito à revisão humana obrigatória.
* **Consequências:** Eliminação do risco de prompt injection executando mutações externas ou expondo dados confidenciais.

---

### ADR-006: Metodologia Spec-Driven Development (SDD) com Quality Gates
* **Contexto:** O desenvolvimento por agentes autônomos corre o risco de introduzir código especulativo, quebrar requisitos prévios e avançar sem testes executados.
* **Decisão:** Impor um fluxo sequencial rígido: cada prompt só transita para `PASSED` se 100% dos testes e critérios forem executados no ambiente com sucesso. O prompt seguinte (`Prompt N+1`) permanece bloqueado enquanto o atual não for formalmente aprovado.
* **Consequências:** Rastreabilidade absoluta, integridade contínua do repositório e ausência de débitos técnicos ocultos.
