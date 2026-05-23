<template>
  <div class="floating-cart" :style="floatingStyle">
    <van-badge :content="badgeCount" :show-zero="false" :max="99">
      <van-button class="floating-cart-btn" round type="primary" @click="showPreview = true">
        <van-icon name="shopping-cart-o" size="22" />
      </van-button>
    </van-badge>

    <van-popup
      v-model:show="showPreview"
      position="bottom"
      round
      closeable
      :style="{ height: '62%' }"
      @open="loadCartPreview"
    >
      <div class="preview-body">
        <div class="preview-title">购物车预览</div>

        <van-empty v-if="!previewItems.length" description="购物车暂无商品" />

        <div v-else class="preview-list">
          <van-swipe-cell v-for="item in previewItems" :key="item.id" class="preview-swipe-cell">
            <div class="preview-item">
              <img class="preview-image" :src="getCartItemImage(item)" :alt="item.productName" />
              <div class="preview-main">
                <div class="preview-name van-multi-ellipsis--l2">{{ item.productName }}</div>
                <div class="preview-spec">{{ item.skuName || '默认规格' }}</div>
                <div class="preview-bottom-row">
                  <span class="preview-price">¥{{ formatPrice(item.unitPrice) }}</span>
                  <van-stepper
                    :model-value="item.quantity"
                    min="1"
                    integer
                    theme="round"
                    button-size="22"
                    @update:model-value="(value) => handleQuantityChange(item, value)"
                  />
                </div>
              </div>
            </div>
            <template #right>
              <div class="swipe-delete" @click="handleDelete(item)">删除</div>
            </template>
          </van-swipe-cell>
        </div>

        <div class="preview-footer">
          <div class="preview-summary">
            <div>共 {{ totalQuantity }} 件</div>
            <div class="preview-total">¥{{ formatPrice(totalAmount) }}</div>
          </div>
          <van-button block round type="primary" @click="openCartPage">去购物车</van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { computed, onActivated, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { deleteCartItem, fetchCartItems, fetchCartQuantity, updateCartItem } from '../api';
import { showFailToast, showSuccessToast } from 'vant';
import { formatPrice } from '../utils/format';
import { buildProductVisual, isInvalidImageUrl } from '../utils/productVisual';
import { getFallbackCartItems, mergeCartItems, removeFallbackCartItem, updateFallbackCartItem } from '../utils/cartFallback';
import { ensureAccessGate } from '../utils/requireLogin';

const props = defineProps({
  right: {
    type: String,
    default: '18px',
  },
  bottom: {
    type: String,
    default: '96px',
  },
});

const CART_REFRESH_EVENT = 'mallfei:cart-changed';

const router = useRouter();
const showPreview = ref(false);
const remoteQuantity = ref(0);
const fallbackQuantity = ref(0);
const previewItems = ref([]);

const badgeCount = computed(() => Math.max(remoteQuantity.value, fallbackQuantity.value));
const floatingStyle = computed(() => ({
  right: props.right,
  bottom: props.bottom,
}));
const totalQuantity = computed(() => previewItems.value.reduce((sum, item) => sum + Number(item.quantity || 0), 0));
const totalAmount = computed(() => previewItems.value.reduce((sum, item) => sum + Number(item.subtotalAmount || 0), 0));

const syncFallbackQuantity = () => {
  fallbackQuantity.value = getFallbackCartItems().reduce((sum, item) => sum + Number(item.quantity || 0), 0);
};

const loadRemoteQuantity = async () => {
  try {
    const { data } = await fetchCartQuantity();
    remoteQuantity.value = Number(data?.data?.totalQuantity || 0);
  } catch {
    remoteQuantity.value = 0;
  }
};

const loadQuantity = async () => {
  syncFallbackQuantity();
  await loadRemoteQuantity();
};

const loadCartPreview = async () => {
  try {
    const { data } = await fetchCartItems();
    const remoteItems = data?.data?.items || [];
    previewItems.value = mergeCartItems(remoteItems);
  } catch {
    previewItems.value = mergeCartItems([]);
  }
  await loadQuantity();
};

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

const handleQuantityChange = async (item, value) => {
  try {
    if (item.isLocalFallback) {
      updateFallbackCartItem(item.id, { quantity: value });
      await loadCartPreview();
      window.dispatchEvent(new CustomEvent(CART_REFRESH_EVENT));
      return;
    }
    await updateCartItem(item.id, { quantity: value });
    await loadCartPreview();
    window.dispatchEvent(new CustomEvent(CART_REFRESH_EVENT));
  } catch (error) {
    showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '数量修改失败');
  }
};

const handleDelete = async (item) => {
  try {
    if (item.isLocalFallback) {
      removeFallbackCartItem(item.id);
    } else {
      await deleteCartItem(item.id);
    }
    await loadCartPreview();
    window.dispatchEvent(new CustomEvent(CART_REFRESH_EVENT));
    showSuccessToast('删除成功');
  } catch (error) {
    showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '删除失败');
  }
};

const openCartPage = async () => {
  if (!await ensureAccessGate(router, '/cart', { force: true })) {
    return;
  }
  showPreview.value = false;
  router.push('/cart');
};

const handleCartChanged = async () => {
  await loadQuantity();
  if (showPreview.value) {
    await loadCartPreview();
  }
};

onMounted(() => {
  loadQuantity();
  window.addEventListener(CART_REFRESH_EVENT, handleCartChanged);
});
onActivated(loadQuantity);
onBeforeUnmount(() => {
  window.removeEventListener(CART_REFRESH_EVENT, handleCartChanged);
});
</script>

<style scoped>
.floating-cart {
  position: fixed;
  z-index: 30;
}

.floating-cart-btn {
  width: 52px;
  height: 52px;
  padding: 0;
  box-shadow: 0 10px 24px rgba(25, 137, 250, 0.24);
}

.preview-body {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px 16px 16px;
}

.preview-title {
  margin-bottom: 14px;
  text-align: center;
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}

.preview-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 2px;
}

.preview-swipe-cell {
  border-bottom: 1px solid #eef2f7;
}

.preview-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  background: #fff;
}

.preview-image {
  width: 68px;
  height: 68px;
  border-radius: 14px;
  object-fit: cover;
  background: #f8fafc;
  flex-shrink: 0;
}

.preview-main {
  flex: 1;
  min-width: 0;
}

.preview-name {
  font-weight: 700;
  color: #111827;
  line-height: 1.5;
}

.preview-spec {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}

.preview-bottom-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}

.preview-price {
  color: #ee0a24;
  font-size: 16px;
  font-weight: 700;
}

.swipe-delete {
  width: 72px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fb7185 0%, #f43f5e 100%);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}

.preview-footer {
  padding-top: 12px;
}

.preview-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  color: #475569;
}

.preview-total {
  color: #ee0a24;
  font-size: 20px;
  font-weight: 700;
}

:deep(.floating-cart-btn .van-button__content) {
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.van-badge__wrapper) {
  display: block;
}
</style>
