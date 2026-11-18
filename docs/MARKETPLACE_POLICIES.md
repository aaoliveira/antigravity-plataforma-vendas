# Políticas de Integração Oficial e Matriz de Capacidades de Marketplaces

> **Documento de Conformidade e Proteção de Contas** &middot; **Versão:** 1.0.0  
> **Status:** Ativo &middot; **Data:** 17/11/2026  
> **Compromisso:** Risco mínimo para contas de vendedores, uso estrito de APIs oficiais e política Fail-Closed.  

---

## 1. Princípios de Proteção de Contas e Legalidade

A proteção da reputação e das credenciais comerciais dos vendedores conectados é um requisito de negócio e arquitetura de primeira grandeza. 
A plataforma assume como premissa que **nenhuma funcionalidade comercial justifica colocar a conta do seller em risco de restrição, penalização algorítmica ou suspensão definitiva**.

### 1.1 Estados de Capacidade dos Adaptadores
Cada recurso de integração exposto por um conector de marketplace é formalmente catalogado em um dos quatro estados:
- **`SUPPORTED`:** Capacidade oficialmente suportada pela API do marketplace, com escopos homologados, endpoints ativos e limites de taxa conhecidos e respeitados.
- **`RESTRICTED`:** Capacidade tecnicamente possível, porém sujeita a termos rígidos, aprovação prévia do canal ou limites estritos de volume (ex.: pesquisa pública com paginação limitada).
- **`UNSUPPORTED`:** Capacidade não disponibilizada oficialmente pelo marketplace. **É proibido tentar obter esse dado por scraping hostil, engenharia reversa ou injeção em navegador.** O sistema deve exibir explicitamente "Dado Indisponível" na interface.
- **`UNVERIFIED`:** Capacidade ainda não validada formalmente com credenciais reais de desenvolvedor ou documentação oficial atualizada. Permanece bloqueada até comprovação.

---

## 2. Matriz de Capacidades por Marketplace

| Capacidade Funcional | Mercado Livre (`MLB`) | Shopee (`BR`) | Amazon (`SP-API`) | TikTok Shop / Magalu / SHEIN |
|---|---|---|---|---|
| **Autenticação Oficial** | `SUPPORTED` (OAuth 2.0 PKCE) | `SUPPORTED` (Open Platform HMAC) | `UNVERIFIED` (LWA + IAM) | `UNVERIFIED` (Em Homologação) |
| **Leitura de Catálogo Próprio** | `SUPPORTED` (`/users/{id}/items/search`) | `SUPPORTED` (`/api/v2/product/...`) | `UNVERIFIED` | `UNVERIFIED` |
| **Leitura de Pedidos Próprios** | `SUPPORTED` (`/orders/search`) | `SUPPORTED` (`/api/v2/order/...`) | `UNVERIFIED` | `UNVERIFIED` |
| **Pesquisa Pública de Mercado** | `SUPPORTED` (`/sites/MLB/search`) | `RESTRICTED` (Apenas APIs permitidas) | `UNVERIFIED` | `UNSUPPORTED` |
| **Snapshots de Preço Observado** | `SUPPORTED` (Endpoints públicos) | `RESTRICTED` (Itens monitorados) | `UNVERIFIED` | `UNSUPPORTED` |
| **Métricas Oficiais de Anúncios** | `SUPPORTED` (`/items/{id}/health`) | `SUPPORTED` (Analytics de seller) | `UNVERIFIED` | `UNVERIFIED` |
| **Vendas / Faturamento de Concorrentes**| `UNSUPPORTED` (Não fornecido) | `UNSUPPORTED` (Não fornecido) | `UNSUPPORTED` | `UNSUPPORTED` |
| **Operações de Escrita (Preço/Estoque)**| `RESTRICTED` (Desabilitado no MVP)| `RESTRICTED` (Desabilitado no MVP)| `RESTRICTED` (Desabilitado) | `RESTRICTED` (Desabilitado) |

> [!IMPORTANT]
> **Vendas de Concorrentes são UNSUPPORTED:** As APIs oficiais do Mercado Livre e Shopee não fornecem o faturamento exato ou a quantidade de vendas unitárias em tempo real de concorrentes. A plataforma nunca forjará esses números. Toda estimativa de demanda deve ser explicitamente rotulada como `ESTIMATED` com metodologia documentada e índice de confiança.

---

## 3. Especificação dos Canais Prioritários do MVP

### 3.1 Mercado Livre (Mercado Libre Developers)
- **Portal de Desenvolvedores:** [https://developers.mercadolibre.com.br](https://developers.mercadolibre.com.br)
- **Método de Autenticação:** OAuth 2.0 Authorization Code Flow com PKCE.
  - Endpoints de Auth: `https://auth.mercadolivre.com.br/authorization` e `https://api.mercadolibre.com/oauth/token`.
- **Escopos Mínimos do MVP:**
  - `read`: Leitura de perfil do vendedor, anúncios e inventário.
  - `offline_access`: Emissão de `refresh_token` para renovação transparente de sessão a cada 6 horas.
  - *Escopo `write`:* **Excluído** da configuração do MVP.
- **Limites Operacionais & Rate Limiting:**
  - Padrão do canal: aproximadamente 10 a 20 requisições por segundo por token de usuário.
  - Cabeçalho HTTP 429 (`Too Many Requests`): quando recebido, o adaptador extrai o cabeçalho `Retry-After` e suspende chamadas para aquela conta durante o período informado, aplicando jitter aleatório de $\pm 10\%$.
- **Webhooks:** Ingestão de tópicos `orders_v2` e `items` processados via filas RabbitMQ com deduplicação por `resource_id`.

### 3.2 Shopee (Shopee Open Platform)
- **Portal de Desenvolvedores:** [https://open.shopee.com](https://open.shopee.com)
- **Método de Autenticação:** Assinatura criptográfica HMAC-SHA256 por requisição:
  $$\text{sign} = \text{HMAC-SHA256}(\text{partner\_id} + \text{path} + \text{timestamp} + \text{access\_token} + \text{shop\_id}, \text{partner\_key})$$
- **Escopos Mínimos do MVP:**
  - Acesso autorizado no nível de loja (*Shop Level Permission*) para leitura de catálogo e pedidos.
- **Limites Operacionais & Rate Limiting:**
  - Quotas conservadoras gerenciadas pelo conector: máximo 5 chamadas síncronas simultâneas por `shop_id`.
  - Tratamento de janelas móveis com rate limiter distribuído no Redis.

---

## 4. Mecanismos de Resiliência e Fail-Closed

1. **Backoff Exponencial com Full Jitter:**
   $$\text{Sleep} = \text{random}(0, \, \min(\text{cap}, \, \text{base} \times 2^{\text{tentativa}}))$$
   Impede que múltiplos workers sincronizando simultaneamente provoquem o efeito manada (*Thundering Herd*) sobre as APIs dos parceiros.
2. **Circuit Breaker por Loja / Canal:**
   - Se uma conta de marketplace retornar 5 erros consecutivos de autenticação (`HTTP 401/403`) ou falhas sistêmicas (`HTTP 5xx`), o circuito abre (`CIRCUIT_BREAKER_OPEN`), interrompendo imediatamente novas tentativas e notificando o seller no Action Center.
3. **Kill Switch Central:**
   - Propriedade de configuração em tempo real no backend (`seller.connectors.<canal>.enabled = false`) capaz de desligar instantaneamente qualquer conector de marketplace sem necessidade de redeploy da aplicação.
4. **Política Fail-Closed:**
   - Em caso de dúvida, resposta ambígua da API, expiração de tokens ou incidente de segurança, o conector **interrompe o fluxo e falha fechado**, garantindo que nenhuma informação corrompida seja gravada no banco.
