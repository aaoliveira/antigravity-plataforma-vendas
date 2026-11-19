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
  const search = searchParams.get('search') || '';
  const activeOnly = searchParams.get('activeOnly') || 'false';
  const page = searchParams.get('page') || '0';
  const size = searchParams.get('size') || '50';

  const backendUrl = new URL(`${BACKEND_URL}/api/v1/companies`);
  if (search) backendUrl.searchParams.set('search', search);
  backendUrl.searchParams.set('activeOnly', activeOnly);
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
        { error: 'FETCH_COMPANIES_FAILED', message: err.detail || err.message || 'Falha ao buscar empresas.' },
        { status: res.status }
      );
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch {
    return NextResponse.json(
      { error: 'COMMUNICATION_ERROR', message: 'Instabilidade de comunicação com o servidor de empresas.' },
      { status: 502 }
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
    const res = await fetch(`${BACKEND_URL}/api/v1/companies`, {
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
        { error: 'CREATE_COMPANY_FAILED', message: err.detail || err.message || 'Falha ao cadastrar empresa.' },
        { status: res.status }
      );
    }

    const data = await res.json();
    return NextResponse.json(data, { status: 201 });
  } catch {
    return NextResponse.json(
      { error: 'COMMUNICATION_ERROR', message: 'Erro de comunicação ao criar empresa.' },
      { status: 502 }
    );
  }
}
