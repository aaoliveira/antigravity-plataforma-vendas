import { describe, it, expect } from 'vitest';
import { middleware } from '@/middleware';
import { NextRequest } from 'next/server';

describe('Route Protection Middleware & HttpOnly Policy', () => {
  it('deve redirecionar requisição anônima na rota "/" para "/login"', () => {
    const request = new NextRequest(new URL('http://localhost:3000/'));

    const response = middleware(request);

    expect(response.status).toBe(307); // Temporary Redirect
    const location = response.headers.get('location');
    expect(location).toContain('/login');
  });

  it('deve permitir acesso à rota de login para usuário anônimo', () => {
    const request = new NextRequest(new URL('http://localhost:3000/login'));

    const response = middleware(request);

    // NextResponse.next() tem status 200
    expect(response.status).toBe(200);
  });

  it('deve redirecionar usuário já autenticado acessando "/login" para o dashboard "/"', () => {
    const request = new NextRequest(new URL('http://localhost:3000/login'));
    request.cookies.set('seller_access_token', 'valid-jwt-token-string');

    const response = middleware(request);

    expect(response.status).toBe(307);
    const location = response.headers.get('location');
    expect(location).toBe('http://localhost:3000/');
  });

  it('deve permitir acesso ao dashboard "/" quando o cookie HttpOnly "seller_access_token" estiver presente', () => {
    const request = new NextRequest(new URL('http://localhost:3000/'));
    request.cookies.set('seller_access_token', 'valid-jwt-token-string');

    const response = middleware(request);

    expect(response.status).toBe(200);
  });
});
