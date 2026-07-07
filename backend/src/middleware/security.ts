import crypto from 'crypto';
import { Request, Response, NextFunction } from 'express';

// ========== JWT 撤销黑名单 ==========

interface RevokedEntry {
  jti: string;
  revokedAt: number;
}

// adminId → revoked jtis
const revocationMap = new Map<string, RevokedEntry[]>();

// 撤销某个 admin 的所有 token（封禁/停用管理员时调用）
export function revokeToken(adminId: string, jti?: string) {
  const entry: RevokedEntry = { jti: jti || '*', revokedAt: Date.now() };
  const list = revocationMap.get(adminId) || [];
  list.push(entry);
  revocationMap.set(adminId, list);
}

// 清除超时记录（定期清理）
setInterval(() => {
  const now = Date.now();
  for (const [adminId, entries] of revocationMap.entries()) {
    const filtered = entries.filter(e => now - e.revokedAt < 24 * 3600_000);
    if (filtered.length === 0) revocationMap.delete(adminId);
    else revocationMap.set(adminId, filtered);
  }
}, 600_000);

export function isTokenRevoked(adminId: string, jti: string): boolean {
  const entries = revocationMap.get(adminId);
  if (!entries) return false;
  return entries.some(e => e.jti === '*' || e.jti === jti);
}

// ========== PoW 挑战（防 LLM 爆破）==========

interface FailedAttempt {
  count: number;
  lastAttempt: number;
  lockedUntil: number;
}

const failedMap = new Map<string, FailedAttempt>();

// 定期清理过期记录（每 10 分钟）
setInterval(() => {
  const now = Date.now();
  for (const [ip, entry] of failedMap.entries()) {
    if (now - entry.lastAttempt > 24 * 3600_000) {
      failedMap.delete(ip);
    }
  }
}, 600_000);

function getLockDuration(failCount: number): number {
  if (failCount >= 20) return 24 * 3600_000;  // 24h
  if (failCount >= 10) return 1 * 3600_000;    // 1h
  if (failCount >= 5) return 15 * 60_000;      // 15min
  return 0;
}

export function getClientIp(req: Request): string {
  return (req.headers['x-forwarded-for'] as string)?.split(',')[0]?.trim()
    || req.socket.remoteAddress
    || 'unknown';
}

export function recordFailedAttempt(ip: string) {
  const entry = failedMap.get(ip) || { count: 0, lastAttempt: 0, lockedUntil: 0 };
  entry.count += 1;
  entry.lastAttempt = Date.now();
  entry.lockedUntil = Date.now() + getLockDuration(entry.count);
  failedMap.set(ip, entry);
}

export function resetFailedAttempts(ip: string) {
  failedMap.delete(ip);
}

export function needsPowChallenge(ip: string): boolean {
  const entry = failedMap.get(ip);
  if (!entry) return false;
  return entry.count >= 3;
}

export function isLocked(ip: string): boolean {
  const entry = failedMap.get(ip);
  if (!entry || entry.lockedUntil <= 0) return false;
  if (Date.now() >= entry.lockedUntil) {
    failedMap.delete(ip);
    return false;
  }
  return true;
}

export function getLockRemaining(ip: string): number {
  const entry = failedMap.get(ip);
  if (!entry) return 0;
  return Math.max(0, Math.ceil((entry.lockedUntil - Date.now()) / 1000));
}

export function generatePowChallenge(): { nonce: string; difficulty: number } {
  return {
    nonce: crypto.randomBytes(16).toString('hex'),
    difficulty: 4,
  };
}

export function verifyPow(nonce: string, answer: string, difficulty: number): boolean {
  const hash = crypto.createHash('sha256').update(nonce + answer).digest('hex');
  return hash.startsWith('0'.repeat(difficulty));
}

// ========== 输入消毒中间件 ==========

function stripHtml(value: any): any {
  if (typeof value === 'string') {
    return value.replace(/<[^>]*>/g, '').replace(/javascript:/gi, '');
  }
  if (Array.isArray(value)) return value.map(stripHtml);
  if (value && typeof value === 'object') {
    const cleaned: any = {};
    for (const key of Object.keys(value)) {
      cleaned[key] = stripHtml(value[key]);
    }
    return cleaned;
  }
  return value;
}

function hasInjectionPattern(value: any): string | null {
  if (typeof value !== 'string') return null;
  const patterns: [RegExp, string][] = [
    // SQL injection: 单引号后跟 SQL 关键词或注释符
    [/'\s*(?:--|or\b|and\b|union\b|select\b|insert\b|update\b|delete\b|drop\b|exec\b)/i, 'SQL injection pattern'],
    // XSS script 标签
    [/<script[\s>]/i, 'XSS script tag'],
    // 危险 SQL 关键词（仅当包含特殊字符时才触发）
    [/(?:\)\s*(?:select|insert|update|delete|drop|union)|;\s*(?:select|insert|update|delete|drop|union))/i, 'SQL keyword detected'],
    // HTML 标签
    [/<\/?[a-z]+[^>]*>/i, 'HTML tag detected'],
  ];
  for (const [regex, desc] of patterns) {
    if (regex.test(value)) return desc;
  }
  return null;
}

export function sanitizeMiddleware(req: Request, _res: Response, next: NextFunction) {
  if (req.body && typeof req.body === 'object') {
    for (const key of Object.keys(req.body)) {
      const detected = hasInjectionPattern(req.body[key]);
      if (detected) {
        return _res.status(400).json({
          code: 400,
          message: `请求包含疑似攻击内容：${detected}`,
        });
      }
    }
    req.body = stripHtml(req.body);
  }
  next();
}

// ========== 注册异常检测 ==========
const loginRecords = new Map<string, { ip: string; ua: string; time: number }>();

export function checkLoginAnomaly(adminId: string, ip: string, ua: string): string | null {
  const prev = loginRecords.get(adminId);
  loginRecords.set(adminId, { ip, ua, time: Date.now() });

  if (!prev) return null;
  const warnings: string[] = [];
  if (prev.ip !== ip) warnings.push(`IP 变更：${prev.ip} → ${ip}`);
  if (prev.ua !== ua) warnings.push('User-Agent 变更');

  return warnings.length > 0 ? warnings.join('; ') : null;
}
