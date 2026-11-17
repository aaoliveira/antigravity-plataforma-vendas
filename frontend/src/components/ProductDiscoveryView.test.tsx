import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import React from 'react';
import { ProductDiscoveryView } from './ProductDiscoveryView';

describe('ProductDiscoveryView (Prompt 15 - Product Discovery & Fornecedores)', () => {
  it('should render the security & invariants banner and catalog tab', () => {
    render(<ProductDiscoveryView />);

    expect(
      screen.getByText(/Product Discovery Avançado & Catálogos de Fornecedores/i)
    ).toBeDefined();
    expect(
      screen.getByText(/Anti-Prompt Injection Ativo/i)
    ).toBeDefined();
    expect(
      screen.getByText(/Validação Humana Mandatória/i)
    ).toBeDefined();
    expect(
      screen.getByText(/Zero Ação Externa Automatizada/i)
    ).toBeDefined();

    expect(screen.getByText('catalogo_distribuidora_tech_2026.pdf')).toBeDefined();
    expect(screen.getByText('Distribuidora Tech Brasil Ltda')).toBeDefined();
  });

  it('should switch to Items sub-tab and filter items by review status', async () => {
    render(<ProductDiscoveryView />);

    const itemsTabBtn = screen.getByRole('button', { name: /Itens Extraídos & Revisão Humana/i });
    fireEvent.click(itemsTabBtn);

    expect(screen.getByText('Teclado Mecânico RGB Switch Blue ABNT2')).toBeDefined();
    expect(screen.getByText('Mouse Gamer 16000 DPI Sensor Óptico')).toBeDefined();

    // Filter by Pendentes
    const pendentesBtn = screen.getByRole('button', { name: 'Pendentes' });
    fireEvent.click(pendentesBtn);

    expect(screen.getByText('Mouse Gamer 16000 DPI Sensor Óptico')).toBeDefined();
    // Reviewed item should not be in filtered view
    expect(screen.queryByText('Teclado Mecânico RGB Switch Blue ABNT2')).toBeNull();
  });

  it('should open mandatory human review modal and allow confirming changes', async () => {
    render(<ProductDiscoveryView />);

    const itemsTabBtn = screen.getByRole('button', { name: /Itens Extraídos & Revisão Humana/i });
    fireEvent.click(itemsTabBtn);

    // Click "Conferir" on first item
    const conferirButtons = screen.getAllByRole('button', { name: /Conferir/i });
    fireEvent.click(conferirButtons[0]);

    expect(screen.getByText(/Conferência Humana Mandatória de Extração/i)).toBeDefined();

    // Confirm review
    const confirmBtn = screen.getByRole('button', { name: /Confirmar Revisão Humana/i });
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(screen.queryByText(/Conferência Humana Mandatória de Extração/i)).toBeNull();
    });
  });

  it('should switch to Similarity tab and allow confirming a candidate match', () => {
    render(<ProductDiscoveryView />);

    const similarityTabBtn = screen.getByRole('button', { name: /Similaridade & Margem Estimada/i });
    fireEvent.click(similarityTabBtn);

    expect(screen.getByText(/Correspondência com Produtos de Mercado & Margem Bruta Estimada/i)).toBeDefined();
    expect(screen.getByText('Teclado Gamer Mecânico RGB Switch Blue ABNT2 Original')).toBeDefined();

    // Find "Confirmar Correspondência" button for Candidate
    const confirmCandidateBtns = screen.getAllByRole('button', { name: /Confirmar Correspondência/i });
    fireEvent.click(confirmCandidateBtns[0]);

    expect(screen.getAllByText('Correspondência Confirmada pelo Usuário').length).toBe(2);
  });

  it('should switch to Visual Search sub-tab and trigger image search', async () => {
    render(<ProductDiscoveryView />);

    const visualTabBtn = screen.getByRole('button', { name: /Busca Visual por Imagem/i });
    fireEvent.click(visualTabBtn);

    expect(screen.getByText(/Busca Visual por Imagem de Produto/i)).toBeDefined();
    expect(screen.getByText('Mouse Gamer Sem Fio Recarregável RGB Ultra Leve')).toBeDefined();

    const searchBtn = screen.getByRole('button', { name: /Executar Busca por Imagem/i });
    fireEvent.click(searchBtn);

    await waitFor(() => {
      expect(screen.getByText('Produto Localizado por Reconhecimento Visual HD')).toBeDefined();
    });
  });
});
