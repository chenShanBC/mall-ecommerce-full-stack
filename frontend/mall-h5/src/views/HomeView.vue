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
          v-for="item in flatCategories"
          :key="item.id"
          plain
          round
          size="large"
          :color="activeCategoryId === item.id ? '#1989fa' : '#969799'"
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

      <van-empty v-if="!filteredProducts.length" description="暂无匹配商品" />

      <div v-else class="product-grid">
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
    </div>

    <FloatingCartButton />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { showFailToast } from 'vant';
import { pinyin } from 'pinyin-pro';
import { fetchCategories, fetchProductPage } from '../api';
import FloatingCartButton from '../components/FloatingCartButton.vue';
import { useUserStore } from '../stores/user';
import { formatPrice } from '../utils/format';
import { getProductImage } from '../utils/productVisual';
import { requireLogin } from '../utils/requireLogin';

const SALES_REFRESH_KEY = 'mallfei:product-sales-refresh';

const router = useRouter();
const userStore = useUserStore();
const keyword = ref('');
const categories = ref([]);
const products = ref([]);
const activeCategoryId = ref(null);

const normalizeText = (value = '') => String(value).toLowerCase().replace(/\s+/g, '');

const toPinyinFull = (value = '') => normalizeText(pinyin(String(value), { toneType: 'none', type: 'array' }).join(''));
const toPinyinInitials = (value = '') => normalizeText(pinyin(String(value), { pattern: 'first', toneType: 'none', type: 'array' }).join(''));

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
  const walk = (nodes = [], parentId = null) => {
    nodes.forEach((node) => {
      result.push({ id: node.id, name: node.name, parentId });
      if (node.children?.length) {
        walk(node.children, node.id);
      }
    });
  };
  walk(categories.value);
  return result;
});

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

const filteredProducts = computed(() => {
  const search = keyword.value.trim();
  const categoryScope = activeCategoryId.value ? categoryDescendantsMap.value.get(activeCategoryId.value) : null;
  return products.value.filter((item) => {
    const matchKeyword = matchesProductKeyword(item, search);
    const matchCategory = !categoryScope || categoryScope.has(item.categoryId);
    return matchKeyword && matchCategory;
  });
});

const loadData = async () => {
  try {
    const [categoryRes, productRes] = await Promise.all([fetchCategories(), fetchProductPage()]);
    categories.value = categoryRes.data.data || [];
    products.value = (productRes.data.data?.records || []).map((item) => ({
      ...item,
      categoryName: categoryMap.value.get(item.categoryId)?.name || '',
    }));
    localStorage.removeItem(SALES_REFRESH_KEY);
  } catch (error) {
    showFailToast(error?.response?.data?.msg || error?.response?.data?.message || '首页数据加载失败');
  }
};

const handleProfileAction = async () => {
  if (!await requireLogin(router, '/profile')) {
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

onMounted(async () => {
  if (userStore.isLogin && !userStore.profile) {
    try {
      await userStore.ensureValidSession();
    } catch {
      await userStore.logout();
    }
  }
  await loadData();
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 70px;
  background: linear-gradient(180deg, #eff6ff 0%, #f6f8fb 220px);
}

.hero {
  padding: 16px;
  background: linear-gradient(135deg, #2563eb, #4f46e5);
  color: #fff;
  border-radius: 0 0 24px 24px;
}

.hero-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.hero-title {
  font-size: 24px;
  font-weight: 700;
}

.hero-subtitle {
  margin-top: 6px;
  font-size: 13px;
  opacity: 0.9;
}

:deep(.hero-action-btn) {
  color: #ffffff;
  border-color: rgba(255, 255, 255, 0.92);
  background: rgba(255, 255, 255, 0.12);
}

:deep(.hero-action-btn .van-button__text) {
  color: #ffffff;
  font-weight: 600;
}

.section-card {
  margin: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 20px;
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(14px);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}

.section-link,
.section-desc {
  font-size: 12px;
  color: #64748b;
}

.category-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 14px;
}

.user-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.summary-item {
  padding: 14px 12px;
  border-radius: 18px;
  background: linear-gradient(180deg, #f8fafc, #eef6ff);
}

.summary-label {
  font-size: 12px;
  color: #64748b;
}

.summary-value {
  margin-top: 10px;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.product-section {
  padding: 0 12px 12px;
}

.products-header {
  margin-bottom: 12px;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.product-card {
  overflow: hidden;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.08);
}

.product-image {
  display: block;
  width: 100%;
  height: 180px;
  object-fit: cover;
  background: #eff6ff;
}

.product-body {
  padding: 14px;
}

.product-name {
  min-height: 44px;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.5;
}

.product-sales {
  margin-top: 8px;
  color: #64748b;
  font-size: 12px;
}

.product-price-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin: 12px 0;
}

.price {
  color: #ee0a24;
  font-size: 20px;
  font-weight: 700;
}

.origin {
  color: #94a3b8;
  font-size: 12px;
  text-decoration: line-through;
}
</style>
