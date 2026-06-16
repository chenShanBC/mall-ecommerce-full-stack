import { computed, reactive, ref } from 'vue';
import * as XLSX from 'xlsx';
import {
  fetchAdminAftersales,
  fetchAdminGlobalRefunds,
  fetchAdminOrders,
  fetchAdminPayCallbackRecords,
  fetchAdminPayReconciliationRecords,
  fetchAdminPays,
  fetchAdminProductPage,
  fetchAdminProductSalesThresholdConfig,
  fetchAdminReconciliationOverview,
  fetchAdminStockReconciliations,
  fetchDashboard,
  fetchStockLogs,
  fetchWarningStocks,
} from '../../api';

const normalizeResponse = (response, fallback = {}) => response?.data?.data ?? fallback;
const recordsOf = (payload) => payload?.records || payload?.list || [];
const totalOf = (payload) => Number(payload?.total || payload?.totalCount || recordsOf(payload).length || 0);
const firstOf = (row, keys, fallback = undefined) => keys.map((key) => row?.[key]).find((value) => value !== undefined && value !== null) ?? fallback;
const toNumber = (value) => Number(value || 0);
const centToYuan = (value) => `¥${(toNumber(value) / 100).toFixed(2)}`;
const sumBy = (rows, keys) => rows.reduce((sum, row) => sum + toNumber(firstOf(row, keys, 0)), 0);
const normalizeProduct = (item) => ({
  ...item,
  id: firstOf(item, ['id', 'productId', 'spuId']),
  name: firstOf(item, ['name', 'productName', 'spuName', 'title'], '未命名商品'),
  status: firstOf(item, ['status', 'saleStatus', 'productStatus'], 'UNKNOWN'),
  salesCount: toNumber(firstOf(item, ['salesCount', 'saleCount', 'soldCount'], 0)),
  salesAmountCent: toNumber(firstOf(item, ['salesAmountCent', 'saleAmountCent', 'gmvCent', 'salesAmount'], 0)),
  availableStock: toNumber(firstOf(item, ['availableStock', 'stock', 'stockQuantity', 'totalStock'], 0)),
});
const normalizeOrder = (item) => ({
  ...item,
  id: toNumber(firstOf(item, ['id', 'orderId'], 0)),
  orderNo: firstOf(item, ['orderNo', 'orderSn', 'sn'], '--'),
  customerName: firstOf(item, ['customerName', 'buyerName', 'receiverName', 'userName', 'nickname', 'memberName', 'realName'], '--'),
  customerPhone: firstOf(item, ['customerPhone', 'buyerPhone', 'mobile', 'phone'], '--'),
  status: firstOf(item, ['status', 'orderStatus'], 'UNKNOWN'),
  payStatus: firstOf(item, ['payStatus', 'paymentStatus', 'status'], 'UNKNOWN'),
  fulfillmentStatus: firstOf(item, ['fulfillmentStatus', 'deliveryStatus', 'shippingStatus', 'fulfillStatus'], firstOf(item, ['status', 'orderStatus'], 'UNKNOWN')),
  aftersaleStatus: firstOf(item, ['aftersaleStatus', 'refundStatus', 'serviceStatus'], 'NONE'),
  paymentMethod: firstOf(item, ['paymentMethod', 'payMethod', 'channel', 'payChannel'], '--'),
  warehouseName: firstOf(item, ['warehouseName', 'deliveryWarehouseName', 'shipWarehouseName'], '--'),
  logisticsNo: firstOf(item, ['logisticsNo', 'trackingNo', 'expressNo', 'deliveryNo'], '--'),
  payAmountCent: toNumber(firstOf(item, ['payAmountCent', 'totalAmountCent', 'amountCent', 'actualAmountCent'], 0)),
  createTime: firstOf(item, ['createTime', 'createdAt', 'orderTime'], '--'),
  payTime: firstOf(item, ['payTime', 'paidAt', 'paymentTime'], '--'),
});
const normalizeAftersale = (item) => ({
  ...item,
  aftersaleNo: firstOf(item, ['aftersaleNo', 'refundNo', 'serviceNo'], '--'),
  orderNo: firstOf(item, ['orderNo', 'orderSn'], '--'),
  status: firstOf(item, ['status', 'reviewStatus'], 'UNKNOWN'),
  refundAmountCent: toNumber(firstOf(item, ['refundAmountCent', 'refundFeeCent', 'amountCent'], 0)),
  reason: firstOf(item, ['reason', 'applyReason', 'description'], '--'),
  createTime: firstOf(item, ['createTime', 'createdAt', 'applyTime'], '--'),
});
const normalizePay = (item) => ({
  ...item,
  orderNo: firstOf(item, ['orderNo', 'orderSn'], '--'),
  payNo: firstOf(item, ['payNo', 'paymentNo', 'tradeNo'], '--'),
  payStatus: firstOf(item, ['payStatus', 'status'], 'UNKNOWN'),
  payAmountCent: toNumber(firstOf(item, ['payAmountCent', 'amountCent', 'totalAmountCent'], 0)),
  createTime: firstOf(item, ['createTime', 'createdAt', 'payTime'], '--'),
});
const normalizeRefund = (item) => ({
  ...item,
  refundNo: firstOf(item, ['refundNo', 'refundOrderNo', 'aftersaleNo'], '--'),
  orderNo: firstOf(item, ['orderNo', 'orderSn'], '--'),
  status: firstOf(item, ['status', 'refundStatus'], 'UNKNOWN'),
  refundAmountCent: toNumber(firstOf(item, ['refundAmountCent', 'amountCent', 'refundFeeCent'], 0)),
});
const normalizeStockWarning = (item) => ({
  ...item,
  skuId: firstOf(item, ['skuId', 'productSkuId', 'id'], '--'),
  productName: firstOf(item, ['productName', 'spuName', 'skuName', 'name'], '--'),
  availableStock: toNumber(firstOf(item, ['availableStock', 'stock', 'stockQuantity'], 0)),
  lockedStock: toNumber(firstOf(item, ['lockedStock', 'lockStock'], 0)),
  lowStockThreshold: toNumber(firstOf(item, ['lowStockThreshold', 'warningThreshold', 'threshold'], 0)),
  warningStatus: firstOf(item, ['warningStatus', 'status'], 'UNKNOWN'),
});

export const DASHBOARD_ROLES = [
  { code: 'OPERATIONS', label: '运营', title: '运营驾驶舱', theme: 'operations', permission: 'dashboard:operations:view' },
  { code: 'FINANCE', label: '财务', title: '财务对账中心', theme: 'finance', permission: 'dashboard:finance:view' },
  { code: 'WAREHOUSE', label: '仓储', title: '仓储监控台', theme: 'warehouse', permission: 'dashboard:warehouse:view' },
  { code: 'PRODUCTS', label: '商品', title: '商品分析台', theme: 'products', permission: 'dashboard:products:view' },
];

export function createDefaultRange() {
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - 6);
  return { type: '7d', startDate: formatDate(start), endDate: formatDate(end) };
}

export function formatDate(date) {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
}

const buildRisk = (label, count, desc, level = 'NORMAL') => ({ label, count: toNumber(count), desc, level });

export function useDashboardData() {
  const range = ref(createDefaultRange());
  const activeRole = ref('OPERATIONS');
  const roleToken = ref(0);
  const loading = ref(false);
  const errors = reactive({});
  const state = reactive({ OPERATIONS: {}, FINANCE: {}, WAREHOUSE: {}, PRODUCTS: {} });
  const cache = new Map();

  const params = computed(() => ({ startDate: range.value.startDate, endDate: range.value.endDate, rangeType: range.value.type }));
  const cacheKeyOf = (role) => `${role}:${range.value.type}:${range.value.startDate}:${range.value.endDate}`;
  const getCache = (role) => cache.get(cacheKeyOf(role));
  const setCache = (role, payload) => cache.set(cacheKeyOf(role), payload);

  const resetRole = (role = activeRole.value, options = {}) => {
    roleToken.value += 1;
    if (options.clearState !== false) state[role] = {};
    if (options.clearCache) cache.delete(cacheKeyOf(role));
    errors[role] = '';
  };

  const safeLoad = async (role, loader, options = {}) => {
    const cachedPayload = options.force ? null : getCache(role);
    if (cachedPayload) {
      state[role] = cachedPayload;
      errors[role] = '';
      return;
    }

    const token = roleToken.value;
    loading.value = true;
    errors[role] = '';
    try {
      const payload = await loader();
      if (token === roleToken.value) {
        state[role] = payload;
        setCache(role, payload);
      }
    } catch (error) {
      if (token === roleToken.value) {
        errors[role] = error?.response?.data?.msg || error?.message || '数据加载失败';
        state[role] = {};
      }
    } finally {
      if (token === roleToken.value) loading.value = false;
    }
  };

  const loadOperations = (options = {}) => safeLoad('OPERATIONS', async () => {
    const [overviewRes, ordersRes, aftersalesRes] = await Promise.all([
      fetchDashboard(params.value),
      fetchAdminOrders({ ...params.value, page: 1, size: 8, sortBy: 'id', sortOrder: 'desc' }),
      fetchAdminAftersales({ ...params.value, page: 1, size: 8, sortField: 'createTime', sortOrder: 'desc' }),
    ]);
    const overview = normalizeResponse(overviewRes, {});
    const orders = normalizeResponse(ordersRes, {});
    const aftersales = normalizeResponse(aftersalesRes, {});
    const stats = overview.stats || {};
    const operationsTrend = Array.isArray(overview.operationsTrend) ? overview.operationsTrend : [];
    const orderRows = recordsOf(orders).map(normalizeOrder).sort((a, b) => Number(b.id || 0) - Number(a.id || 0));
    const aftersaleRows = recordsOf(aftersales).map(normalizeAftersale);
    const pendingReviewAftersaleCount = aftersaleRows.filter((item) => item.status === 'PENDING_REVIEW').length;
    const summary = {
      totalOrderCount: toNumber(stats.totalOrderCount || totalOf(orders)),
      todayOrderCount: toNumber(stats.todayOrderCount),
      pendingOrderCount: toNumber(stats.pendingOrderCount),
      paidOrderCount: toNumber(stats.paidOrderCount || stats.payOrderCount),
      shippedOrderCount: toNumber(stats.shippedOrderCount),
      completedOrderCount: toNumber(stats.completedOrderCount),
      cancelledOrderCount: toNumber(stats.cancelledOrderCount || stats.cancelOrderCount || stats.closedOrderCount),
      abnormalOrderCount: toNumber(stats.paymentExceptionOrderCount || overview.abnormalOrderCount),
      pendingAftersaleCount: pendingReviewAftersaleCount,
      fulfillmentRate: toNumber(overview.fulfillmentRate),
      paidAmountCent: toNumber(overview.paidAmountCent || sumBy(orderRows, ['payAmountCent', 'totalAmountCent', 'amountCent'])),
    };
    const risks = [
      buildRisk('待发货订单', summary.paidOrderCount || summary.pendingOrderCount, '已付款订单需要及时履约发货', summary.paidOrderCount > 0 ? 'WARNING' : 'NORMAL'),
      buildRisk('支付异常订单', summary.abnormalOrderCount, '订单支付状态与履约状态可能不一致', summary.abnormalOrderCount > 0 ? 'HIGH' : 'NORMAL'),
      buildRisk('待审核售后', summary.pendingAftersaleCount, '售后申请需要运营尽快审核', summary.pendingAftersaleCount > 0 ? 'WARNING' : 'NORMAL'),
      buildRisk('待支付订单', summary.pendingOrderCount, '待支付订单需要关注超时关闭与用户支付转化', summary.pendingOrderCount > 0 ? 'LOW' : 'NORMAL'),
    ];
    return { overview, stats, summary, operationsTrend, risks, todos: overview.todos?.length ? overview.todos : risks, orderRows, orderTotal: totalOf(orders), aftersaleRows, aftersaleTotal: totalOf(aftersales), orderOverviewRows: orderRows.slice(0, 6), orderDetailRows: orderRows.slice(0, 4).map((item, index) => ({ ...item, rowType: index < 2 ? 'ORDER' : 'AFTERSALE' })), };
  }, options);

  const loadFinance = (options = {}) => safeLoad('FINANCE', async () => {
    const [overviewRes, paysRes, refundsRes, reconcileRes, diffRes, callbacksRes] = await Promise.all([
      fetchDashboard(params.value),
      fetchAdminPays({ ...params.value, page: 1, size: 8, sortField: 'createTime', sortOrder: 'desc' }),
      fetchAdminGlobalRefunds({ ...params.value, page: 1, size: 8, sortField: 'createTime', sortOrder: 'desc' }),
      fetchAdminReconciliationOverview(),
      fetchAdminPayReconciliationRecords({ ...params.value, page: 1, size: 8, sortField: 'createTime', sortOrder: 'desc' }),
      fetchAdminPayCallbackRecords({ ...params.value, page: 1, size: 8, sortField: 'createTime', sortOrder: 'desc' }),
    ]);
    const overview = normalizeResponse(overviewRes, {});
    const reconcile = normalizeResponse(reconcileRes, {});
    const paysPayload = normalizeResponse(paysRes, {});
    const refundsPayload = normalizeResponse(refundsRes, {});
    const diffsPayload = normalizeResponse(diffRes, {});
    const callbacksPayload = normalizeResponse(callbacksRes, {});
    const pays = recordsOf(paysPayload);
    const refunds = recordsOf(refundsPayload);
    const diffs = recordsOf(diffsPayload);
    const callbacks = recordsOf(callbacksPayload);
    const paidAmountCent = toNumber(overview.paidAmountCent || sumBy(pays, ['payAmountCent', 'amountCent', 'totalAmountCent']));
    const refundAmountCent = toNumber(overview.refundAmountCent || sumBy(refunds, ['refundAmountCent', 'amountCent']));
    const summary = {
      paidAmountCent,
      refundAmountCent,
      netIncomeCent: toNumber(overview.netIncomeCent || paidAmountCent - refundAmountCent),
      payOrderCount: totalOf(paysPayload),
      refundCount: totalOf(refundsPayload),
      taskCount: toNumber(reconcile.taskCount || reconcile.totalCount),
      pendingReconcileCount: toNumber(reconcile.pendingCount || totalOf(diffsPayload)),
      hangingCount: toNumber(reconcile.hangingCount),
      archivedCount: toNumber(reconcile.archivedCount || reconcile.doneCount),
      abnormalReconcileCount: toNumber(reconcile.diffCount || totalOf(diffsPayload)),
      doneReconcileCount: toNumber(reconcile.doneCount),
      callbackFailedCount: callbacks.filter((item) => ['FAILED', 'ERROR'].includes(item.status || item.handleStatus)).length,
      successRate: toNumber(reconcile.successRate),
    };
    const risks = [
      buildRisk('未处理对账差异', summary.pendingReconcileCount, '优先处理支付、退款与渠道账单差异', summary.pendingReconcileCount > 0 ? 'HIGH' : 'NORMAL'),
      buildRisk('挂账未闭环', summary.hangingCount, '已挂账差异需要持续跟进到归档', summary.hangingCount > 0 ? 'WARNING' : 'NORMAL'),
      buildRisk('支付回调失败', summary.callbackFailedCount, '回调失败可能造成资金状态不同步', summary.callbackFailedCount > 0 ? 'HIGH' : 'NORMAL'),
      buildRisk('退款记录', summary.refundCount, '关注退款状态同步和异常退款单', summary.refundCount > 0 ? 'LOW' : 'NORMAL'),
    ];
    return { overview: { ...overview, ...summary }, reconcile, summary, risks, todos: risks, pays, refunds, diffs, callbacks };
  }, options);

  const loadWarehouse = (options = {}) => safeLoad('WAREHOUSE', async () => {
    const [overviewRes, warningsRes, logsRes, reconciliationsRes] = await Promise.all([
      fetchDashboard(params.value),
      fetchWarningStocks({ ...params.value, page: 1, size: 8, sortField: 'availableStock', sortOrder: 'asc' }),
      fetchStockLogs({ ...params.value, page: 1, size: 8, sortField: 'createTime', sortOrder: 'desc' }),
      fetchAdminStockReconciliations({ ...params.value, page: 1, size: 8, sortField: 'createTime', sortOrder: 'desc' }),
    ]);
    const overview = normalizeResponse(overviewRes, {});
    const warningsPayload = normalizeResponse(warningsRes, {});
    const logsPayload = normalizeResponse(logsRes, {});
    const reconciliationsPayload = normalizeResponse(reconciliationsRes, {});
    const warnings = recordsOf(warningsPayload);
    const logs = recordsOf(logsPayload);
    const reconciliations = recordsOf(reconciliationsPayload);
    const stockStats = overview.stockWarningStats || {};
    const outOfStockCount = warnings.filter((item) => toNumber(item.availableStock) <= 0).length;
    const lockedStockTotal = warnings.reduce((sum, item) => sum + toNumber(item.lockedStock), 0);
    const summary = {
      totalSkuCount: toNumber(stockStats.totalCount || totalOf(warningsPayload)),
      lowStockCount: toNumber(stockStats.lowCount || warnings.filter((item) => toNumber(item.availableStock) <= toNumber(item.lowStockThreshold)).length),
      highStockCount: toNumber(stockStats.highCount),
      normalCount: toNumber(stockStats.normalCount),
      warningCount: totalOf(warningsPayload),
      outOfStockCount,
      pendingWarningCount: warnings.filter((item) => !['DONE', 'HANDLED'].includes(item.warningStatus || item.status)).length,
      stockLogCount: totalOf(logsPayload),
      stockDiffCount: totalOf(reconciliationsPayload),
      lockedStockTotal,
    };
    const risks = [
      buildRisk('缺货 SKU', summary.outOfStockCount, '可售库存为 0 的 SKU 需要优先补货', summary.outOfStockCount > 0 ? 'HIGH' : 'NORMAL'),
      buildRisk('低库存 SKU', summary.lowStockCount, '低于预警阈值的 SKU 需要补货', summary.lowStockCount > 0 ? 'WARNING' : 'NORMAL'),
      buildRisk('库存对账差异', summary.stockDiffCount, '系统库存与实际库存存在差异', summary.stockDiffCount > 0 ? 'HIGH' : 'NORMAL'),
      buildRisk('锁定库存总量', summary.lockedStockTotal, '关注长时间锁定未释放库存', summary.lockedStockTotal > 0 ? 'LOW' : 'NORMAL'),
    ];
    return { overview, summary, risks, todos: risks, warnings, warningTotal: totalOf(warningsPayload), logs, reconciliations };
  }, options);

  const loadProducts = (options = {}) => safeLoad('PRODUCTS', async () => {
    const [overviewRes, productsRes, thresholdRes] = await Promise.all([
      fetchDashboard(params.value),
      fetchAdminProductPage({ ...params.value, page: 1, size: 12, sortField: 'salesCount', sortOrder: 'desc' }),
      fetchAdminProductSalesThresholdConfig(),
    ]);
    const overview = normalizeResponse(overviewRes, {});
    const productsPayload = normalizeResponse(productsRes, {});
    const threshold = normalizeResponse(thresholdRes, {});
    const products = recordsOf(productsPayload).map(normalizeProduct);
    const hotThreshold = toNumber(threshold.hotThreshold || threshold.hotSalesThreshold || 100);
    const slowThreshold = toNumber(threshold.slowThreshold || threshold.lowSalesThreshold || 1);
    const hotProducts = products.filter((item) => item.salesCount >= hotThreshold);
    const slowProducts = products.filter((item) => item.salesCount <= slowThreshold);
    const lowStockHotProducts = hotProducts.filter((item) => item.availableStock <= 10);
    const highStockSlowProducts = slowProducts.filter((item) => item.availableStock >= 50);
    const summary = {
      productTotal: totalOf(productsPayload),
      onSaleCount: toNumber(overview.onSaleProductCount || products.filter((item) => ['ON_SALE', 'ONSALE', 'SALE'].includes(item.status)).length),
      offSaleCount: toNumber(overview.offSaleProductCount || products.filter((item) => ['OFF_SALE', 'OFFSALE', 'OFF'].includes(item.status)).length),
      totalSalesCount: sumBy(products, ['salesCount']),
      totalSalesAmountCent: sumBy(products, ['salesAmountCent', 'salesAmount']),
      hotThreshold,
      slowThreshold,
      hotCount: hotProducts.length,
      slowCount: slowProducts.length,
      lowStockHotCount: lowStockHotProducts.length,
      highStockSlowCount: highStockSlowProducts.length,
    };
    const risks = [
      buildRisk('高销量低库存', summary.lowStockHotCount, '热销商品库存不足，建议补货', summary.lowStockHotCount > 0 ? 'HIGH' : 'NORMAL'),
      buildRisk('低销量高库存', summary.highStockSlowCount, '库存积压商品建议促销或清仓', summary.highStockSlowCount > 0 ? 'WARNING' : 'NORMAL'),
      buildRisk('滞销商品', summary.slowCount, '低销量商品建议优化图文、价格或下架', summary.slowCount > 0 ? 'LOW' : 'NORMAL'),
      buildRisk('热销商品', summary.hotCount, '保持热销商品库存供给稳定', 'NORMAL'),
    ];
    return { overview, products, productTotal: totalOf(productsPayload), threshold, summary, risks, todos: risks };
  }, options);

  const loadCurrentRole = async (options = {}) => {
    const loaders = { OPERATIONS: loadOperations, FINANCE: loadFinance, WAREHOUSE: loadWarehouse, PRODUCTS: loadProducts };
    await loaders[activeRole.value]?.(options);
  };

  const switchRole = async (role) => {
    activeRole.value = role;
    if (getCache(role)) {
      state[role] = getCache(role);
      errors[role] = '';
      return;
    }
    await loadCurrentRole();
  };

  const updateRange = async (nextRange) => {
    range.value = nextRange;
    cache.clear();
    resetRole(activeRole.value, { clearState: false });
    await loadCurrentRole();
  };

  const exportRoleReport = (role = activeRole.value) => {
    const workbook = XLSX.utils.book_new();
    const payload = state[role] || {};
    Object.entries(payload).forEach(([key, value]) => {
      const rows = Array.isArray(value) ? value : [value || {}];
      XLSX.utils.book_append_sheet(workbook, XLSX.utils.json_to_sheet(rows), key.slice(0, 31));
    });
    XLSX.writeFile(workbook, `${role}-dashboard-${range.value.startDate}_${range.value.endDate}.xlsx`);
  };

  return { activeRole, range, loading, errors, state, resetRole, switchRole, updateRange, loadCurrentRole, exportRoleReport, centToYuan };
}
