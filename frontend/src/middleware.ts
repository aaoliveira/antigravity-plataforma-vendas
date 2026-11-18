import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

export function middleware(request: NextRequest) {
  const { pathname } = request.nextUrl;
  const token = request.cookies.get('seller_access_token')?.value;

  // Rotas públicas que não devem ser bloqueadas
  const isPublicRoute =
    pathname.startsWith('/login') ||
    pathname.startsWith('/api/auth/login') ||
    pathname.startsWith('/api/auth/logout') ||
    pathname.startsWith('/_next') ||
    pathname.startsWith('/favicon.ico');

  // 1. Se o usuário NÃO está autenticado e tenta acessar uma rota protegida -> Redireciona para /login
  if (!token && !isPublicRoute) {
    const loginUrl = new URL('/login', request.url);
    if (pathname !== '/') {
      loginUrl.searchParams.set('redirect', pathname);
    }
    return NextResponse.redirect(loginUrl);
  }

  // 2. Se o usuário JÁ está autenticado e tenta acessar a tela de /login -> Redireciona para o dashboard
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
