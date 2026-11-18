import React from 'react';
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import { AppShell } from './AppShell';

describe('ResponsiveLayout Component (RF-050 & RNF-010: Responsividade & Zero Overflow)', () => {
  const breakpoints = [
    { name: 'Mobile Pequeno (360px)', width: 360 },
    { name: 'Tablet (768px)', width: 768 },
    { name: 'Desktop Padrão (1024px)', width: 1024 },
    { name: 'Desktop Ultrawide (1440px)', width: 1440 },
  ];

  breakpoints.forEach(({ name, width }) => {
    it(`deve renderizar o layout com contenção de largura no breakpoint ${name}`, () => {
      window.innerWidth = width;
      const { container } = render(<AppShell />);

      // Verifica se o container raiz possui as classes de contenção horizontal
      const rootDiv = container.firstElementChild as HTMLElement;
      expect(rootDiv.className).toContain('w-full');
      expect(rootDiv.className).toContain('max-w-full');
      expect(rootDiv.className).toContain('overflow-x-hidden');

      // Verifica elementos de navegação essenciais
      expect(screen.getByText('Seller Intelligence Platform')).toBeDefined();
      expect(screen.getByRole('navigation', { name: /Navegação rápida móvel/i })).toBeDefined();
    });
  });

  it('deve disponibilizar botões com alvos de toque acessíveis no dock mobile (>= 48px)', () => {
    render(<AppShell />);
    const dockNav = screen.getByRole('navigation', { name: /Navegação rápida móvel/i });
    const buttons = dockNav.querySelectorAll('button');

    expect(buttons.length).toBeGreaterThanOrEqual(4);
    buttons.forEach((btn) => {
      expect(btn.className).toContain('min-h-[48px]');
      expect(btn.className).toContain('min-w-[56px]');
    });
  });
});
