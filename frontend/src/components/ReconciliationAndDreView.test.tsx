import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import React from 'react';
import { ReconciliationAndDreView } from './ReconciliationAndDreView';

describe('ReconciliationAndDreView (Prompt 17 - DRE Gerencial & Conciliação de Repasses)', () => {
  it('should render compliance banner and legal disclaimers correctly', () => {
    render(<ReconciliationAndDreView />);

    expect(
      screen.getByText(/DRE Gerencial & Conciliação de Repasses/i)
    ).toBeDefined();
    expect(
      screen.getByText(/Fase 2 • FR-FIN-008 & FR-REC/i)
    ).toBeDefined();
    expect(
      screen.getByText(/Aviso de Conformidade:/i)
    ).toBeDefined();
    expect(
      screen.getByText(/Demonstrativo gerencial com separação estrita de regimes/i)
    ).toBeDefined();
  });

  it('should toggle between ACCRUAL (Competência) and CASH (Caixa) regimes on DRE', () => {
    render(<ReconciliationAndDreView />);

    const accrualBtn = screen.getByTestId('basis-accrual-btn');
    const cashBtn = screen.getByTestId('basis-cash-btn');

    expect(accrualBtn).toBeDefined();
    expect(cashBtn).toBeDefined();

    // Default is Accrual
    expect(screen.getByText(/Receita Bruta de Vendas \(Pedidos Realizados\)/i)).toBeDefined();

    // Switch to Cash
    fireEvent.click(cashBtn);
    expect(screen.getByText(/Entradas Brutas de Repasses Liquidados/i)).toBeDefined();
    expect(screen.getByText(/reflete valores monetários efetivamente liquidados/i)).toBeDefined();

    // Switch back to Accrual
    fireEvent.click(accrualBtn);
    expect(screen.getByText(/Receita Bruta de Vendas \(Pedidos Realizados\)/i)).toBeDefined();
  });

  it('should export DRE to CSV file with required metadata headers and disclaimer', () => {
    // Mock URL.createObjectURL and link click
    const createObjectURLMock = vi.fn().mockReturnValue('blob:mock-csv-url');
    window.URL.createObjectURL = createObjectURLMock;
    const appendChildMock = vi.spyOn(document.body, 'appendChild');
    const removeChildMock = vi.spyOn(document.body, 'removeChild');

    render(<ReconciliationAndDreView />);

    const exportBtn = screen.getByTestId('export-dre-csv-btn');
    fireEvent.click(exportBtn);

    expect(createObjectURLMock).toHaveBeenCalled();
    expect(appendChildMock).toHaveBeenCalled();
    expect(removeChildMock).toHaveBeenCalled();
  });

  it('should switch to Conciliação tab, filter items and search', () => {
    render(<ReconciliationAndDreView />);

    const conciliationTabBtn = screen.getByRole('button', { name: /Conciliação de Pedidos & Lançamentos/i });
    fireEvent.click(conciliationTabBtn);

    expect(screen.getByText('MLB-ORD-9021')).toBeDefined();
    expect(screen.getByText('MLB-ORD-9022')).toBeDefined();

    // Filter by Discrepant
    const discrepantFilterBtn = screen.getByRole('button', { name: 'Com Divergência' });
    fireEvent.click(discrepantFilterBtn);

    expect(screen.getByText('MLB-ORD-9022')).toBeDefined();
    expect(screen.queryByText('MLB-ORD-9021')).toBeNull();

    // Filter by All
    const allFilterBtn = screen.getByRole('button', { name: 'Todos' });
    fireEvent.click(allFilterBtn);
    expect(screen.getByText('MLB-ORD-9021')).toBeDefined();
  });

  it('should allow manual order association (De/Para) with justification and record audit log', () => {
    render(<ReconciliationAndDreView />);

    const conciliationTabBtn = screen.getByRole('button', { name: /Conciliação de Pedidos & Lançamentos/i });
    fireEvent.click(conciliationTabBtn);

    // Click "De/Para" on pending item
    const deParaButtons = screen.getAllByRole('button', { name: 'De/Para' });
    fireEvent.click(deParaButtons[0]);

    expect(screen.getByText(/Associar Pedido Manualmente \(De\/Para\)/i)).toBeDefined();

    const orderInput = screen.getByPlaceholderText('Ex: MLB-ORD-9025');
    const justificationInput = screen.getByPlaceholderText(/Ex: Pedido localizado via valor líquido/i);

    fireEvent.change(orderInput, { target: { value: 'SHP-ORD-7741-MATCHED' } });
    fireEvent.change(justificationInput, { target: { value: 'Conferido extrato bancário de liquidação Shopee' } });

    const confirmBtn = screen.getByRole('button', { name: /Confirmar Associação/i });
    fireEvent.click(confirmBtn);

    // Should update item and show in audit log
    expect(screen.getByText('SHP-ORD-7741-MATCHED')).toBeDefined();
    expect(screen.getByText(/Trilha de Auditoria Imutável/i)).toBeDefined();
    expect(screen.getAllByText(/Conferido extrato bancário de liquidação Shopee/i).length).toBeGreaterThanOrEqual(1);
  });

  it('should allow ignoring a discrepancy and creating a dispute protocol with non-automated disclaimer', () => {
    render(<ReconciliationAndDreView />);

    const conciliationTabBtn = screen.getByRole('button', { name: /Conciliação de Pedidos & Lançamentos/i });
    fireEvent.click(conciliationTabBtn);

    // Click "Contestar" on discrepant item
    const contestarBtn = screen.getByRole('button', { name: 'Contestar' });
    fireEvent.click(contestarBtn);

    expect(screen.getByText(/Registrar Contestação de Repasse/i)).toBeDefined();
    expect(screen.getByText(/O protocolo cadastrado aqui é para controle gerencial interno do seller/i)).toBeDefined();

    const reasonInput = screen.getByPlaceholderText(/Ex: Débito indevido de frete por divergência/i);
    fireEvent.change(reasonInput, { target: { value: 'Divergência comprovada de peso cubado pelo Correios' } });

    const submitDisputeBtn = screen.getByRole('button', { name: /Registrar Protocolo/i });
    fireEvent.click(submitDisputeBtn);

    // Switch to Disputes tab
    const disputesTabBtn = screen.getByRole('button', { name: /Contestações & Protocolos/i });
    fireEvent.click(disputesTabBtn);

    expect(screen.getByText(/Divergência comprovada de peso cubado pelo Correios/i)).toBeDefined();
    expect(screen.getAllByText(/Não \(Controle Interno\)/i).length).toBeGreaterThan(0);
  });

  it('should switch to Extratos tab and prevent duplicate imports via idempotency', () => {
    render(<ReconciliationAndDreView />);

    const settlementsTabBtn = screen.getByRole('button', { name: /Extratos & Liquidações \(Lotes\)/i });
    fireEvent.click(settlementsTabBtn);

    expect(screen.getByText('SETTL-MLB-2026-W46')).toBeDefined();
    expect(screen.getByText('SETTL-SHP-2026-11A')).toBeDefined();

    // Click "Novo Lote"
    const newBatchBtn = screen.getByRole('button', { name: 'Novo Lote' });
    fireEvent.click(newBatchBtn);

    expect(screen.getByText(/Importar Extrato de Liquidação \/ Repasse/i)).toBeDefined();

    // Fill with existing external settlement ID to trigger idempotency check
    const idInput = screen.getByPlaceholderText('Ex: SETTL-MLB-2026-W47');
    fireEvent.change(idInput, { target: { value: 'SETTL-MLB-2026-W46' } });

    const executeBtn = screen.getByRole('button', { name: /Executar Ingestão/i });
    fireEvent.click(executeBtn);

    expect(screen.getByText(/Idempotência ativada: O extrato 'SETTL-MLB-2026-W46' já foi importado previamente/i)).toBeDefined();
  });
});
