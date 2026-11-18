# Especificação Técnica: P012-FE - Interface de Grupos de Monitoramento, Detalhe, Adição via Pesquisa, Confirmação e Timeline

## 1. Visão Geral e Objetivos

O objetivo desta entrega é implementar a interface completa do módulo de **Monitoramento de Produtos & Concorrentes** no frontend, conectada ao backend aprovado em `P012-BE`. A interface permite ao lojista organizar seus produtos e concorrentes em grupos isolados por tenant, acompanhar o histórico de preços com integridade temporal comprovada, detectar lapsos ou atrasos de coleta (*gaps* temporais), adicionar anúncios diretamente da pesquisa autorizada e excluir registros sob confirmação explícita com retenção legal.

---

## 2. Requisitos Funcionais e Critérios de Aceite

### Critério 1: Tela de Grupos de Monitoramento
- Listagem paginada e filtrável de grupos do tenant ativo.
- Exibição de: Nome, descrição, badge de cor visual, intervalo de coleta (`minIntervalMinutes`), período de retenção (`retentionDays`), status (`ACTIVE`, `PAUSED`), e contagem de itens monitorados.
- Criação e edição de grupos através de modal dedicado (`CreateGroupModal.tsx`).
- Estados vazios (*Empty State*) acessíveis quando nenhum grupo estiver cadastrado.

### Critério 2: Tela de Detalhes do Grupo
- Visualização do grupo com metadados (intervalo, retenção, status, datas).
- Listagem dos itens monitorados pertencentes ao grupo:
  - Canal (`MERCADO_LIVRE`, `SHOPEE`).
  - ID externo do anúncio e link seguro com `rel="noopener noreferrer"`.
  - Título e vendedor (quando aplicável).
  - Preço mais recente e moeda.
  - Data de inclusão (`createdAt`).
  - Data da primeira coleta efetiva (`firstSnapshotAt`): exibe "Aguardando 1ª coleta" quando nula.
  - Data da última coleta (`lastSnapshotAt`).
  - Status do item (`ACTIVE`, `PAUSED`).
- Filtros dinâmicos: marketplace, status e termo de busca.

### Critério 3: Adicionar Item a partir da Pesquisa de Mercado
- Modal `AddItemToGroupModal.tsx` com duas abas/modos:
  1. **Pesquisa Autorizada Integrada:** reutiliza a busca pública autorizada do backend (`GET /api/v1/market-search`), permitindo pesquisar por termo, selecionar um anúncio concorrente ou próprio e adicioná-lo ao grupo com preenchimento automático de campos.
  2. **Inserção Direta:** formulário manual com canal, ID externo, título, vendedor e link.
- Prevenção de duplicidade: captura HTTP 409 e informa ao usuário que o anúncio já está no grupo.

### Critério 4: Remoção com Confirmação e Aviso de Retenção
- Modal acessível `ConfirmDeleteModal.tsx` com `role="dialog"`, `aria-modal="true"` e fechamento por tecla ESC.
- Explicação clara dos efeitos da remoção:
  - Interrupção imediata de coletas agendadas.
  - Retenção obrigatória dos snapshots históricos coletados para fins de auditoria (durante o prazo configurado, padrão 90 dias).

### Critério 5: Timeline Cronológica com Detecção de Gaps e Última Coleta
- Drawer lateral `MonitoringTimelineDrawer.tsx`:
  - Cartão de destaque da **Última Coleta**: data/hora, tempo decorrido relativo, preço registrado e status.
  - Disparo manual de coleta imediata ("Coletar Agora") com feedback de quotas e janela mínima.
  - Sequência cronológica de snapshots ordenados (`observedAt`).
  - **Sinalização Explícita de Gaps Temporais:** quando o intervalo entre duas coletas consecutivas exceder o intervalo esperado (ex.: $> 2 \times$ `minIntervalMinutes`), renderiza um bloco de alerta visual explicando a lacuna (coleta pulada por rate-limit, kill switch ou inatividade), **sem qualquer fabricação ou interpolação artificial de dados**.
  - Nota de transparência: o histórico começa estritamente no primeiro snapshot pós-inclusão.

### Critério 6: Responsividade e Acessibilidade (WCAG 2.2 AA)
- Suporte a Breakpoints: Mobile (360px+), Tablet (768px+) e Desktop (1024px+ e 1440px+).
- Alternância entre visualização em Tabela (desktop) e Cards (mobile).
- Touch targets $\ge 44\text{px}$ em todos os botões e links.

---

## 3. Arquitetura de Componentes e Rotas BFF

### 3.1 Rotas BFF Next.js 15
- `GET /api/monitoring/groups`: lista grupos do tenant via `GET /api/v1/monitoring/groups`.
- `POST /api/monitoring/groups`: cria grupo via `POST /api/v1/monitoring/groups`.
- `GET /api/monitoring/groups/[id]`: consulta grupo por ID via `GET /api/v1/monitoring/groups/{id}`.
- `PUT /api/monitoring/groups/[id]`: atualiza grupo via `PUT /api/v1/monitoring/groups/{id}`.
- `DELETE /api/monitoring/groups/[id]`: exclui logicamente o grupo via `DELETE /api/v1/monitoring/groups/{id}`.
- `GET /api/monitoring/groups/[id]/items`: lista itens do grupo via `GET /api/v1/monitoring/groups/{id}/items`.
- `POST /api/monitoring/groups/[id]/items`: adiciona item ao grupo via `POST /api/v1/monitoring/groups/{id}/items`.
- `DELETE /api/monitoring/groups/[id]/items/[itemId]`: remove item do grupo via `DELETE /api/v1/monitoring/groups/{id}/items/{itemId}`.
- `GET /api/monitoring/items/[itemId]/snapshots`: recupera snapshots ordenados do item via `GET /api/v1/monitoring/items/{itemId}/snapshots`.
- `POST /api/monitoring/items/[itemId]/trigger`: dispara coleta sob demanda via `POST /api/v1/monitoring/items/{itemId}/trigger-snapshot`.

### 3.2 Componentes React
- `frontend/src/components/ProductMonitoringView.tsx`: componente principal integrado ao `AppShell`.
- `frontend/src/components/MonitoringTimelineDrawer.tsx`: gaveta de timeline e histórico.
- `frontend/src/components/AddItemToGroupModal.tsx`: modal para inclusão de itens.
- `frontend/src/components/CreateGroupModal.tsx`: modal para criação/edição de grupos.
- `frontend/src/components/ConfirmDeleteModal.tsx`: modal de confirmação acessível.
