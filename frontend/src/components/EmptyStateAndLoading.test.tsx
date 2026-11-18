import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import { EmptyState } from './EmptyState';
import { LoadingSkeleton } from './LoadingSkeleton';

describe('UI States Components (RF-052: Empty States & Loading Skeletons)', () => {
  it('deve renderizar o EmptyState com título, descrição e acionar o botão de ação', () => {
    const handleAction = vi.fn();

    render(
      <EmptyState
        title="Nenhum Anúncio Encontrado"
        description="Não encontramos anúncios ativos com os filtros selecionados."
        actionLabel="Limpar Filtros"
        onAction={handleAction}
      />
    );

    expect(screen.getByText('Nenhum Anúncio Encontrado')).toBeDefined();
    expect(screen.getByText('Não encontramos anúncios ativos com os filtros selecionados.')).toBeDefined();

    const actionBtn = screen.getByRole('button', { name: 'Limpar Filtros' });
    fireEvent.click(actionBtn);

    expect(handleAction).toHaveBeenCalledTimes(1);
  });

  it('deve renderizar o LoadingSkeleton com role status e acessibilidade', () => {
    const { container } = render(<LoadingSkeleton rows={4} />);

    expect(screen.getByRole('status', { name: 'Carregando conteúdo...' })).toBeDefined();
    expect(screen.getByText('Carregando...')).toBeDefined();

    // 1 header + 1 subtitle + 4 rows = 6 divs com animação
    const pulsingElements = container.querySelectorAll('.animate-pulse');
    expect(pulsingElements.length).toBeGreaterThan(0);
  });
});
