param([string]$BaseUrl = "http://localhost:8080")
. "$PSScriptRoot\test-lib.ps1" -BaseUrl $BaseUrl

Write-Step "Cart, order and pay APIs"
$userToken = Login-User
Invoke-Api -Method GET -Path "/api/cart/items" -Token $userToken | Out-Null
Invoke-Api -Method GET -Path "/api/cart/settlement-preview" -Token $userToken -AllowFailure | Out-Null
$order = New-DirectOrder -UserToken $userToken
$orderNo = $order.data.orderNo
$orderId = $order.data.id
if (-not $orderNo) { throw "orderNo not found" }
$pay = Invoke-Api -Method POST -Path "/api/pay/orders?orderNo=$orderNo" -Token $userToken
$payOrderNo = $pay.data.payOrderNo
if (-not $payOrderNo) { throw "payOrderNo not found" }
Invoke-Api -Method POST -Path "/api/pay/callback/mock-success?orderNo=$orderNo" -Token $userToken | Out-Null
Invoke-Api -Method GET -Path "/api/pay/orders/$payOrderNo" -Token $userToken | Out-Null
Invoke-Api -Method GET -Path "/api/pay/reconcile?orderNo=$orderNo" -Token $userToken | Out-Null
Invoke-Api -Method GET -Path "/api/orders" -Token $userToken | Out-Null
if ($orderId) { Invoke-Api -Method GET -Path "/api/orders/$orderId" -Token $userToken | Out-Null }
Write-Host "Order/pay APIs passed: $orderNo / $payOrderNo" -ForegroundColor Green
