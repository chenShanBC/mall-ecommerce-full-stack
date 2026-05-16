# mallFei 远程部署依赖与版本说明

本文档面向服务器 `ubuntu@43.139.236.10`，代码目录建议使用 `/home/ubuntu/mall-ecommerce-full-stack`，仓库分支为 `master`。

## 1. 基础环境版本

| 依赖 | 建议版本 | 说明 |
| --- | --- | --- |
| Ubuntu | 22.04 LTS 或 24.04 LTS | 当前服务器为 Ubuntu 即可，建议 LTS 版本 |
| Git | 2.34+ | 拉取 GitHub 仓库 |
| JDK | 21 | 当前后端 `backend/pom.xml` 配置 `java.version=21`，不建议只装 JDK 17 |
| Maven | 3.9.x | 构建 Spring Boot 多模块项目 |
| Node.js | 20 LTS | 构建 Vite 6 前端项目 |
| npm | 10.x | 随 Node.js 20 LTS 安装即可 |
| MySQL | 8.0.x | 后端使用 `mysql-connector-j` 和 MySQL JDBC URL |
| Redis | 7.x | Sa-Token / Spring Data Redis 使用 |
| RabbitMQ | 3.13+ 或 4.x | 后端已引入 `spring-boot-starter-amqp`，生产配置包含 RabbitMQ |
| Nginx | 1.24+ | 建议用于前端静态资源托管和 `/api` 反向代理 |

> 注意：服务器当前已安装 `openjdk-17-jdk`，但项目 Maven 配置使用 Java 21。如果不修改后端 `java.version`，远程部署前需要安装 JDK 21 并设置为默认 Java。

## 2. 项目内部关键依赖版本

后端父工程：`backend/pom.xml`

| 依赖 | 版本 |
| --- | --- |
| Java 编译版本 | 21 |
| Spring Boot | 3.3.5 |
| MyBatis-Plus | 3.5.7 |
| Sa-Token | 1.39.0 |
| SpringDoc OpenAPI | 2.6.0 |
| Maven Compiler Plugin | 3.13.0 |

前端 H5：`frontend/mall-h5/package.json`

| 依赖 | 版本范围 |
| --- | --- |
| Vue | ^3.5.13 |
| Vite | ^6.3.5 |
| Axios | ^1.9.0 |
| Pinia | ^3.0.2 |
| Vant | ^4.9.19 |
| Vue Router | ^4.5.1 |
| pinyin-pro | ^3.28.1 |

前端管理端：`frontend/mall-admin/package.json`

| 依赖 | 版本范围 |
| --- | --- |
| Vue | ^3.5.13 |
| Vite | ^6.3.5 |
| Axios | ^1.9.0 |
| Pinia | ^3.0.2 |
| Element Plus | ^2.9.8 |
| ECharts | ^6.0.0 |
| Vue Router | ^4.5.1 |
| unplugin-auto-import | ^21.0.0 |
| unplugin-vue-components | ^32.0.0 |

## 3. 生产环境变量建议

后端生产启动建议显式指定 `prod` profile：

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_HOST=127.0.0.1
export DB_PORT=3306
export DB_USERNAME=mall_fei
export DB_PASSWORD='请替换为强密码'
export REDIS_HOST=127.0.0.1
export REDIS_PORT=6379
export REDIS_DATABASE=0
export RABBITMQ_HOST=127.0.0.1
export RABBITMQ_PORT=5672
export RABBITMQ_USERNAME=mall_fei
export RABBITMQ_PASSWORD='请替换为强密码'
```

如启用支付宝沙箱/支付回调，再补充：

```bash
export MALL_PAY_ALIPAY_APP_ID='你的 app id'
export MALL_PAY_ALIPAY_PRIVATE_KEY='你的应用私钥'
export MALL_PAY_ALIPAY_PUBLIC_KEY='支付宝公钥'
export MALL_PAY_ALIPAY_NOTIFY_URL='http://43.139.236.10:9090/api/pay/callback/alipay'
export MALL_PAY_ALIPAY_RETURN_URL='http://43.139.236.10:9090/api/pay/alipay/return-bridge'
export MALL_PAY_ALIPAY_CLIENT_RETURN_URL='http://43.139.236.10/pay/return'
```

## 4. 数据库与中间件准备

### MySQL

建议创建独立数据库和用户：

```sql
CREATE DATABASE mall_fei DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'mall_fei'@'localhost' IDENTIFIED BY '请替换为强密码';
GRANT ALL PRIVILEGES ON mall_fei.* TO 'mall_fei'@'localhost';
FLUSH PRIVILEGES;
```

项目 SQL 文件位置：

```bash
backend/sql/mall_fei.sql
```

导入示例：

```bash
mysql -u mall_fei -p mall_fei < backend/sql/mall_fei.sql
```

### Redis

生产建议只监听本机 `127.0.0.1`，如开放公网必须配置密码和安全组限制。

### RabbitMQ

生产建议创建独立用户和 vhost，至少需要与 `application-prod.yml` 中的环境变量保持一致。

## 5. 构建命令

后端：

```bash
cd /home/ubuntu/mall-ecommerce-full-stack/backend
mvn clean package -DskipTests
```

后端启动产物通常位于：

```bash
backend/mall-start/target/mall-start-0.0.1-SNAPSHOT.jar
```

启动示例：

```bash
java -jar backend/mall-start/target/mall-start-0.0.1-SNAPSHOT.jar
```

H5 前端：

```bash
cd /home/ubuntu/mall-ecommerce-full-stack/frontend/mall-h5
npm ci
npm run build
```

管理端前端：

```bash
cd /home/ubuntu/mall-ecommerce-full-stack/frontend/mall-admin
npm ci
npm run build
```

## 6. 地址配置说明

当前已按远程部署思路调整：

- 后端 `spring.profiles.active` 支持通过 `SPRING_PROFILES_ACTIVE` 环境变量切换，默认仍为 `dev`。
- 后端支付宝回调默认地址已从 `localhost` 改为 `43.139.236.10`。
- H5 前端接口地址使用 `VITE_API_BASE_URL`，未设置时默认 `/`。
- 管理端接口地址已改为 `VITE_API_BASE_URL`，未设置时默认 `/`。
- 两个前端开发环境仍通过 Vite proxy 转发 `/api` 到本机后端 `http://localhost:9090`，仅用于本地开发。

生产部署推荐使用 Nginx：

- `/` 指向 H5 前端 `dist`
- `/admin/` 指向管理端前端 `dist`
- `/api/` 反向代理到 `http://127.0.0.1:9090/api/`
- 后端端口 `9090` 可只在服务器本机监听或通过安全组限制访问

## 7. 需要开放的端口

| 端口 | 用途 | 是否建议公网开放 |
| --- | --- | --- |
| 22 | SSH | 是，但建议限制来源 IP |
| 80 | HTTP | 是 |
| 443 | HTTPS | 是，配置域名后推荐 |
| 9090 | Spring Boot 后端 | 不建议，推荐 Nginx 代理 |
| 3306 | MySQL | 不建议 |
| 6379 | Redis | 不建议 |
| 5672 | RabbitMQ | 不建议 |
| 15672 | RabbitMQ 管理台 | 不建议，必要时限制来源 IP |

## 8. 远程部署前检查清单

1. `java -version` 确认为 JDK 21。
2. `mvn -version` 显示 Maven 使用的 Java 也是 JDK 21。
3. `node -v` 为 Node.js 20 LTS。
4. `npm -v` 为 npm 10.x。
5. MySQL 已创建 `mall_fei` 数据库并导入 `backend/sql/mall_fei.sql`。
6. Redis 已启动，后端能连接。
7. RabbitMQ 已启动，账号密码与环境变量一致。
8. 后端启动时设置 `SPRING_PROFILES_ACTIVE=prod`。
9. Nginx 已正确代理 `/api` 到 `127.0.0.1:9090`。
10. 如果没有域名，先使用 `http://43.139.236.10` 访问；后续建议绑定域名并配置 HTTPS。
