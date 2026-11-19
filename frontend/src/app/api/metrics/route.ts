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
  const sellerAccountId = searchParams.get('sellerAccountId');
  const marketplace = searchParams.get('marketplace');
  const externalListingId = searchParams.get('externalListingId');
  const scope = searchParams.get('scope');
  const metricKey = searchParams.get('metricKey');
  const status = searchParams.get('status');
  const startDate = searchParams.get('startDate');
  const endDate = searchParams.get('endDate');
  const page = searchParams.get('page') || '0';
  const size = searchParams.get('size') || '50';

  const backendUrl = new URL(`${BACKEND_URL}/api/v1/metrics`);
  if (sellerAccountId) backendUrl.searchParams.set('sellerAccountId', sellerAccountId);
  if (marketplace) backendUrl.searchParams.set('marketplace', marketplace);
  if (externalListingId) backendUrl.searchParams.set('externalListingId', externalListingId);
  if (scope) backendUrl.searchParams.set('scope', scope);
  if (metricKey) backendUrl.searchParams.set('metricKey', metricKey);
  if (status) backendUrl.searchParams.set('status', status);
  if (startDate) backendUrl.searchParams.set('startDate', startDate);
  if (endDate) backendUrl.searchParams.set('endDate', endDate);
  backendUrl.searchParams.set('page', page);
  backendUrl.searchParams.set('size', size);

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
          error: 'FETCH_METRICS_FAILED',
          message: err.detail || err.message || 'Falha ao buscar métricas oficiais.',
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
        message: 'Erro interno de comunicação com serviço de analytics.',
      },
      { status: 500 }
    );
  }
}
