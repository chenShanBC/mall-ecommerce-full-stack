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
          <div v-for="item in previewItems" :key="item.id" class="preview-item">
            <img class="preview-image" :src="getCartItemImage(item)" :alt="item.productName" />
            <div class="preview-main">
              <div class="preview-name van-multi-ellipsis--l2">{{ item.productName }}</div>
              <div class="preview-spec">{{ item.skuName || '默认规格' }}</div>
              <div class="preview-bottom-row">
                <span class="preview-price">¥{{ formatPrice(item.unitPrice) }}</span>
                <span class="preview-qty">x{{ item.quantity }}</span>
              </div>
            </div>
          </div>
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
import { computed, onActivated, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchCartItems, fetchCartQuantity } from '../api';
import { formatPrice } from '../utils/format';
import { buildProductVisual, isInvalidImageUrl } from '../utils/productVisual';
import { getFallbackCartItems, mergeCartItems } from '../utils/cartFallback';

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

const openCartPage = () => {
  showPreview.value = false;
  router.push('/cart');
};

onMounted(loadQuantity);
onActivated(loadQuantity);
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

.preview-item {
  display: flex;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #eef2f7;
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

.preview-qty {
  color: #475569;
  font-size: 13px;
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
