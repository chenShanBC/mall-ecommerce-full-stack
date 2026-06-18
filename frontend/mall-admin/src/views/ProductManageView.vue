<template>
  <AdminLayout title="商品管理" @refresh="loadProducts" @logout="handleLogout">
    <el-card class="admin-page-card">
      <el-form :inline="true" :model="filters" class="admin-filter-form product-filter-form">
        <div class="product-filter-row product-filter-row--threshold">
          <span class="sales-threshold-default-tip">默认口径：热销≥{{ salesThresholdDefault.hotSalesThreshold }}，低销≤{{ salesThresholdDefault.lowSalesThreshold }}</span>
          <el-tag v-if="hasPersonalThreshold" type="warning" effect="light">当前使用本次登录视角</el-tag>
          <el-form-item label="热销≥"><el-input-number v-model="filters.hotSalesThreshold" :min="0" :step="10" controls-position="right" style="width: 130px" /></el-form-item>
          <el-form-item label="低销≤"><el-input-number v-model="filters.lowSalesThreshold" :min="0" :step="1" controls-position="right" style="width: 130px" /></el-form-item>
          <el-form-item><el-button @click="applyPersonalThreshold">保存本次登录视角</el-button></el-form-item>
          <el-form-item v-if="canManageSalesThreshold"><el-button type="warning" plain @click="openSalesThresholdDialog">配置默认阈值</el-button></el-form-item>
        </div>
        <div class="product-filter-row product-filter-row--main">
          <el-form-item label="关键词"><el-input v-model="filters.keyword" clearable placeholder="商品名称" /></el-form-item>
          <el-form-item label="类目"><el-select v-model="filters.categoryId" clearable style="width: 220px"><el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
          <el-form-item label="状态"><el-select v-model="filters.status" clearable style="width: 160px"><el-option label="上架" value="ONLINE" /><el-option label="下架" value="OFFLINE" /></el-select></el-form-item>
          <el-form-item label="销售表现"><el-select v-model="filters.salesBand" clearable style="width: 160px"><el-option label="热销商品" value="HOT" /><el-option label="常规商品" value="NORMAL" /><el-option label="低销商品" value="LOW" /></el-select></el-form-item>
          <el-form-item><el-button type="primary" @click="handleSearch">查询</el-button></el-form-item>
          <el-form-item><el-button @click="handleReset">重置</el-button></el-form-item>
          <el-form-item><el-button @click="exportProducts">导出</el-button></el-form-item>
          <el-form-item v-if="canCreate" class="product-create-action"><el-button type="primary" @click="openCreate">新增商品</el-button></el-form-item>
        </div>
      </el-form>
    </el-card>

    <el-card>
      <div class="admin-table-scroll">
        <el-table v-loading="loading" :data="products" class="admin-table admin-table--safe product-performance-table" :row-class-name="productRowClassName" empty-text="暂无商品数据" @sort-change="handleSortChange">
          <el-table-column prop="id" label="ID" width="80" sortable="custom" />
          <el-table-column prop="name" label="商品名称" min-width="220" sortable="custom" :show-overflow-tooltip="{ effect: 'light', placement: 'bottom-start', showAfter: 300, offset: 8, popperClass: 'admin-table-tooltip' }" />
          <el-table-column prop="categoryId" label="类目ID" width="100" sortable="custom" />
          <el-table-column prop="skuCount" label="SKU数" width="100" sortable="custom" />
          <el-table-column prop="monthlySalesCount" label="月销量" width="120" sortable="custom"><template #default="{ row }"><span class="monthly-sales" :class="`monthly-sales--${salesBand(row).toLowerCase()}`">{{ monthlySalesText(row) }}</span></template></el-table-column>
          <el-table-column prop="salesBand" label="销售表现" width="120"><template #default="{ row }"><span class="sales-band-pill" :class="`sales-band-pill--${salesBand(row).toLowerCase()}`">{{ salesBandLabel(row) }}</span></template></el-table-column>
          <el-table-column prop="salesCount" label="总销量" width="100" sortable="custom"><template #default="{ row }">{{ salesText(row) }}</template></el-table-column>
          <el-table-column prop="stock" label="库存" width="100" />
          <el-table-column prop="status" label="状态" width="120" sortable="custom"><template #default="{ row }"><el-tag :type="productStatusMeta(row.status).type">{{ productStatusMeta(row.status).label }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="280" fixed="right" align="center">
            <template #default="{ row }">
              <div class="product-actions">
                <el-button class="product-action-btn product-action-btn--detail" size="small" @click="openProductDetail(row.id)">详情</el-button>
                <el-button v-if="canEditProduct" class="product-action-btn product-action-btn--edit" size="small" @click="openEdit(row.id)">编辑</el-button>
                <el-button v-if="canUpdateStatus" class="product-action-btn" :class="row.status === 'ONLINE' ? 'product-action-btn--danger' : 'product-action-btn--success'" size="small" @click="openStatusDialog(row)">{{ row.status === 'ONLINE' ? '下架' : '上架' }}</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="admin-pagination"><el-pagination background layout="sizes, prev, pager, next, total" :current-page="pager.page" :page-size="pager.size" :page-sizes="ADMIN_PAGE_SIZES" :total="pager.total" @current-change="handlePageChange" @size-change="handleSizeChange" /></div>
    </el-card>

    <el-dialog v-model="detailVisible" title="商品详情" width="900px" append-to-body destroy-on-close align-center>
      <div v-if="productDetail" class="admin-detail">
        <div class="admin-detail__title">{{ productDetail.name }}</div>
        <div class="product-detail-top">
          <el-image
            v-if="productDetail.mainImageUrl"
            class="product-detail-image"
            :src="productDetail.mainImageUrl"
            :preview-src-list="[productDetail.mainImageUrl]"
            fit="cover"
            preview-teleported
          />
          <div v-else class="product-detail-image product-detail-image--empty">暂无图片</div>
          <div class="admin-detail__grid product-detail-meta">
            <div class="admin-detail__item"><span class="admin-detail__label">商品ID</span>{{ productDetail.id }}</div>
            <div class="admin-detail__item"><span class="admin-detail__label">状态</span><el-tag :type="productStatusMeta(productDetail.status).type">{{ productStatusMeta(productDetail.status).label }}</el-tag></div>
            <div class="admin-detail__item full"><span class="admin-detail__label">描述</span><span v-html="productDetail.description || '-'" /></div>
          </div>
        </div>
        <el-table :data="productDetail.skus || []" class="admin-table">
          <el-table-column prop="id" label="SKU ID" width="90" />
          <el-table-column prop="skuCode" label="SKU编码" width="160" />
          <el-table-column prop="skuName" label="SKU名称" min-width="180" />
          <el-table-column label="售价(元)" width="110"><template #default="{ row }">{{ centToYuan(row.salePriceCent) }}</template></el-table-column>
          <el-table-column prop="salesCount" label="销量" width="90" />
          <el-table-column prop="availableStock" label="可用库存" width="100" />
          <el-table-column prop="totalStock" label="总库存" width="100" />
        </el-table>
      </div>
    </el-dialog>

    <el-dialog v-model="editorVisible" :title="editorMode === 'create' ? '新增商品' : '编辑商品'" width="1080px" top="5vh" append-to-body destroy-on-close align-center>
      <el-alert class="mb12 compact-alert" type="info" :closable="false" show-icon title="价格以元展示，保存自动换算为分；已上架商品的初始库存锁定，日常库存请到库存管理模块操作。" />
      <el-form :model="productForm" label-width="90px" class="admin-dialog-form product-editor-form compact-product-editor">
        <div class="product-basic-grid">
          <el-form-item label="商品名称"><el-input v-model="productForm.name" maxlength="100" show-word-limit :disabled="!canEditBasic" /></el-form-item>
          <el-form-item label="类目"><el-select v-model="productForm.categoryId" class="full-control" :disabled="!canEditBasic"><el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
          <el-form-item v-if="editorMode === 'edit' && canUpdateStatus" label="SPU状态"><el-radio-group v-model="productForm.status"><el-radio-button value="ONLINE">上架</el-radio-button><el-radio-button value="OFFLINE">下架</el-radio-button></el-radio-group></el-form-item>
        </div>
        <el-form-item label="商品主图">
          <div class="single-image-uploader" :class="{ disabled: !canEditBasic }" @dragover.prevent @drop.prevent="handleImageDrop">
            <el-upload :auto-upload="false" accept="image/jpeg,image/png" :show-file-list="false" :disabled="!canEditBasic" :on-change="handleImageSelect">
              <el-button type="primary" :disabled="!canEditBasic" :loading="imageUploading">上传图片</el-button>
            </el-upload>
            <span class="upload-tip">支持 JPG/PNG，单张不超过2MB；上传成功后自动回填图片地址</span>
          </div>
          <div v-if="productForm.imagePreviewUrl || productForm.mainImageUrl" class="single-image-preview">
            <img :src="productForm.imagePreviewUrl || productForm.mainImageUrl" alt="商品主图" />
            <el-button v-if="canEditBasic" size="small" type="danger" text @click="removeImage">删除</el-button>
          </div>
          <el-input v-model="productForm.mainImageUrl" class="image-url-readonly" placeholder="图片URL由上传服务回填；暂无上传服务时请保留原地址或填写测试URL" :disabled="!canEditBasic" />
        </el-form-item>
        <el-form-item label="商品详情">
          <div class="editor-wrapper" :class="{ disabled: !canEditBasic }">
            <Toolbar class="editor-toolbar" :editor="richTextEditor" :default-config="toolbarConfig" mode="default" />
            <Editor v-model="productForm.description" class="editor-content" :default-config="editorConfig" mode="default" @onCreated="handleEditorCreated" />
          </div>
        </el-form-item>

        <div v-if="canEditSku" class="spec-panel">
          <div class="sku-header"><div class="sku-title">可视化规格配置</div><el-button size="small" @click="addSpecGroup">新增规格项</el-button></div>
          <div v-for="(group, groupIndex) in specGroups" :key="groupIndex" class="spec-group">
            <el-input v-model="group.name" placeholder="规格项，如颜色" maxlength="20" @input="generateSkus" />
            <div class="spec-values-wrap">
              <el-tag v-for="(value, valueIndex) in group.values" :key="valueIndex" closable @close="removeSpecValue(groupIndex, valueIndex)">{{ value }}</el-tag>
            </div>
            <el-input v-model="group.pending" class="spec-value-input" placeholder="规格值" @keyup.enter="addSpecValue(groupIndex)" />
            <el-button size="small" @click="addSpecValue(groupIndex)">添加值</el-button>
            <el-button size="small" type="danger" text @click="removeSpecGroup(groupIndex)">删除规格项</el-button>
          </div>
        </div>

        <div class="sku-header"><div class="sku-title">SKU列表</div><el-tag type="info">系统自动生成编码和名称</el-tag></div>
        <el-table :data="productForm.skus" class="admin-table sku-editor-table" empty-text="请先配置规格值生成SKU">
          <el-table-column label="规格组合" min-width="260">
            <template #default="{ row }">
              <div class="spec-combo-list">
                <span v-for="spec in specEntries(row)" :key="`${row.skuCode}-${spec.key}`" class="spec-combo-item">
                  <span class="spec-combo-key">{{ spec.key }}</span>
                  <span class="spec-combo-value">{{ spec.value }}</span>
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="skuCode" label="SKU编码" width="150" show-overflow-tooltip />
          <el-table-column prop="skuName" label="SKU名称" width="170" show-overflow-tooltip />
          <el-table-column label="售价(元)" width="150"><template #default="{ row }"><el-input v-model="row.salePriceYuan" class="sku-price-input" inputmode="decimal" placeholder="0.00" :disabled="!canEditSku" /></template></el-table-column>
          <el-table-column label="原价(元)" width="150"><template #default="{ row }"><el-input v-model="row.originPriceYuan" class="sku-price-input" inputmode="decimal" placeholder="0.00" :disabled="!canEditSku" /></template></el-table-column>
          <el-table-column label="初始库存" width="140"><template #default="{ row }"><el-input-number v-model="row.initialStock" :min="0" :step="1" :disabled="stockLocked(row) || !canEditSku" /></template></el-table-column>
          <el-table-column v-if="editorMode === 'edit'" label="SKU状态" width="150"><template #default="{ row }"><el-radio-group v-model="row.status" :disabled="!canEditSku" size="small"><el-radio-button value="ONLINE">启用</el-radio-button><el-radio-button value="OFFLINE">禁用</el-radio-button></el-radio-group></template></el-table-column>
        </el-table>
      </el-form>
      <template #footer><el-button @click="editorVisible = false">取消</el-button><el-button type="primary" :disabled="!canEditProduct" @click="submitProduct">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="statusVisible" title="商品状态变更" width="460px" append-to-body destroy-on-close align-center>
      <el-form :model="statusForm" label-width="100px" class="admin-dialog-form"><el-form-item label="目标状态"><el-select v-model="statusForm.status" style="width: 200px"><el-option label="上架" value="ONLINE" /><el-option label="下架" value="OFFLINE" /></el-select></el-form-item><el-form-item label="操作原因"><el-input v-model="statusForm.reason" type="textarea" :rows="3" /></el-form-item></el-form>
      <template #footer><el-button @click="statusVisible = false">取消</el-button><el-button type="primary" @click="submitStatus">提交</el-button></template>
    </el-dialog>

    <el-dialog v-model="salesThresholdVisible" title="商品销售表现默认阈值" width="460px" append-to-body destroy-on-close align-center>
      <el-alert class="mb12" type="info" :closable="false" show-icon title="仅 ADMIN / SUPER_ADMIN 可修改默认阈值；其他用户可在商品列表中保存本次登录视角，优先级高于默认阈值。" />
      <el-form :model="salesThresholdForm" label-width="110px" class="admin-dialog-form">
        <el-form-item label="热销商品≥"><el-input-number v-model="salesThresholdForm.hotSalesThreshold" :min="0" :step="10" controls-position="right" /></el-form-item>
        <el-form-item label="低销商品≤"><el-input-number v-model="salesThresholdForm.lowSalesThreshold" :min="0" :step="1" controls-position="right" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="salesThresholdVisible = false">取消</el-button><el-button type="primary" @click="submitSalesThresholdDefault">保存默认值</el-button></template>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import '@wangeditor/editor/dist/css/style.css';
import { computed, nextTick, onActivated, onBeforeUnmount, onMounted, reactive, ref, shallowRef } from 'vue';
import { Editor, Toolbar } from '@wangeditor/editor-for-vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import AdminLayout from '../components/AdminLayout.vue';
import { confirmAction } from '../utils/action';
import { exportRowsToCsv } from '../utils/export';
import { t } from '../utils/i18n';
import { ADMIN_PAGE_SIZE, ADMIN_PAGE_SIZES } from '../utils/pagination';
import { getStatusTagMeta } from '../utils/status';
import { createAdminProduct, fetchAdminCategories, fetchAdminProductDetail, fetchAdminProductPage, fetchAdminProductSalesThresholdConfig, updateAdminProduct, updateAdminProductSalesThresholdConfig, updateAdminProductStatus, uploadProductImage } from '../api';
import { useAdminStore } from '../stores/admin';
import { adminPageCache, isCacheFresh } from '../stores/pageCache';

const route = useRoute();
const router = useRouter();
const adminStore = useAdminStore();
const hasPerm = (permission) => adminStore.hasPermission(permission) || adminStore.hasPermission('product:update');
const canCreate = computed(() => adminStore.hasPermission('product:create'));
const canEditBasic = computed(() => editorMode.value === 'create' ? canCreate.value : hasPerm('product:update'));
const canEditSku = computed(() => editorMode.value === 'create' ? canCreate.value : hasPerm('product:update'));
const canUpdateStatus = computed(() => adminStore.hasAnyPermission(['product:status:update', 'product:on_sale', 'product:off_sale']));
const canEditProduct = computed(() => canEditBasic.value || canEditSku.value || canUpdateStatus.value);
const canManageSalesThreshold = computed(() => adminStore.hasPermission('product:sales-threshold:config'));
const products = ref(adminPageCache.products.list || []);
const loading = ref(false);
const categories = ref(adminPageCache.products.categories || []);
const detailVisible = ref(false);
const editorVisible = ref(false);
const statusVisible = ref(false);
const editorMode = ref('create');
const editingProductId = ref(null);
const currentProductId = ref(null);
const productDetail = ref(null);
const richTextEditor = shallowRef(null);
const specGroups = ref([]);
const originalEditSkus = ref([]);
const toolbarConfig = { excludeKeys: ['fullScreen'] };
const editorConfig = { placeholder: '请输入商品详情，支持图文混排和基础格式排版', MENU_CONF: {} };
const productStatusMeta = (status) => getStatusTagMeta('productStatus', status);
const DEFAULT_SALES_THRESHOLD = { hotSalesThreshold: 100, lowSalesThreshold: 10 };
const productSalesThresholdSessionKey = () => `mallfei-admin-product-sales-threshold-session:${adminStore.adminId || 'anonymous'}`;
const normalizeSalesThreshold = (value, fallback) => {
  const numberValue = Number(value);
  return Number.isFinite(numberValue) && numberValue >= 0 ? numberValue : fallback;
};
const normalizeThresholdConfig = (thresholds = {}) => ({
  hotSalesThreshold: normalizeSalesThreshold(thresholds.hotSalesThreshold, DEFAULT_SALES_THRESHOLD.hotSalesThreshold),
  lowSalesThreshold: normalizeSalesThreshold(thresholds.lowSalesThreshold, DEFAULT_SALES_THRESHOLD.lowSalesThreshold),
});
const loadSalesThresholdSession = () => {
  try {
    const saved = JSON.parse(sessionStorage.getItem(productSalesThresholdSessionKey()) || 'null');
    if (!saved) return null;
    return {
      hotSalesThreshold: normalizeSalesThreshold(saved.hotSalesThreshold, 100),
      lowSalesThreshold: normalizeSalesThreshold(saved.lowSalesThreshold, 10),
    };
  } catch {
    return null;
  }
};
const saveSalesThresholdSession = (thresholds) => {
  sessionStorage.setItem(productSalesThresholdSessionKey(), JSON.stringify({
    hotSalesThreshold: normalizeSalesThreshold(thresholds.hotSalesThreshold, 100),
    lowSalesThreshold: normalizeSalesThreshold(thresholds.lowSalesThreshold, 10),
  }));
};
const salesThresholdDefault = ref({ ...DEFAULT_SALES_THRESHOLD });
const hasPersonalThreshold = ref(Boolean(loadSalesThresholdSession()));
const salesThresholdBase = loadSalesThresholdSession() || salesThresholdDefault.value;
const filters = reactive({ ...adminPageCache.products.filters, keyword: String(route.query.keyword ?? adminPageCache.products.filters.keyword ?? ''), categoryId: route.query.categoryId ? Number(route.query.categoryId) : (adminPageCache.products.filters.categoryId ?? null), status: String(route.query.status ?? adminPageCache.products.filters.status ?? ''), salesBand: String(route.query.salesBand ?? adminPageCache.products.filters.salesBand ?? ''), hotSalesThreshold: normalizeSalesThreshold(route.query.hotSalesThreshold, salesThresholdBase.hotSalesThreshold), lowSalesThreshold: normalizeSalesThreshold(route.query.lowSalesThreshold, salesThresholdBase.lowSalesThreshold) });
const query = reactive({ sortBy: String(route.query.sortBy || adminPageCache.products.query?.sortBy || 'id'), sortOrder: String(route.query.sortOrder || adminPageCache.products.query?.sortOrder || 'desc') });
const pager = reactive({ page: Number(route.query.page || adminPageCache.products.pager.page || 1), size: Number(route.query.size || adminPageCache.products.pager.size || ADMIN_PAGE_SIZE), total: Number(adminPageCache.products.pager.total || 0) });
const refreshSalesThresholdState = () => {
  hasPersonalThreshold.value = Boolean(loadSalesThresholdSession());
};
const loadSalesThresholdDefault = async () => {
  try {
    const { data } = await fetchAdminProductSalesThresholdConfig();
    salesThresholdDefault.value = normalizeThresholdConfig(data?.data);
  } catch (error) {
    salesThresholdDefault.value = { ...DEFAULT_SALES_THRESHOLD };
    ElMessage.warning(error?.response?.data?.msg || error?.response?.data?.message || '商品销售默认口径加载失败，已使用兜底默认值');
  }
};
const resolveEffectiveThreshold = () => loadSalesThresholdSession() || salesThresholdDefault.value;
const applyRouteQuery = () => {
  refreshSalesThresholdState();
  const effectiveThreshold = resolveEffectiveThreshold();
  filters.keyword = String(route.query.keyword ?? '');
  filters.categoryId = route.query.categoryId ? Number(route.query.categoryId) : null;
  filters.status = String(route.query.status ?? '');
  filters.salesBand = String(route.query.salesBand ?? '');
  filters.hotSalesThreshold = normalizeSalesThreshold(route.query.hotSalesThreshold, effectiveThreshold.hotSalesThreshold);
  filters.lowSalesThreshold = normalizeSalesThreshold(route.query.lowSalesThreshold, effectiveThreshold.lowSalesThreshold);
  query.sortBy = String(route.query.sortBy || 'id');
  query.sortOrder = String(route.query.sortOrder || 'desc');
  pager.page = Number(route.query.page || 1);
  pager.size = Number(route.query.size || ADMIN_PAGE_SIZE);
};
const statusForm = reactive({ status: 'OFFLINE', reason: '' });
const salesThresholdVisible = ref(false);
const salesThresholdForm = reactive({ hotSalesThreshold: salesThresholdBase.hotSalesThreshold, lowSalesThreshold: salesThresholdBase.lowSalesThreshold });
const productForm = reactive({ name: '', categoryId: null, mainImageUrl: '', imagePreviewUrl: '', description: '', status: 'OFFLINE', skus: [] });
const imageUploading = ref(false);
const MAX_SKU_PRICE_YUAN = 100000;

const centToYuan = (cent) => ((Number(cent || 0) / 100).toFixed(2));
const yuanToCent = (yuan) => Math.trunc(Number(yuan || 0) * 100);
const countDecimalPlaces = (value) => {
  const text = String(value ?? '').trim();
  if (!text.includes('.')) return 0;
  return text.split('.')[1]?.length || 0;
};
const hasMoreThanTwoDecimals = (value) => countDecimalPlaces(value) > 2;
const validateSkuPrice = (value, label) => {
  const numberValue = Number(value);
  if (!Number.isFinite(numberValue)) return `${label}必须是有效数字`;
  if (numberValue < 0) return `${label}不能为负数`;
  if (numberValue === 0) return `${label}必须大于0`;
  if (numberValue > MAX_SKU_PRICE_YUAN) return `${label}不能超过10万元`;
  if (hasMoreThanTwoDecimals(value)) return `${label}最多保留两位小数`;
  return '';
};
const salesText = (row) => row.salesCount ?? row.sales_count ?? row.totalSales ?? row.salesVolume ?? 0;
const monthlySalesText = (row) => row.monthlySalesCount ?? row.monthly_sales_count ?? salesText(row);
const salesBand = (row) => String(row.salesBand || resolveSalesBand(monthlySalesText(row))).toUpperCase();
const salesBandLabel = (row) => row.salesBandLabel || ({ HOT: '热销', LOW: '低销', NORMAL: '常规' }[salesBand(row)] || '常规');
const resolveSalesBand = (monthlySalesCount) => {
  const monthlySales = Number(monthlySalesCount || 0);
  if (monthlySales >= Number(filters.hotSalesThreshold || 100)) return 'HOT';
  if (monthlySales <= Number(filters.lowSalesThreshold || 10)) return 'LOW';
  return 'NORMAL';
};
const productRowClassName = ({ row }) => `product-row--${salesBand(row).toLowerCase()}`;
const sanitizeHtml = (html) => String(html || '').replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '');
const safeParseSpec = (specJson) => { try { return JSON.parse(specJson || '{}') || {}; } catch { return {}; } };
const specEntries = (sku) => Object.entries(safeParseSpec(sku.specJson)).map(([key, value]) => ({ key, value }));
const normalizeProductStatus = (status) => String(status || 'OFFLINE').trim().toUpperCase() === 'ONLINE' ? 'ONLINE' : 'OFFLINE';
const normalizeSkuStatus = (status) => String(status || 'ONLINE').trim().toUpperCase() === 'OFFLINE' ? 'OFFLINE' : 'ONLINE';
const stockLocked = (sku) => editorMode.value === 'edit' && Boolean(sku.id);
const normalizeUploadPath = (url) => {
  const value = String(url || '').trim();
  if (!value) return '';
  if (/^(https?:)?\/\//i.test(value) || value.startsWith('data:') || value.startsWith('blob:')) return value;
  return value.startsWith('/') ? value : `/${value}`;
};
const resolveImageUrl = (url) => normalizeUploadPath(url);
const resetForm = () => { productForm.name = ''; productForm.categoryId = null; productForm.mainImageUrl = ''; productForm.imagePreviewUrl = ''; productForm.description = ''; productForm.status = 'OFFLINE'; productForm.skus = []; specGroups.value = [{ name: '默认', values: ['默认'], pending: '' }]; generateSkus(); };
const syncRoute = async () => { await router.replace({ path: '/products', query: { ...(filters.keyword ? { keyword: filters.keyword } : {}), ...(filters.categoryId ? { categoryId: String(filters.categoryId) } : {}), ...(filters.status ? { status: filters.status } : {}), ...(filters.salesBand ? { salesBand: filters.salesBand } : {}), ...(Number(filters.hotSalesThreshold) ? { hotSalesThreshold: String(filters.hotSalesThreshold) } : {}), ...(Number(filters.lowSalesThreshold) >= 0 ? { lowSalesThreshold: String(filters.lowSalesThreshold) } : {}), ...(query.sortBy ? { sortBy: query.sortBy } : {}), ...(query.sortOrder ? { sortOrder: query.sortOrder } : {}), ...(pager.page > 1 ? { page: String(pager.page) } : {}), ...(pager.size !== ADMIN_PAGE_SIZE ? { size: String(pager.size) } : {}) } }); };
const loadProducts = async () => { loading.value = true; try { const { data } = await fetchAdminProductPage({ keyword: filters.keyword || undefined, categoryId: filters.categoryId || undefined, status: filters.status || undefined, salesBand: filters.salesBand || undefined, hotSalesThreshold: filters.hotSalesThreshold, lowSalesThreshold: filters.lowSalesThreshold, sortBy: query.sortBy || undefined, sortOrder: query.sortOrder || undefined, page: pager.page, size: pager.size }); const pageData = data?.data || {}; products.value = pageData.records || []; pager.total = pageData.total || 0; if (data && data.success === false) ElMessage.warning(data.message || data.msg || '商品列表加载失败'); Object.assign(adminPageCache.products, { list: products.value, pager: { page: pager.page, size: pager.size, total: pager.total }, filters: { keyword: filters.keyword, categoryId: filters.categoryId, status: filters.status, salesBand: filters.salesBand, hotSalesThreshold: filters.hotSalesThreshold, lowSalesThreshold: filters.lowSalesThreshold }, query: { sortBy: query.sortBy, sortOrder: query.sortOrder }, loaded: true, updatedAt: Date.now() }); } catch (error) { products.value = []; pager.total = 0; ElMessage.error(error?.response?.data?.msg || error?.response?.data?.message || '商品列表加载失败'); } finally { loading.value = false; } };
const loadCategories = async () => { if (categories.value.length && isCacheFresh(adminPageCache.products.categoriesLoadedAt, 5 * 60 * 1000)) return; const { data } = await fetchAdminCategories(); categories.value = data.data || []; adminPageCache.products.categories = categories.value; adminPageCache.products.categoriesLoadedAt = Date.now(); };
const exportProducts = () => exportRowsToCsv('商品基础运营', products.value.map(row => ({ ...row, salesCount: salesText(row), monthlySalesCount: monthlySalesText(row), salesBandLabel: salesBandLabel(row) })), [{ label: 'ID', value: 'id' }, { label: '商品名称', value: 'name' }, { label: '类目ID', value: 'categoryId' }, { label: 'SKU数', value: 'skuCount' }, { label: '月销量', value: 'monthlySalesCount' }, { label: '销售表现', value: 'salesBandLabel' }, { label: '累计销量', value: 'salesCount' }, { label: '库存', value: 'stock' }, { label: '状态', value: 'status' }]);
const handleSearch = async () => { pager.page = 1; await syncRoute(); await loadProducts(); };
const handleReset = async () => { const effectiveThreshold = resolveEffectiveThreshold(); filters.keyword = ''; filters.categoryId = null; filters.status = ''; filters.salesBand = ''; filters.hotSalesThreshold = effectiveThreshold.hotSalesThreshold; filters.lowSalesThreshold = effectiveThreshold.lowSalesThreshold; query.sortBy = 'id'; query.sortOrder = 'desc'; pager.page = 1; pager.size = ADMIN_PAGE_SIZE; await syncRoute(); await loadProducts(); };
const applyPersonalThreshold = async () => { saveSalesThresholdSession(filters); refreshSalesThresholdState(); adminPageCache.dashboard.loaded = false; adminPageCache.dashboard.updatedAt = 0; ElMessage.success('已保存为本次登录视角，退出登录后失效'); await syncRoute(); await loadProducts(); };
const openSalesThresholdDialog = () => { salesThresholdForm.hotSalesThreshold = salesThresholdDefault.value.hotSalesThreshold; salesThresholdForm.lowSalesThreshold = salesThresholdDefault.value.lowSalesThreshold; salesThresholdVisible.value = true; };
const submitSalesThresholdDefault = async () => { if (!canManageSalesThreshold.value) return ElMessage.warning('仅管理员可配置默认阈值'); const { data } = await updateAdminProductSalesThresholdConfig(normalizeThresholdConfig(salesThresholdForm)); salesThresholdDefault.value = normalizeThresholdConfig(data?.data); refreshSalesThresholdState(); salesThresholdVisible.value = false; const sessionThreshold = loadSalesThresholdSession(); if (!sessionThreshold) { filters.hotSalesThreshold = salesThresholdDefault.value.hotSalesThreshold; filters.lowSalesThreshold = salesThresholdDefault.value.lowSalesThreshold; await syncRoute(); await loadProducts(); } ElMessage.success('已保存全局默认阈值'); };
const handlePageChange = async (page) => { pager.page = page; await syncRoute(); await loadProducts(); };
const handleSizeChange = async (size) => { pager.size = size; pager.page = 1; await syncRoute(); await loadProducts(); };
const handleSortChange = async ({ prop, order }) => { query.sortBy = prop || 'id'; query.sortOrder = order === 'descending' ? 'desc' : 'asc'; if (!order) query.sortOrder = 'desc'; pager.page = 1; await syncRoute(); await loadProducts(); };
const openProductDetail = async (productId) => { try { const { data } = await fetchAdminProductDetail(productId); productDetail.value = { ...data.data, mainImageUrl: normalizeUploadPath(data.data?.mainImageUrl) }; detailVisible.value = true; } catch (error) { ElMessage.error(error?.response?.data?.msg || error?.response?.data?.message || '商品详情加载失败'); } };
const openCreate = () => { editorMode.value = 'create'; editingProductId.value = null; resetForm(); editorVisible.value = true; };
const parseSpecGroupsFromSkus = (skus) => { const map = new Map(); skus.forEach((sku) => { Object.entries(safeParseSpec(sku.specJson)).forEach(([key, value]) => { if (!map.has(key)) map.set(key, new Set()); map.get(key).add(value); }); }); return [...map.entries()].map(([name, values]) => ({ name, values: [...values], pending: '' })); };
const openEdit = async (productId) => { const { data } = await fetchAdminProductDetail(productId); const detail = data.data; const imageUrl = normalizeUploadPath(detail.mainImageUrl || '', 'product'); editorMode.value = 'edit'; editingProductId.value = productId; productForm.name = detail.name; productForm.categoryId = detail.categoryId; productForm.mainImageUrl = imageUrl; productForm.imagePreviewUrl = imageUrl; productForm.description = detail.description || ''; productForm.status = normalizeProductStatus(detail.status); productForm.skus = (detail.skus || []).filter(item => item.skuCode).map(item => ({ id: item.id, skuCode: item.skuCode, skuName: item.skuName, specJson: item.specJson || '{}', salePriceYuan: Number(centToYuan(item.salePriceCent)), originPriceYuan: Number(centToYuan(item.originPriceCent)), initialStock: item.totalStock || 0, status: normalizeSkuStatus(item.status), originalStatus: normalizeSkuStatus(item.status) })); originalEditSkus.value = productForm.skus.map(item => ({ ...item })); specGroups.value = parseSpecGroupsFromSkus(productForm.skus); if (!specGroups.value.length) specGroups.value = [{ name: '默认', values: ['默认'], pending: '' }]; editorVisible.value = true; await nextTick(); };
const openStatusDialog = (row) => { currentProductId.value = row.id; const nextStatus = row.status === 'ONLINE' ? 'OFFLINE' : 'ONLINE'; Object.assign(statusForm, { status: nextStatus, reason: nextStatus === 'ONLINE' ? '后台手动上架' : '后台手动下架' }); statusVisible.value = true; };

const addSpecGroup = () => specGroups.value.push({ name: '', values: [], pending: '' });
const removeSpecGroup = (index) => { specGroups.value.splice(index, 1); generateSkus(); };
const addSpecValue = (index) => { const group = specGroups.value[index]; const value = group.pending?.trim(); if (!group.name?.trim()) return ElMessage.warning('请先填写规格项名称'); if (value && !group.values.includes(value)) group.values.push(value); group.pending = ''; generateSkus(); };
const removeSpecValue = (groupIndex, valueIndex) => { specGroups.value[groupIndex].values.splice(valueIndex, 1); generateSkus(); };
const buildCombinations = (groups) => groups.reduce((acc, group) => acc.flatMap(item => group.values.map(value => ({ ...item, [group.name.trim()]: value }))), [{}]);
const generateSkuCode = (spec, index) => `SKU-${(productForm.name || 'PRODUCT').replace(/\s+/g, '').slice(0, 12).toUpperCase()}-${index + 1}`;
const normalizeSpecJson = (spec) => JSON.stringify(Object.keys(spec || {}).sort().reduce((result, key) => ({ ...result, [key]: spec[key] }), {}));
const isSpecSubsetMatch = (baseSpec, targetSpec) => Object.entries(baseSpec || {}).every(([key, value]) => targetSpec?.[key] === value);
const cloneSkuValuesForNewSpec = (sku, spec, index) => ({
  id: null,
  skuCode: generateSkuCode(spec, index),
  skuName: `${productForm.name || '商品'}-${Object.values(spec).join('-')}`,
  specJson: normalizeSpecJson(spec),
  salePriceYuan: Number(sku?.salePriceYuan || 0.01),
  originPriceYuan: Number(sku?.originPriceYuan || sku?.salePriceYuan || 0.01),
  initialStock: Number(sku?.initialStock || 0),
  status: normalizeSkuStatus(sku?.status),
});
const findTemplateSku = (spec, sourceSkus) => sourceSkus.find((sku) => {
  const skuSpec = safeParseSpec(sku.specJson);
  return Object.keys(skuSpec).length > 0 && Object.keys(skuSpec).length < Object.keys(spec).length && isSpecSubsetMatch(skuSpec, spec);
});
const generateSkus = () => {
  const validGroups = specGroups.value.filter(group => group.name?.trim() && group.values.length);
  if (!validGroups.length) {
    productForm.skus = [];
    return;
  }
  const reusableSkus = editorMode.value === 'edit' ? [...originalEditSkus.value, ...productForm.skus] : productForm.skus;
  const exactMap = new Map(reusableSkus.map(sku => [normalizeSpecJson(safeParseSpec(sku.specJson)), sku]));
  const usedExistingIds = new Set();
  productForm.skus = buildCombinations(validGroups).map((spec, index) => {
    const specJson = normalizeSpecJson(spec);
    const exact = exactMap.get(specJson);
    if (exact && (!exact.id || !usedExistingIds.has(exact.id))) {
      if (exact.id) usedExistingIds.add(exact.id);
      return { ...exact, specJson, status: normalizeSkuStatus(exact.status) };
    }
    const template = findTemplateSku(spec, reusableSkus);
    if (template) return cloneSkuValuesForNewSpec(template, spec, index);
    return { id: null, skuCode: generateSkuCode(spec, index), skuName: `${productForm.name || '商品'}-${Object.values(spec).join('-')}`, specJson, salePriceYuan: 0.01, originPriceYuan: 0.01, initialStock: 0, status: 'ONLINE' };
  });
};

const validateImageFile = (file) => {
  if (!file) return '请选择要上传的图片';
  if (!['image/jpeg', 'image/png'].includes(file.type)) return '仅支持JPG、PNG图片';
  if (file.size > 2 * 1024 * 1024) return '单张图片不能超过2MB';
  return '';
};
const resolveUploadedImageUrl = (responseBody) => {
  const payload = responseBody?.data ?? responseBody;
  return normalizeUploadPath(payload?.url || payload?.accessUrl || payload?.fileUrl || payload?.path || '', 'product');
};
const addImageFile = async (file) => {
  const error = validateImageFile(file);
  if (error) return ElMessage.warning(error);
  const localPreviewUrl = URL.createObjectURL(file);
  productForm.imagePreviewUrl = localPreviewUrl;
  imageUploading.value = true;
  try {
    const { data } = await uploadProductImage(file);
    const uploadedUrl = resolveUploadedImageUrl(data);
    if (!uploadedUrl) {
      throw new Error('文件服务未返回图片URL');
    }
    productForm.mainImageUrl = uploadedUrl;
    productForm.imagePreviewUrl = uploadedUrl;
    ElMessage.success('商品图片上传成功');
  } catch (uploadError) {
    productForm.imagePreviewUrl = productForm.mainImageUrl || '';
    ElMessage.error(uploadError?.response?.data?.msg || uploadError?.response?.data?.message || uploadError?.message || '商品图片上传失败');
  } finally {
    URL.revokeObjectURL(localPreviewUrl);
    imageUploading.value = false;
  }
};
const handleImageSelect = (uploadFile) => addImageFile(uploadFile.raw);
const handleImageDrop = (event) => { if (!canEditBasic.value) return; const [file] = [...event.dataTransfer.files]; if (file) addImageFile(file); };
const removeImage = () => { productForm.imagePreviewUrl = ''; productForm.mainImageUrl = ''; };
const handleEditorCreated = (editor) => { richTextEditor.value = editor; if (!canEditBasic.value) editor.disable(); };

const validateProductForm = () => { if (!productForm.name.trim()) return t('productManage.nameRequired'); if (!productForm.categoryId) return t('productManage.categoryRequired'); if (!productForm.mainImageUrl || productForm.mainImageUrl.startsWith('data:')) return '请先上传商品主图'; if (!productForm.skus.length) return t('productManage.skuRequired'); if (productForm.status === 'ONLINE' && !productForm.skus.some(sku => sku.status === 'ONLINE' && sku.initialStock > 0)) return '上架前至少需要一个启用且库存充足的SKU'; for (const sku of productForm.skus) { const skuLabel = sku.skuName || sku.skuCode || 'SKU'; const salePriceError = validateSkuPrice(sku.salePriceYuan, `${skuLabel}售价`); if (salePriceError) return salePriceError; const originPriceError = validateSkuPrice(sku.originPriceYuan, `${skuLabel}原价`); if (originPriceError) return originPriceError; if (Number(sku.originPriceYuan) < Number(sku.salePriceYuan)) return t('productManage.originPriceLessThanSale'); if (sku.initialStock < 0) return t('productManage.initialStockInvalid'); } return ''; };
const submitProduct = async () => {
  const error = validateProductForm();
  if (error) return ElMessage.warning(error);
  const payload = {
    name: productForm.name.trim(),
    categoryId: productForm.categoryId,
    mainImageUrl: productForm.mainImageUrl,
    description: sanitizeHtml(productForm.description),
    skus: productForm.skus.map(item => ({
      id: item.id,
      skuCode: item.skuCode,
      skuName: item.skuName,
      specJson: item.specJson,
      salePriceCent: yuanToCent(item.salePriceYuan),
      originPriceCent: yuanToCent(item.originPriceYuan),
      initialStock: item.initialStock,
      status: normalizeSkuStatus(item.status),
    })),
  };
  try {
    if (editorMode.value === 'create') {
      payload.skus = payload.skus.map(({ status, id, ...rest }) => rest);
      await createAdminProduct(payload);
      ElMessage.success(t('productManage.createSuccess'));
    } else {
      payload.status = normalizeProductStatus(productForm.status);
      await updateAdminProduct(editingProductId.value, payload);
      ElMessage.success(t('productManage.updateSuccess'));
    }
    editorVisible.value = false;
    await loadProducts();
  } catch (submitError) {
    const message = submitError?.response?.data?.msg || submitError?.response?.data?.message || submitError?.message || '商品保存失败';
    if (editorMode.value === 'edit' && normalizeProductStatus(productForm.status) === 'ONLINE') {
      ElMessage.warning(message.includes('上架') || message.includes('SKU') || message.includes('库存') ? message : '已上架商品不支持直接修改规格或初始库存，请先下架商品，库存变更请到库存管理模块操作');
      return;
    }
    ElMessage.error(message);
  }
};
const submitStatus = async () => { try { const targetStatus = normalizeProductStatus(statusForm.status); await confirmAction(t('productManage.confirmStatusUpdate', { status: productStatusMeta(targetStatus).label })); await updateAdminProductStatus(currentProductId.value, { status: targetStatus, reason: statusForm.reason?.trim() || (targetStatus === 'ONLINE' ? '后台手动上架' : '后台手动下架') }); ElMessage.success(t('productManage.statusUpdateSuccess')); statusVisible.value = false; adminPageCache.products.loaded = false; await loadProducts(); } catch (error) { if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || error?.response?.data?.message || t('productManage.statusUpdateFailed')); } };
const handleLogout = async () => { await adminStore.logout(); router.push('/login'); };
const initializeProductsPage = async (force = false) => { await loadSalesThresholdDefault(); applyRouteQuery(); if (!force && adminPageCache.products.loaded && products.value.length && !Object.keys(route.query).length) return; await Promise.all([loadProducts(), loadCategories()]); };
onMounted(async () => { resetForm(); await initializeProductsPage(); });
onActivated(async () => { await initializeProductsPage(true); });
onBeforeUnmount(() => { richTextEditor.value?.destroy(); });
</script>

<style scoped>
.mb12 { margin-bottom: 12px; }
.compact-alert { margin-bottom: 10px; }
.product-page-card {
  width: 100%;
}

.product-filter-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.product-filter-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px 12px;
}
.product-filter-row :deep(.el-form-item) {
  margin: 0;
}
.product-filter-row--threshold {
  width: fit-content;
  max-width: 100%;
  padding-top: 10px;
  border-top: 1px dashed rgba(148, 163, 184, 0.28);
}
.sales-threshold-default-tip {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border: 1px solid rgba(91, 108, 255, 0.14);
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(248, 250, 255, 0.96), rgba(255, 255, 255, 0.9));
  color: #5b6cff;
  font-size: 13px;
  font-weight: 700;
}
.product-filter-row--main {
  width: 100%;
}
.product-filter-row--main :deep(.el-button),
.product-filter-row--threshold :deep(.el-button) {
  min-width: 72px;
}
.product-create-action {
  margin-left: auto !important;
}
.product-actions { display: flex; align-items: center; justify-content: center; gap: 8px; flex-wrap: wrap; white-space: nowrap; }
.product-action-btn { flex: 0 0 auto; min-width: 68px; margin: 0; border: none; border-radius: 999px; box-shadow: 0 10px 24px rgba(15, 23, 42, .08); }
.product-action-btn--detail { color: #2563eb; background: linear-gradient(135deg, #eff6ff, #dbeafe); }
.product-action-btn--edit { color: #4f46e5; background: linear-gradient(135deg, #eef2ff, #e0e7ff); }
.product-action-btn--success { color: #047857; background: linear-gradient(135deg, #ecfdf5, #d1fae5); }
.product-action-btn--danger { color: #be123c; background: linear-gradient(135deg, #fff1f2, #ffe4e6); }
.product-detail-top { display: grid; grid-template-columns: 180px 1fr; gap: 18px; align-items: stretch; margin-bottom: 18px; }
.product-detail-image { width: 180px; height: 180px; border-radius: 16px; overflow: hidden; background: #f5f7fa; box-shadow: 0 8px 24px rgba(15, 23, 42, .08); }
.product-detail-image--empty { display: flex; align-items: center; justify-content: center; color: #909399; border: 1px dashed #dcdfe6; box-shadow: none; }
.product-detail-meta { margin: 0; }
.product-basic-grid { display: grid; grid-template-columns: 1.5fr 1fr 1fr; gap: 12px; }
.product-basic-grid :deep(.el-radio-group) { width: 100%; }
.product-basic-grid :deep(.el-radio-button) { flex: 1; }
.product-basic-grid :deep(.el-radio-button__inner) { width: 100%; }
.full-control { width: 100%; }
.sku-header { margin: 12px 0 8px; display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.sku-title { font-weight: 700; }
.product-editor-form :deep(.el-form-item) { margin-bottom: 12px; }
.product-editor-form :deep(.el-input), .product-editor-form :deep(.el-textarea), .product-editor-form :deep(.el-select) { width: 100%; }
.single-image-uploader { display: flex; align-items: center; gap: 12px; width: 100%; padding: 10px 12px; border: 1px dashed #c0c4cc; border-radius: 10px; background: #fafafa; }
.single-image-uploader.disabled { opacity: .6; pointer-events: none; }
.upload-tip { color: #909399; font-size: 12px; }
.single-image-preview { width: 104px; margin-top: 8px; padding: 6px; border: 1px solid #ebeef5; border-radius: 10px; background: #fff; }
.single-image-preview img { width: 90px; height: 90px; object-fit: cover; border-radius: 8px; display: block; }
.image-url-readonly { margin-top: 8px; }
.editor-wrapper { width: 100%; border: 1px solid #dcdfe6; border-radius: 8px; overflow: hidden; background: #fff; }
.editor-wrapper.disabled { opacity: .65; pointer-events: none; }
.editor-toolbar { border-bottom: 1px solid #ebeef5; }
.editor-content { min-height: 170px; }
.spec-panel { padding: 10px 12px; border: 1px solid #ebeef5; border-radius: 10px; background: #fbfcff; }
.spec-group { display: grid; grid-template-columns: 160px minmax(0, 1fr) 120px 72px 82px; align-items: center; gap: 8px; margin-bottom: 8px; }
.spec-values-wrap { display: flex; flex-wrap: wrap; gap: 4px; max-height: 34px; overflow-y: auto; padding: 2px 0; }
.spec-value-input { width: 120px; }
.spec-combo-list { display: flex; flex-direction: column; gap: 6px; align-items: flex-start; }
.spec-combo-item { display: inline-flex; align-items: center; max-width: 100%; overflow: hidden; border: 1px solid #d9ecff; border-radius: 6px; background: #ecf5ff; color: #409eff; font-size: 12px; line-height: 22px; }
.spec-combo-key { flex: 0 0 auto; padding: 0 7px; background: rgba(64, 158, 255, .12); font-weight: 600; }
.spec-combo-value { min-width: 0; padding: 0 8px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.sku-editor-table { margin-top: 6px; }
.sku-editor-table :deep(.el-input), .sku-editor-table :deep(.el-select), .sku-editor-table :deep(.el-input-number) { width: 100%; }
.sku-editor-table :deep(.el-select__selected-item), .sku-editor-table :deep(.el-select__placeholder) { color: #303133; opacity: 1; }
.sku-status-selected { color: #303133; }
.product-performance-table :deep(.product-row--hot) { background: linear-gradient(90deg, rgba(255, 247, 237, .95), rgba(255, 255, 255, .98)); }
.product-performance-table :deep(.product-row--low) { background: linear-gradient(90deg, rgba(243, 244, 246, .95), rgba(255, 255, 255, .98)); color: #64748b; }
.product-performance-table :deep(.product-row--hot:hover > td) { background: #fff7ed !important; }
.product-performance-table :deep(.product-row--low:hover > td) { background: #f8fafc !important; }
.monthly-sales { display: inline-flex; min-width: 54px; justify-content: center; padding: 4px 10px; border-radius: 999px; font-weight: 700; }
.monthly-sales--hot { color: #c2410c; background: linear-gradient(135deg, #ffedd5, #fed7aa); box-shadow: 0 8px 18px rgba(249, 115, 22, .18); }
.monthly-sales--normal { color: #2563eb; background: #eff6ff; }
.monthly-sales--low { color: #64748b; background: linear-gradient(135deg, #f1f5f9, #e2e8f0); }
.sales-band-pill { display: inline-flex; align-items: center; justify-content: center; min-width: 58px; padding: 4px 12px; border-radius: 999px; font-size: 12px; font-weight: 800; letter-spacing: .04em; }
.sales-band-pill--hot { color: #fff; background: linear-gradient(135deg, #f97316, #ef4444); box-shadow: 0 10px 22px rgba(239, 68, 68, .22); }
.sales-band-pill--normal { color: #1d4ed8; background: #dbeafe; }
.sales-band-pill--low { color: #475569; background: #e2e8f0; }
:global(.sku-status-select-popper .el-select-dropdown__item) { min-width: 96px; }
</style>
