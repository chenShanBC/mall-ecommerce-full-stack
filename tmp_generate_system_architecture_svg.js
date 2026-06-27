const fs = require('fs');

function esc(s){return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')}
function rect(x,y,w,h,cls,rx=8){return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${rx}" class="${cls}"/>`}
function text(x,y,s,cls,anchor='middle'){return `<text x="${x}" y="${y}" text-anchor="${anchor}" class="${cls}">${esc(s)}</text>`}
function line(x1,y1,x2,y2,label=''){const mx=(x1+x2)/2,my=(y1+y2)/2-8;return `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" class="arrow"/>${label?text(mx,my,label,'line-label'):''}`}
function box(x,y,w,h,title,items,cls='box'){let out=rect(x,y,w,h,cls)+`\n${text(x+w/2,y+32,title,'box-title')}`;items.forEach((it,i)=>out+=`\n${text(x+w/2,y+62+i*24,it,'box-text')}`);return out}
function tag(x,y,w,s){return rect(x,y,w,32,'tag',5)+`\n${text(x+w/2,y+22,s,'tag-text')}`}
function moduleGrid(x,y){const mods=['mall-start 启动入口','mall-admin 后台聚合','mall-user 用户地址','mall-auth 认证上下文','mall-product 商品类目','mall-cart 购物车','mall-order 订单','mall-stock 库存','mall-pay 支付退款','mall-aftersale 售后','mall-file 文件服务','mall-common 通用能力'];let out='';const w=190,gx=18,gy=12;mods.forEach((m,i)=>{const c=i%4,r=Math.floor(i/4);out+=`\n${tag(x+c*(w+gx),y+r*(32+gy),w,m)}`});return out}

const svg=`<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="2100" height="1280" viewBox="0 0 2100 1280">
<defs>
<marker id="arrow" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto" markerUnits="strokeWidth"><path d="M0,0 L0,6 L9,3 z" fill="#333"/></marker>
<style><![CDATA[
.title{font:700 38px 'Microsoft YaHei','SimHei',Arial,sans-serif;fill:#222}.sub{font:17px 'Microsoft YaHei','SimHei',Arial,sans-serif;fill:#555}.layer-title{font:700 23px 'Microsoft YaHei','SimHei',Arial,sans-serif;fill:#0b5c75}.box-title{font:700 21px 'Microsoft YaHei','SimHei',Arial,sans-serif;fill:#222}.box-text{font:16px 'Microsoft YaHei','SimHei',Arial,sans-serif;fill:#333}.tag-text{font:14px 'Microsoft YaHei','SimHei',Arial,sans-serif;fill:#203040}.line-label{font:15px 'Microsoft YaHei','SimHei',Arial,sans-serif;fill:#555}.main{fill:#fff;stroke:#202020;stroke-width:3}.titlebar{fill:#f8c99b;stroke:#202020;stroke-width:3}.layer{fill:none;stroke:#3aa4b5;stroke-width:2.5;stroke-dasharray:10 8}.box{fill:#f7fbff;stroke:#2f8fa3;stroke-width:2.4}.client{fill:#f8fbff;stroke:#2f8fa3;stroke-width:2.4}.nginx{fill:#f3fff3;stroke:#2f8fa3;stroke-width:2.4}.app{fill:#fffdf1;stroke:#2f8fa3;stroke-width:2.4}.docker{fill:#fff7f7;stroke:#2f8fa3;stroke-width:2.4}.infra{fill:#f4f0ff;stroke:#2f8fa3;stroke-width:2.4}.tag{fill:#bde7f5;stroke:#1f2c33;stroke-width:1.7}.arrow{stroke:#333;stroke-width:2.2;marker-end:url(#arrow);fill:none}.plain{stroke:#333;stroke-width:2.2;fill:none}
]]></style>
</defs>

${rect(20,20,2060,1240,'main',4)}
${rect(70,55,1960,72,'titlebar',6)}
${text(1050,101,'MallFei 项目系统架构图','title')}
${text(1050,126,'单体多模块 Spring Boot + Vue 前端 + Nginx 反向代理 + Docker 容器化 + MySQL / Redis / RabbitMQ','sub')}

${rect(80,170,1940,155,'layer',4)}
${text(1050,203,'访问与接入层','layer-title')}
${box(130,230,210,70,'PC浏览器',['Admin后台入口'],'client')}
${box(390,230,210,70,'移动端/H5',['C端商城入口'],'client')}
${box(760,220,300,90,'Nginx 接入网关',['/admin -> admin-web','/ -> h5-web','/api -> backend:9090'],'nginx')}
${box(1460,220,250,90,'Swagger/OpenAPI',['接口文档','联调入口'],'box')}
${box(1740,220,230,90,'支付宝能力',['授权登录','支付回调'],'box')}
${line(600,265,760,265,'HTTP')}
${line(1060,265,1460,265,'接口访问')}
${line(1060,285,1740,285,'第三方回调')}

${rect(80,370,1940,260,'layer',4)}
${text(1050,403,'Docker 应用容器层','layer-title')}
${box(170,455,280,105,'admin-web 容器',['nginx:1.27-alpine','mall-admin dist','后台静态资源'],'docker')}
${box(500,455,280,105,'h5-web 容器',['nginx:1.27-alpine','mall-h5 dist','前台静态资源'],'docker')}
${box(850,445,340,125,'backend 容器',['eclipse-temurin:17-jre','app.jar','服务端口：9090'],'docker')}
${box(1260,455,280,105,'基础设施容器',['MySQL / Redis','RabbitMQ'],'docker')}
${box(1590,455,280,105,'Docker 网络',['mall-infra-net','容器间互通'],'docker')}
${line(910,310,310,455,'静态资源')}
${line(910,310,640,455,'静态资源')}
${line(910,310,1020,445,'API反代')}

${rect(80,680,1940,250,'layer',4)}
${text(1050,713,'后端应用层：Spring Boot 单体多模块','layer-title')}
${box(160,760,320,95,'mall-start 启动应用',['Spring Boot 入口','聚合各业务模块'],'app')}
${rect(540,740,980,150,'app',8)}
${text(1030,772,'后端 Maven 多模块','box-title')}
${moduleGrid(610,805)}
${box(1580,760,340,95,'运行形态',['非微服务架构','单 JVM / 单应用进程','模块内协作调用'],'app')}
${line(1020,570,1020,680,'HTTP / Controller')}
${line(480,808,540,808,'模块聚合')}

${rect(80,980,1940,185,'layer',4)}
${text(1050,1013,'数据与中间件层','layer-title')}
${box(170,1060,260,75,'MySQL 8.0',['业务数据 / /data/mall/mysql'],'infra')}
${box(500,1060,260,75,'Redis 7',['登录态 / 缓存 / 库存原子能力'],'infra')}
${box(830,1060,260,75,'RabbitMQ 3',['订单超时 / 支付 / 库存事件'],'infra')}
${box(1160,1060,260,75,'文件存储',['头像 / 商品图片'],'infra')}
${box(1490,1060,350,75,'测试与运维支撑',['PowerShell冒烟脚本 / Maven JUnit测试'],'infra')}
${line(1020,930,300,1060,'JDBC')}
${line(1035,930,630,1060,'Redis')}
${line(1050,930,960,1060,'AMQP')}
${line(1065,930,1290,1060,'文件上传')}
</svg>`;
fs.writeFileSync('documents/MallFei-system-architecture.svg',svg,'utf8');
console.log('documents/MallFei-system-architecture.svg');
