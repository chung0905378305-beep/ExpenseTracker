require('dotenv/config');
const { PrismaClient } = require('@prisma/client');
const bcrypt = require('bcryptjs');

const prisma = new PrismaClient();

async function main() {
  // 演示管理员
  const adminEmail = 'admin@expensetracker.app';
  const adminPwd = await bcrypt.hash('admin123456', 10);
  await prisma.admin.upsert({
    where: { email: adminEmail },
    update: {},
    create: {
      email: adminEmail,
      password: adminPwd,
      name: '超级管理员',
      role: 'superadmin',
      active: true,
    },
  });

  // 套餐：免费版 / 会员版(月) / 会员版(年)
  const free = await prisma.plan.upsert({
    where: { id: 'plan_free' },
    update: {},
    create: {
      id: 'plan_free',
      name: '免费版',
      description: '基础记账与资产管理',
      price: 0,
      interval: 'month',
      features: JSON.stringify([
        'basic_transactions',
        'categories',
        'monthly_stats',
        'csv_export',
        'custom_categories',
        'multi_account',
      ]),
      sortOrder: 0,
    },
  });

  const proMonthly = await prisma.plan.upsert({
    where: { id: 'plan_pro_month' },
    update: {},
    create: {
      id: 'plan_pro_month',
      name: '会员版（月付）',
      description: '解锁全部高级功能',
      price: 30,
      interval: 'month',
      features: JSON.stringify([
        'basic_transactions',
        'categories',
        'monthly_stats',
        'multi_account',
        'budget',
        'recurring',
        'subscription_track',
        'csv_export',
        'data_export',
        'custom_categories',
        'face_id',
        'exchange_rates',
        'priority_support',
      ]),
      sortOrder: 1,
    },
  });

  const proYearly = await prisma.plan.upsert({
    where: { id: 'plan_pro_year' },
    update: {},
    create: {
      id: 'plan_pro_year',
      name: '会员版（年付）',
      description: '更划算的年度会员',
      price: 298,
      interval: 'year',
      features: JSON.stringify([
        'basic_transactions',
        'categories',
        'monthly_stats',
        'multi_account',
        'unlimited_accounts',
        'budget',
        'recurring',
        'subscription_track',
        'csv_export',
        'data_export',
        'custom_categories',
        'ai_analysis',
        'asset_tracking',
        'net_worth',
        'exchange_rates',
        'icloud_sync',
        'face_id',
        'priority_support',
      ]),
      sortOrder: 2,
    },
  });

  // 演示会员
  const demoMembers = [
    {
      appUserId: 'demo_user_001',
      nickname: '小明',
      email: 'xiaoming@example.com',
      platform: 'iOS',
      status: 'active',
      planId: proMonthly.id,
      isTrial: false,
      expireAt: new Date(Date.now() + 1000 * 60 * 60 * 24 * 20),
    },
    {
      appUserId: 'demo_user_002',
      nickname: '小红',
      email: 'xiaohong@example.com',
      platform: 'iOS',
      status: 'trialing',
      planId: proMonthly.id,
      isTrial: true,
      expireAt: new Date(Date.now() + 1000 * 60 * 60 * 24 * 5),
    },
    {
      appUserId: 'demo_user_003',
      nickname: '阿强',
      platform: 'Android',
      status: 'active',
      planId: proYearly.id,
      expireAt: new Date(Date.now() + 1000 * 60 * 60 * 24 * 300),
    },
  ];

  for (const m of demoMembers) {
    const member = await prisma.member.upsert({
      where: { appUserId: m.appUserId },
      update: {},
      create: { ...m },
    });

    // 订阅
    await prisma.subscription.upsert({
      where: { memberId: member.id },
      update: {},
      create: {
        memberId: member.id,
        planId: m.planId,
        status: 'active',
        endDate: m.expireAt,
        autoRenew: !m.isTrial,
        provider: 'appstore',
      },
    });

    // 支付记录
    await prisma.payment.create({
      data: {
        memberId: member.id,
        planId: m.planId,
        amount: m.planId === proYearly.id ? 298 : 30,
        status: 'success',
        provider: 'appstore',
        providerTxId: `tx_${m.appUserId}`,
      },
    });
  }

  // 演示激活码（未使用）
  const demoCodes = [
    { code: 'DEMO-CODE-0001', planId: proMonthly.id, duration: 30, note: '演示激活码', expiresAt: new Date(Date.now() + 90 * 86400_000) },
    { code: 'DEMO-CODE-0002', planId: proYearly.id, duration: 365, note: '演示激活码', expiresAt: new Date(Date.now() + 90 * 86400_000) },
  ];
  for (const c of demoCodes) {
    await prisma.activationCode.upsert({
      where: { code: c.code },
      update: {},
      create: c,
    });
  }

  console.log('Seed complete: 1 admin + 3 plans + 3 members (with subs & payments) + 2 demo activation codes');
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
