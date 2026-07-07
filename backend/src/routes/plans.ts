import { Router, Request, Response } from 'express';
import { z } from 'zod';
import { prisma } from '../prisma';
import { ok, HttpError } from '../utils/response';
import { audit } from '../utils/audit';

const router = Router();

/** 预定义功能标签池 —— 所有可用权益 */
export const FEATURE_TAGS = [
  { key: 'basic_transactions', label: '基础记账', category: '核心功能' },
  { key: 'categories', label: '收支分类', category: '核心功能' },
  { key: 'monthly_stats', label: '月度统计', category: '核心功能' },
  { key: 'multi_account', label: '多账户管理', category: '账户' },
  { key: 'unlimited_accounts', label: '无限制账户', category: '账户' },
  { key: 'budget', label: '预算管理', category: '财务管理' },
  { key: 'recurring', label: '循环记账', category: '财务管理' },
  { key: 'subscription_track', label: '订阅管理', category: '财务管理' },
  { key: 'csv_export', label: 'CSV导出', category: '数据' },
  { key: 'data_export', label: '高级数据导出', category: '数据' },
  { key: 'custom_categories', label: '自定义分类', category: '个性化' },
  { key: 'ai_analysis', label: 'AI智能分析', category: '高级功能' },
  { key: 'asset_tracking', label: '资产追踪', category: '高级功能' },
  { key: 'net_worth', label: '净值追踪', category: '高级功能' },
  { key: 'exchange_rates', label: '汇率转换', category: '高级功能' },
  { key: 'icloud_sync', label: 'iCloud同步', category: '高级功能' },
  { key: 'face_id', label: '面部识别锁', category: '安全' },
  { key: 'priority_support', label: '优先客服', category: '服务' },
] as const;

// SQLite: features 存储为 JSON 字符串，对外始终暴露 string[]
function toOutput(plan: any) {
  return {
    ...plan,
    features: typeof plan.features === 'string' ? JSON.parse(plan.features) : plan.features,
  };
}

// 获取可选功能标签池
router.get('/feature-tags', (_req: Request, res: Response) => {
  return ok(res, FEATURE_TAGS);
});

router.get('/', async (_req: Request, res: Response) => {
  const items = await prisma.plan.findMany({
    orderBy: [{ sortOrder: 'asc' }, { createdAt: 'desc' }],
  });
  return ok(res, items.map(toOutput));
});

const createSchema = z.object({
  name: z.string().min(1, '套餐名必填'),
  description: z.string().optional(),
  price: z.number().min(0),
  currency: z.string().default('CNY'),
  interval: z.enum(['month', 'year']).default('month'),
  features: z.array(z.string()).default([]),
  isActive: z.boolean().default(true),
  sortOrder: z.number().int().default(0),
});

router.post('/', async (req: Request, res: Response) => {
  const parsed = createSchema.safeParse(req.body);
  if (!parsed.success) throw new HttpError(400, parsed.error.errors[0].message);
  const data = { ...parsed.data, features: JSON.stringify(parsed.data.features) };
  const plan = await prisma.plan.create({ data });
  await audit(req, 'plan_create', plan.id, parsed.data);
  return ok(res, toOutput(plan));
});

router.get('/:id', async (req: Request, res: Response) => {
  const plan = await prisma.plan.findUnique({ where: { id: req.params.id } });
  if (!plan) throw new HttpError(404, '套餐不存在');
  return ok(res, toOutput(plan));
});

router.patch('/:id', async (req: Request, res: Response) => {
  const existing = await prisma.plan.findUnique({ where: { id: req.params.id } });
  if (!existing) throw new HttpError(404, '套餐不存在');
  const parsed = createSchema.partial().safeParse(req.body);
  if (!parsed.success) throw new HttpError(400, parsed.error.errors[0].message);
  const data: any = { ...parsed.data };
  if (data.features !== undefined) {
    data.features = JSON.stringify(data.features);
  }
  const plan = await prisma.plan.update({
    where: { id: req.params.id },
    data,
  });
  await audit(req, 'plan_update', req.params.id, parsed.data);
  return ok(res, toOutput(plan));
});

// 开关套餐（开启/关闭），保留已有会员和订阅关联
router.patch('/:id/toggle', async (req: Request, res: Response) => {
  const existing = await prisma.plan.findUnique({ where: { id: req.params.id } });
  if (!existing) throw new HttpError(404, '套餐不存在');
  const plan = await prisma.plan.update({
    where: { id: req.params.id },
    data: { isActive: !existing.isActive },
  });
  await audit(req, existing.isActive ? 'plan_disable' : 'plan_enable', req.params.id);
  return ok(res, toOutput(plan), existing.isActive ? '套餐已关闭' : '套餐已开启');
});

export default router;
