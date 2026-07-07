<template>
  <div>
    <div class="toolbar">
      <select class="select" v-model="statusFilter" @change="load" style="max-width: 160px">
        <option value="">全部状态</option>
        <option value="success">成功</option>
        <option value="failed">失败</option>
        <option value="refunded">已退款</option>
        <option value="pending">待支付</option>
      </select>
      <button class="btn btn-sm" @click="exportXlsx">导出 Excel</button>
    </div>
    <div class="card">
      <table class="tbl">
        <thead>
          <tr>
            <th>会员</th>
            <th>套餐</th>
            <th>金额</th>
            <th>状态</th>
            <th>渠道</th>
            <th>时间</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in items" :key="p.id">
            <td>{{ p.member?.nickname || p.member?.appUserId }}</td>
            <td>{{ p.plan?.name || '—' }}</td>
            <td>¥{{ p.amount }}</td>
            <td><StatusTag :status="p.status" /></td>
            <td>{{ p.provider || '—' }}</td>
            <td>{{ fmtDate(p.paidAt) }}</td>
            <td>
              <button
                v-if="p.status === 'success'"
                class="btn btn-sm btn-danger"
                @click="refund(p)"
              >
                退款
              </button>
            </td>
          </tr>
          <tr v-if="!items.length">
            <td colspan="7" class="muted">暂无支付记录</td>
          </tr>
        </tbody>
      </table>
      <div class="pager" v-if="total > pageSize">
        <button class="btn btn-sm" :disabled="page <= 1" @click="page--; load()">上一页</button>
        <span>{{ page }} / {{ Math.ceil(total / pageSize) }}</span>
        <button
          class="btn btn-sm"
          :disabled="page >= Math.ceil(total / pageSize)"
          @click="page++; load()"
        >
          下一页
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import api from '@/api';
import StatusTag from '@/components/StatusTag.vue';

const statusFilter = ref('');
const page = ref(1);
const pageSize = 20;
const total = ref(0);
const items = ref<any[]>([]);

const fmtDate = (s: string) => new Date(s).toLocaleDateString('zh-CN');

async function load() {
  const { data } = await api.get('/payments', {
    params: { status: statusFilter.value, page: page.value, pageSize },
  });
  items.value = data.data.items;
  total.value = data.data.total;
}

async function refund(p: any) {
  if (!confirm('确认退款 ¥' + p.amount + '？')) return;
  await api.post('/payments/' + p.id + '/refund');
  load();
}

function exportXlsx() {
  const token = (api.defaults.headers.common?.['Authorization'] as string) || '';
  window.open(`/api/v1/export/payments?token=${encodeURIComponent(token.replace('Bearer ', ''))}`, '_blank');
}

onMounted(load);
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
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
.muted {
  color: var(--text-3);
  text-align: center;
  padding: 16px;
}
.pager {
  display: flex;
  align-items: center;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 12px;
  font-size: 13px;
  color: var(--text-2);
}
</style>
