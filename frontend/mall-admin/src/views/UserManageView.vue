<template>
  <AdminLayout title="用户管理" @refresh="loadData" @logout="handleLogout">
    <el-card class="admin-page-card">
      <div class="admin-filter-bar">
        <el-input v-model="query.keyword" placeholder="手机号 / 昵称" clearable style="width: 280px" @keyup.enter="handleSearch" />
        <el-select v-model="query.status" clearable placeholder="用户状态" style="width: 180px">
          <el-option label="启用" value="ENABLED" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button @click="exportUsers">导出</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" class="admin-table admin-table--with-gap" empty-text="暂无用户数据" @sort-change="handleSortChange">
        <el-table-column prop="id" label="用户ID" width="110" sortable="custom" />
        <el-table-column prop="mobile" label="手机号" width="150" sortable="custom" />
        <el-table-column prop="nickname" label="昵称" width="140" sortable="custom" />
        <el-table-column prop="status" label="状态" width="120" sortable="custom">
          <template #default="{ row }"><el-tag :type="userStatusMeta(row.status).type">{{ userStatusMeta(row.status).label }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="addressCount" label="地址数" width="90" />
        <el-table-column label="默认收货信息" min-width="300">
          <template #default="{ row }">
            <div class="user-address-cell">
              <div>{{ row.defaultReceiverName }} / {{ row.defaultReceiverPhone }}</div>
              <div class="user-address-cell__detail">{{ row.defaultAddress }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="标签" min-width="180">
          <template #default="{ row }">
            <el-tag
              v-for="tag in row.tags || []"
              :key="tag"
              class="tag"
              :class="userTagClass(tag)"
              size="small"
              effect="light"
            >
              {{ tag }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <div class="user-actions">
              <el-button class="user-action-btn user-action-btn--detail" size="small" @click="openDetail(row.id)">详情</el-button>
              <el-button v-if="canManage && row.status === 'ENABLED'" class="user-action-btn user-action-btn--danger" size="small" @click="toggleStatus(row, false)">禁用</el-button>
              <el-button v-if="canManage && row.status !== 'ENABLED'" class="user-action-btn user-action-btn--success" size="small" @click="toggleStatus(row, true)">启用</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="admin-pagination">
        <el-pagination background layout="sizes, prev, pager, next, total" :current-page="pager.page" :page-size="pager.size" :page-sizes="ADMIN_PAGE_SIZES" :total="pager.total" @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="用户详情" width="820px">
      <div v-if="detail" class="admin-detail">
        <div class="admin-detail__title">{{ detail.nickname || '-' }}</div>
        <div class="admin-detail__grid user-detail-grid">
          <div v-if="detail.avatarUrl" class="admin-detail__item full user-avatar-panel">
            <span class="admin-detail__label">头像</span>
            <el-image :src="resolveAssetUrl(detail.avatarUrl)" fit="cover" class="user-avatar-image" :preview-src-list="[resolveAssetUrl(detail.avatarUrl)]" preview-teleported />
          </div>
          <div class="admin-detail__item"><span class="admin-detail__label">用户ID</span>{{ detail.id }}</div>
          <div class="admin-detail__item"><span class="admin-detail__label">手机号</span>{{ detail.mobile }}</div>
          <div class="admin-detail__item"><span class="admin-detail__label">状态</span><el-tag :type="userStatusMeta(detail.status).type">{{ userStatusMeta(detail.status).label }}</el-tag></div>
        </div>
        <el-table :data="detail.addresses || []" class="admin-table">
          <el-table-column prop="receiverName" label="收货人" width="120" />
          <el-table-column prop="receiverPhone" label="手机号" width="140" />
          <el-table-column prop="fullAddress" label="地址" min-width="320" />
          <el-table-column prop="isDefault" label="默认地址" width="100">
            <template #default="{ row }"><el-tag :type="row.isDefault ? 'success' : 'info'">{{ row.isDefault ? '是' : '否' }}</el-tag></template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </AdminLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import AdminLayout from '../components/AdminLayout.vue';
import { disableAdminUser, enableAdminUser, fetchAdminUserDetail, fetchAdminUsers } from '../api';
import { useAdminStore } from '../stores/admin';
import { confirmAction } from '../utils/action';
import { exportRowsToCsv } from '../utils/export';
import { ADMIN_PAGE_SIZE, ADMIN_PAGE_SIZES } from '../utils/pagination';
import { getStatusTagMeta } from '../utils/status';

const router = useRouter();
const adminStore = useAdminStore();
const loading = ref(false);
const rows = ref([]);
const detail = ref(null);
const detailVisible = ref(false);
const query = reactive({ keyword: '', status: '', sortBy: 'id', sortOrder: 'asc' });
const pager = reactive({ page: 1, size: ADMIN_PAGE_SIZE, total: 0 });
const canManage = computed(() => adminStore.hasPermission('user:manage'));
const userStatusMeta = (status) => getStatusTagMeta('userAccountStatus', status);
const userTagClass = (tag) => {
  if (tag === '有地址') return 'tag--address-yes';
  if (tag === '无地址') return 'tag--address-no';
  if (tag === '正常用户') return 'tag--status-enabled';
  if (tag === '已禁用') return 'tag--status-disabled';
  return '';
};
const getBackendOrigin = () => {
  const explicitTarget = import.meta.env.VITE_DEV_API_TARGET;
  if (explicitTarget && /^https?:\/\//i.test(explicitTarget)) {
    return explicitTarget.replace(/\/$/, '');
  }
  const apiBase = import.meta.env.VITE_API_BASE_URL || '/api';
  if (/^https?:\/\//i.test(apiBase)) {
    return apiBase.replace(/\/$/, '').replace(/\/api$/, '');
  }
  return window.location.origin;
};
const resolveAssetUrl = (url) => {
  if (!url) return '';
  if (/^(https?:)?\/\//i.test(url) || url.startsWith('data:')) return url;
  const normalized = String(url).replace(/^\/upload\//i, '/uploads/');
  const backendOrigin = getBackendOrigin();
  if (normalized.startsWith('/')) {
    return `${backendOrigin}${normalized}`;
  }
  return `${backendOrigin}/${normalized}`;
};

const loadData = async () => {
  loading.value = true;
  try {
    const { data } = await fetchAdminUsers({ ...query, page: pager.page, size: pager.size });
    rows.value = data.data?.records || [];
    pager.total = data.data?.total || 0;
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '加载用户失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = async () => { pager.page = 1; await loadData(); };
const handleReset = async () => { query.keyword = ''; query.status = ''; query.sortBy = 'id'; query.sortOrder = 'asc'; pager.page = 1; pager.size = ADMIN_PAGE_SIZE; await loadData(); };
const handlePageChange = async (page) => { pager.page = page; await loadData(); };
const handleSizeChange = async (size) => { pager.size = size; pager.page = 1; await loadData(); };
const handleSortChange = async ({ prop, order }) => { query.sortBy = prop || 'id'; query.sortOrder = order === 'descending' ? 'desc' : 'asc'; pager.page = 1; await loadData(); };
const openDetail = async (userId) => { const { data } = await fetchAdminUserDetail(userId); detail.value = data.data; detailVisible.value = true; };

const toggleStatus = async (row, enable) => {
  try {
    await confirmAction(`确认${enable ? '启用' : '禁用'}用户“${row.mobile}”吗？`);
    if (enable) await enableAdminUser(row.id); else await disableAdminUser(row.id);
    ElMessage.success(enable ? '用户已启用' : '用户已禁用');
    await loadData();
    if (detailVisible.value && String(detail.value?.id || '') === String(row.id)) {
      await openDetail(row.id);
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error(error?.response?.data?.msg || '操作失败');
  }
};

const exportUsers = () => {
  exportRowsToCsv('C端用户列表', rows.value, [
    { label: '用户ID', value: 'id' },
    { label: '手机号', value: 'mobile' },
    { label: '昵称', value: 'nickname' },
    { label: '状态', value: 'status' },
    { label: '地址数', value: 'addressCount' },
    { label: '默认收货人', value: 'defaultReceiverName' },
    { label: '默认手机号', value: 'defaultReceiverPhone' },
    { label: '默认地址', value: 'defaultAddress' },
  ]);
};

const handleLogout = async () => { await adminStore.logout(); router.push('/login'); };

onMounted(loadData);
</script>

<style scoped>
.tag {
  height: 24px;
  margin-right: 6px;
  margin-bottom: 6px;
  padding: 0 9px;
  border-radius: 4px;
  font-weight: 500;
  line-height: 22px;
}

.tag--address-yes {
  color: #047857;
  background: #ecfdf5;
  border-color: #a7f3d0;
}

.tag--address-no {
  color: #64748b;
  background: #f8fafc;
  border-color: #cbd5e1;
}

.tag--status-enabled {
  color: #2563eb;
  background: #eff6ff;
  border-color: #bfdbfe;
}

.tag--status-disabled {
  color: #be123c;
  background: #fff1f2;
  border-color: #fecdd3;
}

.user-address-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-address-cell__detail { color: #94a3b8; }

.user-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.user-action-btn {
  min-width: 72px;
  border: none;
  border-radius: 999px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}

.user-action-btn--detail { color: #2563eb; background: linear-gradient(135deg, #eff6ff, #dbeafe); }
.user-action-btn--danger { color: #be123c; background: linear-gradient(135deg, #fff1f2, #ffe4e6); }
.user-action-btn--success { color: #047857; background: linear-gradient(135deg, #ecfdf5, #d1fae5); }

.user-avatar-panel {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
}

.user-avatar-image {
  width: 84px;
  height: 84px;
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}

:deep(.el-table .cell) {
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

:deep(.el-table th.is-sortable .cell) { justify-content: flex-start; }
</style>
