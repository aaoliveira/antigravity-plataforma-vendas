# Especificação Técnica: P021-FE - Central de Relatórios e Downloads Auditados (Seleção, Progresso, Histórico e Download Autorizado)

## 1. Contexto & Objetivos

Esta especificação define o frontend do **P021-FE**, implementando a **Central de Relatórios e Exportações Seguras** da **Seller Intelligence Platform**. A interface permite que os operadores solicitem exportações assíncronas de conjuntos de dados autorizados do seu próprio tenant (`FINANCIAL_FACTS`, `ORDERS`, `CATALOG`, `METRICS`), configurem filtros persistidos, selecionem formatos (`CSV`, `XLSX`), monitorem o progresso do job em tempo real, visualizem o histórico completo com limites e tempos de expiração e efetuem downloads estritamente autorizados através de canais seguros.

### Princípios Inegociáveis & Guardrails Anti-Alucinação:
1. **Zero Links Permanentes Públicos (Regra Estrita de Segurança):**
   - É terminantemente proibido renderizar `<a href="...">` apontando para links públicos permanentes, URLs estáticas de buckets S3 ou endpoints sem autenticação.
   - Todo download é intermediado por uma rota BFF autenticada (`/api/reports/exports/{id}/download?token=...`) que encaminha as credenciais da sessão ativa (`Authorization: Bearer <token>`, `X-Tenant-Id`) para o backend Spring Boot.
   - No navegador, o download é acionado programaticamente via `Blob` em memória (`URL.createObjectURL(blob)`) e o link temporário é revogado imediatamente após o clique (`URL.revokeObjectURL(url)`).
2. **Ciclo de Vida do Job & Estados Explícitos:**
   - A UI mapeia e exibe fielmente os 5 estados do ciclo de vida:
     * `PENDING`: Solicitação recebida e enfileirada no backend.
     * `PROCESSING`: Processamento em lote em andamento. Polling automático ativado.
     * `COMPLETED`: Arquivo gerado com sucesso, contagem de linhas (`totalRows`), sinalização de truncamento (`isTruncated`), tamanho (`fileSizeBytes`), checksum SHA-256 e prazo de expiração (`expiresAt`).
     * `FAILED`: Falha na geração. Mensagem amigável de erro sem vazamento de stacktrace ou PII.
     * `EXPIRED`: Arquivo temporário expirou e foi removido do storage seguro.
3. **Seleção de Colunas e Filtros Combináveis:**
   - Usuário seleciona o dataset alvo:
     * Fatos Financeiros (`FINANCIAL_FACTS`): Receita, custos, taxas, comissões, frete, margem.
     * Pedidos de Venda (`ORDERS`): Pedidos, itens, canais, status, valores.
     * Catálogo & Anúncios (`CATALOG`): SKUs, títulos, canais, preços, categorias, status.
     * Métricas Oficiais (`METRICS`): Visitas, pedidos, receita de anúncios, conversão documentada.
   - Filtros persistidos: Período (`startDate` / `endDate`), Marketplace (`MERCADO_LIVRE` / `SHOPEE`), Conta e Status.
   - Limite de linhas configurável (`maxRows`: de 100 até 50.000).
   - Visualização prévia das colunas que compõem o dataset selecionado.
4. **Consulta Mobile & Acessibilidade (WCAG 2.1 AA):**
   - Operadores em dispositivos móveis ($\ge 360$px) podem consultar status, progresso e histórico em formato de cards responsivos sem scroll horizontal quebrado.
   - Touch targets $\ge 44$px de altura e largura.
   - Navegação por teclado: tecla `Escape` fecha modal de solicitação e gaveta de auditoria.
   - Atributos semânticos ARIA (`role="status"`, `role="region"`, `role="dialog"`, `aria-modal="true"`, `aria-label`).
5. **Trilha de Auditoria Transparente do Tenant:**
   - Gaveta lateral (`ExportAuditLogsDrawer`) permitindo ao operador auditar solicitações, gerações, downloads e eventuais bloqueios de segurança do seu tenant.

---

## 2. Arquitetura da Solução Frontend

### 2.1 Modelos de Tipagem TypeScript (`frontend/src/types/reports.ts`)
- `ExportDataset`: `'FINANCIAL_FACTS' | 'ORDERS' | 'CATALOG' | 'METRICS'`
- `ExportFileFormat`: `'CSV' | 'XLSX'`
- `ExportJobStatus`: `'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'EXPIRED'`
- `ExportAuditAction`: `'EXPORT_REQUESTED' | 'EXPORT_COMPLETED' | 'EXPORT_FAILED' | 'EXPORT_DOWNLOADED' | 'EXPORT_EXPIRED_ACCESS_ATTEMPT' | 'EXPORT_CROSS_TENANT_ACCESS_BLOCKED'`
- `ExportJobResponseDto`: DTO completo retornado pelo backend.
- `ExportRequestDto`: Payload enviado para criação do job.
- `ExportAuditLogDto`: Entradas de auditoria de exportação.

### 2.2 Camada BFF (Next.js 15 App Router Routes)
- `GET /api/reports/exports`: Listagem paginada de jobs de exportação do tenant com filtros opcionais de status.
- `POST /api/reports/exports`: Criação de novo job de exportação com payload validado e chave de idempotência.
- `GET /api/reports/exports/[id]`: Consulta individual de status para polling de jobs em processamento.
- `GET /api/reports/exports/[id]/download?token=...`: Download autenticado. Valida sessão do operador, encaminha para o backend Spring com `Authorization: Bearer <token>`, repassa o binário com `Content-Type` e `Content-Disposition`.
- `GET /api/reports/exports/audit`: Listagem dos logs de auditoria de exportação do tenant.

### 2.3 Componentes React (`frontend/src/components/reporting/`)
- `ReportsExportView.tsx`: Módulo principal contendo cabeçalho com ações, métricas de exportação, filtros por status, lista/tabela de histórico, polling automático inteligente e trigger de download autenticado.
- `ExportRequestModal.tsx`: Modal acessível para configuração de nova exportação (dataset, formato, limite de linhas, filtros de data e canal, preview de colunas).
- `ExportJobStatusCard.tsx`: Card de item de exportação exibindo estado atual, progresso, contagem de registros, badge de truncamento, tamanho, checksum SHA-256 e ações.
- `ExportAuditLogsDrawer.tsx`: Gaveta lateral exibindo trilha de auditoria completa com carimbo ISO, ação, usuário e IP.

### 2.4 Integração com o AppShell
- O módulo é registrado no `navModules` com o ID `'REPORTS'` sob o pilar `'GOVERNANCE'`, garantindo acesso direto tanto no desktop quanto no mobile drawer.

---

## 3. Critérios de Aceite Específicos (Definition of Acceptance)

1. **Estados do Job:**
   - A interface exibe claramente os estados `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED` e `EXPIRED` com badges visuais distintos e informativos.
   - Jobs em processamento realizam polling automático com intervalo seguro e cessam o polling ao atingir estado final.
2. **Download Exige Autorização:**
   - Não existe nenhuma tag `<a href="public-s3-link">` no DOM.
   - O download ocorre exclusivamente por clique em botão de ação autorizada que chama o endpoint BFF com credenciais válidas.
   - Se o token expirar ou o backend retornar 410, a interface apresenta mensagem de erro clara orientando a gerar novo arquivo.
3. **Mobile Permite Consultar Status:**
   - Em telas estreitas ($\ge 360$px), a visualização adapta-se a cards empilhados com informações de status legíveis, badges bem espaçados e controles de toque $\ge 44$px.
