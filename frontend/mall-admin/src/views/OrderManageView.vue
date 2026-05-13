<template>
  <AdminLayout title="订单基础管控" @refresh="loadData" @logout="handleLogout">
    <el-card class="admin-page-card">
      <div class="admin-filter-bar">
        <el-input v-model="query.keyword" placeholder="订单号 / 收货人 / 手机号" clearable style="width: 280px" @keyup.enter="handleSearch" />
        <el-select v-model="query.status" clearable placeholder="订单状态" style="width: 180px">
          <el-option label="待支付" value="PENDING_PAYMENT" />
          <el-option label="已支付" value="PAID" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="已发货" value="SHIPPED" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button @click="exportOrders">导出</el-button>
      </div>
      <el-table v-loading="loading" :data="orders" class="admin-table admin-table--with-gap" empty-text="暂无订单数据" @sort-change="handleSortChange">
        <el-table-column prop="id" label="订单ID" width="110" sortable="custom" />
        <el-table-column prop="orderNo" label="订单号" min-width="180" sortable="custom" />
        <el-table-column prop="status" label="状态" width="140" sortable="custom"><template #default="{ row }"><el-tag :type="orderStatusMeta(row.status).type">{{ orderStatusMeta(row.status).label }}</el-tag></template></el-table-column>
        <el-table-column prop="receiverName" label="收货人" width="120" sortable="custom" />
        <el-table-column prop="receiverPhone" label="手机号" width="130" />
        <el-table-column prop="payAmount" label="支付金额(分)" width="140" sortable="custom" />
        <el-table-column label="操作" width="420">
          <template #default="{ row }">
            <div class="order-actions">
              <el-button class="order-action-btn order-action-btn--detail" size="small" @click="openDetail(row.orderNo)">详情</el-button>
              <el-button v-if="canManage" class="order-action-btn order-action-btn--warn" size="small" @click="openException(row)">异常处理</el-button>
              <el-button v-if="canManage" class="order-action-btn order-action-btn--danger" size="small" @click="cancelOrder(row.orderNo)">取消订单</el-button>
              <el-button v-if="canManage" class="order-action-btn order-action-btn--primary" size="small" @click="shipOrder(row.orderNo)">确认发货</el-button>
              <el-button v-if="canManage" class="order-action-btn order-action-btn--success" size="small" @click="completeOrder(row.orderNo)">完结订单</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="admin-pagination"><el-pagination background layout="sizes, prev, pager, next, total" :current-page="pager.page" :page-size="pager.size" :page-sizes="ADMIN_PAGE_SIZES" :total="pager.total" @current-change="handlePageChange" @size-change="handleSizeChange" /></div>
    </el-card>

    <el-dialog v-model="detailVisible" title="订单详情" width="920px">
      <div v-if="detail" class="admin-detail">
        <div class="admin-detail__title">订单：{{ detail.orderNo }}</div>
        <div class="admin-detail__grid">
          <div class="admin-detail__item"><span class="admin-detail__label">订单状态</span><el-tag :type="orderStatusMeta(detail.status).type">{{ orderStatusMeta(detail.status).label }}</el-tag></div>
          <div class="admin-detail__item"><span class="admin-detail__label">支付金额</span>{{ detail.payAmount ?? '-' }}</div>
          <div class="admin-detail__item"><span class="admin-detail__label">收货人</span>{{ detail.receiverName || '-' }}</div>
          <div class="admin-detail__item"><span class="admin-detail__label">手机号</span>{{ detail.receiverPhone || '-' }}</div>
          <div class="admin-detail__item full"><span class="admin-detail__label">收货地址</span>{{ detail.address || '-' }}</div>
          <div class="admin-detail__item full"><span class="admin-detail__label">备注</span>{{ detail.remark || '-' }}</div>
        </div>
        <el-table :data="detail.items || []" class="admin-table">
          <el-table-column prop="skuId" label="SKU ID" width="90" />
          <el-table-column prop="skuName" label="SKU名称" min-width="220" />
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column prop="salePriceCent" label="单价(分)" width="100" />
          <el-table-column prop="totalAmountCent" label="小计(分)" width="100" />
        </el-table>
      </div>
    </el-dialog>

    <el-dialog v-model="exceptionVisible" title="异常订单处理" width="680px">
      <el-form :model="exceptionForm" label-width="100px" class="admin-dialog-form">
        <el-form-item label="异常类型">
          <el-select v-model="exceptionForm.exceptionType" style="width: 100%">
            <el-option label="地址错误" value="ADDRESS_ERROR" />
            <el-option label="支付异常" value="PAYMENT_EXCEPTION" />
          </el-select>
        </el-form-item>
        <template v-if="exceptionForm.exceptionType === 'ADDRESS_ERROR'">
          <el-form-item label="收货人"><el-input v-model="exceptionForm.receiverName" /></el-form-item>
          <el-form-item label="手机号"><el-input v-model="exceptionForm.receiverPhone" /></el-form-item>
          <el-form-item label="详细地址"><el-input v-model="exceptionForm.receiverDetailAddress" type="textarea" :rows="3" /></el-form-item>
        </template>
        <el-form-item label="处理备注"><el-input v-model="exceptionForm.note" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="exceptionVisible = false">取消</el-button><el-button type="primary" @click="submitException">提交</el-button></template>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import AdminLayout from '../components/AdminLayout.vue';
import { cancelAdminOrder, completeAdminOrder, fetchAdminOrderDetail, fetchAdminOrders, handleAdminOrderException, shipAdminOrder } from '../api';
import { useAdminStore } from '../stores/admin';
import { confirmAction } from '../utils/action';
import { exportRowsToCsv } from '../utils/export';
import { t } from '../utils/i18n';
import { ADMIN_PAGE_SIZE, ADMIN_PAGE_SIZES } from '../utils/pagination';
import { getStatusTagMeta } from '../utils/status';

const route = useRoute();
const router = useRouter();
const adminStore = useAdminStore();
const canManage = computed(() => adminStore.hasPermission('order:manage'));
const orderStatusMeta = (status) => getStatusTagMeta('order', status);
const query = reactive({ keyword: String(route.query.keyword || ''), status: String(route.query.status || ''), sortBy: String(route.query.sortBy || 'id'), sortOrder: String(route.query.sortOrder || 'asc') });
const pager = reactive({ page: Number(route.query.page || 1), size: Number(route.query.size || ADMIN_PAGE_SIZE), total: 0 });
const orders = ref([]);
const loading = ref(false);
const detailVisible = ref(false);
const detail = ref(null);
const exceptionVisible = ref(false);
const currentOrderNo = ref('');
const exceptionForm = reactive({ exceptionType: 'ADDRESS_ERROR', receiverName: '', receiverPhone: '', receiverDetailAddress: '', note: '' });

const syncRoute = async () => {
  await router.replace({
    path: '/orders',
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
    const { data } = await fetchAdminOrders({ ...query, page: pager.page, size: pager.size });
    orders.value = data.data?.records || [];
    pager.total = data.data?.total || 0;
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || t('orderManage.loadFailed'));
  } finally {
    loading.value = false;
  }
};

const exportOrders = () => {
  exportRowsToCsv(t('orderManage.exportName'), orders.value, [
    { label: '订单号', value: 'orderNo' },
    { label: '状态', value: 'status' },
    { label: '收货人', value: 'receiverName' },
    { label: '手机号', value: 'receiverPhone' },
    { label: '支付金额(分)', value: 'payAmount' },
  ]);
};

const handleSearch = async () => { pager.page = 1; await syncRoute(); await loadData(); };
const handleReset = async () => { query.keyword = ''; query.status = ''; query.sortBy = 'id'; query.sortOrder = 'asc'; pager.page = 1; pager.size = ADMIN_PAGE_SIZE; await syncRoute(); await loadData(); };
const handlePageChange = async (page) => { pager.page = page; await syncRoute(); await loadData(); };
const handleSizeChange = async (size) => { pager.size = size; pager.page = 1; await syncRoute(); await loadData(); };
const handleSortChange = async ({ prop, order }) => { query.sortBy = prop || 'id'; query.sortOrder = order === 'descending' ? 'desc' : 'asc'; pager.page = 1; await syncRoute(); await loadData(); };
const openDetail = async (orderNo) => { const { data } = await fetchAdminOrderDetail(orderNo); detail.value = data.data; detailVisible.value = true; };
const openException = (row) => { currentOrderNo.value = row.orderNo; Object.assign(exceptionForm, { exceptionType: 'ADDRESS_ERROR', receiverName: row.receiverName, receiverPhone: row.receiverPhone, receiverDetailAddress: '', note: '' }); exceptionVisible.value = true; };

const submitException = async () => {
  try {
    await confirmAction(t('orderManage.confirmException', { orderNo: currentOrderNo.value }));
    await handleAdminOrderException(currentOrderNo.value, { ...exceptionForm });
    ElMessage.success(t('orderManage.exceptionSuccess'));
    exceptionVisible.value = false;
    await loadData();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || t('orderManage.exceptionFailed'));
  }
};

const cancelOrder = async (orderNo) => { try { await confirmAction(t('orderManage.confirmCancel', { orderNo })); await cancelAdminOrder(orderNo); ElMessage.success(t('orderManage.cancelSuccess')); await loadData(); } catch (error) { if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || t('orderManage.cancelFailed')); } };
const shipOrder = async (orderNo) => { try { await confirmAction(t('orderManage.confirmShip', { orderNo })); await shipAdminOrder(orderNo); ElMessage.success(t('orderManage.shipSuccess')); await loadData(); } catch (error) { if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || t('orderManage.shipFailed')); } };
const completeOrder = async (orderNo) => { try { await confirmAction(t('orderManage.confirmComplete', { orderNo })); await completeAdminOrder(orderNo); ElMessage.success(t('orderManage.completeSuccess')); await loadData(); } catch (error) { if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || t('orderManage.completeFailed')); } };
const handleLogout = async () => { await adminStore.logout(); router.push('/login'); };

onMounted(loadData);
</script>

<style scoped>
.order-actions{display:flex;flex-wrap:wrap;gap:8px}.order-action-btn{min-width:84px;margin:0;border:none;border-radius:999px;box-shadow:0 10px 24px rgba(15,23,42,.08)}.order-action-btn--detail{color:#2563eb;background:linear-gradient(135deg,#eff6ff,#dbeafe)}.order-action-btn--warn{color:#b45309;background:linear-gradient(135deg,#fff7ed,#ffedd5)}.order-action-btn--danger{color:#be123c;background:linear-gradient(135deg,#fff1f2,#ffe4e6)}.order-action-btn--primary{color:#4338ca;background:linear-gradient(135deg,#eef2ff,#e0e7ff)}.order-action-btn--success{color:#047857;background:linear-gradient(135deg,#ecfdf5,#d1fae5)}
</style>
