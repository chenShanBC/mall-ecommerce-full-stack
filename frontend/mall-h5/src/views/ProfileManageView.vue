<template>
  <div class="page">
    <van-nav-bar class="profile-back-card" title="个人信息管理" left-arrow @click-left="handleBack" />

    <div class="section-card">
      <div class="section-title">个人资料</div>
      <div class="avatar-panel">
        <img class="avatar-preview" :src="avatarPreview" alt="avatar" />
        <van-uploader
          :after-read="handleAvatarUpload"
          :max-count="1"
          :preview-image="false"
          accept="image/png,image/jpeg,image/webp"
        >
          <van-button round plain type="primary" size="small" :loading="uploadingAvatar">上传头像</van-button>
        </van-uploader>
        <div class="upload-tip">支持 jpg、jpeg、png、webp，大小不超过 2MB。</div>
      </div>
      <van-form @submit="handleUpdateProfile">
        <van-cell-group inset>
          <van-field v-model.trim="profileForm.nickname" label="昵称" placeholder="请输入昵称" maxlength="20" />
          <van-field :model-value="userStore.maskedMobile" label="绑定手机号" readonly />
        </van-cell-group>
        <div class="tip">当前阶段手机号仅支持展示，换绑功能后续单独开放。</div>
        <div class="actions">
          <van-button round block type="primary" native-type="submit" :loading="savingProfile">保存资料</van-button>
        </div>
      </van-form>
    </div>

    <div class="section-card">
      <div class="section-title">修改密码</div>
      <van-form @submit="handleChangePassword">
        <van-cell-group inset>
          <van-field v-model="passwordForm.oldPassword" label="原密码" type="password" placeholder="请输入原密码" />
          <van-field v-model="passwordForm.newPassword" label="新密码" type="password" placeholder="请输入6-20位新密码" />
          <van-field v-model="passwordForm.confirmPassword" label="确认密码" type="password" placeholder="请再次输入新密码" />
        </van-cell-group>
        <div class="actions">
          <van-button round block type="primary" native-type="submit" :loading="savingPassword">修改密码</van-button>
        </div>
      </van-form>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { showConfirmDialog, showFailToast, showLoadingToast, showSuccessToast } from 'vant';
import { useRouter } from 'vue-router';
import { uploadAvatar } from '../api';
import { useUserStore } from '../stores/user';

const router = useRouter();
const userStore = useUserStore();

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '');
const backendOrigin = (() => {
  const devApiTarget = (import.meta.env.VITE_DEV_API_TARGET || '').replace(/\/$/, '');
  if (devApiTarget) {
    return devApiTarget;
  }
  if (/^https?:\/\//i.test(apiBaseUrl)) {
    return apiBaseUrl.replace(/\/api$/, '');
  }
  return window.location.origin;
})();

const savingProfile = ref(false);
const savingPassword = ref(false);
const uploadingAvatar = ref(false);
const profileForm = reactive({
  nickname: '',
  avatarUrl: '',
});
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

const defaultAvatarSvg = "data:image/svg+xml;utf8," + encodeURIComponent(`
  <svg xmlns='http://www.w3.org/2000/svg' width='120' height='120' viewBox='0 0 120 120'>
    <defs>
      <linearGradient id='g' x1='0' y1='0' x2='1' y2='1'>
        <stop offset='0%' stop-color='#e0e7ff'/>
        <stop offset='100%' stop-color='#c7d2fe'/>
      </linearGradient>
    </defs>
    <circle cx='60' cy='60' r='60' fill='url(#g)'/>
    <circle cx='60' cy='48' r='20' fill='#94a3b8'/>
    <rect x='28' y='74' width='64' height='30' rx='15' fill='#94a3b8'/>
  </svg>
`);

const normalizeAvatarPath = (url) => {
  if (!url) return url;
  const normalizedUrl = String(url).trim().replace(/^\/upload\//i, '/uploads/');
  if (/^https:\/\/img\.mallfei\.local\//i.test(normalizedUrl)) {
    return '';
  }
  return normalizedUrl;
};

const buildAvatarPathFromUploadResult = (fileInfo = {}) => {
  const explicitUrl = fileInfo.url || fileInfo.fileUrl || fileInfo.accessUrl || fileInfo.path;
  if (explicitUrl) {
    return normalizeAvatarPath(explicitUrl);
  }
  const fileName = fileInfo.fileName || fileInfo.filename;
  if (!fileName) {
    return '';
  }
  const normalizedFileName = String(fileName).replace(/^\/+/, '');
  if (normalizedFileName.startsWith('uploads/') || normalizedFileName.startsWith('upload/')) {
    return normalizeAvatarPath(`/${normalizedFileName}`);
  }
  return `/uploads/avatar/${normalizedFileName}`;
};

const resolveAvatarUrl = (url) => {
  const normalizedUrl = normalizeAvatarPath(url);
  if (!normalizedUrl) {
    return defaultAvatarSvg;
  }
  if (/^https?:\/\//i.test(normalizedUrl)) {
    return normalizedUrl;
  }
  if (normalizedUrl.startsWith('/')) {
    return `${backendOrigin}${normalizedUrl}`;
  }
  return `${backendOrigin}/${normalizedUrl}`;
};

const avatarPreview = computed(() => resolveAvatarUrl(profileForm.avatarUrl));

const syncForm = () => {
  profileForm.nickname = userStore.profile?.nickname || '';
  profileForm.avatarUrl = userStore.profile?.avatarUrl || '';
};

const resetPasswordForm = () => {
  passwordForm.oldPassword = '';
  passwordForm.newPassword = '';
  passwordForm.confirmPassword = '';
};

const handleBack = () => {
  if (window.history.length > 1) {
    router.back();
    return;
  }
  router.replace('/profile');
};

const getProfileErrorMessage = (error, fallbackMessage) => {
  const message = error?.response?.data?.msg || fallbackMessage;
  if (message?.includes('昵称已被其他用户使用')) {
    return '昵称已存在，请换一个昵称后再试';
  }
  return message;
};

const persistProfile = async (successMessage) => {
  await userStore.updateProfile({
    nickname: profileForm.nickname,
    avatarUrl: profileForm.avatarUrl,
  });
  syncForm();
  if (successMessage) {
    showSuccessToast(successMessage);
  }
};

const handleAvatarUpload = async (fileItem) => {
  const file = fileItem?.file;
  if (!file) {
    showFailToast('未选择头像文件');
    return;
  }
  const loadingToast = showLoadingToast({
    message: '头像上传中...',
    forbidClick: true,
    duration: 0,
  });
  try {
    uploadingAvatar.value = true;
    const { data } = await uploadAvatar(file);
    const uploadedAvatarUrl = buildAvatarPathFromUploadResult(data?.data);
    if (!uploadedAvatarUrl) {
      throw new Error('上传成功，但未返回可用的头像地址');
    }
    profileForm.avatarUrl = uploadedAvatarUrl;
    await persistProfile();
    await userStore.loadProfile(true);
    syncForm();
    showSuccessToast('头像上传并保存成功');
  } catch (error) {
    showFailToast(error?.response?.data?.msg || '头像上传失败');
  } finally {
    uploadingAvatar.value = false;
    loadingToast.close();
  }
};

const handleUpdateProfile = async () => {
  try {
    savingProfile.value = true;
    await persistProfile('个人资料已保存');
  } catch (error) {
    showFailToast(getProfileErrorMessage(error, '个人资料保存失败'));
  } finally {
    savingProfile.value = false;
  }
};

const handleChangePassword = async () => {
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    showFailToast('两次输入的新密码不一致');
    return;
  }
  try {
    savingPassword.value = true;
    await userStore.changePassword({ ...passwordForm });
    await showConfirmDialog({
      title: '密码修改成功',
      message: '请使用新密码重新登录',
      confirmButtonText: '去登录',
      showCancelButton: false,
    });
    userStore.clearSession();
    router.replace('/login');
  } catch (error) {
    showFailToast(error?.response?.data?.msg || '密码修改失败');
  } finally {
    savingPassword.value = false;
    resetPasswordForm();
  }
};

onMounted(async () => {
  try {
    await userStore.loadProfile();
    syncForm();
  } catch (error) {
    showFailToast(error?.response?.data?.msg || '用户信息加载失败');
  }
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 12px 12px 88px;
  background: #f6f8fb;
}

.profile-back-card {
  margin-bottom: 12px;
  overflow: hidden;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.88);
  border: 1px solid rgba(255, 255, 255, 0.9);
  box-shadow: 0 16px 40px rgba(108, 123, 225, 0.12);
  backdrop-filter: blur(18px);
}

.profile-back-card::after {
  display: none;
}

.profile-back-card :deep(.van-nav-bar__content) {
  height: 56px;
}

.profile-back-card :deep(.van-nav-bar__left) {
  padding-left: 14px;
}

.profile-back-card :deep(.van-icon-arrow-left) {
  color: #5b6cff;
  font-size: 20px;
}

.profile-back-card :deep(.van-nav-bar__title) {
  color: #2d3a64;
  font-size: 17px;
  font-weight: 800;
}

.section-card {
  margin-bottom: 12px;
  overflow: hidden;
  background: #fff;
  border-radius: 18px;
}

.section-title {
  padding: 16px 16px 8px;
  color: #111827;
  font-size: 16px;
  font-weight: 700;
}

.avatar-panel {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  padding: 0 16px 12px;
}

.avatar-preview {
  display: block;
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
  background: #eef2ff;
}

.upload-tip,
.tip {
  color: #64748b;
  font-size: 12px;
}

.tip {
  padding: 12px 16px 0;
}

.actions {
  padding: 16px;
}
</style>
