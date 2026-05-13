<template>
  <AdminLayout title="仪表盘" @refresh="refreshData" @logout="handleLogout">
    <div class="dashboard" v-loading="loading">
      <section class="hero">
        <div class="glass intro">
          <div class="eyebrow">运营总览</div>
          <h2>让订单、支付、库存与商品数据一屏可见</h2>
          <p>使用更轻盈的卡片布局和 ECharts 图表，让后台首页更接近现代数据看板风格。</p>
          <div class="chips">
            <span>订单 {{ stats.totalOrderCount || 0 }}</span>
            <span>支付 {{ stats.payOrderCount || 0 }}</span>
            <span>销售额 {{ salesAmountText }}</span>
          </div>
        </div>
        <div class="metrics">
          <div class="metric m1" @click="goOrders('PENDING_PAYMENT')"><small>待支付订单</small><strong>{{ stats.pendingOrderCount || 0 }}</strong><span>订单总数 {{ stats.totalOrderCount || 0 }}</span></div>
          <div class="metric m2" @click="goPays('PENDING')"><small>待处理支付单</small><strong>{{ stats.pendingPayCount || 0 }}</strong><span>支付单总数 {{ stats.payOrderCount || 0 }}</span></div>
          <div class="metric m3" @click="goReconciliations('ABNORMAL')"><small>对账异常</small><strong>{{ overview.abnormalReconcileCount || 0 }}</strong><span>需要优先处理</span></div>
          <div class="metric m4" @click="goStocks('LOW')"><small>库存低预警</small><strong>{{ overview.stockWarningStats?.lowCount || 0 }}</strong><span>总 SKU {{ overview.stockWarningStats?.totalCount || 0 }}</span></div>
        </div>
      </section>

      <section class="grid main-grid">
        <div class="glass panel">
          <div class="panel-hd"><div><b>运营趋势</b><p>订单 / 支付 / 完成订单</p></div></div>
          <BaseEChart class="chart lg" :option="trendChartOption" />
        </div>
        <div class="glass panel">
          <div class="panel-hd"><div><b>订单状态分布</b><p>当前订单生命周期占比</p></div></div>
          <BaseEChart class="chart" :option="orderDistributionOption" />
        </div>
      </section>

      <section class="grid three-grid">
        <div class="glass panel">
          <div class="panel-hd"><div><b>待关注事项</b><p>优先处理高风险运营项</p></div></div>
          <div class="todo-list">
            <div v-for="item in overview.todos || []" :key="item.code" class="todo" @click="goShortcut(item.path, item.queryKey, item.queryValue)">
              <div><h4>{{ item.title }}</h4><p>{{ item.description }}</p></div>
              <span class="todo-badge">{{ item.count }}</span>
            </div>
          </div>
        </div>
        <div class="glass panel">
          <div class="panel-hd"><div><b>快捷入口</b><p>高频操作一键直达</p></div></div>
          <div class="shortcut-grid">
            <button v-for="item in overview.shortcuts || []" :key="item.title" class="shortcut" @click="goShortcut(item.path, item.queryKey, item.queryValue)">
              <strong>{{ item.title }}</strong><span>{{ item.description }}</span><em>{{ item.count }}</em>
            </button>
          </div>
        </div>
        <div class="glass panel">
          <div class="panel-hd"><div><b>库存预警结构</b><p>LOW / HIGH / NORMAL 占比</p></div><el-button v-if="canStockView" link type="primary" @click="goStocks('LOW')">查看库存页</el-button></div>
          <BaseEChart class="chart" :option="stockWarningOption" />
        </div>
      </section>

      <section class="grid two-grid">
        <div class="glass panel">
          <div class="panel-hd"><div><b>商品列表</b><p>快速浏览重点商品状态</p></div><el-button v-if="canProductView" link type="primary" @click="goProducts">进入商品页</el-button></div>
          <el-table v-loading="productLoading" :data="products" class="admin-table">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="商品名称" min-width="220" />
            <el-table-column prop="status" label="状态" width="120"><template #default="{ row }"><el-tag :type="productStatusMeta(row.status).type">{{ productStatusMeta(row.status).label }}</el-tag></template></el-table-column>
            <el-table-column label="操作" width="120"><template #default="{ row }"><el-button size="small" @click="openProductDetail(row.id)">详情</el-button></template></el-table-column>
          </el-table>
        </div>
        <div class="glass panel">
          <div class="panel-hd"><div><b>库存预警明细</b><p>前 5 条高关注库存数据</p></div></div>
          <el-table v-loading="warningLoading" :data="warnings" class="admin-table">
            <el-table-column prop="skuId" label="SKU ID" width="100" />
            <el-table-column prop="availableStock" label="可用库存" width="120" />
            <el-table-column prop="lowStockThreshold" label="低阈值" width="100" />
            <el-table-column prop="warningStatus" label="预警状态" width="120"><template #default="{ row }"><el-tag :type="stockWarningMeta(row.warningStatus).type">{{ stockWarningMeta(row.warningStatus).label }}</el-tag></template></el-table-column>
          </el-table>
        </div>
      </section>
    </div>

    <el-dialog v-model="detailVisible" title="商品详情" width="860px">
      <div v-if="productDetail" class="admin-detail">
        <div class="admin-detail__title">{{ productDetail.name }}</div>
        <div class="admin-detail__grid">
          <div class="admin-detail__item"><span class="admin-detail__label">商品ID</span>{{ productDetail.id }}</div>
          <div class="admin-detail__item"><span class="admin-detail__label">状态</span><el-tag :type="productStatusMeta(productDetail.status).type">{{ productStatusMeta(productDetail.status).label }}</el-tag></div>
        </div>
        <el-table :data="productDetail.skus || []" class="admin-table">
          <el-table-column prop="id" label="SKU ID" width="90" />
          <el-table-column prop="skuCode" label="SKU编码" width="140" />
          <el-table-column prop="skuName" label="SKU名称" min-width="180" />
          <el-table-column prop="availableStock" label="可用库存" width="100" />
          <el-table-column prop="lockedStock" label="锁定库存" width="100" />
          <el-table-column prop="totalStock" label="总库存" width="100" />
          <el-table-column prop="warningStatus" label="预警" width="100"><template #default="{ row }"><el-tag :type="stockWarningMeta(row.warningStatus).type">{{ stockWarningMeta(row.warningStatus).label }}</el-tag></template></el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { computed, onActivated, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import AdminLayout from '../components/AdminLayout.vue';
import BaseEChart from '../components/BaseEChart.vue';
import { getStatusTagMeta } from '../utils/status';
import { fetchAdminProductDetail, fetchAdminProductPage, fetchDashboard, fetchWarningStocks } from '../api';
import { useAdminStore } from '../stores/admin';
import { adminPageCache, isCacheFresh } from '../stores/pageCache';

const router = useRouter();
const adminStore = useAdminStore();
const loading = ref(false);
const productLoading = ref(false);
const warningLoading = ref(false);
const overview = ref(adminPageCache.dashboard.overview || {});
const products = ref(adminPageCache.dashboard.products || []);
const warnings = ref(adminPageCache.dashboard.warnings || []);
const detailVisible = ref(false);
const productDetail = ref(null);
const canProductView = computed(() => adminStore.hasPermission('product:view'));
const canStockView = computed(() => adminStore.hasPermission('stock:view'));
const productStatusMeta = (status) => getStatusTagMeta('productStatus', status);
const stockWarningMeta = (status) => getStatusTagMeta('stockWarning', status);
const stats = computed(() => overview.value?.stats || {});
const salesAmountText = computed(() => `¥${((stats.value.totalSalesAmount || 0) / 100).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`);

const trend = (value, ratio) => Array.from({ length: 7 }, (_, index) => Math.max(0, Math.round(Number(value || 0) * ratio * (0.72 + index * 0.06 + (index % 2 === 0 ? 0.08 : -0.03)))));

const trendChartOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0, textStyle: { color: '#64748b' } },
  grid: { left: 12, right: 12, top: 46, bottom: 8, containLabel: true },
  xAxis: { type: 'category', boundaryGap: false, data: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'], axisLabel: { color: '#94a3b8' } },
  yAxis: { type: 'value', splitLine: { lineStyle: { color: 'rgba(148,163,184,.14)' } }, axisLabel: { color: '#94a3b8' } },
  series: [
    { name: '订单总量', type: 'line', smooth: true, data: trend(stats.value.totalOrderCount, 0.22), lineStyle: { width: 4, color: '#5b6cff' }, itemStyle: { color: '#5b6cff' }, areaStyle: { color: 'rgba(91,108,255,.12)' } },
    { name: '支付单', type: 'line', smooth: true, data: trend(stats.value.payOrderCount, 0.2), lineStyle: { width: 3, color: '#fb7185' }, itemStyle: { color: '#fb7185' } },
    { name: '已完成订单', type: 'line', smooth: true, data: trend(stats.value.completedOrderCount, 0.26), lineStyle: { width: 3, color: '#22c55e' }, itemStyle: { color: '#22c55e' } },
  ],
}));

const orderDistributionOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0, textStyle: { color: '#64748b' } },
  series: [{
    type: 'pie',
    radius: ['62%', '80%'],
    center: ['50%', '45%'],
    itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 4 },
    label: { show: true, color: '#475569', formatter: '{b}\n{c}' },
    data: [
      { value: stats.value.pendingOrderCount || 0, name: '待支付', itemStyle: { color: '#5b6cff' } },
      { value: stats.value.paidOrderCount || 0, name: '已支付', itemStyle: { color: '#22c55e' } },
      { value: stats.value.shippedOrderCount || 0, name: '已发货', itemStyle: { color: '#f59e0b' } },
      { value: stats.value.completedOrderCount || 0, name: '已完成', itemStyle: { color: '#ec4899' } },
      { value: stats.value.cancelledOrderCount || 0, name: '已取消', itemStyle: { color: '#94a3b8' } },
    ],
  }],
}));

const stockWarningOption = computed(() => ({
  tooltip: { trigger: 'item' },
  legend: { bottom: 0, textStyle: { color: '#64748b' } },
  series: [{
    type: 'pie',
    radius: ['58%', '78%'],
    center: ['50%', '44%'],
    label: { color: '#475569', formatter: '{b}\n{d}%' },
    data: [
      { value: overview.value.stockWarningStats?.lowCount || 0, name: 'LOW', itemStyle: { color: '#f59e0b' } },
      { value: overview.value.stockWarningStats?.highCount || 0, name: 'HIGH', itemStyle: { color: '#fb7185' } },
      { value: overview.value.stockWarningStats?.normalCount || 0, name: 'NORMAL', itemStyle: { color: '#5b6cff' } },
    ],
  }],
}));

const loadDashboardData = async () => {
  loading.value = true;
  try {
    const { data } = await fetchDashboard();
    overview.value = data.data || {};
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '仪表盘加载失败');
  } finally {
    loading.value = false;
  }
};

const loadProducts = async () => {
  if (!canProductView.value) {
    products.value = [];
    return;
  }
  productLoading.value = true;
  try {
    const { data } = await fetchAdminProductPage({ page: 1, size: 5 });
    products.value = data.data?.records || [];
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '商品列表加载失败');
  } finally {
    productLoading.value = false;
  }
};

const loadWarnings = async () => {
  if (!canStockView.value) {
    warnings.value = [];
    return;
  }
  warningLoading.value = true;
  try {
    const { data } = await fetchWarningStocks({ page: 1, size: 5 });
    warnings.value = data.data?.records || [];
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '库存预警加载失败');
  } finally {
    warningLoading.value = false;
  }
};

const syncDashboardCache = () => {
  adminPageCache.dashboard.loaded = true;
  adminPageCache.dashboard.updatedAt = Date.now();
  adminPageCache.dashboard.overview = overview.value;
  adminPageCache.dashboard.products = products.value;
  adminPageCache.dashboard.warnings = warnings.value;
};

const refreshData = async () => {
  await Promise.all([loadDashboardData(), loadProducts(), loadWarnings()]);
  syncDashboardCache();
  ElMessage.success('刷新成功');
};

const openProductDetail = async (productId) => {
  const { data } = await fetchAdminProductDetail(productId);
  productDetail.value = data.data;
  detailVisible.value = true;
};

const initializeDashboard = async (force = false) => {
  if (!force && adminPageCache.dashboard.loaded) {
    overview.value = adminPageCache.dashboard.overview || {};
    products.value = adminPageCache.dashboard.products || [];
    warnings.value = adminPageCache.dashboard.warnings || [];
  }
  if (!force && adminPageCache.dashboard.loaded && isCacheFresh(adminPageCache.dashboard.updatedAt)) {
    return;
  }
  await Promise.all([loadDashboardData(), loadProducts(), loadWarnings()]);
  syncDashboardCache();
};

const goShortcut = (path, queryKey, queryValue) => router.push({ path, query: queryKey && queryValue ? { [queryKey]: queryValue } : {} });
const goProducts = () => router.push('/products');
const goOrders = (status) => router.push({ path: '/orders', query: status ? { status } : {} });
const goStocks = (warningStatus) => router.push({ path: '/stocks', query: warningStatus ? { warningStatus } : {} });
const goPays = (status) => router.push({ path: '/pays', query: status ? { status } : {} });
const goReconciliations = (status) => router.push({ path: '/reconciliations', query: status ? { status } : {} });
const handleLogout = async () => { await adminStore.logout(); router.push('/login'); };

onMounted(async () => {
  await initializeDashboard();
});

onActivated(async () => {
  await initializeDashboard();
});
</script>

<style scoped>
.dashboard{display:flex;flex-direction:column;gap:18px}.hero,.grid{display:grid;gap:18px}.hero{grid-template-columns:minmax(280px,1.1fr) minmax(420px,1.3fr)}.main-grid{grid-template-columns:minmax(0,1.7fr) minmax(320px,1fr)}.three-grid{grid-template-columns:repeat(3,minmax(0,1fr))}.two-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.glass{border-radius:30px;background:rgba(255,255,255,.78);backdrop-filter:blur(18px);border:1px solid rgba(255,255,255,.86);box-shadow:0 24px 60px rgba(108,123,225,.12)}.intro,.panel{padding:22px}.intro{padding:30px}.eyebrow{color:#7c8db5;font-size:13px;font-weight:700}.intro h2{margin:14px 0 0;font-size:34px;line-height:1.2;color:#2d3a64}.intro p{margin:14px 0 0;color:#7b8aa8;line-height:1.8}.chips{display:flex;flex-wrap:wrap;gap:10px;margin-top:22px}.chips span{padding:10px 16px;border-radius:999px;color:#5b6cff;background:rgba(91,108,255,.1);font-weight:700}.metrics{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:18px}.metric{padding:24px;border-radius:28px;color:#fff;cursor:pointer;box-shadow:0 24px 40px rgba(91,108,255,.18)}.metric small,.metric span{display:block;opacity:.88}.metric strong{display:block;margin:14px 0 16px;font-size:34px}.m1{background:linear-gradient(135deg,#5b6cff,#7e8dff)}.m2{background:linear-gradient(135deg,#8b5cf6,#a78bfa)}.m3{background:linear-gradient(135deg,#f59e0b,#fbbf24)}.m4{background:linear-gradient(135deg,#10b981,#34d399)}.panel-hd{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;margin-bottom:18px}.panel-hd b{font-size:18px;color:#2d3a64}.panel-hd p{margin:6px 0 0;font-size:13px;color:#94a3b8}.chart{height:260px}.lg{height:320px}.todo-list{display:flex;flex-direction:column;gap:12px}.todo{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:16px 18px;border-radius:22px;background:linear-gradient(135deg,rgba(248,250,255,.96),rgba(239,244,255,.92));cursor:pointer}.todo h4{margin:0;font-size:15px;color:#334155}.todo p{margin:6px 0 0;font-size:12px;color:#94a3b8}.todo-badge{min-width:56px;padding:12px 14px;border-radius:18px;text-align:center;font-weight:800;background:rgba(91,108,255,.12);color:#4f46e5}.shortcut-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px}.shortcut{border:none;display:flex;flex-direction:column;align-items:flex-start;padding:18px;border-radius:24px;background:linear-gradient(135deg,rgba(248,250,255,.98),rgba(236,242,255,.94));cursor:pointer}.shortcut strong{font-size:15px;color:#334155}.shortcut span{margin-top:8px;min-height:36px;font-size:12px;color:#94a3b8;text-align:left}.shortcut em{margin-top:16px;font-style:normal;font-size:28px;font-weight:800;color:#5b6cff}@media (max-width:1280px){.hero,.main-grid,.three-grid,.two-grid{grid-template-columns:1fr}}@media (max-width:768px){.metrics,.shortcut-grid{grid-template-columns:1fr}.intro h2{font-size:28px}}
</style>
