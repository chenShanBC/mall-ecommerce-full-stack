# 后端骨架说明

## 模块列表

- `mall-common`：通用响应、异常、公共能力
- `mall-auth`：认证与授权
- `mall-user`：用户与地址
- `mall-product`：商品与分类
- `mall-cart`：购物车
- `mall-order`：订单
- `mall-stock`：库存
- `mall-pay`：支付
- `mall-admin`：后台聚合接口
- `mall-start`：启动模块

## 当前状态

当前为最小骨架，已经具备：

- 父工程依赖管理
- Spring Boot 启动模块
- 基础配置文件
- 核心模块占位控制器
- 通用返回体与全局异常处理

## 下一步

- 补充实体、DTO、Mapper、Service
- 编写数据库初始化 SQL
- 接入 MyBatis-Plus、Redis、RabbitMQ 实际配置
- 完成认证与订单主链路
