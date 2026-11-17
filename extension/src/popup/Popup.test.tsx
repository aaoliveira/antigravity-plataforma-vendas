import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Popup } from './Popup';

describe('Extension Popup Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders header, online connection status and privacy guarantee notice', () => {
    render(<Popup />);

    expect(screen.getByText('Seller Intelligence')).toBeDefined();
    expect(screen.getByText('Assistente Contextual v1.0.0')).toBeDefined();
    expect(screen.getByText('Online')).toBeDefined();
    expect(screen.getByText(/Garantia de Privacidade:/i)).toBeDefined();
    expect(screen.getByText(/Esta extensão utiliza apenas a permissão/i)).toBeDefined();
  });

  it('renders parsed product card with action buttons when active tab is a supported product', () => {
    render(<Popup />);

    expect(screen.getByText('Mercado Livre')).toBeDefined();
    expect(screen.getByText('MLB2000984711')).toBeDefined();
    expect(screen.getByText('Teclado Mecânico Gamer RGB Switch Blue')).toBeDefined();
    expect(screen.getByRole('button', { name: /Otimizar no AI Listing Studio/i })).toBeDefined();
    expect(screen.getByRole('button', { name: /Pesquisar Concorrência/i })).toBeDefined();
  });

  it('triggers action to open AI Listing Studio and displays feedback message', () => {
    // Mock window.open
    const originalOpen = window.open;
    window.open = vi.fn();

    render(<Popup />);

    const studioBtn = screen.getByRole('button', { name: /Otimizar no AI Listing Studio/i });
    fireEvent.click(studioBtn);

    expect(screen.getByText(/Abrindo anúncio no AI Listing Studio.../i)).toBeDefined();
    expect(window.open).toHaveBeenCalledWith(
      expect.stringContaining('/?tab=AI_STUDIO&externalId=MLB2000984711'),
      '_blank'
    );

    window.open = originalOpen;
  });

  it('triggers action to open Market Intelligence and displays feedback message', () => {
    const originalOpen = window.open;
    window.open = vi.fn();

    render(<Popup />);

    const marketBtn = screen.getByRole('button', { name: /Pesquisar Concorrência/i });
    fireEvent.click(marketBtn);

    expect(screen.getByText(/Abrindo pesquisa de concorrência na plataforma.../i)).toBeDefined();
    expect(window.open).toHaveBeenCalledWith(
      expect.stringContaining('/?tab=MARKET&query='),
      '_blank'
    );

    window.open = originalOpen;
  });
});
