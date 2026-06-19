<template>
  <div ref="chartRef" class="chart-container"></div>
</template>

<script setup>
import * as echarts from 'echarts';
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';

const props = defineProps({
  option: {
    type: Object,
    required: true,
  },
  autoresize: {
    type: Boolean,
    default: true,
  },
});

const emit = defineEmits(['chart-click']);

const chartRef = ref(null);
let chartInstance = null;
let resizeObserver = null;

const renderChart = async () => {
  await nextTick();
  if (!chartRef.value) return;
  if (!chartInstance) {
    chartInstance = echarts.init(chartRef.value);
    chartInstance.on('click', (params) => emit('chart-click', params));
  }
  chartInstance.setOption(props.option, true);
  chartInstance.resize();
};

const handleResize = () => {
  chartInstance?.resize();
};

onMounted(async () => {
  await renderChart();
  if (props.autoresize && typeof ResizeObserver !== 'undefined' && chartRef.value) {
    resizeObserver = new ResizeObserver(() => {
      handleResize();
    });
    resizeObserver.observe(chartRef.value);
  }
  window.addEventListener('resize', handleResize);
});

watch(() => props.option, renderChart, { deep: true });

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  resizeObserver?.disconnect();
  resizeObserver = null;
  chartInstance?.dispose();
  chartInstance = null;
});
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: 100%;
  min-height: 220px;
}
</style>
