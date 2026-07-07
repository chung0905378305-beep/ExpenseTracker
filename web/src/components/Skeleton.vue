<template>
  <div class="skeleton-loader">
    <div v-if="type === 'table'" class="skel-table">
      <div v-for="i in rows" :key="i" class="skel-row">
        <div class="skeleton skeleton-text" v-for="j in cols" :key="j" :style="{ width: randomWidth() }" />
      </div>
    </div>
    <div v-else-if="type === 'card'" class="skel-grid">
      <div v-for="i in count" :key="i" class="skeleton skeleton-card" />
    </div>
    <div v-else-if="type === 'detail'" class="skel-detail">
      <div class="skeleton skeleton-title" />
      <div class="skeleton skeleton-text" v-for="i in rows" :key="i" :style="{ width: randomWidth() }" />
    </div>
    <div v-else-if="type === 'chart'" class="skel-chart">
      <div class="skeleton skeleton-card" style="height: 200px" />
    </div>
    <div v-else class="skel-detail">
      <div class="skeleton skeleton-text" v-for="i in rows" :key="i" :style="{ width: randomWidth() }" />
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  type?: 'table' | 'card' | 'detail' | 'chart';
  rows?: number;
  cols?: number;
  count?: number;
}>(), {
  type: 'detail',
  rows: 4,
  cols: 4,
  count: 3,
});

function randomWidth() {
  const w = 40 + Math.random() * 55;
  return `${w}%`;
}
</script>

<style scoped>
.skel-table { display: flex; flex-direction: column; gap: 8px; }
.skel-row { display: flex; gap: 12px; }
.skel-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 14px; }
.skel-detail { display: flex; flex-direction: column; }
.skel-chart { padding: 10px 0; }
</style>
