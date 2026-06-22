import { createRouter, createWebHistory } from 'vue-router';
import { useAdminStore } from '../stores/admin';

const DashboardView = () => import('../views/DashboardView.vue');
const LoginView = () => import('../views/LoginView.vue');
const StockManageView = () => import('../views/StockManageView.vue');
const StockLogView = () => import('../views/StockLogView.vue');
const ProductManageView = () => import('../views/ProductManageView.vue');
const AccountManageView = () => import('../views/AccountManageView.vue');
const OperationLogView = () => import('../views/OperationLogView.vue');
const OrderManageView = () => import('../views/OrderManageView.vue');
const PayManageView = () => import('../views/PayManageView.vue');
const AftersaleManageView = () => import('../views/AftersaleManageView.vue');
const ReconciliationManageView = () => import('../views/ReconciliationManageView.vue');
const ProfileCenterView = () => import('../views/ProfileCenterView.vue');
const UserManageView = () => import('../views/UserManageView.vue');

const routes = [
  { path: '/', redirect: '/dashboard' },
  { path: '/dashboard', name: 'dashboard', component: DashboardView, meta: { requiresAuth: true, permission: 'dashboard:view' } },
  { path: '/orders', name: 'orders', component: OrderManageView, meta: { requiresAuth: true, permission: 'order:view' } },
  { path: '/aftersales', name: 'aftersales', component: AftersaleManageView, meta: { requiresAuth: true, permission: 'aftersale:view' } },
  { path: '/pays', name: 'pays', component: PayManageView, meta: { requiresAuth: true, permission: 'payment:view' } },
  { path: '/reconciliations', name: 'reconciliations', component: ReconciliationManageView, meta: { requiresAuth: true, permissions: ['reconciliation:view', 'stock:reconcile:view'] } },
  { path: '/products', name: 'products', component: ProductManageView, meta: { requiresAuth: true, permissions: ['product:view', 'category:view', 'category:manage'] } },
  { path: '/stocks', name: 'stocks', component: StockManageView, meta: { requiresAuth: true, permission: 'stock:view' } },
  { path: '/users', name: 'users', component: UserManageView, meta: { requiresAuth: true, permission: 'user:view' } },

  { path: '/stock-logs', name: 'stock-logs', component: StockLogView, meta: { requiresAuth: true, permission: 'stock:log:view' } },
  { path: '/accounts', name: 'accounts', component: AccountManageView, meta: { requiresAuth: true, permissions: ['admin:view', 'role:view', 'permission:view'] } },
  { path: '/operation-logs', name: 'operation-logs', component: OperationLogView, meta: { requiresAuth: true, permission: 'log:operation:view' } },
  { path: '/profile', name: 'profile', component: ProfileCenterView, meta: { requiresAuth: true } },
  { path: '/login', name: 'login', component: LoginView },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
});

router.beforeEach(async (to) => {
  const adminStore = useAdminStore();
  const token = localStorage.getItem('mall-admin-token');
  if (to.meta.requiresAuth && !token) {
    await adminStore.logout();
    return { path: '/login', query: { redirect: to.fullPath } };
  }
  if (token && to.meta.requiresAuth) {
    const valid = await adminStore.ensureSessionValid();
    if (!valid) {
      return { path: '/login', query: { redirect: to.fullPath } };
    }
    adminStore.initForceLogout(router);
  }
  if (to.meta.permission && !adminStore.hasPermission(to.meta.permission)) return adminStore.getFirstAccessiblePath();
  if (to.meta.permissions && !adminStore.hasAnyPermission(to.meta.permissions)) return adminStore.getFirstAccessiblePath();
  if (to.path === '/login' && token) {
    const valid = await adminStore.ensureSessionValid();
    return valid ? adminStore.getFirstAccessiblePath() : true;
  }
  return true;
});

export default router;
