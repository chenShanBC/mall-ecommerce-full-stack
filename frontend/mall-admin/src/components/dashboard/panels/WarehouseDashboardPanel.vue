<template>
  <div class="role-panel role-panel--warehouse">
    <section class="top-warning" :class="warningLevel"><b>库存健康度置顶预警</b><span>{{ warningText }}</span><el-button link type="primary" @click="go('/stocks')">处理库存</el-button></section>
    <div class="metric-grid"><DashboardMetricCard v-for="item in metrics" :key="item.label" v-bind="item" theme="warehouse" @click="go(item.path, item.query)" /></div>

    <div class="panel-grid">
      <DashboardChartCard title="库存状态结构图" desc="展示正常、预警、缺货与对账异常 SKU 结构" :option="stockOption" :empty="!hasStock" @refresh="$emit('refresh')" @drilldown="handleStockDrilldown" />
      <DashboardChartCard title="库存变动结构图" desc="展示近期入库、出库、锁定、释放和调整流水" :option="changeOption" :empty="!hasChange" @refresh="$emit('refresh')" />
    </div>

    <div class="panel-grid panel-grid--2">
      <DashboardDataTable title="风险雷达 / 仓储待办" desc="聚合缺货、低库存、滞销高库存和库存对账风险" :columns="riskColumns" :rows="data.risks || []" :loading="loading" @refresh="$emit('refresh')" />
      <DashboardDataTable title="库存预警列表" desc="优先展示需要补货、促销清仓或人工处理的库存风险" :columns="warningColumns" :rows="data.warnings || []" :loading="loading" @refresh="$emit('refresh')" />
    </div>

    <div class="panel-grid panel-grid--2">
      <DashboardDataTable title="库存对账异常" desc="展示系统库存和实际库存不一致的记录" :columns="reconcileColumns" :rows="data.reconciliations || []" :loading="loading" @refresh="$emit('refresh')" />
      <DashboardDataTable title="最新库存流水" desc="展示近期库存调整、锁定与释放记录" :columns="logColumns" :rows="data.logs || []" :loading="loading" @refresh="$emit('refresh')" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import DashboardChartCard from '../base/DashboardChartCard.vue';
import DashboardDataTable from '../base/DashboardDataTable.vue';
import DashboardMetricCard from '../base/DashboardMetricCard.vue';

const props = defineProps({ data: { type: Object, default: () => ({}) }, loading: Boolean });
defineEmits(['refresh']);
const router = useRouter();
const summary = computed(() => props.data.summary || {});
const logs = computed(() => props.data.logs || []);
const warningLevel = computed(() => Number(summary.value.lowStockCount || 0) > 0 ? 'danger' : Number(summary.value.highStockCount || 0) > 0 ? 'warning' : 'normal');
const warningText = computed(() => warningLevel.value === 'danger' ? `存在 ${summary.value.outOfStockCount || summary.value.lowStockCount || 0} 个缺货/低库存 SKU，请优先补货` : warningLevel.value === 'warning' ? `存在 ${summary.value.stockDiffCount || summary.value.highStockCount || 0} 个库存风险，请关注对账与积压` : '库存状态正常');
const metrics = computed(() => [
  { label: '预警 SKU 数', value: summary.value.warningCount || props.data.warningTotal, hint: '低库存/高库存预警', danger: Number(summary.value.warningCount || props.data.warningTotal || 0) > 0, path: '/stocks' },
  { label: '缺货 SKU 数', value: summary.value.outOfStockCount, hint: '可售库存为 0', danger: Number(summary.value.outOfStockCount || 0) > 0, path: '/stocks', query: { warningStatus: 'LOW' } },
  { label: '滞销/高库存', value: summary.value.highStockCount, hint: '库存积压 SKU', danger: Number(summary.value.highStockCount || 0) > 0, path: '/stocks', query: { warningStatus: 'HIGH', page: 1 } },
  { label: '库存对账异常', value: summary.value.stockDiffCount, hint: 'Redis/数据库库存不一致', danger: Number(summary.value.stockDiffCount || 0) > 0, path: '/reconciliations', query: { tab: 'stock', stockStatus: 'INCONSISTENT', stockPage: 1 } },
  { label: '今日库存变动', value: summary.value.stockLogCount, hint: '库存流水数量', path: '/stock-logs' },
  { label: '待处理预警', value: summary.value.pendingWarningCount, hint: '未闭环库存预警', danger: Number(summary.value.pendingWarningCount || 0) > 0, path: '/stocks' },
]);
const stockOption = computed(() => ({ tooltip: { trigger: 'item' }, legend: { bottom: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11 } }, series: [{ type: 'pie', radius: ['48%', '68%'], center: ['50%', '42%'], avoidLabelOverlap: true, label: { formatter: '{b}', fontSize: 11 }, labelLine: { length: 8, length2: 8 }, data: [{ name: '正常', value: summary.value.normalCount || 0, itemStyle: { color: '#22c55e' } }, { name: '缺货', value: summary.value.outOfStockCount || 0, itemStyle: { color: '#dc2626' } }, { name: '低库存', value: summary.value.lowStockCount || 0, itemStyle: { color: '#f59e0b' } }, { name: '滞销/高库存', value: summary.value.highStockCount || 0, itemStyle: { color: '#0ea5e9' } }, { name: '对账异常', value: summary.value.stockDiffCount || 0, itemStyle: { color: '#7c3aed' } }] }] }));
const changeGroups = computed(() => logs.value.reduce((acc, item) => { const key = item.changeType || 'UNKNOWN'; acc[key] = (acc[key] || 0) + 1; return acc; }, {}));
const changeOption = computed(() => ({ tooltip: { trigger: 'item' }, legend: { bottom: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11 } }, series: [{ type: 'pie', radius: ['48%', '68%'], center: ['50%', '42%'], label: { formatter: '{b}', fontSize: 11 }, data: Object.entries(changeGroups.value).map(([name, value]) => ({ name, value })) }] }));
const hasStock = computed(() => [summary.value.normalCount, summary.value.outOfStockCount, summary.value.lowStockCount, summary.value.highStockCount, summary.value.stockDiffCount].some(Boolean));
const hasChange = computed(() => Object.keys(changeGroups.value).length > 0);
const riskColumns = [{ prop: 'label', label: '风险/待办', minWidth: 150 }, { prop: 'count', label: '数量', width: 90 }, { prop: 'level', label: '等级', type: 'tag', width: 100 }, { prop: 'desc', label: '说明', minWidth: 220 }];
const warningColumns = [{ prop: 'skuId', label: 'SKU ID', width: 100 }, { prop: 'productName', label: '商品名称', minWidth: 170 }, { prop: 'availableStock', label: '可售库存', width: 120 }, { prop: 'lockedStock', label: '锁定库存', width: 120 }, { prop: 'lowStockThreshold', label: '阈值', width: 100 }, { prop: 'warningStatus', label: '预警', type: 'tag', width: 120 }];
const reconcileColumns = [{ prop: 'skuId', label: 'SKU ID', width: 100 }, { prop: 'status', label: '状态', type: 'tag', width: 120 }, { prop: 'diffQuantity', label: '差异数量', width: 120 }, { prop: 'createTime', label: '创建时间', minWidth: 170 }];
const logColumns = [{ prop: 'skuId', label: 'SKU ID', width: 100 }, { prop: 'changeType', label: '类型', type: 'tag', width: 140 }, { prop: 'changeQuantity', label: '变更量', width: 110 }, { prop: 'operatorName', label: '操作人', width: 120 }, { prop: 'createTime', label: '时间', minWidth: 170 }];
const handleStockDrilldown = (event) => {
  if (event.name === '低库存') go('/stocks', { warningStatus: 'LOW', page: 1 });
  if (event.name === '滞销/高库存') go('/stocks', { warningStatus: 'HIGH', page: 1 });
  if (event.name === '对账异常') go('/reconciliations', { tab: 'stock', stockStatus: 'INCONSISTENT', stockPage: 1 });
};
const go = (path, query = {}) => router.push({ path, query });
</script>

<style scoped>
.role-panel{display:flex;flex-direction:column;gap:14px}.top-warning{display:flex;align-items:center;justify-content:space-between;gap:12px;border-radius:20px;padding:14px 18px;background:#ecfdf5;color:#166534;box-shadow:0 14px 32px rgba(15,23,42,.07)}.top-warning.warning{background:#fffbeb;color:#92400e}.top-warning.danger{background:#fef2f2;color:#b91c1c}.metric-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:14px}.panel-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px}.panel-grid--2{grid-template-columns:1fr 1fr}@media (max-width:1200px){.metric-grid{grid-template-columns:repeat(2,1fr)}.panel-grid,.panel-grid--2{grid-template-columns:1fr}}@media (max-width:640px){.metric-grid{grid-template-columns:1fr}.top-warning{flex-direction:column;align-items:flex-start}}
</style>