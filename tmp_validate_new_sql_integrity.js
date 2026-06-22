const fs = require('fs');
const text = fs.readFileSync('backend/sql/new_mall_fei.sql', 'utf8');
function rows(table) {
  const re = new RegExp('INSERT INTO `'+table+'` VALUES \\(([^\\n]*)\\);', 'g');
  return [...text.matchAll(re)].map(m => m[1]);
}
function splitVals(s) {
  const arr=[]; let cur='', q=false, esc=false;
  for (const c of s) {
    if (esc) { cur+=c; esc=false; continue; }
    if (q && c === '\\') { cur+=c; esc=true; continue; }
    if (c === "'") { q=!q; cur+=c; continue; }
    if (!q && c === ',') { arr.push(cur.trim()); cur=''; continue; }
    cur+=c;
  }
  arr.push(cur.trim()); return arr;
}
function vals(table) { return rows(table).map(splitVals); }
function nums(table, idx=0) { return vals(table).map(r => Number(r[idx])); }
function assertSeq(name, arr) {
  const expected = Array.from({length: arr.length}, (_, i) => i + 1);
  if (arr.join(',') !== expected.join(',')) throw new Error(`${name} not sequential: ${arr.join(',')}`);
}
function set(arr) { return new Set(arr); }
function assertRefs(name, refs, targetSet) {
  const missing = refs.filter(x => !targetSet.has(x));
  if (missing.length) throw new Error(`${name} missing refs: ${[...new Set(missing)].join(',')}`);
}
const spuIds = nums('pms_spu');
const skuRows = vals('pms_sku');
const skuIds = skuRows.map(r => Number(r[0]));
const skuSpuIds = skuRows.map(r => Number(r[1]));
assertSeq('pms_spu.id', spuIds);
assertSeq('pms_sku.id', skuIds);
assertSeq('ims_stock.id', nums('ims_stock'));
assertSeq('oms_order.id', nums('oms_order'));
assertSeq('oms_order_item.id', nums('oms_order_item'));
const spuSet = set(spuIds);
const skuSet = set(skuIds);
assertRefs('pms_sku.spu_id', skuSpuIds, spuSet);
assertRefs('ims_stock.sku_id', vals('ims_stock').map(r => Number(r[1])), skuSet);
assertRefs('cart_item.sku_id', vals('cart_item').map(r => Number(r[3])), skuSet);
assertRefs('oms_order_item.sku_id', vals('oms_order_item').map(r => Number(r[3])), skuSet);
assertRefs('oms_order_item.spu_id', vals('oms_order_item').map(r => Number(r[4])), spuSet);
assertRefs('oms_order_refund_item.sku_id', vals('oms_order_refund_item').map(r => Number(r[4])), skuSet);
assertRefs('oms_order_refund_item.spu_id', vals('oms_order_refund_item').map(r => Number(r[5])), spuSet);
assertRefs('pms_product_sales_daily_stat.spu_id', vals('pms_product_sales_daily_stat').map(r => Number(r[2])), spuSet);
assertRefs('pms_product_sales_daily_stat.sku_id', vals('pms_product_sales_daily_stat').map(r => Number(r[3])), skuSet);
const dirtyPatterns = [/INSERT INTO `pms_spu` VALUES \((?:9\d{4,}|10[3-9])/, /INSERT INTO `pms_sku` VALUES \((?:9\d{4,}|10[3-9]|28[0-9])/, /INSERT INTO `ums_user` VALUES \((?:10[3-9]|1000\d+)/, /img\.mallfei\.local\/avatar/];
for (const p of dirtyPatterns) if (p.test(text)) throw new Error(`dirty pattern remains: ${p}`);
for (const [table, expected] of [['pms_spu', 13], ['pms_sku', 13], ['ims_stock', 13], ['ums_user', 21], ['ums_admin', 8], ['oms_order', 11]]) {
  const re = new RegExp('CREATE TABLE `'+table+'`[\\s\\S]*?AUTO_INCREMENT = (\\d+)');
  const m = text.match(re);
  if (!m || Number(m[1]) !== expected) throw new Error(`${table} AUTO_INCREMENT expected ${expected}, got ${m && m[1]}`);
}
console.log('integrity=ok');
console.log(`spu=${spuIds.length}, sku=${skuIds.length}, stock=${nums('ims_stock').length}, users=${nums('ums_user').length}, admins=${nums('ums_admin').length}, orders=${nums('oms_order').length}`);
