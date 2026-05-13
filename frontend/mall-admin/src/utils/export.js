export function exportRowsToCsv(filename, rows, columns) {
  const headers = columns.map((column) => escapeCsvValue(column.label)).join(',');
  const content = rows.map((row) => columns.map((column) => escapeCsvValue(resolveValue(row, column))).join(',')).join('\n');
  const csv = `\uFEFF${headers}\n${content}`;
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${filename}.csv`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

function resolveValue(row, column) {
  const value = typeof column.value === 'function' ? column.value(row) : row?.[column.value];
  return value ?? '';
}

function escapeCsvValue(value) {
  const text = String(value ?? '');
  if (/[",\n]/.test(text)) {
    return `"${text.replace(/"/g, '""')}"`;
  }
  return text;
}
