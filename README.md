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

## 下一步建议

1. 完善数据库初始化 SQL
2. 完成认证模块与 Sa-Token 配置
3. 完成商品、购物车、订单主链路后端接口
4. 完成 H5 与后台首页、登录、商品等核心页面
5. 进行前后端联调
