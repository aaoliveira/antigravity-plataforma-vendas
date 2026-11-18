import React from 'react';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MarketSearchView } from './MarketSearchView';

const mockSearchResponse = {
  items: [
    {
      itemId: 'MLB-101',
      title: 'Monitor Dell 27 Polegadas 4K UltraHD',
      marketplace: 'MERCADO_LIVRE',
      price: 2499.0,
      currency: 'BRL',
      sellerName: 'Dell Loja Oficial',
      sellerClassification: 'Oficial Concorrente',
      listingUrl: 'https://produto.mercadolivre.com.br/MLB-101',
      gtin: '7891234567890',
      itemCondition: 'NEW',
      availableQuantity: null,
      sourcePolicyId: 'POL-MLB-PUBLIC-01',
      provenanceType: 'OBSERVED',
      collectedAt: '2026-11-18T10:00:00Z',
      salesMetricNotice: 'Métricas de vendas não fornecidas via API oficial (UNSUPPORTED) e não fabricadas.',
    },
    {
      itemId: 'INT-202',
      title: 'Monitor 27 Curvo 165Hz Meu Estoque',
      marketplace: 'INTERNAL',
      price: 1399.0,
      currency: 'BRL',
      sellerName: 'Catálogo Próprio',
      sellerClassification: 'Próprio',
      listingUrl: 'https://marketplace.internal/listings/INT-202',
      gtin: '7899876543210',
      itemCondition: 'NEW',
      availableQuantity: 45,
      sourcePolicyId: 'POL-OWN-CATALOG-01',
      provenanceType: 'OFFICIAL',
      collectedAt: '2026-11-18T11:30:00Z',
      salesMetricNotice: 'Anúncio próprio do tenant.',
    },
    {
      itemId: 'MLB-303',
      title: 'Monitor LG Ultrawide 29 Polegadas IPS',
      marketplace: 'MERCADO_LIVRE',
      price: 1199.9,
      currency: 'BRL',
      sellerName: 'Mega Informática',
      sellerClassification: 'MercadoLíder Platinum',
      listingUrl: 'https://produto.mercadolivre.com.br/MLB-303',
      gtin: '7891112223334',
      itemCondition: 'NEW',
      availableQuantity: null,
      sourcePolicyId: 'POL-MLB-PUBLIC-01',
      provenanceType: 'OBSERVED',
      collectedAt: '2026-11-18T09:15:00Z',
      salesMetricNotice: 'Métricas de vendas não fornecidas via API oficial.',
    },
  ],
  totalElements: 3,
  totalPages: 1,
  currentPage: 0,
  pageSize: 20,
  sourceUsed: 'MERCADO_LIVRE',
  sourcePolicyId: 'POL-MLB-PUBLIC-01',
  capabilityStatus: 'SUPPORTED',
  cached: false,
  capabilityNotice: 'Pesquisa pública autorizada realizada com sucesso. Dados sem fabricação de métricas.',
  rateLimitRemaining: 58,
};

describe('P010-FE: MarketSearchView Component', () => {
  beforeEach(() => {
    vi.resetAllMocks();
    global.fetch = vi.fn().mockImplementation((url: string) => {
      if (url.includes('/api/market-search')) {
        return Promise.resolve({
          ok: true,
          status: 200,
          json: () => Promise.resolve(mockSearchResponse),
        });
      }
      return Promise.reject(new Error('URL não mockada: ' + url));
    });
  });

  it('1. Renderiza formulário de busca e carrega resultados autorizados com sucesso', async () => {
    render(<MarketSearchView />);

    // Verifica campos de busca
    expect(screen.getByLabelText(/Termo de Busca \/ SKU \/ GTIN/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Canal de Mercado/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Apenas Meu Catálogo Próprio/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Executar pesquisa de mercado/i })).toBeInTheDocument();

    // Aguarda carregamento inicial
    await waitFor(() => {
      expect(screen.getByText('Monitor Dell 27 Polegadas 4K UltraHD')).toBeInTheDocument();
      expect(screen.getByText('Monitor 27 Curvo 165Hz Meu Estoque')).toBeInTheDocument();
      expect(screen.getByText('Monitor LG Ultrawide 29 Polegadas IPS')).toBeInTheDocument();
    });
  });

  it('2. Exibe metadados de proveniência, política governante e badges da fonte sempre visíveis', async () => {
    render(<MarketSearchView />);

    await waitFor(() => {
      expect(screen.getByText('Monitor Dell 27 Polegadas 4K UltraHD')).toBeInTheDocument();
    });

    // Banner de Metadados
    expect(screen.getAllByText('POL-MLB-PUBLIC-01').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('SUPPORTED')).toBeInTheDocument();
    expect(screen.getByText(/58 req/i)).toBeInTheDocument();

    // Badges de Proveniência
    expect(screen.getAllByText('OBSERVED').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('OFFICIAL').length).toBeGreaterThanOrEqual(1);

    // Links Oficiais com segurança target="_blank" e rel="noopener noreferrer"
    const linkDell = screen.getAllByRole('link', { name: /Canal ↗|Acessar no Canal Oficial ↗/i })[0];
    expect(linkDell).toHaveAttribute('target', '_blank');
    expect(linkDell).toHaveAttribute('rel', 'noopener noreferrer');
  });

  it('3. Diferencia anúncio próprio e concorrente com badges e permite filtrar por titularidade', async () => {
    render(<MarketSearchView />);

    await waitFor(() => {
      expect(screen.getByText('Monitor 27 Curvo 165Hz Meu Estoque')).toBeInTheDocument();
    });

    // Badges de titularidade
    expect(screen.getAllByText(/Próprio/i).length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText(/Concorrente/i).length).toBeGreaterThanOrEqual(1);

    // Filtra por "Apenas Anúncios Próprios"
    const ownFilterBtn = screen.getByRole('button', { name: /Anúncios Próprios/i });
    fireEvent.click(ownFilterBtn);

    expect(screen.getByText('Monitor 27 Curvo 165Hz Meu Estoque')).toBeInTheDocument();
    expect(screen.queryByText('Monitor Dell 27 Polegadas 4K UltraHD')).not.toBeInTheDocument();

    // Filtra por "Concorrentes"
    const competitorFilterBtn = screen.getByRole('button', { name: /Concorrentes/i });
    fireEvent.click(competitorFilterBtn);

    expect(screen.getByText('Monitor Dell 27 Polegadas 4K UltraHD')).toBeInTheDocument();
    expect(screen.queryByText('Monitor 27 Curvo 165Hz Meu Estoque')).not.toBeInTheDocument();
  });

  it('4. Exibe banner informativo Fail-Closed / Anti-Scraping para Shopee RESTRICTED e Amazon UNVERIFIED', async () => {
    (global.fetch as any).mockImplementationOnce((url: string) => {
      return Promise.resolve({
        ok: true,
        status: 200,
        json: () =>
          Promise.resolve({
            ...mockSearchResponse,
            sourceUsed: 'SHOPEE (OWN_CATALOG_FALLBACK)',
            sourcePolicyId: 'POL-SHOPEE-RESTRICTED-01',
            capabilityStatus: 'RESTRICTED',
            capabilityNotice:
              'Pesquisa concorrencial aberta não é disponibilizada via API pública da Shopee (RESTRICTED). Para evitar penalizações na conta do seller e infração dos Termos de Serviço, a plataforma não realiza web scraping.',
          }),
      });
    });

    render(<MarketSearchView />);

    await waitFor(() => {
      expect(screen.getByText('RESTRICTED')).toBeInTheDocument();
      expect(screen.getByText(/não realiza web scraping/i)).toBeInTheDocument();
    });
  });

  it('5. Trata erro HTTP 429 Too Many Requests com tempo de espera de recomposição', async () => {
    (global.fetch as any).mockImplementationOnce((url: string) => {
      return Promise.resolve({
        ok: false,
        status: 429,
        headers: new Headers({ 'Retry-After': '45' }),
        json: () =>
          Promise.resolve({
            detail: 'Limite de taxa de pesquisa excedido pela API oficial.',
            retryAfter: 45,
          }),
      });
    });

    render(<MarketSearchView />);

    await waitFor(() => {
      expect(screen.getByText(/Limite de Taxa Excedido \(HTTP 429\)/i)).toBeInTheDocument();
      expect(screen.getByText(/45 segundos/i)).toBeInTheDocument();
    });
  });

  it('6. Ordena resultados por Menor Preço e Maior Preço', async () => {
    render(<MarketSearchView />);

    await waitFor(() => {
      expect(screen.getByText('Monitor Dell 27 Polegadas 4K UltraHD')).toBeInTheDocument();
    });

    const sortSelect = screen.getByLabelText(/Ordenar por/i);

    // Ordenar por menor preço
    fireEvent.change(sortSelect, { target: { value: 'price_asc' } });

    // R$ 1.199,90 deve ser o primeiro da lista
    const tableRows = screen.getAllByRole('row');
    // Header é row 0; primeira linha de dados é row 1
    expect(tableRows[1]).toHaveTextContent('Monitor LG Ultrawide 29 Polegadas IPS');

    // Ordenar por maior preço
    fireEvent.change(sortSelect, { target: { value: 'price_desc' } });
    const tableRowsDesc = screen.getAllByRole('row');
    expect(tableRowsDesc[1]).toHaveTextContent('Monitor Dell 27 Polegadas 4K UltraHD');
  });

  it('7. Permite selecionar múltiplos produtos para comparação lado a lado e abrir modal comparativo', async () => {
    render(<MarketSearchView />);

    await waitFor(() => {
      expect(screen.getByText('Monitor Dell 27 Polegadas 4K UltraHD')).toBeInTheDocument();
    });

    // Botão de comparar inicialmente desabilitado (0 selecionados)
    const compareBtn = screen.getByRole('button', { name: /Comparar Selecionados/i });
    expect(compareBtn).toBeDisabled();

    // Seleciona o 1º e 2º produto via checkboxes
    const checkboxes = screen.getAllByLabelText(/Selecionar .* para comparação/i);
    fireEvent.click(checkboxes[0]);
    fireEvent.click(checkboxes[1]);

    // Botão agora está habilitado com 2 selecionados
    expect(compareBtn).not.toBeDisabled();
    fireEvent.click(compareBtn);

    // Modal de comparação deve abrir com os dois produtos lado a lado
    expect(screen.getByRole('dialog', { name: /Comparação de Anúncios Lado a Lado/i })).toBeInTheDocument();
    expect(screen.getAllByText('Monitor Dell 27 Polegadas 4K UltraHD').length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText('Monitor 27 Curvo 165Hz Meu Estoque').length).toBeGreaterThanOrEqual(1);

    // Fecha o modal via botão de fechar
    const closeBtns = screen.getAllByRole('button', { name: /Fechar comparação/i });
    fireEvent.click(closeBtns[0]);

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('8. Garante responsividade mobile com suporte a visualização em cards e touch targets >= 44px', async () => {
    render(<MarketSearchView />);

    await waitFor(() => {
      expect(screen.getByText('Monitor Dell 27 Polegadas 4K UltraHD')).toBeInTheDocument();
    });

    // Alterna para visualização em cards
    const cardsViewBtn = screen.getByRole('button', { name: /Visualização em cards/i });
    fireEvent.click(cardsViewBtn);

    // Cards contêm os títulos e links
    expect(screen.getAllByText('Monitor Dell 27 Polegadas 4K UltraHD').length).toBeGreaterThanOrEqual(1);

    // Valida altura mínima de touch target no botão de busca
    const searchBtn = screen.getByRole('button', { name: /Executar pesquisa de mercado/i });
    expect(searchBtn.className).toContain('min-h-[44px]');
  });
});
