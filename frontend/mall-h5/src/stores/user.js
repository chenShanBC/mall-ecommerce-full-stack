import { defineStore } from 'pinia';
import {
  changeCurrentUserPassword,
  fetchCurrentUser,
  loginByPassword,
  loginBySmsCode,
  logoutUser,
  registerUser,
  updateCurrentUserProfile,
} from '../api';

const hasUserIdentity = (profile) => Boolean(
  profile
  && typeof profile === 'object'
  && (profile.id || profile.userId || profile.loginId || profile.mobile || profile.nickname),
);

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('mall-h5-token') || '',
    profile: null,
    profileLoaded: false,
    sessionCheckPromise: null,
  }),
  getters: {
    isLogin: (state) => Boolean(state.token?.trim()),
    hasValidSession: (state) => Boolean(state.token?.trim() && state.profileLoaded && hasUserIdentity(state.profile)),
    displayName: (state) => state.profile?.nickname || state.profile?.loginId || '访客',
    maskedMobile: (state) => {
      const mobile = state.profile?.mobile || '';
      return /^1\d{10}$/.test(mobile) ? `${mobile.slice(0, 3)}****${mobile.slice(7)}` : mobile || '--';
    },
  },
  actions: {
    async login(form) {
      const { data } = await loginByPassword(form);
      this.token = data.data.token;
      this.profile = data.data;
      this.profileLoaded = true;
      localStorage.setItem('mall-h5-token', this.token);
      sessionStorage.removeItem('mall-h5-token');
      return data.data;
    },
    async loginWithSms(form) {
      const { data } = await loginBySmsCode(form);
      this.token = data.data.token;
      this.profile = data.data;
      this.profileLoaded = true;
      localStorage.setItem('mall-h5-token', this.token);
      sessionStorage.removeItem('mall-h5-token');
      return data.data;
    },
    async register(form) {
      const { data } = await registerUser(form);
      this.token = data.data.token;
      this.profile = data.data;
      this.profileLoaded = true;
      localStorage.setItem('mall-h5-token', this.token);
      sessionStorage.removeItem('mall-h5-token');
      return data.data;
    },
    async loadProfile() {
      if (!this.token?.trim()) {
        this.profile = null;
        this.profileLoaded = false;
        return null;
      }
      const { data } = await fetchCurrentUser();
      const profile = data?.data;

      if (!hasUserIdentity(profile)) {
        this.profile = null;
        this.profileLoaded = false;
        throw new Error('当前会话未获取到有效用户信息');
      }

      this.profile = profile;
      this.profileLoaded = true;
      return profile;
    },
    async ensureValidSession(force = false) {
      if (!this.token?.trim()) {
        this.clearSession();
        return false;
      }
      if (!force && this.profileLoaded && hasUserIdentity(this.profile)) {
        return true;
      }
      if (this.sessionCheckPromise) {
        return this.sessionCheckPromise;
      }

      this.sessionCheckPromise = this.loadProfile()
        .then(() => true)
        .catch(() => {
          this.clearSession();
          return false;
        })
        .finally(() => {
          this.sessionCheckPromise = null;
        });

      return this.sessionCheckPromise;
    },
    async updateProfile(form) {
      const { data } = await updateCurrentUserProfile(form);
      this.profile = data.data;
      this.profileLoaded = true;
      return data.data;
    },
    async changePassword(form) {
      const { data } = await changeCurrentUserPassword(form);
      return data.data;
    },
    clearSession() {
      this.token = '';
      this.profile = null;
      this.profileLoaded = false;
      this.sessionCheckPromise = null;
      localStorage.removeItem('mall-h5-token');
      sessionStorage.removeItem('mall-h5-token');
    },
    async logout() {
      if (this.token) {
        try {
          await logoutUser();
        } catch {
        }
      }
      this.clearSession();
    },
  },
});
