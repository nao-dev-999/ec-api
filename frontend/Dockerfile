# ビルドコンテキストはリポジトリルート（backend/batchのDockerfileと合わせる）
# jsdom 30.x(テストで使用)の依存undici 8.xがNode >=22.19.0を要求するため、CI(ci.yml)と
# 合わせてNode 22を使う(Node 20は既にLTS終了)。
FROM node:22-alpine AS deps
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --ignore-scripts

FROM node:22-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY frontend ./

# NEXT_PUBLIC_*はクライアントバンドルにビルド時に埋め込まれるため、
# 実行時の環境変数では切り替えられない(buildspec.ymlでビルド引数として渡す)
ARG NEXT_PUBLIC_API_BASE_URL
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL

# public/はこのアプリに静的アセットが無く未作成のため、無い場合でもCOPYが失敗しないよう保証する
RUN mkdir -p public

RUN npm run build

# 実行ステージ: next.config.tsのoutput: "standalone"が生成する最小ランタイムのみを含める
FROM node:22-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME=0.0.0.0

RUN addgroup --system --gid 1001 nodejs && adduser --system --uid 1001 nextjs

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs
EXPOSE 3000
ENTRYPOINT ["node", "server.js"]
