import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { DataQualityAlerts } from './DataQualityAlerts';
import { AggregatedKpiSummary } from '@/types/kpis';

const baseSummary: AggregatedKpiSummary = {
  tenantId: '11111111-1111-1111-1111-111111111111',
  totalOrders: 100,
  totalGrossRevenue: 10000.0,
  totalDiscountAmount: 0.0,
  totalNetRevenue: 10000.0,
  totalCancellationAmount: 0.0,
  totalReturnAmount: 0.0,
  totalMarketplaceFees: 1500.0,
  totalFixedFees: 300.0,
  totalShippingCost: 1000.0,
  totalProductCost: 4000.0,
  totalPackagingCost: 200.0,
  totalAdditionalCost: 100.0,
  totalTaxAmount: 600.0,
  totalOtherExpensesAmount: 0.0,
  totalDeductions: 7700.0,
  totalContributionMarginAmount: 2300.0,
  averageContributionMarginRatio: 23.0,
  averageOrderValue: 100.0,
  reconciledCount: 80,
  estimatedCount: 20,
  divergentCount: 0,
  ordersWithMissingDataCount: 0,
  reconciliationRatePercent: 80.0,
  cached: false,
  calculatedAt: '2026-11-23T12:00:00Z',
  antiHallucinationDisclaimer: 'Disclaimer',
};

describe('DataQualityAlerts Component', () => {
  it('deve alertar sobre dados faltantes quando ordersWithMissingDataCount > 0', () => {
    const summaryWithMissing = {
      ...baseSummary,
      ordersWithMissingDataCount: 5,
    };

    render(<DataQualityAlerts summary={summaryWithMissing} isLoading={false} />);

    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.getByText(/5 pedidos contêm dados ausentes ou incompletos/i)).toBeInTheDocument();
    expect(screen.getByText(/Custos unitários de mercadoria \(CMV\)/i)).toBeInTheDocument();
  });

  it('deve alertar sobre divergências contábeis quando divergentCount > 0', () => {
    const summaryWithDivergent = {
      ...baseSummary,
      divergentCount: 3,
    };

    render(<DataQualityAlerts summary={summaryWithDivergent} isLoading={false} />);

    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.getByText(/Divergência detectada em 3 pedidos/i)).toBeInTheDocument();
  });

  it('deve informar sobre liquidações pendentes quando houver pedidos estimados sem divergência', () => {
    render(<DataQualityAlerts summary={baseSummary} isLoading={false} />);

    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(screen.getByText(/Conciliação em andamento \(20 pedidos com liquidação pendente\)/i)).toBeInTheDocument();
  });

  it('deve exibir indicador de 100% reconciliado quando taxa for 100%', () => {
    const fullyReconciled = {
      ...baseSummary,
      reconciledCount: 100,
      estimatedCount: 0,
      divergentCount: 0,
      ordersWithMissingDataCount: 0,
      reconciliationRatePercent: 100.0,
    };

    render(<DataQualityAlerts summary={fullyReconciled} isLoading={false} />);

    expect(screen.getByRole('status')).toBeInTheDocument();
    expect(screen.getByText(/Dados 100% Reconciliados e Auditados/i)).toBeInTheDocument();
  });
});
