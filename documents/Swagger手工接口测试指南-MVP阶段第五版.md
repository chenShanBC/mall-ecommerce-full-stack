# mallFei Swagger 手工接口测试指南（MVP 阶段第五版）

适用工具：Swagger UI 手工测试。

- Base URL：`http://localhost:9090`
- Swagger：`http://localhost:9090/swagger-ui.html`
- OpenAPI JSON：`http://localhost:9090/v3/api-docs`
- 用户鉴权头：`Authorization: Bearer <userToken>`
- 管理员鉴权头：`Authorization: Bearer <adminToken>`

> 本文按当前后端 Controller 与前端 API 调用整理，适合你在 Swagger 页面逐个接口手工验证。涉及新增、修改、删除、状态流转的接口，请尽量在测试库执行。

---

## 1. 手工测试总顺序

推荐严格按下面顺序测试：

1. 环境与 Swagger 可用性检查
2. 用户认证与用户资料
3. 管理员认证与后台基础能力
4. 商品公共接口
5. 地址接口
6. 购物车接口
7. 创建订单与订单查询
8. 支付与支付回调
9. 售后与退款
10. 后台订单、支付、售后管理
11. 后台商品、库存管理
12. 后台对账与线上人工对账
13. 重点业务链路串测

顺序原因：

- 用户 token 是地址、购物车、订单、支付的前置条件。
- 管理员 token 是后台订单、支付、库存、对账的前置条件。
- 商品和库存必须先可用，订单才可能创建成功。
- 支付、售后、对账依赖订单数据。
- 后台看板和对账建议最后测，因为它们依赖前面产生的真实业务数据。

---

## 2. 成功标准与变量记录

### 2.1 统一成功标准

一个接口至少满足：

1. HTTP 状态码为 `200`。
2. 响应体存在。
3. 如果是统一响应对象，要求 `success=true`。
4. `data` 中关键字段与请求入参或业务状态一致。

典型成功响应：

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "success",
  "data": {},
  "timestamp": "2026-06-15T10:00:00"
}
```

### 2.2 建议记录变量

| 变量 | 来源 | 用途 |
| --- | --- | --- |
| `userToken` | 用户登录 | C 端接口鉴权 |
| `adminToken` | 管理员登录 | 后台接口鉴权 |
| `userId` | 当前用户/后台用户列表 | 用户管理测试 |
| `addressId` | 新增地址 | 地址修改、默认、删除 |
| `productId` | 商品列表/详情/新增商品 | 商品详情、下单 |
| `skuId` | 商品详情中的 `skus[].id` | 下单、库存测试 |
| `cartItemId` | 购物车列表 | 修改/删除购物车项 |
| `orderId` | 创建订单 | 用户订单详情、取消、退款 |
| `orderNo` | 创建订单 | 支付、后台订单、对账 |
| `payOrderNo` | 创建支付单 | 支付单详情 |
| `refundNo` | 退款/售后结果或后台退款列表 | 退款同步 |
| `aftersaleNo` | 售后列表/申请售后 | 后台售后审核 |
| `taskId` | 创建线上对账任务 | 线上对账后续步骤 |
| `diffId` | 线上对账差异列表 | 差异处理、日志、挂账 |

---

## 3. 环境与启动检查

### 3.1 启动依赖

在项目 `backend` 目录按你的环境启动 MySQL、Redis、RabbitMQ 等依赖。

成功标准：

- MySQL、Redis、RabbitMQ 均可用。
- 后端应用成功启动。
- Swagger 页面可打开。

### 3.2 打开 Swagger

- 地址：`http://localhost:9090/swagger-ui.html`

成功标准：

- 页面能看到接口分组，例如：用户认证、商品公共接口、订单、支付、Admin Backend。
- 右上或接口锁图标可以输入 Bearer Token。

### 3.3 Swagger 鉴权填写方式

在 Swagger 的 Authorize 弹窗里填写完整值：

```text
Bearer <token>
```

注意：必须包含 `Bearer ` 前缀。

---

## 4. 用户认证与资料接口

接口分组：用户认证。

### 4.1 检查手机号禁用状态

- 接口：`GET /api/users/login/blacklist/status`
- 鉴权：不需要
- Query：
  - `mobile=13800000000`

成功标准：

- `success=true`
- 返回当前手机号是否禁用。

### 4.2 获取登录拼图验证码挑战

- 接口：`GET /api/users/login/captcha/challenge`
- 鉴权：不需要

成功标准：

- `success=true`
- `data` 中存在验证码挑战信息。

### 4.3 校验登录拼图验证码

- 接口：`POST /api/users/login/captcha/verify`
- 鉴权：不需要
- Body 示例需以 4.2 返回字段为准。

说明：

- 该接口依赖验证码挑战结果，手工测试时可作为补充，不作为主链路必测。

### 4.4 用户密码登录

- 接口：`POST /api/users/login/password`
- 鉴权：不需要
- Body：

```json
{
  "mobile": "13800000000",
  "password": "123456"
}
```

成功标准：

- `success=true`
- `data.token` 非空
- `data.mobile=13800000000`

记录：`userToken`。

### 4.5 发送短信验证码

- 接口：`POST /api/users/login/sms/send-code`
- 鉴权：不需要
- Body：

```json
{
  "mobile": "13800000000"
}
```

成功标准：

- `success=true`
- `message=验证码发送成功`

### 4.6 短信验证码登录

- 接口：`POST /api/users/login/sms`
- 鉴权：不需要
- Body 示例：

```json
{
  "mobile": "13800000000",
  "code": "123456"
}
```

说明：

- 验证码以系统实际生成或日志输出为准。

### 4.7 用户注册

- 接口：`POST /api/users/register`
- 鉴权：不需要
- Body：

```json
{
  "mobile": "13900000001",
  "password": "123456",
  "nickname": "manual-user-001"
}
```

成功标准：

- `success=true`
- `message=注册并登录成功`
- `data.token` 非空

注意：

- 手机号必须唯一，重复注册会失败。

### 4.8 获取当前用户信息

- 接口：`GET /api/users/me`
- 鉴权：用户 token

成功标准：

- `success=true`
- `data.mobile` 与登录用户一致。

### 4.9 更新当前用户资料

- 接口：`PUT /api/users/me`
- 鉴权：用户 token
- Body：

```json
{
  "nickname": "manual-user-updated",
  "avatarUrl": "https://example.com/avatar.png"
}
```

成功标准：

- `success=true`
- 用户昵称更新成功。

### 4.10 修改当前用户密码

- 接口：`PUT /api/users/me/password`
- 鉴权：用户 token
- Body 示例：

```json
{
  "oldPassword": "123456",
  "newPassword": "1234567"
}
```

注意：

- 该接口会影响后续登录，手工测试时不建议使用默认测试账号执行。

### 4.11 当前用户黑名单状态

- 接口：`GET /api/users/me/blacklist-status`
- 鉴权：用户 token

成功标准：

- `success=true`
- 返回当前登录用户禁用状态。

### 4.12 用户退出登录

- 接口：`DELETE /api/users/logout`
- 鉴权：用户 token

成功标准：

- `success=true`
- `data=true`

注意：

- 如果退出后还要继续测试，请重新登录并记录新的 `userToken`。

---

## 5. 认证聚合接口

接口分组：认证。

### 5.1 当前登录上下文

- 接口：`GET /api/auth/context`
- 鉴权：用户 token 或管理员 token

成功标准：

- `success=true`
- `data.identityType` 与 token 身份一致。

### 5.2 当前主体黑名单状态

- 接口：`GET /api/auth/blacklist/{userId}`
- 鉴权：登录 token
- 示例：`GET /api/auth/blacklist/1`

成功标准：

- `success=true`
- 返回指定用户黑名单状态。

### 5.3 聚合退出登录

- 接口：`DELETE /api/auth/logout`
- 鉴权：用户 token 或管理员 token

成功标准：

- `success=true`

注意：

- 退出后 token 失效，建议最后测试。

---

## 6. 管理员认证与后台基础接口

接口分组：Admin Backend。

### 6.1 管理员密码登录

- 接口：`POST /api/admin/login/password`
- 鉴权：不需要
- Body：

```json
{
  "username": "admin",
  "password": "123456"
}
```

成功标准：

- `success=true`
- `data.token` 非空
- `data.username=admin`

记录：`adminToken`。

### 6.2 当前管理员信息

- 接口：`GET /api/admin/me`
- 鉴权：管理员 token

成功标准：

- `success=true`
- 返回管理员主体信息。

### 6.3 修改个人资料

- 接口：`PUT /api/admin/me/profile`
- 鉴权：管理员 token
- Body 示例：

```json
{
  "nickname": "admin-updated",
  "avatarUrl": "https://example.com/admin-avatar.png"
}
```

成功标准：

- `success=true`
- 返回当前管理员信息。

### 6.4 修改个人密码

- 接口：`PUT /api/admin/me/password`
- 鉴权：管理员 token
- Body 示例：

```json
{
  "oldPassword": "123456",
  "newPassword": "1234567"
}
```

注意：

- 不建议对默认 `admin` 账号执行，避免影响后续测试。

### 6.5 后台看板

- 接口：`GET /api/admin/dashboard`
- 鉴权：管理员 token

成功标准：

- `success=true`
- 返回订单、支付、用户、商品等聚合统计。

### 6.6 管理员退出登录

- 接口：`DELETE /api/admin/logout`
- 鉴权：管理员 token

成功标准：

- `success=true`
- `data=true`

注意：

- 建议最后测试。

---

## 7. 后台账号、权限、用户与日志

统一鉴权：管理员 token。

### 7.1 运营账号列表

- 接口：`GET /api/admin/accounts`
- Query 示例：`page=1&size=20`

成功标准：

- `success=true`
- 返回分页数据。

### 7.2 内置角色列表

- 接口：`GET /api/admin/roles`

成功标准：

- `success=true`
- `data` 为角色数组。

### 7.3 权限目录

- 接口：`GET /api/admin/permissions`

成功标准：

- `success=true`
- 返回权限目录。

### 7.4 角色权限模板

- 接口：`GET /api/admin/accounts/permission-templates`

成功标准：

- `success=true`
- 返回角色与权限映射。

### 7.5 创建运营账号

- 接口：`POST /api/admin/accounts`
- Body 示例以 Swagger schema 为准，建议：

```json
{
  "username": "manual_admin_001",
  "password": "123456",
  "nickname": "手测运营账号",
  "roleCode": "OPERATOR",
  "permissions": []
}
```

成功标准：

- `success=true`
- `data.id` 非空。

记录：`adminId`。

### 7.6 分配运营角色

- 接口：`PATCH /api/admin/accounts/{adminId}/role`
- Body 示例：

```json
{
  "roleCode": "OPERATOR"
}
```

成功标准：

- `success=true`
- 角色更新成功。

### 7.7 禁用/启用运营账号

- 禁用：`PATCH /api/admin/accounts/{adminId}/disable`
- 启用：`PATCH /api/admin/accounts/{adminId}/enable`

成功标准：

- `success=true`
- 状态变化符合预期。

### 7.8 更新运营账号权限

- 接口：`PUT /api/admin/accounts/{adminId}/permissions`
- Body 示例：

```json
{
  "permissions": ["dashboard:view"]
}
```

成功标准：

- `success=true`
- 权限更新成功。

### 7.9 C 端用户列表

- 接口：`GET /api/admin/users`
- Query 示例：`page=1&size=20`

成功标准：

- `success=true`
- 返回用户分页数据。

记录：`userId`。

### 7.10 C 端用户详情

- 接口：`GET /api/admin/users/{userId}`

成功标准：

- `success=true`
- `data.id={userId}`。

### 7.11 禁用/启用 C 端用户

- 禁用：`PATCH /api/admin/users/{userId}/disable`
- 启用：`PATCH /api/admin/users/{userId}/enable`

注意：

- 不建议禁用当前用于下单测试的用户，除非你要专门测试禁用拦截。

### 7.12 运营操作日志

- 接口：`GET /api/admin/operation-logs`
- Query 示例：`page=1&size=20`

成功标准：

- `success=true`
- 返回操作日志分页。

---

## 8. 商品公共接口

### 8.1 获取类目树

- 接口：`GET /api/categories`
- 鉴权：不需要

成功标准：

- `success=true`
- `data` 为数组。

### 8.2 商品列表

- 接口：`GET /api/products`
- 鉴权：不需要
- Query 示例：`page=1&size=10`

成功标准：

- `success=true`
- `data.records` 为数组。

记录：`productId`。

### 8.3 商品详情

- 接口：`GET /api/products/{productId}`
- 鉴权：不需要
- 示例：`GET /api/products/1`

成功标准：

- `success=true`
- `data.id={productId}`
- `data.skus` 非空。

记录：`skuId`。

---

## 9. 地址接口

统一鉴权：用户 token。

### 9.1 地址列表

- 接口：`GET /api/users/addresses`

成功标准：

- `success=true`
- `data` 为数组。

### 9.2 新增地址

- 接口：`POST /api/users/addresses`
- Body：

```json
{
  "receiverName": "Manual Receiver",
  "receiverPhone": "13800138000",
  "provinceCode": "110000",
  "provinceName": "北京市",
  "cityCode": "110100",
  "cityName": "北京市",
  "districtCode": "110105",
  "districtName": "朝阳区",
  "detailAddress": "望京 SOHO T1 1001",
  "postalCode": "100000",
  "isDefault": true
}
```

成功标准：

- `success=true`
- `data.id` 非空。

记录：`addressId`。

### 9.3 修改地址

- 接口：`PUT /api/users/addresses/{addressId}`
- Body：

```json
{
  "receiverName": "Manual Receiver Updated",
  "receiverPhone": "13800138000",
  "provinceCode": "110000",
  "provinceName": "北京市",
  "cityCode": "110100",
  "cityName": "北京市",
  "districtCode": "110105",
  "districtName": "朝阳区",
  "detailAddress": "望京 SOHO T1 1002",
  "postalCode": "100000",
  "isDefault": false
}
```

成功标准：

- `success=true`
- `data.id={addressId}`。

### 9.4 设置默认地址

- 接口：`PUT /api/users/addresses/{addressId}/default`

成功标准：

- `success=true`
- `data.isDefault=true`。

### 9.5 删除地址

- 接口：`DELETE /api/users/addresses/{addressId}`

成功标准：

- `success=true`
- `data=true`。

---

## 10. 购物车接口

统一鉴权：用户 token。

### 10.1 加入购物车

- 接口：`POST /api/cart/items`
- Body：

```json
{
  "skuId": 1,
  "quantity": 1
}
```

成功标准：

- `success=true`
- 返回购物车项信息。

记录：`cartItemId`。

### 10.2 购物车列表

- 接口：`GET /api/cart/items`

成功标准：

- `success=true`
- `data` 为数组。

### 10.3 修改购物车项数量

- 接口：`PUT /api/cart/items/{cartItemId}`
- Body：

```json
{
  "quantity": 2
}
```

成功标准：

- `success=true`
- 数量更新。

### 10.4 修改购物车选中状态

- 接口：`PUT /api/cart/items/checked`
- Body 示例以 Swagger schema 为准，常见结构：

```json
{
  "cartItemIds": [1],
  "checked": true
}
```

成功标准：

- `success=true`
- 指定购物车项选中状态更新。

### 10.5 删除购物车项

- 接口：`DELETE /api/cart/items/{cartItemId}`

成功标准：

- `success=true`。

### 10.6 清空购物车

- 接口：`DELETE /api/cart/items`

成功标准：

- `success=true`。

### 10.7 购物车数量

- 接口：`GET /api/cart/quantity`

成功标准：

- `success=true`
- 返回数量字段。

### 10.8 结算预览

- 接口：`GET /api/cart/settlement-preview`

成功标准：

- `success=true`
- 返回结算预览对象。

### 10.9 准备购物车结算

- 接口：`POST /api/cart/prepare-checkout`
- Body 示例以 Swagger schema 为准。

成功标准：

- `success=true`
- 返回结算准备结果。

### 10.10 购物车结算下单

- 接口：`POST /api/cart/checkout`
- Body：

```json
{
  "receiverName": "Cart User",
  "receiverPhone": "13800138000",
  "receiverProvinceName": "北京市",
  "receiverCityName": "北京市",
  "receiverDistrictName": "朝阳区",
  "receiverDetailAddress": "望京 SOHO T2 2002",
  "remark": "cart checkout test"
}
```

成功标准：

- `success=true`
- `data.orderNo` 非空。

记录：`orderId`、`orderNo`。

---

## 11. 订单接口

统一鉴权：用户 token。

### 11.1 创建订单

- 接口：`POST /api/orders`
- Body：

```json
{
  "receiverName": "Order User",
  "receiverPhone": "13800138000",
  "receiverProvinceName": "北京市",
  "receiverCityName": "北京市",
  "receiverDistrictName": "朝阳区",
  "receiverDetailAddress": "望京 SOHO T3 3003",
  "remark": "manual order test",
  "items": [
    {
      "skuId": 1,
      "quantity": 1
    }
  ]
}
```

成功标准：

- `success=true`
- `data.id` 非空
- `data.orderNo` 非空
- 订单状态为待支付类状态。

记录：`orderId`、`orderNo`。

### 11.2 订单列表

- 接口：`GET /api/orders`
- Query 示例：`page=1&size=10&status=ALL`

成功标准：

- `success=true`
- 返回订单分页。

### 11.3 订单详情

- 接口：`GET /api/orders/{orderId}`

成功标准：

- `success=true`
- `data.id={orderId}`。

### 11.4 取消订单

- 接口：`DELETE /api/orders/{orderId}/cancel`

前置建议：

- 使用未支付订单测试。

成功标准：

- `success=true`
- 订单状态变为取消/关闭类状态。

### 11.5 用户删除订单

- 接口：`DELETE /api/orders/{orderId}`

前置建议：

- 使用已取消或已完成订单测试。

成功标准：

- `success=true`
- `message=订单已删除`。

### 11.6 确认收货

- 接口：`PUT /api/orders/{orderId}/confirm-receipt`

前置建议：

- 使用已发货订单测试。

成功标准：

- `success=true`
- `message=确认收货成功`。

### 11.7 申请退款

- 接口：`POST /api/orders/{orderId}/refund`
- Body 示例以 Swagger schema 为准，常见字段：

```json
{
  "reason": "手工测试退款",
  "refundAmountCent": 100,
  "description": "Swagger 手工测试申请退款"
}
```

前置建议：

- 使用已支付订单测试。

成功标准：

- `success=true`
- `message=退款申请已提交`
- 返回退款申请信息。

记录：`refundNo`。

---

## 12. 支付接口

### 12.1 创建支付单

- 接口：`POST /api/pay/orders`
- 鉴权：登录 token，建议用户 token
- Query：
  - `orderNo=<orderNo>`
  - `payChannel=MOCK`
  - `returnPath=/orders`

成功标准：

- `success=true`
- `data.payOrderNo` 非空。

记录：`payOrderNo`。

### 12.2 支付单详情

- 接口：`GET /api/pay/orders/{payOrderNo}`
- 鉴权：登录 token

成功标准：

- `success=true`
- `data.payOrderNo={payOrderNo}`。

### 12.3 模拟支付成功回调

- 接口：`POST /api/pay/callback/mock-success`
- 鉴权：不需要
- Query：
  - `orderNo=<orderNo>`

成功标准：

- `success=true`
- 支付单状态变为成功。
- 订单状态变为已支付/待发货类状态。

### 12.4 支付渠道异步回调

- 接口：`POST /api/pay/callback/channel`
- 鉴权：不需要
- Body 以 Swagger schema 为准。

说明：

- 一般不作为主链路手工测试，优先使用 mock-success。

### 12.5 支付宝异步回调

- 接口：`POST /api/pay/callback/alipay`
- 鉴权：不需要
- 参数：支付宝回调参数。

说明：

- 需要真实支付宝参数或模拟签名环境，MVP 手测可跳过。

### 12.6 支付提交页

- 接口：`GET /api/pay/orders/{payOrderNo}/submit-page`
- 鉴权：不需要
- Query：`returnPath=/orders`

成功标准：

- 返回 HTML 页面内容。

### 12.7 支付宝同步回跳桥接

- 接口：`GET /api/pay/alipay/return-bridge`
- 鉴权：不需要

说明：

- 支付宝真实回跳场景使用，MVP 手测可跳过。

### 12.8 支付对账

- 接口：`GET /api/pay/reconcile`
- 鉴权：不需要
- Query：`orderNo=<orderNo>`

成功标准：

- `success=true`
- 返回订单与支付一致性结果。

### 12.9 同步订单支付状态

- 接口：`POST /api/pay/orders/{orderNo}/sync-status`
- 鉴权：登录 token

成功标准：

- `success=true`
- 返回支付单状态。

### 12.10 补偿已成功支付订单状态

- 接口：`POST /api/pay/orders/{orderNo}/repair-paid`
- 鉴权：登录 token

成功标准：

- `success=true`
- 支付成功但订单未同步时可修复。

### 12.11 同步退款状态

- 接口：`POST /api/pay/orders/{orderNo}/refunds/{refundNo}/sync-status`
- 鉴权：登录 token
- Query：`refundAmountCent=100`

成功标准：

- `success=true`
- 退款状态同步。

---

## 13. 售后接口

接口分组：售后。

### 13.1 申请售后退款

- 接口：`POST /api/aftersales/refund`
- 鉴权：用户 token
- Body 示例以 Swagger schema 为准，建议从已支付订单中取 `orderId/orderItemId/skuId`。

示例：

```json
{
  "orderId": 1,
  "reason": "Swagger 手工测试售后退款",
  "description": "商品问题，申请退款"
}
```

成功标准：

- `success=true`
- 返回售后单信息。

记录：`aftersaleNo`。

### 13.2 用户售后列表

- 接口：`GET /api/aftersales`
- 鉴权：用户 token
- Query 示例：`page=1&size=10`

成功标准：

- `success=true`
- 返回售后分页/列表。

---

## 14. 后台订单管理接口

统一鉴权：管理员 token。

### 14.1 后台订单列表

- 接口：`GET /api/admin/orders`
- Query 示例：`page=1&size=10`

成功标准：

- `success=true`
- 返回订单分页。

### 14.2 后台订单详情

- 接口：`GET /api/admin/orders/{orderNo}`

成功标准：

- `success=true`
- `data.orderNo={orderNo}`。

### 14.3 查询订单同商品可切换 SKU

- 接口：`GET /api/admin/orders/{orderNo}/items/{orderItemId}/sku-switch-options`

前置：

- 先从后台订单详情拿到 `orderItemId`。

成功标准：

- `success=true`
- 返回可切换 SKU 列表。

### 14.4 后台取消订单

- 接口：`DELETE /api/admin/orders/{orderNo}`

前置建议：

- 使用待支付或可关闭订单。

成功标准：

- `success=true`
- 订单状态关闭/取消。

### 14.5 后台发货

- 接口：`PATCH /api/admin/orders/{orderNo}/ship`

前置建议：

- 使用已支付待发货订单。

成功标准：

- `success=true`
- 订单状态进入已发货。

### 14.6 后台完结订单

- 接口：`PATCH /api/admin/orders/{orderNo}/complete`

前置建议：

- 使用已发货订单。

成功标准：

- `success=true`
- 订单状态进入已完成。

### 14.7 后台修改订单收货地址

- 接口：`PUT /api/admin/orders/{orderNo}/receiver`
- Body：

```json
{
  "receiverName": "Admin Receiver Updated",
  "receiverPhone": "13800138000",
  "receiverProvinceName": "北京市",
  "receiverCityName": "北京市",
  "receiverDistrictName": "朝阳区",
  "receiverDetailAddress": "后台修改地址 1001"
}
```

成功标准：

- `success=true`
- 收货信息更新。

### 14.8 处理订单异常

- 接口：`POST /api/admin/orders/{orderNo}/exception-handle`
- Body 示例以 Swagger schema 为准。

建议优先测试下面几个专项接口，不直接测复杂通用异常处理。

### 14.9 标记支付异常

- 接口：`POST /api/admin/orders/{orderNo}/payment-exception`
- Body：

```json
{
  "note": "Swagger 手工标记支付异常"
}
```

成功标准：

- `success=true`
- 返回异常处理结果。

### 14.10 核验支付异常

- 接口：`POST /api/admin/orders/{orderNo}/payment-exception/verify`

成功标准：

- `success=true`
- 返回支付核验结果。

### 14.11 支付异常转支付同步状态

- 接口：`POST /api/admin/orders/{orderNo}/payment-exception/transfer-pay-sync`
- Body：

```json
{
  "note": "转支付同步状态"
}
```

成功标准：

- `success=true`。

### 14.12 标记支付异常待处理需求

- 接口：`POST /api/admin/orders/{orderNo}/payment-exception/pending-action`
- Body：

```json
{
  "action": "WAIT_FINANCE_CONFIRM",
  "note": "等待财务确认"
}
```

成功标准：

- `success=true`。

### 14.13 人工确认已支付

- 接口：`POST /api/admin/orders/{orderNo}/confirm-paid`
- Body：

```json
{
  "note": "Swagger 手工确认已支付"
}
```

成功标准：

- `success=true`
- 订单状态修复为已支付类状态。

### 14.14 人工恢复待支付

- 接口：`POST /api/admin/orders/{orderNo}/restore-pending-payment`
- Body：

```json
{
  "note": "Swagger 手工恢复待支付"
}
```

成功标准：

- `success=true`。

### 14.15 人工关闭订单并释放库存

- 接口：`POST /api/admin/orders/{orderNo}/close-and-release-stock`
- Body：

```json
{
  "note": "Swagger 手工关闭订单并释放库存"
}
```

成功标准：

- `success=true`
- 订单关闭且库存释放。

---

## 15. 后台支付、退款与回调记录

统一鉴权：管理员 token。

### 15.1 后台支付单列表

- 接口：`GET /api/admin/pays`
- Query 示例：`page=1&size=10`

成功标准：

- `success=true`
- 返回支付单分页。

### 15.2 后台支付单详情

- 接口：`GET /api/admin/pays/{orderNo}`

成功标准：

- `success=true`
- 返回指定订单支付详情。

### 15.3 后台关闭支付单

- 接口：`POST /api/admin/pays/{orderNo}/close`
- Body 示例：

```json
{
  "reason": "Swagger 手工关闭支付单"
}
```

成功标准：

- `success=true`
- 支付单关闭。

### 15.4 后台同步订单支付状态

- 接口：`POST /api/admin/pays/{orderNo}/sync-status`

成功标准：

- `success=true`。

### 15.5 后台补偿已成功支付订单状态

- 接口：`POST /api/admin/pays/{orderNo}/repair-paid`

成功标准：

- `success=true`。

### 15.6 查询全局退款单列表

- 接口：`GET /api/admin/pays/refunds`
- Query 示例：`page=1&size=10`

成功标准：

- `success=true`
- 返回退款分页。

记录：`refundNo`。

### 15.7 查询订单退款单列表

- 接口：`GET /api/admin/pays/{orderNo}/refunds`

成功标准：

- `success=true`
- 返回指定订单退款列表。

### 15.8 后台同步退款状态

- 接口：`POST /api/admin/pays/{orderNo}/refunds/{refundNo}/sync-status`
- Query：`refundAmountCent=100`

成功标准：

- `success=true`。

### 15.9 查询支付回调记录

- 接口：`GET /api/admin/pays/callback-records`
- Query 示例：`page=1&size=10`

成功标准：

- `success=true`
- 返回回调记录分页。

---

## 16. 后台售后管理

统一鉴权：管理员 token。

### 16.1 后台售后单列表

- 接口：`GET /api/admin/aftersales`
- Query 示例：`page=1&size=10`

成功标准：

- `success=true`
- 返回售后分页。

记录：`aftersaleNo`。

### 16.2 后台售后单详情

- 接口：`GET /api/admin/aftersales/{aftersaleNo}`

成功标准：

- `success=true`
- `data.aftersaleNo={aftersaleNo}`。

### 16.3 后台审核售后单

- 接口：`POST /api/admin/aftersales/{aftersaleNo}/review`
- Body 示例：

```json
{
  "approved": true,
  "reviewRemark": "Swagger 手工审核通过"
}
```

成功标准：

- `success=true`
- 售后状态进入审核通过或退款处理中。

---

## 17. 后台商品管理

统一鉴权：管理员 token。

### 17.1 后台类目列表

- 接口：`GET /api/admin/categories`

成功标准：

- `success=true`
- 返回类目数组。

### 17.2 新增类目

- 接口：`POST /api/admin/categories`
- Body：

```json
{
  "name": "测试类目-手机配件",
  "parentId": 1,
  "sortOrder": 10
}
```

成功标准：

- `success=true`
- `data.id` 非空。

记录：`categoryId`。

### 17.3 修改类目

- 接口：`PUT /api/admin/categories/{categoryId}`
- Body：

```json
{
  "name": "测试类目-已改",
  "parentId": 1,
  "sortOrder": 20,
  "status": "ENABLED"
}
```

成功标准：

- `success=true`
- 类目名称和状态更新。

### 17.4 商品销售表现默认阈值

- 查询：`GET /api/admin/products/sales-threshold-config`
- 修改：`PUT /api/admin/products/sales-threshold-config`

修改 Body 示例以 Swagger schema 为准。

成功标准：

- `success=true`
- 阈值配置可查询、可更新。

### 17.5 后台商品列表

- 接口：`GET /api/admin/products`
- Query 示例：`page=1&size=10`

成功标准：

- `success=true`
- 返回商品分页。

记录：`productId`。

### 17.6 后台商品详情

- 接口：`GET /api/admin/products/{productId}`

成功标准：

- `success=true`
- 返回商品详情。
- `data.skus[].id` 可用于修改商品和库存测试。

记录：`skuId`。

### 17.7 新增商品

- 接口：`POST /api/admin/products`
- Body：

```json
{
  "name": "手测新增商品A",
  "categoryId": 1,
  "mainImageUrl": "https://example.com/product-a.png",
  "description": "用于 Swagger 手工测试的商品",
  "skus": [
    {
      "skuCode": "TEST-SKU-NEW-001",
      "skuName": "手测商品A-默认规格",
      "specJson": "{\"color\":\"black\"}",
      "salePriceCent": 9990,
      "originPriceCent": 12990
    }
  ]
}
```

成功标准：

- `success=true`
- `data.id` 非空。

注意：

- `skuCode` 必须唯一，重复会失败。

### 17.8 修改商品

- 接口：`PUT /api/admin/products/{productId}`
- 前置：先调用后台商品详情获取已有 `skus[].id`。
- Body 示例：

```json
{
  "name": "手测新增商品A-已改",
  "categoryId": 1,
  "mainImageUrl": "https://example.com/product-a.png",
  "description": "用于 Swagger 手工测试的商品-已改",
  "status": "OFFLINE",
  "skus": [
    {
      "id": 1,
      "skuCode": "TEST-SKU-NEW-001",
      "skuName": "手测商品A-默认规格-已改",
      "specJson": "{\"color\":\"black\"}",
      "salePriceCent": 9990,
      "originPriceCent": 12990,
      "status": "OFFLINE"
    }
  ]
}
```

成功标准：

- `success=true`
- 商品名称、状态、SKU 信息更新。

### 17.9 修改商品状态

- 接口：`PATCH /api/admin/products/{productId}/status`
- Body：

```json
{
  "status": "ONLINE"
}
```

成功标准：

- `success=true`
- 商品状态更新。

### 17.10 处理商品运营事件

- 接口：`POST /api/admin/products/{productId}/violation-handle`
- Body 示例以 Swagger schema 为准。

成功标准：

- `success=true`
- 商品运营事件处理完成。

---

## 18. 后台库存管理

统一鉴权：管理员 token。

### 18.1 后台库存列表

- 接口：`GET /api/admin/stocks`
- Query 示例：`page=1&size=20`

成功标准：

- `success=true`
- 返回库存分页。

### 18.2 库存预警列表

- 接口：`GET /api/admin/stocks/warnings`
- Query 示例：`page=1&size=20`

成功标准：

- `success=true`
- 返回库存预警分页。

### 18.3 库存操作日志

- 接口：`GET /api/admin/stocks/logs`
- Query 示例：`page=1&size=20`

成功标准：

- `success=true`
- 返回库存日志分页。

### 18.4 调整库存策略

- 接口：`PUT /api/admin/stocks/{skuId}/policy`
- Body 示例以 Swagger schema 为准，常见字段：

```json
{
  "safeStock": 10,
  "warningThreshold": 5
}
```

成功标准：

- `success=true`
- 库存策略更新。

### 18.5 手工调整库存

- 接口：`PUT /api/admin/stocks/{skuId}/adjust`
- Body 示例：

```json
{
  "adjustType": "INCREASE",
  "quantity": 10,
  "reason": "Swagger 手工补库存"
}
```

成功标准：

- `success=true`
- 库存数量按预期变化。

### 18.6 处理库存预警

- 接口：`POST /api/admin/stocks/{skuId}/warning-handle`
- Body 示例以 Swagger schema 为准。

成功标准：

- `success=true`
- 预警状态处理完成。

### 18.7 发起库存一致性校验

- 接口：`POST /api/admin/stocks/{skuId}/consistency-check`

成功标准：

- `success=true`
- 返回库存一致性校验结果。

---

## 19. 库存内部接口

接口分组：库存。

说明：

- 当前内部库存接口建议统一带管理员 token 测试。
- 如果 Swagger 显示锁图标，请先 Authorize。

### 19.1 健康检查

- 接口：`GET /api/internal/stocks/health`
- 鉴权：管理员 token

成功标准：

- `success=true`
- `data.module=stock`
- `data.status=ready`。

### 19.2 查询库存

- 接口：`GET /api/internal/stocks/{skuId}`
- 鉴权：管理员 token

成功标准：

- `success=true`
- `data.skuId={skuId}`。

### 19.3 同步库存

- 接口：`POST /api/internal/stocks/{skuId}/sync`
- 鉴权：管理员 token

成功标准：

- `success=true`。

### 19.4 预占库存

- 接口：`POST /api/internal/stocks/reserve`
- Body：

```json
{
  "businessType": "ORDER",
  "businessNo": "MANUAL-ORDER-RESERVE-001",
  "items": [
    {
      "skuId": 1,
      "quantity": 1
    }
  ]
}
```

成功标准：

- `success=true`
- 库存预占成功。

### 19.5 取消预占库存

- 接口：`DELETE /api/internal/stocks/cancel`
- Body：

```json
{
  "businessType": "ORDER",
  "businessNo": "MANUAL-ORDER-RESERVE-001",
  "items": [
    {
      "skuId": 1,
      "quantity": 1
    }
  ]
}
```

成功标准：

- `success=true`
- 上一步预占释放。

### 19.6 确认库存

- 接口：`PUT /api/internal/stocks/confirm`
- Body：

```json
{
  "businessType": "ORDER",
  "businessNo": "MANUAL-ORDER-CONFIRM-001",
  "items": [
    {
      "skuId": 1,
      "quantity": 1
    }
  ]
}
```

成功标准：

- `success=true`。

### 19.7 锁定库存

- 接口：`POST /api/internal/stocks/lock`
- Body：

```json
{
  "businessType": "ORDER",
  "businessNo": "MANUAL-ORDER-LOCK-001",
  "items": [
    {
      "skuId": 1,
      "quantity": 1
    }
  ]
}
```

成功标准：

- `success=true`。

### 19.8 释放库存

- 接口：`DELETE /api/internal/stocks/release`
- Body：

```json
{
  "businessType": "ORDER",
  "businessNo": "MANUAL-ORDER-LOCK-001",
  "items": [
    {
      "skuId": 1,
      "quantity": 1
    }
  ]
}
```

成功标准：

- `success=true`。

### 19.9 扣减库存

- 接口：`PATCH /api/internal/stocks/deduct`
- Body：

```json
{
  "businessType": "ORDER",
  "businessNo": "MANUAL-ORDER-DEDUCT-001",
  "items": [
    {
      "skuId": 1,
      "quantity": 1
    }
  ]
}
```

成功标准：

- `success=true`。

---

## 20. 文件上传接口

接口分组：文件。

### 20.1 上传头像

- 接口：`POST /api/files/avatar`
- 鉴权：登录 token
- Content-Type：`multipart/form-data`
- 参数：`file`

成功标准：

- `success=true`
- 返回文件访问地址。

### 20.2 上传商品图片

- 接口：`POST /api/files/product-image`
- 鉴权：管理员 token
- Content-Type：`multipart/form-data`
- 参数：`file`

成功标准：

- `success=true`
- 返回商品图片地址。

---

## 21. 后台对账接口

统一鉴权：管理员 token。

### 21.1 对账增强概览

- 接口：`GET /api/admin/reconciliations/overview`

成功标准：

- `success=true`
- 返回对账概览统计。

### 21.2 后台对账列表

- 接口：`GET /api/admin/reconciliations`
- Query 示例：`page=1&size=10`

成功标准：

- `success=true`
- 返回对账分页。

### 21.3 执行单笔对账

- 接口：`POST /api/admin/reconciliations/{orderNo}/run`

成功标准：

- `success=true`
- 返回指定订单对账结果。

### 21.4 处理对账异常

- 接口：`POST /api/admin/reconciliations/{orderNo}/handle`
- Body 示例以 Swagger schema 为准。

成功标准：

- `success=true`
- 对账异常被处理。

### 21.5 支付/退款对账记录列表

- 接口：`GET /api/admin/reconciliations/pay-records`
- Query 示例：`page=1&size=10`

成功标准：

- `success=true`
- 返回支付/退款对账记录分页。

记录：`payReconciliationRecordId`。

### 21.6 支付/退款对账记录详情

- 接口：`GET /api/admin/reconciliations/pay-records/{id}`

成功标准：

- `success=true`
- 返回记录详情。

### 21.7 业务化支付/退款对账处置

- 接口：`POST /api/admin/reconciliations/pay-records/{id}/handle`
- Body 示例以 Swagger schema 为准。

成功标准：

- `success=true`。

### 21.8 标记支付/退款对账记录已处理

- 接口：`POST /api/admin/reconciliations/pay-records/{id}/done`
- Body：

```json
{
  "remark": "Swagger 手工标记已处理"
}
```

成功标准：

- `success=true`。

### 21.9 忽略支付/退款对账记录

- 接口：`POST /api/admin/reconciliations/pay-records/{id}/ignore`
- Body：

```json
{
  "remark": "Swagger 手工忽略"
}
```

成功标准：

- `success=true`。

### 21.10 库存对账列表

- 接口：`GET /api/admin/reconciliations/stocks`
- Query 示例：`page=1&size=10`

成功标准：

- `success=true`
- 返回库存对账分页。

记录：`stockReconciliationId`。

### 21.11 库存对账详情

- 接口：`GET /api/admin/reconciliations/stocks/{id}`

成功标准：

- `success=true`
- 返回库存对账详情。

### 21.12 修复库存对账差异

- 接口：`POST /api/admin/reconciliations/stocks/{id}/repair`
- Body：

```json
{
  "remark": "Swagger 手工修复库存差异"
}
```

成功标准：

- `success=true`。

### 21.13 忽略库存对账差异

- 接口：`POST /api/admin/reconciliations/stocks/{id}/ignore`
- Body：

```json
{
  "remark": "Swagger 手工忽略库存差异"
}
```

成功标准：

- `success=true`。

---

## 22. 线上人工对账接口

统一鉴权：管理员 token。

### 22.1 查询线上人工对账任务

- 接口：`GET /api/admin/online-reconcile-tasks`
- Query 示例：`page=1&size=10`

成功标准：

- `success=true`
- 返回任务分页。

### 22.2 创建线上人工对账任务

- 接口：`POST /api/admin/online-reconcile-tasks`
- Body 示例以 Swagger schema 为准，常见字段：

```json
{
  "channel": "MOCK",
  "reconcileDate": "2026-06-15",
  "remark": "Swagger 手工创建对账任务"
}
```

成功标准：

- `success=true`
- `data.id` 非空。

记录：`taskId`。

### 22.3 获取线上人工对账任务详情

- 接口：`GET /api/admin/online-reconcile-tasks/{taskId}`

成功标准：

- `success=true`
- `data.id={taskId}`。

### 22.4 生成本地账单快照

- 接口：`POST /api/admin/online-reconcile-tasks/{taskId}/local-bills/generate`

成功标准：

- `success=true`。

### 22.5 查询本地账单快照

- 接口：`GET /api/admin/online-reconcile-tasks/{taskId}/local-bills`
- Query 示例：`page=1&size=10`

成功标准：

- `success=true`
- 返回本地账单分页。

### 22.6 生成 Mock 渠道账单

- 接口：`POST /api/admin/online-reconcile-tasks/{taskId}/channel-bills/mock-generate`
- Body：

```json
{
  "mode": "MATCH"
}
```

成功标准：

- `success=true`。

### 22.7 上传支付宝渠道账单 CSV

- 接口：`POST /api/admin/online-reconcile-tasks/{taskId}/channel-bills/alipay-upload`
- Content-Type：`multipart/form-data`
- 参数：`file`

说明：

- 手工测试需要准备 CSV 文件。

### 22.8 查询渠道账单快照

- 接口：`GET /api/admin/online-reconcile-tasks/{taskId}/channel-bills`
- Query 示例：`page=1&size=10`

成功标准：

- `success=true`。

### 22.9 执行线上账单自动勾兑

- 接口：`POST /api/admin/online-reconcile-tasks/{taskId}/match`

成功标准：

- `success=true`
- 任务状态进入已勾兑或待处理差异状态。

### 22.10 查询线上对账差异明细

- 接口：`GET /api/admin/online-reconcile-tasks/{taskId}/diff-items`
- Query 示例：`page=1&size=10`

成功标准：

- `success=true`
- 返回差异分页。

记录：`diffId`。

### 22.11 查询线上对账差异详情

- 接口：`GET /api/admin/online-reconcile-diff-items/{diffId}`

成功标准：

- `success=true`
- 返回差异详情。

### 22.12 处理线上对账差异

- 接口：`POST /api/admin/online-reconcile-diff-items/{diffId}/handle`
- Body 示例以 Swagger schema 为准。

成功标准：

- `success=true`。

### 22.13 查询线上对账差异操作日志

- 接口：`GET /api/admin/online-reconcile-diff-items/{diffId}/logs`
- Query 示例：`page=1&size=20`

成功标准：

- `success=true`。

### 22.14 查询线上对账挂账跟进列表

- 接口：`GET /api/admin/online-reconcile-hangings`
- Query 示例：`page=1&size=10`

成功标准：

- `success=true`。

### 22.15 新增线上对账挂账跟进

- 接口：`POST /api/admin/online-reconcile-diff-items/{diffId}/follow-up`
- Body：

```json
{
  "remark": "Swagger 手工跟进挂账"
}
```

成功标准：

- `success=true`。

### 22.16 线上对账挂账转财务调账

- 接口：`POST /api/admin/online-reconcile-diff-items/{diffId}/transfer-finance`
- Body：

```json
{
  "remark": "Swagger 手工转财务调账"
}
```

成功标准：

- `success=true`。

### 22.17 线上对账挂账完结闭环

- 接口：`POST /api/admin/online-reconcile-diff-items/{diffId}/close-hanging`
- Body：

```json
{
  "remark": "Swagger 手工完结挂账"
}
```

成功标准：

- `success=true`。

### 22.18 线上对账报表归档查询

- 接口：`GET /api/admin/online-reconcile-archive-report`
- Query 示例：`startDate=2026-06-01&endDate=2026-06-15&channel=MOCK`

成功标准：

- `success=true`
- 返回归档报表。

### 22.19 查询线上对账任务操作日志

- 接口：`GET /api/admin/online-reconcile-tasks/{taskId}/logs`
- Query 示例：`page=1&size=20`

成功标准：

- `success=true`。

### 22.20 归档线上人工对账任务

- 接口：`POST /api/admin/online-reconcile-tasks/{taskId}/complete`
- Body：

```json
{
  "remark": "Swagger 手工归档"
}
```

成功标准：

- `success=true`
- 任务状态归档/完成。

---

## 23. 核心业务链路手测

### 23.1 用户下单支付闭环

按顺序执行：

1. `POST /api/users/login/password`，记录 `userToken`。
2. `GET /api/products/1`，记录可售 `skuId`。
3. `POST /api/orders`，记录 `orderId`、`orderNo`。
4. `POST /api/pay/orders?orderNo=<orderNo>&payChannel=MOCK&returnPath=/orders`，记录 `payOrderNo`。
5. `GET /api/pay/orders/{payOrderNo}`，确认支付单存在。
6. `POST /api/pay/callback/mock-success?orderNo=<orderNo>`。
7. `GET /api/orders/{orderId}`，确认订单状态已支付或待发货。
8. `GET /api/pay/reconcile?orderNo=<orderNo>`，确认支付对账一致。

通过标准：

- 订单创建成功。
- 支付单创建成功。
- mock 回调成功。
- 订单状态与支付状态一致。
- 对账结果一致。

### 23.2 后台发货完结链路

前置：已有已支付订单。

按顺序执行：

1. `POST /api/admin/login/password`，记录 `adminToken`。
2. `GET /api/admin/orders/{orderNo}`。
3. `PATCH /api/admin/orders/{orderNo}/ship`。
4. `GET /api/admin/orders/{orderNo}`，确认已发货。
5. `PATCH /api/admin/orders/{orderNo}/complete`。
6. `GET /api/admin/orders/{orderNo}`，确认已完成。

通过标准：

- 后台能查询订单。
- 订单可从已支付进入已发货。
- 订单可从已发货进入已完成。

### 23.3 退款/售后链路

前置：已有已支付或已完成订单。

按顺序执行：

1. 用户侧 `POST /api/orders/{orderId}/refund` 或 `POST /api/aftersales/refund`。
2. 记录 `refundNo` 或 `aftersaleNo`。
3. 管理端 `GET /api/admin/aftersales?page=1&size=10`。
4. 管理端 `GET /api/admin/aftersales/{aftersaleNo}`。
5. 管理端 `POST /api/admin/aftersales/{aftersaleNo}/review`。
6. 管理端 `GET /api/admin/pays/{orderNo}/refunds`。
7. 必要时执行 `POST /api/admin/pays/{orderNo}/refunds/{refundNo}/sync-status`。

通过标准：

- 用户可发起退款/售后。
- 后台可看到售后单。
- 审核后状态推进。
- 退款单可查询、可同步。

### 23.4 支付异常处理链路

前置：准备一笔状态异常或手工标记异常的订单。

按顺序执行：

1. `POST /api/admin/orders/{orderNo}/payment-exception`。
2. `POST /api/admin/orders/{orderNo}/payment-exception/verify`。
3. 视核验结果选择：
   - `POST /api/admin/orders/{orderNo}/confirm-paid`
   - `POST /api/admin/orders/{orderNo}/restore-pending-payment`
   - `POST /api/admin/orders/{orderNo}/close-and-release-stock`
   - `POST /api/admin/orders/{orderNo}/payment-exception/transfer-pay-sync`
4. `GET /api/admin/orders/{orderNo}`。
5. `GET /api/admin/reconciliations?keyword=<orderNo>` 或 `POST /api/admin/reconciliations/{orderNo}/run`。

通过标准：

- 异常标记成功。
- 核验结果返回明确。
- 人工处置后订单、支付、库存状态一致。

### 23.5 线上人工对账链路

按顺序执行：

1. `POST /api/admin/online-reconcile-tasks`，记录 `taskId`。
2. `POST /api/admin/online-reconcile-tasks/{taskId}/local-bills/generate`。
3. `POST /api/admin/online-reconcile-tasks/{taskId}/channel-bills/mock-generate`。
4. `GET /api/admin/online-reconcile-tasks/{taskId}/local-bills`。
5. `GET /api/admin/online-reconcile-tasks/{taskId}/channel-bills`。
6. `POST /api/admin/online-reconcile-tasks/{taskId}/match`。
7. `GET /api/admin/online-reconcile-tasks/{taskId}/diff-items`。
8. 如有差异，记录 `diffId`，执行差异详情、处理、日志、挂账跟进。
9. `POST /api/admin/online-reconcile-tasks/{taskId}/complete`。
10. `GET /api/admin/online-reconcile-archive-report`。

通过标准：

- 任务创建成功。
- 本地账单、渠道账单可生成。
- 自动勾兑可执行。
- 差异可查询、可处理。
- 任务可归档，归档报表可查询。

---

## 24. 常见失败排查

### 24.1 401 未登录

检查：

- Swagger Authorize 是否填写完整 `Bearer <token>`。
- token 是否过期。
- 是否用了用户 token 调后台接口，或用了管理员 token 调用户接口。

### 24.2 403 无权限

检查：

- 当前管理员是否有对应 `RequirePermission` 权限。
- 是否使用了超级管理员账号。
- 用户是否被禁用。

### 24.3 商品或 SKU 不存在

处理：

- 先测 `GET /api/products?page=1&size=10`。
- 再测 `GET /api/products/{productId}`。
- 使用详情中的真实 `skus[].id` 作为下单 `skuId`。

### 24.4 库存不足

处理：

- 先查 `GET /api/admin/stocks?skuId=<skuId>`。
- 必要时用 `PUT /api/admin/stocks/{skuId}/adjust` 补库存。
- 再重新创建订单。

### 24.5 订单状态不允许操作

示例：

- 未支付订单不能发货。
- 已取消订单不能支付。
- 未发货订单不能确认收货。
- 已完成订单不能重复完成。

处理：

- 按业务链路顺序重新准备测试订单。

### 24.6 对账无数据

处理：

- 先完整走一遍下单支付链路。
- 再执行 `GET /api/pay/reconcile?orderNo=<orderNo>`。
- 后台再执行 `POST /api/admin/reconciliations/{orderNo}/run`。

### 24.7 Swagger Body 字段不确定

处理：

- 以 Swagger 展示的 RequestBody schema 为准。
- 本文示例用于快速手测，若字段名与 schema 有差异，优先按 Swagger。

---

## 25. 建议测试记录表

| 序号 | 模块 | 接口 | 请求数据 | 响应结果 | 是否通过 | 备注 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | 用户认证 | `POST /api/users/login/password` | 默认用户 | token 非空 |  |  |
| 2 | 商品 | `GET /api/products/1` | productId=1 | skuId 非空 |  |  |
| 3 | 订单 | `POST /api/orders` | skuId=1 | orderNo 非空 |  |  |
| 4 | 支付 | `POST /api/pay/orders` | orderNo | payOrderNo 非空 |  |  |
| 5 | 支付回调 | `POST /api/pay/callback/mock-success` | orderNo | 支付成功 |  |  |
| 6 | 后台订单 | `PATCH /api/admin/orders/{orderNo}/ship` | orderNo | 已发货 |  |  |
| 7 | 售后 | `POST /api/orders/{orderId}/refund` | orderId | 退款申请成功 |  |  |
| 8 | 对账 | `POST /api/admin/reconciliations/{orderNo}/run` | orderNo | 对账完成 |  |  |

---

## 26. MVP 阶段建议优先级

### P0 必测

1. 用户登录。
2. 管理员登录。
3. 商品列表/详情。
4. 创建订单。
5. 创建支付单。
6. mock 支付成功回调。
7. 用户订单详情。
8. 后台订单详情。
9. 后台发货/完结。
10. 支付对账。

### P1 建议测

1. 地址增删改查。
2. 购物车增删改查。
3. 用户退款申请。
4. 后台售后审核。
5. 后台退款查询/同步。
6. 库存查询/调整。
7. 支付异常核验与处置。

### P2 补充测

1. 账号权限管理。
2. 商品新增/编辑/上下架。
3. 库存一致性校验。
4. 支付/退款对账记录处置。
5. 线上人工对账任务全流程。
6. 文件上传。
