const fs = require('fs');
const s = fs.readFileSync('backend/sql/new_mall_fei.sql', 'utf8');
function splitVals(v) {
  return v.split(/, (?=(?:[^']*'[^']*')*[^']*$)/).map((x) => {
    x = x.trim();
    if (x === 'NULL') return null;
    if (x.startsWith("'") && x.endsWith("'")) return x.slice(1, -1).replace(/\\'/g, "'");
    return /^-?\d+$/.test(x) ? Number(x) : x;
  });
}
function rows(table) {
  return [...s.matchAll(new RegExp('INSERT INTO `' + table + '` VALUES \\(([^;]+)\\);', 'g'))].map((m) => splitVals(m[1]));
}
const order = rows('oms_order');
const item = rows('oms_order_item');
const pay = rows('pay_order');
const refund = rows('oms_order_refund');
const refundItem = rows('oms_order_refund_item');
const after = rows('aftersale_order');
const payRefund = rows('pay_refund_order');
const cb = rows('pay_callback_record');
const orders = new Map(order.map((r) => [r[1], r]));
const orderById = new Map(order.map((r) => [r[0], r]));
const itemsByOrderNo = new Map();
for (const r of item) (itemsByOrderNo.get(r[2]) || itemsByOrderNo.set(r[2], []).get(r[2])).push(r);
const payByOrderNo = new Map(pay.map((r) => [r[2], r]));
const refundsByNo = new Map(refund.map((r) => [r[4], r]));
const afterRefundNos = new Map(after.filter((r) => r[10]).map((r) => [r[10], r]));
const payRefundByNo = new Map(payRefund.map((r) => [r[1], r]));
const problems = [];
const bad = (msg) => problems.push(msg);
for (const o of order) {
  const its = itemsByOrderNo.get(o[1]) || [];
  if (!its.length) bad(`订单无明细: ${o[1]}`);
  const sum = its.reduce((a, r) => a + Number(r[9] || 0), 0);
  if (sum !== o[4]) bad(`订单金额与明细合计不一致: ${o[1]} order=${o[4]} items=${sum}`);
}
for (const p of pay) {
  const o = orders.get(p[2]);
  if (!o) bad(`支付单无订单: ${p[1]} -> ${p[2]}`);
  else {
    if (p[4] !== o[4]) bad(`支付金额与订单金额不一致: ${p[1]} pay=${p[4]} order=${o[4]}`);
    if (['SUCCESS', 'REFUNDED', 'PARTIALLY_REFUNDED'].includes(p[5]) && !['PAID', 'SHIPPED', 'COMPLETED', 'REFUND_PENDING', 'REFUNDED', 'PAYMENT_EXCEPTION'].includes(o[3])) bad(`支付成功类状态但订单状态异常: ${p[1]} pay=${p[5]} order=${o[3]}`);
  }
}
for (const r of refund) {
  const o = orderById.get(r[1]);
  if (!o) bad(`退款单无订单ID: ${r[4]} orderId=${r[1]}`);
  else {
    if (o[1] !== r[2]) bad(`退款单订单号与订单ID不匹配: ${r[4]}`);
    if (Number(r[5]) > Number(o[4])) bad(`退款金额大于订单金额: ${r[4]}`);
  }
  const a = afterRefundNos.get(r[4]);
  if (!a) bad(`退款单无售后单绑定: ${r[4]}`);
  else {
    if (a[2] !== r[2]) bad(`售后订单号与退款订单号不一致: ${r[4]}`);
    if (a[7] !== r[5]) bad(`售后退款金额与退款单金额不一致: ${r[4]}`);
    if (r[7] === 'REFUND_SUCCESS' && a[5] !== 'REFUND_SUCCESS') bad(`退款成功但售后非成功: ${r[4]} after=${a[5]}`);
    if (r[7] === 'REFUNDING' && a[5] !== 'REFUND_PROCESSING') bad(`退款中但售后非处理中: ${r[4]} after=${a[5]}`);
  }
  const pr = payRefundByNo.get(r[4]);
  if (!pr) bad(`订单退款单无支付退款单: ${r[4]}`);
  else {
    if (pr[2] !== r[2]) bad(`支付退款单订单号不一致: ${r[4]}`);
    if (pr[6] !== r[5]) bad(`支付退款单金额不一致: ${r[4]}`);
    if (pr[7] !== r[7]) bad(`支付退款单状态不一致: ${r[4]} payRefund=${pr[7]} orderRefund=${r[7]}`);
  }
  const ris = refundItem.filter((x) => x[2] === r[4]);
  if (!ris.length) bad(`退款单无退款明细: ${r[4]}`);
  const sum = ris.reduce((a, x) => a + Number(x[6] || 0), 0);
  if (sum !== r[5]) bad(`退款明细金额合计不一致: ${r[4]} refund=${r[5]} items=${sum}`);
  for (const ri of ris) {
    const oi = item.find((x) => x[0] === ri[3]);
    if (!oi) bad(`退款明细无订单明细: ${r[4]} orderItemId=${ri[3]}`);
    else {
      if (oi[2] !== r[2]) bad(`退款明细订单明细不属于该订单: ${r[4]}`);
      if (ri[4] !== oi[4]) bad(`退款明细SKU不一致: ${r[4]}`);
    }
  }
}
for (const a of after) {
  const o = orders.get(a[2]);
  if (!o) bad(`售后单无订单: ${a[1]} -> ${a[2]}`);
  if (a[10] && !refundsByNo.has(a[10])) bad(`售后绑定退款号但退款单不存在: ${a[1]} refundNo=${a[10]}`);
  if (['REFUND_PROCESSING', 'REFUND_SUCCESS', 'REFUND_FAILED'].includes(a[5]) && !a[10]) bad(`售后退款态但未绑定退款号: ${a[1]}`);
  if (a[5] === 'REJECTED' && a[10]) bad(`售后驳回不应绑定退款号: ${a[1]}`);
}
for (const c of cb.filter((r) => r[2] === 'REFUND')) {
  if (c[4] && !refundsByNo.has(c[4])) bad(`退款回调无订单退款单: cb=${c[0]} refundNo=${c[4]}`);
  const r = refundsByNo.get(c[4]);
  if (r && c[8] !== r[5]) bad(`退款回调金额与退款单不一致: cb=${c[0]} refund=${c[4]} cb=${c[8]} refund=${r[5]}`);
}
console.log(JSON.stringify({ counts: { orders: order.length, items: item.length, pay: pay.length, refunds: refund.length, refundItems: refundItem.length, aftersales: after.length, payRefunds: payRefund.length, callbacks: cb.length }, problems }, null, 2));
