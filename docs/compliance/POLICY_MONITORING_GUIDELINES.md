# Diretrizes de Monitoramento Contínuo de Políticas de Marketplaces (Policy Monitoring Guidelines)

## 1. Visão Geral e Princípios Fundamentais
A integridade operacional das integrações da **Seller Intelligence Platform** com os marketplaces parceiros (**Mercado Livre, Shopee, Amazon, TikTok Shop, Magalu e SHEIN**) depende do respeito contínuo e determinístico aos termos de uso, diretrizes de desenvolvedores e limites técnicos de API.

O monitoramento contínuo de políticas tem como objetivos centrais:
1. **Detecção Antecipada:** Identificar alterações de termos, depreciações de endpoints ou modificações contratuais antes que causem impactos ou suspensões.
2. **Fail Closed Determinístico:** Quando uma alteração regulatória ou de segurança não puder ser avaliada com certeza, a plataforma bloqueia preventivamente apenas a funcionalidade vulnerável.
3. **Princípio do Menor Escopo (Least Scope Enforcement):** Isolar o bloqueio ou mitigação exclusivamente à funcionalidade impactada, preservando os fluxos operacionais legítimos e saudáveis.
4. **Governança Ética e Respeito aos Termos:** Executar monitoramento estritamente via canais públicos autorizados, documentações técnicas e APIs oficiais, sem jamais violar controles técnicos ou diretrizes de acesso.

---

## 2. Frequências e Cadências de Monitoramento
As fontes oficiais monitoradas dividem-se em quatro categorias operacionais com cadências específicas:

| Categoria da Fonte | Frequência Base | Modo de Execução | Gatilhos de Antecipação |
|---|---|---|---|
| **API Changelogs & Release Notes** | Diária (03:00 UTC) | Automatizado (`PolicyMonitoringEngine`) | Publicação de webhook de release ou anúncio oficial em canal de desenvolvedores |
| **Developer Agreements & Terms of Service** | Diária (04:00 UTC) | Automatizado (`PolicyMonitoringEngine`) com hash SHA-256 | Notificação prévia por e-mail institucional de canal |
| **Políticas de Privacidade & Dados (LGPD / GDPR)** | Semanal (Domingos 05:00 UTC) | Automatizado com triagem semi-assistida | Alerta regulatório setorial ou atualização legal publicada |
| **Revisão Manual & Auditoria Formal de Conformidade** | Mensal (1º dia útil) | Conduzida por Compliance Officer + Tech Lead | Incidente crítico anterior ou alteração de tier de integração |

---

## 3. Diretrizes Éticas e Regras Técnicas de Consulta

### 3.1. Proibições Terminantes
O motor de conformidade contínua e qualquer operador ou script da plataforma são **expressamente proibidos** de:
- Executar bypass, tentativa de evasão ou contorno de CAPTCHA, Cloudflare Turnstile, hCaptcha ou Proof-of-Work gates.
- Desviar ou violar proteções WAF, rate limits ou regras declaradas em arquivos `robots.txt`.
- Forjar sessões autenticadas de usuários, roubar cookies de navegador ou executar web scraping de áreas restritas que exijam login de usuário final.
- Submeter cargas volumétricas abusivas ou concorrentes contra a infraestrutura pública de documentação dos marketplaces.

### 3.2. Padrões de Requisição HTTP
Toda requisição realizada pelo `PolicyMonitoringEngine` deve obedecer aos seguintes padrões:
- **Método HTTP:** Estritamente `GET` ou `HEAD`.
- **Cabeçalho User-Agent:** Identificação formal e transparente da plataforma, ex.:
  `SellerIntelligencePlatform-ComplianceBot/1.0 (+https://sellerintelligence.com.br/compliance-bot; security@sellerintelligence.com.br)`
- **Cabeçalho Accept:** `text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8`
- **Controle de Taxa:** Intervalo mínimo de 5 segundos entre requisições para o mesmo domínio de marketplace.
- **Timeout Rígido:** Timeout de conexão de 10 segundos e timeout de leitura de 15 segundos.

---

## 4. Proteção Anti-SSRF e Validação de Origens
Para impedir ataques de Server-Side Request Forgery (SSRF) e injeções maliciosas em fontes de monitoramento, o `UrlSecurityValidator` aplica validação estrita prévia:

1. **Protocolo Permitido:** Exclusivamente `HTTPS`. Protocolos `HTTP`, `FTP`, `FILE`, `GOPHER` ou customizados são rejeitados.
2. **Allowlist Estrita de Domínios Oficiais:**
   - Mercado Livre: `*.mercadolivre.com`, `*.mercadolibre.com`, `developers.mercadolivre.com.br`
   - Shopee: `*.shopee.com`, `open.shopee.com`, `open.shopee.com.br`
   - Amazon: `*.amazon.com`, `developer-docs.amazon.com`, `sellercentral.amazon.com.br`
   - TikTok Shop: `*.tiktok.com`, `partner.tiktokshop.com`
   - Magazine Luiza: `*.magalu.com`, `developers.magalu.com`, `integradores.magazineluiza.com.br`
   - SHEIN: `*.shein.com`, `open.shein.com`
3. **Bloqueio de Redes Locais e Metadados de Nuvem:**
   - Bloqueio de `localhost`, `127.0.0.1`, `::1`.
   - Bloqueio de faixas privadas RFC 1918 (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`).
   - Bloqueio de Link-Local e Cloud Metadata (`169.254.169.254`).

---

## 5. Normalização de Conteúdo e Prevenção de Falsos-Positivos
Modificações puramente estéticas (CSS, timestamps de renderização dinâmica, espaçamentos ou hashes de bundles de assets) não devem disparar alarmes falsos nem bloquear integrações.

O `PolicyChangeDetector` implementa o seguinte algoritmo de processamento:
1. **Sanitização de Marcação:** Remoção de tags `<script>`, `<style>`, `<nav>`, `<footer>` e comentários HTML.
2. **Normalização de Whitespace:** Conversão de quebras de linha e tabulações múltiplas em espaços simples unificados.
3. **Classificação Semântica:**
   - Se o hash SHA-256 for idêntico: **Sem Alteração** (`NO_CHANGE`).
   - Se o texto normalizado não tiver variação de palavras-chave contratuais ou regulatórias: classificado como `FORMATTING_ONLY` com severidade `INFORMATIONAL` (não aciona bloqueio).
   - Se houver alteração substantiva em cláusulas contratuais, limites de rate limit, escopos de permissões ou termos de proibição de dados: classificado como `SUBSTANTIVE` ou `BREAKING_TECHNICAL` e submetido imediatamente ao `ComplianceImpactAnalyzer`.

---

## 6. Procedimento de Contingência: Fallback para Revisão Manual
Se um domínio oficial de marketplace retornar códigos HTTP `403 Forbidden`, `429 Too Many Requests`, `5xx Server Error` ou falhar consecutivamente por problemas de rede:
1. **Não insistir em loop:** O motor não deve realizar retries agressivos.
2. **Marcação de Estado:** O status da política monitorada é transitado para `SOURCE_UNAVAILABLE`.
3. **Abertura de Tarefa de Revisão Manual:** É aberto um registro no painel de conformidade e disparada uma notificação técnica para a equipe de Engenharia de Integrações.
4. **Consulta Humana Segura:** Um analista de conformidade acessa a documentação oficial via navegador interativo institucional, faz o download do novo termo, calcula o hash SHA-256 e efetua a atualização manual no registro via painel administrativo com trilha de auditoria.
