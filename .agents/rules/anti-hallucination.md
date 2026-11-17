# Regras Anti-Alucinação, Evidências e Integridade

## 1. Princípio Fundamental
A Seller Intelligence Platform não admite dados, integrações, fórmulas fiscais, métricas de concorrentes ou resultados de testes forjados ou presumidos. Decisões técnicas e de produto devem se apoiar em fatos verificáveis e na hierarquia de evidências estabelecida.

---

## 2. Hierarquia Rígida de Evidências
Ao analisar requisitos, resolver conflitos ou projetar soluções, siga rigorosamente esta ordem de precedência:
1. **PRD oficial:** [docs/product/PRD.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/product/PRD.md).
2. **Decisões aprovadas:** [docs/architecture/DECISION_LOG.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/architecture/DECISION_LOG.md) e ADRs aprovados.
3. **Critérios de aceite:** requisitos e critérios do prompt corrente e prompts anteriores formalmente aprovados.
4. **Código, contratos e testes existentes:** suítes de teste executadas e contratos de API implementados.
5. **Documentação oficial de fornecedores:** documentação técnica vigente de APIs de marketplaces (Mercado Livre, Shopee, Amazon), frameworks (Spring Boot, Spring Security, Flyway, Next.js) e PostgreSQL/Redis/RabbitMQ.
6. **Hipóteses registradas:** premissas explicitamente catalogadas e validadas por decisão humana.

---

## 3. Proibições Absolutas
- **Nunca invente endpoints, escopos OAuth, payloads ou limites de API:** Se um endpoint do Mercado Livre ou Shopee não for comprovado pela documentação oficial ou sandbox autorizado, não crie contratos presumidos.
- **Nunca apresente dados sintéticos como reais:** Mocks, fixtures, seeds ou dados de teste devem ser visivelmente rotulados como tal e restritos aos ambientes de `dev` e `test`. Jamais simule integração externa em produção.
- **Nunca transforme `UNAVAILABLE` em zero:** Ausência de dados de Ads, vendas, estoque, taxas ou repasses significa dado não fornecido/indisponível. Transformar valor nulo/ausente em 0 distorce métricas de rentabilidade e confunde o usuário.
- **Nunca apresente dados privados de concorrentes sem fonte autorizada:** Vendas reais, CTR, conversão, receita ou estoque de concorrentes só podem ser exibidos se disponibilizados legitimamente pelo canal ou fonte oficial permitida. Estimativas devem ser categoricamente rotuladas.
- **Nunca calcule valores monetários via LLM ou `float`/`double`:** Cálculos de margem de contribuição, comissões, frete, impostos e rentabilidade devem ser executados por um motor determinístico em Java usando `BigDecimal`, com escala e arredondamento explicitados em regra versionada.
- **Nunca use scraping hostil ou burlagem de defesas:** É expressamente proibido desenvolver scrapers que contornem login, CAPTCHA, paywalls, rate limits, Cloudflare, Web Application Firewalls ou termos de serviço dos marketplaces. Priorizar APIs oficiais, dados do próprio seller, arquivos importados (CSV/XLSX) e provedores devidamente licenciados.
- **Nunca invente atributos de produtos na IA:** O módulo de geração de anúncios (AI Listing Studio) só pode utilizar fatos, atributos, medidas, materiais e garantias confirmados pelo seller ou pelo catálogo de referência. Não crie certificações (como Inmetro, Anvisa) ou características técnicas não comprovadas.
- **Nunca exponha segredos ou dados pessoais:** Tokens de acesso, refresh tokens, chaves criptográficas, credenciais OAuth, dados pessoais de compradores (LGPD) e dados financeiros confidenciais jamais devem vazar em logs, respostas de IA, payloads de extensão ou testes.
- **Nunca declare funcionalidade concluída sem teste executado e evidência:** A mera existência de código sem suíte de testes correspondente executada e documentada é nula.
- **Nunca altere escopo, stack ou contratos públicos por conveniência:** Modificações de arquitetura, banco, bibliotecas ou escopo exigem registro prévio de ADR e aprovação formal.

---

## 4. Classificação Obrigatória de Proveniência de Dados
Todo indicador, métrica e dado exibido na plataforma ou manipulado nas APIs deve carregar obrigatoriamente metadados de proveniência:
1. `source`: Identificador da fonte de dados (ex.: API Oficial Mercado Livre, Conector Shopee, Importação CSV do Seller, Provedor Licenciado).
2. `collectedAt`: Timestamp ISO 8601 do exato instante de coleta ou processamento.
3. `scope`: Escopo da métrica (organização, empresa, loja, SKU, categoria).
4. `classification`:
   - `OBSERVED`: Dado público ou direto coletado legitimamente sem transformação heurística.
   - `IMPORTED`: Dado fornecido diretamente pelo seller através de conta conectada ou arquivo enviado.
   - `ESTIMATED`: Dado inferido através de metodologia estatística documentada (deve acompanhar indicador de confiança e metodologia resumida).
   - `AI_GENERATED`: Conteúdo, texto ou score formulado por modelo de IA generativa (claramente demarcado como sugestão sujeita a revisão).
   - `UNAVAILABLE`: Dado inexistente, não suportado pela API ou indisponível no momento.

---

## 5. Tratamento de Ambiguidade e Protocolo de Bloqueio
Se surgir qualquer conflito, regra fiscal ambígua, endpoint incerto ou requisito faltante:
1. **Pare imediatamente a implementação da parte dependente.**
2. Registre a ocorrência em [docs/product/OPEN_QUESTIONS.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/product/OPEN_QUESTIONS.md) contendo:
   - ID da questão (`OQ-xxx`).
   - Descrição detalhada da ambiguidade.
   - Impacto na arquitetura, segurança, financeiro ou negócio.
   - Evidências e documentações oficiais já consultadas.
   - Decisão necessária e alternativas possíveis.
3. Se a ambiguidade bloquear segurança, integridade financeira, conformidade jurídica (LGPD) ou integração de dados, altere o status em [docs/delivery/PHASE_STATUS.md](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/delivery/PHASE_STATUS.md) e [docs/delivery/phase-gate.json](file:///home/alexandre/Projetos/antigravity-avantepro-clone/docs/delivery/phase-gate.json) para `BLOCKED` e requisite intervenção humana. Não invente solução paliativa silenciosa.
