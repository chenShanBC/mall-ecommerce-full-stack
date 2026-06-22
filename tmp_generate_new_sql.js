const fs = require('fs');
const path = require('path');

const src = path.join(__dirname, 'backend', 'sql', 'mall_fei.sql');
const out = path.join(__dirname, 'backend', 'sql', 'new_mall_fei.sql');
let text = fs.readFileSync(src, 'utf8').replace(/\r\n/g, '\n');
const now = '2026-06-10 10:00:00';

function val(x) {
  if (x === null || x === undefined) return 'NULL';
  if (typeof x === 'number') return String(x);
  if (typeof x === 'boolean') return x ? '1' : '0';
  return `'${String(x).replace(/\\/g, '\\\\').replace(/'/g, "\\'")}'`;
}
function ins(table, rows) {
  return rows.map(row => `INSERT INTO \`${table}\` VALUES (${row.map(val).join(', ')});`).join('\n') + (rows.length ? '\n' : '');
}
function escapeRegex(s) {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
function replaceRecords(table, rows) {
  const marker = `-- Records of ${table}\n-- ----------------------------\n`;
  const start = text.indexOf(marker);
  if (start < 0) throw new Error(`Missing records marker: ${table}`);
  const dataStart = start + marker.length;
  const next = text.indexOf('\n-- ----------------------------\n-- Table structure for ', dataStart);
  const dataEnd = next < 0 ? text.indexOf('\nSET FOREIGN_KEY_CHECKS', dataStart) : next;
  if (dataEnd < 0) throw new Error(`Cannot find records end: ${table}`);
  text = text.slice(0, dataStart) + ins(table, rows) + text.slice(dataEnd);
}
function resetAuto(table, value) {
  const re = new RegExp('(CREATE TABLE `' + escapeRegex(table) + '`[\\s\\S]*?\\) ENGINE = InnoDB AUTO_INCREMENT =) \\d+');
  text = text.replace(re, `$1 ${value}`);
}

const allPerms = '["dashboard:view", "admin:view", "admin:create", "admin:update", "admin:disable", "role:view", "role:manage", "permission:view", "permission:assign", "log:operation:view", "user:view", "user:edit", "user:disable", "product:view", "product:create", "product:update", "product:on_sale", "product:off_sale", "category:manage", "stock:view", "stock:log:view", "stock:adjust", "stock:reconcile:view", "stock:reconcile:check", "stock:reconcile:repair", "order:view", "order:remark", "order:ship", "order:close", "order:log:view", "aftersale:view", "aftersale:audit", "refund:view", "refund:execute", "finance:view", "payment:view", "reconciliation:view", "reconciliation:handle"]';
const admins = [
  [1, null, 'admin', '123456', '超级管理员', 'SUPER_ADMIN', allPerms, 'ENABLED', 0, '2026-01-01 09:00:00', now, null],
  [2, null, 'finance', '123456', '财务运营', 'FINANCE_OPERATOR', '["dashboard:view", "finance:view", "payment:view", "refund:view", "refund:execute", "reconciliation:view", "reconciliation:handle", "stock:reconcile:view", "order:view", "aftersale:view"]', 'ENABLED', 0, '2026-01-01 09:00:00', now, null],
  [3, null, 'product', '123456', '商品运营', 'PRODUCT_OPERATOR', '["dashboard:view", "product:view", "product:create", "product:update", "product:on_sale", "product:off_sale", "category:manage", "stock:view", "stock:log:view"]', 'ENABLED', 0, '2026-01-01 09:00:00', now, null],
  [4, null, 'stock', '123456', '库存运营', 'STOCK_OPERATOR', '["dashboard:view", "stock:view", "stock:log:view", "stock:adjust", "stock:reconcile:view", "stock:reconcile:check", "stock:reconcile:repair", "product:view"]', 'ENABLED', 0, '2026-01-01 09:00:00', now, null],
  [5, null, 'order', '123456', '订单运营', 'ORDER_OPERATOR', '["dashboard:view", "order:view", "order:remark", "order:ship", "order:close", "order:log:view", "aftersale:view", "aftersale:audit", "refund:view"]', 'ENABLED', 0, '2026-01-01 09:00:00', now, null],
  [6, null, 'service', '123456', '客服', 'CUSTOMER_SERVICE', '["dashboard:view", "user:view", "order:view", "order:remark", "order:log:view", "aftersale:view"]', 'ENABLED', 0, '2026-01-01 09:00:00', now, null],
  [7, null, 'audit', '123456', '审计员', 'AUDIT_OPERATOR', '["dashboard:view", "user:view", "product:view", "stock:view", "stock:log:view", "stock:reconcile:view", "stock:reconcile:check", "order:view", "order:log:view", "aftersale:view", "refund:view", "finance:view", "reconciliation:view", "admin:view", "role:view", "permission:view", "log:operation:view"]', 'ENABLED', 0, '2026-01-01 09:00:00', now, null]
];

const productNames = [
  ['星麦保温杯', 5, 10000], ['鹿岛空气炸锅', 2, 12000], ['青橙护眼台灯', 1, 13000], ['北庭跑步鞋', 5, 14000],
  ['云岚双肩包', 10, 15000], ['山也电动牙刷', 3, 16000], ['沐白乳胶枕', 9, 17000], ['岚森牛奶礼盒', 4, 18000],
  ['初合机械键盘', 1, 19000], ['鲸选防晒霜', 3, 20000], ['有栖行李箱', 10, 21000], ['松禾无线耳机', 8, 22000]
];
const spus = productNames.map((p, idx) => {
  const id = idx + 1;
  return [id, p[0], p[1], `/uploads/product/demo/spu-${String(id).padStart(2, '0')}.png`, `["]`, `演示商品：${p[0]}，用于商城浏览、下单、支付、售后、库存、对账测试。`, 'ONLINE', 0, '2026-01-01 09:00:00', now, null];
});
spus.forEach(row => row[4] = `["/uploads/product/demo/spu-${String(row[0]).padStart(2, '0')}-1.png", "/uploads/product/demo/spu-${String(row[0]).padStart(2, '0')}-2.png"]`);
const skus = productNames.map((p, idx) => {
  const id = idx + 1;
  return [id, id, `SKU-DEMO-${String(id).padStart(4, '0')}`, `${p[0]} 默认规格`, '{"规格":"默认规格","颜色":"标准色"}', p[2], p[2] + 3000, id <= 10 ? id : 0, 'ONLINE', 0, '2026-01-01 09:00:00', now, null];
});
const stocks = skus.map((sku, idx) => {
  const id = idx + 1;
  const total = id === 2 ? 122 : 120 + id;
  const locked = id === 2 ? 0 : 0;
  return [id, id, total, locked, total - locked, 'ACTIVE', 10, 1000, id === 1 ? 'HIGH' : 'NORMAL', 0, '2026-01-01 09:00:00', now];
});
const productSalesDailyStats = [
  [1, '2026-06-10', 1, 1, 'NORMAL', 5, 50000, now, now],
  [2, '2026-06-10', 8, 8, 'NORMAL', 1, 18000, now, now],
  [3, '2026-06-10', 10, 10, 'NORMAL', 1, 20000, now, now]
];
const productSalesEvents = [
  [1, 'ORDER_COMPLETED:RCT202606100010', 'ORDER_COMPLETED', 'RCT202606100010', now]
];

const userProfiles = [
  ['13800000000', '林亦辰', '林先生', '13910002001', '110000', '北京市', '110100', '北京市', '110105', '朝阳区', '望京街道阜通东大街方恒国际中心A座1206室'],
  ['13800000001', '周雨桐', '周女士', '13910002002', '110000', '北京市', '110100', '北京市', '110108', '海淀区', '中关村南大街甲18号院3号楼2单元501室'],
  ['13800000002', '陈嘉明', '陈先生', '13910002003', '310000', '上海市', '310100', '上海市', '310115', '浦东新区', '张江高科碧波路690号1号楼803室'],
  ['13800000003', '许念安', '许女士', '13910002004', '440000', '广东省', '440300', '深圳市', '440305', '南山区', '粤海街道科技园南区深南花园6栋1802室'],
  ['13800000004', '赵一诺', '赵女士', '13910002005', '330000', '浙江省', '330100', '杭州市', '330106', '西湖区', '文三路478号华星时代广场B座1509室'],
  ['13800000005', '王景行', '王先生', '13910002006', '320000', '江苏省', '320500', '苏州市', '320508', '姑苏区', '干将西路515号佳福国际大厦908室'],
  ['13800000006', '李沐阳', '李先生', '13910002007', '510000', '四川省', '510100', '成都市', '510107', '武侯区', '天府大道北段1700号环球中心N2区1215室'],
  ['13800000007', '孙若溪', '孙女士', '13910002008', '420000', '湖北省', '420100', '武汉市', '420106', '武昌区', '中北路汉街总部国际E座2103室'],
  ['13800000008', '吴泽宇', '吴先生', '13910002009', '500000', '重庆市', '500100', '重庆市', '500103', '渝中区', '解放碑民族路188号环球金融中心32层3208室'],
  ['13800000009', '郑书瑶', '郑女士', '13910002010', '350000', '福建省', '350200', '厦门市', '350203', '思明区', '湖滨南路90号立信广场10楼1006室'],
  ['13800000010', '何星河', '何先生', '13910002011', '370000', '山东省', '370200', '青岛市', '370202', '市南区', '香港中路61号远洋大厦A座1701室'],
  ['13800000011', '高芷晴', '高女士', '13910002012', '120000', '天津市', '120100', '天津市', '120101', '和平区', '南京路189号津汇广场2座2205室'],
  ['13800000012', '马承宇', '马先生', '13910002013', '610000', '陕西省', '610100', '西安市', '610103', '碑林区', '长安北路14号省体育场东门写字楼806室'],
  ['13800000013', '胡安琪', '胡女士', '13910002014', '430000', '湖南省', '430100', '长沙市', '430102', '芙蓉区', '五一大道766号中天广场18楼1809室'],
  ['13800000014', '郭子墨', '郭先生', '13910002015', '210000', '辽宁省', '210100', '沈阳市', '210102', '和平区', '青年大街386号华阳国际大厦1512室'],
  ['13800000015', '罗清越', '罗女士', '13910002016', '410000', '河南省', '410100', '郑州市', '410105', '金水区', '农业路东16号省汇中心B座1107室'],
  ['13800000016', '梁知夏', '梁女士', '13910002017', '340000', '安徽省', '340100', '合肥市', '340104', '蜀山区', '潜山路190号华邦ICC写字楼A座1306室'],
  ['13800000017', '宋以航', '宋先生', '13910002018', '230000', '黑龙江省', '230100', '哈尔滨市', '230102', '道里区', '群力第五大道金中环商业广场C座915室'],
  ['13800000018', '唐诗涵', '唐女士', '13910002019', '530000', '云南省', '530100', '昆明市', '530102', '五华区', '青年路389号志远大厦12楼1202室'],
  ['13800000019', '程远舟', '程先生', '13910002020', '450000', '广西壮族自治区', '450100', '南宁市', '450103', '青秀区', '民族大道136号华润大厦B座2006室']
];
const users = userProfiles.map((p, idx) => [idx + 1, p[0], '123456', p[1], '', 'ENABLED', 0, '2026-01-01 09:00:00', now, null]);
const addresses = userProfiles.map((p, idx) => [idx + 1, idx + 1, p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10], '', 1, 0, '2026-01-01 09:00:00', now, null]);
const primaryAddress = addresses[0];
const thirdBinds = [];
const adminLogs = [
  [1, 1, 'admin', 'SYSTEM', 'DATA_RESET', '清洗演示数据：管理员/用户/商品/SPU/SKU/库存/订单支付售后对账数据配套重建', 'SUCCESS', now],
  [2, 2, 'finance', 'PAY', 'RECONCILIATION_PREPARE', '生成支付/退款/对账闭环测试数据', 'SUCCESS', now]
];

const scenarios = [
  ['正常支付成功', 'PAID', 10000, 'CASE01 正常支付成功：订单已支付/支付单SUCCESS/金额一致/渠道成功', '2026-06-10 09:01:00'],
  ['待支付', 'PENDING_PAYMENT', 12000, 'CASE02 待支付：支付单PENDING，渠道未支付，不应判长短款', null],
  ['支付成功订单未同步', 'PENDING_PAYMENT', 13000, 'CASE03 支付单SUCCESS但订单仍待支付：补偿订单已支付', null],
  ['渠道成功本地未成功', 'PENDING_PAYMENT', 14000, 'CASE04 本地PAYING但渠道SUCCESS：同步支付状态+补偿订单', null],
  ['平台成功渠道缺失', 'PAID', 15000, 'CASE05 平台成功但渠道未确认：长款/待渠道调单/恢复待支付/关闭释放/补款', '2026-06-10 09:05:00'],
  ['已支付缺支付单', 'PAID', 16000, 'CASE06 订单已支付但缺少支付单：人工审核/恢复待支付/关闭释放库存', '2026-06-10 09:06:00'],
  ['金额不一致', 'PAID', 17000, 'CASE07 金额不一致：待退款/补款/人工审核', '2026-06-10 09:07:00'],
  ['退款对账', 'REFUND_PENDING', 18000, 'CASE08 有退款单：退款同步/退款对账/退款状态金额不一致', '2026-06-10 09:08:00'],
  ['支付异常处置', 'PAYMENT_EXCEPTION', 19000, 'CASE09 支付异常处理：关闭释放库存/恢复待支付/人工确认已支付', null],
  ['已发货对平', 'SHIPPED', 20000, 'CASE10 已发货且支付对平：重新对账仍一致', '2026-06-10 09:10:00']
];
const orders = scenarios.map((s, idx) => {
  const i = idx + 1;
  const receiverName = primaryAddress[2];
  const receiverPhone = primaryAddress[3];
  const provinceName = primaryAddress[5];
  const cityName = primaryAddress[7];
  const districtName = primaryAddress[9];
  const detailAddress = primaryAddress[10];
  return [i, `RCT20260610${String(i).padStart(4, '0')}`, 1, s[1], s[2], s[2], 0, 0, receiverName, receiverPhone, provinceName, cityName, districtName, detailAddress, s[3], 'MOCK', s[4], null, i === 10 ? '2026-06-10 09:30:00' : null, null, '2026-06-10 12:00:00', 0, null, 0, now, now, null];
});
const orderItems = scenarios.map((s, idx) => {
  const i = idx + 1;
  return [i, i, `RCT20260610${String(i).padStart(4, '0')}`, i, i, `${productNames[i - 1][0]} 默认规格`, `/uploads/product/demo/spu-${String(i).padStart(2, '0')}.png`, s[2], 1, s[2], now, now, null];
});
const payOrders = [
  [1, 'PAYRCT202606100001', 'RCT202606100001', 1, 10000, 'SUCCESS', 'MOCK', 'MOCK-TXN-RCT-001', '{"channelStatus":"SUCCESS"}', now, now, 'PAY:RCT:001', 0, now, now, null],
  [2, 'PAYRCT202606100002', 'RCT202606100002', 1, 12000, 'PENDING', 'MOCK', '', '{"channelStatus":"NOT_PAID"}', null, null, 'PAY:RCT:002', 0, now, now, null],
  [3, 'PAYRCT202606100003', 'RCT202606100003', 1, 13000, 'SUCCESS', 'MOCK', 'MOCK-TXN-RCT-003', '{"channelStatus":"SUCCESS"}', now, now, 'PAY:RCT:003', 0, now, now, null],
  [4, 'PAYRCT202606100004', 'RCT202606100004', 1, 14000, 'PAYING', 'MOCK', '', '{"channelStatus":"SUCCESS"}', null, null, 'PAY:RCT:004', 0, now, now, null],
  [5, 'PAYRCT202606100005', 'RCT202606100005', 1, 15000, 'SUCCESS', 'MOCK', 'MOCK-TXN-RCT-005', '{"channelStatus":"MISSING"}', now, now, 'PAY:RCT:005', 0, now, now, null],
  [6, 'PAYRCT202606100007', 'RCT202606100007', 1, 16500, 'SUCCESS', 'MOCK', 'MOCK-TXN-RCT-007', '{"channelStatus":"SUCCESS","amountCent":16500}', now, now, 'PAY:RCT:007', 0, now, now, null],
  [7, 'PAYRCT202606100008', 'RCT202606100008', 1, 18000, 'SUCCESS', 'MOCK', 'MOCK-TXN-RCT-008', '{"channelStatus":"SUCCESS"}', now, now, 'PAY:RCT:008', 0, now, now, null],
  [8, 'PAYRCT202606100009', 'RCT202606100009', 1, 19000, 'SUCCESS', 'MOCK', 'MOCK-TXN-RCT-009', '{"channelStatus":"SUCCESS"}', now, now, 'PAY:RCT:009', 0, now, now, null],
  [9, 'PAYRCT202606100010', 'RCT202606100010', 1, 20000, 'SUCCESS', 'MOCK', 'MOCK-TXN-RCT-010', '{"channelStatus":"SUCCESS"}', now, now, 'PAY:RCT:010', 0, now, now, null]
];
const callbacks = [
  [1, 'MOCK', 'PAY', 'PAYRCT202606100001', null, 'RCT202606100001', 'PAYRCT202606100001', 'MOCK-TXN-RCT-001', 10000, 'SUCCESS', 'mock-signature', 1, 'PROCESSED', '', '{"case":"normal_success"}', now, now, now, now],
  [2, 'MOCK', 'PAY', 'PAYRCT202606100003', null, 'RCT202606100003', 'PAYRCT202606100003', 'MOCK-TXN-RCT-003', 13000, 'SUCCESS', 'mock-signature', 1, 'PROCESSED', '', '{"case":"pay_success_order_not_synced"}', now, now, now, now],
  [3, 'MOCK', 'PAY', 'PAYRCT202606100004', null, 'RCT202606100004', 'PAYRCT202606100004', 'MOCK-TXN-RCT-004', 14000, 'SUCCESS', 'mock-signature', 1, 'RECEIVED', '渠道已成功，本地支付单仍PAYING', '{"case":"local_paying_channel_success"}', now, null, now, now],
  [4, 'MOCK', 'PAY', 'PAYRCT202606100005', null, 'RCT202606100005', 'PAYRCT202606100005', '', 15000, 'NOT_FOUND', 'mock-signature', 1, 'RECEIVED', '渠道未查到成功交易', '{"case":"platform_success_channel_missing"}', now, null, now, now],
  [5, 'MOCK', 'PAY', 'PAYRCT202606100007', null, 'RCT202606100007', 'PAYRCT202606100007', 'MOCK-TXN-RCT-007', 16500, 'SUCCESS', 'mock-signature', 1, 'AMOUNT_MISMATCH', '支付金额与订单金额不一致', '{"case":"amount_mismatch"}', now, now, now, now],
  [6, 'MOCK', 'REFUND', 'PAYRCT202606100008', 'RFDRCT202606100008', 'RCT202606100008', 'RFDRCT202606100008', 'MOCK-RFD-RCT-008', 17000, 'REFUND_SUCCESS', 'mock-signature', 1, 'AMOUNT_MISMATCH', '退款渠道金额与本地退款金额不一致', '{"case":"refund_amount_mismatch"}', now, now, now, now]
];
const refunds = [[1, 8, 'RCT202606100008', 1, 'RFDRCT202606100008', 18000, 'MOCK-RFD-RCT-008', 'REFUNDING', '对账退款测试', null, now, now]];
const refundItems = [[1, 1, 'RFDRCT202606100008', 8, 8, 8, 18000]];
const payRefunds = [[1, 'RFDRCT202606100008', 'RCT202606100008', 'PAYRCT202606100008', 1, 'MOCK', 18000, 'REFUNDING', 'MOCK-TXN-RCT-008', 'MOCK-RFD-RCT-008', '{"reason":"对账退款测试"}', '{"channelStatus":"REFUND_SUCCESS","amountCent":17000}', '', null, 0, now, now]];
const aftersales = [[1, 'AFTRCT202606100008', 'RCT202606100008', 1, 'ONLY_REFUND', 'REFUNDING', 'PAID', 18000, '对账退款测试', null, 'RFDRCT202606100008', null, 0, now, now, now]];
const recons = [
  [1, 'RCPAY202606100001', 'PAY', 'RCT202606100001', 'PAYRCT202606100001', null, 'PAID/SUCCESS', 'SUCCESS', 10000, 10000, 1, 'NONE', 'NONE', 'CASE01 当前对平，重新对账仍一致', null, now, now],
  [2, 'RCPAY202606100002', 'PAY', 'RCT202606100002', 'PAYRCT202606100002', null, 'PENDING_PAYMENT/PENDING', 'NOT_PAID', 12000, 12000, 1, 'NONE', 'NONE', 'CASE02 待支付，不误判长短款', null, now, now],
  [3, 'RCPAY202606100003', 'PAY', 'RCT202606100003', 'PAYRCT202606100003', null, 'PENDING_PAYMENT/SUCCESS', 'SUCCESS', 13000, 13000, 0, 'ORDER_STATUS_NOT_SYNCED', 'PENDING', 'CASE03 支付单成功但订单未支付，可补偿订单已支付', null, now, now],
  [4, 'RCPAY202606100004', 'PAY', 'RCT202606100004', 'PAYRCT202606100004', null, 'PENDING_PAYMENT/PAYING', 'SUCCESS', 14000, 14000, 0, 'LOCAL_PAYING_CHANNEL_SUCCESS', 'PENDING', 'CASE04 渠道成功但本地未成功，可同步支付状态', null, now, now],
  [5, 'RCPAY202606100005', 'PAY', 'RCT202606100005', 'PAYRCT202606100005', null, 'PAID/SUCCESS', 'MISSING', 15000, 0, 0, 'LONG_PLATFORM_SUCCESS_CHANNEL_MISSING', 'PENDING', 'CASE05 平台成功但渠道未确认，待渠道调单/恢复待支付/关闭并释放库存/待用户补款', null, now, now],
  [6, 'RCPAY202606100006', 'PAY', 'RCT202606100006', null, null, 'PAID/NO_PAY_ORDER', 'MISSING', 16000, 0, 0, 'SHORT_PAID_ORDER_PAY_MISSING', 'PENDING', 'CASE06 订单已支付但缺少支付单，人工审核/恢复待支付/关闭释放库存', null, now, now],
  [7, 'RCPAY202606100007', 'PAY', 'RCT202606100007', 'PAYRCT202606100007', null, 'PAID/SUCCESS', 'SUCCESS', 17000, 16500, 0, 'AMOUNT_MISMATCH', 'PENDING', 'CASE07 金额不一致，待退款/待用户补款/人工审核', null, now, now],
  [8, 'RCREF202606100008', 'REFUND', 'RCT202606100008', 'PAYRCT202606100008', 'RFDRCT202606100008', 'REFUNDING', 'REFUND_SUCCESS', 18000, 17000, 0, 'STATUS_AND_AMOUNT_MISMATCH', 'PENDING', 'CASE08 退款状态与金额不一致，用于退款对账处理', null, now, now],
  [9, 'RCPAY202606100009', 'PAY', 'RCT202606100009', 'PAYRCT202606100009', null, 'PAYMENT_EXCEPTION/SUCCESS', 'SUCCESS', 19000, 19000, 0, 'PAY_SUCCESS_ORDER_NOT_PAID', 'PENDING', 'CASE09 支付异常处理：人工确认已支付/关闭释放库存/恢复待支付', null, now, now],
  [10, 'RCPAY202606100010', 'PAY', 'RCT202606100010', 'PAYRCT202606100010', null, 'SHIPPED/SUCCESS', 'SUCCESS', 20000, 20000, 1, 'NONE', 'NONE', 'CASE10 已发货履约状态下仍对平', null, now, now]
];
const stockRecon = [
  [1, 1, 'CONSISTENT', 'NONE', '{"source":"STOCK_TABLE","totalStock":121,"lockedStock":0,"availableStock":121}', '{"source":"DB_LOCK_RECORD_CALCULATED","totalStock":121,"lockedStock":0,"availableStock":121}', '{"source":"REDIS","totalStock":121,"lockedStock":0,"availableStock":121}', '[]', 'NONE', null, now, null, now, now],
  [2, 2, 'INCONSISTENT', 'WARNING', '{"source":"STOCK_TABLE","totalStock":122,"lockedStock":0,"availableStock":122}', '{"source":"DB_LOCK_RECORD_CALCULATED","totalStock":122,"lockedStock":1,"availableStock":121}', '{"source":"REDIS","totalStock":122,"lockedStock":1,"availableStock":121}', '["库存表锁定库存与锁记录汇总不一致","库存表可用库存与理论可用库存不一致"]', 'PENDING', 'CASE09 库存不一致SKU，用于库存修复/批量修复/忽略差异', now, null, now, now]
];
const stockLogs = [[1, 1, 'INIT', 'SYSTEM', 'INIT_SKU_1', 121, 0, 0, 0, 121, 0, 121, '初始化库存日志', 'SYSTEM', null, 'system', 'INIT', now], [2, 2, 'RECONCILE_DIFF', 'SYSTEM', 'STOCK_DIFF_SKU_2', 0, 122, 0, 122, 122, 0, 122, '库存对账测试差异日志', 'SYSTEM', null, 'system', 'RECONCILIATION_JOB', now]];
const sessionLogs = [[1, 1, 'ADMIN', 'admin', 'admin-web', 'LOGIN_SUCCESS', 'SUCCESS', 'ADMIN:1', 'demo-admin-token', '127.0.0.1', 'Mozilla/5.0', '管理员登录日志', now], [2, 1, 'USER', '13800000000', 'user-h5', 'LOGIN_SUCCESS', 'SUCCESS', 'USER:1', 'demo-user-token', '127.0.0.1', 'Mozilla/5.0 Mobile', '用户登录日志', now]];
const cart = [[1, 1, 1, 1, 1, now, now, null], [2, 2, 2, 2, 1, now, now, null]];
const locks = [[1, 'LOCK-RCT202606100009-001', 2, 'ORDER', 'RCT202606100009', 1, 'LOCKED', now, null, null, 1, 0, 0, now, now]];

const replacements = {
  ums_admin: admins,
  ums_admin_operation_log: adminLogs,
  ums_user: users,
  ums_user_address: addresses,
  ums_user_third_bind: thirdBinds,
  pms_spu: spus,
  pms_sku: skus,
  pms_product_sales_daily_stat: productSalesDailyStats,
  pms_product_sales_stat_event: productSalesEvents,
  ims_stock: stocks,
  ims_stock_lock: locks,
  ims_stock_operation_log: stockLogs,
  ims_stock_reconciliation_record: stockRecon,
  auth_session_event_log: sessionLogs,
  cart_item: cart,
  aftersale_order: aftersales,
  oms_order: orders,
  oms_order_item: orderItems,
  oms_order_refund: refunds,
  oms_order_refund_item: refundItems,
  pay_order: payOrders,
  pay_callback_record: callbacks,
  pay_reconciliation_record: recons,
  pay_refund_order: payRefunds
};
for (const [table, rows] of Object.entries(replacements)) replaceRecords(table, rows);
for (const [table, rows] of Object.entries(replacements)) resetAuto(table, rows.length + 1);

const header = `-- ============================================================\n-- Curated clean demo/test data\n-- 1. 管理员：每个角色仅保留 1 个账号，ID 1-7 连续，操作日志同步清理。\n-- 2. 用户：仅保留 20 个用户，ID 1-20 连续，地址/登录/购物车/订单/支付/售后引用全部同步。\n-- 3. 商品：SPU/SKU 均从 1 连续重建，SKU 的 spu_id、库存、购物车、订单项、退款项、销售统计全部同步引用。\n-- 4. 订单支付售后对账：生成 10 单覆盖正常、待支付、支付成功未同步、渠道成功本地未成功、渠道缺失、缺支付单、金额不一致、退款对账、异常处置、已发货对平。\n-- 5. 脏数据：核心业务表 Records 段整体替换，避免历史孤儿订单、支付单、退款单、对账单、库存锁、日志、商品跳号残留。\n-- ============================================================\n`;
text = text.replace('-- Table structure for admin_operation_config', `${header}\n-- Table structure for admin_operation_config`);
fs.writeFileSync(out, text, { encoding: 'utf8' });
console.log(out);
