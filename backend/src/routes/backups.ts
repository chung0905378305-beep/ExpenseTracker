import { Router, Request, Response } from 'express';
import { ok, HttpError } from '../utils/response';
import { audit } from '../utils/audit';
import { listBackups, createBackup, deleteBackup, restoreBackup, cleanOldBackups } from '../utils/backup';

const router = Router();

// GET /backups — 列出所有备份
router.get('/', async (_req: Request, res: Response) => {
  const list = listBackups();
  return ok(res, { backups: list, count: list.length });
});

// POST /backups — 手动创建备份
router.post('/', async (req: Request, res: Response) => {
  const result = await createBackup();
  // 清理旧备份，保留最近 14 个
  const cleaned = cleanOldBackups(14);
  await audit(req, 'backup_create', result.name, { size: result.size, cleaned });
  return ok(res, { ...result, cleaned });
});

// DELETE /backups/:name — 删除备份
router.delete('/:name', async (req: Request, res: Response) => {
  const ok2 = deleteBackup(req.params.name);
  if (!ok2) throw new HttpError(404, '备份不存在');
  await audit(req, 'backup_delete', req.params.name);
  return ok(res, { deleted: req.params.name });
});

// POST /backups/:name/restore — 恢复备份
router.post('/:name/restore', async (req: Request, res: Response) => {
  const ok2 = restoreBackup(req.params.name);
  if (!ok2) throw new HttpError(404, '备份不存在');
  await audit(req, 'backup_restore', req.params.name);
  return ok(res, { restored: req.params.name, warning: '数据库已恢复，部分运行中连接可能需要重连' });
});

export default router;
