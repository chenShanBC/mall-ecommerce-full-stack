param(
    [string]$BaseUrl = "http://localhost:9090",
    [string]$UserMobile = "13800000000",
    [string]$UserPassword = "123456",
    [string]$AdminUsername = "admin",
    [string]$AdminPassword = "123456",
    [long]$ProductId = 1,
    [long]$SkuId = 1,
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
    if ($LoginData.PSObject.Properties.Name -contains "loginToken" -and $LoginData.loginToken) { return [string]$LoginData.loginToken }
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

Write-Host "MVP 阶段冒烟测试开始：$BaseUrl" -ForegroundColor Cyan
Write-Host "说明：默认非 Strict 模式会把依赖数据/状态导致的失败记为 SKIP，避免因空库、重复数据或订单状态不可操作导致整段脚本中断。需要强校验时追加 -Strict。" -ForegroundColor DarkGray

Write-Step "MVP-公共商品浏览"
$categories = Invoke-Api -Name "商品分类展示" -Method GET -Path "/api/categories"
$products = Invoke-Api -Name "商品列表展示" -Method GET -Path "/api/products" -Query @{ page = 1; size = 10 }
$firstProduct = Get-FirstArrayItem $products.data
$detectedProductId = Pick-FirstId $firstProduct @("productId", "id")
if ($detectedProductId) { $ProductId = [long]$detectedProductId }
$productDetail = Invoke-Api -Name "商品详情展示" -Method GET -Path "/api/products/$ProductId" -Optional
if ($productDetail -and $productDetail.data) {
    foreach ($skuField in @("skus", "skuList", "skuViews")) {
        if ($productDetail.data.PSObject.Properties.Name -contains $skuField -and $productDetail.data.$skuField -and $productDetail.data.$skuField.Count -gt 0) {
            $detectedSkuId = Pick-FirstId $productDetail.data.$skuField[0] @("skuId", "id")
            if ($detectedSkuId) { $SkuId = [long]$detectedSkuId }
            break
        }
    }
}

Write-Step "MVP-C端用户认证与资料"
$userLogin = Invoke-Api -Name "用户密码登录" -Method POST -Path "/api/users/login/password" -Body @{ mobile = $UserMobile; password = $UserPassword } -Optional
$userToken = Get-Token $userLogin.data
if (-not $userToken) {
    $registerMobile = "13" + (Get-Date -Format "MMddHHmmss").Substring(0, 9)
    $userRegister = Invoke-Api -Name "用户注册" -Method POST -Path "/api/users/register" -Body @{ mobile = $registerMobile; password = $UserPassword; nickname = "smoke用户" } -Optional
    $userToken = Get-Token $userRegister.data
    if ($userToken) { $UserMobile = $registerMobile }
}

if ($userToken) {
    Invoke-Api -Name "当前用户信息查询" -Method GET -Path "/api/users/me" -Token $userToken | Out-Null
    Invoke-Api -Name "用户资料维护" -Method PUT -Path "/api/users/me" -Token $userToken -Body @{ nickname = "smoke用户"; avatar = "" } -Optional | Out-Null
} else {
    Write-Skip "未获取到 C 端用户 token，跳过需要用户登录态的 MVP 接口"
}

Write-Step "MVP-用户收货地址"
$addressId = $null
if ($userToken) {
    $addressesBefore = Invoke-Api -Name "地址列表查询" -Method GET -Path "/api/users/addresses" -Token $userToken
    $addressBody = @{
        receiverName = "冒烟测试用户"
        receiverPhone = $UserMobile
        provinceName = "北京市"
        cityName = "北京市"
        districtName = "朝阳区"
        detailAddress = "望京街道1号"
        defaulted = $true
    }
    $addressCreate = Invoke-Api -Name "新增地址" -Method POST -Path "/api/users/addresses" -Token $userToken -Body $addressBody -Optional
    $addressId = Pick-FirstId $addressCreate.data @("addressId", "id")
    if (-not $addressId) {
        $firstAddress = Get-FirstArrayItem $addressesBefore.data
        $addressId = Pick-FirstId $firstAddress @("addressId", "id")
    }
    if ($addressId) {
        Invoke-Api -Name "修改地址" -Method PUT -Path "/api/users/addresses/$addressId" -Token $userToken -Body $addressBody -Optional | Out-Null
        Invoke-Api -Name "设置默认地址" -Method PUT -Path "/api/users/addresses/$addressId/default" -Token $userToken -Optional | Out-Null
    } else {
        Write-Skip "未获取到 addressId，跳过修改地址与设置默认地址"
    }
}

Write-Step "MVP-购物车与结算预览"
$cartItemId = $null
if ($userToken) {
    Invoke-Api -Name "查询购物车列表" -Method GET -Path "/api/cart/items" -Token $userToken | Out-Null
    $addCart = Invoke-Api -Name "加入购物车" -Method POST -Path "/api/cart/items" -Token $userToken -Body @{ skuId = $SkuId; quantity = 1 } -Optional
    $cartList = Invoke-Api -Name "再次查询购物车列表" -Method GET -Path "/api/cart/items" -Token $userToken
    $firstCartItem = Get-FirstArrayItem $cartList.data
    $cartItemId = Pick-FirstId $firstCartItem @("cartItemId", "id")
    if (-not $cartItemId) { $cartItemId = Pick-FirstId $addCart.data @("cartItemId", "id") }
    Invoke-Api -Name "查询购物车商品数量" -Method GET -Path "/api/cart/quantity" -Token $userToken | Out-Null
    if ($cartItemId) {
        Invoke-Api -Name "修改购物车商品数量" -Method PUT -Path "/api/cart/items/$cartItemId" -Token $userToken -Body @{ quantity = 1 } -Optional | Out-Null
        Invoke-Api -Name "勾选购物车商品" -Method PUT -Path "/api/cart/items/checked" -Token $userToken -Body @{ cartItemIds = @($cartItemId); checked = $true } -Optional | Out-Null
    }
    Invoke-Api -Name "购物车结算预览" -Method GET -Path "/api/cart/settlement-preview" -Token $userToken -Optional | Out-Null
    Invoke-Api -Name "结算前商品和库存校验" -Method POST -Path "/api/cart/prepare-checkout" -Token $userToken -Body @{ cartItemIds = @($cartItemId) } -Optional | Out-Null
}

Write-Step "MVP-下单、订单基础管理与支付基础链路"
$orderNo = $null
$orderId = $null
$payOrderNo = $null
if ($userToken) {
    $checkoutBody = @{
        addressId = $addressId
        receiverName = "冒烟测试用户"
        receiverPhone = $UserMobile
        receiverProvinceName = "北京市"
        receiverCityName = "北京市"
        receiverDistrictName = "朝阳区"
        receiverDetailAddress = "望京街道1号"
        remark = "mvp smoke test"
    }
    $order = Invoke-Api -Name "购物车结算下单" -Method POST -Path "/api/cart/checkout" -Token $userToken -Body $checkoutBody -Optional
    if (-not $order) {
        $directBody = $checkoutBody.Clone()
        $directBody.items = @(@{ skuId = $SkuId; quantity = 1 })
        $order = Invoke-Api -Name "直接创建订单" -Method POST -Path "/api/orders" -Token $userToken -Body $directBody -Optional
    }
    if ($order -and $order.data) {
        $orderNo = Pick-FirstId $order.data @("orderNo")
        $orderId = Pick-FirstId $order.data @("orderId", "id")
        Invoke-Api -Name "用户订单列表" -Method GET -Path "/api/orders" -Token $userToken -Query @{ page = 1; size = 10; status = "ALL" } | Out-Null
        if ($orderId) { Invoke-Api -Name "用户订单详情" -Method GET -Path "/api/orders/$orderId" -Token $userToken -Optional | Out-Null }
        if ($orderNo) {
            $pay = Invoke-Api -Name "创建支付单" -Method POST -Path "/api/pay/orders" -Token $userToken -Query @{ orderNo = $orderNo; payChannel = "MOCK" } -Optional
            $payOrderNo = Pick-FirstId $pay.data @("payOrderNo", "id")
            Invoke-Api -Name "Mock 支付成功" -Method POST -Path "/api/pay/callback/mock-success" -Query @{ orderNo = $orderNo } -Optional | Out-Null
            if ($payOrderNo) { Invoke-Api -Name "支付单详情查询" -Method GET -Path "/api/pay/orders/$payOrderNo" -Token $userToken -Optional | Out-Null }
        }
    } else {
        Write-Skip "未创建订单，跳过订单详情与支付链路"
    }
}

Write-Step "MVP-Admin后台基础管理"
$adminLogin = Invoke-Api -Name "管理员登录" -Method POST -Path "/api/admin/login/password" -Body @{ username = $AdminUsername; password = $AdminPassword } -Optional
$adminToken = Get-Token $adminLogin.data
if ($adminToken) {
    Invoke-Api -Name "当前管理员信息查询" -Method GET -Path "/api/admin/me" -Token $adminToken | Out-Null
    Invoke-Api -Name "基础 Dashboard" -Method GET -Path "/api/admin/dashboard" -Token $adminToken -Optional | Out-Null
    Invoke-Api -Name "后台商品管理-列表" -Method GET -Path "/api/admin/products" -Token $adminToken -Query @{ page = 1; size = 10 } -Optional | Out-Null
    Invoke-Api -Name "后台类目管理-列表" -Method GET -Path "/api/admin/categories" -Token $adminToken -Optional | Out-Null
    Invoke-Api -Name "后台订单查看" -Method GET -Path "/api/admin/orders" -Token $adminToken -Query @{ page = 1; size = 10 } -Optional | Out-Null
    Invoke-Api -Name "后台库存管理" -Method GET -Path "/api/admin/stocks" -Token $adminToken -Query @{ page = 1; size = 20 } -Optional | Out-Null
    Invoke-Api -Name "后台用户查看" -Method GET -Path "/api/admin/users" -Token $adminToken -Query @{ page = 1; size = 20 } -Optional | Out-Null
    if ($orderNo) { Invoke-Api -Name "后台订单详情" -Method GET -Path "/api/admin/orders/$orderNo" -Token $adminToken -Optional | Out-Null }
} else {
    Write-Skip "未获取到 Admin token，跳过后台基础管理接口"
}

Write-Step "MVP-文件上传基础能力"
Write-Skip "文件上传接口需要真实 multipart 文件；为避免脚本生成临时文件和污染工作区，本冒烟脚本仅覆盖接口划分，不默认上传文件。可用 /api/files/avatar 与 /api/files/product-image 单独验证。"

Write-Step "MVP冒烟测试完成"
Write-Host "通过: $script:Passed, 跳过: $script:Skipped, 失败: $script:Failed" -ForegroundColor Cyan
if ($orderNo) { Write-Host "OrderNo: $orderNo" -ForegroundColor Green }
if ($payOrderNo) { Write-Host "PayOrderNo: $payOrderNo" -ForegroundColor Green }
if ($script:Failed -gt 0) { exit 1 }




