<template>
  <section class="chart-card">
    <div class="chart-card__head">
      <div>
        <h3>{{ title }}</h3>
        <p>{{ desc }}</p>
      </div>
      <div class="chart-card__actions">
        <el-button link type="primary" @click="$emit('refresh')">刷新</el-button>
        <el-button link @click="downloadChart">下载图表</el-button>
      </div>
    </div>
    <div v-if="empty" class="chart-card__empty">暂无图表数据</div>
    <div v-else ref="chartRef" class="chart-card__canvas"></div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, shallowRef, watch } from 'vue';
import * as echarts from 'echarts/core';
import { BarChart, FunnelChart, LineChart, PieChart } from 'echarts/charts';
import { DataZoomComponent, GridComponent, LegendComponent, TitleComponent, ToolboxComponent, TooltipComponent } from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

echarts.use([LineChart, BarChart, PieChart, FunnelChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent, ToolboxComponent, DataZoomComponent, CanvasRenderer]);

const props = defineProps({
  title: { type: String, required: true },
  desc: { type: String, default: '' },
  option: { type: Object, default: () => ({}) },
  empty: { type: Boolean, default: false },
});

const emit = defineEmits(['refresh', 'drilldown']);

const chartRef = ref(null);
const chart = shallowRef(null);
let resizeObserver;

const safeOption = computed(() => ({
  animationDuration: 420,
  tooltip: { confine: true, ...(props.option.tooltip || {}) },
  toolbox: { show: false, ...(props.option.toolbox || {}) },
  ...props.option,
}));

const disposeChart = () => {
  if (chart.value) {
    chart.value.dispose();
    chart.value = null;
  }
};

const bindChartEvents = () => {
  if (!chart.value) return;
  chart.value.off('click');
  chart.value.on('click', (event) => emit('drilldown', event));
};

const renderChart = async () => {
  await nextTick();
  if (!chartRef.value || props.empty) {
    disposeChart();
    return;
  }
  if (!chart.value) chart.value = echarts.init(chartRef.value, null, { renderer: 'canvas' });
  chart.value.setOption(safeOption.value, true);
  bindChartEvents();
};

const downloadChart = () => {
  if (!chart.value) return;
  const url = chart.value.getDataURL({ type: 'png', pixelRatio: 2, backgroundColor: '#ffffff' });
  const link = document.createElement('a');
  link.href = url;
  link.download = `${props.title}.png`;
  link.click();
};

watch(() => [props.option, props.empty], renderChart, { deep: true });

onMounted(() => {
  renderChart();
  resizeObserver = new ResizeObserver(() => chart.value?.resize());
  if (chartRef.value) resizeObserver.observe(chartRef.value);
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  disposeChart();
});
</script>

<style scoped>
.chart-card{min-height:280px;border-radius:22px;padding:18px;background:#fff;box-shadow:0 16px 36px rgba(15,23,42,.07)}.chart-card__head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;margin-bottom:10px}.chart-card h3{margin:0;color:#1e293b;font-size:17px}.chart-card p{margin:6px 0 0;color:#94a3b8;font-size:12px;line-height:1.5}.chart-card__actions{display:flex;align-items:center;gap:6px;flex-wrap:wrap}.chart-card__canvas{width:100%;height:210px}.chart-card__empty{height:210px;display:flex;align-items:center;justify-content:center;color:#94a3b8;border:1px dashed #dbe3ef;border-radius:16px;background:#f8fafc}@media (max-width:768px){.chart-card{padding:16px}.chart-card__head{flex-direction:column}.chart-card__canvas,.chart-card__empty{height:200px}}
</style>
