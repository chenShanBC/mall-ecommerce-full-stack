param([string]$BaseUrl = "http://localhost:8080")

$ErrorActionPreference = "Stop"

Write-Host "Running mallFei API tests against $BaseUrl" -ForegroundColor Cyan
& "$PSScriptRoot\test-product.ps1" -BaseUrl $BaseUrl
& "$PSScriptRoot\test-user.ps1" -BaseUrl $BaseUrl
& "$PSScriptRoot\test-order-pay.ps1" -BaseUrl $BaseUrl
& "$PSScriptRoot\test-admin.ps1" -BaseUrl $BaseUrl
Write-Host "All API tests passed" -ForegroundColor Green
