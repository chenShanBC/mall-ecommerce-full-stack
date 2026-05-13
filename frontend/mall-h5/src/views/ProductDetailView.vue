<template>
  <div class="detail-page">
    <van-nav-bar title="商品详情" left-arrow @click-left="router.back()" />

    <van-loading v-if="loading" size="24px" vertical class="loading-block">加载中...</van-loading>

    <template v-else-if="product">
      <img class="banner" :src="activeBanner" :alt="product.name" />

      <div class="info-card">
        <div class="price-row">
          <span class="sale-price">¥{{ formatPrice(activeSalePrice) }}</span>
          <span class="origin-price">¥{{ formatPrice(activeOriginPrice) }}</span>
        </div>
        <div class="product-name">{{ product.name }}</div>
        <div class="product-meta">销量 {{ product.sales || 0 }} · 类目ID {{ product.categoryId }} · SKU {{ skuCountText }}</div>
        <div class="product-desc">{{ product.description || '暂无商品描述' }}</div>
      </div>

      <div class="info-card">
        <div class="section-title">规格选择</div>
        <div class="sku-panel">
          <div
            v-for="sku in normalizedSkus"
            :key="sku.id"
            class="sku-card"
            :class="{ active: activeSku.id === sku.id }"
            @click="activeSku = sku"
          >
            <div class="sku-card-header">
              <div class="sku-card-name">{{ sku.skuName }}</div>
              <div class="sku-card-price">¥{{ formatPrice(sku.salePrice || product.salePrice) }}</div>
            </div>
            <div class="sku-card-code">{{ sku.skuCode || '暂无 SKU 编码' }}</div>
            <div class="sku-spec-list">
              <span v-for="spec in sku.specEntries" :key="`${sku.id}-${spec.key}`" class="sku-spec-chip">
                {{ spec.key }}：{{ spec.value }}
              </span>
            </div>
          </div>
        </div>
        <div class="sku-tip">当前选择：{{ activeSku.skuName || '--' }}</div>
      </div>

      <div class="info-card quantity-card">
        <div class="section-title quantity-title">购买数量</div>
        <van-stepper v-model="quantity" min="1" integer />
      </div>

      <FloatingCartButton bottom="156px" />

      <div class="submit-bar">
        <div class="submit-price">
          合计 <span>¥{{ formatPrice(activeSalePrice * quantity) }}</span>
        </div>
        <div class="submit-actions">
          <van-button plain round type="primary" :loading="addingCart" @click="addToCart">加入购物车</van-button>
          <van-button type="primary" round @click="buyNow">立即购买</van-button>
        </div>
      </div>
    </template>

    <van-empty v-else description="商品不存在" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { showFailToast, showSuccessToast } from 'vant';
import { addCartItem, fetchProductDetail } from '../api';
import FloatingCartButton from '../components/FloatingCartButton.vue';
import { useUserStore } from '../stores/user';
import { formatPrice, safeJsonParse } from '../utils/format';
import { buildProductVisual, getProductImage, isInvalidImageUrl } from '../utils/productVisual';
import { requireLogin } from '../utils/requireLogin';
import { saveFallbackCartItem } from '../utils/cartFallback';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const addingCart = ref(false);
const product = ref(null);
const activeSku = ref({});
const quantity = ref(1);

const gallery = computed(() => {
  const images = safeJsonParse(product.value?.albumImages, []);
  return Array.isArray(images) ? images : [];
});

const normalizedSkus = computed(() => {
  const list = product.value?.skus || [];
  return list.map((sku, index) => {
    const parsedSpec = safeJsonParse(sku.specJson, {});
    const specEntries = Object.entries(parsedSpec || {}).map(([key, value]) => ({ key, value }));
    return {
      ...sku,
      specEntries,
      fallbackVisual: buildProductVisual({
        id: sku.id || index,
        name: product.value?.name,
        categoryId: product.value?.categoryId,
        skuName: sku.skuName,
        skuCode: sku.skuCode,
      }),
    };
  });
});

const activeSalePrice = computed(() => activeSku.value?.salePrice || product.value?.salePrice || 0);
const activeOriginPrice = computed(() => activeSku.value?.originPrice || product.value?.originPrice || 0);
const skuCountText = computed(() => `${normalizedSkus.value.length || 0} 个规格`);
const checkoutRedirect = computed(() => `/checkout?source=buyNow&skuId=${activeSku.value.id}&quantity=${quantity.value}&productId=${product.value.id}&salePrice=${activeSalePrice.value}`);

const activeBanner = computed(() => {
  const firstGallery = gallery.value?.[0];
  if (firstGallery && !isInvalidImageUrl(firstGallery)) {
    return firstGallery;
  }
  if (activeSku.value?.id) {
    return activeSku.value.fallbackVisual;
  }
  return getProductImage(product.value || {});
});

const ensureSkuSelected = () => {
  if (!activeSku.value?.id) {
    showFailToast('当前商品暂无可购买 SKU');
    return false;
  }
  return true;
};

const loadDetail = async () => {
  try {
    loading.value = true;
    const { data } = await fetchProductDetail(route.params.id);
    product.value = data.data;
  } catch (error) {
    showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '商品详情加载失败');
  } finally {
    loading.value = false;
  }
};

const saveLocalCartFallback = () => {
  if (!product.value || !activeSku.value?.id) {
    return;
  }
  saveFallbackCartItem({
    skuId: activeSku.value.id,
    spuId: product.value.id,
    productName: product.value.name,
    productImage: product.value.mainImage || activeBanner.value,
    skuName: activeSku.value.skuName,
    specJson: activeSku.value.specJson || '{}',
    unitPrice: activeSalePrice.value,
    quantity: quantity.value,
    subtotalAmount: activeSalePrice.value * quantity.value,
    checked: true,
    canCheckout: true,
  });
};

const addToCart = async () => {
  if (!ensureSkuSelected()) {
    return;
  }
  if (!await requireLogin(router, () => route.fullPath)) {
    return;
  }
  try {
    addingCart.value = true;
    const response = await addCartItem({
      skuId: activeSku.value.id,
      quantity: quantity.value,
      checked: true,
    });
    if (response?.data?.success === false) {
      throw new Error(response?.data?.message || '加入购物车失败');
    }
    saveLocalCartFallback();
    showSuccessToast('加入购物车成功');
  } catch (error) {
    showFailToast(error?.response?.data?.msg || error?.response?.data?.message || (error?.message?.includes('MISCONF') ? 'Redis 写入异常，请重启后端或修复 Redis 配置' : error?.message) || '加入购物车失败');
  } finally {
    addingCart.value = false;
  }
};

const buyNow = async () => {
  if (!ensureSkuSelected()) {
    return;
  }
  if (!await requireLogin(router, () => checkoutRedirect.value)) {
    return;
  }
  router.push({
    path: '/checkout',
    query: {
      source: 'buyNow',
      skuId: activeSku.value.id,
      quantity: quantity.value,
      productId: product.value.id,
      salePrice: activeSalePrice.value,
    },
  });
};

onMounted(async () => {
  if (userStore.isLogin && !userStore.profile) {
    try {
      await userStore.ensureValidSession();
    } catch {
      await userStore.logout();
    }
  }
  await loadDetail();
  activeSku.value = normalizedSkus.value[0] || {};
});
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  padding-bottom: 90px;
  background: #f6f8fb;
}

.loading-block {
  padding-top: 80px;
}

.banner {
  display: block;
  width: 100%;
  height: 320px;
  object-fit: cover;
  background: #fff;
}

.info-card {
  margin: 12px;
  padding: 16px;
  background: #fff;
  border-radius: 18px;
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.sale-price {
  font-size: 28px;
  font-weight: 700;
  color: #ee0a24;
}

.origin-price {
  font-size: 14px;
  color: #94a3b8;
  text-decoration: line-through;
}

.product-name {
  margin-top: 12px;
  font-size: 20px;
  font-weight: 700;
  color: #111827;
}

.product-meta {
  margin-top: 10px;
  font-size: 12px;
  color: #64748b;
}

.product-desc {
  margin-top: 12px;
  font-size: 14px;
  line-height: 1.7;
  color: #475569;
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: #111827;
}

.sku-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.sku-card {
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: #f8fafc;
}

.sku-card.active {
  border-color: #2563eb;
  background: #eff6ff;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.12);
}

.sku-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.sku-card-name {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.sku-card-price {
  font-size: 16px;
  font-weight: 700;
  color: #ee0a24;
}

.sku-card-code {
  margin-top: 8px;
  font-size: 12px;
  color: #94a3b8;
}

.sku-spec-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.sku-spec-chip {
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
  color: #1d4ed8;
  font-size: 12px;
}

.sku-tip {
  margin-top: 12px;
  font-size: 13px;
  color: #475569;
}

.quantity-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.quantity-title {
  margin-bottom: 0;
}

.submit-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 -8px 24px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(14px);
}

.submit-price {
  color: #475569;
  font-size: 14px;
}

.submit-price span {
  color: #ee0a24;
  font-size: 24px;
  font-weight: 700;
}

.submit-actions {
  display: flex;
  gap: 10px;
}
</style>
