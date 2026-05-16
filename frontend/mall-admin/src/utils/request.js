import axios from 'axios';

const LOGIN_PATH = '/login';
const UNAUTHORIZED_CODE = 'AUTH_401';

const redirectToLogin = () => {
  const currentPath = window.location.pathname;
  localStorage.removeItem('mall-admin-token');
  localStorage.removeItem('mall-admin-profile');
  if (currentPath !== LOGIN_PATH) {
    window.location.replace(LOGIN_PATH);
  }
};

const isUnauthorizedPayload = (payload) => {
  const code = String(payload?.code || '');
  const message = String(payload?.msg || payload?.message || '').toLowerCase();
  return code === UNAUTHORIZED_CODE || message.includes('未登录') || message.includes('登录已失效') || message.includes('token');
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
      redirectToLogin();
      return Promise.reject(new Error('AUTH_401'));
    }
    return response;
  },
  (error) => {
    const status = error?.response?.status;
    const payload = error?.response?.data;
    if (status === 401 || isUnauthorizedPayload(payload)) {
      redirectToLogin();
    }
    return Promise.reject(error);
  },
);

export { redirectToLogin };
export default request;
