<template>
  <div class="settings">
    <div class="card">
      <div class="card-title">管理员账号</div>
      <div class="row"><span>邮箱</span><b>{{ auth.admin?.email }}</b></div>
      <div class="row"><span>姓名</span><b>{{ auth.admin?.name || '—' }}</b></div>
      <div class="row"><span>角色</span><b>{{ auth.admin?.role === 'superadmin' ? '超级管理员' : '管理员' }}</b></div>
      <button class="btn btn-sm" @click="logout">退出登录</button>
    </div>

    <div class="card">
      <div class="card-title">修改密码</div>
      <div class="pwd-form">
        <label>旧密码</label>
        <input v-model="pwd.old" class="input" type="password" />
        <label>新密码（至少 8 位）</label>
        <input v-model="pwd.new1" class="input" type="password" />
        <label>确认新密码</label>
        <input v-model="pwd.new2" class="input" type="password" />
        <button class="btn btn-primary" @click="changePwd" :disabled="pwdLoading">
          {{ pwdLoading ? '修改中...' : '修改密码' }}
        </button>
        <span class="msg" v-if="pwdMsg">{{ pwdMsg }}</span>
      </div>
    </div>

    <div class="card">
      <div class="card-title">安全与集成</div>
      <div class="row">
        <span>App API Key</span>
        <b>已在服务端配置（X-App-Key）</b>
      </div>
      <div class="row">
        <span>会员校验接口</span>
        <b>GET /api/v1/verify/membership</b>
      </div>
      <div class="row">
        <span>数据库</span>
        <b>SQLite（本地文件）</b>
      </div>
      <p class="note">
        修改 JWT 密钥、App API Key、数据库连接串请在后端 <code>.env</code> 中配置后重启服务。
      </p>
    </div>

    <div class="card">
      <div class="card-title">权益映射说明</div>
      <p class="note">
        套餐 <code>features</code> 字段即 iOS App 中会员版解锁的能力清单（自动记账、订阅管理、行情、AI 分析等）。
        App 启动时调用会员校验接口，按返回的 <code>features</code> 开/关高级功能。<br/>
        管理员可在会员详情中覆写权限（逐项赋予或收回），最终生效权限 = 套餐默认 + 赋予 - 收回。
      </p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useRouter } from 'vue-router';
import api from '@/api';

const auth = useAuthStore();
const router = useRouter();

const pwd = reactive({ old: '', new1: '', new2: '' });
const pwdLoading = ref(false);
const pwdMsg = ref('');

async function changePwd() {
  if (pwd.new1 !== pwd.new2) { pwdMsg.value = '两次新密码不一致'; return; }
  pwdLoading.value = true;
  pwdMsg.value = '';
  try {
    await api.post('/admins/change-password', { oldPassword: pwd.old, newPassword: pwd.new1 });
    pwdMsg.value = '密码已修改，下次登录生效';
    pwd.old = pwd.new1 = pwd.new2 = '';
  } catch (e: any) {
    pwdMsg.value = e.response?.data?.message || '修改失败';
  } finally { pwdLoading.value = false; }
}

function logout() {
  auth.logout();
  router.push('/login');
}
</script>

<style scoped>
.settings {
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-width: 640px;
}
.card-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
}
.row {
  display: flex;
  justify-content: space-between;
  padding: 9px 0;
  border-bottom: 1px solid var(--border);
  font-size: 13px;
}
.row span { color: var(--text-3); }
.note {
  font-size: 13px;
  color: var(--text-2);
  line-height: 1.7;
  margin: 10px 0 0;
}
code {
  background: var(--bg);
  padding: 1px 6px;
  border-radius: 5px;
  font-size: 12px;
}
.pwd-form { display: flex; flex-direction: column; gap: 8px; max-width: 300px; }
.pwd-form label { font-size: 13px; color: var(--text-2); margin-top: 6px; }
.pwd-form button { margin-top: 12px; }
.msg { font-size: 13px; margin-top: 4px; }
</style>
