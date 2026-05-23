<template>
  <AdminLayout title="商品管理" @refresh="loadProducts" @logout="handleLogout">
    <el-card class="admin-page-card">
      <el-form :inline="true" :model="filters" class="admin-filter-form">
        <el-form-item label="关键词"><el-input v-model="filters.keyword" clearable placeholder="商品名称" /></el-form-item>
        <el-form-item label="类目"><el-select v-model="filters.categoryId" clearable style="width: 220px"><el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="filters.status" clearable style="width: 160px"><el-option label="上架" value="ONLINE" /><el-option label="下架" value="OFFLINE" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="handleSearch">查询</el-button></el-form-item>
        <el-form-item><el-button @click="handleReset">重置</el-button></el-form-item>
        <el-form-item><el-button @click="exportProducts">导出</el-button></el-form-item>
        <el-form-item v-if="canCreate"><el-button type="primary" @click="openCreate">新增商品</el-button></el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <el-table v-loading="loading" :data="products" class="admin-table" empty-text="暂无商品数据" @sort-change="handleSortChange">
        <el-table-column prop="id" label="ID" width="80" sortable="custom" />
        <el-table-column prop="name" label="商品名称" min-width="220" sortable="custom" />
        <el-table-column prop="categoryId" label="类目ID" width="100" sortable="custom" />
        <el-table-column prop="skuCount" label="SKU数" width="100" sortable="custom" />
        <el-table-column prop="stock" label="库存" width="100" />
        <el-table-column prop="status" label="状态" width="120" sortable="custom">
          <template #default="{ row }"><el-tag :type="productStatusMeta(row.status).type">{{ productStatusMeta(row.status).label }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="340">
          <template #default="{ row }">
            <el-button size="small" @click="openProductDetail(row.id)">详情</el-button>
            <el-button v-if="canUpdate" size="small" type="primary" @click="openEdit(row.id)">编辑</el-button>
            <el-button v-if="canUpdate" size="small" type="warning" plain @click="openStatusDialog(row)">{{ row.status === 'ONLINE' ? '下架' : '上架' }}</el-button>
            <el-button v-if="canUpdate" size="small" type="danger" plain @click="openViolationDialog(row)">运营处置</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="admin-pagination"><el-pagination background layout="sizes, prev, pager, next, total" :current-page="pager.page" :page-size="pager.size" :page-sizes="ADMIN_PAGE_SIZES" :total="pager.total" @current-change="handlePageChange" @size-change="handleSizeChange" /></div>
    </el-card>

    <el-dialog v-model="detailVisible" title="商品详情" width="900px" append-to-body destroy-on-close align-center>
      <div v-if="productDetail" class="admin-detail">
        <div class="admin-detail__title">{{ productDetail.name }}</div>
        <div class="admin-detail__grid">
          <div class="admin-detail__item"><span class="admin-detail__label">商品ID</span>{{ productDetail.id }}</div>
          <div class="admin-detail__item"><span class="admin-detail__label">状态</span><el-tag :type="productStatusMeta(productDetail.status).type">{{ productStatusMeta(productDetail.status).label }}</el-tag></div>
          <div class="admin-detail__item full"><span class="admin-detail__label">描述</span>{{ productDetail.description || '-' }}</div>
        </div>
        <el-table :data="productDetail.skus || []" class="admin-table">
          <el-table-column prop="id" label="SKU ID" width="90" />
          <el-table-column prop="skuCode" label="SKU编码" width="140" />
          <el-table-column prop="skuName" label="SKU名称" min-width="180" />
          <el-table-column prop="salePriceCent" label="售价(分)" width="110" />
          <el-table-column prop="availableStock" label="可用库存" width="100" />
          <el-table-column prop="lockedStock" label="锁定库存" width="100" />
          <el-table-column prop="totalStock" label="总库存" width="100" />
          <el-table-column prop="warningStatus" label="预警" width="100">
            <template #default="{ row }"><el-tag :type="stockWarningMeta(row.warningStatus).type">{{ stockWarningMeta(row.warningStatus).label }}</el-tag></template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <el-dialog v-model="editorVisible" :title="editorMode === 'create' ? '新增商品' : '编辑商品'" width="1100px" top="5vh" append-to-body destroy-on-close align-center>
      <el-form :model="productForm" label-width="100px" class="admin-dialog-form product-editor-form">
        <el-form-item label="商品名称"><el-input v-model="productForm.name" maxlength="100" show-word-limit /></el-form-item>
        <el-form-item label="类目"><el-select v-model="productForm.categoryId" style="width: 220px"><el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
        <el-form-item label="主图"><el-input v-model="productForm.mainImageUrl" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="productForm.description" type="textarea" rows="3" maxlength="500" show-word-limit /></el-form-item>
        <el-form-item v-if="editorMode === 'edit'" label="状态"><el-select v-model="productForm.status" style="width: 220px"><el-option label="上架" value="ONLINE" /><el-option label="下架" value="OFFLINE" /></el-select></el-form-item>

        <div class="sku-header">
          <div class="sku-title">SKU列表</div>
          <el-button size="small" @click="addSku">新增SKU</el-button>
        </div>
        <el-table :data="productForm.skus" class="admin-table sku-editor-table">
          <el-table-column label="SKU编码" min-width="150"><template #default="{ row }"><el-input v-model="row.skuCode" maxlength="64" /></template></el-table-column>
          <el-table-column label="SKU名称" min-width="160"><template #default="{ row }"><el-input v-model="row.skuName" maxlength="100" /></template></el-table-column>
          <el-table-column label="规格JSON" min-width="180"><template #default="{ row }"><el-input v-model="row.specJson" /></template></el-table-column>
          <el-table-column label="售价(分)" width="120"><template #default="{ row }"><el-input-number v-model="row.salePriceCent" :min="1" /></template></el-table-column>
          <el-table-column label="原价(分)" width="120"><template #default="{ row }"><el-input-number v-model="row.originPriceCent" :min="1" /></template></el-table-column>
          <el-table-column label="初始库存" width="120"><template #default="{ row }"><el-input-number v-model="row.initialStock" :min="0" /></template></el-table-column>
          <el-table-column v-if="editorMode === 'edit'" label="状态" width="120"><template #default="{ row }"><el-select v-model="row.status"><el-option label="上架" value="ONLINE" /><el-option label="下架" value="OFFLINE" /></el-select></template></el-table-column>
          <el-table-column label="操作" width="90"><template #default="{ $index }"><el-button size="small" type="danger" plain @click="removeSku($index)">删除</el-button></template></el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" @click="submitProduct">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="statusVisible" title="商品状态变更" width="460px" append-to-body destroy-on-close align-center>
      <el-form :model="statusForm" label-width="100px" class="admin-dialog-form">
        <el-form-item label="目标状态"><el-select v-model="statusForm.status" style="width: 200px"><el-option label="上架" value="ONLINE" /><el-option label="下架" value="OFFLINE" /></el-select></el-form-item>
        <el-form-item label="操作原因"><el-input v-model="statusForm.reason" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="statusVisible = false">取消</el-button><el-button type="primary" @click="submitStatus">提交</el-button></template>
    </el-dialog>

    <el-dialog v-model="violationVisible" title="商品运营处置" width="520px" append-to-body destroy-on-close align-center>
      <el-form :model="violationForm" label-width="100px" class="admin-dialog-form">
        <el-form-item label="处置动作"><el-select v-model="violationForm.action" style="width: 220px"><el-option label="违规下架" value="OFFLINE_VIOLATION" /><el-option label="恢复上架" value="RECOVER_ONLINE" /></el-select></el-form-item>
        <el-form-item label="处理原因"><el-input v-model="violationForm.reason" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="violationVisible = false">取消</el-button><el-button type="primary" @click="submitViolation">提交</el-button></template>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { computed, onActivated, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import AdminLayout from '../components/AdminLayout.vue';
import { confirmAction } from '../utils/action';
import { exportRowsToCsv } from '../utils/export';
import { t } from '../utils/i18n';
import { ADMIN_PAGE_SIZE, ADMIN_PAGE_SIZES } from '../utils/pagination';
import { getStatusTagMeta } from '../utils/status';
import { createAdminProduct, fetchAdminCategories, fetchAdminProductDetail, fetchAdminProductPage, handleAdminProductViolation, updateAdminProduct, updateAdminProductStatus } from '../api';
import { useAdminStore } from '../stores/admin';
import { adminPageCache, isCacheFresh } from '../stores/pageCache';

const route = useRoute();
const router = useRouter();
const adminStore = useAdminStore();
const canCreate = computed(() => adminStore.hasPermission('product:create'));
const canUpdate = computed(() => adminStore.hasPermission('product:update'));
const products = ref(adminPageCache.products.list || []);
const loading = ref(false);
const categories = ref(adminPageCache.products.categories || []);
const detailVisible = ref(false);
const editorVisible = ref(false);
const statusVisible = ref(false);
const violationVisible = ref(false);
const editorMode = ref('create');
const editingProductId = ref(null);
const currentProductId = ref(null);
const productDetail = ref(null);
const productStatusMeta = (status) => getStatusTagMeta('productStatus', status);
const stockWarningMeta = (status) => getStatusTagMeta('stockWarning', status);
const filters = reactive({ ...adminPageCache.products.filters, keyword: String(route.query.keyword ?? adminPageCache.products.filters.keyword ?? ''), categoryId: route.query.categoryId ? Number(route.query.categoryId) : (adminPageCache.products.filters.categoryId ?? null), status: String(route.query.status ?? adminPageCache.products.filters.status ?? '') });
const query = reactive({ sortBy: String(route.query.sortBy || adminPageCache.products.query?.sortBy || 'id'), sortOrder: String(route.query.sortOrder || adminPageCache.products.query?.sortOrder || 'asc') });
const pager = reactive({ page: Number(route.query.page || adminPageCache.products.pager.page || 1), size: Number(route.query.size || adminPageCache.products.pager.size || ADMIN_PAGE_SIZE), total: Number(adminPageCache.products.pager.total || 0) });
const statusForm = reactive({ status: 'OFFLINE', reason: '' });
const violationForm = reactive({ action: 'OFFLINE_VIOLATION', reason: '' });
const productForm = reactive({ name: '', categoryId: null, mainImageUrl: '', description: '', status: 'OFFLINE', skus: [] });

const emptySku = () => ({ id: null, skuCode: '', skuName: '', specJson: '', salePriceCent: 1, originPriceCent: 1, initialStock: 0, status: 'OFFLINE' });
const resetForm = () => { productForm.name = ''; productForm.categoryId = null; productForm.mainImageUrl = ''; productForm.description = ''; productForm.status = 'OFFLINE'; productForm.skus = [emptySku()]; };
const syncRoute = async () => {
  await router.replace({
    path: '/products',
    query: {
      ...(filters.keyword ? { keyword: filters.keyword } : {}),
      ...(filters.categoryId ? { categoryId: String(filters.categoryId) } : {}),
      ...(filters.status ? { status: filters.status } : {}),
      ...(query.sortBy ? { sortBy: query.sortBy } : {}),
      ...(query.sortOrder ? { sortOrder: query.sortOrder } : {}),
      ...(pager.page > 1 ? { page: String(pager.page) } : {}),
      ...(pager.size !== ADMIN_PAGE_SIZE ? { size: String(pager.size) } : {}),
    },
  });
};
const loadProducts = async () => {
  loading.value = true;
  try {
    const { data } = await fetchAdminProductPage({ keyword: filters.keyword || undefined, categoryId: filters.categoryId || undefined, status: filters.status || undefined, sortBy: query.sortBy || undefined, sortOrder: query.sortOrder || undefined, page: pager.page, size: pager.size });
    products.value = data.data.records || [];
    pager.total = data.data.total || 0;
    adminPageCache.products.list = products.value;
    adminPageCache.products.pager = { page: pager.page, size: pager.size, total: pager.total };
    adminPageCache.products.filters = { keyword: filters.keyword, categoryId: filters.categoryId, status: filters.status };
    adminPageCache.products.query = { sortBy: query.sortBy, sortOrder: query.sortOrder };
    adminPageCache.products.loaded = true;
    adminPageCache.products.updatedAt = Date.now();
  } finally {
    loading.value = false;
  }
};
const loadCategories = async () => {
  if (categories.value.length && isCacheFresh(adminPageCache.products.categoriesLoadedAt, 5 * 60 * 1000)) {
    return;
  }
  const { data } = await fetchAdminCategories();
  categories.value = data.data || [];
  adminPageCache.products.categories = categories.value;
  adminPageCache.products.categoriesLoadedAt = Date.now();
};
const exportProducts = () => {
  exportRowsToCsv('商品基础运营', products.value, [
    { label: 'ID', value: 'id' },
    { label: '商品名称', value: 'name' },
    { label: '类目ID', value: 'categoryId' },
    { label: 'SKU数', value: 'skuCount' },
    { label: '库存', value: 'stock' },
    { label: '状态', value: 'status' },
  ]);
};
const handleSearch = async () => { pager.page = 1; await syncRoute(); await loadProducts(); };
const handleReset = async () => { filters.keyword = ''; filters.categoryId = null; filters.status = ''; query.sortBy = 'id'; query.sortOrder = 'asc'; pager.page = 1; pager.size = ADMIN_PAGE_SIZE; await syncRoute(); await loadProducts(); };
const handlePageChange = async (page) => { pager.page = page; await syncRoute(); await loadProducts(); };
const handleSizeChange = async (size) => { pager.size = size; pager.page = 1; await syncRoute(); await loadProducts(); };
const handleSortChange = async ({ prop, order }) => { query.sortBy = prop || 'id'; query.sortOrder = order === 'descending' ? 'desc' : 'asc'; pager.page = 1; await syncRoute(); await loadProducts(); };
const openProductDetail = async (productId) => { const { data } = await fetchAdminProductDetail(productId); productDetail.value = data.data; detailVisible.value = true; };
const openCreate = () => { editorMode.value = 'create'; editingProductId.value = null; resetForm(); editorVisible.value = true; };
const openEdit = async (productId) => { const { data } = await fetchAdminProductDetail(productId); const detail = data.data; editorMode.value = 'edit'; editingProductId.value = productId; productForm.name = detail.name; productForm.categoryId = detail.categoryId; productForm.mainImageUrl = detail.mainImageUrl; productForm.description = detail.description; productForm.status = detail.status; productForm.skus = (detail.skus || []).map(item => ({ id: item.id, skuCode: item.skuCode, skuName: item.skuName, specJson: item.specJson, salePriceCent: item.salePriceCent, originPriceCent: item.originPriceCent, initialStock: item.totalStock || 0, status: item.status })); editorVisible.value = true; };
const openStatusDialog = (row) => { currentProductId.value = row.id; Object.assign(statusForm, { status: row.status === 'ONLINE' ? 'OFFLINE' : 'ONLINE', reason: '' }); statusVisible.value = true; };
const openViolationDialog = (row) => { currentProductId.value = row.id; Object.assign(violationForm, { action: row.status === 'ONLINE' ? 'OFFLINE_VIOLATION' : 'RECOVER_ONLINE', reason: '' }); violationVisible.value = true; };
const addSku = () => productForm.skus.push(emptySku());
const removeSku = (index) => { if (productForm.skus.length === 1) return ElMessage.warning(t('productManage.keepOneSku')); productForm.skus.splice(index, 1); };
const validateProductForm = () => {
  if (!productForm.name.trim()) return t('productManage.nameRequired');
  if (!productForm.categoryId) return t('productManage.categoryRequired');
  if (!productForm.mainImageUrl.trim()) return t('productManage.mainImageRequired');
  if (!/^https?:\/\//.test(productForm.mainImageUrl.trim())) return t('productManage.mainImageInvalid');
  if (!productForm.skus.length) return t('productManage.skuRequired');
  const skuCodes = new Set();
  for (const sku of productForm.skus) {
    if (!sku.skuCode.trim()) return t('productManage.skuCodeRequired');
    if (!sku.skuName.trim()) return t('productManage.skuNameRequired');
    if (skuCodes.has(sku.skuCode.trim())) return t('productManage.skuCodeDuplicate');
    skuCodes.add(sku.skuCode.trim());
    if (sku.salePriceCent <= 0) return t('productManage.salePriceInvalid');
    if (sku.originPriceCent <= 0) return t('productManage.originPriceInvalid');
    if (sku.originPriceCent < sku.salePriceCent) return t('productManage.originPriceLessThanSale');
    if (sku.initialStock < 0) return t('productManage.initialStockInvalid');
    if (sku.specJson && sku.specJson.trim()) {
      try { JSON.parse(sku.specJson); } catch { return t('productManage.specJsonInvalid'); }
    }
  }
  return '';
};
const submitProduct = async () => {
  const error = validateProductForm();
  if (error) return ElMessage.warning(error);
  const payload = { name: productForm.name.trim(), categoryId: productForm.categoryId, mainImageUrl: productForm.mainImageUrl.trim(), description: productForm.description?.trim() || '', skus: productForm.skus.map(item => ({ id: item.id, skuCode: item.skuCode.trim(), skuName: item.skuName.trim(), specJson: item.specJson?.trim() || '', salePriceCent: item.salePriceCent, originPriceCent: item.originPriceCent, initialStock: item.initialStock, status: item.status })) };
  if (editorMode.value === 'create') {
    payload.skus = payload.skus.map(({ status, id, ...rest }) => rest);
    await createAdminProduct(payload);
    ElMessage.success(t('productManage.createSuccess'));
  } else {
    payload.status = productForm.status;
    await updateAdminProduct(editingProductId.value, payload);
    ElMessage.success(t('productManage.updateSuccess'));
  }
  editorVisible.value = false;
  await loadProducts();
};
const submitStatus = async () => {
  try {
    await confirmAction(t('productManage.confirmStatusUpdate', { status: productStatusMeta(statusForm.status).label }));
    await updateAdminProductStatus(currentProductId.value, { ...statusForm });
    ElMessage.success(t('productManage.statusUpdateSuccess'));
    statusVisible.value = false;
    await loadProducts();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || t('productManage.statusUpdateFailed'));
  }
};
const submitViolation = async () => {
  try {
    await confirmAction(t('productManage.confirmViolation', { action: violationForm.action }));
    await handleAdminProductViolation(currentProductId.value, { ...violationForm });
    ElMessage.success(t('productManage.violationSuccess'));
    violationVisible.value = false;
    await loadProducts();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || t('productManage.violationFailed'));
  }
};
const handleLogout = async () => { await adminStore.logout(); router.push('/login'); };

const initializeProductsPage = async (force = false) => {
  if (!force && adminPageCache.products.loaded && products.value.length) {
    return;
  }
  await Promise.all([loadProducts(), loadCategories()]);
};

onMounted(async () => { resetForm(); await initializeProductsPage(); });
onActivated(async () => { await initializeProductsPage(); });
</script>

<style scoped>
.toolbar-card { margin-bottom: 16px; }
.detail-title { font-size: 18px; font-weight: 700; }
.detail-sub { margin-top: 6px; color: #909399; }
.sku-header { margin: 16px 0 12px; display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.sku-title { font-weight: 700; }
.pager { margin-top: 16px; display: flex; justify-content: flex-end; }
.product-editor-form :deep(.el-form-item) { margin-bottom: 18px; }
.product-editor-form :deep(.el-input),
.product-editor-form :deep(.el-textarea),
.product-editor-form :deep(.el-select) { width: 100%; }
.sku-editor-table { margin-top: 8px; }
.sku-editor-table :deep(.el-input),
.sku-editor-table :deep(.el-select),
.sku-editor-table :deep(.el-input-number) { width: 100%; }
</style>