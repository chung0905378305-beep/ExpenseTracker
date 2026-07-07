# 记账本 App — 后台会员管理系统设计（BACKEND）

> 独立于 iOS App 的 Web 管理后台。运营人员用它管理 App 的**付费订阅会员**。技术栈：Vue3 + Node + PostgreSQL。

## 1. 目标与定位
- 管理 App 的会员（= App 内订阅用户）、订阅套餐、付费记录，并提供运营数据看板。
- 与 iOS App 通过**会员校验 API** 联动：App 启动时拿收据/会员 Token 调接口，后台返回会员等级、到期日与已解锁功能，App 据此开/关高级功能。
- iOS「我的」页已设计**账户登录卡（登录/注册）+「会员订阅」分组（会员状态 / 管理订阅 / 恢复购买）**：登录后调用 `/membership/verify` 获取权益（`tier/expiresAt/features[]`），在 App 内按 `features[]` 动态解锁高级功能（见 §8 权益映射）。
- 范围（本期）：会员 + 套餐 + 付费 + 看板。（客服工单、内容推送为后续可选，本期不含。）

## 2. 技术架构
- **前端**：Vue 3 + Vite + TypeScript；UI 库 Naive UI（或 Element Plus）；Pinia（状态）+ Vue Router（路由）+ Axios（请求）；图表 ECharts。
- **后端**：Node.js + NestJS（或 Express）+ TypeScript；ORM 用 Prisma；DB PostgreSQL。
- **鉴权**：管理员账号 JWT + 角色（Root / 运营 / 财务）；可选 TOTP 二次验证（2FA）。
- **存储**：PostgreSQL（会员/套餐/订阅/支付/审计）；对象存储（头像/收据截图，可选）。
- **部署**：Docker 容器化；Nginx 反代 + HTTPS；环境变量配置（DB / JWT 密钥 / App 共享密钥）。
- **与 App 集成**：后台暴露 `/api/v1/membership/*` 给 App 端调用（独立鉴权：App 共享密钥 HMAC 签名 + 限流）。

## 3. 数据模型（PostgreSQL / Prisma）
- **Admin**：id, email, passwordHash, role, totpSecret?, lastLoginAt, createdAt
- **Member**：id, appUserId(唯一, App 侧用户标识), email?, displayName?, avatarUrl?, country?, platform(iOS/Android), appVersion?, registeredAt, lastActiveAt, status(active/blocked), note?
- **Plan**（套餐）：id, name, price, currency, interval(month/year), featuresJson(功能键数组), isPublic, sortOrder, createdAt, updatedAt
- **Subscription**：id, memberId, planId, status(trialing/active/canceled/past_due), currentPeriodStart, currentPeriodEnd, cancelAtPeriodEnd(bool), provider(appstore/stripe/手动), providerSubId?, createdAt, updatedAt
- **Payment**：id, memberId, subscriptionId?, amount, currency, status(succeeded/refunded/failed/pending), provider, receiptData?(App Store 收据), transactionId?, createdAt
- **AuditLog**：id, adminId, action, targetType, targetId, detailJson, createdAt

> 索引：Member.appUserId、Subscription(memberId,status)、Payment(memberId,createdAt)、Member.email 检索。

## 4. REST API 设计
### 管理员鉴权
- `POST /admin/login` → {token}
- `POST /admin/2fa` → 校验 TOTP 后发正式 token
- `GET /admin/me` → 当前管理员

### 会员
- `GET /members` → 列表（search/status/platform/注册区间/分页）
- `GET /members/:id` → 详情（资料 + 当前订阅 + 支付历史）
- `PATCH /members/:id/status` → 封禁/解封
- `GET /members/:id/subscriptions` · `GET /members/:id/payments`

### 套餐
- `GET /plans` · `POST /plans` · `PATCH /plans/:id` · `DELETE /plans/:id`

### 订阅
- `GET /subscriptions`（filter: status/plan/区间/provider）
- `PATCH /subscriptions/:id` → 取消 / 退款 / 延长周期

### 支付
- `GET /payments`（filter: status/provider/区间/会员）

### 数据看板
- `GET /stats/overview` → MRR、活跃会员、今日新增、即将到期、退款额
- `GET /stats/trend` → 近 N 日/月 新增、活跃、收入曲线
- `GET /stats/plan-distribution` → 各套餐会员占比

### App 端会员校验（独立密钥）
- `POST /api/v1/membership/verify` { appUserId, transactionId? , receipt? } → { tier, expiresAt, features[], status }
- `POST /api/v1/membership/webhook` → App Store / 支付回调（落 Subscription/Payment，更新状态）

### 审计
- `GET /audit-logs`（filter: admin/action/区间）

## 5. 前端页面（Vue）
1. **登录页**：邮箱+密码；开启 2FA 时第二步输 TOTP。
2. **看板/首页**：KPI 卡片（MRR、活跃会员、今日新增、即将到期、本月退款）+ 趋势图（ECharts 折线）+ 套餐分布（饼图）+ 近期会员/支付流。
3. **会员管理**：表格（头像/名称/邮箱/平台/注册时间/状态/当前套餐）+ 搜索筛选分页；点击行打开**详情抽屉**（资料、订阅时间线、支付记录、封禁/备注操作）。
4. **套餐管理**：CRUD 表格（名称/价格/周期/功能/上架开关/排序）。
5. **订阅管理**：列表（会员/套餐/状态/周期/到期）+ 操作（取消、退款、延长）。
6. **支付记录**：列表（会员/金额/状态/渠道/时间）+ 筛选 + 收据查看。
7. **系统设置**：管理员列表与角色、2FA 配置、审计日志查看。
8. **个人中心**：改密码、退出登录。

## 6. 关键流程
- **新会员**：App 注册 → 首次订阅（App Store 内购/Stripe）→ 回调 `/webhook` → 落 Subscription+Payment → 状态 active。
- **校验联动**：App 启动/恢复购买 → `verify` 返回 features[] → App 解锁对应功能（如 AI 分析、多账户、多源行情）。
- **取消/退款**：后台操作 → Subscription 状态变更 + Payment 标记 → App 下次 `verify` 返回降级，功能收回。
- **看板聚合**：`stats` 实时聚合（量大时落物化视图/定时任务）。
- **封禁**：Member.status=blocked → verify 拒绝 → App 限制登录/功能。

## 7. 安全与合规
- 管理员强密码 + 2FA；所有写操作记 AuditLog；HTTPS 全程；JWT 短时效 + 刷新。
- DB 敏感字段加密；PII 最小化（邮箱脱敏展示）；Cookie/CSRF 防护；API 限流。
- 会员数据遵循《个人信息保护法》：提供导出/删除入口（后台「清除会员数据」）。
- 与 App 通信：HMAC 签名 + 时间戳防重放 + IP 白名单（可选）。

## 8. 会员权益映射（建议，可在套餐功能键配置）
- **免费版**：基础记账、分类/标签、月度预算、基础统计、单账户、iCloud 同步。
- **会员版（解锁）**：AI 分析、无限账户/负债、多源资产行情、邮箱自动记账、隐藏金额、全部 4 套主题、CSV/JSON 备份增强、自定义统计区间。
- App 内按 `features[]` 动态显隐入口与功能。

## 9. 与 iOS App 的接口契约（示意）
```json
// POST /api/v1/membership/verify
// request
{ "appUserId": "u_abc123", "transactionId": "2000...", "platform": "iOS" }
// response
{
  "tier": "pro",
  "expiresAt": "2026-08-07T00:00:00Z",
  "status": "active",
  "features": ["ai_analysis","multi_account","multi_quote","mail_auto","hide_amount","all_themes","custom_range"]
}
```

## 10. 交付物（本期设计阶段）
- 本文档（架构 + 数据模型 + API + 页面 + 权益映射）。
- 看板首页视觉预览（见可视化预览）。
- 后续「开工」后产出：Vue 前端工程 + Node/Nest 后端 + Prisma 迁移 + Docker 编排。
