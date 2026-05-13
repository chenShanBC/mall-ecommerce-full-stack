param([string]$BaseUrl = "http://localhost:8080")
. "$PSScriptRoot\test-lib.ps1" -BaseUrl $BaseUrl

Write-Step "User auth APIs"
$userToken = Login-User
Invoke-Api -Method GET -Path "/api/users/me" -Token $userToken | Out-Null
Invoke-Api -Method POST -Path "/api/users/logout" -Token $userToken | Out-Null
Write-Host "User APIs passed" -ForegroundColor Green
