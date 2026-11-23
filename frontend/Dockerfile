# Multi-stage Dockerfile for Frontend (Next.js 15 App Router / Standalone Output)
FROM node:22-alpine AS builder
WORKDIR /app

# Instalação do pnpm e dependências
RUN npm install -g pnpm@9
COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

# Cópia do código-fonte e build de produção
COPY . .
ENV NEXT_TELEMETRY_DISABLED=1
ENV NODE_ENV=production
RUN pnpm build

# Runtime minimalista
FROM node:22-alpine AS runner
WORKDIR /app

ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

# Usuário e grupo de execução não-root
RUN addgroup --system --gid 1001 nodejs && adduser --system --uid 1001 nextjs

# Cópia do bundle standalone gerado pelo Next.js
COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs
EXPOSE 3000

CMD ["node", "server.js"]
