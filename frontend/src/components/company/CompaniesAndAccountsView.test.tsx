import React from 'react';
import { render, screen, fireEvent, waitFor, within } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { CompaniesAndAccountsView } from './CompaniesAndAccountsView';
import { CompanyDto, CompanyAccountLinkDto, SellerAccountDto } from '@/types/company';

let mockCurrentTenant: { role: string } | null = { role: 'ADMIN' };

vi.mock('@/context/AuthContext', () => ({
  useAuth: () => ({
    currentTenant: mockCurrentTenant,
  }),
}));

const mockCompanies: CompanyDto[] = [
  {
    id: 'comp-alpha',
    tenantId: '11111111-1111-1111-1111-111111111111',
    corporateName: 'Alpha Comércio e Logística LTDA',
    tradeName: 'Alpha Express',
    cnpj: '33000167000101',
    formattedCnpj: '33.000.167/0001-01',
    stateRegistration: '123.456.789.000',
    taxRegime: 'SIMPLES_NACIONAL',
    isActive: true,
    notes: 'Matriz operacional',
    createdAt: '2026-11-19T10:00:00Z',
    updatedAt: '2026-11-19T10:00:00Z',
  },
  {
    id: 'comp-beta',
    tenantId: '11111111-1111-1111-1111-111111111111',
    corporateName: 'Beta Distribuição e Varejo LTDA',
    tradeName: 'Beta Varejo',
    cnpj: '60701190000104',
    formattedCnpj: '60.701.190/0001-04',
    stateRegistration: '987.654.321.000',
    taxRegime: 'LUCRO_PRESUMIDO',
    isActive: true,
    notes: 'Filial Nordeste',
    createdAt: '2026-11-19T10:00:00Z',
    updatedAt: '2026-11-19T10:00:00Z',
  },
];

const mockSellerAccounts: SellerAccountDto[] = [
  {
    id: 'acc-meli',
    marketplace: 'MERCADO_LIVRE',
    externalAccountId: 'MLB_OFFICIAL_100',
    accountName: 'Loja Matriz Mercado Livre',
    status: 'ACTIVE',
  },
  {
    id: 'acc-shopee',
    marketplace: 'SHOPEE',
    externalAccountId: 'SHP_STORE_200',
    accountName: 'Loja Shopee Oficial',
    status: 'ACTIVE',
  },
];

const mockLinksForAlpha: CompanyAccountLinkDto[] = [
  {
    id: 'link-1',
    tenantId: '11111111-1111-1111-1111-111111111111',
    companyId: 'comp-alpha',
    companyName: 'Alpha Comércio e Logística LTDA',
    sellerAccountId: 'acc-meli',
    marketplace: 'MERCADO_LIVRE',
    accountName: 'Loja Matriz Mercado Livre',
    effectiveFrom: '2026-11-01T10:00:00Z',
    effectiveTo: null,
    isActive: true,
    version: 1,
    notes: 'Vínculo inicial',
    linkedBy: 'admin@alpha.com',
    createdAt: '2026-11-01T10:00:00Z',
    updatedAt: '2026-11-01T10:00:00Z',
  },
];

describe('CompaniesAndAccountsView Component (P016-FE)', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockCurrentTenant = { role: 'ADMIN' };

    // Setup fetch mock default
    global.fetch = vi.fn().mockImplementation((url: string, init?: RequestInit) => {
      const urlStr = url.toString();

      if (urlStr.includes('/api/companies?')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: mockCompanies, totalElements: 2 }),
        });
      }

      if (urlStr.includes('/api/catalog/seller-accounts')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockSellerAccounts),
        });
      }

      if (urlStr.includes('/api/companies/comp-alpha/links')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockLinksForAlpha),
        });
      }

      if (urlStr.includes('/api/companies/comp-beta/links')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve([]),
        });
      }

      if (urlStr.includes('/api/companies/accounts/acc-meli/history')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockLinksForAlpha),
        });
      }

      if (urlStr.includes('/api/companies') && init?.method === 'POST') {
        const body = JSON.parse(init.body as string);
        return Promise.resolve({
          ok: true,
          status: 201,
          json: () =>
            Promise.resolve({
              id: 'comp-new',
              corporateName: body.corporateName,
              cnpj: body.cnpj,
              formattedCnpj: '00.000.000/0001-91',
              isActive: true,
            }),
        });
      }

      if (urlStr.includes('/links') && init?.method === 'POST') {
        return Promise.resolve({
          ok: true,
          status: 201,
          json: () => Promise.resolve({ id: 'link-new', isActive: true, version: 2 }),
        });
      }

      if (urlStr.includes('/links/') && init?.method === 'DELETE') {
        return Promise.resolve({
          ok: true,
          status: 204,
        });
      }

      if (urlStr.includes('/api/companies/') && init?.method === 'DELETE') {
        return Promise.resolve({
          ok: true,
          status: 204,
        });
      }

      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({}),
      });
    }) as any;
  });

  it('1. Renderiza o cabeçalho do módulo com título, badges de governança e abas', async () => {
    render(<CompaniesAndAccountsView />);

    expect(await screen.findByText('Empresas & Unidades de Negócio')).toBeInTheDocument();
    expect(screen.getByText('Isolamento Multi-Tenant')).toBeInTheDocument();
    expect(screen.getByText('CNPJ Módulo 11')).toBeInTheDocument();
    expect(screen.getByText('LGPD & Proteção de CNPJ')).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /Empresas Cadastradas/i })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: /Vínculos de Contas de Marketplace/i })).toBeInTheDocument();
  });

  it('2. Proteção de Privacidade de CNPJ: exibe CNPJ ofuscado por padrão e revela sob demanda', async () => {
    render(<CompaniesAndAccountsView />);

    // Por padrão na listagem, o CNPJ da Alpha deve estar ofuscado como 33.***.***/0001-01
    expect(await screen.findByText('33.***.***/0001-01')).toBeInTheDocument();
    expect(screen.queryByText('33.000.167/0001-01')).not.toBeInTheDocument();

    // Clica no botão com aria-label de revelar CNPJ da empresa Alpha
    const revealBtn = screen.getByRole('button', { name: /Revelar CNPJ de Alpha Comércio e Logística LTDA/i });
    fireEvent.click(revealBtn);

    // Agora o CNPJ completo deve estar visível
    expect(screen.getByText('33.000.167/0001-01')).toBeInTheDocument();
    expect(screen.queryByText('33.***.***/0001-01')).not.toBeInTheDocument();

    // Clica novamente para ocultar
    const hideBtn = screen.getByRole('button', { name: /Ocultar CNPJ de Alpha Comércio e Logística LTDA/i });
    fireEvent.click(hideBtn);

    expect(screen.getByText('33.***.***/0001-01')).toBeInTheDocument();
  });

  it('3. Validação e Máscara em tempo real no Modal de Nova Empresa: bloqueia CNPJ inválido', async () => {
    render(<CompaniesAndAccountsView />);

    const newCompanyBtn = await screen.findByRole('button', { name: /Nova Empresa/i });
    fireEvent.click(newCompanyBtn);

    // Modal aberto
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Cadastrar Nova Empresa' })).toBeInTheDocument();

    const corpInput = screen.getByLabelText(/Razão Social/i);
    const cnpjInput = screen.getByPlaceholderText('00.000.000/0000-00');
    const submitBtn = screen.getByRole('button', { name: 'Cadastrar Empresa' });

    // Digita Razão Social
    fireEvent.change(corpInput, { target: { value: 'Comercial Nova LTDA' } });

    // Digita CNPJ com dígitos repetidos (11.111.111/1111-11)
    fireEvent.change(cnpjInput, { target: { value: '11111111111111' } });
    fireEvent.blur(cnpjInput);

    // Deve aplicar a máscara automaticamente no campo
    expect(cnpjInput).toHaveValue('11.111.111/1111-11');

    // Deve exibir aviso de validação sintática e desabilitar botão
    expect(screen.getByText(/Dígitos verificadores incorretos/i)).toBeInTheDocument();
    expect(screen.getByRole('alert')).toHaveTextContent(/CNPJ sintaticamente inválido/i);
    expect(submitBtn).toBeDisabled();

    // Agora digita um CNPJ válido: Banco do Brasil 00.000.000/0001-91
    fireEvent.change(cnpjInput, { target: { value: '00000000000191' } });
    expect(cnpjInput).toHaveValue('00.000.000/0001-91');
    expect(screen.getByText(/✓ CNPJ sintaticamente válido/i)).toBeInTheDocument();
    expect(submitBtn).not.toBeDisabled();
  });

  it('4. Cadastra nova empresa com sucesso via POST /api/companies', async () => {
    render(<CompaniesAndAccountsView />);

    const newCompanyBtn = await screen.findByRole('button', { name: /Nova Empresa/i });
    fireEvent.click(newCompanyBtn);

    const corpInput = screen.getByLabelText(/Razão Social/i);
    const cnpjInput = screen.getByPlaceholderText('00.000.000/0000-00');

    fireEvent.change(corpInput, { target: { value: 'Empresa Teste Aprovada LTDA' } });
    fireEvent.change(cnpjInput, { target: { value: '00000000000191' } });

    const submitBtn = screen.getByRole('button', { name: 'Cadastrar Empresa' });
    fireEvent.click(submitBtn);

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        '/api/companies',
        expect.objectContaining({
          method: 'POST',
          body: expect.stringContaining('"cnpj":"00000000000191"'),
        })
      );
    });

    expect(await screen.findByText('Empresa cadastrada com sucesso.')).toBeInTheDocument();
  });

  it('5. Vínculo Explícito: detecta que conta pertence a outra empresa e exige confirmação de transferência', async () => {
    render(<CompaniesAndAccountsView />);

    // Alterna para aba de Vínculos
    const linksTab = await screen.findByRole('tab', { name: /Vínculos de Contas de Marketplace/i });
    fireEvent.click(linksTab);

    // Abre modal de vincular
    const linkBtn = await screen.findByRole('button', { name: /Vincular Nova Conta/i });
    fireEvent.click(linkBtn);

    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Vincular Conta de Marketplace' })).toBeInTheDocument();

    const companySelect = screen.getByLabelText(/Empresa Receptora/i);
    const accountSelect = screen.getByLabelText(/Conta de Marketplace Conectada/i);

    // Seleciona a Empresa Beta como destino
    fireEvent.change(companySelect, { target: { value: 'comp-beta' } });

    // Seleciona a conta "Loja Matriz Mercado Livre", que já pertence à Empresa Alpha
    fireEvent.change(accountSelect, { target: { value: 'acc-meli' } });

    // Deve exibir aviso ostensivo de transferência
    expect(await screen.findByText(/Aviso de Transferência de Titularidade/i)).toBeInTheDocument();
    expect(screen.getByText(/já está vinculada ativamente à empresa/i)).toBeInTheDocument();

    // Botão de submissão deve estar desabilitado até confirmar checkbox
    const confirmBtn = screen.getByRole('button', { name: /Confirmar Transferência/i });
    expect(confirmBtn).toBeDisabled();

    // Marca o checkbox de confirmação
    const checkbox = screen.getByLabelText(/Confirmo a transferência desta conta/i);
    fireEvent.click(checkbox);
    expect(confirmBtn).not.toBeDisabled();

    // Submete a transferência
    fireEvent.click(confirmBtn);

    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        '/api/companies/comp-beta/links',
        expect.objectContaining({
          method: 'POST',
          body: expect.stringContaining('"replaceActiveLink":true'),
        })
      );
    });

    expect(await screen.findByText('Conta vinculada à empresa com sucesso.')).toBeInTheDocument();
  });

  it('6. Permissões Aplicadas (RBAC): perfil VIEWER desabilita ações de escrita e exibe banner informativo', async () => {
    mockCurrentTenant = { role: 'VIEWER' };

    render(<CompaniesAndAccountsView />);

    expect(await screen.findByText(/Modo Somente Leitura/i)).toBeInTheDocument();
    expect(screen.getByText(/Seu perfil atual possui privilégios de visualização/i)).toBeInTheDocument();

    // Botão Nova Empresa desabilitado
    const newCompanyBtn = screen.getByRole('button', { name: /Nova Empresa/i });
    expect(newCompanyBtn).toBeDisabled();
    expect(newCompanyBtn).toHaveAttribute('aria-disabled', 'true');

    // Botões de editar das linhas desabilitados
    const editBtns = screen.getAllByLabelText(/Editar/i);
    expect(editBtns[0]).toBeDisabled();
  });

  it('7. Abre a gaveta de histórico de vigência da conta', async () => {
    render(<CompaniesAndAccountsView />);

    // Alterna para aba de Vínculos
    const linksTab = await screen.findByRole('tab', { name: /Vínculos de Contas de Marketplace/i });
    fireEvent.click(linksTab);

    // Clica no botão Histórico da primeira conta
    const historyBtns = await screen.findAllByRole('button', { name: /Histórico/i });
    fireEvent.click(historyBtns[0]);

    // Drawer de histórico abre
    const drawer = await screen.findByRole('dialog');
    expect(within(drawer).getByRole('heading', { name: 'Histórico de Vínculos & Vigência' })).toBeInTheDocument();
    expect(within(drawer).getByText(/Loja Matriz Mercado Livre/i)).toBeInTheDocument();
    expect(within(drawer).getByText('v1')).toBeInTheDocument();
    expect(within(drawer).getByText('Vigente')).toBeInTheDocument();

    // Fecha via botão fechar
    const closeBtn = within(drawer).getByRole('button', { name: 'Fechar' });
    fireEvent.click(closeBtn);
    expect(screen.queryByRole('heading', { name: 'Histórico de Vínculos & Vigência' })).not.toBeInTheDocument();
  });

  it('8. Conformidade Mobile: botões de ação e interativos mantêm altura mínima de 44px (touch target WCAG)', async () => {
    render(<CompaniesAndAccountsView />);

    const newCompanyBtn = await screen.findByRole('button', { name: /Nova Empresa/i });
    expect(newCompanyBtn.className).toContain('min-h-[44px]');

    const tabs = screen.getAllByRole('tab');
    for (const tab of tabs) {
      expect(tab.className).toContain('min-h-[44px]');
    }
  });
});
