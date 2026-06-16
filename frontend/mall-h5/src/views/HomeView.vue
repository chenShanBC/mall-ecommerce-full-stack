<template>
  <div class="page home-page">
    <div class="hero">
      <div class="hero-top">
        <div>
          <div class="hero-title">mallFei 商城</div>
          <div class="hero-subtitle">企业级 B2C 电商 MVP 前端演示</div>
        </div>
        <van-button class="hero-action-btn" size="small" round plain @click="handleProfileAction">
          {{ userStore.isLogin ? '个人中心' : '立即登录' }}
        </van-button>
      </div>
      <van-search v-model="keyword" shape="round" placeholder="搜索商品名称（支持中文 / 拼音 / 首字母）" />
    </div>

    <div class="section-card">
      <div class="section-header">
        <span class="section-title">类目导航</span>
        <span class="section-link" @click="resetFilter">全部商品</span>
      </div>
      <div class="category-list">
        <van-tag
          v-for="item in visibleCategories"
          :key="item.id"
          plain
          round
          size="large"
          :color="activeCategoryId === item.id ? '#4f46e5' : '#94a3b8'"
          @click="activeCategoryId = item.id"
        >
          {{ item.name }}
        </van-tag>
      </div>
    </div>

    <div class="section-card user-summary">
      <div class="summary-item">
        <div class="summary-label">登录状态</div>
        <div class="summary-value">{{ userStore.isLogin ? '已登录' : '未登录' }}</div>
      </div>
      <div class="summary-item">
        <div class="summary-label">当前用户</div>
        <div class="summary-value">{{ userStore.displayName }}</div>
      </div>
      <div class="summary-item">
        <div class="summary-label">商品数量</div>
        <div class="summary-value">{{ filteredProducts.length }}</div>
      </div>
    </div>

    <div class="product-section">
      <div class="section-header products-header">
        <span class="section-title">精选商品</span>
        <span class="section-desc">支持详情、立即购买、订单支付联调</span>
      </div>

      <van-empty v-if="!filteredProducts.length && !productsLoading" description="暂无匹配商品" />

      <van-list
        v-else
        v-model:loading="productsLoading"
        :finished="productsFinished"
        finished-text="没有更多商品了"
        loading-text="商品加载中..."
        @load="loadMoreProducts"
      >
        <div class="product-grid">
          <div v-for="item in filteredProducts" :key="item.id" class="product-card" @click="goDetail(item.id)">
            <img class="product-image" :src="getProductImage(item)" :alt="item.name" />
            <div class="product-body">
              <div class="product-name van-multi-ellipsis--l2">{{ item.name }}</div>
              <div class="product-sales">销量 {{ item.sales || 0 }}</div>
              <div class="product-price-row">
                <span class="price">¥{{ formatPrice(item.salePrice) }}</span>
                <span class="origin">¥{{ formatPrice(item.originPrice) }}</span>
              </div>
              <van-button size="small" type="primary" round block @click.stop="goDetail(item.id)">查看详情</van-button>
            </div>
          </div>
        </div>
      </van-list>
    </div>

    <FloatingCartButton draggable />
  </div>
</template>

<script setup>
import { computed, onActivated, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { showFailToast } from 'vant';
import { pinyin } from 'pinyin-pro';
import { fetchCategories, fetchProductPage } from '../api';
import FloatingCartButton from '../components/FloatingCartButton.vue';
import { useUserStore } from '../stores/user';
import { formatPrice } from '../utils/format';
import { getProductImage } from '../utils/productVisual';
import { ensureAccessGate } from '../utils/requireLogin';

const SALES_REFRESH_KEY = 'mallfei:product-sales-refresh';
const HOME_CATEGORIES_CACHE_KEY = 'mallfei:h5-home-categories-cache-v1';
const HOME_PRODUCTS_CACHE_KEY = 'mallfei:h5-home-products-cache-v1';
const HOME_CATEGORIES_CACHE_TTL = 30 * 60 * 1000;
const HOME_PRODUCTS_CACHE_TTL = 3 * 60 * 1000;
const PRODUCT_PAGE_SIZE = 10;

const router = useRouter();
const userStore = useUserStore();
const keyword = ref('');
const categories = ref([]);
const products = ref([]);
const activeCategoryId = ref(null);
const categoriesLoadedAt = ref(0);
const productsLoadedAt = ref(0);
const productPageNum = ref(1);
const productTotal = ref(0);
const productsLoading = ref(false);
const productsFinished = ref(false);

const normalizeText = (value = '') => String(value).toLowerCase().replace(/\s+/g, '');

const pinyinCache = new Map();
const toPinyinCached = (value = '', options = {}) => {
  const text = String(value || '');
  const key = `${options.pattern || 'full'}:${text}`;
  if (!pinyinCache.has(key)) {
    pinyinCache.set(key, normalizeText(pinyin(text, { toneType: 'none', type: 'array', ...options }).join('')));
  }
  return pinyinCache.get(key);
};
const toPinyinFull = (value = '') => toPinyinCached(value);
const toPinyinInitials = (value = '') => toPinyinCached(value, { pattern: 'first' });

const fuzzyMatch = (text, search) => {
  const source = normalizeText(text);
  const target = normalizeText(search);
  if (!target) return true;
  return source.includes(target);
};

const matchesProductKeyword = (item, search) => {
  if (!search) {
    return true;
  }
  const candidates = [
    item.name,
    item.categoryName,
    toPinyinFull(item.name),
    toPinyinInitials(item.name),
    item.categoryName ? toPinyinFull(item.categoryName) : '',
    item.categoryName ? toPinyinInitials(item.categoryName) : '',
  ].filter(Boolean);
  return candidates.some((candidate) => fuzzyMatch(candidate, search));
};

const flatCategories = computed(() => {
  const result = [];
  const seenIds = new Set();
  const walk = (nodes = [], parentId = null) => {
    nodes.forEach((node) => {
      if (!seenIds.has(node.id)) {
        seenIds.add(node.id);
        result.push({ id: node.id, name: node.name, parentId });
      }
      if (node.children?.length) {
        walk(node.children, node.id);
      }
    });
  };
  walk(categories.value);
  return result;
});

const visibleCategories = computed(() => flatCategories.value.filter((item) => item.parentId === null));

const categoryMap = computed(() => new Map(flatCategories.value.map((item) => [item.id, item])));

const categoryDescendantsMap = computed(() => {
  const childrenMap = new Map();
  flatCategories.value.forEach((item) => {
    if (!childrenMap.has(item.parentId)) {
      childrenMap.set(item.parentId, []);
    }
    childrenMap.get(item.parentId).push(item.id);
  });

  const collect = (id) => {
    const directChildren = childrenMap.get(id) || [];
    const all = new Set([id]);
    directChildren.forEach((childId) => {
      collect(childId).forEach((descendantId) => all.add(descendantId));
    });
    return all;
  };

  const result = new Map();
  flatCategories.value.forEach((item) => {
    result.set(item.id, collect(item.id));
  });
  return result;
});

const isProductOnline = (item = {}) => String(item.status || 'ONLINE').toUpperCase() === 'ONLINE';

const filteredProducts = computed(() => {
  const search = keyword.value.trim();
  const categoryScope = activeCategoryId.value ? categoryDescendantsMap.value.get(activeCategoryId.value) : null;
  return products.value.filter((item) => {
    if (!isProductOnline(item)) {
      return false;
    }
    const matchKeyword = matchesProductKeyword(item, search);
    const matchCategory = !categoryScope || categoryScope.has(item.categoryId);
    return matchKeyword && matchCategory;
  });
});

const saveCategoriesCache = () => {
  try {
    localStorage.setItem(HOME_CATEGORIES_CACHE_KEY, JSON.stringify({
      ts: Date.now(),
      categories: categories.value,
    }));
  } catch {
  }
};

const saveProductsCache = () => {
  try {
    localStorage.setItem(HOME_PRODUCTS_CACHE_KEY, JSON.stringify({
      ts: Date.now(),
      products: products.value,
      total: productTotal.value,
      pageNum: productPageNum.value,
      finished: productsFinished.value,
    }));
  } catch {
  }
};

const loadCategoriesCache = () => {
  try {
    const raw = localStorage.getItem(HOME_CATEGORIES_CACHE_KEY);
    if (!raw) return false;
    const parsed = JSON.parse(raw);
    if (!parsed || Date.now() - Number(parsed.ts || 0) > HOME_CATEGORIES_CACHE_TTL) return false;
    categories.value = Array.isArray(parsed.categories) ? parsed.categories : [];
    categoriesLoadedAt.value = Number(parsed.ts || Date.now());
    return true;
  } catch {
    return false;
  }
};

const loadProductsCache = () => {
  try {
    const raw = localStorage.getItem(HOME_PRODUCTS_CACHE_KEY);
    if (!raw) return false;
    const parsed = JSON.parse(raw);
    if (!parsed || Date.now() - Number(parsed.ts || 0) > HOME_PRODUCTS_CACHE_TTL) return false;
    products.value = Array.isArray(parsed.products) ? parsed.products : [];
    productTotal.value = Number(parsed.total || products.value.length);
    productPageNum.value = Number(parsed.pageNum || Math.floor(products.value.length / PRODUCT_PAGE_SIZE) + 1);
    productsFinished.value = Boolean(parsed.finished || (productTotal.value > 0 && products.value.length >= productTotal.value));
    productsLoadedAt.value = Number(parsed.ts || Date.now());
    return true;
  } catch {
    return false;
  }
};

const normalizeProductsWithCategoryName = (list = []) => (list || []).map((item) => ({
  ...item,
  categoryName: categoryMap.value.get(item.categoryId)?.name || '',
}));

const loadCategories = async () => {
  const categoryRes = await fetchCategories();
  categories.value = categoryRes.data.data || [];
  categoriesLoadedAt.value = Date.now();
  saveCategoriesCache();
};

const loadProducts = async ({ reset = true } = {}) => {
  if (reset) {
    productPageNum.value = 1;
    productTotal.value = 0;
    productsFinished.value = false;
  }
  if (productsFinished.value && !reset) {
    productsLoading.value = false;
    return;
  }
  try {
    productsLoading.value = true;
    const pageToLoad = productPageNum.value;
    const productRes = await fetchProductPage({ page: pageToLoad, size: PRODUCT_PAGE_SIZE });
    const pageData = productRes.data.data || {};
    const nextRecords = normalizeProductsWithCategoryName(pageData.records || []);
    products.value = reset ? nextRecords : [...products.value, ...nextRecords];
    productTotal.value = Number(pageData.total || products.value.length);
    productPageNum.value = pageToLoad + 1;
    productsFinished.value = nextRecords.length < PRODUCT_PAGE_SIZE || products.value.length >= productTotal.value;
    productsLoadedAt.value = Date.now();
    saveProductsCache();
  } finally {
    productsLoading.value = false;
  }
};

const loadMoreProducts = async () => {
  await loadProducts({ reset: false });
};

const refreshSingleProductInCache = async (productId) => {
  if (!productId || !products.value.length) return false;
  try {
    const { fetchProductDetail } = await import('../api');
    const { data } = await fetchProductDetail(productId);
    const detail = data?.data;
    if (!detail?.id) return false;
    const idx = products.value.findIndex((item) => Number(item.id) === Number(detail.id));
    if (idx < 0) return false;
    const next = [...products.value];
    next[idx] = {
      ...next[idx],
      id: detail.id,
      name: detail.name,
      categoryId: detail.categoryId,
      mainImage: detail.mainImage,
      salePrice: detail.salePrice,
      originPrice: detail.originPrice,
      sales: detail.sales,
      categoryName: categoryMap.value.get(detail.categoryId)?.name || next[idx].categoryName || '',
    };
    products.value = next;
    productsLoadedAt.value = Date.now();
    saveProductsCache();
    return true;
  } catch {
    return false;
  }
};

const loadData = async ({ silent = false, forceCategories = false, forceProducts = false } = {}) => {
  try {
    const categoriesExpired = Date.now() - categoriesLoadedAt.value > HOME_CATEGORIES_CACHE_TTL;
    const productsExpired = Date.now() - productsLoadedAt.value > HOME_PRODUCTS_CACHE_TTL;
    const needCategories = forceCategories || categoriesExpired || !categories.value.length;
    const needProducts = forceProducts || productsExpired || !products.value.length;

    if (needCategories && needProducts) {
      await Promise.all([loadCategories(), loadProducts()]);
    } else {
      if (needCategories) {
        await loadCategories();
      }
      if (needProducts) {
        await loadProducts();
      } else if (needCategories) {
        products.value = normalizeProductsWithCategoryName(products.value);
      }
    }

    localStorage.removeItem(SALES_REFRESH_KEY);
  } catch (error) {
    if (!silent) {
      showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '首页数据加载失败');
    }
  }
};

const handleProfileAction = async () => {
  if (!await ensureAccessGate(router, '/profile', { force: true })) {
    return;
  }
  router.push('/profile');
};

const resetFilter = () => {
  activeCategoryId.value = null;
  keyword.value = '';
};

const goDetail = (id) => {
  router.push(`/products/${id}`);
};

onMounted(() => {
  if (userStore.isLogin && !userStore.profile) {
    userStore.ensureValidSession().catch(() => {
      userStore.logout();
    });
  }
  const hasCategoriesCache = loadCategoriesCache();
  const hasProductsCache = loadProductsCache();
  if (!hasCategoriesCache || !hasProductsCache) {
    loadData();
  }
});

onActivated(async () => {
  const shouldRefreshBySales = Boolean(localStorage.getItem(SALES_REFRESH_KEY));
  if (!shouldRefreshBySales) {
    return;
  }
  const lastProductId = Number(localStorage.getItem('mallfei:last-visited-product-id') || 0);
  const patched = await refreshSingleProductInCache(lastProductId);
  if (!patched) {
    await loadData({ silent: true, forceProducts: true });
  }
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 8px 0 82px;
  background:
    radial-gradient(circle at top left, rgba(129, 140, 248, 0.18), transparent 24%),
    radial-gradient(circle at top right, rgba(236, 72, 153, 0.1), transparent 22%),
    linear-gradient(180deg, #edf3ff 0%, #f7f9ff 100%);
}

.hero {
  margin: 8px 12px 10px;
  padding: 16px 16px 14px;
  background: rgba(255, 255, 255, 0.78);
  color: #2d3a64;
  border: 1px solid rgba(255, 255, 255, 0.88);
  border-radius: 24px;
  box-shadow: 0 14px 36px rgba(108, 123, 225, 0.12);
  backdrop-filter: blur(18px);
}

.hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.hero-title {
  font-size: 22px;
  line-height: 1.18;
  font-weight: 800;
  letter-spacing: 0.01em;
}

.hero-subtitle {
  margin-top: 5px;
  color: #94a3b8;
  font-size: 12px;
}

:deep(.hero-action-btn) {
  height: 30px;
  padding: 0 12px;
  color: #4f46e5;
  border: none;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.14) 0%, rgba(59, 130, 246, 0.12) 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.75);
}

:deep(.hero-action-btn .van-button__text) {
  color: #4f46e5;
  font-size: 12px;
  font-weight: 700;
}

.section-card {
  margin: 10px 12px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(255, 255, 255, 0.88);
  border-radius: 24px;
  box-shadow: 0 14px 36px rgba(108, 123, 225, 0.1);
  backdrop-filter: blur(18px);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-title {
  font-size: 16px;
  line-height: 1.25;
  font-weight: 800;
  color: #2d3a64;
}

.section-link,
.section-desc {
  font-size: 11px;
  color: #94a3b8;
}

.section-link {
  font-weight: 700;
  color: #4f46e5;
}

.category-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

:deep(.van-tag) {
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(248, 251, 255, 0.86);
  font-size: 12px;
  font-weight: 700;
  line-height: 18px;
}

.user-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.summary-item {
  min-width: 0;
  padding: 10px;
  border-radius: 18px;
  background: linear-gradient(135deg, #f8fbff 0%, #eef3ff 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.75);
}

.summary-label {
  font-size: 11px;
  color: #94a3b8;
}

.summary-value {
  margin-top: 6px;
  font-size: 14px;
  font-weight: 800;
  color: #2d3a64;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-section {
  padding: 0 12px 12px;
}

.products-header {
  margin: 4px 2px 10px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.product-card {
  overflow: hidden;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.84);
  border: 1px solid rgba(255, 255, 255, 0.88);
  box-shadow: 0 14px 34px rgba(108, 123, 225, 0.1);
  backdrop-filter: blur(18px);
}

.product-image {
  display: block;
  width: 100%;
  height: 132px;
  object-fit: cover;
  background: linear-gradient(135deg, #eef2ff 0%, #dbeafe 100%);
}

.product-body {
  padding: 10px;
}

.product-name {
  min-height: 38px;
  font-size: 13px;
  font-weight: 800;
  color: #2d3a64;
  line-height: 1.45;
}

.product-sales {
  margin-top: 5px;
  color: #94a3b8;
  font-size: 11px;
}

.product-price-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin: 8px 0;
}

.price {
  color: #f43f5e;
  font-size: 17px;
  font-weight: 800;
}

.origin {
  color: #94a3b8;
  font-size: 11px;
  text-decoration: line-through;
}

:deep(.product-body .van-button) {
  height: 30px;
  font-size: 12px;
}
</style>
