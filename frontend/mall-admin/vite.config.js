import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const adminBase = env.VITE_ADMIN_BASE || (mode === 'devlocal' ? '/' : '/admin/');
  const apiTarget = env.VITE_DEV_API_TARGET || 'http://127.0.0.1:9090';

  return {
    base: adminBase,
    plugins: [
      vue(),
      AutoImport({
        resolvers: [ElementPlusResolver()],
      }),
      Components({
        resolvers: [ElementPlusResolver()],
      }),
    ],
    server: {
      host: '0.0.0.0',
      port: 5174,
      strictPort: true,
      allowedHosts: ['localhost', '127.0.0.1', '0.0.0.0', 'mallfei.cloud'],
      proxy: {
        '/api': {
          target: apiTarget,
          changeOrigin: true,
          configure: (proxy) => {
            proxy.on('proxyReq', (proxyReq) => {
              proxyReq.removeHeader('origin');
            });
          },
        },
        '/ws': {
          target: apiTarget,
          changeOrigin: true,
          ws: true,
        },
        '/uploads': {
          target: apiTarget,
          changeOrigin: true,
        },
        '/upload': {
          target: apiTarget,
          changeOrigin: true,
        },
      },
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (!id.includes('node_modules')) {
              return;
            }
            if (id.includes('vue-router')) {
              return 'vue-router';
            }
            if (id.includes('pinia')) {
              return 'pinia';
            }
            if (id.includes('axios')) {
              return 'axios';
            }
            if (id.includes('/vue/') || id.includes('@vue')) {
              return 'vue-vendor';
            }
          },
        },
      },
    },
  };
});
