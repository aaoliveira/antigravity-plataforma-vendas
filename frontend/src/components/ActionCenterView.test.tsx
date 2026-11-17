import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { ActionCenterView } from './ActionCenterView';

describe('ActionCenterView Component', () => {
  it('renders header, governance banner and anti-automation guardrail notice', () => {
    render(<ActionCenterView />);

    expect(screen.getByText(/Smart Alerts & Action Center \(Fase 2\)/i)).toBeDefined();
    expect(screen.getByText(/Invariante de Segurança PRD:/i)).toBeDefined();
    expect(screen.getByText(/Isolamento Anti-IDOR Ativo/i)).toBeDefined();
  });

  it('renders alerts inbox with KPI counts and alert cards', () => {
    render(<ActionCenterView />);

    expect(screen.getByText(/Total de Alertas/i)).toBeDefined();
    expect(screen.getByText(/Severidade Crítica/i)).toBeDefined();
    expect(screen.getByText(/Margem Negativa no Pedido #MLB-882910/i)).toBeDefined();
    expect(screen.getByText(/Estoque Baixo: Teclado Mecânico RGB/i)).toBeDefined();
  });

  it('switches between subtabs (Alerts, Tasks, Rules, Notifications)', () => {
    render(<ActionCenterView />);

    // Switch to Tasks
    const tasksTab = screen.getByRole('button', { name: /Action Center \(Tarefas\)/i });
    fireEvent.click(tasksTab);
    expect(screen.getByText(/Status da Tarefa:/i)).toBeDefined();
    expect(screen.getByText(/Ajustar regra de preço mínimo para evitar margem negativa/i)).toBeDefined();

    // Switch to Rules & Simulator
    const rulesTab = screen.getByRole('button', { name: /Catálogo de Regras & Simulador/i });
    fireEvent.click(rulesTab);
    expect(screen.getByText(/Margem de Contribuição Mínima/i)).toBeDefined();
    expect(screen.getByText(/Risco de Ruptura de Estoque/i)).toBeDefined();

    // Switch to Governance & Opt-In
    const notifTab = screen.getByRole('button', { name: /Governança & Opt-In/i });
    fireEvent.click(notifTab);
    expect(screen.getByText(/Consentimento e Governança Anti-Spam/i)).toBeDefined();
    expect(screen.getByText(/Histórico de Entregas e Supressões Auditadas/i)).toBeDefined();
  });

  it('converts an alert into an operational task and transitions status', () => {
    render(<ActionCenterView />);

    // Find and click "Converter em Tarefa" for the first alert
    const convertButtons = screen.getAllByRole('button', { name: /Converter em Tarefa/i });
    fireEvent.click(convertButtons[0]);

    // Modal should be visible
    expect(screen.getByText(/Converter Alerta em Tarefa Operacional/i)).toBeDefined();

    // Submit modal
    const submitBtn = screen.getByRole('button', { name: /Criar Tarefa Rastreável/i });
    fireEvent.click(submitBtn);

    // Switch to Tasks subtab to verify creation
    const tasksTab = screen.getByRole('button', { name: /Action Center \(Tarefas\)/i });
    fireEvent.click(tasksTab);

    expect(screen.getByText(/Resolver: Margem Negativa no Pedido #MLB-882910/i)).toBeDefined();
  });

  it('executes rule simulation and triggers alert feedback', () => {
    render(<ActionCenterView />);

    // Navigate to Rules subtab
    const rulesTab = screen.getByRole('button', { name: /Catálogo de Regras & Simulador/i });
    fireEvent.click(rulesTab);

    // Click "Testar / Simular" on first rule
    const simulateButtons = screen.getAllByRole('button', { name: /Testar \/ Simular/i });
    fireEvent.click(simulateButtons[0]);

    expect(screen.getByText(/Simulador de Regra: Margem de Contribuição Mínima/i)).toBeDefined();

    // Run simulation
    const runBtn = screen.getByRole('button', { name: /Executar Simulação/i });
    fireEvent.click(runBtn);

    // Feedback should appear
    expect(screen.getByText(/Alerta Disparado!/i)).toBeDefined();
    expect(screen.getByText(/Simulação DISPAROU/i)).toBeDefined();
  });

  it('allows toggling notification opt-in and reflects consent state', () => {
    render(<ActionCenterView />);

    // Navigate to Notifications subtab
    const notifTab = screen.getByRole('button', { name: /Governança & Opt-In/i });
    fireEvent.click(notifTab);

    // Initial state: Webhook has SEM CONSENTIMENTO
    expect(screen.getAllByText(/SEM CONSENTIMENTO/i).length).toBeGreaterThan(0);

    // Click "Habilitar Opt-In"
    const enableOptInBtn = screen.getByRole('button', { name: /Habilitar Opt-In/i });
    fireEvent.click(enableOptInBtn);

    // Should now show OPT-IN ATIVO
    expect(screen.getAllByText(/OPT-IN ATIVO/i).length).toBe(3);
  });
});
