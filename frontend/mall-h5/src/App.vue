<template>
  <div class="app-shell">
    <router-view />
    <van-tabbar
      v-if="showTabbar"
      :model-value="activeTab"
      safe-area-inset-bottom
      @change="handleTabChange"
    >
      <van-tabbar-item v-for="item in tabs" :key="item.name" :name="item.name" :icon="item.icon">
        {{ item.label }}
      </van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { requireLogin } from './utils/requireLogin';

const route = useRoute();
const router = useRouter();

const tabs = [
  { name: 'home', path: '/home', icon: 'home-o', label: '首页' },
  { name: 'cart', path: '/cart', icon: 'shopping-cart-o', label: '购物车', requiresAuth: true },
  { name: 'orders', path: '/orders', icon: 'orders-o', label: '订单', requiresAuth: true },
  { name: 'profile', path: '/profile', icon: 'contact-o', label: '我的', requiresAuth: true },
];

const showTabbar = computed(() => Boolean(route.meta.showTabbar));
const activeTab = computed(() => route.name || 'home');

const handleTabChange = async (name) => {
  const target = tabs.find((item) => item.name === name);
  if (!target) {
    return;
  }
  if (target.requiresAuth && !await requireLogin(router, target.path)) {
    return;
  }
  if (route.path !== target.path) {
    router.replace(target.path);
  }
};
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: #f6f8fb;
}
</style>
