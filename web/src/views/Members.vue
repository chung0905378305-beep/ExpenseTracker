<template>
  <div class="members-page">
    <div class="toolbar">
      <h2>会员管理</h2>
      <div class="toolbar-actions">
        <button class="btn btn-sm" @click="exportXlsx">导出 Excel</button>
        <div class="search-box">
          <input v-model="q" class="input" placeholder="搜索昵称/邮箱/ID..." @keyup.enter="search" />
          <select v-model="statusFilter" class="input" style="width:110px" @change="search">
            <option value="">全部状态</option>
            <option value="active">活跃</option>
            <option value="trialing">试用</option>
            <option value="canceled">已取消</option>
            <option value="past_due">逾期</option>
          </select>
          <select v-model="bannedFilter" class="input" style="width:100px" @change="search">
            <option value="">全部</option>
            <option value="true">已封禁</option>
            <option value="false">未封禁</option>
          </select>
          <button class="btn btn-sm" @click="search">搜索</button>
        </div>
      </div>
    </div>

    <!-- 批量操作栏 -->
    <div class="batch-bar" v-if="selectedIds.length > 0">
      <span>已选 {{ selectedIds.length }} 项</span>
      <button class="btn btn-sm btn-danger" @click="batchBan">批量封禁</button>
      <button class="btn btn-sm" @click="batchUnban">批量解封</button>
      <select v-model="batchTagId" class="input" style="width:130px">
        <option value="">选择标签...</option>
        <option v-for="t in tagList" :key="t.id" :value="t.id">{{ t.name }} ({{ t.memberCount }})</option>
      </select>
      <button class="btn btn-sm" @click="batchTag" :disabled="!batchTagId">批量打标签</button>
      <button class="btn btn-sm" @click="clearSelection">取消选择</button>
    </div>

    <table class="table">
      <thead>
        <tr>
          <th style="width:40px"><input type="checkbox" @change="toggleAll" :checked="allSelected" /></th>
          <th>昵称</th><th>邮箱</th><th>标签</th><th>套餐</th><th>状态</th><th>到期</th><th>封禁</th><th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="m in list" :key="m.id" :class="{ selected: selectedIds.includes(m.id) }">
          <td><input type="checkbox" :checked="selectedIds.includes(m.id)" @change="toggleOne(m.id)" /></td>
          <td>{{ m.nickname || '—' }}</td>
          <td>{{ m.email || '—' }}</td>
          <td>
            <span v-if="memberTags[m.id]?.length" class="tags-inline">
              <span v-for="t in memberTags[m.id]" :key="t.tag.id" class="mini-tag" :style="{ background: t.tag.color + '22', color: t.tag.color }">{{ t.tag.name }}</span>
            </span>
            <span v-else class="muted">—</span>
          </td>
          <td>{{ m.plan?.name || '—' }}</td>
          <td><StatusTag :status="m.status" /></td>
          <td>{{ fmt(m.expireAt) }}</td>
          <td><StatusTag :status="m.banned ? 'canceled' : 'active'" :text="m.banned ? '已封禁' : '正常'" /></td>
          <td><button class="btn btn-sm" @click="openDetail(m)">详情</button></td>
        </tr>
      </tbody>
    </table>

    <div class="pager" v-if="total > pageSize">
      <button class="btn btn-sm" :disabled="page <= 1" @click="go(page - 1)">上一页</button>
      <span>{{ page }} / {{ Math.ceil(total / pageSize) }}</span>
      <button class="btn btn-sm" :disabled="page >= Math.ceil(total / pageSize)" @click="go(page + 1)">下一页</button>
    </div>

    <!-- 详情抽屉 -->
    <div class="overlay" v-if="detail" @click.self="detail = null">
      <div class="drawer">
        <div class="drawer-header">
          <h3>{{ detail.nickname || detail.appUserId }}</h3>
          <button class="btn btn-sm" @click="detail = null">关闭</button>
        </div>

        <div class="drawer-tabs">
          <button :class="{ active: tab === 'info' }" @click="tab = 'info'">基本信息</button>
          <button :class="{ active: tab === 'payments' }" @click="tab = 'payments'">支付记录</button>
          <button :class="{ active: tab === 'perms' }" @click="tab = 'perms'">权限管理</button>
          <button :class="{ active: tab === 'tags' }" @click="loadMemberTags()">标签</button>
        </div>

        <!-- 基本信息 -->
        <div class="tab-content" v-if="tab === 'info'">
          <div class="row"><span>APP User ID</span><b>{{ detail.appUserId }}</b></div>
          <div class="row"><span>邮箱</span><b>{{ detail.email || '—' }}</b></div>
          <div class="row"><span>昵称</span><b>{{ detail.nickname || '—' }}</b></div>
          <div class="row"><span>平台</span><b>{{ detail.platform }}</b></div>
          <div class="row"><span>套餐</span><b>{{ detail.plan?.name || '—' }}</b></div>
          <div class="row"><span>状态</span><b>{{ detail.status }}</b></div>
          <div class="row"><span>加入时间</span><b>{{ fmt(detail.joinedAt) }}</b></div>
          <div class="row"><span>到期时间</span><b>{{ fmt(detail.expireAt) }}</b></div>
          <div class="row"><span>订阅自动续费</span><b>{{ detail.subscription?.autoRenew ? '是' : '否' }}</b></div>
          <div class="actions" style="margin-top:14px">
            <button v-if="!detail.banned" class="btn btn-danger btn-sm" @click="ban">封禁会员</button>
            <button v-else class="btn btn-sm" @click="unban">解除封禁</button>
          </div>
        </div>

        <!-- 支付记录 -->
        <div class="tab-content" v-if="tab === 'payments'">
          <table class="table" v-if="detail.payments?.length">
            <thead><tr><th>金额</th><th>状态</th><th>渠道</th><th>时间</th></tr></thead>
            <tbody>
              <tr v-for="p in detail.payments" :key="p.id">
                <td>{{ p.amount }} {{ p.currency }}</td>
                <td><StatusTag :status="p.status" /></td>
                <td>{{ p.provider || '—' }}</td>
                <td>{{ fmt(p.paidAt) }}</td>
              </tr>
            </tbody>
          </table>
          <p v-else class="empty">暂无支付记录</p>
        </div>

        <!-- 权限管理 -->
        <div class="tab-content" v-if="tab === 'perms'">
          <p class="note" v-if="permPlanName">套餐「{{ permPlanName }}」默认权限，可逐项覆写</p>
          <div class="perm-item" v-for="p in allPerms" :key="p.key">
            <div class="perm-info">
              <span class="perm-label">{{ p.label }}</span>
              <span class="perm-desc">{{ p.desc }}</span>
            </div>
            <div class="perm-actions">
              <span class="perm-status" :class="getPermClass(p.key)">{{ getPermText(p.key) }}</span>
              <button class="btn btn-sm" @click="togglePerm(p.key, 'grant')" :disabled="permLoading">+ 赋予</button>
              <button class="btn btn-sm" @click="togglePerm(p.key, 'revoke')" :disabled="permLoading">− 收回</button>
              <button class="btn btn-sm" @click="resetPerm(p.key)" v-if="permOverrides[p.key]" :disabled="permLoading">恢复默认</button>
            </div>
          </div>
          <span class="msg" v-if="permMsg">{{ permMsg }}</span>
        </div>

        <!-- 标签 -->
        <div class="tab-content" v-if="tab === 'tags' && detailTags">
          <div class="tag-manager">
            <div class="tag-add">
              <select v-model="addTagId" class="input">
                <option value="">选择标签添加...</option>
                <option v-for="t in tagList" :key="t.id" :value="t.id">{{ t.name }}</option>
              </select>
              <button class="btn btn-sm" @click="addTagToMember" :disabled="!addTagId">添加</button>
            </div>
            <div class="current-tags" v-if="detailTags.length">
              <span v-for="mt in detailTags" :key="mt.id" class="tag-chip" :style="{ background: mt.tag.color + '22', borderColor: mt.tag.color }">
                {{ mt.tag.name }}
                <button class="tag-remove" @click="removeTag(mt.tag.id)">×</button>
              </span>
            </div>
            <p v-else class="empty">暂无标签</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 确认弹窗 -->
    <div class="overlay" v-if="confirmModal" @click.self="confirmModal = null">
      <div class="mini-modal">
        <p>{{ confirmModal }}</p>
        <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:14px">
          <button class="btn btn-sm" @click="confirmModal = null">取消</button>
          <button class="btn btn-sm btn-danger" @click="confirmBatchAction">确认</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import api from '@/api';
import StatusTag from '@/components/StatusTag.vue';
import { useToastStore } from '@/stores/toast';

const toast = useToastStore();

const q = ref('');
const statusFilter = ref('');
const bannedFilter = ref('');
const page = ref(1);
const pageSize = 20;
const total = ref(0);
const list = ref<any[]>([]);

// 多选
const selectedIds = ref<string[]>([]);
const allSelected = ref(false);

// 标签
const tagList = ref<any[]>([]);
const memberTags = ref<Record<string, any[]>>({});
const detailTags = ref<any[]>([]);
const addTagId = ref('');
const batchTagId = ref('');

// 批量确认
const confirmModal = ref<string | null>(null);
let pendingBatchAction: (() => Promise<void>) | null = null;

// 详情
const detail = ref<any>(null);
const tab = ref('info');

interface PermItem { key: string; label: string; desc: string }
const allPerms = ref<PermItem[]>([]);
const permPlanName = ref('');
const permOverrides = reactive<Record<string, string | null>>({});
const permLoading = ref(false);
const permMsg = ref('');

async function search() { page.value = 1; await load(); }
async function go(p: number) { page.value = p; await load(); }

async function load() {
  const params: any = { page: page.value, pageSize };
  if (q.value) params.q = q.value;
  if (statusFilter.value) params.status = statusFilter.value;
  if (bannedFilter.value) params.banned = bannedFilter.value;
  const { data } = await api.get('/members', { params });
  if (data.code === 0) { list.value = data.data.items; total.value = data.data.total; }
  await loadMemberTagsBatch();
}

async function loadTags() {
  try {
    const { data } = await api.get('/tags');
    if (data.code === 0) tagList.value = data.data;
  } catch { /* ignore */ }
}

async function loadMemberTagsBatch() {
  const ids = list.value.map(m => m.id);
  // Load tags in batches of 10
  for (const id of ids) {
    try {
      const { data } = await api.get(`/tags/members/${id}/tags`);
      if (data.code === 0) memberTags.value[id] = data.data;
    } catch { memberTags.value[id] = []; }
  }
}

async function loadMemberTags() {
  if (!detail.value) return;
  tab.value = 'tags';
  try {
    const { data } = await api.get(`/tags/members/${detail.value.id}/tags`);
    if (data.code === 0) detailTags.value = data.data;
  } catch { detailTags.value = []; }
}

async function addTagToMember() {
  if (!addTagId.value || !detail.value) return;
  try {
    await api.post(`/tags/members/${detail.value.id}/tags`, { tagId: addTagId.value });
    toast.success('标签已添加');
    addTagId.value = '';
    await loadMemberTags();
  } catch (e: any) { toast.error(e.response?.data?.message || '添加失败'); }
}

async function removeTag(tagId: string) {
  if (!detail.value) return;
  try {
    await api.delete(`/tags/members/${detail.value.id}/tags/${tagId}`);
    toast.success('标签已移除');
    await loadMemberTags();
  } catch (e: any) { toast.error('移除失败'); }
}

async function batchTag() {
  if (!batchTagId.value || selectedIds.value.length === 0) return;
  try {
    await api.post('/tags/batch-tag', { memberIds: selectedIds.value, tagId: batchTagId.value });
    toast.success(`已为 ${selectedIds.value.length} 个会员打标签`);
    batchTagId.value = '';
    clearSelection();
    await load();
  } catch (e: any) { toast.error(e.response?.data?.message || '操作失败'); }
}

function toggleAll(e: Event) {
  const checked = (e.target as HTMLInputElement).checked;
  selectedIds.value = checked ? list.value.map(m => m.id) : [];
  allSelected.value = checked;
}

function toggleOne(id: string) {
  const idx = selectedIds.value.indexOf(id);
  if (idx >= 0) selectedIds.value.splice(idx, 1);
  else selectedIds.value.push(id);
  allSelected.value = selectedIds.value.length === list.value.length;
}

function clearSelection() { selectedIds.value = []; allSelected.value = false; }

async function batchBan() {
  confirmModal.value = `确定封禁 ${selectedIds.value.length} 个会员？`;
  pendingBatchAction = async () => {
    try {
      const { data } = await api.post('/batch/ban', { memberIds: selectedIds.value, reason: '批量封禁' });
      toast.success(`已封禁 ${data.data.affected} 个会员`);
      clearSelection();
      await load();
    } catch (e: any) { toast.error(e.response?.data?.message || '操作失败'); }
    confirmModal.value = null;
  };
}

async function batchUnban() {
  confirmModal.value = `确定解封 ${selectedIds.value.length} 个会员？`;
  pendingBatchAction = async () => {
    try {
      const { data } = await api.post('/batch/unban', { memberIds: selectedIds.value });
      toast.success(`已解封 ${data.data.affected} 个会员`);
      clearSelection();
      await load();
    } catch (e: any) { toast.error(e.response?.data?.message || '操作失败'); }
    confirmModal.value = null;
  };
}

async function confirmBatchAction() {
  if (pendingBatchAction) await pendingBatchAction();
}

async function openDetail(m: any) {
  const { data } = await api.get(`/members/${m.id}`);
  if (data.code === 0) detail.value = data.data;
  tab.value = 'info';
  await loadPerms(m.id);
}

async function loadPerms(memberId: string) {
  try {
    const [permRes, allRes] = await Promise.all([
      api.get(`/permissions/members/${memberId}`),
      api.get('/permissions'),
    ]);
    if (permRes.data.code === 0) {
      const d = permRes.data.data;
      permPlanName.value = d.planName;
      for (const key of Object.keys(permOverrides)) delete permOverrides[key];
      for (const o of d.overrides) permOverrides[o.permissionName] = o.action;
    }
    if (allRes.data.code === 0) allPerms.value = allRes.data.data;
  } catch (e) { /** ignore */ }
}

async function togglePerm(key: string, action: string) {
  permLoading.value = true; permMsg.value = '';
  try {
    await api.post(`/permissions/members/${detail.value.id}`, { permissionName: key, action });
    permOverrides[key] = action;
  } catch (e: any) { permMsg.value = e.response?.data?.message || '操作失败'; }
  finally { permLoading.value = false; }
}

async function resetPerm(key: string) {
  permLoading.value = true; permMsg.value = '';
  try {
    await api.delete(`/permissions/members/${detail.value.id}/${key}`);
    delete permOverrides[key];
  } catch (e: any) { permMsg.value = e.response?.data?.message || '操作失败'; }
  finally { permLoading.value = false; }
}

function getPermClass(key: string) {
  return permOverrides[key] === 'grant' ? 'granted' : permOverrides[key] === 'revoke' ? 'revoked' : 'default';
}
function getPermText(key: string) {
  return permOverrides[key] === 'grant' ? '已赋予' : permOverrides[key] === 'revoke' ? '已收回' : '默认';
}

async function ban() {
  if (!detail.value) return;
  await api.post(`/members/${detail.value.id}/ban`, { reason: '管理员操作' });
  detail.value.banned = true;
  await load();
}
async function unban() {
  if (!detail.value) return;
  await api.post(`/members/${detail.value.id}/unban`);
  detail.value.banned = false;
  await load();
}

function exportXlsx() {
  const token = (api.defaults.headers.common?.['Authorization'] as string) || '';
  window.open(`/api/v1/export/members?token=${encodeURIComponent(token.replace('Bearer ', ''))}`, '_blank');
}

function fmt(d: string | null) {
  if (!d) return '—';
  return new Date(d).toLocaleDateString('zh-CN');
}

onMounted(async () => {
  await Promise.all([load(), loadTags()]);
});
</script>

<style scoped>
.members-page { max-width: 1100px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; flex-wrap: wrap; gap: 8px; }
.toolbar h2 { font-size: 16px; }
.toolbar-actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.search-box { display: flex; gap: 6px; align-items: center; }
.search-box .input { width: 140px; }
.batch-bar { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: var(--primary-soft); border-radius: 8px; margin-bottom: 10px; font-size: 13px; }
.pager { display: flex; justify-content: center; align-items: center; gap: 12px; margin-top: 16px; font-size: 13px; }
.overlay { position: fixed; inset: 0; background: rgba(0,0,0,.3); display: flex; justify-content: flex-end; z-index: 999; }
.drawer { width: 520px; max-width: 96vw; background: var(--card-bg); height: 100%; overflow: auto; padding: 20px; }
[data-theme="dark"] .drawer { background: var(--card-bg); }
.drawer-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.drawer-header h3 { font-size: 16px; }
.drawer-tabs { display: flex; gap: 4px; margin-bottom: 16px; flex-wrap: wrap; }
.drawer-tabs button { background: var(--bg); border: none; padding: 7px 14px; border-radius: 8px; font-size: 13px; cursor: pointer; color: var(--text-2); }
.drawer-tabs button.active { background: var(--primary); color: #fff; }
.tab-content .row { display: flex; justify-content: space-between; padding: 9px 0; border-bottom: 1px solid var(--border); font-size: 13px; }
.tab-content .row span { color: var(--text-3); }
.actions { display: flex; gap: 8px; }
.empty { color: var(--text-2); font-size: 13px; padding: 16px 0; }
.selected { background: var(--primary-soft) !important; }
.tags-inline { display: flex; gap: 3px; flex-wrap: wrap; }
.mini-tag { font-size: 10px; padding: 1px 6px; border-radius: 8px; white-space: nowrap; font-weight: 500; }
.muted { color: var(--text-3); }
.tag-manager { padding: 8px 0; }
.tag-add { display: flex; gap: 8px; margin-bottom: 12px; }
.current-tags { display: flex; flex-wrap: wrap; gap: 6px; }
.tag-chip { display: inline-flex; align-items: center; gap: 4px; padding: 4px 10px; border-radius: 14px; font-size: 12px; border: 1px solid; }
.tag-remove { background: none; border: none; cursor: pointer; font-size: 16px; padding: 0; line-height: 1; color: inherit; opacity: .7; }
.tag-remove:hover { opacity: 1; }

.note { font-size: 13px; color: var(--text-2); margin-bottom: 12px; }
.perm-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid var(--border); }
.perm-info { display: flex; flex-direction: column; gap: 2px; }
.perm-label { font-size: 13px; font-weight: 600; }
.perm-desc { font-size: 11px; color: var(--text-3); }
.perm-actions { display: flex; align-items: center; gap: 6px; }
.perm-status { font-size: 11px; padding: 2px 8px; border-radius: 10px; }
.perm-status.default { background: var(--bg); color: var(--text-2); }
.perm-status.granted { background: #EAF3DE; color: #27500A; }
.perm-status.revoked { background: #FCEBEB; color: #A32D2D; }
.msg { font-size: 13px; margin-top: 8px; display: block; }
.mini-modal { background: var(--card-bg); border-radius: 12px; padding: 20px; max-width: 380px; width: 90%; }
.mini-modal p { font-size: 14px; }
</style>
