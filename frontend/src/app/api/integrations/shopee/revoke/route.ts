import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';

const BACKEND_URL = process.env.INTERNAL_BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8082';

export async function POST(req: NextRequest) {
  const cookieStore = await cookies();
  const token = cookieStore.get('seller_access_token')?.value;

  if (!token) {
    return NextResponse.json(
      { error: 'UNAUTHORIZED', message: 'Sessão não autenticada.' },
      { status: 401 }
    );
  }

  try {
    let connectionId: string | null = null;
    try {
      const body = await req.json();
      if (body?.connectionId) {
        connectionId = body.connectionId;
      }
    } catch {
      // Body vazio é aceitável, buscaremos a conexão ativa
    }

    if (!connectionId) {
      // Busca conexão ativa da Shopee para o tenant
      const listRes = await fetch(`${BACKEND_URL}/api/v1/integrations/shopee/connections`, {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (listRes.ok) {
        const connections = await listRes.json();
        const activeConn = connections.find((c: { status: string }) => c.status === 'ACTIVE') || connections[0];
        if (activeConn) {
          connectionId = activeConn.id;
        }
      }
    }

    if (!connectionId) {
      return NextResponse.json(
        { error: 'NOT_FOUND', message: 'Nenhuma conexão da Shopee encontrada para revogar.' },
        { status: 404 }
      );
    }

    const res = await fetch(`${BACKEND_URL}/api/v1/integrations/shopee/connections/${connectionId}/revoke`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
    });

    if (!res.ok) {
      const errorData = await res.json().catch(() => ({}));
      return NextResponse.json(
        { error: 'REVOKE_FAILED', message: errorData.detail || 'Falha ao revogar conexão com a Shopee.' },
        { status: res.status }
      );
    }

    const data = await res.json();
    return NextResponse.json(data);
  } catch {
    return NextResponse.json(
      { error: 'COMMUNICATION_ERROR', message: 'Instabilidade de comunicação com o servidor de integrações.' },
      { status: 502 }
    );
  }
}
