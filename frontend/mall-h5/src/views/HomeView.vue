<template>
  <div class="page">
    <van-nav-bar title="mall-h5 首页" />
    <div class="content">
      <van-card>
        <template #title>
          <div class="header-row">
            <span>B2C 电商 MVP 用户端</span>
            <van-button size="small" type="primary" plain @click="goLogin">登录</van-button>
          </div>
        </template>
        <template #desc>
          当前为 H5 首页第一版，已接入后端商品列表接口。
        </template>
      </van-card>

      <div class="section-title">商品列表</div>

      <van-empty v-if="!products.length" description="暂无商品" />
      <van-card
        v-for="item in products"
        :key="item.id"
        :title="item.name"
        :desc="`销量 ${item.sales}`"
        :thumb="item.mainImage"
        :price="String(item.salePrice)"
        :origin-price="String(item.originPrice)"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { showFailToast } from 'vant';
import { fetchProductPage } from '../api';

const router = useRouter();
const products = ref([]);

const loadProducts = async () => {
  try {
    const { data } = await fetchProductPage();
    products.value = data.data.list || [];
  } catch (error) {
    showFailToast(error?.response?.data?.msg || '加载商品失败');
  }
};

const goLogin = () => {
  router.push('/login');
};

onMounted(loadProducts);
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: #f7f8fa;
}

.content {
  padding: 12px;
}

.header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.section-title {
  margin: 16px 4px 12px;
  font-size: 16px;
  font-weight: 600;
}
</style>
