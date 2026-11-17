import type { Metadata, Viewport } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'Seller Intelligence Platform — Inteligência e Governança Comercial',
  description: 'Plataforma SaaS multi-tenant para vendedores de e-commerce e marketplaces (Mercado Livre, Shopee, Amazon e expansão).',
};

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  maximumScale: 5,
  themeColor: '#0a0e17',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="pt-BR" suppressHydrationWarning>
      <body
        suppressHydrationWarning
        className="min-h-screen bg-slate-950 text-slate-100 antialiased selection:bg-blue-600 selection:text-white flex flex-col w-full max-w-full overflow-x-hidden"
      >
        <main className="w-full flex-1 max-w-full overflow-x-hidden">
          {children}
        </main>
      </body>
    </html>
  );
}
