/**
 * Tipos e contratos TypeScript para o módulo de Relatórios e Exportações Assíncronas (P021-FE).
 */

export type ExportDataset = 'FINANCIAL_FACTS' | 'ORDERS' | 'CATALOG' | 'METRICS';

export type ExportFileFormat = 'CSV' | 'XLSX';

export type ExportJobStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'EXPIRED';

export type ExportAuditAction =
  | 'EXPORT_REQUESTED'
  | 'EXPORT_COMPLETED'
  | 'EXPORT_FAILED'
  | 'EXPORT_DOWNLOADED'
  | 'EXPORT_EXPIRED_ACCESS_ATTEMPT'
  | 'EXPORT_CROSS_TENANT_ACCESS_BLOCKED';

export interface ExportJobResponseDto {
  id: string;
  tenantId: string;
  requestedBy: string;
  idempotencyKey?: string | null;
  dataset: ExportDataset;
  fileFormat: ExportFileFormat;
  filterParamsJson: string;
  status: ExportJobStatus;
  maxRows: number;
  totalRows: number;
  isTruncated: boolean;
  fileName?: string | null;
  fileSizeBytes?: number | null;
  checksumSha256?: string | null;
  downloadUrl?: string | null;
  downloadToken?: string | null;
  expiresAt?: string | null;
  completedAt?: string | null;
  errorMessage?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ExportRequestDto {
  dataset: ExportDataset;
  fileFormat?: ExportFileFormat;
  idempotencyKey?: string;
  maxRows?: number;
  filterParams?: Record<string, unknown>;
}

export interface ExportAuditLogDto {
  id: string;
  tenantId: string;
  exportJobId?: string | null;
  action: ExportAuditAction;
  userId: string;
  ipAddress?: string | null;
  userAgent?: string | null;
  detailsJson?: string | null;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface DatasetColumnDefinition {
  dataset: ExportDataset;
  name: string;
  label: string;
  description: string;
  columns: string[];
}

export const DATASET_CATALOG: Record<ExportDataset, DatasetColumnDefinition> = {
  FINANCIAL_FACTS: {
    dataset: 'FINANCIAL_FACTS',
    name: 'Fatos Financeiros por Pedido',
    label: 'Financeiro (DRE/Margens)',
    description:
      'Receita bruta, descontos, comissões de marketplace, frete, custos cadastrados e margem de contribuição determinística.',
    columns: [
      'ID Fato',
      'ID Pedido',
      'Pedido Externo',
      'Marketplace',
      'Status Pedido',
      'Data Pedido',
      'Receita Bruta (BRL)',
      'Descontos (BRL)',
      'Cancelamentos (BRL)',
      'Devoluções (BRL)',
      'Comissão (BRL)',
      'Tarifa Fixa (BRL)',
      'Frete (BRL)',
      'Custo Produto (BRL)',
      'Imposto Informado (BRL)',
      'Margem Contribuição (BRL)',
      'Status Reconciliação',
      'Liquidação Externa',
      'Versão Cálculo',
    ],
  },
  ORDERS: {
    dataset: 'ORDERS',
    name: 'Pedidos de Venda',
    label: 'Vendas & Pedidos',
    description:
      'Pedidos recebidos dos canais conectados, status operacional, comprador, rastreio e valores totais.',
    columns: [
      'ID Pedido',
      'Pedido Externo',
      'Marketplace',
      'ID Conta',
      'Status',
      'Data Pedido',
      'Valor Total (BRL)',
      'Moeda',
      'Comprador',
      'Rastreio',
    ],
  },
  CATALOG: {
    dataset: 'CATALOG',
    name: 'Catálogo & Anúncios Próprios',
    label: 'Anúncios & Catálogo',
    description:
      'Listings sincronizados, SKUs, títulos, preços correntes, estoque e links de canal.',
    columns: [
      'ID Anúncio',
      'SKU / Código',
      'ID Externo',
      'Título',
      'Marketplace',
      'ID Conta',
      'Preço (BRL)',
      'Estoque',
      'Status',
      'Link Externo',
    ],
  },
  METRICS: {
    dataset: 'METRICS',
    name: 'Métricas Oficiais de Anúncios',
    label: 'Analytics & Métricas Oficiais',
    description:
      'Métricas comprovadas extraídas das APIs oficiais (visitas, pedidos, receita de anúncios, conversão).',
    columns: [
      'ID Métrica',
      'Marketplace',
      'ID Conta',
      'Anúncio Externo',
      'Escopo',
      'Chave Métrica',
      'Granularidade',
      'Início Janela',
      'Fim Janela',
      'Valor Numérico',
      'Status Disponibilidade',
      'Origem Oficial',
    ],
  },
};
