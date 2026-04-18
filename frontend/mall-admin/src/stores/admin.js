import { defineStore } from 'pinia';
import { adminLogin, fetchDashboard } from '../api';

export const useAdminStore = defineStore('admin', {
  state: () => ({
    token: localStorage.getItem('mall-admin-token') || '',
    profile: null,
    dashboard: null,
  }),
  actions: {
    async login(form) {
      const { data } = await adminLogin(form);
      this.token = data.data.token;
      this.profile = data.data;
      localStorage.setItem('mall-admin-token', this.token);
      return data.data;
    },
    async loadDashboard() {
      const { data } = await fetchDashboard();
      this.dashboard = data.data;
      return data.data;
    },
    logout() {
      this.token = '';
      this.profile = null;
      this.dashboard = null;
      localStorage.removeItem('mall-admin-token');
    },
  },
});
