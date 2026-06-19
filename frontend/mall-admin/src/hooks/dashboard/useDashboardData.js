import { computed, reactive, ref } from 'vue';
import * as XLSX from 'xlsx';
import { useAdminStore } from '../../stores/admin';
import {
  fetchAdminAftersales,
  fetchAdminFinanceCumulativeNetIncome,
  fetchAdminFinanceTrend,
  fetchAdminGlobalRefunds,
  fetchAdminOnlineArchiveReport,
  fetchAdminOnlineHangingFollows,
  fetchAdminOnlineDiffItems,
  fetchAdminOnlineReconcileTasks,
  fetchAdminOrders,
  fetchAdminPayCallbackRecords,
  fetchAdminPays,
  fetchAdminProductPage,
  fetchAdminProductSalesThresholdConfig,
  fetchAdminReconciliationOverview,
  fetchAdminStockReconciliations,
  fetchAdminTodayActiveStocks,
  fetchAdminWarehouseTrend,
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
  productId: firstOf(item, ['productId', 'id', 'spuId']),
  name: firstOf(item, ['name', 'productName', 'spuName', 'title'], '未命名商品'),
  status: firstOf(item, ['status', 'saleStatus', 'productStatus'], 'UNKNOWN'),
  salesBand: String(firstOf(item, ['salesBand'], '') || '').toUpperCase(),
  salesCount: toNumber(firstOf(item, ['salesCount', 'saleCount', 'soldCount'], 0)),
  monthlySalesCount: toNumber(firstOf(item, ['monthlySalesCount', 'monthly_sales_count', 'recent30DaySalesCount'], firstOf(item, ['salesCount', 'saleCount', 'soldCount'], 0))),
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
  payNo: firstOf(item, ['payNo', 'payOrderNo', 'paymentNo', 'tradeNo'], '--'),
  payOrderNo: firstOf(item, ['payOrderNo', 'payNo', 'paymentNo', 'tradeNo'], '--'),
  payStatus: firstOf(item, ['payStatus', 'status'], 'UNKNOWN'),
  reconcileStatus: firstOf(item, ['reconcileStatus', 'reconciliationStatus'], ''),
  diffRemark: firstOf(item, ['diffRemark', 'reconcileRemark', 'reconciliationRemark', 'remark', 'processRemark', 'handleRemark', 'suggestedAction', 'diffType', 'reason', 'failReason', 'exceptionReason'], ''),
  payAmountCent: toNumber(firstOf(item, ['payAmountCent', 'payAmount', 'amountCent', 'totalAmountCent'], 0)),
  payTime: firstOf(item, ['payTime', 'paidAt'], ''),
  createTime: firstOf(item, ['createTime', 'createdAt', 'created_at', 'create_time', 'gmtCreate', 'gmt_create'], ''),
});
const normalizeRefund = (item) => ({
  ...item,
  refundNo: firstOf(item, ['refundNo', 'refundOrderNo', 'aftersaleNo'], '--'),
  orderNo: firstOf(item, ['orderNo', 'orderSn'], '--'),
  status: firstOf(item, ['status', 'refundStatus'], 'UNKNOWN'),
  reconcileStatus: firstOf(item, ['reconcileStatus', 'reconciliationStatus'], ''),
  diffRemark: firstOf(item, ['diffRemark', 'reconcileRemark', 'reconciliationRemark', 'remark', 'processRemark', 'handleRemark', 'suggestedAction', 'diffType', 'reason', 'failReason', 'exceptionReason'], ''),
  refundAmountCent: toNumber(firstOf(item, ['refundAmountCent', 'amountCent', 'refundFeeCent'], 0)),
  channel: firstOf(item, ['channel', 'payChannel', 'paymentMethod'], ''),
  refundTime: firstOf(item, ['refundTime'], ''),
  createTime: firstOf(item, ['createTime', 'createdAt', 'created_at', 'create_time', 'gmtCreate', 'gmt_create'], ''),
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

const createCurrentMonthRange = () => {
  const end = new Date();
  const start = new Date(end.getFullYear(), end.getMonth(), 1);
  return { type: 'month', startDate: formatDate(start), endDate: formatDate(end), rangeType: 'month' };
};

const dateFromTimestamp = (timestamp) => {
  const value = Number(timestamp);
  const date = Number.isFinite(value) && value > 0 ? new Date(value) : new Date();
  return formatDate(date);
};

const buildRisk = (label, count, desc, level = 'NORMAL') => ({ label, count: toNumber(count), desc, level });
const DEFAULT_PRODUCT_SALES_THRESHOLD = { hotSalesThreshold: 100, lowSalesThreshold: 10 };
const normalizeSalesThreshold = (value, fallback) => {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) && numberValue >= 0 ? numberValue : fallback;
};
const productSalesThresholdSessionKey = (adminId) => `mallfei-admin-product-sales-threshold-session:${adminId || 'anonymous'}`;
const loadProductSalesThresholdSession = (adminId) => {
  try {
    const saved = JSON.parse(sessionStorage.getItem(productSalesThresholdSessionKey(adminId)) || 'null');
    if (!saved) return null;
    return {
      hotSalesThreshold: normalizeSalesThreshold(saved.hotSalesThreshold, DEFAULT_PRODUCT_SALES_THRESHOLD.hotSalesThreshold),
      lowSalesThreshold: normalizeSalesThreshold(saved.lowSalesThreshold, DEFAULT_PRODUCT_SALES_THRESHOLD.lowSalesThreshold),
    };
  } catch {
    return null;
  }
};
const normalizeProductSalesThresholdConfig = (threshold = {}) => ({
  hotSalesThreshold: normalizeSalesThreshold(threshold.hotThreshold || threshold.hotSalesThreshold, DEFAULT_PRODUCT_SALES_THRESHOLD.hotSalesThreshold),
  lowSalesThreshold: normalizeSalesThreshold(threshold.slowThreshold || threshold.lowSalesThreshold, DEFAULT_PRODUCT_SALES_THRESHOLD.lowSalesThreshold),
});

export function useDashboardData() {
  const adminStore = useAdminStore();
  const range = ref(createDefaultRange());
  const activeRole = ref('OPERATIONS');
  const roleToken = ref(0);
  const loading = ref(false);
  const errors = reactive({});
  const state = reactive({ OPERATIONS: {}, FINANCE: {}, WAREHOUSE: {}, PRODUCTS: {} });
  const cache = new Map();

  const todayStockDate = computed(() => formatDate(new Date()));
  const params = computed(() => ({ startDate: range.value.startDate, endDate: range.value.endDate, rangeType: range.value.type }));
  const productThresholdCacheKey = () => {
    const sessionThreshold = loadProductSalesThresholdSession(adminStore.adminId);
    if (sessionThreshold) return `session:${sessionThreshold.hotSalesThreshold}:${sessionThreshold.lowSalesThreshold}`;
    return 'default';
  };
  const cacheKeyOf = (role) => `${role}:${range.value.type}:${range.value.startDate}:${range.value.endDate}:${role === 'WAREHOUSE' ? todayStockDate.value : ''}:${role === 'PRODUCTS' ? productThresholdCacheKey() : ''}`;
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
      fetchAdminOrders({ ...params.value, page: 1, size: 50, sortBy: 'id', sortOrder: 'desc' }),
      fetchAdminAftersales({ ...params.value, page: 1, size: 50, sortField: 'createTime', sortOrder: 'desc' }),
    ]);
    const overview = normalizeResponse(overviewRes, {});
    const orders = normalizeResponse(ordersRes, {});
    const aftersales = normalizeResponse(aftersalesRes, {});
    const stats = overview.stats || {};
    const operationsTrend = Array.isArray(overview.operationsTrend) ? overview.operationsTrend : [];
    const orderRows = recordsOf(orders).map(normalizeOrder).sort((a, b) => Number(b.id || 0) - Number(a.id || 0));
    const aftersaleRows = recordsOf(aftersales).map(normalizeAftersale);
    const pendingReviewAftersaleCount = aftersaleRows.filter((item) => item.status === 'PENDING_REVIEW').length;
    const totalOrderCount = toNumber(stats.totalOrderCount || totalOf(orders));
    const pendingOrderCount = toNumber(stats.pendingOrderCount);
    const paidOrderCount = toNumber(stats.paidOrderCount || stats.payOrderCount);
    const shippedOrderCount = toNumber(stats.shippedOrderCount);
    const completedOrderCount = toNumber(stats.completedOrderCount);
    const cancelledOrderCount = toNumber(stats.cancelledOrderCount || stats.cancelOrderCount || stats.closedOrderCount);
    const fulfillmentBaseCount = paidOrderCount + shippedOrderCount + completedOrderCount || Math.max(totalOrderCount - pendingOrderCount - cancelledOrderCount, 0);
    const calculatedFulfillmentRate = fulfillmentBaseCount > 0 ? Math.round((completedOrderCount / fulfillmentBaseCount) * 100) : 0;
    const overviewFulfillmentRate = toNumber(overview.fulfillmentRate);
    const summary = {
      totalOrderCount,
      todayOrderCount: toNumber(stats.todayOrderCount),
      pendingOrderCount,
      paidOrderCount,
      shippedOrderCount,
      completedOrderCount,
      cancelledOrderCount,
      abnormalOrderCount: toNumber(stats.paymentExceptionOrderCount || overview.abnormalOrderCount),
      pendingAftersaleCount: pendingReviewAftersaleCount,
      fulfillmentRate: overviewFulfillmentRate > 0 || completedOrderCount === 0 ? overviewFulfillmentRate : calculatedFulfillmentRate,
      paidAmountCent: toNumber(overview.paidAmountCent || sumBy(orderRows, ['payAmountCent', 'totalAmountCent', 'amountCent'])),
    };
    const risks = [
      buildRisk('待发货订单', summary.paidOrderCount || summary.pendingOrderCount, '已付款订单需要及时履约发货', summary.paidOrderCount > 0 ? 'WARNING' : 'NORMAL'),
      buildRisk('支付异常订单', summary.abnormalOrderCount, '订单支付状态与履约状态可能不一致', summary.abnormalOrderCount > 0 ? 'HIGH' : 'NORMAL'),
      buildRisk('待审核售后', summary.pendingAftersaleCount, '售后申请需要运营尽快审核', summary.pendingAftersaleCount > 0 ? 'WARNING' : 'NORMAL'),
      buildRisk('待支付订单', summary.pendingOrderCount, '待支付订单需要关注超时关闭与用户支付转化', summary.pendingOrderCount > 0 ? 'LOW' : 'NORMAL'),
    ];
    return { overview, stats, summary, operationsTrend, risks, todos: overview.todos?.length ? overview.todos : risks, orderRows, orderTotal: totalOf(orders), aftersaleRows, aftersaleTotal: totalOf(aftersales), orderOverviewRows: orderRows.slice(0, 50), orderDetailRows: orderRows.slice(0, 4).map((item, index) => ({ ...item, rowType: index < 2 ? 'ORDER' : 'AFTERSALE' })), };
  }, options);

  const loadFinance = (options = {}) => safeLoad('FINANCE', async () => {
    const monthParams = createCurrentMonthRange();
    const latestFlowEnd = new Date();
    const latestFlowStart = new Date();
    latestFlowStart.setDate(latestFlowEnd.getDate() - 6);
    const latestFlowRange = { startDate: formatDate(latestFlowStart), endDate: formatDate(latestFlowEnd) };
    const latestFlowParams = { ...latestFlowRange, page: 1, size: 100, sortField: 'createTime', sortBy: 'createTime', sortOrder: 'desc' };
    const monthFlowParams = { ...monthParams, page: 1, size: 10000, sortField: 'createTime', sortBy: 'createTime', sortOrder: 'desc' };
    const [overviewRes, cumulativeNetIncomeRes, financeTrendRes, latestPaysRes, monthPaysRes, refundedPaysRes, latestRefundsRes, monthRefundsRes, reconcileRes, pendingTasksRes, completedTasksRes, archiveReportRes, hangingRes, callbacksRes] = await Promise.all([
      fetchDashboard(monthParams),
      fetchAdminFinanceCumulativeNetIncome(),
      fetchAdminFinanceTrend(),
      fetchAdminPays(latestFlowParams),
      fetchAdminPays(monthFlowParams),
      fetchAdminPays({ ...monthParams, status: 'REFUNDED', page: 1, size: 1, sortField: 'createTime', sortOrder: 'desc' }),
      fetchAdminGlobalRefunds(latestFlowParams),
      fetchAdminGlobalRefunds(monthFlowParams),
      fetchAdminReconciliationOverview(),
      fetchAdminOnlineReconcileTasks({ page: 1, size: 200 }),
      fetchAdminOnlineReconcileTasks({ page: 1, size: 1, status: 'COMPLETED' }),
      fetchAdminOnlineArchiveReport({ startDate: monthParams.startDate, endDate: monthParams.endDate }),
      fetchAdminOnlineHangingFollows({ page: 1, size: 200 }),
      fetchAdminPayCallbackRecords({ ...monthParams, page: 1, size: 8, sortField: 'createTime', sortOrder: 'desc' }),
    ]);
    const overview = normalizeResponse(overviewRes, {});
    const cumulativeNetIncome = normalizeResponse(cumulativeNetIncomeRes, {});
    const financeTrend = normalizeResponse(financeTrendRes, []);
    const reconcile = normalizeResponse(reconcileRes, {});
    const paysPayload = normalizeResponse(latestPaysRes, {});
    const monthPaysPayload = normalizeResponse(monthPaysRes, {});
    const refundedPaysPayload = normalizeResponse(refundedPaysRes, {});
    const refundsPayload = normalizeResponse(latestRefundsRes, {});
    const monthRefundsPayload = normalizeResponse(monthRefundsRes, {});
    const pendingTasksPayload = normalizeResponse(pendingTasksRes, {});
    const completedTasksPayload = normalizeResponse(completedTasksRes, {});
    const archiveReportPayload = normalizeResponse(archiveReportRes, {});
    const hangingPayload = normalizeResponse(hangingRes, {});
    const callbacksPayload = normalizeResponse(callbacksRes, {});
    const successPayStatuses = ['SUCCESS', 'REFUND_PENDING', 'REFUNDING', 'PARTIALLY_REFUNDED', 'REFUND_FAILED'];
    const isDisplayPay = (item) => !['CLOSED', 'CANCELLED'].includes(String(firstOf(item, ['payStatus', 'status', 'tradeStatus'], '')).toUpperCase());
    const isFinanceSuccessPay = (item) => successPayStatuses.includes(String(firstOf(item, ['payStatus', 'status', 'tradeStatus'], '')).toUpperCase());
    const isDisplayRefund = (item) => !['CANCELLED', 'CLOSED', 'REJECTED'].includes(String(firstOf(item, ['status', 'refundStatus'], '')).toUpperCase());
    const archiveReportRows = [
      ...recordsOf(archiveReportPayload.tasks ? { records: archiveReportPayload.tasks } : archiveReportPayload),
      ...(Array.isArray(archiveReportPayload.tasks) ? archiveReportPayload.tasks : []),
      ...(Array.isArray(archiveReportPayload.records) ? archiveReportPayload.records : []),
      ...(Array.isArray(archiveReportPayload.list) ? archiveReportPayload.list : []),
      ...(Array.isArray(archiveReportPayload.items) ? archiveReportPayload.items : []),
    ];
    const completedTaskRows = recordsOf(completedTasksPayload);
    const archivedTasks = Array.from(new Set([...archiveReportRows, ...completedTaskRows].filter(Boolean)));
    const archivedTaskByKey = new Map();
    const archivedTaskByDateChannel = new Map();
    const normalizeReconcileKey = (key) => String(key || '').trim();
    const isCompletedReconcileRecord = (record) => {
      const processStatus = String(firstOf(record, ['processStatus', 'handleStatus', 'handleResult', 'processResult'], '')).toUpperCase();
      const taskStatus = String(firstOf(record, ['status', 'taskStatus', 'reconcileStatus', 'archiveStatus'], '')).toUpperCase();
      return ['DONE', 'FINISHED', 'SUCCESS', 'RESOLVED', 'PROCESSED', 'IGNORED'].includes(processStatus)
        || ['COMPLETED', 'COMPLETE', 'ARCHIVED', 'ARCHIVE', 'ARCHIVE_DONE', 'CLOSED'].includes(taskStatus)
        || Boolean(firstOf(record, ['archivedAt', 'archiveTime', 'completedAt', 'completeTime', 'finishedAt', 'finishTime', 'processedAt', 'processTime', 'handledAt', 'handleTime'], ''));
    };
    const currentYear = String(monthParams.startDate || '').slice(0, 4) || String(new Date().getFullYear());
    const normalizeReconcileDate = (value) => {
      const text = normalizeReconcileKey(value);
      const fullDateMatched = text.match(/\d{4}[-/]\d{1,2}[-/]\d{1,2}/);
      if (fullDateMatched) {
        const [year, month, day] = fullDateMatched[0].replace(/\//g, '-').split('-');
        return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
      }
      const shortDateMatched = text.match(/(^|\D)(\d{1,2})[-/月](\d{1,2})(日)?(\D|$)/);
      if (shortDateMatched) return `${currentYear}-${String(shortDateMatched[2]).padStart(2, '0')}-${String(shortDateMatched[3]).padStart(2, '0')}`;
      return '';
    };
    const normalizeReconcileChannel = (value) => {
      const text = normalizeReconcileKey(value).toUpperCase().replace(/[\s_-]/g, '');
      if (!text || text === '--') return '';
      if (['WECHAT', 'WECHATPAY', 'WXPAY', 'WX', '微信', '微信支付'].includes(text)) return 'WECHAT_PAY';
      if (['ALI', 'ALIPAY', 'ALIPAYPAY', '支付宝'].includes(text)) return 'ALIPAY';
      if (['MOCK', '模拟渠道', '模拟支付', '模拟'].includes(text)) return 'MOCK';
      return text;
    };
    const buildDateChannelKey = (date, channel) => {
      const normalizedDate = normalizeReconcileDate(date);
      const normalizedChannel = normalizeReconcileChannel(channel);
      return normalizedDate && normalizedChannel ? `${normalizedDate}:${normalizedChannel}` : '';
    };
    const putRecordKey = (map, key, record) => {
      const normalizedKey = normalizeReconcileKey(key);
      if (normalizedKey && normalizedKey !== '--' && !map.has(normalizedKey)) map.set(normalizedKey, record);
    };
    const putBizOrderKey = (map, bizType, orderNo, record) => {
      const normalizedBizType = normalizeReconcileKey(bizType).toUpperCase();
      const normalizedOrderNo = normalizeReconcileKey(orderNo);
      if (normalizedBizType && normalizedOrderNo && normalizedOrderNo !== '--') putRecordKey(map, `${normalizedBizType}:${normalizedOrderNo}`, record);
    };
    const putDateChannelKey = (map, date, channel, record) => {
      const key = buildDateChannelKey(date, channel);
      if (key && !map.has(key)) map.set(key, record);
    };
    const pickReconcileDate = (record) => firstOf(record, [
      'reconcileDate', 'reconciliationDate', 'billDate', 'tradeDate', 'payDate', 'paidDate', 'paymentDate', 'refundDate', 'date', 'day', 'archiveDate', 'archivedDate', 'archiveTime', 'archivedAt', 'createdAt', 'createTime', 'occurTime', 'payTime', 'refundTime',
    ]);
    const pickReconcileChannel = (record) => firstOf(record, [
      'channel', 'payChannel', 'paymentChannel', 'paymentMethod', 'tradeChannel', 'billChannel', 'payMethod', 'method', 'channelCode', 'payChannelCode',
    ]);
    const collectTaskRecords = (task) => [
      task,
      ...(Array.isArray(task?.items) ? task.items : []),
      ...(Array.isArray(task?.diffItems) ? task.diffItems : []),
      ...(Array.isArray(task?.localBills) ? task.localBills : []),
      ...(Array.isArray(task?.bills) ? task.bills : []),
      ...(Array.isArray(task?.logs) ? task.logs : []),
      ...(Array.isArray(task?.records) ? task.records : []),
      ...(Array.isArray(task?.details) ? task.details : []),
      ...(Array.isArray(task?.children) ? task.children : []),
    ];
    archivedTasks.forEach((task) => {
      collectTaskRecords(task).forEach((record) => {
        putRecordKey(archivedTaskByKey, firstOf(record, ['payOrderNo', 'payNo', 'paymentNo', 'tradeNo', 'channelTradeNo']), task);
        putRecordKey(archivedTaskByKey, firstOf(record, ['refundNo', 'refundOrderNo', 'aftersaleNo', 'channelRefundNo']), task);
        putRecordKey(archivedTaskByKey, firstOf(record, ['orderNo', 'orderSn', 'bizOrderNo']), task);
        putBizOrderKey(archivedTaskByKey, firstOf(record, ['bizType', 'businessType', 'flowType', 'billType']), firstOf(record, ['orderNo', 'orderSn', 'bizOrderNo']), task);
        putDateChannelKey(archivedTaskByDateChannel, pickReconcileDate(record) || pickReconcileDate(task), pickReconcileChannel(record) || pickReconcileChannel(task), task);
      });
      putDateChannelKey(archivedTaskByDateChannel, pickReconcileDate(task), pickReconcileChannel(task), task);
    });
    const findByFlowKeys = (map, row) => map.get(row.payOrderNo) || map.get(row.payNo) || map.get(row.channelTradeNo) || map.get(row.refundNo) || map.get(row.channelRefundNo) || map.get(`${String(row.flowType || row.bizType || '').toUpperCase()}:${row.orderNo}`);
    const findArchivedTaskByDateChannel = (row) => archivedTaskByDateChannel.get(buildDateChannelKey(pickReconcileDate(row), pickReconcileChannel(row)));
    const sameReconcileKey = (left, right) => Boolean(normalizeReconcileKey(left)) && normalizeReconcileKey(left) === normalizeReconcileKey(right);
    const isSameOnlineDiff = (diff, row) => {
      const rowFlowType = String(row.flowType || row.bizType || '').toUpperCase();
      const diffBizType = String(firstOf(diff, ['bizType', 'businessType', 'flowType', 'billType'], '')).toUpperCase();
      if (rowFlowType === 'PAY') {
        if (diffBizType && diffBizType !== 'PAY') return false;
        return [diff.payOrderNo, diff.payNo, diff.paymentNo, diff.tradeNo, diff.channelTradeNo]
          .some((value) => sameReconcileKey(value, row.payOrderNo) || sameReconcileKey(value, row.payNo));
      }
      if (rowFlowType === 'REFUND') {
        if (diffBizType && diffBizType !== 'REFUND') return false;
        return [diff.refundNo, diff.refundOrderNo, diff.aftersaleNo, diff.channelRefundNo]
          .some((value) => sameReconcileKey(value, row.refundNo));
      }
      return false;
    };
    const completedReconcileRows = archivedTasks.flatMap((task) => collectTaskRecords(task).map((record) => ({ ...record, __task: task })));
    const pendingTasks = recordsOf(pendingTasksPayload).filter((task) => !isCompletedReconcileRecord(task));
    const onlineDiffPayloads = await Promise.all(pendingTasks.slice(0, 20).map((task) => fetchAdminOnlineDiffItems(task.id, { page: 1, size: 200 })));
    const onlineDiffRows = onlineDiffPayloads.flatMap((response) => recordsOf(normalizeResponse(response, {})));
    const completedOnlineDiffs = onlineDiffRows.filter(isCompletedReconcileRecord);
    completedReconcileRows.push(...completedOnlineDiffs.map((record) => ({ ...record, __task: record })));
    const onlineDiffs = onlineDiffRows.filter((diff) => !isCompletedReconcileRecord(diff));
    const hangingRows = recordsOf(hangingPayload).filter((hanging) => !isCompletedReconcileRecord(hanging));
    const findCompletedReconcileRecord = (row) => completedReconcileRows.find((record) => isSameOnlineDiff(record, row));
    const findPendingOnlineDiff = (row) => onlineDiffs.find((diff) => isSameOnlineDiff(diff, row));
    const findHangingRecord = (row) => hangingRows.find((hanging) => isSameOnlineDiff(hanging, row));
    const isHangingFlow = (row) => Boolean(findHangingRecord(row));
    const hasPendingReconcileSignal = (row) => Boolean(findPendingOnlineDiff(row) || isHangingFlow(row) || row.processStatus === 'PENDING' || row.processStatus === 'HANGING');
    const attachReconcileRecord = (row) => {
      const existingRemark = firstOf(row, ['diffRemark', 'reconcileRemark', 'reconciliationRemark', 'remark', 'processRemark', 'handleRemark', 'suggestedAction', 'diffType', 'reason', 'failReason', 'exceptionReason'], '');
      const pendingOnlineDiff = findPendingOnlineDiff(row);
      const completedRecord = findCompletedReconcileRecord(row);
      const archivedTask = findByFlowKeys(archivedTaskByKey, row) || findArchivedTaskByDateChannel(row) || completedRecord?.__task || completedRecord;
      if (pendingOnlineDiff) {
        return {
          ...row,
          onlineDiff: pendingOnlineDiff,
          reconcileStatus: 'DIFF',
          diffRemark: existingRemark || firstOf(pendingOnlineDiff, ['processRemark', 'handleRemark', 'suggestedAction', 'diffType', 'remark'], '线上对账差异'),
        };
      }
      const hangingRecord = findHangingRecord(row);
      if (hangingRecord) {
        return {
          ...row,
          hangingFollow: hangingRecord,
          reconcileStatus: 'DIFF',
          diffRemark: existingRemark || firstOf(hangingRecord, ['processRemark', 'handleRemark', 'suggestedAction', 'diffType', 'remark', 'reason', 'failReason'], '挂账中，需持续跟进闭环'),
        };
      }
      if (hasPendingReconcileSignal(row)) {
        return { ...row, reconcileStatus: 'DIFF', diffRemark: existingRemark || '待处理差异，需人工核验' };
      }
      if (archivedTask || completedRecord) {
        return {
          ...row,
          archivedTask,
          completedRecord,
          reconcileStatus: 'MATCHED',
          diffRemark: existingRemark || firstOf(archivedTask || completedRecord, ['processRemark', 'handleRemark', 'suggestedAction', 'diffType', 'remark', 'taskNo', 'reconcileDate', 'billDate', 'archiveTime', 'completedAt', 'completeTime'], '已完成对账'),
        };
      }
      return { ...row, reconcileStatus: 'PENDING', diffRemark: existingRemark };
    };
    const pays = Array.from(new Map(recordsOf(paysPayload).filter(isDisplayPay).map((item) => [firstOf(item, ['payOrderNo', 'payNo', 'id', 'orderNo']), item])).values()).map(normalizePay).map((row) => ({ ...row, flowType: 'PAY', bizType: 'PAY' })).map(attachReconcileRecord);
    const refunds = recordsOf(refundsPayload).filter(isDisplayRefund).map(normalizeRefund).map((row) => ({ ...row, flowType: 'REFUND', bizType: 'REFUND' })).map(attachReconcileRecord);
    const diffs = onlineDiffs;
    const callbacks = recordsOf(callbacksPayload);
    const monthPays = recordsOf(monthPaysPayload).filter(isFinanceSuccessPay).map(normalizePay).map((row) => ({ ...row, flowType: 'PAY', bizType: 'PAY' }));
    const monthRefunds = recordsOf(monthRefundsPayload).filter(isDisplayRefund).map(normalizeRefund).map((row) => ({ ...row, flowType: 'REFUND', bizType: 'REFUND' }));
    const pendingDiffTaskCount = toNumber(
      pendingTasksPayload.pendingDiffTaskCount
      || pendingTasksPayload.pendingTaskCount
      || pendingTasksPayload.totalPendingTaskCount
      || pendingTasks.filter((task) => Number(task.pendingCount || task.pendingDiffCount || 0) > 0).length,
    );
    const pendingDiffCount = toNumber(
      reconcile.pendingDiffCount
      || reconcile.pendingCount
      || pendingTasks.reduce((sum, task) => sum + toNumber(task.pendingCount || task.pendingDiffCount), 0)
      || diffs.length,
    );
    const archivedTaskCount = toNumber(
      completedTasksPayload.completedTaskCount
      || completedTasksPayload.archivedTaskCount
      || completedTasksPayload.totalCompletedTaskCount
      || totalOf(completedTasksPayload)
      || reconcile.completedTasks
      || reconcile.archivedTaskCount,
    );
    const fallbackPaidAmountCent = sumBy(monthPays, ['payAmountCent', 'payAmount', 'amountCent', 'totalAmountCent']);
    const fallbackRefundAmountCent = sumBy(monthRefunds, ['refundAmountCent', 'amountCent']);
    const paidAmountCent = toNumber(firstOf(cumulativeNetIncome, ['monthPaidAmountCent'], fallbackPaidAmountCent));
    const refundAmountCent = toNumber(firstOf(cumulativeNetIncome, ['monthRefundAmountCent'], fallbackRefundAmountCent));
    const netIncomeCent = toNumber(firstOf(cumulativeNetIncome, ['monthNetIncomeCent'], paidAmountCent - refundAmountCent));
    const summary = {
      financeRange: monthParams,
      paidAmountCent,
      refundAmountCent,
      netIncomeCent,
      cumulativeNetIncomeCent: toNumber(firstOf(cumulativeNetIncome, ['cumulativeNetIncomeCent', 'totalNetIncomeCent', 'netIncomeCent'], 0)),
      payOrderCount: monthPays.length || totalOf(monthPaysPayload),
      refundCount: totalOf(refundedPaysPayload),
      refundOrderCount: monthRefunds.length || totalOf(monthRefundsPayload),
      taskCount: toNumber(reconcile.taskCount || reconcile.totalCount || totalOf(pendingTasksPayload)),
      pendingReconcileCount: pendingDiffCount,
      pendingDiffTaskCount,
      hangingCount: totalOf(hangingPayload),
      archivedTaskCount,
      archivedCount: archivedTaskCount,
      abnormalReconcileCount: pendingDiffTaskCount,
      doneReconcileCount: toNumber(reconcile.doneCount),
      callbackFailedCount: callbacks.filter((item) => ['FAILED', 'ERROR'].includes(item.status || item.handleStatus)).length,
      successRate: toNumber(reconcile.successRate),
    };
    const risks = [
      buildRisk('待处理差异任务', summary.pendingDiffTaskCount, '存在待处理差异的对账任务需要进入任务差异明细处理', summary.pendingDiffTaskCount > 0 ? 'HIGH' : 'NORMAL'),
      buildRisk('挂账未闭环', summary.hangingCount, '已挂账差异需要持续跟进到归档', summary.hangingCount > 0 ? 'WARNING' : 'NORMAL'),
      buildRisk('支付回调失败', summary.callbackFailedCount, '回调失败可能造成资金状态不同步', summary.callbackFailedCount > 0 ? 'HIGH' : 'NORMAL'),
      buildRisk('已退款支付单', summary.refundCount, '关注支付单已退款状态和异常退款单', summary.refundCount > 0 ? 'LOW' : 'NORMAL'),
    ];
    return { overview: { ...overview, ...summary }, reconcile, summary, financeTrend, risks, todos: risks, pays, refunds, monthPays, monthRefunds, diffs, callbacks, pendingTasks };
  }, options);

  const loadWarehouse = (options = {}) => safeLoad('WAREHOUSE', async () => {
    const activeStockDate = dateFromTimestamp(options.timestamp ?? Date.now());
    const [overviewRes, stocksRes, warningsRes, logsRes, reconciliationsRes, warehouseTrendRes] = await Promise.all([
      fetchDashboard(params.value),
      fetchAdminTodayActiveStocks({ stockDate: activeStockDate, currentTimestamp: options.timestamp ?? Date.now(), page: 1, size: 100, sortBy: 'latestStockTime', sortOrder: 'desc' }),
      fetchWarningStocks({ ...params.value, page: 1, size: 8, sortField: 'availableStock', sortOrder: 'asc' }),
      fetchStockLogs({ ...params.value, page: 1, size: 8, sortField: 'createTime', sortOrder: 'desc' }),
      fetchAdminStockReconciliations({ ...params.value, status: 'INCONSISTENT', page: 1, size: 8, sortField: 'createTime', sortOrder: 'desc' }),
      fetchAdminWarehouseTrend().catch(() => ({ data: { data: [] } })),
    ]);
    const overview = normalizeResponse(overviewRes, {});
    const stocksPayload = normalizeResponse(stocksRes, {});
    const warningsPayload = normalizeResponse(warningsRes, {});
    const logsPayload = normalizeResponse(logsRes, {});
    const reconciliationsPayload = normalizeResponse(reconciliationsRes, {});
    const warehouseTrend = normalizeResponse(warehouseTrendRes, []);
    const stocks = recordsOf(stocksPayload);
    const warnings = recordsOf(warningsPayload);
    const logs = recordsOf(logsPayload);
    const reconciliations = recordsOf(reconciliationsPayload);
    const stockStats = overview.stockWarningStats || {};
    const outOfStockCount = warnings.filter((item) => toNumber(item.availableStock) <= 0).length;
    const lockedStockTotal = warnings.reduce((sum, item) => sum + toNumber(item.lockedStock), 0);
    const stockDiffCount = totalOf(reconciliationsPayload);
    const summary = {
      totalSkuCount: toNumber(stockStats.totalCount || totalOf(stocksPayload) || totalOf(warningsPayload)),
      lowStockCount: toNumber(stockStats.lowCount || stocks.filter((item) => item.warningStatus === 'LOW' || toNumber(item.availableStock) <= toNumber(item.lowStockThreshold)).length || warnings.filter((item) => toNumber(item.availableStock) <= toNumber(item.lowStockThreshold)).length),
      highStockCount: toNumber(stockStats.highCount || stocks.filter((item) => item.warningStatus === 'HIGH').length),
      normalCount: toNumber(stockStats.normalCount || stocks.filter((item) => item.warningStatus === 'NORMAL').length),
      warningCount: totalOf(warningsPayload),
      outOfStockCount,
      pendingWarningCount: warnings.filter((item) => !['DONE', 'HANDLED'].includes(item.warningStatus || item.status)).length,
      stockLogCount: totalOf(logsPayload),
      stockDiffCount,
      lockedStockTotal,
    };
    const risks = [
      buildRisk('库存预警', summary.warningCount, '查看全部高低库存预警 SKU 并处理库存风险', summary.warningCount > 0 ? 'WARNING' : 'NORMAL'),
      buildRisk('低库存 SKU', summary.lowStockCount, '低于预警阈值的 SKU 需要补货', summary.lowStockCount > 0 ? 'WARNING' : 'NORMAL'),
      buildRisk('高库存 SKU', summary.highStockCount, '库存积压 SKU 建议促销、清仓或调整采购', summary.highStockCount > 0 ? 'WARNING' : 'NORMAL'),
      buildRisk('库存对账不一致', summary.stockDiffCount, 'Redis 与数据库库存数据存在不一致', summary.stockDiffCount > 0 ? 'HIGH' : 'NORMAL'),
    ];
    return { overview, summary, risks, todos: risks, stocks, stockTotal: totalOf(stocksPayload), warnings, warningTotal: totalOf(warningsPayload), logs, reconciliations, warehouseTrend };
  }, options);

  const loadProducts = (options = {}) => safeLoad('PRODUCTS', async () => {
    const [sessionThresholdRes, thresholdRes] = await Promise.all([
      Promise.resolve(loadProductSalesThresholdSession(adminStore.adminId)),
      fetchAdminProductSalesThresholdConfig(),
    ]);
    const sessionThreshold = sessionThresholdRes || null;
    const threshold = normalizeResponse(thresholdRes, {});
    const resolvedThreshold = sessionThreshold || normalizeProductSalesThresholdConfig(threshold);
    const hotThreshold = normalizeSalesThreshold(resolvedThreshold.hotSalesThreshold, DEFAULT_PRODUCT_SALES_THRESHOLD.hotSalesThreshold);
    const slowThreshold = normalizeSalesThreshold(resolvedThreshold.lowSalesThreshold, DEFAULT_PRODUCT_SALES_THRESHOLD.lowSalesThreshold);
    const commonThresholdParams = { hotSalesThreshold: hotThreshold, lowSalesThreshold: slowThreshold };
    const [overviewRes, productsRes, onlineCountRes, hotProductsRes, slowProductsRes] = await Promise.all([
      fetchDashboard({ ...params.value, ...commonThresholdParams }),
      fetchAdminProductPage({ ...params.value, ...commonThresholdParams, page: 1, size: 12, sortBy: 'monthlySalesCount', sortField: 'monthlySalesCount', sortOrder: 'desc' }),
      fetchAdminProductPage({ status: 'ONLINE', ...commonThresholdParams, page: 1, size: 1 }),
      fetchAdminProductPage({ ...commonThresholdParams, salesBand: 'HOT', page: 1, size: 1, sortBy: 'monthlySalesCount', sortField: 'monthlySalesCount', sortOrder: 'desc' }),
      fetchAdminProductPage({ ...commonThresholdParams, salesBand: 'LOW', page: 1, size: 1, sortBy: 'monthlySalesCount', sortField: 'monthlySalesCount', sortOrder: 'asc' }),
    ]);
    const overview = normalizeResponse(overviewRes, {});
    const overviewProductStats = overview.productStats || {};
    const productsPayload = normalizeResponse(productsRes, {});
    const onlineCountPayload = normalizeResponse(onlineCountRes, {});
    const hotProductsPayload = normalizeResponse(hotProductsRes, {});
    const slowProductsPayload = normalizeResponse(slowProductsRes, {});
    const products = recordsOf(productsPayload).map(normalizeProduct);
    const productTotal = toNumber(overviewProductStats.totalCount || totalOf(productsPayload));
    const onSaleCount = toNumber(overviewProductStats.onlineCount || totalOf(onlineCountPayload));
    const hotCount = toNumber(overviewProductStats.hotSellingCount || totalOf(hotProductsPayload));
    const slowCount = toNumber(overviewProductStats.lowSellingCount || totalOf(slowProductsPayload));
    const lowStockHotCount = toNumber(overviewProductStats.hotLowStockCount || 0);
    const highStockSlowCount = toNumber(overviewProductStats.lowHighStockCount || 0);
    const summary = {
      productTotal,
      onSaleCount,
      offSaleCount: toNumber(overview.offSaleProductCount || Math.max(productTotal - onSaleCount, 0)),
      totalSalesCount: sumBy(products, ['salesCount']),
      totalSalesAmountCent: sumBy(products, ['salesAmountCent', 'salesAmount']),
      hotThreshold,
      slowThreshold,
      hotCount,
      slowCount,
      lowStockHotCount,
      highStockSlowCount,
    };
    const risks = [
      buildRisk('热销低库存商品', summary.lowStockHotCount, '热销商品库存不足，建议补货', summary.lowStockHotCount > 0 ? 'HIGH' : 'NORMAL'),
      buildRisk('低销高库存商品', summary.highStockSlowCount, '低销商品库存积压，建议促销或清仓', summary.highStockSlowCount > 0 ? 'WARNING' : 'NORMAL'),
      buildRisk('待上架商品', summary.offSaleCount, '下架商品可评估补充信息后上架', summary.offSaleCount > 0 ? 'LOW' : 'NORMAL'),
      buildRisk('滞销商品', summary.slowCount, '低销量商品建议优化图文、价格或下架', summary.slowCount > 0 ? 'LOW' : 'NORMAL'),
      buildRisk('热销商品', summary.hotCount, '保持热销商品库存供给稳定', 'NORMAL'),
    ];
    return { overview, products, productTotal, threshold, summary, risks, todos: risks };
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
