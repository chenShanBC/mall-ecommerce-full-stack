<template>
  <AdminLayout title="订单基础管控" @refresh="loadData" @logout="handleLogout">
    <el-card class="admin-page-card">
      <div class="admin-filter-bar">
        <el-input v-model="query.keyword" placeholder="订单号 / 收货人 / 手机号" clearable style="width: 280px" @keyup.enter="handleSearch" />
        <el-select v-model="query.status" clearable placeholder="订单状态" style="width: 180px">
          <el-option label="待支付" value="PENDING_PAYMENT" />
          <el-option label="支付异常" value="PAYMENT_EXCEPTION" />
          <el-option label="已支付" value="PAID" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="已发货" value="SHIPPED" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="超时取消" value="TIMEOUT_CANCELLED" />
          <el-option label="已取消" value="CANCELLED" />
          <el-option label="售后中" value="REFUND_PENDING" />
          <el-option label="已退款" value="REFUNDED" />
          <el-option label="部分退款" value="PARTIALLY_REFUNDED" />
          <el-option label="退款关闭" value="REFUND_CLOSED" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
        <el-date-picker
          v-model="createdStartDate"
          type="date"
          placeholder="创建开始日期"
          value-format="YYYY-MM-DD"
          clearable
          style="width: 160px"
          @change="handleCreatedStartDateChange"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button @click="exportOrders">导出</el-button>
      </div>
      <div class="admin-table-scroll">
        <el-table v-loading="loading" :data="orders" class="admin-table admin-table--with-gap admin-table--safe admin-table--wide" empty-text="暂无订单数据" :row-class-name="orderRowClassName" @sort-change="handleSortChange">
          <el-table-column prop="id" label="订单ID" width="110" sortable="custom" />
          <el-table-column prop="orderNo" label="订单号" min-width="180" sortable="custom" :show-overflow-tooltip="{ effect: 'light', placement: 'bottom-start', showAfter: 300, offset: 8, popperClass: 'admin-table-tooltip' }">
            <template #default="{ row }">
              <div class="order-no-cell">
                <span>{{ row.orderNo }}</span>
                <el-tag v-if="isPaymentExceptionOrder(row)" class="order-exception-badge" type="danger" effect="dark" size="small">人工异常</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="140" sortable="custom"><template #default="{ row }"><el-tag :class="orderStatusMeta(row.status).class" :type="orderStatusMeta(row.status).type" effect="plain">{{ orderStatusMeta(row.status).label }}</el-tag></template></el-table-column>
          <el-table-column prop="receiverName" label="收货人" width="120" sortable="custom">
            <template #default="{ row }">
              <el-tooltip
                :content="row.receiverName || '-'"
                placement="top"
                effect="dark"
                :show-after="200"
                :hide-after="100"
                :disabled="!(row.receiverName && String(row.receiverName).length > 8)"
              >
                <span class="order-cell-ellipsis">{{ row.receiverName || '-' }}</span>
              </el-tooltip>
            </template>
          </el-table-column>
          <el-table-column prop="receiverPhone" label="手机号" width="130" />
          <el-table-column prop="payAmount" label="支付金额(元)" width="140" sortable="custom">
            <template #default="{ row }">{{ formatCentToYuan(row.payAmount) }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" sortable="custom">
            <template #default="{ row }">{{ row.createTime || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="540" fixed="right" align="center">
            <template #default="{ row }">
              <div class="order-actions">
                <el-button class="order-action-btn order-action-btn--detail" size="small" @click="openDetail(row.orderNo)">详情</el-button>
                <el-button v-if="canMarkException(row)" class="order-action-btn order-action-btn--warn" size="small" @click="openException(row)">异常处理</el-button>
                <el-button v-if="canHandlePaymentException(row)" class="order-action-btn order-action-btn--exception" size="small" @click="openPaymentException(row)">支付异常处理</el-button>
                <el-button v-if="canCancelOrder(row)" class="order-action-btn order-action-btn--danger" size="small" :disabled="isPaymentExceptionOrder(row)" @click="cancelOrder(row.orderNo)">{{ isPaymentExceptionOrder(row) ? '已冻结取消' : '取消订单' }}</el-button>
                <el-tooltip v-if="isPaymentExceptionOrder(row)" content="支付异常订单需先在“支付异常处理”中人工确认、恢复或关闭" placement="top">
                  <el-button class="order-action-btn order-action-btn--disabled" size="small" disabled>发货冻结</el-button>
                </el-tooltip>
                <el-button v-if="canShipOrder(row)" class="order-action-btn order-action-btn--primary" size="small" @click="shipOrder(row.orderNo)">确认发货</el-button>
                <el-button v-if="canCompleteOrder(row)" class="order-action-btn order-action-btn--success" size="small" @click="completeOrder(row.orderNo)">完结订单</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="admin-pagination"><el-pagination background layout="sizes, prev, pager, next, total" :current-page="pager.page" :page-size="pager.size" :page-sizes="ADMIN_PAGE_SIZES" :total="pager.total" @current-change="handlePageChange" @size-change="handleSizeChange" /></div>
    </el-card>

    <el-dialog v-model="detailVisible" title="订单详情" width="920px">
      <div v-if="detail" class="admin-detail">
        <div class="admin-detail__title">订单：{{ detail.orderNo }}</div>
        <div class="admin-detail__grid">
          <div class="admin-detail__item"><span class="admin-detail__label">订单状态</span><el-tag :class="orderStatusMeta(detail.status).class" :type="orderStatusMeta(detail.status).type" effect="plain">{{ orderStatusMeta(detail.status).label }}</el-tag></div>
          <div class="admin-detail__item"><span class="admin-detail__label">支付金额(元)</span>{{ formatCentToYuan(detail.payAmount) }}</div>
          <div class="admin-detail__item"><span class="admin-detail__label">收货人</span>{{ detail.receiverName || '-' }}</div>
          <div class="admin-detail__item"><span class="admin-detail__label">手机号</span>{{ detail.receiverPhone || '-' }}</div>
          <div class="admin-detail__item full"><span class="admin-detail__label">收货地址</span>{{ detail.address || '-' }}</div>
          <div class="admin-detail__item full"><span class="admin-detail__label">备注</span>{{ detail.remark || '-' }}</div>
        </div>
        <el-table :data="detail.items || []" class="admin-table">
          <el-table-column prop="skuId" label="SKU ID" width="90" />
          <el-table-column prop="skuName" label="SKU名称" min-width="220" />
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column prop="salePriceCent" label="单价(元)" width="100">
            <template #default="{ row }">{{ formatCentToYuan(row.salePriceCent) }}</template>
          </el-table-column>
          <el-table-column prop="totalAmountCent" label="小计(元)" width="100">
            <template #default="{ row }">{{ formatCentToYuan(row.totalAmountCent) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>

    <el-dialog v-model="exceptionVisible" title="订单异常处理" width="720px">
      <el-form :model="exceptionForm" label-width="100px" class="admin-dialog-form">
        <el-form-item label="异常类型">
          <el-select v-model="exceptionForm.exceptionType" style="width: 100%">
            <el-option v-for="option in exceptionTypeOptions" :key="option.value" :label="option.label" :value="option.value" :disabled="option.disabled">
              <div class="exception-option">
                <span>{{ option.name }}</span>
                <small>{{ option.tip }}</small>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item v-if="exceptionForm.exceptionType === 'USER_NEGOTIATION'" label="协商动作">
          <div class="negotiation-action-cards">
            <button
              v-for="option in negotiationActionOptions"
              :key="option.value"
              type="button"
              class="negotiation-action-card"
              :class="{ 'negotiation-action-card--active': exceptionForm.negotiationAction === option.value }"
              @click="selectNegotiationAction(option)"
            >
              <span>{{ option.label }}</span>
            </button>
          </div>
        </el-form-item>
        <template v-if="exceptionForm.exceptionType === 'ADDRESS_ERROR'">
          <el-form-item label="收货人"><el-input v-model="exceptionForm.receiverName" /></el-form-item>
          <el-form-item label="手机号"><el-input v-model="exceptionForm.receiverPhone" /></el-form-item>
          <el-form-item label="省份">
            <el-select v-model="exceptionForm.receiverProvinceName" placeholder="请选择省份" filterable style="width: 100%" @change="handleProvinceChange">
              <el-option v-for="province in provinceOptions" :key="province.label" :label="province.label" :value="province.label" />
            </el-select>
          </el-form-item>
          <el-form-item label="城市">
            <el-select v-model="exceptionForm.receiverCityName" placeholder="请选择城市" filterable style="width: 100%" @change="handleCityChange">
              <el-option v-for="city in cityOptions" :key="city.label" :label="city.label" :value="city.label" />
            </el-select>
          </el-form-item>
          <el-form-item label="区/县">
            <el-select v-model="exceptionForm.receiverDistrictName" placeholder="请选择区/县" filterable style="width: 100%">
              <el-option v-for="district in districtOptions" :key="district.label" :label="district.label" :value="district.label" />
            </el-select>
          </el-form-item>
          <el-form-item label="详细地址"><el-input v-model="exceptionForm.receiverDetailAddress" type="textarea" :rows="3" placeholder="请输入街道、门牌号等详细地址" /></el-form-item>
        </template>
        <el-alert v-if="exceptionForm.exceptionType === 'PAYMENT_EXCEPTION'" title="提交后订单会进入支付异常状态，操作栏将出现“支付异常处理”按钮；后续需在专用弹窗中选择人工确认、恢复待支付或关闭释放库存。" type="warning" show-icon :closable="false" class="exception-alert" />
        <template v-if="exceptionForm.exceptionType === 'USER_NEGOTIATION' && exceptionForm.negotiationAction === 'USER_NEGOTIATION_SWITCH_SKU'">
          <el-alert title="仅支持已支付/处理中且发货前订单同价换SKU；系统会回补原SKU库存并扣减新SKU库存。存在差价时请先线下处理或走退款/补款流程。" type="warning" show-icon :closable="false" class="exception-alert" />
          <el-form-item label="订单商品">
            <el-select v-model="exceptionForm.orderItemId" placeholder="请选择要替换的商品项" style="width: 100%" @change="handleSwitchOrderItemChange">
              <el-option v-for="item in currentExceptionItems" :key="item.id" :label="`${item.skuName} / SKU:${item.skuId} / 数量:${item.quantity} / 单价:${formatCentToYuan(item.salePriceCent)}元`" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="目标规格">
            <el-select v-model="exceptionForm.targetSkuId" :loading="skuSwitchOptionsLoading" placeholder="请选择该商品的其他同价规格" filterable style="width: 100%">
              <el-option v-for="sku in samePriceSkuSwitchOptions" :key="sku.skuId" :label="formatSkuSwitchOptionLabel(sku)" :value="sku.skuId" />
            </el-select>
          </el-form-item>
          <el-alert v-if="exceptionForm.orderItemId && !skuSwitchOptionsLoading && samePriceSkuSwitchOptions.length === 0" title="当前商品暂无其它同价可切换规格，请先维护商品规格或走退款/补款流程。" type="info" show-icon :closable="false" class="exception-alert" />
          <el-form-item label="差价处理"><el-select v-model="exceptionForm.priceDifferenceHandleType" style="width: 100%"><el-option label="同价换SKU" value="SAME_PRICE" /></el-select></el-form-item>
        </template>
        <el-alert v-if="exceptionForm.exceptionType === 'USER_NEGOTIATION' && exceptionForm.negotiationAction === 'USER_NEGOTIATION_RETURN'" title="仅已发货/已完成订单可申请退货。提交后只创建退款申请并记录协商备注，订单保持当前履约状态；待售后审核或退款执行成功后，再进入退款相关状态。" type="info" show-icon :closable="false" class="exception-alert" />
        <el-alert v-if="exceptionForm.exceptionType === 'LOGISTICS_EXCEPTION'" title="仅已发货订单可模拟物流异常；提交后订单状态回退为已支付，发货时间保留用于追踪。" type="warning" show-icon :closable="false" class="exception-alert" />
        <el-form-item label="处理备注"><el-input v-model="exceptionForm.note" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="exceptionVisible = false">取消</el-button><el-button type="primary" @click="submitException">提交</el-button></template>
    </el-dialog>

    <el-dialog v-model="paymentExceptionVisible" title="支付异常处理" width="760px">
      <div v-if="currentExceptionOrder" class="exception-summary">
        <div class="exception-summary__row"><span>订单号</span><strong>{{ currentExceptionOrder.orderNo }}</strong></div>
        <div class="exception-summary__row"><span>当前状态</span><el-tag :class="orderStatusMeta(currentExceptionOrder.status).class" :type="orderStatusMeta(currentExceptionOrder.status).type" effect="plain">{{ orderStatusMeta(currentExceptionOrder.status).label }}</el-tag></div>
        <div class="exception-summary__row"><span>支付金额</span><strong>{{ formatCentToYuan(currentExceptionOrder.payAmount) }} 元</strong></div>
        <div class="exception-summary__row"><span>收货人</span><strong>{{ currentExceptionOrder.receiverName || '-' }}</strong></div>
        <div class="exception-summary__row"><span>暂停动作</span><strong>支付 / 超时关单 / 发货</strong></div>
        <div class="exception-summary__row exception-summary__row--full"><span>异常/处理备注</span><p>{{ currentExceptionOrder.remark || '暂无备注' }}</p></div>
      </div>
      <el-alert title="该订单已进入支付异常状态，系统已暂停继续支付、超时自动关单和发货履约。订单管理只负责异常工单和履约控制，支付核验来自支付单/渠道/回调，对账差异需转入对账管理，已支付关闭需走售后退款。" type="warning" show-icon :closable="false" class="exception-alert" />
      <div class="payment-verify-panel">
        <div class="payment-verify-panel__header">
          <div>
            <strong>支付状态核验</strong>
            <p>先核验支付单、渠道和回调结论，再解锁安全动作。</p>
          </div>
          <el-button type="primary" :loading="paymentVerifying" @click="verifyPaymentException">核验支付状态</el-button>
        </div>
        <div v-if="paymentVerification" class="payment-verify-grid">
          <div><span>核验结论</span><strong>{{ paymentVerification.conclusion }}</strong></div>
          <div><span>风险等级</span><strong>{{ paymentVerification.riskLevel }}</strong></div>
          <div><span>本地支付单</span><strong>{{ paymentVerification.localPayStatus }}</strong></div>
          <div><span>渠道状态</span><strong>{{ paymentVerification.channelPayStatus }}</strong></div>
          <div><span>金额一致</span><strong>{{ paymentVerification.amountConsistent ? '是' : '否' }}</strong></div>
          <div><span>最近回调</span><strong>{{ paymentVerification.callbackProcessStatus }}</strong></div>
          <div class="payment-verify-grid__full"><span>处理建议</span><p>{{ paymentVerification.message }}</p></div>
        </div>
        <el-empty v-else description="尚未核验，危险处理动作已锁定" :image-size="68" />
      </div>
      <el-form :model="exceptionForm" label-width="100px" class="admin-dialog-form">
        <el-form-item label="处理备注"><el-input v-model="exceptionForm.note" type="textarea" :rows="3" placeholder="请填写人工处理依据，例如支付渠道流水、客服核实记录、对账结论等" /></el-form-item>
      </el-form>
      <div v-if="paymentVerification" class="payment-exception-actions">
        <el-button v-if="isPaymentActionAllowed('TRANSFER_PAY_SYNC')" type="warning" @click="markPayPendingAction('PAY_SYNC_PENDING')">转支付同步状态</el-button>
        <el-button v-if="isPaymentActionAllowed('CONFIRM_PAID')" type="success" @click="markPayPendingAction('ORDER_REPAIR_PENDING')">标记支付补偿</el-button>
        <el-button v-if="isPaymentActionAllowed('RESTORE_PENDING_PAYMENT')" type="primary" plain @click="restorePendingPaymentFromException">恢复待支付</el-button>
        <el-button v-if="isPaymentActionAllowed('CLOSE_AND_RELEASE_STOCK')" type="danger" plain @click="markPayPendingAction('CLOSE_RELEASE_PENDING')">标记关闭并释放库存</el-button>
        <el-button v-if="isPaymentActionAllowed('TRANSFER_AMOUNT_RECONCILE')" type="danger" @click="markPayPendingAction('AMOUNT_RECONCILE_PENDING')">转对账金额标榜</el-button>
      </div>
      <template #footer><el-button @click="paymentExceptionVisible = false">关闭</el-button></template>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import AdminLayout from '../components/AdminLayout.vue';
import { cancelAdminOrder, completeAdminOrder, fetchAdminOrderDetail, fetchAdminOrders, fetchAdminOrderSkuSwitchOptions, handleAdminOrderException, markAdminOrderPaymentExceptionPendingAction, restoreAdminOrderPendingPayment, shipAdminOrder, transferAdminOrderPaymentExceptionToPaySync, updateAdminOrderReceiver, verifyAdminOrderPaymentException } from '../api';
import { useAdminStore } from '../stores/admin';
import { confirmAction } from '../utils/action';
import { exportRowsToCsv } from '../utils/export';
import { t } from '../utils/i18n';
import { ADMIN_PAGE_SIZE, ADMIN_PAGE_SIZES } from '../utils/pagination';
import { getStatusTagMeta } from '../utils/status';

const route = useRoute();
const router = useRouter();
const adminStore = useAdminStore();
const canRemark = computed(() => adminStore.hasPermission('order:remark'));
const canClose = computed(() => adminStore.hasPermission('order:close'));
const canShip = computed(() => adminStore.hasPermission('order:ship'));
const ORDER_STATUS_ALIASES = {
  WAIT_PAY: 'PENDING_PAYMENT',
  WAIT_PAYMENT: 'PENDING_PAYMENT',
  WAITING_PAYMENT: 'PENDING_PAYMENT',
  PAY_WAIT: 'PENDING_PAYMENT',
  PAY_PENDING: 'PENDING_PAYMENT',
  TIMEOUT_CANCEL: 'TIMEOUT_CANCELLED',
  TIMEOUT_CANCELED: 'TIMEOUT_CANCELLED',
  CANCEL_TIMEOUT: 'TIMEOUT_CANCELLED',
};
const normalizeOrderStatus = (status) => {
  const normalized = String(status || '').toUpperCase();
  return ORDER_STATUS_ALIASES[normalized] || normalized;
};
const orderStatusMeta = (status) => {
  const normalized = normalizeOrderStatus(status);
  const meta = getStatusTagMeta('order', normalized);
  return { ...meta, class: `order-status-tag order-status-tag--${normalized.toLowerCase().replace(/_/g, '-')}` };
};
const formatCentToYuan = (cent) => {
  const amount = Number(cent);
  if (!Number.isFinite(amount)) return '-';
  return (amount / 100).toFixed(2);
};
const isPaymentExceptionOrder = (row) => normalizeOrderStatus(row?.status) === 'PAYMENT_EXCEPTION';
const EXCEPTION_TYPE_META = {
  PAYMENT_EXCEPTION: { label: '支付异常', name: '支付异常', tip: '支付状态不一致或需人工核验' },
  ADDRESS_ERROR: { label: '地址错误', name: '地址错误', tip: '修改收货人、手机号或收货地址' },
  USER_NEGOTIATION: { label: '用户协商异常', name: '用户协商异常', tip: '申请退货或发货前协商换SKU' },
  LOGISTICS_EXCEPTION: { label: '物流异常', name: '物流异常', tip: '模拟物流异常并回退履约状态' },
};
const EXCEPTION_OPTIONS_BY_STATUS = {
  PENDING_PAYMENT: ['PAYMENT_EXCEPTION', 'ADDRESS_ERROR'],
  PAID: ['PAYMENT_EXCEPTION', 'ADDRESS_ERROR', 'USER_NEGOTIATION'],
  PROCESSING: ['PAYMENT_EXCEPTION', 'ADDRESS_ERROR', 'USER_NEGOTIATION'],
  SHIPPED: ['PAYMENT_EXCEPTION', 'USER_NEGOTIATION', 'LOGISTICS_EXCEPTION'],
};
const getExceptionOptionsByStatus = (status) => EXCEPTION_OPTIONS_BY_STATUS[normalizeOrderStatus(status)] || [];
const isExceptionOptionAllowed = (status, exceptionType) => getExceptionOptionsByStatus(status).includes(exceptionType);
const exceptionTypeOptions = computed(() => getExceptionOptionsByStatus(currentOrderStatus.value).map((value) => ({
  value,
  ...(EXCEPTION_TYPE_META[value] || { label: value, name: value, tip: '' }),
})));
const isPaymentExceptionAllowed = (status) => isExceptionOptionAllowed(status, 'PAYMENT_EXCEPTION');
const isAddressExceptionAllowed = (status) => isExceptionOptionAllowed(status, 'ADDRESS_ERROR');
const isUserNegotiationAllowed = (status) => isExceptionOptionAllowed(status, 'USER_NEGOTIATION');
const isLogisticsExceptionAllowed = (status) => isExceptionOptionAllowed(status, 'LOGISTICS_EXCEPTION');
const getDefaultExceptionType = (status) => getExceptionOptionsByStatus(status)[0] || '';
const canMarkException = (row) => canRemark.value && !isPaymentExceptionOrder(row) && !!getDefaultExceptionType(row.status);
const canHandlePaymentException = (row) => canRemark.value && isPaymentExceptionOrder(row);
const canCancelOrder = (row) => canClose.value && normalizeOrderStatus(row.status) === 'PENDING_PAYMENT';
const canShipOrder = (row) => canShip.value && ['PAID', 'PROCESSING'].includes(normalizeOrderStatus(row.status));
const canCompleteOrder = (row) => canShip.value && normalizeOrderStatus(row.status) === 'SHIPPED';
const orderRowClassName = ({ row }) => (isPaymentExceptionOrder(row) ? 'order-row--payment-exception' : '');
const query = reactive({ keyword: String(route.query.keyword || ''), status: String(route.query.status || ''), sortBy: String(route.query.sortBy || 'id'), sortOrder: String(route.query.sortOrder || 'desc'), startDate: String(route.query.startDate || '') });
const pager = reactive({ page: Number(route.query.page || 1), size: Number(route.query.size || ADMIN_PAGE_SIZE), total: 0 });
const createdStartDate = ref(query.startDate || '');
const orders = ref([]);
const loading = ref(false);
const detailVisible = ref(false);
const detail = ref(null);
const exceptionVisible = ref(false);
const paymentExceptionVisible = ref(false);
const paymentVerifying = ref(false);
const paymentVerification = ref(null);
const currentOrderNo = ref('');
const currentOrderStatus = ref('');
const currentExceptionOrder = ref(null);
const skuSwitchOptionsLoading = ref(false);
const skuSwitchOptions = ref([]);
const provinceOptions = [
  { label: '北京市', children: [{ label: '北京市', children: [{ label: '东城区' }, { label: '西城区' }, { label: '朝阳区' }, { label: '海淀区' }, { label: '丰台区' }, { label: '石景山区' }, { label: '通州区' }, { label: '昌平区' }, { label: '大兴区' }, { label: '顺义区' }] }] },
  { label: '上海市', children: [{ label: '上海市', children: [{ label: '黄浦区' }, { label: '徐汇区' }, { label: '长宁区' }, { label: '静安区' }, { label: '普陀区' }, { label: '浦东新区' }, { label: '闵行区' }, { label: '宝山区' }, { label: '嘉定区' }, { label: '松江区' }] }] },
  { label: '广东省', children: [{ label: '广州市', children: [{ label: '越秀区' }, { label: '荔湾区' }, { label: '天河区' }, { label: '白云区' }, { label: '番禺区' }] }, { label: '深圳市', children: [{ label: '福田区' }, { label: '罗湖区' }, { label: '南山区' }, { label: '宝安区' }, { label: '龙岗区' }, { label: '龙华区' }] }, { label: '东莞市', children: [{ label: '莞城区' }, { label: '南城区' }, { label: '东城区' }, { label: '万江区' }] }] },
  { label: '浙江省', children: [{ label: '杭州市', children: [{ label: '上城区' }, { label: '拱墅区' }, { label: '西湖区' }, { label: '滨江区' }, { label: '萧山区' }, { label: '余杭区' }] }, { label: '宁波市', children: [{ label: '海曙区' }, { label: '江北区' }, { label: '鄞州区' }, { label: '镇海区' }, { label: '北仑区' }] }] },
  { label: '江苏省', children: [{ label: '南京市', children: [{ label: '玄武区' }, { label: '秦淮区' }, { label: '建邺区' }, { label: '鼓楼区' }, { label: '浦口区' }, { label: '江宁区' }] }, { label: '苏州市', children: [{ label: '姑苏区' }, { label: '虎丘区' }, { label: '吴中区' }, { label: '相城区' }, { label: '工业园区' }] }] },
  { label: '四川省', children: [{ label: '成都市', children: [{ label: '锦江区' }, { label: '青羊区' }, { label: '金牛区' }, { label: '武侯区' }, { label: '成华区' }, { label: '高新区' }] }] },
  { label: '湖北省', children: [{ label: '武汉市', children: [{ label: '江岸区' }, { label: '江汉区' }, { label: '硚口区' }, { label: '汉阳区' }, { label: '武昌区' }, { label: '洪山区' }] }] },
];
const exceptionForm = reactive({ exceptionType: '', negotiationAction: 'USER_NEGOTIATION_RETURN', orderItemId: null, targetSkuId: '', priceDifferenceHandleType: 'SAME_PRICE', receiverName: '', receiverPhone: '', receiverProvinceName: '', receiverCityName: '', receiverDistrictName: '', receiverDetailAddress: '', note: '' });
const currentExceptionItems = computed(() => currentExceptionOrder.value?.items || []);
const samePriceSkuSwitchOptions = computed(() => skuSwitchOptions.value.filter((sku) => sku.samePrice && String(sku.status || '').toUpperCase() === 'ONLINE'));
const formatSkuSwitchOptionLabel = (sku) => `${sku.skuName || sku.specJson || '未命名规格'} / SKU:${sku.skuId} / 库存:${sku.availableStock ?? '-'} / 单价:${formatCentToYuan(sku.salePriceCent)}元`;
const negotiationActionOptions = computed(() => {
  const normalized = normalizeOrderStatus(currentOrderStatus.value);
  return [
    { label: '申请退货', value: 'USER_NEGOTIATION_RETURN', statuses: ['SHIPPED', 'COMPLETED'] },
    { label: '切换SKU', value: 'USER_NEGOTIATION_SWITCH_SKU', statuses: ['PAID', 'PROCESSING'] },
  ].filter((option) => option.statuses.includes(normalized));
});
const selectNegotiationAction = (option) => {
  if (!option) return;
  exceptionForm.negotiationAction = option.value;
};
const ensureNegotiationActionAllowed = () => {
  const option = negotiationActionOptions.value.find((item) => item.value === exceptionForm.negotiationAction);
  if (option) return;
  exceptionForm.negotiationAction = negotiationActionOptions.value[0]?.value || 'USER_NEGOTIATION_RETURN';
};
const cityOptions = computed(() => provinceOptions.find((province) => province.label === exceptionForm.receiverProvinceName)?.children || []);
const districtOptions = computed(() => cityOptions.value.find((city) => city.label === exceptionForm.receiverCityName)?.children || []);
const handleProvinceChange = () => {
  exceptionForm.receiverCityName = '';
  exceptionForm.receiverDistrictName = '';
};
const handleCityChange = () => {
  exceptionForm.receiverDistrictName = '';
};
const handleSwitchOrderItemChange = async () => {
  exceptionForm.targetSkuId = '';
  skuSwitchOptions.value = [];
  if (!currentOrderNo.value || !exceptionForm.orderItemId) return;
  skuSwitchOptionsLoading.value = true;
  try {
    const { data } = await fetchAdminOrderSkuSwitchOptions(currentOrderNo.value, exceptionForm.orderItemId);
    skuSwitchOptions.value = data.data || [];
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '加载可切换规格失败');
  } finally {
    skuSwitchOptionsLoading.value = false;
  }
};

const syncRoute = async () => {
  await router.replace({
    path: '/orders',
    query: {
      ...(query.keyword ? { keyword: query.keyword } : {}),
      ...(query.status ? { status: query.status } : {}),
      ...(query.sortBy ? { sortBy: query.sortBy } : {}),
      ...(query.sortOrder ? { sortOrder: query.sortOrder } : {}),
      ...(query.startDate ? { startDate: query.startDate } : {}),
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
    { label: '支付金额(元)', value: (row) => formatCentToYuan(row.payAmount) },
  ]);
};

const handleCreatedStartDateChange = (value) => {
  query.startDate = value || '';
};
const handleSearch = async () => { pager.page = 1; await syncRoute(); await loadData(); };
const handleReset = async () => { query.keyword = ''; query.status = ''; query.startDate = ''; createdStartDate.value = ''; query.sortBy = 'id'; query.sortOrder = 'desc'; pager.page = 1; pager.size = ADMIN_PAGE_SIZE; await syncRoute(); await loadData(); };
const handlePageChange = async (page) => { pager.page = page; await syncRoute(); await loadData(); };
const handleSizeChange = async (size) => { pager.size = size; pager.page = 1; await syncRoute(); await loadData(); };
const handleSortChange = async ({ prop, order }) => { query.sortBy = prop || 'id'; query.sortOrder = order === 'descending' ? 'desc' : 'asc'; pager.page = 1; await syncRoute(); await loadData(); };
const openDetail = async (orderNo) => { const { data } = await fetchAdminOrderDetail(orderNo); detail.value = data.data; detailVisible.value = true; };
const openException = async (row) => {
  if (!canMarkException(row)) {
    ElMessage.warning('当前订单状态不允许异常处理');
    return;
  }
  currentOrderNo.value = row.orderNo;
  currentOrderStatus.value = row.status;
  const { data } = await fetchAdminOrderDetail(row.orderNo);
  const orderDetail = data.data || {};
  currentExceptionOrder.value = orderDetail;
  currentOrderStatus.value = orderDetail.status || row.status;
  skuSwitchOptions.value = [];
  Object.assign(exceptionForm, { exceptionType: getDefaultExceptionType(currentOrderStatus.value), negotiationAction: 'USER_NEGOTIATION_RETURN', orderItemId: null, targetSkuId: '', priceDifferenceHandleType: 'SAME_PRICE', receiverName: orderDetail.receiverName || row.receiverName || '', receiverPhone: orderDetail.receiverPhone || row.receiverPhone || '', receiverProvinceName: orderDetail.receiverProvinceName || '', receiverCityName: orderDetail.receiverCityName || '', receiverDistrictName: orderDetail.receiverDistrictName || '', receiverDetailAddress: orderDetail.receiverDetailAddress || '', note: '' });
  if (exceptionForm.exceptionType === 'USER_NEGOTIATION') {
    ensureNegotiationActionAllowed();
  }
  exceptionVisible.value = true;
};

const openPaymentException = async (row) => {
  currentOrderNo.value = row.orderNo;
  currentOrderStatus.value = row.status;
  paymentVerification.value = null;
  const { data } = await fetchAdminOrderDetail(row.orderNo);
  const orderDetail = data.data || {};
  currentExceptionOrder.value = orderDetail;
  currentOrderStatus.value = orderDetail.status || row.status;
  exceptionForm.exceptionType = 'PAYMENT_EXCEPTION';
  exceptionForm.note = '';
  paymentExceptionVisible.value = true;
};

const showPaymentVerificationMessage = () => {
  ElMessage.success('核验成功，请查看下方具体核验结果信息！');
};

const verifyPaymentException = async () => {
  if (!currentOrderNo.value) return;
  paymentVerifying.value = true;
  try {
    const { data } = await verifyAdminOrderPaymentException(currentOrderNo.value);
    paymentVerification.value = data.data;
    showPaymentVerificationMessage();
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '核验失败');
  } finally {
    paymentVerifying.value = false;
  }
};

const isPaymentActionAllowed = (action) => Array.isArray(paymentVerification.value?.allowedActions) && paymentVerification.value.allowedActions.includes(action);

const pendingPayActionMeta = {
  PAY_SYNC_PENDING: { label: '转支付同步状态', target: '支付单管理', action: async () => transferAdminOrderPaymentExceptionToPaySync(currentOrderNo.value, { note: exceptionForm.note }) },
  ORDER_REPAIR_PENDING: { label: '标记支付补偿', target: '支付单管理', action: async () => markAdminOrderPaymentExceptionPendingAction(currentOrderNo.value, { action: 'ORDER_REPAIR_PENDING', note: exceptionForm.note }) },
  CLOSE_RELEASE_PENDING: { label: '标记关闭并释放库存', target: '支付单管理', action: async () => markAdminOrderPaymentExceptionPendingAction(currentOrderNo.value, { action: 'CLOSE_RELEASE_PENDING', note: exceptionForm.note }) },
  AMOUNT_RECONCILE_PENDING: { label: '转对账金额标榜', target: '对账管理', action: async () => markAdminOrderPaymentExceptionPendingAction(currentOrderNo.value, { action: 'AMOUNT_RECONCILE_PENDING', note: exceptionForm.note }) },
};

const markPayPendingAction = async (action) => {
  const meta = pendingPayActionMeta[action];
  if (!meta) return;
  try {
    await confirmAction(`确认${meta.label}吗？该操作会在${meta.target}中标注对应待处理需求。`);
    await meta.action();
    ElMessage.success(`已经标记对应功能待处理，请到${meta.target}进行处理`);
    paymentExceptionVisible.value = false;
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || '标记待处理失败');
  }
};

const applyOperationResult = async (operationResult) => {
  const latestOrder = operationResult?.data?.order;
  if (latestOrder) {
    detail.value = latestOrder;
    currentExceptionOrder.value = latestOrder;
    currentOrderStatus.value = latestOrder.status;
    orders.value = orders.value.map((row) => (row.orderNo === latestOrder.orderNo ? {
      ...row,
      status: latestOrder.status,
      payAmount: latestOrder.payAmount,
      receiverName: latestOrder.receiverName,
      receiverPhone: latestOrder.receiverPhone,
    } : row));
  } else if (currentOrderNo.value) {
    const { data } = await fetchAdminOrderDetail(currentOrderNo.value);
    detail.value = data.data;
    currentExceptionOrder.value = data.data;
    currentOrderStatus.value = data.data?.status || currentOrderStatus.value;
  }
};

const submitException = async () => {
  try {
    if (exceptionForm.exceptionType === 'ADDRESS_ERROR') {
      const requiredFields = [exceptionForm.receiverName, exceptionForm.receiverPhone, exceptionForm.receiverProvinceName, exceptionForm.receiverCityName, exceptionForm.receiverDistrictName, exceptionForm.receiverDetailAddress];
      if (requiredFields.some((field) => !String(field || '').trim())) {
        ElMessage.error('请完整填写收货人、手机号、省、市、区/县和详细地址');
        return;
      }
    }
    if (exceptionForm.exceptionType === 'USER_NEGOTIATION') {
      const normalizedStatus = normalizeOrderStatus(currentOrderStatus.value);
      if (exceptionForm.negotiationAction === 'USER_NEGOTIATION_RETURN' && !['SHIPPED', 'COMPLETED'].includes(normalizedStatus)) {
        ElMessage.error('用户协商申请退货仅支持已发货或已完成订单');
        return;
      }
      if (exceptionForm.negotiationAction === 'USER_NEGOTIATION_SWITCH_SKU' && !['PAID', 'PROCESSING'].includes(normalizedStatus)) {
        ElMessage.error('用户协商切换SKU仅支持已支付或处理中且发货前订单');
        return;
      }
    }
    await confirmAction(t('orderManage.confirmException', { orderNo: currentOrderNo.value }));
    if (exceptionForm.exceptionType === 'ADDRESS_ERROR') {
      const { data } = await updateAdminOrderReceiver(currentOrderNo.value, {
        receiverName: exceptionForm.receiverName,
        receiverPhone: exceptionForm.receiverPhone,
        receiverProvinceName: exceptionForm.receiverProvinceName,
        receiverCityName: exceptionForm.receiverCityName,
        receiverDistrictName: exceptionForm.receiverDistrictName,
        receiverDetailAddress: exceptionForm.receiverDetailAddress,
        note: exceptionForm.note,
      });
      detail.value = data.data;
      ElMessage.success(t('orderManage.exceptionSuccess'));
    } else {
      const payload = {
        ...exceptionForm,
        exceptionType: exceptionForm.exceptionType === 'USER_NEGOTIATION' ? exceptionForm.negotiationAction : exceptionForm.exceptionType,
        targetSkuId: exceptionForm.targetSkuId ? Number(exceptionForm.targetSkuId) : null,
      };
      const { data: operationResult } = await handleAdminOrderException(currentOrderNo.value, payload);
      await applyOperationResult(operationResult);
      if (exceptionForm.exceptionType === 'PAYMENT_EXCEPTION' && normalizeOrderStatus(operationResult?.data?.order?.status) !== 'PAYMENT_EXCEPTION') {
        ElMessage.error('支付异常标记未生效，请刷新后重试');
        return;
      }
      ElMessage.success(operationResult?.data?.message || '已标记支付异常，请在操作栏点击“支付异常处理”继续处理');
    }
    exceptionVisible.value = false;
    await loadData();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || t('orderManage.exceptionFailed'));
  }
};

const restorePendingPaymentFromException = async () => {
  if (!isPaymentActionAllowed('RESTORE_PENDING_PAYMENT')) {
    ElMessage.warning('请先核验支付状态，且当前核验结论不允许恢复待支付');
    return;
  }
  try {
    await confirmAction('确认将该订单恢复为待支付吗？恢复后会重新进入支付和超时关单流程。');
    const { data: operationResult } = await restoreAdminOrderPendingPayment(currentOrderNo.value, { note: exceptionForm.note });
    await applyOperationResult(operationResult);
    ElMessage.success(operationResult?.data?.message || '已恢复待支付');
    paymentVerification.value = null;
    paymentExceptionVisible.value = false;
    await loadData();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || '恢复待支付失败');
  }
};

const cancelOrder = async (orderNo) => { try { await confirmAction(t('orderManage.confirmCancel', { orderNo })); await cancelAdminOrder(orderNo); ElMessage.success(t('orderManage.cancelSuccess')); await loadData(); } catch (error) { if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || t('orderManage.cancelFailed')); } };
const shipOrder = async (orderNo) => { try { await confirmAction(t('orderManage.confirmShip', { orderNo })); await shipAdminOrder(orderNo); ElMessage.success(t('orderManage.shipSuccess')); await loadData(); } catch (error) { if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || t('orderManage.shipFailed')); } };
const completeOrder = async (orderNo) => { try { await confirmAction(t('orderManage.confirmComplete', { orderNo })); await completeAdminOrder(orderNo); ElMessage.success(t('orderManage.completeSuccess')); await loadData(); } catch (error) { if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || t('orderManage.completeFailed')); } };
const handleLogout = async () => { await adminStore.logout(); router.push('/login'); };

onMounted(loadData);
</script>

<style scoped>
.order-actions {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  align-items: center;
  justify-content: center;
  min-width: max-content;
  white-space: nowrap;
}

.order-action-btn {
  flex: 0 0 auto;
  min-width: 84px;
  margin: 0;
  border: none;
  border-radius: 999px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}

.order-action-btn--detail { color: #2563eb; background: linear-gradient(135deg, #eff6ff, #dbeafe); }
.order-action-btn--warn { color: #92400e; background: linear-gradient(135deg, #fffbeb, #fef3c7); }
.order-action-btn--exception { color: #fff; background: linear-gradient(135deg, #f97316, #dc2626); }
.order-action-btn--danger { color: #be123c; background: linear-gradient(135deg, #fff1f2, #ffe4e6); }
.order-action-btn--primary { color: #4f46e5; background: linear-gradient(135deg, #eef2ff, #e0e7ff); }
.order-action-btn--success { color: #047857; background: linear-gradient(135deg, #ecfdf5, #d1fae5); }
.order-action-btn--disabled,
.order-action-btn.is-disabled,
.order-action-btn.is-disabled:hover {
  color: #94a3b8;
  background: #f1f5f9;
  box-shadow: none;
  opacity: 0.72;
}

.order-no-cell {
  display: inline-flex;
  gap: 8px;
  align-items: center;
}

.order-exception-badge {
  border: 0;
  background: linear-gradient(135deg, #f97316, #dc2626);
}

:deep(.order-row--payment-exception) {
  --el-table-tr-bg-color: #fff7ed;
}

:deep(.order-row--payment-exception td) {
  background: #fff7ed !important;
}

.order-status-tag {
  height: 24px;
  padding: 0 10px;
  justify-content: center;
  border-radius: 4px;
  font-weight: 500;
  line-height: 22px;
}

.order-status-tag--pending-payment {
  --el-tag-text-color: #d48806;
  --el-tag-bg-color: #fffbe6;
  --el-tag-border-color: #ffe58f;
}

.order-status-tag--payment-exception {
  --el-tag-text-color: #b45309;
  --el-tag-bg-color: #fffbeb;
  --el-tag-border-color: #f59e0b;
}

.exception-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 14px;
  border: 1px solid #fde68a;
  border-radius: 14px;
  background: #fffbeb;
}

.exception-summary__row {
  display: flex;
  gap: 8px;
  align-items: center;
  color: #475569;
  font-size: 13px;
}

.exception-summary__row span {
  color: #92400e;
  font-weight: 600;
}

.exception-summary__row--full {
  grid-column: 1 / -1;
  align-items: flex-start;
}

.exception-summary__row p {
  flex: 1;
  margin: 0;
  color: #334155;
  line-height: 1.6;
  word-break: break-all;
}

.exception-alert {
  margin-bottom: 14px;
}

.payment-exception-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.payment-verify-panel {
  padding: 14px;
  margin-bottom: 16px;
  border: 1px solid #dbeafe;
  border-radius: 14px;
  background: linear-gradient(180deg, #eff6ff 0%, #ffffff 100%);
}

.payment-verify-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 12px;
}

.payment-verify-panel__header p {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 13px;
}

.payment-verify-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.payment-verify-grid div {
  padding: 10px;
  border-radius: 10px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
}

.payment-verify-grid span {
  display: block;
  margin-bottom: 4px;
  color: #64748b;
  font-size: 12px;
}

.payment-verify-grid strong {
  color: #0f172a;
  font-size: 13px;
  word-break: break-all;
}

.payment-verify-grid__full {
  grid-column: 1 / -1;
}

.payment-verify-grid__full p {
  margin: 0;
  color: #334155;
  line-height: 1.6;
}

.order-status-tag--paid {
  --el-tag-text-color: #1677ff;
  --el-tag-bg-color: #e6f4ff;
  --el-tag-border-color: #91caff;
}

.order-status-tag--processing,
.order-status-tag--shipped {
  --el-tag-text-color: #531dab;
  --el-tag-bg-color: #f9f0ff;
  --el-tag-border-color: #d3adf7;
}

.order-status-tag--completed {
  --el-tag-text-color: #389e0d;
  --el-tag-bg-color: #f6ffed;
  --el-tag-border-color: #b7eb8f;
}

.order-status-tag--timeout-cancelled {
  --el-tag-text-color: #cf1322;
  --el-tag-bg-color: #fff1f0;
  --el-tag-border-color: #ffa39e;
}

.order-status-tag--cancelled {
  --el-tag-text-color: #8c8c8c;
  --el-tag-bg-color: #f5f5f5;
  --el-tag-border-color: #d9d9d9;
}

.negotiation-action-cards {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.negotiation-action-card {
  height: 34px;
  padding: 0 16px;
  border: 1px solid #dbeafe;
  border-radius: 999px;
  background: #f8fbff;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.18s ease;
}

.negotiation-action-card:hover {
  border-color: #93c5fd;
  color: #2563eb;
  background: #eff6ff;
}

.negotiation-action-card--active {
  border-color: transparent;
  color: #fff;
  background: linear-gradient(135deg, #6366f1, #2563eb);
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.2);
}
.order-cell-ellipsis { display: inline-block; max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; vertical-align: middle; }
</style>
