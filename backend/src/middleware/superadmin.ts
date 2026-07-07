import { Request, Response, NextFunction } from 'express';
import { fail } from '../utils/response';

export function superadminOnly(req: Request, res: Response, next: NextFunction) {
  if (req.auth?.role !== 'superadmin') {
    return fail(res, 403, '仅超级管理员可执行此操作');
  }
  next();
}
