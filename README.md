# 记账本 · 会员管理后台

独立 Web 后台，用于管理 iOS App 的付费会员。技术栈：**Vue3 + Node(Express) + SQLite + Prisma**。

## 目录结构

```
ExpenseTracker/
├── backend/      # Node + Express + Prisma 后端 API
│   ├── prisma/   # schema.prisma + seed.ts
│   └── src/      # 路由、中间件、工具
└── web/          # Vue3 + Vite 前端后台
    └── src/
        ├── views/      # 看板/会员/套餐/订阅/支付/设置
        ├── components/ # KpiCard / StatusTag / EChart
        ├── stores/     # Pinia 鉴权
        └── api/        # axios 实例
```

## 环境要求

- Node.js ≥ 18
- **无需 Docker**（使用 SQLite 本地文件数据库，零依赖）

## 快速启动

```bash
# 后端（终端 1）
cd backend
cp .env.example .env            # 按需修改 JWT_SECRET / APP_API_KEY
npm install
npx prisma migrate dev --name init   # 创建 SQLite 数据库
npm run prisma:seed                  # 写入演示数据
npm run dev                          # → http://localhost:4000

# 前端（终端 2）
cd web
npm install
npm run dev                          # → http://localhost:5173
```

默认管理员账号：**admin@expensetracker.app / admin123456**

前端通过 Vite 代理把 `/api` 转发到 `http://localhost:4000`，无需额外配置跨域。

## 与 iOS App 的会员校验集成

App 启动时调用（服务端用 `X-App-Key` 校验）：

```
GET /api/v1/verify/membership?appUserId=<App用户标识>
Header: X-App-Key: <服务端配置的 APP_API_KEY>
```

返回示例：

```json
{
  "code": 0,
  "data": {
    "active": true,
    "plan": { "id": "plan_pro_month", "name": "会员版（月付）", "interval": "month" },
    "isTrial": false,
    "expireAt": "2026-08-01T00:00:00.000Z",
    "autoRenew": true,
    "features": ["自动记账（邮箱/OCR/AI）", "订阅管理", "股票/加密行情", "AI 智能分析", "隐藏金额 / App 锁", "完整 JSON 备份"]
  }
}
```

App 依据返回的 `features` 列表开/关高级功能。

## API 一览

| 分组 | 路径 | 鉴权 |
| --- | --- | --- |
| 鉴权 | `POST /api/v1/auth/login` | 公开（限流） |
| 会员 | `/api/v1/members` (列表/详情/改/封禁/解封) | JWT |
| 套餐 | `/api/v1/plans` (增删改查) | JWT |
| 订阅 | `/api/v1/subscriptions` (列表/改状态/取消/恢复) | JWT |
| 支付 | `/api/v1/payments` (列表/退款) | JWT |
| 看板 | `/api/v1/dashboard` (KPI/趋势/分布) | JWT |
| 校验 | `/api/v1/verify/membership` | App Key |

所有接口统一返回 `{ code, message, data }`，业务错误 `code != 0`。
