import { defineStore } from 'pinia';
import { ref } from 'vue';

export interface ToastItem {
  id: number;
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
}

let nextId = 0;

export const useToastStore = defineStore('toast', () => {
  const toasts = ref<ToastItem[]>([]);

  function add(type: ToastItem['type'], message: string, duration = 3000) {
    const id = nextId++;
    toasts.value.push({ id, type, message });
    setTimeout(() => remove(id), duration);
  }

  function remove(id: number) {
    toasts.value = toasts.value.filter(t => t.id !== id);
  }

  return { toasts, add, remove, success: (m: string) => add('success', m), error: (m: string) => add('error', m), warning: (m: string) => add('warning', m), info: (m: string) => add('info', m) };
});
