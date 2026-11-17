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
        className="min-h-screen bg-slate-950 text-slate-100 antialiased selection:bg-blue-600 selection:text-white flex flex-col"
      >
        {/* Global Master Header */}
        <header className="border-b border-slate-800/80 bg-slate-900/80 backdrop-blur-md sticky top-0 z-50">
          <div className="flex items-center justify-between px-3 sm:px-6 py-2.5">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-tr from-blue-600 via-indigo-600 to-purple-600 font-black text-white shadow-md shadow-blue-500/20 text-sm">
                SI
              </div>
              <div>
                <h1 className="text-sm font-bold tracking-tight text-white sm:text-base leading-tight">
                  Seller Intelligence Platform
                </h1>
                <p className="text-[11px] text-slate-400 hidden sm:block">
                  SaaS Multi-tenant &middot; E-commerce & Marketplaces
                </p>
              </div>
            </div>

            <div className="flex items-center gap-2">
              <span className="inline-flex items-center gap-1.5 rounded-full border border-emerald-500/30 bg-emerald-500/10 px-2.5 py-1 text-[11px] font-medium text-emerald-400">
                <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-pulse" />
                <span className="hidden xs:inline">Fail-Closed Protegido</span>
              </span>
              <span className="inline-flex items-center rounded-full border border-slate-700 bg-slate-800/80 px-2.5 py-1 text-[11px] font-mono text-slate-300">
                v1.2.0
              </span>
            </div>
          </div>
        </header>

        {/* Full-width Responsive Body */}
        <main className="w-full flex-1">
          {children}
        </main>

        {/* Global Master Footer */}
        <footer className="mt-auto border-t border-slate-800/60 bg-slate-900/30 py-4 text-center text-xs text-slate-400">
          <div className="px-4 flex flex-col sm:flex-row items-center justify-between max-w-7xl mx-auto gap-2">
            <span>Seller Intelligence Platform &copy; 2026 &middot; Todos os direitos reservados.</span>
            <span className="text-[11px] text-slate-400">Monólito Modular &middot; Java 25 LTS &middot; Next.js 15 &middot; PostgreSQL 18</span>
          </div>
        </footer>
      </body>
    </html>
  );
}
