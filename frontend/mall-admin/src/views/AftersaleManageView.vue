<template>
  <AdminLayout title="售后管理" @refresh="loadData" @logout="handleLogout">
    <el-card class="admin-page-card">
      <div class="admin-filter-bar">
        <el-input v-model="query.keyword" placeholder="售后单号 / 订单号" clearable style="width: 240px" @keyup.enter="loadData" />
        <el-input v-model="query.userId" placeholder="用户ID" clearable style="width: 160px" @keyup.enter="loadData" />
        <el-select v-model="query.status" clearable placeholder="全部售后状态" style="width: 180px">
          <el-option v-for="option in aftersaleStatusOptions" :key="option.value" :label="option.label" :value="option.value" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" class="admin-table admin-table--with-gap aftersale-manage-table reconcile-overflow-table" empty-text="暂无售后数据">
        <el-table-column label="售后单号" min-width="190"><template #default="{ row }"><AdminOverflowText :value="row.aftersaleNo" /></template></el-table-column>
        <el-table-column label="订单号" min-width="180"><template #default="{ row }"><AdminOverflowText :value="row.orderNo" /></template></el-table-column>
        <el-table-column label="用户ID" width="100"><template #default="{ row }">{{ row.userId }}</template></el-table-column>
        <el-table-column prop="aftersaleType" label="类型" width="130"><template #default="{ row }"><el-tag effect="plain" :type="aftersaleTypeTagType(row.aftersaleType)">{{ aftersaleTypeLabel(row.aftersaleType) }}</el-tag></template></el-table-column>
        <el-table-column prop="status" label="状态" width="140"><template #default="{ row }"><el-tag :type="aftersaleStatusMeta(row.status).type">{{ aftersaleStatusMeta(row.status).label }}</el-tag></template></el-table-column>
        <el-table-column prop="refundAmount" label="退款金额(分)" width="140"><template #default="{ row }">{{ row.refundAmount ?? row.refundAmountCent ?? '-' }}</template></el-table-column>
        <el-table-column label="申请原因" min-width="180"><template #default="{ row }"><AdminOverflowText :value="row.reason" /></template></el-table-column>
        <el-table-column label="驳回原因" min-width="180"><template #default="{ row }"><AdminOverflowText :value="row.rejectReason" text-class="admin-after-sale-reject-reason" /></template></el-table-column>
        <el-table-column label="失败原因" min-width="180"><template #default="{ row }"><AdminOverflowText :value="row.failReason" text-class="admin-after-sale-reason" /></template></el-table-column>
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button class="admin-after-sale-detail-btn" size="small" @click="openDetail(row.aftersaleNo)">详情</el-button>
            <el-button v-if="isPendingReview(row)" size="small" type="success" @click="openReviewDialog(row, 'APPROVE')">通过</el-button>
            <el-button v-if="isPendingReview(row)" size="small" type="danger" @click="openReviewDialog(row, 'REJECT')">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="detailVisible" title="售后详情" width="720px">
      <div v-if="detail" class="admin-detail__grid">
        <div class="admin-detail__item"><span class="admin-detail__label">售后单号</span>{{ detail.aftersaleNo }}</div>
        <div class="admin-detail__item"><span class="admin-detail__label">订单号</span>{{ detail.orderNo }}</div>
        <div class="admin-detail__item"><span class="admin-detail__label">用户ID</span>{{ detail.userId }}</div>
        <div class="admin-detail__item"><span class="admin-detail__label">售后类型</span>{{ aftersaleTypeLabel(detail.aftersaleType) }}</div>
        <div class="admin-detail__item"><span class="admin-detail__label">售后状态</span><el-tag :type="aftersaleStatusMeta(detail.status).type">{{ aftersaleStatusMeta(detail.status).label }}</el-tag></div>
        <div class="admin-detail__item"><span class="admin-detail__label">退款金额</span>{{ detail.refundAmount ?? detail.refundAmountCent ?? '-' }}</div>
        <div class="admin-detail__item full"><span class="admin-detail__label">申请原因</span>{{ detail.reason || '-' }}</div>
        <div class="admin-detail__item full"><span class="admin-detail__label">驳回原因</span>{{ detail.rejectReason || '-' }}</div>
        <div class="admin-detail__item full"><span class="admin-detail__label">失败原因</span>{{ detail.failReason || '-' }}</div>
      </div>
    </el-dialog>

    <el-dialog v-model="reviewVisible" :title="reviewForm.action === 'APPROVE' ? '通过售后申请' : '驳回售后申请'" width="460px" append-to-body destroy-on-close align-center class="admin-beauty-dialog">
      <el-form :model="reviewForm" label-width="90px" class="admin-dialog-form">
        <el-form-item label="售后单号"><span>{{ reviewForm.aftersaleNo }}</span></el-form-item>
        <el-form-item :label="reviewForm.action === 'APPROVE' ? '审核备注' : '驳回原因'">
          <el-input v-model="reviewForm.reason" type="textarea" :rows="4" maxlength="200" show-word-limit :placeholder="reviewForm.action === 'APPROVE' ? '请输入审核备注，例如：同意退款' : '请输入驳回原因，用户将在 H5 端看到该原因'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button :type="reviewForm.action === 'APPROVE' ? 'success' : 'danger'" :loading="reviewSubmitting" @click="submitReview">{{ reviewForm.action === 'APPROVE' ? '确认通过' : '确认驳回' }}</el-button>
      </template>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import AdminLayout from '../components/AdminLayout.vue';
import AdminOverflowText from '../components/AdminOverflowText.vue';
import { fetchAdminAftersaleDetail, fetchAdminAftersales, reviewAdminAftersale } from '../api';
import { useAdminStore } from '../stores/admin';
import { confirmAction } from '../utils/action';
import { getStatusTagMeta } from '../utils/status';

const router = useRouter();
const route = useRoute();
const adminStore = useAdminStore();
const query = reactive({ keyword: '', userId: '', status: '' });
const aftersaleStatusOptions = [
  { label: '待审核', value: 'PENDING_REVIEW' },
  { label: '审核通过', value: 'APPROVED' },
  { label: '审核驳回', value: 'REJECTED' },
  { label: '退款处理中', value: 'REFUNDING' },
  { label: '退款处理中', value: 'REFUND_PROCESSING' },
  { label: '退款成功', value: 'REFUND_SUCCESS' },
  { label: '退款失败', value: 'REFUND_FAILED' },
  { label: '退款关闭', value: 'REFUND_CLOSED' },
  { label: '已取消', value: 'CANCELLED' },
];
const normalizeStatus = (status) => String(status || '').toUpperCase();
const aftersaleTypeLabel = (type) => {
  const maps = {
    REFUND_ONLY: '仅退款',
    ONLY_REFUND: '仅退款',
    RETURN_REFUND: '退货退款',
    REFUND_RETURN: '退货退款',
    RETURN_AND_REFUND: '退货退款',
    EXCHANGE: '换货',
    REFUND: '退款',
  };
  return maps[normalizeStatus(type)] || '未知类型';
};
const aftersaleTypeTagType = (type) => ({ REFUND_ONLY: 'warning', ONLY_REFUND: 'warning', RETURN_REFUND: 'success', REFUND_RETURN: 'success', RETURN_AND_REFUND: 'success', EXCHANGE: 'info', REFUND: 'primary' }[normalizeStatus(type)] || 'info');
const isPendingReview = (row) => normalizeStatus(row?.status) === 'PENDING_REVIEW';
const aftersaleStatusMeta = (status) => getStatusTagMeta('aftersale', status);
const aftersaleStatusClass = (status) => ({
  PENDING_REVIEW: 'aftersale-status--warning',
  APPROVED: 'aftersale-status--success',
  REJECTED: 'aftersale-status--danger',
  REFUNDING: 'aftersale-status--warning',
  REFUND_PROCESSING: 'aftersale-status--warning',
  REFUND_SUCCESS: 'aftersale-status--success',
  REFUND_FAILED: 'aftersale-status--danger',
  REFUND_CLOSED: 'aftersale-status--info',
  CANCELLED: 'aftersale-status--info',
}[normalizeStatus(status)] || 'aftersale-status--info');
const applyRouteQuery = () => {
  query.keyword = String(route.query.keyword || '');
  query.userId = String(route.query.userId || '');
  query.status = route.query.status ? String(route.query.status) : '';
};
const rows = ref([]);
const loading = ref(false);
const detailVisible = ref(false);
const reviewVisible = ref(false);
const reviewSubmitting = ref(false);
const detail = ref(null);
const reviewForm = reactive({ aftersaleNo: '', action: 'APPROVE', reason: '' });

const loadData = async () => {
  loading.value = true;
  try {
    const params = {
      keyword: query.keyword.trim(),
      userId: query.userId.trim() ? Number(query.userId.trim()) : undefined,
    };
    if (query.status) params.status = query.status;
    const { data } = await fetchAdminAftersales(params);
    rows.value = data.data?.records || [];
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '售后列表加载失败');
  } finally {
    loading.value = false;
  }
};

const resetQuery = () => {
  query.keyword = '';
  query.userId = '';
  query.status = '';
  loadData();
};

const openDetail = async (aftersaleNo) => {
  const { data } = await fetchAdminAftersaleDetail(aftersaleNo);
  detail.value = data.data;
  detailVisible.value = true;
};

const openReviewDialog = (row, action) => {
  reviewForm.aftersaleNo = row.aftersaleNo;
  reviewForm.action = action;
  reviewForm.reason = '';
  reviewVisible.value = true;
};

const submitReview = async () => {
  try {
    await confirmAction(`确认${reviewForm.action === 'APPROVE' ? '通过' : '驳回'}售后单 ${reviewForm.aftersaleNo} 吗？`);
    reviewSubmitting.value = true;
    await reviewAdminAftersale(reviewForm.aftersaleNo, { action: reviewForm.action, reason: reviewForm.reason });
    ElMessage.success(reviewForm.action === 'APPROVE' ? '已生成退款单，请联系支付管理去同步完成退款' : '已驳回退款申请');
    reviewVisible.value = false;
    await loadData();
    if (detail.value?.aftersaleNo === reviewForm.aftersaleNo) {
      await openDetail(reviewForm.aftersaleNo);
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || '售后审核失败');
  } finally {
    reviewSubmitting.value = false;
  }
};

const handleLogout = async () => {
  await adminStore.logout();
  router.push('/login');
};

onMounted(() => {
  applyRouteQuery();
  loadData();
});
watch(() => route.query.status, () => {
  applyRouteQuery();
  loadData();
});
watch(() => route.query.keyword, () => {
  applyRouteQuery();
  loadData();
});
</script>

<style scoped>
.aftersale-manage-table,
.aftersale-manage-table :deep(.el-table__header),
.aftersale-manage-table :deep(.el-table__body) {
  table-layout: fixed;
}

.aftersale-manage-table :deep(.el-table__cell) {
  overflow: hidden;
}

.aftersale-manage-table :deep(.el-table__cell .cell) {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  word-break: keep-all;
}

.aftersale-manage-table :deep(.admin-overflow-cell),
.aftersale-manage-table :deep(.admin-overflow-ellipsis) {
  display: block;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow: hidden;
}

.aftersale-manage-table :deep(.admin-overflow-ellipsis) {
  text-overflow: ellipsis;
  white-space: nowrap;
  word-break: keep-all;
}

.aftersale-manage-table :deep(.admin-after-sale-reason) {
  color: #334155;
  line-height: 1.5;
}

.aftersale-manage-table :deep(.admin-after-sale-reject-reason) {
  color: #ef4444;
  line-height: 1.5;
}

.aftersale-manage-table :deep(.admin-after-sale-detail-btn) {
  color: #60a5fa;
  border-color: #bfdbfe;
  background-color: #eff6ff;
}

.aftersale-manage-table :deep(.admin-after-sale-detail-btn:hover),
.aftersale-manage-table :deep(.admin-after-sale-detail-btn:focus) {
  color: #3b82f6;
  border-color: #93c5fd;
  background-color: #dbeafe;
}
</style>
