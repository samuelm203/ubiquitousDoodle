import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// Die REST-API läuft im Spring-Boot-Backend. Der Dev-Server leitet /api dorthin weiter,
// damit kein CORS im Backend nötig ist. Ziel per API_TARGET überschreibbar.
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const target = env.API_TARGET || 'http://localhost:8080'
  return {
    plugins: [vue()],
    server: {
      port: 5173,
      proxy: { '/api': { target, changeOrigin: true } },
    },
    preview: {
      proxy: { '/api': { target, changeOrigin: true } },
    },
  }
})
