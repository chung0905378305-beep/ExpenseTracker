import { defineStore } from 'pinia';
import api from '@/api';

interface AdminInfo {
  id: string;
  name: string | null;
  email: string;
  role: string;
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('admin_token') || '',
    refreshToken: localStorage.getItem('admin_refresh_token') || '',
    admin: JSON.parse(localStorage.getItem('admin_info') || 'null') as AdminInfo | null,
  }),
  getters: {
    isLoggedIn: (s) => !!s.token,
  },
  actions: {
    setAuth(token: string, refreshToken: string, admin: AdminInfo) {
      this.token = token;
      this.refreshToken = refreshToken;
      this.admin = admin;
      localStorage.setItem('admin_token', token);
      localStorage.setItem('admin_refresh_token', refreshToken);
      localStorage.setItem('admin_info', JSON.stringify(admin));
    },
    updateToken(token: string) {
      this.token = token;
      localStorage.setItem('admin_token', token);
    },
    async tryRefresh(): Promise<boolean> {
      if (!this.refreshToken) return false;
      try {
        const { data } = await api.post('/auth/refresh', {}, {
          headers: { Authorization: `Bearer ${this.refreshToken}` },
        });
        this.updateToken(data.data.token);
        return true;
      } catch {
        return false;
      }
    },
    logout() {
      this.token = '';
      this.refreshToken = '';
      this.admin = null;
      localStorage.removeItem('admin_token');
      localStorage.removeItem('admin_refresh_token');
      localStorage.removeItem('admin_info');
    },
  },
});
