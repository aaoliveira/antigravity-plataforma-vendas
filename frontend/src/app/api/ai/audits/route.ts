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
  const promptKey = searchParams.get('promptKey');
  const provider = searchParams.get('provider');
  const status = searchParams.get('status');
  const startDate = searchParams.get('startDate');
  const endDate = searchParams.get('endDate');
  const page = searchParams.get('page') || '0';
  const size = searchParams.get('size') || '20';

  const backendUrl = new URL(`${BACKEND_URL}/api/v1/ai/audits`);
  if (promptKey) backendUrl.searchParams.set('promptKey', promptKey);
  if (provider) backendUrl.searchParams.set('provider', provider);
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
        { error: 'FETCH_AUDITS_FAILED', message: err.detail || err.message || 'Falha ao buscar auditoria de IA.' },
        { status: res.status }
      );
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : 'Falha na comunicação com o backend.';
    return NextResponse.json(
      { error: 'FETCH_AUDITS_FAILED', message },
      { status: 500 }
    );
  }
}
