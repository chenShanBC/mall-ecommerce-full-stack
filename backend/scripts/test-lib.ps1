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
        [string]$Token = $null,
        [switch]$AllowFailure
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
        if ($AllowFailure) { return $null }
        throw
    }
    if ($response.success -ne $true) {
        $response | ConvertTo-Json -Depth 20 | Write-Host
        if ($AllowFailure) { return $response }
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

function Login-User {
    $login = Invoke-Api -Method POST -Path "/api/users/login/password" -Body @{ mobile = "13800000000"; password = "123456" }
    return Get-Token $login.data
}

function Login-Admin {
    $login = Invoke-Api -Method POST -Path "/api/admin/login/password" -Body @{ username = "admin"; password = "123456" }
    return Get-Token $login.data
}

function New-TestAddressBody {
    return @{
        receiverName = "Smoke User"
        receiverPhone = "13800000000"
        receiverProvinceName = "Beijing"
        receiverCityName = "Beijing"
        receiverDistrictName = "Chaoyang"
        receiverDetailAddress = "Wangjing Street No.1"
        remark = "api test order"
    }
}

function New-DirectOrder([string]$UserToken) {
    $body = New-TestAddressBody
    $body.items = @(@{ skuId = 1; quantity = 1 })
    return Invoke-Api -Method POST -Path "/api/orders" -Token $UserToken -Body $body
}
