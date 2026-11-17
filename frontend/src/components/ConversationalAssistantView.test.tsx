import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import React from 'react';
import { ConversationalAssistantView } from './ConversationalAssistantView';

describe('ConversationalAssistantView (Prompt 18 - Assistente Conversacional & MCP Read-Only)', () => {
  it('should render compliance banner, OWASP LLM01 badge and read-only disclaimers correctly', () => {
    render(<ConversationalAssistantView />);

    expect(
      screen.getByText(/Assistente Conversacional & Servidor MCP/i)
    ).toBeDefined();
    expect(
      screen.getByText(/Fase 3 • FR-AI & FR-MCP • Read-Only/i)
    ).toBeDefined();
    expect(
      screen.getByText(/Anti-Injection Ativo \(OWASP LLM01\)/i)
    ).toBeDefined();
    expect(
      screen.getByText(/Governança Read-Only:/i)
    ).toBeDefined();
    expect(
      screen.getByText(/O servidor MCP e o assistente operam sob bloqueio de operações mutáveis/i)
    ).toBeDefined();
  });

  it('should render the 4 navigation sub-tabs and allow switching to MCP tools catalog', () => {
    render(<ConversationalAssistantView />);

    expect(screen.getByText(/Chat Assistente/i)).toBeDefined();
    expect(screen.getByText(/Ferramentas MCP Read-Only \(5\)/i)).toBeDefined();
    expect(screen.getByText(/Chaves de API MCP/i)).toBeDefined();
    expect(screen.getByText(/Auditoria de Execuções/i)).toBeDefined();

    // Click on MCP Tools tab
    const toolsTabBtn = screen.getByText(/Ferramentas MCP Read-Only \(5\)/i);
    fireEvent.click(toolsTabBtn);

    expect(screen.getByTestId('tools-catalog-container')).toBeDefined();
    expect(screen.getByText('search_market')).toBeDefined();
    expect(screen.getByText('query_tenant_products')).toBeDefined();
    expect(screen.getByText('query_sales_indicators')).toBeDefined();
    expect(screen.getByText('calculate_sku_margin')).toBeDefined();
    expect(screen.getByText('query_reconciliation_summary')).toBeDefined();
  });

  it('should send a user prompt and display simulated grounded assistant response with tool badges', async () => {
    render(<ConversationalAssistantView />);

    const input = screen.getByTestId('chat-input-field');
    const sendBtn = screen.getByTestId('chat-send-btn');

    fireEvent.change(input, { target: { value: 'Qual o faturamento recente de vendas?' } });
    fireEvent.click(sendBtn);

    // Verify user message appears immediately
    expect(screen.getByText('Qual o faturamento recente de vendas?')).toBeDefined();

    // Wait for simulated assistant response
    await waitFor(() => {
      expect(screen.getByText(/Indicadores de Vendas \(Últimos 30 Dias\)/i)).toBeDefined();
      expect(screen.getByText(/query_sales_indicators/i)).toBeDefined();
      expect(screen.getByText(/Fonte: Base de Pedidos do Tenant/i)).toBeDefined();
      expect(screen.getByText('IMPORTED')).toBeDefined();
    }, { timeout: 1500 });
  });

  it('should trigger quick prompt suggestion and calculate SKU margin deterministically', async () => {
    render(<ConversationalAssistantView />);

    const quickPromptBtn = screen.getByText(/Margem SKU FONE-PRO-01/i);
    fireEvent.click(quickPromptBtn);

    await waitFor(() => {
      expect(screen.getByText(/Memória de Cálculo de Margem Determinística/i)).toBeDefined();
      expect(screen.getByText(/calculate_sku_margin/i)).toBeDefined();
      expect(screen.getByText(/Motor Financeiro Determinístico \(FinancialEngine\)/i)).toBeDefined();
      expect(screen.getByText('CALCULATED')).toBeDefined();
    }, { timeout: 1500 });
  });

  it('should block mutable action requests with BLOCKED_MUTABLE_DENIED', async () => {
    render(<ConversationalAssistantView />);

    const mutableQuickPrompt = screen.getByText(/Testar Bloqueio de Ação Mutável/i);
    fireEvent.click(mutableQuickPrompt);

    await waitFor(() => {
      expect(screen.getByText(/Operação Mutável Bloqueada/i)).toBeDefined();
      expect(screen.getByText('BLOCKED_MUTABLE_DENIED')).toBeDefined();
    }, { timeout: 1500 });
  });

  it('should detect adversarial prompt injection and display OWASP LLM01 security warning', async () => {
    render(<ConversationalAssistantView />);

    const injectionPrompt = screen.getByText(/Testar Anti-Prompt Injection/i);
    fireEvent.click(injectionPrompt);

    await waitFor(() => {
      expect(screen.getByText(/Aviso de Segurança Anti-Prompt Injection \(OWASP LLM01\)/i)).toBeDefined();
      expect(screen.getByText(/Fonte: MCP_SECURITY_GATE/i)).toBeDefined();
    }, { timeout: 1500 });
  });

  it('should open new API key modal, generate key with sip_mcp_ prefix and revoke active key', () => {
    render(<ConversationalAssistantView />);

    // Switch to API Keys subtab
    const apiKeysTabBtn = screen.getByText(/Chaves de API MCP/i);
    fireEvent.click(apiKeysTabBtn);

    expect(screen.getByTestId('api-keys-container')).toBeDefined();
    expect(screen.getByText('Cursor MCP Integration')).toBeDefined();

    // Open Modal
    const newKeyBtn = screen.getByTestId('new-api-key-btn');
    fireEvent.click(newKeyBtn);

    expect(screen.getByText('Gerar Nova Chave MCP')).toBeDefined();

    // Fill in name
    const inputName = screen.getByPlaceholderText(/Ex: Cursor MCP Integration/i);
    fireEvent.change(inputName, { target: { value: 'VSCode Copilot' } });

    const generateBtn = screen.getByRole('button', { name: /Gerar Chave/i });
    fireEvent.click(generateBtn);

    // Check plain key generated with prefix sip_mcp_
    expect(screen.getAllByText(/sip_mcp_/i).length).toBeGreaterThan(1);

    // Close modal
    const doneBtn = screen.getByRole('button', { name: /Concluído/i });
    fireEvent.click(doneBtn);

    // Verify newly added key in table
    expect(screen.getByText('VSCode Copilot')).toBeDefined();

    // Revoke the first key
    const revokeButtons = screen.getAllByRole('button', { name: /Revogar/i });
    expect(revokeButtons.length).toBeGreaterThan(0);
    fireEvent.click(revokeButtons[0]);

    // Check revoked status
    expect(screen.getByText('REVOKED')).toBeDefined();
  });

  it('should switch to execution logs subtab and display immutable audit rows', () => {
    render(<ConversationalAssistantView />);

    const logsTabBtn = screen.getByText(/Auditoria de Execuções/i);
    fireEvent.click(logsTabBtn);

    expect(screen.getByTestId('mcp-logs-container')).toBeDefined();
    expect(screen.getByText('query_sales_indicators')).toBeDefined();
    expect(screen.getByText('update_price')).toBeDefined();
    expect(screen.getByText('BLOCKED_MUTABLE_DENIED')).toBeDefined();
  });
});
