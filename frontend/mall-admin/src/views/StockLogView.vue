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
      <div class="stock-log-toolbar">
        <span class="stock-log-toolbar__label">所有列已展示，长文本悬浮查看完整内容</span>
      </div>
      <div class="admin-table-scroll">
        <el-table v-loading="loading" :data="logs" class="admin-table admin-table--safe admin-table--xwide stock-log-table" empty-text="暂无库存日志数据" @sort-change="handleSortChange">
        <el-table-column prop="id" label="ID" width="70" fixed="left" sortable="custom" />
        <el-table-column prop="skuId" label="SKU ID" width="90" fixed="left" sortable="custom" />
        <el-table-column prop="skuName" min-width="200" label="SKU 名称" fixed="left" show-overflow-tooltip sortable="custom" />
        <el-table-column prop="operationType" label="操作类型" width="125" sortable="custom">
          <template #default="{ row }">
            <el-tooltip v-if="row.operationType" effect="light" placement="bottom-start" :show-after="300" :offset="8" popper-class="stock-log-business-tooltip">
              <template #content><span class="stock-log-business-tooltip__text">{{ row.operationType }}</span></template>
              <span class="stock-log-ellipsis-cell">{{ row.operationType }}</span>
            </el-tooltip>
            <span v-else class="stock-log-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="businessType" label="业务类型" width="110" sortable="custom">
          <template #default="{ row }">
            <el-tooltip v-if="row.businessType" effect="light" placement="bottom-start" :show-after="300" :offset="8" popper-class="stock-log-business-tooltip">
              <template #content><span class="stock-log-business-tooltip__text">{{ row.businessType }}</span></template>
              <span class="stock-log-ellipsis-cell">{{ row.businessType }}</span>
            </el-tooltip>
            <span v-else class="stock-log-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="businessNo" label="业务单号" width="150">
          <template #default="{ row }">
            <el-tooltip v-if="row.businessNo" effect="light" placement="bottom-start" :show-after="300" :offset="8" popper-class="stock-log-business-tooltip">
              <template #content><span class="stock-log-business-tooltip__text">{{ row.businessNo }}</span></template>
              <span class="stock-log-ellipsis-cell">{{ row.businessNo }}</span>
            </el-tooltip>
            <span v-else class="stock-log-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="sourceType" label="来源" width="90">
          <template #default="{ row }">
            <el-tooltip v-if="row.sourceType" effect="light" placement="bottom-start" :show-after="300" :offset="8" popper-class="stock-log-business-tooltip">
              <template #content><span class="stock-log-business-tooltip__text">{{ row.sourceType }}</span></template>
              <span class="stock-log-ellipsis-cell">{{ row.sourceType }}</span>
            </el-tooltip>
            <span v-else class="stock-log-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="100" sortable="custom">
          <template #default="{ row }">
            <el-tooltip v-if="row.operatorName" effect="light" placement="bottom-start" :show-after="300" :offset="8" popper-class="stock-log-business-tooltip">
              <template #content><span class="stock-log-business-tooltip__text">{{ row.operatorName }}</span></template>
              <span class="stock-log-ellipsis-cell">{{ row.operatorName }}</span>
            </el-tooltip>
            <span v-else class="stock-log-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="changeQuantity" label="变更量" width="90" sortable="custom">
          <template #default="{ row }">
            <el-tag size="small" :type="Number(row.changeQuantity) >= 0 ? 'success' : 'danger'" effect="light">{{ formatChangeQuantity(row.changeQuantity) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="beforeAvailableStock" label="变更前可用" width="105" sortable="custom" />
        <el-table-column prop="afterAvailableStock" label="变更后可用" width="105" sortable="custom" />
        <el-table-column prop="remark" label="备注" min-width="170">
          <template #default="{ row }">
            <el-tooltip v-if="row.remark" effect="light" placement="bottom-start" :show-after="300" :offset="8" popper-class="stock-log-business-tooltip">
              <template #content><span class="stock-log-business-tooltip__text">{{ row.remark }}</span></template>
              <span class="stock-log-ellipsis-cell">{{ row.remark }}</span>
            </el-tooltip>
            <span v-else class="stock-log-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="160" sortable="custom" />
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
        </el-table>
      </div>
      <div class="admin-pagination"><el-pagination background layout="sizes, prev, pager, next, total" :current-page="pager.page" :page-size="pager.size" :page-sizes="ADMIN_PAGE_SIZES" :total="pager.total" @current-change="handlePageChange" @size-change="handleSizeChange" /></div>
    </el-card>

    <el-dialog v-model="detailDialogVisible" title="库存日志详情" width="680px" destroy-on-close>
      <el-descriptions v-if="selectedLog" :column="2" border class="stock-log-detail">
        <el-descriptions-item label="ID">{{ selectedLog.id }}</el-descriptions-item>
        <el-descriptions-item label="SKU ID">{{ selectedLog.skuId }}</el-descriptions-item>
        <el-descriptions-item label="SKU 名称" :span="2">{{ selectedLog.skuName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ selectedLog.operationType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="业务类型">{{ selectedLog.businessType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="业务单号" :span="2"><span class="detail-long-text">{{ selectedLog.businessNo || '-' }}</span></el-descriptions-item>
        <el-descriptions-item label="来源">{{ selectedLog.sourceType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ selectedLog.operatorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="变更前可用">{{ selectedLog.beforeAvailableStock ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="变更后可用">{{ selectedLog.afterAvailableStock ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="变更量">{{ formatChangeQuantity(selectedLog.changeQuantity) }}</el-descriptions-item>
        <el-descriptions-item label="时间">{{ selectedLog.createdAt || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2"><span class="detail-long-text">{{ selectedLog.remark || '-' }}</span></el-descriptions-item>
      </el-descriptions>
    </el-dialog>
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
const detailDialogVisible = ref(false);
const selectedLog = ref(null);
const filters = reactive({ skuId: String(route.query.skuId || ''), operationType: String(route.query.operationType || ''), startTime: String(route.query.startTime || ''), endTime: String(route.query.endTime || '') });
const query = reactive({ sortBy: String(route.query.sortBy || 'id'), sortOrder: String(route.query.sortOrder || 'desc') });
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
    { label: 'SKU 名称', value: 'skuName' },
    { label: '操作类型', value: 'operationType' },
    { label: '业务类型', value: 'businessType' },
    { label: '业务单号', value: 'businessNo' },
    { label: '操作人', value: 'operatorName' },
    { label: '变更量', value: 'changeQuantity' },
    { label: '时间', value: 'createdAt' },
  ]);
};
const formatChangeQuantity = (quantity) => {
  const value = Number(quantity || 0);
  return value > 0 ? `+${value}` : String(value);
};
const openDetail = (row) => {
  selectedLog.value = row;
  detailDialogVisible.value = true;
};
const handleSearch = async () => { pager.page = 1; await syncRoute(); await loadLogs(); };
const handleReset = async () => { filters.skuId = ''; filters.operationType = ''; filters.startTime = ''; filters.endTime = ''; query.sortBy = 'id'; query.sortOrder = 'desc'; pager.page = 1; pager.size = ADMIN_PAGE_SIZE; await syncRoute(); await loadLogs(); };
const handlePageChange = async (page) => { pager.page = page; await syncRoute(); await loadLogs(); };
const handleSizeChange = async (size) => { pager.size = size; pager.page = 1; await syncRoute(); await loadLogs(); };
const handleSortChange = async ({ prop, order }) => { query.sortBy = prop || 'id'; query.sortOrder = order === 'descending' ? 'desc' : 'asc'; pager.page = 1; await syncRoute(); await loadLogs(); };
const handleLogout = async () => { await adminStore.logout(); router.push('/login'); };

onMounted(loadLogs);
</script>

<style scoped>
.stock-log-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
}

.stock-log-toolbar__label {
  color: #64748b;
  font-size: 13px;
  white-space: nowrap;
}

:deep(.stock-log-table) {
  width: 100%;
}

:deep(.stock-log-table .el-table__body),
:deep(.stock-log-table .el-table__header) {
  table-layout: fixed !important;
}

:deep(.stock-log-table .el-table__cell) {
  overflow: hidden;
}

:deep(.stock-log-table .cell) {
  display: block;
  max-width: 100%;
  overflow: hidden !important;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.stock-log-table .el-tooltip__trigger) {
  display: block !important;
  max-width: 100%;
  overflow: hidden !important;
}

.stock-log-ellipsis-cell {
  display: block;
  width: 100%;
  min-width: 0;
  overflow: hidden;
  color: #606266;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stock-log-empty {
  color: #94a3b8;
}

:deep(.stock-log-table .el-table__row) {
  height: 52px;
}

.detail-long-text {
  display: inline-block;
  max-width: 100%;
  word-break: break-all;
}

.stock-log-detail :deep(.el-descriptions__label) {
  width: 110px;
}
</style>

<style>
.stock-log-business-tooltip.el-popper.is-light {
  max-width: 360px;
  padding: 8px 12px;
  border: 1px solid rgba(99, 102, 241, 0.18);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 10px 28px rgba(79, 70, 229, 0.16), 0 4px 12px rgba(15, 23, 42, 0.08);
  color: #1e293b;
  line-height: 1.45;
}

.stock-log-business-tooltip.el-popper.is-light .el-popper__arrow::before {
  border-color: rgba(99, 102, 241, 0.18);
  background: rgba(255, 255, 255, 0.98);
}

.stock-log-business-tooltip__text {
  display: block;
  word-break: break-all;
  font-size: 12px;
  letter-spacing: 0.01em;
  user-select: text;
}
</style>
