<template>
  <div class="role-panel role-panel--operations">
    <div class="metric-grid">
      <DashboardMetricCard v-for="item in metrics" :key="item.label" v-bind="item" theme="operations" @click="go(item.path, item.query)" />
    </div>

    <div class="panel-grid">
      <DashboardChartCard title="订单履约结构图" desc="展示待支付、待发货、已发货、已完成等履约状态" :option="fulfillmentOption" :empty="!hasFulfillment" @refresh="$emit('refresh')" />
      <DashboardChartCard title="订单运营趋势图" desc="展示订单、成交与售后走势" :option="trendOption" :empty="!hasTrend" @refresh="$emit('refresh')" @drilldown="handleTrendDrilldown" />
    </div>

    <div class="panel-grid panel-grid--2">
      <DashboardDataTable title="风险雷达 / 运营待办" desc="当天最需要优先处理的订单、支付和售后事项" :columns="todoColumns" :rows="todos" :loading="loading" @refresh="$emit('refresh')" />
      <DashboardDataTable title="最新订单" desc="最近订单动态，点击可快速跳转订单详情" :columns="orderColumns" :rows="orderRows" :loading="loading" @refresh="$emit('refresh')" />
    </div>

    <DashboardDataTable title="最新售后" desc="近期需要关注的售后申请" :columns="aftersaleColumns" :rows="aftersaleRows" :loading="loading" @refresh="$emit('refresh')" />
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
const overview = computed(() => props.data.overview || {});
const summary = computed(() => props.data.summary || {});
const orderRows = computed(() => props.data.orderRows || []);
const aftersaleRows = computed(() => props.data.aftersaleRows || []);
const todos = computed(() => props.data.todos || []);
const formatMoney = (cent) => `¥${(Number(cent || 0) / 100).toFixed(2)}`;
const metrics = computed(() => [
  { label: '今日订单数', value: summary.value.todayOrderCount, hint: '当前周期新增订单', path: '/orders' },
  { label: '成交金额', value: formatMoney(summary.value.paidAmountCent), hint: '已支付成功订单金额', path: '/orders', query: { status: 'PAID' } },
  { label: '待发货订单', value: summary.value.paidOrderCount || summary.value.pendingOrderCount, hint: '履约待处理', danger: Number(summary.value.paidOrderCount || summary.value.pendingOrderCount || 0) > 0, path: '/orders', query: { status: 'PAID' } },
  { label: '异常订单', value: summary.value.abnormalOrderCount, hint: '支付/履约异常风险', danger: Number(summary.value.abnormalOrderCount || 0) > 0, path: '/orders', query: { status: 'ABNORMAL' } },
  { label: '待处理售后', value: summary.value.pendingAftersaleCount, hint: '售后审核待办', danger: Number(summary.value.pendingAftersaleCount || 0) > 0, path: '/aftersales' },
  { label: '履约完成率', value: `${summary.value.fulfillmentRate || 0}%`, hint: '订单履约健康度', path: '/orders' },
]);
const trendData = computed(() => [summary.value.todayOrderCount || 0, summary.value.paidOrderCount || 0, summary.value.completedOrderCount || 0, summary.value.pendingAftersaleCount || 0]);
const hasTrend = computed(() => trendData.value.some(Boolean));
const trendOption = computed(() => ({ tooltip: { trigger: 'axis' }, legend: { top: 0 }, grid: { left: 20, right: 20, bottom: 20, containLabel: true }, xAxis: { type: 'category', data: ['订单', '支付', '完成', '售后'] }, yAxis: { type: 'value' }, series: [{ name: '运营趋势', type: 'line', smooth: true, symbolSize: 8, data: trendData.value, itemStyle: { color: '#2563eb' }, areaStyle: { color: 'rgba(37,99,235,.12)' } }] }));
const hasFulfillment = computed(() => [summary.value.pendingOrderCount, summary.value.paidOrderCount, summary.value.shippedOrderCount, summary.value.completedOrderCount, summary.value.abnormalOrderCount].some(Boolean));
const fulfillmentOption = computed(() => ({ tooltip: { trigger: 'item' }, legend: { bottom: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11 } }, series: [{ type: 'pie', radius: ['48%', '68%'], center: ['50%', '42%'], avoidLabelOverlap: true, label: { formatter: '{b}', fontSize: 11 }, labelLine: { length: 8, length2: 8 }, data: [{ name: '待支付', value: summary.value.pendingOrderCount || 0 }, { name: '待发货', value: summary.value.paidOrderCount || 0 }, { name: '已发货', value: summary.value.shippedOrderCount || 0 }, { name: '已完成', value: summary.value.completedOrderCount || 0 }, { name: '异常', value: summary.value.abnormalOrderCount || 0 }] }] }));
const orderColumns = [{ prop: 'orderNo', label: '订单号', minWidth: 170 }, { prop: 'status', label: '状态', type: 'tag', width: 120 }, { prop: 'payAmountCent', label: '金额', width: 120, format: 'money' }, { prop: 'createTime', label: '下单时间', minWidth: 170 }];
const aftersaleColumns = [{ prop: 'aftersaleNo', label: '售后单号', minWidth: 170 }, { prop: 'status', label: '状态', type: 'tag', width: 120 }, { prop: 'reason', label: '原因', minWidth: 180 }, { prop: 'createTime', label: '创建时间', minWidth: 170 }];
const todoColumns = [{ prop: 'label', label: '风险/待办', minWidth: 150 }, { prop: 'count', label: '数量', width: 90 }, { prop: 'level', label: '等级', type: 'tag', width: 100 }, { prop: 'desc', label: '说明', minWidth: 220 }];
const handleTrendDrilldown = (event) => {
  if (event.name === '支付') go('/orders', { status: 'PAID' });
  if (event.name === '完成') go('/orders', { status: 'COMPLETED' });
  if (event.name === '售后') go('/aftersales');
};
const go = (path, query = {}) => router.push({ path, query });
</script>

<style scoped>
.role-panel{display:flex;flex-direction:column;gap:14px}.metric-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:14px}.panel-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px}.panel-grid--2{grid-template-columns:1fr 1fr}@media (max-width:1200px){.metric-grid{grid-template-columns:repeat(2,1fr)}.panel-grid,.panel-grid--2{grid-template-columns:1fr}}@media (max-width:640px){.metric-grid{grid-template-columns:1fr}}
</style>