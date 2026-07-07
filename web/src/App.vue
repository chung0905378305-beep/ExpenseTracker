<template>
  <div class="toast-container" v-if="toast.toasts.length">
    <div
      v-for="t in toast.toasts"
      :key="t.id"
      :class="['toast', `toast-${t.type}`]"
      @click="toast.remove(t.id)"
    >{{ t.message }}</div>
  </div>
  <router-view v-slot="{ Component }">
    <transition name="fade" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>
</template>

<script setup lang="ts">
import { useToastStore } from '@/stores/toast';
const toast = useToastStore();
</script>
