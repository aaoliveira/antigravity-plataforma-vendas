import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'Seller Intelligence Platform — Fundação Técnica',
  description: 'Plataforma SaaS multi-tenant para vendedores de e-commerce e marketplaces.',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="pt-BR">
      <body className="min-h-screen bg-slate-950 text-slate-100 antialiased selection:bg-blue-600 selection:text-white">
        <header className="border-b border-slate-800 bg-slate-900/40 backdrop-blur sticky top-0 z-50">
          <div className="mx-auto flex max-w-7xl items-center justify-between px-6 py-4">
            <div className="flex items-center gap-3">
              <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-600 font-black text-white shadow-lg shadow-blue-500/20">
                SI
              </div>
              <div>
                <h1 className="text-sm font-bold tracking-tight text-white sm:text-base">Seller Intelligence Platform</h1>
                <p className="text-xs text-slate-400">Fase 1 — MVP / Fundação Técnica</p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <span className="inline-flex items-center gap-1.5 rounded-full border border-blue-500/30 bg-blue-500/10 px-3 py-1 text-xs font-medium text-blue-400">
                <span className="h-2 w-2 rounded-full bg-blue-400 animate-pulse" />
                Prompt 02 — Bootstrap
              </span>
            </div>
          </div>
        </header>

        <main className="mx-auto max-w-7xl px-6 py-8">
          {children}
        </main>

        <footer className="mt-auto border-t border-slate-800 bg-slate-900/20 py-6 text-center text-xs text-slate-500">
          Seller Intelligence Platform &copy; 2026 — Monólito Modular &middot; Java 21 &middot; Next.js 15 &middot; PostgreSQL 18
        </footer>
      </body>
    </html>
  );
}
