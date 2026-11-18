import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import React from 'react';
import { MarketplaceIntegrationsView } from './MarketplaceIntegrationsView';

describe('P005-FE: Fluxo Oficial de Conexão com Mercado Livre (OAuth 2.0 PKCE)', () => {
  const originalLocation = window.location;
  const originalHistory = window.history;

  beforeEach(() => {
    vi.clearAllMocks();

    // Mock seguro de window.location e window.history para testes de redirecionamento e URL params
    const mockLocation = {
      ...originalLocation,
      search: '',
      pathname: '/integrations',
      assign: vi.fn(),
      replace: vi.fn(),
      href: 'http://localhost:3000/integrations',
    };

    Object.defineProperty(window, 'location', {
      writable: true,
      configurable: true,
      value: mockLocation,
    });

    vi.spyOn(window.history, 'replaceState').mockImplementation(() => {});
  });

  afterEach(() => {
    Object.defineProperty(window, 'location', {
      writable: true,
      configurable: true,
      value: originalLocation,
    });
    vi.restoreAllMocks();
  });

  it('1. Explica permissões (read, offline_access), garante Risco Zero (sem write) e NUNCA solicita senha', async () => {
    render(<MarketplaceIntegrationsView />);

    // Revoga conexão inicial para expor o botão de conexão
    const revokeBtn = screen.getByRole('button', { name: /Revogar conexão com Mercado Livre/i });
    fireEvent.click(revokeBtn);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Conectar com Mercado Livre/i })).toBeInTheDocument();
    });

    const connectBtn = screen.getByRole('button', { name: /Conectar com Mercado Livre/i });
    fireEvent.click(connectBtn);

    // O modal deve estar visível e acessível
    const dialog = screen.getByRole('dialog', { name: /Conectar Conta do Mercado Livre/i });
    expect(dialog).toBeInTheDocument();

    // 1. Garantia explícita: A senha NUNCA é solicitada
    expect(
      within(dialog).getByText(/A sua senha NUNCA é solicitada nesta aplicação/i)
    ).toBeInTheDocument();
    expect(
      within(dialog).getByText(/Você será redirecionado com segurança para o domínio oficial do Mercado Livre/i)
    ).toBeInTheDocument();

    // 2. Explicação dos escopos solicitados (read e offline_access)
    expect(within(dialog).getByText(/^read$/i)).toBeInTheDocument();
    expect(
      within(dialog).getByText(/Leitura de dados públicos, anúncios cadastrados, métricas oficiais/i)
    ).toBeInTheDocument();

    expect(within(dialog).getByText(/^offline_access$/i)).toBeInTheDocument();
    expect(
      within(dialog).getByText(/Permite renovação segura da sessão sem exigir que você refaça o login/i)
    ).toBeInTheDocument();

    // 3. Garantia de Menor Privilégio: write NÃO é solicitado
    expect(within(dialog).getByText(/Garantia de Risco Zero:/i)).toBeInTheDocument();
    expect(
      within(dialog).getByText(/A plataforma nunca alterará preços, títulos, estoque ou anúncios/i)
    ).toBeInTheDocument();

    // 4. Inexistência absoluta de campo de senha na aplicação
    expect(document.querySelector('input[type="password"]')).toBeNull();
  });

  it('2. Redireciona o usuário para o domínio oficial auth.mercadolivre.com.br com PKCE e state', async () => {
    const mockAuthUrl =
      'https://auth.mercadolivre.com.br/authorization?response_type=code&client_id=123456789&state=anti-csrf-secure-state-token&code_challenge=xyz789&code_challenge_method=S256';

    global.fetch = vi.fn().mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        authorizationUrl: mockAuthUrl,
        state: 'anti-csrf-secure-state-token',
        expiresInSeconds: 300,
      }),
    });

    render(<MarketplaceIntegrationsView />);

    // Revoga para expor botão conectar
    const revokeBtn = screen.getByRole('button', { name: /Revogar conexão com Mercado Livre/i });
    fireEvent.click(revokeBtn);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Conectar com Mercado Livre/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Conectar com Mercado Livre/i }));

    const dialog = screen.getByRole('dialog', { name: /Conectar Conta do Mercado Livre/i });
    const continueBtn = within(dialog).getByRole('button', { name: /Continuar para Mercado Livre/i });

    fireEvent.click(continueBtn);

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith('/api/integrations/mercadolivre/authorize', {
        method: 'POST',
      });
      // Verifica redirecionamento para o domínio oficial
      expect(window.location.assign).toHaveBeenCalledWith(mockAuthUrl);
    });
  });

  it('3. Processa callback oficial, limpa URL imediatamente e atualiza status para ACTIVE', async () => {
    // Simula retorno do Mercado Livre com code e state
    window.location.search = '?code=TG-673b4e-meli-auth-code&state=anti-csrf-secure-state-token';

    global.fetch = vi.fn().mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        id: '99999999-9999-9999-9999-999999999999',
        tenantId: '11111111-1111-1111-1111-111111111111',
        marketplaceType: 'MERCADO_LIVRE',
        status: 'ACTIVE',
        accountIdentifier: 'SELLER_TEST_123',
        scopes: ['read', 'offline_access'],
        connectedAt: '2026-11-18T10:00:00Z',
      }),
    });

    render(<MarketplaceIntegrationsView />);

    // 1. A URL deve ser limpa imediatamente pelo window.history.replaceState
    expect(window.history.replaceState).toHaveBeenCalledWith(
      {},
      expect.any(String),
      '/integrations'
    );

    // 2. O backend de callback deve ser acionado com code e state
    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith('/api/integrations/mercadolivre/callback', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          code: 'TG-673b4e-meli-auth-code',
          state: 'anti-csrf-secure-state-token',
        }),
      });
    });

    // 3. Status é refletido como Conectado / ACTIVE com mensagem de sucesso
    await waitFor(() => {
      expect(
        screen.getByText(/Canal Mercado Livre conectado com sucesso via fluxo oficial OAuth 2.0 PKCE/i)
      ).toBeInTheDocument();
    });

    // 4. Tab do canal exibe Conectado
    const meliTab = screen.getByRole('tab', { name: /Mercado Livre/i });
    expect(within(meliTab).getByText('Conectado')).toBeInTheDocument();
  });

  it('4. Callback NÃO expõe access_token, refresh_token ou segredos no DOM', async () => {
    window.location.search = '?code=TG-secret-code-token&state=anti-csrf-token';

    global.fetch = vi.fn().mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        id: '22222222-2222-2222-2222-222222222222',
        tenantId: '11111111-1111-1111-1111-111111111111',
        marketplaceType: 'MERCADO_LIVRE',
        status: 'ACTIVE',
        accountIdentifier: 'SELLER_DOM_TEST',
        scopes: ['read', 'offline_access'],
        connectedAt: '2026-11-18T10:00:00Z',
      }),
    });

    const { container } = render(<MarketplaceIntegrationsView />);

    await waitFor(() => {
      expect(
        screen.getByText(/Canal Mercado Livre conectado com sucesso via fluxo oficial OAuth 2.0 PKCE/i)
      ).toBeInTheDocument();
    });

    const html = container.innerHTML;
    expect(html).not.toMatch(/TG-secret-code-token/);
    expect(html).not.toMatch(/access_token\s*[:=]/i);
    expect(html).not.toMatch(/refresh_token\s*[:=]/i);
    expect(html).not.toMatch(/bearer\s+[a-zA-Z0-9_\-\.]+/i);
  });

  it('5. Trata cancelamento de autorização pelo usuário (access_denied) com mensagem compreensível', async () => {
    // Simula retorno do Mercado Livre com erro de cancelamento
    window.location.search = '?error=access_denied&error_description=The+user+denied+access';

    render(<MarketplaceIntegrationsView />);

    // URL limpa imediatamente
    expect(window.history.replaceState).toHaveBeenCalledWith(
      {},
      expect.any(String),
      '/integrations'
    );

    // Mensagem compreensível e amigável ao vendedor
    await waitFor(() => {
      expect(
        screen.getByText(/Autorização cancelada no Mercado Livre pelo usuário\. Nenhuma alteração foi realizada na sua conta\./i)
      ).toBeInTheDocument();
    });
  });

  it('6. Trata state anti-CSRF inválido ou expirado com erro claro sem stacktrace', async () => {
    window.location.search = '?code=TG-valid-code&state=expired-state';

    global.fetch = vi.fn().mockResolvedValueOnce({
      ok: false,
      status: 400,
      json: async () => ({
        error: 'CALLBACK_FAILED',
        message: 'Falha na validação de segurança da autorização (state inválido ou expirado).',
      }),
    });

    render(<MarketplaceIntegrationsView />);

    await waitFor(() => {
      const alert = screen.getByTestId('feedback-alert');
      expect(alert).toBeInTheDocument();
      expect(alert).toHaveTextContent(/Falha na validação de segurança da autorização \(state inválido ou expirado\)/i);
      expect(alert.textContent).not.toMatch(/Exception|StackTrace|NullPointer/);
    });
  });

  it('7. Garante responsividade mobile e acessibilidade (touch targets >= 44px e teclado ESC)', async () => {
    render(<MarketplaceIntegrationsView />);

    // Revoga para abrir modal
    const revokeBtn = screen.getByRole('button', { name: /Revogar conexão com Mercado Livre/i });
    fireEvent.click(revokeBtn);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Conectar com Mercado Livre/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Conectar com Mercado Livre/i }));

    const dialog = screen.getByRole('dialog', { name: /Conectar Conta do Mercado Livre/i });
    expect(dialog).toBeInTheDocument();

    // Valida touch targets dos botões do modal
    const closeBtn = within(dialog).getByRole('button', { name: /Fechar modal/i });
    expect(closeBtn.className).toMatch(/min-h-\[44px\]/);
    expect(closeBtn.className).toMatch(/min-w-\[44px\]/);

    const cancelBtn = within(dialog).getByRole('button', { name: /Cancelar/i });
    expect(cancelBtn.className).toMatch(/min-h-\[44px\]/);

    const continueBtn = within(dialog).getByRole('button', { name: /Continuar para Mercado Livre/i });
    expect(continueBtn.className).toMatch(/min-h-\[44px\]/);

    // Valida acessibilidade por teclado: ESC fecha o modal
    fireEvent.keyDown(window, { key: 'Escape' });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});
