<template>
  <div class="app-shell">
    <router-view v-slot="{ Component, route }">
      <keep-alive :include="cachedRouteNames">
        <component :is="Component" :key="route.name" />
      </keep-alive>
    </router-view>
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

const cachedRouteNames = ['home', 'cart', 'orders', 'profile', 'profile-manage', 'address', 'product-detail', 'checkout', 'order-detail', 'pay-return'];
const showTabbar = computed(() => Boolean(route.meta.showTabbar));
const activeTab = computed(() => route.name || 'home');

const handleTabChange = async (name) => {
  const target = tabs.find((item) => item.name === name);
  if (!target) {
    return;
  }
  if (target.requiresAuth && !await requireLogin(router, target.path, { force: false })) {
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
  background:
    radial-gradient(circle at top left, rgba(129, 140, 248, 0.22), transparent 28%),
    radial-gradient(circle at top right, rgba(236, 72, 153, 0.14), transparent 24%),
    linear-gradient(180deg, #edf3ff 0%, #f7f9ff 100%);
}

:deep(.van-tabbar) {
  left: 12px;
  right: 12px;
  bottom: 10px;
  width: auto;
  height: 54px;
  padding: 5px 8px calc(5px + env(safe-area-inset-bottom));
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(18px);
  border: 1px solid rgba(255, 255, 255, 0.88);
  box-shadow: 0 14px 36px rgba(108, 123, 225, 0.16);
  overflow: hidden;
}

:deep(.van-tabbar::after) {
  display: none;
}

:deep(.van-tabbar-item) {
  color: #94a3b8;
  border-radius: 16px;
  font-size: 11px;
  font-weight: 600;
}

:deep(.van-tabbar-item .van-icon) {
  font-size: 20px;
  margin-bottom: 2px;
}

:deep(.van-tabbar-item--active) {
  color: #4f46e5;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.16) 0%, rgba(59, 130, 246, 0.14) 100%);
}
</style>

<style>
:root {
  color-scheme: light;
  --van-primary-color: #4f46e5;
  --van-blue: #4f46e5;
  --van-red: #f43f5e;
  --van-background: #f7f9ff;
  --van-background-2: rgba(255, 255, 255, 0.86);
  --van-text-color: #334155;
  --van-text-color-2: #64748b;
  --van-border-color: rgba(148, 163, 184, 0.16);
  --van-radius-md: 18px;
  --van-radius-lg: 24px;
  --van-button-primary-background: linear-gradient(135deg, #5b6cff 0%, #7c8dff 100%);
  --van-button-primary-border-color: transparent;
}

body {
  margin: 0;
  color: #334155;
  background: linear-gradient(180deg, #edf3ff 0%, #f7f9ff 100%);
  font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif;
}

* {
  box-sizing: border-box;
}

#app {
  min-height: 100vh;
}

.van-nav-bar {
  margin: 12px 12px 0;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.84);
  backdrop-filter: blur(18px);
  border: 1px solid rgba(255, 255, 255, 0.88);
  box-shadow: 0 16px 40px rgba(108, 123, 225, 0.12);
}

.van-nav-bar::after {
  display: none;
}

.van-nav-bar__title {
  color: #2d3a64;
  font-weight: 800;
}

.van-button--round {
  box-shadow: 0 14px 28px rgba(99, 102, 241, 0.18);
}

.van-search {
  border-radius: 999px;
  padding: 6px;
  background: rgba(255, 255, 255, 0.18) !important;
  border: 1px solid rgba(255, 255, 255, 0.28);
}

.van-search__content {
  height: 36px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.75);
}

.van-search .van-field__control {
  font-size: 13px;
}
</style>
