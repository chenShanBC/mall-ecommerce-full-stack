import axios from 'axios';
import { showFailToast } from 'vant';

const DISABLED_USER_MESSAGE = '该用户已禁用，详情可咨询平台/客服';
let disabledRedirecting = false;

const clearAuthTokens = () => {
  localStorage.removeItem('mall-h5-token');
  localStorage.removeItem('mall-h5-user-id');
  sessionStorage.removeItem('mall-h5-token');
};

const redirectToLoginWithDelay = (message, delayMs = 2200) => {
  if (disabledRedirecting) return;
  disabledRedirecting = true;
  showFailToast({ message, duration: delayMs });
  window.setTimeout(() => {
    window.location.href = '/login';
    disabledRedirecting = false;
  }, delayMs);
};
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/',
  timeout: 10000,
});

request.interceptors.request.use((config) => {
  if (config.skipAuth) {
    return config;
  }
  const token = localStorage.getItem('mall-h5-token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => {
    if (response?.data && response.data.success === false) {
      const apiError = new Error(response.data.message || response.data.msg || '请求失败');
      apiError.response = response;
      apiError.isBusinessFailure = true;
      return Promise.reject(apiError);
    }
    return response;
  },
  (error) => {
    const status = error?.response?.status;
    const message = error?.response?.data?.message || error?.response?.data?.msg || '';
    const code = error?.response?.data?.code || '';
    const disabled = status === 403 || code === 'FORBIDDEN' || code === 'AUTH_403' || String(message).includes('禁用');
    error.isDisabledUser = disabled;
    error.isAuthExpired = status === 401;
    if (status === 401 || disabled) {
      clearAuthTokens();
      const forceLogoutGuestRefreshing = sessionStorage.getItem('mallfei:force-logout-guest-refreshing') === '1';
      if (!forceLogoutGuestRefreshing && window.location.pathname !== '/login') {
        redirectToLoginWithDelay(disabled ? DISABLED_USER_MESSAGE : '登录状态已失效，请重新登录', disabled ? 2200 : 1400);
      }
    }
    return Promise.reject(error);
  },
);

export default request;
