<template>
  <button class="metric-card" :class="[`metric-card--${theme}`, { 'metric-card--danger': danger }]" type="button" @click="$emit('click')">
    <div class="metric-card__label">{{ label }}</div>
    <div class="metric-card__value">{{ displayValue }}</div>
    <div class="metric-card__hint">{{ hint || '实时统计' }}</div>
  </button>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  label: { type: String, required: true },
  value: { type: [String, Number], default: 0 },
  hint: { type: String, default: '' },
  theme: { type: String, default: 'operations' },
  danger: { type: Boolean, default: false },
});

defineEmits(['click']);

const displayValue = computed(() => {
  if (typeof props.value === 'number') return props.value.toLocaleString('zh-CN');
  return props.value || '--';
});
</script>

<style scoped>
.metric-card{width:100%;min-height:104px;text-align:left;border:0;border-radius:20px;padding:16px;background:#fff;box-shadow:0 14px 32px rgba(15,23,42,.07);cursor:pointer;transition:transform .18s ease,box-shadow .18s ease}.metric-card:hover{transform:translateY(-2px);box-shadow:0 20px 42px rgba(15,23,42,.11)}.metric-card__label{font-size:13px;font-weight:800;color:#64748b}.metric-card__value{margin-top:10px;font-size:26px;line-height:1;font-weight:900;color:#0f172a}.metric-card__hint{margin-top:9px;font-size:12px;color:#94a3b8}.metric-card--operations{background:linear-gradient(135deg,#fff,#eff6ff)}.metric-card--finance{background:linear-gradient(135deg,#fff,#f5f3ff)}.metric-card--warehouse{background:linear-gradient(135deg,#fff,#ecfdf5)}.metric-card--products{background:linear-gradient(135deg,#fff,#fff7ed)}.metric-card--danger{box-shadow:0 18px 38px rgba(239,68,68,.16)}.metric-card--danger .metric-card__value{color:#dc2626}
</style>
