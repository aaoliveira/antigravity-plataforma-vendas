import { describe, it, expect } from 'vitest';
import { NextRequest } from 'next/server';
import { middleware } from '@/middleware';

describe('Proteção contra Manipulação de Tenant via URL (Anti-Tampering)', () => {
  it('deve interceptar e descartar parâmetro tenantId injetado na URL, redirecionando para URL limpa', () => {
    const maliciousUrl = 'https://app.sellerintelligence.com/?tenantId=22222222-2222-2222-2222-222222222222';
    const req = new NextRequest(maliciousUrl, {
      cookies: { seller_access_token: 'valid-token' },
    });

    const response = middleware(req);
    expect(response).toBeDefined();

    // Deve redirecionar (status 307 ou 308)
    expect(response.status).toBeGreaterThanOrEqual(300);
    expect(response.status).toBeLessThan(400);

    const redirectLocation = response.headers.get('location');
    expect(redirectLocation).toBeDefined();

    const cleanUrl = new URL(redirectLocation!);
    expect(cleanUrl.searchParams.has('tenantId')).toBe(false);
    expect(cleanUrl.pathname).toBe('/');
  });

  it('deve interceptar múltiplos parâmetros proibidos de tenant (orgId, tenant_id, organizationId)', () => {
    const maliciousUrl = 'https://app.sellerintelligence.com/dashboard?orgId=evil-corp&tenant_id=fake-id&organizationId=spoofed';
    const req = new NextRequest(maliciousUrl, {
      cookies: { seller_access_token: 'valid-token' },
    });

    const response = middleware(req);
    expect(response.status).toBeGreaterThanOrEqual(300);

    const cleanUrl = new URL(response.headers.get('location')!);
    expect(cleanUrl.searchParams.has('orgId')).toBe(false);
    expect(cleanUrl.searchParams.has('tenant_id')).toBe(false);
    expect(cleanUrl.searchParams.has('organizationId')).toBe(false);
    expect(cleanUrl.pathname).toBe('/dashboard');
  });

  it('deve preservar parâmetros legítimos que não sejam de identificação de tenant', () => {
    const legitUrl = 'https://app.sellerintelligence.com/login?redirect=/financas';
    const req = new NextRequest(legitUrl);

    const response = middleware(req);
    // Rota pública /login sem token continua normalmente
    expect(response.status).toBe(200);
  });
});
