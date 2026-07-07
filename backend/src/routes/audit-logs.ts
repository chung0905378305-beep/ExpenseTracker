import { Router, Request, Response } from 'express';
import { prisma } from '../prisma';
import { ok } from '../utils/response';

const router = Router();

// GET /audit-logs — 分页 + 筛选
router.get('/', async (req: Request, res: Response) => {
  const page = Math.max(1, Number(req.query.page) || 1);
  const pageSize = Math.min(100, Math.max(1, Number(req.query.pageSize) || 30));
  const action = req.query.action as string | undefined;
  const adminId = req.query.adminId as string | undefined;
  const target = req.query.target as string | undefined;
  const startDate = req.query.startDate as string | undefined;
  const endDate = req.query.endDate as string | undefined;

  const where: any = {};
  if (action) where.action = { contains: action };
  if (adminId) where.adminId = adminId;
  if (target) where.target = { contains: target };
  if (startDate || endDate) {
    where.createdAt = {};
    if (startDate) where.createdAt.gte = new Date(startDate);
    if (endDate) where.createdAt.lte = new Date(endDate + 'T23:59:59.999Z');
  }

  const [total, items] = await Promise.all([
    prisma.auditLog.count({ where }),
    prisma.auditLog.findMany({
      where,
      include: { admin: { select: { id: true, name: true, email: true } } },
      orderBy: { createdAt: 'desc' },
      skip: (page - 1) * pageSize,
      take: pageSize,
    }),
  ]);

  return ok(res, { total, page, pageSize, items });
});

// GET /audit-logs/stats — 操作类型统计
router.get('/stats', async (req: Request, res: Response) => {
  const days = Math.min(90, Math.max(1, Number(req.query.days) || 30));

  const since = new Date(Date.now() - days * 86400_000);

  const [total, actionCounts, adminCounts] = await Promise.all([
    prisma.auditLog.count({ where: { createdAt: { gte: since } } }),
    prisma.auditLog.groupBy({
      by: ['action'],
      where: { createdAt: { gte: since } },
      _count: { action: true },
      orderBy: { _count: { action: 'desc' } },
      take: 20,
    }),
    prisma.auditLog.groupBy({
      by: ['adminId'],
      where: { createdAt: { gte: since } },
      _count: { adminId: true },
      orderBy: { _count: { adminId: 'desc' } },
      take: 10,
    }),
  ]);

  const admins = new Map<string, string>();
  if (adminCounts.length > 0) {
    const adminRecords = await prisma.admin.findMany({
      where: { id: { in: adminCounts.map(a => a.adminId).filter(Boolean) as string[] } },
      select: { id: true, name: true, email: true },
    });
    adminRecords.forEach(a => admins.set(a.id, a.name || a.email));
  }

  return ok(res, {
    total,
    days,
    actions: actionCounts.map(a => ({ action: a.action, count: a._count.action })),
    byAdmin: adminCounts.map(a => ({
      adminId: a.adminId,
      name: admins.get(a.adminId || '') || '未知',
      count: a._count.adminId,
    })),
  });
});

// GET /audit-logs/export — 导出 CSV
router.get('/export', async (req: Request, res: Response) => {
  const startDate = req.query.startDate as string | undefined;
  const endDate = req.query.endDate as string | undefined;

  const where: any = {};
  if (startDate || endDate) {
    where.createdAt = {};
    if (startDate) where.createdAt.gte = new Date(startDate);
    if (endDate) where.createdAt.lte = new Date(endDate + 'T23:59:59.999Z');
  }

  const items = await prisma.auditLog.findMany({
    where,
    include: { admin: { select: { name: true, email: true } } },
    orderBy: { createdAt: 'desc' },
    take: 10000,
  });

  const header = '时间,操作者,操作类型,目标,详情,IP\n';
  const rows = items.map(item => {
    const name = item.admin?.name || item.admin?.email || '系统';
    const detail = item.detail ? item.detail.replace(/"/g, '""') : '';
    const ts = new Date(item.createdAt).toISOString();
    return `${ts},"${name}",${item.action},"${item.target || ''}","${detail}","${item.ip || ''}"`;
  }).join('\n');

  res.setHeader('Content-Type', 'text/csv; charset=utf-8');
  res.setHeader('Content-Disposition', `attachment; filename="audit-logs-${new Date().toISOString().slice(0, 10)}.csv"`);
  // UTF-8 BOM for Excel
  res.send('\uFEFF' + header + rows);
});

export default router;
