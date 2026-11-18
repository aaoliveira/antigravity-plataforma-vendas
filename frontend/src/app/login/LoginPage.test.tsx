import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import LoginPage from './page';

// Mock do useRouter e useSearchParams do Next.js
const mockPush = vi.fn();
vi.mock('next/navigation', () => ({
  useRouter: () => ({
    push: mockPush,
  }),
  useSearchParams: () => ({
    get: (key: string) => (key === 'expired' ? null : null),
  }),
}));

// Mock do AuthContext
const mockLogin = vi.fn();
let mockIsAuthenticated = false;
let mockSessionExpired = false;

vi.mock('@/context/AuthContext', () => ({
  useAuth: () => ({
    login: mockLogin,
    isAuthenticated: mockIsAuthenticated,
    sessionExpired: mockSessionExpired,
  }),
}));

describe('LoginPage Component (Acessibilidade & Autenticação)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockIsAuthenticated = false;
    mockSessionExpired = false;
    localStorage.clear();
  });

  it('deve renderizar campos de e-mail e senha acessíveis com labels e aria-required', () => {
    render(<LoginPage />);

    expect(screen.getByRole('heading', { name: /Acesso à Plataforma/i })).toBeDefined();

    const emailInput = screen.getByLabelText(/E-mail Corporativo/i);
    expect(emailInput).toBeDefined();
    expect(emailInput.getAttribute('type')).toBe('email');
    expect(emailInput.getAttribute('aria-required')).toBe('true');
    expect(emailInput.getAttribute('autocomplete')).toBe('email');

    const passwordInput = screen.getByLabelText(/Senha/i);
    expect(passwordInput).toBeDefined();
    expect(passwordInput.getAttribute('type')).toBe('password');
    expect(passwordInput.getAttribute('aria-required')).toBe('true');
    expect(passwordInput.getAttribute('autocomplete')).toBe('current-password');

    const submitBtn = screen.getByRole('button', { name: /Entrar na Plataforma/i });
    expect(submitBtn).toBeDefined();
  });

  it('deve submeter credenciais e redirecionar para a raiz em caso de sucesso', async () => {
    mockLogin.mockResolvedValueOnce(undefined);
    render(<LoginPage />);

    const emailInput = screen.getByLabelText(/E-mail Corporativo/i);
    const passwordInput = screen.getByLabelText(/Senha/i);
    const submitBtn = screen.getByRole('button', { name: /Entrar na Plataforma/i });

    fireEvent.change(emailInput, { target: { value: 'seller@example.com' } });
    fireEvent.change(passwordInput, { target: { value: 'Password@123' } });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith('seller@example.com', 'Password@123');
      expect(mockPush).toHaveBeenCalledWith('/');
    });

    // Zero tokens sensíveis em localStorage
    expect(localStorage.getItem('token')).toBeNull();
    expect(localStorage.getItem('access_token')).toBeNull();
    expect(localStorage.getItem('seller_access_token')).toBeNull();
  });

  it('deve exibir mensagem de alerta acessível (role="alert") ao ocorrer falha de autenticação', async () => {
    mockLogin.mockRejectedValueOnce(new Error('Credenciais inválidas. Verifique seu e-mail e senha.'));
    render(<LoginPage />);

    const emailInput = screen.getByLabelText(/E-mail Corporativo/i);
    const passwordInput = screen.getByLabelText(/Senha/i);
    const submitBtn = screen.getByRole('button', { name: /Entrar na Plataforma/i });

    fireEvent.change(emailInput, { target: { value: 'seller@example.com' } });
    fireEvent.change(passwordInput, { target: { value: 'SenhaErrada' } });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      const alert = screen.getByRole('alert');
      expect(alert).toBeDefined();
      expect(alert.textContent).toContain('Credenciais inválidas');
    });
  });

  it('deve preencher campos ao acionar os atalhos de contas de demonstração (seed)', () => {
    render(<LoginPage />);

    const alphaBtn = screen.getByRole('button', { name: /Alpha Seller/i });
    fireEvent.click(alphaBtn);

    const emailInput = screen.getByLabelText(/E-mail Corporativo/i) as HTMLInputElement;
    const passwordInput = screen.getByLabelText(/Senha/i) as HTMLInputElement;

    expect(emailInput.value).toBe('seller@example.com');
    expect(passwordInput.value).toBe('Password@123');
  });
});
