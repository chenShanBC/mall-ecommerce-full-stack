export function getStatusTagMeta(kind, value) {
  const normalized = String(value || '').toUpperCase();

  const maps = {
    order: {
      PENDING_PAYMENT: { label: '待支付', type: 'warning' },
      PAID: { label: '已支付', type: 'success' },
      PROCESSING: { label: '处理中', type: 'primary' },
      SHIPPED: { label: '已发货', type: 'primary' },
      COMPLETED: { label: '已完成', type: 'success' },
      CANCELLED: { label: '已取消', type: 'info' },
      TIMEOUT_CANCELLED: { label: '超时取消', type: 'info' },
      REFUNDED: { label: '已退款', type: 'danger' },
    },
    pay: {
      PENDING: { label: '待处理', type: 'warning' },
      SUCCESS: { label: '支付成功', type: 'success' },
      CLOSED: { label: '已关闭', type: 'info' },
      REFUNDED: { label: '已退款', type: 'danger' },
    },
    aftersale: {
      PENDING_REVIEW: { label: '待审核', type: 'warning' },
      APPROVED: { label: '已通过', type: 'success' },
      REJECTED: { label: '已驳回', type: 'danger' },
      REFUND_PROCESSING: { label: '退款处理中', type: 'primary' },
      REFUND_SUCCESS: { label: '退款成功', type: 'success' },
      REFUND_FAILED: { label: '退款失败', type: 'danger' },
      CANCELLED: { label: '已取消', type: 'info' },
    },
    reconcile: {
      CONSISTENT: { label: '一致', type: 'success' },
      ABNORMAL: { label: '异常', type: 'danger' },
    },
    stockWarning: {
      NORMAL: { label: '正常', type: 'success' },
      LOW: { label: '低库存', type: 'warning' },
      HIGH: { label: '高库存', type: 'danger' },
    },
    stockStatus: {
      ACTIVE: { label: '正常', type: 'success' },
      FROZEN: { label: '冻结', type: 'warning' },
      OFFLINE: { label: '下架', type: 'info' },
    },
    operationResult: {
      SUCCESS: { label: '成功', type: 'success' },
      FAILED: { label: '失败', type: 'danger' },
    },
    alertLevel: {
      HIGH: { label: '高风险', type: 'danger' },
      MEDIUM: { label: '中风险', type: 'warning' },
      LOW: { label: '低风险', type: 'info' },
    },
    productStatus: {
      DRAFT: { label: '草稿', type: 'info' },
      OFFLINE: { label: '下架', type: 'warning' },
      ONLINE: { label: '上架', type: 'success' },
      VIOLATION: { label: '违规', type: 'danger' },
      OFFLINE_VIOLATION: { label: '违规下架', type: 'danger' },
      RECOVER_ONLINE: { label: '恢复上架', type: 'success' },
    },
    adminAccountStatus: {
      ENABLED: { label: '启用', type: 'success' },
      DISABLED: { label: '禁用', type: 'danger' },
    },
    userAccountStatus: {
      ENABLED: { label: '启用', type: 'success' },
      DISABLED: { label: '禁用', type: 'danger' },
    },
  };

  return maps[kind]?.[normalized] || { label: value || '-', type: 'info' };
}
