<template>
  <div class="login-page">
    <van-nav-bar title="用户登录" />
    <div class="form-wrapper">
      <van-form @submit="handleSubmit">
        <van-cell-group inset>
          <van-field v-model="form.account" name="account" label="账号" placeholder="请输入 13800000000" />
          <van-field v-model="form.password" type="password" name="password" label="密码" placeholder="请输入 123456" />
        </van-cell-group>
        <div class="tips">示例账号：13800000000，示例密码：123456</div>
        <div class="submit-area">
          <van-button round block type="primary" native-type="submit" :loading="loading">
            登录
          </van-button>
        </div>
      </van-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { showFailToast, showSuccessToast } from 'vant';
import { useRouter } from 'vue-router';
import { useUserStore } from '../stores/user';

const router = useRouter();
const userStore = useUserStore();
const loading = ref(false);
const form = reactive({
  account: '13800000000',
  password: '123456',
});

const handleSubmit = async () => {
  try {
    loading.value = true;
    await userStore.login(form);
    showSuccessToast('登录成功');
    router.push('/home');
  } catch (error) {
    showFailToast(error?.response?.data?.msg || '登录失败');
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: #f7f8fa;
}

.form-wrapper {
  padding: 20px 16px;
}

.tips {
  padding: 16px;
  color: #666;
  font-size: 14px;
}

.submit-area {
  padding: 0 16px;
}
</style>
