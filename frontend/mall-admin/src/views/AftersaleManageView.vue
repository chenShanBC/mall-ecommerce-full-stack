<template>
  <AdminLayout title="售后管理" @refresh="loadData" @logout="handleLogout">
    <el-card class="admin-page-card">
      <div class="admin-filter-bar">
        <el-input v-model="query.keyword" placeholder="售后单号 / 订单号" clearable style="width: 280px" @keyup.enter="loadData" />
        <el-select v-model="query.status" clearable placeholder="售后状态" style="width: 180px">
          <el-option label="待审核" value="PENDING_REVIEW" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" class="admin-table admin-table--with-gap" empty-text="暂无售后数据">
        <el-table-column prop="aftersaleNo" label="售后单号" min-width="190" />
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="aftersaleType" label="类型" width="130" />
        <el-table-column prop="status" label="状态" width="140">
          <template #default="{ row }">
            <el-tag :type="aftersaleStatusMeta(row.status).type">{{ aftersaleStatusMeta(row.status).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="refundAmount" label="退款金额(分)" width="140" />
        <el-table-column prop="reason" label="原因" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row.aftersaleNo)">详情</el-button>
            <el-button v-if="row.status === 'PENDING_REVIEW' && canManage" size="small" type="success" @click="review(row.aftersaleNo, 'APPROVE')">通过</el-button>
            <el-button v-if="row.status === 'PENDING_REVIEW' && canManage" size="small" type="danger" @click="review(row.aftersaleNo, 'REJECT')">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="detailVisible" title="售后详情" width="720px">
      <div v-if="detail" class="admin-detail__grid">
        <div class="admin-detail__item"><span class="admin-detail__label">售后单号</span>{{ detail.aftersaleNo }}</div>
        <div class="admin-detail__item"><span class="admin-detail__label">订单号</span>{{ detail.orderNo }}</div>
        <div class="admin-detail__item"><span class="admin-detail__label">用户ID</span>{{ detail.userId }}</div>
        <div class="admin-detail__item"><span class="admin-detail__label">售后类型</span>{{ detail.aftersaleType }}</div>
        <div class="admin-detail__item"><span class="admin-detail__label">售后状态</span><el-tag :type="aftersaleStatusMeta(detail.status).type">{{ aftersaleStatusMeta(detail.status).label }}</el-tag></div>
        <div class="admin-detail__item"><span class="admin-detail__label">退款金额</span>{{ detail.refundAmount }}</div>
        <div class="admin-detail__item full"><span class="admin-detail__label">原因</span>{{ detail.reason }}</div>
      </div>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import AdminLayout from '../components/AdminLayout.vue';
import { fetchAdminAftersaleDetail, fetchAdminAftersales, reviewAdminAftersale } from '../api';
import { useAdminStore } from '../stores/admin';
import { confirmAction } from '../utils/action';
import { getStatusTagMeta } from '../utils/status';

const router = useRouter();
const adminStore = useAdminStore();
const canManage = computed(() => adminStore.hasPermission('order:manage'));
const aftersaleStatusMeta = (status) => getStatusTagMeta('aftersale', status);
const query = reactive({ keyword: '', status: '' });
const rows = ref([]);
const loading = ref(false);
const detailVisible = ref(false);
const detail = ref(null);

const loadData = async () => {
  loading.value = true;
  try {
    const { data } = await fetchAdminAftersales(query);
    rows.value = data.data?.records || [];
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '售后列表加载失败');
  } finally {
    loading.value = false;
  }
};

const openDetail = async (aftersaleNo) => {
  const { data } = await fetchAdminAftersaleDetail(aftersaleNo);
  detail.value = data.data;
  detailVisible.value = true;
};

const review = async (aftersaleNo, action) => {
  try {
    await confirmAction(`确认${action === 'APPROVE' ? '通过' : '驳回'}售后单 ${aftersaleNo} 吗？`);
    await reviewAdminAftersale(aftersaleNo, { action });
    ElMessage.success('操作成功');
    await loadData();
    if (detail.value?.aftersaleNo === aftersaleNo) {
      await openDetail(aftersaleNo);
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || '售后审核失败');
  }
};

const handleLogout = async () => {
  await adminStore.logout();
  router.push('/login');
};

onMounted(loadData);
</script>
