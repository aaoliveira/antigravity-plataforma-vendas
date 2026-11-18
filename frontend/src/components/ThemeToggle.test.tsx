import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, beforeEach } from 'vitest';
import { ThemeToggle } from './ThemeToggle';

describe('ThemeToggle Component (RF-051: Light Mode Padrão & Dark Mode Opcional)', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
  });

  it('deve inicializar em Light Mode por padrão sem classe dark no html', () => {
    render(<ThemeToggle />);
    expect(document.documentElement.classList.contains('dark')).toBe(false);
    expect(screen.getByRole('button', { name: /Alternar para modo escuro/i })).toBeDefined();
  });

  it('deve alternar para Dark Mode ao clicar, adicionar classe dark e persistir no localStorage', () => {
    render(<ThemeToggle />);
    const button = screen.getByRole('button', { name: /Alternar para modo escuro/i });

    fireEvent.click(button);

    expect(document.documentElement.classList.contains('dark')).toBe(true);
    expect(localStorage.getItem('seller_theme')).toBe('dark');
    expect(screen.getByRole('button', { name: /Alternar para modo claro/i })).toBeDefined();
  });

  it('deve alternar de volta para Light Mode ao segundo clique', () => {
    render(<ThemeToggle />);
    const button = screen.getByRole('button');

    // Light -> Dark
    fireEvent.click(button);
    expect(document.documentElement.classList.contains('dark')).toBe(true);

    // Dark -> Light
    fireEvent.click(button);
    expect(document.documentElement.classList.contains('dark')).toBe(false);
    expect(localStorage.getItem('seller_theme')).toBe('light');
  });

  it('deve carregar a preferência salva no localStorage na montagem', () => {
    localStorage.setItem('seller_theme', 'dark');
    render(<ThemeToggle />);
    expect(document.documentElement.classList.contains('dark')).toBe(true);
    expect(screen.getByRole('button', { name: /Alternar para modo claro/i })).toBeDefined();
  });
});
