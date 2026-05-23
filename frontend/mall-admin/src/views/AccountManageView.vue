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
        <el-table-column label="权限" min-width="520">
          <template #default="{ row }">
            <el-tooltip
              effect="light"
              placement="top-start"
              popper-class="permission-list-tooltip"
              :content="(row.permissions || []).join('、') || '暂无权限'"
            >
              <div class="permission-list-cell">
                <el-tag v-for="permission in previewPermissions(row.permissions)" :key="permission" size="small" class="tag">{{ permission }}</el-tag>
                <el-tag v-if="hiddenPermissionCount(row.permissions) > 0" size="small" type="info" class="tag">+{{ hiddenPermissionCount(row.permissions) }}</el-tag>
                <span v-if="!(row.permissions || []).length" class="permission-list-empty">暂无权限</span>
              </div>
            </el-tooltip>
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
          <el-select v-model="form.roleCode" style="width: 100%" :disabled="mode === 'edit' && form.roleCode === 'SUPER_ADMIN'" @change="applyTemplate">
            <el-option v-for="item in roles" :key="item.code" :label="`${item.name} (${item.code})`" :value="item.code" />
          </el-select>
          <div v-if="form.roleCode === 'SUPER_ADMIN'" class="role-tip role-tip--warning">超级管理员是系统最高权限角色，后端会强制拥有全部权限，不支持在这里单独减权。</div>
          <div v-else class="role-tip">建议优先按角色模板配置，再按最小权限原则增删；提交后会立刻影响菜单、按钮和接口访问。</div>
        </el-form-item>
        <el-form-item label="权限">
          <div class="permission-editor">
            <div class="permission-section permission-section--base">
              <div class="permission-section__title">默认最小权限（不可修改）</div>
              <div class="permission-section__desc">该角色完成岗位职责所必须具备的权限，提交时后端也会强制保留。</div>
              <div class="permission-tags">
                <el-tag v-for="permission in defaultPermissionItems" :key="permission.code" type="success" class="permission-tag">{{ permission.label }}</el-tag>
                <el-empty v-if="!defaultPermissionItems.length" description="暂无默认权限" :image-size="52" />
              </div>
            </div>
            <div class="permission-section permission-section--optional">
              <div class="permission-section__title">可更改权限（角色白名单剩余部分）</div>
              <div class="permission-section__desc">只能在角色权限上限内增删，不能低于默认权限。</div>
              <el-checkbox-group v-model="optionalPermissions" :disabled="form.roleCode === 'SUPER_ADMIN'">
                <el-checkbox v-for="permission in optionalPermissionItems" :key="permission.code" :label="permission.code">{{ permission.label }}</el-checkbox>
              </el-checkbox-group>
              <el-empty v-if="!optionalPermissionItems.length" description="当前角色没有额外可选权限" :image-size="52" />
            </div>
            <div class="permission-section permission-section--forbidden">
              <div class="permission-section__title">不可添加权限（超出角色白名单）</div>
              <div class="permission-section__desc">这些权限不属于当前角色职责范围，前端不可选，后端也会拒绝提交。</div>
              <div class="permission-tags permission-tags--muted">
                <el-tag v-for="permission in forbiddenPermissionItems" :key="permission.code" type="info" class="permission-tag">{{ permission.label }}</el-tag>
                <el-empty v-if="!forbiddenPermissionItems.length" description="当前角色可拥有全部权限" :image-size="52" />
              </div>
            </div>
          </div>
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
import { computed, onMounted, reactive, ref } from 'vue';
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
const form = reactive({ userId: null, username: '', password: '', nickname: '', roleCode: 'PRODUCT_OPERATOR', permissions: ['product:view', 'product:create', 'product:update'] });
const permissionOptions = [
  { code: 'dashboard:view', label: '仪表盘查看' },
  { code: 'user:view', label: '用户查看' },
  { code: 'user:edit', label: '用户编辑' },
  { code: 'user:disable', label: '用户禁启用' },
  { code: 'product:view', label: '商品查看' },
  { code: 'product:create', label: '商品新增' },
  { code: 'product:update', label: '商品编辑' },
  { code: 'product:on_sale', label: '商品上架' },
  { code: 'product:off_sale', label: '商品下架' },
  { code: 'category:manage', label: '类目管理' },
  { code: 'stock:view', label: '库存查看' },
  { code: 'stock:log:view', label: '库存日志' },
  { code: 'stock:adjust', label: '库存调整' },
  { code: 'order:view', label: '订单查看' },
  { code: 'order:remark', label: '订单备注' },
  { code: 'order:ship', label: '订单发货' },
  { code: 'order:close', label: '订单关闭' },
  { code: 'order:log:view', label: '订单日志' },
  { code: 'aftersale:view', label: '售后查看' },
  { code: 'aftersale:audit', label: '售后审核' },
  { code: 'refund:view', label: '退款查看' },
  { code: 'refund:execute', label: '退款/支付高危处理' },
  { code: 'finance:view', label: '财务查看' },
  { code: 'payment:view', label: '支付查看' },
  { code: 'reconciliation:view', label: '对账查看' },
  { code: 'reconciliation:handle', label: '对账处理' },
  { code: 'admin:view', label: '账号查看' },
  { code: 'admin:create', label: '账号新增' },
  { code: 'admin:update', label: '账号编辑' },
  { code: 'admin:disable', label: '账号禁启用' },
  { code: 'role:view', label: '角色查看' },
  { code: 'role:manage', label: '角色管理' },
  { code: 'permission:view', label: '权限查看' },
  { code: 'permission:assign', label: '权限分配' },
  { code: 'log:operation:view', label: '操作日志查看' },
];
const permissionOptionMap = permissionOptions.reduce((acc, item) => { acc[item.code] = item; return acc; }, {});
const accountStatusMeta = (status) => getStatusTagMeta('adminAccountStatus', status);
const PERMISSION_PREVIEW_COUNT = 8;
const previewPermissions = (permissions = []) => permissions.slice(0, PERMISSION_PREVIEW_COUNT);
const hiddenPermissionCount = (permissions = []) => Math.max(permissions.length - PERMISSION_PREVIEW_COUNT, 0);

const syncCurrentAdminPermissionState = async (adminId, roleCode, permissions) => {
  if (String(adminStore.adminId || '') !== String(adminId || '')) {
    return;
  }
  adminStore.applyProfilePatch({ roleCode, permissions: [...permissions] });
  await adminStore.refreshProfile().catch(() => null);
  if (!adminStore.hasAnyPermission(['admin:view', 'role:view', 'permission:view'])) {
    router.replace(adminStore.getFirstAccessiblePath());
    ElMessage.warning('当前账号权限已变更，已自动跳转到可访问页面');
  }
};

const getRoleDefaultPermissions = (roleCode) => {
  const defaults = roleMap.value[roleCode]?.defaultPermissions || [];
  return [...defaults];
};
const getRolePermissionScope = (roleCode) => {
  const scope = roleMap.value[roleCode]?.permissionScope || roleMap.value[roleCode]?.defaultPermissions || [];
  return [...scope];
};
const toPermissionItems = (permissions) => permissions.map((code) => permissionOptionMap[code] || { code, label: code });
const currentDefaultPermissions = computed(() => getRoleDefaultPermissions(form.roleCode));
const currentPermissionScope = computed(() => getRolePermissionScope(form.roleCode));
const defaultPermissionItems = computed(() => toPermissionItems(currentDefaultPermissions.value));
const optionalPermissionItems = computed(() => toPermissionItems(currentPermissionScope.value.filter((permission) => !currentDefaultPermissions.value.includes(permission))));
const forbiddenPermissionItems = computed(() => toPermissionItems(permissionOptions.map((item) => item.code).filter((permission) => !currentPermissionScope.value.includes(permission))));
const optionalPermissions = computed({
  get: () => form.permissions.filter((permission) => !currentDefaultPermissions.value.includes(permission) && currentPermissionScope.value.includes(permission)),
  set: (permissions) => {
    form.permissions = [...new Set([...currentDefaultPermissions.value, ...permissions])];
  },
});
const normalizeFormPermissions = () => {
  form.permissions = [...new Set(form.permissions.filter((permission) => currentPermissionScope.value.includes(permission)).concat(currentDefaultPermissions.value))];
};
const resetForm = () => Object.assign(form, { userId: null, username: '', password: '', nickname: '', roleCode: 'PRODUCT_OPERATOR', permissions: getRoleDefaultPermissions('PRODUCT_OPERATOR').length ? getRoleDefaultPermissions('PRODUCT_OPERATOR') : ['product:view', 'product:create', 'product:update'] });
const applyTemplate = () => {
  const permissions = getRoleDefaultPermissions(form.roleCode);
  form.permissions = permissions.length ? permissions : ['product:view'];
};
const openCreate = () => { mode.value = 'create'; currentAdminId.value = null; resetForm(); normalizeFormPermissions(); dialogVisible.value = true; };
const openEdit = (row) => { mode.value = 'edit'; currentAdminId.value = row.id; Object.assign(form, { userId: row.userId, username: row.username, password: '', nickname: row.nickname, roleCode: row.roleCode, permissions: row.roleCode === 'SUPER_ADMIN' ? getRoleDefaultPermissions('SUPER_ADMIN') : [...(row.permissions || [])] }); normalizeFormPermissions(); dialogVisible.value = true; };

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
      if (form.roleCode === 'SUPER_ADMIN') {
        ElMessage.info('超级管理员始终拥有全部权限，无需单独编辑');
        dialogVisible.value = false;
        return;
      }
      normalizeFormPermissions();
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
    if (row.roleCode === 'SUPER_ADMIN') {
      return ElMessage.info('超级管理员始终拥有全部权限，无需重置');
    }
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
.permission-list-cell {
  width: 100%;
  max-height: 58px;
  overflow: hidden;
  white-space: normal;
  line-height: 28px;
  cursor: help;
}
.permission-list-cell .tag {
  margin-bottom: 0;
  vertical-align: middle;
}
.permission-list-empty {
  color: #94a3b8;
  font-size: 12px;
}
.header-actions { display: flex; gap: 10px; }
.role-tip { margin-top: 6px; color: #909399; font-size: 12px; line-height: 1.5; }
.role-tip--warning { color: #e6a23c; }
.permission-editor { width: 100%; display: grid; gap: 14px; }
.permission-section { padding: 14px 16px; border-radius: 14px; border: 1px solid #e5e7eb; background: #fff; }
.permission-section--base { background: #f0fdf4; border-color: #bbf7d0; }
.permission-section--optional { background: #eff6ff; border-color: #bfdbfe; }
.permission-section--forbidden { background: #f8fafc; border-color: #e2e8f0; }
.permission-section__title { font-weight: 700; color: #1f2937; }
.permission-section__desc { margin: 6px 0 10px; font-size: 12px; color: #64748b; line-height: 1.5; }
.permission-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.permission-tags--muted { opacity: 0.78; }
.permission-tag { margin: 0; }
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

<style>
.permission-list-tooltip {
  max-width: 520px;
  line-height: 1.8;
  word-break: break-all;
}
</style>
