# Questões Abertas e Dúvidas Bloqueantes (Open Questions Log)

> **Documento de Governança e Rastreamento de Incertezas** &middot; **Versão:** 1.0.0  
> **Status:** Ativo &middot; **Data:** 17/11/2026  
> **Regra de Ouro:** Dúvidas de segurança, fiscais ou de APIs bloqueiam a implementação das partes dependentes (`UNVERIFIED` / `BLOCKED`).  

---

## 1. Tabela de Questões em Aberto

| ID | Título da Questão | Severidade | Módulo Impactado | Status | Bloqueador para MVP? |
|---|---|---|---|---|---|
| **OQ-001** | Homologação de Credenciais de Desenvolvedor no Mercado Livre | **Alta** | `marketplace-connection`, `marketplace-adapters` | `RESOLVIDO_PARAMETRIZADO` | Não (Fluxo OAuth 2.0 PKCE implementado; variáveis de ambiente configuráveis para sandbox/prod) |
| **OQ-002** | Acesso e Credenciais da Shopee Open Platform | **Alta** | `marketplace-connection`, `marketplace-adapters` | `AGUARDANDO_HUMANO` | Não para P001/P002; Sim para P003 (Conexões) |
| **OQ-003** | Endpoints Oficiais Permitidos para Pesquisa Pública na Shopee | **Média** | `market-intelligence` | `UNVERIFIED` | Sim para pesquisa concorrencial Shopee |
| **OQ-004** | Parametrização Tributária Detalhada (Anexos do Simples Nacional) | **Média** | `pricing`, `profitability` | `DOCUMENTADO_PRELIMINAR`| Não (usa tabela padrão configurável) |
| **OQ-005** | Homologação do Provedor de LLM para o Assistente IA | **Baixa** | `ai-assistant` | `AGUARDANDO_HUMANO` | Não para P001-P009; Sim para P010 (IA) |

---

## 2. Detalhamento das Questões

### OQ-001: Homologação de Credenciais de Desenvolvedor no Mercado Livre
* **Descrição:** Para autenticar lojas reais via OAuth 2.0 PKCE, é necessário cadastrar uma aplicação no portal [Mercado Livre Developers](https://developers.mercadolibre.com.br) e obter `App ID`, `Secret Key` e configurar a `Redirect URI` autorizada.
* **Impacto:** Durante os primeiros prompts de fundação (P001 e P002), o sistema utiliza contratos e mocks formais nos testes de integração. No entanto, para o Prompt P003 (Conexão Oficial com Mercado Livre), as credenciais de homologação/sandbox serão indispensáveis.
* **Ação Recomendada:** O Tech Lead / Usuário deve providenciar uma conta ativa no programa de desenvolvedores do Mercado Livre e cadastrar o aplicativo da plataforma antes da execução do Prompt P003.

---

### OQ-002: Acesso e Credenciais da Shopee Open Platform
* **Descrição:** A Shopee Open Platform exige aprovação de perfil de parceiro para emissão de `Partner ID` e `Partner Key`, utilizados na geração das assinaturas HMAC-SHA256.
* **Impacto:** Conector da Shopee depende dessas chaves para validar a comunicação em ambiente de sandbox ou produção.
* **Ação Recomendada:** O responsável pelo projeto deve submeter a solicitação de desenvolvedor na Shopee Open Platform ([open.shopee.com](https://open.shopee.com)).

---

### OQ-003: Endpoints Oficiais Permitidos para Pesquisa Pública na Shopee
* **Descrição:** A Shopee restringe a maior parte de seus endpoints de API para o escopo de itens da própria loja do parceiro (`shop_id`). É necessário confirmar se a documentação oficial vigente disponibiliza endpoints para consulta de dados públicos de mercado sem exigir seller token ativo.
* **Impacto:** Se a API oficial não oferecer busca pública aberta, a capacidade de pesquisa de mercado na Shopee será formalmente classificada como `UNSUPPORTED`.
* **Ação Recomendada:** Manter a busca Shopee como `RESTRICTED` / `UNVERIFIED` até confirmação documental e **não implementar web scraping não autorizado**.

---

### OQ-004: Parametrização Tributária Detalhada (Anexos do Simples Nacional)
* **Descrição:** Vendedores enquadrados no Simples Nacional possuem alíquotas efetivas variáveis em função da receita bruta acumulada nos últimos 12 meses (RBT12), divididos entre Anexo I (comércio) e deduções específicas.
* **Impacto:** O motor financeiro precisa permitir a parametrização explícita da alíquota pelo usuário (ex.: 4%, 6.5%, etc.) sem assumir cálculos fiscais genéricos como verdade contábil absoluta.
* **Decisão Adotada:** O sistema disponibilizará configuração explícita de alíquota por CNPJ e produto, exibindo a memória de cálculo com a premissa informada pelo usuário.

---

### OQ-005: Homologação do Provedor de LLM para o Assistente IA
* **Descrição:** O módulo `ai-assistant` precisará se conectar a uma API de modelo de linguagem (ex.: Google Gemini, OpenAI ou Anthropic) para responder a dúvidas operacionais via Tool Calling.
* **Impacto:** Nenhuma dependência imediata nos prompts de fundação (P001 a P009).
* **Ação Recomendada:** Definir qual provedor e credencial de API será configurada antes de iniciar o Prompt P010.
