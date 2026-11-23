import React, { act } from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { ExportJobStatusCard } from './ExportJobStatusCard';
import { ExportJobResponseDto } from '@/types/reports';

const baseMockJob: ExportJobResponseDto = {
  id: '77777777-7777-7777-7777-777777777777',
  tenantId: '11111111-1111-1111-1111-111111111111',
  requestedBy: 'operator@seller.com',
  idempotencyKey: 'exp-test-1',
  dataset: 'FINANCIAL_FACTS',
  fileFormat: 'CSV',
  filterParamsJson: '{}',
  status: 'COMPLETED',
  maxRows: 10000,
  totalRows: 1540,
  isTruncated: false,
  fileName: 'financial_facts_export_77777777.csv',
  fileSizeBytes: 1048576, // 1.00 MB
  checksumSha256: 'a1b2c3d4e5f67890123456789abcdef0123456789abcdef0123456789abcdef0',
  downloadUrl: null,
  downloadToken: 'secure-token-1234567890abcdef',
  expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
  completedAt: new Date().toISOString(),
  errorMessage: null,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
};

describe('ExportJobStatusCard Component', () => {
  it('deve renderizar job no estado COMPLETED com detalhes e botão de download autorizado', () => {
    const onDownload = vi.fn();
    const { container } = render(
      <ExportJobStatusCard job={baseMockJob} onDownload={onDownload} />
    );

    // Estado Concluído
    expect(screen.getByText('Concluído')).toBeInTheDocument();
    expect(screen.getByText('Fatos Financeiros por Pedido')).toBeInTheDocument();
    expect(screen.getByText('1.540')).toBeInTheDocument();
    expect(screen.getByText('1.00 MB')).toBeInTheDocument();
    expect(screen.getByText('financial_facts_export_77777777.csv')).toBeInTheDocument();

    // Botão de Download Seguro presente
    const downloadBtn = screen.getByRole('button', {
      name: /Baixar arquivo seguro financial_facts_export_77777777\.csv/i,
    });
    expect(downloadBtn).toBeInTheDocument();

    // Guardrail Anti-Alucinação / Segurança: NENHUM link público <a> deve estar no DOM!
    const links = container.querySelectorAll('a');
    expect(links.length).toBe(0);

    // Clicar dispara ação autorizada
    fireEvent.click(downloadBtn);
    expect(onDownload).toHaveBeenCalledWith(baseMockJob);
  });

  it('deve exibir indicador de truncamento quando isTruncated for true', () => {
    const truncatedJob: ExportJobResponseDto = {
      ...baseMockJob,
      totalRows: 10000,
      isTruncated: true,
    };

    render(<ExportJobStatusCard job={truncatedJob} onDownload={vi.fn()} />);

    expect(screen.getByText('Truncado')).toBeInTheDocument();
  });

  it('deve renderizar estado PENDING (Na Fila)', () => {
    const pendingJob: ExportJobResponseDto = {
      ...baseMockJob,
      status: 'PENDING',
      totalRows: 0,
      fileSizeBytes: 0,
      downloadToken: null,
      fileName: null,
    };

    render(<ExportJobStatusCard job={pendingJob} onDownload={vi.fn()} />);

    expect(screen.getByText('Na Fila')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Baixar arquivo seguro/i })).toBeNull();
  });

  it('deve renderizar estado PROCESSING (Processando...) com spinner e botão de atualizar', async () => {
    const processingJob: ExportJobResponseDto = {
      ...baseMockJob,
      status: 'PROCESSING',
      totalRows: 0,
      fileSizeBytes: 0,
      downloadToken: null,
    };
    const onRefreshJob = vi.fn().mockResolvedValue(undefined);

    render(
      <ExportJobStatusCard
        job={processingJob}
        onDownload={vi.fn()}
        onRefreshJob={onRefreshJob}
      />
    );

    expect(screen.getByText('Processando...')).toBeInTheDocument();
    const refreshBtn = screen.getByRole('button', { name: /Atualizar/i });
    expect(refreshBtn).toBeInTheDocument();

    await act(async () => {
      fireEvent.click(refreshBtn);
    });

    expect(onRefreshJob).toHaveBeenCalledWith(processingJob.id);
  });

  it('deve renderizar estado FAILED com mensagem amigável de erro', () => {
    const failedJob: ExportJobResponseDto = {
      ...baseMockJob,
      status: 'FAILED',
      errorMessage: 'Falha ao acessar serviço de armazenamento privado.',
      downloadToken: null,
    };

    render(<ExportJobStatusCard job={failedJob} onDownload={vi.fn()} />);

    expect(screen.getByText('Falhou')).toBeInTheDocument();
    expect(
      screen.getByText(/Falha ao acessar serviço de armazenamento privado/i)
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Baixar arquivo seguro/i })).toBeNull();
  });

  it('deve renderizar estado EXPIRED quando job expirar e impedir download', () => {
    const expiredJob: ExportJobResponseDto = {
      ...baseMockJob,
      status: 'EXPIRED',
      expiresAt: new Date(Date.now() - 3600000).toISOString(), // 1 hora atrás
    };

    render(<ExportJobStatusCard job={expiredJob} onDownload={vi.fn()} />);

    expect(screen.getByText('Expirado')).toBeInTheDocument();
    expect(
      screen.getByText('Arquivo removido por expiração de segurança.')
    ).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Baixar arquivo seguro/i })).toBeNull();
  });

  it('deve copiar o hash SHA-256 para a área de transferência', async () => {
    const writeTextMock = vi.fn().mockResolvedValue(undefined);
    Object.assign(navigator, {
      clipboard: {
        writeText: writeTextMock,
      },
    });

    render(<ExportJobStatusCard job={baseMockJob} onDownload={vi.fn()} />);

    const copyBtn = screen.getByRole('button', { name: /Copiar/i });
    fireEvent.click(copyBtn);

    expect(writeTextMock).toHaveBeenCalledWith(baseMockJob.checksumSha256);
  });
});
