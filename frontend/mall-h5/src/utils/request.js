import axios from 'axios';
import { showFailToast } from 'vant';

const request = axios.create({
  baseURL: '/',
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
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    if (status === 401) {
      localStorage.removeItem('mall-h5-token');
      if (window.location.pathname !== '/login') {
        showFailToast('登录状态已失效，请重新登录');
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  },
);

export default request;
