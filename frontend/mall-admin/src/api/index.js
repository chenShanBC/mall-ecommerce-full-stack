import request from '../utils/request';

export function adminLogin(data) { return request.post('/api/admin/login/password', { username: data.username, password: data.password }); }
export function fetchCurrentAdmin() { return request.get('/api/admin/me'); }
export function updateMyAdminProfile(data) { return request.put('/api/admin/me/profile', data); }
export function changeMyAdminPassword(data) { return request.put('/api/admin/me/password', data); }
export function logoutAdmin() { return request.delete('/api/admin/logout'); }
export function fetchDashboard() { return request.get('/api/admin/dashboard'); }
export function fetchAdminAccounts(params = {}) { return request.get('/api/admin/accounts', { params }); }
export function fetchAdminRoles() { return request.get('/api/admin/roles'); }
export function fetchAdminPermissionTemplates() { return request.get('/api/admin/accounts/permission-templates'); }
export function createAdminAccount(data) { return request.post('/api/admin/accounts', data); }
export function assignAdminRole(adminId, data) { return request.patch(`/api/admin/accounts/${adminId}/role`, data); }
export function enableAdminAccount(adminId) { return request.patch(`/api/admin/accounts/${adminId}/enable`); }
export function disableAdminAccount(adminId) { return request.patch(`/api/admin/accounts/${adminId}/disable`); }
export function updateAdminAccountPermissions(adminId, data) { return request.put(`/api/admin/accounts/${adminId}/permissions`, data); }
export function fetchAdminOperationLogs(params = {}) { return request.get('/api/admin/operation-logs', { params }); }
export function fetchAdminUsers(params = {}) { return request.get('/api/admin/users', { params }); }
export function fetchAdminUserDetail(userId) { return request.get(`/api/admin/users/${userId}`); }
export function disableAdminUser(userId) { return request.patch(`/api/admin/users/${userId}/disable`); }
export function enableAdminUser(userId) { return request.patch(`/api/admin/users/${userId}/enable`); }
export function fetchAdminOrders(params = {}) { return request.get('/api/admin/orders', { params }); }
export function fetchAdminOrderDetail(orderNo) { return request.get(`/api/admin/orders/${orderNo}`); }
export function cancelAdminOrder(orderNo) { return request.delete(`/api/admin/orders/${orderNo}`); }
export function shipAdminOrder(orderNo) { return request.patch(`/api/admin/orders/${orderNo}/ship`); }
export function completeAdminOrder(orderNo) { return request.patch(`/api/admin/orders/${orderNo}/complete`); }
export function handleAdminOrderException(orderNo, data) { return request.post(`/api/admin/orders/${orderNo}/exception-handle`, data); }
export function fetchAdminPays(params = {}) { return request.get('/api/admin/pays', { params }); }
export function fetchAdminPayDetail(orderNo) { return request.get(`/api/admin/pays/${orderNo}`); }
export function closeAdminPayOrder(orderNo, data) { return request.post(`/api/admin/pays/${orderNo}/close`, data); }
export function syncAdminPayOrderStatus(orderNo) { return request.post(`/api/admin/pays/${orderNo}/sync-status`); }
export function repairAdminPaidOrder(orderNo) { return request.post(`/api/admin/pays/${orderNo}/repair-paid`); }
export function fetchAdminAftersales(params = {}) { return request.get('/api/admin/aftersales', { params }); }
export function fetchAdminAftersaleDetail(aftersaleNo) { return request.get(`/api/admin/aftersales/${aftersaleNo}`); }
export function reviewAdminAftersale(aftersaleNo, data) { return request.post(`/api/admin/aftersales/${aftersaleNo}/review`, data); }
export function fetchAdminReconciliations(params = {}) { return request.get('/api/admin/reconciliations', { params }); }
export function runAdminReconcile(orderNo) { return request.post(`/api/admin/reconciliations/${orderNo}/run`); }
export function handleAdminReconcile(orderNo, data) { return request.post(`/api/admin/reconciliations/${orderNo}/handle`, data); }
export function fetchAdminProductPage(params = {}) { return request.get('/api/admin/products', { params }); }
export function fetchAdminProductDetail(productId) { return request.get(`/api/admin/products/${productId}`); }
export function createAdminProduct(data) { return request.post('/api/admin/products', data); }
export function updateAdminProduct(productId, data) { return request.put(`/api/admin/products/${productId}`, data); }
export function updateAdminProductStatus(productId, data) { return request.patch(`/api/admin/products/${productId}/status`, data); }
export function handleAdminProductViolation(productId, data) { return request.post(`/api/admin/products/${productId}/violation-handle`, data); }
export function fetchAdminCategories() { return request.get('/api/admin/categories'); }
export function fetchAdminStocks(params = {}) { return request.get('/api/admin/stocks', { params }); }
export function fetchWarningStocks(params = {}) { return request.get('/api/admin/stocks/warnings', { params }); }
export function fetchStockLogs(params = {}) { return request.get('/api/admin/stocks/logs', { params }); }
export function updateAdminStockPolicy(skuId, data) { return request.put(`/api/admin/stocks/${skuId}/policy`, data); }
export function adjustAdminStock(skuId, data) { return request.put(`/api/admin/stocks/${skuId}/adjust`, data); }
export function handleAdminStockWarning(skuId, data) { return request.post(`/api/admin/stocks/${skuId}/warning-handle`, data); }
