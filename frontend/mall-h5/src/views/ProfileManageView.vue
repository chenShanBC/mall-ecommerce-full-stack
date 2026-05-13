<template>
  <div class="page">
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
const backendOrigin = import.meta.env.DEV ? 'http://localhost:9090' : window.location.origin;
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

const resolveAvatarUrl = (url, displayName) => {
  if (!url) {
    return `https://via.placeholder.com/120x120.png?text=${encodeURIComponent(displayName)}`;
  }
  if (/^https?:\/\//i.test(url)) {
    return url;
  }
  if (url.startsWith('/')) {
    return `${backendOrigin}${url}`;
  }
  return `${backendOrigin}/${url}`;
};

const avatarPreview = computed(() => resolveAvatarUrl(profileForm.avatarUrl, userStore.displayName || '用户'));

const syncForm = () => {
  profileForm.nickname = userStore.profile?.nickname || '';
  profileForm.avatarUrl = userStore.profile?.avatarUrl || '';
};

const resetPasswordForm = () => {
  passwordForm.oldPassword = '';
  passwordForm.newPassword = '';
  passwordForm.confirmPassword = '';
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
    profileForm.avatarUrl = data.data.url;
    await userStore.loadProfile();
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
