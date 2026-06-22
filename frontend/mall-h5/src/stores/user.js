import { defineStore } from 'pinia';
import {
  bindCurrentUserMobile,
  changeCurrentUserPassword,
  fetchCurrentUser,
  loginByPassword,
  loginBySmsCode,
  logoutUser,
  registerUser,
  updateCurrentUserProfile,
} from '../api';
import { destroyForceLogoutSocket, initForceLogoutSocket } from '../utils/wsForceLogout';

const hasUserIdentity = (profile) => Boolean(
  profile
  && typeof profile === 'object'
  && (profile.id || profile.userId || profile.loginId || profile.mobile || profile.nickname),
);

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('mall-h5-token') || '',
    userId: Number(localStorage.getItem('mall-h5-user-id') || 0) || null,
    profile: null,
    profileLoaded: false,
    sessionCheckPromise: null,
    lastSessionInvalidReason: '',
    sessionCheckedAt: 0,
    sessionCacheTtl: 60 * 1000,
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
      this.userId = data.data.id || data.data.userId || this.userId;
      this.profile = data.data;
      this.profileLoaded = true;
      this.lastSessionInvalidReason = '';
      localStorage.setItem('mall-h5-token', this.token);
      if (this.userId) localStorage.setItem('mall-h5-user-id', String(this.userId));
      sessionStorage.removeItem('mall-h5-token');
      initForceLogoutSocket(this);
      return data.data;
    },
    async loginWithSms(form) {
      const { data } = await loginBySmsCode(form);
      this.token = data.data.token;
      this.userId = data.data.id || data.data.userId || this.userId;
      this.profile = data.data;
      this.profileLoaded = true;
      this.lastSessionInvalidReason = '';
      localStorage.setItem('mall-h5-token', this.token);
      if (this.userId) localStorage.setItem('mall-h5-user-id', String(this.userId));
      sessionStorage.removeItem('mall-h5-token');
      return data.data;
    },
    async register(form) {
      const { data } = await registerUser(form);
      this.token = data.data.token;
      this.userId = data.data.id || data.data.userId || this.userId;
      this.profile = data.data;
      this.profileLoaded = true;
      this.lastSessionInvalidReason = '';
      localStorage.setItem('mall-h5-token', this.token);
      if (this.userId) localStorage.setItem('mall-h5-user-id', String(this.userId));
      sessionStorage.removeItem('mall-h5-token');
      return data.data;
    },
    async loadProfile(force = false) {
      if (!this.token?.trim()) {
        this.profile = null;
        this.profileLoaded = false;
        this.sessionCheckedAt = 0;
        return null;
      }

      const now = Date.now();
      if (!force && this.profileLoaded && hasUserIdentity(this.profile) && now - this.sessionCheckedAt < this.sessionCacheTtl) {
        return this.profile;
      }

      const { data } = await fetchCurrentUser();
      const profile = data?.data;

      if (!hasUserIdentity(profile)) {
        this.profile = null;
        this.profileLoaded = false;
        this.sessionCheckedAt = 0;
        throw new Error('当前会话未获取到有效用户信息');
      }

      this.profile = profile;
      this.profileLoaded = true;
      this.sessionCheckedAt = now;
      return profile;
    },
    async ensureValidSession(force = false) {
      if (!this.token?.trim()) {
        this.clearSession();
        this.lastSessionInvalidReason = '';
        return false;
      }
      if (!force && this.profileLoaded && hasUserIdentity(this.profile) && Date.now() - this.sessionCheckedAt < this.sessionCacheTtl) {
        return true;
      }
      if (this.sessionCheckPromise) {
        return this.sessionCheckPromise;
      }

      this.sessionCheckPromise = this.loadProfile(force)
        .then(() => {
          this.lastSessionInvalidReason = '';
          return true;
        })
        .catch((error) => {
          const message = error?.response?.data?.message || error?.response?.data?.msg || '';
          this.lastSessionInvalidReason = String(message).includes('禁用') ? 'DISABLED' : 'INVALID';
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
    async bindMobile(form) {
      const { data } = await bindCurrentUserMobile(form);
      this.profile = data.data;
      this.profileLoaded = true;
      return data.data;
    },
    clearSession() {
      this.token = '';
      this.userId = null;
      this.profile = null;
      this.profileLoaded = false;
      this.sessionCheckPromise = null;
      this.lastSessionInvalidReason = '';
      
      localStorage.removeItem('mall-h5-token');
      localStorage.removeItem('mall-h5-user-id');
      localStorage.removeItem('mallfei:h5-home-categories-cache-v1');
      localStorage.removeItem('mallfei:h5-home-products-cache-v1');
      localStorage.removeItem('mallfei:product-sales-refresh');
      localStorage.removeItem('mallfei:last-visited-product-id');
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
      destroyForceLogoutSocket();
    },
  },
});
