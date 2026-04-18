import request from '../utils/request';

export function adminLogin(data) {
  return request.post('/api/admin/auth/login', data);
}

export function fetchDashboard() {
  return request.get('/api/admin/dashboard');
}

export function fetchAdminProductPage() {
  return request.get('/api/admin/product/spu/page');
}
