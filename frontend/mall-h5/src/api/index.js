import request from '../utils/request';

export function loginByPassword(data) {
  return request.post('/api/auth/login/password', data);
}

export function fetchCurrentUser() {
  return request.get('/api/auth/me');
}

export function fetchProductPage() {
  return request.get('/api/product/spu/page');
}
