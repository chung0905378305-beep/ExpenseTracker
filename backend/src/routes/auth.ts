import { Router, Request, Response } from 'express';
import { z } from 'zod';
import bcrypt from 'bcryptjs';
import { prisma } from '../prisma';
import { signToken, signRefreshToken, authMiddleware } from '../middleware/auth';
import {
  getClientIp, isLocked, getLockRemaining,
  needsPowChallenge, generatePowChallenge, verifyPow,
  recordFailedAttempt, resetFailedAttempts, checkLoginAnomaly,
} from '../middleware/security';
import { ok, fail } from '../utils/response';
import { audit } from '../utils/audit';

const router = Router();

// PoW 挑战端点（登录前置）
router.get('/challenge', (req: Request, res: Response) => {
  const ip = getClientIp(req);
  if (isLocked(ip)) {
    return fail(res, 429, `登录频率过高，请在 ${Math.ceil(getLockRemaining(ip) / 60)} 分钟后重试`);
  }
  const required = needsPowChallenge(ip);
  const challenge = generatePowChallenge();
  (req as any).__powChallenge = challenge;
  return ok(res, { required, ...challenge });
});

const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(6),
  powNonce: z.string().optional(),
  powAnswer: z.string().optional(),
});

router.post('/login', async (req: Request, res: Response) => {
  const ip = getClientIp(req);

  // 锁定检查
  if (isLocked(ip)) {
    return fail(res, 429, `登录频率过高，请在 ${Math.ceil(getLockRemaining(ip) / 60)} 分钟后重试`);
  }

  const parsed = loginSchema.safeParse(req.body);
  if (!parsed.success) {
    recordFailedAttempt(ip);
    return fail(res, 400, '参数格式错误');
  }

  // PoW 验证（连续失败 3 次后触发）
  if (needsPowChallenge(ip)) {
    if (!parsed.data.powNonce || !parsed.data.powAnswer) {
      recordFailedAttempt(ip);
      return fail(res, 403, '需要完成人机验证：请先调用 /auth/challenge 获取挑战');
    }
    if (!verifyPow(parsed.data.powNonce, parsed.data.powAnswer, 4)) {
      recordFailedAttempt(ip);
      return fail(res, 403, '人机验证失败');
    }
  }

  const { email, password } = parsed.data;
  const admin = await prisma.admin.findUnique({ where: { email } });
  if (!admin || !admin.active) {
    recordFailedAttempt(ip);
    return fail(res, 401, '邮箱或密码错误');
  }

  const valid = await bcrypt.compare(password, admin.password);
  if (!valid) {
    recordFailedAttempt(ip);
    return fail(res, 401, '邮箱或密码错误');
  }

  // 登录成功：清理失败计数
  resetFailedAttempts(ip);

  // 更新最后登录时间
  await prisma.admin.update({
    where: { id: admin.id },
    data: { lastLogin: new Date() },
  });

  // 异常登录检测
  const ua = req.headers['user-agent'] || 'unknown';
  const anomaly = checkLoginAnomaly(admin.id, ip, ua);
  if (anomaly) {
    await audit(
      { auth: { adminId: admin.id, role: admin.role, jti: 'login' } } as any,
      'login_anomaly', admin.id, { warnings: anomaly }
    );
  }

  const token = signToken({ adminId: admin.id, role: admin.role });
  const refreshToken = signRefreshToken({ adminId: admin.id, role: admin.role });

  await audit(
    { auth: { adminId: admin.id, role: admin.role, jti: 'login' } } as any,
    'admin_login', admin.id, { ip, ua }
  );

  return ok(res, {
    token,
    refreshToken,
    admin: { id: admin.id, email: admin.email, name: admin.name, role: admin.role },
  });
});

// Refresh token 端点
router.post('/refresh', authMiddleware, async (req: Request, res: Response) => {
  // authMiddleware 已验证 token 有效性
  const token = signToken({ adminId: req.auth!.adminId, role: req.auth!.role });
  return ok(res, { token });
});

export default router;
