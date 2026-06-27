const fs = require('fs');

function esc(s) {
  return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}
function rect(x, y, w, h, cls, rx = 8) {
  return `<rect x="${x}" y="${y}" width="${w}" height="${h}" rx="${rx}" class="${cls}"/>`;
}
function text(x, y, content, cls, anchor = 'middle') {
  return `<text x="${x}" y="${y}" text-anchor="${anchor}" class="${cls}">${esc(content)}</text>`;
}
function tag(x, y, w, label, cls) {
  return `${rect(x, y, w, 34, cls, 5)}\n${text(x + w / 2, y + 23, label, 'tag-text')}`;
}
function requiredPanelHeight(groups) {
  const top = 54;
  const bottom = 24;
  const groupGap = 18;
  const titleH = 25;
  const tagH = 34;
  const tagGapY = 9;
  let h = top + bottom;
  groups.forEach((group, idx) => {
    const rows = Math.ceil(group.items.length / (group.cols || 2));
    h += titleH + rows * tagH + Math.max(0, rows - 1) * tagGapY;
    if (idx < groups.length - 1) h += groupGap;
  });
  return h;
}
function panel(x, y, w, title, groups, panelCls, tagCls) {
  const h = requiredPanelHeight(groups);
  let out = `${rect(x, y, w, h, panelCls, 8)}\n${text(x + w / 2, y + 34, title, 'module-title')}`;
  const padX = 20;
  let cursorY = y + 54;
  for (const group of groups) {
    out += `\n${text(x + padX, cursorY + 15, group.name, 'sub-title', 'start')}`;
    const cols = group.cols || 2;
    const gapX = 12;
    const gapY = 9;
    const tagW = Math.floor((w - padX * 2 - gapX * (cols - 1)) / cols);
    const tagStartY = cursorY + 25;
    group.items.forEach((name, idx) => {
      const col = idx % cols;
      const row = Math.floor(idx / cols);
      out += `\n${tag(x + padX + col * (tagW + gapX), tagStartY + row * (34 + gapY), tagW, name, tagCls)}`;
    });
    const rows = Math.ceil(group.items.length / cols);
    cursorY = tagStartY + rows * 34 + Math.max(0, rows - 1) * gapY + 18;
  }
  return { svg: out, h };
}
function side(x, y, h, label) {
  return `${rect(x, y, 140, h, 'side', 8)}\n${text(x + 70, y + h / 2 + 8, label, 'side-title')}`;
}

const cProduct = [{name:'商品浏览', items:['首页门户','商品分类','商品列表','商品详情'], cols:2},{name:'商品信息', items:['SPU/SKU','价格库存','推荐商品'], cols:3}];
const cUser = [{name:'认证登录', items:['用户注册','密码登录','短信登录','拼图验证码'], cols:2},{name:'账号资料', items:['支付宝登录','用户资料','修改密码','手机号绑定'], cols:2}];
const cCart = [{name:'购物车操作', items:['加购商品','列表查询','数量修改','勾选商品'], cols:2},{name:'结算准备', items:['删除清空','数量查询','结算预览','结算校验'], cols:2}];
const cTrade = [{name:'订单支付', items:['创建订单','订单查询','取消删除','确认收货','创建支付单','支付同步'], cols:3},{name:'售后退款', items:['订单退款','仅退款申请','售后查询'], cols:3}];
const cFile = [{name:'上传能力', items:['头像上传','商品图片'], cols:2},{name:'业务引用', items:['用户头像','商品图片'], cols:2}];

const aProduct = [{name:'类目管理', items:['类目列表','新增修改','状态删除'], cols:3},{name:'商品管理', items:['商品列表','商品详情','新增修改','上下架'], cols:2},{name:'商品运营', items:['销售阈值','销售分层','运营事件'], cols:3}];
const aUser = [{name:'用户管理', items:['用户列表','用户详情','启用禁用'], cols:3},{name:'账号权限', items:['账号管理','角色管理','权限目录','权限模板'], cols:2},{name:'审计日志', items:['操作日志','个人资料','修改密码'], cols:3}];
const aOrder = [{name:'订单查询', items:['订单列表','订单详情','SKU切换'], cols:3},{name:'订单处理', items:['取消订单','后台发货','完结订单','修改地址'], cols:2},{name:'异常处置', items:['订单异常','支付异常','关闭释放'], cols:3}];
const aStock = [{name:'库存管理', items:['库存列表','活跃库存','手工调整','策略调整'], cols:2},{name:'库存治理', items:['库存预警','库存日志','一致性校验'], cols:3},{name:'库存对账', items:['对账列表','差异详情','修复忽略'], cols:3}];
const aPay = [{name:'支付管理', items:['支付单','关闭支付','支付同步','支付补偿','回调记录'], cols:3},{name:'退款管理', items:['全局退款','订单退款','退款同步'], cols:3},{name:'售后管理', items:['售后列表','售后详情','售后审核'], cols:3}];
const aRecon = [{name:'基础对账', items:['对账概览','对账列表','单笔对账','异常处理'], cols:4},{name:'支付退款对账', items:['对账记录','记录详情','业务处置','完成忽略'], cols:4},{name:'线上人工对账', items:['对账任务','本地账单','渠道账单','自动勾兑'], cols:4},{name:'差异挂账', items:['差异处理','差异日志','挂账跟进','归档报表'], cols:4}];
const aDash = [{name:'经营视角', items:['后台看板','经营概览','销售表现'], cols:3},{name:'商品视角', items:['热销阈值','低销阈值','库存预警'], cols:3},{name:'财务视角', items:['累计净收入','资金趋势','对账风险'], cols:3},{name:'仓储视角', items:['库存健康','发货压力','活跃库存'], cols:3}];
const aAuthFile = [{name:'认证聚合', items:['登录上下文','主体信息','聚合退出','黑名单校验'], cols:2},{name:'文件服务', items:['头像上传','商品图片'], cols:2},{name:'登录增强', items:['短信验证码','拼图验证码','支付宝登录'], cols:3}];
const aGuard = [{name:'库存保障', items:['预占库存','确认库存','释放库存','回补扣减'], cols:2},{name:'支付保障', items:['渠道回调','支付宝回调','状态同步','支付补偿'], cols:2},{name:'订单保障', items:['超时取消','自动收货','异常修复'], cols:3}];

const cY = 165;
const aY1 = 540;
const aY2 = 930;
const cPanels = [
  panel(235, cY, 350, '商品模块', cProduct, 'front-panel', 'tag-blue'),
  panel(605, cY, 350, '用户模块', cUser, 'front-panel', 'tag-blue'),
  panel(975, cY, 350, '购物车模块', cCart, 'front-panel', 'tag-blue'),
  panel(1345, cY, 390, '交易模块', cTrade, 'front-panel', 'tag-blue'),
  panel(1755, cY, 375, '文件模块', cFile, 'front-panel', 'tag-blue')
];
const aPanels1 = [
  panel(235, aY1, 350, '商品模块', aProduct, 'admin-panel', 'tag-green'),
  panel(605, aY1, 350, '用户权限模块', aUser, 'admin-panel', 'tag-green'),
  panel(975, aY1, 350, '订单模块', aOrder, 'admin-panel', 'tag-green'),
  panel(1345, aY1, 350, '库存模块', aStock, 'admin-panel', 'tag-green'),
  panel(1715, aY1, 415, '支付售后模块', aPay, 'admin-panel', 'tag-green')
];
const aPanels2 = [
  panel(235, aY2, 540, '对账模块', aRecon, 'admin-panel', 'tag-green'),
  panel(805, aY2, 440, '看板模块', aDash, 'admin-panel', 'tag-green'),
  panel(1275, aY2, 415, '认证文件模块', aAuthFile, 'admin-panel', 'tag-green'),
  panel(1720, aY2, 410, '交易保障模块', aGuard, 'admin-panel', 'tag-green')
];
const cH = Math.max(...cPanels.map(p => p.h));
const adminBottom = Math.max(...aPanels2.map(p => aY2 + p.h));
const adminH = adminBottom - aY1;
const canvasH = adminBottom + 80;

const svg = `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="2200" height="${canvasH}" viewBox="0 0 2200 ${canvasH}">
  <defs>
    <style><![CDATA[
      .title { font: 700 36px 'Microsoft YaHei','SimHei',Arial,sans-serif; fill:#222; }
      .module-title { font: 700 22px 'Microsoft YaHei','SimHei',Arial,sans-serif; fill:#222; }
      .sub-title { font: 700 15px 'Microsoft YaHei','SimHei',Arial,sans-serif; fill:#2b3a42; }
      .tag-text { font: 14px 'Microsoft YaHei','SimHei',Arial,sans-serif; fill:#203040; }
      .side-title { font: 700 26px 'Microsoft YaHei','SimHei',Arial,sans-serif; fill:#17351a; }
      .main { fill:#fffdf7; stroke:#202020; stroke-width:3; }
      .top { fill:#f8c99b; stroke:#202020; stroke-width:3; }
      .side { fill:#91f692; stroke:#202020; stroke-width:3; }
      .front-panel { fill:#fff8bd; stroke:#202020; stroke-width:3; }
      .admin-panel { fill:#cdfbd0; stroke:#202020; stroke-width:3; }
      .tag-blue { fill:#a9d5f4; stroke:#1f2c33; stroke-width:2; }
      .tag-green { fill:#8ff1d0; stroke:#1f2c33; stroke-width:2; }
    ]]></style>
  </defs>

  ${rect(20, 20, 2160, canvasH - 40, 'main', 4)}
  ${rect(70, 58, 2060, 70, 'top', 6)}
  ${text(1100, 103, 'MallFei 项目业务架构图', 'title')}
  ${side(70, cY, cH, 'C端商城')}
  ${cPanels.map(p => p.svg).join('\n')}
  ${side(70, aY1, adminH, '后台管理')}
  ${aPanels1.map(p => p.svg).join('\n')}
  ${aPanels2.map(p => p.svg).join('\n')}
</svg>
`;

fs.writeFileSync('documents/MallFei-business-architecture-cn.svg', svg, 'utf8');
console.log('documents/MallFei-business-architecture-cn.svg');
