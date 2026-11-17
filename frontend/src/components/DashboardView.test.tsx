import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { DashboardView } from './DashboardView';

describe('DashboardView (Mobile-First & Hierarchical Navigation)', () => {
  it('renders initial view with Companies & CNPJs hero and default counters', () => {
    render(<DashboardView />);

    expect(screen.getByRole('heading', { name: 'Estrutura Organizacional e Gestão de Acessos' })).toBeDefined();
    expect(screen.getAllByText(/Empresas & CNPJs/i).length).toBeGreaterThan(0);
    expect(screen.getAllByText(/Lojas & Marketplaces/i).length).toBeGreaterThan(0);
  });

  it('switches between strategic pillars and modules', () => {
    render(<DashboardView />);

    // Switch to Market Intelligence
    const marketBtns = screen.getAllByRole('button', { name: /Pesquisa de Mercado/i });
    fireEvent.click(marketBtns[0]);
    expect(screen.getByRole('heading', { name: 'Market Intelligence: Pesquisa de Mercado' })).toBeDefined();

    // Switch to AI Listing Studio
    const aiBtns = screen.getAllByRole('button', { name: /AI Listing Studio/i });
    fireEvent.click(aiBtns[0]);
    expect(screen.getAllByRole('heading', { name: /AI Listing Studio/i }).length).toBeGreaterThan(0);

    // Switch to Compliance
    const complianceBtns = screen.getAllByRole('button', { name: /Compliance & Políticas/i });
    fireEvent.click(complianceBtns[0]);
    expect(screen.getByText(/Continuous Marketplace Compliance/i)).toBeDefined();
  });

  it('navigates within financial module sub-tabs (Dashboard vs Price Simulator)', () => {
    render(<DashboardView />);

    // Switch to Financial Tab
    const finBtns = screen.getAllByRole('button', { name: /Motor Financeiro/i });
    fireEvent.click(finBtns[0]);

    // By default, renders Financial Dashboard
    expect(screen.getByRole('heading', { name: 'Painel Financeiro & Rentabilidade' })).toBeDefined();

    // Switch to Price Simulator sub-tab
    const simBtn = screen.getByRole('button', { name: /Simulador & Regras de Preço/i });
    fireEvent.click(simBtn);

    expect(screen.getByRole('heading', { name: 'Motor Financeiro Determinístico' })).toBeDefined();
  });

  it('filters navigation items using the search input', () => {
    render(<DashboardView />);

    const searchInput = screen.getByPlaceholderText(/Buscar módulo/i);
    fireEvent.change(searchInput, { target: { value: 'DRE' } });

    // DRE should be visible in filtered results
    expect(screen.getAllByText(/DRE & Conciliação/i).length).toBeGreaterThan(0);

    // Clear search
    const clearBtn = screen.getByText('✕');
    fireEvent.click(clearBtn);
    expect(searchInput.getAttribute('value')).toBe('');
  });

  it('opens and closes mobile slide-over navigation drawer', () => {
    render(<DashboardView />);

    const hamburgerBtn = screen.getByLabelText('Abrir menu de navegação');
    fireEvent.click(hamburgerBtn);

    // Drawer should be open
    expect(screen.getByText('Menu Principal')).toBeDefined();

    // Close button
    const closeBtn = screen.getByLabelText('Fechar menu');
    fireEvent.click(closeBtn);

    // Drawer should be closed
    expect(screen.queryByText('Menu Principal')).toBeNull();
  });
});
