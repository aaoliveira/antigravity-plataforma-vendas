import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import React from 'react';
import { MarketplaceIntegrationsView } from './MarketplaceIntegrationsView';

describe('P004-FE: MarketplaceIntegrationsView Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('1. Renderiza a tela de integrações oficiais com status e data da última verificação', () => {
    render(<MarketplaceIntegrationsView />);

    expect(
      screen.getByRole('heading', { name: /Lojas & Integrações Oficiais/i })
    ).toBeInTheDocument();

    expect(screen.getByTestId('last-verification-date')).toHaveTextContent('18/11/2026');
    expect(screen.getByText(/read, offline_access/i)).toBeInTheDocument();
    expect(screen.getByText(/20 req\/s por token/i)).toBeInTheDocument();
  });

  it('2. Distingue visualmente os 4 estados de capacidades (SUPPORTED, RESTRICTED, UNSUPPORTED, UNVERIFIED)', () => {
    render(<MarketplaceIntegrationsView />);

    // Mercado Livre tem SUPPORTED, RESTRICTED e UNSUPPORTED
    const supportedBadges = screen.getAllByTestId('badge-supported');
    expect(supportedBadges.length).toBeGreaterThan(0);
    expect(supportedBadges[0]).toHaveTextContent(/Homologado \/ Suportado/i);

    const restrictedBadges = screen.getAllByTestId('badge-restricted');
    expect(restrictedBadges.length).toBeGreaterThan(0);
    expect(restrictedBadges[0]).toHaveTextContent(/Acesso Restrito \(MVP\)/i);

    const unsupportedBadges = screen.getAllByTestId('badge-unsupported');
    expect(unsupportedBadges.length).toBeGreaterThan(0);
    expect(unsupportedBadges[0]).toHaveTextContent(/Não Disponibilizado pelo Canal/i);

    // Selecionar canal Amazon para verificar UNVERIFIED
    const amazonTab = screen.getByRole('tab', { name: /Amazon \(SP-API\)/i });
    fireEvent.click(amazonTab);

    const unverifiedBadges = screen.getAllByTestId('badge-unverified');
    expect(unverifiedBadges.length).toBeGreaterThan(0);
    expect(unverifiedBadges[0]).toHaveTextContent(/Em Validação Técnica \(Bloqueado\)/i);
  });

  it('3. Apresenta explicação clara para recursos não disponibilizados (UNSUPPORTED) e proibição de scraping', () => {
    render(<MarketplaceIntegrationsView />);

    // Volume de Vendas de Concorrentes é UNSUPPORTED no Mercado Livre
    expect(screen.getByText(/Volume de Vendas de Concorrentes/i)).toBeInTheDocument();
    expect(
      screen.getByText(/As APIs oficiais do Mercado Livre não fornecem volume de vendas ou receita de terceiros/i)
    ).toBeInTheDocument();
    expect(
      screen.getByText(/proíbe terminantemente web scraping, engenharia reversa ou métricas forjadas/i)
    ).toBeInTheDocument();
  });

  it('4. NÃO apresenta capacidades UNVERIFIED como disponíveis para uso', () => {
    render(<MarketplaceIntegrationsView />);

    // Clica na tab da Amazon
    const amazonTab = screen.getByRole('tab', { name: /Amazon \(SP-API\)/i });
    fireEvent.click(amazonTab);

    // O status do canal e capacidades não deve oferecer botões ativos de conexão ou execução
    expect(screen.getByText(/Em Homologação Documental \(Ações Indisponíveis\)/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Conectar com Amazon/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Atualizar status de Amazon/i })).not.toBeInTheDocument();

    // A capacidade não homologada exibe aviso de Fail-Closed
    expect(
      screen.getByText(/Sob a regra Fail-Closed, este recurso permanece bloqueado até homologação formal/i)
    ).toBeInTheDocument();
  });

  it('5. Não exibe NENHUM token de autenticação, senha ou segredo no DOM ou atributos', () => {
    const { container } = render(<MarketplaceIntegrationsView />);
    const htmlContent = container.innerHTML;

    // Padrões de tokens e segredos estritamente proibidos no DOM
    expect(htmlContent).not.toMatch(/bearer\s+[a-zA-Z0-9_\-\.]+/i);
    expect(htmlContent).not.toMatch(/ey[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}/); // JWT pattern
    expect(htmlContent).not.toMatch(/client_secret/i);
    expect(htmlContent).not.toMatch(/refresh_token\s*[:=]/i);
    expect(htmlContent).not.toMatch(/access_token\s*[:=]/i);
  });

  it('6. Permite exclusivamente as ações autorizadas: Conectar, Atualizar Status e Revogar', async () => {
    render(<MarketplaceIntegrationsView />);

    // Canal ativo Mercado Livre: botões Atualizar Status e Revogar
    const refreshBtn = screen.getByRole('button', { name: /Atualizar status de Mercado Livre/i });
    const revokeBtn = screen.getByRole('button', { name: /Revogar conexão com Mercado Livre/i });

    expect(refreshBtn).toBeInTheDocument();
    expect(revokeBtn).toBeInTheDocument();

    // Nenhuma ação indevida (escrita, scraping, alteração de preço)
    expect(screen.queryByRole('button', { name: /Alterar Preço/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Publicar Anúncio/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Scraping/i })).not.toBeInTheDocument();

    // Executar Revogação
    fireEvent.click(revokeBtn);

    await waitFor(() => {
      expect(
        screen.getByText(/Autorização do canal Mercado Livre revogada com sucesso/i)
      ).toBeInTheDocument();
    });

    // Após revogar, o canal fica DISCONNECTED e exibe exclusivamente o botão Conectar
    const connectBtn = screen.getByRole('button', { name: /Conectar com Mercado Livre/i });
    expect(connectBtn).toBeInTheDocument();

    // Executar Conectar - abre o modal oficial de permissões e segurança OAuth (P005-FE)
    fireEvent.click(connectBtn);

    const dialog = screen.getByRole('dialog', { name: /Conectar Conta do Mercado Livre/i });
    expect(dialog).toBeInTheDocument();
    expect(within(dialog).getByText(/A sua senha NUNCA é solicitada nesta aplicação/i)).toBeInTheDocument();
    expect(within(dialog).getByText(/^read$/i)).toBeInTheDocument();
    expect(within(dialog).getByText(/^offline_access$/i)).toBeInTheDocument();

    // Fecha o modal pelo botão fechar
    const closeBtn = within(dialog).getByRole('button', { name: /Fechar modal/i });
    fireEvent.click(closeBtn);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();

    // Testa conexão de canal secundário (Shopee)
    const shopeeTab = screen.getByRole('tab', { name: /Shopee/i });
    fireEvent.click(shopeeTab);

    const revokeShopeeBtn = screen.getByRole('button', { name: /Revogar conexão com Shopee/i });
    fireEvent.click(revokeShopeeBtn);

    await waitFor(() => {
      expect(
        screen.getByText(/Autorização do canal Shopee revogada com sucesso/i)
      ).toBeInTheDocument();
    });

    const connectShopeeBtn = screen.getByRole('button', { name: /Conectar com Shopee/i });
    fireEvent.click(connectShopeeBtn);

    // Executar Conectar Shopee abre o modal oficial de orientações e consentimento (P006-FE)
    const shopeeDialog = screen.getByRole('dialog', { name: /Conectar Loja da Shopee/i });
    expect(shopeeDialog).toBeInTheDocument();
    expect(within(shopeeDialog).getByText(/A sua senha NUNCA é solicitada nesta aplicação/i)).toBeInTheDocument();
    expect(within(shopeeDialog).getByText(/Shopee Open Platform OpenAPI v2/i)).toBeInTheDocument();

    const closeShopeeBtn = within(shopeeDialog).getByRole('button', { name: /Fechar modal/i });
    fireEvent.click(closeShopeeBtn);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('7. Exibe mensagens de erro devidamente sanitizadas sem expor UUIDs técnicos ou stacktraces', () => {
    render(<MarketplaceIntegrationsView />);

    const auditErrorBtn = screen.getByRole('button', { name: /Auditar sanitização de erros/i });
    fireEvent.click(auditErrorBtn);

    const alert = screen.getByTestId('feedback-alert');
    expect(alert).toBeInTheDocument();

    // Não deve conter UUID técnico
    expect(alert.textContent).not.toMatch(/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/);
    // Não deve conter nomes de classes internas Java
    expect(alert.textContent).not.toContain('java.net.SocketTimeoutException');
    // Deve conter texto sanitizado
    expect(alert.textContent).toContain('[recurso]');
  });

  it('8. Suporta filtragem por status de capacidade', () => {
    render(<MarketplaceIntegrationsView />);

    // Clica no filtro "Restritas"
    const restrictedFilterBtn = screen.getByRole('button', { name: /Restritas/i });
    fireEvent.click(restrictedFilterBtn);

    // Deve exibir somente a capacidade restrita
    expect(screen.getByText(/Operações de Escrita em Anúncios/i)).toBeInTheDocument();
    expect(screen.queryByText(/Leitura de Catálogo Próprio/i)).not.toBeInTheDocument();

    // Clica no filtro "Todas"
    const allFilterBtn = screen.getByRole('button', { name: /Todas/i });
    fireEvent.click(allFilterBtn);

    expect(screen.getByText(/Leitura de Catálogo Próprio/i)).toBeInTheDocument();
  });

  it('9. Respeita requisitos de toque mobile (min-height >= 44px)', () => {
    render(<MarketplaceIntegrationsView />);

    const refreshBtn = screen.getByRole('button', { name: /Atualizar status de Mercado Livre/i });
    expect(refreshBtn.className).toContain('min-h-[44px]');

    const revokeBtn = screen.getByRole('button', { name: /Revogar conexão com Mercado Livre/i });
    expect(revokeBtn.className).toContain('min-h-[44px]');
  });
});
