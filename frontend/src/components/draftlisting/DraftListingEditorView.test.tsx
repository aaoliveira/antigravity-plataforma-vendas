import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { DraftListingEditorView } from './DraftListingEditorView';
import {
  DraftListingSummaryDto,
  DraftListingDetailDto,
  MarketplaceRuleLimitsDto,
} from '@/types/draftlisting';

const mockRules: MarketplaceRuleLimitsDto[] = [
  {
    marketplace: 'MERCADO_LIVRE',
    minTitleLength: 1,
    maxTitleLength: 60,
    minDescriptionLength: 1,
    maxDescriptionLength: 25000,
    plainTextOnly: true,
    officialRuleSource: 'Mercado Livre Developers Items API (title max 60 chars)',
  },
  {
    marketplace: 'SHOPEE',
    minTitleLength: 20,
    maxTitleLength: 120,
    minDescriptionLength: 20,
    maxDescriptionLength: 3000,
    plainTextOnly: true,
    officialRuleSource: 'Shopee Open Platform OpenAPI v2 (title 20-120 chars)',
  },
];

const mockDraftSummary: DraftListingSummaryDto = {
  id: 'draft-uuid-001',
  tenantId: 'tenant-uuid-001',
  productId: 'product-uuid-001',
  productVariantId: null,
  targetMarketplace: 'MERCADO_LIVRE',
  currentVersionNumber: 1,
  status: 'DRAFT',
  publicationStatus: 'NOT_PUBLISHED_DRAFT_ONLY',
  externalPublishBlocked: true,
  createdAt: '2026-11-19T14:00:00Z',
  updatedAt: '2026-11-19T14:00:00Z',
};

const mockDraftDetail: DraftListingDetailDto = {
  id: 'draft-uuid-001',
  tenantId: 'tenant-uuid-001',
  productId: 'product-uuid-001',
  productVariantId: null,
  targetMarketplace: 'MERCADO_LIVRE',
  currentVersionNumber: 1,
  status: 'DRAFT',
  publicationStatus: 'NOT_PUBLISHED_DRAFT_ONLY',
  externalPublishBlocked: true,
  currentVersion: {
    id: 'ver-uuid-001',
    draftListingId: 'draft-uuid-001',
    versionNumber: 1,
    promptTemplateId: 'LISTING_DRAFT_GENERATION',
    promptTemplateVersion: 'v1.0.0',
    aiModelName: 'mock-deterministic-v1',
    aiProvider: 'MOCK_PROVIDER',
    tone: 'INFORMATIVE_OBJECTIVE',
    targetMarketplace: 'MERCADO_LIVRE',
    inputAttributesJson: '{"brand":"Tramontina","model":"Solar"}',
    generatedTitle: 'Panela Tramontina Solar Aço Inox 20cm',
    generatedDescription: 'Panela Tramontina Solar em Aço Inox original de fábrica.',
    reviewedTitle: null,
    reviewedDescription: null,
    effectiveTitle: 'Panela Tramontina Solar Aço Inox 20cm',
    effectiveDescription: 'Panela Tramontina Solar em Aço Inox original de fábrica.',
    traceableAttributesJson: '{"brand":"Tramontina"}',
    unprovenClaimsJson: null,
    titleCharCount: 37,
    descriptionCharCount: 56,
    characterLimitValid: true,
    userEdited: false,
    reviewNotes: null,
    createdAt: '2026-11-19T14:00:00Z',
  },
  versionHistory: [
    {
      id: 'ver-uuid-001',
      draftListingId: 'draft-uuid-001',
      versionNumber: 1,
      promptTemplateId: 'LISTING_DRAFT_GENERATION',
      promptTemplateVersion: 'v1.0.0',
      aiModelName: 'mock-deterministic-v1',
      aiProvider: 'MOCK_PROVIDER',
      tone: 'INFORMATIVE_OBJECTIVE',
      targetMarketplace: 'MERCADO_LIVRE',
      inputAttributesJson: '{"brand":"Tramontina","model":"Solar"}',
      generatedTitle: 'Panela Tramontina Solar Aço Inox 20cm',
      generatedDescription: 'Panela Tramontina Solar em Aço Inox original de fábrica.',
      reviewedTitle: null,
      reviewedDescription: null,
      effectiveTitle: 'Panela Tramontina Solar Aço Inox 20cm',
      effectiveDescription: 'Panela Tramontina Solar em Aço Inox original de fábrica.',
      traceableAttributesJson: '{"brand":"Tramontina"}',
      unprovenClaimsJson: null,
      titleCharCount: 37,
      descriptionCharCount: 56,
      characterLimitValid: true,
      userEdited: false,
      reviewNotes: null,
      createdAt: '2026-11-19T14:00:00Z',
    },
  ],
  marketplaceLimits: mockRules[0],
  createdAt: '2026-11-19T14:00:00Z',
  updatedAt: '2026-11-19T14:00:00Z',
};

describe('DraftListingEditorView Component (P020-FE)', () => {
  beforeEach(() => {
    vi.spyOn(global, 'fetch').mockImplementation((input: RequestInfo | URL) => {
      const url = String(input);

      if (url.includes('/api/ai/drafts/rules')) {
        return Promise.resolve({
          ok: true,
          json: async () => mockRules,
        } as Response);
      }

      if (url.includes('/api/ai/drafts/draft-uuid-001/edit')) {
        return Promise.resolve({
          ok: true,
          json: async () => ({
            ...mockDraftDetail,
            currentVersionNumber: 2,
            status: 'REVIEWED',
            currentVersion: {
              ...mockDraftDetail.currentVersion,
              versionNumber: 2,
              userEdited: true,
              reviewedTitle: 'Panela Tramontina Solar Aço Inox 20cm Editada',
              effectiveTitle: 'Panela Tramontina Solar Aço Inox 20cm Editada',
            },
          }),
        } as Response);
      }

      if (url.includes('/api/ai/drafts/draft-uuid-001/approve')) {
        return Promise.resolve({
          ok: true,
          json: async () => ({
            ...mockDraftDetail,
            status: 'APPROVED',
          }),
        } as Response);
      }

      if (url.includes('/api/ai/drafts/draft-uuid-001')) {
        return Promise.resolve({
          ok: true,
          json: async () => mockDraftDetail,
        } as Response);
      }

      if (url.includes('/api/ai/drafts')) {
        return Promise.resolve({
          ok: true,
          json: async () => ({
            content: [mockDraftSummary],
            totalElements: 1,
            totalPages: 1,
            size: 20,
            number: 0,
          }),
        } as Response);
      }

      return Promise.reject(new Error(`Endpoint não mockado: ${url}`));
    });

    Object.assign(navigator, {
      clipboard: {
        writeText: vi.fn().mockImplementation(() => Promise.resolve()),
      },
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('deve renderizar a tela de rascunhos com banner de governança mandatória do MVP', async () => {
    render(<DraftListingEditorView />);

    await waitFor(() => {
      expect(
        screen.getByText(/MODO RASCUNHO ASSISTIDO/i)
      ).toBeInTheDocument();
      expect(
        screen.getByText(/publicação automática ou direta em marketplaces está categoricamente desabilitada no MVP/i)
      ).toBeInTheDocument();
    });
  });

  it('deve carregar a lista de rascunhos e preencher os dados no editor', async () => {
    render(<DraftListingEditorView />);

    await waitFor(() => {
      expect(screen.getByDisplayValue('Panela Tramontina Solar Aço Inox 20cm')).toBeInTheDocument();
      expect(screen.getAllByText(/Versão 1/i).length).toBeGreaterThan(0);
    });
  });

  it('deve aplicar limites de caracteres em tempo real e sinalizar violação quando título exceder 60 caracteres no Mercado Livre', async () => {
    render(<DraftListingEditorView />);

    await waitFor(() => {
      expect(screen.getByDisplayValue('Panela Tramontina Solar Aço Inox 20cm')).toBeInTheDocument();
    });

    const titleInput = screen.getByDisplayValue('Panela Tramontina Solar Aço Inox 20cm');
    const tooLongTitle = 'Panela Tramontina Solar em Aço Inox com Fundo Triplo Original de Fábrica para Cozinha Gourmet Residencial';
    expect(tooLongTitle.length).toBeGreaterThan(60);

    fireEvent.change(titleInput, { target: { value: tooLongTitle } });

    await waitFor(() => {
      expect(screen.getByText(/Excedeu limite máximo/i)).toBeInTheDocument();
      expect(screen.getByText(/O título deve possuir entre 1 e 60 caracteres/i)).toBeInTheDocument();
      const saveBtn = screen.getByRole('button', { name: /Salvar Revisão/i });
      expect(saveBtn).toBeDisabled();
    });
  });

  it('deve salvar edição gerando nova versão (userEdited=true) e exibir mensagem de sucesso', async () => {
    render(<DraftListingEditorView />);

    await waitFor(() => {
      expect(screen.getByDisplayValue('Panela Tramontina Solar Aço Inox 20cm')).toBeInTheDocument();
    });

    const titleInput = screen.getByDisplayValue('Panela Tramontina Solar Aço Inox 20cm');
    fireEvent.change(titleInput, { target: { value: 'Panela Tramontina Solar Aço Inox 20cm Editada' } });

    const saveBtn = screen.getByRole('button', { name: /Salvar Revisão \(Nova Versão\)/i });
    expect(saveBtn).not.toBeDisabled();
    fireEvent.click(saveBtn);

    await waitFor(() => {
      expect(screen.getByText(/Nova versão de revisão salva com sucesso!/i)).toBeInTheDocument();
    });
  });

  it('deve alternar a visualização comparativa lado a lado (Original vs Revisado)', async () => {
    render(<DraftListingEditorView />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Ver Comparação Lado a Lado/i })).toBeInTheDocument();
    });

    const diffBtn = screen.getByRole('button', { name: /Ver Comparação Lado a Lado/i });
    fireEvent.click(diffBtn);

    await waitFor(() => {
      expect(screen.getByText(/Comparação de Conteúdo \(Original vs Revisado\)/i)).toBeInTheDocument();
      expect(screen.getAllByText(/Original \(IA Generativa\)/i).length).toBeGreaterThan(0);
    });
  });

  it('deve abrir a gaveta de histórico de versões e fechar via tecla Escape', async () => {
    render(<DraftListingEditorView />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Histórico \(1\)/i })).toBeInTheDocument();
    });

    const historyBtn = screen.getByRole('button', { name: /Histórico \(1\)/i });
    fireEvent.click(historyBtn);

    await waitFor(() => {
      expect(screen.getByRole('dialog', { name: /Histórico de Versões do Rascunho/i })).toBeInTheDocument();
      expect(screen.getByText(/1 versões registradas de forma auditável/i)).toBeInTheDocument();
    });

    fireEvent.keyDown(window, { key: 'Escape' });

    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: /Histórico de Versões do Rascunho/i })).not.toBeInTheDocument();
    });
  });

  it('deve copiar o título para a área de transferência', async () => {
    render(<DraftListingEditorView />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Copiar Título/i })).toBeInTheDocument();
    });

    const copyBtn = screen.getByRole('button', { name: /Copiar Título/i });
    fireEvent.click(copyBtn);

    expect(navigator.clipboard.writeText).toHaveBeenCalledWith('Panela Tramontina Solar Aço Inox 20cm');
    await waitFor(() => {
      expect(screen.getByText(/Título copiado!/i)).toBeInTheDocument();
    });
  });

  it('deve aprovar o rascunho localmente com sucesso', async () => {
    render(<DraftListingEditorView />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Aprovar Rascunho Local/i })).toBeInTheDocument();
    });

    const approveBtn = screen.getByRole('button', { name: /Aprovar Rascunho Local/i });
    fireEvent.click(approveBtn);

    await waitFor(() => {
      expect(screen.getByText(/Rascunho aprovado com sucesso para uso local!/i)).toBeInTheDocument();
    });
  });

  it('PROIBIÇÃO ESTRITA: nunca deve exibir botões de publicação externa no Mercado Livre ou Shopee no MVP', async () => {
    render(<DraftListingEditorView />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Salvar Revisão/i })).toBeInTheDocument();
    });

    // Procura por botões de publicação externa
    const publishButtons = screen.queryAllByRole('button', {
      name: /(publicar no mercado livre|publicar na shopee|publicar anúncio|enviar para canal)/i,
    });
    expect(publishButtons.length).toBe(0);
  });
});
