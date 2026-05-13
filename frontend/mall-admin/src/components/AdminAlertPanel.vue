<template>
  <el-card class="admin-page-card">
    <template #header>
      <div class="admin-toolbar">
        <span>异常监控</span>
        <el-tag v-if="alerts.length" type="danger">{{ alerts.length }} 条待关注</el-tag>
        <el-tag v-else type="success">运行正常</el-tag>
      </div>
    </template>
    <div v-if="alerts.length" class="alert-list">
      <div v-for="item in alerts" :key="item.id" class="alert-item">
        <div class="alert-item__header">
          <div>
            <div class="alert-item__title">{{ item.title }}</div>
            <div class="alert-item__meta">{{ item.operator }} · {{ item.time }}</div>
          </div>
          <el-tag :type="alertLevelMeta(item.level).type">{{ alertLevelMeta(item.level).label }}</el-tag>
        </div>
        <div class="alert-item__content">{{ item.content }}</div>
      </div>
    </div>
    <el-empty v-else description="暂无异常操作提醒" />
  </el-card>
</template>

<script setup>
import { computed } from 'vue';
import { getStatusTagMeta } from '../utils/status';

const props = defineProps({
  logs: { type: Array, default: () => [] },
});

const alertLevelMeta = (level) => getStatusTagMeta('alertLevel', level);

const alerts = computed(() => props.logs
  .filter((item) => item.operationResult === 'FAILED' || /权限|违规|拒绝|失败/.test(item.operationContent || ''))
  .slice(0, 6)
  .map((item) => ({
    id: item.id,
    title: `${item.operationModule} / ${item.operationType}`,
    content: item.operationContent,
    operator: item.operatorUsername || '-',
    time: item.createdAt || '-',
    level: item.operationResult === 'FAILED' ? 'HIGH' : 'MEDIUM',
  })));
</script>

<style scoped>
.alert-list { display: flex; flex-direction: column; gap: 12px; }
.alert-item { padding: 14px; border: 1px solid #ebeef5; border-radius: 10px; background: #fffaf8; }
.alert-item__header { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; }
.alert-item__title { font-weight: 700; }
.alert-item__meta { margin-top: 4px; color: #909399; font-size: 12px; }
.alert-item__content { margin-top: 10px; color: #606266; line-height: 1.6; }
</style>
