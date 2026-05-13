<template>
  <AdminLayout title="支付单管控" @refresh="loadData" @logout="handleLogout">
    <el-card class="admin-page-card">
      <div class="admin-filter-bar">
        <el-input v-model="query.keyword" placeholder="订单号 / 支付单号" clearable style="width: 280px" @keyup.enter="handleSearch" />
        <el-select v-model="query.status" clearable placeholder="支付状态" style="width: 180px">
          <el-option label="待处理" value="PENDING" />
          <el-option label="支付成功" value="SUCCESS" />
          <el-option label="已关闭" value="CLOSED" />
          <el-option label="已退款" value="REFUNDED" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button @click="exportPays">导出</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" class="admin-table admin-table--with-gap" empty-text="暂无支付单数据" @sort-change="handleSortChange">
        <el-table-column prop="id" label="ID" width="90" sortable="custom" />
        <el-table-column prop="payOrderNo" label="支付单号" min-width="180" sortable="custom" />
        <el-table-column prop="orderNo" label="订单号" min-width="180" sortable="custom" />
        <el-table-column prop="status" label="状态" width="140" sortable="custom">
          <template #default="{ row }"><el-tag :type="payStatusMeta(row.status).type">{{ payStatusMeta(row.status).label }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="payAmount" label="支付金额(分)" width="140" sortable="custom" />
        <el-table-column prop="payChannel" label="支付渠道" width="120" />
        <el-table-column label="操作" width="360">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row.orderNo)">详情</el-button>
            <el-button v-if="canManage" size="small" type="warning" @click="syncStatus(row)">同步状态</el-button>
            <el-button v-if="canManage" size="small" type="success" :disabled="row.status !== 'SUCCESS'" @click="repairPaid(row)">补偿订单</el-button>
            <el-button v-if="canManage" size="small" type="danger" :disabled="row.status === 'SUCCESS' || row.status === 'REFUNDED' || row.status === 'CLOSED'" @click="openClose(row)">关闭</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="admin-pagination">
        <el-pagination background layout="sizes, prev, pager, next, total" :current-page="pager.page" :page-size="pager.size" :page-sizes="ADMIN_PAGE_SIZES" :total="pager.total" @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="支付单详情" width="760px">
      <div v-if="detail" class="admin-detail">
        <div class="admin-detail__title">支付单：{{ detail.payOrderNo }}</div>
        <div class="admin-detail__grid">
          <div class="admin-detail__item"><span class="admin-detail__label">订单号</span>{{ detail.orderNo }}</div>
          <div class="admin-detail__item"><span class="admin-detail__label">状态</span><el-tag :type="payStatusMeta(detail.status).type">{{ payStatusMeta(detail.status).label }}</el-tag></div>
          <div class="admin-detail__item"><span class="admin-detail__label">金额</span>{{ detail.payAmount }}</div>
          <div class="admin-detail__item"><span class="admin-detail__label">渠道</span>{{ detail.payChannel || '-' }}</div>
          <div class="admin-detail__item"><span class="admin-detail__label">交易号</span>{{ detail.transactionNo || '-' }}</div>
          <div class="admin-detail__item"><span class="admin-detail__label">幂等键</span>{{ detail.idempotentKey || '-' }}</div>
          <div class="admin-detail__item full"><span class="admin-detail__label">回调负载</span></div>
          <div class="admin-detail__item full"><el-input :model-value="detail.callbackPayload || ''" type="textarea" :rows="8" readonly /></div>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="closeVisible" title="关闭支付单" width="460px">
      <el-form :model="closeForm" label-width="100px" class="admin-dialog-form">
        <el-form-item label="关闭原因"><el-input v-model="closeForm.reason" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="closeVisible = false">取消</el-button><el-button type="primary" @click="submitClose">提交</el-button></template>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import AdminLayout from '../components/AdminLayout.vue';
import { closeAdminPayOrder, fetchAdminPayDetail, fetchAdminPays, repairAdminPaidOrder, syncAdminPayOrderStatus } from '../api';
import { useAdminStore } from '../stores/admin';
import { confirmAction } from '../utils/action';
import { exportRowsToCsv } from '../utils/export';
import { t } from '../utils/i18n';
import { ADMIN_PAGE_SIZE, ADMIN_PAGE_SIZES } from '../utils/pagination';
import { getStatusTagMeta } from '../utils/status';

const route = useRoute();
const router = useRouter();
const adminStore = useAdminStore();
const canManage = computed(() => adminStore.hasPermission('pay:manage'));
const payStatusMeta = (status) => getStatusTagMeta('pay', status);
const rows = ref([]);
const loading = ref(false);
const detail = ref(null);
const detailVisible = ref(false);
const closeVisible = ref(false);
const currentOrderNo = ref('');
const query = reactive({ keyword: String(route.query.keyword || ''), status: String(route.query.status || ''), sortBy: String(route.query.sortBy || 'id'), sortOrder: String(route.query.sortOrder || 'asc') });
const pager = reactive({ page: Number(route.query.page || 1), size: Number(route.query.size || ADMIN_PAGE_SIZE), total: 0 });
const closeForm = reactive({ reason: '' });

const syncRoute = async () => {
  await router.replace({
    path: '/pays',
    query: {
      ...(query.keyword ? { keyword: query.keyword } : {}),
      ...(query.status ? { status: query.status } : {}),
      ...(query.sortBy ? { sortBy: query.sortBy } : {}),
      ...(query.sortOrder ? { sortOrder: query.sortOrder } : {}),
      ...(pager.page > 1 ? { page: String(pager.page) } : {}),
      ...(pager.size !== ADMIN_PAGE_SIZE ? { size: String(pager.size) } : {}),
    },
  });
};

const loadData = async () => {
  loading.value = true;
  try {
    const { data } = await fetchAdminPays({ ...query, page: pager.page, size: pager.size });
    rows.value = data.data?.records || [];
    pager.total = data.data?.total || 0;
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || t('payManage.loadFailed'));
  } finally {
    loading.value = false;
  }
};
const exportPays = () => {
  exportRowsToCsv(t('payManage.exportName'), rows.value, [
    { label: 'ID', value: 'id' },
    { label: '支付单号', value: 'payOrderNo' },
    { label: '订单号', value: 'orderNo' },
    { label: '状态', value: 'status' },
    { label: '支付金额(分)', value: 'payAmount' },
    { label: '支付渠道', value: 'payChannel' },
  ]);
};
const handleSearch = async () => { pager.page = 1; await syncRoute(); await loadData(); };
const handleReset = async () => { query.keyword = ''; query.status = ''; query.sortBy = 'id'; query.sortOrder = 'asc'; pager.page = 1; pager.size = ADMIN_PAGE_SIZE; await syncRoute(); await loadData(); };
const handlePageChange = async (page) => { pager.page = page; await syncRoute(); await loadData(); };
const handleSizeChange = async (size) => { pager.size = size; pager.page = 1; await syncRoute(); await loadData(); };
const handleSortChange = async ({ prop, order }) => { query.sortBy = prop || 'id'; query.sortOrder = order === 'descending' ? 'desc' : 'asc'; pager.page = 1; await syncRoute(); await loadData(); };
const openDetail = async (orderNo) => { const { data } = await fetchAdminPayDetail(orderNo); detail.value = data.data; detailVisible.value = true; };
const openClose = (row) => { currentOrderNo.value = row.orderNo; closeForm.reason = ''; closeVisible.value = true; };

const syncStatus = async (row) => {
  try {
    await confirmAction(`确认同步订单 ${row.orderNo} 的支付状态吗？`);
    await syncAdminPayOrderStatus(row.orderNo);
    ElMessage.success('支付状态同步成功');
    await loadData();
    if (detailVisible.value && detail.value?.orderNo === row.orderNo) {
      await openDetail(row.orderNo);
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || '同步支付状态失败');
  }
};

const repairPaid = async (row) => {
  try {
    await confirmAction(`确认补偿订单 ${row.orderNo} 的支付状态吗？该操作会将已成功支付但未更新的订单补偿为已支付。`);
    await repairAdminPaidOrder(row.orderNo);
    ElMessage.success('订单支付状态补偿成功');
    await loadData();
    if (detailVisible.value && detail.value?.orderNo === row.orderNo) {
      await openDetail(row.orderNo);
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || '补偿订单支付状态失败');
  }
};

const submitClose = async () => {
  try {
    await confirmAction(t('payManage.confirmClose', { orderNo: currentOrderNo.value }));
    await closeAdminPayOrder(currentOrderNo.value, { ...closeForm });
    ElMessage.success(t('payManage.closeSuccess'));
    closeVisible.value = false;
    await loadData();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || t('payManage.closeFailed'));
  }
};
const handleLogout = async () => { await adminStore.logout(); router.push('/login'); };

onMounted(loadData);
</script>
