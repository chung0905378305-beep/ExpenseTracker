import { Router, Request, Response } from 'express';
import { z } from 'zod';
import { prisma } from '../prisma';
import { ok, HttpError } from '../utils/response';
import { audit } from '../utils/audit';
import { appKeyMiddleware } from '../middleware/auth';

const router = Router();

router.get('/', async (req: Request, res: Response) => {
  const page = Math.max(1, Number(req.query.page) || 1);
  const pageSize = Math.min(100, Math.max(1, Number(req.query.pageSize) || 20));
  const status = req.query.status as string | undefined;
  const memberId = req.query.memberId as string | undefined;

  const where: any = {};
  if (status) where.status = status;
  if (memberId) where.memberId = memberId;

  const [total, items] = await Promise.all([
    prisma.payment.count({ where }),
    prisma.payment.findMany({
      where,
      include: { member: true, plan: true },
      orderBy: { paidAt: 'desc' },
      skip: (page - 1) * pageSize,
      take: pageSize,
    }),
  ]);
  return ok(res, { total, page, pageSize, items });
});

// ========== 创建支付（手动激活）==========
const createSchema = z.object({
  memberId: z.string().min(1),
  planId: z.string().min(1),
  amount: z.number().min(0),
  currency: z.string().default('CNY'),
  provider: z.string().default('manual'),
  providerTxId: z.string().optional(),
});

router.post('/', async (req: Request, res: Response) => {
  const parsed = createSchema.safeParse(req.body);
  if (!parsed.success) throw new HttpError(400, parsed.error.errors[0].message);

  const { memberId, planId, amount, currency, provider, providerTxId } = parsed.data;

  // 校验会员
  const member = await prisma.member.findUnique({ where: { id: memberId } });
  if (!member) throw new HttpError(404, '会员不存在');
  if (member.banned) throw new HttpError(400, '会员已被封禁');

  // 校验套餐
  const plan = await prisma.plan.findUnique({ where: { id: planId } });
  if (!plan) throw new HttpError(404, '套餐不存在');

  const now = new Date();
  const durationDays = plan.interval === 'year' ? 365 : 30;
  const newExpireAt = new Date(
    (member.expireAt && member.expireAt > now ? member.expireAt.getTime() : now.getTime())
    + durationDays * 86400_000
  );

  // 原子操作：创建支付 + 更新会员 + upsert 订阅
  const [payment] = await prisma.$transaction([
    prisma.payment.create({
      data: {
        memberId,
        planId,
        amount,
        currency,
        status: 'success',
        provider,
        providerTxId: providerTxId || null,
        paidAt: now,
      },
    }),
    prisma.member.update({
      where: { id: memberId },
      data: {
        planId,
        status: 'active',
        expireAt: newExpireAt,
        isTrial: false,
      },
    }),
    prisma.subscription.upsert({
      where: { memberId },
      update: {
        planId,
        status: 'active',
        startDate: now,
        endDate: newExpireAt,
        autoRenew: true,
        provider,
      },
      create: {
        memberId,
        planId,
        status: 'active',
        startDate: now,
        endDate: newExpireAt,
        autoRenew: true,
        provider,
      },
    }),
  ]);

  await audit(req, 'payment_create', memberId, {
    planId, amount, planName: plan.name, durationDays, newExpireAt,
  });

  return ok(res, payment);
});

// ========== 退款 ==========
router.post('/:id/refund', async (req: Request, res: Response) => {
  const p = await prisma.payment.findUnique({ where: { id: req.params.id } });
  if (!p) throw new HttpError(404, '支付记录不存在');
  if (p.status === 'refunded') throw new HttpError(400, '该笔已退款');

  const updated = await prisma.payment.update({
    where: { id: req.params.id },
    data: { status: 'refunded', refundedAt: new Date() },
  });
  await audit(req, 'payment_refund', req.params.id, { amount: p.amount });
  return ok(res, updated);
});

// ========== Apple IAP Webhook（骨架）==========
const webhookRouter = Router();

webhookRouter.post('/', async (req: Request, res: Response) => {
  const notification = req.body;

  // Apple Server Notifications v2 格式
  // notificationType: SUBSCRIBED | DID_RENEW | DID_FAIL_TO_RENEW | EXPIRED | REFUND 等
  const type = notification?.notificationType;
  const data = notification?.data;
  const signedTransactionInfo = data?.signedTransactionInfo;

  console.log(`[Apple Webhook] ${type}`, signedTransactionInfo ? 'with transaction' : '');

  // TODO: 生产环境需要验证 JWS 签名
  // TODO: 根据 notificationType 处理：
  //   SUBSCRIBED / DID_RENEW → 激活/续费会员
  //   DID_FAIL_TO_RENEW / EXPIRED → 过期会员
  //   REFUND → 撤销会员

  return ok(res, { received: true, type });
});

export default router;
export { webhookRouter as appleWebhookRouter };
