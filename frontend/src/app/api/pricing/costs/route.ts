import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';

const BACKEND_URL = process.env.INTERNAL_BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8082';

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
    const res = await fetch(`${BACKEND_URL}/api/v1/pricing/costs`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(body),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      return NextResponse.json(
        { error: 'REGISTER_COST_FAILED', message: err.detail || err.message || 'Falha ao registrar custos do produto.' },
        { status: res.status }
      );
    }

    const data = await res.json();
    return NextResponse.json(data, { status: 201 });
  } catch {
    return NextResponse.json(
      { error: 'INTERNAL_SERVER_ERROR', message: 'Erro de comunicação ao registrar custos.' },
      { status: 500 }
    );
  }
}

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
  const productId = searchParams.get('productId');
  if (!productId) {
    return NextResponse.json(
      { error: 'BAD_REQUEST', message: 'productId é obrigatório.' },
      { status: 400 }
    );
  }

  const backendParams = new URLSearchParams();
  backendParams.set('productId', productId);
  if (searchParams.get('variantId')) backendParams.set('variantId', searchParams.get('variantId')!);
  if (searchParams.get('companyId')) backendParams.set('companyId', searchParams.get('companyId')!);
  if (searchParams.get('referenceDate')) backendParams.set('referenceDate', searchParams.get('referenceDate')!);

  try {
    const res = await fetch(`${BACKEND_URL}/api/v1/pricing/costs/effective?${backendParams.toString()}`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (res.status === 404) {
      return NextResponse.json(
        { error: 'NOT_FOUND', message: 'Nenhum custo cadastrado para este produto.' },
        { status: 404 }
      );
    }

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      return NextResponse.json(
        { error: 'FETCH_COST_FAILED', message: err.detail || 'Falha ao consultar custo efetivo.' },
        { status: res.status }
      );
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch {
    return NextResponse.json(
      { error: 'INTERNAL_SERVER_ERROR', message: 'Erro de comunicação ao consultar custos.' },
      { status: 500 }
    );
  }
}
