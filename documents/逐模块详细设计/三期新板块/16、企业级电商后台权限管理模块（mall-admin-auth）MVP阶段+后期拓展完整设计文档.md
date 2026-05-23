# 企业级电商后台权限管理模块（MVP阶段归属 mall-admin，后期可拆 mall-admin-auth / mall-iam）完整设计文档

本文档面向 mallFei 企业级 B2C 电商平台后台权限管理板块，结合 MVP 阶段可落地目标与后期企业级拓展诉求，设计一套以 RBAC 为核心、权限上限为边界、高风险操作可审计、后续可平滑扩展到数据权限、审批流与统一 IAM 的后台权限体系。

本文档可作为后台权限管理、管理员账号管理、角色管理、菜单权限控制、接口鉴权、操作日志、权限初始化与后续企业化扩展的开发依据。

> 模块归属说明：MVP 阶段，后台权限管理功能建议落在 `mall-admin` 模块内实现；`mall-auth` 只负责认证、Token、会话与权限校验基础能力；后期当平台演进到多后台、多组织、多租户、多店铺、多仓库等复杂场景时，再将权限管理能力独立拆分为 `mall-admin-auth` 或 `mall-iam`。

# 一、模块定位

后台权限管理模块是电商后台系统的**安全入口、职责边界、操作约束、审计追踪**核心模块，主要负责控制后台管理员能够登录哪些后台系统、看到哪些菜单、访问哪些接口、执行哪些操作，以及权限变更与高风险操作是否可追溯。

在 mallFei 微服务架构中，权限管理不直接承载商品、订单、售后、财务等业务逻辑，而是为这些业务模块提供统一的后台访问控制能力。MVP 阶段该能力属于后台管理域，建议由 `mall-admin` 承载；`mall-auth` 提供登录态与鉴权能力，但不直接承担角色、权限、菜单、权限分配等后台管理业务。

核心目标：

1. 支撑 MVP 阶段后台基础角色划分和功能权限控制。
2. 避免客服、运营、财务等角色越权操作。
3. 对退款、财务导出、订单改价、账号管理等高风险权限进行严格控制。
4. 所有后台敏感操作可审计、可追踪、可回溯。
5. 后期可扩展数据权限、审批流、多租户、多店铺、多仓库、多组织架构。

# 二、设计原则

## 2.1 最小权限原则

每个后台角色默认只拥有完成岗位职责所需的最小权限，不因操作便利而授予额外高风险权限。

例如：

- 客服可以查看订单和添加备注，但不能执行退款。
- 订单运营可以发货和修改物流，但不能导出财务流水。
- 财务可以查看支付和执行退款，但不能修改订单履约状态。
- 审计人员只读，不允许修改任何业务数据。

## 2.2 权限上限优先于禁止权限

MVP 阶段推荐采用：

```text
角色默认权限 + 角色可授权权限上限
```

不建议只依赖“禁止权限黑名单”。黑名单容易在新增权限时遗漏，导致角色被错误授权。白名单式权限上限更安全。

最终可授予权限规则：

```text
角色最终可授予权限 = 角色权限上限 - 特殊禁止权限
```

其中“特殊禁止权限”仅用于极少数明确不能授权的场景，例如内置审计角色禁止所有写操作。

## 2.3 前后端双重控制

前端控制菜单、按钮、页面入口；后端控制接口访问、业务操作和数据变更。

前端权限控制只提升用户体验，不能作为安全边界。所有核心权限必须在后端校验。

## 2.4 高风险操作强审计

以下操作必须记录操作日志：

- 账号新增、禁用、启用、重置密码
- 角色新增、修改、删除
- 权限分配
- 订单关闭、订单改价
- 退款审核、退款执行
- 财务导出
- 库存调整
- 商品删除
- 用户禁用

日志必须记录操作人、操作时间、操作对象、请求参数摘要、操作前后变化、IP、User-Agent、操作结果。

## 2.5 MVP 可落地，后期好扩展

MVP 阶段不追求一次性实现完整 IAM、组织架构、审批流、ABAC，但数据库和权限编码需要为后续扩展预留空间。

建议 MVP 阶段先落地：

- 管理员账号
- 角色
- 权限
- 角色权限
- 角色权限上限
- 菜单权限
- 按钮权限
- 接口鉴权
- 操作日志

后期扩展：

- 数据权限
- 组织架构
- 审批流
- 权限模板
- 多店铺 / 多仓库 / 多区域
- 动态权限策略
- 权限变更审计

# 三、模块边界

## 3.1 MVP 阶段模块归属

MVP 阶段不建议把“后台权限管理业务”直接放入 `mall-auth`。`mall-auth` 应保持为认证鉴权基础模块，负责登录态、Token、会话、强制下线和权限校验能力；后台管理员、角色、权限、菜单、角色权限分配、操作日志等管理功能属于后台管理业务，建议由 `mall-admin` 承载。

MVP 阶段推荐归属：

| 能力 | 推荐归属 | 说明 |
|---|---|---|
| 管理员账号管理 | `mall-admin` | 后台账号新增、编辑、禁用、分配角色 |
| 角色管理 | `mall-admin` | 内置角色、自定义角色、角色启停 |
| 权限管理 | `mall-admin` | 权限清单、权限树、权限初始化、权限启停 |
| 菜单管理 | `mall-admin` | 后台菜单、路由、按钮权限配置 |
| 角色权限分配 | `mall-admin` | 给角色分配权限，并校验权限上限 |
| 角色权限上限 | `mall-admin` | 控制角色最多能被授予哪些权限 |
| 操作日志 | `mall-admin` / `mall-common` | 业务落库在后台域，通用日志能力可放 common |
| 登录认证 | `mall-auth` | 后台登录认证、Token 签发、Token 校验 |
| 会话管理 | `mall-auth` | 退出登录、踢下线、Session 管理 |
| 权限校验基础能力 | `mall-auth` / Sa-Token | 提供 `hasRole`、`hasPermission` 等基础能力 |

## 3.2 与 mall-auth 的边界

`mall-auth` 负责认证与令牌管理，包括后台管理员登录态、Token 签发、Token 校验、强制下线、权限校验基础能力等。

`mall-admin` 中的权限管理板块负责管理员、角色、权限、菜单、权限分配、权限上限和权限数据来源。

边界说明：

| 模块 | 职责 | 不建议承担 |
|---|---|---|
| `mall-auth` | 登录认证、Token 管理、会话管理、强制下线、权限校验基础能力 | 角色 CRUD、权限 CRUD、菜单 CRUD、权限分配、后台操作日志管理 |
| `mall-admin` / 权限管理 | 管理员账号、角色、权限、菜单、按钮、权限分配、权限上限、操作日志 | Token 签发、底层会话管理、认证协议实现 |

运行时关系：

```text
后台管理员登录
    ↓
mall-admin 查询管理员账号、角色、状态
    ↓
mall-auth 校验登录态并签发 Token
    ↓
mall-admin 加载角色权限集合
    ↓
前端获取菜单与权限标识
    ↓
后续请求由 Sa-Token / 后端权限注解进行接口鉴权
```

权限变更关系：

```text
超管在 mall-admin 修改角色权限
    ↓
mall-admin 校验权限上限并保存权限关系
    ↓
记录权限变更日志
    ↓
刷新或失效 mall-auth / Sa-Token 中的权限缓存
```

## 3.3 后期独立拆分边界

当平台从单后台 B2C MVP 演进到多后台、多组织、多租户、多店铺、多仓库、多区域时，可将 `mall-admin` 内的权限管理能力独立拆分为 `mall-admin-auth` 或 `mall-iam`。

后期拆分后的推荐边界：

| 模块 | 职责 |
|---|---|
| `mall-auth` | 认证、Token、Session、登录协议、统一登录态 |
| `mall-admin` | 后台业务管理入口，调用 IAM 能力获取权限与菜单 |
| `mall-admin-auth` / `mall-iam` | 管理员、组织、角色、权限、菜单、数据权限、权限策略、审批策略 |
| 各业务模块 | 根据权限标识执行业务接口鉴权和业务规则校验 |

## 3.4 与业务模块的边界

权限管理模块不处理业务规则，只提供权限判断。

例如：

- 商品模块判断当前管理员是否有 `product:update`。
- 订单模块判断当前管理员是否有 `order:ship`。
- 售后模块判断当前管理员是否有 `aftersale:audit`。
- 财务模块判断当前管理员是否有 `refund:execute`。

业务模块仍然负责自己的业务状态校验，例如订单是否允许发货、退款单是否允许执行退款。

# 四、MVP 阶段核心能力

## 4.1 管理员账号管理

MVP 阶段支持后台管理员账号的基础管理。

核心功能：

1. 管理员列表查询
2. 管理员详情查看
3. 新增管理员
4. 编辑管理员基础信息
5. 启用 / 禁用管理员
6. 重置管理员密码
7. 分配角色
8. 查看管理员登录记录和操作记录

MVP 约束：

- 超级管理员不能禁用自己。
- 超级管理员不能移除自己的超管角色。
- 禁用管理员后，应立即使其后台登录态失效。
- 管理员账号不建议物理删除，只做禁用或逻辑删除。

## 4.2 角色管理

角色用于描述后台岗位职责，是权限分配的主要载体。

MVP 阶段内置角色：

| 角色编码 | 角色名称 | 说明 |
|---|---|---|
| `SUPER_ADMIN` | 超级管理员 | 系统最高权限，负责账号、角色、权限和高风险操作 |
| `PRODUCT_OPERATOR` | 商品运营 | 负责商品资料、上下架、类目品牌维护 |
| `ORDER_OPERATOR` | 订单运营 | 负责订单发货、物流维护、售后协作 |
| `STOCK_OPERATOR` | 库存运营 | 负责库存查看、入库、出库、盘点 |
| `FINANCE_OPERATOR` | 财务人员 | 负责支付、退款、对账、财务报表 |
| `CUSTOMER_SERVICE` | 客服人员 | 负责用户咨询、订单查询、售后跟进 |
| `USER_OPERATOR` | 用户运营 | 负责用户资料和用户状态维护 |
| `AUDIT_OPERATOR` | 审计人员 | 负责只读查看业务数据和日志 |

角色管理规则：

- 内置角色默认不允许删除。
- 超级管理员角色不允许被普通管理员修改。
- 非超管角色不能被授予超出权限上限的权限。
- 审计角色只允许只读权限。

## 4.3 权限管理

权限是系统中最小的功能控制单元。

权限编码规范：

```text
资源:动作
```

复杂场景可使用三级结构：

```text
资源:子资源:动作
```

示例：

```text
order:view
order:ship
order:logistics:update
refund:execute
system:role:manage
```

权限类型建议分为：

| 权限类型 | 说明 | 示例 |
|---|---|---|
| 菜单权限 | 控制后台菜单是否可见 | `menu:order` |
| 页面权限 | 控制页面访问 | `order:view` |
| 按钮权限 | 控制页面按钮显示 | `order:ship` |
| 接口权限 | 控制后端接口访问 | `refund:execute` |
| 数据权限 | 控制可访问数据范围，后期扩展 | `order:data:store` |

MVP 阶段重点落地菜单权限、按钮权限、接口权限。

## 4.4 角色权限上限

角色权限上限用于限制某个角色最多能被授予哪些权限。

例如客服角色的权限上限只包含：

- `dashboard:view`
- `user:view`
- `order:view`
- `order:remark`
- `order:log:view`
- `aftersale:view`
- `aftersale:remark`

即使拥有角色分配权限的管理员误操作，也不能给客服添加 `refund:execute` 或 `finance:export`。

后端必须在保存角色权限时校验：

```text
待分配权限集合 必须是 角色权限上限集合 的子集
```

# 五、MVP 权限清单

## 5.1 完整权限清单

| 模块 | 权限标识 | 权限名称 | 权限说明 | 风险等级 |
|---|---|---|---|---|
| 仪表盘 | `dashboard:view` | 仪表盘查看 | 查看后台统计概览、运营数据 | 低 |
| 用户管理 | `user:view` | 用户查看 | 查看用户列表、用户详情 | 低 |
| 用户管理 | `user:edit` | 用户编辑 | 修改用户基础信息 | 中 |
| 用户管理 | `user:disable` | 禁用/启用用户 | 禁用或启用用户账号 | 高 |
| 用户管理 | `user:sensitive:view` | 敏感信息查看 | 查看手机号、实名信息等敏感数据 | 高 |
| 商品管理 | `product:view` | 商品查看 | 查看商品列表、商品详情 | 低 |
| 商品管理 | `product:create` | 商品新增 | 新增商品、SPU、SKU | 中 |
| 商品管理 | `product:update` | 商品编辑 | 修改商品标题、价格、详情、规格等 | 中 |
| 商品管理 | `product:on_sale` | 商品上架 | 将商品上架销售 | 中 |
| 商品管理 | `product:off_sale` | 商品下架 | 将商品下架 | 中 |
| 商品管理 | `product:delete` | 商品删除 | 删除或逻辑删除商品 | 高 |
| 商品管理 | `category:manage` | 类目管理 | 新增、修改、删除商品类目 | 中 |
| 商品管理 | `brand:manage` | 品牌管理 | 新增、修改、删除品牌 | 中 |
| 库存管理 | `stock:view` | 库存查看 | 查看可售库存、锁定库存 | 低 |
| 库存管理 | `stock:log:view` | 库存日志查看 | 查看库存变更流水 | 低 |
| 库存管理 | `stock:adjust` | 库存调整 | 手动调整库存数量 | 高 |
| 库存管理 | `stock:in` | 商品入库 | 执行商品入库操作 | 中 |
| 库存管理 | `stock:out` | 商品出库 | 执行商品出库操作 | 中 |
| 库存管理 | `stock:check` | 库存盘点 | 执行库存盘点、盘盈盘亏处理 | 中 |
| 订单管理 | `order:view` | 订单查看 | 查看订单列表、订单详情 | 低 |
| 订单管理 | `order:export` | 订单导出 | 导出订单数据 | 高 |
| 订单管理 | `order:remark` | 订单备注 | 添加或修改后台订单备注 | 低 |
| 订单管理 | `order:ship` | 订单发货 | 填写物流信息并发货 | 中 |
| 订单管理 | `order:logistics:update` | 修改物流 | 修改物流公司、物流单号 | 中 |
| 订单管理 | `order:close` | 关闭订单 | 后台关闭未支付或异常订单 | 高 |
| 订单管理 | `order:price:adjust` | 订单改价 | 修改订单应付金额、运费或优惠 | 高 |
| 订单管理 | `order:log:view` | 订单日志查看 | 查看订单状态流转日志 | 低 |
| 售后管理 | `aftersale:view` | 售后查看 | 查看退货、换货、退款申请 | 低 |
| 售后管理 | `aftersale:audit` | 售后审核 | 审核售后申请，同意或拒绝 | 中 |
| 售后管理 | `aftersale:receive` | 确认退货收货 | 确认已收到用户退回商品 | 中 |
| 售后管理 | `aftersale:close` | 关闭售后 | 关闭异常或已处理售后单 | 高 |
| 售后管理 | `aftersale:remark` | 售后备注 | 添加售后处理备注 | 低 |
| 退款管理 | `refund:view` | 退款查看 | 查看退款单、退款状态 | 低 |
| 退款管理 | `refund:audit` | 退款审核 | 审核是否允许退款 | 中 |
| 退款管理 | `refund:execute` | 执行退款 | 调用支付渠道执行真实退款 | 高 |
| 退款管理 | `refund:export` | 退款导出 | 导出退款流水 | 高 |
| 财务管理 | `finance:view` | 财务查看 | 查看支付流水、对账数据、金额汇总 | 中 |
| 财务管理 | `finance:export` | 财务导出 | 导出支付、退款、对账报表 | 高 |
| 财务管理 | `payment:view` | 支付单查看 | 查看支付订单、支付状态、支付渠道 | 中 |
| 财务管理 | `reconciliation:view` | 对账查看 | 查看支付渠道对账结果 | 中 |
| 财务管理 | `reconciliation:handle` | 对账处理 | 处理对账异常、差异账单 | 高 |
| 系统管理 | `admin:view` | 后台账号查看 | 查看后台管理员账号列表 | 中 |
| 系统管理 | `admin:create` | 后台账号新增 | 新增后台管理员账号 | 高 |
| 系统管理 | `admin:update` | 后台账号编辑 | 修改后台管理员信息 | 高 |
| 系统管理 | `admin:disable` | 后台账号禁用 | 禁用或启用后台管理员 | 高 |
| 系统管理 | `role:view` | 角色查看 | 查看角色列表、角色详情 | 中 |
| 系统管理 | `role:manage` | 角色管理 | 新增、修改、删除角色 | 高 |
| 系统管理 | `permission:view` | 权限查看 | 查看权限树、权限列表 | 中 |
| 系统管理 | `permission:assign` | 权限分配 | 给角色分配权限 | 高 |
| 日志管理 | `log:operation:view` | 操作日志查看 | 查看后台操作日志 | 中 |
| 日志管理 | `log:login:view` | 登录日志查看 | 查看后台登录日志 | 中 |

# 六、角色权限矩阵

| 角色 | 默认权限 |
|---|---|
| `SUPER_ADMIN` 超级管理员 | 全部权限 |
| `PRODUCT_OPERATOR` 商品运营 | `dashboard:view`、`product:view`、`product:create`、`product:update`、`product:on_sale`、`product:off_sale`、`category:manage`、`brand:manage`、`stock:view`、`stock:log:view` |
| `ORDER_OPERATOR` 订单运营 | `dashboard:view`、`order:view`、`order:remark`、`order:ship`、`order:logistics:update`、`order:log:view`、`aftersale:view`、`aftersale:audit`、`aftersale:receive`、`aftersale:remark` |
| `STOCK_OPERATOR` 库存运营 | `dashboard:view`、`stock:view`、`stock:log:view`、`stock:in`、`stock:out`、`stock:check`、`product:view` |
| `FINANCE_OPERATOR` 财务人员 | `dashboard:view`、`finance:view`、`finance:export`、`payment:view`、`refund:view`、`refund:execute`、`refund:export`、`reconciliation:view`、`reconciliation:handle`、`order:view`、`aftersale:view` |
| `CUSTOMER_SERVICE` 客服人员 | `dashboard:view`、`user:view`、`order:view`、`order:remark`、`order:log:view`、`aftersale:view`、`aftersale:remark` |
| `USER_OPERATOR` 用户运营 | `dashboard:view`、`user:view`、`user:edit`、`user:disable`、`order:view` |
| `AUDIT_OPERATOR` 审计人员 | `dashboard:view`、`user:view`、`product:view`、`stock:view`、`stock:log:view`、`order:view`、`order:log:view`、`aftersale:view`、`refund:view`、`finance:view`、`payment:view`、`reconciliation:view`、`admin:view`、`role:view`、`permission:view`、`log:operation:view`、`log:login:view` |

# 七、角色权限上限设计

## 7.1 权限上限表

| 角色 | 可授权权限范围 | 明确不应拥有的权限 |
|---|---|---|
| `SUPER_ADMIN` | 全部权限 | 无业务禁止权限，但不能禁用自己、删除自己、移除自己的超管角色 |
| `PRODUCT_OPERATOR` | 商品相关权限、库存只读权限、日志只读权限 | `product:delete`、`stock:adjust`、`order:price:adjust`、`refund:execute`、`finance:export`、`admin:*`、`role:*`、`permission:*` |
| `ORDER_OPERATOR` | 订单处理、售后处理、订单日志、用户只读 | `order:price:adjust`、`refund:execute`、`finance:export`、`stock:adjust`、`product:delete`、`admin:*`、`role:*`、`permission:*` |
| `STOCK_OPERATOR` | 库存相关权限、商品只读权限 | `refund:*`、`finance:*`、`order:price:adjust`、`product:delete`、`admin:*`、`role:*`、`permission:*` |
| `FINANCE_OPERATOR` | 财务、支付、退款执行、对账、订单只读、售后只读 | `order:ship`、`order:close`、`order:price:adjust`、`product:*`、`stock:adjust`、`admin:*`、`role:*`、`permission:*` |
| `CUSTOMER_SERVICE` | 用户只读、订单只读、订单备注、售后只读、售后备注 | `user:disable`、`user:sensitive:view`、`order:ship`、`order:close`、`order:price:adjust`、`refund:execute`、`finance:*`、`product:delete`、`stock:adjust`、`admin:*`、`role:*`、`permission:*` |
| `USER_OPERATOR` | 用户相关权限、订单只读 | `refund:*`、`finance:*`、`order:price:adjust`、`product:delete`、`stock:adjust`、`admin:*`、`role:*`、`permission:*` |
| `AUDIT_OPERATOR` | 只读权限、日志权限 | 所有新增、编辑、删除、处理、审核、执行、导出、管理类权限 |

## 7.2 审计角色权限规则

审计角色只允许：

```text
*:view
*:log:view
log:*:view
```

审计角色禁止：

```text
*:create
*:update
*:delete
*:edit
*:manage
*:assign
*:execute
*:audit
*:handle
*:export
*:adjust
*:ship
*:close
*:disable
```

# 八、核心业务流程

## 8.1 后台管理员登录流程

1. 管理员输入账号和密码。
2. `mall-admin` 查询管理员账号，校验账号是否存在、是否启用、是否允许登录后台。
3. `mall-auth` 负责密码认证、登录态创建和后台 Token 签发。
4. `mall-admin` 加载管理员角色、权限集合和菜单集合。
5. 返回管理员基础信息、角色列表、权限标识集合、菜单列表和后台 Token。
6. 前端根据菜单和按钮权限渲染后台页面。
7. 后续接口请求携带 Token，由 `mall-auth` / Sa-Token 完成登录态校验，由业务接口完成权限标识校验。

## 8.2 菜单加载流程

1. 前端请求当前管理员菜单。
2. 后端读取当前管理员角色。
3. 聚合角色对应菜单权限。
4. 过滤禁用菜单和无权限菜单。
5. 返回树形菜单结构。
6. 前端动态生成路由和侧边栏。

## 8.3 接口鉴权流程

1. 管理员携带 Token 请求后台接口。
2. 网关或服务端解析 Token。
3. 获取管理员 ID、角色、权限集合。
4. 根据接口所需权限进行校验。
5. 权限通过则执行业务逻辑。
6. 权限不足返回 403。
7. 高风险接口记录操作日志。

## 8.4 角色权限分配流程

1. 超管进入角色管理页面。
2. 选择目标角色。
3. 系统加载目标角色当前权限和可授权权限上限。
4. 前端仅展示可授权范围内的权限。
5. 提交权限变更。
6. 后端再次校验待分配权限是否超出权限上限。
7. 校验通过后保存角色权限。
8. 记录权限变更日志。
9. 可选：强制拥有该角色的管理员重新登录或刷新权限缓存。

## 8.5 高风险操作流程

以 `refund:execute` 为例：

1. 管理员点击执行退款。
2. 前端判断是否拥有 `refund:execute`。
3. 后端接口再次校验 `refund:execute`。
4. 校验退款单业务状态。
5. 二次确认或输入操作原因。
6. 调用支付退款接口。
7. 记录操作日志和退款日志。
8. 返回退款处理结果。

# 九、数据模型设计

## 9.1 管理员表 `admin_user`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | bigint | 管理员ID |
| `username` | varchar | 登录账号 |
| `password_hash` | varchar | 密码哈希 |
| `nickname` | varchar | 昵称 |
| `mobile` | varchar | 手机号 |
| `email` | varchar | 邮箱 |
| `avatar` | varchar | 头像 |
| `status` | tinyint | 状态：1启用，0禁用 |
| `builtin` | tinyint | 是否内置账号 |
| `last_login_time` | datetime | 最后登录时间 |
| `last_login_ip` | varchar | 最后登录IP |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |
| `deleted` | tinyint | 逻辑删除标记 |

## 9.2 角色表 `admin_role`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | bigint | 角色ID |
| `code` | varchar | 角色编码 |
| `name` | varchar | 角色名称 |
| `role_type` | varchar | 角色类型 |
| `description` | varchar | 角色描述 |
| `builtin` | tinyint | 是否内置角色 |
| `status` | tinyint | 是否启用 |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |
| `deleted` | tinyint | 逻辑删除标记 |

## 9.3 权限表 `admin_permission`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | bigint | 权限ID |
| `code` | varchar | 权限编码 |
| `name` | varchar | 权限名称 |
| `module` | varchar | 所属模块 |
| `type` | varchar | 权限类型：MENU、PAGE、BUTTON、API、DATA |
| `risk_level` | varchar | 风险等级：LOW、MEDIUM、HIGH |
| `description` | varchar | 权限说明 |
| `status` | tinyint | 是否启用 |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |
| `deleted` | tinyint | 逻辑删除标记 |

## 9.4 管理员角色表 `admin_user_role`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | bigint | 主键ID |
| `admin_user_id` | bigint | 管理员ID |
| `role_id` | bigint | 角色ID |
| `created_at` | datetime | 创建时间 |

## 9.5 角色权限表 `admin_role_permission`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | bigint | 主键ID |
| `role_id` | bigint | 角色ID |
| `permission_id` | bigint | 权限ID |
| `created_at` | datetime | 创建时间 |

## 9.6 角色权限上限表 `admin_role_permission_scope`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | bigint | 主键ID |
| `role_id` | bigint | 角色ID |
| `permission_id` | bigint | 该角色允许被授予的权限ID |
| `created_at` | datetime | 创建时间 |

## 9.7 菜单表 `admin_menu`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | bigint | 菜单ID |
| `parent_id` | bigint | 父菜单ID |
| `name` | varchar | 菜单名称 |
| `path` | varchar | 前端路由路径 |
| `component` | varchar | 前端组件路径 |
| `icon` | varchar | 菜单图标 |
| `permission_code` | varchar | 菜单对应权限编码 |
| `sort` | int | 排序 |
| `visible` | tinyint | 是否可见 |
| `status` | tinyint | 是否启用 |
| `created_at` | datetime | 创建时间 |
| `updated_at` | datetime | 更新时间 |

## 9.8 操作日志表 `admin_operation_log`

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | bigint | 日志ID |
| `admin_user_id` | bigint | 操作管理员ID |
| `username` | varchar | 操作账号 |
| `module` | varchar | 操作模块 |
| `operation` | varchar | 操作类型 |
| `target_type` | varchar | 操作对象类型 |
| `target_id` | varchar | 操作对象ID |
| `permission_code` | varchar | 涉及权限 |
| `request_method` | varchar | 请求方法 |
| `request_uri` | varchar | 请求地址 |
| `request_params` | text | 请求参数摘要，敏感字段脱敏 |
| `before_data` | text | 操作前数据摘要 |
| `after_data` | text | 操作后数据摘要 |
| `result` | varchar | 操作结果 |
| `failure_reason` | varchar | 失败原因 |
| `ip` | varchar | 操作IP |
| `user_agent` | varchar | User-Agent |
| `created_at` | datetime | 操作时间 |

# 十、接口设计建议

## 10.1 管理员接口

| 接口 | 方法 | 权限 | 说明 |
|---|---|---|---|
| `/admin/users` | GET | `admin:view` | 管理员列表 |
| `/admin/users/{id}` | GET | `admin:view` | 管理员详情 |
| `/admin/users` | POST | `admin:create` | 新增管理员 |
| `/admin/users/{id}` | PUT | `admin:update` | 编辑管理员 |
| `/admin/users/{id}/disable` | POST | `admin:disable` | 禁用管理员 |
| `/admin/users/{id}/enable` | POST | `admin:disable` | 启用管理员 |
| `/admin/users/{id}/roles` | PUT | `admin:update` | 分配角色 |

## 10.2 角色接口

| 接口 | 方法 | 权限 | 说明 |
|---|---|---|---|
| `/admin/roles` | GET | `role:view` | 角色列表 |
| `/admin/roles/{id}` | GET | `role:view` | 角色详情 |
| `/admin/roles` | POST | `role:manage` | 新增角色 |
| `/admin/roles/{id}` | PUT | `role:manage` | 编辑角色 |
| `/admin/roles/{id}/permissions` | PUT | `permission:assign` | 分配角色权限 |
| `/admin/roles/{id}/permission-scope` | GET | `permission:view` | 查询角色可授权权限上限 |

## 10.3 权限和菜单接口

| 接口 | 方法 | 权限 | 说明 |
|---|---|---|---|
| `/admin/permissions/tree` | GET | `permission:view` | 权限树 |
| `/admin/menus/current` | GET | 登录即可 | 当前管理员菜单 |
| `/admin/permissions/current` | GET | 登录即可 | 当前管理员权限标识集合 |
| `/admin/logs/operation` | GET | `log:operation:view` | 操作日志列表 |
| `/admin/logs/login` | GET | `log:login:view` | 登录日志列表 |

# 十一、前端权限控制设计

## 11.1 菜单控制

登录成功后，前端获取当前管理员菜单树，根据后端返回结果动态生成侧边栏和路由。

原则：

- 没有菜单权限，不显示菜单入口。
- 没有页面权限，不能访问页面路由。
- 直接输入 URL 访问无权限页面，应跳转 403 页面。

## 11.2 按钮控制

前端维护权限判断方法，例如：

```text
hasPermission('order:ship')
```

按钮显示规则：

| 按钮 | 所需权限 |
|---|---|
| 发货 | `order:ship` |
| 修改物流 | `order:logistics:update` |
| 关闭订单 | `order:close` |
| 执行退款 | `refund:execute` |
| 财务导出 | `finance:export` |
| 禁用用户 | `user:disable` |

注意：按钮隐藏不等于安全，后端接口必须再次校验。

## 11.3 高风险权限展示

高风险权限在权限树中建议使用醒目标识，例如：

- 高风险标签
- 二次确认弹窗
- 操作原因输入框
- 操作日志说明

MVP 阶段至少应在执行高风险操作时做二次确认。

# 十二、后端鉴权设计

## 12.1 注解式权限校验

建议在接口层使用注解进行权限校验，例如：

```text
@RequiresPermission("order:ship")
```

或者结合 Sa-Token：

```text
StpUtil.checkPermission("order:ship")
```

## 12.2 权限缓存

MVP 阶段可以缓存当前管理员权限集合，减少频繁查库。

缓存建议：

| 缓存 Key | 内容 | 失效策略 |
|---|---|---|
| `admin:permissions:{adminId}` | 管理员权限编码集合 | 权限变更后删除 |
| `admin:menus:{adminId}` | 管理员菜单树 | 权限变更后删除 |
| `admin:roles:{adminId}` | 管理员角色集合 | 角色变更后删除 |

权限变更后应刷新或删除相关管理员缓存。

## 12.3 禁用管理员后的会话处理

管理员被禁用后：

1. 更新管理员状态为禁用。
2. 删除权限缓存。
3. 强制管理员退出登录。
4. 记录操作日志。

# 十三、MVP 实施范围

## 13.1 MVP 必须实现

| 能力 | 是否必须 | 说明 |
|---|---|---|
| 管理员登录 | 是 | 已登录后台才能访问管理系统 |
| 管理员管理 | 是 | 新增、编辑、禁用、分配角色 |
| 角色管理 | 是 | 内置角色、角色权限分配 |
| 权限管理 | 是 | 权限树、权限初始化 |
| 角色权限上限 | 是 | 防止误授权和越权 |
| 菜单权限 | 是 | 控制后台菜单可见性 |
| 按钮权限 | 是 | 控制页面操作入口 |
| 接口鉴权 | 是 | 后端安全边界 |
| 操作日志 | 是 | 高风险操作可追溯 |
| 登录日志 | 建议 | 用于安全审计 |

## 13.2 MVP 暂不实现但预留

| 能力 | MVP 是否实现 | 预留方式 |
|---|---|---|
| 数据权限 | 暂不完整实现 | 权限表预留 DATA 类型，业务表预留店铺/仓库/组织字段 |
| 审批流 | 暂不实现 | 高风险操作保留操作原因字段 |
| 多组织架构 | 暂不实现 | 管理员表预留部门/组织扩展字段 |
| 多店铺权限 | 暂不实现 | 后续通过数据权限扩展 |
| 多仓库权限 | 暂不实现 | 后续在库存模块和数据权限中扩展 |
| 权限申请 | 暂不实现 | 后续接入审批流 |

# 十四、后期拓展设计

## 14.1 数据权限

后期当系统支持多店铺、多仓库、多区域、多部门时，需要引入数据权限。

数据范围类型：

| 数据范围 | 说明 |
|---|---|
| `ALL` | 全部数据 |
| `SELF` | 仅本人创建或处理的数据 |
| `DEPARTMENT` | 本部门数据 |
| `STORE` | 指定店铺数据 |
| `WAREHOUSE` | 指定仓库数据 |
| `REGION` | 指定区域数据 |

示例：

- 订单运营 A 只能看华东区域订单。
- 库存运营 B 只能管理杭州仓库存。
- 财务 C 可以查看全平台支付流水。

## 14.2 高风险操作审批

后期可将以下操作接入审批流：

- 订单改价
- 执行退款
- 大额退款
- 财务导出
- 库存大额调整
- 角色权限变更
- 管理员新增和禁用

审批流字段可包括：

| 字段 | 说明 |
|---|---|
| `approval_status` | 审批状态 |
| `approval_instance_id` | 审批实例ID |
| `applicant_id` | 申请人 |
| `approver_id` | 审批人 |
| `apply_reason` | 申请原因 |
| `approval_comment` | 审批意见 |

## 14.3 权限变更审计

后期建议单独建立权限变更日志，记录：

- 谁修改了角色
- 给哪个角色新增了哪些权限
- 移除了哪些权限
- 修改前后权限差异
- 修改原因
- 是否经过审批

## 14.4 临时授权

企业级场景下可能需要临时授权，例如大促期间临时开放订单导出权限。

临时授权需要具备：

- 授权开始时间
- 授权结束时间
- 授权原因
- 自动过期
- 审批记录
- 到期提醒

## 14.5 多端后台权限

后期如果存在平台后台、商家后台、仓库后台、客服后台，应区分后台应用维度。

权限表可增加：

| 字段 | 说明 |
|---|---|
| `app_code` | 后台应用编码，如 PLATFORM_ADMIN、MERCHANT_ADMIN、WAREHOUSE_ADMIN |

# 十五、安全要求

## 15.1 密码安全

- 密码必须哈希存储，禁止明文保存。
- 建议使用 BCrypt 或同等级别算法。
- 重置密码后应强制重新登录。
- 连续登录失败可限制登录或增加验证码。

## 15.2 敏感信息脱敏

操作日志和接口返回中，以下字段需要脱敏：

- 手机号
- 身份证号
- 银行卡号
- 支付流水号部分信息
- 密码
- Token

## 15.3 防越权

必须防止：

- 普通管理员修改超管账号
- 普通管理员给自己提权
- 普通管理员修改自己的角色
- 非超管分配超出权限上限的权限
- 禁用自己的账号
- 删除或禁用最后一个超管账号

# 十六、异常与错误码建议

| 错误码 | 含义 | 场景 |
|---|---|---|
| `ADMIN_403` | 无权限访问 | 当前管理员无接口权限 |
| `ADMIN_ROLE_SCOPE_EXCEEDED` | 超出角色权限上限 | 给角色分配了不允许的权限 |
| `ADMIN_CANNOT_DISABLE_SELF` | 不能禁用自己 | 管理员尝试禁用自身账号 |
| `ADMIN_CANNOT_MODIFY_SUPER_ADMIN` | 不能修改超管 | 非超管修改超管账号 |
| `ADMIN_LAST_SUPER_ADMIN_REQUIRED` | 至少保留一个超管 | 删除或降级最后一个超管 |
| `ADMIN_ACCOUNT_DISABLED` | 管理员已禁用 | 禁用账号尝试登录 |
| `ADMIN_PERMISSION_NOT_FOUND` | 权限不存在 | 分配了不存在或已禁用权限 |

# 十七、开发落地优先级

## 17.1 第一阶段：基础可用

1. 权限表初始化
2. 角色表初始化
3. 角色权限表初始化
4. 管理员角色绑定
5. 登录后返回权限集合
6. 前端菜单和按钮权限控制
7. 后端接口权限校验

## 17.2 第二阶段：安全增强

1. 角色权限上限表
2. 权限分配时校验上限
3. 高风险操作二次确认
4. 操作日志
5. 禁用管理员强制下线
6. 权限缓存刷新

## 17.3 第三阶段：企业级拓展

1. 数据权限
2. 审批流
3. 权限变更审计
4. 临时授权
5. 多组织、多店铺、多仓库权限
6. 权限申请和审批

# 十八、最终建议

MVP 阶段建议采用以下方案：

```text
RBAC 功能权限 + 角色权限上限 + 高风险操作日志 + 前后端双重鉴权
```

暂不建议 MVP 阶段直接上复杂 ABAC 或完整审批流，否则开发成本较高，且容易影响核心商城业务推进。

但数据库和权限编码需要提前预留扩展能力，尤其是：

- 权限类型字段
- 风险等级字段
- 角色权限上限表
- 操作日志表
- 数据权限类型预留
- 高风险操作原因字段

这样可以保证当前系统足够简单可落地，后续又能平滑升级为企业级后台权限中心。
