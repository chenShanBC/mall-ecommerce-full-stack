<template>
  <AdminLayout title="个人中心设置" @refresh="loadProfileData" @logout="handleLogout">
    <div class="profile-page">
      <el-card class="admin-page-card profile-hero">
        <div class="profile-hero__content">
          <div>
            <div class="profile-hero__eyebrow">ADMIN PROFILE</div>
            <div class="profile-hero__title">{{ adminStore.profile?.nickname || adminStore.profile?.account || '管理员' }}</div>
            <div class="profile-hero__meta">账号：{{ adminStore.profile?.account || '-' }} · 角色：{{ adminStore.profile?.roleCode || '-' }}</div>
          </div>
          <div class="profile-hero__badge">{{ (adminStore.permissions || []).length }} 项权限</div>
        </div>
      </el-card>

      <div class="profile-grid">
        <el-card class="admin-page-card">
          <template #header><div class="panel-head"><span>基础资料</span></div></template>
          <el-form :model="profileForm" label-width="90px" class="admin-dialog-form">
            <el-form-item label="账号"><el-input :model-value="adminStore.profile?.account || ''" disabled /></el-form-item>
            <el-form-item label="昵称"><el-input v-model="profileForm.nickname" maxlength="30" /></el-form-item>
            <el-form-item label="角色"><el-input :model-value="adminStore.profile?.roleCode || ''" disabled /></el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="profileSaving" @click="saveProfile">保存资料</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card class="admin-page-card">
          <template #header><div class="panel-head"><span>密码安全</span></div></template>
          <el-form :model="passwordForm" label-width="90px" class="admin-dialog-form">
            <el-form-item label="原密码"><el-input v-model="passwordForm.oldPassword" type="password" show-password /></el-form-item>
            <el-form-item label="新密码"><el-input v-model="passwordForm.newPassword" type="password" show-password /></el-form-item>
            <el-form-item label="确认密码"><el-input v-model="passwordForm.confirmPassword" type="password" show-password /></el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="passwordSaving" @click="savePassword">修改密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </div>

      <el-card class="admin-page-card">
        <template #header><div class="panel-head"><span>当前权限</span></div></template>
        <div class="permission-tags">
          <el-tag v-for="permission in adminStore.permissions" :key="permission" class="permission-tag" size="large">{{ permission }}</el-tag>
          <div v-if="!adminStore.permissions.length" class="permission-empty">暂无权限数据</div>
        </div>
      </el-card>
    </div>
  </AdminLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import AdminLayout from '../components/AdminLayout.vue';
import { changeMyAdminPassword, updateMyAdminProfile } from '../api';
import { useAdminStore } from '../stores/admin';

const router = useRouter();
const adminStore = useAdminStore();
const profileSaving = ref(false);
const passwordSaving = ref(false);
const profileForm = reactive({ nickname: '' });
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' });

const loadProfileData = async () => {
  const profile = await adminStore.refreshProfile();
  profileForm.nickname = profile?.nickname || '';
};

const saveProfile = async () => {
  if (!profileForm.nickname.trim()) {
    return ElMessage.warning('昵称不能为空');
  }
  try {
    profileSaving.value = true;
    const { data } = await updateMyAdminProfile({ nickname: profileForm.nickname.trim() });
    adminStore.applyProfilePatch(data.data || {});
    ElMessage.success('资料已更新');
    await loadProfileData();
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '更新资料失败');
  } finally {
    profileSaving.value = false;
  }
};

const savePassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    return ElMessage.warning('请完整填写密码信息');
  }
  if (passwordForm.newPassword.length < 6) {
    return ElMessage.warning('新密码至少 6 位');
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    return ElMessage.warning('两次输入的新密码不一致');
  }
  try {
    passwordSaving.value = true;
    await changeMyAdminPassword({ oldPassword: passwordForm.oldPassword, newPassword: passwordForm.newPassword });
    ElMessage.success('密码修改成功');
    passwordForm.oldPassword = '';
    passwordForm.newPassword = '';
    passwordForm.confirmPassword = '';
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '修改密码失败');
  } finally {
    passwordSaving.value = false;
  }
};

const handleLogout = async () => {
  await adminStore.logout();
  router.push('/login');
};

onMounted(loadProfileData);
</script>

<style scoped>
.profile-page{display:flex;flex-direction:column;gap:18px}.profile-hero__content{display:flex;justify-content:space-between;align-items:center;gap:16px}.profile-hero__eyebrow{font-size:12px;font-weight:700;color:#7c8db5}.profile-hero__title{margin-top:10px;font-size:30px;font-weight:800;color:#2d3a64}.profile-hero__meta{margin-top:8px;color:#94a3b8}.profile-hero__badge{padding:14px 18px;border-radius:20px;background:linear-gradient(135deg,#5b6cff,#7c8dff);color:#fff;font-weight:800;box-shadow:0 16px 30px rgba(99,102,241,.22)}.profile-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:18px}.panel-head{font-weight:700;color:#2d3a64}.permission-tags{display:flex;flex-wrap:wrap;gap:10px}.permission-tag{border:none!important;padding:0 14px;height:34px;border-radius:999px;background:linear-gradient(135deg,#eef2ff,#e0e7ff);color:#4f46e5}.permission-empty{color:#94a3b8}@media (max-width:960px){.profile-grid{grid-template-columns:1fr}.profile-hero__content{flex-direction:column;align-items:flex-start}}
</style>
