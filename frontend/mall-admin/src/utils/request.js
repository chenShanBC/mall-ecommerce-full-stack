import axios from 'axios';

const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 10000,
});

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('mall-admin-token');
  if (token) {
    config.headers.Authorization = token;
  }
  return config;
});

export default request;
