<template>
  <section v-if="visible" class="glass role-switcher">
    <div class="section-head">
      <div>
        <b>分角色仪表盘</b>
        <p>仅超级管理员可见，用于在当前页切换不同业务角色视图</p>
      </div>
      <span class="section-head__tip">当前页切换</span>
    </div>
    <div class="role-grid">
      <button
        v-for="role in roles"
        :key="role.code"
        class="role-card"
        :class="{ 'is-active': modelValue === role.code }"
        type="button"
        @click="$emit('update:modelValue', role.code)"
      >
        <div class="role-card__title">{{ role.title }}</div>
        <div class="role-card__desc">{{ role.description }}</div>
        <div class="role-card__footer">
          <strong>{{ role.footerValue }}</strong>
          <span>{{ role.footerLabel }}</span>
        </div>
      </button>
    </div>
  </section>
</template>

<script setup>
defineProps({
  visible: { type: Boolean, default: false },
  roles: { type: Array, default: () => [] },
  modelValue: { type: String, default: 'OPERATIONS' },
});

defineEmits(['update:modelValue']);
</script>

<style scoped>
.glass{border-radius:30px;background:rgba(255,255,255,.78);backdrop-filter:blur(18px);border:1px solid rgba(255,255,255,.86);box-shadow:0 24px 60px rgba(108,123,225,.12)}.role-switcher{padding:22px}.section-head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px;margin-bottom:18px}.section-head b{font-size:18px;color:#2d3a64}.section-head p{margin:6px 0 0;font-size:13px;color:#94a3b8}.section-head__tip{padding:8px 12px;border-radius:999px;background:rgba(91,108,255,.1);color:#5b6cff;font-size:12px;font-weight:700}.role-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:14px}.role-card{border:none;text-align:left;padding:18px;border-radius:24px;background:linear-gradient(135deg,rgba(248,250,255,.98),rgba(236,242,255,.94));cursor:pointer;box-shadow:inset 0 1px 0 rgba(255,255,255,.75);transition:transform .2s ease,box-shadow .2s ease,background .2s ease}.role-card:hover{transform:translateY(-2px);box-shadow:0 18px 34px rgba(91,108,255,.12)}.role-card.is-active{background:linear-gradient(135deg,rgba(91,108,255,.14),rgba(124,141,255,.2));box-shadow:0 18px 34px rgba(91,108,255,.16)}.role-card__title{font-size:16px;font-weight:800;color:#334155}.role-card__desc{margin-top:10px;min-height:42px;font-size:12px;line-height:1.7;color:#94a3b8}.role-card__footer{display:flex;align-items:flex-end;justify-content:space-between;margin-top:18px}.role-card__footer strong{font-size:28px;color:#5b6cff}.role-card__footer span{font-size:12px;color:#64748b}@media (max-width:1280px){.role-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}@media (max-width:768px){.role-switcher{padding:18px}.role-grid{grid-template-columns:1fr}}
</style>
