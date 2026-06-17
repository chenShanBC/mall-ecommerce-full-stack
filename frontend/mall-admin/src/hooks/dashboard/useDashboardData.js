import { computed, reactive, ref } from 'vue';
import * as XLSX from 'xlsx';
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
    return { overview, stats, summary, operationsTrend, risks, todos: overview.todos?.length ? overview.todos : risks, orderRows, orderTotal: totalOf(orders), aftersaleRows, aftersaleTotal: totalOf(aftersales), orderOverviewRows: orderRows.slice(0, 50), orderDetailRows: orderRows.slice(0, 4).map((item, index) => ({ ...item, rowType: index < 2 ? 'ORDER' : 'AFTERSALE' })), };
  }, options);

  const loadFinance = (options = {}) => safeLoad('FINANCE', async () => {
    const monthParams = createCurrentMonthRange();
    const latestFlowEnd = new Date();
    const latestFlowStart = new Date();
    latestFlowStart.setDate(latestFlowEnd.getDate() - 6);
    const latestFlowRange = { startDate: formatDate(latestFlowStart), endDate: formatDate(latestFlowEnd) };
    const latestFlowParams = { ...latestFlowRange, page: 1, size: 100, sortField: 'createTime', sortBy: 'createTime', sortOrder: 'desc' };
    const [overviewRes, cumulativeNetIncomeRes, financeTrendRes, latestPaysRes, refundedPaysRes, refundsRes, reconcileRes, pendingTasksRes, completedTasksRes, archiveReportRes, hangingRes, callbacksRes] = await Promise.all([
      fetchDashboard(monthParams),
      fetchAdminFinanceCumulativeNetIncome(),
      fetchAdminFinanceTrend(),
      fetchAdminPays(latestFlowParams),
      fetchAdminPays({ ...monthParams, status: 'REFUNDED', page: 1, size: 1, sortField: 'createTime', sortOrder: 'desc' }),
      fetchAdminGlobalRefunds(latestFlowParams),
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
    const refundedPaysPayload = normalizeResponse(refundedPaysRes, {});
    const refundsPayload = normalizeResponse(refundsRes, {});
    const pendingTasksPayload = normalizeResponse(pendingTasksRes, {});
    const completedTasksPayload = normalizeResponse(completedTasksRes, {});
    const archiveReportPayload = normalizeResponse(archiveReportRes, {});
    const hangingPayload = normalizeResponse(hangingRes, {});
    const callbacksPayload = normalizeResponse(callbacksRes, {});
    const isDisplayPay = (item) => !['CLOSED', 'CANCELLED'].includes(String(firstOf(item, ['payStatus', 'status', 'tradeStatus'], '')).toUpperCase());
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
    const isHangingFlow = (row) => hangingRows.some((hanging) => isSameOnlineDiff(hanging, row));
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
      if (isHangingFlow(row)) {
        return { ...row, reconcileStatus: 'DIFF', diffRemark: existingRemark || '挂账中，需持续跟进闭环' };
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
    const paidAmountCent = sumBy(pays, ['payAmountCent', 'payAmount', 'amountCent', 'totalAmountCent']);
    const refundAmountCent = sumBy(refunds, ['refundAmountCent', 'amountCent']);
    const summary = {
      financeRange: monthParams,
      paidAmountCent,
      refundAmountCent,
      netIncomeCent: paidAmountCent - refundAmountCent,
      cumulativeNetIncomeCent: toNumber(firstOf(cumulativeNetIncome, ['cumulativeNetIncomeCent', 'totalNetIncomeCent', 'netIncomeCent'], 0)),
      payOrderCount: pays.length || totalOf(paysPayload),
      refundCount: totalOf(refundedPaysPayload),
      refundOrderCount: refunds.length || totalOf(refundsPayload),
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
    return { overview: { ...overview, ...summary }, reconcile, summary, financeTrend, risks, todos: risks, pays, refunds, diffs, callbacks, pendingTasks };
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
