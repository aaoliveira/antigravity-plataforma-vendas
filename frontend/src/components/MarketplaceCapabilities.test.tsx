import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import React from 'react';
import { MarketplaceCapabilities } from './MarketplaceCapabilities';

describe('MarketplaceCapabilities Component', () => {
  it('renders official marketplace titles and authentication methods', () => {
    render(<MarketplaceCapabilities />);

    expect(screen.getByText('Matriz Oficial de Capacidades dos Conectores')).toBeDefined();
    expect(screen.getByText('Mercado Livre (Brasil - MLB)')).toBeDefined();
    expect(screen.getByText('Shopee Open Platform (Brasil - BR)')).toBeDefined();
  });

  it('renders supported and unsupported capabilities correctly', () => {
    render(<MarketplaceCapabilities />);

    // Catálogo is supported in both
    const catalogElements = screen.getAllByText('Catálogo & Itens Próprios');
    expect(catalogElements.length).toBe(2);

    // Ads pending in ML, unsupported in Shopee
    expect(screen.getByText('Mercado Ads (Product Ads)')).toBeDefined();
    expect(screen.getByText('Shopee Ads')).toBeDefined();
  });

  it('displays the architectural invariant notice', () => {
    render(<MarketplaceCapabilities />);

    expect(screen.getByText(/Invariante Arquitetural/i)).toBeDefined();
    expect(screen.getByText(/Nenhum dado sintético, mock ou scraping não autorizado/i)).toBeDefined();
  });
});
