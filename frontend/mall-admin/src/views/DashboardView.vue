<template>
  <div class="dashboard-page">
    <el-container>
      <el-header class="header">
        <span>mall-admin</span>
        <el-button size="small" @click="goLogin">登录页</el-button>
      </el-header>
      <el-main>
        <el-row :gutter="16">
          <el-col :span="6">
            <el-card>
              <div class="metric-title">商品数</div>
              <div class="metric-value">{{ dashboard.productCount || 0 }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card>
              <div class="metric-title">用户数</div>
              <div class="metric-value">{{ dashboard.userCount || 0 }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card>
              <div class="metric-title">订单数</div>
              <div class="metric-value">{{ dashboard.orderCount || 0 }}</div>
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card>
              <div class="metric-title">待处理订单</div>
              <div class="metric-value">{{ dashboard.pendingOrderCount || 0 }}</div>
            </el-card>
          </el-col>
        </el-row>

        <el-card class="table-card">
          <template #header>商品列表</template>
          <el-table :data="products" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="name" label="商品名称" />
            <el-table-column prop="salePrice" label="售价" width="120" />
            <el-table-column prop="stock" label="库存" width="120" />
            <el-table-column prop="status" label="状态" width="120" />
          </el-table>
        </el-card>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { fetchAdminProductPage } from '../api';
import { useAdminStore } from '../stores/admin';

const router = useRouter();
const adminStore = useAdminStore();
const dashboard = ref({});
const products = ref([]);

const loadDashboard = async () => {
  try {
    dashboard.value = await adminStore.loadDashboard();
    const { data } = await fetchAdminProductPage();
    products.value = data.data.list || [];
  } catch (error) {
    ElMessage.error(error?.response?.data?.msg || '后台数据加载失败');
  }
};

const goLogin = () => {
  router.push('/login');
};

onMounted(loadDashboard);
</script>

<style scoped>
.dashboard-page {
  min-height: 100vh;
  background: #f5f7fa;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.metric-title {
  color: #909399;
  font-size: 14px;
}

.metric-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 700;
}

.table-card {
  margin-top: 16px;
}
</style>
