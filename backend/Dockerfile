# 记账本 · 会员管理后台 Dockerfile
# 多阶段构建：构建 → 运行

# ============ 构建阶段 ============
FROM node:22-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --omit=dev

COPY tsconfig.json ./
COPY prisma ./prisma
RUN npx prisma generate

COPY src ./src
RUN npm run build

# ============ 运行阶段 ============
FROM node:22-alpine
WORKDIR /app
RUN apk add --no-cache sqlite tzdata
ENV TZ=Asia/Shanghai
ENV NODE_ENV=production
ENV PORT=4000

COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/prisma ./prisma

# 数据库文件持久化卷
VOLUME ["/app/prisma/data"]

EXPOSE 4000

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

CMD ["node", "dist/index.js"]
