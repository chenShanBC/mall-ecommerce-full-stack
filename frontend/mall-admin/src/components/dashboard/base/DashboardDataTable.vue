<template>
  <section class="data-card">
    <div class="data-card__head">
      <div>
        <h3>{{ title }}</h3>
        <p>{{ desc }}</p>
      </div>
      <el-button link type="primary" @click="$emit('refresh')">刷新</el-button>
    </div>
    <el-table v-loading="loading" :data="rows" class="admin-table" empty-text="暂无数据">
      <el-table-column v-for="column in columns" :key="column.prop" :prop="column.prop" :label="column.label" :min-width="column.minWidth" :width="column.width">
        <template #default="{ row }">
          <el-tag v-if="column.type === 'tag'" :type="resolveTagType(row[column.prop])">{{ formatCell(row, column) }}</el-tag>
          <span v-else>{{ formatCell(row, column) }}</span>
        </template>
      </el-table-column>
    </el-table>
    <div v-if="pageable" class="data-card__pager">
      <el-pagination background layout="prev, pager, next" :current-page="page" :page-size="size" :total="total" @current-change="$emit('page-change', $event)" />
    </div>
  </section>
</template>

<script setup>
defineProps({
  title: { type: String, required: true },
  desc: { type: String, default: '' },
  columns: { type: Array, default: () => [] },
  rows: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  pageable: { type: Boolean, default: false },
  page: { type: Number, default: 1 },
  size: { type: Number, default: 5 },
  total: { type: Number, default: 0 },
});

defineEmits(['refresh', 'page-change']);

const formatCell = (row, column) => {
  const value = row[column.prop];
  if (column.format === 'money') return `¥${(Number(value || 0) / 100).toFixed(2)}`;
  if (column.format === 'percent') return `${Number(value || 0)}%`;
  return value ?? '--';
};

const resolveTagType = (value) => {
  if (['HIGH', 'DANGER', 'ABNORMAL', 'FAILED', 'LOW_STOCK', 'ERROR', 'OVERDUE'].includes(value)) return 'danger';
  if (['LOW', 'WARNING', 'PENDING', 'HANGING'].includes(value)) return 'warning';
  if (['NORMAL', 'SUCCESS', 'COMPLETED', 'ON_SALE', 'DONE', 'ACTIVE'].includes(value)) return 'success';
  return 'info';
};
</script>

<style scoped>
.data-card{border-radius:22px;padding:18px;background:#fff;box-shadow:0 16px 36px rgba(15,23,42,.07);overflow:hidden}.data-card__head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;margin-bottom:12px}.data-card h3{margin:0;color:#1e293b;font-size:17px}.data-card p{margin:6px 0 0;color:#94a3b8;font-size:12px}.data-card__pager{display:flex;justify-content:flex-end;margin-top:12px}
</style>
