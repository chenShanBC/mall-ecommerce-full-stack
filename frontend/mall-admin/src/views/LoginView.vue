<template>
  <div class="login-page">
    <div class="login-shell">
      <section class="login-brand glass-panel">
        <div class="brand-mark">M</div>
        <div class="brand-title">mall-admin</div>
        <div class="brand-subtitle">统一运营控制台</div>
        <p class="brand-desc">把订单、支付、商品、库存与账号权限集中在一个更现代、更清晰的后台界面里。</p>
        <div class="brand-feature-list">
          <div class="brand-feature"><strong>数据看板</strong><span>用图表快速看见业务波动</span></div>
          <div class="brand-feature"><strong>权限分层</strong><span>角色模板与精细权限一起控制</span></div>
          <div class="brand-feature"><strong>运营闭环</strong><span>订单、支付、库存与日志统一管理</span></div>
        </div>
      </section>

      <section class="login-card glass-panel">
        <div class="login-card__header">
          <div class="login-card__eyebrow">WELCOME BACK</div>
          <h1>后台登录</h1>
          <p>请输入管理员账号与密码，进入运营后台。</p>
        </div>
        <el-form :model="form" class="login-form" @submit.prevent="handleSubmit">
          <el-form-item label="账号">
            <el-input v-model="form.username" placeholder="请输入 admin" size="large" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" placeholder="请输入 123456" show-password size="large" />
          </el-form-item>
          <div class="tips">示例账号：admin，示例密码：123456</div>
          <el-button class="login-button" type="primary" :loading="loading" @click="handleSubmit">登录后台</el-button>
        </el-form>
      </section>
    </div>
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
.login-page{min-height:100vh;display:flex;align-items:center;justify-content:center;padding:32px;background:radial-gradient(circle at top left,rgba(129,140,248,.22),transparent 28%),radial-gradient(circle at bottom right,rgba(236,72,153,.14),transparent 26%),linear-gradient(180deg,#edf3ff 0%,#f7f9ff 100%)}.login-shell{width:min(1180px,100%);display:grid;grid-template-columns:minmax(320px,1.05fr) minmax(380px,460px);gap:24px}.glass-panel{border-radius:32px;background:rgba(255,255,255,.8);backdrop-filter:blur(18px);border:1px solid rgba(255,255,255,.88);box-shadow:0 24px 60px rgba(108,123,225,.14)}.login-brand{padding:40px 36px;display:flex;flex-direction:column;justify-content:center}.brand-mark{width:64px;height:64px;border-radius:22px;display:grid;place-items:center;font-size:30px;font-weight:800;color:#4f46e5;background:linear-gradient(135deg,#eef2ff,#dbeafe)}.brand-title{margin-top:24px;font-size:34px;font-weight:800;color:#2d3a64}.brand-subtitle{margin-top:8px;color:#7c8db5;font-weight:700}.brand-desc{margin-top:18px;max-width:520px;color:#6b7a99;line-height:1.8}.brand-feature-list{display:flex;flex-direction:column;gap:14px;margin-top:28px}.brand-feature{padding:16px 18px;border-radius:22px;background:linear-gradient(135deg,rgba(248,250,255,.98),rgba(238,242,255,.94));display:flex;justify-content:space-between;gap:16px}.brand-feature strong{color:#334155}.brand-feature span{color:#94a3b8}.login-card{padding:34px 30px;display:flex;flex-direction:column;justify-content:center}.login-card__eyebrow{font-size:12px;font-weight:700;color:#7c8db5}.login-card__header h1{margin:12px 0 0;font-size:32px;color:#2d3a64}.login-card__header p{margin:12px 0 0;color:#94a3b8}.login-form{margin-top:26px}.tips{margin-bottom:18px;color:#94a3b8;font-size:13px}.login-button{width:100%;height:48px;border:none;border-radius:999px;background:linear-gradient(135deg,#5b6cff,#7c8dff)!important;box-shadow:0 16px 30px rgba(99,102,241,.28)}@media (max-width:960px){.login-shell{grid-template-columns:1fr}.login-brand{display:none}}
</style>
