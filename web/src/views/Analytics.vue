<template>
  <div>
    <div class="toolbar">
      <h2>数据分析</h2>
    </div>

    <div v-if="loading" class="muted">加载中…</div>
    <template v-else>
      <!-- 留存分析 -->
      <div class="card" style="margin-bottom:14px">
        <div class="card-title">留存率分析（近 {{ months }} 月）</div>
        <EChart :option="retentionOption" height="300px" />
      </div>

      <!-- 收入分析 -->
      <div class="card">
        <div class="card-title">收入趋势</div>
        <EChart :option="revenueOption" height="300px" />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import api from '@/api';
import EChart from '@/components/EChart.vue';

const loading = ref(true);
const months = ref(6);
const retentionData = ref<any[]>([]);
const revenueData = ref<any[]>([]);

const retentionOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['新增', '留存', '流失'] },
  grid: { left: 50, right: 30, top: 30, bottom: 30 },
  xAxis: { type: 'category', data: retentionData.value.map((d: any) => d.month) },
  yAxis: { type: 'value' },
  series: [
    { name: '新增', type: 'bar', stack: 'total', data: retentionData.value.map((d: any) => d.total), itemStyle: { color: '#007AFF' } },
    { name: '留存', type: 'bar', stack: 'total', data: retentionData.value.map((d: any) => d.active), itemStyle: { color: '#34C759' } },
    { name: '流失', type: 'bar', stack: 'total', data: retentionData.value.map((d: any) => d.churned), itemStyle: { color: '#FF3B30' } },
  ],
}));

const revenueOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['收入(¥)', 'ARPU(¥)'] },
  grid: { left: 55, right: 55, top: 30, bottom: 30 },
  xAxis: { type: 'category', data: revenueData.value.map((d: any) => d.month) },
  yAxis: [
    { type: 'value', name: '¥' },
    { type: 'value', name: 'ARPU' },
  ],
  series: [
    { name: '收入(¥)', type: 'bar', data: revenueData.value.map((d: any) => d.revenue), itemStyle: { color: '#007AFF' } },
    { name: 'ARPU(¥)', type: 'line', yAxisIndex: 1, smooth: true, data: revenueData.value.map((d: any) => d.arpu), itemStyle: { color: '#FF9500' } },
  ],
}));

onMounted(async () => {
  try {
    const [r1, r2] = await Promise.all([
      api.get('/analytics/retention', { params: { months: months.value } }),
      api.get('/analytics/revenue', { params: { months: months.value } }),
    ]);
    retentionData.value = r1.data.data.data;
    revenueData.value = r2.data.data.data;
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; margin-bottom: 14px; }
.toolbar h2 { font-size: 16px; }
.muted { color: var(--text-3); padding: 40px; text-align: center; }
.card-title { font-size: 14px; font-weight: 600; margin-bottom: 12px; }
</style>
