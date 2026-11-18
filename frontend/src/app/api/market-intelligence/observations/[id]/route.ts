import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';

const BACKEND_URL = process.env.INTERNAL_BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8082';

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const cookieStore = await cookies();
  const token = cookieStore.get('seller_access_token')?.value;

  if (!token) {
    return NextResponse.json(
      { error: 'UNAUTHORIZED', message: 'Sessão não autenticada.' },
      { status: 401 }
    );
  }

  const { id } = await params;

  try {
    const res = await fetch(`${BACKEND_URL}/api/v1/market-intelligence/observations/${id}`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (!res.ok) {
      if (res.status === 404) {
        return NextResponse.json(
          { error: 'NOT_FOUND', message: 'Observação de mercado não encontrada.' },
          { status: 404 }
        );
      }
      return NextResponse.json(
        { error: 'FETCH_FAILED', message: 'Falha ao buscar detalhes da observação de mercado.' },
        { status: res.status }
      );
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch {
    return NextResponse.json(
      { error: 'INTERNAL_SERVER_ERROR', message: 'Erro de comunicação com o serviço de observações.' },
      { status: 500 }
    );
  }
}
