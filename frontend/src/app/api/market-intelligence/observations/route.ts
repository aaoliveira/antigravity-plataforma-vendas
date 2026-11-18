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
  const sourceType = searchParams.get('sourceType');
  const gtin = searchParams.get('gtin');
  const search = searchParams.get('search');
  const startDate = searchParams.get('startDate');
  const endDate = searchParams.get('endDate');

  const backendParams = new URLSearchParams();
  backendParams.set('page', page);
  backendParams.set('size', size);
  if (marketplace && marketplace !== 'ALL') backendParams.set('marketplace', marketplace);
  if (sourceType && sourceType !== 'ALL') backendParams.set('sourceType', sourceType);
  if (gtin && gtin.trim()) backendParams.set('gtin', gtin.trim());
  if (search && search.trim()) backendParams.set('search', search.trim());
  if (startDate && startDate.trim()) backendParams.set('startDate', startDate.trim());
  if (endDate && endDate.trim()) backendParams.set('endDate', endDate.trim());

  try {
    const res = await fetch(`${BACKEND_URL}/api/v1/market-intelligence/observations?${backendParams.toString()}`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (!res.ok) {
      return NextResponse.json(
        { error: 'FETCH_FAILED', message: 'Falha ao buscar observações de mercado.' },
        { status: res.status }
      );
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch {
    return NextResponse.json(
      { error: 'INTERNAL_SERVER_ERROR', message: 'Erro de comunicação com o serviço de inteligência de mercado.' },
      { status: 500 }
    );
  }
}
