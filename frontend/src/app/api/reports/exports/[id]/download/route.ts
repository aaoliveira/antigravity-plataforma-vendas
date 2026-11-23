import { NextRequest, NextResponse } from 'next/server';
import { cookies } from 'next/headers';

const BACKEND_URL = process.env.INTERNAL_BACKEND_URL || process.env.NEXT_PUBLIC_BACKEND_URL || 'http://localhost:8082';

export async function GET(
  request: NextRequest,
  context: { params: Promise<{ id: string }> }
) {
  const { id } = await context.params;
  const cookieStore = await cookies();
  const token =
    cookieStore.get('seller_access_token')?.value ||
    request.headers.get('Authorization')?.replace('Bearer ', '');

  if (!token) {
    return NextResponse.json(
      { error: 'UNAUTHORIZED', message: 'Sessão não autenticada para download.' },
      { status: 401 }
    );
  }

  const { searchParams } = new URL(request.url);
  const downloadToken = searchParams.get('token');

  if (!downloadToken) {
    return NextResponse.json(
      { error: 'BAD_REQUEST', message: 'Token temporário de download obrigatório.' },
      { status: 400 }
    );
  }

  const clientIp =
    request.headers.get('x-forwarded-for')?.split(',')[0].trim() ||
    request.headers.get('x-real-ip') ||
    '127.0.0.1';
  const userAgent = request.headers.get('user-agent') || 'NextJS-BFF';

  try {
    const backendUrl = new URL(`${BACKEND_URL}/api/v1/reports/exports/${id}/download`);
    backendUrl.searchParams.set('token', downloadToken);

    const res = await fetch(backendUrl.toString(), {
      headers: {
        Authorization: `Bearer ${token}`,
        'X-Forwarded-For': clientIp,
        'User-Agent': userAgent,
      },
      cache: 'no-store',
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      return NextResponse.json(
        {
          error: err.title || (res.status === 410 ? 'EXPORT_EXPIRED' : 'DOWNLOAD_FAILED'),
          message:
            err.detail ||
            err.message ||
            (res.status === 410
              ? 'Este arquivo expirou e não está mais disponível para download.'
              : 'Falha ao autorizar download do relatório.'),
        },
        { status: res.status }
      );
    }

    const contentType = res.headers.get('content-type') || 'application/octet-stream';
    const contentDisposition = res.headers.get('content-disposition') || `attachment; filename="export-${id}"`;
    const arrayBuffer = await res.arrayBuffer();

    return new NextResponse(arrayBuffer, {
      status: 200,
      headers: {
        'Content-Type': contentType,
        'Content-Disposition': contentDisposition,
        'Cache-Control': 'no-cache, no-store, must-revalidate',
        Pragma: 'no-cache',
        Expires: '0',
      },
    });
  } catch {
    return NextResponse.json(
      { error: 'INTERNAL_ERROR', message: 'Erro de comunicação com o serviço de download.' },
      { status: 500 }
    );
  }
}
