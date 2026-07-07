<template>
  <div v-if="loading" class="muted">加载中…</div>
  <div v-else>
    <div class="kpi-grid">
      <KpiCard label="MRR（月度经常性收入）" :value="'¥' + fmt(kpi.mrr)" />
      <KpiCard label="活跃会员" :value="kpi.activeMembers" :sub="'总 ' + kpi.totalMembers" />
      <KpiCard label="今日新增" :value="kpi.todayNew" :sub="'本月 ' + kpi.monthNew" />
      <KpiCard label="7 日内到期" :value="kpi.expiringSoon" />
      <KpiCard label="本月退款" :value="'¥' + fmt(kpi.monthRefund)" />
      <KpiCard label="封禁 / 试用" :value="kpi.bannedMembers + ' / ' + kpi.trialingMembers" />
    </div>

    <div class="charts">
      <div class="card">
        <div class="card-title">近 6 月 · 新增会员 & 收入</div>
        <EChart :option="trendOption" height="300px" />
      </div>
      <div class="card">
        <div class="card-title">套餐分布</div>
        <EChart :option="distOption" height="300px" />
      </div>
    </div>

    <div class="card">
      <div class="card-title">近期会员</div>
      <table class="tbl">
        <thead>
          <tr>
            <th>昵称</th>
            <th>平台</th>
            <th>状态</th>
            <th>套餐</th>
            <th>到期</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in recent" :key="m.id">
            <td>{{ m.nickname || m.appUserId }}</td>
            <td>{{ m.platform }}</td>
            <td><StatusTag :status="m.status" /></td>
            <td>{{ m.plan?.name || '—' }}</td>
            <td>{{ m.expireAt ? fmtDate(m.expireAt) : '—' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import api from '@/api';
import KpiCard from '@/components/KpiCard.vue';
import EChart from '@/components/EChart.vue';
import StatusTag from '@/components/StatusTag.vue';

const loading = ref(true);
const kpi = ref<any>({});
const trend = ref<any[]>([]);
const planDistribution = ref<any[]>([]);
const recent = ref<any[]>([]);

const fmt = (n: number) =>
  (n ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
const fmtDate = (s: string) => new Date(s).toLocaleDateString('zh-CN');

const trendOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['新增会员', '收入(¥)'] },
  grid: { left: 50, right: 55, top: 40, bottom: 30 },
  xAxis: { type: 'category', data: trend.value.map((t) => t.month) },
  yAxis: [
    { type: 'value', name: '会员' },
    { type: 'value', name: '¥' },
  ],
  series: [
    {
      name: '新增会员',
      type: 'bar',
      data: trend.value.map((t) => t.newMembers),
      itemStyle: { color: '#007AFF' },
    },
    {
      name: '收入(¥)',
      type: 'line',
      yAxisIndex: 1,
      smooth: true,
      data: trend.value.map((t) => t.revenue),
      itemStyle: { color: '#34C759' },
    },
  ],
}));

const distOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0 },
  series: [
    {
      type: 'pie',
      radius: ['40%', '65%'],
      data: planDistribution.value.map((p) => ({ name: p.name, value: p.count })),
      label: { formatter: '{b}: {c}' },
    },
  ],
}));

onMounted(async () => {
  try {
    const [d, m] = await Promise.all([
      api.get('/dashboard'),
      api.get('/members', { params: { pageSize: 6 } }),
    ]);
    kpi.value = d.data.data.kpi;
    trend.value = d.data.data.trend;
    planDistribution.value = d.data.data.planDistribution;
    recent.value = m.data.data.items;
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.muted {
  color: var(--text-3);
  padding: 40px;
  text-align: center;
}
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}
.charts {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 14px;
  margin-bottom: 16px;
}
.card-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
}
.tbl {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.tbl th,
.tbl td {
  text-align: left;
  padding: 10px 8px;
  border-bottom: 1px solid var(--border);
}
.tbl th {
  color: var(--text-3);
  font-weight: 500;
}
@media (max-width: 900px) {
  .charts {
    grid-template-columns: 1fr;
  }
}
</style>
