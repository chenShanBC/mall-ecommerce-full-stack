# MallFei 后端测试操作手册

## 1. 文档目的

本文用于说明 MallFei 后端当前已经建设的测试体系，包括：

- 当前有哪些测试；
- 每类测试遵循什么原则；
- 测试用例是如何设计的；
- 开发人员如何在本地执行测试；
- 如何根据测试结果判断是否通过；
- 如何从业务结果上判断一个订单是否是“成功订单”。

当前后端测试重点覆盖核心电商链路中的领域规则、模块协作、HTTP API、MQ 行为和订单支付闭环，目标是保证核心业务逻辑在不依赖真实数据库、Redis、MQ、第三方支付平台的情况下可以被快速、稳定、重复验证。

---

## 2. 当前测试体系总览

后端测试代码主要位于各模块的 `src/test/java` 目录下，公共测试支撑模块位于：

```text
backend/mall-test-support
```

当前项目测试大致分为三类：

| 测试类型 | 命名特征 | 基类 | 主要目标 | 是否启动 Spring | 是否连接真实中间件 |
| --- | --- | --- | --- | --- | --- |
| 单元测试 | `*DomainServiceTest` | `BaseUnitTest` | 验证领域服务、领域模型、业务规则 | 否 | 否 |
| 模块测试 | `*ModuleStandardTest` | `BaseModuleTest` | 验证单模块内 Bean 装配、模块协作、关键闭环 | 可启动轻量 Spring TestContext | 否，外部依赖必须 Mock |
| 集成测试 | `*IntegrationTest` | `BaseIntegrationTest` | 验证 HTTP API、MQ 等链路集成行为 | 视测试目标而定 | 当前默认不连接真实中间件 |

> 说明：当前项目中的集成测试更偏向“轻量集成测试”，例如使用 `MockMvc` 验证 Controller 到应用服务的 HTTP 链路，或者用 Mock 方式验证 MQ 发布行为。当前默认不连接真实 MySQL、Redis、RabbitMQ 或第三方支付服务。

---

## 3. 当前已有测试清单

### 3.1 公共测试支撑模块

路径：`backend/mall-test-support`

| 文件 | 作用 |
| --- | --- |
| `BaseUnitTest.java` | 单元测试基类，约束纯领域逻辑不启动 Spring 容器，提供业务异常断言工具 |
| `BaseModuleTest.java` | 模块测试基类，说明模块测试允许 Spring TestContext，但必须 Mock 外部依赖 |
| `BaseIntegrationTest.java` | 集成测试基类，说明真实集成测试后续应使用 Testcontainers 或独立测试环境 |
| `MqVerifySupport.java` | MQ 副作用验证工具 |
| `TestDataFactory.java` | 跨模块通用测试数据工厂，统一用户、商品、订单、支付单等测试标识 |

### 3.2 单元测试

单元测试主要验证领域服务、领域模型和业务规则，通常不启动 Spring 容器，不访问真实外部资源。

| 模块 | 测试文件 | 主要验证内容 |
| --- | --- | --- |
| 认证模块 | `mall-auth/src/test/java/com/mallfei/auth/domain/service/AuthDomainServiceTest.java` | 登录、认证、鉴权相关领域规则 |
| 用户模块 | `mall-user/src/test/java/com/mallfei/user/domain/service/UserDomainServiceTest.java` | 用户注册、登录、状态、验证码等领域规则 |
| 商品模块 | `mall-product/src/test/java/com/mallfei/product/domain/service/ProductDomainServiceTest.java` | 商品、SKU、上下架、价格等领域规则 |
| 购物车模块 | `mall-cart/src/test/java/com/mallfei/cart/domain/service/CartDomainServiceTest.java` | 加购、改数量、选择商品、结算前校验等规则 |
| 订单模块 | `mall-order/src/test/java/com/mallfei/order/domain/service/OrderDomainServiceTest.java` | 订单创建、金额计算、状态流转、取消等规则 |
| 支付模块 | `mall-pay/src/test/java/com/mallfei/pay/domain/service/PayOrderDomainServiceTest.java` | 支付单创建、支付成功、关闭、金额一致性、幂等等规则 |
| 库存模块 | `mall-stock/src/test/java/com/mallfei/stock/domain/service/StockDomainServiceTest.java` | 库存扣减、释放、防超卖等规则 |
| 文件模块 | `mall-file/src/test/java/com/mallfei/file/domain/service/FileDomainServiceTest.java` | 文件上传、文件类型、大小、存储规则 |
| 售后模块 | `mall-aftersale/src/test/java/com/mallfei/aftersale/domain/service/AftersaleDomainServiceTest.java` | 退款、退货、售后单状态流转等规则 |

### 3.3 模块测试

模块测试用于验证某个业务模块内部的协作流程。它允许启动轻量 Spring 测试上下文，但数据库、Redis、MQ、第三方 SDK、对象存储等外部依赖必须 Mock。

| 模块 | 测试文件 | 主要验证内容 |
| --- | --- | --- |
| 购物车模块 | `mall-cart/src/test/java/com/mallfei/cart/standard/CartModuleStandardTest.java` | 购物车模块协作规则、加购、数量、结算前校验等 |
| 订单模块 | `mall-order/src/test/java/com/mallfei/order/standard/OrderModuleStandardTest.java` | 批量下单、金额计算、订单状态流转、超时取消、并发订单号生成 |
| 支付模块 | `mall-pay/src/test/java/com/mallfei/pay/standard/PayModuleStandardTest.java` | 支付成功回调、幂等回调、重复支付、金额不一致阻断、支付超时关闭 |
| 库存模块 | `mall-stock/src/test/java/com/mallfei/stock/standard/StockModuleStandardTest.java` | 库存预占、扣减、释放、防超卖、Lua 库存模拟等 |

### 3.4 集成测试

当前集成测试主要验证接口层、应用服务层、MQ 发送行为等链路集成，不默认连接真实中间件。

| 模块 | 测试文件 | 主要验证内容 |
| --- | --- | --- |
| 购物车模块 | `mall-cart/src/test/java/com/mallfei/cart/integration/CartApiIntegrationTest.java` | 使用 `MockMvc` 验证购物车 HTTP API 统一响应结构 |
| 订单模块 | `mall-order/src/test/java/com/mallfei/order/integration/OrderMqIntegrationTest.java` | 验证订单相关 MQ 集成行为或消息发送约束 |
| 支付模块 | `mall-pay/src/test/java/com/mallfei/pay/integration/PayMqIntegrationTest.java` | 验证支付相关 MQ 集成行为或消息发送约束 |

---

## 4. 测试设计原则

### 4.1 单元测试原则

单元测试遵循以下原则：

1. **不启动 Spring 容器**  
   单元测试只验证纯 Java 领域逻辑，避免上下文启动导致测试变慢、变脆弱。

2. **不连接真实外部资源**  
   不连接 MySQL、Redis、RabbitMQ、OSS、支付宝/微信支付 SDK 等真实资源。

3. **关注业务规则本身**  
   重点验证领域对象、领域服务的输入、输出、异常和状态变化。

4. **异常必须精确断言**  
   业务异常不仅要断言异常类型，还要断言错误码和核心错误文案。

5. **边界条件必须覆盖**  
   例如金额为 1 分、库存刚好为 0、重复支付、非法状态流转、过期时间边界等。

6. **测试数据固定可重复**  
   通过 `TestDataFactory` 或测试内构造固定数据，避免依赖当前时间、随机数或真实环境。

### 4.2 模块测试原则

模块测试遵循以下原则：

1. **验证模块内部协作**  
   关注模块内应用服务、领域服务、仓储接口、消息发布等之间的协作是否符合业务预期。

2. **允许轻量启动 Spring TestContext**  
   例如使用 `@SpringBootTest(classes = TestConfig.class)` 加载最小测试配置。

3. **外部依赖必须 Mock**  
   数据库、Redis、MQ、第三方 SDK、文件存储等外部依赖必须 Mock，不允许真实连接。

4. **覆盖关键业务闭环**  
   例如订单创建后的待支付状态、支付成功后的订单状态推进、库存释放判断等。

5. **验证副作用**  
   对 MQ 消息、第三方调用等副作用，需要使用 Mockito 或公共工具进行显式验证。

### 4.3 集成测试原则

当前集成测试遵循以下原则：

1. **优先使用轻量集成**  
   例如 `@WebMvcTest` + `MockMvc` 验证 Controller、JSON 序列化、统一响应结构。

2. **默认不连接真实环境**  
   当前项目默认不启动真实 MySQL、Redis、RabbitMQ。

3. **真实集成测试需要隔离环境**  
   后续如果要验证真实 HTTP + MySQL + Redis + MQ 的端到端链路，应优先使用 Testcontainers 或独立测试环境。

4. **禁止污染开发/生产数据**  
   不允许集成测试直接连接开发库、生产库或个人本地业务库执行写入操作。

---

## 5. 核心业务测试设计说明

### 5.1 购物车测试设计

购物车测试主要围绕用户从加购到结算前校验的流程设计：

- 加入购物车时，校验商品/SKU 是否有效；
- 重复加购时，验证数量合并逻辑；
- 修改数量时，验证数量上下限；
- 勾选/取消勾选商品时，验证选择状态；
- 结算前校验库存、价格、商品状态；
- HTTP 接口测试验证统一响应结构，例如：
  - `code = SUCCESS`；
  - `data.itemCount` 正确；
  - `data.totalQuantity` 正确。

### 5.2 订单测试设计

订单测试主要围绕下单、金额、状态流转和超时取消设计：

- 批量下单时，订单金额等于所有明细金额之和；
- 单价以“分”为单位，避免浮点精度问题；
- 订单初始状态为待支付；
- 状态只能按合法方向流转：
  - 待支付；
  - 已支付；
  - 处理中；
  - 已发货；
  - 已完成。
- 非法状态流转必须被阻断，例如待支付订单不能直接发货；
- 支付超时订单应自动取消，并触发后续释放库存判断；
- 并发批量下单时，订单号不能重复。

### 5.3 支付测试设计

支付测试主要围绕支付单、支付回调、幂等和金额一致性设计：

- 创建支付单时，支付金额必须与订单金额一致；
- 支付成功回调首次到达时，应同时推进支付单和订单状态；
- 同一个交易号重复回调时，不允许重复推进状态；
- 已关闭支付单不能再次回调成功；
- 支付金额不一致时必须阻断回调，避免错账；
- 待支付单超时后应关闭，后续不能再支付；
- 并发重复支付请求应复用同一个幂等键，只创建一笔支付单。

### 5.4 库存测试设计

库存测试主要围绕防超卖、预占、扣减和释放设计：

- 下单前库存必须足够；
- 库存不足时必须失败；
- 并发扣减不能导致库存变成负数；
- 订单取消或支付超时时，需要释放预占库存；
- 支付成功或订单确认后，库存扣减结果应稳定；
- 对 Lua 库存逻辑可使用模拟器测试核心原子规则。

### 5.5 MQ 测试设计

MQ 测试主要验证是否在正确场景发送或不发送消息：

- 订单创建后可触发延迟取消/超时检查消息；
- 支付成功后可触发订单状态推进消息；
- 取消订单后可触发库存释放消息；
- 业务失败或校验失败时不应发送 MQ；
- MQ 发送行为使用 Mock 验证，不连接真实 RabbitMQ。

---

## 6. 本地测试前置条件

执行测试前需要确认：

1. 已安装 JDK 21；
2. 已安装 Maven 3.8+；
3. 当前命令行目录位于项目后端目录：

```bash
cd backend
```

4. 本地无需启动 MySQL、Redis、RabbitMQ；
5. 本地无需启动前端项目；
6. 本地无需启动后端 `mall-start` 主应用。

---

## 7. 如何执行测试

以下命令均建议在 `backend` 目录下执行。

### 7.1 执行全部测试

```bash
mvn test
```

适用场景：

- 提交代码前完整回归；
- 修改公共模块后确认是否影响全局；
- 修改订单、支付、库存等核心链路后进行全量验证。

### 7.2 执行指定模块测试

例如只执行购物车模块测试：

```bash
mvn -pl mall-cart test
```

例如只执行订单模块测试：

```bash
mvn -pl mall-order test
```

例如只执行支付模块测试：

```bash
mvn -pl mall-pay test
```

如果模块依赖其他本地模块，建议加上 `-am` 自动构建依赖模块：

```bash
mvn -pl mall-order -am test
```

### 7.3 执行指定测试类

例如只执行订单模块标准测试：

```bash
mvn -pl mall-order -Dtest=OrderModuleStandardTest test
```

例如只执行支付模块标准测试：

```bash
mvn -pl mall-pay -Dtest=PayModuleStandardTest test
```

例如只执行购物车 HTTP API 集成测试：

```bash
mvn -pl mall-cart -Dtest=CartApiIntegrationTest test
```

### 7.4 执行指定测试方法

例如只执行订单状态流转测试：

```bash
mvn -pl mall-order -Dtest=OrderModuleStandardTest#shouldFlowOrderStatusForward test
```

例如只执行支付回调幂等测试：

```bash
mvn -pl mall-pay -Dtest=PayModuleStandardTest#shouldKeepCallbackIdempotent test
```

### 7.5 跳过测试进行编译打包

如果只是临时打包，不希望执行测试，可以使用：

```bash
mvn package -DskipTests
```

注意：提交代码前不建议跳过测试。核心业务改动提交前至少应执行相关模块测试。

---

## 8. 如何判断测试是否执行成功

### 8.1 命令行判断

测试成功时，Maven 控制台通常会出现类似结果：

```text
BUILD SUCCESS
```

并且每个测试类结果类似：

```text
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
```

判断标准：

- `Failures = 0`；
- `Errors = 0`；
- `BUILD SUCCESS`；
- 没有编译错误；
- 没有 Spring 测试上下文启动失败；
- 没有断言失败。

### 8.2 Surefire 报告判断

Maven 测试执行后，每个模块会生成测试报告：

```text
backend/模块名/target/surefire-reports/
```

例如购物车集成测试报告：

```text
backend/mall-cart/target/surefire-reports/com.mallfei.cart.integration.CartApiIntegrationTest.txt
```

成功报告示例：

```text
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

如果出现以下内容，则说明测试失败，需要修复：

- `Failures > 0`：断言失败，说明业务结果和预期不一致；
- `Errors > 0`：测试执行异常，可能是空指针、Bean 装配失败、上下文启动失败等；
- `BUILD FAILURE`：整体构建失败。

---

## 9. 如何判断“成功订单”

“成功订单”不能只看是否生成了订单号，而要同时满足订单、支付、库存和消息副作用等多个维度的条件。

### 9.1 测试层面的成功订单判断

在当前测试体系中，一个订单可以认为成功完成核心链路，通常需要满足以下条件：

1. **订单创建成功**
   - 订单号不为空；
   - 用户 ID 正确；
   - 订单明细不为空；
   - 商品数量正确；
   - 应付金额正确；
   - 初始状态为待支付。

2. **金额计算正确**
   - 订单总金额等于所有订单明细 `单价 × 数量` 之和；
   - 金额单位使用“分”；
   - 不出现浮点精度误差；
   - 支付金额与订单应付金额一致。

3. **库存处理正确**
   - 下单前库存足够；
   - 库存预占或扣减成功；
   - 并发场景下不超卖；
   - 失败、取消、超时时可以释放库存。

4. **支付成功**
   - 支付单从待支付变为支付成功；
   - 支付回调交易号存在；
   - 支付回调金额与订单金额一致；
   - 重复回调不会重复推进状态；
   - 已关闭支付单不能再次支付成功。

5. **订单状态推进正确**
   - 支付前订单为待支付；
   - 支付成功后订单变为已支付；
   - 后续可以继续流转为处理中、已发货、已完成；
   - 非法状态流转会被阻断。

6. **副作用符合预期**
   - 成功下单后应发送必要的延迟取消或后续处理消息；
   - 支付成功后应发送必要的订单推进消息；
   - 失败场景不应发送成功消息；
   - 取消或超时场景应触发库存释放逻辑。

### 9.2 数据库层面的成功订单判断

如果是在真实环境或联调环境中判断订单是否成功，建议至少检查以下表或数据对象：

1. **订单主表**
   - 订单号存在；
   - 用户 ID 正确；
   - 订单状态不是已取消；
   - 应付金额正确；
   - 支付金额正确；
   - 创建时间、支付时间等关键时间字段合理。

2. **订单明细表**
   - 明细数量正确；
   - SKU ID 正确；
   - 商品名称、规格快照正确；
   - 单价和小计金额正确。

3. **支付单表**
   - 支付单号存在；
   - 关联订单号正确；
   - 支付状态为成功；
   - 第三方交易号存在；
   - 支付金额等于订单应付金额。

4. **库存记录**
   - 对应 SKU 库存已扣减或预占；
   - 不存在库存负数；
   - 取消订单后库存已释放。

5. **MQ 或业务日志**
   - 有订单创建、支付成功、库存扣减等关键日志；
   - 没有重复支付、重复扣库存、金额不一致等错误日志。

### 9.3 接口层面的成功订单判断

如果通过接口测试判断成功订单，可以按以下顺序验证：

1. 调用下单接口成功返回；
2. 返回结构中 `code = SUCCESS`；
3. 响应数据中包含订单号；
4. 查询订单详情，订单状态为待支付；
5. 调用支付或模拟支付回调接口；
6. 查询订单详情，订单状态变为已支付；
7. 查询支付单，支付状态为成功；
8. 查询库存，库存扣减符合预期；
9. 重复调用支付回调，订单和支付单状态不重复变化。

---

## 10. 推荐的日常测试流程

### 10.1 修改单个领域规则时

例如只修改订单状态流转规则：

```bash
cd backend
mvn -pl mall-order -Dtest=OrderDomainServiceTest test
mvn -pl mall-order -Dtest=OrderModuleStandardTest test
```

### 10.2 修改支付回调逻辑时

```bash
cd backend
mvn -pl mall-pay -Dtest=PayOrderDomainServiceTest test
mvn -pl mall-pay -Dtest=PayModuleStandardTest test
mvn -pl mall-pay -Dtest=PayMqIntegrationTest test
```

### 10.3 修改下单链路时

建议至少执行：

```bash
cd backend
mvn -pl mall-cart -Dtest=CartModuleStandardTest test
mvn -pl mall-order -Dtest=OrderModuleStandardTest test
mvn -pl mall-stock -Dtest=StockModuleStandardTest test
mvn -pl mall-pay -Dtest=PayModuleStandardTest test
```

### 10.4 提交代码前

建议执行全量测试：

```bash
cd backend
mvn test
```

如果全量测试耗时较长，至少执行本次改动相关模块和核心链路模块测试。

---

## 11. 常见失败原因与排查方式

### 11.1 编译失败

表现：

```text
COMPILATION ERROR
```

常见原因：

- 方法签名变更后测试未同步修改；
- 构造函数参数变更；
- 模块依赖未声明；
- Java 版本不一致。

处理方式：

- 根据编译错误定位具体文件和行号；
- 同步调整测试数据构造方式；
- 确认使用 JDK 21。

### 11.2 断言失败

表现：

```text
Failures: 1
```

常见原因：

- 业务规则变更，但测试预期未更新；
- 实现逻辑有缺陷；
- 金额、状态、数量等结果和预期不一致。

处理方式：

- 优先判断是业务规则变了，还是代码实现错了；
- 如果业务规则没变，应修复生产代码；
- 如果业务规则确实调整，应同步修改测试用例和文档。

### 11.3 Spring 上下文启动失败

表现：

```text
Failed to load ApplicationContext
```

常见原因：

- 测试配置类缺少 Bean；
- 某个外部依赖没有 Mock；
- 自动配置扫描了不应该加载的真实组件；
- 模块依赖缺失。

处理方式：

- 检查 `@SpringBootTest(classes = TestConfig.class)` 或 `@WebMvcTest` 配置；
- 对外部依赖增加 `@MockBean`；
- 避免在模块测试中加载完整主应用。

### 11.4 外部资源连接失败

表现：

```text
Connection refused
```

常见原因：

- 测试误连接 MySQL、Redis、RabbitMQ；
- 配置文件被真实环境配置污染；
- 没有 Mock 外部依赖。

处理方式：

- 模块测试和当前轻量集成测试不应连接真实中间件；
- 检查是否误加载 `application-cloud.yml` 或真实环境配置；
- 使用 Mock 替代外部资源。

---

## 12. 后续测试增强建议

当前测试体系已经覆盖了核心领域规则和部分模块/接口链路。后续可以继续增强：

1. **引入 Testcontainers**
   - 使用临时 MySQL、Redis、RabbitMQ 容器；
   - 验证真实 SQL、缓存、MQ 链路；
   - 测试结束自动销毁环境。

2. **补充端到端下单链路测试**
   - 购物车；
   - 下单；
   - 库存扣减；
   - 支付回调；
   - 订单状态推进；
   - 售后退款。

3. **补充接口契约测试**
   - 固定接口入参和出参结构；
   - 防止前后端联调时字段变更未同步。

4. **补充并发压测自动化验证**
   - 防超卖；
   - 重复下单；
   - 重复支付；
   - MQ 重复消费幂等。

5. **接入 CI 流水线**
   - Pull Request 自动执行 `mvn test`；
   - 测试失败禁止合并；
   - 生成测试报告归档。

---

## 13. 总结

当前 MallFei 后端测试体系已经形成了基础分层：

- 单元测试负责验证纯领域规则；
- 模块测试负责验证单模块协作和关键闭环；
- 集成测试负责验证 HTTP API、MQ 等轻量链路；
- `mall-test-support` 负责统一测试规范、测试数据和公共断言能力。

判断测试成功时，以 `BUILD SUCCESS`、`Failures = 0`、`Errors = 0` 为准。判断订单成功时，不能只看订单号是否生成，而应同时验证订单金额、订单状态、支付状态、库存扣减、幂等行为和消息副作用是否全部符合预期。
