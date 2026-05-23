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
import { computed, onActivated, onMounted } from 'vue';
import { showFailToast, showSuccessToast } from 'vant';
import { useRouter } from 'vue-router';
import { useUserStore } from '../stores/user';
import { ensureAccessGate, redirectLoginWithNotice } from '../utils/requireLogin';

const router = useRouter();
const userStore = useUserStore();

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '');
const backendOrigin = (() => {
  if (/^https?:\/\//i.test(apiBaseUrl)) {
    return apiBaseUrl.replace(/\/api$/, '');
  }
  return window.location.origin;
})();

const profileName = computed(() => userStore.displayName || '普通用户');
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
  return url.replace(/^\/upload\//i, '/uploads/');
};

const avatarUrl = computed(() => {
  const rawAvatarUrl = normalizeAvatarPath(userStore.profile?.avatarUrl);
  if (!rawAvatarUrl) {
    return defaultAvatarSvg;
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

const ensureSession = async (force = false) => {
  try {
    const valid = await ensureAccessGate(router, '/profile', { force });
    if (!valid) {
      return;
    }
  } catch {
    showFailToast('登录状态已失效，请重新登录');
    router.replace('/login');
  }
};

onMounted(() => {
  ensureSession(true);
});

onActivated(() => {
  ensureSession(false);
});
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding: 8px 12px 86px;
  background:
    radial-gradient(circle at top left, rgba(129, 140, 248, 0.2), transparent 28%),
    radial-gradient(circle at top right, rgba(236, 72, 153, 0.11), transparent 24%),
    linear-gradient(180deg, #edf3ff 0%, #f7f9ff 100%);
}

.profile-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-top: 4px;
  padding: 20px 18px;
  background: linear-gradient(135deg, #5b6cff 0%, #4f46e5 100%);
  border: 1px solid rgba(255, 255, 255, 0.68);
  border-radius: 24px;
  color: #fff;
  box-shadow: 0 18px 42px rgba(79, 70, 229, 0.22);
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
  gap: 16px;
  margin-top: 16px;
}

.stat-card,
.menu-card {
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(255, 255, 255, 0.88);
  border-radius: 22px;
  box-shadow: 0 14px 36px rgba(108, 123, 225, 0.1);
  backdrop-filter: blur(18px);
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
