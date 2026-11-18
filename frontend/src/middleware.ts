import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

// Parâmetros de URL estritamente proibidos para resolução ou troca de tenant (Anti-Tampering)
const FORBIDDEN_TENANT_PARAMS = [
  'tenantId',
  'tenant_id',
  'tenant',
  'orgId',
  'org_id',
  'organizationId',
  'organization_id',
];

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const token = request.cookies.get('seller_access_token')?.value;

  // 1. Defesa contra manipulação de tenant via URL
  // O tenant ativo NUNCA pode ser alternado por parâmetros de URL. Se detectados, limpa a URL e redireciona.
  let hasInjectedTenantParam = false;
  for (const param of FORBIDDEN_TENANT_PARAMS) {
    if (request.nextUrl.searchParams.has(param)) {
      hasInjectedTenantParam = true;
      request.nextUrl.searchParams.delete(param);
    }
  }

  if (hasInjectedTenantParam) {
    return NextResponse.redirect(request.nextUrl);
  }

  // Rotas públicas que não devem ser bloqueadas
  const isPublicRoute =
    pathname.startsWith('/login') ||
    pathname.startsWith('/api/auth/login') ||
    pathname.startsWith('/api/auth/logout') ||
    pathname.startsWith('/_next') ||
    pathname.startsWith('/favicon.ico');

  // 2. Se o usuário NÃO está autenticado e tenta acessar uma rota protegida -> Redireciona para /login
  if (!token && !isPublicRoute) {
    const loginUrl = new URL('/login', request.url);
    if (pathname !== '/') {
      loginUrl.searchParams.set('redirect', pathname);
    }
    return NextResponse.redirect(loginUrl);
  }

  // 3. Se o usuário JÁ está autenticado e tenta acessar a tela de /login -> Redireciona para o dashboard
  if (token && pathname === '/login') {
    return NextResponse.redirect(new URL('/', request.url));
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    /*
     * Intercepta todas as rotas exceto arquivos estáticos
     */
    '/((?!_next/static|_next/image|favicon.ico).*)',
  ],
};
