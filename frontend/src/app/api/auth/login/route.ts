import { NextResponse } from 'next/server';
import { cookies } from 'next/headers';

const BACKEND_URL = process.env.INTERNAL_BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8082';

export async function POST(request: Request) {
  try {
    const body = await request.json();
    const { email, password, requestedTenantId } = body;

    if (!email || !password) {
      return NextResponse.json(
        { title: 'Dados Inválidos', detail: 'E-mail e senha são obrigatórios.' },
        { status: 400 }
      );
    }

    const backendRes = await fetch(`${BACKEND_URL}/api/v1/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email, password, requestedTenantId }),
    });

    const data = await backendRes.json();

    if (!backendRes.ok) {
      return NextResponse.json(
        {
          title: data.title || 'Falha de Autenticação',
          detail: data.detail || 'Não foi possível autenticar.',
          status: backendRes.status,
        },
        { status: backendRes.status }
      );
    }

    // Grava o token exclusivamente em cookie HttpOnly (zero tokens em localStorage)
    const cookieStore = await cookies();
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
      { title: 'Erro de Conexão', detail: 'Não foi possível conectar ao serviço de autenticação.' },
      { status: 503 }
    );
  }
}
