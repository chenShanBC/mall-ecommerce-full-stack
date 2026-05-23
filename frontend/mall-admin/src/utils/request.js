import axios from 'axios';
import { ADMIN_FORCE_LOGOUT_MESSAGE_KEY } from './adminForceLogout';

const LOGIN_PATH = '/login';
const UNAUTHORIZED_CODE = 'AUTH_401';

const redirectToLogin = (message = '') => {
  const currentPath = window.location.pathname;
  if (message) {
    localStorage.setItem(ADMIN_FORCE_LOGOUT_MESSAGE_KEY, message);
  }
  localStorage.removeItem('mall-admin-token');
  localStorage.removeItem('mall-admin-profile');
  const reasonQuery = message ? `?reason=forceLogout&t=${Date.now()}` : '';
  if (currentPath !== LOGIN_PATH) {
    window.location.replace(`${LOGIN_PATH}${reasonQuery}`);
  } else if (message) {
    window.location.replace(`${LOGIN_PATH}${reasonQuery}`);
  }
};

const getPayloadMessage = (payload) => String(payload?.msg || payload?.message || '');

const isDisabledMessage = (message) => /禁用|停用|disabled/i.test(message || '');

const isUnauthorizedPayload = (payload) => {
  const code = String(payload?.code || '');
  const message = getPayloadMessage(payload).toLowerCase();
  return code === UNAUTHORIZED_CODE || message.includes('未登录') || message.includes('登录已失效') || message.includes('token') || isDisabledMessage(message);
};

const getUnauthorizedRedirectMessage = (payload) => {
  const message = getPayloadMessage(payload);
  return isDisabledMessage(message) ? (message || '您的后台账号已被禁用，请联系超级管理员处理。') : '';
};

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/',
  timeout: 10000,
});

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('mall-admin-token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

request.interceptors.response.use(
  (response) => {
    if (isUnauthorizedPayload(response?.data)) {
      redirectToLogin(getUnauthorizedRedirectMessage(response?.data));
      return Promise.reject(new Error('AUTH_401'));
    }
    return response;
  },
  (error) => {
    const status = error?.response?.status;
    const payload = error?.response?.data;
    if (status === 401 || isUnauthorizedPayload(payload)) {
      redirectToLogin(getUnauthorizedRedirectMessage(payload));
    }
    return Promise.reject(error);
  },
);

export { redirectToLogin };
export default request;
