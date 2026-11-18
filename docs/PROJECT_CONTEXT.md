# Contexto Geral do Projeto — Seller Intelligence Platform

> **Versão:** 1.0.0 &middot; **Status:** Baseline de Governança Aprovado  
> **Data:** 17/11/2026  
> **Metodologia:** Spec-Driven Development (SDD) com Quality Gates Sequenciais  
> **Referência Primária:** [docs/PRD.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/PRD.md)  

---

## 1. Visão do Produto e Contexto de Negócio

A **Seller Intelligence Platform** é uma solução SaaS multi-tenant concebida para vendedores, operadores e gestores de e-commerce que atuam em múltiplos canais de venda. O sistema consolida inteligência de mercado, análise de concorrência, monitoramento de produtos, analytics de anúncios próprios, precificação estratégica, lucratividade determinística e recursos de inteligência artificial assistiva.

A plataforma inspira-se nas capacidades públicas observadas no ecossistema Avantpro, estabelecendo um compromisso inegociável de **não copiar código, identidade visual, conteúdo proprietário ou mecanismos internos**. Toda a engenharia é baseada em integrações autorizadas, dados licenciados, APIs oficiais e fontes públicas permitidas pelos termos de uso dos marketplaces.

### 1.1 O Problema
Vendedores que operam em múltiplos marketplaces (como Mercado Livre, Shopee, Amazon) sofrem com:
- **Dados Fragmentados:** Falta de uma visão unificada de catálogo, pedidos e rentabilidade real por CNPJ, loja e canal.
- **Opacidade Histórica:** Pouca visibilidade temporal de alterações de preço e reputação de concorrentes.
- **Risco de Prejuízo Silencioso:** Dificuldade extrema em apurar com exatidão taxas de comissão, frete subsidiado, custos de embalagem e alíquotas tributárias, vendendo produtos no prejuízo por engano.
- **Risco de Bloqueio de Contas:** Uso de ferramentas amadoras de scraping hostil que violam termos de uso das plataformas, colocando em risco a operação comercial do vendedor.

### 1.2 Proposta de Valor
A plataforma entrega respostas auditáveis e determinísticas para:
1. **O que vender?** Oportunidades de produtos e categorias com demanda comprovada e rotulagem explícita de evidência.
2. **Quem são os concorrentes?** Snapshots históricos legítimos de preços, reputação e atributos observáveis.
3. **Qual a rentabilidade real?** Margem de contribuição precisa calculada em centavos via motor determinístico em `BigDecimal`, separando receita bruta, deduções, taxas, impostos e custos operacionais.
4. **Como otimizar anúncios?** Diagnóstico de qualidade em 4 dimensões (título, descrição, ficha técnica, fotos) com IA assistiva sob guardrails anti-alucinação.

---

## 2. Personas e Cenários de Uso

### Persona A — Seller Individual
- **Perfil:** Opera uma ou poucas lojas nos marketplaces; gerencia todas as etapas da operação.
- **Necessidades:** Descobrir produtos com margem garantida, acompanhar preços da concorrência e evitar taxas ocultas.

### Persona B — Operador de E-commerce
- **Perfil:** Gerencia catálogos extensos, múltiplos canais e alto volume diário de pedidos.
- **Necessidades:** Sincronização automatizada e confiável de anúncios e pedidos, alertas rápidos de divergência e controle de estoque centralizado.

### Persona C — Gestor Financeiro / Comercial
- **Perfil:** Responsável pelo fechamento contábil e apuração de resultados multi-empresa e multi-CNPJ.
- **Necessidades:** DRE gerencial consistente (visão Caixa vs. Competência), conciliação de repasses de marketplaces e memória de cálculo explicável.

### Persona D — Analista de Mercado
- **Perfil:** Especialista focado em precificação, inteligência de categorias e tendências de consumo.
- **Necessidades:** Análise temporal fundamentada, distinção clara entre dado oficial e estimativa estatística, sem métricas falsas de vendas de concorrentes.

---

## 3. Escopo Funcional do MVP vs. Não-Escopo

```
┌────────────────────────────────────────────────────────────────────────┐
│                        ESCOPO DO MVP (Fase 1)                         │
├────────────────────────────────┬───────────────────────────────────────┤
│ Integrações Prioritárias       │ Mercado Livre e Shopee (APIs oficiais)│
│ Multi-tenancy & Segurança      │ Isolamento estrito por tenant, RLS    │
│ Sincronização Própria          │ Catálogo e pedidos próprios autorizados│
│ Inteligência de Mercado        │ Busca normalizada com proveniência    │
│ Monitoramento de Produtos      │ Snapshots históricos pós-inclusão     │
│ Motor Financeiro               │ Margem determinística em BigDecimal   │
│ Analytics Básico               │ Métricas oficiais disponíveis na API  │
│ IA Assistiva                   │ Tool calling somente leitura, rascunhos│
│ Relatórios & Exportação        │ Painéis e exportação CSV/XLSX auditada│
└────────────────────────────────┴───────────────────────────────────────┘
```

### Não-Objetivos do MVP (Escopo Expressamente Proibido nesta Fase)
1. **Planos, Cobrança e Billing SaaS:** Não implementar gateways de pagamento ou gestão de assinaturas do software.
2. **Administração Funcional de Equipes:** Não implementar telas complexas de convites, hierarquia de assentos e gerenciamento funcional de colaboradores (a segurança/autorização mínima do tenant, contudo, é obrigatória e imediata).
3. **Operações de Escrita em Marketplaces:** **Proibido** publicar ou editar anúncios, alterar preços, aceitar promoções, impulsionar produtos, enviar mensagens ou seguir usuários no MVP.
4. **Scraping Hostil e Evasão:** **Proibido** qualquer contorno de CAPTCHAs, spoofing de WAF, rotação de IPs para evasão de rate limits ou scraping de áreas autenticadas.
5. **Métricas Falsas de Concorrentes:** **Proibido** inventar números de vendas, faturamento ou CTR de concorrentes quando a API oficial não fornecer esses dados.
6. **Substituição de ERP/Fiscal:** O sistema não emite notas fiscais nem substitui a escrituração contábil legal.

---

## 4. Metodologia de Trabalho: Spec-Driven Development (SDD)

O desenvolvimento segue um fluxo rigoroso orientado a especificações e gates sequenciais:

1. **Preflight:** Leitura mandatória da documentação e confirmação de que o prompt anterior está formalmente `PASSED`.
2. **Especificação (Spec):** Toda feature possui especificação detalhada em `docs/specs/<feature-id>/` com contratos OpenAPI/DTOs, modelos de dados, regras de negócio e cenários Gherkin antes de qualquer código.
3. **Implementação Guiada:** Separação estrita Backend $\rightarrow$ Frontend. O frontend só inicia após a suíte backend estar 100% verde.
4. **Validação e Evidência:** Execução de testes unitários, integração (Testcontainers), arquitetura (ArchUnit) e segurança cross-tenant. Nenhum gate é aprovado sem logs e evidências reais.
5. **Máquina de Estados:** `NOT_STARTED` $\rightarrow$ `READY` $\rightarrow$ `IN_PROGRESS` $\rightarrow$ `BLOCKED` / `FIXING` $\rightarrow$ `PASSED`.
