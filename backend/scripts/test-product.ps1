param([string]$BaseUrl = "http://localhost:8080")
. "$PSScriptRoot\test-lib.ps1" -BaseUrl $BaseUrl

Write-Step "Product public APIs"
Invoke-Api -Method GET -Path "/api/categories" | Out-Null
Invoke-Api -Method GET -Path "/api/products" | Out-Null
Invoke-Api -Method GET -Path "/api/products/1" | Out-Null
Write-Host "Product APIs passed" -ForegroundColor Green
