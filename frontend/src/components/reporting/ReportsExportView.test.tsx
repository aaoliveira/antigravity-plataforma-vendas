import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { ReportsExportView } from './ReportsExportView';
import { ExportJobResponseDto } from '@/types/reports';

const mockJobs: ExportJobResponseDto[] = [
  {
    id: '11111111-aaaa-1111-aaaa-111111111111',
    tenantId: 'tenant-test',
    requestedBy: 'admin@seller.com',
    idempotencyKey: 'key-1',
    dataset: 'FINANCIAL_FACTS',
    fileFormat: 'CSV',
    filterParamsJson: '{}',
    status: 'COMPLETED',
    maxRows: 10000,
    totalRows: 120,
    isTruncated: false,
    fileName: 'facts_11111111.csv',
    fileSizeBytes: 20480,
    checksumSha256: 'sha256-mock-1',
    downloadUrl: null,
    downloadToken: 'token-abc-123',
    expiresAt: new Date(Date.now() + 86400000).toISOString(),
    completedAt: new Date().toISOString(),
    errorMessage: null,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
  {
    id: '22222222-bbbb-2222-bbbb-222222222222',
    tenantId: 'tenant-test',
    requestedBy: 'admin@seller.com',
    idempotencyKey: 'key-2',
    dataset: 'ORDERS',
    fileFormat: 'XLSX',
    filterParamsJson: '{}',
    status: 'PROCESSING',
    maxRows: 5000,
    totalRows: 0,
    isTruncated: false,
    fileName: null,
    fileSizeBytes: 0,
    checksumSha256: null,
    downloadUrl: null,
    downloadToken: null,
    expiresAt: null,
    completedAt: null,
    errorMessage: null,
    createdAt: new Date().toISOString(),
    updatedAt: new Date().toISOString(),
  },
];

const newCreatedJob: ExportJobResponseDto = {
  id: '33333333-cccc-3333-cccc-333333333333',
  tenantId: 'tenant-test',
  requestedBy: 'operator@seller.com',
  idempotencyKey: 'key-3',
  dataset: 'CATALOG',
  fileFormat: 'CSV',
  filterParamsJson: '{}',
  status: 'PENDING',
  maxRows: 1000,
  totalRows: 0,
  isTruncated: false,
  fileName: null,
  fileSizeBytes: 0,
  checksumSha256: null,
  downloadUrl: null,
  downloadToken: null,
  expiresAt: null,
  completedAt: null,
  errorMessage: null,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
};

describe('ReportsExportView Component', () => {
  let createObjectURLSpy: ReturnType<typeof vi.fn>;
  let revokeObjectURLSpy: ReturnType<typeof vi.fn>;
  let downloadResponseStatus = 200;

  beforeEach(() => {
    vi.clearAllMocks();
    downloadResponseStatus = 200;

    HTMLAnchorElement.prototype.click = vi.fn();
    createObjectURLSpy = vi.fn().mockReturnValue('blob:http://localhost/temp-uuid');
    revokeObjectURLSpy = vi.fn();
    window.URL.createObjectURL = createObjectURLSpy;
    window.URL.revokeObjectURL = revokeObjectURLSpy;

    // Mock fetch central resiliente a todas as rotas
    vi.spyOn(global, 'fetch').mockImplementation((input: RequestInfo | URL, init?: RequestInit) => {
      const url = typeof input === 'string' ? input : input.toString();

      if (url.includes('/api/reports/exports/audit')) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () => ({
            content: [
              {
                id: 'audit-1',
                tenantId: 'tenant-test',
                exportJobId: '11111111-aaaa-1111-aaaa-111111111111',
                action: 'EXPORT_COMPLETED',
                userId: 'admin@seller.com',
                ipAddress: '192.168.1.100',
                userAgent: 'Mozilla/5.0',
                createdAt: new Date().toISOString(),
              },
            ],
          }),
        } as Response);
      }

      if (url.includes('/download')) {
        if (downloadResponseStatus === 410) {
          return Promise.resolve({
            ok: false,
            status: 410,
            json: async () => ({
              error: 'EXPORT_EXPIRED',
              message: 'Este arquivo expirou e não está mais disponível.',
            }),
          } as Response);
        }

        const mockBlob = new Blob(['col1,col2\nval1,val2'], { type: 'text/csv' });
        return Promise.resolve({
          ok: true,
          status: 200,
          blob: async () => mockBlob,
          headers: new Headers({
            'Content-Disposition': 'attachment; filename="facts_11111111.csv"',
          }),
        } as unknown as Response);
      }

      if (url.includes('/api/reports/exports') && init?.method === 'POST') {
        return Promise.resolve({
          ok: true,
          status: 202,
          json: async () => newCreatedJob,
        } as Response);
      }

      if (url.includes('/api/reports/exports')) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: async () => ({
            content: mockJobs,
            totalElements: 2,
            totalPages: 1,
          }),
        } as Response);
      }

      return Promise.resolve({
        ok: true,
        status: 200,
        json: async () => ({}),
      } as Response);
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('deve renderizar a visão inicial com cabeçalho, KPIs e histórico de jobs', async () => {
    render(<ReportsExportView />);

    expect(
      screen.getByRole('heading', { name: /Central de Relatórios & Exportações/i })
    ).toBeInTheDocument();

    // Aguarda carregar dados
    await waitFor(() => {
      expect(screen.getByText('facts_11111111.csv')).toBeInTheDocument();
      expect(screen.getByText('Processando...')).toBeInTheDocument();
    });

    // KPIs renderizados
    expect(screen.getByText('Total de Exportações')).toBeInTheDocument();
    expect(screen.getByText('Concluídos (Disponíveis)')).toBeInTheDocument();
    expect(screen.getByText('Em Processamento')).toBeInTheDocument();
  });

  it('deve permitir abrir o modal de nova exportação e submeter novo job', async () => {
    render(<ReportsExportView />);

    await waitFor(() => {
      expect(screen.getByText('facts_11111111.csv')).toBeInTheDocument();
    });

    // Clica em Nova Exportação
    const newBtn = screen.getByRole('button', { name: /Solicitar nova exportação/i });
    fireEvent.click(newBtn);

    // Modal aberta
    expect(screen.getByRole('dialog', { name: /Solicitar Nova Exportação/i })).toBeInTheDocument();

    // Submete formulário
    const submitBtn = screen.getByRole('button', { name: /Solicitar Exportação Assíncrona/i });
    fireEvent.click(submitBtn);

    // Deve atualizar a lista com o novo job e exibir notificação de sucesso
    await waitFor(() => {
      expect(screen.getByText(/iniciada com sucesso/i)).toBeInTheDocument();
      expect(screen.getByText('Na Fila')).toBeInTheDocument();
    });
  });

  it('deve realizar download autorizado via Blob sem links públicos permanentes no DOM', async () => {
    const { container } = render(<ReportsExportView />);

    await waitFor(() => {
      expect(screen.getByText('facts_11111111.csv')).toBeInTheDocument();
    });

    // Guardrail de Segurança: Zero links públicos antes do download
    expect(container.querySelectorAll('a')).toHaveLength(0);

    const downloadBtn = screen.getByRole('button', {
      name: /Baixar arquivo seguro facts_11111111\.csv/i,
    });
    fireEvent.click(downloadBtn);

    await waitFor(() => {
      expect(createObjectURLSpy).toHaveBeenCalled();
      expect(revokeObjectURLSpy).toHaveBeenCalled();
      expect(
        screen.getByText(/Download do arquivo "facts_11111111\.csv" efetuado com sucesso/i)
      ).toBeInTheDocument();
    });

    // Guardrail de Segurança: Zero links públicos persistem no DOM após o download
    expect(container.querySelectorAll('a')).toHaveLength(0);
  });

  it('deve tratar erro 410 (EXPORT_EXPIRED) no download e atualizar estado para EXPIRED', async () => {
    downloadResponseStatus = 410;

    render(<ReportsExportView />);

    await waitFor(() => {
      expect(screen.getByText('facts_11111111.csv')).toBeInTheDocument();
    });

    const downloadBtn = screen.getByRole('button', {
      name: /Baixar arquivo seguro facts_11111111\.csv/i,
    });
    fireEvent.click(downloadBtn);

    await waitFor(() => {
      expect(
        screen.getByText(/Este arquivo expirou e foi removido do storage seguro/i)
      ).toBeInTheDocument();
      expect(screen.getByText('Expirado')).toBeInTheDocument();
    });
  });

  it('deve abrir a gaveta de auditoria e exibir registros de log', async () => {
    render(<ReportsExportView />);

    const auditBtn = screen.getByRole('button', { name: /Abrir trilha de auditoria/i });
    fireEvent.click(auditBtn);

    await waitFor(() => {
      expect(
        screen.getByRole('dialog', { name: /Trilha de Auditoria de Exportações/i })
      ).toBeInTheDocument();
      expect(screen.getAllByText('admin@seller.com').length).toBeGreaterThan(0);
      expect(screen.getByText('192.168.1.100')).toBeInTheDocument();
    });

    // Fecha com tecla Escape
    fireEvent.keyDown(window, { key: 'Escape' });
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: /Trilha de Auditoria/i })).toBeNull();
    });
  });

  it('deve filtrar os jobs por dataset e por status', async () => {
    render(<ReportsExportView />);

    await waitFor(() => {
      expect(screen.getByText('facts_11111111.csv')).toBeInTheDocument();
      expect(screen.getByText('Pedidos de Venda')).toBeInTheDocument();
    });

    // Filtra por Dataset Pedidos
    const ordersFilterBtn = screen.getByRole('button', { name: /Vendas & Pedidos/i });
    fireEvent.click(ordersFilterBtn);

    // Deve exibir apenas Pedidos
    expect(screen.queryByText('facts_11111111.csv')).toBeNull();
    expect(screen.getByText('Pedidos de Venda')).toBeInTheDocument();

    // Restaura para Todos
    const allDatasetsBtn = screen.getByRole('button', { name: 'Todos' });
    fireEvent.click(allDatasetsBtn);

    expect(screen.getByText('facts_11111111.csv')).toBeInTheDocument();
    expect(screen.getByText('Pedidos de Venda')).toBeInTheDocument();

    // Filtra por Status: Apenas Concluídos
    const statusSelect = screen.getByLabelText(/Filtrar por status/i);
    fireEvent.change(statusSelect, { target: { value: 'COMPLETED' } });

    expect(screen.getByText('facts_11111111.csv')).toBeInTheDocument();
    expect(screen.queryByText('Processando...')).toBeNull();
  });

  it('deve permitir consultar status de jobs em viewport mobile (375px)', async () => {
    window.innerWidth = 375;
    window.innerHeight = 667;
    window.dispatchEvent(new Event('resize'));

    render(<ReportsExportView />);

    await waitFor(() => {
      expect(screen.getByText('facts_11111111.csv')).toBeInTheDocument();
      expect(screen.getByText('Processando...')).toBeInTheDocument();
    });

    const statusBadges = screen.getAllByRole('status');
    expect(statusBadges.length).toBeGreaterThan(0);
    expect(screen.getByText('Concluído')).toBeInTheDocument();
    expect(screen.getByText('Processando...')).toBeInTheDocument();
  });
});
