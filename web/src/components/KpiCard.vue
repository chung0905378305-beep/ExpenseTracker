<template>
  <div class="kpi card">
    <div class="label">{{ label }}</div>
    <div class="value">{{ value }}</div>
    <div class="sub" v-if="sub">{{ sub }}</div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ label: string; value: string | number; sub?: string }>();
</script>

<style scoped>
.kpi {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.label {
  color: var(--text-3);
  font-size: 13px;
}
.value {
  font-size: 26px;
  font-weight: 700;
}
.sub {
  color: var(--text-2);
  font-size: 12px;
}
</style>
