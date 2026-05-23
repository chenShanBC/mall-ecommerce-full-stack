<template>
<AdminLayout :title="text('title')" @refresh="loadStocks" @logout="handleLogout">
<el-card class="admin-page-card stock-filter-card">
  <template #header>
    <div class="stock-card-header">
      <div>
        <div class="stock-card-title">{{ text('title') }}</div>
        <div class="stock-card-subtitle">库存查询、阈值策略与异常预警统一管理</div>
      </div>
      <el-button class="stock-export-button" @click="exportStocks">{{ text('export') }}</el-button>
    </div>
  </template>
  <el-form :inline="true" :model="filters" class="admin-filter-form stock-filter-form">
    <el-form-item :label="text('skuId')"><el-input v-model="filters.skuId" :placeholder="text('skuPlaceholder')" clearable class="stock-filter-control" /></el-form-item>
    <el-form-item :label="text('stockStatus')"><el-select v-model="filters.stockStatus" clearable :placeholder="text('selectStockStatus')" class="stock-filter-control"><el-option :label="text('active')" value="ACTIVE" /><el-option :label="text('frozen')" value="FROZEN" /><el-option :label="text('offline')" value="OFFLINE" /></el-select></el-form-item>
    <el-form-item :label="text('warningStatus')"><el-select v-model="filters.warningStatus" clearable :placeholder="text('selectWarningStatus')" class="stock-filter-control"><el-option :label="text('normal')" value="NORMAL" /><el-option :label="text('low')" value="LOW" /><el-option :label="text('high')" value="HIGH" /></el-select></el-form-item>
    <el-form-item class="stock-filter-actions"><el-button type="primary" @click="handleSearch">{{ text('search') }}</el-button><el-button @click="handleReset">{{ text('reset') }}</el-button></el-form-item>
  </el-form>
</el-card>
<el-card class="stock-table-card"><el-table v-loading="loading" :data="stocks" class="admin-table stock-table" :empty-text="text('noData')" @sort-change="handleSortChange">
<el-table-column prop="skuId" :label="text('skuId')" min-width="110" sortable="custom" />
<el-table-column prop="availableStock" :label="text('available')" min-width="130" sortable="custom" />
<el-table-column prop="lockedStock" :label="text('locked')" min-width="130" sortable="custom" />
<el-table-column prop="totalStock" :label="text('total')" min-width="130" sortable="custom" />
<el-table-column prop="stockStatus" :label="text('stockStatus')" min-width="130" sortable="custom"><template #default="{ row }"><el-tag effect="light" :type="stockStatusMeta(row.stockStatus).type">{{ stockStatusMeta(row.stockStatus).label }}</el-tag></template></el-table-column>
<el-table-column prop="warningStatus" :label="text('warningStatus')" min-width="130" sortable="custom"><template #default="{ row }"><el-tag effect="light" :type="stockWarningMeta(row.warningStatus).type">{{ stockWarningMeta(row.warningStatus).label }}</el-tag></template></el-table-column>
<el-table-column prop="lowStockThreshold" :label="text('lowThreshold')" min-width="120" sortable="custom" />
<el-table-column prop="highStockThreshold" :label="text('highThreshold')" min-width="120" sortable="custom" />
<el-table-column v-if="canAdjust" :label="text('actions')" min-width="300" fixed="right" align="center"><template #default="{ row }"><div class="stock-actions"><el-button class="stock-action-button" size="small" @click="openAdjust(row)">{{ text('adjust') }}</el-button><el-button class="stock-action-button stock-action-button--warning" size="small" type="warning" @click="openPolicy(row)">{{ text('policy') }}</el-button><el-button v-if="row.warningStatus !== 'NORMAL'" class="stock-action-button stock-action-button--danger" size="small" type="danger" @click="openWarning(row)">{{ text('warningHandle') }}</el-button></div></template></el-table-column>
</el-table><div class="admin-pagination"><el-pagination background layout="sizes, prev, pager, next, total" :current-page="pager.page" :page-size="pager.size" :page-sizes="ADMIN_PAGE_SIZES" :total="pager.total" @current-change="handlePageChange" @size-change="handleSizeChange" /></div></el-card>
<el-dialog v-model="adjustVisible" :title="text('adjustTitle')" width="460px" append-to-body destroy-on-close align-center><el-form ref="adjustFormRef" :model="adjustForm" :rules="adjustRules" label-width="100px" class="admin-dialog-form"><el-form-item :label="text('total')" prop="totalStock"><el-input-number v-model="adjustForm.totalStock" :min="0" /></el-form-item><el-form-item :label="text('available')" prop="availableStock"><el-input-number v-model="adjustForm.availableStock" :min="0" /></el-form-item><el-form-item :label="text('locked')" prop="lockedStock"><el-input-number v-model="adjustForm.lockedStock" :min="0" /></el-form-item><el-form-item :label="text('reason')" prop="reason"><el-input v-model="adjustForm.reason" type="textarea" :rows="3" /></el-form-item></el-form><template #footer><el-button :disabled="adjustSubmitting" @click="adjustVisible=false">{{ text('cancel') }}</el-button><el-button type="primary" :loading="adjustSubmitting" @click="submitAdjust">{{ text('submit') }}</el-button></template></el-dialog>
<el-dialog v-model="policyVisible" :title="text('policyTitle')" width="460px" append-to-body destroy-on-close align-center><el-form ref="policyFormRef" :model="policyForm" :rules="policyRules" label-width="100px" class="admin-dialog-form"><el-form-item :label="text('stockStatus')" prop="stockStatus"><el-select v-model="policyForm.stockStatus" style="width:180px"><el-option :label="text('active')" value="ACTIVE" /><el-option :label="text('frozen')" value="FROZEN" /><el-option :label="text('offline')" value="OFFLINE" /></el-select></el-form-item><el-form-item :label="text('lowThreshold')" prop="lowStockThreshold"><el-input-number v-model="policyForm.lowStockThreshold" :min="0" /></el-form-item><el-form-item :label="text('highThreshold')" prop="highStockThreshold"><el-input-number v-model="policyForm.highStockThreshold" :min="0" /></el-form-item><el-form-item :label="text('policyReason')" prop="reason"><el-input v-model="policyForm.reason" type="textarea" :rows="3" /></el-form-item></el-form><template #footer><el-button :disabled="policySubmitting" @click="policyVisible=false">{{ text('cancel') }}</el-button><el-button type="primary" :loading="policySubmitting" @click="submitPolicy">{{ text('submit') }}</el-button></template></el-dialog>
<el-dialog v-model="warningVisible" :title="text('warningTitle')" width="460px" append-to-body destroy-on-close align-center><el-form ref="warningFormRef" :model="warningForm" :rules="warningRules" label-width="100px" class="admin-dialog-form"><el-form-item :label="text('action')" prop="action"><el-select v-model="warningForm.action" style="width:220px"><el-option :label="text('freezeStock')" value="FREEZE" /><el-option :label="text('recoverActive')" value="RECOVER_ACTIVE" /><el-option :label="text('offlineProduct')" value="OFFLINE" /></el-select></el-form-item><el-form-item :label="text('note')" prop="note"><el-input v-model="warningForm.note" type="textarea" :rows="3" /></el-form-item></el-form><template #footer><el-button :disabled="warningSubmitting" @click="warningVisible=false">{{ text('cancel') }}</el-button><el-button type="primary" :loading="warningSubmitting" @click="submitWarning">{{ text('submit') }}</el-button></template></el-dialog>
</AdminLayout>
</template>
<script setup>
import { computed, nextTick, onActivated, onMounted, reactive, ref } from 'vue';import { ElMessage } from 'element-plus';import { useRoute, useRouter } from 'vue-router';import AdminLayout from '../components/AdminLayout.vue';import { adjustAdminStock, fetchAdminStocks, handleAdminStockWarning, updateAdminStockPolicy } from '../api';import { useAdminStore } from '../stores/admin';import { adminPageCache, isCacheFresh } from '../stores/pageCache';import { confirmAction } from '../utils/action';import { exportRowsToCsv } from '../utils/export';import { t } from '../utils/i18n';import { ADMIN_PAGE_SIZE, ADMIN_PAGE_SIZES } from '../utils/pagination';import { getStatusTagMeta } from '../utils/status';
const route=useRoute();const router=useRouter();const adminStore=useAdminStore();const canAdjust=computed(()=>adminStore.hasPermission('stock:adjust'));const loading=ref(false);const stocks=ref(adminPageCache.stocks.list || []);const currentSkuId=ref(null);const adjustVisible=ref(false);const policyVisible=ref(false);const warningVisible=ref(false);const adjustSubmitting=ref(false);const policySubmitting=ref(false);const warningSubmitting=ref(false);const adjustFormRef=ref();const policyFormRef=ref();const warningFormRef=ref();const filters=reactive({ ...adminPageCache.stocks.filters, skuId: String(route.query.skuId ?? adminPageCache.stocks.filters.skuId ?? '') , stockStatus: String(route.query.stockStatus ?? adminPageCache.stocks.filters.stockStatus ?? ''), warningStatus: String(route.query.warningStatus ?? adminPageCache.stocks.filters.warningStatus ?? '')});const query=reactive({ ...adminPageCache.stocks.query, sortBy:String(route.query.sortBy||adminPageCache.stocks.query.sortBy||'skuId'),sortOrder:String(route.query.sortOrder||adminPageCache.stocks.query.sortOrder||'asc')});const adjustForm=reactive({totalStock:0,availableStock:0,lockedStock:0,reason:''});const policyForm=reactive({stockStatus:'ACTIVE',lowStockThreshold:0,highStockThreshold:0,reason:''});const warningForm=reactive({action:'FREEZE',note:''});const pager=reactive({ page:Number(route.query.page||adminPageCache.stocks.pager.page||1), size:Number(route.query.size||adminPageCache.stocks.pager.size||ADMIN_PAGE_SIZE), total:Number(adminPageCache.stocks.pager.total||0) });const stockStatusMeta=(s)=>getStatusTagMeta('stockStatus',s);const stockWarningMeta=(s)=>getStatusTagMeta('stockWarning',s);const text=(key)=>t(`stockManage.${key}`);
const validateNonNegative=(_,value,callback)=>{if(value==null||value<0)return callback(new Error(text('validateNonNegative')));callback();};const validateRequiredText=(messageKey)=>(_,value,callback)=>{if(!value?.trim())return callback(new Error(text(messageKey)));callback();};const validatePolicyThreshold=(_,value,callback)=>{if(value==null||value<0)return callback(new Error(text('validateNonNegative')));if(policyForm.highStockThreshold<policyForm.lowStockThreshold)return callback(new Error(text('highThresholdLessThanLow')||'??????????'));callback();};const adjustRules={totalStock:[{validator:validateNonNegative,trigger:'change'}],availableStock:[{validator:validateNonNegative,trigger:'change'}],lockedStock:[{validator:validateNonNegative,trigger:'change'}],reason:[{validator:validateRequiredText('validateReasonRequired'),trigger:'blur'}]};const policyRules={stockStatus:[{required:true,message:text('selectStockStatus'),trigger:'change'}],lowStockThreshold:[{validator:validatePolicyThreshold,trigger:'change'}],highStockThreshold:[{validator:validatePolicyThreshold,trigger:'change'}],reason:[{validator:validateRequiredText('validateReasonRequired'),trigger:'blur'}]};const warningRules={action:[{required:true,message:text('action'),trigger:'change'}],note:[{validator:validateRequiredText('note'),trigger:'blur'}]};
const syncRoute=async()=>{await router.replace({path:'/stocks',query:{...(filters.skuId?{skuId:String(filters.skuId)}:{}),...(filters.stockStatus?{stockStatus:filters.stockStatus}:{}),...(filters.warningStatus?{warningStatus:filters.warningStatus}:{}),...(query.sortBy?{sortBy:query.sortBy}:{}),...(query.sortOrder?{sortOrder:query.sortOrder}:{}),...(pager.page>1?{page:String(pager.page)}:{}),...(pager.size!==ADMIN_PAGE_SIZE?{size:String(pager.size)}:{})}})};
const loadStocks=async()=>{loading.value=true;try{const {data}=await fetchAdminStocks({skuId:filters.skuId||undefined,stockStatus:filters.stockStatus||undefined,warningStatus:filters.warningStatus||undefined,sortBy:query.sortBy||undefined,sortOrder:query.sortOrder||undefined,page:pager.page,size:pager.size});stocks.value=data.data.records||[];pager.total=data.data.total||0;adminPageCache.stocks.list=stocks.value;adminPageCache.stocks.filters={skuId:filters.skuId,stockStatus:filters.stockStatus,warningStatus:filters.warningStatus};adminPageCache.stocks.query={sortBy:query.sortBy,sortOrder:query.sortOrder};adminPageCache.stocks.pager={page:pager.page,size:pager.size,total:pager.total};adminPageCache.stocks.loaded=true;adminPageCache.stocks.updatedAt=Date.now();}catch(error){ElMessage.error(error?.response?.data?.message||text('loadFailed'));}finally{loading.value=false;}};
const exportStocks=()=>exportRowsToCsv(text('exportName'),stocks.value,[{label:text('skuId'),value:'skuId'},{label:text('available'),value:'availableStock'},{label:text('locked'),value:'lockedStock'},{label:text('total'),value:'totalStock'},{label:text('stockStatus'),value:'stockStatus'},{label:text('warningStatus'),value:'warningStatus'},{label:text('lowThreshold'),value:'lowStockThreshold'},{label:text('highThreshold'),value:'highStockThreshold'}]);
const handleSearch=async()=>{pager.page=1;await syncRoute();await loadStocks();};const handleReset=async()=>{filters.skuId='';filters.stockStatus='';filters.warningStatus='';query.sortBy='skuId';query.sortOrder='asc';pager.page=1;pager.size=ADMIN_PAGE_SIZE;await syncRoute();await loadStocks();};const handlePageChange=async(page)=>{pager.page=page;await syncRoute();await loadStocks();};const handleSizeChange=async(size)=>{pager.size=size;pager.page=1;await syncRoute();await loadStocks();};const handleSortChange=async({prop,order})=>{query.sortBy=prop||'skuId';query.sortOrder=order==='descending'?'desc':'asc';pager.page=1;await syncRoute();await loadStocks();};const resetAdjustValidation=async()=>{await nextTick();adjustFormRef.value?.clearValidate();};const resetPolicyValidation=async()=>{await nextTick();policyFormRef.value?.clearValidate();};const resetWarningValidation=async()=>{await nextTick();warningFormRef.value?.clearValidate();};const openAdjust=async(row)=>{currentSkuId.value=row.skuId;Object.assign(adjustForm,{totalStock:row.totalStock,availableStock:row.availableStock,lockedStock:row.lockedStock,reason:''});adjustVisible.value=true;await resetAdjustValidation();};const openPolicy=async(row)=>{currentSkuId.value=row.skuId;Object.assign(policyForm,{stockStatus:row.stockStatus,lowStockThreshold:row.lowStockThreshold,highStockThreshold:row.highStockThreshold,reason:''});policyVisible.value=true;await resetPolicyValidation();};const openWarning=async(row)=>{currentSkuId.value=row.skuId;Object.assign(warningForm,{action:row.warningStatus==='LOW'?'FREEZE':'OFFLINE',note:''});warningVisible.value=true;await resetWarningValidation();};
const submitAdjust=async()=>{const valid=await adjustFormRef.value?.validate().catch(()=>false);if(!valid)return;if(adjustForm.totalStock!==adjustForm.availableStock+adjustForm.lockedStock){ElMessage.warning(text('validateSumMismatch'));return;}try{await confirmAction(`${text('adjust')} SKU ${currentSkuId.value}`);adjustSubmitting.value=true;await adjustAdminStock(currentSkuId.value,{...adjustForm});ElMessage.success(text('adjustSuccess'));adjustVisible.value=false;await loadStocks();}catch(error){if(error!=='cancel')ElMessage.error(error?.response?.data?.message||text('adjustFailed'));}finally{adjustSubmitting.value=false;}};const submitPolicy=async()=>{const valid=await policyFormRef.value?.validate().catch(()=>false);if(!valid)return;try{await confirmAction(`${text('policy')} SKU ${currentSkuId.value}`);policySubmitting.value=true;await updateAdminStockPolicy(currentSkuId.value,{...policyForm});ElMessage.success(text('policySuccess'));policyVisible.value=false;await loadStocks();}catch(error){if(error!=='cancel')ElMessage.error(error?.response?.data?.message||text('policyFailed'));}finally{policySubmitting.value=false;}};const submitWarning=async()=>{const valid=await warningFormRef.value?.validate().catch(()=>false);if(!valid)return;try{await confirmAction(`${text('warningHandle')} SKU ${currentSkuId.value}`);warningSubmitting.value=true;await handleAdminStockWarning(currentSkuId.value,{...warningForm});ElMessage.success(text('warningSuccess'));warningVisible.value=false;await loadStocks();}catch(error){if(error!=='cancel')ElMessage.error(error?.response?.data?.message||text('warningFailed'));}finally{warningSubmitting.value=false;}};const handleLogout=async()=>{await adminStore.logout();router.push('/login');};const initializeStocksPage=async(force=false)=>{if(!force&&adminPageCache.stocks.loaded&&stocks.value.length&&isCacheFresh(adminPageCache.stocks.updatedAt)){return;}await loadStocks();};onMounted(()=>initializeStocksPage());onActivated(()=>initializeStocksPage());
</script>
<style scoped>
.stock-filter-card,
.stock-table-card {
  border: 0;
  border-radius: 22px;
  box-shadow: 0 14px 34px rgba(15, 23, 42, 0.08);
}
.stock-table-card {
  margin-top: 18px;
}
.stock-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.stock-card-title {
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}
.stock-card-subtitle {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}
.stock-filter-form {
  display: grid;
  grid-template-columns: repeat(3, minmax(220px, 1fr)) auto;
  gap: 18px 22px;
  align-items: end;
}
.stock-filter-form :deep(.el-form-item) {
  margin: 0;
  display: block;
}
.stock-filter-form :deep(.el-form-item__label) {
  display: block;
  margin-bottom: 8px;
  color: #475569;
  font-weight: 600;
  line-height: 1.2;
}
.stock-filter-control,
.stock-filter-control :deep(.el-input__wrapper),
.stock-filter-control :deep(.el-select__wrapper) {
  width: 100%;
}
.stock-filter-actions :deep(.el-form-item__content) {
  display: flex;
  gap: 10px;
  flex-wrap: nowrap;
}
.stock-export-button {
  min-width: 108px;
  border-radius: 999px;
}
.stock-table {
  width: 100%;
}
.stock-table :deep(.el-table__header th) {
  background: #f8fafc;
  color: #334155;
  font-weight: 700;
}
.stock-table :deep(.el-table__cell) {
  padding: 13px 0;
}
.stock-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}
.stock-action-button {
  min-width: 82px;
  margin: 0;
  border-radius: 999px;
}
.stock-action-button--warning {
  box-shadow: 0 8px 18px rgba(245, 158, 11, 0.16);
}
.stock-action-button--danger {
  box-shadow: 0 8px 18px rgba(239, 68, 68, 0.16);
}
@media (max-width: 1200px) {
  .stock-filter-form {
    grid-template-columns: repeat(2, minmax(220px, 1fr));
  }
}
@media (max-width: 768px) {
  .stock-card-header {
    align-items: flex-start;
    flex-direction: column;
  }
  .stock-filter-form {
    grid-template-columns: 1fr;
  }
}
</style>
