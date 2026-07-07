import { Router, Request, Response } from 'express';
import { z } from 'zod';
import { prisma } from '../prisma';
import { ok, fail, HttpError } from '../utils/response';
import { PERMISSION_KEYS } from '../constants/permissions';

const router = Router();

const schema = z.object({
  appUserId: z.string().min(1),
});

// App 启动时调用：校验会员状态，返回权益
// GET /api/v1/verify/membership?appUserId=xxx
router.get('/membership', async (req: Request, res: Response) => {
  const parsed = schema.safeParse(req.query);
  if (!parsed.success) {
    return fail(res, 400, '缺少 appUserId');
  }
  const { appUserId } = parsed.data;
  const member = await prisma.member.findUnique({
    where: { appUserId },
    include: { plan: true, subscription: true },
  });

  if (!member || member.banned) {
    return ok(res, {
      active: false,
      reason: member?.banned ? '账号已封禁' : '未找到会员',
      plan: null,
      expireAt: null,
      features: [],
    });
  }

  const now = new Date();
  const expired = member.expireAt ? member.expireAt < now : false;
  const active = (member.status === 'active' || member.status === 'trialing') && !expired;

  // 从套餐取基础 features
  let planFeatures: string[] = [];
  if (member.plan?.features) {
    const raw = typeof member.plan.features === 'string'
      ? JSON.parse(member.plan.features)
      : member.plan.features;
    planFeatures = Array.isArray(raw) ? raw : [];
  }

  // 从权限覆写表计算最终生效权限
  // PERMISSION_KEYS 是英文 key 列表，planFeatures 可能是中文，需要做匹配
  // 为了兼容，如果 planFeatures 里存的是中文，这里无法精确匹配到英文 key
  // 因此 Plan.features 应存储英文 key 或一致的标识符
  // 这里：planFeatures 中符合 PERMISSION_KEYS 的算命中，否则保留原样
  const overrides = await prisma.memberPermission.findMany({
    where: { memberId: member.id },
  });
  const revoked = new Set(overrides.filter(o => o.action === 'revoke').map(o => o.permissionName));
  const granted = new Set(overrides.filter(o => o.action === 'grant').map(o => o.permissionName));

  const baseSet = new Set(planFeatures);
  const features: string[] = [];

  // 从 PERMISSION_KEYS 遍历标准权限
  for (const perm of PERMISSION_KEYS) {
    const inBase = baseSet.has(perm);
    const isRevoked = revoked.has(perm);
    const isGranted = granted.has(perm);
    if ((inBase && !isRevoked) || isGranted) {
      features.push(perm);
    }
  }

  // 补充 planFeatures 中不在标准权限列表里的（可能是自定义的）
  for (const feat of planFeatures) {
    if (!PERMISSION_KEYS.includes(feat as any) && !features.includes(feat)) {
      const isRevoked = revoked.has(feat);
      if (!isRevoked) features.push(feat);
    }
  }

  return ok(res, {
    active,
    reason: expired ? '会员已过期' : 'ok',
    plan: member.plan
      ? { id: member.plan.id, name: member.plan.name, interval: member.plan.interval }
      : null,
    isTrial: member.isTrial,
    expireAt: member.expireAt,
    autoRenew: member.subscription?.autoRenew ?? false,
    features,
  });
});

export default router;
