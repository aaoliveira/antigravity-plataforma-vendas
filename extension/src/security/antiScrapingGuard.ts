/**
 * Guarda de Segurança e Anti-Scraping da Extensão
 * Em estrita conformidade com PRD Seções 1.2, 4.2, 12.4 e ADR DEC-012/DEC-018:
 * - Proibição absoluta de leitura ou transmissão de cookies de terceiros.
 * - Proibição absoluta de automação de compras, cliques, preços ou publicação externa.
 */

export const FORBIDDEN_ACTIONS = [
  'READ_COOKIES',
  'AUTOMATE_PURCHASE',
  'CHANGE_PRICE',
  'PUBLISH_EXTERNAL_LISTING',
  'READ_BUYER_PRIVATE_DATA',
  'SCRAPE_BACKGROUND_ORDERS',
] as const;

export type ForbiddenAction = typeof FORBIDDEN_ACTIONS[number];

export class SecurityViolationException extends Error {
  constructor(action: string) {
    super(`Ação proibida pelas diretrizes de segurança da Seller Intelligence Platform: ${action}`);
    this.name = 'SecurityViolationException';
  }
}

export function assertActionPermitted(action: string): void {
  for (const forbidden of FORBIDDEN_ACTIONS) {
    if (action.toUpperCase().includes(forbidden)) {
      throw new SecurityViolationException(forbidden);
    }
  }
}

export function validateSafeUrlForPlatform(url: string): boolean {
  if (!url || typeof url !== 'string') return false;
  // Bloqueia esquemas perigosos javascript:, data:, file:
  const lower = url.trim().toLowerCase();
  if (lower.startsWith('javascript:') || lower.startsWith('data:') || lower.startsWith('file:')) {
    return false;
  }
  return true;
}
