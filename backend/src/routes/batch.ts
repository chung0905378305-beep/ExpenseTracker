import { Router, Request, Response } from 'express';
import { z } from 'zod';
import { prisma } from '../prisma';
import { ok, HttpError } from '../utils/response';
import { audit } from '../utils/audit';

const router = Router();

const batchSchema = z.object({
  memberIds: z.array(z.string()).min(1).max(200),
});

// POST /batch/ban — 批量封禁
router.post('/ban', async (req: Request, res: Response) => {
  const parsed = batchSchema.safeParse(req.body);
  if (!parsed.success) throw new HttpError(400, parsed.error.errors[0].message);

  const reason = (req.body.reason as string) || '批量封禁';
  const result = await prisma.member.updateMany({
    where: { id: { in: parsed.data.memberIds }, banned: false },
    data: { banned: true, bannedReason: reason },
  });
  await audit(req, 'batch_ban', '', { count: result.count, reason, ids: parsed.data.memberIds.slice(0, 5) });
  return ok(res, { affected: result.count });
});

// POST /batch/unban — 批量解封
router.post('/unban', async (req: Request, res: Response) => {
  const parsed = batchSchema.safeParse(req.body);
  if (!parsed.success) throw new HttpError(400, parsed.error.errors[0].message);

  const result = await prisma.member.updateMany({
    where: { id: { in: parsed.data.memberIds }, banned: true },
    data: { banned: false, bannedReason: null },
  });
  await audit(req, 'batch_unban', '', { count: result.count, ids: parsed.data.memberIds.slice(0, 5) });
  return ok(res, { affected: result.count });
});

// POST /batch/set-plan — 批量修改套餐
router.post('/set-plan', async (req: Request, res: Response) => {
  const parsed = batchSchema.extend({
    planId: z.string().min(1),
  }).safeParse(req.body);
  if (!parsed.success) throw new HttpError(400, parsed.error.errors[0].message);

  const plan = await prisma.plan.findUnique({ where: { id: parsed.data.planId } });
  if (!plan) throw new HttpError(404, '套餐不存在');

  let result = { count: 0 };
  // Use transaction for atomicity
  await prisma.$transaction(async (tx) => {
    const r = await tx.member.updateMany({
      where: { id: { in: parsed.data.memberIds } },
      data: { planId: parsed.data.planId },
    });
    result.count = r.count;
  });

  await audit(req, 'batch_set_plan', parsed.data.planId, { count: result.count, ids: parsed.data.memberIds.slice(0, 5) });
  return ok(res, { affected: result.count });
});

export default router;
