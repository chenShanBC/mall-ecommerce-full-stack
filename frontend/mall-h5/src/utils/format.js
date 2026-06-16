export function formatPrice(cents) {
  const value = Number(cents || 0) / 100;
  return value.toFixed(2);
}

const STATUS_LABELS = {
  order: {
    PENDING_PAYMENT: '待支付',
    PAID: '已支付',
    PAYMENT_EXCEPTION: '订单异常',
    PROCESSING: '处理中',
    SHIPPED: '已发货',
    COMPLETED: '已完成',
    CANCELLED: '已取消',
    TIMEOUT_CANCELLED: '超时取消',
    CLOSED: '已关闭',
    REFUND_PENDING: '处理中',
    REFUNDED: '已退款',
    REFUND_CLOSED: '退款关闭',
    PARTIALLY_REFUNDED: '部分退款',
  },
  pay: {
    PENDING: '待支付',
    PAYING: '支付中',
    SUCCESS: '支付成功',
    FAILED: '支付失败',
    REFUND_PENDING: '退款中',
    REFUNDING: '退款中',
    REFUNDED: '已退款',
    PARTIALLY_REFUNDED: '部分退款',
    REFUND_FAILED: '退款失败',
  },
  refund: {
    REFUND_PENDING: '待退款',
    REFUNDING: '退款中',
    REFUND_SUCCESS: '退款成功',
    REFUND_FAILED: '退款失败',
    REFUND_CLOSED: '退款关闭',
  },
  aftersale: {
    PENDING_REVIEW: '售后中',
    APPROVED: '审核通过',
    REJECTED: '审核驳回',
    REFUND_PROCESSING: '退款中',
    EFUND_PROCESSING: '退款中',
    REFUND_SUCCESS: '退款成功',
    REFUND_FAILED: '退款失败',
    CANCELLED: '已取消',
  },
};

export function formatStatus(status, kind) {
  const normalized = String(status || '').toUpperCase();
  if (!normalized) {
    return '--';
  }
  if (kind && STATUS_LABELS[kind]?.[normalized]) {
    return STATUS_LABELS[kind][normalized];
  }
  return Object.values(STATUS_LABELS).find((labels) => labels[normalized])?.[normalized] || status;
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
