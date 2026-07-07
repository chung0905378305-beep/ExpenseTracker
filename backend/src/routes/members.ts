import { Router, Request, Response } from 'express';
import { z } from 'zod';
import { prisma } from '../prisma';
import { ok, HttpError } from '../utils/response';
import { audit } from '../utils/audit';

const router = Router();

router.get('/', async (req: Request, res: Response) => {
  const page = Math.max(1, Number(req.query.page) || 1);
  const pageSize = Math.min(100, Math.max(1, Number(req.query.pageSize) || 20));
  const q = (req.query.q as string)?.trim();
  const status = req.query.status as string | undefined;
  const banned =
    req.query.banned === 'true' ? true : req.query.banned === 'false' ? false : undefined;

  const where: any = {};
  if (q) {
    where.OR = [
      { nickname: { contains: q } },
      { email: { contains: q } },
      { appUserId: { contains: q } },
    ];
  }
  if (status) where.status = status;
  if (banned !== undefined) where.banned = banned;

  const [total, items] = await Promise.all([
    prisma.member.count({ where }),
    prisma.member.findMany({
      where,
      include: { plan: true, subscription: true },
      orderBy: { createdAt: 'desc' },
      skip: (page - 1) * pageSize,
      take: pageSize,
    }),
  ]);
  return ok(res, { total, page, pageSize, items });
});

router.get('/:id', async (req: Request, res: Response) => {
  const m = await prisma.member.findUnique({
    where: { id: req.params.id },
    include: {
      plan: true,
      subscription: true,
      payments: { orderBy: { paidAt: 'desc' }, take: 50 },
    },
  });
  if (!m) throw new HttpError(404, '会员不存在');
  return ok(res, m);
});

const updateSchema = z.object({
  planId: z.string().nullable().optional(),
  status: z.enum(['active', 'trialing', 'canceled', 'past_due']).optional(),
  expireAt: z.string().datetime().nullable().optional(),
  nickname: z.string().optional(),
});

router.patch('/:id', async (req: Request, res: Response) => {
  const parsed = updateSchema.safeParse(req.body);
  if (!parsed.success) throw new HttpError(400, parsed.error.errors[0].message);
  const data: any = { ...parsed.data };
  if (data.expireAt !== undefined) {
    data.expireAt = data.expireAt ? new Date(data.expireAt) : null;
  }
  const m = await prisma.member.update({
    where: { id: req.params.id },
    data,
    include: { plan: true, subscription: true },
  });
  await audit(req, 'member_update', req.params.id, data);
  return ok(res, m);
});

router.post('/:id/ban', async (req: Request, res: Response) => {
  const reason = (req.body?.reason as string) || '违规';
  const m = await prisma.member.update({
    where: { id: req.params.id },
    data: { banned: true, bannedReason: reason },
  });
  await audit(req, 'member_ban', req.params.id, { reason });
  return ok(res, m);
});

router.post('/:id/unban', async (req: Request, res: Response) => {
  const m = await prisma.member.update({
    where: { id: req.params.id },
    data: { banned: false, bannedReason: null },
  });
  await audit(req, 'member_unban', req.params.id);
  return ok(res, m);
});

export default router;
