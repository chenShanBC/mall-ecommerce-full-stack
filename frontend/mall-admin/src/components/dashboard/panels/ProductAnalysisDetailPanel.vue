<template>
  <article class="dashboard-card product-analysis-detail-panel">
    <div class="card-head product-analysis-detail-panel__head">
      <div>
        <h3>商品经营明细</h3>
        <p>按当前热销 / 滞销阈值统计商品表现，聚合 SKU 库存风险，支持筛选和快速处理。</p>
      </div>
      <div class="product-analysis-detail-panel__actions">
        <el-button size="small" @click="exportCurrentRows">导出明细</el-button>
        <el-button size="small" type="primary" plain @click="$emit('go-products', currentProductQuery)">进入商品管理</el-button>
      </div>
    </div>

    <el-form :inline="true" :model="filters" class="product-analysis-filter">
      <el-form-item label="关键词">
        <el-input v-model="filters.keyword" clearable placeholder="商品名称 / SPU ID" style="width: 190px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="类目">
        <el-select v-model="filters.categoryId" clearable filterable placeholder="全部类目" style="width: 168px">
          <el-option v-for="item in categories" :key="item.id" :label="item.name || item.categoryName" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="销售表现">
        <el-select v-model="filters.salesBand" clearable placeholder="全部" style="width: 136px">
          <el-option label="热销商品" value="HOT" />
          <el-option label="常规商品" value="NORMAL" />
          <el-option label="滞销商品" value="LOW" />
        </el-select>
      </el-form-item>
      <el-form-item label="库存状态">
        <el-select v-model="filters.stockRisk" clearable placeholder="全部" style="width: 138px">
          <el-option label="库存异常" value="ABNORMAL" />
          <el-option label="库存正常" value="NORMAL" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="filters.status" clearable placeholder="全部" style="width: 118px">
          <el-option label="上架" value="ONLINE" />
          <el-option label="下架" value="OFFLINE" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-checkbox v-model="filters.riskOnly">仅看风险商品</el-checkbox>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="product-analysis-table-wrap">
      <el-table
        v-loading="loading"
        :data="pagedRows"
        class="dashboard-table product-analysis-table"
        height="330"
        empty-text="暂无商品经营明细"
        :row-class-name="rowClassName"
        @sort-change="handleSortChange"
      >
        <el-table-column prop="id" label="SPU ID" width="72" sortable="custom" />
        <el-table-column prop="name" label="商品名称" min-width="112" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="类目" width="76" show-overflow-tooltip />
        <el-table-column label="状态" width="78" align="center">
          <template #default="{ row }"><el-tag size="small" :type="statusMeta(row.status).type" effect="light">{{ statusMeta(row.status).label }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="skuCount" label="SKU" width="64" align="right" sortable="custom" />
        <el-table-column prop="monthlySalesCount" label="月销" width="84" align="right" sortable="custom">
          <template #default="{ row }"><strong :class="['product-analysis-sales', `product-analysis-sales--${row.salesBand.toLowerCase()}`]">{{ row.monthlySalesCount }}</strong></template>
        </el-table-column>
        <el-table-column label="销售" width="90" align="center">
          <template #default="{ row }"><span :class="['product-analysis-band', `product-analysis-band--${row.salesBand.toLowerCase()}`]">{{ salesBandShortLabel(row.salesBand) }}</span></template>
        </el-table-column>
        <el-table-column prop="totalStock" label="总库存" width="90" align="right" sortable="custom" />
        <el-table-column prop="availableStock" label="可用" width="84" align="right" sortable="custom" />
        <el-table-column label="库存" width="94" align="center">
          <template #default="{ row }">
            <el-tooltip :content="riskTooltip(row)" placement="top" effect="dark">
              <el-tag size="small" :type="stockRiskMeta(row.stockRisk).type" effect="light">{{ stockRiskShortLabel(row.stockRisk) }}</el-tag>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="风险SKU" width="86" align="right">
          <template #default="{ row }"><strong :class="{ 'product-analysis-risk-count': row.riskSkuCount > 0 }">{{ row.riskSkuCount }} / {{ row.skuCount }}</strong></template>
        </el-table-column>
        <el-table-column label="操作" width="60" align="center">
          <template #default="{ row }">
            <el-button size="small" link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="product-analysis-pagination">
      <span>共 {{ filteredRows.length }} 条</span>
      <el-pagination v-model:current-page="pager.page" v-model:page-size="pager.size" size="small" background layout="sizes, prev, pager, next" :page-sizes="[5, 10, 20, 50]" :total="filteredRows.length" />
    </div>

    <el-dialog draggable v-model="detailVisible" title="商品经营详情" width="1180px" append-to-body destroy-on-close align-center class="product-analysis-detail-dialog">
      <div v-if="currentRow" class="product-analysis-detail">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="商品名称" :span="2">{{ currentRow.name }}</el-descriptions-item>
          <el-descriptions-item label="SPU ID">{{ currentRow.id }}</el-descriptions-item>
          <el-descriptions-item label="类目">{{ currentRow.categoryName }}</el-descriptions-item>
          <el-descriptions-item label="商品状态"><el-tag size="small" :type="statusMeta(currentRow.status).type">{{ statusMeta(currentRow.status).label }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="销售表现"><span :class="['product-analysis-band', `product-analysis-band--${currentRow.salesBand.toLowerCase()}`]">{{ salesBandMeta(currentRow.salesBand).label }}</span></el-descriptions-item>
          <el-descriptions-item label="月销量">{{ currentRow.monthlySalesCount }}</el-descriptions-item>
          <el-descriptions-item label="总库存">{{ currentRow.totalStock }}</el-descriptions-item>
          <el-descriptions-item label="可用库存">{{ currentRow.availableStock }}</el-descriptions-item>
        </el-descriptions>
        <div class="product-analysis-detail__title">SKU 库存明细</div>
        <el-table :data="detailSkuRows" height="280" empty-text="暂无 SKU 明细">
          <el-table-column prop="skuId" label="SKU ID" width="100" />
          <el-table-column prop="skuName" label="SKU名称" min-width="190" show-overflow-tooltip />
          <el-table-column prop="monthlySalesCount" label="月销量" width="90" align="right" />
          <el-table-column prop="totalStock" label="总库存" width="90" align="right" />
          <el-table-column prop="lockedStock" label="锁定库存" width="90" align="right" />
          <el-table-column prop="availableStock" label="可用库存" width="90" align="right" />
          <el-table-column prop="lowStockThreshold" label="低库存阈值" width="110" align="right" />
          <el-table-column prop="highStockThreshold" label="高库存阈值" width="110" align="right" />
          <el-table-column label="库存预警" width="110"><template #default="{ row }"><el-tag size="small" :type="stockRiskMeta(row.warningStatus).type" effect="light">{{ stockRiskMeta(row.warningStatus).label }}</el-tag></template></el-table-column>
        </el-table>
      </div>
      <template #footer><el-button @click="detailVisible = false">关闭</el-button><el-button type="primary" @click="$emit('go-products', productQuery(currentRow))">进入商品管理</el-button></template>
    </el-dialog>
  </article>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { fetchAdminCategories, fetchAdminProductDetail, fetchAdminProductPage, fetchAdminStocks } from '../../../api';
import { exportRowsToCsv } from '../../../utils/export';
import { getStatusTagMeta } from '../../../utils/status';

const props = defineProps({
  hotSalesThreshold: { type: Number, default: 100 },
  lowSalesThreshold: { type: Number, default: 10 },
  externalFilter: { type: Object, default: () => ({}) },
});

defineEmits(['go-products']);

const loading = ref(false);
const rawRows = ref([]);
const productSkuMap = ref(new Map());
const categories = ref([]);
const detailVisible = ref(false);
const currentRow = ref(null);
const detailSkuRows = ref([]);
const pager = reactive({ page: 1, size: 5 });
const sortState = reactive({ prop: 'monthlySalesCount', order: 'descending' });
const filters = reactive({ keyword: '', categoryId: '', salesBand: '', stockRisk: '', status: '', riskOnly: false });

const pickFirst = (row, keys, fallback = '') => keys.map((key) => row?.[key]).find((value) => value !== undefined && value !== null && value !== '') ?? fallback;
const toNumber = (value, fallback = 0) => Number.isFinite(Number(value)) ? Number(value) : fallback;
const statusMeta = (status) => getStatusTagMeta('productStatus', status);
const salesBandMeta = (band) => ({ HOT: { label: '热销商品' }, NORMAL: { label: '常规商品' }, LOW: { label: '滞销商品' } }[band] || { label: '常规商品' });
const salesBandShortLabel = (band) => ({ HOT: '热销', NORMAL: '常规', LOW: '滞销' }[band] || '常规');
const stockRiskMeta = (risk) => ({ ABNORMAL: { label: '库存异常', type: 'danger' }, LOW: { label: '低库存', type: 'danger' }, HIGH: { label: '高库存', type: 'warning' }, WARNING: { label: '预警', type: 'warning' }, NORMAL: { label: '库存正常', type: 'success' } }[risk] || { label: '库存正常', type: 'success' });
const stockRiskShortLabel = (risk) => ({ ABNORMAL: '异常', LOW: '低库存', HIGH: '高库存', WARNING: '预警', NORMAL: '正常' }[risk] || '正常');
const categoryNameOf = (row) => pickFirst(row, ['categoryName', 'categoryTitle', 'productTypeName'], categories.value.find((item) => String(item.id) === String(row.categoryId))?.name || '--');
const resolveSalesBand = (row) => {
  const band = String(row.salesBand || '').toUpperCase();
  if (['HOT', 'NORMAL', 'LOW'].includes(band)) return band;
  const monthlySales = toNumber(pickFirst(row, ['monthlySalesCount', 'monthly_sales_count', 'recent30DaySalesCount', 'salesCount'], 0));
  if (monthlySales >= props.hotSalesThreshold) return 'HOT';
  if (monthlySales <= props.lowSalesThreshold) return 'LOW';
  return 'NORMAL';
};
const normalizeRow = (row) => {
  const productId = String(pickFirst(row, ['id', 'productId', 'spuId'], ''));
  const skuRows = productSkuMap.value.get(productId) || [];
  const fallbackAvailableStock = toNumber(pickFirst(row, ['availableStock', 'stock'], 0));
  const fallbackLockedStock = toNumber(pickFirst(row, ['lockedStock'], 0));
  const fallbackTotalStock = toNumber(pickFirst(row, ['totalStock'], fallbackAvailableStock + fallbackLockedStock));
  const availableStock = skuRows.length ? skuRows.reduce((sum, sku) => sum + toNumber(sku.availableStock), 0) : fallbackAvailableStock;
  const lockedStock = skuRows.length ? skuRows.reduce((sum, sku) => sum + toNumber(sku.lockedStock), 0) : fallbackLockedStock;
  const totalStock = skuRows.length ? skuRows.reduce((sum, sku) => sum + toNumber(sku.totalStock), 0) : fallbackTotalStock;
  const lowStockSkuCount = skuRows.length ? skuRows.filter((sku) => sku.warningStatus === 'LOW').length : toNumber(pickFirst(row, ['lowStockSkuCount', 'lowStockCount'], 0));
  const highStockSkuCount = skuRows.length ? skuRows.filter((sku) => sku.warningStatus === 'HIGH').length : toNumber(pickFirst(row, ['highStockSkuCount', 'highStockCount'], 0));
  const skuCount = Math.max(skuRows.length || toNumber(pickFirst(row, ['skuCount'], 1), 1), 1);
  const stockRisk = lowStockSkuCount > 0 || highStockSkuCount > 0 ? 'ABNORMAL' : 'NORMAL';
  return {
    ...row,
    id: pickFirst(row, ['id', 'productId', 'spuId'], '--'),
    name: pickFirst(row, ['name', 'productName', 'spuName', 'title'], '--'),
    categoryName: categoryNameOf(row),
    skuCount,
    monthlySalesCount: toNumber(pickFirst(row, ['monthlySalesCount', 'monthly_sales_count', 'recent30DaySalesCount', 'salesCount'], 0)),
    salesBand: resolveSalesBand(row),
    totalStock,
    lockedStock,
    availableStock,
    lowStockSkuCount,
    highStockSkuCount,
    normalStockSkuCount: Math.max(skuCount - lowStockSkuCount - highStockSkuCount, 0),
    riskSkuCount: lowStockSkuCount + highStockSkuCount,
    stockRisk,
  };
};

const normalizedRows = computed(() => rawRows.value.map(normalizeRow));
const filteredRows = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase();
  return normalizedRows.value.filter((row) => {
    if (keyword && !String(`${row.id} ${row.name}`).toLowerCase().includes(keyword)) return false;
    if (filters.categoryId && String(row.categoryId) !== String(filters.categoryId)) return false;
    if (filters.salesBand && row.salesBand !== filters.salesBand) return false;
    if (filters.stockRisk && row.stockRisk !== filters.stockRisk) return false;
    if (filters.status && String(row.status || '').toUpperCase() !== filters.status) return false;
    if (filters.riskOnly && row.riskSkuCount <= 0) return false;
    return true;
  }).sort((a, b) => {
    const prop = sortState.prop || 'monthlySalesCount';
    const direction = sortState.order === 'ascending' ? 1 : -1;
    return (toNumber(a[prop]) - toNumber(b[prop])) * direction;
  });
});
const pagedRows = computed(() => filteredRows.value.slice((pager.page - 1) * pager.size, pager.page * pager.size));
const currentProductQuery = computed(() => ({ keyword: filters.keyword || undefined, categoryId: filters.categoryId || undefined, salesBand: filters.salesBand || undefined, stockWarningStatus: filters.stockRisk || undefined, status: filters.status || undefined, page: 1 }));

const loadRows = async () => {
  loading.value = true;
  try {
    const { data } = await fetchAdminProductPage({ page: 1, size: 1000, hotSalesThreshold: props.hotSalesThreshold, lowSalesThreshold: props.lowSalesThreshold });
    const productPayload = data?.data || {};
    const products = productPayload.records || productPayload.list || [];
    rawRows.value = products;
    const entries = await Promise.all(products.map(async (row) => {
      const productId = String(pickFirst(row, ['id', 'productId', 'spuId'], ''));
      if (!productId) return null;
      try {
        const detailRes = await fetchAdminProductDetail(productId);
        return [productId, (detailRes?.data?.data?.skus || []).map(normalizeSku)];
      } catch (error) {
        return [productId, []];
      }
    }));
    productSkuMap.value = new Map(entries.filter(Boolean));
  } finally {
    loading.value = false;
  }
};
const loadCategories = async () => {
  const { data } = await fetchAdminCategories();
  categories.value = data?.data || [];
};
const handleSearch = () => { pager.page = 1; };
const handleReset = () => {
  Object.assign(filters, { keyword: '', categoryId: '', salesBand: '', stockRisk: '', status: '', riskOnly: false });
  pager.page = 1;
};
const handleSortChange = ({ prop, order }) => {
  sortState.prop = prop || 'monthlySalesCount';
  sortState.order = order || 'descending';
};
const rowClassName = ({ row }) => row.stockRisk === 'ABNORMAL' ? 'product-analysis-table__row--abnormal' : '';
const riskTooltip = (row) => `低库存 SKU：${row.lowStockSkuCount} 个；高库存 SKU：${row.highStockSkuCount} 个；正常 SKU：${row.normalStockSkuCount} 个`;
const productQuery = (row) => ({ keyword: row?.name || row?.id || filters.keyword || undefined, page: 1 });
const resolveSkuWarningStatus = (sku, availableStock, lowStockThreshold, highStockThreshold) => {
  const rawWarningStatus = String(pickFirst(sku, ['warningStatus', 'stockWarningStatus'], '')).toUpperCase();
  if (['LOW', 'HIGH', 'NORMAL', 'WARNING'].includes(rawWarningStatus)) return rawWarningStatus;
  if (availableStock <= lowStockThreshold) return 'LOW';
  if (highStockThreshold > 0 && availableStock >= highStockThreshold) return 'HIGH';
  return 'NORMAL';
};
const normalizeSku = (sku) => {
  const availableStock = toNumber(pickFirst(sku, ['availableStock', 'stock'], 0));
  const lockedStock = toNumber(pickFirst(sku, ['lockedStock'], 0));
  const totalStock = toNumber(pickFirst(sku, ['totalStock'], availableStock + lockedStock));
  const lowStockThreshold = toNumber(pickFirst(sku, ['lowStockThreshold', 'warningThreshold'], 10));
  const highStockThreshold = toNumber(pickFirst(sku, ['highStockThreshold'], 0));
  const warningStatus = resolveSkuWarningStatus(sku, availableStock, lowStockThreshold, highStockThreshold);
  return { ...sku, skuId: pickFirst(sku, ['id', 'skuId', 'skuCode'], '--'), skuName: pickFirst(sku, ['skuName', 'name', 'skuCode'], '--'), monthlySalesCount: toNumber(pickFirst(sku, ['monthlySalesCount', 'salesCount'], 0)), totalStock, lockedStock, availableStock, lowStockThreshold, highStockThreshold, warningStatus };
};
const openDetail = async (row) => {
  currentRow.value = row;
  detailVisible.value = true;
  detailSkuRows.value = [];
  try {
    const { data } = await fetchAdminProductDetail(row.id);
    detailSkuRows.value = (data?.data?.skus || []).map(normalizeSku);
  } catch (error) {
    detailSkuRows.value = [];
  }
};
const exportCurrentRows = () => {
  const columns = [
    { label: 'SPU ID', value: 'id' },
    { label: '商品名称', value: 'name' },
    { label: '商品类目', value: 'categoryName' },
    { label: '商品状态', value: (row) => statusMeta(row.status).label },
    { label: 'SKU数', value: 'skuCount' },
    { label: '月销量', value: 'monthlySalesCount' },
    { label: '销售表现', value: (row) => salesBandMeta(row.salesBand).label },
    { label: '总库存', value: 'totalStock' },
    { label: '可用库存', value: 'availableStock' },
    { label: '库存风险', value: (row) => stockRiskMeta(row.stockRisk).label },
    { label: '风险SKU', value: 'riskSkuCount' },
  ];
  exportRowsToCsv(`商品经营明细-${new Date().toISOString().slice(0, 10)}`, filteredRows.value, columns);
  ElMessage.success('已导出当前筛选结果');
};

watch(() => props.externalFilter, (value = {}) => {
  if (!Object.keys(value || {}).length) return;
  Object.assign(filters, { ...filters, ...value });
  pager.page = 1;
}, { deep: true });
watch(() => [props.hotSalesThreshold, props.lowSalesThreshold], loadRows);

onMounted(async () => {
  await loadCategories();
  await loadRows();
});
</script>

<style scoped>
.product-analysis-detail-panel {
  position: relative;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  margin-top: 18px;
  padding: 18px;
  overflow: hidden;
  box-sizing: border-box;
  border-radius: 26px;
  border: 1px solid rgba(255,255,255,.84);
  background: linear-gradient(180deg, rgba(255,255,255,.94), rgba(248,250,255,.88));
  box-shadow: 0 18px 50px rgba(148,163,184,.14);
  backdrop-filter: blur(18px);
}
.product-analysis-detail-panel::before {
  content: "";
  position: absolute;
  top: -80px;
  right: -90px;
  width: 210px;
  height: 210px;
  border-radius: 999px;
  background: radial-gradient(circle, rgba(249,115,22,.16), rgba(249,115,22,0) 68%);
  pointer-events: none;
}
.product-analysis-detail-panel::after {
  content: "";
  position: absolute;
  left: 18px;
  right: 18px;
  top: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,.92), transparent);
  pointer-events: none;
}
.product-analysis-detail-panel__head {
  position: relative;
  z-index: 1;
  gap: 12px;
  align-items: flex-start;
  margin-bottom: 14px;
}
.product-analysis-detail-panel__head > div:first-child {
  min-width: 0;
}
.product-analysis-detail-panel__head h3 {
  max-width: 100%;
  margin: 0;
  font-size: 18px;
  color: #111827;
}
.product-analysis-detail-panel__head p {
  max-width: 100%;
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: #94a3b8;
}
.product-analysis-detail-panel__actions {
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.product-analysis-filter {
  position: relative;
  z-index: 1;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 10px;
  width: 100%;
  max-width: 100%;
  padding: 12px 14px 4px;
  margin-bottom: 12px;
  overflow: hidden;
  box-sizing: border-box;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(255,247,237,.72), rgba(248,250,252,.88), rgba(239,246,255,.62));
  border: 1px solid rgba(226,232,240,.78);
  box-shadow: inset 0 1px 0 rgba(255,255,255,.72), 0 10px 22px rgba(15,23,42,.045);
}
.product-analysis-filter :deep(.el-form-item) {
  margin-right: 0;
  margin-bottom: 8px;
}
.product-analysis-filter :deep(.el-form-item__label) {
  padding-right: 6px;
  font-size: 12px;
  color: #64748b;
}
.product-analysis-filter :deep(.el-input),
.product-analysis-filter :deep(.el-select) {
  max-width: 100%;
}
.product-analysis-table-wrap {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow: hidden;
  border: 1px solid rgba(226,232,240,.78);
  border-radius: 20px;
  background: rgba(255,255,255,.88);
  box-shadow: 0 12px 28px rgba(15,23,42,.055);
}
.product-analysis-table {
  width: 100%;
  max-width: 100%;
  font-size: 12px;
}
.product-analysis-table :deep(.el-table__inner-wrapper) {
  max-width: 100%;
}
.product-analysis-table :deep(.el-table__header th) {
  height: 44px;
  padding: 8px 0;
  color: #64748b;
  font-size: 12px;
  font-weight: 800;
  background: #f8fafc;
}
.product-analysis-table :deep(.el-table__body .el-table__cell) {
  padding: 14px 0;
}
.product-analysis-table :deep(.cell) {
  padding: 0 5px;
  line-height: 20px;
}
.product-analysis-table :deep(.el-button + .el-button) {
  margin-left: 4px;
}
.product-analysis-table :deep(.product-analysis-table__row--low) {
  --el-table-tr-bg-color: rgba(254, 242, 242, .82);
}
.product-analysis-table :deep(.product-analysis-table__row--high) {
  --el-table-tr-bg-color: rgba(250, 245, 255, .82);
}
.product-analysis-sales { font-weight: 900; }
.product-analysis-sales--hot { color: #ea580c; }
.product-analysis-sales--normal { color: #2563eb; }
.product-analysis-sales--low { color: #64748b; }
.product-analysis-band {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 54px;
  height: 22px;
  padding: 0 7px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 800;
  white-space: nowrap;
}
.product-analysis-band--hot { color: #c2410c; background: #ffedd5; }
.product-analysis-band--normal { color: #1d4ed8; background: #dbeafe; }
.product-analysis-band--low { color: #475569; background: #e2e8f0; }
.product-analysis-risk-count { color: #dc2626; }
.product-analysis-pagination {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  max-width: 100%;
  padding-top: 12px;
  overflow: hidden;
  color: #64748b;
  font-size: 12px;
}
.product-analysis-pagination :deep(.el-pagination) {
  max-width: calc(100% - 56px);
  overflow-x: auto;
  justify-content: flex-end;
}
.product-analysis-detail__title { margin: 18px 0 10px; font-weight: 900; color: #0f172a; }
@media (max-width: 1280px) {
  .product-analysis-filter :deep(.el-input),
  .product-analysis-filter :deep(.el-select) {
    width: 128px !important;
  }
}
@media (max-width: 960px) {
  .product-analysis-detail-panel__head {
    flex-direction: column;
  }
  .product-analysis-detail-panel__actions {
    justify-content: flex-start;
  }
  .product-analysis-pagination {
    align-items: flex-start;
    flex-direction: column;
  }
  .product-analysis-pagination :deep(.el-pagination) {
    max-width: 100%;
    justify-content: flex-start;
  }
}
</style>
