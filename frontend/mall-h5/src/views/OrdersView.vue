<template>
  <div class="page">
    <van-nav-bar title="我的订单" />

    <div class="orders-panel">
      <div class="toolbar-card">
        <van-search
          v-model="keyword"
          shape="round"
          placeholder="搜索订单号或商品名称"
          @update:model-value="handleFilterChange"
          @search="handleFilterChange"
          @clear="handleFilterChange"
        />
        <div class="status-tabs">
          <div
            v-for="tab in statusTabs"
            :key="tab.value"
            class="status-tab"
            :class="{ 'status-tab--active': activeStatus === tab.value }"
            @click="handleStatusChange(tab.value)"
          >
            {{ tab.label }}
          </div>
        </div>
        <div class="history-filter-tip">
          默认隐藏 10 天前已完成的订单；待处理、退款、取消等状态订单仍会保留展示。
        </div>
      </div>

      <div class="order-list">
        <van-empty v-if="!orders.length && !ordersLoading" description="暂无订单" />
        <van-list
          v-else
          v-model:loading="ordersLoading"
          :finished="ordersFinished"
          finished-text="没有更多订单了"
          loading-text="订单加载中..."
          @load="loadMoreOrders"
        >
        <div v-for="item in orders" :key="item.id" class="order-card" @click="goDetail(item.id)">
        <div class="order-top">
          <div class="order-top-label">订单编号</div>
          <div class="order-top-no">{{ item.orderNo }}</div>
          <div class="status-pill" :class="`status-pill--${statusPillClass(displayStatus(item))}`">
            {{ formatOrderListStatus(item) }}
          </div>
          <div v-if="isAftersaleRejected(item)" class="rejected-watermark">售后已驳回</div>
        </div>

        <div class="order-body">
          <img class="order-image" :src="getOrderVisual(item)" :alt="item.firstSkuName || item.orderNo" @error="handleOrderImageError($event, item)" />
          <div class="order-main">
            <div v-if="item.firstSkuName" class="product-name van-multi-ellipsis--l2">{{ item.firstSkuName }}</div>
            <div class="order-info">商品数量：{{ item.itemCount }} 件</div>
            <div class="order-info">支付方式：{{ item.payType || '--' }}</div>
            <div v-if="item.status === 'PENDING_PAYMENT'" class="countdown-text">
              支付剩余时间：{{ formatCountdown(item.remainingPaySeconds) }}
            </div>
            <div v-if="item.status === 'PENDING_PAYMENT'" class="countdown-tip">
              超时支付：{{ item.timeoutMinutes }} 分钟内有效
            </div>
            <div v-if="item.completedAt" class="order-info">完成时间：{{ formatDateTime(item.completedAt) }}</div>
            <div class="card-foot">
              <span>应付金额</span>
              <span class="price">¥{{ formatPrice(item.payAmount) }}</span>
            </div>
            <div v-if="showActions(item.status)" class="action-row">
              <van-button
                v-if="item.status === 'PENDING_PAYMENT'"
                plain
                size="small"
                round
                :loading="cancellingOrderId === item.id"
                @click.stop="handleCancelOrder(item)"
              >
                取消订单
              </van-button>
              <van-button
                v-if="item.status === 'SHIPPED'"
                type="primary"
                size="small"
                round
                :loading="confirmingOrderId === item.id"
                @click.stop="handleConfirmReceipt(item)"
              >
                确认收货
              </van-button>
              <van-button
                v-if="showRefundButton(item.status)"
                type="warning"
                plain
                size="small"
                round
                :loading="refundingOrderId === item.id"
                :disabled="isAftersaleRejected(item)"
                @click.stop="handleRefundApply(item)"
              >
                {{ isAftersaleRejected(item) ? '已驳回' : '申请退款' }}
              </van-button>
              <van-button
                v-if="canDeleteOrder(item.status)"
                type="danger"
                plain
                size="small"
                round
                :loading="deletingOrderId === item.id"
                @click.stop="handleDeleteOrder(item)"
              >
                删除订单
              </van-button>
            </div>
          </div>
        </div>
      </div>
        </van-list>
    </div>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { showConfirmDialog, showFailToast, showSuccessToast } from 'vant';
import { applyAftersaleRefund, cancelOrder, confirmReceipt, deleteUserOrder, fetchAftersales, fetchOrders, repairPaidOrder, syncPayOrderStatus } from '../api';
import { formatCountdown, formatDateTime, formatPrice, formatStatus } from '../utils/format';
import { getProductImage } from '../utils/productVisual';
import { useUserStore } from '../stores/user';
import { requireLogin, redirectLoginWithNotice } from '../utils/requireLogin';

const ORDER_CACHE_KEY = 'mallfei:h5-orders-cache-v1';
const ORDER_CACHE_TTL = 60 * 1000;
const ORDER_SUMMARY_CACHE_KEY = 'mallfei:h5-orders-summary-cache-v1';
const ORDER_SUMMARY_CACHE_TTL = 3 * 60 * 1000;
const HOME_PRODUCTS_CACHE_KEY = 'mallfei:h5-home-products-cache-v1';
const SALES_REFRESH_KEY = 'mallfei:product-sales-refresh';
const ORDER_PAGE_SIZE = 10;

const router = useRouter();
const userStore = useUserStore();
const orders = ref([]);
const aftersaleStatusByOrderNo = ref({});
const orderPageNum = ref(1);
const orderTotal = ref(0);
const ordersLoading = ref(false);
const ordersFinished = ref(false);
let sessionProbeTimer = null;
const keyword = ref('');
const activeStatus = ref('ALL');
const confirmingOrderId = ref(null);
const refundingOrderId = ref(null);
const cancellingOrderId = ref(null);
const deletingOrderId = ref(null);
let timer = null;
let countdownBaseTime = Date.now();
let refreshingAfterCountdown = false;
let payStatusPoller = null;
let payStatusPollAttempts = 0;

const statusTabs = [
  { label: '全部', value: 'ALL' },
  { label: '待支付', value: 'PENDING_PAYMENT_GROUP' },
  { label: '已支付', value: 'PAID_GROUP' },
  { label: '处理中', value: 'PROCESSING_GROUP' },
  { label: '已发货', value: 'SHIPPED_GROUP' },
  { label: '已完成', value: 'COMPLETED_GROUP' },
  { label: '已退款', value: 'REFUNDED_GROUP' },
  { label: '已取消', value: 'CANCELLED_GROUP' },
];

const resetOrderPagination = () => {
  orderPageNum.value = 1;
  orderTotal.value = 0;
  ordersFinished.value = false;
};

const getOrderVisualProduct = (item) => ({
  id: item.firstSkuId || item.id,
  name: item.firstSkuName || `云仓 订单商品 ${item.itemCount || 1}件`,
  skuName: item.firstSkuName || item.orderNo,
  skuCode: item.orderNo,
  skuImageUrl: item.firstSkuImageUrl,
  categoryId: 10,
});

const getOrderVisual = (item) => getProductImage(getOrderVisualProduct(item));

const handleOrderImageError = (event, item) => {
  event.target.src = getProductImage({ ...getOrderVisualProduct(item), skuImageUrl: '' });
};

const terminalStatuses = ['COMPLETED', 'REFUNDED', 'CANCELLED', 'TIMEOUT_CANCELLED', 'CLOSED', 'REFUND_CLOSED'];
const aftersaleStatusOf = (item) => aftersaleStatusByOrderNo.value[item?.orderNo] || '';
const isTerminalOrder = (item) => terminalStatuses.includes(item?.status);
const isAftersaleRejected = (item) => !isTerminalOrder(item) && aftersaleStatusOf(item) === 'REJECTED';

const refundTerminalStatuses = ['REFUND_SUCCESS', 'REFUND_FAILED', 'REFUND_CLOSED'];

const displayStatus = (item) => {
  if (refundTerminalStatuses.includes(item?.latestRefundStatus)) {
    return item.latestRefundStatus;
  }
  return item?.status;
};

const displayStatusKind = (item) => (refundTerminalStatuses.includes(item?.latestRefundStatus) ? 'refund' : 'order');
const formatOrderListStatus = (item) => formatStatus(displayStatus(item), displayStatusKind(item));

const statusPillClass = (status) => {
  if (status === 'PENDING_PAYMENT') {
    return 'warning';
  }
  if (status === 'PAID') {
    return 'paid';
  }
  if (status === 'PROCESSING' || status === 'PAYMENT_EXCEPTION') {
    return 'processing';
  }
  if (status === 'SHIPPED') {
    return 'shipped';
  }
  if (status === 'COMPLETED') {
    return 'success';
  }
  if (status === 'REFUND_PENDING' || status === 'REFUNDING' || status === 'REFUND_PROCESSING' || status === 'EFUND_PROCESSING' || status === 'PENDING_REVIEW') {
    return 'warning';
  }
  if (status === 'REFUNDED' || status === 'REFUND_SUCCESS') {
    return 'refund';
  }
  if (status === 'REFUND_FAILED') {
    return 'danger';
  }
  if (status === 'CANCELLED' || status === 'TIMEOUT_CANCELLED' || status === 'CLOSED' || status === 'REFUND_CLOSED') {
    return 'danger';
  }
  return 'primary';
};

const showRefundButton = (status) => ['PAID', 'PROCESSING', 'SHIPPED'].includes(status);
const canDeleteOrder = (status) => terminalStatuses.includes(status);

const normalizeListData = (payload) => {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.records)) return payload.records;
  if (Array.isArray(payload?.list)) return payload.list;
  if (Array.isArray(payload?.rows)) return payload.rows;
  return [];
};

const normalizeOrderRecord = (item = {}) => ({
  ...item,
  itemCount: Number(item.itemCount ?? item.totalCount ?? item.quantity ?? 0),
  payAmount: item.payAmount ?? item.totalAmount ?? item.amount ?? 0,
  firstSkuName: item.firstSkuName || item.productName || item.skuName || '',
  firstSkuImageUrl: item.firstSkuImageUrl || item.skuImageUrl || item.productImage || item.mainImage || '',
});

const normalizeOrderRecords = (payload) => normalizeListData(payload).map(normalizeOrderRecord);

const refreshAftersaleMarkers = async () => {
  try {
    const { data } = await fetchAftersales();
    const markers = {};
    normalizeOrderRecords(data?.data).forEach((item) => {
      if (item?.orderNo && item?.status) {
        markers[item.orderNo] = item.status;
      }
    });
    aftersaleStatusByOrderNo.value = markers;
  } catch {
    aftersaleStatusByOrderNo.value = {};
  }
};
const showActions = (status) => status === 'PENDING_PAYMENT' || status === 'PAID' || status === 'SHIPPED' || showRefundButton(status) || canDeleteOrder(status);

const stopPayStatusPolling = () => {
  if (payStatusPoller) {
    clearInterval(payStatusPoller);
    payStatusPoller = null;
  }
  payStatusPollAttempts = 0;
};

const syncRecentPaidOrders = async ({ allowRepair = false } = {}) => {
  const recentOrderNo = localStorage.getItem('mallfei:last-pay-order-order-no') || '';
  const recentPayChannel = localStorage.getItem('mallfei:last-pay-channel') || '';
  if (!recentOrderNo || (recentPayChannel !== 'ALIPAY_WAP' && recentPayChannel !== 'ALIPAY_PC')) return false;
  const targetOrder = orders.value.find((item) => item.orderNo === recentOrderNo);
  if (!targetOrder || targetOrder.status !== 'PENDING_PAYMENT') return false;
  try {
    await syncPayOrderStatus(recentOrderNo);
    await loadOrders(false);
    const updatedOrder = orders.value.find((item) => item.orderNo === recentOrderNo);
    if (updatedOrder && updatedOrder.status !== 'PENDING_PAYMENT') {
      return true;
    }
    if (allowRepair) {
      await repairPaidOrder(recentOrderNo);
      await loadOrders(false);
      const repairedOrder = orders.value.find((item) => item.orderNo === recentOrderNo);
      return Boolean(repairedOrder && repairedOrder.status !== 'PENDING_PAYMENT');
    }
    return false;
  } catch {
    return false;
  }
};

const startRecentPaidOrderPolling = () => {
  stopPayStatusPolling();
  payStatusPoller = setInterval(async () => {
    payStatusPollAttempts += 1;
    const synced = await syncRecentPaidOrders({ allowRepair: true });
    if (synced || payStatusPollAttempts >= 12) {
      stopPayStatusPolling();
    }
  }, 5000);
};

const refreshOrdersWhenCountdownEnds = async () => {
  if (refreshingAfterCountdown) {
    return;
  }
  refreshingAfterCountdown = true;
  try {
    await loadOrders(false);
  } finally {
    refreshingAfterCountdown = false;
  }
};

const tickCountdown = () => {
  const elapsedSeconds = Math.max(1, Math.floor((Date.now() - countdownBaseTime) / 1000));
  countdownBaseTime = Date.now();
  let shouldRefresh = false;
  orders.value = orders.value.map((item) => {
    if (item.status !== 'PENDING_PAYMENT') {
      return item;
    }
    const currentSeconds = Number(item.remainingPaySeconds || 0);
    const nextSeconds = Math.max(0, currentSeconds - elapsedSeconds);
    if (nextSeconds === 0 && currentSeconds > 0) {
      shouldRefresh = true;
    }
    return {
      ...item,
      remainingPaySeconds: nextSeconds,
    };
  });
  if (shouldRefresh) {
    refreshOrdersWhenCountdownEnds();
  }
};

const handleVisibilityChange = () => {
  if (!document.hidden) {
    countdownBaseTime = Date.now();
  }
};

const startTimer = () => {
  if (timer) {
    clearInterval(timer);
  }
  timer = setInterval(tickCountdown, 1000);
};

const restoreOrdersCache = () => {
  try {
    const raw = localStorage.getItem(ORDER_CACHE_KEY);
    if (!raw) return false;
    const parsed = JSON.parse(raw);
    if (!parsed?.updatedAt || !Array.isArray(parsed?.orders)) return false;
    if (Date.now() - Number(parsed.updatedAt) > ORDER_CACHE_TTL) return false;
    orders.value = parsed.orders;
    orderTotal.value = Number(parsed.total || orders.value.length);
    orderPageNum.value = Number(parsed.pageNum || Math.floor(orders.value.length / ORDER_PAGE_SIZE) + 1);
    ordersFinished.value = Boolean(parsed.finished || (orderTotal.value > 0 && orders.value.length >= orderTotal.value));
    countdownBaseTime = Date.now();
    return true;
  } catch {
    return false;
  }
};

const restoreOrdersSummaryCache = () => {
  try {
    const raw = localStorage.getItem(ORDER_SUMMARY_CACHE_KEY);
    if (!raw) return false;
    const parsed = JSON.parse(raw);
    if (!parsed?.updatedAt || !Array.isArray(parsed?.orders)) return false;
    if (Date.now() - Number(parsed.updatedAt) > ORDER_SUMMARY_CACHE_TTL) return false;
    orders.value = parsed.orders;
    orderTotal.value = Number(parsed.total || orders.value.length);
    orderPageNum.value = Number(parsed.pageNum || Math.floor(orders.value.length / ORDER_PAGE_SIZE) + 1);
    ordersFinished.value = Boolean(parsed.finished || (orderTotal.value > 0 && orders.value.length >= orderTotal.value));
    countdownBaseTime = Date.now();
    return true;
  } catch {
    return false;
  }
};

const persistOrdersCache = () => {
  try {
    const payload = {
      updatedAt: Date.now(),
      orders: orders.value,
      total: orderTotal.value,
      pageNum: orderPageNum.value,
      finished: ordersFinished.value,
    };
    localStorage.setItem(ORDER_CACHE_KEY, JSON.stringify(payload));
  } catch {
  }
};

const loadOrders = async (showError = true, { reset = true } = {}) => {
  if (reset) {
    resetOrderPagination();
  }
  if (ordersFinished.value && !reset) {
    ordersLoading.value = false;
    return;
  }
  try {
    const valid = await requireLogin(router, '/orders', { force: false });
    if (!valid) {
      return;
    }
    ordersLoading.value = true;
    const pageToLoad = orderPageNum.value;
    const { data } = await fetchOrders({
      page: pageToLoad,
      size: ORDER_PAGE_SIZE,
      status: activeStatus.value === 'ALL' ? undefined : activeStatus.value,
      keyword: keyword.value.trim() || undefined,
    });
    const pageData = data.data || {};
    const nextRecords = normalizeOrderRecords(pageData);
    countdownBaseTime = Date.now();
    orders.value = reset ? nextRecords : [...orders.value, ...nextRecords];
    orderTotal.value = Number(pageData.total || orders.value.length);
    orderPageNum.value = pageToLoad + 1;
    ordersFinished.value = nextRecords.length < ORDER_PAGE_SIZE || orders.value.length >= orderTotal.value;
    persistOrdersCache();
    await refreshAftersaleMarkers();
    if (orders.value.some((item) => item.status === 'PENDING_PAYMENT' && Number(item.remainingPaySeconds || 0) === 0)) {
      setTimeout(() => {
        refreshOrdersWhenCountdownEnds();
      }, 300);
    }
  } catch (error) {
    if (showError) {
      showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '订单列表加载失败');
    }
  } finally {
    ordersLoading.value = false;
  }
};

const loadMoreOrders = async () => {
  await loadOrders(true, { reset: false });
};

const markProductSalesStale = (order = null) => {
  try {
    localStorage.removeItem(HOME_PRODUCTS_CACHE_KEY);
    localStorage.setItem(SALES_REFRESH_KEY, String(Date.now()));
    const firstProductId = order?.firstSpuId || order?.firstProductId || order?.productId;
    if (firstProductId) {
      localStorage.setItem('mallfei:last-visited-product-id', String(firstProductId));
    }
  } catch {
  }
};

const prefetchOrderSummary = async () => {
  try {
    const { data } = await fetchOrders({ page: 1, size: ORDER_PAGE_SIZE });
    const pageData = data.data || {};
    const summaryOrders = normalizeOrderRecords(pageData);
    localStorage.setItem(ORDER_SUMMARY_CACHE_KEY, JSON.stringify({
      updatedAt: Date.now(),
      orders: summaryOrders,
      total: Number(pageData.total || summaryOrders.length),
      pageNum: 2,
      finished: summaryOrders.length < ORDER_PAGE_SIZE || summaryOrders.length >= Number(pageData.total || summaryOrders.length),
    }));
  } catch {
  }
};

const handleFilterChange = () => {
  loadOrders(false);
};

const handleStatusChange = (status) => {
  activeStatus.value = status;
  loadOrders(false);
};

const handleCancelOrder = async (order) => {
  try {
    await showConfirmDialog({
      title: '取消订单',
      message: `确认取消订单 ${order.orderNo} 吗？取消后订单状态将变为“已取消”。`,
      confirmButtonText: '确认取消',
      cancelButtonText: '再想想',
    });
    cancellingOrderId.value = order.id;
    await cancelOrder(order.id);
    await loadOrders();
    showSuccessToast('订单已取消');
  } catch (error) {
    if (error !== 'cancel') {
      showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '取消订单失败');
    }
  } finally {
    cancellingOrderId.value = null;
  }
};

const handleConfirmReceipt = async (order) => {
  if (order?.status !== 'SHIPPED') {
    showFailToast('仅已发货订单可以确认收货');
    return;
  }
  try {
    await showConfirmDialog({
      title: '确认收货',
      message: '确认收货后订单将变为已完成，且该操作不可撤销，确认继续吗？',
      confirmButtonText: '确认收货',
      cancelButtonText: '再想想',
    });
    confirmingOrderId.value = order.id;
    await confirmReceipt(order.id);
    markProductSalesStale(order);
    await loadOrders();
    showSuccessToast('确认收货成功');
  } catch (error) {
    if (error !== 'cancel') {
      showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '确认收货失败');
    }
  } finally {
    confirmingOrderId.value = null;
  }
};

const handleRefundApply = async (order) => {
  try {
    await showConfirmDialog({
      title: '申请退款',
      message: `确认对订单 ${order.orderNo} 发起售后退款申请吗？提交后将等待商家审核。`,
      confirmButtonText: '提交申请',
      cancelButtonText: '再想想',
    });
    refundingOrderId.value = order.id;
    await applyAftersaleRefund({
      orderId: order.id,
      reason: '用户在订单列表发起退款',
    });
    await loadOrders();
    showSuccessToast('售后申请已提交，等待商家审核');
  } catch (error) {
    if (error !== 'cancel') {
      showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '申请退款失败');
    }
  } finally {
    refundingOrderId.value = null;
  }
};

const handleDeleteOrder = async (order) => {
  try {
    await showConfirmDialog({
      title: '删除订单',
      message: '删除后该订单将不再出现在你的订单列表中，但不会影响商家后台对账记录，确认删除吗？',
      confirmButtonText: '确认删除',
      cancelButtonText: '再想想',
    });
    deletingOrderId.value = order.id;
    await deleteUserOrder(order.id);
    await loadOrders();
    showSuccessToast('订单已删除');
  } catch (error) {
    if (error !== 'cancel') {
      showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '删除订单失败');
    }
  } finally {
    deletingOrderId.value = null;
  }
};

const goDetail = (id) => {
  router.push(`/orders/${id}`);
};

const startSessionProbe = () => {
  if (sessionProbeTimer) return;
  sessionProbeTimer = window.setInterval(async () => {
    const blacklisted = await userStore.checkBlacklist(false);
    if (blacklisted) {
      redirectLoginWithNotice(router, '该用户已禁用，详情可咨询平台/客服', 2200);
      return;
    }
    const valid = await userStore.ensureValidSession(false);
    if (!valid) {
      redirectLoginWithNotice(router, '登录已失效，请重新登录', 1200);
    }
  }, 45000);
};

const stopSessionProbe = () => {
  if (!sessionProbeTimer) return;
  clearInterval(sessionProbeTimer);
  sessionProbeTimer = null;
};

onMounted(async () => {
  document.removeEventListener('visibilitychange', handleVisibilityChange);
  document.addEventListener('visibilitychange', handleVisibilityChange);
  restoreOrdersSummaryCache();
  restoreOrdersCache();
  startTimer();

  loadOrders(false).then(async () => {
    const synced = await syncRecentPaidOrders({ allowRepair: true });
    if (!synced) {
      startRecentPaidOrderPolling();
    }
  });
});

onBeforeUnmount(() => {
  document.removeEventListener('visibilitychange', handleVisibilityChange);
  if (timer) {
    clearInterval(timer);
  }
  stopPayStatusPolling();
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 12px 12px 86px;
  background:
    radial-gradient(circle at top left, rgba(129, 140, 248, 0.2), transparent 28%),
    radial-gradient(circle at top right, rgba(236, 72, 153, 0.11), transparent 24%),
    linear-gradient(180deg, #edf3ff 0%, #f7f9ff 100%);
}

.orders-panel {
  margin-top: 18px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.88);
  border-radius: 28px;
  box-shadow: 0 14px 36px rgba(108, 123, 225, 0.1);
  backdrop-filter: blur(18px);
}

.toolbar-card {
  margin-bottom: 14px;
  padding: 0;
  background: transparent;
}

.status-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding: 8px 2px 2px;
}

.status-tab {
  flex-shrink: 0;
  padding: 6px 12px;
  border-radius: 999px;
  background: #f8fafc;
  color: #64748b;
  font-size: 13px;
  font-weight: 600;
  border: 1px solid #e2e8f0;
}

.status-tab--active {
  color: #1677ff;
  background: #eff6ff;
  border-color: #bfdbfe;
}

.history-filter-tip {
  margin: 10px 2px 0;
  padding: 9px 11px;
  border-radius: 14px;
  background: rgba(99, 102, 241, 0.08);
  color: #64748b;
  font-size: 12px;
  line-height: 1.45;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card {
  position: relative;
  overflow: hidden;
  padding: 14px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(226, 232, 240, 0.7);
  border-radius: 22px;
  box-shadow: 0 10px 24px rgba(108, 123, 225, 0.08);
}

.order-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  min-width: 0;
}

.order-top-label {
  flex-shrink: 0;
  font-size: 12px;
  color: #94a3b8;
}

.order-top-no {
  flex: 1;
  min-width: 0;
  font-size: 12px;
  line-height: 1.35;
  font-weight: 700;
  color: #0f172a;
  word-break: break-all;
}

.status-pill {
  flex-shrink: 0;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1;
  border: 1px solid transparent;
}

.status-pill--primary {
  color: #1677ff;
  background: #eff6ff;
  border-color: #bfdbfe;
}

.status-pill--warning {
  color: #d97706;
  background: #fffbeb;
  border-color: #fde68a;
}

.status-pill--paid {
  color: #2563eb;
  background: #eff6ff;
  border-color: #93c5fd;
}

.status-pill--processing {
  color: #7c3aed;
  background: #f5f3ff;
  border-color: #c4b5fd;
}

.status-pill--shipped {
  color: #0891b2;
  background: #ecfeff;
  border-color: #67e8f9;
}

.status-pill--success {
  color: #16a34a;
  background: #f0fdf4;
  border-color: #bbf7d0;
}

.status-pill--refund {
  color: #db2777;
  background: #fdf2f8;
  border-color: #f9a8d4;
}

.status-pill--danger {
  color: #dc2626;
  background: #fef2f2;
  border-color: #fecaca;
}

.rejected-watermark {
  position: absolute;
  top: 8px;
  left: -34px;
  z-index: 2;
  width: 118px;
  padding: 3px 0;
  transform: rotate(-28deg);
  transform-origin: center;
  color: rgba(220, 38, 38, 0.44);
  background: rgba(254, 226, 226, 0.24);
  border-top: 1px solid rgba(248, 113, 113, 0.28);
  border-bottom: 1px solid rgba(248, 113, 113, 0.28);
  font-size: 11px;
  font-weight: 800;
  line-height: 1.15;
  text-align: center;
  letter-spacing: 1px;
  pointer-events: none;
}

.order-body {
  display: flex;
  gap: 12px;
}

.order-image {
  width: 96px;
  height: 96px;
  border-radius: 16px;
  object-fit: cover;
  background: #f8fafc;
  flex-shrink: 0;
}

.order-main {
  flex: 1;
  min-width: 0;
}

.product-name {
  color: #334155;
  font-weight: 600;
}

.order-info {
  margin-top: 10px;
  color: #64748b;
}

.countdown-text {
  margin-top: 10px;
  color: #f59e0b;
  font-weight: 600;
}

.countdown-tip {
  margin-top: 6px;
  color: #94a3b8;
  font-size: 12px;
}

.card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #eef2f7;
}

.action-row {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

.price {
  color: #ee0a24;
  font-weight: 700;
}
</style>
