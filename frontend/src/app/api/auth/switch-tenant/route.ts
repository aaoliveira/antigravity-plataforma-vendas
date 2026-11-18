import { NextResponse } from 'next/server';
import { cookies } from 'next/headers';

const BACKEND_URL = process.env.INTERNAL_BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8082';

export async function POST(request: Request) {
  const cookieStore = await cookies();
  const token = cookieStore.get('seller_access_token')?.value;

  if (!token) {
    return NextResponse.json({ success: false, message: 'Autenticação necessária.' }, { status: 401 });
  }

  try {
    const body = await request.json();
    const { targetTenantId } = body;

    if (!targetTenantId) {
      return NextResponse.json({ success: false, message: 'targetTenantId é obrigatório.' }, { status: 400 });
    }

    const backendRes = await fetch(`${BACKEND_URL}/api/v1/auth/switch-tenant`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify({ targetTenantId }),
    });

    const data = await backendRes.json();

    if (!backendRes.ok) {
      return NextResponse.json(
        {
          success: false,
          title: data.title || 'Falha ao Alternar Tenant',
          detail: data.detail || 'Não foi possível alternar para o tenant informado.',
        },
        { status: backendRes.status }
      );
    }

    // Atualiza o cookie HttpOnly com o novo token emitido pelo backend
    cookieStore.set('seller_access_token', data.accessToken, {
      httpOnly: true,
      secure: process.env.NODE_ENV === 'production',
      sameSite: 'lax',
      path: '/',
      maxAge: data.expiresInSeconds || 900,
    });

    return NextResponse.json({
      success: true,
      activeTenantId: data.activeTenantId,
      roles: data.roles,
    });
  } catch (error) {
    return NextResponse.json(
      { success: false, message: 'Serviço temporariamente indisponível.' },
      { status: 503 }
    );
  }
}
