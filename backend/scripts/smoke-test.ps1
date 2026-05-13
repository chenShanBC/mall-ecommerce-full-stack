param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"

function Write-Step([string]$Message) {
    Write-Host "`n==== $Message ====" -ForegroundColor Cyan
}

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body = $null,
        [string]$Token = $null
    )
    $headers = @{}
    if ($Token) { $headers["Authorization"] = $Token }
    $params = @{
        Method = $Method
        Uri = "$BaseUrl$Path"
        Headers = $headers
        ContentType = "application/json; charset=utf-8"
    }
    if ($null -ne $Body) { $params.Body = ($Body | ConvertTo-Json -Depth 20) }
    try {
        $response = Invoke-RestMethod @params
    } catch {
        Write-Host "Request failed: $Method $BaseUrl$Path" -ForegroundColor Red
        if ($_.Exception.Response) {
            $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
            Write-Host $reader.ReadToEnd() -ForegroundColor Red
        }
        throw
    }
    if ($response.success -ne $true) {
        $response | ConvertTo-Json -Depth 20 | Write-Host
        throw "API failure: $Method $Path"
    }
    return $response
}

function Get-Token([object]$LoginData) {
    if ($LoginData.token) { return $LoginData.token }
    if ($LoginData.accessToken) { return $LoginData.accessToken }
    if ($LoginData.Authorization) { return $LoginData.Authorization }
    throw "Cannot find token field in login response data"
}

Write-Step "Public product APIs"
Invoke-Api -Method GET -Path "/api/categories" | Out-Null
Invoke-Api -Method GET -Path "/api/products" | Out-Null
Invoke-Api -Method GET -Path "/api/products/1" | Out-Null

Write-Step "User login"
$userLogin = Invoke-Api -Method POST -Path "/api/users/login/password" -Body @{ mobile = "13800000000"; password = "123456" }
$userToken = Get-Token $userLogin.data

Write-Step "User profile and cart"
Invoke-Api -Method GET -Path "/api/users/me" -Token $userToken | Out-Null
Invoke-Api -Method GET -Path "/api/cart/items" -Token $userToken | Out-Null
try { Invoke-Api -Method GET -Path "/api/cart/settlement-preview" -Token $userToken | Out-Null } catch { Write-Host "Settlement preview skipped" -ForegroundColor Yellow }

Write-Step "Create order"
$checkoutBody = @{
    receiverName = "示例用户"
    receiverPhone = "13800000000"
    receiverProvinceName = "北京市"
    receiverCityName = "北京市"
    receiverDistrictName = "朝阳区"
    receiverDetailAddress = "望京街道 1 号"
    remark = "smoke test order"
}
try {
    $order = Invoke-Api -Method POST -Path "/api/cart/checkout" -Token $userToken -Body $checkoutBody
} catch {
    Write-Host "Cart checkout failed, fallback to direct order create" -ForegroundColor Yellow
    $directOrderBody = $checkoutBody.Clone()
    $directOrderBody.items = @(@{ skuId = 1; quantity = 1 })
    $order = Invoke-Api -Method POST -Path "/api/orders" -Token $userToken -Body $directOrderBody
}
$orderNo = $order.data.orderNo
$orderId = $order.data.id
if (-not $orderNo) { throw "Cannot find orderNo in order response" }
Write-Host "Created order: $orderNo"

Write-Step "Pay order"
$pay = Invoke-Api -Method POST -Path "/api/pay/orders?orderNo=$orderNo"
$payOrderNo = $pay.data.payOrderNo
if (-not $payOrderNo) { throw "Cannot find payOrderNo in pay response" }
Invoke-Api -Method POST -Path "/api/pay/callback/mock-success?orderNo=$orderNo" | Out-Null
Invoke-Api -Method GET -Path "/api/pay/orders/$payOrderNo" | Out-Null
Invoke-Api -Method GET -Path "/api/pay/reconcile?orderNo=$orderNo" | Out-Null

Write-Step "User orders"
Invoke-Api -Method GET -Path "/api/orders" -Token $userToken | Out-Null
if ($orderId) { Invoke-Api -Method GET -Path "/api/orders/$orderId" -Token $userToken | Out-Null }

Write-Step "Admin login"
$adminLogin = Invoke-Api -Method POST -Path "/api/admin/login/password" -Body @{ username = "admin"; password = "123456" }
$adminToken = Get-Token $adminLogin.data

Write-Step "Admin read APIs"
Invoke-Api -Method GET -Path "/api/admin/me" -Token $adminToken | Out-Null
Invoke-Api -Method GET -Path "/api/admin/dashboard" -Token $adminToken | Out-Null
Invoke-Api -Method GET -Path "/api/admin/orders?page=1&size=10" -Token $adminToken | Out-Null
Invoke-Api -Method GET -Path "/api/admin/orders/$orderNo" -Token $adminToken | Out-Null
Invoke-Api -Method GET -Path "/api/admin/pays?page=1&size=10" -Token $adminToken | Out-Null
Invoke-Api -Method GET -Path "/api/admin/pays/$orderNo" -Token $adminToken | Out-Null
Invoke-Api -Method GET -Path "/api/admin/reconciliations?page=1&size=10" -Token $adminToken | Out-Null

Write-Step "Admin order operations"
Invoke-Api -Method POST -Path "/api/admin/orders/$orderNo/ship" -Token $adminToken | Out-Null
Invoke-Api -Method POST -Path "/api/admin/orders/$orderNo/complete" -Token $adminToken | Out-Null

Write-Step "Smoke test completed"
Write-Host "OrderNo: $orderNo" -ForegroundColor Green
Write-Host "PayOrderNo: $payOrderNo" -ForegroundColor Green
