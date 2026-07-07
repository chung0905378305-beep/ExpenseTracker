<template>
  <div>
    <div class="toolbar">
      <select class="select" v-model="statusFilter" @change="load" style="max-width: 160px">
        <option value="">全部状态</option>
        <option value="active">有效</option>
        <option value="canceled">已取消</option>
        <option value="expired">已过期</option>
        <option value="paused">暂停</option>
        <option value="pending">待生效</option>
      </select>
      <button class="btn btn-sm" @click="exportXlsx">导出 Excel</button>
    </div>
    <div class="card">
      <table class="tbl">
        <thead>
          <tr>
            <th>会员</th>
            <th>套餐</th>
            <th>状态</th>
            <th>自动续费</th>
            <th>到期</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="s in items" :key="s.id">
            <td>{{ s.member?.nickname || s.member?.appUserId }}</td>
            <td>{{ s.plan?.name }}</td>
            <td><StatusTag :status="s.status" /></td>
            <td>{{ s.autoRenew ? '是' : '否' }}</td>
            <td>{{ s.endDate ? fmtDate(s.endDate) : '—' }}</td>
            <td>
              <button
                v-if="s.status === 'active'"
                class="btn btn-sm btn-danger"
                @click="cancel(s)"
              >
                取消
              </button>
              <button
                v-else-if="s.status === 'canceled'"
                class="btn btn-sm"
                @click="reactivate(s)"
              >
                恢复
              </button>
            </td>
          </tr>
          <tr v-if="!items.length">
            <td colspan="6" class="muted">暂无订阅</td>
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
  const { data } = await api.get('/subscriptions', {
    params: { status: statusFilter.value, page: page.value, pageSize },
  });
  items.value = data.data.items;
  total.value = data.data.total;
}

async function cancel(s: any) {
  if (!confirm('确认取消该订阅？')) return;
  await api.post('/subscriptions/' + s.id + '/cancel');
  load();
}
async function reactivate(s: any) {
  await api.post('/subscriptions/' + s.id + '/reactivate');
  load();
}

function exportXlsx() {
  const token = (api.defaults.headers.common?.['Authorization'] as string) || '';
  window.open(`/api/v1/export/subscriptions?token=${encodeURIComponent(token.replace('Bearer ', ''))}`, '_blank');
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
