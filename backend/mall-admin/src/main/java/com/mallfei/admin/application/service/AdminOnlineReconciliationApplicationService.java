package com.mallfei.admin.application.service;

import com.mallfei.admin.application.dto.AdminOnlineReconcileDiffHandleRequest;
import com.mallfei.admin.application.dto.AdminReconcileTaskCreateRequest;
import com.mallfei.admin.application.vo.AdminOnlineReconcileViews.ArchiveReportView;
import com.mallfei.admin.application.vo.AdminOnlineReconcileViews.ChannelBillItemView;
import com.mallfei.admin.application.vo.AdminOnlineReconcileViews.DiffDetailView;
import com.mallfei.admin.application.vo.AdminOnlineReconcileViews.DiffItemView;
import com.mallfei.admin.application.vo.AdminOnlineReconcileViews.DiffTypeStatView;
import com.mallfei.admin.application.vo.AdminOnlineReconcileViews.HangingFollowView;
import com.mallfei.admin.application.vo.AdminOnlineReconcileViews.LocalBillItemView;
import com.mallfei.admin.application.vo.AdminOnlineReconcileViews.OperationLogView;
import com.mallfei.admin.application.vo.AdminOnlineReconcileViews.TaskView;
import com.mallfei.auth.facade.AuthFacade;
import com.mallfei.common.api.PageResult;
import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminOnlineReconciliationApplicationService {

    private static final DateTimeFormatter TASK_NO_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final JdbcTemplate jdbcTemplate;
    private final AuthFacade authFacade;
    private final AdminAccountManagementApplicationService adminAccountManagementApplicationService;
    private final AdminPayManagementApplicationService adminPayManagementApplicationService;

    public AdminOnlineReconciliationApplicationService(JdbcTemplate jdbcTemplate,
                                                       AuthFacade authFacade,
                                                       AdminAccountManagementApplicationService adminAccountManagementApplicationService,
                                                       AdminPayManagementApplicationService adminPayManagementApplicationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.authFacade = authFacade;
        this.adminAccountManagementApplicationService = adminAccountManagementApplicationService;
        this.adminPayManagementApplicationService = adminPayManagementApplicationService;
    }

    public PageResult<TaskView> tasks(String status, String channel, long page, long size) {
        requireAdmin();
        StringBuilder sql = new StringBuilder("SELECT * FROM pay_reconcile_task WHERE 1=1");
        List<Object> args = new ArrayList<>();
        if (!blank(status)) {
            sql.append(" AND status = ?");
            args.add(status.trim().toUpperCase());
        }
        if (!blank(channel)) {
            sql.append(" AND channel = ?");
            args.add(channel.trim().toUpperCase());
        }
        sql.append(" ORDER BY id DESC");
        List<TaskView> rows = jdbcTemplate.query(sql.toString(), taskMapper(), args.toArray());
        return PageResult.of(rows, page, size);
    }

    public TaskView task(Long taskId) {
        requireAdmin();
        return loadTask(taskId);
    }

    @Transactional
    public TaskView createTask(AdminReconcileTaskCreateRequest request) {
        requireAdmin();
        if (!request.reconcileDate().isBefore(LocalDate.now())) {
            throw BusinessException.badRequest("只能创建今天以前的对账任务，请选择历史账期");
        }
        String channel = normalizeTaskChannel(request.channel());
        Long existingCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pay_reconcile_task WHERE reconcile_date = ? AND channel = ?", Long.class, request.reconcileDate(), channel);
        if (existingCount != null && existingCount > 0) {
            throw BusinessException.badRequest("该渠道下该对账日期已存在任务，请选择其它日期或渠道");
        }
        String taskNo = "RC" + request.reconcileDate().format(TASK_NO_DATE) + channel + System.currentTimeMillis() % 100000;
        jdbcTemplate.update("INSERT INTO pay_reconcile_task(task_no,reconcile_date,channel,status,local_bill_status,channel_bill_status,match_status,remark,created_at) VALUES(?,?,?,?,?,?,?,?,NOW())",
                taskNo, request.reconcileDate(), channel, "CREATED", "EMPTY", "EMPTY", "NOT_MATCHED", trim(request.remark()));
        Long id = jdbcTemplate.queryForObject("SELECT id FROM pay_reconcile_task WHERE task_no = ?", Long.class, taskNo);
        adminAccountManagementApplicationService.recordOperation("RECONCILIATION", "ONLINE_RECONCILE_TASK_CREATE", "创建线上对账任务：" + taskNo, "SUCCESS");
        return loadTask(id);
    }

    @Transactional
    public TaskView generateLocalBills(Long taskId) {
        requireAdmin();
        TaskView task = ensureMutableTask(taskId);
        jdbcTemplate.update("DELETE FROM pay_reconcile_diff_item WHERE task_id = ?", taskId);
        jdbcTemplate.update("DELETE FROM pay_reconcile_local_bill_item WHERE task_id = ?", taskId);
        LocalDateTime startTime = task.reconcileDate().atStartOfDay();
        LocalDateTime endTime = task.reconcileDate().plusDays(1).atStartOfDay();
        jdbcTemplate.update("INSERT INTO pay_reconcile_local_bill_item(task_id,biz_type,order_no,pay_order_no,refund_no,user_id,local_status,order_status,amount_cent,channel,transaction_no,trade_time,raw_snapshot,created_at) "
                        + "SELECT ?, 'PAY', p.order_no, p.pay_order_no, NULL, p.user_id, p.pay_status, o.order_status, p.pay_amount_cent, COALESCE(p.pay_channel, ?), p.transaction_no, COALESCE(p.created_at, NOW()), CONCAT('{\"payOrderNo\":\"', p.pay_order_no, '\",\"status\":\"', p.pay_status, '\"}'), NOW() "
                        + "FROM pay_order p LEFT JOIN oms_order o ON p.order_no = o.order_no WHERE p.pay_status IN ('SUCCESS','REFUNDED','PARTIALLY_REFUNDED') AND (? = 'MOCK' OR (? = 'ALIPAY' AND p.pay_channel IN ('ALIPAY', 'ALIPAY_WAP', 'ALIPAY_PC')) OR p.pay_channel = ?) AND p.created_at >= ? AND p.created_at < ?",
                taskId, task.channel(), task.channel(), task.channel(), task.channel(), startTime, endTime);
        jdbcTemplate.update("INSERT INTO pay_reconcile_local_bill_item(task_id,biz_type,order_no,pay_order_no,refund_no,user_id,local_status,order_status,amount_cent,channel,transaction_no,trade_time,raw_snapshot,created_at) "
                        + "SELECT ?, 'REFUND', r.order_no, r.pay_order_no, r.refund_no, r.user_id, r.refund_status, o.order_status, r.refund_amount_cent, COALESCE(r.pay_channel, ?), r.transaction_no, COALESCE(r.success_at, r.created_at, NOW()), CONCAT('{\"refundNo\":\"', r.refund_no, '\",\"status\":\"', r.refund_status, '\"}'), NOW() "
                        + "FROM pay_refund_order r LEFT JOIN oms_order o ON r.order_no = o.order_no WHERE r.refund_status IN ('REFUND_SUCCESS','REFUND_PROCESSING','REFUND_FAILED') AND (? = 'MOCK' OR (? = 'ALIPAY' AND r.pay_channel IN ('ALIPAY', 'ALIPAY_WAP', 'ALIPAY_PC')) OR r.pay_channel = ?) AND (r.created_at >= ? AND r.created_at < ? OR r.success_at >= ? AND r.success_at < ?)",
                taskId, task.channel(), task.channel(), task.channel(), task.channel(), startTime, endTime, startTime, endTime);
        refreshLocalStats(taskId);
        TaskView refreshed = loadTask(taskId);
        if (refreshed.localTotalCount() == null || refreshed.localTotalCount() == 0) {
            jdbcTemplate.update("UPDATE pay_reconcile_task SET status = CASE WHEN channel_bill_status = 'READY' THEN 'CHANNEL_BILL_READY' ELSE 'CREATED' END, local_bill_status = 'EMPTY', match_status = 'NOT_MATCHED', updated_at = NOW() WHERE id = ?", taskId);
            throw BusinessException.badRequest("该对账日期未生成本地账单，请核查账期 " + task.reconcileDate() + " 是否存在本日期的支付/退款订单流水，或检查所选渠道是否正确");
        }
        jdbcTemplate.update("UPDATE pay_reconcile_task SET status = CASE WHEN channel_bill_status = 'READY' THEN 'CHANNEL_BILL_READY' ELSE 'LOCAL_BILL_READY' END, local_bill_status = 'READY', match_status = 'NOT_MATCHED', updated_at = NOW() WHERE id = ?", taskId);
        adminAccountManagementApplicationService.recordOperation("RECONCILIATION", "ONLINE_RECONCILE_LOCAL_BILL_GENERATE", "生成本地账单快照：taskId=" + taskId, "SUCCESS");
        return loadTask(taskId);
    }

    @Transactional
    public TaskView generateMockChannelBills(Long taskId, String mode) {
        requireAdmin();
        TaskView task = ensureMutableTask(taskId);
        String resolvedMode = blank(mode) ? "NORMAL" : mode.trim().toUpperCase();
        jdbcTemplate.update("DELETE FROM pay_reconcile_diff_item WHERE task_id = ?", taskId);
        List<LocalBillItemView> locals = localBills(taskId, null, 1, 10000).records();
        if (locals.isEmpty()) {
            generateLocalBills(taskId);
            locals = localBills(taskId, null, 1, 10000).records();
        }
        jdbcTemplate.update("DELETE FROM pay_reconcile_channel_bill_item WHERE task_id = ?", taskId);
        if (locals.isEmpty()) {
            throw BusinessException.badRequest("当前没有可用于生成Mock渠道账单的本地支付/退款订单，请先创建订单或支付/退款数据");
        }
        if ("FULL_TEST".equals(resolvedMode)) {
            jdbcTemplate.update("INSERT INTO pay_reconcile_channel_bill_item(task_id,biz_type,channel,out_trade_no,order_no,pay_order_no,refund_no,channel_trade_no,channel_status,amount_cent,fee_cent,trade_time,raw_line,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())",
                    taskId, "PAY", task.channel(), "MOCK-LONG-" + taskId, "ORD-MOCK-LONG-" + taskId, "MOCK-LONG-" + taskId, null, "MOCK-TX-LONG-" + taskId, "TRADE_SUCCESS", 9900L, 0L, LocalDateTime.now(), "{\"mockDiff\":\"第1条：长款，渠道有账本地无账\"}");
        }
        int index = 0;
        for (LocalBillItemView local : locals) {
            index++;
            if ("FULL_TEST".equals(resolvedMode) && index == 1) {
                continue;
            }
            Long amount = local.amountCent();
            String channelStatus = channelStatusOf(local.bizType(), local.localStatus());
            String mockDiff = "正常一致";
            if ("FULL_TEST".equals(resolvedMode) && index == 2 && amount != null) {
                amount = amount + Math.max(1L, Math.min(100L, amount));
                mockDiff = "第3条：金额不一致";
            }
            if ("FULL_TEST".equals(resolvedMode) && index == 3) {
                channelStatus = oppositeChannelStatus(local.bizType(), channelStatus);
                mockDiff = "第4条：状态不一致";
            }
            jdbcTemplate.update("INSERT INTO pay_reconcile_channel_bill_item(task_id,biz_type,channel,out_trade_no,order_no,pay_order_no,refund_no,channel_trade_no,channel_refund_no,channel_status,amount_cent,fee_cent,trade_time,raw_line,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())",
                    taskId, local.bizType(), task.channel(), local.payOrderNo(), local.orderNo(), local.payOrderNo(), local.refundNo(),
                    "MOCK-TX-" + Objects.toString(local.payOrderNo(), local.orderNo()), local.refundNo() == null ? null : "MOCK-RF-" + local.refundNo(),
                    channelStatus, amount, 0L, local.tradeTime(), "{\"mockMode\":\"" + resolvedMode + "\",\"mockDiff\":\"" + mockDiff + "\"}");
        }
        refreshChannelStats(taskId);
        jdbcTemplate.update("UPDATE pay_reconcile_task SET status = CASE WHEN local_bill_status = 'READY' THEN 'CHANNEL_BILL_READY' ELSE 'CREATED' END, channel_bill_status = 'READY', match_status = 'NOT_MATCHED', updated_at = NOW() WHERE id = ?", taskId);
        adminAccountManagementApplicationService.recordOperation("RECONCILIATION", "ONLINE_RECONCILE_MOCK_CHANNEL_BILL_GENERATE", "生成Mock渠道账单：taskId=" + taskId + "，mode=" + resolvedMode, "SUCCESS");
        return loadTask(taskId);
    }

    @Transactional
    public TaskView uploadAlipayChannelBills(Long taskId, MultipartFile file) {
        requireAdmin();
        TaskView task = ensureMutableTask(taskId);
        if (!"ALIPAY".equals(task.channel())) {
            throw BusinessException.badRequest("当前任务不是支付宝渠道任务，不能上传支付宝账单");
        }
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("请选择支付宝账单 CSV 文件");
        }
        List<AlipayChannelBillRow> rows = parseAlipayChannelBill(file);
        if (rows.isEmpty()) {
            throw BusinessException.badRequest("支付宝账单中未解析到有效流水，请检查 CSV 格式是否正确");
        }
        jdbcTemplate.update("DELETE FROM pay_reconcile_diff_item WHERE task_id = ?", taskId);
        jdbcTemplate.update("DELETE FROM pay_reconcile_channel_bill_item WHERE task_id = ?", taskId);
        for (AlipayChannelBillRow row : rows) {
            jdbcTemplate.update("INSERT INTO pay_reconcile_channel_bill_item(task_id,biz_type,channel,out_trade_no,order_no,pay_order_no,refund_no,channel_trade_no,channel_refund_no,channel_status,amount_cent,fee_cent,trade_time,raw_line,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())",
                    taskId, row.bizType(), task.channel(), row.outTradeNo(), row.orderNo(), row.payOrderNo(), row.refundNo(), row.channelTradeNo(), row.channelRefundNo(), row.channelStatus(), row.amountCent(), row.feeCent(), row.tradeTime(), row.rawLine());
        }
        refreshChannelStats(taskId);
        jdbcTemplate.update("UPDATE pay_reconcile_task SET status = CASE WHEN local_bill_status = 'READY' THEN 'CHANNEL_BILL_READY' ELSE 'CREATED' END, channel_bill_status = 'READY', match_status = 'NOT_MATCHED', updated_at = NOW() WHERE id = ?", taskId);
        adminAccountManagementApplicationService.recordOperation("RECONCILIATION", "ONLINE_RECONCILE_ALIPAY_CHANNEL_BILL_UPLOAD", "上传支付宝渠道账单：taskId=" + taskId + "，文件=" + file.getOriginalFilename(), "SUCCESS");
        return loadTask(taskId);
    }

    @Transactional
    public TaskView match(Long taskId) {
        requireAdmin();
        TaskView task = ensureMutableTask(taskId);
        if (!"READY".equals(task.localBillStatus()) || !"READY".equals(task.channelBillStatus())) {
            throw BusinessException.badRequest("请先生成本地账单和渠道账单");
        }
        jdbcTemplate.update("DELETE FROM pay_reconcile_diff_item WHERE task_id = ?", taskId);
        List<LocalBillItemView> locals = localBills(taskId, null, 1, 10000).records();
        List<ChannelBillItemView> channels = channelBills(taskId, null, 1, 10000).records();
        Map<String, ChannelBillItemView> channelMap = new LinkedHashMap<>();
        for (ChannelBillItemView channel : channels) {
            channelMap.put(matchKey(channel.bizType(), channel.payOrderNo(), channel.refundNo()), channel);
        }
        long matched = 0;
        long diff = 0;
        for (LocalBillItemView local : locals) {
            String key = matchKey(local.bizType(), local.payOrderNo(), local.refundNo());
            ChannelBillItemView channel = channelMap.remove(key);
            if (channel == null) {
                insertDiff(taskId, local, null, "LOCAL_EXISTS_CHANNEL_MISSING", "HIGH", suggestedAction("LOCAL_EXISTS_CHANNEL_MISSING", local.bizType()));
                diff++;
                continue;
            }
            String diffType = resolveDiffType(local, channel);
            if ("MATCHED".equals(diffType)) {
                matched++;
            } else {
                diff++;
            }
            insertDiff(taskId, local, channel, diffType, diffLevel(diffType), suggestedAction(diffType, local.bizType()));
        }
        for (ChannelBillItemView channel : channelMap.values()) {
            insertDiff(taskId, null, channel, "CHANNEL_EXISTS_LOCAL_MISSING", "HIGH", "MANUAL_REVIEW");
            diff++;
        }
        jdbcTemplate.update("UPDATE pay_reconcile_task SET status = 'MATCHED', match_status = 'MATCHED', matched_count = ?, diff_count = ?, pending_count = ?, done_count = 0, hang_count = 0, matched_at = NOW(), updated_at = NOW() WHERE id = ?", matched, diff, diff, taskId);
        adminAccountManagementApplicationService.recordOperation("RECONCILIATION", "ONLINE_RECONCILE_MATCH", "执行自动勾兑：taskId=" + taskId + "，差异=" + diff, "SUCCESS");
        return loadTask(taskId);
    }

    public PageResult<LocalBillItemView> localBills(Long taskId, String bizType, long page, long size) {
        requireAdmin();
        String sql = "SELECT * FROM pay_reconcile_local_bill_item WHERE task_id = ?" + (blank(bizType) ? "" : " AND biz_type = ?") + " ORDER BY id DESC";
        Object[] args = blank(bizType) ? new Object[]{taskId} : new Object[]{taskId, bizType.trim().toUpperCase()};
        return PageResult.of(jdbcTemplate.query(sql, localMapper(), args), page, size);
    }

    public PageResult<ChannelBillItemView> channelBills(Long taskId, String bizType, long page, long size) {
        requireAdmin();
        String sql = "SELECT * FROM pay_reconcile_channel_bill_item WHERE task_id = ?" + (blank(bizType) ? "" : " AND biz_type = ?") + " ORDER BY id DESC";
        Object[] args = blank(bizType) ? new Object[]{taskId} : new Object[]{taskId, bizType.trim().toUpperCase()};
        return PageResult.of(jdbcTemplate.query(sql, channelMapper(), args), page, size);
    }

    public PageResult<DiffItemView> diffItems(Long taskId, String processStatus, long page, long size) {
        requireAdmin();
        String sql = "SELECT * FROM pay_reconcile_diff_item WHERE task_id = ?" + (blank(processStatus) ? "" : " AND process_status = ?") + " ORDER BY FIELD(process_status,'PENDING','HANGING','DONE','IGNORED'), id DESC";
        Object[] args = blank(processStatus) ? new Object[]{taskId} : new Object[]{taskId, processStatus.trim().toUpperCase()};
        return PageResult.of(jdbcTemplate.query(sql, diffMapper(), args), page, size);
    }

    public DiffDetailView diffDetail(Long diffId) {
        requireAdmin();
        DiffItemView diff = loadDiff(diffId);
        LocalBillItemView localBill = diff.localItemId() == null ? null : jdbcTemplate.query("SELECT * FROM pay_reconcile_local_bill_item WHERE id = ?", localMapper(), diff.localItemId()).stream().findFirst().orElse(null);
        ChannelBillItemView channelBill = diff.channelItemId() == null ? null : jdbcTemplate.query("SELECT * FROM pay_reconcile_channel_bill_item WHERE id = ?", channelMapper(), diff.channelItemId()).stream().findFirst().orElse(null);
        return new DiffDetailView(diff, localBill, channelBill, diffLogs(diffId, 1, 50).records());
    }

    public PageResult<OperationLogView> diffLogs(Long diffId, long page, long size) {
        requireAdmin();
        List<OperationLogView> rows = jdbcTemplate.query("SELECT * FROM pay_reconcile_operation_log WHERE diff_item_id = ? ORDER BY id DESC", operationLogMapper(), diffId);
        return PageResult.of(rows, page, size);
    }

    public PageResult<HangingFollowView> hangingFollows(String keyword, String riskLevel, long page, long size) {
        requireAdmin();
        StringBuilder sql = new StringBuilder("SELECT d.*, t.task_no, t.reconcile_date, t.channel, "
                + "(SELECT l.created_at FROM pay_reconcile_operation_log l WHERE l.diff_item_id = d.id ORDER BY l.id DESC LIMIT 1) latest_follow_at, "
                + "(SELECT COALESCE(l.remark, l.operation_content) FROM pay_reconcile_operation_log l WHERE l.diff_item_id = d.id ORDER BY l.id DESC LIMIT 1) latest_follow_content "
                + "FROM pay_reconcile_diff_item d JOIN pay_reconcile_task t ON d.task_id = t.id WHERE d.process_status = 'HANGING'");
        List<Object> args = new ArrayList<>();
        if (!blank(keyword)) {
            sql.append(" AND (d.order_no LIKE ? OR d.pay_order_no LIKE ? OR d.refund_no LIKE ? OR t.task_no LIKE ?)");
            String like = "%" + keyword.trim() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
            args.add(like);
        }
        sql.append(" ORDER BY COALESCE(d.processed_at, d.updated_at, d.created_at) ASC, d.id DESC");
        List<HangingFollowView> rows = jdbcTemplate.query(sql.toString(), hangingFollowMapper(), args.toArray());
        if (!blank(riskLevel)) {
            String target = riskLevel.trim().toUpperCase();
            rows = rows.stream().filter(row -> target.equals(row.riskLevel())).collect(Collectors.toList());
        }
        return PageResult.of(rows, page, size);
    }

    public ArchiveReportView archiveReport(LocalDate startDate, LocalDate endDate, String channel, String bizType, String diffType) {
        requireAdmin();
        StringBuilder taskSql = new StringBuilder("SELECT DISTINCT t.* FROM pay_reconcile_task t");
        List<Object> args = new ArrayList<>();
        boolean needDiffJoin = !blank(bizType) || !blank(diffType);
        if (needDiffJoin) {
            taskSql.append(" JOIN pay_reconcile_diff_item d ON d.task_id = t.id");
        }
        taskSql.append(" WHERE t.status = 'COMPLETED'");
        if (startDate != null) {
            taskSql.append(" AND t.reconcile_date >= ?");
            args.add(startDate);
        }
        if (endDate != null) {
            taskSql.append(" AND t.reconcile_date <= ?");
            args.add(endDate);
        }
        if (!blank(channel)) {
            taskSql.append(" AND t.channel = ?");
            args.add(channel.trim().toUpperCase());
        }
        if (!blank(bizType)) {
            taskSql.append(" AND d.biz_type = ?");
            args.add(bizType.trim().toUpperCase());
        }
        if (!blank(diffType)) {
            taskSql.append(" AND d.diff_type = ?");
            args.add(diffType.trim().toUpperCase());
        }
        taskSql.append(" ORDER BY t.reconcile_date DESC, t.id DESC");
        List<TaskView> tasks = jdbcTemplate.query(taskSql.toString(), taskMapper(), args.toArray());
        long totalTasks = tasks.size();
        long completedTasks = tasks.stream().filter(task -> "COMPLETED".equals(task.status())).count();
        long totalLocalCount = tasks.stream().mapToLong(task -> nullToZero(task.localTotalCount())).sum();
        long totalChannelCount = tasks.stream().mapToLong(task -> nullToZero(task.channelTotalCount())).sum();
        long totalMatchedCount = tasks.stream().mapToLong(task -> nullToZero(task.matchedCount())).sum();
        long totalDiffCount = tasks.stream().mapToLong(task -> nullToZero(task.diffCount())).sum();
        long totalPendingCount = tasks.stream().mapToLong(task -> nullToZero(task.pendingCount())).sum();
        long totalHangCount = tasks.stream().mapToLong(task -> nullToZero(task.hangCount())).sum();
        long localTotalAmountCent = tasks.stream().mapToLong(task -> nullToZero(task.localTotalAmountCent())).sum();
        long channelTotalAmountCent = tasks.stream().mapToLong(task -> nullToZero(task.channelTotalAmountCent())).sum();
        List<DiffTypeStatView> diffTypeStats = diffTypeStats(startDate, endDate, channel, bizType, diffType);
        long netDiffAmountCent = channelTotalAmountCent - localTotalAmountCent;
        return new ArchiveReportView(totalTasks, completedTasks, totalLocalCount, totalChannelCount, totalMatchedCount,
                totalDiffCount, totalPendingCount, totalHangCount, localTotalAmountCent, channelTotalAmountCent,
                netDiffAmountCent, diffTypeStats, tasks);
    }

    public PageResult<OperationLogView> taskOperationLogs(Long taskId, long page, long size) {
        requireAdmin();
        List<OperationLogView> rows = jdbcTemplate.query("SELECT * FROM pay_reconcile_operation_log WHERE task_id = ? ORDER BY id DESC", operationLogMapper(), taskId);
        return PageResult.of(rows, page, size);
    }

    @Transactional
    public DiffDetailView followHangingDiff(Long diffId, String remark) {
        requireAdmin();
        DiffItemView diff = loadDiff(diffId);
        if (!"HANGING".equals(diff.processStatus())) {
            throw BusinessException.badRequest("当前差异不是挂账状态，不能新增挂账跟进");
        }
        recordOnlineReconcileLog(diff.taskId(), diffId, "FOLLOW_UP", "新增挂账跟进记录", remark);
        return diffDetail(diffId);
    }

    @Transactional
    public DiffDetailView closeHangingDiff(Long diffId, String remark) {
        requireAdmin();
        DiffItemView diff = loadDiff(diffId);
        if (!"HANGING".equals(diff.processStatus())) {
            throw BusinessException.badRequest("当前差异不是挂账状态，不能完结闭环");
        }
        jdbcTemplate.update("UPDATE pay_reconcile_diff_item SET process_status = 'DONE', process_remark = ?, processed_by = ?, processed_at = NOW(), updated_at = NOW() WHERE id = ?", buildProcessRemark("MARK_DONE", remark), currentOperatorName(), diffId);
        recordOnlineReconcileLog(diff.taskId(), diffId, "CLOSE_HANGING", "挂账差异完结闭环", remark);
        refreshDiffStats(diff.taskId());
        return diffDetail(diffId);
    }

    @Transactional
    public DiffDetailView transferHangingDiffToFinance(Long diffId, String remark) {
        requireAdmin();
        DiffItemView diff = loadDiff(diffId);
        if (!"HANGING".equals(diff.processStatus())) {
            throw BusinessException.badRequest("当前差异不是挂账状态，不能转财务调账");
        }
        String processRemark = appendRemark(diff.processRemark(), "已转财务调账，形成财务待办", remark);
        jdbcTemplate.update("UPDATE pay_reconcile_diff_item SET suggested_action = 'SUBMIT_FINANCE_ADJUSTMENT', process_remark = ?, updated_at = NOW() WHERE id = ?", processRemark, diffId);
        recordOnlineReconcileLog(diff.taskId(), diffId, "TRANSFER_FINANCE", "挂账差异转财务调账：已形成财务待办", remark);
        return diffDetail(diffId);
    }

    @Transactional
    public DiffDetailView handleDiff(Long diffId, AdminOnlineReconcileDiffHandleRequest request) {
        requireAdmin();
        DiffItemView diff = loadDiff(diffId);
        ensureMutableTask(diff.taskId());
        if (!"PENDING".equals(diff.processStatus())) {
            throw BusinessException.badRequest("当前差异已进入" + processStatusText(diff.processStatus()) + "流程，不能二次挂账或二次处理");
        }
        String action = request.action().trim().toUpperCase();
        String remark = trim(request.remark());
        validateActionSubmit(diff, action, remark);
        executeBusinessActionIfNeeded(diff, action, remark);
        String nextStatus = switch (action) {
            case "HANG" -> "HANGING";
            case "IGNORE" -> "IGNORED";
            default -> "DONE";
        };
        String processRemark = buildProcessRemark(action, remark);
        jdbcTemplate.update("UPDATE pay_reconcile_diff_item SET process_status = ?, process_remark = ?, suggested_action = CASE WHEN suggested_action IS NULL OR suggested_action = '' THEN ? ELSE suggested_action END, processed_by = ?, processed_at = NOW(), updated_at = NOW() WHERE id = ?", nextStatus, processRemark, action, currentOperatorName(), diffId);
        recordOnlineReconcileLog(diff.taskId(), diffId, action, operationContent(action, diff), remark);
        refreshDiffStats(diff.taskId());
        adminAccountManagementApplicationService.recordOperation("RECONCILIATION", "ONLINE_RECONCILE_DIFF_HANDLE", "处理线上对账差异：diffId=" + diffId + "，动作=" + action, "SUCCESS");
        return diffDetail(diffId);
    }

    @Transactional
    public TaskView completeTask(Long taskId, String remark) {
        requireAdmin();
        TaskView task = ensureMutableTask(taskId);
        refreshDiffStats(taskId);
        TaskView refreshed = loadTask(taskId);
        if (refreshed.pendingCount() != null && refreshed.pendingCount() > 0) {
            throw BusinessException.badRequest("仍有待处理差异，不能归档");
        }
        jdbcTemplate.update("UPDATE pay_reconcile_task SET status = 'COMPLETED', completed_by = ?, completed_at = NOW(), remark = CASE WHEN ? IS NULL OR ? = '' THEN remark WHEN remark IS NULL OR remark = '' THEN ? ELSE CONCAT(remark, '；归档备注=', ?) END, updated_at = NOW() WHERE id = ?", currentOperatorName(), remark, remark, remark, remark, taskId);
        recordOnlineReconcileLog(taskId, null, "COMPLETE_TASK", "完成线上对账任务归档：" + task.taskNo(), remark);
        adminAccountManagementApplicationService.recordOperation("RECONCILIATION", "ONLINE_RECONCILE_TASK_COMPLETE", "归档线上对账任务：" + task.taskNo(), "SUCCESS");
        return loadTask(taskId);
    }

    private void insertDiff(Long taskId, LocalBillItemView local, ChannelBillItemView channel, String diffType, String diffLevel, String suggestedAction) {
        String bizType = local != null ? local.bizType() : channel.bizType();
        jdbcTemplate.update("INSERT INTO pay_reconcile_diff_item(task_id,biz_type,diff_type,diff_level,order_no,pay_order_no,refund_no,local_item_id,channel_item_id,local_status,channel_status,local_amount_cent,channel_amount_cent,diff_amount_cent,suggested_action,process_status,process_remark,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())",
                taskId,
                bizType,
                diffType,
                diffLevel,
                local != null ? local.orderNo() : channel.orderNo(),
                local != null ? local.payOrderNo() : channel.payOrderNo(),
                local != null ? local.refundNo() : channel.refundNo(),
                local == null ? null : local.id(),
                channel == null ? null : channel.id(),
                local == null ? null : local.localStatus(),
                channel == null ? null : channel.channelStatus(),
                local == null ? null : local.amountCent(),
                channel == null ? null : channel.amountCent(),
                diffAmount(local == null ? null : local.amountCent(), channel == null ? null : channel.amountCent()),
                suggestedAction,
                "MATCHED".equals(diffType) ? "DONE" : "PENDING",
                systemDiffRemark(diffType, bizType));
    }

    private String resolveDiffType(LocalBillItemView local, ChannelBillItemView channel) {
        boolean amountOk = Objects.equals(local.amountCent(), channel.amountCent());
        boolean statusOk = statusConsistent(local.bizType(), local.localStatus(), channel.channelStatus());
        if (amountOk && statusOk) return "MATCHED";
        if (!amountOk && !statusOk) return "STATUS_AND_AMOUNT_MISMATCH";
        if (!amountOk) return "AMOUNT_MISMATCH";
        if ("REFUND".equals(local.bizType())) return "REFUND_STATUS_MISMATCH";
        return "STATUS_MISMATCH";
    }

    private boolean statusConsistent(String bizType, String localStatus, String channelStatus) {
        if ("PAY".equals(bizType)) {
            return ("SUCCESS".equals(localStatus) && ("TRADE_SUCCESS".equals(channelStatus) || "TRADE_FINISHED".equals(channelStatus)))
                    || ("CLOSED".equals(localStatus) && "TRADE_CLOSED".equals(channelStatus))
                    || (!"SUCCESS".equals(localStatus) && !"TRADE_SUCCESS".equals(channelStatus) && !"TRADE_FINISHED".equals(channelStatus));
        }
        return ("REFUND_SUCCESS".equals(localStatus) && "REFUND_SUCCESS".equals(channelStatus))
                || ("REFUND_FAILED".equals(localStatus) && "REFUND_FAILED".equals(channelStatus))
                || (!"REFUND_SUCCESS".equals(localStatus) && !"REFUND_SUCCESS".equals(channelStatus));
    }

    private List<AlipayChannelBillRow> parseAlipayChannelBill(MultipartFile file) {
        List<AlipayChannelBillRow> rows = new ArrayList<>();
        Charset charset = StandardCharsets.UTF_8;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), charset))) {
            List<String> headers = null;
            String line;
            while ((line = reader.readLine()) != null) {
                String normalizedLine = removeBom(line).trim();
                if (normalizedLine.isEmpty() || normalizedLine.startsWith("#") || normalizedLine.startsWith("----")) continue;
                List<String> columns = parseCsvLine(normalizedLine);
                if (headers == null && looksLikeAlipayHeader(columns)) {
                    headers = columns.stream().map(this::normalizeCsvHeader).toList();
                    continue;
                }
                if (headers == null) continue;
                Map<String, String> row = toCsvRow(headers, columns);
                AlipayChannelBillRow billRow = toAlipayChannelBillRow(row, normalizedLine);
                if (billRow != null) rows.add(billRow);
            }
        } catch (Exception error) {
            throw BusinessException.badRequest("支付宝账单 CSV 解析失败：" + error.getMessage());
        }
        return rows;
    }

    private AlipayChannelBillRow toAlipayChannelBillRow(Map<String, String> row, String rawLine) {
        String outTradeNo = firstValue(row, "商户订单号", "商户网站唯一订单号", "商户单号", "out_trade_no", "商户订单号/商户网站唯一订单号");
        String channelTradeNo = firstValue(row, "支付宝交易号", "交易号", "支付宝流水号", "trade_no");
        String channelRefundNo = firstValue(row, "退款批次号", "退款请求号", "退款单号", "支付宝退款号", "refund_no");
        String status = firstValue(row, "交易状态", "业务状态", "账务类型", "状态");
        Long amountCent = parseAlipayAmountCent(row);
        if (blank(outTradeNo) && blank(channelTradeNo) && amountCent == null) return null;
        String bizType = resolveAlipayBizType(row, channelRefundNo, amountCent);
        String normalizedStatus = normalizeAlipayStatus(bizType, status, amountCent);
        LocalDateTime tradeTime = parseAlipayTradeTime(firstValue(row, "交易付款时间", "交易创建时间", "完成时间", "发生时间", "入账时间"));
        Long feeCent = yuanToCent(firstValue(row, "服务费", "手续费"));
        String orderNo = firstValue(row, "订单号", "商户订单号", "商户网站唯一订单号", "商户单号");
        String payOrderNo = firstValue(row, "支付单号", "商户订单号", "商户网站唯一订单号", "商户单号");
        String refundNo = firstValue(row, "退款单号", "退款请求号", "退款批次号");
        return new AlipayChannelBillRow(bizType, outTradeNo, orderNo, payOrderNo, refundNo, channelTradeNo, channelRefundNo, normalizedStatus, amountCent == null ? 0L : Math.abs(amountCent), feeCent == null ? 0L : Math.abs(feeCent), tradeTime, rawLine);
    }

    private String channelStatusOf(String bizType, String localStatus) {
        if ("PAY".equals(bizType)) {
            if ("SUCCESS".equals(localStatus) || "REFUNDED".equals(localStatus) || "PARTIALLY_REFUNDED".equals(localStatus)) return "TRADE_SUCCESS";
            if ("CLOSED".equals(localStatus)) return "TRADE_CLOSED";
            return "WAIT_BUYER_PAY";
        }
        if ("REFUND_SUCCESS".equals(localStatus)) return "REFUND_SUCCESS";
        if ("REFUND_FAILED".equals(localStatus)) return "REFUND_FAILED";
        return "REFUND_PROCESSING";
    }

    private String oppositeChannelStatus(String bizType, String currentStatus) {
        if ("PAY".equals(bizType)) {
            return "TRADE_SUCCESS".equals(currentStatus) || "TRADE_FINISHED".equals(currentStatus) ? "WAIT_BUYER_PAY" : "TRADE_SUCCESS";
        }
        return "REFUND_SUCCESS".equals(currentStatus) ? "REFUND_PROCESSING" : "REFUND_SUCCESS";
    }

    private String suggestedAction(String diffType, String bizType) {
        if ("MATCHED".equals(diffType)) return "NONE";
        if ("AMOUNT_MISMATCH".equals(diffType) || "STATUS_AND_AMOUNT_MISMATCH".equals(diffType)) return "MANUAL_REVIEW";
        if ("CHANNEL_EXISTS_LOCAL_MISSING".equals(diffType)) return "MANUAL_REVIEW";
        if ("LOCAL_EXISTS_CHANNEL_MISSING".equals(diffType)) return "CLOSE_ORDER_VOID";
        if ("REFUND".equals(bizType)) return "SYNC_REFUND_RESULT";
        return "SYNC_PAY_STATUS";
    }

    private String systemDiffRemark(String diffType, String bizType) {
        return switch (diffType) {
            case "MATCHED" -> "本地账单与渠道账单一致";
            case "LOCAL_EXISTS_CHANNEL_MISSING" -> "本地存在" + bizTypeText(bizType) + "流水，渠道账单缺失，请核查渠道账单导入范围或渠道流水状态";
            case "CHANNEL_EXISTS_LOCAL_MISSING" -> "渠道存在" + bizTypeText(bizType) + "流水，本地账单缺失，请核查本地支付/退款单是否创建或账期是否跨日";
            case "AMOUNT_MISMATCH" -> "本地与渠道金额不一致，请核对优惠、手续费、退款金额或重复流水";
            case "STATUS_AND_AMOUNT_MISMATCH" -> "本地与渠道状态、金额均不一致，请优先人工核验渠道原始流水";
            case "REFUND_STATUS_MISMATCH" -> "退款状态不一致，请核查退款回调、渠道退款状态和本地退款单状态";
            case "STATUS_MISMATCH" -> "支付状态不一致，请核查支付回调、订单支付状态和渠道交易状态";
            default -> "存在对账差异，请人工核验本地账单与渠道账单";
        };
    }

    private String bizTypeText(String bizType) {
        return "REFUND".equals(bizType) ? "退款" : "支付";
    }

    private String diffLevel(String diffType) {
        return switch (diffType) {
            case "MATCHED" -> "LOW";
            case "AMOUNT_MISMATCH", "STATUS_AND_AMOUNT_MISMATCH", "LOCAL_EXISTS_CHANNEL_MISSING", "CHANNEL_EXISTS_LOCAL_MISSING" -> "HIGH";
            default -> "MEDIUM";
        };
    }

    private void refreshLocalStats(Long taskId) {
        Map<String, Object> stats = jdbcTemplate.queryForMap("SELECT COUNT(*) total_count, COALESCE(SUM(amount_cent),0) total_amount FROM pay_reconcile_local_bill_item WHERE task_id = ?", taskId);
        jdbcTemplate.update("UPDATE pay_reconcile_task SET local_total_count = ?, local_total_amount_cent = ? WHERE id = ?", ((Number) stats.get("total_count")).longValue(), ((Number) stats.get("total_amount")).longValue(), taskId);
    }

    private void refreshChannelStats(Long taskId) {
        Map<String, Object> stats = jdbcTemplate.queryForMap("SELECT COUNT(*) total_count, COALESCE(SUM(amount_cent),0) total_amount FROM pay_reconcile_channel_bill_item WHERE task_id = ?", taskId);
        jdbcTemplate.update("UPDATE pay_reconcile_task SET channel_total_count = ?, channel_total_amount_cent = ? WHERE id = ?", ((Number) stats.get("total_count")).longValue(), ((Number) stats.get("total_amount")).longValue(), taskId);
    }

    private void refreshDiffStats(Long taskId) {
        Map<String, Object> stats = jdbcTemplate.queryForMap("SELECT COUNT(*) diff_count, SUM(CASE WHEN process_status = 'PENDING' THEN 1 ELSE 0 END) pending_count, SUM(CASE WHEN process_status = 'DONE' THEN 1 ELSE 0 END) done_count, SUM(CASE WHEN process_status = 'HANGING' THEN 1 ELSE 0 END) hang_count FROM pay_reconcile_diff_item WHERE task_id = ? AND diff_type <> 'MATCHED'", taskId);
        jdbcTemplate.update("UPDATE pay_reconcile_task SET diff_count = ?, pending_count = ?, done_count = ?, hang_count = ?, updated_at = NOW() WHERE id = ?",
                number(stats.get("diff_count")), number(stats.get("pending_count")), number(stats.get("done_count")), number(stats.get("hang_count")), taskId);
    }

    private void executeBusinessActionIfNeeded(DiffItemView diff, String action, String remark) {
        switch (action) {
            case "SYNC_PAY_STATUS", "SYNC_REFUND_RESULT", "CLOSE_ORDER_VOID", "REPAIR_ORDER_STATUS", "RETRY_REFUND", "FIX_REFUND_STATUS", "REFUND_DUPLICATE_CHARGE", "MARK_DUPLICATE_OFFSET", "VOID_DUPLICATE_FLOW", "MANUAL_SUPPLEMENT_TRADE", "SUBMIT_FINANCE_ADJUSTMENT" -> throw BusinessException.badRequest("当前阶段仅保留轻量处置动作；正式闭环能力请通过后续工单或真实业务服务实现");
            case "MARK_TEST_FLOW_VOID" -> {
                ensureDiffType(diff, "CHANNEL_EXISTS_LOCAL_MISSING");
                markChannelBillManualFlag(diff, "TEST_FLOW_VOID", remark);
            }
            case "REGISTER_AMOUNT_ADJUSTMENT" -> markDiffSuggestedAction(diff.id(), "REGISTER_AMOUNT_ADJUSTMENT");
            case "MARK_DONE", "IGNORE", "HANG", "MANUAL_REVIEW" -> { }
            default -> throw BusinessException.badRequest("不支持的线上对账处理动作：" + action);
        }
    }

    private void ensureBizType(DiffItemView diff, String bizType) {
        if (!bizType.equals(diff.bizType())) throw BusinessException.badRequest("当前差异类型不允许执行该业务动作");
    }

    private void validateActionSubmit(DiffItemView diff, String action, String remark) {
        if (isFinalJudgementAction(action) && blank(remark)) {
            throw BusinessException.badRequest("终态处理动作必须填写判定原因和处理备注");
        }
        if ("MARK_TEST_FLOW_VOID".equals(action)) {
            ensureDiffType(diff, "CHANNEL_EXISTS_LOCAL_MISSING");
        }
    }

    private boolean isFinalJudgementAction(String action) {
        return List.of("MARK_TEST_FLOW_VOID", "MARK_DONE", "HANG").contains(action);
    }

    private void ensureDiffType(DiffItemView diff, String diffType) {
        if (!diffType.equals(diff.diffType())) throw BusinessException.badRequest("当前差异场景不允许执行该处理动作");
    }

    private void markChannelBillManualFlag(DiffItemView diff, String flag, String remark) {
        if (diff.channelItemId() == null) throw BusinessException.badRequest("差异缺少渠道账单，无法标记流水");
        jdbcTemplate.update("UPDATE pay_reconcile_channel_bill_item SET raw_line = CONCAT(COALESCE(raw_line, ''), ?) WHERE id = ?",
                ";{\"manualFlag\":\"" + flag + "\",\"operator\":\"" + currentOperatorName() + "\",\"remark\":\"" + sanitizeJsonText(remark) + "\"}", diff.channelItemId());
    }

    private void supplementLocalBillFromChannel(DiffItemView diff, String remark) {
        if (diff.channelItemId() == null) throw BusinessException.badRequest("差异缺少渠道账单，无法补录本地交易");
        ChannelBillItemView channel = jdbcTemplate.query("SELECT * FROM pay_reconcile_channel_bill_item WHERE id = ?", channelMapper(), diff.channelItemId()).stream().findFirst().orElseThrow(() -> BusinessException.badRequest("渠道账单不存在"));
        jdbcTemplate.update("INSERT INTO pay_reconcile_local_bill_item(task_id,biz_type,order_no,pay_order_no,refund_no,user_id,local_status,order_status,amount_cent,channel,transaction_no,trade_time,raw_snapshot,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())",
                diff.taskId(), channel.bizType(), channel.orderNo(), channel.payOrderNo(), channel.refundNo(), null,
                "REFUND".equals(channel.bizType()) ? "REFUND_SUCCESS" : "SUCCESS", null, channel.amountCent(), channel.channel(), channel.channelTradeNo(), channel.tradeTime(),
                "{\"source\":\"MANUAL_SUPPLEMENT_FROM_CHANNEL\",\"operator\":\"" + currentOperatorName() + "\",\"remark\":\"" + sanitizeJsonText(remark) + "\"}");
        refreshLocalStats(diff.taskId());
    }

    private void markDiffSuggestedAction(Long diffId, String suggestedAction) {
        jdbcTemplate.update("UPDATE pay_reconcile_diff_item SET suggested_action = ?, updated_at = NOW() WHERE id = ?", suggestedAction, diffId);
    }

    private String sanitizeJsonText(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", " ").replace("\n", " ");
    }

    private String requireOrderNo(DiffItemView diff) {
        if (blank(diff.orderNo())) throw BusinessException.badRequest("差异缺少订单号，无法执行业务修复");
        return diff.orderNo();
    }

    private String requireRefundNo(DiffItemView diff) {
        if (blank(diff.refundNo())) throw BusinessException.badRequest("差异缺少退款单号，无法同步退款结果");
        return diff.refundNo();
    }

    private String appendRemark(String existingRemark, String actionRemark, String userRemark) {
        StringBuilder builder = new StringBuilder(blank(existingRemark) ? "" : existingRemark);
        if (!builder.isEmpty()) builder.append("；");
        builder.append(actionRemark);
        if (!blank(userRemark)) builder.append("，说明=").append(userRemark);
        return builder.toString();
    }

    private String processStatusText(String status) {
        return switch (status) {
            case "HANGING" -> "挂账跟进";
            case "DONE" -> "已完成";
            case "IGNORED" -> "已忽略";
            default -> Objects.toString(status, "非待处理");
        };
    }

    private String buildProcessRemark(String action, String remark) {
        String base = switch (action) {
            case "SYNC_PAY_STATUS" -> "已同步支付状态";
            case "REPAIR_ORDER_STATUS" -> "已补偿订单状态";
            case "SYNC_REFUND_RESULT" -> "已同步退款结果";
            case "CLOSE_ORDER_VOID" -> "已关闭订单/标记作废";
            case "MARK_TEST_FLOW_VOID" -> "已标记测试流水作废";
            case "MANUAL_SUPPLEMENT_TRADE" -> "已登记手动补录交易记录";
            case "MARK_DUPLICATE_OFFSET" -> "已标记重复流水待冲账";
            case "REGISTER_AMOUNT_ADJUSTMENT" -> "已登记长短款";
            case "SUBMIT_FINANCE_ADJUSTMENT" -> "已提交财务调账";
            case "RETRY_REFUND" -> "已登记重新发起退款";
            case "FIX_REFUND_STATUS" -> "已人工修正退款状态";
            case "VOID_DUPLICATE_FLOW" -> "已标记重复流水作废";
            case "REFUND_DUPLICATE_CHARGE" -> "已登记重复扣款退款";
            case "HANG" -> "已登记挂账待跟进";
            case "IGNORE" -> "已忽略归档";
            case "MANUAL_REVIEW" -> "已转人工复核";
            default -> "人工确认已处理";
        };
        return blank(remark) ? base : base + "，说明=" + remark;
    }

    private String operationContent(String action, DiffItemView diff) {
        return "线上对账差异处理：动作=" + action + "，类型=" + diff.bizType() + "，差异=" + diff.diffType() + "，订单=" + Objects.toString(diff.orderNo(), "-") + "，支付单=" + Objects.toString(diff.payOrderNo(), "-") + "，退款单=" + Objects.toString(diff.refundNo(), "-");
    }

    private void recordOnlineReconcileLog(Long taskId, Long diffId, String operationType, String content, String remark) {
        jdbcTemplate.update("INSERT INTO pay_reconcile_operation_log(task_id,diff_item_id,operation_type,operation_content,operator_name,remark,created_at) VALUES(?,?,?,?,?,?,NOW())",
                taskId, diffId, operationType, content, currentOperatorName(), trim(remark));
    }

    private DiffItemView loadDiff(Long diffId) {
        List<DiffItemView> rows = jdbcTemplate.query("SELECT * FROM pay_reconcile_diff_item WHERE id = ?", diffMapper(), diffId);
        if (rows.isEmpty()) throw BusinessException.badRequest("对账差异不存在");
        return rows.get(0);
    }

    private String currentOperatorName() {
        AuthenticatedPrincipal principal = authFacade.currentPrincipal();
        if (principal == null) return "SYSTEM";
        return blank(principal.account()) ? String.valueOf(principal.principalId()) : principal.account();
    }

    private long number(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private List<DiffTypeStatView> diffTypeStats(LocalDate startDate, LocalDate endDate, String channel, String bizType, String diffType) {
        StringBuilder sql = new StringBuilder("SELECT d.diff_type, COUNT(*) count_value, COALESCE(SUM(ABS(COALESCE(d.diff_amount_cent, 0))), 0) amount_value "
                + "FROM pay_reconcile_diff_item d JOIN pay_reconcile_task t ON d.task_id = t.id WHERE t.status = 'COMPLETED' AND d.diff_type <> 'MATCHED'");
        List<Object> args = new ArrayList<>();
        if (startDate != null) {
            sql.append(" AND t.reconcile_date >= ?");
            args.add(startDate);
        }
        if (endDate != null) {
            sql.append(" AND t.reconcile_date <= ?");
            args.add(endDate);
        }
        if (!blank(channel)) {
            sql.append(" AND t.channel = ?");
            args.add(channel.trim().toUpperCase());
        }
        if (!blank(bizType)) {
            sql.append(" AND d.biz_type = ?");
            args.add(bizType.trim().toUpperCase());
        }
        if (!blank(diffType)) {
            sql.append(" AND d.diff_type = ?");
            args.add(diffType.trim().toUpperCase());
        }
        sql.append(" GROUP BY d.diff_type ORDER BY count_value DESC");
        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> new DiffTypeStatView(rs.getString("diff_type"), rs.getLong("count_value"), rs.getLong("amount_value")), args.toArray());
    }

    private TaskView ensureMutableTask(Long taskId) {
        TaskView task = loadTask(taskId);
        if ("COMPLETED".equals(task.status())) {
            throw BusinessException.badRequest("对账任务已归档，不能修改");
        }
        return task;
    }

    private TaskView loadTask(Long taskId) {
        List<TaskView> rows = jdbcTemplate.query("SELECT * FROM pay_reconcile_task WHERE id = ?", taskMapper(), taskId);
        if (rows.isEmpty()) throw BusinessException.badRequest("对账任务不存在");
        return rows.get(0);
    }

    private String matchKey(String bizType, String payOrderNo, String refundNo) {
        return bizType + "|" + Objects.toString(payOrderNo, "") + "|" + Objects.toString(refundNo, "");
    }

    private Long diffAmount(Long left, Long right) {
        if (left == null || right == null) return null;
        return left - right;
    }

    private long countLocalSourceBills(LocalDate reconcileDate, String channel) {
        LocalDateTime startTime = reconcileDate.atStartOfDay();
        LocalDateTime endTime = reconcileDate.plusDays(1).atStartOfDay();
        Long payCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pay_order p WHERE (? = 'MOCK' OR (? = 'ALIPAY' AND p.pay_channel IN ('ALIPAY', 'ALIPAY_WAP', 'ALIPAY_PC')) OR p.pay_channel = ?) AND p.created_at >= ? AND p.created_at < ?",
                Long.class, channel, channel, channel, startTime, endTime);
        Long refundCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pay_refund_order r WHERE (? = 'MOCK' OR (? = 'ALIPAY' AND r.pay_channel IN ('ALIPAY', 'ALIPAY_WAP', 'ALIPAY_PC')) OR r.pay_channel = ?) AND COALESCE(r.success_at, r.created_at) >= ? AND COALESCE(r.success_at, r.created_at) < ?",
                Long.class, channel, channel, channel, startTime, endTime);
        return (payCount == null ? 0 : payCount) + (refundCount == null ? 0 : refundCount);
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private boolean looksLikeAlipayHeader(List<String> columns) {
        String joined = columns.stream().map(this::normalizeCsvHeader).collect(Collectors.joining("|"));
        return joined.contains("商户订单号") || joined.contains("支付宝交易号") || joined.contains("交易号") || joined.contains("交易状态");
    }

    private Map<String, String> toCsvRow(List<String> headers, List<String> columns) {
        Map<String, String> row = new HashMap<>();
        for (int i = 0; i < headers.size() && i < columns.size(); i++) {
            row.put(headers.get(i), stripCsvValue(columns.get(i)));
        }
        return row;
    }

    private String normalizeCsvHeader(String value) {
        return stripCsvValue(removeBom(value)).replace(" ", "").replace("\t", "").replace("\uFEFF", "");
    }

    private String stripCsvValue(String value) {
        if (value == null) return null;
        String text = value.trim();
        if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
            text = text.substring(1, text.length() - 1).replace("\"\"", "\"");
        }
        return text.trim();
    }

    private String removeBom(String value) {
        return value == null ? null : value.replace("\uFEFF", "");
    }

    private String firstValue(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String value = row.get(normalizeCsvHeader(key));
            if (!blank(value) && !"-".equals(value.trim())) return value.trim();
        }
        return null;
    }

    private Long parseAlipayAmountCent(Map<String, String> row) {
        Long income = yuanToCent(firstValue(row, "收入金额", "收入"));
        Long expense = yuanToCent(firstValue(row, "支出金额", "支出"));
        if (income != null && income != 0) return income;
        if (expense != null && expense != 0) return -Math.abs(expense);
        return yuanToCent(firstValue(row, "交易金额", "实收金额", "金额", "发生金额"));
    }

    private Long yuanToCent(String value) {
        if (blank(value)) return null;
        String normalized = value.replace(",", "").replace("¥", "").replace("￥", "").trim();
        if (blank(normalized) || "-".equals(normalized)) return null;
        return new BigDecimal(normalized).multiply(BigDecimal.valueOf(100)).setScale(0, java.math.RoundingMode.HALF_UP).longValue();
    }

    private String resolveAlipayBizType(Map<String, String> row, String channelRefundNo, Long amountCent) {
        String bizText = Objects.toString(firstValue(row, "业务类型", "账务类型", "商品名称", "备注"), "").toUpperCase();
        if (!blank(channelRefundNo) || bizText.contains("退款") || bizText.contains("REFUND") || (amountCent != null && amountCent < 0)) return "REFUND";
        return "PAY";
    }

    private String normalizeAlipayStatus(String bizType, String status, Long amountCent) {
        String text = Objects.toString(status, "").toUpperCase();
        if ("REFUND".equals(bizType)) {
            if (text.contains("失败")) return "REFUND_FAILED";
            return "REFUND_SUCCESS";
        }
        if (text.contains("关闭") || text.contains("CLOSED")) return "TRADE_CLOSED";
        if (text.contains("等待") || text.contains("WAIT")) return "WAIT_BUYER_PAY";
        if (amountCent != null && amountCent < 0) return "REFUND_SUCCESS";
        return "TRADE_SUCCESS";
    }

    private LocalDateTime parseAlipayTradeTime(String value) {
        if (blank(value)) return LocalDateTime.now();
        String text = value.trim().replace("/", "-");
        try {
            return LocalDateTime.parse(text.replace(" ", "T"));
        } catch (Exception ignored) {
            try {
                return LocalDate.parse(text).atStartOfDay();
            } catch (Exception error) {
                return LocalDateTime.now();
            }
        }
    }

    private record AlipayChannelBillRow(String bizType, String outTradeNo, String orderNo, String payOrderNo,
                                        String refundNo, String channelTradeNo, String channelRefundNo,
                                        String channelStatus, Long amountCent, Long feeCent,
                                        LocalDateTime tradeTime, String rawLine) {}

    private String normalizeTaskChannel(String channel) {
        if (blank(channel)) {
            throw BusinessException.badRequest("对账渠道不能为空");
        }
        String normalized = channel.trim().toUpperCase();
        if ("ALIPAY_WAP".equals(normalized) || "ALIPAY_PC".equals(normalized)) {
            return "ALIPAY";
        }
        if ("ALIPAY".equals(normalized) || "MOCK".equals(normalized)) {
            return normalized;
        }
        throw BusinessException.badRequest("不支持的对账渠道：" + channel);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void requireAdmin() {
        AuthenticatedPrincipal principal = authFacade.currentPrincipal();
        if (principal == null || !principal.isAdmin()) throw BusinessException.forbidden("仅管理员可访问当前接口");
    }

    private RowMapper<TaskView> taskMapper() {
        return (rs, rowNum) -> new TaskView(rs.getLong("id"), rs.getString("task_no"), rs.getObject("reconcile_date", LocalDate.class), rs.getString("channel"), rs.getString("status"), rs.getString("local_bill_status"), rs.getString("channel_bill_status"), rs.getString("match_status"), rs.getLong("local_total_count"), rs.getLong("local_total_amount_cent"), rs.getLong("channel_total_count"), rs.getLong("channel_total_amount_cent"), rs.getLong("matched_count"), rs.getLong("diff_count"), rs.getLong("pending_count"), rs.getLong("done_count"), rs.getLong("hang_count"), rs.getString("remark"), getDateTime(rs, "created_at"), getDateTime(rs, "matched_at"), getDateTime(rs, "completed_at"));
    }

    private RowMapper<LocalBillItemView> localMapper() {
        return (rs, rowNum) -> new LocalBillItemView(rs.getLong("id"), rs.getLong("task_id"), rs.getString("biz_type"), rs.getString("order_no"), rs.getString("pay_order_no"), rs.getString("refund_no"), getLong(rs, "user_id"), rs.getString("local_status"), rs.getString("order_status"), getLong(rs, "amount_cent"), rs.getString("channel"), rs.getString("transaction_no"), getDateTime(rs, "trade_time"));
    }

    private RowMapper<ChannelBillItemView> channelMapper() {
        return (rs, rowNum) -> new ChannelBillItemView(rs.getLong("id"), rs.getLong("task_id"), rs.getString("biz_type"), rs.getString("channel"), rs.getString("out_trade_no"), rs.getString("order_no"), rs.getString("pay_order_no"), rs.getString("refund_no"), rs.getString("channel_trade_no"), rs.getString("channel_refund_no"), rs.getString("channel_status"), getLong(rs, "amount_cent"), getLong(rs, "fee_cent"), getDateTime(rs, "trade_time"));
    }

    private RowMapper<DiffItemView> diffMapper() {
        return (rs, rowNum) -> new DiffItemView(rs.getLong("id"), rs.getLong("task_id"), rs.getString("biz_type"), rs.getString("diff_type"), rs.getString("diff_level"), rs.getString("order_no"), rs.getString("pay_order_no"), rs.getString("refund_no"), getLong(rs, "local_item_id"), getLong(rs, "channel_item_id"), rs.getString("local_status"), rs.getString("channel_status"), getLong(rs, "local_amount_cent"), getLong(rs, "channel_amount_cent"), getLong(rs, "diff_amount_cent"), rs.getString("suggested_action"), rs.getString("process_status"), rs.getString("process_remark"), rs.getString("processed_by"), getDateTime(rs, "processed_at"), getDateTime(rs, "created_at"));
    }

    private RowMapper<OperationLogView> operationLogMapper() {
        return (rs, rowNum) -> new OperationLogView(rs.getLong("id"), rs.getLong("task_id"), getLong(rs, "diff_item_id"), rs.getString("operation_type"), rs.getString("operation_content"), rs.getString("operator_name"), rs.getString("remark"), getDateTime(rs, "created_at"));
    }

    private RowMapper<HangingFollowView> hangingFollowMapper() {
        return (rs, rowNum) -> {
            LocalDateTime hangingAt = getDateTime(rs, "processed_at");
            if (hangingAt == null) hangingAt = getDateTime(rs, "created_at");
            long hangingDays = hangingAt == null ? 0L : java.time.Duration.between(hangingAt, LocalDateTime.now()).toDays();
            String riskLevel = hangingDays >= 7 ? "HIGH" : hangingDays >= 3 ? "MEDIUM" : "LOW";
            return new HangingFollowView(rs.getLong("id"), rs.getLong("task_id"), rs.getString("task_no"), rs.getObject("reconcile_date", LocalDate.class), rs.getString("channel"),
                    rs.getString("biz_type"), rs.getString("diff_type"), rs.getString("order_no"), rs.getString("pay_order_no"), rs.getString("refund_no"),
                    getLong(rs, "diff_amount_cent"), rs.getString("process_status"), rs.getString("process_remark"), rs.getString("processed_by"),
                    hangingAt, getDateTime(rs, "latest_follow_at"), rs.getString("latest_follow_content"), hangingDays, riskLevel);
        };
    }

    private Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private LocalDateTime getDateTime(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column) == null ? null : rs.getTimestamp(column).toLocalDateTime();
    }
}
