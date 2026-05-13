param(
    [string]$Url = "http://localhost:8080/api/products",
    [int]$TimeoutSeconds = 90
)

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
while ((Get-Date) -lt $deadline) {
    try {
        $response = Invoke-RestMethod -Method GET -Uri $Url -TimeoutSec 3
        if ($response.success -eq $true) {
            Write-Host "Application is ready: $Url" -ForegroundColor Green
            exit 0
        }
    } catch {
        Start-Sleep -Seconds 2
    }
}
Write-Error "Application is not ready after $TimeoutSeconds seconds: $Url"
exit 1
