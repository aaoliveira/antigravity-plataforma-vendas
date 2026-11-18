import { NextResponse } from 'next/server';
import { cookies } from 'next/headers';

const BACKEND_URL = process.env.INTERNAL_BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8082';

export async function GET() {
  const cookieStore = await cookies();
  const token = cookieStore.get('seller_access_token')?.value;

  if (!token) {
    return NextResponse.json({ authenticated: false, message: 'Nenhuma sessão ativa encontrada.' }, { status: 401 });
  }

  try {
    // Consulta os dados do usuário autenticado e memberships
    const userRes = await fetch(`${BACKEND_URL}/api/v1/auth/me`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (userRes.status === 401 || userRes.status === 403) {
      // Token expirou ou foi invalidado no backend
      cookieStore.delete('seller_access_token');
      return NextResponse.json(
        { authenticated: false, error: 'SESSION_EXPIRED', message: 'Sessão expirada. Autentique-se novamente.' },
        { status: 401 }
      );
    }

    if (!userRes.ok) {
      return NextResponse.json({ authenticated: false, message: 'Falha ao recuperar sessão.' }, { status: userRes.status });
    }

    const userData = await userRes.json();

    // Consulta o tenant ativo validado
    let currentTenant = null;
    const tenantRes = await fetch(`${BACKEND_URL}/api/v1/tenancy/current`, {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });

    if (tenantRes.ok) {
      currentTenant = await tenantRes.json();
    }

    return NextResponse.json({
      authenticated: true,
      user: userData,
      currentTenant,
    });
  } catch (error) {
    return NextResponse.json(
      { authenticated: false, error: 'CONNECTION_ERROR', message: 'Serviço de autenticação temporariamente indisponível.' },
      { status: 503 }
    );
  }
}
