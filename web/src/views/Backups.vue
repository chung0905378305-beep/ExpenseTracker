<template>
  <div>
    <div class="top-actions">
      <button class="btn" @click="doBackup" :disabled="backingUp">
        {{ backingUp ? '备份中…' : '+ 立即备份' }}
      </button>
      <span class="hint">数据库路径：ExpenseTracker/backend/prisma/dev.db</span>
    </div>

    <div v-if="loading" class="muted">加载中…</div>
    <div class="card" v-else>
      <table class="tbl">
        <thead>
          <tr>
            <th>备份名称</th>
            <th>大小</th>
            <th>时间</th>
            <th style="width:160px">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="b in backups" :key="b.name">
            <td class="mono">{{ b.name }}</td>
            <td>{{ fmtSize(b.size) }}</td>
            <td>{{ fmtTs(b.date) }}</td>
            <td>
              <button class="btn btn-sm btn-hollow" @click="doRestore(b.name)">恢复</button>
              <button class="btn btn-sm btn-danger" @click="doDelete(b.name)" style="margin-left:6px">删除</button>
            </td>
          </tr>
          <tr v-if="backups.length === 0">
            <td colspan="4" class="muted">暂无备份，点击"立即备份"创建第一个备份</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 确认弹窗 -->
    <div class="overlay" v-if="confirmAction">
      <div class="modal">
        <h3>{{ confirmAction === 'restore' ? '确认恢复' : '确认删除' }}</h3>
        <p v-if="confirmAction === 'restore'" class="warn-text">
          将数据库恢复到 <b>{{ confirmTarget }}</b>，当前数据会被覆盖。此操作不可逆！
        </p>
        <p v-else>
          确定删除备份 <b>{{ confirmTarget }}</b>？
        </p>
        <div style="text-align:right;margin-top:16px;display:flex;gap:8px;justify-content:flex-end">
          <button class="btn btn-hollow" @click="confirmAction = null">取消</button>
          <button class="btn" :class="confirmAction === 'restore' ? 'btn-danger' : ''" @click="confirmDo">{{ confirmAction === 'restore' ? '确认恢复' : '确认删除' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import api from '@/api';
import { useToastStore } from '@/stores/toast';

const toast = useToastStore();
const loading = ref(true);
const backups = ref<any[]>([]);
const backingUp = ref(false);
const confirmAction = ref<string | null>(null);
const confirmTarget = ref('');

function fmtSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}
const fmtTs = (s: string) => new Date(s).toLocaleString('zh-CN', { hour12: false });

async function fetchBackups() {
  loading.value = true;
  try {
    const res = await api.get('/backups');
    backups.value = res.data.data.backups;
  } finally {
    loading.value = false;
  }
}

async function doBackup() {
  backingUp.value = true;
  try {
    const res = await api.post('/backups');
    toast.success(`备份成功：${res.data.data.name}`);
    await fetchBackups();
  } catch (err: any) {
    toast.error(err?.response?.data?.message || '备份失败');
  } finally {
    backingUp.value = false;
  }
}

function doRestore(name: string) {
  confirmTarget.value = name;
  confirmAction.value = 'restore';
}

function doDelete(name: string) {
  confirmTarget.value = name;
  confirmAction.value = 'delete';
}

async function confirmDo() {
  try {
    if (confirmAction.value === 'restore') {
      await api.post(`/backups/${confirmTarget.value}/restore`);
      toast.success('数据库已恢复');
    } else {
      await api.delete(`/backups/${confirmTarget.value}`);
      toast.success('备份已删除');
    }
    confirmAction.value = null;
    await fetchBackups();
  } catch (err: any) {
    toast.error(err?.response?.data?.message || '操作失败');
  }
}

onMounted(fetchBackups);
</script>

<style scoped>
.muted { color: var(--text-3); padding: 30px; text-align: center; }
.top-actions { display: flex; align-items: center; gap: 14px; margin-bottom: 14px; }
.hint { font-size: 12px; color: var(--text-3); }
.tbl { width: 100%; border-collapse: collapse; font-size: 13px; }
.tbl th, .tbl td { text-align: left; padding: 10px 8px; border-bottom: 1px solid var(--border); }
.tbl th { color: var(--text-3); font-weight: 500; }
.mono { font-family: 'SF Mono', 'Menlo', monospace; font-size: 12px; }
.btn-danger { background: #dc3545; color: #fff; border-color: #dc3545; }
.btn-danger:hover { background: #c82333; }
.overlay { position: fixed; inset: 0; background: rgba(0,0,0,.4); z-index: 100; display: flex; align-items: center; justify-content: center; }
.modal { background: var(--card-bg); border-radius: 12px; padding: 24px; max-width: 440px; width: 90%; }
.modal h3 { margin-bottom: 12px; font-size: 16px; }
.modal p { font-size: 14px; color: var(--text-2); }
.warn-text { color: #dc3545 !important; }
</style>
