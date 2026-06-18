<template>
  <AdminLayout title="对账运营" @refresh="refreshCurrent" @logout="handleLogout">
    <el-tabs v-model="mainTab" class="reconcile-main-tabs" @tab-change="handleMainTabChange">
      <el-tab-pane v-if="canViewOnlineReconciliation" label="线上支付/退款对账" name="online">
        <el-card class="admin-page-card online-reconcile-page">
      <div class="reconcile-hero">
        <div>
          <h2>线上人工对账工作台</h2>
          <p>业务详情页仅承载账单快照查看、人工登记和轻量处置；Mock 造数已隔离为测试工具箱入口。</p>
        </div>
        <div class="reconcile-actions">
          <el-button v-if="canImportBill" type="warning" plain @click="mockToolboxVisible = true">测试数据工具箱</el-button>
          <el-button v-if="canCreateTask" type="primary" @click="openCreateTask">创建对账任务</el-button>
        </div>
      </div>

      <div class="admin-filter-bar">
        <el-select v-model="taskQuery.status" clearable placeholder="任务状态" style="width: 170px">
          <el-option label="已创建" value="CREATED" />
          <el-option label="本地账单就绪" value="LOCAL_BILL_READY" />
          <el-option label="渠道账单就绪" value="CHANNEL_BILL_READY" />
          <el-option label="已勾兑" value="MATCHED" />
          <el-option label="已归档" value="COMPLETED" />
        </el-select>
        <el-select v-model="taskQuery.channel" clearable placeholder="渠道" style="width: 150px">
          <el-option label="MOCK模拟渠道" value="MOCK" />
          <el-option label="支付宝" value="ALIPAY" />
        </el-select>
        <el-button type="primary" @click="loadTasks">查询</el-button>
        <el-button @click="resetTasks">重置</el-button>
      </div>

      <el-table v-loading="taskLoading" :data="tasks" class="admin-table admin-table--with-gap" empty-text="暂无对账任务" highlight-current-row @current-change="selectTask">
        <el-table-column label="任务号" min-width="190"><template #default="{ row }"><AdminOverflowText :value="row.taskNo" /></template></el-table-column>
        <el-table-column label="对账日期" width="120"><template #default="{ row }"><AdminOverflowText :value="row.reconcileDate" /></template></el-table-column>
        <el-table-column prop="channel" label="渠道" width="120"><template #default="{ row }"><el-tag>{{ row.channel }}</el-tag></template></el-table-column>
        <el-table-column prop="status" label="任务状态" width="130"><template #default="{ row }"><el-tag :type="taskStatusMeta(row.status).type">{{ taskStatusMeta(row.status).label }}</el-tag></template></el-table-column>
        <el-table-column label="本地账单" width="120"><template #default="{ row }"><el-tag :type="billStatusType(row.localBillStatus)">{{ row.localTotalCount || 0 }} 笔</el-tag></template></el-table-column>
        <el-table-column label="渠道账单" width="120"><template #default="{ row }"><el-tag :type="billStatusType(row.channelBillStatus)">{{ row.channelTotalCount || 0 }} 笔</el-tag></template></el-table-column>
        <el-table-column label="差异" width="120"><template #default="{ row }">{{ row.diffCount || 0 }} / 待 {{ row.pendingCount || 0 }}</template></el-table-column>
        <el-table-column label="金额(元)" min-width="160"><template #default="{ row }">本地 {{ formatCent(row.localTotalAmountCent) }} / 渠道 {{ formatCent(row.channelTotalAmountCent) }}</template></el-table-column>
        <el-table-column label="操作" width="430" fixed="right">
          <template #default="{ row }">
            <div class="reconcile-actions">
              <el-button size="small" @click="selectTask(row)">查看</el-button>
              <template v-if="!isTaskArchived(row)">
                <el-button v-if="canRunTask" size="small" type="primary" plain @click="generateLocal(row)">生成本地</el-button>
                <el-button v-if="canImportBill && isMockTask(row)" size="small" type="warning" plain @click="generateMock(row)">生成Mock账单</el-button>
                <el-upload v-else-if="canImportBill && isAlipayTask(row)" :show-file-list="false" accept=".csv,text/csv" :before-upload="(file) => uploadAlipayCsv(row, file)"><el-button size="small" type="warning" plain>上传支付宝CSV</el-button></el-upload>
                <el-button v-if="canRunTask" size="small" type="success" @click="matchTask(row)">自动勾兑</el-button>
                <el-button v-if="canCompleteTask(row)" size="small" type="info" plain @click="completeTask(row)">完成归档</el-button>
              </template>
              <template v-else-if="isTaskArchived(row)">
                <el-tag type="success" effect="plain">已归档，只读</el-tag>
                <el-button size="small" type="primary" plain @click="exportArchiveTaskPackage(row)">导出归档包</el-button>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="admin-pagination"><el-pagination background layout="sizes, prev, pager, next, total" :current-page="taskPager.page" :page-size="taskPager.size" :page-sizes="ADMIN_PAGE_SIZES" :total="taskPager.total" @current-change="handleTaskPageChange" @size-change="handleTaskSizeChange" /></div>

      <template v-if="currentTask">
        <el-divider content-position="left">任务详情：{{ currentTask.taskNo }}</el-divider>
        <div class="task-summary-grid">
          <div><span>本地账单</span><strong>{{ currentTask.localTotalCount || 0 }}</strong><em>{{ formatCent(currentTask.localTotalAmountCent) }} 元</em></div>
          <div><span>渠道账单</span><strong>{{ currentTask.channelTotalCount || 0 }}</strong><em>{{ formatCent(currentTask.channelTotalAmountCent) }} 元</em></div>
          <div><span>已匹配</span><strong>{{ currentTask.matchedCount || 0 }}</strong><em>自动勾兑成功</em></div>
          <div><span>差异数</span><strong>{{ currentTask.diffCount || 0 }}</strong><em>待处理 {{ currentTask.pendingCount || 0 }}</em></div>
        </div>

        <el-tabs v-model="detailTab" @tab-change="loadDetailTab">
          <el-tab-pane label="差异明细" name="diffs">
            <div class="admin-filter-bar diff-filter-bar"><el-select v-model="diffQuery.processStatus" clearable placeholder="处理状态" style="width: 160px"><el-option label="待处理" value="PENDING" /><el-option label="已完成" value="DONE" /><el-option label="挂账" value="HANGING" /><el-option label="已忽略" value="IGNORED" /></el-select><el-button type="primary" @click="loadDiffs">查询</el-button><el-button v-if="canHandleDiff" :disabled="!selectedDiffRows.length" @click="batchMarkDiffsDone">批量标记已处理 {{ selectedDiffRows.length ? `(${selectedDiffRows.length})` : '' }}</el-button><el-button v-if="canHandleDiff" type="warning" plain :disabled="!selectedDiffRows.length" @click="batchHangDiffs">批量挂账</el-button><el-button plain @click="exportDiffRows">导出差异清单</el-button></div>
            <el-table v-loading="diffLoading" :data="diffRows" class="admin-table diff-table reconcile-diff-table reconcile-overflow-table" empty-text="暂无差异明细" row-key="id" highlight-current-row @selection-change="handleDiffSelectionChange">
              <el-table-column type="selection" width="46" :selectable="canSelectDiff" />
              <el-table-column prop="bizType" label="类型" width="80"><template #default="{ row }"><el-tag effect="plain" :class="row.bizType === 'REFUND' ? 'reconcile-tag--refund' : 'reconcile-tag--pay'">{{ row.bizType === 'REFUND' ? '退款' : '支付' }}</el-tag></template></el-table-column>
              <el-table-column label="差异类型" min-width="190"><template #default="{ row }"><el-tag effect="plain" :class="diffTypeClass(row.diffType)" :type="diffTypeTagType(row.diffType)">{{ diffTypeLabel(row.diffType) }}</el-tag></template></el-table-column>
              <el-table-column prop="diffLevel" label="等级" width="90"><template #default="{ row }"><el-tag effect="light" :type="diffLevelType(row.diffLevel)" :class="diffLevelClass(row.diffLevel)">{{ diffLevelLabel(row.diffLevel) }}</el-tag></template></el-table-column>
              <el-table-column label="订单号" min-width="160"><template #default="{ row }"><AdminOverflowText :value="row.orderNo" /></template></el-table-column>
              <el-table-column label="支付单号" min-width="160"><template #default="{ row }"><AdminOverflowText :value="row.payOrderNo" /></template></el-table-column>
              <el-table-column label="退款单号" min-width="150"><template #default="{ row }"><AdminOverflowText :value="row.refundNo" /></template></el-table-column>
              <el-table-column prop="localStatus" label="本地状态" min-width="120"><template #default="{ row }"><el-tag effect="light" :type="reconcileStatusTagType(row.localStatus)" :class="reconcileStatusClass(row.localStatus)">{{ reconcileStatusLabel(row.localStatus) }}</el-tag></template></el-table-column>
              <el-table-column prop="channelStatus" label="渠道状态" min-width="130"><template #default="{ row }"><el-tag effect="light" :type="reconcileStatusTagType(row.channelStatus)" :class="reconcileStatusClass(row.channelStatus)">{{ reconcileStatusLabel(row.channelStatus) }}</el-tag></template></el-table-column>
              <el-table-column label="金额(元)" min-width="150"><template #default="{ row }"><span class="reconcile-money-compare">{{ formatCent(row.localAmountCent) }} / {{ formatCent(row.channelAmountCent) }}</span></template></el-table-column>
              <el-table-column label="建议动作" min-width="150"><template #default="{ row }"><el-tag effect="plain" :type="actionMeta(row.suggestedAction).type" :class="actionTagClass(row.suggestedAction)">{{ suggestedActionLabel(row.suggestedAction) }}</el-tag></template></el-table-column>
              <el-table-column label="差异备注" min-width="240"><template #default="{ row }"><AdminOverflowText :value="diffRemarkText(row)" text-class="reconcile-remark-text" /></template></el-table-column>
              <el-table-column prop="processStatus" label="处理状态" width="110"><template #default="{ row }"><el-tag effect="light" :type="processStatusType(row.processStatus)" :class="processStatusClass(row.processStatus)">{{ processStatusLabel(row.processStatus) }}</el-tag></template></el-table-column>
              <el-table-column label="操作" width="150" fixed="right"><template #default="{ row }"><el-button size="small" type="primary" plain @click="openDiffDetail(row)">查看/登记</el-button></template></el-table-column>
            </el-table>
            <div class="admin-pagination"><el-pagination background layout="sizes, prev, pager, next, total" :current-page="diffPager.page" :page-size="diffPager.size" :page-sizes="ADMIN_PAGE_SIZES" :total="diffPager.total" @current-change="handleDiffPageChange" @size-change="handleDiffSizeChange" /></div>
          </el-tab-pane>

          <el-tab-pane label="本地账单" name="locals">
            <el-table v-loading="localLoading" :data="localRows" class="admin-table" empty-text="暂无本地账单">
              <el-table-column prop="bizType" label="类型" width="80"><template #default="{ row }"><el-tag effect="plain" :class="row.bizType === 'REFUND' ? 'reconcile-tag--refund' : 'reconcile-tag--pay'">{{ row.bizType === 'REFUND' ? '退款' : '支付' }}</el-tag></template></el-table-column>
              <el-table-column label="订单号" min-width="160"><template #default="{ row }"><AdminOverflowText :value="row.orderNo" text-class="reconcile-code-text" /></template></el-table-column>
              <el-table-column label="支付单号" min-width="160"><template #default="{ row }"><AdminOverflowText :value="row.payOrderNo" text-class="reconcile-flow-text" /></template></el-table-column>
              <el-table-column label="退款单号" min-width="150"><template #default="{ row }"><AdminOverflowText :value="row.refundNo" text-class="reconcile-flow-text" /></template></el-table-column>
              <el-table-column prop="localStatus" label="本地状态" min-width="120"><template #default="{ row }"><el-tag effect="light" :type="reconcileStatusTagType(row.localStatus)" :class="reconcileStatusClass(row.localStatus)">{{ reconcileStatusLabel(row.localStatus) }}</el-tag></template></el-table-column>
              <el-table-column prop="orderStatus" label="订单状态" min-width="120"><template #default="{ row }"><el-tag effect="light" :type="reconcileStatusTagType(row.orderStatus)" :class="reconcileStatusClass(row.orderStatus)">{{ reconcileStatusLabel(row.orderStatus) }}</el-tag></template></el-table-column>
              <el-table-column label="金额(元)" width="120"><template #default="{ row }"><span class="reconcile-money-compare">{{ formatCent(row.amountCent) }}</span></template></el-table-column>
              <el-table-column label="渠道" width="120"><template #default="{ row }"><AdminOverflowText :value="row.channel" text-class="reconcile-channel-text" /></template></el-table-column>
              <el-table-column label="交易时间" min-width="170"><template #default="{ row }"><AdminOverflowText :value="row.tradeTime" text-class="reconcile-time-text" /></template></el-table-column>
            </el-table>
            <div class="admin-pagination"><el-pagination background layout="sizes, prev, pager, next, total" :current-page="localPager.page" :page-size="localPager.size" :page-sizes="ADMIN_PAGE_SIZES" :total="localPager.total" @current-change="handleLocalPageChange" @size-change="handleLocalSizeChange" /></div>
          </el-tab-pane>

          <el-tab-pane label="渠道账单" name="channels">
            <el-table v-loading="channelLoading" :data="channelRows" class="admin-table" empty-text="暂无渠道账单">
              <el-table-column prop="bizType" label="类型" width="80"><template #default="{ row }"><el-tag effect="plain" :class="row.bizType === 'REFUND' ? 'reconcile-tag--refund' : 'reconcile-tag--pay'">{{ row.bizType === 'REFUND' ? '退款' : '支付' }}</el-tag></template></el-table-column>
              <el-table-column label="商户单号" min-width="160"><template #default="{ row }"><AdminOverflowText :value="row.outTradeNo" text-class="reconcile-code-text" /></template></el-table-column>
              <el-table-column label="渠道交易号" min-width="180"><template #default="{ row }"><AdminOverflowText :value="row.channelTradeNo" text-class="reconcile-flow-text" /></template></el-table-column>
              <el-table-column label="退款单号" min-width="150"><template #default="{ row }"><AdminOverflowText :value="row.refundNo" text-class="reconcile-flow-text" /></template></el-table-column>
              <el-table-column label="渠道退款号" min-width="160"><template #default="{ row }"><AdminOverflowText :value="row.channelRefundNo" text-class="reconcile-flow-text" /></template></el-table-column>
              <el-table-column label="渠道状态" min-width="130"><template #default="{ row }"><el-tag effect="light" :type="reconcileStatusTagType(row.channelStatus)" :class="reconcileStatusClass(row.channelStatus)">{{ reconcileStatusLabel(row.channelStatus) }}</el-tag></template></el-table-column>
              <el-table-column label="金额(元)" width="120"><template #default="{ row }"><span class="reconcile-money-compare">{{ formatCent(row.amountCent) }}</span></template></el-table-column>
              <el-table-column label="交易时间" min-width="170"><template #default="{ row }"><AdminOverflowText :value="row.tradeTime" text-class="reconcile-time-text" /></template></el-table-column>
            </el-table>
            <div class="admin-pagination"><el-pagination background layout="sizes, prev, pager, next, total" :current-page="channelPager.page" :page-size="channelPager.size" :page-sizes="ADMIN_PAGE_SIZES" :total="channelPager.total" @current-change="handleChannelPageChange" @size-change="handleChannelSizeChange" /></div>
          </el-tab-pane>
        </el-tabs>
      </template>

      <el-empty v-else description="请选择或创建一个对账任务" />
        </el-card>
      </el-tab-pane>

      <el-tab-pane v-if="canViewOnlineReconciliation" label="差错挂账跟进" name="hanging">
        <el-card class="admin-page-card">
          <div class="section-title-row"><div><h2>差错挂账 & 跟进管理</h2><p>统一管理未解决的线上对账挂账差异，当前阶段仅支持跟进留痕、财务处理标记和人工完结。</p></div><el-button @click="loadHangingFollows">刷新挂账</el-button></div>
          <el-alert title="当前阶段说明" description="本页“跟进”“标记需财务处理”“人工完结”仅作用于对账差异单自身，用于运营留痕和流程标记；不会生成真实财务调账单，也不会修改订单、支付、退款或资金主数据。" type="info" show-icon :closable="false" class="hanging-stage-alert" />
          <div class="admin-filter-bar hanging-filter-bar"><el-input v-model="hangingQuery.keyword" clearable placeholder="订单号/流水号/任务号" style="width: 220px" @keyup.enter="loadHangingFollows" /><el-select v-model="hangingQuery.riskLevel" clearable placeholder="风险等级" style="width: 150px"><el-option label="高风险" value="HIGH" /><el-option label="中风险" value="MEDIUM" /><el-option label="低风险" value="LOW" /></el-select><el-button type="primary" @click="loadHangingFollows">查询</el-button><el-button @click="resetHangingFollows">重置</el-button></div>
          <el-table v-loading="hangingLoading" :data="hangingRows" class="admin-table hanging-follow-table reconcile-overflow-table" empty-text="暂无挂账差异" row-key="id">
            <el-table-column label="任务号" min-width="130"><template #default="{ row }"><AdminOverflowText :value="row.taskNo" text-class="reconcile-code-text" /></template></el-table-column>
            <el-table-column label="账期" width="120"><template #default="{ row }"><AdminOverflowText :value="row.reconcileDate" text-class="reconcile-time-text" /></template></el-table-column>
            <el-table-column label="风险" width="90"><template #default="{ row }"><el-tag effect="light" :type="hangingRiskType(row.riskLevel)" :class="hangingRiskClass(row.riskLevel)">{{ hangingRiskLabel(row.riskLevel) }}</el-tag></template></el-table-column>
            <el-table-column label="差异类型" min-width="140"><template #default="{ row }"><el-tag effect="plain" :class="diffTypeClass(row.diffType)" :type="diffTypeTagType(row.diffType)">{{ diffTypeLabel(row.diffType) }}</el-tag></template></el-table-column>
            <el-table-column label="订单号" min-width="140"><template #default="{ row }"><AdminOverflowText :value="row.orderNo" text-class="reconcile-code-text" /></template></el-table-column>
            <el-table-column label="支付单号" min-width="140"><template #default="{ row }"><AdminOverflowText :value="row.payOrderNo" text-class="reconcile-flow-text" /></template></el-table-column>
            <el-table-column label="差异金额" width="90"><template #default="{ row }"><span class="reconcile-danger-money">{{ formatCent(row.diffAmountCent) }}</span></template></el-table-column>
            <el-table-column label="挂账天数" width="80"><template #default="{ row }"><span class="reconcile-day-pill">{{ row.hangingDays || 0 }} 天</span></template></el-table-column>
            <el-table-column label="最新跟进" min-width="160"><template #default="{ row }"><AdminOverflowText :value="row.latestFollowContent" text-class="reconcile-remark-text" /></template></el-table-column>
            <el-table-column label="操作" width="360" fixed="right" align="center"><template #default="{ row }"><div class="hanging-action-group"><el-button size="small" @click="openHangingDetail(row)">详情</el-button><el-button v-if="canFollowHanging" size="small" type="primary" plain @click="openFollowDialog(row)">跟进</el-button><el-button v-if="canFollowHanging" size="small" type="warning" plain @click="transferHangingToFinance(row)">标记需财务处理</el-button><el-button v-if="canFollowHanging" size="small" type="success" plain @click="closeHanging(row)">人工完结</el-button></div></template></el-table-column>
          </el-table>
          <div class="admin-pagination"><el-pagination background layout="sizes, prev, pager, next, total" :current-page="hangingPager.page" :page-size="hangingPager.size" :page-sizes="ADMIN_PAGE_SIZES" :total="hangingPager.total" @current-change="handleHangingPageChange" @size-change="handleHangingSizeChange" /></div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane v-if="canViewOnlineReconciliation" label="对账报表归档" name="archive">
        <el-card class="admin-page-card">
          <div class="section-title-row"><div><h2>对账报表 & 归档查询</h2><p>归档数据支持线上查询统计，也支持按日期或单个任务导出总表、差异清单、挂账调账记录和操作日志。</p></div><div class="reconcile-actions"><el-button @click="exportArchiveReport">导出任务汇总</el-button><el-button type="primary" plain :disabled="!archiveSelectedTasks.length" @click="exportSelectedArchivePackage">导出已选归档包 {{ archiveSelectedTasks.length ? `(${archiveSelectedTasks.length})` : '' }}</el-button></div></div>
          <div class="admin-filter-bar archive-filter-bar"><el-date-picker v-model="archiveQuery.range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" /><el-select v-model="archiveQuery.channel" clearable placeholder="渠道" style="width: 150px"><el-option label="MOCK模拟渠道" value="MOCK" /><el-option label="支付宝" value="ALIPAY" /></el-select><el-select v-model="archiveQuery.bizType" clearable placeholder="交易类型" style="width: 140px"><el-option label="支付" value="PAY" /><el-option label="退款" value="REFUND" /></el-select><el-select v-model="archiveQuery.diffType" clearable placeholder="差异类型" style="width: 180px"><el-option label="长款/本地有渠道无" value="LOCAL_EXISTS_CHANNEL_MISSING" /><el-option label="单边账/渠道有本地无" value="CHANNEL_EXISTS_LOCAL_MISSING" /><el-option label="金额不符" value="AMOUNT_MISMATCH" /><el-option label="退款异常" value="REFUND_STATUS_MISMATCH" /></el-select><el-button type="primary" @click="loadArchiveReport">查询</el-button><el-button @click="resetArchiveQuery">重置</el-button></div>
          <div v-loading="archiveLoading">
            <div class="task-summary-grid archive-summary"><div><span>任务数</span><strong>{{ archiveReport.totalTasks || 0 }}</strong><em>已归档 {{ archiveReport.completedTasks || 0 }}</em></div><div><span>对账成功率</span><strong>{{ archiveSuccessRate }}%</strong><em>匹配 / 本地账单</em></div><div><span>差异数</span><strong>{{ archiveReport.totalDiffCount || 0 }}</strong><em>挂账 {{ archiveReport.totalHangCount || 0 }}</em></div><div><span>资金差额</span><strong>{{ formatCent(archiveReport.netDiffAmountCent) }}</strong><em>渠道 - 本地</em></div></div>
            <el-table :data="archiveReport.diffTypeStats || []" class="admin-table" empty-text="暂无差异统计"><el-table-column label="差异类型"><template #default="{ row }"><el-tag effect="plain" :class="diffTypeClass(row.diffType)" :type="diffTypeTagType(row.diffType)">{{ diffTypeLabel(row.diffType) }}</el-tag></template></el-table-column><el-table-column prop="count" label="数量" width="120" /><el-table-column label="金额汇总" width="140"><template #default="{ row }">{{ formatCent(row.amountCent) }}</template></el-table-column></el-table>
            <el-divider content-position="left">历史归档任务</el-divider>
            <el-table :data="archiveReport.tasks || []" class="admin-table" empty-text="暂无历史任务" row-key="id" @selection-change="handleArchiveSelectionChange"><el-table-column type="selection" width="46" /><el-table-column label="任务号" min-width="180"><template #default="{ row }"><AdminOverflowText :value="row.taskNo" text-class="reconcile-code-text" /></template></el-table-column><el-table-column label="账期" width="120"><template #default="{ row }"><AdminOverflowText :value="row.reconcileDate" text-class="reconcile-time-text" /></template></el-table-column><el-table-column label="渠道" width="100"><template #default="{ row }"><el-tag effect="plain" class="reconcile-channel-tag">{{ row.channel }}</el-tag></template></el-table-column><el-table-column label="状态" width="110"><template #default="{ row }"><el-tag effect="light" :type="taskStatusMeta(row.status).type" :class="taskStatusClass(row.status)">{{ taskStatusMeta(row.status).label }}</el-tag></template></el-table-column><el-table-column label="本地/渠道金额" min-width="180"><template #default="{ row }"><span class="reconcile-money-compare">{{ formatCent(row.localTotalAmountCent) }} / {{ formatCent(row.channelTotalAmountCent) }}</span></template></el-table-column><el-table-column label="差异/挂账" width="120"><template #default="{ row }"><span class="reconcile-count-pill">{{ row.diffCount || 0 }} / {{ row.hangCount || 0 }}</span></template></el-table-column><el-table-column label="导出/日志" width="300"><template #default="{ row }"><el-button size="small" plain @click="exportArchiveTaskPackage(row)">导出归档包</el-button><el-button size="small" plain @click="exportArchiveTaskDiffs(row)">差异清单</el-button><el-button size="small" plain @click="openTaskLogs(row)">操作日志</el-button></template></el-table-column></el-table>
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane v-if="canViewStockReconciliation" label="库存对账" name="stock">
        <el-card class="admin-page-card stock-reconcile-card">
      <div class="section-title-row">
        <div>
          <h2>库存对账</h2>
          <p>保留原有库存一致性校验结果，可对未处理的不一致记录执行单条修复或批量修复。</p>
        </div>
        <div class="reconcile-actions">
          <el-button type="success" :disabled="!selectedStockRows.length" @click="batchRepairStock">批量修复已选 {{ selectedStockRows.length ? `(${selectedStockRows.length})` : '' }}</el-button>
          <el-button @click="loadStockReconciliations">刷新库存对账</el-button>
        </div>
      </div>
      <div class="admin-filter-bar stock-filter-bar">
        <el-input v-model="stockQuery.skuId" placeholder="SKU ID" clearable style="width: 180px" @keyup.enter="loadStockReconciliations" />
        <el-select v-model="stockQuery.status" clearable placeholder="库存对账状态" style="width: 180px">
          <el-option label="一致" value="CONSISTENT" />
          <el-option label="不一致" value="INCONSISTENT" />
          <el-option label="已修复" value="REPAIRED" />
        </el-select>
        <el-button type="primary" @click="searchStockReconciliations">查询</el-button>
        <el-button @click="resetStockReconciliations">重置</el-button>
      </div>
      <el-table v-loading="stockLoading" :data="stockRows" class="admin-table stock-reconcile-table" empty-text="暂无库存对账记录" row-key="id" :default-sort="{ prop: 'id', order: 'descending' }" @sort-change="handleStockSortChange" @selection-change="handleStockSelectionChange">
        <el-table-column type="selection" width="46" :selectable="canManageStockReconcile" />
        <el-table-column prop="id" label="ID" width="70" sortable="custom" />
        <el-table-column prop="skuId" label="SKU ID" width="100" />
        <el-table-column prop="status" label="状态" width="120"><template #default="{ row }"><el-tag :type="stockStatusType(row.status)" :class="stockStatusClass(row.status)">{{ stockStatusLabel(row.status) }}</el-tag></template></el-table-column>
        <el-table-column prop="repairStatus" label="处理状态" width="120"><template #default="{ row }"><el-tag :type="stockRepairStatusType(row.repairStatus)" :class="stockRepairStatusClass(row.repairStatus)">{{ stockRepairStatusLabel(row.repairStatus) }}</el-tag></template></el-table-column>
        <el-table-column label="数据库库存快照" width="168" align="center" header-align="center">
          <template #default="{ row }">
            <div class="stock-snapshot-cell">
              <span>总 {{ snapshotValue(row.stockSnapshot, 'totalStock') }}</span>
              <span>锁 {{ snapshotValue(row.stockSnapshot, 'lockedStock') }}</span>
              <span>可 {{ snapshotValue(row.stockSnapshot, 'availableStock') }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Redis库存快照" width="168" align="center" header-align="center">
          <template #default="{ row }">
            <div class="stock-snapshot-cell stock-snapshot-cell--redis">
              <span>总 {{ snapshotValue(row.redisSnapshot, 'totalStock') }}</span>
              <span>锁 {{ snapshotValue(row.redisSnapshot, 'lockedStock') }}</span>
              <span>可 {{ snapshotValue(row.redisSnapshot, 'availableStock') }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="预期基准快照" width="168" align="center" header-align="center">
          <template #default="{ row }">
            <div class="stock-snapshot-cell stock-snapshot-cell--expected">
              <span>总 {{ snapshotValue(row.expectedSnapshot, 'totalStock') }}</span>
              <span>锁 {{ snapshotValue(row.expectedSnapshot, 'lockedStock') }}</span>
              <span>可 {{ snapshotValue(row.expectedSnapshot, 'availableStock') }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="差异说明" min-width="300">
          <template #default="{ row }">
            <el-tooltip :content="stockDifferenceText(row)" placement="top-start" :show-after="250" :hide-after="0" :offset="8" popper-class="stock-diff-tooltip" :disabled="!isStockDifferenceOverflow(row)">
              <span :class="['stock-diff-text', { 'stock-diff-ellipsis': isStockDifferenceOverflow(row) }]">{{ stockDifferenceText(row) }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="校验时间" width="190">
          <template #default="{ row }"><AdminOverflowText :value="formatStockCheckedAt(row.checkedAt)" /></template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canManageStockReconcile(row)" size="small" type="success" plain @click="repairStock(row)">修复</el-button>
          </template>
        </el-table-column>
      </el-table>
          <div class="admin-pagination"><el-pagination background layout="sizes, prev, pager, next, total" :current-page="stockPager.page" :page-size="stockPager.size" :page-sizes="ADMIN_PAGE_SIZES" :total="stockPager.total" @current-change="handleStockPageChange" @size-change="handleStockSizeChange" /></div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="createVisible" title="创建线上对账任务" width="460px">
      <el-form :model="createForm" label-width="90px">
        <el-form-item label="对账日期"><el-date-picker v-model="createForm.reconcileDate" type="date" value-format="YYYY-MM-DD" :disabled-date="disableReconcileDate" placeholder="请选择未创建过的对账账期" style="width: 100%" /></el-form-item>
        <el-form-item label="渠道"><el-select v-model="createForm.channel" style="width: 100%"><el-option label="MOCK模拟渠道" value="MOCK" /><el-option label="支付宝" value="ALIPAY" /></el-select></el-form-item>
        <el-alert v-if="createDateDuplicated" title="当前渠道下该对账日期已存在任务，请选择其它日期或渠道" type="warning" show-icon :closable="false" class="create-task-alert" />
        <el-form-item label="备注"><el-input v-model="createForm.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="createVisible = false">取消</el-button><el-button type="primary" @click="submitCreateTask">创建</el-button></template>
    </el-dialog>

    <el-dialog v-model="mockToolboxVisible" title="测试数据工具箱" width="680px">
      <el-alert title="该工具箱仅用于开发/测试造数" description="生成本地账单、Mock渠道账单和异常差异只服务于测试验证；业务差异详情页不再提供修改 Mock 渠道数据或高危模拟闭环入口。" type="warning" show-icon :closable="false" />
      <el-table :data="mockToolboxTasks" class="admin-table mock-toolbox-table" empty-text="暂无可操作的 Mock 对账任务">
        <el-table-column label="任务号" min-width="180"><template #default="{ row }"><AdminOverflowText :value="row.taskNo" /></template></el-table-column>
        <el-table-column label="账期" width="110"><template #default="{ row }"><AdminOverflowText :value="row.reconcileDate" /></template></el-table-column>
        <el-table-column label="测试标识" width="100"><template #default><el-tag type="warning" effect="plain">测试流水</el-tag></template></el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button v-if="canRunTask" size="small" type="primary" plain @click="generateLocal(row)">生成本地快照</el-button>
            <el-button v-if="canImportBill" size="small" type="warning" plain @click="generateMock(row)">生成Mock异常账单</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer><el-button @click="mockToolboxVisible = false">关闭</el-button></template>
    </el-dialog>

    <el-dialog v-model="hangingFollowVisible" title="新增挂账跟进" width="520px">
      <el-form label-width="90px"><el-form-item label="跟进内容"><el-input v-model="hangingFollowForm.remark" type="textarea" :rows="4" placeholder="请输入与渠道/用户/财务沟通内容、预计完成时间等" /></el-form-item></el-form>
      <template #footer><el-button @click="hangingFollowVisible = false">取消</el-button><el-button type="primary" @click="submitHangingFollow">提交</el-button></template>
    </el-dialog>

    <el-dialog v-model="taskLogsVisible" title="对账任务操作日志" width="820px">
      <el-timeline v-if="taskLogs.length" class="reconcile-log-timeline">
        <el-timeline-item v-for="log in taskLogs" :key="log.id" :timestamp="formatLogTime(log.createdAt)">
          <div class="reconcile-log-card">
            <div class="reconcile-log-card__header">
              <el-tag :type="actionMeta(log.operationType).type" effect="light">{{ actionMeta(log.operationType).label }}</el-tag>
              <span>{{ actionMeta(log.operationType).summary }}</span>
            </div>
            <p>{{ normalizeLogContent(log.operationContent, log.operationType) }}</p>
            <small>操作人：{{ log.operatorName || '-' }}<template v-if="log.remark">；处理依据：{{ log.remark }}</template></small>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无操作日志" :image-size="60" />
    </el-dialog>

    <el-dialog v-model="diffDetailVisible" title="线上对账差异详情" width="920px">
      <template v-if="diffDetail">
        <div class="diff-detail-summary">
          <div><span>差异单号</span><strong>#{{ diffDetail.diff.id }}</strong><small><el-tag :type="isMockDiff(diffDetail) ? 'warning' : 'success'" effect="plain">{{ isMockDiff(diffDetail) ? '测试流水' : '正式流水' }}</el-tag></small></div>
          <div><span>差异类型</span><strong>{{ diffTypeLabel(diffDetail.diff.diffType) }}</strong><small v-if="diffTypeTip(diffDetail.diff.diffType)">{{ diffTypeTip(diffDetail.diff.diffType) }}</small></div>
          <div><span>业务类型</span><strong>{{ diffDetail.diff.bizType === 'REFUND' ? '退款' : '支付' }}</strong></div>
          <div><span>处理状态</span><el-tag :type="processStatusType(diffDetail.diff.processStatus)">{{ processStatusLabel(diffDetail.diff.processStatus) }}</el-tag></div>
        </div>
        <div class="bill-compare-grid">
          <div class="bill-compare-card">
            <h3>本地账单快照</h3>
            <template v-if="diffDetail.localBill">
              <p><span>订单号</span><strong>{{ diffDetail.localBill.orderNo || '-' }}</strong></p>
              <p><span>支付单号</span><strong>{{ diffDetail.localBill.payOrderNo || '-' }}</strong></p>
              <p><span>退款单号</span><strong>{{ diffDetail.localBill.refundNo || '-' }}</strong></p>
              <p><span>本地状态</span><strong>{{ reconcileStatusLabel(diffDetail.localBill.localStatus) }}</strong></p>
              <p><span>订单状态</span><strong>{{ reconcileStatusLabel(diffDetail.localBill.orderStatus) }}</strong></p>
              <p><span>金额</span><strong>{{ formatCent(diffDetail.localBill.amountCent) }} 元</strong></p>
              <p><span>交易时间</span><strong>{{ diffDetail.localBill.tradeTime || '-' }}</strong></p>
            </template>
            <el-empty v-else description="本地无账单" :image-size="60" />
          </div>
          <div class="bill-compare-card">
            <h3>渠道账单快照</h3>
            <template v-if="diffDetail.channelBill">
              <p><span>商户单号</span><strong>{{ diffDetail.channelBill.outTradeNo || '-' }}</strong></p>
              <p><span>渠道交易号</span><strong>{{ diffDetail.channelBill.channelTradeNo || '-' }}</strong></p>
              <p><span>退款单号</span><strong>{{ diffDetail.channelBill.refundNo || '-' }}</strong></p>
              <p><span>渠道退款号</span><strong>{{ diffDetail.channelBill.channelRefundNo || '-' }}</strong></p>
              <p><span>渠道状态</span><strong>{{ reconcileStatusLabel(diffDetail.channelBill.channelStatus) }}</strong></p>
              <p><span>金额</span><strong>{{ formatCent(diffDetail.channelBill.amountCent) }} 元</strong></p>
              <p><span>交易时间</span><strong>{{ diffDetail.channelBill.tradeTime || '-' }}</strong></p>
            </template>
            <el-empty v-else description="渠道无账单" :image-size="60" />
          </div>
        </div>
        <div class="diff-audit-panel">
          <el-alert :title="systemRecognitionTitle" :description="systemRecognitionDescription" type="warning" show-icon :closable="false" />
          <div class="diff-audit-actions">
            <el-button size="small" :disabled="!diffOrderNo" @click="goOrderDetail">查看订单详情</el-button>
            <el-button size="small" :disabled="!diffPayKeyword" @click="goPayRecord">查看支付记录</el-button>
            <el-button size="small" :disabled="!diffRefundNo" @click="goRefundRecord">查看退款记录</el-button>
            <el-button size="small" :disabled="!diffCallbackKeyword" @click="goCallbackRecord">查看回调记录</el-button>
          </div>
          <el-alert v-if="channelCheckTips.length" title="渠道流水核查提示" type="info" show-icon :closable="false" class="channel-check-alert">
            <ul class="channel-check-list"><li v-for="tip in channelCheckTips" :key="tip">{{ tip }}</li></ul>
          </el-alert>
          <el-alert v-if="recommendedReason" :title="`推荐原因：${recommendedReason}`" description="系统仅根据业务类型、账单状态和差异类型给出预判，最终原因仍需运营人工确认。" type="success" show-icon :closable="false" />
          <el-alert :title="`差异备注：${diffRemarkText(diffDetail.diff)}`" type="info" show-icon :closable="false" class="diff-handle-alert" />
        </div>
        <el-alert v-if="diffDetail.diff.processStatus === 'HANGING'" title="该差异已登记挂账" description="挂账后不能在详情页重复挂账或二次处理，请到“差错挂账跟进”页新增跟进、转财务或完结闭环。" type="warning" show-icon :closable="false" class="diff-handle-alert" />
        <el-form v-if="diffDetail.diff.processStatus === 'PENDING'" :model="diffHandleForm" label-width="110px" class="diff-handle-form">
          <template v-if="diffHandleSchema">
            <el-alert :title="diffHandleSchema.title" :description="diffHandleSchema.description" type="info" show-icon :closable="false" class="diff-handle-alert" />
            <el-form-item v-if="diffHandleSchema.reasons?.length" label="判定原因">
              <el-radio-group v-model="diffHandleForm.reason">
                <el-radio v-for="reason in diffHandleSchema.reasons" :key="reason" :label="reason">{{ reason }}</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="diffHandleSchema.judgements?.length" label="判定类型">
              <el-radio-group v-model="diffHandleForm.judgement">
                <el-radio v-for="judgement in diffHandleSchema.judgements" :key="judgement" :label="judgement">{{ judgement }}</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="diffHandleSchema.showAmount" label="差异金额">
              <el-input v-model="diffHandleForm.diffAmount" placeholder="请输入差异金额，单位元" />
            </el-form-item>
          </template>
          <el-form-item label="处理备注"><el-input v-model="diffHandleForm.remark" type="textarea" :rows="3" placeholder="请输入处理依据，例如渠道流水核验、客服确认、挂账原因等" /></el-form-item>
        </el-form>
        <div v-if="(canHandleDiff || canRepairDiff) && diffActions(diffDetail.diff).length" class="diff-action-bar">
          <el-tooltip v-for="action in diffActions(diffDetail.diff)" :key="action.value" :disabled="!action.disabledTip" :content="action.disabledTip" placement="top">
            <span><el-button :type="action.type" :plain="action.plain" :loading="diffHandling" :disabled="Boolean(action.disabledTip) || !canExecuteAction(action)" @click="handleDiffAction(action.value)">{{ action.label }}</el-button></span>
          </el-tooltip>
        </div>
        <el-divider content-position="left">处理记录</el-divider>
        <el-timeline v-if="diffDetail.logs?.length" class="reconcile-log-timeline">
          <el-timeline-item v-for="log in diffDetail.logs" :key="log.id" :timestamp="formatLogTime(log.createdAt)">
            <div class="reconcile-log-card">
              <div class="reconcile-log-card__header">
                <el-tag :type="actionMeta(log.operationType).type" effect="light">{{ actionMeta(log.operationType).label }}</el-tag>
                <span>{{ actionMeta(log.operationType).summary }}</span>
              </div>
              <p>{{ normalizeLogContent(log.operationContent, log.operationType) }}</p>
              <small>操作人：{{ log.operatorName || '-' }}<template v-if="log.remark">；处理依据：{{ log.remark }}</template></small>
            </div>
          </el-timeline-item>
        </el-timeline>
        <el-empty v-else description="暂无处理记录" :image-size="60" />
      </template>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { useRoute, useRouter } from 'vue-router';
import * as XLSX from 'xlsx';
import AdminLayout from '../components/AdminLayout.vue';
import AdminOverflowText from '../components/AdminOverflowText.vue';
import { closeAdminOnlineHangingDiff, completeAdminOnlineReconcileTask, createAdminOnlineReconcileTask, fetchAdminOnlineArchiveReport, fetchAdminOnlineChannelBills, fetchAdminOnlineDiffItem, fetchAdminOnlineDiffItems, fetchAdminOnlineHangingFollows, fetchAdminOnlineLocalBills, fetchAdminOnlineReconcileTasks, fetchAdminOnlineTaskLogs, fetchAdminStockReconciliations, followAdminOnlineHangingDiff, generateAdminOnlineLocalBills, generateAdminOnlineMockChannelBills, handleAdminOnlineDiffItem, matchAdminOnlineReconcileTask, repairAdminStockReconciliation, transferAdminOnlineHangingDiffToFinance, uploadAdminAlipayChannelBills } from '../api';
import { useAdminStore } from '../stores/admin';
import { confirmAction } from '../utils/action';
import { ADMIN_PAGE_SIZE, ADMIN_PAGE_SIZES } from '../utils/pagination';

const router = useRouter();
const route = useRoute();
const adminStore = useAdminStore();
const canViewOnlineReconciliation = computed(() => adminStore.hasPermission('reconciliation:view'));
const canViewStockReconciliation = computed(() => adminStore.hasPermission('stock:reconcile:view'));
const firstAccessibleMainTab = () => canViewOnlineReconciliation.value ? 'online' : (canViewStockReconciliation.value ? 'stock' : 'online');
const normalizeMainTab = (tab) => {
  const value = String(tab || '');
  if (value === 'stock') return canViewStockReconciliation.value ? 'stock' : firstAccessibleMainTab();
  if (['online', 'hanging', 'archive'].includes(value)) return canViewOnlineReconciliation.value ? value : firstAccessibleMainTab();
  return firstAccessibleMainTab();
};
const mainTab = ref(normalizeMainTab(route.query.tab || route.query.type));
const canCreateTask = computed(() => adminStore.hasPermission('reconciliation:task:create'));
const canRunTask = computed(() => adminStore.hasPermission('reconciliation:task:run'));
const canArchiveTask = computed(() => adminStore.hasPermission('reconciliation:task:archive'));
const canImportBill = computed(() => adminStore.hasPermission('reconciliation:bill:import'));
const canHandleDiff = computed(() => adminStore.hasPermission('reconciliation:diff:handle'));
const canRepairDiff = computed(() => adminStore.hasPermission('reconciliation:diff:repair'));
const canFollowHanging = computed(() => adminStore.hasPermission('reconciliation:hanging:follow'));
const canManage = computed(() => canHandleDiff.value || canRepairDiff.value || canFollowHanging.value);
const taskQuery = reactive({ status: route.query.status === 'MATCHED' ? 'MATCHED' : '', channel: '' });
const taskPager = reactive({ page: 1, size: ADMIN_PAGE_SIZE, total: 0 });
const tasks = ref([]);
const taskLoading = ref(false);
const currentTask = ref(null);
const detailTab = ref('diffs');
const createVisible = ref(false);
const toLocalDateKey = (value) => {
  if (!value) return '';
  if (typeof value === 'string') return value.slice(0, 10);
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};
const yesterdayKey = () => {
  const date = new Date();
  date.setDate(date.getDate() - 1);
  return toLocalDateKey(date);
};
const createForm = reactive({ reconcileDate: yesterdayKey(), channel: 'MOCK', remark: '' });
const dateKey = (value) => toLocalDateKey(value);
const hasExistingReconcileTask = (reconcileDate, channel) => {
  const targetDate = dateKey(reconcileDate);
  const targetChannel = channel ? String(channel).toUpperCase() : '';
  return !!targetDate && tasks.value.some((task) => dateKey(task.reconcileDate) === targetDate && (!targetChannel || String(task.channel || '').toUpperCase() === targetChannel));
};
const disableReconcileDate = (date) => {
  const targetDate = dateKey(date);
  const yesterday = yesterdayKey();
  return targetDate >= yesterday || hasExistingReconcileTask(targetDate, createForm.channel);
};
const createDateDuplicated = computed(() => hasExistingReconcileTask(createForm.reconcileDate, createForm.channel));
const archiveSuccessRate = computed(() => {
  const total = Number(archiveReport.value?.totalLocalCount || 0);
  if (!total) return '0.00';
  return ((Number(archiveReport.value?.totalMatchedCount || 0) / total) * 100).toFixed(2);
});

const localRows = ref([]);
const channelRows = ref([]);
const diffRows = ref([]);
const localLoading = ref(false);
const channelLoading = ref(false);
const diffLoading = ref(false);
const localPager = reactive({ page: 1, size: ADMIN_PAGE_SIZE, total: 0 });
const channelPager = reactive({ page: 1, size: ADMIN_PAGE_SIZE, total: 0 });
const diffPager = reactive({ page: 1, size: ADMIN_PAGE_SIZE, total: 0 });
const diffQuery = reactive({ processStatus: route.query.processStatus === 'PENDING' ? 'PENDING' : '' });
const diffDetailVisible = ref(false);
const diffDetail = ref(null);
const diffHandling = ref(false);
const selectedDiffRows = ref([]);
const diffHandleForm = reactive({ reason: '', judgement: '', diffAmount: '', remark: '' });
const normalizeStockStatusQuery = (status) => ['CONSISTENT', 'INCONSISTENT', 'REPAIRED'].includes(String(status || '').toUpperCase()) ? String(status).toUpperCase() : '';
const stockQuery = reactive({ skuId: String(route.query.stockSkuId ?? ''), status: normalizeStockStatusQuery(route.query.stockStatus), sortBy: String(route.query.stockSortBy || 'id'), sortOrder: String(route.query.stockSortOrder || 'desc') });
const stockPager = reactive({ page: Number(route.query.stockPage || 1), size: Number(route.query.stockSize || ADMIN_PAGE_SIZE), total: 0 });
const stockRows = ref([]);
const stockLoading = ref(false);
const selectedStockRows = ref([]);
const hangingQuery = reactive({ keyword: '', riskLevel: '' });
const hangingPager = reactive({ page: 1, size: ADMIN_PAGE_SIZE, total: 0 });
const hangingRows = ref([]);
const hangingLoading = ref(false);
const hangingFollowVisible = ref(false);
const currentHanging = ref(null);
const hangingFollowForm = reactive({ remark: '' });
const archiveQuery = reactive({ range: [], channel: '', bizType: '', diffType: '' });
const archiveReport = ref({ diffTypeStats: [], tasks: [] });
const archiveLoading = ref(false);
const archiveSelectedTasks = ref([]);
const taskLogsVisible = ref(false);
const taskLogs = ref([]);
const mockToolboxVisible = ref(false);

const formatCent = (value) => {
  const amount = Number(value);
  if (!Number.isFinite(amount)) return '-';
  return (amount / 100).toFixed(2);
};
const taskStatusMeta = (status) => ({
  CREATED: { label: '已创建', type: 'info' },
  LOCAL_BILL_READY: { label: '本地就绪', type: 'warning' },
  CHANNEL_BILL_READY: { label: '账单就绪', type: 'warning' },
  MATCHED: { label: '已勾兑', type: 'success' },
  COMPLETED: { label: '已归档', type: 'success' },
}[status] || { label: status || '-', type: 'info' });
const taskStatusClass = (status) => ({ CREATED: 'task-status--created', LOCAL_BILL_READY: 'task-status--local-ready', CHANNEL_BILL_READY: 'task-status--channel-ready', MATCHED: 'task-status--matched', COMPLETED: 'task-status--completed' }[status] || '');
const billStatusType = (status) => status === 'READY' ? 'success' : 'info';
const isTaskArchived = (row) => row?.status === 'COMPLETED';
const canCompleteTask = (row) => canArchiveTask.value && !isTaskArchived(row) && Number(row?.pendingCount || 0) === 0;
const diffTypeLabel = (type) => ({
  LOCAL_EXISTS_CHANNEL_MISSING: '漏单：本地有、渠道无',
  CHANNEL_EXISTS_LOCAL_MISSING: '单边账：渠道有、本地无',
  STATUS_MISMATCH: '状态不一致',
  STATUS_AND_AMOUNT_MISMATCH: '状态和金额不一致',
  AMOUNT_MISMATCH: '长短款：金额不一致',
  REFUND_STATUS_MISMATCH: '退款状态不一致',
  REFUND_AMOUNT_MISMATCH: '退款金额不一致',
  DUPLICATE_LOCAL: '本地重复账单',
  DUPLICATE_CHANNEL: '渠道重复账单',
  MATCHED: '已匹配',
}[type] || type || '-');
const diffTypeTip = (type) => ({
  LOCAL_EXISTS_CHANNEL_MISSING: '漏单：本地系统存在支付或退款记录，但渠道账单未返回对应流水；支付场景核实是否用户未支付、支付超时、回调丢失或渠道入账延迟，退款场景核实是否渠道退款延迟入账或账单日期不一致。',
  CHANNEL_EXISTS_LOCAL_MISSING: '单边账：渠道账单存在流水，但本地系统未找到对应支付或退款记录，需要核实是否测试流水、真实收款未入库或重复回调。',
  AMOUNT_MISMATCH: '金额或手续费不符：需要核对签约费率、实收金额与本地应收/应退金额。',
  STATUS_AND_AMOUNT_MISMATCH: '状态和金额同时不一致：建议人工复核后挂账或提交财务调账。',
  REFUND_STATUS_MISMATCH: '退款异常：需要核对本地退款申请与渠道退款结果。',
}[type] || '');
const diffTypeTagType = (type) => ({
  MATCHED: 'success',
  LOCAL_EXISTS_CHANNEL_MISSING: 'warning',
  CHANNEL_EXISTS_LOCAL_MISSING: 'danger',
  AMOUNT_MISMATCH: 'warning',
  STATUS_AND_AMOUNT_MISMATCH: 'warning',
  REFUND_STATUS_MISMATCH: '',
  REFUND_AMOUNT_MISMATCH: '',
  DUPLICATE_LOCAL: 'info',
  DUPLICATE_CHANNEL: 'info',
}[type] || 'info');
const diffTypeClass = (type) => ({
  MATCHED: 'diff-tag--matched',
  LOCAL_EXISTS_CHANNEL_MISSING: 'diff-tag--missing-channel',
  CHANNEL_EXISTS_LOCAL_MISSING: 'diff-tag--single-side',
  AMOUNT_MISMATCH: 'diff-tag--amount',
  STATUS_AND_AMOUNT_MISMATCH: 'diff-tag--amount',
  REFUND_STATUS_MISMATCH: 'diff-tag--refund',
  REFUND_AMOUNT_MISMATCH: 'diff-tag--refund',
  DUPLICATE_LOCAL: 'diff-tag--duplicate',
  DUPLICATE_CHANNEL: 'diff-tag--duplicate',
}[type] || '');
const diffLevelLabel = (level) => ({ HIGH: '高', MEDIUM: '中', LOW: '低' }[level] || level || '-');
const diffLevelType = (level) => ({ HIGH: 'danger', MEDIUM: 'warning', LOW: 'info' }[level] || 'info');
const diffLevelClass = (level) => ({ HIGH: 'diff-level--high', MEDIUM: 'diff-level--medium', LOW: 'diff-level--low' }[level] || '');
const actionMetaMap = {
  MANUAL_REVIEW: { label: '人工复核', type: 'info', summary: '交由运营人工确认原因' },
  SYNC_PAY_STATUS: { label: '同步支付状态', type: 'primary', summary: '同步支付系统状态并刷新订单' },
  SYNC_REFUND_RESULT: { label: '同步退款结果', type: 'primary', summary: '同步退款渠道结果并刷新记录' },
  REPAIR_ORDER_STATUS: { label: '补偿订单状态', type: 'success', summary: '修正本地订单支付状态' },
  MARK_DONE: { label: '标记已处理', type: 'success', summary: '仅完成差异闭环，不再继续追踪' },
  CLOSE_ORDER_VOID: { label: '关闭订单/标记作废', type: 'danger', summary: '关闭异常订单并结束处理' },
  MARK_TEST_FLOW_VOID: { label: '标记测试流水', type: 'info', summary: '标记为测试或压测数据，不纳入正式闭环' },
  MANUAL_SUPPLEMENT_TRADE: { label: '补录交易记录', type: 'primary', summary: '补回真实交易记录并恢复对账一致性' },
  MARK_DUPLICATE_OFFSET: { label: '标记重复流水', type: 'warning', summary: '标记重复入账或重复回调流水' },
  REGISTER_AMOUNT_ADJUSTMENT: { label: '登记长短款', type: 'warning', summary: '记录金额差异并等待后续调账' },
  SUBMIT_FINANCE_ADJUSTMENT: { label: '提交财务调账', type: 'primary', summary: '将差异提交给财务继续处理' },
  RETRY_REFUND: { label: '重新发起退款', type: 'warning', summary: '重新提交退款流程' },
  FIX_REFUND_STATUS: { label: '人工修正退款状态', type: 'primary', summary: '修正本地退款状态并继续闭环' },
  VOID_DUPLICATE_FLOW: { label: '重复作废', type: 'info', summary: '保留有效流水，作废重复流水' },
  REFUND_DUPLICATE_CHARGE: { label: '重复扣款退款', type: 'danger', summary: '对重复扣款进行退款处理' },
  HANG: { label: '登记挂账', type: 'warning', summary: '暂挂待跟进' },
  IGNORE: { label: '忽略差异', type: 'info', summary: '确认后忽略该差异' },
  NONE: { label: '无需处理', type: 'info', summary: '无需额外动作' },
};
const actionMeta = (action) => actionMetaMap[action] || { label: action || '-', type: 'info', summary: '-' };
const actionTagClass = (action) => ['reconcile-action-tag', `reconcile-action-tag--${String(action || 'none').toLowerCase().replace(/_/g, '-')}`];
const suggestedActionLabel = (action) => actionMeta(action).label;
const diffRemarkText = (row = {}) => row.processRemark || row.diffRemark || row.remark || row.suggestedRemark || diffTypeTip(row.diffType) || (row.diffType ? diffTypeLabel(row.diffType) : '--');
const processStatusLabel = (status) => ({ PENDING: '待处理', DONE: '已完成', HANGING: '挂账', IGNORED: '已忽略' }[status] || status || '-');
const processStatusType = (status) => ({ PENDING: 'warning', DONE: 'success', HANGING: 'danger', IGNORED: 'info' }[status] || 'info');
const processStatusClass = (status) => ({ PENDING: 'process-status--pending', DONE: 'process-status--done', HANGING: 'process-status--hanging', IGNORED: 'process-status--ignored' }[status] || '');
const hangingRiskLabel = (risk) => ({ HIGH: '高风险', MEDIUM: '中风险', LOW: '低风险' }[risk] || risk || '-');
const hangingRiskType = (risk) => ({ HIGH: 'danger', MEDIUM: 'warning', LOW: 'info' }[risk] || 'info');
const hangingRiskClass = (risk) => ({ HIGH: 'hanging-risk--high', MEDIUM: 'hanging-risk--medium', LOW: 'hanging-risk--low' }[risk] || '');
const normalizeReconcileStatus = (status) => String(status || '').toUpperCase();
const reconcileStatusLabel = (status) => ({
  SUCCESS: '支付成功',
  PAY_SUCCESS: '支付成功',
  PAID_SUCCESS: '支付成功',
  PAYMENT_SUCCESS: '支付成功',
  PENDING: '待支付',
  WAITING: '待支付',
  WAIT_PAY: '待支付',
  UNPAID: '待支付',
  CLOSED: '支付关闭',
  PAY_CLOSED: '支付关闭',
  FAILED: '支付失败',
  PAY_FAILED: '支付失败',
  REFUNDED: '已退款',
  PARTIALLY_REFUNDED: '部分退款',
  PARTIAL_REFUND: '部分退款',
  REFUND_SUCCESS: '已退款',
  REFUND_FAILED: '退款失败',
  REFUND_PROCESSING: '退款处理中',
  REFUNDING: '退款处理中',
  REFUND_CLOSED: '退款关闭',
  PAID: '已退款',
  SHIPPED: '已发货',
  COMPLETED: '已完成',
  FINISHED: '已完成',
  CANCELLED: '已取消',
  CANCELED: '已取消',
  CREATED: '已创建',
  NEW: '已创建',
  TRADE_SUCCESS: '渠道支付成功',
  TRADE_FINISHED: '渠道交易完成',
  TRADE_CLOSED: '渠道交易关闭',
  TRADE_FAILED: '渠道交易失败',
  WAIT_BUYER_PAY: '渠道待支付',
}[normalizeReconcileStatus(status)] || status || '-');
const reconcileStatusTagType = (status) => ({
  SUCCESS: 'success',
  PAY_SUCCESS: 'success',
  PAID_SUCCESS: 'success',
  PAYMENT_SUCCESS: 'success',
  REFUND_SUCCESS: 'danger',
  REFUNDED: 'danger',
  PAID: 'danger',
  COMPLETED: 'danger',
  FINISHED: 'danger',
  TRADE_SUCCESS: 'success',
  TRADE_FINISHED: 'success',
  SHIPPED: 'primary',
  PENDING: 'warning',
  WAITING: 'warning',
  WAIT_PAY: 'warning',
  UNPAID: 'warning',
  REFUND_PROCESSING: 'warning',
  REFUNDING: 'warning',
  WAIT_BUYER_PAY: 'warning',
  CLOSED: 'info',
  PAY_CLOSED: 'info',
  REFUND_CLOSED: 'info',
  CANCELLED: 'info',
  CANCELED: 'info',
  TRADE_CLOSED: 'info',
  CREATED: 'info',
  NEW: 'info',
  FAILED: 'danger',
  PAY_FAILED: 'danger',
  REFUND_FAILED: 'danger',
  TRADE_FAILED: 'danger',
}[normalizeReconcileStatus(status)] || 'info');
const reconcileStatusClass = (status) => ({
  SUCCESS: 'reconcile-status--success',
  PAY_SUCCESS: 'reconcile-status--success',
  PAID_SUCCESS: 'reconcile-status--success',
  PAYMENT_SUCCESS: 'reconcile-status--success',
  REFUND_SUCCESS: 'reconcile-status--danger',
  REFUNDED: 'reconcile-status--danger',
  PAID: 'reconcile-status--danger',
  COMPLETED: 'reconcile-status--success',
  FINISHED: 'reconcile-status--success',
  TRADE_SUCCESS: 'reconcile-status--success',
  TRADE_FINISHED: 'reconcile-status--success',
  SHIPPED: 'reconcile-status--primary',
  PENDING: 'reconcile-status--warning',
  WAITING: 'reconcile-status--warning',
  WAIT_PAY: 'reconcile-status--warning',
  UNPAID: 'reconcile-status--warning',
  REFUND_PROCESSING: 'reconcile-status--warning',
  REFUNDING: 'reconcile-status--warning',
  WAIT_BUYER_PAY: 'reconcile-status--warning',
  CLOSED: 'reconcile-status--info',
  PAY_CLOSED: 'reconcile-status--info',
  REFUND_CLOSED: 'reconcile-status--info',
  CANCELLED: 'reconcile-status--info',
  CANCELED: 'reconcile-status--info',
  TRADE_CLOSED: 'reconcile-status--info',
  CREATED: 'reconcile-status--info',
  NEW: 'reconcile-status--info',
  FAILED: 'reconcile-status--danger',
  PAY_FAILED: 'reconcile-status--danger',
  REFUND_FAILED: 'reconcile-status--danger',
  TRADE_FAILED: 'reconcile-status--danger',
}[normalizeReconcileStatus(status)] || 'reconcile-status--info');
const stockStatusLabel = (status) => ({ CONSISTENT: '一致', REPAIRED: '已修复', INCONSISTENT: '不一致', ABNORMAL: '异常', IGNORED: '已忽略' }[status] || status || '-');
const stockStatusType = (status) => ({ CONSISTENT: 'success', REPAIRED: 'primary', INCONSISTENT: 'danger', ABNORMAL: 'danger', IGNORED: 'info' }[status] || 'warning');
const stockStatusClass = (status) => ({ CONSISTENT: 'stock-tag--consistent', REPAIRED: 'stock-tag--status-repaired', INCONSISTENT: 'stock-tag--inconsistent', ABNORMAL: 'stock-tag--abnormal', IGNORED: 'stock-tag--ignored' }[status] || '');
const stockSeverityLabel = (severity) => ({ HIGH: '高', MEDIUM: '中', LOW: '低', WARNING: '预警', NONE: '无' }[severity] || severity || '-');
const stockSeverityType = (severity) => ({ HIGH: 'danger', MEDIUM: 'warning', LOW: 'info', WARNING: 'warning', NONE: 'info' }[severity] || 'info');
const stockRepairStatusLabel = (status) => ({ DONE: '已处理', REPAIRED: '已修复', PENDING: '待处理', IGNORED: '已忽略', NONE: '无需处理' }[status] || status || '-');
const stockRepairStatusType = (status) => ({ DONE: 'warning', REPAIRED: 'success', PENDING: 'warning', IGNORED: 'info', NONE: 'info' }[status] || 'info');
const stockRepairStatusClass = (status) => ({ DONE: 'stock-tag--repair-done', REPAIRED: 'stock-tag--repair-repaired', PENDING: 'stock-tag--repair-pending', IGNORED: 'stock-tag--ignored', NONE: 'stock-tag--none' }[status] || '');
const snapshotValue = (snapshot, field) => {
  const value = snapshot?.[field];
  return value === null || value === undefined ? '-' : value;
};
const stockDifferenceText = (row) => (row?.differences || []).join('；') || '-';
const isStockDifferenceOverflow = (row) => stockDifferenceText(row).length > 34;
const formatStockCheckedAt = (value) => value ? String(value).replace('T', ' ') : '-';
const mockToolboxTasks = computed(() => tasks.value.filter((task) => task.channel === 'MOCK' && !isTaskArchived(task)));
const isMockTask = (task) => String(task?.channel || '').toUpperCase() === 'MOCK';
const isAlipayTask = (task) => String(task?.channel || '').toUpperCase() === 'ALIPAY';
const isMockDiff = (detail) => detail?.channelBill?.channel === 'MOCK' || detail?.localBill?.channel === 'MOCK' || currentTask.value?.channel === 'MOCK';

const loadTasks = async () => {
  taskLoading.value = true;
  try {
    const selectedId = currentTask.value?.id;
    const { data } = await fetchAdminOnlineReconcileTasks({ ...taskQuery, page: taskPager.page, size: taskPager.size });
    tasks.value = data.data?.records || [];
    taskPager.total = data.data?.total || 0;
    if (tasks.value.length) {
      const targetTask = tasks.value.find((task) => task.id === selectedId)
        || (route.query.processStatus === 'PENDING'
          ? tasks.value.find((task) => Number(task.pendingCount || 0) > 0) || tasks.value[0]
          : tasks.value[0]);
      await selectTask(targetTask);
    } else {
      currentTask.value = null;
      diffRows.value = [];
      localRows.value = [];
      channelRows.value = [];
      diffPager.total = 0;
      localPager.total = 0;
      channelPager.total = 0;
    }
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '对账任务加载失败');
  } finally {
    taskLoading.value = false;
  }
};
const resetTasks = () => { taskQuery.status = ''; taskQuery.channel = ''; taskPager.page = 1; loadTasks(); };
const applyTaskUpdate = (task) => {
  if (!task?.id) return;
  const index = tasks.value.findIndex((item) => item.id === task.id);
  if (index >= 0) tasks.value.splice(index, 1, task);
  if (currentTask.value?.id === task.id || !currentTask.value) currentTask.value = task;
};
const selectTask = async (row) => {
  if (!row) return;
  currentTask.value = { ...row };
  diffPager.page = 1;
  localPager.page = 1;
  channelPager.page = 1;
  await loadAllDetailTables();
};
const refreshSelectedTask = async () => {
  const selectedId = currentTask.value?.id;
  await loadTasks();
  const latest = tasks.value.find((item) => item.id === selectedId);
  if (latest) currentTask.value = { ...latest };
};
const openCreateTask = () => { createVisible.value = true; };
const submitCreateTask = async () => {
  if (!createForm.reconcileDate) {
    ElMessage.warning('请选择对账日期');
    return;
  }
  if (createDateDuplicated.value) {
    ElMessage.warning('该对账日期已存在任务，请选择其它日期');
    return;
  }
  try {
    const { data } = await createAdminOnlineReconcileTask(createForm);
    createVisible.value = false;
    ElMessage.success('对账任务已创建');
    await loadTasks();
    await selectTask(data.data);
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '创建对账任务失败');
  }
};
const generateLocal = async (row) => {
  try {
    await confirmAction(`确认重新生成任务 ${row.taskNo} 的本地账单快照吗？已有本地账单和差异将被覆盖。`);
    const { data } = await generateAdminOnlineLocalBills(row.id);
    applyTaskUpdate(data.data);
    ElMessage.success('本地账单已生成');
    await loadAllDetailTables();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || '生成本地账单失败，请确认是否存在该日期的支付单');
  }
};
const generateMock = async (row) => {
  try {
    await confirmAction(`确认生成任务 ${row.taskNo} 的 Mock 渠道账单吗？将覆盖已有渠道账单和差异。`);
    const { data } = await generateAdminOnlineMockChannelBills(row.id, { mode: 'FULL_TEST' });
    applyTaskUpdate(data.data);
    ElMessage.success('Mock渠道账单已生成');
    await loadAllDetailTables();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || '生成Mock渠道账单失败');
  }
};
const uploadAlipayCsv = async (row, file) => {
  try {
    const { data } = await uploadAdminAlipayChannelBills(row.id, file);
    applyTaskUpdate(data.data);
    ElMessage.success('支付宝渠道账单已上传');
    if (currentTask.value?.id === row.id) await loadAllDetailTables();
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '上传支付宝账单失败');
  }
  return false;
};
const matchTask = async (row) => {
  try {
    const message = isMockTask(row)
      ? `确认对任务 ${row.taskNo} 执行自动勾兑吗？Mock 渠道下进行自动勾兑会在后台生成部分对账异常信息，用于模拟对账出现差异的情况。`
      : `确认对任务 ${row.taskNo} 执行自动勾兑吗？`;
    await confirmAction(message);
    const { data } = await matchAdminOnlineReconcileTask(row.id);
    applyTaskUpdate(data.data);
    ElMessage.success('自动勾兑完成');
    detailTab.value = 'diffs';
    await loadAllDetailTables();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || '自动勾兑失败');
  }
};
const completeTask = async (row) => {
  try {
    await confirmAction(`确认归档任务 ${row.taskNo} 吗？归档后将不能重新生成账单、勾兑或处理差异。`);
    const { data } = await completeAdminOnlineReconcileTask(row.id, { remark: '后台人工确认对账完成' });
    applyTaskUpdate(data.data);
    ElMessage.success('对账任务已归档');
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || '归档任务失败');
  }
};
const resetDiffHandleForm = () => {
  diffHandleForm.reason = '';
  diffHandleForm.judgement = '';
  diffHandleForm.diffAmount = '';
  diffHandleForm.remark = '';
};
const openDiffDetail = async (row) => {
  try {
    resetDiffHandleForm();
    const { data } = await fetchAdminOnlineDiffItem(row.id);
    diffDetail.value = data.data;
    diffDetailVisible.value = true;
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '加载差异详情失败');
  }
};
const diffOrderNo = computed(() => diffDetail.value?.diff?.orderNo || diffDetail.value?.localBill?.orderNo || diffDetail.value?.channelBill?.orderNo || '');
const diffPayKeyword = computed(() => diffDetail.value?.diff?.payOrderNo || diffDetail.value?.localBill?.payOrderNo || diffDetail.value?.channelBill?.payOrderNo || diffOrderNo.value || '');
const diffRefundNo = computed(() => diffDetail.value?.diff?.refundNo || diffDetail.value?.localBill?.refundNo || diffDetail.value?.channelBill?.refundNo || '');
const diffCallbackKeyword = computed(() => diffDetail.value?.diff?.bizType === 'REFUND' ? (diffRefundNo.value || diffPayKeyword.value) : diffPayKeyword.value);
const systemRecognitionTitle = computed(() => {
  const diff = diffDetail.value?.diff;
  if (!diff) return '系统识别结论';
  return `系统识别：${diff.bizType === 'REFUND' ? '退款' : '支付'} ${diffTypeLabel(diff.diffType)}`;
});
const systemRecognitionDescription = computed(() => {
  const diff = diffDetail.value?.diff;
  if (!diff) return '';
  const local = reconcileStatusLabel(diff.localStatus);
  const channel = reconcileStatusLabel(diff.channelStatus);
  return `本地状态：${local}；渠道状态：${channel}；本地金额：${formatCent(diff.localAmountCent)} 元；渠道金额：${formatCent(diff.channelAmountCent)} 元。请先核查订单、支付、退款与回调记录，再确认原因并提交处理。`;
});
const channelCheckTips = computed(() => {
  const diff = diffDetail.value?.diff;
  if (!diff || diff.diffType !== 'LOCAL_EXISTS_CHANNEL_MISSING') return [];
  if (diff.bizType === 'REFUND') {
    return ['请先确认渠道账单日期是否覆盖退款成功日期', '请确认退款是否存在隔日入账或渠道账单延迟', '请确认渠道后台是否能查到退款流水', '请核对本地退款成功状态是否误更新'];
  }
  return ['请先确认渠道账单日期是否正确', '请确认渠道账单是否存在延迟', '请在渠道后台核对是否真实收款', '请结合回调记录判断是否回调丢失或处理异常'];
});
const recommendedReason = computed(() => {
  const diff = diffDetail.value?.diff;
  if (!diff) return '';
  if (diff.bizType === 'REFUND' && diff.diffType === 'LOCAL_EXISTS_CHANNEL_MISSING') return '渠道退款延迟入账 / 渠道账单日期不一致';
  if (diff.bizType === 'REFUND' && ['REFUND_STATUS_MISMATCH', 'STATUS_MISMATCH'].includes(diff.diffType)) return '渠道退款结果未同步 / 本地退款状态待修正';
  if (diff.bizType === 'PAY' && diff.diffType === 'LOCAL_EXISTS_CHANNEL_MISSING') return ['PAID', 'PAY_SUCCESS', 'TRADE_SUCCESS'].includes(diff.localStatus) ? '渠道账单延迟 / 回调状态需核验' : '用户未支付 / 支付超时';
  if (diff.diffType === 'CHANNEL_EXISTS_LOCAL_MISSING') return '真实收款未入库 / 重复回调 / 测试流水';
  if (['AMOUNT_MISMATCH', 'STATUS_AND_AMOUNT_MISMATCH', 'REFUND_AMOUNT_MISMATCH'].includes(diff.diffType)) return '费率差异 / 长短款 / 金额同步异常';
  if (['DUPLICATE_LOCAL', 'DUPLICATE_CHANNEL'].includes(diff.diffType)) return '重复流水 / 重复回调';
  return '';
});
const diffHandleSchema = computed(() => {
  const diff = diffDetail.value?.diff;
  if (!diff) return null;
  if (diff.diffType === 'LOCAL_EXISTS_CHANNEL_MISSING') {
    if (diff.bizType === 'REFUND') {
      return { title: '退款漏账处理：本地有退款，渠道无退款流水', description: '优先核查渠道退款账单日期、渠道退款流水和本地退款状态，无法确认时登记挂账继续跟进。', reasons: ['渠道退款延迟入账', '渠道退款失败 / 未受理', '本地退款状态误更新', '渠道账单日期不一致', '其他异常'] };
    }
    return { title: '支付漏单处理：本地有单，渠道无流水', description: '先查看本地订单与支付记录，确认无收款后关闭订单/作废；无法确认时登记挂账继续跟进。', reasons: ['用户未支付 / 支付超时', '回调丢失 / 渠道入账延迟', '本地支付状态误更新', '渠道账单日期不一致', '其他异常'] };
  }
  if (diff.diffType === 'CHANNEL_EXISTS_LOCAL_MISSING') {
    return { title: '单边账处理：渠道有流水，本地无订单', description: '核对渠道流水真实性，测试流水可作废，真实收款建议补录交易，重复回调登记冲账。', judgements: ['测试流水', '真实收款', '重复回调'] };
  }
  if (['AMOUNT_MISMATCH', 'STATUS_AND_AMOUNT_MISMATCH', 'REFUND_AMOUNT_MISMATCH'].includes(diff.diffType)) {
    return { title: '金额/手续费不符处理', description: '核对签约手续费规则和资金实收，登记长短款后提交财务调账或临时挂账。', judgements: ['正常扣费', '长款', '短款'], showAmount: true };
  }
  if (diff.bizType === 'REFUND' || ['REFUND_STATUS_MISMATCH', 'REFUND_AMOUNT_MISMATCH'].includes(diff.diffType)) {
    return { title: '退款异常处理', description: '核对本地退款申请记录和渠道退款结果，必要时重试退款或修正本地退款状态。', reasons: ['渠道退款延迟入账', '渠道退款失败 / 未受理', '本地退款状态误更新', '渠道账单日期不一致', '其他异常'] };
  }
  if (['DUPLICATE_LOCAL', 'DUPLICATE_CHANNEL'].includes(diff.diffType)) {
    return { title: '重复交易/重复流水处理', description: '选择保留有效流水，重复流水标记作废；若确认重复扣款，可发起线上退款申请。', judgements: ['保留当前流水', '重复作废', '重复扣款需退款'] };
  }
  return { title: '人工复核处理', description: '请根据本地账单、渠道账单和业务上下文填写处理依据。' };
});
const openAuditRoute = (path, query) => {
  const route = router.resolve({ path, query });
  window.open(route.href, '_blank');
};
const goOrderDetail = () => {
  if (!diffOrderNo.value) return;
  openAuditRoute('/orders', { keyword: diffOrderNo.value });
};
const goPayRecord = () => {
  if (!diffPayKeyword.value) return;
  openAuditRoute('/pays', { keyword: diffPayKeyword.value });
};
const goRefundRecord = () => {
  if (!diffRefundNo.value) return;
  openAuditRoute('/pays', { refundNo: diffRefundNo.value, keyword: diffOrderNo.value || diffPayKeyword.value });
};
const goCallbackRecord = () => {
  if (!diffPayKeyword.value) return;
  openAuditRoute('/pays', { keyword: diffPayKeyword.value, callbackKeyword: diffPayKeyword.value, openCallbacks: '1' });
};
const buildDiffRemark = () => {
  const parts = [];
  if (recommendedReason.value) parts.push(`系统推荐原因：${recommendedReason.value}`);
  if (diffHandleForm.reason) parts.push(`判定原因：${diffHandleForm.reason}`);
  if (diffHandleForm.judgement) parts.push(`判定类型：${diffHandleForm.judgement}`);
  if (diffHandleForm.diffAmount) parts.push(`差异金额：${diffHandleForm.diffAmount}元`);
  if (diffHandleForm.remark) parts.push(`备注：${diffHandleForm.remark}`);
  return parts.join('；');
};
const buildActionPayloadRemark = () => buildDiffRemark() || '运营处理';
const normalizeLogContent = (content, operationType) => {
  if (!content) return actionMeta(operationType).summary;
  return String(content)
    .replace(/动作=([A-Z_]+)/g, (_, action) => `动作=${suggestedActionLabel(action)}`)
    .replace(/类型=PAY/g, '类型=支付')
    .replace(/类型=REFUND/g, '类型=退款')
    .replace(/差异=([A-Z_]+)/g, (_, type) => `差异=${diffTypeLabel(type)}`);
};
const formatLogTime = (value) => value ? String(value).replace('T', ' ') : '-';
const isFinalHandledDiff = (diff) => !!diff && diff.processStatus !== 'PENDING';
const placeholderActions = ['SYNC_PAY_STATUS', 'RETRY_REFUND', 'SUBMIT_FINANCE_ADJUSTMENT', 'MANUAL_SUPPLEMENT_TRADE'];
const placeholderActionTip = (action) => ({
  SYNC_PAY_STATUS: '正式环境动作占位：后续由后端支付服务执行同步，本阶段不直接操作主数据',
  RETRY_REFUND: '正式环境动作占位：后续由退款服务执行重试，本阶段不直接操作退款主数据',
  SUBMIT_FINANCE_ADJUSTMENT: '正式环境动作占位：后续应生成财务调账工单，本阶段不直接调账',
  MANUAL_SUPPLEMENT_TRADE: '正式环境动作占位：后续应生成补录工单，本阶段不直接补录交易',
}[action] || '正式环境动作占位，本阶段不执行');
const canExecuteAction = (action) => !action.placeholder && (action.repair ? canRepairDiff.value : canHandleDiff.value);
const diffActions = (diff) => {
  if (!diff) return [];
  if (diff.processStatus === 'HANGING') return [];
  const handled = isFinalHandledDiff(diff);
  const disabledTip = handled ? '该差异已进入后续流程，不能在详情页重复处理' : '';
  const actions = [];
  const pushAction = (value, extra = {}) => actions.push({ value, ...actionMeta(value), plain: extra.plain ?? true, placeholder: extra.placeholder || false, repair: extra.repair || false, disabledTip: handled ? disabledTip : (extra.placeholder ? placeholderActionTip(value) : '') });
  const mock = isMockDiff(diffDetail.value);
  if (mock) {
    if (diff.diffType === 'CHANNEL_EXISTS_LOCAL_MISSING') {
      pushAction('MARK_TEST_FLOW_VOID');
    }
    pushAction('HANG');
    pushAction('MARK_DONE', { plain: false });
    return actions;
  }
  pushAction('MARK_DONE', { plain: false });
  pushAction('HANG');
  pushAction('SYNC_PAY_STATUS', { placeholder: true, repair: true });
  pushAction('RETRY_REFUND', { placeholder: true, repair: true });
  pushAction('SUBMIT_FINANCE_ADJUSTMENT', { placeholder: true, repair: true });
  pushAction('MANUAL_SUPPLEMENT_TRADE', { placeholder: true, repair: true });
  return actions;
};
const finalJudgementActions = ['MARK_TEST_FLOW_VOID', 'MARK_DONE', 'HANG'];
const validateDiffAction = (action) => {
  if (isFinalHandledDiff(diffDetail.value?.diff)) {
    ElMessage.warning(diffDetail.value?.diff?.processStatus === 'HANGING' ? '该差异已挂账，请到挂账跟进页继续处理，不能二次挂账或二次处理' : '该差异已进入后续流程，不能重复执行');
    return false;
  }
  if (finalJudgementActions.includes(action)) {
    if (diffHandleSchema.value?.reasons?.length && !diffHandleForm.reason) {
      ElMessage.warning('请先选择判定原因');
      return false;
    }
    if (diffHandleSchema.value?.judgements?.length && !diffHandleForm.judgement) {
      ElMessage.warning('请先选择判定类型');
      return false;
    }
    if (diffHandleSchema.value?.showAmount && !diffHandleForm.diffAmount) {
      ElMessage.warning('请先填写差异金额');
      return false;
    }
    if (!diffHandleForm.remark?.trim()) {
      ElMessage.warning('请填写处理备注，说明处理依据');
      return false;
    }
  }
  return true;
};
const handleDiffAction = async (action) => {
  if (!diffDetail.value?.diff || !validateDiffAction(action)) return;
  try {
    await confirmAction(`确认执行处理动作「${suggestedActionLabel(action)}」吗？`);
    diffHandling.value = true;
    const { data } = await handleAdminOnlineDiffItem(diffDetail.value.diff.id, { action, remark: buildActionPayloadRemark() });
    diffDetail.value = data.data;
    resetDiffHandleForm();
    ElMessage.success('差异处理完成');
    await refreshSelectedTask();
    await loadDiffs();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || '处理差异失败');
  } finally {
    diffHandling.value = false;
  }
};
const loadAllDetailTables = async () => {
  await Promise.all([loadDiffs(), loadLocals(), loadChannels()]);
};
const loadDetailTab = async () => {
  if (!currentTask.value) return;
  if (detailTab.value === 'locals') return loadLocals();
  if (detailTab.value === 'channels') return loadChannels();
  return loadDiffs();
};
const loadLocals = async () => {
  if (!currentTask.value) return;
  localLoading.value = true;
  try {
    const { data } = await fetchAdminOnlineLocalBills(currentTask.value.id, { page: localPager.page, size: localPager.size });
    localRows.value = data.data?.records || [];
    localPager.total = data.data?.total || 0;
  } finally { localLoading.value = false; }
};
const loadChannels = async () => {
  if (!currentTask.value) return;
  channelLoading.value = true;
  try {
    const { data } = await fetchAdminOnlineChannelBills(currentTask.value.id, { page: channelPager.page, size: channelPager.size });
    channelRows.value = data.data?.records || [];
    channelPager.total = data.data?.total || 0;
  } finally { channelLoading.value = false; }
};
const loadDiffs = async () => {
  if (!currentTask.value) return;
  diffLoading.value = true;
  try {
    const { data } = await fetchAdminOnlineDiffItems(currentTask.value.id, { ...diffQuery, page: diffPager.page, size: diffPager.size });
    diffRows.value = data.data?.records || [];
    diffPager.total = data.data?.total || 0;
  } finally { diffLoading.value = false; }
};
const refreshCurrent = async () => {
  if (mainTab.value === 'stock') {
    await loadStockReconciliations();
    return;
  }
  if (mainTab.value === 'hanging') {
    await loadHangingFollows();
    return;
  }
  if (mainTab.value === 'archive') {
    await loadArchiveReport();
    return;
  }
  await loadTasks();
  await loadDetailTab();
};
const handleMainTabChange = async (tabName) => {
  const nextTab = normalizeMainTab(tabName);
  if (nextTab !== mainTab.value) {
    mainTab.value = nextTab;
  }
  if (nextTab === 'stock') {
    applyStockReconciliationRoute();
    await loadStockReconciliations();
  } else if (nextTab === 'hanging') {
    await loadHangingFollows();
  } else if (nextTab === 'archive') {
    await loadArchiveReport();
  } else if (!tasks.value.length) {
    await loadTasks();
  }
};
const handleTaskPageChange = (page) => { taskPager.page = page; loadTasks(); };
const handleTaskSizeChange = (size) => { taskPager.size = size; taskPager.page = 1; loadTasks(); };
const handleLocalPageChange = (page) => { localPager.page = page; loadLocals(); };
const handleLocalSizeChange = (size) => { localPager.size = size; localPager.page = 1; loadLocals(); };
const handleChannelPageChange = (page) => { channelPager.page = page; loadChannels(); };
const handleChannelSizeChange = (size) => { channelPager.size = size; channelPager.page = 1; loadChannels(); };
const handleDiffPageChange = (page) => { diffPager.page = page; loadDiffs(); };
const handleDiffSizeChange = (size) => { diffPager.size = size; diffPager.page = 1; loadDiffs(); };
const canSelectDiff = (row) => canHandleDiff.value && ['PENDING', 'HANGING'].includes(row?.processStatus);
const handleDiffSelectionChange = (rows) => {
  selectedDiffRows.value = rows.filter((row) => canSelectDiff(row));
};
const batchHandleDiffs = async (action, successText) => {
  const rows = selectedDiffRows.value;
  if (!rows.length) return;
  try {
    await confirmAction(`确认对已选 ${rows.length} 条差异执行「${suggestedActionLabel(action)}」吗？`);
    for (const row of rows) {
      await handleAdminOnlineDiffItem(row.id, { action, remark: `批量操作：${suggestedActionLabel(action)}` });
    }
    ElMessage.success(successText.replace('{count}', rows.length));
    selectedDiffRows.value = [];
    await refreshSelectedTask();
    await loadDiffs();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || '批量处理差异失败');
  }
};
const batchMarkDiffsDone = () => batchHandleDiffs('MARK_DONE', '已批量标记 {count} 条差异为已处理');
const batchHangDiffs = () => batchHandleDiffs('HANG', '已批量挂账 {count} 条差异');
const downloadExcel = (filename, sheets) => {
  const workbook = XLSX.utils.book_new();
  sheets.forEach(({ name, headers, rows }) => {
    const worksheet = XLSX.utils.aoa_to_sheet([headers, ...rows]);
    worksheet['!cols'] = headers.map((header, index) => ({
      wch: Math.max(String(header).length + 4, ...rows.map((row) => String(row[index] ?? '').length + 2), 12),
    }));
    XLSX.utils.book_append_sheet(workbook, worksheet, name.slice(0, 31));
  });
  XLSX.writeFile(workbook, filename.endsWith('.xlsx') ? filename : `${filename}.xlsx`);
};
const exportDiffRows = () => {
  const rows = diffRows.value || [];
  if (!rows.length) {
    ElMessage.warning('暂无可导出的差异数据');
    return;
  }
  const headers = ['订单号', '支付单号', '退款单号', '业务类型', '差异类型', '本地状态', '渠道状态', '本地金额', '渠道金额', '建议动作', '处理状态'];
  const excelRows = rows.map((row) => [row.orderNo, row.payOrderNo, row.refundNo, row.bizType === 'REFUND' ? '退款' : '支付', diffTypeLabel(row.diffType), reconcileStatusLabel(row.localStatus), reconcileStatusLabel(row.channelStatus), formatCent(row.localAmountCent), formatCent(row.channelAmountCent), suggestedActionLabel(row.suggestedAction), processStatusLabel(row.processStatus)]);
  downloadExcel(`对账差异清单-${currentTask.value?.taskNo || '未选择任务'}.xlsx`, [{ name: '差异清单', headers, rows: excelRows }]);
};
const loadHangingFollows = async () => {
  hangingLoading.value = true;
  try {
    const { data } = await fetchAdminOnlineHangingFollows({ ...hangingQuery, page: hangingPager.page, size: hangingPager.size });
    hangingRows.value = data.data?.records || [];
    hangingPager.total = data.data?.total || 0;
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '挂账跟进列表加载失败');
  } finally {
    hangingLoading.value = false;
  }
};
const resetHangingFollows = () => { hangingQuery.keyword = ''; hangingQuery.riskLevel = ''; hangingPager.page = 1; loadHangingFollows(); };
const handleHangingPageChange = (page) => { hangingPager.page = page; loadHangingFollows(); };
const handleHangingSizeChange = (size) => { hangingPager.size = size; hangingPager.page = 1; loadHangingFollows(); };
const openHangingDetail = (row) => openDiffDetail({ id: row.diffId });
const openFollowDialog = (row) => { currentHanging.value = row; hangingFollowForm.remark = ''; hangingFollowVisible.value = true; };
const submitHangingFollow = async () => {
  if (!currentHanging.value) return;
  if (!hangingFollowForm.remark) {
    ElMessage.warning('请输入跟进内容');
    return;
  }
  try {
    await followAdminOnlineHangingDiff(currentHanging.value.diffId, { remark: hangingFollowForm.remark });
    ElMessage.success('跟进记录已新增');
    hangingFollowVisible.value = false;
    await loadHangingFollows();
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '新增跟进失败');
  }
};
const transferHangingToFinance = async (row) => {
  try {
    await confirmAction(`确认将差异 #${row.diffId} 标记为需财务处理吗？当前阶段仅做对账侧留痕和标记，不会生成真实财务调账单。`);
    await transferAdminOnlineHangingDiffToFinance(row.diffId, { remark: '挂账跟进页标记需财务处理（仅留痕，不生成真实调账单）' });
    ElMessage.success('已标记需财务处理');
    await loadHangingFollows();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || '标记需财务处理失败');
  }
};
const refreshAfterHangingChanged = async () => {
  await loadHangingFollows();
  await refreshSelectedTask();
  if (currentTask.value && detailTab.value === 'diffs') {
    await loadDiffs();
  }
};
const closeHanging = async (row) => {
  try {
    await confirmAction(`确认人工完结差异 #${row.diffId} 的挂账闭环吗？`);
    await closeAdminOnlineHangingDiff(row.diffId, { remark: '挂账跟进页人工确认完结闭环' });
    ElMessage.success('挂账差异已人工完结');
    await refreshAfterHangingChanged();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || '完结挂账失败');
  }
};
const archiveParams = () => {
  const [startDate, endDate] = archiveQuery.range || [];
  return {
    startDate,
    endDate,
    channel: archiveQuery.channel || undefined,
    bizType: archiveQuery.bizType || undefined,
    diffType: archiveQuery.diffType || undefined,
  };
};
const archiveDateLabel = (task) => task?.reconcileDate || (archiveQuery.range?.length ? archiveQuery.range.join('_至_') : toLocalDateKey(new Date()));
const loadArchiveReport = async () => {
  archiveLoading.value = true;
  try {
    const { data } = await fetchAdminOnlineArchiveReport(archiveParams());
    archiveReport.value = data.data || { diffTypeStats: [], tasks: [] };
    archiveSelectedTasks.value = [];
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '报表归档数据加载失败');
  } finally {
    archiveLoading.value = false;
  }
};
const resetArchiveQuery = () => {
  archiveQuery.range = [];
  archiveQuery.channel = '';
  archiveQuery.bizType = '';
  archiveQuery.diffType = '';
  loadArchiveReport();
};
const handleArchiveSelectionChange = (rows) => {
  archiveSelectedTasks.value = rows || [];
};
const exportArchiveReport = () => {
  const rows = archiveReport.value?.tasks || [];
  if (!rows.length) {
    ElMessage.warning('暂无可导出的归档任务');
    return;
  }
  const headers = ['任务号', '账期', '渠道', '状态', '本地笔数', '渠道笔数', '本地金额', '渠道金额', '匹配数', '差异数', '待处理数', '挂账数', '归档时间'];
  const excelRows = rows.map((row) => [row.taskNo, row.reconcileDate, row.channel, taskStatusMeta(row.status).label, row.localTotalCount || 0, row.channelTotalCount || 0, formatCent(row.localTotalAmountCent), formatCent(row.channelTotalAmountCent), row.matchedCount || 0, row.diffCount || 0, row.pendingCount || 0, row.hangCount || 0, row.completedAt || '']);
  downloadExcel(`${archiveDateLabel()}_对账任务汇总表.xlsx`, [{ name: '任务汇总', headers, rows: excelRows }]);
};
const loadArchiveTaskData = async (task) => {
  const [locals, channels, diffs, logs] = await Promise.all([
    fetchAdminOnlineLocalBills(task.id, { page: 1, size: 10000 }),
    fetchAdminOnlineChannelBills(task.id, { page: 1, size: 10000 }),
    fetchAdminOnlineDiffItems(task.id, { page: 1, size: 10000 }),
    fetchAdminOnlineTaskLogs(task.id, { page: 1, size: 10000 }),
  ]);
  return {
    locals: locals.data.data?.records || [],
    channels: channels.data.data?.records || [],
    diffs: diffs.data.data?.records || [],
    logs: logs.data.data?.records || [],
  };
};
const buildArchiveTaskSheets = async (task) => {
  const { locals, channels, diffs, logs } = await loadArchiveTaskData(task);
  const channelByKey = new Map(channels.map((row) => [`${row.bizType}|${row.payOrderNo || row.outTradeNo || ''}|${row.refundNo || ''}`, row]));
  const summaryHeaders = ['账期', '任务号', '业务类型', '订单号', '支付单号', '退款单号', '本地状态', '渠道状态', '本地金额', '渠道金额', '差异类型', '处理状态'];
  const localRows = locals.map((local) => {
    const channel = channelByKey.get(`${local.bizType}|${local.payOrderNo || ''}|${local.refundNo || ''}`) || {};
    const diff = diffs.find((item) => item.localItemId === local.id || (item.payOrderNo === local.payOrderNo && item.refundNo === local.refundNo));
    return [task.reconcileDate, task.taskNo, local.bizType, local.orderNo, local.payOrderNo, local.refundNo, reconcileStatusLabel(local.localStatus), reconcileStatusLabel(channel.channelStatus), formatCent(local.amountCent), formatCent(channel.amountCent), diffTypeLabel(diff?.diffType || 'MATCHED'), processStatusLabel(diff?.processStatus || 'DONE')];
  });
  const channelOnlyRows = channels.filter((channel) => !locals.some((local) => local.bizType === channel.bizType && local.payOrderNo === channel.payOrderNo && local.refundNo === channel.refundNo)).map((channel) => {
    const diff = diffs.find((item) => item.channelItemId === channel.id || (item.payOrderNo === channel.payOrderNo && item.refundNo === channel.refundNo));
    return [task.reconcileDate, task.taskNo, channel.bizType, channel.orderNo, channel.payOrderNo || channel.outTradeNo, channel.refundNo, '-', reconcileStatusLabel(channel.channelStatus), '-', formatCent(channel.amountCent), diffTypeLabel(diff?.diffType || 'CHANNEL_EXISTS_LOCAL_MISSING'), processStatusLabel(diff?.processStatus)];
  });
  const diffHeaders = ['账期', '任务号', '业务类型', '差异类型', '订单号', '支付单号', '退款单号', '本地状态', '渠道状态', '本地金额', '渠道金额', '差异金额', '建议动作', '处理状态', '处理人', '处理时间', '处理备注'];
  const diffRows = diffs.filter((row) => row.diffType !== 'MATCHED').map((row) => [task.reconcileDate, task.taskNo, row.bizType, diffTypeLabel(row.diffType), row.orderNo, row.payOrderNo, row.refundNo, reconcileStatusLabel(row.localStatus), reconcileStatusLabel(row.channelStatus), formatCent(row.localAmountCent), formatCent(row.channelAmountCent), formatCent(row.diffAmountCent), suggestedActionLabel(row.suggestedAction), processStatusLabel(row.processStatus), row.processedBy || '', row.processedAt || '', row.processRemark || '']);
  const hangingHeaders = ['账期', '任务号', '差异ID', '差异类型', '订单号', '支付单号', '退款单号', '差异金额', '处理状态', '处理备注', '调账/跟进记录'];
  const hangingRows = diffs.filter((row) => row.processStatus === 'HANGING' || ['HANG', 'TRANSFER_FINANCE', 'CLOSE_HANGING', 'FOLLOW_UP', 'SUBMIT_FINANCE_ADJUSTMENT', 'REGISTER_AMOUNT_ADJUSTMENT'].includes(row.suggestedAction)).map((row) => {
    const relatedLogs = logs.filter((log) => log.diffItemId === row.id).map((log) => `${log.createdAt || ''} ${log.operatorName || ''} ${log.operationType || ''} ${log.remark || log.operationContent || ''}`).join('；');
    return [task.reconcileDate, task.taskNo, row.id, diffTypeLabel(row.diffType), row.orderNo, row.payOrderNo, row.refundNo, formatCent(row.diffAmountCent), processStatusLabel(row.processStatus), row.processRemark || '', relatedLogs];
  });
  const logHeaders = ['账期', '任务号', '操作类型', '差异ID', '操作内容', '操作人', '备注', '操作时间'];
  const logRows = logs.map((row) => [task.reconcileDate, task.taskNo, suggestedActionLabel(row.operationType) || row.operationType, row.diffItemId || '', row.operationContent || '', row.operatorName || '', row.remark || '', row.createdAt || '']);
  return [
    { name: '全量对账总表', headers: summaryHeaders, rows: [...localRows, ...channelOnlyRows] },
    { name: '差异处理清单', headers: diffHeaders, rows: diffRows },
    { name: '挂账调账记录', headers: hangingHeaders, rows: hangingRows },
    { name: '操作日志', headers: logHeaders, rows: logRows },
  ];
};
const exportArchiveTaskSummary = async (task) => {
  const sheets = await buildArchiveTaskSheets(task);
  downloadExcel(`${task.reconcileDate}_${task.taskNo}_全量对账总表.xlsx`, [sheets[0]]);
};
const exportArchiveTaskDiffs = async (task) => {
  const sheets = await buildArchiveTaskSheets(task);
  downloadExcel(`${task.reconcileDate}_${task.taskNo}_差异处理清单.xlsx`, [sheets[1]]);
};
const exportArchiveTaskPackage = async (task) => {
  try {
    const sheets = await buildArchiveTaskSheets(task);
    downloadExcel(`${task.reconcileDate}_${task.taskNo}_对账归档包.xlsx`, sheets);
    ElMessage.success('归档包 Excel 已导出');
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '归档包导出失败');
  }
};
const exportSelectedArchivePackage = async () => {
  const rows = archiveSelectedTasks.value;
  if (!rows.length) return;
  for (const task of rows) {
    await exportArchiveTaskPackage(task);
  }
};
const openTaskLogs = async (row) => {
  try {
    const { data } = await fetchAdminOnlineTaskLogs(row.id, { page: 1, size: 100 });
    taskLogs.value = data.data?.records || [];
    taskLogsVisible.value = true;
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '任务操作日志加载失败');
  }
};
const syncStockReconciliationRoute = async () => {
  await router.replace({
    path: '/reconciliations',
    query: {
      tab: 'stock',
      ...(stockQuery.status ? { stockStatus: stockQuery.status } : {}),
      ...(stockQuery.skuId ? { stockSkuId: String(stockQuery.skuId) } : {}),
      ...(stockQuery.sortBy ? { stockSortBy: stockQuery.sortBy } : {}),
      ...(stockQuery.sortOrder ? { stockSortOrder: stockQuery.sortOrder } : {}),
      ...(stockPager.page > 1 ? { stockPage: String(stockPager.page) } : {}),
      ...(stockPager.size !== ADMIN_PAGE_SIZE ? { stockSize: String(stockPager.size) } : {}),
    },
  });
};
const applyStockReconciliationRoute = () => {
  stockQuery.skuId = String(route.query.stockSkuId ?? '');
  stockQuery.status = normalizeStockStatusQuery(route.query.stockStatus);
  stockQuery.sortBy = String(route.query.stockSortBy || 'id');
  stockQuery.sortOrder = String(route.query.stockSortOrder || 'desc');
  stockPager.page = Number(route.query.stockPage || 1);
  stockPager.size = Number(route.query.stockSize || ADMIN_PAGE_SIZE);
};
const loadStockReconciliations = async () => {
  stockLoading.value = true;
  try {
    const params = {
      status: stockQuery.status || undefined,
      skuId: stockQuery.skuId || undefined,
      sortBy: stockQuery.sortBy,
      sortOrder: stockQuery.sortOrder,
      page: stockPager.page,
      size: stockPager.size,
    };
    const { data } = await fetchAdminStockReconciliations(params);
    stockRows.value = data.data?.records || [];
    stockPager.total = data.data?.total || 0;
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '库存对账记录加载失败');
  } finally {
    stockLoading.value = false;
  }
};
const searchStockReconciliations = async () => {
  stockPager.page = 1;
  selectedStockRows.value = [];
  await syncStockReconciliationRoute();
  loadStockReconciliations();
};
const resetStockReconciliations = async () => {
  stockQuery.skuId = '';
  stockQuery.status = '';
  stockQuery.sortBy = 'id';
  stockQuery.sortOrder = 'desc';
  stockPager.page = 1;
  selectedStockRows.value = [];
  await syncStockReconciliationRoute();
  loadStockReconciliations();
};
const canManageStockReconcile = (row) => {
  if (!adminStore.hasPermission('stock:reconcile:repair') || !row) return false;
  const inconsistent = ['INCONSISTENT', 'ABNORMAL'].includes(row.status);
  const pending = !row.repairStatus || ['PENDING', 'NONE'].includes(row.repairStatus);
  return inconsistent && pending;
};
const repairStock = async (row) => {
  try {
    await confirmAction(`确认修复库存对账记录 #${row.id} 吗？`);
    await repairAdminStockReconciliation(row.id, { remark: '库存对账页触发单条修复' });
    ElMessage.success('库存对账记录已修复');
    await loadStockReconciliations();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || '修复库存对账失败');
  }
};
const handleStockSelectionChange = (rows) => {
  selectedStockRows.value = rows.filter((row) => canManageStockReconcile(row));
};
const batchRepairStock = async () => {
  const rows = selectedStockRows.value;
  if (!rows.length) return;
  try {
    await confirmAction(`确认批量修复已勾选的 ${rows.length} 条库存对账记录吗？`);
    for (const row of rows) {
      await repairAdminStockReconciliation(row.id, { remark: '库存对账页触发批量修复' });
    }
    ElMessage.success(`已批量修复 ${rows.length} 条库存对账记录`);
    selectedStockRows.value = [];
    await loadStockReconciliations();
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.response?.data?.msg || '批量修复库存对账失败');
  }
};
const handleStockSortChange = async ({ prop, order }) => {
  stockQuery.sortBy = prop || 'id';
  stockQuery.sortOrder = order === 'ascending' ? 'asc' : 'desc';
  stockPager.page = 1;
  selectedStockRows.value = [];
  await syncStockReconciliationRoute();
  loadStockReconciliations();
};
const handleStockPageChange = async (page) => { stockPager.page = page; await syncStockReconciliationRoute(); loadStockReconciliations(); };
const handleStockSizeChange = async (size) => { stockPager.size = size; stockPager.page = 1; await syncStockReconciliationRoute(); loadStockReconciliations(); };
const handleLogout = async () => { await adminStore.logout(); router.push('/login'); };

onMounted(async () => {
  await handleMainTabChange(mainTab.value);
});
</script>

<style scoped>
.online-reconcile-page { min-height: 760px; }
.reconcile-hero { display: flex; justify-content: space-between; gap: 16px; align-items: center; padding: 18px; margin-bottom: 16px; border-radius: 16px; background: linear-gradient(135deg, #eff6ff, #f8fafc); border: 1px solid #dbeafe; }
.reconcile-hero h2 { margin: 0 0 8px; color: #1e3a8a; }
.reconcile-hero p { margin: 0; color: #64748b; }
.reconcile-actions { display: flex; gap: 6px; flex-wrap: nowrap; justify-content: center; align-items: center; white-space: nowrap; }
.reconcile-actions :deep(.el-button) { margin-left: 0; }
.task-summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
.task-summary-grid > div { padding: 16px; border: 1px solid #e2e8f0; border-radius: 14px; background: #fff; display: flex; flex-direction: column; gap: 6px; }
.task-summary-grid span { color: #64748b; font-size: 13px; }
.task-summary-grid strong { font-size: 28px; color: #0f172a; }
.task-summary-grid em { font-style: normal; color: #94a3b8; font-size: 12px; }
.diff-detail-summary { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; margin-bottom: 14px; }
.diff-detail-summary > div { padding: 12px; border-radius: 12px; background: #f8fafc; border: 1px solid #e2e8f0; display: flex; flex-direction: column; gap: 5px; }
.diff-detail-summary span { color: #64748b; font-size: 12px; }
.diff-detail-summary strong { color: #0f172a; font-size: 14px; word-break: break-all; }
.diff-detail-summary small { color: #94a3b8; font-size: 12px; line-height: 1.5; }
.bill-compare-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; margin: 16px 0; }
.bill-compare-card { border: 1px solid #e2e8f0; border-radius: 14px; padding: 14px; background: #fff; }
.bill-compare-card h3 { margin: 0 0 12px; color: #1e3a8a; font-size: 15px; }
.bill-compare-card p { margin: 8px 0; display: flex; justify-content: space-between; gap: 12px; border-bottom: 1px dashed #e5e7eb; padding-bottom: 6px; }
.bill-compare-card p span { color: #64748b; flex: 0 0 90px; }
.bill-compare-card p strong { color: #0f172a; text-align: right; word-break: break-all; }
.diff-audit-panel { display: grid; gap: 12px; margin-top: 14px; }
.diff-audit-actions { display: flex; flex-wrap: wrap; gap: 8px; padding: 10px 12px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 10px; }
.channel-check-alert :deep(.el-alert__content) { width: 100%; }
.channel-check-list { margin: 6px 0 0; padding-left: 18px; line-height: 1.8; color: #475569; }
.diff-handle-form { margin-top: 14px; }
.diff-handle-alert { margin-bottom: 14px; }
.diff-filter-bar { flex-wrap: wrap; }
.diff-table :deep(.diff-tag--matched) { --el-tag-bg-color: #ecfdf5; --el-tag-border-color: #bbf7d0; --el-tag-text-color: #16a34a; }
.diff-table :deep(.diff-tag--missing-channel) { --el-tag-bg-color: #fff7ed; --el-tag-border-color: #fed7aa; --el-tag-text-color: #ea580c; }
.diff-table :deep(.diff-tag--single-side) { --el-tag-bg-color: #fef2f2; --el-tag-border-color: #fecaca; --el-tag-text-color: #dc2626; }
.diff-table :deep(.diff-tag--amount) { --el-tag-bg-color: #fefce8; --el-tag-border-color: #fde68a; --el-tag-text-color: #ca8a04; }
.diff-table :deep(.diff-tag--refund) { --el-tag-bg-color: #faf5ff; --el-tag-border-color: #e9d5ff; --el-tag-text-color: #9333ea; }
.diff-table :deep(.diff-tag--duplicate) { --el-tag-bg-color: #f3f4f6; --el-tag-border-color: #d1d5db; --el-tag-text-color: #4b5563; }
.diff-table :deep(.reconcile-action-tag) { border-radius: 6px; }
.diff-table :deep(.reconcile-action-tag--manual-review) { --el-tag-bg-color: #f8fafc; --el-tag-border-color: #cbd5e1; --el-tag-text-color: #64748b; }
.reconcile-status--success { --el-tag-bg-color: #ecfdf5; --el-tag-border-color: #bbf7d0; --el-tag-text-color: #16a34a; }
.reconcile-status--primary { --el-tag-bg-color: #eff6ff; --el-tag-border-color: #bfdbfe; --el-tag-text-color: #2563eb; }
.reconcile-status--warning { --el-tag-bg-color: #fffbeb; --el-tag-border-color: #fde68a; --el-tag-text-color: #d97706; }
.reconcile-status--info { --el-tag-bg-color: #eff6ff; --el-tag-border-color: #bfdbfe; --el-tag-text-color: #2563eb; }
.reconcile-status--danger { --el-tag-bg-color: #fef2f2; --el-tag-border-color: #fecaca; --el-tag-text-color: #dc2626; }
.reconcile-overflow-table,
.reconcile-overflow-table :deep(.el-table__header),
.reconcile-overflow-table :deep(.el-table__body) {
  table-layout: fixed;
}

.reconcile-overflow-table :deep(.el-table__cell) {
  overflow: hidden;
}

.reconcile-overflow-table :deep(.el-table__cell .cell) {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  word-break: keep-all;
}

.reconcile-overflow-table :deep(.el-tooltip__trigger),
.reconcile-overflow-table :deep(.admin-overflow-cell),
.reconcile-overflow-table :deep(.admin-overflow-ellipsis) {
  display: block;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  overflow: hidden;
}

.reconcile-overflow-table :deep(.admin-overflow-ellipsis) {
  text-overflow: ellipsis;
  white-space: nowrap;
  word-break: keep-all;
}
.hanging-follow-table :deep(.hanging-action-group) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  min-width: 0;
  overflow: visible;
  white-space: nowrap;
}
.hanging-follow-table :deep(.hanging-action-group .el-button) {
  margin-left: 0;
  padding-left: 8px;
  padding-right: 8px;
}
.hanging-filter-bar {
  margin-top: 10px;
  margin-bottom: 14px;
}
.hanging-stage-alert {
  margin-bottom: 22px;
  --el-alert-bg-color: #fff7ed;
  --el-alert-border-color: #fed7aa;
  --el-alert-title-text-color: #c2410c;
  --el-alert-description-text-color: #9a3412;
}
.hanging-follow-table :deep(.reconcile-time-text) {
  display: block;
  padding-left: 4px;
  color: #475569;
  line-height: 1.5;
}
.diff-action-bar { display: flex; justify-content: flex-end; gap: 10px; flex-wrap: wrap; margin: 12px 0 6px; }
.reconcile-log-timeline { padding: 4px 6px 0 0; }
.reconcile-log-card { padding: 10px 12px; border: 1px solid #e5e7eb; border-radius: 12px; background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%); box-shadow: 0 6px 18px rgba(15, 23, 42, 0.05); }
.reconcile-log-card__header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; font-weight: 600; color: #1f2937; }
.reconcile-log-card p { margin: 0 0 8px; color: #4b5563; line-height: 1.6; }
.reconcile-log-card small { color: #6b7280; }
.reconcile-main-tabs :deep(.el-tabs__header) { margin: 0 0 14px; }
.stock-reconcile-card { min-height: 560px; }
.section-title-row { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; margin-bottom: 16px; }
.section-title-row h2 { margin: 0 0 8px; color: #0f172a; }
.section-title-row p { margin: 0; color: #64748b; font-size: 13px; }
.stock-snapshot-cell { display: inline-flex; flex-wrap: nowrap; justify-content: center; align-items: center; gap: 4px; width: 100%; }
.stock-snapshot-cell span { display: inline-flex; align-items: center; padding: 3px 6px; border-radius: 999px; background: #eff6ff; color: #1d4ed8; font-size: 12px; font-weight: 600; white-space: nowrap; }
.stock-snapshot-cell--redis span { background: #ecfdf5; color: #047857; }
.stock-snapshot-cell--expected span { background: #fff7ed; color: #c2410c; }
.stock-reconcile-table :deep(.el-table__cell) { overflow: hidden; }
.stock-reconcile-table :deep(.stock-tag--consistent) { --el-tag-bg-color: #ecfdf5; --el-tag-border-color: #bbf7d0; --el-tag-text-color: #16a34a; }
.stock-reconcile-table :deep(.stock-tag--status-repaired) { --el-tag-bg-color: #eef2ff; --el-tag-border-color: #c7d2fe; --el-tag-text-color: #4f46e5; }
.stock-reconcile-table :deep(.stock-tag--inconsistent),
.stock-reconcile-table :deep(.stock-tag--abnormal) { --el-tag-bg-color: #fef2f2; --el-tag-border-color: #fecaca; --el-tag-text-color: #dc2626; }
.stock-reconcile-table :deep(.stock-tag--repair-done) { --el-tag-bg-color: #fff7ed; --el-tag-border-color: #fed7aa; --el-tag-text-color: #ea580c; }
.stock-reconcile-table :deep(.stock-tag--repair-repaired) { --el-tag-bg-color: #f0fdf4; --el-tag-border-color: #86efac; --el-tag-text-color: #15803d; }
.stock-reconcile-table :deep(.stock-tag--repair-pending) { --el-tag-bg-color: #fefce8; --el-tag-border-color: #fde68a; --el-tag-text-color: #ca8a04; }
.stock-reconcile-table :deep(.stock-tag--none) { --el-tag-bg-color: #f3f4f6; --el-tag-border-color: #e5e7eb; --el-tag-text-color: #6b7280; }
.stock-reconcile-table :deep(.stock-tag--ignored) { --el-tag-bg-color: #f8fafc; --el-tag-border-color: #e2e8f0; --el-tag-text-color: #64748b; }
.stock-diff-ellipsis { display: inline-block; width: 100%; max-width: 240px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; vertical-align: middle; }
:global(.stock-diff-tooltip) { max-width: 520px; line-height: 1.6; word-break: break-word; }
@media (max-width: 1100px) { .task-summary-grid, .diff-detail-summary, .bill-compare-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } .reconcile-hero, .section-title-row { flex-direction: column; align-items: flex-start; } }
@media (max-width: 760px) { .diff-detail-summary, .bill-compare-grid { grid-template-columns: 1fr; } }
</style>
