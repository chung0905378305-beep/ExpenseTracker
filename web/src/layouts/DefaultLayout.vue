<template>
  <div class="layout" :class="{ collapsed: sidebarCollapsed }">
    <aside class="sidebar">
      <div class="logo">
        <span v-if="!sidebarCollapsed">记账本<b>后台</b></span>
        <span v-else>📒</span>
      </div>
      <nav>
        <router-link to="/dashboard" class="nav-item" :title="sidebarCollapsed ? '运营看板' : ''">
          <span class="nav-icon">📊</span><span v-if="!sidebarCollapsed" class="nav-label">运营看板</span>
        </router-link>
        <router-link to="/analytics" class="nav-item" :title="sidebarCollapsed ? '分析' : ''">
          <span class="nav-icon">📈</span><span v-if="!sidebarCollapsed" class="nav-label">数据分析</span>
        </router-link>
        <router-link to="/members" class="nav-item" :title="sidebarCollapsed ? '会员' : ''">
          <span class="nav-icon">👤</span><span v-if="!sidebarCollapsed" class="nav-label">会员</span>
        </router-link>
        <router-link to="/plans" class="nav-item" :title="sidebarCollapsed ? '套餐' : ''">
          <span class="nav-icon">🎁</span><span v-if="!sidebarCollapsed" class="nav-label">套餐</span>
        </router-link>
        <router-link to="/subscriptions" class="nav-item" :title="sidebarCollapsed ? '订阅' : ''">
          <span class="nav-icon">🔄</span><span v-if="!sidebarCollapsed" class="nav-label">订阅</span>
        </router-link>
        <router-link to="/payments" class="nav-item" :title="sidebarCollapsed ? '支付' : ''">
          <span class="nav-icon">💰</span><span v-if="!sidebarCollapsed" class="nav-label">支付</span>
        </router-link>
        <router-link to="/activation-codes" class="nav-item" :title="sidebarCollapsed ? '激活码' : ''">
          <span class="nav-icon">🎫</span><span v-if="!sidebarCollapsed" class="nav-label">激活码</span>
        </router-link>
        <router-link to="/audit-logs" class="nav-item" :title="sidebarCollapsed ? '审计日志' : ''">
          <span class="nav-icon">📋</span><span v-if="!sidebarCollapsed" class="nav-label">审计日志</span>
        </router-link>
        <router-link to="/backups" class="nav-item" :title="sidebarCollapsed ? '备份' : ''">
          <span class="nav-icon">💾</span><span v-if="!sidebarCollapsed" class="nav-label">备份</span>
        </router-link>
        <router-link to="/admins" class="nav-item" :title="sidebarCollapsed ? '管理员' : ''">
          <span class="nav-icon">👥</span><span v-if="!sidebarCollapsed" class="nav-label">管理员</span>
        </router-link>
        <router-link to="/settings" class="nav-item" :title="sidebarCollapsed ? '设置' : ''">
          <span class="nav-icon">⚙️</span><span v-if="!sidebarCollapsed" class="nav-label">设置</span>
        </router-link>
        <a href="/api-docs" target="_blank" class="nav-item" :title="sidebarCollapsed ? 'API文档' : ''">
          <span class="nav-icon">📘</span><span v-if="!sidebarCollapsed" class="nav-label">API 文档</span>
        </a>
      </nav>
      <div class="sidebar-footer" v-if="!sidebarCollapsed">
        <button class="btn btn-sm" @click="toggleTheme">{{ themeBtn }}</button>
      </div>
    </aside>
    <div class="main">
      <header class="topbar">
        <div class="topbar-left">
          <button class="btn-icon hamburger" @click="sidebarCollapsed = !sidebarCollapsed">☰</button>
          <div class="title">{{ pageTitle }}</div>
        </div>
        <div class="user" v-if="auth.admin">
          <button class="btn-icon theme-btn" @click="toggleTheme" v-if="sidebarCollapsed">{{ themeBtn }}</button>
          <span class="avatar">{{ initial }}</span>
          <span class="uname hide-mobile">{{ auth.admin.name || auth.admin.email }}</span>
          <button class="btn btn-sm" @click="logout">退出</button>
        </div>
      </header>
      <main class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();
const sidebarCollapsed = ref(false);
const isDark = ref(false);

const titles: Record<string, string> = {
  dashboard: '运营看板',
  analytics: '数据分析',
  members: '会员管理',
  plans: '套餐管理',
  subscriptions: '订阅管理',
  payments: '支付管理',
  'activation-codes': '激活码管理',
  'audit-logs': '审计日志',
  backups: '数据库备份',
  admins: '管理员',
  settings: '系统设置',
};
const pageTitle = computed(() => titles[route.name as string] || '后台');
const initial = computed(() => {
  const a = auth.admin;
  if (!a) return 'A';
  return (a.name || a.email || 'A')[0].toUpperCase();
});

const themeBtn = computed(() => isDark.value ? '☀️' : '🌙');

function toggleTheme() {
  isDark.value = !isDark.value;
  document.documentElement.setAttribute('data-theme', isDark.value ? 'dark' : '');
  localStorage.setItem('theme', isDark.value ? 'dark' : 'light');
}

function logout() {
  auth.logout();
  router.push('/login');
}

onMounted(() => {
  isDark.value = localStorage.getItem('theme') === 'dark';
  if (isDark.value) document.documentElement.setAttribute('data-theme', 'dark');
});
</script>

<style scoped>
.layout {
  display: flex;
  height: 100%;
}
.sidebar {
  width: 220px;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  transition: width var(--transition);
  flex-shrink: 0;
}
.collapsed .sidebar {
  width: 64px;
}
.logo {
  padding: 20px 18px;
  font-size: 18px;
  font-weight: 700;
  white-space: nowrap;
}
.logo b {
  color: var(--primary);
  font-size: 13px;
  margin-left: 4px;
  font-weight: 500;
}
nav {
  display: flex;
  flex-direction: column;
  padding: 8px;
  gap: 2px;
  flex: 1;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 14px;
  border-radius: 9px;
  color: var(--text-2);
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  transition: all var(--transition);
}
.collapsed .nav-item {
  padding: 11px 12px;
  justify-content: center;
}
.nav-icon { font-size: 16px; flex-shrink: 0; }
.nav-label { overflow: hidden; }
.nav-item:hover {
  background: var(--bg);
  color: var(--text);
}
.nav-item.router-link-active {
  background: var(--primary-soft);
  color: var(--primary);
  font-weight: 600;
}
.sidebar-footer {
  padding: 12px;
  border-top: 1px solid var(--border);
}
.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.topbar {
  height: 60px;
  background: var(--topbar-bg);
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  transition: background var(--transition);
}
.topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.hamburger {
  font-size: 20px;
}
.title {
  font-size: 16px;
  font-weight: 600;
}
.theme-btn {
  font-size: 16px;
}
.user {
  display: flex;
  align-items: center;
  gap: 10px;
}
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
  flex-shrink: 0;
}
.uname {
  font-size: 13px;
  color: var(--text-2);
}
.content {
  flex: 1;
  overflow: auto;
  padding: 22px 24px;
}
@media (max-width: 768px) {
  .content { padding: 16px; }
}
</style>
