export function formatPrice(cents) {
  const value = Number(cents || 0) / 100;
  return value.toFixed(2);
}

export function formatStatus(status) {
  const map = {
    PENDING_PAYMENT: '待支付',
    PAID: '已支付',
    PROCESSING: '处理中',
    SHIPPED: '已发货',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    TIMEOUT_CANCELLED: '超时取消',
    CLOSED: '已关闭',
    REFUND_PENDING: '退款中',
    REFUNDED: '已退款',
    REFUND_CLOSED: '退款关闭',
    PARTIALLY_REFUNDED: '部分退款',
    PENDING: '待支付',
    PAYING: '支付中',
    SUCCESS: '支付成功',
    FAILED: '支付失败',
    REFUND_FAILED: '退款失败',
    REFUND_SUCCESS: '退款成功',
  };
  return map[status] || status || '--';
}

export function formatCountdown(totalSeconds) {
  const seconds = Math.max(0, Number(totalSeconds || 0));
  const minutes = Math.floor(seconds / 60);
  const remainSeconds = seconds % 60;
  return `${String(minutes).padStart(2, '0')}:${String(remainSeconds).padStart(2, '0')}`;
}

export function safeJsonParse(text, fallback = []) {
  if (!text) {
    return fallback;
  }
  try {
    return JSON.parse(text);
  } catch {
    return fallback;
  }
}

export function formatAddress(address) {
  if (!address) {
    return '';
  }
  return [
    address.receiverProvinceName || address.provinceName,
    address.receiverCityName || address.cityName,
    address.receiverDistrictName || address.districtName,
    address.receiverDetailAddress || address.detailAddress,
  ]
    .filter(Boolean)
    .join(' ');
}

export function formatDateTime(value) {
  if (!value) {
    return '--';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  const pad = (num) => String(num).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}
