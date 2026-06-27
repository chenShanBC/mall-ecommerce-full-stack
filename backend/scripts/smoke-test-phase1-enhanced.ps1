param(
    [string]$BaseUrl = "http://localhost:9090",
    [string]$UserMobile = "13800000000",
    [string]$UserPassword = "123456",
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "123456",
    [string]$OrderNo = "",
    [long]$SkuId = 1,
    [long]$TaskId = 0,
    [long]$DiffId = 0,
    [long]$PayReconciliationRecordId = 0,
    [long]$StockReconciliationRecordId = 0,
    [string]$AftersaleNo = "",
    [string]$RefundNo = "",
    [switch]$Strict
)

$ErrorActionPreference = "Stop"

$script:Passed = 0
$script:Skipped = 0
$script:Failed = 0

function Write-Step([string]$Message) {
    Write-Host "`n==== $Message ====" -ForegroundColor Cyan
}

function Write-Ok([string]$Message) {
    $script:Passed++
    Write-Host "[PASS] $Message" -ForegroundColor Green
}

function Write-Skip([string]$Message) {
    $script:Skipped++
    Write-Host "[SKIP] $Message" -ForegroundColor Yellow
}

function Write-Fail([string]$Message) {
    $script:Failed++
    Write-Host "[FAIL] $Message" -ForegroundColor Red
}

function ConvertTo-QueryString([hashtable]$Query) {
    if (-not $Query -or $Query.Count -eq 0) { return "" }
    $pairs = @()
    foreach ($key in $Query.Keys) {
        if ($null -ne $Query[$key] -and "" -ne [string]$Query[$key]) {
            $pairs += "{0}={1}" -f [uri]::EscapeDataString([string]$key), [uri]::EscapeDataString([string]$Query[$key])
        }
    }
    if ($pairs.Count -eq 0) { return "" }
    return "?" + ($pairs -join "&")
}

function Get-ResponseBodyFromError($ErrorRecord) {
    try {
        $response = $ErrorRecord.Exception.Response
        if (-not $response) { return $ErrorRecord.Exception.Message }
        $stream = $response.GetResponseStream()
        if (-not $stream) { return $ErrorRecord.Exception.Message }
        $reader = [System.IO.StreamReader]::new($stream)
        return $reader.ReadToEnd()
    } catch {
        return $ErrorRecord.Exception.Message
    }
}

function Invoke-Api {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [hashtable]$Query = $null,
        [string]$Token = $null,
        [switch]$Optional,
        [switch]$AllowBusinessFailure
    )

    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }

    $uri = "$BaseUrl$Path$(ConvertTo-QueryString $Query)"
    $params = @{
        Method = $Method
        Uri = $uri
        Headers = $headers
        ContentType = "application/json; charset=utf-8"
    }
    if ($null -ne $Body) { $params.Body = ($Body | ConvertTo-Json -Depth 30) }

    try {
        $response = Invoke-RestMethod @params
        if ($null -ne $response.success -and $response.success -ne $true -and -not $AllowBusinessFailure) {
            $bodyText = $response | ConvertTo-Json -Depth 30
            if ($Optional -or -not $Strict) {
                Write-Skip "$Name business failure: $bodyText"
                return $null
            }
            throw "$Name business failure: $bodyText"
        }
        Write-Ok $Name
        return $response
    } catch {
        $bodyText = Get-ResponseBodyFromError $_
        if ($Optional -or -not $Strict) {
            Write-Skip "$Name request failed: $bodyText"
            return $null
        }
        Write-Fail "$Name request failed: $bodyText"
        throw
    }
}

function Get-Token([object]$LoginData) {
    if (-not $LoginData) { return $null }
    foreach ($field in @("token", "accessToken", "tokenValue", "satoken", "satokenValue", "authorization", "Authorization")) {
        if ($LoginData.PSObject.Properties.Name -contains $field -and $LoginData.$field) { return [string]$LoginData.$field }
    }
    return $null
}

function Get-FirstArrayItem($Data) {
    if (-not $Data) { return $null }
    foreach ($field in @("records", "list", "items", "rows", "content")) {
        if ($Data.PSObject.Properties.Name -contains $field -and $Data.$field -and $Data.$field.Count -gt 0) { return $Data.$field[0] }
    }
    if ($Data -is [array] -and $Data.Count -gt 0) { return $Data[0] }
    return $null
}

function Pick-FirstId($Obj, [string[]]$Fields) {
    if (-not $Obj) { return $null }
    foreach ($field in $Fields) {
        if ($Obj.PSObject.Properties.Name -contains $field -and $Obj.$field) { return $Obj.$field }
    }
    return $null
}

function Invoke-IfId {
    param(
        [string]$Name,
        [long]$Id,
        [scriptblock]$Call
    )
    if ($Id -gt 0) { & $Call } else { Write-Skip "$Name 需要有效 ID，当前未传入且列表未发现可用数据" }
}

Write-Host "一期及一期增强阶段冒烟测试开始：$BaseUrl" -ForegroundColor Cyan
Write-Host "说明：该脚本覆盖售后、退款、库存预占/对账、支付同步/异常修复、基础/线上人工对账、权限审计、Dashboard 增强、认证增强等接口。默认非 Strict 模式会跳过缺少前置业务数据或状态不满足的写操作。" -ForegroundColor DarkGray

Write-Step "登录并准备上下文"
$userLogin = Invoke-Api -Name "用户密码登录" -Method POST -Path "/api/users/login/password" -Body @{ mobile = $UserMobile; password = $UserPassword } -Optional
$userToken = Get-Token $userLogin.data
$adminLogin = Invoke-Api -Name "管理员登录" -Method POST -Path "/api/admin/login/password" -Body @{ username = $AdminUsername; password = $AdminPassword } -Optional
$adminToken = Get-Token $adminLogin.data
if (-not $adminToken) {
    Write-Fail "一期/一期增强多数接口依赖 Admin 登录态，未获取到 Admin token"
    if ($Strict) { exit 1 }
}

if ($adminToken -and -not $OrderNo) {
    $orders = Invoke-Api -Name "查询后台订单列表以发现 OrderNo" -Method GET -Path "/api/admin/orders" -Token $adminToken -Query @{ page = 1; size = 10 } -Optional
    $firstOrder = Get-FirstArrayItem $orders.data
    $detectedOrderNo = Pick-FirstId $firstOrder @("orderNo")
    if ($detectedOrderNo) { $OrderNo = [string]$detectedOrderNo }
}

Write-Step "一期-C端认证增强"
Invoke-Api -Name "手机号黑名单状态检查" -Method GET -Path "/api/users/login/blacklist/status" -Query @{ mobile = $UserMobile } -Optional | Out-Null
Invoke-Api -Name "登录拼图验证码挑战" -Method GET -Path "/api/users/login/captcha/challenge" -Optional | Out-Null
Invoke-Api -Name "发送短信验证码" -Method POST -Path "/api/users/login/sms/send-code" -Body @{ mobile = $UserMobile } -Optional | Out-Null
Invoke-Api -Name "支付宝授权地址" -Method GET -Path "/api/users/login/alipay/auth-url" -Optional | Out-Null
if ($userToken) {
    Invoke-Api -Name "当前用户黑名单状态检查" -Method GET -Path "/api/users/me/blacklist-status" -Token $userToken -Optional | Out-Null
    Invoke-Api -Name "手机号绑定验证码发送" -Method POST -Path "/api/users/me/mobile/send-code" -Token $userToken -Body @{ mobile = $UserMobile } -Optional | Out-Null
} else {
    Write-Skip "未获取到 C 端用户 token，跳过当前用户黑名单和手机号绑定接口"
}

Write-Step "一期-订单异常处理与支付异常修复"
if ($adminToken) {
    if ($OrderNo) {
        Invoke-Api -Name "后台订单详情" -Method GET -Path "/api/admin/orders/$OrderNo" -Token $adminToken -Optional | Out-Null
        Invoke-Api -Name "后台修改订单收货地址" -Method PUT -Path "/api/admin/orders/$OrderNo/receiver" -Token $adminToken -Body @{ receiverName = "冒烟测试"; receiverPhone = $UserMobile; provinceName = "北京市"; cityName = "北京市"; districtName = "朝阳区"; detailAddress = "测试地址" } -Optional | Out-Null
        Invoke-Api -Name "后台处理订单异常" -Method POST -Path "/api/admin/orders/$OrderNo/exception-handle" -Token $adminToken -Body @{ action = "REMARK"; note = "phase1 smoke" } -Optional | Out-Null
        Invoke-Api -Name "标记支付异常" -Method POST -Path "/api/admin/orders/$OrderNo/payment-exception" -Token $adminToken -Body @{ note = "phase1 smoke" } -Optional | Out-Null
        Invoke-Api -Name "核验支付异常" -Method POST -Path "/api/admin/orders/$OrderNo/payment-exception/verify" -Token $adminToken -Optional | Out-Null
        Invoke-Api -Name "支付异常转支付同步" -Method POST -Path "/api/admin/orders/$OrderNo/payment-exception/transfer-pay-sync" -Token $adminToken -Body @{ note = "phase1 smoke" } -Optional | Out-Null
        Invoke-Api -Name "标记支付异常待处理动作" -Method POST -Path "/api/admin/orders/$OrderNo/payment-exception/pending-action" -Token $adminToken -Body @{ action = "MANUAL_CHECK"; note = "phase1 smoke" } -Optional | Out-Null
        Invoke-Api -Name "人工确认已支付" -Method POST -Path "/api/admin/orders/$OrderNo/confirm-paid" -Token $adminToken -Body @{ note = "phase1 smoke" } -Optional | Out-Null
        Invoke-Api -Name "人工恢复待支付" -Method POST -Path "/api/admin/orders/$OrderNo/restore-pending-payment" -Token $adminToken -Body @{ note = "phase1 smoke" } -Optional | Out-Null
        Invoke-Api -Name "后台发货（二期提前实现，供自动确认收货链路冒烟）" -Method PATCH -Path "/api/admin/orders/$OrderNo/ship" -Token $adminToken -Optional | Out-Null
        Invoke-Api -Name "后台完结订单" -Method PATCH -Path "/api/admin/orders/$OrderNo/complete" -Token $adminToken -Optional | Out-Null
    } else {
        Write-Skip "未发现 OrderNo，跳过订单异常与支付异常写操作"
    }
}

Write-Step "一期-支付状态同步、退款与支付回调"
if ($OrderNo) {
    Invoke-Api -Name "创建/查询支付单前置" -Method POST -Path "/api/pay/orders" -Token $userToken -Query @{ orderNo = $OrderNo; payChannel = "MOCK" } -Optional | Out-Null
    Invoke-Api -Name "通用支付渠道回调" -Method POST -Path "/api/pay/callback/channel" -Body @{ orderNo = $OrderNo; channel = "MOCK"; tradeStatus = "SUCCESS"; outTradeNo = $OrderNo; tradeNo = "SMOKE-$OrderNo"; totalAmountCent = 1 } -Optional | Out-Null
    Invoke-Api -Name "支付状态同步" -Method POST -Path "/api/pay/orders/$OrderNo/sync-status" -Token $userToken -Optional | Out-Null
    Invoke-Api -Name "补偿已成功支付订单状态" -Method POST -Path "/api/pay/orders/$OrderNo/repair-paid" -Token $userToken -Optional | Out-Null
    if ($adminToken) {
        Invoke-Api -Name "后台支付单列表" -Method GET -Path "/api/admin/pays" -Token $adminToken -Query @{ page = 1; size = 10 } -Optional | Out-Null
        Invoke-Api -Name "后台支付单详情" -Method GET -Path "/api/admin/pays/$OrderNo" -Token $adminToken -Optional | Out-Null
        Invoke-Api -Name "后台同步订单支付状态" -Method POST -Path "/api/admin/pays/$OrderNo/sync-status" -Token $adminToken -Optional | Out-Null
        Invoke-Api -Name "后台补偿已成功支付订单状态" -Method POST -Path "/api/admin/pays/$OrderNo/repair-paid" -Token $adminToken -Optional | Out-Null
        Invoke-Api -Name "查询支付回调记录" -Method GET -Path "/api/admin/pays/callback-records" -Token $adminToken -Query @{ page = 1; size = 10 } -Optional | Out-Null
        Invoke-Api -Name "全局退款单查询" -Method GET -Path "/api/admin/pays/refunds" -Token $adminToken -Query @{ page = 1; size = 10 } -Optional | Out-Null
        Invoke-Api -Name "订单退款单查询" -Method GET -Path "/api/admin/pays/$OrderNo/refunds" -Token $adminToken -Query @{ page = 1; size = 10 } -Optional | Out-Null
    }
    if ($RefundNo) {
        Invoke-Api -Name "退款状态同步" -Method POST -Path "/api/pay/orders/$OrderNo/refunds/$RefundNo/sync-status" -Token $userToken -Optional | Out-Null
        if ($adminToken) { Invoke-Api -Name "后台同步退款状态" -Method POST -Path "/api/admin/pays/$OrderNo/refunds/$RefundNo/sync-status" -Token $adminToken -Optional | Out-Null }
    } else {
        Write-Skip "未传入 RefundNo，跳过单笔退款状态同步"
    }
} else {
    Write-Skip "未发现 OrderNo，跳过支付同步与退款接口"
}

Write-Step "一期-售后与仅退款"
if ($userToken) {
    Invoke-Api -Name "用户售后申请查询" -Method GET -Path "/api/aftersales" -Token $userToken -Optional | Out-Null
    if ($OrderNo) {
        Invoke-Api -Name "用户仅退款售后申请" -Method POST -Path "/api/aftersales/refund" -Token $userToken -Body @{ orderNo = $OrderNo; reason = "phase1 smoke"; refundAmountCent = 1; description = "phase1 smoke" } -Optional | Out-Null
    }
}
if ($adminToken) {
    $aftersales = Invoke-Api -Name "后台售后列表" -Method GET -Path "/api/admin/aftersales" -Token $adminToken -Query @{ page = 1; size = 10 } -Optional
    if (-not $AftersaleNo) {
        $firstAftersale = Get-FirstArrayItem $aftersales.data
        $detectedAftersaleNo = Pick-FirstId $firstAftersale @("aftersaleNo")
        if ($detectedAftersaleNo) { $AftersaleNo = [string]$detectedAftersaleNo }
    }
    if ($AftersaleNo) {
        Invoke-Api -Name "后台售后详情" -Method GET -Path "/api/admin/aftersales/$AftersaleNo" -Token $adminToken -Optional | Out-Null
        Invoke-Api -Name "后台售后审核" -Method POST -Path "/api/admin/aftersales/$AftersaleNo/review" -Token $adminToken -Body @{ approved = $false; reason = "phase1 smoke reject to avoid real refund"; remark = "phase1 smoke" } -Optional | Out-Null
    } else {
        Write-Skip "未发现 AftersaleNo，跳过售后详情与审核"
    }
}

Write-Step "一期-库存预占、确认、释放、日志与预警"
if ($adminToken) {
    Invoke-Api -Name "后台库存列表" -Method GET -Path "/api/admin/stocks" -Token $adminToken -Query @{ page = 1; size = 20 } -Optional | Out-Null
    Invoke-Api -Name "库存预警列表" -Method GET -Path "/api/admin/stocks/warnings" -Token $adminToken -Query @{ page = 1; size = 20 } -Optional | Out-Null
    Invoke-Api -Name "库存操作日志查询" -Method GET -Path "/api/admin/stocks/logs" -Token $adminToken -Query @{ page = 1; size = 20 } -Optional | Out-Null
    Invoke-Api -Name "库存策略调整" -Method PUT -Path "/api/admin/stocks/$SkuId/policy" -Token $adminToken -Body @{ lowStockThreshold = 5; highStockThreshold = 999; stockStatus = "NORMAL" } -Optional | Out-Null
    Invoke-Api -Name "库存预警处理" -Method POST -Path "/api/admin/stocks/$SkuId/warning-handle" -Token $adminToken -Body @{ remark = "phase1 smoke"; handled = $true } -Optional | Out-Null
}
$stockBizNo = "SMOKE-STOCK-" + (Get-Date -Format "yyyyMMddHHmmss")
$stockBody = @{ skuId = $SkuId; quantity = 1; bizNo = $stockBizNo; bizType = "SMOKE" }
Invoke-Api -Name "库存内部健康检查" -Method GET -Path "/api/internal/stocks/health" -Token $adminToken -Optional | Out-Null
Invoke-Api -Name "查询SKU库存" -Method GET -Path "/api/internal/stocks/$SkuId" -Token $adminToken -Optional | Out-Null
Invoke-Api -Name "库存预占" -Method POST -Path "/api/internal/stocks/reserve" -Token $adminToken -Body $stockBody -Optional | Out-Null
Invoke-Api -Name "库存确认" -Method PUT -Path "/api/internal/stocks/confirm" -Token $adminToken -Body $stockBody -Optional | Out-Null
Invoke-Api -Name "库存锁定" -Method POST -Path "/api/internal/stocks/lock" -Token $adminToken -Body $stockBody -Optional | Out-Null
Invoke-Api -Name "库存释放" -Method DELETE -Path "/api/internal/stocks/release" -Token $adminToken -Body $stockBody -Optional | Out-Null
Invoke-Api -Name "库存回补" -Method PUT -Path "/api/internal/stocks/restore" -Token $adminToken -Body $stockBody -Optional | Out-Null
Invoke-Api -Name "库存扣减" -Method PATCH -Path "/api/internal/stocks/deduct" -Token $adminToken -Body $stockBody -Optional | Out-Null
Invoke-Api -Name "取消库存预占" -Method DELETE -Path "/api/internal/stocks/cancel" -Token $adminToken -Body $stockBody -Optional | Out-Null

Write-Step "一期/增强-基础支付退款对账与库存对账"
if ($adminToken) {
    Invoke-Api -Name "后台对账概览" -Method GET -Path "/api/admin/reconciliations/overview" -Token $adminToken -Optional | Out-Null
    Invoke-Api -Name "后台对账列表" -Method GET -Path "/api/admin/reconciliations" -Token $adminToken -Query @{ page = 1; size = 10 } -Optional | Out-Null
    if ($OrderNo) {
        Invoke-Api -Name "执行单笔对账" -Method POST -Path "/api/admin/reconciliations/$OrderNo/run" -Token $adminToken -Optional | Out-Null
        Invoke-Api -Name "处理对账异常" -Method POST -Path "/api/admin/reconciliations/$OrderNo/handle" -Token $adminToken -Body @{ action = "IGNORE"; remark = "phase1 smoke" } -Optional | Out-Null
    }
    $payRecords = Invoke-Api -Name "支付/退款对账记录查询" -Method GET -Path "/api/admin/reconciliations/pay-records" -Token $adminToken -Query @{ page = 1; size = 10 } -Optional
    if ($PayReconciliationRecordId -le 0) {
        $firstPayRecord = Get-FirstArrayItem $payRecords.data
        $detectedPayRecordId = Pick-FirstId $firstPayRecord @("id", "recordId")
        if ($detectedPayRecordId) { $PayReconciliationRecordId = [long]$detectedPayRecordId }
    }
    Invoke-IfId -Name "支付/退款对账记录详情" -Id $PayReconciliationRecordId -Call { Invoke-Api -Name "支付/退款对账记录详情" -Method GET -Path "/api/admin/reconciliations/pay-records/$PayReconciliationRecordId" -Token $adminToken -Optional | Out-Null }
    Invoke-IfId -Name "业务化支付/退款对账处置" -Id $PayReconciliationRecordId -Call { Invoke-Api -Name "业务化支付/退款对账处置" -Method POST -Path "/api/admin/reconciliations/pay-records/$PayReconciliationRecordId/handle" -Token $adminToken -Body @{ action = "IGNORE"; remark = "phase1 smoke" } -Optional | Out-Null }
    Invoke-IfId -Name "标记对账记录已处理" -Id $PayReconciliationRecordId -Call { Invoke-Api -Name "标记对账记录已处理" -Method POST -Path "/api/admin/reconciliations/pay-records/$PayReconciliationRecordId/done" -Token $adminToken -Body @{ remark = "phase1 smoke" } -Optional | Out-Null }
    Invoke-IfId -Name "忽略对账记录" -Id $PayReconciliationRecordId -Call { Invoke-Api -Name "忽略对账记录" -Method POST -Path "/api/admin/reconciliations/pay-records/$PayReconciliationRecordId/ignore" -Token $adminToken -Body @{ remark = "phase1 smoke" } -Optional | Out-Null }

    Invoke-Api -Name "发起库存一致性校验" -Method POST -Path "/api/admin/stocks/$SkuId/consistency-check" -Token $adminToken -Optional | Out-Null
    $stockRecords = Invoke-Api -Name "库存对账列表" -Method GET -Path "/api/admin/reconciliations/stocks" -Token $adminToken -Query @{ page = 1; size = 10 } -Optional
    if ($StockReconciliationRecordId -le 0) {
        $firstStockRecord = Get-FirstArrayItem $stockRecords.data
        $detectedStockRecordId = Pick-FirstId $firstStockRecord @("id", "recordId")
        if ($detectedStockRecordId) { $StockReconciliationRecordId = [long]$detectedStockRecordId }
    }
    Invoke-IfId -Name "库存对账详情" -Id $StockReconciliationRecordId -Call { Invoke-Api -Name "库存对账详情" -Method GET -Path "/api/admin/reconciliations/stocks/$StockReconciliationRecordId" -Token $adminToken -Optional | Out-Null }
    Invoke-IfId -Name "忽略库存差异" -Id $StockReconciliationRecordId -Call { Invoke-Api -Name "忽略库存差异" -Method POST -Path "/api/admin/reconciliations/stocks/$StockReconciliationRecordId/ignore" -Token $adminToken -Body @{ remark = "phase1 smoke" } -Optional | Out-Null }
}

Write-Step "一期/增强-线上人工对账、挂账跟进与归档报表"
if ($adminToken) {
    $tasks = Invoke-Api -Name "查询线上人工对账任务" -Method GET -Path "/api/admin/online-reconcile-tasks" -Token $adminToken -Query @{ page = 1; size = 10 } -Optional
    if ($TaskId -le 0) {
        $firstTask = Get-FirstArrayItem $tasks.data
        $detectedTaskId = Pick-FirstId $firstTask @("taskId", "id")
        if ($detectedTaskId) { $TaskId = [long]$detectedTaskId }
    }
    $createTask = Invoke-Api -Name "创建线上人工对账任务" -Method POST -Path "/api/admin/online-reconcile-tasks" -Token $adminToken -Body @{ taskName = "phase1 smoke"; channel = "MOCK"; bizType = "PAY"; startDate = (Get-Date).AddDays(-1).ToString("yyyy-MM-dd"); endDate = (Get-Date).ToString("yyyy-MM-dd"); remark = "phase1 smoke" } -Optional
    $createdTaskId = Pick-FirstId $createTask.data @("taskId", "id")
    if ($createdTaskId) { $TaskId = [long]$createdTaskId }

    Invoke-IfId -Name "线上人工对账任务详情" -Id $TaskId -Call { Invoke-Api -Name "线上人工对账任务详情" -Method GET -Path "/api/admin/online-reconcile-tasks/$TaskId" -Token $adminToken -Optional | Out-Null }
    Invoke-IfId -Name "生成本地账单快照" -Id $TaskId -Call { Invoke-Api -Name "生成本地账单快照" -Method POST -Path "/api/admin/online-reconcile-tasks/$TaskId/local-bills/generate" -Token $adminToken -Optional | Out-Null }
    Invoke-IfId -Name "查询本地账单" -Id $TaskId -Call { Invoke-Api -Name "查询本地账单" -Method GET -Path "/api/admin/online-reconcile-tasks/$TaskId/local-bills" -Token $adminToken -Query @{ page = 1; size = 10 } -Optional | Out-Null }
    Invoke-IfId -Name "生成Mock渠道账单" -Id $TaskId -Call { Invoke-Api -Name "生成Mock渠道账单" -Method POST -Path "/api/admin/online-reconcile-tasks/$TaskId/channel-bills/mock-generate" -Token $adminToken -Body @{ mode = "MATCH_LOCAL" } -Optional | Out-Null }
    Invoke-IfId -Name "查询渠道账单" -Id $TaskId -Call { Invoke-Api -Name "查询渠道账单" -Method GET -Path "/api/admin/online-reconcile-tasks/$TaskId/channel-bills" -Token $adminToken -Query @{ page = 1; size = 10 } -Optional | Out-Null }
    Invoke-IfId -Name "执行自动勾兑" -Id $TaskId -Call { Invoke-Api -Name "执行自动勾兑" -Method POST -Path "/api/admin/online-reconcile-tasks/$TaskId/match" -Token $adminToken -Optional | Out-Null }
    $diffItems = $null
    Invoke-IfId -Name "查询差异明细" -Id $TaskId -Call { $script:LastDiffItems = Invoke-Api -Name "查询差异明细" -Method GET -Path "/api/admin/online-reconcile-tasks/$TaskId/diff-items" -Token $adminToken -Query @{ page = 1; size = 10 } -Optional }
    if ($DiffId -le 0 -and $script:LastDiffItems) {
        $firstDiff = Get-FirstArrayItem $script:LastDiffItems.data
        $detectedDiffId = Pick-FirstId $firstDiff @("diffId", "id")
        if ($detectedDiffId) { $DiffId = [long]$detectedDiffId }
    }
    Invoke-IfId -Name "差异详情" -Id $DiffId -Call { Invoke-Api -Name "差异详情" -Method GET -Path "/api/admin/online-reconcile-diff-items/$DiffId" -Token $adminToken -Optional | Out-Null }
    Invoke-IfId -Name "处理对账差异" -Id $DiffId -Call { Invoke-Api -Name "处理对账差异" -Method POST -Path "/api/admin/online-reconcile-diff-items/$DiffId/handle" -Token $adminToken -Body @{ action = "IGNORE"; remark = "phase1 smoke" } -Optional | Out-Null }
    Invoke-IfId -Name "差异操作日志" -Id $DiffId -Call { Invoke-Api -Name "差异操作日志" -Method GET -Path "/api/admin/online-reconcile-diff-items/$DiffId/logs" -Token $adminToken -Query @{ page = 1; size = 20 } -Optional | Out-Null }
    Invoke-Api -Name "挂账跟进列表" -Method GET -Path "/api/admin/online-reconcile-hangings" -Token $adminToken -Query @{ page = 1; size = 10 } -Optional | Out-Null
    Invoke-IfId -Name "新增挂账跟进记录" -Id $DiffId -Call { Invoke-Api -Name "新增挂账跟进记录" -Method POST -Path "/api/admin/online-reconcile-diff-items/$DiffId/follow-up" -Token $adminToken -Body @{ remark = "phase1 smoke" } -Optional | Out-Null }
    Invoke-IfId -Name "挂账转财务调账" -Id $DiffId -Call { Invoke-Api -Name "挂账转财务调账" -Method POST -Path "/api/admin/online-reconcile-diff-items/$DiffId/transfer-finance" -Token $adminToken -Body @{ remark = "phase1 smoke" } -Optional | Out-Null }
    Invoke-IfId -Name "挂账完结闭环" -Id $DiffId -Call { Invoke-Api -Name "挂账完结闭环" -Method POST -Path "/api/admin/online-reconcile-diff-items/$DiffId/close-hanging" -Token $adminToken -Body @{ remark = "phase1 smoke" } -Optional | Out-Null }
    Invoke-Api -Name "对账归档报表查询" -Method GET -Path "/api/admin/online-reconcile-archive-report" -Token $adminToken -Optional | Out-Null
    Invoke-IfId -Name "任务操作日志" -Id $TaskId -Call { Invoke-Api -Name "任务操作日志" -Method GET -Path "/api/admin/online-reconcile-tasks/$TaskId/logs" -Token $adminToken -Query @{ page = 1; size = 20 } -Optional | Out-Null }
    Invoke-IfId -Name "归档对账任务" -Id $TaskId -Call { Invoke-Api -Name "归档对账任务" -Method POST -Path "/api/admin/online-reconcile-tasks/$TaskId/complete" -Token $adminToken -Body @{ remark = "phase1 smoke" } -Optional | Out-Null }
}

Write-Step "一期/增强-Dashboard增强、权限与审计"
if ($adminToken) {
    Invoke-Api -Name "基础Dashboard概览" -Method GET -Path "/api/admin/dashboard" -Token $adminToken -Optional | Out-Null
    Invoke-Api -Name "财务累计净收入" -Method GET -Path "/api/admin/dashboard/finance-cumulative-net-income" -Token $adminToken -Optional | Out-Null
    Invoke-Api -Name "近7日财务资金与对账风险趋势" -Method GET -Path "/api/admin/dashboard/finance-trend" -Token $adminToken -Optional | Out-Null
    Invoke-Api -Name "近7日仓储库存健康与发货压力趋势" -Method GET -Path "/api/admin/dashboard/warehouse-trend" -Token $adminToken -Optional | Out-Null
    Invoke-Api -Name "商品销售表现阈值查询" -Method GET -Path "/api/admin/products/sales-threshold-config" -Token $adminToken -Optional | Out-Null
    Invoke-Api -Name "后台账号列表" -Method GET -Path "/api/admin/accounts" -Token $adminToken -Query @{ page = 1; size = 20 } -Optional | Out-Null
    Invoke-Api -Name "角色列表" -Method GET -Path "/api/admin/roles" -Token $adminToken -Optional | Out-Null
    Invoke-Api -Name "权限目录" -Method GET -Path "/api/admin/permissions" -Token $adminToken -Optional | Out-Null
    Invoke-Api -Name "权限模板" -Method GET -Path "/api/admin/accounts/permission-templates" -Token $adminToken -Optional | Out-Null
    Invoke-Api -Name "后台操作日志" -Method GET -Path "/api/admin/operation-logs" -Token $adminToken -Query @{ page = 1; size = 20 } -Optional | Out-Null
}

Write-Step "一期及一期增强冒烟测试完成"
Write-Host "通过: $script:Passed, 跳过: $script:Skipped, 失败: $script:Failed" -ForegroundColor Cyan
if ($OrderNo) { Write-Host "OrderNo: $OrderNo" -ForegroundColor Green }
if ($TaskId -gt 0) { Write-Host "OnlineReconcileTaskId: $TaskId" -ForegroundColor Green }
if ($script:Failed -gt 0) { exit 1 }




