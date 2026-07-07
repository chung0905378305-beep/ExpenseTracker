<template>
  <div class="page">
    <div class="page-header">
      <h2>激活码管理</h2>
      <div class="actions">
        <button class="btn btn-primary" @click="dialog = true">+ 批量生成</button>
        <button class="btn" @click="exportCsv" :disabled="exporting">📥 导出未使用</button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row" v-if="stats">
      <div class="stat-card">
        <div class="stat-val">{{ stats.total }}</div>
        <div class="stat-label">总码数</div>
      </div>
      <div class="stat-card unused">
        <div class="stat-val">{{ stats.unused }}</div>
        <div class="stat-label">未使用</div>
      </div>
      <div class="stat-card used">
        <div class="stat-val">{{ stats.used }}</div>
        <div class="stat-label">已使用</div>
      </div>
      <div class="stat-card expired">
        <div class="stat-val">{{ stats.revoked }}</div>
        <div class="stat-label">已撤销</div>
      </div>
    </div>

    <!-- 筛选 -->
    <div class="toolbar">
      <select v-model="filterStatus" @change="fetchList" class="select" style="width:150px">
        <option value="">全部状态</option>
        <option value="unused">未使用</option>
        <option value="used">已使用</option>
        <option value="revoked">已撤销</option>
      </select>
      <input v-model="search" @keyup.enter="fetchList" class="input" style="width:240px" placeholder="搜索激活码…" />
    </div>

    <!-- 表格 -->
    <Skeleton v-if="loading" type="table" :rows="8" />
    <div v-else-if="!list.length" class="empty-state">
      <div class="icon">🎫</div>
      <div class="title">暂无激活码</div>
      <div class="desc">点击"批量生成"创建激活码</div>
    </div>
    <table v-else>
      <thead>
        <tr>
          <th>激活码</th>
          <th>套餐</th>
          <th>天数</th>
          <th>状态</th>
          <th>备注</th>
          <th>过期时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="ac in list" :key="ac.id">
          <td><code>{{ ac.code }}</code></td>
          <td>{{ ac.plan?.name || '-' }}</td>
          <td>{{ ac.duration }} 天</td>
          <td><span :class="statusTag(ac.status)">{{ statusText(ac.status) }}</span></td>
          <td class="text-muted">{{ ac.note || '-' }}</td>
          <td class="text-muted">{{ fmt(ac.expiresAt) }}</td>
          <td>
            <button v-if="ac.status === 'unused'" class="btn btn-danger btn-sm" @click="revoke(ac.id)">撤销</button>
            <span v-else class="text-muted" style="font-size:12px">—</span>
          </td>
        </tr>
      </tbody>
    </table>

    <div class="pager" v-if="total > pageSize">
      <button class="btn btn-sm" :disabled="page <= 1" @click="page--; fetchList()">上一页</button>
      <span>{{ page }} / {{ Math.ceil(total / pageSize) }}</span>
      <button class="btn btn-sm" :disabled="page >= Math.ceil(total / pageSize)" @click="page++; fetchList()">下一页</button>
    </div>

    <!-- 生成弹窗 -->
    <div v-if="dialog" class="modal-overlay" @click.self="dialog = false">
      <div class="modal card">
        <h3>批量生成激活码</h3>
        <label>套餐</label>
        <select v-model="form.planId" class="input">
          <option v-for="p in plans" :key="p.id" :value="p.id">{{ p.name }} ({{ p.interval === 'year' ? '年付' : '月付' }})</option>
        </select>
        <label>会员天数</label>
        <input v-model.number="form.duration" class="input" type="number" min="1" />
        <label>生成数量</label>
        <input v-model.number="form.quantity" class="input" type="number" min="1" max="1000" />
        <label>码有效期（天）</label>
        <input v-model.number="form.validDays" class="input" type="number" min="1" />
        <label>备注</label>
        <input v-model="form.note" class="input" placeholder="如：7月闲鱼-张三" />
        <div class="modal-btns">
          <button class="btn" @click="dialog = false">取消</button>
          <button class="btn btn-primary" :disabled="generating" @click="generate">
            {{ generating ? '生成中…' : '生成' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import api from '@/api';
import { useToastStore } from '@/stores/toast';
import Skeleton from '@/components/Skeleton.vue';

const toast = useToastStore();

const loading = ref(true);
const list = ref<any[]>([]);
const plans = ref<any[]>([]);
const stats = ref<any>(null);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);
const filterStatus = ref('');
const search = ref('');
const dialog = ref(false);
const generating = ref(false);
const exporting = ref(false);

const form = ref({ planId: '', duration: 30, quantity: 10, validDays: 90, note: '' });

function fmt(d: string) { return new Date(d).toLocaleDateString('zh-CN'); }
function statusText(s: string) {
  const map: Record<string, string> = { unused: '未使用', used: '已使用', revoked: '已撤销' };
  return map[s] || s;
}
function statusTag(s: string) {
  const map: Record<string, string> = { unused: 'tag-primary', used: 'tag-success', revoked: 'tag-muted' };
  return `tag ${map[s] || 'tag-muted'}`;
}

async function fetchList() {
  loading.value = true;
  try {
    const { data } = await api.get('/activation-codes', {
      params: { page: page.value, pageSize: pageSize.value, status: filterStatus.value || undefined, search: search.value || undefined },
    });
    list.value = data.data.items;
    total.value = data.data.total;
    stats.value = data.data.stats;
  } catch { toast.error('加载失败'); }
  finally { loading.value = false; }
}

async function fetchPlans() {
  try {
    const { data } = await api.get('/plans');
    plans.value = data.data;
    if (plans.value.length) form.value.planId = plans.value[0].id;
  } catch { /* ignore */ }
}

async function generate() {
  if (!form.value.planId || form.value.quantity < 1) return toast.warning('请填写完整');
  generating.value = true;
  try {
    await api.post('/activation-codes/generate', form.value);
    toast.success(`成功生成 ${form.value.quantity} 张激活码`);
    dialog.value = false;
    fetchList();
  } catch (e: any) {
    toast.error(e.response?.data?.message || '生成失败');
  } finally { generating.value = false; }
}

async function revoke(id: string) {
  if (!confirm('确定撤销该激活码？')) return;
  try {
    await api.patch(`/activation-codes/${id}/revoke`);
    toast.success('已撤销');
    fetchList();
  } catch (e: any) {
    toast.error(e.response?.data?.message || '撤销失败');
  }
}

async function exportCsv() {
  exporting.value = true;
  try {
    const { data } = await api.get('/activation-codes/export/csv', { responseType: 'blob' });
    const url = URL.createObjectURL(new Blob([data], { type: 'text/csv' }));
    const a = document.createElement('a');
    a.href = url;
    a.download = `activation-codes-${Date.now()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    toast.success('导出成功');
  } catch { toast.error('导出失败'); }
  finally { exporting.value = false; }
}

onMounted(() => { fetchPlans(); fetchList(); });
</script>

<style scoped>
.page { max-width: 1200px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; }
.actions { display: flex; gap: 8px; }
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 20px; }
.stat-card {
  background: var(--surface); border-radius: var(--radius); padding: 16px;
  box-shadow: var(--shadow); text-align: center;
}
.stat-val { font-size: 28px; font-weight: 700; color: var(--text); }
.stat-label { font-size: 12px; color: var(--text-3); margin-top: 4px; }
.unused .stat-val { color: var(--primary); }
.used .stat-val { color: var(--success); }
.expired .stat-val { color: var(--danger); }
.toolbar { display: flex; gap: 10px; margin-bottom: 14px; }
code { background: var(--bg); padding: 2px 6px; border-radius: 4px; font-size: 12px; }
.text-muted { color: var(--text-3); font-size: 12px; }
.pager { display: flex; justify-content: center; align-items: center; gap: 12px; margin-top: 16px; font-size: 13px; color: var(--text-2); }

/* Modal */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal { width: 420px; max-height: 90vh; overflow-y: auto; display: flex; flex-direction: column; gap: 8px; animation: fadeIn 0.2s ease; }
.modal h3 { margin: 0 0 8px; font-size: 17px; }
.modal label { font-size: 12px; color: var(--text-2); margin-top: 4px; }
.modal-btns { display: flex; gap: 10px; justify-content: flex-end; margin-top: 16px; }

@media (max-width: 768px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .modal { width: 90%; }
}
</style>
