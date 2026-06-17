<template>
  <AdminLayout title="仪表盘" @refresh="refreshCurrentRole" @logout="handleLogout">
    <div class="dashboard-page" :class="[`dashboard-page--${currentRoleMeta.theme}`]">
      <section class="dashboard-top-grid">
        <article class="role-dashboard dashboard-card">
          <div class="card-head card-head--compact">
            <div>
              <div class="hero-eyebrow">MALLFEI COMMAND CENTER</div>
              <h2>{{ currentRoleMeta.title }}</h2>
              <p>{{ heroDescription }}</p>
            </div>
            <button class="dashboard-refresh-btn" type="button" @click="refreshCurrentRole">刷新</button>
          </div>
          <div class="role-switcher">
            <button
              v-for="role in visibleRoles"
              :key="role.code"
              class="role-switcher__item"
              :class="[`role-switcher__item--${role.theme}`, { active: activeRole === role.code }]"
              type="button"
              @click="switchDashboardRole(role.code)"
            >
              <small>{{ role.label }}</small>
              <strong>{{ role.title }}</strong>
              <span>{{ role.intro }}</span>
            </button>
          </div>
          <div class="summary-grid summary-grid--in-card">
            <button
              v-for="card in summaryCards"
              :key="card.label"
              class="summary-card summary-card--button"
              type="button"
              @click="handleSummaryCardClick(card)"
            >
              <div class="summary-card__label">{{ card.label }}</div>
              <div class="summary-card__value">{{ card.value }}</div>
              <div class="summary-card__desc">{{ card.desc }}</div>
            </button>
          </div>
        </article>

        <article class="management-dashboard dashboard-card" :class="`management-dashboard--${currentRoleMeta.theme}`">
          <div class="management-head">
            <h2>{{ managementMeta.title }}</h2>
            <p>{{ managementMeta.desc }}</p>
          </div>
          <div class="management-hero">
            <div class="management-hero__main">
              <strong>{{ managementMeta.primaryValue }}</strong>
              <span>{{ managementMeta.primaryLabel }}</span>
            </div>
            <div class="management-hero__list">
              <div v-for="item in managementMeta.metrics" :key="item.label" class="management-metric-row">
                <span>{{ item.label }}</span>
                <b>{{ item.value }}</b>
              </div>
            </div>
          </div>
          <div class="management-footer">
            <div class="management-tips">
              <div v-for="item in managementMeta.tips" :key="item.label" class="management-tip">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
            <el-button size="small" type="primary" @click="managementMeta.action?.()">{{ managementMeta.actionText }}</el-button>
          </div>
        </article>
      </section>

      <el-alert v-if="errors[activeRole]" :title="errors[activeRole]" type="error" show-icon :closable="false" />

      <section class="dashboard-grid">
        <div class="dashboard-main">
          <article class="dashboard-card chart-card">
            <div class="card-head">
              <div>
                <h3>{{ mainChartMeta.title }}</h3>
                <p>{{ mainChartMeta.desc }}</p>
              </div>
              <el-button size="small" type="primary" plain @click="refreshCurrentRole">刷新数据</el-button>
            </div>
            <BaseEChart class="chart-box chart-box--lg" :option="mainChartOption" />
          </article>

          <article class="dashboard-card table-card" v-if="hasTableRows">
            <div class="card-head">
              <div>
                <h3>{{ tableMeta.title }}</h3>
                <p>{{ tableMeta.desc }}</p>
              </div>
              <el-button v-if="tableMeta.actionText" size="small" link type="primary" @click="tableMeta.action?.()">{{ tableMeta.actionText }}</el-button>
            </div>
            <div v-if="activeRole === 'OPERATIONS'" class="ops-table-stack">
              <div class="ops-table-block">
                <div class="nested-block__title">最近订单</div>
                <el-table :data="pagedOperationOrderRows" class="dashboard-table" height="240" empty-text="暂无近期订单">
                  <el-table-column prop="orderNo" label="订单号" min-width="160" />
                  <el-table-column label="客户" min-width="150">
                    <template #default="{ row }">{{ row.customerName || '--' }}</template>
                  </el-table-column>
                  <el-table-column label="下单时间" min-width="170">
                    <template #default="{ row }">{{ row.createTime || '--' }}</template>
                  </el-table-column>
                  <el-table-column label="金额" width="110">
                    <template #default="{ row }">{{ (Number(row.payAmountCent || 0) / 100).toFixed(2) }}</template>
                  </el-table-column>
                  <el-table-column label="订单状态" width="120">
                    <template #default="{ row }">
                      <el-tag size="small" :type="statusMeta('order', row.status).type" effect="light">{{ statusMeta('order', row.status).label }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="履约状态" width="120">
                    <template #default="{ row }">
                      <el-tag size="small" :type="statusMeta('order', row.fulfillmentStatus).type" effect="light">{{ statusMeta('order', row.fulfillmentStatus).label }}</el-tag>
                    </template>
                  </el-table-column>
                </el-table>
                <div class="ops-table-pagination">
                  <span class="ops-table-pagination__summary">共 {{ operationOrderRows.length }} 条</span>
                  <el-pagination
                    v-model:current-page="operationOrderPage"
                    v-model:page-size="operationOrderPageSize"
                    size="small"
                    background
                    layout="sizes, prev, pager, next"
                    :page-sizes="[5, 10, 20, 50]"
                    :total="operationOrderRows.length"
                  />
                </div>
              </div>
              <div class="ops-table-block">
                <div class="nested-block__title">最近售后</div>
                <el-table :data="pagedOperationAftersaleRows" class="dashboard-table" height="240" empty-text="暂无近期售后">
                  <el-table-column prop="aftersaleNo" label="售后单" min-width="150" />
                  <el-table-column prop="orderNo" label="订单号" min-width="150" />
                  <el-table-column label="原因" min-width="170">
                    <template #default="{ row }">{{ row.reason || '--' }}</template>
                  </el-table-column>
                  <el-table-column label="退款金额" width="110">
                    <template #default="{ row }">{{ (Number(row.refundAmountCent || 0) / 100).toFixed(2) }}</template>
                  </el-table-column>
                  <el-table-column label="状态" width="120">
                    <template #default="{ row }">
                      <el-tag size="small" :type="statusMeta('aftersale', row.status).type" effect="light">{{ statusMeta('aftersale', row.status).label }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="创建时间" min-width="170">
                    <template #default="{ row }">{{ row.createTime || '--' }}</template>
                  </el-table-column>
                </el-table>
                <div class="ops-table-pagination">
                  <span class="ops-table-pagination__summary">共 {{ operationAftersaleRows.length }} 条</span>
                  <el-pagination
                    v-model:current-page="operationAftersalePage"
                    v-model:page-size="operationAftersalePageSize"
                    size="small"
                    background
                    layout="sizes, prev, pager, next"
                    :page-sizes="[5, 10, 20, 50]"
                    :total="operationAftersaleRows.length"
                  />
                </div>
              </div>
            </div>
            <div v-else-if="activeRole === 'FINANCE'" class="finance-flow-panel">
              <div class="finance-flow-filter">
                <el-segmented v-model="financeFlowType" size="small" :options="financeFlowTypeOptions" />
                <el-select v-model="financeReconcileStatus" size="small" style="width: 132px">
                  <el-option label="全部状态" value="ALL" />
                  <el-option label="待对账" value="PENDING" />
                  <el-option label="已对账" value="MATCHED" />
                  <el-option label="对账差异" value="DIFF" />
                </el-select>
                <el-input v-model="financeFlowKeyword" size="small" clearable placeholder="搜索订单号 / 退款号 / 流水号" class="finance-flow-search" />
                <el-button class="finance-flow-export" size="small" type="primary" plain @click="exportFinanceFlowExcel">导出 Excel</el-button>
              </div>
              <el-table
                :data="pagedFinanceFlowRows"
                class="dashboard-table finance-flow-table"
                height="300"
                empty-text="暂无收支流水"
                :row-class-name="financeFlowRowClass"
              >
                <el-table-column label="流水类型" width="116">
                  <template #default="{ row }"><el-tag size="small" :type="row.flowType === 'PAY' ? 'primary' : 'danger'" effect="light">{{ row.flowTypeLabel }}</el-tag></template>
                </el-table-column>
                <el-table-column label="关联单号" min-width="210" show-overflow-tooltip>
                  <template #default="{ row }">
                    <button class="finance-flow-link" type="button" @click="handleFinanceFlowView(row)">{{ row.relatedNo }}</button>
                    <small v-if="row.orderNo && row.orderNo !== row.relatedNo" class="finance-flow-sub-no">原订单 {{ row.orderNo }}</small>
                  </template>
                </el-table-column>
                <el-table-column label="发生时间" width="118">
                  <template #default="{ row }">{{ formatFinanceOccurTime(row.occurTime) }}</template>
                </el-table-column>
                <el-table-column label="渠道金额" width="130" align="right">
                  <template #default="{ row }"><strong :class="['finance-amount', row.flowType === 'REFUND' ? 'finance-amount--refund' : 'finance-amount--income']">{{ formatFinanceFlowAmount(row) }}</strong></template>
                </el-table-column>
                <el-table-column label="支付渠道" width="112">
                  <template #default="{ row }"><el-tag size="small" type="info" effect="plain">{{ financeChannelLabel(row.channel) }}</el-tag></template>
                </el-table-column>
                <el-table-column label="对账状态" width="118">
                  <template #default="{ row }"><el-tag size="small" :type="financeReconcileStatusMeta(row.reconcileStatus).type" effect="light">{{ financeReconcileStatusMeta(row.reconcileStatus).label }}</el-tag></template>
                </el-table-column>
                <el-table-column label="差异备注" min-width="180">
                  <template #default="{ row }">
                    <el-tooltip
                      :disabled="!isFinanceRemarkOverflow(row)"
                      :content="financeDiffRemarkText(row)"
                      placement="top-start"
                      :show-after="250"
                      :hide-after="0"
                      :offset="8"
                      effect="dark"
                      popper-class="finance-flow-remark-tooltip"
                    >
                      <span
                        :ref="(el) => setFinanceRemarkTextRef(row, el)"
                        class="finance-remark-text"
                        :class="{ 'finance-diff-remark': row.reconcileStatus === 'DIFF', 'finance-remark-text--ellipsis': isFinanceRemarkOverflow(row) }"
                      >{{ financeDiffRemarkText(row) }}</span>
                    </el-tooltip>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="130" fixed="right">
                  <template #default="{ row }">
                    <el-button size="small" link type="primary" @click="handleFinanceFlowView(row)">查看</el-button>
                    <el-button v-if="row.reconcileStatus === 'DIFF'" size="small" link type="warning" @click="goPendingReconciliationTasks">处理</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div class="finance-flow-pagination">
                <span class="finance-flow-pagination__summary">共 {{ filteredFinanceFlowRows.length }} 条</span>
                <el-pagination
                  v-model:current-page="financeFlowPage"
                  v-model:page-size="financeFlowPageSize"
                  size="small"
                  background
                  layout="sizes, prev, pager, next"
                  :page-sizes="[5, 10, 20, 50]"
                  :total="filteredFinanceFlowRows.length"
                />
              </div>
            </div>
            <component v-else :is="tableMeta.component" :rows="tableMeta.rows" :loading="loading" />
          </article>
        </div>

        <aside class="dashboard-side">
          <article class="dashboard-card status-card">
            <div class="card-head">
              <div>
                <h3>{{ sideMeta.title }}</h3>
                <p>{{ sideMeta.desc }}</p>
              </div>
              <el-tag :type="sideMeta.tagType" effect="light">{{ sideMeta.tag }}</el-tag>
            </div>
            <BaseEChart class="chart-box" :option="sideChartOption" />
          </article>

          <article class="dashboard-card list-card">
            <div class="card-head">
              <div>
                <h3>{{ riskMeta.title }}</h3>
                <p>{{ riskMeta.desc }}</p>
              </div>
              <el-button v-if="riskMeta.actionText" size="small" link type="primary" @click="riskMeta.action?.()">{{ riskMeta.actionText }}</el-button>
            </div>
            <div class="risk-list">
              <button v-for="item in visibleRisks" :key="item.label" class="risk-item" type="button" @click="handleRiskClick(item)">
                <div>
                  <strong>{{ item.label }}</strong>
                  <p>{{ item.desc }}</p>
                </div>
                <span :class="`risk-item__badge risk-item__badge--${normalizeRiskLevel(item.level)}`">{{ item.count }}</span>
              </button>
            </div>
          </article>
        </aside>
      </section>
    </div>
  </AdminLayout>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import * as XLSX from 'xlsx';
import AdminLayout from '../components/AdminLayout.vue';
import BaseEChart from '../components/BaseEChart.vue';
import { fetchAdminProductPage } from '../api';
import { DASHBOARD_ROLES, formatDate, useDashboardData } from '../hooks/dashboard/useDashboardData';
import { useAdminStore } from '../stores/admin';
import { getStatusTagMeta } from '../utils/status';

const route = useRoute();
const router = useRouter();
const adminStore = useAdminStore();
const { activeRole, range, loading, errors, state, resetRole, switchRole, updateRange, loadCurrentRole, exportRoleReport, centToYuan } = useDashboardData();
const rangeType = ref(range.value.type);
const customRange = ref([range.value.startDate, range.value.endDate]);

const visibleRoles = computed(() => DASHBOARD_ROLES.map((role) => ({ ...role, intro: role.code === 'OPERATIONS' ? '订单、支付、售后' : role.code === 'FINANCE' ? '资金、退款、对账' : role.code === 'WAREHOUSE' ? '库存、补货、预警' : '商品、销量、热度' })));
const defaultRoleMeta = { code: 'OPERATIONS', label: '运营', title: '运营驾驶舱', theme: 'operations' };
const currentRoleMeta = computed(() => DASHBOARD_ROLES.find((role) => role.code === activeRole.value) || DASHBOARD_ROLES[0] || defaultRoleMeta);
const currentData = computed(() => state?.[activeRole.value] || {});
const productSnapshot = ref({ products: [], total: 0 });
const summary = computed(() => currentData.value?.summary || {});
const overview = computed(() => currentData.value?.overview || {});
const risks = computed(() => currentData.value?.risks || currentData.value?.todos || []);
const tables = computed(() => {
  const data = currentData.value || {};
  const role = activeRole.value;
  if (role === 'OPERATIONS') return { left: data.orderRows || [], right: data.aftersaleRows || [] };
  if (role === 'FINANCE') return { left: data.pays || [], right: data.refunds || [] };
  if (role === 'WAREHOUSE') return { left: data.warnings || [], right: data.logs || [] };
  return { left: data.products || [], right: [] };
});

const heroDescription = computed(() => {
  const descriptions = {
    OPERATIONS: '围绕订单履约、支付状态与售后处理构建的运营总览，帮助你快速识别需要优先处理的业务风险。',
    FINANCE: '围绕资金回流、退款、对账差异与回调异常构建的财务看板，重点关注闭环效率与账实一致性。',
    WAREHOUSE: '围绕库存健康、补货预警、锁定库存与库存差异构建的仓储监控台，突出异常定位与补货指引。',
    PRODUCTS: '围绕商品销量、热销/滞销、上下架与阈值配置构建的商品分析台，帮助你快速调整商品策略。',
  };
  return descriptions[activeRole.value] || descriptions.OPERATIONS;
});

const formatMoney = (value) => centToYuan(value || 0);
const percentText = (value) => `${Math.round(Number(value || 0))}%`;
const normalizeRiskLevel = (level) => {
  const upper = String(level || '').toUpperCase();
  if (upper === 'HIGH') return 'high';
  if (upper === 'WARNING') return 'warning';
  return 'normal';
};
const statusMeta = (kind, value) => getStatusTagMeta(kind, value);

const buildQuickRange = (type) => {
  const end = new Date();
  const start = new Date();
  if (type === 'today') return { type, startDate: formatDate(end), endDate: formatDate(end) };
  start.setDate(end.getDate() - (type === '30d' ? 29 : 6));
  return { type, startDate: formatDate(start), endDate: formatDate(end) };
};

const handleQuickRangeChange = async (type) => {
  if (type === 'custom') return;
  const nextRange = buildQuickRange(type);
  customRange.value = [nextRange.startDate, nextRange.endDate];
  await updateRange(nextRange);
};

const handleCustomRangeChange = async (value) => {
  if (!value?.[0] || !value?.[1]) return;
  await updateRange({ type: 'custom', startDate: value[0], endDate: value[1] });
};

const normalizeDashboardRole = (role) => {
  const value = String(role || '').toUpperCase();
  return DASHBOARD_ROLES.some((item) => item.code === value) ? value : '';
};

const syncDashboardRoleRoute = async (role, mode = 'replace') => {
  const normalizedRole = normalizeDashboardRole(role);
  if (!normalizedRole) return;
  const nextQuery = { ...route.query, role: normalizedRole };
  if (route.query.role === normalizedRole) return;
  await router[mode]({ path: route.path, query: nextQuery });
};

const switchDashboardRole = async (role) => {
  if (role === activeRole.value) return;
  await switchRole(role);
  await syncDashboardRoleRoute(role, 'push');
};

const refreshCurrentRole = async () => {
  resetRole(activeRole.value, { clearState: false, clearCache: true });
  await loadCurrentRole({ force: true });
};

const handleLogout = async () => {
  await adminStore.logout();
  router.push('/login');
};

const goOrders = (status, extraQuery = {}) => router.push({ path: '/orders', query: { ...(status ? { status } : {}), ...extraQuery, page: 1 } });
const goAftersales = (status) => router.push({ path: '/aftersales', query: status ? { status, page: 1 } : { page: 1 } });
const goPays = (status, extraQuery = {}) => router.push({ path: '/pays', query: { ...(status ? { status } : {}), ...extraQuery, page: 1 } });
const goPayRefunds = (status, extraQuery = {}) => router.push({ path: '/pays', query: { tab: 'refunds', ...(status ? { refundStatus: status } : {}), ...extraQuery, refundPage: 1 } });
const goPayCallbacks = (extraQuery = {}) => router.push({ path: '/pays', query: { tab: 'pays', openCallbacks: '1', ...extraQuery, page: 1 } });
const goStocks = (warningStatus) => router.push({ path: '/stocks', query: warningStatus ? { warningStatus } : {} });
const goReconciliations = (status, extraQuery = {}) => router.push({ path: '/reconciliations', query: { ...(status ? { status } : {}), ...extraQuery } });
const goPendingReconciliationTasks = () => goReconciliations('MATCHED', { tab: 'online', processStatus: 'PENDING' });
const goHangingReconciliations = () => router.push({ path: '/reconciliations', query: { tab: 'hanging', page: 1 } });
const goProducts = () => router.push('/products');

const todayRangeQuery = () => {
  const today = formatDate(new Date());
  return { startDate: today };
};
const operationSummaryRouteMap = {
  今日订单数: () => goOrders('', todayRangeQuery()),
  待发货: () => goOrders('PAID'),
  待售后: () => goAftersales('PENDING_REVIEW'),
};
const financeSummaryRouteMap = {
  当月支付金额: () => goPays('', { paidLifecycle: 'true' }),
  当月退款金额: () => goPayRefunds('REFUND_SUCCESS'),
  待处理差异任务: () => goPendingReconciliationTasks(),
};
const handleSummaryCardClick = (card) => {
  if (activeRole.value === 'OPERATIONS' && operationSummaryRouteMap[card.label]) {
    operationSummaryRouteMap[card.label]();
    return;
  }
  if (activeRole.value === 'FINANCE') {
    if (financeSummaryRouteMap[card.label]) financeSummaryRouteMap[card.label]();
    return;
  }
  managementMeta.value.action?.();
};
const riskRouteMap = {
  待发货订单: () => goOrders('PAID'),
  支付异常订单: () => goOrders('PAYMENT_EXCEPTION'),
  待审核售后: () => goAftersales('PENDING_REVIEW'),
  待支付订单: () => goOrders('PENDING_PAYMENT'),
  待处理差异任务: () => goPendingReconciliationTasks(),
  挂账未闭环: () => goHangingReconciliations(),
  支付回调失败: () => goPayCallbacks({ callbackStatus: 'PROCESS_FAILED' }),
  已退款支付单: () => goPays('REFUNDED'),
  当月已退款支付单: () => goPays('REFUNDED'),
};

const handleRiskClick = (item) => {
  if (riskRouteMap[item.label]) {
    riskRouteMap[item.label]();
    return;
  }
  riskMeta.value.action?.();
};

const productOverview = computed(() => {
  if (activeRole.value === 'PRODUCTS') return summary.value || {};
  const products = productSnapshot.value.products || [];
  return {
    productTotal: productSnapshot.value.total || products.length || overview.value.productTotal || 0,
    onSaleCount: products.filter((item) => ['ON_SALE', 'ONSALE', 'SALE'].includes(item.status)).length || overview.value.onSaleProductCount || 0,
    hotCount: products.filter((item) => Number(item.salesCount || 0) >= 100).length || 0,
    slowCount: products.filter((item) => Number(item.salesCount || 0) <= 1).length || 0,
    lowStockHotCount: products.filter((item) => Number(item.salesCount || 0) >= 100 && Number(item.availableStock || 0) <= 10).length || 0,
  };
});

const productMeta = computed(() => ({
  total: productOverview.value.productTotal || 0,
  onSale: productOverview.value.onSaleCount || 0,
  hot: productOverview.value.hotCount || 0,
  slow: productOverview.value.slowCount || 0,
  lowStockHot: productOverview.value.lowStockHotCount || 0,
}));

const productTips = computed(() => [
  { label: '热销阈值', value: productOverview.value.hotThreshold || 100 },
  { label: '滞销阈值', value: productOverview.value.slowThreshold || 1 },
  { label: '商品策略', value: productMeta.value.hot > productMeta.value.slow ? '扩充热销库存' : '优化滞销商品' },
]);

const managementMeta = computed(() => {
  const role = activeRole.value;
  if (role === 'FINANCE') {
    return {
      eyebrow: 'FINANCE MANAGEMENT',
      title: '财务管理',
      desc: '聚焦支付回款、退款、对账差异和资金闭环。',
      actionText: '查看待处理差异',
      action: () => goPendingReconciliationTasks(),
      primaryLabel: '累计总净收入',
      primaryValue: formatMoney(summary.value.cumulativeNetIncomeCent || 0),
      metrics: [
        { label: '当月支付金额', value: formatMoney(summary.value.paidAmountCent || 0) },
        { label: '当月退款金额', value: formatMoney(summary.value.refundAmountCent || 0) },
        { label: '待处理差异任务', value: summary.value.pendingDiffTaskCount || 0 },
        { label: '挂账未闭环', value: summary.value.hangingCount || 0 },
      ],
      tips: [
        { label: '当月支付单', value: summary.value.payOrderCount || 0 },
        { label: '已退款支付单', value: summary.value.refundCount || 0 },
        { label: '策略建议', value: (summary.value.pendingDiffTaskCount || 0) > 0 ? '优先对账' : '资金健康' },
      ],
    };
  }
  if (role === 'WAREHOUSE') {
    return {
      eyebrow: 'WAREHOUSE MANAGEMENT',
      title: '仓储管理',
      desc: '聚焦库存健康、缺货补货、锁定库存和预警处理。',
      actionText: '进入库存',
      action: () => goStocks('LOW'),
      primaryLabel: 'SKU总数',
      primaryValue: summary.value.totalSkuCount || 0,
      metrics: [
        { label: '低库存', value: summary.value.lowStockCount || 0 },
        { label: '缺货', value: summary.value.outOfStockCount || 0 },
        { label: '锁定库存', value: summary.value.lockedStockTotal || 0 },
        { label: '库存差异', value: summary.value.stockDiffCount || 0 },
      ],
      tips: [
        { label: '预警SKU', value: summary.value.warningCount || 0 },
        { label: '操作日志', value: summary.value.stockLogCount || 0 },
        { label: '策略建议', value: (summary.value.outOfStockCount || 0) > 0 ? '优先补货' : '库存健康' },
      ],
    };
  }
  if (role === 'PRODUCTS') {
    return {
      eyebrow: 'PRODUCT MANAGEMENT',
      title: '商品管理',
      desc: '聚焦商品总量、上架状态、热销滞销和库存表现。',
      actionText: '进入商品',
      action: () => goProducts(),
      primaryLabel: '商品总数',
      primaryValue: productMeta.value.total,
      metrics: [
        { label: '上架', value: productMeta.value.onSale },
        { label: '热销', value: productMeta.value.hot },
        { label: '滞销', value: productMeta.value.slow },
        { label: '热销低库存', value: productMeta.value.lowStockHot },
      ],
      tips: productTips.value,
    };
  }
  return {
    eyebrow: 'OPERATIONS MANAGEMENT',
    title: '运营管理',
    desc: '聚焦订单履约、支付状态、售后处理和运营风险。',
    actionText: '进入订单',
    action: () => goOrders(),
    primaryLabel: '订单总数',
    primaryValue: summary.value.totalOrderCount || 0,
    metrics: [
      { label: '待发货', value: summary.value.paidOrderCount || 0 },
      { label: '待支付', value: summary.value.pendingOrderCount || 0 },
      { label: '待售后', value: summary.value.pendingAftersaleCount || 0 },
      { label: '履约率', value: percentText(summary.value.fulfillmentRate) },
    ],
    tips: [
      { label: '完成订单', value: summary.value.completedOrderCount || 0 },
      { label: '支付异常', value: summary.value.abnormalOrderCount || 0 },
      { label: '策略建议', value: (summary.value.pendingAftersaleCount || 0) > 0 ? '优先售后' : '运营健康' },
    ],
  };
});

const summaryCards = computed(() => {
  const role = activeRole.value;
  if (role === 'OPERATIONS') {
    return [
      { label: '今日订单数', value: summary.value.todayOrderCount || 0, desc: '今日新增订单' },
      { label: '待发货', value: summary.value.paidOrderCount || 0, desc: '已支付待履约' },
      { label: '待售后', value: summary.value.pendingAftersaleCount || 0, desc: '待审核售后单' },
      { label: '履约率', value: percentText(summary.value.fulfillmentRate), desc: '订单完成效率' },
    ];
  }
  if (role === 'FINANCE') {
    return [
      { label: '当月支付金额', value: formatMoney(summary.value.paidAmountCent || 0), desc: '本月支付完成金额' },
      { label: '当月退款金额', value: formatMoney(summary.value.refundAmountCent || 0), desc: '本月退款金额' },
      { label: '当月净收入', value: formatMoney(summary.value.netIncomeCent || 0), desc: '支付金额 - 退款金额' },
      { label: '待处理差异任务', value: summary.value.pendingDiffTaskCount || 0, desc: '存在待处理差异的对账任务' },
    ];
  }
  if (role === 'WAREHOUSE') {
    return [
      { label: 'SKU总数', value: summary.value.totalSkuCount || 0, desc: '库存 SKU 数量' },
      { label: '低库存', value: summary.value.lowStockCount || 0, desc: '需补货 SKU' },
      { label: '缺货', value: summary.value.outOfStockCount || 0, desc: '库存为 0' },
      { label: '锁定库存', value: summary.value.lockedStockTotal || 0, desc: '当前锁定总量' },
    ];
  }
  return [
    { label: '商品总数', value: summary.value.productTotal || 0, desc: '全部商品数量' },
    { label: '上架商品', value: summary.value.onSaleCount || 0, desc: '正常销售中' },
    { label: '热销商品', value: summary.value.hotCount || 0, desc: '销量高于阈值' },
    { label: '滞销商品', value: summary.value.slowCount || 0, desc: '需优化策略' },
  ];
});

const mainChartMeta = computed(() => {
  if (activeRole.value === 'FINANCE') {
    return {
      title: '近 7 日资金趋势与对账风险',
      desc: '展示每日支付、退款、净收入及待处理差异数量，辅助定位高风险账期。',
    };
  }
  return {
    title: '订单运营趋势：近 7 日订单履约与售后走势',
    desc: '围绕订单、履约与售后变化进行连续观察。',
  };
});

const operationsChartOption = computed(() => {
  const trendRows = Array.isArray(currentData.value?.operationsTrend) ? currentData.value.operationsTrend : [];
  const labels = trendRows.map((item) => String(item.date || '').slice(5) || '--');
  const orderCounts = trendRows.map((item) => Number(item.orderCount || 0));
  const completedOrderCounts = trendRows.map((item) => Number(item.completedOrderCount || 0));
  const aftersaleCounts = trendRows.map((item) => Number(item.aftersaleCount || 0));
  return {
    tooltip: { trigger: 'axis' },
    legend: { top: 0, textStyle: { color: '#64748b' } },
    grid: { left: 10, right: 16, top: 40, bottom: 8, containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: labels, axisLabel: { color: '#94a3b8' } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: 'rgba(148,163,184,.14)' } }, axisLabel: { color: '#94a3b8' } },
    series: [
      { name: '订单总量', type: 'line', smooth: true, data: orderCounts, lineStyle: { width: 4, color: '#5b6cff' }, itemStyle: { color: '#5b6cff' }, areaStyle: { color: 'rgba(91,108,255,.12)' } },
      { name: '履约完成', type: 'line', smooth: true, data: completedOrderCounts, lineStyle: { width: 3, color: '#22c55e' }, itemStyle: { color: '#22c55e' } },
      { name: '售后处理', type: 'line', smooth: true, data: aftersaleCounts, lineStyle: { width: 3, color: '#f97316' }, itemStyle: { color: '#f97316' } },
    ],
  };
});

const financeChartOption = computed(() => {
  const trendRows = Array.isArray(currentData.value?.financeTrend) ? currentData.value.financeTrend : [];
  const labels = trendRows.map((item) => String(item.date || '').slice(5) || '--');
  const paidAmounts = trendRows.map((item) => Number((Number(item.paidAmountCent || 0) / 100).toFixed(2)));
  const refundAmounts = trendRows.map((item) => Number((Number(item.refundAmountCent || 0) / 100).toFixed(2)));
  const netIncomeAmounts = trendRows.map((item) => Number((Number(item.netIncomeCent || 0) / 100).toFixed(2)));
  const pendingDiffCounts = trendRows.map((item) => Number(item.pendingDiffCount || 0));
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter: (items = []) => {
        const rows = Array.isArray(items) ? items : [items];
        const title = rows[0]?.axisValueLabel || rows[0]?.name || '';
        const lines = rows.map((item) => {
          const value = item.seriesName === '待处理差异' ? `${Number(item.value || 0)} 单` : `¥${Number(item.value || 0).toFixed(2)}`;
          return `${item.marker || ''}${item.seriesName}：${value}`;
        });
        return [title, ...lines].join('<br/>');
      },
    },
    legend: { top: 0, textStyle: { color: '#64748b' } },
    grid: { left: 10, right: 18, top: 42, bottom: 8, containLabel: true },
    xAxis: { type: 'category', boundaryGap: true, data: labels, axisLabel: { color: '#94a3b8' } },
    yAxis: [
      { type: 'value', name: '金额(元)', splitLine: { lineStyle: { color: 'rgba(148,163,184,.14)' } }, axisLabel: { color: '#94a3b8', formatter: '¥{value}' } },
      { type: 'value', name: '差异数', minInterval: 1, splitLine: { show: false }, axisLabel: { color: '#94a3b8', formatter: '{value}单' } },
    ],
    series: [
      { name: '待处理差异', type: 'bar', yAxisIndex: 1, barWidth: 18, data: pendingDiffCounts, itemStyle: { color: 'rgba(239,68,68,.24)', borderRadius: [8, 8, 0, 0] }, emphasis: { itemStyle: { color: 'rgba(239,68,68,.38)' } } },
      { name: '支付金额', type: 'line', smooth: true, data: paidAmounts, lineStyle: { width: 4, color: '#2563eb' }, itemStyle: { color: '#2563eb' }, areaStyle: { color: 'rgba(37,99,235,.10)' } },
      { name: '退款金额', type: 'line', smooth: true, data: refundAmounts, lineStyle: { width: 3, color: '#f97316' }, itemStyle: { color: '#f97316' } },
      { name: '净收入', type: 'line', smooth: true, data: netIncomeAmounts, lineStyle: { width: 3, color: '#22c55e' }, itemStyle: { color: '#22c55e' } },
    ],
  };
});

const mainChartOption = computed(() => activeRole.value === 'FINANCE' ? financeChartOption.value : operationsChartOption.value);


const sideMeta = computed(() => {
  if (activeRole.value === 'OPERATIONS') return { title: '运营管理', desc: '订单闭环与履约监控', tag: '高优先级', tagType: 'warning' };
  if (activeRole.value === 'FINANCE') return { title: '对账闭环结构', desc: '待处理差异账单、挂账跟进与已归档任务数量分布', tag: '差异结构', tagType: 'success' };
  if (activeRole.value === 'WAREHOUSE') return { title: '仓储管理', desc: '库存健康与异常预警', tag: '库存监控', tagType: 'info' };
  return { title: '商品管理', desc: '商品结构与销售表现', tag: '商品策略', tagType: 'primary' };
});

const sideChartOption = computed(() => {
  if (activeRole.value === 'FINANCE') {
    return {
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['66%', '82%'],
        center: ['50%', '50%'],
        label: { show: true, color: '#475569', formatter: '{b}\n{d}%' },
        data: [
          { value: summary.value.pendingReconcileCount || 0, name: '待处理差异账单', itemStyle: { color: '#2563eb' } },
          { value: summary.value.hangingCount || 0, name: '挂账未闭环', itemStyle: { color: '#7c3aed' } },
          { value: summary.value.archivedTaskCount || 0, name: '已归档任务', itemStyle: { color: '#22c55e' } },
        ],
      }],
    };
  }
  if (activeRole.value === 'WAREHOUSE') {
    return {
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['66%', '82%'],
        center: ['50%', '50%'],
        label: { show: true, color: '#475569', formatter: '{b}\n{d}%' },
        data: [
          { value: summary.value.lowStockCount || 0, name: '低库存', itemStyle: { color: '#f59e0b' } },
          { value: summary.value.outOfStockCount || 0, name: '缺货', itemStyle: { color: '#fb7185' } },
          { value: summary.value.normalCount || 0, name: '正常', itemStyle: { color: '#22c55e' } },
        ],
      }],
    };
  }
  if (activeRole.value === 'PRODUCTS') {
    return {
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['66%', '82%'],
        center: ['50%', '50%'],
        label: { show: true, color: '#475569', formatter: '{b}\n{d}%' },
        data: [
          { value: summary.value.hotCount || 0, name: '热销', itemStyle: { color: '#22c55e' } },
          { value: summary.value.slowCount || 0, name: '滞销', itemStyle: { color: '#f97316' } },
          { value: summary.value.onSaleCount || 0, name: '在售', itemStyle: { color: '#2563eb' } },
        ],
      }],
    };
  }
  return {
    tooltip: { trigger: 'item' },
    legend: {
      bottom: 0,
      left: 'center',
      itemWidth: 10,
      itemHeight: 10,
      icon: 'circle',
      textStyle: { color: '#64748b', fontSize: 12 },
    },
    series: [{
      type: 'pie',
      radius: ['52%', '74%'],
      center: ['50%', '44%'],
      avoidLabelOverlap: true,
      labelLine: { length: 18, length2: 34, smooth: true, lineStyle: { color: 'rgba(148,163,184,.7)', width: 1.2 } },
      label: { show: true, color: '#334155', fontSize: 12, formatter: '{b}\n{c}' },
      itemStyle: { borderWidth: 4, borderColor: '#fff' },
      data: [
        { value: summary.value.pendingOrderCount || 0, name: '待支付', itemStyle: { color: '#2563eb' } },
        { value: summary.value.paidOrderCount || 0, name: '已支付', itemStyle: { color: '#22c55e' } },
        { value: summary.value.shippedOrderCount || 0, name: '已发货', itemStyle: { color: '#f59e0b' } },
        { value: summary.value.completedOrderCount || 0, name: '已完成', itemStyle: { color: '#ec4899' } },
        { value: summary.value.cancelledOrderCount || 0, name: '已取消', itemStyle: { color: '#94a3b8' } },
        { value: summary.value.pendingAftersaleCount || 0, name: '售后中', itemStyle: { color: '#8b5cf6' } },
      ],
    }],
  };
});

const riskMeta = computed(() => {
  if (activeRole.value === 'OPERATIONS') return { title: '运营待办', desc: '关注履约、售后和支付异常', actionText: '查看订单', action: () => goOrders() };
  if (activeRole.value === 'FINANCE') return { title: '财务待办', desc: '关注对账、回调和退款状态', actionText: '查看对账', action: () => goReconciliations() };
  if (activeRole.value === 'WAREHOUSE') return { title: '仓储待办', desc: '关注补货、缺货和锁定库存', actionText: '查看库存', action: () => goStocks('LOW') };
  return { title: '商品待办', desc: '关注热销、滞销与上新状态', actionText: '查看商品', action: () => goProducts() };
});

const visibleRisks = computed(() => (Array.isArray(risks.value) ? risks.value : []).slice(0, 4));
const operationOrderRows = computed(() => currentData.value?.orderOverviewRows || []);
const operationAftersaleRows = computed(() => currentData.value?.aftersaleRows || []);
const operationOrderPage = ref(1);
const operationOrderPageSize = ref(5);
const operationAftersalePage = ref(1);
const operationAftersalePageSize = ref(5);
const pagedOperationOrderRows = computed(() => {
  const start = (operationOrderPage.value - 1) * operationOrderPageSize.value;
  return operationOrderRows.value.slice(start, start + operationOrderPageSize.value);
});
const pagedOperationAftersaleRows = computed(() => {
  const start = (operationAftersalePage.value - 1) * operationAftersalePageSize.value;
  return operationAftersaleRows.value.slice(start, start + operationAftersalePageSize.value);
});
watch([operationOrderPageSize], () => { operationOrderPage.value = 1; });
watch([operationAftersalePageSize], () => { operationAftersalePage.value = 1; });
watch(operationOrderRows, (rows) => {
  const maxPage = Math.max(1, Math.ceil(rows.length / operationOrderPageSize.value));
  if (operationOrderPage.value > maxPage) operationOrderPage.value = maxPage;
});
watch(operationAftersaleRows, (rows) => {
  const maxPage = Math.max(1, Math.ceil(rows.length / operationAftersalePageSize.value));
  if (operationAftersalePage.value > maxPage) operationAftersalePage.value = maxPage;
});
const financeFlowType = ref('ALL');
const financeReconcileStatus = ref('ALL');
const financeFlowKeyword = ref('');
const financeFlowPage = ref(1);
const financeFlowPageSize = ref(5);
const financeRemarkOverflowMap = ref({});
const financeFlowTypeOptions = [
  { label: '全部流水', value: 'ALL' },
  { label: '仅支付', value: 'PAY' },
  { label: '仅退款', value: 'REFUND' },
];
const financeRemarkMap = {
  AMOUNT_NOT_MATCH: '金额不一致',
  AMOUNT_MISMATCH: '金额不一致',
  STATUS_NOT_MATCH: '状态不一致',
  STATUS_MISMATCH: '状态不一致',
  CHANNEL_MISSING: '渠道流水缺失',
  LOCAL_MISSING: '本地流水缺失',
  ORDER_NOT_FOUND: '订单不存在',
  PAY_NOT_FOUND: '支付单不存在',
  REFUND_NOT_FOUND: '退款单不存在',
  CALLBACK_MISSING: '回调记录缺失',
  DUPLICATE_CHANNEL_TRADE: '渠道流水重复',
  CHANNEL_EXISTS_LOCAL_MISSING: '渠道有流水，本地缺失',
  LOCAL_EXISTS_CHANNEL_MISSING: '本地有流水，渠道缺失',
  HANGING: '挂账中',
  PENDING_HANDLE: '待处理',
  PENDING_ACTION: '待处理',
  SUCCESS: '成功',
  FAILED: '失败',
  NONE: '无',
  MANUAL_REVIEW: '人工复核',
  CLOSE_ORDER_VOID: '关闭订单并作废',
  SYNC_PAY_STATUS: '同步支付状态',
  SYNC_REFUND_RESULT: '同步退款结果',
  REGISTER_AMOUNT_ADJUSTMENT: '登记金额调整',
  SUBMIT_FINANCE_ADJUSTMENT: '提交财务调账',
  MARK_TEST_FLOW_VOID: '标记测试流水作废',
  MARK_DONE: '标记已处理',
  IGNORE: '忽略',
  HANG: '挂账',
  DONE: '已完成',
  COMPLETED: '已完成',
  MATCHED: '已勾兑',
  ARCHIVED: '已归档',
  ARCHIVE_DONE: '归档完成',
  PENDING: '待处理',
  STATUS_AND_AMOUNT_MISMATCH: '状态和金额不一致',
  REFUND_STATUS_MISMATCH: '退款状态不一致',
};
const translateFinanceRemark = (value) => {
  const text = String(value || '').trim();
  if (!text) return '--';
  return text.replace(/[A-Z][A-Z0-9_]+/g, (word) => financeRemarkMap[word] || word);
};
const pickFinanceDiffRemark = (row = {}) => (
  row.diffRemark
  || row.reconcileRemark
  || row.reconciliationRemark
  || row.remark
  || row.processRemark
  || row.handleRemark
  || row.suggestedAction
  || row.diffType
  || row.reason
  || row.failReason
  || row.exceptionReason
  || row.raw?.diffRemark
  || row.raw?.reconcileRemark
  || row.raw?.reconciliationRemark
  || row.raw?.remark
  || row.raw?.processRemark
  || row.raw?.handleRemark
  || row.raw?.suggestedAction
  || row.raw?.diffType
  || row.raw?.reason
  || row.raw?.failReason
  || row.raw?.exceptionReason
  || row.onlineDiff?.processRemark
  || row.onlineDiff?.handleRemark
  || row.onlineDiff?.suggestedAction
  || row.onlineDiff?.diffType
  || row.onlineDiff?.remark
  || row.hangingFollow?.processRemark
  || row.hangingFollow?.handleRemark
  || row.hangingFollow?.remark
  || row.hangingDiff?.processRemark
  || row.hangingDiff?.handleRemark
  || row.hangingDiff?.suggestedAction
  || row.hangingDiff?.diffType
  || row.hangingDiff?.remark
  || row.completedRecord?.processRemark
  || row.completedRecord?.handleRemark
  || row.completedRecord?.suggestedAction
  || row.completedRecord?.diffType
  || row.completedRecord?.remark
  || row.archivedTask?.processRemark
  || row.archivedTask?.handleRemark
  || row.archivedTask?.suggestedAction
  || row.archivedTask?.diffType
  || row.archivedTask?.remark
  || row.archivedTask?.taskNo
  || ''
);
const financeDiffRemarkText = (row = {}) => {
  const explicitRemark = pickFinanceDiffRemark(row);
  if (explicitRemark) return translateFinanceRemark(explicitRemark);
  if (row.reconcileStatus === 'DIFF') return '存在对账差异，待处理';
  if (row.reconcileStatus === 'MATCHED') return '已完成对账';
  if (row.reconcileStatus === 'PENDING') return '待对账，等待线上账单核验';
  return '--';
};
const financeRemarkKey = (row = {}) => `${row.flowType || 'FLOW'}:${row.relatedNo || row.orderNo || row.refundNo || row.payNo || ''}`;
const updateFinanceRemarkOverflow = (row, el) => {
  const key = financeRemarkKey(row);
  if (!key) return;
  const overflow = Boolean(el && el.scrollWidth > el.clientWidth + 1);
  if (financeRemarkOverflowMap.value[key] !== overflow) {
    financeRemarkOverflowMap.value = { ...financeRemarkOverflowMap.value, [key]: overflow };
  }
};
const setFinanceRemarkTextRef = (row, el) => {
  requestAnimationFrame(() => updateFinanceRemarkOverflow(row, el));
};
const isFinanceRemarkOverflow = (row) => Boolean(financeRemarkOverflowMap.value[financeRemarkKey(row)]);
const inferReconcileStatus = (row) => {
  const source = String(row.reconcileStatus || row.reconciliationStatus || '').toUpperCase();
  const processStatus = String(row.processStatus || row.onlineDiff?.processStatus || row.hangingFollow?.processStatus || row.hangingDiff?.processStatus || '').toUpperCase();
  const diffType = String(row.diffType || row.onlineDiff?.diffType || row.hangingDiff?.diffType || '').toUpperCase();
  if (row.onlineDiff || row.hangingFollow || row.hangingDiff) return 'DIFF';
  if (diffType && diffType !== 'MATCHED') return 'DIFF';
  if (['PENDING', 'HANGING'].includes(processStatus)) return 'DIFF';
  if (['DIFF', 'DIFFERENCE', 'HANGING', 'HANG', 'PENDING_HANDLE', 'PENDING_ACTION'].includes(source)) return 'DIFF';
  if (row.archivedTask || row.archiveTask || row.archiveReport || row.completedRecord) return 'MATCHED';
  if (['MATCHED', 'DONE', 'COMPLETED', 'SUCCESS', 'ARCHIVED', 'ARHIVED', 'ARCHIVE', 'ARCHIVE_DONE', 'CLOSED'].includes(source)) return 'MATCHED';
  return 'PENDING';
};
const firstValidText = (...values) => values.find((value) => {
  const text = String(value ?? '').trim();
  return text && text !== '--' && text.toLowerCase() !== 'null' && text.toLowerCase() !== 'undefined';
}) || '';
const financeFlowRows = computed(() => {
  const payRows = (tables.value.left || []).map((row) => {
    const flowRow = {
      ...row,
      flowType: 'PAY',
      flowTypeLabel: '支付订单',
      orderNo: row.orderNo || '--',
      refundNo: '',
      relatedNo: row.payNo || row.orderNo || '--',
      occurTime: firstValidText(row.payTime, row.paidAt, row.createTime, row.createdAt, row.created_at, row.create_time, row.gmtCreate, row.gmt_create, row.raw?.createdAt, row.raw?.created_at, row.raw?.create_time),
      amountCent: Number(row.payAmountCent || 0),
      channel: row.paymentMethod || row.payChannel || row.channel || 'UNKNOWN',
      diffRemark: pickFinanceDiffRemark(row),
      channelTradeNo: row.channelTradeNo || row.tradeNo || row.payNo || '',
      raw: row,
    };
    return { ...flowRow, reconcileStatus: inferReconcileStatus(flowRow) };
  });
  const refundRows = (tables.value.right || []).map((row) => {
    const flowRow = {
      ...row,
      flowType: 'REFUND',
      flowTypeLabel: '售后退款',
      orderNo: row.orderNo || '--',
      refundNo: row.refundNo || '--',
      relatedNo: row.refundNo || row.orderNo || '--',
      occurTime: firstValidText(row.refundTime, row.createTime, row.createdAt, row.created_at, row.create_time, row.gmtCreate, row.gmt_create, row.raw?.createdAt, row.raw?.created_at, row.raw?.create_time),
      amountCent: Number(row.refundAmountCent || 0),
      channel: row.paymentMethod || row.payChannel || row.channel || 'UNKNOWN',
      diffRemark: pickFinanceDiffRemark(row),
      channelTradeNo: row.channelTradeNo || row.tradeNo || row.refundNo || '',
      raw: row,
    };
    return { ...flowRow, reconcileStatus: inferReconcileStatus(flowRow) };
  });
  return [...payRows, ...refundRows].sort((a, b) => String(b.occurTime || '').localeCompare(String(a.occurTime || '')));
});
const filteredFinanceFlowRows = computed(() => {
  const keyword = financeFlowKeyword.value.trim().toLowerCase();
  return financeFlowRows.value.filter((row) => {
    const typeMatched = financeFlowType.value === 'ALL' || row.flowType === financeFlowType.value;
    const statusMatched = financeReconcileStatus.value === 'ALL' || row.reconcileStatus === financeReconcileStatus.value;
    const keywordMatched = !keyword || [row.orderNo, row.refundNo, row.relatedNo, row.channelTradeNo].some((value) => String(value || '').toLowerCase().includes(keyword));
    return typeMatched && statusMatched && keywordMatched;
  });
});
const pagedFinanceFlowRows = computed(() => {
  const start = (financeFlowPage.value - 1) * financeFlowPageSize.value;
  return filteredFinanceFlowRows.value.slice(start, start + financeFlowPageSize.value);
});
watch([financeFlowType, financeReconcileStatus, financeFlowKeyword, financeFlowPageSize], () => {
  financeFlowPage.value = 1;
});
watch([pagedFinanceFlowRows, financeFlowPageSize], () => {
  financeRemarkOverflowMap.value = {};
});
watch(filteredFinanceFlowRows, (rows) => {
  const maxPage = Math.max(1, Math.ceil(rows.length / financeFlowPageSize.value));
  if (financeFlowPage.value > maxPage) financeFlowPage.value = maxPage;
});
const financeReconcileStatusMeta = (status) => ({
  PENDING: { label: '待对账', type: 'warning' },
  MATCHED: { label: '已对账', type: 'success' },
  DIFF: { label: '对账差异', type: 'danger' },
}[status] || { label: status || '--', type: 'info' });
const financeChannelLabel = (channel) => ({
  ALIPAY: '支付宝',
  WECHAT: '微信',
  WECHAT_PAY: '微信',
  BALANCE: '余额',
  MOCK: '模拟渠道',
  UNKNOWN: '未知',
}[String(channel || 'UNKNOWN').toUpperCase()] || channel || '未知');
const formatFinanceOccurTime = (time) => {
  const value = String(time || '');
  const matched = value.match(/\d{4}-(\d{2}-\d{2})[ T](\d{2}:\d{2})/);
  if (matched) return `${matched[1]} ${matched[2]}`;
  return value || '--';
};
const formatFinanceFlowAmount = (row) => `${row.flowType === 'REFUND' ? '-' : ''}${formatMoney(row.amountCent)}`;
const exportFinanceFlowExcel = () => {
  const rows = filteredFinanceFlowRows.value.map((row) => ({
    流水类型: row.flowTypeLabel,
    关联单号: row.relatedNo,
    原订单号: row.orderNo || '',
    发生时间: row.occurTime || '',
    渠道金额: formatFinanceFlowAmount(row),
    支付渠道: financeChannelLabel(row.channel),
    对账状态: financeReconcileStatusMeta(row.reconcileStatus).label,
    差异备注: financeDiffRemarkText(row) === '--' ? '' : financeDiffRemarkText(row),
    渠道流水号: row.channelTradeNo || '',
  }));
  const workbook = XLSX.utils.book_new();
  const worksheet = XLSX.utils.json_to_sheet(rows.length ? rows : [{ 提示: '暂无收支流水' }]);
  XLSX.utils.book_append_sheet(workbook, worksheet, '近期收支与对账流水');
  XLSX.writeFile(workbook, `近期收支与对账流水-${formatDate(new Date())}.xlsx`);
};
const financeFlowRowClass = ({ row }) => row.reconcileStatus === 'DIFF' ? 'finance-flow-row--diff' : '';
const handleFinanceFlowView = (row) => {
  const orderKeyword = row.orderNo && row.orderNo !== '--' ? row.orderNo : row.relatedNo;
  if (row.flowType === 'REFUND') {
    goPayRefunds('', { keyword: orderKeyword, orderNo: orderKeyword });
    return;
  }
  goPays('', { keyword: orderKeyword, orderNo: orderKeyword });
};

const DashboardWarehouseTable = {
  props: ['rows'],
  computed: {
    warnings() {
      return this.rows?.warnings || [];
    },
    logs() {
      return this.rows?.logs || [];
    },
  },
  template: `
    <div class="nested-grid">
      <div class="nested-block">
        <div class="nested-block__title">库存预警</div>
        <el-table :data="warnings" class="dashboard-table" height="240">
          <el-table-column prop="skuId" label="SKU" width="100" />
          <el-table-column prop="productName" label="商品" min-width="160" />
          <el-table-column prop="availableStock" label="可用库存" width="110" />
          <el-table-column prop="warningStatus" label="预警状态" min-width="120" />
        </el-table>
      </div>
      <div class="nested-block">
        <div class="nested-block__title">库存日志</div>
        <el-table :data="logs" class="dashboard-table" height="240">
          <el-table-column prop="skuId" label="SKU" width="100" />
          <el-table-column prop="changeType" label="类型" width="120" />
          <el-table-column prop="createTime" label="时间" min-width="160" />
        </el-table>
      </div>
    </div>
  `,
};

const DashboardProductsTable = {
  props: ['rows'],
  template: `
    <el-table :data="rows" class="dashboard-table" height="280">
      <el-table-column prop="id" label="ID" width="90" />
      <el-table-column prop="name" label="商品名称" min-width="220" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="salesCount" label="销量" width="120" />
      <el-table-column prop="availableStock" label="库存" width="120" />
    </el-table>
  `,
};

const tableMeta = computed(() => {
  if (activeRole.value === 'OPERATIONS') {
    return {
      title: '订单运营明细',
      desc: '聚合近期订单、待发货与售后中的关键记录，便于快速处理异常。',
      actionText: '订单中心',
      action: () => goOrders(),
    };
  }
  if (activeRole.value === 'FINANCE') {
    return {
      title: '近期收支与对账流水',
      desc: '统一展示支付与退款流水，快速识别待对账、差异和可处理单据。',
      rows: {
        pays: tables.value.left,
        refunds: tables.value.right,
      },
      actionText: '支付中心',
      action: () => goPays(),
    };
  }
  if (activeRole.value === 'WAREHOUSE') {
    return {
      title: '库存明细',
      desc: '库存预警与操作日志。',
      component: DashboardWarehouseTable,
      rows: {
        warnings: tables.value.left,
        logs: tables.value.right,
      },
      actionText: '库存页',
      action: () => goStocks('LOW'),
    };
  }
  return {
    title: '商品明细',
    desc: '热销商品与商品列表。',
    component: DashboardProductsTable,
    rows: tables.value.left,
    actionText: '商品管理',
    action: () => goProducts(),
  };
});

const hasTableRows = computed(() => {
  if (activeRole.value === 'OPERATIONS') return operationOrderRows.value.length > 0 || operationAftersaleRows.value.length > 0;
  const rows = tableMeta.value.rows;
  if (Array.isArray(rows)) return rows.length > 0;
  return Object.values(rows || {}).some((item) => Array.isArray(item) && item.length > 0);
});

const handleRoleVisibility = () => {
  if (!visibleRoles.value.some((role) => role.code === activeRole.value)) activeRole.value = visibleRoles.value[0]?.code || 'OPERATIONS';
};

const loadProductSnapshot = async () => {
  try {
    const { data } = await fetchAdminProductPage({ page: 1, size: 12, sortField: 'salesCount', sortOrder: 'desc' });
    const payload = data?.data || {};
    productSnapshot.value = { products: payload.records || payload.list || [], total: Number(payload.total || payload.totalCount || 0) };
  } catch (error) {
    productSnapshot.value = { products: [], total: 0 };
  }
};

onMounted(async () => {
  const routeRole = normalizeDashboardRole(route.query.role);
  if (routeRole && routeRole !== activeRole.value) {
    activeRole.value = routeRole;
  }
  handleRoleVisibility();
  await syncDashboardRoleRoute(activeRole.value);
  await Promise.all([loadCurrentRole(), loadProductSnapshot()]);
});

onBeforeUnmount(() => resetRole(activeRole.value));
</script>

<style scoped>
.dashboard-page{display:flex;flex-direction:column;gap:16px;min-height:100%}
.dashboard-card{border:1px solid rgba(255,255,255,.84);background:linear-gradient(180deg,rgba(255,255,255,.94),rgba(248,250,255,.88));box-shadow:0 18px 50px rgba(148,163,184,.14);backdrop-filter:blur(18px)}
.dashboard-top-grid{display:grid;grid-template-columns:minmax(0,1.6fr) minmax(360px,.8fr);gap:16px;align-items:stretch}
.role-dashboard,.management-dashboard{padding:22px;border-radius:28px;overflow:hidden}
.card-head--compact{margin-bottom:16px}
.card-head--compact h2{margin:10px 0 0;font-size:30px;color:#0f172a}
.card-head--compact p{margin:10px 0 0;max-width:760px;line-height:1.75;color:#64748b}
.hero-eyebrow{font-size:12px;font-weight:900;letter-spacing:.18em;color:#64748b}
.management-dashboard{position:relative;display:flex;flex-direction:column;gap:14px;background:linear-gradient(135deg,rgba(255,255,255,.97),rgba(239,246,255,.92),rgba(248,250,252,.96));box-shadow:0 18px 44px rgba(37,99,235,.06)}
.management-dashboard--finance{background:linear-gradient(135deg,rgba(255,255,255,.97),rgba(245,243,255,.92),rgba(248,250,252,.96))}
.management-dashboard--warehouse{background:linear-gradient(135deg,rgba(255,255,255,.97),rgba(240,253,244,.92),rgba(248,250,252,.96))}
.management-dashboard--products{background:linear-gradient(135deg,rgba(255,255,255,.97),rgba(255,247,237,.92),rgba(248,250,252,.96))}
.management-head h2{margin:0;font-size:30px;color:#0f172a}
.management-head p{margin:10px 0 0;line-height:1.7;color:#64748b}
.management-hero{display:grid;grid-template-columns:minmax(150px,.86fr) minmax(0,1.14fr);gap:14px;padding:14px;border-radius:26px;background:rgba(255,255,255,.58);border:1px solid rgba(226,232,240,.68)}
.management-head h2{margin:0;font-size:30px;color:#0f172a}
.management-head p{margin:10px 0 0;line-height:1.7;color:#64748b}
.management-hero{display:grid;grid-template-columns:minmax(150px,.86fr) minmax(0,1.14fr);gap:14px;padding:14px;border-radius:26px;background:rgba(255,255,255,.58);border:1px solid rgba(226,232,240,.68)}
.management-hero__main{position:relative;display:flex;flex-direction:column;justify-content:space-between;min-height:160px;padding:22px;border-radius:24px;background:linear-gradient(145deg,rgba(37,99,235,.96),rgba(96,165,250,.92));color:#fff;box-shadow:0 18px 38px rgba(37,99,235,.18);overflow:hidden}
.management-dashboard--finance .management-hero__main{background:linear-gradient(145deg,rgba(124,58,237,.96),rgba(167,139,250,.92));box-shadow:0 18px 38px rgba(124,58,237,.18)}
.management-dashboard--warehouse .management-hero__main{background:linear-gradient(145deg,rgba(22,163,74,.96),rgba(134,239,172,.92));box-shadow:0 18px 38px rgba(22,163,74,.18)}
.management-dashboard--products .management-hero__main{background:linear-gradient(145deg,rgba(249,115,22,.96),rgba(253,186,116,.92));box-shadow:0 18px 38px rgba(249,115,22,.18)}
.management-hero__main::after{content:"";position:absolute;right:-34px;bottom:-42px;width:124px;height:124px;border-radius:999px;background:rgba(255,255,255,.16)}
.management-hero__main strong{position:relative;z-index:1;font-size:42px;line-height:1;font-weight:900;letter-spacing:-.04em;word-break:keep-all}
.management-hero__main span{position:relative;z-index:1;margin-top:12px;font-size:13px;font-weight:800;opacity:.94}
.management-hero__list{display:flex;flex-direction:column;gap:10px}
.management-metric-row{display:flex;align-items:center;justify-content:space-between;gap:14px;min-height:38px;padding:11px 14px;border-radius:16px;background:rgba(255,255,255,.84);border:1px solid rgba(226,232,240,.72);box-shadow:0 8px 18px rgba(15,23,42,.045)}
.management-metric-row span{font-size:13px;font-weight:700;color:#64748b}
.management-metric-row b{font-size:20px;line-height:1;font-weight:900;color:#0f172a;white-space:nowrap}
.management-footer{margin-top:auto;display:flex;align-items:center;justify-content:space-between;gap:12px;padding:12px;border-radius:22px;background:rgba(248,250,252,.66);border:1px solid rgba(226,232,240,.68)}
.management-tips{display:flex;align-items:center;gap:10px;min-width:0;flex:1}
.management-tip{padding:9px 12px;border-radius:999px;background:rgba(255,255,255,.82);display:flex;align-items:center;gap:8px;min-width:0;border:1px solid rgba(226,232,240,.66)}
.management-tip span{font-size:12px;font-weight:700;color:#64748b;white-space:nowrap}
.management-tip strong{font-size:13px;color:#1e293b;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.dashboard-refresh-btn{height:34px;padding:0 15px;border:none;border-radius:999px;background:linear-gradient(135deg,rgba(37,99,235,.12),rgba(96,165,250,.2));color:#2563eb;font-size:13px;font-weight:800;cursor:pointer;box-shadow:0 8px 18px rgba(37,99,235,.08);transition:all .18s ease}
.dashboard-refresh-btn:hover{transform:translateY(-1px);background:linear-gradient(135deg,#2563eb,#60a5fa);color:#fff;box-shadow:0 12px 24px rgba(37,99,235,.2)}
.role-switcher{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px;padding:12px;border-radius:24px;background:linear-gradient(135deg,rgba(239,246,255,.56),rgba(245,243,255,.38),rgba(255,247,237,.42))}
.role-switcher__item{position:relative;border:1px solid rgba(255,255,255,.74);border-radius:22px;padding:18px;text-align:left;cursor:pointer;overflow:hidden;background:linear-gradient(135deg,rgba(239,246,255,.88),rgba(255,255,255,.7));box-shadow:0 12px 24px rgba(37,99,235,.065);transition:transform .18s ease, box-shadow .18s ease, border-color .18s ease, background .18s ease}
.role-switcher__item::after{content:"";position:absolute;right:-28px;bottom:-34px;width:92px;height:92px;border-radius:999px;background:rgba(37,99,235,.08)}
.role-switcher__item:hover{transform:translateY(-2px);box-shadow:0 16px 30px rgba(15,23,42,.09)}
.role-switcher__item.active{border:1px solid transparent;color:#fff;transform:translateY(-2px)}
.role-switcher__item small,.role-switcher__item strong,.role-switcher__item span{position:relative;z-index:1}
.role-switcher__item small{display:block;font-size:12px;font-weight:800;opacity:.9}
.role-switcher__item strong{display:block;margin-top:10px;font-size:18px;color:#172554}
.role-switcher__item span{display:block;margin-top:8px;font-size:12px;color:#64748b;opacity:.9;line-height:1.6}
.role-switcher__item.active strong,.role-switcher__item.active span{color:#fff}
.role-switcher__item--operations{background:linear-gradient(135deg,rgba(219,234,254,.9),rgba(239,246,255,.76))}
.role-switcher__item--finance{background:linear-gradient(135deg,rgba(237,233,254,.9),rgba(245,243,255,.76))}
.role-switcher__item--warehouse{background:linear-gradient(135deg,rgba(220,252,231,.9),rgba(240,253,244,.76))}
.role-switcher__item--products{background:linear-gradient(135deg,rgba(255,237,213,.9),rgba(255,247,237,.76))}
.role-switcher__item--operations::after{background:rgba(37,99,235,.1)}
.role-switcher__item--finance::after{background:rgba(124,58,237,.1)}
.role-switcher__item--warehouse::after{background:rgba(22,163,74,.1)}
.role-switcher__item--products::after{background:rgba(249,115,22,.11)}
.role-switcher__item--operations.active{background:linear-gradient(135deg,#2563eb,#60a5fa)}
.role-switcher__item--finance.active{background:linear-gradient(135deg,#7c3aed,#a78bfa)}
.role-switcher__item--warehouse.active{background:linear-gradient(135deg,#16a34a,#86efac)}
.role-switcher__item--products.active{background:linear-gradient(135deg,#f97316,#fdba74)}
.summary-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:14px}
.summary-grid--in-card{margin-top:14px}
.summary-grid--in-card .summary-card{position:relative;overflow:hidden;background:linear-gradient(180deg,rgba(248,250,252,.96),rgba(241,245,249,.9));border:1px solid rgba(226,232,240,.86);box-shadow:0 10px 22px rgba(15,23,42,.045)}
.summary-grid--in-card .summary-card::after{content:"";position:absolute;right:-40px;bottom:-48px;width:118px;height:118px;border-radius:999px;background:rgba(148,163,184,.1)}
.summary-card{padding:20px 22px;border-radius:24px;transition:transform .18s ease, box-shadow .18s ease;text-align:left}
.summary-card--button{width:100%;border:none;cursor:pointer;font:inherit}
.summary-card:hover{transform:translateY(-1px);box-shadow:0 12px 26px rgba(15,23,42,.06)}
.summary-card__label,.summary-card__value,.summary-card__desc{position:relative;z-index:1}
.summary-card__label{font-size:12px;font-weight:900;letter-spacing:.04em;color:#475569}
.summary-card__value{margin-top:12px;font-size:34px;font-weight:900;color:#0f172a;letter-spacing:-.04em}
.summary-card__desc{margin-top:8px;font-size:13px;font-weight:700;color:#94a3b8}
.dashboard-grid{display:grid;grid-template-columns:minmax(0,1.65fr) minmax(320px,.85fr);gap:14px;align-items:start}
.dashboard-main,.dashboard-side{display:flex;flex-direction:column;gap:14px}
.chart-card,.table-card,.status-card,.list-card{padding:18px;border-radius:26px}
.list-card{min-height:520px}
.card-head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;margin-bottom:14px}
.card-head h3{margin:0;font-size:18px;color:#111827}
.card-head p{margin:6px 0 0;font-size:13px;color:#94a3b8}
.chart-box{height:260px}
.chart-box--lg{height:320px}
.risk-list{display:flex;flex-direction:column;gap:12px}
.risk-item{position:relative;display:flex;align-items:center;justify-content:space-between;gap:14px;width:100%;padding:16px 18px 16px 20px;border:1px solid rgba(219,234,254,.74);border-radius:20px;background:linear-gradient(135deg,rgba(248,251,255,.98),rgba(239,246,255,.9),rgba(245,243,255,.72));box-shadow:0 12px 28px rgba(37,99,235,.06);text-align:left;cursor:pointer;overflow:hidden;transition:transform .18s ease, box-shadow .18s ease, border-color .18s ease, background .18s ease}
.risk-item::before{content:"";position:absolute;left:0;top:14px;bottom:14px;width:4px;border-radius:999px;background:#2563eb;opacity:.8}
.risk-item:hover{transform:translateY(-2px);border-color:rgba(147,197,253,.82);box-shadow:0 16px 34px rgba(37,99,235,.12);background:linear-gradient(135deg,rgba(255,255,255,.98),rgba(226,239,255,.92),rgba(241,245,255,.82))}
.risk-item strong{display:block;font-size:14px;font-weight:900;color:#0f172a}
.risk-item p{margin:7px 0 0;font-size:12px;line-height:1.55;color:#94a3b8}
.risk-item__badge{min-width:48px;height:36px;padding:0 13px;border-radius:999px;display:inline-flex;align-items:center;justify-content:center;text-align:center;font-size:14px;font-weight:900;box-shadow:inset 0 0 0 1px rgba(255,255,255,.52)}
.risk-item:has(.risk-item__badge--high)::before{background:#ef4444}
.risk-item:has(.risk-item__badge--warning)::before{background:#f59e0b}
.risk-item:has(.risk-item__badge--normal)::before{background:#2563eb}
.risk-item__badge--high{background:linear-gradient(135deg,rgba(254,226,226,.96),rgba(254,202,202,.82));color:#dc2626}
.risk-item__badge--warning{background:linear-gradient(135deg,rgba(254,243,199,.98),rgba(253,230,138,.84));color:#d97706}
.risk-item__badge--normal{background:linear-gradient(135deg,rgba(219,234,254,.98),rgba(191,219,254,.82));color:#2563eb}
.nested-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}
.finance-flow-panel{display:flex;flex-direction:column;gap:12px}
.finance-flow-filter{display:flex;align-items:center;gap:10px;flex-wrap:wrap;padding:10px 12px;border:1px solid rgba(226,232,240,.86);border-radius:18px;background:linear-gradient(180deg,rgba(250,251,255,.96),rgba(243,246,255,.88));box-shadow:inset 0 1px 0 rgba(255,255,255,.78)}
.finance-flow-filter :deep(.el-segmented){--el-segmented-bg-color:rgba(241,245,249,.92);--el-segmented-item-selected-bg-color:#fff;--el-segmented-item-selected-color:#2563eb;--el-border-radius-base:12px;padding:3px;border:1px solid rgba(226,232,240,.9);box-shadow:none}
.finance-flow-filter :deep(.el-segmented__item){border-radius:10px;font-size:12px;font-weight:800;color:#64748b;transition:color .18s ease,background .18s ease,box-shadow .18s ease}
.finance-flow-filter :deep(.el-segmented__item.is-selected){box-shadow:0 6px 14px rgba(15,23,42,.08)}
.finance-flow-filter :deep(.el-select .el-input__wrapper),.finance-flow-filter :deep(.el-input__wrapper){border-radius:12px;background:rgba(255,255,255,.92);box-shadow:0 0 0 1px rgba(226,232,240,.9)}
.finance-flow-filter :deep(.el-input__wrapper.is-focus){box-shadow:0 0 0 1px rgba(37,99,235,.45)}
.finance-flow-search{width:260px;max-width:100%}
.finance-flow-export{border-radius:12px;font-weight:800}
.finance-flow-pagination{display:flex;align-items:center;justify-content:space-between;gap:12px;min-height:34px;padding:2px 4px 0}
.finance-flow-pagination__summary{font-size:12px;font-weight:800;color:#94a3b8;white-space:nowrap}
.finance-flow-table :deep(.el-table__header th){background:rgba(248,250,252,.96);color:#64748b;font-weight:900}
.finance-flow-table :deep(.finance-flow-row--diff td){background:rgba(255,247,237,.86)!important}
.finance-flow-link{display:block;max-width:100%;padding:0;border:0;background:transparent;color:#2563eb;font-weight:900;cursor:pointer;text-align:left;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.finance-flow-link:hover{text-decoration:underline}
.finance-flow-sub-no{display:block;margin-top:4px;color:#94a3b8;font-size:11px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.finance-remark-text{display:block;max-width:100%;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;cursor:default}
.finance-diff-remark{color:#d97706;font-weight:800}
.finance-amount{font-weight:950;font-variant-numeric:tabular-nums}
.finance-amount--income{color:#2563eb}
.finance-amount--refund{color:#f97316}
.nested-block,.ops-table-block{padding:14px;border-radius:20px;background:linear-gradient(180deg,rgba(250,251,255,.95),rgba(243,246,255,.88))}
.ops-table-stack{display:flex;flex-direction:column;gap:12px}
.ops-table-block{border:1px solid rgba(226,232,240,.7);box-shadow:inset 0 1px 0 rgba(255,255,255,.72)}
.ops-table-pagination{display:flex;align-items:center;justify-content:space-between;gap:12px;min-height:34px;padding:8px 4px 0}
.ops-table-pagination__summary{font-size:12px;font-weight:800;color:#94a3b8;white-space:nowrap}
.nested-block__title{margin-bottom:10px;font-size:14px;font-weight:800;color:#334155}
.dashboard-table{width:100%}
.dashboard-page--operations .hero-visual__panel strong{color:#1d4ed8}
.dashboard-page--finance .hero-visual__panel strong{color:#7c3aed}
.dashboard-page--warehouse .hero-visual__panel strong{color:#16a34a}
.dashboard-page--products .hero-visual__panel strong{color:#ea580c}
@media (max-width: 1280px){
  .role-switcher,.summary-grid,.dashboard-grid,.nested-grid{grid-template-columns:1fr 1fr}
  .dashboard-grid{grid-template-columns:1fr}
  .dashboard-hero{flex-direction:column}
  .hero-visual{flex-basis:auto;min-height:160px}
}
@media (max-width: 760px){
  .role-switcher,.summary-grid,.nested-grid{grid-template-columns:1fr}
  .dashboard-hero{padding:18px}
  .hero-copy h2{font-size:28px}
  .chart-box--lg,.chart-box{height:240px}
  .finance-flow-search{width:100%}
  .finance-flow-pagination{align-items:flex-start;flex-direction:column}
}
</style>

<style>
.finance-flow-remark-tooltip{max-width:360px!important;line-height:1.6;word-break:break-word}
</style>
