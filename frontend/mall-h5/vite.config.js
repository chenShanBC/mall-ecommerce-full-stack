import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  server: {
    host: '0.0.0.0',
    port: 5173,
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
      '/upload': {
        target: process.env.VITE_DEV_API_TARGET || 'http://127.0.0.1:9090',
        changeOrigin: true,
      },
      '/uploads': {
        target: process.env.VITE_DEV_API_TARGET || 'http://127.0.0.1:9090',
        changeOrigin: true,
      },
    },
  },
});
