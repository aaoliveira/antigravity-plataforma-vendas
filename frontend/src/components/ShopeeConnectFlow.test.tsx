import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import React from 'react';
import { MarketplaceIntegrationsView } from './MarketplaceIntegrationsView';

describe('P006-FE: Fluxo Oficial de Conexão com Shopee (Open Platform v2 UI)', () => {
  const originalLocation = window.location;
  const originalHistory = window.history;

  beforeEach(() => {
    vi.clearAllMocks();

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

  it('1. Explica processo oficial Shopee OpenAPI v2, Shop Level, Webhooks Push e NUNCA solicita senha', async () => {
    render(<MarketplaceIntegrationsView />);

    // Seleciona a aba da Shopee
    const shopeeTab = screen.getByRole('tab', { name: /Shopee/i });
    fireEvent.click(shopeeTab);

    // Revoga conexão inicial para expor o botão de conexão
    const revokeBtn = screen.getByRole('button', { name: /Revogar conexão com Shopee/i });
    fireEvent.click(revokeBtn);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Conectar com Shopee/i })).toBeInTheDocument();
    });

    const connectBtn = screen.getByRole('button', { name: /Conectar com Shopee/i });
    fireEvent.click(connectBtn);

    // O modal deve estar visível e acessível
    const dialog = screen.getByRole('dialog', { name: /Conectar Loja da Shopee/i });
    expect(dialog).toBeInTheDocument();

    // 1. Garantia explícita: A senha NUNCA é solicitada
    expect(
      within(dialog).getByText(/A sua senha NUNCA é solicitada nesta aplicação/i)
    ).toBeInTheDocument();
    expect(
      within(dialog).getByText(/Você será redirecionado para a página oficial da Shopee Open Platform/i)
    ).toBeInTheDocument();

    // 2. Explicação dos acessos oficiais (Shop Level, Métricas e Webhooks Push)
    expect(within(dialog).getByText(/Loja Ativa na Shopee \(Shop Level\)/i)).toBeInTheDocument();
    expect(
      within(dialog).getByText(/Autorização de acesso aos produtos cadastrados/i)
    ).toBeInTheDocument();
    expect(within(dialog).getByText(/v2.product.get_item_list/i)).toBeInTheDocument();

    expect(within(dialog).getByText(/Métricas Oficiais de Anúncios/i)).toBeInTheDocument();
    expect(within(dialog).getByText(/v2.analytics/i)).toBeInTheDocument();

    expect(within(dialog).getByText(/Webhooks Push em Tempo Real/i)).toBeInTheDocument();
    expect(
      within(dialog).getByText(/Sincronização atômica de status\. Caso a autorização seja revogada/i)
    ).toBeInTheDocument();

    // 3. Garantia de Menor Privilégio: write NÃO é solicitado
    expect(within(dialog).getByText(/Garantia de Risco Zero:/i)).toBeInTheDocument();
    expect(
      within(dialog).getByText(/A aplicação opera com exclusividade em modo de análise e monitoramento/i)
    ).toBeInTheDocument();

    // 4. Inexistência absoluta de campo de senha na aplicação
    expect(document.querySelector('input[type="password"]')).toBeNull();
  });

  it('2. Redireciona o usuário para o portal oficial da Shopee com URL assinada por HMAC-SHA256', async () => {
    const mockAuthUrl =
      'https://partner.shopeemobile.com/api/v2/shop/auth_partner?partner_id=123456&timestamp=1700000000&sign=abcdef1234567890&redirect=http%3A%2F%2Flocalhost%3A3000%2Fintegrations';

    global.fetch = vi.fn().mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        authorizationUrl: mockAuthUrl,
        state: 'shopee-state-123',
        expiresInSeconds: 300,
      }),
    });

    render(<MarketplaceIntegrationsView />);

    const shopeeTab = screen.getByRole('tab', { name: /Shopee/i });
    fireEvent.click(shopeeTab);

    // Revoga para expor botão conectar
    const revokeBtn = screen.getByRole('button', { name: /Revogar conexão com Shopee/i });
    fireEvent.click(revokeBtn);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Conectar com Shopee/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Conectar com Shopee/i }));

    const dialog = screen.getByRole('dialog', { name: /Conectar Loja da Shopee/i });
    const continueBtn = within(dialog).getByRole('button', { name: /Continuar para Shopee/i });

    fireEvent.click(continueBtn);

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith('/api/integrations/shopee/authorize', {
        method: 'POST',
      });
      // Verifica redirecionamento para o portal oficial com assinatura criptográfica
      expect(window.location.assign).toHaveBeenCalledWith(mockAuthUrl);
    });
  });

  it('3. Processa callback oficial com code e shop_id, limpa URL imediatamente e atualiza status para ACTIVE', async () => {
    // Simula retorno da Shopee com code e shop_id
    window.location.search = '?code=shopee-auth-token-xyz&shop_id=987654321';

    global.fetch = vi.fn().mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        id: '88888888-8888-8888-8888-888888888888',
        tenantId: '11111111-1111-1111-1111-111111111111',
        marketplaceType: 'SHOPEE',
        status: 'ACTIVE',
        accountIdentifier: 'SHOP_987654321',
        scopes: ['v2.product.get_item_list', 'v2.order.get_order_list'],
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

    // 2. O backend de callback da Shopee deve ser acionado com code e shop_id numérico
    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith('/api/integrations/shopee/callback', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          code: 'shopee-auth-token-xyz',
          shop_id: 987654321,
        }),
      });
    });

    // 3. Status é refletido como Conectado com feedback de sucesso
    await waitFor(() => {
      expect(
        screen.getByText(/Canal Shopee conectado com sucesso via Shopee Open Platform v2/i)
      ).toBeInTheDocument();
    });

    // 4. Tab da Shopee exibe Conectado
    const shopeeTab = screen.getByRole('tab', { name: /Shopee/i });
    expect(within(shopeeTab).getByText('Conectado')).toBeInTheDocument();
  });

  it('4. Callback NÃO expõe access_token, refresh_token ou segredos no DOM', async () => {
    window.location.search = '?code=secret-shopee-code-99&shop_id=55555';

    global.fetch = vi.fn().mockResolvedValueOnce({
      ok: true,
      json: async () => ({
        id: '77777777-7777-7777-7777-777777777777',
        tenantId: '11111111-1111-1111-1111-111111111111',
        marketplaceType: 'SHOPEE',
        status: 'ACTIVE',
        accountIdentifier: 'SHOP_55555',
        scopes: ['v2.product.get_item_list'],
        connectedAt: '2026-11-18T10:00:00Z',
      }),
    });

    const { container } = render(<MarketplaceIntegrationsView />);

    await waitFor(() => {
      expect(
        screen.getByText(/Canal Shopee conectado com sucesso via Shopee Open Platform v2/i)
      ).toBeInTheDocument();
    });

    const html = container.innerHTML;
    expect(html).not.toMatch(/secret-shopee-code-99/);
    expect(html).not.toMatch(/access_token\s*[:=]/i);
    expect(html).not.toMatch(/refresh_token\s*[:=]/i);
    expect(html).not.toMatch(/partner_key/i);
    expect(html).not.toMatch(/bearer\s+[a-zA-Z0-9_\-\.]+/i);
  });

  it('5. Trata cancelamento de autorização na Shopee com mensagem compreensível e sanitização da URL', async () => {
    // Simula retorno da Shopee com cancelamento
    window.location.search = '?error=cancelled&message=User+declined+permission';

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
        screen.getByText(/Autorização cancelada na Shopee pelo usuário\. Nenhuma alteração foi realizada na sua conta\./i)
      ).toBeInTheDocument();
    });
  });

  it('6. Trata falha no callback da Shopee com erro sanitizado sem stacktraces técnicos', async () => {
    window.location.search = '?code=expired-code&shop_id=12345';

    global.fetch = vi.fn().mockResolvedValueOnce({
      ok: false,
      status: 400,
      json: async () => ({
        error: 'CALLBACK_FAILED',
        message: 'Código de autorização da Shopee expirado ou inválido.',
      }),
    });

    render(<MarketplaceIntegrationsView />);

    await waitFor(() => {
      const alert = screen.getByTestId('feedback-alert');
      expect(alert).toBeInTheDocument();
      expect(alert).toHaveTextContent(/Código de autorização da Shopee expirado ou inválido\./i);
      expect(alert.textContent).not.toMatch(/Exception|StackTrace|NullPointer/);
    });
  });

  it('7. Garante responsividade mobile e acessibilidade (touch targets >= 44px e tecla ESC)', async () => {
    render(<MarketplaceIntegrationsView />);

    const shopeeTab = screen.getByRole('tab', { name: /Shopee/i });
    fireEvent.click(shopeeTab);

    // Revoga para abrir modal
    const revokeBtn = screen.getByRole('button', { name: /Revogar conexão com Shopee/i });
    fireEvent.click(revokeBtn);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Conectar com Shopee/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Conectar com Shopee/i }));

    const dialog = screen.getByRole('dialog', { name: /Conectar Loja da Shopee/i });
    expect(dialog).toBeInTheDocument();

    // Valida touch targets dos botões do modal
    const closeBtn = within(dialog).getByRole('button', { name: /Fechar modal/i });
    expect(closeBtn.className).toMatch(/min-h-\[44px\]/);
    expect(closeBtn.className).toMatch(/min-w-\[44px\]/);

    const cancelBtn = within(dialog).getByRole('button', { name: /Cancelar/i });
    expect(cancelBtn.className).toMatch(/min-h-\[44px\]/);

    const continueBtn = within(dialog).getByRole('button', { name: /Continuar para Shopee/i });
    expect(continueBtn.className).toMatch(/min-h-\[44px\]/);

    // Valida acessibilidade por teclado: ESC fecha o modal
    fireEvent.keyDown(window, { key: 'Escape' });
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('8. Exibe indicador oficial dos Webhooks Push em tempo real (Push Mechanism v2)', () => {
    render(<MarketplaceIntegrationsView />);

    const shopeeTab = screen.getByRole('tab', { name: /Shopee/i });
    fireEvent.click(shopeeTab);

    const pushIndicator = screen.getByTestId('shopee-push-indicator');
    expect(pushIndicator).toBeInTheDocument();
    expect(pushIndicator).toHaveTextContent(/Webhooks Push Ativos \(HMAC-SHA256\)/i);
    expect(pushIndicator).toHaveTextContent(/Push Mechanism v2/i);
    expect(pushIndicator).toHaveTextContent(/Single-Use Token Rotation/i);
  });
});
