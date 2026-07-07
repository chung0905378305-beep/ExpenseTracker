import axios from 'axios';
import { useAuthStore } from '@/stores/auth';
import router from '@/router';
import type { Router } from 'vue-router';

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
});

// 请求拦截：附 JWT
api.interceptors.request.use((config) => {
  const auth = useAuthStore();
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`;
  }
  return config;
});

// 响应拦截：401 → 尝试 refresh → 失败则登录
let isRefreshing = false;
let refreshSubscribers: ((token: string) => void)[] = [];

function onRefreshed(token: string) {
  refreshSubscribers.forEach(cb => cb(token));
  refreshSubscribers = [];
}

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const auth = useAuthStore();

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise(resolve => {
          refreshSubscribers.push((token: string) => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            resolve(api(originalRequest));
          });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const ok = await auth.tryRefresh();
        if (ok) {
          onRefreshed(auth.token);
          originalRequest.headers.Authorization = `Bearer ${auth.token}`;
          return api(originalRequest);
        }
      } finally {
        isRefreshing = false;
      }

      auth.logout();
      (router as Router).push('/login');
    }

    return Promise.reject(error);
  }
);

export default api;
