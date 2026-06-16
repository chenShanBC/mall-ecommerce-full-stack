<template>
  <div class="role-panel role-panel--products">
    <div class="metric-grid"><DashboardMetricCard v-for="item in metrics" :key="item.label" v-bind="item" theme="products" @click="go(item.path, item.query)" /></div>

    <div class="panel-grid">
      <DashboardChartCard title="商品状态结构图" desc="展示上架、下架、热销和滞销商品结构" :option="statusOption" :empty="!hasStatus" @refresh="$emit('refresh')" />
      <DashboardChartCard title="销量 / 销售额排行" desc="展示商品销量与销售额 TOP 表现" :option="salesOption" :empty="!hasSales" @refresh="$emit('refresh')" @drilldown="handleProductDrilldown" />
    </div>

    <div class="panel-grid panel-grid--2">
      <DashboardDataTable title="商品销售排行" desc="按销量排序展示近期表现较好的商品" :columns="productColumns" :rows="hotProducts" :loading="loading" @refresh="$emit('refresh')" />
      <DashboardDataTable title="风险雷达 / 商品待办" desc="聚合热销缺货、滞销积压和商品优化风险" :columns="riskColumns" :rows="data.risks || []" :loading="loading" @refresh="$emit('refresh')" />
    </div>

    <div class="panel-grid panel-grid--2">
      <DashboardDataTable title="商品经营建议" desc="根据销量、销售额和库存风险生成的经营动作建议" :columns="suggestionColumns" :rows="suggestions" :loading="loading" @refresh="$emit('refresh')" />
      <DashboardDataTable title="滞销商品关注" desc="低销量或销售额偏低商品，建议结合库存做促销或下架判断" :columns="productColumns" :rows="slowProducts" :loading="loading" @refresh="$emit('refresh')" />
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
const products = computed(() => props.data.products || []);
const hotThreshold = computed(() => Number(summary.value.hotThreshold || props.data.threshold?.hotThreshold || 100));
const slowThreshold = computed(() => Number(summary.value.slowThreshold || props.data.threshold?.slowThreshold || 1));
const formatMoney = (cent) => `¥${(Number(cent || 0) / 100).toFixed(2)}`;
const hotProducts = computed(() => [...products.value].sort((a, b) => Number(b.salesCount || 0) - Number(a.salesCount || 0)).slice(0, 5));
const slowProducts = computed(() => [...products.value].sort((a, b) => Number(a.salesCount || 0) - Number(b.salesCount || 0)).slice(0, 5));
const hotCount = computed(() => summary.value.hotCount ?? products.value.filter((item) => Number(item.salesCount || 0) >= hotThreshold.value).length);
const slowCount = computed(() => summary.value.slowCount ?? products.value.filter((item) => Number(item.salesCount || 0) <= slowThreshold.value).length);
const metrics = computed(() => [
  { label: '商品总数', value: summary.value.productTotal || props.data.productTotal, hint: '商品池规模', path: '/products' },
  { label: '上架商品数', value: summary.value.onSaleCount, hint: '当前销售中', path: '/products', query: { status: 'ON_SALE' } },
  { label: '下架商品数', value: summary.value.offSaleCount, hint: '待优化商品', path: '/products', query: { status: 'OFF_SALE' } },
  { label: '总销量', value: summary.value.totalSalesCount, hint: '当前样本销量合计', path: '/products' },
  { label: '商品销售额', value: formatMoney(summary.value.totalSalesAmountCent), hint: '当前样本销售额', path: '/products' },
  { label: '热销 / 滞销', value: `${hotCount.value}/${slowCount.value}`, hint: '热销与滞销商品数', danger: slowCount.value > 0, path: '/products' },
  { label: '热销低库存', value: summary.value.lowStockHotCount, hint: '建议优先补货', danger: Number(summary.value.lowStockHotCount || 0) > 0, path: '/stocks' },
]);
const statusOption = computed(() => ({ tooltip: { trigger: 'item' }, legend: { bottom: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11 } }, series: [{ type: 'pie', radius: ['48%', '68%'], center: ['50%', '42%'], label: { formatter: '{b}', fontSize: 11 }, data: [{ name: '上架', value: summary.value.onSaleCount || 0, itemStyle: { color: '#f97316' } }, { name: '下架', value: summary.value.offSaleCount || 0, itemStyle: { color: '#94a3b8' } }, { name: '热销', value: hotCount.value, itemStyle: { color: '#ef4444' } }, { name: '滞销', value: slowCount.value, itemStyle: { color: '#64748b' } }, { name: '低库存热销', value: summary.value.lowStockHotCount || 0, itemStyle: { color: '#0ea5e9' } }] }] }));
const salesOption = computed(() => ({ tooltip: { trigger: 'axis' }, legend: { top: 0 }, grid: { left: 20, right: 42, bottom: 20, containLabel: true }, xAxis: { type: 'category', data: hotProducts.value.map((item) => item.name || `商品${item.id}`) }, yAxis: [{ type: 'value', name: '销量' }, { type: 'value', name: '销售额' }], series: [{ name: '销量', type: 'bar', data: hotProducts.value.map((item) => item.salesCount || 0), itemStyle: { color: '#f97316', borderRadius: [10, 10, 0, 0] } }, { name: '销售额', type: 'line', yAxisIndex: 1, smooth: true, data: hotProducts.value.map((item) => Math.round(Number(item.salesAmountCent || 0) / 100)), itemStyle: { color: '#ea580c' } }] }));
const suggestions = computed(() => products.value.slice(0, 8).map((item) => {
  const sales = Number(item.salesCount || 0);
  const stock = Number(item.availableStock || item.stock || 0);
  const name = item.name || item.productName || item.spuName || `商品${item.id}`;
  if (sales >= hotThreshold.value && stock <= 10) return { name, issue: '高销量低库存', action: '建议补货', level: 'HIGH' };
  if (sales <= slowThreshold.value && stock >= 50) return { name, issue: '低销量高库存', action: '建议促销', level: 'WARNING' };
  if (sales <= slowThreshold.value) return { name, issue: '低动销商品', action: '优化图文/价格', level: 'LOW' };
  return { name, issue: '销售健康', action: '保持供给', level: 'NORMAL' };
}));
const hasStatus = computed(() => [summary.value.onSaleCount, summary.value.offSaleCount, hotCount.value, slowCount.value].some(Boolean));
const hasSales = computed(() => hotProducts.value.length > 0);
const riskColumns = [{ prop: 'label', label: '风险/待办', minWidth: 150 }, { prop: 'count', label: '数量', width: 90 }, { prop: 'level', label: '等级', type: 'tag', width: 100 }, { prop: 'desc', label: '说明', minWidth: 220 }];
const productColumns = [{ prop: 'id', label: 'ID', width: 80 }, { prop: 'name', label: '商品名称', minWidth: 180 }, { prop: 'status', label: '状态', type: 'tag', width: 110 }, { prop: 'salesCount', label: '销量', width: 100 }, { prop: 'salesAmountCent', label: '销售额', width: 120, format: 'money' }];
const suggestionColumns = [{ prop: 'name', label: '商品名称', minWidth: 180 }, { prop: 'issue', label: '问题', minWidth: 140 }, { prop: 'action', label: '建议动作', minWidth: 140 }, { prop: 'level', label: '风险等级', type: 'tag', width: 120 }];
const handleProductDrilldown = (event) => {
  const target = products.value.find((item) => (item.name || `商品${item.id}`) === event.name);
  go('/products', target?.id ? { keyword: target.name || target.id } : {});
};
const go = (path, query = {}) => router.push({ path, query });
</script>

<style scoped>
.role-panel{display:flex;flex-direction:column;gap:14px}.metric-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:14px}.panel-grid{display:grid;grid-template-columns:1fr 1fr;gap:14px}.panel-grid--2{grid-template-columns:1fr 1fr}@media (max-width:1200px){.metric-grid{grid-template-columns:repeat(2,1fr)}.panel-grid,.panel-grid--2{grid-template-columns:1fr}}@media (max-width:640px){.metric-grid{grid-template-columns:1fr}}
</style>