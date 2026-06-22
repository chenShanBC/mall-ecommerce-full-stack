import request from '../utils/request';

export function adminLogin(data) { return request.post('/api/admin/login/password', { username: data.username, password: data.password }); }
export function fetchCurrentAdmin() { return request.get('/api/admin/me'); }
export function updateMyAdminProfile(data) { return request.put('/api/admin/me/profile', data); }
export function changeMyAdminPassword(data) { return request.put('/api/admin/me/password', data); }
export function logoutAdmin() { return request.delete('/api/admin/logout'); }
export function fetchDashboard(params = {}) { return request.get('/api/admin/dashboard', { params }); }
export function fetchAdminFinanceCumulativeNetIncome() { return request.get('/api/admin/dashboard/finance-cumulative-net-income'); }
export function fetchAdminFinanceTrend() { return request.get('/api/admin/dashboard/finance-trend'); }
export function fetchAdminWarehouseTrend() { return request.get('/api/admin/dashboard/warehouse-trend'); }
export function fetchAdminAccounts(params = {}) { return request.get('/api/admin/accounts', { params }); }
export function fetchAdminRoles() { return request.get('/api/admin/roles'); }
export function fetchAdminPermissions() { return request.get('/api/admin/permissions'); }
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
export function fetchAdminOrderSkuSwitchOptions(orderNo, orderItemId) { return request.get(`/api/admin/orders/${orderNo}/items/${orderItemId}/sku-switch-options`); }
export function cancelAdminOrder(orderNo) { return request.delete(`/api/admin/orders/${orderNo}`); }
export function shipAdminOrder(orderNo) { return request.patch(`/api/admin/orders/${orderNo}/ship`); }
export function completeAdminOrder(orderNo) { return request.patch(`/api/admin/orders/${orderNo}/complete`); }
export function updateAdminOrderReceiver(orderNo, data) { return request.put(`/api/admin/orders/${orderNo}/receiver`, data); }
export function handleAdminOrderException(orderNo, data) { return request.post(`/api/admin/orders/${orderNo}/exception-handle`, data); }
export function markAdminOrderPaymentException(orderNo, data = {}) { return request.post(`/api/admin/orders/${orderNo}/payment-exception`, data); }
export function verifyAdminOrderPaymentException(orderNo) { return request.post(`/api/admin/orders/${orderNo}/payment-exception/verify`); }
export function transferAdminOrderPaymentExceptionToPaySync(orderNo, data = {}) { return request.post(`/api/admin/orders/${orderNo}/payment-exception/transfer-pay-sync`, data); }
export function markAdminOrderPaymentExceptionPendingAction(orderNo, data = {}) { return request.post(`/api/admin/orders/${orderNo}/payment-exception/pending-action`, data); }
export function confirmAdminOrderPaid(orderNo, data = {}) { return request.post(`/api/admin/orders/${orderNo}/confirm-paid`, data); }
export function restoreAdminOrderPendingPayment(orderNo, data = {}) { return request.post(`/api/admin/orders/${orderNo}/restore-pending-payment`, data); }
export function closeAdminOrderAndReleaseStock(orderNo, data = {}) { return request.post(`/api/admin/orders/${orderNo}/close-and-release-stock`, data); }
export function fetchAdminPays(params = {}) { return request.get('/api/admin/pays', { params }); }
export function fetchAdminPayDetail(orderNo) { return request.get(`/api/admin/pays/${orderNo}`); }
export function closeAdminPayOrder(orderNo, data) { return request.post(`/api/admin/pays/${orderNo}/close`, data); }
export function syncAdminPayOrderStatus(orderNo) { return request.post(`/api/admin/pays/${orderNo}/sync-status`); }
export function repairAdminPaidOrder(orderNo) { return request.post(`/api/admin/pays/${orderNo}/repair-paid`); }
export function fetchAdminRefunds(orderNo, params = {}) { return request.get(`/api/admin/pays/${orderNo}/refunds`, { params }); }
export function fetchAdminGlobalRefunds(params = {}) { return request.get('/api/admin/pays/refunds', { params }); }
export function syncAdminRefundStatus(orderNo, refundNo, refundAmountCent) { return request.post(`/api/admin/pays/${orderNo}/refunds/${refundNo}/sync-status`, null, { params: { refundAmountCent } }); }
export function fetchAdminPayCallbackRecords(params = {}) { return request.get('/api/admin/pays/callback-records', { params }); }
export function fetchAdminAftersales(params = {}) { return request.get('/api/admin/aftersales', { params }); }
export function fetchAdminAftersaleDetail(aftersaleNo) { return request.get(`/api/admin/aftersales/${aftersaleNo}`); }
export function reviewAdminAftersale(aftersaleNo, data) { return request.post(`/api/admin/aftersales/${aftersaleNo}/review`, data); }
export function fetchAdminOnlineReconcileTasks(params = {}) { return request.get('/api/admin/online-reconcile-tasks', { params }); }
export function createAdminOnlineReconcileTask(data) { return request.post('/api/admin/online-reconcile-tasks', data); }
export function fetchAdminOnlineReconcileTask(taskId) { return request.get(`/api/admin/online-reconcile-tasks/${taskId}`); }
export function generateAdminOnlineLocalBills(taskId) { return request.post(`/api/admin/online-reconcile-tasks/${taskId}/local-bills/generate`); }
export function fetchAdminOnlineLocalBills(taskId, params = {}) { return request.get(`/api/admin/online-reconcile-tasks/${taskId}/local-bills`, { params }); }
export function generateAdminOnlineMockChannelBills(taskId, data = {}) { return request.post(`/api/admin/online-reconcile-tasks/${taskId}/channel-bills/mock-generate`, data); }
export function uploadAdminAlipayChannelBills(taskId, file) { const formData = new FormData(); formData.append('file', file); return request.post(`/api/admin/online-reconcile-tasks/${taskId}/channel-bills/alipay-upload`, formData, { headers: { 'Content-Type': 'multipart/form-data' } }); }
export function fetchAdminOnlineChannelBills(taskId, params = {}) { return request.get(`/api/admin/online-reconcile-tasks/${taskId}/channel-bills`, { params }); }
export function matchAdminOnlineReconcileTask(taskId) { return request.post(`/api/admin/online-reconcile-tasks/${taskId}/match`); }
export function completeAdminOnlineReconcileTask(taskId, data = {}) { return request.post(`/api/admin/online-reconcile-tasks/${taskId}/complete`, data); }
export function fetchAdminOnlineDiffItems(taskId, params = {}) { return request.get(`/api/admin/online-reconcile-tasks/${taskId}/diff-items`, { params }); }
export function fetchAdminOnlineDiffItem(diffId) { return request.get(`/api/admin/online-reconcile-diff-items/${diffId}`); }
export function handleAdminOnlineDiffItem(diffId, data) { return request.post(`/api/admin/online-reconcile-diff-items/${diffId}/handle`, data); }
export function fetchAdminOnlineDiffLogs(diffId, params = {}) { return request.get(`/api/admin/online-reconcile-diff-items/${diffId}/logs`, { params }); }
export function fetchAdminOnlineHangingFollows(params = {}) { return request.get('/api/admin/online-reconcile-hangings', { params }); }
export function followAdminOnlineHangingDiff(diffId, data = {}) { return request.post(`/api/admin/online-reconcile-diff-items/${diffId}/follow-up`, data); }
export function transferAdminOnlineHangingDiffToFinance(diffId, data = {}) { return request.post(`/api/admin/online-reconcile-diff-items/${diffId}/transfer-finance`, data); }
export function closeAdminOnlineHangingDiff(diffId, data = {}) { return request.post(`/api/admin/online-reconcile-diff-items/${diffId}/close-hanging`, data); }
export function fetchAdminOnlineArchiveReport(params = {}) { return request.get('/api/admin/online-reconcile-archive-report', { params }); }
export function fetchAdminOnlineTaskLogs(taskId, params = {}) { return request.get(`/api/admin/online-reconcile-tasks/${taskId}/logs`, { params }); }
export function fetchAdminReconciliationOverview() { return request.get('/api/admin/reconciliations/overview'); }
export function fetchAdminReconciliations(params = {}) { return request.get('/api/admin/reconciliations', { params }); }
export function runAdminReconcile(orderNo) { return request.post(`/api/admin/reconciliations/${orderNo}/run`); }
export function handleAdminReconcile(orderNo, data) { return request.post(`/api/admin/reconciliations/${orderNo}/handle`, data); }
export function fetchAdminPayReconciliationRecords(params = {}) { return request.get('/api/admin/reconciliations/pay-records', { params }); }
export function handleAdminPayReconciliation(id, data) { return request.post(`/api/admin/reconciliations/pay-records/${id}/handle`, data); }
export function markAdminPayReconciliationDone(id, data = {}) { return request.post(`/api/admin/reconciliations/pay-records/${id}/done`, data); }
export function ignoreAdminPayReconciliation(id, data = {}) { return request.post(`/api/admin/reconciliations/pay-records/${id}/ignore`, data); }
export function fetchAdminStockReconciliations(params = {}) { return request.get('/api/admin/reconciliations/stocks', { params }); }
export function fetchAdminStockReconciliationDetail(id) { return request.get(`/api/admin/reconciliations/stocks/${id}`); }
export function repairAdminStockReconciliation(id, data = {}) { return request.post(`/api/admin/reconciliations/stocks/${id}/repair`, data); }
export function ignoreAdminStockReconciliation(id, data = {}) { return request.post(`/api/admin/reconciliations/stocks/${id}/ignore`, data); }
export function fetchAdminProductPage(params = {}) { return request.get('/api/admin/products', { params }); }
export function fetchAdminProductSalesThresholdConfig() { return request.get('/api/admin/products/sales-threshold-config'); }
export function updateAdminProductSalesThresholdConfig(data) { return request.put('/api/admin/products/sales-threshold-config', data); }
export function fetchAdminProductDetail(productId) { return request.get(`/api/admin/products/${productId}`); }
export function createAdminProduct(data) { return request.post('/api/admin/products', data); }
export function updateAdminProduct(productId, data) { return request.put(`/api/admin/products/${productId}`, data); }
export function updateAdminProductStatus(productId, data) { return request.patch(`/api/admin/products/${productId}/status`, data); }
export function handleAdminProductViolation(productId, data) { return request.post(`/api/admin/products/${productId}/violation-handle`, data); }
export function uploadProductImage(file) { const formData = new FormData(); formData.append('file', file); return request.post('/api/files/product-image', formData, { headers: { 'Content-Type': 'multipart/form-data' } }); }
export function fetchAdminCategories() { return request.get('/api/admin/categories'); }
export function createAdminCategory(data) { return request.post('/api/admin/categories', data); }
export function updateAdminCategory(categoryId, data) { return request.put(`/api/admin/categories/${categoryId}`, data); }
export function updateAdminCategoryStatus(categoryId, data) { return request.patch(`/api/admin/categories/${categoryId}/status`, data); }
export function deleteAdminCategory(categoryId) { return request.delete(`/api/admin/categories/${categoryId}`); }
export function fetchAdminStocks(params = {}) { return request.get('/api/admin/stocks', { params }); }
export function fetchAdminTodayActiveStocks(params = {}) { return request.get('/api/admin/stocks/today-active', { params }); }
export function fetchWarningStocks(params = {}) { return request.get('/api/admin/stocks/warnings', { params }); }
export function fetchStockLogs(params = {}) { return request.get('/api/admin/stocks/logs', { params }); }
export function updateAdminStockPolicy(skuId, data) { return request.put(`/api/admin/stocks/${skuId}/policy`, data); }
export function adjustAdminStock(skuId, data) { return request.put(`/api/admin/stocks/${skuId}/adjust`, data); }
export function handleAdminStockWarning(skuId, data) { return request.post(`/api/admin/stocks/${skuId}/warning-handle`, data); }
export function checkAdminStockConsistency(skuId) { return request.post(`/api/admin/stocks/${skuId}/consistency-check`); }
