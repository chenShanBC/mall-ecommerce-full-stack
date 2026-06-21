import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';

export default defineConfig({
  base: process.env.VITE_ADMIN_BASE || '/admin/',
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
        target: process.env.VITE_DEV_API_TARGET || 'http://127.0.0.1:9090',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.removeHeader('origin');
          });
        },
      },
      '/ws': {
        target: process.env.VITE_DEV_API_TARGET || 'http://127.0.0.1:9090',
        changeOrigin: true,
        ws: true,
      },
      '/uploads': {
        target: process.env.VITE_DEV_API_TARGET || 'http://127.0.0.1:9090',
        changeOrigin: true,
      },
      '/upload': {
        target: process.env.VITE_DEV_API_TARGET || 'http://127.0.0.1:9090',
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
});
