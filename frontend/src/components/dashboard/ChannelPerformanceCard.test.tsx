import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { ChannelPerformanceCard } from './ChannelPerformanceCard';
import { ChannelKpiAggregate } from '@/types/kpis';

const mockChannels: ChannelKpiAggregate[] = [
  {
    marketplace: 'MERCADO_LIVRE',
    orderCount: 100,
    grossRevenue: 10000.0,
    netRevenue: 9500.0,
    totalDeductions: 6500.0,
    contributionMarginAmount: 3000.0,
    averageContributionMarginRatio: 31.58,
    revenueSharePercent: 66.67,
    marginSharePercent: 70.0,
  },
  {
    marketplace: 'SHOPEE',
    orderCount: 50,
    grossRevenue: 5000.0,
    netRevenue: 4800.0,
    totalDeductions: 3500.0,
    contributionMarginAmount: 1300.0,
    averageContributionMarginRatio: 27.08,
    revenueSharePercent: 33.33,
    marginSharePercent: 30.0,
  },
];

describe('ChannelPerformanceCard Component', () => {
  it('deve renderizar o estado de carregamento skeleton quando isLoading for true', () => {
    render(<ChannelPerformanceCard channels={[]} isLoading={true} />);

    expect(screen.getByRole('status', { name: /Carregando desempenho por canal/i })).toBeInTheDocument();
  });

  it('deve renderizar a quebra de desempenho por canal com dados reais de Mercado Livre e Shopee', () => {
    render(<ChannelPerformanceCard channels={mockChannels} isLoading={false} />);

    expect(screen.getByText('Mercado Livre')).toBeInTheDocument();
    expect(screen.getByText('(100 pedidos)')).toBeInTheDocument();
    expect(screen.getByText('R$ 10.000,00')).toBeInTheDocument();
    expect(screen.getByText('66,67%')).toBeInTheDocument();

    expect(screen.getByText('Shopee')).toBeInTheDocument();
    expect(screen.getByText('(50 pedidos)')).toBeInTheDocument();
    expect(screen.getByText('R$ 5.000,00')).toBeInTheDocument();
    expect(screen.getByText('33,33%')).toBeInTheDocument();
  });

  it('deve exibir mensagem apropriada quando a lista de canais estiver vazia', () => {
    render(<ChannelPerformanceCard channels={[]} isLoading={false} />);

    expect(screen.getByText(/Nenhum dado por canal encontrado/i)).toBeInTheDocument();
  });
});
