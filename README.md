# mallFei

基于三份需求文档搭建的 B2C 电商 MVP 项目。

## 目录结构

- `backend/`：Spring Boot 3 + Maven 多模块后端
- `frontend/mall-h5/`：Vue 3 + Vite + Vant 用户端
- `frontend/mall-admin/`：Vue 3 + Vite + Element Plus 后台管理端
- `documents/`：原始需求文档
- `cursor完整设计方案.md`：整体设计方案

## 当前阶段

当前已完成项目骨架初始化，包括：

- 后端多模块 Maven 父工程
- 通用响应体与异常处理基础类
- 认证、商品、购物车、订单、支付、后台模块占位控制器
- 启动模块与基础配置
- 前端 H5 与后台 Vite 工程骨架
- 数据库初始化脚本占位文件

## 云端容器化运行（Docker Compose）

已新增云端运行配置文件：

- `docker-compose.cloud.yml`：一键启动 MySQL、Redis、RabbitMQ、后端、H5、Admin、Nginx 网关
- `backend/Dockerfile`：构建并运行 Spring Boot 后端（`cloud` 配置）
- `frontend/mall-h5/Dockerfile`：构建并运行 H5 静态站点
- `frontend/mall-admin/Dockerfile`：构建并运行后台静态站点
- `deploy/nginx.conf`：统一入口与反向代理（`/api` -> backend）

### 启动方式

在项目根目录执行：

```bash
docker compose -f docker-compose.cloud.yml up -d --build
```

### 访问地址

- H5：`http://<服务器IP>/`
- Admin：`http://<服务器IP>/admin/`
- 后端 API：`http://<服务器IP>/api/`
- RabbitMQ 管理台：`http://<服务器IP>:15672`（账号 `admin` / `admin123456`）

### 停止与清理

```bash
docker compose -f docker-compose.cloud.yml down
```

如需连同数据卷一起清理：

```bash
docker compose -f docker-compose.cloud.yml down -v
```
