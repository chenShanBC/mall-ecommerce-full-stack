const fs = require('fs');
const path = require('path');

const sqlPath = path.resolve(process.argv[2]);
const sql = fs.readFileSync(sqlPath, 'utf8');
const lines = sql.split(/\r?\n/);

function parseInsertLine(line) {
  const raw = line.slice(line.indexOf('(') + 1, line.lastIndexOf(')'));
  const values = [];
  let current = '';
  let inString = false;
  let escapeNext = false;

  for (let i = 0; i < raw.length; i += 1) {
    const ch = raw[i];
    if (escapeNext) {
      current += ch;
      escapeNext = false;
      continue;
    }
    if (ch === '\\') {
      current += ch;
      escapeNext = true;
      continue;
    }
    if (ch === "'") {
      inString = !inString;
      current += ch;
      continue;
    }
    if (ch === ',' && !inString) {
      values.push(current.trim());
      current = '';
      continue;
    }
    current += ch;
  }
  values.push(current.trim());
  return values;
}

function stripQuotes(value) {
  if (value == null) return value;
  if (value === 'NULL') return null;
  if (value.startsWith("'") && value.endsWith("'")) {
    return value.slice(1, -1).replace(/\\'/g, "'");
  }
  return value;
}

const categoryRows = lines.filter((line) => line.startsWith('INSERT INTO `pms_category` VALUES'));
const spuRows = lines.filter((line) => line.startsWith('INSERT INTO `pms_spu` VALUES'));

const categories = categoryRows.map(parseInsertLine).map((v) => ({
  id: Number(v[0]),
  name: stripQuotes(v[1]),
  parentId: Number(v[2]),
  level: Number(v[3]),
  sortOrder: Number(v[4]),
  status: stripQuotes(v[5]),
}));

const spus = spuRows.map(parseInsertLine).map((v) => ({
  id: Number(v[0]),
  name: stripQuotes(v[1]),
  categoryId: Number(v[2]),
  mainImageUrl: stripQuotes(v[3]),
  description: stripQuotes(v[5]),
  status: stripQuotes(v[6]),
}));

const catById = new Map(categories.map((item) => [item.id, item]));
const rootCategories = categories.filter((item) => item.parentId === 0);
const childCategories = categories.filter((item) => item.parentId !== 0);

const categoryProductCount = new Map();
for (const spu of spus) {
  categoryProductCount.set(spu.categoryId, (categoryProductCount.get(spu.categoryId) || 0) + 1);
}

const duplicateNameMap = new Map();
for (const spu of spus) {
  duplicateNameMap.set(spu.name, (duplicateNameMap.get(spu.name) || 0) + 1);
}

const duplicates = [...duplicateNameMap.entries()]
  .filter(([, count]) => count > 1)
  .sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))
  .map(([name, count]) => ({ name, count }));

const categorySummary = categories
  .map((item) => ({
    id: item.id,
    name: item.name,
    parentId: item.parentId,
    level: item.level,
    status: item.status,
    productCount: categoryProductCount.get(item.id) || 0,
  }))
  .sort((a, b) => a.id - b.id);

const attachedToRoot = spus.filter((spu) => catById.get(spu.categoryId)?.parentId === 0).length;
const attachedToChild = spus.filter((spu) => catById.get(spu.categoryId)?.parentId !== 0).length;

const rootMappingPreview = rootCategories.map((root) => {
  const children = childCategories.filter((child) => child.parentId === root.id);
  return {
    rootId: root.id,
    rootName: root.name,
    childCount: children.length,
    childNames: children.slice(0, 20).map((child) => child.name),
    productCountOnRoot: categoryProductCount.get(root.id) || 0,
  };
});

const keywordGroups = ['保温杯','跑步鞋','护眼台灯','空气炸锅','无线耳机','行李箱','防晒霜','机械键盘','牛奶礼盒','乳胶枕','电动牙刷','双肩包'];
const keywordStats = keywordGroups.map((keyword) => ({
  keyword,
  count: spus.filter((spu) => spu.name.includes(keyword)).length,
  categoryIds: [...new Set(spus.filter((spu) => spu.name.includes(keyword)).map((spu) => spu.categoryId))].sort((a, b) => a - b),
}));

const result = {
  categoryCount: categories.length,
  rootCategoryCount: rootCategories.length,
  childCategoryCount: childCategories.length,
  spuCount: spus.length,
  attachedToRoot,
  attachedToChild,
  rootMappingPreview,
  duplicates,
  keywordStats,
  topCategoriesByProductCount: categorySummary
    .slice()
    .sort((a, b) => b.productCount - a.productCount || a.id - b.id)
    .slice(0, 30),
};

console.log(JSON.stringify(result, null, 2));
