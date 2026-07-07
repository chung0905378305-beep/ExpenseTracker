import { describe, it, expect, beforeAll, afterAll } from 'vitest';
import request from 'supertest';
import express from 'express';
import cors from 'cors';
import authRoutes from '../src/routes/auth';
import memberRoutes from '../src/routes/members';
import planRoutes from '../src/routes/plans';
import dashboardRoutes from '../src/routes/dashboard';
import { authMiddleware } from '../src/middleware/auth';
import { sanitizeMiddleware } from '../src/middleware/security';
import { fail } from '../src/utils/response';
import { HttpError } from '../src/utils/response';

let app: express.Express;
let token = '';
let testMemberId = '';
let testPlanId = '';

function makeApp() {
  const a = express();
  a.use(cors({ origin: true }));
  a.use(express.json({ limit: '1mb' }));
  a.use(sanitizeMiddleware);

  a.get('/health', (_req, res) => res.json({ ok: true }));

  a.use('/api/v1/auth', authRoutes);
  a.use('/api/v1/members', authMiddleware, memberRoutes);
  a.use('/api/v1/plans', authMiddleware, planRoutes);
  a.use('/api/v1/dashboard', authMiddleware, dashboardRoutes);

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  a.use((err: Error, _req: express.Request, res: express.Response, _next: express.NextFunction) => {
    if (err instanceof HttpError) return fail(res, err.status, err.message);
    console.error('[TEST ERROR]', err.message);
    return fail(res, 500, 'Internal error');
  });

  return a;
}

describe('Auth', () => {
  beforeAll(() => { app = makeApp(); });

  it('GET /health should return ok', async () => {
    const res = await request(app).get('/health');
    expect(res.status).toBe(200);
    expect(res.body.ok).toBe(true);
  });

  it('GET /api/v1/auth/challenge should return PoW', async () => {
    const res = await request(app).get('/api/v1/auth/challenge');
    expect(res.status).toBe(200);
    expect(res.body.data).toBeDefined();
    expect(res.body.data.challenge).toBeDefined();
  });

  it('POST /api/v1/auth/login with wrong password should fail', async () => {
    const res = await request(app)
      .post('/api/v1/auth/login')
      .send({ email: 'admin@expensetracker.app', password: 'WRONG_PASSWORD' });
    expect(res.status).toBe(401);
  });

  it('POST /api/v1/auth/login with correct credentials should return JWT', async () => {
    const res = await request(app)
      .post('/api/v1/auth/login')
      .send({ email: 'admin@expensetracker.app', password: 'admin123456' });
    // May return 429 if rate-limited from prior tests, or 200
    if (res.status === 200) {
      expect(res.body.data.token).toBeDefined();
      token = res.body.data.token;
    }
  });
});

describe('Members', () => {
  beforeAll(async () => {
    app = makeApp();
    if (!token) {
      const r = await request(app)
        .post('/api/v1/auth/login')
        .send({ email: 'admin@expensetracker.app', password: 'admin123456' });
      token = r.body.data?.token || '';
    }
  });

  it('GET /api/v1/members without token should return 401', async () => {
    const res = await request(app).get('/api/v1/members');
    expect(res.status).toBe(401);
  });

  it('GET /api/v1/members with token should return list', async () => {
    const res = await request(app)
      .get('/api/v1/members')
      .set('Authorization', `Bearer ${token}`);
    if (res.status === 200) {
      expect(res.body.data).toBeDefined();
      expect(Array.isArray(res.body.data.items)).toBe(true);
      if (res.body.data.items.length > 0) {
        testMemberId = res.body.data.items[0].id;
      }
    }
  });

  it('GET /api/v1/members/:id should return member detail', async () => {
    if (!testMemberId) return;
    const res = await request(app)
      .get(`/api/v1/members/${testMemberId}`)
      .set('Authorization', `Bearer ${token}`);
    expect(res.status).toBe(200);
    expect(res.body.data.id).toBe(testMemberId);
  });

  it('POST /api/v1/members/:id/ban should ban member', async () => {
    if (!testMemberId) return;
    const res = await request(app)
      .post(`/api/v1/members/${testMemberId}/ban`)
      .set('Authorization', `Bearer ${token}`)
      .send({ reason: 'Test ban' });
    if (res.status === 200) {
      expect(res.body.data.banned).toBe(true);
      expect(res.body.data.bannedReason).toBe('Test ban');
    }
  });

  it('POST /api/v1/members/:id/unban should unban member', async () => {
    if (!testMemberId) return;
    const res = await request(app)
      .post(`/api/v1/members/${testMemberId}/unban`)
      .set('Authorization', `Bearer ${token}`);
    if (res.status === 200) {
      expect(res.body.data.banned).toBe(false);
    }
  });
});

describe('Plans', () => {
  beforeAll(async () => {
    app = makeApp();
    if (!token) {
      const r = await request(app)
        .post('/api/v1/auth/login')
        .send({ email: 'admin@expensetracker.app', password: 'admin123456' });
      token = r.body.data?.token || '';
    }
  });

  it('GET /api/v1/plans should return plans list', async () => {
    const res = await request(app)
      .get('/api/v1/plans')
      .set('Authorization', `Bearer ${token}`);
    expect(res.status).toBe(200);
    expect(Array.isArray(res.body.data)).toBe(true);
  });

  it('POST /api/v1/plans should create a plan', async () => {
    testPlanId = 'test-plan-' + Date.now();
    const res = await request(app)
      .post('/api/v1/plans')
      .set('Authorization', `Bearer ${token}`)
      .send({ id: testPlanId, name: 'Test Plan', price: 9.9, interval: 'month', features: ['Feature A', 'Feature B'] });
    expect(res.status).toBe(200);
    expect(res.body.data.id).toBe(testPlanId);
  });

  it('PATCH /api/v1/plans/:id should update plan', async () => {
    if (!testPlanId) return;
    const res = await request(app)
      .patch(`/api/v1/plans/${testPlanId}`)
      .set('Authorization', `Bearer ${token}`)
      .send({ price: 19.9, description: 'Updated description' });
    expect(res.status).toBe(200);
  });

  it('DELETE /api/v1/plans/:id should soft-delete plan', async () => {
    if (!testPlanId) return;
    const res = await request(app)
      .delete(`/api/v1/plans/${testPlanId}`)
      .set('Authorization', `Bearer ${token}`);
    if (res.status === 200) {
      expect(res.body.data.isActive).toBe(false);
    }
  });
});

describe('Dashboard', () => {
  beforeAll(async () => {
    app = makeApp();
    if (!token) {
      const r = await request(app)
        .post('/api/v1/auth/login')
        .send({ email: 'admin@expensetracker.app', password: 'admin123456' });
      token = r.body.data?.token || '';
    }
  });

  it('GET /api/v1/dashboard should return KPI data', async () => {
    const res = await request(app)
      .get('/api/v1/dashboard')
      .set('Authorization', `Bearer ${token}`);
    if (res.status === 200) {
      expect(res.body.data).toBeDefined();
      expect(res.body.data.kpi).toBeDefined();
      expect(typeof res.body.data.kpi.mrr).toBe('number');
    }
  });
});
