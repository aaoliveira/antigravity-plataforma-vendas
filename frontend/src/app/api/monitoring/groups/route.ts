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
  const backendParams = new URLSearchParams();
  const search = searchParams.get('search');
  if (search && search.trim()) {
    backendParams.set('search', search.trim());
  }
  const page = searchParams.get('page') || '0';
  const size = searchParams.get('size') || '20';
  backendParams.set('page', page);
  backendParams.set('size', size);

  try {
    const res = await fetch(`${BACKEND_URL}/api/v1/monitoring/groups?${backendParams.toString()}`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      return NextResponse.json(
        { error: 'FETCH_GROUPS_FAILED', message: err.detail || 'Falha ao buscar grupos de monitoramento.' },
        { status: res.status }
      );
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch {
    return NextResponse.json(
      { error: 'INTERNAL_SERVER_ERROR', message: 'Erro de comunicação com o serviço de monitoramento.' },
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
    const res = await fetch(`${BACKEND_URL}/api/v1/monitoring/groups`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(body),
    });

    if (res.status === 409) {
      const err = await res.json().catch(() => ({}));
      return NextResponse.json(
        { error: 'DUPLICATE_GROUP', message: err.detail || 'Já existe um grupo de monitoramento com este nome.' },
        { status: 409 }
      );
    }

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      return NextResponse.json(
        { error: 'CREATE_GROUP_FAILED', message: err.detail || 'Falha ao criar grupo de monitoramento.' },
        { status: res.status }
      );
    }

    const created = await res.json();
    return NextResponse.json(created, { status: 201 });
  } catch {
    return NextResponse.json(
      { error: 'INTERNAL_SERVER_ERROR', message: 'Erro ao processar criação de grupo de monitoramento.' },
      { status: 500 }
    );
  }
}
