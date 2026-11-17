import { describe, it, expect } from 'vitest';
import { assertActionPermitted, validateSafeUrlForPlatform, SecurityViolationException } from './antiScrapingGuard';

describe('antiScrapingGuard', () => {
  it('allows safe contextual actions', () => {
    expect(() => assertActionPermitted('INSPECT_CANONICAL_ID')).not.toThrow();
    expect(() => assertActionPermitted('OPEN_WEB_TAB')).not.toThrow();
  });

  it('throws SecurityViolationException for forbidden actions violating PRD/DEC rules', () => {
    expect(() => assertActionPermitted('READ_COOKIES')).toThrow(SecurityViolationException);
    expect(() => assertActionPermitted('AUTOMATE_PURCHASE')).toThrow(SecurityViolationException);
    expect(() => assertActionPermitted('CHANGE_PRICE')).toThrow(SecurityViolationException);
    expect(() => assertActionPermitted('PUBLISH_EXTERNAL_LISTING')).toThrow(SecurityViolationException);
    expect(() => assertActionPermitted('READ_BUYER_PRIVATE_DATA')).toThrow(SecurityViolationException);
    expect(() => assertActionPermitted('SCRAPE_BACKGROUND_ORDERS')).toThrow(SecurityViolationException);
  });

  it('validates safe URLs and rejects dangerous script schemes', () => {
    expect(validateSafeUrlForPlatform('https://produto.mercadolivre.com.br/MLB-123')).toBe(true);
    expect(validateSafeUrlForPlatform('http://localhost:3000/?tab=AI_STUDIO')).toBe(true);
    expect(validateSafeUrlForPlatform('javascript:alert(1)')).toBe(false);
    expect(validateSafeUrlForPlatform('data:text/html;base64,PHNjcmlwdD4=')).toBe(false);
    expect(validateSafeUrlForPlatform('file:///etc/passwd')).toBe(false);
  });
});
