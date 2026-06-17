<template>
  <AdminLayout title="操作日志" @refresh="loadData" @logout="handleLogout">
    <el-card class="admin-page-card">
      <div class="admin-filter-bar">
        <el-input v-model="filters.keyword" clearable placeholder="模块 / 类型 / 内容 / 操作人" style="width: 280px" @keyup.enter="handleSearch" />
        <el-select v-model="filters.module" clearable placeholder="模块" style="width: 160px">
          <el-option v-for="module in operationModules" :key="module" :label="module" :value="module" />
        </el-select>
        <el-select v-model="filters.result" clearable placeholder="结果" style="width: 140px">
          <el-option label="SUCCESS" value="SUCCESS" />
          <el-option label="FAILED" value="FAILED" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button @click="exportLogs">导出</el-button>
      </div>
      <div class="admin-table-scroll">
        <el-table v-loading="loading" :data="logs" class="admin-table admin-table--with-gap admin-table--safe admin-table--wide operation-log-table" empty-text="暂无操作日志数据" @sort-change="handleSortChange">
          <el-table-column prop="id" label="ID" width="80" sortable="custom" show-overflow-tooltip :sort-orders="['descending', 'ascending']" :sort-order="query.sortBy === 'id' && query.sortOrder === 'desc' ? 'descending' : null" />
          <el-table-column prop="operatorUsername" label="操作人" width="120" sortable="custom" show-overflow-tooltip />
          <el-table-column label="模块" width="110" sortable="custom">
            <template #default="{ row }">
              <AdminOverflowText :value="row.operationModule" :text-class="operationLogTextClass(row)" />
            </template>
          </el-table-column>
          <el-table-column label="类型" width="220" sortable="custom" :sort-by="'operationType'">
            <template #default="{ row }">
              <AdminOverflowText :value="row.operationType" :text-class="operationLogTextClass(row)" />
            </template>
          </el-table-column>
          <el-table-column prop="operationContent" label="内容" min-width="520" cell-class-name="operation-content-column">
            <template #default="{ row }">
              <AdminOverflowText :value="row.operationContent" :text-class="operationLogTextClass(row)" popper-class="admin-overflow-tooltip operation-log-content-tooltip" />
            </template>
          </el-table-column>
          <el-table-column prop="operationResult" label="结果" width="110" sortable="custom">
            <template #default="{ row }"><el-tag :type="operationResultMeta(row.operationResult).type">{{ operationResultMeta(row.operationResult).label }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="createdAt" label="时间" width="170" sortable="custom" />
        </el-table>
      </div>
      <div class="admin-pagination"><el-pagination background layout="sizes, prev, pager, next, total" :current-page="pager.page" :page-size="pager.size" :page-sizes="ADMIN_PAGE_SIZES" :total="pager.total" @current-change="handlePageChange" @size-change="handleSizeChange" /></div>
    </el-card>
  </AdminLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import AdminLayout from '../components/AdminLayout.vue';
import AdminOverflowText from '../components/AdminOverflowText.vue';
import { fetchAdminOperationLogs } from '../api';
import { useAdminStore } from '../stores/admin';
import { exportRowsToCsv } from '../utils/export';
import { ADMIN_PAGE_SIZE, ADMIN_PAGE_SIZES } from '../utils/pagination';
import { getStatusTagMeta } from '../utils/status';

const route = useRoute();
const router = useRouter();
const adminStore = useAdminStore();
const operationResultMeta = (status) => getStatusTagMeta('operationResult', status);
const operationModules = ['SYSTEM', 'USER', 'PRODUCT', 'STOCK', 'ORDER', 'AFTERSALE', 'PAY', 'RECONCILIATION'];
const logs = ref([]);
const loading = ref(false);
const filters = reactive({ keyword: String(route.query.keyword || ''), module: String(route.query.module || ''), result: String(route.query.result || '') });
const query = reactive({ sortBy: String(route.query.sortBy || 'id'), sortOrder: String(route.query.sortOrder || 'desc') });
const pager = reactive({ page: Number(route.query.page || 1), size: Number(route.query.size || ADMIN_PAGE_SIZE), total: 0 });

const syncRoute = async () => {
  await router.replace({
    path: '/operation-logs',
    query: {
      ...(filters.keyword ? { keyword: filters.keyword } : {}),
      ...(filters.module ? { module: filters.module } : {}),
      ...(filters.result ? { result: filters.result } : {}),
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
    const { data } = await fetchAdminOperationLogs({
      keyword: filters.keyword || undefined,
      module: filters.module || undefined,
      result: filters.result || undefined,
      sortBy: query.sortBy || undefined,
      sortOrder: query.sortOrder || undefined,
      page: pager.page,
      size: pager.size,
    });
    logs.value = data.data?.records || [];
    pager.total = data.data?.total || 0;
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '加载日志失败');
  } finally {
    loading.value = false;
  }
};

const isAbnormalLog = (row) => row.operationResult === 'FAILED' || /权限|违规|拒绝|失败/.test(row.operationContent || '');
const isEnableLog = (row) => /启用|ENABLE/.test(`${row.operationType || ''} ${row.operationContent || ''}`);
const isDisableLog = (row) => /禁用|DISABLE/.test(`${row.operationType || ''} ${row.operationContent || ''}`);
const operationLogTextClass = (row) => ({
  'danger-text': isAbnormalLog(row),
  'enable-text': !isAbnormalLog(row) && isEnableLog(row),
  'disable-text': !isAbnormalLog(row) && isDisableLog(row),
});
const exportLogs = () => {
  exportRowsToCsv('运营操作日志', logs.value, [
    { label: 'ID', value: 'id' },
    { label: '操作人', value: 'operatorUsername' },
    { label: '模块', value: 'operationModule' },
    { label: '类型', value: 'operationType' },
    { label: '内容', value: 'operationContent' },
    { label: '结果', value: 'operationResult' },
    { label: '时间', value: 'createdAt' },
  ]);
};

const handleSearch = async () => { pager.page = 1; await syncRoute(); await loadData(); };
const handleReset = async () => { filters.keyword = ''; filters.module = ''; filters.result = ''; query.sortBy = 'id'; query.sortOrder = 'desc'; pager.page = 1; pager.size = ADMIN_PAGE_SIZE; await syncRoute(); await loadData(); };
const handlePageChange = async (page) => { pager.page = page; await syncRoute(); await loadData(); };
const handleSizeChange = async (size) => { pager.size = size; pager.page = 1; await syncRoute(); await loadData(); };
const handleSortChange = async ({ prop, order }) => { query.sortBy = prop || 'id'; query.sortOrder = order === 'ascending' ? 'asc' : 'desc'; pager.page = 1; await syncRoute(); await loadData(); };
const handleLogout = async () => { await adminStore.logout(); router.push('/login'); };

onMounted(loadData);
</script>

<style scoped>
:deep(.operation-log-table) {
  width: 100%;
}

:deep(.operation-content-column) {
  overflow: hidden !important;
}

:deep(.operation-log-table .el-table__row) {
  height: 68px;
}

:deep(.operation-content-column .cell) {
  width: 100%;
  min-width: 0;
  overflow: hidden !important;
  white-space: normal;
}

.danger-text { color: #f56c6c; font-weight: 600; }
.enable-text { color: #67c23a; font-weight: 600; }
.disable-text { color: #e6a23c; font-weight: 600; }
</style>
