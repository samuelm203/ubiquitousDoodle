import { defineConfig, loadEnv } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'

// Die REST-API läuft im Spring-Boot-Backend. Der Dev-Server leitet /api dorthin weiter,
// damit kein CORS im Backend nötig ist. Ziel per API_TARGET überschreibbar,
// z. B. API_TARGET=http://localhost:8090 npm run dev
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const target = env.API_TARGET || 'http://localhost:8080'
  return {
    plugins: [svelte()],
    server: {
      port: 5175, // 5173 = frontend-vue, 5174 = frontend-react
      proxy: { '/api': { target, changeOrigin: true } },
    },
    preview: {
      proxy: { '/api': { target, changeOrigin: true } },
    },
  }
})
