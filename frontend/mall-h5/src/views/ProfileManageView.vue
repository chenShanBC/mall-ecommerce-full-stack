<template>
  <div class="page">
    <van-nav-bar class="profile-back-card" title="个人信息管理" left-arrow @click-left="handleBack" />

    <div class="section-card">
      <div class="section-title">个人资料</div>
      <div class="avatar-panel">
        <img class="avatar-preview" :src="avatarPreview" alt="avatar" referrerpolicy="no-referrer" @error="avatarPreviewFailed = true" />
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
          <van-field :model-value="userStore.maskedMobile" label="绑定手机号" readonly>
            <template #button>
              <van-button class="mobile-change-btn" size="small" native-type="button" @click.stop.prevent="openMobileBindPopup">
                更绑手机号
              </van-button>
            </template>
          </van-field>
        </van-cell-group>
        <div class="tip">支付宝头像若因防盗链加载失败，可点击“同步第三方头像”保存到个人资料后刷新。</div>
        <div class="actions">
          <van-button round block type="primary" native-type="submit" :loading="savingProfile">保存资料</van-button>
        </div>
      </van-form>
    </div>

    <van-popup v-model:show="showMobileBindPopup" round position="bottom" class="mobile-bind-popup" @closed="handleMobileBindPopupClosed">
      <div class="popup-header">
        <div>
          <div class="popup-title">更换手机号</div>
          <div class="popup-subtitle">当前手机号：{{ userStore.maskedMobile }}</div>
        </div>
      </div>
      <van-form @submit="handleBindMobile">
        <van-cell-group inset>
          <van-field v-model.trim="mobileBindForm.mobile" label="新手机号" placeholder="请输入要绑定的新手机号" maxlength="11" />
          <van-field v-model.trim="mobileBindForm.code" label="验证码" placeholder="请输入6位验证码" maxlength="6">
            <template #button>
              <van-button
                size="small"
                plain
                type="primary"
                native-type="button"
                :disabled="mobileBindCountdown > 0"
                :loading="sendingMobileCode"
                @click.stop.prevent="handleSendMobileBindCode"
              >
                {{ mobileBindCountdown > 0 ? `${mobileBindCountdown}s` : '发送验证码' }}
              </van-button>
            </template>
          </van-field>
        </van-cell-group>
        <div class="tip">先输入要绑定的新手机号并发送模拟验证码，验证码校验通过后完成绑定或换绑。</div>
        <div v-if="mobileBindDebugCode" class="debug-code">模拟验证码：{{ mobileBindDebugCode }}</div>
        <div class="actions popup-actions">
          <van-button round block type="primary" native-type="submit" :loading="bindingMobile">确认绑定/换绑</van-button>
        </div>
      </van-form>
    </van-popup>

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
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { showConfirmDialog, showFailToast, showLoadingToast, showSuccessToast } from 'vant';
import { useRouter } from 'vue-router';
import { sendMobileBindSmsCode, uploadAvatar } from '../api';
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
const avatarPreviewFailed = ref(false);
const showMobileBindPopup = ref(false);
const sendingMobileCode = ref(false);
const bindingMobile = ref(false);
const mobileBindCountdown = ref(0);
const mobileBindDebugCode = ref('');
let mobileBindCountdownTimer = null;
const profileForm = reactive({
  nickname: '',
  avatarUrl: '',
});
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});
const mobileBindForm = reactive({
  mobile: '',
  code: '',
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
  return String(url).trim().replace(/^\/upload\//i, '/uploads/');
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

const avatarPreview = computed(() => (avatarPreviewFailed.value ? defaultAvatarSvg : resolveAvatarUrl(profileForm.avatarUrl)));

const syncForm = () => {
  avatarPreviewFailed.value = false;
  profileForm.nickname = userStore.profile?.nickname || '';
  profileForm.avatarUrl = userStore.profile?.avatarUrl || '';
};

const resetPasswordForm = () => {
  passwordForm.oldPassword = '';
  passwordForm.newPassword = '';
  passwordForm.confirmPassword = '';
};

const isValidMobile = (mobile) => /^1\d{10}$/.test(mobile || '');

const startMobileBindCountdown = (seconds = 60) => {
  mobileBindCountdown.value = seconds;
  clearInterval(mobileBindCountdownTimer);
  mobileBindCountdownTimer = window.setInterval(() => {
    if (mobileBindCountdown.value <= 1) {
      clearInterval(mobileBindCountdownTimer);
      mobileBindCountdown.value = 0;
      return;
    }
    mobileBindCountdown.value -= 1;
  }, 1000);
};

const resetMobileBindForm = () => {
  mobileBindForm.mobile = '';
  mobileBindForm.code = '';
  mobileBindDebugCode.value = '';
  clearInterval(mobileBindCountdownTimer);
  mobileBindCountdown.value = 0;
};

const openMobileBindPopup = () => {
  showMobileBindPopup.value = true;
};

const handleMobileBindPopupClosed = () => {
  if (!bindingMobile.value) {
    resetMobileBindForm();
  }
};

const validateMobileBindTarget = () => {
  const mobile = mobileBindForm.mobile;
  if (!isValidMobile(mobile)) {
    showFailToast('请输入正确的新手机号');
    return false;
  }
  if (mobile === userStore.profile?.mobile) {
    showFailToast('新手机号不能与当前手机号一致');
    return false;
  }
  return true;
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

const handleSendMobileBindCode = async () => {
  if (!validateMobileBindTarget()) {
    return;
  }
  try {
    sendingMobileCode.value = true;
    const { data } = await sendMobileBindSmsCode({ mobile: mobileBindForm.mobile });
    mobileBindDebugCode.value = data?.data?.debugCode || data?.data?.code || '';
    startMobileBindCountdown(data?.data?.expireSeconds || 60);
    showSuccessToast('模拟验证码已发送');
  } catch (error) {
    showFailToast(error?.response?.data?.message || error?.response?.data?.msg || '验证码发送失败');
  } finally {
    sendingMobileCode.value = false;
  }
};

const handleBindMobile = async () => {
  if (!validateMobileBindTarget()) {
    return;
  }
  if (!/^\d{6}$/.test(mobileBindForm.code || '')) {
    showFailToast('请输入6位验证码');
    return;
  }
  try {
    bindingMobile.value = true;
    await userStore.bindMobile({ ...mobileBindForm });
    await userStore.loadProfile(true);
    showMobileBindPopup.value = false;
    resetMobileBindForm();
    showSuccessToast('手机号绑定成功');
  } catch (error) {
    showFailToast(error?.response?.data?.message || error?.response?.data?.msg || '手机号绑定失败');
  } finally {
    bindingMobile.value = false;
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

onBeforeUnmount(() => {
  clearInterval(mobileBindCountdownTimer);
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

.debug-code {
  margin: 10px 16px 0;
  padding: 10px 12px;
  color: #2563eb;
  font-size: 13px;
  font-weight: 700;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 12px;
}

.actions {
  padding: 16px;
}

.mobile-change-btn {
  min-width: 82px;
  height: 28px;
  padding: 0 12px;
  color: #ffffff;
  font-size: 12px;
  font-weight: 600;
  border: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, #5b6cff 0%, #6f7cff 100%);
  box-shadow: 0 4px 10px rgba(91, 108, 255, 0.16);
}

.mobile-change-btn:active {
  transform: translateY(1px);
  box-shadow: 0 3px 8px rgba(91, 108, 255, 0.14);
}

.mobile-change-btn :deep(.van-button__text) {
  color: #ffffff;
}

.mobile-bind-popup {
  padding: 22px 0 calc(16px + env(safe-area-inset-bottom));
}

.popup-header {
  padding: 0 16px 16px;
}

.popup-title {
  color: #111827;
  font-size: 18px;
  font-weight: 800;
}

.popup-subtitle {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}

.popup-actions {
  padding-bottom: 0;
}
</style>
