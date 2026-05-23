import request from '../utils/request';

export function loginCaptchaChallenge() {
  return request.get('/api/users/login/captcha/challenge', {
    skipAuth: true,
  });
}

export function loginCaptchaVerify(data) {
  return request.post('/api/users/login/captcha/verify', data, {
    skipAuth: true,
  });
}

export function checkLoginBlacklist(mobile) {
  return request.get('/api/users/login/blacklist/status', {
    params: { mobile },
    skipAuth: true,
  });
}

export function loginByPassword(data) {
  return request.post('/api/users/login/password', data, {
    skipAuth: true,
  });
}

export function sendLoginSmsCode(data) {
  return request.post('/api/users/login/sms/send-code', data, {
    skipAuth: true,
  });
}

export function loginBySmsCode(data) {
  return request.post('/api/users/login/sms', data, {
    skipAuth: true,
  });
}

export function registerUser(data) {
  return request.post('/api/users/register', data, {
    skipAuth: true,
  });
}

export function fetchAlipayLoginAuthUrl() {
  return request.get('/api/users/login/alipay/auth-url', {
    skipAuth: true,
  });
}

export function exchangeAlipayLoginTicket(data) {
  return request.post('/api/users/login/alipay/exchange', data, {
    skipAuth: true,
  });
}

export function exchangeAlipayJsapiAuthCode(data) {
  return request.post('/api/users/login/alipay/jsapi-exchange', data, {
    skipAuth: true,
  });
}

export function fetchCurrentUser() {
  return request.get('/api/users/me');
}

export function updateCurrentUserProfile(data) {
  return request.put('/api/users/me', data);
}

export function changeCurrentUserPassword(data) {
  return request.put('/api/users/me/password', data);
}

export function uploadAvatar(file) {
  const formData = new FormData();
  formData.append('file', file);
  return request.post('/api/files/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
}

export function fetchAuthContext() {
  return request.get('/api/auth/context');
}

export function fetchAuthBlacklistStatus(userId) {
  return request.get(`/api/auth/blacklist/${userId}`);
}

export function logoutUser() {
  return request.delete('/api/auth/logout');
}

export function fetchCategories() {
  return request.get('/api/categories');
}

export function fetchProductPage() {
  return request.get('/api/products');
}

export function fetchProductDetail(productId) {
  return request.get(`/api/products/${productId}`);
}

export function addCartItem(data) {
  return request.post('/api/cart/items', data);
}

export function fetchCartItems() {
  return request.get('/api/cart/items');
}

export function updateCartItem(cartItemId, data) {
  return request.put(`/api/cart/items/${cartItemId}`, data);
}

export function updateCartItemChecked(data) {
  return request.put('/api/cart/items/checked', data);
}

export function deleteCartItem(cartItemId) {
  return request.delete(`/api/cart/items/${cartItemId}`);
}

export function clearCartItems() {
  return request.delete('/api/cart/items');
}

export function fetchCartQuantity() {
  return request.get('/api/cart/quantity');
}

export function fetchCartSettlementPreview() {
  return request.get('/api/cart/settlement-preview');
}

export function prepareCartCheckout(data) {
  return request.post('/api/cart/prepare-checkout', data);
}

export function checkoutCart(data) {
  return request.post('/api/cart/checkout', data);
}

export function createOrder(data) {
  return request.post('/api/orders', data);
}

export function fetchOrders() {
  return request.get('/api/orders');
}

export function fetchOrderDetail(orderId) {
  return request.get(`/api/orders/${orderId}`);
}

export function cancelOrder(orderId) {
  return request.delete(`/api/orders/${orderId}/cancel`);
}

export function deleteUserOrder(orderId) {
  return request.delete(`/api/orders/${orderId}`);
}

export function confirmReceipt(orderId) {
  return request.put(`/api/orders/${orderId}/confirm-receipt`);
}

export function applyOrderRefund(orderId, data) {
  return request.post(`/api/orders/${orderId}/refund`, data);
}

export function applyAftersaleRefund(data) {
  return request.post('/api/aftersales/refund', data);
}

export function createPayOrder(orderNo, payChannel = 'MOCK', returnPath = '/orders') {
  return request.post('/api/pay/orders', null, {
    params: { orderNo, payChannel, returnPath },
  });
}

export function mockPaySuccess(orderNo) {
  return request.post('/api/pay/callback/mock-success', null, {
    params: { orderNo },
  });
}

export function fetchPayOrderDetail(payOrderNo) {
  return request.get(`/api/pay/orders/${payOrderNo}`);
}

export function reconcilePayOrder(orderNo) {
  return request.get('/api/pay/reconcile', {
    params: { orderNo },
  });
}

export function syncPayOrderStatus(orderNo) {
  return request.post(`/api/pay/orders/${orderNo}/sync-status`);
}

export function repairPaidOrder(orderNo) {
  return request.post(`/api/pay/orders/${orderNo}/repair-paid`);
}

export function fetchUserAddresses() {
  return request.get('/api/users/addresses');
}

export function createUserAddress(data) {
  return request.post('/api/users/addresses', data);
}

export function updateUserAddress(addressId, data) {
  return request.put(`/api/users/addresses/${addressId}`, data);
}

export function deleteUserAddress(addressId) {
  return request.delete(`/api/users/addresses/${addressId}`);
}

export function setDefaultAddress(addressId) {
  return request.put(`/api/users/addresses/${addressId}/default`);
}
