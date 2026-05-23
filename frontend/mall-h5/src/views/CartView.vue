<template>
  <div class="page">
    <div class="cart-panel">
      <van-nav-bar title="购物车" />

      <div class="card">
      <div class="card-header">
        <div class="card-title">购物车商品</div>
        <van-button v-if="items.length" size="small" plain round type="danger" @click="handleClear">清空</van-button>
      </div>

      <van-empty v-if="!items.length" description="购物车暂无商品" />

      <div v-else class="item-list">
        <div v-for="item in items" :key="item.id" class="item-row">
          <van-checkbox :model-value="item.checked" @update:model-value="(value) => handleToggleChecked(item, value)" />
          <img class="item-image" :src="getCartItemImage(item)" :alt="item.productName" />
          <div class="item-main">
            <div class="item-title">{{ item.productName }}</div>
            <div class="item-spec">{{ item.skuName || '默认规格' }}</div>
            <div class="item-spec" v-if="item.specJson && item.specJson !== '{}'">{{ item.specJson }}</div>
            <div class="item-price-row">
              <span class="item-price">¥{{ formatPrice(item.unitPrice) }}</span>
              <span class="item-subtotal">小计 ¥{{ formatPrice(item.subtotalAmount) }}</span>
            </div>
            <div class="item-actions">
              <van-stepper
                :model-value="item.quantity"
                min="1"
                integer
                @update:model-value="(value) => handleQuantityChange(item, value)"
              />
              <van-button size="small" plain round type="danger" @click="handleDelete(item)">删除</van-button>
            </div>
            <div v-if="!item.canCheckout" class="item-invalid">{{ item.invalidReason }}</div>
          </div>
        </div>
      </div>
    </div>

      <div class="card">
        <div class="card-title">金额汇总</div>
        <div class="summary-row"><span>商品项数</span><span>{{ summary.itemCount }}</span></div>
        <div class="summary-row"><span>商品总件数</span><span>{{ summary.totalQuantity }}</span></div>
        <div class="summary-row total"><span>已勾选金额</span><span>¥{{ formatPrice(summary.checkedTotalAmount) }}</span></div>
        <van-button block round type="primary" class="checkout-btn" :loading="preparingCheckout" @click="handleCheckout">去结算</van-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onActivated, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { showConfirmDialog, showFailToast, showSuccessToast } from 'vant';
import { clearCartItems, deleteCartItem, fetchCartItems, prepareCartCheckout, updateCartItem, updateCartItemChecked } from '../api';
import { formatPrice } from '../utils/format';
import { buildProductVisual, isInvalidImageUrl } from '../utils/productVisual';
import {
  clearFallbackCart,
  mergeCartItems,
  removeFallbackCartItem,
  updateFallbackCartItem,
} from '../utils/cartFallback';
import { useUserStore } from '../stores/user';
import { requireLogin, redirectLoginWithNotice } from '../utils/requireLogin';

const router = useRouter();
const userStore = useUserStore();
const items = ref([]);
const preparingCheckout = ref(false);
let sessionProbeTimer = null;

const CART_CACHE_KEY = 'mallfei:h5-cart-cache-v1';
const CART_CACHE_TTL = 180 * 1000;

const summary = computed(() => ({
  itemCount: items.value.length,
  totalQuantity: items.value.reduce((sum, item) => sum + (item.quantity || 0), 0),
  checkedTotalAmount: items.value
    .filter((item) => item.checked && item.canCheckout)
    .reduce((sum, item) => sum + (item.subtotalAmount || 0), 0),
}));

const getCartItemImage = (item) => {
  if (item.productImage && !isInvalidImageUrl(item.productImage)) {
    return item.productImage;
  }
  return buildProductVisual({
    id: item.skuId,
    name: item.productName,
    skuName: item.skuName,
    skuCode: `SKU-${item.skuId}`,
    categoryId: item.spuId || 10,
  });
};

const persistCartCache = () => {
  try {
    localStorage.setItem(CART_CACHE_KEY, JSON.stringify({
      updatedAt: Date.now(),
      items: items.value,
    }));
  } catch {
  }
};

const restoreCartCache = () => {
  try {
    const raw = localStorage.getItem(CART_CACHE_KEY);
    if (!raw) return false;
    const parsed = JSON.parse(raw);
    if (!parsed?.updatedAt || !Array.isArray(parsed?.items)) return false;
    if (Date.now() - Number(parsed.updatedAt) > CART_CACHE_TTL) return false;
    items.value = parsed.items;
    return true;
  } catch {
    return false;
  }
};

const loadData = async (force = false) => {
  try {
    const valid = await requireLogin(router, '/cart', { force });
    if (!valid) {
      if (userStore.lastSessionInvalidReason === 'DISABLED') {
        redirectLoginWithNotice(router);
      }
      return;
    }
    const { data } = await fetchCartItems();
    const remoteItems = data?.data?.items || [];
    items.value = mergeCartItems(remoteItems);
    persistCartCache();
  } catch (error) {
    items.value = mergeCartItems([]);
    showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '购物车加载失败');
  }
};

const startSessionProbe = () => {
  if (sessionProbeTimer) return;
  sessionProbeTimer = window.setInterval(async () => {
    const valid = await requireLogin(router, '/cart', { force: false });
    if (!valid && userStore.lastSessionInvalidReason === 'DISABLED') {
      redirectLoginWithNotice(router);
    }
  }, 45000);
};

const stopSessionProbe = () => {
  if (!sessionProbeTimer) return;
  clearInterval(sessionProbeTimer);
  sessionProbeTimer = null;
};

const handleQuantityChange = async (item, value) => {
  try {
    if (item.isLocalFallback) {
      updateFallbackCartItem(item.id, { quantity: value });
      await loadData();
      return;
    }
    await updateCartItem(item.id, { quantity: value });
    await loadData();
  } catch (error) {
    showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '数量修改失败');
  }
};

const handleToggleChecked = async (item, checked) => {
  try {
    if (item.isLocalFallback) {
      updateFallbackCartItem(item.id, { checked });
      item.checked = checked;
      await loadData();
      return;
    }
    await updateCartItemChecked({
      cartItemIds: [item.id],
      checked,
    });
    item.checked = checked;
  } catch (error) {
    showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '勾选状态更新失败');
  }
};

const handleDelete = async (item) => {
  try {
    await showConfirmDialog({ title: '删除确认', message: '确认删除该购物车商品吗？' });
    if (item.isLocalFallback) {
      removeFallbackCartItem(item.id);
      showSuccessToast('删除成功');
      await loadData();
      return;
    }
    await deleteCartItem(item.id);
    showSuccessToast('删除成功');
    await loadData();
  } catch (error) {
    if (error === 'cancel') {
      return;
    }
    showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '删除失败');
  }
};

const handleClear = async () => {
  try {
    await showConfirmDialog({ title: '清空确认', message: '确认清空当前购物车吗？' });
    clearFallbackCart();
    await clearCartItems();
    showSuccessToast('已清空购物车');
    await loadData();
  } catch (error) {
    if (error === 'cancel') {
      return;
    }
    showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '清空购物车失败');
  }
};

const handleCheckout = async () => {
  const localFallbackSelected = items.value.filter((item) => item.isLocalFallback && item.checked);
  if (localFallbackSelected.length) {
    showFailToast('当前有未同步到服务端的购物车商品，请稍后再试');
    return;
  }

  const selectedIds = items.value.filter((item) => item.checked).map((item) => item.id);
  if (!selectedIds.length) {
    showFailToast('请先勾选要结算的商品');
    return;
  }
  try {
    preparingCheckout.value = true;
    const { data } = await prepareCartCheckout({ cartItemIds: selectedIds });
    const result = data.data;
    if (!result?.passed) {
      const failed = result?.items?.find((entry) => !entry.passed);
      showFailToast(failed?.message || result?.message || '存在不可结算商品');
      await loadData();
      return;
    }
    router.push('/checkout');
  } catch (error) {
    showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '结算前校验失败');
  } finally {
    preparingCheckout.value = false;
  }
};

onMounted(() => {
  restoreCartCache();
  loadData();
  startSessionProbe();
});
onActivated(() => {
  loadData();
});

onBeforeUnmount(() => {
  stopSessionProbe();
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

.cart-panel {
  margin-top: 18px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.88);
  border-radius: 28px;
  box-shadow: 0 14px 36px rgba(108, 123, 225, 0.1);
  backdrop-filter: blur(18px);
}

:deep(.cart-panel .van-nav-bar) {
  margin: 0 0 12px;
}

.card {
  margin: 0 0 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(226, 232, 240, 0.7);
  border-radius: 22px;
  box-shadow: 0 10px 24px rgba(108, 123, 225, 0.08);
}

.card:last-child {
  margin-bottom: 0;
}

.card-header,
.summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: #334155;
}

.card-header .card-title {
  color: #0f172a;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.item-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  background: linear-gradient(135deg, #f8fbff 0%, #eef3ff 100%);
  border-radius: 18px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.75);
}

.item-image {
  width: 84px;
  height: 84px;
  border-radius: 14px;
  object-fit: cover;
  background: #fff;
  flex-shrink: 0;
}

.item-main {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-weight: 700;
  color: #111827;
}

.item-spec {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
  word-break: break-all;
}

.item-price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 10px;
}

.item-price {
  color: #ee0a24;
  font-size: 18px;
  font-weight: 700;
}

.item-subtotal {
  color: #475569;
  font-size: 13px;
}

.item-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
}

.item-invalid {
  margin-top: 10px;
  color: #ee0a24;
  font-size: 12px;
}

.summary-row {
  margin-top: 12px;
  color: #475569;
}

.total {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid #eef2f7;
  color: #0f172a;
  font-weight: 700;
}

.checkout-btn {
  margin-top: 16px;
}

:deep(.van-empty) {
  min-height: 280px;
}

:deep(.van-empty__description) {
  color: #94a3b8;
}
</style>
