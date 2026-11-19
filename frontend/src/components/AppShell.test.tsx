import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { AppShell } from './AppShell';

describe('AppShell Component (Shell Responsivo & Navegação)', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
    vi.spyOn(global, 'fetch').mockImplementation(() =>
      Promise.resolve({
        ok: true,
        json: async () => ({ content: [], totalElements: 0 }),
      } as unknown as Response)
    );
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('deve renderizar o cabeçalho master com logo, título, tenant e status Fail-Closed', () => {
    render(<AppShell />);

    expect(screen.getByText('Seller Intelligence Platform')).toBeDefined();
    expect(screen.getByText('Matriz Principal')).toBeDefined();
    expect(screen.getAllByText(/Fail-Closed Protegido/i).length).toBeGreaterThan(0);
  });

  it('deve permitir navegar entre módulos selecionando na lista lateral', () => {
    render(<AppShell />);

    // Por padrão o módulo inicial é Finanças
    expect(screen.getAllByText(/Motor Financeiro Determinístico/i).length).toBeGreaterThan(0);

    // Clica no módulo de Product Discovery na sidebar
    const discoveryBtns = screen.getAllByRole('button', { name: /Product Discovery/i });
    fireEvent.click(discoveryBtns[0]);

    // O placeholder deve atualizar para o módulo de discovery
    expect(screen.getByText('Market Intelligence')).toBeDefined();
    expect(screen.getAllByText('Product Discovery').length).toBeGreaterThan(0);
  });

  it('deve renderizar a tela MarketIntelligenceView ao selecionar o módulo MARKET (Pesquisa de Mercado)', () => {
    render(<AppShell />);

    const marketBtns = screen.getAllByRole('button', { name: /Pesquisa de Mercado/i });
    fireEvent.click(marketBtns[0]);

    expect(
      screen.getByRole('heading', { name: /Inteligência de Mercado & Proveniência/i })
    ).toBeInTheDocument();
  });

  it('deve filtrar módulos pelo campo de busca rápida', () => {
    render(<AppShell />);

    const searchInput = screen.getByPlaceholderText(/Buscar módulo.../i);
    fireEvent.change(searchInput, { target: { value: 'Compliance' } });

    expect(screen.getAllByText(/Continuous Compliance/i).length).toBeGreaterThan(0);

    // Limpa a busca
    const clearBtn = screen.getByRole('button', { name: /Limpar busca/i });
    fireEvent.click(clearBtn);
    expect(searchInput.getAttribute('value')).toBe('');
  });

  it('deve alternar entre os estados de demonstração de UI (Normal, Vazio, Loading)', () => {
    render(<AppShell />);

    // Alterna para Estado Vazio
    const emptyBtn = screen.getByRole('button', { name: 'Estado Vazio' });
    fireEvent.click(emptyBtn);

    expect(screen.getByRole('region', { name: /Nenhum dado encontrado/i })).toBeDefined();
    expect(screen.getByRole('button', { name: 'Conectar Nova Loja' })).toBeDefined();

    // Alterna para Loading (Skeleton)
    const loadingBtn = screen.getByRole('button', { name: 'Carregando (Skeleton)' });
    fireEvent.click(loadingBtn);

    expect(screen.getByRole('status', { name: /Carregando conteúdo.../i })).toBeDefined();

    // Retorna para Conteúdo Normal
    const normalBtn = screen.getByRole('button', { name: 'Conteúdo Normal' });
    fireEvent.click(normalBtn);

    expect(screen.queryByRole('status')).toBeNull();
    expect(screen.getByText(/Gestão de Custos, Taxas e Simulação Financeira/i)).toBeDefined();
  });

  it('deve abrir o menu gaveta mobile e fechar via botão e tecla Escape', () => {
    render(<AppShell />);

    const hamburgerBtn = screen.getByRole('button', { name: /Abrir menu de navegação/i });
    fireEvent.click(hamburgerBtn);

    // Gaveta aberta
    expect(screen.getByRole('dialog', { name: /Menu principal de navegação móvel/i })).toBeDefined();
    expect(screen.getByText('Menu Principal')).toBeDefined();

    // Fecha com tecla Escape
    fireEvent.keyDown(window, { key: 'Escape' });
    expect(screen.queryByRole('dialog')).toBeNull();

    // Abre novamente e fecha via botão
    fireEvent.click(hamburgerBtn);
    expect(screen.getByRole('dialog')).toBeDefined();

    const closeBtn = screen.getByRole('button', { name: /Fechar menu/i });
    fireEvent.click(closeBtn);
    expect(screen.queryByRole('dialog')).toBeNull();
  });

  it('deve renderizar a tela MarketplaceIntegrationsView ao selecionar o módulo STORES (Lojas & Marketplaces)', () => {
    render(<AppShell />);

    // Clica no botão do módulo Lojas na barra lateral
    const storesBtns = screen.getAllByRole('button', { name: /Lojas & Marketplaces/i });
    fireEvent.click(storesBtns[0]);

    // Deve renderizar a visualização de integrações oficiais
    expect(
      screen.getByRole('heading', { name: /Lojas & Integrações Oficiais/i })
    ).toBeInTheDocument();
    expect(
      screen.getByRole('region', { name: /Gestão de Integrações e Capacidades de Marketplaces/i })
    ).toBeInTheDocument();
  });

  it('deve renderizar a tela CatalogAndListingsView ao selecionar o módulo SYNC (Catálogo & Anúncios Próprios)', () => {
    render(<AppShell />);

    // Clica no botão do módulo Catálogo na barra lateral
    const catalogBtns = screen.getAllByRole('button', { name: /Catálogo & Anúncios Próprios/i });
    fireEvent.click(catalogBtns[0]);

    // Deve renderizar o título do módulo de catálogo
    expect(
      screen.getByRole('heading', { name: /Catálogo de Produtos & Anúncios/i })
    ).toBeInTheDocument();
  });

  it('deve renderizar a tela OrdersView ao selecionar o módulo ORDERS (Pedidos de Venda & Reconciliação)', () => {
    render(<AppShell />);

    // Clica no botão do módulo Pedidos na barra lateral
    const ordersBtns = screen.getAllByRole('button', { name: /Pedidos de Venda & Reconciliação/i });
    fireEvent.click(ordersBtns[0]);

    // Deve renderizar o título do módulo de pedidos
    expect(
      screen.getByRole('heading', { name: /Pedidos de Venda & Reconciliação/i })
    ).toBeInTheDocument();
  });

  it('deve renderizar a tela ProductMonitoringView ao selecionar o módulo MONITORING (Monitoramento & Concorrentes)', () => {
    render(<AppShell />);

    const monitoringBtns = screen.getAllByRole('button', { name: /Monitoramento & Concorrentes/i });
    fireEvent.click(monitoringBtns[0]);

    expect(
      screen.getByRole('heading', { name: /Monitoramento de Produtos & Concorrentes/i })
    ).toBeInTheDocument();
  });

  it('deve renderizar a tela PricingAndCostsView ao selecionar o módulo FINANCIAL (Motor Financeiro)', () => {
    render(<AppShell />);

    const financialBtns = screen.getAllByRole('button', { name: /Motor Financeiro Determinístico/i });
    fireEvent.click(financialBtns[0]);

    expect(
      screen.getByRole('heading', { name: /Gestão de Custos, Taxas e Simulação Financeira/i })
    ).toBeInTheDocument();
  });

  it('deve renderizar a tela CompaniesAndAccountsView ao selecionar o módulo COMPANIES (Empresas & CNPJs)', () => {
    render(<AppShell />);

    const companyBtns = screen.getAllByRole('button', { name: /Empresas & CNPJs/i });
    fireEvent.click(companyBtns[0]);

    expect(
      screen.getByRole('heading', { name: /Empresas & Unidades de Negócio/i })
    ).toBeInTheDocument();
  });

  it('deve renderizar a tela FinancialFactsDashboardView ao selecionar o módulo RECONCILIATION_DRE (DRE Gerencial & Conciliação)', () => {
    render(<AppShell />);

    const dreBtns = screen.getAllByRole('button', { name: /DRE Gerencial & Conciliação/i });
    fireEvent.click(dreBtns[0]);

    expect(
      screen.getByRole('heading', { name: /Painel Financeiro & Conciliação/i })
    ).toBeInTheDocument();
  });

  it('deve renderizar a tela ListingAnalyticsDashboardView ao selecionar o módulo LISTING_ANALYTICS (Analytics de Anúncios)', () => {
    render(<AppShell />);

    const analyticsBtns = screen.getAllByRole('button', { name: /Analytics de Anúncios/i });
    fireEvent.click(analyticsBtns[0]);

    expect(
      screen.getByRole('heading', { name: /Analytics de Anúncios & Desempenho Oficial/i })
    ).toBeInTheDocument();
  });

  it('deve renderizar a tela AiAssistantView ao selecionar o módulo ASSISTANT (Assistente de IA & Governança)', () => {
    render(<AppShell />);

    const assistantBtns = screen.getAllByRole('button', { name: /Assistente de IA/i });
    fireEvent.click(assistantBtns[0]);

    expect(
      screen.getByRole('heading', { name: /Assistente de IA & Governança Analítica/i })
    ).toBeInTheDocument();
  });
});
