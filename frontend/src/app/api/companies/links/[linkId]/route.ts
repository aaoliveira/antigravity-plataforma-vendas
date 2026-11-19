import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';

const BACKEND_URL = process.env.INTERNAL_BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8082';

export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ linkId: string }> }
) {
  const { linkId } = await params;
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
    const res = await fetch(`${BACKEND_URL}/api/v1/companies/links/${linkId}`, {
      method: 'DELETE',
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      return NextResponse.json(
        { error: 'UNLINK_FAILED', message: err.detail || err.message || 'Falha ao desvincular conta.' },
        { status: res.status }
      );
    }

    return new NextResponse(null, { status: 204 });
  } catch {
    return NextResponse.json(
      { error: 'COMMUNICATION_ERROR', message: 'Erro de comunicação ao desvincular conta.' },
      { status: 502 }
    );
  }
}
