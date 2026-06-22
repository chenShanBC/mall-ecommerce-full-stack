<template>
  <div class="alipay-landing-page">
    <div class="card">
      <div class="title">支付宝授权处理中</div>
      <div class="desc">{{ message }}</div>

      <van-loading v-if="status === 'loading'" size="24px" vertical>处理中</van-loading>

      <div v-else-if="status === 'error'" class="actions">
        <van-button size="small" type="primary" @click="goLogin">返回登录页</van-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { showFailToast, showSuccessToast, showLoadingToast } from 'vant';
import { useRoute, useRouter } from 'vue-router';
import { exchangeAlipayLoginTicket } from '../api';
import { useUserStore } from '../stores/user';

const route = useRoute();
const router = useRouter();
const userStore = useUserStore();

const status = ref('loading');
const message = ref('正在完成登录，请稍候...');

const goLogin = async () => {
  await router.replace('/login');
};

const persistLoginResult = async (result) => {
  userStore.token = result.token;
  userStore.profile = result;
  userStore.profileLoaded = true;
  localStorage.setItem('mall-h5-token', userStore.token);
};

onMounted(async () => {
  const loginTicket = route.query.loginTicket;
  if (!loginTicket) {
    status.value = 'error';
    message.value = '缺少登录票据，请返回登录页重试';
    showFailToast('缺少登录票据');
    return;
  }

  const toast = showLoadingToast({
    message: '登录处理中...',
    duration: 0,
    forbidClick: true,
  });

  try {
    const { data } = await exchangeAlipayLoginTicket({ loginTicket });
    await persistLoginResult(data.data);
    showSuccessToast('支付宝登录成功');
    await router.replace({ path: '/home' });
  } catch (error) {
    status.value = 'error';
    message.value = error?.response?.data?.message || '支付宝登录失败，请返回登录页重试';
    showFailToast('登录处理失败');
    await router.replace({ path: '/login', query: {} });
  } finally {
    toast.close();
  }
});
</script>

<style scoped>
.alipay-landing-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f7f8fa;
}

.card {
  width: min(92vw, 360px);
  padding: 24px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 10px 28px rgba(15, 23, 42, 0.08);
  text-align: center;
}

.title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.desc {
  margin: 8px 0 20px;
  color: #64748b;
  font-size: 13px;
}

.actions {
  display: flex;
  justify-content: center;
}
</style>
