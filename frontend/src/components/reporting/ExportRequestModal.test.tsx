import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { ExportRequestModal } from './ExportRequestModal';
import { ExportRequestDto } from '@/types/reports';

describe('ExportRequestModal Component', () => {
  const mockOnClose = vi.fn();
  const mockOnSubmit = vi.fn<(request: ExportRequestDto) => Promise<boolean>>();

  beforeEach(() => {
    vi.clearAllMocks();
    mockOnSubmit.mockResolvedValue(true);
  });

  it('não deve renderizar quando isOpen for false', () => {
    render(
      <ExportRequestModal
        isOpen={false}
        onClose={mockOnClose}
        onSubmit={mockOnSubmit}
        isSubmitting={false}
      />
    );

    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('deve renderizar modal acessível quando isOpen for true', () => {
    render(
      <ExportRequestModal
        isOpen={true}
        onClose={mockOnClose}
        onSubmit={mockOnSubmit}
        isSubmitting={false}
      />
    );

    const dialog = screen.getByRole('dialog', { name: /Solicitar Nova Exportação/i });
    expect(dialog).toBeInTheDocument();
    expect(dialog).toHaveAttribute('aria-modal', 'true');
  });

  it('deve fechar ao pressionar a tecla Escape ou clicar no botão fechar', () => {
    render(
      <ExportRequestModal
        isOpen={true}
        onClose={mockOnClose}
        onSubmit={mockOnSubmit}
        isSubmitting={false}
      />
    );

    // Fecha com tecla Escape
    fireEvent.keyDown(window, { key: 'Escape' });
    expect(mockOnClose).toHaveBeenCalledTimes(1);

    // Fecha com botão Fechar
    const closeBtn = screen.getByRole('button', { name: /Fechar janela de exportação/i });
    fireEvent.click(closeBtn);
    expect(mockOnClose).toHaveBeenCalledTimes(2);
  });

  it('deve permitir alternar o dataset e exibir preview de colunas correspondentes', () => {
    render(
      <ExportRequestModal
        isOpen={true}
        onClose={mockOnClose}
        onSubmit={mockOnSubmit}
        isSubmitting={false}
      />
    );

    // Clica no dataset de Pedidos de Venda
    const ordersBtn = screen.getByText('Pedidos de Venda');
    fireEvent.click(ordersBtn);

    // Abre o preview de colunas
    const previewBtn = screen.getByRole('button', { name: /Colunas incluídas no relatório/i });
    fireEvent.click(previewBtn);

    // Deve exibir colunas de pedidos
    expect(screen.getByText('Comprador')).toBeInTheDocument();
    expect(screen.getByText('Rastreio')).toBeInTheDocument();
  });

  it('deve permitir alternar o formato entre CSV e XLSX', () => {
    render(
      <ExportRequestModal
        isOpen={true}
        onClose={mockOnClose}
        onSubmit={mockOnSubmit}
        isSubmitting={false}
      />
    );

    const xlsxBtn = screen.getByRole('button', { name: /XLSX/i });
    fireEvent.click(xlsxBtn);

    expect(xlsxBtn).toHaveClass('ring-1');
  });

  it('deve validar intervalo de datas inválido (startDate posterior a endDate)', async () => {
    render(
      <ExportRequestModal
        isOpen={true}
        onClose={mockOnClose}
        onSubmit={mockOnSubmit}
        isSubmitting={false}
      />
    );

    const startDateInput = screen.getByLabelText(/Data Inicial/i);
    const endDateInput = screen.getByLabelText(/Data Final/i);

    fireEvent.change(startDateInput, { target: { value: '2026-12-01' } });
    fireEvent.change(endDateInput, { target: { value: '2026-11-01' } });

    const submitBtn = screen.getByRole('button', { name: /Solicitar Exportação Assíncrona/i });
    fireEvent.click(submitBtn);

    expect(
      screen.getByText('A data inicial não pode ser superior à data final.')
    ).toBeInTheDocument();
    expect(mockOnSubmit).not.toHaveBeenCalled();
  });

  it('deve submeter o formulário com o payload correto e fechar a modal ao obter sucesso', async () => {
    render(
      <ExportRequestModal
        isOpen={true}
        onClose={mockOnClose}
        onSubmit={mockOnSubmit}
        isSubmitting={false}
      />
    );

    // Seleciona Catálogo
    const catalogBtn = screen.getByText('Catálogo & Anúncios Próprios');
    fireEvent.click(catalogBtn);

    // Seleciona formato XLSX
    const xlsxBtn = screen.getByRole('button', { name: /XLSX/i });
    fireEvent.click(xlsxBtn);

    // Seleciona limite de linhas
    const maxRowsSelect = screen.getByLabelText(/Limite de Linhas/i);
    fireEvent.change(maxRowsSelect, { target: { value: '5000' } });

    // Seleciona marketplace Mercado Livre
    const mpSelect = screen.getByLabelText(/Marketplace \/ Canal/i);
    fireEvent.change(mpSelect, { target: { value: 'MERCADO_LIVRE' } });

    // Submete
    const submitBtn = screen.getByRole('button', { name: /Solicitar Exportação Assíncrona/i });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockOnSubmit).toHaveBeenCalledTimes(1);
      const callArg = mockOnSubmit.mock.calls[0][0];
      expect(callArg.dataset).toBe('CATALOG');
      expect(callArg.fileFormat).toBe('XLSX');
      expect(callArg.maxRows).toBe(5000);
      expect(callArg.filterParams?.marketplace).toBe('MERCADO_LIVRE');
      expect(callArg.idempotencyKey).toBeDefined();
      expect(mockOnClose).toHaveBeenCalledTimes(1);
    });
  });
});
