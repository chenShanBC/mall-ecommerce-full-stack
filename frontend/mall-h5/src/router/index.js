import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import LoginView from '../views/LoginView.vue';
import ProductDetailView from '../views/ProductDetailView.vue';
import CheckoutView from '../views/CheckoutView.vue';
import CartView from '../views/CartView.vue';
import OrdersView from '../views/OrdersView.vue';
import OrderDetailView from '../views/OrderDetailView.vue';
import PayReturnView from '../views/PayReturnView.vue';
import ProfileView from '../views/ProfileView.vue';
import ProfileManageView from '../views/ProfileManageView.vue';
import AddressView from '../views/AddressView.vue';
import AlipayLandingView from '../views/AlipayLandingView.vue';
import { useUserStore } from '../stores/user';
import { FORCE_LOGOUT_GUEST_REFRESH_KEY } from '../utils/wsForceLogout';

const routes = [
  {
    path: '/',
    redirect: '/home',
  },
  {
    path: '/home',
    name: 'home',
    component: HomeView,
    meta: { showTabbar: true, title: '首页' },
  },
  {
    path: '/cart',
    name: 'cart',
    component: CartView,
    meta: { showTabbar: true, title: '购物车', requiresAuth: true },
  },
  {
    path: '/orders',
    name: 'orders',
    component: OrdersView,
    meta: { showTabbar: true, title: '订单', requiresAuth: true },
  },
  {
    path: '/profile',
    name: 'profile',
    component: ProfileView,
    meta: { showTabbar: true, title: '我的', requiresAuth: true },
  },
  {
    path: '/profile/manage',
    name: 'profile-manage',
    component: ProfileManageView,
    meta: { title: '个人信息管理', requiresAuth: true },
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { title: '登录注册' },
  },
  {
    path: '/alipay-landing',
    name: 'alipay-landing',
    component: AlipayLandingView,
    meta: { title: '支付宝登录处理中' },
  },
  {
    path: '/products/:id',
    name: 'product-detail',
    component: ProductDetailView,
    meta: { title: '商品详情' },
  },
  {
    path: '/checkout',
    name: 'checkout',
    component: CheckoutView,
    meta: { title: '确认订单', requiresAuth: true },
  },
  {
    path: '/orders/:id',
    name: 'order-detail',
    component: OrderDetailView,
    meta: { title: '订单详情', requiresAuth: true },
  },
  {
    path: '/pay/return',
    name: 'pay-return',
    component: PayReturnView,
    meta: { title: '支付结果' },
  },
  {
    path: '/address',
    name: 'address',
    component: AddressView,
    meta: { title: '收货地址', requiresAuth: true },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach((to) => {
  const userStore = useUserStore();
  const forceLogoutGuestRefreshing = sessionStorage.getItem(FORCE_LOGOUT_GUEST_REFRESH_KEY) === '1';

  if (forceLogoutGuestRefreshing) {
    sessionStorage.removeItem(FORCE_LOGOUT_GUEST_REFRESH_KEY);
    if (to.meta.requiresAuth) {
      return '/home';
    }
  }

  if (to.meta.requiresAuth && !userStore.token?.trim()) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    };
  }

  if (to.path === '/login' && userStore.token?.trim()) {
    return '/home';
  }

  return true;
});

export default router;
