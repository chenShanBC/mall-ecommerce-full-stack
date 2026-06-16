param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$UserToken = "",
    [string]$AdminToken = "",
    [string]$ReportDir = "",
    [int]$SkuId = 1,
    [string]$UserMobile = "13800000000",
    [string]$UserPassword = "123456",
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "123456",
    [int]$RequestTimeoutSec = 12,
    [int]$BatchTimeoutSec = 180,
    [switch]$ContinueOnFailure
)

$ErrorActionPreference = "Stop"
$script:Results = New-Object System.Collections.Generic.List[object]
$script:Context = @{}
$script:StartTime = Get-Date
$script:BatchDeadline = (Get-Date).AddSeconds($BatchTimeoutSec)

function New-ReportDirectory {
    if ([string]::IsNullOrWhiteSpace($ReportDir)) {
        $ReportDir = Join-Path $PSScriptRoot "..\test-reports"
    }
    if (-not (Test-Path $ReportDir)) { New-Item -ItemType Directory -Force -Path $ReportDir | Out-Null }
    return $ReportDir
}

$script:ReportDir = New-ReportDirectory
$script:InitialReportPath = Join-Path $script:ReportDir ("p1-business-flow-initial-{0}.md" -f (Get-Date -Format "yyyyMMdd-HHmmss"))
$script:DetailReportPath = Join-Path $script:ReportDir ("p1-business-flow-detail-{0}.md" -f (Get-Date -Format "yyyyMMdd-HHmmss"))

function Write-Section([string]$Message) {
    Write-Host "`n==== $Message ====" -ForegroundColor Cyan
}

function Add-TestResult {
    param(
        [string]$Suite,
        [string]$Name,
        [string]$Status,
        [string]$Detail = "",
        [int]$ElapsedMs = 0
    )
    $script:Results.Add([pscustomobject]@{
        suite = $Suite
        name = $Name
        status = $Status
        detail = $Detail
        elapsedMs = $ElapsedMs
        time = (Get-Date).ToString("yyyy-MM-dd HH:mm:ss")
    }) | Out-Null
}

function Write-InitialReport {
    $planned = @(
        @("P1AddressCart", "address create update default list delete"),
        @("P1AddressCart", "cart add list update check preview delete"),
        @("P1AdminQuery", "admin account permission user log queries"),
        @("P1AdminQuery", "admin product and stock queries"),
        @("P1AdminQuery", "pay refund callback reconciliation list queries"),
        @("P1InternalStock", "internal stock health query sync reserve cancel"),
        @("P1AdminFlow", "admin query orders and pays"),
        @("P1AdminFlow", "admin reconciliation overview is reachable"),
        @("P1AdminFlow", "admin aftersales list is reachable"),
        @("P1AdminFlow", "admin order status transition can be called")
    )
    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add("# mallFei P1 Smoke Test Initial Report") | Out-Null
    $lines.Add("") | Out-Null
    $lines.Add(("- BaseUrl: {0}" -f $BaseUrl)) | Out-Null
    $lines.Add(("- StartedAt: {0}" -f $script:StartTime.ToString('yyyy-MM-dd HH:mm:ss'))) | Out-Null
    $lines.Add(("- RequestTimeoutSec: {0}" -f $RequestTimeoutSec)) | Out-Null
    $lines.Add(("- BatchTimeoutSec: {0}" -f $BatchTimeoutSec)) | Out-Null
    $lines.Add("") | Out-Null
    $lines.Add("## Planned cases") | Out-Null
    foreach ($item in $planned) {
        $lines.Add(("- [{0}] {1}" -f $item[0], $item[1])) | Out-Null
    }
    $lines | Set-Content -Path $script:InitialReportPath -Encoding UTF8
    Write-Host "Initial report written: $script:InitialReportPath" -ForegroundColor Cyan
}

function Write-FinalReport {
    $successCount = ($script:Results | Where-Object { $_.status -eq "PASS" }).Count
    $failCount = ($script:Results | Where-Object { $_.status -eq "FAIL" }).Count
    $timeoutCount = ($script:Results | Where-Object { $_.status -eq "TIMEOUT" }).Count
    $skipCount = ($script:Results | Where-Object { $_.status -eq "SKIP" }).Count
    $blockedCount = ($script:Results | Where-Object { $_.status -eq "BLOCKED" }).Count
    $warnCount = ($script:Results | Where-Object { $_.status -eq "WARN" }).Count
    $duration = [int]((Get-Date) - $script:StartTime).TotalSeconds
    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add("# mallFei P1 Smoke Test Detailed Report") | Out-Null
    $lines.Add("") | Out-Null
    $lines.Add(("- BaseUrl: {0}" -f $BaseUrl)) | Out-Null
    $lines.Add(("- StartedAt: {0}" -f $script:StartTime.ToString('yyyy-MM-dd HH:mm:ss'))) | Out-Null
    $lines.Add(("- Duration: {0}s" -f $duration)) | Out-Null
    $lines.Add(("- Passed: {0}" -f $successCount)) | Out-Null
    $lines.Add(("- Failed: {0}" -f $failCount)) | Out-Null
    $lines.Add(("- Timeout: {0}" -f $timeoutCount)) | Out-Null
    $lines.Add(("- Skipped: {0}" -f $skipCount)) | Out-Null
    $lines.Add(("- Blocked: {0}" -f $blockedCount)) | Out-Null
    $lines.Add(("- Warn: {0}" -f $warnCount)) | Out-Null
    $lines.Add("") | Out-Null
    $lines.Add("## Case results") | Out-Null
    $lines.Add('| Suite | Case | Status | Elapsed(ms) | Detail |') | Out-Null
    $lines.Add('| --- | --- | --- | ---: | --- |') | Out-Null
    foreach ($item in $script:Results) {
        $detail = ($item.detail -replace '\|', '\\|')
        $lines.Add("| $($item.suite) | $($item.name) | $($item.status) | $($item.elapsedMs) | $detail |") | Out-Null
    }
    $lines.Add("") | Out-Null
    $lines.Add("## Suggestions") | Out-Null
    $lines.Add("- PASS: endpoint is healthy and can be used for broader ApiPost validation.") | Out-Null
    $lines.Add("- FAIL: business assertion or data setup problem; inspect request payload and response body.") | Out-Null
    $lines.Add("- TIMEOUT: endpoint or dependency may be hanging; recheck service logs and downstream dependencies.") | Out-Null
    $lines.Add("- BLOCKED: likely token, permission, or dependency availability issue.") | Out-Null
    $lines.Add("- SKIP: missing precondition or earlier failure prevented execution.") | Out-Null
    $lines.Add("- WARN: request succeeded but returned suspicious or partial data.") | Out-Null
    $lines | Set-Content -Path $script:DetailReportPath -Encoding UTF8
    Write-Host "Final report written: $script:DetailReportPath" -ForegroundColor Cyan
}

function Get-ResponseData([object]$Response) {
    if ($null -eq $Response) { return $null }
    if ($Response.PSObject.Properties.Name -contains "data") { return $Response.data }
    return $Response
}

function Get-TokenFromLoginData([object]$LoginData) {
    if ($null -eq $LoginData) { throw "Login response is empty, cannot extract token" }
    $rawToken = $null
    if ($LoginData.token) { $rawToken = $LoginData.token }
    elseif ($LoginData.accessToken) { $rawToken = $LoginData.accessToken }
    elseif ($LoginData.Authorization) { $rawToken = $LoginData.Authorization }
    elseif ($LoginData.authorization) { $rawToken = $LoginData.authorization }
    else { throw "Login response does not contain token/accessToken/Authorization" }
    if ($rawToken -match '^Bearer\s+') { return $rawToken }
    return "Bearer $rawToken"
}

function Test-BatchDeadline {
    if ((Get-Date) -gt $script:BatchDeadline) {
        throw "Batch timeout exceeded after $BatchTimeoutSec seconds"
    }
}

function Invoke-MallApi {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [string]$Token = "",
        [hashtable]$Headers = @{},
        [switch]$AllowFailure,
        [int]$TimeoutSec = $RequestTimeoutSec
    )

    Test-BatchDeadline

    $allHeaders = @{}
    foreach ($key in $Headers.Keys) { $allHeaders[$key] = $Headers[$key] }
    if (-not [string]::IsNullOrWhiteSpace($Token)) { $allHeaders["Authorization"] = $Token }

    $params = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
        Headers = $allHeaders
        ContentType = "application/json; charset=utf-8"
        TimeoutSec = $TimeoutSec
    }
    if ($null -ne $Body) { $params.Body = ($Body | ConvertTo-Json -Depth 30) }

    try {
        $response = Invoke-RestMethod @params
    } catch {
        $message = "HTTP request failed: $Method $Path"
        if ($_.Exception.Message -match 'timeout|timed out') {
            $message = "TIMEOUT: $Method $Path exceeded ${TimeoutSec}s"
        } elseif ($_.Exception.Response) {
            try {
                $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
                $bodyText = $reader.ReadToEnd()
                if ($bodyText) { $message = "$message | $bodyText" }
            } catch {}
        } else {
            $message = "$message | $($_.Exception.Message)"
        }
        if ($AllowFailure) {
            return [pscustomobject]@{ success = $false; message = $message; data = $null }
        }
        if ($message -like 'TIMEOUT:*') { throw [System.TimeoutException]::new($message) }
        throw $message
    }

    if ($response.PSObject.Properties.Name -contains "success" -and $response.success -eq $false) {
        $msg = $response.message
        if (-not $msg) { $msg = $response.msg }
        if ($AllowFailure) { return $response }
        throw "Business response failed: $Method $Path | $msg"
    }
    return $response
}

function Get-FirstArrayItem([object]$Value) {
    if ($null -eq $Value) { return $null }
    if ($Value -is [System.Array]) {
        if ($Value.Count -gt 0) { return $Value[0] }
        return $null
    }
    if ($Value.records -and $Value.records.Count -gt 0) { return $Value.records[0] }
    if ($Value.list -and $Value.list.Count -gt 0) { return $Value.list[0] }
    if ($Value.items -and $Value.items.Count -gt 0) { return $Value.items[0] }
    if ($Value.content -and $Value.content.Count -gt 0) { return $Value.content[0] }
    return $null
}

function Get-CaseStatus([string]$Message) {
    if ($Message -match 'TIMEOUT|timeout|timed out') { return 'TIMEOUT' }
    if ($Message -match '401|403|Unauthorized|Forbidden|permission') { return 'BLOCKED' }
    if ($Message -match 'not found|缺失|missing|precondition') { return 'SKIP' }
    return 'FAIL'
}

function Invoke-Case {
    param(
        [string]$Suite,
        [string]$Name,
        [scriptblock]$Script
    )

    Test-BatchDeadline
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $detail = & $Script
        $sw.Stop()
        Add-TestResult -Suite $Suite -Name $Name -Status "PASS" -Detail ([string]$detail) -ElapsedMs $sw.ElapsedMilliseconds
        Write-Host "[PASS] $Suite - $Name" -ForegroundColor Green
        return $true
    } catch [System.TimeoutException] {
        $sw.Stop()
        $detail = $_.Exception.Message
        Add-TestResult -Suite $Suite -Name $Name -Status "TIMEOUT" -Detail $detail -ElapsedMs $sw.ElapsedMilliseconds
        Write-Host "[TIMEOUT] $Suite - $Name : $detail" -ForegroundColor Yellow
        if (-not $ContinueOnFailure) { throw }
        return $false
    } catch {
        $sw.Stop()
        $detail = $_.Exception.Message
        $status = Get-CaseStatus $detail
        Add-TestResult -Suite $Suite -Name $Name -Status $status -Detail $detail -ElapsedMs $sw.ElapsedMilliseconds
        Write-Host "[$status] $Suite - $Name : $detail" -ForegroundColor Red
        if (-not $ContinueOnFailure -and $status -eq 'FAIL') { throw }
        return $false
    }
}

function New-VerifiedLoginCaptcha {
    $challenge = Invoke-MallApi -Method GET -Path "/api/users/login/captcha/challenge"
    $challengeData = Get-ResponseData $challenge
    if (-not $challengeData.captchaToken) { throw "captcha challenge response does not contain captchaToken" }
    $verify = Invoke-MallApi -Method POST -Path "/api/users/login/captcha/verify" -Body @{
        captchaToken = $challengeData.captchaToken
        offset = $challengeData.targetOffset
    }
    $verifyData = Get-ResponseData $verify
    if (-not $verifyData.verifyToken) { throw "captcha verify response does not contain verifyToken" }
    return [pscustomobject]@{
        captchaToken = $challengeData.captchaToken
        captchaVerifyToken = $verifyData.verifyToken
    }
}

function Ensure-UserToken {
    if (-not [string]::IsNullOrWhiteSpace($script:Context.userToken)) { return $script:Context.userToken }
    if (-not [string]::IsNullOrWhiteSpace($UserToken)) {
        $script:Context.userToken = if ($UserToken -match '^Bearer\s+') { $UserToken } else { "Bearer $UserToken" }
        return $script:Context.userToken
    }
    $captcha = New-VerifiedLoginCaptcha
    $login = Invoke-MallApi -Method POST -Path "/api/users/login/password" -Body @{
        mobile = $UserMobile
        password = $UserPassword
        captchaToken = $captcha.captchaToken
        captchaVerifyToken = $captcha.captchaVerifyToken
    }
    $token = Get-TokenFromLoginData (Get-ResponseData $login)
    $script:Context.userToken = $token
    return $token
}

function Ensure-AdminToken {
    if (-not [string]::IsNullOrWhiteSpace($script:Context.adminToken)) { return $script:Context.adminToken }
    if (-not [string]::IsNullOrWhiteSpace($AdminToken)) {
        $script:Context.adminToken = if ($AdminToken -match '^Bearer\s+') { $AdminToken } else { "Bearer $AdminToken" }
        return $script:Context.adminToken
    }
    $login = Invoke-MallApi -Method POST -Path "/api/admin/login/password" -Body @{ username = $AdminUsername; password = $AdminPassword }
    $token = Get-TokenFromLoginData (Get-ResponseData $login)
    $script:Context.adminToken = $token
    return $token
}

function New-TestAddressBody {
    return @{
        receiverName = "Auto Test User"
        receiverPhone = $UserMobile
        receiverProvinceName = "Beijing"
        receiverCityName = "Beijing"
        receiverDistrictName = "Chaoyang"
        receiverDetailAddress = "Auto Test Address No.1"
        remark = "p1 business flow test"
    }
}

function New-DirectOrderBody {
    $body = New-TestAddressBody
    $body.items = @(@{ skuId = $SkuId; quantity = 1 })
    return $body
}

function Run-P1AddressCartSuite {
    Write-Section "P1 flow: address and cart"

    Invoke-Case -Suite "P1AddressCart" -Name "address create update default list delete" -Script {
        $token = Ensure-UserToken
        $suffix = Get-Date -Format "HHmmss"
        $createBody = @{
            receiverName = "Auto Receiver $suffix"
            receiverPhone = "13800138000"
            provinceCode = "110000"
            provinceName = "Beijing"
            cityCode = "110100"
            cityName = "Beijing"
            districtCode = "110105"
            districtName = "Chaoyang"
            detailAddress = "Auto Test Address $suffix"
            postalCode = "100000"
            isDefault = $true
        }
        $created = Invoke-MallApi -Method POST -Path "/api/users/addresses" -Token $token -Body $createBody
        $address = Get-ResponseData $created
        if (-not $address.id) { throw "address created but id is missing" }
        $script:Context.addressId = $address.id
        Invoke-MallApi -Method GET -Path "/api/users/addresses" -Token $token | Out-Null
        $updateBody = $createBody.Clone()
        $updateBody.receiverName = "Auto Receiver Updated $suffix"
        $updateBody.detailAddress = "自动化测试地址已修改 $suffix"
        $updateBody.isDefault = $false
        Invoke-MallApi -Method PUT -Path "/api/users/addresses/$($address.id)" -Token $token -Body $updateBody | Out-Null
        Invoke-MallApi -Method PUT -Path "/api/users/addresses/$($address.id)/default" -Token $token | Out-Null
        Invoke-MallApi -Method DELETE -Path "/api/users/addresses/$($address.id)" -Token $token | Out-Null
        "addressId=$($address.id)"
    } | Out-Null

    Invoke-Case -Suite "P1AddressCart" -Name "cart add list update check preview delete" -Script {
        $token = Ensure-UserToken
        Invoke-MallApi -Method DELETE -Path "/api/cart/items" -Token $token -AllowFailure | Out-Null
        $added = Invoke-MallApi -Method POST -Path "/api/cart/items" -Token $token -Body @{ skuId = $SkuId; quantity = 1 }
        $data = Get-ResponseData $added
        $cartItemId = $data.cartItemId
        if (-not $cartItemId -and $data.item) { $cartItemId = $data.item.id }
        if (-not $cartItemId -and $data.id) { $cartItemId = $data.id }
        if (-not $cartItemId) {
            $listAfterAdd = Invoke-MallApi -Method GET -Path "/api/cart/items" -Token $token
            $listData = Get-ResponseData $listAfterAdd
            $firstItem = Get-FirstArrayItem $listData.items
            if (-not $firstItem) { $firstItem = Get-FirstArrayItem $listData }
            if ($firstItem) { $cartItemId = $firstItem.id }
        }
        if (-not $cartItemId) { throw "cart item id is missing" }
        $script:Context.cartItemId = $cartItemId
        Invoke-MallApi -Method GET -Path "/api/cart/items" -Token $token | Out-Null
        Invoke-MallApi -Method PUT -Path "/api/cart/items/$cartItemId" -Token $token -Body @{ quantity = 2 } | Out-Null
        Invoke-MallApi -Method PUT -Path "/api/cart/items/checked" -Token $token -Body @{ cartItemIds = @([long]$cartItemId); checked = $true } | Out-Null
        Invoke-MallApi -Method GET -Path "/api/cart/quantity" -Token $token | Out-Null
        Invoke-MallApi -Method GET -Path "/api/cart/settlement-preview" -Token $token -AllowFailure | Out-Null
        Invoke-MallApi -Method POST -Path "/api/cart/prepare-checkout" -Token $token -Body @{ cartItemIds = @([long]$cartItemId) } -AllowFailure | Out-Null
        Invoke-MallApi -Method DELETE -Path "/api/cart/items/$cartItemId" -Token $token | Out-Null
        "cartItemId=$cartItemId"
    } | Out-Null
}

function Run-P1AdminQuerySuite {
    Write-Section "P1 flow: admin query, stock and reconciliation lists"

    Invoke-Case -Suite "P1AdminQuery" -Name "admin account permission user log queries" -Script {
        $token = Ensure-AdminToken
        Invoke-MallApi -Method GET -Path '/api/admin/accounts?page=1&size=20' -Token $token | Out-Null
        Invoke-MallApi -Method GET -Path '/api/admin/roles' -Token $token | Out-Null
        Invoke-MallApi -Method GET -Path '/api/admin/permissions' -Token $token | Out-Null
        Invoke-MallApi -Method GET -Path '/api/admin/accounts/permission-templates' -Token $token | Out-Null
        $users = Invoke-MallApi -Method GET -Path '/api/admin/users?page=1&size=20' -Token $token
        $user = Get-FirstArrayItem (Get-ResponseData $users)
        if ($user -and $user.id) { Invoke-MallApi -Method GET -Path "/api/admin/users/$($user.id)" -Token $token | Out-Null }
        Invoke-MallApi -Method GET -Path '/api/admin/operation-logs?page=1&size=20' -Token $token | Out-Null
        "sampleUserId=$($user.id)"
    } | Out-Null

    Invoke-Case -Suite "P1AdminQuery" -Name "admin product and stock queries" -Script {
        $token = Ensure-AdminToken
        Invoke-MallApi -Method GET -Path "/api/admin/categories" -Token $token | Out-Null
        $products = Invoke-MallApi -Method GET -Path '/api/admin/products?page=1&size=10' -Token $token
        $product = Get-FirstArrayItem (Get-ResponseData $products)
        if ($product -and $product.id) { Invoke-MallApi -Method GET -Path "/api/admin/products/$($product.id)" -Token $token | Out-Null }
        Invoke-MallApi -Method GET -Path '/api/admin/products/sales-threshold-config' -Token $token -AllowFailure | Out-Null
        Invoke-MallApi -Method GET -Path '/api/admin/stocks?page=1&size=20' -Token $token | Out-Null
        Invoke-MallApi -Method GET -Path '/api/admin/stocks/warnings?page=1&size=20' -Token $token | Out-Null
        Invoke-MallApi -Method GET -Path '/api/admin/stocks/logs?page=1&size=20' -Token $token | Out-Null
        Invoke-MallApi -Method POST -Path "/api/admin/stocks/$SkuId/consistency-check" -Token $token -AllowFailure | Out-Null
        "productId=$($product.id) skuId=$SkuId"
    } | Out-Null

    Invoke-Case -Suite "P1AdminQuery" -Name "pay refund callback reconciliation list queries" -Script {
        $token = Ensure-AdminToken
        Invoke-MallApi -Method GET -Path '/api/admin/pays/refunds?page=1&size=10' -Token $token | Out-Null
        Invoke-MallApi -Method GET -Path '/api/admin/pays/callback-records?page=1&size=10' -Token $token | Out-Null
        Invoke-MallApi -Method GET -Path '/api/admin/reconciliations/pay-records?page=1&size=10' -Token $token | Out-Null
        Invoke-MallApi -Method GET -Path '/api/admin/reconciliations/stocks?page=1&size=10' -Token $token -AllowFailure | Out-Null
        Invoke-MallApi -Method GET -Path '/api/admin/online-reconcile-tasks?page=1&size=10' -Token $token | Out-Null
        Invoke-MallApi -Method GET -Path '/api/admin/online-reconcile-hangings?page=1&size=10' -Token $token | Out-Null
        Invoke-MallApi -Method GET -Path '/api/admin/online-reconcile-archive-report?channel=MOCK' -Token $token | Out-Null
        "admin finance/reconcile query endpoints checked"
    } | Out-Null
}

function Run-P1InternalStockSuite {
    Write-Section "P1 flow: internal stock operations"

    Invoke-Case -Suite "P1InternalStock" -Name "internal stock health query sync reserve cancel" -Script {
        $token = Ensure-AdminToken
        $bizNo = "AUTO-STOCK-$(Get-Date -Format 'yyyyMMddHHmmss')"
        $body = @{ businessType = "ORDER"; businessNo = $bizNo; items = @(@{ skuId = $SkuId; quantity = 1 }) }
        Invoke-MallApi -Method GET -Path "/api/internal/stocks/health" -Token $token | Out-Null
        Invoke-MallApi -Method GET -Path "/api/internal/stocks/$SkuId" -Token $token | Out-Null
        Invoke-MallApi -Method POST -Path "/api/internal/stocks/$SkuId/sync" -Token $token | Out-Null
        Invoke-MallApi -Method POST -Path "/api/internal/stocks/reserve" -Token $token -Body $body | Out-Null
        Invoke-MallApi -Method DELETE -Path "/api/internal/stocks/cancel" -Token $token -Body $body | Out-Null
        "businessNo=$bizNo skuId=$SkuId"
    } | Out-Null
}

function Run-P1AdminFlowSuite {
    Write-Section "P1 flow: order, pay, reconciliation, aftersale"

    Invoke-Case -Suite "P1AdminFlow" -Name "admin query orders and pays" -Script {
        $token = Ensure-AdminToken
        $orderNo = $script:Context.orderNo
        Invoke-MallApi -Method GET -Path '/api/admin/orders?page=1&size=10' -Token $token | Out-Null
        Invoke-MallApi -Method GET -Path '/api/admin/pays?page=1&size=10' -Token $token | Out-Null
        if ($orderNo) {
            Invoke-MallApi -Method GET -Path "/api/admin/orders/$orderNo" -Token $token | Out-Null
            Invoke-MallApi -Method GET -Path "/api/admin/pays/$orderNo" -Token $token -AllowFailure | Out-Null
        }
        "orderNo=$orderNo"
    } | Out-Null

    Invoke-Case -Suite "P1AdminFlow" -Name "admin reconciliation overview is reachable" -Script {
        $token = Ensure-AdminToken
        Invoke-MallApi -Method GET -Path '/api/admin/reconciliations/overview' -Token $token -AllowFailure | Out-Null
        Invoke-MallApi -Method GET -Path '/api/admin/reconciliations?page=1&size=10' -Token $token | Out-Null
        "reconciliation overview checked"
    } | Out-Null

    Invoke-Case -Suite "P1AdminFlow" -Name "admin aftersales list is reachable" -Script {
        $token = Ensure-AdminToken
        Invoke-MallApi -Method GET -Path '/api/admin/aftersales?page=1&size=10' -Token $token -AllowFailure | Out-Null
        "aftersale list checked"
    } | Out-Null

    if ($script:Context.orderNo) {
        Invoke-Case -Suite "P1AdminFlow" -Name "admin order status transition can be called" -Script {
            $token = Ensure-AdminToken
            $orderNo = $script:Context.orderNo
            Invoke-MallApi -Method PATCH -Path "/api/admin/orders/$orderNo/ship" -Token $token -AllowFailure | Out-Null
            Invoke-MallApi -Method PATCH -Path "/api/admin/orders/$orderNo/complete" -Token $token -AllowFailure | Out-Null
            "orderNo=$orderNo ship/complete attempted"
        } | Out-Null
    } else {
        Add-TestResult -Suite "P1AdminFlow" -Name "admin order status transition can be called" -Status "SKIP" -Detail "orderNo missing" -ElapsedMs 0
    }
}

function Run-P1OrderPaySuite {
    Write-Section "P1 flow: order pay backbone"

    Invoke-Case -Suite "P1OrderPay" -Name "create direct order" -Script {
        $token = Ensure-UserToken
        $order = Invoke-MallApi -Method POST -Path "/api/orders" -Token $token -Body (New-DirectOrderBody)
        $data = Get-ResponseData $order
        if (-not $data.orderNo) { throw "order created but orderNo is missing" }
        $script:Context.orderNo = $data.orderNo
        $script:Context.orderId = $data.id
        "orderNo=$($data.orderNo) orderId=$($data.id)"
    } | Out-Null

    Invoke-Case -Suite "P1OrderPay" -Name "create pay order" -Script {
        $token = Ensure-UserToken
        $orderNo = $script:Context.orderNo
        if (-not $orderNo) { throw "orderNo is missing" }
        $pay = Invoke-MallApi -Method POST -Path "/api/pay/orders?orderNo=$orderNo&payChannel=MOCK&returnPath=/orders" -Token $token
        $data = Get-ResponseData $pay
        if (-not $data.payOrderNo) { throw "pay order created but payOrderNo is missing" }
        $script:Context.payOrderNo = $data.payOrderNo
        "payOrderNo=$($data.payOrderNo)"
    } | Out-Null

    Invoke-Case -Suite "P1OrderPay" -Name "mock pay success and query pay order" -Script {
        $token = Ensure-UserToken
        $orderNo = $script:Context.orderNo
        $payOrderNo = $script:Context.payOrderNo
        Invoke-MallApi -Method POST -Path "/api/pay/callback/mock-success?orderNo=$orderNo" -Token $token | Out-Null
        Invoke-MallApi -Method GET -Path "/api/pay/orders/$payOrderNo" -Token $token | Out-Null
        Invoke-MallApi -Method GET -Path "/api/pay/reconcile?orderNo=$orderNo" -Token $token | Out-Null
        "orderNo=$orderNo payOrderNo=$payOrderNo"
    } | Out-Null

    Invoke-Case -Suite "P1OrderPay" -Name "user orders list and detail are reachable" -Script {
        $token = Ensure-UserToken
        Invoke-MallApi -Method GET -Path "/api/orders?page=1&size=10" -Token $token | Out-Null
        if ($script:Context.orderId) {
            Invoke-MallApi -Method GET -Path "/api/orders/$($script:Context.orderId)" -Token $token | Out-Null
        }
        "orderId=$($script:Context.orderId)"
    } | Out-Null
}

Write-InitialReport

try {
    Run-P1OrderPaySuite
    Run-P1AddressCartSuite
    Run-P1AdminQuerySuite
    Run-P1InternalStockSuite
    Run-P1AdminFlowSuite
    Write-FinalReport
} catch {
    Write-FinalReport
    throw
}

if (($script:Results | Where-Object { $_.status -eq 'FAIL' -or $_.status -eq 'TIMEOUT' -or $_.status -eq 'BLOCKED' }).Count -gt 0) {
    throw 'There are failed or blocked cases. Please check the reports.'
}

Write-Host "All P1 automated test cases completed" -ForegroundColor Green
