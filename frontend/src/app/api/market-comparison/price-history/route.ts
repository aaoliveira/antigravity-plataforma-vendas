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
  const externalListingId = searchParams.get('externalListingId');

  if (!marketplace || !externalListingId) {
    return NextResponse.json(
      {
        error: 'BAD_REQUEST',
        message: 'Os parâmetros marketplace e externalListingId são obrigatórios.',
      },
      { status: 400 }
    );
  }

  const backendParams = new URLSearchParams();
  backendParams.set('marketplace', marketplace);
  backendParams.set('externalListingId', externalListingId);

  const startDate = searchParams.get('startDate');
  if (startDate) backendParams.set('startDate', startDate);

  const endDate = searchParams.get('endDate');
  if (endDate) backendParams.set('endDate', endDate);

  try {
    const res = await fetch(
      `${BACKEND_URL}/api/v1/market-comparison/price-history?${backendParams.toString()}`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );

    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      return NextResponse.json(
        {
          error: 'PRICE_HISTORY_FAILED',
          message:
            errorData.detail ||
            errorData.message ||
            'Falha ao obter histórico legítimo de preços.',
        },
        { status: res.status }
      );
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch {
    return NextResponse.json(
      {
        error: 'INTERNAL_SERVER_ERROR',
        message: 'Erro ao se comunicar com o serviço de histórico de preços.',
      },
      { status: 500 }
    );
  }
}

export async function POST(request: NextRequest) {
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

  try {
    const body = await request.json();

    const res = await fetch(`${BACKEND_URL}/api/v1/market-comparison/price-history`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(body),
    });

    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      return NextResponse.json(
        {
          error: 'RECORD_PRICE_FAILED',
          message:
            errorData.detail ||
            errorData.message ||
            'Falha ao registrar ponto de preço.',
        },
        { status: res.status }
      );
    }

    const data = await res.json();
    return NextResponse.json(data, { status: 201 });
  } catch {
    return NextResponse.json(
      {
        error: 'INTERNAL_SERVER_ERROR',
        message: 'Erro ao se comunicar com o serviço de histórico de preços.',
      },
      { status: 500 }
    );
  }
}
