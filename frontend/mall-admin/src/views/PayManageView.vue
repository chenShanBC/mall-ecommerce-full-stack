<template>
  <AdminLayout title="支付单管控" @refresh="loadData" @logout="handleLogout">
    <el-card class="admin-page-card">
      <el-tabs v-model="activeTab" class="pay-manage-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="支付单" name="pays">
          <div class="admin-filter-bar">
            <el-input v-model="query.keyword" placeholder="订单号 / 支付单号" clearable style="width: 280px" @keyup.enter="handleSearch" />
            <el-select v-model="query.status" clearable placeholder="支付状态" style="width: 180px">
              <el-option label="待支付" value="PENDING" />
              <el-option label="支付中" value="PAYING" />
              <el-option label="支付成功" value="SUCCESS" />
              <el-option label="支付失败" value="FAILED" />
              <el-option label="已关闭" value="CLOSED" />
              <el-option label="退款待处理" value="REFUND_PENDING" />
              <el-option label="退款中" value="REFUNDING" />
              <el-option label="部分退款" value="PARTIALLY_REFUNDED" />
              <el-option label="已退款" value="REFUNDED" />
              <el-option label="退款失败" value="REFUND_FAILED" />
            </el-select>
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
            <el-button @click="exportPays">导出</el-button>
            <el-button v-if="canViewCallbacks" type="warning" plain @click="openCallbacks()">回调记录</el-button>
          </div>

          <div class="admin-table-scroll">
            <el-table v-loading="loading" :data="rows" class="admin-table admin-table--with-gap admin-table--safe admin-table--wide" empty-text="暂无支付单数据" @sort-change="handleSortChange">
              <el-table-column prop="id" label="ID" width="90" sortable="custom" />
              <el-table-column prop="payOrderNo" label="支付单号" min-width="220" sortable="custom" :show-overflow-tooltip="{ effect: 'light', placement: 'bottom-start', showAfter: 300, offset: 8, popperClass: 'admin-table-tooltip' }">
                <template #default="{ row }">
                  <div class="pay-no-cell">
                    <span>{{ row.payOrderNo }}</span>
                    <el-tag v-if="rowPendingAction(row)" class="pay-pending-action-badge" :type="pendingActionMeta(rowPendingAction(row)).type" effect="dark" size="small">{{ pendingActionMeta(rowPendingAction(row)).label }}</el-tag>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="orderNo" label="订单号" min-width="180" sortable="custom" show-overflow-tooltip />
              <el-table-column prop="status" label="状态" width="140" sortable="custom">
                <template #default="{ row }"><el-tag :type="payStatusMeta(row.status).type">{{ payStatusMeta(row.status).label }}</el-tag></template>
              </el-table-column>
              <el-table-column prop="payAmount" label="支付金额(元)" width="140" sortable="custom">
                <template #default="{ row }">{{ formatCentToYuan(row.payAmount) }}</template>
              </el-table-column>
              <el-table-column prop="payChannel" label="支付渠道" width="120" show-overflow-tooltip />
              <el-table-column label="操作" width="430" fixed="right" align="center">
                <template #default="{ row }">
                  <div class="pay-actions">
                    <el-button class="pay-action-btn pay-action-btn--detail" size="small" @click="openDetail(row.orderNo)">详情</el-button>
                    <el-tooltip v-if="canSyncPayment && canSyncPayStatus(row)" content="查询支付渠道状态；仅当渠道已支付但本地未成功时会更新支付单，支付单已成功但订单未补偿时会尝试补偿订单" placement="top">
                      <el-button class="pay-action-btn pay-action-btn--warn" size="small" @click="syncStatus(row)">查渠道同步</el-button>
                    </el-tooltip>
                    <el-button v-if="canRepairPayment && canRepairPaidOrder(row)" class="pay-action-btn pay-action-btn--success" size="small" @click="repairPaid(row)">补偿订单</el-button>
                    <el-button v-if="canViewRefunds(row)" class="pay-action-btn pay-action-btn--refund" size="small" @click="openRefunds(row.orderNo)">退款明细</el-button>
                    <el-button v-if="canClosePayment && canClosePayOrder(row)" class="pay-action-btn pay-action-btn--danger" size="small" @click="openClose(row)">关闭</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="admin-pagination">
            <el-pagination background layout="sizes, prev, pager, next, total" :current-page="pager.page" :page-size="pager.size" :page-sizes="ADMIN_PAGE_SIZES" :total="pager.total" @current-change="handlePageChange" @size-change="handleSizeChange" />
          </div>
        </el-tab-pane>

        <el-tab-pane label="退款单" name="refunds">
          <div class="admin-filter-bar">
            <el-input v-model="refundQuery.keyword" placeholder="退款单号 / 订单号" clearable style="width: 300px" @keyup.enter="handleRefundSearch" />
            <el-select v-model="refundQuery.status" clearable placeholder="退款状态" style="width: 180px">
              <el-option label="退款申请" value="REFUND_PENDING" />
              <el-option label="退款处理中" value="REFUNDING" />
              <el-option label="退款成功" value="REFUND_SUCCESS" />
              <el-option label="退款失败" value="REFUND_FAILED" />
              <el-option label="退款关闭" value="REFUND_CLOSED" />
            </el-select>
            <el-button type="primary" @click="handleRefundSearch">查询</el-button>
            <el-button @click="handleRefundReset">重置</el-button>
            <el-button v-if="canViewCallbacks" type="warning" plain @click="openCallbacks">回调记录</el-button>
          </div>

          <div class="admin-table-scroll">
            <el-table v-loading="refundLoading" :data="refundRows" class="admin-table admin-table--with-gap admin-table--safe admin-table--wide" row-key="refundNo" empty-text="暂无退款单数据">
              <el-table-column type="expand">
                <template #default="{ row }">
                  <el-table :data="row.items || []" size="small" border empty-text="暂无退款明细">
                    <el-table-column prop="orderItemId" label="订单明细ID" width="130" />
                    <el-table-column prop="skuId" label="SKU ID" width="130" />
                    <el-table-column prop="quantity" label="退款数量" width="120" />
                    <el-table-column prop="refundAmountCent" label="明细金额(元)" min-width="140">
                      <template #default="{ row: item }">{{ formatCentToYuan(item.refundAmountCent) }}</template>
                    </el-table-column>
                  </el-table>
                </template>
              </el-table-column>
              <el-table-column prop="refundNo" label="退款单号" min-width="190">
                <template #default="{ row }">
                  <el-tooltip :content="textOrDash(row.refundNo)" placement="top" effect="light" popper-class="admin-hover-tooltip">
                    <span class="hover-ellipsis">{{ textOrDash(row.refundNo) }}</span>
                  </el-tooltip>
                </template>
              </el-table-column>
              <el-table-column prop="orderNo" label="订单号" min-width="180">
                <template #default="{ row }">
                  <el-tooltip :content="textOrDash(row.orderNo)" placement="top" effect="light" popper-class="admin-hover-tooltip">
                    <span class="hover-ellipsis">{{ textOrDash(row.orderNo) }}</span>
                  </el-tooltip>
                </template>
              </el-table-column>
              <el-table-column prop="refundAmountCent" label="退款金额(元)" width="140">
                <template #default="{ row }">{{ formatCentToYuan(row.refundAmountCent) }}</template>
              </el-table-column>
              <el-table-column prop="channelRefundNo" label="渠道退款号" min-width="170">
                <template #default="{ row }">
                  <el-tooltip :content="textOrDash(row.channelRefundNo)" placement="top" effect="light" popper-class="admin-hover-tooltip">
                    <span class="hover-ellipsis">{{ textOrDash(row.channelRefundNo) }}</span>
                  </el-tooltip>
                </template>
              </el-table-column>
              <el-table-column prop="refundStatus" label="状态" width="130">
                <template #default="{ row }"><el-tag :type="refundStatusMeta(row.refundStatus).type">{{ refundStatusMeta(row.refundStatus).label }}</el-tag></template>
              </el-table-column>
              <el-table-column prop="refundReason" label="原因" min-width="160">
                <template #default="{ row }">
                  <el-tooltip :content="textOrDash(row.refundReason)" placement="top" effect="light" popper-class="admin-hover-tooltip">
                    <span class="hover-ellipsis">{{ textOrDash(row.refundReason) }}</span>
                  </el-tooltip>
                </template>
              </el-table-column>
              <el-table-column prop="failReason" label="失败原因" min-width="180">
                <template #default="{ row }">
                  <el-tooltip :content="textOrDash(row.failReason)" placement="top" effect="light" popper-class="admin-hover-tooltip">
                    <span class="hover-ellipsis">{{ textOrDash(row.failReason) }}</span>
                  </el-tooltip>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="创建时间" min-width="170">
                <template #default="{ row }">
                  <el-tooltip :content="textOrDash(row.createdAt)" placement="top" effect="light" popper-class="admin-hover-tooltip">
                    <span class="hover-ellipsis">{{ textOrDash(row.createdAt) }}</span>
                  </el-tooltip>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140" fixed="right">
                <template #default="{ row }">
                  <el-button v-if="canExecuteRefundPermission && canExecuteRefund(row)" size="small" type="warning" plain @click="executeRefund(row)">执行退款</el-button>
                  <span v-else class="muted-action">-</span>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="admin-pagination">
            <el-pagination background layout="sizes, prev, pager, next, total" :current-page="refundPager.page" :page-size="refundPager.size" :page-sizes="ADMIN_PAGE_SIZES" :total="refundPager.total" @current-change="handleRefundPageChange" @size-change="handleRefundSizeChange" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="detailVisible" title="支付单详情" width="820px" append-to-body destroy-on-close align-center class="admin-beauty-dialog pay-detail-dialog">
      <div v-if="detail" class="pay-detail-card admin-dialog-detail-panel">
        <div class="pay-detail-hero">
          <div>
            <div class="pay-detail-eyebrow">支付单</div>
            <div class="pay-detail-title">{{ detail.payOrderNo }}</div>
            <div class="pay-detail-subtitle">关联订单：{{ detail.orderNo }}</div>
          </div>
          <div class="pay-detail-amount">
            <span>实付金额</span>
            <strong>¥{{ formatCentToYuan(detail.payAmount) }}</strong>
          </div>
        </div>

        <div class="pay-detail-section">
          <div class="pay-detail-section__title">处理结论</div>
          <div class="pay-detail-summary">
            <div class="pay-detail-summary__item pay-detail-summary__item--status" :class="`pay-detail-summary__item--${String(detail.status || '').toLowerCase()}`">
              <span>支付状态</span>
              <div class="pay-detail-status-pill">{{ payStatusMeta(detail.status).label }}</div>
            </div>
            <div class="pay-detail-summary__item">
              <span>是否可关闭</span>
              <strong>{{ detail.status === 'SUCCESS' || detail.status === 'REFUNDED' || detail.status === 'CLOSED' ? '否' : '是' }}</strong>
            </div>
            <div class="pay-detail-summary__item">
              <span>建议动作</span>
              <strong>{{ payDetailSuggestion(detail.status) }}</strong>
            </div>
          </div>
        </div>

        <div class="pay-detail-section">
          <div class="pay-detail-section__title">业务信息</div>
          <div class="pay-detail-grid">
            <div class="pay-detail-field"><span>订单号</span><strong>{{ detail.orderNo }}</strong></div>
            <div class="pay-detail-field"><span>用户ID</span><strong>{{ detail.userId || '-' }}</strong></div>
            <div class="pay-detail-field"><span>支付渠道</span><strong>{{ detail.payChannel || '-' }}</strong></div>
            <div class="pay-detail-field"><span>渠道交易号</span><strong>{{ detail.transactionNo || '-' }}</strong></div>
          </div>
        </div>

        <div class="pay-detail-section pay-detail-section--muted">
          <div class="pay-detail-section__title">排查辅助</div>
          <div class="pay-detail-grid pay-detail-grid--single">
            <div class="pay-detail-field"><span>幂等键</span><strong>{{ detail.idempotentKey || '-' }}</strong></div>
          </div>
          <el-collapse v-if="detail.callbackPayload" class="pay-detail-collapse">
            <el-collapse-item title="查看原始回调负载（仅排障使用）" name="callbackPayload">
              <el-input :model-value="detail.callbackPayload" type="textarea" :rows="6" readonly />
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>
    </el-dialog>

    <el-dialog v-model="callbackVisible" title="支付/退款回调记录" width="1180px" append-to-body destroy-on-close align-center class="admin-beauty-dialog pay-callback-dialog">
      <div class="admin-filter-bar pay-dialog-filter">
        <el-input v-model="callbackQuery.keyword" placeholder="订单号 / 支付单号 / 退款单号" clearable style="width: 300px" @keyup.enter="loadCallbacks" />
        <el-select v-model="callbackQuery.processStatus" clearable placeholder="处理状态" style="width: 190px">
          <el-option label="已接收" value="RECEIVED" />
          <el-option label="验签失败" value="VERIFY_FAILED" />
          <el-option label="业务不匹配" value="BUSINESS_MISMATCH" />
          <el-option label="金额不一致" value="AMOUNT_MISMATCH" />
          <el-option label="重复忽略" value="IGNORED_DUPLICATE" />
          <el-option label="非成功忽略" value="IGNORED_NON_SUCCESS" />
          <el-option label="处理成功" value="PROCESSED" />
          <el-option label="处理失败" value="PROCESS_FAILED" />
        </el-select>
        <el-button type="primary" @click="loadCallbacks">查询</el-button>
      </div>
      <el-table v-loading="callbackLoading" :data="callbackRows" class="admin-table admin-table--safe" empty-text="暂无回调记录">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="callbackType" label="类型" width="90" />
        <el-table-column prop="orderNo" label="订单号" min-width="170">
          <template #default="{ row }">
            <el-tooltip :content="textOrDash(row.orderNo)" placement="top" effect="light" popper-class="admin-hover-tooltip">
              <span class="hover-ellipsis">{{ textOrDash(row.orderNo) }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="payOrderNo" label="支付单号" min-width="170">
          <template #default="{ row }">
            <el-tooltip :content="textOrDash(row.payOrderNo)" placement="top" effect="light" popper-class="admin-hover-tooltip">
              <span class="hover-ellipsis">{{ textOrDash(row.payOrderNo) }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="refundNo" label="退款单号" min-width="170">
          <template #default="{ row }">
            <el-tooltip :content="textOrDash(row.refundNo)" placement="top" effect="light" popper-class="admin-hover-tooltip">
              <span class="hover-ellipsis">{{ textOrDash(row.refundNo) }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="amountCent" label="金额(元)" width="110">
          <template #default="{ row }">{{ formatCentToYuan(row.amountCent) }}</template>
        </el-table-column>
        <el-table-column prop="tradeStatus" label="渠道状态" min-width="130">
          <template #default="{ row }">
            <el-tooltip :content="textOrDash(row.tradeStatus)" placement="top" effect="light" popper-class="admin-hover-tooltip">
              <span class="hover-ellipsis">{{ textOrDash(row.tradeStatus) }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="verified" label="验签" width="90">
          <template #default="{ row }"><el-tag :type="row.verified ? 'success' : 'danger'">{{ row.verified ? '通过' : '失败' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="processStatus" label="处理状态" width="140">
          <template #default="{ row }"><el-tag :type="callbackStatusMeta(row.processStatus).type">{{ callbackStatusMeta(row.processStatus).label }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="failReason" label="失败原因" min-width="180">
          <template #default="{ row }">
            <el-tooltip :content="textOrDash(row.failReason)" placement="top" effect="light" popper-class="admin-hover-tooltip">
              <span class="hover-ellipsis">{{ textOrDash(row.failReason) }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="接收时间" min-width="170">
          <template #default="{ row }">
            <el-tooltip :content="textOrDash(row.createdAt)" placement="top" effect="light" popper-class="admin-hover-tooltip">
              <span class="hover-ellipsis">{{ textOrDash(row.createdAt) }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
      <div class="admin-pagination">
        <el-pagination background layout="sizes, prev, pager, next, total" :current-page="callbackPager.page" :page-size="callbackPager.size" :page-sizes="ADMIN_PAGE_SIZES" :total="callbackPager.total" @current-change="handleCallbackPageChange" @size-change="handleCallbackSizeChange" />
      </div>
    </el-dialog>

    <el-dialog v-model="closeVisible" title="关闭支付单" width="460px" append-to-body destroy-on-close align-center class="admin-beauty-dialog pay-close-dialog">
      <el-form :model="closeForm" label-width="100px" class="admin-dialog-form">
        <el-form-item label="关闭原因"><el-input v-model="closeForm.reason" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="closeVisible = false">取消</el-button><el-button type="primary" @click="submitClose">提交</el-button></template>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import AdminLayout from '../components/AdminLayout.vue';
import AdminOverflowText from '../components/AdminOverflowText.vue';
import { closeAdminPayOrder, fetchAdminGlobalRefunds, fetchAdminPayCallbackRecords, fetchAdminPayDetail, fetchAdminPayReconciliationRecords, fetchAdminPays, repairAdminPaidOrder, syncAdminPayOrderStatus, syncAdminRefundStatus } from '../api';
import { useAdminStore } from '../stores/admin';
import { confirmAction } from '../utils/action';
import { exportRowsToCsv } from '../utils/export';
import { t } from '../utils/i18n';
import { ADMIN_PAGE_SIZE, ADMIN_PAGE_SIZES } from '../utils/pagination';
import { getStatusTagMeta } from '../utils/status';

const route = useRoute();
const router = useRouter();
const adminStore = useAdminStore();
const canSyncPayment = computed(() => adminStore.hasPermission('payment:sync'));
const canRepairPayment = computed(() => adminStore.hasPermission('payment:repair'));
const canViewCallbacks = computed(() => adminStore.hasPermission('payment:callback:view'));
const canExecuteRefundPermission = computed(() => adminStore.hasPermission('refund:execute'));
const canClosePayment = computed(() => adminStore.hasPermission('payment:close'));
const payStatusMeta = (status) => getStatusTagMeta('pay', status);
const refundStatusMeta = (status) => getStatusTagMeta('refund', status);
const canExecuteRefund = (row) => ['REFUND_PENDING', 'REFUND_FAILED', 'REFUNDING'].includes(row?.refundStatus);
const callbackStatusMeta = (status) => getStatusTagMeta('callbackProcess', status);
const pendingActionOfDiff = (diffType) => {
  const actionMap = {
    LOCAL_PAYING_CHANNEL_SUCCESS: 'PAY_SYNC_PENDING',
    PAY_SUCCESS_ORDER_NOT_PAID: 'ORDER_REPAIR_PENDING',
    UNPAID_ORDER_NEED_CLOSE_RELEASE: 'CLOSE_RELEASE_PENDING',
  };
  return actionMap[diffType] || '';
};
const pendingActionMeta = (action) => {
  const metaMap = {
    PAY_SYNC_PENDING: { label: '需要同步状态', type: 'warning' },
    ORDER_REPAIR_PENDING: { label: '需要支付补偿', type: 'success' },
    CLOSE_RELEASE_PENDING: { label: '需要关闭并释放库存', type: 'danger' },
  };
  return metaMap[action] || { label: '待处理', type: 'info' };
};
const formatCentToYuan = (cent) => {
  const amount = Number(cent);
  if (!Number.isFinite(amount)) return '-';
  return (amount / 100).toFixed(2);
};
const textOrDash = (value) => {
  if (value === null || value === undefined) return '-';
  const text = String(value).trim();
  return text || '-';
};
const payDetailSuggestion = (status) => {
  const suggestionMap = {
    PENDING: '等待用户支付，必要时可关闭',
    PAYING: '优先同步渠道状态',
    SUCCESS: '支付已完成，无需处理',
    CLOSED: '支付已关闭，无需处理',
    REFUND_PENDING: '查看退款单进度',
    PARTIALLY_REFUNDED: '核对退款明细',
    REFUNDED: '退款已完成，无需处理',
    REFUND_FAILED: '同步退款或人工介入',
  };
  return suggestionMap[status] || '按业务规则核验';
};
const activeTab = ref(String(route.query.tab || 'pays'));
const rows = ref([]);
const pendingActionByOrderNo = ref({});
const refundFlowPayStatuses = ['REFUND_PENDING', 'REFUNDING', 'REFUNDED', 'PARTIALLY_REFUNDED', 'REFUND_FAILED'];
const refundedPayStatuses = ['REFUNDED', 'PARTIALLY_REFUNDED'];
const canSyncPayStatus = (row) => !['CLOSED', 'REFUNDED', 'PARTIALLY_REFUNDED'].includes(row?.status);
const canRepairPaidOrder = (row) => row?.status === 'SUCCESS';
const canViewRefunds = (row) => refundedPayStatuses.includes(row?.status);
const canClosePayOrder = (row) => !['SUCCESS', 'CLOSED', 'REFUNDED', 'PARTIALLY_REFUNDED'].includes(row?.status);
const shouldShowPendingAction = (row, action) => {
  if (!action) return false;
  if (refundFlowPayStatuses.includes(row?.status)) return false;
  if (action === 'ORDER_REPAIR_PENDING' && !canRepairPaidOrder(row)) return false;
  return true;
};
const rowPendingAction = (row) => {
  const action = pendingActionByOrderNo.value[row?.orderNo] || '';
  return shouldShowPendingAction(row, action) ? action : '';
};
const loading = ref(false);
const detail = ref(null);
const detailVisible = ref(false);
const closeVisible = ref(false);
const refundLoading = ref(false);
const refundRows = ref([]);
const callbackVisible = ref(false);
const callbackLoading = ref(false);
const callbackRows = ref([]);
const currentOrderNo = ref('');
const PAID_LIFECYCLE_PAY_STATUSES = ['SUCCESS', 'REFUND_PENDING', 'REFUNDING', 'PARTIALLY_REFUNDED', 'REFUNDED', 'REFUND_FAILED'];
const query = reactive({ keyword: String(route.query.keyword || ''), status: String(route.query.status || ''), paidLifecycle: route.query.paidLifecycle === 'true', sortBy: String(route.query.sortBy || 'id'), sortOrder: String(route.query.sortOrder || 'desc') });
const pager = reactive({ page: Number(route.query.page || 1), size: Number(route.query.size || ADMIN_PAGE_SIZE), total: 0 });
const refundQuery = reactive({ keyword: String(route.query.refundKeyword || route.query.keyword || route.query.orderNo || ''), status: String(route.query.refundStatus || '') });
const refundPager = reactive({ page: Number(route.query.refundPage || 1), size: Number(route.query.refundSize || ADMIN_PAGE_SIZE), total: 0 });
const callbackQuery = reactive({ keyword: String(route.query.callbackKeyword || ''), processStatus: String(route.query.callbackStatus || '') });
const callbackPager = reactive({ page: 1, size: ADMIN_PAGE_SIZE, total: 0 });
const closeForm = reactive({ reason: '' });

const syncRoute = async () => {
  await router.replace({
    path: '/pays',
    query: {
      ...(activeTab.value !== 'pays' ? { tab: activeTab.value } : {}),
      ...(query.keyword ? { keyword: query.keyword } : {}),
      ...(query.status ? { status: query.status } : {}),
      ...(query.paidLifecycle ? { paidLifecycle: 'true' } : {}),
      ...(query.sortBy ? { sortBy: query.sortBy } : {}),
      ...(query.sortOrder ? { sortOrder: query.sortOrder } : {}),
      ...(pager.page > 1 ? { page: String(pager.page) } : {}),
      ...(pager.size !== ADMIN_PAGE_SIZE ? { size: String(pager.size) } : {}),
      ...(refundQuery.keyword ? { refundKeyword: refundQuery.keyword } : {}),
      ...(refundQuery.status ? { refundStatus: refundQuery.status } : {}),
      ...(refundPager.page > 1 ? { refundPage: String(refundPager.page) } : {}),
      ...(refundPager.size !== ADMIN_PAGE_SIZE ? { refundSize: String(refundPager.size) } : {}),
    },
  });
};

const loadPendingActions = async (payRows) => {
  const orderNos = new Set((payRows || []).map((row) => row.orderNo).filter(Boolean));
  pendingActionByOrderNo.value = {};
  if (!orderNos.size) return;
  try {
    const { data } = await fetchAdminPayReconciliationRecords({ bizType: 'PAY', status: 'ABNORMAL', repairStatus: 'PENDING', page: 1, size: 500 });
    const nextMap = {};
    (data.data?.records || []).forEach((record) => {
      if (!orderNos.has(record.orderNo)) return;
      const action = pendingActionOfDiff(record.diffType);
      if (action && !nextMap[record.orderNo]) nextMap[record.orderNo] = action;
    });
    pendingActionByOrderNo.value = nextMap;
  } catch (error) {
    pendingActionByOrderNo.value = {};
  }
};

const loadData = async () => {
  loading.value = true;
  try {
    if (query.paidLifecycle) {
      const results = await Promise.all(PAID_LIFECYCLE_PAY_STATUSES.map((status) => fetchAdminPays({ ...query, paidLifecycle: undefined, status, page: 1, size: 500 })));
      const mergedRows = Array.from(new Map(results.flatMap((result) => result.data.data?.records || []).map((row) => [row.payOrderNo || row.id || row.orderNo, row])).values());
      rows.value = mergedRows.slice((pager.page - 1) * pager.size, pager.page * pager.size);
      pager.total = mergedRows.length;
      await loadPendingActions(rows.value);
      return;
    }
    const { data } = await fetchAdminPays({ ...query, paidLifecycle: undefined, page: pager.page, size: pager.size });
    rows.value = data.data?.records || [];
    pager.total = data.data?.total || 0;
    await loadPendingActions(rows.value);
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || t('payManage.loadFailed'));
  } finally {
    loading.value = false;
  }
};
const exportPays = () => {
  exportRowsToCsv(t('payManage.exportName'), rows.value, [
    { label: 'ID', value: 'id' },
    { label: '支付单号', value: 'payOrderNo' },
    { label: '订单号', value: 'orderNo' },
    { label: '状态', value: 'status' },
    { label: '支付金额(元)', value: (row) => formatCentToYuan(row.payAmount) },
    { label: '支付渠道', value: 'payChannel' },
  ]);
};
const handleTabChange = async () => {
  await syncRoute();
  if (activeTab.value === 'refunds') {
    await loadRefunds();
  } else {
    await loadData();
  }
};
const handleSearch = async () => { pager.page = 1; await syncRoute(); await loadData(); };
const handleReset = async () => { query.keyword = ''; query.status = ''; query.paidLifecycle = false; query.sortBy = 'id'; query.sortOrder = 'desc'; pager.page = 1; pager.size = ADMIN_PAGE_SIZE; await syncRoute(); await loadData(); };
const handlePageChange = async (page) => { pager.page = page; await syncRoute(); await loadData(); };
const handleSizeChange = async (size) => { pager.size = size; pager.page = 1; await syncRoute(); await loadData(); };
const handleSortChange = async ({ prop, order }) => { query.sortBy = prop || 'id'; query.sortOrder = order === 'descending' ? 'desc' : 'asc'; pager.page = 1; await syncRoute(); await loadData(); };
const openDetail = async (orderNo) => { const { data } = await fetchAdminPayDetail(orderNo); detail.value = data.data; detailVisible.value = true; };
const openClose = (row) => { currentOrderNo.value = row.orderNo; closeForm.reason = ''; closeVisible.value = true; };

const syncStatus = async (row) => {
  try {
    await confirmAction(`确认查询并同步订单 ${row.orderNo} 的渠道支付状态吗？\n\n该操作仅在“渠道已支付但本地支付单未成功”时会更新支付单；如果支付单已成功或渠道仍未支付，页面状态可能不会变化。`);
    const { data } = await syncAdminPayOrderStatus(row.orderNo);
    const latestPay = data.data || {};
    await loadData();
    if (detailVisible.value && detail.value?.orderNo === row.orderNo) {
      await openDetail(row.orderNo);
    }
    if (row.status !== latestPay.status) {
      ElMessage.success(`渠道同步完成：支付单状态 ${row.status || '-'} → ${latestPay.status || '-'}`);
      return;
    }
    ElMessage.info('已完成渠道核验，当前无需变更支付单状态');
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || '同步支付状态失败');
  }
};

const repairPaid = async (row) => {
  try {
    await confirmAction(`确认补偿订单 ${row.orderNo} 的支付状态吗？该操作会将已成功支付但未更新的订单补偿为已支付。`);
    await repairAdminPaidOrder(row.orderNo);
    ElMessage.success('订单支付状态补偿成功');
    await loadData();
    if (detailVisible.value && detail.value?.orderNo === row.orderNo) {
      await openDetail(row.orderNo);
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || '补偿订单支付状态失败');
  }
};

const loadRefunds = async () => {
  refundLoading.value = true;
  try {
    const { data } = await fetchAdminGlobalRefunds({ keyword: refundQuery.keyword, status: refundQuery.status, page: refundPager.page, size: refundPager.size });
    refundRows.value = data.data?.records || [];
    refundPager.total = data.data?.total || 0;
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '加载退款单失败');
  } finally {
    refundLoading.value = false;
  }
};

const openRefunds = async (orderNo = '') => {
  activeTab.value = 'refunds';
  refundQuery.keyword = orderNo || refundQuery.keyword || query.keyword || '';
  refundPager.page = 1;
  await syncRoute();
  await loadRefunds();
};

const handleRefundSearch = async () => { refundPager.page = 1; await syncRoute(); await loadRefunds(); };
const handleRefundReset = async () => { refundQuery.keyword = ''; refundQuery.status = ''; refundPager.page = 1; refundPager.size = ADMIN_PAGE_SIZE; await syncRoute(); await loadRefunds(); };
const handleRefundPageChange = async (page) => { refundPager.page = page; await syncRoute(); await loadRefunds(); };
const handleRefundSizeChange = async (size) => { refundPager.size = size; refundPager.page = 1; await syncRoute(); await loadRefunds(); };

const executeRefund = async (row) => {
  if (!canExecuteRefund(row)) {
    ElMessage.warning('当前退款单状态不可重复执行退款');
    return;
  }
  try {
    await confirmAction(`确认执行退款单 ${row.refundNo} 的退款操作吗？成功后会按退款明细补偿库存与订单状态。`);
    await syncAdminRefundStatus(row.orderNo, row.refundNo, row.refundAmountCent);
    ElMessage.success('退款执行成功');
    await loadRefunds();
    if (rows.value.length) {
      await loadData();
    }
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || '退款执行失败');
  }
};

const loadCallbacks = async () => {
  callbackLoading.value = true;
  try {
    const { data } = await fetchAdminPayCallbackRecords({ ...callbackQuery, page: callbackPager.page, size: callbackPager.size });
    callbackRows.value = data.data?.records || [];
    callbackPager.total = data.data?.total || 0;
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '加载回调记录失败');
  } finally {
    callbackLoading.value = false;
  }
};

const openCallbacks = async (keyword = '', options = {}) => {
  const safeKeyword = typeof keyword === 'string' ? keyword : '';
  const keepCurrent = options.keepCurrent === true;
  callbackQuery.keyword = keepCurrent ? (safeKeyword || callbackQuery.keyword || '') : safeKeyword;
  callbackPager.page = 1;
  callbackVisible.value = true;
  await loadCallbacks();
};

const openCallbacksFromRoute = async () => {
  const keyword = String(route.query.callbackKeyword || route.query.keyword || route.query.refundKeyword || '');
  callbackQuery.keyword = keyword;
  callbackQuery.processStatus = String(route.query.callbackStatus || '');
  await openCallbacks(keyword, { keepCurrent: true });
};

const handleCallbackPageChange = async (page) => { callbackPager.page = page; await loadCallbacks(); };
const handleCallbackSizeChange = async (size) => { callbackPager.size = size; callbackPager.page = 1; await loadCallbacks(); };

const submitClose = async () => {
  try {
    await confirmAction(t('payManage.confirmClose', { orderNo: currentOrderNo.value }));
    await closeAdminPayOrder(currentOrderNo.value, { ...closeForm });
    ElMessage.success(t('payManage.closeSuccess'));
    closeVisible.value = false;
    await loadData();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || t('payManage.closeFailed'));
  }
};
const handleLogout = async () => { await adminStore.logout(); router.push('/login'); };

onMounted(async () => {
  if (activeTab.value === 'refunds') {
    await loadRefunds();
  } else {
    await loadData();
  }
  if (route.query.openCallbacks === '1' || route.query.callbackKeyword) {
    await openCallbacksFromRoute();
  }
});
</script>

<style scoped>
.hover-ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  color: #334155;
  line-height: 1.5;
  text-overflow: ellipsis;
  vertical-align: middle;
  white-space: nowrap;
  cursor: default;
}

:global(.admin-hover-tooltip) {
  max-width: 520px;
  padding: 10px 14px;
  color: #0f172a !important;
  font-size: 13px;
  line-height: 1.55;
  word-break: break-all;
  border: 1px solid rgba(148, 163, 184, 0.28) !important;
  border-radius: 8px !important;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.16) !important;
}

.pay-actions {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  align-items: center;
  justify-content: center;
  min-width: max-content;
  white-space: nowrap;
}

.pay-action-btn {
  flex: 0 0 auto;
  min-width: 76px;
  margin: 0;
  border: none;
  border-radius: 999px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}

.pay-action-btn--detail { color: #2563eb; background: linear-gradient(135deg, #eff6ff, #dbeafe); }
.pay-action-btn--warn { color: #92400e; background: linear-gradient(135deg, #fffbeb, #fef3c7); }
.pay-action-btn--success { color: #047857; background: linear-gradient(135deg, #ecfdf5, #d1fae5); }
.pay-action-btn--refund { color: #6d28d9; background: linear-gradient(135deg, #f5f3ff, #ede9fe); }
.pay-action-btn--danger { color: #be123c; background: linear-gradient(135deg, #fff1f2, #ffe4e6); }

.pay-no-cell {
  display: inline-flex;
  gap: 8px;
  align-items: center;
}

.pay-pending-action-badge {
  border: 0;
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.pay-dialog-filter {
  margin-bottom: 14px;
}

.pay-detail-card {
  padding: 18px;
}

.pay-detail-hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 20px;
  border-radius: 20px;
  background: linear-gradient(135deg, #eff6ff 0%, #f5f3ff 54%, #fff7ed 100%);
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.08);
}

.pay-detail-eyebrow {
  margin-bottom: 6px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.pay-detail-title {
  color: #0f172a;
  font-size: 20px;
  font-weight: 800;
  word-break: break-all;
}

.pay-detail-subtitle {
  margin-top: 8px;
  color: #64748b;
  font-size: 13px;
  word-break: break-all;
}

.pay-detail-amount {
  min-width: 150px;
  text-align: right;
}

.pay-detail-amount span,
.pay-detail-summary__item span,
.pay-detail-field span {
  display: block;
  margin-bottom: 7px;
  color: #64748b;
  font-size: 12px;
}

.pay-detail-amount strong {
  color: #dc2626;
  font-size: 26px;
  font-weight: 900;
}

.pay-detail-section {
  margin-top: 16px;
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
}

.pay-detail-section--muted {
  background: #f8fafc;
}

.pay-detail-section__title {
  margin-bottom: 12px;
  color: #0f172a;
  font-size: 14px;
  font-weight: 800;
}

.pay-detail-summary,
.pay-detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.pay-detail-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.pay-detail-grid--single {
  grid-template-columns: 1fr;
}

.pay-detail-summary__item,
.pay-detail-field {
  min-height: 72px;
  padding: 14px;
  border-radius: 14px;
  background: #f8fafc;
}

.pay-detail-summary__item--status {
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #f8fafc, #f1f5f9);
}

.pay-detail-summary__item--status::after {
  position: absolute;
  top: -24px;
  right: -24px;
  width: 72px;
  height: 72px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.12);
  content: '';
}

.pay-detail-summary__item--success {
  background: linear-gradient(135deg, #ecfdf5, #d1fae5);
  box-shadow: inset 0 0 0 1px rgba(5, 150, 105, 0.18);
}

.pay-detail-summary__item--success::after {
  background: rgba(16, 185, 129, 0.16);
}

.pay-detail-status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  color: #475569;
  font-size: 14px;
  font-weight: 800;
  background: rgba(255, 255, 255, 0.82);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06);
}

.pay-detail-summary__item--success .pay-detail-status-pill {
  color: #047857;
  background: #ffffff;
}

.pay-detail-summary__item strong,
.pay-detail-field strong {
  color: #0f172a;
  font-size: 14px;
  font-weight: 700;
  word-break: break-all;
}

.pay-detail-collapse {
  margin-top: 12px;
  border: none;
}

@media (max-width: 720px) {
  .pay-detail-hero,
  .pay-detail-summary,
  .pay-detail-grid {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .pay-detail-amount {
    text-align: left;
  }
}
</style>
