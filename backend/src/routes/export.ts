import { Router, Request, Response } from 'express';
import { prisma } from '../prisma';
import * as XLSX from 'xlsx';
import { audit } from '../utils/audit';

const router = Router();

// GET /export/members — 导出会员 Excel
router.get('/members', async (req: Request, res: Response) => {
  const status = req.query.status as string | undefined;
  const where: any = {};
  if (status) where.status = status;

  const items = await prisma.member.findMany({
    where,
    include: { plan: { select: { name: true } }, subscription: { select: { status: true, endDate: true } } },
    orderBy: { createdAt: 'desc' },
    take: 50000,
  });

  const rows = items.map(m => ({
    '用户ID': m.appUserId,
    '昵称': m.nickname || '',
    '邮箱': m.email || '',
    '平台': m.platform,
    '状态': m.status,
    '套餐': m.plan?.name || '',
    '订阅状态': m.subscription?.status || '',
    '到期时间': m.expireAt ? new Date(m.expireAt).toLocaleDateString('zh-CN') : '',
    '是否封禁': m.banned ? '是' : '否',
    '封禁原因': m.bannedReason || '',
    '加入时间': new Date(m.joinedAt).toLocaleString('zh-CN'),
  }));

  sendXlsx(res, rows, 'members');
  await audit(req, 'export', 'members', { count: rows.length });
});

// GET /export/subscriptions — 导出订阅
router.get('/subscriptions', async (req: Request, res: Response) => {
  const items = await prisma.subscription.findMany({
    include: { member: { select: { appUserId: true, nickname: true } }, plan: { select: { name: true } } },
    orderBy: { createdAt: 'desc' },
    take: 50000,
  });

  const rows = items.map(s => ({
    '用户ID': s.member?.appUserId || '',
    '昵称': s.member?.nickname || '',
    '套餐': s.plan?.name || '',
    '状态': s.status,
    '开始日期': new Date(s.startDate).toLocaleDateString('zh-CN'),
    '到期日期': s.endDate ? new Date(s.endDate).toLocaleDateString('zh-CN') : '',
    '自动续费': s.autoRenew ? '是' : '否',
    '支付渠道': s.provider || '',
    '创建时间': new Date(s.createdAt).toLocaleString('zh-CN'),
  }));

  sendXlsx(res, rows, 'subscriptions');
  await audit(req, 'export', 'subscriptions', { count: rows.length });
});

// GET /export/payments — 导出支付
router.get('/payments', async (req: Request, res: Response) => {
  const items = await prisma.payment.findMany({
    include: { member: { select: { appUserId: true, nickname: true } }, plan: { select: { name: true } } },
    orderBy: { paidAt: 'desc' },
    take: 50000,
  });

  const rows = items.map(p => ({
    '用户ID': p.member?.appUserId || '',
    '昵称': p.member?.nickname || '',
    '套餐': p.plan?.name || '',
    '金额': p.amount,
    '币种': p.currency,
    '状态': p.status,
    '支付渠道': p.provider || '',
    '交易ID': p.providerTxId || '',
    '支付时间': new Date(p.paidAt).toLocaleString('zh-CN'),
  }));

  sendXlsx(res, rows, 'payments');
  await audit(req, 'export', 'payments', { count: rows.length });
});

function sendXlsx(res: Response, rows: any[], filename: string) {
  const ws = XLSX.utils.json_to_sheet(rows);
  // Auto column width
  const colWidths = Object.keys(rows[0] || {}).map((key) => ({
    wch: Math.max(key.length * 2, 12),
  }));
  ws['!cols'] = colWidths;

  const wb = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(wb, ws, 'Sheet1');
  const buf = XLSX.write(wb, { type: 'buffer', bookType: 'xlsx' });

  res.setHeader('Content-Type', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
  res.setHeader('Content-Disposition', `attachment; filename="${filename}-${new Date().toISOString().slice(0, 10)}.xlsx"`);
  res.send(buf);
}

export default router;
