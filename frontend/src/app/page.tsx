import { HealthChecker } from '@/components/HealthChecker';

export default function HomePage() {
  return (
    <div className="space-y-8">
      {/* Hero section */}
      <div className="rounded-2xl border border-slate-800 bg-gradient-to-br from-slate-900 via-slate-900/80 to-slate-950 p-8 shadow-2xl">
        <div className="max-w-3xl">
          <span className="inline-block rounded-md bg-blue-500/10 px-2.5 py-1 text-xs font-semibold text-blue-400 border border-blue-500/20">
            Ambiente Local &middot; Monorepo Integrado
          </span>
          <h1 className="mt-4 text-3xl font-extrabold tracking-tight text-white sm:text-4xl">
            Fundação Técnica e Governança do Monólito
          </h1>
          <p className="mt-3 text-base text-slate-300 leading-relaxed">
            Estrutura operacional básica executando monólito modular Spring Boot (Java 21 LTS),
            frontend Next.js (App Router), banco relacional PostgreSQL 18 e mensageria RabbitMQ 4.x.
          </p>
        </div>
      </div>

      {/* Real Health Component */}
      <HealthChecker />

      {/* Infrastructure Matrix Overview */}
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-5">
          <div className="text-xs uppercase font-semibold text-blue-400">Backend Core</div>
          <div className="mt-2 text-lg font-bold text-white">Java 21 / Spring Boot 3.5</div>
          <p className="mt-1 text-xs text-slate-400">Spring Modulith, ArchUnit, Flyway e Bean Validation.</p>
        </div>

        <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-5">
          <div className="text-xs uppercase font-semibold text-emerald-400">Frontend Web</div>
          <div className="mt-2 text-lg font-bold text-white">Next.js 15 / React 19</div>
          <p className="mt-1 text-xs text-slate-400">App Router, TypeScript Strict, Tailwind CSS e Vitest.</p>
        </div>

        <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-5">
          <div className="text-xs uppercase font-semibold text-purple-400">Persistência ACID</div>
          <div className="mt-2 text-lg font-bold text-white">PostgreSQL 18.x</div>
          <p className="mt-1 text-xs text-slate-400">NUMERIC determinístico para finanças e isolamento multi-tenant.</p>
        </div>

        <div className="rounded-xl border border-slate-800 bg-slate-900/40 p-5">
          <div className="text-xs uppercase font-semibold text-amber-400">Mensageria & Filas</div>
          <div className="mt-2 text-lg font-bold text-white">RabbitMQ 4.x / Redis 7</div>
          <p className="mt-1 text-xs text-slate-400">Processamento assíncrono idempotente com Dead-Letter Queues.</p>
        </div>
      </div>
    </div>
  );
}
