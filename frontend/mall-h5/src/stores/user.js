import { defineStore } from 'pinia';
import { fetchCurrentUser, loginByPassword } from '../api';

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('mall-h5-token') || '',
    profile: null,
  }),
  actions: {
    async login(form) {
      const { data } = await loginByPassword(form);
      this.token = data.data.token;
      this.profile = data.data;
      localStorage.setItem('mall-h5-token', this.token);
      return data.data;
    },
    async loadProfile() {
      if (!this.token) {
        return null;
      }
      const { data } = await fetchCurrentUser();
      this.profile = data.data;
      return data.data;
    },
    logout() {
      this.token = '';
      this.profile = null;
      localStorage.removeItem('mall-h5-token');
    },
  },
});
