import swaggerJsdoc from 'swagger-jsdoc';

const options: swaggerJsdoc.Options = {
  definition: {
    openapi: '3.0.0',
    info: {
      title: '记账本 · 会员管理后台 API',
      version: '1.0.0',
      description: 'iOS 记账 App 的会员管理系统 API 文档。管理端需 JWT Bearer Token 鉴权，App 端使用 X-App-Key 鉴权。',
      contact: { name: 'Admin', email: 'admin@expensetracker.app' },
    },
    servers: [
      { url: 'http://localhost:4000', description: '本地开发' },
    ],
    components: {
      securitySchemes: {
        bearerAuth: {
          type: 'http',
          scheme: 'bearer',
          bearerFormat: 'JWT',
          description: '后台管理接口使用，通过 POST /api/v1/auth/login 获取',
        },
        appKeyAuth: {
          type: 'apiKey',
          in: 'header',
          name: 'X-App-Key',
          description: 'App 端接口使用',
        },
      },
      schemas: {
        Error: {
          type: 'object',
          properties: {
            code: { type: 'integer', example: -1 },
            message: { type: 'string', example: '错误信息' },
            data: { type: 'object', nullable: true },
          },
        },
        Pagination: {
          type: 'object',
          properties: {
            total: { type: 'integer' },
            page: { type: 'integer' },
            pageSize: { type: 'integer' },
          },
        },
        Admin: {
          type: 'object',
          properties: {
            id: { type: 'string' },
            email: { type: 'string' },
            name: { type: 'string', nullable: true },
            role: { type: 'string', enum: ['admin', 'superadmin'] },
            active: { type: 'boolean' },
            lastLogin: { type: 'string', nullable: true },
            createdAt: { type: 'string' },
          },
        },
        Member: {
          type: 'object',
          properties: {
            id: { type: 'string' },
            appUserId: { type: 'string' },
            email: { type: 'string', nullable: true },
            nickname: { type: 'string', nullable: true },
            platform: { type: 'string' },
            status: { type: 'string', enum: ['active', 'trialing', 'canceled', 'past_due'] },
            planId: { type: 'string', nullable: true },
            joinedAt: { type: 'string' },
            expireAt: { type: 'string', nullable: true },
            banned: { type: 'boolean' },
            bannedReason: { type: 'string', nullable: true },
          },
        },
        Plan: {
          type: 'object',
          properties: {
            id: { type: 'string' },
            name: { type: 'string' },
            description: { type: 'string', nullable: true },
            price: { type: 'number' },
            currency: { type: 'string' },
            interval: { type: 'string', enum: ['month', 'year'] },
            features: { type: 'array', items: { type: 'string' } },
            isActive: { type: 'boolean' },
            sortOrder: { type: 'integer' },
          },
        },
      },
    },
    paths: {
      '/health': {
        get: { tags: ['系统'], summary: '健康检查', responses: { '200': { description: 'OK' } } },
      },
      '/api/v1/auth/login': {
        post: {
          tags: ['认证'],
          summary: '管理员登录',
          requestBody: {
            required: true,
            content: { 'application/json': { schema: { type: 'object', properties: { email: { type: 'string' }, password: { type: 'string' } }, required: ['email', 'password'] } } },
          },
          responses: {
            '200': { description: '返回 JWT token + refreshToken' },
            '401': { description: '密码错误' },
          },
        },
      },
      '/api/v1/auth/refresh': {
        post: { tags: ['认证'], summary: '刷新 Token', responses: { '200': { description: '返回新 token' } } },
      },
      '/api/v1/admins': {
        get: { tags: ['管理员'], summary: '管理员列表', security: [{ bearerAuth: [] }], responses: { '200': { description: '管理员列表' } } },
        post: { tags: ['管理员'], summary: '创建管理员', security: [{ bearerAuth: [] }], responses: { '200': { description: '创建成功' } } },
      },
      '/api/v1/members': {
        get: {
          tags: ['会员'],
          summary: '会员列表',
          security: [{ bearerAuth: [] }],
          parameters: [
            { name: 'page', in: 'query', schema: { type: 'integer', default: 1 } },
            { name: 'pageSize', in: 'query', schema: { type: 'integer', default: 20 } },
            { name: 'q', in: 'query', description: '搜索关键词' },
            { name: 'status', in: 'query', schema: { type: 'string' } },
            { name: 'banned', in: 'query', schema: { type: 'boolean' } },
          ],
          responses: { '200': { description: '分页会员列表' } },
        },
      },
      '/api/v1/plans': {
        get: { tags: ['套餐'], summary: '套餐列表', security: [{ bearerAuth: [] }], responses: { '200': { description: '套餐列表' } } },
        post: { tags: ['套餐'], summary: '创建套餐', security: [{ bearerAuth: [] }], responses: { '200': { description: '创建成功' } } },
      },
      '/api/v1/dashboard': {
        get: { tags: ['看板'], summary: '运营数据看板', security: [{ bearerAuth: [] }], responses: { '200': { description: 'KPI 数据' } } },
      },
      '/api/v1/subscriptions': {
        get: { tags: ['订阅'], summary: '订阅列表', security: [{ bearerAuth: [] }], responses: { '200': { description: '订阅列表' } } },
      },
      '/api/v1/payments': {
        get: { tags: ['支付'], summary: '支付记录', security: [{ bearerAuth: [] }], responses: { '200': { description: '支付列表' } } },
      },
      '/api/v1/permissions': {
        get: { tags: ['权限'], summary: '权限项列表', security: [{ bearerAuth: [] }], responses: { '200': { description: '权限列表' } } },
      },
      '/api/v1/activation-codes': {
        get: { tags: ['激活码'], summary: '激活码列表', security: [{ bearerAuth: [] }], responses: { '200': { description: '激活码列表' } } },
      },
      '/api/v1/activation-codes/generate': {
        post: { tags: ['激活码'], summary: '生成激活码', security: [{ bearerAuth: [] }], responses: { '200': { description: '生成结果' } } },
      },
      '/api/v1/activate': {
        post: {
          tags: ['激活码'],
          summary: '兑换激活码',
          security: [{ appKeyAuth: [] }],
          requestBody: { content: { 'application/json': { schema: { type: 'object', properties: { code: { type: 'string' }, appUserId: { type: 'string' } }, required: ['code', 'appUserId'] } } } },
          responses: { '200': { description: '兑换成功' }, '400': { description: '激活码无效' } },
        },
      },
      '/api/v1/verify/membership': {
        get: {
          tags: ['App端'],
          summary: '校验会员状态',
          security: [{ appKeyAuth: [] }],
          parameters: [{ name: 'appUserId', in: 'query', required: true, schema: { type: 'string' } }],
          responses: { '200': { description: '会员信息' } },
        },
      },
      '/api/v1/audit-logs': {
        get: { tags: ['审计日志'], summary: '操作日志列表', security: [{ bearerAuth: [] }], responses: { '200': { description: '日志列表' } } },
      },
      '/api/v1/backups': {
        get: { tags: ['备份'], summary: '备份列表', security: [{ bearerAuth: [] }], responses: { '200': { description: '备份列表' } } },
        post: { tags: ['备份'], summary: '创建备份', security: [{ bearerAuth: [] }], responses: { '200': { description: '备份成功' } } },
      },
      '/api/v1/tags': {
        get: { tags: ['标签'], summary: '标签列表', security: [{ bearerAuth: [] }], responses: { '200': { description: '标签列表' } } },
        post: { tags: ['标签'], summary: '创建标签', security: [{ bearerAuth: [] }], responses: { '200': { description: '创建成功' } } },
      },
      '/api/v1/export/members': {
        get: { tags: ['导出'], summary: '导出会员 Excel', security: [{ bearerAuth: [] }], responses: { '200': { description: 'Excel 文件' } } },
      },
      '/api/v1/export/subscriptions': {
        get: { tags: ['导出'], summary: '导出订阅 Excel', security: [{ bearerAuth: [] }], responses: { '200': { description: 'Excel 文件' } } },
      },
      '/api/v1/export/payments': {
        get: { tags: ['导出'], summary: '导出支付 Excel', security: [{ bearerAuth: [] }], responses: { '200': { description: 'Excel 文件' } } },
      },
      '/api/v1/batch/ban': {
        post: { tags: ['批量操作'], summary: '批量封禁', security: [{ bearerAuth: [] }], responses: { '200': { description: '操作结果' } } },
      },
      '/api/v1/batch/unban': {
        post: { tags: ['批量操作'], summary: '批量解封', security: [{ bearerAuth: [] }], responses: { '200': { description: '操作结果' } } },
      },
    },
  },
  apis: [],
};

export const swaggerSpec = swaggerJsdoc(options);
