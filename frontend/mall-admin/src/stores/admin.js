import { defineStore } from 'pinia';
import { adminLogin, fetchCurrentAdmin, fetchDashboard, logoutAdmin } from '../api';
import { destroyAdminForceLogoutSocket, initAdminForceLogoutSocket } from '../utils/adminForceLogout';

export const useAdminStore = defineStore('admin', {
  state: () => ({
    token: localStorage.getItem('mall-admin-token') || '',
    profile: JSON.parse(localStorage.getItem('mall-admin-profile') || 'null'),
    dashboard: null,
    profileLoaded: Boolean(localStorage.getItem('mall-admin-profile')),
    profileLoadingPromise: null,
    sessionCheckPromise: null,
  }),
  getters: {
    isLogin: (state) => Boolean(state.token),
    permissions: (state) => state.profile?.permissions || [],
    roleCode: (state) => state.profile?.roleCode || '',
    adminId: (state) => state.profile?.adminId || state.profile?.principalId || null,
  },
  actions: {
    hasPermission(permission) {
      return this.roleCode === 'SUPER_ADMIN' || this.permissions.includes(permission);
    },
    hasAnyPermission(permissions) {
      return this.roleCode === 'SUPER_ADMIN' || permissions.some((permission) => this.hasPermission(permission));
    },
    getFirstAccessiblePath() {
      const entries = [
        { permission: 'dashboard:view', path: '/dashboard' },
        { permission: 'product:view', path: '/products' },
        { permission: 'order:view', path: '/orders' },
        { permission: 'stock:view', path: '/stocks' },
        { permission: 'payment:view', path: '/pays' },
        { permission: 'reconciliation:view', path: '/reconciliations' },
        { permission: 'stock:reconcile:view', path: '/reconciliations?type=stock' },
        { permission: 'user:view', path: '/users' },
        { permission: 'admin:view', path: '/accounts' },
        { permission: 'log:operation:view', path: '/operation-logs' },
      ];
      return entries.find((item) => this.hasPermission(item.permission))?.path || '/profile';
    },
    persistProfile() {
      if (this.profile) {
        localStorage.setItem('mall-admin-profile', JSON.stringify(this.profile));
      } else {
        localStorage.removeItem('mall-admin-profile');
      }
    },
    initForceLogout(router) {
      if (!router) return;
      initAdminForceLogoutSocket(this, router);
    },
    clearProductSalesThresholdSession() {
      const adminId = this.adminId || 'anonymous';
      sessionStorage.removeItem(`mallfei-admin-product-sales-threshold-session:${adminId}`);
    },
    clearLocalSession() {
      this.clearProductSalesThresholdSession();
      this.clearLocalSession();
    },
    async login(form) {
      const { data } = await adminLogin(form);
      this.token = data.data.token;
      this.profile = data.data;
      this.profileLoaded = true;
      this.profileLoadingPromise = null;
      this.sessionCheckPromise = null;
      localStorage.setItem('mall-admin-token', this.token);
      this.persistProfile();
      this.initForceLogout(window.__mallAdminRouter);
      return data.data;
    },
    async loadProfile(force = false) {
      if (!this.token) {
        return null;
      }
      if (!force && this.profileLoaded && this.profile) {
        return this.profile;
      }
      if (!force && this.profileLoadingPromise) {
        return this.profileLoadingPromise;
      }
      this.profileLoadingPromise = fetchCurrentAdmin()
        .then(({ data }) => {
          this.profile = data.data;
          this.profileLoaded = true;
          this.persistProfile();
          return data.data;
        })
        .finally(() => {
          this.profileLoadingPromise = null;
        });
      return this.profileLoadingPromise;
    },
    async ensureSessionValid(force = false) {
      if (!this.token) {
        return false;
      }
      if (!force && this.sessionCheckPromise) {
        return this.sessionCheckPromise;
      }
      this.sessionCheckPromise = fetchCurrentAdmin()
        .then(({ data }) => {
          this.profile = data.data;
          this.profileLoaded = true;
          this.persistProfile();
          return true;
        })
        .catch(async () => {
          await this.logout();
          return false;
        })
        .finally(() => {
          this.sessionCheckPromise = null;
        });
      return this.sessionCheckPromise;
    },
    async refreshProfile() {
      return this.loadProfile(true);
    },
    applyProfilePatch(patch) {
      this.profile = { ...(this.profile || {}), ...patch };
      this.profileLoaded = true;
      this.persistProfile();
    },
    async loadDashboard() {
      const { data } = await fetchDashboard();
      this.dashboard = data.data;
      return data.data;
    },
    async logout() {
      if (this.token) {
        try {
          await logoutAdmin();
        } catch (error) {
          console.warn('admin logout failed', error);
        }
      }
      this.token = '';
      this.profile = null;
      this.dashboard = null;
      this.profileLoaded = false;
      this.profileLoadingPromise = null;
      this.sessionCheckPromise = null;
      localStorage.removeItem('mall-admin-token');
      localStorage.removeItem('mall-admin-profile');
    },
  },
});
