const fs = require('fs');
const text = fs.readFileSync('backend/sql/new_mall_fei.sql', 'utf8').replace(/\r\n/g, '\n');
const tables = new Map();
for (const m of text.matchAll(/CREATE TABLE `([^`]+)`\s+\(([\s\S]*?)\n\) ENGINE/g)) {
  const cols = [...m[2].matchAll(/^\s*`([^`]+)`\s+/gm)].map(x => x[1]);
  tables.set(m[1], cols);
}
function splitVals(s) {
  const arr=[]; let cur='', q=false, esc=false, depth=0;
  for (let i=0;i<s.length;i++) { const c=s[i];
    if (esc) { cur+=c; esc=false; continue; }
    if (q && c==='\\') { cur+=c; esc=true; continue; }
    if (c==="'") { q=!q; cur+=c; continue; }
    if (!q && (c==='(' || c==='[' || c==='{')) depth++;
    if (!q && (c===')' || c===']' || c==='}')) depth--;
    if (!q && depth===0 && c===',') { arr.push(cur.trim()); cur=''; continue; }
    cur+=c;
  }
  arr.push(cur.trim()); return arr;
}
let bad=0;
for (const m of text.matchAll(/INSERT INTO `([^`]+)` VALUES \(([^\n]*)\);/g)) {
  const table=m[1], vals=splitVals(m[2]), cols=tables.get(table)||[];
  if (cols.length && vals.length!==cols.length) {
    bad++;
    const line=text.slice(0,m.index).split('\n').length;
    console.log(`${line} ${table}: columns=${cols.length}, values=${vals.length}`);
    console.log('cols=', cols.join(','));
    console.log('sql=', m[0].slice(0,500));
  }
}
console.log('bad=',bad);
