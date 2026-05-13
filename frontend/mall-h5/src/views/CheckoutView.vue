<template>
  <div class="page">
    <van-nav-bar title="确认订单" left-arrow @click-left="router.back()" />

    <div class="card address-card" @click="goSelectAddress">
      <div class="card-title-row">
        <span class="card-title">收货地址</span>
        <span class="card-action">去选择</span>
      </div>
      <template v-if="selectedAddress">
        <div class="address-name">{{ selectedAddress.receiverName }} {{ selectedAddress.receiverPhone }}</div>
        <div class="address-detail">{{ formatAddress(selectedAddress) }}</div>
      </template>
      <van-empty v-else image="error" description="请先新增或选择收货地址" />
    </div>

    <div class="card">
      <div class="card-title">订单信息</div>
      <div v-if="isBuyNow && buyNowSummary" class="summary-block">
        <div class="summary-line"><span>商品ID</span><span>{{ buyNowSummary.productId }}</span></div>
        <div class="summary-line"><span>SKU ID</span><span>{{ buyNowSummary.skuId }}</span></div>
        <div class="summary-line"><span>购买数量</span><span>{{ buyNowSummary.quantity }}</span></div>
      </div>
      <div v-else-if="cartPreview" class="summary-block">
        <div class="summary-line"><span>已勾选商品数</span><span>{{ cartPreview.checkedCount }}</span></div>
        <div class="summary-line"><span>预览商品项</span><span>{{ cartPreview.items?.length || 0 }}</span></div>
      </div>
      <div class="summary-line total-line">
        <span>应付金额</span>
        <span class="amount">¥{{ formatPrice(totalAmount) }}</span>
      </div>
    </div>

    <div class="card">
      <div class="card-title">订单备注</div>
      <van-field v-model="remark" rows="3" autosize type="textarea" maxlength="100" placeholder="选填，给商家留言" show-word-limit />
    </div>

    <div class="submit-wrap">
      <van-button block round type="primary" :loading="submitting" @click="submitOrder">提交订单</van-button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { showFailToast, showSuccessToast } from 'vant';
import { checkoutCart, createOrder, fetchCartSettlementPreview, fetchUserAddresses } from '../api';
import { formatAddress, formatPrice } from '../utils/format';

const SELECTED_ADDRESS_KEY = 'mall-h5-selected-address-id';

const route = useRoute();
const router = useRouter();
const addresses = ref([]);
const cartPreview = ref(null);
const remark = ref('');
const submitting = ref(false);
const selectedAddressId = ref(null);

const isBuyNow = computed(() => route.query.source === 'buyNow');
const buyNowSummary = computed(() => {
  if (!isBuyNow.value) {
    return null;
  }
  return {
    productId: Number(route.query.productId),
    skuId: Number(route.query.skuId),
    quantity: Number(route.query.quantity || 1),
    salePrice: Number(route.query.salePrice || 0),
  };
});

const selectedAddress = computed(() => {
  if (!addresses.value.length) {
    return null;
  }
  if (selectedAddressId.value != null) {
    const manualSelected = addresses.value.find((item) => String(item.id) === String(selectedAddressId.value));
    if (manualSelected) {
      return manualSelected;
    }
  }
  return addresses.value.find((item) => item.default) || addresses.value[0] || null;
});

const totalAmount = computed(() => {
  if (isBuyNow.value) {
    return (buyNowSummary.value?.salePrice || 0) * (buyNowSummary.value?.quantity || 1);
  }
  return cartPreview.value?.totalAmount || 0;
});

const restoreSelectedAddressId = () => {
  const cached = localStorage.getItem(SELECTED_ADDRESS_KEY);
  if (cached) {
    selectedAddressId.value = cached;
  }
};

const syncSelectedAddressId = () => {
  if (selectedAddress.value?.id != null) {
    selectedAddressId.value = selectedAddress.value.id;
    localStorage.setItem(SELECTED_ADDRESS_KEY, String(selectedAddress.value.id));
  } else {
    selectedAddressId.value = null;
    localStorage.removeItem(SELECTED_ADDRESS_KEY);
  }
};

const loadAddresses = async () => {
  const { data } = await fetchUserAddresses();
  addresses.value = data.data || [];
  syncSelectedAddressId();
};

const loadCartPreview = async () => {
  if (isBuyNow.value) {
    return;
  }
  const { data } = await fetchCartSettlementPreview();
  cartPreview.value = data.data;
};

const goSelectAddress = () => {
  router.push({
    path: '/address',
    query: {
      mode: 'select',
      from: '/checkout',
    },
  });
};

const buildAddressPayload = () => {
  if (!selectedAddress.value) {
    return null;
  }
  return {
    receiverName: selectedAddress.value.receiverName,
    receiverPhone: selectedAddress.value.receiverPhone,
    receiverProvinceName: selectedAddress.value.provinceName,
    receiverCityName: selectedAddress.value.cityName,
    receiverDistrictName: selectedAddress.value.districtName,
    receiverDetailAddress: selectedAddress.value.detailAddress,
    remark: remark.value,
  };
};

const submitOrder = async () => {
  const addressPayload = buildAddressPayload();
  if (!addressPayload) {
    showFailToast('请先添加收货地址');
    return;
  }
  try {
    submitting.value = true;
    let order;
    if (isBuyNow.value) {
      const { data } = await createOrder({
        ...addressPayload,
        items: [
          {
            skuId: buyNowSummary.value.skuId,
            quantity: buyNowSummary.value.quantity,
          },
        ],
      });
      order = data.data;
    } else {
      const { data } = await checkoutCart(addressPayload);
      order = data.data;
    }
    showSuccessToast('订单提交成功');
    router.replace(`/orders/${order.id}`);
  } catch (error) {
    showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '提交订单失败');
  } finally {
    submitting.value = false;
  }
};

onMounted(async () => {
  try {
    restoreSelectedAddressId();
    await Promise.all([loadAddresses(), loadCartPreview()]);
  } catch (error) {
    showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '订单确认页加载失败');
  }
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 24px;
  background: #f6f8fb;
}

.card {
  margin: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 18px;
}

.address-card {
  cursor: pointer;
}

.card-title-row,
.summary-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-title {
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 700;
}

.card-action {
  color: #1989fa;
  font-size: 13px;
}

.address-name {
  font-size: 16px;
  font-weight: 600;
}

.address-detail,
.summary-block {
  color: #64748b;
  line-height: 1.8;
}

.total-line {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #eef2f7;
}

.amount {
  color: #ee0a24;
  font-size: 20px;
  font-weight: 700;
}

.submit-wrap {
  padding: 12px;
}
</style>
