<template>
  <div class="role-panel role-panel--finance">
    <div class="metric-grid">
      <DashboardMetricCard v-for="item in metrics" :key="item.label" v-bind="item" theme="finance" @click="go(item.path, item.query)" />
    </div>

    <div class="panel-grid">
      <DashboardChartCard title="销售 / 收款趋势图" desc="MVP 版基于当前周期聚合展示实收、退款和净收入" :option="fundOption" :empty="!hasFund" @refresh="$emit('refresh')" @drilldown="handleFundDrilldown" />
      <DashboardChartCard title="对账闭环结构图" desc="展示未处理、挂账、已归档和异常差异分布" :option="diffOption" :empty="!hasDiff" @refresh="$emit('refresh')" />
    </div>

    <div class="panel-grid panel-grid--2">
      <DashboardDataTable title="风险雷达 / 财务待办" desc="聚合未处理差异、挂账、回调失败和退款状态风险" :columns="riskColumns" :rows="data.risks || []" :loading="loading" @refresh="$emit('refresh')" />
      <DashboardDataTable title="最新对账差异" desc="优先处理未闭环的账务差异" :columns="diffColumns" :rows="data.diffs || []" :loading="loading" @refresh="$emit('refresh')" />
    </div>

    <div class="panel-grid panel-grid--3">
      <DashboardDataTable title="最新支付单" desc="最近支付流水和支付状态" :columns="payColumns" :rows="data.pays || []" :loading="loading" @refresh="$emit('refresh')" />
      <DashboardDataTable title="最新退款" desc="近期退款记录和退款状态" :columns="refundColumns" :rows="data.refunds || []" :loading="loading" @refresh="$emit('refresh')" />
      <DashboardDataTable title="支付回调监控" desc="展示近期支付通知状态，便于排查资金异常" :columns="callbackColumns" :rows="data.callbacks || []" :loading="loading" @refresh="$emit('refresh')" />
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
const overview = computed(() => props.data.overview || {});
const formatMoney = (cent) => `¥${(Number(cent || 0) / 100).toFixed(2)}`;
const metrics = computed(() => [
  { label: '实收金额', value: formatMoney(summary.value.paidAmountCent), hint: '支付成功金额', path: '/pays' },
  { label: '退款金额', value: formatMoney(summary.value.refundAmountCent), hint: '退款成功/处理中金额', path: '/pays', query: { tab: 'refunds' } },
  { label: '净收入', value: formatMoney(summary.value.netIncomeCent), hint: '实收 - 退款', path: '/pays' },
  { label: '支付单数', value: summary.value.payOrderCount, hint: '当前周期支付单', path: '/pays' },
  { label: '未处理差异', value: summary.value.pendingReconcileCount || summary.value.abnormalReconcileCount, hint: '对账待办', danger: Number(summary.value.pendingReconcileCount || summary.value.abnormalReconcileCount || 0) > 0, path: '/reconciliations' },
  { label: '挂账任务', value: summary.value.hangingCount, hint: '挂账待闭环', danger: Number(summary.value.hangingCount || 0) > 0, path: '/reconciliations' },
]);
const fundOption = computed(() => ({ tooltip: { trigger: 'axis' }, legend: { top: 0 }, grid: { left: 20, right: 20, bottom: 20, containLabel: true }, xAxis: { type: 'category', data: ['实收', '退款', '净收入'] }, yAxis: { type: 'value' }, series: [{ name: '金额', type: 'bar', barWidth: 34, data: [summary.value.paidAmountCent || 0, summary.value.refundAmountCent || 0, summary.value.netIncomeCent || 0].map((item) => Math.round(Number(item || 0) / 100)), itemStyle: { borderRadius: [10, 10, 0, 0], color: '#7c3aed' } }] }));
const diffOption = computed(() => ({ tooltip: { trigger: 'item' }, legend: { bottom: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11 } }, series: [{ type: 'pie', radius: ['48%', '68%'], center: ['50%', '42%'], avoidLabelOverlap: true, label: { formatter: '{b}', fontSize: 11 }, labelLine: { length: 8, length2: 8 }, data: [{ name: '待处理', value: summary.value.pendingReconcileCount || 0 }, { name: '挂账中', value: summary.value.hangingCount || 0 }, { name: '已归档', value: summary.value.archivedCount || 0 }, { name: '异常差异', value: summary.value.abnormalReconcileCount || 0 }] }] }));
const hasFund = computed(() => [summary.value.paidAmountCent, summary.value.refundAmountCent, summary.value.netIncomeCent].some(Boolean));
const hasDiff = computed(() => [summary.value.pendingReconcileCount, summary.value.hangingCount, summary.value.archivedCount, summary.value.abnormalReconcileCount].some(Boolean));
const riskColumns = [{ prop: 'label', label: '风险/待办', minWidth: 150 }, { prop: 'count', label: '数量', width: 90 }, { prop: 'level', label: '等级', type: 'tag', width: 100 }, { prop: 'desc', label: '说明', minWidth: 220 }];
const payColumns = [{ prop: 'orderNo', label: '订单号', minWidth: 170 }, { prop: 'payStatus', label: '支付状态', type: 'tag', width: 130 }, { prop: 'payAmountCent', label: '金额', width: 120, format: 'money' }, { prop: 'createTime', label: '创建时间', minWidth: 170 }];
const refundColumns = [{ prop: 'refundNo', label: '退款单号', minWidth: 170 }, { prop: 'orderNo', label: '订单号', minWidth: 170 }, { prop: 'status', label: '状态', type: 'tag', width: 120 }, { prop: 'refundAmountCent', label: '金额', width: 120, format: 'money' }];
const diffColumns = [{ prop: 'orderNo', label: '订单号', minWidth: 170 }, { prop: 'diffType', label: '差异类型', type: 'tag', width: 150 }, { prop: 'status', label: '状态', type: 'tag', width: 120 }, { prop: 'diffAmountCent', label: '差异金额', width: 120, format: 'money' }];
const callbackColumns = [{ prop: 'orderNo', label: '订单号', minWidth: 170 }, { prop: 'status', label: '状态', type: 'tag', width: 120 }, { prop: 'tradeNo', label: '交易号', minWidth: 170 }, { prop: 'createTime', label: '回调时间', minWidth: 170 }];
const handleFundDrilldown = (event) => {
  if (event.name === '退款') go('/pays', { tab: 'refunds' });
  if (event.name === '净收入') go('/reconciliations');
};
const go = (path, query = {}) => router.push({ path, query });
</script>

<style scoped>
.role-panel{display:flex;flex-direction:column;gap:14px}.metric-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:14px}.panel-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px}.panel-grid--2{grid-template-columns:1fr 1fr}.panel-grid--3{grid-template-columns:repeat(3,minmax(0,1fr))}@media (max-width:1280px){.metric-grid{grid-template-columns:repeat(2,1fr)}.panel-grid,.panel-grid--2,.panel-grid--3{grid-template-columns:1fr}}@media (max-width:640px){.metric-grid{grid-template-columns:1fr}}
</style>