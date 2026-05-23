import { createApp } from 'vue';
import { createPinia } from 'pinia';
import Vant from 'vant';
import 'vant/lib/index.css';
import App from './App.vue';
import router from './router';
import { useUserStore } from './stores/user';
import { initForceLogoutSocket } from './utils/wsForceLogout';

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);

const userStore = useUserStore(pinia);

app.use(router).use(Vant).mount('#app');

if (userStore.token?.trim()) {
  initForceLogoutSocket(userStore, router);
  userStore.loadProfile().catch(() => {
    userStore.clearSession();
  });
}
