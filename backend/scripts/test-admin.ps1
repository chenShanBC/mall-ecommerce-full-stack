param([string]$BaseUrl = "http://localhost:8080")
. "$PSScriptRoot\test-lib.ps1" -BaseUrl $BaseUrl

Write-Step "Admin APIs"
$userToken = Login-User
$order = New-DirectOrder -UserToken $userToken
$orderNo = $order.data.orderNo
Invoke-Api -Method POST -Path "/api/pay/orders?orderNo=$orderNo" -Token $userToken | Out-Null
Invoke-Api -Method POST -Path "/api/pay/callback/mock-success?orderNo=$orderNo" -Token $userToken | Out-Null
$adminToken = Login-Admin
Invoke-Api -Method GET -Path "/api/admin/me" -Token $adminToken | Out-Null
Invoke-Api -Method GET -Path "/api/admin/dashboard" -Token $adminToken | Out-Null
Invoke-Api -Method GET -Path "/api/admin/orders?page=1&size=10" -Token $adminToken | Out-Null
Invoke-Api -Method GET -Path "/api/admin/orders/$orderNo" -Token $adminToken | Out-Null
Invoke-Api -Method GET -Path "/api/admin/pays?page=1&size=10" -Token $adminToken | Out-Null
Invoke-Api -Method GET -Path "/api/admin/pays/$orderNo" -Token $adminToken | Out-Null
Invoke-Api -Method GET -Path "/api/admin/reconciliations?page=1&size=10" -Token $adminToken | Out-Null
Invoke-Api -Method POST -Path "/api/admin/orders/$orderNo/ship" -Token $adminToken | Out-Null
Invoke-Api -Method POST -Path "/api/admin/orders/$orderNo/complete" -Token $adminToken | Out-Null
Write-Host "Admin APIs passed: $orderNo" -ForegroundColor Green
