import type { Metadata } from 'next';
import './globals.css';
import { AuthProvider } from '@/context/AuthContext';

export const metadata: Metadata = {
  title: 'Seller Intelligence Platform',
  description: 'Plataforma SaaS multi-tenant de inteligência de mercado, analytics e gestão para e-commerce.',
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
        className="bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 antialiased min-h-screen w-full max-w-full overflow-x-hidden"
      >
        <AuthProvider>{children}</AuthProvider>
      </body>
    </html>
  );
}
