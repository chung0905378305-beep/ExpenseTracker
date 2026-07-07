<template>
  <div class="login-wrap">
    <div class="login-bg" />
    <div class="login-card card">
      <div class="icon-lock">🔐</div>
      <h1>记账本</h1>
      <p class="sub">会员管理后台</p>
      <div v-if="error" class="err" @click="error = ''">{{ error }}</div>
      <label>邮箱</label>
      <input class="input" v-model="email" type="email" placeholder="admin@expensetracker.app" />
      <label>密码</label>
      <input
        class="input"
        v-model="password"
        type="password"
        placeholder="••••••••"
        @keyup.enter="submit"
      />
      <button class="btn btn-primary submit" :disabled="loading" @click="submit">
        {{ loading ? '登录中…' : '登 录' }}
      </button>
      <p class="hint">演示：admin@expensetracker.app / admin123456</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import api from '@/api';
import { useAuthStore } from '@/stores/auth';

const email = ref('admin@expensetracker.app');
const password = ref('');
const error = ref('');
const loading = ref(false);
const auth = useAuthStore();
const route = useRoute();
const router = useRouter();

async function submit() {
  error.value = '';
  loading.value = true;
  try {
    const { data } = await api.post('/auth/login', {
      email: email.value,
      password: password.value,
    });
    auth.setAuth(data.data.token, data.data.refreshToken, data.data.admin);
    const redirect = (route.query.redirect as string) || '/dashboard';
    router.push(redirect);
  } catch (e: any) {
    error.value = e.response?.data?.message || '登录失败';
  } finally {
    loading.value = false;
  }
}
</script>

<style scoped>
.login-wrap {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}
.login-bg {
  position: absolute;
  inset: 0;
  background: var(--login-bg);
  z-index: 0;
}
.login-bg::after {
  content: '';
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 20% 50%, rgba(255,255,255,0.1) 0%, transparent 50%),
              radial-gradient(circle at 80% 20%, rgba(255,255,255,0.05) 0%, transparent 40%);
}
.login-card {
  width: 380px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  position: relative;
  z-index: 1;
  animation: fadeIn 0.4s ease;
  background: var(--surface);
}
.icon-lock {
  font-size: 32px;
  text-align: center;
  margin-bottom: -4px;
}
h1 {
  margin: 0;
  font-size: 22px;
  text-align: center;
}
.sub {
  margin: 0 0 16px;
  color: var(--text-3);
  font-size: 13px;
  text-align: center;
}
label {
  font-size: 12px;
  color: var(--text-2);
  margin-top: 6px;
}
.submit {
  margin-top: 20px;
  padding: 11px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 2px;
}
.hint {
  font-size: 12px;
  color: var(--text-3);
  margin: 12px 0 0;
  text-align: center;
}
.err {
  background: #ffeceb;
  color: var(--danger);
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
}
</style>
