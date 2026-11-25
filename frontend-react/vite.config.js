import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// Die REST-API läuft im Spring-Boot-Backend. Der Dev-Server leitet /api dorthin weiter,
// damit kein CORS im Backend nötig ist. Ziel per API_TARGET überschreibbar,
// z. B. API_TARGET=http://localhost:8090 npm run dev
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const target = env.API_TARGET || 'http://localhost:8080'
  return {
    plugins: [react()],
    server: {
      port: 5174, // 5173 nutzt frontend-vue
      proxy: { '/api': { target, changeOrigin: true } },
    },
    preview: {
      proxy: { '/api': { target, changeOrigin: true } },
    },
  }
})
