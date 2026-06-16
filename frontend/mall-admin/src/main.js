import { createApp } from 'vue';
import { createPinia } from 'pinia';
import 'element-plus/dist/index.css';
import './styles/admin-dialog.css';
import './styles/admin-table.css';
import App from './App.vue';
import router from './router';
import { useAdminStore } from './stores/admin';

window.__mallAdminRouter = router;

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);

const adminStore = useAdminStore(pinia);

app.use(router).mount('#app');

if (adminStore.token?.trim()) {
  adminStore.initForceLogout(router);
}
