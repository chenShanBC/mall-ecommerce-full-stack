<template>
  <AdminLayout title="账号权限管理" @refresh="loadData" @logout="handleLogout">
    <el-card class="admin-page-card">
      <template #header>
        <div class="admin-toolbar">
          <span>运营账号</span>
          <div class="header-actions">
            <el-button class="toolbar-button" @click="exportAccounts">导出账号</el-button>
            <el-button class="toolbar-button toolbar-button--primary" type="primary" @click="openCreate">新建账号</el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" :model="filters" class="admin-filter-form">
        <el-form-item label="关键词"><el-input v-model="filters.keyword" clearable placeholder="账号 / 昵称 / 角色" style="width: 240px" @keyup.enter="handleSearch" /></el-form-item>
        <el-form-item label="角色"><el-select v-model="filters.roleCode" clearable placeholder="全部角色" style="width: 180px"><el-option v-for="item in roles" :key="item.code" :label="item.name" :value="item.code" /></el-select></el-form-item>
        <el-form-item label="状态"><el-select v-model="filters.status" clearable placeholder="全部状态" style="width: 160px"><el-option label="启用" value="ENABLED" /><el-option label="禁用" value="DISABLED" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" @click="handleSearch">查询</el-button></el-form-item>
        <el-form-item><el-button @click="handleReset">重置</el-button></el-form-item>
      </el-form>

      <el-table :data="accounts" class="admin-table admin-table--with-gap" empty-text="暂无运营账号数据">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="账号" width="140" />
        <el-table-column prop="nickname" label="昵称" width="140" />
        <el-table-column prop="roleCode" label="角色" width="160" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }"><el-tag :type="accountStatusMeta(row.status).type">{{ accountStatusMeta(row.status).label }}</el-tag></template>
        </el-table-column>
        <el-table-column label="权限" min-width="300">
          <template #default="{ row }">
            <el-tag v-for="permission in row.permissions || []" :key="permission" size="small" class="tag">{{ permission }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="360" align="center">
          <template #default="{ row }">
            <div class="account-actions">
              <el-button class="action-button action-button--edit" size="small" @click="openEdit(row)">编辑权限</el-button>
              <el-button class="action-button action-button--reset" size="small" @click="resetPermissionsByRole(row)">权限重置</el-button>
              <el-button v-if="row.status === 'ENABLED'" class="action-button action-button--disable" size="small" type="warning" @click="toggleStatus(row, false)">禁用账号</el-button>
              <el-button v-else class="action-button action-button--enable" size="small" type="success" @click="toggleStatus(row, true)">启用账号</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="admin-pagination">
        <el-pagination background layout="sizes, prev, pager, next, total" :current-page="pager.page" :page-size="pager.size" :page-sizes="ADMIN_PAGE_SIZES" :total="pager.total" @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="mode === 'create' ? '新建运营账号' : '编辑账号权限'" width="720px">
      <el-form :model="form" label-width="100px" class="admin-dialog-form">
        <el-form-item v-if="mode === 'create'" label="关联用户ID"><el-input v-model.number="form.userId" placeholder="可选" /></el-form-item>
        <el-form-item v-if="mode === 'create'" label="账号"><el-input v-model="form.username" /></el-form-item>
        <el-form-item v-if="mode === 'create'" label="密码"><el-input v-model="form.password" type="password" show-password /></el-form-item>
        <el-form-item v-if="mode === 'create'" label="昵称"><el-input v-model="form.nickname" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleCode" style="width: 100%" @change="applyTemplate">
            <el-option v-for="item in roles" :key="item.code" :label="`${item.name} (${item.code})`" :value="item.code" />
          </el-select>
          <div class="role-tip">建议优先按角色模板配置，再按需做最小权限增删。</div>
        </el-form-item>
        <el-form-item label="权限">
          <el-checkbox-group v-model="form.permissions">
            <el-checkbox label="user:view">用户查看</el-checkbox>
            <el-checkbox label="user:manage">用户管理</el-checkbox>
            <el-checkbox label="product:view">商品查看</el-checkbox>
            <el-checkbox label="product:manage">商品编辑</el-checkbox>
            <el-checkbox label="order:view">订单查看</el-checkbox>
            <el-checkbox label="order:manage">订单处理</el-checkbox>
            <el-checkbox label="stock:view">库存查看</el-checkbox>
            <el-checkbox label="stock:manage">库存处理</el-checkbox>
            <el-checkbox label="pay:view">支付查看</el-checkbox>
            <el-checkbox label="pay:manage">支付处理</el-checkbox>
            <el-checkbox label="reconcile:view">对账查看</el-checkbox>
            <el-checkbox label="reconcile:manage">对账处理</el-checkbox>
            <el-checkbox label="system:log:view">日志查看</el-checkbox>
            <el-checkbox label="system:account:manage">账号管理</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">提交</el-button>
      </template>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import AdminLayout from '../components/AdminLayout.vue';
import { confirmAction } from '../utils/action';
import { getStatusTagMeta } from '../utils/status';
import { createAdminAccount, disableAdminAccount, enableAdminAccount, fetchAdminAccounts, fetchAdminRoles, updateAdminAccountPermissions } from '../api';
import { exportRowsToCsv } from '../utils/export';
import { useAdminStore } from '../stores/admin';
import { ADMIN_PAGE_SIZE, ADMIN_PAGE_SIZES } from '../utils/pagination';

const router = useRouter();
const adminStore = useAdminStore();
const accounts = ref([]);
const roles = ref([]);
const roleMap = ref({});
const dialogVisible = ref(false);
const mode = ref('create');
const currentAdminId = ref(null);
const filters = reactive({ keyword: '', roleCode: '', status: '' });
const pager = reactive({ page: 1, size: ADMIN_PAGE_SIZE, total: 0 });
const form = reactive({ userId: null, username: '', password: '', nickname: '', roleCode: 'PRODUCT_OPERATOR', permissions: ['product:view', 'product:manage'] });
const accountStatusMeta = (status) => getStatusTagMeta('adminAccountStatus', status);

const syncCurrentAdminPermissionState = async (adminId, roleCode, permissions) => {
  if (String(adminStore.adminId || '') !== String(adminId || '')) {
    return;
  }
  adminStore.applyProfilePatch({ roleCode, permissions: [...permissions] });
  await adminStore.refreshProfile().catch(() => null);
  if (!adminStore.hasPermission('system:account:manage')) {
    router.replace(adminStore.getFirstAccessiblePath());
    ElMessage.warning('当前账号权限已变更，已自动跳转到可访问页面');
  }
};

const getRoleDefaultPermissions = (roleCode) => {
  const defaults = roleMap.value[roleCode]?.defaultPermissions || [];
  return [...defaults];
};
const resetForm = () => Object.assign(form, { userId: null, username: '', password: '', nickname: '', roleCode: 'PRODUCT_OPERATOR', permissions: getRoleDefaultPermissions('PRODUCT_OPERATOR').length ? getRoleDefaultPermissions('PRODUCT_OPERATOR') : ['product:view', 'product:manage'] });
const applyTemplate = () => {
  const permissions = getRoleDefaultPermissions(form.roleCode);
  form.permissions = permissions.length ? permissions : ['product:view'];
};
const openCreate = () => { mode.value = 'create'; currentAdminId.value = null; resetForm(); dialogVisible.value = true; };
const openEdit = (row) => { mode.value = 'edit'; currentAdminId.value = row.id; Object.assign(form, { userId: row.userId, username: row.username, password: '', nickname: row.nickname, roleCode: row.roleCode, permissions: [...(row.permissions || [])] }); dialogVisible.value = true; };

const loadData = async () => {
  try {
    const [accountRes, roleRes] = await Promise.all([
      fetchAdminAccounts({ ...filters, page: pager.page, size: pager.size }),
      fetchAdminRoles(),
    ]);
    accounts.value = accountRes.data.data?.records || [];
    pager.total = accountRes.data.data?.total || 0;
    roles.value = roleRes.data.data || [];
    roleMap.value = roles.value.reduce((acc, item) => { acc[item.code] = item; return acc; }, {});
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '加载账号失败');
  }
};

const handleSearch = async () => { pager.page = 1; await loadData(); };
const handleReset = async () => { filters.keyword = ''; filters.roleCode = ''; filters.status = ''; pager.page = 1; pager.size = ADMIN_PAGE_SIZE; await loadData(); };
const handlePageChange = async (page) => { pager.page = page; await loadData(); };
const handleSizeChange = async (size) => { pager.size = size; pager.page = 1; await loadData(); };

const exportAccounts = () => {
  exportRowsToCsv('运营账号列表', accounts.value, [
    { label: 'ID', value: 'id' },
    { label: '账号', value: 'username' },
    { label: '昵称', value: 'nickname' },
    { label: '角色', value: 'roleCode' },
    { label: '状态', value: 'status' },
    { label: '权限', value: (row) => (row.permissions || []).join(' | ') },
  ]);
};

const submit = async () => {
  try {
    if (mode.value === 'create') {
      await createAdminAccount({ ...form, userId: form.userId || null });
      ElMessage.success('创建成功');
    } else {
      await updateAdminAccountPermissions(currentAdminId.value, { roleCode: form.roleCode, permissions: form.permissions });
      await syncCurrentAdminPermissionState(currentAdminId.value, form.roleCode, form.permissions);
      ElMessage.success('更新成功');
    }
    dialogVisible.value = false;
    await loadData();
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '提交失败');
  }
};

const resetPermissionsByRole = async (row) => {
  try {
    await confirmAction(`确认将账号“${row.username}”的权限重置为当前角色默认配置吗？`);
    const permissions = getRoleDefaultPermissions(row.roleCode);
    if (!permissions.length) {
      return ElMessage.warning('当前角色未配置默认权限模板');
    }
    await updateAdminAccountPermissions(row.id, { roleCode: row.roleCode, permissions });
    await syncCurrentAdminPermissionState(row.id, row.roleCode, permissions);
    ElMessage.success('权限已重置为角色默认配置');
    await loadData();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.response?.data?.msg || '权限重置失败');
  }
};

const toggleStatus = async (row, enable) => {
  try {
    await confirmAction(`确认${enable ? '启用' : '禁用'}账号“${row.username}”吗？`);
    if (enable) await enableAdminAccount(row.id); else await disableAdminAccount(row.id);
    ElMessage.success(enable ? '已启用' : '已禁用');
    await loadData();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.response?.data?.msg || '操作失败');
  }
};

const handleLogout = async () => { await adminStore.logout(); router.push('/login'); };
onMounted(loadData);
</script>

<style scoped>
.tag { margin-right: 6px; margin-bottom: 6px; }
.header-actions { display: flex; gap: 10px; }
.role-tip { margin-top: 6px; color: #909399; font-size: 12px; line-height: 1.5; }
.toolbar-button { min-width: 96px; }
.toolbar-button--primary { box-shadow: 0 8px 20px rgba(64, 158, 255, 0.22); }
.account-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}
.action-button {
  min-width: 92px;
  margin: 0;
  border-radius: 999px;
  border-color: transparent;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.08);
}
.action-button--edit {
  color: #2563eb;
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
}
.action-button--reset {
  color: #7c3aed;
  background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
}
.action-button--disable {
  color: #b45309;
  background: linear-gradient(135deg, #fff7ed 0%, #ffedd5 100%);
}
.action-button--enable {
  color: #047857;
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
}
.action-button:hover,
.action-button:focus {
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.12);
}
</style>
