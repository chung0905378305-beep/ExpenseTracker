<template>
  <span class="tag" :class="cls">{{ display }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{ status: string; text?: string }>();

const map: Record<string, { cls: string; text: string }> = {
  active: { cls: 'tag-success', text: '有效' },
  trialing: { cls: 'tag-warning', text: '试用' },
  canceled: { cls: 'tag-muted', text: '已取消' },
  past_due: { cls: 'tag-danger', text: '扣费失败' },
  expired: { cls: 'tag-muted', text: '已过期' },
  paused: { cls: 'tag-warning', text: '暂停' },
  pending: { cls: 'tag-warning', text: '待生效' },
  success: { cls: 'tag-success', text: '成功' },
  failed: { cls: 'tag-danger', text: '失败' },
  refunded: { cls: 'tag-muted', text: '已退款' },
  superadmin: { cls: 'tag-primary', text: '超级管理员' },
  admin: { cls: 'tag-success', text: '管理员' },
  inactive: { cls: 'tag-danger', text: '已禁用' },
};

const m = computed(() => map[props.status] || { cls: 'tag-muted', text: props.status });
const cls = computed(() => m.value.cls);
const display = computed(() => props.text || m.value.text);
</script>
