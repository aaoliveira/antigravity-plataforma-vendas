# Extensão Chrome / Chromium (Manifest V3) — Seller Intelligence Platform

Esta extensão é uma interface contextual complementar que conecta a navegação do vendedor no **Mercado Livre** e **Shopee** à **Seller Intelligence Platform**.

---

## 1. Princípios Arquiteturais e de Privacidade

Em estrita conformidade com as regras do PRD (Seções 1.2, 4.2, 5.14, 8.2 e 12.4) e as decisões de arquitetura **ADR DEC-012** e **ADR DEC-018**:

1. **Permissões Mínimas Estritas (Princípio do Menor Privilégio):**
   - `activeTab`: Ativada **única e exclusivamente** quando o usuário clica no ícone da extensão. Permite identificar o identificador canônico público da página atual (ex.: `MLB12345678` ou `item_id` da Shopee) sem monitorar o histórico do usuário nem rodar em segundo plano.
   - `storage`: Usado estritamente para estado de sessão em memória (`chrome.storage.session`) e preferências locais de interface.
   - **Sem `<all_urls>` ou permissões abusivas:** Não solicita acesso irrestrito à web.

2. **Zero Coleta Hostil e Anti-Scraping:**
   - **Nenhum cookie de terceiros é lido ou transmitido:** Cookies de autenticação do marketplace e senhas são inacessíveis pela extensão.
   - **Nenhuma automação externa:** A extensão é 100% passiva; é terminantemente proibida qualquer automação de compra, checkout, alteração de preço ou publicação de anúncios no marketplace.
   - **Ação explícita do usuário:** Nenhum dado é transferido sem que o usuário clique explicitamente nos botões de inspeção no popup.

3. **Defesa em Profundidade:**
   - Páginas sensíveis (ex.: checkout, carrinho, páginas de conta/pagamento) são rejeitadas preventivamente pelo parser e pela guarda de segurança (`antiScrapingGuard`).

---

## 2. Estrutura do Projeto

```text
extension/
├── public/
│   └── manifest.json         # Manifest V3 declarativo
├── src/
│   ├── background/
│   │   └── serviceWorker.ts  # Service worker para abertura de abas e verificação de saúde
│   ├── content/
│   │   └── contentScript.ts  # Script contextual mínimo em document_idle
│   ├── parsers/
│   │   ├── marketplaceUrlParser.ts      # Extração canônica de IDs de produto ML/Shopee
│   │   └── marketplaceUrlParser.test.ts # Testes unitários do parser
│   ├── popup/
│   │   ├── Popup.tsx         # Interface React do assistente
│   │   ├── Popup.test.tsx    # Testes unitários de interface e cliques
│   │   └── main.tsx          # Ponto de montagem React no DOM
│   ├── security/
│   │   ├── antiScrapingGuard.ts         # Regras ativas contra ações proibidas
│   │   └── antiScrapingGuard.test.ts    # Testes de bloqueio de segurança
│   └── types/
│       └── index.ts          # Contratos tipados de mensagens e páginas
├── popup.html                # Entrypoint HTML do popup
├── vite.config.ts            # Build Vite multi-entry para Manifest V3
└── package.json
```

---

## 3. Instruções de Desenvolvimento e Build

### Instalação de Dependências
```bash
cd extension
pnpm install
```

### Executar Testes Automatizados
```bash
pnpm test
```

### Compilar para Produção (Gerar `dist/`)
```bash
pnpm build
```
O diretório `dist/` conterá o `manifest.json`, `popup.html`, `serviceWorker.js`, `contentScript.js` e assets prontos para carregar no navegador.

---

## 4. Como Carregar a Extensão no Chrome / Chromium

1. Abra o navegador Google Chrome, Chromium ou Brave e acesse `chrome://extensions/`.
2. No canto superior direito, ative a opção **Modo do desenvolvedor** (Developer mode).
3. Clique no botão **Carregar sem compactação** (Load unpacked).
4. Selecione a pasta `extension/dist` deste repositório.
5. O ícone da **Seller Intelligence Platform** aparecerá na barra de ferramentas do navegador.
6. Ao abrir qualquer produto no Mercado Livre ou Shopee, clique no ícone para inspecionar e abrir no **AI Listing Studio** ou **Pesquisa de Mercado**.
