<template>
  <div class="pay-return-page">
    <van-nav-bar title="支付结果" left-arrow @click-left="goBack" />

    <div class="pay-return-card">
      <div class="pay-return-icon" :class="`pay-return-icon--${statusType}`">{{ statusIcon }}</div>
      <div class="pay-return-title">{{ statusTitle }}</div>
      <div class="pay-return-desc">{{ statusDesc }}</div>

      <div v-if="context" class="pay-return-detail">
        <div class="pay-return-line"><span>订单号</span><span>{{ context.orderNo }}</span></div>
        <div class="pay-return-line"><span>支付单号</span><span>{{ context.payOrderNo }}</span></div>
        <div class="pay-return-line"><span>支付渠道</span><span>{{ context.payChannel }}</span></div>
      </div>

      <div class="pay-return-actions">
        <van-button type="primary" round block :loading="processing" @click="goBack">返回订单页</van-button>
        <van-button plain round block @click="goOrders">查看订单列表</van-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { showFailToast } from 'vant';
import { fetchPayOrderDetail, repairPaidOrder, syncPayOrderStatus } from '../api';

const route = useRoute();
const router = useRouter();
const processing = ref(true);
const context = ref(null);
const status = ref('PROCESSING');

const statusType = computed(() => {
  if (status.value === 'SUCCESS') return 'success';
  if (status.value === 'FAILED') return 'danger';
  return 'warning';
});

const statusIcon = computed(() => {
  if (status.value === 'SUCCESS') return '✓';
  if (status.value === 'FAILED') return '!';
  return '...';
});

const statusTitle = computed(() => {
  if (status.value === 'SUCCESS') return '支付成功';
  if (status.value === 'FAILED') return '支付未完成';
  return '正在同步支付结果';
});

const statusDesc = computed(() => {
  if (status.value === 'SUCCESS') return '支付已成功，原订单页面已同步刷新。你可以保留此页面查看结果，或手动关闭当前窗口。';
  if (status.value === 'FAILED') return '暂未确认支付成功，你可以返回订单页继续查看或手动同步。';
  return '请稍候，系统正在同步支付宝返回结果。';
});

const resolveReturnPath = () => context.value?.returnPath || '/orders';

const notifyOpenerRefresh = () => {
  const returnPath = resolveReturnPath();
  try {
    if (window.opener && !window.opener.closed) {
      const orderId = String(localStorage.getItem('mallfei:last-pay-order-id') || '');
      window.opener.postMessage({
        type: 'mallfei:pay-return-refresh',
        orderId,
        payOrderNo: context.value?.payOrderNo || '',
        orderNo: context.value?.orderNo || '',
        payChannel: context.value?.payChannel || '',
        returnPath,
      }, window.location.origin);
      window.opener.localStorage.setItem('mallfei:pay-return-refresh', String(Date.now()));
      if (context.value?.payOrderNo) window.opener.localStorage.setItem('mallfei:last-pay-order-no', context.value.payOrderNo);
      if (context.value?.orderNo) window.opener.localStorage.setItem('mallfei:last-pay-order-order-no', context.value.orderNo);
      if (context.value?.payChannel) window.opener.localStorage.setItem('mallfei:last-pay-channel', context.value.payChannel);
      try {
        window.opener.location.href = returnPath;
      } catch {
      }
      return true;
    }
  } catch {
  }
  return false;
};

const goOrders = () => {
  router.replace('/orders');
};

const goBack = () => {
  router.replace(resolveReturnPath());
};

const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

const queryPaySuccess = async (orderNo, payOrderNo) => {
  const syncResponse = await syncPayOrderStatus(orderNo);
  const syncPayInfo = syncResponse.data?.data;
  if (syncPayInfo?.status === 'SUCCESS') {
    return true;
  }

  if (payOrderNo) {
    const detailResponse = await fetchPayOrderDetail(payOrderNo);
    const payDetail = detailResponse.data?.data;
    if (payDetail?.status === 'SUCCESS') {
      return true;
    }
  }

  try {
    const repairResponse = await repairPaidOrder(orderNo);
    const repairedPayInfo = repairResponse.data?.data;
    if (repairedPayInfo?.status === 'SUCCESS') {
      return true;
    }
  } catch {
  }

  return false;
};

onMounted(async () => {
  const payOrderNo = String(route.query.payOrderNo || '');
  const orderNo = String(route.query.orderNo || '');
  const payChannel = String(route.query.payChannel || '');
  const returnPath = String(route.query.returnPath || '/orders');

  if (!payOrderNo || !orderNo) {
    status.value = 'FAILED';
    processing.value = false;
    showFailToast('支付回跳参数缺失');
    return;
  }

  context.value = {
    payOrderNo,
    orderNo,
    payChannel,
    returnPath,
  };
  localStorage.setItem('mallfei:last-pay-order-no', payOrderNo);
  localStorage.setItem('mallfei:last-pay-order-order-no', orderNo);
  localStorage.setItem('mallfei:last-pay-channel', payChannel);

  try {
    let success = false;
    for (let i = 0; i < 5; i += 1) {
      try {
        success = await queryPaySuccess(orderNo, payOrderNo);
        if (success) {
          break;
        }
        if (i < 4) {
          await wait(1500);
        }
      } catch {
        if (i < 4) {
          await wait(1500);
        }
      }
    }

    status.value = success ? 'SUCCESS' : 'FAILED';
    processing.value = false;

    if (success) {
      localStorage.setItem('mallfei:product-sales-refresh', String(Date.now()));
      notifyOpenerRefresh();
    }
  } catch (error) {
    status.value = 'FAILED';
    processing.value = false;
    showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '支付结果查询失败');
  }
});
</script>

<style scoped>
.pay-return-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8fbff 0%, #f6f8fb 100%);
}

.pay-return-card {
  margin: 16px;
  padding: 28px 20px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}

.pay-return-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 72px;
  height: 72px;
  margin: 0 auto 18px;
  border-radius: 999px;
  font-size: 30px;
  font-weight: 700;
}

.pay-return-icon--success {
  color: #16a34a;
  background: #f0fdf4;
}

.pay-return-icon--warning {
  color: #d97706;
  background: #fffbeb;
}

.pay-return-icon--danger {
  color: #dc2626;
  background: #fef2f2;
}

.pay-return-title {
  text-align: center;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}

.pay-return-desc {
  margin-top: 10px;
  text-align: center;
  font-size: 14px;
  line-height: 1.7;
  color: #64748b;
}

.pay-return-detail {
  margin-top: 20px;
  padding: 14px;
  border-radius: 16px;
  background: #f8fafc;
}

.pay-return-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 8px 0;
  color: #334155;
  font-size: 13px;
  word-break: break-all;
}

.pay-return-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 22px;
}
</style>
