import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';

const BACKEND_URL = process.env.INTERNAL_BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8082';

export async function GET(request: NextRequest) {
  const cookieStore = await cookies();
  const token =
    cookieStore.get('seller_access_token')?.value ||
    request.headers.get('Authorization')?.replace('Bearer ', '');

  if (!token) {
    return NextResponse.json(
      { error: 'UNAUTHORIZED', message: 'Sessão não autenticada.' },
      { status: 401 }
    );
  }

  const { searchParams } = new URL(request.url);
  const q = searchParams.get('q');
  if (!q || !q.trim()) {
    return NextResponse.json(
      { error: 'BAD_REQUEST', message: "O parâmetro de busca 'q' é obrigatório." },
      { status: 400 }
    );
  }

  const backendParams = new URLSearchParams();
  backendParams.set('q', q.trim());

  const marketplace = searchParams.get('marketplace');
  if (marketplace && marketplace !== 'ALL') {
    backendParams.set('marketplace', marketplace);
  }

  const category = searchParams.get('category');
  if (category && category.trim()) {
    backendParams.set('category', category.trim());
  }

  const minPrice = searchParams.get('minPrice');
  if (minPrice && minPrice.trim()) {
    backendParams.set('minPrice', minPrice.trim());
  }

  const maxPrice = searchParams.get('maxPrice');
  if (maxPrice && maxPrice.trim()) {
    backendParams.set('maxPrice', maxPrice.trim());
  }

  const onlyOwnCatalog = searchParams.get('onlyOwnCatalog');
  if (onlyOwnCatalog === 'true') {
    backendParams.set('onlyOwnCatalog', 'true');
  }

  const page = searchParams.get('page') || '0';
  const size = searchParams.get('size') || '20';
  backendParams.set('page', page);
  backendParams.set('size', size);

  try {
    const res = await fetch(`${BACKEND_URL}/api/v1/market-search?${backendParams.toString()}`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (res.status === 429) {
      const errorData = await res.json().catch(() => ({}));
      const retryAfter = res.headers.get('Retry-After') || '60';
      return NextResponse.json(
        {
          error: 'RATE_LIMIT_EXCEEDED',
          message: errorData.detail || 'Limite de taxa de pesquisa atingido. Tente novamente em breve.',
          retryAfter: parseInt(retryAfter, 10),
        },
        {
          status: 429,
          headers: { 'Retry-After': retryAfter },
        }
      );
    }

    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      return NextResponse.json(
        { error: 'SEARCH_FAILED', message: errorData.detail || 'Falha ao executar pesquisa de mercado.' },
        { status: res.status }
      );
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch {
    return NextResponse.json(
      { error: 'INTERNAL_SERVER_ERROR', message: 'Erro de comunicação com o serviço de pesquisa de mercado.' },
      { status: 500 }
    );
  }
}
