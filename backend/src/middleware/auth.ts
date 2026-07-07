import { Request, Response, NextFunction } from 'express';
import jwt from 'jsonwebtoken';
import crypto from 'crypto';
import { CONFIG } from '../config';
import { fail } from '../utils/response';
import { prisma } from '../prisma';
import { isTokenRevoked, revokeToken } from './security';

export interface AuthPayload {
  adminId: string;
  role: string;
  jti: string;
}

declare global {
  // eslint-disable-next-line @typescript-eslint/no-namespace
  namespace Express {
    interface Request {
      auth?: AuthPayload;
    }
  }
}

export function signToken(payload: { adminId: string; role: string }): string {
  return jwt.sign(
    { adminId: payload.adminId, role: payload.role, jti: crypto.randomUUID() },
    CONFIG.jwtSecret,
    { algorithm: 'HS256', expiresIn: '1h' },
  );
}

export function signRefreshToken(payload: { adminId: string; role: string }): string {
  return jwt.sign(
    { adminId: payload.adminId, role: payload.role, jti: crypto.randomUUID(), type: 'refresh' },
    CONFIG.jwtSecret,
    { algorithm: 'HS256', expiresIn: '7d' },
  );
}

export async function revokeAllTokens(adminId: string) {
  // 标记所有该 admin 的 token 已撤销（通过 admin.active=false 间接实现）
  // JWT 黑名单已通过 jti 级别控制
  revokeToken(adminId);
}

export function authMiddleware(req: Request, res: Response, next: NextFunction) {
  const header = req.headers.authorization;
  if (!header || !header.startsWith('Bearer ')) {
    return fail(res, 401, '未授权：缺少 token');
  }
  const token = header.slice(7);
  try {
    const decoded = jwt.verify(token, CONFIG.jwtSecret, {
      algorithms: ['HS256'],
    }) as AuthPayload & { type?: string };

    // 拒绝 refresh token 用于 API 鉴权
    if (decoded.type === 'refresh') return fail(res, 401, 'token 类型错误');

    // 校验 jwt 格式完整性
    if (!decoded.adminId || !decoded.jti) {
      return fail(res, 401, 'token 格式异常');
    }

    // JWT 黑名单检查（防 token 已被撤销但未过期）
    if (isTokenRevoked(decoded.adminId, decoded.jti)) {
      return fail(res, 401, 'token 已失效，请重新登录');
    }

    req.auth = decoded;

    // 核实管理员仍然有效
    prisma.admin.findUnique({ where: { id: decoded.adminId } }).then(admin => {
      if (!admin || !admin.active) {
        // 标记为撤销，下次请求会因黑名单检查失败
        revokeToken(decoded.adminId, decoded.jti);
      }
    }).catch(() => {});

    next();
  } catch (err: any) {
    if (err?.name === 'TokenExpiredError') {
      return fail(res, 401, 'token 已过期，请重新登录');
    }
    return fail(res, 401, 'token 无效');
  }
}

// App 端会员校验接口鉴权：X-App-Key
export function appKeyMiddleware(req: Request, res: Response, next: NextFunction) {
  const key = req.headers['x-app-key'];
  if (!key || key !== CONFIG.appApiKey) {
    return fail(res, 401, 'App Key 无效');
  }
  next();
}
