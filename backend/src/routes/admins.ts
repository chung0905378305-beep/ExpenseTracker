import { Router, Request, Response } from 'express';
import { z } from 'zod';
import bcrypt from 'bcryptjs';
import { prisma } from '../prisma';
import { ok, HttpError } from '../utils/response';
import { audit } from '../utils/audit';
import { superadminOnly } from '../middleware/superadmin';

const router = Router();

// 列出管理员（仅超管）
router.get('/', superadminOnly, async (_req: Request, res: Response) => {
  const items = await prisma.admin.findMany({
    select: { id: true, email: true, name: true, role: true, active: true, lastLogin: true, createdAt: true },
    orderBy: { createdAt: 'asc' },
  });
  return ok(res, items);
});

// 创建管理员
const createSchema = z.object({
  email: z.string().email('邮箱格式不对'),
  password: z.string().min(8, '密码至少 8 位'),
  name: z.string().min(1, '姓名必填'),
  role: z.enum(['admin', 'superadmin']).default('admin'),
});

router.post('/', superadminOnly, async (req: Request, res: Response) => {
  const parsed = createSchema.safeParse(req.body);
  if (!parsed.success) throw new HttpError(400, parsed.error.errors[0].message);

  const { email, password, name, role } = parsed.data;
  const exist = await prisma.admin.findUnique({ where: { email } });
  if (exist) throw new HttpError(409, '邮箱已被使用');

  const passwordHash = await bcrypt.hash(password, 12);
  const admin = await prisma.admin.create({
    data: { email, password: passwordHash, name, role },
    select: { id: true, email: true, name: true, role: true, active: true, createdAt: true },
  });
  await audit(req, 'admin_create', admin.id, { email, name, role });
  return ok(res, admin);
});

// 修改管理员（角色/激活状态：超管；自己改姓名：本人）
const updateSchema = z.object({
  name: z.string().min(1).optional(),
  role: z.enum(['admin', 'superadmin']).optional(),
  active: z.boolean().optional(),
});

router.patch('/:id', async (req: Request, res: Response) => {
  const parsed = updateSchema.safeParse(req.body);
  if (!parsed.success) throw new HttpError(400, parsed.error.errors[0].message);

  const target = await prisma.admin.findUnique({ where: { id: req.params.id } });
  if (!target) throw new HttpError(404, '管理员不存在');

  const isSuper = req.auth?.role === 'superadmin';
  const isSelf = req.auth?.adminId === req.params.id;

  // 非超管只能改自己的姓名，不能改角色/状态
  if (!isSuper && !isSelf) throw new HttpError(403, '无权操作');

  const { role, active, ...rest } = parsed.data;
  const data: any = { ...rest };

  if (isSuper) {
    if (role !== undefined && target.role === 'superadmin' && role !== 'superadmin') {
      throw new HttpError(400, '不能降级超级管理员');
    }
    if (role !== undefined) data.role = role;
    if (active !== undefined && !isSelf) data.active = active;
  }

  const admin = await prisma.admin.update({
    where: { id: req.params.id },
    data,
    select: { id: true, email: true, name: true, role: true, active: true, lastLogin: true, createdAt: true },
  });
  await audit(req, 'admin_update', req.params.id, data);
  return ok(res, admin);
});

// 删除管理员（仅超管，不能删自己）
router.delete('/:id', superadminOnly, async (req: Request, res: Response) => {
  if (req.auth?.adminId === req.params.id) {
    throw new HttpError(400, '不能删除自己');
  }
  const target = await prisma.admin.findUnique({ where: { id: req.params.id } });
  if (!target) throw new HttpError(404, '管理员不存在');
  if (target.role === 'superadmin') {
    throw new HttpError(400, '不能删除超级管理员');
  }
  await prisma.admin.delete({ where: { id: req.params.id } });
  await audit(req, 'admin_delete', req.params.id, { email: target.email });
  return ok(res, null);
});

// 修改自己的密码
const changePasswordSchema = z.object({
  oldPassword: z.string().min(1, '旧密码必填'),
  newPassword: z.string().min(8, '新密码至少 8 位'),
});

router.post('/change-password', async (req: Request, res: Response) => {
  const parsed = changePasswordSchema.safeParse(req.body);
  if (!parsed.success) throw new HttpError(400, parsed.error.errors[0].message);

  const admin = await prisma.admin.findUnique({ where: { id: req.auth!.adminId } });
  if (!admin) throw new HttpError(404, '管理员不存在');

  const valid = await bcrypt.compare(parsed.data.oldPassword, admin.password);
  if (!valid) throw new HttpError(403, '旧密码不正确');

  const newHash = await bcrypt.hash(parsed.data.newPassword, 12);
  await prisma.admin.update({
    where: { id: admin.id },
    data: { password: newHash },
  });
  await audit(req, 'admin_change_password', admin.id);
  return ok(res, null);
});

// 超管重置他人密码
const resetPasswordSchema = z.object({
  newPassword: z.string().min(8, '新密码至少 8 位'),
});

router.post('/:id/reset-password', superadminOnly, async (req: Request, res: Response) => {
  const parsed = resetPasswordSchema.safeParse(req.body);
  if (!parsed.success) throw new HttpError(400, parsed.error.errors[0].message);

  const target = await prisma.admin.findUnique({ where: { id: req.params.id } });
  if (!target) throw new HttpError(404, '管理员不存在');

  const newHash = await bcrypt.hash(parsed.data.newPassword, 12);
  await prisma.admin.update({
    where: { id: target.id },
    data: { password: newHash },
  });
  await audit(req, 'admin_reset_password', req.params.id, { email: target.email });
  return ok(res, null);
});

export default router;
