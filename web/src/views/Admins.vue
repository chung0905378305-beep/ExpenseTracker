<template>
  <div class="admins-page">
    <div class="toolbar">
      <h2>管理员</h2>
      <button class="btn btn-primary" @click="openCreate" v-if="isSuper">添加管理员</button>
    </div>

    <table class="table" v-if="admins.length">
      <thead>
        <tr><th>姓名</th><th>邮箱</th><th>角色</th><th>状态</th><th>最后登录</th><th v-if="isSuper">操作</th></tr>
      </thead>
      <tbody>
        <tr v-for="a in admins" :key="a.id">
          <td>{{ a.name }}</td>
          <td>{{ a.email }}</td>
          <td><StatusTag :status="a.role === 'superadmin' ? 'superadmin' : 'admin'" /></td>
          <td><StatusTag :status="a.active ? 'active' : 'inactive'" /></td>
          <td>{{ fmt(a.lastLogin) }}</td>
          <td v-if="isSuper">
            <div class="actions">
              <button class="btn btn-sm" @click="openEdit(a)">编辑</button>
              <button class="btn btn-sm" @click="openResetPwd(a)">重置密码</button>
              <button class="btn btn-sm btn-danger" @click="del(a)" v-if="a.id !== myId">删除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
    <div class="empty" v-else>暂无管理员</div>

    <!-- 创建/编辑弹窗 -->
    <div class="overlay" v-if="dialog" @click.self="dialog = false">
      <div class="dialog">
        <h3>{{ editing !== null ? '编辑管理员' : '添加管理员' }}</h3>
        <label>姓名</label>
        <input v-model="form.name" class="input" />
        <label>邮箱</label>
        <input v-model="form.email" class="input" :disabled="!!editing" />
        <template v-if="editing === null">
          <label>密码（至少 8 位）</label>
          <input v-model="form.password" class="input" type="password" />
        </template>
        <label>角色</label>
        <select v-model="form.role" class="input">
          <option value="admin">管理员</option>
          <option value="superadmin">超级管理员</option>
        </select>
        <template v-if="editing !== null">
          <label>启用</label>
          <select v-model="form.active" class="input">
            <option :value="true">是</option>
            <option :value="false">否</option>
          </select>
        </template>
        <div class="dialog-btns">
          <button class="btn" @click="dialog = false">取消</button>
          <button class="btn btn-primary" @click="save" :disabled="loading">{{ loading ? '保存中...' : '保存' }}</button>
        </div>
      </div>
    </div>

    <!-- 重置密码弹窗 -->
    <div class="overlay" v-if="pwdDialog" @click.self="pwdDialog = false">
      <div class="dialog">
        <h3>重置密码：{{ pwdTarget?.name }}</h3>
        <label>新密码（至少 8 位）</label>
        <input v-model="newPassword" class="input" type="password" />
        <div class="dialog-btns">
          <button class="btn" @click="pwdDialog = false">取消</button>
          <button class="btn btn-primary" @click="doResetPwd" :disabled="loading">确认</button>
        </div>
      </div>
    </div>

    <div class="error" v-if="error">{{ error }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import api from '@/api';
import { useAuthStore } from '@/stores/auth';
import StatusTag from '@/components/StatusTag.vue';

const auth = useAuthStore();
const myId = computed(() => auth.admin?.id);
const isSuper = computed(() => auth.admin?.role === 'superadmin');

interface Admin {
  id: string; email: string; name: string; role: string; active: boolean;
  lastLogin: string | null; createdAt: string;
}

const admins = ref<Admin[]>([]);
const dialog = ref(false);
const pwdDialog = ref(false);
const editing = ref<Admin | null>(null);
const pwdTarget = ref<Admin | null>(null);
const loading = ref(false);
const error = ref('');

const form = ref({ name: '', email: '', password: '', role: 'admin', active: true });
const newPassword = ref('');

function fmt(d: string | null) {
  if (!d) return '—';
  return new Date(d).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
}

async function load() {
  try {
    const { data } = await api.get('/admins');
    if (data.code === 0) admins.value = data.data;
  } catch (e: any) {
    if (e.response?.status === 403) { error.value = '仅超级管理员可查看'; }
  }
}

function openCreate() {
  editing.value = null;
  form.value = { name: '', email: '', password: '', role: 'admin', active: true };
  dialog.value = true;
}
function openEdit(a: Admin) {
  editing.value = a;
  form.value = { name: a.name, email: a.email, password: '', role: a.role, active: a.active };
  dialog.value = true;
}
function openResetPwd(a: Admin) {
  pwdTarget.value = a;
  newPassword.value = '';
  pwdDialog.value = true;
}

async function save() {
  loading.value = true;
  try {
    if (editing.value) {
      await api.patch(`/admins/${editing.value.id}`, {
        name: form.value.name, role: form.value.role, active: form.value.active,
      });
    } else {
      await api.post('/admins', form.value);
    }
    dialog.value = false;
    await load();
  } catch (e: any) {
    error.value = e.response?.data?.message || '操作失败';
  } finally { loading.value = false; }
}

async function doResetPwd() {
  if (!pwdTarget.value || !newPassword.value) return;
  loading.value = true;
  try {
    await api.post(`/admins/${pwdTarget.value.id}/reset-password`, { newPassword: newPassword.value });
    pwdDialog.value = false;
    alert('密码已重置');
  } catch (e: any) {
    error.value = e.response?.data?.message || '操作失败';
  } finally { loading.value = false; }
}

async function del(a: Admin) {
  if (!confirm(`确认删除管理员 ${a.name}？此操作不可撤销。`)) return;
  try {
    await api.delete(`/admins/${a.id}`);
    await load();
  } catch (e: any) {
    error.value = e.response?.data?.message || '删除失败';
  }
}

onMounted(load);
</script>

<style scoped>
.admins-page { max-width: 860px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.toolbar h2 { font-size: 16px; }
.actions { display: flex; gap: 6px; }
.empty { color: var(--text-2); font-size: 13px; padding: 24px 0; }
.error { color: var(--danger); font-size: 13px; margin-top: 12px; }
.overlay { position: fixed; inset: 0; background: rgba(0,0,0,.3); display: flex; justify-content: center; align-items: center; z-index: 999; }
.dialog { background: #fff; border-radius: 14px; padding: 24px; width: 400px; max-width: 92vw; }
.dialog h3 { margin-bottom: 16px; font-size: 15px; }
.dialog label { display: block; font-size: 13px; color: var(--text-2); margin-bottom: 4px; margin-top: 12px; }
.dialog-btns { display: flex; justify-content: flex-end; gap: 8px; margin-top: 18px; }
</style>
