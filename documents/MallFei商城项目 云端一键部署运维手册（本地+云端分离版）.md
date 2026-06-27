# MallFei商城项目 云端一键部署运维手册（本地\+云端分离版）

## 文档基础信息

- **服务器公网IP**：42\.194\.224\.180
- **服务器登录账号**：ubuntu
- **线上架构**：Nginx反向代理 \+ 全站HTTPS加密 \+ 80端口强制跳转443
- **运行环境**：Ubuntu \+ Docker MySQL \+ Redis \+ RabbitMQ \+ SpringBoot后端 \+ Vite双端前端
- **线上正式访问地址（已全部配置完成）**：
  
- 已配置 SSH 免密登录   -- 本地私钥：C:\Users\23235\.ssh\id_ed25519
  
- - 商城H5移动端首页：**https://mallfei.cloud**
  
- 后台管理系统：**https://mallfei.cloud/admin**
  
- 后端统一接口地址：**https://mallfei.cloud/api**
- **使用说明**：所有命令直接复制即可，严格拆分【本地电脑操作】、【云端服务器操作】两大板块，区分首次部署、日常迭代更新，无需修改任何本地/服务器路径

---

# 一、数据库部署 \& 更新（MySQL）

## 1\.1 本地操作（Windows PowerShell）

作用：将本地SQL脚本上传至服务器根目录，无需改动路径，直接复制执行

```powershell
# 上传数据库脚本到服务器 /home/ubuntu 目录
scp "C:\Users\23235\Desktop\cursorProjects\mallFei\backend\sql\new_mall_fei.sql" ubuntu@42.194.224.180:/home/ubuntu/
```

## 1\.2 云端服务器操作（SSH终端）

首次初始化数据库 / 后续更新数据库，命令完全通用，逐条复制执行

```bash
# 1. 创建商城数据库（不存在则新建，统一utf8mb4编码，适配emoji表情）
docker exec -i mall-mysql mysql -uroot -p'123456' -e "CREATE DATABASE IF NOT EXISTS new_mall_fei DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 导入SQL文件，同步表结构和业务测试数据
docker exec -i mall-mysql mysql -uroot -p'123456' mall_fei < /home/ubuntu/new_mall_fei.sql

# 3. 校验：查看数据表，确认导入成功无缺失
docker exec -i mall-mysql mysql -uroot -p'123456' -D new_mall_fei -e "SHOW TABLES;"
```

## 1\.3 数据库连接信息

- 连接地址：42\.194\.224\.180:3306

- 账号：root

- 密码：123456

- Docker容器名：mall\-mysql

---

# 二、后端SpringBoot部署 \& 更新

## 2\.1 本地操作（分IDEA终端 \+ PowerShell）

### 2\.1\.1 IDEA终端：项目打包（每次代码迭代必执行）

```bash
# 进入backend后端目录，打包指定启动模块，跳过单元测试加快打包速度
mvn -pl mall-start -am clean package -DskipTests
```

### 2\.1\.2 PowerShell：上传Jar包到服务器

```powershell
# 自动覆盖服务器旧jar包，无需手动删除旧服务文件
scp C:\Users\23235\Desktop\cursorProjects\mallFei\backend\mall-start\target\mall-start-0.0.1-SNAPSHOT.jar ubuntu@42.194.224.180:/home/ubuntu/
```

## 2\.2 云端服务器操作（SSH终端）

### 2\.2\.1 停止旧后端进程（一键终止，简洁高效）

```bash
pkill -9 -f "mall-start"  
ps -ef | grep mall-start
```

### 2\.2\.2 后台启动后端服务（固定云端配置，适配线上环境）

```bash
cd /home/ubuntu
nohup java -Xms512m -Xmx1024m -jar mall-start-0.0.1-SNAPSHOT.jar --spring.profiles.active=cloud > api.log 2>&1 &
```

### 2\.2\.3 后端启动校验 \& 日志排查

```bash
# 查看后端Java进程，存在对应进程即为启动成功
ps -ef | grep java

# 实时滚动查看后端运行日志，快速定位接口报错、连接中间件异常
tail -f api.log
```

## 2\.4 后端接口说明

- 线上正式接口地址（前端统一调用）：**https://mallfei\.cloud/api**

- 服务器本地端口：127\.0\.0\.1:9090（仅Nginx内部反向代理使用，外网不暴露端口）

- 接口返回说明：访问/api直接返回401未登录，属于后端Sa\-Token鉴权正常拦截逻辑，服务运行正常

---

# 三、前端H5移动端（mall\-h5）部署 \& 更新

## 3\.1 本地操作（Windows PowerShell）

## 方案 A：标准生产部署（推荐，线上正式使用）

### Windows 本地 PowerShell 操作（只打包 dist，上传静态产物）

```powershell
cd C:\Users\23235\Desktop\cursorProjects\mallFei\frontend\mall-h5
npm run build
```

执行完生成 `dist` 文件夹（纯静态资源，无源码、无依赖）

SCP 上传压缩包到服务器

```bash
scp h5-dist.zip ubuntu@42.194.224.180:/home/ubuntu/
```

### 云端 Ubuntu SSH 操作

```powershell
cd /home/ubuntu
# 解压到nginx静态目录，覆盖旧页面
unzip -o h5-dist.zip -d /home/ubuntu/nginx/mall-h5
# 无需启动node/vite，Nginx自动托管静态文件，不用杀前端进程
# 重载Nginx使新资源生效
sudo nginx -t
sudo nginx -s reload
```

## 3\.3 H5线上访问地址

商城移动端首页：**https://mallfei\.cloud**（全站HTTPS加密，浏览器安全小锁正常显示）

---

# 四、前端管理后台（mall\-admin）部署 \& 更新

## 1. 本地打包 dist（Windows 终端）

```powershell
# 进入后台前端目录
cd C:\Users\23235\Desktop\cursorProjects\mallFei\frontend\mall-admin
# 安装依赖（首次/更新依赖执行）
npm install
# 云端生产打包，对应 package.json "build:cloud" 脚本，自动配置 base=/admin/
npm run build:cloud
# 压缩产出的dist文件夹
if (Test-Path "admin-dist.zip") { Remove-Item "admin-dist.zip" -Force }
zip -r admin-dist.zip dist
```

### 2. 上传压缩包到 Ubuntu 服务器

```powershell
scp admin-dist.zip ubuntu@42.194.224.180:/home/ubuntu/
```

### 3. 服务器 SSH 终端操作（解压 + 部署）

本地开发使用根路径 `/`，登录页访问 `http://localhost:5174/login`。

```bash
# 进入用户根目录
cd /home/ubuntu
# 解压到nginx静态目录
unzip -o admin-dist.zip -d /home/ubuntu/nginx/mall-admin
# 校验nginx配置语法
sudo nginx -t
# 重载nginx，页面更新生效
sudo nginx -s reload
```

### 4\.2\.2 云端启动模式（必须使用 dev:cloud）

云端线上必须使用 `/admin/` 基础路径，因此必须用 `dev:cloud` 启动，不能直接使用默认 `npm run dev`。

- 本地模式：`base=/`，访问 `http://localhost:5174/login`
- 云端模式：`base=/admin/`，访问 `https://mallfei.cloud/admin/`

> 注意：Vite 不允许 mode 名称直接叫 `local`，项目脚本内部使用 `devlocal` 作为本地模式名；日常只需要执行 `npm run dev:local`，无需手动输入 `devlocal`。

## 4\.3 云端服务器操作（SSH终端）



### 3\.4 管理后台启动校验

```bash
# 查看5174端口是否由node/vite监听
ss -lntp | grep :5174

# 查看管理后台启动日志，正常应出现 /admin/ 访问路径
tail -n 30 /home/ubuntu/frontend/mall-admin/admin.log
```

## 4\.4 管理后台线上访问地址

运营管理控制台：**https://mallfei\.cloud/admin/**

---

# 五、前端一键运维快捷命令（日常高频复用）

```bash
# 1. 一键停止所有前端node/vite进程
pkill -f node

# 2. 一键重启H5商城 + 管理后台双前端服务
pkill -f node
cd /home/ubuntu/frontend/mall-admin && nohup npm run dev:cloud -- --host 0.0.0.0 --port 5174 > admin.log 2>&1 < /dev/null &
cd /home/ubuntu/frontend/mall-h5 && nohup npm run dev -- --host 0.0.0.0 --port 5173 > h5.log 2>&1 < /dev/null &

# 3. 排查前端端口占用问题
lsof -i :5173
lsof -i :5174
```

---

# 六、项目中间件统一访问地址

- RabbitMQ控制台：http://42\.194\.224\.180:15672  账号：guest / 密码：guest

- Redis服务：42\.194\.224\.180:6379  无密码

---

# 七、Nginx线上架构说明（面试重点）

1. **全站HTTPS配置**：已配置SSL证书，80端口全部强制301重定向至443，浏览器访问全程加密，地址栏展示安全小锁

2. **Nginx反向代理规则**：
            

    - 访问 / ：反向代理指向5173端口 H5商城前端

    - 访问 /admin 或 /admin/ ：反向代理指向5174端口 管理后台前端

    - 管理后台云端启动必须使用 `npm run dev:cloud`，确保 Vite 基础路径为 `/admin/`，否则登录后可能跳转到根路径并被H5接管

    - 访问 /api ：反向代理指向本地9090端口 SpringBoot后端服务

3. **架构优势**：外网无需暴露任何自定义端口，统一域名访问，符合企业线上生产环境部署规范

---

# 八、全局关键注意事项（面试答辩\+线上避坑）

1. **后端启动强制要求**：启动命令必须携带 `--spring.profiles.active=cloud`，否则无法读取云端配置，无法连接数据库、Redis、RabbitMQ中间件

2. **nohup作用**：后台守护进程，关闭服务器SSH连接窗口，前后端项目依然持续运行，线上服务不会中断

3. **日志排查规范**：后端报错看api\.log、H5前端报错看h5\.log、管理后台报错看admin\.log，线上问题优先查看日志定位原因

4. **域名与备案现状**：
            

    - ICP备案已完成，页脚已固定展示官方ICP备案号

    - 公安联网备案已提交完整资料，目前等待网安审核，页脚已添加合规占位文案，编号下发后直接替换官方图标与编号即可，短期无罚款、关停风险

5. **接口401状态说明**：直接访问https://mallfei\.cloud/api返回未登录401，是后端Sa\-Token权限拦截正常机制，并非服务故障，登录前端系统后可正常调用全部接口

6. **管理后台前端路径说明**：本地开发访问 `http://localhost:5174/login`，云端线上访问 `https://mallfei.cloud/admin/`。云端启动管理后台必须使用 `npm run dev:cloud -- --host 0.0.0.0 --port 5174`，不要使用默认 `npm run dev`，否则后台基础路径会变成 `/`，登录成功后可能进入H5页面。

7. **前端上传说明**：整包更新前端时优先使用 `frontend-deploy.zip` 压缩上传方案，排除 `node_modules`、`dist`、`.vite`，避免上传过慢和依赖权限异常。

> （注：文档部分内容可能由 AI 生成）



