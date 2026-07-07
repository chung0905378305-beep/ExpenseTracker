import { Router, Request, Response } from 'express';
import { prisma } from '../prisma';
import { ok } from '../utils/response';

const router = Router();

router.get('/', async (_req: Request, res: Response) => {
  const now = new Date();
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
  const in7Days = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000);

  const [
    totalMembers,
    activeMembers,
    trialingMembers,
    bannedMembers,
    todayNew,
    monthNew,
    expiringSoon,
  ] = await Promise.all([
    prisma.member.count(),
    prisma.member.count({ where: { status: 'active', banned: false } }),
    prisma.member.count({ where: { status: 'trialing', banned: false } }),
    prisma.member.count({ where: { banned: true } }),
    prisma.member.count({ where: { createdAt: { gte: startOfToday } } }),
    prisma.member.count({ where: { createdAt: { gte: startOfMonth } } }),
    prisma.member.count({
      where: {
        expireAt: { gte: now, lt: in7Days },
        banned: false,
        status: { in: ['active', 'trialing'] },
      },
    }),
  ]);

  // MRR：活跃订阅折算月费
  const activeSubs = await prisma.subscription.findMany({
    where: { status: 'active' },
    include: { plan: true },
  });
  const mrr = activeSubs.reduce(
    (sum, s) => sum + (s.plan.interval === 'year' ? s.plan.price / 12 : s.plan.price),
    0,
  );

  // 本月退款金额
  const monthRefunds = await prisma.payment.aggregate({
    where: { status: 'refunded', refundedAt: { gte: startOfMonth } },
    _sum: { amount: true },
  });

  // 近 6 月新增会员 + 收入
  const trend: { month: string; newMembers: number; revenue: number }[] = [];
  for (let i = 5; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    const next = new Date(now.getFullYear(), now.getMonth() - i + 1, 1);
    const [nm, rev] = await Promise.all([
      prisma.member.count({ where: { createdAt: { gte: d, lt: next } } }),
      prisma.payment.aggregate({
        where: { status: 'success', paidAt: { gte: d, lt: next } },
        _sum: { amount: true },
      }),
    ]);
    trend.push({
      month: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`,
      newMembers: nm,
      revenue: rev._sum.amount || 0,
    });
  }

  // 套餐分布
  const planDist = await prisma.member.groupBy({
    by: ['planId'],
    where: { planId: { not: null } },
    _count: { _all: true },
  });
  const planMap = await prisma.plan.findMany();
  const planDistribution = planDist.map((p) => {
    const plan = planMap.find((x) => x.id === p.planId);
    return { planId: p.planId, name: plan?.name ?? '未知', count: p._count._all };
  });

  return ok(res, {
    kpi: {
      totalMembers,
      activeMembers,
      trialingMembers,
      bannedMembers,
      todayNew,
      monthNew,
      expiringSoon,
      mrr: Math.round(mrr * 100) / 100,
      monthRefund: monthRefunds._sum.amount || 0,
    },
    trend,
    planDistribution,
  });
});

export default router;
