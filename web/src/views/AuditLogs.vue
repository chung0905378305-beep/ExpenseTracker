<template>
  <div>
    <!-- 统计卡片 -->
    <div class="kpi-grid" v-if="stats">
      <div class="kpi-card">
        <div class="kpi-value">{{ stats.total }}</div>
        <div class="kpi-label">{{ stats.days }}天操作总数</div>
      </div>
      <div class="kpi-card" v-for="a in stats.actions.slice(0, 4)" :key="a.action">
        <div class="kpi-value">{{ a.count }}</div>
        <div class="kpi-label">{{ actLabel(a.action) }}</div>
      </div>
    </div>

    <!-- 筛选 -->
    <div class="card" style="margin-bottom:14px">
      <div class="filters">
        <input v-model="filters.action" placeholder="操作类型" class="inp" />
        <input v-model="filters.target" placeholder="目标ID" class="inp" />
        <input type="date" v-model="filters.startDate" class="inp" />
        <input type="date" v-model="filters.endDate" class="inp" />
        <button class="btn btn-sm" @click="search">搜索</button>
        <button class="btn btn-sm btn-hollow" @click="exportCsv">导出 CSV</button>
      </div>
    </div>

    <!-- 日志表格 -->
    <div class="card">
      <div v-if="loading" class="muted">加载中…</div>
      <template v-else>
        <table class="tbl">
          <thead>
            <tr>
              <th style="width:160px">时间</th>
              <th>操作者</th>
              <th>操作</th>
              <th>目标</th>
              <th style="width:100px">IP</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="item.id" @click="selected = item" class="clickable">
              <td class="ts">{{ fmtTs(item.createdAt) }}</td>
              <td>{{ item.admin?.name || item.admin?.email || '系统' }}</td>
              <td><span class="tag" :class="'tag-' + actionClass(item.action)">{{ actLabel(item.action) }}</span></td>
              <td class="mono">{{ item.target || '—' }}</td>
              <td class="mono">{{ item.ip || '—' }}</td>
            </tr>
            <tr v-if="items.length === 0">
              <td colspan="5" class="muted">暂无操作日志</td>
            </tr>
          </tbody>
        </table>

        <div class="pager" v-if="total > pageSize">
          <button class="btn btn-sm" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
          <span class="pg-info">{{ page }} / {{ Math.ceil(total / pageSize) }}</span>
          <button class="btn btn-sm" :disabled="page * pageSize >= total" @click="goPage(page + 1)">下一页</button>
        </div>
      </template>
    </div>

    <!-- 详情弹窗 -->
    <div class="overlay" v-if="selected" @click.self="selected = null">
      <div class="modal">
        <h3>操作详情</h3>
        <div class="detail-grid">
          <div><b>时间</b><span>{{ fmtTs(selected.createdAt) }}</span></div>
          <div><b>操作者</b><span>{{ selected.admin?.name || selected.admin?.email || '系统' }}</span></div>
          <div><b>操作类型</b><span>{{ actLabel(selected.action) }}</span></div>
          <div><b>目标</b><span>{{ selected.target || '—' }}</span></div>
          <div><b>IP</b><span>{{ selected.ip || '—' }}</span></div>
          <div><b>操作者邮箱</b><span>{{ selected.admin?.email || '—' }}</span></div>
        </div>
        <div v-if="selected.detail">
          <b style="margin-bottom:6px;display:block">详细数据</b>
          <pre class="detail-raw">{{ fmtDetail(selected.detail) }}</pre>
        </div>
        <div style="text-align:right;margin-top:16px">
          <button class="btn" @click="selected = null">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import api from '@/api';

const loading = ref(true);
const items = ref<any[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = 30;
const stats = ref<any>(null);
const selected = ref<any>(null);

const filters = ref({ action: '', target: '', startDate: '', endDate: '' });

const actionLabels: Record<string, string> = {
  login: '登录', logout: '退出', login_failed: '登录失败',
  member_update: '修改会员', member_ban: '封禁会员', member_unban: '解封会员',
  plan_create: '创建套餐', plan_update: '修改套餐', plan_delete: '删除套餐',
  permission_grant: '赋予权限', permission_revoke: '撤销权限',
  admin_create: '创建管理员', admin_update: '修改管理员',
  payment_create: '创建支付',
  activation_generate: '生成激活码', activation_revoke: '撤销激活码',
  activation_redeem: '兑换激活码',
  backup_create: '创建备份', backup_delete: '删除备份', backup_restore: '恢复备份',
  batch_ban: '批量封禁', batch_unban: '批量解封',
};

function actLabel(action: string): string {
  return actionLabels[action] || action;
}

function actionClass(action: string): string {
  if (action.includes('ban')) return 'danger';
  if (action.includes('delete') || action.includes('revoke')) return 'warn';
  if (action.includes('create') || action.includes('generate')) return 'success';
  if (action.includes('update') || action.includes('modify')) return 'info';
  if (action.includes('login')) return 'info';
  return 'default';
}

const fmtTs = (s: string) => {
  const d = new Date(s);
  return d.toLocaleString('zh-CN', { hour12: false });
};

function fmtDetail(raw: string) {
  try { return JSON.stringify(JSON.parse(raw), null, 2); }
  catch { return raw; }
}

async function fetchData() {
  loading.value = true;
  try {
    const params: any = { page: page.value, pageSize };
    if (filters.value.action) params.action = filters.value.action;
    if (filters.value.target) params.target = filters.value.target;
    if (filters.value.startDate) params.startDate = filters.value.startDate;
    if (filters.value.endDate) params.endDate = filters.value.endDate;
    const res = await api.get('/audit-logs', { params });
    items.value = res.data.data.items;
    total.value = res.data.data.total;
  } finally {
    loading.value = false;
  }
}

async function fetchStats() {
  try {
    const res = await api.get('/audit-logs/stats', { params: { days: 30 } });
    stats.value = res.data.data;
  } catch { /* ignore */ }
}

function search() { page.value = 1; fetchData(); }
function goPage(p: number) { page.value = p; fetchData(); }

function exportCsv() {
  const params: any = {};
  if (filters.value.startDate) params.startDate = filters.value.startDate;
  if (filters.value.endDate) params.endDate = filters.value.endDate;
  const qs = new URLSearchParams(params).toString();
  const token = (api.defaults.headers.common?.['Authorization'] as string) || '';
  window.open(`/api/v1/audit-logs/export?${qs}&token=${encodeURIComponent(token.replace('Bearer ', ''))}`, '_blank');
}

onMounted(() => { fetchData(); fetchStats(); });
</script>

<style scoped>
.muted { color: var(--text-3); padding: 30px; text-align: center; }
.kpi-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 10px; margin-bottom: 14px; }
.kpi-card { background: var(--card-bg); border-radius: 10px; padding: 14px 16px; border: 1px solid var(--border); }
.kpi-value { font-size: 22px; font-weight: 700; }
.kpi-label { font-size: 12px; color: var(--text-3); margin-top: 4px; }
.filters { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.inp { padding: 7px 10px; border: 1px solid var(--border); border-radius: 6px; font-size: 13px; background: var(--bg); color: var(--text); width: 140px; }
.tbl { width: 100%; border-collapse: collapse; font-size: 13px; }
.tbl th, .tbl td { text-align: left; padding: 9px 8px; border-bottom: 1px solid var(--border); }
.tbl th { color: var(--text-3); font-weight: 500; }
.clickable { cursor: pointer; transition: background var(--transition); }
.clickable:hover { background: var(--bg); }
.ts { white-space: nowrap; color: var(--text-2); }
.mono { font-family: 'SF Mono', 'Menlo', monospace; font-size: 12px; }
.tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.tag-success { background: #e6f9ed; color: #1a7f42; }
.tag-danger { background: #fde8e8; color: #c53030; }
.tag-warn { background: #fef8e7; color: #975a16; }
.tag-info { background: #e3f0ff; color: #1a56db; }
.tag-default { background: var(--bg); color: var(--text-2); }
[data-theme="dark"] .tag-success { background: #1a3a2a; color: #4ade80; }
[data-theme="dark"] .tag-danger { background: #3a1a1a; color: #f87171; }
[data-theme="dark"] .tag-warn { background: #3a2e1a; color: #fbbf24; }
[data-theme="dark"] .tag-info { background: #1a2a4a; color: #60a5fa; }
.pager { display: flex; align-items: center; justify-content: center; gap: 12px; padding-top: 12px; }
.pg-info { font-size: 13px; color: var(--text-2); }
.overlay { position: fixed; inset: 0; background: rgba(0,0,0,.4); z-index: 100; display: flex; align-items: center; justify-content: center; }
.modal { background: var(--card-bg); border-radius: 12px; padding: 24px; max-width: 560px; width: 90%; max-height: 80vh; overflow: auto; }
.modal h3 { margin-bottom: 16px; font-size: 16px; }
.detail-grid { display: grid; grid-template-columns: 100px 1fr; gap: 8px 12px; font-size: 13px; margin-bottom: 14px; }
.detail-grid b { color: var(--text-2); }
.detail-raw { background: var(--bg); border-radius: 6px; padding: 12px; font-size: 12px; max-height: 200px; overflow: auto; white-space: pre-wrap; word-break: break-all; }
</style>
