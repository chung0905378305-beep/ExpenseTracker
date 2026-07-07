import { Router, Request, Response } from 'express';
import { prisma } from '../prisma';
import { ok } from '../utils/response';

const router = Router();

// GET /analytics/retention — 留存分析
router.get('/retention', async (req: Request, res: Response) => {
  const months = Math.min(12, Math.max(1, Number(req.query.months) || 6));

  // 计算每月新增会员，及后续各月的留存
  const since = new Date();
  since.setMonth(since.getMonth() - months);
  since.setDate(1);
  since.setHours(0, 0, 0, 0);

  const members = await prisma.member.findMany({
    where: { joinedAt: { gte: since } },
    select: { id: true, joinedAt: true, expireAt: true, status: true, banned: true },
    orderBy: { joinedAt: 'asc' },
  });

  // 按月分组
  const byMonth: Record<string, { total: number; active: number; churned: number }> = {};

  const monthKeys: string[] = [];
  for (let i = 0; i < months; i++) {
    const d = new Date(since);
    d.setMonth(d.getMonth() + i);
    monthKeys.push(d.toISOString().slice(0, 7)); // "2026-01"
  }

  for (const key of monthKeys) {
    byMonth[key] = { total: 0, active: 0, churned: 0 };
  }

  const now = Date.now();
  for (const m of members) {
    const key = m.joinedAt.toISOString().slice(0, 7);
    if (!byMonth[key]) continue;
    byMonth[key].total++;
    if (m.banned || m.status === 'canceled' || m.status === 'past_due') {
      byMonth[key].churned++;
    } else if (m.expireAt && new Date(m.expireAt).getTime() > now) {
      byMonth[key].active++;
    } else if (!m.expireAt) {
      byMonth[key].active++;
    } else {
      byMonth[key].churned++;
    }
  }

  const data = monthKeys.map(key => ({
    month: key,
    total: byMonth[key].total,
    active: byMonth[key].active,
    churned: byMonth[key].churned,
    retention: byMonth[key].total > 0
      ? Math.round((byMonth[key].active / byMonth[key].total) * 100)
      : 0,
  }));

  return ok(res, { data, months });
});

// GET /analytics/revenue — 收入分析
router.get('/revenue', async (req: Request, res: Response) => {
  const months = Math.min(12, Math.max(1, Number(req.query.months) || 6));

  const since = new Date();
  since.setMonth(since.getMonth() - months);
  since.setDate(1);

  const payments = await prisma.payment.findMany({
    where: {
      status: 'success',
      paidAt: { gte: since },
    },
    select: { amount: true, paidAt: true },
  });

  const byMonth: Record<string, number> = {};
  for (const p of payments) {
    const key = p.paidAt.toISOString().slice(0, 7);
    byMonth[key] = (byMonth[key] || 0) + p.amount;
  }

  const monthKeys: string[] = [];
  for (let i = 0; i < months; i++) {
    const d = new Date(since);
    d.setMonth(d.getMonth() + i);
    monthKeys.push(d.toISOString().slice(0, 7));
  }

  // 计算 ARPU（假设本月活跃人数用 totalMembers）
  const totalMembers = await prisma.member.count({ where: { banned: false } });

  const data = monthKeys.map(key => ({
    month: key,
    revenue: Math.round((byMonth[key] || 0) * 100) / 100,
    arpu: totalMembers > 0 ? Math.round(((byMonth[key] || 0) / totalMembers) * 100) / 100 : 0,
  }));

  return ok(res, { data, months });
});

export default router;
