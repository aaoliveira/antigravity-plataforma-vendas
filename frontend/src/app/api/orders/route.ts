import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';

const BACKEND_URL = process.env.INTERNAL_BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8082';

export async function GET(request: NextRequest) {
  const cookieStore = await cookies();
  const token = cookieStore.get('seller_access_token')?.value;

  if (!token) {
    return NextResponse.json(
      { error: 'UNAUTHORIZED', message: 'Sessão não autenticada.' },
      { status: 401 }
    );
  }

  const { searchParams } = new URL(request.url);
  const page = searchParams.get('page') || '0';
  const size = searchParams.get('size') || '10';
  const marketplace = searchParams.get('marketplace');
  const sellerAccountId = searchParams.get('sellerAccountId');
  const status = searchParams.get('status');
  const startDate = searchParams.get('startDate');
  const endDate = searchParams.get('endDate');
  const search = searchParams.get('search');

  const backendParams = new URLSearchParams();
  backendParams.set('page', page);
  backendParams.set('size', size);
  if (marketplace && marketplace !== 'ALL') backendParams.set('marketplace', marketplace);
  if (sellerAccountId && sellerAccountId !== 'ALL') backendParams.set('sellerAccountId', sellerAccountId);
  if (status && status !== 'ALL') backendParams.set('status', status);
  if (startDate && startDate.trim()) backendParams.set('startDate', startDate.trim());
  if (endDate && endDate.trim()) backendParams.set('endDate', endDate.trim());
  if (search && search.trim()) backendParams.set('search', search.trim());

  try {
    const res = await fetch(`${BACKEND_URL}/api/v1/orders?${backendParams.toString()}`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (!res.ok) {
      return NextResponse.json(
        { error: 'FETCH_FAILED', message: 'Falha ao buscar pedidos de venda.' },
        { status: res.status }
      );
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch {
    return NextResponse.json(
      { error: 'INTERNAL_SERVER_ERROR', message: 'Erro de comunicação com o serviço de pedidos.' },
      { status: 500 }
    );
  }
}
