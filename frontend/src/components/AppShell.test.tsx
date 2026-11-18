import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, beforeEach } from 'vitest';
import { AppShell } from './AppShell';

describe('AppShell Component (Shell Responsivo & Navegação)', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
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

    // Clica no módulo de Mercado na sidebar
    const marketBtns = screen.getAllByRole('button', { name: /Pesquisa de Mercado/i });
    fireEvent.click(marketBtns[0]);

    // O placeholder deve atualizar para o módulo de mercado
    expect(screen.getByText('Market Intelligence')).toBeDefined();
    expect(screen.getAllByText('Pesquisa de Mercado').length).toBeGreaterThan(0);
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
    expect(screen.getByText('Segurança & Isolamento')).toBeDefined();
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
});
