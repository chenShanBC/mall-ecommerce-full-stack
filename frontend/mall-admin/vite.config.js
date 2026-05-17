import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';

export default defineConfig({
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
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://localhost:9090',
        changeOrigin: true,
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => {
            proxyReq.removeHeader('origin');
          });
        },
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
