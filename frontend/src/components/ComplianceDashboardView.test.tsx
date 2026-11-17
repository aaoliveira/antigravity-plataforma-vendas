import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import React from 'react';
import { ComplianceDashboardView } from './ComplianceDashboardView';

describe('ComplianceDashboardView (Prompt 21 - Continuous Compliance & Policy Governance)', () => {
  it('renders overall compliance indicators and FAIL CLOSED badge', () => {
    render(<ComplianceDashboardView />);

    expect(screen.getByText(/Continuous Marketplace Compliance & Policy Governance/i)).toBeDefined();
    expect(screen.getByText(/FAIL CLOSED ACTIVE/i)).toBeDefined();
    expect(screen.getByText(/Marketplaces Monitorados/i)).toBeDefined();
  });

  it('switches to Policies tab and filters by marketplace', () => {
    render(<ComplianceDashboardView />);

    const policiesTab = screen.getByRole('button', { name: /Registro de Políticas/i });
    fireEvent.click(policiesTab);

    expect(screen.getByText(/Políticas e Termos Oficiais Registrados/i)).toBeDefined();
    expect(screen.getByText(/Mercado Livre Developer Terms & OAuth Guidelines/i)).toBeDefined();

    // Filter
    const filterSelect = screen.getByLabelText('Filtrar por canal');
    fireEvent.change(filterSelect, { target: { value: 'SHOPEE' } });

    expect(screen.getByText(/Shopee Open Platform Developer Service Agreement/i)).toBeDefined();
    expect(screen.queryByText(/Mercado Livre Developer Terms/i)).toBeNull();
  });

  it('switches to Incidents tab and allows resolving an incident', () => {
    render(<ComplianceDashboardView />);

    const incidentsTab = screen.getByRole('button', { name: /Incidentes/i });
    fireEvent.click(incidentsTab);

    expect(screen.getByText(/Central de Incidentes de Compliance/i)).toBeDefined();
    expect(screen.getByText(/INC-2026-101/i)).toBeDefined();

    const resolveBtn = screen.getByRole('button', { name: /Resolver/i });
    fireEvent.click(resolveBtn);

    expect(screen.getByText(/RESOLVED/i)).toBeDefined();
  });

  it('switches to Safe Reactivation tab and validates required fields', () => {
    render(<ComplianceDashboardView />);

    const reactivationTab = screen.getByRole('button', { name: /Reativação Segura/i });
    fireEvent.click(reactivationTab);

    expect(screen.getByText(/Protocolo Obrigatório de Reativação Segura/i)).toBeDefined();

    const submitBtn = screen.getByRole('button', { name: /Aprovar Reativação Segura/i });
    fireEvent.click(submitBtn);

    expect(screen.getByText(/Identificação do administrador aprovador é obrigatória/i)).toBeDefined();
  });

  it('switches to APIs tab and displays active and deprecated versions', () => {
    render(<ComplianceDashboardView />);

    const apisTab = screen.getByRole('button', { name: /Ciclo de Vida de APIs/i });
    fireEvent.click(apisTab);

    expect(screen.getByText(/Monitoramento de Ciclo de Vida e Versões de APIs/i)).toBeDefined();
    expect(screen.getByText(/Legacy Metrics API/i)).toBeDefined();
    expect(screen.getByText(/DEPRECATED/i)).toBeDefined();
  });
});
