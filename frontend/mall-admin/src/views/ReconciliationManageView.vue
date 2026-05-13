<template>
  <AdminLayout title="对账运营" @refresh="loadData" @logout="handleLogout">
    <el-card class="admin-page-card">
      <div class="admin-filter-bar">
        <el-select v-model="query.status" clearable placeholder="对账结果" style="width: 180px">
          <el-option label="一致" value="CONSISTENT" />
          <el-option label="异常" value="ABNORMAL" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button @click="exportRows">导出</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" class="admin-table admin-table--with-gap" empty-text="暂无对账数据" @sort-change="handleSortChange">
        <el-table-column prop="orderId" label="订单ID" width="100" sortable="custom" />
        <el-table-column prop="orderNo" label="订单号" min-width="180" sortable="custom" />
        <el-table-column prop="orderStatus" label="订单状态" width="120" sortable="custom"><template #default="{ row }"><el-tag :type="orderStatusMeta(row.orderStatus).type">{{ orderStatusMeta(row.orderStatus).label }}</el-tag></template></el-table-column>
        <el-table-column prop="payOrderNo" label="支付单号" min-width="180" />
        <el-table-column prop="payStatus" label="支付状态" width="120"><template #default="{ row }"><el-tag :type="payStatusMeta(row.payStatus).type">{{ payStatusMeta(row.payStatus).label }}</el-tag></template></el-table-column>
        <el-table-column prop="orderPayAmount" label="订单金额(分)" width="130" sortable="custom" />
        <el-table-column prop="payAmount" label="支付金额(分)" width="130" sortable="custom" />
        <el-table-column prop="reconcileStatus" label="对账结果" width="120" sortable="custom"><template #default="{ row }"><el-tag :type="reconcileStatusMeta(row.reconcileStatus).type">{{ reconcileStatusMeta(row.reconcileStatus).label }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="300">
          <template #default="{ row }">
            <el-button v-if="canManage" size="small" @click="runReconcile(row.orderNo)">重新对账</el-button>
            <el-button v-if="canManage && row.reconcileStatus === 'ABNORMAL'" size="small" type="danger" @click="openHandle(row)">异常处置</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="admin-pagination">
        <el-pagination background layout="sizes, prev, pager, next, total" :current-page="pager.page" :page-size="pager.size" :page-sizes="ADMIN_PAGE_SIZES" :total="pager.total" @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </el-card>

    <el-dialog v-model="handleVisible" title="对账异常处置" width="460px">
      <el-form :model="handleForm" label-width="100px" class="admin-dialog-form">
        <el-form-item label="处置动作"><el-select v-model="handleForm.action" style="width: 220px"><el-option label="重试对账" value="RETRY_RECONCILE" /><el-option label="关闭支付单" value="CLOSE_PAY_ORDER" /></el-select></el-form-item>
        <el-form-item label="处理说明"><el-input v-model="handleForm.reason" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="handleVisible = false">取消</el-button><el-button type="primary" @click="submitHandle">提交</el-button></template>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import AdminLayout from '../components/AdminLayout.vue';
import { fetchAdminReconciliations, handleAdminReconcile, runAdminReconcile } from '../api';
import { useAdminStore } from '../stores/admin';
import { confirmAction } from '../utils/action';
import { exportRowsToCsv } from '../utils/export';
import { ADMIN_PAGE_SIZE, ADMIN_PAGE_SIZES } from '../utils/pagination';
import { getStatusTagMeta } from '../utils/status';

const route = useRoute();
const router = useRouter();
const adminStore = useAdminStore();
const canManage = computed(() => adminStore.hasPermission('reconcile:manage'));
const orderStatusMeta = (status) => getStatusTagMeta('order', status);
const payStatusMeta = (status) => getStatusTagMeta('pay', status);
const reconcileStatusMeta = (status) => getStatusTagMeta('reconcile', status);
const rows = ref([]);
const loading = ref(false);
const handleVisible = ref(false);
const currentOrderNo = ref('');
const query = reactive({ status: String(route.query.status || ''), sortBy: String(route.query.sortBy || 'orderId'), sortOrder: String(route.query.sortOrder || 'asc') });
const pager = reactive({ page: Number(route.query.page || 1), size: Number(route.query.size || ADMIN_PAGE_SIZE), total: 0 });
const handleForm = reactive({ action: 'RETRY_RECONCILE', reason: '' });

const syncRoute = async () => {
  await router.replace({
    path: '/reconciliations',
    query: {
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
    const { data } = await fetchAdminReconciliations({ ...query, page: pager.page, size: pager.size });
    rows.value = data.data?.records || [];
    pager.total = data.data?.total || 0;
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '对账列表加载失败');
  } finally {
    loading.value = false;
  }
};
const exportRows = () => {
  exportRowsToCsv('对账运营列表', rows.value, [
    { label: '订单ID', value: 'orderId' },
    { label: '订单号', value: 'orderNo' },
    { label: '订单状态', value: 'orderStatus' },
    { label: '支付单号', value: 'payOrderNo' },
    { label: '支付状态', value: 'payStatus' },
    { label: '订单金额(分)', value: 'orderPayAmount' },
    { label: '支付金额(分)', value: 'payAmount' },
    { label: '对账结果', value: 'reconcileStatus' },
  ]);
};
const handleSearch = async () => { pager.page = 1; await syncRoute(); await loadData(); };
const handleReset = async () => { query.status = ''; query.sortBy = 'orderId'; query.sortOrder = 'asc'; pager.page = 1; pager.size = ADMIN_PAGE_SIZE; await syncRoute(); await loadData(); };
const handlePageChange = async (page) => { pager.page = page; await syncRoute(); await loadData(); };
const handleSizeChange = async (size) => { pager.size = size; pager.page = 1; await syncRoute(); await loadData(); };
const handleSortChange = async ({ prop, order }) => { query.sortBy = prop || 'orderId'; query.sortOrder = order === 'descending' ? 'desc' : 'asc'; pager.page = 1; await syncRoute(); await loadData(); };
const runReconcile = async (orderNo) => {
  try {
    await confirmAction(`确认重新对账订单“${orderNo}”吗？`);
    await runAdminReconcile(orderNo);
    ElMessage.success('重新对账完成');
    await loadData();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || '对账失败');
  }
};
const openHandle = (row) => { currentOrderNo.value = row.orderNo; Object.assign(handleForm, { action: 'RETRY_RECONCILE', reason: '' }); handleVisible.value = true; };
const submitHandle = async () => {
  try {
    await confirmAction(`确认提交订单“${currentOrderNo.value}”的异常处置吗？`);
    await handleAdminReconcile(currentOrderNo.value, { ...handleForm });
    ElMessage.success('对账异常处置成功');
    handleVisible.value = false;
    await loadData();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || '对账异常处置失败');
  }
};
const handleLogout = async () => { await adminStore.logout(); router.push('/login'); };

onMounted(loadData);
</script>
