<template>
  <div ref="el" :style="{ height: height }"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch } from 'vue';
import * as echarts from 'echarts';

const props = defineProps<{ option: any; height?: string }>();
const el = ref<HTMLElement>();
let chart: echarts.ECharts | null = null;

function render() {
  if (!el.value) return;
  if (!chart) chart = echarts.init(el.value);
  chart.setOption(props.option, true);
}

onMounted(render);
watch(() => props.option, render, { deep: true });
onBeforeUnmount(() => chart?.dispose());
</script>
