<template>
  <el-container class="admin-layout">
    <el-aside width="248px" class="aside">
      <div class="brand-panel">
        <div class="brand-mark">M</div>
        <div>
          <div class="brand-title">mall-admin</div>
          <div class="brand-subtitle">运营控制台</div>
        </div>
      </div>
      <el-menu :default-active="route.path" class="admin-menu" @select="handleMenuSelect">
        <el-menu-item index="/dashboard">仪表盘</el-menu-item>
        <el-menu-item v-if="adminStore.hasPermission('user:view')" index="/users">用户管理</el-menu-item>
        <el-menu-item v-if="adminStore.hasPermission('product:view')" index="/products">商品管理</el-menu-item>
        <el-menu-item v-if="adminStore.hasPermission('stock:view')" index="/stocks">库存管理</el-menu-item>
        <el-menu-item v-if="adminStore.hasPermission('order:view')" index="/orders">订单管控</el-menu-item>
        <el-menu-item v-if="adminStore.hasPermission('aftersale:view')" index="/aftersales">售后管理</el-menu-item>
        <el-menu-item v-if="adminStore.hasPermission('payment:view')" index="/pays">支付单管控</el-menu-item>
        <el-menu-item v-if="adminStore.hasPermission('reconciliation:view')" index="/reconciliations">对账运营</el-menu-item>
        <el-menu-item v-if="adminStore.hasAnyPermission(['admin:view', 'role:view', 'permission:view'])" index="/accounts">账号权限</el-menu-item>
        <el-menu-item v-if="adminStore.hasPermission('log:operation:view')" index="/operation-logs">操作日志</el-menu-item>
        <el-menu-item index="/profile">个人中心</el-menu-item>
        <el-menu-item v-if="adminStore.hasPermission('stock:log:view')" index="/stock-logs">库存日志</el-menu-item>
      </el-menu>
      <div class="aside-footer">
        <div class="aside-footer__label">当前角色</div>
        <div class="aside-footer__value">{{ adminStore.profile?.roleCode || '-' }}</div>
      </div>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div>
          <div class="title">{{ title }}</div>
          <div class="sub-title">当前登录：{{ adminStore.profile?.nickname || '未加载' }} / {{ adminStore.profile?.roleCode || '-' }}</div>
        </div>
        <div class="header-center">
          <div class="header-search-shell">
            <span class="header-search-shell__placeholder">运营后台 · 数据总览</span>
          </div>
        </div>
        <div class="actions">
          <el-button class="header-action-button" size="small" @click="$emit('refresh')">刷新</el-button>
          <el-button class="header-action-button header-action-button--danger" size="small" @click="$emit('logout')">退出</el-button>
        </div>
      </el-header>
      <el-main class="main"><slot /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router';
import { useAdminStore } from '../stores/admin';

defineProps({ title: { type: String, default: '后台管理' } });
defineEmits(['refresh', 'logout']);
const route = useRoute();
const router = useRouter();
const adminStore = useAdminStore();

const handleMenuSelect = async (path) => {
  if (path === route.path) {
    return;
  }
  const valid = await adminStore.ensureSessionValid();
  if (!valid) {
    router.push('/login');
    return;
  }
  router.push(path);
};
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(129, 140, 248, 0.22), transparent 28%),
    radial-gradient(circle at top right, rgba(236, 72, 153, 0.14), transparent 24%),
    linear-gradient(180deg, #edf3ff 0%, #f6f8ff 100%);
}

.aside {
  margin: 18px 0 18px 18px;
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(18px);
  box-shadow: 0 24px 60px rgba(108, 123, 225, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.88);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.brand-panel {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 28px 24px 18px;
}

.brand-mark {
  width: 52px;
  height: 52px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  font-size: 24px;
  font-weight: 800;
  color: #4f46e5;
  background: linear-gradient(135deg, #eef2ff 0%, #dbeafe 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.brand-title {
  font-size: 20px;
  font-weight: 800;
  color: #1f2a44;
  letter-spacing: 0.02em;
}

.brand-subtitle {
  margin-top: 4px;
  color: #94a3b8;
  font-size: 12px;
}

.admin-menu {
  flex: 1;
  border-right: none;
  background: transparent;
  padding: 10px 16px 16px;
}

.admin-menu :deep(.el-menu-item) {
  height: 48px;
  margin-bottom: 10px;
  border-radius: 18px;
  color: #64748b;
  font-weight: 600;
}

.admin-menu :deep(.el-menu-item:hover) {
  color: #4f46e5;
  background: rgba(99, 102, 241, 0.1);
}

.admin-menu :deep(.el-menu-item.is-active) {
  color: #4f46e5;
  background: linear-gradient(135deg, rgba(99, 102, 241, 0.16) 0%, rgba(59, 130, 246, 0.14) 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.7);
}

.aside-footer {
  margin: 0 18px 20px;
  padding: 16px 18px;
  border-radius: 20px;
  background: linear-gradient(135deg, #5b6cff 0%, #7c8dff 100%);
  color: #fff;
  box-shadow: 0 16px 30px rgba(99, 102, 241, 0.25);
}

.aside-footer__label {
  font-size: 12px;
  opacity: 0.82;
}

.aside-footer__value {
  margin-top: 8px;
  font-size: 14px;
  font-weight: 700;
  word-break: break-all;
}

.header {
  margin: 18px 18px 0 18px;
  min-height: 92px;
  display: grid;
  grid-template-columns: minmax(240px, 1fr) minmax(220px, 360px) auto;
  align-items: center;
  gap: 18px;
  padding: 18px 24px;
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(18px);
  border: 1px solid rgba(255, 255, 255, 0.88);
  box-shadow: 0 24px 60px rgba(108, 123, 225, 0.12);
}

.title {
  font-size: 28px;
  line-height: 1.1;
  font-weight: 800;
  color: #2d3a64;
}

.sub-title {
  margin-top: 8px;
  color: #94a3b8;
  font-size: 13px;
}

.header-center {
  display: flex;
  justify-content: center;
}

.header-search-shell {
  width: 100%;
  height: 46px;
  padding: 0 18px;
  border-radius: 999px;
  background: linear-gradient(135deg, #f8faff 0%, #eef2ff 100%);
  border: 1px solid rgba(148, 163, 184, 0.14);
  display: flex;
  align-items: center;
  color: #c0c9df;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.75);
}

.header-search-shell__placeholder {
  font-size: 13px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.header-action-button {
  min-width: 88px;
  height: 40px;
  border: none;
  border-radius: 999px;
  color: #51607a;
  background: linear-gradient(135deg, #f8fbff 0%, #eef3ff 100%);
  box-shadow: 0 14px 28px rgba(148, 163, 184, 0.18);
}

.header-action-button--danger {
  color: #fff;
  background: linear-gradient(135deg, #fb7185 0%, #f43f5e 100%);
  box-shadow: 0 16px 28px rgba(244, 63, 94, 0.28);
}

.main {
  padding: 18px;
  min-height: calc(100vh - 128px);
}

@media (max-width: 1280px) {
  .header {
    grid-template-columns: 1fr;
  }

  .actions,
  .header-center {
    justify-content: flex-start;
  }
}
</style>

<style>
:root {
  color-scheme: light;
}

body {
  margin: 0;
  color: #334155;
  background: linear-gradient(180deg, #edf3ff 0%, #f7f9ff 100%);
  font-family: Inter, "PingFang SC", "Microsoft YaHei", sans-serif;
}

.admin-table .cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.admin-table .caret-wrapper {
  margin-left: 0;
}

* {
  box-sizing: border-box;
}

#app {
  min-height: 100vh;
}

.admin-page-card,
.el-card {
  border: none;
  border-radius: 28px !important;
  background: rgba(255, 255, 255, 0.78) !important;
  backdrop-filter: blur(18px);
  box-shadow: 0 24px 60px rgba(108, 123, 225, 0.12) !important;
}

.admin-page-card .el-card__body,
.el-card .el-card__body {
  padding: 22px;
}

.admin-page-card .el-card__header,
.el-card .el-card__header {
  padding: 20px 22px 0;
  border-bottom: none;
}

.admin-filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
}

.admin-filter-form.el-form--inline {
  display: flex;
  flex-wrap: wrap;
  column-gap: 12px;
  row-gap: 6px;
}

.admin-filter-form.el-form--inline .el-form-item {
  margin-right: 0;
  margin-bottom: 10px;
}

.admin-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.admin-table {
  width: 100%;
}

.admin-table .el-table__inner-wrapper,
.admin-table::before {
  border-radius: 18px;
}

.admin-table th.el-table__cell {
  background: #f5f7ff !important;
  color: #6b7a99;
  font-weight: 700;
}

.admin-table td.el-table__cell,
.admin-table th.el-table__cell {
  padding: 16px 0;
}

.admin-table .el-table__row td.el-table__cell {
  background: rgba(255, 255, 255, 0.7);
}

.admin-table--with-gap {
  margin-top: 16px;
}

.admin-pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.admin-detail {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.admin-detail__title {
  font-size: 18px;
  font-weight: 700;
  color: #2d3a64;
}

.admin-detail__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.admin-detail__item {
  padding: 16px;
  border-radius: 20px;
  background: linear-gradient(135deg, rgba(248, 250, 255, 0.96), rgba(239, 244, 255, 0.92));
}

.admin-detail__item.full {
  grid-column: 1 / -1;
}

.admin-detail__label {
  display: block;
  margin-bottom: 8px;
  font-size: 12px;
  color: #94a3b8;
}

.admin-dialog-form .el-form-item {
  margin-bottom: 18px;
}
</style>
