<template>
  <AdminLayout title="库存日志" @refresh="loadLogs" @logout="handleLogout">
    <el-card class="admin-page-card">
      <el-form :inline="true" :model="filters" class="admin-filter-form">
        <el-form-item label="SKU ID"><el-input v-model="filters.skuId" clearable placeholder="请输入 SKU ID" /></el-form-item>
        <el-form-item label="操作类型"><el-input v-model="filters.operationType" clearable placeholder="请输入操作类型" /></el-form-item>
        <el-form-item label="开始时间"><el-date-picker v-model="filters.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" clearable /></el-form-item>
        <el-form-item label="结束时间"><el-date-picker v-model="filters.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" clearable /></el-form-item>
        <el-form-item><el-button type="primary" @click="handleSearch">查询</el-button></el-form-item>
        <el-form-item><el-button @click="handleReset">重置</el-button></el-form-item>
        <el-form-item><el-button @click="exportLogs">导出</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <div class="admin-table-scroll">
        <el-table v-loading="loading" :data="logs" class="admin-table admin-table--safe admin-table--xwide stock-log-table" empty-text="暂无库存日志数据" @sort-change="handleSortChange">
        <el-table-column prop="id" label="ID" width="70" fixed="left" sortable="custom" />
        <el-table-column prop="skuId" label="SKU ID" width="86" fixed="left" sortable="custom" />
        <el-table-column prop="skuName" label="SKU 名称" min-width="160" fixed="left" show-overflow-tooltip sortable="custom" />
        <el-table-column prop="operationType" label="操作类型" min-width="150" sortable="custom">
          <template #default="{ row }">
            <el-tooltip v-if="row.operationType" effect="light" placement="bottom-start" :show-after="300" :offset="8" popper-class="stock-log-business-tooltip">
              <template #content><span class="stock-log-business-tooltip__text">{{ stockOperationTypeLabel(row.operationType) }}（{{ row.operationType }}）</span></template>
              <span class="stock-log-ellipsis-cell">{{ stockOperationTypeLabel(row.operationType) }}</span>
            </el-tooltip>
            <span v-else class="stock-log-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="businessType" label="业务类型" min-width="100" sortable="custom">
          <template #default="{ row }">
            <el-tooltip v-if="row.businessType" effect="light" placement="bottom-start" :show-after="300" :offset="8" popper-class="stock-log-business-tooltip">
              <template #content><span class="stock-log-business-tooltip__text">{{ stockBusinessTypeLabel(row.businessType) }}（{{ row.businessType }}）</span></template>
              <span class="stock-log-ellipsis-cell">{{ stockBusinessTypeLabel(row.businessType) }}</span>
            </el-tooltip>
            <span v-else class="stock-log-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="businessNo" label="业务单号" min-width="140">
          <template #default="{ row }">
            <el-tooltip v-if="row.businessNo" effect="light" placement="bottom-start" :show-after="300" :offset="8" popper-class="stock-log-business-tooltip">
              <template #content><span class="stock-log-business-tooltip__text">{{ row.businessNo }}</span></template>
              <span class="stock-log-ellipsis-cell">{{ row.businessNo }}</span>
            </el-tooltip>
            <span v-else class="stock-log-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="sourceType" label="来源" width="86">
          <template #default="{ row }">
            <el-tooltip v-if="row.sourceType" effect="light" placement="bottom-start" :show-after="300" :offset="8" popper-class="stock-log-business-tooltip">
              <template #content><span class="stock-log-business-tooltip__text">{{ row.sourceType }}</span></template>
              <span class="stock-log-ellipsis-cell">{{ row.sourceType }}</span>
            </el-tooltip>
            <span v-else class="stock-log-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="92" sortable="custom">
          <template #default="{ row }">
            <el-tooltip v-if="row.operatorName" effect="light" placement="bottom-start" :show-after="300" :offset="8" popper-class="stock-log-business-tooltip">
              <template #content><span class="stock-log-business-tooltip__text">{{ row.operatorName }}</span></template>
              <span class="stock-log-ellipsis-cell">{{ row.operatorName }}</span>
            </el-tooltip>
            <span v-else class="stock-log-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="changeQuantity" label="变更量" width="84" sortable="custom">
          <template #default="{ row }">
            <el-tag size="small" :type="Number(row.changeQuantity) >= 0 ? 'success' : 'danger'" effect="light">{{ formatChangeQuantity(row.changeQuantity) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="beforeAvailableStock" label="变更前可用" width="92" sortable="custom" />
        <el-table-column prop="afterAvailableStock" label="变更后可用" width="92" sortable="custom" />
        <el-table-column prop="remark" label="备注" min-width="110">
          <template #default="{ row }">
            <el-tooltip v-if="row.remark" effect="light" placement="bottom-start" :show-after="300" :offset="8" popper-class="stock-log-business-tooltip">
              <template #content><span class="stock-log-business-tooltip__text">{{ row.remark }}</span></template>
              <span class="stock-log-ellipsis-cell">{{ row.remark }}</span>
            </el-tooltip>
            <span v-else class="stock-log-empty">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="190" sortable="custom" class-name="stock-log-time-column" />
        <el-table-column label="操作" width="64" fixed="right" align="center">
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
        <el-descriptions-item label="操作类型">{{ selectedLog.operationType ? `${stockOperationTypeLabel(selectedLog.operationType)}（${selectedLog.operationType}）` : '-' }}</el-descriptions-item>
        <el-descriptions-item label="业务类型">{{ selectedLog.businessType ? `${stockBusinessTypeLabel(selectedLog.businessType)}（${selectedLog.businessType}）` : '-' }}</el-descriptions-item>
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
const stockOperationTypeOptions = [
  { value: 'INIT', label: '初始化库存' },
  { value: 'MANUAL_ADJUST', label: '手动调整' },
  { value: 'REPLENISH', label: '补货入库' },
  { value: 'INVENTORY_GAIN', label: '盘盈' },
  { value: 'INVENTORY_LOSS', label: '盘亏' },
  { value: 'RESERVE', label: '锁定库存' },
  { value: 'LOCK', label: '锁定库存' },
  { value: 'RELEASE', label: '释放库存' },
  { value: 'UNLOCK', label: '释放库存' },
  { value: 'MANUAL_UNLOCK', label: '手动解锁' },
  { value: 'CONFIRM', label: '确认扣减' },
  { value: 'DEDUCT', label: '扣减库存' },
  { value: 'FORCE_DEDUCT', label: '强制扣减' },
  { value: 'OUTBOUND', label: '出库' },
  { value: 'SHIP', label: '发货出库' },
  { value: 'DELIVERY', label: '配送出库' },
  { value: 'POLICY_UPDATE', label: '库存策略调整' },
  { value: 'STOCK_POLICY_UPDATE', label: '库存策略调整' },
  { value: 'WARNING_HANDLE', label: '预警处理' },
  { value: 'CONSISTENCY_CHECK', label: '一致性校验' },
  { value: 'RECONCILE_REPAIR', label: '对账修复' },
  { value: 'OTHER', label: '其他操作' },
];
const stockOperationTypeMap = Object.fromEntries(stockOperationTypeOptions.map((item) => [item.value, item.label]));
const stockOperationSearchAliasMap = {
  初始化库存: 'INIT',
  初始化: 'INIT',
  手动调整: 'MANUAL_ADJUST',
  补货入库: 'REPLENISH',
  补货: 'REPLENISH',
  盘盈: 'INVENTORY_GAIN',
  盘亏: 'INVENTORY_LOSS',
  锁定库存: 'LOCK',
  锁定: 'LOCK',
  释放库存: 'RELEASE',
  释放: 'RELEASE',
  取消释放: 'CANCEL',
  取消: 'CANCEL',
  订单取消释放: 'ORDER_CANCEL_RELEASE',
  手动解锁: 'MANUAL_UNLOCK',
  确认扣减: 'CONFIRM_DEDUCT',
  扣减库存: 'DEDUCT',
  扣减: 'DEDUCT',
  强制扣减: 'FORCE_DEDUCT',
  出库: 'OUTBOUND',
  发货出库: 'SHIP',
  配送出库: 'DELIVERY',
  库存策略调整: 'STOCK_POLICY_UPDATE',
  策略调整: 'STOCK_POLICY_UPDATE',
  预警处理: 'WARNING_HANDLE',
  一致性校验: 'CONSISTENCY_CHECK',
  库存一致性处理: 'CONSISTENCY_CHECK',
  对账修复: 'RECONCILE_REPAIR',
  其他操作: 'OTHER',
};
const resolveOperationTypeForQuery = (value) => {
  const keyword = String(value || '').trim();
  if (!keyword) return undefined;
  const normalizedKeyword = keyword.replace(/\s+/g, '').toUpperCase();
  const exactAlias = stockOperationSearchAliasMap[keyword] || stockOperationSearchAliasMap[keyword.replace(/\s+/g, '')];
  if (exactAlias) return exactAlias;
  const matchedOption = stockOperationTypeOptions.find((item) => item.label.includes(keyword) || keyword.includes(item.label));
  if (matchedOption) return matchedOption.value;
  if (keyword.includes('确认') && keyword.includes('扣减')) return 'CONFIRM_DEDUCT';
  if (keyword.includes('补货')) return 'REPLENISH';
  if (keyword.includes('取消')) return 'CANCEL';
  if (keyword.includes('锁定')) return 'LOCK';
  if (keyword.includes('释放')) return 'RELEASE';
  if (keyword.includes('扣减')) return 'DEDUCT';
  if (keyword.includes('策略')) return 'STOCK_POLICY_UPDATE';
  if (keyword.includes('预警')) return 'WARNING_HANDLE';
  if (keyword.includes('一致') || keyword.includes('对账')) return 'CONSISTENCY_CHECK';
  return normalizedKeyword;
};
const stockOperationTypeLabel = (type) => {
  const value = String(type || '').trim();
  const upperValue = value.toUpperCase();
  if (!value) return '-';
  if (stockOperationTypeMap[upperValue]) return stockOperationTypeMap[upperValue];
  if (upperValue === 'CANCEL') return '取消释放';
  if (upperValue.includes('CONFIRM') && upperValue.includes('DEDUCT')) return '确认扣减';
  if (upperValue.includes('ORDER') && upperValue.includes('CREATE')) return '订单创建锁定';
  if (upperValue.includes('ORDER') && upperValue.includes('CANCEL')) return '订单取消释放';
  if (upperValue.includes('PAY') && upperValue.includes('TIMEOUT')) return '支付超时释放';
  if (upperValue.includes('RESERVE') || upperValue.includes('LOCK')) return '锁定库存';
  if (upperValue.includes('RELEASE') || upperValue.includes('UNLOCK')) return '释放库存';
  if (upperValue.includes('DEDUCT')) return '扣减库存';
  if (upperValue.includes('REPLENISH')) return '补货入库';
  if (upperValue.includes('POLICY')) return '库存策略调整';
  if (upperValue.includes('WARNING')) return '预警处理';
  if (upperValue.includes('RECONCILE') || upperValue.includes('CONSISTENCY')) return '库存一致性处理';
  if (upperValue.includes('INIT')) return '初始化库存';
  return value;
};
const stockBusinessTypeMap = {
  ORDER: '订单业务',
  PAY: '支付业务',
  CANCEL: '取消释放',
  REPLENISH: '补货入库',
  ADMIN: '后台管理',
  MANUAL: '人工操作',
  STOCK: '库存业务',
  RECONCILE: '库存对账',
  POLICY: '策略调整',
  WARNING: '预警处理',
  INIT: '初始化',
  SYSTEM: '系统任务',
  MQ_SYNC: '消息同步',
  TIMEOUT: '超时释放',
  AFTERSALE: '售后业务',
};
const stockBusinessTypeLabel = (type) => {
  const value = String(type || '').trim();
  const upperValue = value.toUpperCase();
  if (!value) return '-';
  if (stockBusinessTypeMap[upperValue]) return stockBusinessTypeMap[upperValue];
  if (upperValue.includes('ORDER')) return '订单业务';
  if (upperValue.includes('CANCEL')) return '取消释放';
  if (upperValue.includes('REPLENISH')) return '补货入库';
  if (upperValue.includes('POLICY')) return '策略调整';
  if (upperValue.includes('WARNING')) return '预警处理';
  if (upperValue.includes('RECONCILE')) return '库存对账';
  if (upperValue.includes('TIMEOUT')) return '超时释放';
  return value;
};
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
      operationType: resolveOperationTypeForQuery(filters.operationType),
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
  exportRowsToCsv('库存操作日志', logs.value.map((row) => ({ ...row, operationTypeText: stockOperationTypeLabel(row.operationType) })), [
    { label: 'ID', value: 'id' },
    { label: 'SKU ID', value: 'skuId' },
    { label: 'SKU 名称', value: 'skuName' },
    { label: '操作类型', value: 'operationTypeText' },
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

:deep(.stock-log-table .stock-log-time-column .cell) {
  overflow: visible !important;
  text-overflow: clip;
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
