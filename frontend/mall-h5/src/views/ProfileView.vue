<template>
  <div class="page">
    <div class="profile-hero">
      <div class="hero-main">
        <img class="hero-avatar" :src="avatarUrl" alt="avatar" />
        <div>
          <div class="hero-title">{{ profileName }}</div>
          <div class="hero-subtitle">{{ userStore.maskedMobile }}</div>
        </div>
      </div>
      <van-button class="logout-btn" round plain size="small" @click="handleLogout">退出登录</van-button>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">用户身份</div>
        <div class="stat-value">{{ userStore.profile?.identityLabel || '普通用户' }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">绑定手机号</div>
        <div class="stat-value stat-text">{{ userStore.maskedMobile }}</div>
      </div>
    </div>

    <div class="menu-card">
      <van-cell title="个人信息管理" label="修改昵称、头像、密码" is-link @click="router.push('/profile/manage')" />
      <van-cell title="我的订单" is-link @click="router.push('/orders')" />
      <van-cell title="收货地址管理" is-link @click="router.push('/address')" />
      <van-cell title="购物车结算" is-link @click="router.push('/cart')" />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue';
import { showFailToast, showSuccessToast } from 'vant';
import { useRouter } from 'vue-router';
import { useUserStore } from '../stores/user';

const router = useRouter();
const userStore = useUserStore();
const backendOrigin = import.meta.env.DEV ? 'http://localhost:9090' : window.location.origin;

const profileName = computed(() => userStore.displayName || '普通用户');
const avatarUrl = computed(() => {
  const rawAvatarUrl = userStore.profile?.avatarUrl;
  if (!rawAvatarUrl) {
    return `https://via.placeholder.com/120x120.png?text=${encodeURIComponent(profileName.value)}`;
  }
  if (/^https?:\/\//i.test(rawAvatarUrl)) {
    return rawAvatarUrl;
  }
  if (rawAvatarUrl.startsWith('/')) {
    return `${backendOrigin}${rawAvatarUrl}`;
  }
  return `${backendOrigin}/${rawAvatarUrl}`;
});

const handleLogout = async () => {
  await userStore.logout();
  showSuccessToast('已退出登录');
  router.replace('/login');
};

onMounted(async () => {
  try {
    await userStore.loadProfile();
  } catch (error) {
    showFailToast(error?.response?.data?.msg || '用户信息加载失败');
  }
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 0 12px 80px;
  background: #f6f8fb;
}

.profile-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  padding: 20px 18px;
  background: linear-gradient(135deg, #2563eb, #4f46e5);
  border-radius: 24px;
  color: #fff;
}

.hero-main {
  display: flex;
  align-items: center;
  gap: 14px;
}

.hero-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.2);
}

.hero-title {
  font-size: 24px;
  font-weight: 700;
}

.hero-subtitle {
  margin-top: 8px;
  font-size: 13px;
  opacity: 0.9;
}

:deep(.logout-btn) {
  color: #ffffff;
  border-color: rgba(255, 255, 255, 0.9);
  background: rgba(255, 255, 255, 0.12);
}

:deep(.logout-btn .van-button__text) {
  color: #ffffff;
  font-weight: 600;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-top: 12px;
}

.stat-card,
.menu-card {
  background: #fff;
  border-radius: 18px;
}

.stat-card {
  padding: 16px;
}

.stat-label {
  color: #64748b;
  font-size: 13px;
}

.stat-value {
  margin-top: 8px;
  color: #111827;
  font-size: 18px;
  font-weight: 700;
}

.stat-text {
  font-size: 16px;
  word-break: break-all;
}

.menu-card {
  margin-top: 12px;
  overflow: hidden;
}
</style>
