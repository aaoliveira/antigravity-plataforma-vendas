import { describe, it, expect, vi } from 'vitest';
import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { AccessDeniedFeedback } from './AccessDeniedFeedback';
import { SessionBanner } from './SessionBanner';
import NotFound from '@/app/not-found';
import {
  sanitizeErrorMessage,
  getUniformFeedbackForHttpStatus,
  UNIFORM_ACCESS_OR_NOT_FOUND_MESSAGE,
  UNIFORM_SESSION_EXPIRED_MESSAGE,
} from '@/lib/errorHandler';

const UUID_REGEX = /[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}/;

describe('Tratamento Padronizado de Erros e Proteção Cross-Tenant (401/403/404)', () => {
  it('AccessDeniedFeedback deve exibir mensagem uniforme anti-enumeração sem revelar recursos alheios', () => {
    const onBack = vi.fn();
    const { container } = render(<AccessDeniedFeedback onBackClick={onBack} />);

    expect(screen.getByText('Recurso Não Encontrado ou Restrito')).toBeDefined();
    expect(screen.getByText(UNIFORM_ACCESS_OR_NOT_FOUND_MESSAGE)).toBeDefined();

    // Valida que nenhum UUID técnico foi renderizado
    expect(UUID_REGEX.test(container.textContent || '')).toBe(false);

    // Botão de retorno funciona
    const button = screen.getByRole('button', { name: /voltar ao início/i });
    fireEvent.click(button);
    expect(onBack).toHaveBeenCalledTimes(1);
  });

  it('not-found.tsx deve renderizar feedback uniforme com link seguro para o dashboard', () => {
    const { container } = render(<NotFound />);

    expect(screen.getByRole('main')).toBeDefined();
    expect(screen.getByText('Recurso Não Encontrado ou Restrito')).toBeDefined();
    expect(screen.getByText(UNIFORM_ACCESS_OR_NOT_FOUND_MESSAGE)).toBeDefined();

    // Link para dashboard
    const link = screen.getByRole('link', { name: /voltar ao dashboard/i });
    expect(link.getAttribute('href')).toBe('/');

    // Nenhum UUID técnico vazado
    expect(UUID_REGEX.test(container.textContent || '')).toBe(false);
  });

  it('SessionBanner deve exibir alerta acessível de sessão expirada (401)', () => {
    const onLoginClick = vi.fn();
    const { container } = render(<SessionBanner onLoginClick={onLoginClick} />);

    const alert = screen.getByRole('alert');
    expect(alert).toBeDefined();
    expect(screen.getByText('Sessão Expirada ou Acesso Requerido')).toBeDefined();
    expect(screen.getByText(UNIFORM_SESSION_EXPIRED_MESSAGE)).toBeDefined();

    // Nenhum UUID técnico vazado
    expect(UUID_REGEX.test(container.textContent || '')).toBe(false);

    // Clique em entrar novamente
    const button = screen.getByRole('button', { name: /entrar novamente/i });
    fireEvent.click(button);
    expect(onLoginClick).toHaveBeenCalledTimes(1);
  });

  it('sanitizeErrorMessage deve mascarar terminantemente qualquer UUID v4 de tenant ou entidade', () => {
    const rawError = 'Erro ao consultar tenant 11111111-1111-1111-1111-111111111111 e documento 22222222-2222-2222-2222-222222222222';
    const sanitized = sanitizeErrorMessage(rawError);

    expect(sanitized).not.toContain('11111111-1111-1111-1111-111111111111');
    expect(sanitized).not.toContain('22222222-2222-2222-2222-222222222222');
    expect(UUID_REGEX.test(sanitized)).toBe(false);
  });

  it('getUniformFeedbackForHttpStatus deve retornar respostas idênticas para 403 e 404 (anti-enumeração)', () => {
    const feedback403 = getUniformFeedbackForHttpStatus(403);
    const feedback404 = getUniformFeedbackForHttpStatus(404);

    expect(feedback403.title).toBe(feedback404.title);
    expect(feedback403.description).toBe(feedback404.description);
    expect(feedback403.description).toBe(UNIFORM_ACCESS_OR_NOT_FOUND_MESSAGE);
  });
});
