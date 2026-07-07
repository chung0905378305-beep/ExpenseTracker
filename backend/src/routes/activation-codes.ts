import { Router, Request, Response } from 'express';
import { z } from 'zod';
import crypto from 'crypto';
import { prisma } from '../prisma';
import { ok, HttpError } from '../utils/response';
import { audit } from '../utils/audit';
import { appKeyMiddleware } from '../middleware/auth';

const router = Router();

// ========== 生成激活码 ==========
function generateCode(): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; // 去掉易混淆的 0/O/1/I
  const segments = [0, 0, 0].map(() => {
    let seg = '';
    for (let i = 0; i < 4; i++) {
      seg += chars[crypto.randomInt(chars.length)];
    }
    return seg;
  });
  return segments.join('-');
}

const generateSchema = z.object({
  planId: z.string().min(1),
  duration: z.number().int().min(1),
  quantity: z.number().int().min(1).max(1000),
  note: z.string().optional(),
  validDays: z.number().int().min(1).default(30), // 码的有效天数（未兑换则过期）
});

// 批量生成激活码
router.post('/generate', async (req: Request, res: Response) => {
  const parsed = generateSchema.safeParse(req.body);
  if (!parsed.success) throw new HttpError(400, parsed.error.errors[0].message);

  const { planId, duration, quantity, note, validDays } = parsed.data;

  // 校验套餐存在
  const plan = await prisma.plan.findUnique({ where: { id: planId } });
  if (!plan) throw new HttpError(404, '套餐不存在');

  const codes: any[] = [];
  const expiresAt = new Date(Date.now() + validDays * 86400_000);

  // 批量生成，防碰撞
  for (let i = 0; i < quantity; i++) {
    let code: string;
    let attempts = 0;
    do {
      code = generateCode();
      const exists = await prisma.activationCode.findUnique({ where: { code } });
      if (!exists) break;
      attempts++;
    } while (attempts < 5);
    if (attempts >= 5) throw new HttpError(500, '生成失败，请重试');

    const ac = await prisma.activationCode.create({
      data: { code, planId, duration, note, expiresAt },
    });
    codes.push(ac);
  }

  await audit(req, 'activation_code_generate', planId, { quantity, duration, note, validDays });
  return ok(res, { count: codes.length, codes });
});

// ========== 查询激活码列表 ==========
router.get('/', async (req: Request, res: Response) => {
  const page = Math.max(1, Number(req.query.page) || 1);
  const pageSize = Math.min(100, Math.max(1, Number(req.query.pageSize) || 20));
  const status = req.query.status as string | undefined;
  const search = req.query.search as string | undefined;

  const where: any = {};
  if (status) where.status = status;
  if (search) where.code = { contains: search.toUpperCase().replace(/\s/g, '') };

  const [total, items] = await Promise.all([
    prisma.activationCode.count({ where }),
    prisma.activationCode.findMany({
      where,
      include: { plan: { select: { id: true, name: true, interval: true } } },
      orderBy: { createdAt: 'desc' },
      skip: (page - 1) * pageSize,
      take: pageSize,
    }),
  ]);

  // 统计
  const [totalAll, used, revoked, expired] = await Promise.all([
    prisma.activationCode.count(),
    prisma.activationCode.count({ where: { status: 'used' } }),
    prisma.activationCode.count({ where: { status: 'revoked' } }),
    prisma.activationCode.count({
      where: { status: 'unused', expiresAt: { lt: new Date() } },
    }),
  ]);

  return ok(res, {
    total, page, pageSize, items,
    stats: { total: totalAll, used, revoked, expired, unused: totalAll - used - revoked - expired },
  });
});

// ========== 撤销激活码 ==========
router.patch('/:id/revoke', async (req: Request, res: Response) => {
  const ac = await prisma.activationCode.findUnique({ where: { id: req.params.id } });
  if (!ac) throw new HttpError(404, '激活码不存在');
  if (ac.status !== 'unused') throw new HttpError(400, '该码已使用或已撤销');

  const updated = await prisma.activationCode.update({
    where: { id: req.params.id },
    data: { status: 'revoked' },
  });
  await audit(req, 'activation_code_revoke', req.params.id, { code: ac.code });
  return ok(res, updated);
});

// ========== 导出 CSV ==========
router.get('/export/csv', async (req: Request, res: Response) => {
  const codes = await prisma.activationCode.findMany({
    where: { status: 'unused' },
    include: { plan: { select: { name: true } } },
    orderBy: { createdAt: 'desc' },
  });
  const header = '激活码,套餐,有效期(天),备注,过期时间\n';
  const rows = codes.map(c =>
    `${c.code},${c.plan.name},${c.duration},${c.note || ''},${c.expiresAt.toISOString()}`
  ).join('\n');
  res.setHeader('Content-Type', 'text/csv; charset=utf-8');
  res.setHeader('Content-Disposition', `attachment; filename=activation-codes-${Date.now()}.csv`);
  // 写 BOM 确保 Excel 识别中文
  res.write('\uFEFF');
  return res.send(header + rows);
});

// ========== App 端：兑换激活码 ==========
const redeemSchema = z.object({
  appUserId: z.string().min(1),
  code: z.string().min(1),
});

const verifyRouter = Router();
verifyRouter.use(appKeyMiddleware);

verifyRouter.post('/', async (req: Request, res: Response) => {
  const parsed = redeemSchema.safeParse(req.body);
  if (!parsed.success) return ok(res, { success: false, message: '参数格式错误' });

  const { appUserId, code } = parsed.data;
  const normalizedCode = code.toUpperCase().replace(/\s/g, '');

  // 查找激活码
  const ac = await prisma.activationCode.findUnique({
    where: { code: normalizedCode },
    include: { plan: true },
  });
  if (!ac) return ok(res, { success: false, message: '兑换码不存在' });
  if (ac.status === 'used') return ok(res, { success: false, message: '兑换码已被使用' });
  if (ac.status === 'revoked') return ok(res, { success: false, message: '兑换码已失效' });
  if (new Date() > ac.expiresAt) {
    // 自动过期
    await prisma.activationCode.update({ where: { id: ac.id }, data: { status: 'revoked' } });
    return ok(res, { success: false, message: '兑换码已过期' });
  }

  // 查找或创建会员
  let member = await prisma.member.findUnique({ where: { appUserId } });
  if (member?.banned) return ok(res, { success: false, message: '账号已被封禁，无法兑换' });

  // 计算新的到期时间
  const now = new Date();
  let newExpireAt: Date;
  if (member && member.expireAt && member.expireAt > now) {
    // 已有有效会员 → 叠加天数
    newExpireAt = new Date(member.expireAt.getTime() + ac.duration * 86400_000);
  } else {
    newExpireAt = new Date(now.getTime() + ac.duration * 86400_000);
  }

  // 原子操作：标记码为已用 + 创建/更新会员
  const [updatedCode, upsertedMember] = await prisma.$transaction([
    prisma.activationCode.update({
      where: { id: ac.id, status: 'unused' }, // 乐观锁
      data: { status: 'used', usedBy: appUserId, usedAt: now },
    }),
    prisma.member.upsert({
      where: { appUserId },
      update: {
        planId: ac.planId,
        status: 'active',
        expireAt: newExpireAt,
        isTrial: false,
      },
      create: {
        appUserId,
        planId: ac.planId,
        status: 'active',
        platform: 'iOS',
        expireAt: newExpireAt,
        isTrial: false,
      },
    }),
  ]);

  // 创建/更新订阅和支付（使用确定的 memberId）
  const memberId = upsertedMember.id;
  await Promise.all([
    prisma.subscription.upsert({
      where: { memberId },
      update: {
        planId: ac.planId,
        status: 'active',
        startDate: now,
        endDate: newExpireAt,
        autoRenew: false,
        provider: 'activation_code',
      },
      create: {
        memberId,
        planId: ac.planId,
        status: 'active',
        startDate: now,
        endDate: newExpireAt,
        autoRenew: false,
        provider: 'activation_code',
      },
    }),
    prisma.payment.create({
      data: {
        memberId,
        planId: ac.planId,
        amount: 0,
        currency: 'CNY',
        status: 'success',
        provider: 'activation_code',
        providerTxId: ac.code,
        paidAt: now,
      },
    }),
  ]);

  return ok(res, {
    success: true,
    message: '兑换成功',
    planName: ac.plan.name,
    expireAt: newExpireAt,
    duration: ac.duration,
  });
});

export { verifyRouter as activateRouter };
export default router;
