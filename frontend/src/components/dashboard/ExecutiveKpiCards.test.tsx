import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { ExecutiveKpiCards } from './ExecutiveKpiCards';
import { AggregatedKpiSummary } from '@/types/kpis';

const mockSummary: AggregatedKpiSummary = {
  tenantId: '11111111-1111-1111-1111-111111111111',
  totalOrders: 150,
  totalGrossRevenue: 15000.0,
  totalDiscountAmount: 500.0,
  totalNetRevenue: 14500.0,
  totalCancellationAmount: 200.0,
  totalReturnAmount: 100.0,
  totalMarketplaceFees: 2175.0,
  totalFixedFees: 450.0,
  totalShippingCost: 1200.0,
  totalProductCost: 5000.0,
  totalPackagingCost: 300.0,
  totalAdditionalCost: 150.0,
  totalTaxAmount: 900.0,
  totalOtherExpensesAmount: 50.0,
  totalDeductions: 10225.0,
  totalContributionMarginAmount: 4275.0,
  averageContributionMarginRatio: 29.48,
  averageOrderValue: 100.0,
  reconciledCount: 135,
  estimatedCount: 15,
  divergentCount: 0,
  ordersWithMissingDataCount: 0,
  reconciliationRatePercent: 90.0,
  cached: false,
  calculatedAt: '2026-11-23T12:00:00Z',
  antiHallucinationDisclaimer:
    'Este demonstrativo apura estritamente a Margem de Contribuição. Proibido rotular este valor como Lucro Líquido.',
};

describe('ExecutiveKpiCards Component', () => {
  it('deve renderizar os esqueletos de carregamento (skeleton) quando isLoading for true', () => {
    render(<ExecutiveKpiCards summary={null} isLoading={true} />);

    expect(screen.getByRole('status', { name: /Carregando indicadores principais/i })).toBeInTheDocument();
    expect(screen.getByText(/Carregando indicadores executivos/i)).toBeInTheDocument();
  });

  it('deve formatar e renderizar corretamente os 6 cartões de KPIs com valores batendo com o summary', () => {
    render(<ExecutiveKpiCards summary={mockSummary} isLoading={false} />);

    // Pedidos
    expect(screen.getByText('Pedidos Faturados')).toBeInTheDocument();
    expect(screen.getByTestId('kpi-value-total_orders')).toHaveTextContent('150');

    // Receita Bruta
    expect(screen.getByText('Receita Bruta Total')).toBeInTheDocument();
    expect(screen.getByTestId('kpi-value-gross_revenue')).toHaveTextContent('R$ 15.000,00');

    // Receita Líquida
    expect(screen.getByText('Receita Líquida')).toBeInTheDocument();
    expect(screen.getByTestId('kpi-value-net_revenue')).toHaveTextContent('R$ 14.500,00');

    // Margem de Contribuição
    expect(screen.getByText('Margem Contribuição')).toBeInTheDocument();
    expect(screen.getByTestId('kpi-value-contribution_margin')).toHaveTextContent('R$ 4.275,00');
    expect(screen.getByText(/Margem média: 29,48%/i)).toBeInTheDocument();

    // Ticket Médio
    expect(screen.getByText('Ticket Médio (AOV)')).toBeInTheDocument();
    expect(screen.getByTestId('kpi-value-average_order_value')).toHaveTextContent('R$ 100,00');

    // Taxa de Conciliação
    expect(screen.getByText('Taxa de Reconciliação')).toBeInTheDocument();
    expect(screen.getByTestId('kpi-value-reconciliation_rate')).toHaveTextContent('90,00%');
  });

  it('deve disparar onOpenDefinitions com o código correspondente ao clicar no botão de informação', () => {
    const onOpenMock = vi.fn();
    render(<ExecutiveKpiCards summary={mockSummary} isLoading={false} onOpenDefinitions={onOpenMock} />);

    const marginInfoButton = screen.getByRole('button', {
      name: /Ver fórmula e definição de Margem Contribuição/i,
    });
    fireEvent.click(marginInfoButton);

    expect(onOpenMock).toHaveBeenCalledWith('CONTRIBUTION_MARGIN');
  });

  it('deve expandir e renderizar o detalhamento de deduções e custos', () => {
    render(<ExecutiveKpiCards summary={mockSummary} isLoading={false} />);

    expect(screen.getByText(/Detalhamento de Deduções, CMV e Despesas Variáveis/i)).toBeInTheDocument();
    expect(screen.getByText('R$ 2.175,00')).toBeInTheDocument(); // Comissões
    expect(screen.getByText('R$ 5.000,00')).toBeInTheDocument(); // CMV
    expect(screen.getByText('R$ 1.200,00')).toBeInTheDocument(); // Frete
  });
});
