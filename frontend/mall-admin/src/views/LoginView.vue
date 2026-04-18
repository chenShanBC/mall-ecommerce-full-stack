<template>
  <div class="login-page">
    <el-card class="login-card">
      <template #header>
        <span>后台登录</span>
      </template>
      <el-form :model="form" @submit.prevent="handleSubmit">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入 admin" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入 123456" show-password />
        </el-form-item>
        <div class="tips">示例账号：admin，示例密码：123456</div>
        <el-button type="primary" :loading="loading" @click="handleSubmit">登录</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { useAdminStore } from '../stores/admin';

const router = useRouter();
const adminStore = useAdminStore();
const loading = ref(false);
const form = reactive({
  username: 'admin',
  password: '123456',
});

const handleSubmit = async () => {
  try {
    loading.value = true;
    await adminStore.login(form);
    ElMessage.success('登录成功');
    router.push('/dashboard');
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '登录失败');
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.login-card {
  width: 420px;
}

.tips {
  margin-bottom: 16px;
  color: #666;
}
</style>
