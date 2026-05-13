<template>
  <AdminLayout title="库存日志" @refresh="loadLogs" @logout="handleLogout">
    <el-card class="admin-page-card">
      <el-form :inline="true" :model="filters" class="admin-filter-form">
        <el-form-item label="SKU ID"><el-input v-model="filters.skuId" clearable placeholder="请输入 SKU ID" /></el-form-item>
        <el-form-item label="操作类型"><el-input v-model="filters.operationType" clearable placeholder="如 INIT / MANUAL_ADJUST" /></el-form-item>
        <el-form-item label="开始时间"><el-date-picker v-model="filters.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" clearable /></el-form-item>
        <el-form-item label="结束时间"><el-date-picker v-model="filters.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" clearable /></el-form-item>
        <el-form-item><el-button type="primary" @click="handleSearch">查询</el-button></el-form-item>
        <el-form-item><el-button @click="handleReset">重置</el-button></el-form-item>
        <el-form-item><el-button @click="exportLogs">导出</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table v-loading="loading" :data="logs" class="admin-table" empty-text="暂无库存日志数据" @sort-change="handleSortChange">
        <el-table-column prop="id" label="ID" width="80" sortable="custom" />
        <el-table-column prop="skuId" label="SKU ID" width="100" sortable="custom" />
        <el-table-column prop="operationType" label="操作类型" width="140" sortable="custom" />
        <el-table-column prop="businessType" label="业务类型" width="120" sortable="custom" />
        <el-table-column prop="businessNo" label="业务单号" min-width="160" />
        <el-table-column prop="sourceType" label="来源" width="120" />
        <el-table-column prop="operatorName" label="操作人" width="140" sortable="custom" />
        <el-table-column prop="changeQuantity" label="变更量" width="100" sortable="custom" />
        <el-table-column prop="beforeAvailableStock" label="变更前可用" width="120" sortable="custom" />
        <el-table-column prop="afterAvailableStock" label="变更后可用" width="120" sortable="custom" />
        <el-table-column prop="remark" label="备注" min-width="180" />
        <el-table-column prop="createdAt" label="时间" width="180" sortable="custom" />
      </el-table>
      <div class="admin-pagination"><el-pagination background layout="sizes, prev, pager, next, total" :current-page="pager.page" :page-size="pager.size" :page-sizes="ADMIN_PAGE_SIZES" :total="pager.total" @current-change="handlePageChange" @size-change="handleSizeChange" /></div>
    </el-card>
  </AdminLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import AdminLayout from '../components/AdminLayout.vue';
import { fetchStockLogs } from '../api';
import { useAdminStore } from '../stores/admin';
import { exportRowsToCsv } from '../utils/export';
import { ADMIN_PAGE_SIZE, ADMIN_PAGE_SIZES } from '../utils/pagination';

const route = useRoute();
const router = useRouter();
const adminStore = useAdminStore();
const logs = ref([]);
const loading = ref(false);
const filters = reactive({ skuId: String(route.query.skuId || ''), operationType: String(route.query.operationType || ''), startTime: String(route.query.startTime || ''), endTime: String(route.query.endTime || '') });
const query = reactive({ sortBy: String(route.query.sortBy || 'id'), sortOrder: String(route.query.sortOrder || 'asc') });
const pager = reactive({ page: Number(route.query.page || 1), size: Number(route.query.size || ADMIN_PAGE_SIZE), total: 0 });

const syncRoute = async () => {
  await router.replace({
    path: '/stock-logs',
    query: {
      ...(filters.skuId ? { skuId: String(filters.skuId) } : {}),
      ...(filters.operationType ? { operationType: filters.operationType } : {}),
      ...(filters.startTime ? { startTime: filters.startTime } : {}),
      ...(filters.endTime ? { endTime: filters.endTime } : {}),
      ...(query.sortBy ? { sortBy: query.sortBy } : {}),
      ...(query.sortOrder ? { sortOrder: query.sortOrder } : {}),
      ...(pager.page > 1 ? { page: String(pager.page) } : {}),
      ...(pager.size !== ADMIN_PAGE_SIZE ? { size: String(pager.size) } : {}),
    },
  });
};

const loadLogs = async () => {
  loading.value = true;
  try {
    const { data } = await fetchStockLogs({
      skuId: filters.skuId || undefined,
      operationType: filters.operationType || undefined,
      startTime: filters.startTime || undefined,
      endTime: filters.endTime || undefined,
      sortBy: query.sortBy || undefined,
      sortOrder: query.sortOrder || undefined,
      page: pager.page,
      size: pager.size,
    });
    logs.value = data.data.records || [];
    pager.total = data.data.total || 0;
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '库存日志加载失败');
  } finally {
    loading.value = false;
  }
};
const exportLogs = () => {
  exportRowsToCsv('库存操作日志', logs.value, [
    { label: 'ID', value: 'id' },
    { label: 'SKU ID', value: 'skuId' },
    { label: '操作类型', value: 'operationType' },
    { label: '业务类型', value: 'businessType' },
    { label: '业务单号', value: 'businessNo' },
    { label: '操作人', value: 'operatorName' },
    { label: '变更量', value: 'changeQuantity' },
    { label: '时间', value: 'createdAt' },
  ]);
};
const handleSearch = async () => { pager.page = 1; await syncRoute(); await loadLogs(); };
const handleReset = async () => { filters.skuId = ''; filters.operationType = ''; filters.startTime = ''; filters.endTime = ''; query.sortBy = 'id'; query.sortOrder = 'asc'; pager.page = 1; pager.size = ADMIN_PAGE_SIZE; await syncRoute(); await loadLogs(); };
const handlePageChange = async (page) => { pager.page = page; await syncRoute(); await loadLogs(); };
const handleSizeChange = async (size) => { pager.size = size; pager.page = 1; await syncRoute(); await loadLogs(); };
const handleSortChange = async ({ prop, order }) => { query.sortBy = prop || 'id'; query.sortOrder = order === 'descending' ? 'desc' : 'asc'; pager.page = 1; await syncRoute(); await loadLogs(); };
const handleLogout = async () => { await adminStore.logout(); router.push('/login'); };

onMounted(loadLogs);
</script>
