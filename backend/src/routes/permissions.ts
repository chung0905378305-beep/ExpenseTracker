import { Router, Request, Response } from 'express';
import { z } from 'zod';
import { prisma } from '../prisma';
import { ok, HttpError } from '../utils/response';
import { audit } from '../utils/audit';
import { ALL_PERMISSIONS, PERMISSION_KEYS } from '../constants/permissions';

const router = Router();

// 列出所有可用权限名（前端下拉用）
router.get('/', (_req: Request, res: Response) => {
  return ok(res, ALL_PERMISSIONS);
});

// 获取某会员的权限详情（套餐基础 + 覆写 + 最终生效）
router.get('/members/:memberId', async (req: Request, res: Response) => {
  const member = await prisma.member.findUnique({
    where: { id: req.params.memberId },
    include: { plan: true },
  });
  if (!member) throw new HttpError(404, '会员不存在');

  const overrides = await prisma.memberPermission.findMany({
    where: { memberId: member.id },
    include: { admin: { select: { name: true } } },
  });

  // Plan.features 是 JSON 字符串数组或中文数组
  let planFeatures: string[] = [];
  if (member.plan?.features) {
    const raw = typeof member.plan.features === 'string'
      ? JSON.parse(member.plan.features)
      : member.plan.features;
    planFeatures = Array.isArray(raw) ? raw : [];
  }

  // 计算最终生效权限
  const revoked = new Set(overrides.filter(o => o.action === 'revoke').map(o => o.permissionName));
  const granted = new Set(overrides.filter(o => o.action === 'grant').map(o => o.permissionName));

  // Plan 的基础权限映射到英文 key
  // 注意：当前 Plan.features 存的是中文名数组，这里假设后端已做了中文→英文映射
  // 如果没有映射，就用 planFeatures 也能工作（只要 key 一致）
  const baseSet = new Set<string>(planFeatures);
  const effective: string[] = [];
  for (const perm of PERMISSION_KEYS) {
    const inBase = baseSet.has(perm);
    const isRevoked = revoked.has(perm);
    const isGranted = granted.has(perm);
    if ((inBase && !isRevoked) || isGranted) {
      effective.push(perm);
    }
  }

  return ok(res, {
    planId: member.planId,
    planName: member.plan?.name ?? null,
    planFeatures,
    overrides: overrides.map(o => ({
      permissionName: o.permissionName,
      action: o.action,
      reason: o.reason,
      adminName: o.admin?.name ?? null,
      createdAt: o.createdAt,
    })),
    effective,
  });
});

// 覆写权限
const overrideSchema = z.object({
  permissionName: z.string().min(1),
  action: z.enum(['grant', 'revoke']),
  reason: z.string().optional(),
});

router.post('/members/:memberId', async (req: Request, res: Response) => {
  const parsed = overrideSchema.safeParse(req.body);
  if (!parsed.success) throw new HttpError(400, parsed.error.errors[0].message);

  const member = await prisma.member.findUnique({ where: { id: req.params.memberId } });
  if (!member) throw new HttpError(404, '会员不存在');

  const { permissionName, action, reason } = parsed.data;

  const perm = await prisma.memberPermission.upsert({
    where: { memberId_permissionName: { memberId: member.id, permissionName } },
    create: {
      memberId: member.id,
      permissionName,
      action,
      reason: reason ?? null,
      adminId: req.auth!.adminId,
    },
    update: {
      action,
      reason: reason ?? null,
      adminId: req.auth!.adminId,
    },
  });

  await audit(req, `permission_${action}`, member.id, { permissionName, action, reason });
  return ok(res, perm);
});

// 移除覆写（恢复套餐默认）
router.delete('/members/:memberId/:permissionName', async (req: Request, res: Response) => {
  const { memberId, permissionName } = req.params;
  await prisma.memberPermission.deleteMany({
    where: { memberId, permissionName },
  });
  await audit(req, 'permission_reset', memberId, { permissionName });
  return ok(res, null);
});

export default router;
