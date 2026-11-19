import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';

const BACKEND_URL =
  process.env.INTERNAL_BACKEND_URL ||
  process.env.NEXT_PUBLIC_BACKEND_URL ||
  'http://localhost:8082';

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
  const marketplace = searchParams.get('marketplace');
  const scope = searchParams.get('scope');

  const backendUrl = new URL(`${BACKEND_URL}/api/v1/metrics/catalog`);
  if (marketplace) backendUrl.searchParams.set('marketplace', marketplace);
  if (scope) backendUrl.searchParams.set('scope', scope);

  try {
    const res = await fetch(backendUrl.toString(), {
      headers: {
        Authorization: `Bearer ${token}`,
      },
      cache: 'no-store',
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      return NextResponse.json(
        {
          error: 'FETCH_CATALOG_FAILED',
          message: err.detail || err.message || 'Falha ao buscar catálogo de métricas.',
        },
        { status: res.status }
      );
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch (error) {
    return NextResponse.json(
      {
        error: 'INTERNAL_ERROR',
        message: 'Erro interno ao consultar catálogo de métricas.',
      },
      { status: 500 }
    );
  }
}
