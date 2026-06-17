<template>
  <el-tooltip
    :content="displayText"
    placement="top-start"
    effect="light"
    :show-after="180"
    :hide-after="0"
    :offset="4"
    :disabled="!isOverflow"
    :popper-class="popperClass"
  >
    <span ref="triggerRef" class="admin-overflow-cell">
      <span ref="textRef" class="admin-overflow-ellipsis" :class="textClass">{{ displayText }}</span>
    </span>
  </el-tooltip>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';

const props = defineProps({
  value: { type: [String, Number, Boolean], default: '' },
  fallback: { type: String, default: '-' },
  textClass: { type: [String, Array, Object], default: '' },
  popperClass: { type: String, default: 'admin-overflow-tooltip' },
});

const triggerRef = ref(null);
const textRef = ref(null);
const isOverflow = ref(false);
let resizeObserver;
let rafId = 0;

const displayText = computed(() => {
  if (props.value === null || props.value === undefined || props.value === '') return props.fallback;
  return String(props.value);
});

const checkOverflow = async () => {
  await nextTick();
  cancelAnimationFrame(rafId);
  rafId = requestAnimationFrame(() => {
    const triggerEl = triggerRef.value;
    const textEl = textRef.value;
    if (!triggerEl || !textEl) {
      isOverflow.value = false;
      return;
    }
    isOverflow.value = textEl.scrollWidth > textEl.clientWidth + 1;
  });
};

onMounted(() => {
  checkOverflow();
  if (typeof ResizeObserver !== 'undefined' && triggerRef.value) {
    resizeObserver = new ResizeObserver(checkOverflow);
    resizeObserver.observe(triggerRef.value);
  }
  window.addEventListener('resize', checkOverflow);
});

onBeforeUnmount(() => {
  resizeObserver?.disconnect();
  window.removeEventListener('resize', checkOverflow);
  cancelAnimationFrame(rafId);
});

watch(displayText, checkOverflow);
</script>

<style scoped>
.admin-overflow-cell {
  display: block;
  width: 100%;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
}

.admin-overflow-ellipsis {
  display: block;
  width: 100%;
  min-width: 0;
  max-width: 100%;
  overflow: hidden;
  color: inherit;
  line-height: 1.45;
  text-overflow: ellipsis;
  white-space: nowrap;
  word-break: keep-all;
  overflow-wrap: normal;
  vertical-align: middle;
  cursor: default;
}

:global(.admin-overflow-tooltip.el-popper.is-light) {
  max-width: 360px;
  padding: 8px 12px;
  border: 1px solid rgba(99, 102, 241, 0.18);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 10px 28px rgba(79, 70, 229, 0.16), 0 4px 12px rgba(15, 23, 42, 0.08);
  color: #1e293b;
  line-height: 1.45;
}

:global(.admin-overflow-tooltip.el-popper.is-light .el-popper__arrow::before) {
  border-color: rgba(99, 102, 241, 0.18);
  background: rgba(255, 255, 255, 0.98);
}
</style>
