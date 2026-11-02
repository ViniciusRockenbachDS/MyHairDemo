import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Em desenvolvimento, /api é repassado ao Spring Boot (porta 8080):
// o navegador fala só com o Vite e não há problema de CORS.
export default defineConfig({
  plugins: [react()],
  build: {
    // Segurança: não publicar o código-fonte original junto com o site.
    sourcemap: false,
  },
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
});
