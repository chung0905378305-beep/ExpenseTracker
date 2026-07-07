import { Router, Request, Response } from 'express';
import { z } from 'zod';
import { prisma } from '../prisma';
import { ok, HttpError } from '../utils/response';
import { audit } from '../utils/audit';

const router = Router();

// GET /tags — 所有标签（含使用次数）
router.get('/', async (_req: Request, res: Response) => {
  const tags = await prisma.tag.findMany({
    include: { _count: { select: { members: true } } },
    orderBy: { name: 'asc' },
  });
  return ok(res, tags.map(t => ({
    id: t.id,
    name: t.name,
    color: t.color,
    memberCount: t._count.members,
    createdAt: t.createdAt,
  })));
});

const createSchema = z.object({
  name: z.string().min(1).max(30),
  color: z.string().regex(/^#[0-9a-fA-F]{6}$/).default('#007AFF'),
});

// POST /tags
router.post('/', async (req: Request, res: Response) => {
  const parsed = createSchema.safeParse(req.body);
  if (!parsed.success) throw new HttpError(400, parsed.error.errors[0].message);

  const exists = await prisma.tag.findUnique({ where: { name: parsed.data.name } });
  if (exists) throw new HttpError(409, '标签名称已存在');

  const tag = await prisma.tag.create({ data: parsed.data });
  await audit(req, 'tag_create', tag.id, { name: tag.name });
  return ok(res, tag);
});

// PATCH /tags/:id
router.patch('/:id', async (req: Request, res: Response) => {
  const parsed = z.object({
    name: z.string().min(1).max(30).optional(),
    color: z.string().regex(/^#[0-9a-fA-F]{6}$/).optional(),
  }).safeParse(req.body);
  if (!parsed.success) throw new HttpError(400, parsed.error.errors[0].message);

  const tag = await prisma.tag.update({
    where: { id: req.params.id },
    data: parsed.data,
  });
  await audit(req, 'tag_update', req.params.id, parsed.data);
  return ok(res, tag);
});

// DELETE /tags/:id
router.delete('/:id', async (req: Request, res: Response) => {
  await prisma.tag.delete({ where: { id: req.params.id } });
  await audit(req, 'tag_delete', req.params.id);
  return ok(res, { deleted: req.params.id });
});

// ===== 会员标签操作 =====

// POST /members/:id/tags — 给会员打标签
router.post('/members/:id/tags', async (req: Request, res: Response) => {
  const { tagId } = req.body;
  if (!tagId) throw new HttpError(400, '缺少 tagId');

  const [member, tag] = await Promise.all([
    prisma.member.findUnique({ where: { id: req.params.id } }),
    prisma.tag.findUnique({ where: { id: tagId } }),
  ]);
  if (!member) throw new HttpError(404, '会员不存在');
  if (!tag) throw new HttpError(404, '标签不存在');

  const mt = await prisma.memberTag.create({
    data: { memberId: req.params.id, tagId },
    include: { tag: true },
  });
  await audit(req, 'member_tag_add', req.params.id, { tagId, tagName: tag.name });
  return ok(res, mt);
});

// DELETE /members/:id/tags/:tagId — 移除标签
router.delete('/members/:id/tags/:tagId', async (req: Request, res: Response) => {
  await prisma.memberTag.deleteMany({
    where: { memberId: req.params.id, tagId: req.params.tagId },
  });
  await audit(req, 'member_tag_remove', req.params.id, { tagId: req.params.tagId });
  return ok(res, { removed: true });
});

// GET /members/:id/tags — 获取会员标签
router.get('/members/:id/tags', async (req: Request, res: Response) => {
  const mt = await prisma.memberTag.findMany({
    where: { memberId: req.params.id },
    include: { tag: true },
  });
  return ok(res, mt);
});

// ===== 批量标签操作 =====

// POST /batch-tag — 批量打标签
router.post('/batch-tag', async (req: Request, res: Response) => {
  const { memberIds, tagId } = req.body;
  if (!Array.isArray(memberIds) || memberIds.length === 0) throw new HttpError(400, '缺少 memberIds');
  if (!tagId) throw new HttpError(400, '缺少 tagId');

  const tag = await prisma.tag.findUnique({ where: { id: tagId } });
  if (!tag) throw new HttpError(404, '标签不存在');

  let added = 0;
  for (const memberId of memberIds) {
    try {
      await prisma.memberTag.create({ data: { memberId, tagId } });
      added++;
    } catch { /* 已存在则跳过 */ }
  }
  await audit(req, 'batch_tag', '', { memberCount: memberIds.length, tagId, added });
  return ok(res, { added, total: memberIds.length });
});

export default router;
