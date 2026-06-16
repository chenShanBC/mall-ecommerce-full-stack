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
    <el-form-item :label="text('warningStatus')"><el-select v-model="filters.warningStatus" clearable :placeholder="text('selectWarningStatus')" class="stock-filter-control"><el-option :label="text('normal')" value="NORMAL" /><el-option :label="text('allWarning')" value="WARNING" /><el-option :label="text('low')" value="LOW" /><el-option :label="text('high')" value="HIGH" /></el-select></el-form-item>
    <el-form-item class="stock-filter-actions"><el-button type="primary" @click="handleSearch">{{ text('search') }}</el-button><el-button @click="handleReset">{{ text('reset') }}</el-button></el-form-item>
  </el-form>
</el-card>
<el-card class="stock-table-card"><div class="admin-table-scroll"><el-table v-loading="loading" :data="stocks" class="admin-table admin-table--safe admin-table--wide stock-table" :empty-text="text('noData')" @sort-change="handleSortChange">
<el-table-column prop="skuId" :label="text('skuId')" min-width="110" sortable="custom" />
<el-table-column prop="skuName" :label="text('skuName')" min-width="180" :show-overflow-tooltip="{ effect: 'light', placement: 'bottom-start', showAfter: 300, offset: 8, popperClass: 'admin-table-tooltip' }" />
<el-table-column prop="availableStock" :label="text('available')" min-width="130" sortable="custom" />
<el-table-column prop="lockedStock" :label="text('locked')" min-width="130" sortable="custom" />
<el-table-column prop="totalStock" :label="text('total')" min-width="130" sortable="custom" />
<el-table-column prop="stockStatus" :label="text('stockStatus')" min-width="130" sortable="custom"><template #default="{ row }"><el-tag effect="light" :type="stockStatusMeta(row.stockStatus).type">{{ stockStatusMeta(row.stockStatus).label }}</el-tag></template></el-table-column>
<el-table-column prop="warningStatus" :label="text('warningStatus')" min-width="130" sortable="custom"><template #default="{ row }"><el-tag effect="light" :type="stockWarningMeta(row.warningStatus).type">{{ stockWarningMeta(row.warningStatus).label }}</el-tag></template></el-table-column>
<el-table-column prop="lowStockThreshold" :label="text('lowThreshold')" min-width="120" sortable="custom" />
<el-table-column prop="highStockThreshold" :label="text('highThreshold')" min-width="120" sortable="custom" />
<el-table-column v-if="canShowActions" :label="text('actions')" min-width="390" fixed="right" align="center"><template #default="{ row }"><div class="stock-actions"><el-button v-if="canAdjust" class="stock-action-button" size="small" @click="openAdjust(row)">{{ text('adjust') }}</el-button><el-button v-if="canUpdatePolicy" class="stock-action-button stock-action-button--warning" size="small" type="warning" @click="openPolicy(row)">{{ text('policy') }}</el-button><el-button v-if="canCheckConsistency" class="stock-action-button stock-action-button--check" size="small" type="success" :loading="checkingSkuId === row.skuId" @click="checkStockConsistency(row)">{{ text('consistencyCheck') }}</el-button><el-button v-if="canHandleWarning && row.warningStatus !== 'NORMAL'" class="stock-action-button stock-action-button--danger" size="small" type="danger" @click="openWarning(row)">{{ text('warningHandle') }}</el-button></div></template></el-table-column>
</el-table></div><div class="admin-pagination"><el-pagination background layout="sizes, prev, pager, next, total" :current-page="pager.page" :page-size="pager.size" :page-sizes="ADMIN_PAGE_SIZES" :total="pager.total" @current-change="handlePageChange" @size-change="handleSizeChange" /></div></el-card>
<el-dialog v-model="adjustVisible" :title="text('adjustTitle')" width="560px" append-to-body destroy-on-close align-center class="admin-beauty-dialog stock-adjust-dialog"><div class="stock-current-snapshot"><div class="stock-current-snapshot__title">{{ text('currentSnapshot') }}</div><div class="stock-current-snapshot__items"><span>{{ text('total') }} = {{ currentStockSnapshot.totalStock }}</span><span>{{ text('available') }} = {{ currentStockSnapshot.availableStock }}</span><span>{{ text('locked') }} = {{ currentStockSnapshot.lockedStock }}</span></div></div><el-form ref="adjustFormRef" :model="adjustForm" :rules="adjustRules" label-width="108px" class="admin-dialog-form"><el-form-item :label="text('adjustmentType')" prop="adjustmentType"><el-select v-model="adjustForm.adjustmentType" class="stock-adjust-control" @change="handleAdjustTypeChange"><el-option v-for="item in adjustmentTypes" :key="item.value" :label="item.label" :value="item.value"><div class="stock-adjust-option"><span>{{ item.label }}</span><small>{{ item.description }}</small></div></el-option></el-select></el-form-item><el-form-item v-if="isBusinessAdjust" :label="text('changeQuantity')" prop="changeQuantity"><el-input-number v-model="adjustForm.changeQuantity" :min="1" class="stock-adjust-control" /></el-form-item><template v-else><el-form-item :label="text('total')" prop="totalStock"><el-input-number v-model="adjustForm.totalStock" :min="0" class="stock-adjust-control" @change="recalculateAvailable" /></el-form-item><el-form-item :label="text('locked')" prop="lockedStock"><el-input-number v-model="adjustForm.lockedStock" :min="0" class="stock-adjust-control" @change="recalculateAvailable" /></el-form-item><el-form-item :label="text('available')" prop="availableStock"><el-input-number v-model="adjustForm.availableStock" :min="0" class="stock-adjust-control" disabled /></el-form-item></template><el-form-item :label="text('reason')" prop="reason"><el-select v-model="adjustForm.reason" class="stock-adjust-control" :placeholder="text('selectReason')"><el-option v-for="item in currentAdjustReasons" :key="item" :label="item" :value="item" /></el-select></el-form-item><el-form-item :label="text('remark')" prop="remark"><el-input v-model="adjustForm.remark" type="textarea" :rows="3" :placeholder="text('remarkPlaceholder')" /></el-form-item></el-form><template #footer><el-button :disabled="adjustSubmitting" @click="adjustVisible=false">{{ text('cancel') }}</el-button><el-button type="primary" :loading="adjustSubmitting" @click="submitAdjust">{{ text('submit') }}</el-button></template></el-dialog>
<el-dialog v-model="policyVisible" :title="text('policyTitle')" width="460px" append-to-body destroy-on-close align-center class="admin-beauty-dialog stock-policy-dialog"><el-form ref="policyFormRef" :model="policyForm" :rules="policyRules" label-width="100px" class="admin-dialog-form"><el-form-item :label="text('stockStatus')" prop="stockStatus"><el-select v-model="policyForm.stockStatus" style="width:180px"><el-option :label="text('active')" value="ACTIVE" /><el-option :label="text('frozen')" value="FROZEN" /><el-option :label="text('offline')" value="OFFLINE" /></el-select></el-form-item><el-form-item :label="text('lowThreshold')" prop="lowStockThreshold"><el-input-number v-model="policyForm.lowStockThreshold" :min="0" /></el-form-item><el-form-item :label="text('highThreshold')" prop="highStockThreshold"><el-input-number v-model="policyForm.highStockThreshold" :min="0" /></el-form-item><el-form-item :label="text('policyReason')" prop="reason"><el-input v-model="policyForm.reason" type="textarea" :rows="3" /></el-form-item></el-form><template #footer><el-button :disabled="policySubmitting" @click="policyVisible=false">{{ text('cancel') }}</el-button><el-button type="primary" :loading="policySubmitting" @click="submitPolicy">{{ text('submit') }}</el-button></template></el-dialog>
<el-dialog v-model="warningVisible" :title="text('warningTitle')" width="460px" append-to-body destroy-on-close align-center class="admin-beauty-dialog stock-warning-dialog"><el-form ref="warningFormRef" :model="warningForm" :rules="warningRules" label-width="100px" class="admin-dialog-form"><el-form-item :label="text('action')" prop="action"><el-select v-model="warningForm.action" style="width:220px"><el-option :label="text('freezeStock')" value="FREEZE" /><el-option :label="text('recoverActive')" value="RECOVER_ACTIVE" /><el-option :label="text('offlineProduct')" value="OFFLINE" /></el-select></el-form-item><el-form-item :label="text('note')" prop="note"><el-input v-model="warningForm.note" type="textarea" :rows="3" /></el-form-item></el-form><template #footer><el-button :disabled="warningSubmitting" @click="warningVisible=false">{{ text('cancel') }}</el-button><el-button type="primary" :loading="warningSubmitting" @click="submitWarning">{{ text('submit') }}</el-button></template></el-dialog>
</AdminLayout>
</template>
<script setup>
import { computed, nextTick, onActivated, onMounted, reactive, ref } from 'vue';import { ElMessage } from 'element-plus';import { useRoute, useRouter } from 'vue-router';import AdminLayout from '../components/AdminLayout.vue';import { adjustAdminStock, fetchAdminStocks, handleAdminStockWarning, checkAdminStockConsistency, updateAdminStockPolicy } from '../api';import { useAdminStore } from '../stores/admin';import { adminPageCache, isCacheFresh } from '../stores/pageCache';import { confirmAction } from '../utils/action';import { exportRowsToCsv } from '../utils/export';import { t } from '../utils/i18n';import { ADMIN_PAGE_SIZE, ADMIN_PAGE_SIZES } from '../utils/pagination';import { getStatusTagMeta } from '../utils/status';
const route=useRoute();const router=useRouter();const adminStore=useAdminStore();const canAdjust=computed(()=>adminStore.hasPermission('stock:adjust'));const canUpdatePolicy=computed(()=>adminStore.hasPermission('stock:policy:update'));const canHandleWarning=computed(()=>adminStore.hasPermission('stock:warning:handle'));const canCheckConsistency=computed(()=>adminStore.hasPermission('stock:reconcile:check'));const canShowActions=computed(()=>canAdjust.value||canUpdatePolicy.value||canHandleWarning.value||canCheckConsistency.value);const text=(key,params={})=>t(`stockManage.${key}`,params);const adjustmentTypes=[{value:'REPLENISH',label:text('replenish'),description:text('replenishDesc')},{value:'INVENTORY_GAIN',label:text('inventoryGain'),description:text('inventoryGainDesc')},{value:'INVENTORY_LOSS',label:text('inventoryLoss'),description:text('inventoryLossDesc')},{value:'MANUAL_UNLOCK',label:text('manualUnlock'),description:text('manualUnlockDesc')},{value:'FORCE_DEDUCT',label:text('forceDeduct'),description:text('forceDeductDesc')},{value:'OTHER',label:text('other'),description:text('otherDesc')}];const adjustReasonOptions={REPLENISH:[text('reasonPurchaseInbound'),text('reasonReturnInbound'),text('reasonSupplierGift'),text('reasonOther')],INVENTORY_GAIN:[text('reasonStocktakingGain'),text('reasonDataCorrection'),text('reasonOther')],INVENTORY_LOSS:[text('reasonStocktakingLoss'),text('reasonDamaged'),text('reasonExpired'),text('reasonTheftLoss'),text('reasonOther')],MANUAL_UNLOCK:[text('reasonOrderCancel'),text('reasonPaymentTimeout'),text('reasonLockException'),text('reasonOther')],FORCE_DEDUCT:[text('reasonOrderException'),text('reasonInventoryCorrectionDeduct'),text('reasonOfflineDeduct'),text('reasonOther')],OTHER:[text('reasonDataCorrection'),text('reasonHistoricalMigration'),text('reasonOther')]};const currentAdjustReasons=computed(()=>adjustReasonOptions[adjustForm.adjustmentType]||adjustReasonOptions.OTHER);const loading=ref(false);const checkingSkuId=ref(null);const stocks=ref(adminPageCache.stocks.list || []);const currentSkuId=ref(null);const currentStockSnapshot=reactive({totalStock:0,availableStock:0,lockedStock:0});const adjustVisible=ref(false);const policyVisible=ref(false);const warningVisible=ref(false);const adjustSubmitting=ref(false);const policySubmitting=ref(false);const warningSubmitting=ref(false);const adjustFormRef=ref();const policyFormRef=ref();const warningFormRef=ref();const filters=reactive({ ...adminPageCache.stocks.filters, skuId: String(route.query.skuId ?? adminPageCache.stocks.filters.skuId ?? '') , stockStatus: String(route.query.stockStatus ?? adminPageCache.stocks.filters.stockStatus ?? ''), warningStatus: String(route.query.warningStatus ?? adminPageCache.stocks.filters.warningStatus ?? '')});const query=reactive({ ...adminPageCache.stocks.query, sortBy:String(route.query.sortBy||adminPageCache.stocks.query.sortBy||'skuId'),sortOrder:String(route.query.sortOrder||adminPageCache.stocks.query.sortOrder||'asc')});const adjustForm=reactive({adjustmentType:'REPLENISH',changeQuantity:1,totalStock:0,availableStock:0,lockedStock:0,reason:'',remark:''});const policyForm=reactive({stockStatus:'ACTIVE',lowStockThreshold:0,highStockThreshold:0,reason:''});const warningForm=reactive({action:'FREEZE',note:''});const pager=reactive({ page:Number(route.query.page||adminPageCache.stocks.pager.page||1), size:Number(route.query.size||adminPageCache.stocks.pager.size||ADMIN_PAGE_SIZE), total:Number(adminPageCache.stocks.pager.total||0) });const stockStatusMeta=(s)=>getStatusTagMeta('stockStatus',s);const stockWarningMeta=(s)=>getStatusTagMeta('stockWarning',s);const isBusinessAdjust=computed(()=>adjustForm.adjustmentType!=='OTHER');
const validateNonNegative=(_,value,callback)=>{if(value==null||value<0)return callback(new Error(text('validateNonNegative')));callback();};const validatePositive=(_,value,callback)=>{if(value==null||value<=0)return callback(new Error(text('validatePositive')));callback();};const validateRequiredText=(messageKey)=>(_,value,callback)=>{if(!value?.trim())return callback(new Error(text(messageKey)));callback();};const adjustRules={adjustmentType:[{required:true,message:text('adjustmentType'),trigger:'change'}],changeQuantity:[{validator:validatePositive,trigger:'change'}],totalStock:[{validator:validateNonNegative,trigger:'change'}],availableStock:[{validator:validateNonNegative,trigger:'change'}],lockedStock:[{validator:validateNonNegative,trigger:'change'}],reason:[{validator:validateRequiredText('validateReasonRequired'),trigger:'change'}]};const policyRules={stockStatus:[{required:true,message:text('selectStockStatus'),trigger:'change'}],lowStockThreshold:[{validator:validateNonNegative,trigger:'change'}],highStockThreshold:[{validator:validateNonNegative,trigger:'change'}],reason:[{validator:validateRequiredText('validateReasonRequired'),trigger:'blur'}]};const warningRules={action:[{required:true,message:text('action'),trigger:'change'}],note:[{validator:validateRequiredText('note'),trigger:'blur'}]};
const syncRoute=async()=>{await router.replace({path:'/stocks',query:{...(filters.skuId?{skuId:String(filters.skuId)}:{}),...(filters.stockStatus?{stockStatus:filters.stockStatus}:{}),...(filters.warningStatus?{warningStatus:filters.warningStatus}:{}),...(query.sortBy?{sortBy:query.sortBy}:{}),...(query.sortOrder?{sortOrder:query.sortOrder}:{}),...(pager.page>1?{page:String(pager.page)}:{}),...(pager.size!==ADMIN_PAGE_SIZE?{size:String(pager.size)}:{})}})};
const loadStocks=async()=>{loading.value=true;try{const {data}=await fetchAdminStocks({skuId:filters.skuId||undefined,stockStatus:filters.stockStatus||undefined,warningStatus:filters.warningStatus||undefined,sortBy:query.sortBy||undefined,sortOrder:query.sortOrder||undefined,page:pager.page,size:pager.size});stocks.value=data.data.records||[];pager.total=data.data.total||0;adminPageCache.stocks.list=stocks.value;adminPageCache.stocks.filters={skuId:filters.skuId,stockStatus:filters.stockStatus,warningStatus:filters.warningStatus};adminPageCache.stocks.query={sortBy:query.sortBy,sortOrder:query.sortOrder};adminPageCache.stocks.pager={page:pager.page,size:pager.size,total:pager.total};adminPageCache.stocks.loaded=true;adminPageCache.stocks.updatedAt=Date.now();}catch(error){ElMessage.error(error?.response?.data?.message||text('loadFailed'));}finally{loading.value=false;}};
const exportStocks=()=>exportRowsToCsv(text('exportName'),stocks.value,[{label:text('skuId'),value:'skuId'},{label:text('skuName'),value:'skuName'},{label:text('available'),value:'availableStock'},{label:text('locked'),value:'lockedStock'},{label:text('total'),value:'totalStock'},{label:text('stockStatus'),value:'stockStatus'},{label:text('warningStatus'),value:'warningStatus'},{label:text('lowThreshold'),value:'lowStockThreshold'},{label:text('highThreshold'),value:'highStockThreshold'}]);
const handleSearch=async()=>{pager.page=1;await syncRoute();await loadStocks();};const handleReset=async()=>{filters.skuId='';filters.stockStatus='';filters.warningStatus='';query.sortBy='skuId';query.sortOrder='asc';pager.page=1;pager.size=ADMIN_PAGE_SIZE;await syncRoute();await loadStocks();};const handlePageChange=async(page)=>{pager.page=page;await syncRoute();await loadStocks();};const handleSizeChange=async(size)=>{pager.size=size;pager.page=1;await syncRoute();await loadStocks();};const handleSortChange=async({prop,order})=>{query.sortBy=prop||'skuId';query.sortOrder=order==='descending'?'desc':'asc';pager.page=1;await syncRoute();await loadStocks();};const recalculateAvailable=()=>{if(adjustForm.adjustmentType==='OTHER')adjustForm.availableStock=Math.max(0,(Number(adjustForm.totalStock)||0)-(Number(adjustForm.lockedStock)||0));};const resetAdjustReason=()=>{if(!currentAdjustReasons.value.includes(adjustForm.reason))adjustForm.reason='';};const handleAdjustTypeChange=()=>{resetAdjustReason();if(adjustForm.adjustmentType==='OTHER')recalculateAvailable();else adjustForm.changeQuantity=Math.max(1,Number(adjustForm.changeQuantity)||1);};const resetAdjustValidation=async()=>{await nextTick();adjustFormRef.value?.clearValidate();};const resetPolicyValidation=async()=>{await nextTick();policyFormRef.value?.clearValidate();};const resetWarningValidation=async()=>{await nextTick();warningFormRef.value?.clearValidate();};const openAdjust=async(row)=>{currentSkuId.value=row.skuId;Object.assign(currentStockSnapshot,{totalStock:row.totalStock,availableStock:row.availableStock,lockedStock:row.lockedStock});Object.assign(adjustForm,{adjustmentType:'REPLENISH',changeQuantity:1,totalStock:row.totalStock,availableStock:row.availableStock,lockedStock:row.lockedStock,reason:'',remark:''});adjustVisible.value=true;await resetAdjustValidation();};const openPolicy=async(row)=>{currentSkuId.value=row.skuId;Object.assign(policyForm,{stockStatus:row.stockStatus,lowStockThreshold:row.lowStockThreshold,highStockThreshold:row.highStockThreshold,reason:''});policyVisible.value=true;await resetPolicyValidation();};const openWarning=async(row)=>{currentSkuId.value=row.skuId;Object.assign(warningForm,{action:row.warningStatus==='LOW'?'FREEZE':'OFFLINE',note:''});warningVisible.value=true;await resetWarningValidation();};
const submitAdjust=async()=>{const valid=await adjustFormRef.value?.validate().catch(()=>false);if(!valid)return;if(!isBusinessAdjust.value&&adjustForm.totalStock!==adjustForm.availableStock+adjustForm.lockedStock){ElMessage.warning(text('validateSumMismatch'));return;}const payload=isBusinessAdjust.value?{adjustmentType:adjustForm.adjustmentType,changeQuantity:adjustForm.changeQuantity,reason:adjustForm.reason,remark:adjustForm.remark}:{adjustmentType:'OTHER',totalStock:adjustForm.totalStock,availableStock:adjustForm.availableStock,lockedStock:adjustForm.lockedStock,reason:adjustForm.reason,remark:adjustForm.remark};try{await confirmAction(`${text('adjust')} SKU ${currentSkuId.value}`);adjustSubmitting.value=true;await adjustAdminStock(currentSkuId.value,payload);ElMessage.success(text('adjustSuccess'));adjustVisible.value=false;await loadStocks();}catch(error){if(error!=='cancel')ElMessage.error(error?.response?.data?.message||text('adjustFailed'));}finally{adjustSubmitting.value=false;}};const submitPolicy=async()=>{const valid=await policyFormRef.value?.validate().catch(()=>false);if(!valid)return;if(policyForm.highStockThreshold<policyForm.lowStockThreshold){ElMessage.warning(text('highThresholdLessThanLow'));return;}try{await confirmAction(`${text('policy')} SKU ${currentSkuId.value}`);policySubmitting.value=true;await updateAdminStockPolicy(currentSkuId.value,{...policyForm});ElMessage.success(text('policySuccess'));policyVisible.value=false;await loadStocks();}catch(error){if(error!=='cancel')ElMessage.error(error?.response?.data?.message||text('policyFailed'));}finally{policySubmitting.value=false;}};const submitWarning=async()=>{const valid=await warningFormRef.value?.validate().catch(()=>false);if(!valid)return;try{await confirmAction(`${text('warningHandle')} SKU ${currentSkuId.value}`);warningSubmitting.value=true;await handleAdminStockWarning(currentSkuId.value,{...warningForm});ElMessage.success(text('warningSuccess'));warningVisible.value=false;await loadStocks();}catch(error){if(error!=='cancel')ElMessage.error(error?.response?.data?.message||text('warningFailed'));}finally{warningSubmitting.value=false;}};const checkStockConsistency=async(row)=>{try{await confirmAction(`${text('consistencyCheck')} SKU ${row.skuId}`);checkingSkuId.value=row.skuId;const {data}=await checkAdminStockConsistency(row.skuId);const result=data.data;const statusText=result.consistent?text('consistencyConsistent'):text('consistencyInconsistent');ElMessage[result.consistent?'success':'warning'](text('consistencyCheckSuccess',{status:statusText}));}catch(error){if(error!=='cancel')ElMessage.error(error?.response?.data?.message||text('consistencyCheckFailed'));}finally{checkingSkuId.value=null;}};const handleLogout=async()=>{await adminStore.logout();router.push('/login');};const currentRouteQuerySignature=()=>JSON.stringify({skuId:String(route.query.skuId??''),stockStatus:String(route.query.stockStatus??''),warningStatus:String(route.query.warningStatus??''),sortBy:String(route.query.sortBy||'skuId'),sortOrder:String(route.query.sortOrder||'asc'),page:Number(route.query.page||1),size:Number(route.query.size||ADMIN_PAGE_SIZE)});let loadedRouteQuerySignature='';const applyRouteQuery=()=>{filters.skuId=String(route.query.skuId??'');filters.stockStatus=String(route.query.stockStatus??'');filters.warningStatus=String(route.query.warningStatus??'');query.sortBy=String(route.query.sortBy||'skuId');query.sortOrder=String(route.query.sortOrder||'asc');pager.page=Number(route.query.page||1);pager.size=Number(route.query.size||ADMIN_PAGE_SIZE);};const initializeStocksPage=async(force=false)=>{const routeSignature=currentRouteQuerySignature();const routeHasFilter=Boolean(route.query.skuId||route.query.stockStatus||route.query.warningStatus||route.query.sortBy||route.query.sortOrder||route.query.page||route.query.size);if(routeSignature!==loadedRouteQuerySignature){applyRouteQuery();force=true;}if(!force&&!routeHasFilter&&adminPageCache.stocks.loaded&&stocks.value.length&&isCacheFresh(adminPageCache.stocks.updatedAt)){return;}await loadStocks();loadedRouteQuerySignature=routeSignature;};onMounted(()=>initializeStocksPage());onActivated(()=>initializeStocksPage());
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
  border: none;
  border-radius: 999px;
  color: #2563eb;
  background: linear-gradient(135deg, #eff6ff, #dbeafe);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}
.stock-action-button--warning {
  color: #92400e;
  background: linear-gradient(135deg, #fffbeb, #fef3c7);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}
.stock-action-button--check {
  color: #047857;
  background: linear-gradient(135deg, #ecfdf5, #d1fae5);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}
.stock-action-button--danger {
  color: #be123c;
  background: linear-gradient(135deg, #fff1f2, #ffe4e6);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}
.stock-current-snapshot {
  margin-bottom: 18px;
  padding: 16px 18px;
  border: 1px solid rgba(59, 130, 246, 0.14);
  border-radius: 18px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.09), rgba(14, 165, 233, 0.06));
}
.stock-current-snapshot__title {
  color: #1e3a8a;
  font-size: 13px;
  font-weight: 700;
}
.stock-current-snapshot__items {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 10px;
}
.stock-current-snapshot__items span {
  padding: 7px 11px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}
.stock-adjust-control {
  width: 100%;
}
.stock-adjust-option {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}
.stock-adjust-option small {
  color: #94a3b8;
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
