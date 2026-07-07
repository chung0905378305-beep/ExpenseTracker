import 'dotenv/config';
import 'express-async-errors';
import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import rateLimit from 'express-rate-limit';
import { CONFIG } from './config';
import authRoutes from './routes/auth';
import adminRoutes from './routes/admins';
import memberRoutes from './routes/members';
import planRoutes from './routes/plans';
import subscriptionRoutes from './routes/subscriptions';
import paymentRoutes, { appleWebhookRouter } from './routes/payments';
import dashboardRoutes from './routes/dashboard';
import verifyRoutes from './routes/verify';
import permissionRoutes from './routes/permissions';
import activationCodeRoutes, { activateRouter } from './routes/activation-codes';
import auditLogRoutes from './routes/audit-logs';
import backupRoutes from './routes/backups';
import exportRoutes from './routes/export';
import tagRoutes from './routes/tags';
import batchRoutes from './routes/batch';
import analyticsRoutes from './routes/analytics';
import { authMiddleware, appKeyMiddleware } from './middleware/auth';
import { sanitizeMiddleware } from './middleware/security';
import { fail } from './utils/response';
import { HttpError } from './utils/response';
import swaggerUi from 'swagger-ui-express';
import { swaggerSpec } from './swagger';

const app = express();

// ========== 安全层 ==========

// Helmet: CSP + HSTS + X-Frame + X-Content-Type + Referrer-Policy
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: ["'self'", "cdn.jsdelivr.net", "unpkg.com"],
      styleSrc: ["'self'", "'unsafe-inline'", "cdn.jsdelivr.net", "fonts.googleapis.com"],
      imgSrc: ["'self'", "data:"],
      connectSrc: ["'self'", "ws://localhost:*", "ws://192.168.*:*", "ws://10.*:*", "ws://172.*:*"],
      fontSrc: ["'self'", "fonts.gstatic.com"],
    },
  },
  hsts: { maxAge: 31536000, includeSubDomains: true },
  xFrameOptions: { action: 'deny' },
  xContentTypeOptions: true,
  referrerPolicy: { policy: 'same-origin' },
}));

// CORS（支持局域网访问）
const corsOrigins = CONFIG.webOrigin === '*' ? true : CONFIG.webOrigin.split(',');
app.use(cors({ origin: corsOrigins, credentials: true }));

// Body 限制 + JSON 解析
app.use(express.json({ limit: '1mb' }));

// 输入消毒
app.use(sanitizeMiddleware);

// 通用速率限制
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 600,
  standardHeaders: true,
  legacyHeaders: false,
});
app.use(limiter);

// ========== Routes ==========

// Swagger 文档
app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(swaggerSpec, { customSiteTitle: '记账本会员管理 API' }));

app.get('/health', (_req, res) => res.json({ ok: true, ts: new Date().toISOString() }));

// 登录接口单独限流
const authLimiter = rateLimit({
  windowMs: 10 * 60 * 1000,
  max: 30,
  standardHeaders: true,
  legacyHeaders: false,
});
app.use('/api/v1/auth', authLimiter, authRoutes);

// 后台管理接口：JWT 鉴权
app.use('/api/v1/admins', authMiddleware, adminRoutes);
app.use('/api/v1/members', authMiddleware, memberRoutes);
app.use('/api/v1/plans', authMiddleware, planRoutes);
app.use('/api/v1/subscriptions', authMiddleware, subscriptionRoutes);
app.use('/api/v1/payments', authMiddleware, paymentRoutes);
app.use('/api/v1/dashboard', authMiddleware, dashboardRoutes);
app.use('/api/v1/permissions', authMiddleware, permissionRoutes);
app.use('/api/v1/activation-codes', authMiddleware, activationCodeRoutes);
app.use('/api/v1/audit-logs', authMiddleware, auditLogRoutes);
app.use('/api/v1/backups', authMiddleware, backupRoutes);
app.use('/api/v1/export', authMiddleware, exportRoutes);
app.use('/api/v1/tags', authMiddleware, tagRoutes);
app.use('/api/v1/batch', authMiddleware, batchRoutes);
app.use('/api/v1/analytics', authMiddleware, analyticsRoutes);

// App 端会员校验 + 兑换：App Key 鉴权 + 独立限流
const appLimiter = rateLimit({
  windowMs: 60 * 1000,
  max: 100,
  standardHeaders: true,
  legacyHeaders: false,
});
app.use('/api/v1/verify', appKeyMiddleware, appLimiter, verifyRoutes);
app.use('/api/v1/activate', appLimiter, activateRouter);

// Apple IAP Webhook（公网回调，不需要 App Key）
app.use('/api/v1/webhooks/apple', express.raw({ type: 'application/json' }), (req, res, next) => {
  // raw body 转 JSON 用于 webhook 验证
  try {
    if (Buffer.isBuffer(req.body)) {
      (req as any).body = JSON.parse(req.body.toString());
    }
  } catch { /* ignore parse error */ }
  next();
}, appleWebhookRouter);

// 404
app.use((req, res) => {
  fail(res, 404, `未找到接口：${req.method} ${req.path}`);
});

// 统一错误处理
// eslint-disable-next-line @typescript-eslint/no-unused-vars
app.use((err: Error, _req: express.Request, res: express.Response, _next: express.NextFunction) => {
  if (err instanceof HttpError) {
    return fail(res, err.status, err.message);
  }
  console.error(`[ERROR] ${err.name}: ${err.message}`);
  return fail(res, 500, '服务器内部错误');
});

app.listen(CONFIG.port, '0.0.0.0', () => {
  console.log(`🚀 会员管理后台 API 已启动：http://0.0.0.0:${CONFIG.port}`);
  console.log(`   📋 激活码兑换：/api/v1/activate`);
  console.log(`   🍎 Apple Webhook：/api/v1/webhooks/apple`);
});
