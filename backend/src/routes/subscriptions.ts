import { Router, Request, Response } from 'express';
import { z } from 'zod';
import { prisma } from '../prisma';
import { ok, HttpError } from '../utils/response';
import { audit } from '../utils/audit';

const router = Router();

router.get('/', async (req: Request, res: Response) => {
  const page = Math.max(1, Number(req.query.page) || 1);
  const pageSize = Math.min(100, Math.max(1, Number(req.query.pageSize) || 20));
  const status = req.query.status as string | undefined;
  const where: any = {};
  if (status) where.status = status;

  const [total, items] = await Promise.all([
    prisma.subscription.count({ where }),
    prisma.subscription.findMany({
      where,
      include: { member: true, plan: true },
      orderBy: { createdAt: 'desc' },
      skip: (page - 1) * pageSize,
      take: pageSize,
    }),
  ]);
  return ok(res, { total, page, pageSize, items });
});

const statusSchema = z.object({
  status: z.enum(['active', 'canceled', 'expired', 'paused', 'pending']),
});

router.patch('/:id/status', async (req: Request, res: Response) => {
  const parsed = statusSchema.safeParse(req.body);
  if (!parsed.success) throw new HttpError(400, '状态值不合法');
  const sub = await prisma.subscription.update({
    where: { id: req.params.id },
    data: { status: parsed.data.status },
    include: { member: true, plan: true },
  });
  await audit(req, 'subscription_status', req.params.id, parsed.data);
  return ok(res, sub);
});

router.post('/:id/cancel', async (req: Request, res: Response) => {
  const sub = await prisma.subscription.update({
    where: { id: req.params.id },
    data: { status: 'canceled', autoRenew: false },
  });
  if (sub.memberId) {
    await prisma.member.update({
      where: { id: sub.memberId },
      data: { status: 'canceled' },
    });
  }
  await audit(req, 'subscription_cancel', req.params.id);
  return ok(res, sub);
});

router.post('/:id/reactivate', async (req: Request, res: Response) => {
  const sub = await prisma.subscription.update({
    where: { id: req.params.id },
    data: { status: 'active', autoRenew: true },
  });
  if (sub.memberId) {
    await prisma.member.update({
      where: { id: sub.memberId },
      data: { status: 'active' },
    });
  }
  await audit(req, 'subscription_reactivate', req.params.id);
  return ok(res, sub);
});

export default router;
