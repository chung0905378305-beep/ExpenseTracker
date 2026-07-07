import { prisma } from '../prisma';
import { Request } from 'express';

export async function audit(
  req: Request,
  action: string,
  target?: string,
  detail?: object,
) {
  try {
    await prisma.auditLog.create({
      data: {
        adminId: req.auth?.adminId,
        action,
        target,
        detail: detail ? JSON.stringify(detail) : undefined,
        ip: req.ip,
      },
    });
  } catch (e) {
    // 审计失败不影响主流程
    console.warn('audit log failed', e);
  }
}
