<template>
  <div class="page">
    <van-nav-bar title="订单详情" left-arrow @click-left="router.back()" />

    <div v-if="order" class="content">
      <div class="card">
        <div class="head-row">
          <div>
            <div class="order-no-label">订单编号</div>
            <div class="order-no">{{ order.orderNo }}</div>
            <div class="status-badge" :class="`status-badge--${statusClass(order.status)}`">
              {{ formatStatus(order.status) }}
            </div>
            <div v-if="order.status === 'PENDING_PAYMENT'" class="countdown-text">
              支付剩余时间：{{ formatCountdown(remainingPaySeconds) }}
            </div>
            <div v-if="order.status === 'PENDING_PAYMENT'" class="countdown-tip">
              当前订单超时时间为 {{ order.timeoutMinutes }} 分钟，请尽快完成支付。
            </div>
          </div>
          <div class="price">¥{{ formatPrice(order.payAmount) }}</div>
        </div>
      </div>

      <div class="card">
        <div class="card-title">收货信息</div>
        <div>{{ order.receiverName }} {{ order.receiverPhone }}</div>
        <div class="muted">{{ formatAddress(order) }}</div>
        <div v-if="order.remark" class="muted">备注：{{ order.remark }}</div>
      </div>

      <div class="card">
        <div class="card-title">商品信息</div>
        <div v-for="item in order.items || []" :key="item.id" class="goods-row">
          <img class="goods-image" :src="getProductImage(itemVisual(item))" :alt="item.skuName" />
          <div class="goods-main">
            <div class="goods-name">{{ item.skuName }}</div>
            <div class="muted">单价 ¥{{ formatPrice(item.salePrice) }}</div>
            <div class="goods-bottom">
              <span>x{{ item.quantity }}</span>
              <span class="price">¥{{ formatPrice(item.totalAmount) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="card">
        <div class="card-title">支付与操作</div>
        <div class="summary-line"><span>订单状态</span><span :class="`status-text status-text--${statusClass(order.status)}`">{{ formatStatus(order.status) }}</span></div>
        <div class="summary-line"><span>支付方式</span><span>{{ currentPayMethodLabel }}</span></div>
        <div v-if="order.paidAt" class="summary-line"><span>支付时间</span><span>{{ formatDateTime(order.paidAt) }}</span></div>
        <div v-if="order.shippedAt" class="summary-line"><span>发货时间</span><span>{{ formatDateTime(order.shippedAt) }}</span></div>
        <div v-if="order.completedAt" class="summary-line"><span>完成时间</span><span>{{ formatDateTime(order.completedAt) }}</span></div>
        <div v-if="order.cancelledAt" class="summary-line"><span>取消/退款时间</span><span>{{ formatDateTime(order.cancelledAt) }}</span></div>
        <div class="summary-line"><span>总金额</span><span>¥{{ formatPrice(order.totalAmount) }}</span></div>
        <div class="summary-line"><span>应付金额</span><span>¥{{ formatPrice(order.payAmount) }}</span></div>

        <div v-if="order.status === 'PENDING_PAYMENT'" class="pay-channel-panel">
          <div class="pay-channel-title">选择支付渠道</div>
          <div class="pay-channel-options">
            <button
              v-for="channel in payChannels"
              :key="channel.value"
              class="pay-channel-option"
              :class="{ 'pay-channel-option--active': selectedPayChannel === channel.value }"
              @click="handleSelectPayChannel(channel.value)"
            >
              <span>{{ channel.label }}</span>
              <small>{{ channel.description }}</small>
            </button>
          </div>
        </div>

        <div class="action-group">
          <template v-if="order.status === 'PENDING_PAYMENT'">
            <van-button type="primary" round :loading="creatingPay" :disabled="creatingPay" @click="handleCreatePayOrder">创建支付单</van-button>
            <van-button v-if="payOrderNo && selectedPayChannel === 'ALIPAY_PC'" type="primary" plain round @click="handleReopenPayPage">重新打开支付页</van-button>
            <van-button v-if="payOrderNo && selectedPayChannel === 'MOCK'" type="success" round :loading="mockingPay" @click="handleMockPay">模拟支付成功</van-button>
            <van-button v-if="payOrderNo && isAlipayChannel(selectedPayChannel)" type="warning" plain round :loading="syncingPayStatus" @click="handleManualSyncPayStatus">已支付但未刷新？点此同步状态</van-button>
            <van-button v-if="payInfo?.callbackPayload" plain round @click="handleShowPayPayload">查看支付提交信息</van-button>
            <van-button plain round :loading="cancelling" @click="handleCancel">取消订单</van-button>
          </template>

          <template v-else-if="order.status === 'PAID' || order.status === 'SHIPPED'">
            <van-button type="primary" round :loading="confirmingReceipt" @click="handleConfirmReceipt">确认收货</van-button>
            <van-button type="warning" plain round :loading="refunding" @click="handleRefundApply">申请退款</van-button>
          </template>

          <template v-else-if="order.status === 'PROCESSING'">
            <van-button type="warning" plain round :loading="refunding" @click="handleRefundApply">申请退款</van-button>
          </template>
        </div>

        <div v-if="payInfo" class="pay-card">
          <div class="summary-line"><span>支付单号</span><span>{{ payInfo.payOrderNo }}</span></div>
          <div class="summary-line"><span>支付状态</span><span>{{ formatStatus(payInfo.status) }}</span></div>
          <div class="summary-line"><span>支付渠道</span><span>{{ formatPayChannel(payInfo.payChannel) }}</span></div>
          <div v-if="payInfo.redirectUrl" class="summary-line"><span>跳转地址</span><span class="muted">已生成</span></div>
        </div>
      </div>
    </div>

    <van-empty v-else description="订单不存在" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { showConfirmDialog, showDialog, showFailToast, showSuccessToast } from 'vant';
import { applyAftersaleRefund, cancelOrder, confirmReceipt, createPayOrder, fetchOrderDetail, fetchPayOrderDetail, mockPaySuccess, repairPaidOrder, syncPayOrderStatus } from '../api';
import { formatAddress, formatCountdown, formatDateTime, formatPrice, formatStatus } from '../utils/format';
import { getProductImage } from '../utils/productVisual';

const route = useRoute();
const router = useRouter();
const order = ref(null);
const payInfo = ref(null);
const payOrderNo = ref('');
const creatingPay = ref(false);
const mockingPay = ref(false);
const cancelling = ref(false);
const confirmingReceipt = ref(false);
const refunding = ref(false);
const syncingPayStatus = ref(false);
const remainingPaySeconds = ref(0);
const selectedPayChannel = ref('ALIPAY_PC');
const payChannels = [
  { value: 'MOCK', label: 'MOCK 模拟支付', description: '本地快速联调，支持一键模拟支付成功' },
  { value: 'ALIPAY_PC', label: '支付宝 PC 沙箱', description: '推荐本地电脑浏览器联调，稳定性优先' },
  { value: 'ALIPAY_WAP', label: '支付宝 H5 沙箱', description: '手机网站支付联调，沙箱验证码页可能不稳定' },
  { value: 'WECHAT_H5', label: '微信 H5 沙箱', description: '预留微信 H5 支付渠道接入' },
];
let timer = null;
let countdownBaseTime = Date.now();
let refreshingAfterCountdown = false;
let payStatusPoller = null;
let payStatusPollAttempts = 0;

const handlePayReturnStorage = async (event) => {
  if (event.key !== 'mallfei:pay-return-refresh' || !event.newValue) {
    return;
  }
  const lastOrderId = localStorage.getItem('mallfei:last-pay-order-id') || '';
  if (String(route.params.id || '') !== lastOrderId) {
    return;
  }
  try {
    await loadOrder();
    const lastPayOrderNo = localStorage.getItem('mallfei:last-pay-order-no') || '';
    if (lastPayOrderNo) {
      payOrderNo.value = lastPayOrderNo;
      await refreshPayInfo();
    }
  } catch {
  }
};

const handlePayReturnMessage = async (event) => {
  if (!event?.data || event.data?.type !== 'mallfei:pay-return-refresh') {
    return;
  }
  if (String(event.data?.orderId || '') !== String(route.params.id || '')) {
    return;
  }
  try {
    if (event.data?.payOrderNo) {
      payOrderNo.value = String(event.data.payOrderNo);
    }
    await loadOrder();
    await refreshPayInfo();
  } catch {
  }
};

const itemVisual = (item) => ({
  id: item.skuId,
  name: item.skuName,
  skuName: item.skuName,
  skuCode: `SKU-${item.skuId}`,
  skuImageUrl: item.skuImageUrl,
  categoryId: 10,
});

const formatPayChannel = (channel) => payChannels.find((item) => item.value === channel)?.label || channel || '--';
const isAlipayChannel = (channel) => channel === 'ALIPAY_WAP' || channel === 'ALIPAY_PC';

const handleSelectPayChannel = async (channel) => {
  if (channel === 'ALIPAY_WAP') {
    try {
      await showDialog({
        title: '支付宝 H5 沙箱提示',
        message: '即将打开支付宝 H5 沙箱收银台。建议使用手机浏览器或 Chrome 移动端模拟器测试，并确保登录的是沙箱买家账号。\n\n如果页面提示账号或环境异常，请清理 alipaydev.com 相关 Cookie 后重试。',
        confirmButtonText: '继续使用 H5',
        messageAlign: 'left',
      });
    } catch {
    }
  }
  selectedPayChannel.value = channel;
};

const showPcSandboxHint = async () => {
  if (selectedPayChannel.value !== 'ALIPAY_PC') {
    return true;
  }
  try {
    await showDialog({
      title: '支付宝 PC 沙箱提示',
      message: '如果登录支付宝沙箱账号后首次出现收银台错误页，请直接刷新一次当前支付宝页面。\n\n这不会重复创建支付单，也不会重复扣款；若仍失败，你可以回到订单页点击“重新打开支付页”。',
      confirmButtonText: '我知道了',
      messageAlign: 'left',
    });
    return true;
  } catch {
    return true;
  }
};

const currentPayMethodLabel = computed(() => {
  if (payInfo.value?.payChannel) {
    return formatPayChannel(payInfo.value.payChannel);
  }
  if (order.value?.status === 'PENDING_PAYMENT' && selectedPayChannel.value) {
    return formatPayChannel(selectedPayChannel.value);
  }
  return order.value?.payType || '--';
});

const statusClass = (status) => {
  if (status === 'PENDING_PAYMENT') return 'warning';
  if (status === 'PAID') return 'paid';
  if (status === 'PROCESSING') return 'processing';
  if (status === 'SHIPPED') return 'shipped';
  if (status === 'COMPLETED') return 'success';
  if (status === 'REFUNDED') return 'refund';
  if (status === 'CANCELLED' || status === 'TIMEOUT_CANCELLED' || status === 'CLOSED' || status === 'REFUND_CLOSED') return 'danger';
  return 'primary';
};

const stopPayStatusPolling = () => {
  if (payStatusPoller) {
    clearInterval(payStatusPoller);
    payStatusPoller = null;
  }
  payStatusPollAttempts = 0;
};

const syncPaidStateIfNecessary = async ({ silent = true, allowRepair = false } = {}) => {
  if (!order.value?.orderNo) return false;
  try {
    syncingPayStatus.value = !silent;
    const { data } = await syncPayOrderStatus(order.value.orderNo);
    payInfo.value = data.data;
    if (data.data?.payOrderNo) payOrderNo.value = data.data.payOrderNo;
    await loadOrder();
    await refreshPayInfo();
    if (order.value?.status !== 'PENDING_PAYMENT') {
      if (!silent) showSuccessToast('订单支付状态已同步');
      return true;
    }
    if (allowRepair && payInfo.value?.status === 'SUCCESS') {
      const repairResponse = await repairPaidOrder(order.value.orderNo);
      payInfo.value = repairResponse.data.data;
      if (repairResponse.data.data?.payOrderNo) payOrderNo.value = repairResponse.data.data.payOrderNo;
      await loadOrder();
      await refreshPayInfo();
      if (order.value?.status !== 'PENDING_PAYMENT') {
        if (!silent) showSuccessToast('已根据支付结果补偿订单状态');
        return true;
      }
    }
    if (!silent) showFailToast('暂未同步到支付成功，请稍后再试');
    return false;
  } catch (error) {
    if (!silent) showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '同步支付状态失败');
    return false;
  } finally {
    syncingPayStatus.value = false;
  }
};

const startPayStatusPolling = () => {
  stopPayStatusPolling();
  payStatusPoller = setInterval(async () => {
    payStatusPollAttempts += 1;
    const synced = await syncPaidStateIfNecessary({ silent: true, allowRepair: true });
    if (synced || payStatusPollAttempts >= 12 || order.value?.status !== 'PENDING_PAYMENT') {
      stopPayStatusPolling();
    }
  }, 5000);
};

const refreshOrderWhenCountdownEnds = async () => {
  if (refreshingAfterCountdown) return;
  refreshingAfterCountdown = true;
  try {
    await loadOrder();
    await refreshPayInfo();
  } finally {
    refreshingAfterCountdown = false;
  }
};

const loadOrder = async () => {
  const { data } = await fetchOrderDetail(route.params.id);
  order.value = data.data;
  countdownBaseTime = Date.now();
  remainingPaySeconds.value = Number(data.data?.remainingPaySeconds || 0);
  if (data.data?.status === 'PENDING_PAYMENT' && Number(data.data?.remainingPaySeconds || 0) === 0) {
    setTimeout(() => refreshOrderWhenCountdownEnds(), 300);
  }
  if (data.data?.status !== 'PENDING_PAYMENT') {
    stopPayStatusPolling();
  }
};

const tickCountdown = () => {
  if (order.value?.status !== 'PENDING_PAYMENT') {
    remainingPaySeconds.value = 0;
    countdownBaseTime = Date.now();
    return;
  }
  const elapsedSeconds = Math.max(1, Math.floor((Date.now() - countdownBaseTime) / 1000));
  countdownBaseTime = Date.now();
  const currentSeconds = Number(remainingPaySeconds.value || 0);
  const nextSeconds = Math.max(0, currentSeconds - elapsedSeconds);
  remainingPaySeconds.value = nextSeconds;
  if (nextSeconds === 0 && currentSeconds > 0) refreshOrderWhenCountdownEnds();
};

const handleVisibilityChange = () => {
  if (!document.hidden) {
    countdownBaseTime = Date.now();
    loadOrder().then(() => refreshPayInfo());
  }
};

const startTimer = () => {
  if (timer) clearInterval(timer);
  timer = setInterval(tickCountdown, 1000);
};

const refreshPayInfo = async () => {
  if (!payOrderNo.value) return;
  try {
    const { data } = await fetchPayOrderDetail(payOrderNo.value);
    payInfo.value = data.data;
    if (data.data?.payChannel) {
      if (data.data.payChannel === 'ALIPAY_WAP') {
        selectedPayChannel.value = 'ALIPAY_WAP';
      } else {
        selectedPayChannel.value = data.data.payChannel;
      }
    }
  } catch {
  }
};

const openPaySubmitPage = (payOrder) => {
  const targetPayOrderNo = payOrder?.payOrderNo;
  if (!targetPayOrderNo) return false;
  const submitUrl = `/api/pay/orders/${encodeURIComponent(targetPayOrderNo)}/submit-page?returnPath=${encodeURIComponent(route.fullPath || `/orders/${route.params.id}`)}`;
  setTimeout(() => {
    if (payOrder?.payChannel === 'ALIPAY_PC') {
      window.open(submitUrl, '_blank');
      return;
    }
    window.location.href = submitUrl;
  }, 100);
  return true;
};

const handleCreatePayOrder = async () => {
  try {
    creatingPay.value = true;
    const returnPath = route.fullPath || `/orders/${route.params.id}`;
    const { data } = await createPayOrder(order.value.orderNo, selectedPayChannel.value, returnPath);
    payInfo.value = data.data;
    payOrderNo.value = data.data.payOrderNo;
    if (data.data?.payChannel) {
      if (data.data.payChannel === 'ALIPAY_WAP') {
        selectedPayChannel.value = 'ALIPAY_WAP';
      } else {
        selectedPayChannel.value = data.data.payChannel;
      }
    }
    showSuccessToast('支付单创建成功');
    if (isAlipayChannel(data.data?.payChannel)) {
      localStorage.setItem('mallfei:last-pay-order-no', data.data.payOrderNo || '');
      localStorage.setItem('mallfei:last-pay-order-order-no', order.value.orderNo || '');
      localStorage.setItem('mallfei:last-pay-order-id', String(route.params.id || ''));
      localStorage.setItem('mallfei:last-pay-channel', data.data.payChannel || '');
      startPayStatusPolling();
      if (openPaySubmitPage(data.data)) {
        return;
      }
      showFailToast('支付宝沙箱跳转信息缺失，请检查后端配置');
    }
  } catch (error) {
    showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '创建支付单失败');
  } finally {
    creatingPay.value = false;
  }
};

const handleMockPay = async () => {
  try {
    mockingPay.value = true;
    await mockPaySuccess(order.value.orderNo);
    localStorage.setItem('mallfei:product-sales-refresh', String(Date.now()));
    await refreshPayInfo();
    await loadOrder();
    showSuccessToast('模拟支付成功');
  } catch (error) {
    showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '模拟支付失败');
  } finally {
    mockingPay.value = false;
  }
};

const handleManualSyncPayStatus = async () => {
  await syncPaidStateIfNecessary({ silent: false, allowRepair: true });
};

const handleShowPayPayload = async () => {
  await showDialog({ title: '支付提交信息', message: payInfo.value?.callbackPayload || '暂无提交信息', confirmButtonText: '我知道了', messageAlign: 'left' });
};

const handleCancel = async () => {
  try {
    cancelling.value = true;
    stopPayStatusPolling();
    await cancelOrder(order.value.id);
    await loadOrder();
    await refreshPayInfo();
    showSuccessToast('订单已取消');
  } catch (error) {
    showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '取消订单失败');
  } finally {
    cancelling.value = false;
  }
};

const handleConfirmReceipt = async () => {
  try {
    await showConfirmDialog({ title: '确认收货', message: '确认收货后订单将变为已完成，且该操作不可撤销，确认继续吗？', confirmButtonText: '确认收货', cancelButtonText: '再想想' });
    confirmingReceipt.value = true;
    await confirmReceipt(order.value.id);
    await loadOrder();
    showSuccessToast('确认收货成功');
  } catch (error) {
    if (error !== 'cancel') showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '确认收货失败');
  } finally {
    confirmingReceipt.value = false;
  }
};

const handleRefundApply = async () => {
  try {
    await showConfirmDialog({ title: '申请退款', message: '确认发起仅退款申请吗？提交后将进入售后审核流程。', confirmButtonText: '确认申请', cancelButtonText: '再想想' });
    refunding.value = true;
    await applyAftersaleRefund({ orderId: order.value.id, reason: '用户发起退款' });
    await loadOrder();
    await refreshPayInfo();
    showSuccessToast('退款申请已提交');
  } catch (error) {
    if (error !== 'cancel') showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '申请退款失败');
  } finally {
    refunding.value = false;
  }
};

onMounted(async () => {
  window.addEventListener('storage', handlePayReturnStorage);
  window.addEventListener('message', handlePayReturnMessage);
  document.addEventListener('visibilitychange', handleVisibilityChange);
  try {
    await loadOrder();
    startTimer();
    const lastPayOrderNo = localStorage.getItem('mallfei:last-pay-order-no') || '';
    const lastOrderNo = localStorage.getItem('mallfei:last-pay-order-order-no') || '';
    const lastOrderId = localStorage.getItem('mallfei:last-pay-order-id') || '';
    const lastPayChannel = localStorage.getItem('mallfei:last-pay-channel') || '';
    if (String(route.params.id || '') === lastOrderId) {
      if (lastPayOrderNo) payOrderNo.value = lastPayOrderNo;
      if (lastPayChannel) selectedPayChannel.value = lastPayChannel;
      await refreshPayInfo();
      if (lastOrderNo && order.value?.status === 'PENDING_PAYMENT') {
        await syncPaidStateIfNecessary({ silent: true, allowRepair: true });
        if (order.value?.status === 'PENDING_PAYMENT') {
          startPayStatusPolling();
        }
      }
    }
  } catch (error) {
    showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '订单详情加载失败');
  }
});

onBeforeUnmount(() => {
  window.removeEventListener('storage', handlePayReturnStorage);
  window.removeEventListener('message', handlePayReturnMessage);
  document.removeEventListener('visibilitychange', handleVisibilityChange);
  if (timer) clearInterval(timer);
  stopPayStatusPolling();
});
</script>

<style scoped>
.page { min-height: 100vh; background: #f6f8fb; }
.content { padding: 12px 12px 24px; }
.card { margin-bottom: 12px; padding: 16px; background: #fff; border-radius: 18px; }
.head-row, .summary-line, .goods-bottom { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.order-no-label { font-size: 12px; color: #94a3b8; }
.order-no { margin-top: 4px; font-size: 16px; font-weight: 700; line-height: 1.35; letter-spacing: 0.2px; word-break: break-all; }
.status-badge { display: inline-flex; align-items: center; margin-top: 10px; padding: 4px 12px; border-radius: 999px; font-size: 13px; font-weight: 700; border: 1px solid transparent; }
.status-badge--primary, .status-text--primary { color: #1677ff; }
.status-badge--warning, .status-text--warning { color: #d97706; background: #fffbeb; border-color: #fde68a; }
.status-badge--paid, .status-text--paid { color: #2563eb; background: #eff6ff; border-color: #93c5fd; }
.status-badge--processing, .status-text--processing { color: #7c3aed; background: #f5f3ff; border-color: #c4b5fd; }
.status-badge--shipped, .status-text--shipped { color: #0891b2; background: #ecfeff; border-color: #67e8f9; }
.status-badge--success, .status-text--success { color: #16a34a; background: #f0fdf4; border-color: #bbf7d0; }
.status-badge--refund, .status-text--refund { color: #db2777; background: #fdf2f8; border-color: #f9a8d4; }
.status-badge--danger, .status-text--danger { color: #dc2626; background: #fef2f2; border-color: #fecaca; }
.status, .muted { margin-top: 8px; color: #64748b; }
.countdown-text { margin-top: 10px; color: #f59e0b; font-weight: 700; }
.countdown-tip { margin-top: 6px; color: #94a3b8; font-size: 12px; }
.card-title { margin-bottom: 12px; font-size: 16px; font-weight: 700; }
.goods-row { display: flex; gap: 12px; padding: 12px 0; border-bottom: 1px solid #f1f5f9; }
.goods-row:last-child { border-bottom: none; }
.goods-image { width: 88px; height: 88px; border-radius: 14px; object-fit: cover; background: #f8fafc; }
.goods-main { flex: 1; }
.goods-name { font-weight: 600; color: #111827; }
.price { color: #ee0a24; font-weight: 700; }
.pay-channel-panel { margin-top: 16px; padding: 14px; background: linear-gradient(180deg, #f8fbff 0%, #f8fafc 100%); border: 1px solid #e2e8f0; border-radius: 16px; }
.pay-channel-title { margin-bottom: 10px; font-size: 14px; font-weight: 700; color: #0f172a; }
.pay-channel-options { display: flex; flex-direction: column; gap: 10px; }
.pay-channel-option { display: flex; flex-direction: column; align-items: flex-start; gap: 4px; width: 100%; padding: 12px 14px; background: #fff; border: 1px solid #dbeafe; border-radius: 14px; color: #334155; font-size: 14px; text-align: left; }
.pay-channel-option small { color: #94a3b8; line-height: 1.4; }
.pay-channel-option--active { border-color: #1677ff; box-shadow: 0 0 0 2px rgba(22, 119, 255, 0.08); color: #1677ff; }
.action-group { display: flex; flex-direction: column; gap: 12px; margin-top: 16px; }
.pay-card { margin-top: 16px; padding: 14px; background: #f8fafc; border-radius: 14px; }
</style>
