import { defineStore } from 'pinia';
import { adminLogin, fetchCurrentAdmin, fetchDashboard, logoutAdmin } from '../api';

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
      return this.permissions.includes(permission);
    },
    getFirstAccessiblePath() {
      const entries = [
        { permission: 'product:view', path: '/products' },
        { permission: 'order:view', path: '/orders' },
        { permission: 'stock:view', path: '/stocks' },
        { permission: 'pay:view', path: '/pays' },
        { permission: 'reconcile:view', path: '/reconciliations' },
        { permission: 'user:view', path: '/users' },
        { permission: 'system:account:manage', path: '/accounts' },
        { permission: 'system:log:view', path: '/operation-logs' },
      ];
      return entries.find((item) => this.hasPermission(item.permission))?.path || '/dashboard';
    },
    persistProfile() {
      if (this.profile) {
        localStorage.setItem('mall-admin-profile', JSON.stringify(this.profile));
      } else {
        localStorage.removeItem('mall-admin-profile');
      }
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
