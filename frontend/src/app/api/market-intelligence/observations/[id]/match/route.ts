import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';

const BACKEND_URL = process.env.INTERNAL_BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8082';

export async function POST(
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
  let body = {};
  try {
    body = await request.json();
  } catch {
    // optional body
  }

  try {
    const res = await fetch(`${BACKEND_URL}/api/v1/market-intelligence/observations/${id}/match`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });

    if (!res.ok) {
      return NextResponse.json(
        { error: 'MATCH_FAILED', message: 'Falha ao executar matching para a observação.' },
        { status: res.status }
      );
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch {
    return NextResponse.json(
      { error: 'INTERNAL_SERVER_ERROR', message: 'Erro de comunicação ao executar análise de correspondência.' },
      { status: 500 }
    );
  }
}
