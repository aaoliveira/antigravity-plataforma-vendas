import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { TenantSwitcher } from './TenantSwitcher';

const mockSwitchTenant = vi.fn();
let mockUser: any = null;
let mockCurrentTenant: any = null;

vi.mock('@/context/AuthContext', () => ({
  useAuth: () => ({
    user: mockUser,
    currentTenant: mockCurrentTenant,
    switchTenant: mockSwitchTenant,
  }),
}));

describe('TenantSwitcher Component (Isolamento e Troca de Contexto de Tenant)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockCurrentTenant = {
      tenantId: '11111111-1111-1111-1111-111111111111',
      name: 'Alpha Seller Org',
      slug: 'alpha-seller',
      role: 'OWNER',
      status: 'ACTIVE',
    };
    mockUser = {
      userId: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
      email: 'seller@example.com',
      fullName: 'Alpha Seller Admin',
      status: 'ACTIVE',
      memberships: [
        {
          tenantId: '11111111-1111-1111-1111-111111111111',
          tenantName: 'Alpha Seller Org',
          role: 'OWNER',
          status: 'ACTIVE',
        },
        {
          tenantId: '33333333-3333-3333-3333-333333333333',
          tenantName: 'Secondary Store Ltda',
          role: 'ADMIN',
          status: 'ACTIVE',
        },
      ],
    };
  });

  it('deve exibir o nome da organização ativa e o papel do usuário no cabeçalho', () => {
    render(<TenantSwitcher />);

    expect(screen.getByText('Alpha Seller Org')).toBeDefined();
    expect(screen.getByText('OWNER')).toBeDefined();
  });

  it('deve abrir o seletor exibindo EXCLUSIVAMENTE as organizações do próprio usuário (zero vazamento de terceiros)', () => {
    render(<TenantSwitcher />);

    const triggerBtn = screen.getByRole('button', { name: /Organização ativa: Alpha Seller Org/i });
    fireEvent.click(triggerBtn);

    // Lista de opções
    expect(screen.getByRole('listbox')).toBeDefined();
    expect(screen.getByText(/Suas Organizações \(2\)/i)).toBeDefined();
    expect(screen.getAllByText('Alpha Seller Org').length).toBeGreaterThan(0);
    expect(screen.getByText('Secondary Store Ltda')).toBeDefined();

    // Organizações de terceiros não devem existir sob hipótese alguma
    expect(screen.queryByText('Beta Store Org')).toBeNull();
    expect(screen.queryByText('Concorrente')).toBeNull();
  });

  it('deve disparar switchTenant ao selecionar outra organização da lista', async () => {
    mockSwitchTenant.mockResolvedValueOnce(undefined);
    render(<TenantSwitcher />);

    const triggerBtn = screen.getByRole('button', { name: /Organização ativa: Alpha Seller Org/i });
    fireEvent.click(triggerBtn);

    const secondaryOption = screen.getByRole('option', { name: /Secondary Store Ltda/i });
    fireEvent.click(secondaryOption);

    await waitFor(() => {
      expect(mockSwitchTenant).toHaveBeenCalledWith('33333333-3333-3333-3333-333333333333');
    });
  });

  it('deve fechar o dropdown ao pressionar a tecla Escape', () => {
    render(<TenantSwitcher />);

    const triggerBtn = screen.getByRole('button', { name: /Organização ativa: Alpha Seller Org/i });
    fireEvent.click(triggerBtn);

    expect(screen.getByRole('listbox')).toBeDefined();

    fireEvent.keyDown(document, { key: 'Escape' });

    expect(screen.queryByRole('listbox')).toBeNull();
  });

  it('não deve exibir nenhum tenant ID técnico (UUID) no texto visível ao usuário', () => {
    const { container } = render(<TenantSwitcher />);

    const triggerBtn = screen.getByRole('button', { name: /Organização ativa: Alpha Seller Org/i });
    fireEvent.click(triggerBtn);

    // Valida que o texto do componente não contém o UUID 11111111-1111-1111-1111-111111111111 nem 33333333-3333-3333-3333-333333333333
    const textContent = container.textContent || '';
    expect(textContent).not.toContain('11111111-1111-1111-1111-111111111111');
    expect(textContent).not.toContain('33333333-3333-3333-3333-333333333333');

    const uuidRegex = /[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/;
    expect(uuidRegex.test(textContent)).toBe(false);
  });
});
