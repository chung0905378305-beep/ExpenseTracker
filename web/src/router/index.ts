import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import DefaultLayout from '@/layouts/DefaultLayout.vue';
import Login from '@/views/Login.vue';
import Dashboard from '@/views/Dashboard.vue';
import Members from '@/views/Members.vue';
import Plans from '@/views/Plans.vue';
import Subscriptions from '@/views/Subscriptions.vue';
import Payments from '@/views/Payments.vue';
import Settings from '@/views/Settings.vue';
import Admins from '@/views/Admins.vue';
import ActivationCodes from '@/views/ActivationCodes.vue';
import AuditLogs from '@/views/AuditLogs.vue';
import Backups from '@/views/Backups.vue';
import Analytics from '@/views/Analytics.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: Login, meta: { public: true } },
    {
      path: '/',
      component: DefaultLayout,
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', name: 'dashboard', component: Dashboard },
        { path: 'members', name: 'members', component: Members },
        { path: 'plans', name: 'plans', component: Plans },
        { path: 'subscriptions', name: 'subscriptions', component: Subscriptions },
        { path: 'payments', name: 'payments', component: Payments },
        { path: 'admins', name: 'admins', component: Admins },
        { path: 'activation-codes', name: 'activation-codes', component: ActivationCodes },
        { path: 'audit-logs', name: 'audit-logs', component: AuditLogs },
        { path: 'backups', name: 'backups', component: Backups },
        { path: 'analytics', name: 'analytics', component: Analytics },
        { path: 'settings', name: 'settings', component: Settings },
      ],
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
  ],
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  if (!to.meta.public && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } };
  }
  if (to.name === 'login' && auth.isLoggedIn) {
    return { name: 'dashboard' };
  }
});

export default router;
